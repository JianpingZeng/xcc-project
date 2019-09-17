/* 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;  File: edges.c
;;;  Author: Simoncelli
;;;  Description: Edge handling routines for use with convolve.c
;;;  Creation Date:
;;;  ----------------------------------------------------------------
;;;    Object-Based Vision and Image Understanding System (OBVIUS),
;;;      Copyright 1988, Vision Science Group,  Media Laboratory,  
;;;              Massachusetts Institute of Technology.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
*/

/* 
This file contains functions which determine how edges are to be
handled when performing convolutions of images with linear filters.
Any edge handling function which is local and linear may be defined,
except (unfortunately) constants cannot be added.  So to treat the
edges as if the image is surrounded by a gray field, you must paste it
into a gray image, convolve, and crop it out...
The main convolution function is called internal_filter and is defined
in the file convolve.c.  The idea is that the convolution function
calls the edge handling function which computes a new filter based on
the old filter and the distance to the edge of the image.  For
example, reflection is done by reflecting the filter through the
appropriate axis and summing.  Currently defined functions are listed
below.  
*/

#include <stdio.h>
#include <math.h>
#include "convolve.h"

#define sgn(a)  ( ((a)>0)?1:(((a)<0)?-1:0) )
#define clip(a,mn,mx)  ( ((a)<(mn))?(mn):(((a)>=(mx))?(mx-1):(a)) )

int reflect1(), reflect2(), repeat(), zero(), extend(), nocompute();
int ereflect(), predict();

static EDGE_HANDLER edge_foos[] =
  {
    { "reflect1", reflect1 },   /* reflect about edge pixels - new name */
    { "reflect2", reflect2 },   /* standard reflection */
    { "repeat",   repeat   },   /* repeat edge pixel */
    { "zero",     zero     },   /* zero outside of image */
    { "extend",   extend   },   /* extend (reflect & invert) */
    { "dont-compute", nocompute }, /* ignore edge (zero output for filter near edge) */
    { "predict",  predict  },   /* predict based on portion covered by filt */
    { "ereflect", ereflect },	/* orthogonal QMF reflection */
    { "treflect", reflect1 }	/* reflect about edge pixels - old name */
  };

/*
Function looks up an edge handler id string in the structure above, and
returns the associated function 
*/

fptr edge_function(edges)
  char *edges;
  {
  int i;
  for (i = 0; i<sizeof(edge_foos)/sizeof(EDGE_HANDLER); i++)
    if (!strcmp(edges,edge_foos[i].name))
      return(edge_foos[i].func);
  printf("No such edge handler routine!\n");
  return(0);
  }

/* 
---------------- EDGE HANDLER ARGUMENTS ------------------------
filt - floating point array of filter taps.
x_dim, y_dim - x and y dimensions of filt.
x_pos - position of filter relative to the horizontal image edges. Negative
       values indicate left edge, positive indicate right edge.  Zero 
       indicates that the filter is not touching either edge.  An absolute
       value of 1 indicates that the edge tap of the filter is over the 
       edge pixel of the image.
y_pos - analogous to x_pos.
result - floating point array where the resulting filter will go.  The edge
       of this filter will be aligned with the image for application...
f_or_e - equal to one of the two constants EXPAND or FILTER.
-------------------------------------------------------------------- 
*/ 


/* --------------------------------------------------------------------
zero() - Zero outside of image.  Discontinuous, but adds zero energy. */

zero(filt,x_dim,y_dim,x_pos,y_pos,result,f_or_e)
  register float *filt, *result;
  register int x_dim;
  int y_dim, x_pos, y_pos, f_or_e;
  {
  register int y_filt,x_filt, y_res,x_res;
  int filt_sz = x_dim*y_dim;
  int x_start = ((x_pos>0)?(x_pos-1):((x_pos<0)?(x_pos+1):0));
  int y_start = x_dim * ((y_pos>0)?(y_pos-1):((y_pos<0)?(y_pos+1):0));
  int i;

  for (i=0; i<filt_sz; i++) result[i] = 0.0;

  for (y_filt=0, y_res=y_start;
       y_filt<filt_sz;
       y_filt+=x_dim, y_res+=x_dim)
      if ((y_res >= 0) AND (y_res < filt_sz))
	for (x_filt=y_filt, x_res=x_start;
	     x_filt<y_filt+x_dim;
	     x_filt++, x_res++)
	  if ((x_res >= 0) AND (x_res < x_dim))
	    result[y_res+x_res] = filt[x_filt];
  }

/* --------------------------------------------------------------------
repeat() - repeat edge pixel.  Continuous, but content is usually
different from image.  
*/

