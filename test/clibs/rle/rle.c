
#include <stdio.h>
#include <string.h>
#include "int/int.h"
#include "rle.h"

#define MATCH(A, B) \
  ((A).iov_len == (B)->iov_len && memcmp((A).iov_base, (B)->iov_base, (B)->iov_len) == 0)

size_t
rle_encode(struct iovec *in, int count, char *out) {
  if (count < 1) return 0;

  struct iovec *prev = &in[0];
  size_t off = 0;
  size_t n = 1;

  for (size_t i = 1; i <= count; i++) {
    if (i == count || !MATCH(in[i], prev)) {
      out[off++] = n;
      out[off++] = prev->iov_len;
      memcpy(out+off, prev->iov_base, prev->iov_len);
      off += prev->iov_len;
      prev = &in[i];
      n = 0;
    }

    n++;
  }

  return off;
}

void
rle_decode(char *in, size_t len, struct iovec *out) {
  size_t off = 0;
  for (size_t i = 0; i < len;) {
    size_t n = in[i++];
    size_t size = in[i++];
    for (size_t j = 0; j < n; j++) {
      out[off].iov_base = in+i;
      out[off].iov_len = size;
      off++;
    }
    i += size;
  }
}
