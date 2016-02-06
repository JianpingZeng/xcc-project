package hir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import symbol.Symbol.VarSymbol;
import type.Type;
import type.TypeTags;
import utils.Context;
import utils.Log;
import utils.Pair;
import hir.Operand.*;

/**
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: RegisterFactory.java,v 1.21 2004/10/07 00:07:44 joewhaley Exp $
 */
public class RegisterFactory
{	
	private final ArrayList<Register> registers;
	private final Context context;
	private final Map <Pair,Register> stackNumbering;
	private final Map <Pair,Register> localNumbering;
	
	/**
	 * A logger for error and warning report.
	 */
	private static Log log;

	public RegisterFactory(Context context)
    {
		this.context = context;
		log = Log.instance(context);
		registers = new ArrayList<>();
		stackNumbering = new HashMap<>();
		localNumbering = new HashMap<>();
    }
	
	/** Creates new RegisterFactory */
	public RegisterFactory(Context context, int nStack, int nLocal)
	{				
		this.context = context;
		int capacity = (nStack + nLocal) * 2;
		registers = new ArrayList<>(capacity);		
		stackNumbering = new HashMap<>(nStack);
		localNumbering = new HashMap<>(nLocal);
		log = Log.instance(context);
	}

	/**
	 * Gets the i-th register numberred with i.
	 * @param i
	 * @return
	 */
	public Register get(int i)
	{
		return (Register) registers.get(i);
	}

	/**
	 * A private method for numbering the register.
	 * @return
	 */
	private short nextNumber()
	{
		return (short) registers.size();
	}

	/**
	 * Skip v id.
	 */
	void skipNumbers(int v)
	{
		while (--v >= 0)
			registers.add(null);
	}

	/**
	 * Creates a register based on type with default id and no temporary.
	 *  
	 * @param VarSymbol
	 * @return
	 */
	public Register makeReg(VarSymbol sym)
	{
		Register r = new Register(nextNumber(), sym.type, sym.name.toString(), false);
		registers.add(r);
		return r;
	}

	/**
	 * Creates a register operand with Type type.
	 * @param VarSymbol
	 * @return
	 */
	public RegisterOperand makeRegOp(VarSymbol sym)
	{
		Register r = makeReg(sym);
		registers.add(r);
		return new RegisterOperand(r, sym.type);
	}

	/**
	 * Creates a register where temporary variable stores with Type type.
	 *  
	 * @param type
	 * @return
	 */
	public Register makeTempReg(Type type)
	{
		Register r = new Register(nextNumber(), type, "T", true);
		registers.add(r);
		return r;
	}
	
	/**
	 * Creates a register operand where temporary variable stores with Type type.
	 *  
	 * @param type
	 * @return
	 */
	public RegisterOperand makeTempRegOp(Type type)
	{
		Register r = makeTempReg(type);
		registers.add(r);
		return new RegisterOperand(r, type);
	}

	/**
	 * Creates a guard register operand and return it.
	 * @return
	 */
	public static RegisterOperand makeGuardReg()
	{
		return new RegisterOperand(new Register(), null);
	}

	/**
	 * Clones a register based on existing register r2.
	 * @param r2
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public Register makeReg(Register r2) throws CloneNotSupportedException
	{
		Register r = r2.clone();
		r.index = nextNumber();
		registers.add(r);
		return r;
	}

	/**
	 * Clones a register operand based on existing register r2 and Type instance.
	 * @param r2
	 * @param type
	 * @return
	 * @throws CloneNotSupportedException 
	 */
	public RegisterOperand makeRegOp(Register r2, Type type) 
			throws CloneNotSupportedException
	{
		Register r = r2.clone();
		r.index = nextNumber();
		registers.add(r);
		assert type.isAssignable(r2.getType()); 
		return new RegisterOperand(r, type);
	}

	public Register makePairedReg(RegisterFactory that, Register r2) 
			throws CloneNotSupportedException
	{
		assert(this.size() == that.size());
		Register r = makeReg(r2);
		that.registers.add(r);
		return r;
	}

	public void renumberRegisters(short n)
	{
		for (Iterator<Register> i = registers.iterator(); i.hasNext();)
		{
			Register r = i.next();
			r.setNumber((short) (r.getNumber() + n));
		}
		int oldSize = registers.size();
		for (int i = 0; i < n; ++i)
		{
			registers.add(null);
		}
		for (int i = oldSize - 1; i >= 0; --i)
		{
			registers.set(i + n, registers.get(i));
		}
		while (--n >= 0)
		{
			registers.set(n, null);
		}
	}

	public void addAll(RegisterFactory that)
	{
		for (Iterator<Register> i = that.registers.iterator(); i.hasNext();)
		{
			Register r = i.next();
			r.setNumber((short) nextNumber());
			this.registers.add(r);
		}
	}