repeat(filt,x_dim,y_dim,x_pos,y_pos,result,f_or_e)
  register float *filt, *result;
  register int x_dim;
  int y_dim, x_pos, y_pos, f_or_e;
  {
  register int y_filt,x_filt, y_res,x_res;
  int filt_sz = x_dim*y_dim;
  int x_start = ((x_pos>0)?(x_pos-1):((x_pos<0)?(x_pos+1):0));
  int y_start = x_dim * ((y_pos>0)?(y_pos-1):((y_pos<0)?(y_pos+1):0));
  int i;

  for (i=0; i<filt_sz; i++) result[i] = 0.0;

  for (y_filt=0, y_res=y_start;
       y_filt<filt_sz;
       y_filt+=x_dim, y_res+=x_dim)
    for (x_filt=y_filt, x_res=x_start;
	 x_filt<y_filt+x_dim;
	 x_filt++, x_res++)
      result[((y_res>=0)?((y_res<filt_sz)?y_res:(filt_sz-x_dim)):0) /* clip y_res */
	     + ((x_res>=0)?((x_res<x_dim)?x_res:(x_dim-1)):0)]      /* clip x_res */
	    += filt[x_filt];
  }

/* --------------------------------------------------------------------
reflect2() - "Normal" image reflection.  The edge pixel is repeated,
then the next pixel, etc.  Continuous, attempting to maintain
"similar" content, but discontinuous first derivative.
*/

reflect2(filt,x_dim,y_dim,x_pos,y_pos,result,f_or_e)
  register float *filt, *result;
  register int x_dim;
  int y_dim, x_pos, y_pos, f_or_e;
  {
  register int y_filt,x_filt, y_edge,x_edge;
  register int x_base = (x_pos>0)?(x_dim-1):0;
  register int y_base = (y_pos>0)?(x_dim*(y_dim-1)):0; 
  int filt_sz = x_dim*y_dim;
  int x_edge_dist = (x_pos>0)?(x_pos-x_dim-1):(x_pos+1);
  int y_edge_dist = x_dim * ((y_pos>0)?(y_pos-y_dim-1):(y_pos+1));
  int i;

  for (i=0; i<filt_sz; i++) result[i] = 0.0;

  /* printf("x_edge_start: %d, x_base: %d\n", x_edge_dist, x_base); */
  for (y_filt=0, y_edge=y_edge_dist;
       y_filt<filt_sz;
       y_filt+=x_dim, y_edge+=x_dim)
      {
      if (y_edge IS 0) y_edge+=x_dim;
      for (x_filt=y_filt, x_edge=x_edge_dist;
	   x_filt<y_filt+x_dim;
	   x_filt++, x_edge++)
	  {
	  if (x_edge IS 0) x_edge++;
	  result[abs(y_base-abs(y_edge)+x_dim) + abs(x_base-abs(x_edge)+1)]
	    += filt[x_filt];
	  }
      }
  }

/* --------------------------------------------------------------------
reflect1() - Reflection through the edge pixels.  This is the right thing
to do if you are subsampling by 2, since it maintains parity (even 
pixels positions remain even, odd ones remain odd). (note: procedure differs 
depending on f_or_e parameter).  */	 

reflect1(filt,x_dim,y_dim,x_pos,y_pos,result,f_or_e)
  register float *filt, *result;
  register int x_dim;
  int y_dim, x_pos, y_pos, f_or_e;
  {
  int filt_sz = x_dim*y_dim;
  register int x_start = 0, y_start = 0, x_stop = x_dim, y_stop = filt_sz;
  register int y_filt,x_filt, y_edge,x_edge;
  register int x_base = (x_pos>0)?(x_dim-1):0;
  register int y_base = (y_pos>0)?(x_dim*(y_dim-1)):0; 
  int x_edge_dist = (x_pos>0)?(x_pos-x_dim):((x_pos<-1)?(x_pos+1):0);
  int y_edge_dist = x_dim * ((y_pos>0)?(y_pos-y_dim):((y_pos<-1)?(y_pos+1):0));
  int i;
  int mx_pos = (x_dim/2)+1;
  int my_pos = (y_dim/2)+1;

  for (i=0; i<filt_sz; i++) result[i] = 0.0;

  /* if EXPAND and filter is centered on image edge, do not reflect */
  if (f_or_e IS EXPAND)
      {
      if (x_pos IS mx_pos) x_stop = (x_dim+1)/2;
      else if (x_pos IS -mx_pos) { x_start = x_dim/2; x_edge_dist = 0; }

      if (y_pos IS my_pos) y_stop = x_dim*((y_dim+1)/2);
      else if (y_pos IS -my_pos) { y_start = x_dim*(y_dim/2); y_edge_dist = 0;}
      }

  /* reflect at boundary of image */
  for (y_filt=y_start, y_edge=y_edge_dist;
       y_filt<y_stop;
       y_filt+=x_dim, y_edge+=x_dim)
    for (x_filt=y_filt+x_start, x_edge=x_edge_dist;
	 x_filt<y_filt+x_stop;
	 x_filt++, x_edge++)
      result[abs(y_base-abs(y_edge)) + abs(x_base-abs(x_edge))]
	+= filt[x_filt];

  /* if EXPAND and filter is not centered on image edge, mult edge by 2 */
  if (f_or_e IS EXPAND)
      {
      if ( (abs(x_pos) ISNT mx_pos) AND (x_pos ISNT 0) )
	for (y_filt=x_base; y_filt<filt_sz; y_filt+=x_dim)
	  result[y_filt] += result[y_filt];
      if ( (abs(y_pos) ISNT my_pos) AND (y_pos ISNT 0) )
	for (x_filt=y_base; x_filt<y_base+x_dim; x_filt++)
	  result[x_filt] += result[x_filt];
      }
  }

