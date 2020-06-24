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
#include "epic.h"

/*
======================================================================
build_pyr() -- builds a separable QMF-style pyramid.  The pyramid is
written over the original image.  NOTE: the image size must be
divisible by 2^num_levels, but we do not check this here.
======================================================================
*/
build_pyr(image, x_size, y_size, num_levels, lo_filter, hi_filter, filter_size)
  int x_size, y_size, num_levels, filter_size;
  float *image, *lo_filter, *hi_filter;
  {
  int x_level, y_level, level;

  for (level = 0, x_level = x_size, y_level = y_size;  
       level < num_levels;
       level++, x_level /= 2, y_level /= 2)
    build_level(image, x_level, y_level, lo_filter, hi_filter, filter_size, image);
  }

/* 
======================================================================
build_level() -- builds a level of the pyramid by computing 4
filtered and subsampled images.  Since the convolution is separable,
image and result-block can point to the same place!  Image order is
lowpass, horizontal, vertical (transposed), and diagonal.
======================================================================
*/
build_level(image, level_x_size, level_y_size, lo_filter, hi_filter,
	    filter_size, result_block)
  int level_x_size, level_y_size, filter_size;
  float *image, *result_block, *lo_filter, *hi_filter;
  {
  float *hi_imagetemp, *lo_imagetemp, *filtertemp;
  int total_size = level_x_size*level_y_size, i;

  filtertemp = (float *) check_malloc (filter_size*sizeof(float));
  hi_imagetemp = (float *) check_malloc (total_size*sizeof(float)/2);
  lo_imagetemp = (float *) check_malloc (total_size*sizeof(float)/2);
  /* filter and subsample in the X direction */
  internal_filter (image, level_x_size, level_y_size, 
		   lo_filter, filtertemp, filter_size, 1, 
		   0, 2, 0, 1, lo_imagetemp, "reflect1", 0);
  internal_filter (image, level_x_size, level_y_size, 
		   hi_filter, filtertemp, filter_size, 1, 
		   1, 2, 0, 1, hi_imagetemp, "reflect1", 0);

  level_x_size /= 2;
  /* now filter and subsample in the Y direction */
  internal_filter (lo_imagetemp, level_x_size, level_y_size, /* lowpass */
		   lo_filter, filtertemp, 1, filter_size, 
		   0, 1, 0, 2, result_block, "reflect1", 0);
  internal_filter (lo_imagetemp, level_x_size, level_y_size, /* horizontal */
		   hi_filter, filtertemp, 1, filter_size, 
		   0, 1, 1, 2, (result_block += (total_size/4)), "reflect1", 0);
  internal_filter (hi_imagetemp, level_x_size, level_y_size,  /* vertical */
		   lo_filter, filtertemp, 1, filter_size, 
		   0, 1, 0, 2, (result_block += (total_size/4)), "reflect1", 0);
  /* transpose the vertical band for more efficient scanning */
  internal_transpose(result_block, level_y_size/2, level_x_size); 
  internal_filter (hi_imagetemp, level_x_size, level_y_size,  /* diagonal */
		   hi_filter, filtertemp, 1, filter_size, 
		   0, 1, 1, 2, (result_block += (total_size/4)), "reflect1", 0);
  check_free ((char *) filtertemp);
  check_free ((char *) hi_imagetemp);
  check_free ((char *) lo_imagetemp);
}

/* 
======================================================================
In-place matrix tranpose algorithm.  Handles non-square matrices,
too!  Is there a faster algorithm?? 
======================================================================
*/
internal_transpose( mat, rows, cols )
  register float *mat;
  register int cols;
  int rows;
  {
  register int swap_pos;
  register int modulus = rows*cols - 1;
  register int current_pos;
  register float swap_val;

  /* loop, ignoring first and last elements */
  for (current_pos=1; current_pos<modulus; current_pos++)
      {
      /* Compute swap position */
      swap_pos = current_pos;
      do
	  {
	  swap_pos = (swap_pos * cols) % modulus;
	  }
      while (swap_pos < current_pos);
      
      if ( current_pos != swap_pos )
	  {
	  swap_val = mat[swap_pos];
	  mat[swap_pos] = mat[current_pos];
	  mat[current_pos] = swap_val;
	  }
      }
  }


/*
main(argc, argv)
  int argc;
  char *argv[];
  {
  char *filename, *resultfile;
  FILE *fp, *fp_result;
  int readstat, writestat, i, num_levels = 4;
  Byte *byte_image;
  float *image, scale_factor = 16.0;
  int *int_pyr, *res;
static float lo_filter[] =     
  { 0.007987761489921101,  0.02011649866148413,  -0.05015758257647976,
   -0.12422330961337678,   0.29216982108655865,   0.7082136219037853,
    0.29216982108655865,  -0.12422330961337678,  -0.05015758257647976,
    0.02011649866148413,   0.007987761489921101 };

static float hi_filter[] =
  { -0.007987761489921101,  0.02011649866148413,  0.05015758257647976,
    -0.12422330961337678,  -0.29216982108655865,  0.7082136219037853,
    -0.29216982108655865,  -0.12422330961337678,  0.05015758257647976,
     0.02011649866148413,  -0.007987761489921101 };

  filename = argv[1];
  resultfile = argv[2];
  fp = fopen (filename, "r");
  fp_result = fopen (resultfile, "w");
  
  byte_image = (Byte *) check_malloc (256*256*sizeof(*byte_image));
  image = (float *) check_malloc (256*256*sizeof(*image));
  int_pyr = (int *) check_malloc (256*256*sizeof(*int_pyr));
  res = (int *) check_malloc (256*256*sizeof(*res));

  printf("Reading file ... ");
  readstat = read_array(byte_image, 256*256, fp);
  for (i=0; i<256*256; i++)  image[i] = (float) (scale_factor * byte_image[i]);
  printf ("Read %d items of data.\n", readstat);
  fclose(fp);

  printf("Building Pyramid ...\n");
  build_pyr(image, 256, 256, num_levels, lo_filter, hi_filter, 11);

  for (i=0; i<256*256; i++)  int_pyr[i] = (int) image[i];

  printf("Collapsing Pyramid ...\n");
  collapse_pyr(int_pyr, res, 256, 256, num_levels);

  for (i=0; i<256*256; i++)  
    { 
    if (res[i] < 0) 
      { 
      printf("Error: value less than zero: %d\n", res[i]); 
      res[i] = 0; 
      }
    image[i] = (float) (res[i] / scale_factor);
    }

  printf("Writing result file ... \n");
  writestat = write_array(byte_image, 256*256, fp_result);
  fclose(fp_result);
  }
*/

/*
cc -o pyr -O4 build_pyr.c collapse_pyr.c utilities.c convolve.c edges.c -lm
*/


