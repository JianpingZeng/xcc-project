
//
// int.c
//
// Copyright (c) 2012 TJ Holowaychuk <tj@vision-media.ca>
//

#include <stdint.h>
#include "int.h"

// read

uint16_t
read_u16_be(unsigned char *buf) {
  uint16_t n = 0;
  n |= buf[0] << 8;
  n |= buf[1];
  return n;
}

uint16_t
read_u16_le(unsigned char *buf) {
  uint16_t n = 0;
  n |= buf[1] << 8;
  n |= buf[0];
  return n;
}

uint32_t
read_u32_be(unsigned char *buf) {
  uint32_t n = 0;
  n |= buf[0] << 24;
  n |= buf[1] << 16;
  n |= buf[2] << 8;
  n |= buf[3];
  return n;
}

uint32_t
read_u32_le(unsigned char *buf) {
  uint32_t n = 0;
  n |= buf[3] << 24;
  n |= buf[2] << 16;
  n |= buf[1] << 8;
  n |= buf[0];
  return n;
}

uint64_t
read_u64_be(unsigned char *buf) {
  uint64_t n = 0;
  n |= (uint64_t) buf[0] << 56;
  n |= (uint64_t) buf[1] << 48;
  n |= (uint64_t) buf[2] << 40;
  n |= (uint64_t) buf[3] << 32;
  n |= buf[4] << 24;
  n |= buf[5] << 16;
  n |= buf[6] << 8;
  n |= buf[7];
  return n;
}

uint64_t
read_u64_le(unsigned char *buf) {
  uint64_t n = 0;
  n |= (uint64_t) buf[7] << 56;
  n |= (uint64_t) buf[6] << 48;
  n |= (uint64_t) buf[5] << 40;
  n |= (uint64_t) buf[4] << 32;
  n |= buf[3] << 24;
  n |= buf[2] << 16;
  n |= buf[1] << 8;
  n |= buf[0];
  return n;
}

// write

void
write_u16_be(unsigned char *buf, uint16_t n) {
  buf[0] = n >> 8 & 0xff;
  buf[1] = n & 0xff;
}

void
write_u16_le(unsigned char *buf, uint16_t n) {
  buf[1] = n >> 8 & 0xff;
  buf[0] = n & 0xff;
}

void
write_u32_be(unsigned char *buf, uint32_t n) {
  buf[0] = n >> 24 & 0xff;
  buf[1] = n >> 16 & 0xff;
  buf[2] = n >> 8 & 0xff;
  buf[3] = n & 0xff;
}

void
write_u32_le(unsigned char *buf, uint32_t n) {
  buf[3] = n >> 24 & 0xff;
  buf[2] = n >> 16 & 0xff;
  buf[1] = n >> 8 & 0xff;
  buf[0] = n & 0xff;
}

void
write_u64_be(unsigned char *buf, uint64_t n) {
  buf[0] = n >> 56 & 0xff;
  buf[1] = n >> 48 & 0xff;
  buf[2] = n >> 40 & 0xff;
  buf[3] = n >> 32 & 0xff;
  buf[4] = n >> 24 & 0xff;
  buf[5] = n >> 16 & 0xff;
  buf[6] = n >> 8 & 0xff;
  buf[7] = n & 0xff;
}

void
write_u64_le(unsigned char *buf, uint64_t n) {
  buf[7] = n >> 56 & 0xff;
  buf[6] = n >> 48 & 0xff;
  buf[5] = n >> 40 & 0xff;
  buf[4] = n >> 32 & 0xff;
  buf[3] = n >> 24 & 0xff;
  buf[2] = n >> 16 & 0xff;
  buf[1] = n >> 8 & 0xff;
  buf[0] = n & 0xff;
}
