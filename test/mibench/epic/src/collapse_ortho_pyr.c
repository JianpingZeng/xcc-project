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

/* 7/96: modified by Rob Buccigrossi to use (nearly-) orthonormal 
symmetric 9-tap QMF filters (given in epic.c). */

/*
======================================================================
collapse_pyr() -- collapse (reconstruct) a QMF-style pyramid using an
arbitrary filter.  Assumes the pyramid was built with correct
inverse filters, and reflected edge treatment (reflect1).  Assumes
storage order of images: LN, HN, VN, DN, HN-1, VN-1, .... D1.  
WARNING: The contents of the pyramid are destroyed!  Both dimensions 
of the image must be divisible by 2^num_levels (the routine
does not check this).
=======================================================================
*/

/* 9 tap orthanormal */

/* Sum to sqrt 2 */
static float lo_filter[FILTER_SIZE] =     
  {0.028071524524270521, -0.06093975981002458, -0.07338062349083309,
    0.4146915396805326, 0.7973282005652047, 0.4146915396805326,
    -0.07338062349083309, -0.06093975981002458, 0.028071524524270521};

static float hi_filter[FILTER_SIZE] =
 {0.028071524524270521, 0.06093975981002458, -0.07338062349083309,
    -0.4146915396805326, 0.7973282005652047, -0.4146915396805326,
    -0.07338062349083309, 0.06093975981002458, 0.028071524524270521};

/* Sum to 2 

static float lo_filter[FILTER_SIZE] =     
 {0.039699130698712316, -0.0861818348110956, -0.1037758729561299,
  0.5864623996175897, 1.127592354901848, 0.5864623996175897,
  -0.1037758729561299, -0.0861818348110956, 0.039699130698712316};

static float hi_filter[FILTER_SIZE] =      
 {0.039699130698712316, 0.0861818348110956, -0.1037758729561299,
  -0.5864623996175897, 1.127592354901848, -0.5864623996175897,
  -0.1037758729561299, 0.0861818348110956, 0.039699130698712316};
*/

collapse_pyr(pyr, result, x_size, y_size, num_levels)
  int *pyr, x_size, y_size, num_levels;
  int *result;
  {
  float *fpyr,*fres;
  int rxsize, rysize, /* Size of resulting lowpass image */
               ixsize, iysize; /* Size of starting bandpass images */
  int level;
  int i;

  fpyr = (float *) malloc(x_size * y_size * sizeof(float));
  fres = (float *) malloc(x_size * y_size * sizeof(float));

  for(i=0;i<x_size*y_size;i++){
    fpyr[i] = (float) pyr[i];
  }

/*
{
FILE* outfile;
outfile = fopen("lena.data","w");
fwrite((char*)fpyr,sizeof(float),512*512,outfile);
fclose(outfile);
}
*/


  for (level = num_levels-1; level >= 0; level--){
    rxsize = x_size >> level;	/* divide by 2^level */
    rysize = y_size >> level;
    ixsize = rxsize >> 1;
    iysize = rysize >> 1;
    
    for (i=0; i<rxsize*rysize; i++) fres[i] = 0;
    collapse_level(fpyr,ixsize,iysize,fres);
    
    if (level > 0)		/* copy result into pyramid */
      for(i=0; i<rxsize*rysize; i++)
	fpyr[i] = fres[i];
  }				/* end for each level */

  for(i=0;i<x_size*y_size;i++){
    result[i] = (int) fres[i];
  }  
  free(fres);
  free(fpyr);
}

/* 
======================================================================
collapse_level() -- collapses a level of the pyramid
======================================================================
*/

collapse_level(float *pyr, int level_x_size, int level_y_size, float *res){
  float *hi_imagetemp, *lo_imagetemp, *filtertemp;
  int total_size = level_x_size*level_y_size, i;
  int filter_size = FILTER_SIZE;
  float * lo_im, *h_im, *v_im, *d_im;

  lo_im = pyr;
  h_im = pyr + total_size;
  v_im = h_im + total_size;
  internal_transpose(v_im, level_y_size, level_x_size); 
  d_im = v_im + total_size;

  filtertemp = (float *) check_malloc (filter_size*sizeof(float));
  hi_imagetemp = (float *) check_malloc (total_size*sizeof(float)*2);
  lo_imagetemp = (float *) check_malloc (total_size*sizeof(float)*2);

  for(i=0;i<total_size*2;i++){
    hi_imagetemp[i] = 0;
    lo_imagetemp[i] = 0;
  }

  level_y_size *= 2;

  /* Filter and subsample in the Y direction */
  internal_expand (lo_im,lo_filter,filtertemp,1,filter_size,
                   0, 1, 0, 2,
                   lo_imagetemp, level_x_size, level_y_size,
		   "reflect1");

  internal_expand (h_im,hi_filter,filtertemp,1,filter_size,
                   0, 1, 1, 2,
                   lo_imagetemp, level_x_size, level_y_size,
		   "reflect1");

  internal_expand (v_im,lo_filter,filtertemp,1,filter_size,  
                   0, 1, 0, 2,
                   hi_imagetemp, level_x_size, level_y_size,
		   "reflect1");
  internal_expand (d_im,hi_filter,filtertemp,1,filter_size,  
                   0, 1, 1, 2,
                   hi_imagetemp, level_x_size, level_y_size,
		   "reflect1");

  level_x_size *= 2;


  /* filter and upsample in the X direction */
  internal_expand (lo_imagetemp,lo_filter,filtertemp,filter_size,1,  
                   0, 2, 0, 1,
                   res, level_x_size, level_y_size,
		   "reflect1");

  internal_expand (hi_imagetemp,hi_filter,filtertemp,filter_size,1,
                   1, 2, 0, 1,
                   res, level_x_size, level_y_size,
		   "reflect1");


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
======================================================================
In-place (integer) matrix tranpose algorithm.  Handles non-square matrices,
too!  Is there a faster algorithm?? 
======================================================================
*/
internal_int_transpose( mat, rows, cols )
  register int *mat;
  register int cols;
  int rows;
  {
  register int swap_pos;
  register int modulus = rows*cols - 1;
  register int current_pos;
  register int swap_val;

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
