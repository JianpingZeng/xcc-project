package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import tools.Pair;
import tools.SetMultiMap;
import tools.Util;

import java.io.PrintStream;
import java.util.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class RegisterInfoEmitter extends TableGenBackend
{
    private RecordKeeper records;
    private String targetName;
    private CodeGenTarget target;
    public RegisterInfoEmitter(RecordKeeper records) throws Exception
    {
        this.records = records;
        assert records != null;
        target = new CodeGenTarget();
        targetName = target.getName();
    }

    /**
     * Prints out all of register names as the variable name of integer type.
     * <b>Note that</b> This output source file generated by this method is
     * different with {@linkplain #run(String)}. This method just make
     * register name enumeration set.
     */
    public void runEnums(String outputFile) throws Exception
    {
        // The file path where all enum values would be written.
        String className = targetName + "GenRegisterNames";
        try (PrintStream os = outputFile.equals("-") ?
                System.out : new PrintStream(outputFile))
        {
            ArrayList<Record> registers = records.getAllDerivedDefinition("Register");
            if (registers == null || registers.isEmpty())
                throw new Exception(
                        "No 'Register' subclasses defined in td file!");

            os.println("package backend.target.x86;\n");
            emitSourceFileHeaderComment("Target Register Enum Values", os);

            int initValue = 0;
            os.printf("public interface %s \n{" + "\n\tint NoRegister = %d;\n",
                    className, initValue);
            initValue++;
            registers.sort((r1, r2)->
            {
                return r1.getName().compareTo(r2.getName());
            });
            for (Record reg : registers)
            {
                os.printf("\tint %s = %d;", reg.getName(), initValue);
                initValue++;
                os.println();
            }
            os.println("}");
        }
    }

    @Override
    public void run(String outputFile) throws Exception
    {
        // The file path where all enum values would be written.
        String className = targetName + "GenRegisterInfo";

        try (PrintStream os = outputFile.equals("-") ?
                System.out : new PrintStream(outputFile))
        {
            os.println("package backend.target.x86;\n");

            emitSourceFileHeaderComment("Register Information Source Fgrament",
                    os);

            os.println("import backend.codegen.MVT;\n"
                    + "import backend.codegen.MachineFunction;\n"
                    + "import backend.target.TargetMachine;\n"
                    + "import backend.target.TargetRegisterClass;\n"
                    + "import backend.target.TargetRegisterDesc;\n"
                    + "import backend.target.TargetRegisterInfo;\n"
                    + "import backend.codegen.EVT;\n\n"
                    + "import static backend.target.x86.X86GenRegisterNames.*;\n");


            os.printf("public abstract class %s extends TargetRegisterInfo \n{\t",
                            className);

            // generates static array for register classes.
            generateRegClassesArray(os);

            // Generates the value type for each register class.
            generateValueTypeForRegClass(os);

            generateRegisterClasses(os);

            // generates fields and nested classes.
        }
    }

    private void generateRegInfoCtr(PrintStream os)
    {

    }

    private void generateRegClassesArray(PrintStream os)
    {
        ArrayList<CodeGenRegisterClass> regClasses = target.getRegisterClasses();

        for (CodeGenRegisterClass rc : regClasses)
        {
            String name = rc.theDef.getName();

            os.printf("\n\t//%s Register Class...\n", name);
            os.printf("\tpublic static final int[] %s = {\n\t\t", name);
            for (Record r : rc.elts)
            {
                os.printf("%s, ", r.getName());
            }
            os.printf("\n\t};\n\n");
        }
    }

    private void generateValueTypeForRegClass(PrintStream os)
    {
        ArrayList<CodeGenRegisterClass> regClasses = target.getRegisterClasses();

        for (CodeGenRegisterClass rc : regClasses)
        {
            // Given the value type a legal Java name if it is anonymous.
            String name = rc.theDef.getName() + "VTs";

            os.printf("\n\t// %s Register Class Value Type...\n", name);
            os.printf("\tpublic static final EVT[] %s = {\n\t\t", name);
            for (int i = 0; i < rc.vts.size(); i++)
                os.printf("new EVT(new MVT(%s)), ", MVT.getEnumName((rc.vts.get(i))));

            os.print("new EVT(new MVT(MVT.Other))\n\t};\n\n");
        }
    }

    /**
     * Sorting predicate to sort record by name.
     */
    public static Comparator<Record> LessRecord = new Comparator<Record>()
    {
        @Override
        public int compare(Record o1, Record o2)
        {
            return o1.getName().compareTo(o2.getName());
        }
    };

    /**
     * Sorting predicate to sort the record by theire name field.
     */
    private static class LessRecordFieldName implements Comparator<Record>
    {
        @Override
        public int compare(Record o1, Record o2)
        {
            try
            {
                return o1.getValueAsString("Name").compareTo(o2.getValueAsString("Name"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return 0;
            }
        }
    }

    private void generateRegisterClasses(PrintStream os) throws Exception
    {
        ArrayList<CodeGenRegisterClass> regClasses = target.getRegisterClasses();

        // Output the register class ID.
        int idx = 0;
        os.println("\n\t// Defines the Register Class ID.");
        for (CodeGenRegisterClass rc : regClasses)
        {
            os.printf("\tpublic static final int %sRegClassID", rc.getName());
            os.printf(" = %d;\n", (idx+1));
            ++idx;
        }

        os.printf("\n\n");

        os.println("\t// Register Class declaration");
        for (CodeGenRegisterClass rc : regClasses)
        {
            String name = rc.getName();

            // defines a outer static variable.
            os.printf("\tpublic final static %sClass "
                    + "%sRegisterClass = %sClass.getInstance();\n", name, name, name);
        }

        os.println();
        // print a static code block to initialize register class instance.
        os.printf("\t{\n");
        for (CodeGenRegisterClass rc : regClasses)
        {
            String name = rc.getName();
            os.printf("\t\t// Register class initialization for %sRegisterClass.%n", name);
            os.printf("\t\t%sRegisterClass.setSubClasses(%sSubclasses);%n", name, name);
            os.printf("\t\t%sRegisterClass.setSuperClasses(%sSuperclasses);%n", name, name);
            os.printf("\t\t%sRegisterClass.setSubRegClasses(%sSubRegClasses);%n", name, name);
            os.printf("\t\t%sRegisterClass.setSuperRegClasses(%sSuperRegClasses);%n", name, name);
            os.println();
        }
        os.printf("\t}\n");

        // Output register class define.
        for (CodeGenRegisterClass rc : regClasses)
        {
            String name = rc.getName();
            // output the register class definition.
            os.printf("\n\tpublic final static class %sClass extends TargetRegisterClass\n\t{",
                    name);

            os.println("\n\t\t// Only allow one instance for this class.");
            os.printf("\n\t\tprivate static %sClass instance = new %sClass();\n", name, name);
            os.printf("\n\t\tpublic static %sClass getInstance() { return instance;}\n", name);

            os.printf("\n\t\tprivate %sClass()\n\t\t{\n\t\t\t "
                            + "super(%sRegClassID, \"%s\", %sVTs, %sSubclasses, \n"
                            + "\t\t\t%sSuperclasses, %sSubRegClasses, %sSuperRegClasses, \n"
                            + "\t\t\t%d, %d, %d, %s); \n\t\t}\n",
                    name, name, name, name, name,
                    name,
                    name,
                    name,
                    rc.spillSize/8,
                    rc.spillAlignment/8,
                    rc.copyCost,
                    rc.getName());

            os.println(rc.methodBodies);
            os.println("\n\t}");
        }

        // Emit the sub-register classes for each RegisterClass.
        TIntObjectHashMap<TIntHashSet> superClassMap = new TIntObjectHashMap<>();
        TIntObjectHashMap<TIntHashSet> superRegClassMap = new TIntObjectHashMap<>();

        os.println();
        for (int k = 0; k < regClasses.size(); k++)
        {
            CodeGenRegisterClass rc = regClasses.get(k);
            String name = rc.theDef.getName();

            os.printf("\n\t// %s Sub-register Classes...\n", name);
            os.printf("\tpublic static final TargetRegisterClass[] %sSubRegClasses = {\n\t\t", name);

            boolean empty = true;

            for (int j = 0, e = rc.subRegClasses.size(); j < e; ++j)
            {
                Record subReg = rc.subRegClasses.get(j);
                int i = 0, e2 = regClasses.size();
                for (; i != e2; ++i)
                {
                    CodeGenRegisterClass rc2 = regClasses.get(i);
                    if (subReg.getName().equals(rc2.getName()))
                    {
                        if (!empty)
                            os.printf(", ");
                        os.printf("%sRegisterClass", rc2.theDef.getName());
                        empty = false;

                        if (!superRegClassMap.containsKey(i))
                        {
                            superRegClassMap.put(i, new TIntHashSet());
                        }

                        superRegClassMap.get(i).add(k);
                        break;
                    }
                }
                if (i == e2)
                {
                    throw new Exception("Register Class member '" + subReg.getName() +
                        "' is not a valid RegisterClass!");
                }
            }

            os.print(empty ? "" : ", ");
            //os.print("null");
            os.print("\n\t};\n");
        }

        // Emit the super-register classes for each RegisterClass.
        for (int i = 0, e = regClasses.size(); i < e; ++i)
        {
            CodeGenRegisterClass rc = regClasses.get(i);

            String name = rc.theDef.getName();
            os.printf("\n\t// %s Super-register Classes...\n", name);
            os.printf("\tpublic static final TargetRegisterClass[] %sSuperRegClasses = {\n\t\t", name);

            boolean empty = true;
            if (superRegClassMap.containsKey(i))
            {
                for (int val : superRegClassMap.get(i).toArray())
                {
                    CodeGenRegisterClass rc2 = regClasses.get(val);
                    if (!empty)
                        os.print(", ");
                    os.printf("%sRegisterClass", rc2.theDef.getName());
                    empty = false;
                }
            }

            os.printf("%s", empty ? "":", ");
            os.print("\n\t};\n");
        }

        // Emit the sub-classes array for each RegisterClass
        for (int i = 0, e = regClasses.size(); i < e; ++i)
        {
            CodeGenRegisterClass rc = regClasses.get(i);

            String name = rc.theDef.getName();

            HashSet<Record> regSets = new HashSet<>();
            regSets.addAll(rc.elts);

            os.printf("\t// %s Register Class sub-classes...\n", name);
            os.printf("\tpublic static final TargetRegisterClass[] %sSubclasses = {\n\t\t", name);

            boolean empty = true;
            for (int j = 0, e2 = regClasses.size(); j != e2; ++j)
            {
                CodeGenRegisterClass rc2 = regClasses.get(j);

                // RC2 is a sub-class of RC if it is a valid replacement for any
                // instruction operand where an RC register is required. It must satisfy
                // these conditions:
                //
                // 1. All RC2 registers are also in RC.
                // 2. The RC2 spill size must not be smaller that the RC spill size.
                // 3. RC2 spill alignment must be compatible with RC.
                //
                // Sub-classes are used to determine if a virtual register can be used
                // as an instruction operand, or if it must be copied first.
                if (rc.equals(rc2) || rc2.elts.size() > rc.elts.size()
                        || (rc.spillAlignment!= 0 && rc2.spillAlignment % rc.spillAlignment != 0)
                        || rc.spillSize > rc2.spillSize || !isSubRegisterClass(rc2, regSets))
                    continue;

                if (!empty) os.print(", ");
                os.printf("%sRegisterClass", rc2.theDef.getName());
                empty = false;

                if (!superClassMap.containsKey(j))
                {
                    superClassMap.put(j, new TIntHashSet());
                }
                superClassMap.get(j).add(i);
            }

            os.printf("%s", empty ? "" : ", ");
            os.printf("\n\t};\n\t");
        }

        for (int i = 0, e = regClasses.size(); i != e; i++)
        {
            CodeGenRegisterClass rc = regClasses.get(i);

            String name = rc.theDef.getName();
            os.printf("\t// %s Register Class super-classes...\n", name);
            os.printf("\tpublic static final TargetRegisterClass[] %sSuperclasses = {\n\t\t", name);

            boolean empty = true;
            if (superClassMap.containsKey(i))
            {
                for (int val : superClassMap.get(i).toArray())
                {
                    CodeGenRegisterClass rc2 = regClasses.get(val);
                    if (!empty) os.printf(", ");
                    os.printf("%sRegisterClass", rc2.getName());
                    empty = false;
                }
            }

            os.printf("%s", empty ? "":", ");
            os.printf("\n\t};\n\n");
        }

        SetMultiMap<Record, CodeGenRegisterClass> regClassesBelongedTo = new SetMultiMap<>();


        // Output RegisterClass array.
        os.println("\tpublic final static TargetRegisterClass[] registerClasses = {");
        regClasses.forEach(rc->
        {
            os.println("\t\t" + rc.theDef.getName()+"RegisterClass,");
        });
        os.println("\t};\n");

        // emit the register sub-registers / super-registers, aliases set.
        HashMap<Record, TreeSet<Record>> registerSubRegs = new HashMap<>();
        HashMap<Record, TreeSet<Record>> registerSuperRegs = new HashMap<>();
        HashMap<Record, TreeSet<Record>> registerAlias = new HashMap<>();
        HashMap<Record, ArrayList<Pair<Integer, Record>>> subRegList = new HashMap<>();

        ArrayList<CodeGenRegister> regs = target.getRegisters();
        regs.sort((r1, r2)->r1.getName().compareTo(r2.getName()));

        try
        {
            for (CodeGenRegister cgr : regs)
            {
                Record r = cgr.theDef;
                ArrayList<Record> li = r.getValueAsListOfDefs("Aliases");
                for (int i = 0, e = li.size(); i < e; i++)
                {
                    Record reg = li.get(i);
                    if (!registerAlias.containsKey(r))
                        registerAlias.put(r, new TreeSet<>(LessRecord));

                    if (registerAlias.get(r).contains(reg))
                    {
                        System.err.println("Warning: register alias between "
                                + r.getName() + " and " + reg.getName()
                                + " specified multiple times!\n");
                    }
                    registerAlias.get(r).add(reg);
                    if (registerAlias.get(reg).contains(r))
                    {
                        System.err.println("Warning: register alias between "
                                + r.getName() + " and " + reg.getName()
                                + " specified multiple times!\n");
                    }
                    registerAlias.get(reg).add(r);
                }
            }

            // Process sub-register sets.
            for (CodeGenRegister cgr : regs)
            {
                Record r = cgr.theDef;
                ArrayList<Record> list = r.getValueAsListOfDefs("SubRegs");
                if (!registerSubRegs.containsKey(r))
                    registerSubRegs.put(r, new TreeSet<>(LessRecord));

                for (Record subreg : list)
                {
                    if (registerSubRegs.get(r).contains(subreg))
                    {
                        System.err.printf("Warning: register %s specified as a sub-register of %s"
                                + " multiple times!", subreg.getName(), subreg.getName());
                    }
                    addSubSuperReg(r, subreg, registerSubRegs, registerSuperRegs, registerAlias);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Print the SubregHashTable, a simple quadratically probed
        // hash table for determining if a register is a subregister
        // of another register.
        int NumSubRegs = 0;
        HashMap<Record, Integer> RegNo = new HashMap<>();
        for (int i = 0, e = regs.size(); i != e; ++i) 
        {
            RegNo.put(regs.get(i).theDef,  i);
            NumSubRegs += registerSubRegs.get(regs.get(i).theDef).size();
        }
        
        int SubregHashTableSize = 2 * Util.NextPowerOf2(2 * NumSubRegs);
        int[] SubregHashTable = new int[2 * SubregHashTableSize];
        Arrays.fill(SubregHashTable, ~0);
        
        int hashMisses = 0;
        for (int i = 0, e = regs.size(); i != e; ++i)
        {
            Record R = regs.get(i).theDef;
            for (Record R3 : registerSubRegs.get(R))
            {
                // We have to increase the indices of both registers by one when
                // computing the hash because, in the generated code, there
                // will be an extra empty slot at register 0.
                int index = ((i+1) + (RegNo.get(R3) + 1) * 37) & (SubregHashTableSize-1);
                int ProbeAmt = 2;
                while (SubregHashTable[index*2] != ~0 &&
                        SubregHashTable[index*2+1] != ~0) {
                    index = (index + ProbeAmt) & (SubregHashTableSize-1);
                    ProbeAmt += 2;
        
                    hashMisses++;
                }
        
                SubregHashTable[index*2] = i;
                SubregHashTable[index*2+1] = RegNo.get(R3);
            }
        }
        
        os.printf("\n\n\t// Number of hash collisions: %d\n", hashMisses);
        
        if (SubregHashTableSize != 0) 
        {
            //std::string Namespace = regs[0].theDef->getValueAsString("Namespace");
        
            os.printf("\tpublic static final int[] SubregHashTable = {\n");
            for (int i = 0; i < SubregHashTableSize - 1; ++i) 
            {
                // Insert spaces for nice formatting.
                os.printf("\t\t");
        
                if (SubregHashTable[2*i] != ~0) 
                {
                    os.printf("%s, %s, \n", regs.get(SubregHashTable[2*i]).theDef.getName(), regs.get(SubregHashTable[2*i+1]).theDef.getName());
                } 
                else 
                {
                    os.printf("NoRegister, NoRegister, \n");
                }
            }
        
            int Idx = SubregHashTableSize*2-2;
            if (SubregHashTable[Idx] != ~0) 
            {
                os.printf("\t\t%s, %s };\n",
                        regs.get(SubregHashTable[Idx]).theDef.getName(),
                        regs.get(SubregHashTable[Idx+1]).theDef.getName());
            } 
            else 
            {
                os.printf("\t\tNoRegister, NoRegister, \n");
            }

            os.printf("\t};\n");
        
            os.printf("\tpublic static final int SubregHashTableSize = %d;\n", SubregHashTableSize);
        }
        else 
        {
            os.printf("\tpublic static final int[] SubregHashTable = { ~0, ~0 };\n "
                    + "public static final int SubregHashTableSize = 1;\n");
        }
        
        
        // Print the SuperregHashTable, a simple quadratically probed
        // hash table for determining if a register is a super-register
        // of another register.
        int NumSupRegs = 0;
        RegNo.clear();
        for (int i = 0, e = regs.size(); i != e; ++i) 
        {
            RegNo.put(regs.get(i).theDef, i);

            if (!registerSuperRegs.containsKey(regs.get(i).theDef))
                registerSuperRegs.put(regs.get(i).theDef, new TreeSet<>(LessRecord));

            NumSupRegs += registerSuperRegs.get(regs.get(i).theDef).size();
        }
        
        int SuperregHashTableSize = 2 * Util.NextPowerOf2(2 * NumSupRegs);
        int[] SuperregHashTable = new int[2 * SuperregHashTableSize];
        Arrays.fill(SuperregHashTable, ~0);
        
        hashMisses = 0;
        
        for (int i = 0, e = regs.size(); i != e; ++i) 
        {
            Record R = regs.get(i).theDef;
            if (!registerSuperRegs.containsKey(R))
                registerSuperRegs.put(R, new TreeSet<>(LessRecord));

            for (Record RJ : registerSuperRegs.get(R))
            {
                // We have to increase the indices of both registers by one when
                // computing the hash because, in the generated code, there
                // will be an extra empty slot at register 0.
                int index = ((i+1) + (RegNo.get(RJ) + 1) * 37) & (SuperregHashTableSize-1);
                int ProbeAmt = 2;
                while (SuperregHashTable[index*2] != ~0 &&
                        SuperregHashTable[index*2+1] != ~0) 
                {
                    index = (index + ProbeAmt) & (SuperregHashTableSize-1);
                    ProbeAmt += 2;
        
                    hashMisses++;
                }
        
                SuperregHashTable[index*2] = i;
                SuperregHashTable[index*2+1] = RegNo.get(RJ);
            }
        }
        
        os.printf("\n\n\t// Number of hash collisions: %s\n", hashMisses);
        
        if (SuperregHashTableSize != 0) 
        {
            //std::string Namespace = regs[0].theDef->getValueAsString("Namespace");
        
            os.printf("\tpublic final static int[] SuperregHashTable = {\n");
            for (int i = 0; i < SuperregHashTableSize - 1; ++i)
            {
                // Insert spaces for nice formatting.
                os.printf("\t\t");
        
                if (SuperregHashTable[2*i] != ~0)
                {
                    os.printf(regs.get(SuperregHashTable[2*i]).theDef.getName() + ", "
                            + regs.get(SuperregHashTable[2*i+1]).theDef.getName() + ", \n");
                }
                else
                {
                    os.printf("NoRegister,  NoRegister,\n");
                }
            }
        
            int Idx = SuperregHashTableSize*2-2;
            if (SuperregHashTable[Idx] != ~0)
            {
                os.printf("\t\t");
                os.printf(regs.get(SuperregHashTable[Idx]).theDef.getName() + ", "
                        + regs.get(SuperregHashTable[Idx+1]).theDef.getName() + " };\n");
            } 
            else 
            {
                os.printf("\t\tNoRegister,  NoRegister\n");
            }
            os.printf("\t};\n");
        
            os.printf("\tpublic static final int SuperregHashTableSize = %d;\n", SuperregHashTableSize);
        } else {
            os.printf("\tpublic static final int[] SuperregHashTable = { ~0, ~0 };\n"
                    + "\tpublic static final int SuperregHashTableSize = 1;\n");
        }
        
        // Print the AliasHashTable, a simple quadratically probed
        // hash table for determining if a register aliases another register.
        int NumAliases = 0;
        RegNo.clear();
        for (int i = 0, e = regs.size(); i != e; ++i)
        {
            RegNo.put(regs.get(i).theDef, i);
            if (!registerAlias.containsKey(regs.get(i).theDef))
                registerAlias.put(regs.get(i).theDef, new TreeSet<>(LessRecord));

            NumAliases += registerAlias.get(regs.get(i).theDef).size();
        }
        
        int AliasesHashTableSize = 2 * Util.NextPowerOf2(2 * NumAliases);
        int[] AliasesHashTable = new int[2 * AliasesHashTableSize];
        Arrays.fill(AliasesHashTable, ~0);
        //std::fill(AliasesHashTable, AliasesHashTable + 2 * AliasesHashTableSize, ~0);
        
        hashMisses = 0;
        
        for (int i = 0, e = regs.size(); i != e; ++i) 
        {
            Record R = regs.get(i).theDef;
            if (!registerAlias.containsKey(R))
                registerAlias.put(R, new TreeSet<>(LessRecord));

            for (Record RJ : registerAlias.get(R)) 
            {
                // We have to increase the indices of both registers by one when
                // computing the hash because, in the generated code, there
                // will be an extra empty slot at register 0.
                int index = ((i+1) + (RegNo.get(RJ) + 1) * 37) & (AliasesHashTableSize-1);
                int ProbeAmt = 2;
                while (AliasesHashTable[index*2] != ~0 &&
                        AliasesHashTable[index*2+1] != ~0) 
                {
                    index = (index + ProbeAmt) & (AliasesHashTableSize-1);
                    ProbeAmt += 2;
        
                    hashMisses++;
                }
        
                AliasesHashTable[index*2] = i;
                AliasesHashTable[index*2+1] = RegNo.get(RJ);
            }
        }
        
        os.printf("\n\n\t// Number of hash collisions: %s\n", hashMisses);
        
        if (AliasesHashTableSize != 0) 
        {
            //std::string Namespace = regs[0].theDef->getValueAsString("Namespace");
        
            os.printf("\tpublic final static int AliasesHashTable[] = {\n");
            for (int i = 0; i < AliasesHashTableSize - 1; ++i) 
            {

                // Insert spaces for nice formatting.
                os.printf("\t\t");
        
                if (AliasesHashTable[2*i] != ~0)
                {
                    os.printf(regs.get(AliasesHashTable[2*i]).theDef.getName() + ", "
                            + regs.get(AliasesHashTable[2*i+1]).theDef.getName()+ ", \n");
                }
                else
                {
                    os.printf("NoRegister,  NoRegister,\n");
                }
            }
        
            int Idx = AliasesHashTableSize*2-2;
            if (AliasesHashTable[Idx] != ~0)
            {
                os.printf("\t\t" + regs.get(AliasesHashTable[Idx]).theDef.getName() + ", "
                        + regs.get(AliasesHashTable[Idx+1]).theDef.getName() + " };\n");
            }
            else
            {
                os.printf("\t\tNoRegister,  NoRegister,\n");
            }

            os.printf("\t};\n");
        
            os.printf("\tpublic final static int AliasesHashTableSize = %d;\n" , AliasesHashTableSize);
        }
        else
        {
            os.printf("\tpublic static final int[] AliasesHashTable = { ~0, ~0 };\n%s",
                    "\tpublic static final int AliasesHashTableSize = 1;\n");
        }

        
        if (!registerAlias.isEmpty())
            os.printf("\n\n\t// Register Alias Sets...\n");
        
        // Emit the empty alias list
        os.printf("\tpublic static final int[] Empty_AliasSet = { };\n");
        // Loop over all of the registers which have aliases, emitting the alias list
        // to memory.
        for (Map.Entry<Record, TreeSet<Record>> pair : registerAlias.entrySet())
        {
            os.printf("\tpublic static final int[] " + pair.getKey().getName() + "_AliasSet = { ");
            pair.getValue().forEach(val->os.printf(val.getName() + ", "));
            os.printf("};\n");
        }
        
        if (!registerSubRegs.isEmpty())
            os.printf("\n\n\t// Register Sub-registers Sets...\n");
        
        // Emit the empty sub-registers list
        os.printf("\tpublic static final int[] Empty_SubRegsSet = {};\n");
        // Loop over all of the registers which have sub-registers, emitting the
        // sub-registers list to memory.
        for (Map.Entry<Record, TreeSet<Record>> pair : registerSubRegs.entrySet())
        {
            os.printf("\tpublic static final int[] " + pair.getKey().getName() + "_SubRegsSet = { ");

            ArrayList<Record> SubRegsVector = new ArrayList<>(pair.getValue());
            RegisterSorter RS = new RegisterSorter(registerSubRegs);
            SubRegsVector.sort(RS);

            for (int i = 0, e = SubRegsVector.size(); i != e; ++i)
                os.printf(SubRegsVector.get(i).getName() + ", ");

            os.printf("};\n");
        }
        
        if (!registerSuperRegs.isEmpty())
            os.printf("\n\n\t// Register Super-registers Sets...\n");
        
        // Emit the empty super-registers list
        os.printf("\tpublic static final int[] Empty_SuperRegsSet = { 0 };\n");
        // Loop over all of the registers which have super-registers, emitting the
        // super-registers list to memory.

        for (Map.Entry<Record, TreeSet<Record>> pair : registerSuperRegs.entrySet())
        {
            os.printf("\tpublic static final int[] " + pair.getKey().getName() + "_SuperRegsSet = { ");

            ArrayList<Record> SuperRegsVector = new ArrayList<>(pair.getValue());

            RegisterSorter RS = new RegisterSorter(registerSubRegs);
            SuperRegsVector.sort(RS);
            for (int i = 0, e = SuperRegsVector.size(); i != e; ++i)
                os.printf(SuperRegsVector.get(i).getName() + ", ");
            os.printf("};\n");
        }

        // Now that register alias and sub-registers sets have been emitted, emit the
        // register descriptors now.

        os.printf("\n\tpublic static final TargetRegisterDesc[] registerDescriptors = {// Descriptor\n");
        os.printf("\t\tnew TargetRegisterDesc(\"NOREG\", \"NOREG\", null, null, null),\n");

        // Now that register alias sets have been emitted, emit the register
        // descriptors now.
        for (CodeGenRegister reg : regs)
        {
            os.print("\t\tnew TargetRegisterDesc(\"");
            try
            {
                String asmName = reg.theDef.getValueAsString("AsmName");
                if (!asmName.isEmpty())
                    os.print(asmName);
                else
                    os.print(reg.getName());

                os.print("\", ");
                os.printf("\"%s\", ", reg.getName());

                if (registerAlias.containsKey(reg.theDef))
                    os.printf(reg.getName() + "_AliasSet, ");
                else
                    os.printf("Empty_AliasSet, ");

                if (registerSubRegs.containsKey(reg.theDef))
                    os.printf("%s_SubRegsSet, ", reg.getName());
                else
                    os.printf("Empty_SubRegsSet, ");

                if (registerSuperRegs.containsKey(reg.theDef))
                    os.printf("%s_SuperRegsSet", reg.getName());
                else
                    os.printf("Empty_SuperRegsSet");
                os.printf("),\n");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        os.println("\t};\n"); // The end of register descriptor.

        // emit the getCalleeSavedRegs method.
        /*
        os.println("private final int[] calleeSavedRegs = {\n\t");
        ArrayList<Record> csrs = target.getCalleeSavedRegisters();
        csrs.forEach(csr->os.print(csr.getName()+", "));
        os.print("\t};\n");

        os.println("public int[] getCalleeSavedRegs() { return calleeSavedRegs; }");
        os.println();

        // emit information about the callee saved register classes.
        os.println("private final TargetRegisterClass[] calleeSavedRegClasses = {\n\t");

        csrs.forEach(csr->
        {
            Set<CodeGenRegisterClass> rcs = regClassesBelongedTo.get(csr);
            assert rcs != null;
            CodeGenRegisterClass rc = null;
            for (CodeGenRegisterClass tmp : rcs)
            {
                if (rc == null || tmp.spillSize < rc.spillSize)
                    rc = tmp;
            }
            for (Record reg : rc.elts)
            {
                os.print(reg.getName() + ", ");

                // Keep track of which regclasses this register is in.
                regClassesBelongedTo.put(reg, rc);
            }
        });
        */

        String className = targetName + "GenRegisterInfo";

        ArrayList<Record> subRegs = records.getAllDerivedDefinition("SubRegSet");
        for (int i = 0, e = subRegs.size(); i != e; i++)
        {
            int subRegIdx = (int)subRegs.get(i).getValueAsInt("index");
            ArrayList<Record> from = subRegs.get(i).getValueAsListOfDefs("From");
            ArrayList<Record> to = subRegs.get(i).getValueAsListOfDefs("To");
            if (from.size() != to.size())
            {
                System.err.println("Error: register list and sub-register not of equal length in SubRegSet");;
                System.exit(1);
            }

            for (int ii = 0, ee = from.size(); ii < ee; ii++)
            {
                if (!subRegList.containsKey(from.get(ii)))
                    subRegList.put(from.get(ii), new ArrayList<>());

                subRegList.get(from.get(ii)).add(Pair.get(subRegIdx, to.get(ii)));;
            }
        }


        // Emit the subregister + index mapping function based on the information
        // calculated above.
        os.printf("\tpublic int getSubReg(int regNo, int index)\n\t{\n\t");
        os.printf("switch(regNo)\n\t\t{\n\t\t\t");
        os.printf("default: return 0;\n");

        for (Map.Entry<Record, ArrayList<Pair<Integer, Record>>> pair :subRegList.entrySet())
        {
            os.printf("\t\t\tcase %s:\n", pair.getKey().getName());
            os.printf("\t\t\t switch (index) {\n");
            os.printf("\t\t\tdefault: return 0;\n");
            for (int i = 0, e = pair.getValue().size(); i != e; i++)
            {
                os.printf("\t\t\t\tcase %d: return %s;\n", pair.getValue().get(i).first,
                        pair.getValue().get(i).second.getName());
            }
            os.printf("\t\t\t}\n\t\t\t\n");
        }
        os.printf("\t\t}\n");
        //os.printf("\treturn 0;\n");
        os.printf("\t}\n\n");

        // emit the fields and constructors for X86GenRegisterInfo.
        os.printf("\tpublic %s(int callFrameSetupOpCode, "
                + "int callFrameDestroyOpCode)\n\t{\n\t\t", className);
        os.println("super(registerDescriptors, registerClasses,"
                + "\n\t\t\t\tcallFrameSetupOpCode, callFrameDestroyOpCode,"
                + "\n\t\t\t\tSubregHashTable, SubregHashTableSize,"
                + "\n\t\t\t\tSuperregHashTable, SuperregHashTableSize,"
                + "\n\t\t\t\tAliasesHashTable, AliasesHashTableSize);");

        os.println("\t}");
        os.println("}");
    }

    private static void addSubSuperReg(Record r, Record s,
            HashMap<Record, TreeSet<Record>> subRegs,
            HashMap<Record, TreeSet<Record>> superRegs,
            HashMap<Record, TreeSet<Record>> aliases)
    {
        if (r.equals(s))
        {
            System.err.println("Error: recursive sub-register relationship between "
                + " register " + r.getName() + " and it's sub-registers?");
            System.exit(1);
        }

        if (!subRegs.containsKey(r))
            subRegs.put(r, new TreeSet<>(LessRecord));

        if (!subRegs.get(r).add(s))
            return;

        addSuperReg(s, r, subRegs, superRegs, aliases);

        if (!aliases.containsKey(r))
            aliases.put(r, new TreeSet<>(LessRecord));
        if (!aliases.containsKey(s))
            aliases.put(s, new TreeSet<>(LessRecord));

        aliases.get(r).add(s);
        aliases.get(s).add(r);

        if (subRegs.containsKey(s))
        {
            for (Record ss : subRegs.get(s))
            {
                addSubSuperReg(r, ss, subRegs, superRegs, aliases);
            }
        }
    }

    private static void addSuperReg(Record r, Record s,
            HashMap<Record, TreeSet<Record>> subRegs,
            HashMap<Record, TreeSet<Record>> superRegs,
            HashMap<Record, TreeSet<Record>> aliases)
    {
        if (r.equals(s))
        {
            System.err.println("Error: recursive sub-register relationship"
                + " between register " + r.getName() + " and its sub-register?");
            System.exit(1);
        }

        if (!superRegs.containsKey(r))
            superRegs.put(r, new TreeSet<>(LessRecord));

        if (!subRegs.containsKey(s))
            subRegs.put(s, new TreeSet<>(LessRecord));
        if (!aliases.containsKey(r))
            aliases.put(r, new TreeSet<>(LessRecord));
        if (!aliases.containsKey(s))
            aliases.put(s, new TreeSet<>(LessRecord));

        if (!superRegs.get(r).add(s))
            return;

        subRegs.get(s).add(r);
        aliases.get(r).add(s);
        aliases.get(s).add(r);
        if (superRegs.containsKey(s))
        {
            superRegs.get(s).forEach(ss-> {addSuperReg(r, ss, subRegs, superRegs, aliases);});
        }
    }

    private static boolean isSubRegisterClass(CodeGenRegisterClass rc,
            HashSet<Record> regSets)
    {
        for (Record r : rc.elts)
        {
            if (!regSets.contains(r))
                return false;
        }

        return true;
    }

    private static class RegisterSorter implements Comparator<Record>
    {
        private Map<Record, TreeSet<Record>> registerSubRegs;

        public RegisterSorter(Map<Record, TreeSet<Record>> registerSubRegs)
        {
            this.registerSubRegs = registerSubRegs;
        }

        @Override
        public int compare(Record o1, Record o2)
        {
            boolean res = registerSubRegs.containsKey(o1) && registerSubRegs.get(o1).contains(o2);
            return res ? -1 : 1;
        }

        @Override
        public boolean equals(Object obj)
        {
            return false;
        }
    }
}
