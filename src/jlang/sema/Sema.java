package jlang.sema;

import jlang.ast.ASTConsumer;
import jlang.ast.CastKind;
import jlang.ast.Tree;
import jlang.ast.Tree.*;
import jlang.basic.LangOption;
import jlang.cparser.*;
import jlang.cparser.DeclSpec.DeclaratorChunk;
import jlang.cparser.DeclSpec.DeclaratorChunk.FunctionTypeInfo;
import jlang.cparser.DeclSpec.SCS;
import jlang.cparser.DeclSpec.TST;
import jlang.cparser.Token.CharLiteral;
import jlang.cparser.Token.Ident;
import jlang.cparser.Token.IntLiteral;
import jlang.cpp.Preprocessor;
import jlang.cpp.Source;
import jlang.cpp.SourceLocation;
import jlang.cpp.SourceLocation.SourceRange;
import jlang.diag.*;
import jlang.sema.Decl.*;
import jlang.type.*;
import jlang.type.ArrayType.VariableArrayType;
import jlang.type.Type.TagTypeKind;
import tools.*;

import java.util.*;

import static jlang.ast.CastKind.*;
import static jlang.ast.Tree.ExprValueKind.EVK_LValue;
import static jlang.ast.Tree.ExprValueKind.EVK_RValue;
import static jlang.cparser.DeclSpec.TQ.*;
import static jlang.cparser.Parser.exprError;
import static jlang.cparser.Parser.stmtError;
import static jlang.cparser.Tag.*;
import static jlang.diag.DiagnosticSemaTag.*;
import static jlang.sema.BinaryOperatorKind.BO_Div;
import static jlang.sema.BinaryOperatorKind.BO_DivAssign;
import static jlang.sema.LookupResult.LookupResultKind.Found;
import static jlang.sema.Scope.ScopeFlags.CompilationUnitScope;
import static jlang.sema.Scope.ScopeFlags.DeclScope;
import static jlang.sema.Sema.LookupNameKind.*;
import static jlang.sema.UnaryOperatorKind.*;

