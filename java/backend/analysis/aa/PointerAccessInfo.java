/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package backend.analysis.aa;

import backend.value.Value;

/**
 * This struct is used to return results for pointers,
 * globals, and the return value of a function.
 */
public class PointerAccessInfo
{
    /**
     * This may be an Argument for the function, a GlobalVariable, or null,
     * corresponding to the return value for the function.
     */
    Value val;

    /**
     * Whether the pointer is loaded or stored to/from.
     */
    ModRefResult modRefInfo;

    /**
     * Specific fine-grained access information for the argument.
     * If none of these classifications is general enough, the
     * getModRefBehavior method should not return AccessesArguments.
     * If a record is not returned for a particular argument, the argument
     * is never dead and never dereferenced.
     */
    enum AccessType
    {
        /**
         * The pointer is dereferenced.
         */
        ScalarAccess,

        /**
         * The pointer is indexed through as an array of elements.
         */
        ArrayAccess,

        /**
         * Indirect calls are made through the specified function pointer.
         */
        CallsThrough
    }
}
