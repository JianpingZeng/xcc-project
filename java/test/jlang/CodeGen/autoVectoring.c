int fun(int *a, int *b, int num)
{
	int c[num];

	for (int i = 0; i < num; i++)
	{
		c[i] = a[i] + b[i];
	}
	return c[0];
}
