# 1 "sqcts.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "sqcts.c"




# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/assert.h" 1 3 4
# 37 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/assert.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 1 3 4
# 313 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/predefs.h" 1 3 4
# 314 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 2 3 4
# 346 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/cdefs.h" 1 3 4
# 353 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/cdefs.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/wordsize.h" 1 3 4
# 354 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/cdefs.h" 2 3 4
# 347 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 2 3 4
# 378 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/gnu/stubs.h" 1 3 4
# 379 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/features.h" 2 3 4
# 38 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/assert.h" 2 3 4
# 66 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/assert.h" 3 4



extern void __assert_fail (__const char *__assertion, __const char *__file,
      unsigned int __line, __const char *__function)
     __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));


extern void __assert_perror_fail (int __errnum, __const char *__file,
      unsigned int __line,
      __const char *__function)
     __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));




extern void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));



# 6 "sqcts.c" 2
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 1 3 4
# 33 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 1 3 4
# 211 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 3 4
typedef unsigned int size_t;
# 323 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 3 4
typedef unsigned int wchar_t;
# 34 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 2 3 4


# 96 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4


typedef struct
  {
    int quot;
    int rem;
  } div_t;



typedef struct
  {
    long int quot;
    long int rem;
  } ldiv_t;







__extension__ typedef struct
  {
    long long int quot;
    long long int rem;
  } lldiv_t;


# 140 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
extern size_t __ctype_get_mb_cur_max (void) __attribute__ ((__nothrow__)) ;




extern double atof (__const char *__nptr)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) ;

extern int atoi (__const char *__nptr)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) ;

extern long int atol (__const char *__nptr)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) ;





__extension__ extern long long int atoll (__const char *__nptr)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) ;





extern double strtod (__const char *__restrict __nptr,
        char **__restrict __endptr)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;





extern float strtof (__const char *__restrict __nptr,
       char **__restrict __endptr) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;

extern long double strtold (__const char *__restrict __nptr,
       char **__restrict __endptr)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;





extern long int strtol (__const char *__restrict __nptr,
   char **__restrict __endptr, int __base)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;

extern unsigned long int strtoul (__const char *__restrict __nptr,
      char **__restrict __endptr, int __base)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;




__extension__
extern long long int strtoq (__const char *__restrict __nptr,
        char **__restrict __endptr, int __base)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;

__extension__
extern unsigned long long int strtouq (__const char *__restrict __nptr,
           char **__restrict __endptr, int __base)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;





__extension__
extern long long int strtoll (__const char *__restrict __nptr,
         char **__restrict __endptr, int __base)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;

__extension__
extern unsigned long long int strtoull (__const char *__restrict __nptr,
     char **__restrict __endptr, int __base)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;

# 311 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
extern char *l64a (long int __n) __attribute__ ((__nothrow__)) ;


extern long int a64l (__const char *__s)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) ;




# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 1 3 4
# 29 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 3 4


# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/types.h" 1 3 4
# 28 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/types.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/wordsize.h" 1 3 4
# 29 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/types.h" 2 3 4


typedef unsigned char __u_char;
typedef unsigned short int __u_short;
typedef unsigned int __u_int;
typedef unsigned long int __u_long;


typedef signed char __int8_t;
typedef unsigned char __uint8_t;
typedef signed short int __int16_t;
typedef unsigned short int __uint16_t;
typedef signed int __int32_t;
typedef unsigned int __uint32_t;




__extension__ typedef signed long long int __int64_t;
__extension__ typedef unsigned long long int __uint64_t;







__extension__ typedef long long int __quad_t;
__extension__ typedef unsigned long long int __u_quad_t;
# 131 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/types.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/typesizes.h" 1 3 4
# 132 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/types.h" 2 3 4


__extension__ typedef __u_quad_t __dev_t;
__extension__ typedef unsigned int __uid_t;
__extension__ typedef unsigned int __gid_t;
__extension__ typedef unsigned long int __ino_t;
__extension__ typedef __u_quad_t __ino64_t;
__extension__ typedef unsigned int __mode_t;
__extension__ typedef unsigned int __nlink_t;
__extension__ typedef long int __off_t;
__extension__ typedef __quad_t __off64_t;
__extension__ typedef int __pid_t;
__extension__ typedef struct { int __val[2]; } __fsid_t;
__extension__ typedef long int __clock_t;
__extension__ typedef unsigned long int __rlim_t;
__extension__ typedef __u_quad_t __rlim64_t;
__extension__ typedef unsigned int __id_t;
__extension__ typedef long int __time_t;
__extension__ typedef unsigned int __useconds_t;
__extension__ typedef long int __suseconds_t;

