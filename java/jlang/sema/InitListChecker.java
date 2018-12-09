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

package jlang.sema;

import jlang.ast.Tree.*;
import jlang.ast.Tree.DesignatedInitExpr.Designator;
import jlang.clex.IdentifierInfo;
import jlang.cparser.ActionResult;
import jlang.diag.DiagnosticSemaTag;
import jlang.diag.FixItHint;
import jlang.support.SourceLocation;
import jlang.support.SourceRange;
import jlang.type.ArrayType;
import jlang.type.QualType;
import jlang.type.RecordType;
import tools.APSInt;
import tools.OutRef;
import tools.Util;

import java.util.ArrayList;
import java.util.HashMap;

import static jlang.diag.DiagnosticSemaTag.*;
import static jlang.sema.Sema.AssignAction.AA_Initializing;

/**
 * @author Jianping Zeng
 * @version 0.4
 */
public class InitListChecker {
  private Sema sema;
  private boolean hadError;
  private HashMap<InitListExpr, InitListExpr> syntacticToSemantic;
  private InitListExpr fullyStructuredList;

  public InitListChecker(Sema sema, InitListExpr initList,
                         OutRef<QualType> declType) {
    hadError = false;
    this.sema = sema;
    syntacticToSemantic = new HashMap<>();

    OutRef<Integer> newIndex = new OutRef<>(0);
    OutRef<Integer> newStructuredIndex = new OutRef<>(0);

    fullyStructuredList = getStructuredSubobjectInit(initList,
        newIndex.get(), declType.get(), null, 0, initList.getSourceRange());

    checkExplicitInitList(initList, newIndex, declType, fullyStructuredList,
        newStructuredIndex, true);

    if (!hadError)
      fillValueInInitializations(fullyStructuredList);
  }

  /**
   * Recursively replaces NULL values within the given initializer list
   * with expressions that perform value-initialization of the
   * appropriate type.
   *
   * @param list
   */
  private void fillValueInInitializations(InitListExpr list) {
    Util.assertion(list.getType() != sema.context.VoidTy, "Should not have void type");


    SourceLocation loc = list.getSourceRange().getBegin();
    if (list.getSyntacticForm() != null) {
      loc = list.getSyntacticForm().getSourceRange().getBegin();
    }

    RecordType rt;
    if ((rt = list.getType().getAsRecordType()) != null) {
      int init = 0, numInits = list.getNumInits();
      Decl.RecordDecl rdDecl = rt.getDecl();
      for (int i = 0, e = rdDecl.getNumFields(); i != e; i++) {
        Decl.FieldDecl fd = rdDecl.getDeclAt(i);
        if (fd.isUnamaedBitField())
          continue;

        if (init >= numInits || list.getInitAt(init) == null) {
          if (sema.checkValueInitialization(fd.getType(), loc)) {
            hadError = true;
            break;
          }

          if (init < numInits && !hadError) {
            list.setInitAt(init, new ImplicitValueInitExpr(fd.getType()));
          }
        } else if (list.getInitAt(init) instanceof InitListExpr) {
          InitListExpr innerList = (InitListExpr) list.getInitAt(init);
          fillValueInInitializations(innerList);
        }
        ++init;

        // Only look at the first initialization of a union.
        if (rdDecl.isUnion())
          break;
      }
      return;
    }

    QualType eltType;

    int numInits = list.getNumInits();
    long numElements = numInits;
    ArrayType at = sema.context.getAsArrayType(list.getType());
    if (at != null) {
      eltType = at.getElementType();
      if (at instanceof ArrayType.ConstantArrayType) {
        numElements = ((ArrayType.ConstantArrayType) at).getSize().getZExtValue();
      }
    } else {
      eltType = list.getType();
    }

    for (int i = 0; i != numElements; i++) {
      if (i >= numInits || list.getInitAt(i) == null) {
        if (sema.checkValueInitialization(eltType, loc)) {
          hadError = true;
          return;
        }
        if (i < numInits && !hadError) {
          list.setInitAt(i, new ImplicitValueInitExpr(eltType));
        }
      } else if (list.getInitAt(i) instanceof InitListExpr) {
        fillValueInInitializations((InitListExpr) list.getInitAt(i));
      }
    }
  }

  private static Expr isStringInit(Expr init, QualType type, ASTContext ctx) {
    ArrayType at = ctx.getAsArrayType(type);
    if (at == null)
      return null;

    if (!(at instanceof ArrayType.ConstantArrayType) && !(at instanceof ArrayType.IncompleteArrayType))
      return null;

    QualType elementType = at.getElementType();

    // Ignores all of parenthesis.
    init = init.ignoreParens();

    // Only allow String Literal.
    StringLiteral sl = init instanceof StringLiteral ?
        (StringLiteral) init :
        null;
    if (sl == null)
      return null;

    // char array can be initialized with a narrow string.
    // char x[] = "xxx".
    return elementType.isCharType() ? init : null;
  }

  private static boolean checkSingleInitializer(
      OutRef<Expr> init,
      QualType declType,
      Sema s) {
    QualType initType = init.get().getType();

    OutRef<ActionResult<Expr>> x = new OutRef<>(new ActionResult<>(init.get()));
    AssignConvertType convTy = s.checkSingleAssignmentConstraints(declType, x);
    if (x.get().isInvalid()) {
      // Invalid expression.
      init.set(null);
      return true;
    }
    init.set(x.get().get());
    return s.diagnoseAssignmentResult(convTy, init.get().getLocStart(),
        declType, initType, init.get(), AA_Initializing);
  }

