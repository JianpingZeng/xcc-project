// RUN: jlang-cc %s -E | grep 'bar foo (2)' &&
// RUN: jlang-cc %s -E | grep 'm(ABCD)'

#define foo(x) bar x
foo(foo) (2)


#define m(a) a(w)
#define w ABCD
m(m)   // m(ABCD)

