/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
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

package cfe.driver;

/**
 * Represent an abstract compilation step to perform.
 * <p>
 * An action represents an edge in the compilation graph; typically
 * it is a job to transform an input using some tool.
 * <p>
 * The current driver is hard wired to expect actions which produce a
 * single primary output, at least in terms of controlling the
 * compilation. Actions can produce auxiliary files, but can only
 * produce a single output to feed into subsequent actions.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class Action {
  private ActionClass kind;
}
