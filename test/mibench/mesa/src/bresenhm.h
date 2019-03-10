/* $Id: bresenhm.h,v 1.3 1996/10/17 03:24:20 brianp Exp $ */

/*
 * Mesa 3-D graphics library
 * Version:  2.0
 * Copyright (C) 1995-1996  Brian Paul
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */


/*
 * $Log: bresenhm.h,v $
 * Revision 1.3  1996/10/17 03:24:20  brianp
 * use 7 fractional bits instead of 8 in BRESENHAM_Z() macro
 *
 * Revision 1.2  1996/09/15 14:18:10  brianp
 * now use GLframebuffer and GLvisual
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


/*
 * A macro which executes Bresenham's line drawing algorithm.  The
 * previously defined BRESENHAM_PLOT macro is then used to 'plot' pixels.
 */


#ifndef BRESENHAM_H
#define BRESENHAM_H


#include "types.h"



/* TODO:  combine these macros to make a linetemp.h file like tritemp.h */


/*
 * Bresenham's line algorithm.
 */
#define BRESENHAM( x1, y1, x2, y2 )	\
{					\
   GLint dx, dy, xf, yf, ta, tb, tt, i;	\
   if (x1!=x2 || y1!=y2) {		\
      if (x2>x1) {			\
         dx = x2-x1;			\
         xf = 1;			\
      }					\
      else {				\
         dx = x1-x2;			\
         xf = -1;			\
      }					\
      if (y2>y1) {			\
         dy = y2-y1;			\
         yf = 1;			\
      }					\
      else {				\
         dy = y1-y2;			\
         yf = -1;			\
      }					\
      if (dx>dy) {			\
         ta = dy+dy;			\
         tt = ta-dx;			\
         tb = tt-dx;			\
         for (i=0;i<=dx;i++) {		\
	    BRESENHAM_PLOT( x1, y1 )	\
            x1 += xf;			\
            if (tt<0) {			\
               tt += ta;		\
            }				\
            else {			\
               tt += tb;		\
               y1 += yf;		\
            }				\
         }				\
      }					\
      else {				\
         ta = dx+dx;			\
         tt = ta-dy;			\
         tb = tt-dy;			\
         for (i=0;i<=dy;i++) {		\
	    BRESENHAM_PLOT( x1, y1 )	\
            y1 += yf;			\
            if (tt<0) {			\
               tt += ta;		\
            }				\
            else {			\
               tt += tb;		\
               x1 += xf;		\
	    }				\
         }				\
      }					\
   }					\
}




/*
 * Bresenham's line algorithm with Z interpolation.
 * Z interpolation done with fixed point arithmetic, 7 fraction bits.
 */
#define BRESENHAM_Z( ctx, x1, y1, z1, x2, y2, z2 )	\
{							\
   GLint dx, dy, xstep, ystep, ta, tb, tt, i;		\
   GLint dz, dzdx, dzdy;				\
   GLdepth *zptr;					\
   if (x1!=x2 || y1!=y2) {			\
      z1 = z1 << 7;				\
      z2 = z2 << 7;				\
      if (x2>x1) {				\
         dx = x2-x1;				\
         xstep = 1;				\
	 dzdx = 1;				\
      }						\
      else {					\
         dx = x1-x2;				\
         xstep = -1;				\
	 dzdx = -1;				\
      }						\
      if (y2>y1) {				\
         dy = y2-y1;				\
         ystep = 1;				\
	 dzdy = ctx->Buffer->Width;		\
      }						\
      else {					\
         dy = y1-y2;				\
         ystep = -1;				\
	 dzdy = -ctx->Buffer->Width;		\
      }						\
      zptr = Z_ADDRESS(ctx,x1,y1);		\
      if (dx>dy) {				\
         dz = (z2-z1)/dx;			\
         ta = dy+dy;				\
         tt = ta-dx;				\
         tb = tt-dx;				\
         for (i=0;i<=dx;i++) {			\
            GLdepth z = z1>>7;			\
	    BRESENHAM_PLOT( x1, y1, z, zptr )	\
            x1 += xstep;			\
	    zptr += dzdx;			\
            if (tt<0) {				\
               tt += ta;			\
            }					\
            else {				\
               tt += tb;			\
               y1 += ystep;			\
	       zptr += dzdy;			\
            }					\
	    z1 += dz;				\
         }					\
      }						\
      else {					\
         dz = (z2-z1)/dy;			\
         ta = dx+dx;				\
         tt = ta-dy;				\
         tb = tt-dy;				\
         for (i=0;i<=dy;i++) {			\
            GLdepth z = z1>>7;			\
	    BRESENHAM_PLOT( x1, y1, z, zptr )	\
            y1 += ystep;			\
	    zptr += dzdy;			\
            if (tt<0) {				\
               tt += ta;			\
            }					\
            else {				\
               tt += tb;			\
               x1 += xstep;			\
	       zptr += dzdx;			\
	    }					\
	    z1 += dz;				\
         }					\
      }						\
   }						\
}



extern GLuint gl_bresenham( GLcontext* ctx,
                            GLint x1, GLint y1, GLint x2, GLint y2,
			    GLint x[], GLint y[] );


extern GLuint gl_stippled_bresenham( GLcontext* ctx,
                                     GLint x1, GLint y1, GLint x2, GLint y2,
				     GLint x[], GLint y[], GLubyte mask[] );



#endif
