package hir;

import ci.CiKind;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Jianping Zeng<z1215jping@hotmail.com> on 2016/3/7.
 */
public abstract class Value
{
	/**
	 * The kind of this value.
	 */
	protected CiKind kind;

	/**
	 * The list of user who uses this value.
	 */
	protected List<Use> uses;

	public Iterator<Use> iterator()
	{
		return uses.iterator();
	}

	public Value(CiKind kind)
	{
		this.kind = kind;
	}

	/**
	 * Replace the value of uses list with new value.
	 *
	 * <p>
	 *     Got through the uses list for this definition
	 *     and make each use point to "newValue" instead
	 *     of "this". After this completes, this's use list
	 *     is guaranted to be empty.
	 * </p>
	 * @param newValue
	 */
	public void replaceAllUsesWith(Value newValue)
	{

	}

	public boolean isUseEmpty()
	{
		return uses.isEmpty();
	}

	public ListIterator<Use> listIterator()
	{
		return uses.listIterator();
	}

	/**
	 * The numbers of this other value who uses this.
	 * @return
	 */
	public int getNumUses()
	{
		return uses.size();
	}

	/**
	 * Whether or not that there is exactly one user of this value.
	 * @return
	 */
	public boolean hasOneUses()
	{
		return uses.size() == 1;
	}

	/**
	 * Whether or not that there are exactly N uesr of this.
	 * @param N
	 * @return
	 */
	public boolean hasNUses(int N)
	{
		return uses.size() == N;
	}

	/**
	 * Determines if this value has N users or more.
	 * @param N
	 * @return
	 */
	public boolean hasNMoreUsers(int N)
	{
		return uses.size() > N;
	}

	/**
	 * Adds one user into user list.
	 * @param user  The user who uses this to be added into uses list.
	 */
	public void addUser(Use user)
	{
		uses.add(user);
	}
}