  private void checkScalarType(
      InitListExpr initList,
      QualType declType,
      OutRef<Integer> index,
      InitListExpr structuredList,
      OutRef<Integer> structuredIndex) {
    if (index.get() < initList.getNumInits()) {
      Expr expr = initList.getInitAt(index.get());
      if (expr instanceof InitListExpr) {
        sema.diag(initList.getLocStart(),
            err_many_braces_around_scalar_init)
            .addSourceRange(initList.getSourceRange())
            .emit();
        hadError = true;
        index.set(index.get() + 1);
        structuredIndex.set(structuredIndex.get() + 1);
        return;
      } else if (expr instanceof DesignatedInitExpr) {
        sema.diag(expr.getSourceRange().getBegin(),
            err_designator_for_scalar_init)
            .addTaggedVal(declType)
            .addSourceRange(expr.getSourceRange())
            .emit();
        hadError = true;
        index.set(index.get() + 1);
        structuredIndex.set(structuredIndex.get() + 1);
        return;
      }

      Expr saveExpr = expr;
      OutRef<Expr> e = new OutRef<>(expr);
      boolean failure = checkSingleInitializer(e, declType, sema);
      expr = e.get();
      if (failure) {
        hadError = true;
      } else if (saveExpr != expr) {
        initList.setInitAt(index.get(), expr);
      }

      if (hadError)
        structuredIndex.set(structuredIndex.get() + 1);
      else
        updateStructuredListElement(structuredList, structuredIndex, expr);
      index.set(index.get() + 1);
    } else {
      sema.diag(initList.getLocStart(),
          err_empty_scalar_initializer)
          .addSourceRange(initList.getSourceRange())
          .emit();
      hadError = true;
      index.set(index.get() + 1);
      structuredIndex.set(structuredIndex.get() + 1);
    }
  }

  private void checkStructUnionTypes(
      InitListExpr initList,
      QualType declType,
      OutRef<Decl.RecordDecl> structDecl,
      int field,
      boolean subObjectIsDesignatedContext,
      OutRef<Integer> index,
      InitListExpr structuredList,
      OutRef<Integer> structuredIndex,
      boolean topLevelObject) {
    if (structDecl.get().isInvalidDecl()) {
      hadError = true;
      return;
    }

    if (declType.isUnionType() && initList.getNumInits() == 0) {
      Decl.RecordDecl rd = declType.getAsRecordType().getDecl();
      for (int i = 0, e = rd.getNumFields(); i != e; i++) {
        Decl.FieldDecl fd = rd.getDeclAt(i);
        if (fd.getIdentifier() != null) {
          structuredList.setInitializedFieldInUnion(fd);
          break;
        }
      }
      return;
    }

    int fieldEnd = structDecl.get().getNumFields();
    boolean initializedSomething = false;

    while (index.get() < initList.getNumInits()) {
      Expr init = initList.getInitAt(index.get());

      if (init instanceof DesignatedInitExpr) {
        DesignatedInitExpr die = (DesignatedInitExpr) init;
        if (!subObjectIsDesignatedContext)
          return;

        OutRef<Integer> fieldWrapper = new OutRef<>(field);
        OutRef<QualType> tyWrapper = new OutRef<>(declType);
        if (checkDesignatedInitializer(initList, die,
            0, tyWrapper, structDecl,
            fieldWrapper,
            null, index, structuredList,
            structuredIndex, true, topLevelObject)) {
          hadError = true;
        }
        field = fieldWrapper.get();
        declType = tyWrapper.get();

        initializedSomething = true;
        continue;
      }

      if (field == fieldEnd)
        break;      // done, we are run out of fields.

      // we have already initialized a member of a union, we are done!
      if (initializedSomething && declType.isUnionType())
        break;

      Decl.FieldDecl fd = structDecl.get().getDeclAt(field);

      // If we are hit the flexible array member at the end , we are done!
      if (fd.getType().isIncompleteArrayType()) {
        break;
      }
      if (fd.isUnamaedBitField()) {
        // Don't initialize unnamed bitfields, e.g. "int : 20;"
        field++;
        continue;
      }

      checkSubElementType(initList, fd.getType(), index,
          structuredList, structuredIndex);
      initializedSomething = true;

      if (declType.isUnionType()) {
        structuredList.setInitializedFieldInUnion(fd);
      }

      ++field;
    }
    Decl.FieldDecl fd = null;
    if (field == fieldEnd || (fd = structDecl.get().getDeclAt(field)) == null
        || !fd.getType().isIncompleteArrayType()) {
      return;
    }


    if (!topLevelObject && (!(initList.getInitAt(index.get()) instanceof InitListExpr) ||
        ((InitListExpr) initList.getInitAt(index.get())).getNumInits() > 0)) {
      sema.diag(initList.getInitAt(index.get()).getLocStart(),
          err_flexible_array_init_nonempty)
          .addSourceRange(initList.getInitAt(index.get()).getSourceRange())
          .emit();
      sema.diag(fd.getLocation(),
          note_flexible_array_member)
          .addTaggedVal(fd).emit();
      hadError = true;
      index.set(index.get() + 1);
      return;
    } else {
      sema.diag(initList.getInitAt(index.get()).getSourceRange().getBegin(),
          ext_flexible_array_init);
      sema.diag(fd.getLocation(),
          note_flexible_array_member)
          .addTaggedVal(fd).emit();
    }

    if (initList.getInitAt(index.get()) instanceof InitListExpr) {
      checkSubElementType(initList, fd.getType(), index, structuredList,
          structuredIndex);
    } else {
      checkImplicitInitList(initList, fd.getType(), index,
          structuredList, structuredIndex, false);
    }
  }

