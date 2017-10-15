#include <stdio.h>
#include <math.h>
#include <stdlib.h>

float sqrt2(float a)
{
  float x0, x1;
  x0 = 1.0;
  do
  {
    x1 = a / (2 * x0) + x0 / 2;
    if (fabs(x0 - x1) < 10e-3)
      break;
    x0 = x1;
  }  while (1);
  return x0;
}

int main(int argc, char *argv[])
{
  if (argc != 2)
    return 0;
  float a = atof(argv[1]);
	printf("sqrt(%f) = %f\n", a, sqrt2(a)); 
	return 0;
}
