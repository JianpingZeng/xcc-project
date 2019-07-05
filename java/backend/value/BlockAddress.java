/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 *
 * Please refer the LICENSE for detail.
 */

package backend.value;

import backend.type.PointerType;
import backend.type.Type;
import tools.Pair;
import tools.Util;

import java.util.HashMap;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class BlockAddress extends Constant {

  private static final HashMap<Pair<Function, BasicBlock>, BlockAddress> UniqueBlockAddresses =
      new HashMap<>();
  /**
   * Constructs a new instruction representing the specified constants.
   */
  private BlockAddress(Function f, BasicBlock bb) {
    super(PointerType.getUnqual(Type.getInt8Ty(f.getContext())), ValueKind.BlockAddressVal);
    reserve(2);
    setOperand(0, f);
    setOperand(1, bb);
  }

  public static BlockAddress get(Function f, BasicBlock bb) {
    Pair<Function, BasicBlock> key = Pair.get(f, bb);
    if (UniqueBlockAddresses.containsKey(key))
      return UniqueBlockAddresses.get(key);

    BlockAddress res = new BlockAddress(f, bb);
    UniqueBlockAddresses.put(key, res);
    return res;
  }

  public static BlockAddress get(BasicBlock bb) {
    Util.assertion(bb.getParent() != null);
    return get(bb.getParent(), bb);
  }

  public Function getFunction() {
    return (Function)getOperand(1).getValue();
  }
  public BasicBlock getBasicBlock() {
    return (BasicBlock)getOperand(0).getValue();
  }

  @Override
  public boolean isNullValue() {
    return false;
  }

  @Override
  public void destroyConstant() {
    UniqueBlockAddresses.remove(Pair.get(getFunction(), getBasicBlock()));
  }

  @Override
  public void replaceUsesOfWithOnConstant(Value from, Value to, Use u) {
    Function newF = getFunction();
    BasicBlock newBB = getBasicBlock();
    if (u == getOperand(0))
      newF = (Function) to;
    else
      newBB = (BasicBlock) to;

    Pair<Function, BasicBlock> key = Pair.get(newF, newBB);
    if (!UniqueBlockAddresses.containsKey(key)) {
      UniqueBlockAddresses.put(key, this);
      setOperand(0, newF);
      setOperand(1, newBB);
      return;
    }
    BlockAddress newBA = UniqueBlockAddresses.get(key);
    Util.assertion(newBA != this);
    uncheckedReplaceAllUsesWith(newBA);
    destroyConstant();
  }
}
