package hir;

/**
 * The {@code ValueMap} class implements a nested hashtable data structure
 * for use in local and global inst numbering.
 * <p>
 * Thanks to Maxime virtual machine team, because this source code mainly references
 * to it's contribution.
 * Created by Jianping Zeng<z1215jping@hotmail.com> on 2016/2/25.
 */
public class ValueMap
{
	/**
	 * The table of links, indexed by hashing using the {@link Instruction#valueNumber field}.
	 * The hash chains themselves may share parts of the parents' hash chains at the end.
	 */
	private Link[] table;

	/**
	 * Total number of entries in this map and the parent, used to compute unique ids.
	 */
	private int count;

	/**
	 * The maximum size allowed before triggering resizing.
	 */
	private int max;

	/**
	 * Creates a new inst map.
	 */
	public ValueMap()
	{
		table = new Link[19];
	}

	/**
	 * Inserts a inst into the inst map and looks up any previously available inst.
	 *
	 * @param x the instruction to insert into the inst map
	 * @return the inst with which to replace the specified instruction, or the specified
	 * instruction if there is no replacement
	 */
	public Instruction findInsert(Instruction x)
	{
		int valueNumber = x.valueNumber();

		// take insertion of current instruction x into Links list,
		// ignoring it if it's value number is equal to zero that means this
		// instruction is not referenced by other instruction.
		if (valueNumber != 0)
		{
			// inst number != 0 means the instruction can be inst numbered
			int index = indexOf(valueNumber, table);
			Link l = table[index];
			// hash and linear search
			while (l != null)
			{
				if (l.valueNumber == valueNumber && l.inst.valueEqual(x))
				{
					return l.inst;
				}
				l = l.next;
			}
			// not found; insert
			table[index] = new Link(this, valueNumber, x, table[index]);

			// resize it if it's count is larger than max
			if (count > max)
			{
				resize();
			}
		}
		return x;
	}

	/**
	 * Kills all values in this local inst map.
	 */
	public void killAll()
	{
		for (int i = 0; i < table.length; i++)
		{
			table[i] = null;
		}
		count = 0;
	}

	/**
	 * Resizing the hash table where all of map stores.
	 */
	private void resize()
	{
		Link[] ntable = new Link[table.length * 3 + 4];
		for (int i = 0; i < table.length; i++)
		{
			Link l = table[i];
			// now add all the entries from this map
			while (l != null && l.map == this)
			{
				int index = indexOf(l.valueNumber, ntable);
				ntable[index] = new Link(l.map, l.valueNumber, l.inst,
						ntable[index]);
				l = l.next;
			}
		}
		table = ntable;
		max = table.length + table.length / 2;
	}

	private int indexOf(int valueNumber, Link[] t)
	{
		return (valueNumber & 0x7fffffff) % t.length;
	}

	/**
	 * The class that forms hash chains.
	 */
	private static class Link
	{
		/**
		 * The {@code ValueMap} that is owning this link object.
		 */
		final ValueMap map;
		/**
		 * Value numbering of this link object.
		 */
		final int valueNumber;
		/**
		 * Corresponding instruction.
		 */
		final Instruction inst;
		/**
		 * The next link ojbect.
		 */
		final Link next;

		/**
		 * Constrcuts a new link instance.
		 * @param map   The ValueMap object.
		 * @param valueNumber   The corresponding value numbering.
		 * @param inst  The instruction.
		 * @param next  The next link ojbect.
		 */
		Link(ValueMap map, int valueNumber, Instruction inst, Link next)
		{
			this.map = map;
			this.valueNumber = valueNumber;
			this.inst = inst;
			this.next = next;
		}
	}
}

