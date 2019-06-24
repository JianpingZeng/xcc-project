/*
 * Extremely C language Compiler
 *   Copyright (c) 2015-2019, Jianping Zeng.
 *
 * Licensed under the BSD License version 3. Please refer LICENSE
 * for details.
 */

package backend.bitcode.writer;

import backend.support.AttrList;
import backend.support.LLVMContext;
import backend.support.ValueSymbolTable;
import backend.type.Type;
import backend.value.*;
import backend.value.Instruction.CallInst;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.OutRef;
import tools.Pair;
import tools.Util;

import java.util.*;

public class ValueEnumerator {
  private TObjectIntHashMap<Type> typeMap;
  private ArrayList<Pair<Type, Integer>> types;
  private TObjectIntHashMap<Value> valueMap;
  private ArrayList<Pair<Value, Integer>> values;
  private ArrayList<Pair<Value, Integer>> mdValues;
  private ArrayList<MDNode> functionLocalMDs;
  private TObjectIntHashMap<Value> mdValueMap;
  private TObjectIntHashMap<AttrList> attributeMap;
  private ArrayList<AttrList> attributes;

  private TObjectIntHashMap<BasicBlock> globalBasicBlockIDs;
  private TObjectIntHashMap<Instruction> instructionMap;
  private int instructionCount;

  private ArrayList<BasicBlock> basicBlocks;

  private int numModuleValues;
  private int numModuleMDValues;
  private int firstFuncConstantID;
  private int firstInstID;

  public ValueEnumerator(Module m) {
    // initialize members.
    typeMap = new TObjectIntHashMap<>();
    types = new ArrayList<>();
    valueMap = new TObjectIntHashMap<>();
    values = new ArrayList<>();
    mdValues = new ArrayList<>();
    functionLocalMDs = new ArrayList<>();
    mdValueMap = new TObjectIntHashMap<>();
    attributeMap = new TObjectIntHashMap<>();
    attributes = new ArrayList<>();

    globalBasicBlockIDs = new TObjectIntHashMap<>();
    instructionMap = new TObjectIntHashMap<>();
    instructionCount = 0;
    basicBlocks = new ArrayList<>();
    numModuleValues = 0;
    numModuleMDValues = 0;
    firstFuncConstantID = 0;
    firstInstID = 0;

    // enumerate global variables.
    for (GlobalVariable gv : m.getGlobalVariableList())
      enumerateValue(gv);

    // enumerate functions.
    for (Function fn : m.getFunctionList()) {
      enumerateValue(fn);
      enumerateAttributes(fn.getAttributes());
    }

    // enumerate aliases.
    for (GlobalAlias ga : m.getAliasList())
      enumerateValue(ga);

    // Remember what is the cutoff between globalvalue's and other constants.
    int firstConstants = values.size();

    // enumerate the global variable initializers.
    for (GlobalVariable gv : m.getGlobalVariableList()) {
      if (gv.hasInitializer())
        enumerateValue(gv.getInitializer());
    }

    // enumerate aliases.
    for (GlobalAlias ga : m.getAliasList())
      enumerateValue(ga.getAliasee());

    // enumerate types used by type symbol table.
    enumerateTypeSymbolTable(m.getTypeSymbolTable());;

    enumerateValueSymbolTable(m.getValueSymbolTable());
    enumerateNamedMetadata(m);

    ArrayList<Pair<Integer, MDNode>> mds = new ArrayList<>();

    // enumeerate types used by function bodies and argument lists.
    for (Function fn : m.getFunctionList()) {
      for (Argument arg : fn.getArgumentList())
        enumerateType(arg.getType());

      for (BasicBlock bb : fn) {
        for (Instruction inst : bb) {
          for (int i = 0, e = inst.getNumOfOperands(); i < e; i++) {
            Value op = inst.operand(i);
            if (op instanceof MDNode) {
              MDNode md = (MDNode) op;
              if (md.isFunctionLocal() && md.getFunction() != null)
                // Remember what is the cutoff between globalvalue's and other constants.
                continue;

              enumerateOperandType(op);
            }
          }

          enumerateType(inst.getType());
          if (inst instanceof CallInst)
            enumerateAttributes(((CallInst)inst).getAttributes());
          // Enumerate metadata attached with this instruction.
          mds.clear();
          inst.getAllMetadataOtherThanDebugLoc(mds);
          for (Pair<Integer, MDNode> item : mds)
            enumerateMetadata(item.second);

          if (!inst.getDebugLoc().isUnknown()) {
            OutRef<MDNode> scope = new OutRef<>(), ia = new OutRef<>();
            inst.getDebugLoc().getScopeAndInlineAt(scope, ia, inst.getContext());
            if (scope.get() != null)
              enumerateMetadata(scope.get());
            if (ia.get() != null)
              enumerateMetadata(ia.get());
          }
        }
      }
    }

    // Optimize constant ordering.
    optimizeConstants(firstConstants, values.size());
    // Sort the type table by frequency so that most commonly used types are early
    // in the table (have low bit-width).
    types.sort(new Comparator<Pair<Type, Integer>>() {
      @Override
      public int compare(Pair<Type, Integer> o1, Pair<Type, Integer> o2) {
        return o1.second - o2.second;
      }
    });

    // Partition the Type ID's so that the single-value types occur before the
    // aggregate types.  This allows the aggregate types to be dropped from the
    // type table after parsing the global variable initializers.
    ArrayList<Pair<Type, Integer>> singleTypes = new ArrayList<>();
    ArrayList<Pair<Type, Integer>> aggregateTypes = new ArrayList<>();
    for (Pair<Type, Integer> item : types)
      if (item.first.isSingleValueType())
        singleTypes.add(item);
      else
        aggregateTypes.add(item);

    types.clear();
    types.addAll(singleTypes);
    types.addAll(aggregateTypes);

    // Now that we rearranged the type table, rebuild TypeMap.
    for (int i = 0, e = types.size(); i < e; i++)
      typeMap.put(types.get(i).first, i+1);
  }

