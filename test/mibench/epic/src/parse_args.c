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

#include <math.h>
#include <string.h>
#include <stdlib.h>
#include "epic.h"

/* 
==========================================================================
parse_epic_args() -- parse the arglist to the program, determining the 
input and output filenames, and the values of x_size, y_size, num_levels,
and compression_factor.  Read the image data from the input file into the 
float array, and open the output file for writing, checking for errors.
==========================================================================
*/

parse_epic_args (argc, argv, image, x_size, y_size, 
		 num_levels, compression_factor, outfile)
  int argc, *x_size, *y_size, *num_levels;
  double *compression_factor;
  char *argv[];
  float **image;
  FILE **outfile;
  {
  FILE *infile;
  char *filename;
  int arg_ptr=2;   /* first arg is requred infile name */

  /* fill in default values here, then override below if args are present */
  *x_size = *y_size = 256;
  *num_levels = -1;   /* indicates this is not set yet */
  *compression_factor = 20.0;

  if (argc < 2) epic_usage();
  filename = argv[1];
  infile = check_fopen(filename, "r");

  filename = concatenate(argv[1], ".E");   /* default outfile */
  
  while (arg_ptr < argc)
      {
      if (!strcmp(argv[arg_ptr], "-o"))
	  {
	  arg_ptr++;
	  filename = argv[arg_ptr];
	  }
      else if (!strcmp(argv[arg_ptr], "-l"))
	  {
	  arg_ptr++;
	  *num_levels = atoi(argv[arg_ptr]);
	  }
      else if (!strcmp(argv[arg_ptr], "-b"))
	  {
	  arg_ptr++;
	  *compression_factor = (double) atof(argv[arg_ptr]);
	  }
      else if (!strcmp(argv[arg_ptr], "-x"))
	  {
	  arg_ptr++;
	  *x_size = atoi(argv[arg_ptr]);
	  }
      else if (!strcmp(argv[arg_ptr], "-y"))
	  {
	  arg_ptr++;
	  *y_size = atoi(argv[arg_ptr]);
	  }
      else { printf("Unrecognized argument: %s\n", argv[arg_ptr]); epic_usage(); }
      arg_ptr++;
      }  /*   DONE READING ARGS */

  /* Read PGM file here, to get x_size and y_size */
  if (PGMStream(infile))
      {
      *image = ReadMatrixFromPGMStream(infile,x_size,y_size);
      } 
  else 
      {
      *image = (float *) check_malloc(*x_size * *y_size * sizeof(**image));
      read_byte_image(infile, *image, *x_size, *y_size);
      }
  fclose (infile);

  if ( *num_levels == -1 )  /* Choose num_levels automatically */
      {
      *num_levels = 1; /* initialize */
      while (( (*x_size % (1<<*num_levels)) == 0 ) &&
	     ( (*y_size % (1<<*num_levels)) == 0 ) &&
	     ( (*x_size / (1<<*num_levels)) >= FILTER_SIZE ) &&
	     ( (*y_size / (1<<*num_levels)) >= FILTER_SIZE ))
	{ *num_levels += 1; }
      *num_levels -= 1;
      }
  if ( *num_levels < 1 )
      { 
      printf("Error: num_levels must be greater than 0\n");
      exit(-1);
      }
  if ( *num_levels > MAX_LEVELS )
      { 
      printf("Error: cannot build pyramid to more than %d levels.\n", MAX_LEVELS); 
      exit(-1);
      }
  if (( *x_size > MAX_IMAGE_DIM ) ||
      ( *y_size > MAX_IMAGE_DIM ))
      {
      printf("Error: dimensions too big (%d,%d).\n", *x_size, *y_size);
      exit(-1);
      }
  if (( (*x_size % (1<<*num_levels)) != 0 ) ||
      ( (*y_size % (1<<*num_levels)) != 0 ))
      {
      printf ("Error: dimensions (%d,%d) are not divisible by 2^num_levels (num_levels=%d).\n", 
	      *x_size, *y_size, *num_levels);
      exit(-1);
      }
  if (( ( *x_size / (1<<*num_levels)) < FILTER_SIZE ) ||
      ( ( *y_size / (1<<*num_levels)) < FILTER_SIZE ))
      {
      printf ("Error: dimensions (%d,%d) divided by 2^num_levels must be greater than %d.\n", 
	      *x_size, *y_size, FILTER_SIZE);
      exit(-1);
      }
  *outfile = check_fopen(filename, "w");
  }

epic_usage()
  { 
  printf("Usage: \nepic infile [-o outfile] [-x xdim] [-y ydim] [-l levels] [-b binsize]\n"); 
  exit(-1);
  }

parse_unepic_args(argc, argv, epicfile, num_levels, x_size, y_size, 
		  scale_factor, outfile)
  int argc, *x_size, *y_size, *num_levels;
  double *scale_factor;
  char *argv[];
  FILE **epicfile, **outfile; 
  {
  char *filename;
  Byte the_byte;

  if (argc < 2) { printf("Usage:  unepic epicfile [outfile]\n"); exit(-1); }

  *epicfile = check_fopen(argv[1], "r");

  read_byte(temp_byte, *epicfile);
  if (temp_byte != (the_byte = EPIC_ID_TAG))
      { 
      printf("The file %s is not an EPIC file!\n", argv[1]);
      exit(-1); 
      }

  read_byte(temp_byte, *epicfile); *num_levels = (int) temp_byte;
  read_short(temp_short, *epicfile); *x_size = (int) temp_short;
  read_short(temp_short, *epicfile); *y_size = (int) temp_short;
  read_short(temp_short, *epicfile); *scale_factor = (double) temp_short;

  if (argc == 3) filename = argv[2];
  else filename = concatenate(argv[1], ".U");
  *outfile = check_fopen(filename, "w");
  }
