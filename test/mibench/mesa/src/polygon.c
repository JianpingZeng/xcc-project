/* $Id: polygon.c,v 1.3 1996/10/11 03:44:58 brianp Exp $ */

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
 * $Log: polygon.c,v $
 * Revision 1.3  1996/10/11 03:44:58  brianp
 * removed Polygon.OffsetBias
 *
 * Revision 1.2  1996/09/26 22:48:40  brianp
 * set NEW_RASTER_OPS flag in gl_PolygonStipple() if stippling enabled
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include "context.h"
#include "macros.h"
#include "polygon.h"
#include "types.h"



void gl_CullFace( GLcontext *ctx, GLenum mode )
{
   if (mode!=GL_FRONT && mode!=GL_BACK && mode!=GL_FRONT_AND_BACK) {
      gl_error( ctx, GL_INVALID_ENUM, "glCullFace" );
      return;
   }
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glCullFace" );
      return;
   }
   ctx->Polygon.CullFaceMode = mode;
   ctx->NewState |= NEW_RASTER_OPS;
}



void gl_FrontFace( GLcontext *ctx, GLenum mode )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glFrontFace" );
      return;
   }
   if (mode!=GL_CW && mode!=GL_CCW) {
      gl_error( ctx, GL_INVALID_ENUM, "glFrontFace" );
      return;
   }
   ctx->Polygon.FrontFace = mode;
}



void gl_PolygonMode( GLcontext *ctx, GLenum face, GLenum mode )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glPolygonMode" );
      return;
   }
   if (face!=GL_FRONT && face!=GL_BACK && face!=GL_FRONT_AND_BACK) {
      gl_error( ctx, GL_INVALID_ENUM, "glPolygonMode(face)" );
      return;
   }
   else if (mode!=GL_POINT && mode!=GL_LINE && mode!=GL_FILL) {
      gl_error( ctx, GL_INVALID_ENUM, "glPolygonMode(mode)" );
      return;
   }

   if (face==GL_FRONT || face==GL_FRONT_AND_BACK) {
      ctx->Polygon.FrontMode = mode;
   }
   if (face==GL_BACK || face==GL_FRONT_AND_BACK) {
      ctx->Polygon.BackMode = mode;
   }

   /* Compute a handy "shortcut" value: */
   if (ctx->Polygon.FrontMode!=GL_FILL || ctx->Polygon.BackMode!=GL_FILL) {
      ctx->Polygon.Unfilled = GL_TRUE;
   }
   else {
      ctx->Polygon.Unfilled = GL_FALSE;
   }

   ctx->NewState |= NEW_RASTER_OPS;
}



void gl_PolygonStipple( GLcontext *ctx, const GLubyte *mask )
{
   /* TODO:  bit twiddling, unpacking */
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glPolygonStipple" );
      return;
   }
   MEMCPY( ctx->PolygonStipple, mask, 32*4 );
   if (ctx->Polygon.StippleFlag) {
      ctx->NewState |= NEW_RASTER_OPS;
   }
}



void gl_GetPolygonStipple( GLcontext *ctx, GLubyte *mask )
{
   /* TODO */
}



void gl_PolygonOffset( GLcontext *ctx,
                       GLfloat factor, GLfloat units )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glPolygonOffset" );
      return;
   }
   ctx->Polygon.OffsetFactor = factor;
   ctx->Polygon.OffsetUnits = units;
}

