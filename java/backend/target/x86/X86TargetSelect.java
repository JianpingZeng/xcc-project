package backend.target.x86;

import backend.target.TargetSelect;

public class X86TargetSelect extends TargetSelect {
  public X86TargetSelect() {
    super();
  }

  public void InitializeTargetInfo() {
    X86TargetInfo.InitializeX86TargetInfo();
  }

  public void LLVMInitializeTarget() {
    X86TargetInfo.LLVMInitiliazeX86Target();
  }
}