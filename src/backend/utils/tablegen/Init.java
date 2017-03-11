package backend.utils.tablegen;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
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
import backend.utils.tablegen.RecTy.BitsRecTy;
import backend.utils.tablegen.RecTy.ListRecTy;
import backend.utils.tablegen.RecTy.RecordRecTy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

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

        private UnsetInit() {}
        @Override
        public Init convertInitializerTo(RecTy ty)
        {
            return ty.convertValue(this);
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
        public void print(PrintStream os)
        {
            os.print(value? "1":"0");
        }
    }

    /**
     * Represents an initializer for a BitsRecTy value.
     * It contains a vector of bits, whose size is determined by the type.
     */
    public static class BitsInit extends Init
    {
        private ArrayList<Init> bits;
        public BitsInit(int size)
        {
            bits = new ArrayList<>();
            // Fixme: initialize each element with UnsetInit rather than 'null',
            // to avoid NullPointerException.
            for (int i = 0; i < size; i++)
                bits.add(UnsetInit.getInstance());
        }

        public int getNumBits() {return bits.size(); }

        public Init getBit(int bit)
        {
            assert bit < bits.size() :"Bit index out of range";
            return bits.get(bit);
        }

        public void setBit(int bit, Init val)
        {
            assert bit < bits.size() :"Bit index out of range";
            assert bits.get(bit) == null :"Bit already set!";
            bits.set(bit, val);
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
                Init bit = getBit(i);
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
            }

            if (changed)
                return newInit;
            return this;
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
    public static class IntInit extends Init
    {
        private int value;

        public IntInit(int val) {value = val;}

        public int getValue()
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
        public void print(PrintStream os)
        {
            os.print(value);
        }
    }

    /**
     * Represent an initalization by a literal string value.
     */
    public static class StringInit extends Init
    {
        private String value;
        public StringInit(String val)
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

        @Override public void print(PrintStream os)
        {
            os.printf("\"%s\"", value);
        }
    }

    /**
     * [AL, AH, CL] - Represent a list of defs.
     */
    public static class ListInit extends Init
    {
        private ArrayList<Init> values;

        public ListInit(ArrayList<Init> vals)
        {
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
            return new ListInit(vals);
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
            }
            if (changed)
                return new ListInit(resolved);
            return this;
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
    }

    /**
     * op (X, Y) - Combine two inits.
     */
    public static class BinOpInit extends Init
    {
        public enum BinaryOp{ SHL, SRA, SRL, STRCONCAT};

        private BinaryOp opc;
        private Init lhs, rhs;

        public BinOpInit(BinaryOp opc, Init lhs, Init rhs)
        {
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

        /**
         * If possible, fold this to a simpler init.  Return this if not
         * possible to fold.
         * @return
         */
        public Init fold()
        {
            switch (getOpcode())
            {
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
                        int lhsv = lhsi.getValue(), rhsv = rhsi.getValue();
                        int result;
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
            }
            return this;
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

            if (!lhs.equals(lhsi) || !rhs.equals(rhsi))
                return new BinOpInit(getOpcode(), lhsi, rhsi).fold();
            return fold();
        }

        @Override
        public void print(PrintStream os)
        {
            switch (opc)
            {
                case SHL: os.print("!shl"); break;
                case SRA: os.print("!sra"); break;
                case SRL: os.print("!srl"); break;
                case STRCONCAT: os.print("!strconcate"); break;
            }
            os.print("(");
            lhs.print(os);
            os.print(", ");
            rhs.print(os);
            os.print(")");
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
            return new ListInit(listInits);
        }

        public abstract Init resolveBitReference(Record r, RecordVal rval, int bit);

        public abstract Init resolveListElementReference(Record r, RecordVal rval,
                int elt);
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
            Init i = getVariable().resolveReferences(r, rval);
            return i!= null? i : this;
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
        public Init resolveReferences(Record r, RecordVal rval)
        {
            Init i = getVariable().resolveListElementReference(r, rval, getElementNum());
            return i != null ? i : this;
        }
    }

    /**
     * Represent a reference to a 'def' in the description.
     */
    public static class DefInit extends Init
    {
        private Record def;

        public DefInit(Record d)
        {
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
    }

    /**
     * (v a, b) - Represent a DAG tree value.  DAG inits are required
     * to have at least one value then a (possibly empty) list of arguments.  Each
     * argument can have a name associated with it.
     */
    public static class DagInit extends Init
    {
        private Init val;
        private ArrayList<Init> args;
        private ArrayList<String> argNames;

        public DagInit(Init val, ArrayList<Pair<Init, String>> args)
        {
            this.val = val;
            this.args = new ArrayList<>(args.size());
            argNames = new ArrayList<>(args.size());
            args.forEach(pair->{
                this.args.add(pair.first);
                argNames.add(pair.second);
            });
        }

        public DagInit(Init val, ArrayList<Init> args,
                ArrayList<String> argNames)
        {
            this.val = val;
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
            args.forEach(e->newArgs.add(e.resolveReferences(r, rval)));

            Init op = val.resolveReferences(r, rval);
            if (args != newArgs || op != val)
                return new DagInit(op, newArgs, argNames);
            return this;
        }

        @Override
        public void print(PrintStream os)
        {
            os.print("(");
            val.print(os);
            if (!args.isEmpty())
            {
                os.print(" ");
                args.get(0).print(os);
                if (!argNames.get(0).isEmpty())
                    os.printf(":$%f", argNames.get(0));
                for (int i = 1, e = args.size(); i < e; i++)
                {
                    os.print(", ");
                    args.get(i).print(os);
                    if (!argNames.get(i).isEmpty())
                        os.printf(":$%f", argNames.get(i));
                }
            }
            os.print(")");
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
            if (rval != null && rval.getName() != getName()) return null;

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
}
