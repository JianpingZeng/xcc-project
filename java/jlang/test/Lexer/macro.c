#define Test "xxx"

#define fabs(x) (x<0?-x:x)

void foo()
{
    char* str = Test;
    int positive = fabs(-1);
}