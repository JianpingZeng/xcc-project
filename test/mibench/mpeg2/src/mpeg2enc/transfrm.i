# 1 "transfrm.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "transfrm.c"
# 30 "transfrm.c"
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

# 31 "transfrm.c" 2
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 1 3 4
# 30 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4




# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/huge_val.h" 1 3 4
# 35 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4

# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/huge_valf.h" 1 3 4
# 37 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/huge_vall.h" 1 3 4
# 38 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4


# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/inf.h" 1 3 4
# 41 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4


# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/nan.h" 1 3 4
# 44 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4



# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathdef.h" 1 3 4
# 28 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathdef.h" 3 4
typedef float float_t;

typedef double double_t;
# 48 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4
# 71 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 1 3 4
# 53 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern double acos (double __x) __attribute__ ((__nothrow__)); extern double __acos (double __x) __attribute__ ((__nothrow__));

extern double asin (double __x) __attribute__ ((__nothrow__)); extern double __asin (double __x) __attribute__ ((__nothrow__));

extern double atan (double __x) __attribute__ ((__nothrow__)); extern double __atan (double __x) __attribute__ ((__nothrow__));

extern double atan2 (double __y, double __x) __attribute__ ((__nothrow__)); extern double __atan2 (double __y, double __x) __attribute__ ((__nothrow__));


extern double cos (double __x) __attribute__ ((__nothrow__)); extern double __cos (double __x) __attribute__ ((__nothrow__));

extern double sin (double __x) __attribute__ ((__nothrow__)); extern double __sin (double __x) __attribute__ ((__nothrow__));

extern double tan (double __x) __attribute__ ((__nothrow__)); extern double __tan (double __x) __attribute__ ((__nothrow__));




extern double cosh (double __x) __attribute__ ((__nothrow__)); extern double __cosh (double __x) __attribute__ ((__nothrow__));

extern double sinh (double __x) __attribute__ ((__nothrow__)); extern double __sinh (double __x) __attribute__ ((__nothrow__));

extern double tanh (double __x) __attribute__ ((__nothrow__)); extern double __tanh (double __x) __attribute__ ((__nothrow__));

# 87 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern double acosh (double __x) __attribute__ ((__nothrow__)); extern double __acosh (double __x) __attribute__ ((__nothrow__));

extern double asinh (double __x) __attribute__ ((__nothrow__)); extern double __asinh (double __x) __attribute__ ((__nothrow__));

extern double atanh (double __x) __attribute__ ((__nothrow__)); extern double __atanh (double __x) __attribute__ ((__nothrow__));







extern double exp (double __x) __attribute__ ((__nothrow__)); extern double __exp (double __x) __attribute__ ((__nothrow__));


extern double frexp (double __x, int *__exponent) __attribute__ ((__nothrow__)); extern double __frexp (double __x, int *__exponent) __attribute__ ((__nothrow__));


extern double ldexp (double __x, int __exponent) __attribute__ ((__nothrow__)); extern double __ldexp (double __x, int __exponent) __attribute__ ((__nothrow__));


extern double log (double __x) __attribute__ ((__nothrow__)); extern double __log (double __x) __attribute__ ((__nothrow__));


extern double log10 (double __x) __attribute__ ((__nothrow__)); extern double __log10 (double __x) __attribute__ ((__nothrow__));


extern double modf (double __x, double *__iptr) __attribute__ ((__nothrow__)); extern double __modf (double __x, double *__iptr) __attribute__ ((__nothrow__));

# 127 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern double expm1 (double __x) __attribute__ ((__nothrow__)); extern double __expm1 (double __x) __attribute__ ((__nothrow__));


extern double log1p (double __x) __attribute__ ((__nothrow__)); extern double __log1p (double __x) __attribute__ ((__nothrow__));


extern double logb (double __x) __attribute__ ((__nothrow__)); extern double __logb (double __x) __attribute__ ((__nothrow__));






extern double exp2 (double __x) __attribute__ ((__nothrow__)); extern double __exp2 (double __x) __attribute__ ((__nothrow__));


extern double log2 (double __x) __attribute__ ((__nothrow__)); extern double __log2 (double __x) __attribute__ ((__nothrow__));








extern double pow (double __x, double __y) __attribute__ ((__nothrow__)); extern double __pow (double __x, double __y) __attribute__ ((__nothrow__));


extern double sqrt (double __x) __attribute__ ((__nothrow__)); extern double __sqrt (double __x) __attribute__ ((__nothrow__));





extern double hypot (double __x, double __y) __attribute__ ((__nothrow__)); extern double __hypot (double __x, double __y) __attribute__ ((__nothrow__));






extern double cbrt (double __x) __attribute__ ((__nothrow__)); extern double __cbrt (double __x) __attribute__ ((__nothrow__));