  private static void checkStringInit(Expr str,
                                      OutRef<QualType> declType, Sema s) {
    long strLength = s.context.getAsConstantArrayType(str.getType())
        .getSize().getZExtValue();

    ArrayType at = s.context.getAsArrayType(declType.get());
    if (at instanceof ArrayType.IncompleteArrayType) {
      ArrayType.IncompleteArrayType iat = (ArrayType.IncompleteArrayType) at;
      // C99 6.7.8p14. We have an array of character type with unknown size
      // being initialized to a string literal.
      APSInt constVal = new APSInt(32);
      constVal.assign(strLength);

      // Return a new array type (C99 6.7.8p22).
      declType.set(s.context.getConstantArrayWithoutExprType(iat.getElementType(),
          constVal, ArrayType.ArraySizeModifier.Normal, 0));
      return;
    }

    ArrayType.ConstantArrayType cat = (ArrayType.ConstantArrayType) at;

    // C99 6.7.8p14. We have an array of character type with known size.  However,
    // the size may be smaller or larger than the string we are initializing.
    if (strLength - 1 > cat.getSize().getZExtValue()) {
      s.diag(str.getSourceRange().getBegin(),
          warn_initializer_string_for_char_array_too_long)
          .addSourceRange(str.getSourceRange());
    }

    // Set the type to the actual size that we are initializing.  If we have
    // something like:
    //   char x[1] = "foo";
    // then this will set the string literal's type to char[1].
    str.setType(declType.get());
  }

  private void updateStructuredListElement(InitListExpr structuredList,
                                           OutRef<Integer> structuredIndex, Expr expr) {
    if (structuredList == null)
      return;

    Expr prevInit = structuredList.updateInit(structuredIndex.get(), expr);
    if (prevInit != null) {
      // This initializer overwrites a previous initializer. Warn.
      sema.diag(expr.getSourceRange().getBegin(),
          warn_initializer_overrides)
          .addSourceRange(expr.getSourceRange()).emit();
      sema.diag(prevInit.getSourceRange().getBegin(),
          note_previous_initializer).addTaggedVal(0)
          .addSourceRange(prevInit.getSourceRange()).emit();
    }

    structuredIndex.set(structuredIndex.get() + 1);
  }

  private void checkSubElementType(
      InitListExpr initList,
      QualType elementType,
      OutRef<Integer> index,
      InitListExpr structuredList,
      OutRef<Integer> structuredIndex) {
    Expr expr = initList.getInitAt(index.get());
    Expr str;
    if (expr instanceof InitListExpr) {
      InitListExpr subList = (InitListExpr) expr;
      OutRef<Integer> newIndex = new OutRef<>(0);
      OutRef<Integer> newStructuredIndex = new OutRef<>(0);
      InitListExpr newStructuredList = getStructuredSubobjectInit(
          initList, index.get(),
          elementType, structuredList,
          structuredIndex.get(),
          subList.getSourceRange());
      checkExplicitInitList(subList, newIndex,
          new OutRef<>(elementType), newStructuredList,
          newStructuredIndex, false);
      structuredIndex.set(structuredIndex.get() + 1);
      index.set(index.get() + 1);
    } else if ((str = isStringInit(expr, elementType, sema.context)) != null) {
      checkStringInit(str, new OutRef<>(elementType), sema);
      updateStructuredListElement(structuredList, structuredIndex, str);
      index.set(index.get() + 1);
    } else if (elementType.isScalarType()) {
      checkScalarType(initList, elementType, index, structuredList, structuredIndex);
    } else {
      // C99 6.7.8p13:
      //
      //   The initializer for a structure or union object that has
      //   automatic storage duration shall be either an initializer
      //   list as described below, or a single expression that has
      //   compatible structure or union type. In the latter case, the
      //   initial value of the object, including unnamed members, is
      //   that of the expression.
      if (elementType.isRecordType() && sema.context.hasSameUnqualifiedType(expr.getType(), elementType)) {
        updateStructuredListElement(structuredList, structuredIndex, expr);
        index.set(index.get() + 1);
      }

      if (elementType.isAggregateType()) {
        checkImplicitInitList(initList, elementType, index,
            structuredList, structuredIndex, false);
        structuredIndex.set(structuredIndex.get() + 1);
      } else {
        sema.performCopyInitialization(new OutRef<>(expr),
            elementType, AA_Initializing);
        hadError = true;
        index.set(index.get() + 1);
        structuredIndex.set(structuredIndex.get() + 1);
      }
    }
  }

