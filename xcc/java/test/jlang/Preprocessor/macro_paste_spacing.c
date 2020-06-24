// RUN: jlang-cc %s -E | grep "^xy$"

#define A  x ## y
blah

A

