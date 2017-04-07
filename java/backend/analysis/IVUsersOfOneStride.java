package backend.analysis;

import backend.value.Instruction;
import backend.value.Value;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class IVUsersOfOneStride
{
	/**
	 * The stride for all the contained IVStrideUses.
	 * This is a constant for affine strides.
	 */
	public SCEV stride;
	/**
	 * Keep track of all of the users of this stride as well
	 * as the initial value and the operand that uses the IV.
	 */
	public LinkedList<IVStrideUses> users;

	public IVUsersOfOneStride(SCEV stride)
	{
		this.stride = stride;
		users = new LinkedList<>();
	}

	public void addUser(SCEV offset, Instruction user, Value operand)
	{
		users.add(new IVStrideUses(this, offset, user, operand));
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass())
			return false;

		IVUsersOfOneStride ins = (IVUsersOfOneStride)obj;
		return stride.equals(ins.stride) && users.equals(ins.users);
	}

	@Override
	public int hashCode()
	{
		return (stride.hashCode() << 15) ^ users.hashCode();
	}
}
