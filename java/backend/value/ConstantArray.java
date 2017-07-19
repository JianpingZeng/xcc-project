package backend.value;

import backend.type.ArrayType;
import backend.type.Type;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ConstantArray extends Constant
{
    private static class ConstantArrayKey
	{
		ArrayType type;
		ArrayList<Constant> eltVals;

		ConstantArrayKey(ArrayType type, ArrayList<Constant> eltVals)
		{
			this.type = type;
			this.eltVals = eltVals;
		}
	}

	/**
	 * A cache mapping pair of ArrayType and Constant value list to ConstantArray.
	 */
	private static HashMap<ConstantArrayKey, ConstantArray> arrayConstants;

	static {
		arrayConstants = new HashMap<>();
	}


	/**
	 * Constructs a new instruction representing the specified constant.
	 *
	 * @param ty
	 * @param elementVals
	 */
	private ConstantArray(ArrayType ty, ArrayList<Constant> elementVals)
	{
		super(ty,  ValueKind.ConstantArrayVal);
		reserve(elementVals.size());

		assert elementVals.size() == ty.getNumElements()
				:"Invalid initializer vector for constant array";
		for (int i = 0, e = elementVals.size(); i < e; i++)
		{
			Constant c = elementVals.get(i);
			assert c.getType() == ty.getElementType()
					:"Initializer for array element doesn't match array element type!";
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
	 * @param str
	 * @param addNull
	 * @return
	 */
	public static Constant get(String str, boolean addNull)
	{
		ArrayList<Constant> eltVals = new ArrayList<>(32);
		for (int i = 0; i < str.length(); i++)
			eltVals.add(ConstantInt.get(Type.Int8Ty, str.charAt(i)));

		// Add a null terminator into eltVals if addNull is true.
		if (addNull)
			eltVals.add(ConstantInt.get(Type.Int8Ty, 0));

		ArrayType aty = ArrayType.get(Type.Int8Ty, eltVals.size());
		return get(aty, eltVals);
	}

	public static Constant get(ArrayType ty, ArrayList<Constant> elementVals)
	{
		// If this is an all-zero array, return a ConstantAggregateZero object
		if (!elementVals.isEmpty())
		{
			Constant c = elementVals.get(0);
			if (!c.isNullValue())
			{
				return getOrCreateConstantArray(ty, elementVals);
			}

			for (int i = 1, e = elementVals.size(); i < e; i++)
			{
				if (!elementVals.get(i).isNullValue())
					return getOrCreateConstantArray(ty, elementVals);
			}
		}
		return ConstantAggregateZero.get(ty);
	}

	public static ConstantArray getOrCreateConstantArray(ArrayType ty, ArrayList<Constant> vals)
	{
		ConstantArrayKey key = new ConstantArrayKey(ty, vals);
		ConstantArray arr = arrayConstants.get(key);
		if (key != null)
			return arr;
		arr = new ConstantArray(ty, vals);
		arrayConstants.put(key, arr);
		return arr;
	}

	@Override
	public boolean isNullValue()
	{
		return false;
	}

	@Override
	public ArrayType getType() {return (ArrayType)super.getType();}

	/**
	 * This method returns true if the array is an array of Int8Ty and
	 * if the elements of the array are all ConstantInt's.
	 * @return
	 */
	public boolean isString()
	{
		if (getType().getElementType() != Type.Int8Ty)
			return false;

		for (int i = 0, e = getNumOfOperands(); i < e; i++)
			if (!(operand(i) instanceof ConstantInt))
				return false;

		return true;
	}
	@Override
	public Constant operand(int idx)
	{
		return (Constant) super.operand(idx);
	}

	public void setOperand(int idx, Constant c)
	{
		super.setOperand(idx, c, this);
	}
}
