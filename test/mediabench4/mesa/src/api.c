/* $Id: api.c,v 1.11 1997/02/19 18:09:10 brianp Exp $ */

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
 * $Log: api.c,v $
 * Revision 1.11  1997/02/19 18:09:10  brianp
 * now only print "no rendering context" error if MESA_DEBUG is set
 *
 * Revision 1.10  1997/02/10 19:49:29  brianp
 * added glResizeBuffersMESA() code
 *
 * Revision 1.9  1997/02/09 18:49:52  brianp
 * added GL_EXT_texture3D support
 *
 * Revision 1.8  1997/01/08 20:55:25  brianp
 * added GL_EXT_texture_object API functions
 *
 * Revision 1.7  1996/11/09 01:41:17  brianp
 * check if there's no rendering context and return gracefully
 *
 * Revision 1.6  1996/11/07 04:12:13  brianp
 * glTexImage[12]D() reimplemented
 *
 * Revision 1.5  1996/10/11 03:41:44  brianp
 * added glPolygonOffsetEXT()
 *
 * Revision 1.4  1996/09/27 01:23:50  brianp
 * added extra error checking when DEBUG is defined
 *
 * Revision 1.3  1996/09/26 22:52:12  brianp
 * added glInterleavedArrays
 *
 * Revision 1.2  1996/09/14 06:27:22  brianp
 * some functions didn't return needed values
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#include <stdio.h>
#include <stdlib.h>
#include "bitmap.h"
#include "context.h"
#include "eval.h"
#include "image.h"
#include "macros.h"
#include "matrix.h"
#include "teximage.h"
#include "types.h"



#ifdef MULTI_THREADING

/* Get the context associated with the calling thread */
#define GET_CONTEXT	GLcontext *CC = gl_get_thread_context()

#else

/* CC is a global pointer for all threads in the address space */
#define GET_CONTEXT

#endif /*MULTI_THREADED*/



#define CHECK_CONTEXT							\
   if (!CC) {								\
      if (getenv("MESA_DEBUG")) {					\
	 fprintf(stderr,"Mesa user error: no rendering context.\n");	\
      }									\
      return;								\
   }

#define CHECK_CONTEXT_RETURN(R)						\
   if (!CC) {								\
      if (getenv("MESA_DEBUG")) {					\
         fprintf(stderr,"Mesa user error: no rendering context.\n");	\
      }									\
      return (R);							\
   }



#define SHORTCUT


void glAccum( GLenum op, GLfloat value )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Accum)(CC, op, value);
}


void glAlphaFunc( GLenum func, GLclampf ref )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.AlphaFunc)(CC, func, ref);
}


GLboolean glAreTexturesResident( GLsizei n, const GLuint *textures,
                                 GLboolean *residences )
{
   GET_CONTEXT;
   CHECK_CONTEXT_RETURN(GL_FALSE);
   return (*CC->API.AreTexturesResident)(CC, n, textures, residences);
}


void glArrayElement( GLint i )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ArrayElement)(CC, i);
}


void glBegin( GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Begin)( CC, mode );
}


void glBindTexture( GLenum target, GLuint texture )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.BindTexture)(CC, target, texture);
}


void glBitmap( GLsizei width, GLsizei height,
               GLfloat xorig, GLfloat yorig,
               GLfloat xmove, GLfloat ymove,
               const GLubyte *bitmap )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   if (!CC->CompileFlag) {
      /* execute only, try optimized case where no unpacking needed */
      if (   CC->Unpack.LsbFirst==GL_FALSE
          && CC->Unpack.Alignment==1
          && CC->Unpack.RowLength==0
          && CC->Unpack.SkipPixels==0
          && CC->Unpack.SkipRows==0) {
         /* Special case: no unpacking needed */
         struct gl_image image;
         image.Width = width;
         image.Height = height;
         image.Components = 0;
         image.Type = GL_BITMAP;
         image.Data = (GLvoid *) bitmap;
         (*CC->Exec.Bitmap)( CC, width, height, xorig, yorig,
                             xmove, ymove, &image );
      }
      else {
         struct gl_image *image;
         image = gl_unpack_bitmap( CC, width, height, bitmap );
         (*CC->Exec.Bitmap)( CC, width, height, xorig, yorig,
                             xmove, ymove, image );
         gl_free_image( image );
      }
   }
   else {
      /* compile and maybe execute */
      struct gl_image *image;
      image = gl_unpack_bitmap( CC, width, height, bitmap );
      (*CC->API.Bitmap)(CC, width, height, xorig, yorig, xmove, ymove, image );
   }
}


void glBlendFunc( GLenum sfactor, GLenum dfactor )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.BlendFunc)(CC, sfactor, dfactor);
}


void glCallList( GLuint list )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.CallList)(CC, list);
}


void glCallLists( GLsizei n, GLenum type, const GLvoid *lists )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.CallLists)(CC, n, type, lists);
}


void glClear( GLbitfield mask )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Clear)(CC, mask);
}


void glClearAccum( GLfloat red, GLfloat green,
			  GLfloat blue, GLfloat alpha )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ClearAccum)(CC, red, green, blue, alpha);
}



void glClearIndex( GLfloat c )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ClearIndex)(CC, c);
}


void glClearColor( GLclampf red,
			  GLclampf green,
			  GLclampf blue,
			  GLclampf alpha )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ClearColor)(CC, red, green, blue, alpha);
}


void glClearDepth( GLclampd depth )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ClearDepth)( CC, depth );
}


void glClearStencil( GLint s )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ClearStencil)(CC, s);
}


void glClipPlane( GLenum plane, const GLdouble *equation )
{
   GLfloat eq[4];
   GET_CONTEXT;
   CHECK_CONTEXT;
   eq[0] = (GLfloat) equation[0];
   eq[1] = (GLfloat) equation[1];
   eq[2] = (GLfloat) equation[2];
   eq[3] = (GLfloat) equation[3];
   (*CC->API.ClipPlane)(CC, plane, eq );
}


void glColor3b( GLbyte red, GLbyte green, GLbyte blue )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, BYTE_TO_FLOAT(red), BYTE_TO_FLOAT(green),
                           BYTE_TO_FLOAT(blue), 1.0F );
}


void glColor3d( GLdouble red, GLdouble green, GLdouble blue )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, (GLfloat) red, (GLfloat) green,
                           (GLfloat) blue, 1.0F );
}


void glColor3f( GLfloat red, GLfloat green, GLfloat blue )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, red, green, blue, 1.0F );
}


void glColor3i( GLint red, GLint green, GLint blue )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, INT_TO_FLOAT(red), INT_TO_FLOAT(green),
                           INT_TO_FLOAT(blue), 1.0F );
}


void glColor3s( GLshort red, GLshort green, GLshort blue )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, SHORT_TO_FLOAT(red), SHORT_TO_FLOAT(green),
                           SHORT_TO_FLOAT(blue), 1.0F );
}


void glColor3ub( GLubyte red, GLubyte green, GLubyte blue )
{
   GET_CONTEXT;
   (*CC->API.Color4ub)( CC, red, green, blue, 255 );
}


void glColor3ui( GLuint red, GLuint green, GLuint blue )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, UINT_TO_FLOAT(red), UINT_TO_FLOAT(green),
                           UINT_TO_FLOAT(blue), 1.0F );
}


void glColor3us( GLushort red, GLushort green, GLushort blue )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, USHORT_TO_FLOAT(red), USHORT_TO_FLOAT(green),
                           USHORT_TO_FLOAT(blue), 1.0F );
}


void glColor4b( GLbyte red, GLbyte green, GLbyte blue, GLbyte alpha )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, BYTE_TO_FLOAT(red), BYTE_TO_FLOAT(green),
                           BYTE_TO_FLOAT(blue), BYTE_TO_FLOAT(alpha) );
}


void glColor4d( GLdouble red, GLdouble green, GLdouble blue, GLdouble alpha )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, (GLfloat) red, (GLfloat) green,
                           (GLfloat) blue, (GLfloat) alpha );
}


void glColor4f( GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, red, green, blue, alpha );
}

void glColor4i( GLint red, GLint green, GLint blue, GLint alpha )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, INT_TO_FLOAT(red), INT_TO_FLOAT(green),
                           INT_TO_FLOAT(blue), INT_TO_FLOAT(alpha) );
}


void glColor4s( GLshort red, GLshort green, GLshort blue, GLshort alpha )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, SHORT_TO_FLOAT(red), SHORT_TO_FLOAT(green),
                           SHORT_TO_FLOAT(blue), SHORT_TO_FLOAT(alpha) );
}

void glColor4ub( GLubyte red, GLubyte green, GLubyte blue, GLubyte alpha )
{
   GET_CONTEXT;
   (*CC->API.Color4ub)( CC, red, green, blue, alpha );
}

void glColor4ui( GLuint red, GLuint green, GLuint blue, GLuint alpha )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, UINT_TO_FLOAT(red), UINT_TO_FLOAT(green),
                           UINT_TO_FLOAT(blue), UINT_TO_FLOAT(alpha) );
}

void glColor4us( GLushort red, GLushort green, GLushort blue, GLushort alpha )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, USHORT_TO_FLOAT(red), USHORT_TO_FLOAT(green),
                           USHORT_TO_FLOAT(blue), USHORT_TO_FLOAT(alpha) );
}


void glColor3bv( const GLbyte *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, BYTE_TO_FLOAT(v[0]), BYTE_TO_FLOAT(v[1]),
                           BYTE_TO_FLOAT(v[2]), 1.0F );
}


