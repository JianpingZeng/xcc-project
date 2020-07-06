package backend.llReader;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.bitcode.BitcodeUtil;
import backend.bitcode.reader.BitcodeReader;
import backend.io.ByteSequence;
import backend.support.LLVMContext;
import backend.value.Module;
import tools.MemoryBuffer;
import tools.OutRef;
import tools.SMDiagnostic;
import tools.SourceMgr;
import tools.SourceMgr.SMLoc;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class Parser {
  public static Module parseAssemblyFile(String filename, OutRef<SMDiagnostic> diag,
                                         LLVMContext ctx) {
    MemoryBuffer f = MemoryBuffer.getFileOrSTDIN(filename);
    SourceMgr srcMgr = new SourceMgr();
    if (f == null) {
      diag.set(srcMgr.getMessage(new SMLoc(),
          String.format("Could not open input file '%s'\n", filename),
          SourceMgr.DiagKind.DK_Error));
      return null;
    }

    if (BitcodeUtil.isBitcode(ByteSequence.create(f))) {
      OutRef<String> errorMsg = new OutRef<>("");
      Module m = BitcodeReader.parseBitcodeFile(f, errorMsg, ctx);
      if (m == null) {
        System.err.print("error on reading input bitcode file");
        if (!errorMsg.get().isEmpty())
          System.err.printf(", '%s'", errorMsg.get());
        System.err.println();
        System.exit(1);
      }
      return m;
    }

    srcMgr.addNewSourceBuffer(f, new SMLoc());
    Module m = new Module(filename, ctx);
    LLParser parser = new LLParser(f, srcMgr, m, diag, ctx);
    if (parser.run()) {
      return null;
    }
    return m;
  }
}