__extension__ typedef int __daddr_t;
__extension__ typedef long int __swblk_t;
__extension__ typedef int __key_t;


__extension__ typedef int __clockid_t;


__extension__ typedef void * __timer_t;


__extension__ typedef long int __blksize_t;




__extension__ typedef long int __blkcnt_t;
__extension__ typedef __quad_t __blkcnt64_t;


__extension__ typedef unsigned long int __fsblkcnt_t;
__extension__ typedef __u_quad_t __fsblkcnt64_t;


__extension__ typedef unsigned long int __fsfilcnt_t;
__extension__ typedef __u_quad_t __fsfilcnt64_t;

__extension__ typedef int __ssize_t;



typedef __off64_t __loff_t;
typedef __quad_t *__qaddr_t;
typedef char *__caddr_t;


__extension__ typedef int __intptr_t;


__extension__ typedef unsigned int __socklen_t;
# 32 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 2 3 4



typedef __u_char u_char;
typedef __u_short u_short;
typedef __u_int u_int;
typedef __u_long u_long;
typedef __quad_t quad_t;
typedef __u_quad_t u_quad_t;
typedef __fsid_t fsid_t;




typedef __loff_t loff_t;



typedef __ino_t ino_t;
# 62 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 3 4
typedef __dev_t dev_t;




typedef __gid_t gid_t;




typedef __mode_t mode_t;




typedef __nlink_t nlink_t;




typedef __uid_t uid_t;





typedef __off_t off_t;
# 100 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 3 4
typedef __pid_t pid_t;




typedef __id_t id_t;




typedef __ssize_t ssize_t;





typedef __daddr_t daddr_t;
typedef __caddr_t caddr_t;





typedef __key_t key_t;
# 133 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 1 3 4
# 74 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4


typedef __time_t time_t;



# 92 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4
typedef __clockid_t clockid_t;
# 104 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4
typedef __timer_t timer_t;
# 134 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 2 3 4
# 147 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 1 3 4
# 148 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 2 3 4



typedef unsigned long int ulong;
typedef unsigned short int ushort;
typedef unsigned int uint;
# 195 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 3 4
typedef int int8_t __attribute__ ((__mode__ (__QI__)));
typedef int int16_t __attribute__ ((__mode__ (__HI__)));
typedef int int32_t __attribute__ ((__mode__ (__SI__)));
typedef int int64_t __attribute__ ((__mode__ (__DI__)));


typedef unsigned int u_int8_t __attribute__ ((__mode__ (__QI__)));
typedef unsigned int u_int16_t __attribute__ ((__mode__ (__HI__)));
typedef unsigned int u_int32_t __attribute__ ((__mode__ (__SI__)));
typedef unsigned int u_int64_t __attribute__ ((__mode__ (__DI__)));

typedef int register_t __attribute__ ((__mode__ (__word__)));
# 217 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/endian.h" 1 3 4
# 37 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/endian.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/endian.h" 1 3 4
# 38 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/endian.h" 2 3 4
# 61 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/endian.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/byteswap.h" 1 3 4
# 62 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/endian.h" 2 3 4
# 218 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 2 3 4


# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/select.h" 1 3 4
# 31 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/select.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/select.h" 1 3 4
# 32 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/select.h" 2 3 4


# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/sigset.h" 1 3 4
# 24 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/sigset.h" 3 4
typedef int __sig_atomic_t;




typedef struct
  {
    unsigned long int __val[(1024 / (8 * sizeof (unsigned long int)))];
  } __sigset_t;
# 35 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/select.h" 2 3 4



typedef __sigset_t sigset_t;





# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 1 3 4
# 120 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4
struct timespec
  {
    __time_t tv_sec;
    long int tv_nsec;
  };
# 45 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/select.h" 2 3 4

# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/time.h" 1 3 4
# 69 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/time.h" 3 4
struct timeval
  {
    __time_t tv_sec;
    __suseconds_t tv_usec;
  };