  /**
   * Expand a field designator that refers to a member of an
   * anonymous struct or union into a series of field designators that
   * refers to the field within the appropriate subobject.
   * <p>
   * Field/FieldIndex will be updated to point to the (new)
   * currently-designated field.
   */
  private static void expandAnonymousFieldDesignator(Sema sema,
                                                     DesignatedInitExpr die,
                                                     int desigIdx,
                                                     Decl.FieldDecl field,
                                                     OutRef<Decl.RecordDecl> structDecl,
                                                     OutRef<Integer> fieldItr,
                                                     OutRef<Integer> fieldIndex) {
    ArrayList<Decl.FieldDecl> path = new ArrayList<>();
    sema.buildAnonymousStructUnionMemberPath(field, path);

    ArrayList<Designator> replacements = new ArrayList<>();
    // Build the replacement designators.
    for (int i = path.size() - 1; i >= 0; i--) {
      Designator d;
      if (i == 0) {
        d = new Designator(null,
            die.getDesignator(desigIdx).getDotLoc(),
            die.getDesignator(desigIdx).getFieldLoc());
      } else {
        d = new Designator(null,
            new SourceLocation(), new SourceLocation());
      }
      d.setField(path.get(i));
      replacements.add(d);
    }

    die.expandDesignator(desigIdx, replacements);

    Decl.RecordDecl record = (Decl.RecordDecl) path.get(path.size() - 1).getDeclContext();
    structDecl.set(record);
    fieldItr.set(0);
    fieldIndex.set(0);
    for (int e = record.getNumFields(); fieldItr.get() != e; ) {
      int i = fieldItr.get();
      if (record.getDeclAt(i).isUnamaedBitField())
        continue;

      if (record.getDeclAt(i).equals(path.get(path.size() - 1))) {
        return;
      }
      fieldItr.set(fieldItr.get() + 1);
    }
  }

