package jlang.cparser;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public enum DeclKind
{
    TranslationUnitDecl("TranslationUnit"),
    FunctionDecl("Function"),
    StructDecl("Struct"),
    EnumDecl("Enum"),
    TypedefDecl("Typedef"),
    EnumConstant("EnumConstant"),
    LabelDecl("Label"),
    FieldDecl("Field"),
    VarDecl("Var"),
    ParamVar("Param"),
    OriginalParamVar("OriginalParam"),
    FileScopeAsm("FileScopeAsm");

    public String declKindName;

    DeclKind(String name)
    {
        declKindName = name;
    }
}
