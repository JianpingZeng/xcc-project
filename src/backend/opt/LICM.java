package backend.opt;

import backend.analysis.LoopInfo;

/** 
 * </p>
 * This class performs loop invariant code motion, attempting to remove
 * as much code from the body of a loop as possible. It does this by either
 * hoisting code into the pre-header block, or by sinking code to the exit 
 * block if it is safe. Currently, this class does not use alias analysis
 * so that the all backend.opt operated upon memory access are excluded.
 * </p>
 * 
 * <p>This pass expected to run after {@linkplain LoopInversion Loop Inversion}
 * and {@linkplain LoopInfo pass}.
 * performed.
 * </p>
 * 
 * <p>This file is a member of <a href={@docRoot/opt}>Machine Independence
 * Optimization</a>
 * </p>.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LICM
{	

}
