/* $Id: svgamesa.h,v 1.3 1996/10/30 03:12:47 brianp Exp $ */

/*
 * Mesa 3-D graphics library
 * Version:  2.1
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
 * $Log: svgamesa.h,v $
 * Revision 1.3  1996/10/30 03:12:47  brianp
 * incremented version to 2.1
 *
 * Revision 1.2  1996/10/15 00:26:02  brianp
 * added prototype for SVGAMesaSwapBuffers()
 *
 * Revision 1.1  1996/09/13 01:26:41  brianp
 * Initial revision
 *
 */



/*
 * SVGA/Mesa interface for Linux.
 */


/*
 * Intro to using the VGA/Mesa interface
 *
 * 1. #include the <vga.h> file
 * 2. Call vga_init() to initialize the SVGA library.
 * 3. Call vga_setmode() to specify the screen size and color depth.
 * 4. Call SVGAMesaCreateContext() to setup a Mesa context.  If using 8-bit
 *    color Mesa assumes color index mode, if using 16-bit or deeper color
 *    Mesa assumes RGB mode.
 * 5. Call SVGAMesaMakeCurrent() to activate the Mesa context.
 * 6. You can now use the Mesa API functions.
 * 7. Before exiting, call SVGAMesaDestroyContext() then vga_setmode(TEXT)
 *    to restore the original text screen.
 *
 * Notes
 * 1. You must run your executable as root (or use the set UID-bit) because
 *    the SVGA library requires it.
 * 2. The SVGA driver is not fully implemented yet.  See svgamesa.c for what
 *    has to be done yet.
 */


#ifndef SVGAMESA_H
#define SVGAMESA_H


#define SVGAMESA_MAJOR_VERSION 2
#define SVGAMESA_MINOR_VERSION 1



#ifdef __cplusplus
extern "C" {
#endif


/*
 * This is the SVGAMesa context 'handle':
 */
typedef struct svgamesa_context *SVGAMesaContext;



extern SVGAMesaContext SVGAMesaCreateContext( void );

extern void SVGAMesaDestroyContext( SVGAMesaContext ctx );

extern void SVGAMesaMakeCurrent( SVGAMesaContext ctx );

extern SVGAMesaContext SVGAMesaGetCurrentContext( void );

extern void SVGAMesaSwapBuffers( void );


#ifdef __cplusplus
}
#endif


#endif

