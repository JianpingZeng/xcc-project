#define __DARWIN_ALIAS(sym)		""
#define __DARWIN_ALIAS_STARTING(_mac, _iphone, x)   x

char *fopen(const char * restrict __filename, const char * restrict __mode) __DARWIN_ALIAS_STARTING(__MAC_10_6, __IPHONE_2_0, __DARWIN_ALIAS(fopen));
