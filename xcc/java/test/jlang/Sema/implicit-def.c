/* RUN: jlang-cc -fsyntax-only %s -std=c89 &&
 * RUN: not jlang-cc -fsyntax-only %s -std=c99 -pedantic-errors
 */

int A() {
  return X();
}

