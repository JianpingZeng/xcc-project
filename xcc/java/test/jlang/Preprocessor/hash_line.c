// The 1 and # should not go on the same line.
// RUN: jlang-cc %s -E | not grep "1 #" &&
// RUN: jlang-cc %s -E | grep '^1$' &&
// RUN: jlang-cc %s -E | grep '^      #$'
1
#define EMPTY
EMPTY #

