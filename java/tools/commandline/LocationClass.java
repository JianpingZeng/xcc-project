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

package tools.commandline;

/**
 * Allow user to specify which external variable they want to store
 * the results of the command line argument processing into, if they
 * didn't want to store it in the option ifself.
 * @author Jianping Zeng
 * @version 0.1
 */
public abstract class LocationClass<T>
{
    public abstract void setLocation(T location);
}
