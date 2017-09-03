//RUN jlang-cc -fsyntax-only %s

int foo(a, b) int a, b;{return a + b;}

int foo2(b, a, c, d) int a, b; float c; double d; {return a + b;}