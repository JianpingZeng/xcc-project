# 1 "square.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "square.c"
# 95 "square.c"
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



# 96 "square.c" 2
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 1 3 4
# 30 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4




# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 1 3 4
# 211 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 3 4
typedef unsigned int size_t;
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

# 97 "square.c" 2
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 1 3 4
# 33 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdlib.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../lib/gcc/arm-none-linux-gnueabi/4.5.1/include/stddef.h" 1 3 4
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

# 98 "square.c" 2
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

# 99 "square.c" 2

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
# 101 "square.c" 2
# 141 "square.c"
# 1 "square.tab" 1
static const byte Se[256] = {
177, 206, 195, 149, 90, 173, 231, 2, 77, 68, 251, 145, 12, 135, 161, 80,
203, 103, 84, 221, 70, 143, 225, 78, 240, 253, 252, 235, 249, 196, 26, 110,
 94, 245, 204, 141, 28, 86, 67, 254, 7, 97, 248, 117, 89, 255, 3, 34,
138, 209, 19, 238, 136, 0, 14, 52, 21, 128, 148, 227, 237, 181, 83, 35,
 75, 71, 23, 167, 144, 53, 171, 216, 184, 223, 79, 87, 154, 146, 219, 27,
 60, 200, 153, 4, 142, 224, 215, 125, 133, 187, 64, 44, 58, 69, 241, 66,
101, 32, 65, 24, 114, 37, 147, 112, 54, 5, 242, 11, 163, 121, 236, 8,
 39, 49, 50, 182, 124, 176, 10, 115, 91, 123, 183, 129, 210, 13, 106, 38,
158, 88, 156, 131, 116, 179, 172, 48, 122, 105, 119, 15, 174, 33, 222, 208,
 46, 151, 16, 164, 152, 168, 212, 104, 45, 98, 41, 109, 22, 73, 118, 199,
232, 193, 150, 55, 229, 202, 244, 233, 99, 18, 194, 166, 20, 188, 211, 40,
175, 47, 230, 36, 82, 198, 160, 9, 189, 140, 207, 93, 17, 95, 1, 197,
159, 61, 162, 155, 201, 59, 190, 81, 25, 31, 63, 92, 178, 239, 74, 205,
191, 186, 111, 100, 217, 243, 62, 180, 170, 220, 213, 6, 192, 126, 246, 102,
108, 132, 113, 56, 185, 29, 127, 157, 72, 139, 42, 218, 165, 51, 130, 57,
214, 120, 134, 250, 228, 43, 169, 30, 137, 96, 107, 234, 85, 76, 247, 226,
};

static const byte Sd[256] = {
 53, 190, 7, 46, 83, 105, 219, 40, 111, 183, 118, 107, 12, 125, 54, 139,
146, 188, 169, 50, 172, 56, 156, 66, 99, 200, 30, 79, 36, 229, 247, 201,
 97, 141, 47, 63, 179, 101, 127, 112, 175, 154, 234, 245, 91, 152, 144, 177,
135, 113, 114, 237, 55, 69, 104, 163, 227, 239, 92, 197, 80, 193, 214, 202,
 90, 98, 95, 38, 9, 93, 20, 65, 232, 157, 206, 64, 253, 8, 23, 74,
 15, 199, 180, 62, 18, 252, 37, 75, 129, 44, 4, 120, 203, 187, 32, 189,
249, 41, 153, 168, 211, 96, 223, 17, 151, 137, 126, 250, 224, 155, 31, 210,
103, 226, 100, 119, 132, 43, 158, 138, 241, 109, 136, 121, 116, 87, 221, 230,
 57, 123, 238, 131, 225, 88, 242, 13, 52, 248, 48, 233, 185, 35, 84, 21,
 68, 11, 77, 102, 58, 3, 162, 145, 148, 82, 76, 195, 130, 231, 128, 192,
182, 14, 194, 108, 147, 236, 171, 67, 149, 246, 216, 70, 134, 5, 140, 176,
117, 0, 204, 133, 215, 61, 115, 122, 72, 228, 209, 89, 173, 184, 198, 208,
220, 161, 170, 2, 29, 191, 181, 159, 81, 196, 165, 16, 34, 207, 1, 186,
143, 49, 124, 174, 150, 218, 240, 86, 71, 212, 235, 78, 217, 19, 142, 73,
 85, 22, 255, 59, 244, 164, 178, 6, 160, 167, 251, 27, 110, 60, 51, 205,
 24, 94, 106, 213, 166, 33, 222, 254, 42, 28, 243, 10, 26, 25, 39, 45,
};

static const byte G[4][4] = {
0x02U, 0x01U, 0x01U, 0x03U,
0x03U, 0x02U, 0x01U, 0x01U,
0x01U, 0x03U, 0x02U, 0x01U,
0x01U, 0x01U, 0x03U, 0x02U,
};

static const byte iG[4][4] = {
0x0eU, 0x09U, 0x0dU, 0x0bU,
0x0bU, 0x0eU, 0x09U, 0x0dU,
0x0dU, 0x0bU, 0x0eU, 0x09U,
0x09U, 0x0dU, 0x0bU, 0x0eU,
};

static const byte logtab[256] = {
  0, 0, 1, 134, 2, 13, 135, 76, 3, 210, 14, 174, 136, 34, 77, 147,
  4, 26, 211, 203, 15, 152, 175, 168, 137, 240, 35, 89, 78, 53, 148, 9,
  5, 143, 27, 110, 212, 57, 204, 187, 16, 104, 153, 119, 176, 223, 169, 114,
138, 250, 241, 160, 36, 82, 90, 96, 79, 47, 54, 220, 149, 50, 10, 31,
  6, 165, 144, 73, 28, 93, 111, 184, 213, 193, 58, 181, 205, 99, 188, 61,
 17, 68, 105, 129, 154, 39, 120, 196, 177, 230, 224, 234, 170, 85, 115, 216,
139, 246, 251, 22, 242, 244, 161, 64, 37, 66, 83, 228, 91, 163, 97, 191,
 80, 248, 48, 45, 55, 141, 221, 102, 150, 24, 51, 238, 11, 253, 32, 208,
  7, 87, 166, 201, 145, 172, 74, 132, 29, 218, 94, 158, 112, 117, 185, 108,
214, 232, 194, 127, 59, 179, 182, 71, 206, 236, 100, 43, 189, 226, 62, 20,
 18, 41, 69, 125, 106, 156, 130, 199, 155, 198, 40, 124, 121, 122, 197, 123,
178, 70, 231, 126, 225, 19, 235, 42, 171, 131, 86, 200, 116, 107, 217, 157,
140, 101, 247, 44, 252, 207, 23, 237, 243, 63, 245, 21, 162, 190, 65, 227,
 38, 195, 67, 128, 84, 215, 229, 233, 92, 183, 164, 72, 98, 60, 192, 180,
 81, 95, 249, 159, 49, 30, 46, 219, 56, 186, 142, 109, 222, 113, 103, 118,
151, 167, 25, 202, 52, 8, 239, 88, 12, 75, 254, 133, 33, 146, 209, 173,
};

static const byte alogtab[256] = {
  1, 2, 4, 8, 16, 32, 64, 128, 245, 31, 62, 124, 248, 5, 10, 20,
 40, 80, 160, 181, 159, 203, 99, 198, 121, 242, 17, 34, 68, 136, 229, 63,
126, 252, 13, 26, 52, 104, 208, 85, 170, 161, 183, 155, 195, 115, 230, 57,
114, 228, 61, 122, 244, 29, 58, 116, 232, 37, 74, 148, 221, 79, 158, 201,
103, 206, 105, 210, 81, 162, 177, 151, 219, 67, 134, 249, 7, 14, 28, 56,
112, 224, 53, 106, 212, 93, 186, 129, 247, 27, 54, 108, 216, 69, 138, 225,
 55, 110, 220, 77, 154, 193, 119, 238, 41, 82, 164, 189, 143, 235, 35, 70,
140, 237, 47, 94, 188, 141, 239, 43, 86, 172, 173, 175, 171, 163, 179, 147,
211, 83, 166, 185, 135, 251, 3, 6, 12, 24, 48, 96, 192, 117, 234, 33,
 66, 132, 253, 15, 30, 60, 120, 240, 21, 42, 84, 168, 165, 191, 139, 227,
 51, 102, 204, 109, 218, 65, 130, 241, 23, 46, 92, 184, 133, 255, 11, 22,
 44, 88, 176, 149, 223, 75, 150, 217, 71, 142, 233, 39, 78, 156, 205, 111,
222, 73, 146, 209, 87, 174, 169, 167, 187, 131, 243, 19, 38, 76, 152, 197,
127, 254, 9, 18, 36, 72, 144, 213, 95, 190, 137, 231, 59, 118, 236, 45,
 90, 180, 157, 207, 107, 214, 89, 178, 145, 215, 91, 182, 153, 199, 123, 246,
 25, 50, 100, 200, 101, 202, 97, 194, 113, 226, 49, 98, 196, 125, 250, 1,
};



static const word32 offset[8] = {
0x00000001UL, 0x00000002UL, 0x00000004UL, 0x00000008UL,
0x00000010UL, 0x00000020UL, 0x00000040UL, 0x00000080UL,
};

