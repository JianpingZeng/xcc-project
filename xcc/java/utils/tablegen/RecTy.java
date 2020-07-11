package utils.tablegen;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import tools.Util;
import utils.tablegen.Init.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public abstract class RecTy {
  public abstract void print(PrintStream os);

  /**
   * Print out the result of {@linkplain #print(PrintStream)} to the String.
   *
   * @return
   */
  @Override
  public String toString() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    print(new PrintStream(os));
    return os.toString();
  }

  public void dump() {
    print(System.err);
  }

  /**
   * Return {＠code true} if all values of 'this' type can converted to the
   * specified type.
   *
   * @param rhs
   * @return
   */
  public abstract boolean typeIsConvertiableTo(RecTy rhs);

  // These methods should only be called from subclasses of Init.
  public Init convertValue(UnsetInit ui) {
    return null;
  }

  public Init convertValue(BitInit bi) {
    return null;
  }

  public Init convertValue(BitsInit bi) {
    return null;
  }

  public Init convertValue(IntInit ii) {
    return null;
  }

  public Init convertValue(StringInit si) {
    return null;
  }

  public Init convertValue(ListInit li) {
    return null;
  }

  public Init convertValue(BinOpInit ui) {
    return convertValue((TypedInit) ui);
  }

  public Init convertValue(UnOpInit ui) {
    return convertValue((TypedInit) ui);
  }

  public Init convertValue(TernOpInit ti) {
    return convertValue((TypedInit) ti);
  }

  public Init convertValue(CodeInit ci) {
    return null;
  }

  public Init convertValue(VarBitInit vb) {
    return null;
  }

  public Init convertValue(DefInit di) {
    return null;
  }

  public Init convertValue(DagInit di) {
    return null;
  }

  public Init convertValue(TypedInit ti) {
    return null;
  }

  public Init convertValue(VarInit vi) {
    return convertValue((TypedInit) vi);
  }

  public Init convertValue(FieldInit fi) {
    return convertValue((TypedInit) fi);
  }

  // These methods should only be called by subclasses of RecTy.
  // baseClassOf - These virtual methods should be overloaded to return true iff
  // all values of type 'RHS' can be converted to the 'this' type.
  public boolean baseClassOf(BitRecTy rhs) {
    return false;
  }

  public boolean baseClassOf(BitsRecTy rhs) {
    return false;
  }

  public boolean baseClassOf(IntRecTy rhs) {
    return false;
  }

  public boolean baseClassOf(StringRecTy rhs) {
    return false;
  }

  public boolean baseClassOf(ListRecTy rhs) {
    return false;
  }

  public boolean baseClassOf(CodeRecTy rhs) {
    return false;
  }

  public boolean baseClassOf(DagRecTy rhs) {
    return false;
  }

  public boolean baseClassOf(RecordRecTy rhs) {
    return false;
  }


  public static class BitRecTy extends RecTy {
    @Override
    public Init convertValue(UnsetInit ui) {
      return ui;
    }

    @Override
    public Init convertValue(BitInit bi) {
      return bi;
    }

    @Override
    public Init convertValue(BitsInit bi) {
      if (bi.getNumBits() != 1) return null;
      return bi.getBit(0);
    }

    @Override
    public Init convertValue(IntInit ii) {
      long val = ii.getValue();
      if (val != 0 && val != 1) return null;
      return new BitInit(val != 0);
    }

    @Override
    public Init convertValue(StringInit si) {
      return null;
    }

    @Override
    public Init convertValue(ListInit li) {
      return null;
    }

    @Override
    public Init convertValue(BinOpInit ui) {
      return super.convertValue(ui);
    }

    @Override
    public Init convertValue(UnOpInit ui) {
      return super.convertValue(ui);
    }

    @Override
    public Init convertValue(TernOpInit ti) {
      return super.convertValue(ti);
    }

    @Override
    public Init convertValue(CodeInit ci) {
      return null;
    }

    @Override
    public Init convertValue(VarBitInit vb) {
      return vb;
    }

    @Override
    public Init convertValue(DefInit di) {
      return null;
    }

    @Override
    public Init convertValue(DagInit di) {
      return null;
    }

    @Override
    public Init convertValue(TypedInit ti) {
      if (ti.getType() instanceof BitRecTy)
        return ti;
      return null;
    }

    @Override
    public Init convertValue(VarInit vi) {
      return super.convertValue(vi);
    }

    @Override
    public Init convertValue(FieldInit fi) {
      return super.convertValue(fi);
    }

    @Override
    public void print(PrintStream os) {
      os.print("bit");
    }

    @Override
    public boolean typeIsConvertiableTo(RecTy rhs) {
      return rhs.baseClassOf(this);
    }

    @Override
    public boolean baseClassOf(BitRecTy rhs) {
      return true;
    }

    @Override
    public boolean baseClassOf(BitsRecTy rhs) {
      return rhs.getNumBits() == 1;
    }

    @Override
    public boolean baseClassOf(IntRecTy rhs) {
      return true;
    }

    @Override
    public boolean baseClassOf(StringRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(ListRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(CodeRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(DagRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(RecordRecTy rhs) {
      return false;
    }
  }

  public static class BitsRecTy extends RecTy {
    private int size;

    public BitsRecTy(int sz) {
      size = sz;
    }

    public int getNumBits() {
      return size;
    }

    @Override
    public Init convertValue(UnsetInit ui) {
      BitsInit ret = new BitsInit(size);

      for (int i = 0; i < size; i++)
        ret.setBit(i, UnsetInit.getInstance());
      return ret;
    }

    @Override
    public Init convertValue(BitInit bi) {
      if (size != 1) return null;
      BitsInit ret = new BitsInit(1);
      ret.setBit(0, bi);
      return ret;
    }

    @Override
    public Init convertValue(BitsInit bi) {
      return size == bi.getNumBits() ? bi : null;
    }

    /**
     * Check if the given value can fit in the specified number of bits.
     * @param value
     * @param numBits
     * @return
     */
    private static boolean canFitInBItfield(long value, int numBits) {
      return numBits >= 64 || (value >> numBits) == 0 || (value >> (numBits - 1) == -1);
    }
    /**
     * convertValue from Int initializer to bits type: Split the integer up into the
     * appropriate bits.
     *
     * @param ii
     * @return
     */
    @Override
    public Init convertValue(IntInit ii) {
      long val = ii.getValue();
      if (!canFitInBItfield(val, size))
        return null;

      BitsInit ret = new BitsInit(size);
      for (int i = 0; i < size; i++)
        ret.setBit(i, new BitInit((val & (1L << i)) != 0));
      return ret;
    }

    @Override
    public Init convertValue(StringInit si) {
      return null;
    }

    @Override
    public Init convertValue(ListInit li) {
      return null;
    }

    @Override
    public Init convertValue(BinOpInit ui) {
      return null;
    }

    @Override
    public Init convertValue(CodeInit ci) {
      return null;
    }

    @Override
    public Init convertValue(VarBitInit vb) {
      return null;
    }

    @Override
    public Init convertValue(DefInit di) {
      return null;
    }

    @Override
    public Init convertValue(DagInit di) {
      return null;
    }

    @Override
    public Init convertValue(TypedInit ti) {
      if (ti.getType() instanceof BitsRecTy) {
        BitsRecTy bbty = (BitsRecTy) ti.getType();
        if (bbty.size == size) {
          BitsInit ret = new BitsInit(size);
          for (int i = 0; i < size; i++)
            ret.setBit(i, new VarBitInit(ti, i));
          return ret;
        }
      }
      if (size == 1 && ti.getType() instanceof BitRecTy) {
        BitsInit ret = new BitsInit(1);
        ret.setBit(0, ti);
        return ret;
      }

      if (ti instanceof TernOpInit) {
        TernOpInit tern = (TernOpInit) ti;
        if (tern.getOpcode() == TernOpInit.TernaryOp.IF) {
          Init lhs = tern.getLhs();
          Init mhs = tern.getMhs();
          Init rhs = tern.getRhs();

          if (mhs instanceof IntInit && rhs instanceof IntInit) {
            IntInit mhsI = (IntInit) mhs;
            IntInit rhsI = (IntInit) rhs;
            long mhsVal = mhsI.getValue(), rhsVal = rhsI.getValue();
            if (canFitInBItfield(mhsVal, size) && canFitInBItfield(rhsVal, size)) {
              BitsInit ret = new BitsInit(size);

              for (int i = 0; i < size; i++) {
                ret.setBit(i, new TernOpInit(TernOpInit.TernaryOp.IF, lhs,
                    new IntInit((mhsVal & (1L << i)) != 0 ? 1 : 0),
                    new IntInit((rhsVal & (1L << i)) != 0 ? 1 : 0),
                    ti.getType()));
              }
              return ret;
            }
          }
          else {
            if (mhs instanceof BitsInit && rhs instanceof BitsInit) {
              BitsInit mhsBI = (BitsInit) mhs;
              BitsInit rhsBI = (BitsInit) rhs;
              BitsInit ret = new BitsInit(size);
              for (int i = 0; i < size; ++i) {
                ret.setBit(i, new TernOpInit(TernOpInit.TernaryOp.IF, lhs,
                    mhsBI.getBit(i), rhsBI.getBit(i), ti.getType()));
              }
              return ret;
            }
          }
        }
      }

      return null;
    }

    @Override
    public Init convertValue(VarInit vi) {
      return super.convertValue(vi);
    }

    @Override
    public Init convertValue(FieldInit fi) {
      return super.convertValue(fi);
    }

    @Override
    public void print(PrintStream os) {
      os.printf("bits<%d>", size);
    }

    @Override
    public boolean typeIsConvertiableTo(RecTy rhs) {
      return rhs.baseClassOf(this);
    }

    @Override
    public boolean baseClassOf(BitRecTy rhs) {
      return size == 1;
    }

    @Override
    public boolean baseClassOf(BitsRecTy rhs) {
      return size == rhs.size;
    }

    @Override
    public boolean baseClassOf(IntRecTy rhs) {
      return true;
    }

    @Override
    public boolean baseClassOf(StringRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(ListRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(CodeRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(DagRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(RecordRecTy rhs) {
      return false;
    }
  }

  /**
   * Represent an integer value of no particular getNumOfSubLoop.
   */
  public static class IntRecTy extends RecTy {
    @Override
    public Init convertValue(UnsetInit ui) {
      return ui;
    }

    @Override
    public Init convertValue(BitInit bi) {
      return new IntInit(bi.getValue() ? 1 : 0);
    }

    @Override
    public Init convertValue(BitsInit bi) {
      int result = 0;
      for (int i = 0, e = bi.getNumBits(); i < e; i++) {
        if (bi.getBit(i) instanceof BitInit) {
          int val = ((BitInit) bi.getBit(i)).getValue() ? 1 : 0;
          result |= val << i;
        } else
          return null;
      }
      return new IntInit(result);
    }

    @Override
    public Init convertValue(IntInit ii) {
      return ii;
    }

    @Override
    public Init convertValue(StringInit si) {
      return null;
    }

    @Override
    public Init convertValue(ListInit li) {
      return null;
    }

    @Override
    public Init convertValue(BinOpInit ui) {
      return null;
    }

    @Override
    public Init convertValue(CodeInit ci) {
      return null;
    }

    @Override
    public Init convertValue(VarBitInit vb) {
      return vb;
    }

    @Override
    public Init convertValue(DefInit di) {
      return null;
    }

    @Override
    public Init convertValue(DagInit di) {
      return null;
    }

    @Override
    public Init convertValue(TypedInit ti) {
      if (ti.getType().typeIsConvertiableTo(this))
        return ti;
      return null;
    }

    @Override
    public Init convertValue(VarInit vi) {
      return super.convertValue(vi);
    }

    @Override
    public Init convertValue(FieldInit fi) {
      return super.convertValue(fi);
    }

    @Override
    public void print(PrintStream os) {
      os.print("int");
    }

    @Override
    public boolean typeIsConvertiableTo(RecTy rhs) {
      Util.assertion(rhs != null);
      return rhs.baseClassOf(this);
    }

    @Override
    public boolean baseClassOf(BitRecTy rhs) {
      return true;
    }

    @Override
    public boolean baseClassOf(BitsRecTy rhs) {
      return true;
    }

    @Override
    public boolean baseClassOf(IntRecTy rhs) {
      return true;
    }

    @Override
    public boolean baseClassOf(StringRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(ListRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(CodeRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(DagRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(RecordRecTy rhs) {
      return false;
    }
  }

  public static class StringRecTy extends RecTy {
    @Override
    public Init convertValue(UnsetInit ui) {
      return ui;
    }

    @Override
    public Init convertValue(BitInit bi) {
      return null;
    }

    @Override
    public Init convertValue(BitsInit bi) {
      return null;
    }

    @Override
    public Init convertValue(IntInit ii) {
      return null;
    }

    @Override
    public Init convertValue(StringInit si) {
      return si;
    }

    @Override
    public Init convertValue(ListInit li) {
      return null;
    }

    @Override
    public Init convertValue(BinOpInit ui) {
      if (ui.getOpcode() == BinOpInit.BinaryOp.STRCONCAT) {
        Init l = ui.getLhs().convertInitializerTo(this);
        Init r = ui.getRhs().convertInitializerTo(this);

        if (l == null || r == null) return null;
        if (l != ui.getLhs() || r != ui.getRhs())
          return new BinOpInit(BinOpInit.BinaryOp.STRCONCAT, l, r, this);
        return ui;
      }
      return null;
    }

    @Override
    public Init convertValue(CodeInit ci) {
      return null;
    }

    @Override
    public Init convertValue(VarBitInit vb) {
      return null;
    }

    @Override
    public Init convertValue(DefInit di) {
      return null;
    }

    @Override
    public Init convertValue(DagInit di) {
      return null;
    }

    @Override
    public Init convertValue(TypedInit ti) {
      if (ti.getType() instanceof StringRecTy) {
        return ti;
      }
      return null;
    }

    @Override
    public Init convertValue(VarInit vi) {
      return super.convertValue(vi);
    }

    @Override
    public Init convertValue(FieldInit fi) {
      return super.convertValue(fi);
    }

    @Override
    public void print(PrintStream os) {
      os.printf("string");
    }

    @Override
    public boolean typeIsConvertiableTo(RecTy rhs) {
      return rhs.baseClassOf(this);
    }

    @Override
    public boolean baseClassOf(BitRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(BitsRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(IntRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(StringRecTy rhs) {
      return true;
    }

    @Override
    public boolean baseClassOf(ListRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(CodeRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(DagRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(RecordRecTy rhs) {
      return false;
    }
  }

  /**
   * <p>
   * 'list&lt;Ty&gt;' - Represent a list of values, all of which must
   * be of the specified type.
   * </p>
   */
  public static class ListRecTy extends RecTy {
    private RecTy ty;

    public ListRecTy(RecTy eltTy) {
      ty = eltTy;
    }

    public RecTy getElementType() {
      return ty;
    }

    @Override
    public void print(PrintStream os) {
      os.print("list<");
      ty.print(os);
      os.print(">");
    }

    @Override
    public boolean typeIsConvertiableTo(RecTy rhs) {
      return rhs.baseClassOf(this);
    }

    @Override
    public Init convertValue(UnsetInit ui) {
      return ui;
    }

    @Override
    public Init convertValue(BitInit bi) {
      return null;
    }

    @Override
    public Init convertValue(BitsInit bi) {
      return null;
    }

    @Override
    public Init convertValue(IntInit ii) {
      return null;
    }

    @Override
    public Init convertValue(StringInit si) {
      return null;
    }

    @Override
    public Init convertValue(ListInit li) {
      ArrayList<Init> elts = new ArrayList<>();

      for (int i = 0, e = li.getSize(); i < e; i++) {
        Init ci = li.getElement(i).convertInitializerTo(ty);
        if (ci != null)
          elts.add(ci);
        else
          return null;
      }
      return new ListInit(elts, this);
    }

    @Override
    public Init convertValue(BinOpInit ui) {
      return null;
    }

    @Override
    public Init convertValue(CodeInit ci) {
      return null;
    }

    @Override
    public Init convertValue(VarBitInit vb) {
      return null;
    }

    @Override
    public Init convertValue(DefInit di) {
      return null;
    }

    @Override
    public Init convertValue(DagInit di) {
      return null;
    }

    @Override
    public Init convertValue(TypedInit ti) {
      if (ti.getType() instanceof ListRecTy)
        if (((ListRecTy) ti.getType()).getElementType().typeIsConvertiableTo(ty))
          return ti;
      return null;
    }

    @Override
    public Init convertValue(VarInit vi) {
      return super.convertValue(vi);
    }

    @Override
    public Init convertValue(FieldInit fi) {
      return super.convertValue(fi);
    }

    @Override
    public boolean baseClassOf(BitRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(BitsRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(IntRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(StringRecTy rhs) {
      return true;
    }

    @Override
    public boolean baseClassOf(ListRecTy rhs) {
      return rhs.getElementType().typeIsConvertiableTo(ty);
    }

    @Override
    public boolean baseClassOf(CodeRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(DagRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(RecordRecTy rhs) {
      return false;
    }
  }

  /**
   * Represents a code fragment.
   */
  public static class CodeRecTy extends RecTy {
    @Override
    public void print(PrintStream os) {
      os.print("code");
    }

    @Override
    public boolean typeIsConvertiableTo(RecTy rhs) {
      return rhs.baseClassOf(this);
    }

    @Override
    public Init convertValue(UnsetInit ui) {
      return ui;
    }

    @Override
    public Init convertValue(BitInit bi) {
      return null;
    }

    @Override
    public Init convertValue(BitsInit bi) {
      return null;
    }

    @Override
    public Init convertValue(IntInit ii) {
      return null;
    }

    @Override
    public Init convertValue(StringInit si) {
      return null;
    }

    @Override
    public Init convertValue(ListInit li) {
      return null;
    }

    @Override
    public Init convertValue(BinOpInit ui) {
      return null;
    }

    @Override
    public Init convertValue(CodeInit ci) {
      return ci;
    }

    @Override
    public Init convertValue(VarBitInit vb) {
      return null;
    }

    @Override
    public Init convertValue(DefInit di) {
      return null;
    }

    @Override
    public Init convertValue(DagInit di) {
      return null;
    }

    @Override
    public Init convertValue(TypedInit ti) {
      if (ti.getType().typeIsConvertiableTo(this))
        return ti;
      return null;
    }

    @Override
    public Init convertValue(VarInit vi) {
      return super.convertValue(vi);
    }

    @Override
    public Init convertValue(FieldInit fi) {
      return super.convertValue(fi);
    }

    @Override
    public boolean baseClassOf(BitRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(BitsRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(IntRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(StringRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(ListRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(CodeRecTy rhs) {
      return true;
    }

    @Override
    public boolean baseClassOf(DagRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(RecordRecTy rhs) {
      return false;
    }
  }

  /**
   * Represent a dag fragment.
   */
  public static class DagRecTy extends RecTy {
    @Override
    public void print(PrintStream os) {
      os.print("dag");
    }

    @Override
    public boolean typeIsConvertiableTo(RecTy rhs) {
      return rhs.baseClassOf(this);
    }

    @Override
    public Init convertValue(UnsetInit ui) {
      return ui;
    }

    @Override
    public Init convertValue(BitInit bi) {
      return null;
    }

    @Override
    public Init convertValue(BitsInit bi) {
      return null;
    }

    @Override
    public Init convertValue(IntInit ii) {
      return null;
    }

    @Override
    public Init convertValue(StringInit si) {
      return null;
    }

    @Override
    public Init convertValue(ListInit li) {
      return null;
    }

    public Init convertValue(UnOpInit ui) {
      if (ui.getOpcode() == UnOpInit.UnaryOp.CAST) {
        Init init = ui.getOperand().convertInitializerTo(this);
        if (init == null) return null;
        if (!init.equals(ui.getOperand()))
          return new UnOpInit(UnOpInit.UnaryOp.CAST, init, new DagRecTy());
        return ui;
      }
      return null;
    }

    @Override
    public Init convertValue(BinOpInit ui) {
      if (ui.getOpcode() == BinOpInit.BinaryOp.CONCAT) {
        Init lhs = ui.getLhs().convertInitializerTo(this);
        Init rhs = ui.getRhs().convertInitializerTo(this);
        if (lhs == null || rhs == null) return null;
        if (!lhs.equals(ui.getLhs()) || !rhs.equals(ui.getRhs()))
          return new BinOpInit(BinOpInit.BinaryOp.CONCAT, lhs, rhs, new DagRecTy());
        return ui;
      }
      return null;
    }

    @Override
    public Init convertValue(CodeInit ci) {
      return null;
    }

    @Override
    public Init convertValue(VarBitInit vb) {
      return null;
    }

    @Override
    public Init convertValue(DefInit di) {
      return null;
    }

    @Override
    public Init convertValue(DagInit di) {
      return di;
    }

    @Override
    public Init convertValue(TypedInit ti) {
      if (ti.getType().typeIsConvertiableTo(this))
        return ti;
      return null;
    }

    @Override
    public Init convertValue(VarInit vi) {
      return super.convertValue(vi);
    }

    @Override
    public Init convertValue(FieldInit fi) {
      return super.convertValue(fi);
    }

    @Override
    public boolean baseClassOf(BitRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(BitsRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(IntRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(StringRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(ListRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(CodeRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(DagRecTy rhs) {
      return true;
    }

    @Override
    public boolean baseClassOf(RecordRecTy rhs) {
      return false;
    }
  }

  /**
   * '[classname]' - Represent an instance of a class, such as:
   * (R32 X = EAX).
   */
  public static class RecordRecTy extends RecTy {
    private Record rec;

    public RecordRecTy(Record r) {
      rec = r;
    }

    public Record getRecord() {
      return rec;
    }

    @Override
    public void print(PrintStream os) {
      os.print(rec.getName());
    }

    @Override
    public boolean typeIsConvertiableTo(RecTy rhs) {
      return rhs.baseClassOf(this);
    }

    @Override
    public Init convertValue(UnsetInit ui) {
      return ui;
    }

    @Override
    public Init convertValue(BitInit bi) {
      return null;
    }

    @Override
    public Init convertValue(BitsInit bi) {
      return null;
    }

    @Override
    public Init convertValue(IntInit ii) {
      return null;
    }

    @Override
    public Init convertValue(StringInit si) {
      return null;
    }

    @Override
    public Init convertValue(ListInit li) {
      return null;
    }

    @Override
    public Init convertValue(BinOpInit ui) {
      return null;
    }

    @Override
    public Init convertValue(CodeInit ci) {
      return null;
    }

    @Override
    public Init convertValue(VarBitInit vb) {
      return null;
    }

    @Override
    public Init convertValue(DefInit di) {
      // Ensure that DI is a subclass of Rec.
      if (!di.getDef().isSubClassOf(rec))
        return null;
      return di;
    }

    @Override
    public Init convertValue(DagInit di) {
      return null;
    }

    @Override
    public Init convertValue(TypedInit ti) {
      if (ti.getType() instanceof RecordRecTy) {
        RecordRecTy rrty = (RecordRecTy) ti.getType();
        if (rrty.getRecord().isSubClassOf(getRecord())
            || rrty.getRecord() == getRecord())
          return ti;
      }
      return null;
    }

    @Override
    public Init convertValue(VarInit vi) {
      return super.convertValue(vi);
    }

    @Override
    public Init convertValue(FieldInit fi) {
      return super.convertValue(fi);
    }

    @Override
    public boolean baseClassOf(BitRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(BitsRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(IntRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(StringRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(ListRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(CodeRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(DagRecTy rhs) {
      return false;
    }

    @Override
    public boolean baseClassOf(RecordRecTy rhs) {
      if (rec == rhs.getRecord() || rhs.getRecord().isSubClassOf(rec))
        return true;

      ArrayList<Record> scs = rec.getSuperClasses();
      for (Record rc : scs) {
        if (rhs.getRecord().isSubClassOf(rc))
          return true;
      }
      return false;
    }
  }
}
