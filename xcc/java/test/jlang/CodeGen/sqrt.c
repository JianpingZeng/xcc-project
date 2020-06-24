//#define fabs(x) (x - ((x) < 0)*(2*(x)))
float fabs(float x)
{
    return x - (x < 0)*(2*x);
}

float sqrt(float a)
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
  sqrt(2.0);
  return 0;
}
