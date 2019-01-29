# 1 "getpic.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "getpic.c"
# 30 "getpic.c"
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 1 3 4
# 28 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 3 4
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
# 29 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/stdio.h" 2 3 4





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

# 31 "getpic.c" 2

# 1 "config.h" 1
# 33 "getpic.c" 2
# 1 "global.h" 1
# 30 "global.h"
# 1 "mpeg2dec.h" 1
# 31 "global.h" 2
# 45 "global.h"
void Substitute_Frame_Buffer (int bitstream_framenum, int sequence_framenum)
                         ;


void Initialize_Buffer (void);
void Fill_Buffer (void);
unsigned int Show_Bits (int n);
unsigned int Get_Bits1 (void);
void Flush_Buffer (int n);
unsigned int Get_Bits (int n);
int Get_Byte (void);
int Get_Word (void);


void Next_Packet (void);
int Get_Long (void);
void Flush_Buffer32 (void);
unsigned int Get_Bits32 (void);



void Decode_MPEG1_Intra_Block (int comp, int dc_dct_pred[]);
void Decode_MPEG1_Non_Intra_Block (int comp);
void Decode_MPEG2_Intra_Block (int comp, int dc_dct_pred[]);
void Decode_MPEG2_Non_Intra_Block (int comp);


int Get_Hdr (void);
void next_start_code (void);
int slice_header (void);
void marker_bit (char *text);


void Decode_Picture (int bitstream_framenum, int sequence_framenum)
                         ;
void Output_Last_Frame_of_Sequence (int framenum);


int Get_macroblock_type (void);
int Get_motion_code (void);
int Get_dmvector (void);
int Get_coded_block_pattern (void);
int Get_macroblock_address_increment (void);
int Get_Luma_DC_dct_diff (void);
int Get_Chroma_DC_dct_diff (void);


void Fast_IDCT (short *block);
void Initialize_Fast_IDCT (void);


void Initialize_Reference_IDCT (void);
void Reference_IDCT (short *block);


void motion_vectors (int PMV[2][2][2], int dmvector[2], int motion_vertical_field_select[2][2], int s, int motion_vector_count, int mv_format, int h_r_size, int v_r_size, int dmv, int mvscale)

                                                                   ;
void motion_vector (int *PMV, int *dmvector, int h_r_size, int v_r_size, int dmv, int mvscale, int full_pel_vector)
                                                                         ;
void Dual_Prime_Arithmetic (int DMV[][2], int *dmvector, int mvx, int mvy);


void Error (char *text);
void Warning (char *text);
void Print_Bits (int code, int bits, int len);


void form_predictions (int bx, int by, int macroblock_type, int motion_type, int PMV[2][2][2], int motion_vertical_field_select[2][2], int dmvector[2], int stwtype)

                                ;


void Spatial_Prediction (void);


void Write_Frame (unsigned char *src[], int frame);
# 134 "global.h"
extern char Version[]



;

extern char Author[]



;



extern unsigned char scan[2][64]
# 166 "global.h"
;


extern unsigned char default_intra_quantizer_matrix[64]
# 183 "global.h"
;


extern unsigned char Non_Linear_quantizer_scale[32]
# 196 "global.h"
;
# 213 "global.h"
extern int Inverse_Table_6_9[8][4]
# 227 "global.h"
;
# 242 "global.h"
extern int Output_Type;
extern int hiQdither;


extern int Quiet_Flag;
extern int Trace_Flag;
extern int Fault_Flag;
extern int Verbose_Flag;
extern int Two_Streams;
extern int Spatial_Flag;
extern int Reference_IDCT_Flag;
extern int Frame_Store_Flag;
extern int System_Stream_Flag;
extern int Display_Progressive_Flag;
extern int Ersatz_Flag;
extern int Big_Picture_Flag;
extern int Verify_Flag;
extern int Stats_Flag;
extern int User_Data_Flag;
extern int Main_Bitstream_Flag;



extern char *Output_Picture_Filename;
extern char *Substitute_Picture_Filename;
extern char *Main_Bitstream_Filename;
extern char *Enhancement_Layer_Bitstream_Filename;



extern char Error_Text[256];
extern unsigned char *Clip;


