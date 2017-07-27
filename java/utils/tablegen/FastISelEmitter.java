package utils.tablegen;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.codegen.MVT;
import utils.tablegen.Init.DefInit;
import utils.tablegen.Init.IntInit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;

/**
 * The top leve class which coordinates construction and emission of the
 * instruction selector.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class FastISelEmitter extends TableGenBackend
{
    private RecordKeeper records;
    private CodeGenDAGPatterns cgp;
    private String className;

    public FastISelEmitter(RecordKeeper rec) throws Exception
    {
        records = rec;
        cgp = new CodeGenDAGPatterns(records);
    }

    /**
     * Output the isel, returning true on failure.
     * @param outfile   The output file where fast isel sources code will be written.
     */
    public void run(String outfile) throws Exception
    {
        try(PrintStream os = !outfile.equals("-") ?
                new PrintStream(new FileOutputStream(outfile)) :
                System.out)
        {
            CodeGenTarget target = cgp.getTarget();
            os.println("package backend.target.x86;");

            emitSourceFileHeaderComment("\"Fast\" Instruction Selector for the "
                    + target.getName() + " target", os);

            className = target.getName() + "GenFastISel";

            os.println("import backend.codegen.MVT;\n"
                    + "import backend.codegen.MachineBasicBlock;\n"
                    + "import backend.codegen.MachineFunction;\n"
                    + "import backend.codegen.MachineModuleInfo;\n"
                    + "import backend.codegen.selectDAG.ISD;\n"
                    + "import backend.value.BasicBlock;\n"
                    + "import backend.value.Instruction;\n"
                    + "import backend.value.Value;\n"
                    + "import gnu.trove.map.hash.TObjectIntHashMap;\n" + "\n"
                    + "import java.util.HashMap;\n" + "\n"
                    + "import static backend.target.x86.X86GenInstrNames.*;\n"
                    + "import static backend.target.x86.X86GenRegisterInfo.*;\n"
                    + "import static backend.target.x86.X86GenRegisterNames.AL;");
            os.println();
            os.printf("public final class %s extends X86FastISel {\n\n", className);

            os.println("public X86GenFastISel(MachineFunction mf, MachineModuleInfo mmi,\n"
                    + "            TObjectIntHashMap<Value> vm,\n"
                    + "            HashMap<BasicBlock, MachineBasicBlock> bm,\n"
                    + "            TObjectIntHashMap<Instruction.AllocaInst> am)\n"
                    + "    {\n" + "        super(mf, mmi, vm, bm, am);\n"
                    + "    }");
            FastISelMap f = new FastISelMap();
            f.collectPatterns(cgp);
            f.printFunctionDefinitions(os);

            os.println("}");
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * This class holds additional information about an
     * instruction needed to emit code for it.
     */
    public static class InstructionMemo
    {
        public String name = "";
        public CodeGenRegisterClass rc;
        public byte subRegNo = 0;
        public ArrayList<String> physRegs;
    }

    /**
     * This class holds a description of a list of operand
     * types. It has utility methods for emitting text based on the operands.
     */
    public static class OperandsSignature implements Comparable<OperandsSignature>
    {
        public ArrayList<String> operands = new ArrayList<>();

        public boolean isEmpty()
        {
            return operands.isEmpty();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            if (this == obj)
                return true;
            if (getClass() != obj.getClass())
                return false;
            OperandsSignature os = (OperandsSignature)obj;
            return operands.equals(os.operands);
        }

        @Override
        public int hashCode()
        {
            // FIXME 调试的时候在collectPatterns出现的Duplicate Pattern Assert错误是
            // 由该函数计算HashCode时的重复性带来的。
            // 此前的计算方式为： operands.hashCode() << 11 + operands.size();
            // operands.size() << 11 + operands.hashCode();
            // 两者都不行。
            return operands.hashCode();
        }

        /**
         * Examine the given pattern and initialize the contents
         * of the Operands array accordingly. Return true if all the operands
         * are supported, false otherwise.
         * @param instPatNode
         * @param target
         * @param vt
         * @return
         */
        public boolean initialize(TreePatternNode instPatNode,
                CodeGenTarget target,
                int vt)
        {
            if (!instPatNode.isLeaf() &&
                    instPatNode.getOperator().getName().equals("imm"))
            {
                operands.add("i");
                return true;
            }

            if (!instPatNode.isLeaf() &&
                    instPatNode.getOperator().getName().equals("fpimm"))
            {
                operands.add("f");
                return true;
            }

            CodeGenRegisterClass dstRC = null;
            for (int i = 0, e = instPatNode.getNumChildren(); i != e; i++)
            {
                TreePatternNode op = instPatNode.getChild(i);
                // For now, filter out any operand with a predicate.
                if (!op.getPredicateFns().isEmpty())
                    return false;

                // For now, filter out any operand with multiple values.
                if (op.getExtTypes().size() != 1)
                    return false;

                // For now, all the operands must have the same type.
                if (op.getTypeNum(0) != vt)
                    return false;

                if (!op.isLeaf())
                {
                    if (op.getOperator().getName().equals("imm"))
                    {
                        operands.add("i");
                        continue;
                    }
                    if (op.getOperator().getName().equals("fpimm"))
                    {
                        operands.add("f");
                        continue;
                    }
                    // For now, ignore other non-leaf nodes.
                    return false;
                }

                if (!(op.getLeafValue() instanceof DefInit))
                    return false;

                DefInit opDI = (DefInit)op.getLeafValue();
                Record opLeafRec = opDI.getDef();

                CodeGenRegisterClass rc = null;
                if (opLeafRec.isSubClassOf("RegisterClass"))
                    rc = target.getRegisterClass(opLeafRec);
                else if (opLeafRec.isSubClassOf("Register"))
                    rc = target.getRegisterClassForRegister(opLeafRec);
                else
                    return false;

                // For now, require the register operands' register classes to all
                // be the same.
                if (rc == null)
                    return false;

                // For now, all the operands must have the same register class.
                if (dstRC != null)
                {
                    if (!dstRC.equals(rc))
                        return false;
                }
                else
                    dstRC = rc;
                operands.add("r");
            }
            return true;
        }

        public void printParameters(PrintStream os)
        {
            for (int i = 0, e = operands.size(); i != e; i++)
            {
                if (operands.get(i).equals("r"))
                    os.printf("int op%d", i);
                else if (operands.get(i).equals("i"))
                    os.printf("long imm%d", i);
                else if(operands.get(i).equals("f"))
                    os.printf("ConstantFP f%d", i);
                else
                {
                    assert false:"Unknown operand kind";
                    System.exit(1);
                }
                if(i < e - 1)
                    os.printf(", ");
            }
        }

        public void printArguments(PrintStream os, ArrayList<String> pr)
        {
            assert pr.size() == operands.size();
            boolean printedArg = false;
            for (int i = 0, e = operands.size(); i != e; i++)
            {
                if (!pr.get(i).equals(""))
                    continue;
                if (printedArg)
                    os.print(", ");
                switch (operands.get(i))
                {
                    case "r":
                        os.printf("op%d", i);
                        printedArg = true;
                        break;
                    case "i":
                        os.printf("imm%d", i);
                        printedArg = true;
                        break;
                    case "f":
                        os.printf("f%d", i);
                        printedArg = true;
                        break;
                    default:
                        assert false:"Unknown operand kind";
                        System.exit(1);
                }
            }
        }

        public void printArguments(PrintStream os)
        {
            for (int i = 0, e = operands.size(); i != e; i++)
            {
                switch (operands.get(i))
                {
                    case "r":
                        os.printf("op%d", i);
                        break;
                    case "i":
                        os.printf("imm%d", i);
                        break;
                    case "f":
                        os.printf("f%d", i);
                        break;
                    default:
                        assert false:"Unknown operand kind";
                        System.exit(1);
                }
                if (i < e - 1)
                    os.print(", ");
            }
        }

        public void printManglingSuffix(PrintStream os, ArrayList<String> pr)
        {
            for (int i = 0, e = operands.size(); i != e; i++)
            {
                if (!pr.get(i).equals(""))
                {
                    // Implicit physical register operand. e.g. Instruction::Mul expect to
                    // select to a binary op. On x86, mul may take a single operand with
                    // the other operand being implicit. We must emit something that looks
                    // like a binary instruction except for the very inner FastEmitInst_*
                    // call.
                    continue;
                }

                os.printf(operands.get(i));
            }
        }

        public void printManglingSuffix(PrintStream os)
        {
            operands.forEach(os::print);
        }

        @Override
        public int compareTo(OperandsSignature o)
        {
            int i = 0, j = 0;
            int sz1 = operands.size(), sz2 = o.operands.size();
            for (; i != sz1 && j != sz2; j++, i++)
            {
                int cmpRes = operands.get(i).compareTo(o.operands.get(j));
                if (cmpRes < 0) return -1;
                if (cmpRes > 0) return 1;
            }

            if (i == sz1 && j == sz2)
                return 0;
            
            return sz1 < sz2 ? -1 : 1;
        }
    }

    public static class FastISelMap
    {
        private TreeMap<OperandsSignature, TreeMap<String, TreeMap<Integer, TreeMap<Integer, TreeMap<String, InstructionMemo>>>>> simplePatterns;

        public FastISelMap()
        {
            simplePatterns = new TreeMap<>();
        }

        public void collectPatterns(CodeGenDAGPatterns cgp) throws Exception
        {
            CodeGenTarget target = cgp.getTarget();

            int sz = cgp.getPatternsToMatch().size();
            //System.err.println(sz+"\n\n\n\n\n\n");

            for (int idx = 0; idx  < sz; idx ++)
            {
                PatternToMatch pat = cgp.getPatternsToMatch().get(idx);
                //pat.dump();
                //System.err.println();
                TreePatternNode dest = pat.getDstPattern();
                if (dest.isLeaf()) continue;

                Record op = dest.getOperator();
                if (!op.isSubClassOf("Instruction"))
                    continue;
                CodeGenInstruction ii = target.getInstruction(op.getName());
                if (ii.operandList.isEmpty())
                    continue;

                // Ignore multi-instruction pattern.
                boolean multiInstr = false;
                for (int i = 0, e = dest.getNumChildren(); i != e; i++)
                {
                    TreePatternNode childOp = dest.getChild(i);
                    if(childOp.isLeaf())
                        continue;
                    if (childOp.getOperator().isSubClassOf("Instruction"))
                    {
                        multiInstr = true;
                        break;
                    }
                }
                if (multiInstr)
                    continue;

                CodeGenRegisterClass destRc = null;
                int subRegNo = ~0;
                if (!op.getName().equals("EXTRACT_SUBREG"))
                {
                    Record op0Rec = ii.operandList.get(0).rec;
                    if (!op0Rec.isSubClassOf("RegisterClass"))
                        continue;
                    destRc = target.getRegisterClass(op0Rec);
                    if (destRc == null)
                        continue;
                }
                else
                {
                    subRegNo = (int) ((IntInit)dest.getChild(1).getLeafValue()).getValue();
                }

                // Inspect the pattern.
                TreePatternNode instPatNode = pat.getSrcPattern();
                if (instPatNode == null) continue;
                if (instPatNode.isLeaf()) continue;

                Record instPatOp = instPatNode.getOperator();
                String opcodeName = getOpcodeName(instPatOp, cgp);
                int retVT = instPatNode.getTypeNum(0);
                int vt = retVT;
                if (instPatNode.getNumChildren() != 0)
                {
                    vt = instPatNode.getChild(0).getTypeNum(0);
                }

                // For now, filter out instructions which just set a register to
                // an Operand or an immediate, like MOV32ri.
                if (instPatOp.isSubClassOf("Operand"))
                    continue;

                // For now, filter out any instructions with predicates.
                if (!instPatNode.getPredicateFns().isEmpty())
                    continue;

                // Check all the operands.
                OperandsSignature operands = new OperandsSignature();
                if (!operands.initialize(instPatNode, target, vt))
                    continue;

                /*
                if(operands.operands.size() ==1 && operands.operands.get(0).equals("i"))
                {
                    /**
                     44
                     45
                     46
                     580
                     771
                     *
                    System.err.println(idx + ", hascode=" + operands.hashCode());
                }*/

                ArrayList<String> physRegInputs = new ArrayList<>();
                if (!instPatNode.isLeaf() &&
                        (instPatNode.getOperator().getName().equals("imm") ||
                                instPatNode.getOperator().getName().equals("fpimmm")))
                {
                    physRegInputs.add("");
                }

                else if (!instPatNode.isLeaf())
                {
                    for (int i = 0, e = instPatNode.getNumChildren(); i != e; i++)
                    {
                        TreePatternNode childOp = instPatNode.getChild(i);
                        if (!childOp.isLeaf())
                        {
                            physRegInputs.add("");
                            continue;
                        }

                        DefInit opDI = (DefInit)childOp.getLeafValue();
                        Record opLeafRec = opDI.getDef();
                        String phyReg = "";
                        if (opLeafRec.isSubClassOf("Register"))
                        {
                            for (CodeGenRegister reg : target.getRegisters())
                            {
                                if (reg.theDef.equals(opLeafRec))
                                {
                                    phyReg += reg.getName();
                                    break;
                                }
                            }
                        }
                        physRegInputs.add(phyReg);
                    }
                }
                else
                {
                    physRegInputs.add("");
                }

                // Get the predicate that guards this pattern.
                String predicateCheck = pat.getPredicateCheck();

                // Ok, we found a pattern that we can handle. Remember it.
                InstructionMemo memo = new InstructionMemo();
                memo.name = pat.getDstPattern().getOperator().getName();
                memo.rc = destRc;
                memo.subRegNo = (byte)subRegNo;
                memo.physRegs = physRegInputs;

                if (!simplePatterns.containsKey(operands))
                    simplePatterns.put(operands, new TreeMap<>());
                if (!simplePatterns.get(operands).containsKey(opcodeName))
                    simplePatterns.get(operands).put(opcodeName, new TreeMap<>());
                if (!simplePatterns.get(operands).get(opcodeName).containsKey(vt))
                    simplePatterns.get(operands).get(opcodeName).put(vt, new TreeMap<>());
                if (!simplePatterns.get(operands).get(opcodeName).get(vt).containsKey(retVT))
                    simplePatterns.get(operands).get(opcodeName).get(vt).put(retVT, new TreeMap<>());

                /*
                if (idx == 771)
                {
                    for (OperandsSignature os : simplePatterns.keySet())
                    {
                        System.err.println(os.operands);
                        System.err.println("\t");
                        for (String opname : simplePatterns.get(os).keySet())
                        {
                            System.err.println("\t"+opname);
                        }
                    }
                }
                */
                // 错误原因是predicateCheck为空字符串.
                //System.out.println(predicateCheck + predicateCheck.length());
                //if (simplePatterns.get(operands).get(opcodeName).get(vt).get(retVT).containsKey(predicateCheck))
                /*{
                    System.err.println(operands);
                    System.err.println(opcodeName);
                    System.err.println(MVT.getEnumName(vt));
                    System.err.println(MVT.getEnumName(retVT));
                    System.err.println(predicateCheck);
                    System.err.println();

                    System.err.println(idx);
                    pat.dump();
                    System.err.println(operands.operands);
                    System.err.println(opcodeName);
                    System.err.println(MVT.getEnumName(vt));
                    System.err.println(MVT.getEnumName(retVT));
                    System.err.println(predicateCheck);
                }*/

                //assert !simplePatterns.get(operands).get(opcodeName).get(vt).get(retVT).containsKey(predicateCheck)
                //        : "Duplicate pattern!";
                simplePatterns.get(operands).get(opcodeName).get(vt).get(retVT).put(predicateCheck, memo);
            }
        }

        public void printClass(PrintStream os)
        {}

        public void printFunctionDefinitions(PrintStream os)
        {
            // Now emit code for all the patterns that we collected.
            // int idx = 0;

            for (Map.Entry<OperandsSignature, TreeMap<String, TreeMap<Integer, TreeMap<Integer, TreeMap<String, InstructionMemo>>>>>
                    pair : simplePatterns.entrySet())
            {
                OperandsSignature operands = pair.getKey();
                TreeMap<String, TreeMap<Integer, TreeMap<Integer, TreeMap<String, InstructionMemo>>>> otm = pair.getValue();

                // os.println("// " + (idx++));
                for (Map.Entry<String, TreeMap<Integer, TreeMap<Integer, TreeMap<String, InstructionMemo>>>> pair2 : otm.entrySet())
                {
                    String opcode = pair2.getKey();
                    TreeMap<Integer, TreeMap<Integer, TreeMap<String, InstructionMemo>>> tm = pair2.getValue();

                    os.printf("// FastEmit functions for %s.\n", opcode);
                    os.println();

                    // Emit one function for each opcode,type pair.
                    for (Map.Entry<Integer, TreeMap<Integer, TreeMap<String, InstructionMemo>>> pair3 : tm.entrySet())
                    {
                        int vt = pair3.getKey();
                        TreeMap<Integer, TreeMap<String, InstructionMemo>> rm = pair3.getValue();
                        if (rm.size() != 1)
                        {
                            rm.forEach((key, pm) ->
                            {
                                int retVT = key;
                                boolean hasPred = false;
                                os.printf("public int fastEmit_%s_%s_%s_",
                                        getLegalCName(opcode),
                                        getLegalCName(MVT.getName(vt)),
                                        getLegalCName(MVT.getName(retVT)));
                                operands.printManglingSuffix(os);
                                os.printf("(");
                                operands.printParameters(os);
                                os.printf(") {\n");

                                // Emit code for each possible instruction. There may be
                                // multiple if there are subtarget concerns.
                                for (Map.Entry<String, InstructionMemo> pi : pm
                                        .entrySet())
                                {
                                    String predicateCheck = pi.getKey();
                                    InstructionMemo memo = pi.getValue();
                                    if (predicateCheck.isEmpty())
                                    {
                                        assert !hasPred :
                                                "Multiple instructions match, at least one has "
                                                        + "a predicate and at least one doesn't!";
                                    }
                                    else
                                    {
                                        os.printf("\tif (%s) {\n\t",
                                                predicateCheck);
                                        hasPred = true;
                                    }

                                    for (int i = 0; i != memo.physRegs.size(); i++)
                                    {
                                        if (!memo.physRegs.get(i).equals(""))
                                        {
                                            os.printf(
                                                    "\ttii.copyRegToReg(mbb, mbb.size(), %s, "
                                                            + " op%d, tm.getRegisterInfo()."
                                                            + "\n\t\t\tgetPhysicalRegisterRegClass(%s), "
                                                            + "mri.getRegisterClass(op%d));\n",
                                                    memo.physRegs.get(i), i,
                                                    memo.physRegs.get(i), i);
                                        }
                                    }

                                    os.printf("\t\treturn fastEmitInst_");
                                    if (memo.subRegNo == ~0)
                                    {
                                        operands.printManglingSuffix(os,
                                                memo.physRegs);
                                        os.printf("(%s, ", memo.name);
                                        os.printf("%sRegisterClass",
                                                memo.rc.getName());
                                        if (!operands.isEmpty())
                                            os.printf(", ");
                                        operands.printArguments(os,
                                                memo.physRegs);
                                        os.printf(");\n");
                                    }
                                    else
                                    {
                                        os.printf("extractsubreg(%s",
                                                MVT.getName(retVT));
                                        os.printf(", op0, ");
                                        os.printf("%d", memo.subRegNo);
                                        os.printf(");\n");
                                    }

                                    if (hasPred)
                                        os.printf("\t}\n");
                                }

                                // Return 0 if none of the predicates were satisfied.
                                if (hasPred)
                                    os.printf("\treturn 0;\n");
                                os.printf("}\n");
                                os.println();
                            });

                            // Emit one function for the type that demultiplexes on return type.
                            os.printf("public int fastEmit_%s_%s_",
                                    getLegalCName(opcode),
                                    getLegalCName(MVT.getName(vt)));
                            operands.printManglingSuffix(os);
                            os.print("(MVT retVT");
                            if (!operands.isEmpty())
                                os.printf(", ");

                            operands.printParameters(os);
                            os.printf(") {\nswitch (retVT.simpleVT) {\n");
                            for (Map.Entry<Integer, TreeMap<String, InstructionMemo>> ri : rm
                                    .entrySet())
                            {
                                int retVT = ri.getKey();
                                os.printf("\tcase %s: return fastEmit_%s_%s_%s_",
                                        MVT.getName(retVT),
                                        getLegalCName(opcode),
                                        getLegalCName(MVT.getName(vt)),
                                        getLegalCName(MVT.getName(retVT)));
                                operands.printManglingSuffix(os);
                                os.print("(");
                                operands.printArguments(os);
                                os.printf(");\n");
                            }
                            os.printf("\tdefault: return 0;\n}\n}\n\n");
                        }
                        else
                        {
                            // Non-variadic return type.
                            os.printf("public int fastEmit_%s_%s_",
                                    getLegalCName(opcode),
                                    getLegalCName(MVT.getName(vt)));

                            operands.printManglingSuffix(os);
                            os.printf("(MVT retVT");
                            if (!operands.isEmpty())
                                os.printf(", ");
                            operands.printParameters(os);
                            os.printf(") {\n");

                            os.printf("\tif(retVT.simpleVT != %s)\n\t\treturn 0;\n",
                                    MVT.getName(rm.entrySet().iterator().next().getKey()));

                            TreeMap<String, InstructionMemo> pm = rm.entrySet().iterator().next().getValue();
                            boolean hasPred = false;

                            for(Map.Entry<String, InstructionMemo> pi: pm.entrySet())
                            {
                                String predicateCheck = pi.getKey();
                                InstructionMemo memo = pi.getValue();

                                if (predicateCheck.isEmpty())
                                {
                                    assert !hasPred :"Multiple instructions match, at least one has " +
                                            "a predicate and at least one doesn't!";
                                }
                                else
                                {
                                    os.printf("\tif (%s){\n", predicateCheck);
                                    os.printf("\t");
                                    hasPred = true;
                                }

                                for (int i = 0; i != memo.physRegs.size(); i++)
                                {
                                    if (!memo.physRegs.get(i).equals(""))
                                    {
                                        os.printf(
                                                "\ttii.copyRegToReg(mbb, mbb.size(), %s, "
                                                        + " op%d, tm.getRegisterInfo().getPhysicalRegisterRegClass(%s), "
                                                        + "mri.getRegisterClass(op%d));\n",
                                                memo.physRegs.get(i), i,
                                                memo.physRegs.get(i), i);
                                    }
                                }

                                os.printf("\treturn fastEmitInst_");

                                if (memo.subRegNo == ~0)
                                {
                                    operands.printManglingSuffix(os, memo.physRegs);
                                    os.printf("(%s, ", memo.name);
                                    os.printf("%sRegisterClass", memo.rc.getName());
                                    if (!operands.isEmpty())
                                        os.print(", ");

                                    operands.printArguments(os, memo.physRegs);
                                    os.print(");\n");
                                }
                                else
                                {
                                    os.printf("extractsubreg(retVT, op0, %d);", memo.subRegNo);
                                }

                                if (hasPred)
                                    os.print("\t}\n");
                            }

                            if (hasPred)
                                os.printf("\treturn 0;\n");
                            os.printf("}\n");
                            os.printf("\n");
                        }
                    }

                    // Emit one function for the opcode that demultiplexes based on the type.
                    os.printf("public int fastEmit_%s_", getLegalCName(opcode));
                    operands.printManglingSuffix(os);
                    os.printf("(MVT vt, MVT retVT");
                    if (!operands.isEmpty())
                        os.print(", ");

                    operands.printParameters(os);
                    os.printf(") {\n");
                    os.printf("\tswitch (vt.simpleVT) {\n");
                    tm.keySet().forEach(vt->
                    {
                        String typeName = MVT.getName(vt);
                        os.printf("\tcase %s: return fastEmit_%s_%s_",
                                typeName,
                                getLegalCName(opcode),
                                getLegalCName(typeName));
                        operands.printManglingSuffix(os);
                        os.printf("(retVT");
                        if (!operands.isEmpty())
                            os.printf(", ");
                        operands.printArguments(os);
                        os.printf(");\n");
                    });

                    os.printf("\tdefault: return 0;\n");
                    os.printf("\t}\n");
                    os.printf("}\n");
                    os.println();
                }

                os.printf("//Top level FastEmit function.\n");
                os.println();

                // FIXME hashMap没法将所有的operands合并

                // Emit one function for the operand signature that demultiplexes based
                // on opcode and type.
                os.printf("public int fastEmit_");
                operands.printManglingSuffix(os);
                os.printf("(MVT vt, MVT retVT, int opcode");
                if (!operands.isEmpty())
                    os.printf(", ");

                operands.printParameters(os);
                os.printf(") {\n");
                os.printf("\tswitch (opcode) {\n");
                otm.keySet().forEach(opcode->
                {
                    os.printf("\tcase %s: return fastEmit_%s_",
                            opcode.replaceAll("::", "\\."),
                            getLegalCName(opcode));
                    operands.printManglingSuffix(os);
                    os.printf("(vt, retVT");
                    if (!operands.isEmpty())
                        os.printf(", ");
                    operands.printArguments(os);
                    os.printf(");\n");
                });

                os.println("\tdefault: return 0;");
                os.println("\t}");
                os.println("}");
                os.println();
            }
        }
    }

    public static String getOpcodeName(Record r, CodeGenDAGPatterns cgp)
    {
        return cgp.getSDNodeInfo(r).getEnumName();
    }

    public static String getLegalCName(String originName)
    {
        return originName.replaceAll("::", "_").replaceAll("\\.", "_");
    }
}
