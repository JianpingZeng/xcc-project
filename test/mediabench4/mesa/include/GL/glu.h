/* $Id: glu.h,v 1.4 1997/02/19 10:13:54 brianp Exp $ */

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
 * $Log: glu.h,v $
 * Revision 1.4  1997/02/19 10:13:54  brianp
 * now test for __QUICKDRAW__ like for __BEOS__ (Randy Frank)
 *
 * Revision 1.3  1997/02/03 20:05:33  brianp
 * patches for BeOS
 *
 * Revision 1.2  1997/02/03 19:15:15  brianp
 * conditionally include glu_mangle.h
 *
 * Revision 1.1  1996/09/13 01:26:41  brianp
 * Initial revision
 *
 */


#ifndef GLU_H
#define GLU_H


#if defined(USE_MGL_NAMESPACE)
#include "glu_mangle.h"
#endif


#ifdef __cplusplus
extern "C" {
#endif


#include "GL/gl.h"


#define GLU_VERSION_1_1		1


#define GLU_TRUE   GL_TRUE
#define GLU_FALSE  GL_FALSE


enum {
	/* Normal vectors */
	GLU_SMOOTH	= 100000,
	GLU_FLAT	= 100001,
	GLU_NONE	= 100002,

	/* Quadric draw styles */
	GLU_POINT	= 100010,
	GLU_LINE	= 100011,
	GLU_FILL	= 100012,
	GLU_SILHOUETTE	= 100013,

	/* Quadric orientation */
	GLU_OUTSIDE	= 100020,
	GLU_INSIDE	= 100021,

	/* Tesselator */
	GLU_BEGIN	= 100100,
	GLU_VERTEX	= 100101,
	GLU_END		= 100102,
	GLU_ERROR	= 100103,
	GLU_EDGE_FLAG	= 100104,

	/* Contour types */
	GLU_CW		= 100120,
	GLU_CCW		= 100121,
	GLU_INTERIOR	= 100122,
	GLU_EXTERIOR	= 100123,
	GLU_UNKNOWN	= 100124,

	/* Tesselation errors */
	GLU_TESS_ERROR1	= 100151,  /* missing gluEndPolygon */
	GLU_TESS_ERROR2 = 100152,  /* missing gluBeginPolygon */
	GLU_TESS_ERROR3 = 100153,  /* misoriented contour */
	GLU_TESS_ERROR4 = 100154,  /* vertex/edge intersection */
	GLU_TESS_ERROR5 = 100155,  /* misoriented or self-intersecting loops */
	GLU_TESS_ERROR6 = 100156,  /* coincident vertices */
	GLU_TESS_ERROR7 = 100157,  /* all vertices collinear */
	GLU_TESS_ERROR8 = 100158,  /* intersecting edges */
	GLU_TESS_ERROR9 = 100159,  /* not coplanar contours */

	/* NURBS */
	GLU_AUTO_LOAD_MATRIX	= 100200,
	GLU_CULLING		= 100201,
	GLU_PARAMETRIC_TOLERANCE= 100202,
	GLU_SAMPLING_TOLERANCE	= 100203,
	GLU_DISPLAY_MODE	= 100204,
	GLU_SAMPLING_METHOD	= 100205,
	GLU_U_STEP		= 100206,
	GLU_V_STEP		= 100207,

	GLU_PATH_LENGTH		= 100215,
	GLU_PARAMETRIC_ERROR	= 100216,
	GLU_DOMAIN_DISTANCE	= 100217,

	GLU_MAP1_TRIM_2		= 100210,
	GLU_MAP1_TRIM_3		= 100211,

	GLU_OUTLINE_POLYGON	= 100240,
	GLU_OUTLINE_PATCH	= 100241,

	GLU_NURBS_ERROR1  = 100251,   /* spline order un-supported */
	GLU_NURBS_ERROR2  = 100252,   /* too few knots */
	GLU_NURBS_ERROR3  = 100253,   /* valid knot range is empty */
	GLU_NURBS_ERROR4  = 100254,   /* decreasing knot sequence */
	GLU_NURBS_ERROR5  = 100255,   /* knot multiplicity > spline order */
	GLU_NURBS_ERROR6  = 100256,   /* endcurve() must follow bgncurve() */
	GLU_NURBS_ERROR7  = 100257,   /* bgncurve() must precede endcurve() */
	GLU_NURBS_ERROR8  = 100258,   /* ctrlarray or knot vector is NULL */
	GLU_NURBS_ERROR9  = 100259,   /* can't draw pwlcurves */
	GLU_NURBS_ERROR10 = 100260,   /* missing gluNurbsCurve() */
	GLU_NURBS_ERROR11 = 100261,   /* missing gluNurbsSurface() */
	GLU_NURBS_ERROR12 = 100262,   /* endtrim() must precede endsurface() */
	GLU_NURBS_ERROR13 = 100263,   /* bgnsurface() must precede endsurface() */
	GLU_NURBS_ERROR14 = 100264,   /* curve of improper type passed as trim curve */
	GLU_NURBS_ERROR15 = 100265,   /* bgnsurface() must precede bgntrim() */
	GLU_NURBS_ERROR16 = 100266,   /* endtrim() must follow bgntrim() */
	GLU_NURBS_ERROR17 = 100267,   /* bgntrim() must precede endtrim()*/
	GLU_NURBS_ERROR18 = 100268,   /* invalid or missing trim curve*/
	GLU_NURBS_ERROR19 = 100269,   /* bgntrim() must precede pwlcurve() */
	GLU_NURBS_ERROR20 = 100270,   /* pwlcurve referenced twice*/
	GLU_NURBS_ERROR21 = 100271,   /* pwlcurve and nurbscurve mixed */
	GLU_NURBS_ERROR22 = 100272,   /* improper usage of trim data type */
	GLU_NURBS_ERROR23 = 100273,   /* nurbscurve referenced twice */
	GLU_NURBS_ERROR24 = 100274,   /* nurbscurve and pwlcurve mixed */
	GLU_NURBS_ERROR25 = 100275,   /* nurbssurface referenced twice */
	GLU_NURBS_ERROR26 = 100276,   /* invalid property */
	GLU_NURBS_ERROR27 = 100277,   /* endsurface() must follow bgnsurface() */
	GLU_NURBS_ERROR28 = 100278,   /* intersecting or misoriented trim curves */
	GLU_NURBS_ERROR29 = 100279,   /* intersecting trim curves */
	GLU_NURBS_ERROR30 = 100280,   /* UNUSED */
	GLU_NURBS_ERROR31 = 100281,   /* unconnected trim curves */
	GLU_NURBS_ERROR32 = 100282,   /* unknown knot error */
	GLU_NURBS_ERROR33 = 100283,   /* negative vertex count encountered */
	GLU_NURBS_ERROR34 = 100284,   /* negative byte-stride */
	GLU_NURBS_ERROR35 = 100285,   /* unknown type descriptor */
	GLU_NURBS_ERROR36 = 100286,   /* null control point reference */
	GLU_NURBS_ERROR37 = 100287,   /* duplicate point on pwlcurve */

	/* Errors */
	GLU_INVALID_ENUM		= 100900,
	GLU_INVALID_VALUE		= 100901,
	GLU_OUT_OF_MEMORY		= 100902,
	GLU_INCOMPATIBLE_GL_VERSION	= 100903,

	/* New in GLU 1.1 */
	GLU_VERSION	= 100800,
	GLU_EXTENSIONS	= 100801
};


typedef struct GLUquadricObj GLUquadricObj;

typedef struct GLUtriangulatorObj GLUtriangulatorObj;

typedef struct GLUnurbsObj GLUnurbsObj;



#if defined(__BEOS__) || defined(__QUICKDRAW__)
#pragma export on
#endif


/*
 *
 * Miscellaneous functions
 *
 */

extern void gluLookAt( GLdouble eyex, GLdouble eyey, GLdouble eyez,
		       GLdouble centerx, GLdouble centery, GLdouble centerz,
		       GLdouble upx, GLdouble upy, GLdouble upz );


extern void gluOrtho2D( GLdouble left, GLdouble right,
		        GLdouble bottom, GLdouble top );


extern void gluPerspective( GLdouble fovy, GLdouble aspect,
			    GLdouble zNear, GLdouble zFar );


extern void gluPickMatrix( GLdouble x, GLdouble y,
			   GLdouble width, GLdouble height,
			   GLint viewport[4] );

extern GLint gluProject( GLdouble objx, GLdouble objy, GLdouble objz,
                         const GLdouble modelMatrix[16],
                         const GLdouble projMatrix[16],
                         const GLint viewport[4],
                         GLdouble *winx, GLdouble *winy, GLdouble *winz );

extern GLint gluUnProject( GLdouble winx, GLdouble winy, GLdouble winz,
                           const GLdouble modelMatrix[16],
                           const GLdouble projMatrix[16],
                           const GLint viewport[4],
                           GLdouble *objx, GLdouble *objy, GLdouble *objz );

extern const GLubyte* gluErrorString( GLenum errorCode );



/*
 *
 * Mipmapping and image scaling
 *
 */

extern GLint gluScaleImage( GLenum format,
                            GLint widthin, GLint heightin,
                            GLenum typein, const void *datain,
                            GLint widthout, GLint heightout,
                            GLenum typeout, void *dataout );

extern GLint gluBuild1DMipmaps( GLenum target, GLint components,
			        GLint width, GLenum format,
			        GLenum type, const void *data );

extern GLint gluBuild2DMipmaps( GLenum target, GLint components,
                                GLint width, GLint height, GLenum format,
                                GLenum type, const void *data );



/*
 *
 * Quadrics
 *
 */

extern GLUquadricObj *gluNewQuadric( void );

extern void gluDeleteQuadric( GLUquadricObj *state );

extern void gluQuadricDrawStyle( GLUquadricObj *quadObject,
				 GLenum drawStyle );

extern void gluQuadricOrientation( GLUquadricObj *quadObject,
				   GLenum orientation );

extern void gluQuadricNormals( GLUquadricObj *quadObject, GLenum normals );

extern void gluQuadricTexture( GLUquadricObj *quadObject,
			       GLboolean textureCoords );

extern void gluQuadricCallback( GLUquadricObj *qobj,
			        GLenum which, void (*fn)() );

extern void gluCylinder( GLUquadricObj *qobj,
			 GLdouble baseRadius,
			 GLdouble topRadius,
			 GLdouble height,
			 GLint slices, GLint stacks );

extern void gluSphere( GLUquadricObj *qobj,
		       GLdouble radius, GLint slices, GLint stacks );

extern void gluDisk( GLUquadricObj *qobj,
		     GLdouble innerRadius, GLdouble outerRadius,
		     GLint slices, GLint loops );

extern void gluPartialDisk( GLUquadricObj *qobj, GLdouble innerRadius,
			    GLdouble outerRadius, GLint slices, GLint loops,
			    GLdouble startAngle, GLdouble sweepAngle );



/*
 *
 * Nurbs
 *
 */

extern GLUnurbsObj *gluNewNurbsRenderer( void );

extern void gluDeleteNurbsRenderer( GLUnurbsObj *nobj );

extern void gluLoadSamplingMatrices( GLUnurbsObj *nobj,
				     const GLfloat modelMatrix[16],
				     const GLfloat projMatrix[16],
				     const GLint viewport[4] );

extern void gluNurbsProperty( GLUnurbsObj *nobj, GLenum property,
			      GLfloat value );

extern void gluGetNurbsProperty( GLUnurbsObj *nobj, GLenum property,
				 GLfloat *value );

extern void gluBeginCurve( GLUnurbsObj *nobj );

extern void gluEndCurve( GLUnurbsObj * nobj );

extern void gluNurbsCurve( GLUnurbsObj *nobj, GLint nknots, GLfloat *knot,
			   GLint stride, GLfloat *ctlarray, GLint order,
			   GLenum type );

extern void gluBeginSurface( GLUnurbsObj *nobj );

extern void gluEndSurface( GLUnurbsObj * nobj );

extern void gluNurbsSurface( GLUnurbsObj *nobj,
			     GLint sknot_count, GLfloat *sknot,
			     GLint tknot_count, GLfloat *tknot,
			     GLint s_stride, GLint t_stride,
			     GLfloat *ctlarray,
			     GLint sorder, GLint torder,
        	             GLenum type );

extern void gluBeginTrim( GLUnurbsObj *nobj );

extern void gluEndTrim( GLUnurbsObj *nobj );

extern void gluPwlCurve( GLUnurbsObj *nobj, GLint count, GLfloat *array,
			 GLint stride, GLenum type );

extern void gluNurbsCallback( GLUnurbsObj *nobj, GLenum which, void (*fn)() );



/*
 *
 * Polygon tesselation
 *
 */

extern GLUtriangulatorObj* gluNewTess( void );

extern void gluTessCallback( GLUtriangulatorObj *tobj, GLenum which,
			      void (*fn)() );

extern void gluDeleteTess( GLUtriangulatorObj *tobj );

extern void gluBeginPolygon( GLUtriangulatorObj *tobj );

extern void gluEndPolygon( GLUtriangulatorObj *tobj );

extern void gluNextContour( GLUtriangulatorObj *tobj, GLenum type );

extern void gluTessVertex( GLUtriangulatorObj *tobj, GLdouble v[3],
			   void *data );



/*
 *
 * New functions in GLU 1.1
 *
 */

extern const GLubyte* gluGetString( GLenum name );


#if defined(__BEOS__) || defined(__QUICKDRAW__)
#pragma export off
#endif


#ifdef __cplusplus
}
#endif


#endif
