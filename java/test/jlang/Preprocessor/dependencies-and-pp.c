// RUN: jlang -E -o %t.1 %s &&
// RUN: jlang -E -MD -MF %t.d -MT foo -o %t.2 %s &&
// RUN: diff %t.1 %t.2 &&
// RUN: grep "foo:" %t.d &&
// RUN: grep "dependencies-and-pp.c" %t.d
