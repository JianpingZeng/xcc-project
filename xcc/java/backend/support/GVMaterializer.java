package backend.support;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.value.GlobalValue;
import backend.value.Module;
import tools.OutRef;

/**
 * This file provides an abstract interface for loading a module from external storage
 * or internet. This interface allows incremental or random access loading of functions
 * from the file, which is greatly useful for some scenarios, such as JIT compilers or
 * inter-procedural optimizations that don't need the entire program in the memory at
 * the same time.
 * @author Jianping Zeng.
 * @version 0.4
 */
public interface GVMaterializer {
  /**
   * Check to if the specified global value can be materialized from whatever
   * back store this GVMaterializer uses and has not been materialized yet.
   * @param gv
   * @return
   */
  boolean isMaterializable(GlobalValue gv);

  /**
   * Check to see if the specified global value has been materialized and can
   * be dematerialized back to the whatever backing store this GVMaterializer uses.
   * @param gv
   * @return
   */
  boolean isDematerializable(GlobalValue gv);

  /**
   * Make sure the given global value is completely read. If the global value is
   * corrupted, this function returns true and carry the error information in the
   * second argument. If the Materializer don't support this operation, it does nothing.
   * @param gv
   * @param errInfo
   * @return
   */
  boolean materialize(GlobalValue gv, OutRef<String> errInfo);

  /**
   * @param gv
   */
  void dematerialize(GlobalValue gv);

  /**
   * Make sure the module has been fully read. On error, this returns true and fill
   * up the second argument with some useful description information.
   * If it succeed, return false.
   * @param m
   * @param errInfo
   * @return
   */
  boolean materializeModule(Module m, OutRef<String> errInfo);
}
