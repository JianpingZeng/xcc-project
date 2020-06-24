
#include <assert.h>
#include <string.h> // strcmp
#include <stdlib.h> // NULL, free
#include "strndup.h"

int
main(void) {
  char *copy = NULL;
  char str[] = "abcdefghijklmnopqrstuvwxyz";

  copy = strndup(str, 10);
  assert(0 == strcmp("abcdefghij", copy));
  free(copy);

  copy = strndup(str, 5000);
  assert('\0' == copy[5000]);
  free(copy);

  return 0;
}