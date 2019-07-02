package cfe.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import backend.value.Value;
import cfe.sema.ASTContext;
import cfe.type.QualType;

/**
 * Target specific hooks for defining how a type should be passed or
 * returned from functions.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface ABIInfo {
  void computeInfo(CodeGenTypes.CGFunctionInfo fi, ASTContext ctx);

  /**
   * Emit the target dependent code to load a value of type {@code ty} from
   * the va_list pointed by {@code vaListAddr}.
   *
   * @param vaListAddr
   * @param ty
   * @param cgf
   * @return
   */
  Value emitVAArg(Value vaListAddr, QualType ty, CodeGenFunction cgf);
}