# 47 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/select.h" 2 3 4


typedef __suseconds_t suseconds_t;





typedef long int __fd_mask;
# 67 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/select.h" 3 4
typedef struct
  {






    __fd_mask __fds_bits[1024 / (8 * (int) sizeof (__fd_mask))];


  } fd_set;






typedef __fd_mask fd_mask;
# 99 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/select.h" 3 4

# 109 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/select.h" 3 4
extern int select (int __nfds, fd_set *__restrict __readfds,
     fd_set *__restrict __writefds,
     fd_set *__restrict __exceptfds,
     struct timeval *__restrict __timeout);
# 121 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/select.h" 3 4
extern int pselect (int __nfds, fd_set *__restrict __readfds,
      fd_set *__restrict __writefds,
      fd_set *__restrict __exceptfds,
      const struct timespec *__restrict __timeout,
      const __sigset_t *__restrict __sigmask);



# 221 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 2 3 4


# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/sysmacros.h" 1 3 4
# 30 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/sysmacros.h" 3 4
__extension__
extern unsigned int gnu_dev_major (unsigned long long int __dev)
     __attribute__ ((__nothrow__));
__extension__
extern unsigned int gnu_dev_minor (unsigned long long int __dev)
     __attribute__ ((__nothrow__));
__extension__
extern unsigned long long int gnu_dev_makedev (unsigned int __major,
            unsigned int __minor)
     __attribute__ ((__nothrow__));
# 224 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 2 3 4
# 235 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 3 4
typedef __blkcnt_t blkcnt_t;



typedef __fsblkcnt_t fsblkcnt_t;



typedef __fsfilcnt_t fsfilcnt_t;
# 270 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/pthreadtypes.h" 1 3 4
# 38 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/pthreadtypes.h" 3 4
typedef unsigned long int pthread_t;


typedef union
{
  char __size[36];
  long int __align;
} pthread_attr_t;


typedef struct __pthread_internal_slist
{
  struct __pthread_internal_slist *__next;
} __pthread_slist_t;




typedef union
{
  struct __pthread_mutex_s
  {
    int __lock;
    unsigned int __count;
    int __owner;


    int __kind;
    unsigned int __nusers;
    __extension__ union
    {
      int __spins;
      __pthread_slist_t __list;
    };
  } __data;
  char __size[24];
  long int __align;
} pthread_mutex_t;

typedef union
{
  char __size[4];
  long int __align;
} pthread_mutexattr_t;




typedef union
{
  struct
  {
    int __lock;
    unsigned int __futex;
    __extension__ unsigned long long int __total_seq;
    __extension__ unsigned long long int __wakeup_seq;
    __extension__ unsigned long long int __woken_seq;
    void *__mutex;
    unsigned int __nwaiters;
    unsigned int __broadcast_seq;
  } __data;
  char __size[48];
  __extension__ long long int __align;
} pthread_cond_t;

typedef union
{
  char __size[4];
  long int __align;
} pthread_condattr_t;



typedef unsigned int pthread_key_t;



typedef int pthread_once_t;





typedef union
{
  struct
  {
    int __lock;
    unsigned int __nr_readers;
    unsigned int __readers_wakeup;
    unsigned int __writer_wakeup;
    unsigned int __nr_readers_queued;
    unsigned int __nr_writers_queued;
# 141 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/pthreadtypes.h" 3 4
    unsigned char __flags;
    unsigned char __shared;
    unsigned char __pad1;
    unsigned char __pad2;

    int __writer;
  } __data;
  char __size[32];
  long int __align;
} pthread_rwlock_t;

typedef union
{
  char __size[8];
  long int __align;
} pthread_rwlockattr_t;





typedef volatile int pthread_spinlock_t;




typedef union
{
  char __size[20];
  long int __align;
} pthread_barrier_t;

typedef union
{
  char __size[4];
  int __align;
} pthread_barrierattr_t;
# 271 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/types.h" 2 3 4



# 321 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 2 3 4






extern long int random (void) __attribute__ ((__nothrow__));


extern void srandom (unsigned int __seed) __attribute__ ((__nothrow__));





extern char *initstate (unsigned int __seed, char *__statebuf,
   size_t __statelen) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));



extern char *setstate (char *__statebuf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));







