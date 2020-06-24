/* $Id: glu_mangle.h,v 1.1 1997/02/03 19:16:38 brianp Exp $ */

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
 * $Log: glu_mangle.h,v $
 * Revision 1.1  1997/02/03 19:16:38  brianp
 * Initial revision
 *
 */


#define gluLookAt mgluLookAt
#define gluOrtho2D mgluOrtho2D
#define gluPerspective mgluPerspective
#define gluPickMatrix mgluPickMatrix
#define gluProject mgluProject
#define gluUnProject mgluUnProject
#define gluErrorString mgluErrorString
#define gluScaleImage mgluScaleImage
#define gluBuild1DMipmaps mgluBuild1DMipmaps
#define gluBuild2DMipmaps mgluBuild2DMipmaps
#define gluNewQuadric mgluNewQuadric
#define gluDeleteQuadric mgluDeleteQuadric
#define gluQuadricDrawStyle mgluQuadricDrawStyle
#define gluQuadricOrientation mgluQuadricOrientation
#define gluQuadricNormals mgluQuadricNormals
#define gluQuadricTexture mgluQuadricTexture
#define gluQuadricCallback mgluQuadricCallback
#define gluCylinder mgluCylinder
#define gluSphere mgluSphere
#define gluDisk mgluDisk
#define gluPartialDisk mgluPartialDisk
#define gluNewNurbsRenderer mgluNewNurbsRenderer
#define gluDeleteNurbsRenderer mgluDeleteNurbsRenderer
#define gluLoadSamplingMatrices mgluLoadSamplingMatrices
#define gluNurbsProperty mgluNurbsProperty
#define gluGetNurbsProperty mgluGetNurbsProperty
#define gluBeginCurve mgluBeginCurve
#define gluEndCurve mgluEndCurve
#define gluNurbsCurve mgluNurbsCurve
#define gluBeginSurface mgluBeginSurface
#define gluEndSurface mgluEndSurface
#define gluNurbsSurface mgluNurbsSurface
#define gluBeginTrim mgluBeginTrim
#define gluEndTrim mgluEndTrim
#define gluPwlCurve mgluPwlCurve
#define gluNurbsCallback mgluNurbsCallback
#define gluNewTess mgluNewTess
#define gluTessCallback mgluTessCallback
#define gluDeleteTess mgluDeleteTess
#define gluBeginPolygon mgluBeginPolygon
#define gluEndPolygon mgluEndPolygon
#define gluNextContour mgluNextContour
#define gluTessVertex mgluTessVertex
#define gluGetString mgluGetString
