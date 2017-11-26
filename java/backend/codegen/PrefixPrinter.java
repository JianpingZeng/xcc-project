package backend.codegen;

import backend.support.FormattedOutputStream;

import java.io.PrintStream;

/**
 * A printer responsible for providing additional information when print
 * machine basic block and machine instruction.
 * @author Xlous.zeng
 * @version 0.1
 */
public class PrefixPrinter
{
    public PrintStream print(PrintStream os, MachineBasicBlock mbb)
    {
        return os;
    }

    public PrintStream print(PrintStream os, MachineInstr mi)
    {
        return os;
    }

    public FormattedOutputStream print(FormattedOutputStream os, MachineBasicBlock mbb)
    {
        return os;
    }

    public FormattedOutputStream print(FormattedOutputStream os, MachineInstr mi)
    {
        return os;
    }
}