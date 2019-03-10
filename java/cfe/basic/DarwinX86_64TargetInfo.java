package cfe.basic;

public class DarwinX86_64TargetInfo extends TargetInfo.X86_64TargetInfo implements DarwinTargetInfo {
  public DarwinX86_64TargetInfo(String triple) {
    super(triple);
    int64Type = IntType.SignedLong;
  }
}