void glColor3dv( const GLdouble *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, (GLdouble) v[0], (GLdouble) v[1],
                           (GLdouble) v[2], 1.0F );
}


void glColor3fv( const GLfloat *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, v[0],v [1], v[2], 1.0F );
}


void glColor3iv( const GLint *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, INT_TO_FLOAT(v[0]), INT_TO_FLOAT(v[1]),
                           INT_TO_FLOAT(v[2]), 1.0F );
}


void glColor3sv( const GLshort *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, SHORT_TO_FLOAT(v[0]), SHORT_TO_FLOAT(v[1]),
                           SHORT_TO_FLOAT(v[2]), 1.0F );
}


void glColor3ubv( const GLubyte *v )
{
   GET_CONTEXT;
   (*CC->API.Color4ub)( CC, v[0], v[1], v[2], 255 );
}


void glColor3uiv( const GLuint *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, UINT_TO_FLOAT(v[0]), UINT_TO_FLOAT(v[1]),
                           UINT_TO_FLOAT(v[2]), 1.0F );
}


void glColor3usv( const GLushort *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, USHORT_TO_FLOAT(v[0]), USHORT_TO_FLOAT(v[1]),
                           USHORT_TO_FLOAT(v[2]), 1.0F );

}


void glColor4bv( const GLbyte *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, BYTE_TO_FLOAT(v[0]), BYTE_TO_FLOAT(v[1]),
                           BYTE_TO_FLOAT(v[2]), BYTE_TO_FLOAT(v[3]) );
}


void glColor4dv( const GLdouble *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, (GLdouble) v[0], (GLdouble) v[1],
                           (GLdouble) v[2], (GLdouble) v[3] );
}


void glColor4fv( const GLfloat *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, v[0], v[1], v[2], v[3] );
}


void glColor4iv( const GLint *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, INT_TO_FLOAT(v[0]), INT_TO_FLOAT(v[1]),
                           INT_TO_FLOAT(v[2]), INT_TO_FLOAT(v[3]) );
}


void glColor4sv( const GLshort *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, SHORT_TO_FLOAT(v[0]), SHORT_TO_FLOAT(v[1]),
                           SHORT_TO_FLOAT(v[2]), SHORT_TO_FLOAT(v[3]) );
}


void glColor4ubv( const GLubyte *v )
{
   GET_CONTEXT;
   (*CC->API.Color4ub)( CC, v[0], v[1], v[2], v[3] );
}


void glColor4uiv( const GLuint *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, UINT_TO_FLOAT(v[0]), UINT_TO_FLOAT(v[1]),
                           UINT_TO_FLOAT(v[2]), UINT_TO_FLOAT(v[3]) );
}


void glColor4usv( const GLushort *v )
{
   GET_CONTEXT;
   (*CC->API.Color4f)( CC, USHORT_TO_FLOAT(v[0]), USHORT_TO_FLOAT(v[1]),
                           USHORT_TO_FLOAT(v[2]), USHORT_TO_FLOAT(v[3]) );
}


void glColorMask( GLboolean red, GLboolean green,
			 GLboolean blue, GLboolean alpha )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ColorMask)(CC, red, green, blue, alpha);
}


void glColorMaterial( GLenum face, GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ColorMaterial)(CC, face, mode);
}


void glColorPointer( GLint size, GLenum type, GLsizei stride,
                     const GLvoid *ptr )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ColorPointer)(CC, size, type, stride, ptr);
}


void glCopyPixels( GLint x, GLint y, GLsizei width, GLsizei height,
			  GLenum type )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.CopyPixels)(CC, x, y, width, height, type);
}


void glCopyTexImage1D( GLenum target, GLint level,
                       GLenum internalformat,
                       GLint x, GLint y,
                       GLsizei width, GLint border )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.CopyTexImage1D)( CC, target, level, internalformat,
                                 x, y, width, border );
}


void glCopyTexImage2D( GLenum target, GLint level,
                       GLenum internalformat,
                       GLint x, GLint y,
                       GLsizei width, GLsizei height, GLint border )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.CopyTexImage2D)( CC, target, level, internalformat,
                              x, y, width, height, border );
}


void glCopyTexSubImage1D( GLenum target, GLint level,
                          GLint xoffset, GLint x, GLint y,
                          GLsizei width )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.CopyTexSubImage1D)( CC, target, level, xoffset, x, y, width );
}


void glCopyTexSubImage2D( GLenum target, GLint level,
                          GLint xoffset, GLint yoffset,
                          GLint x, GLint y,
                          GLsizei width, GLsizei height )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.CopyTexSubImage2D)( CC, target, level, xoffset, yoffset,
                                 x, y, width, height );
}



void glCullFace( GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.CullFace)(CC, mode);
}


void glDepthFunc( GLenum func )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.DepthFunc)( CC, func );
}


void glDepthMask( GLboolean flag )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.DepthMask)( CC, flag );
}


void glDepthRange( GLclampd near_val, GLclampd far_val )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.DepthRange)( CC, near_val, far_val );
}


void glDeleteLists( GLuint list, GLsizei range )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.DeleteLists)(CC, list, range);
}


void glDeleteTextures( GLsizei n, const GLuint *textures)
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.DeleteTextures)(CC, n, textures);
}


void glDisable( GLenum cap )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Disable)( CC, cap );
}


void glDisableClientState( GLenum cap )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.DisableClientState)( CC, cap );
}


void glDrawArrays( GLenum mode, GLint first, GLsizei count )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.DrawArrays)(CC, mode, first, count);
}


void glDrawBuffer( GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.DrawBuffer)(CC, mode);
}


void glDrawElements( GLenum mode, GLsizei count,
                     GLenum type, const GLvoid *indices )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.DrawElements)( CC, mode, count, type, indices );
}


void glDrawPixels( GLsizei width, GLsizei height,
                   GLenum format, GLenum type, const GLvoid *pixels )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.DrawPixels)( CC, width, height, format, type, pixels );
}


void glEnable( GLenum cap )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Enable)( CC, cap );
}


void glEnableClientState( GLenum cap )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EnableClientState)( CC, cap );
}


void glEnd( void )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.End)( CC );
}


void glEndList( void )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EndList)(CC);
}




void glEvalCoord1d( GLdouble u )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalCoord1f)( CC, (GLfloat) u );
}


void glEvalCoord1f( GLfloat u )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalCoord1f)( CC, u );
}


void glEvalCoord1dv( const GLdouble *u )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalCoord1f)( CC, (GLfloat) *u );
}


void glEvalCoord1fv( const GLfloat *u )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalCoord1f)( CC, (GLfloat) *u );
}


void glEvalCoord2d( GLdouble u, GLdouble v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalCoord2f)( CC, (GLfloat) u, (GLfloat) v );
}


void glEvalCoord2f( GLfloat u, GLfloat v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalCoord2f)( CC, u, v );
}


void glEvalCoord2dv( const GLdouble *u )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalCoord2f)( CC, (GLfloat) u[0], (GLfloat) u[1] );
}


void glEvalCoord2fv( const GLfloat *u )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalCoord2f)( CC, u[0], u[1] );
}


void glEvalPoint1( GLint i )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalPoint1)( CC, i );
}


void glEvalPoint2( GLint i, GLint j )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalPoint2)( CC, i, j );
}


void glEvalMesh1( GLenum mode, GLint i1, GLint i2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalMesh1)( CC, mode, i1, i2 );
}


void glEdgeFlag( GLboolean flag )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EdgeFlag)(CC, flag);
}


void glEdgeFlagv( const GLboolean *flag )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EdgeFlag)(CC, *flag);
}


void glEdgeFlagPointer( GLsizei stride, const GLboolean *ptr )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EdgeFlagPointer)(CC, stride, ptr);
}


void glEvalMesh2( GLenum mode, GLint i1, GLint i2, GLint j1, GLint j2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EvalMesh2)( CC, mode, i1, i2, j1, j2 );
}


void glFeedbackBuffer( GLsizei size, GLenum type, GLfloat *buffer )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.FeedbackBuffer)(CC, size, type, buffer);
}


void glFinish( void )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Finish)(CC);
}


void glFlush( void )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Flush)(CC);
}


void glFogf( GLenum pname, GLfloat param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Fogfv)(CC, pname, &param);
}


void glFogi( GLenum pname, GLint param )
{
   GLfloat fparam = (GLfloat) param;
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Fogfv)(CC, pname, &fparam);
}


void glFogfv( GLenum pname, const GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Fogfv)(CC, pname, params);
}


void glFogiv( GLenum pname, const GLint *params )
{
   GLfloat p[4];
   GET_CONTEXT;
   CHECK_CONTEXT;

   switch (pname) {
      case GL_FOG_MODE:
      case GL_FOG_DENSITY:
      case GL_FOG_START:
      case GL_FOG_END:
      case GL_FOG_INDEX:
         p[0] = (GLfloat) *params;
	 break;
      case GL_FOG_COLOR:
	 p[0] = INT_TO_FLOAT( params[0] );
	 p[1] = INT_TO_FLOAT( params[1] );
	 p[2] = INT_TO_FLOAT( params[2] );
	 p[3] = INT_TO_FLOAT( params[3] );
	 break;
      default:
         /* Error will be caught later in gl_Fogfv */
         ;
   }
   (*CC->API.Fogfv)( CC, pname, p );
}



void glFrontFace( GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.FrontFace)(CC, mode);
}


