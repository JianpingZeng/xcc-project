/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;

import backend.support.LLVMContext;
import backend.value.*;

import java.util.ArrayList;

public class DIBuilder {
    public enum ComplexAddrKind {
        OpPlus(1),
        OpDeref(2);
        public final int value;
        ComplexAddrKind(int val) { value = val; }
    }

    private Module m;
    private LLVMContext context;
    /**
     * The compilation unit being processed.
     */
    private MDNode theCU;
    private MDNode tempEnumTypes;
    private MDNode tempRetainTypes;
    private MDNode tempSubprograms;
    private MDNode tempGVs;
    /**
     * llvm.dbg.declare
     */
    private Function declareFn;
    /**
     * llvm.dbg.value
     */
    private Function valueFn;

    private ArrayList<Value> allEnumTypes;
    private ArrayList<Value> allRetainTypes;
    private ArrayList<Value> allSubprograms;
    private ArrayList<Value> allGVs;

    public DIBuilder(Module m) {
        this.m = m;
        context = m.getContext();
        theCU = null;
        allEnumTypes = new ArrayList<>();
        allRetainTypes = new ArrayList<>();
        allSubprograms = new ArrayList<>();
        allGVs = new ArrayList<>();
    }

    public MDNode getCU() { return theCU; }

    /**
     * Collect any deferred debug info descriptors.
     */
    public void destroy() {
        DIArray enums = getOrCreateArray(allEnumTypes);
        new DIType(tempEnumTypes).replaceAllUsesWith(enums);
    }

    /**
     * Create a compilation unit which provides an anchor for all debugging
     * information generated during this instance of compilation.
     * @param lang  Source programming languages, e.g., DW_LANG_C89, DW_LANG_Java
     * @param file  The path to source file
     * @param dir   The path to the directory where source file resides
     * @param producer  The name of producer generating this compilation unit
     * @param isOptimized   If this source file has been optimized
     * @param flags This flag lists some command line options
     * @param rv    This indicates runtime version of languages, such as objective-C
     */
    public void createCompileUnit(int lang,
                                  String file,
                                  String dir,
                                  String producer,
                                  boolean isOptimized,
                                  String flags,
                                  int rv) {

    }

    public DIFile createFile(String filename, String directory) {}

    public DIEnumerator createEnumerator(String name, long val) {}

    /**
     * Create a c++11 nullptr type.
     * @param name
     * @return
     */
    public DIType createNullPtrType(String name) {}

    /**
     * Create debug info for a basic type with specified name, size and align size.
     * @param name
     * @param sizeInBits
     * @param alignInBits
     * @param encoding
     * @return
     */
    public DIType createBasicType(String name,
                                  long sizeInBits,
                                  long alignInBits,
                                  int encoding) {}

    /**
     * Create a debug info for qualified type. such as 'const|volatile int'
     * @param tag
     * @param fromTy
     * @return
     */
    public DIType createQualifiedType(int tag,
                                      DIType fromTy) {}

    /**
     * Creates a debug information entry for a pointer type points to
     * the {@param pointeeTy}.
     * @param pointeeTy
     * @param sizeInBits
     * @param alignInBits
     * @param name
     * @return
     */
    public DIType createPointerType(DIType pointeeTy,
                                    long sizeInBits,
                                    long alignInBits,
                                    String name) {}

    /**
     * Create a debug info entry for c++ reference type.
      * @param rty
     * @return
     */
    public DIType createReferenceType(DIType rty) {

    }

    public DIType createTypedef(DIType ty, String name,
                                DIFile file,
                                int lineNo,
                                DIDescriptor context) {}

    /**
     * Create a debug info entry for c++ 'friend' declaration.
     * @param ty
     * @param friendTy
     * @return
     */
    public DIType createFriend(DIType ty, DIType friendTy) {}

    /**
     * Create debugging information entry to establish inheritence
     * relationship between two types.
     * @param ty
     * @param baseTy
     * @param baseOffset
     * @param flags
     * @return
     */
    public DIType createInheritence(DIType ty,
                                    DIType baseTy,
                                    long baseOffset,
                                    int flags) {}

