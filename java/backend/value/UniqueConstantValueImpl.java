package backend.value;

import backend.type.Type;
import gnu.trove.list.array.TIntArrayList;
import jlang.type.FoldingSetNodeID;
import tools.APFloat;
import tools.APInt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static backend.value.Instruction.CmpInst.Predicate.FCMP_FALSE;

/**
 * This class is used for keep track of unique constant object for specified
 * integer, float, struct, array etc.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class UniqueConstantValueImpl
{
    private final static HashMap<ExprMapKeyType, Constant> exprConstantMaps
            = new HashMap<>();

    private final static HashMap<APIntKeyInfo, ConstantInt> intConstants;

    private static final HashMap<APFloatKeyInfo, ConstantFP> FPConstants =
            new HashMap<>();

    private static HashMap<Type, ConstantPointerNull> nullPtrConstants;


    private UniqueConstantValueImpl()
    {}

    public static final UniqueConstantValueImpl uniqueImpl = new UniqueConstantValueImpl();

    public Constant getOrCreate(ExprMapKeyType key)
    {
        return null;
    }

    public static class ExprMapKeyType
    {
        Operator opcode;
        Instruction.CmpInst.Predicate predicate;
        ArrayList<Constant> operands;
        TIntArrayList indices;

        public ExprMapKeyType(Operator opc, Constant op)
        {
            this(opc, op, FCMP_FALSE);
        }

        public ExprMapKeyType(Operator opc, Constant op, Instruction.CmpInst.Predicate pred)
        {
            this(opc, op, pred, new TIntArrayList());
        }

        public ExprMapKeyType(Operator opc, Constant op, Instruction.CmpInst.Predicate pred, TIntArrayList indices)
        {
            opcode = opc;
            predicate = pred;
            operands = new ArrayList<>();
            operands.add(op);
            this.indices = new TIntArrayList();
            this.indices.addAll(indices);
        }

        public ExprMapKeyType(Operator opc, List<Constant> ops, Instruction.CmpInst.Predicate pred)
        {
            this(opc, ops, pred, new TIntArrayList());
        }

        public ExprMapKeyType(Operator opc, List<Constant> ops)
        {
            this(opc, ops, Instruction.CmpInst.Predicate.FCMP_FALSE);
        }

        public ExprMapKeyType(Operator opc, List<Constant> ops,
                Instruction.CmpInst.Predicate pred, TIntArrayList indices)
        {
            opcode = opc;
            predicate = pred;
            operands = new ArrayList<>();
            operands.addAll(ops);
            this.indices = new TIntArrayList();
            this.indices.addAll(indices);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (getClass() != obj.getClass())
                return false;
            ExprMapKeyType key = (ExprMapKeyType)obj;
            return opcode == key.opcode && predicate == key.predicate
                    && operands.equals(key.operands) && indices.equals(key.indices);
        }

        @Override
        public int hashCode()
        {
            FoldingSetNodeID id = new FoldingSetNodeID();
            id.addInteger(opcode.hashCode());
            id.addInteger(predicate.hashCode());
            id.addInteger(operands.size());
            operands.forEach(op->id.addInteger(op.hashCode()));
            id.addInteger(indices.size());
            for (int i = 0, e = indices.size(); i < e; i++)
                id.addInteger(indices.get(i));
            return id.computeHash();
        }
    }

    static class APIntKeyInfo
    {
        APInt val;
        Type type;

        APIntKeyInfo(APInt v, Type ty)
        {
            val = v;
            type = ty;
        }

        @Override
        public int hashCode()
        {
            FoldingSetNodeID id = new FoldingSetNodeID();
            id.addInteger(val.getZExtValue());
            id.addInteger(type.hashCode());
            return id.computeHash();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            if (this == obj)
                return true;
            if (getClass() != obj.getClass())
                return false;

            APIntKeyInfo key = (APIntKeyInfo)obj;
            return val.eq(key.val) && type.equals(key.type);
        }
    }

    static class APFloatKeyInfo
    {
        private APFloat flt;
        public APFloatKeyInfo(APFloat flt)
        {
            this.flt = flt;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            if (this == obj)
                return true;
            if (getClass() != obj.getClass())
                return false;
            APFloatKeyInfo key = (APFloatKeyInfo)obj;
            return flt.bitwiseIsEqual(key.flt);
        }

        @Override
        public int hashCode()
        {
            FoldingSetNodeID id = new FoldingSetNodeID();
            id.addString(flt.toString());
            return id.computeHash();
        }
    }
}
