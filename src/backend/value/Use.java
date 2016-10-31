package backend.value;

/**
 * <p>This file defines a class that keeps track of the use-list for given
 * {@code Value} up to date.
 * <p>Also, the class {@Use} represents a edge linked from value definition to
 * its all usesList list.
 * <p>
 * @see Value
 * @author Xlous.zeng
 */
public class Use
{
	/**
	 * The value definition of this use link.
	 */
	private Value val;

	private User user;

	public Use(Value val, User user)
	{
		this.val = val;
		this.user = user;
		if (val != null)
			val.addUse(this);
	}

	public Value getValue()
	{
		return val;
	}

	public void setValue(Value v)
	{
		if (val != null)
			val.killUse(this);
		val = v;
		if (v != null)
			v.addUse(this);
	}

	public User getUser()
	{
		return user;
	}


}
