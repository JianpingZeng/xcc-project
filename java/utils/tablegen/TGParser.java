/* Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng
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
package utils.tablegen;

import gnu.trove.list.array.TIntArrayList;
import cfe.support.MemoryBuffer;
import tools.Error;
import tools.Pair;
import tools.SourceMgr;
import tools.Util;
import utils.tablegen.Init.BinOpInit;
import utils.tablegen.Init.BinOpInit.BinaryOp;
import utils.tablegen.Init.BitsInit;
import utils.tablegen.Init.TypedInit;
import utils.tablegen.Init.UnOpInit.UnaryOp;
import utils.tablegen.Init.VarInit;
import utils.tablegen.RecTy.BitsRecTy;
import utils.tablegen.RecTy.StringRecTy;

import java.io.Reader;
import java.util.*;

import static utils.tablegen.Record.records;

public final class TGParser {
  public static final class LetRecord {
    String name;
    TIntArrayList bits;
    Init value;
    SourceMgr.SMLoc loc;

    LetRecord(String name, TIntArrayList bits, Init val, SourceMgr.SMLoc loc) {
      this.name = name;
      value = val;
      this.bits = bits;
      this.loc = loc;
    }
  }

  public static final class SubClassReference {
    SourceMgr.SMLoc loc;
    Record rec;
    ArrayList<Init> templateArgs = new ArrayList<>();

    public boolean isInvalid() {
      return rec == null;
    }
  }

  /**
   * This class is used for saving the status of current parsing file, contains
   * filename, r (the Reader object to reading filename),
   * inputStream(currently at the last character consumed for include str),
   * fileLineNo (the current line no when encountering include str.)
   */
  public static final class IncludeRec {
    String filename;
    Reader r;
    SimpleCharStream inputStream;
    int startLine, startColumn;

    IncludeRec(String filename, Reader r) {
      this.filename = filename;
      this.r = r;
    }
  }

  private TGLexer lexer;
  private final Stack<ArrayList<LetRecord>> letStack = new Stack<ArrayList<LetRecord>>();
  private HashMap<String, MultiClass> multiClasses = new HashMap<>();
  private MultiClass curMultiClass;

  private Record curRec = null;

  static ArrayList<String> includeDirectories = new ArrayList<>();
  static final Stack<IncludeRec> includeStack = new Stack<IncludeRec>();

  public TGParser(SourceMgr sgr) {
    lexer = new TGLexer(sgr);
  }

  private boolean error(SourceMgr.SMLoc loc, String msg) {
    Error.printError(loc, msg);
    return true;
  }

  /**
   * This method is called when an error needed to be reported.
   */
  private boolean tokError(String msg) {
    Error.printError(lexer.getLoc(), msg);
    return true;
  }

  /**
   * This is the entry point for parsing a table-gen grammar file. This method
   * should return true on error, or false on sucess.
   */
  public boolean parse() {
    lexer.lex();
    if (parseObjectList()) return true;

    // The last token expected to be EOF.
    if (lexer.getCode() == TGLexer.TokKind.Eof)
      return false;
    return tokError("Unexpected input at top level");
  }

  private boolean addValue(Record curRec, SourceMgr.SMLoc loc, RecordVal rv) {
    if (curRec == null)
      curRec = curMultiClass.rec;

    RecordVal rval = curRec.getValue(rv.getName());
    if (rval != null) {
      if (rval.setValue(rv.getValue())) {
        return error(loc, "New definition of '" + rv.getName() + "' of type '" + rv
            .getType().toString()
            + "' is incompatible with previous definition of type '"
            + rval.getType().toString() + "'!\n");
      }
    } else
      curRec.addValue(rv);
    return false;
  }

  private void addSuperClass(Record rc) {
    if (curRec.isSubClassOf(rc)) {
      tokError("Already subclass of '" + rc.getName() + "'!\n");
      System.exit(-1);
    }
    curRec.addSuperClass(rc);
  }

  private boolean setValue(
      Record curRec,
      SourceMgr.SMLoc loc,
      String valName,
      TIntArrayList bitlist,
      Init val) {
    if (val == null)
      return false;

    if (curRec == null) curRec = curMultiClass.rec;

    RecordVal rv = curRec.getValue(valName);
    if (rv == null) {
      return error(loc, "Value '" + valName + "' Unknown!\n");
    }

    // Do not allow assignments like 'X = X'.  This will just cause infinite loops
    // in the resolution machinery.
    if (bitlist == null || bitlist.isEmpty()) {
      if (val instanceof VarInit) {
        VarInit vi = (VarInit) val;
        if (vi.getName().equals(valName))
          return false;
      }
    }

    // If we are assigning to a subset of the bits in the value... then we must be
    // assigning to a field of BitsRecTy, which must have a BitsInit initializer.
    if (bitlist != null && !bitlist.isEmpty()) {
      if (!(rv.getValue() instanceof BitsInit)) {
        tokError("Value '" + valName + "' is not a bits type!\u005cn");
      }

      BitsInit curVal = (BitsInit) rv.getValue();

      // Convert the incoming value to a bits type of the appropriate getNumOfSubLoop...
      Init bi = val.convertInitializerTo(new BitsRecTy(bitlist.size()));
      if (bi == null) {
        val.convertInitializerTo(new BitsRecTy(bitlist.size()));
        return error(loc, "Initializer '" + val.toString()
            + "' not compatible with bit range!\n");
      }

      // We should have a BitsInit type now...
      Util.assertion(bi instanceof BitsInit);
      BitsInit binit = (BitsInit) bi;

      BitsInit newVal = new BitsInit(curVal.getNumBits());
      for (int i = 0, e = bitlist.size(); i < e; i++) {
        int b = bitlist.get(i);
        if (newVal.getBit(b) != null) {
          return error(loc, "Cannot set bit #" + b + " of value '" + valName
              + "' more than once!\n");
        }
        newVal.setBit(b, binit.getBit(i));
      }

      for (int i = 0, e = curVal.getNumBits(); i < e; i++) {
        if (newVal.getBit(i) == null) {
          newVal.setBit(i, curVal.getBit(i));
        }
      }
      val = newVal;
    }

    if (rv.setValue(val)) {
      return error(loc, "Value '" + valName + "' of type '" + rv.getType()
          .toString() + "' is incompatible with initializer '" + val
          .toString() + "'!\n");
    }
    return false;
  }

  /**
   * Add {@code rec} as a subclass to curRec, resolving templateArgs as
   * rec's template arguments.
   */
  private boolean addSubClass(Record curRec, SubClassReference subClass) {
    Record sc = subClass.rec;
    ArrayList<RecordVal> vals = sc.getValues();
    for (RecordVal rv : vals)
      if (addValue(curRec, subClass.loc, rv))
        return true;

    ArrayList<String> targs = sc.getTemplateArgs();
    if (targs != null && subClass.templateArgs != null
        && targs.size() < subClass.templateArgs.size()) {
      return error(subClass.loc,
          "UNKNOWN: More template args specified than expected!\n");
    } else {
      for (int i = 0, e = targs.size(); i < e; i++) {
        String arg = targs.get(i);
        if (i < subClass.templateArgs.size()) {
          // set the value for template argument.
          if (setValue(curRec, subClass.loc, arg, null, subClass.templateArgs.get(i)))
            return true;

          // resolve any reference to this template arg as the targ's value.
          curRec.resolveReferencesTo(curRec.getValue(arg));

          curRec.removeValue(arg);
        } else if (!curRec.getValue(arg).getValue().isComplete()) {
          return error(subClass.loc, "UNKNOWN: Value not specified for template argument #"
              + i + " (" + arg + ") of suclass '" + sc
              .getName() + "'!\n");
        }
      }
    }

    // Since everything went well, we can now set the "superclass" list for the
    // current record.
    ArrayList<Record> supers = sc.getSuperClasses();
    for (Record scs : supers) {
      if (curRec.isSubClassOf(scs))
        return error(subClass.loc, "Already subclass of '" +
            scs.getName() + "'!\n");

      curRec.addSuperClass(scs);
    }

    if (curRec.isSubClassOf(sc))
      return error(subClass.loc, "Already subclass of '" + sc.getName() + "'!\n");
    curRec.addSuperClass(sc);
    return false;
  }

  /**
   * This is the entry point to parsing a single file.
   *
   * @filename The input file namespace.
   * @debug The flag to indicating whether enable debug.
   */
  static boolean parseFile(String filename, List<String> includeDirs,
                           SourceMgr srcMgr) {
    MemoryBuffer f = MemoryBuffer.getFileOrSTDIN(filename);
    if (f == null) {
      System.err.printf("Could not open input file '%s'\n", filename);
      return true;
    }

    srcMgr.addNewSourceBuffer(f, new SourceMgr.SMLoc());
    srcMgr.setIncludeDirs(includeDirs);
    TGParser parser = new TGParser(srcMgr);
    return parser.parse();
  }

  private boolean parseObjectList() {
    return parseObjectList(null);
  }

  /**
   * ObjectList:=     Object*
   *
   * @return
   */
  private boolean parseObjectList(MultiClass curMultiClass) {
    while (isObjectStart(lexer.getCode())) {
      if (parseObject(curMultiClass))
        return true;
    }
    return false;
  }

  /**
   * Checks if the given token kind is legal for starting an Object declaration.
   *
   * @param tok
   * @return
   */
  private boolean isObjectStart(TGLexer.TokKind tok) {
    switch (tok) {
      case Class:
      case Def:
      case Defm:
      case Let:
      case XForEach:
      case Multiclass:
        return true;
      default:
        return false;
    }
  }

  /**
   * Object:=     ClassInst
   * DefInst
   * MultiClassInst
   * DefMInst
   * LETCOMMAND '{' ObjectList '}'
   * LETCOMMAND Object
   *
   * @return
   */
  private boolean parseObject(MultiClass mc) {
    switch (lexer.getCode()) {
      default:
        Util.assertion("This is not an object");
        return false;
      case Let:
        return parseTopLevelLet(mc);
      case Def:
        return parseDef(mc);
      case Defm:
        return parseDefm(mc);
      case Class:
        return parseClass();
      case Multiclass:
        return parseMultiClass();
    }
  }

  /**
   * Parses a 'let' at the top level. This can be a couple of different related
   * production.
   * <pre>
   * Object::= LET LetList IN '{' ObjectList '}'
   *       ::= LET LetList IN Object
   * </pre>
   *
   * @return
   */
  private boolean parseTopLevelLet(MultiClass curMultiClass) {
    Util.assertion(lexer.getCode() == TGLexer.TokKind.Let, "Unexpected token");
    lexer.lex();

    ArrayList<LetRecord> letInfo = parseLetList();
    if (letInfo.isEmpty()) return true;

    letStack.push(letInfo);

    if (lexer.getCode() != TGLexer.TokKind.In)
      return tokError("Expected 'in' at the end of top level 'let'");

    lexer.lex();

    if (lexer.getCode() != TGLexer.TokKind.l_brace) {
      if (parseObject(curMultiClass))
        return true;
    } else {
      SourceMgr.SMLoc braceLoc = lexer.getLoc();
      lexer.lex();

      if (parseObjectList(curMultiClass))
        return true;

      if (lexer.getCode() != TGLexer.TokKind.r_brace) {
        tokError("expected '}' at end of top level let command");
        return error(braceLoc, "to match this '{");
      }

      lexer.lex();
    }

    // Remember to clear the let stack.
    letStack.pop();
    return false;
  }

  /**
   * Parse a non-empty list of assignment expressions into a list of {@linkplain LetRecord}.
   * <pre>
   * LetList ::= LetItem (',' LetItem)*
   * LetItem ::= ID OptionalRangeList '=' Value
   * </pre>
   *
   * @return
   */
  private ArrayList<LetRecord> parseLetList() {
    ArrayList<LetRecord> result = new ArrayList<>();

    while (true) {
      if (lexer.getCode() != TGLexer.TokKind.Id) {
        tokError("expected identifier in let definition");
        return new ArrayList<>();
      }

      String name = lexer.getCurStrVal();
      SourceMgr.SMLoc nameLoc = lexer.getLoc();
      lexer.lex();

      // Check for optional range list.
      TIntArrayList bits = new TIntArrayList();
      if (parseOptionalRangeList(bits))
        return new ArrayList<>();

      bits.reverse();

      if (lexer.getCode() != TGLexer.TokKind.equal) {
        tokError("expected '=' in let expression");
        return new ArrayList<>();
      }
      lexer.lex();    // eat the '='.

      Init val = parseValue(null);
      if (val == null) return new ArrayList<>();

      result.add(new LetRecord(name, bits, val, nameLoc));

      if (lexer.getCode() != TGLexer.TokKind.comma)
        return result;

      lexer.lex();    // Eat the ','
    }
  }

  /**
   * Parse either a range list in <>'s or nothing.
   * <pre>
   * OptionalRangeList ::=     '<' RangeList '>'
   *                   ::=
   * </pre>
   *
   * @param bits
   * @return
   */
  private boolean parseOptionalRangeList(TIntArrayList bits) {
    if (lexer.getCode() != TGLexer.TokKind.less)
      return false;

    SourceMgr.SMLoc startLoc = lexer.getLoc();
    lexer.lex();    // Eat the '<'.

    parseRangeList(bits);
    if (bits.isEmpty()) return true;

    if (lexer.getCode() != TGLexer.TokKind.greater) {
      tokError("expected '>' at end of range list");
      return error(startLoc, "to match this '<'");
    }

    // Eat the '>'.
    lexer.lex();

    return false;
  }

  /**
   * RangeList ::= RangePiece (',' RangePiece)*
   *
   * @param ranges
   */
  private void parseRangeList(TIntArrayList ranges) {
    if (parseRangePiece(ranges)) {
      ranges.clear();
      return;
    }
    while (lexer.getCode() == TGLexer.TokKind.comma) {
      lexer.lex();

      if (parseRangePiece(ranges)) {
        ranges.clear();
        return;
      }
    }
  }

  /**
   * Parse a bit/value range.
   * <pre>
   *   RangePiece ::= INTVAL
   *   RangePiece ::= INTVAL '-' INTVAL
   *   RangePiece ::= INTVAL INTVAL
   * </pre>
   *
   * @param ranges
   * @return
   */
  private boolean parseRangePiece(TIntArrayList ranges) {
    if (lexer.getCode() != TGLexer.TokKind.IntVal) {
      tokError("expected integer or bitrange");
      return true;
    }

    long start = lexer.getCurIntVal();
    long end;

    if (start < 0)
      return tokError("invalid range, cannot be negative");

    switch (lexer.lex()) {
      default:
        ranges.add((int) start);
        return false;
      case minus:
        if (lexer.lex() != TGLexer.TokKind.IntVal) {
          tokError("expected integer value as end of range");
          return true;
        }
        end = lexer.getCurIntVal();
        break;
      case IntVal:
        end = -lexer.getCurIntVal();
        break;
    }
    if (end < 0) {
      return tokError("invalid range, cannot be negative");
    }

    lexer.lex();

    if (start < end) {
      for (; start < end; start++)
        ranges.add((int) start);
    } else {
      for (; start >= end; --start)
        ranges.add((int) start);
    }

    return false;
  }

  /**
   * Parse a tblgen value.  This returns null on error.
   * <pre>
   *   Value       ::= SimpleValue ValueSuffix*
   *   ValueSuffix ::= '{' BitList '}'
   *   ValueSuffix ::= '[' BitList ']'
   *   ValueSuffix ::= '.' ID
   * </pre>
   *
   * @param curRec
   * @return
   */
  private Init parseValue(Record curRec) {
    return parseValue(curRec, null);
  }

  private Init parseValue(Record curRec, RecTy itemType) {
    Init result = parseSimpleValue(curRec, itemType);
    if (result == null) return null;

    while (true) {
      switch (lexer.getCode()) {
        default:
          return result;
        case l_brace: {
          SourceMgr.SMLoc braceLoc = lexer.getLoc();
          lexer.lex();

          TIntArrayList bits = new TIntArrayList();
          parseRangeList(bits);
          if (bits.isEmpty()) return null;

          bits.reverse();

          result = result.convertInitializerBitRange(bits);
          if (result == null) {
            error(braceLoc, "Invalid bit range for value");
            return null;
          }

          // Eat the '}'.
          if (lexer.getCode() != TGLexer.TokKind.r_brace) {
            tokError("expected '}' at end of bit range list");
            return null;
          }

          lexer.lex();
          break;
        }
        case l_square: {
          SourceMgr.SMLoc squareLoc = lexer.getLoc();
          lexer.lex();

          TIntArrayList ranges = new TIntArrayList();
          parseRangeList(ranges);
          if (ranges.isEmpty()) return null;

          result = result.convertIntListSlice(ranges);
          if (result == null) {
            error(squareLoc, "Invalid range for list slice");
            return null;
          }

          if (lexer.getCode() != TGLexer.TokKind.r_square) {
            tokError("expected '] at end of list slice");
            return null;
          }

          lexer.lex();
          break;
        }
        case dot: {
          if (lexer.lex() != TGLexer.TokKind.Id) {
            tokError("exected field iddentifier after '.'");
            return null;
          }

          if (result.getFieldType(lexer.getCurStrVal()) == null) {
            tokError("Cannot access field '" + lexer.getCurStrVal()
                + "' of value '" + result.toString() + "'");
            return null;
          }
          result = new Init.FieldInit(result, lexer.getCurStrVal());
          lexer.lex();    // Eat the field namespace.
          break;
        }
      }
    }
  }

  private static int anonCounter = 0;

  /**
   * Parse a tblgen value.  This returns null on error.
   * <pre>
   *   SimpleValue ::= IDValue
   *   SimpleValue ::= INTVAL
   *   SimpleValue ::= STRVAL+
   *   SimpleValue ::= CODEFRAGMENT
   *   SimpleValue ::= '?'
   *   SimpleValue ::= '{' ValueList '}'
   *   SimpleValue ::= ID '<' ValueListNE '>'
   *   SimpleValue ::= '[' ValueList ']'
   *   SimpleValue ::= '(' IDValue DagArgList ')'
   *   SimpleValue ::= CONCATTOK '(' Value ',' Value ')'
   *   SimpleValue ::= SHLTOK '(' Value ',' Value ')'
   *   SimpleValue ::= SRATOK '(' Value ',' Value ')'
   *   SimpleValue ::= SRLTOK '(' Value ',' Value ')'
   *   SimpleValue ::= STRCONCATTOK '(' Value ',' Value ')'
   * </pre>
   *
   * @param curRec
   * @param itemType
   * @return
   */
  private Init parseSimpleValue(Record curRec, RecTy itemType) {
    Init res = null;
    switch (lexer.getCode()) {
      default:
        tokError("Unknown token when parsing a value");
        break;
      /*case BinaryIntVal: {
        Pair<Long, Integer> binaryVal = lexer.getCurBinaryIntVal();
        Init[] bits = new Init[binaryVal.second];
        for (int i = 0; i < binaryVal.second; i++)
          bits[i] = new Init.BitInit((binaryVal.first & (1L << i)) != 0);
        res = BitsInit.get(bits);
        lexer.lex();
        break;
      }*/
      case IntVal:
        res = new Init.IntInit(lexer.getCurIntVal());
        lexer.lex();
        break;
      case StrVal: {
        StringBuilder val = new StringBuilder(lexer.getCurStrVal());
        lexer.lex();

        while (lexer.getCode() == TGLexer.TokKind.StrVal) {
          val.append(lexer.getCurStrVal());
          lexer.lex();
        }

        res = new Init.StringInit(val.toString());
        break;
      }
      case CodeFragment: {
        res = new Init.CodeInit(lexer.getCurStrVal());
        lexer.lex();
        break;
      }
      case question: {
        res = Init.UnsetInit.getInstance();
        lexer.lex();
        break;
      }
      case Id: {
        SourceMgr.SMLoc nameLoc = lexer.getLoc();
        String name = lexer.getCurStrVal();
        if (lexer.lex() != TGLexer.TokKind.less) {
          // Value := IDValue
          return parseIDValue(curRec, name, nameLoc);
        }

        // Eat the '<', advance to the next token.
        lexer.lex();

        // This is a CLASS<initvalslist> expression.  This is supposed to synthesize
        // a new anonymous definition, deriving from CLASS<initvalslist> with no
        // body.
        Record klass = records.getClass(name);
        if (klass == null) {
          error(nameLoc, "Expected a class namespace, got '" + name + "'");
          return null;
        }

        ArrayList<Init> valueList = parseValueList(curRec, klass, null);
        if (valueList.isEmpty()) return null;

        if (lexer.getCode() != TGLexer.TokKind.greater) {
          tokError("expected '>' at end of value list");
          return null;
        }

        lexer.lex();

        Record newRec = new Record("anonymous.val" + (anonCounter++), nameLoc);
        SubClassReference scRef = new SubClassReference();
        scRef.rec = klass;
        scRef.loc = nameLoc;
        scRef.templateArgs = valueList;
        if (addSubClass(newRec, scRef))
          return null;

        newRec.resolveReferences();
        records.addDef(newRec);

        return new Init.DefInit(newRec);
      }
      case l_brace: {
        SourceMgr.SMLoc loc = lexer.getLoc();
        lexer.lex();

        ArrayList<Init> vals = new ArrayList<>();

        if (lexer.getCode() != TGLexer.TokKind.r_brace) {
          vals = parseValueList(curRec, null, null);
          if (vals.isEmpty()) return null;
        }
        if (lexer.getCode() != TGLexer.TokKind.r_brace) {
          tokError("expected '}' at end of bit list value");
          return null;
        }

        lexer.lex();

        ArrayList<Init> newBits = new ArrayList<>();
        // As we parse {a, b, ...}, 'a' is the first one to be parsed.
        // We'll first read it's element from leftmost to rightmost,
        // then we can reverse it to get the bits in the correct order
        // for the BitsInit value.
        for (int i = 0, e = vals.size(); i < e; i++) {
          Init init = vals.get(i);
          if (init instanceof BitsInit) {
            BitsInit bi = (BitsInit) init;
            for (int j = 0, sz = bi.getNumBits(); j < sz; j++)
              newBits.add(bi.getBit(sz - j - 1));
            continue;
          }
          // bits<n> can also come from variable initializers.
          if (init instanceof VarInit) {
            VarInit vi = (VarInit) init;
            if (vi.getType() instanceof BitsRecTy) {
              BitsRecTy bitsTy = (BitsRecTy) vi.getType();
              for (int j = 0, sz = bitsTy.getNumBits(); j < sz; j++)
                newBits.add(vi.getBit(sz - j - 1));
              continue;
            }
          }
          Init bit = vals.get(i).convertInitializerTo(new RecTy.BitRecTy());
          if (bit == null) {
            error(loc, "Element #" + i + " (" + vals.get(i).toString() +
                ") is not convertible to a bit");
            return null;
          }
          newBits.add(bit);
        }
        Collections.reverse(newBits);
        return BitsInit.get(newBits);
      }
      case l_square: {
        lexer.lex();
        ArrayList<Init> vals = new ArrayList<>();

        RecTy deducedEltTy = null;
        RecTy.ListRecTy givenListTy = null;

        if (itemType != null) {
          RecTy.ListRecTy listType;
          if (!(itemType instanceof RecTy.ListRecTy)) {
            tokError("Type mismatch for list, expected list type, got "
                + itemType.toString());
            listType = null;
          } else
            listType = (RecTy.ListRecTy) itemType;

          givenListTy = listType;
        }

        if (lexer.getCode() != TGLexer.TokKind.r_square) {
          vals = parseValueList(curRec, null, givenListTy != null ? givenListTy.getElementType() : null);
          if (vals.isEmpty()) return null;
        }

        if (lexer.getCode() != TGLexer.TokKind.r_square) {
          tokError("expected ']' at end of list value");
          return null;
        }

        lexer.lex();

        RecTy givenEltTy = null;
        if (lexer.getCode() == TGLexer.TokKind.less) {
          lexer.lex();

          givenEltTy = parseType();
          if (givenEltTy == null)
            return null;

          if (lexer.getCode() != TGLexer.TokKind.greater) {
            tokError("expected '>' at end of list element type");
            return null;
          }
          lexer.lex();
        }


        RecTy eltTy = null;
        /**
         * For make compatibel wit LLVM 1.3 tblgen
         */
        for (int i = 0, e = vals.size(); i < e; i++) {
          if (!(vals.get(i) instanceof TypedInit)) {
            tokError("Untyped list element");
            return null;
          }

          TypedInit targ = (TypedInit) vals.get(i);
          if (eltTy != null) {
            eltTy = resolveTypes(eltTy, targ.getType());
            if (eltTy == null) {
              tokError("Incompatible types in list elements");
              return null;
            }
          } else {
            eltTy = targ.getType();
          }
        }

        if (givenEltTy != null) {
          if (eltTy != null) {
            if (!eltTy.typeIsConvertiableTo(givenEltTy)) {
              tokError("Incompatible types in list elements");
              return null;
            }
          }
          eltTy = givenEltTy;
        }

        if (eltTy == null) {
          if (itemType == null) {
            tokError("No type for list");
            return null;
          }
          deducedEltTy = givenListTy.getElementType();
        } else {
          if (givenListTy != null) {
            if (!eltTy.typeIsConvertiableTo(givenListTy.getElementType())) {
              tokError("Element type mismatch for list");
              return null;
            }
          }
          deducedEltTy = eltTy;
        }

        return new Init.ListInit(vals, deducedEltTy);
      }
      case l_paren: {
        lexer.lex();

        TGLexer.TokKind tk = lexer.getCode();
        if (tk != TGLexer.TokKind.Id
            && tk != TGLexer.TokKind.XCast
            && tk != TGLexer.TokKind.XNameConcat) {
          tokError("expected identifier in dag init");
          return null;
        }

        Init operator = null;
        if (lexer.getCode() == TGLexer.TokKind.Id) {
          operator = parseIDValue(curRec);
          if (operator == null)
            return null;
        } else {
          operator = parseOperation(curRec);
          if (operator == null)
            return null;
        }

        String operatorName = "";
        if (lexer.getCode() == TGLexer.TokKind.colon) {
          if (lexer.lex() != TGLexer.TokKind.VarName) {
            tokError("expected variable namespace in dag operator");
            return null;
          }
          operatorName = lexer.getCurStrVal();
          lexer.lex();
        }

        ArrayList<Pair<Init, String>> dagArgs = new ArrayList<>();
        if (lexer.getCode() != TGLexer.TokKind.r_paren) {
          dagArgs = parseDagArgList(curRec);
          if (dagArgs.isEmpty()) return null;
        }

        if (lexer.getCode() != TGLexer.TokKind.r_paren) {
          tokError("expected ')' in dag init");
          return null;
        }

        lexer.lex();

        return new Init.DagInit(operator, operatorName, dagArgs);
      }
      case XCar:
      case XCdr:
      case XNull:
      case XConcat:
      case XSRA:
      case XSRL:
      case XSHL:
      case XStrConcat:
      case XNameConcat:
      case XCast:
      case XIf:
      case XForEach:
      case XSubst:
      case XEq:
      case XNe:
      case XLt:
      case XLe:
      case XGt:
      case XGe:
      case XAdd:
      case XAnd:
        return parseOperation(curRec);
    }
    return res;
  }

  /**
   * Parse the argument list for a dag literal expression.
   * <pre>
   * ParseDagArgList ::= Value (':' VARNAME)?
   * ParseDagArgList ::= ParseDagArgList ',' Value (':' VARNAME)?
   * </pre>
   *
   * @param curRec
   * @return
   */
  private ArrayList<Pair<Init, String>> parseDagArgList(Record curRec) {
    ArrayList<Pair<Init, String>> result = new ArrayList<>();

    while (true) {
      Init val = parseValue(curRec);
      if (val == null)
        return new ArrayList<>();

      String varName = "";
      if (lexer.getCode() == TGLexer.TokKind.colon) {
        if (lexer.lex() != TGLexer.TokKind.VarName) {
          tokError("expected variable namespace in dag literal");
          return new ArrayList<>();
        }
        varName = lexer.getCurStrVal();
        lexer.lex();
      }

      result.add(Pair.get(val, varName));
      if (lexer.getCode() != TGLexer.TokKind.comma)
        break;
      lexer.lex();    // Eat the ','.
    }

    return result;
  }

  /**
   * Parse an operator.  This returns null on error.
   * <pre>
   * Operation ::= XOperator ['<' Type '>'] '(' Args ')'
   * </pre>
   *
   * @param curRec
   * @return
   */
  private Init parseOperation(Record curRec) {
    TGLexer.TokKind code = lexer.getCode();
    switch (code) {
      default:
        tokError("unknown operation");
        return null;
      case XCar:
      case XCdr:
      case XNull:
      case XCast: {
        // Value ::= !unop '(' Value ')'
        UnaryOp opc;
        RecTy type = null;

        switch (lexer.getCode()) {
          default:
            Util.assertion(false, "Unhandled code!");
          case XCast:
            lexer.lex();
            opc = UnaryOp.CAST;
            type = parseOperatorType();

            if (type == null) {
              tokError("didn't get type for unary operator");
              return null;
            }
            break;
          case XCar:
            lexer.lex();
            opc = UnaryOp.CAR;
            break;
          case XCdr:
            lexer.lex();
            opc = UnaryOp.CDR;
            break;
          case XNull:
            lexer.lex();
            opc = UnaryOp.LNULL;
            type = new RecTy.IntRecTy();
            break;
        }
        if (lexer.getCode() != TGLexer.TokKind.l_paren) {
          tokError("expected '(' after unary operator");
          return null;
        }

        // Eat the '('
        lexer.lex();

        Init lhs = parseValue(curRec);
        if (lhs == null)
          return null;

        if (opc == UnaryOp.CAR || opc == UnaryOp.CDR || opc == UnaryOp.LNULL) {
          if (!(lhs instanceof Init.ListInit) && !(lhs instanceof Init.StringInit)
              && !(lhs instanceof TypedInit)) {
            tokError("expected list or string type argument in unary operator");
            return null;
          }
          if (lhs instanceof TypedInit) {
            TypedInit ti = (TypedInit) lhs;
            if (!(ti.getType() instanceof RecTy.ListRecTy)
                && !(ti.getType() instanceof StringRecTy)) {
              tokError("expected list or string type argument in unary operator");
              return null;
            }
          }

          if (opc == UnaryOp.CAR || opc == UnaryOp.CDR) {
            if (!(lhs instanceof Init.ListInit) && !(lhs instanceof TypedInit)) {
              tokError("expected list type argument in unary operator");
              return null;
            }

            Init.ListInit li;
            if (lhs instanceof Init.ListInit) {
              li = (Init.ListInit) lhs;
              if (li.getSize() == 0) {
                tokError("empty list argument in unary operator");
                return null;
              }

              Init item = li.getElement(0);
              TypedInit titem = null;
              if (!(item instanceof TypedInit)) {
                tokError("untyped list element in unary operator");
                return null;
              }
              titem = (TypedInit) item;

              if (opc == UnaryOp.CAR)
                type = titem.getType();
            } else {
              RecTy.ListRecTy ltype;
              TypedInit ti = (TypedInit) lhs;
              if (!(ti.getType() instanceof RecTy.ListRecTy)) {
                tokError("expected list type argument in unary operator");
                return null;
              }
              ltype = (RecTy.ListRecTy) ti.getType();
              if (opc == UnaryOp.CAR)
                type = ltype.getElementType();
              else
                type = ltype;
            }
          }
        }

        if (lexer.getCode() != TGLexer.TokKind.r_paren) {
          tokError("expected ')' in unary operator");
          return null;
        }

        // Eat the ')'.
        lexer.lex();
        try {
          return new Init.UnOpInit(opc, lhs, type)
              .fold(curRec, curMultiClass);
        } catch (Exception e) {
          return null;
        }
      }
      case XConcat:
      case XSRA:
      case XSRL:
      case XSHL:
      case XStrConcat:
      case XNameConcat:
      case XEq:
      case XNe:
      case XLt:
      case XLe:
      case XGt:
      case XGe:
      case XAdd:
      case XAnd: {
        // Value ::= !binop '(' Value ',' Value ')'
        BinaryOp opc;
        RecTy type = null;
        SourceMgr.SMLoc opLoc = lexer.getLoc();

        switch (lexer.getCode()) {
          default:
            Util.assertion(false, "Unhandled code!");
          case XConcat:
            lexer.lex();
            opc = BinaryOp.CONCAT;
            type = new RecTy.DagRecTy();
            break;
          case XSRA:
            lexer.lex();
            opc = BinaryOp.SRA;
            type = new RecTy.IntRecTy();
            break;
          case XSRL:
            lexer.lex();
            opc = BinaryOp.SRL;
            type = new RecTy.IntRecTy();
            break;
          case XSHL:
            lexer.lex();
            opc = BinaryOp.SHL;
            type = new RecTy.IntRecTy();
            break;
          case XStrConcat:
            lexer.lex();
            opc = BinaryOp.STRCONCAT;
            type = new StringRecTy();
            break;
          case XNameConcat:
            lexer.lex();
            opc = BinaryOp.NAMECONCAT;
            type = parseOperatorType();

            if (type == null) {
              tokError("didn't get type for binary operator");
              return null;
            }
            break;
          case XEq:
          case XNe:
          case XLt:
          case XLe:
          case XGt:
          case XGe: {
            int idx = BinaryOp.EQ.ordinal() +
                lexer.getCode().ordinal() -
                TGLexer.TokKind.XEq.ordinal();
            lexer.lex();  // ignores relational operation.
            opc = BinaryOp.values()[idx];
            type = new RecTy.BitRecTy();
            break;
          }
          case XAdd:
          case XAnd: {
            int idx = BinaryOp.EQ.ordinal() +
                lexer.getCode().ordinal() -
                TGLexer.TokKind.XEq.ordinal();
            lexer.lex();  // ignores relational operation.
            opc = BinaryOp.values()[idx];
            type = new RecTy.IntRecTy();
            break;
          }
        }
        if (lexer.getCode() != TGLexer.TokKind.l_paren) {
          tokError("expected '(' after binary operator");
          return null;
        }

        // eat the '('.
        lexer.lex();

        Init lhs = parseValue(curRec);
        if (lhs == null)
          return null;
        LinkedList<Init> initList = new LinkedList<>();
        initList.add(lhs);
        while (lexer.getCode() == TGLexer.TokKind.comma) {
          // eat the ','
          lexer.lex();
          Init res = parseValue(curRec);
          if (res == null)
            return null;
          initList.add(res);
        }

        if (lexer.getCode() != TGLexer.TokKind.r_paren) {
          tokError("expected ')' in binary operator");
          return null;
        }

        // eat the ')'.
        lexer.lex();

        // We allow to concate multiple strings.
        if (code == TGLexer.TokKind.XStrConcat && initList.size() >= 2) {
          Init temp = initList.removeFirst();
          while (!initList.isEmpty()) {
            temp = new BinOpInit(opc, temp, initList.removeFirst(), type);
          }
          return temp;
        }
        if (initList.size() == 2)
          return new BinOpInit(opc, initList.get(0), initList.get(1), type);
        else
          error(opLoc, "expected two operands to operator");
        return null;
      }
      case XIf:
      case XForEach:
      case XSubst: {
        // Value ::= !ternop '('  Value ','  Value ',' Value ')'
        TernOpInit.TernaryOp opc;
        RecTy type = null;

        TGLexer.TokKind lexcode = lexer.getCode();
        lexer.lex();

        switch (lexcode) {
          default:
            Util.assertion(false, "Unhandled code!");
          case XIf:
            opc = TernOpInit.TernaryOp.IF;
            break;
          case XForEach:
            opc = TernOpInit.TernaryOp.FOREACH;
            break;
          case XSubst:
            opc = TernOpInit.TernaryOp.SUBST;
            break;
        }
        if (lexer.getCode() != TGLexer.TokKind.l_paren) {
          tokError("expected '(' after ternary operator");
          return null;
        }

        lexer.lex();

        Init lhs = parseValue(curRec);
        if (lhs == null)
          return null;

        if (lexer.getCode() != TGLexer.TokKind.comma) {
          tokError("expected ',' in ternary operator");
          return null;
        }

        // eat the ','
        lexer.lex();

        Init mhs = parseValue(curRec);
        if (mhs == null)
          return null;

        if (lexer.getCode() != TGLexer.TokKind.comma) {
          tokError("expected ',' in ternary operator");
          return null;
        }

        // eat the ','
        lexer.lex();

        Init rhs = parseValue(curRec);
        if (rhs == null)
          return null;

        if (lexer.getCode() != TGLexer.TokKind.r_paren) {
          tokError("expected ')' at end of ternary operator");
          return null;
        }

        lexer.lex();

        switch (lexcode) {
          default:
            Util.assertion(false, "Unhandle code");
          case XIf: {
            if (!(mhs instanceof TypedInit) || !(rhs instanceof TypedInit)) {
              tokError("couldn't get type fo !if");
              return null;
            }

            TypedInit mhst = (TypedInit) mhs;
            TypedInit rhst = (TypedInit) rhs;

            if (mhst.getType().typeIsConvertiableTo(rhst.getType())) {
              type = rhst.getType();
            } else if (rhst.getType().typeIsConvertiableTo(mhst.getType())) {
              type = mhst.getType();
            } else {
              tokError("inconsistent types for !if");
              return null;
            }
            break;
          }
          case XForEach: {
            if (!(mhs instanceof TypedInit)) {
              tokError("couldn't get type fo !foreach");
              return null;
            }

            TypedInit mhst = (TypedInit) mhs;
            type = mhst.getType();
            break;
          }
          case XSubst: {
            if (!(rhs instanceof TypedInit)) {
              tokError("couldn't get type fo !subst");
              return null;
            }

            TypedInit rhst = (TypedInit) rhs;
            type = rhst.getType();
            break;
          }
        }
        try {
          return new TernOpInit(opc, lhs, mhs, rhs, type).fold(curRec, curMultiClass);
        } catch (Exception e) {
          return null;
        }
      }
    }
  }

  /**
   * Parse a type for an operator.  This returns null on error.
   * <pre>
   * OperatorType ::= '<' Type '>'
   * </pre>
   *
   * @return
   */
  private RecTy parseOperatorType() {
    RecTy type = null;

    if (lexer.getCode() != TGLexer.TokKind.less) {
      tokError("expected type namespace for operator");
      return null;
    }

    lexer.lex();

    type = parseType();
    if (type == null) {
      tokError("expected type namespace for operator");
      return null;
    }

    if (lexer.getCode() != TGLexer.TokKind.greater) {
      tokError("expected type namespace for operator");
      return null;
    }

    // eat the '>'
    lexer.lex();

    return type;
  }

  /**
   * Find a common t2 that T1 and T2 convert to. Return 0 if no such t2 exists.
   *
   * @param t1
   * @param t2
   * @return
   */
  public static RecTy resolveTypes(RecTy t1, RecTy t2) {
    if (!t1.typeIsConvertiableTo(t2)) {
      if (!t2.typeIsConvertiableTo(t1)) {
        RecTy.RecordRecTy recTy1;
        if (t1 instanceof RecTy.RecordRecTy) {
          recTy1 = (RecTy.RecordRecTy) t1;
          ArrayList<Record> t1SupperClasses = recTy1.getRecord().getSuperClasses();
          for (Record sc : t1SupperClasses) {
            RecTy.RecordRecTy superRecTy1 = new RecTy.RecordRecTy(sc);
            RecTy newType1 = resolveTypes(superRecTy1, t2);
            if (newType1 != null) {
              return newType1;
            }
          }
        }
        RecTy.RecordRecTy recTy2;
        if (t2 instanceof RecTy.RecordRecTy) {
          recTy2 = (RecTy.RecordRecTy) t2;
          ArrayList<Record> t2SupperClasses = recTy2.getRecord().getSuperClasses();
          for (Record sc : t2SupperClasses) {
            RecTy.RecordRecTy superRecTy2 = new RecTy.RecordRecTy(sc);
            RecTy newType2 = resolveTypes(t1, superRecTy2);
            if (newType2 != null)
              return newType2;
          }
        }
        return null;
      }
      return t2;
    }
    return t1;
  }

  /**
   * Parse and return a tblgen type.  This returns null on error.
   * <pre>
   *   Type ::= STRING                       // string type
   *   Type ::= BIT                          // bit type
   *   Type ::= BITS '<' INTVAL '>'          // bits<x> type
   *   Type ::= INT                          // int type
   *   Type ::= LIST '<' Type '>'            // list<x> type
   *   Type ::= CODE                         // code type
   *   Type ::= DAG                          // dag type
   *   Type ::= ClassID                      // Record Type
   * </pre>
   *
   * @return
   */
  private RecTy parseType() {
    switch (lexer.getCode()) {
      default:
        tokError("Unknown token when expecting a type");
        return null;
      case String:
        lexer.lex();
        return new StringRecTy();
      case Bit:
        lexer.lex();
        return new RecTy.BitRecTy();
      case Int:
        lexer.lex();
        return new RecTy.IntRecTy();
      case Code:
        lexer.lex();
        return new RecTy.CodeRecTy();
      case Dag:
        lexer.lex();
        return new RecTy.DagRecTy();
      case Id: {
        Record r = parseClassID();
        if (r != null)
          return new RecTy.RecordRecTy(r);
        return null;
      }
      case Bits: {
        if (lexer.lex() != TGLexer.TokKind.less) {
          tokError("expected '<' after bits type");
          return null;
        }
        if (lexer.lex() != TGLexer.TokKind.IntVal) {
          tokError("expected integer in bits<n> type");
          return null;
        }
        long val = lexer.getCurIntVal();
        if (lexer.lex() != TGLexer.TokKind.greater) {
          tokError("expected '>' at end of bits type");
          return null;
        }
        lexer.lex();
        return new BitsRecTy((int) val);
      }
      case List: {
        if (lexer.lex() != TGLexer.TokKind.less) {
          tokError("expected '<' after list type");
          return null;
        }

        lexer.lex();

        RecTy subType = parseType();
        if (subType == null)
          return null;

        if (lexer.getCode() != TGLexer.TokKind.greater) {
          tokError("expected '>' at end of list type");
          return null;
        }
        lexer.lex();
        return new RecTy.ListRecTy(subType);
      }
    }
  }

  /**
   * ClassID ::= ID
   *
   * @return
   */
  private Record parseClassID() {
    if (lexer.getCode() != TGLexer.TokKind.Id) {
      tokError("expected namespace for ClassID");
      return null;
    }

    Record result = records.getClass(lexer.getCurStrVal());
    if (result == null)
      tokError("Could not find class '" + lexer.getCurStrVal() + "'");

    lexer.lex();
    return result;
  }

  /**
   * ValueList ::= Value (',' Value)
   *
   * @param curRec
   * @param argsRec
   * @param eltTy
   * @return
   */
  private ArrayList<Init> parseValueList(Record curRec, Record argsRec, RecTy eltTy) {
    ArrayList<Init> result = new ArrayList<>();
    RecTy itemType = eltTy;
    int argN = 0;
    if (argsRec != null && eltTy == null) {
      ArrayList<String> targs = argsRec.getTemplateArgs();
      Util.assertion(targs.size() > argN);
      RecordVal rv = argsRec.getValue(targs.get(argN));
      Util.assertion(rv != null, "Template argument record not found?");
      itemType = rv.getType();
      ++argN;
    }

    Init init = parseValue(curRec, itemType);
    if (init == null) {
      result.clear();
      return result;
    }
    result.add(init);

    while (lexer.getCode() == TGLexer.TokKind.comma) {
      lexer.lex();    // eat the ','.

      if (argsRec != null && eltTy == null) {
        ArrayList<String> targs = argsRec.getTemplateArgs();
        if (argN >= targs.size()) {
          tokError("too many template arguments");
          result.clear();
          return result;
        }

        RecordVal rv = argsRec.getValue(targs.get(argN));
        Util.assertion(rv != null, "Template argument record not found!");
        itemType = rv.getType();
        ++argN;
      }

      init = parseValue(curRec, itemType);
      if (init == null) {
        result.clear();
        return result;
      }
      result.add(init);
    }

    return result;
  }

  private Init parseIDValue(Record curRec) {
    Util.assertion(lexer.getCode() == TGLexer.TokKind.Id, "Expected ID in parseIDValue");
    String name = lexer.getCurStrVal();
    SourceMgr.SMLoc loc = lexer.getLoc();
    lexer.lex();

    return parseIDValue(curRec, name, loc);
  }

  private Init parseIDValue(Record curRec, String name,
                            SourceMgr.SMLoc nameLoc) {
    if (curRec != null) {
      RecordVal rv = curRec.getValue(name);
      if (rv != null)
        return new VarInit(name, rv.getType());

      String templateName = curRec.getName() + ":" + name;
      if (curRec.isTemplateArg(templateName)) {
        rv = curRec.getValue(templateName);
        Util.assertion(rv != null, "Template arg does not exist?");
        return new VarInit(templateName, rv.getType());
      }
    }

    if (curMultiClass != null) {
      String mcName = curMultiClass.rec.getName() + "::" + name;
      if (curMultiClass.rec.isTemplateArg(mcName)) {
        RecordVal rv = curMultiClass.rec.getValue(mcName);
        Util.assertion(rv != null, "Template arg does not exist?");
        return new VarInit(mcName, rv.getType());
      }
    }

    Record d = records.getDef(name);
    if (d != null)
      return new Init.DefInit(d);

    error(nameLoc, "Variable not defined: '" + name + "'");
    return null;
  }

  /**
   * Parse and return a top level or multiclass def, return the record
   * corresponding to it.  This returns null on error.
   * <pre>
   *   DefInst ::= DEF ObjectName ObjectBody
   * </pre>
   *
   * @param curMultiClass
   * @return
   */
  private boolean parseDef(MultiClass curMultiClass) {
    SourceMgr.SMLoc loc = lexer.getLoc();
    Util.assertion(lexer.getCode() == TGLexer.TokKind.Def,
        "Unknown tok");
    lexer.lex();
    Record curRec = new Record(parseObjectName(), loc);
    if (curMultiClass == null) {
      if (records.getDef(curRec.getName()) != null) {
        error(loc, "def '" + curRec.getName() + "' already defined");
        return true;
      }
      records.addDef(curRec);
    } else {
      for (int i = 0, e = curMultiClass.defProtoTypes.size(); i < e; i++) {
        if (curMultiClass.defProtoTypes.get(i).getName().equals(curRec.getName())) {
          error(loc, "def '" + curRec.getName() + "' already defined in this multiclass");
          return true;
        }
      }
      curMultiClass.defProtoTypes.add(curRec);
    }

    if (parseObjectBody(curRec))
      return true;

    if (curMultiClass == null)
      curRec.resolveReferences();

    Util.assertion(curRec.getTemplateArgs().isEmpty(), "How does this get template args?");

    if (curMultiClass != null) {
      // Copy the template arguments for the multiclass into the def.
      ArrayList<String> targs = curMultiClass.rec.getTemplateArgs();
      targs.forEach(ta-> {
        RecordVal rv = curMultiClass.rec.getValue(ta);
        Util.assertion(rv != null, "Template doesn't exist?");
        curRec.addValue(rv);
      });
    }
    return false;
  }

  /**
   * If an object namespace is specified, return it.  Otherwise,
   * return an anonymous namespace.
   * ObjectName ::= ID
   * ObjectName ::=
   *
   * @return
   */
  private String parseObjectName() {
    if (lexer.getCode() == TGLexer.TokKind.Id) {
      String name = lexer.getCurStrVal();
      lexer.lex();
      return name;
    }
    return "anonymous." + (anonCounter++);
  }

  /**
   * Parse the instantiation of a multiclass.
   * <pre>
   *   DefMInst ::= DEFM ID ':' DefmSubClassRef ';'
   * </pre>
   *
   * @return
   */
  private boolean parseDefm(MultiClass curMultiClass) {
    Util.assertion(lexer.getCode() == TGLexer.TokKind.Defm,
        "Unexpected token!");
    lexer.lex(); // consume 'defm' keyword.

    SourceMgr.SMLoc defmPrefixLoc = lexer.getLoc();
    String defmPrefix = parseObjectName();
    if (lexer.getCode() != TGLexer.TokKind.colon)
      return tokError("expected ':' after defm identifier");

    lexer.lex(); // eat ':'

    ArrayList<Record> newRecRefs = new ArrayList<>();
    boolean inheritFromClass = false;

    SourceMgr.SMLoc subClassLoc = lexer.getLoc();
    SubClassReference ref = parseSubClassReference(null, true);

    while (true) {
      if (ref.rec == null)
        return true;

      Util.assertion(multiClasses.containsKey(ref.rec.getName()),
          "Didn't lookup multiclass correctly?");
      MultiClass mc = multiClasses.get(ref.rec.getName());
      ArrayList<Init> templateVals = ref.templateArgs;
      ArrayList<String> targs = mc.rec.getTemplateArgs();
      if (targs.size() < templateVals.size()) {
        return error(subClassLoc,
            "more template args specified than multiclass expects");
      }

      // Loop over all the def's in the multiclass, instantiating each one.
      for (int i = 0, e = mc.defProtoTypes.size(); i < e; i++) {
        Record defProto = mc.defProtoTypes.get(i);

        String defName = defProto.getName();
        if (defName.contains("#NAME#"))
          defName = defName.replace("#NAME#", defmPrefix);
        else {
          // Add the suffix to the defm name to get the new name.
          defName = defmPrefix + defName;
        }

        Record curRec = new Record(defName, defmPrefixLoc);
        SubClassReference subRef = new SubClassReference();
        subRef.loc = defmPrefixLoc;
        subRef.rec = defProto;
        addSubClass(curRec, subRef);

        for (int j = 0, sz = targs.size(); j < sz; j++) {
          if (j < templateVals.size()) {
            if (setValue(curRec, defmPrefixLoc, targs.get(j),
                null, templateVals.get(j)))
              return true;

            curRec.resolveReferencesTo(curRec.getValue(targs.get(j)));
            curRec.removeValue(targs.get(j));
          } else if (!curRec.getValue(targs.get(j)).getValue().isComplete()) {
            return error(subClassLoc,
                "value not specified for template #"
                    + i + " (" + targs.get(j) + ") of multiclass '"
                    + mc.rec.getName() + "'");
          }
        }

        for (int j = 0, sz = letStack.size(); j < sz; ++j) {
          ArrayList<LetRecord> list = letStack.get(j);
          for (int k = 0, sz2 = list.size(); k < sz2; k++) {
            LetRecord let = list.get(k);
            if (setValue(curRec, let.loc, let.name, let.bits, let.value)) {
              error(defmPrefixLoc, "when instantiating thsi defm");
              return true;
            }
          }
        }

        if (records.getDef(curRec.getName()) != null) {
          return error(defmPrefixLoc, "def '" + curRec.getName() +
              "' already defined, instantiating defm with subdef '"
              + defProto.getName() + "'");
        }

        // Don't create a top level definition for defm inside multiclasses,
        // instead, only update the prototypes and bind the template args
        // with the new created definition.
        if (this.curMultiClass != null) {
          for (int ii = 0, sz = this.curMultiClass.defProtoTypes.size(); ii < sz; ++ii) {
            if (this.curMultiClass.defProtoTypes.get(ii).getName().equals(curRec.getName()))
              return error(defmPrefixLoc, String.format("defm '%s' already defined in this multiclass", curRec.getName()));
            this.curMultiClass.defProtoTypes.add(curRec);

            ArrayList<String> ta = this.curMultiClass.rec.getTemplateArgs();
            ta.forEach(a -> {
              RecordVal rv = this.curMultiClass.rec.getValue(a);
              Util.assertion(rv != null, "Template arg doesn't exist?");
              curRec.addValue(rv);
            });
          }
        }
        else {
          records.addDef(curRec);
        }

        newRecRefs.add(curRec);
      }

      if (lexer.getCode() != TGLexer.TokKind.comma)
        break;
      lexer.lex();

      subClassLoc = lexer.getLoc();

      // A defm can inherit from regular classes (non-multiclass) as
      // long as they come in the end of the inheritance list.
      inheritFromClass = records.getClass(lexer.getCurStrVal()) != null;
      if (inheritFromClass)
        break;

      ref = parseSubClassReference(null, true);
    }

    if (inheritFromClass) {
      // Process all the classes to inherit as if they were part of a
      // regular 'def' and inherit all record values.
      SubClassReference subClass = parseSubClassReference(null, false);
      while (true) {
        // check for error.
        if (subClass.rec == null) return true;

        // Get the expanded definition prototypes and teach them about
        // the record values the current class to inherit has
        for (int i = 0, e = newRecRefs.size(); i < e; i++) {
          Record curRec = newRecRefs.get(i);
          // add it.
          if (addSubClass(curRec, subClass))
            return true;

          // Process any variables on the let stack.
          for (int j = 0, sz = letStack.size(); j < sz; j++) {
            for (int k = 0, size = letStack.get(j).size(); k < size; k++) {
              if (setValue(curRec, letStack.get(j).get(k).loc, letStack.get(j).get(k).name,
                  letStack.get(j).get(k).bits, letStack.get(j).get(k).value));
              return true;
            }
          }
        }

        if (lexer.getCode() != TGLexer.TokKind.comma) break;
        lexer.lex();  // eat the ','
        subClass = parseSubClassReference(null, false);
      }
    }

    if (curMultiClass == null) {
      newRecRefs.forEach(curRec-> {
        // See Record::setName().  This resolve step will see any new
        // name for the def that might have been created when resolving
        // inheritance, values and arguments above.
        curRec.resolveReferences();
      });
    }

    if (lexer.getCode() != TGLexer.TokKind.semi)
      return tokError("expected ';' at end of defm");

    lexer.lex();
    return false;
  }

  /**
   * Parse a tblgen class definition.
   * <p>
   * ClassInst ::= CLASS ID TemplateArgList? ObjectBody
   *
   * @return
   */
  private boolean parseClass() {
    Util.assertion(lexer.getCode() == TGLexer.TokKind.Class);

    lexer.lex();

    if (lexer.getCode() != TGLexer.TokKind.Id) {
      return tokError("expected class namespace after 'class'");
    }

    Record curRec = records.getClass(lexer.getCurStrVal());
    if (curRec != null) {
      // Check if the current Record is a declaration but definition.
      if (!curRec.isDeclaration()) {
        return tokError("Class '" + curRec.getName() + "' already defined");
      }
    } else {
      curRec = new Record(lexer.getCurStrVal(), lexer.getLoc());
      records.addClass(curRec);
    }

    // eat the namespace.
    lexer.lex();

    if (lexer.getCode() == TGLexer.TokKind.less)
      if (parseTemplateArgList(curRec))
        return true;


    boolean res = parseObjectBody(curRec);
    //if (TableGen.DEBUG)
    //    curRec.dump();

    // Dump the debug information for checking there is field changing of
    // Record Register caused by RegisterWithSubRegs.
    // Done
    /**
     if (TableGen.DEBUG)
     {
     Record r = Record.records.getClass("Register");
     if (r != null)
     r.dump();
     }
     */
    return res;
  }

  /**
   * Parse the body of a def or class.  This consists of an
   * optional ClassList followed by a Body.  CurRec is the current def or class
   * that is being parsed.
   * <pre>
   *   ObjectBody      ::= BaseClassList Body
   *   BaseClassList   ::= empty
   *   BaseClassList   ::= ':' BaseClassListNE
   *   BaseClassListNE ::= SubClassRef (',' SubClassRef)*
   * </pre>
   *
   * @param curRec
   * @return
   */
  private boolean parseObjectBody(Record curRec) {
    if (lexer.getCode() == TGLexer.TokKind.colon) {
      lexer.lex();

      SubClassReference subClass = parseSubClassReference(curRec, false);
      while (true) {
        if (subClass.rec == null)
          return true;

        // add it.
        if (addSubClass(curRec, subClass))
          return true;

        if (lexer.getCode() != TGLexer.TokKind.comma)
          break;

        lexer.lex();    // eat the ','
        subClass = parseSubClassReference(curRec, false);
      }
    }

    for (int i = 0, e = letStack.size(); i != e; i++) {
      for (int j = 0, sz = letStack.get(i).size(); j < sz; j++) {
        LetRecord lr = letStack.get(i).get(j);
        if (setValue(curRec, lr.loc, lr.name, lr.bits, lr.value))
          return true;
      }
    }

    return parseBody(curRec);
  }

  /**
   * Parse a reference to a subclass or to a templated
   * subclass.  This returns a SubClassRefTy with a null Record* on error.
   * <pre>
   *  SubClassRef ::= ClassID
   *  SubClassRef ::= ClassID '<' ValueList '>'
   * </pre>
   *
   * @param curRec
   * @param isDefm
   * @return
   */
  private SubClassReference parseSubClassReference(Record curRec, boolean isDefm) {
    SubClassReference result = new SubClassReference();
    result.loc = lexer.getLoc();

    if (isDefm)
      result.rec = parseDefmID();
    else
      result.rec = parseClassID();

    if (lexer.getCode() != TGLexer.TokKind.less)
      return result;

    lexer.lex();

    if (lexer.getCode() == TGLexer.TokKind.greater) {
      tokError("subclass reference requires a non-empty list of template values");
      result.rec = null;
      return result;
    }

    result.templateArgs = parseValueList(curRec, result.rec, null);
    if (result.templateArgs.isEmpty()) {
      result.rec = null;
      return result;
    }

    if (lexer.getCode() != TGLexer.TokKind.greater) {
      tokError("expected '>' in template value list");
      result.rec = null;
      return result;
    }

    lexer.lex();
    return result;
  }

  private Record parseDefmID() {
    if (lexer.getCode() != TGLexer.TokKind.Id) {
      tokError("expected multiclass namespace");
      return null;
    }

    if (!multiClasses.containsKey(lexer.getCurStrVal())) {
      tokError("couldn't find multiclass '" + lexer.getCurStrVal() + "'");
      return null;
    }

    MultiClass mc = multiClasses.get(lexer.getCurStrVal());
    lexer.lex();
    return mc.rec;
  }

  /**
   * Read the body of a class or def.  Return true on error, false on
   * success.
   * <pre>
   *   Body     ::= ';'
   *   Body     ::= '{' BodyList '}'
   *   BodyList BodyItem*
   * </pre>
   *
   * @param curRec
   * @return
   */
  private boolean parseBody(Record curRec) {
    if (lexer.getCode() == TGLexer.TokKind.semi) {
      lexer.lex();
      return false;
    }

    if (lexer.getCode() != TGLexer.TokKind.l_brace) {
      return error(lexer.getLoc(), "expected ';' or '{' to start body");
    }

    lexer.lex();

    while (lexer.getCode() != TGLexer.TokKind.r_brace) {
      if (parseBodyItem(curRec))
        return true;
    }

    lexer.lex();
    return false;
  }

  /**
   * Parse a single item at within the body of a def or class.
   * <pre>
   *   BodyItem ::= Declaration ';'
   *   BodyItem ::= LET ID OptionalBitList '=' Value ';'
   * </pre>
   *
   * @param curRec
   * @return
   */
  private boolean parseBodyItem(Record curRec) {
    if (lexer.getCode() != TGLexer.TokKind.Let) {
      if (parseDeclaration(curRec, false).isEmpty())
        return true;

      if (lexer.getCode() != TGLexer.TokKind.semi)
        return tokError("expected ';' after declaration");

      lexer.lex();
      return false;
    }

    if (lexer.lex() != TGLexer.TokKind.Id) {
      return tokError("expected field identifier after let");
    }

    SourceMgr.SMLoc idLoc = lexer.getLoc();
    String fieldName = lexer.getCurStrVal();
    lexer.lex();

    TIntArrayList bitList = new TIntArrayList();
    if (parseOptionalBitList(bitList))
      return true;

    bitList.reverse();

    if (lexer.getCode() != TGLexer.TokKind.equal)
      return tokError("expected '=' in let expression");

    lexer.lex();

    RecordVal rv = curRec.getValue(fieldName);
    if (rv == null)
      return tokError("value '" + fieldName + "' unknown!");

    RecTy type = rv.getType();

    Init val = parseValue(curRec, type);
    if (val == null)
      return true;

    if (lexer.getCode() != TGLexer.TokKind.semi)
      return tokError("expected ';' after let expression");

    lexer.lex();

    return setValue(curRec, idLoc, fieldName, bitList, val);
  }

  /**
   * Parse either a bit list in {}'s or nothing.
   * <pre>
   *   OptionalBitList ::= '{' RangeList '}'
   *   OptionalBitList ::=
   * </pre>
   *
   * @param bitlist
   * @return
   */
  private boolean parseOptionalBitList(TIntArrayList bitlist) {
    if (lexer.getCode() != TGLexer.TokKind.l_brace)
      return false;

    SourceMgr.SMLoc startLoc = lexer.getLoc();
    lexer.lex();

    parseRangeList(bitlist);
    if (bitlist.isEmpty()) return true;

    if (lexer.getCode() != TGLexer.TokKind.r_brace) {
      tokError("expected '}' at end of bit list");
      return error(startLoc, "to match this '{'");
    }

    lexer.lex();        // eat the '}'
    return false;
  }

  /**
   * Declaration ::= FIELD? Type ID ('=' Value)?
   *
   * @param curRec
   * @param parsingTemplateArgs
   * @return
   */
  private String parseDeclaration(Record curRec, boolean parsingTemplateArgs) {
    boolean hasField = lexer.getCode() == TGLexer.TokKind.Field;
    if (hasField) lexer.lex();

    RecTy type = parseType();
    if (type == null) return "";

    if (lexer.getCode() != TGLexer.TokKind.Id) {
      tokError("Expected identifier in declaration");
      return "";
    }

    SourceMgr.SMLoc idLoc = lexer.getLoc();
    String declName = lexer.getCurStrVal();
    lexer.lex();

    if (parsingTemplateArgs) {
      if (curRec != null)
        declName = curRec.getName() + ":" + declName;
      else
        Util.assertion(curMultiClass != null);

      if (curMultiClass != null)
        declName = curMultiClass.rec.getName() + "::" + declName;
    }

    if (addValue(curRec, idLoc, new RecordVal(declName, type, hasField ? 1 : 0)))
      return "";

    if (lexer.getCode() == TGLexer.TokKind.equal) {
      lexer.lex();
      SourceMgr.SMLoc loc = lexer.getLoc();
      Init val = parseValue(curRec, type);
      if (val == null || setValue(curRec, loc, declName, null, val))
        return "";
    }

    return declName;
  }

  /**
   * Read a template argument list, which is a non-empty
   * sequence of template-declarations in <>'s.  If CurRec is non-null, these are
   * template args for a def, which may or may not be in a multiclass.  If null,
   * these are the template args for a multiclass.
   * <pre>
   *    TemplateArgList ::= '<' Declaration (',' Declaration)* '>'
   * </pre>
   *
   * @param curRec
   * @return
   */
  private boolean parseTemplateArgList(Record curRec) {
    Util.assertion(lexer.getCode() == TGLexer.TokKind.less, "Not a template arg list!");
    lexer.lex();

    Record theRecToAddTo = curRec != null ? curRec : curMultiClass.rec;

    String templateArg = parseDeclaration(curRec, true);
    if (templateArg.isEmpty())
      return true;

    theRecToAddTo.addTemplateArg(templateArg);

    while (lexer.getCode() == TGLexer.TokKind.comma) {
      lexer.lex();

      templateArg = parseDeclaration(curRec, true);
      if (templateArg.isEmpty())
        return true;
      theRecToAddTo.addTemplateArg(templateArg);
    }

    if (lexer.getCode() != TGLexer.TokKind.greater)
      return tokError("expected '>' at end of template argument list");

    // eat the '>'.
    lexer.lex();
    return false;
  }

  /**
   * Parse a multiclass definition.
   * <pre>
   *  MultiClassInst ::= MULTICLASS ID TemplateArgList?
   *                     ':' BaseMultiClassList '{' MultiClassDef+ '}'
   * </pre>
   *
   * @return
   */
  private boolean parseMultiClass() {
    Util.assertion(lexer.getCode() == TGLexer.TokKind.Multiclass,
        "Unexpected token");
    lexer.lex();
    if (lexer.getCode() != TGLexer.TokKind.Id)
      return tokError("expected identifier after multiclass for namespace");

    String name = lexer.getCurStrVal();
    if (multiClasses.containsKey(name))
      return tokError("multiclass '" + name + "' already defined");

    curMultiClass = new MultiClass(name, lexer.getLoc());
    multiClasses.put(name, curMultiClass);

    lexer.lex();

    if (lexer.getCode() == TGLexer.TokKind.less)
      if (parseTemplateArgList(null))
        return true;

    boolean isHerits = false;

    if (lexer.getCode() == TGLexer.TokKind.colon) {
      isHerits = true;

      lexer.lex();

      SubMultiClassReference subMultiClass = parseMultiClassReference(curMultiClass);

      while (true) {
        if (subMultiClass.mc == null)
          return true;

        if (addSubMultiClass(curMultiClass, subMultiClass))
          return true;

        if (lexer.getCode() != TGLexer.TokKind.comma)
          break;
        lexer.lex();
        subMultiClass = parseSubMultiClassReference(curMultiClass);
      }
    }

    if (lexer.getCode() != TGLexer.TokKind.l_brace) {
      if (!isHerits)
        return tokError("expected '{' in multiclass definition");
      else {
        if (lexer.getCode() != TGLexer.TokKind.semi)
          return tokError("expected ';' in multiclass definition");
        else
          lexer.lex();    // eat the ';'.
      }
    } else {
      if (lexer.lex() == TGLexer.TokKind.r_brace) {
        return tokError("multiclass must contains at least one def");
      }
      while (lexer.getCode() != TGLexer.TokKind.r_brace) {
        switch (lexer.getCode()) {
          default:
            return tokError("expected 'let', 'def', or 'defm' in multiclass body");
          case Let:
          case Def:
          case Defm:
            if (parseObject(curMultiClass))
              return true;
            break;
        }
      }

      lexer.lex();
    }
    // if (TableGen.DEBUG)
    //    curMultiClass.dump();

    // Clear the current being parsed multiclass for avoiding make effect
    // on subsequent parsing.
    curMultiClass = null;
    return false;
  }

  /**
   * Parse a reference to a subclass or to a
   * templated submulticlass.  This returns a SubMultiClassRefTy with a null
   * Record* on error.
   * <pre>
   *  SubMultiClassRef ::= MultiClassID
   *  SubMultiClassRef ::= MultiClassID '<' ValueList '>'
   * </pre>
   *
   * @param curMC
   * @return
   */
  private SubMultiClassReference parseSubMultiClassReference(MultiClass curMC) {
    SubMultiClassReference result = new SubMultiClassReference();
    result.refLoc = lexer.getLoc();

    result.mc = parseMultiClassID();

    if (result.mc == null)
      return result;

    if (lexer.getCode() != TGLexer.TokKind.less)
      return result;

    lexer.lex();

    if (lexer.getCode() == TGLexer.TokKind.greater) {
      tokError("subclass reference requires a non-empty list of template values");
      result.mc = null;
      return result;
    }

    result.templateArgs = parseValueList(curMC.rec, result.mc.rec, null);
    if (result.templateArgs.isEmpty()) {
      result.mc = null;
      return result;
    }

    if (lexer.getCode() != TGLexer.TokKind.greater) {
      tokError("expected '>' in template value list");
      result.mc = null;
      return result;
    }

    lexer.lex();
    return result;
  }

  private boolean addSubMultiClass(MultiClass curMC,
                                   SubMultiClassReference subMultiClass) {
    MultiClass smc = subMultiClass.mc;
    Record curRec = curMC.rec;

    ArrayList<RecordVal> mcVals = curRec.getValues();

    ArrayList<RecordVal> smcVals = smc.rec.getValues();
    for (int i = 0, e = smcVals.size(); i < e; i++) {
      if (addValue(curRec, subMultiClass.refLoc, smcVals.get(i)))
        return true;
    }

    int newDefStart = curMC.defProtoTypes.size();

    for (Record r : smc.defProtoTypes) {
      Record newDef = r.clone();

      for (int i = 0, e = mcVals.size(); i < e; i++)
        if (addValue(newDef, subMultiClass.refLoc, mcVals.get(i)))
          return true;

      curMC.defProtoTypes.add(newDef);
    }

    ArrayList<String> smcTArgs = smc.rec.getTemplateArgs();

    if (smcTArgs.size() < subMultiClass.templateArgs.size())
      return error(subMultiClass.refLoc, "More template args specified than expected");

    for (int i = 0, e = smcTArgs.size(); i < e; i++) {
      if (i < subMultiClass.templateArgs.size()) {
        if (setValue(curRec, subMultiClass.refLoc, smcTArgs.get(i),
            null, subMultiClass.templateArgs.get(i)))
          return true;

        curRec.resolveReferencesTo(curRec.getValue(smcTArgs.get(i)));

        curRec.removeValue(smcTArgs.get(i));

        for (int j = newDefStart, end = curMC.defProtoTypes.size(); j < end; j++) {
          Record def = curMC.defProtoTypes.get(j);

          if (setValue(def, subMultiClass.refLoc, smcTArgs.get(i),
              null, subMultiClass.templateArgs.get(i)))
            return true;

          def.resolveReferencesTo(def.getValue(smcTArgs.get(i)));

          def.removeValue(smcTArgs.get(i));
        }
      } else if (!curRec.getValue(smcTArgs.get(i)).getValue().isComplete()) {
        return error(subMultiClass.refLoc,
            "Value not specified for template argument #"
                + i + " (" + smcTArgs.get(i) + ") of subclass '"
                + smc.rec.getName() + "'!");
      }
    }

    return false;
  }

  /**
   * Parse a reference to a subclass or to a templated submulticlass.
   * This returns a SubMultiClassRefTy with a null Record* on error.
   * <pre>
   *  SubMultiClassRef ::= MultiClassID
   *  SubMultiClassRef ::= MultiClassID '<' ValueList '>'
   * </pre>
   *
   * @param curMultiClass
   * @return
   */
  private SubMultiClassReference parseMultiClassReference(MultiClass curMultiClass) {
    SubMultiClassReference result = new SubMultiClassReference();

    result.refLoc = lexer.getLoc();

    result.mc = parseMultiClassID();

    if (lexer.getCode() != TGLexer.TokKind.less)
      return result;

    lexer.lex();

    if (lexer.getCode() == TGLexer.TokKind.greater) {
      tokError("subclass reference requires a non-empty list of template values");
      result.mc = null;
      return result;
    }

    result.templateArgs = parseValueList(curMultiClass.rec, result.mc.rec, null);
    if (result.templateArgs.isEmpty()) {
      result.mc = null;
      return result;
    }

    if (lexer.getCode() != TGLexer.TokKind.greater) {
      tokError("expected '>' in template value list");
      result.mc = null;
      return result;
    }

    lexer.lex();
    return result;
  }

  /**
   * MultiClassID ::= ID.
   *
   * @return
   */
  private MultiClass parseMultiClassID() {
    if (lexer.getCode() != TGLexer.TokKind.Id) {
      tokError("expected namespace for ClassID");
      return null;
    }

    MultiClass result = null;
    if (!multiClasses.containsKey(lexer.getCurStrVal())) {
      tokError("Couldn't find class '" + lexer.getCurStrVal() + "'");
    }
    result = multiClasses.get(lexer.getCurStrVal());

    lexer.lex();
    return result;
  }

}
