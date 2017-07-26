package backend.target.x86;

import backend.analysis.LiveVariable;
import backend.codegen.*;
import backend.target.TargetInstrDesc;
import backend.target.TargetInstrInfoImpl;
import backend.target.TargetRegisterInfo;
import backend.value.GlobalVariable;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import tools.OutParamWrapper;
import tools.Pair;
import tools.Util;
import tools.commandline.BooleanOpt;
import tools.commandline.OptionHiddenApplicator;
import tools.commandline.OptionNameApplicator;

import java.util.ArrayList;

import static backend.codegen.MachineInstrBuilder.*;
import static backend.codegen.MachineOperand.RegState.Define;
import static backend.codegen.MachineOperand.RegState.Kill;
import static backend.target.x86.X86GenInstrNames.*;
import static backend.target.x86.X86GenRegisterInfo.GR32RegisterClass;
import static backend.target.x86.X86GenRegisterNames.EFLAGS;
import static backend.target.x86.X86GenRegisterNames.RIP;
import static backend.target.x86.X86RegisterInfo.SUBREG_16BIT;
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
     * True if mi has a condition code def, e.g. EFLAGS, that
     * is not marked dead.
     *
     * @param mi
     * @return
     */
    public static boolean hasLiveCondCodeDef(MachineInstr mi)
    {
        for (int i = 0, e = mi.getNumOperands(); i != e; i++)
        {
            MachineOperand mo = mi.getOperand(i);
            if (mo.isReg() && mo.isDef() && mo.getReg() == EFLAGS && !mo.isDead())
                return true;
        }
        return false;
    }

    /**
     * <p>
     * This method must be implemented by targets that
     * set the M_CONVERTIBLE_TO_3_ADDR flag.  When this flag is set, the target
     * may be able to convert a two-address instruction into a true
     * three-address instruction on demand.  This allows the X86 target (for
     * example) to convert ADD and SHL instructions into LEA instructions if they
     * would require register copies due to two-addressness.
     * </p>
     * <p>
     * This method returns a null pointer if the transformation cannot be
     * performed, otherwise it returns the new instruction.
     * </p>
     *
     * @param mbb
     * @param insertPos
     * @param lv
     * @return
     */
    public MachineInstr convertToThreeAddress(MachineBasicBlock mbb,
            int insertPos, LiveVariable lv)
    {
        MachineInstr mi = mbb.getInstAt(insertPos);
        MachineFunction mf = mi.getParent().getParent();

        // All instructions input are two-addr instructions.  Get the known operands.
        int dest = mi.getOperand(0).getReg();
        int src = mi.getOperand(1).getReg();
        boolean isDead = mi.getOperand(0).isDead();
        boolean isKill = mi.getOperand(1).isKill();

        MachineInstr newMI = null;

        // FIXME: 16-bit LEA's are really slow on Athlons, but not bad on P4's.  When
        // we have better subtarget support, enable the 16-bit LEA generation here.
        boolean disableLEA16 = true;

        int miOpc = mi.getOpcode();
        switch (miOpc)
        {
            case SHUFPSrri:
            {
                assert mi.getNumOperands() == 4:"Unknown shufps instruction!";
                if (!tm.getSubtarget().hasSSE2())
                    return null;

                int b = mi.getOperand(1).getReg();
                int c = mi.getOperand(2).getReg();
                if (b != c)
                    return null;

                int a = mi.getOperand(0).getReg();
                long m = mi.getOperand(3).getImm();
                newMI = buildMI(get(PSHUFDri)).addReg(a,
                        Define | getDeadRegState(isDead)).
                        addReg(b, getKillRegState(isKill)).
                        addImm(m).getMInstr();
                break;
            }
            case SHL64ri:
            {
                assert mi.getNumOperands() >= 3:"Unknown shift instruction";
                long shAmt = mi.getOperand(2).getImm();
                if (shAmt == 0 || shAmt >= 4) return null;

                newMI = buildMI(get(LEA64r)).addReg(dest, Define | getDeadRegState(isDead))
                        .addReg(0)      // base register
                        .addImm(1<<shAmt)   // scale
                        .addReg(src, getKillRegState(isKill))   // index register
                        .addImm(0)      // displacement.
                        .getMInstr();
                // LEA 0(1<<shAmt * src + 0), %dest.
                break;
            }
            case SHL32ri:
            {
                assert mi.getNumOperands() >= 3:"Unknown shufps instruction!";
                long shAmt = mi.getOperand(2).getImm();
                if (shAmt == 0 || shAmt >= 4) return null;

                int opc = tm.getSubtarget().is64Bit() ? LEA64_32r : LEA32r;
                newMI = buildMI(get(opc))
                        .addReg(dest, Define | getDeadRegState(isDead))
                        .addReg(0)
                        .addImm(1 << shAmt)
                        .addReg(src, getKillRegState(isKill))
                        .addImm(0)
                        .getMInstr();
                break;
            }
            case SHL16ri:
            {
                assert mi.getNumOperands() >= 3:"Unknown shift instruction";

                long shAmt = mi.getOperand(2).getImm();
                if (shAmt == 0 || shAmt >= 4)
                    return null;

                if (disableLEA16)
                {
                    // If 16-bit LEA is disabled, use 32-bit LEA via subregisters.
                    MachineRegisterInfo regInfo = mbb.getParent().getMachineRegisterInfo();
                    int opc = tm.getSubtarget().is64Bit() ? LEA64_32r : LEA32r;
                    int leaInReg = regInfo.createVirtualRegister(GR32RegisterClass);
                    int leaOutReg = regInfo.createVirtualRegister(GR32RegisterClass);

                    buildMI(mbb, insertPos, get(IMPLICIT_DEF), leaInReg);
                    MachineInstr insMI = buildMI(mbb,insertPos, get(INSERT_SUBREG), leaInReg)
                            .addReg(leaInReg)
                            .addReg(src, getKillRegState(isKill))
                            .addImm(SUBREG_16BIT)
                            .getMInstr();

                    MachineInstr extMI = buildMI(mbb, insertPos, get(EXTRACT_SUBREG))
                            .addReg(dest, Define | getDeadRegState(isDead))
                            .addReg(leaOutReg, Kill)
                            .addImm(SUBREG_16BIT)
                            .getMInstr();
                    if (lv != null)
                    {
                        lv.getVarInfo(leaInReg).kills.add(newMI);
                        lv.getVarInfo(leaOutReg).kills.add(extMI);
                        if (isKill)
                            lv.replaceKillInstruction(src, mi, newMI);
                        if (isDead)
                            lv.replaceKillInstruction(dest, mi, extMI);
                    }
                    return extMI;
                }
                else
                {
                    newMI = buildMI(get(LEA16r))
                            .addReg(dest, Define | getDeadRegState(isDead))
                            .addReg(0)
                            .addImm(1 << shAmt)
                            .addReg(src, getKillRegState(isKill))
                            .addImm(0)
                            .getMInstr();
                }
                break;
            }
            default:
            {
                // The following opcodes also sets the condition code register(s). Only
                // convert them to equivalent lea if the condition code register def's
                // are dead!
                if (hasLiveCondCodeDef(mi))
                    return null;

                boolean is64Bit = tm.getSubtarget().is64Bit;
                switch (miOpc)
                {
                    case INC64r:
                    case INC32r:
                    case INC64_32r:
                    {
                        assert mi.getNumOperands() >= 2:"Unknown inc instruction";
                        int opc = miOpc == INC64r ? LEA64r : (is64Bit ? LEA64_32r : LEA32r);
                        newMI = addLeaRegOffset(buildMI(get(opc))
                            .addReg(dest, Define | getDeadRegState(isDead)), src, isKill, 1)
                            .getMInstr();
                        break;
                    }
                    case INC16r:
                    case INC64_16r:
                    {
                        if (disableLEA16)
                            return null;
                        assert mi.getNumOperands() >= 2 :"Unknown inc instruction";
                        newMI = addRegOffset(buildMI(get(LEA16r)).
                                addReg(dest, Define | getDeadRegState(isDead)),
                                src, isKill, 1)
                                .getMInstr();
                        break;
                    }
                    case DEC64r:
                    case DEC32r:
                    case DEC64_32r:
                    {
                        assert mi.getNumOperands() >= 2 :"Unknown dec instruction";
                        int opc = miOpc == DEC64r ? LEA64r : (is64Bit ? LEA64_32r : LEA32r);
                        newMI = addLeaRegOffset(buildMI(get(opc)).
                                addReg(dest, Define | getDeadRegState(isDead)),
                                src, isKill, -1)
                                .getMInstr();
                        break;
                    }
                    case DEC16r:
                    case DEC64_16r:
                        if (disableLEA16) return null;
                        assert mi.getNumOperands() >= 2 : "Unknown dec instruction!";
                        newMI = addRegOffset(buildMI(get(LEA16r))
                                        .addReg(dest, Define | getDeadRegState(isDead)),
                                src, isKill, -1)
                                .getMInstr();
                        break;
                    case ADD64rr:
                    case ADD32rr: {
                        assert mi.getNumOperands() >= 3 : "Unknown add instruction!";
                        int Opc = miOpc == ADD64rr ? LEA64r
                                : (is64Bit ? LEA64_32r : LEA32r);
                        int Src2 = mi.getOperand(2).getReg();
                        boolean isKill2 = mi.getOperand(2).isKill();
                        newMI = addRegReg(buildMI(get(Opc))
                                        .addReg(dest, Define|getDeadRegState(isDead)),
                                src, isKill, Src2, isKill2)
                                .getMInstr();
                        if (lv != null && isKill2)
                            lv.replaceKillInstruction(Src2, mi, newMI);
                        break;
                    }
                    case ADD16rr: {
                        if (disableLEA16)
                            return null;
                        assert mi.getNumOperands() >= 3 : "Unknown add instruction!";
                        int Src2 = mi.getOperand(2).getReg();
                        boolean isKill2 = mi.getOperand(2).isKill();
                        newMI = addRegReg(buildMI(get(LEA16r))
                                        .addReg(dest, Define | getDeadRegState(isDead)),
                                src, isKill, Src2, isKill2)
                                .getMInstr();
                        if (lv != null && isKill2)
                            lv.replaceKillInstruction(Src2, mi, newMI);
                        break;
                    }
                    case ADD64ri32:
                    case ADD64ri8:
                        assert mi.getNumOperands() >= 3 : "Unknown add instruction!";
                        if (mi.getOperand(2).isImm())
                            newMI = addLeaRegOffset(buildMI(get(LEA64r))
                                            .addReg(dest, Define | getDeadRegState(isDead)),
                                    src, isKill, mi.getOperand(2).getImm())
                                    .getMInstr();
                        break;
                    case ADD32ri:
                    case ADD32ri8:
                        assert mi.getNumOperands() >= 3 : "Unknown add instruction!";
                        if (mi.getOperand(2).isImm()) {
                            int Opc = is64Bit ? LEA64_32r : LEA32r;
                            newMI = addLeaRegOffset(buildMI(get(Opc))
                                            .addReg(dest, Define | getDeadRegState(isDead)),
                                    src, isKill, mi.getOperand(2).getImm())
                                    .getMInstr();
                        }
                        break;
                    case ADD16ri:
                    case ADD16ri8:
                        if (disableLEA16) return null;
                        assert mi.getNumOperands() >= 3 : "Unknown add instruction!";
                        if (mi.getOperand(2).isImm())
                            newMI = addRegOffset(buildMI(get(LEA16r))
                                            .addReg(dest, Define | getDeadRegState(isDead)),
                                    src, isKill, mi.getOperand(2).getImm())
                                    .getMInstr();
                        break;
                    case SHL16ri:
                        if (disableLEA16) return null;
                    case SHL32ri:
                    case SHL64ri: {
                        assert mi.getNumOperands() >= 3 && mi.getOperand(2).isImm() :
                                "Unknown shl instruction!";
                        long ShAmt = mi.getOperand(2).getImm();
                        if (ShAmt == 1 || ShAmt == 2 || ShAmt == 3) {
                            X86AddressMode am = new X86AddressMode();
                            am.scale = 1 << ShAmt;
                            am.indexReg = src;
                            int Opc = miOpc == SHL64ri ? LEA64r
                                    : (miOpc == SHL32ri
                                    ? (is64Bit ? LEA64_32r : LEA32r) : LEA16r);
                            newMI = addFullAddress(buildMI(get(Opc))
                                    .addReg(dest, Define | getDeadRegState(isDead)), am)
                                    .getMInstr();
                            if (isKill)
                                newMI.getOperand(3).setIsKill(true);
                        }
                        break;
                    }
                }
            }
        }
        if (newMI == null)
            return null;

        if (lv != null)
        {
            if (isKill)
                lv.replaceKillInstruction(src, mi, newMI);
            if (isDead)
                lv.replaceKillInstruction(dest,mi, newMI);
        }

        mbb.insert(insertPos, newMI);
        return newMI;
    }

    /**
     * We have a few instructions that must be hacked on to commute them.
     * @param mi
     * @param newMI
     * @return
     */
    public MachineInstr commuteInstuction(MachineInstr mi, boolean newMI)
    {
        switch (mi.getOpcode()) 
        {
            case SHRD16rri8: // A = SHRD16rri8 B, C, I -> A = SHLD16rri8 C, B, (16-I)
            case SHLD16rri8: // A = SHLD16rri8 B, C, I -> A = SHRD16rri8 C, B, (16-I)
            case SHRD32rri8: // A = SHRD32rri8 B, C, I -> A = SHLD32rri8 C, B, (32-I)
            case SHLD32rri8: // A = SHLD32rri8 B, C, I -> A = SHRD32rri8 C, B, (32-I)
            case SHRD64rri8: // A = SHRD64rri8 B, C, I -> A = SHLD64rri8 C, B, (64-I)
            case SHLD64rri8:{// A = SHLD64rri8 B, C, I -> A = SHRD64rri8 C, B, (64-I)
                int Opc;
                int Size;
                switch (mi.getOpcode()) 
                {
                    default:
                        Util.shouldNotReachHere("Unreachable!");
                    case SHRD16rri8: Size = 16; Opc = SHLD16rri8; break;
                    case SHLD16rri8: Size = 16; Opc = SHRD16rri8; break;
                    case SHRD32rri8: Size = 32; Opc = SHLD32rri8; break;
                    case SHLD32rri8: Size = 32; Opc = SHRD32rri8; break;
                    case SHRD64rri8: Size = 64; Opc = SHLD64rri8; break;
                    case SHLD64rri8: Size = 64; Opc = SHRD64rri8; break;
                }
                long Amt = mi.getOperand(3).getImm();
                if (newMI)
                {
                    MachineFunction mf = mi.getParent().getParent();
                    mi = mi.clone();
                    newMI = false;
                }
                mi.setDesc(get(Opc));
                mi.getOperand(3).setImm(Size-Amt);
                return super.commuteInstruction(mi, newMI);
            }
            case CMOVB16rr:
            case CMOVB32rr:
            case CMOVB64rr:
            case CMOVAE16rr:
            case CMOVAE32rr:
            case CMOVAE64rr:
            case CMOVE16rr:
            case CMOVE32rr:
            case CMOVE64rr:
            case CMOVNE16rr:
            case CMOVNE32rr:
            case CMOVNE64rr:
            case CMOVBE16rr:
            case CMOVBE32rr:
            case CMOVBE64rr:
            case CMOVA16rr:
            case CMOVA32rr:
            case CMOVA64rr:
            case CMOVL16rr:
            case CMOVL32rr:
            case CMOVL64rr:
            case CMOVGE16rr:
            case CMOVGE32rr:
            case CMOVGE64rr:
            case CMOVLE16rr:
            case CMOVLE32rr:
            case CMOVLE64rr:
            case CMOVG16rr:
            case CMOVG32rr:
            case CMOVG64rr:
            case CMOVS16rr:
            case CMOVS32rr:
            case CMOVS64rr:
            case CMOVNS16rr:
            case CMOVNS32rr:
            case CMOVNS64rr:
            case CMOVP16rr:
            case CMOVP32rr:
            case CMOVP64rr:
            case CMOVNP16rr:
            case CMOVNP32rr:
            case CMOVNP64rr:
            case CMOVO16rr:
            case CMOVO32rr:
            case CMOVO64rr:
            case CMOVNO16rr:
            case CMOVNO32rr:
            case CMOVNO64rr: {
                int opc = 0;
                switch (mi.getOpcode()) {
                    default: break;
                    case CMOVB16rr:  opc = CMOVAE16rr; break;
                    case CMOVB32rr:  opc = CMOVAE32rr; break;
                    case CMOVB64rr:  opc = CMOVAE64rr; break;
                    case CMOVAE16rr: opc = CMOVB16rr; break;
                    case CMOVAE32rr: opc = CMOVB32rr; break;
                    case CMOVAE64rr: opc = CMOVB64rr; break;
                    case CMOVE16rr:  opc = CMOVNE16rr; break;
                    case CMOVE32rr:  opc = CMOVNE32rr; break;
                    case CMOVE64rr:  opc = CMOVNE64rr; break;
                    case CMOVNE16rr: opc = CMOVE16rr; break;
                    case CMOVNE32rr: opc = CMOVE32rr; break;
                    case CMOVNE64rr: opc = CMOVE64rr; break;
                    case CMOVBE16rr: opc = CMOVA16rr; break;
                    case CMOVBE32rr: opc = CMOVA32rr; break;
                    case CMOVBE64rr: opc = CMOVA64rr; break;
                    case CMOVA16rr:  opc = CMOVBE16rr; break;
                    case CMOVA32rr:  opc = CMOVBE32rr; break;
                    case CMOVA64rr:  opc = CMOVBE64rr; break;
                    case CMOVL16rr:  opc = CMOVGE16rr; break;
                    case CMOVL32rr:  opc = CMOVGE32rr; break;
                    case CMOVL64rr:  opc = CMOVGE64rr; break;
                    case CMOVGE16rr: opc = CMOVL16rr; break;
                    case CMOVGE32rr: opc = CMOVL32rr; break;
                    case CMOVGE64rr: opc = CMOVL64rr; break;
                    case CMOVLE16rr: opc = CMOVG16rr; break;
                    case CMOVLE32rr: opc = CMOVG32rr; break;
                    case CMOVLE64rr: opc = CMOVG64rr; break;
                    case CMOVG16rr:  opc = CMOVLE16rr; break;
                    case CMOVG32rr:  opc = CMOVLE32rr; break;
                    case CMOVG64rr:  opc = CMOVLE64rr; break;
                    case CMOVS16rr:  opc = CMOVNS16rr; break;
                    case CMOVS32rr:  opc = CMOVNS32rr; break;
                    case CMOVS64rr:  opc = CMOVNS64rr; break;
                    case CMOVNS16rr: opc = CMOVS16rr; break;
                    case CMOVNS32rr: opc = CMOVS32rr; break;
                    case CMOVNS64rr: opc = CMOVS64rr; break;
                    case CMOVP16rr:  opc = CMOVNP16rr; break;
                    case CMOVP32rr:  opc = CMOVNP32rr; break;
                    case CMOVP64rr:  opc = CMOVNP64rr; break;
                    case CMOVNP16rr: opc = CMOVP16rr; break;
                    case CMOVNP32rr: opc = CMOVP32rr; break;
                    case CMOVNP64rr: opc = CMOVP64rr; break;
                    case CMOVO16rr:  opc = CMOVNO16rr; break;
                    case CMOVO32rr:  opc = CMOVNO32rr; break;
                    case CMOVO64rr:  opc = CMOVNO64rr; break;
                    case CMOVNO16rr: opc = CMOVO16rr; break;
                    case CMOVNO32rr: opc = CMOVO32rr; break;
                    case CMOVNO64rr: opc = CMOVO64rr; break;
                }
                if (newMI)
                {
                    mi = mi.clone();
                    newMI = false;
                }
                mi.setDesc(get(opc));
                // Fallthrough intended.
            }
            default:
                return super.commuteInstruction(mi, newMI);
        }
    }

    public static int getCondFromBranchOpc(int brOpc)
    {
        switch (brOpc)
        {
            default:
                return CondCode.COND_INVALID;
            case JE:
                return CondCode.COND_E;
            case JNE:
                return CondCode.COND_NE;
            case JL:  return CondCode.COND_L;
            case JLE: return CondCode.COND_LE;
            case JG:  return CondCode.COND_G;
            case JGE: return CondCode.COND_GE;
            case JB:  return CondCode.COND_B;
            case JBE: return CondCode.COND_BE;
            case JA:  return CondCode.COND_A;
            case JAE: return CondCode.COND_AE;
            case JS:  return CondCode.COND_S;
            case JNS: return CondCode.COND_NS;
            case JP:  return CondCode.COND_P;
            case JNP: return CondCode.COND_NP;
            case JO:  return CondCode.COND_O;
            case JNO: return CondCode.COND_NO;
        }
    }

    public int getCondBranchFromCond(int cc)
    {
        switch (cc)
        {
            default:
                Util.shouldNotReachHere("Illegal condition code!");
            case  CondCode.COND_E:
                return JE;
            case CondCode.COND_NE:
                return JNE;
            case CondCode.COND_L:
                return JL;
            case CondCode.COND_LE:
                return JLE;
            case CondCode.COND_G:
                return JG;
            case CondCode.COND_GE:
                return JGE;
            case CondCode.COND_B:
                return JB;
            case CondCode.COND_BE:
                return JBE;
            case CondCode.COND_A:
                return JA;
            case CondCode.COND_AE:
                return JAE;
            case CondCode.COND_S:
                return JS;
            case CondCode.COND_NS:
                return JNS;
            case CondCode.COND_P:
                return JP;
            case CondCode.COND_NP:
                return JNP;
            case CondCode.COND_O:
                return JO;
            case CondCode.COND_NO:
                return JNO;
        }
    }

    public int getOppositeBranchCondition(int cc)
    {
        switch (cc)
        {
            default:
                Util.shouldNotReachHere("Illegal condition code!");
            case CondCode.COND_E:
                return CondCode.COND_NE;
            case CondCode.COND_NE:
                return CondCode.COND_E;
            case CondCode.COND_L:
                return CondCode.COND_GE;
            case CondCode.COND_LE:
                return CondCode.COND_G;
            case CondCode.COND_G:
                return CondCode.COND_LE;
            case CondCode.COND_GE:
                return CondCode.COND_L;
            case CondCode.COND_B:
                return CondCode.COND_AE
            case CondCode.COND_BE:
                return CondCode.COND_A;
            case CondCode.COND_A:
                return CondCode.COND_BE;
            case CondCode.COND_AE:
                return CondCode.COND_B;
            case CondCode.COND_S:
                return CondCode.COND_NS;
            case CondCode.COND_NS:
                return CondCode.COND_S;
            case CondCode.COND_P:
                return CondCode.COND_NP;
            case CondCode.COND_NP:
                return CondCode.COND_P;
            case CondCode.COND_O:
                return CondCode.COND_NO;
            case CondCode.COND_NO:
                return CondCode.COND_O;
        }
    }

    public boolean isUnpredicatedTerminator(MachineInstr mi)
    {
        TargetInstrDesc tid = mi.getDesc();
        if (!tid.isTerminator())
            return false;

        if (tid.isBranch() && !tid.isBarrier())
            return true;
        if (!tid.isPredicable())
            return true;
        return !isPredicated(mi);
    }

    public static boolean isBrAnalysisUnpredicatedTerminator(
            MachineInstr mi,
            X86InstrInfo ii)
    {
        if (mi.getOpcode() == FP_REG_KILL)
            return false;
        return ii.isUnpredicatedTerminator(mi);
    }

    public boolean analyzeBranch(
            MachineBasicBlock mbb,
            MachineBasicBlock tbb,
            MachineBasicBlock fbb,
            ArrayList<MachineOperand> cond,
            boolean allowModify)
    {
        // Start from the bottom of the block and work up, examining the
        // terminator instructions.
        MachineBasicBlock::iterator I = MBB.end();
        while (I != MBB.begin()) {
            --I;
            // Working from the bottom, when we see a non-terminator
            // instruction, we're done.
            if (!isBrAnalysisUnpredicatedTerminator(I, *this))
            break;
            // A terminator that isn't a branch can't easily be handled
            // by this analysis.
            if (!I->getDesc().isBranch())
                return true;
            // Handle unconditional branches.
            if (I->getOpcode() == X86::JMP) {
                if (!AllowModify) {
                    TBB = I->getOperand(0).getMBB();
                    continue;
                }

                // If the block has any instructions after a JMP, delete them.
                while (next(I) != MBB.end())
                    next(I)->eraseFromParent();
                Cond.clear();
                FBB = 0;
                // Delete the JMP if it's equivalent to a fall-through.
                if (MBB.isLayoutSuccessor(I->getOperand(0).getMBB())) {
                    TBB = 0;
                    I->eraseFromParent();
                    I = MBB.end();
                    continue;
                }
                // TBB is used to indicate the unconditinal destination.
                TBB = I->getOperand(0).getMBB();
                continue;
            }
            // Handle conditional branches.
            X86::CondCode BranchCode = GetCondFromBranchOpc(I->getOpcode());
            if (BranchCode == X86::COND_INVALID)
                return true;  // Can't handle indirect branch.
            // Working from the bottom, handle the first conditional branch.
            if (Cond.empty()) {
                FBB = TBB;
                TBB = I->getOperand(0).getMBB();
                Cond.push_back(MachineOperand::CreateImm(BranchCode));
                continue;
            }
            // Handle subsequent conditional branches. Only handle the case
            // where all conditional branches branch to the same destination
            // and their condition opcodes fit one of the special
            // multi-branch idioms.
            assert(Cond.size() == 1);
            assert(TBB);
            // Only handle the case where all conditional branches branch to
            // the same destination.
            if (TBB != I->getOperand(0).getMBB())
                return true;
            X86::CondCode OldBranchCode = (X86::CondCode)Cond[0].getImm();
            // If the conditions are the same, we can leave them alone.
            if (OldBranchCode == BranchCode)
                continue;
            // If they differ, see if they fit one of the known patterns.
            // Theoretically we could handle more patterns here, but
            // we shouldn't expect to see them if instruction selection
            // has done a reasonable job.
            if ((OldBranchCode == X86::COND_NP &&
                    BranchCode == X86::COND_E) ||
                    (OldBranchCode == X86::COND_E &&
                            BranchCode == X86::COND_NP))
                BranchCode = X86::COND_NP_OR_E;
            else if ((OldBranchCode == X86::COND_P &&
                    BranchCode == X86::COND_NE) ||
                    (OldBranchCode == X86::COND_NE &&
                            BranchCode == X86::COND_P))
                BranchCode = X86::COND_NE_OR_P;
            else
                return true;
            // Update the MachineOperand.
            Cond[0].setImm(BranchCode);
        }

        return false;
    }

    unsigned X86InstrInfo::RemoveBranch(MachineBasicBlock &MBB) const {
    MachineBasicBlock::iterator I = MBB.end();
    unsigned Count = 0;

    while (I != MBB.begin()) {
        --I;
        if (I->getOpcode() != X86::JMP &&
                GetCondFromBranchOpc(I->getOpcode()) == X86::COND_INVALID)
            break;
        // Remove the branch.
        I->eraseFromParent();
        I = MBB.end();
        ++Count;
    }

    return Count;
}

    unsigned
    X86InstrInfo::InsertBranch(MachineBasicBlock &MBB, MachineBasicBlock *TBB,
        MachineBasicBlock *FBB,
                           const SmallVectorImpl<MachineOperand> &Cond) const {
    // FIXME this should probably have a DebugLoc operand
    DebugLoc dl = DebugLoc::getUnknownLoc();
    // Shouldn't be a fall through.
    assert(TBB && "InsertBranch must not be told to insert a fallthrough");
    assert((Cond.size() == 1 || Cond.size() == 0) &&
            "X86 branch conditions have one component!");

    if (Cond.empty()) {
        // Unconditional branch?
        assert(!FBB && "Unconditional branch with multiple successors!");
        BuildMI(&MBB, dl, get(X86::JMP)).addMBB(TBB);
        return 1;
    }

    // Conditional branch.
    unsigned Count = 0;
    X86::CondCode CC = (X86::CondCode)Cond[0].getImm();
    switch (CC) {
        case X86::COND_NP_OR_E:
            // Synthesize NP_OR_E with two branches.
            BuildMI(&MBB, dl, get(X86::JNP)).addMBB(TBB);
            ++Count;
            BuildMI(&MBB, dl, get(X86::JE)).addMBB(TBB);
            ++Count;
            break;
        case X86::COND_NE_OR_P:
            // Synthesize NE_OR_P with two branches.
            BuildMI(&MBB, dl, get(X86::JNE)).addMBB(TBB);
            ++Count;
            BuildMI(&MBB, dl, get(X86::JP)).addMBB(TBB);
            ++Count;
            break;
        default: {
            unsigned Opc = GetCondBranchFromCond(CC);
            BuildMI(&MBB, dl, get(Opc)).addMBB(TBB);
            ++Count;
        }
    }
    if (FBB) {
        // Two-way Conditional branch. Insert the second branch.
        BuildMI(&MBB, dl, get(X86::JMP)).addMBB(FBB);
        ++Count;
    }
    return Count;
}

    /// isHReg - Test if the given register is a physical h register.
    static bool isHReg(unsigned Reg) {
        return X86::GR8_ABCD_HRegClass.contains(Reg);
    }

    bool X86InstrInfo::copyRegToReg(MachineBasicBlock &MBB,
        MachineBasicBlock::iterator MI,
        unsigned DestReg, unsigned SrcReg,
                                const TargetRegisterClass *DestRC,
                                const TargetRegisterClass *SrcRC) const {
    DebugLoc DL = DebugLoc::getUnknownLoc();
    if (MI != MBB.end()) DL = MI->getDebugLoc();

    // Determine if DstRC and SrcRC have a common superclass in common.
  const TargetRegisterClass *CommonRC = DestRC;
    if (DestRC == SrcRC)
    /* Source and destination have the same register class. */;
    else if (CommonRC->hasSuperClass(SrcRC))
        CommonRC = SrcRC;
    else if (!DestRC->hasSubClass(SrcRC)) {
        // Neither of GR64_NOREX or GR64_NOSP is a superclass of the other,
        // but we want to copy then as GR64. Similarly, for GR32_NOREX and
        // GR32_NOSP, copy as GR32.
        if (SrcRC->hasSuperClass(&X86::GR64RegClass) &&
        DestRC->hasSuperClass(&X86::GR64RegClass))
        CommonRC = &X86::GR64RegClass;
    else if (SrcRC->hasSuperClass(&X86::GR32RegClass) &&
        DestRC->hasSuperClass(&X86::GR32RegClass))
        CommonRC = &X86::GR32RegClass;
    else
        CommonRC = 0;
    }

    if (CommonRC) {
        unsigned Opc;
        if (CommonRC == &X86::GR64RegClass || CommonRC == &X86::GR64_NOSPRegClass) {
            Opc = X86::MOV64rr;
        } else if (CommonRC == &X86::GR32RegClass ||
                CommonRC == &X86::GR32_NOSPRegClass) {
            Opc = X86::MOV32rr;
        } else if (CommonRC == &X86::GR16RegClass) {
            Opc = X86::MOV16rr;
        } else if (CommonRC == &X86::GR8RegClass) {
            // Copying to or from a physical H register on x86-64 requires a NOREX
            // move.  Otherwise use a normal move.
            if ((isHReg(DestReg) || isHReg(SrcReg)) &&
                    TM.getSubtarget<X86Subtarget>().is64Bit())
                Opc = X86::MOV8rr_NOREX;
            else
                Opc = X86::MOV8rr;
        } else if (CommonRC == &X86::GR64_ABCDRegClass) {
            Opc = X86::MOV64rr;
        } else if (CommonRC == &X86::GR32_ABCDRegClass) {
            Opc = X86::MOV32rr;
        } else if (CommonRC == &X86::GR16_ABCDRegClass) {
            Opc = X86::MOV16rr;
        } else if (CommonRC == &X86::GR8_ABCD_LRegClass) {
            Opc = X86::MOV8rr;
        } else if (CommonRC == &X86::GR8_ABCD_HRegClass) {
            if (TM.getSubtarget<X86Subtarget>().is64Bit())
                Opc = X86::MOV8rr_NOREX;
            else
                Opc = X86::MOV8rr;
        } else if (CommonRC == &X86::GR64_NOREXRegClass ||
                CommonRC == &X86::GR64_NOREX_NOSPRegClass) {
            Opc = X86::MOV64rr;
        } else if (CommonRC == &X86::GR32_NOREXRegClass) {
            Opc = X86::MOV32rr;
        } else if (CommonRC == &X86::GR16_NOREXRegClass) {
            Opc = X86::MOV16rr;
        } else if (CommonRC == &X86::GR8_NOREXRegClass) {
            Opc = X86::MOV8rr;
        } else if (CommonRC == &X86::RFP32RegClass) {
            Opc = X86::MOV_Fp3232;
        } else if (CommonRC == &X86::RFP64RegClass || CommonRC == &X86::RSTRegClass) {
            Opc = X86::MOV_Fp6464;
        } else if (CommonRC == &X86::RFP80RegClass) {
            Opc = X86::MOV_Fp8080;
        } else if (CommonRC == &X86::FR32RegClass) {
            Opc = X86::FsMOVAPSrr;
        } else if (CommonRC == &X86::FR64RegClass) {
            Opc = X86::FsMOVAPDrr;
        } else if (CommonRC == &X86::VR128RegClass) {
            Opc = X86::MOVAPSrr;
        } else if (CommonRC == &X86::VR64RegClass) {
            Opc = X86::MMX_MOVQ64rr;
        } else {
            return false;
        }
        BuildMI(MBB, MI, DL, get(Opc), DestReg).addReg(SrcReg);
        return true;
    }

    // Moving EFLAGS to / from another register requires a push and a pop.
    if (SrcRC == &X86::CCRRegClass) {
        if (SrcReg != X86::EFLAGS)
            return false;
        if (DestRC == &X86::GR64RegClass || DestRC == &X86::GR64_NOSPRegClass) {
            BuildMI(MBB, MI, DL, get(X86::PUSHFQ));
            BuildMI(MBB, MI, DL, get(X86::POP64r), DestReg);
            return true;
        } else if (DestRC == &X86::GR32RegClass ||
                DestRC == &X86::GR32_NOSPRegClass) {
            BuildMI(MBB, MI, DL, get(X86::PUSHFD));
            BuildMI(MBB, MI, DL, get(X86::POP32r), DestReg);
            return true;
        }
    } else if (DestRC == &X86::CCRRegClass) {
        if (DestReg != X86::EFLAGS)
            return false;
        if (SrcRC == &X86::GR64RegClass || DestRC == &X86::GR64_NOSPRegClass) {
            BuildMI(MBB, MI, DL, get(X86::PUSH64r)).addReg(SrcReg);
            BuildMI(MBB, MI, DL, get(X86::POPFQ));
            return true;
        } else if (SrcRC == &X86::GR32RegClass ||
                DestRC == &X86::GR32_NOSPRegClass) {
            BuildMI(MBB, MI, DL, get(X86::PUSH32r)).addReg(SrcReg);
            BuildMI(MBB, MI, DL, get(X86::POPFD));
            return true;
        }
    }

    // Moving from ST(0) turns into FpGET_ST0_32 etc.
    if (SrcRC == &X86::RSTRegClass) {
        // Copying from ST(0)/ST(1).
        if (SrcReg != X86::ST0 && SrcReg != X86::ST1)
            // Can only copy from ST(0)/ST(1) right now
            return false;
        bool isST0 = SrcReg == X86::ST0;
        unsigned Opc;
        if (DestRC == &X86::RFP32RegClass)
            Opc = isST0 ? X86::FpGET_ST0_32 : X86::FpGET_ST1_32;
        else if (DestRC == &X86::RFP64RegClass)
            Opc = isST0 ? X86::FpGET_ST0_64 : X86::FpGET_ST1_64;
        else {
            if (DestRC != &X86::RFP80RegClass)
                return false;
            Opc = isST0 ? X86::FpGET_ST0_80 : X86::FpGET_ST1_80;
        }
        BuildMI(MBB, MI, DL, get(Opc), DestReg);
        return true;
    }

    // Moving to ST(0) turns into FpSET_ST0_32 etc.
    if (DestRC == &X86::RSTRegClass) {
        // Copying to ST(0) / ST(1).
        if (DestReg != X86::ST0 && DestReg != X86::ST1)
            // Can only copy to TOS right now
            return false;
        bool isST0 = DestReg == X86::ST0;
        unsigned Opc;
        if (SrcRC == &X86::RFP32RegClass)
            Opc = isST0 ? X86::FpSET_ST0_32 : X86::FpSET_ST1_32;
        else if (SrcRC == &X86::RFP64RegClass)
            Opc = isST0 ? X86::FpSET_ST0_64 : X86::FpSET_ST1_64;
        else {
            if (SrcRC != &X86::RFP80RegClass)
                return false;
            Opc = isST0 ? X86::FpSET_ST0_80 : X86::FpSET_ST1_80;
        }
        BuildMI(MBB, MI, DL, get(Opc)).addReg(SrcReg);
        return true;
    }

    // Not yet supported!
    return false;
}

    static unsigned getStoreRegOpcode(unsigned SrcReg,
                                  const TargetRegisterClass *RC,
            bool isStackAligned,
            TargetMachine &TM) {
        unsigned Opc = 0;
        if (RC == &X86::GR64RegClass || RC == &X86::GR64_NOSPRegClass) {
            Opc = X86::MOV64mr;
        } else if (RC == &X86::GR32RegClass || RC == &X86::GR32_NOSPRegClass) {
            Opc = X86::MOV32mr;
        } else if (RC == &X86::GR16RegClass) {
            Opc = X86::MOV16mr;
        } else if (RC == &X86::GR8RegClass) {
            // Copying to or from a physical H register on x86-64 requires a NOREX
            // move.  Otherwise use a normal move.
            if (isHReg(SrcReg) &&
                    TM.getSubtarget<X86Subtarget>().is64Bit())
                Opc = X86::MOV8mr_NOREX;
            else
                Opc = X86::MOV8mr;
        } else if (RC == &X86::GR64_ABCDRegClass) {
            Opc = X86::MOV64mr;
        } else if (RC == &X86::GR32_ABCDRegClass) {
            Opc = X86::MOV32mr;
        } else if (RC == &X86::GR16_ABCDRegClass) {
            Opc = X86::MOV16mr;
        } else if (RC == &X86::GR8_ABCD_LRegClass) {
            Opc = X86::MOV8mr;
        } else if (RC == &X86::GR8_ABCD_HRegClass) {
            if (TM.getSubtarget<X86Subtarget>().is64Bit())
                Opc = X86::MOV8mr_NOREX;
            else
                Opc = X86::MOV8mr;
        } else if (RC == &X86::GR64_NOREXRegClass ||
                RC == &X86::GR64_NOREX_NOSPRegClass) {
            Opc = X86::MOV64mr;
        } else if (RC == &X86::GR32_NOREXRegClass) {
            Opc = X86::MOV32mr;
        } else if (RC == &X86::GR16_NOREXRegClass) {
            Opc = X86::MOV16mr;
        } else if (RC == &X86::GR8_NOREXRegClass) {
            Opc = X86::MOV8mr;
        } else if (RC == &X86::RFP80RegClass) {
            Opc = X86::ST_FpP80m;   // pops
        } else if (RC == &X86::RFP64RegClass) {
            Opc = X86::ST_Fp64m;
        } else if (RC == &X86::RFP32RegClass) {
            Opc = X86::ST_Fp32m;
        } else if (RC == &X86::FR32RegClass) {
            Opc = X86::MOVSSmr;
        } else if (RC == &X86::FR64RegClass) {
            Opc = X86::MOVSDmr;
        } else if (RC == &X86::VR128RegClass) {
            // If stack is realigned we can use aligned stores.
            Opc = isStackAligned ? X86::MOVAPSmr : X86::MOVUPSmr;
        } else if (RC == &X86::VR64RegClass) {
            Opc = X86::MMX_MOVQ64mr;
        } else {
            llvm_unreachable("Unknown regclass");
        }

        return Opc;
    }

    void X86InstrInfo::storeRegToStackSlot(MachineBasicBlock &MBB,
        MachineBasicBlock::iterator MI,
        unsigned SrcReg, bool isKill, int FrameIdx,
                                       const TargetRegisterClass *RC) const {
  const MachineFunction &MF = *MBB.getParent();
    bool isAligned = (RI.getStackAlignment() >= 16) ||
            RI.needsStackRealignment(MF);
    unsigned Opc = getStoreRegOpcode(SrcReg, RC, isAligned, TM);
    DebugLoc DL = DebugLoc::getUnknownLoc();
    if (MI != MBB.end()) DL = MI->getDebugLoc();
    addFrameReference(BuildMI(MBB, MI, DL, get(Opc)), FrameIdx)
            .addReg(SrcReg, getKillRegState(isKill));
}

    void X86InstrInfo::storeRegToAddr(MachineFunction &MF, unsigned SrcReg,
        bool isKill,
        SmallVectorImpl<MachineOperand> &Addr,
                                  const TargetRegisterClass *RC,
        SmallVectorImpl<MachineInstr*> &NewMIs) const {
    bool isAligned = (RI.getStackAlignment() >= 16) ||
            RI.needsStackRealignment(MF);
    unsigned Opc = getStoreRegOpcode(SrcReg, RC, isAligned, TM);
    DebugLoc DL = DebugLoc::getUnknownLoc();
    MachineInstrBuilder MIB = BuildMI(MF, DL, get(Opc));
    for (unsigned i = 0, e = Addr.size(); i != e; ++i)
        MIB.addOperand(Addr[i]);
    MIB.addReg(SrcReg, getKillRegState(isKill));
    NewMIs.push_back(MIB);
}

    static unsigned getLoadRegOpcode(unsigned DestReg,
                                 const TargetRegisterClass *RC,
            bool isStackAligned,
                                 const TargetMachine &TM) {
        unsigned Opc = 0;
        if (RC == &X86::GR64RegClass || RC == &X86::GR64_NOSPRegClass) {
            Opc = X86::MOV64rm;
        } else if (RC == &X86::GR32RegClass || RC == &X86::GR32_NOSPRegClass) {
            Opc = X86::MOV32rm;
        } else if (RC == &X86::GR16RegClass) {
            Opc = X86::MOV16rm;
        } else if (RC == &X86::GR8RegClass) {
            // Copying to or from a physical H register on x86-64 requires a NOREX
            // move.  Otherwise use a normal move.
            if (isHReg(DestReg) &&
                    TM.getSubtarget<X86Subtarget>().is64Bit())
                Opc = X86::MOV8rm_NOREX;
            else
                Opc = X86::MOV8rm;
        } else if (RC == &X86::GR64_ABCDRegClass) {
            Opc = X86::MOV64rm;
        } else if (RC == &X86::GR32_ABCDRegClass) {
            Opc = X86::MOV32rm;
        } else if (RC == &X86::GR16_ABCDRegClass) {
            Opc = X86::MOV16rm;
        } else if (RC == &X86::GR8_ABCD_LRegClass) {
            Opc = X86::MOV8rm;
        } else if (RC == &X86::GR8_ABCD_HRegClass) {
            if (TM.getSubtarget<X86Subtarget>().is64Bit())
                Opc = X86::MOV8rm_NOREX;
            else
                Opc = X86::MOV8rm;
        } else if (RC == &X86::GR64_NOREXRegClass ||
                RC == &X86::GR64_NOREX_NOSPRegClass) {
            Opc = X86::MOV64rm;
        } else if (RC == &X86::GR32_NOREXRegClass) {
            Opc = X86::MOV32rm;
        } else if (RC == &X86::GR16_NOREXRegClass) {
            Opc = X86::MOV16rm;
        } else if (RC == &X86::GR8_NOREXRegClass) {
            Opc = X86::MOV8rm;
        } else if (RC == &X86::RFP80RegClass) {
            Opc = X86::LD_Fp80m;
        } else if (RC == &X86::RFP64RegClass) {
            Opc = X86::LD_Fp64m;
        } else if (RC == &X86::RFP32RegClass) {
            Opc = X86::LD_Fp32m;
        } else if (RC == &X86::FR32RegClass) {
            Opc = X86::MOVSSrm;
        } else if (RC == &X86::FR64RegClass) {
            Opc = X86::MOVSDrm;
        } else if (RC == &X86::VR128RegClass) {
            // If stack is realigned we can use aligned loads.
            Opc = isStackAligned ? X86::MOVAPSrm : X86::MOVUPSrm;
        } else if (RC == &X86::VR64RegClass) {
            Opc = X86::MMX_MOVQ64rm;
        } else {
            llvm_unreachable("Unknown regclass");
        }

        return Opc;
    }

    void X86InstrInfo::loadRegFromStackSlot(MachineBasicBlock &MBB,
        MachineBasicBlock::iterator MI,
        unsigned DestReg, int FrameIdx,
                                        const TargetRegisterClass *RC) const{
  const MachineFunction &MF = *MBB.getParent();
    bool isAligned = (RI.getStackAlignment() >= 16) ||
            RI.needsStackRealignment(MF);
    unsigned Opc = getLoadRegOpcode(DestReg, RC, isAligned, TM);
    DebugLoc DL = DebugLoc::getUnknownLoc();
    if (MI != MBB.end()) DL = MI->getDebugLoc();
    addFrameReference(BuildMI(MBB, MI, DL, get(Opc), DestReg), FrameIdx);
}

    void X86InstrInfo::loadRegFromAddr(MachineFunction &MF, unsigned DestReg,
        SmallVectorImpl<MachineOperand> &Addr,
                                 const TargetRegisterClass *RC,
        SmallVectorImpl<MachineInstr*> &NewMIs) const {
    bool isAligned = (RI.getStackAlignment() >= 16) ||
            RI.needsStackRealignment(MF);
    unsigned Opc = getLoadRegOpcode(DestReg, RC, isAligned, TM);
    DebugLoc DL = DebugLoc::getUnknownLoc();
    MachineInstrBuilder MIB = BuildMI(MF, DL, get(Opc), DestReg);
    for (unsigned i = 0, e = Addr.size(); i != e; ++i)
        MIB.addOperand(Addr[i]);
    NewMIs.push_back(MIB);
}

    bool X86InstrInfo::spillCalleeSavedRegisters(MachineBasicBlock &MBB,
        MachineBasicBlock::iterator MI,
                                const std::vector<CalleeSavedInfo> &CSI) const {
    if (CSI.empty())
        return false;

    DebugLoc DL = DebugLoc::getUnknownLoc();
    if (MI != MBB.end()) DL = MI->getDebugLoc();

    bool is64Bit = TM.getSubtarget<X86Subtarget>().is64Bit();
    bool isWin64 = TM.getSubtarget<X86Subtarget>().isTargetWin64();
    unsigned SlotSize = is64Bit ? 8 : 4;

    MachineFunction &MF = *MBB.getParent();
    unsigned FPReg = RI.getFrameRegister(MF);
    X86MachineFunctionInfo *X86FI = MF.getInfo<X86MachineFunctionInfo>();
    unsigned CalleeFrameSize = 0;

    unsigned Opc = is64Bit ? X86::PUSH64r : X86::PUSH32r;
    for (unsigned i = CSI.size(); i != 0; --i) {
        unsigned Reg = CSI[i-1].getReg();
    const TargetRegisterClass *RegClass = CSI[i-1].getRegClass();
        // Add the callee-saved register as live-in. It's killed at the spill.
        MBB.addLiveIn(Reg);
        if (Reg == FPReg)
            // X86RegisterInfo::emitPrologue will handle spilling of frame register.
            continue;
        if (RegClass != &X86::VR128RegClass && !isWin64) {
            CalleeFrameSize += SlotSize;
            BuildMI(MBB, MI, DL, get(Opc)).addReg(Reg, RegState::Kill);
        } else {
            storeRegToStackSlot(MBB, MI, Reg, true, CSI[i-1].getFrameIdx(), RegClass);
        }
    }

    X86FI->setCalleeSavedFrameSize(CalleeFrameSize);
    return true;
}

    bool X86InstrInfo::restoreCalleeSavedRegisters(MachineBasicBlock &MBB,
        MachineBasicBlock::iterator MI,
                                const std::vector<CalleeSavedInfo> &CSI) const {
    if (CSI.empty())
        return false;

    DebugLoc DL = DebugLoc::getUnknownLoc();
    if (MI != MBB.end()) DL = MI->getDebugLoc();

    MachineFunction &MF = *MBB.getParent();
    unsigned FPReg = RI.getFrameRegister(MF);
    bool is64Bit = TM.getSubtarget<X86Subtarget>().is64Bit();
    bool isWin64 = TM.getSubtarget<X86Subtarget>().isTargetWin64();
    unsigned Opc = is64Bit ? X86::POP64r : X86::POP32r;
    for (unsigned i = 0, e = CSI.size(); i != e; ++i) {
        unsigned Reg = CSI[i].getReg();
        if (Reg == FPReg)
            // X86RegisterInfo::emitEpilogue will handle restoring of frame register.
            continue;
    const TargetRegisterClass *RegClass = CSI[i].getRegClass();
        if (RegClass != &X86::VR128RegClass && !isWin64) {
            BuildMI(MBB, MI, DL, get(Opc), Reg);
        } else {
            loadRegFromStackSlot(MBB, MI, Reg, CSI[i].getFrameIdx(), RegClass);
        }
    }
    return true;
}

    static MachineInstr *FuseTwoAddrInst(MachineFunction &MF, unsigned Opcode,
                                     const SmallVectorImpl<MachineOperand> &MOs,
        MachineInstr *MI,
                                     const TargetInstrInfo &TII) {
    // Create the base instruction with the memory operand as the first part.
    MachineInstr *NewMI = MF.CreateMachineInstr(TII.get(Opcode),
            MI->getDebugLoc(), true);
    MachineInstrBuilder MIB(NewMI);
    unsigned NumAddrOps = MOs.size();
    for (unsigned i = 0; i != NumAddrOps; ++i)
        MIB.addOperand(MOs[i]);
    if (NumAddrOps < 4)  // FrameIndex only
        addOffset(MIB, 0);

    // Loop over the rest of the ri operands, converting them over.
    unsigned NumOps = MI->getDesc().getNumOperands()-2;
    for (unsigned i = 0; i != NumOps; ++i) {
        MachineOperand &MO = MI->getOperand(i+2);
        MIB.addOperand(MO);
    }
    for (unsigned i = NumOps+2, e = MI->getNumOperands(); i != e; ++i) {
        MachineOperand &MO = MI->getOperand(i);
        MIB.addOperand(MO);
    }
    return MIB;
}

    static MachineInstr *FuseInst(MachineFunction &MF,
        unsigned Opcode, unsigned OpNo,
                              const SmallVectorImpl<MachineOperand> &MOs,
        MachineInstr *MI, const TargetInstrInfo &TII) {
    MachineInstr *NewMI = MF.CreateMachineInstr(TII.get(Opcode),
            MI->getDebugLoc(), true);
    MachineInstrBuilder MIB(NewMI);

    for (unsigned i = 0, e = MI->getNumOperands(); i != e; ++i) {
        MachineOperand &MO = MI->getOperand(i);
        if (i == OpNo) {
            assert(MO.isReg() && "Expected to fold into reg operand!");
            unsigned NumAddrOps = MOs.size();
            for (unsigned i = 0; i != NumAddrOps; ++i)
                MIB.addOperand(MOs[i]);
            if (NumAddrOps < 4)  // FrameIndex only
                addOffset(MIB, 0);
        } else {
            MIB.addOperand(MO);
        }
    }
    return MIB;
}

    static MachineInstr *MakeM0Inst(const TargetInstrInfo &TII, unsigned Opcode,
                                const SmallVectorImpl<MachineOperand> &MOs,
        MachineInstr *MI) {
    MachineFunction &MF = *MI->getParent()->getParent();
    MachineInstrBuilder MIB = BuildMI(MF, MI->getDebugLoc(), TII.get(Opcode));

    unsigned NumAddrOps = MOs.size();
    for (unsigned i = 0; i != NumAddrOps; ++i)
        MIB.addOperand(MOs[i]);
    if (NumAddrOps < 4)  // FrameIndex only
        addOffset(MIB, 0);
    return MIB.addImm(0);
}

    MachineInstr*
    X86InstrInfo::foldMemoryOperandImpl(MachineFunction &MF,
        MachineInstr *MI, unsigned i,
                                    const SmallVectorImpl<MachineOperand> &MOs,
        unsigned Align) const {
  const DenseMap<unsigned*, std::pair<unsigned,unsigned> > *OpcodeTablePtr=NULL;
    bool isTwoAddrFold = false;
    unsigned NumOps = MI->getDesc().getNumOperands();
    bool isTwoAddr = NumOps > 1 &&
            MI->getDesc().getOperandConstraint(1, TOI::TIED_TO) != -1;

    MachineInstr *NewMI = NULL;
    // Folding a memory location into the two-address part of a two-address
    // instruction is different than folding it other places.  It requires
    // replacing the *two* registers with the memory location.
    if (isTwoAddr && NumOps >= 2 && i < 2 &&
            MI->getOperand(0).isReg() &&
                    MI->getOperand(1).isReg() &&
                            MI->getOperand(0).getReg() == MI->getOperand(1).getReg()) {
        OpcodeTablePtr = &RegOp2MemOpTable2Addr;
        isTwoAddrFold = true;
    } else if (i == 0) { // If operand 0
        if (MI->getOpcode() == X86::MOV16r0)
            NewMI = MakeM0Inst(*this, X86::MOV16mi, MOs, MI);
    else if (MI->getOpcode() == X86::MOV32r0)
            NewMI = MakeM0Inst(*this, X86::MOV32mi, MOs, MI);
    else if (MI->getOpcode() == X86::MOV8r0)
            NewMI = MakeM0Inst(*this, X86::MOV8mi, MOs, MI);
        if (NewMI)
            return NewMI;

        OpcodeTablePtr = &RegOp2MemOpTable0;
    } else if (i == 1) {
        OpcodeTablePtr = &RegOp2MemOpTable1;
    } else if (i == 2) {
        OpcodeTablePtr = &RegOp2MemOpTable2;
    }

    // If table selected...
    if (OpcodeTablePtr) {
        // Find the Opcode to fuse
        DenseMap<unsigned*, std::pair<unsigned,unsigned> >::iterator I =
                OpcodeTablePtr->find((unsigned*)MI->getOpcode());
        if (I != OpcodeTablePtr->end()) {
            unsigned MinAlign = I->second.second;
            if (Align < MinAlign)
                return NULL;
            if (isTwoAddrFold)
                NewMI = FuseTwoAddrInst(MF, I->second.first, MOs, MI, *this);
      else
            NewMI = FuseInst(MF, I->second.first, i, MOs, MI, *this);
            return NewMI;
        }
    }

    // No fusion
    if (PrintFailedFusing)
        cerr << "We failed to fuse operand " << i << " in " << *MI;
    return NULL;
}


    MachineInstr* X86InstrInfo::foldMemoryOperandImpl(MachineFunction &MF,
        MachineInstr *MI,
                                           const SmallVectorImpl<unsigned> &Ops,
        int FrameIndex) const {
    // Check switch flag
    if (NoFusing) return NULL;

  const MachineFrameInfo *MFI = MF.getFrameInfo();
    unsigned Alignment = MFI->getObjectAlignment(FrameIndex);
    if (Ops.size() == 2 && Ops[0] == 0 && Ops[1] == 1) {
        unsigned NewOpc = 0;
        switch (MI->getOpcode()) {
            default: return NULL;
            case X86::TEST8rr:  NewOpc = X86::CMP8ri; break;
            case X86::TEST16rr: NewOpc = X86::CMP16ri; break;
            case X86::TEST32rr: NewOpc = X86::CMP32ri; break;
            case X86::TEST64rr: NewOpc = X86::CMP64ri32; break;
        }
        // Change to CMPXXri r, 0 first.
        MI->setDesc(get(NewOpc));
        MI->getOperand(1).ChangeToImmediate(0);
    } else if (Ops.size() != 1)
        return NULL;

    SmallVector<MachineOperand,4> MOs;
    MOs.push_back(MachineOperand::CreateFI(FrameIndex));
    return foldMemoryOperandImpl(MF, MI, Ops[0], MOs, Alignment);
}

    MachineInstr* X86InstrInfo::foldMemoryOperandImpl(MachineFunction &MF,
        MachineInstr *MI,
                                           const SmallVectorImpl<unsigned> &Ops,
        MachineInstr *LoadMI) const {
    // Check switch flag
    if (NoFusing) return NULL;

    // Determine the alignment of the load.
    unsigned Alignment = 0;
    if (LoadMI->hasOneMemOperand())
        Alignment = LoadMI->memoperands_begin()->getAlignment();
  else if (LoadMI->getOpcode() == X86::V_SET0 ||
            LoadMI->getOpcode() == X86::V_SETALLONES)
        Alignment = 16;
    if (Ops.size() == 2 && Ops[0] == 0 && Ops[1] == 1) {
        unsigned NewOpc = 0;
        switch (MI->getOpcode()) {
            default: return NULL;
            case X86::TEST8rr:  NewOpc = X86::CMP8ri; break;
            case X86::TEST16rr: NewOpc = X86::CMP16ri; break;
            case X86::TEST32rr: NewOpc = X86::CMP32ri; break;
            case X86::TEST64rr: NewOpc = X86::CMP64ri32; break;
        }
        // Change to CMPXXri r, 0 first.
        MI->setDesc(get(NewOpc));
        MI->getOperand(1).ChangeToImmediate(0);
    } else if (Ops.size() != 1)
        return NULL;

    SmallVector<MachineOperand,X86AddrNumOperands> MOs;
    if (LoadMI->getOpcode() == X86::V_SET0 ||
            LoadMI->getOpcode() == X86::V_SETALLONES) {
        // Folding a V_SET0 or V_SETALLONES as a load, to ease register pressure.
        // Create a constant-pool entry and operands to load from it.

        // x86-32 PIC requires a PIC base register for constant pools.
        unsigned PICBase = 0;
        if (TM.getRelocationModel() == Reloc::PIC_) {
            if (TM.getSubtarget<X86Subtarget>().is64Bit())
                PICBase = X86::RIP;
            else
                // FIXME: PICBase = TM.getInstrInfo()->getGlobalBaseReg(&MF);
                // This doesn't work for several reasons.
                // 1. GlobalBaseReg may have been spilled.
                // 2. It may not be live at MI.
                return false;
        }

        // Create a v4i32 constant-pool entry.
        MachineConstantPool &MCP = *MF.getConstantPool();
    const VectorType *Ty =
                VectorType::get(Type::getInt32Ty(MF.getFunction()->getContext()), 4);
        Constant *C = LoadMI->getOpcode() == X86::V_SET0 ?
                Constant::getNullValue(Ty) :
        Constant::getAllOnesValue(Ty);
        unsigned CPI = MCP.getConstantPoolIndex(C, 16);

        // Create operands to load from the constant pool entry.
        MOs.push_back(MachineOperand::CreateReg(PICBase, false));
        MOs.push_back(MachineOperand::CreateImm(1));
        MOs.push_back(MachineOperand::CreateReg(0, false));
        MOs.push_back(MachineOperand::CreateCPI(CPI, 0));
        MOs.push_back(MachineOperand::CreateReg(0, false));
    } else {
        // Folding a normal load. Just copy the load's address operands.
        unsigned NumOps = LoadMI->getDesc().getNumOperands();
        for (unsigned i = NumOps - X86AddrNumOperands; i != NumOps; ++i)
            MOs.push_back(LoadMI->getOperand(i));
    }
    return foldMemoryOperandImpl(MF, MI, Ops[0], MOs, Alignment);
}


    bool X86InstrInfo::canFoldMemoryOperand(const MachineInstr *MI,
                                  const SmallVectorImpl<unsigned> &Ops) const {
    // Check switch flag
    if (NoFusing) return 0;

    if (Ops.size() == 2 && Ops[0] == 0 && Ops[1] == 1) {
        switch (MI->getOpcode()) {
            default: return false;
            case X86::TEST8rr:
            case X86::TEST16rr:
            case X86::TEST32rr:
            case X86::TEST64rr:
                return true;
        }
    }

    if (Ops.size() != 1)
        return false;

    unsigned OpNum = Ops[0];
    unsigned Opc = MI->getOpcode();
    unsigned NumOps = MI->getDesc().getNumOperands();
    bool isTwoAddr = NumOps > 1 &&
            MI->getDesc().getOperandConstraint(1, TOI::TIED_TO) != -1;

    // Folding a memory location into the two-address part of a two-address
    // instruction is different than folding it other places.  It requires
    // replacing the *two* registers with the memory location.
  const DenseMap<unsigned*, std::pair<unsigned,unsigned> > *OpcodeTablePtr=NULL;
    if (isTwoAddr && NumOps >= 2 && OpNum < 2) {
        OpcodeTablePtr = &RegOp2MemOpTable2Addr;
    } else if (OpNum == 0) { // If operand 0
        switch (Opc) {
            case X86::MOV8r0:
            case X86::MOV16r0:
            case X86::MOV32r0:
                return true;
            default: break;
        }
        OpcodeTablePtr = &RegOp2MemOpTable0;
    } else if (OpNum == 1) {
        OpcodeTablePtr = &RegOp2MemOpTable1;
    } else if (OpNum == 2) {
        OpcodeTablePtr = &RegOp2MemOpTable2;
    }

    if (OpcodeTablePtr) {
        // Find the Opcode to fuse
        DenseMap<unsigned*, std::pair<unsigned,unsigned> >::iterator I =
                OpcodeTablePtr->find((unsigned*)Opc);
        if (I != OpcodeTablePtr->end())
            return true;
    }
    return false;
}

    bool X86InstrInfo::unfoldMemoryOperand(MachineFunction &MF, MachineInstr *MI,
        unsigned Reg, bool UnfoldLoad, bool UnfoldStore,
        SmallVectorImpl<MachineInstr*> &NewMIs) const {
    DenseMap<unsigned*, std::pair<unsigned,unsigned> >::iterator I =
            MemOp2RegOpTable.find((unsigned*)MI->getOpcode());
    if (I == MemOp2RegOpTable.end())
        return false;
    DebugLoc dl = MI->getDebugLoc();
    unsigned Opc = I->second.first;
    unsigned Index = I->second.second & 0xf;
    bool FoldedLoad = I->second.second & (1 << 4);
    bool FoldedStore = I->second.second & (1 << 5);
    if (UnfoldLoad && !FoldedLoad)
        return false;
    UnfoldLoad &= FoldedLoad;
    if (UnfoldStore && !FoldedStore)
        return false;
    UnfoldStore &= FoldedStore;

  const TargetInstrDesc &TID = get(Opc);
  const TargetOperandInfo &TOI = TID.OpInfo[Index];
  const TargetRegisterClass *RC = TOI.getRegClass(&RI);
    SmallVector<MachineOperand, X86AddrNumOperands> AddrOps;
    SmallVector<MachineOperand,2> BeforeOps;
    SmallVector<MachineOperand,2> AfterOps;
    SmallVector<MachineOperand,4> ImpOps;
    for (unsigned i = 0, e = MI->getNumOperands(); i != e; ++i) {
        MachineOperand &Op = MI->getOperand(i);
        if (i >= Index && i < Index + X86AddrNumOperands)
            AddrOps.push_back(Op);
        else if (Op.isReg() && Op.isImplicit())
            ImpOps.push_back(Op);
        else if (i < Index)
            BeforeOps.push_back(Op);
        else if (i > Index)
            AfterOps.push_back(Op);
    }

    // Emit the load instruction.
    if (UnfoldLoad) {
        loadRegFromAddr(MF, Reg, AddrOps, RC, NewMIs);
        if (UnfoldStore) {
            // Address operands cannot be marked isKill.
            for (unsigned i = 1; i != 1 + X86AddrNumOperands; ++i) {
                MachineOperand &MO = NewMIs[0]->getOperand(i);
                if (MO.isReg())
                    MO.setIsKill(false);
            }
        }
    }

    // Emit the data processing instruction.
    MachineInstr *DataMI = MF.CreateMachineInstr(TID, MI->getDebugLoc(), true);
    MachineInstrBuilder MIB(DataMI);

    if (FoldedStore)
        MIB.addReg(Reg, RegState::Define);
    for (unsigned i = 0, e = BeforeOps.size(); i != e; ++i)
        MIB.addOperand(BeforeOps[i]);
    if (FoldedLoad)
        MIB.addReg(Reg);
    for (unsigned i = 0, e = AfterOps.size(); i != e; ++i)
        MIB.addOperand(AfterOps[i]);
    for (unsigned i = 0, e = ImpOps.size(); i != e; ++i) {
        MachineOperand &MO = ImpOps[i];
        MIB.addReg(MO.getReg(),
                getDefRegState(MO.isDef()) |
                        RegState::Implicit |
                        getKillRegState(MO.isKill()) |
                        getDeadRegState(MO.isDead()) |
                        getUndefRegState(MO.isUndef()));
    }
    // Change CMP32ri r, 0 back to TEST32rr r, r, etc.
    unsigned NewOpc = 0;
    switch (DataMI->getOpcode()) {
        default: break;
        case X86::CMP64ri32:
        case X86::CMP32ri:
        case X86::CMP16ri:
        case X86::CMP8ri: {
            MachineOperand &MO0 = DataMI->getOperand(0);
            MachineOperand &MO1 = DataMI->getOperand(1);
            if (MO1.getImm() == 0) {
                switch (DataMI->getOpcode()) {
                    default: break;
                    case X86::CMP64ri32: NewOpc = X86::TEST64rr; break;
                    case X86::CMP32ri:   NewOpc = X86::TEST32rr; break;
                    case X86::CMP16ri:   NewOpc = X86::TEST16rr; break;
                    case X86::CMP8ri:    NewOpc = X86::TEST8rr; break;
                }
                DataMI->setDesc(get(NewOpc));
                MO1.ChangeToRegister(MO0.getReg(), false);
            }
        }
    }
    NewMIs.push_back(DataMI);

    // Emit the store instruction.
    if (UnfoldStore) {
    const TargetRegisterClass *DstRC = TID.OpInfo[0].getRegClass(&RI);
        storeRegToAddr(MF, Reg, true, AddrOps, DstRC, NewMIs);
    }

    return true;
}

    bool
    X86InstrInfo::unfoldMemoryOperand(SelectionDAG &DAG, SDNode *N,
        SmallVectorImpl<SDNode*> &NewNodes) const {
    if (!N->isMachineOpcode())
        return false;

    DenseMap<unsigned*, std::pair<unsigned,unsigned> >::iterator I =
            MemOp2RegOpTable.find((unsigned*)N->getMachineOpcode());
    if (I == MemOp2RegOpTable.end())
        return false;
    unsigned Opc = I->second.first;
    unsigned Index = I->second.second & 0xf;
    bool FoldedLoad = I->second.second & (1 << 4);
    bool FoldedStore = I->second.second & (1 << 5);
  const TargetInstrDesc &TID = get(Opc);
  const TargetRegisterClass *RC = TID.OpInfo[Index].getRegClass(&RI);
    unsigned NumDefs = TID.NumDefs;
    std::vector<SDValue> AddrOps;
    std::vector<SDValue> BeforeOps;
    std::vector<SDValue> AfterOps;
    DebugLoc dl = N->getDebugLoc();
    unsigned NumOps = N->getNumOperands();
    for (unsigned i = 0; i != NumOps-1; ++i) {
        SDValue Op = N->getOperand(i);
        if (i >= Index-NumDefs && i < Index-NumDefs + X86AddrNumOperands)
            AddrOps.push_back(Op);
        else if (i < Index-NumDefs)
            BeforeOps.push_back(Op);
        else if (i > Index-NumDefs)
            AfterOps.push_back(Op);
    }
    SDValue Chain = N->getOperand(NumOps-1);
    AddrOps.push_back(Chain);

    // Emit the load instruction.
    SDNode *Load = 0;
  const MachineFunction &MF = DAG.getMachineFunction();
    if (FoldedLoad) {
        EVT VT = *RC->vt_begin();
        bool isAligned = (RI.getStackAlignment() >= 16) ||
                RI.needsStackRealignment(MF);
        Load = DAG.getTargetNode(getLoadRegOpcode(0, RC, isAligned, TM), dl,
                VT, MVT::Other, &AddrOps[0], AddrOps.size());
        NewNodes.push_back(Load);
    }

    // Emit the data processing instruction.
    std::vector<EVT> VTs;
  const TargetRegisterClass *DstRC = 0;
    if (TID.getNumDefs() > 0) {
        DstRC = TID.OpInfo[0].getRegClass(&RI);
        VTs.push_back(*DstRC->vt_begin());
    }
    for (unsigned i = 0, e = N->getNumValues(); i != e; ++i) {
        EVT VT = N->getValueType(i);
        if (VT != MVT::Other && i >= (unsigned)TID.getNumDefs())
            VTs.push_back(VT);
    }
    if (Load)
        BeforeOps.push_back(SDValue(Load, 0));
    std::copy(AfterOps.begin(), AfterOps.end(), std::back_inserter(BeforeOps));
    SDNode *NewNode= DAG.getTargetNode(Opc, dl, VTs, &BeforeOps[0],
            BeforeOps.size());
    NewNodes.push_back(NewNode);

    // Emit the store instruction.
    if (FoldedStore) {
        AddrOps.pop_back();
        AddrOps.push_back(SDValue(NewNode, 0));
        AddrOps.push_back(Chain);
        bool isAligned = (RI.getStackAlignment() >= 16) ||
                RI.needsStackRealignment(MF);
        SDNode *Store = DAG.getTargetNode(getStoreRegOpcode(0, DstRC,
                isAligned, TM),
                dl, MVT::Other,
                &AddrOps[0], AddrOps.size());
        NewNodes.push_back(Store);
    }

    return true;
}

    unsigned X86InstrInfo::getOpcodeAfterMemoryUnfold(unsigned Opc,
        bool UnfoldLoad, bool UnfoldStore) const {
    DenseMap<unsigned*, std::pair<unsigned,unsigned> >::iterator I =
            MemOp2RegOpTable.find((unsigned*)Opc);
    if (I == MemOp2RegOpTable.end())
        return 0;
    bool FoldedLoad = I->second.second & (1 << 4);
    bool FoldedStore = I->second.second & (1 << 5);
    if (UnfoldLoad && !FoldedLoad)
        return 0;
    if (UnfoldStore && !FoldedStore)
        return 0;
    return I->second.first;
}

    bool X86InstrInfo::BlockHasNoFallThrough(const MachineBasicBlock &MBB) const {
    if (MBB.empty()) return false;

    switch (MBB.back().getOpcode()) {
        case X86::TCRETURNri:
        case X86::TCRETURNdi:
        case X86::RET:     // Return.
        case X86::RETI:
        case X86::TAILJMPd:
        case X86::TAILJMPr:
        case X86::TAILJMPm:
        case X86::JMP:     // Uncond branch.
        case X86::JMP32r:  // Indirect branch.
        case X86::JMP64r:  // Indirect branch (64-bit).
        case X86::JMP32m:  // Indirect branch through mem.
        case X86::JMP64m:  // Indirect branch through mem (64-bit).
            return true;
        default: return false;
    }
}

    bool X86InstrInfo::
    ReverseBranchCondition(SmallVectorImpl<MachineOperand> &Cond) const {
    assert(Cond.size() == 1 && "Invalid X86 branch condition!");
    X86::CondCode CC = static_cast<X86::CondCode>(Cond[0].getImm());
    if (CC == X86::COND_NE_OR_P || CC == X86::COND_NP_OR_E)
        return true;
    Cond[0].setImm(GetOppositeBranchCondition(CC));
    return false;
}

    bool X86InstrInfo::
    isSafeToMoveRegClassDefs(const TargetRegisterClass *RC) const {
    // FIXME: Return false for x87 stack register classes for now. We can't
    // allow any loads of these registers before FpGet_ST0_80.
    return !(RC == &X86::CCRRegClass || RC == &X86::RFP32RegClass ||
            RC == &X86::RFP64RegClass || RC == &X86::RFP80RegClass);
}

    unsigned X86InstrInfo::sizeOfImm(const TargetInstrDesc *Desc) {
    switch (Desc->TSFlags & X86II::ImmMask) {
        case X86II::Imm8:   return 1;
        case X86II::Imm16:  return 2;
        case X86II::Imm32:  return 4;
        case X86II::Imm64:  return 8;
        default: llvm_unreachable("Immediate size not set!");
            return 0;
    }
}

    /// isX86_64ExtendedReg - Is the MachineOperand a x86-64 extended register?
    /// e.g. r8, xmm8, etc.
    bool X86InstrInfo::isX86_64ExtendedReg(const MachineOperand &MO) {
    if (!MO.isReg()) return false;
    switch (MO.getReg()) {
        default: break;
        case X86::R8:    case X86::R9:    case X86::R10:   case X86::R11:
        case X86::R12:   case X86::R13:   case X86::R14:   case X86::R15:
        case X86::R8D:   case X86::R9D:   case X86::R10D:  case X86::R11D:
        case X86::R12D:  case X86::R13D:  case X86::R14D:  case X86::R15D:
        case X86::R8W:   case X86::R9W:   case X86::R10W:  case X86::R11W:
        case X86::R12W:  case X86::R13W:  case X86::R14W:  case X86::R15W:
        case X86::R8B:   case X86::R9B:   case X86::R10B:  case X86::R11B:
        case X86::R12B:  case X86::R13B:  case X86::R14B:  case X86::R15B:
        case X86::XMM8:  case X86::XMM9:  case X86::XMM10: case X86::XMM11:
        case X86::XMM12: case X86::XMM13: case X86::XMM14: case X86::XMM15:
            return true;
    }
    return false;
}


    /// determineREX - Determine if the MachineInstr has to be encoded with a X86-64
    /// REX prefix which specifies 1) 64-bit instructions, 2) non-default operand
    /// size, and 3) use of X86-64 extended registers.
    unsigned X86InstrInfo::determineREX(const MachineInstr &MI) {
    unsigned REX = 0;
  const TargetInstrDesc &Desc = MI.getDesc();

    // Pseudo instructions do not need REX prefix byte.
    if ((Desc.TSFlags & X86II::FormMask) == X86II::Pseudo)
        return 0;
    if (Desc.TSFlags & X86II::REX_W)
        REX |= 1 << 3;

    unsigned NumOps = Desc.getNumOperands();
    if (NumOps) {
        bool isTwoAddr = NumOps > 1 &&
                Desc.getOperandConstraint(1, TOI::TIED_TO) != -1;

        // If it accesses SPL, BPL, SIL, or DIL, then it requires a 0x40 REX prefix.
        unsigned i = isTwoAddr ? 1 : 0;
        for (unsigned e = NumOps; i != e; ++i) {
      const MachineOperand& MO = MI.getOperand(i);
            if (MO.isReg()) {
                unsigned Reg = MO.getReg();
                if (isX86_64NonExtLowByteReg(Reg))
                    REX |= 0x40;
            }
        }

        switch (Desc.TSFlags & X86II::FormMask) {
            case X86II::MRMInitReg:
                if (isX86_64ExtendedReg(MI.getOperand(0)))
                    REX |= (1 << 0) | (1 << 2);
                break;
            case X86II::MRMSrcReg: {
                if (isX86_64ExtendedReg(MI.getOperand(0)))
                    REX |= 1 << 2;
                i = isTwoAddr ? 2 : 1;
                for (unsigned e = NumOps; i != e; ++i) {
        const MachineOperand& MO = MI.getOperand(i);
                    if (isX86_64ExtendedReg(MO))
                        REX |= 1 << 0;
                }
                break;
            }
            case X86II::MRMSrcMem: {
                if (isX86_64ExtendedReg(MI.getOperand(0)))
                    REX |= 1 << 2;
                unsigned Bit = 0;
                i = isTwoAddr ? 2 : 1;
                for (; i != NumOps; ++i) {
        const MachineOperand& MO = MI.getOperand(i);
                    if (MO.isReg()) {
                        if (isX86_64ExtendedReg(MO))
                            REX |= 1 << Bit;
                        Bit++;
                    }
                }
                break;
            }
            case X86II::MRM0m: case X86II::MRM1m:
            case X86II::MRM2m: case X86II::MRM3m:
            case X86II::MRM4m: case X86II::MRM5m:
            case X86II::MRM6m: case X86II::MRM7m:
            case X86II::MRMDestMem: {
                unsigned e = (isTwoAddr ? X86AddrNumOperands+1 : X86AddrNumOperands);
                i = isTwoAddr ? 1 : 0;
                if (NumOps > e && isX86_64ExtendedReg(MI.getOperand(e)))
                    REX |= 1 << 2;
                unsigned Bit = 0;
                for (; i != e; ++i) {
        const MachineOperand& MO = MI.getOperand(i);
                    if (MO.isReg()) {
                        if (isX86_64ExtendedReg(MO))
                            REX |= 1 << Bit;
                        Bit++;
                    }
                }
                break;
            }
            default: {
                if (isX86_64ExtendedReg(MI.getOperand(0)))
                    REX |= 1 << 0;
                i = isTwoAddr ? 2 : 1;
                for (unsigned e = NumOps; i != e; ++i) {
        const MachineOperand& MO = MI.getOperand(i);
                    if (isX86_64ExtendedReg(MO))
                        REX |= 1 << 2;
                }
                break;
            }
        }
    }
    return REX;
}

    /// sizePCRelativeBlockAddress - This method returns the size of a PC
    /// relative block address instruction
    ///
    static unsigned sizePCRelativeBlockAddress() {
        return 4;
    }

    /// sizeGlobalAddress - Give the size of the emission of this global address
    ///
    static unsigned sizeGlobalAddress(bool dword) {
        return dword ? 8 : 4;
    }

    /// sizeConstPoolAddress - Give the size of the emission of this constant
    /// pool address
    ///
    static unsigned sizeConstPoolAddress(bool dword) {
        return dword ? 8 : 4;
    }

    /// sizeExternalSymbolAddress - Give the size of the emission of this external
    /// symbol
    ///
    static unsigned sizeExternalSymbolAddress(bool dword) {
        return dword ? 8 : 4;
    }

    /// sizeJumpTableAddress - Give the size of the emission of this jump
    /// table address
    ///
    static unsigned sizeJumpTableAddress(bool dword) {
        return dword ? 8 : 4;
    }

    static unsigned sizeConstant(unsigned Size) {
        return Size;
    }

    static unsigned sizeRegModRMByte(){
        return 1;
    }

    static unsigned sizeSIBByte(){
        return 1;
    }

    static unsigned getDisplacementFieldSize(const MachineOperand *RelocOp) {
        unsigned FinalSize = 0;
        // If this is a simple integer displacement that doesn't require a relocation.
        if (!RelocOp) {
            FinalSize += sizeConstant(4);
            return FinalSize;
        }

        // Otherwise, this is something that requires a relocation.
        if (RelocOp->isGlobal()) {
            FinalSize += sizeGlobalAddress(false);
        } else if (RelocOp->isCPI()) {
            FinalSize += sizeConstPoolAddress(false);
        } else if (RelocOp->isJTI()) {
            FinalSize += sizeJumpTableAddress(false);
        } else {
            llvm_unreachable("Unknown value to relocate!");
        }
        return FinalSize;
    }

    static unsigned getMemModRMByteSize(const MachineInstr &MI, unsigned Op,
            bool IsPIC, bool Is64BitMode) {
  const MachineOperand &Op3 = MI.getOperand(Op+3);
        int DispVal = 0;
  const MachineOperand *DispForReloc = 0;
        unsigned FinalSize = 0;

        // Figure out what sort of displacement we have to handle here.
        if (Op3.isGlobal()) {
            DispForReloc = &Op3;
        } else if (Op3.isCPI()) {
            if (Is64BitMode || IsPIC) {
                DispForReloc = &Op3;
            } else {
                DispVal = 1;
            }
        } else if (Op3.isJTI()) {
            if (Is64BitMode || IsPIC) {
                DispForReloc = &Op3;
            } else {
                DispVal = 1;
            }
        } else {
            DispVal = 1;
        }

  const MachineOperand &Base     = MI.getOperand(Op);
  const MachineOperand &IndexReg = MI.getOperand(Op+2);

        unsigned BaseReg = Base.getReg();

        // Is a SIB byte needed?
        if ((!Is64BitMode || DispForReloc || BaseReg != 0) &&
                IndexReg.getReg() == 0 &&
                (BaseReg == 0 || X86RegisterInfo::getX86RegNum(BaseReg) != N86::ESP)) {
            if (BaseReg == 0) {  // Just a displacement?
                // Emit special case [disp32] encoding
                ++FinalSize;
                FinalSize += getDisplacementFieldSize(DispForReloc);
            } else {
                unsigned BaseRegNo = X86RegisterInfo::getX86RegNum(BaseReg);
                if (!DispForReloc && DispVal == 0 && BaseRegNo != N86::EBP) {
                    // Emit simple indirect register encoding... [EAX] f.e.
                    ++FinalSize;
                    // Be pessimistic and assume it's a disp32, not a disp8
                } else {
                    // Emit the most general non-SIB encoding: [REG+disp32]
                    ++FinalSize;
                    FinalSize += getDisplacementFieldSize(DispForReloc);
                }
            }

        } else {  // We need a SIB byte, so start by outputting the ModR/M byte first
            assert(IndexReg.getReg() != X86::ESP &&
                    IndexReg.getReg() != X86::RSP && "Cannot use ESP as index reg!");

            bool ForceDisp32 = false;
            if (BaseReg == 0 || DispForReloc) {
                // Emit the normal disp32 encoding.
                ++FinalSize;
                ForceDisp32 = true;
            } else {
                ++FinalSize;
            }

            FinalSize += sizeSIBByte();

            // Do we need to output a displacement?
            if (DispVal != 0 || ForceDisp32) {
                FinalSize += getDisplacementFieldSize(DispForReloc);
            }
        }
        return FinalSize;
    }


    static unsigned GetInstSizeWithDesc(const MachineInstr &MI,
                                    const TargetInstrDesc *Desc,
            bool IsPIC, bool Is64BitMode) {

        unsigned Opcode = Desc->Opcode;
        unsigned FinalSize = 0;

        // Emit the lock opcode prefix as needed.
        if (Desc->TSFlags & X86II::LOCK) ++FinalSize;

        // Emit segment override opcode prefix as needed.
        switch (Desc->TSFlags & X86II::SegOvrMask) {
            case X86II::FS:
            case X86II::GS:
                ++FinalSize;
                break;
            default: llvm_unreachable("Invalid segment!");
            case 0: break;  // No segment override!
        }

        // Emit the repeat opcode prefix as needed.
        if ((Desc->TSFlags & X86II::Op0Mask) == X86II::REP) ++FinalSize;

        // Emit the operand size opcode prefix as needed.
        if (Desc->TSFlags & X86II::OpSize) ++FinalSize;

        // Emit the address size opcode prefix as needed.
        if (Desc->TSFlags & X86II::AdSize) ++FinalSize;

        bool Need0FPrefix = false;
        switch (Desc->TSFlags & X86II::Op0Mask) {
            case X86II::TB:  // Two-byte opcode prefix
            case X86II::T8:  // 0F 38
            case X86II::TA:  // 0F 3A
                Need0FPrefix = true;
                break;
            case X86II::TF: // F2 0F 38
                ++FinalSize;
                Need0FPrefix = true;
                break;
            case X86II::REP: break; // already handled.
            case X86II::XS:   // F3 0F
                ++FinalSize;
                Need0FPrefix = true;
                break;
            case X86II::XD:   // F2 0F
                ++FinalSize;
                Need0FPrefix = true;
                break;
            case X86II::D8: case X86II::D9: case X86II::DA: case X86II::DB:
            case X86II::DC: case X86II::DD: case X86II::DE: case X86II::DF:
                ++FinalSize;
                break; // Two-byte opcode prefix
            default: llvm_unreachable("Invalid prefix!");
            case 0: break;  // No prefix!
        }

        if (Is64BitMode) {
            // REX prefix
            unsigned REX = X86InstrInfo::determineREX(MI);
            if (REX)
                ++FinalSize;
        }

        // 0x0F escape code must be emitted just before the opcode.
        if (Need0FPrefix)
            ++FinalSize;

        switch (Desc->TSFlags & X86II::Op0Mask) {
            case X86II::T8:  // 0F 38
                ++FinalSize;
                break;
            case X86II::TA:  // 0F 3A
                ++FinalSize;
                break;
            case X86II::TF: // F2 0F 38
                ++FinalSize;
                break;
        }

        // If this is a two-address instruction, skip one of the register operands.
        unsigned NumOps = Desc->getNumOperands();
        unsigned CurOp = 0;
        if (NumOps > 1 && Desc->getOperandConstraint(1, TOI::TIED_TO) != -1)
            CurOp++;
        else if (NumOps > 2 && Desc->getOperandConstraint(NumOps-1, TOI::TIED_TO)== 0)
            // Skip the last source operand that is tied_to the dest reg. e.g. LXADD32
            --NumOps;

        switch (Desc->TSFlags & X86II::FormMask) {
            default: llvm_unreachable("Unknown FormMask value in X86 MachineCodeEmitter!");
            case X86II::Pseudo:
                // Remember the current PC offset, this is the PIC relocation
                // base address.
                switch (Opcode) {
                    default:
                        break;
                    case TargetInstrInfo::INLINEASM: {
      const MachineFunction *MF = MI.getParent()->getParent();
      const TargetInstrInfo &TII = *MF->getTarget().getInstrInfo();
                        FinalSize += TII.getInlineAsmLength(MI.getOperand(0).getSymbolName(),
                                *MF->getTarget().getTargetAsmInfo());
                        break;
                    }
                    case TargetInstrInfo::DBG_LABEL:
                    case TargetInstrInfo::EH_LABEL:
                        break;
                    case TargetInstrInfo::IMPLICIT_DEF:
                    case TargetInstrInfo::DECLARE:
                    case X86::DWARF_LOC:
                    case X86::FP_REG_KILL:
                        break;
                    case X86::MOVPC32r: {
                        // This emits the "call" portion of this pseudo instruction.
                        ++FinalSize;
                        FinalSize += sizeConstant(X86InstrInfo::sizeOfImm(Desc));
                        break;
                    }
                }
                CurOp = NumOps;
                break;
            case X86II::RawFrm:
                ++FinalSize;

                if (CurOp != NumOps) {
      const MachineOperand &MO = MI.getOperand(CurOp++);
                    if (MO.isMBB()) {
                        FinalSize += sizePCRelativeBlockAddress();
                    } else if (MO.isGlobal()) {
                        FinalSize += sizeGlobalAddress(false);
                    } else if (MO.isSymbol()) {
                        FinalSize += sizeExternalSymbolAddress(false);
                    } else if (MO.isImm()) {
                        FinalSize += sizeConstant(X86InstrInfo::sizeOfImm(Desc));
                    } else {
                        llvm_unreachable("Unknown RawFrm operand!");
                    }
                }
                break;

            case X86II::AddRegFrm:
                ++FinalSize;
                ++CurOp;

                if (CurOp != NumOps) {
      const MachineOperand &MO1 = MI.getOperand(CurOp++);
                    unsigned Size = X86InstrInfo::sizeOfImm(Desc);
                    if (MO1.isImm())
                        FinalSize += sizeConstant(Size);
                    else {
                        bool dword = false;
                        if (Opcode == X86::MOV64ri)
                            dword = true;
                        if (MO1.isGlobal()) {
                            FinalSize += sizeGlobalAddress(dword);
                        } else if (MO1.isSymbol())
                            FinalSize += sizeExternalSymbolAddress(dword);
                        else if (MO1.isCPI())
                            FinalSize += sizeConstPoolAddress(dword);
                        else if (MO1.isJTI())
                            FinalSize += sizeJumpTableAddress(dword);
                    }
                }
                break;

            case X86II::MRMDestReg: {
                ++FinalSize;
                FinalSize += sizeRegModRMByte();
                CurOp += 2;
                if (CurOp != NumOps) {
                    ++CurOp;
                    FinalSize += sizeConstant(X86InstrInfo::sizeOfImm(Desc));
                }
                break;
            }
            case X86II::MRMDestMem: {
                ++FinalSize;
                FinalSize += getMemModRMByteSize(MI, CurOp, IsPIC, Is64BitMode);
                CurOp +=  X86AddrNumOperands + 1;
                if (CurOp != NumOps) {
                    ++CurOp;
                    FinalSize += sizeConstant(X86InstrInfo::sizeOfImm(Desc));
                }
                break;
            }

            case X86II::MRMSrcReg:
                ++FinalSize;
                FinalSize += sizeRegModRMByte();
                CurOp += 2;
                if (CurOp != NumOps) {
                    ++CurOp;
                    FinalSize += sizeConstant(X86InstrInfo::sizeOfImm(Desc));
                }
                break;

            case X86II::MRMSrcMem: {
                int AddrOperands;
                if (Opcode == X86::LEA64r || Opcode == X86::LEA64_32r ||
                        Opcode == X86::LEA16r || Opcode == X86::LEA32r)
                    AddrOperands = X86AddrNumOperands - 1; // No segment register
                else
                    AddrOperands = X86AddrNumOperands;

                ++FinalSize;
                FinalSize += getMemModRMByteSize(MI, CurOp+1, IsPIC, Is64BitMode);
                CurOp += AddrOperands + 1;
                if (CurOp != NumOps) {
                    ++CurOp;
                    FinalSize += sizeConstant(X86InstrInfo::sizeOfImm(Desc));
                }
                break;
            }

            case X86II::MRM0r: case X86II::MRM1r:
            case X86II::MRM2r: case X86II::MRM3r:
            case X86II::MRM4r: case X86II::MRM5r:
            case X86II::MRM6r: case X86II::MRM7r:
                ++FinalSize;
                if (Desc->getOpcode() == X86::LFENCE ||
                        Desc->getOpcode() == X86::MFENCE) {
                    // Special handling of lfence and mfence;
                    FinalSize += sizeRegModRMByte();
                } else if (Desc->getOpcode() == X86::MONITOR ||
                        Desc->getOpcode() == X86::MWAIT) {
                    // Special handling of monitor and mwait.
                    FinalSize += sizeRegModRMByte() + 1; // +1 for the opcode.
                } else {
                    ++CurOp;
                    FinalSize += sizeRegModRMByte();
                }

                if (CurOp != NumOps) {
      const MachineOperand &MO1 = MI.getOperand(CurOp++);
                    unsigned Size = X86InstrInfo::sizeOfImm(Desc);
                    if (MO1.isImm())
                        FinalSize += sizeConstant(Size);
                    else {
                        bool dword = false;
                        if (Opcode == X86::MOV64ri32)
                            dword = true;
                        if (MO1.isGlobal()) {
                            FinalSize += sizeGlobalAddress(dword);
                        } else if (MO1.isSymbol())
                            FinalSize += sizeExternalSymbolAddress(dword);
                        else if (MO1.isCPI())
                            FinalSize += sizeConstPoolAddress(dword);
                        else if (MO1.isJTI())
                            FinalSize += sizeJumpTableAddress(dword);
                    }
                }
                break;

            case X86II::MRM0m: case X86II::MRM1m:
            case X86II::MRM2m: case X86II::MRM3m:
            case X86II::MRM4m: case X86II::MRM5m:
            case X86II::MRM6m: case X86II::MRM7m: {

                ++FinalSize;
                FinalSize += getMemModRMByteSize(MI, CurOp, IsPIC, Is64BitMode);
                CurOp += X86AddrNumOperands;

                if (CurOp != NumOps) {
      const MachineOperand &MO = MI.getOperand(CurOp++);
                    unsigned Size = X86InstrInfo::sizeOfImm(Desc);
                    if (MO.isImm())
                        FinalSize += sizeConstant(Size);
                    else {
                        bool dword = false;
                        if (Opcode == X86::MOV64mi32)
                            dword = true;
                        if (MO.isGlobal()) {
                            FinalSize += sizeGlobalAddress(dword);
                        } else if (MO.isSymbol())
                            FinalSize += sizeExternalSymbolAddress(dword);
                        else if (MO.isCPI())
                            FinalSize += sizeConstPoolAddress(dword);
                        else if (MO.isJTI())
                            FinalSize += sizeJumpTableAddress(dword);
                    }
                }
                break;
            }

            case X86II::MRMInitReg:
                ++FinalSize;
                // Duplicate register, used by things like MOV8r0 (aka xor reg,reg).
                FinalSize += sizeRegModRMByte();
                ++CurOp;
                break;
        }

        if (!Desc->isVariadic() && CurOp != NumOps) {
            std::string msg;
            raw_string_ostream Msg(msg);
            Msg << "Cannot determine size: " << MI;
            llvm_report_error(Msg.str());
        }


        return FinalSize;
    }


    unsigned X86InstrInfo::GetInstSizeInBytes(const MachineInstr *MI) const {
  const TargetInstrDesc &Desc = MI->getDesc();
    bool IsPIC = TM.getRelocationModel() == Reloc::PIC_;
    bool Is64BitMode = TM.getSubtargetImpl()->is64Bit();
    unsigned Size = GetInstSizeWithDesc(*MI, &Desc, IsPIC, Is64BitMode);
    if (Desc.getOpcode() == X86::MOVPC32r)
        Size += GetInstSizeWithDesc(*MI, &get(X86::POP32r), IsPIC, Is64BitMode);
    return Size;
}

    /// getGlobalBaseReg - Return a virtual register initialized with the
    /// the global base register value. Output instructions required to
    /// initialize the register in the function entry block, if necessary.
    ///
    unsigned X86InstrInfo::getGlobalBaseReg(MachineFunction *MF) const {
    assert(!TM.getSubtarget<X86Subtarget>().is64Bit() &&
            "X86-64 PIC uses RIP relative addressing");

    X86MachineFunctionInfo *X86FI = MF->getInfo<X86MachineFunctionInfo>();
    unsigned GlobalBaseReg = X86FI->getGlobalBaseReg();
    if (GlobalBaseReg != 0)
        return GlobalBaseReg;

    // Insert the set of GlobalBaseReg into the first MBB of the function
    MachineBasicBlock &FirstMBB = MF->front();
    MachineBasicBlock::iterator MBBI = FirstMBB.begin();
    DebugLoc DL = DebugLoc::getUnknownLoc();
    if (MBBI != FirstMBB.end()) DL = MBBI->getDebugLoc();
    MachineRegisterInfo &RegInfo = MF->getRegInfo();
    unsigned PC = RegInfo.createVirtualRegister(X86::GR32RegisterClass);

  const TargetInstrInfo *TII = TM.getInstrInfo();
    // Operand of MovePCtoStack is completely ignored by asm printer. It's
    // only used in JIT code emission as displacement to pc.
    BuildMI(FirstMBB, MBBI, DL, TII->get(X86::MOVPC32r), PC).addImm(0);

    // If we're using vanilla 'GOT' PIC style, we should use relative addressing
    // not to pc, but to _GLOBAL_OFFSET_TABLE_ external.
    if (TM.getSubtarget<X86Subtarget>().isPICStyleGOT()) {
        GlobalBaseReg = RegInfo.createVirtualRegister(X86::GR32RegisterClass);
        // Generate addl $__GLOBAL_OFFSET_TABLE_ + [.-piclabel], %some_register
        BuildMI(FirstMBB, MBBI, DL, TII->get(X86::ADD32ri), GlobalBaseReg)
                .addReg(PC).addExternalSymbol("_GLOBAL_OFFSET_TABLE_", 0,
                X86II::MO_GOT_ABSOLUTE_ADDRESS);
    } else {
        GlobalBaseReg = PC;
    }

    X86FI->setGlobalBaseReg(GlobalBaseReg);
    return GlobalBaseReg;
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
