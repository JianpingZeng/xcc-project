package frontend.cparser;

import frontend.ast.Tree;
import frontend.ast.Tree.*;
import frontend.cparser.DeclSpec.DeclaratorChunk;
import frontend.cparser.DeclSpec.FieldDeclarator;
import frontend.cparser.DeclSpec.ParsedSpecifiers;
import frontend.cparser.Declarator.TheContext;
import frontend.cparser.Token.Ident;
import frontend.sema.*;
import frontend.sema.Decl.LabelDecl;
import frontend.sema.Scope.ScopeFlags;
import frontend.type.QualType;
import tools.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

import static frontend.cparser.DeclSpec.SCS.SCS_typedef;
import static frontend.cparser.DeclSpec.TQ.TQ_const;
import static frontend.cparser.DeclSpec.TQ.TQ_restrict;
import static frontend.cparser.DeclSpec.TQ.TQ_volatile;
import static frontend.cparser.DeclSpec.TSC.TSC_complex;
import static frontend.cparser.DeclSpec.TSS.TSS_signed;
import static frontend.cparser.DeclSpec.TSS.TSS_unsigned;
import static frontend.cparser.DeclSpec.TST.*;
import static frontend.cparser.DeclSpec.TSW.TSW_long;
import static frontend.cparser.DeclSpec.TSW.TSW_short;
import static frontend.cparser.Parser.ParenParseOption.CastExpr;
import static frontend.cparser.Parser.ParenParseOption.CompoundLiteral;
import static frontend.cparser.Parser.ParenParseOption.SimpleExpr;
import static frontend.sema.Sema.TagUseKind.TUK_declaration;
import static frontend.sema.Sema.TagUseKind.TUK_definition;
import static frontend.sema.Sema.TagUseKind.TUK_reference;

/**
 * This is a frontend.parser for C language.
 * <p>
 * The frontend.parser map a token sequence into an abstract syntax tree. It operates by
 * recursive descent, with code derived systematically from an EBNF grammar.
 * For efficiency reasons, an operator predecessor scheme is used for parsing
 * binary operation expression, also, the special three-operation(?:) is handled
 * as binary operation for specially.
 *
 * @author Xlous.zeng
 * @version 0.1
 */
public class Parser implements Tag
{
    /**
     * A frontend.parser structure recording information about the state and
     * context of parsing.  Includes lexer information with up to two
     * tokens of look-ahead; more are not needed for C.
     */
    static class LookAheadToken
    {
        /* The look ahead token.*/
        Token[] tokens;
        /* How many look-ahead tokens are available (0, 1 or 2).*/
        short tokenAvail;
        /**
         * True if a syntax error is being recovered from; false otherwise.
         * c_parser_error sets this flag.  It should clear this flag when
         * enough tokens have been consumed to recover from the error.
         */
        boolean error;

        LookAheadToken()
        {
            tokens = new Token[2];
            tokenAvail = 0;
            error = false;
        }
    }

    static class FieldCallBack
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
                    fd.declarator.getDeclSpec().getSourceRange().getStart(),
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
         * frontend.parser self where the new scope is createed with the flags.
         * @param self
         * @param scopeFlags
         */
        ParseScope(Parser self, int scopeFlags)
        {
            this(self, scopeFlags, true);
        }

        /**
         * Construct a new ParseScope object to manage a scopein the
         * frontend.parser self where the new scope is createed with the flags.
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
    /**
     * A log for reporting error and warning information.
     */
    private Log log;

    /**
     * A buffer for storing look ahead tokens.
     */
    private LookAheadToken lookAheadToken;

    private Sema action;

    private String file;

