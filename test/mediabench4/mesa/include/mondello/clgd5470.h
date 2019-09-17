/*
  file: clgd5470.h
*/

#ifndef CLGD5470_H
#define CLGD5470_H

#include "type.h"

void clgd5470Init();
void clgd5470GetHostMemory(BYTE *mem);
void clgd5470PutHostMemory(BYTE *mem);

extern unsigned int *clgd5470LogicalBase;
extern unsigned int *clgd5470LogicalBase2;  /* second meg */
extern unsigned int *listPtr;               /* display list pointer */
extern unsigned int listStart;              /*       see graphics.c */

extern unsigned int *listMax;	/* threshold for starting execution of instrs */
extern unsigned int listNum;   /* list number being written to               */
extern unsigned int *listPtr0; /* base of list 0                             */
extern unsigned int *listMax0; /* threshold of list 0                        */
extern unsigned int *listPtr1; /* base of list 1                             */
extern unsigned int *listMax1; /* threshold of list 1                        */

#endif
