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

import tools.SourceMgr;
import tools.commandline.*;

import static tools.commandline.Desc.desc;
import static tools.commandline.FormattingFlags.Positional;
import static tools.commandline.FormattingFlags.Prefix;
import static tools.commandline.Initializer.init;
import static tools.commandline.ValueDesc.valueDesc;
import static utils.tablegen.Record.records;
import static utils.tablegen.TableGen.ActionType.*;

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
        GenRegisterNames,
        GenRegisterInfo,
        GenInstrNames,
        GenInstrInfo,
        GenCallingConv,
        GenAsmPrinter,
        PrintRecords,
        GenDAGISel,
        GenFastISel,
        GenSubtarget,

    }

    private static Opt<ActionType> action = new Opt<ActionType>(
            new Parser<>(),
            desc("Action to performance"),
            new ValueClass<>(
                    new ValueClass.Entry<>(GenRegisterNames, "gen-register-names",
                            "Generates register names"),
                    new ValueClass.Entry<>(GenRegisterInfo, "gen-register-info",
                            "Generates register description file"),
                    new ValueClass.Entry<>(GenInstrNames, "gen-instr-names",
                            "Generates instruction names"),
                    new ValueClass.Entry<>(GenInstrInfo, "gen-instr-info",
                            "Generates instruction descriptions"),
                    new ValueClass.Entry<>(GenCallingConv, "gen-callingconv",
                            "Generate calling convention descriptions"),
                    new ValueClass.Entry<>(GenAsmPrinter, "gen-asm-printer",
                            "Generates assembly printer"),
                    new ValueClass.Entry<>(PrintRecords, "print-records",
                            "Print all records to stdout (default)"),
                    new ValueClass.Entry<>(GenDAGISel, "gen-dag-isel",
                            "Generate a DAG instruction selector"),
                    new ValueClass.Entry<>(GenFastISel, "gen-fast-isel",
                            "Generate a \"fast\" instruction selector"),
                    new ValueClass.Entry<>(GenSubtarget, "gen-subtarget",
                            "Generate subtarget enumerations")
            ));

    private static StringOpt outputFileName = new StringOpt(
            new OptionNameApplicator("o"),
            init("-"),
            desc("Specify the output file name"),
            valueDesc("filename"));

    private static ListOpt<String> includeDirs = new ListOpt<>(
            new ParserString(),
            new OptionNameApplicator("I"),
            desc("Directory of includes file"),
            valueDesc("directory"),
            new FormattingFlagsApplicator(Prefix));

    private static StringOpt inputFilename = new StringOpt(
            new FormattingFlagsApplicator(Positional),
            desc("<input file>"),
            init("-"));

    // For debug, dump each def and class.
    public static final boolean DEBUG = false;

    public static void main(String[] args)
    {
        try
        {
            CL.parseCommandLineOptions(args);
            if (outputFileName.value == null)
            {
                outputFileName.value = "-";
            }

            if (TGParser.parseFile(inputFilename.value, includeDirs, new SourceMgr()))
                System.exit(1);

            String outputFile = outputFileName.value;
            switch (action.value)
            {
                case PrintRecords:
                    records.dump();
                    break;
                case GenRegisterNames:
                    new RegisterInfoEmitter(records).runEnums(outputFile);
                    break;
                case GenRegisterInfo:
                    new RegisterInfoEmitter(records).run(outputFile);
                    break;
                case GenInstrNames:
                    new InstrInfoEmitter(records).runEnums(outputFile);
                    break;
                case GenInstrInfo:
                    new InstrInfoEmitter(records).run(outputFile);
                    break;
                case GenCallingConv:
                    new CallingConvEmitter(records).run(outputFile);
                    break;
                case GenAsmPrinter:
                    new AsmWriterEmitter().run(outputFile);
                    break;
                case GenDAGISel:
                    // TODO: 17-7-14
                    assert false:"Current DAGIsel not supported!";
                    break;
                case GenFastISel:
                    new FastISelEmitter(records).run(outputFile);
                    break;
                case GenSubtarget:
                    new SubtargetEmitter(records).run(outputFile);
                    break;
                default:
                    assert false : "Invalid action type!";
                    System.exit(1);
            }
        }
        catch (Exception ex)
        {
            System.err.println(ex.getMessage());

            System.exit(-1);
        }
    }
}
