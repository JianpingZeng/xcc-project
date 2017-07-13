package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import tools.Pair;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class TernOpInit extends Init.OpInit
{
    public enum TernaryOp {SUBST, FOREACH, IF}

    private TernaryOp opc;
    private Init lhs, mhs, rhs;

    public TernOpInit(TernaryOp opc, Init lhs, Init mhs, Init rhs, RecTy type)
    {
        super(type);
        this.opc = opc;
        this.lhs = lhs;
        this.mhs = mhs;
        this.rhs = rhs;
    }

    public TernaryOp getOpcode()
    {
        return opc;
    }

    public Init getLhs()
    {
        return lhs;
    }

    public Init getMhs()
    {
        return mhs;
    }

    public Init getRhs()
    {
        return rhs;
    }

    @Override
    public void print(PrintStream os)
    {
        os.println(toString());
    }

    @Override
    public int getNumOperands()
    {
        return 3;
    }

    @Override
    public Init getOperand(int i)
    {
        assert i >= 0 && i < 3;
        return i == 0 ? lhs : i == 1 ? mhs : rhs;
    }

    @Override
    public Init fold(Record curRec, MultiClass curMultiClass)
            throws Exception
    {
        switch (getOpcode())
        {
            default: assert false: "Unknown binop";
            case SUBST:
            {
                DefInit lhsd;
                VarInit lhsv;
                StringInit lhss;
                
                boolean lhsIsDef = lhs instanceof DefInit;
                boolean lhsIsVar = rhs instanceof VarInit;
                boolean lhsIsString = lhs instanceof StringInit;

                DefInit mhsd;
                VarInit mhsv;
                StringInit mhss;
                boolean mhsIsDef = mhs instanceof DefInit;
                boolean mhsIsVar = mhs instanceof VarInit;
                boolean mhsIsString = mhs instanceof StringInit;

                DefInit rhsd;
                VarInit rhsv;
                StringInit rhss;
                boolean rhsIsDef = rhs instanceof DefInit;
                boolean rhsIsVar = rhs instanceof VarInit;
                boolean rhsIsString = rhs instanceof StringInit;

                if ((lhsIsDef && mhsIsDef && rhsIsDef)
                        || (lhsIsVar && mhsIsVar && rhsIsVar)
                        || (lhsIsString && mhsIsString && rhsIsString)) 
                {
                    if (rhsIsDef) 
                    {
                        lhsd = (DefInit)lhs;
                        mhsd = (DefInit)mhs;
                        rhsd = (DefInit)rhs;
                        Record Val = rhsd.getDef();
                        if (lhsd.toString().equals(rhsd.toString()))
                        {
                            Val = mhsd.getDef();
                        }
                        return new DefInit(Val);
                    }
                    if (rhsIsVar) 
                    {
                        lhsv = (VarInit)lhs;
                        mhsv = (VarInit)mhs;
                        rhsv = (VarInit)rhs; 
                        String Val = rhsv.getName();
                        if (Objects.equals(lhsv.toString(), rhsv.toString()))
                        {
                            Val = mhsv.getName();
                        }
                        return new VarInit(Val, getType());
                    }
                    if (rhsIsString)
                    {
                        lhss = (StringInit)lhs;
                        mhss = (StringInit)mhs;
                        rhss = (StringInit)rhs;
                        String val = rhss.getValue();
                        do
                        {
                            if (val.contains(lhss.getValue()))
                            {
                                val.replace(lhss.getValue(), mhss.getValue());
                            }
                        } while (val.contains(lhss.getValue()));

                        return new StringInit(val);
                    }
                }
                break;
            }

            case FOREACH:
            {
                Init Result = foreachHelper(lhs, mhs, rhs, getType(),
                        curRec, curMultiClass);
                if (Result != null)
                {
                    return Result;
                }
                break;
            }

            case IF:
            {
                IntInit lhsi;
                if (lhs instanceof IntInit)
                {
                    lhsi = (IntInit)lhs;
                    if (lhsi.getValue() != 0)
                    {
                        return mhs;
                    }
                    else {
                        return rhs;
                    }
                }
                break;
            }
        }

        return this;
    }

    private static Init foreachHelper(Init lhs, Init mhs,
            Init rhs, RecTy type,
            Record curRec, MultiClass curMultiClass)
    {
        boolean mhsIsDag = (mhs instanceof DagInit);
        boolean mhsIsList = mhs instanceof ListInit;

        boolean typeIsDag = type instanceof RecTy.DagRecTy;
        boolean typeIsList = (type instanceof RecTy.ListRecTy);

        boolean rhsIsOp = rhs instanceof OpInit;

        if (!rhsIsOp)
        {
            System.err.println("!foreach requires an operator");
            assert false:"No operator for !foreach";
        }

        boolean lhsIsType = lhs instanceof TypedInit;
        if (!lhsIsType)
        {
            System.err.println("!foreach requires typed variable");
            assert false:"No typed variable for !foreach";
        }

        if ((mhsIsDag && typeIsDag) || (mhsIsList && typeIsList))
        {
            if (mhsIsDag)
            {
                DagInit mhsd = (DagInit)mhs;
                OpInit rhso = (OpInit)rhs;

                Init val = mhsd.getOperator();
                Init result = evaluateOperation(rhso, lhs, val, type, curRec, curMultiClass);

                if (result != null)
                {
                    val = result;
                }

                ArrayList<Pair<Init, String>> args = new ArrayList<>();
                for (int i = 0, e = mhsd.getNumArgs(); i < e; i++)
                {
                    Init arg = mhsd.getArg(i);
                    String argName = mhsd.getArgName(i);

                    Init res = evaluateOperation(rhso, lhs, arg, type, curRec, curMultiClass);;

                    if (res != null)
                        arg = res;

                    args.add(Pair.get(arg, argName));
                }
                return new DagInit(val, "", args);
            }
            if (mhsIsList)
            {
                ListInit mhsl = (ListInit)mhs;
                ArrayList<Init> newOperands = new ArrayList<>();
                ArrayList<Init> newList = new ArrayList<>();
                for (int i = 0, e = mhsl.getSize(); i < e; i++)
                    newList.add(mhsl.getElement(i));

                for (int j = 0, e = newList.size(); j < e; j++)
                {
                    Init item = newList.get(j);
                    newOperands.clear();
                    OpInit rhso = (OpInit)rhs;
                    for (int i = 0, sz = rhso.getNumOperands(); i < sz; ++i)
                    {
                        if (lhs.toString().equals(rhso.getOperand(i).toString()))
                            newOperands.add(item);
                        else
                            newOperands.add(rhso.getOperand(i));
                    }

                    OpInit newOp = rhso.clone(newOperands);
                    Init newItem;
                    try
                    {
                        newItem = newOp.fold(curRec, curMultiClass);
                        if (!newItem.equals(newOp))
                        {
                            newList.set(j, newItem);
                        }
                    }
                    catch (Exception e1)
                    {
                        e1.printStackTrace();
                    }
                }

                return new ListInit(newList, mhsl.getType());
            }
        }

        return null;
    }

    private static Init evaluateOperation(OpInit rhso, Init lhs, Init arg,
            RecTy type, Record curRec, MultiClass curMultiClass)
    {
        ArrayList<Init> newOperands = new ArrayList<>();

        TypedInit targ = arg instanceof TypedInit ? (TypedInit)arg : null;
        if (targ != null && targ.getType().toString().equals("dag"))
        {
            return foreachHelper(lhs, arg, rhso, type, curRec, curMultiClass);
        }

        for (int i = 0, e = rhso.getNumOperands(); i < e; i++)
        {
            OpInit rhsoo =  rhso.getOperand(i) instanceof OpInit ?
                    (OpInit)rhso.getOperand(i): null;
            if (rhsoo != null)
            {
                Init result = evaluateOperation(rhsoo, lhs, arg, type, curRec, curMultiClass);
                if (result != null)
                    newOperands.add(result);
                else
                    newOperands.add(arg);
            }
            else if (lhs.toString().equals(rhso.getOperand(i).toString()))
                newOperands.add(arg);
            else
            {
                newOperands.add(rhso.getOperand(i));
            }
        }

        OpInit newOp = rhso.clone(newOperands);
        Init newVal = null;
        try
        {
            newVal = newOp.fold(curRec, curMultiClass);
            if (!newVal.equals(newOp))
                return newVal;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public OpInit clone(List<Init> operands)
    {
        assert operands.size() == 3;
        return new TernOpInit(getOpcode(), operands.get(0),
                operands.get(1), operands.get(2), getType());
    }

    @Override
    public Init resolveReferences(Record r, RecordVal rval)
    {
        try
        {
            Init Lhs = lhs.resolveReferences(r, rval);

            if (opc == TernaryOp.IF && !lhs.equals(Lhs))
            {
                if (Lhs instanceof IntInit)
                {
                    IntInit value = (IntInit)Lhs;
                    // Short-circuit
                    if (value.getValue() != 0)
                    {
                        Init Mhs = mhs.resolveReferences(r, rval);
                        return (new TernOpInit(getOpcode(), Lhs, Mhs, rhs, getType())).fold(r, null);
                    }
                    else
                    {
                        Init Rhs = rhs.resolveReferences(r, rval);
                        return (new TernOpInit(getOpcode(), Lhs, mhs, Rhs, getType())).fold(r, null);
                    }
                }
            }

            Init Mhs = mhs.resolveReferences(r, rval);
            Init Rhs = rhs.resolveReferences(r, rval);

            if (Lhs != lhs || Mhs != mhs || Rhs != rhs)
                return (new TernOpInit(getOpcode(), Lhs, Mhs, Rhs, getType())).fold(r, null);
            return fold(r, null);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        switch (opc)
        {
            case SUBST: sb.append("!subst"); break;
            case FOREACH: sb.append("!foreach"); break;
            case IF: sb.append("!if"); break;
        }
        return sb.append("(")
                .append(lhs.toString())
                .append(", ")
                .append(mhs.toString())
                .append(", ")
                .append(rhs.toString())
                .append(")")
                .toString();
    }

    @Override
    public TernOpInit clone()
    {
        return new TernOpInit(opc, lhs.clone(), mhs.clone(), rhs.clone(), getType());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null) return false;
        if(getClass() != obj.getClass())
            return false;

        TernOpInit to = (TernOpInit)obj;
        return opc == to.opc && lhs.equals(to.lhs)
                && mhs.equals(to.mhs)
                && rhs.equals(to.rhs);
    }
}
