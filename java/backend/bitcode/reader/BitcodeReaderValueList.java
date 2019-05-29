package backend.bitcode.reader;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.support.LLVMContext;
import backend.type.Type;
import backend.value.*;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class BitcodeReaderValueList {
  /**
   * A class for maintaining the slot number definition as a placeholder for the actual
   * definition for forward constants defs.
   */
  public static class ConstantPlaceHolder extends ConstantExpr {
    /**
     * Constructs a new instruction representing the specified constants.
     *
     * @param ty
     */
    protected ConstantPlaceHolder(Type ty) {
      super(ty, Operator.UserOp1);
      reserve(1);
      setOperand(0, UndefValue.get(LLVMContext.Int32Ty));
    }
  }

  private ArrayList<Value> values;
  private LinkedList<Pair<Constant, Integer>> resolveConstants;

  public BitcodeReaderValueList() {
    values = new ArrayList<>();
    resolveConstants = new LinkedList<>();
  }

  public int size() { return values.size(); }
  public void add(Value v) { values.add(v); }
  public void clear() {
    Util.assertion(resolveConstants.isEmpty(), "Constants not resolved?");
    values.clear();
  }

  public void set(int idx, Value val) {
    values.set(idx, val);
  }

  public Value get(int idx) {
    return values.get(idx);
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }

  public void resize(int n) {
    if (n < size()) {
      shrinkTo(n);
      return;
    }
    for (int i = size(); i < n; i++)
      add(null);
  }

  public Constant getConstantFwdRefs(int idx, Type ty) {
    if (idx >= size())
      resize(idx + 1);

    Value v = values.get(idx);
    if (v != null) {
      Util.assertion(ty.equals(v.getType()), "Type mismatch in constant table!");
      return (Constant) v;
    }
    Constant c = new ConstantPlaceHolder(ty);
    values.set(idx, c);
    return c;
  }

  public Value getValueFwdRef(int idx, Type ty) {
    if (idx >= size())
      resize(idx+1);


    Value v = values.get(idx);
    if (v != null) {
      Util.assertion(ty == null || ty.equals(v.getType()), "Type mismatch in constant table!");
      return v;
    }
    // no type specified, must be invalid reference.
    if (ty == null) return null;

    // create and return a placeholder, which will later be used.
    Value c = new Argument(ty);
    values.set(idx, c);
    return c;
  }

  public void assignValue(Value val, int idx) {
    if (idx == size()) {
      add(val);
      return;
    }

    if (idx >= size())
      resize(idx + 1);

    Value oldV = values.get(idx);
    if (oldV == null) {
      values.set(idx, val);
      return;
    }

    // Handle constants and non-constants (e.g. instrs) differently for
    // efficiency.
    if (oldV instanceof Constant) {
      Constant phc = (Constant) oldV;
      resolveConstants.add(Pair.get(phc, idx));
      values.set(idx, val);
    }
    else {
      // If there was a forward reference to this value, replace it.
      Value prevVal = oldV;
      oldV.replaceAllUsesWith(val);
    }
  }

  /**
   * Once all constants are read, this method will be called to resolves any forward references.
   * The idea behind this is that we sometimes get constants (such as large arrays) which reference
   * many forward ref constants.  Replacing each of these causes a lot of thrashing when
   * building/reuniquing the constant.  Instead of doing this, we look at all the uses and rewrite
   * all the place holders at once for any constant that uses a placeholder.
   */
  public void resolveConstantForwardRefs() {
    ArrayList<Constant> newOps = new ArrayList<>();
    while (!resolveConstants.isEmpty()) {
      Value realVal = get(resolveConstants.getLast().second);
      Constant placeholder = resolveConstants.getLast().first;
      resolveConstants.removeLast();

      // loop over all users of the placeholder, updating them to reference the new value.
      // if they refer more than one placeholder, update them all at once.
      while (!placeholder.isUseEmpty()) {
        Use ui = placeholder.useAt(0);
        User u = ui.getUser();

        // if the using object isn't uniqued, just update the operands.
        // This handles instructions and initializers for global variables.
        if (!(u instanceof Constant) || u instanceof GlobalValue) {
          ui.setValue(realVal);
          continue;
        }

        // otherwise, we have a constant that uses the placeholder. Replace that
        // constant with a new constant that has all placeholder uses updated.
        Constant userC = (Constant) u;
        for (int i = 0, e = userC.getNumOfOperands(); i < e; i++) {
          Value newOp = null;
          Constant op = userC.operand(i);
          if (!(op instanceof ConstantPlaceHolder)) {
            // not a placeholder reference.
            newOp = op;
          }
          else if (op.equals(placeholder)) {
            // common case is that it just refers to this one placeholder.
            newOp = realVal;
          }
          else {
            // otherwise, look up the placeholder in resolveConstants.
            boolean found = false;
            for (Pair<Constant, Integer> itr : resolveConstants) {
              if (itr.first.equals(op)) {
                newOp = get(itr.second);
                found = true;
                break;
              }
            }
            Util.assertion(found);
          }
          newOps.add((Constant) newOp);
        }

        // Make the new constant.
        Constant newC = null;
        if (userC instanceof ConstantArray) {
          ConstantArray ca = (ConstantArray) userC;
          newC = ConstantArray.get(ca.getType(), newOps);
        }
        else if (userC instanceof ConstantStruct) {
          ConstantStruct st = (ConstantStruct) userC;
          newC = ConstantStruct.get(newOps, st.getType().isPacked());
        }
        else if (userC instanceof ConstantVector) {
          newC = ConstantVector.get(newOps);
        }
        else {
          Util.assertion(userC instanceof ConstantExpr, "Must be a ConstantExpr");
          newC = ((ConstantExpr)userC).getWithOperands(newOps);
        }

        userC.replaceAllUsesWith(newC);
        userC.destroyConstant();
        newOps.clear();
      }

      placeholder.replaceAllUsesWith(realVal);
    }
  }

  public void shrinkTo(int newSize) {
    Util.assertion(newSize <= size(), "Invalid shrinkTo request!");
    values.subList(newSize, size()).clear();
  }
}