struct random_data
  {
    int32_t *fptr;
    int32_t *rptr;
    int32_t *state;
    int rand_type;
    int rand_deg;
    int rand_sep;
    int32_t *end_ptr;
  };

extern int random_r (struct random_data *__restrict __buf,
       int32_t *__restrict __result) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int srandom_r (unsigned int __seed, struct random_data *__buf)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int initstate_r (unsigned int __seed, char *__restrict __statebuf,
   size_t __statelen,
   struct random_data *__restrict __buf)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 4)));

extern int setstate_r (char *__restrict __statebuf,
         struct random_data *__restrict __buf)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));






extern int rand (void) __attribute__ ((__nothrow__));

extern void srand (unsigned int __seed) __attribute__ ((__nothrow__));




extern int rand_r (unsigned int *__seed) __attribute__ ((__nothrow__));







extern double drand48 (void) __attribute__ ((__nothrow__));
extern double erand48 (unsigned short int __xsubi[3]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));


extern long int lrand48 (void) __attribute__ ((__nothrow__));
extern long int nrand48 (unsigned short int __xsubi[3])
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));


extern long int mrand48 (void) __attribute__ ((__nothrow__));
extern long int jrand48 (unsigned short int __xsubi[3])
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));


extern void srand48 (long int __seedval) __attribute__ ((__nothrow__));
extern unsigned short int *seed48 (unsigned short int __seed16v[3])
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern void lcong48 (unsigned short int __param[7]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));





struct drand48_data
  {
    unsigned short int __x[3];
    unsigned short int __old_x[3];
    unsigned short int __c;
    unsigned short int __init;
    unsigned long long int __a;
  };


extern int drand48_r (struct drand48_data *__restrict __buffer,
        double *__restrict __result) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));
extern int erand48_r (unsigned short int __xsubi[3],
        struct drand48_data *__restrict __buffer,
        double *__restrict __result) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));


extern int lrand48_r (struct drand48_data *__restrict __buffer,
        long int *__restrict __result)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));
extern int nrand48_r (unsigned short int __xsubi[3],
        struct drand48_data *__restrict __buffer,
        long int *__restrict __result)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));


extern int mrand48_r (struct drand48_data *__restrict __buffer,
        long int *__restrict __result)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));
extern int jrand48_r (unsigned short int __xsubi[3],
        struct drand48_data *__restrict __buffer,
        long int *__restrict __result)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));


extern int srand48_r (long int __seedval, struct drand48_data *__buffer)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int seed48_r (unsigned short int __seed16v[3],
       struct drand48_data *__buffer) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int lcong48_r (unsigned short int __param[7],
        struct drand48_data *__buffer)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));









extern void *malloc (size_t __size) __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) ;

extern void *calloc (size_t __nmemb, size_t __size)
     __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) ;










extern void *realloc (void *__ptr, size_t __size)
     __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern void free (void *__ptr) __attribute__ ((__nothrow__));




extern void cfree (void *__ptr) __attribute__ ((__nothrow__));



# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/alloca.h" 1 3 4
# 25 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/alloca.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 1 3 4
# 26 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/alloca.h" 2 3 4







extern void *alloca (size_t __size) __attribute__ ((__nothrow__));






# 498 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 2 3 4




extern void *valloc (size_t __size) __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) ;




extern int posix_memalign (void **__memptr, size_t __alignment, size_t __size)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;




extern void abort (void) __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));



extern int atexit (void (*__func) (void)) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
# 530 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4





extern int on_exit (void (*__func) (int __status, void *__arg), void *__arg)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));






extern void exit (int __status) __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));
# 553 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4






extern void _Exit (int __status) __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));






extern char *getenv (__const char *__name) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;




extern char *__secure_getenv (__const char *__name)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;





extern int putenv (char *__string) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));





extern int setenv (__const char *__name, __const char *__value, int __replace)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));


extern int unsetenv (__const char *__name) __attribute__ ((__nothrow__));






extern int clearenv (void) __attribute__ ((__nothrow__));
# 604 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
extern char *mktemp (char *__template) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;
# 615 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
extern int mkstemp (char *__template) __attribute__ ((__nonnull__ (1))) ;
# 637 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
extern int mkstemps (char *__template, int __suffixlen) __attribute__ ((__nonnull__ (1))) ;
# 658 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
extern char *mkdtemp (char *__template) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;
# 707 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4





