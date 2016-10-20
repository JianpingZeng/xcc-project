package comp;

import java.util.Arrays;

import exception.CompletionFailure;
import ast.Flags;
import utils.*;
import symbol.*;
import symbol.TypeSymbol;
import symbol.Symbol.*;
import type.*;

/**
 * A class that defines all predefined constants and operators as well as
 * special classes such as java.lang.Object, which need to be known to the
 * compiler. All symbols are held in instance fields. This makes it possible to
 * work in multiple concurrent projects, which might use different class files
 * for library classes.
 */
public class Symtab implements Flags, OpCodes
{

	/**
	 * The context key for the symbol table.
	 */
	private static final Context.Key symtabKey = new Context.Key();

	/**
	 * Get the symbol table instance.
	 */
	public static Symtab instance(Context context)
	{
		Symtab instance = (Symtab) context.get(symtabKey);
		if (instance == null) instance = new Symtab(context);
		return instance;
	}

	private final Name.Table names;

	/**
	 * A symbol that stands for a missing symbol.
	 */
	public final TypeSymbol noSymbol;

	/**
	 * The error symbol.
	 */
	public final TypeSymbol errSymbol;

	/**
	 * Builtin types.
	 */
	public final Type byteType;
	public final Type charType;
	public final Type shortType;
	public final Type intType;
	public final Type longType;
	public final Type floatType;
	public final Type doubleType;
	public final Type boolType;
	public final Type voidType;
	public final Type errType;

	public final TopLevelSymbol predefTopLevelSymbol;

	public final TypeSymbol methodClass;
	
	public final TypeSymbol arrayClass;
	/**
	 * A value for the unknown type.
	 */
	public final Type unknownType;

	/**
	 * The predefined type that belongs to a tag.
	 */
	public final Type[] typeOfTag = new Type[TypeClass.TypeTagCount];

	public void initType(Type type, TypeSymbol c)
	{
		type.tsym = c;
		typeOfTag[type.tag] = type;
	}

	public void initType(Type type, String name)
	{
		initType(type, new TypeSymbol(names.fromString(name), type));
	}

	/**
	 * Enter a binary operation, as above but with two opcodes, which get
	 * encoded as (opcode1 << ByteCodeTags.preShift) + opcode2.
	 * @param opcode The opecode
	 */
	private void enterBinop(String name, Type left, Type right, Type res,
	        int opcode)
	{
		OperatorSymbol sym = new OperatorSymbol(0,
		        names.fromString(name),
		        new FunctionType(res, Arrays.asList(left, right), methodClass),
		        opcode);
		predefTopLevelSymbol.topScope.enter(sym);
	}
	
	/**
	 * Enter a binary operation, as above but with two opcodes, which get
	 * encoded as (opcode1 << OpCodes.preShift) + opcode2.
	 * @param opcode The opecode
	 */
	private void enterBinop(String name, Type left, Type right, Type res,
	        int opcode1, int opcode2)
	{
		OperatorSymbol sym = new OperatorSymbol(0,
		        names.fromString(name),
		        new FunctionType(res, Arrays.asList(left, right), methodClass),
		        opcode1 >> OpCodes.preShift + opcode2);
		predefTopLevelSymbol.topScope.enter(sym);
	}

	/**
	 * Enter a unary operation into symbol table.
	 * @param name The name of the operator.
	 * @param arg The type of the LIROperand.
	 * @param res The operation's result type.
	 * @param opcode The operation's bytecode instruction.
	 */
	private OperatorSymbol enterUnop(String name, Type arg, Type res, int opcode)
	{
		OperatorSymbol sym = new OperatorSymbol(0,
		        names.fromString(name),
		        new FunctionType(res, Arrays.asList(arg), methodClass),
		        opcode);
		predefTopLevelSymbol.topScope.enter(sym);
		return sym;
	}

	
	
