package backend.bitcode.reader;

import backend.support.LLVMContext;
import backend.value.MDNode;
import backend.value.Value;
import tools.Util;

import java.util.ArrayList;
import java.util.Collections;

/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */
public class BitcodeReaderMDValueList {
    private ArrayList<Value> values;
    private LLVMContext context;
    BitcodeReaderMDValueList(LLVMContext ctx) {
        values = new ArrayList<>();
        context = ctx;
    }
    public int size() { return values.size(); }
    public void addLast(Value val) { values.add(val); }
    public void clear() { values.clear();}
    public void removeLast() { values.remove(size()-1); }
    public Value getLast() { return values.get(size()-1); }
    public boolean isEmpty() { return values.isEmpty(); }
    public void resize(int size) {
        if (size == size()) return;
        if (size < size()) {
            while (size() > size)
                removeLast();
        } else {
            values.addAll(Collections.nCopies(size - size(), null));
        }
    }
    public void shrinkTo(int size) {
        Util.assertion(size <= size(), "Invalid shrinkTo function call!");
        resize(size);
    }
    public Value getValueFwdRef(int idx) {
        if (idx >= size())
            resize(idx+1);

        Value val = values.get(idx);
        if (val != null) {
            Util.assertion(val.getType().isMetadataTy(), "Type mismatch in value table!");
            return val;
        }

        // create and return a placeholder.
        val = MDNode.getTemporary(context, new Value[0]);
        values.set(idx, val);
        return val;
    }
    public void assignValue(Value v, int idx) {
        if (idx == size()) {
            addLast(v);
            return;
        }

        if (idx > size())
            resize(idx+1);

        Value oldVal = values.get(idx);
        if (oldVal == null) {
            values.set(idx, v);
            return;
        }
        // handle forward reference
        oldVal.replaceAllUsesWith(v);
        values.set(idx, v);
    }
}