void glFrustum( GLdouble left, GLdouble right,
				GLdouble bottom, GLdouble top,
				GLdouble nearval, GLdouble farval )
{
   GLfloat x, y, a, b, c, d;
   GLfloat m[16];
   GET_CONTEXT;
   CHECK_CONTEXT;

   if (nearval<=0.0 || farval<=0.0) {
      gl_error( CC, GL_INVALID_VALUE, "glFrustum(near or far)" );
   }

   x = (2.0*nearval) / (right-left);
   y = (2.0*nearval) / (top-bottom);
   a = (right+left) / (right-left);
   b = (top+bottom) / (top-bottom);
   c = -(farval+nearval) / ( farval-nearval);
   d = -(2.0*farval*nearval) / (farval-nearval);  /* error? */

#define M(row,col)  m[col*4+row]
   M(0,0) = x;     M(0,1) = 0.0F;  M(0,2) = a;      M(0,3) = 0.0F;
   M(1,0) = 0.0F;  M(1,1) = y;     M(1,2) = b;      M(1,3) = 0.0F;
   M(2,0) = 0.0F;  M(2,1) = 0.0F;  M(2,2) = c;      M(2,3) = d;
   M(3,0) = 0.0F;  M(3,1) = 0.0F;  M(3,2) = -1.0F;  M(3,3) = 0.0F;
#undef M

   (*CC->API.MultMatrixf)( CC, m );
}


GLuint glGenLists( GLsizei range )
{
   GET_CONTEXT;
   CHECK_CONTEXT_RETURN(0);
   return (*CC->API.GenLists)(CC, range);
}


void glGenTextures( GLsizei n, GLuint *textures )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GenTextures)(CC, n, textures);
}


void glGetBooleanv( GLenum pname, GLboolean *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetBooleanv)(CC, pname, params);
}


void glGetClipPlane( GLenum plane, GLdouble *equation )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetClipPlane)(CC, plane, equation);
}


void glGetDoublev( GLenum pname, GLdouble *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetDoublev)(CC, pname, params);
}


GLenum glGetError( void )
{
   GET_CONTEXT;
   if (!CC) {
      /* No current context */
      return GL_NO_ERROR;
   }
   return (*CC->API.GetError)(CC);
}


void glGetFloatv( GLenum pname, GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetFloatv)(CC, pname, params);
}


void glGetIntegerv( GLenum pname, GLint *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetIntegerv)(CC, pname, params);
}


void glGetLightfv( GLenum light, GLenum pname, GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetLightfv)(CC, light, pname, params);
}


void glGetLightiv( GLenum light, GLenum pname, GLint *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetLightiv)(CC, light, pname, params);
}


void glGetMapdv( GLenum target, GLenum query, GLdouble *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetMapdv)( CC, target, query, v );
}


void glGetMapfv( GLenum target, GLenum query, GLfloat *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetMapfv)( CC, target, query, v );
}


void glGetMapiv( GLenum target, GLenum query, GLint *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetMapiv)( CC, target, query, v );
}


void glGetMaterialfv( GLenum face, GLenum pname, GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetMaterialfv)(CC, face, pname, params);
}


void glGetMaterialiv( GLenum face, GLenum pname, GLint *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetMaterialiv)(CC, face, pname, params);
}


void glGetPixelMapfv( GLenum map, GLfloat *values )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetPixelMapfv)(CC, map, values);
}


void glGetPixelMapuiv( GLenum map, GLuint *values )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetPixelMapuiv)(CC, map, values);
}


void glGetPixelMapusv( GLenum map, GLushort *values )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetPixelMapusv)(CC, map, values);
}


void glGetPointerv( GLenum pname, GLvoid **params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetPointerv)(CC, pname, params);
}


void glGetPolygonStipple( GLubyte *mask )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetPolygonStipple)(CC, mask);
}


const GLubyte *glGetString( GLenum name )
{
   GET_CONTEXT;
   CHECK_CONTEXT_RETURN(NULL);
   return (*CC->API.GetString)(CC, name);
}



void glGetTexEnvfv( GLenum target, GLenum pname, GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetTexEnvfv)(CC, target, pname, params);
}


void glGetTexEnviv( GLenum target, GLenum pname, GLint *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetTexEnviv)(CC, target, pname, params);
}


void glGetTexGeniv( GLenum coord, GLenum pname, GLint *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetTexGeniv)(CC, coord, pname, params);
}


void glGetTexGendv( GLenum coord, GLenum pname, GLdouble *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetTexGendv)(CC, coord, pname, params);
}


void glGetTexGenfv( GLenum coord, GLenum pname, GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetTexGenfv)(CC, coord, pname, params);
}



void glGetTexImage( GLenum target, GLint level, GLenum format,
      			GLenum type, GLvoid *pixels )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetTexImage)(CC, target, level, format, type, pixels);
}


void glGetTexLevelParameterfv( GLenum target, GLint level,
                               GLenum pname, GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetTexLevelParameterfv)(CC, target, level, pname, params);
}


void glGetTexLevelParameteriv( GLenum target, GLint level,
                               GLenum pname, GLint *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetTexLevelParameteriv)(CC, target, level, pname, params);
}




void glGetTexParameterfv( GLenum target, GLenum pname, GLfloat *params)
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetTexParameterfv)(CC, target, pname, params);
}


void glGetTexParameteriv( GLenum target, GLenum pname, GLint *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetTexParameteriv)(CC, target, pname, params);
}


void glHint( GLenum target, GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Hint)(CC, target, mode);
}


void glIndexd( GLdouble c )
{
   GET_CONTEXT;
   (*CC->API.Indexf)( CC, (GLfloat) c );
}


void glIndexf( GLfloat c )
{
   GET_CONTEXT;
   (*CC->API.Indexf)( CC, c );
}


void glIndexi( GLint c )
{
   GET_CONTEXT;
   (*CC->API.Indexi)( CC, c );
}


void glIndexs( GLshort c )
{
   GET_CONTEXT;
   (*CC->API.Indexi)( CC, (GLint) c );
}


#ifdef GL_VERSION_1_1
void glIndexub( GLubyte c )
{
   GET_CONTEXT;
   (*CC->API.Indexi)( CC, (GLint) c );
}
#endif


void glIndexdv( const GLdouble *c )
{
   GET_CONTEXT;
   (*CC->API.Indexf)( CC, (GLfloat) *c );
}


void glIndexfv( const GLfloat *c )
{
   GET_CONTEXT;
   (*CC->API.Indexf)( CC, *c );
}


void glIndexiv( const GLint *c )
{
   GET_CONTEXT;
   (*CC->API.Indexi)( CC, *c );
}


void glIndexsv( const GLshort *c )
{
   GET_CONTEXT;
   (*CC->API.Indexi)( CC, (GLint) *c );
}


#ifdef GL_VERSION_1_1
void glIndexubv( const GLubyte *c )
{
   GET_CONTEXT;
   (*CC->API.Indexi)( CC, (GLint) *c );
}
#endif


void glIndexMask( GLuint mask )
{
   GET_CONTEXT;
   (*CC->API.IndexMask)(CC, mask);
}


void glIndexPointer( GLenum type, GLsizei stride, const GLvoid *ptr )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.IndexPointer)(CC, type, stride, ptr);
}


void glInterleavedArrays( GLenum format, GLsizei stride,
                          const GLvoid *pointer )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.InterleavedArrays)( CC, format, stride, pointer );
}


void glInitNames( void )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.InitNames)(CC);
}


GLboolean glIsList( GLuint list )
{
   GET_CONTEXT;
   CHECK_CONTEXT_RETURN(GL_FALSE);
   return (*CC->API.IsList)(CC, list);
}


GLboolean glIsTexture( GLuint texture )
{
   GET_CONTEXT;
   CHECK_CONTEXT_RETURN(GL_FALSE);
   return (*CC->API.IsTexture)(CC, texture);
}


void glLightf( GLenum light, GLenum pname, GLfloat param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Lightfv)( CC, light, pname, &param, 1 );
}



void glLighti( GLenum light, GLenum pname, GLint param )
{
   GLfloat fparam = (GLfloat) param;
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Lightfv)( CC, light, pname, &fparam, 1 );
}



void glLightfv( GLenum light, GLenum pname, const GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Lightfv)( CC, light, pname, params, 4 );
}



void glLightiv( GLenum light, GLenum pname, const GLint *params )
{
   GLfloat fparam[4];
   GET_CONTEXT;
   CHECK_CONTEXT;

   switch (pname) {
      case GL_AMBIENT:
      case GL_DIFFUSE:
      case GL_SPECULAR:
         fparam[0] = INT_TO_FLOAT( params[0] );
         fparam[1] = INT_TO_FLOAT( params[1] );
         fparam[2] = INT_TO_FLOAT( params[2] );
         fparam[3] = INT_TO_FLOAT( params[3] );
         break;
      case GL_POSITION:
         fparam[0] = (GLfloat) params[0];
         fparam[1] = (GLfloat) params[1];
         fparam[2] = (GLfloat) params[2];
         fparam[3] = (GLfloat) params[3];
         break;
      case GL_SPOT_DIRECTION:
         fparam[0] = (GLfloat) params[0];
         fparam[1] = (GLfloat) params[1];
         fparam[2] = (GLfloat) params[2];
         break;
      case GL_SPOT_EXPONENT:
      case GL_SPOT_CUTOFF:
      case GL_CONSTANT_ATTENUATION:
      case GL_LINEAR_ATTENUATION:
      case GL_QUADRATIC_ATTENUATION:
         fparam[0] = (GLfloat) params[0];
         break;
      default:
         /* error will be caught later in gl_Lightfv */
         ;
   }
   (*CC->API.Lightfv)( CC, light, pname, fparam, 4 );
}