	/**
	 * Constructor; enters all predefined identifiers and operators into symbol
	 * table.
	 */
	private Symtab(Context context) throws CompletionFailure
	{
		super();
		context.put(symtabKey, this);
		names = Name.Table.instance(context);
		byteType = new Type(TypeClass.BYTE, null);
		charType = new Type(TypeClass.Char, null);
		shortType = new Type(TypeClass.Short, null);
		intType = new Type(TypeClass.Int, null);
		longType = new Type(TypeClass.LongInteger, null);
		floatType = new Type(TypeClass.FLOAT, null);
		doubleType = new Type(TypeClass.DOUBLE, null);
		boolType = new Type(TypeClass.BOOL, null);
		voidType = new Type(TypeClass.Void, null);
		unknownType = new Type(TypeClass.UNKNOWN, null)
		{

			public boolean isSameType(Type that)
			{
				return true;
			}

			public boolean isSubType(Type that)
			{
				return false;
			}

			public boolean isSuperType(Type that)
			{
				return true;
			}
		};
		noSymbol = new TypeSymbol(names.empty, Type.noType);
		noSymbol.kind = SymbolKinds.NIL;
		errSymbol = new Symbol.ErrorSymbol(names.any);
		errType = new ErrorType(errSymbol);
		methodClass = new TypeSymbol(names.Method, Type.noType);
		arrayClass = new TypeSymbol(names.Array, Type.noType);				
		initType(byteType, "byte");
		initType(shortType, "short");
		initType(charType, "char");
		initType(intType, "int");
		initType(longType, "long");
		initType(floatType, "float");
		initType(doubleType, "double");
		initType(boolType, "bool");
		initType(voidType, "void");
		initType(errType, errSymbol);
		initType(unknownType, "<any?>");

		this.predefTopLevelSymbol = new TopLevelSymbol(names.empty);
		Scope scope = new Scope(predefTopLevelSymbol);
		predefTopLevelSymbol.topScope = scope;
		scope.enter(byteType.tsym);
		scope.enter(shortType.tsym);
		scope.enter(intType.tsym);
		scope.enter(longType.tsym);
		scope.enter(floatType.tsym);
		scope.enter(doubleType.tsym);
		scope.enter(boolType.tsym);
		scope.enter(errType.tsym);
		
		enterUnop("+", intType, intType, nop);
        enterUnop("+", longType, longType, nop);
        enterUnop("+", floatType, floatType, nop);
        enterUnop("+", doubleType, doubleType, nop);
        enterUnop("-", intType, intType, ineg);
        enterUnop("-", longType, longType, lneg);
        enterUnop("-", floatType, floatType, fneg);
        enterUnop("-", doubleType, doubleType, dneg);
        enterUnop("~", intType, intType, ixor);
        enterUnop("~", longType, longType, lxor);
        enterUnop("++", byteType, byteType, iadd);
        enterUnop("++", shortType, shortType, iadd);
        enterUnop("++", charType, charType, iadd);
        enterUnop("++", intType, intType, iadd);
        enterUnop("++", longType, longType, ladd);
        enterUnop("++", floatType, floatType, fadd);
        enterUnop("++", doubleType, doubleType, dadd);
        enterUnop("--", byteType, byteType, isub);
        enterUnop("--", shortType, shortType, isub);
        enterUnop("--", charType, charType, isub);
        enterUnop("--", intType, intType, isub);
        enterUnop("--", longType, longType, lsub);
        enterUnop("--", floatType, floatType, fsub);
        enterUnop("--", doubleType, doubleType, dsub);
        enterUnop("!", boolType, boolType, bool_not);
        enterBinop("+", intType, intType, intType, iadd);
        enterBinop("+", longType, longType, longType, ladd);
        enterBinop("+", floatType, floatType, floatType, fadd);
        enterBinop("+", doubleType, doubleType, doubleType, dadd);
        enterBinop("-", intType, intType, intType, isub);
        enterBinop("-", longType, longType, longType, lsub);
        enterBinop("-", floatType, floatType, floatType, fsub);
        enterBinop("-", doubleType, doubleType, doubleType, dsub);
        enterBinop("*", intType, intType, intType, imul);
        enterBinop("*", longType, longType, longType, lmul);
        enterBinop("*", floatType, floatType, floatType, fmul);
        enterBinop("*", doubleType, doubleType, doubleType, dmul);
        enterBinop("/", intType, intType, intType, idiv);
        enterBinop("/", longType, longType, longType, ldiv);
        enterBinop("/", floatType, floatType, floatType, fdiv);
        enterBinop("/", doubleType, doubleType, doubleType, ddiv);
        enterBinop("%", intType, intType, intType, imod);
        enterBinop("%", longType, longType, longType, lmod);
        enterBinop("%", floatType, floatType, floatType, fmod);
        enterBinop("%", doubleType, doubleType, doubleType, dmod);
        enterBinop("&", boolType, boolType, boolType, iand);
        enterBinop("&", intType, intType, intType, iand);
        enterBinop("&", longType, longType, longType, land);
        enterBinop("|", boolType, boolType, boolType, ior);
        enterBinop("|", intType, intType, intType, ior);
        enterBinop("|", longType, longType, longType, lor);
        enterBinop("^", boolType, boolType, boolType, ixor);
        enterBinop("^", intType, intType, intType, ixor);
        enterBinop("^", longType, longType, longType, lxor);
        enterBinop("<<", intType, intType, intType, ishl);
        enterBinop("<<", longType, intType, longType, lshl);
        enterBinop("<<", intType, longType, intType, ishll);
        enterBinop("<<", longType, longType, longType, lshll);
        enterBinop(">>", intType, intType, intType, ishr);
        enterBinop(">>", longType, intType, longType, lshr);
        enterBinop(">>", intType, longType, intType, ishrl);
        enterBinop(">>", longType, longType, longType, lshrl);
        enterBinop(">>>", intType, intType, intType, iushr);
        enterBinop(">>>", longType, intType, longType, lushr);
        enterBinop(">>>", intType, longType, intType, iushrl);
        enterBinop(">>>", longType, longType, longType, lushrl);
        enterBinop("<", intType, intType, boolType, if_icmplt)
        ;
        enterBinop("<", longType, longType, boolType, lcmp, iflt);
        enterBinop("<", floatType, floatType, boolType, fcmpg, iflt);
        enterBinop("<", doubleType, doubleType, boolType, dcmpg, iflt);
        enterBinop(">", intType, intType, boolType, if_icmpgt)
        ;
        enterBinop(">", longType, longType, boolType, lcmp, ifgt);
        enterBinop(">", floatType, floatType, boolType, fcmpl, ifgt);
        enterBinop(">", doubleType, doubleType, boolType, dcmpl, ifgt);
        enterBinop("<=", intType, intType, boolType, if_icmple)
        ;
        enterBinop("<=", longType, longType, boolType, lcmp, ifle);
        enterBinop("<=", floatType, floatType, boolType, fcmpg, ifle);
        enterBinop("<=", doubleType, doubleType, boolType, dcmpg, ifle);
        enterBinop(">=", intType, intType, boolType, if_icmpge)
        ;
        enterBinop(">=", longType, longType, boolType, lcmp, ifge);
        enterBinop(">=", floatType, floatType, boolType, fcmpl, ifge);
        enterBinop(">=", doubleType, doubleType, boolType, dcmpl, ifge);
        enterBinop("==", intType, intType, boolType, if_icmpeq)
        ;
        enterBinop("==", longType, longType, boolType, lcmp, ifeq);
        enterBinop("==", floatType, floatType, boolType, fcmpl, ifeq);
        enterBinop("==", doubleType, doubleType, boolType, dcmpl, ifeq);
        enterBinop("==", boolType, boolType, boolType, if_icmpeq);
        
        enterBinop("!=", intType, intType, boolType, if_icmpne);
        
        enterBinop("!=", longType, longType, boolType, lcmp, ifne);
        enterBinop("!=", floatType, floatType, boolType, fcmpl, ifne);
        enterBinop("!=", doubleType, doubleType, boolType, dcmpl, ifne);
        enterBinop("!=", boolType, boolType, boolType, if_icmpne);
        
        enterBinop("&&", boolType, boolType, boolType, bool_and);
        enterBinop("||", boolType, boolType, boolType, bool_or);
		
	}
}
