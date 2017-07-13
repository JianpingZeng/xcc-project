package backend.transform.scalars;

import backend.analysis.DomTreeInfo;
import backend.analysis.DomTreeNodeBase;
import backend.value.BasicBlock;
import backend.value.Operator;
import backend.utils.PredIterator;
import backend.pass.AnalysisUsage;
import backend.pass.FunctionPass;
import backend.support.DepthFirstOrder;
import backend.transform.scalars.GVNPRE.Expression.ExpressionOpCode;
import backend.value.Function;
import backend.value.Instruction;
import backend.value.Instruction.*;
import backend.value.User;
import backend.value.Value;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.BitMap;
import tools.Pair;
import tools.Util;

import java.util.*;

import static backend.support.DepthFirstOrder.dfTravesal;

/**
 * This class implements a pass on FunctionProto to perform global value numbering, and
 * Partial redundancy elimination based GVN.
 * <p>
 * The implementation original stem from the literature of "Value-based partial
 * redundancy elimination, Thomas", in the same time, partial thoughts is extracted
 * from another paper "An Implementation of GVN-PRE in LLVM, Prashanth Radhakrishnan"
 * </p>
 * 
 * @author xlous.zeng
 * @version 0.1
 */
public final class GVNPRE implements FunctionPass
{
    static class Expression
    {
        enum ExpressionOpCode
        {
            ADD, FADD, SUB, FSUB, MUL, FMUL,
            UDIV, SDIV, FDIV, UREM, SREM, FREM,
            SHL, LSHR, ASHR, AND, OR, XOR, ICMPEQ,
            ICMPNE, ICMPUGT, ICMPULT, ICMPULE, ICMPSLT,
            ICMPSLE, ICMPSGT, ICMPSGE, FCMPOEQ,
            FCMPOGT, FCMPOGE, FCMPOLT, FCMPOLE, FCMPONE,
            FCMPORD, FCMPUNO, FCMPUEQ, FCMPUGT, FCMPUGE,
            FCMPULT, FCMPULE, FCMPUNE, TRUNC, ZEXT, SEXT,
            FPTOUI, FPTOSI, UITOFP, SITOFP, FPTRUNC, FPEXT,
            PTRTOINT, INTTOPTR, BITCAST, GEP
        }

        ExpressionOpCode opcode;
        backend.type.Type type;
        int firstVN;
        int secondVN;
        int thirdVN;
        TIntArrayList varargs;

        Expression()
        {
            this(null);
        }

        Expression(ExpressionOpCode opcode)
        {
            this.opcode = opcode;
            varargs = new TIntArrayList(4);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if (!getClass().equals(obj.getClass()))
                return false;

            Expression rhs = (Expression)obj;
            if (!opcode.equals(rhs.opcode))
                return false;
            if (firstVN != rhs.firstVN
                    || secondVN != rhs.secondVN
                    || thirdVN != rhs.thirdVN)
                return false;
            if (varargs.size() != rhs.varargs.size())
                return false;
            for (int i = 0, e = varargs.size(); i < e; i++)
                if (varargs.get(i) != rhs.varargs.get(i))
                    return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = opcode.ordinal();
            result = 37 * result + type.hashCode();
            result = 37 * result + firstVN;
            result = 37 * result + secondVN;
            result = 37 * result + thirdVN;
            for (int i = 0, e= varargs.size(); i < e; i++)
                result = 37 * result + varargs.get(i);

            return result;
        }
    }

    static class ValueTable
    {
        private TObjectIntHashMap<Value> valueNumbering;
        private TObjectIntHashMap<Expression> expressionNumbering;

        private int nextValueNumber;

        private ExpressionOpCode getOpcode(Op2 inst)
        {
            switch (inst.getOpcode())
            {
                case Add:
                    return ExpressionOpCode.ADD;
                case FAdd:
                    return ExpressionOpCode.FADD;
                case Sub:
                    return ExpressionOpCode.SUB;
                case FSub:
                    return ExpressionOpCode.FSUB;
                case Mul:
                    return ExpressionOpCode.MUL;
                case FMul:
                    return ExpressionOpCode.FMUL;
                case SDiv:
                    return ExpressionOpCode.SDIV;
                case UDiv:
                    return ExpressionOpCode.UDIV;
                case FDiv:
                    return ExpressionOpCode.FDIV;
                case URem:
                    return ExpressionOpCode.UREM;
                case SRem:
                    return ExpressionOpCode.SREM;
                case FRem:
                    return ExpressionOpCode.FREM;
                case Shl:
                    return ExpressionOpCode.SHL;
                case LShr:
                    return ExpressionOpCode.LSHR;
                case AShr:
                    return ExpressionOpCode.ASHR;
                case And:
                    return ExpressionOpCode.AND;
                case Or:
                    return ExpressionOpCode.OR;
                case Xor:
                    return ExpressionOpCode.XOR;
                default:
                    Util.shouldNotReachHere("Binary operator with unknown opcode?");;
                    return ExpressionOpCode.ADD;
            }
        }

