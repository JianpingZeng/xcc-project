package backend.analysis;

import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.pass.RegisterPass;
import backend.support.DepthFirstOrder;
import backend.support.LoopInfoBase;
import backend.utils.PredIterator;
import backend.value.BasicBlock;
import backend.value.Function;
import backend.value.Loop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

/**
 * This class defined as a helper class for identifying all loop in a method. 
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LoopInfo
		implements LoopInfoBase<BasicBlock, Loop>,FunctionPass
{
	static
	{
		new RegisterPass("loops", "Natural Loop Information",
				LoopInfo.class, true, true);
	}
	private HashMap<BasicBlock, Loop> bbMap = new HashMap<>();

	private ArrayList<Loop> topLevelLoops = new ArrayList<>();

	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		assert au != null;
		au.addRequired(DomTreeInfo.class);
	}

	@Override
	public boolean runOnFunction(Function f)
	{
		calculate((DomTreeInfo) getAnalysisToUpDate(DomTreeInfo.class));
		return false;
	}

	private void calculate(DomTreeInfo dt)
	{
		BasicBlock rootNode = dt.getRootNode().getBlock();

		Set<BasicBlock> dfList = DepthFirstOrder.reversePostOrder(rootNode);
		for (BasicBlock bb : dfList)
		{
			Loop loop = considerForLoop(bb, dt);
			if (loop != null)
				topLevelLoops.add(loop);
		}
	}

	private Loop considerForLoop(BasicBlock bb, DomTreeInfo dt)
	{
		if (bbMap.containsKey(bb))
			return null;

		Stack<BasicBlock> todoStack = new Stack<>();
		PredIterator itr = bb.predIterator();
		while (itr.hasNext())
		{
			BasicBlock pred = itr.next();
			if (dt.dominates(bb, pred))
				todoStack.push(pred);
		}

		if (todoStack.isEmpty()) return null;

		Loop l = new Loop(bb);
		bbMap.put(bb, l);

		BasicBlock entryBlock = bb.getParent().getEntryBlock();
		while (!todoStack.isEmpty())
		{
			BasicBlock cur = todoStack.pop();
			// The current block is not contained in loop as yet,
			// and it is reachable from entry block.
			if (!l.contains(cur) && dt.dominates(entryBlock, cur))
			{
				// Check to see if this block already belongs to a loop.  If this occurs
				// then we have a case where a loop that is supposed to be a child of
				// the current loop was processed before the current loop.  When this
				// occurs, this child loop gets added to a part of the current loop,
				// making it a sibling to the current loop.  We have to reparent this
				// loop.
				Loop subLoop = getLoopFor(cur);
				if (subLoop != null)
				{
					if (subLoop.getHeaderBlock() == cur && isNotAlreadyContainedIn(subLoop, l))
					{
						assert subLoop.getParentLoop() != null && subLoop.getParentLoop() != l;
						Loop subParentLoop = subLoop.getParentLoop();
						assert subParentLoop.getSubLoops().contains(subLoop);
						subParentLoop.subLoops.remove(subLoop);

						subLoop.setParentLoop(l);
						l.subLoops.add(subLoop);
					}
				}

				l.blocks.add(cur);
				for (PredIterator predItr = cur.predIterator(); predItr.hasNext();)
				{
					todoStack.push(predItr.next());
				}
			}
		}

		for (BasicBlock block : l.blocks)
		{
			// If there are any loops nested within this loop, create them.
			Loop newLoop = considerForLoop(block, dt);
			if (newLoop != null)
			{
				l.subLoops.add(newLoop);
				newLoop.setParentLoop(l);
			}

            // Add the basic blocks that comprise this loop to the BBMap so that this
            // loop can be found for them.
            if (!bbMap.containsKey(block))
            {
                bbMap.put(block, l);
            }
		}

		HashMap<BasicBlock, Loop> containingLoops = new HashMap<>();
		for (int i = 0; i < l.subLoops.size(); i++)
		{
			Loop childLoop = l.subLoops.get(i);
			assert childLoop.getParentLoop() == l;

			Loop containedLoop;
			if ((containedLoop = containingLoops.get(childLoop.getHeaderBlock())) != null)
			{
				moveSiblingLoopInto(childLoop, containedLoop);
				--i;
			}
			else
			{
				for (int b = 0, e = childLoop.blocks.size(); b < e; b++)
				{
					Loop blockLoop = containingLoops.get(childLoop.blocks.get(i));
					// If the block in the child loop are the same nested into another
                    // loop.
					if (blockLoop != childLoop)
					{
						Loop subLoop = blockLoop;
						for (int j = 0, sz = subLoop.blocks.size(); j < sz; j++)
						{
							containingLoops.put(subLoop.blocks.get(j), childLoop);

							moveSiblingLoopInto(subLoop, childLoop);
							--i;
						}
					}
				}
			}
		}

		return l;
	}

	/**
	 * This method moves the newChild loop to live inside of the newParent,
	 * instead of being a slibing of it.
	 * @param newChild
	 * @param newParent
	 */
	private void moveSiblingLoopInto(Loop newChild, Loop newParent)
	{
		Loop oldParent = newChild.getParentLoop();
		assert oldParent != null && oldParent == newParent.getParentLoop();

		assert oldParent.subLoops.contains(newChild) :"Parent field incorrent!";
		oldParent.subLoops.remove(newChild);
		newParent.subLoops.add(newChild);
		newChild.setParentLoop(null);

		insertLoopInto(newChild, newParent);
	}

	private void insertLoopInto(Loop child, Loop parent)
	{
		BasicBlock header = child.getHeaderBlock();
		assert parent.contains(header) : "This loop should not be inserted here";

		// Check to see if it belongs in a child loop...
		for (int i = 0, e = parent.subLoops.size(); i < e; i++)
		{
			if (parent.subLoops.get(i).contains(header))
			{
				insertLoopInto(child, parent.subLoops.get(i));
				return;
			}
		}

		parent.subLoops.add(child);
		child.setParentLoop(parent);
	}

	private boolean isNotAlreadyContainedIn(Loop subLoop, Loop parentLoop)
	{
		if (subLoop == null) return true;
		if (subLoop == parentLoop) return false;
		return isNotAlreadyContainedIn(subLoop.getParentLoop(), parentLoop);
	}

	@Override
	public String getPassName()
	{
		return "The statistic of loop info on HIR";
	}

	@Override
	public HashMap<BasicBlock, Loop> getBBMap()
	{
		return bbMap;
	}

	@Override
	public ArrayList<Loop> getTopLevelLoop()
	{
		return topLevelLoops;
	}

	@Override
	public int getLoopDepth(BasicBlock bb)
	{
		Loop loop = getLoopFor(bb);
		return loop != null ? loop.getLoopDepth() : 0;
	}

	@Override
	public boolean isLoopHeader(BasicBlock bb)
	{
		Loop loop = getLoopFor(bb);
		return loop != null && bb == loop.getHeaderBlock();
	}

	@Override
	public void ensureIsTopLevel(Loop loop, String msg)
	{
		assert loop.getParentLoop() == null:msg;
	}

	@Override
	public void removeBlock(BasicBlock block)
	{
		if (bbMap.containsKey(block))
		{
			Loop loop = bbMap.get(block);
			while(loop != null)
			{
				loop.removeBlockFromLoop(block);
				loop = loop.getParentLoop();
			}
			bbMap.remove(block);
		}
	}

	public boolean isEmpty()
	{
		return bbMap.isEmpty();
	}
}
