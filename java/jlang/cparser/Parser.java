package jlang.cparser;

import tools.Util;
import jlang.ast.Tree;
import jlang.ast.Tree.Expr;
import jlang.ast.Tree.Stmt;
import jlang.clex.CommentHandler.DefaultCommentHandler;
import jlang.clex.*;
import jlang.cparser.DeclSpec.DeclaratorChunk;
import jlang.cparser.DeclSpec.FieldDeclarator;
import jlang.cparser.DeclSpec.ParamInfo;
import jlang.cparser.DeclSpec.ParsedSpecifiers;
import jlang.cparser.Declarator.TheContext;
import jlang.diag.*;
import jlang.sema.Decl;
import jlang.sema.Decl.LabelDecl;
import jlang.sema.PrecedenceLevel;
import jlang.sema.Scope;
import jlang.sema.Scope.ScopeFlags;
import jlang.sema.Sema;
import jlang.support.LangOptions;
import jlang.support.SourceLocation;
import jlang.support.SourceRange;
import jlang.type.QualType;
import tools.OutRef;
import tools.Pair;

import java.util.ArrayList;
import java.util.HashSet;

import static java.util.Collections.emptyList;
import static jlang.clex.TokenKind.*;
import static jlang.clex.TokenKind.Enum;
import static jlang.cparser.DeclSpec.SCS.*;
import static jlang.cparser.DeclSpec.TQ.*;
import static jlang.cparser.DeclSpec.TSC.TSC_complex;
import static jlang.cparser.DeclSpec.TSS.TSS_signed;
import static jlang.cparser.DeclSpec.TSS.TSS_unsigned;
import static jlang.cparser.DeclSpec.TST.*;
import static jlang.cparser.DeclSpec.TSW.*;
import static jlang.cparser.Declarator.TheContext.FileContext;
import static jlang.cparser.Parser.ParenParseOption.*;
import static jlang.sema.Scope.ScopeFlags.DeclScope;
import static jlang.sema.Scope.ScopeFlags.FunctionProtoTypeScope;
import static jlang.sema.Sema.TagUseKind.*;

