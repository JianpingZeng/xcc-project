/* $Id: FooMesa.h,v 1.2 1997/02/03 20:03:20 brianp Exp $ */

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
 * $Log: FooMesa.h,v $
 * Revision 1.2  1997/02/03 20:03:20  brianp
 * patches for BeOS
 *
 * Revision 1.1  1996/09/15 14:30:09  brianp
 * Initial revision
 *
 */


/*
 * Example Foo/Mesa interface.  See src/ddsample.c for more info.
 */



#ifndef FOOMESA_H
#define FOOMESA_H



typedef struct foo_mesa_visual  *FooMesaVisual;

typedef struct foo_mesa_buffer  *FooMesaBuffer;

typedef struct foo_mesa_context *FooMesaContext;



#ifdef BEOS
#pragma export on
#endif


extern FooMesaVisual FooMesaChooseVisual( /* your params */ );

extern void FooMesaDestroyVisual( FooMesaVisual visual );


extern FooMesaBuffer FooMesaCreateBuffer( FooMesaVisual visual,
                                          int /* your window id */ );

extern void FooMesaDestroyBuffer( FooMesaBuffer buffer );


extern FooMesaContext FooMesaCreateContext( FooMesaVisual visual,
                                            FooMesaContext sharelist );

extern void FooMesaDestroyContext( FooMesaContext context );


extern void FooMesaMakeCurrent( FooMesaContext context, FooMesaBuffer buffer );


extern void FooMesaSwapBuffers( FooMesaBuffer buffer );


/* Probably some more functions... */


#ifdef BEOS
#pragma export off
#endif

#endif

