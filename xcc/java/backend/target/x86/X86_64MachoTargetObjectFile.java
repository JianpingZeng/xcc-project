/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.target.x86;

import backend.codegen.MachineModuleInfo;
import backend.mc.MCExpr;
import backend.mc.MCStreamer;
import backend.support.NameMangler;
import backend.value.GlobalValue;

public class X86_64MachoTargetObjectFile extends TargetLoweringObjectFileMachO {
  public X86_64MachoTargetObjectFile() {
    super();
  }

  @Override
  public MCExpr getExprForDwarfGlobalReference(GlobalValue gv,
                                               NameMangler mangler,
                                               MachineModuleInfo mmi,
                                               int encoding,
                                               MCStreamer streamer) {
    return super.getExprForDwarfGlobalReference(gv, mangler, mmi, encoding, streamer);
  }
}
