package tools;
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

import java.util.HashMap;

/**
 * Make sure that only one instance of a particular type may be
 * created on any given run of the compiler...
 * note that this involves updating our map if an abstract type gets refined
 * somehow...
 * @author Xlous.zeng
 * @version 0.1
 */
public class TypeMap<K, V> extends HashMap<K, V>
{

}
