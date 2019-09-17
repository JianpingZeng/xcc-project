/* $Id: misc.c,v 1.11 1997/02/10 20:40:51 brianp Exp $ */

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
 * $Log: misc.c,v $
 * Revision 1.11  1997/02/10 20:40:51  brianp
 * added GL_MESA_resize_buffers to extensions string
 *
 * Revision 1.10  1997/02/09 18:44:35  brianp
 * added GL_EXT_texture3D support
 *
 * Revision 1.9  1997/01/08 20:55:02  brianp
 * added GL_EXT_texture_object
 *
 * Revision 1.8  1996/11/05 01:41:45  brianp
 * fixed potential scissor/clear color buffer bug
 *
 * Revision 1.7  1996/10/30 03:14:02  brianp
 * incremented version to 2.1
 *
 * Revision 1.6  1996/10/11 03:42:17  brianp
 * added GL_EXT_polygon_offset to extensions string
 *
 * Revision 1.5  1996/10/02 02:51:44  brianp
 * created clear_color_buffers() which handles draw mode GL_FRONT_AND_BACK
 *
 * Revision 1.4  1996/09/25 03:22:14  brianp
 * glDrawBuffer(GL_NONE) works now
 *
 * Revision 1.3  1996/09/24 00:16:10  brianp
 * set NewState flag in glRead/DrawBuffer() and glHint()
 * fixed display list bug in gl_Hint()
 *
 * Revision 1.2  1996/09/15 14:18:37  brianp
 * now use GLframebuffer and GLvisual
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#include <stdlib.h>
#include <string.h>
#include "accum.h"
#include "alphabuf.h"
#include "context.h"
#include "depth.h"
#include "macros.h"
#include "masking.h"
#include "misc.h"
#include "stencil.h"
#include "types.h"





void gl_ClearIndex( GLcontext *ctx, GLfloat c )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glClearIndex" );
      return;
   }
   ctx->Color.ClearIndex = (GLuint) c;
   if (!ctx->Visual->RGBAflag) {
      /* it's OK to call glClearIndex in RGBA mode but it should be a NOP */
      (*ctx->Driver.ClearIndex)( ctx, ctx->Color.ClearIndex );
   }
}



void gl_ClearColor( GLcontext *ctx, GLclampf red, GLclampf green,
                    GLclampf blue, GLclampf alpha )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glClearColor" );
      return;
   }

   ctx->Color.ClearColor[0] = CLAMP( red,   0.0F, 1.0F );
   ctx->Color.ClearColor[1] = CLAMP( green, 0.0F, 1.0F );
   ctx->Color.ClearColor[2] = CLAMP( blue,  0.0F, 1.0F );
   ctx->Color.ClearColor[3] = CLAMP( alpha, 0.0F, 1.0F );

   if (ctx->Visual->RGBAflag) {
      GLubyte r = (GLint) (ctx->Color.ClearColor[0] * ctx->Visual->RedScale);
      GLubyte g = (GLint) (ctx->Color.ClearColor[1] * ctx->Visual->GreenScale);
      GLubyte b = (GLint) (ctx->Color.ClearColor[2] * ctx->Visual->BlueScale);
      GLubyte a = (GLint) (ctx->Color.ClearColor[3] * ctx->Visual->AlphaScale);
      (*ctx->Driver.ClearColor)( ctx, r, g, b, a );
   }
}




/*
 * Clear the color buffer when glColorMask or glIndexMask is in effect.
 */
static void clear_color_buffer_with_masking( GLcontext *ctx )
{
   GLint x, y, height, width;

   /* Compute region to clear */
   if (ctx->Scissor.Enabled) {
      x = ctx->Buffer->Xmin;
      y = ctx->Buffer->Ymin;
      height = ctx->Buffer->Ymax - ctx->Buffer->Ymin + 1;
      width  = ctx->Buffer->Xmax - ctx->Buffer->Xmin + 1;
   }
   else {
      x = 0;
      y = 0;
      height = ctx->Buffer->Height;
      width  = ctx->Buffer->Width;
   }

   if (ctx->Visual->RGBAflag) {
      /* RGBA mode */
      GLubyte red[MAX_WIDTH], green[MAX_WIDTH];
      GLubyte blue[MAX_WIDTH], alpha[MAX_WIDTH];
      GLubyte r = ctx->Color.ClearColor[0] * ctx->Visual->RedScale;
      GLubyte g = ctx->Color.ClearColor[1] * ctx->Visual->GreenScale;
      GLubyte b = ctx->Color.ClearColor[2] * ctx->Visual->BlueScale;
      GLubyte a = ctx->Color.ClearColor[3] * ctx->Visual->AlphaScale;
      GLint i;
      for (i=0;i<height;i++,y++) {
         MEMSET( red,   (int) r, width );
         MEMSET( green, (int) g, width );
         MEMSET( blue,  (int) b, width );
         MEMSET( alpha, (int) a, width );
         gl_mask_color_span( ctx, width, x, y, red, green, blue, alpha );
         (*ctx->Driver.WriteColorSpan)( ctx,
                                 width, x, y, red, green, blue, alpha, NULL );
         if (ctx->RasterMask & ALPHABUF_BIT) {
            gl_write_alpha_span( ctx, width, x, y, alpha, NULL );
         }
      }
   }
   else {
      /* Color index mode */
      GLuint indx[MAX_WIDTH];
      GLubyte mask[MAX_WIDTH];
      GLint i, j;
      MEMSET( mask, 1, width );
      for (i=0;i<height;i++,y++) {
         for (j=0;j<width;j++) {
            indx[j] = ctx->Color.ClearIndex;
         }
         gl_mask_index_span( ctx, width, x, y, indx );
         (*ctx->Driver.WriteIndexSpan)( ctx, width, x, y, indx, mask );
      }
   }
}