        private ExpressionOpCode getOpcode(CmpInst inst)
        {
            if (inst.getOpcode() == Operator.ICmp)
            {
                switch (inst.getPredicate())
                {
                    case ICMP_EQ:
                        return ExpressionOpCode.ICMPEQ;
                    case ICMP_NE:
                        return ExpressionOpCode.ICMPNE;
                    case ICMP_UGT:
                        return ExpressionOpCode.ICMPUGT;
                    case ICMP_UGE:
                        return ExpressionOpCode.ICMPUGT;
                    case ICMP_ULT:
                        return ExpressionOpCode.ICMPULT;
                    case ICMP_ULE:
                        return ExpressionOpCode.ICMPULE;
                    case ICMP_SGT:
                        return ExpressionOpCode.ICMPSGT;
                    case ICMP_SGE:
                        return ExpressionOpCode.ICMPSGE;
                    case ICMP_SLT:
                        return ExpressionOpCode.ICMPSLT;
                    case ICMP_SLE:
                        return ExpressionOpCode.ICMPSLE;
                    default:
                        Util.shouldNotReachHere("Comparision with unknown predicate?");
                        return ExpressionOpCode.ICMPEQ;
                }
            }
            else
            {
                switch (inst.getPredicate())
                {
                    case FCMP_OEQ:
                        return ExpressionOpCode.FCMPOEQ;
                    case FCMP_OGT:
                        return ExpressionOpCode.FCMPOGT;
                    case FCMP_OGE:
                        return ExpressionOpCode.FCMPOGE;
                    case FCMP_OLT:
                        return ExpressionOpCode.FCMPOLT;
                    case FCMP_OLE:
                        return ExpressionOpCode.FCMPOLE;
                    case FCMP_ONE:
                        return ExpressionOpCode.FCMPONE;
                    case FCMP_ORD:
                        return ExpressionOpCode.FCMPORD;
                    case FCMP_UNO:
                        return ExpressionOpCode.FCMPUNO;
                    case FCMP_UEQ:
                        return ExpressionOpCode.FCMPUEQ;
                    case FCMP_UGT:
                        return ExpressionOpCode.FCMPUGT;
                    case FCMP_UGE:
                        return ExpressionOpCode.FCMPUGE;
                    case FCMP_ULT:
                        return ExpressionOpCode.FCMPULT;
                    case FCMP_ULE:
                        return ExpressionOpCode.FCMPULE;
                    case FCMP_UNE:
                        return ExpressionOpCode.FCMPUNE;

                    // THIS SHOULD NEVER HAPPEN
                    default:
                        Util.shouldNotReachHere("Comparison with unknown predicate?");
                        return ExpressionOpCode.FCMPOEQ;
                }
            }
        }

        private ExpressionOpCode getOpcode(CastInst inst)
        {
            switch (inst.getOpcode())
            {
                case Trunc:
                    return ExpressionOpCode.TRUNC;
                case ZExt:
                    return ExpressionOpCode.ZEXT;
                case SExt:
                    return ExpressionOpCode.SEXT;
                case FPToUI:
                    return ExpressionOpCode.FPTOUI;
                case FPToSI:
                    return ExpressionOpCode.FPTOSI;
                case UIToFP:
                    return ExpressionOpCode.UITOFP;
                case SIToFP:
                    return ExpressionOpCode.SITOFP;
                case FPTrunc:
                    return ExpressionOpCode.FPTRUNC;
                case FPExt:
                    return ExpressionOpCode.FPEXT;
                case PtrToInt:
                    return ExpressionOpCode.PTRTOINT;
                case IntToPtr:
                    return ExpressionOpCode.INTTOPTR;
                case BitCast:
                    return ExpressionOpCode.BITCAST;

                // THIS SHOULD NEVER HAPPEN
                default:
                    Util.shouldNotReachHere("Cast operator with unknown opcode?");
                    return ExpressionOpCode.BITCAST;
            }
        }

        private Expression createExpression(Op2 inst)
        {
            Expression e = new Expression();
            e.opcode = getOpcode(inst);
            e.type = inst.getType();
            e.firstVN = lookupOrAdd(inst.operand(0));
            e.secondVN = lookupOrAdd(inst.operand(1));
            e.thirdVN = 0;
            return e;
        }

        private Expression createExpression(CmpInst inst)
        {
            Expression e = new Expression();
            e.opcode = getOpcode(inst);
            e.type = inst.getType();
            e.firstVN = lookupOrAdd(inst.operand(0));
            e.secondVN = lookupOrAdd(inst.operand(1));
            e.thirdVN = 0;
            return e;
        }

        private Expression createExpression(CastInst inst)
        {
            Expression e = new Expression();
            e.opcode = getOpcode(inst);
            e.type = inst.getType();
            e.firstVN = lookupOrAdd(inst.operand(0));
            e.secondVN = 0;
            e.thirdVN = 0;
            return e;
        }

        private Expression createExpression(GetElementPtrInst inst)
        {
            Expression e = new Expression();
            e.opcode = ExpressionOpCode.GEP;
            e.type = inst.getType();
            e.firstVN = lookupOrAdd(inst.getPointerOperand());
            e.secondVN = 0;
            e.thirdVN = 0;

            for (int i = inst.getIndexBegin(); i < inst.getIndexEnd(); i++)
                e.varargs.add(lookupOrAdd(inst.operand(i)));

            return e;
        }