  private boolean checkDesignatedInitializer(
      InitListExpr initList,
      DesignatedInitExpr die,
      int desigIdx,
      OutRef<QualType> currentObjectType,
      OutRef<Decl.RecordDecl> structDecl,
      OutRef<Integer> nextField,
      OutRef<APSInt> nextElementIndex,
      OutRef<Integer> index,
      InitListExpr structuredList,
      OutRef<Integer> structuredIndex,
      boolean finishSubobjectInit,
      boolean topLevelObject) {
    if (desigIdx == die.getSize()) {
      boolean prevHadError = hadError;

      int oldIdx = index.get();
      initList.setInitAt(oldIdx, die.getInit());
      checkSubElementType(initList, currentObjectType.get(), index,
          structuredList, structuredIndex);

      if (!initList.getInitAt(oldIdx).equals(die.getInit())) {
        die.setInit(initList.getInitAt(oldIdx));
      }
      initList.setInitAt(oldIdx, die);

      return hadError && !prevHadError;
    }

    boolean isFirstDesignator = desigIdx == 0;
    Util.assertion(isFirstDesignator || structuredList != null, "Need a non-designated initializer list to start from");


    Designator d = die.getDesignator(desigIdx);

    structuredList = isFirstDesignator ? syntacticToSemantic.get(initList)
        : getStructuredSubobjectInit(initList, index.get(), currentObjectType.get(),
        structuredList, structuredIndex.get(), new SourceRange(d.getStartLocation(),
            die.getSourceRange().getEnd()));

    Util.assertion(structuredList != null, "Expected a structured initializer list");

    if (d.isFieldDesignator()) {
      // C99 6.7.8p7:
      //
      //   If a designator has the form
      //
      //      . identifier
      //
      //   then the current object (defined below) shall have
      //   structure or union type and the identifier shall be the
      //   name of a member of that type.
      RecordType rt = currentObjectType.get().getAsRecordType();
      if (rt == null) {
        SourceLocation loc = d.getDotLoc();
        if (!loc.isValid())
          loc = d.getFieldLoc();

        sema.diag(loc, err_field_designator_non_aggr)
            .addTaggedVal(false)
            .addTaggedVal(currentObjectType.get())
            .emit();
        index.set(index.get() + 1);
        return true;
      }

      // Note: we perform a linear search of the fields here, despite
      // the fact that we have a faster lookup method, because we always
      // need to compute the field's index.
      Decl.FieldDecl knownField = d.getField();
      IdentifierInfo fieldName = d.getFieldName();

      Decl.RecordDecl rd = rt.getDecl();
      int fieldIndex = 0, field = 0;
      int fieldEnd = rt.getDecl().getNumFields();
      for (; field != fieldEnd; field++) {
        Decl.FieldDecl fd = rd.getDeclAt(field);
        if (fd.isUnamaedBitField())
          continue;

        if (fd.equals(knownField) || fd.getIdentifier().equals(fieldName)) {
          break;
        }

        fieldIndex++;
      }

      if (field == fieldEnd) {
        // There was no normal field in the struct with the designated
        // name
        sema.diag(d.getFieldLoc(), err_field_designator_unknown)
            .addTaggedVal(fieldName)
            .addTaggedVal(currentObjectType.get())
            .emit();
        index.set(index.get() + 1);
        return true;
      } else if (knownField == null && ((Decl.RecordDecl) rt.getDecl().getDeclAt
          (field).getDeclContext()).isAnonymousStructOrUnion()) {
        OutRef<Decl.RecordDecl> x3 = new OutRef<>(rd);
        OutRef<Integer> x = new OutRef<>(field);
        OutRef<Integer> x2 = new OutRef<>(fieldIndex);

        expandAnonymousFieldDesignator(sema, die, desigIdx,
            rt.getDecl().getDeclAt(fieldIndex),
            x3, x, x2);
        d = die.getDesignator(desigIdx);
        field = x.get();
        fieldIndex = x2.get();
        rd = x3.get();
      }

      if (rt.getDecl().isUnion()) {
        fieldIndex = 0;
        structuredList.setInitializedFieldInUnion(rd.getDeclAt(field));
      }

      d.setField(rd.getDeclAt(field));

      if (fieldIndex >= structuredList.getNumInits()) {
        structuredList.reserveInits(fieldIndex + 1);
      }

      if (rd.getDeclAt(field).getType().isIncompleteArrayType()) {
        boolean invalid = false;
        if ((desigIdx + 1) != die.getSize()) {
          Designator nextD =
              die.getDesignator(desigIdx + 1);
          sema.diag(nextD.getStartLocation(),
              err_designator_into_flexible_array_member)
              .addSourceRange(new SourceRange(nextD.getStartLocation(),
                  die.getSourceRange().getEnd()))
              .emit();

          Decl.FieldDecl fd = rd.getDeclAt(field);
          sema.diag(fd.getLocation(), note_flexible_array_member)
              .addTaggedVal(fd)
              .emit();
          invalid = true;
        }

        if (!hadError && (die.getInit() instanceof InitListExpr)) {
          sema.diag(die.getInit().getSourceRange().getBegin(),
              err_flexible_array_init_needs_braces)
              .addSourceRange(die.getInit().getSourceRange())
              .emit();

          Decl.FieldDecl fd = rd.getDeclAt(field);
          sema.diag(fd.getLocation(), note_flexible_array_member)
              .addTaggedVal(fd)
              .emit();
          invalid = true;
        }

        // Handle GNU flexible array initializers.
        if (!invalid && !topLevelObject && ((InitListExpr) die.getInit()).getNumInits() > 0) {
          sema.diag(die.getSourceRange().getBegin(),
              err_flexible_array_init_nonempty)
              .addSourceRange(die.getSourceRange());
          Decl.FieldDecl fd = rd.getDeclAt(field);
          sema.diag(fd.getLocation(), note_flexible_array_member)
              .addTaggedVal(fd)
              .emit();
          invalid = true;
        }

        if (invalid) {
          index.set(index.get() + 1);
          return true;
        }

        boolean prevHadError = hadError;
        int oldIndx = index.get();
        Decl.FieldDecl fd = rd.getDeclAt(field);

        initList.setInitAt(index.get(), die.getInit());
        checkSubElementType(initList, fd.getType(), index,
            structuredList, new OutRef<>(fieldIndex));
        initList.setInitAt(oldIndx, die);
        if (hadError && !prevHadError) {
          field++;
          fieldIndex++;
          if (nextField != null) {
            structDecl.set(rd);
            nextField.set(field);
          }

          structuredIndex.set(fieldIndex);
          return true;
        }
      } else {
        Decl.FieldDecl fd = rd.getDeclAt(field);
        // Recurse to check later designated subobjects.
        QualType fieldType = fd.getType();
        if (checkDesignatedInitializer(initList,
            die, desigIdx + 1,
            new OutRef<>(fieldType), null,
            null, null, index,
            structuredList, new OutRef<>(fieldIndex),
            true, false)) {
          return true;
        }
      }

      ++field;
      ++fieldIndex;

      if (isFirstDesignator) {
        if (nextField != null) {
          nextField.set(field);
          structDecl.set(rd);
        }

        structuredIndex.set(fieldIndex);
        return false;
      }

      if (!finishSubobjectInit)
        return false;

      if (rt.getDecl().isUnion())
        return hadError;

      boolean prevHadError = hadError;
      checkStructUnionTypes(initList, currentObjectType.get(),
          new OutRef<>(rd), field, false, index,
          structuredList, structuredIndex, false);

      return hadError && !prevHadError;
    }

    // C99 6.7.8p6:
    //
    //   If a designator has the form
    //
    //      [ constant-expression ]
    //
    //   then the current object (defined below) shall have array
    //   type and the expression shall be an integer constant
    //   expression. If the array is of unknown size, any
    //   nonnegative value is valid.
    //
    // Additionally, cope with the GNU extension that permits
    // designators of the form
    //
    //      [ constant-expression ... constant-expression ]

    ArrayType at = sema.context.getAsArrayType(currentObjectType.get());
    if (at == null) {
      sema.diag(d.getLBracketLoc(), err_array_designator_non_array)
          .addTaggedVal(currentObjectType.get())
          .emit();
      index.set(index.get() + 1);
      return true;
    }

    Expr indexExpr = null;
    APSInt designatedStartIndex, designatedEndIndex = new APSInt();
    if (d.isArrayDesignator()) {
      indexExpr = die.getArrayIndex(d);
      designatedStartIndex = indexExpr.evaluateAsInt(sema.context);
      designatedEndIndex.assign(designatedStartIndex);
    } else {
      Util.assertion(d.isArrayRangeDesignator(), "Need array or array-range designator");

      designatedStartIndex = die.getArrayRangeStart(d).evaluateAsInt(sema.context);
      designatedEndIndex = die.getArrayRangeEnd(d).evaluateAsInt(sema.context);
      indexExpr = die.getArrayRangeEnd(d);

      if (designatedStartIndex.getZExtValue() != designatedEndIndex.getZExtValue()) {
        fullyStructuredList.sawArrayRangeDesignator();
      }
    }

    if (at instanceof ArrayType.ConstantArrayType) {
      ArrayType.ConstantArrayType cat = (ArrayType.ConstantArrayType) at;
      APSInt maxElements = new APSInt(cat.getSize(), false);
      designatedStartIndex.extOrTrunc(maxElements.getBitWidth());
      designatedEndIndex.extOrTrunc(maxElements.getBitWidth());
      designatedStartIndex.setIsUnsigned(maxElements.isUnsigned());
      designatedEndIndex.setIsUnsigned(maxElements.isUnsigned());

      if (designatedEndIndex.ge(maxElements)) {
        sema.diag(indexExpr.getSourceRange().getBegin(),
            err_array_designator_too_large)
            .addTaggedVal(designatedEndIndex.toString(10))
            .addTaggedVal(maxElements.toString(10))
            .addSourceRange(indexExpr.getSourceRange())
            .emit();
        index.set(index.get() + 1);
        return true;
      }
    } else {
      if (designatedStartIndex.getBitWidth() > designatedEndIndex.getBitWidth()) {
        designatedEndIndex.extend(designatedStartIndex.getBitWidth());
      } else if (designatedEndIndex.getBitWidth() > designatedStartIndex.getBitWidth()) {
        designatedStartIndex.extend(designatedEndIndex.getBitWidth());
      }
      designatedEndIndex.setIsUnsigned(true);
      designatedStartIndex.setIsUnsigned(true);
    }

    if (designatedEndIndex.getZExtValue() >= structuredList.getNumInits()) {
      structuredList.reserveInits(designatedEndIndex.getZExtValue() + 1);
    }


    // Repeatedly perform subobject initializations in the range
    // [DesignatedStartIndex, DesignatedEndIndex].

    // Move to the next designator
    int elementIndex = (int) designatedStartIndex.getZExtValue();
    int oldIndex = index.get();
    while (designatedStartIndex.le(designatedEndIndex)) {
      QualType elementType = at.getElementType();
      index.set(oldIndex);

      if (checkDesignatedInitializer(initList, die, desigIdx + 1,
          new OutRef<>(elementType),
          null, null, null, index, structuredList,
          new OutRef<>(elementIndex),
          (designatedStartIndex.eq(designatedEndIndex)),
          false)) {
        return true;
      }

      designatedStartIndex.increment();
      elementIndex = (int) designatedStartIndex.getZExtValue();
    }

    if (isFirstDesignator) {
      if (nextElementIndex != null)
        nextElementIndex.set(designatedStartIndex);
      structuredIndex.set((int) elementIndex);
      return false;
    }

    if (!finishSubobjectInit)
      return false;

    boolean prevHadError = hadError;
    checkArrayType(initList, currentObjectType, designatedStartIndex, false,
        index, structuredList, new OutRef<>(elementIndex));
    return hadError && !prevHadError;
  }

