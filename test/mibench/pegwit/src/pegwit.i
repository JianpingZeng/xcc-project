# 1 "pegwit.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "pegwit.c"







# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 1 3 4
# 26 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 3 4
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
# 27 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/string.h" 2 3 4






# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 1 3 4
# 211 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 3 4
typedef unsigned int size_t;
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

# 9 "pegwit.c" 2
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 1 3 4
# 30 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4




# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 1 3 4
# 35 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 2 3 4

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
# 37 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 2 3 4
# 45 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
struct _IO_FILE;



typedef struct _IO_FILE FILE;





# 65 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
typedef struct _IO_FILE __FILE;
# 75 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 1 3 4
# 32 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/_G_config.h" 1 3 4
# 15 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/_G_config.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 1 3 4
# 16 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/_G_config.h" 2 3 4




# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/wchar.h" 1 3 4
# 83 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/wchar.h" 3 4
typedef struct
{
  int __count;
  union
  {

    unsigned int __wch;



    char __wchb[4];
  } __value;
} __mbstate_t;
# 21 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/_G_config.h" 2 3 4

typedef struct
{
  __off_t __pos;
  __mbstate_t __state;
} _G_fpos_t;
typedef struct
{
  __off64_t __pos;
  __mbstate_t __state;
} _G_fpos64_t;
# 53 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/_G_config.h" 3 4
typedef int _G_int16_t __attribute__ ((__mode__ (__HI__)));
typedef int _G_int32_t __attribute__ ((__mode__ (__SI__)));
typedef unsigned int _G_uint16_t __attribute__ ((__mode__ (__HI__)));
typedef unsigned int _G_uint32_t __attribute__ ((__mode__ (__SI__)));
# 33 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 2 3 4
# 53 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stdarg.h" 1 3 4
# 40 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stdarg.h" 3 4
typedef __builtin_va_list __gnuc_va_list;
# 54 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 2 3 4
# 170 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
struct _IO_jump_t; struct _IO_FILE;
# 180 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
typedef void _IO_lock_t;





struct _IO_marker {
  struct _IO_marker *_next;
  struct _IO_FILE *_sbuf;



  int _pos;
# 203 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
};


enum __codecvt_result
{
  __codecvt_ok,
  __codecvt_partial,
  __codecvt_error,
  __codecvt_noconv
};
# 271 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
struct _IO_FILE {
  int _flags;




  char* _IO_read_ptr;
  char* _IO_read_end;
  char* _IO_read_base;
  char* _IO_write_base;
  char* _IO_write_ptr;
  char* _IO_write_end;
  char* _IO_buf_base;
  char* _IO_buf_end;

  char *_IO_save_base;
  char *_IO_backup_base;
  char *_IO_save_end;

  struct _IO_marker *_markers;

  struct _IO_FILE *_chain;

  int _fileno;



  int _flags2;

  __off_t _old_offset;



  unsigned short _cur_column;
  signed char _vtable_offset;
  char _shortbuf[1];



  _IO_lock_t *_lock;
# 319 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
  __off64_t _offset;
# 328 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
  void *__pad1;
  void *__pad2;
  void *__pad3;
  void *__pad4;
  size_t __pad5;

  int _mode;

  char _unused2[15 * sizeof (int) - 4 * sizeof (void *) - sizeof (size_t)];

};


typedef struct _IO_FILE _IO_FILE;


struct _IO_FILE_plus;

extern struct _IO_FILE_plus _IO_2_1_stdin_;
extern struct _IO_FILE_plus _IO_2_1_stdout_;
extern struct _IO_FILE_plus _IO_2_1_stderr_;
# 364 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
typedef __ssize_t __io_read_fn (void *__cookie, char *__buf, size_t __nbytes);







typedef __ssize_t __io_write_fn (void *__cookie, __const char *__buf,
     size_t __n);







typedef int __io_seek_fn (void *__cookie, __off64_t *__pos, int __w);


typedef int __io_close_fn (void *__cookie);
# 416 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
extern int __underflow (_IO_FILE *);
extern int __uflow (_IO_FILE *);
extern int __overflow (_IO_FILE *, int);
# 460 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
extern int _IO_getc (_IO_FILE *__fp);
extern int _IO_putc (int __c, _IO_FILE *__fp);
extern int _IO_feof (_IO_FILE *__fp) __attribute__ ((__nothrow__));
extern int _IO_ferror (_IO_FILE *__fp) __attribute__ ((__nothrow__));

extern int _IO_peekc_locked (_IO_FILE *__fp);





extern void _IO_flockfile (_IO_FILE *) __attribute__ ((__nothrow__));
extern void _IO_funlockfile (_IO_FILE *) __attribute__ ((__nothrow__));
extern int _IO_ftrylockfile (_IO_FILE *) __attribute__ ((__nothrow__));
# 490 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/libio.h" 3 4
extern int _IO_vfscanf (_IO_FILE * __restrict, const char * __restrict,
   __gnuc_va_list, int *__restrict);
extern int _IO_vfprintf (_IO_FILE *__restrict, const char *__restrict,
    __gnuc_va_list);
extern __ssize_t _IO_padn (_IO_FILE *, int, __ssize_t);
extern size_t _IO_sgetn (_IO_FILE *, void *, size_t);

extern __off64_t _IO_seekoff (_IO_FILE *, __off64_t, int, int);
extern __off64_t _IO_seekpos (_IO_FILE *, __off64_t, int);

extern void _IO_free_backup_area (_IO_FILE *) __attribute__ ((__nothrow__));
# 76 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 2 3 4
# 89 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4


typedef _G_fpos_t fpos_t;




# 141 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/stdio_lim.h" 1 3 4
# 142 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 2 3 4



extern struct _IO_FILE *stdin;
extern struct _IO_FILE *stdout;
extern struct _IO_FILE *stderr;







extern int remove (__const char *__filename) __attribute__ ((__nothrow__));

extern int rename (__const char *__old, __const char *__new) __attribute__ ((__nothrow__));




extern int renameat (int __oldfd, __const char *__old, int __newfd,
       __const char *__new) __attribute__ ((__nothrow__));








extern FILE *tmpfile (void) ;
# 186 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern char *tmpnam (char *__s) __attribute__ ((__nothrow__)) ;





extern char *tmpnam_r (char *__s) __attribute__ ((__nothrow__)) ;
# 204 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern char *tempnam (__const char *__dir, __const char *__pfx)
     __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) ;








extern int fclose (FILE *__stream);




extern int fflush (FILE *__stream);

# 229 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern int fflush_unlocked (FILE *__stream);
# 243 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4