extern unsigned char *backward_reference_frame[3];
extern unsigned char *forward_reference_frame[3];

extern unsigned char *auxframe[3];
extern unsigned char *current_frame[3];
extern unsigned char *substitute_frame[3];



extern unsigned char *llframe0[3];
extern unsigned char *llframe1[3];

extern short *lltmp;
extern char *Lower_Layer_Picture_Filename;





extern int Coded_Picture_Width;
extern int Coded_Picture_Height;
extern int Chroma_Width;
extern int Chroma_Height;
extern int block_count;
extern int Second_Field;
extern int profile, level;


extern int horizontal_size;
extern int vertical_size;
extern int mb_width;
extern int mb_height;
extern double bit_rate;
extern double frame_rate;






extern int aspect_ratio_information;
extern int frame_rate_code;
extern int bit_rate_value;
extern int vbv_buffer_size;
extern int constrained_parameters_flag;


extern int profile_and_level_indication;
extern int progressive_sequence;
extern int chroma_format;
extern int low_delay;
extern int frame_rate_extension_n;
extern int frame_rate_extension_d;


extern int video_format;
extern int color_description;
extern int color_primaries;
extern int transfer_characteristics;
extern int matrix_coefficients;
extern int display_horizontal_size;
extern int display_vertical_size;


extern int temporal_reference;
extern int picture_coding_type;
extern int vbv_delay;
extern int full_pel_forward_vector;
extern int forward_f_code;
extern int full_pel_backward_vector;
extern int backward_f_code;



extern int f_code[2][2];
extern int intra_dc_precision;
extern int picture_structure;
extern int top_field_first;
extern int frame_pred_frame_dct;
extern int concealment_motion_vectors;

extern int intra_vlc_format;

extern int repeat_first_field;

extern int chroma_420_type;
extern int progressive_frame;
extern int composite_display_flag;
extern int v_axis;
extern int field_sequence;
extern int sub_carrier;
extern int burst_amplitude;
extern int sub_carrier_phase;




extern int frame_center_horizontal_offset[3];
extern int frame_center_vertical_offset[3];




extern int layer_id;
extern int lower_layer_prediction_horizontal_size;
extern int lower_layer_prediction_vertical_size;
extern int horizontal_subsampling_factor_m;
extern int horizontal_subsampling_factor_n;
extern int vertical_subsampling_factor_m;
extern int vertical_subsampling_factor_n;



extern int lower_layer_temporal_reference;
extern int lower_layer_horizontal_offset;
extern int lower_layer_vertical_offset;
extern int spatial_temporal_weight_code_table_index;
extern int lower_layer_progressive_frame;
extern int lower_layer_deinterlaced_field_select;







extern int copyright_flag;
extern int copyright_identifier;
extern int original_or_copy;
extern int copyright_number_1;
extern int copyright_number_2;
extern int copyright_number_3;


extern int drop_flag;
extern int hour;
extern int minute;
extern int sec;
extern int frame;
extern int closed_gop;
extern int broken_link;




extern struct layer_data {

  int Infile;
  unsigned char Rdbfr[2048];
  unsigned char *Rdptr;
  unsigned char Inbfr[16];

  unsigned int Bfr;
  unsigned char *Rdmax;
  int Incnt;
  int Bitcnt;

  int intra_quantizer_matrix[64];
  int non_intra_quantizer_matrix[64];
  int chroma_intra_quantizer_matrix[64];
  int chroma_non_intra_quantizer_matrix[64];

  int load_intra_quantizer_matrix;
  int load_non_intra_quantizer_matrix;
  int load_chroma_intra_quantizer_matrix;
  int load_chroma_non_intra_quantizer_matrix;

  int MPEG2_Flag;

  int scalable_mode;

  int q_scale_type;
  int alternate_scan;

  int pict_scal;

  int priority_breakpoint;
  int quantizer_scale;
  int intra_slice;
  short block[12][64];
} base, enhan, *ld;
# 477 "global.h"
extern int Decode_Layer;
# 486 "global.h"
extern int global_MBA;
extern int global_pic;
extern int True_Framenum;
# 34 "getpic.c" 2


