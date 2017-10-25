#define __DARWIN_EXTSN(sym)
#define __DARWIN_ALIAS_STARTING(_mac, _iphone, x)   x

#if defined(_DARWIN_UNLIMITED_STREAMS) || defined(_DARWIN_C_SOURCE)
FILE	*fopen(const char * __restrict __filename, const char * __restrict __mode) __DARWIN_ALIAS_STARTING(__MAC_10_6, __IPHONE_3_2, __DARWIN_EXTSN(fopen));
#else /* !_DARWIN_UNLIMITED_STREAMS && !_DARWIN_C_SOURCE */
FILE	*fopen(const char * __restrict __filename, const char * __restrict __mode) __DARWIN_ALIAS_STARTING(__MAC_10_6, __IPHONE_2_0, __DARWIN_ALIAS(fopen));
#endif /* (DARWIN_UNLIMITED_STREAMS || _DARWIN_C_SOURCE) */