extern FILE *fopen (__const char *__restrict __filename,
      __const char *__restrict __modes) ;




extern FILE *freopen (__const char *__restrict __filename,
        __const char *__restrict __modes,
        FILE *__restrict __stream) ;
# 272 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4

# 283 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern FILE *fdopen (int __fd, __const char *__modes) __attribute__ ((__nothrow__)) ;
# 296 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern FILE *fmemopen (void *__s, size_t __len, __const char *__modes)
  __attribute__ ((__nothrow__)) ;




extern FILE *open_memstream (char **__bufloc, size_t *__sizeloc) __attribute__ ((__nothrow__)) ;






extern void setbuf (FILE *__restrict __stream, char *__restrict __buf) __attribute__ ((__nothrow__));



extern int setvbuf (FILE *__restrict __stream, char *__restrict __buf,
      int __modes, size_t __n) __attribute__ ((__nothrow__));





extern void setbuffer (FILE *__restrict __stream, char *__restrict __buf,
         size_t __size) __attribute__ ((__nothrow__));


extern void setlinebuf (FILE *__stream) __attribute__ ((__nothrow__));








extern int fprintf (FILE *__restrict __stream,
      __const char *__restrict __format, ...);




extern int printf (__const char *__restrict __format, ...);

extern int sprintf (char *__restrict __s,
      __const char *__restrict __format, ...) __attribute__ ((__nothrow__));





extern int vfprintf (FILE *__restrict __s, __const char *__restrict __format,
       __gnuc_va_list __arg);




extern int vprintf (__const char *__restrict __format, __gnuc_va_list __arg);

extern int vsprintf (char *__restrict __s, __const char *__restrict __format,
       __gnuc_va_list __arg) __attribute__ ((__nothrow__));





extern int snprintf (char *__restrict __s, size_t __maxlen,
       __const char *__restrict __format, ...)
     __attribute__ ((__nothrow__)) __attribute__ ((__format__ (__printf__, 3, 4)));

extern int vsnprintf (char *__restrict __s, size_t __maxlen,
        __const char *__restrict __format, __gnuc_va_list __arg)
     __attribute__ ((__nothrow__)) __attribute__ ((__format__ (__printf__, 3, 0)));

# 394 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern int vdprintf (int __fd, __const char *__restrict __fmt,
       __gnuc_va_list __arg)
     __attribute__ ((__format__ (__printf__, 2, 0)));
extern int dprintf (int __fd, __const char *__restrict __fmt, ...)
     __attribute__ ((__format__ (__printf__, 2, 3)));








extern int fscanf (FILE *__restrict __stream,
     __const char *__restrict __format, ...) ;




extern int scanf (__const char *__restrict __format, ...) ;

extern int sscanf (__const char *__restrict __s,
     __const char *__restrict __format, ...) __attribute__ ((__nothrow__));
# 425 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern int fscanf (FILE *__restrict __stream, __const char *__restrict __format, ...) __asm__ ("" "__isoc99_fscanf")

                               ;
extern int scanf (__const char *__restrict __format, ...) __asm__ ("" "__isoc99_scanf")
                              ;
extern int sscanf (__const char *__restrict __s, __const char *__restrict __format, ...) __asm__ ("" "__isoc99_sscanf")

                          __attribute__ ((__nothrow__));
# 445 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4








extern int vfscanf (FILE *__restrict __s, __const char *__restrict __format,
      __gnuc_va_list __arg)
     __attribute__ ((__format__ (__scanf__, 2, 0))) ;





extern int vscanf (__const char *__restrict __format, __gnuc_va_list __arg)
     __attribute__ ((__format__ (__scanf__, 1, 0))) ;


extern int vsscanf (__const char *__restrict __s,
      __const char *__restrict __format, __gnuc_va_list __arg)
     __attribute__ ((__nothrow__)) __attribute__ ((__format__ (__scanf__, 2, 0)));
# 476 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern int vfscanf (FILE *__restrict __s, __const char *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vfscanf")



     __attribute__ ((__format__ (__scanf__, 2, 0))) ;
extern int vscanf (__const char *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vscanf")

     __attribute__ ((__format__ (__scanf__, 1, 0))) ;
extern int vsscanf (__const char *__restrict __s, __const char *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vsscanf")



     __attribute__ ((__nothrow__)) __attribute__ ((__format__ (__scanf__, 2, 0)));
# 504 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4









extern int fgetc (FILE *__stream);
extern int getc (FILE *__stream);





extern int getchar (void);

# 532 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern int getc_unlocked (FILE *__stream);
extern int getchar_unlocked (void);
# 543 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern int fgetc_unlocked (FILE *__stream);











extern int fputc (int __c, FILE *__stream);
extern int putc (int __c, FILE *__stream);





extern int putchar (int __c);

# 576 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern int fputc_unlocked (int __c, FILE *__stream);







extern int putc_unlocked (int __c, FILE *__stream);
extern int putchar_unlocked (int __c);






extern int getw (FILE *__stream);


extern int putw (int __w, FILE *__stream);








extern char *fgets (char *__restrict __s, int __n, FILE *__restrict __stream)
     ;






extern char *gets (char *__s) ;

# 638 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern __ssize_t __getdelim (char **__restrict __lineptr,
          size_t *__restrict __n, int __delimiter,
          FILE *__restrict __stream) ;
extern __ssize_t getdelim (char **__restrict __lineptr,
        size_t *__restrict __n, int __delimiter,
        FILE *__restrict __stream) ;







extern __ssize_t getline (char **__restrict __lineptr,
       size_t *__restrict __n,
       FILE *__restrict __stream) ;








extern int fputs (__const char *__restrict __s, FILE *__restrict __stream);





extern int puts (__const char *__s);






extern int ungetc (int __c, FILE *__stream);






extern size_t fread (void *__restrict __ptr, size_t __size,
       size_t __n, FILE *__restrict __stream) ;




extern size_t fwrite (__const void *__restrict __ptr, size_t __size,
        size_t __n, FILE *__restrict __s) ;

# 710 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern size_t fread_unlocked (void *__restrict __ptr, size_t __size,
         size_t __n, FILE *__restrict __stream) ;
extern size_t fwrite_unlocked (__const void *__restrict __ptr, size_t __size,
          size_t __n, FILE *__restrict __stream) ;








extern int fseek (FILE *__stream, long int __off, int __whence);




extern long int ftell (FILE *__stream) ;




extern void rewind (FILE *__stream);

# 746 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern int fseeko (FILE *__stream, __off_t __off, int __whence);




extern __off_t ftello (FILE *__stream) ;
# 765 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4






extern int fgetpos (FILE *__restrict __stream, fpos_t *__restrict __pos);




