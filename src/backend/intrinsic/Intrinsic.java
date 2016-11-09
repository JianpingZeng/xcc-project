package backend.intrinsic;

import backend.hir.Module;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.Function;

import java.util.ArrayList;

/**
 * Intrinsic instruction.
 * @author Xlous.zeng
 * @version 0.1
 */
public class Intrinsic
{
	public enum ID
	{
		not_intrinsic("non_intrinsic"),
		memcpy("memcpy"),
		memmove("memmove"),
		memset("memset"),

		num_intrinsics("num_intrinsics");

		String name;
		ID(String  name)
		{this.name = name;}
	}

	public static ID getID(int id) {return ID.values()[id];}

	public static String getName(ID id, ArrayList<Type> types)
	{
		if (types.isEmpty())
			return id.name;

		String result = id.name;
		for (Type ty : types)
		{
			if (ty instanceof PointerType)
			{
				result += ".p" + Type.getString(((PointerType)ty).getElemType());
			}
			else
				result += "." + Type.getString(ty);
		}
		return result;
	}

	public static backend.type.FunctionType getType(ID id, ArrayList<Type> types)
	{
		return null;//TODO
	}

	public static Function getDeclaration(Module m, ID id, ArrayList<Type> types)
	{
		// We must to ensure that the intrinsic function can have only one global declaration.
		return (Function) m.getOrInsertFunction(getName(id, types), getType(id, types));
	}
}
