# 1 "sha1.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "sha1.c"
# 25 "sha1.c"
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

# 26 "sha1.c" 2
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

# 27 "sha1.c" 2

# 1 "sha1.h" 1


typedef struct {
    unsigned long state[5];
    unsigned long count[2];
    unsigned char buffer[64];
} hash_context;

void hash_initial( hash_context * c );
void hash_process( hash_context * c, unsigned char * data, unsigned len );
void hash_final( hash_context * c, unsigned long[5] );
# 29 "sha1.c" 2
# 53 "sha1.c"
void SHA1Transform(unsigned long state[5], unsigned char buffer[64])
{
unsigned long a, b, c, d, e;
typedef union {
    unsigned char c[64];
    unsigned long l[16];
} CHAR64LONG16;
CHAR64LONG16* block;





    block = (CHAR64LONG16*)buffer;


    a = state[0];
    b = state[1];
    c = state[2];
    d = state[3];
    e = state[4];

    e+=((b&(c^d))^d)+block->l[0]+0x5A827999+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=((a&(b^c))^c)+block->l[1]+0x5A827999+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=((e&(a^b))^b)+block->l[2]+0x5A827999+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=((d&(e^a))^a)+block->l[3]+0x5A827999+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));;
    a+=((c&(d^e))^e)+block->l[4]+0x5A827999+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=((b&(c^d))^d)+block->l[5]+0x5A827999+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=((a&(b^c))^c)+block->l[6]+0x5A827999+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=((e&(a^b))^b)+block->l[7]+0x5A827999+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));;
    b+=((d&(e^a))^a)+block->l[8]+0x5A827999+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=((c&(d^e))^e)+block->l[9]+0x5A827999+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=((b&(c^d))^d)+block->l[10]+0x5A827999+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=((a&(b^c))^c)+block->l[11]+0x5A827999+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));;
    c+=((e&(a^b))^b)+block->l[12]+0x5A827999+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=((d&(e^a))^a)+block->l[13]+0x5A827999+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=((c&(d^e))^e)+block->l[14]+0x5A827999+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=((b&(c^d))^d)+block->l[15]+0x5A827999+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));;
    d+=((a&(b^c))^c)+(block->l[16&15] = (((block->l[(16 +13)&15]^block->l[(16 +8)&15] ^block->l[(16 +2)&15]^block->l[16&15]) << (1)) | ((block->l[(16 +13)&15]^block->l[(16 +8)&15] ^block->l[(16 +2)&15]^block->l[16&15]) >> (32 - (1)))))+0x5A827999+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=((e&(a^b))^b)+(block->l[17&15] = (((block->l[(17 +13)&15]^block->l[(17 +8)&15] ^block->l[(17 +2)&15]^block->l[17&15]) << (1)) | ((block->l[(17 +13)&15]^block->l[(17 +8)&15] ^block->l[(17 +2)&15]^block->l[17&15]) >> (32 - (1)))))+0x5A827999+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=((d&(e^a))^a)+(block->l[18&15] = (((block->l[(18 +13)&15]^block->l[(18 +8)&15] ^block->l[(18 +2)&15]^block->l[18&15]) << (1)) | ((block->l[(18 +13)&15]^block->l[(18 +8)&15] ^block->l[(18 +2)&15]^block->l[18&15]) >> (32 - (1)))))+0x5A827999+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=((c&(d^e))^e)+(block->l[19&15] = (((block->l[(19 +13)&15]^block->l[(19 +8)&15] ^block->l[(19 +2)&15]^block->l[19&15]) << (1)) | ((block->l[(19 +13)&15]^block->l[(19 +8)&15] ^block->l[(19 +2)&15]^block->l[19&15]) >> (32 - (1)))))+0x5A827999+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));;
    e+=(b^c^d)+(block->l[20&15] = (((block->l[(20 +13)&15]^block->l[(20 +8)&15] ^block->l[(20 +2)&15]^block->l[20&15]) << (1)) | ((block->l[(20 +13)&15]^block->l[(20 +8)&15] ^block->l[(20 +2)&15]^block->l[20&15]) >> (32 - (1)))))+0x6ED9EBA1+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=(a^b^c)+(block->l[21&15] = (((block->l[(21 +13)&15]^block->l[(21 +8)&15] ^block->l[(21 +2)&15]^block->l[21&15]) << (1)) | ((block->l[(21 +13)&15]^block->l[(21 +8)&15] ^block->l[(21 +2)&15]^block->l[21&15]) >> (32 - (1)))))+0x6ED9EBA1+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=(e^a^b)+(block->l[22&15] = (((block->l[(22 +13)&15]^block->l[(22 +8)&15] ^block->l[(22 +2)&15]^block->l[22&15]) << (1)) | ((block->l[(22 +13)&15]^block->l[(22 +8)&15] ^block->l[(22 +2)&15]^block->l[22&15]) >> (32 - (1)))))+0x6ED9EBA1+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=(d^e^a)+(block->l[23&15] = (((block->l[(23 +13)&15]^block->l[(23 +8)&15] ^block->l[(23 +2)&15]^block->l[23&15]) << (1)) | ((block->l[(23 +13)&15]^block->l[(23 +8)&15] ^block->l[(23 +2)&15]^block->l[23&15]) >> (32 - (1)))))+0x6ED9EBA1+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));;
    a+=(c^d^e)+(block->l[24&15] = (((block->l[(24 +13)&15]^block->l[(24 +8)&15] ^block->l[(24 +2)&15]^block->l[24&15]) << (1)) | ((block->l[(24 +13)&15]^block->l[(24 +8)&15] ^block->l[(24 +2)&15]^block->l[24&15]) >> (32 - (1)))))+0x6ED9EBA1+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=(b^c^d)+(block->l[25&15] = (((block->l[(25 +13)&15]^block->l[(25 +8)&15] ^block->l[(25 +2)&15]^block->l[25&15]) << (1)) | ((block->l[(25 +13)&15]^block->l[(25 +8)&15] ^block->l[(25 +2)&15]^block->l[25&15]) >> (32 - (1)))))+0x6ED9EBA1+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=(a^b^c)+(block->l[26&15] = (((block->l[(26 +13)&15]^block->l[(26 +8)&15] ^block->l[(26 +2)&15]^block->l[26&15]) << (1)) | ((block->l[(26 +13)&15]^block->l[(26 +8)&15] ^block->l[(26 +2)&15]^block->l[26&15]) >> (32 - (1)))))+0x6ED9EBA1+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=(e^a^b)+(block->l[27&15] = (((block->l[(27 +13)&15]^block->l[(27 +8)&15] ^block->l[(27 +2)&15]^block->l[27&15]) << (1)) | ((block->l[(27 +13)&15]^block->l[(27 +8)&15] ^block->l[(27 +2)&15]^block->l[27&15]) >> (32 - (1)))))+0x6ED9EBA1+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));;
    b+=(d^e^a)+(block->l[28&15] = (((block->l[(28 +13)&15]^block->l[(28 +8)&15] ^block->l[(28 +2)&15]^block->l[28&15]) << (1)) | ((block->l[(28 +13)&15]^block->l[(28 +8)&15] ^block->l[(28 +2)&15]^block->l[28&15]) >> (32 - (1)))))+0x6ED9EBA1+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=(c^d^e)+(block->l[29&15] = (((block->l[(29 +13)&15]^block->l[(29 +8)&15] ^block->l[(29 +2)&15]^block->l[29&15]) << (1)) | ((block->l[(29 +13)&15]^block->l[(29 +8)&15] ^block->l[(29 +2)&15]^block->l[29&15]) >> (32 - (1)))))+0x6ED9EBA1+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=(b^c^d)+(block->l[30&15] = (((block->l[(30 +13)&15]^block->l[(30 +8)&15] ^block->l[(30 +2)&15]^block->l[30&15]) << (1)) | ((block->l[(30 +13)&15]^block->l[(30 +8)&15] ^block->l[(30 +2)&15]^block->l[30&15]) >> (32 - (1)))))+0x6ED9EBA1+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=(a^b^c)+(block->l[31&15] = (((block->l[(31 +13)&15]^block->l[(31 +8)&15] ^block->l[(31 +2)&15]^block->l[31&15]) << (1)) | ((block->l[(31 +13)&15]^block->l[(31 +8)&15] ^block->l[(31 +2)&15]^block->l[31&15]) >> (32 - (1)))))+0x6ED9EBA1+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));;
    c+=(e^a^b)+(block->l[32&15] = (((block->l[(32 +13)&15]^block->l[(32 +8)&15] ^block->l[(32 +2)&15]^block->l[32&15]) << (1)) | ((block->l[(32 +13)&15]^block->l[(32 +8)&15] ^block->l[(32 +2)&15]^block->l[32&15]) >> (32 - (1)))))+0x6ED9EBA1+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=(d^e^a)+(block->l[33&15] = (((block->l[(33 +13)&15]^block->l[(33 +8)&15] ^block->l[(33 +2)&15]^block->l[33&15]) << (1)) | ((block->l[(33 +13)&15]^block->l[(33 +8)&15] ^block->l[(33 +2)&15]^block->l[33&15]) >> (32 - (1)))))+0x6ED9EBA1+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=(c^d^e)+(block->l[34&15] = (((block->l[(34 +13)&15]^block->l[(34 +8)&15] ^block->l[(34 +2)&15]^block->l[34&15]) << (1)) | ((block->l[(34 +13)&15]^block->l[(34 +8)&15] ^block->l[(34 +2)&15]^block->l[34&15]) >> (32 - (1)))))+0x6ED9EBA1+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=(b^c^d)+(block->l[35&15] = (((block->l[(35 +13)&15]^block->l[(35 +8)&15] ^block->l[(35 +2)&15]^block->l[35&15]) << (1)) | ((block->l[(35 +13)&15]^block->l[(35 +8)&15] ^block->l[(35 +2)&15]^block->l[35&15]) >> (32 - (1)))))+0x6ED9EBA1+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));;
    d+=(a^b^c)+(block->l[36&15] = (((block->l[(36 +13)&15]^block->l[(36 +8)&15] ^block->l[(36 +2)&15]^block->l[36&15]) << (1)) | ((block->l[(36 +13)&15]^block->l[(36 +8)&15] ^block->l[(36 +2)&15]^block->l[36&15]) >> (32 - (1)))))+0x6ED9EBA1+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=(e^a^b)+(block->l[37&15] = (((block->l[(37 +13)&15]^block->l[(37 +8)&15] ^block->l[(37 +2)&15]^block->l[37&15]) << (1)) | ((block->l[(37 +13)&15]^block->l[(37 +8)&15] ^block->l[(37 +2)&15]^block->l[37&15]) >> (32 - (1)))))+0x6ED9EBA1+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=(d^e^a)+(block->l[38&15] = (((block->l[(38 +13)&15]^block->l[(38 +8)&15] ^block->l[(38 +2)&15]^block->l[38&15]) << (1)) | ((block->l[(38 +13)&15]^block->l[(38 +8)&15] ^block->l[(38 +2)&15]^block->l[38&15]) >> (32 - (1)))))+0x6ED9EBA1+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=(c^d^e)+(block->l[39&15] = (((block->l[(39 +13)&15]^block->l[(39 +8)&15] ^block->l[(39 +2)&15]^block->l[39&15]) << (1)) | ((block->l[(39 +13)&15]^block->l[(39 +8)&15] ^block->l[(39 +2)&15]^block->l[39&15]) >> (32 - (1)))))+0x6ED9EBA1+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));;
    e+=(((b|c)&d)|(b&c))+(block->l[40&15] = (((block->l[(40 +13)&15]^block->l[(40 +8)&15] ^block->l[(40 +2)&15]^block->l[40&15]) << (1)) | ((block->l[(40 +13)&15]^block->l[(40 +8)&15] ^block->l[(40 +2)&15]^block->l[40&15]) >> (32 - (1)))))+0x8F1BBCDC+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=(((a|b)&c)|(a&b))+(block->l[41&15] = (((block->l[(41 +13)&15]^block->l[(41 +8)&15] ^block->l[(41 +2)&15]^block->l[41&15]) << (1)) | ((block->l[(41 +13)&15]^block->l[(41 +8)&15] ^block->l[(41 +2)&15]^block->l[41&15]) >> (32 - (1)))))+0x8F1BBCDC+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=(((e|a)&b)|(e&a))+(block->l[42&15] = (((block->l[(42 +13)&15]^block->l[(42 +8)&15] ^block->l[(42 +2)&15]^block->l[42&15]) << (1)) | ((block->l[(42 +13)&15]^block->l[(42 +8)&15] ^block->l[(42 +2)&15]^block->l[42&15]) >> (32 - (1)))))+0x8F1BBCDC+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=(((d|e)&a)|(d&e))+(block->l[43&15] = (((block->l[(43 +13)&15]^block->l[(43 +8)&15] ^block->l[(43 +2)&15]^block->l[43&15]) << (1)) | ((block->l[(43 +13)&15]^block->l[(43 +8)&15] ^block->l[(43 +2)&15]^block->l[43&15]) >> (32 - (1)))))+0x8F1BBCDC+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));;
    a+=(((c|d)&e)|(c&d))+(block->l[44&15] = (((block->l[(44 +13)&15]^block->l[(44 +8)&15] ^block->l[(44 +2)&15]^block->l[44&15]) << (1)) | ((block->l[(44 +13)&15]^block->l[(44 +8)&15] ^block->l[(44 +2)&15]^block->l[44&15]) >> (32 - (1)))))+0x8F1BBCDC+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=(((b|c)&d)|(b&c))+(block->l[45&15] = (((block->l[(45 +13)&15]^block->l[(45 +8)&15] ^block->l[(45 +2)&15]^block->l[45&15]) << (1)) | ((block->l[(45 +13)&15]^block->l[(45 +8)&15] ^block->l[(45 +2)&15]^block->l[45&15]) >> (32 - (1)))))+0x8F1BBCDC+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=(((a|b)&c)|(a&b))+(block->l[46&15] = (((block->l[(46 +13)&15]^block->l[(46 +8)&15] ^block->l[(46 +2)&15]^block->l[46&15]) << (1)) | ((block->l[(46 +13)&15]^block->l[(46 +8)&15] ^block->l[(46 +2)&15]^block->l[46&15]) >> (32 - (1)))))+0x8F1BBCDC+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=(((e|a)&b)|(e&a))+(block->l[47&15] = (((block->l[(47 +13)&15]^block->l[(47 +8)&15] ^block->l[(47 +2)&15]^block->l[47&15]) << (1)) | ((block->l[(47 +13)&15]^block->l[(47 +8)&15] ^block->l[(47 +2)&15]^block->l[47&15]) >> (32 - (1)))))+0x8F1BBCDC+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));;
    b+=(((d|e)&a)|(d&e))+(block->l[48&15] = (((block->l[(48 +13)&15]^block->l[(48 +8)&15] ^block->l[(48 +2)&15]^block->l[48&15]) << (1)) | ((block->l[(48 +13)&15]^block->l[(48 +8)&15] ^block->l[(48 +2)&15]^block->l[48&15]) >> (32 - (1)))))+0x8F1BBCDC+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=(((c|d)&e)|(c&d))+(block->l[49&15] = (((block->l[(49 +13)&15]^block->l[(49 +8)&15] ^block->l[(49 +2)&15]^block->l[49&15]) << (1)) | ((block->l[(49 +13)&15]^block->l[(49 +8)&15] ^block->l[(49 +2)&15]^block->l[49&15]) >> (32 - (1)))))+0x8F1BBCDC+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=(((b|c)&d)|(b&c))+(block->l[50&15] = (((block->l[(50 +13)&15]^block->l[(50 +8)&15] ^block->l[(50 +2)&15]^block->l[50&15]) << (1)) | ((block->l[(50 +13)&15]^block->l[(50 +8)&15] ^block->l[(50 +2)&15]^block->l[50&15]) >> (32 - (1)))))+0x8F1BBCDC+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=(((a|b)&c)|(a&b))+(block->l[51&15] = (((block->l[(51 +13)&15]^block->l[(51 +8)&15] ^block->l[(51 +2)&15]^block->l[51&15]) << (1)) | ((block->l[(51 +13)&15]^block->l[(51 +8)&15] ^block->l[(51 +2)&15]^block->l[51&15]) >> (32 - (1)))))+0x8F1BBCDC+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));;
    c+=(((e|a)&b)|(e&a))+(block->l[52&15] = (((block->l[(52 +13)&15]^block->l[(52 +8)&15] ^block->l[(52 +2)&15]^block->l[52&15]) << (1)) | ((block->l[(52 +13)&15]^block->l[(52 +8)&15] ^block->l[(52 +2)&15]^block->l[52&15]) >> (32 - (1)))))+0x8F1BBCDC+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=(((d|e)&a)|(d&e))+(block->l[53&15] = (((block->l[(53 +13)&15]^block->l[(53 +8)&15] ^block->l[(53 +2)&15]^block->l[53&15]) << (1)) | ((block->l[(53 +13)&15]^block->l[(53 +8)&15] ^block->l[(53 +2)&15]^block->l[53&15]) >> (32 - (1)))))+0x8F1BBCDC+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=(((c|d)&e)|(c&d))+(block->l[54&15] = (((block->l[(54 +13)&15]^block->l[(54 +8)&15] ^block->l[(54 +2)&15]^block->l[54&15]) << (1)) | ((block->l[(54 +13)&15]^block->l[(54 +8)&15] ^block->l[(54 +2)&15]^block->l[54&15]) >> (32 - (1)))))+0x8F1BBCDC+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=(((b|c)&d)|(b&c))+(block->l[55&15] = (((block->l[(55 +13)&15]^block->l[(55 +8)&15] ^block->l[(55 +2)&15]^block->l[55&15]) << (1)) | ((block->l[(55 +13)&15]^block->l[(55 +8)&15] ^block->l[(55 +2)&15]^block->l[55&15]) >> (32 - (1)))))+0x8F1BBCDC+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));;
    d+=(((a|b)&c)|(a&b))+(block->l[56&15] = (((block->l[(56 +13)&15]^block->l[(56 +8)&15] ^block->l[(56 +2)&15]^block->l[56&15]) << (1)) | ((block->l[(56 +13)&15]^block->l[(56 +8)&15] ^block->l[(56 +2)&15]^block->l[56&15]) >> (32 - (1)))))+0x8F1BBCDC+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=(((e|a)&b)|(e&a))+(block->l[57&15] = (((block->l[(57 +13)&15]^block->l[(57 +8)&15] ^block->l[(57 +2)&15]^block->l[57&15]) << (1)) | ((block->l[(57 +13)&15]^block->l[(57 +8)&15] ^block->l[(57 +2)&15]^block->l[57&15]) >> (32 - (1)))))+0x8F1BBCDC+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=(((d|e)&a)|(d&e))+(block->l[58&15] = (((block->l[(58 +13)&15]^block->l[(58 +8)&15] ^block->l[(58 +2)&15]^block->l[58&15]) << (1)) | ((block->l[(58 +13)&15]^block->l[(58 +8)&15] ^block->l[(58 +2)&15]^block->l[58&15]) >> (32 - (1)))))+0x8F1BBCDC+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=(((c|d)&e)|(c&d))+(block->l[59&15] = (((block->l[(59 +13)&15]^block->l[(59 +8)&15] ^block->l[(59 +2)&15]^block->l[59&15]) << (1)) | ((block->l[(59 +13)&15]^block->l[(59 +8)&15] ^block->l[(59 +2)&15]^block->l[59&15]) >> (32 - (1)))))+0x8F1BBCDC+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));;
    e+=(b^c^d)+(block->l[60&15] = (((block->l[(60 +13)&15]^block->l[(60 +8)&15] ^block->l[(60 +2)&15]^block->l[60&15]) << (1)) | ((block->l[(60 +13)&15]^block->l[(60 +8)&15] ^block->l[(60 +2)&15]^block->l[60&15]) >> (32 - (1)))))+0xCA62C1D6+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=(a^b^c)+(block->l[61&15] = (((block->l[(61 +13)&15]^block->l[(61 +8)&15] ^block->l[(61 +2)&15]^block->l[61&15]) << (1)) | ((block->l[(61 +13)&15]^block->l[(61 +8)&15] ^block->l[(61 +2)&15]^block->l[61&15]) >> (32 - (1)))))+0xCA62C1D6+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=(e^a^b)+(block->l[62&15] = (((block->l[(62 +13)&15]^block->l[(62 +8)&15] ^block->l[(62 +2)&15]^block->l[62&15]) << (1)) | ((block->l[(62 +13)&15]^block->l[(62 +8)&15] ^block->l[(62 +2)&15]^block->l[62&15]) >> (32 - (1)))))+0xCA62C1D6+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=(d^e^a)+(block->l[63&15] = (((block->l[(63 +13)&15]^block->l[(63 +8)&15] ^block->l[(63 +2)&15]^block->l[63&15]) << (1)) | ((block->l[(63 +13)&15]^block->l[(63 +8)&15] ^block->l[(63 +2)&15]^block->l[63&15]) >> (32 - (1)))))+0xCA62C1D6+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));;
    a+=(c^d^e)+(block->l[64&15] = (((block->l[(64 +13)&15]^block->l[(64 +8)&15] ^block->l[(64 +2)&15]^block->l[64&15]) << (1)) | ((block->l[(64 +13)&15]^block->l[(64 +8)&15] ^block->l[(64 +2)&15]^block->l[64&15]) >> (32 - (1)))))+0xCA62C1D6+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=(b^c^d)+(block->l[65&15] = (((block->l[(65 +13)&15]^block->l[(65 +8)&15] ^block->l[(65 +2)&15]^block->l[65&15]) << (1)) | ((block->l[(65 +13)&15]^block->l[(65 +8)&15] ^block->l[(65 +2)&15]^block->l[65&15]) >> (32 - (1)))))+0xCA62C1D6+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=(a^b^c)+(block->l[66&15] = (((block->l[(66 +13)&15]^block->l[(66 +8)&15] ^block->l[(66 +2)&15]^block->l[66&15]) << (1)) | ((block->l[(66 +13)&15]^block->l[(66 +8)&15] ^block->l[(66 +2)&15]^block->l[66&15]) >> (32 - (1)))))+0xCA62C1D6+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=(e^a^b)+(block->l[67&15] = (((block->l[(67 +13)&15]^block->l[(67 +8)&15] ^block->l[(67 +2)&15]^block->l[67&15]) << (1)) | ((block->l[(67 +13)&15]^block->l[(67 +8)&15] ^block->l[(67 +2)&15]^block->l[67&15]) >> (32 - (1)))))+0xCA62C1D6+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));;
    b+=(d^e^a)+(block->l[68&15] = (((block->l[(68 +13)&15]^block->l[(68 +8)&15] ^block->l[(68 +2)&15]^block->l[68&15]) << (1)) | ((block->l[(68 +13)&15]^block->l[(68 +8)&15] ^block->l[(68 +2)&15]^block->l[68&15]) >> (32 - (1)))))+0xCA62C1D6+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=(c^d^e)+(block->l[69&15] = (((block->l[(69 +13)&15]^block->l[(69 +8)&15] ^block->l[(69 +2)&15]^block->l[69&15]) << (1)) | ((block->l[(69 +13)&15]^block->l[(69 +8)&15] ^block->l[(69 +2)&15]^block->l[69&15]) >> (32 - (1)))))+0xCA62C1D6+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=(b^c^d)+(block->l[70&15] = (((block->l[(70 +13)&15]^block->l[(70 +8)&15] ^block->l[(70 +2)&15]^block->l[70&15]) << (1)) | ((block->l[(70 +13)&15]^block->l[(70 +8)&15] ^block->l[(70 +2)&15]^block->l[70&15]) >> (32 - (1)))))+0xCA62C1D6+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));; d+=(a^b^c)+(block->l[71&15] = (((block->l[(71 +13)&15]^block->l[(71 +8)&15] ^block->l[(71 +2)&15]^block->l[71&15]) << (1)) | ((block->l[(71 +13)&15]^block->l[(71 +8)&15] ^block->l[(71 +2)&15]^block->l[71&15]) >> (32 - (1)))))+0xCA62C1D6+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));;
    c+=(e^a^b)+(block->l[72&15] = (((block->l[(72 +13)&15]^block->l[(72 +8)&15] ^block->l[(72 +2)&15]^block->l[72&15]) << (1)) | ((block->l[(72 +13)&15]^block->l[(72 +8)&15] ^block->l[(72 +2)&15]^block->l[72&15]) >> (32 - (1)))))+0xCA62C1D6+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=(d^e^a)+(block->l[73&15] = (((block->l[(73 +13)&15]^block->l[(73 +8)&15] ^block->l[(73 +2)&15]^block->l[73&15]) << (1)) | ((block->l[(73 +13)&15]^block->l[(73 +8)&15] ^block->l[(73 +2)&15]^block->l[73&15]) >> (32 - (1)))))+0xCA62C1D6+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=(c^d^e)+(block->l[74&15] = (((block->l[(74 +13)&15]^block->l[(74 +8)&15] ^block->l[(74 +2)&15]^block->l[74&15]) << (1)) | ((block->l[(74 +13)&15]^block->l[(74 +8)&15] ^block->l[(74 +2)&15]^block->l[74&15]) >> (32 - (1)))))+0xCA62C1D6+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));; e+=(b^c^d)+(block->l[75&15] = (((block->l[(75 +13)&15]^block->l[(75 +8)&15] ^block->l[(75 +2)&15]^block->l[75&15]) << (1)) | ((block->l[(75 +13)&15]^block->l[(75 +8)&15] ^block->l[(75 +2)&15]^block->l[75&15]) >> (32 - (1)))))+0xCA62C1D6+(((a) << (5)) | ((a) >> (32 - (5))));b=(((b) << (30)) | ((b) >> (32 - (30))));;
    d+=(a^b^c)+(block->l[76&15] = (((block->l[(76 +13)&15]^block->l[(76 +8)&15] ^block->l[(76 +2)&15]^block->l[76&15]) << (1)) | ((block->l[(76 +13)&15]^block->l[(76 +8)&15] ^block->l[(76 +2)&15]^block->l[76&15]) >> (32 - (1)))))+0xCA62C1D6+(((e) << (5)) | ((e) >> (32 - (5))));a=(((a) << (30)) | ((a) >> (32 - (30))));; c+=(e^a^b)+(block->l[77&15] = (((block->l[(77 +13)&15]^block->l[(77 +8)&15] ^block->l[(77 +2)&15]^block->l[77&15]) << (1)) | ((block->l[(77 +13)&15]^block->l[(77 +8)&15] ^block->l[(77 +2)&15]^block->l[77&15]) >> (32 - (1)))))+0xCA62C1D6+(((d) << (5)) | ((d) >> (32 - (5))));e=(((e) << (30)) | ((e) >> (32 - (30))));; b+=(d^e^a)+(block->l[78&15] = (((block->l[(78 +13)&15]^block->l[(78 +8)&15] ^block->l[(78 +2)&15]^block->l[78&15]) << (1)) | ((block->l[(78 +13)&15]^block->l[(78 +8)&15] ^block->l[(78 +2)&15]^block->l[78&15]) >> (32 - (1)))))+0xCA62C1D6+(((c) << (5)) | ((c) >> (32 - (5))));d=(((d) << (30)) | ((d) >> (32 - (30))));; a+=(c^d^e)+(block->l[79&15] = (((block->l[(79 +13)&15]^block->l[(79 +8)&15] ^block->l[(79 +2)&15]^block->l[79&15]) << (1)) | ((block->l[(79 +13)&15]^block->l[(79 +8)&15] ^block->l[(79 +2)&15]^block->l[79&15]) >> (32 - (1)))))+0xCA62C1D6+(((b) << (5)) | ((b) >> (32 - (5))));c=(((c) << (30)) | ((c) >> (32 - (30))));;

    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;
    state[4] += e;

    a = b = c = d = e = 0;
}




