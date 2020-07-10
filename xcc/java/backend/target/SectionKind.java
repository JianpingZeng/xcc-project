package backend.target;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
import static backend.target.SectionKind.Kind.*;
/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class SectionKind {
  public enum Kind {
    /// Metadata - Debug info sections or other metadata.
    Metadata,

    /// Text - Text section, used for functions and other executable code.
    Text,

    /// ReadOnly - Data that is never written to at program runtime by the
    /// program or the dynamic linker.  Things in the top-level readonly
    /// SectionKind are not mergeable.
    ReadOnly,

    /// MergableCString - Any null-terminated string which allows merging.
    /// These values are known to end in a nul value of the specified size,
    /// not otherwise contain a nul value, and be mergable.  This allows the
    /// linker to unique the strings if it so desires.

    /// Mergeable1ByteCString - 1 byte mergable, null terminated, string.
    Mergeable1ByteCString,

    /// Mergeable2ByteCString - 2 byte mergable, null terminated, string.
    Mergeable2ByteCString,

    /// Mergeable4ByteCString - 4 byte mergable, null terminated, string.
    Mergeable4ByteCString,

    /// MergeableConst - These are sections for merging fixed-length
    /// ants together.  For example, this can be used to unique
    /// ant pool entries etc.
    MergeableConst,

    /// MergeableConst4 - This is a section used by 4-byte ants,
    /// for example, floats.
    MergeableConst4,

    /// MergeableConst8 - This is a section used by 8-byte ants,
    /// for example, doubles.
    MergeableConst8,

    /// MergeableConst16 - This is a section used by 16-byte ants,
    /// for example, vectors.
    MergeableConst16,

    /// Writeable - This is the base of all segments that need to be written
    /// to during program runtime.

    /// ThreadLocal - This is the base of all TLS segments.  All TLS
    /// objects must be writeable, otherwise there is no reason for them to
    /// be thread local!

    /// ThreadBSS - Zero-initialized TLS data objects.
    ThreadBSS,

    /// ThreadData - Initialized TLS data objects.
    ThreadData,

    /// GlobalWriteableData - Writeable data that is global (not thread
    /// local).

    /// BSS - Zero initialized writeable data.
    BSS,

    /// BSSLocal - This is BSS (zero initialized and writable) data
    /// which has local linkage.
    BSSLocal,

    /// BSSExtern - This is BSS data with normal external linkage.
    BSSExtern,

    /// Common - Data with common linkage.  These represent tentative
    /// definitions, which always have a zero initializer and are never
    /// marked 'ant'.
    Common,

    /// DataRel - This is the most general form of data that is written
    /// to by the program, it can have random relocations to arbitrary
    /// globals.
    DataRel,

    /// DataRelLocal - This is writeable data that has a non-zero
    /// initializer and has relocations in it, but all of the
    /// relocations are known to be within the final linked image
    /// the global is linked into.
    DataRelLocal,

    /// DataNoRel - This is writeable data that has a non-zero
    /// initializer, but whose initializer is known to have no
    /// relocations.
    DataNoRel,

    /// ReadOnlyWithRel - These are global variables that are never
    /// written to by the program, but that have relocations, so they
    /// must be stuck in a writeable section so that the dynamic linker
    /// can write to them.  If it chooses to, the dynamic linker can
    /// mark the pages these globals end up on as read-only after it is
    /// done with its relocation phase.
    ReadOnlyWithRel,

    /// ReadOnlyWithRelLocal - This is data that is readonly by the
    /// program, but must be writeable so that the dynamic linker
    /// can perform relocations in it.  This is used when we know
    /// that all the relocations are to globals in this final
    /// linked image.
    ReadOnlyWithRelLocal
  }

  private Kind K;
  public boolean isMetadata()  { return K == Metadata; }
  public boolean isText()  { return K == Text; }

  public boolean isReadOnly()  {
    return K == ReadOnly || isMergeableCString() ||
        isMergeableConst();
  }

  public boolean isMergeableCString()  {
    return K == Mergeable1ByteCString || K == Mergeable2ByteCString ||
        K == Mergeable4ByteCString;
  }
  public boolean isMergeable1ByteCString()  { return K == Mergeable1ByteCString; }
  public boolean isMergeable2ByteCString()  { return K == Mergeable2ByteCString; }
  public boolean isMergeable4ByteCString()  { return K == Mergeable4ByteCString; }

  public boolean isMergeableConst()  {
    return K == MergeableConst || K == MergeableConst4 ||
        K == MergeableConst8 || K == MergeableConst16;
  }
  public boolean isMergeableConst4()  { return K == MergeableConst4; }
  public boolean isMergeableConst8()  { return K == MergeableConst8; }
  public boolean isMergeableConst16()  { return K == MergeableConst16; }

  public boolean isWriteable()  {
    return isThreadLocal() || isGlobalWriteableData();
  }

  public boolean isThreadLocal()  {
    return K == ThreadData || K == ThreadBSS;
  }

  public boolean isThreadBSS()  { return K == ThreadBSS; }
  public boolean isThreadData()  { return K == ThreadData; }

  public boolean isGlobalWriteableData()  {
    return isBSS() || isCommon() || isDataRel() || isReadOnlyWithRel();
  }

  public boolean isBSS()  { return K == BSS || K == BSSLocal || K == BSSExtern; }
  public boolean isBSSLocal()  { return K == BSSLocal; }
  public boolean isBSSExtern()  { return K == BSSExtern; }

  public boolean isCommon()  { return K == Common; }

  public boolean isDataRel()  {
    return K == DataRel || K == DataRelLocal || K == DataNoRel;
  }

  public boolean isDataRelLocal()  {
    return K == DataRelLocal || K == DataNoRel;
  }

  public boolean isDataNoRel()  { return K == DataNoRel; }

  public boolean isReadOnlyWithRel()  {
    return K == ReadOnlyWithRel || K == ReadOnlyWithRelLocal;
  }

  public boolean isReadOnlyWithRelLocal()  {
    return K == ReadOnlyWithRelLocal;
  }
  private static SectionKind get(Kind K) {
    SectionKind Res = new SectionKind();
    Res.K = K;
    return Res;
  }

  public static SectionKind getMetadata() { return get(Metadata); }
  public static SectionKind getText() { return get(Text); }
  public static SectionKind getReadOnly() { return get(ReadOnly); }
  public static SectionKind getMergeable1ByteCString() {
    return get(Mergeable1ByteCString);
  }
  public static SectionKind getMergeable2ByteCString() {
    return get(Mergeable2ByteCString);
  }
  public static SectionKind getMergeable4ByteCString() {
    return get(Mergeable4ByteCString);
  }
  public static SectionKind getMergeableConst() { return get(MergeableConst); }
  public static SectionKind getMergeableConst4() { return get(MergeableConst4); }
  public static SectionKind getMergeableConst8() { return get(MergeableConst8); }
  public static SectionKind getMergeableConst16() { return get(MergeableConst16); }
  public static SectionKind getThreadBSS() { return get(ThreadBSS); }
  public static SectionKind getThreadData() { return get(ThreadData); }
  public static SectionKind getBSS() { return get(BSS); }
  public static SectionKind getBSSLocal() { return get(BSSLocal); }
  public static SectionKind getBSSExtern() { return get(BSSExtern); }
  public static SectionKind getCommon() { return get(Common); }
  public static SectionKind getDataRel() { return get(DataRel); }
  public static SectionKind getDataRelLocal() { return get(DataRelLocal); }
  public static SectionKind getDataNoRel() { return get(DataNoRel); }
  public static SectionKind getReadOnlyWithRel() { return get(ReadOnlyWithRel); }
  public static SectionKind getReadOnlyWithRelLocal(){
    return get(ReadOnlyWithRelLocal);
  }
}
