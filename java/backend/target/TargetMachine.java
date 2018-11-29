package backend.target;

import backend.codegen.MachineCodeEmitter;
import backend.mc.MCAsmInfo;
import backend.passManaging.FunctionPassManager;
import backend.passManaging.PassManagerBase;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Primary interface to complete machine description for the backend.target machine.
 * Our goal is that all backend.target-specific information should accessible through
 * this interface.
 *
 * @author Jianping Zeng
 * @version 0.1
 * @see TargetData
 */
public abstract class TargetMachine {
  /**
   * Code generation optimization level.
   */
  public enum CodeGenOpt {
    None,
    Less,
    Default,
    Aggressive
  }

  /**
   * hese enums are meant to be passed into
   * addPassesToEmitFile to indicate what type of file to emit.
   */
  public enum CodeGenFileType {
    AssemblyFile, ObjectFile, DynamicLibrary
  }

  public enum CodeModel {
    Default,
    Small,
    Kernel,
    Medium,
    Large
  }

  public enum RelocModel {
    Default,
    Static,
    PIC_,
    DynamicNoPIC
  }

  protected CodeModel codeModel;

  protected RelocModel relocModel;

  protected Target theTarget;

  protected MCAsmInfo asmInfo;

  protected boolean asmVerbosityDefault;

  /**
   * Can only called by subclass.
   */
  protected TargetMachine(Target target) {
    theTarget = target;
    codeModel = CodeModel.Default;
    relocModel = RelocModel.Default;
  }

  public Target getTarget() {
    return theTarget;
  }

  public TargetData getTargetData() {
    return null;
  }

  public MCAsmInfo getMCAsmInfo() {
    return asmInfo;
  }

  public CodeModel getCodeModel() {
    return codeModel;
  }

  public void setCodeModel(CodeModel model) {
    codeModel = model;
  }

  public RelocModel getRelocationModel() {
    return relocModel;
  }

  public void setRelocationModel(RelocModel model) {
    relocModel = model;
  }

  public void setAsmVerbosityDefault(boolean val) {
    asmVerbosityDefault = val;
  }

  public boolean getAsmVerbosityDefault() {
    return asmVerbosityDefault;
  }

  // Interface to the major aspects of target machine information:
  // 1.Instruction opcode and operand information.
  // 2.Pipeline and scheduling information.
  // 3.Register information.
  // 4.Stack frame information.
  // 5.Cache hierarchy information.
  // 6.Machine-level optimization information (peepole only).
  public abstract TargetInstrInfo getInstrInfo();

  public abstract TargetRegisterInfo getRegisterInfo();

  public abstract TargetFrameInfo getFrameInfo();

  public abstract TargetLowering getTargetLowering();

  public TargetSubtarget getSubtarget() {
    return null;
  }

  public TargetIntrinsicInfo getIntrinsinsicInfo() {
    return null;
  }

  /**
   * Add passes to the specified pass manager to get the specified file emitted.
   * Typically this will involve several steps of code generation.
   * This method should return true if assembly emission is not supported.
   * <p>
   * This method should return FileModel::Error if emission of this file type
   * is not supported.
   * </p>
   * <p>
   * Note that: this method would be overriden by concrete subclass for
   * different backend.target, like IA32, Sparc.
   * </p>
   *
   * @param pm
   * @param asmOutStream
   * @param genFileType
   * @param optLevel
   * @return
   */
  public FileModel addPassesToEmitFile(
      PassManagerBase pm,
      OutputStream asmOutStream,
      CodeGenFileType genFileType,
      CodeGenOpt optLevel) {
    return FileModel.None;
  }

  /**
   * if the passes to emit the specified file had to be split up (e.g., to add
   * an object writer pass), this method can be used to finish up adding passes
   * to emit the file, if necessary.
   *
   * @param pm
   * @param mce
   * @param opt
   * @return
   */
  public boolean addPassesToEmitFileFinish(
      PassManagerBase pm,
      MachineCodeEmitter mce,
      CodeGenOpt opt) {
    return true;
  }

  /**
   * If the target want to support emission of ELF object code, so that this
   * method must be implemented aimed to generate ELF code.
   *
   * @param pm
   * @param os
   * @return
   */
  public MachineCodeEmitter addELFWriter(FunctionPassManager pm, PrintStream os) {
    return null;
  }
}
