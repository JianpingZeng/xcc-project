#include<stdio.h>

main()
{
    int c;
    while ((c=getchar()) !=EOF) {
        if (c != 0xd)
            putchar(c);
    }
}
