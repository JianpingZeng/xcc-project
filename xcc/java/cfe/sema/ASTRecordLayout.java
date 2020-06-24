package cfe.sema;
/*
 * Extremely C language CompilerInstance
 * Copyright (c) 2015-2020, Jianping Zeng
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

import cfe.ast.Tree.Expr;
import cfe.sema.Decl.FieldDecl;
import cfe.sema.Decl.RecordDecl;
import cfe.type.ArrayType;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class contains the layout information for a {@linkplain RecordDecl}
 * whic is a struct/union class. The decl represented must be definition.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class ASTRecordLayout {
  /**
   * Size of record in bytes.
   */
  private long size;

  /**
   * Size of record data without tailing padding.
   */
  private long dataSize;

  /**
   * An arrays of field offset in bits.
   */
  private long[] fieldOffsets;

  /**
   * Alignment of record in bytes.
   */
  private long alignment;

  /**
   * The number of fields.
   */
  private int fieldCount;

  private ASTRecordLayout(long size, long dataSize,
                          long[] fieldOffsets, long alignment) {
    this.size = size;
    this.dataSize = dataSize;
    this.alignment = alignment;
    fieldCount = fieldOffsets.length;
    if (fieldCount > 0) {
      this.fieldOffsets = new long[fieldCount];
      System.arraycopy(fieldOffsets, 0, this.fieldOffsets, 0, fieldCount);
    }
  }

  public long getSize() {
    return size;
  }

  public long getDataSize() {
    return dataSize;
  }

  public long getFieldCount() {
    return fieldCount;
  }

  public long getAlignment() {
    return alignment;
  }

  public long getFieldOffsetAt(int fieldNo) {
    Util.assertion(fieldNo >= 0 && fieldNo < fieldCount, "Invalid field No!");
    return fieldOffsets[fieldNo];
  }

  /**
   * A cache diagMapping from already parsed RecordDecl to its corresponding
   * ASTRecordLayout.
   */
  private static HashMap<RecordDecl, ASTRecordLayout> recordLayouts
      = new HashMap<>();

  /**
   * Computes information for record layout, which suggests getNumOfSubLoop and field
   * offset information.
   *
   * @param decl
   * @return
   */
  public static ASTRecordLayout getRecordLayout(ASTContext ctx, RecordDecl decl) {
    Util.assertion(decl.isCompleteDefinition(), "Can not layout the forward declaration.");
    ASTRecordLayout entity = recordLayouts.get(decl);
    if (recordLayouts.containsKey(decl))
      return entity;

    ASTRecordLayoutBuilder builder = new ASTRecordLayoutBuilder(ctx);
    builder.layout(decl);

    ASTRecordLayout newEntry = new ASTRecordLayout(builder.getSize(),
        builder.getDataSize(),
        builder.getFieldOffsets(),
        builder.getAlignment());

    // Caches the computed decl.
    recordLayouts.put(decl, newEntry);
    return newEntry;
  }

  /**
   * @author Jianping Zeng
   * @version 0.4
   */
  public static class ASTRecordLayoutBuilder {
    /**
     * The current size of the record layout.
     */
    private long size;
    /**
     * The current alignment of the record layout.
     */
    private long alignment;

    /**
     * The alignment if attributed packed is not used.
     */
    private long maxFieldAlignment;

    /**
     * The array bolds field offset in bits.
     */
    private ArrayList<Long> fieldOffsets;

    /**
     * The flags indicates if this record is packed or not.
     */
    private boolean packed;

    private boolean isUnion;

    private ASTContext ctx;
    private long nextOffset;

    public ASTRecordLayoutBuilder(ASTContext context) {
      size = 0;
      alignment = 8;
      maxFieldAlignment = 0;
      fieldOffsets = new ArrayList<>(16);
      packed = false;
      isUnion = false;
      ctx = context;
    }

    /**
     * Sets the layout information for RecordDecl.
     *
     * @return
     */
    public void layout(RecordDecl d) {
      initializeLayout(d);
      layoutFields(d);

      // Finally, round the size of the total struct up to the
      // alignment of the struct itself.
      finishLayout(d);
    }

    private void initializeLayout(RecordDecl d) {
      isUnion = d.isUnion();
      // TODO Set the packed attributes, pragma attribute, align attribute etc.
    }

    /**
     * Layouts each field, for now, just sequentially respecting alignment.
     * In the future, this will need to be tweakable by targets.
     *
     * @param d
     */
    private void layoutFields(RecordDecl d) {
      for (int i = 0; i < d.getDeclCounts(); i++) {
        layoutField(d.getDeclAt(i));
      }
    }

    private void layoutField(FieldDecl fd) {
      long fieldOffset = isUnion ? 0 : getDataSize();
      long fieldSize;
      int fieldAlign;
      if (fd.isBitField()) {
        Expr bitWidthExpr = fd.getBitWidth();
        fieldSize = bitWidthExpr.evaluateAsInt(ctx).getZExtValue();

        Pair<Long, Integer> fieldInfo = ctx.getTypeInfo(fd.getType());
        long typeSize = fieldInfo.first;
        fieldAlign = fieldInfo.second;

        if (packed)
          fieldAlign = 1;

        // The maximum field alignment overrides the aligned attribute.
        if (maxFieldAlignment != 0)
          fieldAlign = (int) Math.min(fieldAlign, maxFieldAlignment);

        // Check if we need to add padding to give the field the correct
        // alignment.
        if (fieldSize == 0 || (fieldOffset & (fieldAlign - 1)) + fieldSize > typeSize)
          fieldOffset = (fieldOffset + (fieldAlign - 1)) & ~(fieldAlign - 1);  // alignment.

        // Padding members don't affect overall alignment
        if (fd.getIdentifier() == null)
          fieldAlign = 1;
      } else {
        if (fd.getType().isIncompleteArrayType()) {
          // This is a flexible array member; we can't directly
          // query getTypeInfo about these, so we figure it out here.
          // Flexible array members don't have any getNumOfSubLoop, but they
          // have to be aligned appropriately for their element jlang.type.'
          fieldSize = 0;
          ArrayType ty = ctx.getAsArrayType(fd.getType());
          fieldAlign = ctx.getTypeAlign(ty.getElementType());
        } else {
          Pair<Long, Integer> fieldInfo = ctx.getTypeInfo(fd.getType());
          fieldSize = fieldInfo.first;
          fieldAlign = fieldInfo.second;
        }

        if (packed)
          fieldAlign = 8;

        // The maximum field alignment overrides the aligned attribute.
        if (maxFieldAlignment != 0)
          fieldAlign = (int) Math.min(fieldAlign, maxFieldAlignment);

        // Round up the current record size to the field's alignment boundary.
        fieldOffset = (fieldOffset + (fieldAlign - 1)) & ~(fieldAlign - 1);
      }

      // Put this field at the current position.
      fieldOffsets.add(fieldOffset);
      if (isUnion)
        size = Math.max(getSize(), fieldSize);
      else
        size = fieldOffset + fieldSize;

      // Remember the next available offset.
      nextOffset = size;

      // Update the data size.
      updateAlignment(fieldAlign);
    }

    private void updateAlignment(long newAlign) {
      if (newAlign <= alignment)
        return;

      Util.assertion((newAlign & (newAlign - 1)) == 0, "Alignment not a power of 2");
      alignment = newAlign;
    }

    private void finishLayout(RecordDecl d) {
      size = (size + (alignment - 1)) & ~(alignment - 1);
    }

    public long getSize() {
      return size;
    }

    public long getAlignment() {
      return alignment;
    }

    public long getMaxFieldAlignment() {
      return maxFieldAlignment;
    }

    public boolean isPacked() {
      return packed;
    }

    public boolean isUnion() {
      return isUnion;
    }

    public long getDataSize() {
      return nextOffset;
    }

    public long[] getFieldOffsets() {
      long[] res = new long[fieldOffsets.size()];
      for (int i = 0; i < res.length; i++)
        res[i] = fieldOffsets.get(i);
      return res;
    }
  }
}
