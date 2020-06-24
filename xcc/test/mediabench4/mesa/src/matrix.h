/* $Id: matrix.h,v 1.1 1996/09/13 01:38:16 brianp Exp $ */

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
 * $Log: matrix.h,v $
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#ifndef MATRIX_H
#define MATRIX_H


#include "types.h"



extern void gl_compute_modelview_inverse( GLcontext *ctx );


extern void gl_rotation_matrix( GLfloat angle, GLfloat x, GLfloat y, GLfloat z,
                                GLfloat m[] );



extern void gl_Frustum( GLcontext *ctx,
                        GLdouble left, GLdouble right,
                        GLdouble bottom, GLdouble top,
                        GLdouble nearval, GLdouble farval );

extern void gl_PushMatrix( GLcontext *ctx );

extern void gl_PopMatrix( GLcontext *ctx );

extern void gl_LoadMatrixf( GLcontext *ctx, const GLfloat *m );

extern void gl_MatrixMode( GLcontext *ctx, GLenum mode );

extern void gl_MultMatrixf( GLcontext *ctx, const GLfloat *m );

extern void gl_Viewport( GLcontext *ctx,
                         GLint x, GLint y, GLsizei width, GLsizei height );

extern void gl_Rotatef( GLcontext *ctx,
                        GLfloat angle, GLfloat x, GLfloat y, GLfloat z );

extern void gl_Scalef( GLcontext *ctx, GLfloat x, GLfloat y, GLfloat z );

extern void gl_Translatef( GLcontext *ctx, GLfloat x, GLfloat y, GLfloat z );


#endif
