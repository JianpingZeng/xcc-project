package jlang.basic;

import backend.support.Triple;
import jlang.support.LangOptions;

import static jlang.basic.TargetInfo.define;

public interface DarwinTargetInfo extends TargetInfo.OSTargetInfo {
  default boolean getTLSSupported() {
    return false;
  }

  default void getDarwinDefines(StringBuilder defs, LangOptions langOpt) {
    define(defs, "__APPLE_CC__", "5621");
    define(defs, "__APPLE__");
    define(defs, "__MACH__");

    define(defs, "__weak", "");
    define(defs, "__strong", "");
  }

  default void getDarwinOSXDefines(StringBuilder defs, String striple) {
    Triple tri = new Triple(striple);
    if (tri.getOS() != Triple.OSType.Darwin)
      return;

    int[] versions = new int[3];
    tri.getDarwinNumber(versions);
    int major = versions[0], minor = versions[1], rev = versions[2];
    StringBuilder macosStr = new StringBuilder("1000");
    if (major >= 4 && major <= 13) {
      // 10.0 - 10.9
      // darwin7 -> 1030, darwin8 -> 1040, darwin9 ->1050, etc
      macosStr.setCharAt(2, (char) ('0' + (major - 4)));
    }

    macosStr.setCharAt(3, (char) (Math.min(minor, 9) + '0'));
    ;
    define(defs, "__ENVIRONMENT_MAC_OS_X_VERSION_MIN_REQUIRTED__", macosStr.toString());
  }

  @Override
  default void getOSDefines(LangOptions langOpts, String triple, StringBuilder defs) {
    getDarwinDefines(defs, langOpts);
    getDarwinOSXDefines(defs, triple);
  }

  default String getUnicodeStringSymbolPrefix() {
    return "__utf16_string_";
  }

  default String getUnicodeStringSection() {
    return "__TEXT,__ustring";
  }
}