        public ValueTable()
        {
            valueNumbering = new TObjectIntHashMap<>();
            expressionNumbering = new TObjectIntHashMap<>();
            nextValueNumber = 1;
        }

        public int lookupOrAdd(Value val)
        {
            if (valueNumbering.containsKey(val))
                return valueNumbering.get(val);

            Expression e = null;
            if (val instanceof Op2)
            {
                e = createExpression((Op2) val);
            }
            else if (val instanceof CmpInst)
            {
                e = createExpression((CmpInst) val);
            }
            else if (val instanceof CastInst)
            {
                e = createExpression((CastInst) val);
            }
            else
            {
                valueNumbering.put(val, nextValueNumber);
                return nextValueNumber++;
            }

            if (expressionNumbering.containsKey(e))
            {
                int num = expressionNumbering.get(e);
                valueNumbering.put(val, num);
                return num;
            }
            else
            {
                expressionNumbering.put(e, nextValueNumber);
                valueNumbering.put(val, nextValueNumber);
                return nextValueNumber++;
            }
        }

        public int lookup(Value val)
        {
            if (valueNumbering.containsKey(val))
                return valueNumbering.get(val);
            else
                Util.shouldNotReachHere("Value could not add as yet?");

            return 0;
        }

        public void add(Value val, int num)
        {
            if (valueNumbering.containsKey(val))
                valueNumbering.remove(val);
            valueNumbering.put(val, num);
        }

        public void clear()
        {
            valueNumbering.clear();
            expressionNumbering.clear();
            nextValueNumber = 1;
        }

        public void remove(Value val)
        {
            valueNumbering.remove(val);
        }

        public int size()
        {
            return nextValueNumber;
        }
    }

    static class ValueNumberedSet
    {
        private HashSet<Value> contents;
        private BitMap numbers;

        public ValueNumberedSet()
        {
            contents = new HashSet<>();
            numbers = new BitMap(1);
        }

        public HashSet<Value> getContents()
        {
            return contents;
        }

        public boolean insert(Value val)
        {
            return contents.add(val);
        }

        public void remove(Value val)
        {
            contents.remove(val);
        }

        public boolean contains(Value val)
        {
            return contents.contains(val);
        }

        public int size()
        {
            return contents.size();
        }

        public void set(int i)
        {
            numbers.set(i);
        }

        public void reset(int i)
        {
            numbers.clear(i);
        }

        public boolean test(int i)
        {
            return numbers.get(i);
        }

        public void clear()
        {
            contents.clear();
            numbers.clear();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;

            ValueNumberedSet other = (ValueNumberedSet)obj;
            return contents.equals(other.contents)
                    && numbers.equals(other.numbers);
        }

        @Override
        public int hashCode()
        {
            return (contents.hashCode() ^ contents.hashCode() >>> 16)
                    * 37 + numbers.hashCode() ^ numbers.hashCode() >>> 16;
        }
    }

    private ValueTable valueTable;
    private ArrayList<Instruction> createdExprs;
    private HashMap<BasicBlock, ValueNumberedSet> availableOut;
    private HashMap<BasicBlock, ValueNumberedSet> anticipatibleIn;
    private HashMap<BasicBlock, ValueNumberedSet> generatedPhis;

    private GVNPRE()
    {
        valueTable = new ValueTable();
        createdExprs = new ArrayList<>();
        availableOut = new HashMap<>();
        anticipatibleIn = new HashMap<>();
        generatedPhis = new HashMap<>();
    }

	/**
     * A public interface to this class for creating an instance of
     * GVNPRE pass.
     * @return
     */
    public static GVNPRE createGVNPREPass()
    {
        return new GVNPRE();
    }

    @Override
    public void getAnalysisUsage(AnalysisUsage au)
    {
        au.addRequired(BreakCriticalEdge.class);
        au.addRequired(UnifyFunctionExitNodes.class);
        au.addRequired(DomTreeInfo.class);
    }

    @Override
    public String getPassName()
    {
        return "Global value number and PRE pass";
    }

