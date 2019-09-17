/* $Id: glx.h,v 1.3 1997/02/03 19:15:42 brianp Exp $ */

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
 * $Log: glx.h,v $
 * Revision 1.3  1997/02/03 19:15:42  brianp
 * conditionally include glx_mangle.h
 *
 * Revision 1.2  1996/09/19 03:50:42  brianp
 * added glXReleaseBuffersMESA(), GLX_MESA_release_buffers extension
 *
 * Revision 1.1  1996/09/13 01:26:41  brianp
 * Initial revision
 *
 */


#ifndef GLX_H
#define GLX_H


/*
 * A pseudo-GLX implementation to allow GLX-based OpenGL programs to
 * work with Mesa.
 *
 * Notes:
 *   1. If the visual passed to glXGetConfig was not one returned by
 *      glXChooseVisual then the GLX_RGBA and GLX_DOUBLEBUFFER queries
 *      will always return True and the GLX_DEPTH_SIZE query will always
 *      return non-zero.
 *   2. The glXIsDirect() function always returns True.
 */



#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include "GL/gl.h"
#ifdef MESA
#include "GL/xmesa.h"
#endif


#if defined(USE_MGL_NAMESPACE)
#include "glx_mangle.h"
#endif


#ifdef __cplusplus
extern "C" {
#endif


#define GLX_VERSION_1_1		1


/*
 * Tokens for glXChooseVisual and glXGetConfig:
 */
enum _GLX_CONFIGS {
	GLX_USE_GL		= 1,
	GLX_BUFFER_SIZE		= 2,
	GLX_LEVEL		= 3,
	GLX_RGBA		= 4,
	GLX_DOUBLEBUFFER	= 5, 
	GLX_STEREO		= 6,
	GLX_AUX_BUFFERS		= 7,
	GLX_RED_SIZE		= 8,
	GLX_GREEN_SIZE		= 9,
	GLX_BLUE_SIZE		= 10,
	GLX_ALPHA_SIZE		= 11,
	GLX_DEPTH_SIZE		= 12,
	GLX_STENCIL_SIZE	= 13,
	GLX_ACCUM_RED_SIZE	= 14,
	GLX_ACCUM_GREEN_SIZE	= 15,
	GLX_ACCUM_BLUE_SIZE	= 16,
	GLX_ACCUM_ALPHA_SIZE	= 17,

	/* GLX_EXT_visual_info extension */
	GLX_X_VISUAL_TYPE_EXT		= 0x22,
	GLX_TRANSPARENT_TYPE_EXT	= 0x23,
	GLX_TRANSPARENT_INDEX_VALUE_EXT	= 0x24,
	GLX_TRANSPARENT_RED_VALUE_EXT	= 0x25,
	GLX_TRANSPARENT_GREEN_VALUE_EXT	= 0x26,
	GLX_TRANSPARENT_BLUE_VALUE_EXT	= 0x27,
	GLX_TRANSPARENT_ALPHA_VALUE_EXT	= 0x28
};


/*
 * Error codes returned by glXGetConfig:
 */
#define GLX_BAD_SCREEN		1
#define GLX_BAD_ATTRIBUTE	2
#define GLX_NO_EXTENSION	3
#define GLX_BAD_VISUAL		4
#define GLX_BAD_CONTEXT		5
#define GLX_BAD_VALUE       	6
#define GLX_BAD_ENUM		7


/*
 * GLX 1.1 and later:
 */
#define GLX_VENDOR		1
#define GLX_VERSION		2
#define GLX_EXTENSIONS 		3


/*
 * GLX_visual_info extension
 */
#define GLX_TRUE_COLOR_EXT		0x8002
#define GLX_DIRECT_COLOR_EXT		0x8003
#define GLX_PSEUDO_COLOR_EXT		0x8004
#define GLX_STATIC_COLOR_EXT		0x8005
#define GLX_GRAY_SCALE_EXT		0x8006
#define GLX_STATIC_GRAY_EXT		0x8007
#define GLX_NONE_EXT			0x8000
#define GLX_TRANSPARENT_RGB_EXT		0x8008
#define GLX_TRANSPARENT_INDEX_EXT	0x8009


/*
 * Compile-time extension tests
 */
#ifdef MESA
#define GLX_EXT_visual_info		1
#define GLX_MESA_pixmap_colormap	1
#define GLX_MESA_release_buffers	1
#endif



#ifdef MESA
   typedef XMesaContext GLXContext;
   typedef Pixmap GLXPixmap;
   typedef Drawable GLXDrawable;
#else
   typedef void * GLXContext;
   typedef XID GLXPixmap;
   typedef XID GLXDrawable;
#endif
typedef XID GLXContextID;



extern XVisualInfo* glXChooseVisual( Display *dpy, int screen,
				     int *attribList );

extern GLXContext glXCreateContext( Display *dpy, XVisualInfo *vis,
				    GLXContext shareList, Bool direct );

extern void glXDestroyContext( Display *dpy, GLXContext ctx );

extern Bool glXMakeCurrent( Display *dpy, GLXDrawable drawable,
			    GLXContext ctx);

extern void glXCopyContext( Display *dpy, GLXContext src, GLXContext dst,
			    GLuint mask );

extern void glXSwapBuffers( Display *dpy, GLXDrawable drawable );

extern GLXPixmap glXCreateGLXPixmap( Display *dpy, XVisualInfo *visual,
				     Pixmap pixmap );

extern void glXDestroyGLXPixmap( Display *dpy, GLXPixmap pixmap );

extern Bool glXQueryExtension( Display *dpy, int *errorb, int *event );

extern Bool glXQueryVersion( Display *dpy, int *maj, int *min );

extern Bool glXIsDirect( Display *dpy, GLXContext ctx );

extern int glXGetConfig( Display *dpy, XVisualInfo *visual,
			 int attrib, int *value );

extern GLXContext glXGetCurrentContext( void );

extern GLXDrawable glXGetCurrentDrawable( void );

extern void glXWaitGL( void );

extern void glXWaitX( void );

extern void glXUseXFont( Font font, int first, int count, int list );



/* GLX 1.1 and later */
extern const char *glXQueryExtensionsString( Display *dpy, int screen );

extern const char *glXQueryServerString( Display *dpy, int screen, int name );

extern const char *glXGetClientString( Display *dpy, int name );



/*
 * Mesa GLX Extensions
 */

#ifdef GLX_MESA_pixmap_colormap
extern GLXPixmap glXCreateGLXPixmapMESA( Display *dpy, XVisualInfo *visual,
                                         Pixmap pixmap, Colormap cmap );
#endif

#ifdef GLX_MESA_release_buffers
extern Bool glXReleaseBuffersMESA( Display *dpy, Window w );
#endif



#ifdef __cplusplus
}
#endif

#endif
