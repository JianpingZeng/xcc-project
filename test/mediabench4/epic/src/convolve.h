/* 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;  File: convolve.h
;;;  Author: Simoncelli
;;;  Description: Header file for convolve.c
;;;  Creation Date:
;;;  ----------------------------------------------------------------
;;;    Object-Based Vision and Image Understanding System (OBVIUS),
;;;      Copyright 1988, Vision Science Group,  Media Laboratory,  
;;;              Massachusetts Institute of Technology.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
*/

#define abs(x)	  (x>=0 ? x : -(x))
#define ROOT2 1.4142135623730951
#define FILTER 0
#define EXPAND 1
#define IS    ==
#define ISNT  !=
#define AND &&
#define OR ||

typedef  int (*fptr)();

typedef struct 
  {
  char *name;
  fptr func;
  } EDGE_HANDLER;

fptr edge_function();
int internal_filter(), internal_expand();


/* Local Variables: */
/* buffer-read-only: t */
/* End: */
