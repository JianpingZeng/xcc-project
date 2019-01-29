#ifndef SYSTEM_H
#define SYSTEM_H

#ifdef UNIX
#if !defined(HAVE_UNISTD_H) && !defined(MACH) && !defined(_BSD)
#define HAVE_UNISTD_H
#endif

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#else
#include <stdio.h>
#include <sys/types.h>
#endif

int getch();
int kbhit();

/* replacement function for obsolete clock(), just provides random data */
long Clock();

#endif /* UNIX */

#if defined(UNIX) || defined(AMIGA) || defined(VMS)
void ttycbreak();
void ttynorm();
#else
#define ttycbreak()	/* nothing */
#define ttynorm()	/* nothing */
#endif

#if !defined(MSDOS) && !defined(ATARI)
char *strlwr(char *);
#endif

void breakHandler(int);

#endif /* SYSTEM_H */
