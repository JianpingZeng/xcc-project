/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
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

package utils.tablegen;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class JlangOptionsEmitter extends TableGenBackend
{
    private RecordKeeper records;
    public JlangOptionsEmitter(RecordKeeper records)
    {
        this.records = records;
    }

    private static final Comparator<Record> OptionComparator = new Comparator<Record>()
    {
        @Override
        public int compare(Record r1, Record r2)
        {
            try
            {
                boolean sen1 = r1.getValueAsDef("Kind").getValueAsBit("Sentinel");
                boolean sen2 = r2.getValueAsDef("kind").getValueAsBit("Sentinel");
                if (sen1 != sen2)
                    return sen1 ? -1 : 1;
                if (!sen1)
                {
                    String name1 = r1.getValueAsDef("Kind").getValueAsString("Name");
                    String name2 = r2.getValueAsDef("Kind").getValueAsString("Name");
                    if (!name1.equals(name2))
                        return name1.compareTo(name2);
                }
                long pred1 = r1.getValueAsDef("Kind").getValueAsInt("Precedence");
                long pred2 = r2.getValueAsDef("Kind").getValueAsInt("Precedence");
                assert pred1 != pred2:"Two equivalent options";
                return pred1 < pred2 ? -1 : 1;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return 0;
        }
    };

    private static String getOptionName(Record option) throws Exception
    {
        Init init = option.getValueInit("EnumName");
        if (init instanceof Init.UnsetInit)
            return option.getValueAsString("Name");
        return option.getValueAsString("EnumName");
    }

    /**
     * Emit Option ID for each option.
     * @param options
     * @param os
     */
    private void emitOptionIDs(ArrayList<Record> options, PrintStream os)
            throws Exception
    {
        os.println("\t This class defines some sort of static Constant for representing Option ID.");
        os.println("\tpublic interface OptionID");
        os.println("\t{");
        int i = 0;
        for (Record opt : options)
        {
            os.printf("\t\tint OPT_%s = %d;", getOptionName(opt), i++);
        }
        os.println("\t}\n\n");
    }

    private void emitOptionKind(ArrayList<Record> options, PrintStream os)
            throws Exception
    {
        os.println("\t This class defines some sort of static Constant for representing Option Kind.");
        os.println("\tpublic interface OptionKind");
        os.println("\t{");
        int i = 0;
        for (Record opt : options)
        {
            os.printf("\t\tint KIND_%s = %d;", opt.getValueAsDef("Kind").getValueAsString("Name"), i++);
        }
        os.println("\t}\n\n");
    }

    private void emitGroup(ArrayList<Record> groups, PrintStream os)
    {
        os.println("// Emission for Groups.");
    }

    @Override
    public void run(String outputFile) throws Exception
    {
        assert outputFile != null && !outputFile.isEmpty()
                :"Invalid path to output file";
        String className = "Option";
        try (PrintStream os = outputFile.equals("-") ?
                System.out:new PrintStream(new FileOutputStream(outputFile)))
        {
            ArrayList<Record> options = records.getAllDerivedDefinition("Option");
            ArrayList<Record> groups = records.getAllDerivedDefinition("OptionGroup");
            options.sort(OptionComparator);

            emitSourceFileHeaderComment("Options definitions for Jlang driver", os);

            os.printf("public class %s\n", className);
            os.println("{%n");
            emitOptionIDs(options, os);
            emitOptionKind(options, os);

            emitGroup(groups, os);

            os.println("// Emission for Options.");
            for (int i = 0, e = options.size(); i < e; i++)
            {
                Record opt = options.get(i);
                String groupName = "INVALID";

                if (opt.getValueInit("Group") instanceof Init.DefInit)
                {
                    Init.DefInit di = (Init.DefInit)opt.getValueInit("Group");
                    groupName = getOptionName(di.getDef());
                }

                String aliasName = "INVALID";
                if (opt.getValueInit("Alias") instanceof Init.DefInit)
                {
                    Init.DefInit li = (Init.DefInit) opt.getValueInit("Alias");
                    aliasName = getOptionName(li.getDef());
                }
                // Option flags.
                StringBuilder flags = new StringBuilder("null");
                Init.ListInit li = opt.getValueAsListInit("Flags");
                if (li.getSize() > 0)
                {
                    for (int j = 0, sz = li.getSize(); j < sz;j++)
                    {
                        if (j != 0)
                            flags.append("|");
                        flags.append(((Init.DefInit) li.getElement(j)).getDef().getName());
                    }
                }

                // Help information.
                String helpText = "null";
                if (!(opt.getValueInit("HelpText") instanceof Init.UnsetInit))
                {
                    helpText = "        " + opt.getValueAsString("HelpText");
                }

                // Metavarname.
                String metaVarName = "null";
                if (!(opt.getValueInit("MetaVarName") instanceof Init.UnsetInit))
                {
                    metaVarName = opt.getValueAsString("MetaVarName");
                }

                os.printf("Option(\"%s\", %s, %s, %s, %s, %s, %d, \"%s\", \"%s\")",
                        opt.getValueAsString("Name"),
                        "OPT_" + getOptionName(opt),
                        "KIND_" + opt.getValueAsDef("Kind").getValueAsString("Name"),
                        "GROUP_" + groupName,
                        "OPT_" + aliasName,
                        flags.toString(),
                        opt.getValueAsInt("NumArgs"),
                        helpText,
                        metaVarName);
                if (i < e - 1)
                    os.println(",");
                else
                    os.println(";");
            }

            os.println("\tpublic final String optionName;");
            os.println("\tpublic final int id;");
            os.println("\tpublic final int kind;");
            os.println("\tpublic final int group;");
            os.println("\tpublic final int alias;");
            os.println("\tpublic final String flags;");
            os.println("\tpublic final int param;");
            os.println("\tpublic final String helpText;");
            os.println("\tpublic final String emtaVarName;");
            os.println("\t");
            os.println("}");
        }
    }
}
