package backend.value;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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
import backend.type.Type;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public final class NamedMDNode extends Value {
  private Module parent;
  private ArrayList<MDNode> operands;

  public NamedMDNode(LLVMContext ctx, String name, MDNode[] elts, Module m) {
    this(ctx, name, Arrays.asList(elts), m);
  }

  public NamedMDNode(LLVMContext ctx, String name, List<MDNode> elts, Module m) {
    super(Type.getMetadataTy(ctx), ValueKind.NamedMDNodeVal, name);
    parent = m;
    this.name = name;
    operands = new ArrayList<>();
    operands.addAll(elts);
  }

  public NamedMDNode(LLVMContext ctx, String name) {
    this(ctx, name, new ArrayList<>(), null);
  }

  public static NamedMDNode create(LLVMContext ctx,
                                   String name,
                                   ArrayList<MDNode> elts,
                                   Module m) {
    return new NamedMDNode(ctx, name, elts, m);
  }

  public int getNumOfOperands() {
    return operands.size();
  }

  public MDNode getOperand(int index) {
    Util.assertion(index >= 0 && index < getNumOfOperands());
    return operands.get(index);
  }

  public void addOperand(MDNode n) {
    operands.add(n);
  }

  public void setOperand(int idx, MDNode node) {
    Util.assertion(idx >= 0 && idx < getNumOfOperands());
    operands.set(idx, node);
  }

  public void setParent(Module module) {
    parent = module;
  }

  public Module getParent() {
    return parent;
  }
}