    /**
     * Create a debugging information entry for a c++ class member.
     * @param scope
     * @param name
     * @param file
     * @param lineNo
     * @param sizeInBits
     * @param alignInBits
     * @param offsetInBits
     * @param flags
     * @param ty
     * @return
     */
    public DIType createMemberType(DIDescriptor scope,
                                   String name,
                                   DIFile file,
                                   int lineNo,
                                   long sizeInBits,
                                   long alignInBits,
                                   long offsetInBits,
                                   int flags,
                                   DIType ty) {}

    /**
     * Create a debug info entry for Objective-C instance variable.
     * @param name  Member name
     * @param file  File where this member is defined
     * @param lineNo    Line number
     * @param sizeInBits    member size in bits
     * @param alignInBits   member alignment size in bits
     * @param offsetInBits  member offset
     * @param flags         Flags to encode member attribute, e.g., private
     * @param ty            Parent type
     * @param propertyName  Name of the Objective-C property associated with this ivar
     * @param propertyGetterName    Name of the Objective-C property getter selector
     * @param propertySetterName    Name of the Objective-C property setter selector
     * @param propertyAttributes    Objective-C property attributes.
     * @return
     */
    public DIType createObjCIVar(String name,
                                 DIFile file,
                                 int lineNo,
                                 long sizeInBits,
                                 long alignInBits,
                                 long offsetInBits,
                                 int flags,
                                 DIType ty,
                                 String propertyName,
                                 String propertyGetterName,
                                 String propertySetterName,
                                 String propertyAttributes) {}

    public DIType createObjCIVar(String name,
                                 DIFile file,
                                 int lineNo,
                                 long sizeInBits,
                                 long alignInBits,
                                 long offsetInBits,
                                 int flags,
                                 DIType ty,
                                 String propertyName,
                                 String propertyGetterName,
                                 String propertySetterName) {
        return createObjCIVar(name, file, lineNo, sizeInBits, alignInBits, offsetInBits,
                flags, ty, propertyName, propertyGetterName, propertySetterName, "");
    }

    public DIType createObjCIVar(String name,
                                 DIFile file,
                                 int lineNo,
                                 long sizeInBits,
                                 long alignInBits,
                                 long offsetInBits,
                                 int flags,
                                 DIType ty,
                                 String propertyName,
                                 String propertyGetterName) {
        return createObjCIVar(name, file, lineNo, sizeInBits, alignInBits,
                offsetInBits, flags, ty, propertyName, propertyGetterName, "");
    }

    public DIType createObjCIVar(String name,
                                 DIFile file,
                                 int lineNo,
                                 long sizeInBits,
                                 long alignInBits,
                                 long offsetInBits,
                                 int flags,
                                 DIType ty,
                                 String propertyName) {
        return createObjCIVar(name, file, lineNo, sizeInBits, alignInBits,
                offsetInBits, flags, ty, propertyName, "");
    }

    public DIType createObjCIVar(String name,
                                 DIFile file,
                                 int lineNo,
                                 long sizeInBits,
                                 long alignInBits,
                                 long offsetInBits,
                                 int flags,
                                 DIType ty) {
        return createObjCIVar(name, file, lineNo, sizeInBits, alignInBits,
                offsetInBits, flags, ty, "");
    }

    /**
     * Create a debug info entry for a class.
     * @param scope
     * @param name
     * @param file
     * @param lineNo
     * @param sizeInBits
     * @param alignInBits
     * @param flags
     * @param derivedFrom
     * @param elements
     * @param vtableHolder
     * @param templateParams
     * @return
     */
    public DIType createClassType(DIDescriptor scope,
                                  String name,
                                  DIFile file,
                                  int lineNo,
                                  long sizeInBits,
                                  long alignInBits,
                                  int flags,
                                  DIType derivedFrom,
                                  DIArray elements,
                                  MDNode vtableHolder,
                                  MDNode templateParams) {}

    public DIType createClassType(DIDescriptor scope,
                                  String name,
                                  DIFile file,
                                  int lineNo,
                                  long sizeInBits,
                                  long alignInBits,
                                  int flags,
                                  DIType derivedFrom,
                                  DIArray elements,
                                  MDNode vtableHolder) {
        return createClassType(scope, name, file, lineNo, sizeInBits, alignInBits,
                flags, derivedFrom, elements, vtableHolder, null);
    }