extern int fsetpos (FILE *__stream, __const fpos_t *__pos);
# 788 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4

# 797 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4


extern void clearerr (FILE *__stream) __attribute__ ((__nothrow__));

extern int feof (FILE *__stream) __attribute__ ((__nothrow__)) ;

extern int ferror (FILE *__stream) __attribute__ ((__nothrow__)) ;




extern void clearerr_unlocked (FILE *__stream) __attribute__ ((__nothrow__));
extern int feof_unlocked (FILE *__stream) __attribute__ ((__nothrow__)) ;
extern int ferror_unlocked (FILE *__stream) __attribute__ ((__nothrow__)) ;








extern void perror (__const char *__s);






# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/sys_errlist.h" 1 3 4
# 27 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/sys_errlist.h" 3 4
extern int sys_nerr;
extern __const char *__const sys_errlist[];
# 827 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 2 3 4




extern int fileno (FILE *__stream) __attribute__ ((__nothrow__)) ;




extern int fileno_unlocked (FILE *__stream) __attribute__ ((__nothrow__)) ;
# 846 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern FILE *popen (__const char *__command, __const char *__modes) ;





extern int pclose (FILE *__stream);





extern char *ctermid (char *__s) __attribute__ ((__nothrow__));
# 886 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
extern void flockfile (FILE *__stream) __attribute__ ((__nothrow__));



extern int ftrylockfile (FILE *__stream) __attribute__ ((__nothrow__)) ;


extern void funlockfile (FILE *__stream) __attribute__ ((__nothrow__));
# 916 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4

# 10 "pegwit.c" 2
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 1 3 4
# 30 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4








# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 1 3 4
# 39 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 2 3 4



# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/time.h" 1 3 4
# 43 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 2 3 4
# 58 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4


typedef __clock_t clock_t;



# 74 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4


typedef __time_t time_t;



# 92 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4
typedef __clockid_t clockid_t;
# 104 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4
typedef __timer_t timer_t;
# 120 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4
struct timespec
  {
    __time_t tv_sec;
    long int tv_nsec;
  };








struct tm
{
  int tm_sec;
  int tm_min;
  int tm_hour;
  int tm_mday;
  int tm_mon;
  int tm_year;
  int tm_wday;
  int tm_yday;
  int tm_isdst;


  long int tm_gmtoff;
  __const char *tm_zone;




};








struct itimerspec
  {
    struct timespec it_interval;
    struct timespec it_value;
  };


struct sigevent;





typedef __pid_t pid_t;








extern clock_t clock (void) __attribute__ ((__nothrow__));


extern time_t time (time_t *__timer) __attribute__ ((__nothrow__));


extern double difftime (time_t __time1, time_t __time0)
     __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern time_t mktime (struct tm *__tp) __attribute__ ((__nothrow__));





extern size_t strftime (char *__restrict __s, size_t __maxsize,
   __const char *__restrict __format,
   __const struct tm *__restrict __tp) __attribute__ ((__nothrow__));

# 217 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4
extern size_t strftime_l (char *__restrict __s, size_t __maxsize,
     __const char *__restrict __format,
     __const struct tm *__restrict __tp,
     __locale_t __loc) __attribute__ ((__nothrow__));
# 230 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4



extern struct tm *gmtime (__const time_t *__timer) __attribute__ ((__nothrow__));



extern struct tm *localtime (__const time_t *__timer) __attribute__ ((__nothrow__));





extern struct tm *gmtime_r (__const time_t *__restrict __timer,
       struct tm *__restrict __tp) __attribute__ ((__nothrow__));



extern struct tm *localtime_r (__const time_t *__restrict __timer,
          struct tm *__restrict __tp) __attribute__ ((__nothrow__));





extern char *asctime (__const struct tm *__tp) __attribute__ ((__nothrow__));


extern char *ctime (__const time_t *__timer) __attribute__ ((__nothrow__));







extern char *asctime_r (__const struct tm *__restrict __tp,
   char *__restrict __buf) __attribute__ ((__nothrow__));


extern char *ctime_r (__const time_t *__restrict __timer,
        char *__restrict __buf) __attribute__ ((__nothrow__));




extern char *__tzname[2];
extern int __daylight;
extern long int __timezone;




extern char *tzname[2];



extern void tzset (void) __attribute__ ((__nothrow__));



extern int daylight;
extern long int timezone;





extern int stime (__const time_t *__when) __attribute__ ((__nothrow__));
# 313 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4
extern time_t timegm (struct tm *__tp) __attribute__ ((__nothrow__));


extern time_t timelocal (struct tm *__tp) __attribute__ ((__nothrow__));


extern int dysize (int __year) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
# 328 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4
extern int nanosleep (__const struct timespec *__requested_time,
        struct timespec *__remaining);



extern int clock_getres (clockid_t __clock_id, struct timespec *__res) __attribute__ ((__nothrow__));


extern int clock_gettime (clockid_t __clock_id, struct timespec *__tp) __attribute__ ((__nothrow__));


extern int clock_settime (clockid_t __clock_id, __const struct timespec *__tp)
     __attribute__ ((__nothrow__));






extern int clock_nanosleep (clockid_t __clock_id, int __flags,
       __const struct timespec *__req,
       struct timespec *__rem);


extern int clock_getcpuclockid (pid_t __pid, clockid_t *__clock_id) __attribute__ ((__nothrow__));




extern int timer_create (clockid_t __clock_id,
    struct sigevent *__restrict __evp,
    timer_t *__restrict __timerid) __attribute__ ((__nothrow__));


extern int timer_delete (timer_t __timerid) __attribute__ ((__nothrow__));


extern int timer_settime (timer_t __timerid, int __flags,
     __const struct itimerspec *__restrict __value,
     struct itimerspec *__restrict __ovalue) __attribute__ ((__nothrow__));


extern int timer_gettime (timer_t __timerid, struct itimerspec *__value)
     __attribute__ ((__nothrow__));


extern int timer_getoverrun (timer_t __timerid) __attribute__ ((__nothrow__));
# 417 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/time.h" 3 4

# 11 "pegwit.c" 2
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/ctype.h" 1 3 4
# 30 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/ctype.h" 3 4

# 41 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/ctype.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/endian.h" 1 3 4
# 37 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/endian.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/endian.h" 1 3 4
# 38 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/endian.h" 2 3 4
# 61 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/endian.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/byteswap.h" 1 3 4
# 62 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/endian.h" 2 3 4
# 42 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/ctype.h" 2 3 4






