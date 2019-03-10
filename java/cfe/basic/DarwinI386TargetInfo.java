package cfe.basic;

import backend.support.Triple;
import cfe.support.LangOptions;

public class DarwinI386TargetInfo extends TargetInfo.X86_32TargetInfo implements DarwinTargetInfo {
  public DarwinI386TargetInfo(String triple) {
    super(triple);
    TLSSupported = getTLSSupported();
    longDoubleWidth = 128;
    longDoubleAlign = 128;
    sizeType = IntType.UnsignedLongLong;
    intPtrType = IntType.SignedLong;
    descriptionString = "e-p:32:32:32-i1:8:8-i8:8:8-i16:16:16-i32:32:32-" +
        "i64:32:64-f32:32:32-f64:32:64-v64:64:64-v128:128:128-" +
        "a0:0:64-f80:128:128";
  }

  public void getDarwinLanguageOptions(LangOptions opts, String triple) {
    Triple theTriple = new Triple(triple);
    if (theTriple.getOS() != Triple.OSType.Darwin)
      return;
    int major = theTriple.getDarwinMajorNumber();
    if (major > 9) {
      opts.blocks = true;
      opts.setStackProtectMode(LangOptions.StackProtectMode.SSPOn);
    }
  }

  @Override
  public void getDefaultLangOptions(LangOptions langOpts) {
    super.getDefaultLangOptions(langOpts);
    getDarwinLanguageOptions(langOpts, getTargetTriple());
  }
}
