package backend.value;

import backend.support.LLVMContext;
import backend.type.ArrayType;
import backend.type.Type;
import backend.value.UniqueConstantValueImpl.ConstantArrayKey;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;

import static backend.value.UniqueConstantValueImpl.getUniqueImpl;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class ConstantArray extends Constant {
  /**
   * Constructs a new instruction representing the specified constant.
   *
   * @param ty
   * @param elementVals
   */
  ConstantArray(ArrayType ty, ArrayList<Constant> elementVals) {
    super(ty, ValueKind.ConstantArrayVal);
    reserve(elementVals.size());

    Util.assertion(elementVals.size() == ty.getNumElements(), "Invalid initializer vector for constant array");

    for (int i = 0, e = elementVals.size(); i < e; i++) {
      Constant c = elementVals.get(i);
      Util.assertion(c.getType().equals(ty.getElementType()), "Initializer for array element doesn't match array element type!");

      setOperand(i, new Use(c, this));
    }
  }

  /**
   * This method constructs a ConstantArray and initializes it with a text
   * string. The default behavior (AddNull==true) causes a null terminator to
   * be placed at the end of the array. This effectively increases the length
   * of the array by one (you've been warned).  However, in some situations
   * this is not desired so if AddNull==false then the string is copied without
   * null termination.
   *
   * @param str
   * @param addNull
   * @return
   */
  public static Constant get(LLVMContext ctx, String str, boolean addNull) {
    ArrayList<Constant> eltVals = new ArrayList<>(32);
    for (int i = 0; i < str.length(); i++)
      eltVals.add(ConstantInt.get(Type.getInt8Ty(ctx), str.charAt(i)));

    // Add a null terminator into eltVals if addNull is true.
    if (addNull)
      eltVals.add(ConstantInt.get(Type.getInt8Ty(ctx), 0));

    ArrayType aty = ArrayType.get(Type.getInt8Ty(ctx), eltVals.size());
    return get(aty, eltVals);
  }

  public static Constant get(ArrayType ty, Constant[] elementVals) {
    return get(ty, new ArrayList<>(Arrays.asList(elementVals)));
  }

  public static Constant get(ArrayType ty, ArrayList<Constant> elementVals) {
    // If this is an all-zero array, return a ConstantAggregateZero object
    if (!elementVals.isEmpty()) {
      ConstantArrayKey key = new ConstantArrayKey(ty, elementVals);
      Constant c = elementVals.get(0);
      if (!c.isNullValue()) {
        return getUniqueImpl().getOrCreate(key);
      }

      for (int i = 1, e = elementVals.size(); i < e; i++) {
        if (!elementVals.get(i).isNullValue())
          return getUniqueImpl().getOrCreate(key);
      }
    }
    return ConstantAggregateZero.get(ty);
  }

  @Override
  public boolean isNullValue() {
    return false;
  }

  @Override
  public ArrayType getType() {
    return (ArrayType) super.getType();
  }

  /**
   * This method returns true if the array is an array of Int8Ty and
   * if the elements of the array are all ConstantInt's.
   *
   * @return
   */
  public boolean isString() {
    if (!getType().getElementType().isIntegerTy(8))
      return false;

    for (int i = 0, e = getNumOfOperands(); i < e; i++)
      if (!(operand(i) instanceof ConstantInt))
        return false;

    return true;
  }

  @Override
  public Constant operand(int idx) {
    return super.operand(idx);
  }

  @Override
  public void replaceUsesOfWithOnConstant(Value from, Value to, Use u) {
    Util.assertion(to instanceof Constant, "Can't make Constant refer to non-constant");

    Constant toV = (Constant) to;

    ArrayList<Constant> values = new ArrayList<>();
    boolean isAllZeros = false;
    int numUpdated = 0;

    if (!toV.isNullValue()) {
      for (Use use : operandList) {
        Constant val = (Constant) use.getValue();
        if (val.equals(from)) {
          val = toV;
          ++numUpdated;
        }
        values.add(val);
      }
    } else {
      isAllZeros = true;
      for (Use use : operandList) {
        Constant val = (Constant) use.getValue();
        if (val.equals(from)) {
          val = toV;
          ++numUpdated;
        }
        values.add(val);
        if (isAllZeros) isAllZeros = val.isNullValue();
      }
    }
    Constant replacement;
    if (isAllZeros)
      replacement = ConstantAggregateZero.get(getType());
    else {
      ConstantArrayKey key = new ConstantArrayKey(getType(), values);
      if (UniqueConstantValueImpl.ArrayConstants.containsKey(key)) {
        replacement = getUniqueImpl().getOrCreate(key);
      } else {
        for (int i = 0, e = getNumOfOperands(); i < e; i++) {
          if (operand(i).equals(from))
            setOperand(i, toV);
        }
        ConstantArray ca = new ConstantArray(getType(), values);
        UniqueConstantValueImpl.ArrayConstants.put(key, ca);
        return;
      }
    }
    Util.assertion(!replacement.equals(this), "I didn't contain from!");
    replaceAllUsesWith(replacement);
    destroyConstant();
  }

  @Override
  public void destroyConstant() {
    getUniqueImpl().remove(this);
  }

  public void setOperand(int idx, Constant c) {
    super.setOperand(idx, c, this);
  }

  public String getAsString() {
    Util.assertion(isString(), "Not a string");
    StringBuilder sb = new StringBuilder();

    for (int i = 0, e = getNumOfOperands(); i != e; i++) {
      sb.append((char)((ConstantInt) operand(i)).getZExtValue());
    }
    return sb.toString();
  }
}
