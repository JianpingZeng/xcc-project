package backend.target;

import backend.pass.*;
import backend.type.ArrayType;
import backend.type.IntegerType;
import backend.type.StructType;
import backend.type.Type;
import tools.Pair;
import tools.Util;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class TargetData implements ImmutablePass
{
    /**
     * Enum used to categorise the alignment types stored by TargetAlignElem.
     */
    public enum AlignTypeEnum
    {
        INTEGER_ALIGN('i'),
        VECTOR_ALIGN('v'),
        FLOAT_ALIGN('f'),
        AGGREGATE_TYPE('a'),
        STACK_OBJECT('s');

        public char name;
        AlignTypeEnum(char name)
        {
            this.name = name;
        }
    }

    public static class TargetAlignElem
    {
        public AlignTypeEnum alignType;
        public byte abiAlign;
        public byte prefAlign;
        public int typeBitWidth;

        public static TargetAlignElem get(
                AlignTypeEnum alignType,
                byte abiAlign,
                byte prefAlign,
                int typeBitWidth)
        {
            assert abiAlign <= prefAlign :"Preferred alignment worse than ABI";
            TargetAlignElem elem = new TargetAlignElem();
            elem.alignType = alignType;
            elem.prefAlign = prefAlign;
            elem.typeBitWidth = typeBitWidth;
            return elem;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null) return false;
            if (this == obj) return true;

            if (getClass() != obj.getClass()) return false;

            TargetAlignElem elem = (TargetAlignElem)obj;
            return alignType == elem.alignType && abiAlign == elem.abiAlign
                    && prefAlign == elem.prefAlign && typeBitWidth == elem.typeBitWidth;
        }

        public PrintStream dump(PrintStream os)
        {
            os.printf("%s%d:%d:%d", alignType.name, typeBitWidth, abiAlign * 8, prefAlign * 8);
            return os;
        }
    }
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
	private int pointerMemSize;

	private int pointerABIAlign;
	/**
	 * default to 8.
	 */
	private int pointerPrefAlign;

	/**
	 * default to "".
	 */
	private String targetName;

	private ArrayList<TargetAlignElem> alignments = new ArrayList<>();

	public TargetData()
	{
		this.targetName = "";
		littleEndian = false;
		pointerMemSize = 8;
		pointerPrefAlign = 8;
		doubleAlignment = 8;
		floatAlignment = 4;
		longAlignment = 8;
		intAlignment = 4;
		shortAlignment = 2;
		byteAlignment = 1;
	}

	public TargetData(String targetDescription)
	{
		init(targetDescription);
	}
	/**
	A TargetDescription string consists of a sequence of hyphen-delimited
	specifiers for target endianness, pointer size and alignments, and various
	primitive type sizes and alignments. A typical string looks something like:
	<br><br>
	"E-p:32:32:32-i1:8:8-i8:8:8-i32:32:32-i64:32:64-f32:32:32-f64:32:64"
	<br><br>
	(note: this string is not fully specified and is only an example.)
	<p>
	Alignments come in two flavors: ABI and preferred. ABI alignment (abi_align,
	below) dictates how a type will be aligned within an aggregate and when used
	as an argument.  Preferred alignment (pref_align, below) determines a type's
	alignment when emitted as a global.
	</p>
	<p>
	Specifier string details:
	<br><br>
	<i>[E|e]</i>: Endianness. "E" specifies a big-endian target data model, "e"
	specifies a little-endian target data model.
	<br><br>
	<i>p:@verbatim<size>:<abi_align>:<pref_align>@endverbatim</i>: Pointer size,
	ABI and preferred alignment.
	<br><br>
	<i>@verbatim<type><size>:<abi_align>:<pref_align>@endverbatim</i>: Numeric type
	alignment. Type is
	one of <i>i|f|v|a</i>, corresponding to integer, floating point, vector, or
	aggregate.  Size indicates the size, e.g., 32 or 64 bits.
	</p>
	The default string, fully specified, is:
	<br><br>
	"E-p:64:64:64-a0:0:8-f32:32:32-f64:64:64"
	"-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64"
	"-v64:64:64-v128:128:128"
	<br><br>
	Note that in the case of aggregates, 0 is the default ABI and preferred
	alignment. This is a special case, where the aggregate's computed worst-case
	alignment will be used.
	*/
	private void init(String targetDescription) {
        StringBuilder temp = new StringBuilder(targetDescription);

        littleEndian = false;
        pointerMemSize = 8;
        pointerABIAlign = pointerPrefAlign = 8;

        // Default alignments.
        setAlignment(AlignTypeEnum.INTEGER_ALIGN, (byte) 1, (byte) 1, (byte) 1); // i1
        setAlignment(AlignTypeEnum.INTEGER_ALIGN, (byte) 1, (byte) 1, (byte) 8); // i8
        setAlignment(AlignTypeEnum.INTEGER_ALIGN, (byte) 2, (byte) 2, (byte) 16); // i16
        setAlignment(AlignTypeEnum.INTEGER_ALIGN, (byte) 4, (byte) 4, (byte) 32); // i32
        setAlignment(AlignTypeEnum.INTEGER_ALIGN, (byte) 8, (byte) 8, (byte) 64); // i64
        setAlignment(AlignTypeEnum.FLOAT_ALIGN, (byte) 4, (byte) 4, (byte) 32); // f32
        setAlignment(AlignTypeEnum.FLOAT_ALIGN, (byte) 8, (byte) 1, (byte) 64); // f64
        setAlignment(AlignTypeEnum.VECTOR_ALIGN, (byte) 8, (byte) 8, (byte) 64); // v2i32, v1i64
        setAlignment(AlignTypeEnum.VECTOR_ALIGN, (byte) 16, (byte) 16, (byte) 128); // v16i8, v8i16, v4i32,...
        setAlignment(AlignTypeEnum.AGGREGATE_TYPE, (byte) 0, (byte) 8, (byte) 0); // struct

        while (temp.length() != 0) {

            String token = getToken(temp, "-");
            String arg0 = getToken(temp, ":");
            int i = 0;
            switch (arg0.charAt(i)) {
                case 'E':
                    littleEndian = false;
                    break;
                case 'e':
                    littleEndian = true;
                    break;
                case 'p':
                    pointerMemSize = Integer.parseInt(getToken(temp, ":")) / 8;
                    pointerABIAlign = Integer.parseInt(getToken(temp, ":")) / 8;
                    pointerPrefAlign = Integer.parseInt(getToken(temp, ":")) / 8;

                    if (pointerPrefAlign == 0)
                        pointerPrefAlign = pointerABIAlign;
                    break;
                case 'i':
                case 'v':
                case 'f':
                case 's':
                case 'a': {
                    AlignTypeEnum alignType = AlignTypeEnum.STACK_OBJECT;
                    switch (arg0.charAt(i)) {
                        case 'i':
                            alignType = AlignTypeEnum.INTEGER_ALIGN;
                            break;
                        case 'v':
                            alignType = AlignTypeEnum.VECTOR_ALIGN;
                            break;
                        case 'f':
                            alignType = AlignTypeEnum.FLOAT_ALIGN;
                            break;
                        case 's':
                            alignType = AlignTypeEnum.STACK_OBJECT;
                            break;
                        case 'a':
                            alignType = AlignTypeEnum.AGGREGATE_TYPE;
                            break;
                    }
                    int size = Integer.parseInt(arg0.substring(1));
                    byte abiAlign = (byte) (Byte.parseByte(getToken(temp, ":")) / (byte) 8);
                    byte prefAlign = (byte) (Integer.parseInt(getToken(temp, ":")) / 8);
                    if (prefAlign == 0)
                        prefAlign = abiAlign;
                    setAlignment(alignType, abiAlign, prefAlign, size);
                    break;
                }
                default:
                    break;
            }
        }
    }

	private String getToken(StringBuilder str, String delimiters)
    {
        int start = 0;
        for (; start < str.length(); )
            if (str.substring(start).startsWith(delimiters))
                start += delimiters.length();
        int end = 0;
        end = str.indexOf(delimiters, start);
        String result = str.substring(start, end);
        str.delete(start, end);
        return result;
    }

	private void setAlignment(AlignTypeEnum alignType, byte abiAlign, byte prefAlign, int bitWidth)
    {
        assert abiAlign <= prefAlign :"Preferred alignment worse than abi alignemnt.";
        for (int i = 0, e = alignments.size(); i < e; ++i)
        {
            if (alignments.get(i).alignType == alignType
                    && alignments.get(i).typeBitWidth == bitWidth)
            {
                // Update the abi, prefered alignments.
                alignments.get(i).abiAlign = abiAlign;
                alignments.get(i).prefAlign = prefAlign;
                return;
            }
        }
    }

	@Override
	public void initializePass()
	{

	}

	public TargetData(String targetName, boolean isLittleEndian, int ptrSize,
			int ptrAlign, int doubleAlign, int floatAlign, int longAlign, int intAlign,
			int shortAlign, int byteAlign)
	{
		this.targetName = targetName;
		littleEndian = isLittleEndian;
		pointerMemSize = ptrSize;
		pointerPrefAlign = ptrAlign;
		doubleAlignment = doubleAlign;
		assert pointerPrefAlign == doubleAlignment;
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
		pointerMemSize = td.pointerMemSize;
		pointerPrefAlign = td.pointerPrefAlign;
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

	public int getPointerSizeInBits() {return pointerMemSize *8;}

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
	 * Obtains the data getNumOfSubLoop and alignment for specified type on targeted machine.
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
				res = new Pair<>((long)td.getPointerMemSize(),td.getPointerPrefAlign());
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

	public int getPointerMemSize() {return pointerMemSize;}

	public int getPointerPrefAlign() {return pointerPrefAlign;}

	public String getTargetName(){return targetName;}

	public StructLayout getStructLayout(StructType ty)
	{
		return new StructLayout(ty, this);
	}

	@Override
	public String getPassName(){return "Target Data pass!";}

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
