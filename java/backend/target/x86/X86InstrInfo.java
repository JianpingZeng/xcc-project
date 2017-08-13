package backend.target.x86;

import backend.analysis.LiveVariables;
import backend.codegen.*;
import backend.codegen.MachineOperand.RegState;
import backend.target.*;
import backend.value.GlobalVariable;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import tools.OutParamWrapper;
import tools.Pair;
import tools.Util;
import tools.commandline.BooleanOpt;
import tools.commandline.OptionHiddenApplicator;
import tools.commandline.OptionNameApplicator;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import static backend.codegen.MachineInstrBuilder.*;
import static backend.codegen.MachineOperand.RegState.Define;
import static backend.codegen.MachineOperand.RegState.Kill;
import static backend.support.ErrorHandling.llvmReportError;
import static backend.target.TargetMachine.RelocModel.PIC_;
import static backend.target.TargetOperandInfo.OperandConstraint.TIED_TO;
import static backend.target.x86.CondCode.*;
import static backend.target.x86.X86GenInstrNames.*;
import static backend.target.x86.X86GenRegisterInfo.*;
import static backend.target.x86.X86GenRegisterNames.*;
import static backend.target.x86.X86II.*;
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
                // FIXME: TEST*rr EAX,EAX --. CMP [mem], 0
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
                // FIXME: TEST*rr . swapped operand of TEST*mr.
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
                assert mi.getNumOperands() >= 2 && mi.getOperand(0).isRegister()
                        && mi.getOperand(1)
                        .isRegister() : "invalid register-register move instruction";
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
                        && mi.getOperand(3).isRegister() && mi.getOperand(4).isImm()
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

    private static int x86AddrNumOperands = 5;

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
                        && mi.getOperand(2).isRegister() && mi.getOperand(3).isImm()
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
            case MO_DLLIMPORT:     // dllimport stub.
            case MO_GOTPCREL:  // rip-relative GOT reference.
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
                // Loads from ant pools are trivially rematerializable.
                if (mi.getOperand(1).isRegister() && mi.getOperand(2).isImm() && mi.getOperand(3).isRegister() &&
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
                if (mi.getOperand(2).isImm() && mi.getOperand(3).isRegister()
                        && mi.getOperand(3).getReg() == 0 && !mi.getOperand(4).isRegister())
                {
                    // lea fi#, lea GV, etc. are all rematerializable.
                    if (!mi.getOperand(1).isRegister())
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
                if (!mo.isRegister())
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
     * function.  For example, loading a value from the ant pool or from
     * from the argument area of a function if it does not change.  This should
     * only return true of *all* loads the instruction does are invariant (if it
     * does multiple loads).
     *
     * @param mi
     * @return
     */
    @Override public boolean isInvariantLoad(MachineInstr mi)
    {
        // This code cares about loads from three cases: ant pool entries,
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
            if (mo.isRegister() && mo.isDef() && mo.getReg() == EFLAGS && !mo.isDead())
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
    public MachineInstr convertToThreeAddress(
            MachineBasicBlock mbb,
            int insertPos,
            LiveVariables lv)
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
                            newMI = X86InstrBuilder.addFullAddress(buildMI(get(Opc))
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
            case SHRD16rri8: // A = SHRD16rri8 B, C, I . A = SHLD16rri8 C, B, (16-I)
            case SHLD16rri8: // A = SHLD16rri8 B, C, I . A = SHRD16rri8 C, B, (16-I)
            case SHRD32rri8: // A = SHRD32rri8 B, C, I . A = SHLD32rri8 C, B, (32-I)
            case SHLD32rri8: // A = SHLD32rri8 B, C, I . A = SHRD32rri8 C, B, (32-I)
            case SHRD64rri8: // A = SHRD64rri8 B, C, I . A = SHLD64rri8 C, B, (64-I)
            case SHLD64rri8:{// A = SHLD64rri8 B, C, I . A = SHRD64rri8 C, B, (64-I)
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
                return COND_INVALID;
            case JE:
                return COND_E;
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
            case JNP: return COND_NP;
            case JO:  return CondCode.COND_O;
            case JNO: return CondCode.COND_NO;
        }
    }

    public int getCondBranchFromCond(long cc)
    {
        switch ((int) cc)
        {
            default:
                Util.shouldNotReachHere("Illegal condition code!");
            case  COND_E:
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
            case COND_NP:
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
            case COND_E:
                return CondCode.COND_NE;
            case CondCode.COND_NE:
                return COND_E;
            case CondCode.COND_L:
                return CondCode.COND_GE;
            case CondCode.COND_LE:
                return CondCode.COND_G;
            case CondCode.COND_G:
                return CondCode.COND_LE;
            case CondCode.COND_GE:
                return CondCode.COND_L;
            case CondCode.COND_B:
                return CondCode.COND_AE;
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
                return COND_NP;
            case COND_NP:
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
            OutParamWrapper<MachineBasicBlock> tbb,
            OutParamWrapper<MachineBasicBlock> fbb,
            ArrayList<MachineOperand> cond,
            boolean allowModify)
    {
        // Start from the bottom of the block and work up, examining the
        // terminator instructions.
        int i = mbb.size();
        while (i != 0)
        {
            --i;
            MachineInstr mi = mbb.getInstAt(i);
            // Working from the bottom, when we see a non-terminator
            // instruction, we're done.
            if (!isBrAnalysisUnpredicatedTerminator(mi, this))
                break;
            // A terminator that isn't a branch can't easily be handled
            // by this analysis.
            if (!mi.getDesc().isBranch())
                return true;
            // Handle unconditional branches.
            if (mi.getOpcode() == JMP)
            {
                if (!allowModify) {
                    tbb.set(mi.getOperand(0).getMBB());
                    continue;
                }

                // If the block has any instructions after a JMP, delete them.
                while (i + 1 != mbb.size())
                    mbb.removeInstrAt(i+1);

                cond.clear();
                fbb.set(null);
                // Delete the JMP if it's equivalent to a fall-through.
                if (mbb.isLayoutSuccessor(mi.getOperand(0).getMBB())) {
                    tbb.set(null);
                    mi.removeFromParent();
                    i = mbb.size();
                    continue;
                }
                // TBB is used to indicate the unconditinal destination.
                tbb.set(mi.getOperand(0).getMBB());
                continue;
            }
            // Handle conditional branches.
            int branchCode = getCondFromBranchOpc(mi.getOpcode());
            if (branchCode == COND_INVALID)
                return true;  // Can't handle indirect branch.
            // Working from the bottom, handle the first conditional branch.
            if (cond.isEmpty())
            {
                fbb.set(tbb.get());
                tbb.set(mi.getOperand(0).getMBB());
                cond.add(MachineOperand.createImm(branchCode));
                continue;
            }
            // Handle subsequent conditional branches. Only handle the case
            // where all conditional branches branch to the same destination
            // and their condition opcodes fit one of the special
            // multi-branch idioms.
            assert(cond.size() == 1);
            assert(tbb != null);
            // Only handle the case where all conditional branches branch to
            // the same destination.
            if (!tbb.get().equals(mi.getOperand(0).getMBB()))
                return true;
            long oldBranchCode = cond.get(0).getImm();
            // If the conditions are the same, we can leave them alone.
            if (oldBranchCode == branchCode)
                continue;
            // If they differ, see if they fit one of the known patterns.
            // Theoretically we could handle more patterns here, but
            // we shouldn't expect to see them if instruction selection
            // has done a reasonable job.
            if ((oldBranchCode == COND_NP &&
                    branchCode == COND_E) ||
                    (oldBranchCode == COND_E &&
                            branchCode == COND_NP))
                branchCode = COND_NP_OR_E;
            else if ((oldBranchCode == COND_P &&
                    branchCode == COND_NE) ||
                    (oldBranchCode == COND_NE &&
                            branchCode == COND_P))
                branchCode = COND_NE_OR_P;
            else
                return true;
            // Update the MachineOperand.
            cond.get(0).setImm(branchCode);
        }

        return false;
    }

    public int removeBranch(MachineBasicBlock mbb)
    {
        int i = mbb.size();
        int Count = 0;

        while (i != 0)
        {
            --i;
            if (mbb.getInstAt(i).getOpcode() != JMP &&
                    getCondFromBranchOpc(mbb.getInstAt(i).getOpcode()) == COND_INVALID)
                break;

            // Remove the branch.
            mbb.getInstAt(i).removeFromParent();
            i = mbb.size();
            ++Count;
        }

        return Count;
    }

    public int insertBranch(MachineBasicBlock mbb, MachineBasicBlock tbb,
            MachineBasicBlock fbb, ArrayList<MachineOperand> cond)
    {
        // FIXME this should probably have a DebugLoc operand
        // DebugLoc dl = DebugLoc::getUnknownLoc();
        // Shouldn't be a fall through.
        assert tbb
                != null : "InsertBranch must not be told to insert a fallthrough";
        assert (cond.size() == 1 || cond.size()
                == 0) : "X86 branch conditions have one component!";

        if (cond.isEmpty())
        {
            // Unconditional branch?
            assert fbb
                    == null : "Unconditional branch with multiple successors!";
            buildMI(mbb, get(JMP)).addMBB(tbb);
            return 1;
        }

        // Conditional branch.
        int count = 0;
        long cc = cond.get(0).getImm();
        switch ((int) cc)
        {
            case COND_NP_OR_E:
                // Synthesize NP_OR_E with two branches.
                buildMI(mbb, get(JNP)).addMBB(tbb);
                ++count;
                buildMI(mbb, get(JE)).addMBB(tbb);
                ++count;
                break;
            case COND_NE_OR_P:
                // Synthesize NE_OR_P with two branches.
                buildMI(mbb, get(JNE)).addMBB(tbb);
                ++count;
                buildMI(mbb, get(JP)).addMBB(tbb);
                ++count;
                break;
            default:
            {
                int opc = getCondBranchFromCond(cc);
                buildMI(mbb, get(opc)).addMBB(tbb);
                ++count;
            }
        }
        if (fbb != null)
        {
            // Two-way Conditional branch. Insert the second branch.
            buildMI(mbb, get(JMP)).addMBB(fbb);
            ++count;
        }
        return count;
    }

    /// isHReg - Test if the given register is a physical h register.
    public static boolean isHReg(int Reg)
    {
        return GR8_ABCD_HRegisterClass.contains(Reg);
    }

    public boolean copyRegToReg(MachineBasicBlock mbb, int index, int DestReg,
            int SrcReg, TargetRegisterClass DestRC, TargetRegisterClass SrcRC)
    {
        // DebugLoc DL = DebugLoc::getUnknownLoc();
        //if (MI != mbb.end()) DL = MI.getDebugLoc();

        // Determine if DstRC and SrcRC have a common superclass in common.
        TargetRegisterClass CommonRC = DestRC;
        if (DestRC.equals(SrcRC))
    /* Source and destination have the same register class. */
            ;
        else if (CommonRC.hasSuperClass(SrcRC))
            CommonRC = SrcRC;
        else if (!DestRC.hasSubClass(SrcRC))
        {
            // Neither of GR64_NOREX or GR64_NOSP is a superclass of the other,
            // but we want to copy then as GR64. Similarly, for GR32_NOREX and
            // GR32_NOSP, copy as GR32.
            if (SrcRC.hasSuperClass(GR64RegisterClass) && DestRC
                    .hasSuperClass(GR64RegisterClass))
                CommonRC = GR64RegisterClass;
            else if (SrcRC.hasSuperClass(GR32RegisterClass) && DestRC
                    .hasSuperClass(GR32RegisterClass))
                CommonRC = GR32RegisterClass;
            else
                CommonRC = null;
        }

        if (CommonRC != null)
        {
            int Opc;
            if (CommonRC == GR64RegisterClass
                    || CommonRC == GR64_NOSPRegisterClass)
            {
                Opc = MOV64rr;
            }
            else if (CommonRC == GR32RegisterClass
                    || CommonRC == GR32_NOSPRegisterClass)
            {
                Opc = MOV32rr;
            }
            else if (CommonRC == GR16RegisterClass)
            {
                Opc = MOV16rr;
            }
            else if (CommonRC == GR8RegisterClass)
            {
                // Copying to or from a physical H register on x86-64 requires a NOREX
                // move.  Otherwise use a normal move.
                if ((isHReg(DestReg) || isHReg(SrcReg)) && ((X86Subtarget) tm
                        .getSubtarget()).is64Bit())
                    Opc = MOV8rr_NOREX;
                else
                    Opc = MOV8rr;
            }
            else if (CommonRC == GR64_ABCDRegisterClass)
            {
                Opc = MOV64rr;
            }
            else if (CommonRC == GR32_ABCDRegisterClass)
            {
                Opc = MOV32rr;
            }
            else if (CommonRC == GR16_ABCDRegisterClass)
            {
                Opc = MOV16rr;
            }
            else if (CommonRC == GR8_ABCD_LRegisterClass)
            {
                Opc = MOV8rr;
            }
            else if (CommonRC == GR8_ABCD_HRegisterClass)
            {
                if (((X86Subtarget) tm.getSubtarget()).is64Bit())
                    Opc = MOV8rr_NOREX;
                else
                    Opc = MOV8rr;
            }
            else if (CommonRC == GR64_NOREXRegisterClass
                    || CommonRC == GR64_NOREX_NOSPRegisterClass)
            {
                Opc = MOV64rr;
            }
            else if (CommonRC == GR32_NOREXRegisterClass)
            {
                Opc = MOV32rr;
            }
            else if (CommonRC == GR16_NOREXRegisterClass)
            {
                Opc = MOV16rr;
            }
            else if (CommonRC == GR8_NOREXRegisterClass)
            {
                Opc = MOV8rr;
            }
            else if (CommonRC == RFP32RegisterClass)
            {
                Opc = MOV_Fp3232;
            }
            else if (CommonRC == RFP64RegisterClass
                    || CommonRC == RSTRegisterClass)
            {
                Opc = MOV_Fp6464;
            }
            else if (CommonRC == RFP80RegisterClass)
            {
                Opc = MOV_Fp8080;
            }
            else if (CommonRC == FR32RegisterClass)
            {
                Opc = FsMOVAPSrr;
            }
            else if (CommonRC == FR64RegisterClass)
            {
                Opc = FsMOVAPDrr;
            }
            else if (CommonRC == VR128RegisterClass)
            {
                Opc = MOVAPSrr;
            }
            else if (CommonRC == VR64RegisterClass)
            {
                Opc = MMX_MOVQ64rr;
            }
            else
            {
                return false;
            }
            buildMI(mbb, index, get(Opc), DestReg).addReg(SrcReg);
            return true;
        }

        // Moving EFLAGS to / from another register requires a push and a pop.
        if (SrcRC == CCRRegisterClass)
        {
            if (SrcReg != EFLAGS)
                return false;
            if (DestRC == GR64RegisterClass || DestRC == GR64_NOSPRegisterClass)
            {
                buildMI(mbb, index, get(PUSHFQ));
                buildMI(mbb, index, get(POP64r), DestReg);
                return true;
            }
            else if (DestRC == GR32RegisterClass
                    || DestRC == GR32_NOSPRegisterClass)
            {
                buildMI(mbb, index, get(PUSHFD));
                buildMI(mbb, index, get(POP32r), DestReg);
                return true;
            }
        }
        else if (DestRC == CCRRegisterClass)
        {
            if (DestReg != EFLAGS)
                return false;
            if (SrcRC == GR64RegisterClass || DestRC == GR64_NOSPRegisterClass)
            {
                buildMI(mbb, index, get(PUSH64r)).addReg(SrcReg);
                buildMI(mbb, index, get(POPFQ));
                return true;
            }
            else if (SrcRC == GR32RegisterClass
                    || DestRC == GR32_NOSPRegisterClass)
            {
                buildMI(mbb, index, get(PUSH32r)).addReg(SrcReg);
                buildMI(mbb, index, get(POPFD));
                return true;
            }
        }

        // Moving from ST(0) turns into FpGET_ST0_32 etc.
        if (SrcRC == RSTRegisterClass)
        {
            // Copying from ST(0)/ST(1).
            if (SrcReg != ST0 && SrcReg != ST1)
                // Can only copy from ST(0)/ST(1) right now
                return false;
            boolean isST0 = SrcReg == ST0;
            int Opc;
            if (DestRC == RFP32RegisterClass)
                Opc = isST0 ? FpGET_ST0_32 : FpGET_ST1_32;
            else if (DestRC == RFP64RegisterClass)
                Opc = isST0 ? FpGET_ST0_64 : FpGET_ST1_64;
            else
            {
                if (DestRC != RFP80RegisterClass)
                    return false;
                Opc = isST0 ? FpGET_ST0_80 : FpGET_ST1_80;
            }
            buildMI(mbb, index, get(Opc), DestReg);
            return true;
        }

        // Moving to ST(0) turns into FpSET_ST0_32 etc.
        if (DestRC == RSTRegisterClass)
        {
            // Copying to ST(0) / ST(1).
            if (DestReg != ST0 && DestReg != ST1)
                // Can only copy to TOS right now
                return false;
            boolean isST0 = DestReg == ST0;
            int Opc;
            if (SrcRC == RFP32RegisterClass)
                Opc = isST0 ? FpSET_ST0_32 : FpSET_ST1_32;
            else if (SrcRC == RFP64RegisterClass)
                Opc = isST0 ? FpSET_ST0_64 : FpSET_ST1_64;
            else
            {
                if (SrcRC != RFP80RegisterClass)
                    return false;
                Opc = isST0 ? FpSET_ST0_80 : FpSET_ST1_80;
            }
            buildMI(mbb, index, get(Opc)).addReg(SrcReg);
            return true;
        }

        // Not yet supported!
        return false;
    }

    static int getStoreRegOpcode(int SrcReg, TargetRegisterClass rc,
            boolean isStackAligned, TargetMachine tm)
    {
        int Opc = 0;
        if (rc == GR64RegisterClass || rc == GR64_NOSPRegisterClass)
        {
            Opc = MOV64mr;
        }
        else if (rc == GR32RegisterClass || rc == GR32_NOSPRegisterClass)
        {
            Opc = MOV32mr;
        }
        else if (rc == GR16RegisterClass)
        {
            Opc = MOV16mr;
        }
        else if (rc == GR8RegisterClass)
        {
            // Copying to or from a physical H register on x86-64 requires a NOREX
            // move.  Otherwise use a normal move.
            if (isHReg(SrcReg) && ((X86Subtarget) tm.getSubtarget()).is64Bit())
                Opc = MOV8mr_NOREX;
            else
                Opc = MOV8mr;
        }
        else if (rc == GR64_ABCDRegisterClass)
        {
            Opc = MOV64mr;
        }
        else if (rc == GR32_ABCDRegisterClass)
        {
            Opc = MOV32mr;
        }
        else if (rc == GR16_ABCDRegisterClass)
        {
            Opc = MOV16mr;
        }
        else if (rc == GR8_ABCD_LRegisterClass)
        {
            Opc = MOV8mr;
        }
        else if (rc == GR8_ABCD_HRegisterClass)
        {
            if (((X86Subtarget) tm.getSubtarget()).is64Bit())
                Opc = MOV8mr_NOREX;
            else
                Opc = MOV8mr;
        }
        else if (rc == GR64_NOREXRegisterClass
                || rc == GR64_NOREX_NOSPRegisterClass)
        {
            Opc = MOV64mr;
        }
        else if (rc == GR32_NOREXRegisterClass)
        {
            Opc = MOV32mr;
        }
        else if (rc == GR16_NOREXRegisterClass)
        {
            Opc = MOV16mr;
        }
        else if (rc == GR8_NOREXRegisterClass)
        {
            Opc = MOV8mr;
        }
        else if (rc == RFP80RegisterClass)
        {
            Opc = ST_FpP80m;   // pops
        }
        else if (rc == RFP64RegisterClass)
        {
            Opc = ST_Fp64m;
        }
        else if (rc == RFP32RegisterClass)
        {
            Opc = ST_Fp32m;
        }
        else if (rc == FR32RegisterClass)
        {
            Opc = MOVSSmr;
        }
        else if (rc == FR64RegisterClass)
        {
            Opc = MOVSDmr;
        }
        else if (rc == VR128RegisterClass)
        {
            // If stack is realigned we can use aligned stores.
            Opc = isStackAligned ? MOVAPSmr : MOVUPSmr;
        }
        else if (rc == VR64RegisterClass)
        {
            Opc = MMX_MOVQ64mr;
        }
        else
        {
            Util.shouldNotReachHere("Unknown regclass");
        }

        return Opc;
    }

    public void storeRegToStackSlot(MachineBasicBlock mbb, int index,
            int SrcReg, boolean isKill, int FrameIdx, TargetRegisterClass rc)
    {
        MachineFunction MF = mbb.getParent();
        boolean isAligned =
                (registerInfo.getStackAlignment() >= 16) || registerInfo
                        .needsStackRealignment(MF);
        int Opc = getStoreRegOpcode(SrcReg, rc, isAligned, tm);
        //DebugLoc DL = DebugLoc::getUnknownLoc();
        //if (MI != mbb.end()) DL = MI.getDebugLoc();
        addFrameReference(buildMI(mbb, index, get(Opc)), FrameIdx)
                .addReg(SrcReg, getKillRegState(isKill));
    }

    void storeRegToAddr(MachineFunction MF, int SrcReg, boolean isKill,
            ArrayList<MachineOperand> Addr, TargetRegisterClass rc,
            ArrayList<MachineInstr> NewMIs)
    {
        boolean isAligned =
                (registerInfo.getStackAlignment() >= 16) || registerInfo
                        .needsStackRealignment(MF);
        int Opc = getStoreRegOpcode(SrcReg, rc, isAligned, tm);
        //DebugLoc DL = DebugLoc::getUnknownLoc();
        MachineInstrBuilder MIB = buildMI(get(Opc));
        for (int i = 0, e = Addr.size(); i != e; ++i)
            MIB.addOperand(Addr.get(i));
        MIB.addReg(SrcReg, getKillRegState(isKill));
        NewMIs.add(MIB.getMInstr());
    }

    static int getLoadRegOpcode(int DestReg, TargetRegisterClass rc,
            boolean isStackAligned, TargetMachine tm)
    {
        int Opc = 0;
        if (rc == GR64RegisterClass || rc == GR64_NOSPRegisterClass)
        {
            Opc = MOV64rm;
        }
        else if (rc == GR32RegisterClass || rc == GR32_NOSPRegisterClass)
        {
            Opc = MOV32rm;
        }
        else if (rc == GR16RegisterClass)
        {
            Opc = MOV16rm;
        }
        else if (rc == GR8RegisterClass)
        {
            // Copying to or from a physical H register on x86-64 requires a NOREX
            // move.  Otherwise use a normal move.
            if (isHReg(DestReg) && ((X86Subtarget) tm.getSubtarget()).is64Bit())
                Opc = MOV8rm_NOREX;
            else
                Opc = MOV8rm;
        }
        else if (rc == GR64_ABCDRegisterClass)
        {
            Opc = MOV64rm;
        }
        else if (rc == GR32_ABCDRegisterClass)
        {
            Opc = MOV32rm;
        }
        else if (rc == GR16_ABCDRegisterClass)
        {
            Opc = MOV16rm;
        }
        else if (rc == GR8_ABCD_LRegisterClass)
        {
            Opc = MOV8rm;
        }
        else if (rc == GR8_ABCD_HRegisterClass)
        {
            if (((X86Subtarget) tm.getSubtarget()).is64Bit())
                Opc = MOV8rm_NOREX;
            else
                Opc = MOV8rm;
        }
        else if (rc == GR64_NOREXRegisterClass
                || rc == GR64_NOREX_NOSPRegisterClass)
        {
            Opc = MOV64rm;
        }
        else if (rc == GR32_NOREXRegisterClass)
        {
            Opc = MOV32rm;
        }
        else if (rc == GR16_NOREXRegisterClass)
        {
            Opc = MOV16rm;
        }
        else if (rc == GR8_NOREXRegisterClass)
        {
            Opc = MOV8rm;
        }
        else if (rc == RFP80RegisterClass)
        {
            Opc = LD_Fp80m;
        }
        else if (rc == RFP64RegisterClass)
        {
            Opc = LD_Fp64m;
        }
        else if (rc == RFP32RegisterClass)
        {
            Opc = LD_Fp32m;
        }
        else if (rc == FR32RegisterClass)
        {
            Opc = MOVSSrm;
        }
        else if (rc == FR64RegisterClass)
        {
            Opc = MOVSDrm;
        }
        else if (rc == VR128RegisterClass)
        {
            // If stack is realigned we can use aligned loads.
            Opc = isStackAligned ? MOVAPSrm : MOVUPSrm;
        }
        else if (rc == VR64RegisterClass)
        {
            Opc = MMX_MOVQ64rm;
        }
        else
        {
            Util.shouldNotReachHere("Unknown regclass");
        }

        return Opc;
    }

    public void loadRegFromStackSlot(MachineBasicBlock mbb, int index,
            int DestReg, int FrameIdx, TargetRegisterClass rc)
    {
        MachineFunction mf = mbb.getParent();
        boolean isAligned =
                (registerInfo.getStackAlignment() >= 16) || registerInfo
                        .needsStackRealignment(mf);
        int opc = getLoadRegOpcode(DestReg, rc, isAligned, tm);
        //DebugLoc DL = DebugLoc::getUnknownLoc();
        //if (index != mbb.size()) DL = MI.getDebugLoc();
        addFrameReference(buildMI(mbb, index, get(opc), DestReg), FrameIdx);
    }

    public void loadRegFromAddr(MachineFunction MF, int DestReg,
            ArrayList<MachineOperand> Addr, TargetRegisterClass rc,
            ArrayList<MachineInstr> NewMIs)
    {
        boolean isAligned =
                (registerInfo.getStackAlignment() >= 16) || registerInfo
                        .needsStackRealignment(MF);
        int Opc = getLoadRegOpcode(DestReg, rc, isAligned, tm);
        // DebugLoc DL = DebugLoc::getUnknownLoc();
        MachineInstrBuilder MIB = buildMI(get(Opc), DestReg);
        for (int i = 0, e = Addr.size(); i != e; ++i)
            MIB.addOperand(Addr.get(i));
        NewMIs.add(MIB.getMInstr());
    }

    public boolean spillCalleeSavedRegisters(
            MachineBasicBlock mbb,
            int index,
            ArrayList<CalleeSavedInfo> csi)
    {
        if (csi.isEmpty())
            return false;

        //DebugLoc DL = DebugLoc::getUnknownLoc();
        //if (MI != mbb.end()) DL = MI.getDebugLoc();

        boolean is64Bit = tm.getSubtarget().is64Bit();
        boolean isWin64 = tm.getSubtarget().isTargetWin64();
        int slotSize = is64Bit ? 8 : 4;

        MachineFunction mf = mbb.getParent();
        int fpReg = registerInfo.getFrameRegister(mf);
        X86MachineFunctionInfo X86FI = (X86MachineFunctionInfo)mf.getInfo();
        int CalleeFrameSize = 0;

        int Opc = is64Bit ? PUSH64r : PUSH32r;
        for (int i = csi.size(); i != 0; --i)
        {
            int Reg = csi.get(i - 1).getReg();
            TargetRegisterClass RegisterClass = csi.get(i - 1)
                    .getRegisterClass();
            // Add the callee-saved register as live-in. It's killed at the spill.
            mbb.addLiveIn(Reg);
            if (Reg == fpReg)
                // X86RegisterInfo::emitPrologue will handle spilling of frame register.
                continue;
            if (RegisterClass != VR128RegisterClass && !isWin64)
            {
                CalleeFrameSize += slotSize;
                buildMI(mbb, index, get(Opc)).addReg(Reg, RegState.Kill);
            }
            else
            {
                storeRegToStackSlot(mbb, index, Reg, true,
                        csi.get(i - 1).getFrameIdx(), RegisterClass);
            }
        }

        X86FI.setCalleeSavedFrameSize(CalleeFrameSize);
        return true;
    }

    public boolean restoreCalleeSavedRegisters(MachineBasicBlock mbb, int index,
            ArrayList<CalleeSavedInfo> CSI)
    {
        if (CSI.isEmpty())
            return false;

        //DebugLoc DL = DebugLoc::getUnknownLoc();
        //if (MI != mbb.end()) DL = MI.getDebugLoc();

        MachineFunction MF = mbb.getParent();
        int FPReg = registerInfo.getFrameRegister(MF);
        boolean is64Bit = ((X86Subtarget) tm.getSubtarget()).is64Bit();
        boolean isWin64 = ((X86Subtarget) tm.getSubtarget()).isTargetWin64();
        int Opc = is64Bit ? POP64r : POP32r;
        for (int i = 0, e = CSI.size(); i != e; ++i)
        {
            int Reg = CSI.get(i).getReg();
            if (Reg == FPReg)
                // X86RegisterInfo::emitEpilogue will handle restoring of frame register.
                continue;
            TargetRegisterClass RegisterClass = CSI.get(i).getRegisterClass();
            if (RegisterClass != VR128RegisterClass && !isWin64)
            {
                buildMI(mbb, index, get(Opc), Reg);
            }
            else
            {
                loadRegFromStackSlot(mbb, index, Reg, CSI.get(i).getFrameIdx(),
                        RegisterClass);
            }
        }
        return true;
    }

    static MachineInstr FuseTwoAddrInst(MachineFunction MF, int Opcode,
            ArrayList<MachineOperand> MOs, MachineInstr MI, TargetInstrInfo TII)
    {
        // Create the base instruction with the memory operand as the first part.
        MachineInstrBuilder MIB = buildMI(TII.get(Opcode));

        int NumAddrOps = MOs.size();
        for (int i = 0; i != NumAddrOps; ++i)
            MIB.addOperand(MOs.get(i));
        if (NumAddrOps < 4)  // FrameIndex only
            addOffset(MIB, 0);

        // Loop over the rest of the ri operands, converting them over.
        int NumOps = MI.getDesc().getNumOperands() - 2;
        for (int i = 0; i != NumOps; ++i)
        {
            MachineOperand MO = MI.getOperand(i + 2);
            MIB.addOperand(MO);
        }
        for (int i = NumOps + 2, e = MI.getNumOperands(); i != e; ++i)
        {
            MachineOperand MO = MI.getOperand(i);
            MIB.addOperand(MO);
        }
        return MIB.getMInstr();
    }

    static MachineInstr FuseInst(MachineFunction MF, int Opcode, int OpNo,
            ArrayList<MachineOperand> MOs, MachineInstr MI, TargetInstrInfo TII)
    {
        MachineInstrBuilder MIB = buildMI(TII.get(Opcode));

        for (int i = 0, e = MI.getNumOperands(); i != e; ++i)
        {
            MachineOperand MO = MI.getOperand(i);
            if (i == OpNo)
            {
                assert MO.isRegister() : "Expected to fold into reg operand!";
                int NumAddrOps = MOs.size();
                for (int j = 0; j != NumAddrOps; ++j)
                    MIB.addOperand(MOs.get(j));
                if (NumAddrOps < 4)  // FrameIndex only
                    addOffset(MIB, 0);
            }
            else
            {
                MIB.addOperand(MO);
            }
        }
        return MIB.getMInstr();
    }

    static MachineInstr MakeM0Inst(TargetInstrInfo TII, int Opcode,
            ArrayList<MachineOperand> MOs, MachineInstr MI)
    {
        MachineFunction MF = MI.getParent().getParent();
        MachineInstrBuilder MIB = buildMI(TII.get(Opcode));

        int NumAddrOps = MOs.size();
        for (int i = 0; i != NumAddrOps; ++i)
            MIB.addOperand(MOs.get(i));
        if (NumAddrOps < 4)  // FrameIndex only
            addOffset(MIB, 0);
        return MIB.addImm(0).getMInstr();
    }

    @Override
    protected MachineInstr foldMemoryOperandImpl(MachineFunction MF,
            MachineInstr MI, int i, ArrayList<MachineOperand> MOs, int Align)
    {
        TIntObjectHashMap<Pair<Integer, Integer>> OpcodeTablePtr = null;
        boolean isTwoAddrFold = false;
        int NumOps = MI.getDesc().getNumOperands();
        boolean isTwoAddr = NumOps > 1
                && MI.getDesc().getOperandConstraint(1, TIED_TO) != -1;

        MachineInstr NewMI = null;
        // Folding a memory location into the two-address part of a two-address
        // instruction is different than folding it other places.  It requires
        // replacing the *two* registers with the memory location.
        if (isTwoAddr && NumOps >= 2 && i < 2 && MI.getOperand(0).isRegister() && MI
                .getOperand(1).isRegister() && MI.getOperand(0).getReg() == MI
                .getOperand(1).getReg())
        {
            OpcodeTablePtr = regOp2MemOpTable2Addr;
            isTwoAddrFold = true;
        }
        else if (i == 0)
        { // If operand 0
            if (MI.getOpcode() == MOV16r0)
                NewMI = MakeM0Inst(this, MOV16mi, MOs, MI);
            else if (MI.getOpcode() == MOV32r0)
                NewMI = MakeM0Inst(this, MOV32mi, MOs, MI);
            else if (MI.getOpcode() == MOV8r0)
                NewMI = MakeM0Inst(this, MOV8mi, MOs, MI);
            if (NewMI != null)
                return NewMI;

            OpcodeTablePtr = regOp2MemOpTable0;
        }
        else if (i == 1)
        {
            OpcodeTablePtr = regOp2MemOpTable1;
        }
        else if (i == 2)
        {
            OpcodeTablePtr = regOp2MemOpTable2;
        }

        // If table selected...
        if (OpcodeTablePtr != null)
        {
            // Find the Opcode to fuse
            if (OpcodeTablePtr.containsKey(MI.getOpcode()))
            {
                int MinAlign = OpcodeTablePtr.get(MI.getOpcode()).second;
                if (Align < MinAlign)
                    return null;
                if (isTwoAddrFold)
                    NewMI = FuseTwoAddrInst(MF,
                            OpcodeTablePtr.get(MI.getOpcode()).first, MOs, MI,
                            this);
                else
                    NewMI = FuseInst(MF,
                            OpcodeTablePtr.get(MI.getOpcode()).first, i, MOs,
                            MI, this);
                return NewMI;
            }
        }

        // No fusion
        if (PrintFailedFusing.value)
        {
            System.err.printf("We failed to fuse operand %d in ", i);
            MI.print(System.err, null);
        }
        return null;
    }

    public MachineInstr foldMemoryOperandImpl(MachineFunction MF,
            MachineInstr MI, TIntArrayList Ops, int FrameIndex)
    {
        // Check switch flag
        if (NoFusing.value)
            return null;

        MachineFrameInfo MFI = MF.getFrameInfo();
        int Alignment = MFI.getObjectAlignment(FrameIndex);
        if (Ops.size() == 2 && Ops.get(0) == 0 && Ops.get(1) == 1)
        {
            int NewOpc = 0;
            switch (MI.getOpcode())
            {
                default:
                    return null;
                case TEST8rr:
                    NewOpc = CMP8ri;
                    break;
                case TEST16rr:
                    NewOpc = CMP16ri;
                    break;
                case TEST32rr:
                    NewOpc = CMP32ri;
                    break;
                case TEST64rr:
                    NewOpc = CMP64ri32;
                    break;
            }
            // Change to CMPXXri r, 0 first.
            MI.setDesc(get(NewOpc));
            MI.getOperand(1).changeToImmediate(0);
        }
        else if (Ops.size() != 1)
            return null;

        ArrayList<MachineOperand> MOs = new ArrayList<>();
        MOs.add(MachineOperand.createFrameIndex(FrameIndex));
        return foldMemoryOperandImpl(MF, MI, Ops.get(0), MOs, Alignment);
    }

    public MachineInstr foldMemoryOperandImpl(MachineFunction MF,
            MachineInstr MI, TIntArrayList Ops, MachineInstr LoadMI)
    {
        // Check switch flag
        if (NoFusing.value)
            return null;

        // Determine the alignment of the load.
        int Alignment = 0;
        if (LoadMI.hasOneMemOperand())
            Alignment = LoadMI.getMemOperand(0).getAlignment();
        else if (LoadMI.getOpcode() == V_SET0
                || LoadMI.getOpcode() == V_SETALLONES)
            Alignment = 16;
        if (Ops.size() == 2 && Ops.get(0) == 0 && Ops.get(1) == 1)
        {
            int NewOpc = 0;
            switch (MI.getOpcode())
            {
                default:
                    return null;
                case TEST8rr:
                    NewOpc = CMP8ri;
                    break;
                case TEST16rr:
                    NewOpc = CMP16ri;
                    break;
                case TEST32rr:
                    NewOpc = CMP32ri;
                    break;
                case TEST64rr:
                    NewOpc = CMP64ri32;
                    break;
            }
            // Change to CMPXXri r, 0 first.
            MI.setDesc(get(NewOpc));
            MI.getOperand(1).changeToImmediate(0);
        }
        else if (Ops.size() != 1)
            return null;

        ArrayList<MachineOperand> MOs = new ArrayList<>();
        if (LoadMI.getOpcode() == V_SET0 || LoadMI.getOpcode() == V_SETALLONES)
        {
            // Folding a V_SET0 or V_SETALLONES as a load, to ease register pressure.
            // Create a ant-pool entry and operands to load from it.

            // x86-32 PIC requires a PIC base register for ant pools.
            int PICBase = 0;
            if (tm.getRelocationModel() == PIC_)
            {
                if (tm.getSubtarget().is64Bit())
                    PICBase = RIP;
                else
                    // FIXME: PICBase = tm.getInstrInfo().getGlobalBaseReg(MF);
                    // This doesn't work for several reasons.
                    // 1. globalBaseReg may have been spilled.
                    // 2. It may not be live at MI.
                    return null;
            }

            Util.shouldNotReachHere("vector type is not supported!");
        /*
        // Create a v4i32 ant-pool entry.
        MachineConstantPool MCP = MF.getConstants();
     VectorType Ty =
                VectorType::get(Type::getInt32Ty(MF.getFunction().getContext()), 4);
        Constant *C = LoadMI.getOpcode() == V_SET0 ?
                Constant::getNullValue(Ty) :
        Constant::getAllOnesValue(Ty);
        int CPI = MCP.getConstantPoolIndex(C, 16);

        // Create operands to load from the ant pool entry.
        MOs.add(MachineOperand::CreateReg(PICBase, false));
        MOs.add(MachineOperand::CreateImm(1));
        MOs.add(MachineOperand::CreateReg(0, false));
        MOs.add(MachineOperand::CreateCPI(CPI, 0));
        MOs.add(MachineOperand::CreateReg(0, false));
        */
        }
        else
        {
            // Folding a normal load. Just copy the load's address operands.
            int NumOps = LoadMI.getDesc().getNumOperands();
            for (int i = NumOps - x86AddrNumOperands; i != NumOps; ++i)
                MOs.add(LoadMI.getOperand(i));
        }
        return foldMemoryOperandImpl(MF, MI, Ops.get(0), MOs, Alignment);
    }

    public boolean canFoldMemoryOperand(MachineInstr MI, TIntArrayList Ops)
    {
        // Check switch flag
        if (NoFusing.value)
            return false;

        if (Ops.size() == 2 && Ops.get(0) == 0 && Ops.get(1) == 1)
        {
            switch (MI.getOpcode())
            {
                default:
                    return false;
                case TEST8rr:
                case TEST16rr:
                case TEST32rr:
                case TEST64rr:
                    return true;
            }
        }

        if (Ops.size() != 1)
            return false;

        int OpNum = Ops.get(0);
        int Opc = MI.getOpcode();
        int NumOps = MI.getDesc().getNumOperands();
        boolean isTwoAddr = NumOps > 1
                && MI.getDesc().getOperandConstraint(1, TIED_TO) != -1;

        // Folding a memory location into the two-address part of a two-address
        // instruction is different than folding it other places.  It requires
        // replacing the *two* registers with the memory location.
        TIntObjectHashMap<Pair<Integer, Integer>> OpcodeTablePtr = null;
        if (isTwoAddr && NumOps >= 2 && OpNum < 2)
        {
            OpcodeTablePtr = regOp2MemOpTable2Addr;
        }
        else if (OpNum == 0)
        { // If operand 0
            switch (Opc)
            {
                case MOV8r0:
                case MOV16r0:
                case MOV32r0:
                    return true;
                default:
                    break;
            }
            OpcodeTablePtr = regOp2MemOpTable0;
        }
        else if (OpNum == 1)
        {
            OpcodeTablePtr = regOp2MemOpTable1;
        }
        else if (OpNum == 2)
        {
            OpcodeTablePtr = regOp2MemOpTable2;
        }

        if (OpcodeTablePtr != null)
        {
            // Find the Opcode to fuse

            if (OpcodeTablePtr.containsKey(Opc))
                return true;
        }
        return false;
    }

    public boolean unfoldMemoryOperand(MachineFunction MF, MachineInstr MI,
            int Reg, boolean UnfoldLoad, boolean UnfoldStore,
            ArrayList<MachineInstr> NewMIs)
    {

        if (!memOp2RegOpTable.containsKey(MI.getOpcode()))
            return false;
        Pair<Integer, Integer> pair = memOp2RegOpTable.get(MI.getOpcode());
        //DebugLoc dl = MI.getDebugLoc();
        int Opc = pair.first;
        int index = pair.second & 0xf;
        boolean FoldedLoad = (pair.second & (1 << 4)) != 0;
        boolean FoldedStore = (pair.second & (1 << 5)) != 0;
        if (UnfoldLoad && !FoldedLoad)
            return false;
        UnfoldLoad &= FoldedLoad;
        if (UnfoldStore && !FoldedStore)
            return false;
        UnfoldStore &= FoldedStore;

        TargetInstrDesc TID = get(Opc);
        TargetOperandInfo TOI = TID.opInfo[index];
        TargetRegisterClass rc = TOI.getRegisterClass(registerInfo);
        ArrayList<MachineOperand> AddrOps = new ArrayList<>();
        ArrayList<MachineOperand> BeforeOps = new ArrayList<>();
        ArrayList<MachineOperand> AfterOps = new ArrayList<>();
        ArrayList<MachineOperand> ImpOps = new ArrayList<>();
        for (int i = 0, e = MI.getNumOperands(); i != e; ++i)
        {
            MachineOperand Op = MI.getOperand(i);
            if (i >= index && i < index + x86AddrNumOperands)
                AddrOps.add(Op);
            else if (Op.isRegister() && Op.isImplicit())
                ImpOps.add(Op);
            else if (i < index)
                BeforeOps.add(Op);
            else if (i > index)
                AfterOps.add(Op);
        }

        // Emit the load instruction.
        if (UnfoldLoad)
        {
            loadRegFromAddr(MF, Reg, AddrOps, rc, NewMIs);
            if (UnfoldStore)
            {
                // Address operands cannot be marked isKill.
                for (int i = 1; i != 1 + x86AddrNumOperands; ++i)
                {
                    MachineOperand MO = NewMIs.get(0).getOperand(i);
                    if (MO.isRegister())
                        MO.setIsKill(false);
                }
            }
        }

        // Emit the data processing instruction.
        //MachineInstr DataMI = MF.CreateMachineInstr(TID, MI.getDebugLoc(), true);
        MachineInstrBuilder MIB = buildMI(TID);

        if (FoldedStore)
            MIB.addReg(Reg, RegState.Define);
        for (int i = 0, e = BeforeOps.size(); i != e; ++i)
            MIB.addOperand(BeforeOps.get(i));
        if (FoldedLoad)
            MIB.addReg(Reg);
        for (int i = 0, e = AfterOps.size(); i != e; ++i)
            MIB.addOperand(AfterOps.get(i));
        for (int i = 0, e = ImpOps.size(); i != e; ++i)
        {
            MachineOperand MO = ImpOps.get(i);
            MIB.addReg(MO.getReg(),
                    getDefRegState(MO.isDef()) | RegState.Implicit
                            | getKillRegState(MO.isKill()) | getDeadRegState(
                            MO.isDead()) | getUndefRegState(MO.isUndef()));
        }
        // Change CMP32ri r, 0 back to TEST32rr r, r, etc.
        int NewOpc = 0;
        switch (MIB.getMInstr().getOpcode())
        {
            default:
                break;
            case CMP64ri32:
            case CMP32ri:
            case CMP16ri:
            case CMP8ri:
            {
                MachineOperand MO0 = MIB.getMInstr().getOperand(0);
                MachineOperand MO1 = MIB.getMInstr().getOperand(1);
                if (MO1.getImm() == 0)
                {
                    switch (MIB.getMInstr().getOpcode())
                    {
                        default:
                            break;
                        case CMP64ri32:
                            NewOpc = TEST64rr;
                            break;
                        case CMP32ri:
                            NewOpc = TEST32rr;
                            break;
                        case CMP16ri:
                            NewOpc = TEST16rr;
                            break;
                        case CMP8ri:
                            NewOpc = TEST8rr;
                            break;
                    }
                    MIB.getMInstr().setDesc(get(NewOpc));
                    MO1.changeToRegister(MO0.getReg(), false);
                }
            }
        }
        NewMIs.add(MIB.getMInstr());

        // Emit the store instruction.
        if (UnfoldStore)
        {
            TargetRegisterClass DstRC = TID.opInfo[0]
                    .getRegisterClass(registerInfo);
            storeRegToAddr(MF, Reg, true, AddrOps, DstRC, NewMIs);
        }

        return true;
    }

    public int getOpcodeAfterMemoryUnfold(int Opc, boolean UnfoldLoad,
            boolean UnfoldStore)
    {
        if (!memOp2RegOpTable.containsKey(Opc))
            return 0;

        boolean FoldedLoad = (memOp2RegOpTable.get(Opc).second & (1 << 4)) != 0;
        boolean FoldedStore =
                (memOp2RegOpTable.get(Opc).second & (1 << 5)) != 0;
        if (UnfoldLoad && !FoldedLoad)
            return 0;
        if (UnfoldStore && !FoldedStore)
            return 0;
        return memOp2RegOpTable.get(Opc).first;
    }

    public boolean BlockHasNoFallThrough(MachineBasicBlock mbb)
    {
        if (mbb.isEmpty())
            return false;

        switch (mbb.back().getOpcode())
        {
            case TCRETURNri:
            case TCRETURNdi:
            case RET:     // Return.
            case RETI:
            case TAILJMPd:
            case TAILJMPr:
            case TAILJMPm:
            case JMP:     // Uncond branch.
            case JMP32r:  // Indirect branch.
            case JMP64r:  // Indirect branch (64-bit).
            case JMP32m:  // Indirect branch through mem.
            case JMP64m:  // Indirect branch through mem (64-bit).
                return true;
            default:
                return false;
        }
    }

    public boolean ReverseBranchCondition(ArrayList<MachineOperand> cond)
    {
        assert cond.size() == 1 : "Invalid X86 branch condition!";
        long CC = cond.get(0).getImm();
        if (CC == COND_NE_OR_P || CC == COND_NP_OR_E)
            return true;
        cond.get(0).setImm(getOppositeBranchCondition((int) CC));
        return false;
    }

    public boolean isSafeToMoveRegisterClassDefs(TargetRegisterClass rc)
    {
        // FIXME: Return false for x87 stack register classes for now. We can't
        // allow any loads of these registers before FpGet_ST0_80.
        return !(rc == CCRRegisterClass || rc == RFP32RegisterClass
                || rc == RFP64RegisterClass || rc == RFP80RegisterClass);
    }

    public static int sizeOfImm(TargetInstrDesc Desc)
    {
        switch (Desc.tSFlags & X86II.ImmMask)
        {
            case X86II.Imm8:
                return 1;
            case X86II.Imm16:
                return 2;
            case X86II.Imm32:
                return 4;
            case X86II.Imm64:
                return 8;
            default:
                Util.shouldNotReachHere("Immediate size not set!");
                return 0;
        }
    }

    public static boolean isX86_64NonExtLowByteReg(int reg)
    {
        return reg == SPL || reg == BPL || reg == SIL || reg == DIL;
    }

    /// isX86_64ExtendedReg - Is the MachineOperand a x86-64 extended register?
    /// e.g. r8, xmm8, etc.
    public static boolean isX86_64ExtendedReg(MachineOperand MO)
    {
        if (!MO.isRegister())
            return false;
        switch (MO.getReg())
        {
            default:
                break;
            case R8:
            case R9:
            case R10:
            case R11:
            case R12:
            case R13:
            case R14:
            case R15:
            case R8D:
            case R9D:
            case R10D:
            case R11D:
            case R12D:
            case R13D:
            case R14D:
            case R15D:
            case R8W:
            case R9W:
            case R10W:
            case R11W:
            case R12W:
            case R13W:
            case R14W:
            case R15W:
            case R8B:
            case R9B:
            case R10B:
            case R11B:
            case R12B:
            case R13B:
            case R14B:
            case R15B:
            case XMM8:
            case XMM9:
            case XMM10:
            case XMM11:
            case XMM12:
            case XMM13:
            case XMM14:
            case XMM15:
                return true;
        }
        return false;
    }

    /// determineREX - Determine if the MachineInstr has to be encoded with a X86-64
    /// REX prefix which specifies 1) 64-bit instructions, 2) non-default operand
    /// size, and 3) use of X86-64 extended registers.
    public static int determineREX(MachineInstr MI)
    {
        int REX = 0;
        TargetInstrDesc Desc = MI.getDesc();

        // Pseudo instructions do not need REX prefix byte.
        if ((Desc.tSFlags & X86II.FormMask) == X86II.Pseudo)
            return 0;
        if ((Desc.tSFlags & X86II.REX_W) != 0)
            REX |= 1 << 3;

        int NumOps = Desc.getNumOperands();
        if (NumOps != 0)
        {
            boolean isTwoAddr =
                    NumOps > 1 && Desc.getOperandConstraint(1, TIED_TO) != -1;

            // If it accesses SPL, BPL, SIL, or DIL, then it requires a 0x40 REX prefix.
            int i = isTwoAddr ? 1 : 0;
            for (int e = NumOps; i != e; ++i)
            {
                MachineOperand MO = MI.getOperand(i);
                if (MO.isRegister())
                {
                    int Reg = MO.getReg();
                    if (isX86_64NonExtLowByteReg(Reg))
                        REX |= 0x40;
                }
            }

            switch (Desc.tSFlags & X86II.FormMask)
            {
                case X86II.MRMInitReg:
                    if (isX86_64ExtendedReg(MI.getOperand(0)))
                        REX |= (1 << 0) | (1 << 2);
                    break;
                case X86II.MRMSrcReg:
                {
                    if (isX86_64ExtendedReg(MI.getOperand(0)))
                        REX |= 1 << 2;
                    i = isTwoAddr ? 2 : 1;
                    for (int e = NumOps; i != e; ++i)
                    {
                        MachineOperand MO = MI.getOperand(i);
                        if (isX86_64ExtendedReg(MO))
                            REX |= 1 << 0;
                    }
                    break;
                }
                case X86II.MRMSrcMem:
                {
                    if (isX86_64ExtendedReg(MI.getOperand(0)))
                        REX |= 1 << 2;
                    int Bit = 0;
                    i = isTwoAddr ? 2 : 1;
                    for (; i != NumOps; ++i)
                    {
                        MachineOperand MO = MI.getOperand(i);
                        if (MO.isRegister())
                        {
                            if (isX86_64ExtendedReg(MO))
                                REX |= 1 << Bit;
                            Bit++;
                        }
                    }
                    break;
                }
                case X86II.MRM0m:
                case X86II.MRM1m:
                case X86II.MRM2m:
                case X86II.MRM3m:
                case X86II.MRM4m:
                case X86II.MRM5m:
                case X86II.MRM6m:
                case X86II.MRM7m:
                case X86II.MRMDestMem:
                {
                    int e = (isTwoAddr ?
                            x86AddrNumOperands + 1 :
                            x86AddrNumOperands);
                    i = isTwoAddr ? 1 : 0;
                    if (NumOps > e && isX86_64ExtendedReg(MI.getOperand(e)))
                        REX |= 1 << 2;
                    int Bit = 0;
                    for (; i != e; ++i)
                    {
                        MachineOperand MO = MI.getOperand(i);
                        if (MO.isRegister())
                        {
                            if (isX86_64ExtendedReg(MO))
                                REX |= 1 << Bit;
                            Bit++;
                        }
                    }
                    break;
                }
                default:
                {
                    if (isX86_64ExtendedReg(MI.getOperand(0)))
                        REX |= 1 << 0;
                    i = isTwoAddr ? 2 : 1;
                    for (int e = NumOps; i != e; ++i)
                    {
                        MachineOperand MO = MI.getOperand(i);
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
    static int sizePCRelativeBlockAddress()
    {
        return 4;
    }

    /// sizeGlobalAddress - Give the size of the emission of this global address
    ///
    static int sizeGlobalAddress(boolean dword)
    {
        return dword ? 8 : 4;
    }

    /// sizeConstPoolAddress - Give the size of the emission of this ant
    /// pool address
    ///
    static int sizeConstPoolAddress(boolean dword)
    {
        return dword ? 8 : 4;
    }

    /// sizeExternalSymbolAddress - Give the size of the emission of this external
    /// symbol
    ///
    static int sizeExternalSymbolAddress(boolean dword)
    {
        return dword ? 8 : 4;
    }

    /// sizeJumpTableAddress - Give the size of the emission of this jump
    /// table address
    ///
    static int sizeJumpTableAddress(boolean dword)
    {
        return dword ? 8 : 4;
    }

    static int sizeConstant(int Size)
    {
        return Size;
    }

    static int sizeRegModRMByte()
    {
        return 1;
    }

    static int sizeSIBByte()
    {
        return 1;
    }

    static int getDisplacementFieldSize(MachineOperand RelocOp)
    {
        int FinalSize = 0;
        // If this is a simple integer displacement that doesn't require a relocation.
        if (RelocOp == null)
        {
            FinalSize += sizeConstant(4);
            return FinalSize;
        }

        // Otherwise, this is something that requires a relocation.
        if (RelocOp.isGlobalAddress())
        {
            FinalSize += sizeGlobalAddress(false);
        }
        else if (RelocOp.isConstantPoolIndex())
        {
            FinalSize += sizeConstPoolAddress(false);
        }
        else if (RelocOp.isJumpTableIndex())
        {
            FinalSize += sizeJumpTableAddress(false);
        }
        else
        {
            Util.shouldNotReachHere("Unknown value to relocate!");
        }
        return FinalSize;
    }

    static int getMemModRMByteSize(MachineInstr MI, int Op, boolean IsPIC,
            boolean Is64BitMode)
    {
        MachineOperand Op3 = MI.getOperand(Op + 3);
        int DispVal = 0;
        MachineOperand DispForReloc = null;
        int FinalSize = 0;

        // Figure out what sort of displacement we have to handle here.
        if (Op3.isGlobalAddress())
        {
            DispForReloc = Op3;
        }
        else if (Op3.isConstantPoolIndex())
        {
            if (Is64BitMode || IsPIC)
            {
                DispForReloc = Op3;
            }
            else
            {
                DispVal = 1;
            }
        }
        else if (Op3.isJumpTableIndex())
        {
            if (Is64BitMode || IsPIC)
            {
                DispForReloc = Op3;
            }
            else
            {
                DispVal = 1;
            }
        }
        else
        {
            DispVal = 1;
        }

        MachineOperand Base = MI.getOperand(Op);
        MachineOperand IndexReg = MI.getOperand(Op + 2);

        int BaseReg = Base.getReg();

        // Is a SIB byte needed?
        if ((!Is64BitMode || DispForReloc != null || BaseReg != 0)
                && IndexReg.getReg() == 0 && (BaseReg == 0
                || X86RegisterInfo.getX86RegNum(BaseReg) != ESP))
        {
            if (BaseReg == 0)
            {  // Just a displacement?
                // Emit special case [disp32] encoding
                ++FinalSize;
                FinalSize += getDisplacementFieldSize(DispForReloc);
            }
            else
            {
                int BaseRegNo = X86RegisterInfo.getX86RegNum(BaseReg);
                if (DispForReloc == null && DispVal == 0 && BaseRegNo != EBP)
                {
                    // Emit simple indirect register encoding... [EAX] f.e.
                    ++FinalSize;
                    // Be pessimistic and assume it's a disp32, not a disp8
                }
                else
                {
                    // Emit the most general non-SIB encoding: [REG+disp32]
                    ++FinalSize;
                    FinalSize += getDisplacementFieldSize(DispForReloc);
                }
            }

        }
        else
        {  // We need a SIB byte, so start by outputting the ModR/M byte first
            assert IndexReg.getReg() != ESP && IndexReg.getReg()
                    != RSP : "Cannot use ESP as index reg!";

            boolean ForceDisp32 = false;
            if (BaseReg == 0 || DispForReloc != null)
            {
                // Emit the normal disp32 encoding.
                ++FinalSize;
                ForceDisp32 = true;
            }
            else
            {
                ++FinalSize;
            }

            FinalSize += sizeSIBByte();

            // Do we need to output a displacement?
            if (DispVal != 0 || ForceDisp32)
            {
                FinalSize += getDisplacementFieldSize(DispForReloc);
            }
        }
        return FinalSize;
    }

    static int getInstSizeWithDesc(MachineInstr mi, TargetInstrDesc desc,
            boolean isPIC, boolean is64BitMode)
    {

        int opcode = desc.getOpcode();
        int finalSize = 0;

        // Emit the lock opcode prefix as needed.
        if ((desc.tSFlags & X86II.LOCK) != 0)
            ++finalSize;

        // Emit segment override opcode prefix as needed.
        switch (desc.tSFlags & X86II.SegOvrMask)
        {
            case X86II.FS:
            case X86II.GS:
                ++finalSize;
                break;
            default:
                Util.shouldNotReachHere("Invalid segment!");
            case 0:
                break;  // No segment override!
        }

        // Emit the repeat opcode prefix as needed.
        if ((desc.tSFlags & X86II.Op0Mask) == X86II.REP)
            ++finalSize;

        // Emit the operand size opcode prefix as needed.
        if ((desc.tSFlags & X86II.OpSize) != 0)
            ++finalSize;

        // Emit the address size opcode prefix as needed.
        if ((desc.tSFlags & X86II.AdSize) != 0)
            ++finalSize;

        boolean Need0FPrefix = false;
        switch (desc.tSFlags & X86II.Op0Mask)
        {
            case X86II.TB:  // Two-byte opcode prefix
            case X86II.T8:  // 0F 38
            case X86II.TA:  // 0F 3A
                Need0FPrefix = true;
                break;
            case X86II.TF: // F2 0F 38
                ++finalSize;
                Need0FPrefix = true;
                break;
            case X86II.REP:
                break; // already handled.
            case X86II.XS:   // F3 0F
                ++finalSize;
                Need0FPrefix = true;
                break;
            case X86II.XD:   // F2 0F
                ++finalSize;
                Need0FPrefix = true;
                break;
            case X86II.D8:
            case X86II.D9:
            case X86II.DA:
            case X86II.DB:
            case X86II.DC:
            case X86II.DD:
            case X86II.DE:
            case X86II.DF:
                ++finalSize;
                break; // Two-byte opcode prefix
            default:
                Util.shouldNotReachHere("Invalid prefix!");
            case 0:
                break;  // No prefix!
        }

        if (is64BitMode)
        {
            // REX prefix
            int REX = determineREX(mi);
            if (REX != 0)
                ++finalSize;
        }

        // 0x0F escape code must be emitted just before the opcode.
        if (Need0FPrefix)
            ++finalSize;

        switch (desc.tSFlags & X86II.Op0Mask)
        {
            case X86II.T8:  // 0F 38
                ++finalSize;
                break;
            case X86II.TA:  // 0F 3A
                ++finalSize;
                break;
            case X86II.TF: // F2 0F 38
                ++finalSize;
                break;
        }

        // If this is a two-address instruction, skip one of the register operands.
        int NumOps = desc.getNumOperands();
        int CurOp = 0;
        if (NumOps > 1 && desc.getOperandConstraint(1, TIED_TO) != -1)
            CurOp++;
        else if (NumOps > 2
                && desc.getOperandConstraint(NumOps - 1, TIED_TO) == 0)
            // Skip the last source operand that is tied_to the dest reg. e.g. LXADD32
            --NumOps;

        switch (desc.tSFlags & X86II.FormMask)
        {
            default:
                Util.shouldNotReachHere(
                        "Unknown FormMask value in X86 MachineCodeEmitter!");
            case X86II.Pseudo:
                // Remember the current PC offset, this is the PIC relocation
                // base address.
                switch (opcode)
                {
                    default:
                        break;
                    case INLINEASM:
                    {
                        assert false : "Inline asm is not supported yet!";
                        break;
                    }
                    case DBG_LABEL:
                    case EH_LABEL:
                        break;
                    case IMPLICIT_DEF:
                    case DECLARE:
                    case DWARF_LOC:
                    case FP_REG_KILL:
                        break;
                    case MOVPC32r:
                    {
                        // This emits the "call" portion of this pseudo instruction.
                        ++finalSize;
                        finalSize += sizeConstant(sizeOfImm(desc));
                        break;
                    }
                }
                CurOp = NumOps;
                break;
            case X86II.RawFrm:
                ++finalSize;

                if (CurOp != NumOps)
                {
                    MachineOperand MO = mi.getOperand(CurOp++);
                    if (MO.isMBB())
                    {
                        finalSize += sizePCRelativeBlockAddress();
                    }
                    else if (MO.isGlobalAddress())
                    {
                        finalSize += sizeGlobalAddress(false);
                    }
                    else if (MO.isExternalSymbol())
                    {
                        finalSize += sizeExternalSymbolAddress(false);
                    }
                    else if (MO.isImm())
                    {
                        finalSize += sizeConstant(sizeOfImm(desc));
                    }
                    else
                    {
                        Util.shouldNotReachHere("Unknown RawFrm operand!");
                    }
                }
                break;

            case X86II.AddRegFrm:
                ++finalSize;
                ++CurOp;

                if (CurOp != NumOps)
                {
                    MachineOperand MO1 = mi.getOperand(CurOp++);
                    int Size = sizeOfImm(desc);
                    if (MO1.isImm())
                        finalSize += sizeConstant(Size);
                    else
                    {
                        boolean dword = false;
                        if (opcode == MOV64ri)
                            dword = true;
                        if (MO1.isGlobalAddress())
                        {
                            finalSize += sizeGlobalAddress(dword);
                        }
                        else if (MO1.isExternalSymbol())
                            finalSize += sizeExternalSymbolAddress(dword);
                        else if (MO1.isConstantPoolIndex())
                            finalSize += sizeConstPoolAddress(dword);
                        else if (MO1.isJumpTableIndex())
                            finalSize += sizeJumpTableAddress(dword);
                    }
                }
                break;

            case X86II.MRMDestReg:
            {
                ++finalSize;
                finalSize += sizeRegModRMByte();
                CurOp += 2;
                if (CurOp != NumOps)
                {
                    ++CurOp;
                    finalSize += sizeConstant(sizeOfImm(desc));
                }
                break;
            }
            case X86II.MRMDestMem:
            {
                ++finalSize;
                finalSize += getMemModRMByteSize(mi, CurOp, isPIC, is64BitMode);
                CurOp += x86AddrNumOperands + 1;
                if (CurOp != NumOps)
                {
                    ++CurOp;
                    finalSize += sizeConstant(sizeOfImm(desc));
                }
                break;
            }

            case X86II.MRMSrcReg:
                ++finalSize;
                finalSize += sizeRegModRMByte();
                CurOp += 2;
                if (CurOp != NumOps)
                {
                    ++CurOp;
                    finalSize += sizeConstant(sizeOfImm(desc));
                }
                break;

            case X86II.MRMSrcMem:
            {
                int AddrOperands;
                if (opcode == LEA64r || opcode == LEA64_32r || opcode == LEA16r
                        || opcode == LEA32r)
                    AddrOperands =
                            x86AddrNumOperands - 1; // No segment register
                else
                    AddrOperands = x86AddrNumOperands;

                ++finalSize;
                finalSize += getMemModRMByteSize(mi, CurOp + 1, isPIC,
                        is64BitMode);
                CurOp += AddrOperands + 1;
                if (CurOp != NumOps)
                {
                    ++CurOp;
                    finalSize += sizeConstant(sizeOfImm(desc));
                }
                break;
            }

            case X86II.MRM0r:
            case X86II.MRM1r:
            case X86II.MRM2r:
            case X86II.MRM3r:
            case X86II.MRM4r:
            case X86II.MRM5r:
            case X86II.MRM6r:
            case X86II.MRM7r:
                ++finalSize;
                if (desc.getOpcode() == LFENCE || desc.getOpcode() == MFENCE)
                {
                    // Special handling of lfence and mfence;
                    finalSize += sizeRegModRMByte();
                }
                else if (desc.getOpcode() == MONITOR
                        || desc.getOpcode() == MWAIT)
                {
                    // Special handling of monitor and mwait.
                    finalSize += sizeRegModRMByte() + 1; // +1 for the opcode.
                }
                else
                {
                    ++CurOp;
                    finalSize += sizeRegModRMByte();
                }

                if (CurOp != NumOps)
                {
                    MachineOperand MO1 = mi.getOperand(CurOp++);
                    int Size = sizeOfImm(desc);
                    if (MO1.isImm())
                        finalSize += sizeConstant(Size);
                    else
                    {
                        boolean dword = false;
                        if (opcode == MOV64ri32)
                            dword = true;
                        if (MO1.isGlobalAddress())
                        {
                            finalSize += sizeGlobalAddress(dword);
                        }
                        else if (MO1.isExternalSymbol())
                            finalSize += sizeExternalSymbolAddress(dword);
                        else if (MO1.isConstantPoolIndex())
                            finalSize += sizeConstPoolAddress(dword);
                        else if (MO1.isJumpTableIndex())
                            finalSize += sizeJumpTableAddress(dword);
                    }
                }
                break;

            case X86II.MRM0m:
            case X86II.MRM1m:
            case X86II.MRM2m:
            case X86II.MRM3m:
            case X86II.MRM4m:
            case X86II.MRM5m:
            case X86II.MRM6m:
            case X86II.MRM7m:
            {

                ++finalSize;
                finalSize += getMemModRMByteSize(mi, CurOp, isPIC, is64BitMode);
                CurOp += x86AddrNumOperands;

                if (CurOp != NumOps)
                {
                    MachineOperand MO = mi.getOperand(CurOp++);
                    int Size = sizeOfImm(desc);
                    if (MO.isImm())
                        finalSize += sizeConstant(Size);
                    else
                    {
                        boolean dword = false;
                        if (opcode == MOV64mi32)
                            dword = true;
                        if (MO.isGlobalAddress())
                        {
                            finalSize += sizeGlobalAddress(dword);
                        }
                        else if (MO.isExternalSymbol())
                            finalSize += sizeExternalSymbolAddress(dword);
                        else if (MO.isConstantPoolIndex())
                            finalSize += sizeConstPoolAddress(dword);
                        else if (MO.isJumpTableIndex())
                            finalSize += sizeJumpTableAddress(dword);
                    }
                }
                break;
            }

            case X86II.MRMInitReg:
                ++finalSize;
                // Duplicate register, used by things like MOV8r0 (aka xor reg,reg).
                finalSize += sizeRegModRMByte();
                ++CurOp;
                break;
        }

        if (!desc.isVariadic() && CurOp != NumOps)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream os = new PrintStream(baos);
            os.printf("Cannot determine size: ");
            mi.print(os, null);
            llvmReportError(baos.toString());
        }

        return finalSize;
    }

    public int GetInstSizeInBytes(MachineInstr MI)
    {
        TargetInstrDesc Desc = MI.getDesc();
        boolean IsPIC = tm.getRelocationModel() == PIC_;
        boolean Is64BitMode = tm.getSubtarget().is64Bit();
        int Size = getInstSizeWithDesc(MI, Desc, IsPIC, Is64BitMode);
        if (Desc.getOpcode() == MOVPC32r)
            Size += getInstSizeWithDesc(MI, get(POP32r), IsPIC, Is64BitMode);
        return Size;
    }

    /**
     * Return a virtual register initialized with the
     * the global base register value. Output instructions required to
     * initialize the register in the function entry block, if necessary.
     *
     * @param mf
     * @return
     */
    public int getGlobalBaseReg(MachineFunction mf)
    {
        assert !tm.getSubtarget()
                .is64Bit() : "X86-64 PIC uses RIP relative addressing";

        X86MachineFunctionInfo X86FI = (X86MachineFunctionInfo) mf.getInfo();
        int GlobalBaseReg = X86FI.getGlobalBaseReg();
        if (GlobalBaseReg != 0)
            return GlobalBaseReg;

        // Insert the set of globalBaseReg into the first mbb of the function
        MachineBasicBlock FirstMBB = mf.getEntryBlock();
        int MBBI = 0;
        //DebugLoc DL = DebugLoc::getUnknownLoc();
        //if (MBBI != FirstMBB.end()) DL = MBBI.getDebugLoc();
        MachineRegisterInfo RegInfo = mf.getMachineRegisterInfo();
        int PC = RegInfo.createVirtualRegister(GR32RegisterClass);

        TargetInstrInfo TII = tm.getInstrInfo();
        // Operand of MovePCtoStack is completely ignored by asm printer. It's
        // only used in JIT code emission as displacement to pc.
        buildMI(FirstMBB, MBBI, TII.get(MOVPC32r), PC).addImm(0);

        // If we're using vanilla 'GOT' PIC style, we should use relative addressing
        // not to pc, but to _GLOBAL_OFFSET_TABLE_ external.
        if (((X86Subtarget) tm.getSubtarget()).isPICStyleGOT())
        {
            GlobalBaseReg = RegInfo.createVirtualRegister(GR32RegisterClass);
            // Generate addl $__GLOBAL_OFFSET_TABLE_ + [.-piclabel], %some_register
            buildMI(FirstMBB, MBBI, TII.get(ADD32ri), GlobalBaseReg).addReg(PC)
                    .addExternalSymbol("_GLOBAL_OFFSET_TABLE_", 0,
                            X86II.MO_GOT_ABSOLUTE_ADDRESS);
        }
        else
        {
            GlobalBaseReg = PC;
        }

        X86FI.setGlobalBaseReg(GlobalBaseReg);
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

    public static boolean isGlobalStubReference(int targetFlag)
    {
        switch (targetFlag)
        {
            case MO_DLLIMPORT: // dllimport stub.
            case MO_GOTPCREL:  // rip-relative GOT reference.
            case MO_GOT:       // normal GOT reference.
            case MO_DARWIN_NONLAZY_PIC_BASE:        // Normal $non_lazy_ptr ref.
            case MO_DARWIN_NONLAZY:                 // Normal $non_lazy_ptr ref.
            case MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE: // Hidden $non_lazy_ptr ref.
            case MO_DARWIN_HIDDEN_NONLAZY:          // Hidden $non_lazy_ptr ref.
                return true;
            default:
                return false;
        }
    }

    public static boolean isGlobalRelativeToPICBase(int flags)
    {
        switch(flags)
        {
            case MO_GOTOFF:
            case MO_GOT:
            case MO_PIC_BASE_OFFSET:
            case MO_DARWIN_NONLAZY_PIC_BASE:
            case MO_DARWIN_HIDDEN_NONLAZY_PIC_BASE:
                return true;
            default:
                return false;
        }
    }
}