void glLightModelf( GLenum pname, GLfloat param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.LightModelfv)( CC, pname, &param );
}


void glLightModeli( GLenum pname, GLint param )
{
   GLfloat fparam[4];
   GET_CONTEXT;
   CHECK_CONTEXT;
   fparam[0] = (GLfloat) param;
   (*CC->API.LightModelfv)( CC, pname, fparam );
}


void glLightModelfv( GLenum pname, const GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.LightModelfv)( CC, pname, params );
}


void glLightModeliv( GLenum pname, const GLint *params )
{
   GLfloat fparam[4];
   GET_CONTEXT;
   CHECK_CONTEXT;

   switch (pname) {
      case GL_LIGHT_MODEL_AMBIENT:
         fparam[0] = INT_TO_FLOAT( params[0] );
         fparam[1] = INT_TO_FLOAT( params[1] );
         fparam[2] = INT_TO_FLOAT( params[2] );
         fparam[3] = INT_TO_FLOAT( params[3] );
         break;
      case GL_LIGHT_MODEL_LOCAL_VIEWER:
      case GL_LIGHT_MODEL_TWO_SIDE:
         fparam[0] = (GLfloat) params[0];
         break;
      default:
         /* Error will be caught later in gl_LightModelfv */
         ;
   }
   (*CC->API.LightModelfv)( CC, pname, fparam );
}


void glLineWidth( GLfloat width )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.LineWidth)(CC, width);
}


void glLineStipple( GLint factor, GLushort pattern )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.LineStipple)(CC, factor, pattern);
}


void glListBase( GLuint base )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ListBase)(CC, base);
}


void glLoadIdentity( void )
{
   static GLfloat identity[16] = {
      1.0, 0.0, 0.0, 0.0,
      0.0, 1.0, 0.0, 0.0,
      0.0, 0.0, 1.0, 0.0,
      0.0, 0.0, 0.0, 1.0
   };
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.LoadMatrixf)( CC, identity );
}


void glLoadMatrixd( const GLdouble *m )
{
   GLfloat fm[16];
   GLuint i;
   GET_CONTEXT;
   CHECK_CONTEXT;

   for (i=0;i<16;i++) {
      fm[i] = (GLfloat) m[i];
   }

   (*CC->API.LoadMatrixf)( CC, fm );
}


void glLoadMatrixf( const GLfloat *m )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.LoadMatrixf)( CC, m );
}


void glLoadName( GLuint name )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.LoadName)(CC, name);
}


void glLogicOp( GLenum opcode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.LogicOp)(CC, opcode);
}



void glMap1d( GLenum target, GLdouble u1, GLdouble u2, GLint stride,
              GLint order, const GLdouble *points )
{
   GLfloat *pnts;
   GLboolean retain;
   GET_CONTEXT;
   CHECK_CONTEXT;

   pnts = gl_copy_map_points1d( target, stride, order, points );
   retain = CC->CompileFlag;
   (*CC->API.Map1f)( CC, target, u1, u2, stride, order, pnts, retain );
}


void glMap1f( GLenum target, GLfloat u1, GLfloat u2, GLint stride,
              GLint order, const GLfloat *points )
{
   GLfloat *pnts;
   GLboolean retain;
   GET_CONTEXT;
   CHECK_CONTEXT;

   pnts = gl_copy_map_points1f( target, stride, order, points );
   retain = CC->CompileFlag;
   (*CC->API.Map1f)( CC, target, u1, u2, stride, order, pnts, retain );
}


void glMap2d( GLenum target,
              GLdouble u1, GLdouble u2, GLint ustride, GLint uorder,
              GLdouble v1, GLdouble v2, GLint vstride, GLint vorder,
              const GLdouble *points )
{
   GLfloat *pnts;
   GLboolean retain;
   GET_CONTEXT;
   CHECK_CONTEXT;

   pnts = gl_copy_map_points2d( target, ustride, uorder,
                                vstride, vorder, points );
   retain = CC->CompileFlag;
   (*CC->API.Map2f)( CC, target, u1, u2, ustride, uorder,
                     v1, v2, vstride, vorder, pnts, retain );
}


void glMap2f( GLenum target,
              GLfloat u1, GLfloat u2, GLint ustride, GLint uorder,
              GLfloat v1, GLfloat v2, GLint vstride, GLint vorder,
              const GLfloat *points )
{
   GLfloat *pnts;
   GLboolean retain;
   GET_CONTEXT;
   CHECK_CONTEXT;

   pnts = gl_copy_map_points2f( target, ustride, uorder,
                                vstride, vorder, points );
   retain = CC->CompileFlag;
   (*CC->API.Map2f)( CC, target, u1, u2, ustride, uorder,
                     v1, v2, vstride, vorder, pnts, retain );
}


void glMapGrid1d( GLint un, GLdouble u1, GLdouble u2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.MapGrid1f)( CC, un, (GLfloat) u1, (GLfloat) u2 );
}


void glMapGrid1f( GLint un, GLfloat u1, GLfloat u2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.MapGrid1f)( CC, un, u1, u2 );
}


void glMapGrid2d( GLint un, GLdouble u1, GLdouble u2,
          GLint vn, GLdouble v1, GLdouble v2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.MapGrid2f)( CC, un, (GLfloat) u1, (GLfloat) u2,
                                  vn, (GLfloat) v1, (GLfloat) v2 );
}


void glMapGrid2f( GLint un, GLfloat u1, GLfloat u2,
          GLint vn, GLfloat v1, GLfloat v2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.MapGrid2f)( CC, un, u1, u2, vn, v1, v2 );
}


void glMaterialf( GLenum face, GLenum pname, GLfloat param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Materialfv)( CC, face, pname, &param );
}



void glMateriali( GLenum face, GLenum pname, GLint param )
{
   GLfloat fparam[4];
   GET_CONTEXT;
   CHECK_CONTEXT;
   fparam[0] = (GLfloat) param;
   (*CC->API.Materialfv)( CC, face, pname, fparam );
}


void glMaterialfv( GLenum face, GLenum pname, const GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Materialfv)( CC, face, pname, params );
}


void glMaterialiv( GLenum face, GLenum pname, const GLint *params )
{
   GLfloat fparam[4];
   GET_CONTEXT;
   CHECK_CONTEXT;
   switch (pname) {
      case GL_AMBIENT:
      case GL_DIFFUSE:
      case GL_SPECULAR:
      case GL_EMISSION:
      case GL_AMBIENT_AND_DIFFUSE:
         fparam[0] = INT_TO_FLOAT( params[0] );
         fparam[1] = INT_TO_FLOAT( params[1] );
         fparam[2] = INT_TO_FLOAT( params[2] );
         fparam[3] = INT_TO_FLOAT( params[3] );
         break;
      case GL_SHININESS:
         fparam[0] = (GLfloat) params[0];
         break;
      case GL_COLOR_INDEXES:
         fparam[0] = (GLfloat) params[0];
         fparam[1] = (GLfloat) params[1];
         fparam[2] = (GLfloat) params[2];
         break;
      default:
         /* Error will be caught later in gl_Materialfv */
         ;
   }
   (*CC->API.Materialfv)( CC, face, pname, fparam );
}


void glMatrixMode( GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.MatrixMode)( CC, mode );
}


void glMultMatrixd( const GLdouble *m )
{
   GLfloat fm[16];
   GLuint i;
   GET_CONTEXT;
   CHECK_CONTEXT;

   for (i=0;i<16;i++) {
      fm[i] = (GLfloat) m[i];
   }

   (*CC->API.MultMatrixf)( CC, fm );
}


void glMultMatrixf( const GLfloat *m )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.MultMatrixf)( CC, m );
}


void glNewList( GLuint list, GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.NewList)(CC, list, mode);
}

void glNormal3b( GLbyte nx, GLbyte ny, GLbyte nz )
{
   GET_CONTEXT;
   (*CC->API.Normal3f)( CC, BYTE_TO_FLOAT(nx),
                             BYTE_TO_FLOAT(ny), BYTE_TO_FLOAT(nz) );
}


void glNormal3d( GLdouble nx, GLdouble ny, GLdouble nz )
{
   GLfloat fx, fy, fz;
   GET_CONTEXT;
   if (ABSD(nx)<0.00001)   fx = 0.0F;   else  fx = nx;
   if (ABSD(ny)<0.00001)   fy = 0.0F;   else  fy = ny;
   if (ABSD(nz)<0.00001)   fz = 0.0F;   else  fz = nz;
   (*CC->API.Normal3f)( CC, fx, fy, fz );
}


void glNormal3f( GLfloat nx, GLfloat ny, GLfloat nz )
{
   GET_CONTEXT;
#ifdef SHORTCUT
   if (CC->CompileFlag) {
      (*CC->Save.Normal3f)( CC, nx, ny, nz );
      if (CC->ExecuteFlag) {
         CC->Current.Normal[0] = nx;
         CC->Current.Normal[1] = ny;
         CC->Current.Normal[2] = nz;
      }
   }
   else {
      /* Execute */
      CC->Current.Normal[0] = nx;
      CC->Current.Normal[1] = ny;
      CC->Current.Normal[2] = nz;
   }
#else
   (*CC->API.Normal3f)( CC, nx, ny, nz );
#endif
}