	/**
	 * Builds the AvailOut for each basic block and additional auxiliary
     * data structure.
     * @param inst
     * @param curAvailOut
     * @param curPhis
     * @param curExprs
     * @param curTemps
     */
    private void buildAvailOut(Instruction inst,
            ValueNumberedSet curAvailOut,
            ValueNumberedSet curPhis,
            ValueNumberedSet curExprs,
            ArrayList<Value> curTemps)
    {
        if (inst instanceof PhiNode)
        {
            PhiNode pn = (PhiNode)inst;
            int num = valueTable.lookupOrAdd(inst);
            curPhis.insert(pn);
            curPhis.set(num);
        }
        else if (inst instanceof CastInst)
        {
            CastInst ci = (CastInst)inst;
            int num = valueTable.lookupOrAdd(ci);
            if (ci.operand(0) instanceof Instruction)
            {
                Instruction i = (Instruction)ci.operand(0);
                int ii = valueTable.lookup(i);
                if (!curExprs.test(ii))
                {
                    curExprs.insert(i);
                    curExprs.set(ii);
                }
            }
            if (!curExprs.test(num))
            {
                curExprs.insert(ci);
                curExprs.set(num);
            }
        }
        else if (inst instanceof CmpInst || inst instanceof Op2)
        {
            int num = valueTable.lookupOrAdd(inst);

            if (inst.operand(0) instanceof Instruction)
            {
                int op0Num = valueTable.lookup(inst.operand(0));
                if (!curExprs.test(op0Num))
                {
                    curExprs.insert(inst.operand(0));
                    curExprs.set(op0Num);
                }
            }
            if (inst.operand(1) instanceof Instruction)
            {
                int op1Num = valueTable.lookup(inst.operand(1));
                if (!curExprs.test(op1Num))
                {
                    curExprs.insert(inst.operand(1));
                    curExprs.set(op1Num);
                }
            }
            if (!curExprs.test(num))
            {
                curExprs.insert(inst);
                curExprs.set(num);
            }
        }
        else if (inst instanceof GetElementPtrInst)
        {
            GetElementPtrInst gep = (GetElementPtrInst)inst;
            int num = valueTable.lookupOrAdd(gep);

            Value basePtr = gep.getPointerOperand();

            if (basePtr instanceof Instruction && !curExprs.test(valueTable.lookup(basePtr)))
            {
                curExprs.insert(basePtr);
                curExprs.set(valueTable.lookup(basePtr));
            }

            for (int idx = gep.getIndexBegin(); idx < gep.getIndexEnd(); idx++)
            {
                Value op = gep.operand(idx);
                if (op instanceof Instruction && !curExprs.test(valueTable.lookup(op)))
                {
                    curExprs.insert(op);
                    curExprs.set(valueTable.lookup(op));
                }
            }

            if (!curExprs.test(num))
            {
                curExprs.insert(gep);
                curExprs.set(num);
            }
        }
        else if (!inst.isTerminator())
        {
            valueTable.lookupOrAdd(inst);
            curTemps.add(inst);
        }

        if (!inst.isTerminator())
        {
            if (!curAvailOut.test(valueTable.lookup(inst)))
                curAvailOut.set(valueTable.lookup(inst));
        }
    }

    private Value findLeader(ValueNumberedSet vals, int v)
    {
        if (!vals.test(v))
            return null;
        for (Value val : vals.getContents())
            if (valueTable.lookup(val) == v)
                return val;

        Util.shouldNotReachHere("No leader found, but present bit is set?");
        return null;
    }

    private Value phiTranslate(Value val, BasicBlock pred, BasicBlock succ)
    {
        if (val == null)
            return val;

        if (val instanceof CastInst)
        {
            CastInst ci = (CastInst)val;
            Value newOp1;
            if (ci.operand(0) instanceof Instruction)
                newOp1 = phiTranslate(ci.operand(0), pred, succ);
            else
                newOp1 = ci.operand(0);

            if (newOp1 == null)
                return null;

            if (newOp1 != ci.operand(0))
            {
                Instruction newVal = CastInst.create(ci.getOpcode(),
                        newOp1, ci.getType(), ci.getName() + ".expr", null);
                int v = valueTable.lookupOrAdd(newVal);
                Value leader = findLeader(availableOut.get(pred), v);
                if (leader == null)
                {
                    createdExprs.add(newVal);
                    return newVal;
                }
                else
                {
                    valueTable.remove(newVal);
                    return leader;
                }
            }
        }

        if (val instanceof Op2 || val instanceof CmpInst)
        {
            User u = (User)val;
            Value newOp1;
            if (u.operand(0) instanceof Instruction)
                newOp1 = phiTranslate(u.operand(0), pred, succ);
            else
                newOp1 = u.operand(0);
            if (newOp1 == null)
                return null;

            Value newOp2;
            if (u.operand(1) instanceof Instruction)
                newOp2 = phiTranslate(u.operand(1), pred, succ);
            else
                newOp2 = u.operand(1);

            if (newOp2 == null)
                return null;

            if (newOp1 != u.operand(0) || newOp2 != u.operand(1))
            {
                Instruction newVal = null;
                if (val instanceof Op2)
                {
                    Op2 op = (Op2)val;
                    newVal = Op2.create(op.getOpcode(), newOp1, newOp2,
                            op.getName() +".expr");
                }
                else
                {
                    CmpInst ci = (CmpInst)val;
                    newOp2 = CmpInst.create(ci.getOpcode(), ci.getPredicate(),
                            newOp1, newOp2, ci.getName()+".expr", null);
                }
                int v = valueTable.lookupOrAdd(newVal);

                Value leader = findLeader(availableOut.get(pred), v);
                if (leader == null)
                {
                    createdExprs.add(newVal);
                    return newVal;
                }
                else
                {
                    valueTable.remove(newVal);
                    return leader;
                }
            }
        }
        if (val instanceof GetElementPtrInst)
        {
            GetElementPtrInst gep = (GetElementPtrInst)val;
            Value basePtr = gep.getPointerOperand();
            Value newBasePtr = null;
            if (basePtr instanceof Instruction)
            {
                newBasePtr = phiTranslate(basePtr, pred, succ);
            }
            else
                newBasePtr = basePtr;

            if (newBasePtr == null)
                return null;

            boolean changedIdx = false;
            ArrayList<Value> idx = new ArrayList<>();
            for (int i = gep.getIndexBegin(); i < gep.getIndexEnd(); i++)
            {
                Value operand = gep.operand(i);
                if (operand instanceof Instruction)
                {
                    Value newOp = phiTranslate(operand, pred, succ);
                    idx.add(newOp);
                    if (newOp != operand)
                        changedIdx = true;
                }
                else
                    idx.add(operand);
            }

            if (newBasePtr != basePtr || changedIdx)
            {
                Instruction newVal = new GetElementPtrInst(newBasePtr, idx,
                        val.getName()+".expr", null);
                int v = valueTable.lookupOrAdd(newVal);
                Value leader = findLeader(availableOut.get(pred), v);
                if (leader == null)
                {
                    createdExprs.add(newVal);
                    return newVal;
                }
                else
                {
                    valueTable.remove(newVal);
                    return leader;
                }
            }
        }
        else if (val instanceof PhiNode)
        {
            PhiNode pn = (PhiNode)val;
            if (pn.getParent() == succ)
                return pn.getIncomingValueForBlock(pred);
        }

        return val;
    }

