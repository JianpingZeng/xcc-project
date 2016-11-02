package frontend.symbol;

import tools.Name;

/**
 * A scope represents an area of visibility in a C-flat program. The
 * Scope class is a container for symbols which provides
 * efficient access to symbols given their names.
 * <p>
 * Note that the index of Name {@see Name} be used as hash value rather than
 * the string represents name.
 * <p>
 * Scopes are implemented
 * as hash tables. Scopes can be nested; the next field of a scope points
 * to its next outerLoop scope. Nested scopes can share their hash tables.
 */
public class Scope
{
	/**
	 * Next enclosing scope with whom this scope shares a hashtable.
	 * <p>
	 * Travels the nested scope from current scope to outerLoop which denoted as next field.
	 */
	public Scope next;

	/**
	 * The owner enclosing this scope.
	 */
	public Symbol owner;

	/**
	 * A hash entityTable for the scope's entries.
	 */
	public Entry[] entityTable;

	/**
	 * Mask for hash codes, always equal to (entityTable.getArraySize - 1).
	 */
	int hashMask;

	/**
	 * A linear list that also isDeclScope all entries in
	 * reverse order of appearance (i.e later entries are pushed on top).
	 */
	public Entry elems;

	/**
	 * The id of elements in this scope.
	 */
	public int nelems = 0;

	/**
	 * Every hash bucket is a list of Entry's which ends in sentinel.
	 */
	private static final Entry sentinel = new Entry(null, null, null, null);

	/**
	 * The hash entityTable's initial getArraySize.
	 */
	private static final int INITIAL_SIZE = 128;

	/**
	 * A value for the empty scope.
	 */
	public static final Scope emptyScope = new Scope(null, null, new Entry[] {});

	/**
	 * Construct a new scope, within scope next, with given owner, using
	 * given entityTable. The entityTable's getArraySize must be an exponent of 2.
	 */
	Scope(Scope next, Symbol owner,  Entry[] table)
	{
		super();
		this.next = next;
		this.owner = owner;
		this.entityTable = table;
		this.hashMask = table.length - 1;
		this.elems = null;
		this.nelems = 0;
	}

	/**
	 * Construct a new scope at which all of entity stores is sentinel,
	 * with next field is null using a fresh entityTable of getArraySize INITIAL_SIZE.
	 */
	public Scope(Symbol owner)
	{
		this(null, owner, new Entry[INITIAL_SIZE]);
		for (int i = 0; i < INITIAL_SIZE; i++)
			entityTable[i] = sentinel;
	}

	/**
	 * Construct a fresh scope within this scope, with same next filed,
	 * which shares its entityTable with the outerLoop scope. Used in connection with
	 * method leave if scope access is stack-like in order to avoid allocation
	 * of fresh tables.
	 */
	public Scope dup()
	{
		return new Scope(this, owner, this.entityTable);
	}

	/**
	 * Construct a fresh scope within this scope, with same owner,
	 * with a new hash entityTable, whose contents initially are those of
	 * the entityTable of its outerLoop scope.
	 */
	public Scope dupUnshared()
	{
		return new Scope(this, owner, this.entityTable.clone());
	}

	/**
	 * Remove all entries of this scope from its entityTable and then return next scope.
	 */
	public Scope leave()
	{
		while (elems != null)
		{
			int hash = elems.sym.name.index & hashMask;
			Entry e = entityTable[hash];
			assert e == elems : elems.sym;
			entityTable[hash] = elems.shadowed;
			elems = elems.sibling;
		}
		return next;
	}

	/**
	 * Double getArraySize of hash entityTable.
	 */
	private void dble()
	{
		Entry[] oldtable = entityTable;
		Entry[] newtable = new Entry[oldtable.length * 2];
		Scope s = this;
		do
		{
			s.entityTable = newtable;
			s.hashMask = newtable.length - 1;
			s = s.next;
		} while (s != null);

		for (int i = 0; i < newtable.length; i++)
			newtable[i] = sentinel;
		for (int i = 0; i < oldtable.length; i++)
			copy(oldtable[i]);
	}

	/**
	 * Copy the given entry and all entries shadowed by it to entityTable
	 */
	private void copy(Entry e)
	{
		if (e.sym != null)
		{
			copy(e.shadowed);
			int hash = e.sym.name.index & hashMask;
			e.shadowed = entityTable[hash];
			entityTable[hash] = e;
		}
	}

	/**
	 * Enter frontend.symbol sym in this scope.
	 */
	public void enter(Symbol sym)
	{
		enter(sym, this);
	}

	/**
	 * Enter frontend.symbol sym in this scope, but mark that it comes from
	 * given scope `s'. This is used to enter entries in import scopes.
	 */
	public void enter(Symbol sym, Scope s)
	{

		// double the getArraySize of entity entityTable if the loading factor no greater than 2/3
		if (nelems * 3 >= hashMask * 2)
			dble();
		int hash = sym.name.index & hashMask;
		Entry e = new Entry(sym, entityTable[hash], elems, s);
		entityTable[hash] = e;
		elems = e;
		nelems++;
	}

	/**
	 * Enter frontend.symbol sym in this scope if not already there.
	 */
	public void enterIfAbsent(Symbol sym)
	{
		Entry e = lookup(sym.name);
		// firstly, search the frontend.symbol its name as same as given sym
		// finally, the e should be sentinel if given sym is not present.
		while (e.scope == this && e.sym.kind != sym.kind)
			e = e.next();
		// enter the given sym into current scope.
		if (e.scope != this)
			enter(sym);
	}

	/**
	 * ReturnInst the entry associated with given name, starting in
	 * this scope and proceeding outwards. IfStmt no entry was found,
	 * return the sentinel, which is characterized by having a null in
	 * both its scope and sym fields, whereas both fields are non-null
	 * for regular entries.
	 */
	public Entry lookup(Name name)
	{
		Entry e = entityTable[name.index & hashMask];
		while (e.scope != null && e.sym.name != name)
			e = e.shadowed;
		return e;
	}

	/**
	 * A class for scope entries.
	 */
	public static class Entry
	{

		/**
		 * The referenced frontend.symbol.
		 * sym == null   iff   this == sentinel
		 */
		public Symbol sym;

		/**
		 * An entry with the same hash code, or sentinel.
		 */
		public Entry shadowed;

		/**
		 * Next entry in same scope.
		 */
		public Entry sibling;

		/**
		 * The entry's scope.
		 * scope == null   iff   this == sentinel.
		 */
		public Scope scope;

		public Entry(Symbol sym, Entry shadowed, Entry sibling, Scope scope)
		{
			super();
			this.sym = sym;
			this.shadowed = shadowed;
			this.sibling = sibling;
			this.scope = scope;
		}

		/**
		 * ReturnInst next entry with the same name as this entry, proceeding
		 * outwards if not found in this scope.
		 */
		public Entry next()
		{
			Entry e = shadowed;
			while (e.scope != null && e.sym.name != sym.name)
				e = e.shadowed;
			return e;
		}
	}

	/**
	 * An error scope, for which the owner should be an error frontend.symbol.
	 */
	public static class ErrorScope extends Scope
	{

		ErrorScope(Scope next, Symbol owner, Entry[] table)
		{
			super(next,owner, table);
		}

		public Scope dup()
		{
			return new ErrorScope(this, owner, entityTable);
		}

		public Scope dupUnshared()
		{
			return new ErrorScope(this, owner, entityTable.clone());
		}

		public Entry lookup(Name name)
		{
			Entry e = super.lookup(name);
			if (e.scope == null)
				return new Entry(null, null, null, null);
			else
				return e;
		}
	}
}
