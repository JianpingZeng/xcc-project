package jlang.clex;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import jlang.diag.Diagnostic;

import java.util.ArrayList;

import static jlang.diag.DiagnosticLexKindsTag.*;

/**
 * Instances of this interface defined to handle the various of Pragma that
 * the language front-end uses. Each handler optionally has a name (e.g. "pack")
 * and handlePragma method is invoked when a pragma with identifier is found.
 * If a handler does not match nay of the declared pragmas the handler with a
 * null identifier is invoked, if it exists.
 *
 * Note that the PragmaNamespace class can be used to subdivide pragmas, e.g.
 * we treat "#pragma STDC" and "#pragma GCC" as namespace that contain other
 * pragmas.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class PragmaHandler
{
    private IdentifierInfo name;

    public PragmaHandler(IdentifierInfo name)
    {
        this.name = name;
    }

    public IdentifierInfo getName()
    {
        return name;
    }

    public abstract void handlePragma(Preprocessor pp, Token firstToken);

    public PragmaNameSpace getIfNamespace()
    {
        return null;
    }

    /**
     * Pragma STDC implementations.
     */
    public enum STDCSetting
    {
        STDC_ON, STDC_OFF, STDC_DEFAULT, STDC_INVALID
    }
    public static STDCSetting lexOnOffSwitch(Preprocessor pp)
    {
        Token tok = new Token();
        pp.lexUnexpandedToken(tok);

        if (tok.isNot(TokenKind.identifier))
        {
            pp.diag(tok, ext_stdc_pragma_syntax).emit();
            return STDCSetting.STDC_INVALID;
        }
        IdentifierInfo ii = tok.getIdentifierInfo();
        STDCSetting result;
        if (ii.isStr("ON"))
            result = STDCSetting.STDC_ON;
        else if (ii.isStr("OFF"))
            result = STDCSetting.STDC_OFF;
        else if (ii.isStr("DEFAULT"))
            result = STDCSetting.STDC_DEFAULT;
        else
        {
            pp.diag(tok, ext_stdc_pragma_syntax).emit();
            return STDCSetting.STDC_INVALID;
        }
        // Verify that this is followed by eom.
        pp.lexUnexpandedToken(tok);
        if (tok.isNot(TokenKind.eom))
            pp.diag(tok, ext_stdc_pragma_syntax_eom).emit();

        return result;
    }

    /**
     * Handle GCC "#pragma poison".
     */
    public final static class PragmaPoisonHandler extends PragmaHandler
    {
        public PragmaPoisonHandler(IdentifierInfo name)
        {
            super(name);
        }

        /**
         * Handle GCC "#pragma poison", poisonToken is the location.
         *
         * @param pp
         * @param poisonToken
         */
        @Override
        public void handlePragma(Preprocessor pp, Token poisonToken)
        {
            pp.handlePragmaPoison(poisonToken);
        }
    }

    /**
     * Handle the "#pragma system_header" marks the current file as a system
     * header, which silences warnings in it.
     */
    public final static class PragmaSystemHeaderHandler extends PragmaHandler
    {
        public PragmaSystemHeaderHandler(IdentifierInfo name)
        {
            super(name);
        }

        @Override
        public void handlePragma(Preprocessor pp, Token token)
        {
            pp.handlePragmaSystemHeader(token);
            pp.checkEndOfDirective("pragma");
        }
    }

    public final static class PragmaDependencyHandler extends PragmaHandler
    {
        public PragmaDependencyHandler(IdentifierInfo name)
        {
            super(name);
        }

        @Override
        public void handlePragma(Preprocessor pp, Token depToken)
        {
            pp.handlePragmaDependency(depToken);
        }
    }

    /**
     * PragmaDiagnosticHandler - e.g. '#pragma GCC diagnostic ignored "-Wformat"'
     * Since clang's diagnostic supports extended functionality beyond GCC's
     * the constructor takes a clangMode flag to tell it whether or not to allow
     * clang's extended functionality, or whether to reject it.
     */
    public final static class PragmaDiagnosticHandler extends PragmaHandler
    {
        private boolean clangMode;
        public PragmaDiagnosticHandler(IdentifierInfo name, boolean clangMode)
        {
            super(name);
            this.clangMode = clangMode;
        }

        @Override
        public void handlePragma(Preprocessor pp, Token firstToken)
        {
            Token tok = new Token();
            pp.lexUnexpandedToken(tok);
            if (tok.isNot(TokenKind.identifier))
            {
                int diag = clangMode ? warn_pragma_diagnostic_clang_invalid
                        : warn_pragma_diagnostic_gcc_invalid;
                pp.diag(tok, diag).emit();
                return;
            }

            IdentifierInfo ii = tok.getIdentifierInfo();
            Diagnostic.Mapping map;
            if (ii.isStr("warning"))
                map = Diagnostic.Mapping.MAP_WARNING;
            else if (ii.isStr("error"))
                map = Diagnostic.Mapping.MAP_ERROR;
            else if (ii.isStr("ignored"))
                map = Diagnostic.Mapping.MAP_IGNORE;
            else if (ii.isStr("fatal"))
                map = Diagnostic.Mapping.MAP_FATAL;
            else if (clangMode)
            {
                if (ii.isStr("pop"))
                {
                    if (!pp.getDiagnostics().popMappings())
                        pp.diag(tok, warn_pragma_diagnostic_clang_cannot_ppp).emit();
                    return;
                }

                if (ii.isStr("push"))
                {
                    pp.getDiagnostics().pushMappings();
                    return;
                }
                pp.diag(tok, warn_pragma_diagnostic_clang_invalid).emit();
                return;
            }
            else
            {
                pp.diag(tok, warn_pragma_diagnostic_gcc_invalid).emit();
                return;
            }

            pp.lexUnexpandedToken(tok);

            // We need at least one string.
            if (tok.isNot(TokenKind.string_literal))
            {
                pp.diag(tok.getLocation(), warn_pragma_diagnostic_invalid_token).emit();
                return;
            }

            // String concatenation allows multiple strings, which can even come
            // from macro expansion.
            // "foo" "bar" "baz"
            ArrayList<Token> strToks = new ArrayList<>();
            while (tok.is(TokenKind.string_literal))
            {
                strToks.add(tok.clone());
                pp.lexUnexpandedToken(tok);
            }

            if (tok.isNot(TokenKind.eom))
            {
                pp.diag(tok.getLocation(), warn_pragma_diagnostic_invalid_token).emit();
                return;
            }

            // Concatenate and parse the strings.
            Token toks[] = new Token[strToks.size()];
            strToks.toArray(toks);
            StringLiteralParser parser = new StringLiteralParser(toks, pp);
            if (parser.hadError)
            {
                return;
            }
            if (parser.pascal)
            {
                int diag = clangMode ? warn_pragma_diagnostic_clang_invalid :
                        warn_pragma_diagnostic_gcc_invalid;
                pp.diag(tok, diag).emit();
                return;
            }

            String warningName = parser.getString();
            if (warningName.length() < 3 || warningName.charAt(0) != '-' ||
                    warningName.charAt(1) != 'W')
            {
                pp.diag(toks[0], warn_pragma_diagnostic_invalid_option).emit();
                return;
            }

            if (pp.getDiagnostics().setDiagnosticGroupMapping(warningName.substring(2), map))
            {
                pp.diag(toks[0], warn_pragma_diagnostic_unknown_warning)
                        .addTaggedVal(warningName)
                        .emit();
            }
        }
    }

    /**
     * Handle "#pragma comment ..."
     */
    public static final class PragmaCommentHandler extends PragmaHandler
    {
        public PragmaCommentHandler(IdentifierInfo name)
        {
            super(name);
        }

        @Override
        public void handlePragma(Preprocessor pp, Token commentToken)
        {
            pp.handlePragmaComment(commentToken);
        }
    }

    /**
     * Handle "#pragma STDC_FP_CONTRACT".
     */
    public static final class PragmaSTDC_FP_CONTRACTHandler extends PragmaHandler
    {
        public PragmaSTDC_FP_CONTRACTHandler(IdentifierInfo name)
        {
            super(name);
        }

        @Override
        public void handlePragma(Preprocessor pp, Token firstToken)
        {
            // We just ignore the setting of FP_CONTRACT. Since we don't do contractions
            // at all, our default is OFF and setting it to ON is an optimization hint
            // we can safely ignore.  When we support -ffma or something, we would need
            // to diagnose that we are ignoring FMA.
            lexOnOffSwitch(pp);
        }
    }

    /**
     * Handle the "#pragma STDC_ACCESS".
     */
    public static final class PragmaSTDC_FENV_ACCESSHandler extends PragmaHandler
    {
        public PragmaSTDC_FENV_ACCESSHandler(IdentifierInfo name)
        {
            super(name);
        }

        @Override
        public void handlePragma(Preprocessor pp, Token firstToken)
        {
            if (lexOnOffSwitch(pp) == STDCSetting.STDC_ON)
                pp.diag(firstToken, warn_stdc_fenv_access_not_supported).emit();
        }
    }

    /**
     * Handle the "#pragma STDC_CX_LIMITED_RANGE".
     */
    public static final class PragmaSTDC_CX_LIMITED_RANGEHandler extends PragmaHandler
    {
        public PragmaSTDC_CX_LIMITED_RANGEHandler(IdentifierInfo name)
        {
            super(name);
        }

        @Override
        public void handlePragma(Preprocessor pp, Token firstToken)
        {
            lexOnOffSwitch(pp);
        }
    }

    /**
     * Handle the "#pragma STDC ...".
     */
    public static final class PragmaSTDC_UnknownHandler extends PragmaHandler
    {
        public PragmaSTDC_UnknownHandler()
        {
            super(null);
        }

        @Override
        public void handlePragma(Preprocessor pp, Token firstToken)
        {
            // C99 6.10.6p2, unknown forms are not allowed.
            pp.diag(firstToken, ext_stdc_pragma_ignored).emit();
        }
    }
}
