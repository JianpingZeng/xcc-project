#ifndef _COMPILER_H
#define _COMPILER_H

#ifndef __STDC__
# ifdef signed
#  undef signed
# endif
# ifdef volatile
#  undef volatile
# endif
# ifdef const
#  undef const
# endif
# define signed /**/
# ifdef __GNUC__
#  define volatile __volatile__
#  define const __const__
# else
#  define const /**/
# endif /* __GNUC__ */
#endif /* !__STDC__ */

#ifdef NO_INLINE

extern void outb();
extern void outw();
extern void outl();
extern unsigned int inb();
extern unsigned int inw();
extern unsigned int inl();
#if NeedFunctionPrototypes
extern unsigned char rdinx(unsigned short, unsigned char);
extern void wrinx(unsigned short, unsigned char, unsigned char);
extern void modinx(unsigned short, unsigned char, unsigned char, unsigned char);
extern int testrg(unsigned short, unsigned char);
extern int textinx2(unsigned short, unsigned char, unsigned char);
extern int textinx(unsigned short, unsigned char);
#else /* NeedFunctionProtoypes */
extern unsigned char rdinx();
extern void wrinx();
extern void modinx();
extern int testrg();
extern int textinx2();
extern int textinx();
#endif /* NeedFunctionProtoypes */

#else /* NO_INLINE */

#ifdef __GNUC__

#ifndef FAKEIT
#ifdef GCCUSESGAS

/*
 * If gcc uses gas rather than the native assembler, the syntax of these
 * inlines has to be different.		DHD
 */

static __inline__ void
outb(port, val)
short port;
char val;
{
   __asm__ __volatile__("outb %0,%1" : :"a" (val), "d" (port));
}


static __inline__ void
outw(port, val)
short port;
short val;
{
   __asm__ __volatile__("outw %0,%1" : :"a" (val), "d" (port));
}

static __inline__ void
outl(port, val)
short port;
int val;
{
   __asm__ __volatile__("outl %0,%1" : :"a" (val), "d" (port));
}

static __inline__ unsigned int
inb(port)
short port;
{
   unsigned char ret;
   __asm__ __volatile__("inb %1,%0" :
       "=a" (ret) :
       "d" (port));
   return ret;
}

static __inline__ unsigned int
inw(port)
short port;
{
   unsigned short ret;
   __asm__ __volatile__("inw %1,%0" :
       "=a" (ret) :
       "d" (port));
   return ret;
}

static __inline__ unsigned int
inl(port)
short port;
{
   unsigned int ret;
   __asm__ __volatile__("inl %1,%0" :
       "=a" (ret) :
       "d" (port));
   return ret;
}

#else	/* GCCUSESGAS */

static __inline__ void
outb(port, val)
     short port;
     char val;
{
  __asm__ __volatile__("out%B0 (%1)" : :"a" (val), "d" (port));
}

static __inline__ void
outw(port, val)
     short port;
     short val;
{
  __asm__ __volatile__("out%W0 (%1)" : :"a" (val), "d" (port));
}

static __inline__ void
outl(port, val)
     short port;
     unsigned int val;
{
  __asm__ __volatile__("out%L0 (%1)" : :"a" (val), "d" (port));
}

static __inline__ unsigned int
inb(port)
     short port;
{
  unsigned char ret;
  __asm__ __volatile__("in%B0 (%1)" :
		   "=a" (ret) :
		   "d" (port));
  return ret;
}

static __inline__ unsigned int
inw(port)
     short port;
{
  unsigned short ret;
  __asm__ __volatile__("in%W0 (%1)" :
		   "=a" (ret) :
		   "d" (port));
  return ret;
}

static __inline__ unsigned int
inl(port)
     short port;
{
  unsigned int ret;
  __asm__ __volatile__("in%L0 (%1)" :
                   "=a" (ret) :
                   "d" (port));
  return ret;
}

#endif /* GCCUSESGAS */

#else /* FAKEIT */

static __inline__ void
outb(port, val)
     short port;
     char val;
{
}

static __inline__ void
outw(port, val)
     short port;
     short val;
{
}

