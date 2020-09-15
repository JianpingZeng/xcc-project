/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

package backend.debug;

import backend.intrinsic.Intrinsic;
import backend.support.LLVMContext;
import backend.type.Type;
import backend.value.*;
import tools.Util;

import java.util.ArrayList;
import java.util.Arrays;

public class DIBuilder {
  public enum ComplexAddrKind {
    OpPlus(1),
    OpDeref(2);
    public final int value;

    ComplexAddrKind(int val) {
      value = val;
    }
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

  public MDNode getCU() {
    return theCU;
  }

  /**
   * Collect any deferred debug info descriptors.
   */
  public void destroy() {
    DIArray enums = getOrCreateArray(allEnumTypes);
    new DIType(tempEnumTypes).replaceAllUsesWith(enums);

    DIArray retainType = getOrCreateArray(allRetainTypes);
    new DIType(tempRetainTypes).replaceAllUsesWith(retainType);
    ;

    DIArray sps = getOrCreateArray(allSubprograms);
    new DIType(tempSubprograms).replaceAllUsesWith(sps);
    ;
    for (int i = 0, e = sps.getNumElements(); i < e; ++i) {
      DISubprogram subprogram = new DISubprogram(sps.getElement(i).getDbgNode());
      NamedMDNode nmd = getFnSpecificMDNode(m, subprogram);
      if (nmd != null) {
        ArrayList<Value> variables = new ArrayList<>();
        for (int ii = 0, ee = nmd.getNumOfOperands(); ii < ee; ++ii) {
          variables.add(nmd.getOperand(ii));
        }
        MDNode temp = subprogram.getVariableNodes();
        if (temp != null) {
          DIArray av = getOrCreateArray(variables);
          new DIType(temp).replaceAllUsesWith(av);
        }
        nmd.eraseFromParent();
      }
    }
    DIArray gvs = getOrCreateArray(allGVs);
    new DIType(tempGVs).replaceAllUsesWith(gvs);
  }

  /**
   * If {@code n} is a compilation unit, return it other null.
   *
   * @param n
   * @return
   */
  static MDNode getNonCompileUnitScope(MDNode n) {
    if (new DIDescriptor(n).isCompileUnit())
      return null;
    return n;
  }

  /**
   * Return a NamedMDNode, if it is available, holding function
   * specific information.
   *
   * @param m
   * @param sp
   * @return
   */
  static NamedMDNode getFnSpecificMDNode(Module m,
                                         DISubprogram sp) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("llvm.dbg.lv.");
    String fname = "fn";
    if (sp.getFunction() != null)
      fname = sp.getFunction().getName();
    else
      fname = sp.getName();
    if (fname.startsWith("\1"))
      fname = fname.substring(1);
    fixupObjcLikeName(fname, buffer);
    return m.getNamedMetadata(buffer.toString());
  }

