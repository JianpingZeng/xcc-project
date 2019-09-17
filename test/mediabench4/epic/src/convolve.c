/* 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;  File: convolve.c
;;;  Author: Eero Simoncelli
;;;  Description: General convolution code for 2D images
;;;  Creation Date: Spring, 1987.
;;;  ----------------------------------------------------------------
;;;    Object-Based Vision and Image Understanding System (OBVIUS),
;;;      Copyright 1988, Vision Science Group,  Media Laboratory,  
;;;              Massachusetts Institute of Technology.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
*/

#include <stdio.h>
#include <math.h>
#include "convolve.h"

/* --------------------------------------------------------------------
  Correlate FILT with IMAGE, subsampling according to GRID parameters,
  with values placed into result array.  TEMP is a temporary
  array the size of the filter.  EDGES is a string -- see convolve.h.
  The convolution is done in 9 sections, where the border sections use
  specially computed edge-handling filters (see edges.c). The origin 
  of the filter is assumed to be (floor(x_fdim/2), floor(y_fdim/2)).
  10/6/89 - approximately optimized the choice of register vars on SPARCS.
------------------------------------------------------------------------ */

internal_filter(image, x_dim, y_dim, filt, temp, x_fdim, y_fdim,
		xgrid_start,xgrid_step,ygrid_start,ygrid_step,result,edges)
  register float *image, *temp;
  register int x_fdim, x_dim;
  register int xgrid_step;
  register float *result;
  int ygrid_step;
  int xgrid_start, ygrid_start;
  float *filt; 
  int y_dim, y_fdim;
  char *edges;
  { 
  register double sum;
  register int x_filt, im_pos, y_filt_lin;
  register int y_im_lin, x_pos, filt_size = x_fdim*y_fdim;
  register int y_pos, res_pos;
  register int last_ctr_col = x_dim - x_fdim;
  int last_ctr_row = (y_dim - y_fdim) * x_dim;
  int first_row, first_col;
  int x_fmid = x_fdim/2;
  int y_fmid = y_fdim/2;
  int x_stop = x_fdim - x_fmid + 1;
  int y_stop = y_fdim - y_fmid + 1;
  int ygrid_step_full = ygrid_step*x_dim;
  int prev_res_pos, x_res_dim = (x_dim-xgrid_start+xgrid_step-1)/xgrid_step;
  int rt_edge_res_pos;
  fptr reflect = edge_function(edges);  /* look up edge-handling function */
  if (!reflect) 
      {
      fprintf(stderr,"Unknown edge handler: %s\n",edges);
      return(-1);
      }
  
  for (y_pos=ygrid_start-y_fmid-1,res_pos=0;    /* top */
       y_pos<0;
       y_pos+=ygrid_step)
      {
      for (x_pos=xgrid_start-x_fmid-1;        /* top-left corner */
	   x_pos<0;
	   x_pos+=xgrid_step,res_pos++)
	  {
	  (*reflect)(filt,x_fdim,y_fdim,x_pos,y_pos,temp,FILTER);
	  sum=0.0;
	  for (y_filt_lin=x_fdim,x_filt=y_im_lin=0;
	       y_filt_lin<=filt_size;
	       y_im_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (im_pos=y_im_lin;
		 x_filt<y_filt_lin;
		 x_filt++,im_pos++)
	      sum+=image[im_pos]*temp[x_filt];
	  result[res_pos] = sum;
	  }
      first_col = x_pos+1;
      (*reflect)(filt,x_fdim,y_fdim,0,y_pos,temp,FILTER);
      for (x_pos=first_col;	            /* top edge */
	   x_pos<last_ctr_col;
	   x_pos+=xgrid_step,res_pos++) 
	  {
	  sum=0.0;
	  for (y_filt_lin=x_fdim,x_filt=y_im_lin=0;
	       y_filt_lin<=filt_size;
	       y_im_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (im_pos=x_pos+y_im_lin;
		 x_filt<y_filt_lin;
		 x_filt++,im_pos++)
	      sum+=image[im_pos]*temp[x_filt];
	  result[res_pos] = sum;
	  }
      rt_edge_res_pos = res_pos + x_res_dim;   /* save this for later ... */
      for (x_pos+=(1-last_ctr_col);         /* top-right corner */
	   x_pos<x_stop;
	   x_pos+=xgrid_step,res_pos++) 
	  {
	  (*reflect)(filt,x_fdim,y_fdim,x_pos,y_pos,temp,FILTER);
	  sum=0.0;
	  for (y_filt_lin=x_fdim,x_filt=y_im_lin=0;
	       y_filt_lin<=filt_size;
	       y_im_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (im_pos=y_im_lin+last_ctr_col;
		 x_filt<y_filt_lin;
		 x_filt++,im_pos++)
	      sum+=image[im_pos]*temp[x_filt];
	  result[res_pos] = sum;
	  }
      }                        /* end top */   

  first_row = x_dim*(y_pos+1);   /* need this to go down the sides */
  prev_res_pos = res_pos;
  for (x_pos=xgrid_start-x_fmid-1;           /* left edge */
       x_pos<0;
       x_pos+=xgrid_step)
      {
      res_pos = prev_res_pos;
      (*reflect)(filt,x_fdim,y_fdim,x_pos,0,temp,FILTER);
      for (y_pos=first_row;
	   y_pos<last_ctr_row;
	   y_pos+=ygrid_step_full, res_pos+=x_res_dim)
	  {
	  sum=0.0;
	  for (y_filt_lin=x_fdim,x_filt=0,y_im_lin=y_pos;
	       y_filt_lin<=filt_size;
	       y_im_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (im_pos=y_im_lin;
		 x_filt<y_filt_lin;
		 x_filt++,im_pos++)
	      sum+=image[im_pos]*temp[x_filt];
	  result[res_pos] = sum;
	  }
      prev_res_pos++;
      }
  (*reflect)(filt,x_fdim,y_fdim,0,0,temp,FILTER);
  for (y_pos=first_row;		/* center region of image */
       y_pos<last_ctr_row;
       y_pos+=ygrid_step_full)
      {
      res_pos = prev_res_pos;
      for (x_pos=first_col;
	   x_pos<last_ctr_col;
	   x_pos+=xgrid_step,res_pos++) 
	  {
	  sum=0.0;
	  for (y_filt_lin=x_fdim,x_filt=0,y_im_lin=y_pos;
	       y_filt_lin<=filt_size;
	       y_im_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (im_pos=x_pos+y_im_lin;
		 x_filt<y_filt_lin;
		 x_filt++,im_pos++)
	      sum+=image[im_pos]*temp[x_filt];
	  result[res_pos] = sum;
	  }
      prev_res_pos+=x_res_dim;
      }
  prev_res_pos = rt_edge_res_pos;
  for (x_pos+=(1-last_ctr_col);                  /* right edge */
       x_pos<x_stop;
       x_pos+=xgrid_step) 
      {
      res_pos = prev_res_pos;
      (*reflect)(filt,x_fdim,y_fdim,x_pos,0,temp,FILTER);
      for (y_pos=first_row;
	   y_pos<last_ctr_row;
	   y_pos+=ygrid_step_full, res_pos+=x_res_dim)
	  {
	  sum=0.0;
	  for (y_filt_lin=x_fdim,x_filt=0,y_im_lin=y_pos;
	       y_filt_lin<=filt_size;
	       y_im_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (im_pos=y_im_lin+last_ctr_col;
		 x_filt<y_filt_lin;
		 x_filt++,im_pos++)
	      sum+=image[im_pos]*temp[x_filt];
	  result[res_pos] = sum;
	  }
      prev_res_pos++;
      }				/* end mid */
  
  res_pos -= (x_res_dim - 1);            /* go to lower left corner */
  for (y_pos=((y_pos-last_ctr_row)/x_dim)+1;     	/* bottom */
       y_pos<y_stop;
       y_pos+=ygrid_step) 
      {
      for (x_pos=xgrid_start-x_fmid-1;         /* bottom-left corner */
	   x_pos<0;
	   x_pos+=xgrid_step,res_pos++)
	  {
	  (*reflect)(filt,x_fdim,y_fdim,x_pos,y_pos,temp,FILTER);
	  sum=0.0;
	  for (y_filt_lin=x_fdim,x_filt=0,y_im_lin=last_ctr_row;
	       y_filt_lin<=filt_size;
	       y_im_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (im_pos=y_im_lin;
		 x_filt<y_filt_lin;
		 x_filt++,im_pos++)
	      sum+=image[im_pos]*temp[x_filt];
	  result[res_pos] = sum;
	  }
      (*reflect)(filt,x_fdim,y_fdim,0,y_pos,temp,FILTER);
      for (x_pos=first_col;		        /* bottom edge */
	   x_pos<last_ctr_col;
	   x_pos+=xgrid_step,res_pos++) 
	  {
	  sum=0.0;
	  for (y_filt_lin=x_fdim,x_filt=0,y_im_lin=last_ctr_row;
	       y_filt_lin<=filt_size;
	       y_im_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (im_pos=x_pos+y_im_lin;
		 x_filt<y_filt_lin;
		 x_filt++,im_pos++)
	      sum+=image[im_pos]*temp[x_filt];
	  result[res_pos] = sum;
	  }
      for (x_pos+=1-last_ctr_col;	/* bottom-right corner */
	   x_pos<x_stop;
	   x_pos+=xgrid_step,res_pos++) 
	  {
	  (*reflect)(filt,x_fdim,y_fdim,x_pos,y_pos,temp,FILTER);
	  sum=0.0;
	  for (y_filt_lin=x_fdim,x_filt=0,y_im_lin=last_ctr_row;
	       y_filt_lin<=filt_size;
	       y_im_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (im_pos=y_im_lin+last_ctr_col;
		 x_filt<y_filt_lin;
		 x_filt++,im_pos++)
	      sum+=image[im_pos]*temp[x_filt];
	  result[res_pos] = sum;
	  }
      }				/* end bottom */
  }			/* end of internal_filter */


/* --------------------------------------------------------------------

  Upsample image according to grid parameters and then convolve with
  filt, adding values into result array.

  WARNING: this subroutine ADDS the expanded image into the result, so
  the user must zero the result before invocation! 

------------------------------------------------------------------------ */	 

internal_expand(image,filt,temp,x_fdim,y_fdim,xgrid_start,xgrid_step,
		  ygrid_start,ygrid_step,result,x_dim,y_dim,edges)
  register float *result, *temp;
  register int x_fdim, x_dim;
  register int xgrid_step;
  register float *image; 
  int ygrid_step;
  int xgrid_start, ygrid_start;
  float *filt; 
  int y_fdim, y_dim;
  char *edges;
  {
  register double val;
  register int x_filt, res_pos, y_filt_lin;
  register int y_res_lin, x_pos, filt_size = x_fdim*y_fdim;
  register int y_pos, im_pos;
  register int last_ctr_col = x_dim - x_fdim;
  int last_ctr_row = (y_dim - y_fdim) * x_dim;
  int first_col, first_row;
  int x_fmid = x_fdim/2;
  int y_fmid = y_fdim/2;
  int x_stop = x_fdim - x_fmid + 1;
  int y_stop = y_fdim - y_fmid + 1;
  int ygrid_step_full = ygrid_step*x_dim;
  int prev_im_pos, x_im_dim = (x_dim-xgrid_start+xgrid_step-1)/xgrid_step;
  int rt_edge_im_pos;
  fptr reflect = edge_function(edges);	 
  if (!reflect) 
      {
      fprintf(stderr,"Unknown edge handler: %s\n",edges);
      return(-1);
      }

  for (y_pos=ygrid_start-y_fmid-1,im_pos=0; /* top */
       y_pos<0;
       y_pos+=ygrid_step)
      {
      for (x_pos=xgrid_start-x_fmid-1; /* upper-left corner */
	   x_pos<0;
	   x_pos+=xgrid_step,im_pos++)
	  {
	  (*reflect)(filt,x_fdim,y_fdim,x_pos,y_pos,temp,EXPAND);
	  val = image[im_pos];
	  for (y_filt_lin=y_res_lin=0;
	       y_filt_lin<filt_size;
	       y_res_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (res_pos=y_res_lin,x_filt=y_filt_lin;
		 x_filt<y_filt_lin+x_fdim;
		 x_filt++,res_pos++)
	      result[res_pos] += val*temp[x_filt];
	  }
      first_col = x_pos+1;
      (*reflect)(filt,x_fdim,y_fdim,0,y_pos,temp,EXPAND);
      for (x_pos=first_col;		/* top edge */
	   x_pos<last_ctr_col;
	   x_pos+=xgrid_step,im_pos++) 
	  {
	  val = image[im_pos];
	  for (y_filt_lin=y_res_lin=0;
	       y_filt_lin<filt_size;
	       y_res_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (res_pos=x_pos+y_res_lin,x_filt=y_filt_lin;
		 x_filt<y_filt_lin+x_fdim;
		 x_filt++,res_pos++)
	      result[res_pos]  += val*temp[x_filt];
	  }
      rt_edge_im_pos = im_pos + x_im_dim;
      for (x_pos+=(1-last_ctr_col);	/* upper-right corner */
	   x_pos<x_stop;
	   x_pos+=xgrid_step,im_pos++) 
	  {
	  (*reflect)(filt,x_fdim,y_fdim,x_pos,y_pos,temp,EXPAND);
	  val = image[im_pos];		      
	  for (y_filt_lin=y_res_lin=0;
	       y_filt_lin<filt_size;
	       y_res_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (res_pos=y_res_lin+last_ctr_col,x_filt=y_filt_lin;
		 x_filt<y_filt_lin+x_fdim;
		 x_filt++,res_pos++)
	      result[res_pos] += val*temp[x_filt];
	  }
      }				    /* end top */   
  
  first_row = x_dim*(y_pos+1);      /* need this to go down sides */
  prev_im_pos = im_pos;
  for (x_pos=xgrid_start-x_fmid-1;  /* left edge */
       x_pos<0;
       x_pos+=xgrid_step)
      {
      im_pos = prev_im_pos;
      (*reflect)(filt,x_fdim,y_fdim,x_pos,0,temp,EXPAND);
      for (y_pos=first_row;
	   y_pos<last_ctr_row;                
	   y_pos+=ygrid_step_full, im_pos+=x_im_dim)
	  {
	  val = image[im_pos];
	  for (y_filt_lin=0,y_res_lin=y_pos;
	       y_filt_lin<filt_size;
	       y_res_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (res_pos=y_res_lin,x_filt=y_filt_lin;
		 x_filt<y_filt_lin+x_fdim;
		 x_filt++,res_pos++)
	      result[res_pos] += val*temp[x_filt];
	  }
      prev_im_pos++;
      }
  (*reflect)(filt,x_fdim,y_fdim,0,0,temp,EXPAND);
  for (y_pos=first_row;		/* center region of image */
       y_pos<last_ctr_row;
       y_pos+=ygrid_step_full)
      {
      im_pos = prev_im_pos;
      for (x_pos=first_col;
	   x_pos<last_ctr_col;
	   x_pos+=xgrid_step,im_pos++) 
	  {
	  val = image[im_pos];
	  for (y_filt_lin=0,y_res_lin=y_pos;
	       y_filt_lin<filt_size;
	       y_res_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (res_pos=x_pos+y_res_lin,x_filt=y_filt_lin;
		 x_filt<y_filt_lin+x_fdim;
		 x_filt++,res_pos++)
	      result[res_pos]  += val*temp[x_filt];
	  }
      prev_im_pos+=x_im_dim;
      }
  prev_im_pos = rt_edge_im_pos;
  for (x_pos+=(1-last_ctr_col);	/* right edge */
       x_pos<x_stop;
       x_pos+=xgrid_step)
      {
      im_pos = prev_im_pos;
      (*reflect)(filt,x_fdim,y_fdim,x_pos,0,temp,EXPAND);
      for (y_pos=first_row;
	   y_pos<last_ctr_row;
	   y_pos+=ygrid_step_full, im_pos+=x_im_dim)
	  {
	  val = image[im_pos];		      
	  for (y_filt_lin=0,y_res_lin=y_pos;
	       y_filt_lin<filt_size;
	       y_res_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (res_pos=y_res_lin+last_ctr_col,x_filt=y_filt_lin;
		 x_filt<y_filt_lin+x_fdim;
		 x_filt++,res_pos++)
	      result[res_pos] += val*temp[x_filt];
	  }
      prev_im_pos++;
      }				/* end mid */
  
  im_pos -= (x_im_dim -1);
  for (y_pos=((y_pos-last_ctr_row)/x_dim)+1;
       y_pos<y_stop;		/* bottom */
       y_pos+=ygrid_step) 
      {
      for (x_pos=xgrid_start-x_fmid-1; 
	   x_pos<0;
	   x_pos+=xgrid_step,im_pos++)
	  {
	  (*reflect)(filt,x_fdim,y_fdim,x_pos,y_pos,temp,EXPAND);
	  val = image[im_pos];
	  for (y_filt_lin=0,y_res_lin=last_ctr_row;
	       y_filt_lin<filt_size;
	       y_res_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (res_pos=y_res_lin,x_filt=y_filt_lin;
		 x_filt<y_filt_lin+x_fdim;
		 x_filt++,res_pos++)
	      result[res_pos] += val*temp[x_filt];
	  }
      (*reflect)(filt,x_fdim,y_fdim,0,y_pos,temp,EXPAND);
      for (x_pos=first_col;
	   x_pos<last_ctr_col;
	   x_pos+=xgrid_step,im_pos++) 
	  {
	  val = image[im_pos];
	  for (y_filt_lin=0, y_res_lin=last_ctr_row;
	       y_filt_lin<filt_size;
	       y_res_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (res_pos=x_pos+y_res_lin,x_filt=y_filt_lin;
		 x_filt<y_filt_lin+x_fdim;
		 x_filt++,res_pos++)
	      result[res_pos]  += val*temp[x_filt];
	  }
      for (x_pos+=1-last_ctr_col;
	   x_pos<x_stop;
	   x_pos+=xgrid_step,im_pos++) 
	  {
	  (*reflect)(filt,x_fdim,y_fdim,x_pos,y_pos,temp,EXPAND);
	  val = image[im_pos];		      
	  for (y_filt_lin=0,y_res_lin=last_ctr_row;
	       y_filt_lin<filt_size;
	       y_res_lin+=x_dim,y_filt_lin+=x_fdim)
	    for (res_pos=y_res_lin+last_ctr_col,x_filt=y_filt_lin;
		 x_filt<y_filt_lin+x_fdim;
		 x_filt++,res_pos++)
	      result[res_pos] += val*temp[x_filt];
	  }
      }				/* end bottom */
  }				/* end of internal_expand */
  

/* Local Variables: */
/* buffer-read-only: t */
/* End: */