extern int system (__const char *__command) ;

# 729 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
extern char *realpath (__const char *__restrict __name,
         char *__restrict __resolved) __attribute__ ((__nothrow__)) ;






typedef int (*__compar_fn_t) (__const void *, __const void *);
# 747 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4



extern void *bsearch (__const void *__key, __const void *__base,
        size_t __nmemb, size_t __size, __compar_fn_t __compar)
     __attribute__ ((__nonnull__ (1, 2, 5))) ;



extern void qsort (void *__base, size_t __nmemb, size_t __size,
     __compar_fn_t __compar) __attribute__ ((__nonnull__ (1, 4)));
# 766 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
extern int abs (int __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)) ;
extern long int labs (long int __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)) ;



__extension__ extern long long int llabs (long long int __x)
     __attribute__ ((__nothrow__)) __attribute__ ((__const__)) ;







extern div_t div (int __numer, int __denom)
     __attribute__ ((__nothrow__)) __attribute__ ((__const__)) ;
extern ldiv_t ldiv (long int __numer, long int __denom)
     __attribute__ ((__nothrow__)) __attribute__ ((__const__)) ;




__extension__ extern lldiv_t lldiv (long long int __numer,
        long long int __denom)
     __attribute__ ((__nothrow__)) __attribute__ ((__const__)) ;

# 802 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
extern char *ecvt (double __value, int __ndigit, int *__restrict __decpt,
     int *__restrict __sign) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4))) ;




extern char *fcvt (double __value, int __ndigit, int *__restrict __decpt,
     int *__restrict __sign) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4))) ;




extern char *gcvt (double __value, int __ndigit, char *__buf)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3))) ;




extern char *qecvt (long double __value, int __ndigit,
      int *__restrict __decpt, int *__restrict __sign)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4))) ;
extern char *qfcvt (long double __value, int __ndigit,
      int *__restrict __decpt, int *__restrict __sign)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4))) ;
extern char *qgcvt (long double __value, int __ndigit, char *__buf)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3))) ;




extern int ecvt_r (double __value, int __ndigit, int *__restrict __decpt,
     int *__restrict __sign, char *__restrict __buf,
     size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4, 5)));
extern int fcvt_r (double __value, int __ndigit, int *__restrict __decpt,
     int *__restrict __sign, char *__restrict __buf,
     size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4, 5)));

extern int qecvt_r (long double __value, int __ndigit,
      int *__restrict __decpt, int *__restrict __sign,
      char *__restrict __buf, size_t __len)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4, 5)));
extern int qfcvt_r (long double __value, int __ndigit,
      int *__restrict __decpt, int *__restrict __sign,
      char *__restrict __buf, size_t __len)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4, 5)));







extern int mblen (__const char *__s, size_t __n) __attribute__ ((__nothrow__)) ;


extern int mbtowc (wchar_t *__restrict __pwc,
     __const char *__restrict __s, size_t __n) __attribute__ ((__nothrow__)) ;


extern int wctomb (char *__s, wchar_t __wchar) __attribute__ ((__nothrow__)) ;



extern size_t mbstowcs (wchar_t *__restrict __pwcs,
   __const char *__restrict __s, size_t __n) __attribute__ ((__nothrow__));

extern size_t wcstombs (char *__restrict __s,
   __const wchar_t *__restrict __pwcs, size_t __n)
     __attribute__ ((__nothrow__));








extern int rpmatch (__const char *__response) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) ;
# 907 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
extern int posix_openpt (int __oflag) ;
# 942 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
extern int getloadavg (double __loadavg[], int __nelem)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
# 958 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4

# 7 "sqcts.c" 2
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 1 3 4
# 28 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4





# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 1 3 4
# 34 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 2 3 4









extern void *memcpy (void *__restrict __dest,
       __const void *__restrict __src, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));


extern void *memmove (void *__dest, __const void *__src, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));






extern void *memccpy (void *__restrict __dest, __const void *__restrict __src,
        int __c, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));





extern void *memset (void *__s, int __c, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));


extern int memcmp (__const void *__s1, __const void *__s2, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));
# 94 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
extern void *memchr (__const void *__s, int __c, size_t __n)
      __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));


