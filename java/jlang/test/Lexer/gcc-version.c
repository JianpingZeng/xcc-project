/* Convenience macros to test the versions of glibc and gcc.
   Use them like this:
   #if __GNUC_PREREQ (2,8)
   ... code requiring gcc 2.8 or later ...
   #endif
   Note - they won't work for gcc1 or glibc1, since the _MINOR macros
   were not defined then.  */
#if defined __GNUC__ && defined __GNUC_MINOR__
int xx = __GNUC__;
int yy = __GNUC_MINOR__;
# define __GNUC_PREREQ(maj, min) \
	(min)
#else
# define __GNUC_PREREQ(maj, min) 0
#endif


#if __GNUC_PREREQ (4, 1)
int x = 1;
# else
int y = 1;
# endif
#else
int z = 1;
#endif