enum
{
  _ISupper = ((0) < 8 ? ((1 << (0)) << 8) : ((1 << (0)) >> 8)),
  _ISlower = ((1) < 8 ? ((1 << (1)) << 8) : ((1 << (1)) >> 8)),
  _ISalpha = ((2) < 8 ? ((1 << (2)) << 8) : ((1 << (2)) >> 8)),
  _ISdigit = ((3) < 8 ? ((1 << (3)) << 8) : ((1 << (3)) >> 8)),
  _ISxdigit = ((4) < 8 ? ((1 << (4)) << 8) : ((1 << (4)) >> 8)),
  _ISspace = ((5) < 8 ? ((1 << (5)) << 8) : ((1 << (5)) >> 8)),
  _ISprint = ((6) < 8 ? ((1 << (6)) << 8) : ((1 << (6)) >> 8)),
  _ISgraph = ((7) < 8 ? ((1 << (7)) << 8) : ((1 << (7)) >> 8)),
  _ISblank = ((8) < 8 ? ((1 << (8)) << 8) : ((1 << (8)) >> 8)),
  _IScntrl = ((9) < 8 ? ((1 << (9)) << 8) : ((1 << (9)) >> 8)),
  _ISpunct = ((10) < 8 ? ((1 << (10)) << 8) : ((1 << (10)) >> 8)),
  _ISalnum = ((11) < 8 ? ((1 << (11)) << 8) : ((1 << (11)) >> 8))
};
# 81 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/ctype.h" 3 4
extern __const unsigned short int **__ctype_b_loc (void)
     __attribute__ ((__nothrow__)) __attribute__ ((__const));
extern __const __int32_t **__ctype_tolower_loc (void)
     __attribute__ ((__nothrow__)) __attribute__ ((__const));
extern __const __int32_t **__ctype_toupper_loc (void)
     __attribute__ ((__nothrow__)) __attribute__ ((__const));
# 96 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/ctype.h" 3 4






extern int isalnum (int) __attribute__ ((__nothrow__));
extern int isalpha (int) __attribute__ ((__nothrow__));
extern int iscntrl (int) __attribute__ ((__nothrow__));
extern int isdigit (int) __attribute__ ((__nothrow__));
extern int islower (int) __attribute__ ((__nothrow__));
extern int isgraph (int) __attribute__ ((__nothrow__));
extern int isprint (int) __attribute__ ((__nothrow__));
extern int ispunct (int) __attribute__ ((__nothrow__));
extern int isspace (int) __attribute__ ((__nothrow__));
extern int isupper (int) __attribute__ ((__nothrow__));
extern int isxdigit (int) __attribute__ ((__nothrow__));



extern int tolower (int __c) __attribute__ ((__nothrow__));


extern int toupper (int __c) __attribute__ ((__nothrow__));








extern int isblank (int) __attribute__ ((__nothrow__));


# 142 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/ctype.h" 3 4
extern int isascii (int __c) __attribute__ ((__nothrow__));



extern int toascii (int __c) __attribute__ ((__nothrow__));



extern int _toupper (int) __attribute__ ((__nothrow__));
extern int _tolower (int) __attribute__ ((__nothrow__));
# 247 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/ctype.h" 3 4
extern int isalnum_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isalpha_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int iscntrl_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isdigit_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int islower_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isgraph_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isprint_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int ispunct_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isspace_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isupper_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isxdigit_l (int, __locale_t) __attribute__ ((__nothrow__));

extern int isblank_l (int, __locale_t) __attribute__ ((__nothrow__));



extern int __tolower_l (int __c, __locale_t __l) __attribute__ ((__nothrow__));
extern int tolower_l (int __c, __locale_t __l) __attribute__ ((__nothrow__));


extern int __toupper_l (int __c, __locale_t __l) __attribute__ ((__nothrow__));
extern int toupper_l (int __c, __locale_t __l) __attribute__ ((__nothrow__));
# 323 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/ctype.h" 3 4

# 12 "pegwit.c" 2

# 1 "stats_time.h" 1



# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/time.h" 1 3 4
# 29 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/time.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/time.h" 1 3 4
# 69 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/time.h" 3 4
struct timeval
  {
    __time_t tv_sec;
    __suseconds_t tv_usec;
  };
# 30 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/time.h" 2 3 4

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







# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/time.h" 1 3 4
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



# 32 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/time.h" 2 3 4








# 57 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/time.h" 3 4
struct timezone
  {
    int tz_minuteswest;
    int tz_dsttime;
  };

typedef struct timezone *__restrict __timezone_ptr_t;
# 73 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/time.h" 3 4
extern int gettimeofday (struct timeval *__restrict __tv,
    __timezone_ptr_t __tz) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));




extern int settimeofday (__const struct timeval *__tv,
    __const struct timezone *__tz)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));





extern int adjtime (__const struct timeval *__delta,
      struct timeval *__olddelta) __attribute__ ((__nothrow__));




enum __itimer_which
  {

    ITIMER_REAL = 0,


    ITIMER_VIRTUAL = 1,



    ITIMER_PROF = 2

  };



struct itimerval
  {

    struct timeval it_interval;

    struct timeval it_value;
  };






typedef int __itimer_which_t;




extern int getitimer (__itimer_which_t __which,
        struct itimerval *__value) __attribute__ ((__nothrow__));




extern int setitimer (__itimer_which_t __which,
        __const struct itimerval *__restrict __new,
        struct itimerval *__restrict __old) __attribute__ ((__nothrow__));




extern int utimes (__const char *__file, __const struct timeval __tvp[2])
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));



extern int lutimes (__const char *__file, __const struct timeval __tvp[2])
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));


extern int futimes (int __fd, __const struct timeval __tvp[2]) __attribute__ ((__nothrow__));
# 191 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/sys/time.h" 3 4

# 5 "stats_time.h" 2

struct timeval ss_time_start;
struct timeval ss_time_end;
# 14 "pegwit.c" 2

# 1 "ec_crypt.h" 1



# 1 "ec_curve.h" 1



# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 1 3 4
# 149 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 3 4
typedef int ptrdiff_t;
# 323 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 3 4
typedef unsigned int wchar_t;
# 5 "ec_curve.h" 2

# 1 "ec_field.h" 1





# 1 "ec_param.h" 1
# 7 "ec_field.h" 2
# 1 "ec_vlong.h" 1





# 1 "ec_param.h" 1
# 7 "ec_vlong.h" 2



 typedef unsigned char byte;
 typedef unsigned short word16;
 typedef unsigned long word32;




typedef word16 vlPoint [((17*15 + 15)/16 + 1) + 2];


void vlPrint (FILE *out, const char *tag, const vlPoint k);


void vlClear (vlPoint p);

void vlShortSet (vlPoint p, word16 u);


