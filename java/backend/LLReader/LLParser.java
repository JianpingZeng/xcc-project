/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

package backend.LLReader;

import backend.support.CallingConv;
import backend.support.LLVMContext;
import backend.type.*;
import backend.value.Function;
import backend.value.GlobalValue;
import backend.value.GlobalValue.LinkageType;
import backend.value.GlobalValue.VisibilityTypes;
import backend.value.MetadataBase;
import backend.value.Module;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import jlang.codegen.CodeGenTypes;
import jlang.support.MemoryBuffer;
import tools.*;
import tools.SourceMgr.SMLoc;

import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeMap;

import static backend.LLReader.LLTokenKind.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class LLParser
{
    public static class UpRefRecord
    {
        SMLoc loc;
        int nestedLevel;
        Type lastContainedTy;
        OpaqueType upRefTy;

        public UpRefRecord(SMLoc loc, int nestedLevel,OpaqueType upRefTy)
        {
            this.loc = loc;
            this.nestedLevel = nestedLevel;
            this.lastContainedTy = upRefTy;
            this.upRefTy = upRefTy;
        }
    }

    private LLLexer lexer;
    private Module m;
    private TreeMap<String, Pair<Type, SMLoc>> forwardRefTypes;
    private TIntObjectHashMap<Pair<Type, SMLoc>> forwardRefTypeIDs;
    private ArrayList<Type> numberedTypes;
    private TIntObjectHashMap<MetadataBase> metadataCache;
    private TIntObjectHashMap<Pair<MetadataBase, SMLoc>> forwardRefMDNodes;
    private ArrayList<UpRefRecord> upRefs;

    private TreeMap<String, Pair<GlobalValue, SMLoc>> forwardRefVals;
    private TIntObjectHashMap<Pair<GlobalValue, SMLoc>> forwardRefValIDs;
    private ArrayList<GlobalValue> numberedVals;

    public LLParser(MemoryBuffer buf, SourceMgr smg, SMDiagnostic diag, Module m)
    {
        lexer = new LLLexer(buf, smg, diag);
        this.m = m;
        forwardRefTypes = new TreeMap<>();
        forwardRefTypeIDs = new TIntObjectHashMap<>();
        numberedTypes = new ArrayList<>();
        metadataCache = new TIntObjectHashMap<>();
        forwardRefMDNodes = new TIntObjectHashMap<>();
        upRefs = new ArrayList<>();
        forwardRefVals = new TreeMap<>();
        forwardRefValIDs = new TIntObjectHashMap<>();
        numberedVals = new ArrayList<>();
    }

    /**
     * The entry to parse input ll file.
     * <pre>
     *  module ::= toplevelentity*
     * </pre>
     * @return
     */
    public boolean run()
    {
        // obtain a token.
        lexer.lex();
        return parseTopLevelEntities() || validateEndOfModule();
    }

    private boolean error(SMLoc loc, String msg)
    {
        return lexer.error(loc, msg);
    }

    private boolean tokError(String msg)
    {
        return error(lexer.getLoc(), msg);
    }

    /**
     * The top level entities.
     * @return Sucessful return true, otherwise return false.
     */
    private boolean parseTopLevelEntities()
    {
        while (true)
        {
            switch (lexer.getTokKind())
            {
                default:
                    return tokError("expected top-level entity");
                case Eof:
                    return false;
                case kw_declare:
                    if (parseDeclare())
                        return true;
                    break;
                case kw_define:
                    if (parseDefine())
                        return true;
                    break;
                case kw_module:
                    if (parseModuleAsm())
                        return true;
                    break;
                case kw_target:
                    if (parseTargetDefinition())
                        return true;
                    break;
                case kw_deplibs:
                    if (parseDepLibs())
                        return true;
                    break;
                case kw_type:
                    if (parseUnnamedType())
                        return true;
                    break;
                case LocalVarID:
                    if (parseUnnamedType())
                        return true;
                    break;
                case StringConstant:
                case LocalVar:
                    if (parseNamedType())
                        return true;
                    break;
                case GlobalID:
                    if (parseUnnamedGlobal())
                        return true;
                    break;
                case GlobalVar:
                    if (parseNamedGlobal())
                        return true;
                    break;
                case Metadata:
                    if (parseStandaloneMetadata())
                        return true;
                    break;
                case NamedMD:
                    if (parseNamedMetadata())
                        return true;
                    break;

                case kw_private:
                case kw_linker_private:
                case kw_internal:
                case kw_weak:
                case kw_weak_odr:
                case kw_linkonce:
                case kw_linkonce_odr:
                case kw_appending:
                case kw_dllexport:
                case kw_common:
                case kw_dllimport:
                case kw_external:
                case kw_extern_weak:
                {
                    OutParamWrapper<LinkageType> linkage = new OutParamWrapper<>();
                    OutParamWrapper<VisibilityTypes> visibility = new OutParamWrapper<>();
                    if (parseOptionalLinkage(linkage)
                            || parseOptionalVisibility(visibility)
                            || parseGlobal("", new SMLoc(), linkage, true, visibility))
                    {
                        return true;
                    }
                    break;
                }
                case kw_default:
                case kw_hidden:
                case kw_protected:
                {
                    OutParamWrapper<VisibilityTypes> visibility = new OutParamWrapper<>();
                    if ( parseOptionalVisibility(visibility)
                            || parseGlobal("", new SMLoc(), 0, false, visibility))
                    {
                        return true;
                    }
                    break;
                }
                case kw_thread_local:
                case kw_addrspace:
                case kw_constant:
                case kw_global:
                    if (parseGlobal("", new SMLoc(), 0, false, 0))
                        return true;
                    break;
            }
        }
    }

    /**
     * Top-level entity ::= 'declare' FunctionHeader
     * @return
     */
    private boolean parseDeclare()
    {
        assert lexer.getTokKind() == LLTokenKind.kw_declare;
        lexer.lex();

        Function f = parseFunctionHeader(false);
        return f != null;
    }

    /**
     * FunctionHeader ::= OptionalLinkage OptionalVisibility OptionalCallingConvetion OptRetAttrs
     *                    Type GlobalName '(' ArgList ')' OptFuncAttrs
     *                    OptSection OptionalAlign OptGC
     *
     * @param isDefine
     * @return
     */
    private Function parseFunctionHeader(boolean isDefine)
    {
        // parse linkage
        SMLoc linkageLoc = lexer.getLoc();
        OutParamWrapper<LinkageType> linkage = new OutParamWrapper<>();
        OutParamWrapper<VisibilityTypes> visibility = new OutParamWrapper<>();
        OutParamWrapper<CallingConv> cc = new OutParamWrapper<>();

        Type retType = LLVMContext.VoidTy;
        SMLoc retTypeLoc = lexer.getLoc();
        int retAttrs;
        if (parseOptionalLinkage(linkage) ||
                parseOptionalVisibility(visibility) ||
                parseCallingConv(cc) ||
                (retAttrs = parseOptionalAttrs(1)) != -1 ||
                (retType = parseType(retTypeLoc, true/*void allowed*/)) != null)
            return null;

    }

    private boolean parseOptionalLinkage(OutParamWrapper<LinkageType> linkage)
    {
        OutParamWrapper<Boolean> hasLinkage = new OutParamWrapper<>();
        return parseOptionalLinkage(linkage, hasLinkage);
    }

    /**
     * ParseOptionalLinkage
     ///   ::= empty
    ///   ::= 'private'
    ///   ::= 'linker_private'
    ///   ::= 'internal'
    ///   ::= 'weak'
    ///   ::= 'weak_odr'
    ///   ::= 'linkonce'
    ///   ::= 'linkonce_odr'
    ///   ::= 'appending'
    ///   ::= 'dllexport'
    ///   ::= 'common'
    ///   ::= 'dllimport'
    ///   ::= 'extern_weak'
    ///   ::= 'external'
     */
    private boolean parseOptionalLinkage(
            OutParamWrapper<LinkageType> linkage,
            OutParamWrapper<Boolean> hasLinkage)
    {
        hasLinkage.set(false);
        switch (lexer.getTokKind())
        {
            default:
                linkage.set(LinkageType.ExternalLinkage);
                return false;
            case kw_private:
                linkage.set(LinkageType.PrivateLinkage);
                break;
            case kw_linker_private:
                linkage.set(LinkageType.LinkerPrivateLinkage);
                break;
            case kw_internal:
                linkage.set(LinkageType.InteralLinkage);
                break;
            case kw_weak:
            case kw_weak_odr:
            case kw_linkonce:
            case kw_linkonce_odr:
            case kw_available_externally:
            case kw_appending:
            case kw_dllexport:
            case kw_dllimport:
            case kw_extern_weak:
                assert false:"Unsupported linkage type 'weak'";
                break;
            case kw_external:
                linkage.set(LinkageType.ExternalLinkage);
                break;
            case kw_common:
                linkage.set(LinkageType.CommonLinkage);
                break;
        }
        lexer.lex();
        hasLinkage.set(true);
        return false;
    }

    private boolean parseOptionalVisibility(OutParamWrapper<VisibilityTypes> visibility)
    {
        switch (lexer.getTokKind())
        {
            default:
                visibility.set(VisibilityTypes.DefaultVisibility);
                return false;
            case kw_default:
                visibility.set(VisibilityTypes.DefaultVisibility);
                break;
            case kw_hidden:
                visibility.set(VisibilityTypes.HiddenVisibility);
                break;
            case kw_protected:
                visibility.set(VisibilityTypes.ProtectedVisibility);
                break;
        }
        lexer.lex();
        return false;
    }

    /**
     * CallingConvention
     ///   ::= empty
    ///   ::= 'ccc'
    ///   ::= 'fastcc'
    ///   ::= 'coldcc'
    ///   ::= 'x86_stdcallcc'
    ///   ::= 'x86_fastcallcc'
    ///   ::= 'arm_apcscc'
    ///   ::= 'arm_aapcscc'
    ///   ::= 'arm_aapcs_vfpcc'
    ///   ::= 'cc' UINT
     * @param cc
     * @return
     */
    private boolean parseCallingConv(OutParamWrapper<CallingConv> cc)
    {
        switch (lexer.getTokKind())
        {
            default:
                cc.set(CallingConv.C);
                return false;
            case kw_fastcc:
                cc.set(CallingConv.Fast);
                break;
            case kw_coldcc:
                cc.set(CallingConv.Cold);
                break;
            case kw_x86_fastcallcc:
                cc.set(CallingConv.X86_FastCall);
                break;
            case kw_x86_stdcallcc:
                cc.set(CallingConv.X86_StdCall);
                break;
        }
        lexer.lex();
        return false;
    }

    private boolean expectToken(LLTokenKind kind)
    {
        if (lexer.getTokKind() != kind)
            return false;
        lexer.lex();
        return true;
    }

    private boolean parseOptionalAlignment(OutParamWrapper<Integer> align)
    {
        if (!expectToken(LLTokenKind.kw_align))
            return false;

        SMLoc alignLoc = lexer.getLoc();
        if (parseInt32(align))
            return true;
        if (!Util.isPowerOf2(align.get()))
            return error(alignLoc, "alignment is not power of 2");
        return false;
    }

    private boolean parseInt32(OutParamWrapper<Integer> align)
    {
        if (lexer.getTokKind() != LLTokenKind.APSInt || lexer.getAPsIntVal().isSigned())
            return tokError("expected integer");
        long intVal = lexer.getAPsIntVal().getLimitedValue(0xFFFFFFFFL+1);
        if (intVal != (int)intVal)
        {
            return tokError("expected 32 bit integer(too large)");
        }
        align.set((int)intVal);
        lexer.lex();
        return false;
    }

    private boolean parseOptionalCommaAlignment(OutParamWrapper<Integer> align)
    {
        if (!expectToken(LLTokenKind.comma))
            return false;
        return parseToken(LLTokenKind.kw_align, "expect 'align'") ||
                parseInt32(align);
    }

    private boolean parseToken(LLTokenKind expectToken, String errorMsg)
    {
        if (lexer.getTokKind() != expectToken)
            return tokError(errorMsg);
        lexer.lex();
        return false;
    }
    private boolean parseStringConstant(OutParamWrapper<String> result)
    {
        if (lexer.getTokKind() != LLTokenKind.StringConstant)
            return false;
        result.set(lexer.getStrVal());
        lexer.lex();
        return false;
    }

    private boolean parseIndexList(TIntArrayList indices)
    {
        if (lexer.getTokKind() != LLTokenKind.comma)
            return tokError("expected ',' as start of index list");

        OutParamWrapper<Integer> index = new OutParamWrapper<>(0);
        while (expectToken(LLTokenKind.comma))
        {
            if (parseInt32(index)) return true;
            indices.add(index.get());
        }
        return false;
    }

    private int parseOptionalAttrs()
    {}

    private boolean parseType(OutParamWrapper<Type> result, boolean allowVoid)
    {
        SMLoc typeLoc = lexer.getLoc();
        if (parseTypeRec(result))
            return true;

        if (!upRefs.isEmpty())
            return error(upRefs.get(upRefs.size()-1).loc, "invalid unresolved type upward reference");
        if (!allowVoid && result.get().equals(LLVMContext.VoidTy))
            return error(typeLoc, "void type only allowed for function results");
        return false;
    }

    private boolean parseTypeRec(OutParamWrapper<Type> result)
    {
        switch (lexer.getTokKind())
        {
            default:
                return tokError("expected type");
            case Type:
                result.set(lexer.getTyVal());
                lexer.lex();
                break;
            case kw_opaque:
                result.set(OpaqueType.get());
                lexer.lex();
                break;
            case lbrace:
                if (parseStructType(result, false))
                    return true;
                break;
            case lsquare:
                lexer.lex();
                if (parseArrayVectorType(result, false))
                    return true;
                break;
            case less:
                // Either vector or packed struct.
                lexer.lex();
                if (lexer.getTokKind() == LLTokenKind.lbrace)
                {
                    if (parseStructType(result, true) ||
                            parseToken(LLTokenKind.greater, "expected '>' at end of packed struct"))
                        return true;
                }
                else if (parseArrayVectorType(result, true))
                    return true;
                break;
            case LocalVar:
            case StringConstant:
                // TypeRec ::= %bar
                Type ty = m.getTypeByName(lexer.getStrVal());
                if (ty != null)
                    result.set(ty);
                else
                {
                    result.set(OpaqueType.get());
                    forwardRefTypes.put(lexer.getStrVal(), Pair.get(result.get(), lexer.getLoc()));
                    m.addTypeName(lexer.getStrVal(), result.get());
                }
                lexer.lex();
                break;
            case LocalVarID:
                // TypeRec ::= %4
                int typeId = lexer.getIntVal();
                if(typeId < numberedTypes.size())
                    result.set(numberedTypes.get(typeId));
                else
                {
                    if (forwardRefTypeIDs.containsKey(typeId))
                    {
                        result.set(forwardRefTypeIDs.get(typeId).first);
                    }
                    else
                    {
                        result.set(OpaqueType.get());
                        forwardRefTypeIDs.put(typeId, Pair.get(result.get(), lexer.getLoc()));
                    }
                }
                lexer.lex();
                break;
            case backslash:
                // TypeRec ::= '\' 4
                lexer.lex();
                OutParamWrapper<Integer> val = new OutParamWrapper<>(0);
                if (parseInt32(val)) return true;
                OpaqueType ot = OpaqueType.get();
                upRefs.add(new UpRefRecord(lexer.getLoc(), val.get(), ot));
                result.set(ot);
                break;
        }

        // parse type suffixes.
        while (true)
        {
            switch (lexer.getTokKind())
            {
                // end of type
                default: return false;

                // TypeRec ::= TypeRec '*'
                case star:
                    if (result.get().equals(LLVMContext.LabelTy))
                        return tokError("basic block pointers are invalid");
                    if (result.get().equals(LLVMContext.VoidTy))
                        return tokError("pointers to void are invalid, use i8* instead");
                    if (!PointerType.isValidElementType(result.get()))
                        return tokError("pointer to this type is invalid");
                    result.set(handleUpRefs(PointerType.getUnqual(result.get())));
                    lexer.lex();
                    break;
                case kw_addrspace:
                    // TypeRec ::= TypeRec 'addrspace' '(' uint32 ')' '*'
                    if (result.get().equals(LLVMContext.LabelTy))
                        return tokError("basic block pointers are invalid");
                    if (result.get().equals(LLVMContext.VoidTy))
                        return tokError("pointers to void are invalid, use i8* instead");
                    if (!PointerType.isValidElementType(result.get()))
                        return tokError("pointer to this type is invalid");

                    OutParamWrapper<Integer> addrSpace = new OutParamWrapper<>(0);
                    if (parseOptionalAddrSpace(addrSpace) || parseToken(star,
                            "expected '*' in address space"))
                        return true;

                    result.set(handleUpRefs(PointerType.get(result.get(), addrSpace.get())));
                    lexer.lex();
                    break;
                case lparen:
                    // Types '(' ArgTypeListI ')' OptFuncAttrs
                    if (parseFunctionType(result))
                        return true;
                    break;
            }
        }
    }

    /**
     * AddressSpace ::= /empty/
     *                  'addrspace' '(' uint32 ')'
     * @param addrSpace
     * @return
     */
    private boolean parseOptionalAddrSpace(OutParamWrapper<Integer> addrSpace)
    {
        if (!expectToken(kw_addrspace))
            return false;

        return parseToken(lparen, "expected '(' in address space")
                || parseInt32(addrSpace)
                || parseToken(rparen, "expected ')' in address space");
    }

    private static class ArgInfo
    {
        SMLoc loc;
        Type type;
        int attrs;
        String name;
        public ArgInfo(SMLoc loc, Type ty, int attr, String name)
        {
            this.loc = loc;
            this.type = ty;
            this.attrs = attr;
            this.name = name;
        }
    }

    private boolean parseFunctionType(OutParamWrapper<Type> result)
    {
        assert lexer.getTokKind() == lparen;

        if (!FunctionType.isValidArgumentType(result.get()))
            return tokError("invalid function return type");

        ArrayList<ArgInfo> argList = new ArrayList<>();
    }

    private boolean parseStructType(OutParamWrapper<Type> result, boolean packed)
    {
        assert lexer.getTokKind() == lbrace;
        lexer.lex();    // eat the '{'
        if (expectToken(rbrace))
        {
            result.set(StructType.get(packed));
            return false;
        }

        ArrayList<Type> paramList = new ArrayList<>();
        SMLoc eltTyLoc = lexer.getLoc();
        if (parseTypeRec(result)) return true;
        paramList.add(result.get());

        if (result.get().equals(LLVMContext.VoidTy))
            return error(eltTyLoc, "struct element can not have void type");
        if (!StructType.isValidElementType(result.get()))
            return error(eltTyLoc, "invalid element type for struct");

        while (expectToken(comma))
        {
            eltTyLoc = lexer.getLoc();
            if (parseTypeRec(result))
                return true;

            if (result.get().equals(LLVMContext.VoidTy))
                return error(eltTyLoc, "invalid element type for struct");
            if (!StructType.isValidElementType(result.get()))
                return error(eltTyLoc, "invalid element type for struct");
            paramList.add(result.get());
        }
        if (parseToken(rbrace, "expected '}' at the end of struct"))
            return true;

        result.set(handleUpRefs(StructType.get(paramList, packed)));
        return false;
    }

    private Type handleUpRefs(Type ty)
    {
        if (!ty.isAbstract() || upRefs.isEmpty())
            return ty;

        OpaqueType typeToResolve = null;

        for (int i = 0, e = upRefs.size(); i < e; i++)
        {
            boolean containedType = false;
            for (int j = 0, sz = ty.getNumContainedTypes(); j < sz; j++)
            {
                if (upRefs.get(i).lastContainedTy.equals(ty.getContainedType(j)))
                {
                    containedType = true;
                    break;
                }
            }
            if (!containedType)
                continue;

            int level = --upRefs.get(i).nestedLevel;
            upRefs.get(i).lastContainedTy = ty;

            if (level != 0)
                continue;
            if (typeToResolve == null)
                typeToResolve = upRefs.get(i).upRefTy;
            else
                upRefs.get(i).upRefTy.refineAbstractTypeTo(typeToResolve);
            upRefs.remove(i);
            --i;
            --e;
        }
        if (typeToResolve != null)
            typeToResolve.refineAbstractTypeTo(ty);
        return ty;
    }

    /**
     * <pre>
     * TypeRec
     *  ::= '[' APSINTVAL 'x' Types ']'
     *  ::= '<' APSINTVAL 'x' Types '>'
     * </pre>
     * @param result
     * @param isVector
     * @return
     */
    private boolean parseArrayVectorType(OutParamWrapper<Type> result, boolean isVector)
    {
        if (lexer.getTokKind() != APSInt || lexer.getAPsIntVal().isSigned() ||
            lexer.getAPsIntVal().getBitWidth() > 64)
            return tokError("expected number in address space");

        SMLoc sizeLoc = lexer.getLoc();
        long size = lexer.getAPsIntVal().getZExtValue();
        lexer.lex();

        if (parseToken(kw_x, "expected 'x' after element count"))
            return true;

        SMLoc typeLoc = lexer.getLoc();
        OutParamWrapper<Type> eltTy = new OutParamWrapper<>(LLVMContext.VoidTy);
        if (parseTypeRec(eltTy))
            return error(typeLoc, "array and vector element type can't be void");

        if (parseToken(isVector ? greater : rsquare, "expected end of sequential type"))
            return true;

        if (isVector)
        {
            if (size == 0)
                return error(sizeLoc, "zero element vector is illegal");
            if ((int)size!= size)
                return error(sizeLoc, "size too large for vector");
            return error(sizeLoc, "Currently, vector type is not supported");
        }
        else
        {
            if (!ArrayType.isValidElementType(eltTy.get()))
                return error(typeLoc, "invalid array element type");
            result.set(handleUpRefs(ArrayType.get(eltTy.get(), (int)size)));
        }
        return false;
    }

    private boolean parseDefine()
    {}
    /**
     * Validate the parsed LLVM module.
     * @return
     */
    private boolean validateEndOfModule()
    {
        return true;
    }
}
