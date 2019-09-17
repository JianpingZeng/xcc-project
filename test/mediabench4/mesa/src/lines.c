/* $Id: lines.c,v 1.9 1997/03/08 02:04:27 brianp Exp $ */

/*
 * Mesa 3-D graphics library
 * Version:  2.2
 * Copyright (C) 1995-1997  Brian Paul
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
 * $Log: lines.c,v $
 * Revision 1.9  1997/03/08 02:04:27  brianp
 * better implementation of feedback function
 *
 * Revision 1.8  1997/02/09 18:44:20  brianp
 * added GL_EXT_texture3D support
 *
 * Revision 1.7  1997/01/09 19:48:00  brianp
 * now call gl_texturing_enabled()
 *
 * Revision 1.6  1996/11/08 02:21:21  brianp
 * added null drawing function for GL_NO_RASTER
 *
 * Revision 1.5  1996/09/27 01:28:56  brianp
 * removed unused variables
 *
 * Revision 1.4  1996/09/25 02:01:54  brianp
 * new texture coord interpolation
 *
 * Revision 1.3  1996/09/15 14:18:10  brianp
 * now use GLframebuffer and GLvisual
 *
 * Revision 1.2  1996/09/15 01:48:58  brianp
 * removed #define NULL 0
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#include "bresenhm.h"
#include "context.h"
#include "feedback.h"
#include "interp.h"
#include "lines.h"
#include "dlist.h"
#include "macros.h"
#include "pb.h"
#include "texture.h"
#include "types.h"
#include "vb.h"




void gl_LineWidth( GLcontext *ctx, GLfloat width )
{
   if (width<=0.0) {
      gl_error( ctx, GL_INVALID_VALUE, "glLineWidth" );
      return;
   }
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glLineWidth" );
      return;
   }
   ctx->Line.Width = width;
   ctx->NewState |= NEW_RASTER_OPS;
}



void gl_LineStipple( GLcontext *ctx, GLint factor, GLushort pattern )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glLineStipple" );
      return;
   }
   ctx->Line.StippleFactor = CLAMP( factor, 1, 256 );
   ctx->Line.StipplePattern = pattern;
   ctx->NewState |= NEW_RASTER_OPS;
}



/**********************************************************************/
/*****                    Rasterization                           *****/
/**********************************************************************/


/*
 * There are 4 pairs (RGBA, CI) of line drawing functions:
 *   1. simple:  width=1 and no special rasterization functions (fastest)
 *   2. flat:  width=1, non-stippled, flat-shaded, any raster operations
 *   3. smooth:  width=1, non-stippled, smooth-shaded, any raster operations
 *   4. general:  any other kind of line (slowest)
 */


/*
 * All line drawing functions have the same arguments:
 * v1, v2 - indexes of first and second endpoints into vertex buffer arrays
 * pv     - provoking vertex: which vertex color/index to use for flat shading.
 */



