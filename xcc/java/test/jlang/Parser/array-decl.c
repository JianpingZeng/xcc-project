// RUN: jlang-cc -fsyntax-only %s
struct str;

void test2(int *P, int A) {
  struct str;

  // Hard case for array decl, not Array[*].
  int Array[*(int*)P+A];
}