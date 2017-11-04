// RUN: jlang-cc -fsyntax-only -verify %s
int f4(*XX)(void); /* expected-error {{cannot return}} expected-warning {{type specifier missing, defaults to 'int'}} */