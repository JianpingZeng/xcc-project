# 1 "putpic.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "putpic.c"
# 30 "putpic.c"
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

# 31 "putpic.c" 2
# 1 "config.h" 1
# 32 "putpic.c" 2
# 1 "global.h" 1
# 30 "global.h"
# 1 "mpeg2enc.h" 1
# 92 "mpeg2enc.h"
struct mbinfo {
  int mb_type;
  int motion_type;
  int dct_type;
  int mquant;
  int cbp;
  int skipped;
  int MV[2][2][2];
  int mv_field_sel[2][2];
  int dmvector[2];
  double act;
  int var;
};


struct motion_data {
  int forw_hor_f_code,forw_vert_f_code;
  int sxf,syf;
  int back_hor_f_code,back_vert_f_code;
  int sxb,syb;
};
# 31 "global.h" 2
# 46 "global.h"
void range_checks (void);
void profile_and_level_checks ();


void init_fdct (void);
void fdct (short *block);


void idct (short *block);
void init_idct (void);


void motion_estimation (unsigned char *oldorg, unsigned char *neworg, unsigned char *oldref, unsigned char *newref, unsigned char *cur, unsigned char *curref, int sxf, int syf, int sxb, int syb, struct mbinfo *mbi, int secondfield, int ipflag)


                                                   ;


void error (char *text);


void predict (unsigned char *reff[], unsigned char *refb[], unsigned char *cur[3], int secondfield, struct mbinfo *mbi)
                                                              ;


void initbits (void);
void putbits (int val, int n);
void alignbits (void);
int bitcount (void);


void putseqhdr (void);
void putseqext (void);
void putseqdispext (void);
void putuserdata (char *userdata);
void putgophdr (int frame, int closed_gop);
void putpicthdr (void);
void putpictcodext (void);
void putseqend (void);


void putintrablk (short *blk, int cc);
void putnonintrablk (short *blk);
void putmv (int dmv, int f_code);


void putpict (unsigned char *frame);


void putseq (void);


void putDClum (int val);
void putDCchrom (int val);
void putACfirst (int run, int val);
void putAC (int run, int signed_level, int vlcformat);
void putaddrinc (int addrinc);
void putmbtype (int pict_type, int mb_type);
void putmotioncode (int motion_code);
void putdmv (int dmv);
void putcbp (int cbp);


int quant_intra (short *src, short *dst, int dc_prec, unsigned char *quant_mat, int mquant)
                                        ;
int quant_non_intra (short *src, short *dst, unsigned char *quant_mat, int mquant)
                                        ;
void iquant_intra (short *src, short *dst, int dc_prec, unsigned char *quant_mat, int mquant)
                                        ;
void iquant_non_intra (short *src, short *dst, unsigned char *quant_mat, int mquant)
                                        ;


void rc_init_seq (void);
void rc_init_GOP (int np, int nb);
void rc_init_pict (unsigned char *frame);
void rc_update_pict (void);
int rc_start_mb (void);
int rc_calc_mquant (int j);
void vbv_end_of_picture (void);
void calc_vbv_delay (void);


void readframe (char *fname, unsigned char *frame[]);


void calcSNR (unsigned char *org[3], unsigned char *rec[3]);
void stats (void);


void transform (unsigned char *pred[], unsigned char *cur[], struct mbinfo *mbi, short blocks[][64])
                                          ;
void itransform (unsigned char *pred[], unsigned char *cur[], struct mbinfo *mbi, short blocks[][64])
                                          ;
void dct_type_estimation (unsigned char *pred, unsigned char *cur, struct mbinfo *mbi)
                      ;


void writeframe (char *fname, unsigned char *frame[]);




extern char version[]



;

extern char author[]



;


extern unsigned char zig_zag_scan[64]
# 172 "global.h"
;


extern unsigned char alternate_scan[64]
# 185 "global.h"
;


extern unsigned char default_intra_quantizer_matrix[64]
# 202 "global.h"
;


extern unsigned char non_linear_mquant_table[32]
# 215 "global.h"
;






extern unsigned char map_non_linear_mquant[113]
# 233 "global.h"
;




extern unsigned char *newrefframe[3], *oldrefframe[3], *auxframe[3];

