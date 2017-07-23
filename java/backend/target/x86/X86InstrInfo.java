package backend.target.x86;

import backend.codegen.*;
import backend.target.TargetInstrDesc;
import backend.target.TargetInstrInfoImpl;
import backend.target.TargetRegisterInfo;
import backend.value.GlobalVariable;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import tools.OutParamWrapper;
import tools.Pair;
import tools.commandline.BooleanOpt;
import tools.commandline.OptionHiddenApplicator;
import tools.commandline.OptionNameApplicator;

import static backend.codegen.MachineInstrBuilder.buildMI;
import static backend.target.x86.X86GenInstrNames.*;
import static backend.target.x86.X86GenRegisterNames.EFLAGS;
import static backend.target.x86.X86GenRegisterNames.RIP;
import static tools.commandline.Desc.desc;
import static tools.commandline.Initializer.init;
import static tools.commandline.OptionHidden.Hidden;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86InstrInfo extends TargetInstrInfoImpl
{
    public static BooleanOpt NoFusing = new BooleanOpt(
            new OptionNameApplicator("disable-spill-fusing"),
            desc("Disable fusing of spill code into instructions"));

    public static BooleanOpt PrintFailedFusing = new BooleanOpt(
            new OptionNameApplicator("print-failed-fuse-candidates"),
            desc("Print instructions that the allocator wants to"
                    + " fuse, but the X86 backend currently can't"),
            new OptionHiddenApplicator(Hidden));

    public static BooleanOpt ReMatPICStubLoad = new BooleanOpt(
            new OptionNameApplicator("remat-pic-stub-load"),
            desc("Re-materialize load from stub in PIC mode"), init(false),
            new OptionHiddenApplicator(Hidden));

    private X86TargetMachine tm;
    private X86RegisterInfo registerInfo;

    /**
     * RegOp2MemOpTable2Addr, RegOp2MemOpTable0, RegOp2MemOpTable1,
     * RegOp2MemOpTable2 - Load / store folding opcode maps.
     */
    private TIntObjectHashMap<Pair<Integer, Integer>> regOp2MemOpTable2Addr;
    private TIntObjectHashMap<Pair<Integer, Integer>> regOp2MemOpTable0;
    private TIntObjectHashMap<Pair<Integer, Integer>> regOp2MemOpTable1;
    private TIntObjectHashMap<Pair<Integer, Integer>> regOp2MemOpTable2;

    /**
     * MemOp2RegOpTable - Load / store unfolding opcode map.
     */
    private TIntObjectHashMap<Pair<Integer, Integer>> memOp2RegOpTable;

    private static final int[][] opTbl2Addr = { { ADC32ri, ADC32mi },
            { ADC32ri8, ADC32mi8 }, { ADC32rr, ADC32mr }, { ADC64ri32, ADC64mi32 },
            { ADC64ri8, ADC64mi8 }, { ADC64rr, ADC64mr }, { ADD16ri, ADD16mi },
            { ADD16ri8, ADD16mi8 }, { ADD16rr, ADD16mr }, { ADD32ri, ADD32mi },
            { ADD32ri8, ADD32mi8 }, { ADD32rr, ADD32mr }, { ADD64ri32, ADD64mi32 },
            { ADD64ri8, ADD64mi8 }, { ADD64rr, ADD64mr }, { ADD8ri, ADD8mi },
            { ADD8rr, ADD8mr }, { AND16ri, AND16mi }, { AND16ri8, AND16mi8 },
            { AND16rr, AND16mr }, { AND32ri, AND32mi }, { AND32ri8, AND32mi8 },
            { AND32rr, AND32mr }, { AND64ri32, AND64mi32 }, { AND64ri8, AND64mi8 },
            { AND64rr, AND64mr }, { AND8ri, AND8mi }, { AND8rr, AND8mr },
            { DEC16r, DEC16m }, { DEC32r, DEC32m }, { DEC64_16r, DEC64_16m },
            { DEC64_32r, DEC64_32m }, { DEC64r, DEC64m }, { DEC8r, DEC8m },
            { INC16r, INC16m }, { INC32r, INC32m }, { INC64_16r, INC64_16m },
            { INC64_32r, INC64_32m }, { INC64r, INC64m }, { INC8r, INC8m },
            { NEG16r, NEG16m }, { NEG32r, NEG32m }, { NEG64r, NEG64m },
            { NEG8r, NEG8m }, { NOT16r, NOT16m }, { NOT32r, NOT32m },
            { NOT64r, NOT64m }, { NOT8r, NOT8m }, { OR16ri, OR16mi },
            { OR16ri8, OR16mi8 }, { OR16rr, OR16mr }, { OR32ri, OR32mi },
            { OR32ri8, OR32mi8 }, { OR32rr, OR32mr }, { OR64ri32, OR64mi32 },
            { OR64ri8, OR64mi8 }, { OR64rr, OR64mr }, { OR8ri, OR8mi },
            { OR8rr, OR8mr }, { ROL16r1, ROL16m1 }, { ROL16rCL, ROL16mCL },
            { ROL16ri, ROL16mi }, { ROL32r1, ROL32m1 }, { ROL32rCL, ROL32mCL },
            { ROL32ri, ROL32mi }, { ROL64r1, ROL64m1 }, { ROL64rCL, ROL64mCL },
            { ROL64ri, ROL64mi }, { ROL8r1, ROL8m1 }, { ROL8rCL, ROL8mCL },
            { ROL8ri, ROL8mi }, { ROR16r1, ROR16m1 }, { ROR16rCL, ROR16mCL },
            { ROR16ri, ROR16mi }, { ROR32r1, ROR32m1 }, { ROR32rCL, ROR32mCL },
            { ROR32ri, ROR32mi }, { ROR64r1, ROR64m1 }, { ROR64rCL, ROR64mCL },
            { ROR64ri, ROR64mi }, { ROR8r1, ROR8m1 }, { ROR8rCL, ROR8mCL },
            { ROR8ri, ROR8mi }, { SAR16r1, SAR16m1 }, { SAR16rCL, SAR16mCL },
            { SAR16ri, SAR16mi }, { SAR32r1, SAR32m1 }, { SAR32rCL, SAR32mCL },
            { SAR32ri, SAR32mi }, { SAR64r1, SAR64m1 }, { SAR64rCL, SAR64mCL },
            { SAR64ri, SAR64mi }, { SAR8r1, SAR8m1 }, { SAR8rCL, SAR8mCL },
            { SAR8ri, SAR8mi }, { SBB32ri, SBB32mi }, { SBB32ri8, SBB32mi8 },
            { SBB32rr, SBB32mr }, { SBB64ri32, SBB64mi32 }, { SBB64ri8, SBB64mi8 },
            { SBB64rr, SBB64mr }, { SHL16rCL, SHL16mCL }, { SHL16ri, SHL16mi },
            { SHL32rCL, SHL32mCL }, { SHL32ri, SHL32mi }, { SHL64rCL, SHL64mCL },
            { SHL64ri, SHL64mi }, { SHL8rCL, SHL8mCL }, { SHL8ri, SHL8mi },
            { SHLD16rrCL, SHLD16mrCL }, { SHLD16rri8, SHLD16mri8 },
            { SHLD32rrCL, SHLD32mrCL }, { SHLD32rri8, SHLD32mri8 },
            { SHLD64rrCL, SHLD64mrCL }, { SHLD64rri8, SHLD64mri8 },
            { SHR16r1, SHR16m1 }, { SHR16rCL, SHR16mCL }, { SHR16ri, SHR16mi },
            { SHR32r1, SHR32m1 }, { SHR32rCL, SHR32mCL }, { SHR32ri, SHR32mi },
            { SHR64r1, SHR64m1 }, { SHR64rCL, SHR64mCL }, { SHR64ri, SHR64mi },
            { SHR8r1, SHR8m1 }, { SHR8rCL, SHR8mCL }, { SHR8ri, SHR8mi },
            { SHRD16rrCL, SHRD16mrCL }, { SHRD16rri8, SHRD16mri8 },
            { SHRD32rrCL, SHRD32mrCL }, { SHRD32rri8, SHRD32mri8 },
            { SHRD64rrCL, SHRD64mrCL }, { SHRD64rri8, SHRD64mri8 },
            { SUB16ri, SUB16mi }, { SUB16ri8, SUB16mi8 }, { SUB16rr, SUB16mr },
            { SUB32ri, SUB32mi }, { SUB32ri8, SUB32mi8 }, { SUB32rr, SUB32mr },
            { SUB64ri32, SUB64mi32 }, { SUB64ri8, SUB64mi8 }, { SUB64rr, SUB64mr },
            { SUB8ri, SUB8mi }, { SUB8rr, SUB8mr }, { XOR16ri, XOR16mi },
            { XOR16ri8, XOR16mi8 }, { XOR16rr, XOR16mr }, { XOR32ri, XOR32mi },
            { XOR32ri8, XOR32mi8 }, { XOR32rr, XOR32mr }, { XOR64ri32, XOR64mi32 },
            { XOR64ri8, XOR64mi8 }, { XOR64rr, XOR64mr }, { XOR8ri, XOR8mi },
            { XOR8rr, XOR8mr } };

    public X86InstrInfo(X86TargetMachine tm)
    {
        super(X86GenInstrInfo.X86Insts);
        TIntArrayList ambEntries = new TIntArrayList();
        registerInfo = null;
        regOp2MemOpTable2Addr = new TIntObjectHashMap<>();
        regOp2MemOpTable0 = new TIntObjectHashMap<>();
        regOp2MemOpTable1 = new TIntObjectHashMap<>();
        regOp2MemOpTable2 = new TIntObjectHashMap<>();
        memOp2RegOpTable = new TIntObjectHashMap<>();

        for (int i = 0, e = opTbl2Addr.length; i != e; i++)
        {
            int regOp = opTbl2Addr[i][0];
            int memOp = opTbl2Addr[i][1];

            if (regOp2MemOpTable2Addr.put(regOp, Pair.get(memOp, 0)) != null)
                assert false : "Dupliate entries?";

            int auxInfo = 0 | (1 << 4) | (1 << 5);
            if (memOp2RegOpTable.put(memOp, Pair.get(memOp, auxInfo)) != null)
                ambEntries.add(memOp);
        }

        // If the third value is 1, then it's folding either a load or a store.
        int[][] opTbl0 = { { BT16ri8, BT16mi8, 1, 0 }, { BT32ri8, BT32mi8, 1, 0 },
                { BT64ri8, BT64mi8, 1, 0 }, { CALL32r, CALL32m, 1, 0 },
                { CALL64r, CALL64m, 1, 0 }, { CMP16ri, CMP16mi, 1, 0 },
                { CMP16ri8, CMP16mi8, 1, 0 }, { CMP16rr, CMP16mr, 1, 0 },
                { CMP32ri, CMP32mi, 1, 0 }, { CMP32ri8, CMP32mi8, 1, 0 },
                { CMP32rr, CMP32mr, 1, 0 }, { CMP64ri32, CMP64mi32, 1, 0 },
                { CMP64ri8, CMP64mi8, 1, 0 }, { CMP64rr, CMP64mr, 1, 0 },
                { CMP8ri, CMP8mi, 1, 0 }, { CMP8rr, CMP8mr, 1, 0 },
                { DIV16r, DIV16m, 1, 0 }, { DIV32r, DIV32m, 1, 0 },
                { DIV64r, DIV64m, 1, 0 }, { DIV8r, DIV8m, 1, 0 },
                { EXTRACTPSrr, EXTRACTPSmr, 0, 16 }, { FsMOVAPDrr, MOVSDmr, 0, 0 },
                { FsMOVAPSrr, MOVSSmr, 0, 0 }, { IDIV16r, IDIV16m, 1, 0 },
                { IDIV32r, IDIV32m, 1, 0 }, { IDIV64r, IDIV64m, 1, 0 },
                { IDIV8r, IDIV8m, 1, 0 }, { IMUL16r, IMUL16m, 1, 0 },
                { IMUL32r, IMUL32m, 1, 0 }, { IMUL64r, IMUL64m, 1, 0 },
                { IMUL8r, IMUL8m, 1, 0 }, { JMP32r, JMP32m, 1, 0 },
                { JMP64r, JMP64m, 1, 0 }, { MOV16ri, MOV16mi, 0, 0 },
                { MOV16rr, MOV16mr, 0, 0 }, { MOV32ri, MOV32mi, 0, 0 },
                { MOV32rr, MOV32mr, 0, 0 }, { MOV64ri32, MOV64mi32, 0, 0 },
                { MOV64rr, MOV64mr, 0, 0 }, { MOV8ri, MOV8mi, 0, 0 },
                { MOV8rr, MOV8mr, 0, 0 }, { MOV8rr_NOREX, MOV8mr_NOREX, 0, 0 },
                { MOVAPDrr, MOVAPDmr, 0, 16 }, { MOVAPSrr, MOVAPSmr, 0, 16 },
                { MOVDQArr, MOVDQAmr, 0, 16 }, { MOVPDI2DIrr, MOVPDI2DImr, 0, 0 },
                { MOVPQIto64rr, MOVPQI2QImr, 0, 0 },
                { MOVPS2SSrr, MOVPS2SSmr, 0, 0 }, { MOVSDrr, MOVSDmr, 0, 0 },
                { MOVSDto64rr, MOVSDto64mr, 0, 0 },
                { MOVSS2DIrr, MOVSS2DImr, 0, 0 }, { MOVSSrr, MOVSSmr, 0, 0 },
                { MOVUPDrr, MOVUPDmr, 0, 0 }, { MOVUPSrr, MOVUPSmr, 0, 0 },
                { MUL16r, MUL16m, 1, 0 }, { MUL32r, MUL32m, 1, 0 },
                { MUL64r, MUL64m, 1, 0 }, { MUL8r, MUL8m, 1, 0 },
                { SETAEr, SETAEm, 0, 0 }, { SETAr, SETAm, 0, 0 },
                { SETBEr, SETBEm, 0, 0 }, { SETBr, SETBm, 0, 0 },
                { SETEr, SETEm, 0, 0 }, { SETGEr, SETGEm, 0, 0 },
                { SETGr, SETGm, 0, 0 }, { SETLEr, SETLEm, 0, 0 },
                { SETLr, SETLm, 0, 0 }, { SETNEr, SETNEm, 0, 0 },
                { SETNOr, SETNOm, 0, 0 }, { SETNPr, SETNPm, 0, 0 },
                { SETNSr, SETNSm, 0, 0 }, { SETOr, SETOm, 0, 0 },
                { SETPr, SETPm, 0, 0 }, { SETSr, SETSm, 0, 0 },
                { TAILJMPr, TAILJMPm, 1, 0 }, { TEST16ri, TEST16mi, 1, 0 },
                { TEST32ri, TEST32mi, 1, 0 }, { TEST64ri32, TEST64mi32, 1, 0 },
                { TEST8ri, TEST8mi, 1, 0 } };

        for (int i = 0, e = opTbl0.length; i != e; i++)
        {
            int regOp = opTbl0[i][0];
            int memOp = opTbl0[i][1];
            int align = opTbl0[i][3];

            if (regOp2MemOpTable0.put(regOp, Pair.get(memOp, align)) != null)
                assert false : "Duplicated entries?";

            int foldedLoad = opTbl0[i][2];
            int auxInfo = 0 | (foldedLoad << 4) | ((foldedLoad ^ 1) << 5);
            if (regOp != X86GenInstrNames.FsMOVAPDrr && regOp != X86GenInstrNames.FsMOVAPSrr)
            {
                if (memOp2RegOpTable.put(memOp, Pair.get(regOp, auxInfo)) != null)
                    ambEntries.add(memOp);
            }
        }

        int[][] opTbl1 = { { CMP16rr, CMP16rm, 0 }, { CMP32rr, CMP32rm, 0 },
                { CMP64rr, CMP64rm, 0 }, { CMP8rr, CMP8rm, 0 },
                { CVTSD2SSrr, CVTSD2SSrm, 0 }, { CVTSI2SD64rr, CVTSI2SD64rm, 0 },
                { CVTSI2SDrr, CVTSI2SDrm, 0 }, { CVTSI2SS64rr, CVTSI2SS64rm, 0 },
                { CVTSI2SSrr, CVTSI2SSrm, 0 }, { CVTSS2SDrr, CVTSS2SDrm, 0 },
                { CVTTSD2SI64rr, CVTTSD2SI64rm, 0 },
                { CVTTSD2SIrr, CVTTSD2SIrm, 0 },
                { CVTTSS2SI64rr, CVTTSS2SI64rm, 0 },
                { CVTTSS2SIrr, CVTTSS2SIrm, 0 }, { FsMOVAPDrr, MOVSDrm, 0 },
                { FsMOVAPSrr, MOVSSrm, 0 }, { IMUL16rri, IMUL16rmi, 0 },
                { IMUL16rri8, IMUL16rmi8, 0 }, { IMUL32rri, IMUL32rmi, 0 },
                { IMUL32rri8, IMUL32rmi8, 0 }, { IMUL64rri32, IMUL64rmi32, 0 },
                { IMUL64rri8, IMUL64rmi8, 0 }, { Int_CMPSDrr, Int_CMPSDrm, 0 },
                { Int_CMPSSrr, Int_CMPSSrm, 0 }, { Int_COMISDrr, Int_COMISDrm, 0 },
                { Int_COMISSrr, Int_COMISSrm, 0 },
                { Int_CVTDQ2PDrr, Int_CVTDQ2PDrm, 16 },
                { Int_CVTDQ2PSrr, Int_CVTDQ2PSrm, 16 },
                { Int_CVTPD2DQrr, Int_CVTPD2DQrm, 16 },
                { Int_CVTPD2PSrr, Int_CVTPD2PSrm, 16 },
                { Int_CVTPS2DQrr, Int_CVTPS2DQrm, 16 },
                { Int_CVTPS2PDrr, Int_CVTPS2PDrm, 0 },
                { Int_CVTSD2SI64rr, Int_CVTSD2SI64rm, 0 },
                { Int_CVTSD2SIrr, Int_CVTSD2SIrm, 0 },
                { Int_CVTSD2SSrr, Int_CVTSD2SSrm, 0 },
                { Int_CVTSI2SD64rr, Int_CVTSI2SD64rm, 0 },
                { Int_CVTSI2SDrr, Int_CVTSI2SDrm, 0 },
                { Int_CVTSI2SS64rr, Int_CVTSI2SS64rm, 0 },
                { Int_CVTSI2SSrr, Int_CVTSI2SSrm, 0 },
                { Int_CVTSS2SDrr, Int_CVTSS2SDrm, 0 },
                { Int_CVTSS2SI64rr, Int_CVTSS2SI64rm, 0 },
                { Int_CVTSS2SIrr, Int_CVTSS2SIrm, 0 },
                { Int_CVTTPD2DQrr, Int_CVTTPD2DQrm, 16 },
                { Int_CVTTPS2DQrr, Int_CVTTPS2DQrm, 16 },
                { Int_CVTTSD2SI64rr, Int_CVTTSD2SI64rm, 0 },
                { Int_CVTTSD2SIrr, Int_CVTTSD2SIrm, 0 },
                { Int_CVTTSS2SI64rr, Int_CVTTSS2SI64rm, 0 },
                { Int_CVTTSS2SIrr, Int_CVTTSS2SIrm, 0 },
                { Int_UCOMISDrr, Int_UCOMISDrm, 0 },
                { Int_UCOMISSrr, Int_UCOMISSrm, 0 }, { MOV16rr, MOV16rm, 0 },
                { MOV32rr, MOV32rm, 0 }, { MOV64rr, MOV64rm, 0 },
                { MOV64toPQIrr, MOVQI2PQIrm, 0 }, { MOV64toSDrr, MOV64toSDrm, 0 },
                { MOV8rr, MOV8rm, 0 }, { MOVAPDrr, MOVAPDrm, 16 },
                { MOVAPSrr, MOVAPSrm, 16 }, { MOVDDUPrr, MOVDDUPrm, 0 },
                { MOVDI2PDIrr, MOVDI2PDIrm, 0 }, { MOVDI2SSrr, MOVDI2SSrm, 0 },
                { MOVDQArr, MOVDQArm, 16 }, { MOVSD2PDrr, MOVSD2PDrm, 0 },
                { MOVSDrr, MOVSDrm, 0 }, { MOVSHDUPrr, MOVSHDUPrm, 16 },
                { MOVSLDUPrr, MOVSLDUPrm, 16 }, { MOVSS2PSrr, MOVSS2PSrm, 0 },
                { MOVSSrr, MOVSSrm, 0 }, { MOVSX16rr8, MOVSX16rm8, 0 },
                { MOVSX32rr16, MOVSX32rm16, 0 }, { MOVSX32rr8, MOVSX32rm8, 0 },
                { MOVSX64rr16, MOVSX64rm16, 0 }, { MOVSX64rr32, MOVSX64rm32, 0 },
                { MOVSX64rr8, MOVSX64rm8, 0 }, { MOVUPDrr, MOVUPDrm, 16 },
                { MOVUPSrr, MOVUPSrm, 16 }, { MOVZDI2PDIrr, MOVZDI2PDIrm, 0 },
                { MOVZQI2PQIrr, MOVZQI2PQIrm, 0 },
                { MOVZPQILo2PQIrr, MOVZPQILo2PQIrm, 16 },
                { MOVZX16rr8, MOVZX16rm8, 0 }, { MOVZX32rr16, MOVZX32rm16, 0 },
                { MOVZX32_NOREXrr8, MOVZX32_NOREXrm8, 0 },
                { MOVZX32rr8, MOVZX32rm8, 0 }, { MOVZX64rr16, MOVZX64rm16, 0 },
                { MOVZX64rr32, MOVZX64rm32, 0 }, { MOVZX64rr8, MOVZX64rm8, 0 },
                { PSHUFDri, PSHUFDmi, 16 }, { PSHUFHWri, PSHUFHWmi, 16 },
                { PSHUFLWri, PSHUFLWmi, 16 }, { RCPPSr, RCPPSm, 16 },
                { RCPPSr_Int, RCPPSm_Int, 16 }, { RSQRTPSr, RSQRTPSm, 16 },
                { RSQRTPSr_Int, RSQRTPSm_Int, 16 }, { RSQRTSSr, RSQRTSSm, 0 },
                { RSQRTSSr_Int, RSQRTSSm_Int, 0 }, { SQRTPDr, SQRTPDm, 16 },
                { SQRTPDr_Int, SQRTPDm_Int, 16 }, { SQRTPSr, SQRTPSm, 16 },
                { SQRTPSr_Int, SQRTPSm_Int, 16 }, { SQRTSDr, SQRTSDm, 0 },
                { SQRTSDr_Int, SQRTSDm_Int, 0 }, { SQRTSSr, SQRTSSm, 0 },
                { SQRTSSr_Int, SQRTSSm_Int, 0 }, { TEST16rr, TEST16rm, 0 },
                { TEST32rr, TEST32rm, 0 }, { TEST64rr, TEST64rm, 0 },
                { TEST8rr, TEST8rm, 0 },
                // FIXME: TEST*rr EAX,EAX ---> CMP [mem], 0
                { UCOMISDrr, UCOMISDrm, 0 }, { UCOMISSrr, UCOMISSrm, 0 } };

        for (int i = 0, e = opTbl1.length; i != e; i++)
        {
            int regOp = opTbl1[i][0];
            int memOp = opTbl1[i][1];
            int alig = opTbl1[i][2];

            if (regOp2MemOpTable1.put(regOp, Pair.get(memOp, alig)) != null)
                assert false : "Duplicated entries?";

            // Index 1, folded load
            int auxInfo = 1 | (1 << 4);
            if (regOp != FsMOVAPDrr && regOp != FsMOVAPSrr)
            {
                if (memOp2RegOpTable.put(memOp, Pair.get(regOp, auxInfo)) != null)
                    ambEntries.add(memOp);
            }
        }

        int[][] opTbl2 = { { ADC32rr, ADC32rm, 0 }, { ADC64rr, ADC64rm, 0 },
                { ADD16rr, ADD16rm, 0 }, { ADD32rr, ADD32rm, 0 },
                { ADD64rr, ADD64rm, 0 }, { ADD8rr, ADD8rm, 0 },
                { ADDPDrr, ADDPDrm, 16 }, { ADDPSrr, ADDPSrm, 16 },
                { ADDSDrr, ADDSDrm, 0 }, { ADDSSrr, ADDSSrm, 0 },
                { ADDSUBPDrr, ADDSUBPDrm, 16 }, { ADDSUBPSrr, ADDSUBPSrm, 16 },
                { AND16rr, AND16rm, 0 }, { AND32rr, AND32rm, 0 },
                { AND64rr, AND64rm, 0 }, { AND8rr, AND8rm, 0 },
                { ANDNPDrr, ANDNPDrm, 16 }, { ANDNPSrr, ANDNPSrm, 16 },
                { ANDPDrr, ANDPDrm, 16 }, { ANDPSrr, ANDPSrm, 16 },
                { CMOVA16rr, CMOVA16rm, 0 }, { CMOVA32rr, CMOVA32rm, 0 },
                { CMOVA64rr, CMOVA64rm, 0 }, { CMOVAE16rr, CMOVAE16rm, 0 },
                { CMOVAE32rr, CMOVAE32rm, 0 }, { CMOVAE64rr, CMOVAE64rm, 0 },
                { CMOVB16rr, CMOVB16rm, 0 }, { CMOVB32rr, CMOVB32rm, 0 },
                { CMOVB64rr, CMOVB64rm, 0 }, { CMOVBE16rr, CMOVBE16rm, 0 },
                { CMOVBE32rr, CMOVBE32rm, 0 }, { CMOVBE64rr, CMOVBE64rm, 0 },
                { CMOVE16rr, CMOVE16rm, 0 }, { CMOVE32rr, CMOVE32rm, 0 },
                { CMOVE64rr, CMOVE64rm, 0 }, { CMOVG16rr, CMOVG16rm, 0 },
                { CMOVG32rr, CMOVG32rm, 0 }, { CMOVG64rr, CMOVG64rm, 0 },
                { CMOVGE16rr, CMOVGE16rm, 0 }, { CMOVGE32rr, CMOVGE32rm, 0 },
                { CMOVGE64rr, CMOVGE64rm, 0 }, { CMOVL16rr, CMOVL16rm, 0 },
                { CMOVL32rr, CMOVL32rm, 0 }, { CMOVL64rr, CMOVL64rm, 0 },
                { CMOVLE16rr, CMOVLE16rm, 0 }, { CMOVLE32rr, CMOVLE32rm, 0 },
                { CMOVLE64rr, CMOVLE64rm, 0 }, { CMOVNE16rr, CMOVNE16rm, 0 },
                { CMOVNE32rr, CMOVNE32rm, 0 }, { CMOVNE64rr, CMOVNE64rm, 0 },
                { CMOVNO16rr, CMOVNO16rm, 0 }, { CMOVNO32rr, CMOVNO32rm, 0 },
                { CMOVNO64rr, CMOVNO64rm, 0 }, { CMOVNP16rr, CMOVNP16rm, 0 },
                { CMOVNP32rr, CMOVNP32rm, 0 }, { CMOVNP64rr, CMOVNP64rm, 0 },
                { CMOVNS16rr, CMOVNS16rm, 0 }, { CMOVNS32rr, CMOVNS32rm, 0 },
                { CMOVNS64rr, CMOVNS64rm, 0 }, { CMOVO16rr, CMOVO16rm, 0 },
                { CMOVO32rr, CMOVO32rm, 0 }, { CMOVO64rr, CMOVO64rm, 0 },
                { CMOVP16rr, CMOVP16rm, 0 }, { CMOVP32rr, CMOVP32rm, 0 },
                { CMOVP64rr, CMOVP64rm, 0 }, { CMOVS16rr, CMOVS16rm, 0 },
                { CMOVS32rr, CMOVS32rm, 0 }, { CMOVS64rr, CMOVS64rm, 0 },
                { CMPPDrri, CMPPDrmi, 16 }, { CMPPSrri, CMPPSrmi, 16 },
                { CMPSDrr, CMPSDrm, 0 }, { CMPSSrr, CMPSSrm, 0 },
                { DIVPDrr, DIVPDrm, 16 }, { DIVPSrr, DIVPSrm, 16 },
                { DIVSDrr, DIVSDrm, 0 }, { DIVSSrr, DIVSSrm, 0 },
                { FsANDNPDrr, FsANDNPDrm, 16 }, { FsANDNPSrr, FsANDNPSrm, 16 },
                { FsANDPDrr, FsANDPDrm, 16 }, { FsANDPSrr, FsANDPSrm, 16 },
                { FsORPDrr, FsORPDrm, 16 }, { FsORPSrr, FsORPSrm, 16 },
                { FsXORPDrr, FsXORPDrm, 16 }, { FsXORPSrr, FsXORPSrm, 16 },
                { HADDPDrr, HADDPDrm, 16 }, { HADDPSrr, HADDPSrm, 16 },
                { HSUBPDrr, HSUBPDrm, 16 }, { HSUBPSrr, HSUBPSrm, 16 },
                { IMUL16rr, IMUL16rm, 0 }, { IMUL32rr, IMUL32rm, 0 },
                { IMUL64rr, IMUL64rm, 0 }, { MAXPDrr, MAXPDrm, 16 },
                { MAXPDrr_Int, MAXPDrm_Int, 16 }, { MAXPSrr, MAXPSrm, 16 },
                { MAXPSrr_Int, MAXPSrm_Int, 16 }, { MAXSDrr, MAXSDrm, 0 },
                { MAXSDrr_Int, MAXSDrm_Int, 0 }, { MAXSSrr, MAXSSrm, 0 },
                { MAXSSrr_Int, MAXSSrm_Int, 0 }, { MINPDrr, MINPDrm, 16 },
                { MINPDrr_Int, MINPDrm_Int, 16 }, { MINPSrr, MINPSrm, 16 },
                { MINPSrr_Int, MINPSrm_Int, 16 }, { MINSDrr, MINSDrm, 0 },
                { MINSDrr_Int, MINSDrm_Int, 0 }, { MINSSrr, MINSSrm, 0 },
                { MINSSrr_Int, MINSSrm_Int, 0 }, { MULPDrr, MULPDrm, 16 },
                { MULPSrr, MULPSrm, 16 }, { MULSDrr, MULSDrm, 0 },
                { MULSSrr, MULSSrm, 0 }, { OR16rr, OR16rm, 0 },
                { OR32rr, OR32rm, 0 }, { OR64rr, OR64rm, 0 }, { OR8rr, OR8rm, 0 },
                { ORPDrr, ORPDrm, 16 }, { ORPSrr, ORPSrm, 16 },
                { PACKSSDWrr, PACKSSDWrm, 16 }, { PACKSSWBrr, PACKSSWBrm, 16 },
                { PACKUSWBrr, PACKUSWBrm, 16 }, { PADDBrr, PADDBrm, 16 },
                { PADDDrr, PADDDrm, 16 }, { PADDQrr, PADDQrm, 16 },
                { PADDSBrr, PADDSBrm, 16 }, { PADDSWrr, PADDSWrm, 16 },
                { PADDWrr, PADDWrm, 16 }, { PANDNrr, PANDNrm, 16 },
                { PANDrr, PANDrm, 16 }, { PAVGBrr, PAVGBrm, 16 },
                { PAVGWrr, PAVGWrm, 16 }, { PCMPEQBrr, PCMPEQBrm, 16 },
                { PCMPEQDrr, PCMPEQDrm, 16 }, { PCMPEQWrr, PCMPEQWrm, 16 },
                { PCMPGTBrr, PCMPGTBrm, 16 }, { PCMPGTDrr, PCMPGTDrm, 16 },
                { PCMPGTWrr, PCMPGTWrm, 16 }, { PINSRWrri, PINSRWrmi, 16 },
                { PMADDWDrr, PMADDWDrm, 16 }, { PMAXSWrr, PMAXSWrm, 16 },
                { PMAXUBrr, PMAXUBrm, 16 }, { PMINSWrr, PMINSWrm, 16 },
                { PMINUBrr, PMINUBrm, 16 }, { PMULDQrr, PMULDQrm, 16 },
                { PMULHUWrr, PMULHUWrm, 16 }, { PMULHWrr, PMULHWrm, 16 },
                { PMULLDrr, PMULLDrm, 16 }, { PMULLDrr_int, PMULLDrm_int, 16 },
                { PMULLWrr, PMULLWrm, 16 }, { PMULUDQrr, PMULUDQrm, 16 },
                { PORrr, PORrm, 16 }, { PSADBWrr, PSADBWrm, 16 },
                { PSLLDrr, PSLLDrm, 16 }, { PSLLQrr, PSLLQrm, 16 },
                { PSLLWrr, PSLLWrm, 16 }, { PSRADrr, PSRADrm, 16 },
                { PSRAWrr, PSRAWrm, 16 }, { PSRLDrr, PSRLDrm, 16 },
                { PSRLQrr, PSRLQrm, 16 }, { PSRLWrr, PSRLWrm, 16 },
                { PSUBBrr, PSUBBrm, 16 }, { PSUBDrr, PSUBDrm, 16 },
                { PSUBSBrr, PSUBSBrm, 16 }, { PSUBSWrr, PSUBSWrm, 16 },
                { PSUBWrr, PSUBWrm, 16 }, { PUNPCKHBWrr, PUNPCKHBWrm, 16 },
                { PUNPCKHDQrr, PUNPCKHDQrm, 16 },
                { PUNPCKHQDQrr, PUNPCKHQDQrm, 16 },
                { PUNPCKHWDrr, PUNPCKHWDrm, 16 }, { PUNPCKLBWrr, PUNPCKLBWrm, 16 },
                { PUNPCKLDQrr, PUNPCKLDQrm, 16 },
                { PUNPCKLQDQrr, PUNPCKLQDQrm, 16 },
                { PUNPCKLWDrr, PUNPCKLWDrm, 16 }, { PXORrr, PXORrm, 16 },
                { SBB32rr, SBB32rm, 0 }, { SBB64rr, SBB64rm, 0 },
                { SHUFPDrri, SHUFPDrmi, 16 }, { SHUFPSrri, SHUFPSrmi, 16 },
                { SUB16rr, SUB16rm, 0 }, { SUB32rr, SUB32rm, 0 },
                { SUB64rr, SUB64rm, 0 }, { SUB8rr, SUB8rm, 0 },
                { SUBPDrr, SUBPDrm, 16 }, { SUBPSrr, SUBPSrm, 16 },
                { SUBSDrr, SUBSDrm, 0 }, { SUBSSrr, SUBSSrm, 0 },
                // FIXME: TEST*rr -> swapped operand of TEST*mr.
                { UNPCKHPDrr, UNPCKHPDrm, 16 }, { UNPCKHPSrr, UNPCKHPSrm, 16 },
                { UNPCKLPDrr, UNPCKLPDrm, 16 }, { UNPCKLPSrr, UNPCKLPSrm, 16 },
                { XOR16rr, XOR16rm, 0 }, { XOR32rr, XOR32rm, 0 },
                { XOR64rr, XOR64rm, 0 }, { XOR8rr, XOR8rm, 0 },
                { XORPDrr, XORPDrm, 16 }, { XORPSrr, XORPSrm, 16 } };

        for (int i = 0, e = opTbl2.length; i < e; i++)
        {
            int regOp = opTbl2[i][0];
            int memOp = opTbl2[i][1];
            int align = opTbl2[i][2];
            if (regOp2MemOpTable2.put(regOp, Pair.get(memOp, align)) != null)
                assert false : "Duplicated entries?";

            // Index 2, folded load
            int auxInfo = 2 | (1 << 4);
            if (memOp2RegOpTable.put(memOp, Pair.get(regOp, auxInfo)) != null)
                ambEntries.add(memOp);
        }

        // Remove ambiguous entries.
        assert ambEntries.isEmpty() : "Duplicated entries in unfolded map?";
    }

    /**
     * TargetInstrInfo is a superset of MRegister info.  As such, whenever
     * a client has an instance of instruction info, it should always be able
     * to get register info as well (through this method).
     */
    public TargetRegisterInfo getRegisterInfo()
    {
        return registerInfo;
    }

    @Override
    public boolean isMoveInstr(MachineInstr mi, OutParamWrapper<Integer> srcReg,
            OutParamWrapper<Integer> destReg,
            OutParamWrapper<Integer> srcSubIdx,
            OutParamWrapper<Integer> destSubIdx)
    {
        switch (mi.getOpcode())
        {
            default:
                return false;
            case MOV8rr:
            case MOV8rr_NOREX:
            case MOV16rr:
            case MOV32rr:
            case MOV64rr:
            case MOVSSrr:
            case MOVSDrr:

                // FP Stack register class copies
            case MOV_Fp3232:
            case MOV_Fp6464:
            case MOV_Fp8080:
            case MOV_Fp3264:
            case MOV_Fp3280:
            case MOV_Fp6432:
            case MOV_Fp8032:

            case FsMOVAPSrr:
            case FsMOVAPDrr:
            case MOVAPSrr:
            case MOVAPDrr:
            case MOVDQArr:
            case MOVSS2PSrr:
            case MOVSD2PDrr:
            case MOVPS2SSrr:
            case MOVPD2SDrr:
            case MMX_MOVQ64rr:
                assert mi.getNumOperands() >= 2 && mi.getOperand(0).isReg()
                        && mi.getOperand(1)
                        .isReg() : "invalid register-register move instruction";
                srcReg.set(mi.getOperand(1).getReg());
                destReg.set(mi.getOperand(0).getReg());
                srcSubIdx.set(mi.getOperand(1).getSubReg());
                destSubIdx.set(mi.getOperand(0).getSubReg());
                return true;
        }
    }

    @Override public int isLoadFromStackSlot(MachineInstr mi,
            OutParamWrapper<Integer> frameIndex)
    {
        switch (mi.getOpcode())
        {
            default:
                break;
            case MOV8rm:
            case MOV16rm:
            case MOV32rm:
            case MOV64rm:
            case LD_Fp64m:
            case MOVSSrm:
            case MOVSDrm:
            case MOVAPSrm:
            case MOVAPDrm:
            case MOVDQArm:
            case MMX_MOVD64rm:
            case MMX_MOVQ64rm:
            {
                if (mi.getOperand(1).isFrameIndex() && mi.getOperand(2).isImm()
                        && mi.getOperand(3).isReg() && mi.getOperand(4).isImm()
                        && mi.getOperand(2).getImm() == 1
                        && mi.getOperand(3).getReg() == 0
                        && mi.getOperand(4).getImm() == 0)
                {
                    frameIndex.set(mi.getOperand(1).getIndex());
                    return mi.getOperand(0).getReg();
                }
            }
        }
        return 0;
    }

    private int x86AddrNumOperands = 5;

    @Override public int isStoreToStackSlot(MachineInstr mi,
            OutParamWrapper<Integer> frameIndex)
    {
        switch (mi.getOpcode())
        {
            default:
                break;
            case MOV8mr:
            case MOV16mr:
            case MOV32mr:
            case MOV64mr:
            case ST_FpP64m:
            case MOVSSmr:
            case MOVSDmr:
            case MOVAPSmr:
            case MOVAPDmr:
            case MOVDQAmr:
            case MMX_MOVD64mr:
            case MMX_MOVQ64mr:
            case MMX_MOVNTQmr:
            {
                if (mi.getOperand(0).isFrameIndex() && mi.getOperand(1).isImm()
                        && mi.getOperand(2).isReg() && mi.getOperand(3).isImm()
                        && mi.getOperand(1).getImm() == 1
                        && mi.getOperand(2).getReg() == 0
                        && mi.getOperand(3).getImm() == 0)
                {
                    frameIndex.set(mi.getOperand(0).getIndex());
                    return mi.getOperand(x86AddrNumOperands).getReg();
                }
            }
        }
        return 0;
    }

    /**
     * Return true if register is PIC base (i.e.g defined by
     * MOVPC32r.
     *
     * @param baseReg
     * @param mri
     * @return
     */
    private static boolean regIsPICBase(int baseReg, MachineRegisterInfo mri)
    {
        boolean isPICBase = false;
        MachineRegisterInfo.DefUseChainIterator itr = mri.getDefIterator(baseReg);
        for (; !itr.atEnd(); itr.next())
        {
            MachineInstr defMI = itr.getMachineInstr();
            if (defMI.getOpcode() != MOVPC32r)
                return false;

            assert !isPICBase : "More than one PIC base?";
            isPICBase = true;
        }
        return isPICBase;
    }

    /**
     * Return true if a load with the specified
     * operand is a candidate for remat: for this to be true we need to know that
     * the load will always return the same value, even if moved.
     *
     * @param mo
     * @param tm
     * @return
     */
    private static boolean canRematLoadWithDispOperand(MachineOperand mo,
            X86TargetMachine tm)
    {
        if (mo.isConstantPoolIndex())
            return true;

        if (mo.isGlobalAddress())
        {
            if (isGlobalStubRerence(mo.getTargetFlags()))
                return true;

            GlobalVariable gv = (GlobalVariable) mo.getGlobal();
            if (gv.isConstant())
                return true;
        }
        return false;
    }

    private static boolean isGlobalStubRerence(int targetFlags)
    {
        switch (targetFlags)
        {
            case X86II.MO_DLLIMPORT:     // dllimport stub.
            case X86II.MO_GOTPCREL:  // rip-relative GOT reference.
            case X86II.MO_GOT:       // normal GOT reference.
            case X86II.MO_DARWIN_NONLAZY_PIC_BASE:        // Normal $non_lazy_ptr ref.
            case X86II.MO_DARWIN_NONLAZY:                 // Normal $non_lazy_ptr ref.
            case X86II.MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE: // Hidden $non_lazy_ptr ref.
            case X86II.MO_DARWIN_HIDDEN_NONLAZY:          // Hidden $non_lazy_ptr ref.
                return true;
            default:
                return false;
        }
    }

    @Override
    protected boolean isReallyTriviallyReMaterializable(MachineInstr mi)
    {
        switch (mi.getOpcode())
        {
            default:
                break;
            case MOV8rm:
            case MOV16rm:
            case MOV32rm:
            case MOV64rm:
            case LD_Fp64m:
            case MOVSSrm:
            case MOVSDrm:
            case MOVAPSrm:
            case MOVAPDrm:
            case MOVDQArm:
            case MMX_MOVD64rm:
            case MMX_MOVQ64rm:
            {
                // Loads from constant pools are trivially rematerializable.
                if (mi.getOperand(1).isReg() && mi.getOperand(2).isImm() && mi.getOperand(3).isReg() &&
                        mi.getOperand(3).getReg() == 0
                        && canRematLoadWithDispOperand(mi.getOperand(4), tm))
                {
                    int baseReg = mi.getOperand(1).getReg();
                    if (baseReg == 0 || baseReg == RIP)
                        return true;

                    if (!ReMatPICStubLoad.value && mi.getOperand(4).isGlobalAddress())
                        return false;

                    MachineFunction mf = mi.getParent().getParent();
                    MachineRegisterInfo mri = mf.getMachineRegisterInfo();
                    boolean isPICBase = false;
                    MachineRegisterInfo.DefUseChainIterator itr = mri.getDefIterator(baseReg);
                    for (; !itr.atEnd(); itr.next())
                    {
                        MachineInstr defMI = itr.getMachineInstr();
                        if (defMI.getOpcode() != MOVPC32r)
                            return false;

                        assert !isPICBase : "More than one PIC base?";
                        isPICBase = true;
                    }
                    return isPICBase;
                }
                return false;
            }
            case LEA32r:
            case LEA64r:
            {
                if (mi.getOperand(2).isImm() && mi.getOperand(3).isReg()
                        && mi.getOperand(3).getReg() == 0 && !mi.getOperand(4).isReg())
                {
                    // lea fi#, lea GV, etc. are all rematerializable.
                    if (!mi.getOperand(1).isReg())
                        return true;
                    int baseReg = mi.getOperand(1).getReg();
                    if (baseReg == 0)
                        return true;

                    MachineFunction mf = mi.getParent().getParent();
                    MachineRegisterInfo mri = mf.getMachineRegisterInfo();
                    return regIsPICBase(baseReg, mri);
                }
                return false;
            }
        }
        // All other instructions marked M_REMATERIALIZABLE are always trivially
        // rematerializable.
        return true;
    }

    /**
     * Return true if it's safe insert an instruction that
     * would clobber the EFLAGS condition register. Note the result may be
     * conservative. If it cannot definitely determine the safety after visiting
     * two instructions it assumes it's not safe.
     *
     * @param mbb
     * @param idx
     * @return
     */
    private static boolean isSafeToClobberEFlags(MachineBasicBlock mbb, int idx)
    {
        if (idx == mbb.size())
            return true;

        for (int i = 0; i < 2; i++)
        {
            MachineInstr mi = mbb.getInstAt(idx);
            boolean seenDef = false;
            for (int j = 0, e = mi.getNumOperands(); j < e; j++)
            {
                MachineOperand mo = mi.getOperand(j);
                if (!mo.isReg())
                    continue;
                if (mo.getReg() == EFLAGS)
                {
                    if (mo.isUse())
                        return false;
                    seenDef = true;
                }
            }

            if (seenDef)
            {
                // This instruction defines EFLAGS, no need to look any further.
                return true;
            }
            idx++;

            if (idx == mbb.size())
                return true;
        }
        return false;
    }

    @Override
    public void reMaterialize(MachineBasicBlock mbb, int insertPos, int destReg,
            int subIdx, MachineInstr origin)
    {
        if (subIdx != 0 && TargetRegisterInfo.isPhysicalRegister(destReg))
        {
            destReg = registerInfo.getSubReg(destReg, subIdx);
            subIdx = 0;
        }

        // MOV32r0 etc. are implemented with xor which clobbers condition code.
        // Re-materialize them as movri instructions to avoid side effects.
        boolean clone = true;
        int opc = origin.getOpcode();
        switch (opc)
        {
            default:
                break;
            case MOV8r0:
            case MOV16r0:
            case MOV32r0:
            {
                if (!isSafeToClobberEFlags(mbb, insertPos))
                {
                    switch (opc)
                    {
                        default:
                            break;
                        case MOV8r0:
                            opc = MOV8ri;
                            break;
                        case MOV16r0:
                            opc = MOV16ri;
                            break;
                        case MOV32r0:
                            opc = MOV32ri;
                            break;
                    }
                    clone = false;
                }
                break;
            }
        }

        if (clone)
        {
            MachineInstr mi = origin.clone();
            mi.getOperand(0).setReg(destReg);
            mbb.insert(insertPos, mi);
        }
        else
        {
            buildMI(mbb, insertPos, get(opc), destReg).addImm(0);
        }

        // Get the prior instruction to current instr.
        MachineInstr newMI = mbb.getInstAt(insertPos - 1);
        newMI.getOperand(0).setSubreg(subIdx);
    }

    /**
     * Return true if the specified instruction (which is marked
     * mayLoad) is loading from a location whose value is invariant across the
     * function.  For example, loading a value from the constant pool or from
     * from the argument area of a function if it does not change.  This should
     * only return true of *all* loads the instruction does are invariant (if it
     * does multiple loads).
     *
     * @param mi
     * @return
     */
    @Override public boolean isInvariantLoad(MachineInstr mi)
    {
        // This code cares about loads from three cases: constant pool entries,
        // invariant argument slots, and global stubs.  In order to handle these cases
        // for all of the myriad of X86 instructions, we just scan for a CP/FI/GV
        // operand and base our analysis on it.  This is safe because the address of
        // none of these three cases is ever used as anything other than a load base
        // and X86 doesn't have any instructions that load from multiple places.
        for (int i = 0, e = mi.getNumOperands(); i < e; i++)
        {
            MachineOperand mo = mi.getOperand(i);
            if (mo.isConstantPoolIndex())
                return true;

            if (mo.isGlobalAddress())
                return isGlobalStubRerence(mo.getTargetFlags());

            if (mo.isFrameIndex())
            {
                MachineFrameInfo mfi = mi.getParent().getParent().getFrameInfo();
                int idx = mo.getIndex();
                return mfi.isFixedObjectIndex(idx) && mfi.isImmutableObjectIndex(idx);
            }
        }
        return false;
    }

    /**
     * True if MI has a condition code def, e.g. EFLAGS, that
     * is not marked dead.
     * @param mi
     * @return
     */
    static boolean hasLiveCondCodeDef(MachineInstr mi)
    {
        for (int i = 0, e = mi.getNumOperands(); i != e; i++)
        {
            MachineOperand mo = mi.getOperand(i);
            if (mo.isReg() && mo.isDef() && mo.getReg() == EFLAGS &&
                    !mo.isDead())
                return true;
        }
        return false;
    }

    public int getBaseOpcodeFor(TargetInstrDesc tid)
    {
        return tid.tSFlags >> X86II.OpcodeShift;
    }

    /**
	 * This function returns the "base" X86 opcode for the
	 * specified opcode number.
	 * @param opcode
	 * @return
	 */
	public int getBaseOpcodeFor(int opcode)
	{
		return getBaseOpcodeFor(get(opcode));
	}

    /**
     * Return a virtual register initialized with the
     * the global base register value. Output instructions required to
     * initialize the register in the function entry block, if necessary.
     * @param mf
     * @return
     */
	public int getGlobalBaseReg(MachineFunction mf)
	{
		assert !tm.getSubtarget().is64Bit():"X86-64 PIC uses RIP relative addressing";
        // TODO: 17-7-23
        return 0;
	}
}
