/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package utils.as;

import backend.bitcode.writer.BitcodeWriter;
import backend.support.LLVMContext;
import backend.value.Module;
import tools.*;
import tools.commandline.CL;
import tools.commandline.FormattingFlagsApplicator;
import tools.commandline.StringOpt;

import static tools.commandline.Desc.desc;
import static tools.commandline.FormattingFlags.Positional;
import static tools.commandline.Initializer.init;
import static tools.commandline.OptionNameApplicator.optionName;
import static tools.commandline.ValueDesc.valueDesc;

public class AS {
    private static final StringOpt InputFilename =
            new StringOpt(new FormattingFlagsApplicator(Positional),
                    desc("<input LLVM IR file>"),
                    init("-"),
                    valueDesc("filename"));
    private static final StringOpt OutputFilename =
            new StringOpt(optionName("o"), desc("Override output filename"),
                    valueDesc("filename"),
                    init(""));

    public static void main(String[] args) {
        String[] temp;
        if (args[0].equals("llvm-as.cpp")) {
            temp = args;
        } else {
            temp = new String[args.length + 1];
            temp[0] = "llvm-as.cpp";
            System.arraycopy(args, 0, temp, 1, args.length);
        }

        new PrintStackTraceProgram(temp);
        CL.parseCommandLineOptions(args, "The LLVM IR Assembler");

        OutRef<SMDiagnostic> diag = new OutRef<>();
        Module theModule = backend.llReader.Parser.parseAssemblyFile(InputFilename.value, diag,
                LLVMContext.getGlobalContext());
        if (theModule == null) {
            diag.get().print("llvm-as.cpp", System.err);
            System.exit(0);
        }
        if (OutputFilename.value.equals("-") || (OutputFilename.value.isEmpty() && InputFilename.value.equals("-"))) {
            System.out.println("warning: write bitcode to standard out");
            OutputFilename.value = "-";
        }
        else if (OutputFilename.value.isEmpty())
            OutputFilename.value = InputFilename.value.split("\\.")[0] + ".bc";

        BitcodeWriter.writeBitcodeToFile(theModule, OutputFilename.value);
    }
}
