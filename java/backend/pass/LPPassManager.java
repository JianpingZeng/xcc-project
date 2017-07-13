package backend.pass;

import backend.value.Loop;
import backend.analysis.LoopInfo;
import backend.value.Function;

import java.util.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class LPPassManager implements FunctionPass, PassManagerBase
{
	private LinkedList<Loop> loopQueue = new LinkedList<>();
	private boolean skipThisLoop;
	private boolean redoThisLoop;
	private LoopInfo li;
	private Loop currentLoop;
	private ArrayList<Pass> loopPasses = new ArrayList<>();
	private HashSet<Pass> containedPasses = new HashSet<>();

	@Override
	public void getAnalysisUsage(AnalysisUsage au)
	{
		au.addRequired(LoopInfo.class);
	}

	private static void addLoopIntoQueue(Loop loop, Queue<Loop> queues)
	{
		queues.add(loop);
		for (Loop sub : loop.getSubLoops())
			addLoopIntoQueue(sub, queues);
	}

	/**
	 * Execute all of the passes scheduled for execution.
	 *
	 * @param f
	 * @return
	 */
	@Override
	public boolean runOnFunction(Function f)
	{
		li = getAnalysisToUpDate(LoopInfo.class);
		boolean changed = false;

		for (Loop l : li.getTopLevelLoop())
			addLoopIntoQueue(l, loopQueue);

		if (loopQueue.isEmpty())
			return false;

		// Initialization.
		for (Loop loop : loopQueue)
		{
			for (Pass p : loopPasses)
			{
				if (p instanceof LoopPass)
				{
					changed |= ((LoopPass)p).doInitialization(loop, this);
				}
			}
		}

		// Walk loops.
		while (!loopQueue.isEmpty())
		{
			currentLoop = loopQueue.peek();
			skipThisLoop = false;
			redoThisLoop = false;

			for (Pass p : loopPasses)
			{
				initializeAnalysisImpl(p);
				if (p instanceof LoopPass)
				{
					changed |= ((LoopPass)p).runOnLoop(currentLoop, this);
				}
				//removeDeadedPasses(p);
				if (skipThisLoop)
					break;
			}
			// Pop the loop from queue after running all passes.
			loopQueue.poll();
			if (redoThisLoop)
				loopQueue.add(currentLoop);
		}

		// Finalization.
		for (Loop loop : loopQueue)
		{
			for (Pass p : loopPasses)
			{
				if (p instanceof LoopPass)
				{
					changed |= ((LoopPass)p).doFinalization();
				}
			}
		}
		return changed;
	}

	@Override
	public String getPassName()
	{
		return "Loop pass manager";
	}

	public Pass getContainedPass(int idx)
	{
		assert idx >= 0 && idx < loopPasses.size();
		return loopPasses.get(idx);
	}

	/**
	 * Add a pass to the queue of passes to run.
	 *
	 * @param p
	 */
	@Override
	public void add(Pass p)
	{
		if (p instanceof LoopPass)
		{
			LoopPass lp = (LoopPass) p;
			if (containedPasses.add(lp))
				loopPasses.add(lp);
		}
	}

	@Override
	public PassManagerType getPassManagerType()
	{
		return PassManagerType.PMT_LoopPassManager;
	}

	public void deleteLoopFromQueue(Loop loop)
	{
		Loop parentLoop = loop.getParentLoop();
		if (parentLoop != null)
		{
			loop.getBlocks().forEach(bb->
			{
				if (li.getLoopFor(bb).equals(loop))
					li.changeLoopFor(bb, parentLoop);
			});

			for (int idx = 0; idx < parentLoop.getSubLoops().size(); idx++)
			{
				Loop subLoop = parentLoop.getSubLoops().get(idx);
				if (subLoop.equals(loop))
				{
					parentLoop.removeChildLoop(idx);
					break;
				}
			}
			while (!loop.isEmpty())
			{
				parentLoop.addChildLoop(loop.removeChildLoop(loop.getNumOfSubLoop() - 1));
			}
		}
		else
		{
			for (int i = 0; i < loop.getNumOfBlocks(); ++i)
			{
				if (li.getLoopFor(loop.getBlock(i)).equals(loop))
				{
					li.removeBlock(loop.getBlock(i));
					--i;
				}
			}

			for (int idx = 0, e = li.getTopLevelLoop().size(); idx < e; idx++)
			{
				Loop l = li.getTopLevelLoop().get(idx);
				if (l.equals(loop))
				{
					li.removeTopLevelLoop(idx);
					break;
				}
			}

			while (!loop.isEmpty())
				li.addTopLevelLoop(loop.removeChildLoop(loop.getNumOfSubLoop() - 1));
		}

		if (currentLoop.equals(loop))
		{
			skipThisLoop = true;
			return;
		}

		for (Iterator<Loop> itr = loopQueue.iterator(); itr.hasNext(); )
		{
			if (itr.next().equals(loop))
			{
				itr.remove();
				break;
			}
		}
	}

	public void insertLoop(Loop loop, Loop parentLoop)
	{
		if (parentLoop != null)
			parentLoop.addChildLoop(loop);
		else
			li.addTopLevelLoop(loop);

		if (loop.equals(currentLoop))
			redoLoop(loop);
		else if (parentLoop==null)
			loopQueue.addFirst(loop);
		else
		{
			for (int i = 0; i < loopQueue.size(); i++)
			{
				if (parentLoop.equals(loopQueue.get(i)))
				{
					loopQueue.add(i+1, loop);
					break;
				}
			}
		}
	}

	public void redoLoop(Loop loop)
	{
		redoThisLoop = true;
	}

	private void initializeAnalysisImpl(Pass pass)
	{

	}
}