    public DIType createClassType(DIDescriptor scope,
                                  String name,
                                  DIFile file,
                                  int lineNo,
                                  long sizeInBits,
                                  long alignInBits,
                                  int flags,
                                  DIType derivedFrom,
                                  DIArray elements) {
        return createClassType(scope, name, file, lineNo, sizeInBits, alignInBits,
                flags, derivedFrom, elements, null);
    }

    /**
     * Create a debug info entry for a struct type.
     * @param scope
     * @param name
     * @param file
     * @param lineNo
     * @param sizeInBits
     * @param alignInBits
     * @param flags
     * @param elements
     * @param runtimeLang
     * @return
     */
    public DIType createStructType(DIDescriptor scope,
                                   String name,
                                   DIFile file,
                                   int lineNo,
                                   long sizeInBits,
                                   long alignInBits,
                                   int flags,
                                   DIArray elements,
                                   int runtimeLang) {}

    public DIType createStructType(DIDescriptor scope,
                                   String name,
                                   DIFile file,
                                   int lineNo,
                                   long sizeInBits,
                                   long alignInBits,
                                   int flags,
                                   DIArray elements) {
        return createStructType(scope, name, file, lineNo, sizeInBits, alignInBits,
                flags, elements, 0);
    }

    /**
     * Create a debug info entry for an union type.
     * @param scope
     * @param name
     * @param file
     * @param lineNo
     * @param sizeInBits
     * @param alignInBits
     * @param flags
     * @param elements
     * @param runtimeLang
     * @return
     */
    public DIType createUnionType(DIDescriptor scope,
                                   String name,
                                   DIFile file,
                                   int lineNo,
                                   long sizeInBits,
                                   long alignInBits,
                                   int flags,
                                   DIArray elements,
                                   int runtimeLang) {}

    public DIType createUnionType(DIDescriptor scope,
                                   String name,
                                   DIFile file,
                                   int lineNo,
                                   long sizeInBits,
                                   long alignInBits,
                                   int flags,
                                   DIArray elements) {
        return createUnionType(scope, name, file, lineNo, sizeInBits, alignInBits,
                flags, elements, 0);
    }

    /**
     * Create debugging info entry for template type parameter.
     * @param scope
     * @param name
     * @param ty
     * @param file
     * @param lineNo
     * @param columnNo
     * @return
     */
    public DITemplateTypeParameter createTemplateTypeParameter(DIDescriptor scope,
                                                               String name,
                                                               DIType ty,
                                                               MDNode file,
                                                               int lineNo,
                                                               int columnNo) {}

    public DITemplateTypeParameter createTemplateTypeParameter(DIDescriptor scope,
                                                               String name,
                                                               DIType ty,
                                                               MDNode file,
                                                               int lineNo) {
        return createTemplateTypeParameter(scope, name, ty, file, lineNo, 0);
    }

    public DITemplateTypeParameter createTemplateTypeParameter(DIDescriptor scope,
                                                               String name,
                                                               DIType ty,
                                                               MDNode file) {
        return createTemplateTypeParameter(scope, name, ty, file, 0);
    }
    public DITemplateTypeParameter createTemplateTypeParameter(DIDescriptor scope,
                                                               String name,
                                                               DIType ty) {
        return createTemplateTypeParameter(scope, name, ty, null);
    }

    /**
     * Create a debugging info entry for template value parameter.
     * @param scope
     * @param name
     * @param value
     * @param file
     * @param lineNo
     * @return
     */
    public DITemplateTypeParameter createTemplateValueParameter(DIDescriptor scope,
                                                               String name,
                                                               long value,
                                                               MDNode file,
                                                               int lineNo,
                                                               int columnNo) {
        
    }

