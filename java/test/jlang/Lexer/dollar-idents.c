// RUN: jlang-cc -dump-tokens %s 2> %t &&
// RUN: grep "identifier '\$A'" %t &&
// RUN: jlang-cc -dump-tokens -x assembler-with-cpp %s 2> %t &&
// RUN: grep "identifier 'A'" %t
// PR3808

$A
