// RUN: clang-cc -fsyntax-only -verify %s -pedantic -std=c99

int x;

int y;   // expected-warning {{defaults to 'int'}}

// PR2796
int (__attribute__(()) *z)(long y);
// rdar://6131260
int foo42(void) {
  int x, __attribute__((unused)) y, z;
  return 0;
}