    public DITemplateTypeParameter createTemplateValueParameter(DIDescriptor scope,
                                                                String name,
                                                                long value,
                                                                MDNode file,
                                                                int lineNo) {
        return createTemplateValueParameter(scope, name, value,file, lineNo, 0);
    }
    public DITemplateTypeParameter createTemplateValueParameter(DIDescriptor scope,
                                                                String name,
                                                                long value,
                                                                MDNode file) {
        return createTemplateValueParameter(scope, name, value, file, 0);
    }
    public DITemplateTypeParameter createTemplateValueParameter(DIDescriptor scope,
                                                                String name,
                                                                long value) {
        return createTemplateValueParameter(scope, name, value, null);
    }

    /**
     * Create debugging formation entry for an array.
     * @param size  The static size of an array.
     * @param alignInBits   Alignment in size
     * @param ty    Element type
     * @param subscripts    Subscripts
     * @return
     */
    public DIType createArrayType(long size, long alignInBits, DIType ty,
                                  DIArray subscripts) {}

    /**
     * Create debugging information entry for a vector.
     * @param size  The static size of an vector.
     * @param alignInBits   Alignment in size
     * @param ty    Element type
     * @param subscripts    Subscripts
     * @return
     */
    public DIType createVectorType(long size, long alignInBits,
                                   DIType ty, DIArray subscripts) {}

    /**
     * Create debugging information entry for an enumeartion.
     * @param scope The parent scope where the enumeration is defined or declared.
     * @param name  The name of enumeration if it has one
     * @param file  File where this member is defined
     * @param lineNo    Line number
     * @param sizeInBits    member size
     * @param alignInBits   Alignment
     * @param elements  Enumeration elements list
     * @return
     */
    public DIType createEnumerationType(DIDescriptor scope,
                                        String name,
                                        DIFile file,
                                        int lineNo,
                                        long sizeInBits,
                                        long alignInBits,
                                        DIArray elements) {}

    /**
     * Create a subroutine type.
     * @param file  File where this subroutine is defined
     * @param parameterTypes    An array of function formal argument types. This
     *                          includes the return type as the 0-th element
     * @return
     */
    public DIType createSubroutineType(DIFile file,
                                       DIArray parameterTypes) {}

    /**
     * Create an new DIType with "artificial" flag set
     * @param ty
     * @return
     */
    public DIType createArtificialType(DIType ty) {}

    public DIType createTemporaryType() {}

    public DIType createTemporaryType(DIFile file) {}

    public void retainType(DIType ty) {}

    public DIDescriptor createUnspecifiedParameter() {}

    public DIArray getOrCreateArray(ArrayList<Value> elements) {}
    public DIArray getOrCreateArray(Value... elements) {}
    public DISubrange getOrCreateSubrange(long lo, long hi) {}

    /**
     * Create a new descriptor for the specified global variable with initialized value.
     * @param name  Name of variable
     * @param file  File where this variable is defined
     * @param lineNo    Line number
     * @param ty    Variable type
     * @param isLocalToUnit Is local to the compilation unit
     * @param val   Initialization value
     * @return
     */
    public DIGlobalVariable createGlobalVariable(String name,
                                                 DIFile file,
                                                 int lineNo,
                                                 DIType ty,
                                                 boolean isLocalToUnit,
                                                 Value val) {}

    public DIGlobalVariable createStaticVariable(DIDescriptor context,
                                                 String name,
                                                 String linkageName,
                                                 DIFile file,
                                                 int lineNo,
                                                 DIType ty,
                                                 boolean isLocalToUnit,
                                                 Value val) {}

    public DIVariable createLocalVariable(int tag,
                                          DIDescriptor scope,
                                          String name,
                                          DIFile file,
                                          int lineNo,
                                          DIType ty,
                                          boolean alwaysPreserve,
                                          int flags,
                                          int argNo) {
    }

    public DIVariable createLocalVariable(int tag,
                                          DIDescriptor scope,
                                          String name,
                                          DIFile file,
                                          int lineNo,
                                          DIType ty,
                                          boolean alwaysPreserve,
                                          int flags) {
        return createLocalVariable(tag, scope, name, file,
                lineNo, ty, alwaysPreserve, flags, 0);
    }

    public DIVariable createLocalVariable(int tag,
                                          DIDescriptor scope,
                                          String name,
                                          DIFile file,
                                          int lineNo,
                                          DIType ty,
                                          boolean alwaysPreserve) {
        return createLocalVariable(tag, scope, name, file,
                lineNo, ty, alwaysPreserve, 0);
    }

