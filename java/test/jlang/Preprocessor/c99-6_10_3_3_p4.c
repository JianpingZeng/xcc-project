// RUN: jlang-cc -E %s | grep -F 'char p[] = "x ## y";'
#define hash_hash # ## # 
#define mkstr(a) # a 
#define in_between(a) mkstr(a) 
#define join(c, d) in_between(c hash_hash d) 
char p[] = join(x, y);