extern unsigned char *neworgframe[3], *oldorgframe[3], *auxorgframe[3];

extern unsigned char *predframe[3];

extern short (*blocks)[64];

extern unsigned char intra_q[64], inter_q[64];
extern unsigned char chrom_intra_q[64],chrom_inter_q[64];

extern int dc_dct_pred[3];

extern struct mbinfo *mbinfo;

extern struct motion_data *motion_data;

extern unsigned char *clp;


extern char id_string[256], tplorg[256], tplref[256];
extern char iqname[256], niqname[256];
extern char statname[256];
extern char errortext[256];

extern FILE *outfile, *statfile;
extern int inputtype;

extern int quiet;




extern int N;
extern int M;
extern int P;
extern int nframes;
extern int frame0, tc0;
extern int mpeg1;
extern int fieldpic;



extern int horizontal_size, vertical_size;
extern int width, height;
extern int chrom_width,chrom_height,block_count;
extern int mb_width, mb_height;
extern int width2, height2, mb_height2, chrom_width2;
extern int aspectratio;
extern int frame_rate_code;
extern double frame_rate;
extern double bit_rate;
extern int vbv_buffer_size;
extern int constrparms;
extern int load_iquant, load_niquant;
extern int load_ciquant,load_cniquant;




extern int profile, level;
extern int prog_seq;
extern int chroma_format;
extern int low_delay;




extern int video_format;
extern int color_primaries;
extern int transfer_characteristics;
extern int matrix_coefficients;
extern int display_horizontal_size, display_vertical_size;




extern int temp_ref;
extern int pict_type;
extern int vbv_delay;




extern int forw_hor_f_code, forw_vert_f_code;
extern int back_hor_f_code, back_vert_f_code;
extern int dc_prec;
extern int pict_struct;
extern int topfirst;

extern int frame_pred_dct_tab[3], frame_pred_dct;
extern int conceal_tab[3];
extern int qscale_tab[3], q_scale_type;
extern int intravlc_tab[3], intravlc;
extern int altscan_tab[3], altscan;
extern int repeatfirst;
extern int prog_frame;
# 33 "putpic.c" 2


static void putmvs (int MV[2][2][2], int PMV[2][2][2], int mv_field_sel[2][2], int dmvector[2], int s, int motion_type, int hor_f_code, int vert_f_code)

                                   ;