# 125 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4


extern char *strcpy (char *__restrict __dest, __const char *__restrict __src)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *strncpy (char *__restrict __dest,
        __const char *__restrict __src, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));


extern char *strcat (char *__restrict __dest, __const char *__restrict __src)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *strncat (char *__restrict __dest, __const char *__restrict __src,
        size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));


extern int strcmp (__const char *__s1, __const char *__s2)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern int strncmp (__const char *__s1, __const char *__s2, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));


extern int strcoll (__const char *__s1, __const char *__s2)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern size_t strxfrm (char *__restrict __dest,
         __const char *__restrict __src, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));






# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/xlocale.h" 1 3 4
# 28 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/xlocale.h" 3 4
typedef struct __locale_struct
{

  struct locale_data *__locales[13];


  const unsigned short int *__ctype_b;
  const int *__ctype_tolower;
  const int *__ctype_toupper;


  const char *__names[13];
} *__locale_t;


typedef __locale_t locale_t;
# 162 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 2 3 4


extern int strcoll_l (__const char *__s1, __const char *__s2, __locale_t __l)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2, 3)));

extern size_t strxfrm_l (char *__dest, __const char *__src, size_t __n,
    __locale_t __l) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 4)));




extern char *strdup (__const char *__s)
     __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) __attribute__ ((__nonnull__ (1)));






extern char *strndup (__const char *__string, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) __attribute__ ((__nonnull__ (1)));
# 208 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4

# 233 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
extern char *strchr (__const char *__s, int __c)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));
# 260 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
extern char *strrchr (__const char *__s, int __c)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));


# 279 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4



extern size_t strcspn (__const char *__s, __const char *__reject)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));


extern size_t strspn (__const char *__s, __const char *__accept)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));
# 312 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
extern char *strpbrk (__const char *__s, __const char *__accept)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));
# 340 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
extern char *strstr (__const char *__haystack, __const char *__needle)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));




extern char *strtok (char *__restrict __s, __const char *__restrict __delim)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));




extern char *__strtok_r (char *__restrict __s,
    __const char *__restrict __delim,
    char **__restrict __save_ptr)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 3)));

extern char *strtok_r (char *__restrict __s, __const char *__restrict __delim,
         char **__restrict __save_ptr)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 3)));
# 395 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4


extern size_t strlen (__const char *__s)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));





extern size_t strnlen (__const char *__string, size_t __maxlen)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));





extern char *strerror (int __errnum) __attribute__ ((__nothrow__));

# 425 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
extern int strerror_r (int __errnum, char *__buf, size_t __buflen) __asm__ ("" "__xpg_strerror_r") __attribute__ ((__nothrow__))

                        __attribute__ ((__nonnull__ (2)));
# 443 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
extern char *strerror_l (int __errnum, __locale_t __l) __attribute__ ((__nothrow__));





extern void __bzero (void *__s, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));



extern void bcopy (__const void *__src, void *__dest, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));


extern void bzero (void *__s, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));


extern int bcmp (__const void *__s1, __const void *__s2, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));
# 487 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
extern char *index (__const char *__s, int __c)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));
# 515 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
extern char *rindex (__const char *__s, int __c)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));




extern int ffs (int __i) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
# 534 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
extern int strcasecmp (__const char *__s1, __const char *__s2)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));


extern int strncasecmp (__const char *__s1, __const char *__s2, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));
# 557 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
extern char *strsep (char **__restrict __stringp,
       __const char *__restrict __delim)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));




extern char *strsignal (int __sig) __attribute__ ((__nothrow__));


extern char *__stpcpy (char *__restrict __dest, __const char *__restrict __src)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));
extern char *stpcpy (char *__restrict __dest, __const char *__restrict __src)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));



extern char *__stpncpy (char *__restrict __dest,
   __const char *__restrict __src, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));
extern char *stpncpy (char *__restrict __dest,
        __const char *__restrict __src, size_t __n)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));
# 644 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4

# 8 "sqcts.c" 2

# 1 "square.h" 1
# 9 "square.h"
 typedef unsigned char byte;
 typedef unsigned short word16;



 typedef unsigned long word32;



extern const char *squareBanner;

typedef byte squareBlock[(4*sizeof(word32))];
typedef word32 squareKeySchedule[8 +1][4];

