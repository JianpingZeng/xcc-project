package backend.target.x86;

import backend.codegen.ELFWriter;
import backend.codegen.MachineCodeEmitter;
import backend.passManaging.FunctionPassManager;
import backend.passManaging.PassManagerBase;
import backend.target.*;
import tools.Util;

import java.io.PrintStream;

import static backend.support.BackendCmdOptions.InstructionSelector;
import static backend.target.TargetMachine.CodeModel.Small;
import static backend.target.TargetMachine.RelocModel.*;
import static backend.target.TargetOptions.OverrideStackAlignment;
import static backend.target.x86.X86CodeEmitter.createX86CodeEmitterPass;
import static backend.target.x86.X86FloatingPointRegKill.createX86FPRegKillPass;
import static backend.target.x86.X86FloatingPointStackifier.createX86FPStackifierPass;
import static backend.target.x86.X86Subtarget.PICStyle.*;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class X86TargetMachine extends LLVMTargetMachine {
  /**
   * A stack frame info class used for organizing data layout of frame when
   * function calling.
   */
  private X86FrameLowering frameInfo;
  private X86Subtarget subtarget;
  private TargetData dataLayout;
  private X86TargetLowering tli;
  private RelocModel defRelocModel;

  public X86TargetMachine(Target t, String triple,
                          String cpu, String fs,
                          boolean is64Bit) {
    super(t, triple);
    subtarget = new X86Subtarget(this, triple, cpu, fs, OverrideStackAlignment.value, is64Bit);
    dataLayout = new TargetData(subtarget.getDataLayout());
    frameInfo = new X86FrameLowering(this, subtarget);
    tli = new X86TargetLowering(this);

    defRelocModel = getRelocationModel();

    if (getRelocationModel() == RelocModel.Default) {
      if (!subtarget.isTargetDarwin())
        setRelocationModel(Static);
      else if (subtarget.is64Bit())
        setRelocationModel(PIC_);
      else
        setRelocationModel(DynamicNoPIC);
    }

    Util.assertion(getRelocationModel() != Default, "Relocation mode not picked");
    if (getCodeModel() == CodeModel.Default)
      setCodeModel(Small);

    // ELF and X86-64 don't have a distinct DynamicNoPIC model.  DynamicNoPIC
    // is defined as a model for code which may be used in static or dynamic
    // executables but not necessarily a shared library. On X86-32 we just
    // compile in -static mode, in x86-64 we use PIC.
    if (getRelocationModel() == DynamicNoPIC) {
      if (is64Bit)
        setRelocationModel(PIC_);
      else if (!subtarget.isTargetDarwin())
        setRelocationModel(Static);
    }

    // If we are on Darwin, disallow static relocation model in X86-64 mode, since
    // the Mach-O file format doesn't support it.
    if (getRelocationModel() == Static &&
        subtarget.isTargetDarwin() &&
        is64Bit) {
      setRelocationModel(PIC_);
    }

    // Determine the PICStyle based on the target selected.
    if (getRelocationModel() == Static) {
      // Unless we're in PIC or DynamicNoPIC mode, set the PIC style to None.
      subtarget.setPICStyle(None);
    } else if (subtarget.isTargetCygMing()) {
      subtarget.setPICStyle(None);
    } else if (subtarget.isTargetDarwin()) {
      if (subtarget.is64Bit())
        subtarget.setPICStyle(RIPRel);
      else if (getRelocationModel() == PIC_)
        subtarget.setPICStyle(StubPIC);
      else {
        Util.assertion((getRelocationModel() == DynamicNoPIC));
        subtarget.setPICStyle(StubDynamicNoPIC);
      }
    } else if (subtarget.isTargetELF()) {
      if (subtarget.is64Bit())
        subtarget.setPICStyle(RIPRel);
      else
        subtarget.setPICStyle(GOT);
    }
    // Finally, if we have "none" as our PIC style, force to static mode.
    if (subtarget.getPICStyle() == None)
      setRelocationModel(Static);
  }

  @Override
  public void setCodeModelForStatic() {
    if (getCodeModel() != CodeModel.Default) return;
    // For static codegen, if we're not already set, use Small codegen.
    super.setCodeModelForStatic();
  }

  @Override
  public X86Subtarget getSubtarget() {
    return subtarget;
  }

  @Override
  public TargetData getTargetData() {
    return dataLayout;
  }

  /**
   * You should directly call argetSubtarget::getInstrInfo().
   * @return
   */
  @Deprecated
  @Override
  public TargetInstrInfo getInstrInfo() {
    return subtarget.getInstrInfo();
  }

  /**
   * You should directly call argetSubtarget::getRegisterInfo().
   * @return
   */
  @Deprecated
  @Override
  public TargetRegisterInfo getRegisterInfo() {
    return subtarget.getRegisterInfo();
  }

  public TargetFrameLowering getFrameLowering() {
    return frameInfo;
  }

  @Override
  public X86TargetLowering getTargetLowering() {
    return tli;
  }

  @Override
  public boolean addInstSelector(PassManagerBase pm, CodeGenOpt level) {
    //pm.add(createX86FastISel(this, level));
    switch (InstructionSelector.value) {
      case DAGISel:
        pm.add(X86DAGISel.createX86DAGISel(this, level));
        break;
      default:
        Util.shouldNotReachHere("Unknown Instruction Selector");
    }

    // FIXME dead mi elim pass eliminates used instr. 2018/1/6
    //pm.add(createDeadMachineInstructionElimPass());
    pm.add(createX86FPRegKillPass());
    return false;
  }

  @Override
  public boolean addPreRegAlloc(PassManagerBase pm, CodeGenOpt level) {
    pm.add(MSAC.createMaxStackAlignmentCalculatorPass());
    return false;
  }

  @Override
  public boolean addPostRegAlloc(PassManagerBase pm, CodeGenOpt level) {
    // converts virtual register in X86 FP inst into floating point stack slot.
    pm.add(createX86FPStackifierPass());
    return false;
  }

  @Override
  public boolean addCodeEmitter(PassManagerBase pm, CodeGenOpt level,
                                MachineCodeEmitter mce) {
    pm.add(createX86CodeEmitterPass(this, mce));
    return false;
  }

  @Override
  public MachineCodeEmitter addELFWriter(
      FunctionPassManager pm,
      PrintStream os) {
    ELFWriter writer = new ELFWriter(os, this);
    pm.add(writer);
    return writer.getMachineCodeEmitter();
  }
}
