package comp; 

/**
 * @author JianpingZeng
 * @version 1.0
 */
public interface OpCodes
{
	int illegal = -1;
	
	int nop = 0;
	
	 /**
     * instruction codes.
     */
     int iadd = 96;

    /**
     * instruction codes.
     */
    int ladd = 97;

    /**
     * instruction codes.
     */
    int fadd = 98;

    /**
     * instruction codes.
     */
    int dadd = 99;

    /**
     * instruction codes.
     */
    int isub = 100;

    /**
     * instruction codes.
     */
    int lsub = 101;

    /**
     * instruction codes.
     */
    int fsub = 102;

    /**
     * instruction codes.
     */
    int dsub = 103;

    /**
     * instruction codes.
     */
    int imul = 104;

    /**
     * instruction codes.
     */
    int lmul = 105;

    /**
     * instruction codes.
     */
    int fmul = 106;

    /**
     * instruction codes.
     */
    int dmul = 107;

    /**
     * instruction codes.
     */
    int idiv = 108;

    /**
     * instruction codes.
     */
    int ldiv = 109;

    /**
     * instruction codes.
     */
    int fdiv = 110;

    /**
     * instruction codes.
     */
    int ddiv = 111;

    /**
     * instruction codes.
     */
    int imod = 112;

    /**
     * instruction codes.
     */
    int lmod = 113;

    /**
     * instruction codes.
     */
    int fmod = 114;

    /**
     * instruction codes.
     */
    int dmod = 115;

    /**
     * instruction codes.
     */
    int ineg = 116;

    /**
     * instruction codes.
     */
    int lneg = 117;

    /**
     * instruction codes.
     */
    int fneg = 118;

    /**
     * instruction codes.
     */
    int dneg = 119;

    /**
     * instruction codes.
     */
    int ishl = 120;

    /**
     * instruction codes.
     */
    int lshl = 121;

    /**
     * instruction codes.
     */
    int ishr = 122;

    /**
     * instruction codes.
     */
    int lshr = 123;

    /**
     * instruction codes.
     */
    int iushr = 124;

    /**
     * instruction codes.
     */
    int lushr = 125;

    /**
     * instruction codes.
     */
    int iand = 126;

    /**
     * instruction codes.
     */
    int land = 127;

    /**
     * instruction codes.
     */
    int ior = 128;

    /**
     * instruction codes.
     */
    int lor = 129;

    /**
     * instruction codes.
     */
    int ixor = 130;

    /**
     * instruction codes.
     */
    int lxor = 131;

    /**
     * instruction codes.
     */
    int iinc = 132;

    /**
     * instruction codes.
     */
    int i2l = 133;

    /**
     * instruction codes.
     */
    int i2f = 134;

    /**
     * instruction codes.
     */
    int i2d = 135;

    /**
     * instruction codes.
     */
    int l2i = 136;

    /**
     * instruction codes.
     */
    int l2f = 137;

    /**
     * instruction codes.
     */
    int l2d = 138;

    /**
     * instruction codes.
     */
    int f2i = 139;

    /**
     * instruction codes.
     */
    int f2l = 140;

    /**
     * instruction codes.
     */
    int f2d = 141;

    /**
     * instruction codes.
     */
    int d2i = 142;

    /**
     * instruction codes.
     */
    int d2l = 143;

    /**
     * instruction codes.
     */
    int d2f = 144;

    /**
     * instruction codes.
     */
    int int2byte = 145;

    /**
     * instruction codes.
     */
    int int2char = 146;

    /**
     * instruction codes.
     */
    int int2short = 147;

    /**
     * instruction codes.
     */
    int lcmp = 148;

    /**
     * instruction codes.
     */
    int fcmpl = 149;

    /**
     * instruction codes.
     */
    int fcmpg = 150;

    /**
     * instruction codes.
     */
    int dcmpl = 151;

    /**
     * instruction codes.
     */
    int dcmpg = 152;

    /**
     * instruction codes.
     */
    int ifeq = 153;

    /**
     * instruction codes.
     */
    int ifne = 154;

    /**
     * instruction codes.
     */
    int iflt = 155;

    /**
     * instruction codes.
     */
    int ifge = 156;

    /**
     * instruction codes.
     */
    int ifgt = 157;

    /**
     * instruction codes.
     */
    int ifle = 158;

    /**
     * instruction codes.
     */
    int if_icmpeq = 159;

    /**
     * instruction codes.
     */
    int if_icmpne = 160;

    /**
     * instruction codes.
     */
    int if_icmplt = 161;

    /**
     * instruction codes.
     */
    int if_icmpge = 162;

    /**
     * instruction codes.
     */
    int if_icmpgt = 163;

    /**
     * instruction codes.
     */
    int if_icmple = 164;

    /**
     * Virtual instruction codes; used for constant folding.
     */
    int string_add = 256;

    /**
     * Virtual instruction codes; used for constant folding.
     */
    int bool_not = 257;

    /**
     * Virtual instruction codes; used for constant folding.
     */
    int bool_and = 258;

    /**
     * Virtual instruction codes; used for constant folding.
     */
    int bool_or = 259;
	
    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    int ishll = 270;

    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    int lshll = 271;

    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    int ishrl = 272;

    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    int lshrl = 273;

    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    int iushrl = 274;

    /**
     * Virtual opcodes; used for shifts with long shiftcount
     */
    int lushrl = 275;
    
    /**
     * Shift and mask constants for shifting prefix instructions.
     *  a pair of instruction codes such as LCMP ; IFEQ is encoded
     *  in Symtab as (LCMP << preShift) + IFEQ.
     */
    int preShift = 9;
    int preMask = (1 << preShift) - 1;
}
