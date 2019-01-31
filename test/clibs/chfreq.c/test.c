
#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <assert.h>
#include "chfreq.h"

int
main (void) {
  char *str = NULL;
  uint32_t **freq = NULL;
  uint32_t *cur = NULL;

  str = "aabbccddeeffgghhiijjkkllmmnnooppqqrrssttuuvvwwxxyyzz";
  freq = chfreq(str);

  for (int i = 0; NULL != (cur = freq[i]); ++i) {
    assert(2 == cur[1]);
  }

  str = "kinkajou's are awesome";
  freq = chfreq(str);

  for (int i = 0; NULL != (cur = freq[i]); ++i) {
    switch (cur[0]) {
      case 'k': assert(2 == cur[1]); break;
      case 'i': assert(1 == cur[1]); break;
      case 'n': assert(1 == cur[1]); break;
      case 'a': assert(3 == cur[1]); break;
      case 'j': assert(1 == cur[1]); break;
      case 'o': assert(2 == cur[1]); break;
      case 'u': assert(1 == cur[1]); break;
      case '\'': assert(1 == cur[1]); break;
      case 'r': assert(1 == cur[1]); break;
      case 'e': assert(3 == cur[1]); break;
      case 'w': assert(1 == cur[1]); break;
      case 's': assert(2 == cur[1]); break;
      case 'm': assert(1 == cur[1]); break;
      case ' ': assert(2 == cur[1]); break;
    }
  }

  return 0;
}
