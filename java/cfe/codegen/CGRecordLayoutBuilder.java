package cfe.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import backend.type.ArrayType;
import backend.type.Type;
import cfe.sema.ASTRecordLayout;
import cfe.sema.Decl;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.TreeSet;

import static backend.target.TargetData.roundUpAlignment;

/**
 * Record builder helper.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class CGRecordLayoutBuilder {
  private CodeGenTypes types;

  private boolean packed;
  private int alignment;
  private int alignmentAsLLVMStruct;
  private int bitsAvailableInLastField;
  private long nextFieldOffsetInBytes;
  private ArrayList<backend.type.Type> fieldTypes;

  private ArrayList<Pair<Decl.FieldDecl, Integer>> llvmFields;

  private static class LLVMBitFieldInfo {
    Decl.FieldDecl fd;
    int fieldNo;
    int start;
    int size;

    LLVMBitFieldInfo(Decl.FieldDecl fd, int fieldNo, int start, int size) {
      this.fd = fd;
      this.fieldNo = fieldNo;
      this.start = start;
      this.size = size;
    }
  }

  private ArrayList<LLVMBitFieldInfo> llvmBitFields;

  private CGRecordLayoutBuilder(CodeGenTypes types) {
    this.types = types;
    packed = false;
    alignment = 0;
    alignmentAsLLVMStruct = 1;
    bitsAvailableInLastField = 0;
    nextFieldOffsetInBytes = 0;
    fieldTypes = new ArrayList<>();
    llvmFields = new ArrayList<>();
    llvmBitFields = new ArrayList<>();
  }

  /**
   * Will layout a RecordDecl.
   *
   * @param d
   */
  private void layout(Decl.RecordDecl d) {
    alignment = (int) types.getContext().getASTRecordLayout(d).getAlignment() / 8;
    packed = false;
    if (d.isUnion()) {
      layoutUnion(d);
      return;
    }

    if (layoutFields(d))
      return;

    // We weren't able to layout the struct, try again with a packed struct.
    packed = true;
    alignmentAsLLVMStruct = 1;
    nextFieldOffsetInBytes = 0;
    fieldTypes.clear();
    llvmFields.clear();
    llvmBitFields.clear();
    layoutFields(d);
  }

  /**
   * Will layout a union RecordDecl.
   *
   * @param d
   */
  private void layoutUnion(Decl.RecordDecl d) {
    Util.assertion(d.isUnion(), "Cannot call layoutUnion on a non-uion decl");

    ASTRecordLayout layout = types.getContext().getASTRecordLayout(d);

    backend.type.Type ty = null;
    long size = 0;
    int align = 0;

    int fieldNo = 0;
    for (int i = 0, e = d.getNumFields(); i < e; i++, fieldNo++) {
      Decl.FieldDecl fd = d.getDeclAt(i);
      Util.assertion(layout.getFieldOffsetAt(fieldNo) == 0, "Union field offset did not start at the beginning of record!");


      if (fd.isBitField()) {
        long fieldSize = fd.getBitWidth().evaluateAsInt(types.getContext()).getZExtValue();

        // Ignores the sized bit fields.
        if (fieldSize == 0)
          continue;

        types.addBitFieldInfo(fd, 0, 0, (int) fieldSize);
      } else {
        types.addFieldInfo(fd, 0);
      }

      backend.type.Type fieldTy = types.convertTypeForMemRecursive(fd.getType());
      int fieldAlign = types.getTargetData().getABITypeAlignment(fieldTy);
      long fieldSize = types.getTargetData().getTypeAllocSize(fieldTy);

      if (fieldAlign < align)
        continue;
      if (fieldAlign > align || fieldSize > size) {
        ty = fieldTy;
        align = fieldAlign;
        size = fieldSize;
      }
    }

    if (ty != null) {
      appendField(0, ty);

      if (getTypeAlignment(ty) > layout.getAlignment() / 8) {
        packed = true;
        align = 1;
      }
    }

    // Append tail padding.
    if (layout.getSize() / 8 > size)
      appendPadding(layout.getSize() / 8, align);
  }

  /**
   * try to layout all fields in the record decl.
   * Returns false if the operation failed because the struct is not packed.
   *
   * @param d
   * @return
   */
  private boolean layoutFields(Decl.RecordDecl d) {
    Util.assertion(!d.isUnion(), "Can't call layoutFields on a union!");
    Util.assertion(alignment != 0, "Did not set alignment");

    ASTRecordLayout layout = types.getContext().getASTRecordLayout(d);

    for (int i = 0, e = d.getNumFields(); i < e; i++) {
      Decl.FieldDecl field = d.getDeclAt(i);
      if (!layoutField(field, layout.getFieldOffsetAt(i))) {
        Util.assertion(!packed, "Could not layout fields even with a packed LLVM struct!");
        return false;
      }
    }

    // Append tail padding if necessary.
    appendTailPadding(layout.getSize());
    return true;
  }

  /**
   * layout a single field. Returns false if the operation failed
   * because the current struct is not packed.
   *
   * @param d
   * @param fieldOffset
   * @return
   */
  private boolean layoutField(Decl.FieldDecl d, long fieldOffset) {
    if (d.isBitField()) {
      layoutBitField(d, fieldOffset);
      return true;
    }

    Util.assertion(fieldOffset % 8 == 0, "FieldOffset is not on a byte boundary!");
    long fieldOffsetInBytes = fieldOffset / 8;

    backend.type.Type ty = types.convertTypeForMemRecursive(d.getType());
    int typeAlignment = getTypeAlignment(ty);

    if (typeAlignment > alignment) {
      return false;
    }

    long alignedNextFieldOffsetInBytes = roundUpAlignment(nextFieldOffsetInBytes, typeAlignment);

    if (fieldOffsetInBytes < alignedNextFieldOffsetInBytes) {
      return false;
    }

    if (alignedNextFieldOffsetInBytes < fieldOffsetInBytes) {
      long paddingInBytes = fieldOffsetInBytes - nextFieldOffsetInBytes;
      appendBytes(paddingInBytes);
    }

    llvmFields.add(Pair.get(d, fieldTypes.size()));
    appendField(fieldOffsetInBytes, ty);
    return true;
  }

  /**
   * layout a single bit field.
   *
   * @param d
   * @param fieldOffset
   */
  private void layoutBitField(Decl.FieldDecl d, long fieldOffset) {
    long fieldSize = d.getBitWidth().evaluateAsInt(types.getContext()).getZExtValue();

    if (fieldSize == 0)
      return;

    long nextFieldOffset = nextFieldOffsetInBytes * 8;
    int numBytesToAppend;

    if (fieldOffset < nextFieldOffset) {
      Util.assertion(bitsAvailableInLastField != 0, "Bitfield size mismatch");
      Util.assertion(nextFieldOffsetInBytes != 0, "Must have laid out at least one bytes!");

      numBytesToAppend = (int) roundUpAlignment(fieldSize - bitsAvailableInLastField, 8) / 8;
    } else {
      Util.assertion(fieldOffset % 8 == 0, "Field offset not aligned correctly");

      appendBytes((fieldOffset - nextFieldOffset) / 8);
      numBytesToAppend = (int) roundUpAlignment(fieldSize, 8) / 8;

      Util.assertion(numBytesToAppend != 0, "No bytes to append!");
    }

    backend.type.Type ty = types.convertTypeForMemRecursive(d.getType());
    long typeSizeInBits = getTypeSizeInBytes(ty) * 8;
    llvmBitFields.add(new LLVMBitFieldInfo(d, (int) (fieldOffset / typeSizeInBits),
        (int) (fieldOffset % typeSizeInBits),
        (int) fieldSize));
    bitsAvailableInLastField = (int) (nextFieldOffsetInBytes * 8 - (fieldOffset + fieldSize));
  }

  /**
   * appends a field with the given offset and type.
   *
   * @param fieldOffsetInBytes
   * @param fieldTy
   */
  private void appendField(long fieldOffsetInBytes, backend.type.Type fieldTy) {
    alignmentAsLLVMStruct = Math.max(alignmentAsLLVMStruct, getTypeAlignment(fieldTy));
    long fieldSizeInBytes = getTypeSizeInBytes(fieldTy);
    fieldTypes.add(fieldTy);
    nextFieldOffsetInBytes = fieldOffsetInBytes + fieldSizeInBytes;
    bitsAvailableInLastField = 0;
  }

  /**
   * appends enough padding bytes so that the total struct
   * size matches the alignment of the passed in type.
   *
   * @param fieldOffsetInBytes
   * @param fieldTy
   */
  private void appendPadding(long fieldOffsetInBytes, backend.type.Type fieldTy) {
    int fieldAlignment = getTypeAlignment(fieldTy);
    appendPadding(fieldOffsetInBytes, fieldAlignment);
  }

  /**
   * appends enough padding bytes so that the total
   * struct size is a multiple of the field alignment.
   *
   * @param fieldOffsetInBytes
   * @param fieldAlignment
   */
  private void appendPadding(long fieldOffsetInBytes, int fieldAlignment) {
    Util.assertion(nextFieldOffsetInBytes <= fieldOffsetInBytes);

    long alignedNextFieldOffsetInBytes = roundUpAlignment(nextFieldOffsetInBytes, fieldAlignment);
    if (alignedNextFieldOffsetInBytes < fieldOffsetInBytes) {
      long paddingInBytes = fieldOffsetInBytes - nextFieldOffsetInBytes;
      appendBytes(paddingInBytes);
    }
  }

  /**
   * append a given number of bytes to the record.
   *
   * @param numBytes
   */
  private void appendBytes(long numBytes) {
    if (numBytes <= 0)
      return;
    backend.type.Type ty = Type.getInt8Ty(types.getLLVMContext());
    if (numBytes > 1)
      ty = ArrayType.get(ty, numBytes);

    appendField(nextFieldOffsetInBytes, ty);
  }

  /**
   * append enough tail padding so that the type will have
   * the passed size.
   *
   * @param recordSize
   */
  private void appendTailPadding(long recordSize) {
    Util.assertion(recordSize % 8 == 0, "Invalid record size");

    long recordSizeInBytes = recordSize / 8;
    Util.assertion(nextFieldOffsetInBytes <= recordSizeInBytes, "Size mismatch!");

    int numPadBytes = (int) (recordSizeInBytes - nextFieldOffsetInBytes);
    appendBytes(numPadBytes);
  }

  private int getTypeAlignment(backend.type.Type ty) {
    return types.getTargetData().getTypeAlign(ty);
  }

  private long getTypeSizeInBytes(backend.type.Type ty) {
    return types.getTargetData().getTypeSizeInBits(ty) / 8;
  }

  /**
   * Return the right record layout for a given record decl.
   *
   * @param types
   * @param d
   * @return
   */
  public static CGRecordLayout computeLayout(CodeGenTypes types,
                                             Decl.RecordDecl d) {
    CGRecordLayoutBuilder builder = new CGRecordLayoutBuilder(types);

    builder.layout(d);

    backend.type.Type ty = backend.type.StructType.get(types.getLLVMContext(), builder.fieldTypes, builder.packed);
    Util.assertion(types.getContext().getASTRecordLayout(d).getSize() / 8 == types.getTargetData().getTypeAllocSize(ty), "Type size mismatch");


    // Add all field numbers.
    for (int i = 0, e = builder.llvmFields.size(); i < e; i++) {

      Decl.FieldDecl fd = builder.llvmFields.get(i).first;
      int fieldNo = builder.llvmFields.get(i).second;

      types.addFieldInfo(fd, fieldNo);
    }

    // Add bitfield info.
    for (int i = 0, e = builder.llvmBitFields.size(); i < e; i++) {
      LLVMBitFieldInfo info = builder.llvmBitFields.get(i);
      types.addBitFieldInfo(info.fd, info.fieldNo, info.start, info.size);
    }

    return new CGRecordLayout(ty, new TreeSet<>());
  }
}