void glNormal3i( GLint nx, GLint ny, GLint nz )
{
   GET_CONTEXT;
   (*CC->API.Normal3f)( CC, INT_TO_FLOAT(nx),
                             INT_TO_FLOAT(ny), INT_TO_FLOAT(nz) );
}


void glNormal3s( GLshort nx, GLshort ny, GLshort nz )
{
   GET_CONTEXT;
   (*CC->API.Normal3f)( CC, SHORT_TO_FLOAT(nx),
                             SHORT_TO_FLOAT(ny), SHORT_TO_FLOAT(nz) );
}


void glNormal3bv( const GLbyte *v )
{
   GET_CONTEXT;
   (*CC->API.Normal3f)( CC, BYTE_TO_FLOAT(v[0]),
                             BYTE_TO_FLOAT(v[1]), BYTE_TO_FLOAT(v[2]) );
}


void glNormal3dv( const GLdouble *v )
{
   GLfloat fx, fy, fz;
   GET_CONTEXT;
   if (ABSD(v[0])<0.00001)   fx = 0.0F;   else  fx = v[0];
   if (ABSD(v[1])<0.00001)   fy = 0.0F;   else  fy = v[1];
   if (ABSD(v[2])<0.00001)   fz = 0.0F;   else  fz = v[2];
   (*CC->API.Normal3f)( CC, fx, fy, fz );
}


void glNormal3fv( const GLfloat *v )
{
   GET_CONTEXT;
#ifdef SHORTCUT
        if (CC->CompileFlag) {
           (*CC->Save.Normal3fv)( CC, v );
           if (CC->ExecuteFlag) {
              CC->Current.Normal[0] = v[0];
              CC->Current.Normal[1] = v[1];
              CC->Current.Normal[2] = v[2];
           }
        }
        else {
           /* Execute */
           GLfloat *n = CC->Current.Normal;
           n[0] = v[0];
           n[1] = v[1];
           n[2] = v[2];
        }
#else
   (*CC->API.Normal3fv)( CC, v );
#endif
}


void glNormal3iv( const GLint *v )
{
   GET_CONTEXT;
   (*CC->API.Normal3f)( CC, INT_TO_FLOAT(v[0]),
                             INT_TO_FLOAT(v[1]), INT_TO_FLOAT(v[2]) );
}


void glNormal3sv( const GLshort *v )
{
   GET_CONTEXT;
   (*CC->API.Normal3f)( CC, SHORT_TO_FLOAT(v[0]),
                             SHORT_TO_FLOAT(v[1]), SHORT_TO_FLOAT(v[2]) );
}


void glNormalPointer( GLenum type, GLsizei stride, const GLvoid *ptr )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.NormalPointer)(CC, type, stride, ptr);
}


void glOrtho( GLdouble left, GLdouble right,
              GLdouble bottom, GLdouble top,
              GLdouble nearval, GLdouble farval )
{
   GLfloat x, y, z;
   GLfloat tx, ty, tz;
   GLfloat m[16];
   GET_CONTEXT;
   CHECK_CONTEXT;

   x = 2.0 / (right-left);
   y = 2.0 / (top-bottom);
   z = -2.0 / (farval-nearval);
   tx = -(right+left) / (right-left);
   ty = -(top+bottom) / (top-bottom);
   tz = -(farval+nearval) / (farval-nearval);

#define M(row,col)  m[col*4+row]
   M(0,0) = x;     M(0,1) = 0.0F;  M(0,2) = 0.0F;  M(0,3) = tx;
   M(1,0) = 0.0F;  M(1,1) = y;     M(1,2) = 0.0F;  M(1,3) = ty;
   M(2,0) = 0.0F;  M(2,1) = 0.0F;  M(2,2) = z;     M(2,3) = tz;
   M(3,0) = 0.0F;  M(3,1) = 0.0F;  M(3,2) = 0.0F;  M(3,3) = 1.0F;
#undef M

   (*CC->API.MultMatrixf)( CC, m );
}


void glPassThrough( GLfloat token )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PassThrough)(CC, token);
}


void glPixelMapfv( GLenum map, GLint mapsize, const GLfloat *values )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PixelMapfv)( CC, map, mapsize, values );
}


void glPixelMapuiv( GLenum map, GLint mapsize, const GLuint *values )
{
   GLfloat fvalues[MAX_PIXEL_MAP_TABLE];
   GLuint i;
   GET_CONTEXT;
   CHECK_CONTEXT;

   if (map==GL_PIXEL_MAP_I_TO_I || map==GL_PIXEL_MAP_S_TO_S) {
      for (i=0;i<mapsize;i++) {
    fvalues[i] = (GLfloat) values[i];
      }
   }
   else {
      for (i=0;i<mapsize;i++) {
    fvalues[i] = UINT_TO_FLOAT( values[i] );
      }
   }
   (*CC->API.PixelMapfv)( CC, map, mapsize, fvalues );
}



void glPixelMapusv( GLenum map, GLint mapsize, const GLushort *values )
{
   GLfloat fvalues[MAX_PIXEL_MAP_TABLE];
   GLuint i;
   GET_CONTEXT;
   CHECK_CONTEXT;

   if (map==GL_PIXEL_MAP_I_TO_I || map==GL_PIXEL_MAP_S_TO_S) {
      for (i=0;i<mapsize;i++) {
    fvalues[i] = (GLfloat) values[i];
      }
   }
   else {
      for (i=0;i<mapsize;i++) {
    fvalues[i] = USHORT_TO_FLOAT( values[i] );
      }
   }
   (*CC->API.PixelMapfv)( CC, map, mapsize, fvalues );
}


void glPixelStoref( GLenum pname, GLfloat param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PixelStorei)( CC, pname, (GLint) param );
}


void glPixelStorei( GLenum pname, GLint param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PixelStorei)( CC, pname, param );
}


void glPixelTransferf( GLenum pname, GLfloat param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PixelTransferf)(CC, pname, param);
}


void glPixelTransferi( GLenum pname, GLint param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PixelTransferf)(CC, pname, (GLfloat) param);
}


void glPixelZoom( GLfloat xfactor, GLfloat yfactor )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PixelZoom)(CC, xfactor, yfactor);
}


void glPointSize( GLfloat size )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PointSize)(CC, size);
}


void glPolygonMode( GLenum face, GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PolygonMode)(CC, face, mode);
}


void glPolygonOffset( GLfloat factor, GLfloat units )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PolygonOffset)( CC, factor, units );
}


#ifdef GL_EXT_polygon_offset
void glPolygonOffsetEXT( GLfloat factor, GLfloat bias )
{
   glPolygonOffset( factor, bias * DEPTH_SCALE );
}
#endif


void glPolygonStipple( const GLubyte *mask )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PolygonStipple)(CC, mask);
}


void glPopAttrib( void )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PopAttrib)(CC);
}


void glPopClientAttrib( void )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PopClientAttrib)(CC);
}


void glPopMatrix( void )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PopMatrix)( CC );
}


void glPopName( void )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PopName)(CC);
}


void glPrioritizeTextures( GLsizei n, const GLuint *textures,
                           const GLclampf *priorities )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PrioritizeTextures)(CC, n, textures, priorities);
}


void glPushMatrix( void )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PushMatrix)( CC );
}


void glRasterPos2d( GLdouble x, GLdouble y )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) x, (GLfloat) y, 0.0F, 1.0F );
}


void glRasterPos2f( GLfloat x, GLfloat y )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) x, (GLfloat) y, 0.0F, 1.0F );
}


void glRasterPos2i( GLint x, GLint y )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) x, (GLfloat) y, 0.0F, 1.0F );
}


void glRasterPos2s( GLshort x, GLshort y )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) x, (GLfloat) y, 0.0F, 1.0F );
}


void glRasterPos3d( GLdouble x, GLdouble y, GLdouble z )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) x, (GLfloat) y, (GLfloat) z, 1.0F );
}


void glRasterPos3f( GLfloat x, GLfloat y, GLfloat z )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) x, (GLfloat) y, (GLfloat) z, 1.0F );
}


void glRasterPos3i( GLint x, GLint y, GLint z )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) x, (GLfloat) y, (GLfloat) z, 1.0F );
}


void glRasterPos3s( GLshort x, GLshort y, GLshort z )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) x, (GLfloat) y, (GLfloat) z, 1.0F );
}


void glRasterPos4d( GLdouble x, GLdouble y, GLdouble z, GLdouble w )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) x, (GLfloat) y,
                               (GLfloat) z, (GLfloat) w );
}


void glRasterPos4f( GLfloat x, GLfloat y, GLfloat z, GLfloat w )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, x, y, z, w );
}


void glRasterPos4i( GLint x, GLint y, GLint z, GLint w )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) x, (GLfloat) y,
                               (GLfloat) z, (GLfloat) w );
}


void glRasterPos4s( GLshort x, GLshort y, GLshort z, GLshort w )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) x, (GLfloat) y,
                               (GLfloat) z, (GLfloat) w );
}


void glRasterPos2dv( const GLdouble *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) v[0], (GLfloat) v[1], 0.0F, 1.0F );
}


void glRasterPos2fv( const GLfloat *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) v[0], (GLfloat) v[1], 0.0F, 1.0F );
}


void glRasterPos2iv( const GLint *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) v[0], (GLfloat) v[1], 0.0F, 1.0F );
}


void glRasterPos2sv( const GLshort *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) v[0], (GLfloat) v[1], 0.0F, 1.0F );
}


/*** 3 element vector ***/

void glRasterPos3dv( const GLdouble *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], 1.0F );
}


