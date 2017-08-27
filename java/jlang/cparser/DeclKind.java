package jlang.cparser;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public enum DeclKind
{
    TranslationUnitDecl("TranslationUnit"),
    FunctionDecl("Function"),
    RecordDecl("Struct"),
    EnumDecl("Enum"),
    TypedefDecl("Typedef"),
    EnumConstant("EnumConstant"),
    LabelDecl("Label"),
    FieldDecl("Field"),
    VarDecl("Var"),
    ParamVarDecl("Param"),
    OriginalParamVar("OriginalParam"),
    FileScopeAsmDecl("FileScopeAsmDecl");

    public String declKindName;

    DeclKind(String name)
    {
        declKindName = name;
    }
}
