/*
   file: clgd547x.h
   auth: Peter McDermott
   date: Mon Feb 12 14:38:24 CST 1996
   
*/

#ifndef _CLGD547X_H
#define _CLGD547X_H

#include "type.h"
#include "clgd5471.h"
#include "clgd5472.h"

/* taken from /usr/src/xc/programs/xfree86/common/xf86.h */

#define V_PHSYNC    0x0001
#define V_NHSYNC    0x0002
#define V_PVSYNC    0x0004
#define V_NVSYNC    0x0008
#define V_INTERLACE 0x0010
#define V_DBLSCAN   0x0020
#define V_CSYNC     0x0040
#define V_PCSYNC    0x0080
#define V_NCSYNC    0x0100
#define V_PIXMUX    0x1000
#define V_DBLCLK    0x2000

typedef struct {
  char *name;
  double Clock;
  int CrtcHDisplay;
  int CrtcHSyncStart;
  int CrtcHSyncEnd;
  int CrtcHTotal;
  
  int CrtcVDisplay;
  int CrtcVSyncStart;
  int CrtcVSyncEnd;
  int CrtcVTotal;
  
  int bitsPerPixel;
  int buffers;
  int Flags;
} mode, *modePtr;

typedef struct {
  int inMondelloMode;
  clgd5471State *s5471;
  clgd5472State *s5472;
  BYTE *hostMem;

  uint width;
  uint height;
  
} clgd547xState, *clgd547xStatePtr;

extern int clgd547xPCISlot;             /* PCI slot the card is in */

extern char *clgd547xPhysicalBase;      /* physical linear memory detected via PCI */
extern char *clgd547xLogicalBase;       /* logical linear memory (phys. mapped here) */
extern char *clgd547xLogicalBase2;
extern char *clgd547xSecondMegBase;     /* logical base of frame buffer mem (2nd meg) */

extern int clgd547xLogicalSize;
extern int clgd547xInMondelloMode;       /* card is in or not in extended mode */
extern int clgd547xAlreadyInited;        /* whether or not the card has been inited */

extern clgd547xState *clgd547xStateInfo; /* global state information */

int clgd547xProbe();
int clgd547xInit();

void clgd547xSetMode(mode *m);
clgd547xState *clgd547xCreateState();
void clgd547xDeleteState(clgd547xState *s);
void clgd547xSaveState(clgd547xState *s);
void clgd547xRestoreState(clgd547xState *s);
int clgd547xTestMemory(unsigned int *ptr);


/* functions provided to jump-start Mesa conversion */

void clgd547x_init();
void clgd547x_done();

int clgd547x_getxdim();
int clgd547x_getydim();
int clgd547x_getdepth();
int clgd547x_getcolors();
void clgd547x_setcolorindex(uint index, uint red, uint blue, uint green);

#endif