  private void checkArrayType(
      InitListExpr initList,
      OutRef<QualType> declType,
      APSInt elementIndex,
      boolean subObjectIsDesignatedContext,
      OutRef<Integer> index,
      InitListExpr structuredList,
      OutRef<Integer> structuredIndex) {
    // Check for the special-case of initializing an array with a string.
    if (index.get() < initList.getNumInits()) {
      Expr str = isStringInit(initList.getInitAt(index.get()),
          declType.get(), sema.context);
      if (str != null) {
        checkStringInit(str, declType, sema);

        updateStructuredListElement(structuredList, structuredIndex, str);
        structuredList.resizeInits(structuredIndex.get());
        index.set(index.get() + 1);
        return;
      }
    }

    ArrayType.VariableArrayType vat = sema.context.getAsVariableArrayType(declType.get());
    if (vat != null) {
      // Check for VLAs; in standard C it would be possible to check this
      // earlier (gcc accepts them in all sorts of strange places).
      sema.diag(vat.getSizeExpr().getLocStart(),
          err_variable_object_no_init)
          .addSourceRange(vat.getSizeExpr().getSourceRange())
          .emit();
      hadError = true;
      index.set(index.get() + 1);
      structuredIndex.set(structuredIndex.get() + 1);
      return;
    }

    APSInt maxElements = new APSInt(elementIndex.getBitWidth(), elementIndex.isUnsigned());
    boolean maxElementsKnown = false;
    ArrayType.ConstantArrayType cat = sema.context.getAsConstantArrayType(declType.get());
    if (cat != null) {
      maxElements.assign(cat.getSize());
      elementIndex.extOrTrunc(maxElements.getBitWidth());
      elementIndex.setIsUnsigned(maxElements.isUnsigned());
      maxElementsKnown = true;
    }

    QualType elementType = sema.context.getAsArrayType(declType.get()).getElementType();

    while (index.get() < initList.getNumInits()) {
      Expr init = initList.getInitAt(index.get());

      if (init instanceof DesignatedInitExpr) {
        DesignatedInitExpr die = (DesignatedInitExpr) init;

        if (!subObjectIsDesignatedContext)
          return;

        // Handle this designated initializer. elementIndex will be
        // updated to be the next array element we'll initialize.
        OutRef<APSInt> x = new OutRef<>(elementIndex);
        boolean res = checkDesignatedInitializer(initList, die, 0, declType,
            null, null, x, index, structuredList, structuredIndex,
            true, false);
        elementIndex = x.get();
        if (res) {
          hadError = true;
          continue;
        }

        if (elementIndex.getBitWidth() > maxElements.getBitWidth())
          maxElements.extend(elementIndex.getBitWidth());
        else if (elementIndex.getBitWidth() < maxElements.getBitWidth())
          elementIndex.extend(maxElements.getBitWidth());

        elementIndex.setIsUnsigned(maxElements.isUnsigned());

        // If the array is of incomplete type, keep track of the number of
        // elements in the initializer.
        if (!maxElementsKnown && elementIndex.gt(maxElements))
          maxElements.assign(elementIndex);

        continue;
      }

      if (maxElementsKnown && elementIndex.eq(maxElements))
        break;


      // Check sub element type.
      checkSubElementType(initList, elementType, index,
          structuredList, structuredIndex);
      elementIndex.increment();

      if (!maxElementsKnown && elementIndex.gt(maxElements)) {
        maxElements.assign(elementIndex);
      }
    }
    if (!hadError && declType.get().isIncompleteType()) {
      APSInt zero = new APSInt(maxElements.getBitWidth(), maxElements.isUnsigned());
      if (maxElements.eq(zero)) {
        sema.diag(initList.getLocStart(), ext_typecheck_zero_array_size).emit();
      }
      declType.set(sema.context.getConstantArrayType(elementType,
          maxElements, ArrayType.ArraySizeModifier.Normal, 0));
    }
  }

