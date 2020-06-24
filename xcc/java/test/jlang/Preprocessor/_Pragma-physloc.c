// RUN: jlang-cc %s -E | grep '#pragma x y z' &&
// RUN: jlang-cc %s -E | grep '#pragma a b c'

_Pragma("x y z")
_Pragma("a b c")

