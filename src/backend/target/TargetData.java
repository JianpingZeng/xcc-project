package backend.target;

import backend.pass.*;
import backend.type.ArrayType;
import backend.type.IntegerType;
import backend.type.StructType;
import backend.type.Type;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

/**
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class TargetData extends ImmutablePass
{
	/**
	 * Register the default TargetData pass.
	 */
	public static RegisterPass targetDataRegPass =
			new RegisterPass("Target Data Layout", TargetData.class);
	/**
	 * default to false.
	 */
	private boolean littleEndian;
	/**
	 * default to 1.
	 */
	private int byteAlignment;
	/**
	 * default to 2.
	 */
	private int shortAlignment;
	/**
	 * default to 4.
	 */
	private int intAlignment;
	/**
	 * default to 8.
	 */
	private int longAlignment;
	/**
	 * default to 4.
	 */
	private int floatAlignment;
	/**
	 * default to 8.
	 */
	private int doubleAlignment;
	/**
	 * Pointer size in bytes by default to 8.
	 */
	private int pointerSize;
	/**
	 * default to 8.
	 */
	private int pointerAlignment;
	/**
	 * default to "".
	 */
	private String targetName;

	public TargetData()
	{
		this.targetName = "";
		littleEndian = false;
		pointerSize = 8;
		pointerAlignment = 8;
		doubleAlignment = 8;
		floatAlignment = 4;
		longAlignment = 8;
		intAlignment = 4;
		shortAlignment = 2;
		byteAlignment = 1;
	}

	@Override public void initializePass()
	{

	}

	public TargetData(String targetName, boolean isLittleEndian, int ptrSize,
			int ptrAlign, int doubleAlign, int floatAlign, int longAlign, int intAlign,
			int shortAlign, int byteAlign)
	{
		this.targetName = targetName;
		littleEndian = isLittleEndian;
		pointerSize = ptrSize;
		pointerAlignment = ptrAlign;
		doubleAlignment = doubleAlign;
		assert pointerAlignment == doubleAlignment;
		floatAlignment = floatAlign;
		longAlignment = longAlign;
		intAlignment = intAlign;
		shortAlignment = shortAlign;
		byteAlignment = byteAlign;
	}

	public TargetData(TargetData td)
	{
		targetName = td.getTargetName();
		littleEndian = td.isLittleEndian();
		pointerSize = td.pointerSize;
		pointerAlignment = td.pointerAlignment;
		doubleAlignment = td.doubleAlignment;
		floatAlignment = td.floatAlignment;
		longAlignment = td.longAlignment;
		intAlignment = td.intAlignment;
		shortAlignment = td.shortAlignment;
		byteAlignment = td.byteAlignment;
	}

	public IntegerType getIntPtrType()
	{
		return IntegerType.get(getPointerSizeInBits());
	}

	public int getPointerSizeInBits() {return pointerSize *8;}

	public long getTypeSizeInBits(Type type)
	{
		return getTypeSize(type)*8;
	}

	public long getTypeSize(Type type)
	{
		return getTypeInfo(type, this).first;
	}

	public int getTypeAlign(Type type)
	{
		return getTypeInfo(type, this).second;
	}

	/**
	 * Obtains the data size and alignment for specified type on targeted machine.
	 * @param type
	 * @param td
	 * @return
	 */
	public static Pair<Long, Integer> getTypeInfo(Type type, TargetData td)
	{
		assert type.isSized() :"Can not getTypeInfo for unsized type";
		Pair<Long, Integer> res = null;
		switch (type.getPrimitiveID())
		{
			case Type.VoidTyID:
			case Type.Int1TyID:
			case Type.Int8TyID:
				res = new Pair<>(1L, td.getByteAlignment());
				break;
			case Type.Int16TyID:
				res = new Pair<>(2L, td.getShortAlignment());
				break;
			case Type.Int32TyID:
				res = new Pair<>(4L, td.getIntAlignment());
				break;
			case Type.Int64TyID:
				res = new Pair<>(8L, td.getLongAlignment());
				break;
			case Type.FloatTyID:
				res = new Pair<>(4L, td.getFloatAlignment());
				break;
			case Type.DoubleTyID:
				res = new Pair<>(8L, td.getDoubleAlignment());
				break;
			case Type.LabelTyID:
			case Type.PointerTyID:
				res = new Pair<>((long)td.getPointerSize(),td.getPointerAlignment());
				break;
			case Type.ArrayTyID:
			{
				final ArrayType aty = (ArrayType)type;
				Pair<Long, Integer> eltInfo = getTypeInfo(aty.getElemType(), td);
				res = new Pair<>(eltInfo.first*aty.getNumElements(), eltInfo.second);
				break;
			}
			case Type.StructTyID:
			{
				// Get the struct layout annotation, which is createed lazily on demand.
				final StructLayout layout = td.getStructLayout((StructType)type);
				res = new Pair<>(layout.structSize, layout.structAlignment);
				break;
			}

			case Type.TypeTyID:
			default:
			{
				assert false:"Bad type for getTypeInfo!";
				break;
			}
		}
		return res;
	}

	public boolean isLittleEndian(){return littleEndian;}

	public int getByteAlignment(){return byteAlignment;}

	public int getShortAlignment(){return shortAlignment;}

	public int getIntAlignment(){return intAlignment;}

	public int getLongAlignment(){return longAlignment;}

	public int getFloatAlignment(){return floatAlignment;}

	public int getDoubleAlignment() {return doubleAlignment;}

	public int getPointerSize() {return pointerSize;}

	public int getPointerAlignment() {return pointerAlignment;}

	public String getTargetName(){return targetName;}

	public StructLayout getStructLayout(StructType ty)
	{
		return new StructLayout(ty, this);
	}

	@Override
	public String getPassName(){return "Target Data pass!";}

	@Override public Pass getAnalysisToUpDate(PassInfo pi)
	{
		return null;
	}

	@Override
	public void addToPassManager(ModulePassManager pm, AnalysisUsage au)
	{

	}

	@Override
	public void addToPassManager(FunctionPassManager pm, AnalysisUsage au)
	{

	}

	/**
	 * This class is used to lazily compute structure layout information for
	 * a backend.target machine, based on this TargetData structure.
	 */
	public class StructLayout
	{
		public ArrayList<Long> memberOffsets;
		public long structSize;
		int structAlignment;
		private StructLayout(StructType st, TargetData td)
		{
			structAlignment = 0;
			structSize = 0;

			// Loop over each element in struct type, placing them in memory.
			for (Type eltTy : st.getElementTypes())
			{
				Pair<Long, Integer> typeInfo = getTypeInfo(eltTy, td);
				long tySize = typeInfo.first;
				int tyAlign = typeInfo.second;

				// Add padding if necessary to make the data alignment properly.
				if (structSize % tyAlign != 0)
				{
					// add padding.
					structSize = Util.roundUp(structSize, tyAlign);
				}

				// Keep track of maximum alignment constraints.
				structAlignment = Math.max(tyAlign, structAlignment);

				memberOffsets.add(structSize);
				structSize += tySize;
			}

			// Empty structure has one alignment
			if (structAlignment == 0) structAlignment = 1;

			// Add padding to the end of struct so that it could be put in an array
			// and all array element all be aligned correctly.
			if (structSize % structAlignment != 0)
				structSize = Util.roundUp(structSize, structAlignment);
		}
	}
}