  public int getValueID(Value val) {
    if (val instanceof MDNode || val instanceof MDString) {
      Util.assertion(mdValueMap.containsKey(val),
          "Value not in slot calculator!");
      return mdValueMap.get(val) - 1;
    }
    Util.assertion(valueMap.containsKey(val),
        "Value not in slot calculator!");
    return valueMap.get(val) - 1;
  }

  public int getTypeID(Type ty) {
    Util.assertion(typeMap.containsKey(true), "Type not in ValueEnumerator!");
    return typeMap.get(ty);
  }

  public int getInstructionID(Instruction inst) {
    Util.assertion(instructionMap.containsKey(inst),
        "Instruction is not mapped!");
    return instructionMap.get(inst);
  }

  public void setInstructionID(Instruction inst) {
    instructionMap.put(inst, instructionCount++);
  }

  public int getAttributeID(AttrList al) {
    Util.assertion(attributeMap.containsKey(al), "Attributes is not mapped!");
    return attributeMap.get(al);
  }

  public Pair<Integer, Integer> getFunctionConstantRange() {
    return Pair.get(firstFuncConstantID, firstInstID);
  }

  public ArrayList<Pair<Value, Integer>> getValues() {
    return values;
  }

  public ArrayList<Pair<Value, Integer>> getMdValues() {
    return mdValues;
  }

  public ArrayList<MDNode> getFunctionLocalMDValues() {
    return functionLocalMDs;
  }

  public ArrayList<Pair<Type, Integer>> getTypes() {
    return types;
  }

  public ArrayList<BasicBlock> getBasicBlocks() {
    return basicBlocks;
  }

  public ArrayList<AttrList> getAttributes() {
    return attributes;
  }

