
//
// int.h
//
// Copyright (c) 2012 TJ Holowaychuk <tj@vision-media.ca>
//

#ifndef INT
#define INT

#include <stdint.h>

// read

uint16_t
read_u16_be(unsigned char *);

uint16_t
read_u16_le(unsigned char *);

uint32_t
read_u32_be(unsigned char *);

uint32_t
read_u32_le(unsigned char *);

uint64_t
read_u64_be(unsigned char *);

uint64_t
read_u64_le(unsigned char *);

// write

void
write_u16_be(unsigned char *, uint16_t);

void
write_u16_le(unsigned char *, uint16_t);

void
write_u32_be(unsigned char *, uint32_t);

void
write_u32_le(unsigned char *, uint32_t);

void
write_u64_be(unsigned char *, uint64_t);

void
write_u64_le(unsigned char *, uint64_t);

#endif
