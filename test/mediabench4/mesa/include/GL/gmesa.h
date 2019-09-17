/* $Id: gmesa.h,v 1.3 1997/02/19 10:13:54 brianp Exp $ */

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
 * $Log: gmesa.h,v $
 * Revision 1.3  1997/02/19 10:13:54  brianp
 * now test for __QUICKDRAW__ like for __BEOS__ (Randy Frank)
 *
 * Revision 1.2  1997/02/03 20:06:22  brianp
 * patches for BeOS
 *
 * Revision 1.1  1996/09/13 01:26:41  brianp
 * Initial revision
 *
 */


/*
 * (G)eneric Mesa interface
 *
 * This generic interface to Mesa provides minimal functionality.  It's
 * intended to be an _experimental_ interface for projects such as Linux/3-D
 * hardware.  There may eventually be many different implementations of this
 * interface for different operating systems and graphics cards.
 *
 * 
 * Usage:
 *    1. #include <GL/gmesa.h>
 *    2. use GMesaCreateContext() to get a new GMesa context
 *    3. use GMesaMakeCurrent() to activate a GMesa context
 *    4. do your OpenGL rendering
 *    5. use GMesaSwapBuffers() in double buffer mode to swap color buffers
 *    6. use GMesaDestroyContext() to destroy a GMesa context
 *    7. use GMesaGetContext() to return the current context
 *
 *
 * Implementation:
 *    1. GMesaCreateContext() should initialize the hardware and return
 *       a GMesa context struct pointer
 *    2. GMesaMakeCurrent() should activate the context
 *    3. GMesaDestroyContext() should free the context's resources and
 *       reset the hardware
 *    4. GMesaSwapBuffers() should swap the front/back color buffers for
 *       the current context
 *    5. It may be the case that an implementation of this interface only
 *       supports one context at a time.  That's fine.
 */



#ifndef GMESA_H
#define GMESA_H


#ifdef __cplusplus
extern "C" {
#endif


/*
 * A version identifier:
 */
#define GMESA_VERSION 1



/*
 * This is the GMesa context 'handle':
 */
typedef struct gmesa_context *GMesaContext;



#if defined(__BEOS__) || defined(__QUICKDRAW__)
#pragma export on
#endif


extern GMesaContext GMesaCreateContext( void );

extern void GMesaDestroyContext( GMesaContext ctx );

extern void GMesaMakeCurrent( GMesaContext ctx );

extern GMesaContext GMesaGetCurrentContext( void );

extern GMesaSwapBuffers( void );


#if defined(__BEOS__) || defined(__QUICKDRAW__)
#pragma export off
#endif


#ifdef __cplusplus
}
#endif


#endif

