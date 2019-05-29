package backend.support;
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

import backend.intrinsic.Intrinsic;
import backend.type.FunctionType;
import backend.value.Function;
import backend.value.GlobalVariable;
import tools.OutRef;
import tools.Util;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class AutoUpgrade {

  public static boolean upgradeGlobalVariable(GlobalVariable gv) {
    switch (gv.getName()) {
      case ".llvm.eh.catch.all.value":
        gv.setName("llvm.eh.catch.all.value");
        return true;
    }
    return false;
  }

  public static boolean upgradeIntrinsicFunction(Function f,
                                                  OutRef<Function> newFn) {
    newFn.set(null);
    boolean upgraded = upgradeInstrinsicFunction1(f, newFn);
    if (newFn.get() != null)
      f = newFn.get();

    Intrinsic.ID id = f.getIntrinsicID();
    if (id != Intrinsic.ID.not_intrinsic)
      f.setAttributes(Intrinsic.getAttributes(id));

    return upgraded;
  }

  private static boolean upgradeInstrinsicFunction1(Function f,
                                                    OutRef<Function> newFn) {
    Util.assertion(f != null, "Illegal to upgrade a on-existent function.");

    String name = f.getName();
    FunctionType fty = f.getFunctionType();
    // quickly eliminate it, if it's not a candidate.
    if (name.length() <= 8 || !name.startsWith("llvm."))
      return false;
    Util.shouldNotReachHere("Unimplemented");
    return false;
  }
}