static void picture_data (int framenum);
static void macroblock_modes (int *pmacroblock_type, int *pstwtype, int *pstwclass, int *pmotion_type, int *pmotion_vector_count, int *pmv_format, int *pdmv, int *pmvscale, int *pdct_type)

                                 ;
static void Clear_Block (int comp);
static void Sum_Block (int comp);
static void Saturate (short *bp);
static void Add_Block (int comp, int bx, int by, int dct_type, int addflag)
                             ;
static void Update_Picture_Buffers (void);
static void frame_reorder (int bitstream_framenum, int sequence_framenum)
                         ;
static void Decode_SNR_Macroblock (int *SNRMBA, int *SNRMBAinc, int MBA, int MBAmax, int *dct_type)
                                      ;

static void motion_compensation (int MBA, int macroblock_type, int motion_type, int PMV[2][2][2], int motion_vertical_field_select[2][2], int dmvector[2], int stwtype, int dct_type)

                                             ;

static void skipped_macroblock (int dc_dct_pred[3], int PMV[2][2][2], int *motion_type, int motion_vertical_field_select[2][2], int *stwtype, int *macroblock_type)

                                      ;

static int slice (int framenum, int MBAmax);

static int start_of_slice (int MBAmax, int *MBA, int *MBAinc, int dc_dct_pred[3], int PMV[2][2][2])
                                                     ;

static int decode_macroblock (int *macroblock_type, int *stwtype, int *stwclass, int *motion_type, int *dct_type, int PMV[2][2][2], int dc_dct_pred[3], int motion_vertical_field_select[2][2], int dmvector[2])


                                                           ;



void Decode_Picture(bitstream_framenum, sequence_framenum)
int bitstream_framenum, sequence_framenum;
{

  if (picture_structure==3 && Second_Field)
  {

    printf("odd number of field pictures\n");
    Second_Field = 0;
  }


  Update_Picture_Buffers();
# 92 "getpic.c"
  if(Ersatz_Flag)
    Substitute_Frame_Buffer(bitstream_framenum, sequence_framenum);





  if (base.pict_scal && !Second_Field)
  {
    Spatial_Prediction();
  }


  picture_data(bitstream_framenum);



  frame_reorder(bitstream_framenum, sequence_framenum);

  if (picture_structure!=3)
    Second_Field = !Second_Field;
}




static void picture_data(framenum)
int framenum;
{
  int MBAmax;
  int ret;


  MBAmax = mb_width*mb_height;

  if (picture_structure!=3)
    MBAmax>>=1;

  for(;;)
  {
    if((ret=slice(framenum, MBAmax))<0)
      return;
  }

}





static int slice(framenum, MBAmax)
int framenum, MBAmax;
{
  int MBA;
  int MBAinc, macroblock_type, motion_type, dct_type;
  int dc_dct_pred[3];
  int PMV[2][2][2], motion_vertical_field_select[2][2];
  int dmvector[2];
  int stwtype, stwclass;
  int SNRMBA, SNRMBAinc;
  int ret;

  MBA = 0;
  MBAinc = 0;

  if((ret=start_of_slice(MBAmax, &MBA, &MBAinc, dc_dct_pred, PMV))!=1)
    return(ret);

  if (Two_Streams && enhan.scalable_mode==3)
  {
    SNRMBA=0;
    SNRMBAinc=0;
  }

  Fault_Flag=0;

  for (;;)
  {


    if (MBA>=MBAmax)
      return(-1);
# 189 "getpic.c"
    ld = &base;

    if (MBAinc==0)
    {
      if (base.scalable_mode==1 && base.priority_breakpoint==1)
          ld = &enhan;

      if (!Show_Bits(23) || Fault_Flag)
      {
resync:
        Fault_Flag = 0;
        return(0);
      }
      else
      {
        if (base.scalable_mode==1 && base.priority_breakpoint==1)
          ld = &enhan;


        MBAinc = Get_macroblock_address_increment();

        if (Fault_Flag) goto resync;
      }
    }

    if (MBA>=MBAmax)
    {

      if (!Quiet_Flag)
        printf("Too many macroblocks in picture\n");
      return(-1);
    }

    if (MBAinc==1)
    {
      ret = decode_macroblock(&macroblock_type, &stwtype, &stwclass,
              &motion_type, &dct_type, PMV, dc_dct_pred,
              motion_vertical_field_select, dmvector);

      if(ret==-1)
        return(-1);

      if(ret==0)
        goto resync;

    }
    else
    {

      skipped_macroblock(dc_dct_pred, PMV, &motion_type,
        motion_vertical_field_select, &stwtype, &macroblock_type);
    }




    if (Two_Streams && enhan.scalable_mode==3)
      Decode_SNR_Macroblock(&SNRMBA, &SNRMBAinc, MBA, MBAmax, &dct_type);


    motion_compensation(MBA, macroblock_type, motion_type, PMV,
      motion_vertical_field_select, dmvector, stwtype, dct_type);



    MBA++;
    MBAinc--;


    if (Two_Streams && enhan.scalable_mode==3)
    {
      SNRMBA++;
      SNRMBAinc--;
    }

    if (MBA>=MBAmax)
      return(-1);
  }
}



