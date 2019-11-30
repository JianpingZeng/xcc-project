package backend.target.mips;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.codegen.*;
import backend.codegen.dagisel.*;
import backend.mc.MCRegisterClass;
import backend.support.CallingConv;
import backend.target.TargetLowering;
import backend.value.Function;
import tools.Util;

import java.util.ArrayList;

import static backend.codegen.PseudoSourceValue.getFixedStack;
import static backend.target.TargetLowering.LegalizeAction.Custom;
import static backend.target.TargetLowering.LegalizeAction.Expand;
import static backend.target.TargetLowering.LegalizeAction.Promote;

/**
 * This class is used for lower Mips-specific operations to the Machine instructions
 * which are supported by the target machine.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class MipsTargetLowering extends TargetLowering {
  private MipsSubtarget subtarget;
  private boolean hasMips64;
  private boolean isN64;

  public MipsTargetLowering(MipsTargetMachine tm) {
    super(tm, new MipsTargetObjectFile());
    subtarget = tm.getSubtarget();
    hasMips64 = subtarget.hasMips64();
    isN64 = subtarget.isABI_N64();

    // Mips does not have i1 type, so use i32 for
    // setcc operations results (slt, sgt, ...).
    setBooleanContents(BooleanContent.ZeroOrOneBooleanContent);
    setBooleanVectorContents(BooleanContent.ZeroOrOneBooleanContent);

    // Set up the register classes
    addRegisterClass(MVT.i32, MipsGenRegisterInfo.CPURegsRegisterClass);
    addRegisterClass(MVT.f32, MipsGenRegisterInfo.FGR32RegisterClass);

    if (hasMips64)
      addRegisterClass(MVT.i64, MipsGenRegisterInfo.CPU64RegsRegisterClass);

    // When dealing with single precision only, use libcalls
    if (!subtarget.isSingleFloat()) {
      if (hasMips64)
        addRegisterClass(MVT.f64, MipsGenRegisterInfo.FGR64RegisterClass);
      else
        addRegisterClass(MVT.f64, MipsGenRegisterInfo.AFGR64RegisterClass);
    }

    // Load extented operations for i1 types must be promoted
    setLoadExtAction(LoadExtType.EXTLOAD,  new MVT(MVT.i1),  Promote);
    setLoadExtAction(LoadExtType.ZEXTLOAD, new MVT(MVT.i1),  Promote);
    setLoadExtAction(LoadExtType.SEXTLOAD, new MVT(MVT.i1),  Promote);

    // MIPS doesn't have extending float->double load/store
    setLoadExtAction(LoadExtType.EXTLOAD, new MVT(MVT.f32), Expand);
    setTruncStoreAction(MVT.f64, MVT.f32, Expand);

    // Used by legalize types to correctly generate the setcc result.
    // Without this, every float setcc comes with a AND/OR with the result,
    // we don't want this, since the fpcmp result goes to a flag register,
    // which is used implicitly by brcond and select operations.
    addPromotedToType(ISD.SETCC, MVT.i1, MVT.i32);

    // Mips Custom Operations
    setOperationAction(ISD.GlobalAddress,      MVT.i32,   Custom);
    setOperationAction(ISD.GlobalAddress,      MVT.i64,   Custom);
    setOperationAction(ISD.BlockAddress,       MVT.i32,   Custom);
    setOperationAction(ISD.GlobalTLSAddress,   MVT.i32,   Custom);
    setOperationAction(ISD.JumpTable,          MVT.i32,   Custom);
    setOperationAction(ISD.ConstantPool,       MVT.i32,   Custom);
    setOperationAction(ISD.SELECT,             MVT.f32,   Custom);
    setOperationAction(ISD.SELECT,             MVT.f64,   Custom);
    setOperationAction(ISD.SELECT,             MVT.i32,   Custom);
    setOperationAction(ISD.BRCOND,             MVT.Other, Custom);
    setOperationAction(ISD.DYNAMIC_STACKALLOC, MVT.i32,   Custom);
    setOperationAction(ISD.VASTART,            MVT.Other, Custom);

    setOperationAction(ISD.SDIV, MVT.i32, Expand);
    setOperationAction(ISD.SREM, MVT.i32, Expand);
    setOperationAction(ISD.UDIV, MVT.i32, Expand);
    setOperationAction(ISD.UREM, MVT.i32, Expand);
    setOperationAction(ISD.SDIV, MVT.i64, Expand);
    setOperationAction(ISD.SREM, MVT.i64, Expand);
    setOperationAction(ISD.UDIV, MVT.i64, Expand);
    setOperationAction(ISD.UREM, MVT.i64, Expand);

    // Operations not directly supported by Mips.
    setOperationAction(ISD.BR_JT,             MVT.Other, Expand);
    setOperationAction(ISD.BR_CC,             MVT.Other, Expand);
    setOperationAction(ISD.SELECT_CC,         MVT.Other, Expand);
    setOperationAction(ISD.UINT_TO_FP,        MVT.i32,   Expand);
    setOperationAction(ISD.FP_TO_UINT,        MVT.i32,   Expand);
    setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i1,    Expand);
    setOperationAction(ISD.CTPOP,             MVT.i32,   Expand);
    setOperationAction(ISD.CTTZ,              MVT.i32,   Expand);
    setOperationAction(ISD.ROTL,              MVT.i32,   Expand);
    setOperationAction(ISD.ROTL,              MVT.i64,   Expand);

    if (!subtarget.hasMips32r2())
      setOperationAction(ISD.ROTR, MVT.i32,   Expand);

    if (!subtarget.hasMips64r2())
      setOperationAction(ISD.ROTR, MVT.i64,   Expand);

    setOperationAction(ISD.SHL_PARTS,         MVT.i32,   Expand);
    setOperationAction(ISD.SRA_PARTS,         MVT.i32,   Expand);
    setOperationAction(ISD.SRL_PARTS,         MVT.i32,   Expand);
    setOperationAction(ISD.FCOPYSIGN,         MVT.f32,   Custom);
    setOperationAction(ISD.FCOPYSIGN,         MVT.f64,   Custom);
    setOperationAction(ISD.FSIN,              MVT.f32,   Expand);
    setOperationAction(ISD.FSIN,              MVT.f64,   Expand);
    setOperationAction(ISD.FCOS,              MVT.f32,   Expand);
    setOperationAction(ISD.FCOS,              MVT.f64,   Expand);
    setOperationAction(ISD.FPOWI,             MVT.f32,   Expand);
    setOperationAction(ISD.FPOW,              MVT.f32,   Expand);
    setOperationAction(ISD.FPOW,              MVT.f64,   Expand);
    setOperationAction(ISD.FLOG,              MVT.f32,   Expand);
    setOperationAction(ISD.FLOG2,             MVT.f32,   Expand);
    setOperationAction(ISD.FLOG10,            MVT.f32,   Expand);
    setOperationAction(ISD.FEXP,              MVT.f32,   Expand);
    setOperationAction(ISD.FMA,               MVT.f32,   Expand);
    setOperationAction(ISD.FMA,               MVT.f64,   Expand);

    setOperationAction(ISD.EXCEPTIONADDR,     MVT.i32, Expand);
    setOperationAction(ISD.EHSELECTION,       MVT.i32, Expand);

    setOperationAction(ISD.VAARG,             MVT.Other, Expand);
    setOperationAction(ISD.VACOPY,            MVT.Other, Expand);
    setOperationAction(ISD.VAEND,             MVT.Other, Expand);

    // Use the default for now
    setOperationAction(ISD.STACKSAVE,         MVT.Other, Expand);
    setOperationAction(ISD.STACKRESTORE,      MVT.Other, Expand);

    setOperationAction(ISD.MEMBARRIER,        MVT.Other, Custom);
    setOperationAction(ISD.ATOMIC_FENCE,      MVT.Other, Custom);

    setOperationAction(ISD.ATOMIC_LOAD,       MVT.i32,    Expand);
    setOperationAction(ISD.ATOMIC_STORE,      MVT.i32,    Expand);

    if (subtarget.isSingleFloat())
      setOperationAction(ISD.SELECT_CC, MVT.f64, Expand);

    if (!subtarget.hasSEInReg()) {
      setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i8,  Expand);
      setOperationAction(ISD.SIGN_EXTEND_INREG, MVT.i16, Expand);
    }

    if (!subtarget.hasBitCount())
      setOperationAction(ISD.CTLZ, MVT.i32, Expand);

    if (!subtarget.hasSwap())
      setOperationAction(ISD.BSWAP, MVT.i32, Expand);

    setTargetDAGCombine(ISD.ADDE);
    setTargetDAGCombine(ISD.SUBE);
    setTargetDAGCombine(ISD.SDIVREM);
    setTargetDAGCombine(ISD.UDIVREM);
    setTargetDAGCombine(ISD.SETCC);
    setTargetDAGCombine(ISD.AND);
    setTargetDAGCombine(ISD.OR);

    setMinFunctionAlignment(2);

    setStackPointerRegisterToSaveRestore(MipsGenRegisterNames.SP);
    computeRegisterProperties();

    setExceptionPointerRegister(MipsGenRegisterNames.A0);
    setExceptionSelectorRegister(MipsGenRegisterNames.A1);
  }

  @Override
  public MachineFunctionInfo createMachineFunctionInfo(MachineFunction mf) {
    return new MipsFunctionInfo(mf);
  }

  @Override
  public int getFunctionAlignment(Function fn) {
    return 2;
  }

  @Override
  public SDValue lowerOperation(SDValue op, SelectionDAG dag) {
    switch (op.getOpcode()) {
      case ISD.BRCOND:               return lowerBRCOND(op, dag);
      case ISD.ConstantPool:        return lowerConstantPool(op, dag);
      case ISD.DYNAMIC_STACKALLOC: return lowerDYNAMIC_STACKALLOC(op, dag);
      case ISD.GlobalAddress:       return lowerGlobalAddress(op, dag);
      case ISD.BlockAddress:        return lowerBlockAddress(op, dag);
      case ISD.GlobalTLSAddress:    return lowerGlobalTLSAddress(op, dag);
      case ISD.JumpTable:            return lowerJumpTable(op, dag);
      case ISD.SELECT:               return lowerSELECT(op, dag);
      case ISD.VASTART:              return lowerVASTART(op, dag);
      case ISD.FCOPYSIGN:            return lowerFCOPYSIGN(op, dag);
      case ISD.FRAMEADDR:            return lowerFRAMEADDR(op, dag);
      case ISD.MEMBARRIER:           return lowerMEMBARRIER(op, dag);
      case ISD.ATOMIC_FENCE:        return lowerATOMIC_FENCE(op, dag);
    }
    return new SDValue();
  }

  private SDValue lowerATOMIC_FENCE(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerMEMBARRIER(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerFRAMEADDR(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerFCOPYSIGN(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerVASTART(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerSELECT(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerJumpTable(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerGlobalTLSAddress(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerBlockAddress(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerGlobalAddress(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerDYNAMIC_STACKALLOC(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerConstantPool(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  private SDValue lowerBRCOND(SDValue op, SelectionDAG dag) {
    Util.shouldNotReachHere();
    return null;
  }

  @Override
  public SDValue lowerCall(SDValue chain,
                           SDValue callee,
                           CallingConv cc,
                           boolean isVarArg,
                           boolean isTailCall,
                           ArrayList<OutputArg> outs,
                           ArrayList<InputArg> ins,
                           SelectionDAG dag,
                           ArrayList<SDValue> inVals) {
    Util.shouldNotReachHere();
    return null;
  }

  // Mips O32 ABI rules:
  // ---
  // i32 - Passed in A0, A1, A2, A3 and stack
  // f32 - Only passed in f32 registers if no int reg has been used yet to hold
  //       an argument. Otherwise, passed in A1, A2, A3 and stack.
  // f64 - Only passed in two aliased f32 registers if no int reg has been used
  //       yet to hold an argument. Otherwise, use A2, A3 and stack. If A1 is
  //       not used, it must be shadowed. If only A3 is avaiable, shadow it and
  //       go to stack.
  //
  //  For vararg functions, all arguments are passed in A0, A1, A2, A3 and stack.
  //===----------------------------------------------------------------------===//
  private static final CCAssignFn CC_MipsO32 = new CCAssignFn() {
    @Override
    public boolean apply(int valNo, EVT valVT, EVT locVT,
                         CCValAssign.LocInfo locInfo,
                         ArgFlagsTy argFlags, CCState state) {
      int intRegSize = 4, floatRegSize = 2;
      int[] IntRegs = {MipsGenRegisterNames.A0, MipsGenRegisterNames.A1,
                       MipsGenRegisterNames.A2, MipsGenRegisterNames.A3};
      int[] F32Regs = {MipsGenRegisterNames.F12, MipsGenRegisterNames.F14};
      int[] F64Regs = {MipsGenRegisterNames.D6, MipsGenRegisterNames.D7};

      // byval args.
      if (argFlags.isByVal()) {
        state.handleByVal(valNo, valVT, locVT, locInfo,
            1 /*minsize*/, 4 /*min align*/, argFlags);
        int nextReg = (state.getNextStackOffset() + 3) / 4;
        for (int r = state.getFirstUnallocated(IntRegs); r < Math.min(intRegSize, nextReg); ++r)
          state.allocateReg(IntRegs[r]);
        return false;
      }

      // promote i8, i16 -> i32.
      if (locVT.equals(new EVT(MVT.i8)) || locVT.equals(new EVT(MVT.i16))) {
        locVT = new EVT(MVT.i32);
        if (argFlags.isSExt())
          locInfo = CCValAssign.LocInfo.SExt;
        else if (argFlags.isZExt())
          locInfo = CCValAssign.LocInfo.ZExt;
        else
          locInfo = CCValAssign.LocInfo.AExt;
      }

      int reg = 0;
      // f32 and f64 are allocated in A0, A1, A2, A3 when either of the following
      // is true: function is vararg, argument is 3rd or higher, there is previous
      // argument which is not f32 or f64.
      boolean allocateFloatsInIntReg = state.isVarArg() || valNo > 1 || state.getFirstUnallocated(F32Regs) != valNo;
      int origAlign = argFlags.getOrigAlign();
      boolean is64 = valVT.equals(new EVT(MVT.i32)) && origAlign == 0;

      if (valVT.equals(new EVT(MVT.i32)) || (valVT.equals(new EVT(MVT.f32)) && allocateFloatsInIntReg)) {
        reg = state.allocateReg(IntRegs);
        // if this is the first part of an i64 arg, the allocated register must be eigher A0 or A2.
        if (is64 && (reg == MipsGenRegisterNames.A1 || reg == MipsGenRegisterNames.A3))
          reg = state.allocateReg(IntRegs);
        locVT = new EVT(MVT.i32);
      }
      else if (valVT.equals(new EVT(MVT.f64)) && allocateFloatsInIntReg) {
        // Allocate int register and shadow next int register. If first
        // available register is Mips::A1 or Mips::A3, shadow it too.
        reg = state.allocateReg(IntRegs);
        if (reg == MipsGenRegisterNames.A1 || reg == MipsGenRegisterNames.A3)
          reg = state.allocateReg(IntRegs);

        state.allocateReg(IntRegs);
        locVT = new EVT(MVT.i32);
      }
      else if (valVT.isFloatingPoint() && !allocateFloatsInIntReg) {
        // we are guaranteed to find an available float register
        if (valVT.equals(new EVT(MVT.f32))) {
          reg = state.allocateReg(F32Regs);
          state.allocateReg(IntRegs);
        }
        else {
          reg = state.allocateReg(F64Regs);
          // shadow int registers.
          int reg2 = state.allocateReg(IntRegs);

          if (reg2 == MipsGenRegisterNames.A1 || reg2 == MipsGenRegisterNames.A3)
            reg = state.allocateReg(IntRegs);
          state.allocateReg(IntRegs);
        }
      }
      else
        Util.shouldNotReachHere("Can't handle unknown valVT!");

      int sizeInBytes = valVT.getSizeInBits() / 8;
      int offset = state.allocateStack(sizeInBytes, origAlign);
      if (reg == 0)
        state.addLoc(CCValAssign.getMem(valNo, valVT, offset, locVT, locInfo));
      else
        state.addLoc(CCValAssign.getReg(valNo, valVT, reg, locVT, locInfo));
      return false;
    }
  };

  @Override
  public SDValue lowerFormalArguments(SDValue chain,
                                      CallingConv callingConv,
                                      boolean varArg,
                                      ArrayList<InputArg> ins,
                                      SelectionDAG dag,
                                      ArrayList<SDValue> inVals) {
    MachineFunction mf = dag.getMachineFunction();
    MipsFunctionInfo mipsFI = (MipsFunctionInfo) mf.getInfo();
    MachineFrameInfo mfi = mf.getFrameInfo();

    mipsFI.setVarArgsFrameINdex(0);

    // used with vargs to accumulate store chains.
    ArrayList<SDValue> outChains = new ArrayList<>();

    ArrayList<CCValAssign> argLocs = new ArrayList<>();
    CCState ccInfo = new CCState(callingConv, varArg, tm, argLocs, dag.getContext());
    if (subtarget.isABI_O32())
      ccInfo.analyzeFormalArguments(ins, CC_MipsO32);
    else
      ccInfo.analyzeFormalArguments(ins, MipsGenCallingConv.CC_Mips);

    // MipsFI->LastInArgFI is 0 at the entry of this function.
    int lastFI = 0;
    for (int i = 0, e = argLocs.size(); i < e; ++i) {
      CCValAssign va = argLocs.get(i);

      // Arguments stored an registers.
      if (va.isRegLoc()) {
        EVT regVT = va.getLocVT();
        int argReg = va.getLocReg();
        MCRegisterClass rc = null;
        if (regVT.equals(new EVT(MVT.i32)))
          rc = MipsGenRegisterInfo.CPURegsRegisterClass;
        else if (regVT.equals(new EVT(MVT.i64)))
          rc = MipsGenRegisterInfo.CPU64RegsRegisterClass;
        else if (regVT.equals(new EVT(MVT.f32)))
          rc = MipsGenRegisterInfo.FGR32RegisterClass;
        else if (regVT.equals(new EVT(MVT.f64)))
          rc = hasMips64 ? MipsGenRegisterInfo.FGR64RegisterClass : MipsGenRegisterInfo.AFGR64RegisterClass;
        else
          Util.shouldNotReachHere("regVT is not supported by formalArgument lowering!");

        int reg = mf.addLiveIn(argReg, rc);
        SDValue argValue = dag.getCopyFromReg(chain, reg, regVT);

        // If this is an 8 or 16-bit value, it has been passed promoted
        // to 32 bits.  Insert an assert[sz]ext to capture this, then
        // truncate to the right size.
        if (!va.getLocInfo().equals(CCValAssign.LocInfo.Full)) {
          int opcode = 0;
          if (va.getLocInfo().equals(CCValAssign.LocInfo.SExt))
            opcode = ISD.AssertSext;
          else if (va.getLocInfo().equals(CCValAssign.LocInfo.ZExt))
            opcode = ISD.AssertZext;
          if (opcode != 0)
            argValue = dag.getNode(opcode, regVT, argValue, dag.getValueType(va.getValVT()));

          argValue = dag.getNode(ISD.TRUNCATE, va.getValVT(), argValue);
        }

        // Handle O32 ABI cases: i32->f32 and (i32,i32)->f64
        if (subtarget.isABI_O32()) {
          if (regVT.equals(new EVT(MVT.i32)) && va.getValVT().equals(new EVT(MVT.f32)))
            argValue = dag.getNode(ISD.BIT_CONVERT, new EVT(MVT.f32), argValue);
          if (regVT.equals(new EVT(MVT.i32)) && va.getValVT().equals(new EVT(MVT.f64))) {
            int reg2 = mf.addLiveIn(getNextIntArgReg(argReg), rc);
            SDValue argValue2 = dag.getCopyFromReg(chain, reg2, regVT);
            if (!subtarget.isLittleEndian()) {
              SDValue temp = argValue;
              argValue = argValue2;
              argValue2 = temp;
            }
            argValue = dag.getNode(MipsISD.BuildPairF64, new EVT(MVT.f64), argValue, argValue2);
          }
        }

        inVals.add(argValue);
      }
      else {
        // va is in the stack.
        Util.assertion(va.isMemLoc());

        ArgFlagsTy flags = ins.get(i).flags;
        if (flags.isByVal()) {
          Util.assertion(subtarget.isABI_O32(), "No support for byVal args by ABIs other than O32 yet.");
          Util.assertion(flags.getByValSize() != 0, "byVal args of size 0 should have been ignored by front-end!");

          int numWords = (flags.getByValSize() + 3) /4;
          lastFI = mfi.createFixedObject(numWords * 4, va.getLocMemOffset(), true);
          SDValue fin = dag.getFrameIndex(lastFI, new EVT(getPointerTy()), false);
          inVals.add(fin);
          readByValArg(mf, chain, outChains, dag, numWords, fin, va, flags);
          continue;
        }

        lastFI = mfi.createFixedObject(va.getValVT().getSizeInBits()/8, va.getLocMemOffset(), true);
        SDValue fin = dag.getFrameIndex(lastFI, new EVT(getPointerTy()), false);
        inVals.add(dag.getLoad(va.getValVT(), chain, fin, getFixedStack(lastFI), 0, false,  0));
      }
    }

    // The mips ABIs for returning structs by value requires that we copy
    // the sret argument into $v0 for the return. Save the argument into
    // a virtual register so that we can access it from the return points.
    if (mf.getFunction().hasStructRetAttr()) {
      int reg = mipsFI.getSRetReturnReg();
      if (reg == 0) {
        reg = mf.getMachineRegisterInfo().createVirtualRegister(getRegClassFor(new EVT(MVT.i32)));
        mipsFI.setSRetReturnReg(reg);
      }

      SDValue copy = dag.getCopyToReg(dag.getEntryNode(), reg, inVals.get(0));
      chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), copy, chain);
    }

    if (varArg && subtarget.isABI_O32()) {
      // Record the frame index of the first variable argument
      // which is a value necessary to VASTART.
      int nextStackOffset = ccInfo.getNextStackOffset();
      Util.assertion(nextStackOffset % 4 == 0, "nextStackOffset must be aligned to 4-byte boundary");
      lastFI = mfi.createFixedObject(4, nextStackOffset, true);
      mipsFI.setVarArgsFrameINdex(lastFI);

      for (; nextStackOffset < 16; nextStackOffset += 4) {
        MCRegisterClass rc = MipsGenRegisterInfo.CPURegsRegisterClass;
        int idx = nextStackOffset / 4;
        int reg = mf.addLiveIn(O32IntRegs[idx], rc);
        SDValue argValue = dag.getCopyFromReg(chain, reg, new EVT(MVT.i32));
        lastFI = mfi.createFixedObject(4, nextStackOffset, true);
        SDValue ptrOff = dag.getFrameIndex(lastFI, new EVT(getPointerTy()), false);
        outChains.add(dag.getStore(chain, argValue, ptrOff, null, 0, false, 0));
      }
    }

    mipsFI.setLastInArgFI(lastFI);

    if (!outChains.isEmpty()) {
      outChains.add(chain);
      chain = dag.getNode(ISD.TokenFactor, new EVT(MVT.Other), outChains);
    }
    return chain;
  }

  /**
   * Return the next argument register on the O32 model.
   * @param reg
   * @return
   */
  private static int getNextIntArgReg(int reg) {
    Util.assertion(reg == MipsGenRegisterNames.A0 || reg == MipsGenRegisterNames.A2);
    return reg == MipsGenRegisterNames.A0 ? MipsGenRegisterNames.A1 : MipsGenRegisterNames.A3;
  }

  private static final int O32IntRegsSize = 4;

  private static final int[] O32IntRegs = {
      MipsGenRegisterNames.A0, MipsGenRegisterNames.A1, MipsGenRegisterNames.A2, MipsGenRegisterNames.A3
  };

  private static void readByValArg(MachineFunction mf,
                                   SDValue chain,
                                   ArrayList<SDValue> outChains,
                                   SelectionDAG dag,
                                   int numWords,
                                   SDValue fin,
                                   CCValAssign va,
                                   ArgFlagsTy flags) {
    int locMem = va.getLocMemOffset();
    int firstWord = locMem / 4;

    for (int i = 0; i < numWords; ++i) {
      int curWord = firstWord + i;
      if (curWord >= O32IntRegsSize)
        break;

      int srcReg = O32IntRegs[curWord];
      int reg = mf.addLiveIn(srcReg, MipsGenRegisterInfo.CPURegsRegisterClass);
      SDValue storePtr = dag.getNode(ISD.ADD, new EVT(MVT.i32), fin, dag.getConstant(i*4, new EVT(MVT.i32), false));
      SDValue store = dag.getStore(chain, dag.getRegister(reg, new EVT(MVT.i32)), storePtr,
          null, 0, false, 0);
      outChains.add(store);
    }
  }

  @Override
  public SDValue lowerReturn(SDValue chain,
                             CallingConv cc,
                             boolean isVarArg,
                             ArrayList<OutputArg> outs,
                             SelectionDAG dag) {
    MachineFunction mf = dag.getMachineFunction();

    ArrayList<CCValAssign> retLocs = new ArrayList<>();
    CCState ccInfo = new CCState(cc, isVarArg, tm, retLocs, dag.getContext());
    ccInfo.analyzeReturn(outs, MipsGenCallingConv.RetCC_Mips);

    if (mf.getMachineRegisterInfo().isLiveOutEmpty()) {
      retLocs.stream().filter(CCValAssign::isRegLoc).forEach(loc-> {
        mf.getMachineRegisterInfo().addLiveOut(loc.getLocReg());
      });
    }

    SDValue inFlag = new SDValue();
    int i = 0;
    for (CCValAssign loc : retLocs) {
      Util.assertion(loc.isRegLoc(), "return value must be passed by register!");
      chain = dag.getCopyToReg(chain, loc.getLocReg(), outs.get(i++).val, inFlag);
      inFlag = chain.getValue(1);
    }

    // Handle the sret structure attribute.
    if (mf.getFunction().hasStructRetAttr()) {
      MipsFunctionInfo mipsFI = (MipsFunctionInfo) mf.getInfo();
      int sretReg = mipsFI.getSRetReturnReg();
      Util.assertion(sretReg != 0, "the sret register should be allocated in the front block!");

      SDValue sretVal = dag.getCopyFromReg(chain, sretReg, new EVT(getPointerTy()));
      // Copy the value of sretReg to the V0.
      chain = dag.getCopyToReg(chain, MipsGenRegisterNames.V0, sretVal);
      inFlag = chain.getValue(1);
    }

    // ret $ra
    if (inFlag.getNode() != null)
      return dag.getNode(MipsISD.Ret, new EVT(MVT.Other), chain, dag.getRegister(MipsGenRegisterNames.RA, new EVT(MVT.i32)), inFlag);
    else
      return dag.getNode(MipsISD.Ret, new EVT(MVT.Other), chain, dag.getRegister(MipsGenRegisterNames.RA, new EVT(MVT.i32)));
  }

  @Override
  public MachineBasicBlock emitInstrWithCustomInserter(MachineInstr mi,
                                                       MachineBasicBlock mbb) {
    Util.shouldNotReachHere();
    return null;
  }

  @Override
  public String getTargetNodeName(int opcode) {
    switch (opcode) {
      case MipsISD.JmpLink:           return "MipsISD.JmpLink";
      case MipsISD.Hi:                return "MipsISD.Hi";
      case MipsISD.Lo:                return "MipsISD.Lo";
      case MipsISD.GPRel:             return "MipsISD.GPRel";
      case MipsISD.TlsGd:             return "MipsISD.TlsGd";
      case MipsISD.TprelHi:           return "MipsISD.TprelHi";
      case MipsISD.TprelLo:           return "MipsISD.TprelLo";
      case MipsISD.ThreadPointer:     return "MipsISD.ThreadPointer";
      case MipsISD.Ret:               return "MipsISD.Ret";
      case MipsISD.FPBrcond:          return "MipsISD.FPBrcond";
      case MipsISD.FPCmp:             return "MipsISD.FPCmp";
      case MipsISD.CMovFP_T:          return "MipsISD.CMovFP_T";
      case MipsISD.CMovFP_F:          return "MipsISD.CMovFP_F";
      case MipsISD.FPRound:           return "MipsISD.FPRound";
      case MipsISD.MAdd:              return "MipsISD.MAdd";
      case MipsISD.MAddu:             return "MipsISD.MAddu";
      case MipsISD.MSub:              return "MipsISD.MSub";
      case MipsISD.MSubu:             return "MipsISD.MSubu";
      case MipsISD.DivRem:            return "MipsISD.DivRem";
      case MipsISD.DivRemU:           return "MipsISD.DivRemU";
      case MipsISD.BuildPairF64:      return "MipsISD.BuildPairF64";
      case MipsISD.ExtractElementF64: return "MipsISD.ExtractElementF64";
      case MipsISD.WrapperPIC:        return "MipsISD.WrapperPIC";
      case MipsISD.DynAlloc:          return "MipsISD.DynAlloc";
      case MipsISD.Sync:              return "MipsISD.Sync";
      case MipsISD.Ext:               return "MipsISD.Ext";
      case MipsISD.Ins:               return "MipsISD.Ins";
      default:                         return null;
    }
  }

  @Override
  public int getSetCCResultType(EVT vt) {
    return MVT.i32;
  }

  @Override
  public SDValue performDAGCombine(SDNode n, DAGCombinerInfo combineInfo) {
    SelectionDAG dag = combineInfo.dag;
    int opc = n.getOpcode();
    switch (opc) {
      default: break;
      case ISD.ADDE:
        return performADDECombine(n, dag, combineInfo, subtarget);
      case ISD.SUBE:
        return performSUBECombine(n, dag, combineInfo, subtarget);
      case ISD.SDIVREM:
      case ISD.UDIVREM:
        return performDivRemCombine(n, dag, combineInfo, subtarget);
      case ISD.SETCC:
        return performSETCCCombine(n, dag, combineInfo, subtarget);
      case ISD.AND:
        return performANDCombine(n, dag, combineInfo, subtarget);
      case ISD.OR:
        return performORCombine(n, dag, combineInfo, subtarget);
    }
    return new SDValue();
  }

  private static SDValue performORCombine(SDNode n,
                                          SelectionDAG dag,
                                          DAGCombinerInfo combineInfo,
                                          MipsSubtarget subtarget) {
    Util.shouldNotReachHere();
    return null;
  }

  private static SDValue performANDCombine(SDNode n,
                                           SelectionDAG dag,
                                           DAGCombinerInfo combineInfo,
                                           MipsSubtarget subtarget) {
    Util.shouldNotReachHere();
    return null;
  }

  private static SDValue performSETCCCombine(SDNode n,
                                             SelectionDAG dag,
                                             DAGCombinerInfo combineInfo,
                                             MipsSubtarget subtarget) {
    Util.shouldNotReachHere();
    return null;
  }

  private static SDValue performDivRemCombine(SDNode n,
                                              SelectionDAG dag,
                                              DAGCombinerInfo combineInfo,
                                              MipsSubtarget subtarget) {
    Util.shouldNotReachHere();
    return null;
  }

  private static SDValue performSUBECombine(SDNode n,
                                            SelectionDAG dag,
                                            DAGCombinerInfo combineInfo,
                                            MipsSubtarget subtarget) {
    Util.shouldNotReachHere();
    return null;
  }

  private static SDValue performADDECombine(SDNode n,
                                            SelectionDAG dag,
                                            DAGCombinerInfo combineInfo,
                                            MipsSubtarget subtarget) {
    Util.shouldNotReachHere();
    return null;
  }
}
