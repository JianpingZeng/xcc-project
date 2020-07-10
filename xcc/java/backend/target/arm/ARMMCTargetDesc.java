package backend.target.arm;
/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
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

import backend.support.Triple;

/**
 * @author Jianping Zeng.
 * @version 0.4
 */
public class ARMMCTargetDesc {
  public static String parseARMTriple(String tt) {
    int len = tt.length();
    int idx = 0;
    boolean isThumb = false;
    if (len >= 5 && tt.substring(0, 4).equals("armv"))
      idx = 4;
    else if (len >= 6 && tt.substring(0, 5).equals("thumb")) {
      isThumb = true;
      if (len >= 7 && tt.charAt(5) == 'v')
        idx = 6;
    }

    String armArchFeature = null;
    if (idx != 0) {
      int subversion = tt.charAt(idx) - '0';
      if (subversion >= 7 && subversion <= 9) {
        if (len >= idx + 2 && tt.charAt(idx + 1) == 'm') {
          // v7m: FeatureNoARM, FeatureDB, FeatureHWDiv, FeatureMClass
          armArchFeature = "+v7,+noarm,+db,+hwdiv,+mclass";
        }
        else if (len >= idx + 3 && tt.charAt(idx + 1) == 'e' && tt.charAt(idx+2) == 'm') {
          // v7em: FeatureNoARM, FeatureDB, FeatureHWDiv, FeatureDSPThumb2,
          //       FeatureT2XtPk, FeatureMClass
          armArchFeature = "+v7,+noarm,+db,+hwdiv,+t2dsp,t2xtpk,+mclass";
        }
        else
          // v7a: FeatureNEON, FeatureDB, FeatureDSPThumb2, FeatureT2XtPk
          armArchFeature = "+v7,+neon,+db,+t2dsp,+t2xtpk";
      }
      else if (subversion == 6) {
        if (len >= idx + 3 && tt.charAt(idx+1) == 't' && tt.charAt(idx+2) == '2')
          armArchFeature = "+v6t2";
        else if (len >= idx + 2 && tt.charAt(idx + 1) == 'm')
          // v6m: FeatureNoARM, FeatureMClass
          armArchFeature = "+v6t2,+noarm,+mclass";
      else
          armArchFeature = "+v6";
      }
      else if (subversion == 5) {
        if (len >= idx + 3 && tt.charAt(idx+1) == 't' && tt.charAt(idx+2) == 'e')
          armArchFeature = "+v5t3";
        else
          armArchFeature = "+v5t";
      }
      else if(subversion == 4 && len >= idx + 2 && tt.charAt(idx + 1) == 't')
        armArchFeature = "+v4t";
    }
    if (isThumb) {
      if (armArchFeature == null)
        armArchFeature = "+thumb-mode";
      else
        armArchFeature += ",+thumb-mode";
    }
    Triple triple = new Triple(tt);
    if (triple.getOS() == Triple.OSType.NativeClient) {
      if (armArchFeature == null)
        armArchFeature = "+nacl-mode";
      else
        armArchFeature += ",+nacl-mode";
    }
    return armArchFeature;
  }
}