void glRasterPos3fv( const GLfloat *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], 1.0F );
}


void glRasterPos3iv( const GLint *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], 1.0F );
}


void glRasterPos3sv( const GLshort *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], 1.0F );
}


void glRasterPos4dv( const GLdouble *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], (GLfloat) v[3] );
}


void glRasterPos4fv( const GLfloat *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, v[0], v[1], v[2], v[3] );
}


void glRasterPos4iv( const GLint *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], (GLfloat) v[3] );
}


void glRasterPos4sv( const GLshort *v )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.RasterPos4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], (GLfloat) v[3] );
}


void glReadBuffer( GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ReadBuffer)( CC, mode );
}


void glReadPixels( GLint x, GLint y, GLsizei width, GLsizei height,
           GLenum format, GLenum type, GLvoid *pixels )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ReadPixels)( CC, x, y, width, height, format, type, pixels );
}


void glRectd( GLdouble x1, GLdouble y1, GLdouble x2, GLdouble y2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Rectf)( CC, (GLfloat) x1, (GLfloat) y1,
                    (GLfloat) x2, (GLfloat) y2 );
}


void glRectf( GLfloat x1, GLfloat y1, GLfloat x2, GLfloat y2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Rectf)( CC, x1, y1, x2, y2 );
}


void glRecti( GLint x1, GLint y1, GLint x2, GLint y2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Rectf)( CC, (GLfloat) x1, (GLfloat) y1,
                         (GLfloat) x2, (GLfloat) y2 );
}


void glRects( GLshort x1, GLshort y1, GLshort x2, GLshort y2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Rectf)( CC, (GLfloat) x1, (GLfloat) y1,
                     (GLfloat) x2, (GLfloat) y2 );
}


void glRectdv( const GLdouble *v1, const GLdouble *v2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Rectf)(CC, (GLfloat) v1[0], (GLfloat) v1[1],
                    (GLfloat) v2[0], (GLfloat) v2[1]);
}


void glRectfv( const GLfloat *v1, const GLfloat *v2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Rectf)(CC, v1[0], v1[1], v2[0], v2[1]);
}


void glRectiv( const GLint *v1, const GLint *v2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Rectf)( CC, (GLfloat) v1[0], (GLfloat) v1[1],
                     (GLfloat) v2[0], (GLfloat) v2[1] );
}


void glRectsv( const GLshort *v1, const GLshort *v2 )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Rectf)(CC, (GLfloat) v1[0], (GLfloat) v1[1],
        (GLfloat) v2[0], (GLfloat) v2[1]);
}


void glScissor( GLint x, GLint y, GLsizei width, GLsizei height)
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Scissor)(CC, x, y, width, height);
}


GLboolean glIsEnabled( GLenum cap )
{
   GET_CONTEXT;
   CHECK_CONTEXT_RETURN(GL_FALSE);
   return (*CC->API.IsEnabled)( CC, cap );
}



void glPushAttrib( GLbitfield mask )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PushAttrib)(CC, mask);
}


void glPushClientAttrib( GLbitfield mask )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PushClientAttrib)(CC, mask);
}


void glPushName( GLuint name )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.PushName)(CC, name);
}


GLint glRenderMode( GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT_RETURN(0);
   return (*CC->API.RenderMode)(CC, mode);
}


void glRotated( GLdouble angle, GLdouble x, GLdouble y, GLdouble z )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Rotatef)( CC, (GLfloat) angle,
                       (GLfloat) x, (GLfloat) y, (GLfloat) z );
}


void glRotatef( GLfloat angle, GLfloat x, GLfloat y, GLfloat z )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Rotatef)( CC, angle, x, y, z );
}


void glSelectBuffer( GLsizei size, GLuint *buffer )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.SelectBuffer)(CC, size, buffer);
}


void glScaled( GLdouble x, GLdouble y, GLdouble z )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Scalef)( CC, (GLfloat) x, (GLfloat) y, (GLfloat) z );
}


void glScalef( GLfloat x, GLfloat y, GLfloat z )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Scalef)( CC, x, y, z );
}


void glShadeModel( GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ShadeModel)(CC, mode);
}


void glStencilFunc( GLenum func, GLint ref, GLuint mask )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.StencilFunc)(CC, func, ref, mask);
}


void glStencilMask( GLuint mask )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.StencilMask)(CC, mask);
}


void glStencilOp( GLenum fail, GLenum zfail, GLenum zpass )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.StencilOp)(CC, fail, zfail, zpass);
}


void glTexCoord1d( GLdouble s )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, 0.0, 0.0, 1.0 );
}


void glTexCoord1f( GLfloat s )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, s, 0.0, 0.0, 1.0 );
}


void glTexCoord1i( GLint s )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, 0.0, 0.0, 1.0 );
}


void glTexCoord1s( GLshort s )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, 0.0, 0.0, 1.0 );
}


void glTexCoord2d( GLdouble s, GLdouble t )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, (GLfloat) t, 0.0, 1.0 );
}


void glTexCoord2f( GLfloat s, GLfloat t )
{
   GET_CONTEXT;
#ifdef SHORTCUT
        if (CC->CompileFlag) {
           (*CC->Save.TexCoord4f)( CC, s, t, 0.0F, 1.0F );
           if (CC->ExecuteFlag) {
              CC->Current.TexCoord[0] = s;
              CC->Current.TexCoord[1] = t;
              CC->Current.TexCoord[2] = 0.0F;
              CC->Current.TexCoord[3] = 1.0F;
           }
        }
        else {
           /* Execute */
           GLfloat *tex = CC->Current.TexCoord;
           tex[0] = s;
           tex[1] = t;
           tex[2] = 0.0F;
           tex[3] = 1.0F;
        }
#else
   (*CC->API.TexCoord4f)( CC, s, tex, 0.0, 1.0 );
#endif
}


void glTexCoord2i( GLint s, GLint t )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, (GLfloat) t, 0.0, 1.0 );
}


void glTexCoord2s( GLshort s, GLshort t )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, (GLfloat) t, 0.0, 1.0 );
}


void glTexCoord3d( GLdouble s, GLdouble t, GLdouble r )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, (GLfloat) t, (GLfloat) r, 1.0 );
}


void glTexCoord3f( GLfloat s, GLfloat t, GLfloat r )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, s, t, r, 1.0 );
}


void glTexCoord3i( GLint s, GLint t, GLint r )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, (GLfloat) t,
                               (GLfloat) r, 1.0 );
}


void glTexCoord3s( GLshort s, GLshort t, GLshort r )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, (GLfloat) t,
                               (GLfloat) r, 1.0 );
}


void glTexCoord4d( GLdouble s, GLdouble t, GLdouble r, GLdouble q )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, (GLfloat) t,
                               (GLfloat) r, (GLfloat) q );
}


void glTexCoord4f( GLfloat s, GLfloat t, GLfloat r, GLfloat q )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, s, t, r, q );
}


void glTexCoord4i( GLint s, GLint t, GLint r, GLint q )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, (GLfloat) t,
                               (GLfloat) r, (GLfloat) q );
}


void glTexCoord4s( GLshort s, GLshort t, GLshort r, GLshort q )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) s, (GLfloat) t,
                               (GLfloat) r, (GLfloat) q );
}


void glTexCoord1dv( const GLdouble *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) *v, 0.0, 0.0, 1.0 );
}


void glTexCoord1fv( const GLfloat *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, *v, 0.0, 0.0, 1.0 );
}


void glTexCoord1iv( const GLint *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, *v, 0.0, 0.0, 1.0 );
}


void glTexCoord1sv( const GLshort *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) *v, 0.0, 0.0, 1.0 );
}


void glTexCoord2dv( const GLdouble *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) v[0], (GLfloat) v[1], 0.0, 1.0 );
}


void glTexCoord2fv( const GLfloat *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, v[0], v[1], 0.0, 1.0 );
}


void glTexCoord2iv( const GLint *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) v[0], (GLfloat) v[1], 0.0, 1.0 );
}


void glTexCoord2sv( const GLshort *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) v[0], (GLfloat) v[1], 0.0, 1.0 );
}


void glTexCoord3dv( const GLdouble *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], 1.0 );
}


void glTexCoord3fv( const GLfloat *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, v[0], v[1], v[2], 1.0 );
}


void glTexCoord3iv( const GLint *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                          (GLfloat) v[2], 1.0 );
}


void glTexCoord3sv( const GLshort *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], 1.0 );
}


void glTexCoord4dv( const GLdouble *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], (GLfloat) v[3] );
}


void glTexCoord4fv( const GLfloat *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, v[0], v[1], v[2], v[3] );
}


void glTexCoord4iv( const GLint *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], (GLfloat) v[3] );
}


void glTexCoord4sv( const GLshort *v )
{
   GET_CONTEXT;
   (*CC->API.TexCoord4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                               (GLfloat) v[2], (GLfloat) v[3] );
}


void glTexCoordPointer( GLint size, GLenum type, GLsizei stride,
                        const GLvoid *ptr )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexCoordPointer)(CC, size, type, stride, ptr);
}


void glTexGend( GLenum coord, GLenum pname, GLdouble param )
{
   GLfloat p = (GLfloat) param;
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexGenfv)( CC, coord, pname, &p );
}


void glTexGenf( GLenum coord, GLenum pname, GLfloat param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexGenfv)( CC, coord, pname, &param );
}


void glTexGeni( GLenum coord, GLenum pname, GLint param )
{
   GLfloat p = (GLfloat) param;
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexGenfv)( CC, coord, pname, &p );
}


