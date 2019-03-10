#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "bench.h"

int
main(void) {
  printf("start\n");

  float t1_wall = wall();
  float t1_cpu = cpu();

  sleep(5);

  float t2_wall = wall();
  float t2_cpu = cpu();

  printf("     creation time (wall):   %2.2f\n", t2_wall - t1_wall);
  printf("     creation time (cpu):    %2.2f\n", t2_cpu - t1_cpu);

  printf("end\n");
  return 0;
}