static const word32 Te0[256] = {
0x26b1b197UL, 0xa7cece69UL, 0xb0c3c373UL, 0x4a9595dfUL,
0xee5a5ab4UL, 0x02adadafUL, 0xdce7e73bUL, 0x06020204UL,
0xd74d4d9aUL, 0xcc444488UL, 0xf8fbfb03UL, 0x469191d7UL,
0x140c0c18UL, 0x7c8787fbUL, 0x16a1a1b7UL, 0xf05050a0UL,
0xa8cbcb63UL, 0xa96767ceUL, 0xfc5454a8UL, 0x92dddd4fUL,
0xca46468cUL, 0x648f8febUL, 0xd6e1e137UL, 0xd24e4e9cUL,
0xe5f0f015UL, 0xf2fdfd0fUL, 0xf1fcfc0dUL, 0xc8ebeb23UL,
0xfef9f907UL, 0xb9c4c47dUL, 0x2e1a1a34UL, 0xb26e6edcUL,
0xe25e5ebcUL, 0xeaf5f51fUL, 0xa1cccc6dUL, 0x628d8defUL,
0x241c1c38UL, 0xfa5656acUL, 0xc5434386UL, 0xf7fefe09UL,
0x0907070eUL, 0xa36161c2UL, 0xfdf8f805UL, 0x9f7575eaUL,
0xeb5959b2UL, 0xf4ffff0bUL, 0x05030306UL, 0x66222244UL,
0x6b8a8ae1UL, 0x86d1d157UL, 0x35131326UL, 0xc7eeee29UL,
0x6d8888e5UL, 0x00000000UL, 0x120e0e1cUL, 0x5c343468UL,
0x3f15152aUL, 0x758080f5UL, 0x499494ddUL, 0xd0e3e333UL,
0xc2eded2fUL, 0x2ab5b59fUL, 0xf55353a6UL, 0x65232346UL,
0xdd4b4b96UL, 0xc947478eUL, 0x3917172eUL, 0x1ca7a7bbUL,
0x459090d5UL, 0x5f35356aUL, 0x08ababa3UL, 0x9dd8d845UL,
0x3db8b885UL, 0x94dfdf4bUL, 0xd14f4f9eUL, 0xf95757aeUL,
0x5b9a9ac1UL, 0x439292d1UL, 0x98dbdb43UL, 0x2d1b1b36UL,
0x443c3c78UL, 0xadc8c865UL, 0x5e9999c7UL, 0x0c040408UL,
0x678e8ee9UL, 0xd5e0e035UL, 0x8cd7d75bUL, 0x877d7dfaUL,
0x7a8585ffUL, 0x38bbbb83UL, 0xc0404080UL, 0x742c2c58UL,
0x4e3a3a74UL, 0xcf45458aUL, 0xe6f1f117UL, 0xc6424284UL,
0xaf6565caUL, 0x60202040UL, 0xc3414182UL, 0x28181830UL,
0x967272e4UL, 0x6f25254aUL, 0x409393d3UL, 0x907070e0UL,
0x5a36366cUL, 0x0f05050aUL, 0xe3f2f211UL, 0x1d0b0b16UL,
0x10a3a3b3UL, 0x8b7979f2UL, 0xc1ecec2dUL, 0x18080810UL,
0x6927274eUL, 0x53313162UL, 0x56323264UL, 0x2fb6b699UL,
0x847c7cf8UL, 0x25b0b095UL, 0x1e0a0a14UL, 0x957373e6UL,
0xed5b5bb6UL, 0x8d7b7bf6UL, 0x2cb7b79bUL, 0x768181f7UL,
0x83d2d251UL, 0x170d0d1aUL, 0xbe6a6ad4UL, 0x6a26264cUL,
0x579e9ec9UL, 0xe85858b0UL, 0x519c9ccdUL, 0x708383f3UL,
0x9c7474e8UL, 0x20b3b393UL, 0x01acacadUL, 0x50303060UL,
0x8e7a7af4UL, 0xbb6969d2UL, 0x997777eeUL, 0x110f0f1eUL,
0x07aeaea9UL, 0x63212142UL, 0x97dede49UL, 0x85d0d055UL,
0x722e2e5cUL, 0x4c9797dbUL, 0x30101020UL, 0x19a4a4bdUL,
0x5d9898c5UL, 0x0da8a8a5UL, 0x89d4d45dUL, 0xb86868d0UL,
0x772d2d5aUL, 0xa66262c4UL, 0x7b292952UL, 0xb76d6ddaUL,
0x3a16162cUL, 0xdb494992UL, 0x9a7676ecUL, 0xbcc7c77bUL,
0xcde8e825UL, 0xb6c1c177UL, 0x4f9696d9UL, 0x5937376eUL,
0xdae5e53fUL, 0xabcaca61UL, 0xe9f4f41dUL, 0xcee9e927UL,
0xa56363c6UL, 0x36121224UL, 0xb3c2c271UL, 0x1fa6a6b9UL,
0x3c141428UL, 0x31bcbc8dUL, 0x80d3d353UL, 0x78282850UL,
0x04afafabUL, 0x712f2f5eUL, 0xdfe6e639UL, 0x6c242448UL,
0xf65252a4UL, 0xbfc6c679UL, 0x15a0a0b5UL, 0x1b090912UL,
0x32bdbd8fUL, 0x618c8cedUL, 0xa4cfcf6bUL, 0xe75d5dbaUL,
0x33111122UL, 0xe15f5fbeUL, 0x03010102UL, 0xbac5c57fUL,
0x549f9fcbUL, 0x473d3d7aUL, 0x13a2a2b1UL, 0x589b9bc3UL,
0xaec9c967UL, 0x4d3b3b76UL, 0x37bebe89UL, 0xf35151a2UL,
0x2b191932UL, 0x211f1f3eUL, 0x413f3f7eUL, 0xe45c5cb8UL,
0x23b2b291UL, 0xc4efef2bUL, 0xde4a4a94UL, 0xa2cdcd6fUL,
0x34bfbf8bUL, 0x3bbaba81UL, 0xb16f6fdeUL, 0xac6464c8UL,
0x9ed9d947UL, 0xe0f3f313UL, 0x423e3e7cUL, 0x29b4b49dUL,
0x0baaaaa1UL, 0x91dcdc4dUL, 0x8ad5d55fUL, 0x0a06060cUL,
0xb5c0c075UL, 0x827e7efcUL, 0xeff6f619UL, 0xaa6666ccUL,
0xb46c6cd8UL, 0x798484fdUL, 0x937171e2UL, 0x48383870UL,
0x3eb9b987UL, 0x271d1d3aUL, 0x817f7ffeUL, 0x529d9dcfUL,
0xd8484890UL, 0x688b8be3UL, 0x7e2a2a54UL, 0x9bdada41UL,
0x1aa5a5bfUL, 0x55333366UL, 0x738282f1UL, 0x4b393972UL,
0x8fd6d659UL, 0x887878f0UL, 0x7f8686f9UL, 0xfbfafa01UL,
0xd9e4e43dUL, 0x7d2b2b56UL, 0x0ea9a9a7UL, 0x221e1e3cUL,
0x6e8989e7UL, 0xa06060c0UL, 0xbd6b6bd6UL, 0xcbeaea21UL,
0xff5555aaUL, 0xd44c4c98UL, 0xecf7f71bUL, 0xd3e2e231UL,
};

static const word32 Te1[256] = {
0xb1b19726UL, 0xcece69a7UL, 0xc3c373b0UL, 0x9595df4aUL,
0x5a5ab4eeUL, 0xadadaf02UL, 0xe7e73bdcUL, 0x02020406UL,
0x4d4d9ad7UL, 0x444488ccUL, 0xfbfb03f8UL, 0x9191d746UL,
0x0c0c1814UL, 0x8787fb7cUL, 0xa1a1b716UL, 0x5050a0f0UL,
0xcbcb63a8UL, 0x6767cea9UL, 0x5454a8fcUL, 0xdddd4f92UL,
0x46468ccaUL, 0x8f8feb64UL, 0xe1e137d6UL, 0x4e4e9cd2UL,
0xf0f015e5UL, 0xfdfd0ff2UL, 0xfcfc0df1UL, 0xebeb23c8UL,
0xf9f907feUL, 0xc4c47db9UL, 0x1a1a342eUL, 0x6e6edcb2UL,
0x5e5ebce2UL, 0xf5f51feaUL, 0xcccc6da1UL, 0x8d8def62UL,
0x1c1c3824UL, 0x5656acfaUL, 0x434386c5UL, 0xfefe09f7UL,
0x07070e09UL, 0x6161c2a3UL, 0xf8f805fdUL, 0x7575ea9fUL,
0x5959b2ebUL, 0xffff0bf4UL, 0x03030605UL, 0x22224466UL,
0x8a8ae16bUL, 0xd1d15786UL, 0x13132635UL, 0xeeee29c7UL,
0x8888e56dUL, 0x00000000UL, 0x0e0e1c12UL, 0x3434685cUL,
0x15152a3fUL, 0x8080f575UL, 0x9494dd49UL, 0xe3e333d0UL,
0xeded2fc2UL, 0xb5b59f2aUL, 0x5353a6f5UL, 0x23234665UL,
0x4b4b96ddUL, 0x47478ec9UL, 0x17172e39UL, 0xa7a7bb1cUL,
0x9090d545UL, 0x35356a5fUL, 0xababa308UL, 0xd8d8459dUL,
0xb8b8853dUL, 0xdfdf4b94UL, 0x4f4f9ed1UL, 0x5757aef9UL,
0x9a9ac15bUL, 0x9292d143UL, 0xdbdb4398UL, 0x1b1b362dUL,
0x3c3c7844UL, 0xc8c865adUL, 0x9999c75eUL, 0x0404080cUL,
0x8e8ee967UL, 0xe0e035d5UL, 0xd7d75b8cUL, 0x7d7dfa87UL,
0x8585ff7aUL, 0xbbbb8338UL, 0x404080c0UL, 0x2c2c5874UL,
0x3a3a744eUL, 0x45458acfUL, 0xf1f117e6UL, 0x424284c6UL,
0x6565caafUL, 0x20204060UL, 0x414182c3UL, 0x18183028UL,
0x7272e496UL, 0x25254a6fUL, 0x9393d340UL, 0x7070e090UL,
0x36366c5aUL, 0x05050a0fUL, 0xf2f211e3UL, 0x0b0b161dUL,
0xa3a3b310UL, 0x7979f28bUL, 0xecec2dc1UL, 0x08081018UL,
0x27274e69UL, 0x31316253UL, 0x32326456UL, 0xb6b6992fUL,
0x7c7cf884UL, 0xb0b09525UL, 0x0a0a141eUL, 0x7373e695UL,
0x5b5bb6edUL, 0x7b7bf68dUL, 0xb7b79b2cUL, 0x8181f776UL,
0xd2d25183UL, 0x0d0d1a17UL, 0x6a6ad4beUL, 0x26264c6aUL,
0x9e9ec957UL, 0x5858b0e8UL, 0x9c9ccd51UL, 0x8383f370UL,
0x7474e89cUL, 0xb3b39320UL, 0xacacad01UL, 0x30306050UL,
0x7a7af48eUL, 0x6969d2bbUL, 0x7777ee99UL, 0x0f0f1e11UL,
0xaeaea907UL, 0x21214263UL, 0xdede4997UL, 0xd0d05585UL,
0x2e2e5c72UL, 0x9797db4cUL, 0x10102030UL, 0xa4a4bd19UL,
0x9898c55dUL, 0xa8a8a50dUL, 0xd4d45d89UL, 0x6868d0b8UL,
0x2d2d5a77UL, 0x6262c4a6UL, 0x2929527bUL, 0x6d6ddab7UL,
0x16162c3aUL, 0x494992dbUL, 0x7676ec9aUL, 0xc7c77bbcUL,
0xe8e825cdUL, 0xc1c177b6UL, 0x9696d94fUL, 0x37376e59UL,
0xe5e53fdaUL, 0xcaca61abUL, 0xf4f41de9UL, 0xe9e927ceUL,
0x6363c6a5UL, 0x12122436UL, 0xc2c271b3UL, 0xa6a6b91fUL,
0x1414283cUL, 0xbcbc8d31UL, 0xd3d35380UL, 0x28285078UL,
0xafafab04UL, 0x2f2f5e71UL, 0xe6e639dfUL, 0x2424486cUL,
0x5252a4f6UL, 0xc6c679bfUL, 0xa0a0b515UL, 0x0909121bUL,
0xbdbd8f32UL, 0x8c8ced61UL, 0xcfcf6ba4UL, 0x5d5dbae7UL,
0x11112233UL, 0x5f5fbee1UL, 0x01010203UL, 0xc5c57fbaUL,
0x9f9fcb54UL, 0x3d3d7a47UL, 0xa2a2b113UL, 0x9b9bc358UL,
0xc9c967aeUL, 0x3b3b764dUL, 0xbebe8937UL, 0x5151a2f3UL,
0x1919322bUL, 0x1f1f3e21UL, 0x3f3f7e41UL, 0x5c5cb8e4UL,
0xb2b29123UL, 0xefef2bc4UL, 0x4a4a94deUL, 0xcdcd6fa2UL,
0xbfbf8b34UL, 0xbaba813bUL, 0x6f6fdeb1UL, 0x6464c8acUL,
0xd9d9479eUL, 0xf3f313e0UL, 0x3e3e7c42UL, 0xb4b49d29UL,
0xaaaaa10bUL, 0xdcdc4d91UL, 0xd5d55f8aUL, 0x06060c0aUL,
0xc0c075b5UL, 0x7e7efc82UL, 0xf6f619efUL, 0x6666ccaaUL,
0x6c6cd8b4UL, 0x8484fd79UL, 0x7171e293UL, 0x38387048UL,
0xb9b9873eUL, 0x1d1d3a27UL, 0x7f7ffe81UL, 0x9d9dcf52UL,
0x484890d8UL, 0x8b8be368UL, 0x2a2a547eUL, 0xdada419bUL,
0xa5a5bf1aUL, 0x33336655UL, 0x8282f173UL, 0x3939724bUL,
0xd6d6598fUL, 0x7878f088UL, 0x8686f97fUL, 0xfafa01fbUL,
0xe4e43dd9UL, 0x2b2b567dUL, 0xa9a9a70eUL, 0x1e1e3c22UL,
0x8989e76eUL, 0x6060c0a0UL, 0x6b6bd6bdUL, 0xeaea21cbUL,
0x5555aaffUL, 0x4c4c98d4UL, 0xf7f71becUL, 0xe2e231d3UL,
};