void putpict(frame)
unsigned char *frame;
{
  int i, j, k, comp, cc;
  int mb_type;
  int PMV[2][2][2];
  int prev_mquant;
  int cbp, MBAinc;

  rc_init_pict(frame);


  putpicthdr();

  if (!mpeg1)
    putpictcodext();

  prev_mquant = rc_start_mb();

  k = 0;

  for (j=0; j<mb_height2; j++)
  {


    for (i=0; i<mb_width; i++)
    {

      if (i==0)
      {

        alignbits();

        if (mpeg1 || vertical_size<=2800)
          putbits(0x101L +j,32);
        else
        {
          putbits(0x101L +(j&127),32);
          putbits(j>>7,3);
        }


        putbits(q_scale_type ? map_non_linear_mquant[prev_mquant]
                             : prev_mquant >> 1, 5);

        putbits(0,1);



        for (cc=0; cc<3; cc++)
          dc_dct_pred[cc] = 0;

        PMV[0][0][0]=PMV[0][0][1]=PMV[1][0][0]=PMV[1][0][1]=0;
        PMV[0][1][0]=PMV[0][1][1]=PMV[1][1][0]=PMV[1][1][1]=0;

        MBAinc = i + 1;
      }

      mb_type = mbinfo[k].mb_type;


      mbinfo[k].mquant = rc_calc_mquant(k);


      if (mb_type & 1)
      {
        for (comp=0; comp<block_count; comp++)
          quant_intra(blocks[k*block_count+comp],blocks[k*block_count+comp],
                      dc_prec,intra_q,mbinfo[k].mquant);
        mbinfo[k].cbp = cbp = (1<<block_count) - 1;
      }
      else
      {
        cbp = 0;
        for (comp=0;comp<block_count;comp++)
          cbp = (cbp<<1) | quant_non_intra(blocks[k*block_count+comp],
                                           blocks[k*block_count+comp],
                                           inter_q,mbinfo[k].mquant);

        mbinfo[k].cbp = cbp;

        if (cbp)
          mb_type|= 2;
      }


      if (cbp && prev_mquant!=mbinfo[k].mquant)
        mb_type|= 16;


      if (i!=0 && i!=mb_width-1 && !cbp)
      {


        if (pict_type==2 && !(mb_type&8))
        {




          for (cc=0; cc<3; cc++)
            dc_dct_pred[cc] = 0;

          PMV[0][0][0]=PMV[0][0][1]=PMV[1][0][0]=PMV[1][0][1]=0;
          PMV[0][1][0]=PMV[0][1][1]=PMV[1][1][0]=PMV[1][1][1]=0;

          mbinfo[k].mb_type = mb_type;
          mbinfo[k].skipped = 1;
          MBAinc++;
          k++;
          continue;
        }

        if (pict_type==3 && pict_struct==3
            && mbinfo[k].motion_type==2
            && ((mbinfo[k-1].mb_type^mb_type)&(8|4))==0
            && (!(mb_type&8) ||
                (PMV[0][0][0]==mbinfo[k].MV[0][0][0] &&
                 PMV[0][0][1]==mbinfo[k].MV[0][0][1]))
            && (!(mb_type&4) ||
                (PMV[0][1][0]==mbinfo[k].MV[0][1][0] &&
                 PMV[0][1][1]==mbinfo[k].MV[0][1][1])))
        {
# 171 "putpic.c"
          mbinfo[k].mb_type = mb_type;
          mbinfo[k].skipped = 1;
          MBAinc++;
          k++;
          continue;
        }

        if (pict_type==3 && pict_struct!=3
            && mbinfo[k].motion_type==1
            && ((mbinfo[k-1].mb_type^mb_type)&(8|4))==0
            && (!(mb_type&8) ||
                (PMV[0][0][0]==mbinfo[k].MV[0][0][0] &&
                 PMV[0][0][1]==mbinfo[k].MV[0][0][1] &&
                 mbinfo[k].mv_field_sel[0][0]==(pict_struct==2)))
            && (!(mb_type&4) ||
                (PMV[0][1][0]==mbinfo[k].MV[0][1][0] &&
                 PMV[0][1][1]==mbinfo[k].MV[0][1][1] &&
                 mbinfo[k].mv_field_sel[0][1]==(pict_struct==2))))
        {
# 200 "putpic.c"
          mbinfo[k].mb_type = mb_type;
          mbinfo[k].skipped = 1;
          MBAinc++;
          k++;
          continue;
        }
      }


      mbinfo[k].skipped = 0;




      if (pict_type==2 && !cbp && !(mb_type&8))
        mb_type|= 8;

      putaddrinc(MBAinc);
      MBAinc = 1;

      putmbtype(pict_type,mb_type);

      if (mb_type & (8|4) && !frame_pred_dct)
        putbits(mbinfo[k].motion_type,2);

      if (pict_struct==3 && cbp && !frame_pred_dct)
        putbits(mbinfo[k].dct_type,1);

      if (mb_type & 16)
      {
        putbits(q_scale_type ? map_non_linear_mquant[mbinfo[k].mquant]
                             : mbinfo[k].mquant>>1,5);
        prev_mquant = mbinfo[k].mquant;
      }

      if (mb_type & 8)
      {

        putmvs(mbinfo[k].MV,PMV,mbinfo[k].mv_field_sel,mbinfo[k].dmvector,0,
          mbinfo[k].motion_type,forw_hor_f_code,forw_vert_f_code);
      }

      if (mb_type & 4)
      {

        putmvs(mbinfo[k].MV,PMV,mbinfo[k].mv_field_sel,mbinfo[k].dmvector,1,
          mbinfo[k].motion_type,back_hor_f_code,back_vert_f_code);
      }

      if (mb_type & 2)
      {
        putcbp((cbp >> (block_count-6)) & 63);
        if (chroma_format!=1)
          putbits(cbp,block_count-6);
      }

      for (comp=0; comp<block_count; comp++)
      {

        if (cbp & (1<<(block_count-1-comp)))
        {
          if (mb_type & 1)
          {
            cc = (comp<4) ? 0 : (comp&1)+1;
            putintrablk(blocks[k*block_count+comp],cc);
          }
          else
            putnonintrablk(blocks[k*block_count+comp]);
        }
      }


      if (!(mb_type & 1))
        for (cc=0; cc<3; cc++)
          dc_dct_pred[cc] = 0;

      if (mb_type & 1 || (pict_type==2 && !(mb_type & 8)))
      {
        PMV[0][0][0]=PMV[0][0][1]=PMV[1][0][0]=PMV[1][0][1]=0;
        PMV[0][1][0]=PMV[0][1][1]=PMV[1][1][0]=PMV[1][1][1]=0;
      }

      mbinfo[k].mb_type = mb_type;
      k++;
    }
  }

  rc_update_pict();
  vbv_end_of_picture();
}







