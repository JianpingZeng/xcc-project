package jlang.cparser;

import jlang.ast.Tree;
import jlang.ast.Tree.CompoundStmt;
import jlang.ast.Tree.Expr;
import jlang.ast.Tree.InitListExpr;
import jlang.ast.Tree.Stmt;
import jlang.basic.LangOption;
import jlang.cparser.DeclSpec.DeclaratorChunk;
import jlang.cparser.DeclSpec.FieldDeclarator;
import jlang.cparser.DeclSpec.ParamInfo;
import jlang.cparser.DeclSpec.ParsedSpecifiers;
import jlang.cparser.Declarator.TheContext;
import jlang.cparser.Token.Ident;
import jlang.cpp.Preprocessor;
import jlang.cpp.SourceLocation;
import jlang.basic.SourceRange;
import jlang.diag.*;
import jlang.sema.Decl;
import jlang.sema.Decl.LabelDecl;
import jlang.sema.PrecedenceLevel;
import jlang.sema.Scope;
import jlang.sema.Scope.ScopeFlags;
import jlang.sema.Sema;
import jlang.type.QualType;
import tools.OutParamWrapper;

import java.util.ArrayList;
import java.util.HashSet;

import static java.util.Collections.emptyList;
import static jlang.cparser.DeclSpec.SCS.SCS_typedef;
import static jlang.cparser.DeclSpec.TQ.*;
import static jlang.cparser.DeclSpec.TSC.TSC_complex;
import static jlang.cparser.DeclSpec.TSS.TSS_signed;
import static jlang.cparser.DeclSpec.TSS.TSS_unsigned;
import static jlang.cparser.DeclSpec.TST.*;
import static jlang.cparser.DeclSpec.TSW.TSW_long;
import static jlang.cparser.DeclSpec.TSW.TSW_short;
import static jlang.cparser.Parser.ParenParseOption.*;
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
public class Parser implements Tag, DiagnosticParseTag, DiagnosticSemaTag, DiagnosticCommonKindsTag
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

    private void enterScope(int scopeFlags)
    {
        action.setCurScope(new Scope(getCurScope(), scopeFlags));
    }

    private void exitScope()
    {
        assert getCurScope() != null : "Scope imbalance.";

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
     * The scanner used for lexical analysis
     */
    private Scanner S;
    /**
     * The keywords table.
     */
    private Keywords keywords;

    private Sema action;
    private Preprocessor pp;
    /**
     * A diagnose for reporting error and warning information in appropriate style.
     */
    private Diagnostic diags;

    public Diagnostic getDiags()
    {
        return diags;
    }

    public Preprocessor getPP()
    {
        return pp;
    }

    private void init(Preprocessor pp, Sema action)
    {
        this.pp = pp;
        this.diags = pp.getDiagnostics();
        this.S = new Scanner(pp);
        this.action = action;
        keywords = Keywords.instance();
    }

    /**
     * Constructs a jlang.parser from a given scanner.
     */
    private Parser(Preprocessor pp, Sema action)
    {
        init(pp, action);
    }

    public static Parser instance(Preprocessor pp, Sema action)
    {
        return new Parser(pp, action);
    }

    LangOption getLangOption()
    {
        return pp.getLangOption();
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
        return diags.report(new FullSourceLoc(loc, pp.getInputFile()), diagID);
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
        assert getCurScope() == null;
        enterScope(ScopeFlags.DeclScope.value);
        action.actOnTranslationUnitScope(getCurScope());

        ArrayList<Decl> result = new ArrayList<>();
        // Parse them.
        while (!parseTopLevel(result));

        exitScope();
        assert getCurScope() == null : "Scope imbalance!";
    }

    public boolean parseTopLevel(ArrayList<Decl> result)
    {
        assert result != null;

        if (nextTokenIs(EOF))
        {
            diag(S.token, ext_empty_source_file);
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
        Token tok = S.token;
        SourceLocation pos = tok.loc;
        switch (tok.tag)
        {
            case SEMI:
            {
                diag(pos, ext_top_level_semi);
                consumeToken();
                return declGroups();
            }
            case RBRACE:
                diag(pos, err_expected_external_declaration);
                consumeToken();
                return declGroups();
            case EOF:
                diag(pos, err_expected_external_declaration);
                return declGroups();
            /**
             * TODO current, inline asm isn't supported.
            case ASM:
            {}
            */
            case TYPEDEF:
            {
                // A function definition can not start with those keyword.
                ArrayList<Stmt> stmts = new ArrayList<>();
                OutParamWrapper<SourceLocation> declEnd = new OutParamWrapper<>();
                return parseDeclaration(stmts, TheContext.FileContext, declEnd);
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
        if (nextTokenIs(SEMI))
        {
            consumeToken();
            Decl theDecl = action.parsedFreeStandingDeclSpec(getCurScope(), declSpecs);
            // This stmt in Clang just is used to inform people that a declaration
            // was parsed complete.
            // declSpecs.complete(theDecl);
            return action.convertDeclToDeclGroup(theDecl);
        }
        return parseDeclGroup(declSpecs, TheContext.FileContext, true);
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
        final DeclaratorChunk.FunctionTypeInfo fti =
                declarator.getFunctionTypeInfo();

        // If this is C90 and the declspecs were completely missing, fudge in an
        // implicit int.  We do this here because this is the only place where
        // declaration-specifiers are completely optional in the grammar.
        // FIXME

        // If this declaration was formed with a K&R-style identifier list for the
        // arguments, parse declarations for all of the args next.
        // int foo(a,b) int a; float b; {}
        if (fti.isKNRPrototype())
        {
            // TODO parseKNRParamDeclaration(declarator);
        }

        // We should have an opening brace.
        if (nextTokenIsNot(LBRACE))
        {
            diag(S.token, err_expected_fn_body);
            skipUntil(LBRACE, true);

            // if we didn't find a '{', bail out.
            if (nextTokenIsNot(LBRACE))
                return null;
        }

        ParseScope bodyScope = new ParseScope(this, ScopeFlags.FnScope.value
                | ScopeFlags.DeclScope.value);

        Decl res = action.actOnFunctionDef(getCurScope(), declarator);

        return parseFunctionStatementBody(res, bodyScope);
    }

    private Decl parseFunctionStatementBody(Decl funcDecl, ParseScope bodyScope)
    {
        assert nextTokenIs(LBRACE);

        SourceLocation lBraceLoc = S.token.getLocation();
        ActionResult<Stmt> body = parseCompoundStatementBody(lBraceLoc);

        if (body.isInvalid())
            body = action.actOnCompoundStmtBody(lBraceLoc, lBraceLoc, emptyList(), false);

        bodyScope.exit();
        return action.actOnFinishFunctionBody(funcDecl, body.get());
    }

    private ActionResult<Stmt> parseCompoundStatementBody(SourceLocation startLoc)
    {
        ArrayList<Stmt> stmts = new ArrayList<>();
        for (Token tok = S.token; !tokenIs(tok, RBRACE) && !tokenIs(tok, EOF);)
        {
            ActionResult<Stmt> res = parseStatementOrDeclaration(stmts, false);
            stmts.add(res.get());
        }
        Token tok = S.token;
        if (!tokenIs(tok, RBRACE))
        {
            diag(S.token, err_expected_rbrace);
            return new ActionResult<>();
        }
        SourceLocation rbraceLoc = consumeBrace();

        return new ActionResult<>(new CompoundStmt(stmts, startLoc, rbraceLoc));
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
     * @param stmts
     * @return
     */
    private ActionResult<Stmt> parseStatementOrDeclaration(ArrayList<Stmt> stmts,
            boolean onlyStatements)
    {
        Token tok = S.token;
        switch (tok.tag)
        {
            // C99 6.8.1 labeled-statement
            case IDENTIFIER:
            {
                if (nextTokenIs(COLON))
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
                    SourceLocation declStart = S.token.loc;
                    OutParamWrapper<SourceLocation> end = new OutParamWrapper<>();
                    ArrayList<Decl> decls = parseDeclaration(stmts, TheContext.BlockContext, end);
                    SourceLocation declEnd = end.get();

                    return action.actOnDeclStmt(decls, declStart, declEnd);
                }
                if (nextTokenIs(RBRACE))
                {
                    diag(S.token.loc, err_expected_statement);
                    return stmtError();
                }
                // expression[opt] ';'
                return parseExprStatement();
            }

            case CASE:
            {
                // C99 6.8.1: labeled-statement
                return parseCaseStatement(false, null);
            }
            case DEFAULT:
            {
                // C99 6.8.1: labeled-statement
                return parseDefaultStatement();
            }
            case LBRACE:
            {
                // C99 6.8.2: compound-statement
                return parseCompoundStatement();
            }
            case SEMI:
            {
                // null statement
                SourceLocation loc = consumeToken();
                return action.actOnNullStmt(loc);
            }
            case IF:
            {
                // C99 6.8.4.1: if-statement
                return parseIfStatement();
            }
            case SWITCH:
            {
                return parseSwitchStatement();
            }
            case WHILE:
                return parseWhileStatement();
            case DO:
                return parseDoStatement();
            case FOR:
                return parseForStatement();
            case GOTO:
                return parseGotoStatement();
            case CONTINUE:
                return parseContinueStatement();
            case BREAK:
                return parseBreakStatement();
            case RETURN:
                return parseReturnStatement();
        }
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
        assert nextTokenIs(IDENTIFIER) && S.token.getIdentifierInfo() != null
                : "Not a valid identifier";
        Token identTok = S.token;  // Save the identifier token.
        consumeToken(); // Eat the identifier.

        assert nextTokenIs(COLON) : "Not a label";
        // identifier ':' statement
        SourceLocation colonLoc = consumeToken();

        ActionResult<Stmt> res = parseStatement();
        if (res.isInvalid())
            return new ActionResult<Stmt>(new Tree.NullStmt(colonLoc));

        LabelDecl ld = action.lookupOrCreateLabel(S.token.getIdentifierInfo(),
                S.token.getLocation());

        return action.actOnLabelStmt(S.token.getLocation(), ld, colonLoc, res);
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
        assert nextTokenIs(CASE) || missingCase : "Not a case stmt!";

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
                skipUntil(COLON, true);
                return stmtError();
            }
            // GNU case range extension.
            SourceLocation dotDotDotLoc;
            ActionResult<Expr> rhs;
            if (nextTokenIs(ELLIPSIS))
            {
                diag(S.token, ext_gnu_case_range);
                dotDotDotLoc = consumeToken();

                rhs = parseConstantExpression();
                if (rhs.isInvalid())
                {
                    skipUntil(COLON, true);
                    return stmtError();
                }
            }

            if (nextTokenIs(COLON))
            {
                colonLoc = consumeToken();
            }
            else if (nextTokenIs(SEMI))
            {
                colonLoc = consumeToken();
                diag(colonLoc, err_expected_colon_after).addTaggedVal("'case'")
                .addFixItHint(
                        FixItHint.createReplacement(colonLoc, ":"));
            }
            else
            {
                diag(prevTokLocation, err_expected_colon_after).addTaggedVal("'case'")
                .addFixItHint(
                        FixItHint.createInsertion(prevTokLocation, ":"));
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
        } while(nextTokenIs(CASE));

        assert !topLevelCase.isInvalid() :"Should have parsed at least one case statement";

        ActionResult<Stmt> subStmt;
        if (nextTokenIsNot(RBRACE))
        {
            subStmt = parseStatement();
        }
        else
        {
            diag(colonLoc, err_label_end_of_compound_statement);
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
        assert (nextTokenIs(DEFAULT)) :"Not a default statement!";

        // eat the 'default' keyword
        SourceLocation defaultLoc = consumeToken();

        SourceLocation colonLoc = SourceLocation.NOPOS;
        if (nextTokenIs(COLON))
        {
            colonLoc = consumeToken();
        }
        else if (nextTokenIs(SEMI))
        {
            colonLoc = consumeToken();
            diag(colonLoc, err_expected_colon_after).addTaggedVal("'default'")
                    .addFixItHint(
                            FixItHint.createReplacement(colonLoc, ":"));
        }
        else
        {
            diag(prevTokLocation, err_expected_colon_after).addTaggedVal("'default'")
            .addFixItHint(
                    FixItHint.createInsertion(prevTokLocation, ":"));
            colonLoc = prevTokLocation;
        }

        // diagnose the common error "switch (X) { default:}", which is not valid.
        if (nextTokenIs(RBRACE))
        {
            diag(S.token, err_label_end_of_compound_statement);
            return stmtError();
        }

        ActionResult<Stmt> subStmt = parseStatement();
        if (subStmt.isInvalid())
            return stmtError();

        return  action.actOnDefaultStmt(defaultLoc, colonLoc, subStmt.get());
    }

    private ActionResult<Stmt> parseCompoundStatement()
    {
        return parseCompoundStatement(false, ScopeFlags.DeclScope.value);
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
        assert nextTokenIs(LBRACE) : "Not a compound statement!";

        // Enter a scope to hold everything within the compound stmt.
        // Compound statements can always hold declarations.
        ParseScope compoundScope = new ParseScope(this, scopeFlags);

        // parse the statements in the body.
        return parseCompoundStatementBody(S.token.getLocation(), isStmtExpr);
    }

    private ActionResult<Stmt> parseCompoundStatementBody(
            SourceLocation startLoc, boolean isStmtExpr)
    {
        ArrayList<Stmt> stmts = new ArrayList<>();
        while (nextTokenIsNot(RBRACE) && nextTokenIsNot(EOF))
        {
            ActionResult<Stmt> res = parseStatementOrDeclaration(stmts, false);
            stmts.add(res.get());
        }
        if (nextTokenIsNot(RBRACE))
        {
            diag(S.token, err_expected_lbrace);
            return stmtError();
        }
        // consume '}'
        SourceLocation endLoc = matchRHSPunctuation(RBRACE, startLoc);

        return action.actOnCompoundStmtBody(startLoc, endLoc, stmts, isStmtExpr);
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
        assert nextTokenIs(IF) : "Not an if stmt!";
        SourceLocation ifLoc = consumeToken(); // eat 'if'

        if (nextTokenIsNot(LPAREN))
        {
            diag(S.token, err_expected_lparen_after).addTaggedVal("if");
            return stmtError();
        }
        // C99 6.8.4p3 - In C99, the if statement is a block.  This is not
        // the case for C90.
        // But we take the parser working in the C99 mode for convenience.
        ParseScope ifScope = new ParseScope(this, ScopeFlags.DeclScope.value);
        ActionResult<Expr> condExpr;
        OutParamWrapper<ActionResult<Expr>> x = new OutParamWrapper<>();
        if (parseParenExpression(x, ifLoc, true))
            return  stmtError();

        condExpr = x.get();
        // C99 6.8.4p3
        // In C99, the body of the if statement is a scope, even if
        // there is only a single statement but compound statement.

        // the scope for 'then' statement if there is a '{'
        ParseScope InnerScope = new ParseScope(this,
                ScopeFlags.DeclScope.value,
                nextTokenIs(LBRACE) && getLangOption().c99);

        SourceLocation thenStmtLoc = S.token.getLocation();
        // parse the 'then' statement
        ActionResult<Stmt> thenStmt = parseStatement();

        // pop the 'then' scope if needed.
        InnerScope.exit();

        // IfStmt there is a 'else' statement, parse it.
        SourceLocation elseStmtLoc = SourceLocation.NOPOS;
        SourceLocation elseLoc = SourceLocation.NOPOS;
        ActionResult<Stmt> elseStmt = new ActionResult<>();

        if (nextTokenIs(ELSE))
        {
            // eat the 'else' keyword.
            elseLoc = consumeToken();
            elseStmtLoc = S.token.getLocation();

            // the scope for 'else' statement if there is a '{'
            InnerScope = new ParseScope(this,
                    ScopeFlags.DeclScope.value,
                    nextTokenIs(LBRACE) && getLangOption().c99);

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

    public static ActionResult<Stmt> stmtError()
    {
        return new ActionResult<>(true);
    }

    private ActionResult<Stmt> parseStatement()
    {
        ArrayList<Stmt> stmts = new ArrayList<>();
        return parseStatementOrDeclaration(stmts, true);
    }

    private ActionResult<QualType> parseTypeName()
    {
        return parseTypeName(null);
    }

	/**
     * type-name: [C99 6.7.6]
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
            OutParamWrapper<ParenParseOption> exprType,
            boolean stopIfCastExpr,
            boolean parseAsExprList,
            OutParamWrapper<QualType> castTy,
            OutParamWrapper<SourceLocation> rParenLoc)
    {
        assert nextTokenIs(LPAREN):"Not a paren expression.";
        // eat the '('.
        SourceLocation lParenLoc = consumeParen();
        ActionResult<Expr> result = new ActionResult<>(true);

        if (exprType.get().ordinal()>= CompoundLiteral.ordinal() && isDeclarationSpecifier())
        {
            // This is a compound literal expression or cast expression.
            // First of all, parse declarator.
            ActionResult<QualType> ty = parseTypeName();

            // Match the ')'.
            if (nextTokenIs(RPAREN))
                rParenLoc.set(consumeParen());
            else
                matchRHSPunctuation(RPAREN, lParenLoc);

            if (nextTokenIs(LBRACE))
            {
                exprType.set(CompoundLiteral);
                // TODO return parseCompoundLiteralExpression(ty, lParenLoc, rParenLoc);
                return null;
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
                        /*(parseAsExprList)*/true,
                        /*placeHolder*/0);
                if (!result.isInvalid())
                {
                    result = action.actOnCastExpr(getCurScope(), lParenLoc,
                            castTy.get(), rParenLoc.get(), result.get());
                }
                return result;
            }
            diag(S.token, err_expected_lbrace_in_compound_literal);
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
                        S.token.getLocation(),
                        exprs);
            }
        }
        else
        {
            // simple simple surronding with '()'.
            result = parseExpression();
            exprType.set(SimpleExpr);

            // Don't build a parentheses expression, since it is not needed.
            if (!result.isInvalid() && nextTokenIs(RPAREN))
            {
                // obtains the location of next token of ')'.
                SourceLocation rparen = S.token.getLocation();
                result = action.actOnParenExpr(rParenLoc.get(), rparen, result.get());
            }
        }
        if (result.isInvalid())
        {
            skipUntil(RPAREN, true);
            return exprError();
        }
        if (nextTokenIs(RPAREN))
            rParenLoc.set(consumeParen());
        else
        matchRHSPunctuation(RPAREN, lParenLoc);
        return result;
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
            if (nextTokenIsNot(COMMA))
                return false;

            // consume a ',' and add it's location into comma Locs list.
            commaLocs.add(consumeToken());
        }
    }

    /**
     * Parse the expression surrounding with a pair of parenthesis.
     * <pre>
     *     '(' expression ')'
     * </pre>
     * @param cond
     * @param loc
     * @param convertToBoolean
     * @return
     */
    private boolean parseParenExpression(
            OutParamWrapper<ActionResult<Expr>> cond,
            SourceLocation loc,
            boolean convertToBoolean)
    {
        assert cond != null;
        assert nextTokenIs(LPAREN);
        SourceLocation lparenLoc = consumeParen();

        cond.set(parseExpression());
        if (!cond.get().isInvalid() && convertToBoolean)
        {
            //TODO convert the condition expression to boolean
        }

        if (cond.get().isInvalid() && nextTokenIsNot(RPAREN))
        {
            skipUntil(SEMI, true);
            // skip may have stopped if it found the ')'. IfStmt so, we can
            // continue parsing the if statement.
            if (nextTokenIsNot(RPAREN))
                return true;
        }

        // eat the ')'
        consumeToken();
        // condition is valid or ')' is present.
        return false;
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
        Token tok = S.token;
        assert tokenIs(tok, SWITCH) :"Not a switch statement?";
        // eat the 'switch'
        SourceLocation switchLoc = consumeToken();

        if (nextTokenIsNot(LBRACE))
        {
            diag(S.token, err_expected_lparen_after).addTaggedVal("switch");
            return stmtError();
        }
        // C99 6.8.4p3. In C99, the switch statements is a block. This is not
        // implemented in C90. So, just take C99 into consideration for convenience.
        int scopeFlags = ScopeFlags.SwitchScope.value | ScopeFlags.BreakScope.value;
        if (getLangOption().c99)
            scopeFlags |= ScopeFlags.DeclScope.value | ScopeFlags.ControlScope.value;
        ParseScope switchScope = new ParseScope(this, scopeFlags);

        // Parse the condition expression.
        ActionResult<Expr> condExpr;
        OutParamWrapper<ActionResult<Expr>> res = new OutParamWrapper<>();
        if (parseParenExpression(res, switchLoc, false))
        {
            return stmtError();
        }
        condExpr = res.get();

        ActionResult<Stmt> switchStmt = action.actOnStartOfSwitchStmt(switchLoc, condExpr.get());
        if (switchStmt.isInvalid())
        {
            // skip the switch body
            if (nextTokenIs(LBRACE))
            {
                consumeToken();
                skipUntil(RBRACE, false);
            }
            else
                skipUntil(SEMI, true);
            return switchStmt;
        }

        // C99 6.8.4p3 - In C99, the body of the switch statement is a scope, even if
        // there is no compound stmt.  C90 does not have this clause.  We only do this
        // if the body isn't a compound statement to avoid push/pop in common cases.
        ParseScope innerScope = new ParseScope(this, ScopeFlags.DeclScope.value,
                nextTokenIs(LBRACE) & getLangOption().c99);

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
        assert nextTokenIs(WHILE) :"Not a while statement!";
        // eat the 'while'
        SourceLocation whileLoc = consumeToken();

        if (nextTokenIsNot(LPAREN))
        {
            diag(whileLoc, err_expected_lparen_after).addTaggedVal("while");
            return stmtError();
        }

        // C99 6.8.5p5 - In C99, the while statement is a block.  This is not
        // the case for C90.  Start the loop scope.
        int scopeFlags = 0;
        if (getLangOption().c99)
            scopeFlags = ScopeFlags.BlockScope.value
                | ScopeFlags.ContinueScope.value
                | ScopeFlags.ControlScope.value
                | ScopeFlags.DeclScope.value;
        else
            scopeFlags = ScopeFlags.BlockScope.value
                    | ScopeFlags.ContinueScope.value;

        ParseScope whileScope = new ParseScope(this, scopeFlags);

        OutParamWrapper<ActionResult<Expr>> wrapper = new OutParamWrapper<>();

        // parse the condition.
        if (parseParenExpression(wrapper, whileLoc, true))
            return stmtError();

        ActionResult<Expr> cond = wrapper.get();

        // C99 6.8.5p5 - In C99, the body of the if statement is a scope, even if
        // there is no compound stmt.  C90 does not have this clause.  We only do this
        // if the body isn't a compound statement to avoid push/pop in common cases.
        ParseScope innerScope = new ParseScope(this, ScopeFlags.DeclScope.value,
                nextTokenIs(LBRACE) && getLangOption().c99);

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
        assert nextTokenIs(DO):"Not a do stmt!";

        // eat the 'do'.
        SourceLocation doLoc = consumeToken();
        int scopeFlags = 0;
        if (getLangOption().c99)
            scopeFlags = ScopeFlags.BreakScope.value|
                ScopeFlags.ContinueScope.value | ScopeFlags.DeclScope.value;
        else
            scopeFlags |= ScopeFlags.BreakScope.value |
                    ScopeFlags.ContinueScope.value;

        ParseScope doScope = new ParseScope(this, scopeFlags);

        ParseScope innerScope = new ParseScope(this,
                ScopeFlags.DeclScope.value,
                nextTokenIs(LBRACE) && getLangOption().c99);

        ActionResult<Stmt> body = parseStatement();
        // Pop the body scope.
        innerScope.exit();

        if (nextTokenIsNot(WHILE))
        {
            if (!body.isInvalid())
            {
                diag(S.token, err_expected_while);
                diag(doLoc, note_matching).addTaggedVal("do");
                skipUntil(SEMI, true);
            }
            return stmtError();
        }

        // eat the 'while'.
        SourceLocation whileLoc = consumeToken();

        if (nextTokenIsNot(LPAREN))
        {
            diag(S.token, err_expected_lparen_after).addTaggedVal("do/while");
            skipUntil(SEMI, true);
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
        assert nextTokenIs(FOR):"Not a for loop";
        // eat the 'for'.
        SourceLocation forLoc = consumeToken();

        if (nextTokenIsNot(LPAREN))
        {
            diag(S.token, err_expected_lparen_after).addTaggedVal("for");
            skipUntil(SEMI, true);
            return stmtError();
        }
        int scopeFlags = 0;
        if (getLangOption().c99)
            scopeFlags = ScopeFlags.BreakScope.value
                | ScopeFlags.ContinueScope.value
                | ScopeFlags.DeclScope.value
                | ScopeFlags.ControlScope.value;
        else
            scopeFlags = ScopeFlags.BreakScope.value
                    | ScopeFlags.ContinueScope.value;

        ParseScope forScope = new ParseScope(this, scopeFlags);
        SourceLocation lParenLoc = consumeParen();

        ActionResult<Expr> value;
        ActionResult<Stmt> firstPart = new ActionResult<Stmt>();
        ActionResult<Expr> secondPart = new ActionResult<Expr>();
        boolean secondPartIsInvalid = false;

        // parse the first part
        if (nextTokenIs(SEMI))
        {
            // for ';';
            consumeToken();
        }
        else if (isSimpleDeclaration())
        {
            // parse the declaration, for (int X = 4;
            SourceLocation declStart = S.token.loc;
            ArrayList<Stmt> stmts = new ArrayList<>(32);

            ArrayList<Decl> declGroup = parseSimpleDeclaration(stmts, TheContext.ForContext,
                    false);
            firstPart = action.actOnDeclStmt(declGroup, declStart, S.token.loc);
            if (nextTokenIs(SEMI))
            {
                consumeToken();
            }
            else
            {
                diag(S.token, err_expected_semi_for);
                skipUntil(SEMI, true);
            }
        }
        else
        {
            // for (X = 4;
            value = parseExpression();
            if (!value.isInvalid())
                firstPart = action.actOnExprStmt(value);

            if (nextTokenIs(SEMI))
                consumeToken();
            else
            {
                if (!value.isInvalid())
                    diag(S.token, err_expected_semi_for);
                skipUntil(SEMI, true);
            }
        }

        // parse the second part of the for specifier
        if (nextTokenIs(SEMI))
        {
            // for (...;;
            // no second part
        }
        else if (nextTokenIs(RPAREN))
        {
            // missing both semicolons
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

        if (nextTokenIsNot(SEMI))
        {
            if (!secondPartIsInvalid)
                diag(S.token, err_expected_semi_for);
            else
                skipUntil(RPAREN, true);
        }

        if (nextTokenIs(SEMI))
            consumeToken();

        // parse the third part of for specifier
        ActionResult<Expr> thirdPart = null;
        if (nextTokenIsNot(RPAREN))
        {
            // for (...;...;...)
            thirdPart = parseExpression();
        }

        if (!nextTokenIs(RPAREN))
        {
            diag(S.token, err_expected_lparen_after).
                    addTaggedVal(S.token.getIdentifierInfo());
            skipUntil(RBRACE, true);
        }

        SourceLocation rParenLoc = consumeParen();

        // C99 6.8.5p5 - In C99, the body of the if statement is a scope, even if
        // there is no compound stmt.  C90 does not have this clause.  We only do this
        // if the body isn't a compound statement to avoid push/pop in common cases.

        ParseScope innerScope = new ParseScope(this,
                ScopeFlags.DeclScope.value,
                nextTokenIs(LBRACE));

        ActionResult<Stmt> body = parseStatement();
        innerScope.exit();

        forScope.exit();

        if (body.isInvalid())
            return stmtError();

        return  action.actOnForStmt(forLoc,
                lParenLoc, firstPart.get(),
                secondPart.get(),
                thirdPart.get(),
                rParenLoc, body.get());
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
     * @param stmts
     * @param context
     * @param requiredSemi
     * @return
     */
    private ArrayList<Decl> parseSimpleDeclaration(ArrayList<Stmt> stmts,
            TheContext context,
                    boolean requiredSemi)
    {
        // Parse the common declaration-specifiers piece.
        DeclSpec ds = new DeclSpec();
        parseDeclarationSpecifiers(ds);

        // C99 6.7.2.3p6: Handle "struct-or-union identifier;", "enum { X };"
        // declaration-specifiers init-declarator-list[opt] ';'
        if (nextTokenIs(SEMI))
        {
            if (requiredSemi) consumeToken();
            Decl decl = action.parsedFreeStandingDeclSpec(getCurScope(), ds);

            // TODO ds.complete(decl);
            return action.convertDeclToDeclGroup(decl);
        }

        return parseDeclGroup(ds, context, false);
    }

    private ArrayList<Decl> parseDeclGroup(DeclSpec ds,
            TheContext context, boolean allowFunctionDefinition)
    {
        Declarator d = new Declarator(ds, context);
        parseDeclarator(d);

        // Bail out if the first declarator didn't seem well-formed.
        if (!d.hasName() && !d.mayOmitIdentifier())
        {
            // skip until ; or }
            skipUntil(RBRACE, true);
            if (nextTokenIs(SEMI))
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
                    diag(S.token, err_function_declared_typedef);

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
                diag(S.token, err_invalid_token_after_toplevel_declarator);
            }
            else
            {
                diag(S.token, err_expected_fn_body);
            }
            skipUntil(SEMI, true);
            return declGroups();
        }

        ArrayList<Decl> res = parseInitDeclaratorListAfterFirstDeclarator
                (d, ds, context);
        // eat the last ';'.
        expectAndConsume(SEMI, err_expected_semi_declaration);
        return res;
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
        while (nextTokenIs(COMMA))
        {
            // eat the ','.
            consumeToken();

            // clear the D for parsing next declarator.
            d.clear();

            parseDeclarator(d);

            Decl thisDecl = parseDeclarationAfterDeclarator(d);

            // Next stmt is used for diagnostic in Clang 3.0
            // d.complete(thisDecl);
            if (thisDecl != null)
                declsInGroup.add(thisDecl);
        }

        if (context != TheContext.ForContext
                && nextTokenIsNot(SEMI))
        {
            if (!isDeclarationSpecifier())
            {
                skipUntil(RBRACE, true);
                if (nextTokenIs(SEMI))
                    consumeToken();
            }
        }

        return action.finalizeDeclaratorGroup(ds, declsInGroup);
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
     * @param d
     * @return
     */
    private Decl parseDeclarationAfterDeclarator(Declarator d)
    {
        // inform the semantic module that we just parsed this declarator.
        Decl thisDecl = action.actOnDeclarator(getCurScope(), d);
        if (nextTokenIs(EQ))
        {
            // eat the '='.
            consumeToken();
            // Parses the initializer expression after than '='.
            ActionResult<Expr> init = parseInitializer();
            if (init.isInvalid())
            {
                skipUntil(COMMA, true);
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
        if (nextTokenIsNot(LBRACE))
            return parseAssignExpression();
        return parseBraceInitializer();
    }

    /**
     * Called when parsing an initializer that has a
     * leading open brace.
     *
     * initializer: [C99 6.7.8]
     *   '{' initializer-list '}'
     *   '{' initializer-list ',' '}'
     *
     *   initializer-list:
     *     initializer
     *     initializer-list ',' initializer
     * @return
     */
    private ActionResult<Expr> parseAssignmentExpression()
    {
        assert nextTokenIs(LBRACE);

        SourceLocation lBraceLoc = consumeToken(); // eat '{'
        ArrayList<Expr> initExpr = new ArrayList<>();

        if (nextTokenIs(RBRACE))
        {
            SourceLocation rBraceLoc = consumeToken();
            diag(lBraceLoc, ext_gnu_empty_initializer);
            return new ActionResult<Expr>(new InitListExpr(lBraceLoc, rBraceLoc, new ArrayList<>()));
        }

        boolean initExprOk = true;
        while (true)
        {
            ActionResult<Expr> subInit = parseInitializer();

            if (!subInit.isInvalid())
                initExpr.add(subInit.get());
            else
            {
                initExprOk = false;
                if (nextTokenIsNot(COMMA))
                {
                    skipUntil(RBRACE, false);
                    break;
                }
            }

            if (nextTokenIsNot(COMMA))
                break;

            consumeToken();

            if (nextTokenIs(RBRACE))
                break;
        }

        boolean close = nextTokenIs(RBRACE);
        SourceLocation rBraceLoc = consumeToken();
        if (initExprOk && close)
        {
            return new ActionResult<Expr>(new InitListExpr(lBraceLoc, rBraceLoc, initExpr));
        }
        return exprError();
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
        if (nextTokenIs(BARBAR))
        {
            diag(lbraceLoc, ext_gnu_empty_initializer);

            return action.actOnInitList(lbraceLoc, emptyList(), consumeBrace());
        }

        boolean initExprOK = true;
        while (true)
        {
            ActionResult<Expr> init = parseInitializer();
            if (!init.isInvalid())
                initExprs.add(init.get());
            else
            {
                initExprOK = false;
                if (nextTokenIsNot(COMMA))
                {
                    skipUntil(RBRACE, false, true);
                    break;
                }
            }

            // If we don't have a comma continued list, we're done.
            if (nextTokenIsNot(COMMA))
                break;

            // Eat the ','.
            consumeToken();

            /// Handle trailing comma.
            if (nextTokenIs(RBRACE))
                break;
        }
        if (initExprOK && nextTokenIs(RBRACE))
            return action.actOnInitList(lbraceLoc, initExprs, consumeBrace());;

        matchRHSPunctuation(RBRACE, lbraceLoc);
        return exprError();
    }

    private ActionResult<Stmt> parseGotoStatement()
    {
        assert nextTokenIs(GOTO):"Not a goto stmt!";

        SourceLocation gotoLoc = consumeToken(); // eat the 'goto'

        // 'goto label'.
        ActionResult<Stmt> res = null;
        if (nextTokenIs(IDENTIFIER))
        {
            LabelDecl ld = action.lookupOrCreateLabel(S.token.getIdentifierInfo(),
                    S.token.getLocation());
            res = action.actOnGotoStmt(gotoLoc, S.token.getLocation(), ld);
            consumeToken();
        }
        else
        {
            // erroreous case
            diag(S.token,  err_expected_ident);
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
        assert nextTokenIs(CONTINUE):"Not a continue stmt!";
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
        assert nextTokenIs(BREAK):"Not a break stmt!";
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
        assert nextTokenIs(RETURN):"Not a return stmt!";
        SourceLocation returnLoc = consumeToken();

        ActionResult<Expr> res = new ActionResult<>();
        if (nextTokenIsNot(SEMI))
        {
            // The case of return expression ';'.
            res = parseExpression();
            if (res.isInvalid())
            {
                skipUntil(SEMI, true);
                return stmtError();
            }
        }

        return  action.actOnReturnStmt(returnLoc, res.get());
    }

    /**
     * This function used for parsing declaration, and returns a list which contains
     * each Decl.
     * @param stmts
     * @param dc
     * @param declEnd
     * @return
     */
    private ArrayList<Decl> parseDeclaration(ArrayList<Stmt> stmts,
            TheContext dc, OutParamWrapper<SourceLocation> declEnd)
    {
        assert declEnd != null;

        ArrayList<Decl> res = parseSimpleDeclaration(stmts, dc, false);
        if (nextTokenIsNot(SEMI))
        {
            diag(S.token.loc, err_expected_semi_after);
            skipUntil(SEMI, true);
        }
        else
        {
            // eat a ';'.
            declEnd.set(consumeToken());
        }
        return res;
    }

    /**
     * Parses an expression statement.
     * @return
     */
    private ActionResult<Stmt> parseExprStatement()
    {
        Token oldTok = S.token;

        ActionResult<Expr> res = parseExpression();
        if (res.isInvalid())
        {
            // If the expression is invalid, skip ahead to the next semicolon
            // or '}'.
            skipUntil(RBRACE, true);
            if (nextTokenIs(SEMI))
                consumeToken();
            return stmtError();
        }

        if (nextTokenIs(COLON) && getCurScope().isSwitchScope()
                && action.checkCaseExpression(res.get()))
        {
            // If a constant expression is followed by a colon inside a switch block,
            // suggest a missing case keyword.
            diag(oldTok.loc, err_expected_case_before_expression)
            .addFixItHint(FixItHint.
                    createInsertion(oldTok.getLocation(), "case "));

            // Recover parsing as a case statement.
            return parseCaseStatement(true, res);
        }
        expectAndConsume(SEMI, err_expected_semi_after_expr);

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
        assert declarator.isFunctionDeclarator() :"Isn't a function declarator";
        // int X() {}
        return nextTokenIs(RBRACE);
    }
    /**
     * Determines whether the current token is the part of declaration or
     * declaration list, if it occurs after a declarator.
     * @return
     */
    private boolean isDeclarationAfterDeclarator()
    {
        Token tok = S.token;
        return tokenIs(tok, EQ)            // int X()= -> not a function def
                || tokenIs(tok, COMMA)     // int X(), -> not a function def
                || tokenIs(tok, SEMI);     // int X(); -> not a function def
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
     * Function specifiers (inline) are from C99, and are currently
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
            SourceLocation loc = S.token.loc;
            declSpecs.setRangeStart(loc);
            declSpecs.setRangeEnd(loc);
        }

        out:
        while (nextTokenIs(IDENTIFIER) || nextTokenIsKeyword())
        {
            Token tok = S.token;
            SourceLocation loc = tok.getLocation();
            boolean isInvalid = false;
            String prevSpec = null;
            int diagID = -1;

            switch (tok.tag)
            {
                case IDENTIFIER:
                {
                    // This identifier can only be a typedef getIdentifier if we haven't
                    // already seen a type-specifier.
                    if (declSpecs.hasTypeSpecifier())
                        break out;

                    // So, the current token is a typedef getIdentifier or error.
                    QualType type = action.getTypeByName(
                            tok.getIdentifierInfo(),
                            tok.loc,
                            getCurScope());
                    if (type == null)
                    {
                        // return true, if there is incorrect.
                        if (parseImplicitInt(declSpecs))
                            break out;
                    }

                    OutParamWrapper<String> wrapper1 = new OutParamWrapper<>();
                    OutParamWrapper<Integer> wrapper2 = new OutParamWrapper<>();

                    isInvalid = declSpecs.setTypeSpecType(TST_typename, loc,
                            wrapper1, wrapper2, type);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();

                    if (isInvalid)
                        break;

                    declSpecs.setRangeEnd(S.token.getLocation());
                    // the identifier
                    consumeToken();
                    continue;
                }
                case STATIC:
                    declSpecs.setStorageClassSpec(DeclSpec.SCS.SCS_static, loc);
                    break;
                case EXTERN:
                    isInvalid = declSpecs
                            .setStorageClassSpec(DeclSpec.SCS.SCS_extern, loc);
                    break;
                case REGISTER:
                    declSpecs.setStorageClassSpec(DeclSpec.SCS.SCS_register,
                            loc);
                    break;
                case TYPEDEF:
                    isInvalid = declSpecs
                            .setStorageClassSpec(SCS_typedef, loc);
                    break;
                case AUTO:
                    declSpecs.setStorageClassSpec(DeclSpec.SCS.SCS_auto, loc);
                    break;

                case INLINE:
                    declSpecs.setFunctionSpecInline(loc);
                    break;

                case UNSIGNED:
                    isInvalid = declSpecs.setTypeSpecSign(TSS_unsigned, loc);
                    break;
                case SIGNED:
                    isInvalid = declSpecs.setTypeSpecSign(TSS_signed, loc);
                    break;

                case LONG:
                    isInvalid = declSpecs.setTypeSpecWidth(TSW_long, loc);
                    break;
                case SHORT:
                    isInvalid = declSpecs.setTypeSpecWidth(TSW_short, loc);
                    break;
                case COMPLEX:
                    isInvalid = declSpecs.setTypeSpecComplex(TSC_complex, loc);
                    break;
                case CHAR:
                {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper<>();
                    OutParamWrapper<Integer> wrapper2 = new OutParamWrapper<>();
                    isInvalid = declSpecs.setTypeSpecType(TST_char, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                case INT:
                {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper<>();
                    OutParamWrapper<Integer> wrapper2 = new OutParamWrapper<>();
                    isInvalid = declSpecs.setTypeSpecType(TST_int, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                case FLOAT:
                {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper<>();
                    OutParamWrapper<Integer> wrapper2 = new OutParamWrapper<>();
                    isInvalid = declSpecs.setTypeSpecType(TST_float, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                case DOUBLE:
                {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper<>();
                    OutParamWrapper<Integer> wrapper2 = new OutParamWrapper<>();
                    isInvalid = declSpecs.setTypeSpecType(TST_double, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                case VOID:
                {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper<>();
                    OutParamWrapper<Integer> wrapper2 = new OutParamWrapper<>();
                    isInvalid = declSpecs.setTypeSpecType(TST_void, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                case BOOL:       {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper<>();
                    OutParamWrapper<Integer> wrapper2 = new OutParamWrapper<>();
                    isInvalid = declSpecs.setTypeSpecType(TST_bool, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diagID = wrapper2.get();
                    break;
                }
                // enum specifier
                case ENUM:
                {
                    consumeToken();
                    parseEnumSpecifier(loc, declSpecs);
                    continue;
                }
                // struct-union specifier
                case STRUCT:
                case UNION:
                {
                    // eat the 'struct' or 'union'.
                    int kind = tok.tag;
                    consumeToken();
                    parseStructOrUnionSpecifier(kind, loc, declSpecs);
                    continue;
                }
                // type-qualifiers
                case CONST:
                    isInvalid = declSpecs.setTypeQualifier(TQ_const, loc);
                    break;
                case VOLATILE:
                    isInvalid = declSpecs.setTypeQualifier(TQ_volatile, loc);
                    break;
                case RESTRICT:
                    isInvalid = declSpecs.setTypeQualifier(TQ_restrict, loc);
                    break;
                default:
                    break out;
            }
            // if the specifier is illegal, issue a diagnostic.
            if (isInvalid)
            {
                assert prevSpec != null :"Method did not return previous specifier!";
                assert diagID >= 0;
                diag(tok, diagID).addTaggedVal(prevSpec);
            }
            declSpecs.setRangeEnd(S.token.loc);
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
        assert nextTokenIs(IDENTIFIER) : "should have identifier.";
        SourceLocation loc = consumeToken();

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
        assert !ds.hasTypeSpecifier() : "Type specifier checked above";

        // Since we know that this either implicit int (which is rare) or an
        // error, we'd do lookahead to try to do better recovery.
        if (isValidAfterIdentifierDeclarator(peekAheadToken()))
        {
            // if this token is valid for implicit int,
            // e.g. "static x = 4", then we just avoid eating the identifier, so
            // it will be parsed as the identifier in the declarator.
            return false;
        }

        String tagName = null;
        int tagKind = -1;
        String identifierInfo = S.token.getIdentifierInfo();
        switch (action.isTagName(identifierInfo, getCurScope()))
        {
            default:break;
            case TST_enum: tagName = "enum"; tagKind = Tag.ENUM; break;
            case TST_struct: tagName = "struct"; tagKind = Tag.STRUCT; break;
            case TST_union: tagName = "union"; tagKind = Tag.UNION; break;
        }
        if (tagName != null)
        {
            diag(loc, err_use_of_tag_name_without_tag)
            .addTaggedVal(identifierInfo)
            .addTaggedVal(tagName)
            .addFixItHint(
                    FixItHint.createInsertion(S.token.loc,
                    tagName));

            if (tagKind == ENUM)
            {
                parseEnumSpecifier(loc, ds);
            }
            else
            {
                parseStructOrUnionSpecifier(tagKind, loc, ds);
            }
            return true;
        }
        diag(loc, err_unknown_typename).addTaggedVal(identifierInfo);

        OutParamWrapper<String> prevSpec = new OutParamWrapper<>();
        OutParamWrapper<Integer> diag = new OutParamWrapper<>();
        // mark as an error
        ds.setTypeSpecType(TST_error, loc, prevSpec, diag);
        ds.setRangeEnd(S.token.getLocation());
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
     * int x   =             17;         // init-declarator-list
     * int x   ,             y;          // init-declarator-list
     * int x   :             4;          // struct-declarator
     * <p>
     * This is not, because 'x' does not immediately follow the declspec (though
     * ')' happens to be valid anyway).
     * int (x)
     */
    private boolean isValidAfterIdentifierDeclarator(Token tok)
    {
        return tokenIs(tok, LBRACKET) || tokenIs(tok, LPAREN) || tokenIs(tok,
                SEMI) || tokenIs(tok, COMMA) || tokenIs(tok, EQ) || tokenIs(tok,
                COLON);
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
        Token tok = S.token;

        // Must have either 'enum name' or 'enum {...}'.
        if (!tokenIs(tok, IDENTIFIER) && !tokenIs(tok, LBRACE))
        {
            diag(tok, err_expected_ident_lbrace);
            // skip the rest of this declarator, up until a ',' or ';' encounter.
            skipUntil(COMMA, true);
            return;
        }

        String name = null;
        SourceLocation nameLoc = SourceLocation.NOPOS;
        if (tokenIs(tok, IDENTIFIER))
        {
            name = tok.getIdentifierInfo();
            nameLoc = tok.loc;
        }

        // There are three options here.  If we have 'enum foo;', then this is a
        // forward declaration.  If we have 'enum foo {...' then this is a
        // definition. Otherwise we have something like 'enum foo xyz', a reference.
        Sema.TagUseKind tuk;
        if (tokenIs(tok, LBRACE))
            tuk = TUK_definition;
        else if (tokenIs(tok, SEMI))
            tuk = TUK_declaration;
        else
            tuk = TUK_reference;

        if (name == null && tuk != TUK_definition)
        {
            diag(tok, err_enumerator_unnamed_no_def);
            // Skip the rest of this declarator, up until the comma or semicolon.
            skipUntil(COMMA, true);
            return;
        }

        ActionResult<Decl> tagDecl = action.actOnTag(
                getCurScope(),
                TST_enum,
                tuk,
                startLoc,
                name,
                nameLoc);

        if (tagDecl.isInvalid())
        {
            // the action failed to produce an enumeration tag.
            // if this is a definition, consume the entire definition.
            if (tokenIs(tok, LBRACE))
            {
                consumeToken();
                skipUntil(RBRACE, true);
            }

            ds.setTypeSpecError();
            return;
        }

         if (tokenIs(tok, LBRACE))
            parseEnumBody(startLoc, tagDecl.get());

        OutParamWrapper<String> prevSpecWrapper = new OutParamWrapper<>();
        OutParamWrapper<Integer> diagWrapper = new OutParamWrapper<>();

        if (ds.setTypeSpecType(TST_enum,
                startLoc,
                name != null ? nameLoc : startLoc,
                prevSpecWrapper, diagWrapper,
                tagDecl.get()))
        {
            diag(startLoc, diagWrapper.get()).
                    addTaggedVal(prevSpecWrapper.get());
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
        ParseScope enumScope = new ParseScope(this, ScopeFlags.DeclScope.value);
        action.actOnTagStartDefinition(getCurScope(), enumDecl);

        // eat '{'
        SourceLocation lbraceLoc = consumeBrace();
        if (nextTokenIs(RBRACE))
        {
            diag(S.token, ext_empty_struct_union_enum);
        }

        Token tok = S.token;

        ArrayList<Decl> enumConstantDecls = new ArrayList<>(32);
        Decl lastEnumConstDecl = null;
        // Parse the enumerator-list.
        while(tokenIs(tok, IDENTIFIER))
        {
            String name = tok.getIdentifierInfo();
            SourceLocation identLoc = consumeToken();

            SourceLocation equalLoc = SourceLocation.NOPOS;
            ActionResult<Expr> val = null;
            if (nextTokenIs(EQ))
            {
                equalLoc = consumeToken();
                val = parseConstantExpression();
                // if the constant expression is invalid, skip rest of
                // enum-declaration-list until a comma.
                if (val.isInvalid())
                {
                    skipUntil(COMMA, RBRACE, true);
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
            if (nextTokenIsNot(COMMA))
                break;
            SourceLocation commaLoc = consumeToken();

            if (nextTokenIsNot(IDENTIFIER) && !getLangOption().c99)
            {
                // we missing a ',' between enumerators
                diag(commaLoc, ext_enumerator_list_comma)
                .addTaggedVal(getLangOption().c99)
                .addFixItHint(FixItHint.createRemoval(
                        new SourceRange(commaLoc)));
            }
        }

        // eat the '}'
       SourceLocation rBraceLoc = matchRHSPunctuation(RBRACE, lbraceLoc);

        action.actOnEnumBody(
                startLoc,
                lbraceLoc,
                rBraceLoc,
                enumDecl,
                enumConstantDecls,
                getCurScope());

        enumScope.exit();
        action.actOnTagFinishDefinition(getCurScope(), enumDecl, rBraceLoc);
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
            int tagTokKind,
            SourceLocation startLoc,
            DeclSpec ds)
    {
        DeclSpec.TST tagType;
        if (tagTokKind == STRUCT)
            tagType = TST_struct;
        else
        {
            assert tagTokKind == UNION : "Not a union specifier";
            tagType = TST_union;
        }

        // Parse the (optional) class name
        String name = null;
        SourceLocation nameLoc = SourceLocation.NOPOS;
        if (nextTokenIs(IDENTIFIER))
        {
            name = S.token.getIdentifierInfo();
            nameLoc = consumeToken();
        }

        // There are four cases for the grammar starting with keyword class-specifier.
        // 1. forward declaration: for example, struct X;
        // 2. definition: for example, struct X {...};
        // 3. annoymous definition of struct: struct {...};
        // 4. reference used in varaible declaration: struct X x;

        Sema.TagUseKind tuk;
        if (nextTokenIs(LBRACE))
        {
            // so,this is a struct definition.
            tuk = TUK_definition;
        }
        else if (nextTokenIs(SEMI))
            tuk = TUK_declaration;
        else
            tuk = TUK_reference;

        if (name == null && (ds.getTypeSpecType() == TST_error
                || tuk != TUK_definition))
        {
            if (ds.getTypeSpecType() != TST_error)
            {
                // we have a declaration or reference to an anonymous struct.
                diag(startLoc, err_anon_type_definition);
            }
            skipUntil(COMMA, true);
            return;
        }

        // create the tag portion of the struct or union.

        // declaration or definitions of a struct or union type
        ActionResult<Decl> tagOrTempResult =
                action.actOnTag(
                getCurScope(), tagType,
                tuk, startLoc, name, nameLoc);
        // if there is a body, parse it and perform actions
        if (nextTokenIs(LBRACE))
        {
            parseStructOrUnionBody(startLoc, tagType, tagOrTempResult.get());
        }
        else if (tuk == TUK_definition)
        {
            diag(S.token, err_expected_lbrace);
        }


        if (tagOrTempResult.isInvalid())
        {
            ds.setTypeSpecError();
            return;
        }

        OutParamWrapper<String> w1 = new OutParamWrapper<>();
        OutParamWrapper<Integer> w2 = new OutParamWrapper<>();
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
            diag(startLoc, diagID).addTaggedVal(prevSpec);
        }

        if (tuk == TUK_definition)
        {
            boolean expectedSemi = true;
            switch (S.token.tag)
            {
                default:
                    break;
                    // struct foo {...} ;
                case Token.SEMI:
                    // struct foo {...} *         P;
                case Token.STAR:
                    // struct foo {...} V         ;
                case Token.IDENTIFIER:
                    //(struct foo {...} )         {4}
                case Token.RPAREN:
                    // struct foo {...} (         x);
                case Token.LPAREN:
                    expectedSemi = false;
                    break;
                // type-specifier
                case Token.CONST:             // struct foo {...} const     x;
                case Token.VOLATILE:          // struct foo {...} volatile     x;
                case Token.RESTRICT:          // struct foo {...} restrict     x;
                case Token.INLINE:            // struct foo {...} inline   foo();
                    // storage-class specifier
                case Token.STATIC:            // struct foo {...} static     x;
                case Token.EXTERN:            // struct foo {...} extern     x;
                case Token.TYPEDEF:           // struct foo {...} typedef    x;
                case Token.REGISTER:          // struct foo {...} register   x;
                case Token.AUTO:              // struct foo {...} auto       x;
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
                    if (isKnownBeTypeSpecifier(S.token))
                        expectedSemi = false;
                    break;
                }

                // struct bar { struct foo {...} }
                case Token.RBRACE:
                    // missing ';' at the end f struct is ccepted as an extension in C mode
                    expectedSemi = false;
                    break;
            }
            if (expectedSemi)
            {
                if (nextTokenIs(SEMI))
                {
                    consumeToken();
                }
                else
                {
                    diag(S.token, err_expected_semi_after).
                            addTaggedVal(tagType == TST_union ? "union":"struct");
                    skipUntil(SEMI, true);
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
        switch (tok.tag)
        {
            default:
                return false;
            case SHORT:
            case LONG:
            case SIGNED:
            case UNSIGNED:
            case COMPLEX:
            case VOID:
            case CHAR:
            case INT:
            case FLOAT:
            case DOUBLE:
            case BOOL:

            case STRUCT:
            case UNION:
            case ENUM:

            case ANN_TYPENAME:
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
                | ScopeFlags.DeclScope.value);
        action.actOnTagStartDefinition(getCurScope(), tagDecl);

        // Empty structs are an extension in C (C99 6.7.2.1p7)
        if (nextTokenIs(RBRACE))
        {
            consumeToken();
            diag(lBraceLoc, ext_empty_struct_union_enum).addTaggedVal(structOrUnion);
            skipUntil(SEMI, true);
        }

        // an array stores all fields declared in current struct/union body.
        ArrayList<Decl> fieldDecls = new ArrayList<>(32);

        // While we still have something to read until '}' or 'eof' encounter.
        while (nextTokenIsNot(RBRACE) && nextTokenIsNot(EOF))
        {
            // each iteration of this loop reads one struct-declaration.
            if (nextTokenIs(SEMI))
            {
                diag(S.token, ext_extra_struct_semi).addTaggedVal(DeclSpec
                .getSpecifierName(tagType))
                .addFixItHint(FixItHint.createRemoval
                        (new SourceRange(S.token.getLocation())));
                consumeToken();
                continue;
            }

            // parse all the comma separated declarators.
            DeclSpec ds = new DeclSpec();

            FieldCallBack callBack = new FieldCallBack(this, tagDecl,
                    fieldDecls);
            parseStructDeclaration(ds, callBack);

            if (nextTokenIs(SEMI))
                consumeToken();
            else if (nextTokenIs(RBRACE))
            {
                expectAndConsume(SEMI, ext_expected_semi_decl_list);
                break;
            }
            else
            {
                expectAndConsume(SEMI, ext_expected_semi_decl_list);
                skipUntil(RBRACE, true);
                // if we stopped at a ';', consume it.
                if (nextTokenIs(SEMI))
                    consumeToken();
            }
        }

        SourceLocation rBraceLoc = consumeBrace();
        // eat '}'
        action.actOnFields(getCurScope(),
                recordLoc,
                tagDecl,
                fieldDecls,
                lBraceLoc, rBraceLoc);

        structScope.exit();
        action.actOnTagFinishDefinition(getCurScope(),
                tagDecl,
                rBraceLoc);
    }

    /**
     * Parse a struct declaration without the terminating semicolon.
     * <pre>
     *   struct-declaration:
     *     specifier-qualifier-list struct-declarator-list
     *
     *   specifier-qualifier-list:
     *     type-specifier specifier-qualifier-list[opt]
     *     type-qualifier specifier-qualifier-list[opt]
     *
     *   struct-declarator-list:
     *     struct-declarator
     *     struct-declarator-list , struct-declarator
     *
     *   struct-declarator:
     *     declarator
     *     declarator[opt] : constant-expression
     * </pre>
     *
     * @param ds
     * @param callBack
     */
    private void parseStructDeclaration(DeclSpec ds, FieldCallBack callBack)
    {
        // parse common specifier-qualifier
        parseSpecifierQualifierList(ds);

        // If there are no declarators, this is a single standing declaration-
        // specifier.
        if (nextTokenIs(SEMI))
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
            if (nextTokenIsNot(COLON))
            {
                parseDeclarator(declaratorField.declarator);
            }

            // parse the constant-subExpr after declarator if there is a ':;
            if (nextTokenIs(COLON))
            {
                consumeToken();
                ActionResult<Expr> result = parseConstantExpression();
                if (result.isInvalid())
                    skipUntil(SEMI, true);
                else
                    declaratorField.bitFieldSize = result.get();
            }

            // we are done with this declarator; invoke the callback
            callBack.invoke(declaratorField);

            if (nextTokenIsNot(COMMA))
                return;

            consumeToken();
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
     *
     * @param declarator
     */
    private void parseDeclarator(Declarator declarator)
    {
        Token tok = S.token;
        //declarator.setRangeEnd(tok.loc);
        if (nextTokenIsNot(STAR))
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
                        tok.loc,
                        ds.getConstSpecLoc(),
                        ds.getVolatileSpecLoc(),
                        ds.getRestrictSpecLoc()),
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
        if (nextTokenIs(IDENTIFIER) && declarator.mayHaveIdentifier())
        {
            /**
             The direct declarator must start with an identifier (possibly
             omitted) or a parenthesized declarator (possibly abstract).  In
             an ordinary declarator, initial parentheses must start a
             parenthesized declarator.  In an abstract declarator or parameter
             declarator, they could start a parenthesized declarator or a
             parameter list.
             */
            String id = S.token.getIdentifierInfo();
            assert id != null : "Not an identifier?";
            declarator.setIdentifier(id, S.token.getLocation());
            consumeToken();
        }
        else if (nextTokenIs(LPAREN))
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
            declarator.setIdentifier(null, S.token.loc);
        }
        else
        {
            if (declarator.getContext() == TheContext.StructFieldContext)
                diag(S.token, err_expected_member_name_or_semi)
                        .addSourceRange(declarator.getDeclSpec().getSourceRange());
            else
                diag(S.token, err_expected_ident_lparen);

            declarator.setIdentifier(null, S.token.getLocation());
            declarator.setInvalidType(true);
        }

        assert declarator.isPastIdentifier()
                :"Haven't past the location of the identifier yet?";

        while (true)
        {
            if (nextTokenIs(LPAREN))
                parseFunctionDeclarator(consumeParen(), declarator, false);
            else if (nextTokenIs(LBRACKET))
                parseBracketDeclarator(declarator);
            else
                break;
        }
    }

    private boolean isDeclarationSpecifier(boolean disambiguatingWithExpression)
    {
        switch (S.token.tag)
        {
            default:return false;
            case IDENTIFIER:
                return isDeclarationSpecifier();
            case Tag.TYPEDEF:
            case Tag.EXTERN:
            case Tag.STATIC:
            case Tag.AUTO:
            case Tag.REGISTER:

            case Tag.SHORT:
            case Tag.LONG:
            case Tag.SIGNED:
            case Tag.UNSIGNED:
            case Tag.COMPLEX:
            case Tag.VOID:
            case Tag.CHAR:
            case Tag.INT:
            case Tag.FLOAT:
            case Tag.DOUBLE:
            case Tag.BOOL:

            case Tag.STRUCT:
            case Tag.UNION:
            case Tag.ENUM:

            case Tag.CONST:
            case Tag.VOLATILE:
            case Tag.RESTRICT:

            case Tag.INLINE:
                return true;

            // typedefs-getIdentifier
            case ANN_TYPENAME:
                return !disambiguatingWithExpression;
        }
    }

    private boolean isDeclarationSpecifier()
    {
        switch (S.token.tag)
        {
            default: return false;
            case Tag.TYPEDEF:
            case Tag.EXTERN:
            case Tag.STATIC:
            case Tag.AUTO:
            case Tag.REGISTER:

            case Tag.SHORT:
            case Tag.LONG:
            case Tag.SIGNED:
            case Tag.UNSIGNED:
            case Tag.COMPLEX:
            case Tag.VOID:
            case Tag.CHAR:
            case Tag.INT:
            case Tag.FLOAT:
            case Tag.DOUBLE:
            case Tag.BOOL:

            case Tag.STRUCT:
            case Tag.UNION:
            case Tag.ENUM:

            case Tag.CONST:
            case Tag.VOLATILE:
            case Tag.RESTRICT:

            case Tag.INLINE:
                return true;
        }
    }

    /**
     * direct-declarator:
     *   '(' declarator ')'
     *   direct-declarator '(' parameter-type-list ')'
     *   direct-declarator '(' identifier-list[opt] ')'
     * @param declarator
     */
    private void parseParenDeclarator(Declarator declarator)
    {
        // eat the '('.
        assert nextTokenIs(LPAREN);
        SourceLocation lparenLoc = consumeParen();

        assert !declarator.isPastIdentifier()
                :"Should be called before passing identifier";
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
        else
        {
            // 'int()' is a function.
            isGrouping = !(nextTokenIs(RPAREN) || isDeclarationSpecifier());
        }
        // If this is a grouping paren, handle:
        // direct-declarator: '(' declarator ')'
        if (isGrouping)
        {
            parseDeclarator(declarator);

            // match ')'
            consumeParen();
            return;
        }

        // Okay, if this wasn't a grouping paren, it must be the start of a function
        // argument list.  Recognize that this declarator will never have an
        // identifier (and remember where it would have been), then call into
        // ParseFunctionDeclarator to handle of argument list.
        declarator.setIdentifier(null, S.token.loc);
        consumeToken();
        parseFunctionDeclarator(lparenLoc, declarator, true);
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
        assert declarator.isPastIdentifier():"Should not call before identifier!";
        // this should be true when the function has typed arguments.
        // Otherwise, it will be treated as K&R style function.
        boolean hasProto = false;

        // This parameter list may be empty.
        SourceLocation rparenLoc;
        SourceLocation endLoc;
        if (nextTokenIs(RPAREN))
        {
            if (requireArg)
                diag(S.token, err_argument_required_after_attribute);

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
        ParseScope protoTypeScope =
                new ParseScope(this,
                        ScopeFlags.FunctionProtoTypeScope.value
                        | ScopeFlags.DeclScope.value);
        if (nextTokenIsNot(RPAREN))
        {
            ellipsisLoc = parseParameterDeclarationClause(declarator, paramInfos);
        }

        hasProto = !paramInfos.isEmpty();

        // if we have the closing ')', eat it.
        rparenLoc = endLoc = matchRHSPunctuation(RPAREN, lparenLoc);

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
        return nextTokenIs(IDENTIFIER) && nextTokenIsNot(ANN_TYPENAME)
                    && (tokenIs(peekAheadToken(), COMMA)
                || tokenIs(peekAheadToken(), RPAREN));
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
        HashSet<String> paramsSoFar = new HashSet<>(8);
        ArrayList<ParamInfo> paramInfos = new ArrayList<>();

        // If there was no identifier specified for the declarator, either we are in
        // an abstract-declarator, or we are in a parameter declarator which was found
        // to be abstract.  In abstract-declarators, identifier lists are not valid:
        // diagnose this.
        if (declarator.getIdentifier() == null)
            diag(S.token, ext_ident_list_in_param);

        paramsSoFar.add(S.token.getIdentifierInfo());
        paramInfos.add(new ParamInfo(S.token.getIdentifierInfo(),
                S.token.getLocation(),
                null));

        // Consume the first identifier.
        consumeToken();

        while (nextTokenIs(COMMA))
        {
            // Eat the comma.
            consumeToken();

            // if the next token is not a identifier,
            // report the error and skip it until ')'.
            if (nextTokenIsNot(IDENTIFIER))
            {
                diag(S.token, err_expected_ident);
                skipUntil(RPAREN, true);
                paramInfos.clear();
                return;
            }

            // reject typedef int y; int test(x, y), but continue parsing.
            String paramII = S.token.getIdentifierInfo();
            if (action.getTypeByName(paramII, S.token.getLocation(), getCurScope()) != null)
            {
                diag(S.token, err_unexpected_typedef_ident).
                        addTaggedVal(paramII);
            }

            // Verify that the argument identifier has not already been mentioned.
            if (!paramsSoFar.add(paramII))
            {
                diag(S.token, err_param_redefinition).addTaggedVal(paramII);
            }
            else
            {
                // Remember this identifier in ParamInfo.
                paramInfos.add(new ParamInfo(paramII, S.token.getLocation(),
                        null));
            }

            // Eat the identifier.
            consumeToken();
        }
        // If we have the closing ')', eat it and we're done.
        SourceLocation rparenloc = matchRHSPunctuation(RPAREN, lparenLoc);
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
     *       declaration-specifiers abstract-declarator[opt]
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
            if (nextTokenIs(ELLIPSIS))
            {
                // consume the '...'
                ellipsisLoc = consumeToken();
                break;
            }

            // parse the declaration-specifiers
            // just use the parsingDeclaration "scope" of the declarator.
            DeclSpec ds = new DeclSpec();
            SourceLocation dsstartLoc = S.token.getLocation();

            // parse the declaration specifiers
            parseDeclarationSpecifiers(ds);

            // Parse the declarator.
            // This is "FunctionProtoTypeContext".
            Declarator paramDecls = new Declarator(ds, TheContext.FunctionProtoTypeContext);
            parseDeclarator(paramDecls);

            // remember this parsed parameter in ParamInfo.
            String paramName = paramDecls.getIdentifier();

            // if no parameter specified, verify that "something" was specified,
            // otherwise we have a missing type and identifier.
            if (ds.isEmpty() && paramName == null
                    && paramDecls.getNumTypeObjects() == 0)
            {
                diag(dsstartLoc, err_missing_param);
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
            if (nextTokenIsNot(COMMA))
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
        if (nextTokenIs(RBRACKET))
        {
            // eat the ']'.
            rbracketLoc = matchRHSPunctuation(RBRACKET, lbracketLoc);

            // remember that we parsed the empty array declaration.
            declarator.addTypeInfo(
                    DeclaratorChunk.getArray(0, false, false, null,
                            lbracketLoc, rbracketLoc),
                    rbracketLoc);
            return;
        }
        else if (nextTokenIs(INTLITERAL)
                && tokenIs(peekAheadToken(), RBRACKET))
        {
            // [4] is evey common. parse the number
            ActionResult<Expr> numOfSize = action.actOnNumericConstant(S.token);

            // eat the ']'.
            rbracketLoc = matchRHSPunctuation(RBRACKET, lbracketLoc);
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
        if (nextTokenIs(STATIC))
            staticLoc = consumeToken();

        // If there is a type-qualifier-list, read it now.
        // Type qualifiers in an array subscript are a C99 feature.
        DeclSpec ds = new DeclSpec();
        parseTypeQualifierListOpt(ds);

        // If we haven't already read 'static', check to see if there is one after the
        // type-qualifier-list.
        if (!staticLoc.isValid() && nextTokenIs(STATIC))
            staticLoc = consumeToken();

        // Handle "direct-declarator [ type-qual-list[opt] * ]".
        boolean isStar = false;
        ActionResult<Expr> numElements = new ActionResult<>();

        if (nextTokenIs(STAR) && tokenIs(peekAheadToken(), RBRACKET))
        {
            consumeToken();     // Eat the '*'.
            if (staticLoc.isValid())
            {
                diag(staticLoc, err_unspecified_vla_size_with_static);
                staticLoc = SourceLocation.NOPOS;
            }
            isStar = true;
        }
        else if (nextTokenIsNot(RBRACKET))
        {
            // Parse the constant-expression or assignment-expression now (depending
            // on dialect).
            numElements = parseAssignExpression();
        }
        // If there was an error parsing the assignment-expression, recovery.
        if (numElements.isInvalid())
        {
            declarator.setInvalidType(true);
            skipUntil(RBRACKET, true);
            return;
        }

        SourceLocation endLoc = matchRHSPunctuation(RBRACKET, lbracketLoc);

        // Remember that we parsed a array type, and remember its features.
        declarator.addTypeInfo(DeclaratorChunk.getArray(
                ds.getTypeQualifier(), staticLoc.isValid(),
                isStar, numElements.get(),
                lbracketLoc, rbracketLoc),
                rbracketLoc);
    }

    /**
     * ParseTypeQualifierListOpt
     * <pre>
     * type-qualifier-list: [C99 6.7.5]
     *   type-qualifier
     * </pre>
     * @param ds
     */
    private void parseTypeQualifierListOpt(DeclSpec ds)
    {
        while(true)
        {
            boolean isInvalid = false;
            SourceLocation loc = S.token.getLocation();
            switch (S.token.tag)
            {
                case Tag.CONST:
                    isInvalid = ds.setTypeQualifier(TQ_const, loc);
                    break;
                case Tag.VOLATILE:
                    isInvalid = ds.setTypeQualifier(TQ_volatile, loc);
                    break;
                case Tag.RESTRICT:
                    isInvalid = ds.setTypeQualifier(TQ_restrict, loc);
                    break;
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
            loc = consumeToken();
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
            diag(S.token, err_typename_requires_specqual);
        }
        // issue diagnostic and remove storage class if present
        if ((specs & ParsedSpecifiers.PQ_StorageClassSpecifier) != 0)
        {
            if (ds.getStorageClassSpecLoc().isValid())
                diag(ds.getStorageClassSpecLoc(), err_typename_invalid_storageclass);

            ds.clearStorageClassSpec();
        }

        // issue diagnostic and remove function specifier if present
        if ((specs & ParsedSpecifiers.PQ_FunctionSpecifier) != 0)
        {
            if (ds.isInlineSpecifier())
                diag(ds.getInlineSpecLoc(), err_typename_invalid_functionspec);
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
        int nextTokPrec = getBinOpPrecedence(S.token.tag);

        SourceLocation colonLoc = SourceLocation.NOPOS;
        while(true)
        {
            // if this token has a lower precedence than we are allowed to parse
            // then we are done.
            if (nextTokPrec < minPrec)
                return lhs;

            // consume the operator token, then advance the tokens stream.
            Token opToken = S.token;
            consumeToken();

            // Special case handling for the ternary (?...:...) operator.
            ActionResult<Expr> ternaryMiddle = new ActionResult<>(true);
            if (nextTokPrec == PrecedenceLevel.Conditional)
            {
                if(nextTokenIsNot(COLON))
                {
                    // Handle this production specially
                    //   logical-OR-expression '?' expression ':' conditional-expression
                    // In particular, the RHS of the '?' is 'expression', not
                    // 'logical-OR-expression' as we might expect.
                    ternaryMiddle = parseExpression();
                    if (ternaryMiddle.isInvalid())
                    {
                        lhs = exprError();
                        ternaryMiddle = null;
                    }
                }
                else
                {
                    // Special cas handling of "X ? Y:Z" where Y is empty.
                    //   logical-OR-expression '?' ':' conditional-expression   [GNU]
                    ternaryMiddle = null;
                    diag(S.token, ext_gnu_conditional_expr);
                }

                if (nextTokenIs(COLON))
                    colonLoc = consumeToken(); // eat the ':'.
                else
                {
                    SourceLocation filoc = S.token.getLocation();
                    // We missing a ':' after ternary middle expression.
                    diag(S.token, err_expected_colon)
                    .addFixItHint(FixItHint.
                            createInsertion(filoc, ": "));
                    diag(opToken, note_matching).addTaggedVal("?");
                    colonLoc = S.token.loc;
                }
            }

            // Parse another leaf here for the RHS of the operator.
            // ParseCastExpression works here because all RHS expressions in C
            // have it as a prefix, at least.
            ActionResult<Expr> rhs = parseCastExpression(false, false, false, 0);

            if (rhs.isInvalid())
                lhs = exprError();

            // Remember the precedence of this operator and get the precedence of the
            // operator immediately to the right of the RHS.
            int thisPrec = nextTokPrec;
            nextTokPrec = getBinOpPrecedence(S.token.tag);

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
                nextTokPrec = getBinOpPrecedence(S.token.tag);
            }

            assert (nextTokPrec <= thisPrec):"Recursive doesn't works!";

            if (!lhs.isInvalid())
            {
                // Combine the LHS and RHS into the LHS (e.g. builds AST)
                if (ternaryMiddle.isInvalid())
                {
                    lhs = action.actOnBinOp( opToken.loc,
                            opToken.tag, lhs.get(), rhs.get());
                }
                else
                {
                    lhs = action.actOnConditionalOp(opToken.loc, colonLoc,
                            lhs.get(), ternaryMiddle.get(), rhs.get());
                }
            }
        }
    }

    private ActionResult<Expr>
        parseCastExpression(
            boolean isUnaryExpression,
            boolean isAddressOfOperand,
            boolean isTypeCast,
            int placeHolder)
    {
        OutParamWrapper<Boolean> x = new OutParamWrapper<>(false);
        ActionResult<Expr> res = parseCastExpression(isUnaryExpression,
                isAddressOfOperand, isTypeCast, x);
        if (x.get())
            diag(S.token, err_expected_expression);
        return res;
    }

    enum ParenParseOption
    {
        SimpleExpr,      // Only parse '(' expression ')'
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
            OutParamWrapper<Boolean> notCastExpr)
    {
        ActionResult<Expr> res = null;
        Token nextTok = S.token;
        int savedKind = nextTok.tag;
        notCastExpr.set(false);

        switch (savedKind)
        {
            case LPAREN:
            {
                QualType castTy = null;
                OutParamWrapper<QualType> out1 = new OutParamWrapper<>();
                SourceLocation rParenLoc = SourceLocation.NOPOS;
                OutParamWrapper<SourceLocation> out2 =
                        new OutParamWrapper<>(rParenLoc);

                ParenParseOption parenExprTppe =
                        isUnaryExpression ? CompoundLiteral:CastExpr;
                OutParamWrapper<ParenParseOption> out3 = new OutParamWrapper<>(parenExprTppe);

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
            case Token.INTLITERAL:
            case Token.FLOATLITERAL:
            case Token.DOUBLELITERAL:
            case Token.LONGLITERAL:
                res = action.actOnNumericConstant(nextTok);
                consumeToken();
                break;
            case Token.IDENTIFIER:
            {
                String id = S.token.getIdentifierInfo();
                SourceLocation loc = consumeToken();

                // primary-expression: identifier
                if (isAddressOfOperand && isPostfixExpressionSuffixStart())
                    isAddressOfOperand = false;

                // Function designators are allowed to be undeclared (C99 6.5.1p2), so we
                // need to know whether or not this identifier is a function designator or
                // not.
                res = action.actOnIdentifierExpr(getCurScope(),
                        loc,
                        id,
                        nextTokenIs(LPAREN),
                        isAddressOfOperand);
                break;
            }
            case CHARLITERAL:
                res = action.actOnCharacterConstant(nextTok);
                consumeToken();
                break;
            case STRINGLITERAL:
                res = parseStringLiteralExpression();
                break;
            case PLUSPLUS:
            case SUBSUB:
            {
                // unary-expression: '++' unary-expression [C99]
                // unary-expression: '--' unary-expression [C99]
                SourceLocation savedLoc = consumeToken();
                res = parseCastExpression(true, false, false, 0);
                if (!res.isInvalid())
                    res = action.actOnUnaryOp(savedLoc, savedKind, res.get());
                return res;
            }

            case STAR:
            case PLUS:
            case SUB:
            case TILDE:
            case BAR:
            {
                SourceLocation savedLoc = consumeToken();
                res = parseCastExpression(false, false, false, 0);
                if (!res.isInvalid())
                    res = action.actOnUnaryOp(savedLoc, savedKind, res.get());
                return res;
            }
            case SIZEOF:
                return parseUnaryExpression();
            case CHAR:
            case BOOL:
            case SHORT:
            case INT:
            case LONG:
            case SIGNED:
            case UNSIGNED:
            case FLOAT:
            case DOUBLE:
            case VOID:
            {
                diag(S.token, err_expected_expression);
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
        int kind = S.token.tag;
        return kind == LBRACKET || kind == LPAREN || kind == DOT
                || kind == SUBGT || kind == PLUSPLUS || kind == SUBSUB;
    }

    /**
     * Parse a sizeof or alignof expression.
     * <pre>
     * unary-expression:  [C99 6.5.3]
     *   'sizeof' unary-expression
     *   'sizeof' '(' type-getIdentifier ')'
     * </pre>
     * @return
     */
    private ActionResult<Expr> parseUnaryExpression()
    {
        assert nextTokenIs(SIZEOF):"Not a sizeof expression!";
        Token opTok = S.token;
        SourceLocation opLoc = consumeToken();

        return null;
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
            switch (S.token.tag)
            {
                case IDENTIFIER:
                    // fall through; this is a primary expression.
                default:
                    return lhs;
                case LBRACKET:
                {
                    // postfix-expression: p-e '[' expression ']'
                    SourceLocation lBracketLoc = consumeBracket();
                    ActionResult<Expr> idx = parseExpression();
                    SourceLocation rBracketLoc = consumeBracket();

                    if (!lhs.isInvalid() && !idx.isInvalid() && nextTokenIs(RBRACKET))
                        lhs = action.actOnArraySubscriptExpr(lhs.get(), lBracketLoc,
                                idx.get(), rBracketLoc);
                    else
                        lhs = exprError();

                    // match the ']'.
                    expectAndConsume(RBRACKET, err_expected_rsquare);
                    break;
                }
                case LPAREN:
                {
                    // p-e: p-e '(' argument-expression-list[opt] ')'
                    loc = consumeParen();
                    ArrayList<Expr> exprs = new ArrayList<>();
                    ArrayList<SourceLocation> commaLocs = new ArrayList<>();

                    if (!lhs.isInvalid())
                    {
                        if (nextTokenIsNot(RPAREN))
                            if (parseExpressionList(exprs, commaLocs))
                                lhs = exprError();
                    }

                    if (lhs.isInvalid())
                        skipUntil(RPAREN, true);
                    else if (nextTokenIsNot(RPAREN))
                        lhs = exprError();
                    else
                    {
                        assert exprs.isEmpty()
                                || exprs.size() == commaLocs.size() + 1
                                :"Unexpected number of commas!";

                        lhs = action.actOnCallExpr(lhs.get(), loc, exprs, S.token.loc);
                        // eat the ')'.
                        consumeToken();
                    }
                }
                case Token.SUBGT:
                case DOT:
                {
                    // postfix-expression: p-e '->' id-expression
                    // postfix-expression: p-e '.' id-expression
                    loc = consumeToken();
                    Ident id = (Ident)S.token;
                    SourceLocation opLoc = id.loc;
                    String name = id.name;
                    int opKind = id.tag;

                    if (!lhs.isInvalid())
                        lhs = action.actOnMemberAccessExpr(getCurScope(),lhs.get(), opLoc, opKind, name);
                    break;
                }
                case PLUSPLUS:
                case SUBSUB:
                {
                    if (!lhs.isInvalid())
                    {
                        Token tok = S.token;
                        lhs = action.actOnPostfixUnaryOp(tok.loc, tok.tag,lhs.get());
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
        assert nextTokenIs(STRINGLITERAL):"Not a string literal!";

        Token.StringLiteral str = (Token.StringLiteral)S.token;
        consumeToken();
        return action.actOnStringLiteral(str);
    }

    /**
     * Returns the precedence of specified operator token.
     * @param kind
     * @return
     */
    private int getBinOpPrecedence(int kind)
    {
        switch (kind)
        {
            default: return PrecedenceLevel.Unkonw;
            case Token.GT:
                return PrecedenceLevel.Relational;
            case Token.GTGT:
                return PrecedenceLevel.Shift;
            case Token.COMMA:
            case Token.EQ:
            case Token.STAREQ:
            case Token.SLASHEQ:
            case Token.PERCENTEQ:
            case Token.PLUSEQ:
            case Token.SUBEQ:
            case Token.LTLTEQ:
            case Token.GTGTEQ:
            case Token.AMPEQ:
            case Token.CARETEQ:
            case Token.BAREQ:
                return PrecedenceLevel.Assignment;
            case Token.QUES:
                return PrecedenceLevel.Conditional;
            case Token.BARBAR:
                return PrecedenceLevel.LogicalOr;
            case Token.AMPAMP:
                return PrecedenceLevel.LogicalAnd;
            case Token.BAR:
                return PrecedenceLevel.InclusiveOr;
            case Token.CARET:
                return PrecedenceLevel.ExclusiveOr;
            case Token.AMP:
                return PrecedenceLevel.And;
            case Token.EQEQ:
            case Token.BANGEQ:
                return PrecedenceLevel.Equality;
            case Token.LTEQ:
            case Token.LT:
            case Token.GTEQ:
                return PrecedenceLevel.Relational;
            case Token.LTLT:
                return PrecedenceLevel.Shift;
            case Token.PLUS:
            case Token.SUB:
                return PrecedenceLevel.Additive;
            case Token.PERCENT:
            case Token.STAR:
            case Token.SLASH:
                return PrecedenceLevel.Multiplicative;
            case Token.DOT:
            case Token.SUBGT:
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
        ActionResult<Expr> lhs = parseCastExpression(false, false, false, 0);

        //   An expression is potentially evaluated unless it appears where an
        //   integral constant expression is required (see 5.19) [...].
        ActionResult<Expr> res = parseRHSOfBinaryExpression(lhs, PrecedenceLevel.Conditional);
        return res;
    }

    private ActionResult<Expr> parseAssignExpression()
    {
        ActionResult<Expr> lhs = parseCastExpression(false, false, false, 1);
        return parseRHSOfBinaryExpression(lhs, PrecedenceLevel.Assignment);
    }

    /**
     * Read tokens until we get to the specified token, then consume it.
     * Because we cannot guarantee that the toke will ever occur, this skips to
     * the next token, or to some likely good stopping point.
     * IfStmt {@code stopAtSemi} is true, skipping will stop at a ';' character.
     *
     * @param tag
     * @param stopAtSemi
     */
    private boolean skipUntil(int tag, boolean stopAtSemi)
    {
        return skipUntil(new int[] { tag }, stopAtSemi, false);
    }

    private boolean skipUntil(int tag, boolean stopAtSemi, boolean dontConsume)
    {
        return skipUntil(new int[] { tag }, stopAtSemi, dontConsume);
    }

    private boolean skipUntil(int tag1, int tag2, boolean stopAtSemi)
    {
        return skipUntil(new int[] { tag1, tag2 }, stopAtSemi, false);
    }

    private boolean skipUntil(int[] tags, boolean stopAtSemi, boolean dontConsume)
    {
        boolean isFirstTokenSkipped = true;
        while (true)
        {
            for (int tag : tags)
            {
                if (nextTokenIs(tag))
                {
                    if (dontConsume)
                    {
                        // Noop, don't consume the token;
                    }
                    else
                    {
                        consumeToken();
                    }
                    return true;
                }
            }
            switch (S.token.tag)
            {
                case Tag.EOF:
                    // ran out of tokens
                    return false;
                case Tag.LPAREN:
                    consumeToken();
                    skipUntil(RPAREN, false);
                    break;
                case Tag.LBRACKET:
                    consumeToken();
                    skipUntil(RBRACKET, false);
                    break;
                case Tag.LBRACE:
                    consumeToken();
                    skipUntil(RBRACE, false);
                    break;

                case Tag.RPAREN:
                case Tag.RBRACKET:
                case Tag.RBRACE:
                    if (!isFirstTokenSkipped)
                        return false;
                    consumeToken();
                    break;
                case Tag.STRINGLITERAL:
                    consumeToken();
                    break;
                case Tag.SEMI:
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
        return S.token.tag == LPAREN || S.token.tag == RPAREN;
    }

    private boolean isTokenBracket()
    {
        return S.token.tag == LBRACKET || S.token.tag == RBRACKET;
    }

    private boolean isTokenBrace()
    {
        return S.token.tag == LBRACE || S.token.tag == RBRACE;
    }

    private boolean isTokenStringLiteral()
    {
        return S.token.tag == STRINGLITERAL;
    }

    /**
     * Consume the next token (it must is StringToken, ParenToken,
     * BraceToken, BracketToken) from Scanner.
     * @return  return the location of just consumed token.
     */
    private SourceLocation consumeToken()
    {
        assert !isTokenStringLiteral() && !isTokenParen() && !isTokenBracket() &&
                !isTokenBrace() : "Should consume special tokens with Consume*Token";
        prevTokLocation = S.token.getLocation();
        S.lex();
        return prevTokLocation;
    }

    private SourceLocation consumeAnyToken()
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
        assert isTokenParen() : "wrong consume method";
        if (S.token.tag == LPAREN)
            ++parenCount;
        else if (parenCount != 0)
            --parenCount;       // Don't let unbalanced )'s drive the count negative.
        prevTokLocation = S.token.getLocation();
        S.lex();
        return prevTokLocation;
    }
    private int bracketCount;

    private SourceLocation consumeBracket()
    {
        assert isTokenBracket() : "wrong consume method";
        if (S.token.tag == LBRACKET)
            ++bracketCount;
        else if (bracketCount != 0)
            --bracketCount;     // Don't let unbalanced ]'s drive the count negative.

        prevTokLocation = S.token.getLocation();
        S.lex();
        return prevTokLocation;
    }

    private SourceLocation consumeStringToken()
    {
        assert isTokenStringLiteral() :
                "Should only consume string literals with this method";
        prevTokLocation = S.token.getLocation();
        S.lex();
        return prevTokLocation;
    }

    private int braceCount;
    private SourceLocation prevTokLocation;

    private SourceLocation consumeBrace()
    {
        assert nextTokenIs(RBRACE) || nextTokenIs(LBRACE)
                :"Wrong consume method";
        Token tok = S.token;
        if (tok.tag == LBRACE)
            ++braceCount;
        else
            --braceCount;

        prevTokLocation = tok.getLocation();
        S.lex();
        return prevTokLocation;
    }

    private boolean expectAndConsume(int expectedTok, int diagID)
    {
        return expectAndConsume(expectedTok, diagID, "", UNKNOWN);
    }

    private boolean expectAndConsume(int expectedTok, int diagID,
            String msg, int skipToTok)
    {
        if (S.token.tag == expectedTok)
        {
            consumeAnyToken();
            return false;
        }

        diag(S.token, diagID).addTaggedVal(msg);

        if (skipToTok != UNKNOWN)
            skipUntil(skipToTok, true);
        return true;
    }

    private SourceLocation matchRHSPunctuation(int rhsTok,
            SourceLocation lhsLoc)
    {
        if (S.token.tag == rhsTok)
            return consumeAnyToken();

        SourceLocation r = S.token.getLocation();
        String lhsName = "unknown";
        int did = err_parse_error;
        switch (rhsTok)
        {
            default:break;
            case RPAREN:
                lhsName = "(";
                did = err_expected_rparen;
                break;
            case RBRACE:
                lhsName = "{";
                did = err_expected_rbrace;
                break;
            case RBRACKET:
                lhsName = "[";
                did = err_expected_rsquare;
                break;
        }

        diag(S.token, did);
        diag(lhsLoc, note_matching).addTaggedVal(lhsName);
        skipUntil(rhsTok, true);
        return r;
    }

    /**
     * Peeks ahead token and returns that token without consuming any token.
     *
     * @return
     */
    private Token peekAheadToken()
    {
        return S.nextToken();
    }

    /**
     * Checks if the next token is expected.
     *
     * @param expectedToken A TargetData token to be compared with next Token.
     * @return
     */
    private boolean nextTokenIs(int expectedToken)
    {
        return S.token.tag == expectedToken;
    }

    private boolean tokenIs(Token token, int expectedToken)
    {
        return token.tag == expectedToken;
    }

    /**
     * Returns true if the next token doesn't match expected token.
     *
     * @param expectedToken
     * @return
     */
    private boolean nextTokenIsNot(int expectedToken)
    {
        return !nextTokenIs(expectedToken);
    }

    /**
     * ReturnStmt true if the next token is keyword.
     *
     * @return
     */
    private boolean nextTokenIsKeyword()
    {
        return keywords.isKeyword(S.token);
    }
}
