/* $Id: interp.c,v 1.2 1996/09/25 02:01:28 brianp Exp $ */

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
 * $Log: interp.c,v $
 * Revision 1.2  1996/09/25 02:01:28  brianp
 * removed gl_interp_texcoords() and gl_interp_texcoords2()
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#include "interp.h"
#include "macros.h"
#include "types.h"



/*
 * Linear integer interpolation of Z (depth) values:
 * Iterpolate n integer values between z0 and z1 and put into zspan.
 * When finished, zspan[0] = z0, zspan[n-1] = z1, and the rest of
 * zspan[] is filled with interpolated values.
 * We have to be careful to avoid integer overflow!
 */
void gl_interpolate_z( GLint n, GLint z0, GLint z1, GLdepth zspan[] )
{
   GLint i, dz;

   switch (n) {
      case 1:
         zspan[0] = z0;
         return;
      case 2:
         zspan[0] = z0;
         zspan[1] = z1;
         return;
      case 3:
         zspan[0] = z0;
         zspan[1] = (z0 + z1) >> 1;
         zspan[2] = z1;
         return;
      default:
         z0 = z0 << 7;
         z1 = z1 << 7;
         dz = (z1-z0) / (n-1);
         for (i=0;i<n;i++) {
            zspan[i] = z0 >> 7;
            z0 += dz;
         }
         return;
   }
}



/*
 * Linear integer interpolation:
 * Iterpolate n integer values between y0 and y1 and put into yspan.
 * When finished, yspan[0] = y0, yspan[n-1] = y1, and the rest of
 * yspan[] is filled with interpolated values.
 */
void gl_interpolate_i( GLint n, GLint y0, GLint y1, GLint yspan[] )
{
   switch (n) {
      case 1:
         yspan[0] = y0;
	 return;
      case 2:
         yspan[0] = y0;
         yspan[1] = y1;
	 return;
      case 3:
         yspan[0] = y0;
	 yspan[1] = (y0+y1) >> 1;
         yspan[2] = y1;
	 return;
      default:
	 if (y0==y1) {
	    register GLint i;
	    for (i=0;i<n;i++) {
	       yspan[i] = y0;
	    }
	 }
	 else {
	    register GLint i;
	    register GLint dx, dy;
	    register GLint a, b, d;
	    register GLint y;
	    register GLint qa, qb;
   	    dx = n-1;
	    dy = y1 - y0;
	    qa = dy / dx;
	    dy = dy % dx;
	    if (dy<0) {
	       dy = -dy;
	       qb = qa - 1;
	    }
	    else {
	       qb = qa + 1;
	    }
	    a = dy+dy;   d = a-dx;   b = d-dx;
	    y = y0;
	    for (i=0;i<n;i++) {
	       yspan[i] = y;
	       if (d<0) {
		  d += a;
		  y += qa;
	       }
	       else {
		  d += b;
		  y += qb;
	       }
	    }
	 }
   }
}



/*
 * Interpolate RGBA values.
 * Input:  n - number of values to generate
 *         r0, r1 - first and last alpha values
 *         g0, g1 - first and last alpha values
 *         b0, b1 - first and last alpha values
 *         a0, a1 - first and last alpha values
 * Output:  rspan, gspan, bspan, aspan - interpolated color values in
 *          the range [0,CC.RedScale], [0,CC.GreenScale], [0,CC.BlueScale],
 *          and [0,CC.AlphaScale].
 */
void gl_interpolate_rgba( GLint n,
                          GLfixed r0, GLfixed r1, GLubyte rspan[],
                          GLfixed g0, GLfixed g1, GLubyte gspan[],
                          GLfixed b0, GLfixed b1, GLubyte bspan[],
                          GLfixed a0, GLfixed a1, GLubyte aspan[] )
{
   GLint i, m;
   GLfixed dr, dg, db, da;

   switch (n) {
      case 1:
         rspan[0] = FixedToInt(r0);
         gspan[0] = FixedToInt(g0);
         bspan[0] = FixedToInt(b0);
         aspan[0] = FixedToInt(a0);
	 return;
      case 2:
         rspan[0] = FixedToInt(r0);   rspan[1] = FixedToInt(r1);
         gspan[0] = FixedToInt(g0);   gspan[1] = FixedToInt(g1);
         bspan[0] = FixedToInt(b0);   bspan[1] = FixedToInt(b1);
         aspan[0] = FixedToInt(a0);   aspan[1] = FixedToInt(a1);
	 return;
      default:
         m = n-1;
         dr = (r1-r0) / m;
         dg = (g1-g0) / m;
         db = (b1-b0) / m;
         da = (a1-a0) / m;
         for (i=0;i<n;i++) {
            rspan[i] = FixedToInt(r0);    r0 += dr;
            gspan[i] = FixedToInt(g0);    g0 += dg;
            bspan[i] = FixedToInt(b0);    b0 += db;
            aspan[i] = FixedToInt(a0);    a0 += da;
         }
         return;
   }
}

