/*
  file: clgd5471.h
  auth: Peter McDermott
  date: Feb 12 - sometime
*/

#ifndef CLGD5471_H
#define CLGD5471_H

#include "type.h"

typedef struct {
  int validState;
  
  BYTE *sequencerRegs;
  BYTE *graphicsRegs;  
  BYTE miscReg;       /* this may not be a necessary reg to save */
  BYTE *hostMemory;
  
} clgd5471State, *clgd5471StatePtr;

clgd5471State *clgd5471CreateState();
void clgd5471DeleteState(clgd5471State *state);

void clgd5471SaveState(clgd5471State *state);
void clgd5471RestoreState(clgd5471State *state);

/*----------------------------------------------------------------------------
  outSeq() sends byte to the given sequencer register (upper byte is addr)
-----------------------------------------------------------------------------*/
#define outSeq(data_addr) outw(0x3c4,data_addr);

/*----------------------------------------------------------------------------
  outGraph() sends byte to the given graph. ctl. register (upper byte is addr)
-----------------------------------------------------------------------------*/
#define outGraph(data_addr) outw(0x3ce,data_addr);

#endif

