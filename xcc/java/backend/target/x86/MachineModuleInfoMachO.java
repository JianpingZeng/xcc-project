/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.target.x86;

import backend.codegen.MachineModuleInfo;
import backend.mc.MCSymbol;
import tools.Pair;

import java.util.HashMap;

public class MachineModuleInfoMachO extends MachineModuleInfo {
  private HashMap<MCSymbol, Pair<MCSymbol, Boolean>> fnStubs;
  private HashMap<MCSymbol, Pair<MCSymbol, Boolean>> gvStubs;
  private HashMap<MCSymbol, Pair<MCSymbol, Boolean>> hiddenGVStubs;
}