static void macroblock_modes(pmacroblock_type,pstwtype,pstwclass,
  pmotion_type,pmotion_vector_count,pmv_format,pdmv,pmvscale,pdct_type)
  int *pmacroblock_type, *pstwtype, *pstwclass;
  int *pmotion_type, *pmotion_vector_count, *pmv_format, *pdmv, *pmvscale;
  int *pdct_type;
{
  int macroblock_type;
  int stwtype, stwcode, stwclass;
  int motion_type = 0;
  int motion_vector_count, mv_format, dmv, mvscale;
  int dct_type;
  static unsigned char stwc_table[3][4]
    = { {6,3,7,4}, {2,1,5,4}, {2,5,7,4} };
  static unsigned char stwclass_table[9]
    = {0, 1, 2, 1, 1, 2, 3, 3, 4};


  macroblock_type = Get_macroblock_type();

  if (Fault_Flag) return;


  if (macroblock_type & 32)
  {
    if (spatial_temporal_weight_code_table_index==0)
      stwtype = 4;
    else
    {
      stwcode = Get_Bits(2);
# 308 "getpic.c"
      stwtype = stwc_table[spatial_temporal_weight_code_table_index-1][stwcode];
    }
  }
  else
    stwtype = (macroblock_type & 64) ? 8 : 0;


  stwclass = stwclass_table[stwtype];


  if (macroblock_type & (8|4))
  {
    if (picture_structure==3)
    {
      motion_type = frame_pred_frame_dct ? 2 : Get_Bits(2);
# 333 "getpic.c"
    }
    else
    {
      motion_type = Get_Bits(2);
# 347 "getpic.c"
    }
  }
  else if ((macroblock_type & 1) && concealment_motion_vectors)
  {

    motion_type = (picture_structure==3) ? 2 : 1;
  }
# 363 "getpic.c"
  if (picture_structure==3)
  {
    motion_vector_count = (motion_type==1 && stwclass<2) ? 2 : 1;
    mv_format = (motion_type==2) ? 1 : 0;
  }
  else
  {
    motion_vector_count = (motion_type==2) ? 2 : 1;
    mv_format = 0;
  }

  dmv = (motion_type==3);
# 385 "getpic.c"
  mvscale = ((mv_format==0) && (picture_structure==3));


  dct_type = (picture_structure==3)
             && (!frame_pred_frame_dct)
             && (macroblock_type & (2|1))
             ? Get_Bits(1)
             : 0;
# 402 "getpic.c"
  *pmacroblock_type = macroblock_type;
  *pstwtype = stwtype;
  *pstwclass = stwclass;
  *pmotion_type = motion_type;
  *pmotion_vector_count = motion_vector_count;
  *pmv_format = mv_format;
  *pdmv = dmv;
  *pmvscale = mvscale;
  *pdct_type = dct_type;
}
# 421 "getpic.c"
static void Add_Block(comp,bx,by,dct_type,addflag)
int comp,bx,by,dct_type,addflag;
{
  int cc,i, j, iincr;
  unsigned char *rfp;
  short *bp;




  cc = (comp<4) ? 0 : (comp&1)+1;

  if (cc==0)
  {


    if (picture_structure==3)
      if (dct_type)
      {

        rfp = current_frame[0]
              + Coded_Picture_Width*(by+((comp&2)>>1)) + bx + ((comp&1)<<3);
        iincr = (Coded_Picture_Width<<1) - 8;
      }
      else
      {

        rfp = current_frame[0]
              + Coded_Picture_Width*(by+((comp&2)<<2)) + bx + ((comp&1)<<3);
        iincr = Coded_Picture_Width - 8;
      }
    else
    {

      rfp = current_frame[0]
            + (Coded_Picture_Width<<1)*(by+((comp&2)<<2)) + bx + ((comp&1)<<3);
      iincr = (Coded_Picture_Width<<1) - 8;
    }
  }
  else
  {



    if (chroma_format!=3)
      bx >>= 1;
    if (chroma_format==1)
      by >>= 1;
    if (picture_structure==3)
    {
      if (dct_type && (chroma_format!=1))
      {

        rfp = current_frame[cc]
              + Chroma_Width*(by+((comp&2)>>1)) + bx + (comp&8);
        iincr = (Chroma_Width<<1) - 8;
      }
      else
      {

        rfp = current_frame[cc]
              + Chroma_Width*(by+((comp&2)<<2)) + bx + (comp&8);
        iincr = Chroma_Width - 8;
      }
    }
    else
    {

      rfp = current_frame[cc]
            + (Chroma_Width<<1)*(by+((comp&2)<<2)) + bx + (comp&8);
      iincr = (Chroma_Width<<1) - 8;
    }
  }

  bp = ld->block[comp];

  if (addflag)
  {
    for (i=0; i<8; i++)
    {
      for (j=0; j<8; j++)
      {
        *rfp = Clip[*bp++ + *rfp];
        rfp++;
      }

      rfp+= iincr;
    }
  }
  else
  {
    for (i=0; i<8; i++)
    {
      for (j=0; j<8; j++)
        *rfp++ = Clip[*bp++ + 128];

      rfp+= iincr;
    }
  }
}