int vlEqual (const vlPoint p, const vlPoint q);

int vlGreater (const vlPoint p, const vlPoint q);

int vlNumBits (const vlPoint k);


int vlTakeBit (const vlPoint k, word16 i);


void vlRandom (vlPoint k);


void vlCopy (vlPoint p, const vlPoint q);


void vlAdd (vlPoint u, const vlPoint v);

void vlSubtract (vlPoint u, const vlPoint v);

void vlRemainder (vlPoint u, const vlPoint v);

void vlMulMod (vlPoint u, const vlPoint v, const vlPoint w, const vlPoint m);

void vlShortLshift (vlPoint u, int n);

void vlShortRshift (vlPoint u, int n);

int vlShortMultiply (vlPoint p, const vlPoint q, word16 d);


int vlSelfTest (int test_count);
# 8 "ec_field.h" 2
# 27 "ec_field.h"
 typedef word16 lunit;





 typedef word16 ltemp;


typedef lunit gfPoint [(2*(17 +1))];



int gfInit (void);


void gfQuit (void);


void gfPrint (FILE *out, const char *tag, const gfPoint p);


int gfEqual (const gfPoint p, const gfPoint q);


void gfClear (gfPoint p);


void gfRandom (gfPoint p);


void gfCopy (gfPoint p, const gfPoint q);


void gfAdd (gfPoint p, const gfPoint q, const gfPoint r);


void gfMultiply (gfPoint r, const gfPoint p, const gfPoint q);


void gfSmallDiv (gfPoint p, lunit b);


void gfSquare (gfPoint p, const gfPoint q);


int gfInvert (gfPoint p, const gfPoint q);



void gfSquareRoot (gfPoint p, lunit b);


int gfTrace (const gfPoint p);


int gfQuadSolve (gfPoint p, const gfPoint q);


int gfYbit (const gfPoint p);


void gfPack (const gfPoint p, vlPoint k);


void gfUnpack (gfPoint p, const vlPoint k);


int gfSelfTest (int test_count);
# 7 "ec_curve.h" 2
# 16 "ec_curve.h"
typedef struct {
 gfPoint x, y;
} ecPoint;


extern const vlPoint prime_order;
extern const ecPoint curve_point;


int ecCheck (const ecPoint *p);


void ecPrint (FILE *out, const char *tag, const ecPoint *p);


int ecEqual (const ecPoint *p, const ecPoint *q);


void ecCopy (ecPoint *p, const ecPoint *q);


int ecCalcY (ecPoint *p, int ybit);


void ecRandom (ecPoint *p);


void ecClear (ecPoint *p);


void ecAdd (ecPoint *p, const ecPoint *r);


void ecSub (ecPoint *p, const ecPoint *r);


void ecNegate (ecPoint *p);


void ecDouble (ecPoint *p);


void ecMultiply (ecPoint *p, const vlPoint k);


int ecYbit (const ecPoint *p);


void ecPack (const ecPoint *p, vlPoint k);


void ecUnpack (ecPoint *p, const vlPoint k);


int ecSelfTest (int test_count);
# 5 "ec_crypt.h" 2


typedef struct {
 vlPoint r, s;
} cpPair;


void cpMakePublicKey (vlPoint vlPublicKey, const vlPoint vlPrivateKey);
void cpEncodeSecret (const vlPoint vlPublicKey, vlPoint vlMessage, vlPoint vlSecret);
void cpDecodeSecret (const vlPoint vlPrivateKey, const vlPoint vlMessage, vlPoint d);
void cpSign(const vlPoint vlPrivateKey, const vlPoint secret, const vlPoint mac, cpPair * cpSig);
int cpVerify(const vlPoint vlPublicKey, const vlPoint vlMac, cpPair * cpSig );
# 16 "pegwit.c" 2
# 1 "sha1.h" 1


typedef struct {
    unsigned long state[5];
    unsigned long count[2];
    unsigned char buffer[64];
} hash_context;

void hash_initial( hash_context * c );
void hash_process( hash_context * c, unsigned char * data, unsigned len );
void hash_final( hash_context * c, unsigned long[5] );
# 17 "pegwit.c" 2
# 1 "square.h" 1
# 18 "square.h"
extern const char *squareBanner;

typedef byte squareBlock[(4*sizeof(word32))];
typedef word32 squareKeySchedule[8 +1][4];

void squareGenerateRoundKeys (const squareBlock key,
 squareKeySchedule roundKeys_e, squareKeySchedule roundKeys_d);
void squareExpandKey (const squareBlock key, squareKeySchedule roundKeys_e);
void squareEncrypt (word32 text[4], squareKeySchedule roundKeys);
void squareDecrypt (word32 text[4], squareKeySchedule roundKeys);
# 18 "pegwit.c" 2
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
# 19 "pegwit.c" 2
# 1 "binasc.h" 1
int flushArmour(FILE * stream);
size_t freadPlus(void *ptr, size_t size, size_t n, FILE *stream);
size_t fwritePlus(const void *ptr, size_t size, size_t n, FILE *stream);
int fputcPlus(int c, FILE *stream);
void burnBinasc(void);
# 20 "pegwit.c" 2







const char manual [] =
  "Pegwit v8.7\n"
  "Usage (init/encrypt/decrypt/sign/verify) :\n"
  "-i <secret-key >public-key\n"
  "-e public-key plain cipher <random-junk\n"
  "-d cipher plain <secret-key\n"
  "-s plain <secret-key >signature\n"
  "-v public-key plain <signature\n"
  "-E plain cipher <key\n"
  "-D cipher plain <key\n"
  "-S text <secret-key >clearsigned-text\n"
  "-V public-key clearsigned-text >text\n"
  "-f[operation] [type pegwit -f for details]\n";
const char filterManual [] =
  "Pegwit [filter sub-mode]\n"
  "Usage (encrypt/decrypt/sign/verify) :\n"
  "-fe public-key random-junk <plain >ascii-cipher\n"
  "-fd secret-key <ascii-cipher >plain\n"
  "-fE key <plain >ascii-cipher\n"
  "-fD key <ascii-cipher >plain\n"
  "-fS secret-key <text >clearsigned-text\n"
  "-fV public-key <clearsigned-text >text\n";


const char pubkey_magic [] = "pegwit v8 public key =";
const char err_output [] = "Pegwit, error writing output, disk full?";
const char err_open_failed [] = "Pegwit, error : failed to open ";
const char err_bad_public_key [] = "Pegwit, error : public key must start with \"";
const char err_signature [] = "signature did not verify\a\a\a\n";
const char err_decrypt [] = "decryption failed\a\a\a\n";

