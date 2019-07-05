/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package cfe.codegen;

import backend.support.LLVMContext;
import backend.value.Value;
import cfe.sema.ASTContext;
import cfe.type.QualType;

/**
 * The default implementation for ABI specific retails.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class DefaultABIInfo implements ABIInfo {
  private ABIArgInfo classifyReturnType(QualType retType) {
    if (CodeGenFunction.hasAggregateLLVMType(retType))
      return ABIArgInfo.getIndirect(0);

    return retType.isPromotableIntegerType() ? ABIArgInfo.getExtend()
        : ABIArgInfo.getDirect();
  }

  private ABIArgInfo classifyArgumentType(QualType argType) {
    if (CodeGenFunction.hasAggregateLLVMType(argType))
      return ABIArgInfo.getIndirect(0);

    return argType.isPromotableIntegerType() ? ABIArgInfo.getExtend()
        : ABIArgInfo.getDirect();
  }

  @Override
  public void computeInfo(CodeGenTypes.CGFunctionInfo fi, ASTContext ctx, LLVMContext vmContext) {
    fi.setReturnInfo(classifyReturnType(fi.getReturnType()));
    for (int i = 0, e = fi.getNumOfArgs(); i < e; i++)
      fi.setArgInfo(i, classifyArgumentType(fi.getArgInfoAt(i).type));
  }

  @Override
  public Value emitVAArg(Value vaListAddr, QualType ty, CodeGenFunction cgf, LLVMContext ctx) {
    return null;
  }
}
