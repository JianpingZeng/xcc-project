// This code snip stems from Mac OSX i386/types.h header file.
/*
 * mbstate_t is an opaque object to keep conversion state, during multibyte
 * stream conversions.  The content must not be referenced by user programs.
 */
typedef union {
	char		__mbstate8[128];
	long long	_mbstateL;			/* for alignment */
} __mbstate_t;