const char begin_clearsign [] = "###\n";
const char end_clearsign [] = "### end pegwit v8 signed text\n";
const char end_ckarmour [] = "### end pegwit v8.7 -fE encrypted text\n";
const char end_pkarmour [] = "### end pegwit v8.7 -fe encrypted text\n";
const char escape [] = "## ";
const char warn_long_line [] =
  "Very long line - > 8k bytes.  Binary file?\n"
  "Clearsignature dubious\a\a\a\n";
const char warn_control_chars [] =
  "Large number of control characters.  Binary file?\n"
  "Clearsignature dubious\a\a\a\n";
const char err_clearsig_header_not_found [] =
  "Clearsignature header \"###\" not found\a\a\a\n";





void hash_process_file( hash_context * c, FILE * f_inp, unsigned barrel )
{
  unsigned n;
  unsigned char buffer[0x4000];
  while (1)
  {
    n = fread( buffer, 1, 0x4000, f_inp );
    if (n==0) break;
    {
      unsigned j;
      for ( j=0; j<barrel; j+=1 )
      {
        hash_process( c+j, buffer, n );
      }
    }
    if (n < 0x4000) break;
  }
  memset( buffer, sizeof(buffer), 0 );
  fseek( f_inp, 0, 0 );
}

int downcase(char c)
{
      if((((c) & ~0x7f) == 0)) if(((*__ctype_b_loc ())[(int) ((c))] & (unsigned short int) _ISupper)) return tolower(c);
      return c;
}

int case_blind_compare(const char *a, const char *b)
{
    while(*a && *b)
    {
        if(downcase(*a) < downcase(*b)) return -1;
        if(downcase(*a) > downcase(*b)) return 1;
        a += 1;
        b += 1;
    }
    if(*a) return 1;
    if(*b) return -1;
    return 0;
}

void hash_process_ascii( hash_context * c, FILE * f_inp,
  FILE * f_out, unsigned barrel, int write)
{
  unsigned n;
  unsigned char buffer[0x4000], *begin;
  unsigned long bytes=0, control=0;

  while (1)
  {
      unsigned i;

      fgets((char*)buffer, 0x4000, f_inp);
      if(feof(f_inp)) break;

      n = strlen((char*)buffer);
      begin = buffer;

      if(n > 0x2000)
      {
        fputs( warn_long_line, f_out );
      }

      bytes += n;
      for(i=0; i<n; ++i)
      {
        if(buffer[i] >= 0x7F) ++control;
        if(buffer[i] < ' ' && buffer[i] != '\n' && buffer[i] != '\r'
          && buffer[i] != '\t') ++control;
      }

      if(write)
      {
        if (!strncmp( (char*)buffer, escape, 2 ) ||
            !case_blind_compare((char*)buffer, "from") )
        {
          fputs( escape, f_out );
        }
        fputs( (char*)buffer, f_out);
      }
      else
      {
        if(!strncmp((char*)buffer, escape, 3)) {n-=3, begin+=3;}
        else if(!strncmp((char*)buffer, end_clearsign, 3)) break;
        fputs((char*)begin, f_out);
      }

      for ( i=0; i<barrel; ++i )
      {
        hash_process( c+i, begin, n );
      }
  }
  if(control*6 > bytes)
  {
    fputs( warn_control_chars, stderr );
  }

  memset( buffer, sizeof(buffer), 0 );
}

typedef struct
{
  unsigned count;
  word32 seed[2+5*3];
} prng;

void prng_init( prng * p )
{
  memset( p, 0, sizeof(*p) );
}

void prng_set_secret( prng * p, FILE * f_key )
{
  hash_context c[1];
  hash_initial( c );
  hash_process_file( c, f_key, 1 );
  hash_final( c, p->seed+1 );
  p->count = 1+5;
}

void prng_init_mac(hash_context c[2])
{

  unsigned char b;
  for ( b=0; b<2; ++b )
  {
    hash_initial( c+b );
    hash_process( c+1, &b, 1 );
  }
}

void prng_set_mac( prng * p, FILE * f_inp, int barrel )
{

  unsigned char b;
  hash_context c[2];
  for ( b=0; b<barrel; b+=1 )
  {
    hash_initial( c+b );
    if ( b==1 ) hash_process( c+1, &b, 1 );
  }
  hash_process_file( c, f_inp, barrel );
  for ( b=0; b<barrel; b+=1 )
  {
    hash_final( c, p->seed+1+5*(b+1) );
  }
  p->count = 1 + (barrel+1)*5;
}

void clearsign( prng * p, FILE * f_inp, FILE * f_out )
{
  hash_context c[2];

  prng_init_mac(c);
  fputs(begin_clearsign,f_out);
  hash_process_ascii( c, f_inp, f_out, 2, 1 );
  fputs(end_clearsign,f_out);
  hash_final( c, p->seed+1+5 );
  hash_final( c, p->seed+1+2*5 );
  p->count = 1 + 3*5;
}

int position(FILE * f_inp)
{
  while(!feof(f_inp))
  {
      char buffer[1024];
      fgets(buffer, 1024, f_inp);
      if(!strncmp(buffer, begin_clearsign, 3)) break;
  }
  if(feof(f_inp))
  {
    fputs( err_clearsig_header_not_found, stderr );
    return 0;
  }
  return 1;
}

int readsign( prng * p, FILE * f_inp, FILE * f_out )
{
  hash_context c[2];
  prng_init_mac(c);

  if(!position(f_inp)) return 1;
  hash_process_ascii( c, f_inp, f_out, 2, 0 );
  hash_final( c, p->seed+1+5 );
  hash_final( c, p->seed+1+2*5 );
  p->count = 1 + 3*5;

  return 0;
}

void prng_set_time( prng * p )
{
  p->seed[1+3*5] = (word32) time(0);
  p->count = 2 + 3*5;
}

word32 prng_next( prng * p )
{
  word32 tmp[5];
  byte buffer[ ( 3*5 + 2 ) * 4 ];
  unsigned i,j;
  hash_context c;

  p->seed[0] += 1;
  for ( i = 0; i < p->count; i+=1 )
  {
    for ( j = 0; j < 4; j += 1 )
    {
      buffer[ i*4 + j ] = (byte) ( p->seed[i] >> (j*8) );
    }
  }

  hash_initial( &c );
  hash_process( &c, buffer, p->count*4 );
  hash_final( &c, tmp );
  memset( buffer, 0, sizeof(buffer) );
  return tmp[0];
}

void prng_to_vlong( prng * p, vlPoint V )
{
  unsigned i;
  V[0] = 15;
  for (i=1;i<16;i+=1)
    V[i] = (unsigned short) prng_next( p );
}