static void feedback_line( GLcontext *ctx, GLuint v1, GLuint v2, GLuint pv )
{
   struct vertex_buffer *VB = ctx->VB;
   GLfloat x1, y1, z1, w1;
   GLfloat x2, y2, z2, w2;
   GLfloat tex1[4], tex2[4], invq;
   GLfloat invRedScale   = ctx->Visual->InvRedScale;
   GLfloat invGreenScale = ctx->Visual->InvGreenScale;
   GLfloat invBlueScale  = ctx->Visual->InvBlueScale;
   GLfloat invAlphaScale = ctx->Visual->InvAlphaScale;

   x1 = VB->Win[v1][0];
   y1 = VB->Win[v1][1];
   z1 = VB->Win[v1][2] / DEPTH_SCALE;
   w1 = VB->Clip[v1][3];

   x2 = VB->Win[v2][0];
   y2 = VB->Win[v2][1];
   z2 = VB->Win[v2][2] / DEPTH_SCALE;
   w2 = VB->Clip[v2][3];

   invq = 1.0F / VB->TexCoord[v1][3];
   tex1[0] = VB->TexCoord[v1][0] * invq;
   tex1[1] = VB->TexCoord[v1][1] * invq;
   tex1[2] = VB->TexCoord[v1][2] * invq;
   tex1[3] = VB->TexCoord[v1][3];
   invq = 1.0F / VB->TexCoord[v2][3];
   tex2[0] = VB->TexCoord[v2][0] * invq;
   tex2[1] = VB->TexCoord[v2][1] * invq;
   tex2[2] = VB->TexCoord[v2][2] * invq;
   tex2[3] = VB->TexCoord[v2][3];

   if (ctx->StippleCounter==0) {
      FEEDBACK_TOKEN( ctx, (GLfloat) GL_LINE_RESET_TOKEN );
   }
   else {
      FEEDBACK_TOKEN( ctx, (GLfloat) GL_LINE_TOKEN );
   }
   if (ctx->Light.ShadeModel==GL_FLAT) {
      GLfloat color[4];
      /* convert color from integer to a float in [0,1] */
      color[0] = (GLfloat) VB->Color[pv][0] * invRedScale;
      color[1] = (GLfloat) VB->Color[pv][1] * invGreenScale;
      color[2] = (GLfloat) VB->Color[pv][2] * invBlueScale;
      color[3] = (GLfloat) VB->Color[pv][3] * invAlphaScale;
      gl_feedback_vertex( ctx, x1,y1,z1,w1, color,
                          (GLfloat) VB->Index[pv], tex1 );
      gl_feedback_vertex( ctx, x2,y2,z2,w2, color,
                          (GLfloat) VB->Index[pv], tex2 );
   }
   else {
      GLfloat color[4];
      /* convert color from fixed point to a float in [0,1] */
      color[0] = FixedToFloat(VB->Color[v1][0]) * invRedScale;
      color[1] = FixedToFloat(VB->Color[v1][1]) * invGreenScale;
      color[2] = FixedToFloat(VB->Color[v1][2]) * invBlueScale;
      color[3] = FixedToFloat(VB->Color[v1][3]) * invAlphaScale;
      gl_feedback_vertex( ctx, x1,y1,z1,w1, color,
                          (GLfloat) VB->Index[v1], tex1 );
      /* convert color from fixed point to a float in [0,1] */
      color[0] = FixedToFloat(VB->Color[v2][0]) * invRedScale;
      color[1] = FixedToFloat(VB->Color[v2][1]) * invGreenScale;
      color[2] = FixedToFloat(VB->Color[v2][2]) * invBlueScale;
      color[3] = FixedToFloat(VB->Color[v2][3]) * invAlphaScale;
      gl_feedback_vertex( ctx, x2,y2,z2,w2, color,
                          (GLfloat) VB->Index[v2], tex2 );
   }
   ctx->StippleCounter++;
}



static void select_line( GLcontext *ctx, GLuint v1, GLuint v2, GLuint pv )
{
   gl_update_hitflag( ctx, ctx->VB->Win[v1][2] / DEPTH_SCALE );
   gl_update_hitflag( ctx, ctx->VB->Win[v2][2] / DEPTH_SCALE );
}



#if MAX_WIDTH > MAX_HEIGHT
#  define MAXPOINTS MAX_WIDTH
#else
#  define MAXPOINTS MAX_HEIGHT
#endif


/*
 * Flat shaded, width=1, non-stippled, color index line.
 */
