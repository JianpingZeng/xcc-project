package backend.lir;

import hir.BasicBlock;

import java.util.List;
import backend.lir.backend.TargetFunctionAssembler;
import driver.Backend;

/**
 * This file defines a class for generating assembly for LIR instructions.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class LIRAssembler
{
    /**
     * Backend abstraction.
     */
    final Backend backend;
    /**
     * A assembler for target compiled function.
     */
    final TargetFunctionAssembler tasm;
    final StackFrame frame;
    /**
     * constructor with two parameters, the first represents backend and
     * another represents assembler for targeted function.
     * @param backend
     * @param tasm
     */
	public LIRAssembler(Backend backend, TargetFunctionAssembler tasm)
    {
        this.backend = backend;
        this.tasm = tasm;
        this.frame = backend.frameMap();
    }
	
	public void emitCode(List<BasicBlock> blocks)
	{
	    
	}
}