/* --------------------------------------------------------------------
extend() - Extend image by reflecting and inverting about edge pixel
value.  Maintains continuity in intensity AND first derivative (but
not higher derivs).
*/

extend(filt,x_dim,y_dim,x_pos,y_pos,result,f_or_e)
  register float *filt, *result;
  register int x_dim;
  int y_dim, x_pos, y_pos, f_or_e;
  {
  int filt_sz = x_dim*y_dim;
  register int x_start = 0, y_start = 0, x_stop = x_dim, y_stop = filt_sz;
  register int y_filt,x_filt, y_edge,x_edge;
  register int x_base = (x_pos>0)?(x_dim-1):0;
  register int y_base = (y_pos>0)?(x_dim*(y_dim-1)):0; 
  int x_edge_dist = (x_pos>0)?(x_pos-x_dim):((x_pos<-1)?(x_pos+1):0);
  int y_edge_dist = x_dim * ((y_pos>0)?(y_pos-y_dim):((y_pos<-1)?(y_pos+1):0));
  int i;
  int mx_pos = (x_dim/2)+1;
  int my_pos = (y_dim/2)+1;

  for (i=0; i<filt_sz; i++) result[i] = 0.0;

  /* if EXPAND and filter is centered on image edge, do not reflect */
  if (f_or_e IS EXPAND)
      {
      if (x_pos IS mx_pos) x_stop = (x_dim+1)/2;
      else if (x_pos IS -mx_pos) { x_start = x_dim/2; x_edge_dist = 0; }

      if (y_pos IS my_pos) y_stop = x_dim*((y_dim+1)/2);
      else if (y_pos IS -my_pos) { y_start = x_dim*(y_dim/2); y_edge_dist = 0;}
      }

  /* reflect at boundary of image */
  for (y_filt=y_start, y_edge=y_edge_dist;
       y_filt<y_stop;
       y_filt+=x_dim, y_edge+=x_dim)
    for (x_filt=y_filt+x_start, x_edge=x_edge_dist;
	 x_filt<y_filt+x_stop;
	 x_filt++, x_edge++)
      if (((!y_base AND (sgn(y_edge) IS -1)) /* y overhanging */
	   OR
	   (y_base AND (sgn(y_edge) IS 1)))
	  ISNT			             /* XOR */
	  ((!x_base AND (sgn(x_edge) IS -1)) /* x overhanging */
	   OR
	   (x_base AND (sgn(x_edge) IS 1))))
	  {
	  result[abs(y_base-abs(y_edge)) + abs(x_base-abs(x_edge))]
	    -= filt[x_filt];
	  result[clip(y_base+y_edge,0,y_dim) + clip(x_base+x_edge,0,x_dim)]
	    += filt[x_filt] + filt[x_filt];
	  }
      else result[abs(y_base-abs(y_edge)) + abs(x_base-abs(x_edge))]
	  += filt[x_filt];

  }

/* --------------------------------------------------------------------
nocompute() - Return zero for values where filter hangs over the edge.
*/

nocompute(filt,x_dim,y_dim,x_pos,y_pos,result,f_or_e)
  register float *filt, *result;
  register int x_dim;
  int y_dim, x_pos, y_pos, f_or_e;
  {
  register int i;
  register int size = x_dim*y_dim;

  if ( (x_pos>1) OR (x_pos<-1) OR (y_pos>1) OR (y_pos<-1) )
    for (i=0; i<size; i++)  result[i] = 0.0;
  else
    for (i=0; i<size; i++)  result[i] = filt[i];
  }