void hash_initial(hash_context* context)
{

    context->state[0] = 0x67452301;
    context->state[1] = 0xEFCDAB89;
    context->state[2] = 0x98BADCFE;
    context->state[3] = 0x10325476;
    context->state[4] = 0xC3D2E1F0;
    context->count[0] = context->count[1] = 0;
}



void hash_process( hash_context * context, unsigned char * data, unsigned len )
{
unsigned int i, j;
unsigned long blen = ((unsigned long)len)<<3;

    j = (context->count[0] >> 3) & 63;
    if ((context->count[0] += blen) < blen ) context->count[1]++;
    context->count[1] += (len >> 29);
    if ((j + len) > 63) {
        memcpy(&context->buffer[j], data, (i = 64-j));
        SHA1Transform(context->state, context->buffer);
        for ( ; i + 63 < len; i += 64) {
            SHA1Transform(context->state, &data[i]);
        }
        j = 0;
    }
    else i = 0;
    memcpy(&context->buffer[j], &data[i], len - i);
}




void hash_final( hash_context* context, unsigned long digest[5] )
{
unsigned long i, j;
unsigned char finalcount[8];

    for (i = 0; i < 8; i++) {
        finalcount[i] = (unsigned char)((context->count[(i >= 4 ? 0 : 1)]
         >> ((3-(i & 3)) * 8) ) & 255);
    }
    hash_process(context, (unsigned char *)"\200", 1);
    while ((context->count[0] & 504) != 448) {
        hash_process(context, (unsigned char *)"\0", 1);
    }
    hash_process(context, finalcount, 8);
    for (i = 0; i < 5; i++) {
        digest[i] = context->state[i];
    }

    i = j = 0;
    memset(context->buffer, 0, 64);
    memset(context->state, 0, 20);
    memset(context->count, 0, 8);
    memset(&finalcount, 0, 8);



}
