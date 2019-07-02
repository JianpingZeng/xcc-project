package backend.transform.ipo;

/*
 * Extremely C language Compiler
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

public final class InlineCost {
  public enum InlineKind {
    /**
     * Indicates the callee never be inlined.
     */
    Never,
    /**
     * Indicates the compiler determines whether the callee
     * should be inlined or not inspect to the inlining cost.
     */
    Value,
    /**
     * Tells the compiler always inline those functions with
     * always_inline attribute.
     */
    Always
  }

  private InlineKind kind;
  private int cost;

  private InlineCost(int cost) {
    this(cost, InlineKind.Value);
  }

  private InlineCost(int cost, InlineKind kind) {
    this.cost = cost;
    this.kind = kind;
  }

  public int getCost() {
    return cost;
  }

  public InlineKind getKind() {
    return kind;
  }

  public static InlineCost getNever() {
    return new InlineCost(0, InlineKind.Never);
  }

  public static InlineCost getAlways() {
    return new InlineCost(0, InlineKind.Always);
  }

  public static InlineCost get(int cost) {
    return new InlineCost(cost);
  }

  public boolean isNeverInline() {
    return kind == InlineKind.Never;
  }

  public boolean isAlwaysInline() {
    return kind == InlineKind.Always;
  }
}
