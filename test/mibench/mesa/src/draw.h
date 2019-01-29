/* $Id: draw.h,v 1.2 1996/11/09 03:12:56 brianp Exp $ */

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
 * $Log: draw.h,v $
 * Revision 1.2  1996/11/09 03:12:56  brianp
 * added gl_render_vb()
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#ifndef DRAW_H
#define DRAW_H


#include "types.h"


extern void gl_transform_vb_part2( GLcontext *ctx, GLboolean alldone );

extern void gl_transform_vb_part1( GLcontext *ctx, GLboolean alldone );

extern void gl_render_vb( GLcontext *ctx, GLboolean alldone );


extern void gl_save_and_execute_vertex( GLcontext *ctx, GLfloat x, GLfloat y,
				        GLfloat z, GLfloat w );


extern void gl_nop_vertex( GLcontext *ctx,
                           GLfloat x, GLfloat y, GLfloat z, GLfloat w );


extern void gl_set_vertex_function( GLcontext *ctx );


extern void gl_eval_vertex( GLcontext *ctx,
                            const GLfloat vertex[4], const GLfloat normal[3],
			    const GLint color[4], GLuint index,
                            const GLfloat texcoord[4] );


extern void gl_RasterPos4f( GLcontext *ctx,
                            GLfloat x, GLfloat y, GLfloat z, GLfloat w );


extern void gl_windowpos( GLcontext *ctx,
                          GLfloat x, GLfloat y, GLfloat z, GLfloat w );


extern void gl_Begin( GLcontext *ctx, GLenum p );

extern void gl_End( GLcontext *ctx );


#endif
