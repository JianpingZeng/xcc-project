// RUN: jlang-cc -ast-print %s

#include <stddef.h>

char    test1(void) { return '\\'; }
char    test3(void) { return '\''; }
char    test5(void) { return '\a'; }
char    test7(void) { return '\b'; }
char    test9(void) { return '\e'; }
char    test11(void) { return '\f'; }
char    test13(void) { return '\n'; }
char    test15(void) { return '\r'; }
char    test17(void) { return '\t'; }
char    test19(void) { return '\v'; }

char    test21(void) { return 'c'; }
char    test23(void) { return '\x3'; }
