package backend.value;

import backend.support.LLVMContext;
import backend.type.ArrayType;
import backend.type.IntegerType;
import backend.type.StructType;
import backend.type.Type;
import backend.value.Instruction.CmpInst.Predicate;
import gnu.trove.list.array.TIntArrayList;
import jlang.type.FoldingSetNodeID;
import tools.APFloat;
import tools.APInt;
import tools.FltSemantics;

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
    private final static HashMap<ExprMapKeyType, ConstantExpr> ExprConstantMaps
            = new HashMap<>();

    private final static HashMap<APIntKeyType, ConstantInt> IntConstants
            = new HashMap<>();

    private final static HashMap<APFloatKeyType, ConstantFP> FPConstants
            = new HashMap<>();

    private final static HashMap<Type, ConstantPointerNull> NullPtrConstants
            = new HashMap<>();

    private final static HashMap<ConstantStructKey, ConstantStruct> StructConstants
            = new HashMap<>();

    private final static HashMap<MDNodeKeyType, MDNode> MDNodeConstants
            = new HashMap<>();

    private final static HashMap<String, MDString> MDStringConstants
            = new HashMap<>();

    /**
     * A cache mapping pair of ArrayType and Constant value list to ConstantArray.
     */
    private static HashMap<ConstantArrayKey, ConstantArray> ArrayConstants
            = new HashMap<>();

    /**
     * Force the constructor of this class be private.
     */
    private UniqueConstantValueImpl()
    {}

    private static final UniqueConstantValueImpl uniqueImpl
            = new UniqueConstantValueImpl();

    public static UniqueConstantValueImpl getUniqueImpl()
    {
        return uniqueImpl;
    }

    /**
     * Get the unique constant corresponding to specified key. This method
     * will create a new one and return it when it is not exist in unique map.
     * @param key
     * @return
     */
    public ConstantExpr getOrCreate(ExprMapKeyType key)
    {
        if (ExprConstantMaps.containsKey(key))
            return ExprConstantMaps.get(key);

        ConstantExpr ce;
        Operator opc = key.opcode;
        if (opc.isComparison())
        {
            assert key.operands.size() == 2;
            Constant lhs = key.operands.get(0), rhs = key.operands.get(1);
            ce = new CmpConstantExpr(lhs.getType(), opc, lhs, rhs, key.predicate);
            ExprConstantMaps.put(key, ce);
            return ce;
        }
        else if (opc.isBinaryOps())
        {
            assert key.operands.size() == 2;
            Constant lhs = key.operands.get(0), rhs = key.operands.get(1);
            ce = new BinaryConstantExpr(opc, lhs, rhs);
            ExprConstantMaps.put(key, ce);
            return ce;
        }
        else if (opc.isUnaryOps())
        {
            assert key.operands.size() == 1;
            Constant op = key.operands.get(0);
            ce = new UnaryConstExpr(opc, op, op.getType());
            ExprConstantMaps.put(key, ce);
            return ce;
        }
        else
        {
            assert opc.isGEP():"Unknown ExprMapKeyType";
            assert key.operands.size() > 1;
            Constant base = key.operands.get(0);
            ArrayList<Constant> idx = new ArrayList<>();
            idx.addAll(key.operands.subList(1, key.operands.size()));
            ce = new GetElementPtrConstantExpr(base, idx, base.getType());
            ExprConstantMaps.put(key, ce);
            return ce;
        }
    }
    /**
     * Get the unique constant corresponding to specified key. This method
     * will create a new one and return it when it is not exist in unique map.
     * @param key
     * @return
     */
    public ConstantInt getOrCreate(APIntKeyType key)
    {
        if (IntConstants.containsKey(key))
            return IntConstants.get(key);

        ConstantInt ci = new ConstantInt(key.type, key.val);
        IntConstants.put(key, ci);
        return ci;
    }

    /**
     * Get the unique constant corresponding to specified key. This method
     * will create a new one and return it when it is not exist in unique map.
     * @param key
     * @return
     */
    public ConstantFP getOrCreate(APFloatKeyType key)
    {
        if (FPConstants.containsKey(key))
            return FPConstants.get(key);

        Type ty = floatSemanticsToType(key.flt.getSemantics());
        ConstantFP flt = new ConstantFP(ty, key.flt);
        FPConstants.put(key, flt);
        return flt;
    }

    public ConstantPointerNull getOrCreate(Type ty)
    {
        if (NullPtrConstants.containsKey(ty))
            return NullPtrConstants.get(ty);

        ConstantPointerNull cpn = new ConstantPointerNull(ty);
        NullPtrConstants.put(ty, cpn);
        return cpn;
    }

    public void removeKey(Type ty)
    {
        NullPtrConstants.remove(ty);
    }

    public ConstantArray getOrCreate(ConstantArrayKey key)
    {
        if (ArrayConstants.containsKey(key))
            return ArrayConstants.get(key);

        ConstantArray ca = new ConstantArray(key.type, key.eltVals);
        ArrayConstants.put(key, ca);
        return ca;
    }

    public ConstantStruct getOrCreate(ConstantStructKey key)
    {
        if (StructConstants.containsKey(key))
            return StructConstants.get(key);

        ConstantStruct cs = new ConstantStruct(key.st, key.elts);
        StructConstants.put(key, cs);
        return cs;
    }

    public MDNode getOrCreate(MDNodeKeyType key)
    {
        if (MDNodeConstants.containsKey(key))
            return MDNodeConstants.get(key);

        MDNode node = new MDNode(key.elts);
        MDNodeConstants.put(key, node);
        return node;
    }

    public MDString getOrCreate(String key)
    {
        assert key != null && !key.isEmpty();
        if (MDStringConstants.containsKey(key))
            return MDStringConstants.get(key);

        MDString md = new MDString(key);
        MDStringConstants.put(key, md);
        return md;
    }

    private static Type floatSemanticsToType(FltSemantics semantics)
    {
        if (semantics == APFloat.IEEEsingle)
            return LLVMContext.FloatTy;
        if (semantics == APFloat.IEEEdouble)
            return LLVMContext.DoubleTy;
        if (semantics == APFloat.x87DoubleExtended)
            return LLVMContext.X86_FP80Ty;
        if (semantics == APFloat.IEEEquad)
            return LLVMContext.FP128Ty;

        assert false:"Unknown FP format";
        return null;
    }

    public static class ExprMapKeyType
    {
        Operator opcode;
        Predicate predicate;
        ArrayList<Constant> operands;
        TIntArrayList indices;

        public ExprMapKeyType(Operator opc, Constant op)
        {
            this(opc, op, FCMP_FALSE);
        }

        public ExprMapKeyType(Operator opc, Constant op, Predicate pred)
        {
            this(opc, op, pred, new TIntArrayList());
        }

        public ExprMapKeyType(Operator opc, Constant op, Predicate pred, TIntArrayList indices)
        {
            opcode = opc;
            predicate = pred;
            operands = new ArrayList<>();
            operands.add(op);
            this.indices = new TIntArrayList();
            this.indices.addAll(indices);
        }

        public ExprMapKeyType(Operator opc, List<Constant> ops, Predicate pred)
        {
            this(opc, ops, pred, new TIntArrayList());
        }

        public ExprMapKeyType(Operator opc, List<Constant> ops)
        {
            this(opc, ops, Predicate.FCMP_FALSE);
        }

        public ExprMapKeyType(Operator opc, List<Constant> ops,
                Predicate pred, TIntArrayList indices)
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

    public static class APIntKeyType
    {
        APInt val;
        IntegerType type;

        APIntKeyType(APInt v, IntegerType ty)
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

            APIntKeyType key = (APIntKeyType)obj;
            return val.eq(key.val) && type.equals(key.type);
        }
    }

    public static class APFloatKeyType
    {
        private APFloat flt;
        public APFloatKeyType(APFloat flt)
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
            APFloatKeyType key = (APFloatKeyType)obj;
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

    public static class ConstantArrayKey
    {
        ArrayType type;
        ArrayList<Constant> eltVals;

        ConstantArrayKey(ArrayType type, ArrayList<Constant> eltVals)
        {
            this.type = type;
            this.eltVals = eltVals;
        }
    }

    public static class ConstantStructKey
    {
        private StructType st;
        private ArrayList<Constant> elts;

        public ConstantStructKey(StructType ty, List<Constant> indices)
        {
            st = ty;
            elts = new ArrayList<>();
            elts.addAll(indices);
        }
    }

    public static class MDNodeKeyType
    {
        private ArrayList<Value> elts;
        public MDNodeKeyType(List<Value> eles)
        {
            elts = new ArrayList<>();
            elts.addAll(eles);
        }

        @Override
        public int hashCode()
        {
            FoldingSetNodeID id = new FoldingSetNodeID();
            id.addInteger(elts.size());
            elts.forEach(elt->id.addInteger(elt.hashCode()));
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
            MDNodeKeyType key = (MDNodeKeyType)obj;
            if (key.elts.size() != elts.size()) return false;
            for (int i = 0, e = elts.size(); i < e; i++)
            {
                if (!key.elts.get(i).equals(elts.get(i)))
                    return false;
            }
            return true;
        }
    }
}