static void Decode_SNR_Macroblock(SNRMBA, SNRMBAinc, MBA, MBAmax, dct_type)
  int *SNRMBA, *SNRMBAinc;
  int MBA, MBAmax;
  int *dct_type;
{
  int SNRmacroblock_type, SNRcoded_block_pattern, SNRdct_type, dummy;
  int slice_vert_pos_ext, quantizer_scale_code, comp, code;

  ld = &enhan;

  if (*SNRMBAinc==0)
  {
    if (!Show_Bits(23))
    {
      next_start_code();
      code = Show_Bits(32);

      if (code<0x101 || code>0x1AF)
      {

        if (!Quiet_Flag)
          printf("SNR: Premature end of picture\n");
        return;
      }

      Flush_Buffer32();


      slice_vert_pos_ext = slice_header();


      *SNRMBAinc = Get_macroblock_address_increment();


      *SNRMBA =
        ((slice_vert_pos_ext<<7) + (code&255) - 1)*mb_width + *SNRMBAinc - 1;

      *SNRMBAinc = 1;
    }
    else
    {
      if (*SNRMBA>=MBAmax)
      {
        if (!Quiet_Flag)
          printf("Too many macroblocks in picture\n");
        return;
      }


      *SNRMBAinc = Get_macroblock_address_increment();
    }
  }

  if (*SNRMBA!=MBA)
  {

    if (!Quiet_Flag)
      printf("Cant't synchronize streams\n");
    return;
  }

  if (*SNRMBAinc==1)
  {
    macroblock_modes(&SNRmacroblock_type, &dummy, &dummy,
      &dummy, &dummy, &dummy, &dummy, &dummy,
      &SNRdct_type);

    if (SNRmacroblock_type & 2)
      *dct_type = SNRdct_type;

    if (SNRmacroblock_type & 16)
    {
      quantizer_scale_code = Get_Bits(5);
      ld->quantizer_scale =
        ld->q_scale_type ? Non_Linear_quantizer_scale[quantizer_scale_code] : quantizer_scale_code<<1;
    }


    if (SNRmacroblock_type & 2)
    {
      SNRcoded_block_pattern = Get_coded_block_pattern();

      if (chroma_format==2)
        SNRcoded_block_pattern = (SNRcoded_block_pattern<<2) | Get_Bits(2);
      else if (chroma_format==3)
        SNRcoded_block_pattern = (SNRcoded_block_pattern<<6) | Get_Bits(6);
    }
    else
      SNRcoded_block_pattern = 0;


    for (comp=0; comp<block_count; comp++)
    {
      Clear_Block(comp);

      if (SNRcoded_block_pattern & (1<<(block_count-1-comp)))
        Decode_MPEG2_Non_Intra_Block(comp);
    }
  }
  else
  {
    for (comp=0; comp<block_count; comp++)
      Clear_Block(comp);
  }

  ld = &base;
}