	/**
	 * Perform phi translation on every element of a set.
     * @param anticInSucc
     * @param cur
     * @param succ
     * @param anticOut
     */
    private void phiTranslateSet(ValueNumberedSet anticInSucc, BasicBlock cur,
            BasicBlock succ, ValueNumberedSet anticOut)
    {
        for (Value val : anticInSucc.getContents())
        {
            Value v = phiTranslate(val, cur, succ);
            if (v != null && !anticOut.test(valueTable.lookup(v)))
            {
                anticOut.insert(v);
                anticOut.set(valueTable.lookup(v));
            }
        }
    }

    private boolean buildSetAntiOut(BasicBlock bb,
            ValueNumberedSet anticOut,
            HashSet<BasicBlock> visited)
    {
        if (bb.getTerminator().getNumOfSuccessors() == 1)
        {
            BasicBlock succ = bb.getTerminator().suxAt(0);
            if (succ != bb && !visited.contains(succ))
            {
                return true;
            }
            else
            {
                phiTranslateSet(anticipatibleIn.get(succ), bb, succ, anticOut);
            }
        }
        else if (bb.getTerminator().getNumOfSuccessors() > 1)
        {
            BasicBlock first = bb.getTerminator().suxAt(0);
            for (Value val : anticipatibleIn.get(first).getContents())
            {
                anticOut.insert(val);
                anticOut.set(valueTable.lookup(val));
            }

            for (int i = 1; i < bb.getTerminator().getNumOfSuccessors(); i++)
            {
                ValueNumberedSet succAnticIn = anticipatibleIn.
                        get(bb.getTerminator().suxAt(i));

                ArrayList<Value> temps = new ArrayList<>();
                for (Value val : anticOut.getContents())
                {
                    if (!succAnticIn.test(valueTable.lookup(val)))
                        temps.add(val);
                }

                temps.forEach(val->
                {
                    anticOut.remove(val);
                    anticOut.reset(valueTable.lookup(val));
                });
            }
        }
        return false;
    }

    private void toposort(ValueNumberedSet vals, LinkedList<Value> worklist)
    {
        HashSet<Value> visited = new HashSet<>();
        Stack<Value> stack = new Stack<>();

        for (Value val : vals.getContents())
        {
            if (visited.add(val))
                stack.add(val);

            while (!stack.isEmpty())
            {
                Value e = stack.peek();

                if (e instanceof CastInst)
                {
                    User u = (User)e;
                    Value l = findLeader(vals, valueTable.lookup(u.operand(0)));
                    if (l != null && (l instanceof Instruction) && !visited.contains(l))
                        stack.add(l);
                    else
                    {
                        worklist.add(e);
                        visited.add(e);
                        stack.pop();
                    }
                }
                else if (e instanceof Op2 || e instanceof CmpInst)
                {
                    User u = (User)e;
                    Value l = findLeader(vals, valueTable.lookup(u.operand(0)));
                    Value r = findLeader(vals, valueTable.lookup(u.operand(1)));

                    if (l != null && (l instanceof Instruction) && !visited.contains(l))
                    {
                        stack.add(l);
                    }
                    else if (r != null && (r instanceof Instruction) && !visited.contains(r))
                    {
                        stack.add(r);
                    }
                    else
                    {
                        worklist.add(e);
                        visited.add(e);
                        stack.pop();
                    }
                }
                else if (e instanceof GetElementPtrInst)
                {
                    GetElementPtrInst gep = (GetElementPtrInst)e;
                    Value p = findLeader(vals, valueTable.lookup(((GetElementPtrInst) e).getPointerOperand()));
                    if (p != null && p instanceof Instruction && !visited.contains(p))
                        stack.add(p);
                    else
                    {
                        boolean pushVa = false;
                        for (int i = gep.getIndexBegin(); i < gep.getIndexEnd(); i++)
                        {
                            Value op = gep.operand(i);
                            Value leader = findLeader(vals, valueTable.lookup(op));
                            if (leader != null && leader instanceof Instruction
                                    && !visited.contains(leader))
                            {
                                stack.add(leader);
                                pushVa = true;
                            }
                        }

                        if (!pushVa)
                        {
                            worklist.add(e);
                            visited.add(e);
                            stack.pop();
                        }
                    }
                }
                else
                {
                    visited.add(e);
                    worklist.add(e);
                    stack.pop();
                }
            }
            stack.clear();
        }
    }