static const word32 Te2[256] = {
0xb19726b1UL, 0xce69a7ceUL, 0xc373b0c3UL, 0x95df4a95UL,
0x5ab4ee5aUL, 0xadaf02adUL, 0xe73bdce7UL, 0x02040602UL,
0x4d9ad74dUL, 0x4488cc44UL, 0xfb03f8fbUL, 0x91d74691UL,
0x0c18140cUL, 0x87fb7c87UL, 0xa1b716a1UL, 0x50a0f050UL,
0xcb63a8cbUL, 0x67cea967UL, 0x54a8fc54UL, 0xdd4f92ddUL,
0x468cca46UL, 0x8feb648fUL, 0xe137d6e1UL, 0x4e9cd24eUL,
0xf015e5f0UL, 0xfd0ff2fdUL, 0xfc0df1fcUL, 0xeb23c8ebUL,
0xf907fef9UL, 0xc47db9c4UL, 0x1a342e1aUL, 0x6edcb26eUL,
0x5ebce25eUL, 0xf51feaf5UL, 0xcc6da1ccUL, 0x8def628dUL,
0x1c38241cUL, 0x56acfa56UL, 0x4386c543UL, 0xfe09f7feUL,
0x070e0907UL, 0x61c2a361UL, 0xf805fdf8UL, 0x75ea9f75UL,
0x59b2eb59UL, 0xff0bf4ffUL, 0x03060503UL, 0x22446622UL,
0x8ae16b8aUL, 0xd15786d1UL, 0x13263513UL, 0xee29c7eeUL,
0x88e56d88UL, 0x00000000UL, 0x0e1c120eUL, 0x34685c34UL,
0x152a3f15UL, 0x80f57580UL, 0x94dd4994UL, 0xe333d0e3UL,
0xed2fc2edUL, 0xb59f2ab5UL, 0x53a6f553UL, 0x23466523UL,
0x4b96dd4bUL, 0x478ec947UL, 0x172e3917UL, 0xa7bb1ca7UL,
0x90d54590UL, 0x356a5f35UL, 0xaba308abUL, 0xd8459dd8UL,
0xb8853db8UL, 0xdf4b94dfUL, 0x4f9ed14fUL, 0x57aef957UL,
0x9ac15b9aUL, 0x92d14392UL, 0xdb4398dbUL, 0x1b362d1bUL,
0x3c78443cUL, 0xc865adc8UL, 0x99c75e99UL, 0x04080c04UL,
0x8ee9678eUL, 0xe035d5e0UL, 0xd75b8cd7UL, 0x7dfa877dUL,
0x85ff7a85UL, 0xbb8338bbUL, 0x4080c040UL, 0x2c58742cUL,
0x3a744e3aUL, 0x458acf45UL, 0xf117e6f1UL, 0x4284c642UL,
0x65caaf65UL, 0x20406020UL, 0x4182c341UL, 0x18302818UL,
0x72e49672UL, 0x254a6f25UL, 0x93d34093UL, 0x70e09070UL,
0x366c5a36UL, 0x050a0f05UL, 0xf211e3f2UL, 0x0b161d0bUL,
0xa3b310a3UL, 0x79f28b79UL, 0xec2dc1ecUL, 0x08101808UL,
0x274e6927UL, 0x31625331UL, 0x32645632UL, 0xb6992fb6UL,
0x7cf8847cUL, 0xb09525b0UL, 0x0a141e0aUL, 0x73e69573UL,
0x5bb6ed5bUL, 0x7bf68d7bUL, 0xb79b2cb7UL, 0x81f77681UL,
0xd25183d2UL, 0x0d1a170dUL, 0x6ad4be6aUL, 0x264c6a26UL,
0x9ec9579eUL, 0x58b0e858UL, 0x9ccd519cUL, 0x83f37083UL,
0x74e89c74UL, 0xb39320b3UL, 0xacad01acUL, 0x30605030UL,
0x7af48e7aUL, 0x69d2bb69UL, 0x77ee9977UL, 0x0f1e110fUL,
0xaea907aeUL, 0x21426321UL, 0xde4997deUL, 0xd05585d0UL,
0x2e5c722eUL, 0x97db4c97UL, 0x10203010UL, 0xa4bd19a4UL,
0x98c55d98UL, 0xa8a50da8UL, 0xd45d89d4UL, 0x68d0b868UL,
0x2d5a772dUL, 0x62c4a662UL, 0x29527b29UL, 0x6ddab76dUL,
0x162c3a16UL, 0x4992db49UL, 0x76ec9a76UL, 0xc77bbcc7UL,
0xe825cde8UL, 0xc177b6c1UL, 0x96d94f96UL, 0x376e5937UL,
0xe53fdae5UL, 0xca61abcaUL, 0xf41de9f4UL, 0xe927cee9UL,
0x63c6a563UL, 0x12243612UL, 0xc271b3c2UL, 0xa6b91fa6UL,
0x14283c14UL, 0xbc8d31bcUL, 0xd35380d3UL, 0x28507828UL,
0xafab04afUL, 0x2f5e712fUL, 0xe639dfe6UL, 0x24486c24UL,
0x52a4f652UL, 0xc679bfc6UL, 0xa0b515a0UL, 0x09121b09UL,
0xbd8f32bdUL, 0x8ced618cUL, 0xcf6ba4cfUL, 0x5dbae75dUL,
0x11223311UL, 0x5fbee15fUL, 0x01020301UL, 0xc57fbac5UL,
0x9fcb549fUL, 0x3d7a473dUL, 0xa2b113a2UL, 0x9bc3589bUL,
0xc967aec9UL, 0x3b764d3bUL, 0xbe8937beUL, 0x51a2f351UL,
0x19322b19UL, 0x1f3e211fUL, 0x3f7e413fUL, 0x5cb8e45cUL,
0xb29123b2UL, 0xef2bc4efUL, 0x4a94de4aUL, 0xcd6fa2cdUL,
0xbf8b34bfUL, 0xba813bbaUL, 0x6fdeb16fUL, 0x64c8ac64UL,
0xd9479ed9UL, 0xf313e0f3UL, 0x3e7c423eUL, 0xb49d29b4UL,
0xaaa10baaUL, 0xdc4d91dcUL, 0xd55f8ad5UL, 0x060c0a06UL,
0xc075b5c0UL, 0x7efc827eUL, 0xf619eff6UL, 0x66ccaa66UL,
0x6cd8b46cUL, 0x84fd7984UL, 0x71e29371UL, 0x38704838UL,
0xb9873eb9UL, 0x1d3a271dUL, 0x7ffe817fUL, 0x9dcf529dUL,
0x4890d848UL, 0x8be3688bUL, 0x2a547e2aUL, 0xda419bdaUL,
0xa5bf1aa5UL, 0x33665533UL, 0x82f17382UL, 0x39724b39UL,
0xd6598fd6UL, 0x78f08878UL, 0x86f97f86UL, 0xfa01fbfaUL,
0xe43dd9e4UL, 0x2b567d2bUL, 0xa9a70ea9UL, 0x1e3c221eUL,
0x89e76e89UL, 0x60c0a060UL, 0x6bd6bd6bUL, 0xea21cbeaUL,
0x55aaff55UL, 0x4c98d44cUL, 0xf71becf7UL, 0xe231d3e2UL,
};

