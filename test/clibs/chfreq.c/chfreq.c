
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include "chfreq.h"

static size_t
ssize (const char *str) {
  size_t size = 0;
  while ('\0' != str[size]) ++size;
  return size;
}

static int
find (uint32_t **mat, const char ch) {
  int idx = 0;
  uint32_t *cur = NULL;
  while ((cur = mat[idx])) {
    if (ch == (char) cur[0]) {
      return idx;
    } else { idx++; }
  }
  return -1;
}

uint32_t **
chfreq (const char *src) {
  uint32_t **mat = NULL;
  char ch = 0;
  size_t size = 1;
  int pos = 0;
  int i = 0;
  int idx = -1;

  // alloc
  mat = (uint32_t **) calloc(size, sizeof(uint32_t *));
  if (NULL == mat) { return NULL; }

  // build
  while ('\0' != (ch = src[i++])) {
    idx = find(mat, ch);
    if (-1 == idx) {
      idx = pos++;
      mat = (uint32_t **) realloc(mat, sizeof(uint32_t *) * ssize(src));
      mat[idx] = (uint32_t *) calloc(2, sizeof(uint32_t));
      mat[idx][0] = ch;
      mat[idx][1] = 1;
      size++;
    } else {
      mat[idx][1]++;
    }
  }

  mat[size] = NULL;

  return mat;
}

