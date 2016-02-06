package comp; 

/** 
 * @author Jianping Zeng <z1215jping@hotmail.com>
 * @version 2016年1月17日 下午3:08:32 
 */
public interface OpCodes
{
	public static final int illegal = -1;
	
	public static final int nop = 0;		
	
	 /**
     * instruction codes.
     */
    public static final int iadd = 96;

    /**
     * instruction codes.
     */
    public static final int ladd = 97;

    /**
     * instruction codes.
     */
    public static final int fadd = 98;

    /**
     * instruction codes.
     */
    public static final int dadd = 99;

    /**
     * instruction codes.
     */
    public static final int isub = 100;

    /**
     * instruction codes.
     */
    public static final int lsub = 101;

    /**
     * instruction codes.
     */
    public static final int fsub = 102;

    /**
     * instruction codes.
     */
    public static final int dsub = 103;

    /**
     * instruction codes.
     */
    public static final int imul = 104;

    /**
     * instruction codes.
     */
    public static final int lmul = 105;

    /**
     * instruction codes.
     */
    public static final int fmul = 106;

    /**
     * instruction codes.
     */
    public static final int dmul = 107;

    /**
     * instruction codes.
     */
    public static final int idiv = 108;

    /**
     * instruction codes.
     */
    public static final int ldiv = 109;

    /**
     * instruction codes.
     */
    public static final int fdiv = 110;

    /**
     * instruction codes.
     */
    public static final int ddiv = 111;

    /**
     * instruction codes.
     */
    public static final int imod = 112;

    /**
     * instruction codes.
     */
    public static final int lmod = 113;

    /**
     * instruction codes.
     */
    public static final int fmod = 114;

    /**
     * instruction codes.
     */
    public static final int dmod = 115;

    /**
     * instruction codes.
     */
    public static final int ineg = 116;

    /**
     * instruction codes.
     */
    public static final int lneg = 117;

    /**
     * instruction codes.
     */
    public static final int fneg = 118;

    /**
     * instruction codes.
     */
    public static final int dneg = 119;

    /**
     * instruction codes.
     */
    public static final int ishl = 120;

    /**
     * instruction codes.
     */
    public static final int lshl = 121;

    /**
     * instruction codes.
     */
    public static final int ishr = 122;

    /**
     * instruction codes.
     */
    public static final int lshr = 123;

    /**
     * instruction codes.
     */
    public static final int iushr = 124;

    /**
     * instruction codes.
     */
    public static final int lushr = 125;

    /**
     * instruction codes.
     */
    public static final int iand = 126;

    /**
     * instruction codes.
     */
    public static final int land = 127;

    /**
     * instruction codes.
     */
    public static final int ior = 128;

    /**
     * instruction codes.
     */
    public static final int lor = 129;

    /**
     * instruction codes.
     */
    public static final int ixor = 130;

    /**
     * instruction codes.
     */
    public static final int lxor = 131;

    /**
     * instruction codes.
     */
    public static final int iinc = 132;

    /**
     * instruction codes.
     */
    public static final int i2l = 133;

    /**
     * instruction codes.
     */
    public static final int i2f = 134;

    /**
     * instruction codes.
     */
    public static final int i2d = 135;

    /**
     * instruction codes.
     */
    public static final int l2i = 136;

    /**
     * instruction codes.
     */
    public static final int l2f = 137;

    /**
     * instruction codes.
     */
    public static final int l2d = 138;

    /**
     * instruction codes.
     */
    public static final int f2i = 139;

    /**
     * instruction codes.
     */
    public static final int f2l = 140;

    /**
     * instruction codes.
     */
    public static final int f2d = 141;

    /**
     * instruction codes.
     */
    public static final int d2i = 142;

    /**
     * instruction codes.
     */
    public static final int d2l = 143;

    /**
     * instruction codes.
     */
    public static final int d2f = 144;

    /**
     * instruction codes.
     */
    public static final int int2byte = 145;

    /**
     * instruction codes.
     */
    public static final int int2char = 146;

    /**
     * instruction codes.
     */
    public static final int int2short = 147;

    /**
     * instruction codes.
     */
    public static final int lcmp = 148;

    /**
     * instruction codes.
     */
    public static final int fcmpl = 149;

    /**
     * instruction codes.
     */
    public static final int fcmpg = 150;

    /**
     * instruction codes.
     */
    public static final int dcmpl = 151;

    /**
     * instruction codes.
     */
    public static final int dcmpg = 152;

    /**
     * instruction codes.
     */
    public static final int ifeq = 153;

    /**
     * instruction codes.
     */
    public static final int ifne = 154;

    /**
     * instruction codes.
     */
    public static final int iflt = 155;

    /**
     * instruction codes.
     */
    public static final int ifge = 156;

    /**
     * instruction codes.
     */
    public static final int ifgt = 157;

    /**
     * instruction codes.
     */
    public static final int ifle = 158;

    /**
     * instruction codes.
     */
    public static final int if_icmpeq = 159;

    /**
     * instruction codes.
     */
    public static final int if_icmpne = 160;

    /**
     * instruction codes.
     */
    public static final int if_icmplt = 161;

    /**
     * instruction codes.
     */
    public static final int if_icmpge = 162;

    /**
     * instruction codes.
     */
    public static final int if_icmpgt = 163;

    /**
     * instruction codes.
     */
    public static final int if_icmple = 164;

    /**
     * Virtual instruction codes; used for constant folding.
     */
    public static final int string_add = 256;

    /**
     * Virtual instruction codes; used for constant folding.
     */
    public static final int bool_not = 257;

    /**
     * Virtual instruction codes; used for constant folding.
     */
    public static final int bool_and = 258;

    /**
     * Virtual instruction codes; used for constant folding.
     */
    public static final int bool_or = 259;
	
    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    public static final int ishll = 270;

    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    public static final int lshll = 271;

    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    public static final int ishrl = 272;

    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    public static final int lshrl = 273;

    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    public static final int iushrl = 274;

    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    public static final int lushrl = 275;
    
    /**
     * Shift and mask constants for shifting prefix instructions.
     *  a pair of instruction codes such as LCMP ; IFEQ is encoded
     *  in Symtab as (LCMP << preShift) + IFEQ.
     */
    public static final int preShift = 9;
    public static final int preMask = (1 << preShift) - 1;
}