  public int getGlobalBasicBlockID(BasicBlock bb) {
    if (globalBasicBlockIDs.containsKey(bb))
      return globalBasicBlockIDs.get(bb) - 1;

    incorporateFunctionInfoGlobalBBIDs(bb.getParent(),
        globalBasicBlockIDs);
    return getGlobalBasicBlockID(bb);
  }

  private static void incorporateFunctionInfoGlobalBBIDs(Function fn,
                                                         TObjectIntHashMap<BasicBlock> globalBasicBlockIDs) {
    int counter = 0;
    for (BasicBlock bb : fn)
      globalBasicBlockIDs.put(bb, ++counter);
  }

  public void incorporateFunction(Function f) {
    instructionCount = 0;
    numModuleValues = values.size();
    numModuleMDValues = mdValues.size();

    // Adding function arguments to the value table.
    for (Argument arg : f.getArgumentList())
      enumerateValue(arg);

    firstFuncConstantID = values.size();
    // Add all function-level constants to the value table.
    for (BasicBlock bb : f) {
      for (Instruction inst : bb) {
        for (int i = 0, e = inst.getNumOfOperands(); i < e; i++) {
          Value op = inst.operand(i);
          if ((op instanceof Constant && !(op instanceof GlobalValue))
              || op instanceof InlineAsm)
            enumerateValue(op);
        }
        basicBlocks.add(bb);
        valueMap.put(bb, basicBlocks.size());
      }
    }

    // Optimize the constant layout.
    optimizeConstants(firstFuncConstantID, values.size());

    // Add the function's parameter attributes so they are available for use in
    // the function's instruction.
    enumerateAttributes(f.getAttributes());

    firstInstID = values.size();
    ArrayList<MDNode> fnLocalMDList = new ArrayList<>();
    // Add all of the instructions.
    for (BasicBlock bb : f) {
      for (Instruction inst : bb) {
        for (int i = 0, e = inst.getNumOfOperands(); i < e; i++) {
          Value op = inst.operand(i);
          if (op instanceof MDNode) {
            MDNode md = (MDNode) op;
            if (md.isFunctionLocal() && md.getFunction() != null)
              fnLocalMDList.add(md);
          }
        }

        ArrayList<Pair<Integer, MDNode>> mds = new ArrayList<>();
        inst.getAllMetadataOtherThanDebugLoc(mds);
        mds.forEach(item-> {
          MDNode n = item.second;
          if (n.isFunctionLocal() && n.getFunction() != null)
            fnLocalMDList.add(n);
        });

        if (!inst.getType().isVoidType())
          enumerateValue(inst);
      }
    }

    // Add all of the function-local metadata.
    fnLocalMDList.forEach(this::enumerateFunctionLocalMetadata);
  }

  public void purgeFunction() {
    // Remove purged values from the ValueMap.
    for (int i = numModuleValues, e = values.size(); i < e; i++)
      valueMap.remove(values.get(i).first);
    for (int i = numModuleMDValues, e = mdValues.size(); i < e; i++)
      mdValueMap.remove(mdValues.get(i).first);
    for (int i = 0, e = basicBlocks.size(); i < e; i++)
      valueMap.remove(basicBlocks.get(i));


    for (int i = values.size() - 1; i >= numModuleValues; i--)
      values.remove(i);
    for (int i = mdValues.size(); i >= numModuleMDValues; i--)
      mdValues.remove(i);

    basicBlocks.clear();
    functionLocalMDs.clear();
  }

  private static class CstSortPredicate implements Comparator<Pair<Value, Integer>> {

    private ValueEnumerator ve;

    CstSortPredicate(ValueEnumerator ve) {
      this.ve = ve;
    }

    @Override
    public int compare(Pair<Value, Integer> o1, Pair<Value, Integer> o2) {
      if (!o1.first.getType().equals(o2.first.getType()))
        return ve.getTypeID(o1.first.getType()) -
            ve.getTypeID(o2.first.getType());
      // then by frequency.
      return o2.second - o1.second;
    }

