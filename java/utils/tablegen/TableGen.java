package utils.tablegen;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TableGen
{
    /**
     * This enum contains all kind of action that user wnat to perform, includes
     * GenRegisterNames, GenRegisterInfo, GenInstrNames, GenInstrInfo.
     */
    enum ActionType
    {
        GenRegisterNames("gen-reg-names", "Generates register names"),
        GenRegisterInfo("gen-reg-info", "Generates register information file"),
        GenInstrNames("gen-instr-names", "Generates instr names"),
        GenInstrInfo("gen-instr-info", "Generates instr information"),
        GenAsmPrinter("gen-asm-print", "Generates assembly printer");

        String optName;
        String desc;

        ActionType(String optName, String desc)
        {
            this.optName = optName;
            this.desc = desc;
        }
    }

    private static ArrayList<Option> allOpts = new ArrayList<>();
    private static OptionGroup optGroup = new OptionGroup();
    static
    {
        Option outputFileName = new Option("o", true,
                "Specify the output file name");

        Option includeDirs = new Option("I", true,
                "Directory of includes file");
        includeDirs.setArgs(Option.UNINITIALIZED);

        for (ActionType act :  ActionType.values())
            optGroup.addOption(new Option(act.optName, false, act.desc));
        optGroup.setRequired(true);

        allOpts.add(outputFileName);
        allOpts.add(includeDirs);
    }

    public static void main(String[] args)
    {
        CommandLine cmdline;
        Options opts = new Options();
        allOpts.forEach(opts::addOption);
        opts.addOptionGroup(optGroup);

        DefaultParser defaultParser = new DefaultParser();
        try
        {
            cmdline = defaultParser.parse(opts, args);
            List<String> leftArgs = cmdline.getArgList();
            String outputFile = cmdline.getOptionValue("o");
            if (outputFile == null)
            {
                outputFile = "-";
            }
            else
            {
                if (!Files.exists(new File(outputFile).toPath()))
                    throw new FileNotFoundException(outputFile);
            }

            String inputFilename = leftArgs.isEmpty()? "-": leftArgs.get(0);
            String[] includeDirs = cmdline.getOptionValues("I");
            TGParser.parseFile(inputFilename, new ArrayList<>(Arrays.asList(includeDirs)));

            switch (ActionType.valueOf(optGroup.getSelected()))
            {
                case GenRegisterNames:
                    new RegisterInfoEmitter(Record.records).runEnums(outputFile);
                    break;
                case GenRegisterInfo:
                    new RegisterInfoEmitter(Record.records).run(outputFile);
                    break;
                case GenInstrNames:
                    new InstrInfoEmitter(Record.records).runEnums(outputFile);
                    break;
                case GenInstrInfo:
                    new InstrInfoEmitter(Record.records).run(outputFile);
                    break;
                case GenAsmPrinter:
                    new AsmWriterEmitter().run(outputFile);
                    break;
                default:
                    assert false :"Invalid action type!";
                    System.exit(1);
            }
        }
        catch (FileNotFoundException ex)
        {
            System.err.println(ex.getMessage());
            System.exit(-1);
        }
        catch (Exception ex)
        {
            System.err.println(ex.getMessage());
            new HelpFormatter().printHelp("tblgen", opts);
            System.exit(-1);
        }
    }
}