void squareGenerateRoundKeys (const squareBlock key,
 squareKeySchedule roundKeys_e, squareKeySchedule roundKeys_d);
void squareExpandKey (const squareBlock key, squareKeySchedule roundKeys_e);
void squareEncrypt (word32 text[4], squareKeySchedule roundKeys);
void squareDecrypt (word32 text[4], squareKeySchedule roundKeys);
# 10 "sqcts.c" 2
# 1 "sqcts.h" 1
# 18 "sqcts.h"
typedef struct {
 squareKeySchedule roundKeys_e, roundKeys_d;
 byte mask[(4*sizeof(word32))];
} squareCtsContext;

void squareCtsInit (squareCtsContext *ctxCts, const squareBlock key);
void squareCtsSetIV (squareCtsContext *ctxCts, const squareBlock iv);
void squareCtsEncrypt (squareCtsContext *ctxCts, byte *buffer, unsigned length);
void squareCtsDecrypt (squareCtsContext *ctxCts, byte *buffer, unsigned length);
void squareCtsFinal (squareCtsContext *ctxCts);
# 11 "sqcts.c" 2
# 23 "sqcts.c"
void squareCtsInit (squareCtsContext *ctxCts, const squareBlock key)
{
 ((ctxCts != ((void *)0)) ? (void) (0) : __assert_fail ("ctxCts != ((void *)0)", "sqcts.c", 25, __PRETTY_FUNCTION__));
 ((key != ((void *)0)) ? (void) (0) : __assert_fail ("key != ((void *)0)", "sqcts.c", 26, __PRETTY_FUNCTION__));
 memset (ctxCts, 0, sizeof (squareCtsContext));
 squareGenerateRoundKeys (key, ctxCts->roundKeys_e, ctxCts->roundKeys_d);
}


void squareCtsSetIV (squareCtsContext *ctxCts, const squareBlock iv)
{
 ((ctxCts != ((void *)0)) ? (void) (0) : __assert_fail ("ctxCts != ((void *)0)", "sqcts.c", 34, __PRETTY_FUNCTION__));
 if (iv != ((void *)0)) {
  memcpy (ctxCts->mask, iv, (4*sizeof(word32)));
 }

 squareEncrypt (((word32 *)(ctxCts->mask)), ctxCts->roundKeys_e);
}


void squareCtsEncrypt (squareCtsContext *ctxCts, byte *buffer, unsigned length)
{
 byte *mask;
 unsigned i;

 ((ctxCts != ((void *)0)) ? (void) (0) : __assert_fail ("ctxCts != ((void *)0)", "sqcts.c", 48, __PRETTY_FUNCTION__));
 ((buffer != ((void *)0)) ? (void) (0) : __assert_fail ("buffer != ((void *)0)", "sqcts.c", 49, __PRETTY_FUNCTION__));
 ((length >= (4*sizeof(word32))) ? (void) (0) : __assert_fail ("length >= (4*sizeof(word32))", "sqcts.c", 50, __PRETTY_FUNCTION__));
 mask = ctxCts->mask;
 while (length >= (4*sizeof(word32))) {

  ((word32 *)(buffer))[0] ^= ((word32 *)(mask))[0];
  ((word32 *)(buffer))[1] ^= ((word32 *)(mask))[1];
  ((word32 *)(buffer))[2] ^= ((word32 *)(mask))[2];
  ((word32 *)(buffer))[3] ^= ((word32 *)(mask))[3];
  squareEncrypt (((word32 *)(buffer)), ctxCts->roundKeys_e);

  mask = buffer;

  buffer += (4*sizeof(word32));
  length -= (4*sizeof(word32));
 }

 { (((word32 *)(ctxCts->mask)))[0] = (((word32 *)(mask)))[0]; (((word32 *)(ctxCts->mask)))[1] = (((word32 *)(mask)))[1]; (((word32 *)(ctxCts->mask)))[2] = (((word32 *)(mask)))[2]; (((word32 *)(ctxCts->mask)))[3] = (((word32 *)(mask)))[3]; };
 if (length != 0) {

  for (i = 0; i < length; i++) {
   ctxCts->mask[i] ^= buffer[i];
  }
  memcpy (buffer, mask, length);
  squareEncrypt (((word32 *)(ctxCts->mask)), ctxCts->roundKeys_e);
  memcpy (mask, ctxCts->mask, (4*sizeof(word32)));

 }
 mask = ((void *)0);
}


