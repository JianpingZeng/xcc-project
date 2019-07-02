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

package driver;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public interface TypeID {
  int TY_INVALID = -1;
  int TY_PP_C = 0;
  int TY_C = 1;
  int TY_PP_ObjC = 2;
  int TY_ObjC = 3;
  int TY_PP_CXX = 4;
  int TY_CXX = 5;
  int TY_PP_ObjCXX = 6;
  int TY_ObjCXX = 7;
  int TY_PP_CHeader = 8;
  int TY_CHeader = 9;
  int TY_PP_ObjCHeader = 10;
  int TY_ObjCHeader = 11;
  int TY_PP_CXXHeader = 12;
  int TY_CXXHeader = 13;
  int TY_PP_ObjCXXHeader = 14;
  int TY_ObjCXXHeader = 15;
  int TY_Ada = 16;
  int TY_PP_Asm = 17;
  int TY_Asm = 18;
  int TY_PP_Fortran = 19;
  int TY_Fortran = 20;
  int TY_Java = 21;
  int TY_LLVMAsm = 22;
  int TY_LLVMBC = 23;
  int TY_Plist = 24;
  int TY_PCH = 25;
  int TY_Object = 26;
  int TY_Treelang = 27;
  int TY_Image = 28;
  int TY_Dependencies = 29;
  int TY_Nothing = 30;
}
