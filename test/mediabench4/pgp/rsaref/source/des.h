/* DES.H - header file for DESC.C
 */

/* Copyright (C) 1991-2 RSA Laboratories, a division of RSA Data
   Security, Inc. All rights reserved.
 */

/* DES-CBC context.
 */
typedef struct {
  unsigned char subkeyBit[16][48];                               /* subkeys */
  unsigned char ivBit[64];                           /* initializing vector */
  int encrypt;                                              /* encrypt flag */
} DES_CBC_CTX;

void DES_CBCInit PROTO_LIST 
  ((DES_CBC_CTX *, unsigned char *, unsigned char *, int));
int DES_CBCUpdate PROTO_LIST
  ((DES_CBC_CTX *, unsigned char *, unsigned char *, unsigned int));
void DES_CBCFinal PROTO_LIST ((DES_CBC_CTX *));
