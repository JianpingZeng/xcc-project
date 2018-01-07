package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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

import gnu.trove.list.array.TIntArrayList;
import tools.Pair;
import utils.tablegen.RecTy.BitsRecTy;
import utils.tablegen.RecTy.ListRecTy;
import utils.tablegen.RecTy.RecordRecTy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Initializer class definition.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class Init
{
    /**
     * This method should be overridden by concrete subclass
     * that is not be completely specified as yet.
     * @return
     */
    public boolean isComplete() { return true; }

    /**
     * Print out this value.
     * @param os
     */
    public abstract void print(PrintStream os);

    /**
     * Print out the result of {@linkplain #print(PrintStream)} to the String.
     * @return
     */
    @Override
    public String toString()
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        print(new PrintStream(os));
        return os.toString();
    }

    /**
     * Print out this value using stderr.
     */
    public void dump()
    {
        print(System.err);
    }

    /**
     * This method is a simple call-back method that should be overridden to
     * call the appropriate {@linkplain RecTy#convertValue} method.
     * @param ty
     * @return
     */
    public abstract Init convertInitializerTo(RecTy ty);

    /**
     * This method is used to implement the bitrange selection operator.
     * Given an initializer, it selects the specified bits out, returning them
     * as a new init of bits type.  If it is not legal to use the bit subscript
     * operator on this initializer, return null.
     * @param bits
     * @return
     */
    public Init convertInitializerBitRange(TIntArrayList bits)
    {
        return null;
    }

    /**
     * This method is used to implement the list slice
     * selection operator.  Given an initializer, it selects the specified list
     * elements, returning them as a new init of list type.  If it is not legal
     * to take a slice of this, return null.
     * @param elements
     * @return
     */
    public Init convertIntListSlice(TIntArrayList elements)
    {
        return null;
    }

    /**
     * Implementors of this method should return the type of the named field if
     * they are of record type.
     * @param fieldName
     * @return
     */
    public RecTy getFieldType(String fieldName)
    {
        return null;
    }

    /**
     * This method complements getFieldType to return the
     * initializer for the specified field.  If getFieldType returns non-null
     * this method should return non-null, otherwise it returns null.
     * @param r
     * @param fieldName
     * @return
     */
    public Init getFieldInit(Record r, String fieldName)
    {
        return null;
    }

    /**
     * This method is used by classes that refer to other
     * variables which may not be defined at the time they expression is formed.
     * If a value is set for the variable later, this method will be called on
     * users of the value to allow the value to propagate out.
     * @param r
     * @param rval
     * @return
     */
    public Init resolveReferences(Record r, RecordVal rval)
    {
        return this;
    }

    @Override
    public abstract Init clone();

    @Override
    public abstract boolean equals(Object obj);

    /**
     * Represents an uninitialized value-'?'.
     */
    public static class UnsetInit extends Init
    {
        private static final UnsetInit instance = new UnsetInit();
        public static UnsetInit getInstance()
        {
            return instance;
        }

        private UnsetInit()
        {
            super();
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        @Override
        public UnsetInit clone()
        {
            return instance;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;

            UnsetInit other = (UnsetInit)obj;
            return other == instance;
        }

        @Override
        public boolean isComplete()
        {
            return false;
        }

        @Override
        public void print(PrintStream os)
        {
            os.print("?");
        }
    }

    /**
     * Represent a concrete initializer for a bit which either is true or false.
     */
    public static class BitInit extends Init
    {
        private boolean value;
        public BitInit(boolean val) {value = val;}

        public boolean getValue() {return value; }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        @Override
        public BitInit clone()
        {
            return new BitInit(value);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;
            return value == ((BitInit)obj).value;
        }

        @Override
        public void print(PrintStream os)
        {
            os.print(value? "1":"0");
        }
    }

    /**
     * Represents an initializer for a BitsRecTy value.
     * It contains a vector of bits, whose getNumOfSubLoop is determined by the type.
     */
    public static class BitsInit extends Init
    {
        private ArrayList<Init> bits;
        private ArrayList<Boolean> setted;
        public BitsInit(int size)
        {
            bits = new ArrayList<>();
            setted = new ArrayList<>();
            // Fixme: initialize each element with UnsetInit rather than 'null',
            // to avoid NullPointerException.
            for (int i = 0; i < size; i++)
            {
                bits.add(UnsetInit.getInstance());
                setted.add(false);
            }
        }

        public int getNumBits()
        {
            return bits.size();
        }

        public Init getBit(int bit)
        {
            assert bit < bits.size() :"Bit index out of range";
            return bits.get(bit);
        }

        public void setBit(int bit, Init val)
        {
            assert bit < bits.size() :"Bit index out of range";
            assert !setted.get(bit) :"Bit already set!";
            bits.set(bit, val);
            setted.set(bit, true);
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        @Override
        public Init convertInitializerBitRange(TIntArrayList bits)
        {
            BitsInit bi = new BitsInit(bits.size());
            for (int i = 0, e = bits.size(); i != e; i++)
            {
                if (bits.get(i) >= getNumBits())
                    return null;
                bi.setBit(i, getBit(bits.get(i)));
            }
            return bi;
        }

        @Override
        public boolean isComplete()
        {
            for (int i = 0, e = getNumBits(); i < e; i++)
            {
                if (!getBit(i).isComplete()) return false;
            }
            return true;
        }

        @Override
        public void print(PrintStream os)
        {
            os.print("{");
            for (int i = 0, e = getNumBits(); i < e; i++)
            {
                if (i != 0) os.print(", ");
                Init bit = getBit(e-i-1);
                if (bit != null)
                    bit.print(os);
                else
                    os.print("*");
            }
            os.print("}");
        }

        @Override
        public Init resolveReferences(Record r, RecordVal rval)
        {
            boolean changed = false;
            BitsInit newInit = new BitsInit(getNumBits());
            for (int i = 0, e = bits.size(); i < e; i++)
            {
                Init b, curBit = getBit(i);

                do
                {
                    b = curBit;
                    //System.out.println(r.toString());
                    //System.out.println(rval.toString());

                    curBit = curBit.resolveReferences(r, rval);
                    changed |= !b.equals(curBit);
                }while (!b.equals(curBit));

                newInit.setBit(i, curBit);
            }

            if (changed)
                return newInit;
            return this;
        }

        @Override
        public BitsInit clone()
        {
            BitsInit res = new BitsInit(getNumBits());
            for (int i = 0, e = bits.size(); i < e; i++)
                res.bits.set(i, bits.get(i).clone());

            return res;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;
            BitsInit bi = (BitsInit)obj;
            return bits.equals(bi.bits);
        }

        // Print this bitstream with the specified format, returning true if
        // it is not possible.
        public boolean printInHex(PrintStream os)
        {
            int result = 0;
            for (int i = 0, e = getNumBits(); i < e; i++)
            {
                Init bit = getBit(i);
                if (bit instanceof BitInit)
                {
                    result |= (((BitInit)bit).getValue() ? 1:0) << i;
                }
                else
                    return true;
            }

            os.printf("0x%s", Long.toHexString(result).toUpperCase());
            return false;
        }

        public boolean printAsVariable(PrintStream os)
        {
            assert getNumBits() != 0;
            Init bit = getBit(0);
            if (!(bit instanceof VarBitInit)) return true;

            VarBitInit firstBit = (VarBitInit)bit;
            TypedInit var = firstBit.getVariable();

            if (!(firstBit.getVariable().getType() instanceof BitsRecTy))
                return true;
            BitsRecTy ty = (BitsRecTy)firstBit.getVariable().getType();
            if (ty.getNumBits() != getNumBits()) return true;

            for (int i = 0, e = getNumBits(); i < e; i++)
            {
                Init b = getBit(i);
                VarBitInit vbi;
                if (!(b instanceof VarBitInit) || (vbi = (VarBitInit)b).getVariable() != var
                        || vbi.getBitNum() != i)
                {
                    return true;
                }
            }

            var.print(os);
            return false;
        }

        public boolean printAsUnset(PrintStream os)
        {
            for (int i = 0, e = getNumBits(); i < e; i++)
                if (!(getBit(i) instanceof UnsetInit))
                    return true;
            os.print("?");
            return false;
        }
    }

    /**
     * Represent an initalization by a literal integer value.
     */
    public static class IntInit extends TypedInit
    {
        private long value;

        public IntInit(long val)
        {
            super(new RecTy.IntRecTy());
            value = val;
        }

        public long getValue()
        {
            return value;
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        @Override
        public Init convertInitializerBitRange(TIntArrayList bits)
        {
            BitsInit bi = new BitsInit(bits.size());

            for (int i = 0, e = bits.size(); i < e; i++)
            {
                if (bits.get(i) >= 32)
                    return null;

                boolean res = (value & (1 << bits.get(i))) != 0;
                bi.setBit(i, new BitInit(res));
            }
            return bi;
        }

        @Override
        public IntInit clone()
        {
            return new IntInit(value);
        }

        @Override public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;

            return ((IntInit)obj).value == value;
        }

        @Override
        public Init resolveBitReference(Record r, RecordVal rval, int bit)
        {
            assert false:"Illegal bit reference off int";
            return null;
        }

        @Override
        public Init resolveListElementReference(Record r, RecordVal rval,
                int elt)
        {
            assert false:"Illegal element reference off int";
            return null;
        }

        @Override
        public void print(PrintStream os)
        {
            os.print(value);
        }
    }

    /**
     * Represent an initalization by a literal string value.
     */
    public static class StringInit extends TypedInit
    {
        private String value;
        public StringInit(String val)
        {
            super(new RecTy.StringRecTy());
            value = val;
        }

        public String getValue()
        {
            return value;
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        @Override
        public StringInit clone()
        {
            return new StringInit(getValue());
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;

            return value.equals(((StringInit)obj).value);
        }

        @Override
        public void print(PrintStream os)
        {
            os.printf("\"%s\"", value);
        }

        @Override
        public Init resolveBitReference(Record r, RecordVal rval, int bit)
        {
            assert false:"Illegal bit reference off string";
            return null;
        }

        @Override
        public Init resolveListElementReference(Record r, RecordVal rval,
                int elt)
        {
            assert false:"Illegal element reference off string";
            return null;
        }
    }

    /**
     * [AL, AH, CL] - Represent a list of defs.
     */
    public static class ListInit extends TypedInit
    {
        private ArrayList<Init> values;

        public ListInit(ArrayList<Init> vals, RecTy eltTy)
        {
            super(new ListRecTy(eltTy));
            values = new ArrayList<>(vals);
        }

        public int getSize()
        {
            return values.size();
        }

        public Init getElement(int idx)
        {
            assert idx >= 0 && idx < getSize() :"List element index out of range";
            return values.get(idx);
        }

        @Override
        public Init convertIntListSlice(TIntArrayList elements)
        {
            ArrayList<Init> vals = new ArrayList<>();
            for (int i = 0, e = elements.size(); i< e; i++)
            {
                if (elements.get(i) >= getSize())
                    return null;
                vals.add(getElement(elements.get(i)));
            }
            return new ListInit(vals, getType());
        }

        @Override
        public Init resolveBitReference(Record r, RecordVal rval, int bit)
        {
            assert false:"Illegal bit reference off list!";
            return null;
        }

        @Override
        public Init resolveListElementReference(Record r, RecordVal rval,
                int elt)
        {
            if (elt >= getSize())
                return null;

            Init e = getElement(elt);
            if (!(e instanceof UnsetInit))
                return e;

            return null;
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        @Override
        public Init resolveReferences(Record r, RecordVal rval)
        {
            ArrayList<Init> resolved = new ArrayList<>(getSize());
            boolean changed = false;

            for (int i = 0, e = getSize(); i < e; i++)
            {
                Init init, curElt = getElement(i);

                do
                {
                    init = curElt;
                    curElt = curElt.resolveReferences(r, rval);
                    changed |= !init.equals(curElt);
                }while (!init.equals(curElt));

                // Add resovled Init into resolved list and constructed as a member
                // of new ListInit object to be returned.
                // FIXME Previously, there is no resolved.add(init)  that
                // FIXME causes pattern list of PMOVSXWQrr record is empty. 2017.7.21 (done!!!)
                resolved.add(init);
            }
            if (changed)
                return new ListInit(resolved, getType());
            return this;
        }

        @Override
        public ListInit clone()
        {
            ArrayList<Init> list = new ArrayList<>();
            values.forEach(e->list.add(e.clone()));

            return new ListInit(list, ((ListRecTy)getType()).getElementType());
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;

            ListInit li = (ListInit)obj;

            return values.equals(li.values);
        }

        @Override
        public void print(PrintStream os)
        {
            os.print("[");
            for (int i = 0, e = values.size(); i < e; i++)
            {
                if (i != 0) os.print(", ");
                os.print(values.get(i));
            }
            os.print("]");
        }

        public Record getElementAsRecord(int index) throws Exception
        {
            assert index >= 0
                    && index < getSize() : "List element index out of range!";
            if (values.get(index) instanceof DefInit)
            {
                return ((DefInit)values.get(index)).getDef();
            }
            throw new Exception("Expected record in list!");
        }
    }

    /**
     * op (X, Y) - Combine two inits.
     */
    public static class BinOpInit extends OpInit
    {
        public enum BinaryOp{ SHL, SRA, SRL, CONCAT, STRCONCAT, NAMECONCAT};

        private BinaryOp opc;
        private Init lhs, rhs;

        public BinOpInit(BinaryOp opc, Init lhs, Init rhs, RecTy type)
        {
            super(type);
            this.opc = opc;
            this.lhs = lhs;
            this.rhs = rhs;
        }

        public BinaryOp getOpcode()
        {
            return opc;
        }

        public Init getLhs()
        {
            return lhs;
        }

        public Init getRhs()
        {
            return rhs;
        }

        @Override
        public int getNumOperands()
        {
            return 2;
        }

        @Override
        public Init getOperand(int i)
        {
            assert i == 0 || i == 1:"Invalid operand index";
            return i == 0 ? lhs : rhs;
        }

        @Override
        public Init fold(Record curRec, MultiClass curMultiClass)
                throws Exception
        {
            switch (getOpcode())
            {
                default:
                    assert false:"Unknown binop";
                case CONCAT:
                {
                    DagInit lhss, rhss;
                    if (lhs instanceof DagInit && rhs instanceof DagInit)
                    {
                        lhss = (DagInit)lhs;
                        rhss = (DagInit)rhs;

                        DefInit lop = (DefInit) lhss.getOperator();
                        DefInit rop = (DefInit) rhss.getOperator();

                        if (!lop.getDef().equals(rop.getDef()))
                        {
                            boolean lisOPs = lop.getDef().getName().equals("outs")
                                    || !lop.getDef().getName().equals("ins")
                                    || !lop.getDef().getName().equals("defs");
                            boolean rIsOPs = rop.getDef().getName().equals("outs")
                                    || !rop.getDef().getName().equals("ins")
                                    || !rop.getDef().getName().equals("defs");

                            if (!lisOPs || !rIsOPs)
                                throw new Exception("Concated Dag operators don't match");
                        }

                        ArrayList<Init> args = new ArrayList<>();
                        ArrayList<String> argNames = new ArrayList<>();

                        for (int i = 0, e = lhss.getNumArgs(); i < e; ++i)
                        {
                            args.add(lhss.getArg(i));
                            argNames.add(lhss.getArgName(i));
                        }

                        for (int i = 0, e = rhss.getNumArgs(); i < e; ++i)
                        {
                            args.add(rhss.getArg(i));
                            argNames.add(rhss.getArgName(i));
                        }

                        return new DagInit(lhss.getOperator(), "", args, argNames);
                    }
                    break;
                }
                case STRCONCAT:
                {
                    if (lhs instanceof StringInit && rhs instanceof StringInit)
                    {
                        StringInit lhss = (StringInit)lhs;
                        StringInit rhss = (StringInit)rhs;
                        return new StringInit(lhss.getValue() + rhss.getValue());
                    }
                    break;
                }
                case SHL:
                case SRA:
                case SRL:
                {
                    if (lhs instanceof IntInit && rhs instanceof  IntInit)
                    {
                        IntInit lhsi = (IntInit)lhs;
                        IntInit rhsi = (IntInit)rhs;
                        long lhsv = lhsi.getValue(), rhsv = rhsi.getValue();
                        long result;
                        switch (getOpcode())
                        {
                            default:assert false:"Bad opcode!";
                            case SHL: result = lhsv << rhsv; break;
                            case SRA: result = lhsv >> rhsv; break;
                            case SRL: result = lhsv >>> rhsv; break;
                        }
                        return new IntInit(result);
                    }
                    break;
                }
                case NAMECONCAT:
                {
                    StringInit lhss, rhss;
                    if (lhs instanceof StringInit && rhs instanceof StringInit)
                    {
                        lhss = (StringInit)lhs;
                        rhss = (StringInit)rhs;

                        String name = lhss.getValue() + rhss.getValue();

                        if (curRec != null)
                        {
                            RecordVal rv = curRec.getValue(name);
                            if(rv != null)
                            {
                                if (!rv.getType().equals(getType()))
                                    throw new Exception("type mismatch in nameconcat");
                            }
                            return new VarInit(name, rv.getType());
                        }

                        String templateArgName = curRec.getName() + ":" + name;
                        if(curRec.isTemplateArg(templateArgName))
                        {
                            RecordVal rv = curRec.getValue(templateArgName);
                            assert rv != null:"Template arg doesn't exist?";

                            if (!rv.getType().equals(getType()))
                                throw new Exception("type mismatch in nameconcat");

                            return new VarInit(templateArgName, rv.getType());
                        }


                        if (curMultiClass != null)
                        {
                            String mcname = curMultiClass.rec.getName() + "::" + name;

                            if(curMultiClass.rec.isTemplateArg(mcname))
                            {
                                RecordVal rv = curMultiClass.rec.getValue(mcname);
                                assert rv != null :"Template arg doesn't exist?";

                                if (!rv.getType().equals(getType()))
                                    throw new Exception("type mismatch in nameconcat");

                                return new VarInit(mcname, rv.getType());
                            }
                        }
                        Record r = Record.records.getDef(name);
                        if (r != null)
                            return new DefInit(r);

                        System.err.println("Variable not defined in !nameconcat: '" + name + "'");
                        assert false:"Variable not found in !nameconcat";
                        return null;
                    }
                }
            }
            return this;
        }

        @Override
        public OpInit clone(List<Init> operands)
        {
            assert operands.size() == 2:"Wrong number of operands for binary operator";
            return new BinOpInit(getOpcode(), operands.get(0), operands.get(1), getType());
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        @Override
        public Init resolveReferences(Record r, RecordVal rval)
        {
            Init lhsi = lhs.resolveReferences(r, rval);
            Init rhsi = rhs.resolveReferences(r, rval);
            try
            {
                if (!lhs.equals(lhsi) || !rhs.equals(rhsi))
                {
                    return new BinOpInit(getOpcode(), lhsi, rhsi, getType())
                            .fold(r, null);
                }
                return fold(r, null);
            }
            catch (Exception e)
            {
                return null;
            }
        }

        @Override
        public BinOpInit clone()
        {
            return new BinOpInit(opc, lhs.clone(), rhs.clone(), getType());
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;

            BinOpInit boi = (BinOpInit)obj;
            return opc == boi.opc && lhs.equals(boi.lhs) && rhs.equals(boi.rhs);
        }

        @Override
        public void print(PrintStream os)
        {
            os.println(toString());
        }

        @Override
        public String toString()
        {
            StringBuilder result = new StringBuilder();
            switch (opc)
            {
                case CONCAT: result.append("!con"); break;
                case SHL: result.append("!shl"); break;
                case SRA: result.append("!sra"); break;
                case SRL: result.append("!srl"); break;
                case STRCONCAT: result.append("!strconcat"); break;
                case NAMECONCAT:
                    result.append("!nameconcat<").append(getType().toString())
                            .append(">"); break;
            }

            return result.append("(").append(lhs.toString()).append(", ")
                    .append(rhs.toString()).append(")").toString();
        }
    }

    /**
     * "[{...}]" - Represent a code fragment.
     */
    public static class CodeInit extends Init
    {
        private String value;

        CodeInit(String val)
        {
            value = val;
        }

        public String getValue()
        {
            return value;
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        @Override
        public CodeInit clone()
        {
            return new CodeInit(value);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;
            return value.equals(((CodeInit)obj).value);
        }

        @Override
        public void print(PrintStream os)
        {
            os.printf("[{%s}]", value);
        }
    }

    /**
     * This is the common super-class of types that have a specific,
     * explicit, type.
     */
    public static abstract class TypedInit extends Init
    {
        private RecTy ty;

        public TypedInit(RecTy ty)
        {
            this.ty = ty;
        }

        public RecTy getType()
        {
            return ty;
        }

        @Override
        public Init convertInitializerBitRange(TIntArrayList bits)
        {
            if (!(getType() instanceof BitsRecTy))
                return null;

            BitsRecTy t = (BitsRecTy)getType();
            int numBits = t.getNumBits();

            BitsInit bi = new BitsInit(bits.size());
            for (int i = 0, e = bits.size(); i < e; i++)
            {
                if (bits.get(i) >= numBits)
                    return null;

                bi.setBit(i, new VarBitInit(this, bits.get(i)));
            }
            return bi;
        }

        @Override
        public Init convertIntListSlice(TIntArrayList elements)
        {
            if (!(getType() instanceof ListRecTy))
                return null;

            ListRecTy t = (ListRecTy)getType();
            if (elements.size() == 1)
                return new VarListElementInit(this, elements.get(0));

            ArrayList<Init> listInits = new ArrayList<>(elements.size());
            for (int i = 0, e = elements.size(); i < e; i++)
                listInits.add(new VarListElementInit(this, elements.get(i)));
            return new ListInit(listInits, getType());
        }

        public abstract Init resolveBitReference(Record r, RecordVal rval, int bit);

        public abstract Init resolveListElementReference(Record r, RecordVal rval,
                int elt);

        @Override
        public abstract TypedInit clone();
    }

    /**
     * Opcode{0} - Represent access to one bit of a variable or field.
     */
    public static class VarBitInit extends Init
    {
        private TypedInit ti;
        private int bit;

        public VarBitInit(TypedInit init, int bit)
        {
            ti = init;
            this.bit = bit;
            assert init.getType() != null && init.getType() instanceof BitsRecTy
                    &&((BitsRecTy)init.getType()).getNumBits() > bit
                    :"Illegal VarBitInit expression!";
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        public TypedInit getVariable() {return ti; }

        public int getBitNum() {return bit;}

        @Override
        public void print(PrintStream os)
        {
            ti.print(os);
            os.printf("{%d}", bit);
        }

        @Override
        public Init resolveReferences(Record r, RecordVal rval)
        {
            Init i = getVariable().resolveBitReference(r, rval, getBitNum());
            return i!= null? i : this;
        }

        @Override
        public VarBitInit clone()
        {
            return new VarBitInit(ti.clone(), bit);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;

            VarBitInit vbi = (VarBitInit)obj;
            return bit == vbi.bit && ti.equals(vbi.ti);
        }
    }

    /**
     * List[4] - Represent access to one element of a var or
     * field.
     */
    public static class VarListElementInit extends TypedInit
    {
        private TypedInit ti;
        private int element;

        public VarListElementInit(TypedInit init, int elt)
        {
            super(((ListRecTy)init.getType()).getElementType());
            ti = init;
            element = elt;
            assert init.getType() != null && init.getType() instanceof ListRecTy
                    :"Illegal VarBitInit expression!";
        }

        public TypedInit getVariable() {return ti;}

        public int getElementNum() {return element; }

        @Override
        public void print(PrintStream os)
        {
            ti.print(os);
            os.printf("[%d]", element);
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        @Override
        public Init resolveBitReference(Record r, RecordVal rval, int bit)
        {
            return null;
        }

        @Override
        public Init resolveListElementReference(Record r, RecordVal rval,
                int elt)
        {
            return null;
        }

        @Override
        public VarListElementInit clone()
        {
            return new VarListElementInit(ti.clone(), element);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;

            VarListElementInit vle = (VarListElementInit)obj;
            return ti.equals(vle.ti) && element == vle.element;
        }

        @Override
        public Init resolveReferences(Record r, RecordVal rval)
        {
            Init i = getVariable().resolveListElementReference(r, rval, getElementNum());
            return i != null ? i : this;
        }
    }

    /**
     * Represent a reference to a 'def' in the description.
     */
    public static class DefInit extends TypedInit
    {
        private Record def;

        public DefInit(Record d)
        {
            super(new RecordRecTy(d));
            def = d;
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        public Record getDef()
        {
            return def;
        }

        @Override
        public RecTy getFieldType(String fieldName)
        {
            RecordVal rv = def.getValue(fieldName);
            return rv != null? rv.getType() : null;
        }

        @Override
        public Init getFieldInit(Record r, String fieldName)
        {
            return def.getValue(fieldName).getValue();
        }

        @Override
        public void print(PrintStream os)
        {
            os.print(def.getName());
        }

        @Override
        public Init resolveBitReference(Record r, RecordVal rval, int bit)
        {
            assert false:"Illegal bit reference off def";
            return null;
        }

        @Override
        public Init resolveListElementReference(Record r, RecordVal rval,
                int elt)
        {
            assert false:"Illegal element reference off def";
            return null;
        }

        @Override
        public DefInit clone()
        {
            return new DefInit(def);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;
            DefInit di = (DefInit)obj;
            return def.equals(di.def);
        }
    }

    /**
     * (v a, b) - Represent a DAG tree value.  DAG inits are required
     * to have at least one value then a (possibly empty) list of arguments.  Each
     * argument can have a name associated with it.
     */
    public static class DagInit extends TypedInit
    {
        private Init val;
        private String varName;
        private ArrayList<Init> args;
        private ArrayList<String> argNames;

        public DagInit(Init val, String vn,  ArrayList<Pair<Init, String>> args)
        {
            super(new RecTy.DagRecTy());
            this.val = val;
            varName = vn;
            this.args = new ArrayList<>(args.size());
            argNames = new ArrayList<>(args.size());
            args.forEach(pair->{
                this.args.add(pair.first);
                argNames.add(pair.second);
            });
        }

        public DagInit(Init val,String vn, ArrayList<Init> args, ArrayList<String> argNames)
        {
            super(new RecTy.DagRecTy());
            this.val = val;
            varName = vn;
            this.args = args;
            this.argNames = argNames;
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        public Init getOperator()
        {
            return val;
        }

        public int getNumArgs()
        {
            return args.size();
        }

        public Init getArg(int num)
        {
            assert num>= 0 && num < args.size():"Arg number out of range!";
            return args.get(num);
        }

        public String getArgName(int num)
        {
            assert num>= 0 && num < argNames.size():"Arg number out of range!";
            return argNames.get(num);
        }

        public void setArg(int num, Init init)
        {
            assert num>= 0 && num < args.size():"Arg number out of range!";
            args.set(num, init);
        }

        public void setArgName(int num, String argName)
        {
            assert num>= 0 && num < argNames.size():"Arg number out of range!";
            argNames.set(num, argName);
        }

        @Override
        public Init resolveReferences(Record r, RecordVal rval)
        {
            ArrayList<Init> newArgs = new ArrayList<>();
            for (Init i : args)
                newArgs.add(i.resolveReferences(r, rval));

            Init op = val.resolveReferences(r, rval);
            if (!args.equals(newArgs) || !op.equals(val))
                return new DagInit(op, "", newArgs, argNames);
            return this;
        }

        @Override
        public void print(PrintStream os)
        {
            os.print("(");
            val.print(os);
            if (!varName.isEmpty())
                os.printf(":%s", varName);

            if (!args.isEmpty())
            {
                os.print(" ");
                args.get(0).print(os);
                if (!argNames.get(0).isEmpty())
                    os.printf(":$%s", argNames.get(0));
                for (int i = 1, e = args.size(); i < e; i++)
                {
                    os.print(", ");
                    args.get(i).print(os);
                    if (!argNames.get(i).isEmpty())
                        os.printf(":$%s", argNames.get(i));
                }
            }
            os.print(")");
        }

        @Override
        public Init resolveBitReference(Record r, RecordVal rval, int bit)
        {
            assert false:"Illegal bit reference off dag";
            return null;
        }

        @Override
        public Init resolveListElementReference(Record r, RecordVal rval,
                int elt)
        {
            assert false:"Illegal element reference off dag";
            return null;
        }

        @Override
        public DagInit clone()
        {
            ArrayList<Init> argList = new ArrayList<>();
            ArrayList<String> argNameList = new ArrayList<>();
            args.forEach(e->argList.add(e.clone()));
            argNameList.addAll(argNames);

            return new DagInit(val.clone(), varName, argList, argNameList);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;
            DagInit di = (DagInit)obj;

            return val.equals(di.val) && varName.equals(di.varName)
                    && args.equals(di.args) && argNames.equals(di.argNames);
        }

        public String getName()
        {
            return varName;
        }
    }

    /**
     * 'Opcode' - Represent a reference to an entire variable object.
     */
    public static class VarInit extends TypedInit
    {
        private String varName;

        public VarInit(String name, RecTy ty)
        {
            super(ty);
            varName = name;
        }

        public String getName()
        {
            return varName;
        }

        @Override
        public void print(PrintStream os)
        {
            os.print(varName);
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        @Override
        public Init resolveBitReference(Record r, RecordVal rval, int bit)
        {
            if (r.isTemplateArg(getName())) return null;
            if (rval != null && !rval.getName().equals(getName())) return null;

            RecordVal rv = r.getValue(getName());
            assert rv != null :"Reference to a non-existant variable?";
            assert rv.getValue() instanceof BitsInit;
            BitsInit bi = (BitsInit)rv.getValue();

            assert bit < bi.getNumBits() :"Bit reference out of range!";
            Init b = bi.getBit(bit);

            if (!(b instanceof UnsetInit))
                return b;
            return null;
        }

        @Override
        public Init resolveListElementReference(Record r, RecordVal rval,
                int elt)
        {
            if (r.isTemplateArg(getName())) return null;
            if (rval != null && rval.getName() != getName()) return null;

            RecordVal rv = r.getValue(getName());
            assert rv != null :"Reference to a non-existant variable?";
            assert rv.getValue() instanceof ListInit :"Invalid list element!";
            ListInit li = (ListInit)rv.getValue();

            if (elt >= li.getSize())
                return null;

            Init e = li.getElement(elt);
            if (!(e instanceof UnsetInit))
                return e;
            return null;
        }

        @Override
        public VarInit clone()
        {
            return new VarInit(varName, getType());
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;

            return varName.equals(((VarInit)obj).varName);
        }

        @Override
        public RecTy getFieldType(String fieldName)
        {
            if (getType() instanceof RecordRecTy)
            {
                RecordRecTy rty = (RecordRecTy)getType();
                RecordVal rv = rty.getRecord().getValue(fieldName);
                if (rv != null)
                    return rv.getType();
            }
            return null;
        }

        @Override
        public Init getFieldInit(Record r, String fieldName)
        {
            if (getType() instanceof RecordRecTy)
            {
                RecordRecTy rty = (RecordRecTy)getType();
                RecordVal rv = r.getValue(varName);
                if (rv != null)
                {
                    Init theInit = rv.getValue();
                    assert theInit != this :"Infinite loop detected";
                    Init i = theInit.getFieldInit(r, fieldName);
                    return i;
                }
            }
            return null;
        }

        @Override
        public Init resolveReferences(Record r, RecordVal rval)
        {
            RecordVal val = r.getValue(varName);
            if (rval == val || (rval == null && !(val.getValue() instanceof UnsetInit)))
                return val.getValue();
            return this;
        }
    }

    /**
     * X.Y - Represent a reference to a subfield of a variable.
     */
    public static class FieldInit extends TypedInit
    {
        /**
         * Record we are referring to.
         */
        private Init rec;
        /**
         * Field we re accessing.
         */
        private String fieldName;

        FieldInit(Init r, String fname)
        {
            super(r.getFieldType(fname));
            rec = r;
            fieldName = fname;
            assert getType() != null :"FieldInit with non record type!";
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }

        @Override
        public Init resolveBitReference(Record r, RecordVal rval, int bit)
        {
            Init bitsVal = rec.getFieldInit(r, fieldName);
            if (bitsVal != null)
            {
                if (bitsVal instanceof BitsInit)
                {
                    BitsInit bi = (BitsInit)bitsVal;
                    assert bit < bi.getNumBits() :"Bit reference out of range!";
                    Init b = bi.getBit(bit);

                    if (b instanceof BitInit)
                        return b;
                }
            }
            return null;
        }

        @Override
        public Init resolveListElementReference(Record r, RecordVal rval,
                int elt)
        {
            Init listVal = rec.getFieldInit(r, fieldName);
            if (listVal != null)
            {
                if (listVal instanceof ListInit)
                {
                    ListInit li = (ListInit)listVal;
                    if (elt >= li.getSize()) return null;
                    Init e = li.getElement(elt);

                    if (!(e instanceof UnsetInit))
                        return e;
                }
            }
            return null;
        }

        @Override
        public FieldInit clone()
        {
            return new FieldInit(rec.clone(), fieldName);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;
            FieldInit fi = (FieldInit)obj;

            return rec.equals(fi.rec) && fieldName.equals(fi.fieldName);
        }

        @Override
        public Init resolveReferences(Record r, RecordVal rval)
        {
            Init newRec = (rval != null ? rec.resolveReferences(r, rval) : rec);

            Init bitsVal = newRec.getFieldInit(r, fieldName);
            if (bitsVal != null)
            {
                Init bvr = bitsVal.resolveReferences(r, rval);
                return bvr.isComplete() ? bvr : this;
            }

            if (!newRec.equals(rec))
            {
                /**
                dump();
                newRec.dump();
                System.err.println();
                */
                return new FieldInit(newRec, fieldName);
            }
            return this;
        }

        @Override
        public void print(PrintStream os)
        {
            rec.print(os);
            os.printf(".%s", fieldName);
        }
    }

    /**
     * Super class for operators.
     */
    public static abstract class OpInit extends TypedInit
    {
        public OpInit(RecTy type)
        {
            super(type);
        }

        public abstract int getNumOperands();

        public abstract Init getOperand(int i);

        public abstract Init fold(Record curRec, MultiClass curMultiClass)
                throws Exception;

        public abstract OpInit clone(List<Init> operands);

        @Override
        public Init resolveBitReference(Record r, RecordVal rval, int bit)
        {
            try
            {
                Init folded = fold(r, null);
                if (!folded.equals(this))
                {
                    if (folded instanceof TypedInit)
                    {
                        TypedInit typed = (TypedInit) folded;
                        return typed.resolveBitReference(r, rval, bit);
                    }
                }
                return null;
            }
            catch (Exception e)
            {
                return null;
            }
        }

        @Override
        public Init resolveListElementReference(Record r, RecordVal rval,
                int elt)
        {
            try
            {

                Init folded = fold(r, null);
                if (!folded.equals(this))
                {
                    if (folded instanceof TypedInit)
                    {
                        TypedInit typed = (TypedInit) folded;
                        return typed.resolveListElementReference(r, rval, elt);
                    }
                }
                return null;
            }
            catch (Exception e)
            {
                return null;
            }
        }

        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
        }
    }

    public static final class UnOpInit extends OpInit
    {
        public enum UnaryOp
        {
            CAST, CAR, CDR, LNULL
        }
        private UnaryOp opc;
        private Init lhs;

        public UnOpInit(UnaryOp opc, Init lhs, RecTy type)
        {
            super(type);
            this.lhs = lhs;
            this.opc = opc;
        }

        public UnaryOp getOpcode()
        {
            return opc;
        }

        public Init getOperand()
        {
            return lhs;
        }

        @Override
        public void print(PrintStream os)
        {
            os.print(toString());
        }

        @Override
        public int getNumOperands()
        {
            return 1;
        }

        @Override
        public Init getOperand(int i)
        {
            assert i >= 0 && i <1;
            return lhs;
        }

        @Override
        public Init fold(Record curRec, MultiClass curMultiClass)
                throws Exception
        {
            switch (opc)
            {
                default:
                    assert false:"Unknown unop";
                    return null;
                case CAST:
                {
                    if (getType().toString().equals("string"))
                    {
                        if (lhs instanceof StringInit)
                            return lhs;

                        if (lhs instanceof DefInit)
                            return new StringInit(((DefInit)lhs).getDef().getName());
                    }
                    else
                    {
                        if (lhs instanceof StringInit)
                        {
                            StringInit stri = (StringInit)lhs;
                            String name = stri.getValue();

                            if (curRec != null)
                            {
                                RecordVal rv = curRec.getValue(name);
                                if (rv != null)
                                {
                                    if (!rv.getType().equals(getType()))
                                    {
                                        throw new Exception(
                                                "type mismatch in nameconcat");
                                    }
                                    return new VarInit(name, rv.getType());
                                }

                                String templateArgName = curRec.getName() + ":" + name;
                                if (curRec.isTemplateArg(templateArgName))
                                {
                                    rv = curRec.getValue(templateArgName);
                                    assert rv != null :"Template arg doesn't exist?";

                                    if (!rv.getType().equals(getType()))
                                        throw new Exception("type mismatch in nameconcat");

                                    return new VarInit(templateArgName, rv.getType());
                                }
                            }

                            if (curMultiClass != null)
                            {
                                String mcName = curMultiClass.rec.getName();
                                if (curMultiClass.rec.isTemplateArg(mcName))
                                {
                                    RecordVal rv = curMultiClass.rec.getValue(mcName);
                                    assert rv != null :"Template arg doesn't exist?";

                                    if (!rv.getType().equals(getType()))
                                    {
                                        throw new Exception("type mismatch in nameconat");
                                    }
                                    return new VarInit(mcName, rv.getType());
                                }
                            }

                            Record d = Record.records.getDef(name);
                            if (d != null)
                                return new DefInit(d);

                            System.err.println("Variable not defined: '" + name + "'");
                            assert false:"Variale not found";
                            return null;
                        }
                    }
                    break;
                }
                case CAR:
                {
                    if (lhs instanceof ListInit)
                    {
                        ListInit li = (ListInit)lhs;
                        if (li.getSize() == 0)
                        {
                            assert false:"empty list in car";
                            return null;
                        }
                        return li.getElement(0);
                    }
                    break;
                }
                case CDR:
                {
                    if (lhs instanceof ListInit)
                    {
                        ListInit li = (ListInit)lhs;
                        if (li.getSize() == 0)
                        {
                            assert false:"empty list in cdr";
                            return null;
                        }
                        ArrayList<Init> list = new ArrayList<>();
                        for (int i = 1; i < li.getSize(); i++)
                            list.add(li.getElement(i));

                        return new ListInit(list, li.getType());
                    }
                    break;
                }
                case LNULL:
                {
                    if (lhs instanceof ListInit)
                    {
                        ListInit li = (ListInit)lhs;
                        if (li.getSize() == 0)
                            return new IntInit(1);
                        else
                            return new IntInit(0);
                    }
                    if (lhs instanceof StringInit)
                    {
                        StringInit si = (StringInit)lhs;
                        if (si.getValue().isEmpty())
                            return new IntInit(1);
                        else
                            return new IntInit(0);
                    }
                    break;
                }
            }
            return this;
        }

        @Override
        public OpInit clone(List<Init> operands)
        {
            assert operands.size() == 1:"Wrong number of operands for unary operation";
            return new UnOpInit(opc, operands.get(0), getType());
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            switch (opc)
            {
                case CAST:
                    sb.append("!cast<");
                    sb.append(getType().toString());
                    sb.append(">");
                    break;
                case CAR:
                    sb.append("!car");
                    break;
                case CDR:
                    sb.append("!cdr");
                    break;
                case LNULL:
                    sb.append("!null");
                    break;
            }
            return sb.append("(").append(lhs.toString()).append(")").toString();
        }

        @Override
        public UnOpInit clone()
        {
            return new UnOpInit(opc, lhs.clone(), getType());
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (obj == null) return false;
            if(getClass() != obj.getClass())
                return false;
            UnOpInit uo = (UnOpInit)obj;

            return opc == uo.opc && lhs.equals(uo.lhs);
        }
    }
}
