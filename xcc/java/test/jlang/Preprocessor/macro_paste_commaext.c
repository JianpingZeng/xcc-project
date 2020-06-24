// RUN: jlang-cc %s -E | grep 'V);' &&
// RUN: jlang-cc %s -E | grep 'W, 1, 2);' &&
// RUN: jlang-cc %s -E | grep 'X, 1, 2);' &&
// RUN: jlang-cc %s -E | grep 'Y, );' &&
// RUN: jlang-cc %s -E | grep 'Z, );'

#define debug(format, ...) format, ## __VA_ARGS__)
debug(V);
debug(W, 1, 2);
debug(X, 1, 2 );
debug(Y, );
debug(Z,);