    private void clean(ValueNumberedSet anticIn)
    {
        LinkedList<Value> worklist = new LinkedList<>();
        toposort(anticIn, worklist);

        for (Iterator<Value> itr = worklist.iterator(); itr.hasNext();)
        {
            Value v = itr.next();

            if (v instanceof CastInst)
            {
                CastInst ci = (CastInst)v;
                boolean lhsValid = !(ci.operand(0) instanceof Instruction);
                lhsValid |= anticIn.test(valueTable.lookup(ci.operand(0)));

                if (!lhsValid)
                {
                    anticIn.remove(ci);
                    anticIn.reset(valueTable.lookup(v));
                }
            }
            else if (v instanceof Op2 || v instanceof CmpInst)
            {
                User u = (User)v;
                boolean lhsValid = !(u.operand(0) instanceof Instruction);
                lhsValid |= anticIn.test(valueTable.lookup(u.operand(0)));

                boolean rhsValid = !(u.operand(1) instanceof Instruction);
                rhsValid |= anticIn.test(valueTable.lookup(u.operand(1)));

                if (!lhsValid || !rhsValid)
                {
                    anticIn.remove(v);
                    anticIn.reset(valueTable.lookup(v));
                }
            }else if (v instanceof GetElementPtrInst)
            {
                GetElementPtrInst gep = (GetElementPtrInst)v;
                Value basePtr = gep.getPointerOperand();
                boolean baseValid = !(basePtr instanceof Instruction);
                baseValid |= anticIn.test(valueTable.lookup(basePtr));

                boolean varValid = true;
                for (int i = gep.getIndexBegin(); i < gep.getIndexEnd(); i++)
                {
                    Value op = gep.operand(i);
                    if (varValid)
                    {
                        varValid = !(op instanceof Instruction) || anticIn.test(valueTable.lookup(op));
                    }
                }

                if (!baseValid || !varValid)
                {
                    anticIn.remove(v);
                    anticIn.reset(valueTable.lookup(v));
                }
            }
        }
    }

    private int buildSetAntiIn(BasicBlock block,
            ValueNumberedSet antiOut,
            ValueNumberedSet curExprs,
            ArrayList<Value> curTemps,
            HashSet<BasicBlock> visited)
    {
        ValueNumberedSet antiIn = anticipatibleIn.get(block);
        int old = antiIn.size();

        boolean defer = buildSetAntiOut(block, antiOut, visited);
        if (defer)
            return 0;

        antiIn.clear();
        for (Value val : antiOut.getContents())
        {
            antiIn.insert(val);
            antiIn.set(valueTable.lookup(val));
        }

        for (Value val : curExprs.getContents())
        {
            if (!antiIn.test(valueTable.lookup(val)))
            {
                antiIn.insert(val);
                antiIn.set(valueTable.lookup(val));
            }
        }

        for (Value val : curTemps)
        {
            antiIn.remove(val);
            antiIn.reset(valueTable.lookup(val));
        }

        clean(antiIn);
        antiOut.clear();

        return old != antiIn.size() ? 2 : 1;
    }

	/**
	 * Computes the AvailableOut and AnticipateIn for each basic block
     * in program.
     */
    private void buildSets(Function f)
    {
        HashMap<BasicBlock, ValueNumberedSet> generatedExprs = new HashMap<>();
        HashMap<BasicBlock, ArrayList<Value>> generatedTemps = new HashMap<>();

        DomTreeInfo dt = getAnalysisToUpDate(DomTreeInfo.class);

        DomTreeNodeBase<BasicBlock> root = dt.getNode(f.getEntryBlock());
        LinkedList<DomTreeNodeBase<BasicBlock>> worklist =
                new LinkedList<>();
        worklist.addLast(root);

        // Phase #1: compute the AvailOut[BB].
        while (!worklist.isEmpty())
        {
            DomTreeNodeBase<BasicBlock> curNode = worklist.removeLast();
            BasicBlock bb = curNode.getBlock();

            ValueNumberedSet curAvailOut = new ValueNumberedSet();
            availableOut.put(bb, curAvailOut);

            ValueNumberedSet curPhis = new ValueNumberedSet();
            generatedPhis.put(bb, curPhis);

            ValueNumberedSet curExprs = new ValueNumberedSet();
            generatedExprs.put(bb, curExprs);

            ArrayList<Value> curTemps = new ArrayList<>();
            generatedExprs.put(bb, curExprs);

            if (curNode.getIDom() != null)
                curAvailOut = availableOut.get(curNode.getIDom().getBlock());
            for (Instruction inst : bb)
                buildAvailOut(inst, curAvailOut, curPhis, curExprs, curTemps);
        }

        // Phase #2.1: Computes the AnticipateIn[BB]
        boolean changed = true;
        HashSet<BasicBlock> visited = new HashSet<>();
        HashSet<BasicBlock> blockChanged = new HashSet<>();
        f.getBasicBlockList().forEach(bb->blockChanged.add(bb));

        while (changed)
        {
            changed = false;
            ValueNumberedSet antiOut = new ValueNumberedSet();

            LinkedList<BasicBlock> po = DepthFirstOrder.postOrder(f.getEntryBlock());
            for (BasicBlock bb : po)
            {
                int result = buildSetAntiIn(bb, antiOut, generatedExprs.get(bb),
                        generatedTemps.get(bb), visited);

                if (result == 0)
                {
                    changed = true;
                }
                else
                {
                    visited.add(bb);
                    if (result == 2)
                    {
                        for (PredIterator<BasicBlock> itr = bb.predIterator(); itr.hasNext();)
                            blockChanged.add(itr.next());
                    }
                    else
                    {
                        blockChanged.remove(bb);
                    }
                    changed |= result == 2;
                }
            }
        }
        // Phase#2.2: Computes the AnticipateOut[BB]

    }

