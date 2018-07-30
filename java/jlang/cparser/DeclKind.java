package jlang.cparser;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public enum DeclKind
{
    EnumConstant("EnumConstant"),
    EnumDecl("Enum"),
    FunctionDecl("Function"),
    FieldDecl("Field"),
    FileScopeAsmDecl("FileScopeAsmDecl"),
    RecordDecl("Struct"),
    TranslationUnitDecl("TranslationUnit"),
    LabelDecl("Label"),
    OriginalParamVar("OriginalParam"),
    ParamVarDecl("Param"),
    TypedefDecl("Typedef"),
    VarDecl("Var");

    public String declKindName;

    DeclKind(String name)
    {
        declKindName = name;
    }
}