    public DIVariable createLocalVariable(int tag,
                                          DIDescriptor scope,
                                          String name,
                                          DIFile file,
                                          int lineNo,
                                          DIType ty) {
        return createLocalVariable(tag, scope, name, file, lineNo, ty, false);
    }

    /**
     * Create a new descriptor for the specified variable which has a complex
     * address expression for its address.
     * @param tag
     * @param scope
     * @param name
     * @param file
     * @param lineNo
     * @param ty
     * @param addr
     * @param argNo
     * @return
     */
    public DIVariable createComplexVariable(int tag,
                                            DIDescriptor scope,
                                            String name,
                                            DIFile file,
                                            int lineNo,
                                            DIType ty,
                                            ArrayList<Value> addr,
                                            int argNo) {}
    public DIVariable createComplexVariable(int tag,
                                            DIDescriptor scope,
                                            String name,
                                            DIFile file,
                                            int lineNo,
                                            DIType ty,
                                            ArrayList<Value> addr) {
        return createComplexVariable(tag, scope, name, file, lineNo, ty, addr, 0);
    }

    /**
     * Create a new descriptor for the specified subprogram.
     * @param scope         Function scope
     * @param name          Function name
     * @param linkageName   Mangled function name
     * @param file          File where this function is defined
     * @param lineNo        Line number
     * @param ty            Function type
     * @param isLocalToUnit True if this function is not visible to external unit
     * @param isDefinition  True if this is a function definition instead of declaration
     * @param flags         This flags are used to emit dwarf attributes, such as prototype or not
     * @param isOptimized   True if optimization is on
     * @param fn            Function pointer
     * @param tparm         Function template parameters if applicable
     * @param decl
     * @return
     */
    public DISubrange createFunction(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int flags,
                                     boolean isOptimized,
                                     Function fn,
                                     MDNode tparm,
                                     MDNode decl) {}

    public DISubrange createFunction(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int flags,
                                     boolean isOptimized,
                                     Function fn,
                                     MDNode tparm) {
        return createFunction(scope, name, linkageName, file, lineNo,
                ty, isLocalToUnit, isDefinition, flags, isOptimized,
                fn, tparm, null);
    }

    public DISubrange createFunction(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int flags,
                                     boolean isOptimized,
                                     Function fn) {
        return createFunction(scope, name, linkageName, file, lineNo,
                ty, isLocalToUnit, isDefinition, flags, isOptimized,
                fn, null);
    }

    public DISubrange createFunction(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int flags,
                                     boolean isOptimized) {
        return createFunction(scope, name, linkageName, file, lineNo,
                ty, isLocalToUnit, isDefinition, flags, isOptimized,
                null);
    }

    public DISubrange createFunction(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int flags) {
        return createFunction(scope, name, linkageName, file, lineNo,
                ty, isLocalToUnit, isDefinition, flags, false);
    }

    /**
     * Create a new descriptor for the c++/Java member method.
     * @param scope         Function scope
     * @param name          Function name
     * @param linkageName   Mangled function name
     * @param file          File where this function is defined
     * @param lineNo        Line number
     * @param ty            Function type
     * @param isLocalToUnit True if this function is not visible to external unit
     * @param isDefinition  True if this is a function definition instead of declaration
     * @param virtuality    Attributes describing virtualness of function
     * @param vtableIndex   Index number of this method in virtual table
     * @param vtableHolder  Type that holds vtable
     * @param flags         This flags are used to emit dwarf attributes, such as prototype or not
     * @param isOptimized   True if optimization is on
     * @param fn            Function pointer
     * @param tparm         Function template parameters if applicable
     * @return
     */
    public DISubprogram createMethod(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int virtuality,
                                     int vtableIndex,
                                     MDNode vtableHolder,
                                     int flags,
                                     boolean isOptimized,
                                     Function fn,
                                     MDNode tparm) {}