  private void checkListElementType(
      InitListExpr initList,
      OutRef<QualType> type,
      boolean subObjectIsDesignatedContext,
      OutRef<Integer> index,
      InitListExpr structuredList,
      OutRef<Integer> structuredIndex,
      boolean topLevelObject) {
    if (type.get().isScalarType()) {
      checkScalarType(initList, type.get(), index, structuredList, structuredIndex);
    } else if (type.get().isRecordType()) {
      Decl.RecordDecl rd = type.get().getAsRecordType().getDecl();
      checkStructUnionTypes(initList, type.get(), new OutRef<>(rd),
          0, subObjectIsDesignatedContext,
          index, structuredList, structuredIndex, topLevelObject);
    } else if (type.get().isArrayType()) {
      APSInt zero = new APSInt((int) sema.context.getTypeSize(sema.context.getSizeType()),
          false);
      checkArrayType(initList, type, zero, subObjectIsDesignatedContext,
          index, structuredList, structuredIndex);
    } else if (type.get().isVoidType() || type.get().isFunctionType()) {
      // this type is invalid, issue a diagnostic.
      index.set(index.get() + 1);
      sema.diag(initList.getLocStart(), err_illegal_initializer_type)
          .addTaggedVal(type.get()).emit();
      hadError = true;
    } else {
      // In C, all types are either scalars or aggregates, but
      // additional handling is needed here for C++ (and possibly others?).
      Util.assertion(false, "Unsupported initializer type!");
    }
  }

  private int numArrayElements(QualType declType) {
    int maxElements = 0x7FFFFFFF;
    ArrayType.ConstantArrayType cat = sema.context.getAsConstantArrayType(declType);
    if (cat != null) {
      maxElements = (int) cat.getSize().getZExtValue();
    }
    return maxElements;
  }

  private int numStructUnionElements(QualType declType) {
    Decl.RecordDecl decl = declType.getAsRecordType().getDecl();
    int initializableMembers = 0;
    for (int i = 0, e = decl.getNumFields(); i != e; i++) {
      Decl.FieldDecl field = decl.getDeclAt(i);
      if (field.getIdentifier() != null && !field.isBitField())
        initializableMembers++;
    }

    if (decl.isUnion())
      initializableMembers = Math.min(initializableMembers, 1);

    return initializableMembers - (decl.hasFlexibleArrayNumber() ? 1 : 0);
  }

  private void checkImplicitInitList(
      InitListExpr parentInitList,
      QualType type,
      OutRef<Integer> index,
      InitListExpr structuredList,
      OutRef<Integer> structuredIndex,
      boolean topLevelObject) {
    int maxElements = 0;
    if (type.isArrayType()) {
      maxElements = numArrayElements(type);
    } else if (type.isStructureType() || type.isUnionType()) {
      maxElements = numStructUnionElements(type);
    } else
      Util.assertion(false, "checkImplicitInitList(): illegal type");

    if (maxElements == 0) {
      sema.diag(parentInitList.getInitAt(index.get()).getLocStart(),
          err_implicit_empty_initializer).emit();
      index.set(index.get() + 1);
      hadError = true;
      return;
    }

    InitListExpr structuredSubobjectInitList = getStructuredSubobjectInit(parentInitList,
        index.get(), type, structuredList, structuredIndex.get(),
        new SourceRange(parentInitList.getInitAt(index.get()).getSourceRange().getBegin(),
            parentInitList.getSourceRange().getEnd()));

    OutRef<Integer> structuredSubobjectInitIndex = new OutRef<>(0);
    int startIndex = index.get();

    checkListElementType(parentInitList,
        new OutRef<>(type), false, index,
        structuredSubobjectInitList,
        structuredSubobjectInitIndex, topLevelObject);
    int endIndex = (index.get() == startIndex) ? startIndex : index.get() - 1;
    structuredSubobjectInitList.setType(type);

    if (endIndex < parentInitList.getNumInits()) {
      SourceLocation endLoc = parentInitList.getInitAt(endIndex).getSourceRange().getEnd();
      structuredSubobjectInitList.setRBraceLoc(endLoc);
    }
  }