	/**
     * Insert a value into a set, replacing any values already in
     * the set that have the same value number
     * @param vals
     * @param v
     */
    private void valReplace(ValueNumberedSet vals, Value v)
    {
        if (vals.contains(v))
            return;

        int num = valueTable.lookup(v);
        Value leader = findLeader(vals, num);
        if (leader != null)
            vals.remove(leader);

        vals.insert(v);
        vals.set(num);
    }

    private void insertPRE(Value e, BasicBlock bb,
            HashMap<BasicBlock, Value> avail,
            HashMap<BasicBlock, ValueNumberedSet> newSets)
    {
        for (PredIterator<BasicBlock> itr = bb.predIterator(); itr.hasNext();)
        {
            BasicBlock pred = itr.next();
            Value e2 = avail.get(pred);
            if (!availableOut.get(pred).test(valueTable.lookup(e2)))
            {
                User u = (User)e2;

                Value s1 = null;
                if (u.operand(0) instanceof Op2
                        || u.operand(0) instanceof CmpInst
                        || u.operand(0) instanceof CastInst
                        || u.operand(0) instanceof GetElementPtrInst)
                {
                    s1 = findLeader(availableOut.get(pred), valueTable.lookup(u.operand(0)));
                }
                else
                {
                    s1 = u.operand(0);
                }

                Value s2 = null;
                if (u instanceof Op2 || u instanceof CmpInst)
                {
                    if (u.operand(1) instanceof Op2
                            || u.operand(1) instanceof CmpInst
                            || u.operand(1) instanceof CastInst
                            || u.operand(1) instanceof GetElementPtrInst)
                    {
                        s2 = findLeader(availableOut.get(pred), valueTable.lookup(u.operand(1)));
                    }
                    else
                    {
                        s2 = u.operand(1);
                    }
                }

                ArrayList<Value> varargs = new ArrayList<>();
                if (u instanceof GetElementPtrInst)
                {
                    GetElementPtrInst gep = (GetElementPtrInst)u;

                    for (int i = gep.getIndexBegin(); i < gep.getIndexEnd(); i++)
                    {
                        Value op = gep.operand(i);
                        if (op instanceof Op2
                                || op instanceof CmpInst
                                || op instanceof CastInst
                                || op instanceof GetElementPtrInst)
                        {
                            varargs.add(findLeader(availableOut.get(pred), valueTable.lookup(op)));
                        }
                        else
                        {
                            varargs.add(op);
                        }
                    }
                }

                Value newVal = null;
                if (u instanceof Op2)
                {
                    newVal = Op2.create(u.opcode, s1, s2,
                            u.getName()+".gnvpre",
                            pred.getTerminator());
                }
                else if (u instanceof CmpInst)
                {
                    CmpInst ci = (CmpInst)u;
                    newVal = CmpInst.create(u.opcode, ci.getPredicate(),
                            s1, s2, u.getName() + ".gvnpre",
                            pred.getTerminator());
                }
                else if (u instanceof CastInst)
                {
                    newVal = CastInst.create(u.opcode, s1, u.getType(),
                            u.getName()+".gvnpre", pred.getTerminator());
                }
                else if (u instanceof GetElementPtrInst)
                {
                    GetElementPtrInst gep = (GetElementPtrInst)u;
                    newVal = new GetElementPtrInst(s1, varargs,
                            gep.getName()+".gvnpre", pred.getTerminator());
                }

                valueTable.add(newVal, valueTable.lookup(u));

                ValueNumberedSet predAvail = availableOut.get(pred);
                valReplace(predAvail, newVal);
                valReplace(newSets.get(pred), newVal);
                predAvail.set(valueTable.lookup(newVal));

                if (avail.containsKey(pred))
                    avail.remove(pred);
                avail.put(pred, newVal);
            }
        }

        PhiNode pn = null;
        for (PredIterator itr = bb.predIterator(); itr.hasNext();)
        {
            BasicBlock pred = itr.next();
            if (pn == null)
                pn = new PhiNode(avail.get(pred).getType(),
                        "gvnpre-join", bb.getFirstInst());
            pn.addIncoming(avail.get(pred), pred);
        }

        valueTable.add(pn, valueTable.lookup(pn));
        valReplace(availableOut.get(bb), pn);
        generatedPhis.get(bb).insert(pn);
        generatedPhis.get(bb).set(valueTable.lookup(e));
        newSets.get(bb).insert(pn);
        newSets.get(bb).set(valueTable.lookup(e));
    }

