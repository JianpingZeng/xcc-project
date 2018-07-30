package backend.codegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
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

import tools.Util;
import backend.target.TargetLowering.LegalizeAction;

import static backend.target.TargetLowering.LegalizeAction.*;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class ValueTypeAction
{
    /**
     * This is a bitvector that contains two bits for each
     * value type, where the two bits correspond to the LegalizeAction enum.
     * This can be queried with "getTypeAction(VT)".
     */
    private int[] valueTypeAction = new int[(MVT.MAX_ALLOWED_VALUETYPE / 32) * 2];

    public ValueTypeAction()
    {
        valueTypeAction[0] = valueTypeAction[1] = 0;
        valueTypeAction[2] = valueTypeAction[3] = 0;
    }

    public LegalizeAction getTypeAction(EVT vt)
    {
        if (vt.isExtended())
        {
            if (vt.isVector())
            {
                return vt.isPow2VectorType() ? Expand : Promote;
            }
            if (vt.isInteger())
            {
                // First promote to a power-of-two size, then expand if necessary.
                return vt.equals(vt.getRoundIntegerType()) ? Expand : Promote;
            }
            Util.assertion(false, "Unsupported extended type!");
            return Legal;
        }
        int i = vt.getSimpleVT().simpleVT;
        Util.assertion( i < 4 * valueTypeAction.length * 32);
        return LegalizeAction.values()[(valueTypeAction[i >> 4] >> ((2 * i) & 31) & 3)];
    }

    public void setTypeAction(EVT vt, LegalizeAction action)
    {
        int i = vt.getSimpleVT().simpleVT;
        Util.assertion( i < 4 * valueTypeAction.length * 32);
        valueTypeAction[i >> 4] |= action.ordinal() << ((i * 2) & 31);
    }

}