static void flat_ci_line( GLcontext *ctx, GLuint v1, GLuint v2, GLuint pv )
{
   struct vertex_buffer *VB = ctx->VB;
   struct pixel_buffer *PB = ctx->PB;
   GLint x1 = (GLint) VB->Win[v1][0];
   GLint y1 = (GLint) VB->Win[v1][1];
   GLint x2 = (GLint) VB->Win[v2][0];
   GLint y2 = (GLint) VB->Win[v2][1];
   GLint n;

   PB_SET_INDEX( ctx, PB, VB->Index[pv] );

   /* compute pixel locations */
   n = gl_bresenham( ctx, x1, y1, x2, y2, PB->x+PB->count, PB->y+PB->count );

   /* interpolate z values */
   if (ctx->Depth.Test) {
      GLdepth *zptr = PB->z + PB->count;
      GLint z1 = (GLint) (VB->Win[v1][2] + ctx->LineZoffset);
      GLint z2 = (GLint) (VB->Win[v2][2] + ctx->LineZoffset);
      GL_INTERPOLATE_Z( n, z1, z2, zptr );
   }

   PB->count += n;
   PB_CHECK_FLUSH( ctx, PB );
}


/*
 * Flat-shaded, width=1, non-stippled, rgba line.
 */
static void flat_rgba_line( GLcontext *ctx, GLuint v1, GLuint v2, GLuint pv )
{
   struct vertex_buffer *VB = ctx->VB;
   struct pixel_buffer *PB = ctx->PB;
   GLint x1 = (GLint) VB->Win[v1][0];
   GLint y1 = (GLint) VB->Win[v1][1];
   GLint x2 = (GLint) VB->Win[v2][0];
   GLint y2 = (GLint) VB->Win[v2][1];
   GLint n;

   /* Note that color components are ints, not fixed point here */
   PB_SET_COLOR( ctx, PB, VB->Color[pv][0], VB->Color[pv][1],
                 VB->Color[pv][2], VB->Color[pv][3] );

   /* compute pixel locations */
   n = gl_bresenham( ctx, x1, y1, x2, y2, PB->x+PB->count, PB->y+PB->count );

   /* interpolate z values */
   if (ctx->Depth.Test) {
      GLdepth *zptr = PB->z + PB->count;
      GLint z1 = (GLint) (VB->Win[v1][2] + ctx->LineZoffset);
      GLint z2 = (GLint) (VB->Win[v2][2] + ctx->LineZoffset);
      GL_INTERPOLATE_Z( n, z1, z2, zptr );
   }

   PB->count += n;
   PB_CHECK_FLUSH( ctx, PB );
}



/*
 * Smooth-shaded, width=1, non-stippled, color index line.
 */
static void smooth_ci_line( GLcontext *ctx, GLuint v1, GLuint v2, GLuint pv )
{
   struct vertex_buffer *VB = ctx->VB;
   struct pixel_buffer *PB = ctx->PB;
   GLint x1 = (GLint) VB->Win[v1][0];
   GLint y1 = (GLint) VB->Win[v1][1];
   GLint x2 = (GLint) VB->Win[v2][0];
   GLint y2 = (GLint) VB->Win[v2][1];
   GLint n;

   /* compute pixel locations */
   n = gl_bresenham( ctx, x1, y1, x2, y2, PB->x+PB->count, PB->y+PB->count );

   /* interpolate z values */
   if (ctx->Depth.Test) {
      GLdepth *zptr = PB->z + PB->count;
      GLint z1 = (GLint) (VB->Win[v1][2] + ctx->LineZoffset);
      GLint z2 = (GLint) (VB->Win[v2][2] + ctx->LineZoffset);
      GL_INTERPOLATE_Z( n, z1, z2, zptr );
   }

   /* interpolate index */
   gl_interpolate_i( n, (GLint) VB->Index[v1], (GLint) VB->Index[v2],
                     (GLint *) PB->i+PB->count );

   PB->count += n;
   PB_CHECK_FLUSH( ctx, PB );
}


/*
 * Smooth-shaded, width=1, non-stippled, RGBA line.
 */
