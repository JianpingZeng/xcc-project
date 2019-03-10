
#include <assert.h>
#include <unistd.h> // sleep
#include "timestamp.h"

int
main(void) {
  int64_t start = timestamp();
  sleep(5);
  int64_t end = timestamp();
  int diff = end - start;
  // meh..
  assert(diff >= 5000);
  assert(diff < 5010);
  return 0;
}