static const word32 Te3[256] = {
0x9726b1b1UL, 0x69a7ceceUL, 0x73b0c3c3UL, 0xdf4a9595UL,
0xb4ee5a5aUL, 0xaf02adadUL, 0x3bdce7e7UL, 0x04060202UL,
0x9ad74d4dUL, 0x88cc4444UL, 0x03f8fbfbUL, 0xd7469191UL,
0x18140c0cUL, 0xfb7c8787UL, 0xb716a1a1UL, 0xa0f05050UL,
0x63a8cbcbUL, 0xcea96767UL, 0xa8fc5454UL, 0x4f92ddddUL,
0x8cca4646UL, 0xeb648f8fUL, 0x37d6e1e1UL, 0x9cd24e4eUL,
0x15e5f0f0UL, 0x0ff2fdfdUL, 0x0df1fcfcUL, 0x23c8ebebUL,
0x07fef9f9UL, 0x7db9c4c4UL, 0x342e1a1aUL, 0xdcb26e6eUL,
0xbce25e5eUL, 0x1feaf5f5UL, 0x6da1ccccUL, 0xef628d8dUL,
0x38241c1cUL, 0xacfa5656UL, 0x86c54343UL, 0x09f7fefeUL,
0x0e090707UL, 0xc2a36161UL, 0x05fdf8f8UL, 0xea9f7575UL,
0xb2eb5959UL, 0x0bf4ffffUL, 0x06050303UL, 0x44662222UL,
0xe16b8a8aUL, 0x5786d1d1UL, 0x26351313UL, 0x29c7eeeeUL,
0xe56d8888UL, 0x00000000UL, 0x1c120e0eUL, 0x685c3434UL,
0x2a3f1515UL, 0xf5758080UL, 0xdd499494UL, 0x33d0e3e3UL,
0x2fc2ededUL, 0x9f2ab5b5UL, 0xa6f55353UL, 0x46652323UL,
0x96dd4b4bUL, 0x8ec94747UL, 0x2e391717UL, 0xbb1ca7a7UL,
0xd5459090UL, 0x6a5f3535UL, 0xa308ababUL, 0x459dd8d8UL,
0x853db8b8UL, 0x4b94dfdfUL, 0x9ed14f4fUL, 0xaef95757UL,
0xc15b9a9aUL, 0xd1439292UL, 0x4398dbdbUL, 0x362d1b1bUL,
0x78443c3cUL, 0x65adc8c8UL, 0xc75e9999UL, 0x080c0404UL,
0xe9678e8eUL, 0x35d5e0e0UL, 0x5b8cd7d7UL, 0xfa877d7dUL,
0xff7a8585UL, 0x8338bbbbUL, 0x80c04040UL, 0x58742c2cUL,
0x744e3a3aUL, 0x8acf4545UL, 0x17e6f1f1UL, 0x84c64242UL,
0xcaaf6565UL, 0x40602020UL, 0x82c34141UL, 0x30281818UL,
0xe4967272UL, 0x4a6f2525UL, 0xd3409393UL, 0xe0907070UL,
0x6c5a3636UL, 0x0a0f0505UL, 0x11e3f2f2UL, 0x161d0b0bUL,
0xb310a3a3UL, 0xf28b7979UL, 0x2dc1ececUL, 0x10180808UL,
0x4e692727UL, 0x62533131UL, 0x64563232UL, 0x992fb6b6UL,
0xf8847c7cUL, 0x9525b0b0UL, 0x141e0a0aUL, 0xe6957373UL,
0xb6ed5b5bUL, 0xf68d7b7bUL, 0x9b2cb7b7UL, 0xf7768181UL,
0x5183d2d2UL, 0x1a170d0dUL, 0xd4be6a6aUL, 0x4c6a2626UL,
0xc9579e9eUL, 0xb0e85858UL, 0xcd519c9cUL, 0xf3708383UL,
0xe89c7474UL, 0x9320b3b3UL, 0xad01acacUL, 0x60503030UL,
0xf48e7a7aUL, 0xd2bb6969UL, 0xee997777UL, 0x1e110f0fUL,
0xa907aeaeUL, 0x42632121UL, 0x4997dedeUL, 0x5585d0d0UL,
0x5c722e2eUL, 0xdb4c9797UL, 0x20301010UL, 0xbd19a4a4UL,
0xc55d9898UL, 0xa50da8a8UL, 0x5d89d4d4UL, 0xd0b86868UL,
0x5a772d2dUL, 0xc4a66262UL, 0x527b2929UL, 0xdab76d6dUL,
0x2c3a1616UL, 0x92db4949UL, 0xec9a7676UL, 0x7bbcc7c7UL,
0x25cde8e8UL, 0x77b6c1c1UL, 0xd94f9696UL, 0x6e593737UL,
0x3fdae5e5UL, 0x61abcacaUL, 0x1de9f4f4UL, 0x27cee9e9UL,
0xc6a56363UL, 0x24361212UL, 0x71b3c2c2UL, 0xb91fa6a6UL,
0x283c1414UL, 0x8d31bcbcUL, 0x5380d3d3UL, 0x50782828UL,
0xab04afafUL, 0x5e712f2fUL, 0x39dfe6e6UL, 0x486c2424UL,
0xa4f65252UL, 0x79bfc6c6UL, 0xb515a0a0UL, 0x121b0909UL,
0x8f32bdbdUL, 0xed618c8cUL, 0x6ba4cfcfUL, 0xbae75d5dUL,
0x22331111UL, 0xbee15f5fUL, 0x02030101UL, 0x7fbac5c5UL,
0xcb549f9fUL, 0x7a473d3dUL, 0xb113a2a2UL, 0xc3589b9bUL,
0x67aec9c9UL, 0x764d3b3bUL, 0x8937bebeUL, 0xa2f35151UL,
0x322b1919UL, 0x3e211f1fUL, 0x7e413f3fUL, 0xb8e45c5cUL,
0x9123b2b2UL, 0x2bc4efefUL, 0x94de4a4aUL, 0x6fa2cdcdUL,
0x8b34bfbfUL, 0x813bbabaUL, 0xdeb16f6fUL, 0xc8ac6464UL,
0x479ed9d9UL, 0x13e0f3f3UL, 0x7c423e3eUL, 0x9d29b4b4UL,
0xa10baaaaUL, 0x4d91dcdcUL, 0x5f8ad5d5UL, 0x0c0a0606UL,
0x75b5c0c0UL, 0xfc827e7eUL, 0x19eff6f6UL, 0xccaa6666UL,
0xd8b46c6cUL, 0xfd798484UL, 0xe2937171UL, 0x70483838UL,
0x873eb9b9UL, 0x3a271d1dUL, 0xfe817f7fUL, 0xcf529d9dUL,
0x90d84848UL, 0xe3688b8bUL, 0x547e2a2aUL, 0x419bdadaUL,
0xbf1aa5a5UL, 0x66553333UL, 0xf1738282UL, 0x724b3939UL,
0x598fd6d6UL, 0xf0887878UL, 0xf97f8686UL, 0x01fbfafaUL,
0x3dd9e4e4UL, 0x567d2b2bUL, 0xa70ea9a9UL, 0x3c221e1eUL,
0xe76e8989UL, 0xc0a06060UL, 0xd6bd6b6bUL, 0x21cbeaeaUL,
0xaaff5555UL, 0x98d44c4cUL, 0x1becf7f7UL, 0x31d3e2e2UL,
};

static const word32 Td0[256] = {
0x02bc68e3UL, 0x0c628555UL, 0x31233f2aUL, 0xf713ab61UL,
0x726dd498UL, 0x199acb21UL, 0x61a4223cUL, 0xcd3d9d45UL,
0x23b4fd05UL, 0x5f07c42bUL, 0xc0012c9bUL, 0x0f80d93dUL,
0x745c6c48UL, 0x857e7ff9UL, 0x1fab73f1UL, 0x0edeedb6UL,
0xed6b3c28UL, 0x1a789749UL, 0x8d912a9fUL, 0x339f57c9UL,
0xaaa807a9UL, 0x7ded0da5UL, 0x8f2d427cUL, 0xc9b04d76UL,
0x57e8914dUL, 0xcc63a9ceUL, 0xd296eeb4UL, 0xb6e12830UL,
0xb961f10dUL, 0x266719bdUL, 0x80ad9b41UL, 0xc76ea0c0UL,
0x41f28351UL, 0x34f0db92UL, 0xfc1ea26fUL, 0x4cce328fUL,
0x7333e013UL, 0x6dc6a769UL, 0x93646de5UL, 0xfa2f1abfUL,
0xb7bf1cbbUL, 0xb5037458UL, 0x4f2c6ee7UL, 0x96b7895dUL,
0x2a059ce8UL, 0xa3196644UL, 0xfb712e34UL, 0x6529f20fUL,
0x7a8281feUL, 0xf12213b1UL, 0xec3508a3UL, 0x7e0f51cdUL,
0x14a67affUL, 0xf893725cUL, 0x1297c22fUL, 0xc3e370f3UL,
0x1c492f99UL, 0x681543d1UL, 0x1b26a3c2UL, 0xb332cc88UL,
0x6f7acf8aUL, 0x9f06e8b0UL, 0x1ef5477aUL, 0xda79bbd2UL,
0x210895e6UL, 0x5ce59843UL, 0x0631b8d0UL, 0xaf7be311UL,
0x5365417eUL, 0x102baaccUL, 0x9ce4b4d8UL, 0xd4a75664UL,
0x59367cfbUL, 0x84204b72UL, 0xf64d9feaUL, 0xdfaa5f6aUL,
0xcedfc12dUL, 0x58684870UL, 0x81f3afcaUL, 0x91d80506UL,
0x694b775aUL, 0xa528de94UL, 0x4210df39UL, 0x47c33b81UL,
0xa6ca82fcUL, 0xc5d2c823UL, 0xb26cf803UL, 0x9ad50c08UL,
0x40acb7daUL, 0xe109b97dUL, 0x2c342438UL, 0xa24752cfUL,
0xd174b2dcUL, 0x2b5ba863UL, 0x9555d535UL, 0x11759e47UL,
0xe2ebe515UL, 0xc630944bUL, 0xa8146f4aUL, 0x869c2391UL,
0x39cc6a4cUL, 0x4aff8a5fUL, 0x4d900604UL, 0xbbdd99eeUL,
0xca52111eUL, 0x18c4ffaaUL, 0x986964ebUL, 0xfffcfe07UL,
0x015e348bUL, 0xbe0e7d56UL, 0xd99be7baUL, 0x32c16342UL,
0x7bdcb575UL, 0x17442697UL, 0x66cbae67UL, 0xcb0c2595UL,
0x67959aecUL, 0xd02a8657UL, 0x99375060UL, 0x05d3e4b8UL,
0xba83ad65UL, 0x35aeef19UL, 0x13c9f6a4UL, 0xa94a5bc1UL,
0xd61b3e87UL, 0x5e59f0a0UL, 0x5b8a1418UL, 0x3b7002afUL,
0x76e004abUL, 0xbf5049ddUL, 0x63184adfUL, 0x56b6a5c6UL,
0x0a533d85UL, 0x371287faUL, 0xa794b677UL, 0x7f516546UL,
0x09b161edUL, 0xe9e6ec1bUL, 0x258545d5UL, 0x523b75f5UL,
0x3d41ba7fUL, 0x8842ce27UL, 0x434eebb2UL, 0x97e9bdd6UL,
0xf39e7b52UL, 0x457f5362UL, 0xa0fb3a2cUL, 0x70d1bc7bUL,
0x6bf71fb9UL, 0x1d171b12UL, 0xc8ee79fdUL, 0xf07c273aUL,
0xd7450a0cUL, 0x7960dd96UL, 0xabf63322UL, 0x891cfaacUL,
0x5dbbacc8UL, 0x307d0ba1UL, 0x4ba1bed4UL, 0x940be1beUL,
0x540acd25UL, 0x62467e54UL, 0x8211f3a2UL, 0x3ea3e617UL,
0xe6663526UL, 0x750258c3UL, 0x9b8b3883UL, 0xc2bd4478UL,
0xdc480302UL, 0x8ba0924fUL, 0x7cb3392eUL, 0xe584694eUL,
0x718f88f0UL, 0x27392d36UL, 0x3ffdd29cUL, 0x6e24fb01UL,
0xdd163789UL, 0x00000000UL, 0xe0578df6UL, 0x6c9893e2UL,
0x15f84e74UL, 0x5ad42093UL, 0xe73801adUL, 0xb45d40d3UL,
0x87c2171aUL, 0x2d6a10b3UL, 0x2fd67850UL, 0x3c1f8ef4UL,
0xa1a50ea7UL, 0x364cb371UL, 0xae25d79aUL, 0x24db715eUL,
0x50871d16UL, 0xd5f962efUL, 0x9086318dUL, 0x161a121cUL,
0xcf81f5a6UL, 0x076f8c5bUL, 0x491dd637UL, 0x923a596eUL,
0x6477c684UL, 0xb83fc586UL, 0xf9cd46d7UL, 0xb0d090e0UL,
0x834fc729UL, 0xfd4096e4UL, 0x0b0d090eUL, 0x2056a16dUL,
0x22eac98eUL, 0x2e884cdbUL, 0x8e7376f7UL, 0xbcb215b5UL,
0xc15f1810UL, 0x6aa92b32UL, 0xb18ea46bUL, 0x5554f9aeUL,
0xee896040UL, 0x08ef5566UL, 0x442167e9UL, 0xbdec213eUL,
0x77be3020UL, 0xadc78bf2UL, 0x29e7c080UL, 0x8ccf1e14UL,
0x4843e2bcUL, 0x8afea6c4UL, 0xd8c5d331UL, 0x60fa16b7UL,
0x9dba8053UL, 0xf2c04fd9UL, 0x783ee91dUL, 0x3a2e3624UL,
0xdef46be1UL, 0xefd754cbUL, 0xf4f1f709UL, 0xf5afc382UL,
0x28b9f40bUL, 0x51d9299dUL, 0x38925ec7UL, 0xeb5a84f8UL,
0xe8b8d890UL, 0x0d3cb1deUL, 0x048dd033UL, 0x03e25c68UL,
0xe4da5dc5UL, 0x9e58dc3bUL, 0x469d0f0aUL, 0xd3c8da3fUL,
0xdb278f59UL, 0xc48cfca8UL, 0xac99bf79UL, 0x4e725a6cUL,
0xfea2ca8cUL, 0xe3b5d19eUL, 0xa476ea1fUL, 0xea04b073UL,
};

