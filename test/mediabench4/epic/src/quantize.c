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
==========================================================================
quantize_pyr() -- quantizes the sub-band images in the pyramid.  The
pyramid should be stored in the order: L(N) V(N) H(N) D(N) V(N-1)
H(N-1) D(N-1) V(N-2) ... D(1), where L=Lowpass, V=Vertical,
H=Horizontal, D=Diagonal and the number in paretheses is the level
number.  Compression_factor is a user parameter which determines how
much compression is desired.  The quantize_pyr routine must determine
the binsizes to be used.  For now, we use the same binsize for each
band, decreasing by a constant factor at each level.  CLEARLY NON-OPTIMAL!
BinValueType should be an unsigned thing!
==========================================================================
*/

quantize_pyr (float_pyr, q_pyr, im_size, num_levels, compression_factor, bin_size)
  float *float_pyr;
  BinIndexType *q_pyr;
  BinValueType *bin_size;
  int im_size, num_levels;
  double compression_factor;
  {
  int level, bin_index, im_num, pyr_offset;
  double the_bin_size = compression_factor;
  BinValueType max_bin_size = 0;

  max_bin_size = ~max_bin_size;   /* all 1's */

  for (im_size/=4, level=1, bin_index=3*num_levels;
       level <= num_levels; 
       level++, im_size /= 4, the_bin_size /= 2.0)
    for (im_num = 3, pyr_offset = 3*im_size;
	 im_num >= ((level == num_levels) ? 0 : 1);
	 im_num--, pyr_offset-=im_size, bin_index--)
	{
	if (the_bin_size<0.0) bin_size[bin_index] = 0;
	else if (the_bin_size>max_bin_size) bin_size[bin_index] = max_bin_size;
	else  bin_size[bin_index] = (BinValueType) (the_bin_size+0.5); /* round */

	quantize_image(float_pyr+pyr_offset, q_pyr+pyr_offset, im_size, 
		       (bin_size+bin_index));
	/* printf("QUANTIZE: bin_size = %d.\n", (bin_size[bin_index])); */
	}
  }
  
/* 
==========================================================================
quantize_image() -- Quantizes float_im and puts integer index values
into int_im.  The index values are simply round(val/binsize). 
==========================================================================
*/

quantize_image (float_im, q_im, im_size, bin_size)
  register float *float_im;
  register BinValueType *bin_size;
  register BinIndexType *q_im;
  int im_size;
  {
  register int i;
  register double the_bin_size;
  register double ftemp, max_abs = 0.0;
  
  /* find maximum abs of image */
  for (i=0; i<im_size; i++)
      {
      ftemp = float_im[i];
      if (ftemp < 0.0)       ftemp = -ftemp;
      if (ftemp > max_abs)  max_abs = ftemp;
      }

  
  /* Check for too many bins */
  if ( max_abs >= ((MAX_BINS-2)*(*bin_size)/2) )    
      { 
      i = (int) *bin_size;
      *bin_size = (BinValueType) ( 2*max_abs/(MAX_BINS-2) + 0.5 );
      printf("QUANTIZE: bin_size %d is too small. Changed to %d.\n",
	     i, (*bin_size));
      }
  the_bin_size = (double) *bin_size;

/*  printf("Max_bin_index: %f\n", max_abs/the_bin_size);  */
  
  for (i=0; i<im_size; i++)
      {
      ftemp = float_im[i] / the_bin_size;
      q_im[i] = (BinIndexType) ((ftemp<0.0)?(ftemp-0.5):(ftemp+0.5));
      }
  }

/*
==========================================================================
unquantize_pyr() -- multiplies the values of q_pyr by the bin_size.
==========================================================================
*/

unquantize_pyr(q_pyr, pyr, im_size, num_levels, bin_size)
  BinIndexType *q_pyr;
  int *pyr, im_size, num_levels;
  BinValueType *bin_size;
  {
  int level, bin_index, im_num, pyr_offset = 0;

  for (im_size>>=(num_levels*2), level=num_levels, bin_index=0;
       level > 0;
       level--, im_size<<=2)
    for (im_num=((level == num_levels) ? 0 : 1); 
	 im_num < 4;
	 im_num++, pyr_offset+=im_size, bin_index++)
	{
	/* printf ("Unquantizing level %d, image %d\n", level, im_num); */
	unquantize_image((q_pyr+pyr_offset), (pyr+pyr_offset), im_size, 
			 bin_size[bin_index]);
	}
  }

/* Hack: best estimator tends to be a bit less than the middle of the bin. 
   so subtract (Est_correction * binsize).  THis number should be in [0,0.5) */
#define EST_CORRECTION 0.18

unquantize_image(q_im, res, im_size, bin_size)
  register BinIndexType *q_im;
  register int *res, im_size;
  register BinValueType bin_size;
  {
  register int i;
  /* 0.5 is for proper rounding on coercion to int! */
  register float correction = bin_size * EST_CORRECTION - 0.5;

  for (i=0; i<im_size; i++)  
      {
      if (q_im[i] >= 1)
        res[i] = (int) (q_im[i] * bin_size - correction);
      else if (q_im[i] <= -1)
	res[i] = (int) (q_im[i] * bin_size + correction);
      else 
	res[i] = 0;
      }
  }

/*
main(argc, argv)
  int argc;
  char *argv[];
  {
  FILE *fp;
  BinIndexType *q_im;
  float *image;
  int num_bins, ctr_bin_offset;
  BinValueType *bin_vals;
  int *res;
  
  image = (float *) check_malloc(256*256*sizeof(*image));
  q_im = (BinIndexType *) check_malloc(256*256*sizeof(*q_im));
  res = (int *) check_malloc(256*256*sizeof(*res));

  fp = check_fopen(argv[1], "r");
  read_byte_image(fp, image, 256, 256);
  fclose (fp);

  bin_vals = (BinValueType *) check_malloc( MAX_BINS*sizeof(*bin_vals) );

  quantize_image (image, q_im, 65536, 10.0, bin_vals, &num_bins, &ctr_bin_offset);

  unquantize_image (q_im, res, 65536, bin_vals, ctr_bin_offset);

  printf("Writing result/data...\n");
  fp = check_fopen("result/data", "w");
  write_array(res, 65536, fp);
  fclose(fp);
  }
*/

/*
cc -o quantize +x quantize.c utilities.c fileio.c -lm
*/
