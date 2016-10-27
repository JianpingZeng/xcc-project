package sema;
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

import ast.Tree;
import utils.OutParamWrapper;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class LValueExprEvaluator extends ExprEvaluatorBase<Boolean>
{
    public LValueExprEvaluator(OutParamWrapper<LValue> result)
    {
    }

    @Override protected Boolean success(APValue v, Tree.Expr e)
    {
        return null;
    }

    @Override protected Boolean error(Tree.Expr expr)
    {
        return null;
    }
}
