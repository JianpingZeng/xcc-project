/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

/**
 * This package encloses some important classes designed for implementing an
 * advancing linear scan register allocator with live internal splitting.
 * <p>
 * If you want to understand more details about this allocator, please
 * refer to the following papers:
 * <pre>
 * 1. Linear Scan Register Allocation for the Java HotSpot™ Client Compiler, Christian Wimmer.
 * 2. Kotzmann, Thomas, et al. "Design of the Java HotSpot™ client compiler for Java 6." ACM
 *    Transactions on Architecture and Code Optimization (TACO) 5.1 (2008): 7.
 * 3. Wimmer, Christian, and Hanspeter Mössenböck. "Optimized interval splitting in a linear scan register allocator."
 *    Proceedings of the 1st ACM/USENIX international conference on Virtual execution environments. ACM, 2005.
 * 4. Wimmer, Christian, and Michael Franz. "Linear scan register allocation on SSA form."
 *    Proceedings of the 8th annual IEEE/ACM international symposium on Code generation and optimization. ACM, 2010.
 * </pre>
 *
 * @author Jianping Zeng
 * @version 0.4
 */
package backend.codegen.linearscan;