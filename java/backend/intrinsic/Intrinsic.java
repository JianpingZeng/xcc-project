package backend.intrinsic;

import backend.support.LLVMContext;
import backend.type.FunctionType;
import backend.type.IntegerType;
import backend.value.Module;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.Function;
import tools.Util;

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
		memcpy("llvm.memcpy"),
		memmove("llvm.memmove"),
		memset("llvm.memset"),
		vastart("llvm.vastart"),
		vaend("llvm.vaend"),
		vacopy("llvm.vacopy"),
		returnaddress("llvm.returnaddress"),
		frameaddress("llvm.frameaddress"),
		setjmp("llvm.setjmp"),
		longjmp("llvm.longjmp"),
		sqrt("llvm.sqrt"),
		powi("llvm.powi"),
		sin("llvm.sin"),
		cos("llvm.cos"),
		log("llvm.log"),
		log2("llvm.log2"),
		log10("llvm.log10"),
		exp("llvm.exp"),
		exp2("llvm.exp2"),
		pow("llvm.pow"),
		pcmarker("llvm.pcmarker"),
		bswap("llvm.bswap"),
		cttz("llvm.cttz"),
		ctlz("llvm.ctlz"),
		ctpop("llvm.ctpop"),
		stacksave("llvm.stacksave"),
		stackstore("llvm.stackstore"),
		stackprotector("llvm.stackprotector"),
		trap("llvm.trap"),
		uadd_with_overflow("llvm.uadd_with_overflow"),
		sadd_with_overflow("llvm.sadd_with_overflow"),
		usub_with_overflow("llvm.usub_with_overflow"),
		ssub_with_overflow("llvm.ssub_with_overflow"),
		umul_with_overflow("llvm.umul_with_overflow"),
		smul_with_overflow("llvm.smul_with_overflow"),
		num_intrinsics("num_intrinsics");

		public String name;
		ID(String  name)
		{this.name = name;}
	}

	public static ID getID(int id) {return ID.values()[id];}

	public static String getName(ID id, ArrayList<Type> types)
	{
		if (types.isEmpty())
			return id.name;

		StringBuilder result = new StringBuilder(id.name);
		for (Type ty : types)
		{
			if (ty instanceof PointerType)
			{
				result.append(".p").append((((PointerType) ty).getElementType()).getDescription());
			}
			else
				result.append(".").append(ty.getDescription());
		}
		return result.toString();
	}

	public static backend.type.FunctionType getType(ID id, ArrayList<Type> types)
	{
		backend.type.Type resultTy = null;
		ArrayList<Type> argTys = new ArrayList<>();
		boolean isVararg = false;
		switch (id)
		{
			case memcpy:
			case memmove:
				// llvm.memcpy
				// llvm.memmove
				resultTy = LLVMContext.VoidTy;
				argTys.add(PointerType.getUnqual(LLVMContext.Int8Ty));
				argTys.add(PointerType.getUnqual(LLVMContext.Int8Ty));
				argTys.add(types.get(0));
				argTys.add(LLVMContext.Int32Ty);
				break;
			case memset:
				// llvm.memset
				resultTy = LLVMContext.VoidTy;
				argTys.add(PointerType.getUnqual(LLVMContext.Int8Ty));
				argTys.add(LLVMContext.Int8Ty);
				argTys.add(types.get(0));
				argTys.add(LLVMContext.Int32Ty);
				break;
			default:
				Util.shouldNotReachHere("Unknown intrinsic function!");
				break;
		}
		return FunctionType.get(resultTy, argTys, isVararg);
	}

	public static Function getDeclaration(Module m, ID id, ArrayList<Type> types)
	{
		// We must to ensure that the intrinsic function can have only one global declaration.
		return (Function) m.getOrInsertFunction(getName(id, types), getType(id, types));
	}
}
