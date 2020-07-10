package utils.tablegen;
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

import tools.Error;
import tools.Util;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class PseudoLoweringEmitter extends TableGenBackend {
  private enum MapKind { Operand, Imm, Reg }
  private static class OpData {
    private MapKind kind;
    private Object data;
    OpData(MapKind kind, int operandNo) {
      this.kind = kind;
      data = operandNo;
    }
    OpData(MapKind kind, long imm) {
      this.kind = kind;
      data = imm;
    }
    OpData(MapKind kind, Record reg) {
      this.kind = kind;
      data = reg;
    }

    int getOperand() { return (int)data; }
    long getImm() { return (long)data; }
    Record getReg() { return (Record)data; }
  }
  private static class PseudoExpansion {
    // the source pseudo instruction definition.
    CodeGenInstruction source;
    // the destinaion instruction to lower to.
    CodeGenInstruction dest;
    TreeMap<Integer, OpData> operandMap;

    PseudoExpansion(CodeGenInstruction src, CodeGenInstruction dst, TreeMap<Integer, OpData> map) {
      source = src;
      dest = dst;
      operandMap = map;
    }
  }

  private RecordKeeper records;
  private CodeGenTarget target;
  private ArrayList<PseudoExpansion> expansions;

  public PseudoLoweringEmitter(RecordKeeper recordKeeper) {
    records = recordKeeper;
    target = new CodeGenTarget(records);
    expansions = new ArrayList<>();
  }

  @Override
  public void run(String outputFile) throws Exception {
    Record expansionCLass = records.getClass("PseudoInstExpansion");
    Record instructionClass = records.getClass("PseudoInstExpansion");
    Util.assertion(expansionCLass != null && instructionClass != null, "class definition missed!");

    ArrayList<Record> insts = new ArrayList<>();
    records.getDefs().forEach((key, value)-> {
      if (value.isSubClassOf(expansionCLass) && value.isSubClassOf(instructionClass))
        insts.add(value);
    });

    // process the pseudo expansion definitions, validating them as we do it.
    insts.forEach(this::evaluateExpansion);

    // generate expansion code to lower the pseudo instruction to an MCInst of the real instruction.
    try (PrintStream os = outputFile.equals("-") ? System.out :
        new PrintStream(new FileOutputStream(outputFile))) {
      String className = target.getName() + "GenMCPseudoLowering";
      emitLoweringEmitter(os, className);
    }
  }

  private void evaluateExpansion(Record rec) {
    if (Util.DEBUG)
      System.err.printf("Pseudo definition: %s\n", rec.getName());

    Init.DagInit dag = rec.getValueAsDag("ResultInst");
    Util.assertion(dag != null, "missing result instruction in pseudo expansion!");
    if (!(dag.getOperator() instanceof Init.DefInit))
      Error.printFatalError(rec.getLoc(), String.format("%s has unexpected operator type!", rec.getName()));

    Init.DefInit opDef = (Init.DefInit) dag.getOperator();
    Record operator = opDef.getDef();
    if (!operator.isSubClassOf("Instruction"))
      Error.printFatalError(rec.getLoc(), String.format("Pseudo result '%s' is not an instruction", operator.getName()));

    CodeGenInstruction inst = new CodeGenInstruction(operator);

    if (inst.isPseudo || inst.isCodeGenOnly)
      Error.printFatalError(rec.getLoc(),
          String.format("Pseudo result '%s' can't be another instruction", operator.getName()));

    if (inst.operandList.size() != dag.getNumArgs())
      Error.printFatalError(rec.getLoc(), String.format("Pseudo result '%s' operand count mismatch", operator.getName()));

    int numOps = 0;
    for (CodeGenInstruction.OperandInfo oi : inst.operandList)
      numOps += oi.miNumOperands;

    TreeMap<Integer, OpData> operandMap = new TreeMap<>();
    addDagOperandMapping(rec, dag, inst, operandMap, 0);

    // If there are more operands that weren't in the DAG, they have to
    // be operands that have default values, or we have an error. Currently,
    // PredicateOperand and OptionalDefOperand both have default values.


    // Validate that each result pattern argument has a matching (by name)
    // argument in the source instruction, in either the (outs) or (ins) list.
    // Also check that the type of the arguments match.
    //
    // Record the mapping of the source to result arguments for use by
    // the lowering emitter.
    CodeGenInstruction source = new CodeGenInstruction(rec);
    TreeMap<String, Integer> sourceOperands = new TreeMap<>();
    for (int i = 0, e = source.operandList.size(); i < e; ++i)
      sourceOperands.put(source.operandList.get(i).name, i);

    if (Util.DEBUG)
      System.err.println("  Operand mapping:");

    for (int i = 0, e = inst.operandList.size(); i < e; ++i) {
      if (operandMap.get(inst.operandList.get(i).miOperandNo).kind != MapKind.Operand)
        continue;

      if (!sourceOperands.containsKey(dag.getArgName(i))) {
        Error.printFatalError(rec.getLoc(),
            String.format("Pseudo output operand '%s' has no matching source operand", dag.getName()));
      }

      int sourceOpNo = sourceOperands.get(dag.getArgName(i));
      // Map the source operand to the destination operand index for each
      // MachineInstr operand.
      for (int j = 0, sz = (int) inst.operandList.get(i).miNumOperands; j < sz; ++j) {
        operandMap.get(inst.operandList.get(i).miOperandNo + j).data = sourceOpNo;
        if (Util.DEBUG)
          System.err.printf("    %d ==> %d\n", sourceOpNo, i);
      }
    }
    expansions.add(new PseudoExpansion(source, inst, operandMap));
  }

  private int addDagOperandMapping(Record rec, Init.DagInit dag, CodeGenInstruction inst,
                                   TreeMap<Integer, OpData> operandMap, int baseIdx) {
    int opsAdded = 0;
    for (int i = 0, e = dag.getNumArgs(); i< e; ++i) {
      if (dag.getArg(i) instanceof Init.DefInit) {
        Init.DefInit di = (Init.DefInit) dag.getArg(i);
        // Physical register reference. Explicit check for the special case
        // "zero_reg" definition.
        if (di.getDef().isSubClassOf("Register") || di.getDef().getName().equals("zero_reg")) {
          operandMap.put(baseIdx + i, new OpData(MapKind.Reg, di.getDef()));
          ++opsAdded;
          continue;
        }

        // Normal operands should always have the same type, or we have a
        // problem.
        Util.assertion(baseIdx == 0, "Named subargument in pseud expansion!");
        if (!di.getDef().equals(inst.operandList.get(baseIdx + i).rec))
          Error.printFatalError(rec.getLoc(), String.format("Pseudo operand type '%s' doesn't match expansion operand type '%s'",
                  di.getDef().getName(),
                  inst.operandList.get(baseIdx + i).rec.getName()));

        for (int j = 0, sz = inst.operandList.size(); j < sz; ++j)
          operandMap.put(baseIdx + i + j, new OpData(MapKind.Operand, -1));
        opsAdded += inst.operandList.size();
      }
      else if (dag.getArg(i) instanceof Init.IntInit) {
        Init.IntInit ii = (Init.IntInit) dag.getArg(i);
        operandMap.put(baseIdx + i, new OpData(MapKind.Imm, ii.getValue()));
        ++opsAdded;
      }
      else if (dag.getArg(i) instanceof Init.DagInit) {
        // Just add the operands recursively. This is almost certainly
        // a constant value for a complex operand (> 1 MI operand).
        Init.DagInit subdag = (Init.DagInit) dag.getArg(i);
        int newOps = addDagOperandMapping(rec, subdag, inst, operandMap, baseIdx + i);
        opsAdded += newOps;
        baseIdx += newOps - 1;
      }
      else
        Util.shouldNotReachHere("Unhandled pseudo expansion argument type!");
    }
    return opsAdded;
  }

  private void emitLoweringEmitter(PrintStream os, String className) {
    // emit a file header.
    emitSourceFileHeaderComment("Pseudo-instruction MC lowering Source Fragment", os);

    os.println("package backend.target.arm;");
    os.println("import backend.codegen.MachineInstr;");
    os.println("import backend.mc.MCInst;");
    os.println("import backend.mc.MCOperand;");
    os.println("import backend.mc.MCStreamer;");

    os.printf("public class %s {\n", className);
    os.println();
    os.printf("\tstatic boolean emitPseudoExpansionLowering(MCStreamer os, MachineInstr mi, %sAsmPrinter printer) {\n",
        target.getName());
    os.println("\t\tswitch(mi.getOpcode()) {");
    os.println("\t\t\tdefault: return false;");
    for (PseudoExpansion expansion : expansions) {
      CodeGenInstruction source = expansion.source;
      CodeGenInstruction dest = expansion.dest;
      os.printf("\t\t\tcase %sGenInstrNames.%s: {\n", source.namespace, source.theDef.getName());
      os.println("\t\t\t\tMCInst tmpInst = new MCInst();");
      os.println("\t\t\t\tMCOperand mcop;");
      os.printf("\t\t\t\ttmpInst.setOpcode(%sGenInstrNames.%s);\n", dest.namespace, dest.theDef.getName());

      // copy the operands from the source instruction.
      int miOpNo = 0;
      for (int opNo = 0, e = dest.operandList.size(); opNo < e; ++opNo) {
        os.printf("\t\t\t\t// Operand: %s\n", dest.operandList.get(opNo).name);
        for (int i = 0, sz = (int) dest.operandList.get(opNo).miNumOperands; i < sz; ++i) {
          switch (expansion.operandMap.get(miOpNo+i).kind) {
            default:
              Util.shouldNotReachHere("Unknown operand type?");
              break;
            case Operand:
              os.printf("\t\t\t\tmcop = printer.lowerOperand(mi.getOperand(%d));\n",
                  source.operandList.get(expansion.operandMap.get(miOpNo).getOperand()).miOperandNo + i);
              os.println("\t\t\t\ttmpInst.addOperand(mcop);");
              break;
            case Reg:
              Record reg = expansion.operandMap.get(miOpNo+i).getReg();
              String regName = "0";
              if (!reg.getName().equals("zero_reg"))
                regName = reg.getValueAsString("Namespace") + "GenRegisterNames."+reg.getName();

              os.printf("\t\t\t\ttmpInst.addOperand(MCOperand.createReg(%s));\n", regName);
              break;
            case Imm:
              os.printf("\t\t\t\ttmpInst.addOperand(MCOperand.createImm(%d));\n",
                  expansion.operandMap.get(miOpNo+i).getImm());
              break;
          }
        }
        miOpNo += dest.operandList.get(opNo).miNumOperands;
      }

      if (dest.isVariadic) {
        os.println("\t\t\t\t// variable_ops");
        os.printf("\t\t\t\tfor (int i = %d, e = mi.getNumOperands(); i < e; ++i) {\n", miOpNo);
        os.println("\t\t\t\t\tmcop = printer.lowerOperand(mi.getOperand(i));");
        os.println("\t\t\t\t\tif (mcop != null) tmpInst.addOperand(mcop);");
        os.println("\t\t\t\t}");
      }

      os.println("\t\t\t\tos.emitInstruction(tmpInst);");
      os.println("\t\t\t\tbreak;");
      os.println("\t\t\t}");
    }

    os.println("\t\t}");
    os.println("\t\treturn true;");
    os.println("\t}");
    os.println("}");
  }
}