    public DISubprogram createMethod(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int virtuality,
                                     int vtableIndex,
                                     MDNode vtableHolder,
                                     int flags,
                                     boolean isOptimized,
                                     Function fn) {
        return createMethod(scope, name, linkageName, file, lineNo,
                ty, isLocalToUnit, isDefinition, virtuality, vtableIndex,
                vtableHolder, flags, isOptimized, fn, null);
    }

    public DISubprogram createMethod(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int virtuality,
                                     int vtableIndex,
                                     MDNode vtableHolder,
                                     int flags,
                                     boolean isOptimized) {
        return createMethod(scope, name, linkageName, file, lineNo,
                ty, isLocalToUnit, isDefinition, virtuality, vtableIndex,
                vtableHolder, flags, isOptimized, null);
    }

    public DISubprogram createMethod(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int virtuality,
                                     int vtableIndex,
                                     MDNode vtableHolder,
                                     int flags) {
        return createMethod(scope, name, linkageName, file, lineNo,
                ty, isLocalToUnit, isDefinition, virtuality, vtableIndex,
                vtableHolder, flags, false);
    }

    public DISubprogram createMethod(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int virtuality,
                                     int vtableIndex,
                                     MDNode vtableHolder) {
        return createMethod(scope, name, linkageName, file, lineNo,
                ty, isLocalToUnit, isDefinition, virtuality, vtableIndex,
                vtableHolder, 0);
    }
    public DISubprogram createMethod(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int virtuality,
                                     int vtableIndex) {
        return createMethod(scope, name, linkageName, file, lineNo,
                ty, isLocalToUnit, isDefinition, virtuality, vtableIndex,
                null);
    }

    public DISubprogram createMethod(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition,
                                     int virtuality) {
        return createMethod(scope, name, linkageName, file, lineNo,
                ty, isLocalToUnit, isDefinition, virtuality, 0);
    }

    public DISubprogram createMethod(DIDescriptor scope,
                                     String name,
                                     String linkageName,
                                     DIFile file,
                                     int lineNo,
                                     DIType ty,
                                     boolean isLocalToUnit,
                                     boolean isDefinition) {
        return createMethod(scope, name, linkageName, file, lineNo,
                ty, isLocalToUnit, isDefinition, 0);
    }

    public DINameSpace createNameSpace(DIDescriptor scope,
                                       String name,
                                       DIFile file,
                                       int lineNo) {}

    /**
     * This creates a descriptor for a lexical block with a new file attached.
     * This 
     * @param scope
     * @param file
     * @return
     */
    public DILexicalBlockFile createLexicalBlockFile(DIDescriptor scope,
                                                     DIFile file) {}


    public DILexicalBlock createLexicalBlock(DIDescriptor scope, DIFile file,
                                      int line, int col) {}

    /**
     * Insert a new llvm.dbg.declare intrinsic call.
     *
     * @param storage     Value of the variable
     * @param varInfo     Variable's debug info descriptor.
     * @param insertAtEnd Location for the new intrinsic.
     */
    public Instruction insertDeclare(Value storage, DIVariable varInfo,
                                     BasicBlock insertAtEnd) {
    }

    /**
     * Insert a new llvm.dbg.declare intrinsic call.
     *
     * @param storage      Value of the variable
     * @param varInfo      Variable's debug info descriptor.
     * @param insertBefore Location for the new intrinsic.
     */
    public Instruction insertDeclare(Value storage, DIVariable varInfo,
                                     Instruction insertBefore) {
    }


    /**
     * Insert a new llvm.dbg.value intrinsic call.
     *
     * @param val         Value of the variable
     * @param offset      Offset
     * @param varInfo     Variable's debug info descriptor.
     * @param insertAtEnd Location for the new intrinsic.
     */
    public Instruction insertDbgValueIntrinsic(Value val, long offset,
                                               DIVariable varInfo,
                                               BasicBlock insertAtEnd) {
    }

    /**
     * Insert a new llvm.dbg.value intrinsic call.
     *
     * @param val          Value of the variable
     * @param offset       Offset
     * @param varInfo      Variable's debug info descriptor.
     * @param insertBefore Location for the new intrinsic.
     */
    public Instruction insertDbgValueIntrinsic(Value val, long offset,
                                               DIVariable varInfo,
                                               Instruction insertBefore) {
    }
}