void squareCtsDecrypt (squareCtsContext *ctxCts, byte *buffer, unsigned length)
{
 unsigned i;
 squareBlock temp;

 ((ctxCts != ((void *)0)) ? (void) (0) : __assert_fail ("ctxCts != ((void *)0)", "sqcts.c", 86, __PRETTY_FUNCTION__));
 ((buffer != ((void *)0)) ? (void) (0) : __assert_fail ("buffer != ((void *)0)", "sqcts.c", 87, __PRETTY_FUNCTION__));
 ((length >= (4*sizeof(word32))) ? (void) (0) : __assert_fail ("length >= (4*sizeof(word32))", "sqcts.c", 88, __PRETTY_FUNCTION__));
 while (length >= 2*(4*sizeof(word32))) {

  { (((word32 *)(temp)))[0] = (((word32 *)(buffer)))[0]; (((word32 *)(temp)))[1] = (((word32 *)(buffer)))[1]; (((word32 *)(temp)))[2] = (((word32 *)(buffer)))[2]; (((word32 *)(temp)))[3] = (((word32 *)(buffer)))[3]; };

  squareDecrypt (((word32 *)(buffer)), ctxCts->roundKeys_d);
  ((word32 *)(buffer))[0] ^= ((word32 *)(ctxCts->mask))[0];
  ((word32 *)(buffer))[1] ^= ((word32 *)(ctxCts->mask))[1];
  ((word32 *)(buffer))[2] ^= ((word32 *)(ctxCts->mask))[2];
  ((word32 *)(buffer))[3] ^= ((word32 *)(ctxCts->mask))[3];

  { (((word32 *)(ctxCts->mask)))[0] = (((word32 *)(temp)))[0]; (((word32 *)(ctxCts->mask)))[1] = (((word32 *)(temp)))[1]; (((word32 *)(ctxCts->mask)))[2] = (((word32 *)(temp)))[2]; (((word32 *)(ctxCts->mask)))[3] = (((word32 *)(temp)))[3]; };

  buffer += (4*sizeof(word32));
  length -= (4*sizeof(word32));
 }


 { (((word32 *)(temp)))[0] = (((word32 *)(buffer)))[0]; (((word32 *)(temp)))[1] = (((word32 *)(buffer)))[1]; (((word32 *)(temp)))[2] = (((word32 *)(buffer)))[2]; (((word32 *)(temp)))[3] = (((word32 *)(buffer)))[3]; };
 if (length > (4*sizeof(word32))) {

  squareDecrypt (((word32 *)(buffer)), ctxCts->roundKeys_d);
  for (i = 0; i < length - (4*sizeof(word32)); i++) {



   buffer[i] ^= (buffer[i + (4*sizeof(word32))] ^= buffer[i]);


  }

  squareDecrypt (((word32 *)(buffer)), ctxCts->roundKeys_d);
 } else {

  squareDecrypt (((word32 *)(buffer)), ctxCts->roundKeys_d);
 }
 ((word32 *)(buffer))[0] ^= ((word32 *)(ctxCts->mask))[0];
 ((word32 *)(buffer))[1] ^= ((word32 *)(ctxCts->mask))[1];
 ((word32 *)(buffer))[2] ^= ((word32 *)(ctxCts->mask))[2];
 ((word32 *)(buffer))[3] ^= ((word32 *)(ctxCts->mask))[3];

 { (((word32 *)(ctxCts->mask)))[0] = (((word32 *)(temp)))[0]; (((word32 *)(ctxCts->mask)))[1] = (((word32 *)(temp)))[1]; (((word32 *)(ctxCts->mask)))[2] = (((word32 *)(temp)))[2]; (((word32 *)(ctxCts->mask)))[3] = (((word32 *)(temp)))[3]; };







}


void squareCtsFinal (squareCtsContext *ctxCts)
{
 ((ctxCts != ((void *)0)) ? (void) (0) : __assert_fail ("ctxCts != ((void *)0)", "sqcts.c", 142, __PRETTY_FUNCTION__));
 memset (ctxCts, 0, sizeof (squareCtsContext));
}