void glTexGendv( GLenum coord, GLenum pname, const GLdouble *params )
{
   GLfloat p[4];
   GET_CONTEXT;
   CHECK_CONTEXT;
   p[0] = params[0];
   p[1] = params[1];
   p[2] = params[2];
   p[3] = params[3];
   (*CC->API.TexGenfv)( CC, coord, pname, p );
}


void glTexGeniv( GLenum coord, GLenum pname, const GLint *params )
{
   GLfloat p[4];
   GET_CONTEXT;
   CHECK_CONTEXT;
   p[0] = params[0];
   p[1] = params[1];
   p[2] = params[2];
   p[3] = params[3];
   (*CC->API.TexGenfv)( CC, coord, pname, p );
}


void glTexGenfv( GLenum coord, GLenum pname, const GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexGenfv)( CC, coord, pname, params );
}




void glTexEnvf( GLenum target, GLenum pname, GLfloat param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexEnvfv)( CC, target, pname, &param );
}



void glTexEnvi( GLenum target, GLenum pname, GLint param )
{
   GLfloat p = (GLfloat) param;
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexEnvfv)( CC, target, pname, &p );
}



void glTexEnvfv( GLenum target, GLenum pname, const GLfloat *param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexEnvfv)( CC, target, pname, param );
}



void glTexEnviv( GLenum target, GLenum pname, const GLint *param )
{
   GLfloat p[4];
   p[0] = INT_TO_FLOAT( param[0] );
   p[1] = INT_TO_FLOAT( param[1] );
   p[2] = INT_TO_FLOAT( param[2] );
   p[3] = INT_TO_FLOAT( param[3] );
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexEnvfv)( CC, target, pname, p );
}


void glTexImage1D( GLenum target, GLint level, GLint internalformat,
                   GLsizei width, GLint border,
                   GLenum format, GLenum type, const GLvoid *pixels )
{
   struct gl_image *teximage;
   GLenum destType;
   GET_CONTEXT;
   CHECK_CONTEXT;
   if (type==GL_UNSIGNED_BYTE) {
      destType = GL_UNSIGNED_BYTE;
   }
   else if (type==GL_BITMAP) {
      destType = GL_BITMAP;
   }
   else {
      destType = GL_FLOAT;
   }
   teximage = gl_unpack_image( CC, width, 1, format, type,
                               destType, pixels, GL_FALSE );
   (*CC->API.TexImage1D)( CC, target, level, internalformat,
                          width, border, format, type, teximage );
}



void glTexImage2D( GLenum target, GLint level, GLint internalformat,
                   GLsizei width, GLsizei height, GLint border,
                   GLenum format, GLenum type, const GLvoid *pixels )
{
   struct gl_image *teximage;
   GLenum destType;
   GET_CONTEXT;
   CHECK_CONTEXT;
   if (type==GL_UNSIGNED_BYTE) {
      destType = GL_UNSIGNED_BYTE;
   }
   else if (type==GL_BITMAP) {
      destType = GL_BITMAP;
   }
   else {
      destType = GL_FLOAT;
   }
   teximage = gl_unpack_image( CC, width, height, format, type,
                               destType, pixels, GL_FALSE );
   (*CC->API.TexImage2D)( CC, target, level, internalformat,
                          width, height, border, format, type, teximage );
}


void glTexParameterf( GLenum target, GLenum pname, GLfloat param )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexParameterfv)( CC, target, pname, &param );
}


void glTexParameteri( GLenum target, GLenum pname, GLint param )
{
   GLfloat fparam = param;
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexParameterfv)( CC, target, pname, &fparam );
}


void glTexParameterfv( GLenum target, GLenum pname, const GLfloat *params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexParameterfv)( CC, target, pname, params );
}


void glTexParameteriv( GLenum target, GLenum pname, const GLint *params )
{
   GLfloat p[4];
   GET_CONTEXT;
   CHECK_CONTEXT;
   if (pname==GL_TEXTURE_BORDER_COLOR) {
      p[0] = INT_TO_FLOAT( params[0] );
      p[1] = INT_TO_FLOAT( params[1] );
      p[2] = INT_TO_FLOAT( params[2] );
      p[3] = INT_TO_FLOAT( params[3] );
   }
   else {
      p[0] = (GLfloat) params[0];
      p[1] = (GLfloat) params[1];
      p[2] = (GLfloat) params[2];
      p[3] = (GLfloat) params[3];
   }
   (*CC->API.TexParameterfv)( CC, target, pname, p );
}


void glTexSubImage1D( GLenum target, GLint level, GLint xoffset,
                      GLsizei width, GLenum format,
                      GLenum type, const GLvoid *pixels )
{
   struct gl_image *image;
   GET_CONTEXT;
   CHECK_CONTEXT;
   image = gl_unpack_texsubimage( CC, width, 1, format, type, pixels );
   (*CC->API.TexSubImage1D)( CC, target, level, xoffset, width,
                             format, type, image );
}


void glTexSubImage2D( GLenum target, GLint level,
                      GLint xoffset, GLint yoffset,
                      GLsizei width, GLsizei height,
                      GLenum format, GLenum type,
                      const GLvoid *pixels )
{
   struct gl_image *image;
   GET_CONTEXT;
   CHECK_CONTEXT;
   image = gl_unpack_texsubimage( CC, width, height, format, type, pixels );
   (*CC->API.TexSubImage2D)( CC, target, level, xoffset, yoffset,
                             width, height, format, type, image );
}


void glTranslated( GLdouble x, GLdouble y, GLdouble z )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Translatef)( CC, (GLfloat) x, (GLfloat) y, (GLfloat) z );
}


void glTranslatef( GLfloat x, GLfloat y, GLfloat z )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.Translatef)( CC, x, y, z );
}


void glVertex2d( GLdouble x, GLdouble y )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) x, (GLfloat) y, 0.0F, 1.0F );
}


void glVertex2f( GLfloat x, GLfloat y )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, x, y, 0.0F, 1.0F );
}


void glVertex2i( GLint x, GLint y )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) x, (GLfloat) y, 0.0F, 1.0F );
}


void glVertex2s( GLshort x, GLshort y )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) x, (GLfloat) y, 0.0F, 1.0F );
}


void glVertex3d( GLdouble x, GLdouble y, GLdouble z )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) x, (GLfloat) y, (GLfloat) z, 1.0F );
}


void glVertex3f( GLfloat x, GLfloat y, GLfloat z )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, x, y, z, 1.0F );
}


void glVertex3i( GLint x, GLint y, GLint z )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) x, (GLfloat) y, (GLfloat) z, 1.0F );
}


void glVertex3s( GLshort x, GLshort y, GLshort z )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) x, (GLfloat) y, (GLfloat) z, 1.0F );
}


void glVertex4d( GLdouble x, GLdouble y, GLdouble z, GLdouble w )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) x, (GLfloat) y,
                            (GLfloat) z, (GLfloat) w );
}


void glVertex4f( GLfloat x, GLfloat y, GLfloat z, GLfloat w )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, x, y, z, w );
}


void glVertex4i( GLint x, GLint y, GLint z, GLint w )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) x, (GLfloat) y,
                            (GLfloat) z, (GLfloat) w );
}


void glVertex4s( GLshort x, GLshort y, GLshort z, GLshort w )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) x, (GLfloat) y,
                            (GLfloat) z, (GLfloat) w );
}


void glVertex2dv( const GLdouble *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) v[0], (GLfloat) v[1], 0.0F, 1.0F );
}


void glVertex2fv( const GLfloat *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, v[0], v[1], 0.0F, 1.0F );
}


void glVertex2iv( const GLint *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) v[0], (GLfloat) v[1], 0.0F, 1.0F );
}


void glVertex2sv( const GLshort *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) v[0], (GLfloat) v[1], 0.0F, 1.0F );
}


void glVertex3dv( const GLdouble *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                            (GLfloat) v[2], 1.0F );
}


void glVertex3fv( const GLfloat *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, v[0], v[1], v[2], 1.0F );
}


void glVertex3iv( const GLint *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                            (GLfloat) v[2], 1.0F );
}


void glVertex3sv( const GLshort *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                            (GLfloat) v[2], 1.0F );
}


void glVertex4dv( const GLdouble *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                            (GLfloat) v[2], (GLfloat) v[3] );
}


void glVertex4fv( const GLfloat *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, v[0], v[1], v[2], v[3] );
}


void glVertex4iv( const GLint *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                  (GLfloat) v[2], (GLfloat) v[3] );
}


void glVertex4sv( const GLshort *v )
{
   GET_CONTEXT;
   (*CC->API.Vertex4f)( CC, (GLfloat) v[0], (GLfloat) v[1],
                            (GLfloat) v[2], (GLfloat) v[3] );
}


void glVertexPointer( GLint size, GLenum type, GLsizei stride,
                      const GLvoid *ptr )
{
   GET_CONTEXT;
   (*CC->API.VertexPointer)(CC, size, type, stride, ptr);
}


void glViewport( GLint x, GLint y, GLsizei width, GLsizei height )
{
   GET_CONTEXT;
   (*CC->API.Viewport)( CC, x, y, width, height );
}



/**
 ** Extensions
 **
 ** Some of these are incorporated into the 1.1 API.  They also remain as
 ** extensions for backward compatibility.  May be removed in the future.
 **/


/* GL_EXT_blend_minmax */

void glBlendEquationEXT( GLenum mode )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.BlendEquation)(CC, mode);
}


