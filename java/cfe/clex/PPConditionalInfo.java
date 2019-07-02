package cfe.clex;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2019, Jianping Zeng.
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

import cfe.support.SourceLocation;

/**
 * Information about the conditional stack (#if directives)
 * currently active.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class PPConditionalInfo {
  /**
   * Location where the conditional started.
   */
  SourceLocation ifLoc;

  /**
   * True if this was contained in a skipping directive, e.g.
   * in a "#if 0" block.
   */
  boolean wasSkipping;

  /**
   * True if we have emitted tokens already, and now we're in
   * an #else block or something.  Only useful in Skipping blocks.
   */
  boolean foundNonSkip;

  /**
   * True if we've seen a #else in this block.  If so,
   * #elif/#else directives are not allowed.
   */
  boolean foundElse;
}