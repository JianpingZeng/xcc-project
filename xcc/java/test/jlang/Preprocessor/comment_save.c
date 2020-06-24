// RUN: jlang-cc -E -C %s | grep '^// foo$' &&
// RUN: jlang-cc -E -C %s | grep -F '^/* bar */$'

// foo
/* bar */