/* GL_EXT_blend_color */

void glBlendColorEXT( GLclampf red, GLclampf green,
                      GLclampf blue, GLclampf alpha )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.BlendColor)(CC, red, green, blue, alpha);
}


/* GL_EXT_vertex_array */

void glVertexPointerEXT( GLint size, GLenum type, GLsizei stride,
                         GLsizei count, const GLvoid *ptr )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.VertexPointer)(CC, size, type, stride, ptr);
}


void glNormalPointerEXT( GLenum type, GLsizei stride, GLsizei count,
                         const GLvoid *ptr )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.NormalPointer)(CC, type, stride, ptr);
}


void glColorPointerEXT( GLint size, GLenum type, GLsizei stride,
                        GLsizei count, const GLvoid *ptr )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ColorPointer)(CC, size, type, stride, ptr);
}


void glIndexPointerEXT( GLenum type, GLsizei stride,
                        GLsizei count, const GLvoid *ptr )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.IndexPointer)(CC, type, stride, ptr);
}


void glTexCoordPointerEXT( GLint size, GLenum type, GLsizei stride,
                           GLsizei count, const GLvoid *ptr )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.TexCoordPointer)(CC, size, type, stride, ptr);
}


void glEdgeFlagPointerEXT( GLsizei stride, GLsizei count,
                           const GLboolean *ptr )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.EdgeFlagPointer)(CC, stride, ptr);
}


void glGetPointervEXT( GLenum pname, GLvoid **params )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.GetPointerv)(CC, pname, params);
}


void glArrayElementEXT( GLint i )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ArrayElement)(CC, i);
}


void glDrawArraysEXT( GLenum mode, GLint first, GLsizei count )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.DrawArrays)(CC, mode, first, count);
}


/* GL_EXT_texture_object */

GLboolean glAreTexturesResidentEXT( GLsizei n, const GLuint *textures,
                                    GLboolean *residences )
{
   return glAreTexturesResident( n, textures, residences );
}


void glBindTextureEXT( GLenum target, GLuint texture )
{
   glBindTexture( target, texture );
}


void glDeleteTexturesEXT( GLsizei n, const GLuint *textures)
{
   glDeleteTextures( n, textures );
}


void glGenTexturesEXT( GLsizei n, GLuint *textures )
{
   glGenTextures( n, textures );
}


GLboolean glIsTextureEXT( GLuint texture )
{
   return glIsTexture( texture );
}


void glPrioritizeTexturesEXT( GLsizei n, const GLuint *textures,
                              const GLclampf *priorities )
{
   glPrioritizeTextures( n, textures, priorities );
}



/* GL_EXT_texture3D */

void glCopyTexSubImage3DEXT( GLenum target, GLint level, GLint xoffset,
                             GLint yoffset, GLint zoffset,
                             GLint x, GLint y, GLsizei width,
                             GLsizei height )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.CopyTexSubImage3DEXT)( CC, target, level, xoffset, yoffset,
                                    zoffset, x, y, width, height );
}



void glTexImage3DEXT( GLenum target, GLint level, GLenum internalformat,
                      GLsizei width, GLsizei height, GLsizei depth,
                      GLint border, GLenum format, GLenum type,
                      const GLvoid *pixels )
{
   struct gl_image *teximage;
   GLenum destType;
   GET_CONTEXT;
   CHECK_CONTEXT;
   if (type==GL_UNSIGNED_BYTE) {
      destType = GL_UNSIGNED_BYTE;
   }
   else if (type==GL_BITMAP) {
      destType = GL_BITMAP;
   }
   else {
      destType = GL_FLOAT;
   }
   teximage = gl_unpack_image3D( CC, width, height, depth, format, type,
                                 destType, pixels, GL_FALSE );
   (*CC->API.TexImage3DEXT)( CC, target, level, internalformat,
                             width, height, depth, border, format, type, 
                             teximage );
}


void glTexSubImage3DEXT( GLenum target, GLint level, GLint xoffset,
                         GLint yoffset, GLint zoffset, GLsizei width,
                         GLsizei height, GLsizei depth, GLenum format,
                         GLenum type, const GLvoid *pixels )
{
   struct gl_image *image;
   GET_CONTEXT;
   CHECK_CONTEXT;
   image = gl_unpack_texsubimage3D( CC, width, height, depth, format, type,
                                    pixels );
   (*CC->API.TexSubImage3DEXT)( CC, target, level, xoffset, yoffset, zoffset,
                                width, height, depth, format, type, image );
}




#ifdef GL_MESA_window_pos
/*
 * Mesa implementation of glWindowPos*MESA()
 */
void glWindowPos4fMESA( GLfloat x, GLfloat y, GLfloat z, GLfloat w )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.WindowPos4fMESA)( CC, x, y, z, w );
}
#else
/* Implementation in winpos.c is used */
#endif


void glWindowPos2iMESA( GLint x, GLint y )
{
   glWindowPos4fMESA( (GLfloat) x, (GLfloat) y, 0.0F, 1.0F );
}

void glWindowPos2sMESA( GLshort x, GLshort y )
{
   glWindowPos4fMESA( (GLfloat) x, (GLfloat) y, 0.0F, 1.0F );
}

void glWindowPos2fMESA( GLfloat x, GLfloat y )
{
   glWindowPos4fMESA( x, y, 0.0F, 1.0F );
}

void glWindowPos2dMESA( GLdouble x, GLdouble y )
{
   glWindowPos4fMESA( (GLfloat) x, (GLfloat) y, 0.0F, 1.0F );
}

void glWindowPos2ivMESA( const GLint *p )
{
   glWindowPos4fMESA( (GLfloat) p[0], (GLfloat) p[1], 0.0F, 1.0F );
}

void glWindowPos2svMESA( const GLshort *p )
{
   glWindowPos4fMESA( (GLfloat) p[0], (GLfloat) p[1], 0.0F, 1.0F );
}

void glWindowPos2fvMESA( const GLfloat *p )
{
   glWindowPos4fMESA( p[0], p[1], 0.0F, 1.0F );
}

void glWindowPos2dvMESA( const GLdouble *p )
{
   glWindowPos4fMESA( (GLfloat) p[0], (GLfloat) p[1], 0.0F, 1.0F );
}

void glWindowPos3iMESA( GLint x, GLint y, GLint z )
{
   glWindowPos4fMESA( (GLfloat) x, (GLfloat) y, (GLfloat) z, 1.0F );
}

void glWindowPos3sMESA( GLshort x, GLshort y, GLshort z )
{
   glWindowPos4fMESA( (GLfloat) x, (GLfloat) y, (GLfloat) z, 1.0F );
}

void glWindowPos3fMESA( GLfloat x, GLfloat y, GLfloat z )
{
   glWindowPos4fMESA( x, y, z, 1.0F );
}

void glWindowPos3dMESA( GLdouble x, GLdouble y, GLdouble z )
{
   glWindowPos4fMESA( (GLfloat) x, (GLfloat) y, (GLfloat) z, 1.0F );
}

void glWindowPos3ivMESA( const GLint *p )
{
   glWindowPos4fMESA( (GLfloat) p[0], (GLfloat) p[1], (GLfloat) p[2], 1.0F );
}

void glWindowPos3svMESA( const GLshort *p )
{
   glWindowPos4fMESA( (GLfloat) p[0], (GLfloat) p[1], (GLfloat) p[2], 1.0F );
}

void glWindowPos3fvMESA( const GLfloat *p )
{
   glWindowPos4fMESA( p[0], p[1], p[2], 1.0F );
}

void glWindowPos3dvMESA( const GLdouble *p )
{
   glWindowPos4fMESA( (GLfloat) p[0], (GLfloat) p[1], (GLfloat) p[2], 1.0F );
}

void glWindowPos4iMESA( GLint x, GLint y, GLint z, GLint w )
{
   glWindowPos4fMESA( (GLfloat) x, (GLfloat) y, (GLfloat) z, (GLfloat) w );
}

void glWindowPos4sMESA( GLshort x, GLshort y, GLshort z, GLshort w )
{
   glWindowPos4fMESA( (GLfloat) x, (GLfloat) y, (GLfloat) z, (GLfloat) w );
}

void glWindowPos4dMESA( GLdouble x, GLdouble y, GLdouble z, GLdouble w )
{
   glWindowPos4fMESA( (GLfloat) x, (GLfloat) y, (GLfloat) z, (GLfloat) w );
}


void glWindowPos4ivMESA( const GLint *p )
{
   glWindowPos4fMESA( (GLfloat) p[0], (GLfloat) p[1],
                      (GLfloat) p[2], (GLfloat) p[3] );
}

void glWindowPos4svMESA( const GLshort *p )
{
   glWindowPos4fMESA( (GLfloat) p[0], (GLfloat) p[1],
                      (GLfloat) p[2], (GLfloat) p[3] );
}

void glWindowPos4fvMESA( const GLfloat *p )
{
   glWindowPos4fMESA( p[0], p[1], p[2], p[3] );
}

void glWindowPos4dvMESA( const GLdouble *p )
{
   glWindowPos4fMESA( (GLfloat) p[0], (GLfloat) p[1],
                      (GLfloat) p[2], (GLfloat) p[3] );
}



/* GL_MESA_resize_buffers */

/*
 * Called by user application when window has been resized.
 */
void glResizeBuffersMESA( void )
{
   GET_CONTEXT;
   CHECK_CONTEXT;
   (*CC->API.ResizeBuffersMESA)( CC );
}