/**
 * This is a jlang.parser for C language.
 * <p>
 * The jlang.parser map a token sequence into an abstract syntax tree. It operates by
 * recursive descent, with code derived systematically from an EBNF grammar.
 * For efficiency reasons, an operator predecessor scheme is used for parsing
 * binary operation expression, also, the special three-operation(?:) is handled
 * as binary operation for specially.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class Parser implements Tag,
        DiagnosticParseTag,
        DiagnosticSemaTag,
        DiagnosticCommonKindsTag
{
    private static class FieldCallBack
    {
        Parser parser;
        Decl tagDecl;
        ArrayList<Decl> fieldDecls;

        FieldCallBack(Parser parser, Decl tagDecl, ArrayList<Decl> fieldDecls)
        {
            this.parser = parser;
            this.tagDecl = tagDecl;
            this.fieldDecls = fieldDecls;
        }

        Decl invoke(FieldDeclarator fd)
        {
            // install the declarator into the current tagDecl.
            Decl field = parser.action.actOnField(
                    parser.getCurScope(), tagDecl,
                    fd.declarator.getDeclSpec().getSourceRange().getBegin(),
                    fd.declarator, fd.bitFieldSize);
            fieldDecls.add(field);

            return field;
        }
    }

    static class ParseScope
    {
        private Parser self;

        /**
         * Construct a new ParseScope object to manage a scopein the
         * jlang.parser self where the new scope is createed with the flags.
         * @param self
         * @param scopeFlags
         */
        ParseScope(Parser self, int scopeFlags)
        {
            this(self, scopeFlags, true);
        }

        /**
         * Construct a new ParseScope object to manage a scopein the
         * jlang.parser self where the new scope is createed with the flags.
         * @param self  The instance of {@linkplain Parser} enclosing this scope.
         * @param scopeFlags Which indicates the kind of this scope.
         * @param manageScope Indicates if this scope do something, when it is
         *                    false, this scope do nothing.
         */
        ParseScope(Parser self, int scopeFlags, boolean manageScope)
        {
            this.self = self;
            if (manageScope)
                self.enterScope(scopeFlags);
            else
                this.self = null;
        }

        void exit()
        {
            if (self != null)
            {
                self.exitScope();
                self = null;
            }
        }
    }

    /**
     * Start a new scope.
     * @param scopeFlags
     */
    private void enterScope(int scopeFlags)
    {
        action.setCurScope(new Scope(getCurScope(), scopeFlags));
    }

    /**
     * Pop a scope off the scope stack.
     */
    private void exitScope()
    {
        Util.assertion(getCurScope() != null,  "Scope imbalance.");

        // inform the action module that this scope is cleared if there are any
        // decls in it.
        if (!getCurScope().declEmpty())
        {
            action.actOnPopScope(getCurScope());
        }
        Scope oldScope = getCurScope();
        action.setCurScope(oldScope.getParent());
    }

    /**
     * Semantic action.
     */
    private Sema action;
    /**
     * The scanner used for lexical analysis
     */
    private Preprocessor pp;
    /**
     * A diagnose for reporting error and warning information in appropriate style.
     */
    private Diagnostic diags;

    private int braceCount;

    private SourceLocation prevTokLocation;

    Token tok;

    DelimiterTracker quantityTracker;

    public Diagnostic getDiags()
    {
        return diags;
    }

    public Preprocessor getPP()
    {
        return pp;
    }

    /**
     * Initialize the parser.
     */
    public void initialize()
    {
        // Prime the lexer look-ahead.
        consumeToken();

        Util.assertion(getCurScope() == null, "A scope is already active?");
        enterScope(DeclScope.value);
        action.actOnTranslationUnitScope(getCurScope());

        if (tok.is(eof) && !getLangOption().gnuMode)
            diag(tok, ext_empty_source_file);       // Empty source file is gnu extension
    }

    /**
     * Constructs a jlang.parser from a given scanner.
     */
    public Parser(Preprocessor pp, Sema action)
    {
        this.pp = pp;
        this.action = action;
        diags = pp.getDiagnostics();
        tok = new Token();
        tok.setKind(eof);
        pp.addCommentHandler(new DefaultCommentHandler(action));
        quantityTracker = new DelimiterTracker();

        // Add #pragma handlers.
        // todo add PragmaHandler. 2017.8.14
    }

    LangOptions getLangOption()
    {
        return pp.getLangOptions();
    }

    Preprocessor getPreprocessor()
    {
        return pp;
    }

    public Sema getAction()
    {
        return action;
    }

    public Diagnostic.DiagnosticBuilder diag(SourceLocation loc, int diagID)
    {
        return diags.report(new FullSourceLoc(loc, pp.getSourceManager()), diagID);
    }

    private Diagnostic.DiagnosticBuilder diag(Token tok, int diagID)
    {
        return diag(tok.getLocation(), diagID);
    }

    /**
     * Parse a translation unit (C90 6.7, C99 6.9).
     * <pre>
     * translation-unit:
     *   external-declarations
     * external-declarations:
     *   external-declaration
     *   external-declarations external-declaration
     * </pre>
     */
    public void compilationUnit()
    {
        Util.assertion( getCurScope() == null);
        enterScope(DeclScope.value);
        action.actOnTranslationUnitScope(getCurScope());

        ArrayList<Decl> result = new ArrayList<>();
        // Parse them.
        while (!parseTopLevel(result));

        exitScope();
        Util.assertion(getCurScope() == null,  "Scope imbalance!");
    }

    /**
     * Parse one top-level declaration, return whatever the
     * action tells us to.  This returns true if the EOF was encountered.
     * @param result
     * @return
     */
    public boolean parseTopLevel(ArrayList<Decl> result)
    {
        Util.assertion( result != null);
        result.clear();
        if (nextTokenIs(eof))
        {
            action.actOnEndOfTranslationUnit();
            return true;
        }

        // parse all external declaration.
        result.addAll(parseExternalDeclaration());
        return false;
    }

    /**
     * Parse an external declaration (C90 6.7, C99 6.9).
     * <p>
     * external-declaration:
     * function-definition
     * declaration
     */
    private ArrayList<Decl> parseExternalDeclaration()
    {
        SourceLocation pos = tok.getLocation();
        switch (tok.getKind())
        {
            case semi:
            {
                diag(pos, ext_top_level_semi).emit();
                consumeToken();
                return declGroups();
            }
            case r_brace:
                diag(pos, err_expected_external_declaration).emit();
                consumeToken();
                return declGroups();
            case eof:
                diag(pos, err_expected_external_declaration).emit();
                return declGroups();

            case __Extension__:
                // __extension__ silences extension warnings in the subexpression.
                consumeToken();
                return parseExternalDeclaration();
            case Asm:
                //Util.assertion(false, "Current inline assembly.");
                OutRef<SourceLocation> endLoc = new OutRef<>();
                ActionResult<Expr> result = parseSimpleAsm(endLoc);
                expectAndConsume(semi, err_expected_semi_after, "top-level asm block", semi);
                if (result.isInvalid())
                    return new ArrayList<>();

                return action.convertDeclToDeclGroup(
                        action.actOnFileScopeAsmDecl(tok.getLocation(), result));
            case sub:
            case plus:
                diag(tok, err_expected_external_declaration).emit();
                consumeToken();
                return new ArrayList<>();
            case Typedef:
            {
                // A function definition can not start with those keyword.
                OutRef<SourceLocation> declEnd = new OutRef<>();
                return parseDeclaration(FileContext, declEnd);
            }
            /**
             * Else fall through, and yield a syntax error trying to parse
             * as a declaration or function definition.
             */
            default:
            {
                // We can't tell whether this is a function-definition or declaration yet.
                /* A declaration or a function definition.  We can only tell
                 * which after parsing the declaration specifiers, if any, and
                 * the first declarator.
                 */
                return parseDeclarationOrFunctionDefinition();
            }
        }
    }

    /**
     * Parses the inline assembly code declared in File context.
     * [GNU]    simple-asm-expr:
     *              'asm' '(' asm-string-literal ')'
     * @return
     */
    private ActionResult<Expr> parseSimpleAsm(OutRef<SourceLocation> endLoc)
    {
        Util.assertion(tok.is(Asm), "Not an inline assembly code");
        SourceLocation asmLoc = consumeToken();

        if (tok.isNot(l_paren))
        {
            diag(tok, err_expected_lparen_after).addTaggedVal("asm").emit();
            return exprError();
        }

        asmLoc = consumeParen();

        ActionResult<Expr> res = parseAsmStringLiteral();
        if (res.isInvalid())
        {
            skipUntil(r_paren, true, true);
            if (endLoc != null)
                endLoc.set(tok.getLocation());
            consumeAnyToken();
        }
        else
        {
            asmLoc = matchRHSPunctuation(r_paren, asmLoc);
            if (endLoc != null)
                endLoc.set(asmLoc);
        }

        return res;
    }

    /**
     * Parse the inline assembly literal.
     * [GNU]    asm-string-literal
     *              string-literal
     * @return
     */
    private ActionResult<Expr> parseAsmStringLiteral()
    {
        if (!isTokenStringLiteral())
        {
            diag(tok, err_expected_string_literal).emit();
            return exprError();
        }

        ActionResult<Expr> res = parseStringLiteralExpression();
        if (res.isInvalid())
            return exprError();

        return res;
    }

    /**
     * Parse a declaration or function definition (C90 6.5, 6.7.1, C99
     * 6.7, 6.9.1).
     * <pre>
     * declaration:
     *      declaration-specifiers init-declarator-list[opt] ';'
     *      init-declarator-list ';'  [warn in C99 mode]
     *
     * function-definition: [C99 6.9.1]
     *      declaration-specifiers declarator declaration-list
     *      compound-statement
     *
     * function-definition: [C90] - implicit int result
     *      declaration-specifiers[opt] declarator declaration-list[opt]
     *      compound-statement
     *
     * declaration-list:
     *      declaration
     *      declaration-list declaration
     *
     * init-declarator-list:
     *      init-declarator
     *      init-declarator-list , init-declarator
     *
     * init-declarator:
     *      declarator simple-asm-subExpr[opt] attributes[opt]
     *      declarator simple-asm-subExpr[opt] attributes[opt] = initializer
     * <pre>
     * C99 requires declaration specifiers in a function definition; the
     * absence is diagnosed through the diagnosis of implicit int.  In GNU
     * C we also allow but diagnose declarations without declaration
     * specifiers, but only at top level (elsewhere they conflict with
     * other syntax).
     */
    private ArrayList<Decl> parseDeclarationOrFunctionDefinition()
    {
        DeclSpec declSpecs = new DeclSpec();

        // Parse the common declaration-specifiers piece code.
        parseDeclarationSpecifiers(declSpecs);

        // C99 6.7.2.3p6: Handle "struct-or-union identifier;", "enum { X };"
        // declaration-specifiers init-declarator-list[opt] ';'
        if (nextTokenIs(semi))
        {
            consumeToken();
            Decl theDecl = action.parsedFreeStandingDeclSpec(getCurScope(), declSpecs);
            // This stmt in Clang just is used to inform people that a declaration
            // was parsed complete.
            // declSpecs.complete(theDecl);
            return action.convertDeclToDeclGroup(theDecl);
        }
        return parseDeclGroup(declSpecs, FileContext, true);
    }


    private ArrayList<Decl> declGroups()
    {
        return new ArrayList<>();
    }

    /**
     * We parsed and verified that the specified
     * Declarator is well formed.  If this is a K&R-style function, read the
     * parameters declaration-list, then start the compound-statement.
     * <pre>
     *       function-definition: [C99 6.9.1]
     *         decl-specs      declarator declaration-list[opt] compound-statement
     * [C90] function-definition: [C99 6.7.1] - implicit int result
     * [C90]   decl-specs[opt] declarator declaration-list[opt] compound-statement
     * </pre>
     * @param declarator
     * @return
     */
    private Decl parseFunctionDefinition(Declarator declarator)
    {
        DeclaratorChunk.FunctionTypeInfo fti = declarator.getFunctionTypeInfo();

        // If this is C90 and the declspecs were completely missing, fudge in an
        // implicit int.  We do this here because this is the only place where
        // declaration-specifiers are completely optional in the grammar.
        if (getLangOption().implicitInt && declarator.getDeclSpec().isEmpty())
        {
            OutRef<String> x = new OutRef<>("");
            OutRef<Integer> y = new OutRef<>(0);
            declarator.getDeclSpec().setTypeSpecType(TST_int,
                    declarator.getIdentifierLoc(),
                    x, y);
            String prevSpec = x.get();
            int diagID = y.get();
            declarator.setRangeStart(declarator.getDeclSpec().getSourceRange().getBegin());
        }

        // If this declaration was formed with a K&R-style identifier list for the
        // arguments, parse declarations for all of the args next.
        // int foo(a,b) int a; float b; {}
        if (fti.isKNRPrototype())
        {
            parseKNRParamDeclarations(declarator);
        }

        // We should have an opening brace.
        if (nextTokenIsNot(l_brace))
        {
            diag(tok, err_expected_fn_body).emit();
            skipUntil(l_brace, true);

            // if we didn't find a '{', bail out.
            if (nextTokenIsNot(l_brace))
                return null;
        }

        // Enter a scope for the function body.
        ParseScope bodyScope = new ParseScope(this, ScopeFlags.FnScope.value
                | DeclScope.value);

        Decl res = action.actOnStartOfFunctionDef(getCurScope(), declarator);
        Decl decl = parseFunctionStatementBody(res);
        bodyScope.exit();
        return decl;
    }

    /**
     * Parse 'declaration-list[opt]' which provides types for a function with
     * a K&R-style identifier list for arguments
     * @param d
     */
    private void parseKNRParamDeclarations(Declarator d)
    {
        // We know that the top level of this declarator is a function.
        DeclaratorChunk.FunctionTypeInfo fti = d.getFunctionTypeInfo();

        // Enter function-declaration scope, limiting any decalarators to
        // the function prototype scope, including parameter declarators.
        ParseScope prototypeScope = new ParseScope(this,
                FunctionProtoTypeScope.value | DeclScope.value);

        // Read all the argument declarations.
        while (isDeclarationSpecifier())
        {
            SourceLocation dsStart = tok.getLocation();

            // Parse the common declaration-specifiers piece.
            DeclSpec ds = new DeclSpec();
            parseDeclarationSpecifiers(ds);

            // C99 6.9.1p6: 'each declaration in the declaration list shall have at
            // least one declarator'.
            // NOTE: GCC just makes this an ext-warn.  It's not clear what it does with
            // the declarations though.  It's trivial to ignore them, really hard to do
            // anything else with them.
            if (tok.is(semi))
            {
                diag(dsStart, err_declaration_does_not_declare_param).emit();
                consumeToken();
                continue;
            }

            // C99 6.9.1p6: Declarations shall contain no storage-class specifiers other
            // than register.
            if (ds.getStorageClassSpec() != SCS_unspecified
                    && ds.getStorageClassSpec() != SCS_register)
            {
                diag(ds.getStorageClassSpecLoc(),
                        err_invalid_storage_class_in_func_decl).emit();
                ds.clearStorageClassSpec();
            }

            Declarator paramDeclarator = new Declarator(ds,
                    TheContext.KNRTypeListContext);
            parseDeclarator(paramDeclarator);

            while (true)
            {
                AttributeList attrList = null;
                // if attribute are present, parse them.
                if (tok.is(__Attribute))
                {
                    attrList = parseAttributes().first;
                    // FIXME: 17-10-28 use the attribute list.
                }

                // Ask the actions module to compute the type for this declarator.
                Decl param = action.actOnParamDeclarator(getCurScope(), paramDeclarator);

                if (param != null && paramDeclarator.getIdentifier() != null)
                {
                    // A missing identifier has already been diagnosed.
                    // So the declarator ident must be existed on reaching here.

                    // Scan the argument list looking for the correct param to apply this
                    // type.
                    int i = 0;
                    for (; i != fti.numArgs ; i++)
                    {
                        if (fti.argInfo.get(i).ident.equals(paramDeclarator.getIdentifier()))
                        {
                            // Reject redefinitions of parameters.
                            if (fti.argInfo.get(i).param != null)
                            {
                                diag(paramDeclarator.getIdentifierLoc(), err_param_redefinition)
                                        .addTaggedVal(paramDeclarator.getIdentifier())
                                        .emit();
                            }
                            else
                            {
                                fti.argInfo.get(i).param = param;
                                break;
                            }
                        }
                    }

                    // C99 6.9.1p6: those declarators shall declare only identifiers from
                    // the identifier list.
                    if (i == fti.numArgs)
                    {
                        diag(paramDeclarator.getIdentifierLoc(), err_no_matching_param)
                                .addTaggedVal(paramDeclarator.getIdentifier())
                                .emit();
                    }
                }

                // If we don't have a comma, it is either the end of the list
                // "int a, int b;" or an error, bail out.
                if (tok.isNot(comma))
                    break;

                // Consume the comma.
                consumeToken();

                // Parse the next declarator.
                paramDeclarator.clear();
                parseDeclarator(paramDeclarator);
            }

            if (tok.is(semi))
                consumeToken();
            else
            {
                diag(tok, err_parse_error);
                // Skip to end of block or statement.
                skipUntil(semi, true);
                if (tok.is(semi))
                    consumeToken();
            }
        }
        // The actions module must verify that all arguments were declared.
        action.actOnFinishKNRParamDeclarations(getCurScope(), d, tok.getLocation());
        prototypeScope.exit();
    }

    private Decl parseFunctionStatementBody(Decl funcDecl)
    {
        Util.assertion( nextTokenIs(l_brace));

        SourceLocation lBraceLoc = tok.getLocation();
        ActionResult<Stmt> body = parseCompoundStatementBody(false);

        if (body.isInvalid())
            body = action.actOnCompoundStmtBody(lBraceLoc, lBraceLoc, emptyList(), false);

        return action.actOnFinishFunctionBody(funcDecl, body.get());
    }

    /**
     * ===----------------------------------------------------------------------===<br>
     * C99 6.8: Statements and Blocks.
     * ===----------------------------------------------------------------------===<br>
     *
     * ParseStatementOrDeclaration - Read 'statement' or 'declaration'.
     * <pre>
     *       StatementOrDeclaration:
     *         statement
     *         declaration
     *
     *       statement:
     *         labeled-statement
     *         compound-statement
     *         expression-statement
     *         selection-statement
     *         iteration-statement
     *         jump-statement
     *
     *       labeled-statement:
     *         identifier ':' statement
     *         'case' constant-expression ':' statement
     *         'default' ':' statement
     *
     *       selection-statement:
     *         if-statement
     *         switch-statement
     *
     *       iteration-statement:
     *         while-statement
     *         do-statement
     *         for-statement
     *
     *       expression-statement:
     *         expression[opt] ';'
     *
     *       jump-statement:
     *         'goto' identifier ';'
     *         'continue' ';'
     *         'break' ';'
     *         'return' expression[opt] ';'
     * </pre>
     * @return  The declarations group.
     */
    private ActionResult<Stmt> parseStatementOrDeclaration(
            boolean onlyStatements)
    {
        String semiError = null;
        ActionResult<Stmt> res;

        switch (tok.getKind())
        {
            case identifier:
            {
                // C99 6.8.1 labeled-statement
                if (nextToken().is(colon))
                {
                    // identifier ':' statement
                    return parseLabeledStatement();
                }
            }
            // fall through
            default:
            {
                if (!onlyStatements && isDeclarationSpecifier())
                {
                    SourceLocation declStart = tok.getLocation();
                    OutRef<SourceLocation> end = new OutRef<>();
                    ArrayList<Decl> decls = parseDeclaration(TheContext.BlockContext, end);
                    SourceLocation declEnd = end.get();

                    return action.actOnDeclStmt(decls, declStart, declEnd);
                }
                if (nextTokenIs(r_brace))
                {
                    diag(tok.getLocation(), err_expected_statement).emit();
                    return stmtError();
                }
                // expression[opt] ';'
                return parseExprStatement();
            }
            case Case:
            {
                // C99 6.8.1: labeled-statement
                return parseCaseStatement(false, null);
            }
            case Default:
            {
                // C99 6.8.1: labeled-statement
                return parseDefaultStatement();
            }
            case l_brace:
            {
                // C99 6.8.2: compound-statement
                return parseCompoundStatement();
            }
            case semi:
            {
                // null statement
                SourceLocation loc = consumeToken();
                return action.actOnNullStmt(loc);
            }
            case If:
            {
                // C99 6.8.4.1: if-statement
                return parseIfStatement();
            }
            case Switch:
            {
                return parseSwitchStatement();
            }
            case While:
                return parseWhileStatement();
            case Do:
                res = parseDoStatement();
                semiError = "do/while";
                break;
            case For:
                return parseForStatement();
            case Goto:
                res = parseGotoStatement();
                semiError = "goot";
                break;
            case Continue:
                res = parseContinueStatement();
                semiError = "continue";
                break;
            case Break:
                res = parseBreakStatement();
                semiError = "break";
                break;
            case Return:
                res = parseReturnStatement();
                semiError = "return";
                break;
        }
        // If we reached this code, the statement must end in a ';'.
        if (tok.is(semi))
            consumeToken();
        else if (!res.isInvalid())
        {
            // If the result was valid, then we do want to diagnose this.  Use
            // ExpectAndConsume to emit the diagnostic, even though we know it won't
            // succeed.
            expectAndConsume(semi, err_expected_semi_after_stmt, semiError, Unknown);
            // Skip until we see a }' or ';', but don't eat it.
            skipUntil(r_brace, true, true);
        }
        return res;
    }

    /**
     * Parse labelled statement.
     * <pre>
     *     labelled-statement:
     *       identifier ':' statement
     * </pre>
     * @return
     */
    private ActionResult<Stmt> parseLabeledStatement()
    {
        Util.assertion(nextTokenIs(identifier) && tok.getIdentifierInfo() != null,  "Not a valid identifier");

        Token identTok = tok;  // Save the identifier token.
        consumeToken(); // Eat the identifier.

        Util.assertion(nextTokenIs(colon),  "Not a label");
        // identifier ':' statement
        SourceLocation colonLoc = consumeToken();

        // read label attributes, if present.
        AttributeList attr = null;
        if (tok.is(__Attribute))
            attr = parseAttributes().first;

        ActionResult<Stmt> res = parseStatement();
        if (res.isInvalid())
            return new ActionResult<Stmt>(new Tree.NullStmt(colonLoc));

        LabelDecl ld = action.lookupOrCreateLabel(tok.getIdentifierInfo(),
                tok.getLocation());

        return action.actOnLabelStmt(tok.getLocation(), ld, colonLoc, res);
    }

    /**
     * <pre>
     * labeled-statement:
     *   'case' constant-expression ':' statement
     * </pre>
     * @return
     */
    private ActionResult<Stmt> parseCaseStatement(
            boolean missingCase,
            ActionResult<Expr> expr)
    {
        Util.assertion(nextTokenIs(Case) || missingCase,  "Not a case stmt!");

        // It is very very common for code to contain many case statements recursively
        // nested, as in (but usually without indentation):
        //  case 1:
        //    case 2:
        //      case 3:
        //         case 4:
        //           case 5: etc.
        //
        // Parsing this naively works, but is both inefficient and can cause us to run
        // out of stack space in our recursive descent jlang.parser.  As a special case,
        // flatten this recursion into an iterative loop.  This is complex and gross,
        // but all the grossness is constrained to ParseCaseStatement (and some
        // wierdness in the actions), so this is just local grossness :).

        // TopLevelCase - This is the highest level we have parsed.  'case 1' in the
        // example above.
        ActionResult<Stmt> topLevelCase = new ActionResult<>(true);

        // This is the deepest statement we have parsed, which
        // gets updated each time a new case is parsed, and whose body is unset so
        // far.  When parsing 'case 4', this is the 'case 3' node.
        Stmt deepestParsedCaseStmt = null;

        SourceLocation colonLoc = SourceLocation.NOPOS;
        do
        {
            // consume 'case'
            SourceLocation caseLoc = missingCase ? expr.get().getExprLocation()
                    : consumeToken();  // Eat the 'case'.

            // parse the constant subExpr after'case'
            ActionResult<Expr> lhs = missingCase ? expr : parseConstantExpression();
            missingCase = false;

            if (lhs.isInvalid())
            {
                skipUntil(colon, true);
                return stmtError();
            }
            // GNU case range extension.
            SourceLocation dotDotDotLoc;
            ActionResult<Expr> rhs;
            if (nextTokenIs(ellipsis))
            {
                diag(tok, ext_gnu_case_range).emit();
                dotDotDotLoc = consumeToken();

                rhs = parseConstantExpression();
                if (rhs.isInvalid())
                {
                    skipUntil(colon, true);
                    return stmtError();
                }
            }

            if (nextTokenIs(colon))
            {
                colonLoc = consumeToken();
            }
            else if (nextTokenIs(semi))
            {
                colonLoc = consumeToken();
                diag(colonLoc, err_expected_colon_after).addTaggedVal("'case'")
                .addFixItHint(FixItHint.createReplacement(colonLoc, ":"))
                .emit();
            }
            else
            {
                diag(prevTokLocation, err_expected_colon_after).addTaggedVal("'case'")
                .addFixItHint(FixItHint.createInsertion(prevTokLocation, ":"))
                .emit();
                colonLoc = prevTokLocation;
            }
            // Make semantic checking on case constant expression.
            ActionResult<Stmt> Case = action.actOnCaseStmt(caseLoc, lhs.get(), colonLoc);
            if (Case.isInvalid())
            {
                if (topLevelCase.isInvalid())
                    return parseStatement();
            }
            else
            {
                // If this is the first case statement we parsed, it becomes TopLevelCase.
                // Otherwise we link it into the current chain.
                Stmt nextDeepest = Case.get();
                if (topLevelCase.isInvalid())
                    topLevelCase = Case;
                else
                {
                    action.actOnCaseStmtBody(deepestParsedCaseStmt, Case.get());
                }
                deepestParsedCaseStmt = nextDeepest;
            }
            // handle all case statements
        } while(nextTokenIs(Case));

        Util.assertion(!topLevelCase.isInvalid(), "Should have parsed at least one case statement");

        ActionResult<Stmt> subStmt;
        if (nextTokenIsNot(r_brace))
        {
            subStmt = parseStatement();
        }
        else
        {
            diag(colonLoc, err_label_end_of_compound_statement).emit();
            subStmt = new ActionResult<>(true);
        }

        if (subStmt.isInvalid())
            subStmt = action.actOnNullStmt(new SourceLocation());

        // install the case body into the most deeply-nested case statement
        action.actOnCaseStmtBody(deepestParsedCaseStmt, subStmt.get());

        // return the top level parsed statement
        return  topLevelCase;
    }

    /**
     * <pre>
     *   labeled-statement:
     *     'default' ':' statement
     * </pre>
     * Not that this does not parse the 'statement' at the end.
     * @return
     */
    private ActionResult<Stmt> parseDefaultStatement()
    {
        Util.assertion((nextTokenIs(Default)), "Not a default statement!");

        // eat the 'default' keyword
        SourceLocation defaultLoc = consumeToken();

        SourceLocation colonLoc = SourceLocation.NOPOS;
        if (nextTokenIs(colon))
        {
            colonLoc = consumeToken();
        }
        else if (nextTokenIs(semi))
        {
            colonLoc = consumeToken();
            diag(colonLoc, err_expected_colon_after).addTaggedVal("'default'")
                    .addFixItHint(FixItHint.createReplacement(colonLoc, ":"))
                    .emit();
        }
        else
        {
            diag(prevTokLocation, err_expected_colon_after).addTaggedVal("'default'")
                    .addFixItHint(FixItHint.createInsertion(prevTokLocation, ":"))
                    .emit();
            colonLoc = prevTokLocation;
        }

        // diagnose the common error "switch (X) { default:}", which is not valid.
        if (nextTokenIs(r_brace))
        {
            diag(tok, err_label_end_of_compound_statement).emit();
            return stmtError();
        }

        ActionResult<Stmt> subStmt = parseStatement();
        if (subStmt.isInvalid())
            return stmtError();

        return  action.actOnDefaultStmt(defaultLoc, colonLoc, subStmt.get());
    }

    private ActionResult<Stmt> parseCompoundStatement()
    {
        return parseCompoundStatement(false, DeclScope.value);
    }

    /**
     * Parse a "{}" block.
     * <pre>
     *   compound-statement: [C99 6.8.2]
     *     { block-item-list[opt] }
     *
     *    block-item-list:
     *      block-item
     *      block-item-list block-item
     *
     *    block-item:
     *      declaration
     *      statement
     * </pre>
     * @return
     */
    private ActionResult<Stmt> parseCompoundStatement(boolean isStmtExpr, int scopeFlags)
    {
        Util.assertion(tok.is(l_brace),  "Not a compound statement!");

        // Enter a scope to hold everything within the compound stmt.
        // Compound statements can always hold declarations.
        ParseScope compoundScope = new ParseScope(this, scopeFlags);

        // parse the statements in the body.
        ActionResult<Stmt> res = parseCompoundStatementBody(isStmtExpr);
        compoundScope.exit();
        return res;
    }

    private ActionResult<Stmt> parseCompoundStatementBody(boolean isStmtExpr)
    {
        Util.assertion( tok.is(l_brace));
        BalancedDelimiterTracker tracker = new BalancedDelimiterTracker(this, l_brace);
        if (tracker.consumeOpen())
            return stmtError();     // Eat the '{'.

        ArrayList<Stmt> stmts = new ArrayList<>();

        // "__label__ X, Y, Z;" is the GNU "Local Label" extension.  These are
        // only allowed at the start of a compound stmt regardless of the language.
        while (tok.is(__Label__))
        {
            SourceLocation labelLoc = consumeToken();
            diag(labelLoc, ext_gnu_local_label).emit();

            ArrayList<Decl> decls = new ArrayList<>();
            while (true)
            {
                if (tok.isNot(identifier))
                {
                    diag(tok, err_expected_ident).emit();
                    break;
                }

                IdentifierInfo ii = tok.getIdentifierInfo();
                SourceLocation idloc = consumeToken();
                decls.add(action.lookupOrCreateLabel(ii, idloc, labelLoc));

                if (tok.isNot(comma))
                {
                    break;
                }
                consumeToken();
            }

            DeclSpec ds = new DeclSpec();
            ArrayList<Decl> res = action.finalizeDeclaratorGroup(getCurScope(), ds, decls);
            ActionResult<Stmt> r = action.actOnDeclStmt(res, labelLoc, tok.getLocation());
            expectAndConsume(semi, err_expected_semi_declaration);
            if (r.isUsable())
            {
                stmts.add(r.get());
            }
        }
        while (nextTokenIsNot(r_brace) && nextTokenIsNot(eof))
        {
            ActionResult<Stmt> res;
            if (tok.isNot(__Extension__))
            {
                 res = parseStatementOrDeclaration(false);
            }
            else
            {
                // __extension__ can start declaration and it can also be a unary
                // operator for expression. Consume multiple __extension__ markers
                // until we can determine which is which.
                SourceLocation extLoc = consumeToken();
                while (tok.is(__Extension__))
                    consumeToken();

                // If this is the start of simple declaration, parse it.
                if (isDeclarationSpecifier())
                {
                    // __extension__ silence warnings in the subdecalaration.
                    SourceLocation declStart = tok.getLocation();
                    OutRef<SourceLocation> declEnd = new OutRef<>();
                    ArrayList<Decl> decls = parseDeclaration(TheContext.BlockContext, declEnd);
                    res = action.actOnDeclStmt(decls, declStart, declEnd.get());
                }
                else
                {
                    // Otherwise this is a unary __extension__ marker.
                    ActionResult<Expr> expr = parseExpressionWithLeadingExtension(extLoc);
                    if (expr.isInvalid())
                    {
                        skipUntil(semi, true);
                        continue;
                    }

                    expectAndConsume(semi, err_expected_semi_after_expr);
                    res = action.actOnExprStmt(expr);
                }
            }
            stmts.add(res.get());
        }

        // We broke out of the while loop because we found a '}' or EOF.
        if (nextTokenIsNot(r_brace))
        {
            diag(tok, err_expected_lbrace).emit();
            return stmtError();
        }
        if (tracker.consumeClose())
            return stmtError();

        // consume '}'

        return action.actOnCompoundStmtBody(tracker.getOpenLocation(),
                tracker.getCloseLocation(), stmts, isStmtExpr);
    }

    /**
     * This method be called when a leading __extension__ was seen and consumed.
     * This is neccessary because the token gets consumed in the process of
     * disambiguating between an expression and a decalaration.
     * @param extLoc
     * @return
     */
    private ActionResult<Expr> parseExpressionWithLeadingExtension(SourceLocation extLoc)
    {
        ActionResult<Expr> lhs = parseCastExpression(false, false, false);
        if (lhs.isInvalid())
            return lhs;

        lhs = action.actOnUnaryOp(extLoc, __Extension__, lhs.get());
        if (lhs.isInvalid())
            return lhs;

        return parseRHSOfBinaryExpression(lhs, PrecedenceLevel.Comma);
    }

    /**
     * Parse if statement.
     * <pre>
     *   if-statement: [C99 6.8.4.1]
     *     'if' '(' expression ')' statement
     *     'if' '(' expression ')' statement 'else' statement
     * </pre>
     * @return
     */
    private ActionResult<Stmt> parseIfStatement()
    {
        Util.assertion(nextTokenIs(If),  "Not an if stmt!");
        SourceLocation ifLoc = consumeToken(); // eat 'if'

        if (nextTokenIsNot(l_paren))
        {
            diag(tok, err_expected_lparen_after).addTaggedVal("if").emit();
            return stmtError();
        }
        // C99 6.8.4p3 - In C99, the if statement is a block.  This is not
        // the case for C90.
        // But we take the parser working in the C99 mode for convenience.

        boolean isC99 = getLangOption().c99;

        ParseScope ifScope = new ParseScope(this, DeclScope.value, isC99);
        ActionResult<Expr> condExpr;
        OutRef<ActionResult<Expr>> x = new OutRef<>();
        if (parseParenExprOrCondition(x, ifLoc, true))
            return stmtError();

        condExpr = x.get();

        if (tok.is(semi))
        {
            // cosume this ';'.
            consumeToken();
            diag(tok, warn_empty_if_body).emit();
            return action.actOnIfStmt(ifLoc, condExpr,
                    new Tree.NullStmt(SourceLocation.NOPOS),
                    new Tree.NullStmt(SourceLocation.NOPOS));
        }
        // C99 6.8.4p3
        // In C99, the body of the if statement is a scope, even if
        // there is only a single statement but compound statement.

        // the scope for 'then' statement if there is a '{'
        ParseScope InnerScope = new ParseScope(this,
                DeclScope.value,
                tok.isNot(l_brace) && isC99);

        SourceLocation thenStmtLoc = tok.getLocation();
        // parse the 'then' statement
        ActionResult<Stmt> thenStmt = parseStatement();

        // pop the 'then' scope if needed.
        InnerScope.exit();

        // IfStmt there is a 'else' statement, parse it.
        SourceLocation elseStmtLoc = SourceLocation.NOPOS;
        SourceLocation elseLoc = SourceLocation.NOPOS;
        ActionResult<Stmt> elseStmt = new ActionResult<>();

        if (nextTokenIs(Else))
        {
            // eat the 'else' keyword.
            elseLoc = consumeToken();
            elseStmtLoc = tok.getLocation();

            // the scope for 'else' statement if there is a '{'
            InnerScope = new ParseScope(this,
                    DeclScope.value,
                    nextTokenIs(l_brace) && getLangOption().c99);

            elseStmt = parseStatement();

            // Pop 'else' statement scope if needed.
            InnerScope.exit();
        }

        ifScope.exit();

        // IfStmt the condition expression is invalid and then return null as result.
        if (condExpr.isInvalid())
            return stmtError();

        if ((thenStmt.isInvalid() && elseStmt.isInvalid())
                || (thenStmt.isInvalid() && elseStmt.get() == null)
                || (thenStmt.get() == null && elseStmt.isInvalid()))
        {
            // Both invalid, or one is invalid other is non-present
            return stmtError();
        }

        // Now if both are invalid, replace with a ';'
        if (thenStmt.isInvalid())
            thenStmt = action.actOnNullStmt(thenStmtLoc);
        if (elseStmt.isInvalid())
            elseStmt = action.actOnNullStmt(elseStmtLoc);

        return action.actOnIfStmt(ifLoc, condExpr, thenStmt.get(), elseStmt.get());
    }

    /**
     * ParseParenExprOrCondition:
     * <pre>
     * [C  ]     '(' expression ')'
     * </pre>
     * @param exprResult
     * @param ifLoc
     * @param convertToBoolean
     * @return
     */
    private boolean parseParenExprOrCondition(
            OutRef<ActionResult<Expr>> exprResult,
            SourceLocation ifLoc,
            boolean convertToBoolean)
    {
        BalancedDelimiterTracker t = new BalancedDelimiterTracker(this, l_paren);
        t.consumeOpen();

        exprResult.set(parseExpression());

        // If require, convert to a boolean value.
        if (!exprResult.get().isInvalid() && convertToBoolean)
        {
            exprResult.set(action.actOnBooleanCondition(getCurScope(),
                    ifLoc, exprResult.get().get()));
        }

        // If the parser was confused by the condition and we don't have a ')', try to
        // recover by skipping ahead to a semi and bailing out.  If condexp is
        // semantically invalid but we have well formed code, keep going.
        if (exprResult.get().isInvalid() && tok.isNot(r_paren))
        {
            skipUntil(semi, true);
            // Skipping may have stopped if it found the containing ')'.  If so, we can
            // continue parsing the if statement.
            if (tok.isNot(r_paren))
                return true;
        }

        // Otherwise, the condition is valid or the right parenthesis is present.
        t.consumeClose();
        return false;
    }

    public static ActionResult<Stmt> stmtError()
    {
        return new ActionResult<>(true);
    }

    private ActionResult<Stmt> parseStatement()
    {
        return parseStatementOrDeclaration(true);
    }

    private ActionResult<QualType> parseTypeName()
    {
        return parseTypeName(null);
    }

	/**
     * type-asmName: [C99 6.7.6]
     *      specifier-qualifier-list abstract-declarator[opt]
     * @return
     */
    private ActionResult<QualType> parseTypeName(SourceRange range)
    {
        DeclSpec ds = new DeclSpec();
        parseSpecifierQualifierList(ds);

        // Parse the abstract-declarator, if present.
        Declarator declaratorInfo = new Declarator(ds, TheContext.TypeNameContext);
        parseDeclarator(declaratorInfo);
        if (range != null)
            range = declaratorInfo.getSourceRange();
        if (declaratorInfo.isInvalidType())
            return new ActionResult<>();

        return action.actOnTypeName(getCurScope(), declaratorInfo);
    }

    /**
     * This method parrse the unit that starts with a token "(",
     * based on what is allowed by {@code exprType}. The actual thing parsed is
     * returned in {@code exprType}. If {@code stopIfCastExpr} is true, it will
     * only return the parsed type, not the cast-expression.
     * <pre>
     * primary-expression: [C99 6.5.1]
     *   '(' expression ')'
     *
     * postfix-expression: [C99 6.5.2]
     * //TODO  '(' type-getIdentifier ')' '{' initializer-list '}'
     * //TODO  '(' type-getIdentifier ')' '{' initializer-list ',' '}'
     *   '(' type-getIdentifier ')' cast-expression
     * </pre>
     * @param exprType
     * @param stopIfCastExpr
     * @param parseAsExprList
     * @param castTy
     * @param rParenLoc
     * @return
     */
    private ActionResult<Expr> parseParenExpression(
            OutRef<ParenParseOption> exprType,
            boolean stopIfCastExpr,
            boolean parseAsExprList,
            OutRef<QualType> castTy,
            OutRef<SourceLocation> rParenLoc)
    {
        Util.assertion(nextTokenIs(l_paren), "Not a paren expression.");
        // eat the '('.
        SourceLocation lParenLoc = consumeParen();
        ActionResult<Expr> result = new ActionResult<>(true);

        if (exprType.get().compareTo(CompoundStmt) >= 0 && tok.is(l_brace))
        {
            diag(tok, ext_gnu_statement_expr).emit();
            ActionResult<Stmt> stmt = parseCompoundStatement(true, DeclScope.value);
            exprType.set(CompoundStmt);

            // If the substmt parsed correctly, build the AST node.
            if (!stmt.isInvalid() && tok.is(r_paren))
            {
                result = action.actOnStmtExpr(lParenLoc, stmt, tok.getLocation());
            }
        }
        else if (exprType.get().compareTo(CompoundLiteral) >= 0
                && isDeclarationSpecifier())
        {
            // This is a compound literal expression or cast expression.
            // First of all, parse declarator.
            ActionResult<QualType> ty = parseTypeName();

            // Match the ')'.
            if (nextTokenIs(r_paren))
                rParenLoc.set(consumeParen());
            else
                matchRHSPunctuation(r_paren, lParenLoc);

            if (nextTokenIs(l_brace))
            {
                exprType.set(CompoundLiteral);
                return parseCompoundLiteralExpression(ty.get(), lParenLoc, rParenLoc.get());
            }
            else if (exprType.get() == CastExpr)
            {
                // We parsed '(' type-getIdentifier ')' and the thing after it wasn't a '('.
                if (ty.isInvalid())
                    return exprError();

                castTy.set(ty.get());

                // Note that this doesn't parse the subsequent cast-expression, it just
                // returns the parsed type to the callee.
                if (stopIfCastExpr)
                {
                    return new ActionResult<Expr>();
                }

                // Parse the cast-expression that follows it next.
                result = parseCastExpression(
                        /*isUnaryExpression*/false,
                        /*isAddressOfOperands*/false,
                        /*(parseAsExprList)*/true);
                if (!result.isInvalid())
                {
                    result = action.actOnCastExpr(getCurScope(), lParenLoc,
                            castTy.get(), rParenLoc.get(), result.get());
                }
                return result;
            }
            diag(tok, err_expected_lbrace_in_compound_literal).emit();
            return exprError();
        }
        else if (parseAsExprList)
        {
            // Parse the expression-list.
            ArrayList<Expr> exprs = new ArrayList<>();
            ArrayList<SourceLocation> commaLocs = new ArrayList<>();

            if (!parseExpressionList(exprs, commaLocs))
            {
                exprType.set(SimpleExpr);
                result = action.actOnParenOrParenList(
                        rParenLoc.get(),
                        tok.getLocation(),
                        exprs);
            }
        }
        else
        {
            // simple simple surronding with '()'.
            result = parseExpression();
            exprType.set(SimpleExpr);

            // Don't build a parentheses expression, since it is not needed.
            if (!result.isInvalid() && nextTokenIs(r_paren))
            {
                // obtains the location of next token of ')'.
                SourceLocation rparen = tok.getLocation();
                result = action.actOnParenExpr(rParenLoc.get(), rparen, result.get());
            }
        }
        if (result.isInvalid())
        {
            skipUntil(r_paren, true);
            return exprError();
        }
        if (nextTokenIs(r_paren))
            rParenLoc.set(consumeParen());
        else
        matchRHSPunctuation(r_paren, lParenLoc);
        return result;
    }

    /**
     * We have parsed the parenthesized type-name and we are at the left brace.
     * <pre>
     *       postfix-expression: [C99 6.5.2]
     *         '(' type-name ')' '{' initializer-list '}'
     *         '(' type-name ')' '{' initializer-list ',' '}'
     * </pre>
     * @param ty
     * @param lParenLoc
     * @param rParenLoc
     * @return
     */
    private ActionResult<Expr> parseCompoundLiteralExpression(
            QualType ty,
            SourceLocation lParenLoc,
            SourceLocation rParenLoc)
    {
        Util.assertion(tok.is(l_brace), "The current token should be '{'");
        if (!getLangOption().c99)
        {
            diag(lParenLoc, ext_c99_compound_literal);
        }
        ActionResult<Expr> res = parseInitializer();
        if (!res.isInvalid() && !ty.isNull())
            return action.actOnCompoundLiteral(lParenLoc, ty, lParenLoc, res);

        return res;
    }

    /**
     * argument-expression-list:
     *  assignment-expression
     *  argument-expression-list , assignment-expression
     * @param exprs
     * @param commaLocs
     * @return
     */
    private boolean parseExpressionList(
            ArrayList<Expr> exprs,
            ArrayList<SourceLocation> commaLocs)
    {
        while(true)
        {
            ActionResult<Expr> res = parseAssignExpression();
            if (res.isInvalid())
                return true;

            exprs.add(res.get());
            if (nextTokenIsNot(comma))
                return false;

            // consume a ',' and add it's location into comma Locs list.
            commaLocs.add(consumeToken());
        }
    }

    /**
     * For sufficiency, A simple precedence-based jlang.parser for binary/unary operators.
     * <br>
     * Note: we diverge from the C99 grammar when parsing the assignment-expression
     * production.  C99 specifies that the LHS of an assignment operator should be
     * parsed as a unary-expression, but consistency dictates that it be a
     * conditional-expession.  In practice, the important thing here is that the
     * LHS of an assignment has to be an l-value, which productions between
     * unary-expression and conditional-expression don't produce.  Because we want
     * consistency, we parse the LHS as a conditional-expression, then check for
     * l-value-ness in semantic analysis stages.
     * <pre>
     *       multiplicative-expression: [C99 6.5.5]
     *         cast-expression
     *         multiplicative-expression '*' cast-expression
     *         multiplicative-expression '/' cast-expression
     *         multiplicative-expression '%' cast-expression
     *
     *       additive-expression: [C99 6.5.6]
     *         multiplicative-expression
     *         additive-expression '+' multiplicative-expression
     *         additive-expression '-' multiplicative-expression
     *
     *       shift-expression: [C99 6.5.7]
     *         additive-expression
     *         shift-expression '<<' additive-expression
     *         shift-expression '>>' additive-expression
     *
     *       relational-expression: [C99 6.5.8]
     *         shift-expression
     *         relational-expression '<' shift-expression
     *         relational-expression '>' shift-expression
     *         relational-expression '<=' shift-expression
     *         relational-expression '>=' shift-expression
     *
     *       equality-expression: [C99 6.5.9]
     *         relational-expression
     *         equality-expression '==' relational-expression
     *         equality-expression '!=' relational-expression
     *
     *       AND-expression: [C99 6.5.10]
     *         equality-expression
     *         AND-expression '&' equality-expression
     *
     *       exclusive-OR-expression: [C99 6.5.11]
     *         AND-expression
     *         exclusive-OR-expression '^' AND-expression
     *
     *       inclusive-OR-expression: [C99 6.5.12]
     *         exclusive-OR-expression
     *         inclusive-OR-expression '|' exclusive-OR-expression
     *
     *       logical-AND-expression: [C99 6.5.13]
     *         inclusive-OR-expression
     *         logical-AND-expression '&&' inclusive-OR-expression
     *
     *       logical-OR-expression: [C99 6.5.14]
     *         logical-AND-expression
     *         logical-OR-expression '||' logical-AND-expression
     *
     *       conditional-expression: [C99 6.5.15]
     *         logical-OR-expression
     *         logical-OR-expression '?' expression ':' conditional-expression
     * [GNU]   logical-OR-expression '?' ':' conditional-expression
     *
     *       assignment-expression: [C99 6.5.16]
     *         conditional-expression
     *         unary-expression assignment-operator assignment-expression
     *
     *       assignment-operator: one of
     *         = *= /= %= += -= <<= >>= &= ^= |=
     *
     *       expression: [C99 6.5.17]
     *         assignment-expression
     *         expression ',' assignment-expression
     * </pre>
     * @return
     */
    private ActionResult<Expr> parseExpression()
    {
        ActionResult<Expr> lhs = parseAssignExpression();
        if (lhs.isInvalid())
            return lhs;

        return parseRHSOfBinaryExpression(lhs, PrecedenceLevel.Comma);
    }

    /**
     * Parse switch statement.
     * <pre>
     *     switch-statement:
     *       'switch' '(' expression ')' statement
     * </pre>
     * @return
     */
    private ActionResult<Stmt> parseSwitchStatement()
    {
        Util.assertion(tok.is(Switch), "Not a switch statement?");
        // eat the 'switch'
        SourceLocation switchLoc = consumeToken();

        if (tok.isNot(l_paren))
        {
            diag(tok, err_expected_lparen_after).addTaggedVal("switch").emit();
            return stmtError();
        }
        // C99 6.8.4p3. In C99, the switch statements is a block. This is not
        // implemented in C90. So, just take C99 into consideration for convenience.
        int scopeFlags = ScopeFlags.SwitchScope.value | ScopeFlags.BreakScope.value;
        if (getLangOption().c99)
            scopeFlags |= DeclScope.value | ScopeFlags.ControlScope.value;
        ParseScope switchScope = new ParseScope(this, scopeFlags);

        // Parse the condition expression.
        ActionResult<Expr> condExpr;
        OutRef<ActionResult<Expr>> res = new OutRef<>();
        if (parseParenExprOrCondition(res, switchLoc, false))
        {
            return stmtError();
        }
        condExpr = res.get();

        ActionResult<Stmt> switchStmt = action.actOnStartOfSwitchStmt(switchLoc, condExpr.get());
        if (switchStmt.isInvalid())
        {
            // skip the switch body
            if (nextTokenIs(l_brace))
            {
                consumeToken();
                skipUntil(r_brace, false);
            }
            else
                skipUntil(semi, true);
            return switchStmt;
        }

        // C99 6.8.4p3 - In C99, the body of the switch statement is a scope, even if
        // there is no compound stmt.  C90 does not have this clause.  We only do this
        // if the body isn't a compound statement to avoid push/pop in common cases.
        ParseScope innerScope = new ParseScope(this, DeclScope.value,
                nextTokenIs(l_brace) & getLangOption().c99);

        // read the body statement
        ActionResult<Stmt> body = parseStatement();

        // pop innerScope
        innerScope.exit();
        switchScope.exit();

        if (body.isInvalid())
            body = action.actOnNullStmt(switchLoc);

        return  action.actOnFinishSwitchStmt(switchLoc, switchStmt.get(), body.get());
    }

    /**
     * <pre>
     * ParseWhileStatement
     *       while-statement: [C99 6.8.5.1]
     *         'while' '(' expression ')' statement
     * </pre>
     * @return
     */
    private ActionResult<Stmt> parseWhileStatement()
    {
        Util.assertion(nextTokenIs(While), "Not a while statement!");
        // eat the 'while'
        SourceLocation whileLoc = consumeToken();

        if (nextTokenIsNot(l_paren))
        {
            diag(whileLoc, err_expected_lparen_after)
                    .addTaggedVal("while").emit();
            return stmtError();
        }

        // C99 6.8.5p5 - In C99, the while statement is a block.  This is not
        // the case for C90.  Star the loop scope.
        int scopeFlags = 0;
        if (getLangOption().c99)
            scopeFlags = ScopeFlags.BreakScope.value
                | ScopeFlags.ContinueScope.value
                | ScopeFlags.ControlScope.value
                | DeclScope.value;
        else
            scopeFlags = ScopeFlags.BreakScope.value
                    | ScopeFlags.ContinueScope.value;

        ParseScope whileScope = new ParseScope(this, scopeFlags);

        OutRef<ActionResult<Expr>> wrapper = new OutRef<>();

        // parse the condition.
        if (parseParenExprOrCondition(wrapper, whileLoc, true))
            return stmtError();

        ActionResult<Expr> cond = wrapper.get();

        // C99 6.8.5p5 - In C99, the body of the if statement is a scope, even if
        // there is no compound stmt.  C90 does not have this clause.  We only do this
        // if the body isn't a compound statement to avoid push/pop in common cases.
        ParseScope innerScope = new ParseScope(this, DeclScope.value,
                nextTokenIs(l_brace) && getLangOption().c99);

        // parse the body of while stmt.
        ActionResult<Stmt> body = parseStatement();

        // Pop the body scope if needed
        innerScope.exit();
        whileScope.exit();

        if (cond.isInvalid() || body.isInvalid())
            return stmtError();

        return action.actOnWhileStmt(whileLoc, cond.get(), body.get());
    }

    /**
     * ParseDoStatement
     * <pre>
     * do-statement: [C99 6.8.5.2]
     *   'do' statement 'while' '(' expression ')' ';'
     * </pre>
     * Note: this lets the caller parse the end ';'.
     * @return
     */
    private ActionResult<Stmt> parseDoStatement()
    {
        Util.assertion(nextTokenIs(Do), "Not a do stmt!");

        // eat the 'do'.
        SourceLocation doLoc = consumeToken();
        int scopeFlags = 0;
        if (getLangOption().c99)
            scopeFlags = ScopeFlags.BreakScope.value|
                ScopeFlags.ContinueScope.value | DeclScope.value;
        else
            scopeFlags |= ScopeFlags.BreakScope.value |
                    ScopeFlags.ContinueScope.value;

        ParseScope doScope = new ParseScope(this, scopeFlags);

        ParseScope innerScope = new ParseScope(this,
                DeclScope.value,
                nextTokenIs(l_brace) && getLangOption().c99);

        ActionResult<Stmt> body = parseStatement();
        // Pop the body scope.
        innerScope.exit();

        if (nextTokenIsNot(While))
        {
            if (!body.isInvalid())
            {
                diag(tok, err_expected_while).emit();
                diag(doLoc, note_matching).addTaggedVal("do").emit();
                skipUntil(semi, true);
            }
            return stmtError();
        }

        // eat the 'while'.
        SourceLocation whileLoc = consumeToken();

        if (nextTokenIsNot(l_paren))
        {
            diag(tok, err_expected_lparen_after).addTaggedVal("do/while").emit();
            skipUntil(semi, true);
            return stmtError();
        }

        SourceLocation lParenLoc = consumeParen();  // eat '('.
        ActionResult<Expr> cond = parseExpression();
        SourceLocation rParenLoc = consumeParen();  // eat ')'.
        doScope.exit();

        if (cond.isInvalid() || body.isInvalid())
            return stmtError();
        return  action.actOnDoStmt(doLoc, body.get(), whileLoc, lParenLoc,
                cond.get(), rParenLoc);
    }

    /**
     * ParseForStatement
     * <pre>
     * for-statement: [C99 6.8.5.3]
     *   'for' '(' expr[opt] ';' expr[opt] ';' expr[opt] ')' statement
     *   'for' '(' declaration expr[opt] ';' expr[opt] ')' statement
     * </pre>
     * @return
     */
    private ActionResult<Stmt> parseForStatement()
    {
        Util.assertion(nextTokenIs(For), "Not a for loop");
        // eat the 'for'.
        SourceLocation forLoc = consumeToken();

        if (nextTokenIsNot(l_paren))
        {
            diag(tok, err_expected_lparen_after).addTaggedVal("for").emit();
            skipUntil(semi, true);
            return stmtError();
        }
        int scopeFlags;
        if (getLangOption().c99)
            scopeFlags = ScopeFlags.BreakScope.value
                | ScopeFlags.ContinueScope.value
                | DeclScope.value
                | ScopeFlags.ControlScope.value;
        else
            scopeFlags = ScopeFlags.BreakScope.value
                    | ScopeFlags.ContinueScope.value;

        ParseScope forScope = new ParseScope(this, scopeFlags);
        BalancedDelimiterTracker tracker = new BalancedDelimiterTracker(this, l_paren);
        tracker.consumeOpen();

        ActionResult<Expr> value;
        ActionResult<Stmt> firstPart = new ActionResult<Stmt>();
        ActionResult<Expr> secondPart = new ActionResult<Expr>();
        boolean secondPartIsInvalid = false;

        // parse the first part
        if (nextTokenIs(semi))
        {
            // for ';';
            consumeToken();
        }
        else if (isSimpleDeclaration())
        {
            // parse the declaration, for (int X = 4;
            SourceLocation declStart = tok.getLocation();
            OutRef<SourceLocation> end = new OutRef<>();
            ArrayList<Decl> declGroup = parseSimpleDeclaration(end,
                    TheContext.ForContext, false);

            if (tok.is(semi))
            {
                consumeToken(); // "for(int x = 1;)"
            }
            else
            {
                diag(tok, err_expected_semi_for);
                skipUntil(semi, true);
            }

            firstPart = action.actOnDeclStmt(declGroup, declStart, end.get());
        }
        else
        {
            // for (X = 4;
            value = parseExpression();
            if (!value.isInvalid())
                firstPart = action.actOnExprStmt(value);

            if (nextTokenIs(semi))
                consumeToken();
            else
            {
                if (!value.isInvalid())
                    diag(tok, err_expected_semi_for).emit();
                skipUntil(semi, true);
            }
        }

        // parse the second part of the for specifier
        if (nextTokenIs(semi))
        {
            // for (...;;
            // no second part
        }
        else if (nextTokenIs(r_paren))
        {
            // missing both semicolons
            // for (...;)
        }
        else
        {
            ActionResult<Expr> second = parseExpression();
            if (!second.isInvalid())
                second = action.actOnBooleanCondition(getCurScope(),
                        forLoc,
                        second.get());

            secondPartIsInvalid = second.isInvalid();
            secondPart = second;
        }

        if (nextTokenIsNot(semi))
        {
            if (!secondPartIsInvalid)
                diag(tok, err_expected_semi_for).emit();
            else
                skipUntil(r_paren, true);
        }

        if (nextTokenIs(semi))
            consumeToken();

        // parse the third part of for specifier
        ActionResult<Expr> thirdPart = null;
        if (nextTokenIsNot(r_paren))
        {
            // for (...;...;...)
            thirdPart = parseExpression();
        }

        if (!nextTokenIs(r_paren))
        {
            diag(tok, err_expected_lparen_after)
                    .addTaggedVal(tok.getIdentifierInfo())
                    .emit();
            skipUntil(r_brace, true);
        }

        tracker.consumeClose();

        // C99 6.8.5p5 - In C99, the body of the if statement is a scope, even if
        // there is no compound stmt.  C90 does not have this clause.  We only do this
        // if the body isn't a compound statement to avoid push/pop in common cases.

        ParseScope innerScope = new ParseScope(this,
                DeclScope.value,
                nextTokenIs(l_brace));

        ActionResult<Stmt> body = parseStatement();
        innerScope.exit();

        forScope.exit();

        if (body.isInvalid())
            return stmtError();

        return  action.actOnForStmt(forLoc,
                tracker.getOpenLocation(),
                firstPart != null?firstPart.get():null,
                secondPart.get(),
                thirdPart != null?thirdPart.get():null,
                tracker.getCloseLocation(),
                body.get());
    }

    private boolean isSimpleDeclaration()
    {
        return isDeclarationSpecifier(true);
    }

    /**
     * <pre>
     * simple-declaration: [C99 6.7: declaration]
     *   declaration-specifiers init-declarator-list[opt] ';'
     *
     *   declaration-specifiers:
     *     storage-class-specifier declaration-specifiers
     *     type-specifier declaration-specifiers
     *     type-qualifier declaration-specifiers
     *     function-specifier declaration-specifiers
     *
     *   initializer-declarator-list:
     *     init-declarator
     *     init-declarator initializer-declarator-list
     *
     *   init-declarator:
     *     declarator
     *     declarator = initializer
     * </pre>
     *
     * If RequireSemi is false, this does not check for a ';' at the end of the
     * declaration.  If it is true, it checks for and eats it.
     * @param end   The output parameter for obtaining the end location of this
     *              declaratin in source file.
     * @param context
     * @param requiredSemi
     * @return
     */
    private ArrayList<Decl> parseSimpleDeclaration(
            OutRef<SourceLocation> end,
            TheContext context, boolean requiredSemi)
    {
        // Parse the common declaration-specifiers piece.
        DeclSpec ds = new DeclSpec();
        parseDeclarationSpecifiers(ds);

        // C99 6.7.2.3p6: Handle "struct-or-union identifier;", "enum { X };"
        // declaration-specifiers init-declarator-list[opt] ';'
        if (nextTokenIs(semi))
        {
            // obtain end location of this struct/union/enum specifier.
            end.set(consumeToken());
            Decl decl = action.parsedFreeStandingDeclSpec(getCurScope(), ds);
            // TODO ds.complete(decl);
            return action.convertDeclToDeclGroup(decl);
        }

        return parseDeclGroup(ds, context, false, end);
    }

    private ArrayList<Decl> parseDeclGroup(DeclSpec ds,
            TheContext context,
            boolean allowFunctionDefinition)
    {
        return parseDeclGroup(ds, context, allowFunctionDefinition, null);
    }

    private ArrayList<Decl> parseDeclGroup(DeclSpec ds,
            TheContext context,
            boolean allowFunctionDefinition,
            OutRef<SourceLocation> end)
    {
        Declarator d = new Declarator(ds, context);
        parseDeclarator(d);

        // Bail out if the first declarator didn't seem well-formed.
        if (!d.hasName() && !d.mayOmitIdentifier())
        {
            // skip until ; or }
            skipUntil(r_brace, true);
            if (nextTokenIs(semi))
                consumeToken();
            return declGroups();
        }

        // check to see if we have a function "definition" which must heave a body.
        if (allowFunctionDefinition && d.isFunctionDeclarator()
                && !isDeclarationAfterDeclarator())
        {
            if (isStartOfFunctionDefinition(d))
            {
                if (ds.getStorageClassSpec() == SCS_typedef)
                {
                    diag(tok, err_function_declared_typedef).emit();

                    // recover by treating the 'typedef' as spurious
                    ds.clearStorageClassSpec();
                }

                Decl decl = parseFunctionDefinition(d);
                return action.convertDeclToDeclGroup(decl);
            }
            if (isDeclarationSpecifier())
            {
                // If there is an invalid declaration specifier right after the function
                // prototype, then we must be in a missing semicolon case where this isn't
                // actually a body.  Just fall through into the code that handles it as a
                // prototype, and let the top-level code handle the erroneous declspec
                // where it would otherwise expect a comma or semicolon.
                diag(tok, err_invalid_token_after_toplevel_declarator).emit();
            }
            else
            {
                diag(tok, err_expected_fn_body).emit();
            }
            skipUntil(semi, true);
            return declGroups();
        }

        // Parse the init-declarator-list for a normal declaration.
        ArrayList<Decl> res = parseInitDeclaratorListAfterFirstDeclarator
                (d, ds, context);

        // Set the end location for current declaration.
        if (end != null)
            end.set(tok.getLocation());

        if (context != TheContext.ForContext &&
                expectAndConsume(semi, context == FileContext ?
                err_invalid_token_after_toplevel_declarator :
                err_expected_semi_declaration))
        {
            // Okay, there was no semicolon and one was expected.  If we see a
            // declaration specifier, just assume it was missing and continue parsing.
            // Otherwise things are very confused and we skip to recover.
            if (!isDeclarationSpecifier())
            {
                skipUntil(r_brace, true, true);
                if (tok.is(semi))
                    consumeToken();
            }
        }
        return action.finalizeDeclaratorGroup(getCurScope(), ds, res);
    }

    private ArrayList<Decl> parseInitDeclaratorListAfterFirstDeclarator(
            Declarator d,
            DeclSpec ds,
            TheContext context)
    {
        ArrayList<Decl> declsInGroup = new ArrayList<>(8);
        Decl firstDecl = parseDeclarationAfterDeclarator(d);

        // Next stmt is used for diagnostic in Clang 3.0
        // d.complete(firstDecl);
        if (firstDecl != null)
            declsInGroup.add(firstDecl);

        // If we don't have a comma, it is either the end of the list (a ';') or an
        // error, bail out.
        while (nextTokenIs(comma))
        {
            // eat the ','.
            consumeToken();

            // clear the D for parsing next declarator.
            d.clear();

            // Accept attributes in an init-declarator.  In the first declarator in a
            // declaration, these would be part of the declspec.  In subsequent
            // declarators, they become part of the declarator itself, so that they
            // don't apply to declarators after *this* one.  Examples:
            //    short __attribute__((common)) var;    -> declspec
            //    short var __attribute__((common));    -> declarator
            //    short x, __attribute__((common)) var;    -> declarator
            if (tok.is(__Attribute))
            {
                Pair<AttributeList, SourceLocation> res = parseAttributes();
                d.addAttributes(res.first, res.second);
            }
            parseDeclarator(d);

            Decl thisDecl = parseDeclarationAfterDeclarator(d);

            // Next stmt is used for diagnostic in Clang 3.0
            // d.complete(thisDecl);
            if (thisDecl != null)
                declsInGroup.add(thisDecl);
        }

        if (context != TheContext.ForContext
                && nextTokenIsNot(semi))
        {
            if (!isDeclarationSpecifier())
            {
                skipUntil(r_brace, true);
                if (nextTokenIs(semi))
                    consumeToken();
            }
        }

        return declsInGroup;
    }

    /**
     * Parse 'declaration' after parsing 'declaration-specifiers
     * declarator'. This method parses the rem of the declaration
     * (including any attributes or initializer, among other things) and
     * finalizes the declaration.
     *
     *       init-declarator: [C99 6.7]
     *         declarator
     *         declarator '=' initializer
     * [GNU]   declarator simple-asm-expr[opt] attributes[opt]
     * [GNU]   declarator simple-asm-expr[opt] attributes[opt] '=' initializer
     * @param d
     * @return
     */
    private Decl parseDeclarationAfterDeclarator(Declarator d)
    {
        // If a simple-asm-expr is present, parse it.
        if (tok.is(Asm))
        {
            OutRef<SourceLocation> loc = new OutRef<>();
            ActionResult<Expr> res = parseSimpleAsm(loc);
            if (res.isInvalid())
            {
                skipUntil(semi, true, true);
                return null;
            }

            d.setAsmLabel(res.get());
            d.setRangeEnd(loc.get());
        }

        // if attributes are present, parse them.
        if (tok.is(__Attribute))
        {
            Pair<AttributeList, SourceLocation> res = parseAttributes();
            d.addAttributes(res.first, res.second);
        }

        // inform the semantic module that we just parsed this declarator.
        Decl thisDecl = action.actOnDeclarator(getCurScope(), d);
        if (nextTokenIs(equal))
        {
            // eat the '='.
            consumeToken();
            // Parses the initializer expression after than '='.
            ActionResult<Expr> init = parseInitializer();
            if (init.isInvalid())
            {
                skipUntil(comma, true);
                action.actOnInitializerError(thisDecl);
            }
            else
            {
                action.addInitializerToDecl(thisDecl, init.get(),
                /*DirectInit=*/false);
            }
        }
        else
        {
            action.actOnUninitializedDecl(thisDecl);
        }
        return thisDecl;
    }

    /**
     * C99 6.7.8: Initialization.
     * ParseInitializer
     *   initializer: [C99 6.7.8]
     *   assignment-expression
     *   '{' ...
     * @return
     */
    private ActionResult<Expr> parseInitializer()
    {
        if (tok.isNot(l_brace))
            return parseAssignExpression();
        return parseBraceInitializer();
    }

    public static ActionResult<Expr> exprError()
    {
        return new ActionResult<Expr>(true);
    }

	/**
     * Called when parsing an initializer that has a
     * leading open brace.
     * <pre>
     *
     *       initializer: [C99 6.7.8]
     *         '{' initializer-list '}'
     *         '{' initializer-list ',' '}'
     * [GNU]   '{' '}'
     *
     *       initializer-list:
     *         designation[opt] initializer
     *         initializer-list ',' designation[opt] initializer
     * </pre>
     * @return
     */
    private ActionResult<Expr> parseBraceInitializer()
    {
        SourceLocation lbraceLoc = consumeBrace();

        ArrayList<Expr> initExprs = new ArrayList<>();
        if (tok.is(r_brace))
        {
            diag(lbraceLoc, ext_gnu_empty_initializer).emit();
            return action.actOnInitList(lbraceLoc, emptyList(), consumeBrace());
        }

        boolean initExprOK = true;
        while (true)
        {
            // If we know that this cannot be a designation, just parse the nested
            // initializer directly.

            ActionResult<Expr> init;
            if (mayBeDesignationStart(tok.getKind(), pp))
                init = parseInitializerWithPotentialDesignator();
            else
                init = parseInitializer();

            if (!init.isInvalid())
                initExprs.add(init.get());
            else
            {
                initExprOK = false;
                if (nextTokenIsNot(comma))
                {
                    skipUntil(r_brace, false, true);
                    break;
                }
            }

            // If we don't have a comma continued list, we're done.
            if (nextTokenIsNot(comma))
                break;

            // Eat the ','.
            consumeToken();

            // Handle trailing comma.
            if (nextTokenIs(r_brace))
                break;
        }
        if (initExprOK && nextTokenIs(r_brace))
            return action.actOnInitList(lbraceLoc, initExprs, consumeBrace());;

        matchRHSPunctuation(r_brace, lbraceLoc);
        return exprError();
    }

    /**
     * Checks if it is possible the next token be a part of designation since C99.
     * <pre>
     * designation::=
     *             designator-list =
     * designator-list ::=
     *             designator
     *             designator designator-list
     * designator::=
     *             [constant-expression]
     *             . identifier
     * </pre>
     * @param kind
     * @param pp
     * @return
     */
    private boolean mayBeDesignationStart(TokenKind kind, Preprocessor pp)
    {
        switch (kind)
        {
            default:
                return false;
            case dot:
            case l_bracket:
                return true;
            case identifier:
                return pp.lookAhead(0).is(colon);
        }
    }

    /**
     * Parse the 'initializer' production
     * checking to see if the token stream starts with a designator.
     * <pre>
     *       designation:
     *         designator-list '='
     * [GNU]   array-designator
     * [GNU]   identifier ':'
     *
     *       designator-list:
     *         designator
     *         designator-list designator
     *
     *       designator:
     *         array-designator
     *         '.' identifier
     *
     *       array-designator:
     *         '[' constant-expression ']'
     * [GNU]   '[' constant-expression '...' constant-expression ']'
     * </pre>
     * @return
     */
    private ActionResult<Expr> parseInitializerWithPotentialDesignator()
    {
        if (tok.is(identifier))
        {
            IdentifierInfo fieldName = tok.getIdentifierInfo();

            String newSyntax = ".";
            newSyntax += fieldName.getName();
            newSyntax += " = ";

            SourceLocation nameLoc = consumeToken();
            Util.assertion(tok.is(colon), "mayBeDesignationStart not working properly!");

            SourceLocation colonLoc = consumeToken();
            diag(tok, ext_gnu_old_style_field_designator)
                    .addFixItHint(FixItHint.createReplacement(new SourceRange(nameLoc, colonLoc),
                            newSyntax))
                    .emit();

            Designation d = new Designation();
            d.addDesignator(Designator.getField(fieldName, new SourceLocation(), nameLoc));
            return action.actOnDesignatedInitializer(d, colonLoc,
                    true, parseInitializer());
        }

        Designation d = new Designation();
        // Parse each designator in the designator list until we find an initializer.
        while (tok.is(dot) || tok.is(l_bracket))
        {
            if (tok.is(dot))
            {
                // designator: '.' identifier
                SourceLocation dotLoc = consumeToken();

                if (tok.isNot(identifier))
                {
                    diag(tok.getLocation(), err_expected_field_designator).emit();
                    return exprError();
                }

                IdentifierInfo ii = tok.getIdentifierInfo();
                d.addDesignator(Designator.getField(ii, dotLoc, tok.getLocation()));
                consumeToken(); // Consume the identifier.
                continue;
            }
            Util.assertion( tok.is(l_bracket));

            // designator: '[' constant-expression ']'
            // designator: '[' constant-expression ... constant-expression']'
            SourceLocation lBracketLoc = consumeBracket();
            SourceLocation ellipseLoc;
            SourceLocation rBracketLoc;

            ActionResult<Expr> start = parseConstantExpression();
            if (start.isInvalid())
            {
                skipUntil(r_bracket, true);
                return start;
            }

            ActionResult<Expr> end = null;
            if (tok.is(ellipsis))
            {
                diag(tok, ext_gnu_array_range).emit();

                ellipseLoc = consumeToken();
                end = parseConstantExpression();
                if (end.isInvalid())
                {
                    skipUntil(r_bracket, true);
                    return end;
                }

                rBracketLoc = matchRHSPunctuation(r_bracket, lBracketLoc);

                d.addDesignator(Designator.getArrayRange(start, end,
                        lBracketLoc, ellipseLoc, rBracketLoc));
            }
            else
            {
                rBracketLoc = matchRHSPunctuation(r_bracket, lBracketLoc);
                d.addDesignator(Designator.getArray(start, lBracketLoc, rBracketLoc));
            }
        }

        Util.assertion(!d.isEmpty(), "Designator is emtpy?");

        // Handle a normal designator sequence end, which is an equal.
        if (tok.is(equal))
        {
            SourceLocation equalLoc = consumeToken();
            return action.actOnDesignatedInitializer(d, equalLoc,
                    false, parseInitializer());
        }

        // We read some number of designators and found something that isn't an = or
        // an initializer.  If we have exactly one array designator, this
        // is the GNU 'designation: array-designator' extension.  Otherwise, it is a
        // parse error.
        if (d.getNumDesignators() == 1 &&
                (d.getDesignator(0).isArrayDesignator() ||
                d.getDesignator(0).isArrayRangeDesignator()))
        {
            diag(tok, ext_gnu_missing_equal_designator)
                    .addFixItHint(FixItHint.createInsertion(tok.getLocation(),
                            "= "))
                    .emit();
            return action.actOnDesignatedInitializer(d, tok.getLocation(),
                    true, parseInitializer());
        }

        diag(tok, err_expected_equal_designator).emit();
        return exprError();
    }

    private ActionResult<Stmt> parseGotoStatement()
    {
        Util.assertion(nextTokenIs(Goto), "Not a goto stmt!");

        SourceLocation gotoLoc = consumeToken(); // eat the 'goto'

        // 'goto label'.
        ActionResult<Stmt> res = null;
        if (nextTokenIs(identifier))
        {
            LabelDecl ld = action.lookupOrCreateLabel(tok.getIdentifierInfo(),
                    tok.getLocation());
            res = action.actOnGotoStmt(gotoLoc, tok.getLocation(), ld);
            consumeToken();
        }
        else
        {
            // erroreous case
            diag(tok,  err_expected_ident).emit();
            return stmtError();
        }
        return res;
    }

    /**
     * Parse continue statement.
     * <pre>
     *   jump-statement:
     *     'continue' ';'
     * </pre>
     * Note: this lets the called parse the end ';'.
     * @return
     */
    private ActionResult<Stmt> parseContinueStatement()
    {
        Util.assertion(nextTokenIs(Continue), "Not a continue stmt!");
        SourceLocation continueLoc = consumeToken();
        return action.actOnContinueStmt(continueLoc, getCurScope());
    }

    /**
     * Parse break statement in C99 mode.
     * <pre>
     *   jump-statement:
     *     'break' ';'
     * </pre>
     * Note: this lets the called parse the end ';'.
     * @return
     */
    private ActionResult<Stmt> parseBreakStatement()
    {
        Util.assertion(nextTokenIs(Break), "Not a break stmt!");
        SourceLocation breakLoc = consumeToken();
        return action.actOnBreakStmt(breakLoc, getCurScope());
    }
    /**
     * Parse return statement in C99 mode.
     * <pre>
     *   jump-statement:
     *     'return' ';'
     *     'return' expression ';'
     * </pre>
     * @return
     */
    private ActionResult<Stmt> parseReturnStatement()
    {
        Util.assertion(nextTokenIs(Return), "Not a return stmt!");
        SourceLocation returnLoc = consumeToken();

        ActionResult<Expr> res = new ActionResult<>();
        if (nextTokenIsNot(semi))
        {
            // The case of return expression ';'.
            res = parseExpression();
            if (res.isInvalid())
            {
                skipUntil(semi, true);
                return stmtError();
            }
        }

        return  action.actOnReturnStmt(returnLoc, res.get());
    }

    /**
     * Parse a full 'declaration', which consists of declaration-specifiers,
     * some number of declarators, and a semicolon. 'Context' should be a
     * Declarator::TheContext value.  This returns the location of the
     * semicolon in declEnd.
     *
     *       declaration: [C99 6.7]
     *         block-declaration ->
     *           simple-declaration
     *           others                   [FIXME]
     * [C++]   template-declaration
     * [C++]   namespace-definition
     * [C++]   using-directive
     * [C++]   using-declaration
     * [C++0x] static_Util.assertion(-declaration     * @param dc
     * @param declEnd
     * @return
     */
    private ArrayList<Decl> parseDeclaration(
            TheContext dc,
            OutRef<SourceLocation> declEnd)
    {
        Util.assertion(declEnd != null);
        return parseSimpleDeclaration(declEnd, dc, false);
    }

    /**
     * Parses an expression statement.
     * @return
     */
    private ActionResult<Stmt> parseExprStatement()
    {
        Token oldTok = tok.clone();

        ActionResult<Expr> res = parseExpression();
        if (res.isInvalid())
        {
            // If the expression is invalid, skip ahead to the next semicolon
            // or '}'.
            skipUntil(r_brace, true);
            if (nextTokenIs(semi))
                consumeToken();
            return stmtError();
        }

        if (nextTokenIs(colon) && getCurScope().isSwitchScope()
                && action.checkCaseExpression(res.get()))
        {
            // If a constant expression is followed by a colon inside a switch block,
            // suggest a missing case keyword.
            diag(oldTok.getLocation(), err_expected_case_before_expression)
                .addFixItHint(FixItHint.createInsertion(oldTok.getLocation(), "case "))
                .emit();

            // Recover parsing as a case statement.
            return parseCaseStatement(true, res);
        }
        expectAndConsume(semi, err_expected_semi_after_expr);

        return action.actOnExprStmt(res);
    }

    /**
     * Checks if the next token, it occurs after a declarator, indicates the
     * start of a function definition.
     * @param declarator
     * @return
     */
    private boolean isStartOfFunctionDefinition(Declarator declarator)
    {
        Util.assertion(declarator.isFunctionDeclarator(), "Isn't a function declarator");
        // int X() {}
        if (tok.is(l_brace))
            return true;

        // K&R C function declaration.
        // void foo(a, b) int a, int b; {}
        return isDeclarationSpecifier();
    }
    /**
     * Determines whether the current token is the part of declaration or
     * declaration list, if it occurs after a declarator.
     * @return
     */
    private boolean isDeclarationAfterDeclarator()
    {
        return tok.is(equal)            // int X()= -> not a function def
                || tok.is(comma)         // int X(), -> not a function def
                || tok.is(semi)          // int X(); -> not a function def
                || tok.is(__Attribute)  // int X() __attribute__ not a function def.
                || tok.is(Asm);          // int X() __asm__() not a function def.
    }

    /**
     * Parse some declaration specifiers (possibly none) (C90 6.5, C99
     * 6.7), adding them to SPECS (which may already include some).
     * <p>
     * <pre>
     *
     * declaration-specifiers:
     *   storage-class-specifier declaration-specifiers[opt]
     *   type-specifier declaration-specifiers[opt]
     *   type-qualifier declaration-specifiers[opt]
     *   function-specifier declaration-specifiers[opt]
     *
     * FunctionProto specifiers (inline) are from C99, and are currently
     *   handled as storage class specifiers, as is __thread.
     *
     * C90 6.5.1, C99 6.7.1:
     * storage-class-specifier:
     *   typedef
     *   extern
     *   static
     *   auto
     *   register
     *
     * C99 6.7.4:
     * function-specifier:
     *   inline
     *
     * C90 6.5.2, C99 6.7.2:
     * type-specifier:
     *   void
     *   char
     *   short
     *   int
     *   long
     *   float
     *   double
     *   signed
     *   unsigned
     *   _Bool
     *   _Complex
     *   [_Imaginary removed in C99 TC2]
     *   struct-or-union-specifier
     *   enum-specifier
     *   typedef-getIdentifier
     *
     * (_Bool and _Complex are new in C99.)
     *
     * C90 6.5.3, C99 6.7.3:
     *
     * type-qualifier:
     *   const
     *   restrict
     *   volatile
     *
     * (restrict is new in C99.)
     * </pre>
     *
     * @param declSpecs
     */
    private void parseDeclarationSpecifiers(DeclSpec declSpecs)
    {
        if (declSpecs.getSourceRange().isInvalid())
        {
            SourceLocation loc = tok.getLocation();
            declSpecs.setRangeStart(loc);
            declSpecs.setRangeEnd(loc);
        }

        out:
        while (true)
        {
            SourceLocation loc = tok.getLocation();
            boolean isInvalid = false;
            String prevSpec = "";
            int diagID = -1;

            switch (tok.getKind())
            {
                case identifier:
                {
                    // This identifier can only be a typedef identifier if we haven't
                    // already seen a type-specifier.
                    if (declSpecs.hasTypeSpecifier())
                        break out;

                    // So, the current token is a typedef identifier or error.
                    QualType type = action.getTypeByName(
                            tok.getIdentifierInfo(),
                            tok.getLocation(),
                            getCurScope());
                    if (type == null)
                    {
                        // return true, if there is incorrect.
                        if (parseImplicitInt(declSpecs))
                            continue;
                        break out;
                    }

                    OutRef<String> wrapper1 = new OutRef<>(prevSpec);
                    OutRef<Integer> wrapper2 = new OutRef<>(diagID);

                    isInvalid = declSpecs.setTypeSpecType(TST_typename, loc,
                            wrapper1, wrapper2, type);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();

                    if (isInvalid)
                        break;

                    declSpecs.setRangeEnd(tok.getLocation());
                    // the identifier
                    consumeToken();
                    continue;
                }

                // GNU attributes support.
                case __Attribute:
                    declSpecs.addAttributes(parseAttributes().first);
                    continue;

                case Static:
                    declSpecs.setStorageClassSpec(DeclSpec.SCS.SCS_static, loc);
                    break;
                case Extern:
                    isInvalid = declSpecs
                            .setStorageClassSpec(DeclSpec.SCS.SCS_extern, loc);
                    break;
                case Register:
                    declSpecs.setStorageClassSpec(DeclSpec.SCS.SCS_register,
                            loc);
                    break;
                case Typedef:
                    isInvalid = declSpecs
                            .setStorageClassSpec(SCS_typedef, loc);
                    break;
                case Auto:
                    declSpecs.setStorageClassSpec(DeclSpec.SCS.SCS_auto, loc);
                    break;

                case Inline:
                    declSpecs.setFunctionSpecInline(loc);
                    break;
                case __Thread:
                    Util.assertion(false, "Thread not supported.");
                    break;


                case Unsigned:
                    isInvalid = declSpecs.setTypeSpecSign(TSS_unsigned, loc);
                    break;
                case Signed:
                    isInvalid = declSpecs.setTypeSpecSign(TSS_signed, loc);
                    break;

                case Long:
                    if (declSpecs.getTypeSpecWidth() != TSW_long)
                        isInvalid = declSpecs.setTypeSpecWidth(TSW_long, loc);
                    else
                        isInvalid = declSpecs.setTypeSpecWidth(TSW_longlong, loc);
                    break;
                case Short:
                    isInvalid = declSpecs.setTypeSpecWidth(TSW_short, loc);
                    break;
                case Complex:
                    isInvalid = declSpecs.setTypeSpecComplex(TSC_complex, loc);
                    break;
                case Char:
                {
                    OutRef<String> wrapper1 = new OutRef<>(prevSpec);
                    OutRef<Integer> wrapper2 = new OutRef<>(diagID);
                    isInvalid = declSpecs.setTypeSpecType(TST_char, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                case Int:
                {
                    OutRef<String> wrapper1 = new OutRef<>(prevSpec);
                    OutRef<Integer> wrapper2 = new OutRef<>(diagID);
                    isInvalid = declSpecs.setTypeSpecType(TST_int, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                case Float:
                {
                    OutRef<String> wrapper1 = new OutRef<>(prevSpec);
                    OutRef<Integer> wrapper2 = new OutRef<>(diagID);
                    isInvalid = declSpecs.setTypeSpecType(TST_float, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                case Double:
                {
                    OutRef<String> wrapper1 = new OutRef<>(prevSpec);
                    OutRef<Integer> wrapper2 = new OutRef<>(diagID);
                    isInvalid = declSpecs.setTypeSpecType(TST_double, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                case Void:
                {
                    OutRef<String> wrapper1 = new OutRef<>(prevSpec);
                    OutRef<Integer> wrapper2 = new OutRef<>(diagID);
                    isInvalid = declSpecs.setTypeSpecType(TST_void, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                case Bool:
                {
                    OutRef<String> wrapper1 = new OutRef<>(prevSpec);
                    OutRef<Integer> wrapper2 = new OutRef<>(diagID);
                    isInvalid = declSpecs.setTypeSpecType(TST_bool, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                case _Decimal32:
                    break;
                case _Decimal64:
                    break ;
                case _Decimal128:
                    break;
                // enum specifier
                case Enum:
                {
                    consumeToken();
                    parseEnumSpecifier(loc, declSpecs);
                    continue;
                }
                // struct-union specifier
                case Struct:
                case Union:
                {
                    // eat the 'struct' or 'union'.
                    TokenKind kind = tok.getKind();
                    consumeToken();
                    parseStructOrUnionSpecifier(kind, loc, declSpecs);
                    continue;
                }
                // type-qualifiers
                case Const:
                    isInvalid = declSpecs.setTypeQualifier(TQ_const, loc);
                    break;
                case Volatile:
                    isInvalid = declSpecs.setTypeQualifier(TQ_volatile, loc);
                    break;
                case Restrict:
                    isInvalid = declSpecs.setTypeQualifier(TQ_restrict, loc);
                    break;
                // GNU typeof support.
                case Typeof:
                    break;

                default:
                    break out;
            }
            // if the specifier is illegal, issue a diagnostic.
            if (isInvalid)
            {
                Util.assertion(prevSpec != null, "Method did not return previous specifier!");
                Util.assertion( diagID >= 0);
                diag(tok, diagID).addTaggedVal(prevSpec).emit();
            }
            declSpecs.setRangeEnd(tok.getLocation());
            consumeToken();
        }
        // finish and return to caller
        declSpecs.finish(diags, pp);
    }

    /**
     * This method is called when we have an non-typename
     * identifier in a declspec (which normally terminates the decl spec) when
     * the declspec has no type specifier.  In this case, the declspec is either
     * malformed or is "implicit int" (in K&R and C89).
     *
     * @param ds
     * @return
     */
    private boolean parseImplicitInt(DeclSpec ds)
    {
        Util.assertion(tok.is(identifier),  "should have identifier.");
        SourceLocation loc = tok.getLocation();

        // IfStmt we see an identifier that is not a type getIdentifier, we normally would
        // parse it as the identifer being declared.  However, when a typename
        // is typo'd or the definition is not included, this will incorrectly
        // parse the typename as the identifier getIdentifier and fall over misparsing
        // later parts of the diagnostic.
        //
        // As such, we try to do some lookup-ahead in cases where this would
        // otherwise be an "implicit-int" case to see if this is invalid.  For
        // example: "static foo_t x = 4;"  In this case, if we parsed foo_t as
        // an identifier with implicit int, we'd get a parse error because the
        // next token is obviously invalid for a type.  Parse these as a case
        // with an invalid type specifier.
        Util.assertion(!ds.hasTypeSpecifier(),  "Type specifier checked above");

        // Since we know that this either implicit int (which is rare) or an
        // error, we'd do lookahead to try to do better recovery.
        if (isValidAfterIdentifierDeclarator(nextToken()))
        {
            // if this token is valid for implicit int,
            // e.g. "static x = 4", then we just avoid eating the identifier, so
            // it will be parsed as the identifier in the declarator.
            return false;
        }

        String tagName = null;
        TokenKind tagKind = Unknown;
        IdentifierInfo identifierInfo = tok.getIdentifierInfo();
        switch (action.isTagName(identifierInfo, getCurScope()))
        {
            default:break;
            case TST_enum: tagName = "enum"; tagKind = Enum; break;
            case TST_struct: tagName = "struct"; tagKind = Struct; break;
            case TST_union: tagName = "union"; tagKind = Union; break;
        }
        if (tagName != null)
        {
            diag(loc, err_use_of_tag_name_without_tag)
                .addTaggedVal(tagName)
                .addTaggedVal(identifierInfo)
                .addFixItHint(FixItHint.createInsertion(tok.getLocation(),
                    tagName))
                .emit();

            if (tagKind == Enum)
            {
                parseEnumSpecifier(loc, ds);
            }
            else
            {
                parseStructOrUnionSpecifier(tagKind, loc, ds);
            }
            return true;
        }
        diag(loc, err_unknown_typename).addTaggedVal(identifierInfo).emit();

        OutRef<String> prevSpec = new OutRef<>();
        OutRef<Integer> diag = new OutRef<>();
        // mark as an error
        ds.setTypeSpecType(TST_error, loc, prevSpec, diag);
        ds.setRangeEnd(tok.getLocation());
        consumeToken();
        return false;
    }

    /**
     * ReturnStmt true if the specified token is valid after the identifier in a
     * declarator which immediately follows the declspec.  For example, these
     * things are valid:
     * <p>
     * int x   [             4];         // direct-declarator
     * int x   (             int y);     // direct-declarator
     * int(int x   )                         // direct-declarator
     * int x   ;                         // simple-declaration
     * int x   ,             y;          // init-declarator-list
     * int x   =             17;         // init-declarator-list
     * int x __asm__        ("foo");     // init-declarator-list
     * int x   :             4;          // struct-declarator
     * <p>
     * This is not, because 'x' does not immediately follow the declspec (though
     * ')' happens to be valid anyway).
     * int (x)
     */
    private boolean isValidAfterIdentifierDeclarator(Token tok)
    {
        return tok.is(l_bracket) || tok.is(l_paren) || tok.is(r_paren)
                || tok.is(semi) || tok.is(comma) || tok.is(equal) ||
                tok.is(Asm) || tok.is(colon);
    }

    private Scope getCurScope()
    {
        return action.getCurScope();
    }

    /**
     * Parse an enum specifier (C90 6.5.2.2, C99 6.7.2.2).
     * <pre>
     * enum-specifier:
     *   enum identifier[opt] { enumerator-list }
     *   enum identifier[opt] { enumerator-list , } [C99]
     *   enum identifier
     * </pre>
     * @param startLoc
     * @param ds
     */
    private void parseEnumSpecifier(SourceLocation startLoc, DeclSpec ds)
    {
        AttributeList attr = null;

        if (tok.is(__Attribute))
        {
            attr = parseAttributes().first;
        }

        // Must have either 'enum asmName' or 'enum {...}'.
        if (!tok.is(identifier) && !tok.is(l_brace))
        {
            diag(tok, err_expected_ident_lbrace).emit();
            // skip the rest of this declarator, up until a ',' or ';' encounter.
            skipUntil(comma, true);
            return;
        }

        IdentifierInfo name = null;
        SourceLocation nameLoc = SourceLocation.NOPOS;
        if (tok.is(identifier))
        {
            name = tok.getIdentifierInfo();
            nameLoc = tok.getLocation();

            // Consume 'enum-name' character.
            consumeToken();
        }

        // There are three options here.  If we have 'enum foo;', then this is a
        // forward declaration.  If we have 'enum foo {...' then this is a
        // definition. Otherwise we have something like 'enum foo xyz', a reference.
        Sema.TagUseKind tuk;
        if (tok.is(l_brace))
            tuk = TUK_definition;
        else if (tok.is(semi))
            tuk = TUK_declaration;
        else
            tuk = TUK_reference;

        if (name == null && tuk != TUK_definition)
        {
            diag(tok, err_enumerator_unnamed_no_def).emit();
            // Skip the rest of this declarator, up until the comma or semicolon.
            skipUntil(comma, true);
            return;
        }

        ActionResult<Decl> tagDecl = action.actOnTag(
                getCurScope(),
                TST_enum,
                tuk,
                startLoc,
                name,
                nameLoc,
                attr);

        if (tagDecl.isInvalid())
        {
            // the action failed to produce an enumeration getKind().
            // if this is a definition, consume the entire definition.
            if (tok.is(l_brace))
            {
                consumeToken();
                skipUntil(r_brace, true);
            }

            ds.setTypeSpecError();
            return;
        }

         if (tok.is(l_brace))
            parseEnumBody(startLoc, tagDecl.get());

        OutRef<String> prevSpecWrapper = new OutRef<>();
        OutRef<Integer> diagWrapper = new OutRef<>();

        if (ds.setTypeSpecType(TST_enum,
                startLoc,
                name != null ? nameLoc : startLoc,
                prevSpecWrapper, diagWrapper,
                tagDecl.get()))
        {
            diag(startLoc, diagWrapper.get())
                    .addTaggedVal(prevSpecWrapper.get())
                    .emit();
        }
    }

    /**
     * Parse a {} enclosed enumerator-list.
     * <pre>
     * enumerator-list:
         enumerator
         enumerator-list , enumerator

         enumerator:
         enumeration-constant
         enumeration-constant = constant-expression
     * </pre>
     * @param startLoc
     * @param enumDecl
     */
    private void parseEnumBody(SourceLocation startLoc, Decl enumDecl)
    {
        ParseScope enumScope = new ParseScope(this, DeclScope.value);
        action.actOnTagStartDefinition(getCurScope(), enumDecl);

        // eat '{'
        SourceLocation lbraceLoc = consumeBrace();
        if (nextTokenIs(r_brace))
        {
            diag(tok, ext_empty_struct_union_enum).emit();
        }

        ArrayList<Decl> enumConstantDecls = new ArrayList<>(32);
        Decl lastEnumConstDecl = null;
        // Parse the enumerator-list.
        while(tok.is(identifier))
        {
            IdentifierInfo name = tok.getIdentifierInfo();
            SourceLocation identLoc = consumeToken();

            SourceLocation equalLoc = SourceLocation.NOPOS;
            ActionResult<Expr> val = new ActionResult<>();
            if (nextTokenIs(equal))
            {
                equalLoc = consumeToken();
                val = parseConstantExpression();
                // if the constant expression is invalid, skip rest of
                // enum-declaration-list until a comma.
                if (val.isInvalid())
                {
                    skipUntil(comma, r_brace, true);
                }
            }

            // install the enumerator constant into enumDecl
            Decl enumConstDecl = action.actOnEnumConstant(
                    getCurScope(),
                    enumDecl,
                    lastEnumConstDecl,
                    identLoc,
                    name,
                    equalLoc,
                    val.get());

            enumConstantDecls.add(enumConstDecl);
            lastEnumConstDecl = enumConstDecl;

            // finish enumerator-list
            if (nextTokenIsNot(comma))
                break;
            SourceLocation commaLoc = consumeToken();

            if (nextTokenIsNot(identifier) && !getLangOption().c99)
            {
                // we missing a ',' between enumerators
                diag(commaLoc, ext_enumerator_list_comma)
                    .addTaggedVal(getLangOption().c99)
                    .addFixItHint(FixItHint.createRemoval(new SourceRange(commaLoc)))
                    .emit();
            }
        }

        // eat the '}'
        SourceLocation rBraceLoc = matchRHSPunctuation(r_brace, lbraceLoc);

        AttributeList attr = null;
        if (tok.is(__Attribute))
        {
            attr = parseAttributes().first;
        }

        action.actOnEnumBody(
                startLoc,
                lbraceLoc,
                rBraceLoc,
                enumDecl,
                enumConstantDecls,
                getCurScope(),
                attr);

        enumScope.exit();
        action.actOnTagFinishDefinition(getCurScope(), enumDecl, rBraceLoc);
    }

    /**
     * Parse a non-empty attributes list.
     *
     * [GNU] attributes:
     *         attribute
     *         attributes attribute
     *
     * [GNU]  attribute:
     *          '__attribute__' '(' '(' attribute-list ')' ')'
     *
     * [GNU]  attribute-list:
     *          attrib
     *          attribute_list ',' attrib
     *
     * [GNU]  attrib:
     *          empty
     *          attrib-name
     *          attrib-name '(' identifier ')'
     *          attrib-name '(' identifier ',' nonempty-expr-list ')'
     *          attrib-name '(' argument-expression-list [C99 6.5.2] ')'
     *
     * [GNU]  attrib-name:
     *          identifier
     *          typespec
     *          typequal
     *          storageclass
     *          
     * FIXME: The GCC grammar/code for this construct implies we need two
     * token lookahead. Comment from gcc: "If they start with an identifier 
     * which is followed by a comma or close parenthesis, then the arguments 
     * start with that identifier; otherwise they are an expression list."
     *
     * At the moment, I am not doing 2 token lookahead. I am also unaware of
     * any attributes that don't work (based on my limited testing). Most
     * attributes are very simple in practice. Until we find a bug, I don't see
     * a pressing need to implement the 2 token lookahead.
     * @return The returned value's sencond element represents the end location
     * of this attribute list.
     */
    private Pair<AttributeList, SourceLocation> parseAttributes()
    {
        Util.assertion(tok.is(__Attribute), "Not an attributes list!");

        AttributeList attr = null;
        SourceLocation loc = new SourceLocation();
        while (tok.is(__Attribute))
        {
            // eat the "__attribute__"
            consumeToken();
            if (expectAndConsume(l_paren, err_expected_lparen_after, "attribute", Unknown))
            {
                skipUntil(r_paren, true);
                return Pair.get(attr, new SourceLocation());
            }
            if (expectAndConsume(l_paren, err_expected_lparen_after, "(", Unknown))
            {
                skipUntil(r_paren, true);
                return Pair.get(attr, new SourceLocation());
            }

            // Parse the attribute list. e.g. __attribute_((weak, alias("__f"))).
            while (tok.is(identifier) || isDeclarationSpecifier() ||
                    tok.is(comma))
            {
                if(tok.is(comma))
                {
                    consumeToken();
                    continue;
                }

                IdentifierInfo attrName = tok.getIdentifierInfo();
                SourceLocation attrNameLoc = consumeToken();

                // check if we have "paramerized" attribute.
                if (tok.is(l_paren))
                {
                    consumeParen();

                    if (tok.is(identifier))
                    {
                        IdentifierInfo paramName = tok.getIdentifierInfo();
                        SourceLocation paramLoc = consumeToken();

                        if (tok.is(r_paren))
                        {
                            // __attribute__(( mode(byte) ))
                            consumeParen();
                            attr = new AttributeList(attrName, attrNameLoc,
                                    paramName, paramLoc, null, attr);
                        }
                        else if (tok.is(comma))
                        {
                            // __attribute__(( format(printf, 1, 2) ))
                            consumeToken();

                            ArrayList<Expr> args = new ArrayList<>();
                            boolean argExprsOK = true;
                            // now parse the non-empty comma separated list
                            // of expression.
                            while (true)
                            {
                                ActionResult<Expr> arg = parseAssignExpression();
                                if (arg.isInvalid())
                                {
                                    argExprsOK = false;
                                    skipUntil(r_paren, true);
                                    break;
                                }
                                else
                                {
                                    args.add(arg.get());
                                }
                                if (!tok.is(comma))
                                    break;
                                // eat the ',' move to the next argument.
                                consumeToken();
                            }
                            if (argExprsOK && tok.is(r_paren))
                            {
                                consumeParen();
                                attr = new AttributeList(attrName, attrNameLoc,
                                        paramName, paramLoc, args.toArray(),
                                        attr);
                            }
                        }
                    }
                    else
                    {
                        // not a identifier.
                        switch (tok.getKind())
                        {
                            case r_paren:
                                // parse a possibly empty comma separated list of expressions
                                // __attribute__(( nonnull() ))
                                consumeParen();
                                attr = new AttributeList(attrName, attrNameLoc,
                                        null, new SourceLocation(), null, attr);
                                break;
                            case Char:
                            case Bool:
                            case Short:
                            case Int:
                            case Long:
                            case Signed:
                            case Unsigned:
                            case Float:
                            case Double:
                            case Void:
                            case Typedef:
                            {
                                consumeToken();
                                attr = new AttributeList(attrName, attrNameLoc,
                                        null, new SourceLocation(),
                                        null, attr);
                                if (tok.is(r_paren))
                                    consumeParen();
                                break;
                            }
                            default:
                            {
                                ArrayList<Expr> argExprs = new ArrayList<>();
                                boolean argExprOK = true;
                                // __attribute__(( aligned(16) ))
                                // now parse the list of expressions
                                while (true)
                                {
                                    ActionResult<Expr> argExpr = parseAssignExpression();
                                    if (argExpr.isInvalid())
                                    {
                                        argExprOK = false;
                                        skipUntil(r_paren, true);
                                        break;
                                    }
                                    else
                                    {
                                        argExprs.add(argExpr.get());
                                    }
                                    if (tok.isNot(comma))
                                        break;
                                    consumeParen();
                                }
                                // Match ')'
                                if (argExprOK && tok.is(r_paren))
                                {
                                    consumeParen();
                                    attr = new AttributeList(attrName, attrNameLoc,
                                            null,
                                            new SourceLocation(), argExprs.toArray(),
                                            attr);
                                }
                                break;
                            }
                        }
                    }
                }
                else
                {
                    attr = new AttributeList(attrName, attrNameLoc, null,
                            new SourceLocation(), null, attr);
                }
            }
            if (expectAndConsume(r_paren, err_expected_rparen))
                skipUntil(r_paren, false);
            loc = tok.getLocation();
            if (expectAndConsume(r_paren, err_expected_rparen))
                skipUntil(r_paren, false);
        }
        return Pair.get(attr, loc);
    }

    /**
     * Parses C structs or unions specifiers.
     * <pre>
     *     struct-or-union-specifier: [C99 6.7.2.1]
     *       struct-or-union identifier[opt] '{' struct-contents '}'
     *       struct-or-union identifier
     *
     *     struct-contents:
     *       struct-declaration-list
     *
     *     struct-declaration-list:
     *       struct-declaration ;
     *       struct-declaration-list struct-declaration ;
     *
     *     struct-or-union:
     *      'struct'
     *      'union'
     * </pre>
     *
     */
    private void parseStructOrUnionSpecifier(
            TokenKind tagTokKind,
            SourceLocation startLoc,
            DeclSpec ds)
    {
        DeclSpec.TST tagType;
        if (tagTokKind == Struct)
            tagType = TST_struct;
        else
        {
            Util.assertion(tagTokKind == Union,  "Not a union specifier");
            tagType = TST_union;
        }

        AttributeList attr = null;

        // if attributes exist after tag, parse them.
        if (tok.is(__Attribute))
        {
            attr = parseAttributes().first;
        }

        // Parse the (optional) class asmName
        IdentifierInfo name = null;
        SourceLocation nameLoc = SourceLocation.NOPOS;
        if (nextTokenIs(identifier))
        {
            name = tok.getIdentifierInfo();
            nameLoc = consumeToken();
        }

        // There are four cases for the grammar starting with keyword class-specifier.
        // 1. forward declaration: for example, struct X;
        // 2. definition: for example, struct X {...};
        // 3. annoymous definition of struct: struct {...};
        // 4. reference used in varaible declaration: struct X x;

        Sema.TagUseKind tuk;
        if (nextTokenIs(l_brace))
        {
            // so,this is a struct definition.
            tuk = TUK_definition;
        }
        else if (nextTokenIs(semi))
            tuk = TUK_declaration;
        else
            tuk = TUK_reference;

        if (name == null && (ds.getTypeSpecType() == TST_error
                || tuk != TUK_definition))
        {
            if (ds.getTypeSpecType() != TST_error)
            {
                // we have a declaration or reference to an anonymous struct.
                diag(startLoc, err_anon_type_definition).emit();
            }
            skipUntil(comma, true);
            return;
        }

        // create the getKind() portion of the struct or union.

        // declaration or definitions of a struct or union type
        ActionResult<Decl> tagOrTempResult =
                action.actOnTag(
                getCurScope(), tagType,
                tuk, startLoc, name, nameLoc, attr);
        // if there is a body, parse it and perform actions
        if (nextTokenIs(l_brace))
        {
            parseStructOrUnionBody(startLoc, tagType, tagOrTempResult.get());
        }
        else if (tuk == TUK_definition)
        {
            diag(tok, err_expected_lbrace).emit();
        }

        if (tagOrTempResult.isInvalid())
        {
            ds.setTypeSpecError();
            return;
        }

        OutRef<String> w1 = new OutRef<>("");
        OutRef<Integer> w2 = new OutRef<>(0);
        String prevSpec = null;
        int diagID = 0;
        boolean result;

        result = ds.setTypeSpecType(tagType, startLoc,
                nameLoc!=SourceLocation.NOPOS?nameLoc: startLoc,
                w1, w2,
                tagOrTempResult.get());
        prevSpec = w1.get();
        diagID = w2.get();

        if (result)
        {
            // report error
            diag(startLoc, diagID).addTaggedVal(prevSpec).emit();
        }

        if (tuk == TUK_definition)
        {
            boolean expectedSemi = true;
            switch (tok.getKind())
            {
                default:
                    break;
                    // struct foo {...} ;
                case semi:
                    // struct foo {...} *         P;
                case star:
                    // struct foo {...} V         ;
                case identifier:
                    //(struct foo {...} )         {4}
                case r_paren:
                    // struct foo {...} (         x);
                case l_paren:
                    expectedSemi = false;
                    break;
                // type-specifier
                case Const:             // struct foo {...} const     x;
                case Volatile:          // struct foo {...} volatile     x;
                case Restrict:          // struct foo {...} restrict     x;
                case Inline:            // struct foo {...} inline   foo();
                    // storage-class specifier
                case Static:            // struct foo {...} static     x;
                case Extern:            // struct foo {...} extern     x;
                case Typedef:           // struct foo {...} typedef    x;
                case Register:          // struct foo {...} register   x;
                case Auto:              // struct foo {...} auto       x;
                {
                    // As shown above, type qualifiers and storage class specifiers absolutely
                    // can occur after class specifiers according to the grammar.  However,
                    // almost no one actually writes code like this.  IfStmt we see one of these,
                    // it is much more likely that someone missed a semi colon and the
                    // type/storage class specifier we're seeing is part of the *next*
                    // intended declaration, as in:
                    //
                    //   struct foo { ... }
                    //   typedef int X;
                    //
                    // We'd really like to emit a missing semicolon error instead of emitting
                    // an error on the 'int' saying that you can't have two type specifiers in
                    // the same declaration of X.  Because of this, we lookup ahead past this
                    // token to see if it's a type specifier.  IfStmt so, we know the code is
                    // otherwise invalid, so we can produce the expected semi error.
                    if (!isKnownBeTypeSpecifier(nextToken()))
                        expectedSemi = false;
                    break;
                }

                // struct bar { struct foo {...} }
                case r_brace:
                    // missing ';' at the end f struct is ccepted as an extension in C mode
                    expectedSemi = false;
                    break;
            }
            if (expectedSemi)
            {
                if (nextTokenIs(semi))
                {
                    consumeToken();
                }
                else
                {
                    diag(tok, err_expected_semi_after)
                            .addTaggedVal(tagType == TST_union ? "union":"struct")
                            .emit();
                    skipUntil(semi, true);
                }
            }
        }
    }

    /**
     * ReturnStmt true if we know that the specified token
     * is definitely a type-specifier.  ReturnStmt false if it isn't part of a type
     * specifier or if we're not sure.
     *
     * @param tok
     * @return
     */
    private boolean isKnownBeTypeSpecifier(Token tok)
    {
        switch (tok.getKind())
        {
            default:
                return false;
            case Short:
            case Long:
            case Signed:
            case Unsigned:
            case Complex:
            case Void:
            case Char:
            case Int:
            case Float:
            case Double:
            case Bool:

            case Struct:
            case Union:
            case Enum:
                return true;
        }
    }

    /**
     * ParseStructUnionBody
     * struct-contents:
     *   struct-declaration-list
     *
     * struct-declaration-list:
     *   struct-declaration struct-declaration-list
     *
     * struct-declaration:
     *  specifier-qualifier-list struct-declarator-list
     *
     * struct-declarator-list:
     *  struct-declarator
     *  struct-declarator, struct-declarator-list
     *
     * struct-declarator:
     *  declarator
     *  declarator[opt] : constant-expression
     *
     *
     * @param recordLoc
     * @param tagType
     */
    private void parseStructOrUnionBody(
            SourceLocation recordLoc,
            DeclSpec.TST tagType,
            Decl tagDecl)
    {
        // expect a '{'.
        SourceLocation lBraceLoc = consumeBrace();

        // obtains the struct or union specifier getIdentifier.
        String structOrUnion = DeclSpec.getSpecifierName(tagType);

        ParseScope structScope = new ParseScope(this,
        ScopeFlags.ClassScope.value
                | DeclScope.value);
        action.actOnTagStartDefinition(getCurScope(), tagDecl);

        // Empty structs are an extension in C (C99 6.7.2.1p7)
        if (nextTokenIs(r_brace))
        {
            consumeToken();
            diag(lBraceLoc, ext_empty_struct_union_enum)
                    .addTaggedVal(structOrUnion)
                    .emit();
            skipUntil(semi, true);
        }

        // an array stores all fields declared in current struct/union body.
        ArrayList<Decl> fieldDecls = new ArrayList<>(32);

        // While we still have something to read until '}' or 'eof' encounter.
        while (nextTokenIsNot(r_brace) && nextTokenIsNot(eof))
        {
            // each iteration of this loop reads one struct-declaration.
            if (nextTokenIs(semi))
            {
                diag(tok, ext_extra_struct_semi).addTaggedVal(DeclSpec
                .getSpecifierName(tagType))
                .addFixItHint(FixItHint.createRemoval
                        (new SourceRange(tok.getLocation()))).emit();
                consumeToken();
                continue;
            }

            // parse all the comma separated declarators.
            DeclSpec ds = new DeclSpec();

            FieldCallBack callBack = new FieldCallBack(this, tagDecl,
                    fieldDecls);
            parseStructDeclaration(ds, callBack);

            if (nextTokenIs(semi))
                consumeToken();
            else if (nextTokenIs(r_brace))
            {
                expectAndConsume(semi, ext_expected_semi_decl_list);
                break;
            }
            else
            {
                expectAndConsume(semi, err_expected_semi_decl_list);
                skipUntil(r_brace, true);
                // if we stopped at a ';', consume it.
                if (nextTokenIs(semi))
                    consumeToken();
            }
        }

        SourceLocation rBraceLoc = consumeBrace();

        AttributeList attr = null;
        // if attribtue exist after struct contents, parse them.
        if (tok.is(__Attribute))
        {
            attr = parseAttributes().first;
        }

        // eat '}'
        action.actOnFields(getCurScope(),
                recordLoc,
                tagDecl,
                fieldDecls,
                lBraceLoc,
                rBraceLoc,
                attr);

        structScope.exit();
        action.actOnTagFinishDefinition(getCurScope(),
                tagDecl,
                rBraceLoc);
    }

    /**
     * Parse a struct declaration without the terminating semicolon.
     * <pre>
     *       struct-declaration:
     *         specifier-qualifier-list struct-declarator-list
     * [GNU]   __extension__ struct-declaration
     * [GNU]   specifier-qualifier-list
     *       struct-declarator-list:
     *         struct-declarator
     *         struct-declarator-list ',' struct-declarator
     * [GNU]   struct-declarator-list ',' attributes[opt] struct-declarator
     *       struct-declarator:
     *         declarator
     * [GNU]   declarator attributes[opt]
     *         declarator[opt] ':' constant-expression
     * [GNU]   declarator[opt] ':' constant-expression attributes[opt]
     * </pre>
     *
     * @param ds
     * @param callBack
     */
    private void parseStructDeclaration(DeclSpec ds, FieldCallBack callBack)
    {
        if (tok.is(__Extension__))
        {
            // __extension__ sliences extension warnings in the supexpression.
            consumeToken();
            parseStructDeclaration(ds, callBack);
        }

        // parse common specifier-qualifier
        parseSpecifierQualifierList(ds);

        // If there are no declarators, this is a single standing declaration-
        // specifier.
        if (nextTokenIs(semi))
        {
            action.parsedFreeStandingDeclSpec(getCurScope(), ds);
            return;
        }
        // read struct-declarators until we find a ';'.
        while (true)
        {
            FieldDeclarator declaratorField = new FieldDeclarator(ds);

            // struct-declarator:
            //   declarator
            //   declarator[opt] : constant-expression
            if (tok.isNot(colon))
            {
                parseDeclarator(declaratorField.declarator);
            }

            // parse the constant-subExpr after declarator if there is a ':;
            if (tok.is(colon))
            {
                consumeToken();
                ActionResult<Expr> result = parseConstantExpression();
                if (result.isInvalid())
                    skipUntil(semi, true);
                else
                    declaratorField.bitFieldSize = result.get();
            }

            // if attribute exists after the declarator, parse them.
            if (tok.is(__Attribute))
            {
                Pair<AttributeList, SourceLocation> res = parseAttributes();
                declaratorField.declarator.addAttributes(res.first, res.second);
            }

            // we are done with this declarator; invoke the callback
            callBack.invoke(declaratorField);

            if (nextTokenIsNot(comma))
                return;

            // consume ','
            consumeToken();

            if (tok.is(__Attribute))
            {
                Pair<AttributeList, SourceLocation> res = parseAttributes();
                declaratorField.declarator.addAttributes(res.first, res.second);
            }
        }
    }

    /**
     * Parse and verify a newly-initialized declarator.
     * Possibly an abstract declarator
     * <p>
     * <p>
     * declarator:
     *   pointer[opt] direct-declarator
     * <p>
     * <p>
     * pointer:
     *   '*' type-qualifier-list[opt]
     *   '*' type-qualifier-list[opt] pointer
     * @param declarator
     */
    private void parseDeclarator(Declarator declarator)
    {
        //declarator.setRangeEnd(tok.getLocation());
        if (nextTokenIsNot(star))
        {
            // parse direct-declartor
            parseDirectDeclarator(declarator);
            return;
        }

        // Otherwise, '*'-> pointer.
        SourceLocation loc = consumeToken();
        declarator.setRangeEnd(loc);


        DeclSpec ds = new DeclSpec();
        parseTypeQualifierListOpt(ds);
        declarator.extendWithDeclSpec(ds);

        // recursively parse remained piece
        parseDeclarator(declarator);

        // remember this pointer type and add it into declarator's type list.
        declarator.addTypeInfo(DeclaratorChunk.getPointer
                (ds.getTypeQualifier(),
                        tok.getLocation(),
                        ds.getConstSpecLoc(),
                        ds.getVolatileSpecLoc(),
                        ds.getRestrictSpecLoc(),
                        ds.getAttributes()),
               ds.getRangeEnd());
    }

    /**
     * <p>
     * direct-declarator:
     *   identifier
     *   '(' declarator ')'
     *   direct-declarator array-declarator
     *   direct-declarator ( parameter-type-list )
     *   direct-declarator ( identifier-list[opt] )

     * type-qualifier-list:
     *   type-qualifier
     *   type-qualifier-list type-qualifier
     * <p>
     * parameter-type-list:
     *   parameter-list
     *   parameter-list , ...
     * <p>
     * parameter-list:
     *   parameter-declaration
     *   parameter-list , parameter-declaration
     * <p>
     * parameter-declaration:
     *   declaration-specifiers declarator
     *   declaration-specifiers abstract-declarator[opt]
     * <p>
     * identifier-list:
     *   identifier
     *   identifier-list , identifier
     * <p>
     * abstract-declarator:
     *   pointer
     *   pointer[opt] direct-abstract-declarator
     * <p>
     * direct-abstract-declarator:
     *   ( abstract-declarator )
     *   direct-abstract-declarator[opt] array-declarator
     *   direct-abstract-declarator[opt] ( parameter-type-list[opt] )
     * @param declarator
     */
    private void parseDirectDeclarator(Declarator declarator)
    {
        if (nextTokenIs(identifier) && declarator.mayHaveIdentifier())
        {
            /**
             The direct declarator must start with an identifier (possibly
             omitted) or a parenthesized declarator (possibly abstract).  In
             an ordinary declarator, initial parentheses must start a
             parenthesized declarator.  In an abstract declarator or parameter
             declarator, they could start a parenthesized declarator or a
             parameter list.
             */
            IdentifierInfo id = tok.getIdentifierInfo();
            Util.assertion(id != null,  "Not an identifier?");
            declarator.setIdentifier(id, tok.getLocation());
            consumeToken();
        }
        else if (nextTokenIs(l_paren))
        {
            // direct-declarator:
            //   '(' declarator ')'
            // e.g. char (*X) or int (*XX)(void)
            parseParenDeclarator(declarator);
        }
        else if (declarator.mayOmitIdentifier())
        {
            // This could be something simple like "int" (in which case the declarator
            // portion is empty), if an abstract-declarator is allowed.
            declarator.setIdentifier(null, tok.getLocation());
        }
        else
        {
            if (declarator.getContext() == TheContext.StructFieldContext)
                diag(tok, err_expected_member_name_or_semi)
                        .addSourceRange(declarator.getDeclSpec()
                        .getSourceRange())
                        .emit();
            else
                diag(tok, err_expected_ident_lparen).emit();

            declarator.setIdentifier(null, tok.getLocation());
            declarator.setInvalidType(true);
        }

        Util.assertion(declarator.isPastIdentifier(), "Haven't past the location of the identifier yet?");


        while (true)
        {
            if (nextTokenIs(l_paren))
                parseFunctionDeclarator(consumeParen(), declarator, false);
            else if (nextTokenIs(l_bracket))
                parseBracketDeclarator(declarator);
            else
                break;
        }
    }

    private boolean isDeclarationSpecifier(boolean disambiguatingWithExpression)
    {
        switch (tok.getKind())
        {
            default:return false;
            case identifier:
                return isDeclarationSpecifier();
            case Typedef:
            case Extern:
            case Static:
            case Auto:
            case Register:

            case Short:
            case Long:
            case Signed:
            case Unsigned:
            case Complex:
            case Void:
            case Char:
            case Int:
            case Float:
            case Double:
            case Bool:

            case Struct:
            case Union:
            case Enum:

            case Const:
            case Volatile:
            case Restrict:

            case Inline:
                return true;

            // typedefs-getIdentifier
            //case ANN_TYPENAME:
              //  return !disambiguatingWithExpression;
        }
    }

    private boolean isDeclarationSpecifier()
    {
        switch (tok.getKind())
        {
            default: return false;
            case Typedef:
            case Extern:
            case Static:
            case Auto:
            case Register:

            case Short:
            case Long:
            case Signed:
            case Unsigned:
            case Complex:
            case Void:
            case Char:
            case Int:
            case Float:
            case Double:
            case Bool:

            case Struct:
            case Union:
            case Enum:

            case Const:
            case Volatile:
            case Restrict:

            case Inline:
                return true;

            case identifier:
            {
                // Parse 'typedef name' or 'typename'.
                // So, the current token is a typedef identifier or error.
                QualType type = action.getTypeByName(
                        tok.getIdentifierInfo(),
                        tok.getLocation(),
                        getCurScope());
                return type != null;
            }
        }
    }

    /**
     * direct-declarator:
     *   '(' declarator ')'
     * [GNU]   '(' attributes declarator ')'
     *   direct-declarator '(' parameter-type-list ')'
     *   direct-declarator '(' identifier-list[opt] ')'
     * [GNU]   direct-declarator '(' parameter-forward-declarations
     *                    parameter-type-list[opt] ')'
     * @param declarator
     */
    private void parseParenDeclarator(Declarator declarator)
    {
        // eat the '('.
        Util.assertion( nextTokenIs(l_paren));
        SourceLocation lparenLoc = consumeParen();

        Util.assertion(!declarator.isPastIdentifier(), "Should be called before passing identifier");


        // Eat any attributes before we look at whether this is a grouping or function
        // declarator paren.  If this is a grouping paren, the attribute applies to
        // the type being built up, for example:
        //     int (__attribute__(()) *x)(long y)
        // If this ends up not being a grouping paren, the attribute applies to the
        // first argument, for example:
        //     int (__attribute__(()) int x)
        // In either case, we need to eat any attributes to be able to determine what
        // sort of paren this is.
        AttributeList attr = null;
        boolean requireArg = false;
        if (tok.is(__Attribute))
        {
            attr = parseAttributes().first;
            // We require that the argument list (if this is a non-grouping paren) be
            // present even if the attribute list was empty.
            requireArg = true;
        }

        boolean isGrouping;

        // If we haven't past the identifier yet (or where the identifier would be
        // stored, if this is an abstract declarator), then this is probably just
        // grouping parens. However, if this could be an abstract-declarator, then
        // this could also be the start of function arguments (consider 'void()').
        if (!declarator.mayOmitIdentifier())
        {
            // If this can't be an abstract-declarator, this *must* be a grouping
            // paren, because we haven't seen the identifier yet.
            isGrouping = true;
        }
        else if (nextTokenIs(r_paren) || isDeclarationSpecifier())
        {
            // 'int()' is a function.
            // *(int arga, void (*argb)(double Y))
            // This is a type-ident.
            isGrouping = false;
        }
        else
        {
            isGrouping = true;
        }

        // If this is a grouping paren, handle:
        // direct-declarator: '(' declarator ')'
        // direct-declarator: '(' attributes declarator ')'
        if (isGrouping)
        {
            boolean hadGroupingParens = declarator.hasGroupingParens();
            declarator.setGroupingParens(true);

            if (attr != null)
                declarator.addAttributes(attr, new SourceLocation());

            parseDeclarator(declarator);

            // match ')'
            SourceLocation loc = matchRHSPunctuation(r_paren, lparenLoc);
            declarator.setGroupingParens(hadGroupingParens);
            declarator.setRangeEnd(loc);
            return;
        }

        // Okay, if this wasn't a grouping paren, it must be the start of a function
        // argument list.  Recognize that this declarator will never have an
        // identifier (and remember where it would have been), then call into
        // ParseFunctionDeclarator to handle of argument list.
        declarator.setIdentifier(null, tok.getLocation());
        parseFunctionDeclarator(lparenLoc, declarator, requireArg);
    }

	/**
     * This method also handles this portion of the grammar:
     * <pre>
     *       parameter-type-list: [C99 6.7.5]
     *         parameter-list
     *         parameter-list ',' '...'
     *
     *       parameter-list: [C99 6.7.5]
     *         parameter-declaration
     *         parameter-list ',' parameter-declaration
     *
     *       parameter-declaration: [C99 6.7.5]
     *         declaration-specifiers declarator
     * [GNU]   declaration-specifiers declarator attributes
     *         declaration-specifiers abstract-declarator[opt]
     * [GNU]   declaration-specifiers abstract-declarator[opt] attributes
     * </pre>
     * @param declarator
     */
    private void parseFunctionDeclarator(
            SourceLocation lparenLoc,
            Declarator declarator,
            boolean requireArg)
    {
        Util.assertion(declarator.isPastIdentifier(), "Should not call before identifier!");
        // this should be true when the function has typed arguments.
        // Otherwise, it will be treated as K&R style function.
        boolean hasProto = false;

        // This parameter list may be empty.
        SourceLocation rparenLoc;
        SourceLocation endLoc;
        if (nextTokenIs(r_paren))
        {
            if (requireArg)
                diag(tok, err_argument_required_after_attribute).emit();

            rparenLoc = consumeParen();
            endLoc = rparenLoc;

            // Remember that we parsed a function type.
            declarator.addTypeInfo(DeclaratorChunk.getFunction(
                hasProto, false, SourceLocation.NOPOS,
                null, 0, lparenLoc, rparenLoc), endLoc);

            return;
        }

        // Alternatively, this parameter list may be an identifier list form for a
        // K&R-style function:  void foo(a,b,c)
        if (isFunctionDeclaratorIdentifierList())
        {
            // Parses the function declaration of K&R form.
            // Identifier list.  Note that '(' identifier-list ')' is only allowed for
            // normal declarators, not for abstract-declarators.
            parseFunctionDeclaratorIdentifierList(lparenLoc, declarator);
            return;
        }

        // Finally, a normal, non-empty parameter type list.

        // build an array of information about the parsed arguments.
        ArrayList<ParamInfo> paramInfos = new ArrayList<>(8);
        // The the location where we see an ellipsis.
        SourceLocation ellipsisLoc = SourceLocation.NOPOS;
        DeclSpec ds = new DeclSpec();

        // enter function-declaration scope,
        ParseScope protoTypeScope = new ParseScope(this,
                FunctionProtoTypeScope.value
                | DeclScope.value);
        if (nextTokenIsNot(r_paren))
        {
            ellipsisLoc = parseParameterDeclarationClause(declarator, paramInfos);
        }
        else if (requireArg)
        {
            diag(tok, err_argument_required_after_attribute).emit();
        }

        hasProto = !paramInfos.isEmpty();

        // if we have the closing ')', eat it.
        rparenLoc = endLoc = matchRHSPunctuation(r_paren, lparenLoc);

        // leave prototype scope
        protoTypeScope.exit();

        declarator.addTypeInfo(DeclaratorChunk.getFunction(hasProto,
                ellipsisLoc != SourceLocation.NOPOS,
                ellipsisLoc,
                paramInfos,
                ds.getTypeQualifier(),
                lparenLoc,
                rparenLoc), endLoc);

    }

    private boolean isFunctionDeclaratorIdentifierList()
    {
        return nextTokenIs(identifier) && (tokenIs(nextToken(), comma)
                || tokenIs(nextToken(), r_paren));
    }

	/**
     * <pre>
     * identifier-list: [C99 6.7.5]
     *      identifier
     *      identifier-list ',' identifier
     * </pre>
     * @param lparenLoc
     * @param declarator
     */
    private void parseFunctionDeclaratorIdentifierList(
            SourceLocation lparenLoc,
            Declarator declarator)
    {
        HashSet<IdentifierInfo> paramsSoFar = new HashSet<>(8);
        ArrayList<ParamInfo> paramInfos = new ArrayList<>();

        // If there was no identifier specified for the declarator, either we are in
        // an abstract-declarator, or we are in a parameter declarator which was found
        // to be abstract.  In abstract-declarators, identifier lists are not valid:
        // diagnose this.
        if (declarator.getIdentifier() == null)
            diag(tok, ext_ident_list_in_param).emit();

        paramsSoFar.add(tok.getIdentifierInfo());
        paramInfos.add(new ParamInfo(tok.getIdentifierInfo(),
                tok.getLocation(),
                null));

        // Consume the first identifier.
        consumeToken();

        while (nextTokenIs(comma))
        {
            // Eat the comma.
            consumeToken();

            // if the next token is not a identifier,
            // report the error and skip it until ')'.
            if (nextTokenIsNot(identifier))
            {
                diag(tok, err_expected_ident).emit();
                skipUntil(r_paren, true);
                paramInfos.clear();
                return;
            }

            // reject typedef int y; int test(x, y), but continue parsing.
            IdentifierInfo paramII = tok.getIdentifierInfo();
            if (action.getTypeByName(paramII, tok.getLocation(), getCurScope()) != null)
            {
                diag(tok, err_unexpected_typedef_ident)
                        .addTaggedVal(paramII).emit();
            }

            // Verify that the argument identifier has not already been mentioned.
            if (!paramsSoFar.add(paramII))
            {
                diag(tok, err_param_redefinition).addTaggedVal(paramII).emit();
            }
            else
            {
                // Remember this identifier in ParamInfo.
                paramInfos.add(new ParamInfo(paramII, tok.getLocation(),
                        null));
            }

            // Eat the identifier.
            consumeToken();
        }
        // If we have the closing ')', eat it and we're done.
        SourceLocation rparenloc = matchRHSPunctuation(r_paren, lparenLoc);
        declarator.addTypeInfo(DeclaratorChunk.getFunction(
                false,
                false, SourceLocation.NOPOS,
                paramInfos, 0, lparenLoc, rparenloc),
                rparenloc);
    }

    /**
     * Parse a (possibly empty) parameter-list after the '('.
     * This function will not parse a K&R-style identifier list.
     * <p>
     * declarator is the instance of {@linkplain Declarator} being parsed.
     * After returning, paramInfos will hold the parsed parameters.
     * ellipsisLoc will be returned as result, if any was parsed.
     *
     * <pre>
     *     parameter-type-list:[C99 6.7.5]
     *      parameter-list
     *      parameter-list ',' '...'
     *
     *     parameter-list: [C99 6.7.5]
     *       parameter-declaration
     *       parameter-list, parameter-declaration
     *
     *     parameter-declaration: [C99 6.7.5]
     *       declaration-speicifers declarator
     * [GNU]   declaration-specifiers declarator attributes
     *       declaration-specifiers abstract-declarator[opt]
     * [GNU]   declaration-specifiers abstract-declarator[opt] attributes
     * </pre>
     *
     * @param declarator
     * @param paramInfos
     * @return
     */
    private SourceLocation parseParameterDeclarationClause(
            Declarator declarator,
            ArrayList<ParamInfo> paramInfos)
    {
        SourceLocation ellipsisLoc = SourceLocation.NOPOS;
        while(true)
        {
            if (nextTokenIs(ellipsis))
            {
                // consume the '...'
                ellipsisLoc = consumeToken();
                break;
            }

            // parse the declaration-specifiers
            // just use the parsingDeclaration "scope" of the declarator.
            DeclSpec ds = new DeclSpec();
            SourceLocation dsstartLoc = tok.getLocation();

            // parse the declaration specifiers
            parseDeclarationSpecifiers(ds);

            // Parse the declarator.
            // This is "FunctionProtoTypeContext".
            Declarator paramDecls = new Declarator(ds, TheContext.FunctionProtoTypeContext);
            parseDeclarator(paramDecls);

            // Parse GNU attributes, if present.
            if (tok.is(__Attribute))
            {
                Pair<AttributeList, SourceLocation> res = parseAttributes();
                paramDecls.addAttributes(res.first, res.second);
            }

            // remember this parsed parameter in ParamInfo.
            IdentifierInfo paramName = paramDecls.getIdentifier();

            // if no parameter specified, verify that "something" was specified,
            // otherwise we have a missing type and identifier.
            if (ds.isEmpty() && paramName == null
                    && paramDecls.getNumTypeObjects() == 0)
            {
                diag(dsstartLoc, err_missing_param).emit();
            }
            else
            {
                // Otherwise, we have see something. Add it to the ParamInfo
                // we are building and let semantic analysis try to verify it.

                // Inform the actions module about the parameter declarator,
                // so it gets added to the current scope.
                Decl param = action.actOnParamDeclarator(getCurScope(), paramDecls);

                // Here, we can not handle default argument inC mode.
                paramInfos.add(new ParamInfo(paramName,
                        paramDecls.getIdentifierLoc(), param));
            }

            // If the next token is ',', consume it and keep reading argument
            if (nextTokenIsNot(comma))
            {
                break;
            }
            // Consume the comma.
            consumeToken();
        }
        return ellipsisLoc;
    }

    /**
     * <pre>
     * [C90]   direct-declarator '[' constant-expression[opt] ']'
     * [C99]   direct-declarator '[' type-qual-list[opt] assignment-subExpr[opt] ']'
     * [C99]   direct-declarator '[' 'static' type-qual-list[opt] assign-subExpr ']'
     * [C99]   direct-declarator '[' type-qual-list 'static' assignment-subExpr ']'
     * [C99]   direct-declarator '[' type-qual-list[opt] '*' ']'
     * </pre>
     */
    private void parseBracketDeclarator(Declarator declarator)
    {
        SourceLocation lbracketLoc = consumeBracket();

        SourceLocation rbracketLoc = SourceLocation.NOPOS;

        // C array syntax has many features, but by-far the most common is [] and [4].
        // This code does a fast path to handle some of the most obvious cases.
        if (tok.is(r_bracket))
        {
            // eat the ']'.
            rbracketLoc = matchRHSPunctuation(r_bracket, lbracketLoc);

            // remember that we parsed the empty array declaration.
            declarator.addTypeInfo(
                    DeclaratorChunk.getArray(0, false, false, null,
                            lbracketLoc, rbracketLoc),
                    rbracketLoc);
            return;
        }
        else if (tok.is(numeric_constant)
                && getLookAheadToken(1).is(r_bracket))
        {
            // [4] is evey common. parse the number
            ActionResult<Expr> numOfSize = action.actOnNumericConstant(tok);

            // eat the ']'.
            consumeToken();
            rbracketLoc = matchRHSPunctuation(r_bracket, lbracketLoc);
            if (numOfSize.isInvalid())
                numOfSize.release();

            // remember that we parsed a array type
            declarator.addTypeInfo(DeclaratorChunk.getArray(0,
                            false, false,
                            numOfSize.get(),
                            lbracketLoc, rbracketLoc),
                    rbracketLoc);
            return;
        }

        // If valid, this location is the position where we read the 'static' keyword.
        SourceLocation staticLoc = SourceLocation.NOPOS;
        if (nextTokenIs(Static))
            staticLoc = consumeToken();

        // If there is a type-qualifier-list, read it now.
        // Type qualifiers in an array subscript are a C99 feature.
        DeclSpec ds = new DeclSpec();
        parseTypeQualifierListOpt(ds);

        // If we haven't already read 'static', check to see if there is one after the
        // type-qualifier-list.
        if (!staticLoc.isValid() && nextTokenIs(Static))
            staticLoc = consumeToken();

        // Handle "direct-declarator [ type-qual-list[opt] * ]".
        boolean isStar = false;
        ActionResult<Expr> numElements = new ActionResult<>();

        if (nextTokenIs(star) && tokenIs(nextToken(), r_bracket))
        {
            consumeToken();     // Eat the '*'.
            if (staticLoc.isValid())
            {
                diag(staticLoc, err_unspecified_vla_size_with_static).emit();
                staticLoc = SourceLocation.NOPOS;
            }
            isStar = true;
        }
        else if (nextTokenIsNot(r_bracket))
        {
            // Parse the constant-expression or assignment-expression now (depending
            // on dialect).
            numElements = parseAssignExpression();
        }
        // If there was an error parsing the assignment-expression, recovery.
        if (numElements.isInvalid())
        {
            declarator.setInvalidType(true);
            skipUntil(r_bracket, true);
            return;
        }

        SourceLocation endLoc = matchRHSPunctuation(r_bracket, lbracketLoc);

        // Remember that we parsed a array type, and remember its features.
        declarator.addTypeInfo(DeclaratorChunk.getArray(
                ds.getTypeQualifier(), staticLoc.isValid(),
                isStar, numElements.get(),
                lbracketLoc, rbracketLoc),
                rbracketLoc);
    }

    private void parseTypeQualifierListOpt(DeclSpec ds)
    {
        parseTypeQualifierListOpt(ds, true);
    }

    /**
     * ParseTypeQualifierListOpt
     * <pre>
     * type-qualifier-list: [C99 6.7.5]
     *   type-qualifier
     *       type-qualifier-list: [C99 6.7.5]
     *         type-qualifier
     * [GNU]   attributes                        [ only if AttributesAllowed=true ]
     *         type-qualifier-list type-qualifier
     * [GNU]   type-qualifier-list attributes    [ only if AttributesAllowed=true ]
     * </pre>
     * @param ds
     */
    private void parseTypeQualifierListOpt(DeclSpec ds,  boolean allowAttribute)
    {
        while(true)
        {
            boolean isInvalid = false;
            SourceLocation loc = tok.getLocation();
            switch (tok.getKind())
            {
                case Const:
                    isInvalid = ds.setTypeQualifier(TQ_const, loc);
                    break;
                case Volatile:
                    isInvalid = ds.setTypeQualifier(TQ_volatile, loc);
                    break;
                case Restrict:
                    isInvalid = ds.setTypeQualifier(TQ_restrict, loc);
                    break;
                case __Attribute:
                    if (allowAttribute)
                    {
                        ds.addAttributes(parseAttributes().first);
                        continue;
                    }
                    // otherwise,fall through
                default:
                    // if this is not a type-qualifier token, we are done read type
                    // qualifiers.
                    ds.finish(diags, pp);
                    return;
            }
            // if the specifier combination was not legal, issue a diagnostic.
            if (isInvalid)
            {
                // TODO: report error.
            }
            consumeToken();
        }
    }

    /**
     * specifier-qualifier-list:
     *   type-specifier specifier-qualifier-list[opt]
     *   type-qualifier specifier-qualifier-list[opt]
     * @param ds
     */
    private void parseSpecifierQualifierList(DeclSpec ds)
    {
        // specifier-qualifier-list is a subset of declaration-specifiers.
        // parse declaration-specifiers and complain about extra stuff.
        // valid it
        parseDeclarationSpecifiers(ds);

        // validate declspec for type-getIdentifier
        int specs = ds.getParsedSpecifiers();
        if (specs == ParsedSpecifiers.PQ_none)
        {
            diag(tok, err_typename_requires_specqual).emit();
        }
        // issue diagnostic and remove storage class if present
        if ((specs & ParsedSpecifiers.PQ_StorageClassSpecifier) != 0)
        {
            if (ds.getStorageClassSpecLoc().isValid())
                diag(ds.getStorageClassSpecLoc(), err_typename_invalid_storageclass)
                        .emit();

            ds.clearStorageClassSpec();
        }

        // issue diagnostic and remove function specifier if present
        if ((specs & ParsedSpecifiers.PQ_FunctionSpecifier) != 0)
        {
            if (ds.isInlineSpecifier())
                diag(ds.getInlineSpecLoc(), err_typename_invalid_functionspec)
                        .emit();
            ds.clearFunctionSpecifier();
        }
    }

    /**
     * Parse a binary expression that starts with lhs and has a precedence of at
     * least minPrec.
     * @param lhs
     * @param minPrec
     * @return
     */
    private ActionResult<Expr> parseRHSOfBinaryExpression(ActionResult<Expr> lhs,
            int minPrec)
    {
        int nextTokPrec = getBinOpPrecedence(tok.getKind());

        SourceLocation colonLoc = SourceLocation.NOPOS;
        while(true)
        {
            // if this token has a lower precedence than we are allowed to parse
            // then we are done.
            if (nextTokPrec < minPrec)
                return lhs;

            // consume the operator token, then advance the tokens stream.
            Token opToken = tok.clone();
            consumeToken();

            // Special case handling for the ternary (?...:...) operator.
            ActionResult<Expr> ternaryMiddle = new ActionResult<>(true);
            if (nextTokPrec == PrecedenceLevel.Conditional)
            {
                if(nextTokenIsNot(colon))
                {
                    // Handle this production specially
                    //   logical-OR-expression '?' expression ':' conditional-expression
                    // In particular, the RHS of the '?' is 'expression', not
                    // 'logical-OR-expression' as we might expect.
                    ternaryMiddle = parseExpression();
                    if (ternaryMiddle.isInvalid())
                    {
                        lhs = exprError();
                        ternaryMiddle = exprError();
                    }
                }
                else
                {
                    // Special cas handling of "X ? Y:Z" where Y is empty.
                    //   logical-OR-expression '?' ':' conditional-expression   [GNU]
                    ternaryMiddle = exprError();
                    diag(tok, ext_gnu_conditional_expr).emit();
                }

                if (nextTokenIs(colon))
                    colonLoc = consumeToken(); // eat the ':'.
                else
                {
                    SourceLocation filoc = tok.getLocation();
                    // We missing a ':' after ternary middle expression.
                    diag(tok, err_expected_colon)
                    .addFixItHint(FixItHint.
                            createInsertion(filoc, ": ")).emit();
                    diag(opToken, note_matching).addTaggedVal("?").emit();
                    colonLoc = tok.getLocation();
                }
            }

            // Parse another leaf here for the RHS of the operator.
            // ParseCastExpression works here because all RHS expressions in C
            // have it as a prefix, at least.
            ActionResult<Expr> rhs = parseCastExpression(false, false, false);

            if (rhs.isInvalid())
                return rhs;

            // Remember the precedence of this operator and get the precedence of the
            // operator immediately to the right of the RHS.
            int thisPrec = nextTokPrec;
            nextTokPrec = getBinOpPrecedence(tok.getKind());

            // Assignment and conditional expression are right-associative.
            boolean isRightAssoc = thisPrec == PrecedenceLevel.Conditional
                    || thisPrec == PrecedenceLevel.Assignment;

            if (nextTokPrec > thisPrec
                    || (thisPrec == nextTokPrec && isRightAssoc))
            {
                // If this is left-associative, only parse things on the RHS that bind
                // more tightly than the current operator.  If it is left-associative, it
                // is okay, to bind exactly as tightly.  For example, compile A=B=C=D as
                // A=(B=(C=D)), where each paren is a level of recursion here.
                // The function takes ownership of the RHS.
                rhs = parseRHSOfBinaryExpression(rhs, thisPrec + (isRightAssoc?0:1));

                if (rhs.isInvalid())
                    lhs = exprError();
                nextTokPrec = getBinOpPrecedence(tok.getKind());
            }

            Util.assertion((nextTokPrec <= thisPrec), "Recursive doesn't works!");

            if (!lhs.isInvalid())
            {
                // Combine the LHS and RHS into the LHS (e.g. builds AST)
                if (ternaryMiddle.isInvalid())
                {
                    lhs = action.actOnBinOp(opToken.getLocation(),
                            opToken.getKind(), lhs.get(), rhs.get());
                }
                else
                {
                    lhs = action.actOnConditionalOp(opToken.getLocation(), colonLoc,
                            lhs.get(), ternaryMiddle.get(), rhs.get());
                }
            }
        }
    }

    private ActionResult<Expr> parseCastExpression(
            boolean isUnaryExpression,
            boolean isAddressOfOperand,
            boolean isTypeCast)
    {
        OutRef<Boolean> x = new OutRef<>(false);
        ActionResult<Expr> res = parseCastExpression(isUnaryExpression,
                isAddressOfOperand, isTypeCast, x);
        if (x.get())
            diag(tok, err_expected_expression).emit();
        return res;
    }

    enum ParenParseOption
    {
        SimpleExpr,      // Only parse '(' expression ')'
        CompoundStmt,    // Also allow '(' compound-statement ')'
        CompoundLiteral, // Also allow '(' type-getIdentifier ')' '{' ... '}'
        CastExpr         // Also allow '(' type-getIdentifier ')' <anything>
    }

    /**
     * Parse a cast-expression, or, if isUnaryExpression is
     * true, parse a unary-expression. isAddressOfOperand exists because an
     * id-expression that is the operand of address-of gets special treatment
     * due to member pointers.
     * @param isUnaryExpression
     * @return A pair of both cast-expr and notCastExpr of type boolean.
     */
    private ActionResult<Expr>
        parseCastExpression(
            boolean isUnaryExpression,
            boolean isAddressOfOperand,
            boolean isTypeCast,
            OutRef<Boolean> notCastExpr)
    {
        ActionResult<Expr> res = null;
        Token nextTok = tok;
        TokenKind savedKind = nextTok.getKind();
        notCastExpr.set(false);

        switch (savedKind)
        {
            case l_paren:
            {
                QualType castTy = null;
                OutRef<QualType> out1 = new OutRef<>();
                SourceLocation rParenLoc = SourceLocation.NOPOS;
                OutRef<SourceLocation> out2 =
                        new OutRef<>(rParenLoc);

                ParenParseOption parenExprTppe =
                        isUnaryExpression ? CompoundLiteral:CastExpr;
                OutRef<ParenParseOption> out3 = new OutRef<>(parenExprTppe);

                res = parseParenExpression(out3, false, isTypeCast,
                        out1, out2);

                castTy = out1.get();
                rParenLoc = out2.get();
                parenExprTppe = out3.get();

                if (res.isInvalid())
                    return res;

                switch (parenExprTppe)
                {
                    // nothing to do.
                    case SimpleExpr: break;
                    // nothing to do.
                    case CompoundLiteral:
                        // We parsed '(' type-getIdentifier ')' '{' ... '}'.  If any suffixes of
                        // postfix-expression exist, parse them now.
                        break;
                    case CastExpr:
                        // We have parsed the cast-expression and no postfix-expr pieces are
                        // following.
                        return res;
                }
                break;
            }
            case numeric_constant:
                res = action.actOnNumericConstant(nextTok);
                consumeToken();
                break;
            case identifier:
            {
                IdentifierInfo id = tok.getIdentifierInfo();
                SourceLocation loc = consumeToken();

                // primary-expression: identifier
                if (isAddressOfOperand && isPostfixExpressionSuffixStart())
                    isAddressOfOperand = false;

                // FunctionProto designators are allowed to be undeclared (C99 6.5.1p2), so we
                // need to know whether or not this identifier is a function designator or
                // not.
                res = action.actOnIdentifierExpr(getCurScope(),
                        loc,
                        id,
                        tok.is(l_paren),
                        isAddressOfOperand);
                break;
            }
            case char_constant: 
                res = action.actOnCharacterConstant(nextTok);
                consumeToken();
                break;
            case string_literal:
                res = parseStringLiteralExpression();
                break;
            case plusplus:
            case subsub:
            {
                // unary-expression: '++' unary-expression [C99]
                // unary-expression: '--' unary-expression [C99]
                SourceLocation savedLoc = consumeToken();
                res = parseCastExpression(true, false, false);
                if (!res.isInvalid())
                    res = action.actOnUnaryOp(savedLoc, savedKind, res.get());
                return res;
            }
            case amp:
            case star:
            case plus:
            case sub:
            case tilde:
            case bar:
            case bang:
            case __Real__:  // unary-expression: '__real' cast-expresion [GNU]
            case __Imag:    // unary-expression: '__imag' cast-expresion [GNU]
            {
                SourceLocation savedLoc = consumeToken();
                res = parseCastExpression(false, false, false);
                if (!res.isInvalid())
                    res = action.actOnUnaryOp(savedLoc, savedKind, res.get());
                return res;
            }
            case __Extension__:
            {
                //unary-expression:'__extension__' cast-expr [GNU]
                SourceLocation savedLoc = consumeToken();
                res = parseCastExpression(false, false, false);
                if (!res.isInvalid())
                    res = action.actOnUnaryOp(savedLoc, savedKind, res.get());
                return res;
            }
            case Sizeof:
            case __Alignof:
                return parseSizeofAlignofExpression();
            case Char:
            case Bool:
            case Short:
            case Int:
            case Long:
            case Signed:
            case Unsigned:
            case Float:
            case Double:
            case Void:
            {
                diag(tok, err_expected_expression).emit();
                return exprError();
            }
            default:
                notCastExpr.set(true);
                return exprError();
        }

        // These can be followed by postfix-expr pieces.
        return parsePostfixExpressionSuffix(res);
    }

    /**
     * Returns true if the next token would start a postfix-expression
     * suffix.
     */
    private boolean isPostfixExpressionSuffixStart()
    {
        TokenKind kind = tok.getKind();
        return kind == l_bracket || kind == l_paren || kind == dot
                || kind == arrow || kind == plusplus || kind == subsub;
    }

    static class ParenExprArg
    {
        boolean isCastExpr;
        QualType castTy;
        SourceRange castRange;

        ParenExprArg(boolean isCast,
                QualType castTy,
                SourceRange castRange)
        {
            this.isCastExpr = isCast;
            this.castTy = castTy;
            this.castRange = castRange;
        }

        ParenExprArg()
        {
            isCastExpr = false;
            castTy = null;
            castRange = new SourceRange();
        }
    }

    /**
     * Parse a sizeof or alignof expression.
     * <pre>
     * unary-expression:  [C99 6.5.3]
     *         'sizeof' unary-expression
     *         'sizeof' '(' type-getIdentifier ')'
     * [GNU]   '__alignof' unary-expression
     * [GNU]   '__alignof' '(' type-ident ')'
     * </pre>
     * @return
     */
    private ActionResult<Expr> parseSizeofAlignofExpression()
    {
        Util.assertion(nextTokenIs(Sizeof), "Not a sizeof expression!");
        Token opTok = tok.clone();
        SourceLocation opLoc = consumeToken();

        ParenExprArg arg = new ParenExprArg();
        ActionResult<Expr> operand = parseExprAfterSizeof(opTok, arg);;

        // The sizeof type.
        if (arg.isCastExpr)
        {
            // 'sizeof' '(' type ')'
            return action.actOnSizeofExpr(opLoc,
                    arg.castTy,
                    arg.castRange);
        }

        // Reaching here, it must be sizeof expression.
        if (!operand.isInvalid())
        {
            operand = action.actOnSizeofExpr(
                    opLoc,
                    true,
                    operand.get(),
                    arg.castRange);
        }

        return operand;
    }

    /**
     * Parse a sizeof or alignof expression.
     * <pre>
     * unary-expression: [C99 6.5.3]
     *       'sizeof' unary-expression
     *       'sizeof' '(' type-ident ')'
     * [GNU] '__alignof' unary-expression
     * [GNU] '__alignof' '(' type-ident ')'
     * </pre>
     * @param opTok
     * @param arg
     * @return
     */
    private ActionResult<Expr> parseExprAfterSizeof(
            Token opTok,
            ParenExprArg arg)
    {
        Util.assertion(opTok.is(TokenKind.Sizeof), "Not a sizeof/alignof expression!");

        ActionResult<Expr> operand;
        // If this operand doesn't start with '(',it must be an expression.
        if (tok.isNot(l_paren))
        {
            arg.isCastExpr = false;

            // The GNU typeof and alignof extensions also behave as unevaluated
            // operands.
            operand = parseCastExpression(true, false, false);
        }
        else
        {
            // If it starts with a '(', we know that it is either a parenthesized
            // type-ident, or it is a unary-expression that starts with a compound
            // literal, or starts with a primary-expression that is a parenthesized
            // expression.
            OutRef<ParenParseOption> exprType =
                    new OutRef<>(CastExpr);
            OutRef<SourceLocation> lparenLoc =
                    new OutRef<>(opTok.getLocation());
            OutRef<SourceLocation> rParenLoc =
                    new OutRef<>(new SourceLocation());
            OutRef<QualType> castTy =
                    new OutRef<>(arg.castTy);
            operand = parseParenExpression(exprType, true, false,castTy, rParenLoc);

            arg.castRange = new SourceRange(lparenLoc.get(), rParenLoc.get());
            arg.castTy = castTy.get();

            // If ParseParenExpression parsed a '(typename)' sequence only, then this is
            // a type.
            if (exprType.get() == CastExpr)
            {
                arg.isCastExpr = true;
                return ActionResult.empty();
            }

            // If this is a parenthesized expression, it is the start of a
            // unary-expression, but doesn't include any postfix pieces.  Parse these
            // now if present.
            operand = parsePostfixExpressionSuffix(operand);
        }
        // If we get here, the operand to the typeof/sizeof/alignof was an expresion.
        arg.isCastExpr = false;
        return operand;
    }

    /**
     * Once the leading part of a postfix-expression
     * is parsed, this method parses any suffixes that apply.
     * <pre>
     * postfix-expression: [C99 6.5.2]
     *   primary-expression
     *   postfix-expression '[' expression ']'
     *   postfix-expression '[' braced-init-list ']'
     *   postfix-expression '(' argument-expression-list[opt] ')'
     *   postfix-expression '.' identifier
     *   postfix-expression '->' identifier
     *   postfix-expression '++'
     *   postfix-expression '--'
     *   '(' type-getIdentifier ')' '{' initializer-list '}'
     *   '(' type-getIdentifier ')' '{' initializer-list ',' '}'
     *
     * argument-expression-list: [C99 6.5.2]
     *   argument-expression
     *   argument-expression-list ',' assignment-expression
     * </pre>
     * @param lhs
     * @return
     */
    private  ActionResult<Expr> parsePostfixExpressionSuffix(ActionResult<Expr> lhs)
    {
        SourceLocation loc;
        while(true)
        {
            switch (tok.getKind())
            {
                case identifier:
                    // fall through; this is a primary expression.
                default:
                    return lhs;
                case l_bracket:
                {
                    // postfix-expression: p-e '[' expression ']'
                    BalancedDelimiterTracker delim = new BalancedDelimiterTracker(this, l_bracket);

                    // match the '['.
                    delim.consumeOpen();
                    SourceLocation lBracketLoc = delim.getCloseLocation();
                    ActionResult<Expr> idx = parseExpression();

                    if (lhs.isInvalid() || idx.isInvalid() || !tok.is(r_bracket))
                    {
                        lhs = exprError();
                        break;
                    }
                    // match the ']'.
                    delim.consumeClose();
                    SourceLocation rBracketLoc = delim.getCloseLocation();
                    lhs = action.actOnArraySubscriptExpr(lhs.get(), lBracketLoc,
                            idx.get(), rBracketLoc);

                    break;
                }
                case l_paren:
                {
                    // p-e: p-e '(' argument-expression-list[opt] ')'
                    BalancedDelimiterTracker delim = new BalancedDelimiterTracker(this, l_paren);
                    delim.consumeOpen();
                    loc = delim.getOpenLocation();
                    ArrayList<Expr> exprs = new ArrayList<>();
                    ArrayList<SourceLocation> commaLocs = new ArrayList<>();

                    if (!lhs.isInvalid())
                    {
                        if (nextTokenIsNot(r_paren))
                            if (parseExpressionList(exprs, commaLocs))
                                lhs = exprError();
                    }

                    if (lhs.isInvalid())
                        skipUntil(r_paren, true);
                    else if (nextTokenIsNot(r_paren))
                        lhs = exprError();
                    else
                    {
                        Util.assertion(exprs.isEmpty()                                || exprs.size() == commaLocs.size() + 1, "Unexpected number of commas!");


                        lhs = action.actOnCallExpr(lhs.get(), loc, exprs, tok.getLocation());
                        // eat the ')'.
                        delim.consumeClose();
                    }
                    break;
                }
                case arrow: 
                case dot:
                {
                    // postfix-expression: p-e '->' id-expression
                    // postfix-expression: p-e '.' id-expression
                    TokenKind opKind = tok.getKind();
                    consumeToken();
                    IdentifierInfo ii = tok.getIdentifierInfo();
                    SourceLocation opLoc = tok.getLocation();

                    // Consume the member name.
                    consumeToken();

                    if (!lhs.isInvalid())
                        lhs = action.actOnMemberAccessExpr(getCurScope(),lhs.get(), opLoc, opKind, ii);
                    break;
                }
                case plusplus:
                case subsub:
                {
                    if (!lhs.isInvalid())
                    {
                        lhs = action.actOnPostfixUnaryOp(tok.getLocation(), tok.getKind(), lhs.get());
                    }
                    consumeToken();
                    break;
                }
            }
        }
    }

    /**
     * This method handles string form literals.
     *
     * <pre>
     * primary-expression: [C99 6.5.1]
     *  string-literal
     * </pre>
     * @return
     */
    private ActionResult<Expr> parseStringLiteralExpression()
    {
        Util.assertion(nextTokenIs(string_literal),  "Not a string literal!");

        ArrayList<Token> stringToks = new ArrayList<>();
        do
        {
            stringToks.add(tok);
            consumeStringToken();
        }while (nextTokenIs(string_literal));

        return action.actOnStringLiteral(stringToks);
    }

    /**
     * Returns the precedence of specified operator token.
     * @param kind
     * @return
     */
    private int getBinOpPrecedence(TokenKind kind)
    {
        switch (kind)
        {
            default: return PrecedenceLevel.Unkonw;
            case greater:
                return PrecedenceLevel.Relational;
            case greatergreater:
                return PrecedenceLevel.Shift;
            case comma:
                return PrecedenceLevel.Comma;
            case equal: 
            case starequal:
            case slashequal:
            case percentequal:
            case plusequal:
            case subequal:
            case lesslessequal:
            case greatergreaterequal:
            case ampequal:
            case caretequal:
            case barequal:
                return PrecedenceLevel.Assignment;
            case question:
                return PrecedenceLevel.Conditional;
            case barbar:
                return PrecedenceLevel.LogicalOr;
            case ampamp:
                return PrecedenceLevel.LogicalAnd;
            case bar:
                return PrecedenceLevel.InclusiveOr;
            case caret:
                return PrecedenceLevel.ExclusiveOr;
            case amp:
                return PrecedenceLevel.And;
            case equalequal:
            case bangequal:
                return PrecedenceLevel.Equality;
            case lessequal:
            case less:
            case greaterequal:
                return PrecedenceLevel.Relational;
            case lessless:
                return PrecedenceLevel.Shift;
            case plus:
            case sub:
                return PrecedenceLevel.Additive;
            case percent:
            case star:
            case slash:
                return PrecedenceLevel.Multiplicative;
            case dot:
            case arrow:
                return PrecedenceLevel.PointerToMember;

        }
    }


    /**
     * Parses constant expression.
     * <pre>
     * constant-expression:
     *   conditional-expression
     * </pre>
     * Constant expression shall not contain assignment, increment, decrement,
     * function-call, or comma operators, except when they are contained within
     * a subexpression that is not evaluated.
     * @return
     */
    private ActionResult<Expr> parseConstantExpression()
    {
        ActionResult<Expr> lhs = parseCastExpression(false, false, false);

        //   An expression is potentially evaluated unless it appears where an
        //   integral constant expression is required (see 5.19) [...].
        ActionResult<Expr> res = parseRHSOfBinaryExpression(lhs, PrecedenceLevel.Conditional);
        return res;
    }

    private ActionResult<Expr> parseAssignExpression()
    {
        ActionResult<Expr> lhs = parseCastExpression(false, false, false);
        if (lhs.isInvalid())
            return lhs;

        return parseRHSOfBinaryExpression(lhs, PrecedenceLevel.Assignment);
    }

    /**
     * Read tokens until we get to the specified token, then consume it.
     * Because we cannot guarantee that the toke will ever occur, this skips to
     * the next token, or to some likely good stopping point.
     * IfStmt {@code stopAtSemi} is true, skipping will stop at a ';' character.
     *
     * @param kind       
     * @param stopAtSemi 
     */
    public boolean skipUntil(TokenKind kind, boolean stopAtSemi)
    {
        return skipUntil(new TokenKind[] { kind }, stopAtSemi, false);
    }

    private boolean skipUntil(TokenKind kind, boolean stopAtSemi, boolean dontConsume)
    {
        return skipUntil(new TokenKind[] { kind }, stopAtSemi, dontConsume);
    }

    private boolean skipUntil(TokenKind tag1, TokenKind tag2, boolean stopAtSemi)
    {
        return skipUntil(new TokenKind[] { tag1, tag2 }, stopAtSemi, false);
    }

    private boolean skipUntil(TokenKind[] tags, boolean stopAtSemi, boolean dontConsume)
    {
        boolean isFirstTokenSkipped = true;
        while (true)
        {
            for (TokenKind kind : tags)
            {
                if (tok.is(kind))
                {
                    if (dontConsume)
                    {
                        // Noop, don't consume the token;
                    }
                    else
                    {
                        consumeAnyToken();
                    }
                    return true;
                }
            }
            switch (tok.getKind())
            {
                case eof:
                    // ran out of tokens
                    return false;
                case l_paren:
                    consumeAnyToken();
                    skipUntil(r_paren, false);
                    break;
                case l_bracket:
                    consumeAnyToken();
                    skipUntil(r_bracket, false);
                    break;
                case l_brace:
                    consumeAnyToken();
                    skipUntil(r_brace, false);
                    break;

                case r_paren:
                case r_bracket:
                case r_brace:
                    if (!isFirstTokenSkipped)
                        return false;
                    consumeAnyToken();
                    break;
                case string_literal:
                    consumeAnyToken();
                    break;
                case semi:
                    if (stopAtSemi)
                        return false;
                    // fall through
                default:
                    consumeToken();
                    break;
            }
            isFirstTokenSkipped = false;
        }
    }

    private boolean isTokenParen()
    {
        return tok.is(l_paren) || tok.is(r_paren);
    }

    private boolean isTokenBracket()
    {
        return tok.is(l_bracket) || tok.is(r_bracket);
    }

    private boolean isTokenBrace()
    {
        return tok.is(l_brace) || tok.is(r_brace);
    }

    private boolean isTokenStringLiteral()
    {
        return tok.is(string_literal);
    }

    /**
     * Consume the next token (it must is StringToken, ParenToken,
     * BraceToken, BracketToken) from Scanner.
     * @return  return the location of just consumed token.
     */
    private SourceLocation consumeToken()
    {
        Util.assertion(!isTokenStringLiteral() && !isTokenParen() && !isTokenBracket() &&                !isTokenBrace(),  "Should consume special tokens with Consume*Token");

        prevTokLocation = tok.getLocation();
        tok = new Token();
        pp.lex(tok);
        return prevTokLocation;
    }

    public SourceLocation consumeAnyToken()
    {
        if (isTokenParen())
            return consumeParen();
        else if (isTokenBrace())
            return consumeBrace();
        else if (isTokenBracket())
            return consumeBracket();
        else if (isTokenStringLiteral())
            return consumeStringToken();
        else
            return consumeToken();

    }
    private int parenCount;
    private SourceLocation consumeParen()
    {
        Util.assertion(isTokenParen(),  "wrong consume method");
        if (tok.getKind() == l_paren)
            ++parenCount;
        else if (parenCount != 0)
            --parenCount;       // Don't let unbalanced )'s drive the count negative.
        prevTokLocation = tok.getLocation();
        tok = new Token();
        pp.lex(tok);
        return prevTokLocation;
    }
    private int bracketCount;

    private SourceLocation consumeBracket()
    {
        Util.assertion(isTokenBracket(),  "wrong consume method");
        if (tok.getKind() == l_bracket)
            ++bracketCount;
        else if (bracketCount != 0)
            --bracketCount;     // Don't let unbalanced ]'s drive the count negative.

        prevTokLocation = tok.getLocation();
        tok = new Token();
        pp.lex(tok);
        return prevTokLocation;
    }

    private SourceLocation consumeStringToken()
    {
        Util.assertion(isTokenStringLiteral(),                 "Should only consume string literals with this method");

        prevTokLocation = tok.getLocation();
        tok = new Token();
        pp.lex(tok);
        return prevTokLocation;
    }

    private SourceLocation consumeBrace()
    {
        Util.assertion(nextTokenIs(r_brace) || nextTokenIs(l_brace), "Wrong consume method");

        
        if (tok.getKind() == l_brace)
            ++braceCount;
        else
            --braceCount;

        prevTokLocation = tok.getLocation();
        tok = new Token();
        pp.lex(tok);
        return prevTokLocation;
    }

    private boolean expectAndConsume(TokenKind expectedTok, int diagID)
    {
        return expectAndConsume(expectedTok, diagID, "", Unknown);
    }

    public boolean expectAndConsume(TokenKind expectedTok, int diagID,
            String msg, TokenKind skipToTok)
    {
        if (tok.getKind() == expectedTok)
        {
            consumeAnyToken();
            return false;
        }

        SourceLocation endLoc = pp.getLocEndOfToken(prevTokLocation);
        String spelling = Token.getTokenSimpleSpelling(expectedTok);
        if (endLoc.isValid() && spelling != null)
        {
            diag(endLoc, diagID)
                    .addTaggedVal(msg)
                    .addFixItHint(FixItHint.createInsertion(endLoc, spelling))
                    .emit();
        }
        else
        {
            diag(tok, diagID).emit();
        }
        if (skipToTok != Unknown)
            skipUntil(skipToTok, true);
        return true;
    }

    private SourceLocation matchRHSPunctuation(TokenKind rhsTok,
            SourceLocation lhsLoc)
    {
        if (tok.getKind() == rhsTok)
            return consumeAnyToken();

        SourceLocation r = tok.getLocation();
        String lhsName = "unknown";
        int did = err_parse_error;
        switch (rhsTok)
        {
            default:break;
            case r_paren:
                lhsName = "(";
                did = err_expected_rparen;
                break;
            case r_brace:
                lhsName = "{";
                did = err_expected_rbrace;
                break;
            case r_bracket:
                lhsName = "[";
                did = err_expected_rsquare;
                break;
        }

        diag(tok, did).emit();
        diag(lhsLoc, note_matching).addTaggedVal(lhsName).emit();
        skipUntil(rhsTok, true);
        return r;
    }

    /**
     * This peeks ahead n tokens and returns that token without
     * consuming any tokens. lookAhead(0) return 'tok', lookAhead(1)
     * returns the token after tok, etc.
     *
     * Note that this differs from the Preprocessor's lookAhead method,
     * because the Parser always has on token lexed that the preprocessor doesn't.
     * @param n
     * @return
     */
    private Token getLookAheadToken(int n)
    {
        if (n == 0 || tok.is(eof)) return tok;
        return pp.lookAhead(n-1);
    }
    /**
     * Peeks ahead token and returns that token without consuming any token.
     *
     * @return
     */
    private Token nextToken()
    {
        return pp.lookAhead(0);
    }

    /**
     * Checks if the next token is expected.
     *
     * @param expectedToken A TargetData token to be compared with next Token.
     * @return
     */
    private boolean nextTokenIs(TokenKind expectedToken)
    {
        return tok.is(expectedToken);
    }

    private boolean tokenIs(Token token, TokenKind expectedToken)
    {
        return token.is(expectedToken);
    }

    /**
     * Returns true if the next token doesn't match expected token.
     *
     * @param expectedToken
     * @return
     */
    private boolean nextTokenIsNot(TokenKind expectedToken)
    {
        return tok.isNot(expectedToken);
    }
}