    private int insertMergePoint(LinkedList<Value> worklist,
            DomTreeNodeBase<BasicBlock> node,
            HashMap<BasicBlock, ValueNumberedSet> newSets)
    {
        boolean changed = false;
        boolean newStuff = false;

        BasicBlock bb = node.getBlock();

        for (Value val : worklist)
        {
            if (val instanceof Op2 || val instanceof CmpInst
                    || val instanceof CastInst
                    || val instanceof GetElementPtrInst)
            {
                if(availableOut.get(node.getIDom().getBlock()).test(valueTable.lookup(val)))
                    continue;

                HashMap<BasicBlock, Value> avail = new HashMap<>();
                boolean bySome = false;
                boolean allSame = true;
                Value firstS = null;

                for (PredIterator<BasicBlock> itr = bb.predIterator(); itr.hasNext();)
                {
                    BasicBlock pred = itr.next();
                    Value e = phiTranslate(val, pred, bb);
                    Value e2 = findLeader(availableOut.get(pred), valueTable.lookup(e));

                    if (e2 == null)
                    {
                        if (avail.containsKey(pred))
                            avail.remove(pred);
                        avail.put(pred, e);
                        allSame = false;
                    }
                    else
                    {
                        if (avail.containsKey(pred))
                            avail.remove(pred);

                        avail.put(pred, e2);
                        bySome = true;

                        if (firstS == null)
                            firstS = e2;
                        else if (firstS != e2)
                            allSame = false;
                    }
                }

                if (bySome && !allSame && !generatedPhis.get(bb).test(valueTable.lookup(val)))
                {
                    insertPRE(val, bb, avail, newSets);

                    changed = true;
                    newStuff = true;
                }
            }
        }

        int retval = 0;
        if (changed)
            retval += 1;
        if (newStuff)
            retval += 1;
        return retval;
    }

    private boolean insert(Function f)
    {
        boolean changed = false;

        DomTreeInfo dt = getAnalysisToUpDate(DomTreeInfo.class);

        HashMap<BasicBlock, ValueNumberedSet> newSets = new HashMap<>();
        boolean newStuff = true;
        while (newStuff)
        {
            newStuff = false;
            for (DomTreeNodeBase<BasicBlock> node : dfTravesal(dt.getRootNode()))
            {
                BasicBlock bb = node.getBlock();

                if (bb == null)
                    continue;

                ValueNumberedSet availOut = availableOut.get(bb);
                ValueNumberedSet anticIn = anticipatibleIn.get(bb);

                if (node.getIDom() != null)
                {
                    ValueNumberedSet domSet = newSets.get(node.getIDom().getBlock());
                    if (domSet == null)
                        domSet = new ValueNumberedSet();
                    newSets.put(node.getIDom().getBlock(), domSet);

                    for (Value val : domSet.getContents())
                    {
                        valReplace(newSets.get(bb), val);
                        valReplace(availOut, val);
                    }
                }

                if (bb.getNumPredecessors() > 1)
                {
                    LinkedList<Value> worklist = new LinkedList<>();
                    toposort(anticIn, worklist);

                    int ret = insertMergePoint(worklist, node, newSets);
                    if (ret == 1 || ret == 2)
                    {
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private boolean eliminate()
    {
        boolean changed = false;
        LinkedList<Pair<Instruction, Value>> replace = new LinkedList<>();
        ArrayList<Instruction> erase = new ArrayList<>();

        DomTreeInfo dt = getAnalysisToUpDate(DomTreeInfo.class);

        for (DomTreeNodeBase<BasicBlock> node : dfTravesal(dt.getRootNode()))
        {
            BasicBlock bb = node.getBlock();

            for (Iterator<Instruction> itr = bb.iterator(); itr.hasNext(); )
            {
                Instruction inst = itr.next();
                if (inst instanceof Op2 || inst instanceof CmpInst
                        || inst instanceof CastInst
                        || inst instanceof GetElementPtrInst)
                {
                    if (availableOut.get(bb).test(valueTable.lookup(inst))
                            && !availableOut.get(bb).contains(inst))
                    {
                        Value leader = findLeader(availableOut.get(bb), valueTable.lookup(inst));
                        if (leader instanceof Instruction)
                        {
                            Instruction ii = (Instruction)leader;
                            if (ii.getParent() != null && !ii.equals(inst))
                            {
                                replace.add(Pair.get(inst, leader));
                                erase.add(inst);
                            }
                        }
                    }
                }
            }
        }

        while (!replace.isEmpty())
        {
            Pair<Instruction, Value> rep = replace.removeLast();
            rep.first.replaceAllUsesWith(rep.second);
            changed = true;
        }

        for (Instruction del : erase)
        {
            del.eraseFromBasicBlock();
            changed = true;
        }

        return changed;
    }

    private void cleanup()
    {
        createdExprs.clear();
    }

    @Override
    public boolean runOnFunction(Function f)
    {
        valueTable.clear();
        createdExprs.clear();
        availableOut.clear();
        anticipatibleIn.clear();
        generatedPhis.clear();

        boolean changedFunction = false;

        // Step #1: Builds the flow set.
        buildSets(f);

        // Step #2: Inserts the computed expression in the
        // appropriate point for make partially redundant to
        // fully available.
        changedFunction |= insert(f);

        // Step #3: Eliminate the fully redundancy.
        changedFunction |= eliminate();

        cleanup();
        return changedFunction;
    }
}