/* --------------------------------------------------------------------
Reflect, multiplying tap of filter which is at the edge of the image
by root 2.  This maintains orthogonality of QMF-style filters, but it
is not useful for most applications.  
*/

ereflect(filt,x_dim,y_dim,x_pos,y_pos,result,f_or_e)
  register float *filt, *result;
  register int x_dim;
  int y_dim, x_pos, y_pos, f_or_e;
  {
  register int y_filt,x_filt, y_edge,x_edge;
  register int x_base = (x_pos>0)?(x_dim-1):0;
  register int y_base = x_dim * ( (y_pos>0)?(y_dim-1):0 ); 
  int filt_sz = x_dim*y_dim;
  int x_edge_dist = (x_pos>1)?(x_pos-x_dim):((x_pos<-1)?(x_pos+1):0);
  int y_edge_dist = x_dim * ( (y_pos>1)?(y_pos-y_dim):((y_pos<-1)?(y_pos+1):0) );
  int i;
  double norm,onorm;

  for (i=0; i<filt_sz; i++) result[i] = 0.0;

  /* reflect at boundary */       
  for (y_filt=0, y_edge=y_edge_dist;
       y_filt<filt_sz;
       y_filt+=x_dim, y_edge+=x_dim)
    for (x_filt=y_filt, x_edge=x_edge_dist;
	 x_filt<y_filt+x_dim;
	 x_filt++, x_edge++)
      result[abs(y_base-abs(y_edge)) + abs(x_base-abs(x_edge))]
	+= filt[x_filt];

  /* now multiply edge by root 2 */
  if (x_pos ISNT 0) 
    for (y_filt=x_base; y_filt<filt_sz; y_filt+=x_dim)
      result[y_filt] *= ROOT2;
  if (y_pos ISNT 0) 
    for (x_filt=y_base; x_filt<y_base+x_dim; x_filt++)
      result[x_filt] *= ROOT2;

  /* now normalize to norm of original filter */
  for (norm=0.0,i=0; i<filt_sz; i++)
    norm += (result[i]*result[i]);
  norm=sqrt(norm);

  for (onorm=0.0,i=0; i<filt_sz; i++)
    onorm += (filt[i]*filt[i]);
  onorm = sqrt(onorm);

  norm = norm/onorm;
  for (i=0; i<filt_sz; i++)
    result[i] /= norm;
  }

/* --------------------------------------------------------------------
predict() - version of prediction.  Like zero, but multiplies the result by
the reciprocal of the percentage of filter being used.  (i.e. if 50% of the
filter is hanging over the edge of the image, multiply the taps being used by
2.  *** Fix this to divide by the squared sum of taps being used and multiply by
the squared sum of ALL taps.
*/

predict(filt,x_dim,y_dim,x_pos,y_pos,result,f_or_e)
  register float *filt, *result;
  register int x_dim;
  int y_dim, x_pos, y_pos, f_or_e;
  {
  register int y_filt,x_filt, y_res,x_res;
  register float taps_used = 0; /* int *** */
  register float fraction;
  int filt_sz = x_dim*y_dim;
  int x_start = ((x_pos>0)?(x_pos-1):((x_pos<0)?(x_pos+1):0));
  int y_start = x_dim * ((y_pos>0)?(y_pos-1):((y_pos<0)?(y_pos+1):0));
  int i;

  for (i=0; i<filt_sz; i++) result[i] = 0.0;

  for (y_filt=0, y_res=y_start;
       y_filt<filt_sz;
       y_filt+=x_dim, y_res+=x_dim)
      if ((y_res >= 0) AND (y_res < filt_sz))
	for (x_filt=y_filt, x_res=x_start;
	     x_filt<y_filt+x_dim;
	     x_filt++, x_res++)
	  if ((x_res >= 0) AND (x_res < x_dim))
	      {
	      result[y_res+x_res] = filt[x_filt];
	      taps_used += filt[x_filt]; /* 1.0 *** */
	      }

  if (f_or_e IS FILTER)
      {
      fraction = ( (float) 2.0 /* filt_sz *** */ ) / ( (float) taps_used );
      for (i=0; i<filt_sz; i++) result[i] *= fraction;
      }
  }


/* ------- printout stuff for testing ------------------------------
  fprintf(stderr,"Xpos: %d, Ypos: %d", x_pos, y_pos);
    for (y_filt=0; y_filt<y_dim; y_filt++)
      {
      fprintf(stderr,"\n");
      for (x_filt=0; x_filt<x_dim; x_filt++)
	fprintf(stderr,"%6.1f", result[y_filt*x_dim+x_filt]);
      }
  fprintf(stderr,"\n");
*/



/* Local Variables: */
/* buffer-read-only: t */
/* End: */
