
//
// rgba.h
//
// Copyright (c) 2012 TJ Holowaychuk <tj@vision-media.ca>
//

#ifndef RGBA_H
#define RGBA_H

#include <stdint.h>

/*
 * RGBA struct.
 */

typedef struct {
  double r, g, b, a;
} rgba_t;

// protos

rgba_t
rgba_new(uint32_t rgba);

uint32_t
rgba_from_string(const char *str, short *ok);

void
rgba_to_string(rgba_t rgba, char *buf, size_t len);

void
rgba_inspect(uint32_t rgba);

#endif /* RGBA_H */