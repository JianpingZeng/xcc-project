package jlang.sema;

import jlang.ast.ASTConsumer;
import jlang.ast.CastKind;
import jlang.ast.Tree;
import jlang.ast.Tree.*;
import jlang.basic.*;
import jlang.clex.*;
import jlang.cparser.*;
import jlang.cparser.DeclSpec.DeclaratorChunk;
import jlang.cparser.DeclSpec.DeclaratorChunk.ArrayTypeInfo;
import jlang.cparser.DeclSpec.DeclaratorChunk.FunctionTypeInfo;
import jlang.cparser.DeclSpec.DeclaratorChunk.PointerTypeInfo;
import jlang.cparser.DeclSpec.SCS;
import jlang.cparser.DeclSpec.TST;
import jlang.diag.*;
import jlang.sema.Decl.*;
import jlang.type.*;
import jlang.type.ArrayType.ArraySizeModifier;
import jlang.type.ArrayType.VariableArrayType;
import jlang.type.TagKind;
import tools.Context;
import tools.OutParamWrapper;
import tools.Pair;
import tools.Util;
import java.util.*;
import static jlang.ast.CastKind.*;
import static jlang.ast.Tree.ExprObjectKind.OK_BitField;
import static jlang.ast.Tree.ExprObjectKind.OK_Ordinary;
import static jlang.ast.Tree.ExprValueKind.EVK_LValue;
import static jlang.ast.Tree.ExprValueKind.EVK_RValue;
import static jlang.basic.Linkage.NoLinkage;
import static jlang.clex.TokenKind.arrow;
import static jlang.clex.TokenKind.char_constant;
import static jlang.clex.TokenKind.numeric_constant;
import static jlang.cparser.DeclSpec.TQ.*;
import static jlang.cparser.Declarator.TheContext.FunctionProtoTypeContext;
import static jlang.cparser.Parser.exprError;
import static jlang.cparser.Parser.stmtError;
import static jlang.sema.BinaryOperatorKind.BO_Div;
import static jlang.sema.BinaryOperatorKind.BO_DivAssign;
import static jlang.sema.LookupResult.LookupResultKind.Found;
import static jlang.sema.Scope.ScopeFlags.CompilationUnitScope;
import static jlang.sema.Scope.ScopeFlags.DeclScope;
import static jlang.sema.Sema.LookupNameKind.*;
import static jlang.sema.UnaryOperatorKind.*;

