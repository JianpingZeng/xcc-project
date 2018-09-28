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

package backend.llReader;

import backend.support.LLVMContext;
import backend.type.IntegerType;
import backend.value.Operator;
import jlang.support.MemoryBuffer;
import tools.APFloat;
import tools.*;
import tools.APSInt;

import java.util.TreeMap;

import static backend.llReader.LLTokenKind.*;
import static backend.llReader.LLTokenKind.Error;
import static backend.value.Operator.*;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public final class LLLexer {
  private MemoryBuffer buffer;
  private int curPtr;
  private OutRef<SMDiagnostic> errorInfo;
  private SourceMgr smg;

  private String error;

  /**
   * The kind of LLVM token.
   */
  private LLTokenKind tokKind;
  /**
   * The start position of token in MemoryBuffer.
   */
  private int tokStart;
  private String strVal;
  private int intVal;
  private backend.type.Type tyVal;
  private tools.APFloat floatVal;
  private tools.APSInt apsIntVal;
  /**
   * For instruction opecode.
   */
  private Operator opc;
  /**
   * A table resides all of LLVM IR keyword.
   */
  private TreeMap<String, LLTokenKind> keywords;
  private TreeMap<String, backend.type.Type> typeKeywords;
  private TreeMap<String, Pair<Operator, LLTokenKind>> opcKeywords;

  public LLLexer(MemoryBuffer buf, SourceMgr smg, OutRef<SMDiagnostic> diag) {
    this.buffer = buf;
    curPtr = buf.getBufferStart();
    errorInfo = diag;
    this.smg = smg;
    error = "";
    tokKind = Eof;
    strVal = "";
    intVal = 0;
    tyVal = null;
    keywords = new TreeMap<>();
    typeKeywords = new TreeMap<>();
    opcKeywords = new TreeMap<>();
    initKeywords();
    initTypeKeywords();
    initOpcKeywords();
  }

  private void initKeywords() {
    keywords.put("begin", kw_begin);
    keywords.put("end", kw_end);
    keywords.put("true", kw_true);
    keywords.put("false", kw_false);
    keywords.put("declare", kw_declare);
    keywords.put("define", kw_define);
    keywords.put("global", kw_global);
    keywords.put("constant", kw_constant);

    keywords.put("private", kw_private);
    keywords.put("linker_private", kw_linker_private);
    keywords.put("internal", kw_internal);
    keywords.put("available_externally", kw_available_externally);
    keywords.put("linkonce", kw_linkonce);
    keywords.put("linkonce_odr", kw_linkonce_odr);
    keywords.put("weak", kw_weak);
    keywords.put("weak_odr", kw_weak_odr);
    keywords.put("appending", kw_appending);
    keywords.put("dllimport", kw_dllimport);
    keywords.put("dllexport", kw_dllexport);
    keywords.put("common", kw_common);
    keywords.put("default", kw_default);
    keywords.put("hidden", kw_hidden);
    keywords.put("protected", kw_protected);
    keywords.put("extern_weak", kw_extern_weak);
    keywords.put("external", kw_external);
    keywords.put("thread_local", kw_thread_local);
    keywords.put("zeroinitializer", kw_zeroinitializer);
    keywords.put("undef", kw_undef);
    keywords.put("null", kw_null);
    keywords.put("to", kw_to);
    keywords.put("tail", kw_tail);
    keywords.put("target", kw_target);
    keywords.put("triple", kw_triple);
    keywords.put("deplibs", kw_deplibs);
    keywords.put("datalayout", kw_datalayout);
    keywords.put("volatile", kw_volatile);
    keywords.put("nuw", kw_nuw);
    keywords.put("nsw", kw_nsw);
    keywords.put("exact", kw_exact);
    keywords.put("inbounds", kw_inbounds);
    keywords.put("align", kw_align);
    keywords.put("addrspace", kw_addrspace);
    keywords.put("section", kw_section);
    keywords.put("alias", kw_alias);
    keywords.put("module", kw_module);
    keywords.put("asm", kw_asm);
    keywords.put("sideeffect", kw_sideeffect);
    keywords.put("gc", kw_gc);
    keywords.put("select", kw_select);

    keywords.put("ccc", kw_ccc);
    keywords.put("fastcc", kw_fastcc);
    keywords.put("coldcc", kw_coldcc);
    keywords.put("x86_stdcallcc", kw_x86_stdcallcc);
    keywords.put("x86_fastcallcc", kw_x86_fastcallcc);
    keywords.put("arm_apcscc", kw_arm_apcscc);
    keywords.put("arm_aapcscc", kw_arm_aapcscc);
    keywords.put("arm_aapcs_vfpcc", kw_arm_aapcs_vfpcc);

    keywords.put("cc", kw_cc);
    keywords.put("c", kw_c);

    keywords.put("signext", kw_signext);
    keywords.put("zeroext", kw_zeroext);
    keywords.put("inreg", kw_inreg);
    keywords.put("sret", kw_sret);
    keywords.put("nounwind", kw_nounwind);
    keywords.put("noreturn", kw_noreturn);
    keywords.put("noalias", kw_noalias);
    keywords.put("nocapture", kw_nocapture);
    keywords.put("byval", kw_byval);
    keywords.put("nest", kw_nest);
    keywords.put("readnone", kw_readnone);
    keywords.put("readonly", kw_readonly);

    keywords.put("noinline", kw_noinline);
    keywords.put("alwaysinline", kw_alwaysinline);
    keywords.put("optsize", kw_optsize);
    keywords.put("ssp", kw_ssp);
    keywords.put("sspreq", kw_sspreq);
    keywords.put("noredzone", kw_noredzone);
    keywords.put("noimplicitfloat", kw_noimplicitfloat);
    keywords.put("naked", kw_naked);

    keywords.put("type", kw_type);
    keywords.put("opaque", kw_opaque);

    keywords.put("eq", kw_eq);
    keywords.put("ne", kw_ne);
    keywords.put("slt", kw_slt);
    keywords.put("sgt", kw_sgt);
    keywords.put("sle", kw_sle);
    keywords.put("sge", kw_sge);
    keywords.put("ult", kw_ult);
    keywords.put("ugt", kw_ugt);
    keywords.put("ule", kw_ule);
    keywords.put("uge", kw_uge);
    keywords.put("oeq", kw_oeq);
    keywords.put("one", kw_one);
    keywords.put("olt", kw_olt);
    keywords.put("ogt", kw_ogt);
    keywords.put("ole", kw_ole);
    keywords.put("oge", kw_oge);
    keywords.put("ord", kw_ord);
    keywords.put("uno", kw_uno);
    keywords.put("ueq", kw_ueq);
    keywords.put("une", kw_une);
    keywords.put("x", kw_x);
  }

  private void initTypeKeywords() {
    typeKeywords.put("void", LLVMContext.VoidTy);
    typeKeywords.put("float", LLVMContext.FloatTy);
    typeKeywords.put("double", LLVMContext.DoubleTy);
    typeKeywords.put("x86_fp80", LLVMContext.X86_FP80Ty);
    typeKeywords.put("fp128", LLVMContext.FP128Ty);
    typeKeywords.put("label", LLVMContext.LabelTy);
    typeKeywords.put("metadata", null);
  }

  private void initOpcKeywords() {
    opcKeywords.put("add", Pair.get(Add, kw_add));
    opcKeywords.put("fadd", Pair.get(FAdd, kw_fadd));
    opcKeywords.put("sub", Pair.get(Sub, kw_sub));
    opcKeywords.put("fsub", Pair.get(FSub, kw_fsub));
    opcKeywords.put("mul", Pair.get(Mul, kw_mul));
    opcKeywords.put("fmul", Pair.get(FMul, kw_fmul));
    opcKeywords.put("udiv", Pair.get(UDiv, kw_udiv));
    opcKeywords.put("sdiv", Pair.get(SDiv, kw_sdiv));
    opcKeywords.put("fdiv", Pair.get(FDiv, kw_fdiv));
    opcKeywords.put("urem", Pair.get(URem, kw_urem));
    opcKeywords.put("srem", Pair.get(SRem, kw_srem));
    opcKeywords.put("frem", Pair.get(FRem, kw_frem));
    opcKeywords.put("shl", Pair.get(Shl, kw_shl));
    opcKeywords.put("lshr", Pair.get(LShr, kw_lshr));
    opcKeywords.put("ashr", Pair.get(AShr, kw_ashr));
    opcKeywords.put("and", Pair.get(And, kw_and));
    opcKeywords.put("or", Pair.get(Or, kw_or));
    opcKeywords.put("xor", Pair.get(Xor, kw_xor));
    opcKeywords.put("icmp", Pair.get(ICmp, kw_icmp));
    opcKeywords.put("fcmp", Pair.get(FCmp, kw_fcmp));

    opcKeywords.put("phi", Pair.get(Phi, kw_phi));
    opcKeywords.put("call", Pair.get(Call, kw_call));
    opcKeywords.put("trunc", Pair.get(Trunc, kw_trunc));
    opcKeywords.put("zext", Pair.get(ZExt, kw_zext));
    opcKeywords.put("sext", Pair.get(SExt, kw_sext));
    opcKeywords.put("fptrunc", Pair.get(FPTrunc, kw_fptrunc));
    opcKeywords.put("fpext", Pair.get(FPExt, kw_fpext));
    opcKeywords.put("uitofp", Pair.get(UIToFP, kw_uitofp));
    opcKeywords.put("sitofp", Pair.get(SIToFP, kw_sitofp));
    opcKeywords.put("fptoui", Pair.get(FPToUI, kw_fptoui));
    opcKeywords.put("fptosi", Pair.get(FPToSI, kw_fptosi));
    opcKeywords.put("inttoptr", Pair.get(IntToPtr, kw_inttoptr));
    opcKeywords.put("ptrtoint", Pair.get(PtrToInt, kw_ptrtoint));
    opcKeywords.put("bitcast", Pair.get(BitCast, kw_bitcast));
    opcKeywords.put("select", null/*Pair.get(Select, kw_select)*/);
    opcKeywords.put("va_arg", null/*Pair.get(VAArg, kw_va_arg)*/);
    opcKeywords.put("ret", Pair.get(Ret, kw_ret));
    opcKeywords.put("br", Pair.get(Br, kw_br));
    opcKeywords.put("switch", Pair.get(Switch, kw_switch));
    opcKeywords.put("invoke", null/*Pair.get(Invoke, kw_invoke)*/);
    opcKeywords.put("unwind", null/*Pair.get(Unwind, kw_unwind)*/);
    opcKeywords.put("unreachable", Pair.get(Unreachable, kw_unreachable));

    opcKeywords.put("malloc", Pair.get(Malloc, kw_malloc));
    opcKeywords.put("alloca", Pair.get(Alloca, kw_alloca));
    opcKeywords.put("free", Pair.get(Free, kw_free));
    opcKeywords.put("load", Pair.get(Load, kw_load));
    opcKeywords.put("store", Pair.get(Store, kw_store));
    opcKeywords.put("getelementptr",
        Pair.get(GetElementPtr, kw_getelementptr));

    opcKeywords.put("extractelement", null/*Pair.get(ExtractElement, kw_extractelement)*/);
    opcKeywords.put("insertelement", null/*Pair.get(InsertElement, kw_insertelement)*/);
    opcKeywords.put("shufflevector", null/*Pair.get(ShuffleVector, kw_shufflevector)*/);
    opcKeywords.put("getresult", null/*Pair.get(ExtractValue, kw_getresult)*/);
    opcKeywords.put("extractvalue", null/*Pair.get(ExtractValue, kw_extractvalue)*/);
    opcKeywords.put("insertvalue", null/*Pair.get(InsertValue, kw_insertvalue)*/);
  }

  public LLTokenKind lex() {
    tokKind = lexTok();
    return tokKind;
  }

  public SourceMgr.SMLoc getLoc() {
    return SourceMgr.SMLoc.get(buffer, tokStart);
  }

  public LLTokenKind getTokKind() {
    return tokKind;
  }

  public String getStrVal() {
    return strVal;
  }

  public backend.type.Type getTyVal() {
    return tyVal;
  }

  public int getIntVal() {
    return intVal;
  }

  public tools.APSInt getAPsIntVal() {
    return apsIntVal;
  }

  public tools.APFloat getFloatVal() {
    return floatVal;
  }

  public boolean error(SourceMgr.SMLoc loc, String msg) {
    errorInfo.set(smg.getMessage(loc, msg, SourceMgr.DiagKind.DK_Error));
    return true;
  }

  public boolean error(String msg) {
    return error(getLoc(), msg);
  }

  public String getFilename() {
    return buffer.getBufferName();
  }

  private int getNextChar() {
    char ch = buffer.getCharAt(curPtr++);
    switch (ch) {
      default:
        return ch;
      case '\0':
        if (curPtr - 1 != buffer.length())
          return '\0';
        --curPtr;
        return -1;
    }
  }

  private LLTokenKind lexTok() {
    tokStart = curPtr;
    int ch = getNextChar();
    switch (ch) {
      default:
        // Handle letters: [a-zA-Z_]
        if (Character.isLetter(ch) || ch == '_')
          return lexIdentifier();

        return Error;
      case -1:
        // EOF.
        return Eof;
      case '\0':
      case ' ':
      case '\t':
      case '\n':
      case '\r':
        // skip whitespace character.
        return lexTok();
      case '+':
        return lexPositive();
      case '@':
        return lexAt();
      case '%':
        return lexPercent();
      case '"':
        return lexQuote();
      case '.': {
        int ptr = isLabelTail(buffer, curPtr);
        if (ptr >= 0) {
          curPtr = ptr;
          strVal = buffer.getSubString(tokStart, curPtr - 1);
          return LabelStr;
        }
        if (buffer.getCharAt(curPtr) == '.' && buffer.getCharAt(curPtr + 1) == '.') {
          curPtr += 2;
          return dotdotdot;
        }
        return Error;
      }
      case '$': {
        int ptr = isLabelTail(buffer, curPtr);
        if (!(ptr < 0)) {
          curPtr = ptr;
          strVal = buffer.getSubString(tokStart, curPtr - 1);
          return LabelStr;
        }
        return Error;
      }
      case ';':
        // Comment.
        skipLineComment();
        return lexTok();
      case '!':
        return lexMetadata();
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
      case '-':
        return lexDigitOrNegative();
      case '=':
        return equal;
      case '[':
        return lsquare;
      case ']':
        return rsquare;
      case '{':
        return lbrace;
      case '}':
        return rbrace;
      case '<':
        return less;
      case '>':
        return greater;
      case ')':
        return rparen;
      case '(':
        return lparen;
      case ',':
        return comma;
      case '*':
        return star;
      case '\\':
        return backslash;
    }
  }

  /**
   * [-+]?[0-9]+[.][0-9]*([eE][-+]?[0-9]+)?
   *
   * @return
   */
  private LLTokenKind lexPositive() {
    if (!Character.isDigit(buffer.getCharAt(curPtr)))
      return Error;

    // Recognizes digit sequence
    while (Character.isDigit(buffer.getCharAt(curPtr)))
      ++curPtr;

    if (buffer.getCharAt(curPtr) != '.') {
      curPtr = tokStart + 1;
      return Error;
    }

    if (Character.isDigit(buffer.getCharAt(curPtr))) {
      do {
        ++curPtr;
      } while (Character.isDigit(buffer.getCharAt(curPtr)));
    }
    char exp = buffer.getCharAt(curPtr);
    if (exp == 'E' || exp == 'e') {
      ++curPtr;
      char ch = buffer.getCharAt(curPtr);
      if (ch == '+' || ch == '-')
        ++curPtr;   // skip the negative or positive sign.
      else if (Character.isDigit(ch)) {
        do {
          ++curPtr;
        } while (Character.isDigit(buffer.getCharAt(curPtr)));
      } else {
        return Error;
      }
    }
    floatVal = new APFloat(Double.valueOf(buffer.getSubString(tokStart, curPtr)));
    return tokKind = APFloat;
  }

  /**
   * Lex all tokens that start with an @ character:
   * GlobalVar   @\"[^\"]*\"
   * GlobalVar   @[-a-zA-Z$._][-a-zA-Z$._0-9]*
   * GlobalVarID @[0-9]+
   *
   * @return
   */
  private LLTokenKind lexAt() {
    if (buffer.getCharAt(curPtr) == '"') {
      // GlobalVar   @\"[^\"]*\"
      ++curPtr;
      while (true) {
        int ch = getNextChar();
        if (ch == -1) {
          error("End of file in global variable name");
          return Error;
        }
        if (ch == '"') {
          strVal = buffer.getSubString(tokStart + 2, curPtr - 1);
          strVal = unEscapeLexed(strVal);
          return GlobalVar;
        }
      }
    }

    // Handle GlobalVarName: @[-a-zA-Z$._][-a-zA-Z$._0-9]*
    char ch = buffer.getCharAt(curPtr);
    if (isLLIdentifierPart(ch)) {
      do {
        ch = buffer.getCharAt(++curPtr);
      } while (isLLIdentifierPart(ch) || Character.isDigit(ch));

      strVal = buffer.getSubString(tokStart + 1, curPtr);
      return GlobalVar;
    }

    // Handle GlobalVarID @[0-9]+
    if (Character.isDigit(ch)) {
      do {
        ch = buffer.getCharAt(++curPtr);
      } while (Character.isDigit(ch));

      long val = Long.parseLong(buffer.getSubString(tokStart + 1, curPtr));
      if ((int) val != val) {
        error("invalid value number(too long)");
      }
      intVal = (int) val;
      return GlobalID;
    } else {
      curPtr = tokStart + 1;
      return Error;
    }
  }

  private static boolean isHexDigit(char ch) {
    return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch < 'f') || (ch >= 'A' && ch <= 'F');
  }

  private static String unEscapeLexed(String str) {
    if (str == null || str.isEmpty())
      return str;

    StringBuilder buf = new StringBuilder();
    for (int i = 0, e = str.length(); i != e; ) {
      if (str.charAt(i) == '\\') {
        if (i < e - 1 && str.charAt(i + 1) == '\\') {
          buf.append('\\');
          i += 2;
        } else if (i < e - 2 && isHexDigit(str.charAt(i + 1)) && isHexDigit(str.charAt(i + 2))) {
          buf.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
          i += 3;
        } else {
          buf.append(str.charAt(i++));
        }
      } else {
        buf.append(str.charAt(i++));
      }
    }
    return buf.toString();
  }

  private static boolean isLLIdentifierPart(int ch) {
    return ch == '-' || Character.isLetter((char) ch) ||
        ch == '$' || ch == '.' || ch == '_';
  }

  /**
   * Return true for [-a-zA-Z$._0-9].
   *
   * @param ch
   * @return
   */
  private static boolean isLabelChar(int ch) {
    return isLLIdentifierPart(ch) || Character.isDigit((char) ch);
  }

  /**
   * Lex all tokens that start with a % character:
   * LocalVar   ::= %\"[^\"]*\"
   * LocalVar   ::= %[-a-zA-Z$._][-a-zA-Z$._0-9]*
   * LocalVarID ::= %[0-9]+
   *
   * @return
   */
  private LLTokenKind lexPercent() {
    char ch = buffer.getCharAt(curPtr);
    if (ch == '"') {
      ++curPtr;
      while (true) {
        int nextCh = getNextChar();
        if (nextCh == -1) {
          error("End of file in local variable name");
          return Error;
        }
        if (nextCh != '"')
          continue;

        strVal = buffer.getSubString(tokStart + 1, curPtr - 1);
        return LocalVar;
      }
    } else if (isLLIdentifierPart(ch)) {
      ++curPtr;
      while (true) {
        int nextCh = getNextChar();
        if (nextCh == -1) {
          error("End of file in local variable name");
          return Error;
        }
        if (!isLabelChar(nextCh)) {
          --curPtr;
          break;
        }
      }
      strVal = buffer.getSubString(tokStart + 1, curPtr);
      return LocalVar;
    } else if (Character.isDigit(ch)) {
      ++curPtr;
      while (true) {
        int nextCh = getNextChar();
        if (nextCh == -1) {
          error("End of file in local variable ID");
          return Error;
        }
        if (!Character.isDigit((char) nextCh)) {
          --curPtr;
          break;
        }
      }
      long val = Long.parseLong(buffer.getSubString(tokStart + 1, curPtr));
      if ((int) val != val) {
        error("Invalid local variable id(too long)");
        return Error;
      }
      intVal = (int) val;
      return LocalVarID;
    } else {
      error("Invalid local variable name after '%'");
      return Error;
    }
  }

  /**
   * Lex all tokens that start with a " character:
   * QuoteLabel        "[^"]+":
   * StringConstant    "[^"]*"
   *
   * @return
   */
  private LLTokenKind lexQuote() {
    int ch = getNextChar();
    if (ch == -1) {
      error("End of file in quote");
      return Error;
    }
    if (ch == '"') {
      // String constant.
      strVal = "";
      return StringConstant;
    } else {
      while (true) {
        ch = getNextChar();
        if (ch == -1) {
          error("End of file in quote");
          return Error;
        }
        if (ch != '"')
          continue;
        if (buffer.getCharAt(curPtr) != ':') {
          // label.
          strVal = buffer.getSubString(tokStart + 1, curPtr - 1);
          strVal = unEscapeLexed(strVal);
          return StringConstant;
        }
        ++curPtr;
        // label.
        strVal = buffer.getSubString(tokStart + 1, curPtr - 2);
        strVal = unEscapeLexed(strVal);
        return LabelStr;
      }
    }
  }

  /**
   * Handle several related productions:
   * Label             [-a-zA-Z$._0-9]+:
   * NInteger          -[0-9]+
   * FPConstant        [-+]?[0-9]+[.][0-9]*([eE][-+]?[0-9]+)?
   * PInteger          [0-9]+
   * HexFPConstant     0x[0-9A-Fa-f]+
   * HexFP80Constant   0xK[0-9A-Fa-f]+
   * HexFP128Constant  0xL[0-9A-Fa-f]+
   * HexPPC128Constant 0xM[0-9A-Fa-f]+
   *
   * @return
   */
  private LLTokenKind lexDigitOrNegative() {
    char ch = buffer.getCharAt(curPtr);
    if (isLLIdentifierPart(buffer.getCharAt(tokStart)) && isLLIdentifierPart(ch)) {
      // okay, this is not a number after the '-'. It probably a label.
      int end = isLabelTail(buffer, curPtr);
      if (end < 0)
        return Error;

      strVal = buffer.getSubString(tokStart, end - 1);
      curPtr = end;
      return LabelStr;
    }

    // Skip sequnce of digits.
    while (Character.isDigit(buffer.getCharAt(curPtr)))
      ++curPtr;

    if (isLabelChar(buffer.getCharAt(curPtr)) || buffer.getCharAt(curPtr) == ':') {
      int end = isLabelTail(buffer, curPtr);
      if (end >= 0) {
        strVal = buffer.getSubString(tokStart, curPtr - 1);
        curPtr = end;
        return LabelStr;
      }
    }

    if (buffer.getCharAt(curPtr) != '.') {
      if (buffer.getCharAt(tokStart) == '0' && buffer.getCharAt(tokStart + 1) == 'x') {
        ++curPtr;   // skip the 'x' after '0'.
        return lex0x();
      }
      int len = curPtr - tokStart;
      int numBits = len * 64 / 19 + 2;
      APInt tmp = new APInt(numBits, buffer.getSubString(tokStart, tokStart + len), 10);
      if (buffer.getCharAt(tokStart) == '-') {
        int minBits = tmp.getMinSignedBits();
        if (minBits > 0 && minBits < numBits)
          tmp.trunc(minBits);
        apsIntVal = new APSInt(tmp, false);
      } else {
        int activeBits = tmp.getActiveBits();
        if (activeBits > 0 && activeBits < numBits)
          tmp.trunc(activeBits);
        apsIntVal = new APSInt(tmp, true);
      }
      return APSInt;
    }
    ++curPtr;

    // Skip over [0-9]*([eE][-+]?[0-9]+)?
    while (Character.isDigit(buffer.getCharAt(curPtr))) ++curPtr;

    if (buffer.getCharAt(curPtr) == 'e' || buffer.getCharAt(curPtr) == 'E') {
      ch = buffer.getCharAt(curPtr + 1);
      if (((ch == '-' || ch == '+') && curPtr < buffer.length() - 2 &&
          Character.isDigit(buffer.getCharAt(curPtr + 2)))) {
        curPtr += 2;
        while (curPtr < buffer.length() &&
            Character.isDigit(buffer.getCharAt(curPtr)))
          ++curPtr;
      }
    }
    floatVal = new APFloat(Double.valueOf(buffer.getSubString(tokStart, curPtr)));
    return APFloat;
  }

  private long hexToInt(String str) {
    long result = 0;
    for (int i = 0, e = str.length(); i < e; i++) {
      long oldResult = result;
      char ch = str.charAt(i);
      result *= 16;
      if (ch >= '0' && ch <= '9')
        result += ch - '0';
      else if (ch >= 'a' && ch <= 'f')
        result += ch - 'a' + 10;
      else if (ch >= 'A' && ch <= 'F')
        result += ch - 'A' + 10;
      else {
        error("illegal hex number!");
        return 0;
      }
      if (oldResult > result)
        error("constant bigger than 64 bits detected!");
    }
    return result;
  }

  /**
   * Handle productions that start with 0x, knowing that it matches and
   * that this is not a label:
   * HexFPConstant     0x[0-9A-Fa-f]+
   * HexFP80Constant   0xK[0-9A-Fa-f]+
   * HexFP128Constant  0xL[0-9A-Fa-f]+
   * HexPPC128Constant 0xM[0-9A-Fa-f]+
   *
   * @return
   */
  private LLTokenKind lex0x() {
    int ch = getNextChar();
    if (ch == -1) {
      // EOF
      error("End of file in hex decimal");
      return Error;
    }
    char kind;
    if (ch >= 'K' && ch <= 'M') {
      kind = (char) ch;
      ++curPtr;
    } else if (isHexDigit((char) ch)) {
      kind = 'J';
    } else {
      tokStart = curPtr;
      error("Illegal character in hex decimal number");
      return Error;
    }
    // HexFP80Constant   0xK[0-9A-Fa-f]+
    if (kind >= 'K' && kind <= 'M' && !isHexDigit((char) getNextChar())) {
      tokStart = curPtr + 1;
      error("Must have at least one hex digit after 'K,L,M'");
      return Error;
    }
    while (true) {
      ch = getNextChar();
      if (ch == -1) {
        // EOF
        error("End of file in hex decimal");
        return Error;
      }
      if (!isHexDigit((char) ch)) {
        // done!
        --curPtr;   // backward one character.
        break;
      }
    }

    // F80HexFPConstant - x87 long double in hexadecimal format (10 bytes)
    long[] pair = new long[2];
    switch (kind) {
      case 'J':
        long val = hexToInt(buffer.getSubString(tokStart + 2, curPtr));
        floatVal = new APFloat(Util.bitsToDouble(val));
        break;
      case 'K':
        fp80HexFPToIntPair(buffer.getSubString(tokStart + 3, curPtr), pair);
        floatVal = new APFloat(new APInt(pair, 80));
        break;
      case 'L':
        hexToIntPair(buffer.getSubString(tokStart + 3, curPtr), pair);
        floatVal = new APFloat(new APInt(pair, 128));
        break;
      case 'M':
        hexToIntPair(buffer.getSubString(tokStart + 3, curPtr), pair);
        floatVal = new APFloat(new APInt(pair, 128));
        break;
      default:
        Util.assertion(false, "Illegal character after '0x'");
        break;
    }
    return APFloat;
  }

  /**
   * translate an 80 bit FP80 number (20 hexits) into
   * { low64, high16 } as usual for an APInt.
   *
   * @param str
   * @param pair
   */
  private void fp80HexFPToIntPair(String str, long[] pair) {
    // 16[high] + 64[low].
    pair[1] = 0;
    int index = 0, len = str.length();
    for (int i = 0; i < 4 && index < len; i++, index++) {
      Util.assertion(index < len);
      char ch = str.charAt(index);
      pair[1] *= 16;  // shift-add to accumulate the hex floating point number.
      if (ch >= '0' && ch <= '9')
        pair[1] += ch - '0';
      else if (ch >= 'A' && ch <= 'F')
        pair[1] += ch - 'A' + 10;
      else if (ch >= 'a' && ch <= 'f')
        pair[1] += ch - 'a' + 10;
    }
    pair[0] = 0;
    for (int i = 0; i < 16 && index < len; i++, index++) {
      char ch = str.charAt(index);
      pair[0] *= 16;  // shift-add to accumulate the hex floating point number.
      if (ch >= '0' && ch <= '9')
        pair[0] += ch - '0';
      else if (ch >= 'A' && ch <= 'F')
        pair[0] += ch - 'A' + 10;
      else if (ch >= 'a' && ch <= 'f')
        pair[0] += ch - 'a' + 10;
    }
    if (index != len)
      error("consant bigger than 80 bits");
  }

  /**
   * Translate 128 bits hex floating number into a pair of integer as follows.
   * [low64, high64]
   *
   * @param str
   * @param pair
   */
  private void hexToIntPair(String str, long[] pair) {
    // 64 + 64.
    pair[1] = 0;
    int index = 0, len = str.length();
    for (int i = 0; i < 16 && index < len; i++, index++) {
      Util.assertion(index < len);
      char ch = str.charAt(index);
      pair[1] *= 16;  // shift-add to accumulate the hex floating point number.
      if (ch >= '0' && ch <= '9')
        pair[1] += ch - '0';
      else if (ch >= 'A' && ch <= 'F')
        pair[1] += ch - 'A' + 10;
      else if (ch >= 'a' && ch <= 'f')
        pair[1] += ch - 'a' + 10;
    }
    pair[0] = 0;
    for (int i = 0; i < 16 && index < len; i++, index++) {
      char ch = str.charAt(index);
      pair[0] *= 16;  // shift-add to accumulate the hex floating point number.
      if (ch >= '0' && ch <= '9')
        pair[0] += ch - '0';
      else if (ch >= 'A' && ch <= 'F')
        pair[0] += ch - 'A' + 10;
      else if (ch >= 'a' && ch <= 'f')
        pair[0] += ch - 'a' + 10;
    }
    if (index != len)
      error("consant bigger than 128 bits");
  }

  /**
   * Handle several related productions:
   * <pre>
   *    Label           [-a-zA-Z$._0-9]+:
   *    IntegerType     i[0-9]+
   *    Keyword         sdiv, float, ...
   *    HexIntConstant  [us]0x[0-9A-Fa-f]+
   * </pre>
   *
   * @return
   */
  private LLTokenKind lexIdentifier() {
    if (buffer.getCharAt(tokStart) == 'i' && Character.isDigit(buffer.getCharAt(curPtr))) {
      //IntegerType     i[0-9]+
      while (true) {
        int ch = getNextChar();
        if (ch == -1) {
          tokStart = curPtr;
          error("End of file in integer type");
          return Error;
        }
        if (!(ch >= '0' && ch <= '9')) {
          --curPtr;
          break;
        }
      }
      int numBits = Integer.parseInt(buffer.getSubString(tokStart + 1, curPtr));
      if (numBits < IntegerType.MIN_INT_BITS || numBits > IntegerType.MAX_INT_BITS) {
        error("bitwidth for integer type out of ranges");
        return Error;
      }
      tyVal = IntegerType.get(numBits);
      return Type;
    }
    // Check for [us]0x[0-9A-Fa-f]+ which are Hexadecimal constant generated by
    // the CFE to avoid forcing it to deal with 64-bit numbers.
    char fisrtCh = buffer.getCharAt(tokStart);
    if ((fisrtCh == 'u' || fisrtCh == 's') &&
        buffer.getCharAt(curPtr) == '0' &&
        buffer.getCharAt(curPtr + 1) == 'x' &&
        isHexDigit(buffer.getCharAt(curPtr + 2))) {

      return APSInt;
    }

    // Takes both label and keyword into consideration.
    // If '$', '.' and '-' present, it must be label.
    boolean nonKeyword = false;
    while (true) {
      int ch = getNextChar();
      if (ch == -1) {
        error("End of file in label or keyword");
        return Error;
      }
      if (ch == '$' || ch == '.' || ch == '-')
        nonKeyword = true;
      if (!isLabelChar(ch)) {
        // discard the invalid char for label.
        --curPtr;
        break;
      }
    }
    strVal = buffer.getSubString(tokStart, curPtr);
    if (nonKeyword || buffer.getCharAt(curPtr) == ':') {
      // If the ending character is ':', it is exactly a Label.
      if (buffer.getCharAt(curPtr) == ':') ++curPtr;
      return LabelStr;
    } else {
      if (keywords.containsKey(strVal))
        return keywords.get(strVal);
      if (typeKeywords.containsKey(strVal)) {
        tyVal = typeKeywords.get(strVal);
        if (tyVal == null) {
          error("Unsupported type keyword, '" + strVal + "'");
          return Error;
        }
        strVal = null;
        return Type;
      }
      if (opcKeywords.containsKey(strVal)) {
        Pair<Operator, LLTokenKind> pair = opcKeywords.get(strVal);
        if (tyVal == null) {
          error("Unsupported Instruction opcode, '" + strVal + "'");
          return Error;
        }
        strVal = null;
        opc = pair.first;
        return pair.second;
      }
    }

    return LabelStr;
  }

  private void skipLineComment() {
    while (true) {
      int ch = getNextChar();
      if (ch == '\n' || ch == '\r' || ch == -1)
        return;
    }
  }

  /**
   * LexMetadata:
   * !{...}
   * !42
   * !foo
   *
   * @return
   */
  private LLTokenKind lexMetadata() {
    int ch = getNextChar();
    if (ch == -1) {
      error("End of file in metadata");
      return Error;
    }
    if (Character.isLetter((char) ch)) {
      do {
        ch = getNextChar();
      } while (isLabelChar(ch));
      // skip the !
      strVal = buffer.getSubString(tokStart + 1, curPtr - 1);
      return NamedMD;
    } else {
      return Metadata;
    }
  }

  private static int isLabelTail(MemoryBuffer buffer, int curPtr) {
    while (true) {
      if (buffer.getCharAt(curPtr) == ':')
        return curPtr + 1;
      if (!isLabelChar(buffer.getCharAt(curPtr)))
        return -1;
      ++curPtr;
    }
  }
}
