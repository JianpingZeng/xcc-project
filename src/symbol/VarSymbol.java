package symbol;

import hir.Instruction;
import type.Type;
import type.TypeTags;
import utils.Name;
import utils.Position;

/**
 * A class for variable symbols
 * @author Jianping Zeng
 */
public class VarSymbol extends Symbol
{

	/**
	 * The flags, such as storage class or qualifier,
	 * which restricted by {@link TypeTags}.
	 */
	public long flags;
	/**
	 * The variable's declaration position.
	 */
	public int pos = Position.NOPOS;

	/**
	 * The variable's address. Used for different purposes during flow
	 * analysis, translation and code generation. Flow analysis: If this is
	 * a blank final or local variable, its sequence id. Translation: If
	 * this is a private field, its access id. Code generation: If this
	 * is a local variable, its logical slot id.
	 */
	public int adr = -1;

	/**
	 * The variable's constant value, if this is a constant. Before the
	 * constant value is evaluated, it points to an initalizer environment.
	 */
	public Object constValue;

	/**
	 * The corresponding allocated Local or Global instruction for this
	 * variable in HIR.
	 */
	public Instruction.Alloca varInst;

	/**
	 * Construct a variable symbol, given its flags, name, type and owner.
	 */
	public VarSymbol(long flags, Name name, Type type)
	{
		super(VAR, name, type);
		this.flags = flags;
	}

	/**
	 * Clone this symbol.
	 */
	public Symbol clone()
	{
		VarSymbol v = new VarSymbol(flags, name, type);
		v.pos = pos;
		v.adr = adr;
		v.constValue = constValue;
		return v;
	}

	public String toString()
	{
		return "variable " + name;
	}
}
