package backend.llReader;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import backend.support.LLVMContext;
import backend.type.OpaqueType;
import backend.type.Type;
import backend.value.*;
import com.sun.javafx.binding.StringFormatter;
import tools.Pair;
import tools.SourceMgr.SMLoc;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class PerFunctionState
{
    private LLParser parser;
    private Function fn;
    private TreeMap<String, Pair<Value, SMLoc>> forwardRefVals;
    private TreeMap<Integer, Pair<Value, SMLoc>> forwardRefValIDs;
    private ArrayList<Value> numberedVals;

    public PerFunctionState(LLParser p, Function f)
    {
        parser = p;
        fn = f;
        forwardRefVals = new TreeMap<>();
        forwardRefValIDs = new TreeMap<>();
        numberedVals = new ArrayList<>();

        // insert unnamed arguments into the numberedVals list.
        for (Argument arg : fn.getArgumentList())
        {
            if (!arg.hasName())
                numberedVals.add(arg);
        }
    }

    public boolean verifyFunctionComplete()
    {
        if (!forwardRefVals.isEmpty())
        {
            Map.Entry<String, Pair<Value, SMLoc>> itr = forwardRefVals.entrySet().iterator().next();
            return parser.error(itr.getValue().second,
                    StringFormatter.format("use of undefined value '%%%s'", itr.getKey()).getValue());
        }

        if (!forwardRefValIDs.isEmpty())
        {
            Map.Entry<Integer, Pair<Value, SMLoc>> itr = forwardRefValIDs.entrySet().iterator().next();
            return parser.error(itr.getValue().second,
                    StringFormatter.format("use of undefined value '%%%d'", itr.getKey()).getValue());
        }
        return false;
    }

    public BasicBlock defineBB(String name, SMLoc nameLoc)
    {
        BasicBlock bb;
        if (name == null || name.isEmpty())
            bb = getBB(numberedVals.size(), nameLoc);
        else
            bb = getBB(name, nameLoc);
        if (bb == null)
            return null;    // already diagnosed error.

        // Remove bb from forward refs list.
        if (name == null || name.isEmpty())
        {
            forwardRefValIDs.remove(numberedVals.size());
            numberedVals.add(bb);
        }
        else
        {
            // BB forward references are already in the function symbol table.
            forwardRefVals.remove(name);
        }
        return bb;
    }

    public BasicBlock getBB(String name, SMLoc loc)
    {
        Value val = getVal(name, LLVMContext.LabelTy, loc);
        return val instanceof BasicBlock ? (BasicBlock)val : null;
    }

    public BasicBlock getBB(int id, SMLoc loc)
    {
        Value val = getVal(id, LLVMContext.LabelTy, loc);
        return val instanceof BasicBlock ? (BasicBlock)val : null;
    }

    /**
     * After an instruction is parsed and inserted into its
     * basic block, this installs its name.
     * @param nameID
     * @param nameStr
     * @param nameLoc
     * @param inst
     * @return
     */
    public boolean setInstName(int nameID, String nameStr, SMLoc nameLoc,
            Instruction inst)
    {
        if (inst.getType().equals(LLVMContext.VoidTy))
        {
            if (nameID != -1 || (nameStr != null && !nameStr.isEmpty()))
                return parser.error(nameLoc, "instruction returning void can't have a name");
            return false;
        }

        if (nameStr == null || nameStr.isEmpty())
        {
            if (nameID == -1)
                nameID = numberedVals.size();

            if (nameID != numberedVals.size())
                return parser.error(nameLoc,
                        StringFormatter.format("instruction expected to be numbered '%%%d'",
                                numberedVals.size()).getValue());

            if (forwardRefValIDs.containsKey(nameID))
            {
                Pair<Value, SMLoc> itr = forwardRefValIDs.get(nameID);
                if (!itr.first.getType().equals(inst.getType()))
                {
                    return parser.error(nameLoc,
                            StringFormatter.format("instruction forward referenced with type '%%%s'"
                                    , itr.first.getType().getDescription()).getValue());
                }
                itr.first.replaceAllUsesWith(inst);
                forwardRefValIDs.remove(nameID);
            }
            numberedVals.add(inst);
            return false;
        }

        // Otherwise, the instruction have a name, resolve forward refs.
        if (forwardRefVals.containsKey(nameStr))
        {
            Pair<Value, SMLoc> itr = forwardRefVals.get(nameStr);
            if (!itr.first.getType().equals(inst.getType()))
            {
                return parser.error(nameLoc, StringFormatter.format("instruction forward referenced with type '%%%s'",
                        itr.first.getType().getDescription()).getValue());
            }
            itr.first.replaceAllUsesWith(inst);
            forwardRefVals.remove(nameStr);
        }
        // set the name of instruction.
        /*
        if (inst.getName().equals(nameStr))
        {
            return parser.error(nameLoc, StringFormatter
                    .format("multiple definition of local value named '%%%s'",
                            nameStr).getValue());
        }*/
        inst.setName(nameStr);
        return false;
    }

    public Value getVal(int id, Type ty, SMLoc loc)
    {
        Value val =  id < numberedVals.size() ? numberedVals.get(id) : null;

        if (val == null)
        {
            if (forwardRefValIDs.containsKey(id))
                val = forwardRefValIDs.get(id).first;
        }

        if (val != null)
        {
            if (val.getType().equals(ty)) return val;
            if (ty.equals(LLVMContext.LabelTy))
                parser.error(loc, StringFormatter
                        .format("'%%%d' is not a basic block", id).toString());
            else
                parser.error(loc, StringFormatter.format("'%%%d' defined with type '%s'",
                        id, val.getType().getDescription()).getValue());
            return null;
        }

        if (!ty.isFirstClassType() && !(ty instanceof OpaqueType) &&
                !ty.equals(LLVMContext.LabelTy))
        {
            parser.error(loc, "invalid use of a non-first class type");
            return null;
        }


        // Otherwise, create a new forward reference for this value and remember it.
        Value fwdVal;
        if (ty.equals(LLVMContext.LabelTy))
            fwdVal = BasicBlock.createBasicBlock("", fn);
        else
            fwdVal = new Argument(ty,"", fn);

        forwardRefValIDs.put(id, Pair.get(fwdVal, loc));
        return fwdVal;
    }

    public Value getVal(String name, Type ty, SMLoc loc)
    {
        Value val = fn.getValueSymbolTable().getValue(name);

        if (val == null)
        {
            if (forwardRefVals.containsKey(name))
                val = forwardRefVals.get(name).first;
        }

        if (val != null)
        {
            if (val.getType().equals(ty)) return val;
            if (ty.equals(LLVMContext.LabelTy))
                parser.error(loc, StringFormatter
                        .format("'%%%s' is not a basic block", name).toString());
            else
                parser.error(loc, StringFormatter.format("'%%%s' defined with type '%s'",
                        name, val.getType().getDescription()).getValue());
            return null;
        }

        if (!ty.isFirstClassType() && !(ty instanceof OpaqueType) &&
                !ty.equals(LLVMContext.LabelTy))
        {
            parser.error(loc, "invalid use of a non-first class type");
            return null;
        }

        // Otherwise, create a new forward reference for this value and remember it.
        Value fwdVal;
        if (ty.equals(LLVMContext.LabelTy))
            fwdVal = BasicBlock.createBasicBlock(name, fn);
        else
            fwdVal = new Argument(ty, name, fn);
        forwardRefVals.put(name, Pair.get(fwdVal, loc));
        return fwdVal;
    }
}