static const word32 Td1[256] = {
0xbc68e302UL, 0x6285550cUL, 0x233f2a31UL, 0x13ab61f7UL,
0x6dd49872UL, 0x9acb2119UL, 0xa4223c61UL, 0x3d9d45cdUL,
0xb4fd0523UL, 0x07c42b5fUL, 0x012c9bc0UL, 0x80d93d0fUL,
0x5c6c4874UL, 0x7e7ff985UL, 0xab73f11fUL, 0xdeedb60eUL,
0x6b3c28edUL, 0x7897491aUL, 0x912a9f8dUL, 0x9f57c933UL,
0xa807a9aaUL, 0xed0da57dUL, 0x2d427c8fUL, 0xb04d76c9UL,
0xe8914d57UL, 0x63a9ceccUL, 0x96eeb4d2UL, 0xe12830b6UL,
0x61f10db9UL, 0x6719bd26UL, 0xad9b4180UL, 0x6ea0c0c7UL,
0xf2835141UL, 0xf0db9234UL, 0x1ea26ffcUL, 0xce328f4cUL,
0x33e01373UL, 0xc6a7696dUL, 0x646de593UL, 0x2f1abffaUL,
0xbf1cbbb7UL, 0x037458b5UL, 0x2c6ee74fUL, 0xb7895d96UL,
0x059ce82aUL, 0x196644a3UL, 0x712e34fbUL, 0x29f20f65UL,
0x8281fe7aUL, 0x2213b1f1UL, 0x3508a3ecUL, 0x0f51cd7eUL,
0xa67aff14UL, 0x93725cf8UL, 0x97c22f12UL, 0xe370f3c3UL,
0x492f991cUL, 0x1543d168UL, 0x26a3c21bUL, 0x32cc88b3UL,
0x7acf8a6fUL, 0x06e8b09fUL, 0xf5477a1eUL, 0x79bbd2daUL,
0x0895e621UL, 0xe598435cUL, 0x31b8d006UL, 0x7be311afUL,
0x65417e53UL, 0x2baacc10UL, 0xe4b4d89cUL, 0xa75664d4UL,
0x367cfb59UL, 0x204b7284UL, 0x4d9feaf6UL, 0xaa5f6adfUL,
0xdfc12dceUL, 0x68487058UL, 0xf3afca81UL, 0xd8050691UL,
0x4b775a69UL, 0x28de94a5UL, 0x10df3942UL, 0xc33b8147UL,
0xca82fca6UL, 0xd2c823c5UL, 0x6cf803b2UL, 0xd50c089aUL,
0xacb7da40UL, 0x09b97de1UL, 0x3424382cUL, 0x4752cfa2UL,
0x74b2dcd1UL, 0x5ba8632bUL, 0x55d53595UL, 0x759e4711UL,
0xebe515e2UL, 0x30944bc6UL, 0x146f4aa8UL, 0x9c239186UL,
0xcc6a4c39UL, 0xff8a5f4aUL, 0x9006044dUL, 0xdd99eebbUL,
0x52111ecaUL, 0xc4ffaa18UL, 0x6964eb98UL, 0xfcfe07ffUL,
0x5e348b01UL, 0x0e7d56beUL, 0x9be7bad9UL, 0xc1634232UL,
0xdcb5757bUL, 0x44269717UL, 0xcbae6766UL, 0x0c2595cbUL,
0x959aec67UL, 0x2a8657d0UL, 0x37506099UL, 0xd3e4b805UL,
0x83ad65baUL, 0xaeef1935UL, 0xc9f6a413UL, 0x4a5bc1a9UL,
0x1b3e87d6UL, 0x59f0a05eUL, 0x8a14185bUL, 0x7002af3bUL,
0xe004ab76UL, 0x5049ddbfUL, 0x184adf63UL, 0xb6a5c656UL,
0x533d850aUL, 0x1287fa37UL, 0x94b677a7UL, 0x5165467fUL,
0xb161ed09UL, 0xe6ec1be9UL, 0x8545d525UL, 0x3b75f552UL,
0x41ba7f3dUL, 0x42ce2788UL, 0x4eebb243UL, 0xe9bdd697UL,
0x9e7b52f3UL, 0x7f536245UL, 0xfb3a2ca0UL, 0xd1bc7b70UL,
0xf71fb96bUL, 0x171b121dUL, 0xee79fdc8UL, 0x7c273af0UL,
0x450a0cd7UL, 0x60dd9679UL, 0xf63322abUL, 0x1cfaac89UL,
0xbbacc85dUL, 0x7d0ba130UL, 0xa1bed44bUL, 0x0be1be94UL,
0x0acd2554UL, 0x467e5462UL, 0x11f3a282UL, 0xa3e6173eUL,
0x663526e6UL, 0x0258c375UL, 0x8b38839bUL, 0xbd4478c2UL,
0x480302dcUL, 0xa0924f8bUL, 0xb3392e7cUL, 0x84694ee5UL,
0x8f88f071UL, 0x392d3627UL, 0xfdd29c3fUL, 0x24fb016eUL,
0x163789ddUL, 0x00000000UL, 0x578df6e0UL, 0x9893e26cUL,
0xf84e7415UL, 0xd420935aUL, 0x3801ade7UL, 0x5d40d3b4UL,
0xc2171a87UL, 0x6a10b32dUL, 0xd678502fUL, 0x1f8ef43cUL,
0xa50ea7a1UL, 0x4cb37136UL, 0x25d79aaeUL, 0xdb715e24UL,
0x871d1650UL, 0xf962efd5UL, 0x86318d90UL, 0x1a121c16UL,
0x81f5a6cfUL, 0x6f8c5b07UL, 0x1dd63749UL, 0x3a596e92UL,
0x77c68464UL, 0x3fc586b8UL, 0xcd46d7f9UL, 0xd090e0b0UL,
0x4fc72983UL, 0x4096e4fdUL, 0x0d090e0bUL, 0x56a16d20UL,
0xeac98e22UL, 0x884cdb2eUL, 0x7376f78eUL, 0xb215b5bcUL,
0x5f1810c1UL, 0xa92b326aUL, 0x8ea46bb1UL, 0x54f9ae55UL,
0x896040eeUL, 0xef556608UL, 0x2167e944UL, 0xec213ebdUL,
0xbe302077UL, 0xc78bf2adUL, 0xe7c08029UL, 0xcf1e148cUL,
0x43e2bc48UL, 0xfea6c48aUL, 0xc5d331d8UL, 0xfa16b760UL,
0xba80539dUL, 0xc04fd9f2UL, 0x3ee91d78UL, 0x2e36243aUL,
0xf46be1deUL, 0xd754cbefUL, 0xf1f709f4UL, 0xafc382f5UL,
0xb9f40b28UL, 0xd9299d51UL, 0x925ec738UL, 0x5a84f8ebUL,
0xb8d890e8UL, 0x3cb1de0dUL, 0x8dd03304UL, 0xe25c6803UL,
0xda5dc5e4UL, 0x58dc3b9eUL, 0x9d0f0a46UL, 0xc8da3fd3UL,
0x278f59dbUL, 0x8cfca8c4UL, 0x99bf79acUL, 0x725a6c4eUL,
0xa2ca8cfeUL, 0xb5d19ee3UL, 0x76ea1fa4UL, 0x04b073eaUL,
};

static const word32 Td2[256] = {
0x68e302bcUL, 0x85550c62UL, 0x3f2a3123UL, 0xab61f713UL,
0xd498726dUL, 0xcb21199aUL, 0x223c61a4UL, 0x9d45cd3dUL,
0xfd0523b4UL, 0xc42b5f07UL, 0x2c9bc001UL, 0xd93d0f80UL,
0x6c48745cUL, 0x7ff9857eUL, 0x73f11fabUL, 0xedb60edeUL,
0x3c28ed6bUL, 0x97491a78UL, 0x2a9f8d91UL, 0x57c9339fUL,
0x07a9aaa8UL, 0x0da57dedUL, 0x427c8f2dUL, 0x4d76c9b0UL,
0x914d57e8UL, 0xa9cecc63UL, 0xeeb4d296UL, 0x2830b6e1UL,
0xf10db961UL, 0x19bd2667UL, 0x9b4180adUL, 0xa0c0c76eUL,
0x835141f2UL, 0xdb9234f0UL, 0xa26ffc1eUL, 0x328f4cceUL,
0xe0137333UL, 0xa7696dc6UL, 0x6de59364UL, 0x1abffa2fUL,
0x1cbbb7bfUL, 0x7458b503UL, 0x6ee74f2cUL, 0x895d96b7UL,
0x9ce82a05UL, 0x6644a319UL, 0x2e34fb71UL, 0xf20f6529UL,
0x81fe7a82UL, 0x13b1f122UL, 0x08a3ec35UL, 0x51cd7e0fUL,
0x7aff14a6UL, 0x725cf893UL, 0xc22f1297UL, 0x70f3c3e3UL,
0x2f991c49UL, 0x43d16815UL, 0xa3c21b26UL, 0xcc88b332UL,
0xcf8a6f7aUL, 0xe8b09f06UL, 0x477a1ef5UL, 0xbbd2da79UL,
0x95e62108UL, 0x98435ce5UL, 0xb8d00631UL, 0xe311af7bUL,
0x417e5365UL, 0xaacc102bUL, 0xb4d89ce4UL, 0x5664d4a7UL,
0x7cfb5936UL, 0x4b728420UL, 0x9feaf64dUL, 0x5f6adfaaUL,
0xc12dcedfUL, 0x48705868UL, 0xafca81f3UL, 0x050691d8UL,
0x775a694bUL, 0xde94a528UL, 0xdf394210UL, 0x3b8147c3UL,
0x82fca6caUL, 0xc823c5d2UL, 0xf803b26cUL, 0x0c089ad5UL,
0xb7da40acUL, 0xb97de109UL, 0x24382c34UL, 0x52cfa247UL,
0xb2dcd174UL, 0xa8632b5bUL, 0xd5359555UL, 0x9e471175UL,
0xe515e2ebUL, 0x944bc630UL, 0x6f4aa814UL, 0x2391869cUL,
0x6a4c39ccUL, 0x8a5f4affUL, 0x06044d90UL, 0x99eebbddUL,
0x111eca52UL, 0xffaa18c4UL, 0x64eb9869UL, 0xfe07fffcUL,
0x348b015eUL, 0x7d56be0eUL, 0xe7bad99bUL, 0x634232c1UL,
0xb5757bdcUL, 0x26971744UL, 0xae6766cbUL, 0x2595cb0cUL,
0x9aec6795UL, 0x8657d02aUL, 0x50609937UL, 0xe4b805d3UL,
0xad65ba83UL, 0xef1935aeUL, 0xf6a413c9UL, 0x5bc1a94aUL,
0x3e87d61bUL, 0xf0a05e59UL, 0x14185b8aUL, 0x02af3b70UL,
0x04ab76e0UL, 0x49ddbf50UL, 0x4adf6318UL, 0xa5c656b6UL,
0x3d850a53UL, 0x87fa3712UL, 0xb677a794UL, 0x65467f51UL,
0x61ed09b1UL, 0xec1be9e6UL, 0x45d52585UL, 0x75f5523bUL,
0xba7f3d41UL, 0xce278842UL, 0xebb2434eUL, 0xbdd697e9UL,
0x7b52f39eUL, 0x5362457fUL, 0x3a2ca0fbUL, 0xbc7b70d1UL,
0x1fb96bf7UL, 0x1b121d17UL, 0x79fdc8eeUL, 0x273af07cUL,
0x0a0cd745UL, 0xdd967960UL, 0x3322abf6UL, 0xfaac891cUL,
0xacc85dbbUL, 0x0ba1307dUL, 0xbed44ba1UL, 0xe1be940bUL,
0xcd25540aUL, 0x7e546246UL, 0xf3a28211UL, 0xe6173ea3UL,
0x3526e666UL, 0x58c37502UL, 0x38839b8bUL, 0x4478c2bdUL,
0x0302dc48UL, 0x924f8ba0UL, 0x392e7cb3UL, 0x694ee584UL,
0x88f0718fUL, 0x2d362739UL, 0xd29c3ffdUL, 0xfb016e24UL,
0x3789dd16UL, 0x00000000UL, 0x8df6e057UL, 0x93e26c98UL,
0x4e7415f8UL, 0x20935ad4UL, 0x01ade738UL, 0x40d3b45dUL,
0x171a87c2UL, 0x10b32d6aUL, 0x78502fd6UL, 0x8ef43c1fUL,
0x0ea7a1a5UL, 0xb371364cUL, 0xd79aae25UL, 0x715e24dbUL,
0x1d165087UL, 0x62efd5f9UL, 0x318d9086UL, 0x121c161aUL,
0xf5a6cf81UL, 0x8c5b076fUL, 0xd637491dUL, 0x596e923aUL,
0xc6846477UL, 0xc586b83fUL, 0x46d7f9cdUL, 0x90e0b0d0UL,
0xc729834fUL, 0x96e4fd40UL, 0x090e0b0dUL, 0xa16d2056UL,
0xc98e22eaUL, 0x4cdb2e88UL, 0x76f78e73UL, 0x15b5bcb2UL,
0x1810c15fUL, 0x2b326aa9UL, 0xa46bb18eUL, 0xf9ae5554UL,
0x6040ee89UL, 0x556608efUL, 0x67e94421UL, 0x213ebdecUL,
0x302077beUL, 0x8bf2adc7UL, 0xc08029e7UL, 0x1e148ccfUL,
0xe2bc4843UL, 0xa6c48afeUL, 0xd331d8c5UL, 0x16b760faUL,
0x80539dbaUL, 0x4fd9f2c0UL, 0xe91d783eUL, 0x36243a2eUL,
0x6be1def4UL, 0x54cbefd7UL, 0xf709f4f1UL, 0xc382f5afUL,
0xf40b28b9UL, 0x299d51d9UL, 0x5ec73892UL, 0x84f8eb5aUL,
0xd890e8b8UL, 0xb1de0d3cUL, 0xd033048dUL, 0x5c6803e2UL,
0x5dc5e4daUL, 0xdc3b9e58UL, 0x0f0a469dUL, 0xda3fd3c8UL,
0x8f59db27UL, 0xfca8c48cUL, 0xbf79ac99UL, 0x5a6c4e72UL,
0xca8cfea2UL, 0xd19ee3b5UL, 0xea1fa476UL, 0xb073ea04UL,
};