static void smooth_rgba_line( GLcontext *ctx, GLuint v1, GLuint v2, GLuint pv )
{
   struct vertex_buffer *VB = ctx->VB;
   struct pixel_buffer *PB = ctx->PB;
   GLint x1 = (GLint) VB->Win[v1][0];
   GLint y1 = (GLint) VB->Win[v1][1];
   GLint x2 = (GLint) VB->Win[v2][0];
   GLint y2 = (GLint) VB->Win[v2][1];
   GLint n;

   /* compute pixel locations */
   n = gl_bresenham( ctx, x1, y1, x2, y2, PB->x+PB->count, PB->y+PB->count );

   /* interpolate z values */
   if (ctx->Depth.Test) {
      GLdepth *zptr = PB->z + PB->count;
      GLint z1 = (GLint) (VB->Win[v1][2] + ctx->LineZoffset);
      GLint z2 = (GLint) (VB->Win[v2][2] + ctx->LineZoffset);
      GL_INTERPOLATE_Z( n, z1, z2, zptr );
   }

   /* interpolate color, VB->Colors are in fixed point */
   gl_interpolate_rgba( n,
                        VB->Color[v1][0], VB->Color[v2][0], PB->r+PB->count,
                        VB->Color[v1][1], VB->Color[v2][1], PB->g+PB->count,
                        VB->Color[v1][2], VB->Color[v2][2], PB->b+PB->count,
                        VB->Color[v1][3], VB->Color[v2][3], PB->a+PB->count );

   PB->count += n;
   PB_CHECK_FLUSH( ctx, PB );
}



/*
 * General CI line:  any width, smooth or flat, stippled, any raster ops.
 */