/**
 * This file defines the {@linkplain Sema} class, which performs semantic
 * analysis and builds ASTs for C language.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class Sema implements DiagnosticParseTag,
        DiagnosticCommonKindsTag,
        DiagnosticSemaTag
{
	/**
     * Used for emitting the right warning by DefaultVariadicArgumentPromotion.
     */
    enum VariadicCallType
    {
        VariadicFunction,
        VariadicDoesNotApply
    }

    public enum TagUseKind
    {
        TUK_reference,      // Reference to a tc: 'struct foo *X;'
        TUK_declaration,    // Forward declaration of a tc: 'struct foo;'
        TUK_definition     // Definition of a tc: 'struct foo {int X;} Y;'
    }

    /**
     * Describes the kind of getIdentifier lookup up to perform.
     * <br>
     * When an identifier is encountered in a C program, a lookup is performed
     * to locate the declaration that introduced that identifier and that is
     * currently in scope. C allows more than one declaration for the same identifier
     * to be in scope simultaneously if these identifiers belong to different
     * categories, called getIdentifier spaces:
     * <ol>
     *   <li>
     *     Label getIdentifier space: all identifiers declared as labels.
     *   </li>
     *   <li>
     *     Tag names: all identifiers declared as names of structs, unions and
     *     enumerated types. Note that all three kinds of tags share one getIdentifier space.
     *   </li>
     *   <li>
     *     Member names: all identifiers declared as members of any one struct or
     *     union. Every struct and union introduces its own getIdentifier space of this kind.
     *   </li>
     *   <li>
     *     All other identifiers, called ordinary identifiers to distinguish from
     *     (1-3) (function names, object names, typedef names, enumeration constants).
     *   </li>
     * </ol>
     * <br>
     * At the point of lookup, the getIdentifier space of an identifier is determined by
     * the manner in which it is used:
     * <ol>
     *  <li>
     *   identifier appearing as the operand of a goto statement is looked up in
     *   the label getIdentifier space.
     *  </li>
     *  <li>
     *    identifier that follows the keyword struct, union, or enum is looked up
     *    in the tc getIdentifier space.
     *  </li>
     *  <li>
     *   identifier that follows the member access or member access through pointer
     *   operator is looked up in the getIdentifier space of members of the type determined
     *   by the left-hand operand of the member access operator.
     *  </li>
     *  <li>
     *   all other identifiers are looked up in the getIdentifier space of ordinary identifiers.
     *  </li>
     * </ol>
     *
     * The task of resolving the various kinds of names into zero or more declarations
     * within a particular scope. The major entry point are
     * {@linkplain #lookupName(LookupResult, Scope)}, which peforms unqualified
     * getIdentifier lookup.
     * <br>
     * All getIdentifier lookup is performed based on specific criteria, which specify
     * what names will visible to getIdentifier lookup and how far getIdentifier lookup should work.
     * These criteria are important both for capturing languages and for peformance,
     * since getIdentifier lookup is often a bottleneck in the compilation of C. Name
     * lookup cirteria is specified via the {@linkplain LookupNameKind} enumeration.
     * <br>
     * The result of getIdentifier lookup can vary based on the kind of getIdentifier lookup performed
     * , the current languange, and the translation unit. In C, for example, getIdentifier
     * lookup will either return nothing(no entity found) or a single declaration.
     *
     * All of the possible results of getIdentifier lookup are captured by the {@linkplain
     * LookupResult} class, which provides the ability to distinguish among them.
     */
    public enum LookupNameKind
    {
        LookupOrdinaryName, LookupTagName, LookupLabelName, LookupMemberName
    }

    private Scope curScope;
    private DeclContext curContext;
    private LangOptions langOpts;
    private Preprocessor pp;
    private Stack<FunctionScopeInfo> functionScopes;
    private static Context SEMA_CONTEXT = new Context();
    private ASTConsumer consumer;
    private ASTContext context;

    public Sema(Preprocessor pp, ASTContext ctx, ASTConsumer consumer)
    {
        langOpts = pp.getLangOptions();
        this.pp = pp;
        context = ctx;
        this.consumer = consumer;
        initialize();
    }

    private void initialize()
    {
        curScope = new Scope(null, CompilationUnitScope.value);
        functionScopes = new Stack<>();
    }

    public ASTConsumer getASTConsumer()
    {
        return consumer;
    }

    PartialDiagnostic pdiag(int diagID)
    {
        return new PartialDiagnostic(diagID);
    }

    private SemaDiagnosticBuilder diag(SourceLocation loc, int diagID)
    {
        Diagnostic.DiagnosticBuilder db = pp.diag(loc, diagID);
        return new SemaDiagnosticBuilder(db, this, diagID);
    }

    private SemaDiagnosticBuilder diag(SourceLocation loc, PartialDiagnostic pdiag)
    {
        int diagID = pdiag.getDiagID();
        SemaDiagnosticBuilder builder = new SemaDiagnosticBuilder(
                diag(loc, diagID), this, diagID);
        pdiag.emit(builder);
        return builder;
    }

    /**
     * Performs name lookup for a name that was parsed in the
     * source code
     * @param s The scope from which unqualified name lookup will
     * @param name The name of the entity that name lookup will
     * search for.
     * @param lookupKind
     * @return The result of unqualified name lookup.
     */
    public LookupResult lookupParsedName(Scope s, String name,
            LookupNameKind lookupKind)
    {
        return lookupParsedName(s, name, lookupKind, SourceLocation.NOPOS);
    }

	/**
     * Performs name lookup for a name that was parsed in the
     * source code
     * @param s The scope from which unqualified name lookup will
     * @param name The name of the entity that name lookup will
     * search for.
     * @param lookupKind
     * @param loc If provided, the source location where we're performing
     * name lookup. At present, this is only used to produce diagnostics when
     * C library functions (like "malloc") are implicitly declared.
     * @return The result of unqualified name lookup.
     */
    public LookupResult lookupParsedName(Scope s, String name,
            LookupNameKind lookupKind, SourceLocation loc)
    {
        LookupResult result = new LookupResult(this, name, loc, lookupKind);
        lookupName(result, s);
        return result;
    }

    /**
     * If the identifier refers to the type, then this method just returns the
     * declaration of this type within this scope.
     * <p>
     * This routine performs ordinary name lookup of the identifier II
     * within the given scope, to determine whether the name refers to
     * a type. If so, returns an a QualType corresponding to that
     * type. Otherwise, returns NULL.
     *
     * If name lookup results in an ambiguity, this routine will complain
     * and then return NULL.
     * </p>
     *
     * @param identifierInfo
     * @param nameLoc
     * @param curScope
     * @return
     */
    public QualType getTypeByName(IdentifierInfo identifierInfo,
            SourceLocation nameLoc, Scope curScope)
    {
        LookupResult res = lookupParsedName(curScope, identifierInfo.getName(),
                LookupOrdinaryName);

        NamedDecl ndecl = null;
        switch (res.getResultKind())
        {
            case NotFound:
            case Ambiguous:
                return null;
            case Found:
                ndecl = res.getFoundDecl();
                break;
        }

        if (ndecl != null)
        {
            QualType qt = new QualType();
            if (ndecl instanceof TypeDecl)
            {
                TypeDecl td = (TypeDecl)ndecl;
                if (qt.isNull())
                    qt = new QualType(td.getTypeForDecl());
            }
            else
                return null;
            return qt;
        }
        return null;
    }

    public Scope getCurScope()
    {
        return curScope;
    }

    public void setCurScope(Scope cur)
    {
        curScope = cur;
    }

	/**
     * Determines if a tc with a given kind is acceptable as a redeclaration of
     * the given tc declaration.
     * @param previous
     * @param newTag
     * @param newTagLoc
     * @param name
     * @return  Return true if the new tc kind is acceptable, false otherwise.
     */
    private boolean isacceptableTagRedeclaration(
            TagDecl previous,
            TagKind newTag,
            SourceLocation newTagLoc,
            IdentifierInfo name)
    {
        // TODO: 17-5-10
        TagKind oldTag = previous.getTagKind();
        return oldTag == newTag;
    }

    /**
     * This method was invoked when it sees 'struct X {...}' or 'struct X;'.
     * In the former class, the getIdentifier must be non null. In the later case, getIdentifier
     * will be null.
     *
     * @param curScope
     * @param tagType  Indicates what kind of tc this is.
     * @param tuk      Indicates whether this is a reference/declaration/definition
     *                 of a tc.
     * @param kwLoc
     * @param name
     * @param nameLoc
     * @return
     */
    public ActionResult<Decl> actOnTag(
            Scope curScope,
            TST tagType,
            TagUseKind tuk,
            SourceLocation kwLoc,
            IdentifierInfo name,
            SourceLocation nameLoc)
    {
        // if this is not a definition, it must have a getIdentifier
        assert (name != null || tuk == TagUseKind.TUK_definition)
                : "Nameless record must be a definition";

        TagKind kind = TagKind.getTagTypeKindForTypeSpec(tagType);

        QualType enumUnderlying = null;
        if (kind == TagKind.TTK_enum)
        {
            // C99, Each enumerator that appears in the body of an enumeration
            // specifier becomes an integer constant with type int in the
            // enclosing scope and can be used whenever integer constants are required
            enumUnderlying = context.IntTy;
        }

        LookupResult result = new LookupResult(this, name.getName(), nameLoc,
                LookupTagName);
        DeclContext searchDC = curContext;
        DeclContext dc = curContext;
        NamedDecl prevDecl = null;
        boolean invalid = false;

        if (name != null)
        {
            // if this is a named struct, check to see if there was a previous
            // forward declaration or definition.
            lookupName(result, curScope);

            if (result.isAmbiguous())
            {
                name = null;
                prevDecl = null;
                invalid = true;
            }
            else
            {
                prevDecl = result.getFoundDecl();
            }

            if (tuk != TagUseKind.TUK_reference)
            {
                // This makes sure that we ignore the contexts associated
                // with C structs, unions, and enums when looking for a matching
                // tc declaration or definition.
                while (searchDC instanceof RecordDecl
                        || searchDC instanceof EnumDecl)
                    searchDC = searchDC.getParent();
            }
        }
        // If there is a previous tc definition or forward declaration was found,
        // Handle it.
        if (prevDecl != null)
        {
            if (prevDecl instanceof TagDecl)
            {
                TagDecl prevTagDecl = (TagDecl)prevDecl;
                // If this is a use of a previous tc, or if the tc is already declared
                // in the same scope (so that the definition/declaration completes or
                // rementions the tc), reuse the decl.
                if (tuk == TagUseKind.TUK_reference || isDeclInScope(prevDecl, searchDC, curScope))
                {
                    // Make sure that this wasn't declared as an enum and now used as a
                    // struct or something similar.
                    if (!isacceptableTagRedeclaration(prevTagDecl, kind, kwLoc, name))
                    {
                        boolean safeToContinue = (prevTagDecl.getTagKind() != TagKind.TTK_enum
                        && kind != TagKind.TTK_enum);
                        if (safeToContinue)
                        {
                            diag(kwLoc, err_use_with_wrong_tag)
                                    .addTaggedVal(name)
                                    .addFixItHint(FixItHint.createReplacement
                                    (new SourceRange(kwLoc),
                                    prevTagDecl.getKindName()))
                                    .emit();
                        }
                        else
                        {
                            diag(kwLoc, err_use_with_wrong_tag).
                                    addTaggedVal(name).emit();
                        }
                        diag(prevDecl.getLocation(), note_previous_use).emit();
                        if (safeToContinue)
                            kind = prevTagDecl.getTagKind();
                        else
                        {
                            name = null;
                            prevDecl = null;
                            invalid = true;
                        }
                    }

                    if (!invalid)
                    {
                        // If this is a use, just return the declaration we found.
                        if (tuk == TagUseKind.TUK_reference)
                        {
                            return new ActionResult<>(prevDecl);
                        }
                        // Diagnose attempts to redefine a tc.
                        if (tuk == TagUseKind.TUK_definition)
                        {
                            TagDecl def = prevTagDecl.getDefinition();
                            if (def != null)
                            {
                                diag(nameLoc, err_redefinition)
                                        .addTaggedVal(name).emit();
                                diag(def.getLocation(), note_previous_definition).emit();
                                name = null;
                                prevDecl = null;
                                invalid = true;
                            }
                            else
                            {
                                // Reaching here, it indicates that the previous
                                // is forward declaration, and this is actually
                                // complete definition.
                                TagType tag = (TagType)context.getTagDeclType(prevTagDecl).getType();
                                if (tag.isBeingDefined())
                                {
                                    diag(nameLoc, err_nested_redefinition)
                                            .addTaggedVal(name).emit();
                                    diag(prevTagDecl.getLocation(), note_previous_definition).emit();
                                    name = null;
                                    prevDecl = null;
                                    invalid = true;
                                }
                            }
                            // Okay, this is definition of a previously declared or referenced
                            // tc PrevDecl. We're going to create a new Decl for it.
                        }
                    }
                    // If we get here we have (another) forward declaration or we
                    // have a definition.  Just create a new decl.
                }
                else
                {
                    // If we get here, this is a definition of a new tc type in a nested
                    // scope, e.g. "struct foo; void bar() { struct foo; }", just create a
                    // new decl/type.  We set PrevDecl to NULL so that the entities
                    // have distinct types.
                    prevDecl = null;
                }
            }
            else
            {
                // prevDecl is anything else kinds declaration with
                // same name, we just compliation it.
                if (isDeclInScope(prevDecl, searchDC, curScope))
                {
                    diag(nameLoc, err_redefinition_different_kind)
                            .addTaggedVal(name).emit();
                    diag(prevDecl.getLocation(), note_previous_definition).emit();
                    name = null;
                    prevDecl = null;
                    invalid = true;
                }
                else
                {
                    // The existing declaration isn't relevant to us; we're in a
                    // new scope, so clear out the previous declaration.
                    prevDecl = null;
                }
            }
        }

        //CreateNewDecl:
        {
            TagDecl newDecl = null;

            // If there is an identifier, use the location of the identifier as the
            // location of the decl, otherwise use the location of the struct/union
            // keyword.
            SourceLocation loc = nameLoc.isValid() ? nameLoc : kwLoc;

            boolean isForwardReference = false;
            // Current tc is enum declaration, reference, or definition.
            if (tagType == TST.TST_enum)
            {
                newDecl = new EnumDecl(name,searchDC, loc, (EnumDecl)prevDecl);

                // if this is an undefined enum, warns it.
                if (tuk != TagUseKind.TUK_definition && !invalid)
                {
                    diag(loc, ext_forward_ref_enum).emit();
                    if (tuk == TagUseKind.TUK_reference)
                        isForwardReference = true;

                }
                if (enumUnderlying != null)
                {
                    EnumDecl ed = (EnumDecl)newDecl;
                    ed.setIntegerType(enumUnderlying);
                }
            }
            else
            {
                // struct/union
                newDecl = new RecordDecl(name, kind, curContext, loc, (RecordDecl)prevDecl);
            }

            if (invalid)
                newDecl.setInvalidDecl(true);

            // If we're declaring or defining a tc in function prototype scope
            // in C, note that this type can only be used within the function.
            if (name != null && curScope.isFunctionProtoTypeScope())
            {
                diag(loc, warn_decl_in_param_list)
                .addTaggedVal(context.getTagDeclType(newDecl)).emit();
            }

            newDecl.setLexicalDeclaration(curContext);

            if (tuk == TagUseKind.TUK_definition)
                newDecl.startDefinition();
            if (name != null)
            {
                curScope = getNonFieldDeclScope(curScope);
                pushOnScopeChains(newDecl, curScope, !isForwardReference);
            }
            else
            {
                curContext.addDecl(newDecl);
            }
            return new ActionResult<>(newDecl);
        }
    }

    /**
     * Add this decl to the scope
     *
     * @param newDecl
     * @param scope
     * @param addToScope
     */
    private void pushOnScopeChains(NamedDecl newDecl, Scope scope,
            boolean addToScope)
    {
        // move up the scope chain until we find the nearest enclosing
        // non-transparent context.
        while (scope != null && scope.getEntity().isTransparentContext())
            scope = scope.getParent();

        if (addToScope)
            curContext.addDecl(newDecl);

        scope.addDecl(newDecl);
    }

    /**
     * Performs unqualified getIdentifier lookup up starting from current scope.
     * <br>
     * Unqualified getIdentifier lookup up (C99 6.2.1) is used to find names within the
     * current scope, for example, 'x' in
     * <pre>
     *   int x;
     *   int f()
     *   {
     *       return x;  // Unqualified names lookup finds 'x' in the global scope.
     *   }
     *
     *   Different lookup criteria can find different names. For example, a
     *   particular scope can have both a struct and a function of the same
     *   getIdentifier, and each can be found by certain lookup criteria. For more
     *   information about lookup criteria, see class {@linkplain LookupNameKind}.
     * </pre>
     *
     * @param result
     * @param s
     */
    public boolean lookupName(LookupResult result, Scope s)
    {
        String name = result.getLookupName();
        if (name == null) return false;

        LookupNameKind nameKind = result.getLookupKind();

        IdentifierNamespace idns = result.getIdentifierNamespace();

        // Scan up the scope chain looking for a decl that
        // matches this identifier that is in the appropriate namespace.
        for (Decl decl : s.getDeclInScope())
        {
            // skip anonymous or non getIdentifier declaration.
            if (!(decl instanceof NamedDecl))
                continue;
            NamedDecl namedDecl = (NamedDecl)decl;
            if (namedDecl.isSameInIdentifierNameSpace(idns))
            {
                // just deal with the decl have same identifier namespace as idns.
                if (name.equals(namedDecl.getDeclName()))
                {
                    result.addDecl(namedDecl);
                }

                result.resolveKind();
                return true;
            }
        }

        // TODO LookupBiutin().
        // If we didn't find a use of this identifier, and if the identifier
        // corresponds to a jlang.driver builtin, create the decl object for the
        // builtin now, injecting it into translation unit scope, and return it.
        return  false;
    }

    private Scope getNonFieldDeclScope(Scope s)
    {
        while ((s.getFlags() & DeclScope.value) != 0 || (s.getEntity() != null
                && s.getEntity().isTransparentContext()))
            s = s.getParent();
        return s;
    }

    /**
     * Each field of a struct/union is passed into this function in order to
     * create a {@linkplain FieldDecl} object for it.
     * @param scope
     * @param tagDecl
     * @param declStart
     * @param declarator
     * @param bitWidth
     * @return
     */
    public Decl actOnField(
            Scope scope,
            Decl tagDecl,
            SourceLocation declStart,
            Declarator declarator,
            Expr bitWidth)
    {
        IdentifierInfo ii = declarator.getIdentifier();
        SourceLocation loc = declStart;
        RecordDecl record = tagDecl instanceof RecordDecl? (RecordDecl)tagDecl:null;
        if (ii != null)
            loc = declarator.getIdentifierLoc();

        QualType t = getTypeForDeclarator(declarator, null);
        diagnoseFunctionSpecifiers(declarator);
        NamedDecl prevDecl = lookupName(scope, ii.getName(), loc, LookupMemberName);

        if (prevDecl != null && !isDeclInScope(prevDecl, record, scope))
            prevDecl = null;

        SourceLocation tssl = declarator.getSourceRange().getBegin();
        FieldDecl newFD = checkFieldDecl(ii, t, null, record, loc,
                false, bitWidth, tssl, prevDecl, declarator);
        if (newFD.isInvalidDecl() && prevDecl != null)
        {
            // Do nothing.
        }
        else if (ii != null)
        {
            pushOnScopeChains(newFD, scope, true);
        }
        else
        {
            record.addDecl(newFD);
        }
        return newFD;
    }

    /**
     * Build a new FieldDecl and check its well-formedness.
     * @param ii
     * @param t
     * @param dInfo
     * @param record
     * @param loc
     * @param isMutable
     * @param bitWidth
     * @param tssl
     * @param prevDecl
     * @param d
     * @return
     */
    private FieldDecl checkFieldDecl(
            IdentifierInfo ii,
            QualType t,
            DeclaratorInfo dInfo,
            RecordDecl record,
            SourceLocation loc,
            boolean isMutable,
            Expr bitWidth,
            SourceLocation tssl,
            NamedDecl prevDecl,
            Declarator d)
    {
        boolean invalidDecl = false;
        if (d != null) invalidDecl = d.isInvalidType();

        if (t.isNull())
        {
            invalidDecl = true;
            t = context.IntTy;
        }

        // C99 6.7.2.1p8: A member of a structure or union may have any type other
        // than a variably modified type.
        if (t.isVariablyModifiedType())
        {
            boolean sizeIsNegative;
            OutParamWrapper<Boolean> x = new OutParamWrapper<>(false);
            QualType fixedTy = tryToFixInvalidVariablyModifiedType(t, context, x);
            sizeIsNegative = x.get();

            if (!fixedTy.isNull())
            {
                diag(loc, warn_illegal_constant_array_size).emit();
                t = fixedTy;
            }
            else
            {
                if(sizeIsNegative)
                    diag(loc, err_typecheck_negative_array_size).emit();
                else
                    diag(loc, err_typecheck_field_variable_size).emit();
                invalidDecl = true;
            }
        }

        boolean zeroWidth = false;
        OutParamWrapper<Boolean> x = new OutParamWrapper<>(false);
        if (bitWidth != null && verifyField(loc, ii, t, bitWidth, x))
        {
            invalidDecl = true;
            zeroWidth = false;
        }
        zeroWidth = x.get();

        FieldDecl newFD = new FieldDecl(record, ii, loc, t, bitWidth, false);

        if (invalidDecl)
            newFD.setInvalidDecl(true);

        if (prevDecl != null && !(prevDecl instanceof TagDecl))
        {
            diag(loc, err_duplicate_member).addTaggedVal(ii).emit();
            diag(prevDecl.getLocation(), note_previous_declaration).emit();
            newFD.setInvalidDecl(true);
        }

        return newFD;
    }

    private boolean verifyField(
            SourceLocation fieldLoc,
            IdentifierInfo fieldName,
            QualType fieldTy,
            Expr bitWidth,
            OutParamWrapper<Boolean> zeroWidth)
    {
        if (zeroWidth != null)
            zeroWidth.set(true);

        // C99 6.7.2.1p4 - verify the field type.
        // C++ 9.6p3: A bit-field shall have integral or enumeration type.
        if (!fieldTy.isIntegerType())
        {
            if (requireCompleteType(fieldLoc, fieldTy, err_field_incomplete))
                return true;
            if (fieldName != null)
            {
                return diag(fieldLoc, err_not_integral_type_anon_bitfield)
                        .addTaggedVal(fieldName).addTaggedVal(fieldTy)
                        .addSourceRange(bitWidth.getSourceRange())
                        .emit();
            }
            return diag(fieldLoc, err_not_integral_type_anon_bitfield)
                    .addTaggedVal(fieldTy)
                    .addSourceRange(bitWidth.getSourceRange()).emit();
        }

        APSInt value = new APSInt();
        OutParamWrapper<APSInt> x = new OutParamWrapper<>(value);
        if (verifyIntegerConstantExpression(bitWidth, x))
            return true;
        value = x.get();

        if (!value.eq(0) && zeroWidth != null)
            zeroWidth.set(false);

        // Zero-width bitfield is ok for anonymous field.
        if (value.eq(0) && fieldName != null)
            return diag(fieldLoc, err_bitfield_has_zero_width)
                    .addTaggedVal(fieldName)
                    .emit();
        if (value.isSigned() && value.isNegative())
        {
            if (fieldName != null)
                return diag(fieldLoc, err_bitfield_has_negative_width)
                        .addTaggedVal(fieldName)
                        .addTaggedVal(value.toString(10))
                        .emit();
            return diag(fieldLoc, err_anon_bitfield_has_negative_width)
                    .addTaggedVal(value.toString(10)).emit();
        }

        long typeSize = context.getTypeSize(fieldTy);
        if (value.getZExtValue() > typeSize)
        {
            if (fieldName != null)
                return diag(fieldLoc, err_bitfield_width_exceeds_type_size)
                        .addTaggedVal(fieldName).addTaggedVal((int)typeSize)
                        .emit();
            return diag(fieldLoc, err_anon_bitfield_width_exceeds_type_size)
                    .addTaggedVal((int)typeSize).emit();
        }
        return false;
    }

    public void actOnFields(Scope curScope,
            SourceLocation recordLoc,
            Decl tagDecl,
            ArrayList<Decl> fieldDecls,
            SourceLocation startLoc,
            SourceLocation endLoc)
    {
        assert tagDecl != null:"missing record decl";

        if (tagDecl.isInvalidDecl())
        {
            return;
        }

        int numNamedMembers = 0;
        ArrayList<FieldDecl> recFields = new ArrayList<>();

        RecordDecl record = (RecordDecl) tagDecl;
        for (int i = 0, e = record.getNumFields(); i < e; i++)
        {
            FieldDecl fd = record.getDeclAt(i);
            Type fdTy = fd.getDeclType().getType();

            if (!fd.isAnonymousStructOrUnion())
            {
                recFields.add(fd);
            }

            // If the field is already invalid for some reason, don't emit more
            // diagnostics about it.
            if (fd.isInvalidDecl())
                continue;

            if (fdTy.isFunctionType())
            {
                diag(fd.getLocation(), err_field_declared_as_function)
                        .addTaggedVal(fd.getDeclName()).emit();
                fd.setInvalidDecl(true);
                tagDecl.setInvalidDecl(true);
                continue;
            }
            else if (fdTy.isIncompleteArrayType()
                    && i == e - 1
                    && record != null && record.isStruct())
            {
                if (numNamedMembers < 1)
                {
                    diag(fd.getLocation(), err_flexible_array_empty_struct)
                            .addTaggedVal(fd.getDeclName()).emit();
                    fd.setInvalidDecl(true);
                    tagDecl.setInvalidDecl(true);
                    continue;
                }
                // Okay, we have a legal flexible array member at the end of the struct.
                if (record != null)
                    record.setHasFlexibleArrayMember(true);
            }
            else if (requireCompleteType(fd.getLocation(), fd.getDeclType(),
                    err_field_incomplete))
            {
                // Incomplete type
                fd.setInvalidDecl(true);
                tagDecl.setInvalidDecl(true);
                continue;
            }
            else if (fdTy.isRecordType())
            {
                RecordType fdtty = fdTy.getAsRecordType();
                if (fdtty.getDecl().hasFlexibleArrayNumber())
                {
                    if (record != null && record.isUnion())
                        record.setHasFlexibleArrayMember(true);
                    else
                    {
                        if (i != e - 1)
                            diag(fd.getLocation(), ext_variable_sized_type_in_struct)
                                    .addTaggedVal(fd.getDeclName())
                                    .addTaggedVal(fd.getDeclType())
                                    .emit();
                        else
                        {
                            diag(fd.getLocation(), ext_flexible_array_in_struct)
                                    .addTaggedVal(fd.getDeclName())
                                    .emit();
                            if (record != null)
                                record.setHasFlexibleArrayMember(true);

                        }
                    }
                }
                if (record != null && fdtty.getDecl().hasObjectMember())
                    record.setHasObjectMember(true);
            }
            // keep track of the number of named members.
            if (fd.getIdentifier() != null)
                ++numNamedMembers;
        }

        // Okay, we successfully defined 'Record'.
        if (record != null)
            record.completeDefinition();
    }

    public ActionResult<Expr> actOnNumericConstant(Token token)
    {
        assert token != null && token.is(numeric_constant);
        // FIXME: parse the int/long/float/double number with numericParser. 2017.4.8
        // A fast path for handling a single digit which is quite common case.
        // Avoiding do something difficulty.
        if (token.getLength() == 1)
        {
            char val = pp.getSpellingOfSingleCharacterNumericConstant(token);
            int intSize = pp.getTargetInfo().getIntWidth();
            return new ActionResult<>(new IntegerLiteral(
                    new APInt(intSize, val - '0'),
                    context.IntTy, token.getLocation()));
        }

        String integerBuffer = pp.getSpelling(token);

        // Creates a numeric parser for parsing the given number in string style.
        NumericLiteralParser literal = new NumericLiteralParser(integerBuffer,
                token.getLocation(), pp);

        if (literal.hadError)
            return exprError();

        Expr res = null;

        if (literal.isFloatingLiteral())
        {
            QualType ty;
            if (literal.isFloat)
                ty = context.FloatTy;
            else if (!literal.isLong)
                ty = context.DoubleTy;
            else
                ty = context.DoubleTy;      // FIXME: 17-5-5 Current long double is not supported.

            FltSemantics format = context.getFloatTypeSemantics(ty);

            boolean isExact = false;
            OutParamWrapper<Boolean> x = new OutParamWrapper<>(isExact);
            APFloat val = literal.getFloatValue(format, x);
            isExact = x.get();
            res = new FloatingLiteral(val, isExact, ty, token.getLocation());
        }
        else
        {
            QualType ty = new QualType();

            // long long is C99 feature.
            if (!pp.getLangOptions().c99 && literal.isLongLong)
            {
                diag(token.getLocation(), ext_longlong).emit();
            }

            APInt resultVal = new APInt(context.target.getIntMaxWidth(), 0);
            if (literal.getIntegerValue(resultVal))
            {
                // if the value didn't fit into the uintmat_t, warn and force filt.
                diag(token.getLocation(), warn_integer_too_large).emit();
                ty = context.UnsignedLongLongTy;
                assert (context.getTypeSize(ty) == resultVal.getBitWidth())
                        :"long long is not intmax_t?";
            }
            else
            {
                // If this value fits into a ULL, try to figure out what else it
                // fits into according to the rules of C99.6.4.4.1p5.

                boolean allowUnsigned = literal.isUnsigned;

                // Check from smallest to largest, picking the smallest type we can.
                int width = 0;
                if (!literal.isLong && !literal.isLongLong)
                {
                    int intSize = context.target.getIntWidth();

                    // Does it fit in a unsigned int?
                    if (resultVal.isIntN(intSize))
                    {
                        if (!literal.isUnsigned && !resultVal.get(intSize - 1))
                            ty = context.IntTy;
                        else if (allowUnsigned)
                            ty = context.UnsignedIntTy;
                        width = intSize;
                    }
                    // Are long/unsigned long possibilities?
                    if (ty.isNull() && !literal.isLongLong)
                    {
                        int longSize = context.target.getLongWidth();

                        // does it fit in a unsigned long?
                        if (resultVal.isIntN(longSize))
                        {
                            // Does it fit in a signed long?
                            if(!literal.isUnsigned && !resultVal.get(longSize - 1))
                                ty = context.LongTy;
                            else if (allowUnsigned)
                                ty = context.UnsignedLongTy;
                            width = longSize;
                        }
                    }

                    if (ty.isNull())
                    {
                        int longlongSize = context.target.getLongLongWidth();
                        // Does it fit in a unsigned long long?
                        if (resultVal.isIntN(longlongSize))
                        {
                            if (!literal.isUnsigned && !resultVal.get(longlongSize-1))
                                ty = context.LongLongTy;
                            else if (allowUnsigned)
                                ty = context.UnsignedLongLongTy;
                            width = longlongSize;
                        }
                    }

                    // If we still couldn't decide a type, we probably have
                    // something that does not fit in a signed long, but has no U suffix.
                    if (ty.isNull())
                    {
                        diag(token.getLocation(), warn_integer_too_large_for_signed).
                                addTaggedVal(literal.toString()).emit();
                        ty = context.UnsignedLongTy;
                        width = context.target.getLongLongWidth();
                    }

                    if (resultVal.getBitWidth() != width)
                        resultVal = resultVal.trunc(width);
                }
                return new ActionResult<>(new IntegerLiteral(resultVal, ty, token.getLocation()));
            }
        }
        if (literal.isImaginary)
        {
            // FIXME: 17-5-5 currently imaginary number is not supported
        }
        return new ActionResult<>(res);
    }

    public void actOnPopScope(Scope curScope)
    {
        if (curScope.declEmpty())
            return;
        assert (curScope.getFlags() & DeclScope.value)
                != 0 : "Scope shouldn't isDeclScope decls.";
        Iterator<Decl> itr = curScope.getDeclInScope().iterator();
        while (itr.hasNext())
        {
            Decl decl = itr.next();
            assert decl != null : "This decl didn't get pushed??";
            assert decl instanceof NamedDecl : "Decl isn't NamedDecl?";

            NamedDecl nd = (NamedDecl) decl;
            if (nd.name == null)
                continue;

            //TODO: Diagnose unused variables in this scope.
            itr.remove();
        }
    }

    /**
     * Called from {@linkplain Parser#parseFunctionDeclarator(SourceLocation, Declarator, boolean)}
     * to introduce parameters into function prototype scope.
     *
     * @param scope
     * @param paramDecls
     * @return
     */
    public Decl actOnParamDeclarator(Scope scope, Declarator paramDecls)
    {
        DeclSpec ds = paramDecls.getDeclSpec();
        StorageClass storageClass = StorageClass.SC_none;

        //Verify C99 6.7.5.3p2: The only SCS allowed is 'register'.
        if (ds.getStorageClassSpec() == SCS.SCS_register)
        {
            storageClass = StorageClass.SC_register;
        }
        else if (ds.getStorageClassSpec() != SCS.SCS_unspecified)
        {
            diag(ds.getStorageClassSpecLoc(), err_invalid_storage_class_in_func_decl).emit();
            paramDecls.getDeclSpec().clearStorageClassSpec();
        }

        diagnoseFunctionSpecifiers(paramDecls);

        OutParamWrapper<DeclaratorInfo> x = new OutParamWrapper<>();
        QualType paramDeclType = getTypeForDeclarator(paramDecls, x);
        DeclaratorInfo dInfo = x.get();


        // ensure we have a invalid getIdentifier
        IdentifierInfo ii = paramDecls.getIdentifier();
        if (ii != null)
        {
            // check redeclaration, e.g. int foo(int x, int x);
            LookupResult result = new LookupResult(this, ii.getName(),
                    paramDecls.getIdentifierLoc(), LookupOrdinaryName);
            lookupName(result, scope);

            if (result.isSingleResult())
            {
                NamedDecl preDecl = result.getFoundDecl();

                // checks if redeclaration
                if (scope.isDeclScope(preDecl))
                {
                    diag(paramDecls.getIdentifierLoc(), err_param_redefinition)
                            .addTaggedVal(ii);

                    // Recover by removing the name.
                    ii = null;
                    paramDecls.setIdentifier(null, paramDecls.getIdentifierLoc());
                    paramDecls.setInvalidType(true);
                }
            }
        }

        QualType t = adjustParameterType(paramDeclType);

        ParamVarDecl newVar;
        if (t.equals(paramDeclType))
            // parameter type did not needed adjustment.
            newVar = ParamVarDecl.create(curContext, ii, paramDecls.getIdentifierLoc(),
                    paramDeclType, storageClass);
        else
            // Keep track of both the ajusted and unadjusted type.
            newVar = OriginalParamVarDecl.create(curContext, paramDecls.getIdentifierLoc(),
                    ii, t, paramDeclType, storageClass);

        if (paramDecls.isInvalidType())
            newVar.setInvalidDecl(true);

        assert (scope.isFunctionProtoTypeScope());
        assert (scope.getFunctionProtoTypeDepth() >= 1);

        newVar.setScopeInfo(scope.getFunctionProtoTypeDepth() - 1,
                scope.getProtoTypeIndex());

        scope.addDecl(newVar);

        return newVar;
    }

    public void actOnTagStartDefinition(Scope scope, Decl tagDecl)
    {
        TagDecl tag = (TagDecl) tagDecl;

        // Enter teh tc context.
        pushDeclContext(scope, tag);
    }

    private void pushDeclContext(Scope scope, TagDecl tag)
    {
        curContext = tag;
        scope.setEntity(curContext);
    }

    private void pushDeclContext(Scope scope, DeclContext dc)
    {
        curContext = dc;
        scope.setEntity(dc);
    }

    public void actOnTagFinishDefinition(Scope scope, Decl tagDecl,
            SourceLocation rBraceLoc)
    {
        TagDecl tag = (TagDecl) tagDecl;
        tag.setRBraceLoc(rBraceLoc);

        popDeclContext();
        // Notify the consumer that we've defined a tc.
        consumer.handleTagDeclDefinition(tag);

    }

    private void popDeclContext()
    {
        curContext = getContainingDC(curContext);
    }

    private DeclContext getContainingDC(DeclContext curContext)
    {
        Decl decl = (Decl) curContext;
        return decl.getDeclContext();
    }

    private NamedDecl lookupName(Scope s,
            String name, SourceLocation loc,
            LookupNameKind lookupKind)
    {
        LookupResult result = new LookupResult(this, name, loc, lookupKind);
        lookupName(result, s);
        if (result.getResultKind() != Found)
            return null;
        else
            return result.getFoundDecl();
    }

    /**
     * If the context is a function, this function return true if decl is
     * in Scope 's', otherwise 's' is ignored and this function returns true
     * if 'decl' belongs to the given declaration context.
     *
     * @param decl
     * @param context
     * @param s
     * @return
     */
    private boolean isDeclInScope(NamedDecl decl, DeclContext context, Scope s)
    {
        if (context instanceof FunctionDecl)
        {
            return s.isDeclScope(decl);
        }
        else
        {
            return context.isDeclInContext(decl);
        }
    }

    private EnumConstantDecl checkEnumConstant(
            EnumDecl enumDecl,
            EnumConstantDecl lastEnumConst,
            SourceLocation idLoc,
            IdentifierInfo id,
            Expr val)
    {
        APSInt enumVal = new APSInt(32);
        QualType eltTy = new QualType();
        if (val != null)
        {
            // Make sure to promote the operand type to int.
            Expr temp = usualUnaryConversions(val).get();
            if (!temp.equals(val))
            {
                val = temp;
            }
            // C99 6.7.2.2p2: Make sure we have an integer constant expression.
            OutParamWrapper<APSInt> xx = new OutParamWrapper<>(enumVal);
            boolean verifyRet = verifyIntegerConstantExpression(val, xx);
            enumVal = xx.get();
            if (verifyRet)
                val = null;
            else
                eltTy = val.getType();
        }
        if (val == null)
        {
            if (lastEnumConst != null)
            {
                enumVal = lastEnumConst.getInitValue();
                enumVal.increase();

                if (enumVal.lt(lastEnumConst.getInitValue()))
                    diag(idLoc, warn_enum_value_overflow).emit();
                eltTy = lastEnumConst.getDeclType();
            }
            else
            {
                eltTy  = context.IntTy;
                enumVal.zextOrTrunc((int)context.getTypeSize(eltTy));
            }
        }
        return new EnumConstantDecl(id, enumDecl, idLoc, eltTy, val, enumVal);
    }

    public Decl actOnEnumConstant(Scope scope,
            Decl enumConstDecl,
            Decl lastConstEnumDecl,
            SourceLocation identLoc,
            IdentifierInfo name,
            SourceLocation equalLoc,
            Expr val)
    {
        EnumDecl theEnumDecl = (EnumDecl) enumConstDecl;
        EnumConstantDecl lastEnumConst = (EnumConstantDecl) lastConstEnumDecl;

        // The scope passed in may not be a decl scope.  Zip up the scope tree until
        // we find one that is.
        Scope s = getNonFieldDeclScope(scope);
        NamedDecl prevDecl = lookupName(scope, name.getName(), identLoc,
                LookupOrdinaryName);

        // redefinition diagnostic.
        if (prevDecl != null)
        {
            if (!(prevDecl instanceof TagDecl) & isDeclInScope(prevDecl,
                    curContext, s))
            {
                if (prevDecl instanceof EnumConstantDecl)
                {
                    diag(identLoc, err_redefinition_of_enumerator).emit();
                }
                else
                {
                    diag(identLoc, err_redefinition).emit();
                }
                diag(prevDecl.getLocation(), note_previous_definition).emit();
                return null;
            }
        }

        EnumConstantDecl newEnumConstDecl = checkEnumConstant(theEnumDecl,
                lastEnumConst, identLoc, name, val);
        if (newEnumConstDecl != null)
        {
            pushOnScopeChains(newEnumConstDecl, s, true);
        }
        return newEnumConstDecl;
    }

    public void actOnEnumBody(SourceLocation startLoc,
            SourceLocation lBraceLoc,
            SourceLocation rBraceLoc,
            Decl decl,
            ArrayList<Decl> enumConstantDecls,
            Scope curScope)
    {
        EnumDecl enumDecl = (EnumDecl) decl;
        QualType enumType = new QualType(enumDecl.getTypeForDecl());

        int intWidth = context.target.getIntWidth();
        int charWidth = context.target.getCharWidth();
        int shortWidth = context.target.getShortWidth();

        // Verify that all the values are okay, compute the size of the values, and
        // reverse the list.
        int numNegativeBits = 0;
        int numPositiveBits = 0;

        // Keep track of whether all elements have type int.
        boolean allElementsInt = true;

        for (Decl d : enumConstantDecls)
        {
            if (d instanceof EnumConstantDecl)
            {
                EnumConstantDecl ecd = (EnumConstantDecl) d;

                // If the enum value doesn't fit in an int, emit an extension warning.
                APSInt initVal = ecd.getInitValue();
                assert initVal.getBitWidth() >= intWidth :
                        "Shoult have promoted value to int";
                if (initVal.getBitWidth() > intWidth)
                {
                    APSInt v = new APSInt(initVal);
                    v.trunc(intWidth);
                    v.extend(initVal.getBitWidth());
                    if (v.ne(initVal))
                        diag(ecd.getLocation(), ext_enum_value_not_int).emit();
                }

                // Keep track of the size of positive and negative values.
                if (initVal.isUnsigned() || initVal.isNonNegative())
                    numPositiveBits = Math.max(numPositiveBits, initVal.getActiveBits());
                else
                    numNegativeBits = Math.max(numNegativeBits, initVal.getMinSignedBits());

                // Keep track of whether every enum element has type int (very commmon).
                if (allElementsInt)
                    allElementsInt = ecd.getDeclType().equals(context.IntTy);
            }
        }
        QualType bestType = new QualType();
        int bestWidth;

        // Figure out the type that should be used for this enum.
        if (numNegativeBits != 0)
        {
            if(numNegativeBits <= intWidth && numPositiveBits < intWidth)
            {
                bestType = context.IntTy;
                bestWidth = intWidth;
            }
            else
            {
                bestWidth = context.target.getLongWidth();
                if(numNegativeBits <= bestWidth && numPositiveBits < bestWidth)
                    bestType = context.LongTy;
                else
                {
                    bestWidth = context.target.getLongLongWidth();
                    if (numNegativeBits > bestWidth || numPositiveBits >= bestWidth)
                        diag(enumDecl.getLocation(), warn_enum_too_large).emit();
                    bestType = context.LongLongTy;
                }
            }
        }
        else
        {
            // If there is no negative value, figure out which of uint, ulong, ulonglong
            // fits.
            if (numPositiveBits <= intWidth)
            {
                bestType = context.UnsignedIntTy;
                bestWidth = intWidth;
            }
            else
            {
                bestWidth = context.target.getLongLongWidth();
                assert numPositiveBits <= bestWidth
                        :"How could an initialization get larger than ULL?";
                bestType = context.UnsignedLongLongTy;
            }
        }

        // Loop over all of the enumerator constants, changing their types to match
        // the type of the enum if needed.
        for (Decl d : enumConstantDecls)
        {
            if (d instanceof EnumConstantDecl)
            {
                EnumConstantDecl ecd = (EnumConstantDecl)d;
                // Standard C says the enumerators have int type, but we allow, as an
                // extension, the enumerators to be larger than int size.  If each
                // enumerator value fits in an int, type it as an int, otherwise type it the
                // same as the enumerator decl itself.  This means that in "enum { X = 1U }"
                // that X has type 'int', not 'unsigned'.
                if (ecd.getDeclType().equals(context.IntTy))
                {
                    APSInt v = ecd.getInitValue();
                    v.setIsUnsigned(true);
                    ecd.setInitValue(v);

                    continue;
                }

                // Determine whether the value fits into an int.
                APSInt initVal = ecd.getInitValue();
                boolean fitsInInt;
                if (initVal.isUnsigned() || !initVal.isNegative())
                    fitsInInt = initVal.getActiveBits() < intWidth;
                else
                    fitsInInt = initVal.getMinSignedBits() <= intWidth;

                // If it fits into an integer type, force it.  Otherwise force it to match
                // the enum decl type.
                QualType newTy = new QualType();
                int newWidth;
                boolean newSign;
                if (fitsInInt)
                {
                    newTy = context.IntTy;
                    newWidth = intWidth;
                    newSign = true;
                }
                else if (ecd.getDeclType().equals(bestType))
                {
                    // Already the right type!
                    continue;
                }
                else
                {
                    newTy = bestType;
                    newWidth = bestWidth;
                    newSign = bestType.isSignedIntegerType();
                }

                // Adjust the APSInt value.
                initVal.extOrTrunc(newWidth);
                initVal.setIssigned(newSign);
                ecd.setInitValue(initVal);

                // Adjust the Expr initializer and type.
                if (ecd.getInitExpr() != null)
                {
                    ecd.setInitExpr(new ImplicitCastExpr(newTy, EVK_RValue,
                            ecd.getInitExpr(), CastKind.CK_BitCast,
                            ecd.getInitExpr().getExprLocation()));
                }

                ecd.setDeclType(newTy);
            }
        }

        enumDecl.completeDefinition(bestType);
    }


    public void actOnTranslationUnitScope(Scope scope)
    {
        translateUnitScope = scope;
        pushDeclContext(scope, context.getTranslateUnitDecl());
    }

    public Decl actOnFunctionDef(Scope fnBodyScope, Declarator declarator)
    {
        assert getCurFunctionDecl() == null : "FunctionProto parsing confused";
        assert declarator.isFunctionDeclarator() : "Not a function declarator";

        Scope parentScope = fnBodyScope.getParent();
        declarator.setFunctionDefinition(true);
        Decl res = handleDeclarator(parentScope, declarator);
        return actOnStartOfFunctionDef(fnBodyScope, res);
    }

    /**
     * Diagnose function specifiers on a declaration of an identifier that
     * does not identify a function.
     * @param d
     */
    private void diagnoseFunctionSpecifiers(Declarator d)
    {
        if (d.getDeclSpec().isInlineSpecifier())
            diag(d.getDeclSpec().getInlineSpecLoc(), err_inline_non_function).emit();
    }

    private TypeDefDecl parseTypedefDecl(Scope s, Declarator d, QualType ty)
    {
        assert d.getIdentifier() != null
                :"Wrong callback for declspec without declarator";
        assert !ty.isNull() :"GetTypeForDeclarator() returned null type";

        TypeDefDecl newTD = new TypeDefDecl(curContext,
                d.getIdentifier(), d.getIdentifierLoc(), ty);

        if (ty.getType() instanceof TagType)
        {
            TagDecl td = ((TagType)ty.getType()).getDecl();
            if (td.getDeclName() == null && td.getTypedefAnonDecl() == null)
                td.setTypedefAnonDecl(newTD);
        }

        if (d.isInvalidType())
            newTD.setInvalidDecl(true);
        return newTD;
    }

	/**
     * We just parsed a typedef 'New' which has the
     * same name and scope as a previous declaration 'Old'.  Figure out
     * how to resolve this situation, merging decls or emitting
     * diagnostics as appropriate. If there was an error, set New to be invalid.
     * @param newOne
     * @param oldOne
     */
    private void mergeTypeDefDecl(TypeDefDecl newOne, Decl oldOne)
    {
        // If either decl is known invalid already, set the new one to be invalid and
        // don't bother doing any merging checks.
        if (newOne.isInvalidDecl() || oldOne.isInvalidDecl())
        {
            newOne.setInvalidDecl(true);
            return;
        }
        // Verify the old decl was also a type.
        if (!(oldOne instanceof TypeDecl))
        {
            diag(newOne.getLocation(), err_redefinition_different_kind)
                    .addTaggedVal(newOne.getDeclName()).emit();
            if (oldOne.getLocation().isValid())
            {
                diag(oldOne.getLocation(), note_previous_definition).emit();
                newOne.setInvalidDecl(true);
                return;
            }
        }
        TypeDecl oldTD = (TypeDecl)oldOne;

        QualType oldType;
        if (oldOne instanceof TypeDefDecl)
            oldType = ((TypeDefDecl)oldOne).getUnderlyingType();
        else
            oldType = context.getTypeDeclType(oldTD, null);

        // If the typedef types are not identical, reject them in all languages and
        // with any extensions enabled.
        if (oldType.equals(newOne.getUnderlyingType())
                && oldType.getType().getCanonicalTypeInternal()
                != newOne.getUnderlyingType().getType().getCanonicalTypeInternal())
        {
            diag(newOne.getLocation(),
                    err_redefinition_different_typedef)
                    .addTaggedVal(newOne.getUnderlyingType())
                    .addTaggedVal(oldType).emit();
            if (oldTD.getLocation().isValid())
                diag(oldTD.getLocation(), note_previous_definition).emit();
            newOne.setInvalidDecl(true);
            return;
        }

        diag(newOne.getLocation(), warn_redefinition_of_typedef)
                .addTaggedVal(newOne.getDeclName()).emit();
        diag(oldTD.getLocation(), note_previous_definition).emit();
    }

    private NamedDecl actOnTypedefDeclarator(Scope s,
        Declarator d,
        DeclContext dc,
        QualType ty,
        LookupResult previous,
        OutParamWrapper<Boolean> redeclaration)
    {
        diagnoseFunctionSpecifiers(d);

        TypeDefDecl newTD = parseTypedefDecl(s, d, ty);
        if (newTD == null) return null;

        if (d.isInvalidType())
            newTD.setInvalidDecl(true);

        if (previous.getFoundDecl() != null && isDeclInScope(previous.getFoundDecl(), dc, s))
        {
            redeclaration.set(true);
            mergeTypeDefDecl(newTD, previous.getFoundDecl());
        }

        return newTD;
    }

    private Decl handleDeclarator(Scope s, Declarator d)
    {
        IdentifierInfo name = d.getIdentifier();
        SourceLocation nameLoc = d.getIdentifierLoc();

        if (name == null)
        {
            if (!d.isInvalidType())
            {
                diag(d.getDeclSpec().getSourceRange().getBegin(),
                        err_declarator_need_ident)
                        .addSourceRange(d.getDeclSpec().getSourceRange())
                        .addSourceRange(d.getSourceRange())
                        .emit();
            }
            return null;
        }
        // The scope passed in may not be a decl scope.  Zip up the scope tree until
        // we find one that is.
        while ((s.getFlags() & Scope.ScopeFlags.DeclScope.value) == 0)
            s = s.getParent();

        NamedDecl New = null;
        QualType ty = getTypeForDeclarator(d, null);

        LookupResult previous = new LookupResult(this, name.getName(), nameLoc,
                LookupOrdinaryName);

        boolean isLinkageLookup = false;

        // If the declaration we're planning to build will be a function
        // or object with linkage, then lookup for another declaration with
        // linkage (C99 6.2.2p4-5
        if (d.getDeclSpec().getStorageClassSpec() == SCS.SCS_typedef)
        {
            // nothing to do.
        }
        else if (ty.isFunctionType())
        {
            if (curContext.isFunction()
                    || d.getDeclSpec().getStorageClassSpec() != SCS.SCS_static)
                isLinkageLookup = true;
        }
        else if (d.getDeclSpec().getStorageClassSpec() == SCS.SCS_extern)
            isLinkageLookup = true;

        lookupName(previous, s);
        boolean redeclaration = false;
        OutParamWrapper<Boolean> xx = new OutParamWrapper<>(redeclaration);

        if (d.getDeclSpec().getStorageClassSpec() == SCS.SCS_typedef)
        {
            New = actOnTypedefDeclarator(s, d, curContext, ty, previous, xx);
        }
        else if (ty.isFunctionType())
        {
            New = actOnFunctionDeclarator(s, d, curContext, ty, previous, xx);
        }
        else
        {
            New = actOnVariableDeclarator(s, d, curContext, ty, previous, xx);
        }
        redeclaration = xx.get();

        if (New == null)
            return null;

        if (New.getDeclName() != null && !(redeclaration && New.isInvalidDecl()))
            pushOnScopeChains(New, s, true);

        return New;
    }

    private NamedDecl actOnFunctionDeclarator(Scope s,
            Declarator d,
            DeclContext dc,
            QualType ty,
            LookupResult previous,
            OutParamWrapper<Boolean> redeclaration)
    {
        assert ty.isFunctionType();

        IdentifierInfo name = d.getIdentifier();
        SourceLocation nameLoc = d.getIdentifierLoc();
        StorageClass sc = getFunctionStorageClass(d);

        boolean isInline = d.getDeclSpec().isInlineSpecifier();
        boolean hasPrototype = (d.getNumTypeObjects() != 0
                && ((FunctionTypeInfo)d.getTypeObject(0).typeInfo).hasProtoType)
                || (!(ty.getType() instanceof FunctionType) && ty.isFunctionType());

        FunctionDecl newFD = new FunctionDecl(name, dc, nameLoc,
                ty, sc, isInline, hasPrototype);

        if (newFD == null) return null;

        // Copy the parameter declarations from the declarator D to the function
        // declaration NewFD, if they are available.
        ArrayList<ParamVarDecl> params = new ArrayList<>(16);
        if (d.isFunctionDeclarator())
        {
            FunctionTypeInfo fti = d.getFunctionTypeInfo();

            // Check for C99 6.7.5.3p10 - foo(void) is a non-varargs
            // function that takes no arguments, not a function that takes a
            // single void argument.
            // We let through "const void" here because Sema::GetTypeForDeclarator
            // already checks for that case.
            DeclSpec.ParamInfo arg = fti.argInfo.get(0);
            if (fti.numArgs == 1 && !fti.isVariadic && arg.name == null
                    && arg.param != null && arg.param instanceof ParamVarDecl
                    && ((ParamVarDecl)arg.param).getDeclType().isVoidType())
            {
                // Empty arg list, don't push any params.
                ParamVarDecl param = (ParamVarDecl)arg.param;
            }
            else if (fti.numArgs > 0 && arg.param != null)
            {
                for (int i = 0; i < fti.numArgs; i++)
                {
                    ParamVarDecl param = (ParamVarDecl)arg.param;
                    assert param.getDeclContext() != newFD:"Was set before!";
                    param.setDeclContext(newFD);
                    params.add(param);

                    if (param.isInvalidDecl())
                        newFD.setInvalidDecl(true);
                }
            }
        }
        else
        {
            // TODO FunctionPrototype.
        }

        // Finally, we know we have the right number of parameters, install them.
        newFD.setParams(params);

        // Set this FunctionDecl's range up to the right paren.
        newFD.setRangeEnd(d.getSourceRange().getEnd());
        return newFD;
    }

    /**
     * Helper method to turn variable array
     * types into constant array types in certain situations which would otherwise
     * be errors (for GCC compatibility).
     * @param ty
     * @param context
     * @param sizeIsNegative
     * @return
     */
    private static QualType tryToFixInvalidVariablyModifiedType(
            QualType ty,
            ASTContext context,
            OutParamWrapper<Boolean> sizeIsNegative)
    {
        sizeIsNegative.set(false);

        if (ty.isPointerType())
        {
            PointerType pty = ty.getAsPointerType();
            QualType pointee = pty.getPointeeType();
            QualType fixedType = tryToFixInvalidVariablyModifiedType(pointee, context, sizeIsNegative);
            if (fixedType.isNull()) return fixedType;

            fixedType = context.getPointerType(fixedType);
            fixedType.setCVRQualifiers(ty.getCVRQualifiers());
            return fixedType;
        }

        if (!ty.isVariableArrayType())
            return new QualType();

        VariableArrayType vlaTy = context.getAsVariableArrayType(ty);
        if (vlaTy.getElemType().isVariableArrayType())
            return new QualType();

        Expr.EvalResult result = new Expr.EvalResult();
        if (vlaTy.getSizeExpr() == null
                || !vlaTy.getSizeExpr().evaluate(result, context)
                || !result.val.isInt())
            return new QualType();

        APSInt res = result.val.getInt();
        if (res.le(new APSInt(res.getBitWidth(), res.isUnsigned())))
        {
            Expr arySizeExpr = vlaTy.getSizeExpr();

            return context.getConstantArrayWithExprType(vlaTy.getElemType(),
                    res, arySizeExpr, ArraySizeModifier.Normal, 0,
                    vlaTy.getBrackets());
        }

        sizeIsNegative.set(false);
        return new QualType();
    }

	/**
     * We just parsed a variable 'New' which has the same name
     * and scope as a previous declaration 'Old'.  Figure out how to resolve this
     * situation, merging decls or emitting diagnostics as appropriate.
     * @param newOne
     * @param oldOne
     */
    private void mergeVarDecl(VarDecl newOne, Decl oldOne)
    {
        if (newOne.isInvalidDecl() || oldOne.isInvalidDecl())
        {
            newOne.setInvalidDecl(true);
            return;
        }

        if (!(oldOne instanceof VarDecl))
        {
            diag(newOne.getLocation(), err_redefinition_different_kind)
                    .addTaggedVal(newOne.getDeclName()).emit();
            diag(oldOne.getLocation(), note_previous_definition).emit();
            newOne.setInvalidDecl(true);
            return;
        }

        VarDecl Old = (VarDecl)oldOne;

        QualType mergedTy = context.mergeType(newOne.getDeclType(), Old.getDeclType());
        if (mergedTy.isNull())
        {
            diag(newOne.getLocation(), err_redefinition_different_type)
                .addTaggedVal(newOne.getDeclName()).emit();
            diag(Old.getLocation(), note_previous_definition).emit();
            newOne.setInvalidDecl(true);
            return;
        }

        newOne.setDeclType(mergedTy);

        // C99 6.2.2p4: Check if we have a static decl followed by a non-static.
        if (newOne.getStorageClass() == StorageClass.SC_static &&
                (Old.getStorageClass() == StorageClass.SC_none 
                        || Old.hasExternalStorage())) {
            diag(newOne.getLocation(), err_static_non_static).
                    addTaggedVal(newOne.getDeclName()).emit();
            diag(Old.getLocation(), note_previous_definition).emit();
            newOne.setInvalidDecl(true);
            return;
        }
        // C99 6.2.2p4: 
        //   For an identifier declared with the storage-class specifier
        //   extern in a scope in which a prior declaration of that
        //   identifier is visible,23) if the prior declaration specifies
        //   internal or external linkage, the linkage of the identifier at
        //   the later declaration is the same as the linkage specified at
        //   the prior declaration. If no prior declaration is visible, or
        //   if the prior declaration specifies no linkage, then the
        //   identifier has external linkage.
        if (newOne.hasExternalStorage() && Old.hasLinkage())
            /* Okay */;
        else if (newOne.getStorageClass() != StorageClass.SC_static &&
                Old.getStorageClass() == StorageClass.SC_static) {
            diag(newOne.getLocation(), err_non_static_static).
                    addTaggedVal(newOne.getDeclName()).emit();
            diag(Old.getLocation(), note_previous_definition).emit();
            newOne.setInvalidDecl(true);
            return;
        }

        // Variables with external linkage are analyzed in FinalizeDeclaratorGroup.

        // FIXME: The test for external storage here seems wrong? We still
        // need to check for mismatches.
        if (!newOne.hasExternalStorage() && !newOne.isFileVarDecl() &&
                // Don't complain about out-of-line definitions of static members.
                !(Old.getDeclContext().isRecord() &&
            !newOne.getDeclContext().isRecord()))
        {
            diag(newOne.getLocation(), err_redefinition).
                    addTaggedVal(newOne.getDeclName()).emit();
            diag(Old.getLocation(), note_previous_definition).emit();
            newOne.setInvalidDecl(true);
            return;
        }

        // Keep a chain of previous declarations.
        // todo newOne.setPreviousDeclaration(Old);
    }

	/**
     * This routine performs all of the type-checking required for a
     * variable declaration once it has been built. It is used both to
     * check variables after they have been parsed and their declarators
     * have been translated into a declaration
     * @param newVD
     * @param prevDecl
     * @param redeclaration
     */
    private void checkVariableDeclaration(VarDecl newVD,
            NamedDecl prevDecl,
            OutParamWrapper<Boolean> redeclaration)
    {
        if (newVD.isInvalidDecl())
            return;
        QualType ty = newVD.getDeclType();

        boolean isVM = ty.isVariablyModifiedType();
        if ((isVM && newVD.hasLinkage()) || (ty.isVariableArrayType()
            && newVD.hasGlobalStorage()))
        {
            // TODO

            OutParamWrapper<Boolean> SizeIsNegative = new OutParamWrapper<>(false);
            QualType FixedTy = tryToFixInvalidVariablyModifiedType(ty, context, SizeIsNegative);

            if (FixedTy.isNull() && ty.isVariableArrayType()) {
                VariableArrayType VAT = context.getAsVariableArrayType(ty);
                // FIXME: This won't give the correct result for
                // int a[10][n];
                SourceRange SizeRange = VAT.getSizeExpr().getSourceRange();

                if (newVD.isFileVarDecl())
                    diag(newVD.getLocation(), err_vla_decl_in_file_scope)
                            .addSourceRange(SizeRange)
                            .emit();
                else if (newVD.getStorageClass() == StorageClass.SC_static)
                    diag(newVD.getLocation(), err_vla_decl_has_static_storage)
                            .addSourceRange(SizeRange)
                            .emit();
                else
                    diag(newVD.getLocation(), err_vla_decl_has_extern_linkage)
                            .addSourceRange(SizeRange)
                            .emit();
                newVD.setInvalidDecl(true);
                return;
            }

            if (FixedTy.isNull())
            {
                if (newVD.isFileVarDecl())
                    diag(newVD.getLocation(), err_vm_decl_in_file_scope).emit();
                else
                    diag(newVD.getLocation(), err_vm_decl_has_extern_linkage).emit();
                newVD.setInvalidDecl(true);
                return;
            }

            diag(newVD.getLocation(), warn_illegal_constant_array_size).emit();
            newVD.setDeclType(FixedTy);
            return;
        }

        if (ty.isVoidType() && !newVD.hasExternalStorage())
        {
            diag(newVD.getLocation(), err_typecheck_decl_incomplete_type)
                    .addTaggedVal(ty).emit();
            newVD.setInvalidDecl(true);
            return;
        }
        if (prevDecl != null)
        {
            redeclaration.set(true);
            mergeVarDecl(newVD, prevDecl);
        }
    }

    private NamedDecl actOnVariableDeclarator(Scope s,
            Declarator d,
            DeclContext dc,
            QualType ty,
            LookupResult previous,
            OutParamWrapper<Boolean> redeclaration)
    {
        IdentifierInfo name = d.getIdentifier();
        SourceLocation nameLoc = d.getIdentifierLoc();
        SCS scsSpec = d.getDeclSpec().getStorageClassSpec();
        StorageClass sc = storageClassSpecToVarDeclStorageClass(scsSpec);

        if (name == null)
        {
            diag(d.getIdentifierLoc(), err_bad_variable_name).emit();
            return null;
        }

        diagnoseFunctionSpecifiers(d);

        if (!dc.isRecord() && s.getFuncParent() == null)
        {
            // C99 6.9p2: The storage-class specifiers auto and register shall not
            // appear in the declaration specifiers in an external declaration.
            if (sc == StorageClass.SC_auto || sc == StorageClass.SC_register)
            {
                if (sc == StorageClass.SC_register)
                    diag(d.getIdentifierLoc(), err_unsupported_global_register).emit();
                else
                    diag(d.getIdentifierLoc(), err_typecheck_sclass_fscope).emit();
                d.setInvalidType(true);
            }
        }

        if (dc.isRecord() && !curContext.isRecord())
        {
            // This is an out-of-line definition of a static data member.
            if(sc == StorageClass.SC_static)
            {
                diag(d.getDeclSpec().getStorageClassSpecLoc(), err_static_out_of_line)
                        .addFixItHint(FixItHint.createRemoval(
                        new SourceRange(d.getDeclSpec().getStorageClassSpecLoc())))
                        .emit();
            }else if (sc == StorageClass.SC_none)
                sc = StorageClass.SC_static;
        }

        // Create a variable decl now.
        VarDecl newVD = new VarDecl(DeclKind.VarDecl, dc, name, nameLoc,ty, sc);
        if (d.isInvalidType())
            newVD.setInvalidDecl(true);

        checkVariableDeclaration(newVD, previous.getFoundDecl(),
                redeclaration);
        return newVD;
    }

    private StorageClass storageClassSpecToVarDeclStorageClass(SCS scsSpec)
    {
        switch (scsSpec)
        {
            case SCS_unspecified:
            case SCS_typedef:
                return StorageClass.SC_none;
            case SCS_extern:return StorageClass.SC_extern;
            case SCS_static: return StorageClass.SC_static;
            case SCS_auto: return StorageClass.SC_auto;
            case SCS_register: return StorageClass.SC_register;
        }
        Util.shouldNotReachHere("unkonwn storage class!");
        return StorageClass.SC_none;
    }

    private StorageClass getFunctionStorageClass(Declarator d)
    {
        StorageClass sc;
        switch (d.getDeclSpec().getStorageClassSpec())
        {
            default: assert false :"Unknown storage class!";
            case SCS_auto:
            case SCS_register:
                diag(d.getDeclSpec().getStorageClassSpecLoc(), err_typecheck_sclass_func)
                        .emit();
                d.setInvalidType(true);
                break;
            case SCS_unspecified:
                sc = StorageClass.SC_none;
            case SCS_extern:
            case SCS_static:
            {
                if (curContext.getLookupContext().isFunction())
                {
                    // C99 6.7.1p5:
                    //   The declaration of an identifier for a function that has
                    //   block scope shall have no explicit storage-class specifier
                    //   other than extern
                    diag(d.getDeclSpec().getStorageClassSpecLoc(),err_static_block_func)
                            .emit();
                    sc = StorageClass.SC_none;
                }
                else
                {
                    sc = StorageClass.SC_static;
                }
                break;
            }
        }
        // No explicit storage class has already been returned
        sc = StorageClass.SC_none;
        return sc;
    }

    private QualType convertDeclSpecToType(DeclSpec ds, Declarator d, SourceLocation loc)
    {
        QualType result = null;

        switch (ds.getTypeSpecType())
        {
            case TST_void:
                result = context.VoidTy;
                break;
            case TST_char:
                if (ds.getTypeSpecSign() == DeclSpec.TSS.TSS_unspecified)
                    result = context.CharTy;
                else if (ds.getTypeSpecSign() == DeclSpec.TSS.TSS_signed)
                    result = context.SignedCharTy;
                else
                {
                    assert ds.getTypeSpecSign() == DeclSpec.TSS.TSS_unsigned
                            :"Unknown TSS value";
                    result = context.UnsignedCharTy;
                }
                break;
            case TST_unspecified:
                // Unspecified typespec defaults to int in C90.
                if (getLangOptions().implicitInt)
                {
                    // In C89 mode, we only warn if there is a completely missing declspec
                    // when one is not allowed.
                    if (ds.isEmpty())
                    {
                        diag(loc, ext_missing_declspec)
                                .addSourceRange(ds.getSourceRange()).addFixItHint
                                (FixItHint.createInsertion(ds.getSourceRange().getBegin(), "int"))
                                .emit();
                    }
                }
                else if (!ds.hasTypeSpecifier())
                {
                    // C99 require a type specifier.  For example, C99 6.7.2p2 says:
                    // "At least one type specifier shall be given in the declaration
                    // specifiers in each declaration, and in the specifier-qualifier list in
                    // each struct declaration and type name."
                    diag(loc, ext_missing_type_specifier).
                            addSourceRange(ds.getSourceRange());
                }
                // fall through
            case TST_int:
            {
                if (ds.getTypeSpecSign() != DeclSpec.TSS.TSS_unsigned)
                {
                    switch (ds.getTypeSpecWidth())
                    {
                        case TSW_unspecified:
                            result = context.IntTy;
                            break;
                        case TSW_short:
                            result = context.ShortTy;
                            break;
                        case TSW_long:
                            result = context.LongTy;
                            break;
                        case TSW_longlong:
                            result = context.LongLongTy;
                            if (!getLangOptions().c99)
                                diag(ds.getTypeSpecWidthLoc(), ext_longlong).emit();
                            break;
                    }
                }
                else
                {
                    switch (ds.getTypeSpecWidth())
                    {
                        case TSW_unspecified:
                            result = context.UnsignedIntTy;break;
                        case TSW_short:
                            result = context.UnsignedShortTy;break;
                        case TSW_long:
                            result = context.UnsignedLongTy;break;
                        case TSW_longlong:
                            result = context.UnsignedLongLongTy;
                            if (!getLangOptions().c99)
                                diag(ds.getTypeSpecWidthLoc(), ext_longlong).emit();
                            break;
                    }
                }
                break;
            }
            case TST_float:
                result = context.FloatTy;
                break;
            case TST_double:
                result = context.DoubleTy;
                break;
            case TST_bool:
                // _Bool
                result = context.BoolTy;
                break;
            case TST_enum:
            case TST_struct:
            case TST_union:
            {
                Decl decl = ds.getRepAsDecl();
                if (!(decl instanceof TypeDecl))
                {
                    result = context.IntTy;
                    d.setInvalidType(true);
                    break;
                }
                TypeDecl typeDecl = (TypeDecl)decl;

                assert ds.getTypeSpecWidth() == DeclSpec.TSW.TSW_unspecified
                        && ds.getTypeSpecComplex() == DeclSpec.TSC.TSC_unspecified
                        && ds.getTypeSpecSign() == DeclSpec.TSS.TSS_unspecified
                        : "No qualifiers on tc names!";

                // TypeQuals handled by caller.
                result = context.getTypeDeclType(typeDecl);

                if (typeDecl.isInvalidDecl())
                    d.setInvalidType(true);
                break;
            }
            case TST_typename:
            {
                assert ds.getTypeSpecWidth() == DeclSpec.TSW.TSW_unspecified
                        && ds.getTypeSpecComplex() == DeclSpec.TSC.TSC_unspecified
                        && ds.getTypeSpecSign() == DeclSpec.TSS.TSS_unspecified
                        :"Can't handle qualifiers on typedef names yet";

                result = ds.getRepAsType();
                if (result.isNull())
                    d.setInvalidType(true);
                break;
            }
            case TST_error:
                result = context.IntTy;
                d.setInvalidType(true);
                break;
        }
        return result;
    }

    /**
     * Convert the type for the specified declarator to type instance. Skip the
     * outermost skip type object.
     * @param d
     * @return
     */
    private QualType getTypeForDeclarator(Declarator d, OutParamWrapper<DeclaratorInfo> dInfo)
    {
        // long long is C99 feature.
        if (!getLangOptions().c99 && d.getDeclSpec().getTypeSpecWidth() == DeclSpec.TSW.TSW_longlong)
        {
            diag(d.getDeclSpec().getTypeSpecWidthLoc(), ext_longlong);
        }

        DeclSpec ds = d.getDeclSpec();
        SourceLocation declLoc = d.getIdentifierLoc();
        if (!declLoc.isValid())
            declLoc = ds.getSourceRange().getBegin();

        // Determine the type of the declarator.
        QualType result = new QualType();
        switch (d.getKind())
        {
            case DK_Abstract:
            case DK_Normal:
            {
                result = convertDeclSpecToType(ds, d, d.getIdentifierLoc());
                break;
            }
        }

        // The name of the field we are declaring, if any.
        IdentifierInfo name = null;
        if (d.getIdentifier() != null)
            name = d.getIdentifier();

        boolean shouldBuildInfo = dInfo != null;

        // The QualType refering to the type as writtern in source code.
        // We cannot directly use result because it can change due to tsemantic analysis.
        QualType sourceTy = result.clone();

        for (int i = 0, e = d.getNumTypeObjects(); i < e; i++)
        {
            DeclaratorChunk declType = d.getTypeObject(i);
            switch (declType.getKind())
            {
                default:
                    assert false:"Unknown decltype!";
                    break;
                case Pointer:
                {
                    PointerTypeInfo pti = ((PointerTypeInfo)declType.typeInfo);
                    if (shouldBuildInfo)
                        sourceTy = context.getPointerType(sourceTy).
                                getQualifiedType(pti.typeQuals);
                    result = buildPointerType(result, pti.typeQuals, declType.getLocation(), name);
                    break;
                }
                case Array:
                {
                    ArrayTypeInfo ati = ((ArrayTypeInfo)declType.typeInfo);
                    if (shouldBuildInfo)
                        sourceTy = context.getIncompleteArrayType(sourceTy,
                                ArraySizeModifier.Normal, ati.typeQuals);
                    Expr arraySize = ati.numElts;
                    ArraySizeModifier asm;
                    if (ati.isStar)
                        asm = ArraySizeModifier.Star;
                    else if (ati.hasStatic)
                        asm = ArraySizeModifier.Static;
                    else
                        asm = ArraySizeModifier.Normal;

                    // int X[*] is only allowed on function pramater.
                    if (asm == ArraySizeModifier.Star &&
                            d.getContext() != FunctionProtoTypeContext)
                    {
                        diag(declType.getLocation(), err_array_star_outside_prototype).emit();
                        asm = ArraySizeModifier.Normal;
                        d.setInvalidType(true);
                    }

                    result = buildArrayType(result, asm, arraySize, ati.typeQuals,
                                    new SourceRange(declType.getLocation(),
                                    declType.getEndLocation()), name);
                    break;
                }
                case Function:
                {
                    FunctionTypeInfo fti = ((FunctionTypeInfo)declType.typeInfo);
                    if (shouldBuildInfo)
                    {
                        ArrayList<QualType> argTys = new ArrayList<>();

                        for (int j = 0, sz = fti.numArgs; j < sz; ++j)
                        {
                            if (fti.argInfo.get(j).param instanceof ParamVarDecl)
                            {
                                ParamVarDecl param = (ParamVarDecl)fti.argInfo.get(j).param;;
                                argTys.add(param.getDeclType());
                            }
                        }
                        sourceTy = context.getFunctionType(sourceTy, argTys,
                                fti.isVariadic, fti.typeQuals);
                    }

                    // If the function declarator has a prototype (i.e. it is not () and
                    // does not have a K&R-style identifier list), then the arguments are part
                    // of the type, otherwise the argument list is ().

                    // C99 6.7.5.3p1: The return type may not be a function or array type.
                    if (result.isArrayType() || result.isFunctionType())
                    {
                        diag(declType.getLocation(), err_func_returning_array_function)
                                .emit();
                        result = context.IntTy;
                        d.setInvalidType(true);
                    }

                    // If the function has no formal parameters list.
                    if (fti.numArgs == 0)
                    {
                        if (fti.isVariadic)
                        {
                            diag(fti.ellipsisLoc, err_ellipsis_first_arg).emit();
                            result = context.getFunctionType(result, null, fti.isVariadic, 0);
                        }
                        else
                        {
                            // Simple void foo(), where the incoming {@code result}
                            // is the result type.
                            result = context.getFunctionNoProtoType(result);
                        }
                    }
                    else if (fti.argInfo.get(0).param == null)
                    {
                        // C99 6.7.5.3p3: Reject int(x,y,z) when it's not a function definition.
                        diag(fti.argInfo.get(0).identLoc, err_ident_list_in_fn_declaration)
                                .emit();
                    }
                    else
                    {
                        // Otherwise, we have a function with an argument list that is
                        // potentially variadic.
                        ArrayList<QualType> argTys = new ArrayList<>();
                        for (DeclSpec.ParamInfo paramInfo : fti.argInfo)
                        {
                            ParamVarDecl param = (ParamVarDecl)paramInfo.param;
                            QualType argTy = param.getDeclType();
                            assert !argTy.isNull() :"Couldn't parse type?";

                            assert argTy.equals(adjustParameterType(argTy))
                                    :"Unadjusted type?";

                            if (argTy.isVoidType())
                            {
                                // If this is something like 'float(int, void)', reject it.  'void'
                                // is an incomplete type (C99 6.2.5p19) and function decls cannot
                                // have arguments of incomplete type.
                                if (fti.numArgs != 1 || fti.isVariadic)
                                {
                                    diag(declType.getLocation(), err_void_only_param)
                                            .emit();
                                    argTy = context.IntTy;
                                    param.setDeclType(argTy);
                                }
                                else if (paramInfo.name != null)
                                {
                                    // Reject, but continue to parse 'int(void abc)'.
                                    diag(paramInfo.identLoc, err_param_with_void_type)
                                            .emit();
                                    argTy = context.IntTy;
                                    param.setDeclType(argTy);
                                }
                                else
                                {
                                    if (argTy.getCVRQualifiers() != 0)
                                    {
                                        diag(declType.getLocation(), err_void_param_qualified)
                                                .emit();
                                    }
                                    break;
                                }
                            }
                            else if (!fti.hasProtoType)
                            {
                                if (context.isPromotableIntegerType(argTy))
                                {
                                    argTy = context.getPromotedIntegerType(argTy);
                                }
                                else
                                {
                                    if (argTy.isBuiltinType() && argTy.getTypeClass() == TypeClass.Float)
                                        argTy = context.DoubleTy;
                                }
                            }
                            argTys.add(argTy);
                        }

                        result = context.getFunctionType(result, argTys,
                                fti.isVariadic, fti.typeQuals);
                    }
                    break;
                }
            }
            if (result.isNull())
            {
                d.setInvalidType(true);
                result = context.IntTy;
            }
        }

        if (shouldBuildInfo)
            dInfo.set(getDeclaratorInfoForDeclarator(d, sourceTy, 0));

        return result;
    }

    /**
     * Create and instantiate a DeclaratorInfo with type source information.
     * @param d
     * @param t
     * @param skip
     * @return
     */
    private DeclaratorInfo getDeclaratorInfoForDeclarator(
            Declarator d, QualType t, int skip)
    {
        // TODO: 17-5-8 getDeclaratorInfoForDeclarator
        return null;
    }

    /**
     * Perform adjustment on the parameter type of a function.
     *
     * This routine adjusts the given parameter type @p T to the actual
     * parameter type used by semantic analysis (C99 6.7.5.3p[7,8],
     * @param ty
     * @return
     */
    private QualType adjustParameterType(QualType ty)
    {
        if (ty.isArrayType())
        {
            // C99 6.7.5.3p7:
            //   A declaration of a parameter as "array of type" shall be
            //   adjusted to "qualified pointer to type", where the type
            //   qualifiers (if any) are those specified within the [ and ] of
            //   the array type derivation.
            return context.getArrayDecayedType(ty);
        }
        else if (ty.isFunctionType())
        {
            // C99 6.7.5.3p8:
            //   A declaration of a parameter as "function returning type"
            //   shall be adjusted to "pointer to function returning type", as
            //   in 6.3.2.1.
            return context.getPointerType(ty);
        }
        return ty;
    }

    /**
     * Build a pointer type.
     * @param t     The type to which we will be built.
     * @param quals The const/volatile/restrict quailifiers to be applied to
     *              the pointer type.
     * @param loc   The location of the entity whose type involves this pointer
     *              type or if there is no such entity.
     * @param name  The entity name which involves pointer type.
     * @return
     */
    private QualType buildPointerType(
            QualType t,
            int quals,
            SourceLocation loc,
            IdentifierInfo name)
    {
        // Enforce C99 6.7.3p2: "Types other than pointer types derived from
        // object or incomplete types shall not be restrict-qualified."
        if ((quals & QualType.RESTRICT_QUALIFIER) != 0 && !t.isIncompleteOrObjectType())
        {
            diag(loc, err_typecheck_invalid_restrict_invalid_pointee)
                    .addTaggedVal(t);
            quals &= ~QualType.RESTRICT_QUALIFIER;
        }
        // Creates a pointer type.
        return context.getPointerType(t).getQualifiedType(quals);
    }

    /**
     * Build an array type.
     * @param t     The type of each element in this array.
     * @param asm   C99 array size modifier (e.g. '*', 'static').
     * @param arraySize The expression describing the number of elements
     * @param typeQuals The cvr-qualifiers to be appied to the array's element type.
     * @param brackets  The location of left and right bracket.
     * @param name  The name of entity that involves the array type, if any.
     * @return  A suitable array type, if there are no errors.Otherwise, return null.
     */
    private QualType buildArrayType(QualType t, ArraySizeModifier asm,
            Expr arraySize,
            int typeQuals, SourceRange brackets, IdentifierInfo name)
    {
        SourceLocation loc = brackets.getBegin();

        // C99 6.7.5.2p1: If the element type is an incomplete or function type,
        // reject it (e.g. void ary[7], struct foo ary[7], void ary[7]())
        if (requireCompleteType(loc, t, err_illegal_decl_array_incomplete_type))
            return new QualType();

        if (t.isFunctionType())
        {
            diag(loc, err_illegal_decl_array_of_functions)
                    .addTaggedVal(getPrintableNameForEntity(name));
            return new QualType();
        }

        RecordType eltTy = t.getAsRecordType();
        if (eltTy != null)
        {
            // If the element type is a struct or union that contains a variadic
            // array, accept it as a GNU extension: C99 6.7.2.1p2.
            if (eltTy.getDecl().hasFlexibleArrayNumber())
                diag(loc, ext_flexible_array_in_array).addTaggedVal(t);
        }

        // C99 6.7.5.2p1: The size expression shall have integer type.
        if (arraySize != null && !arraySize.getType().isIntegerType())
        {
            diag(arraySize.getLocStart(), err_array_size_non_int)
                    .addTaggedVal(arraySize.getType())
                    .addSourceRange(arraySize.getSourceRange());
            return new QualType();
        }

        APSInt constVal = new APSInt(32);
        OutParamWrapper<APSInt> outVal = new OutParamWrapper<>(constVal);

        if (arraySize == null)
        {
            if (asm == ArraySizeModifier.Star)
                t = context.getVariableArrayType(t, null, asm, typeQuals, brackets);
            else
                t = context.getIncompleteArrayType(t, asm, typeQuals);
        }
        else if (!arraySize.isIntegerConstantExpr(outVal, context))
        {
            // Per C99, a variable array is an array with either a non-constant
            // size or an element type that has a non-constant-size
            t = context.getVariableArrayType(t, arraySize, asm, typeQuals, brackets);
        }
        else
        {
            // C99 6.7.5.2p1: If the expression is a constant expression, it shall
            // have a value greater than zero.
            constVal = outVal.get();
            if (constVal.isSigned())
            {
                if (constVal.isNegative())
                {
                    diag(arraySize.getLocStart(), err_typecheck_negative_array_size)
                            .addSourceRange(arraySize.getSourceRange());
                    return new QualType();
                }
                else if (constVal.eq(0))
                {
                    // GCC accepts zero sized static arrays.
                    diag(arraySize.getLocStart(), ext_typecheck_zero_array_size)
                            .addSourceRange(arraySize.getSourceRange());
                }
            }

            t = context.getConstantArrayWithExprType(t, constVal, arraySize,
                    asm, typeQuals, brackets);
        }

        // If this is not C99, ext warn about VLA and C99 array size modifiers.
        if (!getLangOptions().c99)
        {
            if (arraySize != null && !arraySize.isIntegerConstantExpr(context))
                diag(loc, ext_vla);
            else if (asm != ArraySizeModifier.Normal || typeQuals != 0)
                diag(loc, ext_c99_array_usage);
        }

        return t;
    }

    private static String getPrintableNameForEntity(IdentifierInfo ii)
    {
        if (ii != null)
            return ii.getName();
        return "type name";
    }

    private Decl actOnStartOfFunctionDef(Scope fnBodyScope, Decl d)
    {
        if (d == null)
            return d;
        FunctionDecl fd = (FunctionDecl)d;

        // Enter a new function scope
        pushFunctionScope();

        // See if this is a redefinition.
        checkForFunctionRedefinition(fd);

        // The return type of a function definition must be complete
        // (C99 6.9.1p3.
        QualType resultType = fd.getReturnType();
        if (!resultType.isVoidType() && !fd.isInvalidDecl())
            requireCompleteType(fd.getLocation(), resultType,
                    err_func_def_incomplete_result);

        fd.setInvalidDecl(true);

        if (fnBodyScope != null)
            pushDeclContext(fnBodyScope, fd);

        // Check the validity of our function parameters
        checkParmsForFunctionDef(fd);

        // Introduce our parameters into the function scope
        for (int i = 0, e = fd.getNumParams(); i< e;i++)
        {
            ParamVarDecl param = fd.getParamDecl(i);
            param.setOwningFunction(fd);

            if (param.getDeclName() != null && fnBodyScope != null)
                checkShadow(fnBodyScope, param);
            pushOnScopeChains(param, fnBodyScope, true);
        }
        return fd;
    }

    private void pushFunctionScope()
    {
        functionScopes.add(new FunctionScopeInfo());
    }

    /**
     * checks if a function can be redefined. Currently,
     * only extern inline functions can be redefined, and even then only in
     * GNU89 mode.
     * @param opts
     * @return
     */
    private static  boolean canRedefineFunction(FunctionDecl fd, LangOptions opts)
    {
        return (opts.gnuMode && fd.isInlineSpecified()
                && fd.getStorageClass() ==StorageClass.SC_extern);
    }

    private void checkForFunctionRedefinition(FunctionDecl fd)
    {
        if (fd.isDefined() && !canRedefineFunction(fd, getLangOptions()))
        {
            if (getLangOptions().gnuMode && fd.isInlineSpecified()
                    && fd.getStorageClass() == StorageClass.SC_extern)
                diag(fd.getLocation(), err_redefinition_extern_inline)
                        .addTaggedVal(fd.getDeclName())
                        .addTaggedVal(0)
                        .emit();
            else
                diag(fd.getLocation(), err_redefinition).addTaggedVal(fd.getDeclName())
                    .emit();
            diag(fd.getLocation(), note_previous_definition).emit();
        }
    }

    /**
     * Checks the parameters of the given FunctionDecl.
     * @param fd
     */
    private boolean checkParmsForFunctionDef(FunctionDecl fd)
    {
        boolean hasInvalidParm = false;
        for (ParamVarDecl var : fd.getParamInfo())
        {
            // C99 6.7.5.3p4: the parameters in a parameter type list in a
            // function declarator that is part of a function definition of
            // that function shall not have incomplete type.
            if (!var.isInvalidDecl() &&
                    requireCompleteType(var.getLocation(), var.getDeclType(),
                            err_typecheck_decl_incomplete_type))
            {
                var.setInvalidDecl(true);
                hasInvalidParm = true;
            }

            // C99 6.9.1p5: If the declarator includes a parameter type list, the
            // declaration of each parameter shall include an identifier.
            if (var.getIdentifier() == null && !var.isImplicit())
                diag(var.getLocation(), err_parameter_name_omitted).emit();
        }
        return hasInvalidParm;
    }

    private void checkShadow(Scope s, VarDecl d)
    {
        LookupResult r = new LookupResult(this, d.getDeclName().getName(), d.getLocation(),
                LookupOrdinaryName);
        lookupName(r, s);
        checkShadow(s, d, r);
    }

	/**
     * Diagnose variable or built-in function shadowing.  Implements
     * -Wshadow.
     *
     * This method is called whenever a VarDecl is added to a "useful"
     * scope.
     * @param s the scope in which the shadowing name is being declared
     * @param d
     * @param r the lookup of the name
     */
    private void checkShadow(Scope s, VarDecl d, LookupResult r)
    {
        // Don't diagnose declarations at file scope.
        if (d.hasGlobalStorage())
            return;

        DeclContext dc = d.getDeclContext();

        // Only diagnose if we're shadowing an unambiguous field or variable.
        if (r.getResultKind() != Found)
            return;

        NamedDecl shadowedDecl = r.getFoundDecl();
        if (!(shadowedDecl instanceof VarDecl)
                && !(shadowedDecl instanceof FieldDecl))
            return;

        DeclContext oldDC = shadowedDecl.getDeclContext();

        // Only warn about certain kinds of shadowing for class members.
        if (dc != null && dc.isRecord())
        {
            if (!oldDC.isRecord())
                return;
        }

        int kind;
        if (oldDC instanceof RecordDecl)
        {
            if (shadowedDecl instanceof FieldDecl)
                kind = 3; // field.
            else
                kind = 2; // static data member.
        }
        else if (oldDC.isFileContext())
            kind = 1; // global
        else
            kind = 0; // local

        String name = r.getLookupName();

        // emit warning and note.
        diag(r.getNameLoc(), warn_decl_shadow).
                addTaggedVal(name).
                addTaggedVal(kind).
                addTaggedVal(oldDC, Diagnostic.ArgumentKind.ak_std_string)
                .emit();
        diag(shadowedDecl.getLocation(), note_previous_declaration).emit();
    }

    public ActionResult<Stmt> actOnDeclStmt(
            ArrayList<Decl> decls,
            SourceLocation declStart,
            SourceLocation declEnd)
    {
        if (decls.isEmpty())
            return null;
        return new ActionResult<Stmt>(new DeclStmt(decls, declStart, declEnd));
    }

    public LabelDecl lookupOrCreateLabel(
            IdentifierInfo name,
            SourceLocation loc)
    {
        NamedDecl res = lookupName(curScope, name.getName(), loc, LookupLabelName);
        if (res != null && res.getDeclContext() != curContext)
            res = null;
        if (res == null)
        {
            res = new LabelDecl(name, curContext, null, loc);
            Scope s = curScope.getFuncParent();
            assert s != null : "Not in a function?";
            pushOnScopeChains(res, s, true);
        }

        return (LabelDecl) res;
    }

    public ActionResult<Stmt> actOnLabelStmt(
            SourceLocation identLoc,
            LabelDecl theDecl,
            SourceLocation colonLoc,
            ActionResult<Stmt> subStmt)
    {
        // if the label was multiple defined, reject it and issue diagnostic
        if (theDecl.stmt != null)
        {
            diag(identLoc, err_redefinition_of_label).addTaggedVal(theDecl.getDeclName()).emit();
            diag(theDecl.getLocation(), note_previous_declaration).emit();
            return subStmt;
        }

        // otherwise, things are well-form.
        Tree.LabelledStmt s = new Tree.LabelledStmt(theDecl, subStmt.get(), colonLoc);
        theDecl.setStmt(s);
        return new ActionResult<Stmt>(s);
    }

    public ActionResult<Stmt> actOnCaseStmt(
            SourceLocation caseLoc,
            Expr expr,
            SourceLocation colonLoc)
    {
        assert expr != null : "missing expression within case statement";

        if (verifyIntegerConstantExpression(expr))
            return stmtError();

        return new ActionResult<>(new CaseStmt(expr, null, caseLoc, colonLoc));
    }

    private boolean verifyIntegerConstantExpression(Expr expr)
    {
        APSInt result = new APSInt(32);
        OutParamWrapper<APSInt> xx = new OutParamWrapper<>();
        return verifyIntegerConstantExpression(expr, xx);
    }

    private boolean verifyIntegerConstantExpression(Expr expr,
            OutParamWrapper<APSInt> result)
    {
        OutParamWrapper<APSInt> iceResult = new OutParamWrapper<>();

        if (expr.isIntegerConstantExpr(iceResult, context))
        {
            if (result.get() != null)
                result.set(iceResult.get());
            return false;
        }
        Expr.EvalResult evalResult = new Expr.EvalResult();
        if (!expr.evaluate(evalResult, context)
                || !evalResult.val.isInt()
                || evalResult.hasSideEffects())
        {
            diag(expr.getExprLocation(), err_expr_not_ice).
                    addSourceRange(expr.getSourceRange()).emit();
            if (evalResult.diag >= 0)
            {
                if (evalResult.diag != note_invalid_subexpr_in_ice
                        || !expr.ignoreParens().equals(evalResult.
                        diagExpr.ignoreParens()))
                {
                    diag(evalResult.diagLoc, evalResult.diag).emit();
                }
            }
            return true;
        }

        diag(expr.getExprLocation(), ext_expr_not_ice)
                .addSourceRange(expr.getSourceRange()).emit();

        if (evalResult.diag >= 0 && pp.getDiagnostics().getDiagnosticLevel(ext_expr_not_ice)
                != Diagnostic.Level.Ignored)
            diag(evalResult.diagLoc, evalResult.diag).emit();

        if (result.get() != null)
            result.set(evalResult.val.getInt());

        return false;
    }

    public void actOnCaseStmtBody(Stmt caseStmt, Stmt subStmt)
    {
        assert caseStmt != null && caseStmt instanceof CaseStmt;
        CaseStmt cs = (CaseStmt) caseStmt;
        cs.subStmt = subStmt;
    }

    public ActionResult<Stmt> actOnDefaultStmt(
            SourceLocation defaultLoc,
            SourceLocation colonLoc,
            Stmt subStmt)
    {
        return new ActionResult<>(
                new DefaultStmt(defaultLoc, colonLoc, subStmt));
    }

    /**
     * Issures the diagnose message for the given expression statement.
     * @param stmt
     */
    private void diagnoseUnusedExprResult(Stmt stmt)
    {
        if (!(stmt instanceof Expr))
            return;

        Expr e = (Expr)stmt;

        // Ignores expressions that have void type.
        if (e.getType().isVoidType())
            return;
        OutParamWrapper<SourceLocation> loc = new OutParamWrapper<>();
        OutParamWrapper<SourceRange> r1 = new OutParamWrapper<>();
        OutParamWrapper<SourceRange> r2 = new OutParamWrapper<>();
        if (!e.isUnusedResultAWarning(loc, r1, r2))
            return;

        int diagID = warn_unused_expr;
        e = e.ignoreParens();
        diag(loc.get(), diagID).addSourceRange(r1.get()).addSourceRange(r2.get())
                .emit();
    }

    public ActionResult<Stmt> actOnCompoundStmtBody(
            SourceLocation l,
            SourceLocation r,
            List<Stmt> stmts,
            boolean isStmtExpr)
    {
        int numElts = stmts.size();

        // If we're in C89 mode, check that we don't have any decls after stmts.  If
        // so, emit an extension diagnostic.
        if (!getLangOptions().c99)
        {
            int i = 0;
            // Skip over all declaration.
            for (; i < numElts && (stmts.get(i) instanceof DeclStmt); i++);

            // We found the end of the list or a statement.  Scan for another declstmt.
            for (; i < numElts && !(stmts.get(i) instanceof DeclStmt); i++);

            if (i != numElts)
            {
                Decl d = ((DeclStmt)stmts.get(i)).iterator().next();
                diag(d.getLocation(), ext_mixed_decls_code).emit();
            }
        }

        // Warn about unused expressions in statements.
        for (int i = 0; i < numElts; i++)
        {
            if (isStmtExpr && i == numElts - 1)
                continue;
            diagnoseUnusedExprResult(stmts.get(i));
        }

        return new ActionResult<>(new CompoundStmt(stmts, l, r));
    }

    public ActionResult<Stmt> actOnIfStmt(
            SourceLocation ifLoc,
            ActionResult<Expr> condExpr,
            Stmt thenStmt,
            Stmt elseStmt)
    {
        if (condExpr.get() == null)
            return stmtError();
        return new ActionResult<>(new IfStmt(condExpr.get(), thenStmt, elseStmt, ifLoc));
    }

    /**
     * Attempt to convert a given expression to integeral or enumerate type.
     *
     * @param switchLoc
     * @param expr
     * @return
     */
    private ActionResult<Expr> convertToIntegerOrEnumerationType(
            SourceLocation switchLoc,
            Expr expr)
    {
        QualType t = expr.getType();
        // if the subExpr already is a integral or enumeration type, we got it.
        if (!t.isIntegralOrEnumerationType())
        {
            diag(switchLoc, err_typecheck_expect_scalar_operand).emit();
        }
        return new ActionResult<>(expr);
    }

    private ActionResult<Expr> defaultFunctionLValueConversion(Expr e)
    {
        return exprError();
    }

    private ActionResult<Expr> defaultFunctionArrayLValueConversion(Expr e)
    {
        ActionResult<Expr> res = defaultFunctionArrayConversion(e);
        if (res.isInvalid())
            return exprError();
        res = defaultLvalueConversion(res.get());
        if (res.isInvalid())
            return exprError();

        return res;
    }

    /**
     * Perform the default conversion of arrays and functions to pointers.
     * Return the result of converting EXP.  For any other expression, just
     * return EXP after removing NOPs.
     */
    private ActionResult<Expr> defaultFunctionArrayConversion(Expr expr)
    {
        QualType ty = expr.getType();
        assert !ty.isNull() : "DefaultFunctionArrayConversion - missing type.";
        if (ty.getType().isFunctionType())
            expr = implicitCastExprToType(expr, ty, EVK_RValue,
                    CK_FunctionToPointerDecay).get();
        else if (ty.isArrayType())
        {
            if (expr.isLValue() || getLangOptions().c99)
            {
                expr = implicitCastExprToType(expr,
                        context.getArrayDecayedType(ty),
                        EVK_RValue,
                        CK_ArrayToPointerDecay).get();
            }
        }
        return new ActionResult<>(expr);
    }

    /**
     * Performs various conversions that are common to most operator (C99 6.3).
     * The conversions of array and function types are sometimes suppressed.
     * For example, the array-pointer conversion doesn't apply if the array is
     * an arguments to the sizeof or address(&) operators.
     * In those cases, this rountine should <b>not</b> called.
     *
     * @param expr
     * @return
     */
    private ActionResult<Expr> usualUnaryConversions(Expr expr)
    {
        // First, convert to an r-value.
        ActionResult<Expr> res = defaultFunctionArrayLValueConversion(expr);
        if (res.isInvalid())
            return new ActionResult<>(expr);
        expr = res.get();

        QualType t = expr.getType();
        assert !t.isNull() : "UsualUnaryConversion - missing type";

        // try to perform integral promotions if the object has a promotable type.
        if (t.getType().isIntegralOrEnumerationType())
        {
            QualType ty = expr.isPromotableBitField(context);
            if (!ty.isNull())
            {
                expr = implicitCastExprToType(expr, ty, EVK_RValue,
                        CK_IntegralCast).get();
                return new ActionResult<>(expr);
            }
            if (context.isPromotableIntegerType(ty))
            {
                QualType qt = context.getPromotedIntegerType(ty);
                expr = implicitCastExprToType(expr, qt, EVK_RValue,
                        CK_IntegralCast).get();
                return new ActionResult<>(expr);
            }
        }
        return new ActionResult<>(expr);
    }

    /**
     * If the {@code expr} is not of type 'Type', perform an operation of inserting
     * cast type for implicitly type casting.
     * </br>
     * If there is already an implicit cast, merge into the existing one.
     *
     * @param expr The expression to be casted.
     * @param ty   The TargetData type which expr would be casted to.
     * @param kind The kind of type cast.
     * @return The result expression have be implicitly casted.
     */
    private ActionResult<Expr> implicitCastExprToType(
            Expr expr, QualType ty,
            ExprValueKind valueKind, CastKind kind)
    {
        QualType exprTy = expr.getType();
        if (exprTy.equals(ty))
            return new ActionResult<>(expr);
        if (expr instanceof ImplicitCastExpr)
        {
            ImplicitCastExpr imptExpr = (ImplicitCastExpr) expr;
            if (imptExpr.getCastKind() == kind)
            {
                imptExpr.setType(ty);
                imptExpr.setValueKind(valueKind);
                return new ActionResult<>(expr);
            }
        }

        return new ActionResult<>(
                new ImplicitCastExpr(ty, valueKind, expr, kind, expr.getExprLocation()));
    }

    public ActionResult<Stmt> actOnStartOfSwitchStmt(
            SourceLocation switchLoc,
            Expr condExpr)
    {
        if (condExpr == null)
            return stmtError();
        ActionResult<Expr> condResult = convertToIntegerOrEnumerationType(
                switchLoc, condExpr);
        if (condResult.isInvalid())
        {
            return stmtError();
        }
        condExpr = condResult.get();

        // C99 6.7.4.2p5 - Integer promotion are performed on the controlling expression.
        condResult = usualUnaryConversions(condExpr);
        if (condResult.isInvalid())
            return stmtError();
        condExpr = condResult.get();

        getCurFunction().setHasBranchIntoScope();
        SwitchStmt ss = new SwitchStmt(condExpr, switchLoc);
        getCurFunction().switchStack.push(ss);

        return new ActionResult<Stmt>(ss);
    }

    public FunctionScopeInfo getCurFunction()
    {
        assert !functionScopes.isEmpty() :
                "Should not call this method when not in function scope";
        return functionScopes.peek();
    }

    public ActionResult<Stmt> actOnFinishSwitchStmt(
            SourceLocation switchLoc,
            Stmt switchStmt,
            Stmt body)
    {
        assert (switchStmt instanceof SwitchStmt) : "stmt must be switch stmt.";
        SwitchStmt ss = (SwitchStmt) switchStmt;
        assert ss == getCurFunction().switchStack.peek()
                : "switch stack missing push/pop";
        ss.setBody(body);
        getCurFunction().switchStack.pop();

        Expr condExpr = ss.getCond();
        if (condExpr == null)
            return stmtError();

        QualType condType = condExpr.getType();
        if (!condType.isIntegerType())
        {
            diag(switchLoc, err_typecheck_statement_requires_integer)
                    .addTaggedVal(condType).addSourceRange(condExpr.getSourceRange())
                    .emit();
            return stmtError();
        }

        long condWidth = context.getTypeSize(condType);
        boolean condIsSigned = context.isSignedIntegerOrEnumerationType(condType);

        // Accumulate all of the case values in a vector so that we can sort them
        // and detect duplicates.
        // This vector contains the int for the case after it has been converted to
        // the condition type.
        Vector<Pair<APSInt, SwitchCase>> caseLists = new Vector<>();
        DefaultStmt prevDefaultStmt = null;
        boolean caseListErroneous = false;

        for (SwitchCase sc = ss.getSwitchCaseList();
             sc != null; sc = sc.getNextCaseStmt())
        {
            if (sc instanceof DefaultStmt)
            {
                DefaultStmt ds = (DefaultStmt) sc;
                if (prevDefaultStmt != null)
                {
                    diag(ds.defaultLoc, err_multiple_default_labels_defined).emit();
                    diag(prevDefaultStmt.defaultLoc, note_duplicate_case_prev).emit();
                    caseListErroneous = true;
                }
                prevDefaultStmt = ds;
            }
            else
            {
                // We already verified that expression has a i-c-e value
                // (C99 6.8.4.2p3) - get that value now.
                CaseStmt cs = (CaseStmt) sc;
                Expr lo = cs.getCondExpr();
                APSInt loVal = lo.evaluateAsInt(context);

                // Convert the value to the same width/sign as the condition.
                convertIntegerToTypeWarnOnOverflow(loVal, condWidth,
                        condIsSigned, lo.getExprLocation(),
                        warn_case_value_overflow);

                // if the case constant is not the same type as the condition, insert
                // an implicit cast

                lo = implicitCastExprToType(lo, condType, EVK_RValue,
                        CK_IntegralCast).get();
                cs.setCondExpr(lo);

                caseLists.add(new Pair<>(loVal, cs));
            }

            // If we don't have a default statement, check whether the
            // condition is constant.
            APSInt constantCondValue = null;
            boolean hasConstantCond = false;
            boolean shouldCheckConstantCond = false;
            if (prevDefaultStmt == null)
            {
                Expr.EvalResult result = new Expr.EvalResult();
                hasConstantCond = condExpr.evaluate(result, context);
                if (hasConstantCond)
                {
                    assert result.getValue().isInt()
                            : "switch condition evaluated to non-int";
                    constantCondValue = result.getValue().getInt();
                    shouldCheckConstantCond = true;

                    int len = constantCondValue.getBitWidth();
                    assert (len == condWidth && constantCondValue.isSigned() == condIsSigned);
                }
            }

            // sort all the scalar case value so we can easily detect duplicates.
            caseLists.sort((lhs, rhs) ->
            {
                if (lhs.first.lt(rhs.first))
                    return -1;
                if (lhs.first.eq(rhs.first)
                        && lhs.second.getCaseLoc().getRawEncoding() < rhs.second.getCaseLoc()
                        .getRawEncoding())
                    return -1;
                return 1;
            });

            if (!caseLists.isEmpty())
            {
                for (int i = 0, e = caseLists.size(); i < e; i++)
                {
                    Pair<APSInt, SwitchCase> Case = caseLists.get(i);
                    if (shouldCheckConstantCond && Case.first.eq(constantCondValue))
                    {
                        shouldCheckConstantCond = false;
                    }

                    if (i != 0 && Case.first.eq(caseLists.get(i - 1).first))
                    {
                        diag(Case.second.getCaseLoc(),err_duplicate_case)
                                .addTaggedVal(Case.first.toString(10))
                                .emit();
                        Pair<APSInt, SwitchCase> prevDup = caseLists.get(i - 1);
                        diag(prevDup.second.getCaseLoc(), note_duplicate_case_prev)
                                .addTaggedVal(prevDup.first.toString(10))
                                .emit();

                        caseListErroneous = true;
                    }
                }
            }

            // complain if we have a constant condition and we didn't find a match.
            if (!caseListErroneous && shouldCheckConstantCond)
            {
                diag(condExpr.getExprLocation(), warn_missing_case_for_condition)
                        .addTaggedVal(constantCondValue.toString(10))
                        .addSourceRange(condExpr.getSourceRange())
                        .emit();
            }

            // Check to see if switch is over an Enum and handles all of its
            // values.  We only issue a warning if there is not 'default:', but
            // we still do the analysis to preserve this information in the AST
            // (which can be used by flow-based analyes).
            //
            final EnumType et = condType.getType().getAsEnumType();
            // if switch has default case, the ignore it.
            if (!caseListErroneous && !hasConstantCond && et != null)
            {
                final EnumDecl ed = et.getDecl();
                ArrayList<Pair<APSInt, EnumConstantDecl>> enumVals = new ArrayList<>(
                        64);
                // gather all enum values, set their type and sort them.
                // allowing easier comparision with caseLists.
                for (Iterator<Decl> it = ed.iterator(); it.hasNext(); )
                {
                    EnumConstantDecl enumDecl = (EnumConstantDecl) it.next();
                    APSInt val = enumDecl.getInitValue();
                    adjustAPSInt(val, condWidth, condIsSigned);
                    enumVals.add(Pair.get(val, enumDecl));
                }

                enumVals.sort((o1, o2) ->
                {
                    if (o1.first.lt(o2.first))
                        return 1;
                    else
                        return -1;
                });

                // See which case values aren't in enum.
                // TODO: we might want to check whether case values are out of the
                // enum even if we don't want to check whether all cases are handled.
                if (prevDefaultStmt == null)
                {
                    Iterator<Pair<APSInt, EnumConstantDecl>> ei = enumVals
                            .iterator();
                    for (Pair<APSInt, SwitchCase> ci : caseLists)
                    {
                        Pair<APSInt, EnumConstantDecl> next = ei.next();
                        while (ei.hasNext() && next.first.lt(ci.first))
                            next = ei.next();
                        if (!ei.hasNext() || next.first.gt(ci.first))
                        {
                            diag(ci.second.getCaseLoc(), warn_not_in_enum)
                                    .addTaggedVal(ed.getDeclName())
                                    .emit();
                        }
                    }
                }

                // Check which enum values are not in switch statement
                boolean hasCaseNotInSwitch = false;
                Iterator<Pair<APSInt, SwitchCase>> ci = caseLists.iterator();
                ArrayList<IdentifierInfo> unhandledNames = new ArrayList<>(8);

                for (Pair<APSInt, EnumConstantDecl> ei : enumVals)
                {
                    while (ci.hasNext() && ci.next().first.lt(ei.first));

                    if (ci.hasNext() && ci.next().first.eq(ei.first))
                        continue;

                    hasCaseNotInSwitch = true;
                    if (prevDefaultStmt == null)
                    {
                        unhandledNames.add(ei.second.getDeclName());
                    }
                }

                // Produce a nice diagnostic if multiple values aren't handled.
                switch (unhandledNames.size())
                {
                    case 0:
                        break;
                    case 1:
                        diag(condExpr.getExprLocation(), warn_missing_case1)
                                .addTaggedVal(unhandledNames.get(0))
                                .emit();
                        break;
                    case 2:
                        diag(condExpr.getExprLocation(), warn_missing_case2)
                                .addTaggedVal(unhandledNames.get(0))
                                .addTaggedVal(unhandledNames.get(1))
                                .emit();
                        break;
                    case 3:
                        diag(condExpr.getExprLocation(), warn_missing_case3)
                                .addTaggedVal(unhandledNames.get(0))
                                .addTaggedVal(unhandledNames.get(1))
                                .addTaggedVal(unhandledNames.get(2))
                                .emit();
                        break;
                    default:
                        diag(condExpr.getExprLocation(), warn_missing_cases).
                                addTaggedVal(unhandledNames.size()).
                                addTaggedVal(unhandledNames.get(0)).
                                addTaggedVal(unhandledNames.get(1));
                }
                if (!hasCaseNotInSwitch)
                    ss.setAllEnumCasesCovered();
            }
        }
        if (caseListErroneous)
            return stmtError();

        return new ActionResult<Stmt>(ss);
    }

    private void adjustAPSInt(APSInt value, long width, boolean isSigned)
    {
        if (value.getBitWidth() < width)
        {
            value = value.extend(width);
        }
        else if (value.getBitWidth() > width)
            value = value.trunc(width);
        value.setIssigned(isSigned);
    }

	/**
     * Converts the specified APSInt to the the specified width and sign. If an
     * overflow occurs, detect it and issures a warning information with specified
     * diagnostic.
     * @param val
     * @param newWidth
     * @param newSign
     * @param loc
     * @param diagID
     */
    private void convertIntegerToTypeWarnOnOverflow(
            APSInt val,
            long newWidth,
            boolean newSign,
            SourceLocation loc,
            int diagID)
    {
        if (newWidth > val.getBitWidth())
        {
            APSInt oldVal = new APSInt(val);
            val.extend(newWidth);

            if (!newSign && oldVal.isSigned() && oldVal.isNegative())
            {
                diag(loc, diagID).addTaggedVal(oldVal.toString(10))
                        .addTaggedVal(val.toString(10))
                        .emit();
            }
            val.setIssigned(newSign);
        }
        else if (newWidth < val.getBitWidth())
        {
            // If this is a trucation, check for overflow.
            APSInt convVal = new APSInt(val);
            convVal.trunc(newWidth);
            convVal.setIssigned(newSign);
            convVal.extend(val.getBitWidth());
            convVal.setIssigned(newSign);
            if (!convVal.eq(val))
                diag(loc, diagID).addTaggedVal(val.toString(10)).
                        addTaggedVal(convVal.toString(10)).
                        emit();

            // Regardless of whether a diagnostic was emitted, really do the
            // truncation.
            val.trunc(newWidth);
            val.setIssigned(newSign);
        }
        else if (newSign != val.isSigned())
        {
            // Convert the sign to match the sign of the condition.  This can cause
            // overflow as well.
            APSInt oldVal = new APSInt(val);
            val.setIssigned(newSign);

            // Sign bit changed.
            if (val.isNegative())
                diag(loc, diagID).addTaggedVal(oldVal.toString(10)).
                        addTaggedVal(val.toString(10)).emit();
        }
    }

    /**
     * Returns the pre-promoted qualified type of each expression.
     *
     * @param expr
     * @return
     */
    private QualType getTypeBeforeIntegralPromotion(Expr expr)
    {
        while (expr instanceof ImplicitCastExpr)
        {
            ImplicitCastExpr impcast = (ImplicitCastExpr) expr;
            if (impcast.getCastKind() != CK_IntegralCast)
                break;
            expr = impcast.getSubExpr();
        }
        return expr.getType();
    }

    public ActionResult<Stmt> actOnWhileStmt(
            SourceLocation whileLoc,
            Expr cond,
            Stmt body)
    {
        if (cond == null || body == null)
            return stmtError();
        diagnoseUnusedExprResult(body);
        return new ActionResult<>(new WhileStmt(cond, body, whileLoc));
    }

    private ActionResult<Expr> checkBooleanCondition(Expr cond, SourceLocation loc)
    {
        ActionResult<Expr> result;
        result = defaultFunctionArrayConversion(cond);
        if (result.isInvalid())
            return exprError();

        cond = result.get();
        QualType t = cond.getType();
        if (!t.isScalarType())  // C99 6.8.4 1p1
        {
            diag(loc, err_typecheck_statement_requires_scalar).
                    addTaggedVal(t).
                    addSourceRange(cond.getSourceRange()).
                    emit();
            return exprError();
        }

        return new ActionResult<>(cond);
    }

    /**
     * Find and report any interesting
     * implicit conversions in the given expression.  There are a couple
     * of competing diagnostics here, -Wconversion and -Wsign-compare.
     * @param sema
     * @param e
     * @param loc
     */
    private static void analyzeImplicitConversion(
            Sema sema, Expr e, SourceLocation loc)
    {
        // TODO: 17-5-9
    }

    /**
     * Diagnose problems involving the use of the given expression as a boolean
     * condition (e.g. in an if statement). Also performs the standard function
     * and array decays, possible changing the input variable.
     *
     * @param expr The expression to be evaluated.
     * @param loc  The location associated with the condition.
     */
    private void checkImplicitConversion(Expr expr, SourceLocation loc)
    {
        // Check for array bounds violations in cases where the check isn't triggered
        // elsewhere for other Expr types (like BinaryOperators), e.g. when an
        // ArraySubscriptExpr is on the RHS of a variable initialization.
        checkArrayAccess(expr);

        // This is not the right CC for (e.g.) a variable initialization.
        analyzeImplicitConversion(this, expr, loc);
    }

    public ActionResult<Stmt> actOnDoStmt(
            SourceLocation doLoc,
            Stmt body,
            SourceLocation whileLoc,
            SourceLocation lParenLoc,
            Expr cond,
            SourceLocation rParenLoc)
    {
        assert cond != null : "ActOnDoStmt(): missing expression";

        ActionResult<Expr> condResult = checkBooleanCondition(cond, doLoc);
        if (condResult.isInvalid())
            return stmtError();
        cond = condResult.get();

        checkImplicitConversion(cond, doLoc);

        diagnoseUnusedExprResult(body);
        return new ActionResult<>(new DoStmt(body, cond, doLoc, whileLoc, rParenLoc));
    }

    /**
     * This method is invoked when a declspec with no declarator
     * (e.g. "struct foo;") is parsed.
     *
     * @param s
     * @param ds
     * @return
     */
    public Decl parsedFreeStandingDeclSpec(Scope s, DeclSpec ds)
    {
        Decl tagD = null;
        TagDecl tag = null;
        TST typeSpec = ds.getTypeSpecType();
        if (typeSpec == TST.TST_struct || typeSpec == TST.TST_union
                || typeSpec == TST.TST_enum)
        {
            tagD = ds.getRepAsDecl();

            // we probably had an error.
            if (tagD == null)
                return null;

            tag = (TagDecl) tagD;
        }

        if (tag != null)
            tag.setFreeStanding(true);

        int typeQuals;
        if ((typeQuals = ds.getTypeQualifier()) != 0)
        {
            // Enforce C99 6.7.3p2: "Types other than pointer types derived from object
            // or incomplete types shall not be restrict-qualified."
            if ((typeQuals & TQ_restrict.value) != 0)
                diag(ds.getRestrictSpecLoc(), err_typecheck_invalid_restrict_not_pointer)
                        .addSourceRange(ds.getSourceRange()).emit();
        }

        if (ds.getTypeSpecType() == TST.TST_error)
            return tagD;

        boolean emittedWarning = false;
        if (!ds.isMissingDeclaratorOk())
        {
            // Warning about typedefs of enums without names, since this is the
            // extension in both microsoft and GNU C.
            if (ds.getStorageClassSpec() == SCS.SCS_typedef && tag != null
                    && tag instanceof EnumDecl)
            {
                diag(ds.getSourceRange().getBegin(), ext_typedef_without_a_name)
                        .addSourceRange(ds.getSourceRange()).emit();
                return tag;
            }

            diag(ds.getSourceRange().getBegin(), ext_no_declarators)
                    .addSourceRange(ds.getSourceRange()).emit();
            emittedWarning = true;
        }

        // We're going to complain about a bunch of spurious specifiers;
        // only do this if we're declaring a tc, because otherwise we
        // should be getting ext_no_declarators.
        if (emittedWarning || (tagD != null && tagD.isInvalidDecl()))
            return tagD;

        if (ds.getTypeQualifier() > 0)
        {
            if ((ds.getTypeQualifier() & TQ_const.value) != 0)
                diag(ds.getConstSpecLoc(), warn_standalone_specifier).
                        addTaggedVal("const").emit();
            if ((ds.getTypeQualifier() & TQ_volatile.value) != 0)
                diag(ds.getVolatileSpecLoc(), warn_standalone_specifier).
                        addTaggedVal("volatile").emit();
        }

        if (ds.isInlineSpecifier())
            diag(ds.getInlineSpecLoc(), warn_standalone_specifier).
                    addTaggedVal("inline").emit();

        return tagD;
    }

    public ArrayList<Decl> convertDeclToDeclGroup(Decl decl)
    {
        ArrayList<Decl> res = new ArrayList<Decl>();
        res.add(decl);
        return res;
    }

    public ArrayList<Decl> finalizeDeclaratorGroup(
            DeclSpec ds,
            ArrayList<Decl> declsInGroup)
    {
        ArrayList<Decl> res = new ArrayList<>();
        if (ds.isTypeSpecOwned())
            declsInGroup.add(ds.getRepAsDecl());

        for (Decl d : declsInGroup)
            if (d != null)
                res.add(d);

        return res;
    }

    public void actOnUninitializedDecl(Decl realDecl)
    {
        if (realDecl == null)
            return;

        if (realDecl instanceof VarDecl)
        {
            VarDecl var = (VarDecl) realDecl;
            QualType type = var.getDeclType();

            switch (var.isThisDeclarationADefinition())
            {
                case Definition:
                {
                    if (!var.hasInit())
                        break;
                    // fall through
                }
                case DeclarationOnly:
                {
                    // CompoundStmt scope. C99 6.7p7: If an identifier for an object is
                    // declared with no linkage (C99 6.2.2p6), the type for the
                    // object shall be complete.
                    if (var.isLocalVarDecl() && var.getLinkage() != NoLinkage
                            && !var.isInvalidDecl()
                            && requireCompleteType(var.getLocation(), type,
                            err_typecheck_decl_incomplete_type))
                    {
                        var.setInvalidDecl(true);
                    }
                    return;
                }
                case TentativeDefinition:
                {
                    // File scope. C99 6.9.2p2: A declaration of an identifier for an
                    // object that has file scope without an initializer, and without a
                    // storage-class specifier or with the storage-class specifier "static",
                    // constitutes a tentative definition. Note: A tentative definition with
                    // external linkage is valid (C99 6.2.2p5).
                    if (!var.isInvalidDecl())
                    {
                        final ArrayType.IncompleteArrayType arrayT = context.getAsInompleteArrayType(type);
                        if (arrayT != null)
                        {
                            if (requireCompleteType(var.getLocation(), arrayT.getElemType(),
                                    err_illegal_decl_array_incomplete_type))
                                var.setInvalidDecl(true);
                        }
                        else if (var.getStorageClass() == StorageClass.SC_static)
                        {
                            // C99 6.9.2p3: If the declaration of an identifier for an object is
                            // a tentative definition and has internal linkage (C99 6.2.2p3), the
                            // declared type shall not be an incomplete type.
                            // NOTE: code such as the following
                            //     static struct s;
                            //     struct s { int a; };
                            // is accepted by gcc. Hence here we issue a warning instead of
                            // an error and we do not invalidate the static declaration.
                            // NOTE: to avoid multiple warnings, only check the first declaration.
                            // todo if (var.getPreviousDeclaration() == null)
                            requireCompleteType(var.getLocation(), type,
                                    ext_typecheck_decl_incomplete_type);
                        }
                    }
                    return;
                }
            }

            // Provide a specific diagnostic for uninitialized variable
            // definitions with incomplete array type.
            if (type.isIncompleteArrayType())
            {
                diag(var.getLocation(), err_typecheck_incomplete_array_needs_initializer).emit();
                var.setInvalidDecl(true);
                return;
            }

            if (var.isInvalidDecl())
                return;

            if (requireCompleteType(var.getLocation(), context.getBaseElementType(type),
                    err_typecheck_decl_incomplete_type))
            {
                var.setInvalidDecl(true);
            }
        }
    }

    public Decl actOnDeclarator(Scope curScope, Declarator d)
    {
        d.setFunctionDefinition(false);

        return handleDeclarator(curScope, d);
    }

    public ActionResult<Stmt> actOnExprStmt(ActionResult<Expr> expr)
    {
        Expr e = expr.get();
        if (e == null)
            return stmtError();

        // C99 6.8.3p2: The expression in an expression statement is evaluated as a
        // void expression for its side effects.  Conversion to void allows any
        // operand, even incomplete types.
        return new ActionResult<>(e);
    }

    public ActionResult<Expr> actOnBooleanCondition(Scope scope, SourceLocation loc,
            Expr expr)
    {
        if (expr == null)
            return exprError();

        return checkBooleanCondition(expr, loc);
    }

    public ActionResult<Stmt> actOnForStmt(
            SourceLocation forLoc,
            SourceLocation lParenLoc,
            Stmt firstPart,
            Expr secondPart,
            Expr thirdPart,
            SourceLocation rParenLoc,
            Stmt body)
    {
        if (firstPart instanceof DeclStmt || firstPart == null)
        {
            // C99 6.8.5p3: The declaration part of a 'for' statement shall only
            // declare identifiers for objects having storage class 'auto' or
            // 'register'.
            DeclStmt ds = (DeclStmt) firstPart;
            for (Decl d : ds)
            {
                if (d instanceof VarDecl)
                {
                    VarDecl vd = (VarDecl) d;
                    if (vd != null && vd.isLocalVarDecl() && !vd
                            .hasLocalStorage())
                    {
                        diag(d.getLocation(), err_non_variable_decl_in_for).emit();
                    }
                }
            }
            ForStmt NewFor = new ForStmt(forLoc, rParenLoc, firstPart,
                    secondPart, thirdPart, body, rParenLoc);
            return new ActionResult<>(NewFor);
        }
        return stmtError();
    }

    public ActionResult<Stmt> actOnGotoStmt(
            SourceLocation gotoLoc,
            SourceLocation idLoc,
            LabelDecl ld)
    {
        getCurFunction().setHasBranchIntoScope();
        ld.setUsed();
        return new ActionResult<>(new GotoStmt(ld, gotoLoc, idLoc));
    }

    public ActionResult<Stmt> actOnContinueStmt(
            SourceLocation continueLoc,
            Scope curScope)
    {
        Scope s = curScope.getContinueParent();
        if (s == null)
        {
            // C99 6.8.6.2p1: A break shall appear only in or as a loop body.
            diag(continueLoc, err_continue_not_in_loop).emit();
            return stmtError();
        }
        return new ActionResult<>(new ContinueStmt(continueLoc));
    }

    public ActionResult<Stmt> actOnBreakStmt(
            SourceLocation breakLoc,
            Scope curScope)
    {
        Scope s = curScope.getBreakParent();
        if (s == null)
        {
            // C99 6.8.6.3p1: A break shall appear only in or as a switch/loop body.
            diag(breakLoc, err_break_not_in_loop_or_switch).emit();
            return stmtError();
        }
        return new ActionResult<>(new BreakStmt(breakLoc));
    }

    private DeclContext getFunctionLevelDeclContext()
    {
        DeclContext dc = curContext;
        while (dc instanceof EnumDecl)
            dc = dc.getParent();

        return dc;
    }

    /**
     * inside of a function body, this returns a pointer to the function decl for
     * the function being parsed.  If we're currently in a 'block', this returns
     * the containing context.
     */
    public FunctionDecl getCurFunctionDecl()
    {
        DeclContext dc = getFunctionLevelDeclContext();
        return (FunctionDecl) dc;
    }

    public ActionResult<Stmt> actOnReturnStmt(
            SourceLocation returnLoc,
            Expr retValExpr)
    {
        final FunctionDecl fd = getCurFunctionDecl();
        QualType retType;
        QualType declaredRetType;
        if (fd != null)
        {
            retType = fd.getReturnType();
            declaredRetType = retType;
        }
        else
        {
            // If we don't have a function scope, bail out.
            return stmtError();
        }

        ReturnStmt res = null;
        if (retType.isVoidType())
        {
            if (retValExpr != null)
            {
                // C99 6.8.6.4p1 (ext_ since GCC warns)
                int diagID =  ext_return_has_expr;
                if (retValExpr.getType().isVoidType())
                    diagID = ext_return_has_void_expr;
                else
                {
                    ActionResult<Expr> result = ignoredValueConversions(retValExpr);
                    if (result.isInvalid())
                        return stmtError();
                    retValExpr = implicitCastExprToType(retValExpr, context.VoidTy, EVK_RValue,
                            CK_ToVoid).get();
                }

                if (diagID != ext_return_has_void_expr)
                {
                    NamedDecl curDecl = getCurFunctionDecl();
                    diag(returnLoc, diagID)
                            .addTaggedVal(curDecl.getDeclName())
                            .addTaggedVal(0)
                            .addSourceRange(retValExpr.getSourceRange())
                            .emit();
                }

                checkImplicitConversion(retValExpr, returnLoc);
            }
            res = new ReturnStmt(returnLoc, retValExpr);
        }
        else if (retValExpr == null)
        {
            int diagID = warn_return_missing_expr;  // C90 6.6.6.4p4
            // C99 6.8.6.4p1 (ext_ since GCC warns)
            if (getLangOptions().c99)
                diagID = ext_return_missing_expr;
            diag(returnLoc, diagID)
                    .addTaggedVal(fd.getIdentifier())
                    .addTaggedVal(0)
                    .emit();
            res = new ReturnStmt(returnLoc);
        }
        else
        {
            // we have a non-void function with an expression, continue checking

            // C99 6.8.6.4p3(136): The return statement is not an assignment. The
            // overlap restriction of subclause 6.5.16.1 does not apply to the case of
            // function return.
            checkReturnStackAddress(retValExpr, retType, returnLoc);
            checkImplicitConversion(retValExpr, returnLoc);
            res = new ReturnStmt(returnLoc, retValExpr);
        }
        return new ActionResult<>(res);
    }

    private ActionResult<Expr> performImplicitConversion(
            Expr from,
            QualType toType)
    {
        QualType srcFrom = from.getType();
        Type decayedTy;
        QualType resTy = srcFrom;
        CastKind cast = null;
        if (srcFrom.isConstantArrayType())
        {
            ArrayType.ConstantArrayType cat = context.getAsConstantArrayType(srcFrom);
            decayedTy = context.getPointerType(cat.getElemType()).getType();
            resTy = new QualType(decayedTy, QualType.CONST_QUALIFIER);
            cast = CastKind.CK_ArrayToPointerDecay;
        }
        else if (srcFrom.isFunctionType())
        {
            resTy = context.getPointerType(new QualType(srcFrom.getAsFunctionType()));
            decayedTy = resTy.getType();
            cast = CastKind.CK_FunctionToPointerDecay;
        }
        if (resTy != srcFrom)
            from = new ImplicitCastExpr(resTy, EVK_RValue, from,
                    cast, from.getExprLocation());

        if (toType.isPointerType() && resTy.isPointerType())
        {
            if (toType.isVoidType() && resTy.isVoidType())
                return new ActionResult<>(from);
        }
        if (!context.isCompatible(resTy, toType))
            from = new ImplicitCastExpr(toType, EVK_RValue, from,
                    cast, from.getExprLocation());
        return new ActionResult<>(from);
    }

    /**
     * Check if a return statement returns the address of a stack variable.
     *
     * @param retValExpr
     * @param retType
     * @param returnLoc
     */
    private void checkReturnStackAddress(
            Expr retValExpr,
            QualType retType,
            SourceLocation returnLoc)
    {
        // TODO: 2017/3/28
    }

	/**
     * Perform the conversions required for an expression used in a
     * context that ignores the result.
     * @param e
     * @return
     */
    private ActionResult<Expr> ignoredValueConversions(Expr e)
    {
        if (e.isRValue())
        {
            if (e.getType().isFunctionType())
                return defaultFunctionArrayConversion(e);

            return new ActionResult<>(e);
        }
        ActionResult<Expr> res = defaultFunctionLValueConversion(e);
        if (res.isInvalid())
            return new ActionResult<>(e);

        if (!e.getType().isVoidType())
            requireCompleteType(e.getExprLocation(), e.getType(), err_incomplete_type);
        return new ActionResult<>(e);
    }

    /**
     * Checks if the {@code expr} should be viewed as a integer or enums constant.
     *
     * @param expr
     * @return
     */
    public boolean checkCaseExpression(Expr expr)
    {
        if (expr.isIntegerConstantExpr(context))
            return context.isSignedIntegerOrEnumerationType(expr.getType());
        return false;
    }

    /**
     * Binary Operators.  'Tok' is the token for the operator.
     * @return
     */
    public ActionResult<Expr> actOnBinOp(
            SourceLocation tokLoc,
            TokenKind tokenKind,
            Expr lhs,
            Expr rhs)
    {
        BinaryOperatorKind operatorKind = convertTokenKindToBinaryOpcode(tokenKind);
        assert lhs!= null:"actOnBinOp(): missing lhs!";
        assert rhs!=null:"actOnBinOp(): missing rhs!";

        // TODO emit warnings for tricky precedence issues, e.g. "bitfield & 0x4 == 0"
        return buildBinOp(tokLoc, operatorKind, lhs, rhs);
    }

    /**
     * Creates a new built-in binary operation with operator {@code opc} at
     * location {@code opLoc}.
     * This routine only supports built-in operations.
     *
     * @param opLoc
     * @param opc
     * @param lhs
     * @param rhs
     * @return
     */
    public ActionResult<Expr> buildBinOp(
            SourceLocation opLoc,
            BinaryOperatorKind opc,
            Expr lhs,
            Expr rhs)
    {
        ActionResult<Expr> lhsExpr = new ActionResult<>(lhs);
        ActionResult<Expr> rhsExpr = new ActionResult<>(rhs);

        OutParamWrapper<ActionResult<Expr>> x = new OutParamWrapper<>(lhsExpr);
        OutParamWrapper<ActionResult<Expr>> y = new OutParamWrapper<>(rhsExpr);

        // Result type of binary operator.
        QualType resultTy = new QualType();

        // The following two variables are used for compound assignment operators
        QualType compLHSTy = new QualType();    // Type of LHS after promotions for computation
        QualType compResultTy = new QualType(); // Type of computation result
        ExprValueKind VK = EVK_RValue;

        switch (opc)
        {
            case BO_Assign:
                resultTy = checkAssignmentOperands(lhs, y, opLoc, resultTy);
                break;
            case BO_Mul:
            case BO_Div:
                resultTy = checkMultiplyDivideOperands(x, y, opLoc,
                        false, opc == BO_Div);
                break;
            case BO_Rem:
                resultTy = checkRemainderOperands(x, y, opLoc, false);
                break;
            case BO_Add:
                resultTy = checkAdditionOperands(x, y, opLoc);
                break;
            case BO_Sub:
                resultTy = checkSubtractionOperands(x, y, opLoc);
                break;
            case BO_Shl:
            case BO_Shr:
                resultTy = checkShiftOperands(x, y, opLoc, opc);
                break;
            case BO_LE:
            case BO_LT:
            case BO_GE:
            case BO_GT:
                resultTy = checkComparisonOperands(x, y, opLoc, opc, true);
                break;
            case BO_EQ:
            case BO_NE:
                resultTy = checkComparisonOperands(x, y, opLoc, opc, false);
                break;
            case BO_And:
            case BO_Xor:
            case BO_Or:
                resultTy = checkBitwiseOperands(x, y, opLoc);
                break;
            case BO_LAnd:
            case BO_LOr:
                resultTy = checkLogicalOperands(x, y, opLoc, opc);
                break;
            case BO_MulAssign:
            case BO_DivAssign:
                compResultTy = checkMultiplyDivideOperands(x, y, opLoc, true, opc == BO_DivAssign);
                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !x.get().isInvalid() && !y.get().isInvalid())
                {
                    resultTy = checkAssignmentOperands(x.get().get(), y, opLoc,
                            compResultTy);
                }
                break;
            case BO_RemAssign:
                compResultTy = checkRemainderOperands(x, y, opLoc, true);
                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !x.get().isInvalid() && !y.get().isInvalid())
                    resultTy = checkAssignmentOperands(x.get().get(), y, opLoc, compResultTy);
                break;
            case BO_AddAssign:
            {
                OutParamWrapper<QualType> xx = new OutParamWrapper<>(compLHSTy);
                compResultTy = checkAdditionOperands(x, y, opLoc, xx);
                compLHSTy = xx.get();

                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !x.get().isInvalid() && !y.get().isInvalid())
                    resultTy = checkAssignmentOperands(x.get().get(), y,
                            opLoc, compResultTy);
                break;
            }
            case BO_SubAssign:
            {
                OutParamWrapper<QualType> xx = new OutParamWrapper<>(compLHSTy);
                compResultTy = checkSubtractionOperands(x, y, opLoc, xx);
                compLHSTy = xx.get();

                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !x.get().isInvalid() && !y.get().isInvalid())
                {
                    resultTy = checkAssignmentOperands(x.get().get(), y, opLoc,
                            compResultTy);
                }
                break;
            }
            case BO_ShrAssign:
            case BO_ShlAssign:
            {
                compResultTy = checkShiftOperands(x, y, opLoc, opc, true);

                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !x.get().isInvalid() && !y.get().isInvalid())
                {
                    resultTy = checkAssignmentOperands(x.get().get(), y, opLoc,
                            compResultTy);
                }
                break;
            }
            case BO_AndAssign:
            case BO_XorAssign:
            case BO_OrAssign:
            {
                compResultTy = checkBitwiseOperands(x, y, opLoc, true);

                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !x.get().isInvalid() && !y.get().isInvalid())
                    resultTy = checkAssignmentOperands(x.get().get(), y,
                            opLoc, compResultTy);
                break;
            }
            case BO_Comma:
                resultTy = checkCommaOperands(x, y, opLoc);
                break;
        }

        // Finish computing. restore the value of x and y into lhsExpr and rhsExpr respectively.
        lhsExpr = x.get();
        rhsExpr = y.get();

        if (resultTy.isNull() || lhsExpr.isInvalid() || rhsExpr.isInvalid())
            return exprError();

        // TODO  2016.10.16 Check for array bounds violations for both sides of the BinaryOperator
        checkArrayAccess(lhsExpr.get());
        checkArrayAccess(rhsExpr.get());

        if (compResultTy.isNull())
            return new ActionResult<>(new BinaryExpr(lhsExpr.get(), rhsExpr.get(),
                    opc, VK, resultTy, opLoc));

        // else it is compound assignment operator.
        return new ActionResult<>(
                new CompoundAssignExpr(lhsExpr.get(),
                        rhsExpr.get(), opc, VK, resultTy, compLHSTy, compResultTy,
                        opLoc));
    }

    /**
     * Parse a ?: operation. Note that lhs may be null in the case of a GNU
     * conditional expression.
     * @return
     */
    public ActionResult<Expr> actOnConditionalOp(
            SourceLocation quesLoc,
            SourceLocation colonLoc,
            Expr condExpr,
            Expr lhsExpr,
            Expr rhsExpr)
    {
        Expr commonExpr;
        if (lhsExpr == null)
        {
            commonExpr = condExpr;

            if (commonExpr.getValueKind() == rhsExpr.getValueKind()
                    && context.isSameType(commonExpr.getType(), rhsExpr.getType()))
            {
                ActionResult<Expr> commonRes = usualUnaryConversions(commonExpr);
                if (commonRes.isInvalid())
                    return exprError();
                commonExpr = commonRes.get();
            }

            lhsExpr = condExpr = commonExpr;
        }

        ActionResult<Expr> cond = new ActionResult<>(condExpr);
        ActionResult<Expr> lhs = new ActionResult<>(lhsExpr);
        ActionResult<Expr> rhs = new ActionResult<>(rhsExpr);

        QualType result = checkConditionalOperands(cond, lhs, rhs, EVK_RValue, quesLoc);
        if (result.isNull() || cond.isInvalid() && lhs.isInvalid() || rhs.isInvalid())
            return exprError();

        //TODO DiagnoseConditionalPrecedence
        ConditionalExpr res = new ConditionalExpr(cond.get(), quesLoc, lhs.get(), colonLoc,
                rhs.get(), result, EVK_RValue);
        return new ActionResult<>(res);
    }

    /**
     * Note that LHS is not null here, even if this is the gnu "x ?: y" extension.
     * In that case, LHS = cond.
     * C99 6.5.15
     * @param cond
     * @param lhs
     * @param rhs
     * @param kind
     * @param quesLoc
     * @return
     */
    private QualType checkConditionalOperands(
            ActionResult<Expr> cond,
            ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            ExprValueKind kind,
            SourceLocation quesLoc)
    {
        // TODO: 2017/3/28
        return null;
    }

    /**
     * C99 6.5.16.1
     * @param lhs
     * @param rhs
     * @param loc
     * @param compoundType
     * @return
     */
    private QualType checkAssignmentOperands(
            Expr lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation loc,
            QualType compoundType)
    {
        // TODO: 2017/4/9
        return null;
    }

    private QualType checkMultiplyDivideOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation opLoc,
            boolean isCompAssign,
            boolean isDiv)
    {
        QualType compType = usualArithmeticConversions(lhs, rhs, isCompAssign);

        if (lhs.get().isInvalid() || rhs.get().isInvalid())
            return new QualType();

        if (!lhs.get().get().getType().isArithmeticType()
                || !rhs.get().get().getType().isArithmeticType())
        {
            return invalidOperands(opLoc, lhs.get(), rhs.get());
        }

        /**
         * TODO Check division by zero.
        if (isDiv && rhs.get().isNullPointerConstant())
        {

        }
         */
        return compType;
    }

    private QualType invalidOperands(
            SourceLocation loc,
            ActionResult<Expr> lhs,
            ActionResult<Expr> rhs)
    {
        diag(loc, err_typecheck_invalid_operands).
                addTaggedVal(lhs.get().getType()).
                addTaggedVal(rhs.get().getType()).
                addSourceRange(lhs.get().getSourceRange()).
                addSourceRange(rhs.get().getSourceRange()).
                emit();
        return new QualType();
    }

    private boolean handleIntegerToComplexFloatConversion(
            OutParamWrapper<ActionResult<Expr>> intExpr,
            OutParamWrapper<ActionResult<Expr>> complexExpr,
            QualType intType,
            QualType complexType,
            boolean skipCast)
    {
        if (intType.isComplexType() || intType.isRealType())
            return true;
        if (skipCast) return false;

        if (intType.isIntegerType())
        {
            QualType fpTy = ((ComplexType)complexType.getType()).getElementType();
            intExpr.set(implicitCastExprToType(intExpr.get().get(), fpTy,
                    EVK_RValue, CK_IntegralToFloating));
            intExpr.set(implicitCastExprToType(intExpr.get().get(), complexType,
                    EVK_RValue, CK_FloatingRealToComplex));
        }
        else
        {
            assert intType.isComplexType();
            intExpr.set(implicitCastExprToType(intExpr.get().get(), complexType,
                    EVK_RValue, CK_IntegralComplexToFloatingComplex));
        }
        return false;
    }

    private QualType handleComplexFloatToComplexFloatConversion(
            OutParamWrapper<ActionResult<Expr>> lhsExpr,
            OutParamWrapper<ActionResult<Expr>> rhsExpr,
            QualType lhsType,
            QualType rhsType,
            boolean isCompAssign)
    {
        int order = context.getFloatingTypeOrder(lhsType, rhsType);
        if (order < 0)
        {
            // _Complex float -> _Complex double
            if (!isCompAssign)
                lhsExpr.set(implicitCastExprToType(lhsExpr.get().get(), rhsType,
                        EVK_RValue, CK_FloatingComplexCast));
            return rhsType;
        }
        if (order > 0)
        {
            // _Complex float -> _Complex double
            rhsExpr.set(implicitCastExprToType(rhsExpr.get().get(), lhsType,
                    EVK_RValue, CK_FloatingComplexCast));
        }
        return lhsType;
    }

	/**
     * Converts otherExpr to complex float and promotes complexExpr if
     * necessary.  Helper function of UsualArithmeticConversions()
     * @param complexExpr
     * @param otherExpr
     * @param complexType
     * @param otherType
     * @param convertComplexExpr
     * @param convertOtherExpr
     * @return
	 */
    private QualType handleOtherComplexFloatConversion(
            OutParamWrapper<ActionResult<Expr>> complexExpr,
            OutParamWrapper<ActionResult<Expr>> otherExpr,
            QualType complexType,
            QualType otherType,
            boolean convertComplexExpr,
            boolean convertOtherExpr)
    {
        int order = context.getFloatingTypeOrder(complexType, otherType);
        if (order > 0)
        {
            if (convertOtherExpr)
            {
                QualType fp = ((ComplexType)complexType.getType()).getElementType();
                otherExpr.set(implicitCastExprToType(otherExpr.get().get(), fp,
                        EVK_RValue, CK_FloatingCast));
                otherExpr.set(implicitCastExprToType(otherExpr.get().get(), complexType,
                        EVK_RValue, CK_FloatingRealToComplex));
            }
            return complexType;
        }

        // otherTy is at least as wide.  Find its corresponding complex type.
        QualType result = (order == 0 ? complexType : (otherType));

        // double -> _Complex double
        if (convertOtherExpr)
            otherExpr.set(implicitCastExprToType(otherExpr.get().get(), result,
                    EVK_RValue, CK_FloatingRealToComplex));

        // _Complex float -> _Complex double
        if (convertComplexExpr && order < 0)
            complexExpr.set(implicitCastExprToType(complexExpr.get().get(), result,
                    EVK_RValue, CK_FloatingComplexCast));

        return result;
    }

	/**
	 * Handle arithmetic conversion with complex types.  Helper function of
     * UsualArithmeticConversions()
     * @param lhs
     * @param rhs
     * @param lhsType
     * @param rhsType
     * @param isCompAssign
     * @return
     */
    private QualType handleComplexFloatConversion(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            QualType lhsType,
            QualType rhsType,
            boolean isCompAssign)
    {
        if (!handleIntegerToComplexFloatConversion(rhs, lhs, rhsType, lhsType, /*skipCast*/false))
            return lhsType;

        if (!handleIntegerToComplexFloatConversion(lhs, rhs, lhsType, rhsType, /*skipCast*/false))
            return rhsType;

        boolean lhsComplexFloat = lhsType.isComplexType();
        boolean rhsComplexFloat = rhsType.isComplexType();

        // If both are complex, just cast to the more precise type.
        if (lhsComplexFloat && rhsComplexFloat)
        {
            QualType res = handleComplexFloatToComplexFloatConversion(lhs, rhs, lhsType,
                    rhsType, isCompAssign);
            return res;
        }
        // If only one operand is complex, promote it if necessary and convert the
        // other operand to complex.
        if (lhsComplexFloat)
        {
            QualType res = handleOtherComplexFloatConversion(lhs, rhs, lhsType, rhsType,
                    /*convertComplexExpr*/!isCompAssign,
                    /*convertOtherExpr*/ true);
            return res;
        }

        assert rhsComplexFloat;

        QualType res = handleOtherComplexFloatConversion(rhs, lhs, rhsType, lhsType,
                /*convertComplexExpr*/true,
                /*convertOtherExpr*/ !isCompAssign);
        return res;
    }

	/**
     * Handle arithmethic conversion with floating point types.  Helper
     * function of UsualArithmeticConversions()
     * @param lhs
     * @param rhs
     * @param LHSType
     * @param RHSType
     * @param IsCompAssign
     * @return
     */
    private QualType handleFloatConversion(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            QualType LHSType,
            QualType RHSType,
            boolean IsCompAssign)
    {
        boolean LHSFloat = LHSType.isRealType();
        boolean RHSFloat = RHSType.isRealType();

        // If we have two real floating types, convert the smaller operand
        // to the bigger result.
        if (LHSFloat && RHSFloat) {
            int order = context.getFloatingTypeOrder(LHSType, RHSType);
            if (order > 0) {
                rhs.set(implicitCastExprToType(rhs.get().get(), LHSType,
                        EVK_RValue, CK_FloatingCast));
                return LHSType;
            }

            assert order < 0 : "illegal float comparison";
            if (!IsCompAssign)
                lhs.set(implicitCastExprToType(lhs.get().get(), RHSType,
                        EVK_RValue, CK_FloatingCast));
            return RHSType;
        }

        if (LHSFloat)
            return handleIntToFloatConversion(lhs, rhs, LHSType, RHSType,
                                      /*convertFloat=*/!IsCompAssign,
                                      /*convertInt=*/ true);
        assert(RHSFloat);
        return handleIntToFloatConversion(rhs, lhs, RHSType, LHSType,
                                    /*convertInt=*/ true,
                                    /*convertFloat=*/!IsCompAssign);
    }

	/**
	 * Hande arithmetic conversion from integer to float.  Helper function
     * of UsualArithmeticConversions()
     * @param floatExpr
     * @param intExpr
     * @param floatTy
     * @param intTy
     * @param convertInt
     * @param convertFloat
     * @return
     */
    private QualType handleIntToFloatConversion(
            OutParamWrapper<ActionResult<Expr>> floatExpr,
            OutParamWrapper<ActionResult<Expr>> intExpr,
            QualType floatTy,
            QualType intTy,
            boolean convertInt,
            boolean convertFloat)
    {
        if (intTy.isIntegerType())
        {
            if (convertInt)
                // Convert intExpr to the lhs floating point type.
                intExpr.set(implicitCastExprToType(intExpr.get().get(), context.FloatTy,
                        EVK_RValue, CK_IntegralToFloating));
            return context.FloatTy;
        }

        // Convert both sides to the appropriate complex float.
        //assert(intTy.isComplexType()isComplexIntegerType());
        QualType result = context.getComplexType(context.FloatTy);

        // _Complex int -> _Complex float
        if (convertInt)
            intExpr.set(implicitCastExprToType(intExpr.get().get(), result,
                    EVK_RValue, CK_IntegralComplexToFloatingComplex));

        // float -> _Complex float
        if (convertFloat)
            floatExpr.set(implicitCastExprToType(floatExpr.get().get(), result,
                    EVK_RValue, CK_FloatingRealToComplex));

        return result;
    }

    /// \brief Handle integer arithmetic conversions.  Helper function of
    /// UsualArithmeticConversions()
    private QualType handleIntegerConversion(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            QualType lhsType,
            QualType rhsType,
            boolean isCompAssign)
    {
        // The rules for this case are in C99 6.3.1.8
        int order = context.getIntegerTypeOrder(lhsType, rhsType);
        boolean lhsSigned = lhsType.isSignedIntegerType();
        boolean rhsSigned = rhsType.isSignedIntegerType();
        if (lhsSigned == rhsSigned)
        {
            // Same signedness; use the higher-ranked type
            if (order >= 0)
            {
                rhs.set(implicitCastExprToType(rhs.get().get(), lhsType,
                        EVK_RValue, CK_IntegralCast));
                return lhsType;
            }
            else if (!isCompAssign)
            {
                lhs.set(implicitCastExprToType(lhs.get().get(), rhsType,
                        EVK_RValue, CK_IntegralCast));
            }
            return rhsType;
        }
        else if (order != (lhsSigned ? 1 : -1))
        {
            // The unsigned type has greater than or equal rank to the
            // signed type, so use the unsigned type
            if (rhsSigned)
            {
                rhs.set(implicitCastExprToType(rhs.get().get(), lhsType,
                        EVK_RValue, CK_IntegralCast));
                return lhsType;
            }
            else if (!isCompAssign)
            {
                lhs.set(implicitCastExprToType(lhs.get().get(), rhsType,
                        EVK_RValue, CK_IntegralCast));
            }
            return rhsType;
        }
        else if (context.getIntWidth(lhsType) != context.getIntWidth(rhsType))
        {
            // The two types are different widths; if we are here, that
            // means the signed type is larger than the unsigned type, so
            // use the signed type.
            if (lhsSigned)
            {
                rhs.set(implicitCastExprToType(rhs.get().get(), lhsType,
                        EVK_RValue, CK_IntegralCast));
                return lhsType;
            }
            else if (!isCompAssign)
            {
                lhs.set(implicitCastExprToType(lhs.get().get(), rhsType,
                        EVK_RValue, CK_IntegralCast));
            }
            return rhsType;
        }
        else
        {
            // The signed type is higher-ranked than the unsigned type,
            // but isn't actually any bigger (like unsigned int and long
            // on most 32-bit systems).  Use the unsigned type corresponding
            // to the signed type.
            QualType result = context.getCorrespondingUnsignedType(lhsSigned ? lhsType : rhsType);
            rhs.set(implicitCastExprToType(rhs.get().get(), lhsType,
                    EVK_RValue, CK_IntegralCast));
            if (!isCompAssign)
                lhs.set(implicitCastExprToType(lhs.get().get(), rhsType,
                        EVK_RValue, CK_IntegralCast));
            return result;
        }
    }

    /**
     * Performs various conversions that are common to
     * binary operators (C99 6.3.1.8). If both operands aren't arithmetic, this
     * routine returns the first non-arithmetic type found. The client is
     * responsible for emitting appropriate error diagnostics.
     * @param isCompAssign  Whether this operation is compound assginment.
     * @return
     */
    private QualType usualArithmeticConversions(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            boolean isCompAssign)
    {
        if (!isCompAssign)
        {
            lhs.set(usualUnaryConversions(lhs.get().get()));
            if (lhs.get().isInvalid())
                return new QualType();
        }

        rhs.set(usualUnaryConversions(rhs.get().get()));
        if (lhs.get().isInvalid())
            return new QualType();

        // For conversion purposes, we ignore any qualifiers.
        // For example, "const float" and "float" are equivalent.
        QualType lhsType = lhs.get().get().getType().getUnQualifiedType();
        QualType rhsType = rhs.get().get().getType().getUnQualifiedType();

        // if both types are identical, no conversions is desired.
        if (lhsType.equals(rhsType))
            return lhsType;

        // If either side is a non-arithmetic type (e.g. a pointer), we are done.
        // The caller can deal with this (e.g. pointer + int).
        if (!lhsType.isArithmeticType() || !rhsType.isArithmeticType())
            return lhsType;

        // Apply unary and bitfield promotions to the LHS's type.
        QualType lhsUnpromotedType = lhsType;
        if (context.isPromotableIntegerType(lhsType))
            lhsType = context.getPromotedIntegerType(lhsType);

        QualType lhsBitfieldPromoteTy = lhs.get().get().isPromotableBitField(context);
        if (!lhsBitfieldPromoteTy.isNull())
            lhsType = lhsBitfieldPromoteTy;
        if (lhsType != lhsUnpromotedType && !isCompAssign)
            lhs.set(implicitCastExprToType(lhs.get().get(), lhsType, EVK_RValue,CK_IntegralCast));

        // if both types are identical, no conversions is desired.
        if (lhsType.equals(rhsType))
            return lhsType;

        // At this point, we have two different arithmetic type.

        // Handle complex types first (C99 6.3.1.8p1)
        if (lhsType.isComplexType() || rhsType.isComplexType())
        {
            return handleComplexFloatConversion(lhs, rhs, lhsType, rhsType,
                    isCompAssign);
        }

        // Now deal with real types, e.g. "float", "double", "long double".
        if (lhsType.isRealType() || rhsType.isRealType())
        {
            return handleFloatConversion(lhs, rhs, lhsType, rhsType, isCompAssign);
        }

        // Finally, we have two differing integer types
        return handleIntegerConversion(lhs, rhs, lhsType, rhsType, isCompAssign);
    }

    private QualType checkRemainderOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation opLoc,
            boolean isCompAssign)
    {
        QualType compType = usualArithmeticConversions(lhs, rhs, isCompAssign);
        if (lhs.get().isInvalid() || rhs.get().isInvalid())
            return new QualType();

        if (!lhs.get().get().getType().isIntegerType()
                || !rhs.get().get().getType().isIntegerType())
            return invalidOperands(opLoc, lhs.get(), rhs.get());

        /**
         * TODO check for rem by zero.
         *
         */
        return compType;
    }

    private QualType checkAdditionOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation opLoc,
            OutParamWrapper<QualType> compLHSTy)
    {
        QualType compType = usualArithmeticConversions(lhs, rhs, compLHSTy!=null);
        if (lhs.get().isInvalid() || rhs.get().isInvalid())
            return new QualType();

        // Handle the common case, both two operands are arithmetic type.
        if (lhs.get().get().getType().isArithmeticType()
                && rhs.get().get().getType().isArithmeticType())
        {
            if (compLHSTy != null) compLHSTy.set(compType);
            return compType;
        }

        // Put any potential pointer into pExpr.
        Expr pExp = lhs.get().get(), iExp = rhs.get().get();
        if (iExp.getType().isPointerType())
            Util.swap(pExp, iExp);

        if (!pExp.getType().isPointerType() ||
                !iExp.getType().isIntegerType())
            return invalidOperands(opLoc, lhs.get(), rhs.get());

        if (!checkArithmeticOpPointerOperand(opLoc, pExp))
            return new QualType();
        /**
         * TODO check array bounds for pointer arithmetic.
         *
         */
        checkArrayAccess(pExp, iExp);
        if (compLHSTy != null)
        {
            QualType lhsTy = lhs.get().get().isPromotableBitField(context);
            if (lhsTy.isNull())
            {
                lhsTy = lhs.get().get().getType();
                if (context.isPromotableIntegerType(lhsTy))
                    lhsTy = context.getPromotedIntegerType(lhsTy);
            }
            compLHSTy.set(lhsTy);
        }
        return pExp.getType();
    }

    private QualType checkAdditionOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>>  rhs,
            SourceLocation opLoc)
    {
        return checkAdditionOperands(lhs, rhs, opLoc, null);
    }

    private boolean checkArithmeticOpPointerOperand(
            SourceLocation loc, 
            Expr lhs, 
            Expr rhs)
    {
        boolean isLHSPointer = lhs.getType().isPointerType();
        boolean isRHSPointer = rhs.getType().isPointerType();

        if (!isLHSPointer && !isRHSPointer) return true;

        QualType lhsPointeeTy = new QualType() , rhsPointeeTy = new QualType();
        if (isLHSPointer) lhsPointeeTy = lhs.getType().getPointeeType();
        if (isRHSPointer) rhsPointeeTy = rhs.getType().getPointeeType();

        // Check for arithmetic on pointers to incomplete types.
        boolean isLHSVoidPtr = isLHSPointer && lhsPointeeTy.isVoidType();
        boolean isRHSVoidPtr = isRHSPointer && rhsPointeeTy.isVoidType();
        if (isLHSVoidPtr || isRHSVoidPtr) {
            if (!isRHSVoidPtr) diagnoseArithmeticOnVoidPointer(loc, lhs);
            else if (!isLHSVoidPtr) diagnoseArithmeticOnVoidPointer(loc, rhs);
            else diagnoseArithmeticOnTwoVoidPointers(loc, lhs, rhs);

            return true;
        }
        boolean isLHSFuncPtr = isLHSPointer && lhsPointeeTy.isFunctionType();
        boolean isRHSFuncPtr = isRHSPointer && rhsPointeeTy.isFunctionType();

        if (isLHSFuncPtr || isRHSFuncPtr) {
            if (!isRHSFuncPtr) diagnoseArithmeticOnFunctionPointer(loc, lhs);
            else if (!isLHSFuncPtr) diagnoseArithmeticOnFunctionPointer(loc, rhs);
            else diagnoseArithmeticOnTwoFunctionPointers(loc, lhs, rhs);

            return true;
        }

        if (checkArithmeticIncompletePointerType(loc, lhs)) return false;
        return !checkArithmeticIncompletePointerType(loc, rhs);

    }

    private boolean checkArithmeticOpPointerOperand(
            SourceLocation loc, Expr operand)
    {
        if (!operand.getType().isPointerType())
            return true;

        QualType pointeeTy = operand.getType().getPointeeType();
        if (pointeeTy.isVoidType())
        {
            diag(loc, ext_gnu_void_ptr).emit();
            return true;
        }
        if (pointeeTy.isFunctionType())
        {
            diag(loc, ext_gnu_ptr_func_arith).
                    addTaggedVal(pointeeTy).
                    addSourceRange(operand.getSourceRange()).
                    emit();
            return true;
        }

        return !checkArithmeticIncompletePointerType(loc, operand);
    }

    private void diagnoseArithmeticOnVoidPointer(SourceLocation loc, Expr expr)
    {
        diag(loc, ext_gnu_void_ptr).emit();
    }

    private void diagnoseArithmeticOnTwoVoidPointers(SourceLocation loc, Expr lhs, Expr rhs)
    {
        diag(loc, ext_gnu_void_ptr).emit();
    }

    private void diagnoseArithmeticOnFunctionPointer(SourceLocation loc, Expr operand)
    {
        diag(loc, ext_gnu_ptr_func_arith).addTaggedVal(operand.getType()).
                addSourceRange(operand.getSourceRange()).emit();
    }

    private void diagnoseArithmeticOnTwoFunctionPointers(SourceLocation loc, Expr lhs, Expr rhs)
    {
        diag(loc, ext_gnu_ptr_func_arith).
                addTaggedVal(lhs.getType()).
                addTaggedVal(rhs.getType()).
                emit();
    }

    /**
     *  emit error if Operand is incomplete pointer type.
     * @return
     */
    private boolean checkArithmeticIncompletePointerType(
            SourceLocation loc,
            Expr op)
    {
        if (op.getType().isPointerType())
        {
            QualType pointeeTy = op.getType().getPointeeType();
            if (requireCompleteType(loc, pointeeTy,
                    pdiag(err_typecheck_arithmetic_incomplete_type).
                            addTaggedVal(pointeeTy).
                            addSourceRange(op.getSourceRange())));
                return true;
        }
        return false;
    }

    private boolean requireCompleteType(
            SourceLocation loc,
            QualType t,
            int diagID)
    {
        return requireCompleteType(loc, t, pdiag(diagID), Pair.get(new SourceLocation(), pdiag(0)));
    }

    private boolean requireCompleteType(
            SourceLocation loc,
            QualType t,
            PartialDiagnostic pdiag)
    {
        return requireCompleteType(loc, t, pdiag, Pair.get(new SourceLocation(), pdiag(0)));
    }

    /**
     * Ensure that the specified type is complete.
     * <br>
     * This routine checks whether the type {@code t} is complete in the any context
     * where complete type is required. If {@code t} is a complete type, returns
     * false. if failed, issues the diagnostic {@code diag} info and return true.
     * @param loc The location in the source code where the diagnostic message
     *            should refer.
     * @param t The type that this routine is examining for complete.
     * @return Return true if {@code t} is not a complete type, false otherwise.
     */
    private boolean requireCompleteType(
            SourceLocation loc,
            QualType t,
            PartialDiagnostic pdiag,
            Pair<SourceLocation, PartialDiagnostic> notes)
    {
        int diag = pdiag.getDiagID();

        // If we have a complete type, we're done.
        if (!t.isIncompleteType())
            return false;

        if (diag == 0)
            return true;

        // If we have a array type with constant getNumOfSubLoop, attempt to instantiate it.
        QualType elemType = t;
        ArrayType.ConstantArrayType array = context.getAsConstantArrayType(t);
        if (array != null)
            elemType = array.getElemType();

        final TagType tag = context.<TagType>getAs(elemType);
        // Avoids diagnostic invalid decls as incomplete.
        if (tag != null && tag.getDecl().isInvalidDecl())
            return true;

        // We have an incomplete type. Produce a diagnostic.
        diag(loc, pdiag).addTaggedVal(t).emit();

        if (notes.first.isValid())
            diag(notes.first, notes.second).emit();

        // If the type was a forward declaration of a class/struct/union
        // type, produce a note.
        if (tag != null && !tag.getDecl().isInvalidDecl())
            diag(tag.getDecl().getLocation(), 
                    tag.isBeingDefined() ? note_type_being_defined : note_forward_declaration).
                    addTaggedVal(new QualType(tag)).emit();
                    
        return true;
    }

    private void checkArrayAccess(Expr pExpr, Expr iExpr)
    {
        // TODO: 2017/4/8  
    }

    private void checkArrayAccess(final Expr e)
    {
        // TODO: 2017/4/8
    }

    private QualType checkSubtractionOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation opLoc)
    {
        return checkSubtractionOperands(lhs, rhs, opLoc, new OutParamWrapper<>());
    }

	/**
	 * Detect when a NULL constant is used improperly in an
     * expression.  These are mainly cases where the null pointer is used as an
     * integer instead of a pointer.
     * @param lhs
     * @param rhs
     * @param opLoc
     * @param isCompare
     */
    private void checkArithmeticNull(
            Sema sema,
            ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            SourceLocation opLoc,
            boolean isCompare)
    {
        // FIXME: 2017/4/8  this is not needed
    }

	/**
	 * Emit error when two pointers are incompatible.
     * @param sema
     * @param loc
     * @param lhs
     * @param rhs
     */
    private static void diagnosePointerIncompatibility(
            Sema sema,
            SourceLocation loc,
            Expr lhs, Expr rhs)
    {
        assert(lhs.getType().isPointerType());
        assert(rhs.getType().isPointerType());
        sema.diag(loc, err_typecheck_sub_ptr_compatible)
                .addTaggedVal(lhs.getType())
                .addTaggedVal(rhs.getType())
                .addSourceRange(lhs.getSourceRange())
                .addSourceRange(rhs.getSourceRange())
                .emit();
    }

    // C99 6.5.6
    private QualType checkSubtractionOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation opLoc,
            OutParamWrapper<QualType> compLHSTy)
    {
        checkArithmeticNull(this, lhs.get(), rhs.get(), opLoc, /*isCompare*/false);

        QualType compType = usualArithmeticConversions(lhs, rhs, compLHSTy != null);
        if (lhs.get().isInvalid() || rhs.get().isInvalid())
            return new QualType();

        // Handle the common case, both two operands are arithmetic type.
        if (lhs.get().get().getType().isArithmeticType()
                && rhs.get().get().getType().isArithmeticType())
        {
            if (compLHSTy!=null) compLHSTy.set(compType);
            return compType;
        }

        // Either ptr - int  or ptr - ptr.
        if (lhs.get().get().getType().isPointerType())
        {
            QualType lPointee = lhs.get().get().getType().getPointeeType();

            // The case is ptr - int.
            if (rhs.get().get().getType().isIntegerType())
            {
                if (!checkArithmeticOpPointerOperand(opLoc, lhs.get().get()))
                    return new QualType();

                Expr iExpr = rhs.get().get().ignoreParenCasts();
                UnaryExpr negRex = new UnaryExpr(iExpr, UO_Minus,
                        iExpr.getType(),
                        EVK_RValue,
                        iExpr.getExprLocation());
                checkArrayAccess(lhs.get().get().ignoreParenCasts(), negRex);
                if (compLHSTy != null) compLHSTy.set(lhs.get().get().getType());
                return lhs.get().get().getType();
            }

            // handle ptr - ptr case
            if (rhs.get().get().getType().isPointerType())
            {
                final PointerType rhsPtry = rhs.get().get().getType().getAsPointerType();
                QualType rpointee = rhsPtry.getPointeeType();

                // Pointee types must be compatible C99 6.5.6p3
                if (!context.isCompatible(lPointee, rpointee))
                {
                    diagnosePointerIncompatibility(this, opLoc, lhs.get().get(), rhs.get().get());
                    return new QualType();
                }

                if (!checkArithmeticOpPointerOperand(opLoc, lhs.get().get(), rhs.get().get()))
                    return new QualType();

                if (compLHSTy!= null) compLHSTy.set(lhs.get().get().getType());
                return context.getPointerDiffType();
            }
        }

        return invalidOperands(opLoc, lhs.get(), rhs.get());
    }

    private QualType checkShiftOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation opLoc,
            BinaryOperatorKind opc)

    {
        return checkShiftOperands(lhs, rhs, opLoc, opc, false);
    }

    private QualType checkShiftOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation opLoc,
            BinaryOperatorKind opc,
            boolean isCompAssign)
    {
        if (!lhs.get().get().getType().isIntegerType()
                || !rhs.get().get().getType().isIntegerType())
        {
            return invalidOperands(opLoc, lhs.get(), rhs.get());
        }

        // Shifts don't perform usual arithmetic conversions, they just do integer
        // promotions on each operand. C99 6.5.7p3

        // For the LHS, do usual unary conversions, but then reset them away
        // if this is a compound assignment.
        ActionResult<Expr> oldLHS = lhs.get();

        lhs.set(usualUnaryConversions(lhs.get().get()));
        if (lhs.get().isInvalid())
            return new QualType();

        QualType lhsType = lhs.get().get().getType();
        if (isCompAssign) lhs.set(oldLHS);

        // The rhs is simpler
        rhs.set(usualUnaryConversions(rhs.get().get()));
        if (rhs.get().isInvalid())
            return new QualType();

        // TODO DiagnoseBadShiftValues
        return lhsType;
    }

    private QualType checkComparisonOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation opLoc,
            BinaryOperatorKind opc,
            boolean isRelational)
    {
        // TODO: 2017/3/28
        return null;
    }

    private QualType checkBitwiseOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation opLoc)
    {
        return checkBitwiseOperands(lhs, rhs, opLoc, false);
    }

    private QualType checkBitwiseOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation opLoc,
            boolean isCompAssign)
    {
        // TODO: 2017/3/28
        return null;
    }

    private QualType checkLogicalOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation opLoc,
            BinaryOperatorKind opc)
    {
        // TODO: 2017/3/28
        return null;
    }

    private QualType checkCommaOperands(
            OutParamWrapper<ActionResult<Expr>> lhs,
            OutParamWrapper<ActionResult<Expr>> rhs,
            SourceLocation loc)
    {
        // TODO: 2017/4/9
        return null;
    }

    public static BinaryOperatorKind convertTokenKindToBinaryOpcode(TokenKind tokenKind)
    {
        BinaryOperatorKind opc = null;
        switch (tokenKind)
        {
            default:
                Util.shouldNotReachHere("Unknown binary operator token!");
            case star:          opc = BinaryOperatorKind.BO_Mul;break;
            case slash:         opc = BO_Div; break;
            case percent:       opc = BinaryOperatorKind.BO_Rem; break;
            case plus:          opc = BinaryOperatorKind.BO_Add; break;
            case sub:           opc = BinaryOperatorKind.BO_Sub;break;
            case lessless:          opc = BinaryOperatorKind.BO_Shl; break;
            case greatergreater:          opc = BinaryOperatorKind.BO_Shr; break;
            case lessequal:          opc = BinaryOperatorKind.BO_LE;break;
            case less:            opc = BinaryOperatorKind.BO_LT; break;
            case greaterequal:          opc = BinaryOperatorKind.BO_GE; break;
            case greater:            opc = BinaryOperatorKind.BO_GT; break;
            case bangequal:        opc = BinaryOperatorKind.BO_NE; break;
            case equal:            opc = BinaryOperatorKind.BO_EQ; break;
            case amp:           opc = BinaryOperatorKind.BO_And; break;
            case caret:         opc = BinaryOperatorKind.BO_Xor; break;
            case bar:           opc = BinaryOperatorKind.BO_Or; break;
            case ampamp:        opc = BinaryOperatorKind.BO_LAnd; break;
            case barbar:        opc = BinaryOperatorKind.BO_LOr;break;
            case equalequal:          opc = BinaryOperatorKind.BO_Assign; break;
            case starequal:        opc = BinaryOperatorKind.BO_MulAssign;break;
            case slashequal:       opc = BinaryOperatorKind.BO_DivAssign; break;
            case percentequal:     opc = BinaryOperatorKind.BO_RemAssign;break;
            case plusequal:        opc = BinaryOperatorKind.BO_AddAssign; break;
            case subequal:         opc = BinaryOperatorKind.BO_SubAssign;break;
            case lesslessequal:        opc = BinaryOperatorKind.BO_ShlAssign;break;
            case greatergreaterequal:        opc = BinaryOperatorKind.BO_ShrAssign; break;
            case ampequal:         opc = BinaryOperatorKind.BO_AndAssign; break;
            case caretequal:       opc = BinaryOperatorKind.BO_XorAssign;break;
            case barequal:         opc = BinaryOperatorKind.BO_OrAssign;break;
            case comma:         opc = BinaryOperatorKind.BO_Comma;break;
        }
        return opc;
    }

    public ActionResult<Expr> actOnCharacterConstant(Token tok)
    {
        assert tok.is(char_constant) : "Invalid character literal!";

        String charBuffer = pp.getSpelling(tok);
        CharLiteralParser literal = new CharLiteralParser(charBuffer,
                tok.getLocation(), pp);
        if (literal.hadError())
            return exprError();

        QualType ty = context.IntTy;

        return new ActionResult<>(new CharacterLiteral(
                (int)literal.getValue(),
                false,
                ty, tok.getLocation()));
    }

	/**
     * Allocates a array of type char for holding the string literal.
     * @param stringToks
     * @return
     */
    public ActionResult<Expr> actOnStringLiteral(ArrayList<Token> stringToks)
    {
        assert !stringToks.isEmpty():"string literal must have at least one char";

        Token[] arr = new Token[stringToks.size()];
        stringToks.toArray(arr);
        StringLiteralParser literal = new StringLiteralParser(arr, pp);
        if (literal.hadError)
            return exprError();

        ArrayList<SourceLocation> stringLocs = new ArrayList<>();
        stringToks.forEach(tok->stringLocs.add(tok.getLocation()));

        QualType strTy = context.CharTy;

        strTy = context.getConstantType(strTy,
                new APInt(32, literal.getNumStringChars() +1),
                ArraySizeModifier.Normal, 0);

        return new ActionResult<Expr>(StringLiteral.create(literal.getString(),
                false, strTy, stringLocs));
    }

    private UnaryOperatorKind convertTokenKindToUnaryOperator(TokenKind kind)
    {
        switch (kind)
        {
            default:
                Util.shouldNotReachHere("Unknown unary operator!");
                return null;
            case plusplus:
                return UO_PreInc;
            case subsub:
                return UO_PreDec;
            case amp:
                return UO_AddrOf;
            case star:
                return UO_Deref;
            case plus:
                return UO_Plus;
            case sub:
                return UO_Minus;
            case tilde:
                return UO_Not;
            case bang:
                return UO_LNot;
        }
    }

    /**
     * Unary operators.
     *
     * @param opLoc The location of unary operator.
     * @param tokenKind The token kind for unary operators.
     * @param inputExpr The input expression.
     * @return
     */
    public ActionResult<Expr> actOnUnaryOp(
            SourceLocation opLoc,
            TokenKind tokenKind,
            Expr inputExpr)
    {
        UnaryOperatorKind opc = convertTokenKindToUnaryOperator(tokenKind);
        return createUnaryOp(opLoc, opc, inputExpr);
    }

    /**
     * Type check unary operator (prefix '*').
     * @param op
     * @param vk
     * @param opLoc
     * @return
     */
    private QualType checkIndirectOperand(Expr op,
            OutParamWrapper<ExprValueKind> vk,
            SourceLocation opLoc)
    {
        ActionResult<Expr> convRes = usualUnaryConversions(op);
        if (convRes.isInvalid())
            return new QualType();

        op = convRes.get();
        QualType opTy = op.getType();
        QualType result = new QualType();

        // Note that per both C89 and C99, indirection is always legal, even if OpTy
        // is an incomplete type or void.  It would be possible to warn about
        // dereferencing a void pointer, but it's completely well-defined, and such a
        // warning is unlikely to catch any mistakes.
        PointerType pt = context.<PointerType>getAs(opTy);
        if (pt != null)
            result = pt.getPointeeType();
        else
        {
            // TODO: 2017/4/8
        }
        
        if (result.isNull())
        {
            diag(opLoc, err_typecheck_indirection_requires_pointer)
                    .addTaggedVal(opTy).addSourceRange(op.getSourceRange())
                    .emit();
            return new QualType();
        }

        // Dereferences are usually l-values...
        vk.set(EVK_LValue);

        // Except that certain expressions are never l-values in C.
        if (result.isCForbiddenLVaue())
            vk.set(EVK_RValue);
        return result;
    }

    private QualType checkInrementDecrementOperand(
            Expr op,
            OutParamWrapper<ExprValueKind> vk,
            SourceLocation opLoc,
            boolean isIncre,
            boolean isPrefix)
    {
        QualType resType = op.getType();
        assert !resType.isNull():"no type for increment/decrement!";

        if (resType.isRealType())
        {
            // OK!
        }
        else if (resType.isPointerType())
        {
            // C99 6.5.2.4p2, 6.5.6p2
            if (!checkArithmeticOpPointerOperand(opLoc, op))
                return new QualType();

            // Diagnose bad cases.
        }
        else if (resType.isComplexType())
        {
            // C99 does not support ++/-- on complex types, we allow as an extension.
            diag(opLoc, ext_integer_increment_complex).addTaggedVal(resType)
                    .addSourceRange(op.getSourceRange())
                    .emit();
        }
        else
        {
            diag(opLoc, err_typecheck_illegal_increment_decrement).
                    addTaggedVal(resType).
                    addTaggedVal(isIncre?1:0).
                    addSourceRange(op.getSourceRange()).
                    emit();
            return new QualType();
        }

        // At this point, we know we have a real, complex or pointer type.
        // Now construct sure the operand is a modifiable lvalue.
        if (checkForModifiableLvalue(op, opLoc))
            return new QualType();

        // a prefix increment/decrement is a Lvalue.
        if (isPrefix)
        {
            vk.set(EVK_LValue);
            return resType;
        }
        else
        {
            vk.set(EVK_RValue);
            return resType.clearQualified();
        }
    }

    /**
     * Verify that E is a modifiable lvalue. If not, emit an error and
     * return true.  If so, return false.
     * @param e
     * @param oploc
     * @return
     */
    private boolean checkForModifiableLvalue(Expr e, SourceLocation oploc)
    {
        // C99 6.3.2.1: an lvalue that does not have array type,
        // does not have an incomplete type, does not have a const-qualified type,
        // and if it is a structure or union, does not have any member (including,
        // recursively, any member or element of all contained aggregates or unions)
        // with a const-qualified type.
        SourceLocation originLoc = oploc;
        OutParamWrapper<SourceLocation> x = new OutParamWrapper<>(oploc);
        Expr.IsModifiableLvalueResult res = e.isModifiableLvalue(context, x);

        int diag = 0;
        boolean needType = false;
        switch (res)
        {
            case MLV_ConstQualified:
                diag = err_typecheck_assign_const;
                break;
            case MLV_ArrayType:
                diag = err_typecheck_array_not_modifiable_lvalue;
                needType = true;
                break;
            case MLV_NotObjectType:
                diag = err_typecheck_non_object_not_modifiable_lvalue;
                needType = true;
                break;
            case MLV_LValueCast:
                diag = err_typecheck_lvalue_casts_not_supported;
                break;
            case MLV_Valid:
                Util.shouldNotReachHere("did not take early return for MLV_Valid");
            case MLV_InvalidExpression:
                diag = err_typecheck_expression_not_modifiable_lvalue;
                break;
            case MLV_IncompleteType:
            case MLV_IncompleteVoidType:
                return requireCompleteType(oploc, e.getType(),
                        pdiag(err_typecheck_incomplete_type_not_modifiable_lvalue).
                                addSourceRange(e.getSourceRange()));
        }

        SourceRange assign = new SourceRange();
        if (!oploc.equals(originLoc))
            assign = new SourceRange(originLoc, originLoc);
        if (needType)
            diag(oploc, diag).addTaggedVal(e.getType()).
                    addSourceRange(e.getSourceRange()).
                    addSourceRange(assign).
                    emit();
        else
            diag(oploc, diag).addSourceRange(e.getSourceRange()).
                    addSourceRange(assign).emit();
        return true;
    }

    /**
     * The operand of & must be either a function designator or
     * an lvalue designating an object. If it is an lvalue, the object cannot be
     * declared with storage class register or be a bit field.
     * Note: The usual conversions are <b>not</b> applied to the operand of the
     * & operator (C99 6.3.2.1P[2-4]), and it result is never an lvalue.
     * @param origOp
     * @param opLoc
     * @return
     */
    private QualType checkAddressOfOperand(Expr origOp, SourceLocation opLoc)
    {
        // Make sure to ignore parentheses in subsequnet checks
        Expr op = origOp.ignoreParens();

        // Implement C99-only parts of addressof rules.
        if (op instanceof UnaryExpr)
        {
            UnaryExpr uOp = (UnaryExpr)op;
            if (uOp.getOpCode() == UO_Deref)
            {
                // C99 6.5.3.2, the address of a deref always returns
                // a valid result (assuming the deref expression is valid).
                return uOp.getSubExpr().getType();
            }
        }
        // TODO complete checkAddressOfOperand()
        // LLVM SemaExpr.clex:7409.
        return new QualType();
    }

    public ActionResult<QualType> actOnTypeName(Scope s, Declarator d)
    {
        // C99 6.7.6: Type names have no identifier.  This is already validated by
        // the jlang.parser.
        assert d.getIdentifier() == null:"Type must have no identifier!";
        // TODO
        return null;
    }

    public ActionResult<Expr> actOnCastOfParenListExpr(
            Scope s,
            SourceLocation lparenLoc,
            SourceLocation rparenLoc,
            Expr expr,
            QualType castTy)
    {
        ParenListExpr pe = (ParenListExpr)expr;

        expr = maybeConvertParenListExprToParenExpr(expr).get();
        return actOnCastExpr(s, lparenLoc, castTy, rparenLoc, expr);
    }

	/**
     * Check type constraints for casting between types.
     * @param range
     * @param castTy
     * @param expr
     * @param kind
     * @return
     */
    private boolean checkCastTypes(
            SourceRange range,
            QualType castTy,
            Expr expr,
            OutParamWrapper<CastKind> kind)
    {
        expr = defaultFunctionArrayConversion(expr).get();

        // C99 6.5.4p2: the cast type needs to be void or scalar and the expression
        // type needs to be scalar.
        if (castTy.isVoidType())
        {
            // Cast to void allows any expr type.
        }
        else if (!castTy.isScalarType())
        {
            if (castTy.getType().getCanonicalTypeInternal().getUnQualifiedType().
                    equals(expr.getType().getUnQualifiedType().getType().getCanonicalTypeInternal())
                    && (castTy.isStructureType() || castTy.isUnionType()))
            {
                // GCC struct/union extension: allow cast to self.
                diag(range.getBegin(), ext_typecheck_cast_nonscalar)
                        .addTaggedVal(castTy)
                        .addSourceRange(expr.getSourceRange())
                        .emit();
                kind.set(CK_NoOp);
            }
            else if (castTy.isUnionType())
            {
                // GCC cast to union extension
                RecordDecl rd = ((RecordType)castTy.getType()).getDecl();
                int i = 0, e = rd.getNumFields();
                for (; i < e; i++)
                {
                    FieldDecl fd = rd.getDeclAt(i);
                    if (fd.getDeclType().getType().getCanonicalTypeInternal().getUnQualifiedType().
                            equals(expr.getType().getType().getCanonicalTypeInternal().getUnQualifiedType()))
                    {
                        diag(range.getBegin(), ext_typecheck_cast_to_union)
                                .addSourceRange(expr.getSourceRange())
                                .emit();
                        break;
                    }
                }
                if (i == e)
                {
                    diag(range.getBegin(), err_typecheck_cast_to_union_no_type)
                            .addTaggedVal(expr.getType())
                            .addSourceRange(expr.getSourceRange())
                            .emit();
                    return true;
                }
                kind.set(CK_ToUnion);
            }
            else
            {
                // Reject any other conversions to non-scalar types.
                diag(range.getBegin(), err_typecheck_cond_expect_scalar)
                    .addTaggedVal(castTy)
                    .addSourceRange(expr.getSourceRange())
                    .emit();
                return true;
            }
        }
        else if (!expr.getType().isScalarType())
        {
            diag(expr.getLocStart(),err_typecheck_expect_scalar_operand)
                    .addTaggedVal(expr.getType())
                    .addSourceRange(expr.getSourceRange())
                    .emit();
            return true;
        }
        else if (!castTy.isArithmeticType())
        {
            QualType castExprType = expr.getType();
            if (castExprType.isIntegerType() && castExprType.isArithmeticType())
            {
                diag(expr.getLocStart(),
                        err_cast_pointer_from_non_pointer_int)
                        .addTaggedVal(castExprType)
                        .addSourceRange(expr.getSourceRange())
                        .emit();
                return true;
            }
        }
        else if (!expr.getType().isArithmeticType())
        {
            if (!castTy.isIntegerType() && castTy.isArithmeticType())
            {
                diag(expr.getLocStart(),
                        err_cast_pointer_to_non_pointer_int)
                        .addTaggedVal(castTy)
                        .addSourceRange(expr.getSourceRange())
                        .emit();
                return true;
            }
        }
        return false;
    }

    public ActionResult<Expr> actOnCastExpr(
            Scope s,
            SourceLocation lParenLoc,
            QualType castTy,
            SourceLocation rParenLoc,
            Expr castExpr)
    {
        CastKind kind = CastKind.CK_Invalid;

        assert castTy != null && castExpr != null :
                "actOnCastExpr(): missing type or castExpr";
        if (castExpr instanceof ParenListExpr)
            return actOnCastOfParenListExpr(s, lParenLoc, rParenLoc, castExpr, castTy);
        OutParamWrapper<CastKind> x = new OutParamWrapper<>(kind);
        if (checkCastTypes(new SourceRange(lParenLoc, rParenLoc), castTy, castExpr, x))
            return exprError();

        kind = x.get();
        return new ActionResult<>(new ExplicitCastExpr(castTy,
                castExpr, kind,
                lParenLoc, rParenLoc));
    }

    public ActionResult<Expr> actOnParenOrParenList(
            SourceLocation lParenLoc,
            SourceLocation rParenLoc,
            ArrayList<Expr> exprs)
    {
        assert exprs!=null&& !exprs.isEmpty()
                : "actOnParenOrParenList missing expression list!";

        Expr res = null;
        int size = exprs.size();
        if (size == 1)
            res = new ParenExpr(exprs.get(0), lParenLoc, rParenLoc);
        else
            res = new ParenListExpr(lParenLoc, exprs, rParenLoc, exprs.get(size - 1).getType());

        return new ActionResult<>(res);

    }

    public ActionResult<Expr> actOnParenExpr(
            SourceLocation lParenLoc,
            SourceLocation rParenLoc,
            Expr expr)
    {
        assert expr != null:"actOnParenExpr() missing expression.";

        return new ActionResult<>(new ParenExpr(expr, lParenLoc,rParenLoc));
    }

    public ActionResult<Expr> actOnArraySubscriptExpr(
            Expr base,
            SourceLocation lParenLoc,
            Expr idx,
            SourceLocation rParenLoc)
    {
        // Since this might be a postfix expression, get rid of ParenListExprs.
        ActionResult<Expr> res = maybeConvertParenListExprToParenExpr(base);

        if (res.isInvalid())
            return exprError();
        base = res.get();

        Expr lhsExpr = base;
        Expr rhsExpr = idx;

        // perform default conversion
        res = defaultFunctionArrayLValueConversion(lhsExpr);
        if (res.isInvalid()) return exprError();
        lhsExpr = res.get();

        res = defaultFunctionArrayLValueConversion(rhsExpr);
        if (res.isInvalid()) return exprError();
        rhsExpr = res.get();

        QualType lhsTy = lhsExpr.getType(), rhsTy = rhsExpr.getType();
        ExprValueKind vk = EVK_RValue;

        // C99 6.5.2.1p2: the expression e1[e2] is by definition precisely equivalent
        // to the expression *((e1)+(e2)). This means the array "Base" may actually be
        // in the subscript position. As a result, we need to derive the array base
        // and index from the expression types.

        // Following code was intended to take semantics checking on baseExpr and
        // indexExpr. it is not worked in creating ArraySubscriptExpr.
        Expr baseExpr, idxExpr;
        QualType resultTy;
        if (lhsTy.isPointerType())
        {
            baseExpr = lhsExpr;
            idxExpr = rhsExpr;
            resultTy = lhsTy.getAsPointerType().getPointeeType();
        }
        else if (rhsTy.isPointerType())
        {
            // handle the uncommon case of "123[Ptr]".
            baseExpr = rhsExpr;
            idxExpr = lhsExpr;
            resultTy = rhsTy.getAsPointerType().getPointeeType();
        }
        else if (lhsTy.isArrayType())
        {
            // If we see an array that wasn't promoted by
            // DefaultFunctionArrayLvalueConversion, it must be an array that
            // wasn't promoted because of the C90 rule that doesn't
            // allow promoting non-lvalue arrays.  Warn, then
            // force the promotion here.
            diag(lhsExpr.getLocStart(), ext_subscript_non_lvalue)
                    .addSourceRange(lhsExpr.getSourceRange()).emit();
            lhsExpr = implicitCastExprToType(lhsExpr,
                    context.getArrayDecayedType(lhsTy),
                    EVK_RValue,
                    CK_ArrayToPointerDecay).get();
            lhsTy = lhsExpr.getType();
            baseExpr = lhsExpr;
            idxExpr = rhsExpr;

            resultTy = lhsTy.getAsPointerType().getPointeeType();
        }
        else if(rhsTy.isArrayType())
        {
            // Same as previous, except for 123[f().a] case
            diag(rhsExpr.getLocStart(), ext_subscript_non_lvalue).
                    addSourceRange(rhsExpr.getSourceRange()).emit();
            rhsExpr = implicitCastExprToType(rhsExpr,
                    context.getArrayDecayedType(rhsTy),
                    EVK_RValue,
                    CK_ArrayToPointerDecay).get();
            rhsTy = rhsExpr.getType();

            baseExpr = rhsExpr;
            idxExpr = lhsExpr;
            resultTy = rhsTy.getAsPointerType().getPointeeType();
        }
        else
        {
            diag(lParenLoc, err_typecheck_subscript_value)
                    .addSourceRange(lhsExpr.getSourceRange())
                    .addSourceRange(rhsExpr.getSourceRange())
                    .emit();
            return exprError();
        }

        // C99 6.5.2.1p1
        if (!idxExpr.getType().isIntegerType())
        {
            diag(lParenLoc, err_typecheck_subscript_not_integer).emit();
            return exprError();
        }

        if (context.isSpecifiedBuiltinType(idxExpr.getType(), TypeClass.UnsignedChar)
                || context.isSpecifiedBuiltinType(idxExpr.getType(), TypeClass.Char))
        {
            diag(lParenLoc, warn_subscript_is_char)
                    .addSourceRange(idxExpr.getSourceRange())
                    .emit();
        }

        // C99 6.5.2.1p1: "shall have type "pointer to *object* type".
        if (resultTy.isFunctionType())
        {
            diag(baseExpr.getLocStart(), err_subscript_function_type)
                    .addTaggedVal(resultTy)
                    .addSourceRange(baseExpr.getSourceRange())
                    .emit();
            return exprError();
        }

        if (resultTy.isVoidType())
        {
            // GNU extension: subscripting on pointer to void.
            diag(lParenLoc, ext_gnu_subscript_void_type)
                    .addSourceRange(baseExpr.getSourceRange())
                    .emit();

            // C forbids expressions of unqualified void type from being l-values.
            // See IsCForbiddenLValueType.
            if (!resultTy.hasQualifiers())
                vk = EVK_RValue;
        }
        else if (requireCompleteType(lParenLoc, resultTy,
                pdiag(err_subscript_incomplete_type).
                        addSourceRange(baseExpr.getSourceRange())))
        {
            return exprError();
        }

        // Checking end!!!

        return new ActionResult<>(new ArraySubscriptExpr(lhsExpr, rhsExpr,
                resultTy, vk, rParenLoc));
    }

    private ActionResult<Expr> maybeConvertParenListExprToParenExpr(Expr e)
    {
        if (!(e instanceof ParenListExpr))
            return new ActionResult<>(e);

        ParenListExpr ex = (ParenListExpr)e;
        ActionResult<Expr> res = new ActionResult<>(ex.getExpr(0));
        for (int i = 0, size = ex.getNumExprs();!res.isInvalid()&&i < size; ++i)
            res = actOnBinOp(ex.getExprLoc(), TokenKind.comma, res.get(), ex.getExpr(i));

        if (res.isInvalid())
            return exprError();
        return res;
    }

    /**
     * Handle a call to a function with the specified array of arguments.
     * @param fn
     * @param lParenLoc
     * @param args
     * @param rParenLoc
     * @return
     */
    public ActionResult<Expr> actOnCallExpr(
            Expr fn,
            SourceLocation lParenLoc,
            ArrayList<Expr> args,
            SourceLocation rParenLoc)
    {
        // Since this might be a postfix expression, get rid of ParenListExprs.
        ActionResult<Expr> result = maybeConvertParenListExprToParenExpr(fn);
        if (result.isInvalid())
            return exprError();
        fn = result.get();

        // Only the direct calling a function will be handled.
        // get the appropriate declaration of function.
        Expr nakedFn = fn.ignoreParens();
        NamedDecl namedDecl = null;
        if (nakedFn instanceof UnaryExpr)
        {
            UnaryExpr uop = (UnaryExpr)nakedFn;
            if (uop.getOpCode() == UO_AddrOf)
                nakedFn = uop.getSubExpr().ignoreParens();
        }

        if (nakedFn instanceof DeclRefExpr)
            namedDecl = ((DeclRefExpr)nakedFn).getDecl();
        else if (nakedFn instanceof MemberExpr)
            namedDecl = ((MemberExpr)nakedFn).getMemberDecl();

        return buildResolvedCallExpr(nakedFn, namedDecl, lParenLoc, args, rParenLoc);
    }

    // Check for a valid return type
    private boolean checkCallReturnType(
            QualType returnType,
            SourceLocation loc,
            CallExpr ce,
            FunctionDecl fd)
    {
        if (returnType.isVoidType() || !returnType.isIncompleteType())
            return false;

        PartialDiagnostic note = fd != null ?
                pdiag(note_function_with_incomplete_return_type_declared_here)
                .addTaggedVal(fd.getDeclName().getName())
                : pdiag(0);
        SourceLocation noteLoc = fd != null ? fd.getLocation() : new SourceLocation();

        return requireCompleteType(loc, returnType, fd != null ?
                pdiag(err_call_function_incomplete_return)
                        .addSourceRange(ce.getSourceRange())
                        .addTaggedVal(fd.getDeclName().getName())
                : pdiag(err_call_incomplete_return).
                    addSourceRange(ce.getSourceRange()), Pair.get(noteLoc, note));
    }


	/**
     * Converts the arguments specified in args to the parameter types of the
     * function fnDecl with function prototype proto.
     * call is the call expression itself, and fn is the function expression.
     * @param call
     * @param fn
     * @param fndecl
     * @param proto
     * @param args
     * @param rparenLoc
     * @return
	 */
    private boolean convertArgumentsForCall(CallExpr call,
            Expr fn,
            FunctionDecl fndecl,
            FunctionProtoType proto,
            ArrayList<Expr> args,
            SourceLocation rparenLoc)
    {
        // C99 6.5.2.2p7 - the arguments are implicitly converted, as if by
        // assignment, to the types of the corresponding parameter, ...
        int numArgsInProto = proto.getNumArgs();
        boolean invalid = false;
        int fnKind = 0;

        // If too few arguments are available (and we don't have default
        // arguments for the remaining parameters), don't make the call.
        int numArgs = args.size();
        if (numArgs < numArgsInProto)
        {
            diag(rparenLoc,
                    err_typecheck_call_too_few_args).
                    addTaggedVal(fnKind).
                    addTaggedVal(numArgs).
                    addSourceRange(fn.getSourceRange()).
                    emit();

            // Emit the location of the prototype.
            if (fndecl != null)
            {
                diag(fndecl.getLocation(), note_callee_decl).
                        addTaggedVal(fndecl.getDeclName()).
                        emit();
            }
            call.setNumArgs(numArgsInProto);
            return true;
        }

        // If too many are passed and not variadic, error on the extras and drop
        // them.
        if (numArgs > numArgsInProto)
        {
            if (!proto.isVariadic())
            {
                diag(args.get(numArgsInProto-1).getLocStart(), err_typecheck_call_too_many_args)
                        .addTaggedVal(fnKind).addTaggedVal(numArgsInProto)
                        .addTaggedVal(numArgs).addSourceRange(fn.getSourceRange())
                        .addSourceRange(new SourceRange(args.get(numArgsInProto-1).getLocStart(),
                                args.get(numArgs - 1).getLocEnd()))
                        .emit();

                // Emit the location of the prototype.
                if (fndecl != null)
                {
                    diag(fndecl.getLocation(), note_callee_decl).
                            addTaggedVal(fndecl.getDeclName()).
                            emit();
                }

                // This drop off the extra arguments.
                call.setNumArgs(numArgsInProto);
                return true;
            }
        }
        ArrayList<Expr> allArgs = new ArrayList<>();
        VariadicCallType callType = proto.isVariadic() ?
                VariadicCallType.VariadicFunction
                : VariadicCallType.VariadicDoesNotApply;
        invalid = gatherArgumentsForCall(call.getLocStart(), fndecl,
                proto, 0, args, allArgs, callType);

        if (invalid)
            return true;

        int totalNumArgs = allArgs.size();
        for (int i = 0; i < totalNumArgs; ++i)
            call.setArgAt(i, allArgs.get(i));

        return false;
    }

    private boolean gatherArgumentsForCall(
            SourceLocation callLoc,
            FunctionDecl fnDecl,
            FunctionProtoType proto,
            int firstProtoArg,
            ArrayList<Expr> args,
            ArrayList<Expr> allArgs)
    {
        return gatherArgumentsForCall(callLoc, fnDecl, proto,
                firstProtoArg, args, allArgs,
                VariadicCallType.VariadicDoesNotApply);
    }

	/**
	 * (C99 6.5.2.2p6). Used for function calls that do not have a prototype.
     * Arguments that have type float are promoted to double.
     *
     * All other argument types are converted by UsualUnaryConversions().
     * @param e
     * @return
     */
    private ActionResult<Expr> defaultArgumentPromotion(Expr e)
    {
        QualType ty = e.getType();

        assert !ty.isNull() :"defaultArgumentPromotion - missing type";

        ActionResult<Expr> res = usualUnaryConversions(e);
        if (res.isInvalid())
            return new ActionResult<>(e);
        e = res.get();

        // If this is a 'float' (CVR qualified or typedef) promote to double.
        if (context.isSpecifiedBuiltinType(ty, TypeClass.Float))
            e = implicitCastExprToType(e, context.DoubleTy, EVK_RValue, CK_FloatingCast).get();
        return new ActionResult<>(e);
    }

	/**
     * Collector argument expressions for various form of call prototypes.
     * @return
     */
    private boolean gatherArgumentsForCall(
            SourceLocation callLoc,
            FunctionDecl fnDecl,
            FunctionProtoType proto,
            int firstProtoArg,
            ArrayList<Expr> args,
            ArrayList<Expr> allArgs,
            VariadicCallType callType)
    {
        int numArgInProto = proto.getNumArgs();
        int numArgsToCheck = args.size();

        boolean invalid = false;
        if (numArgsToCheck != numArgInProto)
            // Use default arguments for missing arguments
            numArgsToCheck = numArgInProto;

        int argIdx = 0;
        for (int i = firstProtoArg; i < numArgsToCheck; ++i)
        {
            QualType protoArgType = proto.getArgType(i);

            Expr arg = null;
            if (argIdx < numArgsToCheck)
            {
                arg = args.get(argIdx++);

                if (requireCompleteType(arg.getLocStart(),
                        protoArgType,
                        pdiag(err_call_incomplete_argument)
                            .addSourceRange(arg.getSourceRange())))
                {
                    return true;
                }

                // Pass the argument
                ParamVarDecl param = null;
                if (fnDecl != null && i < fnDecl.getNumParams())
                    param = fnDecl.getParamDecl(i);

	            /**
                 * TODO
                 *    InitializedEntity Entity =
                 Param? InitializedEntity::InitializeParameter(Context, Param)
                 : InitializedEntity::InitializeParameter(Context, ProtoArgType,
                 Proto.isArgConsumed(i));
                 ExprResult ArgE = PerformCopyInitialization(Entity,
                 SourceLocation(),
                 Owned(Arg));
                 if (ArgE.isInvalid())
                 return true;

                 Arg = ArgE.takeAs<Expr>();
                 */
            }
            checkArrayAccess(arg);

            allArgs.add(arg);
        }

        // If this is a variadic call, handle args passed through "...".
        if (callType != VariadicCallType.VariadicDoesNotApply)
        {
            // Do argument promotion, (C99 6.5.2.2p7).
            for (int i = argIdx; i < numArgsToCheck; i++)
            {
                ActionResult<Expr> arg = defaultArgumentPromotion(args.get(i));

                invalid |= arg.isInvalid();
                allArgs.add(arg.get());
            }

            // Check for array bounds violations.
            for (int i = argIdx; i != numArgsToCheck; ++i)
                checkArrayAccess(args.get(i));
        }
        return invalid;
    }

	/**
     * Check a direct function call for various correctness
     * and safety properties not strictly enforced by the C type system.
     * @param fnDecl
     * @param ce
     * @return
     */
    private boolean checkFunctionCall(FunctionDecl fnDecl, CallExpr ce)
    {
        return false;
    }

    /**
     * Build a call to a resolved expression, i.e. an expression not of
     * OverloadTy.  The expression should unary-convert to an expression of
     * function-pointer.
     *
     * @param fn
     * @param ndecl The declaration being called, if available
     * @param lParenLoc
     * @param args
     * @param rParenLoc
     * @return
     */
    private ActionResult<Expr> buildResolvedCallExpr(
            Expr fn,
            NamedDecl ndecl,
            SourceLocation lParenLoc,
            ArrayList<Expr> args,
            SourceLocation rParenLoc)
    {
        FunctionDecl fnDecl = (ndecl instanceof FunctionDecl)
                ? (FunctionDecl)ndecl : null;

        // Promote the function operand.
        ActionResult<Expr> res = usualUnaryConversions(fn);
        if (res.isInvalid())
            return exprError();

        fn = res.get();

        CallExpr call = new CallExpr(fn, args, context.BoolTy, EVK_RValue, rParenLoc);

        FunctionType funcTy;
        if (fn.getType().isPointerType())
        {
            // C99 6.5.2.2p1 - "The expression that denotes the called function shall
            // have type pointer to function".
            PointerType pt = fn.getType().getAsPointerType();
            funcTy = pt.getPointeeType().getAsFunctionType();

            if (funcTy == null)
            {
                diag(lParenLoc, err_typecheck_call_not_function).
                        addTaggedVal(fn.getType()).
                        addSourceRange(fn.getSourceRange())
                        .emit();
                return exprError();
            }
        }
        else
        {
            // Handle calls to expressions of unknown-any type.
            diag(lParenLoc, err_typecheck_call_not_function).
                    addTaggedVal(fn.getType()).
                    addSourceRange(fn.getSourceRange()).
                    emit();
            return exprError();
        }

        // check for a valid return type.
        if (checkCallReturnType(funcTy.getResultType(),
                fn.getSourceRange().getBegin(),
                call, fnDecl))
        {
            return exprError();
        }

        // We know the result type of call, set it.
        call.setType(funcTy.getCallReturnType(context));
        call.setValueKind(EVK_RValue);

        FunctionProtoType proto = null;
        if (funcTy instanceof FunctionProtoType)
        {
            proto = (FunctionProtoType)funcTy;
            if (convertArgumentsForCall(call, fn, fnDecl, proto, args, rParenLoc))
                return exprError();
        }
        else
        {
            if (fnDecl != null)
            {
                // Promote the argument (C99 6.5.2.2p6).
                for (int i = 0, e = args.size(); i < e; i++)
                {
                    Expr arg = args.get(i);
                    if (proto != null && i < proto.getNumArgs())
                    {
                        // TODO: 2017/4/9 initialize the arguments
                    }
                    else
                    {
                        ActionResult<Expr> argE = defaultArgumentPromotion(arg);

                        if (argE.isInvalid())
                            return exprError();

                        arg = argE.get();
                    }

                    if (requireCompleteType(arg.getLocStart(),
                                            arg.getType(),
                            pdiag(err_call_incomplete_argument)
                            .addSourceRange(arg.getSourceRange())))
                    {
                        return exprError();
                    }
                    call.setArgAt(i, arg);
                }
            }
        }

        // TODO Check for sentinels
        //if (NDecl)
        //    DiagnoseSentinelCalls(NDecl, LParenLoc, Args, NumArgs);

        // Do special checking on direct calls to function.
        if (fnDecl != null)
        {
            if (checkFunctionCall(fnDecl, call))
                return exprError();
        }

        return new ActionResult<>(call);
    }

    /**
     * This method called when parser encounter member access expression like
     *   expression.identifier
     *   expression->identifier
     * @param s
     * @param base The base expression.
     * @param opLoc The source location of '.' or '->' token.
     * @param opKind Indicates which the access operator is, either arrow or period.
     * @param name The member name being accessed.
     * @return
     */
    public ActionResult<Expr> actOnMemberAccessExpr(
            Scope s,
            Expr base,
            SourceLocation opLoc,
            TokenKind opKind,
            String name)
    {
        boolean isArrow = opKind == arrow;
        // This is a postfix expression, so get rid of ParenListExprs.
        ActionResult<Expr> result = maybeConvertParenListExprToParenExpr(base);
        if (result.isInvalid()) return exprError();
        base = result.get();

        LookupResult res = new LookupResult(this, name, opLoc, LookupMemberName);
        ActionResult<Expr> baseResult = new ActionResult<>(base);
        OutParamWrapper<ActionResult<Expr>> x = new OutParamWrapper<>(baseResult);
        result = lookupMemberExpr(res, x, isArrow, opLoc);
        baseResult = x.get();

        if (baseResult.isInvalid() || result.isInvalid())
            return exprError();
        base = baseResult.get();

        return buildMemberReferenceExpr(base, base.getType(), opLoc, isArrow, res);
    }

	/**
     * Build a MemberExpr AST node.
     * @return
     */
    private MemberExpr buildMemberExpr(
            Expr base,
            boolean isArrow,
            ValueDecl member,
            QualType type,
            ExprValueKind evk,
            SourceLocation loc,
            ExprObjectKind ok)
    {
        return new MemberExpr(base, isArrow, member, type, evk, loc, ok);
    }

    private ActionResult<Expr> buildFieldReferenceExpr(
            Expr baseEpxr,
            boolean isArrow,
            FieldDecl field)
    {
        ExprValueKind vk = EVK_LValue;
        ExprObjectKind ok = OK_Ordinary;

        if (!isArrow)
        {
            if (baseEpxr.getObjectKind() == OK_Ordinary)
                vk = baseEpxr.getValueKind();
            else
                vk = EVK_RValue;
        }
        if (vk != EVK_RValue && field.isBitField())
        {
            ok = OK_BitField;
        }

        // Figure out the type of the member; see C99 6.5.2.3p3
        QualType memberType = field.getDeclType();
        QualType baseType = baseEpxr.getType();
        if (isArrow) baseType = context.<PointerType>getAs(baseType).getPointeeType();

        QualType.Qualifier baseQuals = baseType.getQualifiers();
        QualType.Qualifier memberQuals = memberType.getType().getCanonicalTypeInternal().getQualifiers();
        QualType.Qualifier combined = baseQuals.add(memberQuals);

        if (!combined.equals(memberQuals))
        {
            memberType = context.getQualifiedType(memberType, combined);
        }

        return new ActionResult<>(buildMemberExpr(baseEpxr, isArrow, field,
                memberType, vk, baseEpxr.getLocStart(), ok));
    }

    private ActionResult<Expr> buildMemberReferenceExpr(
            Expr baseExpr,
            QualType baseExprType,
            SourceLocation opLoc,
            boolean isArrow,
            LookupResult lookupResult)
    {
        QualType baseType = baseExprType;
        if (isArrow)
        {
            assert baseType.isPointerType();
            baseType = context.<PointerType>getAs(baseType).getPointeeType();
        }
        if (lookupResult.isAmbiguous())
            return exprError();
        String memberName = lookupResult.getLookupName();
        SourceLocation memberLoc = lookupResult.getNameLoc();

        if (lookupResult.isEmpty())
        {
            NamedDecl dc = context.<RecordType>getAs(baseType).getDecl();
            diag(lookupResult.getNameLoc(), err_no_member)
            .addTaggedVal(memberName)
            .addTaggedVal(dc.getDeclName())
            .addSourceRange(baseExpr != null ? baseExpr.getSourceRange():new SourceRange())
            .emit();

            return exprError();
        }

        assert lookupResult.isSingleResult();
        NamedDecl memberDecl = lookupResult.getFoundDecl();

        if (memberDecl.isInvalidDecl())
            return exprError();

        if (memberDecl instanceof FieldDecl)
        {
            FieldDecl fd = (FieldDecl)memberDecl;

            return buildFieldReferenceExpr(baseExpr, isArrow, fd);
        }

        if (memberDecl instanceof VarDecl)
        {
            VarDecl var = (VarDecl)memberDecl;
            return new ActionResult<>(buildMemberExpr(baseExpr, isArrow,
                    var, var.getDeclType(), EVK_LValue, memberLoc, OK_Ordinary));
        }

        assert !(memberDecl instanceof FunctionDecl);

        if (memberDecl instanceof EnumConstantDecl)
        {
            EnumConstantDecl Enum = (EnumConstantDecl)memberDecl;
            return new ActionResult<>(buildMemberExpr(baseExpr, isArrow,
                    Enum, Enum.getDeclType(), EVK_LValue, memberLoc, OK_Ordinary));
        }

        // We found something that we didn't expect. Complain.
        if (memberDecl instanceof TypeDecl)
        {
            diag(memberLoc, err_typecheck_member_reference_type)
                    .addTaggedVal(memberName)
                    .addTaggedVal(baseType)
                    .addTaggedVal(isArrow?1:0)
                    .emit();
        }
        else
        {
            diag(memberLoc, err_typecheck_member_reference_unknown)
                    .addTaggedVal(memberName)
                    .addTaggedVal(baseType)
                    .addTaggedVal(isArrow?1:0)
                    .emit();
        }
        diag(memberDecl.getLocation(), note_member_declared_here)
                .addTaggedVal(memberName)
                .emit();
        return exprError();
    }

    private ActionResult<Expr> lookupMemberExpr(
            LookupResult lookupResult,
            OutParamWrapper<ActionResult<Expr>> baseExpr,
            boolean isArrow,
            SourceLocation opLoc)
    {
        assert baseExpr.get().get() != null:"no base expressin!";

        // Perform default conversions.
        Expr e = baseExpr.get().get();
        baseExpr.set(defaultFunctionArrayConversion(e));

        if (baseExpr.get().isInvalid())
            return exprError();

        if (isArrow)
        {
            baseExpr.set(defaultLvalueConversion(baseExpr.get().get()));
            if (baseExpr.get().isInvalid())
                return exprError();
        }

        QualType baseType = baseExpr.get().get().getType();
        String memberName = lookupResult.getLookupName();
        SourceLocation memberLoc = lookupResult.getNameLoc();

        // For later type-checking purposes, turn arrow accesses into dot
        // accesses.
        if (isArrow)
        {
            if (baseType.isPointerType())
            {
                PointerType ptr = baseType.getAsPointerType();
                baseType = ptr.getPointeeType();
            }
            else if (baseType.isRecordType())
            {
                // Recover from arrow accesses to records, e.g.:
                //   struct MyRecord foo;
                //   foo->bar
                diag(opLoc, err_typecheck_member_reference_suggestion)
                        .addTaggedVal(baseType).addTaggedVal(isArrow?1:0)
                        .addSourceRange(baseExpr.get().get().getSourceRange())
                        .addFixItHint(FixItHint.createReplacement(opLoc, "."))
                        .emit();
                isArrow = false;
            }
            else
            {
                diag(memberLoc, err_typecheck_member_reference_arrow)
                        .addTaggedVal(baseType)
                        .addSourceRange(baseExpr.get().get().getSourceRange())
                        .emit();
                return exprError();
            }
        }

        // Handle field access to simple records.
        if (baseType.isRecordType())
        {
            RecordType rty = baseType.getAsRecordType();
            if (lookupMemberExprInRecord(lookupResult,
                    baseExpr.get().get().getSourceRange(),
                    rty, opLoc))
                return exprError();

            // Returning valid-but-null is how we indicate to the caller that
            // the lookup result was filled in.
            return exprError();
        }
        // Recover from dot accesses to pointers, e.g.:
        //   type *foo;
        //   foo.bar
        if (baseType.isPointerType())
        {
            PointerType ptr = context.<PointerType>getAs(baseType);
            if (!isArrow && ptr.getPointeeType().isRecordType())
            {
                diag(opLoc, err_typecheck_member_reference_suggestion)
                        .addTaggedVal(baseType).addTaggedVal(isArrow?1:0)
                        .addSourceRange(baseExpr.get().get().getSourceRange())
                        .addFixItHint(FixItHint.createReplacement(opLoc, "->"))
                        .emit();

                // Recurse as an -> access.
                isArrow = true;
                return lookupMemberExpr(lookupResult, baseExpr, isArrow, opLoc);
            }
        }


        diag(memberLoc, err_typecheck_member_reference_struct_union)
                .addTaggedVal(baseType)
                .addSourceRange(baseExpr.get().get().getSourceRange())
                .emit();
        return exprError();
    }

    /**
     * Performs lvalue-to-rvalue conversion on the operand.  This is
     * DefaultFunctionArrayLvalueConversion, except that it assumes the operand
     * isn't of function or array type.
     * @param e
     * @return
     */
    private ActionResult<Expr> defaultLvalueConversion(Expr e)
    {
        QualType t = e.getType();
        assert !t.isNull():"r-value conversion on typeless expression!";

        // The C standard is actually really unclear on this point, and
        // DR106 tells us what the result should be but not why.  It's
        // generally best to say that void types just doesn't undergo
        // lvalue-to-rvalue at all.  Note that expressions of unqualified
        // 'void' type are never l-values, but qualified void can be.
        if (t.isVoidType())
            return new ActionResult<>(e);

        // TODO checkForNullPointerDereference(e);

        // C99 6.3.2.1p2:
        //   If the lvalue has qualified type, the value has the unqualified
        //   version of the type of the lvalue; otherwise, the value has the
        //   type of the lvalue.
        if (t.hasQualifiers())
            t = t.clearQualified();

        return new ActionResult<>(
                new ImplicitCastExpr(t, EVK_RValue, e, CK_LValueToRValue, e.getExprLocation())
        );
    }

    private boolean lookupMemberExprInRecord(
            LookupResult res,
            SourceRange baseRange,
            RecordType rty,
            SourceLocation opLoc)
    {
        RecordDecl recordDecl = rty.getDecl();
        if (requireCompleteType(opLoc, new QualType(rty),
                pdiag(err_typecheck_incomplete_tag).
                        addSourceRange(baseRange)))
            return true;

        return false;
    }


    public ActionResult<Expr> actOnPostfixUnaryOp(
            SourceLocation loc,
            TokenKind kind,
            Expr lhs)
    {
        UnaryOperatorKind opc;
        switch (kind)
        {
            default:
                Util.shouldNotReachHere("Unknown unary op!");
            case plusplus:
                opc = UO_PostInc;break;
            case subsub:
                opc = UO_PostDec;break;
        }
        return createUnaryOp(loc, opc, lhs);
    }

    private ActionResult<Expr> createUnaryOp(
            SourceLocation opLoc,
            UnaryOperatorKind opc,
            Expr inputExpr)
    {
        ActionResult<Expr> input = new ActionResult<>(inputExpr);
        ExprValueKind vk = EVK_RValue;
        QualType resultTy = new QualType();

        switch (opc)
        {
            case UO_PreInc:
            case UO_PreDec:
            case UO_PostDec:
            case UO_PostInc:
                OutParamWrapper<ExprValueKind> o1 = new OutParamWrapper<>(vk);
                resultTy = checkInrementDecrementOperand(input.get(), o1, opLoc,
                        opc == UO_PreInc || opc == UO_PostInc,
                        opc == UO_PostDec || opc == UO_PreDec);
                vk = o1.get();
                break;
            case UO_AddrOf:
                resultTy = checkAddressOfOperand(input.get(), opLoc);
                break;
            case UO_Deref:
                input = defaultFunctionArrayLValueConversion(inputExpr);
                o1 = new OutParamWrapper<>(vk);
                resultTy = checkIndirectOperand(input.get(), o1, opLoc);
                vk = o1.get();
                break;

            case UO_Plus:
            case UO_Minus:
            {
                input = usualUnaryConversions(input.get());
                if (input.isInvalid())
                    return exprError();
                resultTy = input.get().getType();
                if (resultTy.isArithmeticType()) // C99 6.5.3.3p1
                    break;
                else
                {
                    diag(opLoc, err_typecheck_unary_expr)
                    .addTaggedVal(resultTy).addSourceRange
                            (input.get().getSourceRange()).emit();
                    return exprError();
                }
            }
            case UO_Not:  // bitwise not.
            {
                input = usualUnaryConversions(input.get());
                if (input.isInvalid()) return exprError();

                resultTy = input.get().getType();
                if (resultTy.isIntegerType())
                    break;
                else
                {
                    diag(opLoc, err_typecheck_unary_expr)
                            .addTaggedVal(resultTy).addSourceRange
                            (input.get().getSourceRange()).emit();
                    return exprError();
                }
            }
            case UO_LNot:  // logical not operation.
            {
                input = defaultFunctionArrayConversion(input.get());
                if (input.isInvalid()) return exprError();

                resultTy = input.get().getType();

                if (!resultTy.isScalarType())
                {
                    diag(opLoc, err_typecheck_unary_expr)
                            .addTaggedVal(resultTy)
                            .addSourceRange(input.get().getSourceRange())
                            .emit();
                    return exprError();
                }
                // LNot always has type int. C99 6.5.3.3p5.
                resultTy = context.IntTy;
                break;
            }
        }
        if (resultTy.isNull() || input.isInvalid())
            return exprError();

        // Checks for array bounds violation in the operands of the UnaryOperator,
        // except for the "*" and "&" operators that have to be handled specially
        // by checkArrayAccess()
        checkArrayAccess(input.get());
        return new ActionResult<>(new UnaryExpr(input.get(), opc, resultTy, vk, opLoc));
    }

	/**
	 * Diagnose the empty lookup.
     * @param s
     * @param res
     * @return
     */
    private boolean diagnoseEmptyLookup(Scope s,
            LookupResult res)
    {
        String name = res.getLookupName();

        int diagnostic = err_undeclared_var_use;
        //int diagnosticSuggest = err_undeclared_var_use_suggest;
        // We can not recovery.
        diag(res.getNameLoc(), diagnostic).addTaggedVal(name).emit();
        return true;
    }

    private LangOptions getLangOptions()
    {
        return pp.getLangOptions();
    }

	/**
	 * A mapping from external names to the most recent
     * locally-scoped external declaration with that name.
     *
     * This map contains external declarations introduced in local
     * scoped, e.g.,
     *
     * <code>
     * void f()
      *  {
     *   void foo(int, int);
     * }
     * </code>
     *
     * Here, the name "foo" will be associated with the declaration on
     * "foo" within f. This name is not visible outside of
     * "f". However, we still find it in two cases:
     *
     *   - If we are declaring another external with the name "foo", we
     *     can find "foo" as a previous declaration, so that the types
     *     of this external declaration can be checked for
     *     compatibility.
     *
     *   - If we would implicitly declare "foo" (e.g., due to a call to
     *     "foo" in C when no prototype or definition is visible), then
     *     we find this declaration of "foo" and complain that it is
     *     not visible.
     */
    private HashMap<String, NamedDecl> locallyScopedExternalDecls
            = new HashMap<>();

    private Scope translateUnitScope;

    /**
	 * A undeclared identifier was used in a functon call, forming a call to
     * an implicitly defined function (per C99 6.5.1p2).
     * @param nameLoc
     * @param name
     * @param s
     * @return
     */
    private NamedDecl implicitDefineFunction(SourceLocation nameLoc,
            String name, Scope s)
    {
        // Before we produce a declaration for an implicitly defined
        // function, see whether there was a locally-scoped declaration of
        // this name as a function or variable. If so, use that
        // (non-visible) declaration, and complain about it.
        if (locallyScopedExternalDecls.containsKey(name))
        {
            NamedDecl prev = locallyScopedExternalDecls.get(name);
            diag(nameLoc, warn_use_out_of_scope_declaration)
                    .addTaggedVal(prev.getDeclName()).emit();
            diag(prev.getLocation(), note_previous_declaration).emit();
            return prev;
        }

        // Extension in C99.  Legal in C90, but warn about it.
        if (getLangOptions().c99)
            diag(nameLoc, ext_implicit_function_decl)
                    .addTaggedVal(name).emit();
        else
            diag(nameLoc, warn_implicit_function_decl)
                    .addTaggedVal(name).emit();
        DeclSpec ds = new DeclSpec();
        OutParamWrapper<String> x = new OutParamWrapper<>();
        OutParamWrapper<Integer> y = new OutParamWrapper<>();
        boolean error = ds.setTypeSpecType(TST.TST_int, nameLoc, x, y);
        String dummy = x.get();
        int diagID = y.get();
        assert !error :"Error setting up implicit decl!";
        Declarator d = new Declarator(ds, Declarator.TheContext.BlockContext);
        d.addTypeInfo(DeclaratorChunk.getFunction(false, false,
                SourceLocation.NOPOS, null, 0, nameLoc, nameLoc),
                SourceLocation.NOPOS);

        d.setIdentifier(name, nameLoc);

        // Insert this function into translation-unit scope.
        DeclContext prevDC = curContext;
        curContext = context.getTranslateUnitDecl();

        FunctionDecl fd = (FunctionDecl) actOnDeclarator(translateUnitScope, d);
        fd.setImplicit(true);
        curContext = prevDC;
        return fd;
    }

    public ActionResult<Expr> actOnIdentifierExpr(
            Scope s,
            SourceLocation loc,
            IdentifierInfo id,
            boolean hasTrailingLParen,
            boolean isAddressOfOperand)
    {
        assert !isAddressOfOperand && hasTrailingLParen:
                "cannot be direct & operand and have a trailing lparen";

        String name = id.getName();
        SourceLocation nameLoc = loc;

        // Perform the required lookup.
        //LookupResult res = new LookupResult(this, name, nameLoc, LookupOrdinaryName);
        LookupResult res = lookupParsedName(s, name, LookupOrdinaryName, nameLoc);

        if (res.isAmbiguous())
        {
            diag(nameLoc, err_ambiguous_reference).addTaggedVal(name).emit();
            diag(res.getFoundDecl().getLocation(), note_ambiguous_candidate)
                    .addTaggedVal(res.getFoundDecl().getDeclName()).emit();
            return exprError();
        }
        if (res.isEmpty())
        {
            // Otherwise, this could be an implicitly declared function reference (legal
            // in C90, extension in C99
            if (hasTrailingLParen && name != null)
            {
                NamedDecl d = implicitDefineFunction(nameLoc, name, s);
                if (d != null)
                    res.addDecl(d);
            }

            // If this name wasn't predeclared and if this is not a function
            // call, diagnose the problem.
            if (res.isEmpty())
            {
                if (diagnoseEmptyLookup(s, res))
                    return exprError();

                assert !res.isEmpty() :"diagnoseEmptyLookup returned false!";
            }
        }

        // Make sure we find a declaration with specified getIdentifier.
        assert !res.isEmpty() && res.isSingleResult();

        return buildDeclarationNameExpr(res);
    }

    private void diagnoseUnusedParameters(ArrayList<ParamVarDecl> params)
    {
        // TODO: 2017/3/28  
    }
    
    private void checkFallThroughForFunctionDef(FunctionDecl fd, Stmt body)
    {
        // TODO: 2017/3/28
    }

    private HashMap<String, LabelledStmt> functionLabelMap = new HashMap<>();

    private HashMap<String, LabelledStmt> getLabelMap()
    {
        return functionLabelMap;
    }

    private Stack<SwitchStmt> functionSwitchStack = new Stack<>();

    private Stack<SwitchStmt> getSwtichBlock()
    {
        return functionSwitchStack;
    }

	/**
	 * his is set to true when a function or
     * contains a VLA or an ObjC try block, which introduce
     * scopes that need to be checked for goto conditions.  If a function does
     * not contain this, then it need not have the jump checker run on it.
     */
    private boolean curFunctionNeedsScopeChecking;
    
    private void diagnoseInvalidJumps(Stmt body)
    {
        // TODO: 2017/3/28
    }

    public Decl actOnFinishFunctionBody(Decl funcDecl, Stmt fnBody)
    {
        if (funcDecl instanceof FunctionDecl)
        {
            FunctionDecl fd = (FunctionDecl)funcDecl;
            fd.setBody(fnBody);

            if (fd.isMain())
            {
                // C and C++ allow for main to automagically return 0.
                // Implements C++ [basic.start.main]p5 and C99 5.1.2.2.3.
                fd.setHasImplicitReturnZero(true);
            }
            else
                checkFallThroughForFunctionDef(fd, fnBody);

            if (!fd.isInvalidDecl())
                diagnoseUnusedParameters(fd.getParamInfo());
        }
        else
        {
            return null;
        }

        popDeclContext();

        // Verify and clean out per-function state.
        for (Map.Entry<String, LabelledStmt> pair : functionLabelMap.entrySet())
        {
            LabelledStmt l = pair.getValue();

            if (l.body != null)
                continue;
            diag(l.loc, err_undeclared_label_use).addTaggedVal(l.getName()).emit();

            if (fnBody == null)
            {
                // The whole function wasn't parsed correctly, just delete this.
                continue;
            }

            l.body = new NullStmt(l.label.getLocation());

            CompoundStmt compound = (CompoundStmt)fnBody;
            ArrayList<Stmt> elts = new ArrayList<>();
            elts.addAll(compound.stats);

            elts.add(l);
            compound.stats = elts;
        }
        functionLabelMap.clear();

        if (fnBody == null)
            return funcDecl;
        // Verify that that gotos and switch cases don't jump into scopes illegally.
        if (curFunctionNeedsScopeChecking)
            diagnoseInvalidJumps(fnBody);

        return funcDecl;
    }

    /**
     * Complete semantic analysis for a reference to the given declaration.
     * @return
     */
    private ActionResult<Expr> buildDeclarationNameExpr(
            LookupResult res)
    {
        return exprError();
    }

    private boolean checkDeclInExpr(SourceLocation loc, NamedDecl decl)
    {
        if (decl instanceof TypedefNameDecl)
        {
            diag(loc, err_unexpected_typedef_ident)
                    .addTaggedVal(decl.getDeclName())
                    .emit();
            return true;
        }
        return false;
    }

    /**
     * This is called at the very end of the
     * translation unit when EOF is reached and all but the top-level scope is
     * popped.
     */
    public void actOnEndOfTranslationUnit()
    {
        // TODO: 2017/3/28
    }

    /**
     * Given that there was an error parsing an initializer for the given
     * declaration. Try to return to some form of sanity.
     * @param decl
     */
    public void actOnInitializerError(Decl decl)
    {
        if (decl == null || decl.isInvalidDecl())
            return;

        if (!(decl instanceof VarDecl))
            return;
        VarDecl vd = (VarDecl)decl;
        if (vd == null)
            return;

        QualType ty = vd.getDeclType();
        if (requireCompleteType(vd.getLocation(),
                context.getBaseElementType(ty),
                err_typecheck_decl_incomplete_type))
        {
            vd.setInvalidDecl(true);
        }
    }

    /**
     * Adds the initializer {@code init} to declaration decl.
     * If {@code directDecl} is {@code true}, this is a C++ direct initializer
     * rather than copy initialization.
     *
     * @param decl
     * @param init
     * @param directDecl
     */
    public void addInitializerToDecl(Decl decl, Expr init, boolean directDecl)
    {
        // If there is no declaration, there was an error parsing it.
        if (decl == null || decl.isInvalidDecl())
            return;

        // Check self-reference within variable initializer.
        if (decl instanceof VarDecl)
        {
            VarDecl vd = (VarDecl) decl;

            // Variables declared within a function/method body are handled
            // by a jlang.dataflow analysis.
            if (!vd.hasLocalStorage() && !vd.isStaticLocal())
                checkSelfReference(decl, init);
        }
        else
        {
            checkSelfReference(decl, init);
        }

        if (!(decl instanceof VarDecl))
        {
            assert !(decl instanceof FieldDecl) : "field init shoudln't gt here!";
            diag(decl.getLocation(), err_illegal_initializer).emit();
            decl.setInvalidDecl(true);
            return;
        }

        VarDecl vd = (VarDecl) decl;

        // A definition must end up with a complete type, which means it must be
        // complete with the restriction that an array type might be completed by the
        // initializer; note that later code assumes this restriction.
        QualType baseDeclType = vd.getDeclType();
        ArrayType array = context.getAsInompleteArrayType(baseDeclType);

        if (array != null)
        {
            baseDeclType = array.getElemType();
        }
        if (requireCompleteType(vd.getLocation(), baseDeclType, err_typecheck_decl_incomplete_type))
        {
            decl.setInvalidDecl(true);
            return;
        }
        // TODO Check redefinition.

        // Get the decls type and save a reference for later, since
        // CheckInitializerTypes may change it.
        QualType declTy = vd.getDeclType(), savedTy = declTy;
        if (vd.isLocalVarDecl())
        {
            if (vd.hasExternalStorage())
            {
                // C99 6.7.8p5
                diag(vd.getLocation(), err_block_extern_cant_init).emit();
                vd.setInvalidDecl(true);
            }
            else if(!vd.isInvalidDecl())
            {
                // TODO initialization sequence 2016.10.23

                // C99 6.7.8p4.
                if (vd.getStorageClass() == StorageClass.SC_static)
                {
                    checkForConstantInitializer(init, declTy);
                }
            }
        }
        else if (vd.isFileVarDecl())
        {
            if (vd.hasExternalStorage() &&
                    !context.getBaseElementType(vd.getDeclType()).isConstQualifed())
            {
                diag(vd.getLocation(), warn_extern_init).emit();
            }
            if (!vd.isInvalidDecl())
            {
                // TODO initialization sequence 2016.10.23

                // C99 6.7.8p4. All file scoped initializers need to be constant.
                checkForConstantInitializer(init, declTy);
            }
        }

        // If the type changed, it means we had an incomplete type that was
        // completed by the initializer. For example:
        //   int ary[] = { 1, 3, 5 };
        // "ary" transitions from a VariableArrayType to a ConstantArrayType.
        if (!vd.isInvalidDecl() && !declTy.equals(savedTy))
        {
            vd.setDeclType(declTy);
            init.setType(declTy);
        }

        // Check any implicit conversions within the expression.
        checkImplicitConversion(init, vd.getLocation());

        vd.setInit(init);
    }


    public ActionResult<Expr> actOnInitList(SourceLocation lbraceLoc,
            List<Expr> initExprs, SourceLocation rbraceLoc)
    {
        InitListExpr e = new InitListExpr(lbraceLoc, rbraceLoc, new ArrayList<>(initExprs));
        e.setType(context.VoidTy);
        return new ActionResult<>(e);
    }

    /**
     * Issues warning message if original variable used in initialization expression
     * @param decl
     * @param init
     */
    private void checkSelfReference(Decl decl, Expr init)
    {
        new SelfReferenceChecker(this, decl).visitExpr(init);
    }

    private boolean checkForConstantInitializer(Expr init, QualType declType)
    {
        // Need strict checking.  In C89, we need to check for
        // any assignment, increment, decrement, function-calls, or
        // commas outside of a sizeof.  In C99, it's the same list,
        // except that the aforementioned are allowed in unevaluated
        // expressions.  Everything else falls under the
        // "may accept other forms of constant expressions" jlang.exception.
        if (init.isConstantInitializer())
            return false;
        diag(init.getExprLocation(), err_init_element_not_constant)
                .addSourceRange(init.getSourceRange()).emit();
        return true;
    }

	/**
     * This method is called *for error recovery purposes only*
     * to determine if the specified name is a valid tc name ("struct foo").  If
     * so, this returns the TST for the tc corresponding to it (TST_enum,
     * TST_union, TST_struct, TST_class).  This is used to diagnose cases in C
     * where the user forgot to specify the tc.
     * @param identifierInfo
     * @param scope
     * @return
     */
    public TST isTagName(IdentifierInfo identifierInfo, Scope scope)
    {
        NamedDecl ndecl = lookupName(scope, identifierInfo.getName(),
                SourceLocation.NOPOS, LookupTagName);
        if (ndecl != null)
        {
            if (ndecl instanceof TagDecl)
            {
                TagDecl tdecl = (TagDecl) ndecl;
                switch (tdecl.getTagKind())
                {
                    case TTK_struct:
                        return TST.TST_struct;
                    case TTK_union:
                        return TST.TST_union;
                    case TTK_enum:
                        return TST.TST_enum;
                }
            }
        }
        return TST.TST_unspecified;
    }

    public ActionResult<Stmt> actOnNullStmt(SourceLocation loc)
    {
        return new ActionResult<>(new Tree.NullStmt(loc), true);
    }
}
