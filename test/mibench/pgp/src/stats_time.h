#ifndef STATS_TIME
#define STATS_TIME

#include <sys/time.h>

struct timeval ss_time_start;
struct timeval ss_time_end;

#define SS_START \
	gettimeofday(&ss_time_start,NULL);

#define SS_STOP \
	gettimeofday(&ss_time_end,NULL);

#define SS_PRINT \
  printf("The run time is: %ld us \n", \
            ((ss_time_end.tv_sec - ss_time_start.tv_sec)*1000000 \
				    + (ss_time_end.tv_usec-ss_time_start.tv_usec)));

#endif