/**
 * This file defines the {@linkplain Sema} class, which performs semantic
 * analysis and builds ASTs for C.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public final class Sema implements DiagnosticParseTag, DiagnosticCommonKindsTag,
        DiagnosticSemaTag
{
    public enum TagUseKind
    {
        TUK_reference,      // Reference to a tag: 'struct foo *X;'
        TUK_declaration,    // Forward declaration of a tag: 'struct foo;'
        TUK_definition     // Definition of a tag: 'struct foo {int X;} Y;'
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
     *    in the tag getIdentifier space.
     *  </li>
     *  <li>
     *   identifier that follows the member access or member access through pointer
     *   operator is looked up in the getIdentifier space of members of the jlang.type determined
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
    final Parser parser;
    private Stack<FunctionScopeInfo> functionScopes;
    private static Context SEMA_CONTEXT = new Context();
    private ASTConsumer consumer;
    private ASTContext context;

    public Sema(Preprocessor pp, ASTContext ctx, ASTConsumer consumer)
    {
        parser = Parser.instance(pp, this);
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

    public Parser getParser()
    {
        return parser;
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
    public QualType getTypeByName(String identifierInfo,
            SourceLocation nameLoc, Scope curScope)
    {
        LookupResult res = lookupParsedName(curScope, identifierInfo,
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
     * Determines if a tag with a given kind is acceptable as a redeclaration of
     * the given tag declaration.
     * @param previous
     * @param newTag
     * @param newTagLoc
     * @param name
     * @return  Return true if the new tag kind is acceptable, false otherwise.
     */
    private boolean isacceptableTagRedeclaration(
            TagDecl previous,
            TagTypeKind newTag,
            SourceLocation newTagLoc,
            String name)
    {
        TagTypeKind oldTag = previous.getTagKind();
        return oldTag == newTag;
    }

    /**
     * This method was invoked when it sees 'struct X {...}' or 'struct X;'.
     * In the former class, the getIdentifier must be non null. In the later case, getIdentifier
     * will be null.
     *
     * @param curScope
     * @param tagType  Indicates what kind of tag this is.
     * @param tuk      Indicates whether this is a reference/declaration/definition
     *                 of a tag.
     * @param startLoc
     * @param name
     * @param nameLoc
     * @return
     */
    public ActionResult<Decl> actOnTag(
            Scope curScope,
            TST tagType,
            TagUseKind tuk,
            SourceLocation kwLoc,
            String name,
            SourceLocation nameLoc)
    {
        // if this is not a definition, it must have a getIdentifier
        assert (name != null || tuk == TagUseKind.TUK_definition)
                : "Nameless record must be a definition";

        TagTypeKind kind = TagTypeKind.getTagTypeKindForTypeSpec(tagType);

        QualType enumUnderlying = null;
        if (kind == TagTypeKind.TTK_enum)
        {
            // C99, Each enumerator that appears in the body of an enumeration
            // specifier becomes an integer constant with jlang.type int in the
            // enclosing scope and can be used whenever integer constants are required
            enumUnderlying = Type.IntTy;
        }

        LookupResult result = new LookupResult(this, name, nameLoc,
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
                // tag declaration or definition.
                while (searchDC instanceof RecordDecl
                        || searchDC instanceof EnumDecl)
                    searchDC = searchDC.getParent();
            }
        }
        // If there is a previous tag definition or forward declaration was found,
        // Handle it.
        if (prevDecl != null)
        {
            if (prevDecl instanceof TagDecl)
            {
                TagDecl prevTagDecl = (TagDecl)prevDecl;
                // If this is a use of a previous tag, or if the tag is already declared
                // in the same scope (so that the definition/declaration completes or
                // rementions the tag), reuse the decl.
                if (tuk == TagUseKind.TUK_reference || isDeclInScope(prevDecl, searchDC, curScope))
                {
                    // Make sure that this wasn't declared as an enum and now used as a
                    // struct or something similar.
                    if (!isacceptableTagRedeclaration(prevTagDecl, kind, kwLoc, name))
                    {
                        boolean safeToContinue = (prevTagDecl.getTagKind() != TagTypeKind.TTK_enum
                        && kind != TagTypeKind.TTK_enum);
                        if (safeToContinue)
                        {
                            parser.diag(kwLoc, err_use_with_wrong_tag)
                                    .addTaggedVal(name)
                                    .addCodeModificationHint(
                                            CodeModificationHint.createReplacement
                                                    (new SourceRange(kwLoc),
                                                        prevTagDecl.getKindName()));
                        }
                        else
                        {
                            parser.diag(kwLoc, err_use_with_wrong_tag).
                                    addTaggedVal(name);
                        }
                        parser.diag(prevDecl.getLocation(), note_previous_use);
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
                        // Diagnose attempts to redefine a tag.
                        if (tuk == TagUseKind.TUK_definition)
                        {
                            TagDecl def = prevTagDecl.getDefinition();
                            if (def != null)
                            {
                                parser.diag(nameLoc, err_redefinition)
                                        .addTaggedVal(name);
                                parser.diag(def.getLocation(), note_previous_definition);
                                name = null;
                                prevDecl = null;
                                invalid = true;
                            }
                            else
                            {
                                // Reaching here, it indicates that the previous
                                // is forward declaration, and this is actually
                                // complete definition.
                                TagType tag = (TagType)context.getTagDeclType(prevTagDecl).baseType();
                                if (tag.isBeingDefined())
                                {
                                    parser.diag(nameLoc, err_nested_redefinition)
                                            .addTaggedVal(name);
                                    parser.diag(prevTagDecl.getLocation(), note_previous_definition);
                                    name = null;
                                    prevDecl = null;
                                    invalid = true;
                                }
                            }
                            // Okay, this is definition of a previously declared or referenced
                            // tag PrevDecl. We're going to create a new Decl for it.
                        }
                    }
                    // If we get here we have (another) forward declaration or we
                    // have a definition.  Just create a new decl.
                }
                else
                {
                    // If we get here, this is a definition of a new tag type in a nested
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
                    parser.diag(nameLoc, err_redefinition_different_kind)
                            .addTaggedVal(name);
                    parser.diag(prevDecl.getLocation(), note_previous_definition);
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
            // Current tag is enum declaration, reference, or definition.
            if (tagType == TST.TST_enum)
            {
                newDecl = new EnumDecl(name,searchDC, loc, (EnumDecl)prevDecl);

                // if this is an undefined enum, warns it.
                if (tuk != TagUseKind.TUK_definition && !invalid)
                {
                    parser.diag(loc, ext_forward_ref_enum);
                    if (tuk == TagUseKind.TUK_reference)
                        isForwardReference = true;

                }
                if (enumUnderlying != null)
                {
                    EnumDecl ed = (EnumDecl)newDecl;
                    if (ed != null)
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

            // If we're declaring or defining a tag in function prototype scope
            // in C, note that this jlang.type can only be used within the function.
            if (name != null && curScope.isFunctionProtoTypeScope())
            {
                parser.diag(loc, warn_decl_in_param_list)
                .addTaggedVal(context.getTagDeclType(newDecl));
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

    public Decl actOnField(
            Scope scope,
            Decl tagDecl,
            SourceLocation startLoc,
            Declarator declarator,
            Expr bitFieldSize)
    {
        return null;
    }

    public Decl actOnFields(Scope curScope,
            SourceLocation recordLoc,
            Decl tagDecl,
            ArrayList<Decl> fieldDecls,
            SourceLocation startLoc,
            SourceLocation endLoc)
    {
        // TODO: 2017/3/28  
        return null;
    }

    public ActionResult<Expr> actOnNumericConstant(Token token)
    {
        assert token != null
                && (token.tag == Tag.INTLITERAL
                || token.tag == Tag.LONGLITERAL
                || token.tag == Tag.FLOATLITERAL
                || token.tag == Tag.DOUBLELITERAL);

        switch (token.tag)
        {
            case Tag.FLOATLITERAL:
            case Tag.DOUBLELITERAL:
            {
                QualType ty = token.tag == FLOATLITERAL? Type.FloatTy:Type.DoubleTy;

                APFloat val = new APFloat();
                // TODO deal with float point number.
            }

            case INTLITERAL:
            case LONGLITERAL:
            {
                QualType ty = new QualType();
                IntLiteral literal = (IntLiteral)token;

                APInt resultVal = new APInt(32, 0);

                if (literal.getIntegerValue(resultVal))
                {
                    // current long long can not be supported.
                    parser.syntaxError(literal.loc, "%s integer too large",
                            literal.toString());
                    return exprError();
                }
                else
                {
                    // TODO deal with integer
                    boolean allowedUnsigned =
                            literal.isUnsigned() || literal.getRadix() != 10;

                    // check from smallest to largest, picking the smallest jlang.type we can.
                    int width = 0;
                    if (!literal.isLong())
                    {
                        // Are int/ unsigned int possibility?
                        int intSize = 32;

                        // does it fit in a unsigned int?
                        if (resultVal.isIntN(intSize))
                        {
                            BitSet x = new BitSet();
                            if (!literal.isUnsigned() && !resultVal.get(intSize - 1))
                                ty = Type.IntTy;
                            else if (allowedUnsigned)
                                ty = Type.UnsignedIntTy;
                            width = intSize;
                        }
                    }
                    // Are long/unsigned long possibilities?
                    if (ty.isNull() &&literal.isLong())
                    {
                        int longSize = 32;

                        // does it fit in a unsigned long?
                        if (resultVal.isIntN(longSize))
                        {
                            // Does it fit in a signed long?
                            if(!literal.isUnsigned() && !resultVal.get(longSize - 1))
                                ty = Type.LongTy;
                            else if (allowedUnsigned)
                                ty = Type.UnsignedLongTy;
                            width = longSize;
                        }
                    }

                    // If we still couldn't decide a jlang.type, we probably have
                    // something that does not fit in a signed long, but has no U suffix.
                    if (ty.isNull())
                    {
                        parser.syntaxError(literal.loc, "%s integer too large for signed",
                                literal.toString());
                        ty = Type.UnsignedLongTy;
                        width = 32;
                    }

                    if (resultVal.getBitWidth() != width)
                        resultVal = resultVal.trunc(width);
                }
                return new ActionResult<>(new IntegerLiteral(resultVal, ty, token.loc));
            }
            default:
                return exprError();
        }

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

            //TODO: Diagose unused variables in this scope.
            itr.remove();
        }
    }

    /**
     * Called from {@linkplain Parser#parseFunctionDeclarator(Declarator)}
     * to introduce parameters into function prototype scope.
     *
     * @param scope
     * @param paramDecls
     * @return
     */
    public Decl actOnParamDeclarator(Scope scope, Declarator paramDecls)
    {
        final DeclSpec ds = paramDecls.getDeclSpec();
        StorageClass storageClass = StorageClass.SC_none;

        //Verify C99 6.7.5.3p2: The only SCS allowed is 'register'.
        if (ds.getStorageClassSpec() == SCS.SCS_register)
        {
            storageClass = StorageClass.SC_register;
        }
        else if (ds.getStorageClassSpec() != SCS.SCS_unspecified)
        {
            //TODO report error invalid storage class speciifer.
        }

        if (ds.isInlineSpecifier())
        {
            //TODO report error inline non function.
        }

        // ensure we have a invalid getIdentifier
        String name = paramDecls.getIdentifier();
        if (name == null)
        {
            // TODO report error: invalid identifier getIdentifier
            paramDecls.setInvalidType(true);
        }

        // check redeclaration, e.g. int foo(int x, int x);
        LookupResult result = new LookupResult(this, name,
                paramDecls.getIdentifierLoc(), LookupOrdinaryName);
        lookupName(result, scope);

        if (result.isSingleResult())
        {
            NamedDecl preDecl = result.getFoundDecl();

            // checks if redeclaration
            if (scope.isDeclScope(preDecl))
            {
                // TODO report error parameter redeclaration.
                name = null;
                paramDecls.setIdentifier(null, paramDecls.getIdentifierLoc());
                paramDecls.setInvalidType(true);
            }
        }

        ///
        ParamVarDecl newVar = new ParamVarDecl(DeclKind.FunctionDecl,
                curContext, name, paramDecls.getIdentifierLoc(), null,/* TODO: don't handle it. */
                storageClass);
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

        // Enter teh tag context.
        pushDeclContext(scope, tag);
    }

    private void pushDeclContext(Scope scope, TagDecl tag)
    {
        DeclContext dc = tag;
        curContext = dc;
        scope.setEntity(dc);
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
        // Notify the consumer that we've defined a tag.
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
     * IfStmt the context is a function, this function return true if decl is
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
            String id,
            Expr val)
    {
        APSInt enumVal = new APSInt(32);
        QualType eltTy = new QualType();
        if (val != null)
        {
            // Make sure to promote the operand type to int.
            Expr temp = usualUnaryConversion(val).get();
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
                    parser.diag(idLoc, warn_enum_value_overflow);
                eltTy = lastEnumConst.getDeclType();
            }
            else
            {
                eltTy  = Type.IntTy;
                enumVal.zextOrTrunc((int)eltTy.getTypeSize());
            }
        }
        return new EnumConstantDecl(id, enumDecl, idLoc, eltTy, val, enumVal);
    }

    public Decl actOnEnumConstant(Scope scope,
            Decl enumConstDecl,
            Decl lastConstEnumDecl,
            SourceLocation identLoc,
            String name,
            SourceLocation equalLoc,
            Expr val)
    {
        EnumDecl theEnumDecl = (EnumDecl) enumConstDecl;
        EnumConstantDecl lastEnumConst = (EnumConstantDecl) lastConstEnumDecl;

        // The scope passed in may not be a decl scope.  Zip up the scope tree until
        // we find one that is.
        Scope s = getNonFieldDeclScope(scope);
        NamedDecl prevDecl = lookupName(scope, name, identLoc,
                LookupOrdinaryName);

        // redefinition diagnostic.
        if (prevDecl != null)
        {
            if (!(prevDecl instanceof TagDecl) & isDeclInScope(prevDecl,
                    curContext, s))
            {
                if (prevDecl instanceof EnumConstantDecl)
                {
                    parser.diag(identLoc, err_redefinition_of_enumerator);;
                }
                else
                {
                    parser.diag(identLoc, err_redefinition);
                }
                parser.diag(prevDecl.getLocation(), note_previous_definition);
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
                        parser.diag(ecd.getLocation(), ext_enum_value_not_int);
                }

                // Keep track of the size of positive and negative values.
                if (initVal.isUnsigned() || initVal.isNonNegative())
                    numPositiveBits = Math.max(numPositiveBits, initVal.getActiveBits());
                else
                    numNegativeBits = Math.max(numNegativeBits, initVal.getMinSignedBits());

                // Keep track of whether every enum element has type int (very commmon).
                if (allElementsInt)
                    allElementsInt = ecd.getDeclType().equals(Type.IntTy);
            }
        }
        QualType bestType = new QualType();
        int bestWidth;

        // Figure out the type that should be used for this enum.
        if (numNegativeBits != 0)
        {
            if(numNegativeBits <= intWidth && numPositiveBits < intWidth)
            {
                bestType = Type.IntTy;
                bestWidth = intWidth;
            }
            else
            {
                bestWidth = context.target.getLongWidth();
                if(numNegativeBits <= bestWidth && numPositiveBits < bestWidth)
                    bestType = Type.LongTy;
                else
                {
                    bestWidth = context.target.getLonglongWidth();
                    if (numNegativeBits > bestWidth || numPositiveBits >= bestWidth)
                        parser.diag(enumDecl.getLocation(), warn_enum_too_large);
                    bestType = Type.LongLongTy;
                }
            }
        }
        else
        {
            // If there is no negative value, figure out which of uint, ulong, ulonglong
            // fits.
            if (numPositiveBits <= intWidth)
            {
                bestType = Type.UnsignedIntTy;
                bestWidth = intWidth;
            }
            else
            {
                bestWidth = context.target.getLonglongWidth();
                assert numPositiveBits <= bestWidth
                        :"How could an initialization get larger than ULL?";
                bestType = Type.UnsignedLongLongTy;
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
                if (ecd.getDeclType().equals(Type.IntTy))
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
                    newTy = Type.IntTy;
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
                    newSign = bestType.isSignedType();
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
        pushDeclContext(scope,
                new TranslationUnitDecl(curContext, SourceLocation.NOPOS));
    }

    public Decl actOnFunctionDef(Scope fnBodyScope, Declarator declarator)
    {
        assert getCurScope() == null : "Functio parsing confused";
        assert declarator.isFunctionDeclarator() : "Not a function declarator";

        Scope parentScope = fnBodyScope.getParent();
        declarator.setFunctionDefinition(true);
        Decl res = handleDeclarator(parentScope, declarator);
        return actOnStartOfFunctionDef(fnBodyScope, res);
    }

    private void diagnoseFunctionSpecifiers(Declarator d)
    {
        if (d.getDeclSpec().isInlineSpecifier())
            parser.diag(d.getDeclSpec().getInlineSpecLoc(),
                    err_inline_non_function);
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
            parser.diag(newOne.getLocation(), err_redefinition_different_kind)
                    .addTaggedVal(newOne.getDeclName());
            if (oldOne.getLocation().isValid())
            {
                parser.diag(oldOne.getLocation(), note_previous_definition);
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
                && oldType.getCanonicalTypeInternal()
                != newOne.getUnderlyingType().getCanonicalTypeInternal())
        {
            parser.diag(newOne.getLocation(),
                    err_redefinition_different_typedef)
                    .addTaggedVal(newOne.getUnderlyingType())
                    .addTaggedVal(oldType);
            if (oldTD.getLocation().isValid())
                parser.diag(oldTD.getLocation(), note_previous_definition);
            newOne.setInvalidDecl(true);
            return;
        }

        parser.diag(newOne.getLocation(), warn_redefinition_of_typedef)
                .addTaggedVal(newOne.getDeclName());
        parser.diag(oldTD.getLocation(), note_previous_definition);
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
        String name = d.getIdentifier();
        SourceLocation nameLoc = d.getIdentifierLoc();

        if (name == null)
        {
            if (!d.isInvalidType())
            {
                parser.diag(d.getDeclSpec().getSourceRange().getStart(),
                        err_declarator_need_ident)
                        .addSourceRange(d.getDeclSpec().getSourceRange())
                        .addSourceRange(d.getSourceRange());
            }
            return null;
        }
        // The scope passed in may not be a decl scope.  Zip up the scope tree until
        // we find one that is.
        while ((s.getFlags() & Scope.ScopeFlags.DeclScope.value) == 0)
            s = s.getParent();

        NamedDecl New = null;
        QualType ty = getTypeForDeclarator(d);

        LookupResult previous = new LookupResult(this, name, nameLoc,
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

        String name = d.getIdentifier();
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
    
    private QualType tryToFixInvalidVariablyModifiedType(QualType ty,
            ASTContext context, boolean sizeIsNegative)
    {
        // TODO: 2017/3/28
        return null;
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
            parser.diag(newOne.getLocation(), err_redefinition_different_kind)
                    .addTaggedVal(newOne.getDeclName());
            parser.diag(oldOne.getLocation(), note_previous_definition);
            newOne.setInvalidDecl(true);
            return;
        }

        VarDecl Old = (VarDecl)oldOne;

        QualType mergedTy = context.mergeType(newOne.getDeclType(), Old.getDeclType());
        if (mergedTy.isNull())
        {
            parser.diag(newOne.getLocation(), err_redefinition_different_type)
            .addTaggedVal(newOne.getDeclName());
            parser.diag(Old.getLocation(), note_previous_definition);
            newOne.setInvalidDecl(true);
            return;
        }

        newOne.setDeclType(mergedTy);

        // C99 6.2.2p4: Check if we have a static decl followed by a non-static.
        if (newOne.getStorageClass() == StorageClass.SC_static &&
                (Old.getStorageClass() == StorageClass.SC_none 
                        || Old.hasExternalStorage())) {
            parser.diag(newOne.getLocation(), err_static_non_static).
                    addTaggedVal(newOne.getDeclName());
            parser.diag(Old.getLocation(), note_previous_definition);
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
            parser.diag(newOne.getLocation(), err_non_static_static).
                    addTaggedVal(newOne.getDeclName());
            parser.diag(Old.getLocation(), note_previous_definition);
            return newOne.setInvalidDecl();
        }

        // Variables with external linkage are analyzed in FinalizeDeclaratorGroup.

        // FIXME: The test for external storage here seems wrong? We still
        // need to check for mismatches.
        if (!newOne.hasExternalStorage() && !newOne.isFileVarDecl() &&
                // Don't complain about out-of-line definitions of static members.
                !(Old.getDeclContext().isRecord() &&
            !newOne.getDeclContext().isRecord()))
        {
            parser.diag(newOne.getLocation(), err_redefinition).
                    addTaggedVal(newOne.getDeclName());
            parser.diag(Old.getLocation(), note_previous_definition);
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
     * have been translated into a declaratio
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

            boolean SizeIsNegative = false;
            QualType FixedTy = tryToFixInvalidVariablyModifiedType(ty, context, SizeIsNegative);

            if (FixedTy.isNull() && ty.isVariableArrayType()) {
                VariableArrayType VAT = ty.getAsVariableArrayType();
                // FIXME: This won't give the correct result for
                // int a[10][n];
                SourceRange SizeRange = VAT.getSizeExpr().getSourceRange();

                if (newVD.isFileVarDecl())
                    parser.diag(newVD.getLocation(), err_vla_decl_in_file_scope)
                            .addSourceRange(SizeRange);
                else if (newVD.getStorageClass() == StorageClass.SC_static)
                    parser.diag(newVD.getLocation(), err_vla_decl_has_static_storage)
                            .addSourceRange(SizeRange);
                else
                    parser.diag(newVD.getLocation(), err_vla_decl_has_extern_linkage)
                            .addSourceRange(SizeRange);
                newVD.setInvalidDecl(true);
                return;
            }

            if (FixedTy.isNull())
            {
                if (newVD.isFileVarDecl())
                    parser.diag(newVD.getLocation(), err_vm_decl_in_file_scope);
                else
                    parser.diag(newVD.getLocation(), err_vm_decl_has_extern_linkage);
                newVD.setInvalidDecl(true);
                return;
            }

            parser.diag(newVD.getLocation(), warn_illegal_constant_array_size);
            newVD.setDeclType(FixedTy);
            return;
        }

        if (ty.isVoidType() && !newVD.hasExternalStorage())
        {
            parser.diag(newVD.getLocation(), err_typecheck_decl_incomplete_type)
                    .addTaggedVal(ty);
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
        String name = d.getIdentifier();
        SourceLocation nameLoc = d.getIdentifierLoc();
        SCS scsSpec = d.getDeclSpec().getStorageClassSpec();
        StorageClass sc = storageClassSpecToVarDeclStorageClass(scsSpec);

        if (name == null)
        {
            parser.diag(d.getIdentifierLoc(), err_bad_variable_name);
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
                    parser.diag(d.getIdentifierLoc(), err_unsupported_global_register);
                else
                    parser.diag(d.getIdentifierLoc(), err_typecheck_sclass_fscope);
                d.setInvalidType(true);
            }
        }

        if (dc.isRecord() && !curContext.isRecord())
        {
            // This is an out-of-line definition of a static data member.
            if(sc == StorageClass.SC_static)
            {
                parser.diag(d.getDeclSpec().getStorageClassSpecLoc(),
                        err_static_out_of_line)
                        .addCodeModificationHint(
                        CodeModificationHint.createRemoval(
                        new SourceRange(d.getDeclSpec().getStorageClassSpecLoc())));
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

    private StorageClass  storageClassSpecToVarDeclStorageClass(SCS scsSpec)
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
                parser.diag(d.getDeclSpec().getStorageClassSpecLoc(),
                        err_typecheck_sclass_func);
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
                    parser.diag(d.getDeclSpec().getStorageClassSpecLoc(),
                            err_static_block_func);
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

    /**
     * Convert the jlang.type for the specified declarator to jlang.type instance.
     * @param d
     * @return
     */
    QualType getTypeForDeclarator(Declarator d)
    {
        // Determine the jlang.type of the declarator.
        DeclSpec ds = d.getDeclSpec();
        int declLoc = d.getIdentifierLoc();
        if (declLoc == SourceLocation.NOPOS)
            declLoc = ds.getSourceRange().getStart();

        QualType result = null;
        switch (ds.getTypeSpecType())
        {
            case TST_void:
                result = Type.VoidTy;
                break;
            case TST_char:
                if (ds.getTypeSpecSign() == DeclSpec.TSS.TSS_unspecified)
                    result = Type.CharTy;
                else if (ds.getTypeSpecSign() == DeclSpec.TSS.TSS_signed)
                    result = Type.SignedCharTy;
                else
                {
                    assert ds.getTypeSpecSign() == DeclSpec.TSS.TSS_unsigned
                            :"Unknown TSS value";
                    result = Type.UnsignedCharTy;
                }
                break;
            case TST_unspecified:
                if (!ds.hasTypeSpecifier())
                {
                    // C99 requires a jlang.type specifier.
                    parser.syntaxError(declLoc, "jlang.type specifier missing, defaults to 'int'");
                }
                // fall through
            case TST_int:
            {
                if (ds.getTypeSpecSign() != DeclSpec.TSS.TSS_unsigned)
                {
                    switch (ds.getTypeSpecWidth())
                    {
                        case TSW_unspecified:
                            result = Type.IntTy;break;
                        case TSW_short:
                            result = Type.ShortTy;break;
                        case TSW_long:
                            result = Type.LongTy;break;
                        case TSW_longlong:
                            parser.syntaxWarning(ds.getTypeSpecWidthLoc(),
                                    "long long jlang.type is the features in C99.");
                            break;
                    }
                }
                else
                {
                    switch (ds.getTypeSpecWidth())
                    {
                        case TSW_unspecified:
                            result = Type.UnsignedIntTy;break;
                        case TSW_short:
                            result = Type.UnsignedShortTy;break;
                        case TSW_long:
                            result = Type.UnsignedLongTy;break;
                        case TSW_longlong:
                            parser.syntaxWarning(ds.getTypeSpecWidthLoc(),
                                    "long long jlang.type is the features in C99.");
                            break;
                    }
                }
                break;
            }
            case TST_float:
                result = Type.FloatTy;break;
            case TST_double:
                result = Type.DoubleTy;break;
            case TST_bool:
                // _Bool
                result = Type.BoolTy;break;
            case TST_enum:
            case TST_struct:
            case TST_union:
            {
                TypeDecl typeDecl = (TypeDecl) ds.getRepAsDecl();
                if (typeDecl == null)
                {
                    result = Type.IntTy;
                    d.setInvalidType(true);
                    break;
                }

                assert ds.getTypeSpecWidth() == null && ds.getTypeSpecComplex() == null
                        && ds.getTypeSpecSign() == null:"No qualifiers on tag names!";

                // TypeQuals handled by caller.
                result = QualType.getTypeDeclType(typeDecl);

                if (typeDecl.isInvalidDecl())
                    d.setInvalidType(true);
                break;
            }
            case TST_typename:
            {
                assert ds.getTypeSpecWidth() == null
                        && ds.getTypeSpecComplex() == null
                        && ds.getTypeSpecSign() == null
                        :"Can't handle qualifiers on typedef names yet";

                result = ds.getRepAsType();
                if (result.isNull())
                    d.setInvalidType(true);
                break;
            }
            case TST_error:
                result = Type.IntTy;
                d.setInvalidType(true);
                break;
        }

        // Apply const/volatile/restrict qualifiers to T.
        if (ds.getTypeQualifier() != 0)
        {
            // Enforce C99 6.7.3p2: "Types other than pointer types derived from object
            // or incomplete types shall not be restrict-qualified."
            int typeQuals = ds.getTypeQualifier();
            if ((typeQuals & TQ_restrict.value) != 0)
            {
                if (result.isPointerType())
                {
                    QualType eleTy = result.getPointerType().getPointeeType();

                    // If we have a pointer, the pointee must have an object
                    // incomplete jlang.type.
                    if (!eleTy.isIncompleteOrObjectType())
                    {
                        parser.syntaxError(ds.getRestrictSpecLoc(),
                                "pointer to function jlang.type %s may not be 'restrict' qualified",
                                eleTy.toString());
                        typeQuals &= ~TQ_restrict.value;
                    }
                }
                else
                {
                    parser.syntaxError(ds.getRestrictSpecLoc(),
                            "restrict requires a pointer (%s is invalid)",
                            result.toString());
                    typeQuals &= ~TQ_restrict.value;
                }
            }

            // Warn about CV qualifiers on functions: C99 6.7.3p8: "If the specification
            // of a function jlang.type includes any jlang.type qualifiers, the behavior is
            // undefined."
            if (result.isFunctionType() && typeQuals != 0)
            {
                int loc;
                if ((typeQuals & TQ_const.value) != 0)
                    loc = ds.getConstSpecLoc();
                else if ((typeQuals & TQ_volatile.value) != 0)
                    loc = ds.getVolatileSpecLoc();
                else
                {
                    assert (typeQuals & TQ_restrict.value) != 0
                            :"Has CVR quals but not c, v, or R?";
                    loc = ds.getRestrictSpecLoc();
                }

                parser.syntaxError(loc, "jlang.type qualifiers can not applied into function jlang.type");
            }

            QualType.Qualifier quals = QualType.Qualifier.fromCVRMask(typeQuals);
            result = QualType.getQualifiedType(result, quals);
        }
        return result;
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

        // The return jlang.type of a function definition must be complete
        // (C99 6.9.1p3.
        QualType resultType = fd.getReturnType();
        if (!resultType.isVoidType() && !fd.isInvalidDecl())
            requireCompleteType(fd.getLocation(), resultType);

        fd.setInvalidDecl(true);

        if (fnBodyScope != null)
            pushDeclContext(fnBodyScope, fd);

        // Check the validity of our function parameters
        checkParmsForFunctionDef(fd.getParamInfo());

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

    private void checkForFunctionRedefinition(FunctionDecl fd)
    {

    }

    private void checkParmsForFunctionDef(ArrayList<ParamVarDecl> params)
    {

    }

    private void checkShadow(Scope s, VarDecl d)
    {
        LookupResult r = new LookupResult(this, d.getDeclName(), d.getLocation(),
                LookupOrdinaryName);
        lookupName(r, s);
        checkShadow(s, d, r);
    }

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

        // Emit warning and note.
        parser.syntaxWarning(r.getNameLoc(),
                "declaration shadows a variable %s",
                name);
        parser.syntaxError(shadowedDecl.getLocation(), "previous declaration is here");
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
            String name,
            SourceLocation loc)
    {
        NamedDecl res = lookupName(curScope, name, loc, LookupLabelName);
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
            SourceLocation loc,
            LabelDecl ld,
            SourceLocation colonLoc,
            ActionResult<Stmt> stmt)
    {
        // if the label was multiple defined, reject it and issue diagnostic
        if (ld.stmt != null)
        {
            // TODO report error
            return stmt;
        }

        // otherwise, things are well-form.
        Tree.LabelledStmt s = new Tree.LabelledStmt(ld, stmt.get(), colonLoc);
        ld.setStmt(s);
        return new ActionResult<Stmt>(s);
    }

    public ActionResult<Stmt> actOnCaseStmt(
            SourceLocation caseLoc,
            Expr expr,
            SourceLocation colonLoc)
    {
        assert expr != null : "missing expression within case statement";

        if (verifyIntegerConstantExpression(expr))
            return null;

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
        if (!expr.evaluate(evalResult) || !evalResult.val.isInt()
                || evalResult.hasSideEffects())
        {
            parser.diag(expr.getExprLocation(), err_expr_not_ice).
                    addSourceRange(expr.getSourceRange());
            if (evalResult.diag >= 0)
            {
                if (evalResult.diag != note_invalid_subexpr_in_ice
                        || !expr.ignoreParens().equals(evalResult.
                        diagExpr.ignoreParens()))
                {
                    parser.diag(evalResult.diagLoc, evalResult.diag);
                }
            }
            return true;
        }

        parser.diag(expr.getExprLocation(), ext_expr_not_ice)
                .addSourceRange(expr.getSourceRange());

        if (evalResult.diag >= 0 && Diagnostics.getDiagnosticLevel(ext_expr_not_ice)
                != Diagnostics.Level.Ignored)
            parser.diag(evalResult.diagLoc, evalResult.diag);

        if (result.get() != null)
            result.set(evalResult.val.getInt());

        return false;
    }

    public void actOnCaseStmtBody(Stmt stmt, Stmt subStmt)
    {
        assert stmt != null;
        CaseStmt cs = (CaseStmt) stmt;
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
    
    private void diagnoseUnusedExprResult(Stmt stmt)
    {
        // TODO: 2017/3/28
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
                parser.diag(d.getLocation(), ext_mixed_decls_code);
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
        return new ActionResult<>(
                new IfStmt(condExpr.get(), thenStmt, elseStmt, ifLoc));
    }

    /**
     * Attempt to convert a given expression to integeral or enumerate jlang.type.
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
        // if the subExpr already is a integral or enumeration jlang.type, we got it.
        if (!t.getType().isIntegralOrEnumerationType())
        {
            parser.diag(switchLoc, err_typecheck_expect_scalar_operand);
        }
        return new ActionResult<>(expr);
    }

    /**
     * Perform the default conversion of arrays and functions to pointers.
     * Return the result of converting EXP.  For any other expression, just
     * return EXP after removing NOPs.
     */
    private ActionResult<Expr> defaultFunctionArrayConversion(Expr expr)
    {
        QualType ty = expr.getType();
        assert !ty.isNull() : "DefaultFunctionArrayConversion - missing jlang.type.";
        if (ty.getType().isFunctionType())
            expr = implicitCastExprToType(expr, ty, EVK_RValue,
                    CK_FunctionToPointerDecay).get();
        else if (ty.getType().isConstantArrayType())
        {
            if (expr.isLValue())
            {
                expr = implicitCastExprToType(expr,
                        QualType.getArrayDecayedType(ty), EVK_RValue,
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
    private ActionResult<Expr> usualUnaryConversion(Expr expr)
    {
        ActionResult<Expr> res = defaultFunctionArrayConversion(expr);
        if (res.isInvalid())
            return new ActionResult<>(expr);
        expr = res.get();

        QualType t = expr.getType();
        assert t != null : "UsualUnaryConversion - missing jlang.type";

        // try to perform integral promotions if the object has a promotable jlang.type.
        if (t.getType().isIntegralOrEnumerationType())
        {
            QualType ty = expr.isPromotableBitField();
            if (!ty.isNull())
            {
                expr = implicitCastExprToType(expr, ty, EVK_RValue,
                        CK_IntegralCast).get();
                return new ActionResult<>(expr);
            }
            if (ty.isPromotableIntegerType())
            {
                QualType qt = ty.getPromotedIntegerType();
                expr = implicitCastExprToType(expr, qt, EVK_RValue,
                        CK_IntegralCast).get();
                return new ActionResult<>(expr);
            }
        }
        return new ActionResult<>(expr);
    }

    /**
     * If the {@code expr} is not of jlang.type 'Type', perform an operation of inserting
     * cast jlang.type for implicitly jlang.type casting.
     * </br>
     * If there is already an implicit cast, merge into the existing one.
     *
     * @param expr The expression to be casted.
     * @param ty   The TargetData jlang.type which expr would be casted to.
     * @param kind The kind of jlang.type cast.
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

    public ActionResult<Stmt> actOnStartOfSwitchStmt(int switchLoc,
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
        condResult = usualUnaryConversion(condExpr);
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
        if (functionScopes.isEmpty())
            return null;
        return functionScopes.peek();
    }

    public ActionResult<Stmt> actOnFinishSwitchStmt(
            SourceLocation switchLoc,
            Stmt switchStmt,
            Stmt body)
    {
        assert (switchStmt instanceof SwitchStmt) : "stmt must be switch stmt.";
        SwitchStmt ss = (SwitchStmt) switchStmt;
        assert ss == getCurFunction().switchStack
                .peek() : "switch stack missing push/pop";
        ss.setBody(body);
        getCurFunction().switchStack.pop();

        Expr condExpr = ss.getCond();
        if (condExpr == null)
            return stmtError();

        QualType condType = condExpr.getType();
        Expr condExprBeforePromotion = condExpr;
        QualType condTypeBeforePromotion = getTypeBeforeIntegralPromotion(
                condExprBeforePromotion);

        long condWidth = condExprBeforePromotion.getIntWidth();
        boolean condIsSigned = condExprBeforePromotion.
                isSignedIntegerOrEnumeration();

        // Accumulate all of the case values in a vector so that we can sort them
        // and detect duplicates.
        // This vector contains the int for the case after it has been converted to
        // the condition jlang.type.
        Vector<Pair<APSInt, SwitchCase>> caseLists = new Vector<>();
        DefaultStmt prevDefaultStmt = null;
        boolean caseListErroneous = false;

        for (SwitchCase sc = ss.getSwitchCaseList();
             sc != null; sc = sc.getNextCaseStmt())
        {
            if (sc.tag == Tree.DefaultStmtClass)
            {
                DefaultStmt ds = (DefaultStmt) sc;
                if (prevDefaultStmt != null)
                {
                    // TODO report error
                    parser.syntaxError(ds.defaultLoc,
                            "multiple default statement defined");
                    parser.syntaxError(prevDefaultStmt.defaultLoc,
                            "previous default statement default here");
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
                APSInt loVal = lo.evaluateKnownConstInt();
                convertIntegerToTypeWarnOnOverflow(loVal, condWidth,
                        condIsSigned, lo.getExprLocation(),
                        "warn case value overflow");

                // if the case constant is not the same jlang.type as the condition, insert
                // an implicit cast

                lo = implicitCastExprToType(lo, condType, EVK_RValue,
                        CK_IntegralCast).get();
                cs.setCondExpr(lo);

                caseLists.add(new Pair<>(loVal, cs));
            }

            // If we don't have a default statement, check whether the
            // condition is constant.
            // TODO complete the sematic validate for case substatement.
            // TODO reference to LLVM SemaStmt.cpp:657.
            APSInt constantCondValue = null;
            boolean hasConstantCond = false;
            boolean shouldCheckConstantCond = false;
            if (prevDefaultStmt == null)
            {
                Expr.EvalResult result = condExprBeforePromotion.evaluate();
                hasConstantCond = result != null;
                if (hasConstantCond)
                {
                    assert result.getValue()
                            .isInt() : "switch condition evaluated to non-int";
                    constantCondValue = result.getValue().getInt();
                    shouldCheckConstantCond = true;

                    int len = constantCondValue.getBitWidth();
                    assert (len == condWidth
                            && constantCondValue.isSigned() == condIsSigned);
                }
            }

            // sort all the scalar case value so we can easily detect duplicates.
            caseLists.sort(new Comparator<Pair<APSInt, SwitchCase>>()
            {
                @Override public int compare(Pair<APSInt, SwitchCase> lhs,
                        Pair<APSInt, SwitchCase> rhs)
                {
                    if (lhs.first.lt(rhs.first))
                        return -1;
                    if (lhs.first.eq(rhs.first)
                            && lhs.second.getCaseLoc() < rhs.second
                            .getCaseLoc())
                        return -1;
                    return 1;
                }
            });

            if (!caseLists.isEmpty())
            {
                for (int i = 0, e = caseLists.size(); i < e; i++)
                {
                    Pair<APSInt, SwitchCase> Case = caseLists.get(i);
                    if (shouldCheckConstantCond && Case.first
                            .eq(constantCondValue))
                    {
                        shouldCheckConstantCond = false;
                    }

                    if (i != 0 && Case.first.eq(caseLists.get(i - 1).first))
                    {
                        // TODO If we have a duplicate, report it.
                        parser.syntaxError(Case.second.getCaseLoc(),
                                "duplicate case " + Case.first.toString(10));
                        Pair<APSInt, SwitchCase> prevDup = caseLists.get(i - 1);
                        parser.syntaxError(prevDup.second.getCaseLoc(),
                                "previous duplicate case" + prevDup.first
                                        .toString(10));

                        caseListErroneous = true;
                    }
                }
            }

            // complain if we have a constant condition and we didn't find a match.
            if (!caseListErroneous && shouldCheckConstantCond)
            {
                parser.syntaxWarning(condExpr.getExprLocation(),
                        "missing case for condition",
                        constantCondValue.toString(10));
            }

            // Check to see if switch is over an Enum and handles all of its
            // values.  We only issue a warning if there is not 'default:', but
            // we still do the analysis to preserve this information in the AST
            // (which can be used by flow-based analyes).
            //
            final EnumType et = condTypeBeforePromotion.getType().getEnumType();
            // if switch has default case, the ignore it.
            if (!caseListErroneous && !hasConstantCond && et != null)
            {
                final EnumDecl ed = et.getDecl();
                ArrayList<Pair<APSInt, EnumConstantDecl>> enumVals = new ArrayList<>(
                        64);
                // gather all enum values, set their jlang.type and sort them.
                // allowing easier comparision with caseLists.
                for (Iterator<Decl> it = ed.iterator(); it.hasNext(); )
                {
                    EnumConstantDecl enumDecl = (EnumConstantDecl) it.next();
                    APSInt val = enumDecl.getInitValue();
                    adjustAPSInt(val, condWidth, condIsSigned);
                }

                enumVals.sort(new Comparator<Pair<APSInt, EnumConstantDecl>>()
                {
                    @Override
                    public int compare(Pair<APSInt, EnumConstantDecl> o1,
                            Pair<APSInt, EnumConstantDecl> o2)
                    {
                        if (o1.first.lt(o2.first))
                            return 1;
                        else
                            return -1;
                    }
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
                            parser.syntaxWarning(ci.second.getCaseLoc(),
                                    "enum constant not in enum",
                                    ed.getDeclName());
                        }
                    }
                }

                // Check which enum values are not in switch statement
                boolean hasCaseNotInSwitch = false;
                Iterator<Pair<APSInt, SwitchCase>> ci = caseLists.iterator();
                ArrayList<String> unhandledNames = new ArrayList<>(8);

                for (Pair<APSInt, EnumConstantDecl> ei : enumVals)
                {
                    APSInt ciVal;

                    while (ci.hasNext() && ci.next().first.lt(ei.first))
                        ;

                    if (ci.hasNext() && ci.next().first.eq(ei.first))
                        continue;

                    hasCaseNotInSwitch = true;
                    if (prevDefaultStmt == null)
                    {
                        unhandledNames.add(ei.second.getDeclName());
                    }
                }

                switch (unhandledNames.size())
                {
                    case 0:
                        break;
                    case 1:
                        parser.syntaxWarning(condExpr.getExprLocation(),
                                "missing one case", unhandledNames.get(0));
                        break;
                    case 2:
                        parser.syntaxWarning(condExpr.getExprLocation(),
                                "missing cases", unhandledNames.get(0),
                                unhandledNames.get(1));
                        break;
                    default:
                        parser.syntaxWarning(condExpr.getExprLocation(),
                                "missing cases", unhandledNames.get(0),
                                unhandledNames.get(1), unhandledNames.get(2));
                        break;
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

    private void convertIntegerToTypeWarnOnOverflow(APSInt loVal,
            long condWidth, boolean condIsSigned, SourceLocation loc, String dign)
    {
        // TODO: 2017/3/28
    }

    /**
     * Returns the pre-promoted qualified jlang.type of each expression.
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
        if (cond == null)
            return stmtError();
        // TODO diagnostic unused expression results.
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
            parser.syntaxError(loc,
                    "statement requires expression of scalar jlang.type",
                    "(" + t.getType() + "invalid)");
            return exprError();
        }

        return new ActionResult<>(cond);
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
        // TODO: 2017/3/28
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

        // TODO dignostic unused expression result.
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
    public Decl parseFreeStandingDeclSpec(Scope s, DeclSpec ds)
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
            if ((typeQuals & TQ_restrict.value) != 0)
                parser.syntaxError(ds.getRestrictSpecLoc(),
                        "restrict requires a pointer or reference");
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
                parser.syntaxError(ds.getSourceRange().getStart(),
                        "typedef requires a getIdentifier");
                return tag;
            }

            parser.syntaxError(ds.getSourceRange().getStart(),
                    "declaration does not declare anything");
            emittedWarning = true;
        }

        // We're going to complain about a bunch of spurious specifiers;
        // only do this if we're declaring a tag, because otherwise we
        // should be getting ext_no_declarators.
        if (emittedWarning || (tagD != null && tagD.isInvalidDecl()))
            return tagD;

        if (ds.getTypeQualifier() > 0)
        {
            if ((ds.getTypeQualifier() & TQ_const.value) != 0)
                parser.syntaxWarning(ds.getConstSpecLoc(),
                        "const ignored on this declaration");
            if ((ds.getTypeQualifier() & TQ_volatile.value) != 0)
                parser.syntaxWarning(ds.getVolatileSpecLoc(),
                        "volatile ignored on this declaration");
        }

        if (ds.isInlineSpecifier())
            parser.syntaxWarning(ds.getInlineSpecLoc(),
                    "inline ignored on this declaration");

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
                    // declared with no linkage (C99 6.2.2p6), the jlang.type for the
                    // object shall be complete.
                    if (var.isLocalVarDecl() && !var.getLinkage() && !var.isInvalidDecl()
                            && requireCompleteType(var.getLocation(), type))
                        var.setInvalidDecl(true);
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
                        final ArrayType.IncompleteArrayType arrayT = type.getAsInompleteArrayType();
                        if (arrayT != null)
                        {
                            if (requireCompleteType(var.getLocation(), arrayT.getElemType()))
                                var.setInvalidDecl(true);
                        }
                        else if (var.getStorageClass() == StorageClass.SC_static)
                        {
                            // C99 6.9.2p3: If the declaration of an identifier for an object is
                            // a tentative definition and has internal linkage (C99 6.2.2p3), the
                            // declared jlang.type shall not be an incomplete jlang.type.
                            // NOTE: code such as the following
                            //     static struct s;
                            //     struct s { int a; };
                            // is accepted by gcc. Hence here we issue a warning instead of
                            // an error and we do not invalidate the static declaration.
                            // NOTE: to avoid multiple warnings, only check the first declaration.
                            if (var.getPreviousDeclaration() == null)
                                requireCompleteType(var.getLocation(), type);
                        }
                    }
                    return;
                }
            }

            // Provide a specific diagnostic for uninitialized variable
            // definitions with incomplete array jlang.type.
            if (type.isIncompleteArrayType())
            {
                parser.syntaxError(var.getLocation(),
                        "definition of variable with array jlang.type needs an explicit getNumOfSubLoop or an initializer");
                var.setInvalidDecl(true);
                return;
            }

            if (var.isInvalidDecl())
                return;

            if (requireCompleteType(var.getLocation(), QualType.getBaseElementType(type)))
            {
                var.setInvalidDecl(true);
                return;
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
                        parser.syntaxError(d.getLocation(),
                                "non-variable declaration in 'for' loop");
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
            parser.syntaxError(continueLoc,
                    "'continue' statement not in loop statement");
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
            parser.syntaxError(breakLoc,
                    "'break' statement not in loop or switch statement");
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
            Expr e)
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
            if (e != null)
            {
                String diag = "void function should not return a value";
                // C99 6.8.6.4p1
                if (e.getType().isVoidType())
                    diag = "void function should not return void expression";
                else
                {
                    ActionResult<Expr> result = new ActionResult<>(e);
                    result = ignoreValueConversion(e);
                    if (result.isInvalid())
                        return stmtError();
                    e = implicitCastExprToType(e, Type.VoidTy, EVK_RValue,
                            CK_ToVoid).get();
                }

                parser.syntaxError(e.getExprLocation(), diag);

                checkImplicitConversion(e, returnLoc);
            }
            res = new ReturnStmt(returnLoc, e);
        }
        else if (e == null)
        {
            parser.syntaxError(returnLoc,
                    "non void function should return a value at",
                    getCurFunctionDecl().getDeclName());
            res = new ReturnStmt(returnLoc);
        }
        else
        {
            // we have a non-void function with an expression, continue checking

            // C99 6.8.6.4p3(136): The return statement is not an assignment. The
            // overlap restriction of subclause 6.5.16.1 does not apply to the case of
            // function return.
            checkReturnStackAddress(e, retType, returnLoc);
        }

        if (e != null)
        {
            if (declaredRetType != retType)
            {
                ActionResult<Expr> result = performImplicitConversion(e,
                        declaredRetType);
                if (result.isInvalid())
                    return stmtError();
                e = result.get();
            }

            checkImplicitConversion(e, returnLoc);
        }
        res = new ReturnStmt(returnLoc, e);

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
            decayedTy = new PointerType(
                    srcFrom.getConstantArrayType().getElementType());
            resTy = new QualType(decayedTy, QualType.CONST_QUALIFIER);
            cast = CastKind.CK_ArrayToPointerDecay;
        }
        else if (srcFrom.isFunctionType())
        {
            decayedTy = new PointerType(srcFrom.getFunctionType());
            resTy = new QualType(decayedTy);
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
        if (!resTy.isCompatible(toType))
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
    private void checkReturnStackAddress(Expr retValExpr, QualType retType,
            int returnLoc)
    {
        // TODO: 2017/3/28
    }

    private ActionResult<Expr> ignoreValueConversion(Expr e)
    {
        if (e.isRValue())
        {
            if (e.getType().isFunctionType())
                return defaultFunctionArrayConversion(e);

            return new ActionResult<>(e);
        }

        if (!e.getType().isVoidType())
            parser.syntaxError(e.getExprLocation(), "imcomplete jlang.type ",
                    e.getType().toString(), " where required complete jlang.type");
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
        return expr.getType().isIntegralOrEnumerationType();
    }

    /**
     * Binary Operators.  'Tok' is the token for the operator.
     * @return
     */
    public ActionResult<Expr> actOnBinOp(
            SourceLocation tokLoc,
            int tokenKind,
            Expr lhs,
            Expr rhs)
    {
        BinaryOperatorKind operatorKind = convertTokenKindToBinaryOpcode(tokenKind);
        assert lhs!= null:"actOnBinOp(): missing lhs!";
        assert rhs!=null:"actOnBinOp(): missing rhs!";

        // TODO Emit warnings for tricky precedence issues, e.g. "bitfield & 0x4 == 0"
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

        // Result jlang.type of binary operator.
        QualType resultTy = new QualType();

        // The following two variables are used for compound assignment operators
        QualType compLHSTy = new QualType();    // Type of LHS after promotions for computation
        QualType compResultTy = new QualType(); // Type of computation result
        ExprValueKind VK = EVK_RValue;

        switch (opc)
        {
            case BO_Assign:
                resultTy = checkAssignmentOperands();
                break;
            case BO_Mul:
            case BO_Div:
                resultTy = checkMultiplyDivideOperands(lhsExpr, rhsExpr, opLoc,
                        false, opc == BO_Div);
                break;
            case BO_Rem:
                resultTy = checkRemainderOperands(lhsExpr, rhsExpr, opLoc, false);
                break;
            case BO_Add:
                resultTy = checkAdditionOperands(lhsExpr, rhsExpr, opLoc);
                break;
            case BO_Sub:
                resultTy = checkSubtractionOperands(lhsExpr, rhsExpr, opLoc);
                break;
            case BO_Shl:
            case BO_Shr:
                resultTy = checkShiftOperands(lhsExpr, rhsExpr, opLoc, opc);
                break;
            case BO_LE:
            case BO_LT:
            case BO_GE:
            case BO_GT:
                resultTy = checkComparisonOperands(lhsExpr, rhsExpr, opLoc, opc, true);
                break;
            case BO_EQ:
            case BO_NE:
                resultTy = checkComparisonOperands(lhsExpr, rhsExpr, opLoc, opc, false);
                break;
            case BO_And:
            case BO_Xor:
            case BO_Or:
                resultTy = checkBitwiseOperands(lhsExpr, rhsExpr, opLoc);
                break;
            case BO_LAnd:
            case BO_LOr:
                resultTy = checkLogicalOperands(lhsExpr, rhsExpr, opLoc, opc);
                break;
            case BO_MulAssign:
            case BO_DivAssign:
                compResultTy = checkMultiplyDivideOperands(lhsExpr, rhsExpr, opLoc, true, opc == BO_DivAssign);
                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !lhsExpr.isInvalid() && !rhsExpr.isInvalid())
                    resultTy = checkAssignmentOperands(lhsExpr.get(), rhsExpr, opLoc, compResultTy);
                break;
            case BO_RemAssign:
                compResultTy = checkRemainderOperands(lhsExpr, rhsExpr, opLoc, true);
                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !lhsExpr.isInvalid() && !rhsExpr.isInvalid())
                    resultTy = checkAssignmentOperands(lhsExpr.get(), rhsExpr, opLoc, compResultTy);
                break;
            case BO_AddAssign:
            {
                OutParamWrapper<QualType> x = new OutParamWrapper<>(compLHSTy);
                compResultTy = checkAdditionOperands(lhsExpr, rhsExpr, opLoc, x);
                compLHSTy = x.get();

                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !lhsExpr.isInvalid() && !rhsExpr.isInvalid())
                    resultTy = checkAssignmentOperands(lhsExpr.get(), rhsExpr,
                            opLoc, compResultTy);
                break;
            }
            case BO_SubAssign:
            {
                OutParamWrapper<QualType> x = new OutParamWrapper<>(compLHSTy);
                compResultTy = checkSubtractionOperands(lhsExpr, rhsExpr, opLoc,
                        x);
                compLHSTy = x.get();

                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !lhsExpr.isInvalid() && !rhsExpr.isInvalid())
                    resultTy = checkAssignmentOperands(lhsExpr.get(), rhsExpr,
                            opLoc, compResultTy);
                break;
            }
            case BO_ShrAssign:
            case BO_ShlAssign:
            {
                compResultTy = checkShiftOperands(lhsExpr, rhsExpr, opLoc, opc, true);

                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !lhsExpr.isInvalid() && !rhsExpr.isInvalid())
                    resultTy = checkAssignmentOperands(lhsExpr.get(), rhsExpr,
                            opLoc, compResultTy);
                break;
            }
            case BO_AndAssign:
            case BO_XorAssign:
            case BO_OrAssign:
            {
                compResultTy = checkBitwiseOperands(lhsExpr, rhsExpr, opLoc, true);

                compLHSTy = compResultTy;
                if (!compResultTy.isNull() && !lhsExpr.isInvalid() && !rhsExpr.isInvalid())
                    resultTy = checkAssignmentOperands(lhsExpr.get(), rhsExpr,
                            opLoc, compResultTy);
                break;
            }
            case BO_Comma:
                resultTy = checkCommaOperands(lhsExpr, rhsExpr, opLoc);
                break;
        }

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

            if (commonExpr.getValuekind() == rhsExpr.getValuekind()
                    && commonExpr.getType().isSameType(rhsExpr.getType()))
            {
                ActionResult<Expr> commonRes = usualUnaryConversion(commonExpr);
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
            ActionResult<Expr> rhs,
            SourceLocation loc,
            QualType compoundType)
    {
        return null;
    }

    private QualType checkMultiplyDivideOperands(
            ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            int opLoc,
            boolean isCompAssign,
            boolean isDiv)
    {
        QualType compType = usualArithmeticConversions(lhs, rhs, isCompAssign);

        if (lhs.isInvalid() || rhs.isInvalid())
            return new QualType();

        if (!lhs.get().getType().isArithmeticType()
                || !rhs.get().getType().isArithmeticType())
        {
            return invalidOperands(opLoc, lhs, rhs);
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
            int loc,
            ActionResult<Expr> lhs,
            ActionResult<Expr> rhs)
    {
        parser.syntaxError(loc,
                "invalid operands to binary expression (%s and %s)",
                lhs.get().getType().toString(),
                rhs.get().getType().toString());
        return new QualType();
    }

    /**
     * Performs various conversions that are common to
     * binary operators (C99 6.3.1.8). If both operands aren't arithmetic, this
     * routine returns the first non-arithmetic jlang.type found. The client is
     * responsible for emitting appropriate error diagnostics.
     * @return
     */
    private QualType usualArithmeticConversions(
            ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            boolean isCompAssign)
    {
        if (!isCompAssign)
        {
            lhs = usualUnaryConversion(lhs.get());
            if (lhs.isInvalid())
                return new QualType();
        }

        rhs = usualUnaryConversion(rhs.get());
        if (lhs.isInvalid())
            return new QualType();

        // For conversion purposes, we ignore any qualifiers.
        // For example, "const float" and "float" are equivalent.
        QualType lhsType = lhs.get().getType().clearQualified();
        QualType rhsType = rhs.get().getType().clearQualified();

        // if both types are identical, no conversions is desired.
        if (lhsType.equals(rhsType))
            return lhsType;

        // If either side is a non-arithmetic jlang.type (e.g. a pointer), we are done.
        // The caller can deal with this (e.g. pointer + int).
        if (!lhsType.isArithmeticType() || !rhsType.isArithmeticType())
            return lhsType;

        // Apply unary and bitfield promotions to the LHS's jlang.type.
        QualType lhsUnpromotedType = lhsType;
        if (lhsType.isPromotableIntegerType())
            lhsType = lhsType.getPromotedIntegerType();
        QualType lhsBitfieldPromoteTy = lhs.get().isPromotableBitField();
        if (!lhsBitfieldPromoteTy.isNull())
            lhsType = lhsBitfieldPromoteTy;
        if (lhsType != lhsUnpromotedType && !isCompAssign)
            lhs = implicitCastExprToType(lhs.get(), lhsType, EVK_RValue,CK_IntegralCast);

        // if both types are identical, no conversions is desired.
        if (lhsType.equals(rhsType))
            return lhsType;

        // At this point, we have two different arithmetic jlang.type.

        // Handle complex types first (C99 6.3.1.8p1)
        if (lhsType.isComplexType() || rhsType.isComplexType())
            return handleComplexFloatConversion(this, );

        // Now deal with real types, e.g. "float", "double", "long double".
        if (lhsType.isRealType() || rhsType.isRealType())
            return handleFloatConversion();

        // Finally, we have two differing integer types
        return handleIntegerConversion();

    }

    private QualType checkRemainderOperands(
            ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            int opLoc,
            boolean isCompAssign)
    {
        QualType compType = usualArithmeticConversions(lhs, rhs, isCompAssign);
        if (lhs.isInvalid() || rhs.isInvalid())
            return new QualType();

        if (!lhs.get().getType().isIntegerType()
                || !rhs.get().getType().isIntegerType())
            return invalidOperands(opLoc, lhs, rhs);

        /**
         * TODO check for rem by zero.
         *
         */
        return compType;
    }

    private QualType checkAdditionOperands(ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            int opLoc,
            OutParamWrapper<QualType> compLHSTy)
    {
        QualType compType = usualArithmeticConversions(lhs, rhs, compLHSTy!=null);
        if (lhs.isInvalid() || rhs.isInvalid())
            return new QualType();

        // Handle the common case, both two operands are arithmetic jlang.type.
        if (lhs.get().getType().isArithmeticType()
                && rhs.get().getType().isArithmeticType())
        {
            if (compLHSTy != null) compLHSTy.set(compType);
            return compType;
        }

        // Put any potential pointer into pExpr.
        Expr pExp = lhs.get(), iExp = rhs.get();
        if (iExp.getType().isPointerType())
            Util.swap(pExp, iExp);

        if (!pExp.getType().isPointerType() ||
                !iExp.getType().isIntegerType())
            return invalidOperands(opLoc, lhs, rhs);

        if (!checkArithmeticOpPointerOperand(opLoc, pExp))
            return new QualType();
        /**
         * TODO check array bounds for pointer arithmetic.
         *
         */
        checkArrayAccess(pExp, iExp);
        if (compLHSTy != null)
        {
            QualType lhsTy = lhs.get().isPromotableBitField();
            if (lhsTy.isNull())
            {
                lhsTy = lhs.get().getType();
                if (lhsTy.isPromotableIntegerType())
                    lhsTy = lhsTy.getPromotedIntegerType();
            }
            compLHSTy.set(lhsTy);
        }
        return pExp.getType();
    }

    private QualType checkAdditionOperands(ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            int opLoc)
    {
        return checkAdditionOperands(lhs, rhs, opLoc, null);
    }

    private boolean checkArithmeticOpPointerOperand(
            int loc, Expr lhs, Expr rhs)
    {
        boolean isLHSPointer = lhs.getType().isPointerType();
        boolean isRHSPointer = rhs.getType().isPointerType();

        if (!isLHSPointer && !isRHSPointer) return true;

        QualType lhsPointeeTy = new QualType() , rhsPointeeTy = new QualType();
        if (isLHSPointer) lhsPointeeTy = lhs.getType().getPointee();
        if (isRHSPointer) rhsPointeeTy = rhs.getType().getPointee();

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

        QualType pointeeTy = operand.getType().getPointee();
        if (pointeeTy.isVoidType())
        {
            parser.syntaxError(loc, "arithmetic on a pointer to void");
            return true;
        }
        if (pointeeTy.isFunctionType())
        {
            parser.syntaxError(loc,
                    "arithmetic on a pointer to the function jlang.type '%s' is a GNU extension",
                    pointeeTy.toString());
            return true;
        }

        return !checkArithmeticIncompletePointerType(loc, operand);
    }

    private void diagnoseArithmeticOnVoidPointer(int loc, Expr expr)
    {
        parser.syntaxError(loc, "arithmetic on a pointer to void a GNU extension");
    }

    private void diagnoseArithmeticOnTwoVoidPointers(int loc, Expr lhs, Expr rhs)
    {
        parser.syntaxError(loc, "arithmetic on a pointer to void a GNU extension");
    }

    private void diagnoseArithmeticOnFunctionPointer(int loc, Expr operand)
    {
        parser.syntaxError(loc,
                "arithmetic on a pointer to the function jlang.type '%s' is a GNU extension",
                operand.getType().getPointee().toString());
    }

    private void diagnoseArithmeticOnTwoFunctionPointers(int loc, Expr lhs, Expr rhs)
    {
        parser.syntaxError(loc,
                "arithmetic on a pointer to the function jlang.type '%s' is a GNU extension",
                lhs.getType().getPointee().toString());
    }

    /**
     *  Emit error if Operand is incomplete pointer jlang.type.
     * @return
     */
    private boolean checkArithmeticIncompletePointerType(int loc, Expr op)
    {
        if (op.getType().isPointerType())
        {
            QualType pointeeTy = op.getType().getPointee();
            if (requireCompleteType(loc, pointeeTy))
                return true;
        }
        return false;
    }

    /**
     * Ensure that the specified jlang.type is complete.
     * <br>
     * This routine checks whether the jlang.type {@code t} is complete in the any context
     * where complete jlang.type is required. If {@code t} is a complete jlang.type, returns
     * false. if failed, issues the diagnostic {@code diag} info and return true.
     * @param loc The location in the source code where the diagnostic message
     *            should refer.
     * @param t The jlang.type that this routine is examining for complete.
     * @param diag The diagnostic message.
     * @return Return true if {@code t} is not a complete jlang.type, false otherwise.
     */
    private boolean requireCompleteType(
            SourceLocation loc,
            QualType t)
    {
        if (!t.isIncompleteType())
            return false;

        // If we have a array jlang.type with constant getNumOfSubLoop, attempt to instantiate it.
        QualType elemType = t;
        ArrayType.ConstantArrayType array = t.getAsConstantArrayType();
        if (array != null)
            elemType = array.getElemType();

        final TagType tag = elemType.<TagType>getAs();
        // Avoids diagnostic invalid decls as incomplete.
        if (tag != null && tag.getDecl().isInvalidDecl())
            return true;

        // We have an incomplete jlang.type, producing dianostic message.
        // If the jlang.type was a forward declaration of a struct/union jlang.type
        // produce a error.
        if (tag != null && !tag.getDecl().isInvalidDecl())
            parser.syntaxError(tag.getDecl().getLocation(),
                    tag.isBeingDefined()?"definition of %s is not complete until the closing '}'":
                            "forward declaration of %s",
                    new QualType(tag).toString());
        return true;
    }

    private void checkArrayAccess(Expr pExpr, Expr iExpr)
    {

    }

    private void checkArrayAccess(final Expr e)
    {

    }

    private QualType checkSubtractionOperands(ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            int opLoc)
    {
        return checkSubtractionOperands(lhs, rhs, opLoc, null);
    }

    private QualType checkSubtractionOperands(ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            int opLoc,
            OutParamWrapper<QualType> compLHSTy)
    {
        QualType compType = usualArithmeticConversions(lhs, rhs, compLHSTy != null);
        if (lhs.isInvalid() || rhs.isInvalid())
            return new QualType();

        // Handle the common case, both two operands are arithmetic jlang.type.
        if (lhs.get().getType().isArithmeticType()
                && rhs.get().getType().isArithmeticType())
        {
            if (compLHSTy!=null) compLHSTy.set(compType);
            return compType;
        }

        // Either ptr - int  or ptr - ptr.
        if (lhs.get().getType().isPointerType())
        {
            QualType lPointee = lhs.get().getType().getPointee();

            // The case is ptr - int.
            if (rhs.get().getType().isIntegerType())
            {
                if (!checkArithmeticOpPointerOperand(opLoc, lhs.get()))
                    return new QualType();

                Expr iExpr = rhs.get().ignoreParenCasts();
                UnaryExpr negRex = new UnaryExpr(iExpr, UO_Minus,
                        iExpr.getType(),
                        EVK_RValue,
                        iExpr.getExprLocation());
                checkArrayAccess(lhs.get().ignoreParenCasts(), negRex);
                if (compLHSTy != null) compLHSTy.set(lhs.get().getType());
                return lhs.get().getType();
            }

            // handle ptr - ptr case
            if (rhs.get().getType().isPointerType())
            {
                final PointerType rhsPtry = rhs.get().getType().getPointerType();
                QualType rpointee = rhsPtry.getPointee();

                // Pointee types must be compatible C99 6.5.6p3
                if (!lPointee.isCompatible(rpointee))
                {
                    parser.syntaxError(opLoc,
                            "%diff %s and %s are not pointers to compatible types",
                            lhs.get().getType().toString(),
                            rhs.get().getType().toString());
                    return new QualType();
                }

                if (!checkArithmeticOpPointerOperand(opLoc, lhs.get(), rhs.get()))
                    return new QualType();

                if (compLHSTy!= null) compLHSTy.set(lhs.get().getType());
                return Type.IntTy;
            }

            return invalidOperands(opLoc, lhs, rhs);
        }

        // Put any potential pointer into pExpr.
        Expr pExp = lhs.get(), iExp = rhs.get();
        if (iExp.getType().isPointerType())
            Util.swap(pExp, iExp);

        if (!pExp.getType().isPointerType() ||
                !iExp.getType().isIntegerType())
            return invalidOperands(opLoc, lhs, rhs);

        if (!checkArithmeticOpPointerOperand(opLoc, pExp))
            return new QualType();
        /**
         * TODO check array bounds for pointer arithmetic.
         *
         */
        checkArrayAccess(pExp, iExp);

        return pExp.getType();
    }

    private QualType checkShiftOperands(ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            int opLoc,
            BinaryOperatorKind opc)

    {
        return checkShiftOperands(lhs, rhs, opLoc, opc, false);
    }

    private QualType checkShiftOperands(ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            int opLoc,
            BinaryOperatorKind opc,
            boolean isCompAssign)
    {
        if (!lhs.get().getType().isIntegerType()
                || !rhs.get().getType().isIntegerType())
        {
            return invalidOperands(opLoc, lhs, rhs);
        }

        // Shifts don't perform usual arithmetic conversions, they just do integer
        // promotions on each operand. C99 6.5.7p3

        // For the LHS, do usual unary conversions, but then reset them away
        // if this is a compound assignment.
        ActionResult<Expr> oldLHS = lhs;

        lhs = usualUnaryConversion(lhs.get());
        if (lhs.isInvalid())
            return new QualType();

        QualType lhsType = lhs.get().getType();
        if (isCompAssign) lhs = oldLHS;

        // The rhs is simpler
        rhs = usualUnaryConversion(rhs.get());
        if (rhs.isInvalid())
            return new QualType();

        // TODO DiagnoseBadShiftValues
        return lhsType;
    }

    private QualType checkComparisonOperands(ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            SourceLocation opLoc,
            BinaryOperatorKind opc,
            boolean isRelational)
    {
        // TODO: 2017/3/28
        return null;
    }

    private QualType checkBitwiseOperands(
            ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            SourceLocation opLoc)
    {
        return checkBitwiseOperands(lhs, rhs, opLoc, false);
    }

    private QualType checkBitwiseOperands(
            ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            SourceLocation opLoc,
            boolean isCompAssign)
    {
        // TODO: 2017/3/28
        return null;
    }

    private QualType checkLogicalOperands(
            ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            SourceLocation opLoc,
            BinaryOperatorKind opc)
    {
        // TODO: 2017/3/28
        return null;
    }

    private QualType checkCommaOperands(
            ActionResult<Expr> lhs,
            ActionResult<Expr> rhs,
            SourceLocation loc)
    {
        return null;
    }

    public static BinaryOperatorKind convertTokenKindToBinaryOpcode(int tokenKind)
    {
        BinaryOperatorKind opc = null;
        switch (tokenKind)
        {
            default:
                Util.shouldNotReachHere("Unknown binary operator token!");
            case STAR:          opc = BinaryOperatorKind.BO_Mul;break;
            case SLASH:         opc = BO_Div; break;
            case PERCENT:       opc = BinaryOperatorKind.BO_Rem; break;
            case PLUS:          opc = BinaryOperatorKind.BO_Add; break;
            case SUB:           opc = BinaryOperatorKind.BO_Sub;break;
            case LTLT:          opc = BinaryOperatorKind.BO_Shl; break;
            case GTGT:          opc = BinaryOperatorKind.BO_Shr; break;
            case LTEQ:          opc = BinaryOperatorKind.BO_LE;break;
            case LT:            opc = BinaryOperatorKind.BO_LT; break;
            case GTEQ:          opc = BinaryOperatorKind.BO_GE; break;
            case GT:            opc = BinaryOperatorKind.BO_GT; break;
            case BANGEQ:        opc = BinaryOperatorKind.BO_NE; break;
            case EQ:            opc = BinaryOperatorKind.BO_EQ; break;
            case AMP:           opc = BinaryOperatorKind.BO_And; break;
            case CARET:         opc = BinaryOperatorKind.BO_Xor; break;
            case BAR:           opc = BinaryOperatorKind.BO_Or; break;
            case AMPAMP:        opc = BinaryOperatorKind.BO_LAnd; break;
            case BARBAR:        opc = BinaryOperatorKind.BO_LOr;break;
            case EQEQ:          opc = BinaryOperatorKind.BO_Assign; break;
            case STAREQ:        opc = BinaryOperatorKind.BO_MulAssign;break;
            case SLASHEQ:       opc = BinaryOperatorKind.BO_DivAssign; break;
            case PERCENTEQ:     opc = BinaryOperatorKind.BO_RemAssign;break;
            case PLUSEQ:        opc = BinaryOperatorKind.BO_AddAssign; break;
            case SUBEQ:         opc = BinaryOperatorKind.BO_SubAssign;break;
            case LTLTEQ:        opc = BinaryOperatorKind.BO_ShlAssign;break;
            case GTGTEQ:        opc = BinaryOperatorKind.BO_ShrAssign; break;
            case AMPEQ:         opc = BinaryOperatorKind.BO_AndAssign; break;
            case CARETEQ:       opc = BinaryOperatorKind.BO_XorAssign;break;
            case BAREQ:         opc = BinaryOperatorKind.BO_OrAssign;break;
            case COMMA:         opc = BinaryOperatorKind.BO_Comma;break;
        }
        return opc;
    }

    public ActionResult<Expr> actOnCharacterConstant(Token tok)
    {
        assert tok.tag == CHARLITERAL :"Invalid character literal!";
        CharLiteral ch = (CharLiteral)tok;
        QualType ty = Type.IntTy;

        return new ActionResult<>(new CharacterLiteral(
                ch.getValue(),
                ty, EVK_RValue,
                ch.loc));
    }

	/**
     * Allocates a array of type char for holding the string literal.
     * @param str
     * @return
     */
    public ActionResult<Expr> actOnStringLiteral(Token.StringLiteral str)
    {
        assert !str.getValue().isEmpty():"string literal must have at least one char";

        String s = str.getValue();
        assert s!= null && s.length() > 0:"Must have at least one string!";
        QualType strTy = Type.CharTy;
        strTy = ArrayType.ConstantArrayType.getConstantType
                (strTy, new APInt(32, s.length() + 1));

        return new ActionResult<>(new StringLiteral(strTy, str.getValue(),
                EVK_RValue, str.loc));
    }

    private UnaryOperatorKind convertTokenKindToUnaryOperator(int kind)
    {
        switch (kind)
        {
            default:
                Util.shouldNotReachHere("Unknown unary operator!");
                return null;
            case PLUSPLUS:
                return UO_PreInc;
            case SUBSUB:
                return UO_PreDec;
            case AMP:
                return UO_AddrOf;
            case STAR:
                return UO_Deref;
            case PLUS:
                return UO_Plus;
            case SUB:
                return UO_Minus;
            case TILDE:
                return UO_Not;
            case BANG:
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
            int tokenKind,
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
        ActionResult<Expr> convRes = usualUnaryConversion(op);
        if (convRes.isInvalid())
            return new QualType();
        op = convRes.get();
        QualType opTy = op.getType();
        QualType result = new QualType();

        // Note that per both C89 and C99, indirection is always legal, even if OpTy
        // is an incomplete jlang.type or void.  It would be possible to warn about
        // dereferencing a void pointer, but it's completely well-defined, and such a
        // warning is unlikely to catch any mistakes.
        if (opTy.isPointerType())
            result = opTy.getPointerType().getPointeeType();
        else
        {}
        if (result.isNull())
        {
            parser.syntaxError(opLoc,
                    "indirection requires pointer operand (%s invalid)",
                    opTy.toString());
            return new QualType();
        }

        // Dereferences are usually l-values...
        vk.set(EVK_LValue);

        // ...except that certain expressions are never l-values in C.
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
        assert !resType.isNull():"no jlang.type for increment/decrement!";

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
            parser.syntaxError(opLoc,
                    "ISO C does not support '++'/'--' on complex integer jlang.type %s",
                    resType.toString());
        }
        else
        {
            parser.syntaxError(opLoc, "cannot select "
                    + (isIncre?"increment":"decrement")
                    + " value of jlang.type %s", resType.toString());
            return new QualType();
        }

        // At this point, we know we have a real, complex or pointer jlang.type.
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
     * Verify that E is a modifiable lvalue.
     * If not, emit an error and return true.  If so, return false.
     * @param e
     * @param oploc
     * @return
     */
    private boolean checkForModifiableLvalue(Expr e, SourceLocation oploc)
    {
        // C99 6.3.2.1: an lvalue that does not have array jlang.type,
        // does not have an incomplete jlang.type, does not have a const-qualified jlang.type,
        // and if it is a structure or union, does not have any member (including,
        // recursively, any member or element of all contained aggregates or unions)
        // with a const-qualified jlang.type.
        // TODO LLVM SemaExpr.cpp:6903
        return true;
        /*
        if (e.getValuekind() == EVK_RValue)
            return false;

        QualType resTy = e.getType();
        if (resTy.isConstantArrayType())
            return false;
        if (resTy.isIncompleteType())
            return false;
        if (resTy.isRecordType() || resTy.isUnionType())
        {

        }*/
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
        // LLVM SemaExpr.cpp:7409.
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
            if (castTy.getCanonicalTypeInternal().getUnQualifiedType().
                    equals(expr.getType().getUnQualifiedType().getCanonicalTypeInternal())
                    && (castTy.isStructureType() || castTy.isUnionType()))
            {
                // GCC struct/union extension: allow cast to self.
                parser.diag(range.getStart(), ext_typecheck_cast_nonscalar)
                        .addTaggedVal(castTy)
                        .addSourceRange(expr.getSourceRange());
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
                    if (fd.getDeclType().getCanonicalTypeInternal().getUnQualifiedType().
                            equals(expr.getType().getCanonicalTypeInternal().getUnQualifiedType()))
                    {
                        parser.diag(range.getStart(), ext_typecheck_cast_to_union)
                                .addSourceRange(expr.getSourceRange());
                        break;
                    }
                }
                if (i == e)
                {
                    parser.diag(range.getStart(), err_typecheck_cast_to_union_no_type)
                            .addTaggedVal(expr.getType())
                            .addSourceRange(expr.getSourceRange());
                    return true;
                }
                kind.set(CK_ToUnion);
            }
            else
            {
                // Reject any other conversions to non-scalar types.
                parser.diag(range.getStart(), err_typecheck_cond_expect_scalar)
                .addTaggedVal(castTy)
                .addSourceRange(expr.getSourceRange());
                return true;
            }
        }
        else if (!expr.getType().isScalarType())
        {
            parser.diag(expr.getLocStart(),
                    err_typecheck_expect_scalar_operand)
            .addTaggedVal(expr.getType())
            .addSourceRange(expr.getSourceRange());
            return true;
        }
        else if (!castTy.isArithmeticType())
        {
            QualType castExprType = expr.getType();
            if (castExprType.getIntegerType() == null
                    && castExprType.isArithmeticType())
            {
                parser.diag(expr.getLocStart(),
                        err_cast_pointer_from_non_pointer_int)
                        .addTaggedVal(castExprType)
                        .addSourceRange(expr.getSourceRange());
                return true;
            }
        }
        else if (!expr.getType().isArithmeticType())
        {
            if (!castTy.isIntegerType() && castTy.isArithmeticType())
            {
                parser.diag(expr.getLocStart(),
                        err_cast_pointer_to_non_pointer_int)
                        .addTaggedVal(castTy)
                        .addSourceRange(expr.getSourceRange());
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

        if (res.isInvalid()) return exprError();
        base = res.get();

        Expr lhsExpr = base;
        Expr rhsExpr = idx;

        // perform default conversion
        res = defaultFunctionArrayConversion(lhsExpr);
        if (res.isInvalid()) return exprError();
        lhsExpr = res.get();

        res = defaultFunctionArrayConversion(rhsExpr);
        if (res.isInvalid()) return exprError();
        rhsExpr = res.get();

        QualType lhsTy = lhsExpr.getType(), rhsTy = rhsExpr.getType();
        ExprValueKind vk = EVK_RValue;

        // C99 6.5.2.1p2: the expression e1[e2] is by definition precisely equivalent
        // to the expression *((e1)+(e2)). This means the array "Base" may actually be
        // in the subscript position. As a result, we need to derive the array base
        // and index from the expression types.
        Expr baseExpr, idxExpr;
        QualType resultTy;
        if (lhsTy.isPointerType())
        {
            baseExpr = lhsExpr;
            idxExpr = rhsExpr;
            resultTy = lhsTy.getPointerType().getPointeeType();
        }
        else if (rhsTy.isPointerType())
        {
            // handle the uncommon case of "123[Ptr]".
            baseExpr = rhsExpr;
            idxExpr = lhsExpr;
            resultTy = rhsTy.getPointerType().getPointeeType();
        }
        else if (lhsTy.isConstantArrayType())
        {
            // If we see an array that wasn't promoted by
            // DefaultFunctionArrayLvalueConversion, it must be an array that
            // wasn't promoted because of the C90 rule that doesn't
            // allow promoting non-lvalue arrays.  Warn, then
            // force the promotion here.
            parser.syntaxError(lhsExpr.getExprLocation(),
                    "ISO C90 does not allow subscripting non-lvalue array");
            lhsExpr = implicitCastExprToType(lhsExpr,
                    QualType.getArrayDecayedType(lhsTy),
                    EVK_RValue,
                    CK_ArrayToPointerDecay).get();
            lhsTy = lhsExpr.getType();
            baseExpr = lhsExpr;
            idxExpr = rhsExpr;

            resultTy = lhsTy.getPointerType().getPointeeType();
        }
        else if(rhsTy.isConstantArrayType())
        {
            // Same as previous, except for 123[f().a] case
            parser.syntaxError(rhsExpr.getExprLocation(),
                    "ISO C90 does not allow subscripting non-lvalue array");
            rhsExpr = implicitCastExprToType(rhsExpr,
                    QualType.getArrayDecayedType(rhsTy),
                    EVK_RValue,
                    CK_ArrayToPointerDecay).get();
            rhsTy = rhsExpr.getType();

            baseExpr = rhsExpr;
            idxExpr = lhsExpr;
            resultTy = rhsTy.getPointerType().getPointeeType();
        }
        else
        {
            parser.syntaxError(lParenLoc, "array subscript is not an integer");
            return exprError();
        }

        if (!idxExpr.getType().isIntegerType())
        {
            parser.syntaxError(lParenLoc, "array subscript is not an integer");
            return exprError();
        }
        int t = idxExpr.getType().getTypeKind();
        if (t == TypeClass.Char
                || t == TypeClass.UnsignedChar)
        {
            parser.syntaxWarning(lParenLoc, "array subscript is of jlang.type 'char'");
        }
        if (resultTy.isFunctionType())
        {
            parser.syntaxError(baseExpr.getExprLocation(),
                    "subscript of pointer to function jlang.type %s",
                    resultTy.toString());
            return exprError();
        }

        if (resultTy.isVoidType())
        {
            parser.syntaxError(lParenLoc,
                    "subscript of a pointer to void is a GNU extension");
            if (!resultTy.hasQualifiers()) vk = EVK_RValue;
        }

        assert vk == EVK_RValue || !resultTy.isCForbiddenLVaue();

        return new ActionResult<>(new ArraySubscriptExpr(lhsExpr, rhsExpr, resultTy, vk, rParenLoc));
    }

    private ActionResult<Expr> maybeConvertParenListExprToParenExpr(
            Expr e)
    {
        ParenListExpr ex = (ParenListExpr)e;
        if (ex == null)
            return new ActionResult<>(e);

        ActionResult<Expr> res = new ActionResult<>(ex.getExpr(0));
        for (int i = 0, size = ex.getNumExprs();!res.isInvalid()&&i < size; ++i)
            res = actOnBinOp(ex.getExprLoc(), Token.COMMA, res.get(), ex.getExpr(i));

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
        ActionResult<Expr> result = maybeConvertParenListExprToParenExpr(fn);
        if (result.isInvalid()) return exprError();
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

    /**
     * Build a call to a resolved expression, i.e. an expression not of
     * \p OverloadTy.  The expression should unary-convert to an expression of
     * function-pointer.
     *
     * @param fn
     * @param ndecl the declaration being called, if available
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
        FunctionDecl fnDecl = (FunctionDecl)ndecl;
        ActionResult<Expr> res = usualUnaryConversion(fn);
        if (res.isInvalid())
            return exprError();

        fn = res.get();

        CallExpr call = new CallExpr(fn, args, Type.BoolTy, EVK_RValue, rParenLoc);

        final FunctionType funcTy;
        if (fn.getType().isPointerType())
        {
            // C99 6.5.2.2p1 - "The expression that denotes the called function shall
            // have jlang.type pointer to function".
            PointerType pt = fn.getType().getPointerType();
            funcTy = pt.getPointeeType().getFunctionType();

            if (funcTy == null)
            {
                parser.syntaxError(lParenLoc,
                        "called object jlang.type %s is not a function or function pointer",
                        fn.getType().toString());
                return exprError();
            }
        }
        else
        {
            // Handle calls to expressions of unknown-any jlang.type.
            parser.syntaxError(lParenLoc,
                    "called object jlang.type %s is not a function or function pointer",
                    fn.getType().toString());
            return exprError();
        }

        // check for a valid return jlang.type.
        if (checkCallReturnType(funcTy.getReturnType(),
                fn.getExprLocation(), call, fnDecl))
            return exprError();

        call.setType(funcTy.getCallReturnType());
        call.setValueKind(EVK_RValue);

        if (funcTy instanceof FunctionProtoType)
        {
            FunctionProtoType proto = (FunctionProtoType)funcTy;
            if (convertArgumentsForCall(cal,, fn, fnDecl, proto, args, rParenLoc))
                return exprError();
        }
        else
        {
            if (fnDecl != null)
            {

                FunctionProtoType proto = null;
                if (!fnDecl.hasProtoType())
                    proto = fnDecl.getReturnType().getProtoType();

                // Promote the argument (C99 6.5.2.2p6).
                for (int i = 0, e = args.size(); i < e; i++)
                {
                    Expr arg = args.get(i);
                    if (proto != null && i < proto.getNumbArgs())
                    {

                    }
                    else
                    {
                        ActionResult<Expr> argE = defaultArgumentPromotion(arg);

                        if (argE.isInvalid())
                            return new ActionResult<>(true);

                        arg = argE.get();
                    }

                    if (requireCompleteType(arg.getExprLocation(),
                            arg.getType()))
                        return exprError();

                    call.setArgAt(i, arg);
                }
            }
        }

        // Do special checking on direct calls to function.
        if (fnDecl != null)
        {
            if (checkFunctionCall(fnDecl, call))
                return exprError();
        }

        return new ActionResult<>(call);
    }

    /**
     * This method called when jlang.parser encounter something like
     *   expression.identifier
     *   expression->identifier
     * @return
     */
    public ActionResult<Expr> actOnMemberAccessExpr(
            Scope s,
            Expr base,
            SourceLocation opLoc,
            int opKind,
            String name)
    {
        boolean isArrow = opKind == SUBGT;
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

        result = buildMemberReferenceExpr();
        return result;
    }

    private ActionResult<Expr> buildMemberReference(
            Expr base,
            QualType type,
            SourceLocation opLoc,
            boolean isArrow,
            LookupResult res)
    {
        // TODO: 2017/3/28
    }

    private ActionResult<Expr> lookupMemberExpr(
            LookupResult res,
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
        String memberName = res.getLookupName();
        SourceLocation memberLoc = res.getNameLoc();

        // For later jlang.type-checking purposes, turn arrow accesses into dot
        // accesses.
        if (isArrow)
        {
            if (baseType.isPointerType())
            {
                PointerType ptr = baseType.getPointerType();
                baseType = ptr.getPointeeType();
            }
            else
            {
                parser.syntaxError(memberLoc,
                        "member reference jlang.type %s is not a pointer",
                        baseType.toString());
                return exprError();
            }
        }

        // Handle field access to simple records.
        if (baseType.isRecordType())
        {
            RecordType rty = baseType.getRecordType();
            if (lookupMemberExprInRecord(res,
                    baseExpr.get().get().getExprLocation(),
                    rty, opLoc))
                return exprError();

            // Returning valid-but-null is how we indicate to the caller that
            // the lookup result was filled in.
            return new ActionResult<>(null);
        }

        parser.syntaxError(memberLoc,
                "member reference base jlang.type %s is not a structure or union",
                baseType.toString());
        return exprError();
    }

    /**
     * erforms lvalue-to-rvalue conversion on
     * the operand.  This is DefaultFunctionArrayLvalueConversion,
     * except that it assumes the operand isn't of function or array
     * jlang.type.
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
        // 'void' jlang.type are never l-values, but qualified void can be.
        if (t.isVoidType())
            return new ActionResult<>(e);

        // TODO checkForNullPointerDereference(e);

        // C99 6.3.2.1p2:
        //   If the lvalue has qualified jlang.type, the value has the unqualified
        //   version of the jlang.type of the lvalue; otherwise, the value has the
        //   jlang.type of the lvalue.
        if (t.hasQualifiers())
            t = t.clearQualified();

        return new ActionResult<>(
                new ImplicitCastExpr(t, EVK_RValue, e, CK_LValueToRValue, e.getExprLocation())
        );
    }

    private boolean lookupMemberExprInRecord(
            LookupResult res,
            SourceLocation loc,
            RecordType rty,
            SourceLocation opLoc)
    {
        RecordDecl recordDecl = rty.getDecl();
        if (requireCompleteType(opLoc, new QualType(rty)))
            return true;

        // TODO
    }


    public ActionResult<Expr> actOnPostfixUnaryOp(
            SourceLocation loc,
            int kind,
            Expr lhs)
    {
        UnaryOperatorKind opc;
        switch (kind)
        {
            default:
                Util.shouldNotReachHere("Unknown unary op!");
            case PLUSPLUS:
                opc = UO_PostInc;break;
            case SUBSUB:
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
                input = defaultFunctionArrayConversion(inputExpr);
                o1 = new OutParamWrapper<>(vk);
                resultTy = checkIndirectOperand(input.get(), o1, opLoc);
                vk = o1.get();
                break;

            case UO_Plus:
            case UO_Minus:
            {
                input = usualUnaryConversion(input.get());
                if (input.isInvalid())
                    return exprError();
                resultTy = input.get().getType();
                if (resultTy.isArithmeticType()) // C99 6.5.3.3p1
                    break;
                else
                {
                    parser.diag(opLoc, err_typecheck_unary_expr)
                    .addTaggedVal(resultTy).addSourceRange
                            (input.get().getSourceRange());
                    return exprError();
                }
            }
            case UO_Not:  // bitwise not.
            {
                input = usualUnaryConversion(input.get());
                if (input.isInvalid()) return exprError();

                resultTy = input.get().getType();
                if (resultTy.isIntegerType())
                    break;
                else
                {
                    parser.diag(opLoc, err_typecheck_unary_expr)
                            .addTaggedVal(resultTy).addSourceRange
                            (input.get().getSourceRange());
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
                    parser.diag(opLoc, err_typecheck_unary_expr)
                            .addTaggedVal(resultTy)
                            .addSourceRange(input.get().getSourceRange());
                    return exprError();
                }
                // LNot always has type int. C99 6.5.3.3p5.
                resultTy = Type.IntTy;
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
        parser.diag(res.getNameLoc(), diagnostic).addTaggedVal(name);
        return true;
    }

    private LangOption getLangOptions()
    {
        return parser.getPP().getLangOption();
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
            parser.diag(nameLoc, warn_use_out_of_scope_declaration)
                    .addTaggedVal(prev.getDeclName());
            parser.diag(prev.getLocation(), note_previous_declaration);
            return prev;
        }

        // Extension in C99.  Legal in C90, but warn about it.
        if (getLangOptions().c99)
            parser.diag(nameLoc, ext_implicit_function_decl)
                    .addTaggedVal(name);
        else
            parser.diag(nameLoc, warn_implicit_function_decl)
                    .addTaggedVal(name);
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
            String id,
            boolean hasTrailingLParen,
            boolean isAddressOfOperand)
    {
        assert !isAddressOfOperand && hasTrailingLParen:
                "cannot be direct & operand and have a trailing lparen";

        String name = id;
        SourceLocation nameLoc = loc;

        // Perform the required lookup.
        //LookupResult res = new LookupResult(this, name, nameLoc, LookupOrdinaryName);
        LookupResult res = lookupParsedName(s, name, LookupOrdinaryName, nameLoc);

        if (res.isAmbiguous())
        {
            parser.diag(nameLoc, err_ambiguous_reference).addTaggedVal(name);
            parser.diag(res.getFoundDecl().getLocation(), note_ambiguous_candidate)
                    .addTaggedVal(res.getFoundDecl().getDeclName());
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
            parser.diag(l.loc, err_undeclared_label_use).addTaggedVal(l.getName());

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
            parser.diag(loc, err_unexpected_typedef_ident)
                    .addTaggedVal(decl.getDeclName());
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
        if (requireCompleteType(vd.getLocation(), QualType.getBaseElementType(ty)))
        {
            vd.setInvalidDecl(true);
            return;
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
            parser.syntaxError(decl.getLocation(),
                    "illegal initializer (only variables can be initialized)");
            decl.setInvalidDecl(true);
            return;
        }

        VarDecl vd = (VarDecl) decl;

        // A definition must end up with a complete jlang.type, which means it must be
        // complete with the restriction that an array jlang.type might be completed by the
        // initializer; note that later code assumes this restriction.
        QualType baseDeclType = vd.getDeclType();
        ArrayType array = baseDeclType.getAsInompleteArrayType();

        if (array != null)
        {
            baseDeclType = array.getElemType();
        }
        if (requireCompleteType(vd.getLocation(), baseDeclType))
        {
            decl.setInvalidDecl(true);
            return;
        }
        // TODO Check redefinition.

        // Get the decls jlang.type and save a reference for later, since
        // CheckInitializerTypes may change it.
        QualType declTy = vd.getDeclType(), savedTy = declTy;
        if (vd.isLocalVarDecl())
        {
            if (vd.hasExternalStorage())
            {
                // C99 6.7.8p5
                parser.syntaxError(vd.getLocation(),
                        "'extern' variable cannot have an initializer");
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
                    !QualType.getBaseElementType(vd.getDeclType()).isConstQualifed())
            {
                parser.syntaxWarning(vd.getLocation(), "'extern' variable has an initializer");
            }
            if (!vd.isInvalidDecl())
            {
                // TODO initialization sequence 2016.10.23

                // C99 6.7.8p4. All file scoped initializers need to be constant.
                checkForConstantInitializer(init, declTy);
            }
        }

        // If the jlang.type changed, it means we had an incomplete jlang.type that was
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
        e.setType(Type.VoidTy);
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
        parser.syntaxError(init.getExprLocation(),
                "initializer element is not a compile-time constant");
        return true;
    }

	/**
     * This method is called *for error recovery purposes only*
     * to determine if the specified name is a valid tag name ("struct foo").  If
     * so, this returns the TST for the tag corresponding to it (TST_enum,
     * TST_union, TST_struct, TST_class).  This is used to diagnose cases in C
     * where the user forgot to specify the tag.
     * @param identifierInfo
     * @param scope
     * @return
     */
    public TST isTagName(String identifierInfo, Scope scope)
    {
        NamedDecl ndecl = lookupName(scope, identifierInfo,
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
}
