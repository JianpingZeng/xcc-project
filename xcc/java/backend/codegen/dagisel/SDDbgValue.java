package backend.codegen.dagisel;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2018, Jianping Zeng.
 * All rights reserved.
 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import backend.debug.DebugLoc;
import backend.value.MDNode;
import backend.value.Value;
import tools.Util;

/**
 * Holding information from dbg_value node through DAGISel.
 * @author Jianping Zeng.
 * @version 0.4
 */
public class SDDbgValue {
  public enum DbgValueKind {
    SDNODE,
    CONST,
    FRAMEIX
  }

  private static class S {
    SDNode node;
    int resNo;
    S(SDNode n, int no) { node = n; resNo = no; }
  }

  private DbgValueKind kind;
  /**
   * Val can be class S or Value or frameIx (type int).
   */
  private Object val;
  private MDNode mdPtr;
  private long offset;
  private DebugLoc dl;
  private int order;
  private boolean isInvalid;

  /**
   * Constructor for non-constants.
   * @param mdP
   * @param n
   * @param r
   * @param off
   * @param dl
   * @param o
   */
  public SDDbgValue(MDNode mdP, SDNode n, int r, long off,
                    DebugLoc dl, int o) {
    mdPtr = mdP;
    offset = off;
    this.dl = dl;
    order = o;
    isInvalid = false;
    kind = DbgValueKind.SDNODE;
    val = new S(n, r);
  }

  /**
   * Constructor for constant.
   * @param mdP
   * @param c
   * @param off
   * @param dl
   * @param o
   */
  public SDDbgValue(MDNode mdP, Value c, long off,
                    DebugLoc dl, int o) {
    mdPtr = mdP;
    offset = off;
    this.dl = dl;
    order = o;
    isInvalid = false;
    kind = DbgValueKind.CONST;
    val = c;
  }

  /**
   * Constructor for frame indices.
   * @param mdP
   * @param fi
   * @param off
   * @param dl
   * @param o
   */
  public SDDbgValue(MDNode mdP, int fi, long off,
                    DebugLoc dl, int o) {
    mdPtr = mdP;
    offset = off;
    this.dl = dl;
    order = o;
    isInvalid = false;
    kind = DbgValueKind.FRAMEIX;
    val = fi;
  }

  public DbgValueKind getKind() { return kind; }

  public MDNode getMDPtr() { return mdPtr; }

  public SDNode getSDNode() {
    Util.assertion(kind == DbgValueKind.SDNODE);
    return ((S)val).node;
  }

  public int getResNo() {
    Util.assertion(kind == DbgValueKind.SDNODE);
    return ((S)val).resNo;
  }

  public Value getConst() {
    Util.assertion(kind == DbgValueKind.CONST);
    return (Value) val;
  }

  public int getFrameIx() {
    Util.assertion(kind == DbgValueKind.FRAMEIX);
    return (int) val;
  }

  public long getOffset() { return offset; }

  public DebugLoc getDebugLoc() { return dl; }

  public int getOrder() { return order; }

  public void setInvalidated() {
    isInvalid = true;
  }

  public boolean isInvalidated() {
    return isInvalid;
  }
}