static void Clear_Block(comp)
int comp;
{
  short *Block_Ptr;
  int i;

  Block_Ptr = ld->block[comp];

  for (i=0; i<64; i++)
    *Block_Ptr++ = 0;
}




static void Sum_Block(comp)
int comp;
{
  short *Block_Ptr1, *Block_Ptr2;
  int i;

  Block_Ptr1 = base.block[comp];
  Block_Ptr2 = enhan.block[comp];

  for (i=0; i<64; i++)
    *Block_Ptr1++ += *Block_Ptr2++;
}




static void Saturate(Block_Ptr)
short *Block_Ptr;
{
  int i, sum, val;

  sum = 0;


  for (i=0; i<64; i++)
  {
    val = Block_Ptr[i];

    if (val>2047)
      val = 2047;
    else if (val<-2048)
      val = -2048;

    Block_Ptr[i] = val;
    sum+= val;
  }


  if ((sum&1)==0)
    Block_Ptr[63]^= 1;

}




static void Update_Picture_Buffers()
{
  int cc;
  unsigned char *tmp;

  for (cc=0; cc<3; cc++)
  {

    if (picture_coding_type==3)
    {
      current_frame[cc] = auxframe[cc];
    }
    else
    {

      if (!Second_Field)
      {
        tmp = forward_reference_frame[cc];





        forward_reference_frame[cc] = backward_reference_frame[cc];


        backward_reference_frame[cc] = tmp;
      }




      current_frame[cc] = backward_reference_frame[cc];
    }





    if (picture_structure==2)
      current_frame[cc]+= (cc==0) ? Coded_Picture_Width : Chroma_Width;
  }
}




void Output_Last_Frame_of_Sequence(Framenum)
int Framenum;
{
  if (Second_Field)
    printf("last frame incomplete, not stored\n");
  else
    Write_Frame(backward_reference_frame,Framenum-1);
}



static void frame_reorder(Bitstream_Framenum, Sequence_Framenum)
int Bitstream_Framenum, Sequence_Framenum;
{

  static int Oldref_progressive_frame, Newref_progressive_frame;

  if (Sequence_Framenum!=0)
  {
    if (picture_structure==3 || Second_Field)
    {
      if (picture_coding_type==3)
        Write_Frame(auxframe,Bitstream_Framenum-1);
      else
      {
        Newref_progressive_frame = progressive_frame;
        progressive_frame = Oldref_progressive_frame;

        Write_Frame(forward_reference_frame,Bitstream_Framenum-1);

        Oldref_progressive_frame = progressive_frame = Newref_progressive_frame;
      }
    }







  }
  else
    Oldref_progressive_frame = progressive_frame;

}



static void motion_compensation(MBA, macroblock_type, motion_type, PMV,
  motion_vertical_field_select, dmvector, stwtype, dct_type)
int MBA;
int macroblock_type;
int motion_type;
int PMV[2][2][2];
int motion_vertical_field_select[2][2];
int dmvector[2];
int stwtype;
int dct_type;
{
  int bx, by;
  int comp;



  bx = 16*(MBA%mb_width);
  by = 16*(MBA/mb_width);


  if (!(macroblock_type & 1))
    form_predictions(bx,by,macroblock_type,motion_type,PMV,
      motion_vertical_field_select,dmvector,stwtype);


  if (base.scalable_mode==1)
    ld = &base;


  for (comp=0; comp<block_count; comp++)
  {



    if (Two_Streams && enhan.scalable_mode==3)
      Sum_Block(comp);




    if ((Two_Streams && enhan.scalable_mode==3) || ld->MPEG2_Flag)
      Saturate(ld->block[comp]);


    if (Reference_IDCT_Flag)
      Reference_IDCT(ld->block[comp]);
    else
      Fast_IDCT(ld->block[comp]);


    Add_Block(comp,bx,by,dct_type,(macroblock_type & 1)==0);
  }

}




