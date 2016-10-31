package backend.asm;

import java.util.ArrayList;

/**
 * This class represents a label presdo instruction within assembly code.
 *
 * @author Xlous.zeng
 */
public final class Label
{
	private int position = -1;

	/**
	 * References to instructions that jump to this unresolved label.
	 * These instructions need to be patched when the label is bound
	 * using the {@link #patchInstructions(AbstractAssembler)} method.
	 */
	private ArrayList<Integer> patchPositions = new ArrayList<Integer>(4);

	/**
	 * Returns the position of this label in the code buffer.
	 *
	 * @return the position
	 */
	public int position()
	{
		assert position >= 0 : "Unbound label is being referenced";
		return position;
	}

	public Label()
	{
	}

	/**
	 * Binds the label to the specified position.
	 *
	 * @param pos the position
	 */
	protected void bind(int pos)
	{
		this.position = pos;
		assert isBound();
	}
	
	/**
	 * Checks if this label is bound to a physical memory location.
	 * @return
	 */
	public boolean isBound()
	{
		return position >= 0;
	}
	/**
	 * Add a reference to branch instruction into a list of patch position.
	 * @param branchLocation
	 */
	public void addPatchAt(int branchLocation)
	{
		assert !isBound() : "Label is already bound";
		patchPositions.add(branchLocation);
	}
	/**
	 * Set the branch target of these instructions which reference to this label. 
	 * @param masm
	 */
	protected void patchInstructions(AbstractAssembler masm)
	{
		assert isBound() : "Label should be bound";
		int target = position;
		for (int i = 0; i < patchPositions.size(); ++i)
		{
			int pos = patchPositions.get(i);
			masm.patchJumpTarget(pos, target);
		}
	}

	@Override 
	public String toString()
	{
		return isBound() ? String.valueOf(position()) : "?";
	}
}
