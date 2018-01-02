// RUN: jlang-cc %s -Eonly 2>&1 | grep error &&
// RUN: jlang-cc %s -Eonly 2>&1 | not grep unterminated &&
// RUN: jlang-cc %s -Eonly 2>&1 | not grep scratch

#define COMM / ## *
COMM

