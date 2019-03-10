/* Sun compatible u-law to linear conversion */
extern short            u2s[];
extern unsigned char    s2u[];
 
#define audio_u2s(x)  (u2s[  (unsigned  char)(x)       ])
#define audio_s2u(x)  (s2u[ ((unsigned short)(x)) >> 3 ])
