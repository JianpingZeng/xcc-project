package jlang.symbol;

import hir.Instruction;
import jlang.type.Type;
import jlang.type.TypeClass;
import tools.Name;
import tools.Position;

/**
 * A class for variable symbols
 * @author Xlous.zeng
 */
public class VarSymbol extends Symbol
{

	/**
	 * The flags, such as storage class or qualifier,
	 * which restricted by {@link TypeClass}.
	 */
	public long flags;
	/**
	 * The variable's declaration position.
	 */
	public int pos = Position.NOPOS;

	/**
	 * The variable's address. Used for different purposes during flow
	 * analysis, translation and code generation. Flow analysis: IfStmt this is
	 * a blank final or local variable, its sequence id. Translation: IfStmt
	 * this is a private field, its access id. Code generation: IfStmt this
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
	 * variable in Module.
	 */
	public Instruction.AllocaInst varInst;

	/**
	 * Construct a variable jlang.symbol, given its flags, getName, jlang.type and owner.
	 */
	public VarSymbol(long flags, Name name, Type type)
	{
		super(VAR, name, type);
		this.flags = flags;
	}

	/**
	 * Clone this jlang.symbol.
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
