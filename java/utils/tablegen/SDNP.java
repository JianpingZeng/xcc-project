package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

/**
 * SelectionDAG node properties.
 * SDNPMemOperand: indicates that a node touches memory and therefore must
 * have an associated memory operand that describes the access.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public interface SDNP {
  int SDNPCommutative = 0;
  int SDNPAssociative = 1;
  int SDNPHasChain = 2;
  int SDNPOutFlag = 3;
  int SDNPInFlag = 4;
  int SDNPOptInFlag = 5;
  int SDNPMayLoad = 6;
  int SDNPMayStore = 7;
  int SDNPSideEffect = 8;
  int SDNPMemOperand = 9;
  int SDNPVariadic = 10;
}