static void general_ci_line( GLcontext *ctx, GLuint v1, GLuint v2, GLuint pv )
{
   struct vertex_buffer *VB = ctx->VB;
   struct pixel_buffer *PB = ctx->PB;
   GLint x1, y1, x2, y2;
   GLint x[MAXPOINTS], y[MAXPOINTS];
   GLdepth z[MAXPOINTS];
   GLubyte mask[MAXPOINTS];
   GLuint index[MAXPOINTS];
   GLint i, n;
   GLint dx, dy;

   x1 = (GLint) VB->Win[v1][0];
   y1 = (GLint) VB->Win[v1][1];
   x2 = (GLint) VB->Win[v2][0];
   y2 = (GLint) VB->Win[v2][1];

   /* compute pixel locations */
   if (ctx->Line.StippleFlag) {
      n = gl_stippled_bresenham( ctx, x1, y1, x2, y2, x, y, mask );
   }
   else {
      n = gl_bresenham( ctx, x1, y1, x2, y2, x, y );
      for (i=0;i<n;i++) {
	 mask[i] = 1;
      }
   }

   if (ctx->Depth.Test) {
      /* interpolate z */
      GLint z1 = (GLint) (VB->Win[v1][2] + ctx->LineZoffset);
      GLint z2 = (GLint) (VB->Win[v2][2] + ctx->LineZoffset);
      GL_INTERPOLATE_Z( n, z1, z2, z );
   }

   if (ctx->Light.ShadeModel==GL_FLAT) {
      GLuint indx = VB->Index[pv];
      for (i=0;i<n;i++) {
	 index[i] = indx;
      }
   }
   else {
      /* interpolate index */
      gl_interpolate_i( n, (GLint) VB->Index[v1], (GLint) VB->Index[v2],
		        (GLint *) index );
   }

   /* compute delta x and delta y */
   if (x1>x2) {
      dx = x1 - x2;
   }
   else {
      dx = x2 - x1;
   }
   if (y1>y2) {
      dy = y1 - y2;
   }
   else {
      dy = y2 - y1;
   }


   /* render */
   if (ctx->Line.Width==2.0F) {
      /* special case, easy to optimize */
      if (dx>dy) {
	 /* X-major: duplicate pixels in Y direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       PB_WRITE_CI_PIXEL( PB, x[i], y[i]-1, z[i], index[i] );
	       PB_WRITE_CI_PIXEL( PB, x[i], y[i], z[i], index[i] );
	    }
	 }
      }
      else {
	 /* Y-major: duplicate pixels in X direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       PB_WRITE_CI_PIXEL( PB, x[i]-1, y[i], z[i], index[i] );
	       PB_WRITE_CI_PIXEL( PB, x[i], y[i], z[i], index[i] );
	    }
	 }
      }
      PB_CHECK_FLUSH( ctx, PB );
   }
   else {
      GLint width, w0, w1;
      width = (GLint) CLAMP( ctx->Line.Width, MIN_LINE_WIDTH, MAX_LINE_WIDTH );
      w0 = -width / 2;
      w1 = w0 + width - 1;

      if (dx>dy) {
	 /* X-major: duplicate pixels in Y direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       GLint yy;
	       GLint y0 = y[i] + w0;
	       GLint y1 = y[i] + w1;
	       for (yy=y0;yy<=y1;yy++) {
		  PB_WRITE_CI_PIXEL( PB, x[i], yy, z[i], index[i] );
	       }
	       PB_CHECK_FLUSH( ctx, PB );
	    }
	 }
      }
      else {
	 /* Y-major: duplicate pixels in X direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       GLint xx;
	       GLint x0 = x[i] + w0;
	       GLint x1 = x[i] + w1;
	       for (xx=x0;xx<=x1;xx++) {
		  PB_WRITE_CI_PIXEL( PB, xx, y[i], z[i], index[i] );
	       }
	       PB_CHECK_FLUSH( ctx, PB );
	    }
	 }
      }
   }
}


/*
 * General RGBA line:  any width, smooth or flat, stippled, any raster ops.
 */
static void general_rgba_line( GLcontext *ctx,
                               GLuint v1, GLuint v2, GLuint pv )
{
   struct vertex_buffer *VB = ctx->VB;
   struct pixel_buffer *PB = ctx->PB;
   GLint x1, y1, x2, y2;
   GLint x[MAXPOINTS], y[MAXPOINTS];
   GLdepth z[MAXPOINTS];
   GLubyte mask[MAXPOINTS];
   GLubyte red[MAXPOINTS], green[MAXPOINTS], blue[MAXPOINTS], alpha[MAXPOINTS];
   GLint i, n;
   GLint dx, dy;

   x1 = (GLint) VB->Win[v1][0];
   y1 = (GLint) VB->Win[v1][1];
   x2 = (GLint) VB->Win[v2][0];
   y2 = (GLint) VB->Win[v2][1];

   /* compute the line */
   if (ctx->Line.StippleFlag) {
      n = gl_stippled_bresenham( ctx, x1, y1, x2, y2, x, y, mask );
   }
   else {
      n = gl_bresenham( ctx, x1, y1, x2, y2, x, y );
      for (i=0;i<n;i++) {
	 mask[i] = 1;
      }
   }

   if (ctx->Depth.Test) {
      GLint z1 = (GLint) (VB->Win[v1][2] + ctx->LineZoffset);
      GLint z2 = (GLint) (VB->Win[v2][2] + ctx->LineZoffset);
      GL_INTERPOLATE_Z( n, z1, z2, z );
   }

   if (ctx->Light.ShadeModel==GL_FLAT) {
      GLint r, g, b, a;
      r = VB->Color[pv][0];  /* colors are ints, not fixed point */
      g = VB->Color[pv][1];
      b = VB->Color[pv][2];
      a = VB->Color[pv][3];
      for (i=0;i<n;i++) {
	 red[i]   = r;
	 green[i] = g;
	 blue[i]  = b;
	 alpha[i] = a;
      }
   }
   else {
      /* interpolate color, VB->Colors are in fixed point */
      gl_interpolate_rgba( n,
                           VB->Color[v1][0], VB->Color[v2][0], red,
                           VB->Color[v1][1], VB->Color[v2][1], green,
                           VB->Color[v1][2], VB->Color[v2][2], blue,
                           VB->Color[v1][3], VB->Color[v2][3], alpha );
   }

   /* compute delta x and delta y */
   if (x1>x2) {
      dx = x1 - x2;
   }
   else {
      dx = x2 - x1;
   }
   if (y1>y2) {
      dy = y1 - y2;
   }
   else {
      dy = y2 - y1;
   }

   /* render */
   if (ctx->Line.Width==2.0F) {
      /* special case, easy to optimize */
      if (dx>dy) {
	 /* X-major: duplicate pixels in Y direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       PB_WRITE_RGBA_PIXEL( PB, x[i], y[i]-1, z[i],
				    red[i], green[i], blue[i], alpha[i] );
	       PB_WRITE_RGBA_PIXEL( PB, x[i], y[i], z[i],
				    red[i], green[i], blue[i], alpha[i] );
	    }
	 }
      }
      else {
	 /* Y-major: duplicate pixels in X direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       PB_WRITE_RGBA_PIXEL( PB, x[i]-1, y[i], z[i],
				    red[i], green[i], blue[i], alpha[i] );
	       PB_WRITE_RGBA_PIXEL( PB, x[i], y[i], z[i],
				    red[i], green[i], blue[i], alpha[i] );
	    }
	 }
      }
      PB_CHECK_FLUSH( ctx, PB );
   }
   else {
      GLint width, w0, w1;
      width = (GLint) CLAMP( ctx->Line.Width, MIN_LINE_WIDTH, MAX_LINE_WIDTH );
      w0 = -width / 2;
      w1 = w0 + width - 1;

      if (dx>dy) {
	 /* X-major: duplicate pixels in Y direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       GLint yy;
	       GLint y0 = y[i] + w0;
	       GLint y1 = y[i] + w1;
	       for (yy=y0;yy<=y1;yy++) {
		  PB_WRITE_RGBA_PIXEL( PB, x[i], yy, z[i],
				       red[i], green[i], blue[i], alpha[i] );
	       }
	       PB_CHECK_FLUSH( ctx, PB );
	    }
	 }
      }
      else {
	 /* Y-major: duplicate pixels in X direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       GLint xx;
	       GLint x0 = x[i] + w0;
	       GLint x1 = x[i] + w1;
	       for (xx=x0;xx<=x1;xx++) {
		  PB_WRITE_RGBA_PIXEL( PB, xx, y[i], z[i],
				       red[i], green[i], blue[i], alpha[i] );
	       }
	       PB_CHECK_FLUSH( ctx, PB );
	    }
	 }
      }
   }
}



/*
 * Textured RGBA line:  any width, smooth or flat, stippled, any raster ops
 * with texturing.
 */
static void textured_rgba_line( GLcontext *ctx,
                                GLuint v1, GLuint v2, GLuint pv )
{
   struct vertex_buffer *VB = ctx->VB;
   struct pixel_buffer *PB = ctx->PB;
   GLint x1, y1, x2, y2;
   GLint x[MAXPOINTS], y[MAXPOINTS];
   GLdepth z[MAXPOINTS];
   GLubyte mask[MAXPOINTS];
   /* Allocate arrays dynamically on Mac */
   DEFARRAY(GLubyte,red,MAXPOINTS);
   DEFARRAY(GLubyte,green,MAXPOINTS);
   DEFARRAY(GLubyte,blue,MAXPOINTS);
   DEFARRAY(GLubyte,alpha,MAXPOINTS);
   DEFARRAY(GLfloat,s,MAXPOINTS);
   DEFARRAY(GLfloat,t,MAXPOINTS);
   DEFARRAY(GLfloat,u,MAXPOINTS);
#if 0
   DEFARRAY(GLfloat,v,MAXPOINTS);
#endif
   GLint i, n;
   GLint dx, dy;

   x1 = (GLint) VB->Win[v1][0];
   y1 = (GLint) VB->Win[v1][1];
   x2 = (GLint) VB->Win[v2][0];
   y2 = (GLint) VB->Win[v2][1];

   /* compute the line */
   if (ctx->Line.StippleFlag) {
      n = gl_stippled_bresenham( ctx, x1, y1, x2, y2, x, y, mask );
   }
   else {
      n = gl_bresenham( ctx, x1, y1, x2, y2, x, y );
      for (i=0;i<n;i++) {
	 mask[i] = 1;
      }
   }

   if (ctx->Depth.Test) {
      GLint z1 = (GLint) (VB->Win[v1][2] + ctx->LineZoffset);
      GLint z2 = (GLint) (VB->Win[v2][2] + ctx->LineZoffset);
      GL_INTERPOLATE_Z( n, z1, z2, z );
   }

   if (ctx->Light.ShadeModel==GL_FLAT) {
      GLint r, g, b, a;
      r = VB->Color[pv][0];  /* colors are ints, not in fixed point */
      g = VB->Color[pv][1];
      b = VB->Color[pv][2];
      a = VB->Color[pv][3];
      for (i=0;i<n;i++) {
	 red[i]   = r;
	 green[i] = g;
	 blue[i]  = b;
	 alpha[i] = a;
      }
   }
   else {
      /* interpolate color, VB->Colors are in fixed point */
      gl_interpolate_rgba( n,
                           VB->Color[v1][0], VB->Color[v2][0], red,
                           VB->Color[v1][1], VB->Color[v2][1], green,
                           VB->Color[v1][2], VB->Color[v2][2], blue,
                           VB->Color[v1][3], VB->Color[v2][3], alpha );
   }

   /* interpolate texture coordinates */
   {
      GLfloat w1 = 1.0F / VB->Clip[v1][3];
      GLfloat w2 = 1.0F / VB->Clip[v2][3];
      GLfloat s1 = VB->TexCoord[v1][0] * w1;
      GLfloat s2 = VB->TexCoord[v2][0] * w2;
      GLfloat t1 = VB->TexCoord[v1][1] * w1;
      GLfloat t2 = VB->TexCoord[v2][1] * w2;
      GLfloat u1 = VB->TexCoord[v1][2] * w1;
      GLfloat u2 = VB->TexCoord[v2][2] * w2;
      /* don't interpolate r since we don't do 3-D textures, yet */
      GLfloat q1 = VB->TexCoord[v1][3] * w1;
      GLfloat q2 = VB->TexCoord[v2][3] * w2;
      GLfloat inv_n = 1.0F / (GLfloat) n;
      GLfloat ds = (s2-s1) * inv_n;
      GLfloat dt = (t2-t1) * inv_n;
      GLfloat du = (u2-u1) * inv_n;
      GLfloat dq = (q2-q1) * inv_n;
      for (i=0;i<n;i++) {
         s[i] = s1 / q1;
         t[i] = t1 / q1;
         u[i] = u1 / q1;
         s1 += ds;
         t1 += dt;
         u1 += du;
         q1 += dq;
      }
   }

   /* compute delta x and delta y */
   if (x1>x2) {
      dx = x1 - x2;
   }
   else {
      dx = x2 - x1;
   }
   if (y1>y2) {
      dy = y1 - y2;
   }
   else {
      dy = y2 - y1;
   }

   /* render */
   if (ctx->Line.Width==2.0F) {
      /* special case, easy to optimize */
      if (dx>dy) {
	 /* X-major: duplicate pixels in Y direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       PB_WRITE_TEX_PIXEL( PB, x[i], y[i]-1, z[i],
			  red[i], green[i], blue[i], alpha[i], s[i], t[i], u[i] );
	       PB_WRITE_TEX_PIXEL( PB, x[i], y[i], z[i],
			  red[i], green[i], blue[i], alpha[i], s[i], t[i], u[i] );
	    }
	 }
      }
      else {
	 /* Y-major: duplicate pixels in X direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       PB_WRITE_TEX_PIXEL( PB, x[i]-1, y[i], z[i],
			  red[i], green[i], blue[i], alpha[i], s[i], t[i], u[i] );
	       PB_WRITE_TEX_PIXEL( PB, x[i], y[i], z[i],
			  red[i], green[i], blue[i], alpha[i], s[i], t[i], u[i] );
	    }
	 }
      }
      PB_CHECK_FLUSH( ctx, PB );
   }
   else {
      GLint width, w0, w1;
      width = (GLint) CLAMP( ctx->Line.Width, MIN_LINE_WIDTH, MAX_LINE_WIDTH );
      w0 = -width / 2;
      w1 = w0 + width - 1;

      if (dx>dy) {
	 /* X-major: duplicate pixels in Y direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       GLint yy;
	       GLint y0 = y[i] + w0;
	       GLint y1 = y[i] + w1;
	       for (yy=y0;yy<=y1;yy++) {
		  PB_WRITE_TEX_PIXEL( PB, x[i], yy, z[i],
			     red[i], green[i], blue[i], alpha[i], s[i], t[i], u[i] );
	       }
	       PB_CHECK_FLUSH( ctx, PB );
	    }
	 }
      }
      else {
	 /* Y-major: duplicate pixels in X direction */
	 for (i=0;i<n;i++) {
	    if (mask[i]) {
	       GLint xx;
	       GLint x0 = x[i] + w0;
	       GLint x1 = x[i] + w1;
	       for (xx=x0;xx<=x1;xx++) {
		  PB_WRITE_TEX_PIXEL( PB, xx, y[i], z[i],
			     red[i], green[i], blue[i], alpha[i], s[i], t[i], u[i] );
	       }
	       PB_CHECK_FLUSH( ctx, PB );
	    }
	 }
      }
   }

   /* Deallocate dynamic arrays on Mac */
   UNDEFARRAY(red);
   UNDEFARRAY(green);
   UNDEFARRAY(blue);
   UNDEFARRAY(alpha);
   UNDEFARRAY(s);
   UNDEFARRAY(t);
   UNDEFARRAY(u);
#if 0
   UNDEFARRAY(v); 
#endif
}



/*
 * Null rasterizer for measuring transformation speed.
 */
static void null_line( GLcontext *ctx, GLuint v1, GLuint v2, GLuint pv )
{
}



/*
 * Determine which line drawing function to use given the current
 * rendering context.
 */
void gl_set_line_function( GLcontext *ctx )
{
   GLboolean rgbmode = ctx->Visual->RGBAflag;
   /* TODO: antialiased lines */

   if (ctx->RenderMode==GL_RENDER) {
      if (ctx->NoRaster) {
         ctx->LineFunc = null_line;
         return;
      }
      if (ctx->Driver.LineFunc) {
         /* Device driver will draw lines. */
         ctx->LineFunc = ctx->Driver.LineFunc;
      }
      else if (gl_texturing_enabled(ctx)) {
	 ctx->LineFunc = textured_rgba_line;
      }
      else if (ctx->Line.Width!=1.0 || ctx->Line.StippleFlag
               || ctx->Line.SmoothFlag || ctx->Texture.Enabled) {
	 ctx->LineFunc = rgbmode ? general_rgba_line : general_ci_line;
      }
      else {
	 if (ctx->Light.ShadeModel==GL_SMOOTH) {
	    /* Width==1, non-stippled, smooth-shaded, any raster ops */
	    ctx->LineFunc = rgbmode ? smooth_rgba_line : smooth_ci_line;
	 }
         else {
	    /* Width==1, non-stippled, flat-shaded, any raster ops */
	    ctx->LineFunc = rgbmode ? flat_rgba_line : flat_ci_line;
         }
      }
   }
   else if (ctx->RenderMode==GL_FEEDBACK) {
      ctx->LineFunc = feedback_line;
   }
   else {
      /* GL_SELECT mode */
      ctx->LineFunc = select_line;
   }
}


