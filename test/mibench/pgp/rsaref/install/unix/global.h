/* GLOBAL.H - RSAREF types and constants
 */

/****************************************************************
  				NOTE:
 This copy has been modified for compilation on DEC OSF/1 on Alpha
 AXP.  See the definition of UINT4 below.
 John Kohl, <jtk@atria.com>, 1994/June/16
 ****************************************************************/
/* Copyright (C) 1991-2 RSA Laboratories, a division of RSA Data
   Security, Inc. All rights reserved.
 */

/* PROTOTYPES should be set to one if and only if the compiler supports
     function argument prototyping.
   The following makes PROTOTYPES default to 0 if it has not already been
     defined with C compiler flags.
 */
#ifndef PROTOTYPES
#define PROTOTYPES 0
#endif

/* POINTER defines a generic pointer type */
typedef unsigned char *POINTER;

/* UINT2 defines a two byte word */
typedef unsigned short int UINT2;

/* UINT4 defines a four byte word */
#ifdef __alpha
typedef unsigned int UINT4;
#else
typedef unsigned long int UINT4;
#endif

/* PROTO_LIST is defined depending on how PROTOTYPES is defined above.
   If using PROTOTYPES, then PROTO_LIST returns the list, otherwise it
     returns an empty list.  
 */
#if PROTOTYPES
#define PROTO_LIST(list) list
#else
#define PROTO_LIST(list) ()
#endif

