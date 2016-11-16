package backend.value;
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

import backend.hir.Operator;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class BinaryConstantExpr extends ConstantExpr
{
    /**
     * Constructs a new instruction representing the specified constants.
     *
     * @param opcode
     */
    public BinaryConstantExpr(Operator opcode, Constant c1, Constant c2)
    {
        super(c1.getType(), opcode);
        reserve(2);
        setOperand(0, c1);
        setOperand(1, c2);
    }
}
