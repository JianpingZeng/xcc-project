int foo(int *arr, int len)
{
    int i = 0;
    int sum = 0;
    while (i < len)
    {
        if (i > 100)
            break;
        sum += arr[i];
        i++;
    }
    return sum;
}

int foo2(int *arr, int len)
{
    int i = 0;
    int sum = 0;
    while (i < len)
    {
        if (i <= 100)
            continue;
        sum += arr[i];
        i++;
    }
    return sum;
}