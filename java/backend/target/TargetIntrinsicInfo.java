package backend.target;

import backend.intrinsic.Intrinsic;
import backend.type.Type;
import backend.value.Function;
import backend.value.Module;

public abstract class TargetIntrinsicInfo {
  private String[] intrinsics;

  public TargetIntrinsicInfo(String[] info) {
    intrinsics = info;
  }

  public abstract int getNumIntrinsics();

  public abstract Function getDeclaration(Module m, String builtinName);

  public abstract Function getDeclaration(Module m, String builtinName, Type[] tys);

  public abstract boolean isOverloaded(Module m, String[] builtinName);

  public Intrinsic.ID getIntrinsicID(Function f) {
    return Intrinsic.ID.not_intrinsic;
  }

  public abstract String getName(int iid, Type... tys);
}