/*
 * Clear the front and/or back color buffers.  Also clear the alpha
 * buffer(s) if present.
 */
static void clear_color_buffers( GLcontext *ctx )
{
   if (ctx->Color.SWmasking) {
      clear_color_buffer_with_masking( ctx );
   }
   else {
      GLint x = ctx->Buffer->Xmin;
      GLint y = ctx->Buffer->Ymin;
      GLint height = ctx->Buffer->Ymax - ctx->Buffer->Ymin + 1;
      GLint width  = ctx->Buffer->Xmax - ctx->Buffer->Xmin + 1;
      (*ctx->Driver.Clear)( ctx, !ctx->Scissor.Enabled,
                            x, y, width, height );
      if (ctx->RasterMask & ALPHABUF_BIT) {
         /* front and/or back alpha buffers will be cleared here */
         gl_clear_alpha_buffers( ctx );
      }
   }

   if (ctx->RasterMask & FRONT_AND_BACK_BIT) {
      /*** Also clear the back buffer ***/
      (*ctx->Driver.SetBuffer)( ctx, GL_BACK );
      if (ctx->Color.SWmasking) {
         clear_color_buffer_with_masking( ctx );
      }
      else {
         GLint x = ctx->Buffer->Xmin;
         GLint y = ctx->Buffer->Ymin;
         GLint height = ctx->Buffer->Ymax - ctx->Buffer->Ymin + 1;
         GLint width  = ctx->Buffer->Xmax - ctx->Buffer->Xmin + 1;
         (*ctx->Driver.Clear)( ctx, !ctx->Scissor.Enabled,
                               x, y, width, height );
      }
      (*ctx->Driver.SetBuffer)( ctx, GL_FRONT );
   }
}



void gl_Clear( GLcontext *ctx, GLbitfield mask )
{
#ifdef PROFILE
   GLdouble t0 = gl_time();
#endif

   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glClear" );
      return;
   }

   if (ctx->NewState) {
      gl_update_state( ctx );
   }

   if (mask & GL_COLOR_BUFFER_BIT)   clear_color_buffers( ctx );
   if (mask & GL_DEPTH_BUFFER_BIT)   (*ctx->Driver.ClearDepthBuffer)( ctx );
   if (mask & GL_ACCUM_BUFFER_BIT)   gl_clear_accum_buffer( ctx );
   if (mask & GL_STENCIL_BUFFER_BIT) gl_clear_stencil_buffer( ctx );

#ifdef PROFILE
   ctx->ClearTime += gl_time() - t0;
   ctx->ClearCount++;
#endif
}



const GLubyte *gl_GetString( GLcontext *ctx, GLenum name )
{
   static char *vendor = "Brian Paul";
   static char *renderer = "Mesa";
   static char *version = "1.1 Mesa 2.2";
   static char *extensions = "GL_EXT_blend_color GL_EXT_blend_minmax GL_EXT_blend_logic_op GL_EXT_blend_subtract GL_EXT_polygon_offset GL_EXT_vertex_array GL_EXT_texture_object GL_EXT_texture3D GL_MESA_window_pos GL_MESA_resize_buffers";

   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glGetString" );
      return (GLubyte *) 0;
   }

   switch (name) {
      case GL_VENDOR:
         return (GLubyte *) vendor;
      case GL_RENDERER:
         return (GLubyte *) renderer;
      case GL_VERSION:
         return (GLubyte *) version;
      case GL_EXTENSIONS:
         return (GLubyte *) extensions;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetString" );
         return (GLubyte *) 0;
   }
}



void gl_Finish( GLcontext *ctx )
{
   /* Don't compile into display list */
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glFinish" );
      return;
   }
   if (ctx->Driver.Finish) {
      (*ctx->Driver.Finish)( ctx );
   }
}



void gl_Flush( GLcontext *ctx )
{
   /* Don't compile into display list */
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glFlush" );
      return;
   }
   if (ctx->Driver.Flush) {
      (*ctx->Driver.Flush)( ctx );
   }
}