static void putmvs(MV,PMV,mv_field_sel,dmvector,s,motion_type,
  hor_f_code,vert_f_code)
int MV[2][2][2],PMV[2][2][2];
int mv_field_sel[2][2];
int dmvector[2];
int s,motion_type,hor_f_code,vert_f_code;
{
  if (pict_struct==3)
  {
    if (motion_type==2)
    {

      putmv(MV[0][s][0]-PMV[0][s][0],hor_f_code);
      putmv(MV[0][s][1]-PMV[0][s][1],vert_f_code);
      PMV[0][s][0]=PMV[1][s][0]=MV[0][s][0];
      PMV[0][s][1]=PMV[1][s][1]=MV[0][s][1];
    }
    else if (motion_type==1)
    {

      putbits(mv_field_sel[0][s],1);
      putmv(MV[0][s][0]-PMV[0][s][0],hor_f_code);
      putmv((MV[0][s][1]>>1)-(PMV[0][s][1]>>1),vert_f_code);
      putbits(mv_field_sel[1][s],1);
      putmv(MV[1][s][0]-PMV[1][s][0],hor_f_code);
      putmv((MV[1][s][1]>>1)-(PMV[1][s][1]>>1),vert_f_code);
      PMV[0][s][0]=MV[0][s][0];
      PMV[0][s][1]=MV[0][s][1];
      PMV[1][s][0]=MV[1][s][0];
      PMV[1][s][1]=MV[1][s][1];
    }
    else
    {

      putmv(MV[0][s][0]-PMV[0][s][0],hor_f_code);
      putdmv(dmvector[0]);
      putmv((MV[0][s][1]>>1)-(PMV[0][s][1]>>1),vert_f_code);
      putdmv(dmvector[1]);
      PMV[0][s][0]=PMV[1][s][0]=MV[0][s][0];
      PMV[0][s][1]=PMV[1][s][1]=MV[0][s][1];
    }
  }
  else
  {

    if (motion_type==1)
    {

      putbits(mv_field_sel[0][s],1);
      putmv(MV[0][s][0]-PMV[0][s][0],hor_f_code);
      putmv(MV[0][s][1]-PMV[0][s][1],vert_f_code);
      PMV[0][s][0]=PMV[1][s][0]=MV[0][s][0];
      PMV[0][s][1]=PMV[1][s][1]=MV[0][s][1];
    }
    else if (motion_type==2)
    {

      putbits(mv_field_sel[0][s],1);
      putmv(MV[0][s][0]-PMV[0][s][0],hor_f_code);
      putmv(MV[0][s][1]-PMV[0][s][1],vert_f_code);
      putbits(mv_field_sel[1][s],1);
      putmv(MV[1][s][0]-PMV[1][s][0],hor_f_code);
      putmv(MV[1][s][1]-PMV[1][s][1],vert_f_code);
      PMV[0][s][0]=MV[0][s][0];
      PMV[0][s][1]=MV[0][s][1];
      PMV[1][s][0]=MV[1][s][0];
      PMV[1][s][1]=MV[1][s][1];
    }
    else
    {

      putmv(MV[0][s][0]-PMV[0][s][0],hor_f_code);
      putdmv(dmvector[0]);
      putmv(MV[0][s][1]-PMV[0][s][1],vert_f_code);
      putdmv(dmvector[1]);
      PMV[0][s][0]=PMV[1][s][0]=MV[0][s][0];
      PMV[0][s][1]=PMV[1][s][1]=MV[0][s][1];
    }
  }
}