static const word32 Td3[256] = {
0xe302bc68UL, 0x550c6285UL, 0x2a31233fUL, 0x61f713abUL,
0x98726dd4UL, 0x21199acbUL, 0x3c61a422UL, 0x45cd3d9dUL,
0x0523b4fdUL, 0x2b5f07c4UL, 0x9bc0012cUL, 0x3d0f80d9UL,
0x48745c6cUL, 0xf9857e7fUL, 0xf11fab73UL, 0xb60edeedUL,
0x28ed6b3cUL, 0x491a7897UL, 0x9f8d912aUL, 0xc9339f57UL,
0xa9aaa807UL, 0xa57ded0dUL, 0x7c8f2d42UL, 0x76c9b04dUL,
0x4d57e891UL, 0xcecc63a9UL, 0xb4d296eeUL, 0x30b6e128UL,
0x0db961f1UL, 0xbd266719UL, 0x4180ad9bUL, 0xc0c76ea0UL,
0x5141f283UL, 0x9234f0dbUL, 0x6ffc1ea2UL, 0x8f4cce32UL,
0x137333e0UL, 0x696dc6a7UL, 0xe593646dUL, 0xbffa2f1aUL,
0xbbb7bf1cUL, 0x58b50374UL, 0xe74f2c6eUL, 0x5d96b789UL,
0xe82a059cUL, 0x44a31966UL, 0x34fb712eUL, 0x0f6529f2UL,
0xfe7a8281UL, 0xb1f12213UL, 0xa3ec3508UL, 0xcd7e0f51UL,
0xff14a67aUL, 0x5cf89372UL, 0x2f1297c2UL, 0xf3c3e370UL,
0x991c492fUL, 0xd1681543UL, 0xc21b26a3UL, 0x88b332ccUL,
0x8a6f7acfUL, 0xb09f06e8UL, 0x7a1ef547UL, 0xd2da79bbUL,
0xe6210895UL, 0x435ce598UL, 0xd00631b8UL, 0x11af7be3UL,
0x7e536541UL, 0xcc102baaUL, 0xd89ce4b4UL, 0x64d4a756UL,
0xfb59367cUL, 0x7284204bUL, 0xeaf64d9fUL, 0x6adfaa5fUL,
0x2dcedfc1UL, 0x70586848UL, 0xca81f3afUL, 0x0691d805UL,
0x5a694b77UL, 0x94a528deUL, 0x394210dfUL, 0x8147c33bUL,
0xfca6ca82UL, 0x23c5d2c8UL, 0x03b26cf8UL, 0x089ad50cUL,
0xda40acb7UL, 0x7de109b9UL, 0x382c3424UL, 0xcfa24752UL,
0xdcd174b2UL, 0x632b5ba8UL, 0x359555d5UL, 0x4711759eUL,
0x15e2ebe5UL, 0x4bc63094UL, 0x4aa8146fUL, 0x91869c23UL,
0x4c39cc6aUL, 0x5f4aff8aUL, 0x044d9006UL, 0xeebbdd99UL,
0x1eca5211UL, 0xaa18c4ffUL, 0xeb986964UL, 0x07fffcfeUL,
0x8b015e34UL, 0x56be0e7dUL, 0xbad99be7UL, 0x4232c163UL,
0x757bdcb5UL, 0x97174426UL, 0x6766cbaeUL, 0x95cb0c25UL,
0xec67959aUL, 0x57d02a86UL, 0x60993750UL, 0xb805d3e4UL,
0x65ba83adUL, 0x1935aeefUL, 0xa413c9f6UL, 0xc1a94a5bUL,
0x87d61b3eUL, 0xa05e59f0UL, 0x185b8a14UL, 0xaf3b7002UL,
0xab76e004UL, 0xddbf5049UL, 0xdf63184aUL, 0xc656b6a5UL,
0x850a533dUL, 0xfa371287UL, 0x77a794b6UL, 0x467f5165UL,
0xed09b161UL, 0x1be9e6ecUL, 0xd5258545UL, 0xf5523b75UL,
0x7f3d41baUL, 0x278842ceUL, 0xb2434eebUL, 0xd697e9bdUL,
0x52f39e7bUL, 0x62457f53UL, 0x2ca0fb3aUL, 0x7b70d1bcUL,
0xb96bf71fUL, 0x121d171bUL, 0xfdc8ee79UL, 0x3af07c27UL,
0x0cd7450aUL, 0x967960ddUL, 0x22abf633UL, 0xac891cfaUL,
0xc85dbbacUL, 0xa1307d0bUL, 0xd44ba1beUL, 0xbe940be1UL,
0x25540acdUL, 0x5462467eUL, 0xa28211f3UL, 0x173ea3e6UL,
0x26e66635UL, 0xc3750258UL, 0x839b8b38UL, 0x78c2bd44UL,
0x02dc4803UL, 0x4f8ba092UL, 0x2e7cb339UL, 0x4ee58469UL,
0xf0718f88UL, 0x3627392dUL, 0x9c3ffdd2UL, 0x016e24fbUL,
0x89dd1637UL, 0x00000000UL, 0xf6e0578dUL, 0xe26c9893UL,
0x7415f84eUL, 0x935ad420UL, 0xade73801UL, 0xd3b45d40UL,
0x1a87c217UL, 0xb32d6a10UL, 0x502fd678UL, 0xf43c1f8eUL,
0xa7a1a50eUL, 0x71364cb3UL, 0x9aae25d7UL, 0x5e24db71UL,
0x1650871dUL, 0xefd5f962UL, 0x8d908631UL, 0x1c161a12UL,
0xa6cf81f5UL, 0x5b076f8cUL, 0x37491dd6UL, 0x6e923a59UL,
0x846477c6UL, 0x86b83fc5UL, 0xd7f9cd46UL, 0xe0b0d090UL,
0x29834fc7UL, 0xe4fd4096UL, 0x0e0b0d09UL, 0x6d2056a1UL,
0x8e22eac9UL, 0xdb2e884cUL, 0xf78e7376UL, 0xb5bcb215UL,
0x10c15f18UL, 0x326aa92bUL, 0x6bb18ea4UL, 0xae5554f9UL,
0x40ee8960UL, 0x6608ef55UL, 0xe9442167UL, 0x3ebdec21UL,
0x2077be30UL, 0xf2adc78bUL, 0x8029e7c0UL, 0x148ccf1eUL,
0xbc4843e2UL, 0xc48afea6UL, 0x31d8c5d3UL, 0xb760fa16UL,
0x539dba80UL, 0xd9f2c04fUL, 0x1d783ee9UL, 0x243a2e36UL,
0xe1def46bUL, 0xcbefd754UL, 0x09f4f1f7UL, 0x82f5afc3UL,
0x0b28b9f4UL, 0x9d51d929UL, 0xc738925eUL, 0xf8eb5a84UL,
0x90e8b8d8UL, 0xde0d3cb1UL, 0x33048dd0UL, 0x6803e25cUL,
0xc5e4da5dUL, 0x3b9e58dcUL, 0x0a469d0fUL, 0x3fd3c8daUL,
0x59db278fUL, 0xa8c48cfcUL, 0x79ac99bfUL, 0x6c4e725aUL,
0x8cfea2caUL, 0x9ee3b5d1UL, 0x1fa476eaUL, 0x73ea04b0UL,
};
# 142 "square.c" 2

const char *squareBanner =
 "Square cipher v.2.5 (compiled on " "Nov 20 2018" " " "10:55:13" ").\n"






 ;
# 210 "square.c"
static void squareTransform (word32 roundKey[4])