static void skipped_macroblock(dc_dct_pred, PMV, motion_type,
  motion_vertical_field_select, stwtype, macroblock_type)
int dc_dct_pred[3];
int PMV[2][2][2];
int *motion_type;
int motion_vertical_field_select[2][2];
int *stwtype;
int *macroblock_type;
{
  int comp;


  if (base.scalable_mode==1)
    ld = &base;

  for (comp=0; comp<block_count; comp++)
    Clear_Block(comp);



  dc_dct_pred[0]=dc_dct_pred[1]=dc_dct_pred[2]=0;



  if (picture_coding_type==2)
    PMV[0][0][0]=PMV[0][0][1]=PMV[1][0][0]=PMV[1][0][1]=0;


  if (picture_structure==3)
    *motion_type = 2;
  else
  {
    *motion_type = 1;




    motion_vertical_field_select[0][0]=motion_vertical_field_select[0][1] =
      (picture_structure==2);
  }




  *stwtype = (picture_coding_type==1) ? 8 : 0;


  *macroblock_type&= ~1;

}





static int start_of_slice(MBAmax, MBA, MBAinc,
  dc_dct_pred, PMV)
int MBAmax;
int *MBA;
int *MBAinc;
int dc_dct_pred[3];
int PMV[2][2][2];
{
  unsigned int code;
  int slice_vert_pos_ext;

  ld = &base;

  Fault_Flag = 0;

  next_start_code();
  code = Show_Bits(32);

  if (code<0x101 || code>0x1AF)
  {

    if (!Quiet_Flag)
      printf("start_of_slice(): Premature end of picture\n");

    return(-1);
  }

  Flush_Buffer32();


  slice_vert_pos_ext = slice_header();



  if (base.scalable_mode==1)
  {
    ld = &enhan;
    next_start_code();
    code = Show_Bits(32);

    if (code<0x101 || code>0x1AF)
    {

      if (!Quiet_Flag)
        printf("DP: Premature end of picture\n");
      return(-1);
    }

    Flush_Buffer32();


    slice_vert_pos_ext = slice_header();

    if (base.priority_breakpoint!=1)
      ld = &base;
  }


  *MBAinc = Get_macroblock_address_increment();

  if (Fault_Flag)
  {
    printf("start_of_slice(): MBAinc unsuccessful\n");
    return(0);
  }





  *MBA = ((slice_vert_pos_ext<<7) + (code&255) - 1)*mb_width + *MBAinc - 1;
  *MBAinc = 1;




  dc_dct_pred[0]=dc_dct_pred[1]=dc_dct_pred[2]=0;


  PMV[0][0][0]=PMV[0][0][1]=PMV[1][0][0]=PMV[1][0][1]=0;
  PMV[0][1][0]=PMV[0][1][1]=PMV[1][1][0]=PMV[1][1][1]=0;


  return(1);
}



static int decode_macroblock(macroblock_type, stwtype, stwclass,
  motion_type, dct_type, PMV, dc_dct_pred,
  motion_vertical_field_select, dmvector)
