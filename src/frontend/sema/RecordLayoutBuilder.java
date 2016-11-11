package frontend.sema;
/*
 * Xlous C language CompilerInstance
 * Copyright (c) 2015-2016, Xlous
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

import frontend.sema.Decl.FieldDecl;
import frontend.sema.Decl.RecordDecl;
import frontend.type.ArrayType;
import frontend.type.QualType;
import tools.Pair;
import tools.Util;

import java.util.ArrayList;

/**
 * This class represents building an instance of {@linkplain RecordLayoutInfo}
 * , initializing each member of class {@linkplain RecordLayoutInfo}, for example
 * , size, dataSize and alignment etc.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class RecordLayoutBuilder
{
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
    private long unpackedAlignment;

    /**
     * The array bolds field offset in bits.
     */
    private ArrayList<Long> fieldOffsets;

    /**
     * The flags indicates if this record is packed or not.
     */
    private boolean packed;

    private boolean isUnion;

    /**
     * The data size of the record being laid out with tail padding.
     */
    private long dataSize;

    public RecordLayoutBuilder(int align)
    {
        size = 0;
        alignment = align;
        unpackedAlignment = align;
        fieldOffsets = new ArrayList<>(16);
        packed = false;
        isUnion = false;
        dataSize = 0;
    }

    /**
     * Sets the layout information for RecordDecl.
     * @return
     */
    public void layout(RecordDecl d)
    {
        initializeLayout(d);
        layoutFields(d);

        // Finally, round the size of the total struct up to the
        // alignment of the struct itself.
        finishLayout(d);
    }

    private void initializeLayout(RecordDecl d)
    {
        isUnion = d.isUnion();
    }

    /**
     * Layouts each field, for now, just sequentially respecting alignment.
     * In the future, thsi will need to be tweakable by targets.
     * @param d
     */
    private void layoutFields(RecordDecl d)
    {
        for (int i = 0; i < d.getDeclCounts(); i++)
        {
            layoutField(d.getDeclAt(i));
        }
    }

    private void layoutField(FieldDecl fd)
    {
        if (fd.isBitField())
        {
            layoutBitField(fd);
            return;
        }

        long unpaddedFieldOffset = getDataSizeInBits();

        long fieldOffset = isUnion?0:getDataSize();
        int fieldSize;
        int fieldAlign;

        if (fd.getDeclType().isIncompleteArrayType())
        {
            // This is a flexible array member; we can't directly
            // query getTypeInfo about these, so we figure it out here.
            // Flexible array members don't have any size, but they
            // have to be aligned appropriately for their element frontend.type.'
            fieldSize = 0;
            ArrayType ty = fd.getDeclType().getAsArrayType();
            fieldAlign = QualType.getTypeAlignInBytes(ty.getElemType());
        }
        else
        {
            Pair<Integer, Integer> fieldInfo = QualType.getTypeInfoInBytes(fd.getDeclType());
            fieldSize = fieldInfo.first;
            fieldAlign = fieldInfo.second;
        }

        if (packed)
            fieldAlign = 1;
        // Round up the current record size to the field's alignment boundary.
        fieldOffset = Util.roundUp(fieldOffset, fieldAlign);

        // TODO place field in empty subobject to make use of space.
        // Put this field at the current position.
        fieldOffsets.add(QualType.toBits(fieldOffset));

        // TODO Check field padding

        long fieldSizeInBits = QualType.toBits(fieldSize);
        if (isUnion)
            size = Math.max(getSize(), fieldSizeInBits);
        else
            size = fieldOffset + fieldSize;

        // Update the data size
        dataSize = getSizeInBits();
        alignment = fieldAlign;
    }

    private void layoutBitField(FieldDecl d)
    {

    }

    private void finishLayout(RecordDecl d)
    {
        size = Util.roundUp(getSizeInBits(), QualType.toBits(alignment));
    }

    public long getSize()
    {
        assert size %8 == 0;
        return size>>3;
    }

    public long getSizeInBits()
    {
        return size;
    }

    public long getAlignment()
    {
        return alignment;
    }

    public long getUnpackedAlignment()
    {
        return unpackedAlignment;
    }

    public boolean isPacked()
    {
        return packed;
    }

    public boolean isUnion()
    {
        return isUnion;
    }

    public long getDataSize()
    {
        assert dataSize % 8 == 0;
        return dataSize >> 3;
    }

    public long getDataSizeInBits()
    {
        return dataSize;
    }

    public long[] getFieldOffsets()
    {
        long[] res = new long[fieldOffsets.size()];
        System.arraycopy(fieldOffsets.toArray(), 0, res, 0, res.length);
        return res;
    }
}
