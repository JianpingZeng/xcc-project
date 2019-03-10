/* $Id: wmesa.h,v 1.2 1997/02/22 16:48:49 brianp Exp $ */

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
 *
 * Windows driver by: Mark E. Peterson (markp@ic.mankato.mn.us)
 * Updated by Li Wei (liwei@aiar.xjtu.edu.cn)
 *
 */


/*
 * $Log: wmesa.h,v $
 * Revision 1.2  1997/02/22 16:48:49  brianp
 * WMesaDestroyContext now takes no arguments
 *
 * Revision 1.1  1996/09/13 01:26:41  brianp
 * Initial revision
 *
 */



#ifndef WMESA_H
#define WMESA_H


#ifdef __cplusplus
extern "C" {
#endif


#include <windows.h>
#include "gl\gl.h"


/*
 * This is the WMesa context 'handle':
 */
typedef struct wmesa_context *WMesaContext;



/*
 * Create a new WMesaContext for rendering into a window.  You must
 * have already created the window of correct visual type and with an
 * appropriate colormap.
 *
 * Input:
 *         hWnd - Window handle
 *         Pal  - Palette to use
 *         rgb_flag - GL_TRUE = RGB mode,
 *                    GL_FALSE = color index mode
 *         db_flag - GL_TRUE = double-buffered,
 *                   GL_FALSE = single buffered
 *
 * Note: Indexed mode requires double buffering under Windows.
 *
 * Return:  a WMesa_context or NULL if error.
 */
extern WMesaContext WMesaCreateContext(HWND hWnd,HPALETTE Pal,
                                       GLboolean rgb_flag,GLboolean db_flag);


/*
 * Destroy a rendering context as returned by WMesaCreateContext()
 */
/*extern void WMesaDestroyContext( WMesaContext ctx );*/
extern void WMesaDestroyContext( void );


/*
 * Make the specified context the current one.
 */
extern void WMesaMakeCurrent( WMesaContext ctx );


/*
 * Return a handle to the current context.
 */
extern WMesaContext WMesaGetCurrentContext( void );


/*
 * Swap the front and back buffers for the current context.  No action
 * taken if the context is not double buffered.
 */
extern void WMesaSwapBuffers(void);


/*
 * In indexed color mode we need to know when the palette changes.
 */
extern void WMesaPaletteChange(HPALETTE Pal);



#ifdef __cplusplus
}
#endif


#endif

