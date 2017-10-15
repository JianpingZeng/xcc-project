#include <math.h>

struct T
{
	int a; 
	int b;
};

float foo()
{
	struct T xx[2] = {[0].a = 0, [0].b = 0, [1].a = 1, [1].b = 1};
	int t1 = (xx[0].a - xx[1].b);
	int t2 =  (xx[0].b - xx[1].b);
	return sqrt(t1*t1 + t2 * t2);
}
