package backend.hir;

import backend.lir.ci.LIRKind;
import frontend.codegen.HIRGenModule;
import frontend.type.Type;

/**
 * This class represents a signature of a function, which consists the name of
 * function, parameter list etc.
 * @author Xlous.zeng
 */
public class Signature
{
	private Type ret;
	private String name;
	private Type[] params;

	/**
	 * Constructs  a new signature with given return frontend.type, name of function,
	 * parameter frontend.type list.
	 * @param ret   The return inst frontend.type.
	 * @param name  The name of function.
	 * @param params    The parameter frontend.type list.
	 */
	public  Signature(Type ret, String name, Type[] params)
	{
		this.ret = ret;
		this.name = name;
		this.params = params;
	}

	/**
	 * Gets  the number of arguments in this signature.
	 * @return  The number of arguments.
	 */
	public int argumentCount()
	{
		return this.params.length;
	}

	/**
	 * Acquires the parameter frontend.type at given index.
	 * @param index The index into the parmaeters, with {@code 0} hints that first argument.
	 * @return  The {@code index}'th argument frontend.type.
	 */
	public Type argumentTypeAt(int index)
	{
		assert ( index >= 0 && index < params.length);
		return params[index];
	}

	/**
	 * Acquires the parameter kind at specified index.
	 * @param index The index into the parmaeters, with {@code 0} hints that first argument.
	 * @return  The {@code index}'th argument kind.
	 */
	public LIRKind argumentKindAt(int index)
	{
		assert ( index >= 0 && index < params.length);
		return HIRGenModule.type2Kind(params[index]);
	}

	/**
	 * Gets the return frontend.type of this signature.
	 * @return
	 */
	public Type returnType()
	{
		return ret;
	}

	/**
	 * Gets the return kind of this signature.
	 * @return
	 */
	public LIRKind returnKind()
	{
		return HIRGenModule.type2Kind(ret);
	}
}