    @Override
    public boolean equals(Object obj) {
      return false;
    }
  }

  private void optimizeConstants(int cstStart, int cstEnd) {
    if (cstEnd == cstStart || cstStart + 1 == cstEnd) return;

    // Optimize constant ordering.
    CstSortPredicate p = new CstSortPredicate(this);
    values.subList(cstStart, cstEnd).sort(p);

    // Ensure that integer constants are at the start of the constant pool.  This
    // is important so that GEP structure indices come before gep constant exprs.
    ArrayList<Pair<Value, Integer>> integers = new ArrayList<>();
    ArrayList<Pair<Value, Integer>> nonIntegers = new ArrayList<>();
    for (Pair<Value, Integer> entry : values)
      if (entry.first.getType().isIntegerTy())
        integers.add(entry);
      else
        nonIntegers.add(entry);

    values.clear();
    values.addAll(integers);
    values.addAll(nonIntegers);

    // Rebuild the modified portion of ValueMap.
    for (; cstStart != cstEnd; cstStart++)
      valueMap.put(values.get(cstStart).first, cstStart+1);
  }

  private void enumerateMDNodeOperands(MDNode n) {
    for (int i = 0, e = n.getNumOperands(); i < e; i++) {
      Value v = n.getOperand(i);
      if (v != null) {
        if (v instanceof MDNode || v instanceof MDString)
          enumerateMetadata(v);
        else if (v instanceof Instruction)
          enumerateValue(v);
      }
      else
        enumerateType(LLVMContext.VoidTy);
    }
  }
  private void enumerateMetadata(Value md) {
    Util.assertion(md instanceof MDNode ||
        md instanceof MDString, "Invalid metadata kind!");
    enumerateType(md.getType());

    if (md instanceof MDNode) {
      MDNode n = (MDNode) md;
      if (n.isFunctionLocal() && n.getFunction() != null) {
        enumerateMDNodeOperands(n);
        return;
      }
    }

    // Check to see if it's already in!
    if (mdValueMap.containsKey(md)) {
      // Increment use count.
      mdValues.get(mdValueMap.get(md) - 1).second++;
      return;
    }

    mdValues.add(Pair.get(md, 1));
    int mdValueID = mdValues.size();
    mdValueMap.put(md, mdValueID);

    if (md instanceof MDNode)
      enumerateMDNodeOperands((MDNode) md);
  }
  private void enumerateFunctionLocalMetadata(MDNode n) {
    Util.assertion(n.isFunctionLocal() &&
    n.getFunction() != null,
        "enumerateFunctionLocalMetadata called on non-functin-local mdnode!");
    enumerateType(n.getType());
    if (mdValueMap.containsKey(n)) {
      mdValues.get(mdValueMap.get(n) - 1).second++;
      return;
    }

    mdValues.add(Pair.get(n, 1));
    mdValueMap.put(n, mdValues.size());

    // To incoroporate function-local information visit all function-local
    // MDNodes and all function-local values they reference.
    for (int i = 0, e = n.getNumOperands(); i< e; i++) {
      Value v = n.getOperand(i);
      if (v != null) {
        if (v instanceof MDNode) {
          MDNode md = (MDNode) v;
          if (md.isFunctionLocal() && md.getFunction() != null)
            enumerateFunctionLocalMetadata(md);
        }
        else if (v instanceof Instruction ||
            v instanceof Argument)
          enumerateValue(v);
      }
    }

    // Also, collect all function-local MDNodes for easy access.
    functionLocalMDs.add(n);
  }

  private void enumerateNamedMDNode(NamedMDNode nmd) {
    for (int i = 0, e = nmd.getNumOfOperands(); i < e; i++)
      enumerateMetadata(nmd.getOperand(i));
  }

