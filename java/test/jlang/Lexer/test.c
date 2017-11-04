#include "test.h"
#include <stdio.h>

static int countItr = 0;
float ffabs(float x)
{
	return ((x >= 0) << 1)*x - x;
}
float fsqrt(float a)
{
	float x1 = 0.8f;
	float x2 = 0.0f;
	while(1) 
	{
		x2 = (a / x1 + x1) / 2.0;
		++countItr;
		if (ffabs(x1 - x2) <= 0.001)
		   break;	
		x1 = x2;
	}

	return x2;
}

int main()
{
	float x = 2.0;
	countItr = 0;
	printf("the sqrt of %f is %f.\n", x, fsqrt(x));
	//printf("the count of iteration is %d.\n", countItr);
	return 0;
}
