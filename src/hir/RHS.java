package hir; 

import type.Type;

/**
 * A labelled interface just for distinguishing between left hand side and
 * right hand side.
 * @author Jianping Zeng <z1215jping@hotmail.com>
 *
 */
public interface RHS
{
	Type getType();
}
