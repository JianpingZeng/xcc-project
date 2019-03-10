/* $Id: texture.h,v 1.5 1997/02/09 18:53:14 brianp Exp $ */

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
 * $Log: texture.h,v $
 * Revision 1.5  1997/02/09 18:53:14  brianp
 * added GL_EXT_texture3D support
 *
 * Revision 1.4  1997/01/09 19:48:58  brianp
 * added gl_texturing_enabled()
 *
 * Revision 1.3  1996/11/14 01:03:09  brianp
 * removed const's from gl_texgen() function to avoid VMS compiler warning
 *
 * Revision 1.2  1996/11/08 02:20:09  brianp
 * gl_do_texgen() replaced with gl_texgen()
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#ifndef TEXTURE_H
#define TEXTURE_H


#include "types.h"


/*** Called from API ***/

extern void gl_GetTexEnvfv( GLcontext *ctx,
                            GLenum target, GLenum pname, GLfloat *params );

extern void gl_GetTexEnviv( GLcontext *ctx,
                            GLenum target, GLenum pname, GLint *params );

extern void gl_GetTexGendv( GLcontext *ctx,
                            GLenum coord, GLenum pname, GLdouble *params );

extern void gl_GetTexGenfv( GLcontext *ctx,
                            GLenum coord, GLenum pname, GLfloat *params );

extern void gl_GetTexGeniv( GLcontext *ctx,
                            GLenum coord, GLenum pname, GLint *params );

extern void gl_GetTexLevelParameterfv( GLcontext *ctx,
                                       GLenum target, GLint level,
                                       GLenum pname, GLfloat *params );

extern void gl_GetTexLevelParameteriv( GLcontext *ctx,
                                       GLenum target, GLint level,
                                       GLenum pname, GLint *params );

extern void gl_GetTexParameterfv( GLcontext *ctx, GLenum target,
                                  GLenum pname, GLfloat *params );

extern void gl_GetTexParameteriv( GLcontext *ctx,
                                  GLenum target, GLenum pname, GLint *params );


extern void gl_TexEnvfv( GLcontext *ctx,
                         GLenum target, GLenum pname, const GLfloat *param );


extern void gl_TexParameterfv( GLcontext *ctx, GLenum target, GLenum pname,
                               const GLfloat *params );


extern void gl_TexGenfv( GLcontext *ctx,
                         GLenum coord, GLenum pname, const GLfloat *params );



/*** Internal functions ***/


extern void gl_texgen( GLcontext *ctx, GLint n,
                       GLfloat obj[][4], GLfloat eye[][4],
                       GLfloat normal[][3], GLfloat texcoord[][4] );


extern void gl_texture_pixels_1d( GLcontext *ctx,
                                  GLuint n,
                                  GLfloat s[], GLfloat lambda[],
				  GLubyte red[], GLubyte green[],
				  GLubyte blue[], GLubyte alpha[] );


extern void gl_texture_pixels_2d( GLcontext *ctx,
                                  GLuint n,
                                  GLfloat s[], GLfloat t[], GLfloat lambda[],
				  GLubyte red[], GLubyte green[],
				  GLubyte blue[], GLubyte alpha[] );


extern void gl_texture_pixels_3d( GLcontext *ctx,
                                  GLuint n,
                                  GLfloat s[], GLfloat t[], 
                                  GLfloat r[], GLfloat lambda[],
                                  GLubyte red[], GLubyte green[],
                                  GLubyte blue[], GLubyte alpha[] );


extern void gl_update_texture_state( GLcontext *ctx );


extern GLboolean gl_texturing_enabled( GLcontext *ctx );


#endif

