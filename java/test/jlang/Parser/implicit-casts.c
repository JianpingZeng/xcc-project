// RUN: jlang-cc -fsyntax-only -verify -pedantic %s
double X;
void test1(int c) {
  X = 5;
}
void test2() {
  int i = 0;
  double d = i;
}
int test3() {
  int a[2];
  a[0] = test3; // expected-warning{{incompatible pointer to integer conversion assigning 'int ()', expected 'int'}}
  return 0;
}
short x; void test4(char c) { x += c; }
int y; void test5(char c) { y += c; }