    private Parser(String file, Context context, Sema sema)
    {
        this.file = file;
        try
        {
            init(new FileInputStream(file), context, sema);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private void init(InputStream in, Context context, Sema action)
    {
        this.S = new Scanner(in);
        this.log = Log.instance(context);
        this.action = action;
        keywords = Keywords.instance();
        log = Log.instance(context);
        lookAheadToken = new LookAheadToken();

        // initialize look ahead token buffer
        if (lookAheadToken.tokenAvail == 0)
        {
            lookAheadToken.tokens[0] = S.token;
            lookAheadToken.tokenAvail = 1;
        }
    }

    /**
     * Constructs a frontend.parser from a given scanner.
     */
    private Parser(InputStream in, Context context, Sema action)
    {
        init(in, context, action);
    }

    public static Parser instance(InputStream in, Context context, Sema action)
    {
        return new Parser(in, context, action);
    }

    /**
     * Parse a translation unit (C90 6.7, C99 6.9).
     * <p>
     * translation-unit:
     * external-declarations
     * <p>
     * external-declarations:
     * external-declaration
     * external-declarations external-declaration
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
        assert getCurScope() == null :"Scope imbalance!";
    }

    public boolean parseTopLevel(ArrayList<Decl> result)
    {
        assert result!=null;

        if (nextTokenIs(EOF))
        {
            syntaxError(peekToken().loc, "ISO C forbids an empty source file.");
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
        Token tok = peekToken();
        int pos = tok.loc;
        switch (tok.tag)
        {
            case SEMI:
            {
                syntaxError(pos,
                        "ISO C does not allow extra ';' outside of a function");
                consumeToken();
                return declGroups();
            }
            case RBRACE:
                syntaxError(pos, "expected external declaration");
                consumeToken();
                return declGroups();
            case EOF:
                syntaxError(pos, "expected external declaration");
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
                OutParamWrapper<Integer> declEnd = new OutParamWrapper<>();
                parseDeclaration(stmts, TheContext.FileContext, declEnd);
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
                return parseDeclarationOrFuncDefinition();
            }
        }
    }

    /**
     * Parse a declaration or function definition (C90 6.5, 6.7.1, C99
     * 6.7, 6.9.1).
     * declaration:
     * declaration-specifiers init-declarator-list[backend.opt] ;
     * <p>
     * function-definition:
     * declaration-specifiers[backend.opt] declarator declaration-list[backend.opt]
     * compound-statement
     * <p>
     * declaration-list:
     * declaration
     * declaration-list declaration
     * <p>
     * init-declarator-list:
     * init-declarator
     * init-declarator-list , init-declarator
     * <p>
     * init-declarator:
     * declarator simple-asm-subExpr[backend.opt] attributes[backend.opt]
     * declarator simple-asm-subExpr[backend.opt] attributes[backend.opt] = initializer
     * <p>
     * C99 requires declaration specifiers in a function definition; the
     * absence is diagnosed through the diagnosis of implicit int.  In GNU
     * C we also allow but diagnose declarations without declaration
     * specifiers, but only at top level (elsewhere they conflict with
     * other syntax).
     */
    private ArrayList<Decl> parseDeclarationOrFuncDefinition()
    {
        DeclSpec declSpecs = new DeclSpec();

        // Parse the common declaration-specifiers piece code.
        parseDeclarationSpecifiers(declSpecs);

        // C99 6.7.2.3p6: Handle "struct-or-union identifier;", "enum { X };"
        // declaration-specifiers init-declarator-list[backend.opt] ';'
        if (nextTokenIs(SEMI))
        {
            consumeToken();
            Decl theDecl = action.parseFreeStandingDeclSpec(getCurScope(), declSpecs);
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
     *         decl-specs      declarator declaration-list[backend.opt] compound-statement
     * [C90] function-definition: [C99 6.7.1] - implicit int result
     * [C90]   decl-specs[backend.opt] declarator declaration-list[backend.opt] compound-statement
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
            syntaxError(peekToken().loc, "expected function body.");
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
        Token tok = peekToken();
        assert tokenIs(tok, LBRACE);

        int lBraceLoc = tok.loc;
        ActionResult<Stmt> body = parseCompoundStatementBody(lBraceLoc);
        return null;
    }

    private ActionResult<Stmt> parseCompoundStatementBody(int startLoc)
    {
        ArrayList<Stmt> stmts = new ArrayList<>();
        for (Token tok = peekToken(); !tokenIs(tok, RBRACE) && !tokenIs(tok, EOF);)
        {
            ActionResult<Stmt> res = parseStatementOrDeclaration(stmts, false);
            stmts.add(res.get());
        }
        Token tok = peekToken();
        if (!tokenIs(tok, RBRACE))
        {
            syntaxError(tok.loc, "expected '}'");
            return null;
        }

        return new ActionResult<>(new CompoundStmt(stmts, startLoc));
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
     *         expression[backend.opt] ';'
     *
     *       jump-statement:
     *         'goto' identifier ';'
     *         'continue' ';'
     *         'break' ';'
     *         'return' expression[backend.opt] ';'
     * </pre>
     * @param stmts
     * @return
     */
    private ActionResult<Stmt> parseStatementOrDeclaration(ArrayList<Stmt> stmts,
            boolean onlyStatements)
    {
        Token tok = peekToken();
        consumeToken();
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
                    int declStart = peekToken().loc;
                    OutParamWrapper<Integer> end = new OutParamWrapper<>();
                    ArrayList<Decl> decls = parseDeclaration(stmts, TheContext.BlockContext, end);
                    int declEnd = end.get();

                    return action.actOnDeclStmt(decls, declStart, declEnd);
                }
                if (nextTokenIs(RBRACE))
                {
                    // TODO report error
                    syntaxError(peekToken().loc, "expected statement");
                    return null;
                }
                return parseExprStatement();
            }

            case CASE:
            {
                // C99 6.8.1: labeled-statement
                return parseCaseStatement();
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
                int loc = consumeToken();
                return new ActionResult<Stmt>(new Tree.NullStmt(loc));
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
        Token identTok = peekToken();
        assert tokenIs(identTok, IDENTIFIER) && ((Ident)identTok).name != null :
         "Not a valid identifier";
        consumeToken();

        Token tok = peekToken();
        assert tokenIs(tok, COLON) : "Not a label";
        int colonLoc = consumeToken();

        ActionResult<Stmt> res = parseStatement();
        if (res.isInvalid())
            return new ActionResult<Stmt>(new Tree.NullStmt(colonLoc));

        LabelDecl ld = action.lookupOrCreateLabel(((Ident)identTok).name, identTok.loc);

        return action.actOnLabelStmt(identTok.loc, ld, colonLoc, res);
    }

    /**
     * <pre>
     * labeled-statement:
     *   'case' constant-expression ':' statement
     * </pre>
     * @return
     */
    private ActionResult<Stmt> parseCaseStatement()
    {
        assert nextTokenIs(CASE) : "Not a case stmt!";

        // It is very very common for code to contain many case statements recursively
        // nested, as in (but usually without indentation):
        //  case 1:
        //    case 2:
        //      case 3:
        //         case 4:
        //           case 5: etc.
        //
        // Parsing this naively works, but is both inefficient and can cause us to run
        // out of stack space in our recursive descent frontend.parser.  As a special case,
        // flatten this recursion into an iterative loop.  This is complex and gross,
        // but all the grossness is constrained to ParseCaseStatement (and some
        // wierdness in the actions), so this is just local grossness :).

        // TopLevelCase - This is the highest level we have parsed.  'case 1' in the
        // example above.
        ActionResult<Stmt> topLevelCase = null;

        // This is the deepest statement we have parsed, which
        // gets updated each time a new case is parsed, and whose body is unset so
        // far.  When parsing 'case 4', this is the 'case 3' node.
        ActionResult<Stmt> deepestParsedCaseStmt = null;

        int colonLoc = Position.NOPOS;
        do
        {
            // consume 'case'
            int caseLoc = consumeToken();

            // parse the constant subExpr after'case'
            ActionResult<Expr> expr = parseConstantExpression();
            if (expr.isInvalid())
            {
                skipUntil(COLON, true);
                return null;
            }

            if (nextTokenIs(COLON))
            {
                colonLoc = consumeToken();
            }
            else
            {
                // TODO report error
                syntaxError(caseLoc, "expected a ':'");
            }

            ActionResult<Stmt> Case = action.actOnCaseStmt(caseLoc, expr.get(), colonLoc);
            if (Case ==null)
            {
                if (topLevelCase == null)
                    return parseStatement();
            }
            else
            {
                if (topLevelCase == null)
                    topLevelCase = Case;
                else
                {
                    assert deepestParsedCaseStmt != null;
                    action.actOnCaseStmtBody(deepestParsedCaseStmt.get(), Case.get());
                }
                deepestParsedCaseStmt = Case;
            }
            // handle all case statements
        }while(nextTokenIs(CASE));

        assert topLevelCase != null :"Should have parsed at least one case statement";

        ActionResult<Stmt> subStmt = null;
        if (nextTokenIsNot(RBRACE))
        {
            subStmt = parseStatement();
        }
        else
        {
            // TODO report error
            syntaxError(colonLoc, "label end of compound statement");
        }

        if (subStmt == null)
            subStmt = stmtError();

        // install the case body into the most deeply-nested case statement
        action.actOnCaseStmtBody(deepestParsedCaseStmt.get(), subStmt.get());

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
        int defaultLoc = consumeToken();

        int colonLoc = Position.NOPOS;
        if (nextTokenIs(COLON))
        {
            colonLoc = consumeToken();
        }
        else
        {
            // TODO report error
            syntaxError(peekToken().loc, "expected a ':'");
        }

        // diagnose the common error "switch (X) { default:}", which is not valid.
        if (nextTokenIs(RBRACE))
        {
            syntaxError(peekToken().loc, "label end of compound statement");
            return null;
        }

        ActionResult<Stmt> subStmt = parseStatement();
        if (subStmt == null)
            return null;

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
     *     { block-item-list[backend.opt] }
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
        Token tok = peekToken();
        assert tokenIs(tok, LBRACE) : "Not a compound statement!";

        // Enter a scope to hold everything within the compound stmt.
        // Compound statements can always hold declarations.
        ParseScope compoundScope = new ParseScope(this, scopeFlags);

        // parse the statements in the body.
        return parseCompoundStatementBody(tok.loc, isStmtExpr);
    }

    private ActionResult<Stmt> parseCompoundStatementBody(int startLoc, boolean isStmtExpr)
    {
        ArrayList<Stmt> stmts = new ArrayList<>();
        while (nextTokenIsNot(RBRACE) && nextTokenIsNot(EOF))
        {
            ActionResult<Stmt> res = parseStatementOrDeclaration(stmts, false);
            stmts.add(res.get());
        }
        if (nextTokenIsNot(RBRACE))
        {
            //TODO report error
            syntaxError(peekToken().loc, "not a matching '{'");
            return null;
        }
        // consume '}'
        consumeToken();

        return action.actOnCompoundStmtBody(startLoc, stmts, isStmtExpr);
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
        assert nextTokenIs(IF):"Not an if stmt!";
        int ifLoc = consumeToken(); // eat 'if'

        if (nextTokenIsNot(LPAREN))
        {
            // TODO report error
            syntaxError(peekToken().loc, "expected '(' after if");
            return stmtError();
        }
        // C99 6.8.4p3 - In C99, the if statement is a block.  This is not
        // the case for C90.
        // But we take the frontend.parser working in the C99 mode for convenience.
        ParseScope ifScope = new ParseScope(this, ScopeFlags.DeclScope.value);
        ActionResult<Expr> condExpr = null;
        ActionResult<Expr>[] res = new ActionResult[1];
        if (parseParenExpression(res, ifLoc, true))
            return  stmtError();

        condExpr = res[0];
        // C99 6.8.4p3
        // In C99, the body of the if statement is a scope, even if
        // there is only a single statement but compound statement.

        // the scope for 'then' statement if there is a '{'
        ParseScope InnerScope = new ParseScope(this,
                ScopeFlags.DeclScope.value,
                nextTokenIs(LBRACE));
        int thenStmtLoc = peekToken().loc;
        // parse the 'then' statement
        ActionResult<Stmt> thenStmt = parseStatement();

        // pop the 'then' scope if needed.
        InnerScope.exit();

        // IfStmt there is a 'else' statement, parse it.
        int elseStmtLoc = Position.NOPOS;
        int elseLoc = Position.NOPOS;
        ActionResult<Stmt> elseStmt = null;
        if (nextTokenIs(ELSE))
        {
            // eat the 'else' keyword.
            elseLoc = consumeToken();
            elseStmtLoc = peekToken().loc;

            // the scope for 'else' statement if there is a '{'
            InnerScope = new ParseScope(this,
                    ScopeFlags.DeclScope.value,
                    nextTokenIs(LBRACE));

            elseStmt = parseStatement();

            // Pop 'else' statement scope if needed.
            InnerScope.exit();
        }

        ifScope.exit();
        // IfStmt the condition expression is invalid and then return null as result.
        if (condExpr.isInvalid())
            return stmtError();

        if (thenStmt.isInvalid() && elseStmt.isInvalid()
                || thenStmt.isInvalid() && elseStmt.get() == null
                || thenStmt.get() == null && elseStmt.isInvalid())
        {
            // Both invalid, or one is invalid other is non-present
            return stmtError();
        }

        // Now if both are invalid, replace with a ';'
        if (thenStmt.isInvalid())
            thenStmt = new ActionResult<Stmt>(new Tree.NullStmt(thenStmtLoc), true);
        if (elseStmt.isInvalid())
            elseStmt = new ActionResult<Stmt>(new Tree.NullStmt(elseStmtLoc), true);

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

    /**
     * This method parrse the unit that starts with a token "(",
     * based on what is allowed by {@code exprType}. The actual thing parsed is
     * returned in {@code exprType}. If {@code stopIfCastExpr} is true, it will
     * only return the parsed frontend.type, not the cast-expression.
     * <pre>
     * primary-expression: [C99 6.5.1]
     *   '(' expression ')'
     *
     * postfix-expression: [C99 6.5.2]
     * //TODO  '(' frontend.type-name ')' '{' initializer-list '}'
     * //TODO  '(' frontend.type-name ')' '{' initializer-list ',' '}'
     *   '(' frontend.type-name ')' cast-expression
     * </pre>
     * @param exprType
     * @param stopIfCastExpr
     * @param isTypeCast
     * @param castTy
     * @param rParenLoc
     * @return
     */
    private ActionResult<Expr> parseParenExpression(
            OutParamWrapper<ParenParseOption> exprType,
            boolean stopIfCastExpr,
            boolean isTypeCast,
            OutParamWrapper<QualType> castTy,
            OutParamWrapper<Integer> rParenLoc)
    {
        assert nextTokenIs(LPAREN):"Not a paren expression.";
        // eat the '('.
        int lParenLoc = consumeToken();
        ActionResult<Expr> result = new ActionResult<>(true);

        if (exprType.get().ordinal()>= CompoundLiteral.ordinal())
        {
            // This is a compound literal expression or cast expression.

            // First of all, parse declarator.
            DeclSpec ds = new DeclSpec();
            parseSpecifierQualifierList(ds);
            Declarator declaratorInfo = new Declarator(ds, TheContext.TypeNameContext);
            parseDeclarator(declaratorInfo);

            int rparen = expect(RPAREN);
            if (nextTokenIs(LBRACE))
            {
                // eat the '{'.
                consumeToken();
                exprType.set(CompoundLiteral);
                // TODO handle compound literal 2016.10.18.
            }
            else if (exprType.get() == CastExpr)
            {
                // We parsed '(' frontend.type-name ')' and the thing after it wasn't a '('.
                if (declaratorInfo.isInvalidType())
                    return exprError();

                // Note that this doesn't parse the subsequent cast-expression, it just
                // returns the parsed frontend.type to the callee.
                if (stopIfCastExpr)
                {
                    ActionResult<QualType> ty = action.actOnTypeName(getCurScope(), declaratorInfo);
                    castTy.set(ty.get());
                    return new ActionResult<Expr>();
                }

                // Parse the cast-expression that follows it next.
                result = parseCastExpression(
                        /*isUnaryExpression*/false,
                        /*isAddressOfOperands*/false,
                        /*isTypeCast*/true,
                        /*placeHolder*/0);
                if (!result.isInvalid())
                {
                    result = action.actOnCastExpr(getCurScope(), lParenLoc,
                            declaratorInfo, castTy, rParenLoc, result.get());
                }
                return result;
            }
        }
        else if (isTypeCast)
        {
            ArrayList<Expr> exprs = new ArrayList<>();
            ArrayList<Integer> commaLocs = new ArrayList<>();

            if (!parseExpressionList(exprs, commaLocs))
            {
                exprType.set(SimpleExpr);
                result = action.actOnParenOrParenList(
                        rParenLoc.get(),
                        peekToken().loc,
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
                int rparen = peekToken().loc;
                result = action.actOnParenExpr(rParenLoc.get(), rparen, result.get());
            }
        }

        int rparen = expect(RPAREN);
        rParenLoc.set(rparen);
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
            ArrayList<Integer> commaLocs)
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
    private boolean parseParenExpression(ActionResult<Expr>[] cond,
            int loc,
            boolean convertToBoolean)
    {
        assert cond != null && cond.length == 1;
        assert nextTokenIs(LPAREN);
        int lparenLoc = consumeToken();
        cond[0] = parseExpression();
        if (!cond[0].isInvalid() && convertToBoolean)
        {
            //TODO convert the condition expression to boolean
        }

        if (cond[0].isInvalid() && nextTokenIsNot(RPAREN))
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
     * For sufficiency, A simple precedence-based frontend.parser for binary/unary operators.
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
        Token tok = peekToken();
        assert tokenIs(tok, SWITCH) :"Not a switch statement?";
        // eat the 'switch'
        int switchLoc = consumeToken();

        if (nextTokenIsNot(LBRACE))
        {
            syntaxError(peekToken().loc, "expected '{' after switch");
            return stmtError();
        }
        // C99 6.8.4p3. In C99, the switch statements is a block. This is not
        // implemented in C90. So, just take C99 into consideration for convenience.
        ParseScope switchScope = new ParseScope(this, ScopeFlags.BlockScope.value
        | ScopeFlags.SwitchScope.value | ScopeFlags.DeclScope.value |
        ScopeFlags.ControlScope.value);

        // Parse the condition exprsesion.
        ActionResult<Expr> condExpr = null;
        ActionResult<Expr>[] res = new ActionResult[1];
        if (parseParenExpression(res, switchLoc, false))
        {
            return stmtError();
        }
        condExpr = res[0];

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
                nextTokenIs(LBRACE));

        // read the body statement
        ActionResult<Stmt> body = parseStatement();

        // pop innerScope
        innerScope.exit();
        switchScope.exit();

        if (body.isInvalid())
            body = new ActionResult<Stmt>(new Tree.NullStmt(switchLoc));

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
        Token tok = peekToken();
        assert tokenIs(tok, WHILE) :"Not a while statement!";
        int whileLoc = tok.loc;
        consumeToken();   // eat the 'while'

        if (nextTokenIsNot(LPAREN))
        {
            syntaxError(whileLoc, "expected '(' after ", "while");
            return stmtError();
        }

        // C99 6.8.5p5 - In C99, the while statement is a block.  This is not
        // the case for C90.  Start the loop scope.
        int scopeFlags = ScopeFlags.BlockScope.value
                | ScopeFlags.ContinueScope.value
                | ScopeFlags.ControlScope.value
                | ScopeFlags.DeclScope.value;

        ParseScope whileScope = new ParseScope(this, scopeFlags);
        ActionResult<Expr>[] wrapper = new ActionResult[1];

        // parse the condition.
        if (parseParenExpression(wrapper, whileLoc, true))
            return stmtError();

        ActionResult<Expr> cond = wrapper[0];

        // C99 6.8.5p5 - In C99, the body of the if statement is a scope, even if
        // there is no compound stmt.  C90 does not have this clause.  We only do this
        // if the body isn't a compound statement to avoid push/pop in common cases.
        ParseScope innerScope = new ParseScope(this, ScopeFlags.DeclScope.value,
                nextTokenIs(LBRACE));
        // parse the body of while stmt.
        ActionResult<Stmt> body = parseStatement();

        // Pop the body scope if needed
        innerScope.exit();
        whileScope.exit();

        if (cond.isInvalid() && body.isInvalid())
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
        Token tok = peekToken();
        assert tokenIs(tok, DO):"Not a do stmt!";
        int doLoc = tok.loc;

        int scopeFlags = ScopeFlags.BreakScope.value|
                ScopeFlags.ContinueScope.value | ScopeFlags.DeclScope.value;

        ParseScope doScope = new ParseScope(this, scopeFlags);

        ParseScope innerScope = new ParseScope(this,
                ScopeFlags.DeclScope.value,
                nextTokenIs(LBRACE));

        ActionResult<Stmt> body = parseStatement();
        // Pop the body scope.
        innerScope.exit();

        if (nextTokenIsNot(WHILE))
        {
            if (!body.isInvalid())
            {
                syntaxError(peekToken().loc, "expected while");
                syntaxError(doLoc, "note matching", "do");
                skipUntil(SEMI, true);
            }
            return stmtError();
        }

        int whileLoc = consumeToken();

        if (nextTokenIsNot(LPAREN))
        {
            syntaxError(peekToken().loc, "expected '(' after", "do/while");
            skipUntil(SEMI, true);
            return stmtError();
        }

        int lParenLoc = consumeToken();  // eat '('.
        ActionResult<Expr> cond = parseExpression();
        int rParenLoc = consumeToken();  // eat ')'.
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
     *   'for' '(' expr[backend.opt] ';' expr[backend.opt] ';' expr[backend.opt] ')' statement
     *   'for' '(' declaration expr[backend.opt] ';' expr[backend.opt] ')' statement
     * </pre>
     * @return
     */
    private ActionResult<Stmt> parseForStatement()
    {
        Token tok = peekToken();
        assert tokenIs(tok, FOR):"Not a for loop";
        int forLoc = consumeToken();

        if (nextTokenIsNot(LPAREN))
        {
            syntaxError(peekToken().loc, "expected '(' after", "for");
            skipUntil(SEMI, true);
            return stmtError();
        }

        int scopeFlags = ScopeFlags.BreakScope.value
                | ScopeFlags.ContinueScope.value
                | ScopeFlags.DeclScope.value
                | ScopeFlags.ControlScope.value;

        ParseScope forScope = new ParseScope(this, scopeFlags);
        int lParenLoc = consumeToken();

        ActionResult<Expr> value;
        ActionResult<Stmt> firstPart = new ActionResult<Stmt>();
        ActionResult<Expr> secondPart = new ActionResult<Expr>();
        boolean secondPartIsInvalid = false;

        // parse the first part
        if (nextTokenIs(SEMI))
        {
            // for (;
            consumeToken();
        }
        else if (isSimpleDeclaration())
        {
            // parse the declaration, for (int X = 4;
            int declStart = peekToken().loc;
            ArrayList<Stmt> stmts = new ArrayList<>(32);

            ArrayList<Decl> declGroup = parseSimpleDeclaration(stmts, TheContext.ForContext,
                    false);
            firstPart = action.actOnDeclStmt(declGroup, declStart, peekToken().loc);
            if (nextTokenIs(SEMI))
            {
                consumeToken();
            }
            else
            {
                syntaxError(peekToken().loc, "expected ';'");
            }
        }
        else
        {
            value = parseExpression();
            if (!value.isInvalid())
                firstPart = action.actOnExprStmt(value);

            if (nextTokenIs(SEMI))
                consumeToken();
            else
            {
                if (!value.isInvalid())
                    syntaxError(peekToken().loc, "expected ';'");
                else
                {
                    skipUntil(RPAREN, true);
                    if (nextTokenIs(SEMI))
                        consumeToken();
                }
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
                syntaxError(peekToken().loc, "expected a ';'");
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

        tok = peekToken();
        if (!tokenIs(tok, RPAREN))
        {
            syntaxError(peekToken().loc, "expected a ')'");
            skipUntil(RBRACE, true);
        }

        int rParenLoc = tok.loc;

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
     *   declaration-specifiers init-declarator-list[backend.opt] ';'
     *
     *   declaration-specifiers:
     *     storage-class-specifier declaration-specifiers
     *     frontend.type-specifier declaration-specifiers
     *     frontend.type-qualifier declaration-specifiers
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
        // declaration-specifiers init-declarator-list[backend.opt] ';'
        if (nextTokenIs(SEMI))
        {
            if (requiredSemi) consumeToken();
            Decl decl = action.parseFreeStandingDeclSpec(getCurScope(), ds);

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
                    syntaxError(peekToken().loc,
                            "function definition declared 'typedef'");

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
            }
            else
            {
                syntaxError(peekToken().loc, "expected function body after function declarator");
                skipUntil(SEMI, true);
                return declGroups();
            }
        }

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

        // eat the last ';'.
        consumeToken();

        return action.finalizeDeclaratorGroup(ds, declsInGroup);
    }

    /**
     * Parse 'declaration' after parsing 'declaration-specifiers
     * declarator'. This method parses the remainder of the declaration
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
            return parseAssignmentExpression();
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

        int lBraceLoc = consumeToken(); // eat '{'
        ArrayList<Expr> initExpr = new ArrayList<>();

        if (nextTokenIs(RBRACE))
        {
            int rBraceLoc = consumeToken();
            syntaxError(lBraceLoc, "use of GNU empty initializer extension");
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
        int rBraceLoc = consumeToken();
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

    private ActionResult<Expr> parseBraceInitializer()
    {
        ActionResult<Expr> lhs = parseCastExpression(
                /*isUnaryExpression=*/false, false, false, 0);
        return parseRHSOfBinaryExpression(lhs, PrecedenceLevel.Assignment);
    }

    private ActionResult<Stmt> parseGotoStatement()
    {
        Token tok = peekToken();
        assert tokenIs(tok, GOTO):"Not a goto stmt!";
        int gotoLoc = consumeToken(); // eat the 'goto'

        // 'goto label'.
        ActionResult<Stmt> res = null;
        if (nextTokenIs(IDENTIFIER))
        {
            Ident id = (Ident)peekToken();
            LabelDecl ld = action.lookupOrCreateLabel(id.name, id.loc);
            res = action.actOnGotoStmt(gotoLoc, id.loc, ld);
            consumeToken();
        }
        else
        {
            // erroreous case
            syntaxError(peekToken().loc, "expected a ", "identifier");
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
        int continueLoc = consumeToken();
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
        int breakLoc = consumeToken();
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
        Token tok = peekToken();
        assert tokenIs(tok, RETURN):"Not a return stmt!";
        int returnLoc = consumeToken();

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
            TheContext dc, OutParamWrapper<Integer> declEnd)
    {
        assert declEnd != null;

        ArrayList<Decl> res = parseSimpleDeclaration(stmts, dc, false);
        if (nextTokenIsNot(SEMI))
        {
            syntaxError(peekToken().loc, "expected a ';'");
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
        Token oldTok = peekToken();

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
            syntaxError(oldTok.loc, "expected 'case' keyword before expression");

            // Recover parsing as a case statement.
            return parseCaseStatement();
        }
        accept(SEMI);

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
        Token tok = peekToken();
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
     *   storage-class-specifier declaration-specifiers[backend.opt]
     *   frontend.type-specifier declaration-specifiers[backend.opt]
     *   frontend.type-qualifier declaration-specifiers[backend.opt]
     *   function-specifier declaration-specifiers[backend.opt]
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
     * frontend.type-specifier:
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
     *   typedef-name
     *
     * (_Bool and _Complex are new in C99.)
     *
     * C90 6.5.3, C99 6.7.3:
     *
     * frontend.type-qualifier:
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
            int loc = peekToken().loc;
            declSpecs.setRangeStart(loc);
            declSpecs.setRangeEnd(loc);
        }

        out:
        while (nextTokenIs(IDENTIFIER) || nextTokenIsKeyword())
        {
            Token tok = peekToken();
            int loc = tok.loc;
            boolean isInvalid = false;
            String prevSpec = null;
            String diag = null;

            switch (tok.tag)
            {
                case IDENTIFIER:
                {
                    // This identifier can only be a typedef name if we haven't
                    // already seen a frontend.type-specifier.
                    if (declSpecs.hasTypeSpecifier())
                        break out;

                    // So, the current token is a typedef name or error.
                    QualType type = action.getTypeByName((Ident) tok, tok.loc,
                                    getCurScope());
                    if (type == null)
                    {
                        if (parseImplicitInt(declSpecs))
                            continue;
                        break out;
                    }

                    OutParamWrapper<String> wrapper1 = new OutParamWrapper();
                    OutParamWrapper<String> wrapper2 = new OutParamWrapper();

                    isInvalid = declSpecs.setTypeSpecType(TST_typename, loc,
                            wrapper1, wrapper2, type);
                    prevSpec = wrapper1.get();
                    diag = wrapper2.get();

                    if (isInvalid)
                        break;

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

                // frontend.type-specifier
                case CHAR:
                {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper();
                    OutParamWrapper<String> wrapper2 = new OutParamWrapper();
                    isInvalid = declSpecs.setTypeSpecType(TST_char, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diag = wrapper2.get();
                    break;
                }
                case INT:
                {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper();
                    OutParamWrapper<String> wrapper2 = new OutParamWrapper();
                    isInvalid = declSpecs.setTypeSpecType(TST_int, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diag = wrapper2.get();
                    break;
                }
                case FLOAT:
                {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper();
                    OutParamWrapper<String> wrapper2 = new OutParamWrapper();
                    isInvalid = declSpecs.setTypeSpecType(TST_float, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diag = wrapper2.get();
                    break;
                }
                case DOUBLE:
                {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper();
                    OutParamWrapper<String> wrapper2 = new OutParamWrapper();
                    isInvalid = declSpecs.setTypeSpecType(TST_double, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diag = wrapper2.get();
                    break;
                }
                case VOID:
                {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper();
                    OutParamWrapper<String> wrapper2 = new OutParamWrapper();
                    isInvalid = declSpecs.setTypeSpecType(TST_void, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diag = wrapper2.get();
                    break;
                }
                case BOOL:       {
                    OutParamWrapper<String> wrapper1 = new OutParamWrapper();
                    OutParamWrapper<String> wrapper2 = new OutParamWrapper();
                    isInvalid = declSpecs.setTypeSpecType(TST_bool, loc, wrapper1, wrapper2);
                    prevSpec = wrapper1.get();
                    diag = wrapper2.get();
                    break;
                }
                // enum specifier
                case ENUM:
                {
                    loc = consumeToken();
                    parseEnumSpecifier(loc, declSpecs);
                    continue;
                }
                // struct-union specifier
                case STRUCT:
                case UNION:
                {
                    // eat the 'struct' or 'union'.
                    loc = consumeToken();
                    parseStructOrUnionSpecifier(loc, declSpecs);
                    continue;
                }
                // frontend.type-qualifiers
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

            }
            declSpecs.setRangeEnd(peekToken().loc);
            consumeToken();
        }
        // finish and return to caller
        declSpecs.finish(this);
    }

    /**
     * This method is called when we have an non-typename
     * identifier in a declspec (which normally terminates the decl spec) when
     * the declspec has no frontend.type specifier.  In this case, the declspec is either
     * malformed or is "implicit int" (in K&R and C89).
     *
     * @param ds
     * @return
     */
    private boolean parseImplicitInt(DeclSpec ds)
    {
        assert nextTokenIs(IDENTIFIER) : "should have identifier.";
        int loc = consumeToken();

        // IfStmt we see an identifier that is not a frontend.type name, we normally would
        // parse it as the identifer being declared.  However, when a typename
        // is typo'd or the definition is not included, this will incorrectly
        // parse the typename as the identifier name and fall over misparsing
        // later parts of the diagnostic.
        //
        // As such, we try to do some look-ahead in cases where this would
        // otherwise be an "implicit-int" case to see if this is invalid.  For
        // example: "static foo_t x = 4;"  In this case, if we parsed foo_t as
        // an identifier with implicit int, we'd get a parse error because the
        // next token is obviously invalid for a frontend.type.  Parse these as a case
        // with an invalid frontend.type specifier.
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

        OutParamWrapper<String> prevSpec = new OutParamWrapper<>();
        OutParamWrapper<String> diag = new OutParamWrapper<>();
        // mark as an error
        ds.setTypeSpecType(TST_error, loc, prevSpec, diag);
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
     *   enum identifier[backend.opt] { enumerator-list }
     *   enum identifier[backend.opt] { enumerator-list , } [C99]
     *   enum identifier
     * </pre>
     * @param startLoc
     * @param ds
     */
    private void parseEnumSpecifier(int startLoc, DeclSpec ds)
    {
        Token tok = peekToken();
        int kwLoc = tok.loc;

        // Must have either 'enum name' or 'enum {...}'.
        if (!tokenIs(tok, IDENTIFIER) && !tokenIs(tok, LBRACE))
        {
            // TODO report error
            syntaxError(tok.loc, "expected an identifier or '{'");
            // skip the rest of this declarator, up until a ',' or ';' encounter.
            skipUntil(COMMA, true);
            return;
        }

        String name = null;
        int nameLoc = Position.NOPOS;
        if (tokenIs(tok, IDENTIFIER))
        {
            name = ((Ident)tok).name;
            nameLoc = tok.loc;
        }

        // There are three options here.  IfStmt we have 'enum foo;', then this is a
        // forward declaration.  IfStmt we have 'enum foo {...' then this is a
        // definition. Otherwise we have something like 'enum foo xyz', a reference.
        Sema.TagUseKind tuk;
        if (tokenIs(tok, LBRACE))
            tuk = TUK_definition;
        else if (tokenIs(tok, SEMI))
            tuk = TUK_declaration;
        else
            tuk = TUK_reference;

        if (nameLoc == Position.NOPOS && tuk != TUK_definition)
        {
            // TODO report error
            syntaxError(tok.loc, "expected a identifier before ';'");
            skipUntil(COMMA, true);
            return;
        }

        ActionResult<Decl> tagDecl = action.actOnTag(getCurScope(),
                TST_enum, tuk,
                startLoc, name,
                nameLoc,
                kwLoc);

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
        OutParamWrapper<String> diagWrapper = new OutParamWrapper<>();

        if (ds.setTypeSpecType(TST_enum,
                startLoc,
                nameLoc != Position.NOPOS ? nameLoc : startLoc,
                prevSpecWrapper, diagWrapper,
                tagDecl.get()))
        {
            // TODO report error

        }
    }

    /**
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
    private void parseEnumBody(int startLoc, Decl enumDecl)
    {
        ParseScope enumScope = new ParseScope(this, ScopeFlags.DeclScope.value);
        action.actOnTagStartDefinition(getCurScope(), enumDecl);

        // eat '{'
        int lBraceLoc = consumeToken();
        if (nextTokenIs(RBRACE))
        {
            syntaxError(peekToken().loc, "empty enum.");
        }
        Token tok = peekToken();

        ArrayList<Decl> enumConstantDecls = new ArrayList<>(32);
        Decl lastEnumConstDecl = null;
        while(tokenIs(tok, IDENTIFIER))
        {
            String name = ((Ident)tok).name;
            int identLoc = consumeToken();

            int equalLoc = Position.NOPOS;
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

            if (nextTokenIs(IDENTIFIER))
            {
                // we missing a ',' between enumerators
                syntaxError(peekToken().loc, "enumerator list missing a ,'");
                continue;
            }

            // finish enumerator-list
            if (nextTokenIsNot(COMMA))
                break;
            int commaLoc = consumeToken();

            if (nextTokenIs(RBRACE))
                break;
        }

        // eat the '}'
       int rBraceLoc = consumeToken();

        action.actOnEnumBody(
                startLoc,
                lBraceLoc,
                rBraceLoc,
                enumDecl,
                enumConstantDecls,
                getCurScope());

        enumScope.exit();
        action.actOnTagStartDefinition(getCurScope(), enumDecl);
    }

    /**
     * Parses C structs or unions specifiers.
     * <pre>
     *     struct-or-union-specifier: [C99 6.7.2.1]
     *       struct-or-union identifier[backend.opt] '{' struct-contents '}'
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
     * @param loc
     * @param ds
     */
    private void parseStructOrUnionSpecifier(int loc, DeclSpec ds)
    {
        DeclSpec.TST tagType;
        int kind = peekToken().tag;
        int startLoc = peekToken().loc;
        if (kind == STRUCT)
            tagType = TST_struct;
        else
        {
            assert kind == UNION : "Not a union specifier";
            tagType = TST_union;
        }

        // eat the 'struct' or 'union'.
        int kwLoc = consumeToken();

        // There are four cases for the grammar starting with keyword class-specifier.
        // 1. forward declaration: for example, struct X;
        // 2. definition: for example, struct X {...};
        // 3. annoymous definition of struct: struct {...};
        // 4. reference used in varaible declaration: struct X x;
        if (nextTokenIs(IDENTIFIER))
        {
            Token tok = peekToken();
            String name = ((Ident) tok).name;
            int nameLoc = tok.loc;

            // eat the name = 'identifier'.
            consumeToken();

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
            consumeToken();

            if (name == null && (ds.getTypeSpecType() == TST_error
                    || tuk != TUK_definition))
            {
                if (ds.getTypeSpecType() != TST_error)
                {
                    // we have a declaration or reference to an anonymous struct.
                    syntaxError(startLoc,
                            "declaration of anonymous '%s' must be a definition",
                            DeclSpec.getSpecifierName(tagType));
                }
                skipUntil(COMMA, true);
                return;
            }

            // create the tag portion of the struct or union.

            // declaration or definitions of a struct or union frontend.type
            ActionResult<Decl> tagOrTempResult = action.actOnTag(
                    getCurScope(), tagType,
                    tuk, startLoc, name, nameLoc, kwLoc);
            // if there is a body, parse it and perform actions
            if (tuk == TUK_definition)
            {
                assert nextTokenIs(LBRACE);
                parseStructOrUnionBody(startLoc, tagType, tagOrTempResult.get());
            }

            String prevSpec = null;
            String diag = null;
            boolean result;
            if (!tagOrTempResult.isInvalid())
            {
                OutParamWrapper<String> w1 = new OutParamWrapper<>();
                OutParamWrapper<String> w2 = new OutParamWrapper<>();

                result = ds.setTypeSpecType(tagType, startLoc,
                        nameLoc!=Position.NOPOS?nameLoc: startLoc,
                        w1, w2,
                        tagOrTempResult.get());
            }
            else
            {
                ds.setTypeSpecError();
                return;
            }

            if (result)
            {
                // report error
                syntaxError(startLoc, diag, prevSpec);
            }

            if (tuk == TUK_definition)
            {
                boolean expectedSemi = true;
                switch (peekToken().tag)
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
                    // frontend.type-specifier
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
                        // As shown above, frontend.type qualifiers and storage class specifiers absolutely
                        // can occur after class specifiers according to the grammar.  However,
                        // almost no one actually writes code like this.  IfStmt we see one of these,
                        // it is much more likely that someone missed a semi colon and the
                        // frontend.type/storage class specifier we're seeing is part of the *next*
                        // intended declaration, as in:
                        //
                        //   struct foo { ... }
                        //   typedef int X;
                        //
                        // We'd really like to emit a missing semicolon error instead of emitting
                        // an error on the 'int' saying that you can't have two frontend.type specifiers in
                        // the same declaration of X.  Because of this, we look ahead past this
                        // token to see if it's a frontend.type specifier.  IfStmt so, we know the code is
                        // otherwise invalid, so we can produce the expected semi error.
                        if (isKnownBeTypeSpecifier(peekToken()))
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
                        syntaxError(peekToken().loc,
                                "expected a ';' after tag declaration %s",
                                tagType == TST_union ? "union":"struct");
                        skipUntil(SEMI, true);
                    }
                }
            }
        }
    }

    /**
     * ReturnStmt true if we know that the specified token
     * is definitely a frontend.type-specifier.  ReturnStmt false if it isn't part of a frontend.type
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
     *  declarator[backend.opt] : constant-expression
     *
     *
     * @param recordLoc
     * @param tagType
     */
    private void parseStructOrUnionBody(int recordLoc, DeclSpec.TST tagType,
            Decl tagDecl)
    {
        // expect a '{'.
        int lBraceLoc = expect(LBRACE);

        // obtains the struct or union specifier name.
        String structOrUnion = DeclSpec.getSpecifierName(tagType);

        ParseScope structScope = new ParseScope(this,
        ScopeFlags.ClassScope.value
                | ScopeFlags.DeclScope.value);
        action.actOnTagStartDefinition(getCurScope(), tagDecl);

        // Empty structs are an extension in C (C99 6.7.2.1p7)
        if (nextTokenIs(RBRACE))
        {
            consumeToken();
            syntaxWarning(lBraceLoc, "empty %s is a GNU extension", structOrUnion);
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
                syntaxWarning(peekToken().loc,
                        "extra semicolon in struct or union specified");
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
                syntaxError(peekToken().loc, "expected a ';' at the end.");
                break;
            }
            else
            {
                syntaxError(peekToken().loc, "expected a ';' at the end.");
                skipUntil(RBRACE, true);
                // if we stopped at a ';', consume it.
                if (nextTokenIs(SEMI))
                    consumeToken();
            }
        }

        int rBraceLoc = consumeToken();
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
     *     frontend.type-specifier specifier-qualifier-list[backend.opt]
     *     frontend.type-qualifier specifier-qualifier-list[backend.opt]
     *
     *   struct-declarator-list:
     *     struct-declarator
     *     struct-declarator-list , struct-declarator
     *
     *   struct-declarator:
     *     declarator
     *     declarator[backend.opt] : constant-expression
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
            action.parseFreeStandingDeclSpec(getCurScope(), ds);
            return;
        }
        // read struct-declarators until we find a ';'.
        while (true)
        {
            FieldDeclarator declaratorField = new FieldDeclarator(ds);

            // struct-declarator:
            //   declarator
            //   declarator[backend.opt] : constant-expression
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
     *   pointer[backend.opt] direct-declarator
     * <p>
     * <p>
     * pointer:
     *   '*' frontend.type-qualifier-list[backend.opt]
     *   '*' frontend.type-qualifier-list[backend.opt] pointer
     *
     * @param declarator
     */
    private void parseDeclarator(Declarator declarator)
    {
        Token tok = peekToken();
        consumeToken();
        declarator.setRangeEnd(tok.loc);

        // first, parse pointer
        if (tokenIs(tok, STAR))
        {
            DeclSpec ds = new DeclSpec();
            parseTypeQualifierListOpt(ds);
            declarator.extendWithDeclSpec(ds);

            // recursively parse remained piece
            parseDeclarator(declarator);

            // remember this pointer frontend.type and add it into declarator's frontend.type list.
            declarator.addTypeInfo(DeclaratorChunk.getPointer
                    (ds.getTypeQualifier(),
                            tok.loc,
                            ds.getConstSpecLoc(),
                            ds.getVolatileSpecLoc(),
                            ds.getRestrictSpecLoc()),
                    ds.getRangeEnd());
        }
        else
        {
            // parse direct-declartor
            parseDirectDeclarator(declarator);
        }
    }

    /**
     * <p>
     * direct-declarator:
     *   identifier
     *   '(' declarator ')'
     *   direct-declarator array-declarator
     *   direct-declarator ( parameter-frontend.type-list )
     *   direct-declarator ( identifier-list[backend.opt] )

     * frontend.type-qualifier-list:
     *   frontend.type-qualifier
     *   frontend.type-qualifier-list frontend.type-qualifier
     * <p>
     * parameter-frontend.type-list:
     *   parameter-list
     *   parameter-list , ...
     * <p>
     * parameter-list:
     *   parameter-declaration
     *   parameter-list , parameter-declaration
     * <p>
     * parameter-declaration:
     *   declaration-specifiers declarator
     *   declaration-specifiers abstract-declarator[backend.opt]
     * <p>
     * identifier-list:
     *   identifier
     *   identifier-list , identifier
     * <p>
     * abstract-declarator:
     *   pointer
     *   pointer[backend.opt] direct-abstract-declarator
     * <p>
     * direct-abstract-declarator:
     *   ( abstract-declarator )
     *   direct-abstract-declarator[backend.opt] array-declarator
     *   direct-abstract-declarator[backend.opt] ( parameter-frontend.type-list[backend.opt] )
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
            Ident id = ((Ident)peekToken());
            assert id.name != null : "Not an identifier?";
            declarator.setIdentifier(id, id.loc);
            consumeToken();
        }
        else if (nextTokenIs(LPAREN))
        {
            // direct-declarator:
            //   '(' declarator ')'
            // e.g. char (*X) or int (*XX)(void)

            consumeToken(); // eat the '('.
            parseParenDeclarator(declarator);
        }
        else if (declarator.mayOmitIdentifier())
        {
            // This could be something simple like "int" (in which case the declarator
            // portion is empty), if an abstract-declarator is allowed.
            declarator.setIdentifier(null, peekToken().loc);
        }
        else
        {
            // TODO error report
            if (declarator.getContext() == TheContext.StructFieldContext)
                syntaxError(declarator.getDeclSpec().getRangeStart(),
                        "Expected member name or ';'");
            else
                accept(LPAREN);

            declarator.setIdentifier(null, peekToken().loc);
            declarator.setInvalidType(true);
        }

        assert declarator.isPastIdentifier()
                :"Haven't past the location of the identifier yet?";

        while (true)
        {
            Token tok = peekToken();
            if (tokenIs(tok, LPAREN))
                parseFunctionDeclarator(declarator);
            else if (tokenIs(tok, LBRACKET))
                parseBracketDeclarator(declarator);
            else
                break;
        }
    }

    private boolean isDeclarationSpecifier(boolean disambiguatingWithExpression)
    {
        switch (peekToken().tag)
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

            // typedefs-name
            case ANN_TYPENAME:
                return !disambiguatingWithExpression;
        }
    }

    private boolean isDeclarationSpecifier()
    {
        switch (peekToken().tag)
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
     *   direct-declarator '(' parameter-frontend.type-list ')'
     *   direct-declarator '(' identifier-list[backend.opt] ')'
     * @param declarator
     */
    private void parseParenDeclarator(Declarator declarator)
    {
        // eat the '('.
        consumeToken();

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
            isGrouping = !(nextTokenIs(RPAREN)        // 'int()' is a function.
                    || isDeclarationSpecifier());
        // If this is a grouping paren, handle:
        // direct-declarator: '(' declarator ')'
        if (isGrouping)
        {
            parseDeclarator(declarator);

            // match ')'
            consumeToken();
            return;
        }

        // Okay, if this wasn't a grouping paren, it must be the start of a function
        // argument list.  Recognize that this declarator will never have an
        // identifier (and remember where it would have been), then call into
        // ParseFunctionDeclarator to handle of argument list.
        declarator.setIdentifier(null, peekToken().loc);
        consumeToken();
        parseFunctionDeclarator(declarator);
    }

    private void parseFunctionDeclarator(Declarator declarator)
    {
        assert declarator.isPastIdentifier():"Should not call before identifier!";

        // eat '('
        int startLoc = consumeToken();
        // this should be true when the function has typed arguments.
        // Otherwise, it will be treated as K&R style function.
        boolean hasProto = false;

        // build an array of information about the parsed arguments.
        ArrayList<DeclSpec.ParamInfo> paramInfos = new ArrayList<>(8);
        // The the location where we see an ellipsis.
        int ellipsisLoc = Position.NOPOS;
        DeclSpec ds = new DeclSpec();

        int endLoc;
        if (isFunctionDeclaratorIdentifierList())
        {
            // Parses the function declaration of K&R form.
            parseFunctionDeclaratorIdentifierList(declarator, paramInfos);

            endLoc = peekToken().loc;
            // eat the ')'
            consumeToken();
        }
        else
        {
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
            endLoc = consumeToken();

            // leave prototype scope
            protoTypeScope.exit();
        }

        // TODO: remenber that we parsed a function frontend.type
        declarator.addTypeInfo(DeclaratorChunk.getFunction(hasProto,
                ellipsisLoc != Position.NOPOS,
                ellipsisLoc,
                paramInfos,
                ds.getTypeQualifier(),
                startLoc,
                endLoc), endLoc);
    }

    private boolean isFunctionDeclaratorIdentifierList()
    {
        return nextTokenIs(IDENTIFIER) && nextTokenIsNot(ANN_TYPENAME)
                    && (tokenIs(peekAheadToken(), COMMA) || tokenIs(peekAheadToken(), RPAREN));
    }

    private void parseFunctionDeclaratorIdentifierList(
            Declarator declarator,
            ArrayList<DeclSpec.ParamInfo> paramInfos)
    {
        HashSet<Ident> paramsSoFar = new HashSet<>(8);
        while (true)
        {
            // if the next token is not a identifier,
            // report the error and skip it until ')'.
            if (nextTokenIsNot(IDENTIFIER))
            {
                // TODO report error
                skipUntil(RPAREN, true);
                paramInfos.clear();
                return;
            }
            // reject typedef int y; int test(x, y), but continue parsing.
            Ident id = (Ident)peekToken();
            if (action.getTypeByName(id, id.loc, getCurScope()) != null)
            {
                // TODO report error
                syntaxError(id.loc, "unexpected typedef identifier.");
            }

            if (!paramsSoFar.add(id))
            {
                // TODO report error
                syntaxError(id.loc, "parameter redefinition in function parmeter list..");
            }
            else
            {
                paramInfos.add(new DeclSpec.ParamInfo(id.name, id.loc, null));
            }
            consumeToken();

            // the list continues if we see a comma.
            if (nextTokenIsNot(COMMA))
                break;
            consumeToken();
        }
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
     *     parameter-frontend.type-list:[C99 6.7.5]
     *      parameter-list
     *      parameter-list ',' '...'
     *
     *     parameter-list: [C99 6.7.5]
     *       parameter-declaration
     *       parameter-list, parameter-declaration
     *
     *     parameter-declaration: [C99 6.7.5]
     *       declaration-speicifers declarator
     *       declaration-specifiers abstract-declarator[backend.opt]
     * </pre>
     *
     * @param declarator
     * @param paramInfos
     * @return
     */
    private int parseParameterDeclarationClause(Declarator declarator,
            ArrayList<DeclSpec.ParamInfo> paramInfos)
    {
        int ellipsisLoc = Position.NOPOS;
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
            int dsstartLoc = peekToken().loc;

            // parse the declaration specifiers
            parseDeclarationSpecifiers(ds);

            // Parse the declarator.
            // This is "FunctionProtoTypeContext".
            Declarator paramDecls = new Declarator(ds, TheContext.FunctionProtoTypeContext);
            parseDeclarator(paramDecls);

            // remember this parsed parameter in ParamInfo.
            String paramName = paramDecls.getName();

            // if no parameter specified, verify that "something" was specified,
            // otherwise we have a missing frontend.type and identifier.
            if (ds.isEmpty() && paramName == null
                    && paramDecls.getNumTypeObjects() == 0)
            {
                // TODO report error
                syntaxError(dsstartLoc, "missing parameter.");
            }
            else
            {
                // Otherwise, we have see something. Add it to the ParamInfo
                // we are building and let semantic analysis try to verify it.

                // Inform the actions module about the parameter declarator,
                // so it gets added to the current scope.
                Decl param = action.actOnParamDeclarator(getCurScope(), paramDecls);

                // Here, we can not handle default argument inC mode.
                paramInfos.add(new DeclSpec.ParamInfo(paramName,
                        paramDecls.getIdentifierLoc(), param));
            }

            // IfStmt the next token is ',', consume it and keep reading argument
            if (nextTokenIs(COMMA))
            {
                // eat comma
                consumeToken();
            }
            else if (nextTokenIs(ELLIPSIS))
            {
                ellipsisLoc = consumeToken();
                // we have ellipsis without a proceeding ',', which is ill-formed
                // in C. Complain and provide the fix.
                // TODO report error
                syntaxError(ellipsisLoc, "missing a ',' before '...'");
                break;
            }
        }
        return ellipsisLoc;
    }

    /**
     * [C90]   direct-declarator '[' constant-expression[backend.opt] ']'
     * [C99]   direct-declarator '[' frontend.type-qual-list[backend.opt] assignment-subExpr[backend.opt] ']'
     * [C99]   direct-declarator '[' 'static' frontend.type-qual-list[backend.opt] assign-subExpr ']'
     * [C99]   direct-declarator '[' frontend.type-qual-list 'static' assignment-subExpr ']'
     * [C99]   direct-declarator '[' frontend.type-qual-list[backend.opt] '*' ']'
     */
    private void parseBracketDeclarator(Declarator declarator)
    {
        assert nextTokenIs(LBRACKET);
        int lbracketLoc = consumeToken();
        int rbracketLoc;

        // C array syntax has many features, but by-far the most common is [] and [4].
        // This code does a fast path to handle some of the most obvious cases.
        if (nextTokenIs(RBRACKET))
        {
            // eat the ']'.
            rbracketLoc = consumeToken();

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
            ActionResult<Expr> numOfSize = action.actOnNumericConstant(peekToken());

            // eat the ']'.
            rbracketLoc = consumeToken();

            // remember that we parsed a array frontend.type
            declarator.addTypeInfo(
                    DeclaratorChunk.getArray(0,
                            false, false,
                            numOfSize.get(),
                            lbracketLoc, rbracketLoc),
                    rbracketLoc);
            return;
        }
        else
        {
            //TODO:  Currently, we can not to handle C99 feature
            log.unreachable("Currently, we can not to handle C99 feature");
            return;
        }
    }

    /**
     * ParseTypeQualifierListOpt
     * <pre>
     * frontend.type-qualifier-list: [C99 6.7.5]
     *   frontend.type-qualifier
     * </pre>
     * @param ds
     */
    private void parseTypeQualifierListOpt(DeclSpec ds)
    {
        while(true)
        {
            boolean isInvalid = false;
            Token tok = peekToken();
            int loc = tok.loc;
            switch (tok.tag)
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
                    // if this is not a frontend.type-qualifier token, we are done read frontend.type
                    // qualifiers.
                    ds.finish(this);
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
     *   frontend.type-specifier specifier-qualifier-list[backend.opt]
     *   frontend.type-qualifier specifier-qualifier-list[backend.opt]
     * @param ds
     */
    private void parseSpecifierQualifierList(DeclSpec ds)
    {
        // specifier-qualifier-list is a subset of declaration-specifiers.
        // parse declaration-specifiers and complain about extra stuff.
        // valid it
        parseDeclarationSpecifiers(ds);

        // validate declspec for frontend.type-name
        int specs = ds.getParsedSpecifiers();
        if (specs == ParsedSpecifiers.PQ_none)
        {
            // TODO report error
            syntaxError(peekToken().loc, "typename requires specifier-qualifiers.");
        }
        // issue diagnostic and remove storage class if present
        if ((specs & ParsedSpecifiers.PQ_StorageClassSpecifier) != 0)
        {
            // TODO report error
            syntaxError(peekToken().loc, "invalid storage class qualifiers.");
            ds.clearStorageClassSpec();
        }

        // issue diagnostic and remove function specifier if present
        if ((specs & ParsedSpecifiers.PQ_FunctionSpecifier) != 0)
        {
            if (ds.isInlineSpecifier())
                syntaxError(peekToken().loc, "invalid inline specifier.");
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
        int nextTokPrec = getBinOpPrecedence(peekToken().tag);

        int colonLoc = Position.NOPOS;
        while(true)
        {
            // if this token has a lower precedence than we are allowed to parse
            // then we are done.
            if (nextTokPrec < minPrec)
                return lhs;

            // consume the operator token, then advance the tokens stream.
            Token opToken = peekToken();
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
                    syntaxError(peekToken().loc,
                            "use of GNU ?: conditional expression extension, omitting middle operand");
                }

                if (nextTokenIs(COLON))
                    colonLoc = consumeToken(); // eat the ':'.
                else
                {
                    // We missing a ':' after ternary middle expression.
                    syntaxError(peekToken().loc, "expected a ':'");
                    syntaxError(opToken.loc, "to match this %s", "?");
                    colonLoc = peekToken().loc;
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
            nextTokPrec = getBinOpPrecedence(peekToken().tag);

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
                nextTokPrec = getBinOpPrecedence(peekToken().tag);
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
            syntaxError(peekToken().loc, "expected an expression.");
        return res;
    }

    enum ParenParseOption
    {
        SimpleExpr,      // Only parse '(' expression ')'
        CompoundLiteral, // Also allow '(' frontend.type-name ')' '{' ... '}'
        CastExpr         // Also allow '(' frontend.type-name ')' <anything>
    }

    /**
     * Parse a cast-expression, or, if isUnaryExpression is
     * true, parse a unary-expression. isAddressOfOperand exists because an
     * id-expression that is the operand of address-of gets special treatment
     * due to member pointers.
     * @param isUnaryExpression
     * @return A pair of both cast-expr and notCastExpr of frontend.type boolean.
     */
    private ActionResult<Expr>
        parseCastExpression(
            boolean isUnaryExpression,
            boolean isAddressOfOperand,
            boolean isTypeCast,
            OutParamWrapper<Boolean> notCastExpr)
    {
        // TODO parseCastExpression
        ActionResult<Expr> res = null;
        Token nextTok = peekToken();
        int savedKind = nextTok.tag;
        notCastExpr.set(false);

        switch (savedKind)
        {
            case LPAREN:
            {
                QualType castTy = null;
                OutParamWrapper<QualType> out1 = new OutParamWrapper<>(castTy);
                int rParenLoc = Position.NOPOS;
                OutParamWrapper<Integer> out2 = new OutParamWrapper<>(rParenLoc);
                ParenParseOption parenExprTppe =
                        isUnaryExpression ? CompoundLiteral:CastExpr;
                OutParamWrapper<ParenParseOption> out3 = new OutParamWrapper<>(parenExprTppe);

                res = parseParenExpression(out3, false, isTypeCast,
                        out1, out2);

                castTy = out1.get();
                rParenLoc = out2.get();
                parenExprTppe = out3.get();

                switch (parenExprTppe)
                {
                    // nothing to do.
                    case SimpleExpr: break;
                    // nothing to do.
                    case CompoundLiteral:
                        // We parsed '(' frontend.type-name ')' '{' ... '}'.  If any suffixes of
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
                Ident id = (Ident)peekToken();
                consumeToken();

                // primary-expression: identifier
                if (isAddressOfOperand && isPostfixExpressionSuffixStart())
                    isAddressOfOperand = false;

                // Function designators are allowed to be undeclared (C99 6.5.1p2), so we
                // need to know whether or not this identifier is a function designator or
                // not.
                res = action.actOnIdExpr(getCurScope(),
                        id,
                        nextTokenIs(LPAREN),
                        isAddressOfOperand,
                        nextTokenIs(LPAREN));
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
                int savedLoc = consumeToken();
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
                int savedLoc = consumeToken();
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
                syntaxError(peekToken().loc, "expected an expression");
                return exprError();
            }
            default:
                notCastExpr.set(true);
                return exprError();
        }

        // These can be followed by postfix-expr pieces.
        ActionResult<Expr> postfixExpr = parsePostfixExpressionSuffix(res);
        return postfixExpr;
    }
    /**
     * Returns true if the next token would start a postfix-expression
     * suffix.
     */
    private boolean isPostfixExpressionSuffixStart()
    {
        int kind = peekToken().tag;
        return kind == LBRACKET || kind == LPAREN || kind == DOT
                || kind == SUBGT || kind == PLUSPLUS || kind == SUBSUB;
    }

    /**
     * Parse a sizeof or alignof expression.
     * <pre>
     * unary-expression:  [C99 6.5.3]
     *   'sizeof' unary-expression
     *   'sizeof' '(' frontend.type-name ')'
     * </pre>
     * @return
     */
    private ActionResult<Expr> parseUnaryExpression()
    {
        assert nextTokenIs(SIZEOF):"Not a sizeof expression!";
        Token opTok = peekToken();
        int opLoc = consumeToken();

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
     *   postfix-expression '(' argument-expression-list[backend.opt] ')'
     *   postfix-expression '.' identifier
     *   postfix-expression '->' identifier
     *   postfix-expression '++'
     *   postfix-expression '--'
     *   '(' frontend.type-name ')' '{' initializer-list '}'
     *   '(' frontend.type-name ')' '{' initializer-list ',' '}'
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
        int loc;
        while(true)
        {
            switch (peekToken().tag)
            {
                case IDENTIFIER:
                    // fall through; this is a primary expression.
                default:
                    return lhs;
                case LBRACKET:
                {
                    // postfix-expression: p-e '[' expression ']'
                    int lBracket = consumeToken();
                    ActionResult<Expr> idx = parseExpression();

                    int rBracketLoc = peekToken().loc;
                    if (!lhs.isInvalid() && !idx.isInvalid() && nextTokenIs(RBRACKET))
                        lhs = action.actOnArraySubscriptExpr(lhs.get(), lBracket,
                                idx.get(), rBracketLoc);
                    else
                        lhs = exprError();

                    // match the ']'.
                    expect(RBRACKET);
                    break;
                }
                case LPAREN:
                {
                    // p-e: p-e '(' argument-expression-list[backend.opt] ')'
                    loc = consumeToken();
                    ArrayList<Expr> exprs = new ArrayList<>();
                    ArrayList<Integer> commaLocs = new ArrayList<>();

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

                        lhs = action.actOnCallExpr(lhs.get(), loc, exprs, peekToken().loc);
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
                    Ident id = (Ident)peekToken();
                    int opLoc = id.loc;
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
                        Token tok = peekToken();
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

        Token.StringLiteral str = (Token.StringLiteral)peekToken();
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
        return skipUntil(new int[] { tag }, stopAtSemi);
    }

    private boolean skipUntil(int tag1, int tag2, boolean stopAtSemi)
    {
        return skipUntil(new int[] { tag1, tag2 }, stopAtSemi);
    }

    private boolean skipUntil(int[] tags, boolean stopAtSemi)
    {
        boolean isFirstTokenSkipped = true;
        while (true)
        {
            for (int tag : tags)
            {
                if (nextTokenIs(tag))
                {
                    consumeToken();
                    return true;
                }
            }
            switch (peekToken().tag)
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

    /**
     * Checks if the next token is an identifier for typedef statement.
     */
    private boolean nextTokenIsTypeName()
    {
        assert nextTokenIs(
                IDENTIFIER) : "The next token must be an identifier.";
        String name = ((Ident) peekToken()).name;

        return false;
    }

    /**
     * Consume the next token from frontend.parser.
     * @return  return the location of just consumed token.
     */
    private int consumeToken()
    {
        int loc;
        if (lookAheadToken.tokenAvail == 2)
        {
            loc = lookAheadToken.tokens[0].loc;
            lookAheadToken.tokens[0] = lookAheadToken.tokens[1];
        }
        else
        {
            assert lookAheadToken.tokenAvail == 1;
            assert lookAheadToken.tokens[0].tag != EOF;
            loc = lookAheadToken.tokens[0].loc;
        }
        lookAheadToken.tokenAvail--;
        return loc;
    }

    /**
     * Peeks ahead token and returns that token without consuming any token.
     *
     * @return
     */
    private Token peekAheadToken()
    {
        if (lookAheadToken.tokenAvail == 0)
        {
            lookAheadToken.tokens[0] = S.token;
            S.nextToken();
            lookAheadToken.tokens[1] = S.token;
            S.nextToken();
        }
        else if (lookAheadToken.tokenAvail == 1)
        {
            lookAheadToken.tokens[1] = S.token;
            S.nextToken();
        }
        return lookAheadToken.tokens[1];
    }

    /**
     * ReturnStmt a reference to next token if necessary.
     *
     * @return
     */
    private Token peekToken()
    {
        if (lookAheadToken.tokenAvail == 0)
        {
            lookAheadToken.tokens[0] = S.token;
            S.nextToken();
            lookAheadToken.tokenAvail = 1;
        }
        return lookAheadToken.tokens[0];
    }

    /**
     * Checks if the next token is expected.
     *
     * @param expectedToken A target token to be compared with next Token.
     * @return
     */
    private boolean nextTokenIs(int expectedToken)
    {
        return peekToken().tag == expectedToken;
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
        return peekToken().tag != expectedToken;
    }

    /**
     * ReturnStmt true if the next token is keyword.
     *
     * @return
     */
    private boolean nextTokenIsKeyword()
    {
        Token token = peekToken();
        return keywords.isKeyword(token);
    }

    /**
     * Skips the parameter lists of function surrounding with parenthesis.
     */
    private void skipParenthesis()
    {
        while (true)
        {
            Token tok = peekToken();
            consumeToken();
            if (tokenIs(tok, EOF))
                syntaxError(tok.loc, "Premature end of input");
            if (tokenIs(tok, RPAREN))
                return;
            if (tokenIs(tok, LPAREN))
                skipParenthesis();
        }
    }

    /**
     * Returns true if next token is equal to given token, false otherwise.
     *
     * @param token
     * @return
     */
    private boolean match(int token)
    {

        return S.token.tag == token;
    }

    private boolean match(Token t, int token)
    {
        return t.tag == token;
    }

    /**
     * IfStmt next input token matches given token, skip it, otherwise report an
     * error.
     */
    private void accept(int token)
    {
        Token tok = peekToken();
        if (tokenIs(tok, token))
        {
            consumeToken();
        }
        else
        {
            int pos = Position.line(tok.loc) > Position.line(S.prevEndPos + 1) ?
                    S.prevEndPos + 1 :
                    tok.loc;
            syntaxError(pos, "expected a", keywords.token2string(token));
        }
        skipUntil(LBRACE, true);
        if (nextTokenIs(SEMI))
            consumeToken();
    }

    public void syntaxError(int pos, String key, String arg1, String arg2)
    {
        if (pos != S.errPos)
            log.error(pos, key, arg1, arg2);
        S.errPos = pos;
    }

    public void syntaxError(int pos, String key, String arg)
    {
        if (pos != S.errPos)
            log.error(pos, key, arg);
        //skipUntil(Tag.SEMI, true);
        S.errPos = pos;
    }

    public void syntaxError(int pos, String key)
    {
        if (pos != S.errPos)
            log.error(pos, key);
        //skipUntil(Tag.SEMI, true);
        S.errPos = pos;
    }

    /**
     * Complains a warning at the specified position about info.
     * @param pos
     * @param key
     */
    public void syntaxWarning(int pos, String key)
    {
        if (pos != S.errPos)
            log.warning(pos, key);
        //skipUntil(Tag.SEMI, true);
        S.errPos = pos;
    }

    /**
     * Complains a warning at the specified position about info.
     * @param pos
     * @param key
     */
    public void syntaxWarning(int pos, String key, String msg)
    {
        if (pos != S.errPos)
            log.warning(pos, key, msg);
        //skipUntil(Tag.SEMI, true);
        S.errPos = pos;
    }

    public void syntaxWarning(int pos, String key, String msg1, String msg2)
    {
        if (pos != S.errPos)
            log.warning(pos, key, msg1, msg2);
        //skipUntil(Tag.SEMI, true);
        S.errPos = pos;
    }

    public void syntaxWarning(int pos, String key, String msg1, String msg2, String msg3)
    {
        if (pos != S.errPos)
            log.warning(pos, key, msg1, msg2, msg3);
        //skipUntil(Tag.SEMI, true);
        S.errPos = pos;
    }

    /**
     * Expects a token with specified tag, return its position if expected successfully,
     * otherwise, return -1.
     * @param tag
     * @return
     */
    private int expect(int tag)
    {
        Token tok = peekToken();
        if (tok.tag == tag)
        {
            return consumeToken();
        }
        syntaxError(tok.loc, "%s expected, but got %s",
                Token.getTokenName(tag),
                Token.getTokenName(tok.tag));

        skipUntil(SEMI, true);
        if (nextTokenIs(SEMI))
            consumeToken();
        return -1;
    }
}