{
 int i, j;
 word16 mtemp;
 byte A[4][4], B[4][4];

 for (i = 0; i < 4; i++) {
  A[i][0] = ((byte) ((roundKey[i]) ));
  A[i][1] = ((byte) ((roundKey[i]) >> 8));
  A[i][2] = ((byte) ((roundKey[i]) >> 16));
  A[i][3] = ((byte) ((roundKey[i]) >> 24));
 }


 for (i = 0; i < 4; i++) {
  for (j = 0; j < 4; j++) {
   B[i][j] =
    ((A[i][0] && G[0][j]) ? alogtab[(mtemp = logtab[A[i][0]] + logtab[G[0][j]]) >= 255 ? mtemp - 255 : mtemp] : 0) ^
    ((A[i][1] && G[1][j]) ? alogtab[(mtemp = logtab[A[i][1]] + logtab[G[1][j]]) >= 255 ? mtemp - 255 : mtemp] : 0) ^
    ((A[i][2] && G[2][j]) ? alogtab[(mtemp = logtab[A[i][2]] + logtab[G[2][j]]) >= 255 ? mtemp - 255 : mtemp] : 0) ^
    ((A[i][3] && G[3][j]) ? alogtab[(mtemp = logtab[A[i][3]] + logtab[G[3][j]]) >= 255 ? mtemp - 255 : mtemp] : 0);
  }
 }

 for (i = 0; i < 4; i++) {
  roundKey[i] =
   ((word32) (B[i][0]) ) ^
   ((word32) (B[i][1]) << 8) ^
   ((word32) (B[i][2]) << 16) ^
   ((word32) (B[i][3]) << 24);
 }

 mtemp = 0;
 memset (A, 0, sizeof (A));
 memset (B, 0, sizeof (B));
}


void squareGenerateRoundKeys (const squareBlock key,
 squareKeySchedule roundKeys_e, squareKeySchedule roundKeys_d)
{
 int t;


 { (roundKeys_e[0])[0] = (((word32 *)(key)))[0]; (roundKeys_e[0])[1] = (((word32 *)(key)))[1]; (roundKeys_e[0])[2] = (((word32 *)(key)))[2]; (roundKeys_e[0])[3] = (((word32 *)(key)))[3]; };
 for (t = 1; t < 8 +1; t++) {
  roundKeys_d[8 -t][0] = roundKeys_e[t][0] =
   roundKeys_e[t-1][0] ^ (((roundKeys_e[t-1][3]) >> (8)) | ((roundKeys_e[t-1][3]) << (32 - (8)))) ^ offset[t-1];
  roundKeys_d[8 -t][1] = roundKeys_e[t][1] =
   roundKeys_e[t-1][1] ^ roundKeys_e[t][0];
  roundKeys_d[8 -t][2] = roundKeys_e[t][2] =
   roundKeys_e[t-1][2] ^ roundKeys_e[t][1];
  roundKeys_d[8 -t][3] = roundKeys_e[t][3] =
   roundKeys_e[t-1][3] ^ roundKeys_e[t][2];

  squareTransform (roundKeys_e[t-1]);
 }
 { (roundKeys_d[8])[0] = (roundKeys_e[0])[0]; (roundKeys_d[8])[1] = (roundKeys_e[0])[1]; (roundKeys_d[8])[2] = (roundKeys_e[0])[2]; (roundKeys_d[8])[3] = (roundKeys_e[0])[3]; };
}


void squareExpandKey (const squareBlock key, squareKeySchedule roundKeys_e)
{
 int t;


 { (roundKeys_e[0])[0] = (((word32 *)(key)))[0]; (roundKeys_e[0])[1] = (((word32 *)(key)))[1]; (roundKeys_e[0])[2] = (((word32 *)(key)))[2]; (roundKeys_e[0])[3] = (((word32 *)(key)))[3]; };
 for (t = 1; t < 8 +1; t++) {
  roundKeys_e[t][0] = roundKeys_e[t-1][0] ^ (((roundKeys_e[t-1][3]) >> (8)) | ((roundKeys_e[t-1][3]) << (32 - (8)))) ^ offset[t-1];
  roundKeys_e[t][1] = roundKeys_e[t-1][1] ^ roundKeys_e[t][0];
  roundKeys_e[t][2] = roundKeys_e[t-1][2] ^ roundKeys_e[t][1];
  roundKeys_e[t][3] = roundKeys_e[t-1][3] ^ roundKeys_e[t][2];

  squareTransform (roundKeys_e[t-1]);
 }
}
# 527 "square.c"
void squareEncrypt (word32 text[4], squareKeySchedule roundKeys)
{
 word32 temp[4];


 text[0] ^= roundKeys[0][0];
 text[1] ^= roundKeys[0][1];
 text[2] ^= roundKeys[0][2];
 text[3] ^= roundKeys[0][3];


 { temp[0] = Te0[((byte) ((text[0]) ))] ^ Te1[((byte) ((text[1]) ))] ^ Te2[((byte) ((text[2]) ))] ^ Te3[((byte) ((text[3]) ))] ^ roundKeys[1][0]; temp[1] = Te0[((byte) ((text[0]) >> 8))] ^ Te1[((byte) ((text[1]) >> 8))] ^ Te2[((byte) ((text[2]) >> 8))] ^ Te3[((byte) ((text[3]) >> 8))] ^ roundKeys[1][1]; temp[2] = Te0[((byte) ((text[0]) >> 16))] ^ Te1[((byte) ((text[1]) >> 16))] ^ Te2[((byte) ((text[2]) >> 16))] ^ Te3[((byte) ((text[3]) >> 16))] ^ roundKeys[1][2]; temp[3] = Te0[((byte) ((text[0]) >> 24))] ^ Te1[((byte) ((text[1]) >> 24))] ^ Te2[((byte) ((text[2]) >> 24))] ^ Te3[((byte) ((text[3]) >> 24))] ^ roundKeys[1][3]; };
 { text[0] = Te0[((byte) ((temp[0]) ))] ^ Te1[((byte) ((temp[1]) ))] ^ Te2[((byte) ((temp[2]) ))] ^ Te3[((byte) ((temp[3]) ))] ^ roundKeys[2][0]; text[1] = Te0[((byte) ((temp[0]) >> 8))] ^ Te1[((byte) ((temp[1]) >> 8))] ^ Te2[((byte) ((temp[2]) >> 8))] ^ Te3[((byte) ((temp[3]) >> 8))] ^ roundKeys[2][1]; text[2] = Te0[((byte) ((temp[0]) >> 16))] ^ Te1[((byte) ((temp[1]) >> 16))] ^ Te2[((byte) ((temp[2]) >> 16))] ^ Te3[((byte) ((temp[3]) >> 16))] ^ roundKeys[2][2]; text[3] = Te0[((byte) ((temp[0]) >> 24))] ^ Te1[((byte) ((temp[1]) >> 24))] ^ Te2[((byte) ((temp[2]) >> 24))] ^ Te3[((byte) ((temp[3]) >> 24))] ^ roundKeys[2][3]; };
 { temp[0] = Te0[((byte) ((text[0]) ))] ^ Te1[((byte) ((text[1]) ))] ^ Te2[((byte) ((text[2]) ))] ^ Te3[((byte) ((text[3]) ))] ^ roundKeys[3][0]; temp[1] = Te0[((byte) ((text[0]) >> 8))] ^ Te1[((byte) ((text[1]) >> 8))] ^ Te2[((byte) ((text[2]) >> 8))] ^ Te3[((byte) ((text[3]) >> 8))] ^ roundKeys[3][1]; temp[2] = Te0[((byte) ((text[0]) >> 16))] ^ Te1[((byte) ((text[1]) >> 16))] ^ Te2[((byte) ((text[2]) >> 16))] ^ Te3[((byte) ((text[3]) >> 16))] ^ roundKeys[3][2]; temp[3] = Te0[((byte) ((text[0]) >> 24))] ^ Te1[((byte) ((text[1]) >> 24))] ^ Te2[((byte) ((text[2]) >> 24))] ^ Te3[((byte) ((text[3]) >> 24))] ^ roundKeys[3][3]; };
 { text[0] = Te0[((byte) ((temp[0]) ))] ^ Te1[((byte) ((temp[1]) ))] ^ Te2[((byte) ((temp[2]) ))] ^ Te3[((byte) ((temp[3]) ))] ^ roundKeys[4][0]; text[1] = Te0[((byte) ((temp[0]) >> 8))] ^ Te1[((byte) ((temp[1]) >> 8))] ^ Te2[((byte) ((temp[2]) >> 8))] ^ Te3[((byte) ((temp[3]) >> 8))] ^ roundKeys[4][1]; text[2] = Te0[((byte) ((temp[0]) >> 16))] ^ Te1[((byte) ((temp[1]) >> 16))] ^ Te2[((byte) ((temp[2]) >> 16))] ^ Te3[((byte) ((temp[3]) >> 16))] ^ roundKeys[4][2]; text[3] = Te0[((byte) ((temp[0]) >> 24))] ^ Te1[((byte) ((temp[1]) >> 24))] ^ Te2[((byte) ((temp[2]) >> 24))] ^ Te3[((byte) ((temp[3]) >> 24))] ^ roundKeys[4][3]; };
 { temp[0] = Te0[((byte) ((text[0]) ))] ^ Te1[((byte) ((text[1]) ))] ^ Te2[((byte) ((text[2]) ))] ^ Te3[((byte) ((text[3]) ))] ^ roundKeys[5][0]; temp[1] = Te0[((byte) ((text[0]) >> 8))] ^ Te1[((byte) ((text[1]) >> 8))] ^ Te2[((byte) ((text[2]) >> 8))] ^ Te3[((byte) ((text[3]) >> 8))] ^ roundKeys[5][1]; temp[2] = Te0[((byte) ((text[0]) >> 16))] ^ Te1[((byte) ((text[1]) >> 16))] ^ Te2[((byte) ((text[2]) >> 16))] ^ Te3[((byte) ((text[3]) >> 16))] ^ roundKeys[5][2]; temp[3] = Te0[((byte) ((text[0]) >> 24))] ^ Te1[((byte) ((text[1]) >> 24))] ^ Te2[((byte) ((text[2]) >> 24))] ^ Te3[((byte) ((text[3]) >> 24))] ^ roundKeys[5][3]; };
 { text[0] = Te0[((byte) ((temp[0]) ))] ^ Te1[((byte) ((temp[1]) ))] ^ Te2[((byte) ((temp[2]) ))] ^ Te3[((byte) ((temp[3]) ))] ^ roundKeys[6][0]; text[1] = Te0[((byte) ((temp[0]) >> 8))] ^ Te1[((byte) ((temp[1]) >> 8))] ^ Te2[((byte) ((temp[2]) >> 8))] ^ Te3[((byte) ((temp[3]) >> 8))] ^ roundKeys[6][1]; text[2] = Te0[((byte) ((temp[0]) >> 16))] ^ Te1[((byte) ((temp[1]) >> 16))] ^ Te2[((byte) ((temp[2]) >> 16))] ^ Te3[((byte) ((temp[3]) >> 16))] ^ roundKeys[6][2]; text[3] = Te0[((byte) ((temp[0]) >> 24))] ^ Te1[((byte) ((temp[1]) >> 24))] ^ Te2[((byte) ((temp[2]) >> 24))] ^ Te3[((byte) ((temp[3]) >> 24))] ^ roundKeys[6][3]; };
 { temp[0] = Te0[((byte) ((text[0]) ))] ^ Te1[((byte) ((text[1]) ))] ^ Te2[((byte) ((text[2]) ))] ^ Te3[((byte) ((text[3]) ))] ^ roundKeys[7][0]; temp[1] = Te0[((byte) ((text[0]) >> 8))] ^ Te1[((byte) ((text[1]) >> 8))] ^ Te2[((byte) ((text[2]) >> 8))] ^ Te3[((byte) ((text[3]) >> 8))] ^ roundKeys[7][1]; temp[2] = Te0[((byte) ((text[0]) >> 16))] ^ Te1[((byte) ((text[1]) >> 16))] ^ Te2[((byte) ((text[2]) >> 16))] ^ Te3[((byte) ((text[3]) >> 16))] ^ roundKeys[7][2]; temp[3] = Te0[((byte) ((text[0]) >> 24))] ^ Te1[((byte) ((text[1]) >> 24))] ^ Te2[((byte) ((text[2]) >> 24))] ^ Te3[((byte) ((text[3]) >> 24))] ^ roundKeys[7][3]; };


 { text[0] = ((word32) (Se[((byte) ((temp[0]) ))]) ) ^ ((word32) (Se[((byte) ((temp[1]) ))]) << 8) ^ ((word32) (Se[((byte) ((temp[2]) ))]) << 16) ^ ((word32) (Se[((byte) ((temp[3]) ))]) << 24) ^ roundKeys[8][0]; text[1] = ((word32) (Se[((byte) ((temp[0]) >> 8))]) ) ^ ((word32) (Se[((byte) ((temp[1]) >> 8))]) << 8) ^ ((word32) (Se[((byte) ((temp[2]) >> 8))]) << 16) ^ ((word32) (Se[((byte) ((temp[3]) >> 8))]) << 24) ^ roundKeys[8][1]; text[2] = ((word32) (Se[((byte) ((temp[0]) >> 16))]) ) ^ ((word32) (Se[((byte) ((temp[1]) >> 16))]) << 8) ^ ((word32) (Se[((byte) ((temp[2]) >> 16))]) << 16) ^ ((word32) (Se[((byte) ((temp[3]) >> 16))]) << 24) ^ roundKeys[8][2]; text[3] = ((word32) (Se[((byte) ((temp[0]) >> 24))]) ) ^ ((word32) (Se[((byte) ((temp[1]) >> 24))]) << 8) ^ ((word32) (Se[((byte) ((temp[2]) >> 24))]) << 16) ^ ((word32) (Se[((byte) ((temp[3]) >> 24))]) << 24) ^ roundKeys[8][3]; };





}


