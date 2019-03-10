
/**
 * `main.c' - chfreq
 *
 * copyright (c) 2014 joseph werle <joseph.werle@gmail.com>
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <string.h>
#include "chfreq.h"

static void
usage () {
  fprintf(stderr, "usage: chfreq [-hV] [-f format]\n");
}

static char *
read_stdin () {
  size_t bsize = 1024;
  size_t size = 1;
  char buf[bsize];
  char *res = (char *) malloc(sizeof(char) * bsize);
  char *tmp = NULL;

  // memory issue
  if (NULL == res) { return NULL; }

  // cap
  res[0] = '\0';

  // read
  if (NULL != fgets(buf, bsize, stdin)) {
    // store
    tmp = res;
    // resize
    size += (size_t) strlen(buf);
    // realloc
    res = (char *) realloc(res, size);

    // memory issues
    if (NULL == res) {
      free(tmp);
      return NULL;
    }

    // yield
    strcat(res, buf);

    return res;
  }

  free(res);

  return NULL;
}

int
main (int argc, char **argv) {
  uint32_t **freq = NULL;
  uint32_t *cur = NULL;
  char *buf = NULL;
  char *format = NULL;
  char *opt = 0;
  int cfirst = 1;

  if (1 == argc && 1 == isatty(0)) { return usage(), 1; }
  else if (ferror(stdin)) { return 1; }

  if (argc > 1) {
    while (*argv++) {
      opt = argv[0];
      switch (opt[0]) {
        case '-':
          switch (opt[1]) {
            case 'h': return usage(), 0;
            case 'V': return printf("%s\n", CHFREQ_VERSION), 0;
            case 'f': opt = *argv++; format = *argv++; break;
          }
          break;
      }
    }
  }

  if (NULL == format) {
    format = "%c | %d";
  }

  // get order of format
  {
    char ch = 0;
    int i = 0;
    while ('\0' != (ch = format[i++])) {
      if ('%' == ch) {
        if ('c' == format[i]) { cfirst = 1; break; }
        else { cfirst = 0; break; }
      }
    }
  }

  if (1 == isatty(0)) { return usage(), 1; }
  else if (ferror(stdin)) { return 1; }
  else {
    do {
      buf = read_stdin();
      if (NULL == buf) { return 1; }
      freq = chfreq(buf);
      for (int i = 0; (cur = freq[i]); ++i) {
        if (1 == cfirst) { printf(format, cur[0], cur[1]); }
        else { printf(format, cur[1], cur[0]); }
        printf("\n");
      }
    } while (NULL != buf);
  }
  return 0;
}
