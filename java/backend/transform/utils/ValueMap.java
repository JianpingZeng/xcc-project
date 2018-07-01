package backend.transform.utils;

import tools.Util;
import backend.transform.scalars.GVNPRE;
import backend.value.Instruction;
import backend.value.Value;
/**
 * This file defines a class that implements a nested hash table that maps
 * {@code Instruction} to an integer.
 * <br>
 * This class is used for global redundant instruction elimination corporated with
 * {@code GVNPRE} class.
 * <br>
 * Thanks to Maxime virtual machine team, because this source code mainly references
 * to it's contribution.
 * <br>
 * Created by Jianping Zeng  on 2016/3/17.
 * @see GVNPRE
 */
public class ValueMap
{
	/**
	 * This class only used for forming hash chain.
	 */
	private static class Link
	{
		final ValueMap map;
		final int valueNumber;
		final Value value;
		final Link next;

		/**
		 * Constructor.
		 * @param map
		 * @param number
		 * @param value
		 * @param next
		 */
		Link(ValueMap map, int number, Value value, Link next)
		{
			this.map = map;
			this.valueNumber = number;
			this.value = value;
			this.next = next;
		}
	}

	private final ValueMap parent;

	/**
	 * The table of links, indexed by {@link Instruction#valueNumber()} method.
	 */
	private Link[] table;

	/**
	 * The numbers of entries in this map.
	 */
	private int count;

	/**
	 * The maximun getArraySize of hash table allowed. It will be resized when load
	 * factor is no less than 0.75.
	 */
	private int max;

	/**
	 * Constructs a new map.
	 */
	public ValueMap()
	{
		parent = null;
		table = new Link[19];
	}

	public ValueMap(ValueMap parent)
	{
		this.parent = parent;
		this.table = parent.table.clone();
		this.count = parent.count;
		this.max = table.length + table.length / 2;
	}


	/**
	 * Inserts a inst into the inst map and looks up any previously available inst.
	 *
	 * @param x the instruction to insert into the inst map
	 * @return the inst with which to replace the specified instruction, or the specified
	 * instruction if there is no replacement
	 */
	public Value findInsert(Value x)
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
				if (l.valueNumber == valueNumber && l.value.valueEqual(x))
				{
					return l.value;
				}
				l = l.next;
			}
			// not found
			// insert at head of chain
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
	 * Kills all VALUES in this local inst map.
	 */
	public void killAll()
	{
		Util.assertion(parent == null, 				"should only used for local number value.");


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

		if (parent != null)
		{
			for (int idx = 0; idx < table.length; ++idx)
			{
				Link current = table[idx];
				// skips all of entries in this map.
				while (current != null && current.map == this)
					current = current.next;
				// copy all of entries in parent map and do insertion into a new
				// hash table.
				while (current != null)
				{
					int index = indexOf(current.valueNumber, ntable);
					ntable[index] = new Link(current.map,current.valueNumber,
							current.value, ntable[index]);

					current = current.next;
				}
			}
		}

		for (int i = 0; i < table.length; i++)
		{
			Link l = table[i];
			// now add all the entries from this map
			while (l != null && l.map == this)
			{
				int index = indexOf(l.valueNumber, ntable);
				ntable[index] = new Link(l.map, l.valueNumber, l.value,
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
}
