package backend.target.x86;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import backend.value.GlobalValue;

/**
 * This struct holds a generalized full x86 address mode.
 * The base register can be a frame index, which will eventually be replaced
 * with BP or SP and Disp being offsetted accordingly.  The displacement may
 * also include the offset of a global value.
 * @author Xlous.zeng
 * @version 0.1
 */
public class X86AddressMode
{
    public enum BaseType
    {
        RegBase,
        FrameIndexBase
    }

    public abstract class Base
    {
        public abstract int getBase();

        public abstract void setBase(int base);
    }

    public class RegisterBase extends Base
    {
        private int reg;

        public RegisterBase(int r)
        {
            reg = r;
        }

        @Override
        public int getBase()
        {
            return reg;
        }

        @Override
        public void setBase(int base)
        {
            reg = base;
        }
    }

    public class FrameIndexBase extends Base
    {
        private int frameIndex;

        public FrameIndexBase(int fi)
        {
            frameIndex = fi;
        }

        @Override
        public int getBase()
        {
            return frameIndex;
        }

        @Override
        public void setBase(int base)
        {
            frameIndex = base;
        }
    }

    public BaseType baseType;
    public Base base;
    public int scale;
    public int indexReg;
    public int disp;
    public GlobalValue gv;
    public int gvOpFlags;

    public X86AddressMode()
    {
        baseType = BaseType.RegBase;
        scale = 1;
        indexReg = 0;
        disp = 0;
        gv = null;
        gvOpFlags = 0;
        base = new RegisterBase(0);
    }
}