void hash_to_vlong( word32 * mac, vlPoint V )
{
  unsigned i;
  V[0] = 15;
  for (i=0;i<8;i+=1)
  {
    word32 x = mac[i];
    V[i*2+1] = (word16) x;
    V[i*2+2] = (word16) (x>>16);
  }
}

void get_vlong( FILE *f, vlPoint v )
{
  unsigned u;
  vlPoint w;
  vlClear (v);
  w[0] = 1;
  while (1)
  {
    u = fgetc( f );
    if ( u >= '0' && u <= '9' )
      u -= '0';
    else if ( u >= 'a' && u <= 'z' )
      u -= 'a' - 10;
    else if ( u >= 'A' && u <= 'Z' )
      u -= 'A' - 10;
    else if ( u <= ' ' )
      continue;
    else
      break;
    vlShortLshift (v, 4);
    w[1] = (word16) u;
    vlAdd (v, w);
  }
}

void get_vlong_a( FILE *f, vlPoint v )
{
  unsigned i=0;
  char buffer[256], u;

  vlPoint w;
  vlClear (v);
  w[0] = 1;
  buffer[0]=0;
  fgets(buffer, 256, f);

  while ((u = buffer[i++]) != 0)
  {
    if ( u >= '0' && u <= '9' )
      u -= '0';
    else if ( u >= 'a' && u <= 'z' )
      u -= 'a' - 10;
    else if ( u >= 'A' && u <= 'Z' )
      u -= 'A' - 10;
    else if ( u <= ' ' )
      continue;
    else
      break;
    vlShortLshift (v, 4);
    w[1] = (word16) u;
    vlAdd (v, w);
  }
}

const char hex[16] = "0123456789abcdef";

void put_vlong( vlPoint v )
{
  unsigned i,j;
  for (i = v[0]; i > 0; i--)
  {
    unsigned x = v[i];
    for (j=0;j<4;j+=1)
      putchar( hex[ (x >> (12-4*j)) % 16 ] );
  }
}

void put_binary_vlong (FILE *f, vlPoint v)
{
  unsigned n = ((255 +1+7)/8);
  while (n--)
  {
    if (v[0] == 0) v[1] = 0;
    fputcPlus (v[1] & 0xff, f);
    vlShortRshift (v, 8);
  }
}

void get_binary_vlong(FILE *f, vlPoint v)
{
  byte u[((255 +1+7)/8)];
  vlPoint w;
  unsigned n = ((255 +1+7)/8);
  freadPlus(u, 1, ((255 +1+7)/8), f);
  vlClear (v); w[0] = 1;
  while (n--)
  {
    vlShortLshift (v, 8);
    w[1] = u[n];
    vlAdd (v, w);
  }
}


typedef word32 big_buf[1+0x1000/4];


void vlong_to_square_block( const vlPoint V, squareBlock key )
{
  vlPoint v;
  unsigned j;
  vlCopy (v, V);
  for (j = 0; j < ((4*sizeof(word32))); j++)
  {
    if (v[0] == 0) v[1] = 0;
    key[j] = (byte)v[1];
    vlShortRshift (v, 8);
  }
}

void increment( squareBlock iv )
{
  int i = 0;
  while (iv[i]==0xff) iv[i++] = 0;
  iv[i] += 1;
}

int sym_encrypt( vlPoint secret, FILE * f_inp, FILE * f_out )
{
  squareBlock key,iv;
  squareCtsContext ctx;
  big_buf buffer;

  int n,err = 0;
  byte pad;

  memset( iv, 0, sizeof(iv) );
  vlong_to_square_block (secret, key);
 squareCtsInit( &ctx, key );
  pad = 0;
  while (n = fread( buffer, 1, 0x1000, f_inp ) )
  {
    if ( n < 0x1000 )
    {
      pad = 0;
      if (n<((4*sizeof(word32))))
        pad = 17-n;
      else if (n&1)
        pad = 2;
      memset( n+(byte*)buffer, pad, pad );
      n += pad;
    }
    squareCtsSetIV( &ctx, iv );
    increment(iv);
    squareCtsEncrypt( &ctx, (byte*)buffer, n );
    {
      int written = fwritePlus( buffer,1,n,f_out );
      if ( written != n )
      {
        fputs( err_output, stderr );
        err = 1;
        break;
      }
    }
  }

  squareCtsFinal( &ctx );
  memset( key, 0, sizeof(key) );
  return err;
}

int sym_decrypt( vlPoint secret, FILE * f_inp, FILE * f_out)
{
  squareBlock key,iv;
  big_buf b1,b2;
  byte * buf1 = (byte*)b1, * buf2 = (byte*)b2;
  squareCtsContext ctx;
  int err = 0, n = 0;

  memset(iv,0,sizeof(iv));
  vlong_to_square_block( secret, key );
 squareCtsInit( &ctx, key );

  while (1)
  {
    int i = 0;
    if ( n == 0 || n == 0x1000 )
      i = freadPlus( buf1, 1, 0x1000, f_inp );
    if (n)
    {
      if ( n < ((4*sizeof(word32))) )
      {
        decrypt_error:
        fputs( err_decrypt, stderr );
        err = 1;
        break;
      }

      if ( i == 1 )
      {
        n += 1;
        buf2[0x1000] = buf1[0];
        i = 0;
      }
      squareCtsSetIV( &ctx, iv );
      increment( iv );
      squareCtsDecrypt( &ctx, buf2, n );

      if ( n & 1 )
      {
        byte pad = buf2[n-1];

        if ( pad < 1 || pad > ((4*sizeof(word32))) ) goto decrypt_error;
        n -= pad;
        {
          int j;
          for (j=0;j<pad;j+=1)
            if ( buf2[n+j] != pad ) goto decrypt_error;
        }
      }
      {
        int written = fwrite( buf2, 1, n, f_out );
        if ( written != n )
        {
          fputs( err_output, stderr );
          err = 1;
          break;
        }
      }
    }
    if ( i == 0 ) break;
    { byte * tmp = buf1; buf1=buf2; buf2 = tmp; }
    n = i;
  }
  memset( key, 0, sizeof(key) );
  squareCtsFinal( &ctx );
  return err;
}