  private void checkExplicitInitList(
      InitListExpr initList,
      OutRef<Integer> index,
      OutRef<QualType> type,
      InitListExpr structuredList,
      OutRef<Integer> structuedIndex,
      boolean topLevelObject) {
    Util.assertion(initList.isExplicit(), "Illegal implicit InitListExpr!");
    syntacticToSemantic.put(initList, structuredList);
    structuredList.setSyntacticForm(initList);

    checkListElementType(initList, type, true, index, structuredList,
        structuedIndex, topLevelObject);

    initList.setType(type.get());
    structuredList.setType(type.get());

    if (hadError)
      return;
    if (index.get() < initList.getNumInits()) {
      // We have overflow initializers.
      if (structuedIndex.get() == 1 &&
          isStringInit(structuredList.getInitAt(0), type.get(), sema.context) != null) {
        int dk = warn_excess_initializers_in_char_array_initializer;
        sema.diag(initList.getInitAt(index.get()).getLocStart(), dk)
            .addSourceRange(initList.getInitAt(index.get()).getSourceRange())
            .emit();
      } else if (!type.get().isIncompleteType()) {
        // Don't complain for incomplete types, since we'll get an error
        // elsewhere
        QualType currentObjectType = structuredList.getType();
        int initKind = currentObjectType.isArrayType() ? 0 :
            currentObjectType.isScalarType() ? 1 :
                currentObjectType.isUnionType() ? 2 : 3;

        int dk = warn_excess_initializers;
        sema.diag(initList.getInitAt(index.get()).getLocStart(), dk)
            .addTaggedVal(initKind)
            .addSourceRange(initList.getInitAt(index.get()).getSourceRange())
            .emit();
      }
    }

    if (type.get().isScalarType() && !topLevelObject) {
      sema.diag(initList.getLocStart(), warn_braces_around_scalar_init)
          .addSourceRange(initList.getSourceRange())
          .addFixItHint(FixItHint.createRemoval(new SourceRange(initList.getLocStart())))
          .addFixItHint(FixItHint.createRemoval(new SourceRange(initList.getLocEnd())))
          .emit();
    }
  }

  private InitListExpr getStructuredSubobjectInit(
      InitListExpr listExpr,
      int index,
      QualType currentObjectType,
      InitListExpr structuredList,
      int structuredIndex,
      SourceRange range) {
    Expr existingInit = null;
    if (structuredList == null) {
      if (syntacticToSemantic.containsKey(listExpr)) {
        existingInit = syntacticToSemantic.get(listExpr);
      } else {
        existingInit = null;
      }
    } else if (structuredIndex < structuredList.getNumInits())
      existingInit = structuredList.getInitAt(structuredIndex);

    InitListExpr result = existingInit instanceof InitListExpr ?
        (InitListExpr) existingInit : null;
    if (result != null)
      return result;

    if (existingInit != null) {
      // We are creating an initializer list that initializes the
      // subobjects of the current object, but there was already an
      // initialization that completely initialized the current
      // subobject, e.g., by a compound literal:
      //
      // struct X { int a, b; };
      // struct X xs[] = { [0] = (struct X) { 1, 2 }, [0].b = 3 };
      //
      // Here, xs[0].a == 0 and xs[0].b == 3, since the second,
      // designated initializer re-initializes the whole
      // subobject [0], overwriting previous initializers.
      sema.diag(range.getBegin(), DiagnosticSemaTag.warn_subobject_initializer_overrides)
          .addSourceRange(range).emit();
      sema.diag(existingInit.getSourceRange().getBegin(), DiagnosticSemaTag.note_previous_initializer)
          .addTaggedVal(0)    // has side effect.
          .addSourceRange(existingInit.getSourceRange())
          .emit();
    }

    result = new InitListExpr(range.getBegin(), range.getEnd(), new ArrayList<>());
    result.setType(currentObjectType);

    // Pre-allocate storage for the structured initializer list.
    long numElements = 0;
    long numInits = 0;
    if (structuredList == null) {
      numInits = listExpr.getNumInits();
    } else if (index < listExpr.getNumInits()) {
      InitListExpr subList = listExpr.getInitAt(index) instanceof InitListExpr
          ? (InitListExpr) listExpr.getInitAt(index) : null;
      if (subList != null) {
        numInits = subList.getNumInits();
      }
    }

    ArrayType at = sema.context.getAsArrayType(currentObjectType);
    RecordType recType = null;
    if (at != null) {
      if (at instanceof ArrayType.ConstantArrayType) {
        ArrayType.ConstantArrayType catype = (ArrayType.ConstantArrayType) at;
        numElements = catype.getSize().getZExtValue();

        // Simple heuristic so that we don't allocate a very large
        // initializer with many empty entries at the end.
        if (numInits != 0 && numElements > numInits)
          numElements = 0;
      }
    } else if ((recType = currentObjectType.getAsRecordType()) != null) {
      Decl.RecordDecl rd = recType.getDecl();
      if (rd.isUnion())
        numElements = 1;
      else
        numElements = rd.getNumFields();
    }

    if (numElements < numInits)
      numElements = listExpr.getNumInits();

    result.reserveInits(numElements);

    // Link this initializer list into the structured initializer lists.
    if (structuredList != null) {
      structuredList.updateInit(structuredIndex, result);
    } else {
      result.setSyntacticForm(listExpr);
      syntacticToSemantic.put(listExpr, result);
    }

    return result;
  }

  public boolean hadError() {
    return hadError;
  }

  public InitListExpr getFullyStructuredList() {
    return fullyStructuredList;
  }
}
