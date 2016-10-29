package type;
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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class LabelType extends Type
{
    /**
     * Constructor with one parameter which represents the kind of type
     * for reason of comparison convenient.
     *
     */
    private LabelType()
    {
        super(TypeClass.Label);
    }

    private static LabelType instance;
    
    public static LabelType New()
    {
        if (instance == null)
            instance = new LabelType();
        return instance;
    }

    /**
     * Returns the size of the specified type in bits.
     * </br>
     * This method doesn't work on incomplete types.
     *
     * @return
     */
    @Override public long getTypeSize()
    {
        return 0;
    }

    @Override public boolean isSameType(Type other)
    {
        return false;
    }

    /**
     * Indicates if this type can be casted into target type.
     *
     * @param target
     * @return
     */
    @Override public boolean isCastableTo(Type target)
    {
        return false;
    }
}
