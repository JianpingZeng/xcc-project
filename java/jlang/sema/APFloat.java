package jlang.sema;
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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class APFloat
{
    public static final FltSemantics IEEEsingle = new FltSemantics((short) 127, (short)-126, 24, true );
    public static final FltSemantics IEEEdouble = new FltSemantics((short)1023, (short)-1022, 53, true);
    public static final FltSemantics IEEEquad = new FltSemantics((short)16383, (short)-16382, 113, true);
    public static final FltSemantics x87DoubleExtended = new FltSemantics((short)16383, (short)-16382, 64, true);
    public static final FltSemantics Bogus = new FltSemantics((short) 0, (short) 0, 0, true);
}
