package frontend.sema;
/*
 * Xlous C language Compiler
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

import frontend.sema.Decl.RecordDecl;

import java.util.HashMap;

/**
 * This class contains the layout information for a {@linkplain RecordDecl}
 * whic is a struct/union class. The decl represented must be definition.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class RecordLayoutInfo
{
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

    public RecordLayoutInfo(long size, long dataSize,
            long[] fieldOffsets, long alignment)
    {
        this.size = size;
        this.dataSize = dataSize;
        this.alignment = alignment;
        fieldCount =  fieldOffsets.length;
        if (fieldCount>0)
        {
            this.fieldOffsets = new long[fieldCount];
            System.arraycopy(fieldOffsets, 0, this.fieldOffsets, 0, fieldCount);
        }
    }

    public long getSize()
    {
        return size;
    }

    public long getDataSize()
    {
        return dataSize;
    }

    public long getFieldCount()
    {
        return fieldCount;
    }

    public long getAlignment()
    {
        return alignment;
    }

    public long getFiedOffsetAt(int fieldNo)
    {
        assert fieldNo>=0 && fieldNo< fieldCount:"Invalid field No!";
        return fieldOffsets[fieldNo];
    }

    /**
     * A cache mapping from already parsed RecordDecl to its corresponding
     * RecordLayoutInfo.
     */
    private static HashMap<RecordDecl, RecordLayoutInfo> recordLayouts
            = new HashMap<>();

    /**
     * Computes information for record layout, which suggests size and field
     * offset informations.
     * @param decl
     * @return
     */
    public static RecordLayoutInfo getRecordLayout(RecordDecl decl)
    {
        assert decl.isCompleteDefinition():"Cann't layout the forward declaration.";
        RecordLayoutInfo entity = recordLayouts.get(decl);
        if (entity != null)
            return entity;

        RecordLayoutBuilder builder = new RecordLayoutBuilder(1);
        builder.layout(decl);

        RecordLayoutInfo newEntry = new RecordLayoutInfo(builder.getSize(),
                builder.getSize(),
                builder.getFieldOffsets(),
                builder.getAlignment());

        // Caches the computed decl.
        recordLayouts.put(decl, newEntry);

        return newEntry;
    }
}
