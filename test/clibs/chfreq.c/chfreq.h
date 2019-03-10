
/**
 * `chfreq.h' - chfreq
 *
 * copyright (c) 2014 joseph werle <joseph.werle@gmail.com>
 */

#ifndef CHFREQ_H
#define CHFREQ_H 1

#define CHFREQ_VERSION "0.0.1"

/**
 * Returns an array of `uint32_t' type
 * arrays who's character is the first
 * index and the occurence is the second
 * index.
 *
 * example:
 *
 *  mat = {
 *    {'a', 2}
 *  }
 */

uint32_t **
chfreq (const char *);

#endif
