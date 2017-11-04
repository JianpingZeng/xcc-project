#define VariadicMacro(ID, NAME,...) #ID #NAME

char* str = VariadicMacro(X, Y);