void squareDecrypt (word32 text[4], squareKeySchedule roundKeys)
{
 word32 temp[4];


 text[0] ^= roundKeys[0][0];
 text[1] ^= roundKeys[0][1];
 text[2] ^= roundKeys[0][2];
 text[3] ^= roundKeys[0][3];


 { temp[0] = Td0[((byte) ((text[0]) ))] ^ Td1[((byte) ((text[1]) ))] ^ Td2[((byte) ((text[2]) ))] ^ Td3[((byte) ((text[3]) ))] ^ roundKeys[1][0]; temp[1] = Td0[((byte) ((text[0]) >> 8))] ^ Td1[((byte) ((text[1]) >> 8))] ^ Td2[((byte) ((text[2]) >> 8))] ^ Td3[((byte) ((text[3]) >> 8))] ^ roundKeys[1][1]; temp[2] = Td0[((byte) ((text[0]) >> 16))] ^ Td1[((byte) ((text[1]) >> 16))] ^ Td2[((byte) ((text[2]) >> 16))] ^ Td3[((byte) ((text[3]) >> 16))] ^ roundKeys[1][2]; temp[3] = Td0[((byte) ((text[0]) >> 24))] ^ Td1[((byte) ((text[1]) >> 24))] ^ Td2[((byte) ((text[2]) >> 24))] ^ Td3[((byte) ((text[3]) >> 24))] ^ roundKeys[1][3]; };
 { text[0] = Td0[((byte) ((temp[0]) ))] ^ Td1[((byte) ((temp[1]) ))] ^ Td2[((byte) ((temp[2]) ))] ^ Td3[((byte) ((temp[3]) ))] ^ roundKeys[2][0]; text[1] = Td0[((byte) ((temp[0]) >> 8))] ^ Td1[((byte) ((temp[1]) >> 8))] ^ Td2[((byte) ((temp[2]) >> 8))] ^ Td3[((byte) ((temp[3]) >> 8))] ^ roundKeys[2][1]; text[2] = Td0[((byte) ((temp[0]) >> 16))] ^ Td1[((byte) ((temp[1]) >> 16))] ^ Td2[((byte) ((temp[2]) >> 16))] ^ Td3[((byte) ((temp[3]) >> 16))] ^ roundKeys[2][2]; text[3] = Td0[((byte) ((temp[0]) >> 24))] ^ Td1[((byte) ((temp[1]) >> 24))] ^ Td2[((byte) ((temp[2]) >> 24))] ^ Td3[((byte) ((temp[3]) >> 24))] ^ roundKeys[2][3]; };
 { temp[0] = Td0[((byte) ((text[0]) ))] ^ Td1[((byte) ((text[1]) ))] ^ Td2[((byte) ((text[2]) ))] ^ Td3[((byte) ((text[3]) ))] ^ roundKeys[3][0]; temp[1] = Td0[((byte) ((text[0]) >> 8))] ^ Td1[((byte) ((text[1]) >> 8))] ^ Td2[((byte) ((text[2]) >> 8))] ^ Td3[((byte) ((text[3]) >> 8))] ^ roundKeys[3][1]; temp[2] = Td0[((byte) ((text[0]) >> 16))] ^ Td1[((byte) ((text[1]) >> 16))] ^ Td2[((byte) ((text[2]) >> 16))] ^ Td3[((byte) ((text[3]) >> 16))] ^ roundKeys[3][2]; temp[3] = Td0[((byte) ((text[0]) >> 24))] ^ Td1[((byte) ((text[1]) >> 24))] ^ Td2[((byte) ((text[2]) >> 24))] ^ Td3[((byte) ((text[3]) >> 24))] ^ roundKeys[3][3]; };
 { text[0] = Td0[((byte) ((temp[0]) ))] ^ Td1[((byte) ((temp[1]) ))] ^ Td2[((byte) ((temp[2]) ))] ^ Td3[((byte) ((temp[3]) ))] ^ roundKeys[4][0]; text[1] = Td0[((byte) ((temp[0]) >> 8))] ^ Td1[((byte) ((temp[1]) >> 8))] ^ Td2[((byte) ((temp[2]) >> 8))] ^ Td3[((byte) ((temp[3]) >> 8))] ^ roundKeys[4][1]; text[2] = Td0[((byte) ((temp[0]) >> 16))] ^ Td1[((byte) ((temp[1]) >> 16))] ^ Td2[((byte) ((temp[2]) >> 16))] ^ Td3[((byte) ((temp[3]) >> 16))] ^ roundKeys[4][2]; text[3] = Td0[((byte) ((temp[0]) >> 24))] ^ Td1[((byte) ((temp[1]) >> 24))] ^ Td2[((byte) ((temp[2]) >> 24))] ^ Td3[((byte) ((temp[3]) >> 24))] ^ roundKeys[4][3]; };
 { temp[0] = Td0[((byte) ((text[0]) ))] ^ Td1[((byte) ((text[1]) ))] ^ Td2[((byte) ((text[2]) ))] ^ Td3[((byte) ((text[3]) ))] ^ roundKeys[5][0]; temp[1] = Td0[((byte) ((text[0]) >> 8))] ^ Td1[((byte) ((text[1]) >> 8))] ^ Td2[((byte) ((text[2]) >> 8))] ^ Td3[((byte) ((text[3]) >> 8))] ^ roundKeys[5][1]; temp[2] = Td0[((byte) ((text[0]) >> 16))] ^ Td1[((byte) ((text[1]) >> 16))] ^ Td2[((byte) ((text[2]) >> 16))] ^ Td3[((byte) ((text[3]) >> 16))] ^ roundKeys[5][2]; temp[3] = Td0[((byte) ((text[0]) >> 24))] ^ Td1[((byte) ((text[1]) >> 24))] ^ Td2[((byte) ((text[2]) >> 24))] ^ Td3[((byte) ((text[3]) >> 24))] ^ roundKeys[5][3]; };
 { text[0] = Td0[((byte) ((temp[0]) ))] ^ Td1[((byte) ((temp[1]) ))] ^ Td2[((byte) ((temp[2]) ))] ^ Td3[((byte) ((temp[3]) ))] ^ roundKeys[6][0]; text[1] = Td0[((byte) ((temp[0]) >> 8))] ^ Td1[((byte) ((temp[1]) >> 8))] ^ Td2[((byte) ((temp[2]) >> 8))] ^ Td3[((byte) ((temp[3]) >> 8))] ^ roundKeys[6][1]; text[2] = Td0[((byte) ((temp[0]) >> 16))] ^ Td1[((byte) ((temp[1]) >> 16))] ^ Td2[((byte) ((temp[2]) >> 16))] ^ Td3[((byte) ((temp[3]) >> 16))] ^ roundKeys[6][2]; text[3] = Td0[((byte) ((temp[0]) >> 24))] ^ Td1[((byte) ((temp[1]) >> 24))] ^ Td2[((byte) ((temp[2]) >> 24))] ^ Td3[((byte) ((temp[3]) >> 24))] ^ roundKeys[6][3]; };
 { temp[0] = Td0[((byte) ((text[0]) ))] ^ Td1[((byte) ((text[1]) ))] ^ Td2[((byte) ((text[2]) ))] ^ Td3[((byte) ((text[3]) ))] ^ roundKeys[7][0]; temp[1] = Td0[((byte) ((text[0]) >> 8))] ^ Td1[((byte) ((text[1]) >> 8))] ^ Td2[((byte) ((text[2]) >> 8))] ^ Td3[((byte) ((text[3]) >> 8))] ^ roundKeys[7][1]; temp[2] = Td0[((byte) ((text[0]) >> 16))] ^ Td1[((byte) ((text[1]) >> 16))] ^ Td2[((byte) ((text[2]) >> 16))] ^ Td3[((byte) ((text[3]) >> 16))] ^ roundKeys[7][2]; temp[3] = Td0[((byte) ((text[0]) >> 24))] ^ Td1[((byte) ((text[1]) >> 24))] ^ Td2[((byte) ((text[2]) >> 24))] ^ Td3[((byte) ((text[3]) >> 24))] ^ roundKeys[7][3]; };


 { text[0] = ((word32) (Sd[((byte) ((temp[0]) ))]) ) ^ ((word32) (Sd[((byte) ((temp[1]) ))]) << 8) ^ ((word32) (Sd[((byte) ((temp[2]) ))]) << 16) ^ ((word32) (Sd[((byte) ((temp[3]) ))]) << 24) ^ roundKeys[8][0]; text[1] = ((word32) (Sd[((byte) ((temp[0]) >> 8))]) ) ^ ((word32) (Sd[((byte) ((temp[1]) >> 8))]) << 8) ^ ((word32) (Sd[((byte) ((temp[2]) >> 8))]) << 16) ^ ((word32) (Sd[((byte) ((temp[3]) >> 8))]) << 24) ^ roundKeys[8][1]; text[2] = ((word32) (Sd[((byte) ((temp[0]) >> 16))]) ) ^ ((word32) (Sd[((byte) ((temp[1]) >> 16))]) << 8) ^ ((word32) (Sd[((byte) ((temp[2]) >> 16))]) << 16) ^ ((word32) (Sd[((byte) ((temp[3]) >> 16))]) << 24) ^ roundKeys[8][2]; text[3] = ((word32) (Sd[((byte) ((temp[0]) >> 24))]) ) ^ ((word32) (Sd[((byte) ((temp[1]) >> 24))]) << 8) ^ ((word32) (Sd[((byte) ((temp[2]) >> 24))]) << 16) ^ ((word32) (Sd[((byte) ((temp[3]) >> 24))]) << 24) ^ roundKeys[8][3]; };





}