	public RegisterFactory deepCopy() throws CloneNotSupportedException
	{
		RegisterFactory that = new RegisterFactory(this.context,
				this.stackNumbering.size(),
		        this.localNumbering.size());
		deepCopyInto(that);
		return that;
	}

	/**
	 * Deeply coping all of registers from this into that.
	 * 
	 * @param that
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public Map<Register, Register> deepCopyInto(RegisterFactory that) 
			throws CloneNotSupportedException
	{
		Map<Register, Register> m = new HashMap<>();
		for (Iterator<Register> i = iterator(); i.hasNext();)
		{
			Register r = i.next();
			Register r2 = r.clone();
			that.registers.add(r2);
			m.put(r, r2);
		}
		renumber(m, this.stackNumbering, that.stackNumbering);
		renumber(m, this.localNumbering, that.localNumbering);
		return m;
	}

	/**
	 * Renumber that register.
	 * 
	 * @param map	A map that maps original register to copy.
	 * @param fromNumbering
	 * @param toNumbering
	 */
	private void renumber(Map<Register, Register> map, 
			Map<Pair, Register> fromNumbering, Map<Pair, Register> toNumbering)
	{
		for (Iterator<Entry<Pair, Register>> i = fromNumbering.entrySet().iterator(); i.hasNext();)
		{
			Entry<Pair, Register> e = i.next();
			Pair p = (Pair) e.getKey();
			Register r = (Register) e.getValue();
			Register r2 = (Register) map.get(r);
			toNumbering.put(p, r2);
		}
	}

	public int numberOfStackRegisters()
	{
		return stackNumbering.size();
	}

	public int numberOfLocalRegisters()
	{
		return localNumbering.size();
	}

	public int size()
	{
		return registers.size();
	}

	public Iterator<Register> iterator()
	{
		return registers.iterator();
	}

	public String toString()
	{
		return "Registers: " + registers.size();
	}

	public String fullDump()
	{
		return "Registers: " + registers.toString();
	}
	
	public static class Register implements Cloneable
	{
		private short index;
		private byte flags;
		/** The name of this register. */
		public String registerName;
		public static final byte TEMP = (byte) 0x20;
		public static final byte SSA = (byte) 0x40;

		public static final byte INT = (byte) 0x01;
		public static final byte FLOAT = INT + 1;
		public static final byte LONG = FLOAT + 1;
		public static final byte DOUBLE = LONG + 1;
		public static final byte GUARD = DOUBLE + 1;
		public static final byte PHYSICAL = GUARD + 1;
		
		// for quickly measuring whether the type is legal or not. 
		public static final byte TYPEMASK = PHYSICAL;

		private Register()
		{
			this.index = -1;
			this.flags = GUARD | TEMP;
			this.registerName = "T" + index;
		}

		private Register(short id, String name, byte flags)
		{
			this.index = id;
			this.registerName = name;
			this.flags = flags;
		}
		
		private Register(short id, Type type, String name, boolean isTemp)
		{
			this.index = id;
			if (isTemp) { 
				flags = TEMP;
				this.registerName = "T" + index;
			}
			else 
				this.registerName = name;
			
			if (type.isIntLike())
				flags |= INT;
			else if (type.tag == TypeTags.FLOAT)
				flags |= FLOAT;
			else if (type.tag == TypeTags.LONG)
				flags |= LONG;
			else if (type.tag == TypeTags.DOUBLE)
				flags |= DOUBLE;
			else
				log.unreachable(type.toString());
		}

		public int getNumber()
		{
			return index;
		}

		public void setNumber(short id)
		{
			this.index = id;
		}

		public boolean isTemp()
		{
			return (flags & TEMP) != 0;
		}

		public void setSSA()
		{
			flags |= SSA;
		}

		public void clearSSA()
		{
			flags &= ~SSA;
		}

		public boolean isSSA()
		{
			return (flags & SSA) != 0;
		}

		public boolean isGuard()
		{
			return (flags & TYPEMASK) == GUARD;
		}

		public boolean isPhysical()
		{
			return (flags & TYPEMASK) == PHYSICAL;
		}

		public Type getType()
		{
			int t = flags & TYPEMASK;
			switch (t)
			{
				case INT:
					return Type.INTType;
				case FLOAT:
					return Type.FLOATType;
				case LONG:
					return Type.LONGType;
				case DOUBLE:
					return Type.DOUBLEType;			
			}
			return null;
		}

		public String toString()
		{
			return registerName;
		}
		
		@Override
		public Register clone() throws CloneNotSupportedException
		{
			Register res = null;
			try
            {
	            res = new Register(this.index, this.registerName, this.flags); 
            }
            catch (Exception e)
            {
            	throw new CloneNotSupportedException();
            }
			return res;
		}
	}

}
