
#ifndef RLE_H
#define RLE_H

#include <sys/uio.h>
#include "int/int.h"

size_t
rle_encode(struct iovec *in, int count, char *out);

void
rle_decode(char *in, size_t len, struct iovec *out);

#endif /* RLE_H */