static __inline__ void
outl(port, val)
     short port;
     int val;
{
}

static __inline__ unsigned int
inb(port)
     short port;
{
  return 0;
}

static __inline__ unsigned int
inw(port)
     short port;
{
  return 0;
}

static __inline__ unsigned int
inl(port)
     short port;
{
  return 0;
}

#endif /* FAKEIT */

#else /* __GNUC__ */
#if !defined(AMOEBA) && !defined(MINIX)
# if defined(__STDC__) && (__STDC__ == 1)
#  ifndef asm
#   define asm __asm
#  endif
# endif
# ifdef SVR4
#  include <sys/types.h>
#  ifndef __USLC__
#   define __USLC__
#  endif
# endif
# include <sys/inline.h>
#endif
#endif

/*
 *-----------------------------------------------------------------------
 * Port manipulation convenience functions
 *-----------------------------------------------------------------------
 */

#ifndef __GNUC__
#define __inline__ /**/
#endif

/*
 * rdinx - read the indexed byte port 'port', index 'ind', and return its value
 */
static __inline__ unsigned char 
#ifdef __STDC__
rdinx(unsigned short port, unsigned char ind)
#else
rdinx(port, ind)
unsigned short port;
unsigned char ind;
#endif
{
	if (port == 0x3C0)		/* reset attribute flip-flop */
		(void) inb(0x3DA);
	outb(port, ind);
	return(inb(port+1));
}

/*
 * wrinx - write 'val' to port 'port', index 'ind'
 */
static __inline__ void 
#ifdef __STDC__
wrinx(unsigned short port, unsigned char ind, unsigned char val)
#else
wrinx(port, ind, val)
unsigned short port;
unsigned char ind, val;
#endif
{
	outb(port, ind);
	outb(port+1, val);
}

/*
 * modinx - in register 'port', index 'ind', set the bits in 'mask' as in 'new';
 *	    the other bits are unchanged.
 */
static __inline__ void
#ifdef __STDC__
modinx(unsigned short port, unsigned char ind, 
       unsigned char mask, unsigned char new)
#else
modinx(port, ind, mask, new)
unsigned short port;
unsigned char ind, mask, new;
#endif
{
	unsigned char tmp;

	tmp = (rdinx(port, ind) & ~mask) | (new & mask);
	wrinx(port, ind, tmp);
}

/*
 * tstrg - returns true iff the bits in 'mask' of register 'port' are
 *	   readable & writable.
 */

static __inline__ int
#ifdef __STDC__
testrg(unsigned short port, unsigned char mask)
#else
tstrg(port, mask)
unsigned short port;
unsigned char mask;
#endif
{
	unsigned char old, new1, new2;

	old = inb(port);
	outb(port, old & ~mask);
	new1 = inb(port) & mask;
	outb(port, old | mask);
	new2 = inb(port) & mask;
	outb(port, old);
	return((new1 == 0) && (new2 == mask));
}

/*
 * testinx2 - returns true iff the bits in 'mask' of register 'port', index
 *	      'ind' are readable & writable.
 */
static __inline__ int
#ifdef __STDC__
testinx2(unsigned short port, unsigned char ind, unsigned char mask)
#else
testinx2(port, ind, mask)
unsigned short port;
unsigned char ind, mask;
#endif
{
	unsigned char old, new1, new2;

	old = rdinx(port, ind);
	wrinx(port, ind, old & ~mask);
	new1 = rdinx(port, ind) & mask;
	wrinx(port, ind, old | mask);
	new2 = rdinx(port, ind) & mask;
	wrinx(port, ind, old);
	return((new1 == 0) && (new2 == mask));
}

/*
 * testinx - returns true iff all bits of register 'port', index 'ind' are 
 *     	     readable & writable.
 */
static __inline__ int
#ifdef __STDC__
testinx(unsigned short port, unsigned char ind)
#else
testinx(port, ind, mask)
unsigned short port;
unsigned char ind;
#endif
{
	return(testinx2(port, ind, 0xFF));
}

#endif /* NO_INLINE */
#endif /* _COMPILER_H */