  private void enumerateValue(Value val) {
    Util.assertion(!val.getType().isVoidType(),
        "Can't insert void values!");
    Util.assertion(!(val instanceof MDNode) &&
        !(val instanceof MDString), "enumerateValue can't handle Metadata!");
    // Check to see if it's alreay in!
    if (valueMap.containsKey(val)) {
      // Increment use count.
      values.get(valueMap.get(val) - 1).second++;
      return;
    }

    enumerateType(val.getType());

    // Enumerate the type of this value.
    if (val instanceof Constant) {
      Constant c = (Constant) val;
      if (val instanceof GlobalVariable) {
        // Enumerate the type of this value.
      } else if (c instanceof ConstantArray &&
          ((ConstantArray) c).isString()) {
        // Do not enumerate the initializers for an array of simple characters.
        // The initializers just polute the value table, and we emit the strings
        // specially.
      }
      else if (c.getNumOfOperands() != 0) {
        // If a constant has operands, enumerate them.  This makes sure that if a
        // constant has uses (for example an array of const ints), that they are
        // inserted also.

        // We prefer to enumerate them with values before we enumerate the user
        // itself.  This makes it more likely that we can avoid forward references
        // in the reader.  We know that there can be no cycles in the constants
        // graph that don't go through a global variable.
        for (int i = 0, e = c.getNumOfOperands(); i < e; i++) {
          Value op = c.operand(i);
          if (!(op instanceof BasicBlock))
            enumerateValue(op);
        }

        // Finally, add the value.  Doing this could make the ValueID reference be
        // dangling, don't reuse it.
        values.add(Pair.get(val, 1));
        valueMap.put(val, values.size());
        return;
      }
    }

    // Add the value.
    values.add(Pair.get(val, 1));
    valueMap.put(val, values.size());
  }

  private void enumerateType(Type t) {
    if (typeMap.containsKey(t)) {
      // If we've already seen this type, just increase its occurrence count.
      types.get(typeMap.get(t)-1).second++;
      return;
    }

    // First time we saw this type, add it.
    types.add(Pair.get(t, 1));
    typeMap.put(t, types.size());

    // Enumerate subtypes.
    for (int i = 0, e = t.getNumContainedTypes(); i < e; i++) {
      enumerateType(t.getContainedType(i));
    }
  }

  private void enumerateOperandType(Value v) {
    enumerateType(v.getType());

    if (v instanceof Constant) {
      Constant c = (Constant) v;
      // If this constant is already enumerated, ignore it, we know its type must
      // be enumerated.
      if (valueMap.containsKey(v))
        return;

      // This constant may have operands, make sure to enumerate the types in
      // them.
      for (int i = 0, e = c.getNumOfOperands(); i < e; i++) {
        Value op = c.operand(i);
        if (op instanceof BasicBlock)
          continue;

        enumerateOperandType(op);
      }

      if (v instanceof MDNode) {
        MDNode md = (MDNode) v;
        for (int i = 0, e = md.getNumOperands(); i < e; i++)
          if (md.getOperand(i) != null)
            enumerateOperandType(md.getOperand(i));;
      }
    }
    else if (v instanceof MDString || v instanceof MDNode)
      enumerateMetadata(v);
  }

  private void enumerateAttributes(AttrList al) {
    if (al.isEmpty()) return;
    // do a lookup.
    if (!attributeMap.containsKey(al)) {
      // never saw it before.
      attributes.add(al);
      attributeMap.put(al, attributes.size());
    }
  }

  private void enumerateTypeSymbolTable(TreeMap<String, Type> st) {
    for (Map.Entry<String, Type> entry : st.entrySet())
      enumerateType(entry.getValue());
  }
  private void enumerateValueSymbolTable(ValueSymbolTable st) {
    for (Map.Entry<String, Value> entry : st.getMap().entrySet())
      enumerateValue(entry.getValue());
  }
  private void enumerateNamedMetadata(Module m) {
    for (NamedMDNode md : m.getNamedMDList())
      enumerateNamedMDNode(md);
  }
}
