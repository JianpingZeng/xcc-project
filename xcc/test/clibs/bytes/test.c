
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <assert.h>
#include "bytes.h"

void
equal(char *a, char *b) {
  if (strcmp(a, b)) {
    fprintf(stderr, "expected: %s\n", a);
    fprintf(stderr, "actual: %s\n", b);
    free(b);
    exit(1);
  }
  free(b);
}

void
test_string_to_bytes(void) {
  assert(100 == string_to_bytes("100"));
  assert(100 == string_to_bytes("100b"));
  assert(100 == string_to_bytes("100 bytes"));
  assert(1024 == string_to_bytes("1kb"));
  assert(1024 * 1024 == string_to_bytes("1mb"));
  assert(1024 * 1024 * 1024 == string_to_bytes("1gb"));
  assert(2 * 1024 == string_to_bytes("2kb"));
  assert(5 * 1024 * 1024 == string_to_bytes("5mb"));
}

void
test_bytes_to_string(void) {
  equal("100b", bytes_to_string(100));
  equal("1kb", bytes_to_string(1024));
  equal("1mb", bytes_to_string(1024 * 1024));
  equal("5mb", bytes_to_string(1024 * 1024 * 5));
  equal("1gb", bytes_to_string(1024 * 1024 * 1024));
  equal("5gb", bytes_to_string((long long) 1024 * 1024 * 1024 * 5));
}

int
main(void) {
  test_string_to_bytes();
  test_bytes_to_string();
  printf("\n  \e[32m\u2713 \e[90mok\e[0m\n\n");
  return 0;
}
