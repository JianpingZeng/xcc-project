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
collapse_pyr() -- collapse (reconstruct) a QMF-style pyramid using a
3-tap (1-2-1) filter.  Assumes the pyramid was built with correct
inverse filters, and reflected edge treatment (reflect1).  Assumes
storage order of images: LN, HN, VN, DN, HN-1, VN-1, .... D1.  
WARNING: The contents of the pyramid are destroyed!  Both dimensions 
of the image must be divisible by 2^num_levels (the routine
does not check this).
=======================================================================
*/

collapse_pyr(pyr, result, x_size, y_size, num_levels)
  int *pyr, x_size, y_size, num_levels;
  register int *result;
  {
  register int val, rpos, row_rpos;
  register int *im;
  register int rxsize, rysize, /* Size of resulting lowpass image */
               ixsize, iysize, /* Size of starting bandpass images */
               ipos;           /* Where we are in bandpass images */
  register int ccount, lcount;
  register int rx2size;        /* Size of 2*rxsize */
  int level;

  for (level = num_levels-1; level >= 0; level--)
      {
      rxsize = x_size >> level;	/* divide by 2^level */
      rysize = y_size >> level;
      rx2size = rxsize << 1;      /* mul by 2 */
      ixsize = rxsize >> 1;
      iysize = rysize >> 1;

      for (rpos=0; rpos<rxsize*rysize; rpos++) result[rpos] = 0;

      im = pyr;			/***** lowpass  image ******/
      for (ipos=ixsize+1, rpos=rx2size+2, lcount=1;
	   lcount < iysize;  ipos++, rpos+=rxsize+2, lcount++)
	for (ccount = 1;  ccount < ixsize;  ipos++, rpos+=2, ccount++)
	    {
	    if (val = im[ipos])
		{
		result[rpos]                  += val;
		result[rpos-1]                += (val >>= 1); 
		result[rpos+1]                += val;
		result[row_rpos = rpos-rxsize] += val;
		result[row_rpos-1]            += (val >>= 1); 
		result[row_rpos+1]            += val;
		result[row_rpos = rpos+rxsize] += (val << 1);
		result[row_rpos-1]            += val;
		result[row_rpos+1]            += val;
		}
	    }
      /* top, low */
      for (ipos=1, rpos=2, ccount=1; 
	   ccount<ixsize; ipos++, rpos+=2, ccount++)
	  {
	  if (val = im[ipos])
	      {
	      result[rpos]        += val;
	      result[rpos-1]      += (val >>= 1);
	      result[rpos+1]      += val;
	      result[row_rpos = rpos+rxsize]    += val;
	      result[row_rpos-1]  += (val >>= 1);
	      result[row_rpos+1]  += val;
	      }
	  }
      /* left, low */
      for (ipos=ixsize, rpos=rx2size, ccount=1; 
	   ccount<iysize; ipos+=ixsize, rpos+=rx2size, ccount++)
	  {
	  if (val = im[ipos])
	      {
	      result[rpos]        += val;
	      result[rpos+1]      += (val >>= 1);
	      result[row_rpos=rpos-rxsize]  += val;
	      result[row_rpos+1] += (val >> 1);
	      result[row_rpos=rpos+rxsize]  += val;
	      result[row_rpos+1] += (val >> 1);
	      }
	  }
      /* bottom, low */
      for (ipos=ixsize*(iysize-1)+1, rpos=rxsize*(rysize-1)+2, ccount=1; 
	   ccount<ixsize; ipos++, rpos+=2, ccount++)
	  {
	  if (val = (im[ipos] >> 1))
	      {
	      result[rpos]   += val;
	      result[rpos-1] += (val >>= 1);
	      result[rpos+1] += val;
	      }
	  }
      /* right, low */
      for (ipos=(ixsize<<1)-1, rpos=rx2size+rxsize-1, ccount=1; 
	   ccount<iysize; ipos+=ixsize, rpos+=rx2size, ccount++)
	  {
	  if (val = (im[ipos] >> 1))
	      {
	      result[rpos] += val;
	      result[rpos-rxsize] += (val >>= 1);
	      result[rpos+rxsize] += val;
	      }
	  }
      if (val = im[0])
	  {
	  result[0] += val;  
	  result[1] += (val >>= 1); 
	  result[rxsize] += val;
	  result[rxsize+1] += (val >> 1);
	  }
      if (val = (im[ixsize-1] >> 1))
	  {
	  result[rxsize-1] += val;  
	  result[rx2size-1] += val >> 1;
	  }
      if (val = (im[ixsize*(iysize-1)] >> 1))
	  {
	  result[rxsize*(rysize-1)] += val;  
	  result[rxsize*(rysize-1)+1] += val>>1;
	  }
      result[rxsize*rysize-1] += im[ixsize*iysize-1] >> 2;   
      im = pyr+(2*ixsize*iysize);	/****** vertical image ******/
      internal_int_transpose(im, ixsize, iysize);   /* transpose vertical image back */
      for (ipos=ixsize, rpos=(rx2size)+1, lcount = 1;
	   lcount < iysize;  ipos+=1, rpos+=rxsize+2, lcount++)
	for (ccount = 1;  ccount < ixsize;  ipos++, rpos+=2, ccount++)
	    {
	    if (val = im[ipos])
		{
		result[rpos]       += val;
		result[rpos-1]     -= (val >>= 1); /* mul by 1/2 */
		result[rpos+1]     -= val;
		result[row_rpos = rpos-rxsize]   += val;
		result[row_rpos-1] -= (val >>= 1); /* mul by 1/2 */
		result[row_rpos+1] -= val;
		result[row_rpos = rpos+rxsize]   += (val << 1);
		result[row_rpos-1] -= val;
		result[row_rpos+1] -= val;
		}
	    }
      for (ipos=0, rpos=1, ccount=1; /* top, vertical */
	   ccount<ixsize; ipos++, rpos+=2, ccount++)
	  {
	  if (val = im[ipos])
	      {
	      result[rpos]        += val;
	      result[rpos-1]      -= (val >>= 1);
	      result[rpos+1]      -= val;
	      result[row_rpos = rpos+rxsize]    += val;
	      result[row_rpos-1]  -= (val >>= 1);
	      result[row_rpos+1]  -= val;
	      }
	  }
      for (ipos=(ixsize<<1)-1, rpos=rx2size+rxsize-1, ccount=1; /* right, vertical */
	   ccount<iysize; ipos+=ixsize, rpos+=rx2size, ccount++)
	  {
	  if (val = im[ipos])
	      {
	      result[rpos]        += val;
	      result[rpos-1]      -= (val >>= 1);
	      result[row_rpos=rpos-rxsize]  += val;
	      result[row_rpos-1] -= (val >> 1);
	      result[row_rpos=rpos+rxsize]  += val;
	      result[row_rpos-1] -= (val >> 1);
	      }
	  }
      for (ipos=ixsize*(iysize-1), rpos=rxsize*(rysize-1)+1, ccount=1;  /* bottom, vert */
	   ccount<ixsize; ipos++, rpos+=2, ccount++)
	  {
	  if (val = (im[ipos] >> 1))
		{
		result[rpos]   += val;
		result[rpos-1] -= (val >>= 1);
		result[rpos+1] -= val;
		}
	  }
      for (ipos=ixsize, rpos=rx2size, ccount=1; /* left, vertical */ 
	   ccount<iysize; ipos+=ixsize, rpos+=rx2size, ccount++)
	  {
	  val = - (im[ipos] >> 1);
	  if (val)
	      {
	      result[rpos] += val;
	      result[rpos-rxsize] += (val >>= 1);
	      result[rpos+rxsize] += val;
	      }
	  }
      if (val = - (im[0] >> 1))
	  {
	  result[0] += val;  
	  result[rxsize] += val >> 1;
	  }
      if (val = im[ixsize-1])
	  {
	  result[rxsize-1] += val;  
	  result[rxsize-2] -= (val >>= 1);
	  result[rx2size-1] += val;
	  result[rx2size-2] -= val >> 1;
	  }
      result[rxsize*(rysize-1)] -= im[ixsize*(iysize-1)] >> 2;
      if (val = (im[ixsize*iysize-1] >> 1))
	  {
	  result[rxsize*rysize-1] += val;
	  result[rxsize*rysize-2] -= val >> 1;
	  }
      im = pyr+(ixsize*iysize);	/***** horizontal  image *****/  
      for (ipos=1, rpos=rxsize+2, lcount = 1;
	   lcount < iysize;  ipos+=1, rpos+=rxsize+2, lcount++)
	for (ccount = 1;  ccount < ixsize;  ipos++, rpos+=2, ccount++)
	    {
	    if (val = im[ipos])
		{
		result[rpos]       += val;
		result[rpos-1]     += (val >>= 1); /* mul by 1/2 */
		result[rpos+1]     += val;
		result[row_rpos = rpos-rxsize]   -= val;
		result[row_rpos-1] -= (val >>= 1); /* mul by 1/2 */
		result[row_rpos+1] -= val;
		result[row_rpos = rpos+rxsize]   -= (val << 1);
		result[row_rpos-1] -= val;
		result[row_rpos+1] -= val;
		}
	    }
      for (ipos=1, rpos=2, ccount=1; /* top, horizontal */
	   ccount<ixsize; ipos++, rpos+=2, ccount++)
	  {
	  if (val = - (im[ipos] >> 1))
	      {
	      result[rpos]        += val;
	      result[rpos-1]      += (val >>= 1);
	      result[rpos+1]      += val;
	      }
	  }
      for (ipos=0, rpos=rxsize, ccount=1; /* left, horizontal */
	   ccount<iysize; ipos+=ixsize, rpos+=rx2size, ccount++)
	  {
	  if (val = im[ipos])
	      {
	      result[rpos]        += val;
	      result[rpos+1]      += (val >>= 1);
	      result[row_rpos = rpos-rxsize]  -= val;
	      result[row_rpos+1] -= (val >> 1);
	      result[row_rpos = rpos+rxsize]  -= val;
	      result[row_rpos+1] -= (val >> 1);
	      }
	  }
      for (ipos=ixsize*(iysize-1)+1, rpos=rxsize*(rysize-1)+2, ccount=1; /* bottom, hor */
	   ccount<ixsize; ipos++, rpos+=2, ccount++)
	  {
	  if (val = im[ipos])
	      {
	      result[rpos]   += val;
	      result[rpos-1] += (val >>= 1);
	      result[rpos+1] += val;
	      result[row_rpos = rpos-rxsize] -= val;
	      result[row_rpos+1] -= (val >>= 1);
	      result[row_rpos-1] -= val;
	      }
	  }
      for (ipos=ixsize-1, rpos=rx2size-1, ccount=1; /* right, horizontal */
	   ccount<iysize; ipos+=ixsize, rpos+=rx2size, ccount++)
	  {
	  if (val = (im[ipos] >> 1))
	      {
	      result[rpos] += val;
	      result[rpos-rxsize] -= (val >>= 1);
	      result[rpos+rxsize] -= val;
	      }
	  }
      if (val = - (im[0] >> 1))
	  {
	  result[0] += val;  
	  result[1] += val >> 1; 
	  }
      result[rxsize-1] -= im[ixsize-1] >> 2;
      if (val = im[ixsize*(iysize-1)])
	  {
	  result[rxsize*(rysize-1)] += val;  
	  result[rxsize*(rysize-1)+1] += (val >>= 1);
	  result[rxsize*(rysize-2)] -= val;
	  result[rxsize*(rysize-2)+1] -= val >> 1;
	  }
      if (val = (im[ixsize*iysize-1] >> 1))
	  {
	  result[rxsize*rysize-1] += val;
	  result[rxsize*(rysize-1)-1] -= val >> 1; 
	  }
      im = pyr+(3*ixsize*iysize);	/*****  diagonal  image ******/
      for (ipos=0, rpos=rxsize+1, lcount = 1;
	   lcount < iysize;  ipos+=1, rpos+=rxsize+2, lcount++)
	for (ccount = 1;  ccount < ixsize;  ipos++, rpos+=2, ccount++)
	    {
	    if (val = im[ipos])
		{
		result[rpos]       += val;
		result[rpos-1]     -= (val >>= 1); /* mul by 1/2 */
		result[rpos+1]     -= val;
		result[row_rpos=rpos-rxsize]   -= val;
		result[row_rpos-1] += (val >>= 1); /* mul by 1/2 */
		result[row_rpos+1] += val;
		result[row_rpos=rpos+rxsize]   -= (val << 1);
		result[row_rpos-1] += val;
		result[row_rpos+1] += val;
		}
	    }
      for (ipos=0, rpos=1, ccount=1; /* top, diag */
	   ccount<ixsize; ipos++, rpos+=2, ccount++)
	  {
	  if (val = (im[ipos] >> 1))
	      {
	      result[rpos]        -= val;
	      result[rpos-1]      += (val >>= 1);
	      result[rpos+1]      += val;
	      }
	  }
      for (ipos=0, rpos=rxsize, ccount=1; /* left, diag */
	   ccount<iysize; ipos+=ixsize, rpos+=rx2size, ccount++)
	  {
	  if (val = (im[ipos] >> 1))
	      {
	      result[rpos]        -= val;
	      result[rpos+rxsize]  += (val >>= 1);
	      result[rpos-rxsize]  += val;
	      }
	  }
      for (ipos=ixsize*(iysize-1), rpos=rxsize*(rysize-1)+1, ccount=1; /* bottom, diag */
	   ccount<ixsize; ipos++, rpos+=2, ccount++)
	  {
	  if (val = im[ipos])
	      {
	      result[rpos]   += val;
	      result[rpos-1] -= (val >>= 1);
	      result[rpos+1] -= val;
	      result[row_rpos=rpos-rxsize] -= val;
	      result[row_rpos+1] += (val >>= 1);
	      result[row_rpos-1] += val;
	      }
	  }
      for (ipos=ixsize-1, rpos=rx2size-1, ccount=1; /* right diag */
	   ccount<iysize; ipos+=ixsize, rpos+=rx2size, ccount++)
	  {
	  if (val = im[ipos])
	      {
	      result[rpos] += val;
	      result[rpos-1] -= (val >>= 1);
	      result[row_rpos=rpos-rxsize] -= val;
	      result[row_rpos-1] += val >> 1;
	      result[row_rpos=rpos+rxsize] -= val;
	      result[row_rpos-1] += val >> 1;
	      }
	  }
      /* val = im[0] >> 2; */
      result[0] +=  (im[0] >> 2);  
      if (val = (im[ixsize-1] >> 1))
	  {
	  result[rxsize-1] -= val;
	  result[rxsize-2] += val >> 1;
	  }
      if (val = (im[ixsize*(iysize-1)] >> 1))
	  {
	  result[rxsize*(rysize-1)] -= val;  
	  result[rxsize*(rysize-2)] += val >> 1;
	  }
      if (val = im[ixsize*iysize-1])
	  {
	  result[row_rpos=rxsize*rysize-1] += val;
	  result[row_rpos-1] -= (val >>= 1); 
	  result[row_rpos-rxsize] -= val;
	  result[row_rpos-rxsize-1] += val >> 1;
	  }

      if (level > 0)		/* copy result into pyramid */
	for(im=pyr, rpos=0; rpos<rxsize*rysize; rpos++)
	  im[rpos] = result[rpos];
      }				/* end for each level */
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