void gl_Hint( GLcontext *ctx, GLenum target, GLenum mode )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glHint" );
      return;
   }
   if (mode!=GL_DONT_CARE && mode!=GL_FASTEST && mode!=GL_NICEST) {
      gl_error( ctx, GL_INVALID_ENUM, "glHint(mode)" );
      return;
   }
   switch (target) {
      case GL_FOG_HINT:
         ctx->Hint.Fog = mode;
         break;
      case GL_LINE_SMOOTH_HINT:
         ctx->Hint.LineSmooth = mode;
         break;
      case GL_PERSPECTIVE_CORRECTION_HINT:
         ctx->Hint.PerspectiveCorrection = mode;
         break;
      case GL_POINT_SMOOTH_HINT:
         ctx->Hint.PointSmooth = mode;
         break;
      case GL_POLYGON_SMOOTH_HINT:
         ctx->Hint.PolygonSmooth = mode;
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glHint(target)" );
   }
   ctx->NewState |= NEW_ALL;   /* just to be safe */
}



void gl_DrawBuffer( GLcontext *ctx, GLenum mode )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glDrawBuffer" );
      return;
   }
   switch (mode) {
      case GL_FRONT:
      case GL_FRONT_LEFT:
      case GL_FRONT_AND_BACK:
         if ( (*ctx->Driver.SetBuffer)( ctx, GL_FRONT ) == GL_FALSE ) {
            gl_error( ctx, GL_INVALID_ENUM, "glDrawBuffer" );
            return;
         }
         ctx->Color.DrawBuffer = mode;
         ctx->Buffer->Alpha = ctx->Buffer->FrontAlpha;
	 ctx->NewState |= NEW_RASTER_OPS;
         break;
      case GL_BACK:
      case GL_BACK_LEFT:
         if ( (*ctx->Driver.SetBuffer)( ctx, GL_BACK ) == GL_FALSE) {
            gl_error( ctx, GL_INVALID_ENUM, "glDrawBuffer" );
            return;
         }
         ctx->Color.DrawBuffer = mode;
         ctx->Buffer->Alpha = ctx->Buffer->BackAlpha;
	 ctx->NewState |= NEW_RASTER_OPS;
         break;
      case GL_NONE:
         ctx->Color.DrawBuffer = mode;
         ctx->Buffer->Alpha = NULL;
         ctx->NewState |= NEW_RASTER_OPS;
         break;
      case GL_FRONT_RIGHT:
      case GL_BACK_RIGHT:
      case GL_LEFT:
      case GL_RIGHT:
      case GL_AUX0:
         gl_error( ctx, GL_INVALID_OPERATION, "glDrawBuffer" );
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glDrawBuffer" );
   }
}



void gl_ReadBuffer( GLcontext *ctx, GLenum mode )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glReadBuffer" );
      return;
   }
   switch (mode) {
      case GL_FRONT:
      case GL_FRONT_LEFT:
         if ( (*ctx->Driver.SetBuffer)( ctx, GL_FRONT ) == GL_FALSE) {
            gl_error( ctx, GL_INVALID_ENUM, "glReadBuffer" );
            return;
         }
         ctx->Pixel.ReadBuffer = mode;
         ctx->Buffer->Alpha = ctx->Buffer->FrontAlpha;
         ctx->NewState |= NEW_RASTER_OPS;
         break;
      case GL_BACK:
      case GL_BACK_LEFT:
         if ( (*ctx->Driver.SetBuffer)( ctx, GL_BACK ) == GL_FALSE) {
            gl_error( ctx, GL_INVALID_ENUM, "glReadBuffer" );
            return;
         }
         ctx->Pixel.ReadBuffer = mode;
         ctx->Buffer->Alpha = ctx->Buffer->BackAlpha;
         ctx->NewState |= NEW_RASTER_OPS;
         break;
      case GL_FRONT_RIGHT:
      case GL_BACK_RIGHT:
      case GL_LEFT:
      case GL_RIGHT:
      case GL_AUX0:
         gl_error( ctx, GL_INVALID_OPERATION, "glReadBuffer" );
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glReadBuffer" );
   }

   /* Remember, the draw buffer is the default state */
   (void) (*ctx->Driver.SetBuffer)( ctx, ctx->Color.DrawBuffer );
}




void gl_Rectf( GLcontext *ctx, GLfloat x1, GLfloat y1, GLfloat x2, GLfloat y2 )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glRect" );
      return;
   }
   (*ctx->API.Begin)( ctx, GL_QUADS );
   (*ctx->API.Vertex4f)( ctx, x1, y1, 0.0F, 1.0F );
   (*ctx->API.Vertex4f)( ctx, x2, y1, 0.0F, 1.0F );
   (*ctx->API.Vertex4f)( ctx, x2, y2, 0.0F, 1.0F );
   (*ctx->API.Vertex4f)( ctx, x1, y2, 0.0F, 1.0F );
   (*ctx->API.End)( ctx );
}