extern double ceil (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __ceil (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern double fabs (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __fabs (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern double floor (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __floor (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern double fmod (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fmod (double __x, double __y) __attribute__ ((__nothrow__));




extern int __isinf (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int __finite (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int isinf (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int finite (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern double drem (double __x, double __y) __attribute__ ((__nothrow__)); extern double __drem (double __x, double __y) __attribute__ ((__nothrow__));



extern double significand (double __x) __attribute__ ((__nothrow__)); extern double __significand (double __x) __attribute__ ((__nothrow__));





extern double copysign (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __copysign (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));






extern double nan (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __nan (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int __isnan (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern int isnan (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern double j0 (double) __attribute__ ((__nothrow__)); extern double __j0 (double) __attribute__ ((__nothrow__));
extern double j1 (double) __attribute__ ((__nothrow__)); extern double __j1 (double) __attribute__ ((__nothrow__));
extern double jn (int, double) __attribute__ ((__nothrow__)); extern double __jn (int, double) __attribute__ ((__nothrow__));
extern double y0 (double) __attribute__ ((__nothrow__)); extern double __y0 (double) __attribute__ ((__nothrow__));
extern double y1 (double) __attribute__ ((__nothrow__)); extern double __y1 (double) __attribute__ ((__nothrow__));
extern double yn (int, double) __attribute__ ((__nothrow__)); extern double __yn (int, double) __attribute__ ((__nothrow__));






extern double erf (double) __attribute__ ((__nothrow__)); extern double __erf (double) __attribute__ ((__nothrow__));
extern double erfc (double) __attribute__ ((__nothrow__)); extern double __erfc (double) __attribute__ ((__nothrow__));
extern double lgamma (double) __attribute__ ((__nothrow__)); extern double __lgamma (double) __attribute__ ((__nothrow__));






extern double tgamma (double) __attribute__ ((__nothrow__)); extern double __tgamma (double) __attribute__ ((__nothrow__));





extern double gamma (double) __attribute__ ((__nothrow__)); extern double __gamma (double) __attribute__ ((__nothrow__));






extern double lgamma_r (double, int *__signgamp) __attribute__ ((__nothrow__)); extern double __lgamma_r (double, int *__signgamp) __attribute__ ((__nothrow__));







extern double rint (double __x) __attribute__ ((__nothrow__)); extern double __rint (double __x) __attribute__ ((__nothrow__));


extern double nextafter (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __nextafter (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern double nexttoward (double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __nexttoward (double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern double remainder (double __x, double __y) __attribute__ ((__nothrow__)); extern double __remainder (double __x, double __y) __attribute__ ((__nothrow__));



extern double scalbn (double __x, int __n) __attribute__ ((__nothrow__)); extern double __scalbn (double __x, int __n) __attribute__ ((__nothrow__));



extern int ilogb (double __x) __attribute__ ((__nothrow__)); extern int __ilogb (double __x) __attribute__ ((__nothrow__));




extern double scalbln (double __x, long int __n) __attribute__ ((__nothrow__)); extern double __scalbln (double __x, long int __n) __attribute__ ((__nothrow__));



extern double nearbyint (double __x) __attribute__ ((__nothrow__)); extern double __nearbyint (double __x) __attribute__ ((__nothrow__));



extern double round (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __round (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern double trunc (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __trunc (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));




extern double remquo (double __x, double __y, int *__quo) __attribute__ ((__nothrow__)); extern double __remquo (double __x, double __y, int *__quo) __attribute__ ((__nothrow__));






extern long int lrint (double __x) __attribute__ ((__nothrow__)); extern long int __lrint (double __x) __attribute__ ((__nothrow__));
extern long long int llrint (double __x) __attribute__ ((__nothrow__)); extern long long int __llrint (double __x) __attribute__ ((__nothrow__));



extern long int lround (double __x) __attribute__ ((__nothrow__)); extern long int __lround (double __x) __attribute__ ((__nothrow__));
extern long long int llround (double __x) __attribute__ ((__nothrow__)); extern long long int __llround (double __x) __attribute__ ((__nothrow__));



extern double fdim (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fdim (double __x, double __y) __attribute__ ((__nothrow__));


extern double fmax (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fmax (double __x, double __y) __attribute__ ((__nothrow__));


extern double fmin (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fmin (double __x, double __y) __attribute__ ((__nothrow__));



extern int __fpclassify (double __value) __attribute__ ((__nothrow__))
     __attribute__ ((__const__));


extern int __signbit (double __value) __attribute__ ((__nothrow__))
     __attribute__ ((__const__));



extern double fma (double __x, double __y, double __z) __attribute__ ((__nothrow__)); extern double __fma (double __x, double __y, double __z) __attribute__ ((__nothrow__));








extern double scalb (double __x, double __n) __attribute__ ((__nothrow__)); extern double __scalb (double __x, double __n) __attribute__ ((__nothrow__));
# 72 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4
# 94 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 1 3 4
# 53 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern float acosf (float __x) __attribute__ ((__nothrow__)); extern float __acosf (float __x) __attribute__ ((__nothrow__));

extern float asinf (float __x) __attribute__ ((__nothrow__)); extern float __asinf (float __x) __attribute__ ((__nothrow__));

extern float atanf (float __x) __attribute__ ((__nothrow__)); extern float __atanf (float __x) __attribute__ ((__nothrow__));

extern float atan2f (float __y, float __x) __attribute__ ((__nothrow__)); extern float __atan2f (float __y, float __x) __attribute__ ((__nothrow__));


extern float cosf (float __x) __attribute__ ((__nothrow__)); extern float __cosf (float __x) __attribute__ ((__nothrow__));

extern float sinf (float __x) __attribute__ ((__nothrow__)); extern float __sinf (float __x) __attribute__ ((__nothrow__));

extern float tanf (float __x) __attribute__ ((__nothrow__)); extern float __tanf (float __x) __attribute__ ((__nothrow__));




extern float coshf (float __x) __attribute__ ((__nothrow__)); extern float __coshf (float __x) __attribute__ ((__nothrow__));

extern float sinhf (float __x) __attribute__ ((__nothrow__)); extern float __sinhf (float __x) __attribute__ ((__nothrow__));

extern float tanhf (float __x) __attribute__ ((__nothrow__)); extern float __tanhf (float __x) __attribute__ ((__nothrow__));

# 87 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern float acoshf (float __x) __attribute__ ((__nothrow__)); extern float __acoshf (float __x) __attribute__ ((__nothrow__));

extern float asinhf (float __x) __attribute__ ((__nothrow__)); extern float __asinhf (float __x) __attribute__ ((__nothrow__));

extern float atanhf (float __x) __attribute__ ((__nothrow__)); extern float __atanhf (float __x) __attribute__ ((__nothrow__));







extern float expf (float __x) __attribute__ ((__nothrow__)); extern float __expf (float __x) __attribute__ ((__nothrow__));


extern float frexpf (float __x, int *__exponent) __attribute__ ((__nothrow__)); extern float __frexpf (float __x, int *__exponent) __attribute__ ((__nothrow__));


extern float ldexpf (float __x, int __exponent) __attribute__ ((__nothrow__)); extern float __ldexpf (float __x, int __exponent) __attribute__ ((__nothrow__));


extern float logf (float __x) __attribute__ ((__nothrow__)); extern float __logf (float __x) __attribute__ ((__nothrow__));


extern float log10f (float __x) __attribute__ ((__nothrow__)); extern float __log10f (float __x) __attribute__ ((__nothrow__));


extern float modff (float __x, float *__iptr) __attribute__ ((__nothrow__)); extern float __modff (float __x, float *__iptr) __attribute__ ((__nothrow__));

# 127 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern float expm1f (float __x) __attribute__ ((__nothrow__)); extern float __expm1f (float __x) __attribute__ ((__nothrow__));


extern float log1pf (float __x) __attribute__ ((__nothrow__)); extern float __log1pf (float __x) __attribute__ ((__nothrow__));


extern float logbf (float __x) __attribute__ ((__nothrow__)); extern float __logbf (float __x) __attribute__ ((__nothrow__));






extern float exp2f (float __x) __attribute__ ((__nothrow__)); extern float __exp2f (float __x) __attribute__ ((__nothrow__));


extern float log2f (float __x) __attribute__ ((__nothrow__)); extern float __log2f (float __x) __attribute__ ((__nothrow__));








extern float powf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __powf (float __x, float __y) __attribute__ ((__nothrow__));


extern float sqrtf (float __x) __attribute__ ((__nothrow__)); extern float __sqrtf (float __x) __attribute__ ((__nothrow__));





extern float hypotf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __hypotf (float __x, float __y) __attribute__ ((__nothrow__));






extern float cbrtf (float __x) __attribute__ ((__nothrow__)); extern float __cbrtf (float __x) __attribute__ ((__nothrow__));








extern float ceilf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __ceilf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern float fabsf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __fabsf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern float floorf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __floorf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern float fmodf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fmodf (float __x, float __y) __attribute__ ((__nothrow__));




extern int __isinff (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int __finitef (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int isinff (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int finitef (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern float dremf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __dremf (float __x, float __y) __attribute__ ((__nothrow__));



extern float significandf (float __x) __attribute__ ((__nothrow__)); extern float __significandf (float __x) __attribute__ ((__nothrow__));





extern float copysignf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __copysignf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));






extern float nanf (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __nanf (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int __isnanf (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern int isnanf (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern float j0f (float) __attribute__ ((__nothrow__)); extern float __j0f (float) __attribute__ ((__nothrow__));
extern float j1f (float) __attribute__ ((__nothrow__)); extern float __j1f (float) __attribute__ ((__nothrow__));
extern float jnf (int, float) __attribute__ ((__nothrow__)); extern float __jnf (int, float) __attribute__ ((__nothrow__));
extern float y0f (float) __attribute__ ((__nothrow__)); extern float __y0f (float) __attribute__ ((__nothrow__));
extern float y1f (float) __attribute__ ((__nothrow__)); extern float __y1f (float) __attribute__ ((__nothrow__));
extern float ynf (int, float) __attribute__ ((__nothrow__)); extern float __ynf (int, float) __attribute__ ((__nothrow__));






extern float erff (float) __attribute__ ((__nothrow__)); extern float __erff (float) __attribute__ ((__nothrow__));
extern float erfcf (float) __attribute__ ((__nothrow__)); extern float __erfcf (float) __attribute__ ((__nothrow__));
extern float lgammaf (float) __attribute__ ((__nothrow__)); extern float __lgammaf (float) __attribute__ ((__nothrow__));






extern float tgammaf (float) __attribute__ ((__nothrow__)); extern float __tgammaf (float) __attribute__ ((__nothrow__));





extern float gammaf (float) __attribute__ ((__nothrow__)); extern float __gammaf (float) __attribute__ ((__nothrow__));






extern float lgammaf_r (float, int *__signgamp) __attribute__ ((__nothrow__)); extern float __lgammaf_r (float, int *__signgamp) __attribute__ ((__nothrow__));







extern float rintf (float __x) __attribute__ ((__nothrow__)); extern float __rintf (float __x) __attribute__ ((__nothrow__));


extern float nextafterf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __nextafterf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern float nexttowardf (float __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __nexttowardf (float __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern float remainderf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __remainderf (float __x, float __y) __attribute__ ((__nothrow__));



extern float scalbnf (float __x, int __n) __attribute__ ((__nothrow__)); extern float __scalbnf (float __x, int __n) __attribute__ ((__nothrow__));



extern int ilogbf (float __x) __attribute__ ((__nothrow__)); extern int __ilogbf (float __x) __attribute__ ((__nothrow__));




extern float scalblnf (float __x, long int __n) __attribute__ ((__nothrow__)); extern float __scalblnf (float __x, long int __n) __attribute__ ((__nothrow__));



extern float nearbyintf (float __x) __attribute__ ((__nothrow__)); extern float __nearbyintf (float __x) __attribute__ ((__nothrow__));



extern float roundf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __roundf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern float truncf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __truncf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));




extern float remquof (float __x, float __y, int *__quo) __attribute__ ((__nothrow__)); extern float __remquof (float __x, float __y, int *__quo) __attribute__ ((__nothrow__));






extern long int lrintf (float __x) __attribute__ ((__nothrow__)); extern long int __lrintf (float __x) __attribute__ ((__nothrow__));
extern long long int llrintf (float __x) __attribute__ ((__nothrow__)); extern long long int __llrintf (float __x) __attribute__ ((__nothrow__));



extern long int lroundf (float __x) __attribute__ ((__nothrow__)); extern long int __lroundf (float __x) __attribute__ ((__nothrow__));
extern long long int llroundf (float __x) __attribute__ ((__nothrow__)); extern long long int __llroundf (float __x) __attribute__ ((__nothrow__));



extern float fdimf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fdimf (float __x, float __y) __attribute__ ((__nothrow__));


extern float fmaxf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fmaxf (float __x, float __y) __attribute__ ((__nothrow__));


extern float fminf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fminf (float __x, float __y) __attribute__ ((__nothrow__));



extern int __fpclassifyf (float __value) __attribute__ ((__nothrow__))
     __attribute__ ((__const__));


extern int __signbitf (float __value) __attribute__ ((__nothrow__))
     __attribute__ ((__const__));



extern float fmaf (float __x, float __y, float __z) __attribute__ ((__nothrow__)); extern float __fmaf (float __x, float __y, float __z) __attribute__ ((__nothrow__));








extern float scalbf (float __x, float __n) __attribute__ ((__nothrow__)); extern float __scalbf (float __x, float __n) __attribute__ ((__nothrow__));
# 95 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4
# 145 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
# 1 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 1 3 4
# 53 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern long double acosl (long double __x) __asm__ ("" "acos") __attribute__ ((__nothrow__)); extern long double __acosl (long double __x) __asm__ ("" "__acos") __attribute__ ((__nothrow__));

extern long double asinl (long double __x) __asm__ ("" "asin") __attribute__ ((__nothrow__)); extern long double __asinl (long double __x) __asm__ ("" "__asin") __attribute__ ((__nothrow__));

extern long double atanl (long double __x) __asm__ ("" "atan") __attribute__ ((__nothrow__)); extern long double __atanl (long double __x) __asm__ ("" "__atan") __attribute__ ((__nothrow__));

extern long double atan2l (long double __y, long double __x) __asm__ ("" "atan2") __attribute__ ((__nothrow__)); extern long double __atan2l (long double __y, long double __x) __asm__ ("" "__atan2") __attribute__ ((__nothrow__));


extern long double cosl (long double __x) __asm__ ("" "cos") __attribute__ ((__nothrow__)); extern long double __cosl (long double __x) __asm__ ("" "__cos") __attribute__ ((__nothrow__));

extern long double sinl (long double __x) __asm__ ("" "sin") __attribute__ ((__nothrow__)); extern long double __sinl (long double __x) __asm__ ("" "__sin") __attribute__ ((__nothrow__));

extern long double tanl (long double __x) __asm__ ("" "tan") __attribute__ ((__nothrow__)); extern long double __tanl (long double __x) __asm__ ("" "__tan") __attribute__ ((__nothrow__));




extern long double coshl (long double __x) __asm__ ("" "cosh") __attribute__ ((__nothrow__)); extern long double __coshl (long double __x) __asm__ ("" "__cosh") __attribute__ ((__nothrow__));

extern long double sinhl (long double __x) __asm__ ("" "sinh") __attribute__ ((__nothrow__)); extern long double __sinhl (long double __x) __asm__ ("" "__sinh") __attribute__ ((__nothrow__));

extern long double tanhl (long double __x) __asm__ ("" "tanh") __attribute__ ((__nothrow__)); extern long double __tanhl (long double __x) __asm__ ("" "__tanh") __attribute__ ((__nothrow__));

# 87 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern long double acoshl (long double __x) __asm__ ("" "acosh") __attribute__ ((__nothrow__)); extern long double __acoshl (long double __x) __asm__ ("" "__acosh") __attribute__ ((__nothrow__));

extern long double asinhl (long double __x) __asm__ ("" "asinh") __attribute__ ((__nothrow__)); extern long double __asinhl (long double __x) __asm__ ("" "__asinh") __attribute__ ((__nothrow__));

extern long double atanhl (long double __x) __asm__ ("" "atanh") __attribute__ ((__nothrow__)); extern long double __atanhl (long double __x) __asm__ ("" "__atanh") __attribute__ ((__nothrow__));







extern long double expl (long double __x) __asm__ ("" "exp") __attribute__ ((__nothrow__)); extern long double __expl (long double __x) __asm__ ("" "__exp") __attribute__ ((__nothrow__));


extern long double frexpl (long double __x, int *__exponent) __asm__ ("" "frexp") __attribute__ ((__nothrow__)); extern long double __frexpl (long double __x, int *__exponent) __asm__ ("" "__frexp") __attribute__ ((__nothrow__));


extern long double ldexpl (long double __x, int __exponent) __asm__ ("" "ldexp") __attribute__ ((__nothrow__)); extern long double __ldexpl (long double __x, int __exponent) __asm__ ("" "__ldexp") __attribute__ ((__nothrow__));


extern long double logl (long double __x) __asm__ ("" "log") __attribute__ ((__nothrow__)); extern long double __logl (long double __x) __asm__ ("" "__log") __attribute__ ((__nothrow__));


extern long double log10l (long double __x) __asm__ ("" "log10") __attribute__ ((__nothrow__)); extern long double __log10l (long double __x) __asm__ ("" "__log10") __attribute__ ((__nothrow__));


extern long double modfl (long double __x, long double *__iptr) __asm__ ("" "modf") __attribute__ ((__nothrow__)); extern long double __modfl (long double __x, long double *__iptr) __asm__ ("" "__modf") __attribute__ ((__nothrow__));

# 127 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/bits/mathcalls.h" 3 4


extern long double expm1l (long double __x) __asm__ ("" "expm1") __attribute__ ((__nothrow__)); extern long double __expm1l (long double __x) __asm__ ("" "__expm1") __attribute__ ((__nothrow__));


extern long double log1pl (long double __x) __asm__ ("" "log1p") __attribute__ ((__nothrow__)); extern long double __log1pl (long double __x) __asm__ ("" "__log1p") __attribute__ ((__nothrow__));


extern long double logbl (long double __x) __asm__ ("" "logb") __attribute__ ((__nothrow__)); extern long double __logbl (long double __x) __asm__ ("" "__logb") __attribute__ ((__nothrow__));






extern long double exp2l (long double __x) __asm__ ("" "exp2") __attribute__ ((__nothrow__)); extern long double __exp2l (long double __x) __asm__ ("" "__exp2") __attribute__ ((__nothrow__));


extern long double log2l (long double __x) __asm__ ("" "log2") __attribute__ ((__nothrow__)); extern long double __log2l (long double __x) __asm__ ("" "__log2") __attribute__ ((__nothrow__));








extern long double powl (long double __x, long double __y) __asm__ ("" "pow") __attribute__ ((__nothrow__)); extern long double __powl (long double __x, long double __y) __asm__ ("" "__pow") __attribute__ ((__nothrow__));


extern long double sqrtl (long double __x) __asm__ ("" "sqrt") __attribute__ ((__nothrow__)); extern long double __sqrtl (long double __x) __asm__ ("" "__sqrt") __attribute__ ((__nothrow__));





extern long double hypotl (long double __x, long double __y) __asm__ ("" "hypot") __attribute__ ((__nothrow__)); extern long double __hypotl (long double __x, long double __y) __asm__ ("" "__hypot") __attribute__ ((__nothrow__));






extern long double cbrtl (long double __x) __asm__ ("" "cbrt") __attribute__ ((__nothrow__)); extern long double __cbrtl (long double __x) __asm__ ("" "__cbrt") __attribute__ ((__nothrow__));








extern long double ceill (long double __x) __asm__ ("" "ceil") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __ceill (long double __x) __asm__ ("" "__ceil") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern long double fabsl (long double __x) __asm__ ("" "fabs") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __fabsl (long double __x) __asm__ ("" "__fabs") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern long double floorl (long double __x) __asm__ ("" "floor") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __floorl (long double __x) __asm__ ("" "__floor") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern long double fmodl (long double __x, long double __y) __asm__ ("" "fmod") __attribute__ ((__nothrow__)); extern long double __fmodl (long double __x, long double __y) __asm__ ("" "__fmod") __attribute__ ((__nothrow__));




extern int __isinfl (long double __value) __asm__ ("" "__isinf") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int __finitel (long double __value) __asm__ ("" "__finite") __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int isinfl (long double __value) __asm__ ("" "isinf") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern int finitel (long double __value) __asm__ ("" "finite") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern long double dreml (long double __x, long double __y) __asm__ ("" "drem") __attribute__ ((__nothrow__)); extern long double __dreml (long double __x, long double __y) __asm__ ("" "__drem") __attribute__ ((__nothrow__));



extern long double significandl (long double __x) __asm__ ("" "significand") __attribute__ ((__nothrow__)); extern long double __significandl (long double __x) __asm__ ("" "__significand") __attribute__ ((__nothrow__));





extern long double copysignl (long double __x, long double __y) __asm__ ("" "copysign") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __copysignl (long double __x, long double __y) __asm__ ("" "__copysign") __attribute__ ((__nothrow__)) __attribute__ ((__const__));






extern long double nanl (__const char *__tagb) __asm__ ("" "nan") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __nanl (__const char *__tagb) __asm__ ("" "__nan") __attribute__ ((__nothrow__)) __attribute__ ((__const__));





extern int __isnanl (long double __value) __asm__ ("" "__isnan") __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern int isnanl (long double __value) __asm__ ("" "isnan") __attribute__ ((__nothrow__)) __attribute__ ((__const__));


extern long double j0l (long double) __asm__ ("" "j0") __attribute__ ((__nothrow__)); extern long double __j0l (long double) __asm__ ("" "__j0") __attribute__ ((__nothrow__));
extern long double j1l (long double) __asm__ ("" "j1") __attribute__ ((__nothrow__)); extern long double __j1l (long double) __asm__ ("" "__j1") __attribute__ ((__nothrow__));
extern long double jnl (int, long double) __asm__ ("" "jn") __attribute__ ((__nothrow__)); extern long double __jnl (int, long double) __asm__ ("" "__jn") __attribute__ ((__nothrow__));
extern long double y0l (long double) __asm__ ("" "y0") __attribute__ ((__nothrow__)); extern long double __y0l (long double) __asm__ ("" "__y0") __attribute__ ((__nothrow__));
extern long double y1l (long double) __asm__ ("" "y1") __attribute__ ((__nothrow__)); extern long double __y1l (long double) __asm__ ("" "__y1") __attribute__ ((__nothrow__));
extern long double ynl (int, long double) __asm__ ("" "yn") __attribute__ ((__nothrow__)); extern long double __ynl (int, long double) __asm__ ("" "__yn") __attribute__ ((__nothrow__));






extern long double erfl (long double) __asm__ ("" "erf") __attribute__ ((__nothrow__)); extern long double __erfl (long double) __asm__ ("" "__erf") __attribute__ ((__nothrow__));
extern long double erfcl (long double) __asm__ ("" "erfc") __attribute__ ((__nothrow__)); extern long double __erfcl (long double) __asm__ ("" "__erfc") __attribute__ ((__nothrow__));
extern long double lgammal (long double) __asm__ ("" "lgamma") __attribute__ ((__nothrow__)); extern long double __lgammal (long double) __asm__ ("" "__lgamma") __attribute__ ((__nothrow__));






extern long double tgammal (long double) __asm__ ("" "tgamma") __attribute__ ((__nothrow__)); extern long double __tgammal (long double) __asm__ ("" "__tgamma") __attribute__ ((__nothrow__));





extern long double gammal (long double) __asm__ ("" "gamma") __attribute__ ((__nothrow__)); extern long double __gammal (long double) __asm__ ("" "__gamma") __attribute__ ((__nothrow__));






extern long double lgammal_r (long double, int *__signgamp) __asm__ ("" "lgamma_r") __attribute__ ((__nothrow__)); extern long double __lgammal_r (long double, int *__signgamp) __asm__ ("" "__lgamma_r") __attribute__ ((__nothrow__));







extern long double rintl (long double __x) __asm__ ("" "rint") __attribute__ ((__nothrow__)); extern long double __rintl (long double __x) __asm__ ("" "__rint") __attribute__ ((__nothrow__));


extern long double nextafterl (long double __x, long double __y) __asm__ ("" "nextafter") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __nextafterl (long double __x, long double __y) __asm__ ("" "__nextafter") __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern long double nexttowardl (long double __x, long double __y) __asm__ ("" "nexttoward") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __nexttowardl (long double __x, long double __y) __asm__ ("" "__nexttoward") __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern long double remainderl (long double __x, long double __y) __asm__ ("" "remainder") __attribute__ ((__nothrow__)); extern long double __remainderl (long double __x, long double __y) __asm__ ("" "__remainder") __attribute__ ((__nothrow__));



extern long double scalbnl (long double __x, int __n) __asm__ ("" "scalbn") __attribute__ ((__nothrow__)); extern long double __scalbnl (long double __x, int __n) __asm__ ("" "__scalbn") __attribute__ ((__nothrow__));



extern int ilogbl (long double __x) __asm__ ("" "ilogb") __attribute__ ((__nothrow__)); extern int __ilogbl (long double __x) __asm__ ("" "__ilogb") __attribute__ ((__nothrow__));




extern long double scalblnl (long double __x, long int __n) __asm__ ("" "scalbln") __attribute__ ((__nothrow__)); extern long double __scalblnl (long double __x, long int __n) __asm__ ("" "__scalbln") __attribute__ ((__nothrow__));



extern long double nearbyintl (long double __x) __asm__ ("" "nearbyint") __attribute__ ((__nothrow__)); extern long double __nearbyintl (long double __x) __asm__ ("" "__nearbyint") __attribute__ ((__nothrow__));



extern long double roundl (long double __x) __asm__ ("" "round") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __roundl (long double __x) __asm__ ("" "__round") __attribute__ ((__nothrow__)) __attribute__ ((__const__));



extern long double truncl (long double __x) __asm__ ("" "trunc") __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __truncl (long double __x) __asm__ ("" "__trunc") __attribute__ ((__nothrow__)) __attribute__ ((__const__));




extern long double remquol (long double __x, long double __y, int *__quo) __asm__ ("" "remquo") __attribute__ ((__nothrow__)); extern long double __remquol (long double __x, long double __y, int *__quo) __asm__ ("" "__remquo") __attribute__ ((__nothrow__));






extern long int lrintl (long double __x) __asm__ ("" "lrint") __attribute__ ((__nothrow__)); extern long int __lrintl (long double __x) __asm__ ("" "__lrint") __attribute__ ((__nothrow__));
extern long long int llrintl (long double __x) __asm__ ("" "llrint") __attribute__ ((__nothrow__)); extern long long int __llrintl (long double __x) __asm__ ("" "__llrint") __attribute__ ((__nothrow__));



extern long int lroundl (long double __x) __asm__ ("" "lround") __attribute__ ((__nothrow__)); extern long int __lroundl (long double __x) __asm__ ("" "__lround") __attribute__ ((__nothrow__));
extern long long int llroundl (long double __x) __asm__ ("" "llround") __attribute__ ((__nothrow__)); extern long long int __llroundl (long double __x) __asm__ ("" "__llround") __attribute__ ((__nothrow__));



extern long double fdiml (long double __x, long double __y) __asm__ ("" "fdim") __attribute__ ((__nothrow__)); extern long double __fdiml (long double __x, long double __y) __asm__ ("" "__fdim") __attribute__ ((__nothrow__));


extern long double fmaxl (long double __x, long double __y) __asm__ ("" "fmax") __attribute__ ((__nothrow__)); extern long double __fmaxl (long double __x, long double __y) __asm__ ("" "__fmax") __attribute__ ((__nothrow__));


extern long double fminl (long double __x, long double __y) __asm__ ("" "fmin") __attribute__ ((__nothrow__)); extern long double __fminl (long double __x, long double __y) __asm__ ("" "__fmin") __attribute__ ((__nothrow__));



extern int __fpclassifyl (long double __value) __asm__ ("" "__fpclassify") __attribute__ ((__nothrow__))
     __attribute__ ((__const__));


extern int __signbitl (long double __value) __asm__ ("" "__signbit") __attribute__ ((__nothrow__))
     __attribute__ ((__const__));



extern long double fmal (long double __x, long double __y, long double __z) __asm__ ("" "fma") __attribute__ ((__nothrow__)); extern long double __fmal (long double __x, long double __y, long double __z) __asm__ ("" "__fma") __attribute__ ((__nothrow__));








extern long double scalbl (long double __x, long double __n) __asm__ ("" "scalb") __attribute__ ((__nothrow__)); extern long double __scalbl (long double __x, long double __n) __asm__ ("" "__scalb") __attribute__ ((__nothrow__));
# 146 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 2 3 4
# 161 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
extern int signgam;
# 202 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
enum
  {
    FP_NAN,

    FP_INFINITE,

    FP_ZERO,

    FP_SUBNORMAL,

    FP_NORMAL

  };
# 295 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
typedef enum
{
  _IEEE_ = -1,
  _SVID_,
  _XOPEN_,
  _POSIX_,
  _ISOC_
} _LIB_VERSION_TYPE;




extern _LIB_VERSION_TYPE _LIB_VERSION;
# 320 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4
struct exception

  {
    int type;
    char *name;
    double arg1;
    double arg2;
    double retval;
  };




extern int matherr (struct exception *__exc);
# 476 "/home/xlous/Development/experiment/arm-2010.09/bin/../arm-none-linux-gnueabi/libc/usr/include/math.h" 3 4

# 32 "transfrm.c" 2
# 1 "config.h" 1
# 33 "transfrm.c" 2
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
# 34 "transfrm.c" 2


static void add_pred (unsigned char *pred, unsigned char *cur, int lx, short *blk)
                      ;
static void sub_pred (unsigned char *pred, unsigned char *cur, int lx, short *blk)
                      ;


void transform(pred,cur,mbi,blocks)
unsigned char *pred[], *cur[];
struct mbinfo *mbi;
short blocks[][64];
{
  int i, j, i1, j1, k, n, cc, offs, lx;

  k = 0;

  for (j=0; j<height2; j+=16)
    for (i=0; i<width; i+=16)
    {
      for (n=0; n<block_count; n++)
      {
        cc = (n<4) ? 0 : (n&1)+1;
        if (cc==0)
        {

          if ((pict_struct==3) && mbi[k].dct_type)
          {

            offs = i + ((n&1)<<3) + width*(j+((n&2)>>1));
            lx = width<<1;
          }
          else
          {

            offs = i + ((n&1)<<3) + width2*(j+((n&2)<<2));
            lx = width2;
          }

          if (pict_struct==2)
            offs += width;
        }
        else
        {



          i1 = (chroma_format==3) ? i : i>>1;
          j1 = (chroma_format!=1) ? j : j>>1;

          if ((pict_struct==3) && mbi[k].dct_type
              && (chroma_format!=1))
          {

            offs = i1 + (n&8) + chrom_width*(j1+((n&2)>>1));
            lx = chrom_width<<1;
          }
          else
          {

            offs = i1 + (n&8) + chrom_width2*(j1+((n&2)<<2));
            lx = chrom_width2;
          }

          if (pict_struct==2)
            offs += chrom_width;
        }

        sub_pred(pred[cc]+offs,cur[cc]+offs,lx,blocks[k*block_count+n]);
        fdct(blocks[k*block_count+n]);
      }

      k++;
    }
}


void itransform(pred,cur,mbi,blocks)
unsigned char *pred[],*cur[];
struct mbinfo *mbi;
short blocks[][64];
{
  int i, j, i1, j1, k, n, cc, offs, lx;

  k = 0;

  for (j=0; j<height2; j+=16)
    for (i=0; i<width; i+=16)
    {
      for (n=0; n<block_count; n++)
      {
        cc = (n<4) ? 0 : (n&1)+1;

        if (cc==0)
        {

          if ((pict_struct==3) && mbi[k].dct_type)
          {

            offs = i + ((n&1)<<3) + width*(j+((n&2)>>1));
            lx = width<<1;
          }
          else
          {

            offs = i + ((n&1)<<3) + width2*(j+((n&2)<<2));
            lx = width2;
          }

          if (pict_struct==2)
            offs += width;
        }
        else
        {



          i1 = (chroma_format==3) ? i : i>>1;
          j1 = (chroma_format!=1) ? j : j>>1;

          if ((pict_struct==3) && mbi[k].dct_type
              && (chroma_format!=1))
          {

            offs = i1 + (n&8) + chrom_width*(j1+((n&2)>>1));
            lx = chrom_width<<1;
          }
          else
          {

            offs = i1 + (n&8) + chrom_width2*(j1+((n&2)<<2));
            lx = chrom_width2;
          }

          if (pict_struct==2)
            offs += chrom_width;
        }

        idct(blocks[k*block_count+n]);
        add_pred(pred[cc]+offs,cur[cc]+offs,lx,blocks[k*block_count+n]);
      }

      k++;
    }
}


static void add_pred(pred,cur,lx,blk)
unsigned char *pred, *cur;
int lx;
short *blk;
{
  int i, j;

  for (j=0; j<8; j++)
  {
    for (i=0; i<8; i++)
      cur[i] = clp[blk[i] + pred[i]];
    blk+= 8;
    cur+= lx;
    pred+= lx;
  }
}


static void sub_pred(pred,cur,lx,blk)
unsigned char *pred, *cur;
int lx;
short *blk;
{
  int i, j;

  for (j=0; j<8; j++)
  {
    for (i=0; i<8; i++)
      blk[i] = cur[i] - pred[i];
    blk+= 8;
    cur+= lx;
    pred+= lx;
  }
}






void dct_type_estimation(pred,cur,mbi)
unsigned char *pred,*cur;
struct mbinfo *mbi;
{
  short blk0[128], blk1[128];
  int i, j, i0, j0, k, offs, s0, s1, sq0, sq1, s01;
  double d, r;

  k = 0;

  for (j0=0; j0<height2; j0+=16)
    for (i0=0; i0<width; i0+=16)
    {
      if (frame_pred_dct || pict_struct!=3)
        mbi[k].dct_type = 0;
      else
      {





        for (j=0; j<8; j++)
        {
          offs = width*((j<<1)+j0) + i0;
          for (i=0; i<16; i++)
          {
            blk0[16*j+i] = cur[offs] - pred[offs];
            blk1[16*j+i] = cur[offs+width] - pred[offs+width];
            offs++;
          }
        }

        s0=s1=sq0=sq1=s01=0;

        for (i=0; i<128; i++)
        {
          s0+= blk0[i];
          sq0+= blk0[i]*blk0[i];
          s1+= blk1[i];
          sq1+= blk1[i]*blk1[i];
          s01+= blk0[i]*blk1[i];
        }

        d = (sq0-(s0*s0)/128.0)*(sq1-(s1*s1)/128.0);

        if (d>0.0)
        {
          r = (s01-(s0*s1)/128.0)/sqrt(d);
          if (r>0.5)
            mbi[k].dct_type = 0;
          else
            mbi[k].dct_type = 1;
        }
        else
          mbi[k].dct_type = 1;
      }
      k++;
    }
}
