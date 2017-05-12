package jlang.clex;
/*
 * Extremely C language Compiler.
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

import jlang.basic.SourceRange;
import jlang.sema.Sema;

/**
 * Abstract base class that describes a handler that will receive
 * source ranges for each of the comments encountered in the source file.
 * @author Xlous.zeng
 * @version 0.1
 */
public interface CommentHandler
{
    void handleComoment(Preprocessor pp, SourceRange comment);

    class DefaultCommentHandler implements CommentHandler
    {
        private Sema actions;

        public DefaultCommentHandler(Sema actions)
        {
            this.actions = actions;
        }
        @Override
        public void handleComoment(Preprocessor pp, SourceRange comment)
        {
            actions.actOnComment(comment);
        }
    }
}

