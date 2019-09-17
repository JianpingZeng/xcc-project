/*
--------------------------------------------------------------------- 
---		 EPIC (Efficient Pyramid Image Coder)             ---
---	 Designed by Eero P. Simoncelli and Edward H. Adelson     ---
---		    Written by Eero P. Simoncelli                 ---
---  Developed at the Vision Science Group, The Media Laboratory  ---
---	Copyright 1989, Massachusetts Institute of Technology     ---
---			 All rights reserved.                     ---
---------------------------------------------------------------------

Permission to use, copy, or modify this software and its documentation
for educational and research purposes only and without fee is hereby
granted, provided that this copyright notice appear on all copies and
supporting documentation.  For any other uses of this software, in
original or modified form, including but not limited to distribution
in whole or in part, specific prior permission must be obtained from
M.I.T. and the authors.  These programs shall not be used, rewritten,
or adapted as the basis of a commercial software or hardware product
without first obtaining appropriate licenses from M.I.T.  M.I.T. makes
no representations about the suitability of this software for any
purpose.  It is provided "as is" without express or implied warranty.

---------------------------------------------------------------------
*/

#include "epic.h"  

/*
==========================================================================
read_byte_image() -- reads the image from the stream (assumes the file
is already open for reading).  Returns error code.
==========================================================================
*/

read_byte_image(stream, image, x_size, y_size)
  FILE *stream;
  register float *image;  /* already allocated */
  int x_size, y_size;
  {
  register int i, im_size = x_size * y_size;
  register Byte *byte_image;
  int status;

  byte_image = (Byte *) check_malloc (im_size*sizeof(*byte_image));
  status = fread(byte_image, sizeof(*byte_image), im_size, stream);
  if (status != im_size) { printf ("Error reading byte image file.\n"); exit(-1); }
  for (i=0; i<im_size; i++) image[i] = (float) byte_image[i];
  check_free ((char *) byte_image);
  }

/*
==========================================================================
Returns true if the open file is a PGM stream
- FOR NOW we make the rather rash assumption that a PGM file starts with
  a P.  This will not differentiate between an ascii or a raw PGM file
  (so here we assume raw)
==========================================================================
*/

int PGMStream(infile)
  FILE *infile;
  {
  char c;
  c = getc(infile);
  ungetc(c,infile);
  return (c == 'P');
  }

/*
==========================================================================
Assume the first character is 'P', also we assume that
all comments go between P5 and the (column row) specification.
Again, we assume that it is a raw pgm file
==========================================================================
*/

float *ReadMatrixFromPGMStream(Infile, xsize, ysize)
  FILE *Infile;
  int *xsize, *ysize;
  {
  float *M;
  char buf[80];
  int rows,columns;
  int i;
  int value;
  int ascii_pgm;
  
  if (Infile == NULL) return(NULL);
  fgets(buf,sizeof(buf),Infile);
  
  if (buf[1] == '2'){
    ascii_pgm = 1;
  } else if (buf[1] == '5'){
    ascii_pgm = 0;
  } else {
    fprintf(stdout,"ReadMatrixFromPGMStream(): File not P2 Or P5 PGM image\n");
    exit(-1);
  }

  do {
    fgets(buf,sizeof(buf),Infile);
  } while(buf[0] == '#');
		
  sscanf(buf," %d %d",xsize,ysize);

  fgets(buf,sizeof(buf),Infile);
  if (strncmp(buf,"255",3) != 0){
    fprintf(stdout,"ReadMatrixFromPGMStream():  File is not a 255-shade PGM image\n");
    exit(-1);
  }
		
  M = (float *) malloc((*xsize)*(*ysize) * sizeof(*M));

  if (M == NULL) {
    fprintf(stdout,"ReadMatrixFromPGMStream():  Unable to alocate enough memory\n");
    close (Infile);
    exit(-1);
  }

  for (i=0;i<(*xsize)*(*ysize);i++){
    if (ascii_pgm){
      fscanf(Infile, " %d",&value);
    } else {
      value = fgetc(Infile);
    }
    M[i] = (float) value;
  }

  return (M);
  }

/*
==========================================================================
write_byte_image() -- Write integer image to file as a stream of bytes.
==========================================================================
*/

write_byte_image(stream, image, x_size, y_size)
  FILE *stream;
  register int *image;
  int x_size, y_size;
  {
  register int i, im_size = x_size * y_size;
  register Byte *byte_image;
  int status;

  byte_image = (Byte *) check_malloc (im_size*sizeof(*byte_image));
  for (i=0; i<im_size; i++) byte_image[i] = (Byte) image[i];
  status = fwrite(byte_image, sizeof(*byte_image), im_size, stream);
  check_free ((char *) byte_image);

  if (status != im_size) { printf ("Error writing byte image file.\n"); exit(-1); }
  }


write_pgm_image(stream, image, x_size, y_size)
  FILE *stream;
  register int *image;
  int x_size, y_size;
  {
  register int i, im_size = x_size * y_size;
  register Byte *byte_image;
  int status;

  byte_image = (Byte *) check_malloc (im_size*sizeof(*byte_image));
  for (i=0; i<im_size; i++) byte_image[i] = (Byte) image[i];


  fprintf(stream, "P5\n");
  fprintf(stream, "# CREATOR: UNEPIC, Version %.2f\n", EPIC_VERSION);
  fprintf(stream, "%d %d\n", x_size, y_size);
  fprintf(stream, "255\n");     /* HARDWIRED DEPTH: 1 byte */

  status = fwrite(byte_image, sizeof(*byte_image), im_size, stream);
  check_free ((char *) byte_image);

  if (status != im_size) { printf ("Error writing byte image file.\n"); exit(-1); }
  }
