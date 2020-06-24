
#ifndef BENCH_H
#define BENCH_H 1

/**
 * Get the current wall time.
 */

float
wall(void);

/**
 * Get the current cpu time.
 */

float
cpu(void);

#ifdef __APPLE__

#include <mach/mach_time.h>

float
wall(void) {
  double multiplier = 0;
  if (multiplier <= 0) {
    mach_timebase_info_data_t info;
    mach_timebase_info(&info);
    multiplier = (double) info.numer / (double) info.denom / 1000000000.0;
  }
  return (float) (multiplier * mach_absolute_time());
}

float
cpu(void) {
  return wall();
}

#else

#ifdef __linux__
#include <linux/time.h>
#else
#include <time.h>
#endif


#ifdef CLOCK_MONOTONIC_RAW
#  define CLOCK_SUITABLE CLOCK_MONOTONIC_RAW
#else
#  define CLOCK_SUITABLE CLOCK_MONOTONIC
#endif

float
wall(void) {
  struct timespec tp;
  clock_gettime(CLOCK_SUITABLE, &tp);
  return tp.tv_sec + 1e-9 * tp.tv_nsec;
}

float
cpu(void) {
  struct timespec tp;
  clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &tp);
  return tp.tv_sec + 1e-9 * tp.tv_nsec;
}

#endif

#endif