  static NamedMDNode getOrInsertFnSpecificMDNode(Module m,
                                                 DISubprogram sp) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("llvm.dbg.lv.");
    String fname = "fn";
    if (sp.getFunction() != null)
      fname = sp.getFunction().getName();
    else
      fname = sp.getName();
    if (fname.startsWith("\1"))
      fname = fname.substring(1);
    fixupObjcLikeName(fname, buffer);
    return m.getOrCreateNamedMetadata(buffer.toString());
  }

  /**
   * Replace some special characters in ObjC language, such as,
   * '[', ']', etc, with '.'.
   *
   * @param str The input string
   * @param out The output result without special characters.
   */
  static void fixupObjcLikeName(String str,
                                StringBuilder out) {
    boolean isObjcLike = false;
    for (int i = 0, e = str.length(); i < e; ++i) {
      char ch = str.charAt(i);
      if (ch == '[')
        isObjcLike = true;
      if (isObjcLike && (ch == '[' || ch == ']' ||
          ch == ' ' || ch == ':' || ch == '+' ||
          ch == '(' || ch == ')'))
        out.append('.');
      else
        out.append(ch);
    }
  }

  private static Constant getTagConstant(LLVMContext context,
                                         int tag) {
    Util.assertion((tag & Dwarf.LLVMDebugVersionMask) == 0,
        "Tag too large for debug encoding?");
    return ConstantInt.get(Type.getInt32Ty(context), tag | Dwarf.LLVMDebugVersion);
  }

  /**
   * Create a compilation unit which provides an anchor for all debugging
   * information generated during this instance of compilation.
   *
   * @param lang           Source programming languages, e.g., DW_LANG_C89, DW_LANG_Java
   * @param file           The path to source file
   * @param dir            The path to the directory where source file resides
   * @param producer       The name of producer generating this compilation unit
   * @param isOptimized    If this source file has been optimized
   * @param flags          This flag lists some command line options
   * @param runtimeVersion This indicates runtime version of languages, such as objective-C
   */
  public void createCompileUnit(int lang,
                                String file,
                                String dir,
                                String producer,
                                boolean isOptimized,
                                String flags,
                                int runtimeVersion) {
    Util.assertion(lang <= Dwarf.DW_LANG_D &&
        lang >= Dwarf.DW_LANG_C89, "Invalid Language tag");
    Util.assertion(!file.isEmpty(),
        "Unable to create a compilation unit with empty filename");
    Value[] telts = new Value[]{getTagConstant(context, Dwarf.DW_TAG_base_type)};
    tempEnumTypes = MDNode.getTemporary(context, telts);
    Value[] thElts = new Value[]{tempEnumTypes};
    MDNode enumHolder = MDNode.get(context, thElts);

    tempRetainTypes = MDNode.getTemporary(context, telts);
    Value[] trElts = new Value[]{tempRetainTypes};
    MDNode retainHolder = MDNode.get(context, trElts);

    tempSubprograms = MDNode.getTemporary(context, telts);
    Value[] tsElts = new Value[]{tempSubprograms};
    MDNode spHolder = MDNode.get(context, tsElts);

    tempGVs = MDNode.getTemporary(context, telts);
    Value[] tvElts = new Value[]{tempGVs};
    MDNode gvHolder = MDNode.get(context, tvElts);

    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_compile_unit),
        Constant.getNullValue(Type.getInt32Ty(context)),
        ConstantInt.get(Type.getInt32Ty(context), lang),
        MDString.get(context, file),
        MDString.get(context, dir),
        MDString.get(context, producer),
        // isMain field
        ConstantInt.get(Type.getInt1Ty(context), 1),
        ConstantInt.get(Type.getInt1Ty(context), isOptimized ? 1 : 0),
        MDString.get(context, flags),
        ConstantInt.get(Type.getInt32Ty(context), runtimeVersion),
        enumHolder,
        retainHolder,
        spHolder,
        gvHolder
    };
    // create a named metadata so that it is easier to find
    // out the compilation unit in a module.
    theCU = new DICompileUnit(MDNode.get(context, elts)).getDbgNode();
    NamedMDNode nmd = m.getOrCreateNamedMetadata("llvm. dbg.cu");
    nmd.addOperand(theCU);
  }

  /**
   * Create a file descriptor
   *
   * @param filename
   * @param directory
   * @return
   */
  public DIFile createFile(String filename, String directory) {
    Util.assertion(theCU != null,
        "Unable to create a DW_TAG_file_type without CompileUnit!");
    Util.assertion(!filename.isEmpty(), "Unable to create an empty file");
    ;
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_file_type),
        MDString.get(context, filename),
        MDString.get(context, directory)
    };
    return new DIFile(MDNode.get(context, elts));
  }

  /**
   * Create a single enumerator value.
   *
   * @param name
   * @param val
   * @return
   */
  public DIEnumerator createEnumerator(String name, long val) {
    Util.assertion(!name.isEmpty(), "Unable to create an empty enumerator!");
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_enumerator),
        MDString.get(context, name),
        ConstantInt.get(Type.getInt64Ty(context), val)
    };
    return new DIEnumerator(MDNode.get(context, elts));
  }

  /**
   * Create a c++0x nullptr type.
   *
   * @param name
   * @return
   */
  public DIType createNullPtrType(String name) {
    Util.assertion(!name.isEmpty(), "Unable to create an empty type!");

    // nullptr is encoded in DIBasicType format.
    // { Line number, filename, size, align, offset, flags, encoding }
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_unspecified_type),
        null, // the cu
        MDString.get(context, name),
        null, // filename,
        ConstantInt.get(Type.getInt32Ty(context), 0), // line
        ConstantInt.get(Type.getInt64Ty(context), 0), // size
        ConstantInt.get(Type.getInt64Ty(context), 0), // align
        ConstantInt.get(Type.getInt64Ty(context), 0), // offset
        ConstantInt.get(Type.getInt32Ty(context), 0), // flags
        ConstantInt.get(Type.getInt32Ty(context), 0), // encoding
    };
    return new DIType(MDNode.get(context, elts));
  }

  /**
   * Create debug info for a basic type with specified name, size and align size.
   *
   * @param name
   * @param sizeInBits
   * @param alignInBits
   * @param encoding
   * @return
   */
  public DIType createBasicType(String name,
                                long sizeInBits,
                                long alignInBits,
                                int encoding) {
    Util.assertion(!name.isEmpty(), "Unable to create an empty type!");

    // DIBasicType format.
    // { Line number, filename, size, align, offset, flags, encoding }
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_base_type),
        null, // the cu
        MDString.get(context, name),
        null, // filename,
        ConstantInt.get(Type.getInt32Ty(context), 0), // line
        ConstantInt.get(Type.getInt64Ty(context), sizeInBits), // size
        ConstantInt.get(Type.getInt64Ty(context), alignInBits), // align
        ConstantInt.get(Type.getInt64Ty(context), 0), // offset
        ConstantInt.get(Type.getInt32Ty(context), 0), // flags
        ConstantInt.get(Type.getInt32Ty(context), encoding), // encoding
    };
    return new DIType(MDNode.get(context, elts));
  }

  /**
   * Create a debug info for qualified type. such as 'const|volatile int'
   *
   * @param tag
   * @param fromTy
   * @return
   */
  public DIType createQualifiedType(int tag,
                                    DIType fromTy) {
    // DIDerivedType format.
    Value[] elts = new Value[]{
        getTagConstant(context, tag),
        null, // the cu
        MDString.get(context, ""), // empty name
        null, // filename,
        ConstantInt.get(Type.getInt32Ty(context), 0), // line
        ConstantInt.get(Type.getInt64Ty(context), 0), // size
        ConstantInt.get(Type.getInt64Ty(context), 0), // align
        ConstantInt.get(Type.getInt64Ty(context), 0), // offset
        ConstantInt.get(Type.getInt32Ty(context), 0), // flags
        fromTy.getDbgNode()
    };
    return new DIType(MDNode.get(context, elts));
  }

  /**
   * Creates a debug information entry for a pointer type points to
   * the {@param pointeeTy}.
   *
   * @param pointeeTy
   * @param sizeInBits
   * @param alignInBits
   * @param name
   * @return
   */
  public DIType createPointerType(DIType pointeeTy,
                                  long sizeInBits,
                                  long alignInBits,
                                  String name) {
    Util.assertion(!name.isEmpty(), "Unable to create an empty pointer type!");
    // DIDerivedType format.
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_pointer_type),
        null, // the cu
        MDString.get(context, name),
        null, // filename,
        ConstantInt.get(Type.getInt32Ty(context), 0), // line
        ConstantInt.get(Type.getInt64Ty(context), sizeInBits), // size
        ConstantInt.get(Type.getInt64Ty(context), alignInBits), // align
        ConstantInt.get(Type.getInt64Ty(context), 0), // offset
        ConstantInt.get(Type.getInt32Ty(context), 0), // flags
        pointeeTy.getDbgNode()
    };
    return new DIType(MDNode.get(context, elts));
  }

  /**
   * Create a debug info entry for c++ reference type.
   *
   * @param rty
   * @return
   */
  public DIType createReferenceType(DIType rty) {
    Util.assertion(rty.verify(), "Unable to create an unverified reference type!");
    // DIDerivedType format.
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_reference_type),
        null, // the cu
        null, // name
        null, // filename,
        ConstantInt.get(Type.getInt32Ty(context), 0), // line
        ConstantInt.get(Type.getInt64Ty(context), 0), // size
        ConstantInt.get(Type.getInt64Ty(context), 0), // align
        ConstantInt.get(Type.getInt64Ty(context), 0), // offset
        ConstantInt.get(Type.getInt32Ty(context), 0), // flags
        rty.getDbgNode()
    };
    return new DIType(MDNode.get(context, elts));
  }

  public DIType createTypedef(DIType ty,
                              String name,
                              DIFile file,
                              int lineNo,
                              DIDescriptor context) {
    Util.assertion(!name.isEmpty(), "Unable to create an empty typedef!");
    // DIDerivedType format.
    Value[] elts = new Value[]{
        getTagConstant(this.context, Dwarf.DW_TAG_typedef),
        getNonCompileUnitScope(context.getDbgNode()),
        MDString.get(this.context, name),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(this.context), lineNo), // line
        ConstantInt.get(Type.getInt64Ty(this.context), 0), // size
        ConstantInt.get(Type.getInt64Ty(this.context), 0), // align
        ConstantInt.get(Type.getInt64Ty(this.context), 0), // offset
        ConstantInt.get(Type.getInt32Ty(this.context), 0), // flags
        ty.getDbgNode()
    };
    return new DIType(MDNode.get(this.context, elts));
  }

  /**
   * Create a debug info entry for c++ 'friend' declaration.
   *
   * @param ty
   * @param friendTy
   * @return
   */
  public DIType createFriend(DIType ty, DIType friendTy) {
    Util.assertion(friendTy.verify(), "Invalid type!");
    // DIDerivedType format.
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_friend),
        ty.getDbgNode(), // the cu
        null, // name
        ty.getFile().getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), 0), // line
        ConstantInt.get(Type.getInt64Ty(context), 0), // size
        ConstantInt.get(Type.getInt64Ty(context), 0), // align
        ConstantInt.get(Type.getInt64Ty(context), 0), // offset
        ConstantInt.get(Type.getInt32Ty(context), 0), // flags
        friendTy.getDbgNode()
    };
    return new DIType(MDNode.get(context, elts));
  }

  /**
   * Create debugging information entry to establish inheritence
   * relationship between two types.
   *
   * @param ty
   * @param baseTy
   * @param baseOffset
   * @param flags
   * @return
   */
  public DIType createInheritence(DIType ty,
                                  DIType baseTy,
                                  long baseOffset,
                                  int flags) {
    Util.assertion(ty.verify(), "Invalid Inheritence type!");
    // DIDerivedType format.
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_inheritance),
        ty.getDbgNode(), // the cu
        null, // name
        ty.getFile().getDbgNode(), // filename,
        ConstantInt.get(Type.getInt32Ty(context), 0), // line
        ConstantInt.get(Type.getInt64Ty(context), 0), // size
        ConstantInt.get(Type.getInt64Ty(context), 0), // align
        ConstantInt.get(Type.getInt64Ty(context), baseOffset), // offset
        ConstantInt.get(Type.getInt32Ty(context), flags), // flags
        baseTy.getDbgNode()
    };
    return new DIType(MDNode.get(context, elts));
  }

  /**
   * Create a debugging information entry for a c++ class member.
   *
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
                                 DIType ty) {
    // DIDerivedType format.
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_member),
        getNonCompileUnitScope(scope.getDbgNode()), // the cu
        MDString.get(context, name),
        file.getDbgNode(), // filename,
        ConstantInt.get(Type.getInt32Ty(context), lineNo), // line
        ConstantInt.get(Type.getInt64Ty(context), sizeInBits), // size
        ConstantInt.get(Type.getInt64Ty(context), alignInBits), // align
        ConstantInt.get(Type.getInt64Ty(context), offsetInBits), // offset
        ConstantInt.get(Type.getInt32Ty(context), flags), // flags
        ty.getDbgNode()
    };
    return new DIType(MDNode.get(context, elts));
  }

  /**
   * Create a debug info entry for Objective-C instance variable.
   *
   * @param name               Member name
   * @param file               File where this member is defined
   * @param lineNo             Line number
   * @param sizeInBits         member size in bits
   * @param alignInBits        member alignment size in bits
   * @param offsetInBits       member offset
   * @param flags              Flags to encode member attribute, e.g., private
   * @param ty                 Parent type
   * @param propertyName       Name of the Objective-C property associated with this ivar
   * @param propertyGetterName Name of the Objective-C property getter selector
   * @param propertySetterName Name of the Objective-C property setter selector
   * @param propertyAttributes Objective-C property attributes.
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
                               int propertyAttributes) {
    // DIDerivedType format.
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_member),
        getNonCompileUnitScope(file.getDbgNode()), // the cu
        MDString.get(context, name),
        file.getDbgNode(), // filename,
        ConstantInt.get(Type.getInt32Ty(context), lineNo), // line
        ConstantInt.get(Type.getInt64Ty(context), sizeInBits), // size
        ConstantInt.get(Type.getInt64Ty(context), alignInBits), // align
        ConstantInt.get(Type.getInt64Ty(context), offsetInBits), // offset
        ConstantInt.get(Type.getInt32Ty(context), flags), // flags
        ty.getDbgNode(),
        MDString.get(context, propertyName),
        MDString.get(context, propertyGetterName),
        MDString.get(context, propertySetterName),
        ConstantInt.get(Type.getInt32Ty(context), propertyAttributes)
    };
    return new DIType(MDNode.get(context, elts));
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
                               String propertyGetterName,
                               String propertySetterName) {
    return createObjCIVar(name, file, lineNo, sizeInBits, alignInBits, offsetInBits,
        flags, ty, propertyName, propertyGetterName, propertySetterName, 0);
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
   *
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
                                long offsetInBits,
                                int flags,
                                DIType derivedFrom,
                                DIArray elements,
                                MDNode vtableHolder,
                                MDNode templateParams) {
    // DICompositeType format
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_class_type),
        getNonCompileUnitScope(scope.getDbgNode()),
        MDString.get(context, name),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), lineNo),
        ConstantInt.get(Type.getInt64Ty(context), sizeInBits),
        ConstantInt.get(Type.getInt64Ty(context), alignInBits),
        ConstantInt.get(Type.getInt64Ty(context), offsetInBits),
        ConstantInt.get(Type.getInt32Ty(context), flags),
        derivedFrom.getDbgNode(),
        elements.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), 0),
        vtableHolder,
        templateParams
    };
    return new DIType(MDNode.get(context, elts));
  }

  public DIType createClassType(DIDescriptor scope,
                                String name,
                                DIFile file,
                                int lineNo,
                                long sizeInBits,
                                long alignInBits,
                                long offsetInBits,
                                int flags,
                                DIType derivedFrom,
                                DIArray elements,
                                MDNode vtableHolder) {
    return createClassType(scope, name, file, lineNo, sizeInBits, alignInBits,
        offsetInBits, flags, derivedFrom, elements, vtableHolder, null);
  }

  public DIType createClassType(DIDescriptor scope,
                                String name,
                                DIFile file,
                                int lineNo,
                                long sizeInBits,
                                long alignInBits,
                                long offsetInBits,
                                int flags,
                                DIType derivedFrom,
                                DIArray elements) {
    return createClassType(scope, name, file, lineNo, sizeInBits, alignInBits,
        offsetInBits, flags, derivedFrom, elements, null);
  }

  /**
   * Create a debug info entry for a struct type.
   *
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
                                 int runtimeLang) {
    // DICompositeType format
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_structure_type),
        getNonCompileUnitScope(scope.getDbgNode()),
        MDString.get(context, name),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), lineNo),
        ConstantInt.get(Type.getInt64Ty(context), sizeInBits),
        ConstantInt.get(Type.getInt64Ty(context), alignInBits),
        ConstantInt.get(Type.getInt64Ty(context), 0),
        ConstantInt.get(Type.getInt32Ty(context), flags),
        Constant.getNullValue(Type.getInt32Ty(context)),
        elements.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), runtimeLang),
        Constant.getNullValue(Type.getInt32Ty(context)),
    };
    return new DIType(MDNode.get(context, elts));
  }

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
   *
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
                                int runtimeLang) {
    // DICompositeType format
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_union_type),
        getNonCompileUnitScope(scope.getDbgNode()),
        MDString.get(context, name),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), lineNo),
        ConstantInt.get(Type.getInt64Ty(context), sizeInBits),
        ConstantInt.get(Type.getInt64Ty(context), alignInBits),
        ConstantInt.get(Type.getInt64Ty(context), 0),
        ConstantInt.get(Type.getInt32Ty(context), flags),
        Constant.getNullValue(Type.getInt32Ty(context)),
        elements.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), runtimeLang),
        Constant.getNullValue(Type.getInt32Ty(context)),
    };
    return new DIType(MDNode.get(context, elts));
  }

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
   *
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
                                                             int columnNo) {
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_template_type_parameter),
        getNonCompileUnitScope(scope.getDbgNode()),
        MDString.get(context, name),
        ty.getDbgNode(),
        file,
        ConstantInt.get(Type.getInt32Ty(context), lineNo),
        ConstantInt.get(Type.getInt32Ty(context), columnNo),
    };
    return new DITemplateTypeParameter(MDNode.get(context, elts));
  }

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
   *
   * @param scope
   * @param name
   * @param value
   * @param file
   * @param lineNo
   * @return
   */
  public DITemplateValueParameter createTemplateValueParameter(DIDescriptor scope,
                                                               String name,
                                                               DIType ty,
                                                               long value,
                                                               MDNode file,
                                                               int lineNo,
                                                               int columnNo) {
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_template_value_parameter),
        getNonCompileUnitScope(scope.getDbgNode()),
        MDString.get(context, name),
        ty.getDbgNode(),
        ConstantInt.get(Type.getInt64Ty(context), value),
        file,
        ConstantInt.get(Type.getInt32Ty(context), lineNo),
        ConstantInt.get(Type.getInt32Ty(context), columnNo),
    };
    return new DITemplateValueParameter(MDNode.get(context, elts));
  }

  public DITemplateValueParameter createTemplateValueParameter(DIDescriptor scope,
                                                               String name,
                                                               DIType ty,
                                                               long value,
                                                               MDNode file,
                                                               int lineNo) {
    return createTemplateValueParameter(scope, name, ty, value, file, lineNo, 0);
  }

  public DITemplateValueParameter createTemplateValueParameter(DIDescriptor scope,
                                                               String name,
                                                               DIType ty,
                                                               long value,
                                                               MDNode file) {
    return createTemplateValueParameter(scope, name, ty, value, file, 0);
  }

  public DITemplateValueParameter createTemplateValueParameter(DIDescriptor scope,
                                                               String name,
                                                               DIType ty,
                                                               long value) {
    return createTemplateValueParameter(scope, name, ty, value, null);
  }

  /**
   * Create debugging formation entry for an array.
   *
   * @param size        The static size of an array.
   * @param alignInBits Alignment in size
   * @param ty          Element type
   * @param subscripts  Subscripts
   * @return
   */
  public DIType createArrayType(long size,
                                long alignInBits,
                                DIType ty,
                                DIArray subscripts) {
    // DICompositeType format.
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_array_type),
        null, // theCU,
        MDString.get(context, ""),  // name
        null, // filename
        ConstantInt.get(Type.getInt32Ty(context), 0), // line no
        ConstantInt.get(Type.getInt64Ty(context), size), // size
        ConstantInt.get(Type.getInt64Ty(context), alignInBits), // alignment
        ConstantInt.get(Type.getInt32Ty(context), 0),
        ConstantInt.get(Type.getInt32Ty(context), 0),
        ty.getDbgNode(),
        subscripts.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), 0),
        Constant.getNullValue(Type.getInt32Ty(context))
    };
    return new DIType(MDNode.get(context, elts));
  }

  /**
   * Create debugging information entry for a vector.
   *
   * @param size        The static size of an vector.
   * @param alignInBits Alignment in size
   * @param ty          Element type
   * @param subscripts  Subscripts
   * @return
   */
  public DIType createVectorType(long size,
                                 long alignInBits,
                                 DIType ty,
                                 DIArray subscripts) {
    // DICompositeType format.
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_vector_type),
        null, // theCU,
        MDString.get(context, ""),  // name
        null, // filename
        ConstantInt.get(Type.getInt32Ty(context), 0), // line no
        ConstantInt.get(Type.getInt64Ty(context), size), // size
        ConstantInt.get(Type.getInt64Ty(context), alignInBits), // alignment
        ConstantInt.get(Type.getInt32Ty(context), 0),
        ConstantInt.get(Type.getInt32Ty(context), 0),
        ty.getDbgNode(),
        subscripts.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), 0),
        Constant.getNullValue(Type.getInt32Ty(context))
    };
    return new DIType(MDNode.get(context, elts));
  }

  /**
   * Create debugging information entry for an enumeration.
   *
   * @param scope       The parent scope where the enumeration is defined or declared.
   * @param name        The name of enumeration if it has one
   * @param file        File where this member is defined
   * @param lineNo      Line number
   * @param sizeInBits  member size
   * @param alignInBits Alignment
   * @param elements    Enumeration elements list
   * @return
   */
  public DIType createEnumerationType(DIDescriptor scope,
                                      String name,
                                      DIFile file,
                                      int lineNo,
                                      long sizeInBits,
                                      long alignInBits,
                                      DIArray elements) {
    // DICompositeType format.
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_enumeration_type),
        getNonCompileUnitScope(scope.getDbgNode()),
        MDString.get(context, name),  // name
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), lineNo), // line no
        ConstantInt.get(Type.getInt64Ty(context), sizeInBits), // size
        ConstantInt.get(Type.getInt64Ty(context), alignInBits), // alignment
        ConstantInt.get(Type.getInt32Ty(context), 0),
        ConstantInt.get(Type.getInt32Ty(context), 0),
        Constant.getNullValue(Type.getInt32Ty(context)),
        elements.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), 0),
        Constant.getNullValue(Type.getInt32Ty(context))
    };
    MDNode res = MDNode.get(context, elts);
    allEnumTypes.add(res);
    return new DIType(res);
  }

  /**
   * Create a subroutine type.
   *
   * @param file           File where this subroutine is defined
   * @param parameterTypes An array of function formal argument types. This
   *                       includes the return type as the 0-th element
   * @return
   */
  public DIType createSubroutineType(DIFile file,
                                     DIArray parameterTypes) {
    // DICompositeType format.
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_subroutine_type),
        Constant.getNullValue(Type.getInt32Ty(context)),
        MDString.get(context, null),  // name
        Constant.getNullValue(Type.getInt32Ty(context)),
        ConstantInt.get(Type.getInt32Ty(context), 0), // line no
        ConstantInt.get(Type.getInt64Ty(context), 0), // size
        ConstantInt.get(Type.getInt64Ty(context), 0), // alignment
        ConstantInt.get(Type.getInt32Ty(context), 0),
        ConstantInt.get(Type.getInt32Ty(context), 0),
        Constant.getNullValue(Type.getInt32Ty(context)),
        parameterTypes.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), 0),
        Constant.getNullValue(Type.getInt32Ty(context))
    };
    return new DIType(MDNode.get(context, elts));
  }

  /**
   * Create an new DIType with "artificial" flag set
   *
   * @param ty
   * @return
   */
  public DIType createArtificialType(DIType ty) {
    if (ty.isArtifical()) return ty;

    Value[] elts = new Value[9];
    MDNode n = ty.getDbgNode();
    Util.assertion(n != null, "Unexpected input DIType!");
    for (int i = 0, e = n.getNumOfOperands(); i < e; ++i) {
      Value v = n.operand(i);
      if (v != null)
        elts[i] = v;
      else
        elts[i] = Constant.getNullValue(Type.getInt32Ty(context));
    }
    int flags = ty.getFlags();
    flags |= DIType.FlagArtificial;
    elts[8] = ConstantInt.get(Type.getInt32Ty(context), flags);
    return new DIType(MDNode.get(context, elts));
  }

  public DIType createTemporaryType() {
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_base_type)
    };
    return new DIType(MDNode.get(context, elts));
  }

  public DIType createTemporaryType(DIFile file) {
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_base_type),
        theCU,
        null,
        file.getDbgNode()
    };
    return new DIType(MDNode.get(context, elts));
  }

  public void retainType(DIType ty) {
    allRetainTypes.add(ty.getDbgNode());
  }

  public DIDescriptor createUnspecifiedParameter() {
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_unspecified_parameters)
    };
    return new DIType(MDNode.get(context, elts));
  }

  public DIArray getOrCreateArray(ArrayList<Value> elements) {
    if (elements.isEmpty()) {
      Value nullVal = Constant.getNullValue(Type.getInt32Ty(context));
      return new DIArray(MDNode.get(context, nullVal));
    }
    return new DIArray(MDNode.get(context, elements));
  }

  public DIArray getOrCreateArray(Value... elements) {
    if (elements.length <= 0) {
      Value nullVal = Constant.getNullValue(Type.getInt32Ty(context));
      return new DIArray(MDNode.get(context, nullVal));
    }
    return new DIArray(MDNode.get(context, elements));
  }

  public DISubrange getOrCreateSubrange(long lo, long hi) {
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_subrange_type),
        ConstantInt.get(Type.getInt64Ty(context), lo),
        ConstantInt.get(Type.getInt64Ty(context), hi)
    };
    return new DISubrange(MDNode.get(context, elts));
  }

  /**
   * Create a new descriptor for the specified global variable with initialized value.
   *
   * @param name          Name of variable
   * @param file          File where this variable is defined
   * @param lineNo        Line number
   * @param ty            Variable type
   * @param isLocalToUnit Is local to the compilation unit
   * @param val           Initialization value
   * @return
   */
  public DIGlobalVariable createGlobalVariable(String name,
                                               DIFile file,
                                               int lineNo,
                                               DIType ty,
                                               boolean isLocalToUnit,
                                               Value val) {
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_variable),
        Constant.getNullValue(Type.getInt32Ty(context)),
        null,
        MDString.get(context, name),
        MDString.get(context, name),
        MDString.get(context, name),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), lineNo),
        ty.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), isLocalToUnit ? 1 : 0),
        ConstantInt.get(Type.getInt32Ty(context), 1), // is definition
        val
    };
    MDNode node = MDNode.get(context, elts);
    allGVs.add(node);
    return new DIGlobalVariable(node);
  }

  public DIGlobalVariable createStaticVariable(DIDescriptor context,
                                               String name,
                                               String linkageName,
                                               DIFile file,
                                               int lineNo,
                                               DIType ty,
                                               boolean isLocalToUnit,
                                               Value val) {
    Value[] elts = new Value[]{
        getTagConstant(this.context, Dwarf.DW_TAG_variable),
        Constant.getNullValue(Type.getInt32Ty(this.context)),
        getNonCompileUnitScope(context.getDbgNode()),
        MDString.get(this.context, name),
        MDString.get(this.context, name),
        MDString.get(this.context, linkageName),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(this.context), lineNo),
        ty.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(this.context), isLocalToUnit ? 1 : 0),
        ConstantInt.get(Type.getInt32Ty(this.context), 1), // is definition
        val
    };
    MDNode node = MDNode.get(this.context, elts);
    allGVs.add(node);
    return new DIGlobalVariable(node);
  }

  public DIVariable createLocalVariable(int tag,
                                        DIDescriptor scope,
                                        String name,
                                        DIFile file,
                                        int lineNo,
                                        DIType ty,
                                        boolean alwaysPreserve,
                                        int flags,
                                        int argNo) {
    Value[] elts = new Value[]{
        getTagConstant(context, tag),
        getNonCompileUnitScope(scope.getDbgNode()),
        MDString.get(context, name),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), lineNo | (argNo << 24)),
        ty.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), flags),
        Constant.getNullValue(Type.getInt32Ty(context))
    };
    MDNode node = MDNode.get(context, elts);
    if (alwaysPreserve) {
      // Compiler optimizations might remove local variable.
      // If there is an interest to preserve variable info
      // in named mdnode.
      DISubprogram fn = new DISubprogram(scope.getDbgNode());
      NamedMDNode fnLocals = getOrInsertFnSpecificMDNode(m, fn);
      fnLocals.addOperand(node);
    }
    return new DIVariable(node);
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
   *
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
                                          int argNo) {
    Value[] elts = new Value[]{
        getTagConstant(context, tag),
        getNonCompileUnitScope(scope.getDbgNode()),
        MDString.get(context, name),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), (lineNo | (argNo << 24))),
        ty.getDbgNode(),
        Constant.getNullValue(Type.getInt32Ty(context)),
        Constant.getNullValue(Type.getInt32Ty(context)),
    };
    ArrayList<Value> tempList = new ArrayList<>();
    tempList.addAll(Arrays.asList(elts));
    tempList.addAll(addr);
    return new DIVariable(MDNode.get(context, tempList));
  }

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
   *
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
  public DISubprogram createFunction(DIDescriptor scope,
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
                                     MDNode decl) {
    Value[] telts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_base_type),
    };
    MDNode temp = MDNode.getTemporary(context, telts);
    Value[] tvElts = new Value[]{temp};
    MDNode thHolder = MDNode.get(context, tvElts);
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_subprogram),
        Constant.getNullValue(Type.getInt32Ty(context)),
        getNonCompileUnitScope(scope.getDbgNode()),
        MDString.get(context, name),
        MDString.get(context, name),
        MDString.get(context, linkageName),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), lineNo),
        ty.getDbgNode(),
        ConstantInt.get(Type.getInt1Ty(context), isLocalToUnit ? 1 : 0),
        ConstantInt.get(Type.getInt1Ty(context), isDefinition ? 1 : 0),
        ConstantInt.get(Type.getInt32Ty(context), 0),
        ConstantInt.get(Type.getInt32Ty(context), 0),
        Constant.getNullValue(Type.getInt32Ty(context)),
        ConstantInt.get(Type.getInt32Ty(context), flags),
        ConstantInt.get(Type.getInt1Ty(context), isOptimized ? 1 : 0),
        fn,
        tparm,
        decl,
        thHolder
    };
    MDNode node = MDNode.get(context, elts);
    allSubprograms.add(node);
    return new DISubprogram(node);
  }

  public DISubprogram createFunction(DIDescriptor scope,
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

  public DISubprogram createFunction(DIDescriptor scope,
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

  public DISubprogram createFunction(DIDescriptor scope,
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

  public DISubprogram createFunction(DIDescriptor scope,
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
   *
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
                                   MDNode tparm) {
    Value[] telts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_base_type),
    };
    MDNode temp = MDNode.getTemporary(context, telts);
    Value[] tvElts = new Value[]{temp};
    MDNode thHolder = MDNode.get(context, tvElts);
    Value[] elts = new Value[]{
        getTagConstant(context, Dwarf.DW_TAG_subprogram),
        Constant.getNullValue(Type.getInt32Ty(context)),
        getNonCompileUnitScope(scope.getDbgNode()),
        MDString.get(context, name),
        MDString.get(context, name),
        MDString.get(context, linkageName),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), lineNo),
        ty.getDbgNode(),
        ConstantInt.get(Type.getInt1Ty(context), isLocalToUnit ? 1 : 0),
        ConstantInt.get(Type.getInt1Ty(context), isDefinition ? 1 : 0),
        ConstantInt.get(Type.getInt32Ty(context), virtuality),
        ConstantInt.get(Type.getInt32Ty(context), vtableIndex),
        vtableHolder,
        ConstantInt.get(Type.getInt32Ty(context), flags),
        ConstantInt.get(Type.getInt1Ty(context), isOptimized ? 1 : 0),
        fn,
        tparm,
        Constant.getNullValue(Type.getInt32Ty(context)),
        thHolder
    };
    MDNode node = MDNode.get(context, elts);
    allSubprograms.add(node);
    return new DISubprogram(node);
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
                                     int lineNo) {
    Value[] elts = new Value[] {
        getTagConstant(context, Dwarf.DW_TAG_namespace),
        getNonCompileUnitScope(scope.getDbgNode()),
        MDString.get(context, name),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), lineNo)
    };
    return new DINameSpace(MDNode.get(context, elts));
  }

  /**
   * This creates a descriptor for a lexical block with a new file attached.
   * This
   *
   * @param scope
   * @param file
   * @return
   */
  public DILexicalBlockFile createLexicalBlockFile(DIDescriptor scope,
                                                   DIFile file) {
    Value[] elts = new Value[] {
        getTagConstant(context, Dwarf.DW_TAG_lexical_block),
        scope.getDbgNode(),
        file.getDbgNode()
    };
    return new DILexicalBlockFile(MDNode.get(context, elts));
  }

  private static int uniqueId = 0;
  public DILexicalBlock createLexicalBlock(DIDescriptor scope,
                                           DIFile file,
                                           int line,
                                           int col) {
    Value[] elts = new Value[] {
        getTagConstant(context, Dwarf.DW_TAG_lexical_block),
        getNonCompileUnitScope(scope.getDbgNode()),
        ConstantInt.get(Type.getInt32Ty(context), line),
        ConstantInt.get(Type.getInt32Ty(context), col),
        file.getDbgNode(),
        ConstantInt.get(Type.getInt32Ty(context), uniqueId++),
    };
    return new DILexicalBlock(MDNode.get(context, elts));
  }

  /**
   * Insert a new llvm.dbg.declare intrinsic call.
   *
   * @param storage     Value of the variable
   * @param varInfo     Variable's debug info descriptor.
   * @param insertAtEnd Location for the new intrinsic.
   */
  public Instruction insertDeclare(Value storage,
                                   DIVariable varInfo,
                                   BasicBlock insertAtEnd) {
    Util.assertion(storage != null, "no storage passed to dbg.declare");
    Util.assertion(varInfo.verify(), "empty DIVariable passed to dbg.declare");
    if (declareFn == null)
      declareFn = Intrinsic.getDeclaration(context, m, Intrinsic.ID.dbg_declare);

    Value[] args = new Value[] {MDNode.get(storage.getContext(), storage), varInfo.getDbgNode()};
    Instruction.TerminatorInst ti = insertAtEnd.getTerminator();
    if (ti != null)
      return Instruction.CallInst.create(declareFn, args, "", ti);
    else
      return Instruction.CallInst.create(declareFn, args, "", insertAtEnd);
  }

  /**
   * Insert a new llvm.dbg.declare intrinsic call.
   *
   * @param storage      Value of the variable
   * @param varInfo      Variable's debug info descriptor.
   * @param insertBefore Location for the new intrinsic.
   */
  public Instruction insertDeclare(Value storage,
                                   DIVariable varInfo,
                                   Instruction insertBefore) {
    Util.assertion(storage != null, "no storage passed to dbg.declare");
    Util.assertion(varInfo.verify(), "empty DIVariable passed to dbg.declare");
    if (declareFn == null)
      declareFn = Intrinsic.getDeclaration(context, m, Intrinsic.ID.dbg_declare);

    Value[] args = new Value[] {MDNode.get(storage.getContext(), storage), varInfo.getDbgNode()};
    return Instruction.CallInst.create(declareFn, args, "", insertBefore);
  }


  /**
   * Insert a new llvm.dbg.value intrinsic call.
   *
   * @param val         Value of the variable
   * @param offset      Offset
   * @param varInfo     Variable's debug info descriptor.
   * @param insertAtEnd Location for the new intrinsic.
   */
  public Instruction insertDbgValueIntrinsic(Value val,
                                             long offset,
                                             DIVariable varInfo,
                                             BasicBlock insertAtEnd) {
    Util.assertion(val != null, "no value passed to dbg.value");
    Util.assertion(varInfo.verify(), "invalid DIVariable passed to dbg.value");
    if (valueFn == null)
      valueFn = Intrinsic.getDeclaration(context, m, Intrinsic.ID.dbg_value);
    Value[] args = new Value[] {
        MDNode.get(val.getContext(), val),
        ConstantInt.get(Type.getInt64Ty(context), offset),
        varInfo.getDbgNode()
    };
    Instruction.TerminatorInst ti = insertAtEnd.getTerminator();
    if (ti != null)
      return Instruction.CallInst.create(valueFn, args, "", ti);
    else
      return Instruction.CallInst.create(valueFn, args, "", insertAtEnd);
  }

  /**
   * Insert a new llvm.dbg.value intrinsic call.
   *
   * @param val          Value of the variable
   * @param offset       Offset
   * @param varInfo      Variable's debug info descriptor.
   * @param insertBefore Location for the new intrinsic.
   */
  public Instruction insertDbgValueIntrinsic(Value val,
                                             long offset,
                                             DIVariable varInfo,
                                             Instruction insertBefore) {
    Util.assertion(val != null, "no value passed to dbg.value");
    Util.assertion(varInfo.verify(), "invalid DIVariable passed to dbg.value");
    if (valueFn == null)
      valueFn = Intrinsic.getDeclaration(context, m, Intrinsic.ID.dbg_value);
    Value[] args = new Value[] {
        MDNode.get(val.getContext(), val),
        ConstantInt.get(Type.getInt64Ty(context), offset),
        varInfo.getDbgNode()
    };
    return Instruction.CallInst.create(valueFn, args, "", insertBefore);
  }
}
