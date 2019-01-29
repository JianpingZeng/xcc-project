/*
  file: clgd5472.h
*/

#ifndef clgd5472_H
#define clgd5472_H

#include <sys/types.h>
#include "type.h"

typedef struct {
 int dummy;
} clgd5472State, *clgd5472StatePtr;

clgd5472State *clgd5472CreateState();
void clgd5472DeleteState(clgd5472State *state);
void clgd5472SaveState(clgd5472State *state);
void clgd5472RestoreState(clgd5472State *state);

void clgd5472InitLUT();
void clgd5472WriteIndex(uint index, uint red, uint green, uint blue);

void setBuffer(int buffer);  /* for double buffering, 0=first, 1=second */

#define clgd5472ShowCsr() outDacW(0x36,0x01);
#define clgd5472HideCsr() outDacW(0x36,0x00);

#endif