int do_operation( FILE * f_key, FILE * f_inp, FILE * f_out, FILE * f_sec, int operation )
{
  prng p;
  vlPoint pub,secret,session,mac,msg;
  cpPair sig;
  int err = 0;


  prng_init( &p );
  if ( operation == 'v' || operation == 'e' || 'V' == operation )
  {
    get_vlong( f_key, pub );
    if ( operation == 'e' )
    {
      if ( f_sec ) prng_set_secret( &p, f_sec );
      prng_set_mac( &p, f_inp, 1 );
    }
  }
  else
  {
    setbuf(f_key,0);
    prng_set_secret( &p, f_key );
    if ( operation == 'E' || operation == 'D' )
      hash_to_vlong( p.seed+1, secret );
    else
      prng_to_vlong( &p, secret );
  }

  if ( operation == 's' || operation == 'v' )
  {
    prng_set_mac( &p, f_inp, 2 );
    hash_to_vlong( p.seed+1+5, mac );
  }
  if('S' == operation)
  {
    clearsign( &p, f_inp, f_out );
    hash_to_vlong( p.seed+1+5, mac );
  }
  if('V' == operation)
  {
    if ( readsign( &p, f_inp, f_out ) )
      return 2;
    hash_to_vlong( p.seed+1+5, mac );
  }




  if ( operation == 'E' )
  {
    if(stdout == f_out) fputs(begin_clearsign,f_out);
    err = sym_encrypt( secret, f_inp, f_out );
    if(stdout == f_out)
    {
      if(!flushArmour(f_out)) return 3;
      fputs(end_ckarmour, f_out);
    }
  }
  else if ( operation == 'D' )
  {
    if(stdin == f_inp) if(!position(f_inp)) return 2;
    err = sym_decrypt( secret, f_inp, f_out );
  }
  else
  {
    gfInit();
    if ( operation == 'i' )
    {
      cpMakePublicKey( pub, secret );
      fputs( pubkey_magic, f_out);
      put_vlong( pub );
    }
    else if ( operation == 'e' )
    {
      if(stdout == f_out) fputs(begin_clearsign,f_out);
      prng_set_time( &p );
      prng_to_vlong( &p, session );
      cpEncodeSecret( pub, msg, session );
      put_binary_vlong( f_out, msg );
      err = sym_encrypt( session, f_inp, f_out );
      if(stdout == f_out)
      {
        if(!flushArmour(f_out)) return 3;
        fputs(end_pkarmour, f_out);
      }
    }
    else if ( operation == 'd')
    {
      if(stdin == f_inp) if(!position(f_inp)) return 2;
      get_binary_vlong( f_inp, msg );
      cpDecodeSecret( secret, msg, session );
      err = sym_decrypt( session, f_inp, f_out );
    }
    else if ( operation == 's' || 'S' == operation)
    {
      do
      {
        prng_to_vlong( &p, session );
        cpSign( secret, session, mac, &sig );
      } while ( sig.r[0] == 0 );
      put_vlong( sig.s );
      if('S' == operation) fputs("\n", f_out);
      else fputs( ":", f_out );
      put_vlong( sig.r );
      if('S' == operation) fputs("\n", f_out);
    }
    else
  {
    if('v' == operation)
      {
        get_vlong( f_sec, sig.s );
        get_vlong( f_sec, sig.r );
   }
   else
      {
        get_vlong_a( f_inp, sig.s );
        get_vlong_a( f_inp, sig.r );
    }
      err = !cpVerify( pub, mac, &sig );
      if (err)
     fputs( err_signature, stderr );
    }
    gfQuit();
  }
  fflush(f_out);

  prng_init( &p );
  vlClear( secret );
  vlClear( session );
  return err;
}

FILE * chkopen( char * s, char * mode )
{
  FILE * result = fopen(s,mode);
  if (!result)
  {
    fputs( err_open_failed, stderr );
    fputs( s, stderr );
  }
  return result;
}

void burn_stack(void)
{


  unsigned char x [ 20000 ];
  memset( x, 0, sizeof(x) );
}
# 705 "pegwit.c"
int main(int argc, char * argv[] )
{

  int err, operation, filter=0;
  unsigned expect, arg_ix;
  FILE * f_key, * f_inp, * f_out, *f_sec;
  char openForRead [3] = "rb";
  char openForWrite [3] = "wb";
  char openKey[3] = "rb";
# 730 "pegwit.c"
  if ( argc<2 || argv[1][0] != '-')
  {
    error:
    if(filter) goto filterError;
    fputs( manual, stderr );


    return 1;
  }
  operation = argv[1][1];

  if('f' == operation)
  {
      filter=1;
      operation = argv[1][2];
      if(0 == argv[1][2])
      {
         filterError:
         fputs(filterManual, stderr);
         return 1;
      }
      if (0 != argv[1][3]) goto error;
  }
  else if (argv[1][2] != 0 ) goto error;


  expect = 0;

  if(!filter)
  {
    if ( operation == 'i' ) expect = 2;
    else if ( operation == 's' || 'S' == operation ) expect = 3;
    else if ( operation == 'd' || operation == 'v' || 'V' == operation ||
    operation == 'D' || operation == 'E' ) expect = 4;
    else if ( operation == 'e' ) expect = 5;
  }
  else
  {
    if('V' == operation || 'S' == operation || 'E' == operation ||
      'D' == operation || 'd' == operation ) expect = 3;
   else if ('e' == operation) expect = 4;
  }

  if ( argc != expect ) goto error;

  arg_ix = 2;

  f_key = stdin;
  if ( operation == 'e' || operation == 'v' || 'V' == operation || filter )
  {
    unsigned i, isPub = 1;

    if('S' == operation || 'd' == operation) openKey[1] = 0;

    f_key = chkopen( argv[arg_ix++], openKey );

    if (!f_key) return 1;
    if(filter && 'e' != operation && 'V' != operation) isPub = 0;

    for (i=0;isPub && pubkey_magic[i];i+=1)
    {
      if ( fgetc( f_key ) != pubkey_magic[i] )
      {
        fputs( err_bad_public_key, stderr );
        fputs( pubkey_magic, stderr );
        fputc( '"', stderr );
        return 1;
      }
    }
  }

  f_inp = stdin;
  f_out = stdout;

  if(!filter)
  {
    if('V' == operation || 'S' == operation)
      openForRead[1] = openForWrite[1] = 0;

    f_sec = 0;
    if('e' == operation || 'v' == operation) f_sec = stdin;
    if ( argc > arg_ix )
    {
      f_inp = chkopen( argv[arg_ix++], openForRead );
      if (!f_inp) return 1;
    }
    if ( argc > arg_ix )
    {
      f_out = chkopen( argv[arg_ix++], openForWrite );
      if (!f_out) return 1;
    }
  }
  else
  {
      f_sec = 0;
      if('e' == operation)
      {
        f_sec = chkopen( argv[arg_ix++], openForRead );
        if (!f_sec) return 1;
      }
  }

  err = do_operation( f_key, f_inp, f_out, f_sec, operation );

  burn_stack();
  burnBinasc();


  return err;
}
