package backend.intrinsic;

import backend.support.LLVMContext;
import backend.type.FunctionType;
import backend.type.PointerType;
import backend.type.Type;
import backend.value.Function;
import backend.value.Module;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Intrinsic instruction.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class Intrinsic {
  public enum ID {
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
    stackrestore("llvm.stackrestore"),
    stackprotector("llvm.stackprotector"),
    trap("llvm.trap"),
    uadd_with_overflow("llvm.uadd.with.overflow"),
    sadd_with_overflow("llvm.sadd.with.overflow"),
    usub_with_overflow("llvm.usub.with.overflow"),
    ssub_with_overflow("llvm.ssub.with.overflow"),
    umul_with_overflow("llvm.umul.with.overflow"),
    smul_with_overflow("llvm.smul.with.overflow"),
    num_intrinsics("num_intrinsics");

    public String name;

    ID(String name) {
      this.name = name;
    }
  }

  public static ID getID(int id) {
    return ID.values()[id];
  }

  public static String getName(ID id) {
    return id.name;
  }

  public static String getName(ID id, ArrayList<Type> types) {
    if (types.isEmpty())
      return id.name;

    StringBuilder result = new StringBuilder(id.name);
    for (Type ty : types) {
      if (ty instanceof PointerType) {
        result.append(".p").append((((PointerType) ty).getElementType()).getDescription());
      } else
        result.append(".").append(ty.getDescription());
    }
    return result.toString();
  }

  public static backend.type.FunctionType getType(ID id) {
    return getType(id, null);
  }

  public static backend.type.FunctionType getType(ID id, ArrayList<Type> types) {
    backend.type.Type resultTy = null;
    ArrayList<Type> argTys = new ArrayList<>();
    boolean isVararg = false;
    switch (id) {
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
      case stacksave:
        resultTy = PointerType.getUnqual(LLVMContext.Int8Ty);
        break;
      case stackrestore:
        resultTy = LLVMContext.VoidTy;
        argTys.add(PointerType.getUnqual(LLVMContext.Int8Ty));
        break;
      default:
        Util.shouldNotReachHere("Unknown intrinsic function!");
        break;
    }
    return FunctionType.get(resultTy, argTys, isVararg);
  }

  public static Function getDeclaration(Module m, ID id, ArrayList<Type> types) {
    // We must to ensure that the intrinsic function can have only one global declaration.
    return (Function) m.getOrInsertFunction(getName(id, types), getType(id, types));
  }

  public static Function getDeclaration(Module m, ID id, Type[] tys) {
    ArrayList<Type> res = new ArrayList<>();
    res.addAll(Arrays.asList(tys));
    return (Function) m.getOrInsertFunction(getName(id, res), getType(id, res));
  }

  public static Function getDeclaration(Module m, ID id) {
    return (Function) m.getOrInsertFunction(getName(id), getType(id));
  }
}
