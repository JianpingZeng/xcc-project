package backend.target;

import backend.intrinsic.Intrinsic;
import backend.type.Type;
import backend.value.Function;
import backend.value.Module;

public class TargetIntrinsicInfo
{
    private String[] intrinsics;
    public TargetIntrinsicInfo(String[] info)
    {
        intrinsics = info;
    }

    public int getNumIntrinsics()
    {
        return intrinsics != null ? intrinsics.length: 0;
    }

    public Function getDeclaration(Module m, String builtinName)
    {
        return null;
    }

    public Function getDeclaration(Module m, String builtinName,
                                   Type[] tys)
    {
        return null;
    }

    public boolean isOverloaded(Module m, String[] builtinName)
    {
        return false;
    }

    public Intrinsic.ID getIntrinsicID(Function f)
    {
        return Intrinsic.ID.not_intrinsic;
    }
}