int *macroblock_type;
int *stwtype;
int *stwclass;
int *motion_type;
int *dct_type;
int PMV[2][2][2];
int dc_dct_pred[3];
int motion_vertical_field_select[2][2];
int dmvector[2];
{

  int quantizer_scale_code;
  int comp;

  int motion_vector_count;
  int mv_format;
  int dmv;
  int mvscale;
  int coded_block_pattern;


  if (base.scalable_mode==1)
  {
    if (base.priority_breakpoint<=2)
      ld = &enhan;
    else
      ld = &base;
  }


  macroblock_modes(macroblock_type, stwtype, stwclass,
    motion_type, &motion_vector_count, &mv_format, &dmv, &mvscale,
    dct_type);

  if (Fault_Flag) return(0);

  if (*macroblock_type & 16)
  {
    quantizer_scale_code = Get_Bits(5);
# 1045 "getpic.c"
    if (ld->MPEG2_Flag)
      ld->quantizer_scale =
      ld->q_scale_type ? Non_Linear_quantizer_scale[quantizer_scale_code]
       : (quantizer_scale_code << 1);
    else
      ld->quantizer_scale = quantizer_scale_code;


    if (base.scalable_mode==1)

      base.quantizer_scale = ld->quantizer_scale;
  }







  if ((*macroblock_type & 8)
    || ((*macroblock_type & 1)
    && concealment_motion_vectors))
  {
    if (ld->MPEG2_Flag)
      motion_vectors(PMV,dmvector,motion_vertical_field_select,
        0,motion_vector_count,mv_format,f_code[0][0]-1,f_code[0][1]-1,
        dmv,mvscale);
    else
      motion_vector(PMV[0][0],dmvector,
      forward_f_code-1,forward_f_code-1,0,0,full_pel_forward_vector);
  }

  if (Fault_Flag) return(0);


  if (*macroblock_type & 4)
  {
    if (ld->MPEG2_Flag)
      motion_vectors(PMV,dmvector,motion_vertical_field_select,
        1,motion_vector_count,mv_format,f_code[1][0]-1,f_code[1][1]-1,0,
        mvscale);
    else
      motion_vector(PMV[0][1],dmvector,
        backward_f_code-1,backward_f_code-1,0,0,full_pel_backward_vector);
  }

  if (Fault_Flag) return(0);

  if ((*macroblock_type & 1) && concealment_motion_vectors)
    Flush_Buffer(1);

  if (base.scalable_mode==1 && base.priority_breakpoint==3)
    ld = &enhan;



  if (*macroblock_type & 2)
  {
    coded_block_pattern = Get_coded_block_pattern();

    if (chroma_format==2)
    {

      coded_block_pattern = (coded_block_pattern<<2) | Get_Bits(2);
# 1118 "getpic.c"
     }
     else if (chroma_format==3)
     {

      coded_block_pattern = (coded_block_pattern<<6) | Get_Bits(6);
# 1132 "getpic.c"
    }
  }
  else
    coded_block_pattern = (*macroblock_type & 1) ?
      (1<<block_count)-1 : 0;

  if (Fault_Flag) return(0);


  for (comp=0; comp<block_count; comp++)
  {

    if (base.scalable_mode==1)
    ld = &base;

    Clear_Block(comp);

    if (coded_block_pattern & (1<<(block_count-1-comp)))
    {
      if (*macroblock_type & 1)
      {
        if (ld->MPEG2_Flag)
          Decode_MPEG2_Intra_Block(comp,dc_dct_pred);
        else
          Decode_MPEG1_Intra_Block(comp,dc_dct_pred);
      }
      else
      {
        if (ld->MPEG2_Flag)
          Decode_MPEG2_Non_Intra_Block(comp);
        else
          Decode_MPEG1_Non_Intra_Block(comp);
      }

      if (Fault_Flag) return(0);
    }
  }

  if(picture_coding_type==4)
  {


    marker_bit("D picture end_of_macroblock bit");
  }



  if (!(*macroblock_type & 1))
    dc_dct_pred[0]=dc_dct_pred[1]=dc_dct_pred[2]=0;


  if ((*macroblock_type & 1) && !concealment_motion_vectors)
  {


    PMV[0][0][0]=PMV[0][0][1]=PMV[1][0][0]=PMV[1][0][1]=0;
    PMV[0][1][0]=PMV[0][1][1]=PMV[1][1][0]=PMV[1][1][1]=0;
  }



  if ((picture_coding_type==2)
    && !(*macroblock_type & (8|1)))
  {


    PMV[0][0][0]=PMV[0][0][1]=PMV[1][0][0]=PMV[1][0][1]=0;



    if (picture_structure==3)
      *motion_type = 2;
    else
    {
      *motion_type = 1;

      motion_vertical_field_select[0][0] = (picture_structure==2);
    }
  }

  if (*stwclass==4)
  {


    PMV[0][0][0]=PMV[0][0][1]=PMV[1][0][0]=PMV[1][0][1]=0;
    PMV[0][1][0]=PMV[0][1][1]=PMV[1][1][0]=PMV[1][1][1]=0;
  }


  return(1);

}
