/* R_ENCODE.H - header file for R_ENCODE.C
 */

/* Copyright (C) 1991-2 RSA Laboratories, a division of RSA Data
   Security, Inc. All rights reserved.
 */

void R_EncodePEMBlock PROTO_LIST
  ((unsigned char *, unsigned int *, unsigned char *, unsigned int));
int R_DecodePEMBlock PROTO_LIST
  ((unsigned char *, unsigned int *, unsigned char *, unsigned int));

