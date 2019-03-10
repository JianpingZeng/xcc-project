/* $Id: gl.h,v 1.11 1997/03/11 00:27:43 brianp Exp $ */

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
 * $Log: gl.h,v $
 * Revision 1.11  1997/03/11 00:27:43  brianp
 * changed GL_POLYGON_OFFSET_FACTOR value to 0x8038
 *
 * Revision 1.10  1997/02/17 17:16:00  brianp
 * now test for __QUICKDRAW__ like for __BEOS__ (Randy Frank)
 *
 * Revision 1.9  1997/02/10 20:05:25  brianp
 * replaced GL_TEXTURE_BINDING_xD_EXT with GL_TEXTURE_xD_BINDING_EXT
 *
 * Revision 1.8  1997/02/10 19:58:29  brianp
 * added GL_MESA_resize_buffers extension
 *
 * Revision 1.7  1997/02/03 20:25:23  brianp
 * added GL_EXT_texture3D
 *
 * Revision 1.6  1997/02/03 20:04:28  brianp
 * patches for BeOS
 *
 * Revision 1.5  1997/02/03 19:14:54  brianp
 * conditionally include gl_mangle.h
 *
 * Revision 1.4  1997/01/30 21:07:44  brianp
 * added some missing 1.1 GLenums, GL_TEXTURE_BINDING_[12]D were misnamed
 *
 * Revision 1.3  1997/01/08 20:56:16  brianp
 * added GL_EXT_texture_object extension functions and enums
 *
 * Revision 1.2  1996/10/11 03:45:23  brianp
 * put GL_EXT_polygon_offset back in
 * removed old texture _EXT symbols
 *
 * Revision 1.1  1996/09/13 01:26:41  brianp
 * Initial revision
 *
 */


#ifndef GL_H
#define GL_H


#if defined(USE_MGL_NAMESPACE)
#include "gl_mangle.h"
#endif


#ifdef __cplusplus
extern "C" {
#endif




/*
 * Apps can test for this symbol to do conditional compilation if needed.
 */
#define MESA


#define GL_VERSION_1_1   1


/*
 *
 * Enumerations
 *
 */

typedef enum {
	/* Boolean values */
	GL_FALSE			= 0,
	GL_TRUE				= 1,

	/* Data types */
	GL_BYTE				= 0x1400,
	GL_UNSIGNED_BYTE		= 0x1401,
	GL_SHORT			= 0x1402,
	GL_UNSIGNED_SHORT		= 0x1403,
	GL_INT				= 0x1404,
	GL_UNSIGNED_INT			= 0x1405,
	GL_FLOAT			= 0x1406,
	GL_DOUBLE			= 0x140A,
	GL_2_BYTES			= 0x1407,
	GL_3_BYTES			= 0x1408,
	GL_4_BYTES			= 0x1409,

	/* Primitives */
	GL_LINES			= 0x0001,
	GL_POINTS			= 0x0000,
	GL_LINE_STRIP			= 0x0003,
	GL_LINE_LOOP			= 0x0002,
	GL_TRIANGLES			= 0x0004,
	GL_TRIANGLE_STRIP		= 0x0005,
	GL_TRIANGLE_FAN			= 0x0006,
	GL_QUADS			= 0x0007,
	GL_QUAD_STRIP			= 0x0008,
	GL_POLYGON			= 0x0009,
	GL_EDGE_FLAG			= 0x0B43,

	/* Vertex Arrays */
	GL_VERTEX_ARRAY			= 0x8074,
	GL_NORMAL_ARRAY			= 0x8075,
	GL_COLOR_ARRAY			= 0x8076,
	GL_INDEX_ARRAY			= 0x8077,
	GL_TEXTURE_COORD_ARRAY		= 0x8078,
	GL_EDGE_FLAG_ARRAY		= 0x8079,
	GL_VERTEX_ARRAY_SIZE		= 0x807A,
	GL_VERTEX_ARRAY_TYPE		= 0x807B,
	GL_VERTEX_ARRAY_STRIDE		= 0x807C,
	GL_NORMAL_ARRAY_TYPE		= 0x807E,
	GL_NORMAL_ARRAY_STRIDE		= 0x807F,
	GL_COLOR_ARRAY_SIZE		= 0x8081,
	GL_COLOR_ARRAY_TYPE		= 0x8082,
	GL_COLOR_ARRAY_STRIDE		= 0x8083,
	GL_INDEX_ARRAY_TYPE		= 0x8085,
	GL_INDEX_ARRAY_STRIDE		= 0x8086,
	GL_TEXTURE_COORD_ARRAY_SIZE	= 0x8088,
	GL_TEXTURE_COORD_ARRAY_TYPE	= 0x8089,
	GL_TEXTURE_COORD_ARRAY_STRIDE	= 0x808A,
	GL_EDGE_FLAG_ARRAY_STRIDE	= 0x808C,
	GL_VERTEX_ARRAY_POINTER		= 0x808E,
	GL_NORMAL_ARRAY_POINTER		= 0x808F,
	GL_COLOR_ARRAY_POINTER		= 0x8090,
	GL_INDEX_ARRAY_POINTER		= 0x8091,
	GL_TEXTURE_COORD_ARRAY_POINTER	= 0x8092,
	GL_EDGE_FLAG_ARRAY_POINTER	= 0x8093,
        GL_V2F				= 0x2A20,
	GL_V3F				= 0x2A21,
	GL_C4UB_V2F			= 0x2A22,
	GL_C4UB_V3F			= 0x2A23,
	GL_C3F_V3F			= 0x2A24,
	GL_N3F_V3F			= 0x2A25,
	GL_C4F_N3F_V3F			= 0x2A26,
	GL_T2F_V3F			= 0x2A27,
	GL_T4F_V4F			= 0x2A28,
	GL_T2F_C4UB_V3F			= 0x2A29,
	GL_T2F_C3F_V3F			= 0x2A2A,
	GL_T2F_N3F_V3F			= 0x2A2B,
	GL_T2F_C4F_N3F_V3F		= 0x2A2C,
	GL_T4F_C4F_N3F_V4F		= 0x2A2D,

	/* Matrix Mode */
	GL_MATRIX_MODE			= 0x0BA0,
	GL_MODELVIEW			= 0x1700,
	GL_PROJECTION			= 0x1701,
	GL_TEXTURE			= 0x1702,

	/* Points */
	GL_POINT_SMOOTH			= 0x0B10,
	GL_POINT_SIZE			= 0x0B11,
	GL_POINT_SIZE_GRANULARITY 	= 0x0B13,
	GL_POINT_SIZE_RANGE		= 0x0B12,

	/* Lines */
	GL_LINE_SMOOTH			= 0x0B20,
	GL_LINE_STIPPLE			= 0x0B24,
	GL_LINE_STIPPLE_PATTERN		= 0x0B25,
	GL_LINE_STIPPLE_REPEAT		= 0x0B26,
	GL_LINE_WIDTH			= 0x0B21,
	GL_LINE_WIDTH_GRANULARITY	= 0x0B23,
	GL_LINE_WIDTH_RANGE		= 0x0B22,

	/* Polygons */
	GL_POINT			= 0x1B00,
	GL_LINE				= 0x1B01,
	GL_FILL				= 0x1B02,
	GL_CCW				= 0x0901,
	GL_CW				= 0x0900,
	GL_FRONT			= 0x0404,
	GL_BACK				= 0x0405,
	GL_CULL_FACE			= 0x0B44,
	GL_CULL_FACE_MODE		= 0x0B45,
	GL_POLYGON_SMOOTH		= 0x0B41,
	GL_POLYGON_STIPPLE		= 0x0B42,
	GL_FRONT_FACE			= 0x0B46,
	GL_POLYGON_MODE			= 0x0B40,
	GL_POLYGON_OFFSET_FACTOR	= 0x8038,
	GL_POLYGON_OFFSET_UNITS		= 0x2A00,
	GL_POLYGON_OFFSET_POINT		= 0x2A01,
	GL_POLYGON_OFFSET_LINE		= 0x2A02,
	GL_POLYGON_OFFSET_FILL		= 0x8037,

	/* Display Lists */
	GL_COMPILE			= 0x1300,
	GL_COMPILE_AND_EXECUTE		= 0x1301,
	GL_LIST_BASE			= 0x0B32,
	GL_LIST_INDEX			= 0x0B33,
	GL_LIST_MODE			= 0x0B30,

	/* Depth buffer */
	GL_NEVER			= 0x0200,
	GL_LESS				= 0x0201,
	GL_GEQUAL			= 0x0206,
	GL_LEQUAL			= 0x0203,
	GL_GREATER			= 0x0204,
	GL_NOTEQUAL			= 0x0205,
	GL_EQUAL			= 0x0202,
	GL_ALWAYS			= 0x0207,
	GL_DEPTH_TEST			= 0x0B71,
	GL_DEPTH_BITS			= 0x0D56,
	GL_DEPTH_CLEAR_VALUE		= 0x0B73,
	GL_DEPTH_FUNC			= 0x0B74,
	GL_DEPTH_RANGE			= 0x0B70,
	GL_DEPTH_WRITEMASK		= 0x0B72,
	GL_DEPTH_COMPONENT		= 0x1902,

	/* Lighting */
	GL_LIGHTING			= 0x0B50,
	GL_LIGHT0			= 0x4000,
	GL_LIGHT1			= 0x4001,
	GL_LIGHT2			= 0x4002,
	GL_LIGHT3			= 0x4003,
	GL_LIGHT4			= 0x4004,
	GL_LIGHT5			= 0x4005,
	GL_LIGHT6			= 0x4006,
	GL_LIGHT7			= 0x4007,
	GL_SPOT_EXPONENT		= 0x1205,
	GL_SPOT_CUTOFF			= 0x1206,
	GL_CONSTANT_ATTENUATION		= 0x1207,
	GL_LINEAR_ATTENUATION		= 0x1208,
	GL_QUADRATIC_ATTENUATION	= 0x1209,
	GL_AMBIENT			= 0x1200,
	GL_DIFFUSE			= 0x1201,
	GL_SPECULAR			= 0x1202,
	GL_SHININESS			= 0x1601,
	GL_EMISSION			= 0x1600,
	GL_POSITION			= 0x1203,
	GL_SPOT_DIRECTION		= 0x1204,
	GL_AMBIENT_AND_DIFFUSE		= 0x1602,
	GL_COLOR_INDEXES		= 0x1603,
	GL_LIGHT_MODEL_TWO_SIDE		= 0x0B52,
	GL_LIGHT_MODEL_LOCAL_VIEWER	= 0x0B51,
	GL_LIGHT_MODEL_AMBIENT		= 0x0B53,
	GL_FRONT_AND_BACK		= 0x0408,
	GL_SHADE_MODEL			= 0x0B54,
	GL_FLAT				= 0x1D00,
	GL_SMOOTH			= 0x1D01,
	GL_COLOR_MATERIAL		= 0x0B57,
	GL_COLOR_MATERIAL_FACE		= 0x0B55,
	GL_COLOR_MATERIAL_PARAMETER	= 0x0B56,
	GL_NORMALIZE			= 0x0BA1,

	/* User clipping planes */
	GL_CLIP_PLANE0			= 0x3000,
	GL_CLIP_PLANE1			= 0x3001,
	GL_CLIP_PLANE2			= 0x3002,
	GL_CLIP_PLANE3			= 0x3003,
	GL_CLIP_PLANE4			= 0x3004,
	GL_CLIP_PLANE5			= 0x3005,

	/* Accumulation buffer */
	GL_ACCUM_RED_BITS		= 0x0D58,
	GL_ACCUM_GREEN_BITS		= 0x0D59,
	GL_ACCUM_BLUE_BITS		= 0x0D5A,
	GL_ACCUM_ALPHA_BITS		= 0x0D5B,
	GL_ACCUM_CLEAR_VALUE		= 0x0B80,
	GL_ACCUM			= 0x0100,
	GL_ADD				= 0x0104,
	GL_LOAD				= 0x0101,
	GL_MULT				= 0x0103,
	GL_RETURN			= 0x0102,

	/* Alpha testing */
	GL_ALPHA_TEST			= 0x0BC0,
	GL_ALPHA_TEST_REF		= 0x0BC2,
	GL_ALPHA_TEST_FUNC		= 0x0BC1,

	/* Blending */
	GL_BLEND			= 0x0BE2,
	GL_BLEND_SRC			= 0x0BE1,
	GL_BLEND_DST			= 0x0BE0,
	GL_ZERO				= 0,
	GL_ONE				= 1,
	GL_SRC_COLOR			= 0x0300,
	GL_ONE_MINUS_SRC_COLOR		= 0x0301,
	GL_DST_COLOR			= 0x0306,
	GL_ONE_MINUS_DST_COLOR		= 0x0307,
	GL_SRC_ALPHA			= 0x0302,
	GL_ONE_MINUS_SRC_ALPHA		= 0x0303,
	GL_DST_ALPHA			= 0x0304,
	GL_ONE_MINUS_DST_ALPHA		= 0x0305,
	GL_SRC_ALPHA_SATURATE		= 0x0308,
	GL_CONSTANT_COLOR		= 0x8001,
	GL_ONE_MINUS_CONSTANT_COLOR	= 0x8002,
	GL_CONSTANT_ALPHA		= 0x8003,
	GL_ONE_MINUS_CONSTANT_ALPHA	= 0x8004,

	/* Render Mode */
	GL_FEEDBACK			= 0x1C01,
	GL_RENDER			= 0x1C00,
	GL_SELECT			= 0x1C02,

	/* Feedback */
	GL_2D				= 0x0600,
	GL_3D				= 0x0601,
	GL_3D_COLOR			= 0x0602,
	GL_3D_COLOR_TEXTURE		= 0x0603,
	GL_4D_COLOR_TEXTURE		= 0x0604,
	GL_POINT_TOKEN			= 0x0701,
	GL_LINE_TOKEN			= 0x0702,
	GL_LINE_RESET_TOKEN		= 0x0707,
	GL_POLYGON_TOKEN		= 0x0703,
	GL_BITMAP_TOKEN			= 0x0704,
	GL_DRAW_PIXEL_TOKEN		= 0x0705,
	GL_COPY_PIXEL_TOKEN		= 0x0706,
	GL_PASS_THROUGH_TOKEN		= 0x0700,
	GL_FEEDBACK_BUFFER_POINTER	= 0x0DF0,
	GL_FEEDBACK_BUFFER_SIZE		= 0x0DF1,
	GL_FEEDBACK_BUFFER_TYPE		= 0x0DF2,


	/* Fog */
	GL_FOG				= 0x0B60,
	GL_FOG_MODE			= 0x0B65,
	GL_FOG_DENSITY			= 0x0B62,
	GL_FOG_COLOR			= 0x0B66,
	GL_FOG_INDEX			= 0x0B61,
	GL_FOG_START			= 0x0B63,
	GL_FOG_END			= 0x0B64,
	GL_LINEAR			= 0x2601,
	GL_EXP				= 0x0800,
	GL_EXP2				= 0x0801,

	/* Logic Ops */
	GL_LOGIC_OP			= 0x0BF1,
	GL_INDEX_LOGIC_OP		= 0x0BF1,
	GL_COLOR_LOGIC_OP		= 0x0BF2,
	GL_LOGIC_OP_MODE		= 0x0BF0,
	GL_CLEAR			= 0x1500,
	GL_SET				= 0x150F,
	GL_COPY				= 0x1503,
	GL_COPY_INVERTED		= 0x150C,
	GL_NOOP				= 0x1505,
	GL_INVERT			= 0x150A,
	GL_AND				= 0x1501,
	GL_NAND				= 0x150E,
	GL_OR				= 0x1507,
	GL_NOR				= 0x1508,
	GL_XOR				= 0x1506,
	GL_EQUIV			= 0x1509,
	GL_AND_REVERSE			= 0x1502,
	GL_AND_INVERTED			= 0x1504,
	GL_OR_REVERSE			= 0x150B,
	GL_OR_INVERTED			= 0x150D,

	/* Stencil */
	GL_STENCIL_TEST			= 0x0B90,
	GL_STENCIL_WRITEMASK		= 0x0B98,
	GL_STENCIL_BITS			= 0x0D57,
	GL_STENCIL_FUNC			= 0x0B92,
	GL_STENCIL_VALUE_MASK		= 0x0B93,
	GL_STENCIL_REF			= 0x0B97,
	GL_STENCIL_FAIL			= 0x0B94,
	GL_STENCIL_PASS_DEPTH_PASS	= 0x0B96,
	GL_STENCIL_PASS_DEPTH_FAIL	= 0x0B95,
	GL_STENCIL_CLEAR_VALUE		= 0x0B91,
	GL_STENCIL_INDEX		= 0x1901,
	GL_KEEP				= 0x1E00,
	GL_REPLACE			= 0x1E01,
	GL_INCR				= 0x1E02,
	GL_DECR				= 0x1E03,

	/* Buffers, Pixel Drawing/Reading */
	GL_NONE				= 0,
	GL_LEFT				= 0x0406,
	GL_RIGHT			= 0x0407,
	/*GL_FRONT			= 0x0404, */
	/*GL_BACK			= 0x0405, */
	/*GL_FRONT_AND_BACK		= 0x0408, */
	GL_FRONT_LEFT			= 0x0400,
	GL_FRONT_RIGHT			= 0x0401,
	GL_BACK_LEFT			= 0x0402,
	GL_BACK_RIGHT			= 0x0403,
	GL_AUX0				= 0x0409,
	GL_AUX1				= 0x040A,
	GL_AUX2				= 0x040B,
	GL_AUX3				= 0x040C,
	GL_COLOR_INDEX			= 0x1900,
	GL_RED				= 0x1903,
	GL_GREEN			= 0x1904,
	GL_BLUE				= 0x1905,
	GL_ALPHA			= 0x1906,
	GL_LUMINANCE			= 0x1909,
	GL_LUMINANCE_ALPHA		= 0x190A,
	GL_ALPHA_BITS			= 0x0D55,
	GL_RED_BITS			= 0x0D52,
	GL_GREEN_BITS			= 0x0D53,
	GL_BLUE_BITS			= 0x0D54,
	GL_INDEX_BITS			= 0x0D51,
	GL_SUBPIXEL_BITS		= 0x0D50,
	GL_AUX_BUFFERS			= 0x0C00,
	GL_READ_BUFFER			= 0x0C02,
	GL_DRAW_BUFFER			= 0x0C01,
	GL_DOUBLEBUFFER			= 0x0C32,
	GL_STEREO			= 0x0C33,
	GL_BITMAP			= 0x1A00,
	GL_COLOR			= 0x1800,
	GL_DEPTH			= 0x1801,
	GL_STENCIL			= 0x1802,
	GL_DITHER			= 0x0BD0,
	GL_RGB				= 0x1907,
	GL_RGBA				= 0x1908,

	/* Implementation limits */
	GL_MAX_LIST_NESTING		= 0x0B31,
	GL_MAX_ATTRIB_STACK_DEPTH	= 0x0D35,
	GL_MAX_MODELVIEW_STACK_DEPTH	= 0x0D36,
	GL_MAX_NAME_STACK_DEPTH		= 0x0D37,
	GL_MAX_PROJECTION_STACK_DEPTH	= 0x0D38,
	GL_MAX_TEXTURE_STACK_DEPTH	= 0x0D39,
	GL_MAX_EVAL_ORDER		= 0x0D30,
	GL_MAX_LIGHTS			= 0x0D31,
	GL_MAX_CLIP_PLANES		= 0x0D32,
	GL_MAX_TEXTURE_SIZE		= 0x0D33,
	GL_MAX_PIXEL_MAP_TABLE		= 0x0D34,
	GL_MAX_VIEWPORT_DIMS		= 0x0D3A,
	GL_MAX_CLIENT_ATTRIB_STACK_DEPTH= 0x0D3B,

	/* Gets */
	GL_ATTRIB_STACK_DEPTH		= 0x0BB0,
	GL_CLIENT_ATTRIB_STACK_DEPTH	= 0x0BB1,
	GL_COLOR_CLEAR_VALUE		= 0x0C22,
	GL_COLOR_WRITEMASK		= 0x0C23,
	GL_CURRENT_INDEX		= 0x0B01,
	GL_CURRENT_COLOR		= 0x0B00,
	GL_CURRENT_NORMAL		= 0x0B02,
	GL_CURRENT_RASTER_COLOR		= 0x0B04,
	GL_CURRENT_RASTER_DISTANCE	= 0x0B09,
	GL_CURRENT_RASTER_INDEX		= 0x0B05,
	GL_CURRENT_RASTER_POSITION	= 0x0B07,
	GL_CURRENT_RASTER_TEXTURE_COORDS = 0x0B06,
	GL_CURRENT_RASTER_POSITION_VALID = 0x0B08,
	GL_CURRENT_TEXTURE_COORDS	= 0x0B03,
	GL_INDEX_CLEAR_VALUE		= 0x0C20,
	GL_INDEX_MODE			= 0x0C30,
	GL_INDEX_WRITEMASK		= 0x0C21,
	GL_MODELVIEW_MATRIX		= 0x0BA6,
	GL_MODELVIEW_STACK_DEPTH	= 0x0BA3,
	GL_NAME_STACK_DEPTH		= 0x0D70,
	GL_PROJECTION_MATRIX		= 0x0BA7,
	GL_PROJECTION_STACK_DEPTH	= 0x0BA4,
	GL_RENDER_MODE			= 0x0C40,
	GL_RGBA_MODE			= 0x0C31,
	GL_TEXTURE_MATRIX		= 0x0BA8,
	GL_TEXTURE_STACK_DEPTH		= 0x0BA5,
	GL_VIEWPORT			= 0x0BA2,


	/* Evaluators */
	GL_AUTO_NORMAL			= 0x0D80,
	GL_MAP1_COLOR_4			= 0x0D90,
	GL_MAP1_GRID_DOMAIN		= 0x0DD0,
	GL_MAP1_GRID_SEGMENTS		= 0x0DD1,
	GL_MAP1_INDEX			= 0x0D91,
	GL_MAP1_NORMAL			= 0x0D92,
	GL_MAP1_TEXTURE_COORD_1		= 0x0D93,
	GL_MAP1_TEXTURE_COORD_2		= 0x0D94,
	GL_MAP1_TEXTURE_COORD_3		= 0x0D95,
	GL_MAP1_TEXTURE_COORD_4		= 0x0D96,
	GL_MAP1_VERTEX_3		= 0x0D97,
	GL_MAP1_VERTEX_4		= 0x0D98,
	GL_MAP2_COLOR_4			= 0x0DB0,
	GL_MAP2_GRID_DOMAIN		= 0x0DD2,
	GL_MAP2_GRID_SEGMENTS		= 0x0DD3,
	GL_MAP2_INDEX			= 0x0DB1,
	GL_MAP2_NORMAL			= 0x0DB2,
	GL_MAP2_TEXTURE_COORD_1		= 0x0DB3,
	GL_MAP2_TEXTURE_COORD_2		= 0x0DB4,
	GL_MAP2_TEXTURE_COORD_3		= 0x0DB5,
	GL_MAP2_TEXTURE_COORD_4		= 0x0DB6,
	GL_MAP2_VERTEX_3		= 0x0DB7,
	GL_MAP2_VERTEX_4		= 0x0DB8,
	GL_COEFF			= 0x0A00,
	GL_DOMAIN			= 0x0A02,
	GL_ORDER			= 0x0A01,

	/* Hints */
	GL_FOG_HINT			= 0x0C54,
	GL_LINE_SMOOTH_HINT		= 0x0C52,
	GL_PERSPECTIVE_CORRECTION_HINT	= 0x0C50,
	GL_POINT_SMOOTH_HINT		= 0x0C51,
	GL_POLYGON_SMOOTH_HINT		= 0x0C53,
	GL_DONT_CARE			= 0x1100,
	GL_FASTEST			= 0x1101,
	GL_NICEST			= 0x1102,

	/* Scissor box */
	GL_SCISSOR_TEST			= 0x0C11,
	GL_SCISSOR_BOX			= 0x0C10,

	/* Pixel Mode / Transfer */
	GL_MAP_COLOR			= 0x0D10,
	GL_MAP_STENCIL			= 0x0D11,
	GL_INDEX_SHIFT			= 0x0D12,
	GL_INDEX_OFFSET			= 0x0D13,
	GL_RED_SCALE			= 0x0D14,
	GL_RED_BIAS			= 0x0D15,
	GL_GREEN_SCALE			= 0x0D18,
	GL_GREEN_BIAS			= 0x0D19,
	GL_BLUE_SCALE			= 0x0D1A,
	GL_BLUE_BIAS			= 0x0D1B,
	GL_ALPHA_SCALE			= 0x0D1C,
	GL_ALPHA_BIAS			= 0x0D1D,
	GL_DEPTH_SCALE			= 0x0D1E,
	GL_DEPTH_BIAS			= 0x0D1F,
	GL_PIXEL_MAP_S_TO_S_SIZE	= 0x0CB1,
	GL_PIXEL_MAP_I_TO_I_SIZE	= 0x0CB0,
	GL_PIXEL_MAP_I_TO_R_SIZE	= 0x0CB2,
	GL_PIXEL_MAP_I_TO_G_SIZE	= 0x0CB3,
	GL_PIXEL_MAP_I_TO_B_SIZE	= 0x0CB4,
	GL_PIXEL_MAP_I_TO_A_SIZE	= 0x0CB5,
	GL_PIXEL_MAP_R_TO_R_SIZE	= 0x0CB6,
	GL_PIXEL_MAP_G_TO_G_SIZE	= 0x0CB7,
	GL_PIXEL_MAP_B_TO_B_SIZE	= 0x0CB8,
	GL_PIXEL_MAP_A_TO_A_SIZE	= 0x0CB9,
	GL_PIXEL_MAP_S_TO_S		= 0x0C71,
	GL_PIXEL_MAP_I_TO_I		= 0x0C70,
	GL_PIXEL_MAP_I_TO_R		= 0x0C72,
	GL_PIXEL_MAP_I_TO_G		= 0x0C73,
	GL_PIXEL_MAP_I_TO_B		= 0x0C74,
	GL_PIXEL_MAP_I_TO_A		= 0x0C75,
	GL_PIXEL_MAP_R_TO_R		= 0x0C76,
	GL_PIXEL_MAP_G_TO_G		= 0x0C77,
	GL_PIXEL_MAP_B_TO_B		= 0x0C78,
	GL_PIXEL_MAP_A_TO_A		= 0x0C79,
	GL_PACK_ALIGNMENT		= 0x0D05,
	GL_PACK_LSB_FIRST		= 0x0D01,
	GL_PACK_ROW_LENGTH		= 0x0D02,
	GL_PACK_SKIP_PIXELS		= 0x0D04,
	GL_PACK_SKIP_ROWS		= 0x0D03,
	GL_PACK_SWAP_BYTES		= 0x0D00,
	GL_UNPACK_ALIGNMENT		= 0x0CF5,
	GL_UNPACK_LSB_FIRST		= 0x0CF1,
	GL_UNPACK_ROW_LENGTH		= 0x0CF2,
	GL_UNPACK_SKIP_PIXELS		= 0x0CF4,
	GL_UNPACK_SKIP_ROWS		= 0x0CF3,
	GL_UNPACK_SWAP_BYTES		= 0x0CF0,
	GL_ZOOM_X			= 0x0D16,
	GL_ZOOM_Y			= 0x0D17,

	/* Texture mapping */
	GL_TEXTURE_ENV			= 0x2300,
	GL_TEXTURE_ENV_MODE		= 0x2200,
	GL_TEXTURE_1D			= 0x0DE0,
	GL_TEXTURE_2D			= 0x0DE1,
	GL_TEXTURE_WRAP_S		= 0x2802,
	GL_TEXTURE_WRAP_T		= 0x2803,
	GL_TEXTURE_MAG_FILTER		= 0x2800,
	GL_TEXTURE_MIN_FILTER		= 0x2801,
	GL_TEXTURE_ENV_COLOR		= 0x2201,
	GL_TEXTURE_GEN_S		= 0x0C60,
	GL_TEXTURE_GEN_T		= 0x0C61,
	GL_TEXTURE_GEN_MODE		= 0x2500,
	GL_TEXTURE_BORDER_COLOR		= 0x1004,
	GL_TEXTURE_WIDTH		= 0x1000,
	GL_TEXTURE_HEIGHT		= 0x1001,
	GL_TEXTURE_BORDER		= 0x1005,
	GL_TEXTURE_COMPONENTS		= 0x1003,
	GL_TEXTURE_RED_SIZE		= 0x805C,
	GL_TEXTURE_GREEN_SIZE		= 0x805D,
	GL_TEXTURE_BLUE_SIZE		= 0x805E,
	GL_TEXTURE_ALPHA_SIZE		= 0x805F,
	GL_TEXTURE_LUMINANCE_SIZE	= 0x8060,
	GL_TEXTURE_INTENSITY_SIZE	= 0x8061,
	GL_NEAREST_MIPMAP_NEAREST	= 0x2700,
	GL_NEAREST_MIPMAP_LINEAR	= 0x2702,
	GL_LINEAR_MIPMAP_NEAREST	= 0x2701,
	GL_LINEAR_MIPMAP_LINEAR		= 0x2703,
	GL_OBJECT_LINEAR		= 0x2401,
	GL_OBJECT_PLANE			= 0x2501,
	GL_EYE_LINEAR			= 0x2400,
	GL_EYE_PLANE			= 0x2502,
	GL_SPHERE_MAP			= 0x2402,
	GL_DECAL			= 0x2101,
	GL_MODULATE			= 0x2100,
	GL_NEAREST			= 0x2600,
	GL_REPEAT			= 0x2901,
	GL_CLAMP			= 0x2900,
	GL_S				= 0x2000,
	GL_T				= 0x2001,
	GL_R				= 0x2002,
	GL_Q				= 0x2003,
	GL_TEXTURE_GEN_R		= 0x0C62,
	GL_TEXTURE_GEN_Q		= 0x0C63,

	GL_PROXY_TEXTURE_1D		= 0x8063,
	GL_PROXY_TEXTURE_2D		= 0x8064,
	GL_TEXTURE_PRIORITY		= 0x8066,
	GL_TEXTURE_RESIDENT		= 0x8067,
	GL_TEXTURE_BINDING_1D		= 0x8068,
	GL_TEXTURE_BINDING_2D		= 0x8069,

	/* Internal texture formats */
	GL_ALPHA4			= 0x803B,
	GL_ALPHA8			= 0x803C,
	GL_ALPHA12			= 0x803D,
	GL_ALPHA16			= 0x803E,
	GL_LUMINANCE4			= 0x803F,
	GL_LUMINANCE8			= 0x8040,
	GL_LUMINANCE12			= 0x8041,
	GL_LUMINANCE16			= 0x8042,
	GL_LUMINANCE4_ALPHA4		= 0x8043,
	GL_LUMINANCE6_ALPHA2		= 0x8044,
	GL_LUMINANCE8_ALPHA8		= 0x8045,
	GL_LUMINANCE12_ALPHA4		= 0x8046,
	GL_LUMINANCE12_ALPHA12		= 0x8047,
	GL_LUMINANCE16_ALPHA16		= 0x8048,
	GL_INTENSITY			= 0x8049,
	GL_INTENSITY4			= 0x804A,
	GL_INTENSITY8			= 0x804B,
	GL_INTENSITY12			= 0x804C,
	GL_INTENSITY16			= 0x804D,
	GL_R3_G3_B2			= 0x2A10,
	GL_RGB4				= 0x804F,
	GL_RGB5				= 0x8050,
	GL_RGB8				= 0x8051,
	GL_RGB10			= 0x8052,
	GL_RGB12			= 0x8053,
	GL_RGB16			= 0x8054,
	GL_RGBA2			= 0x8055,
	GL_RGBA4			= 0x8056,
	GL_RGB5_A1			= 0x8057,
	GL_RGBA8			= 0x8058,
	GL_RGB10_A2			= 0x8059,
	GL_RGBA12			= 0x805A,
	GL_RGBA16			= 0x805B,

	/* Utility */
	GL_VENDOR			= 0x1F00,
	GL_RENDERER			= 0x1F01,
	GL_VERSION			= 0x1F02,
	GL_EXTENSIONS			= 0x1F03,

	/* Errors */
	GL_INVALID_VALUE		= 0x0501,
	GL_INVALID_ENUM			= 0x0500,
	GL_INVALID_OPERATION		= 0x0502,
	GL_STACK_OVERFLOW		= 0x0503,
	GL_STACK_UNDERFLOW		= 0x0504,
	GL_OUT_OF_MEMORY		= 0x0505,

	/*
	 * 1.0 Extensions
	 */
        /* GL_EXT_blend_minmax and GL_EXT_blend_color */
	GL_CONSTANT_COLOR_EXT		= 0x8001,
	GL_ONE_MINUS_CONSTANT_COLOR_EXT	= 0x8002,
	GL_CONSTANT_ALPHA_EXT		= 0x8003,
	GL_ONE_MINUS_CONSTANT_ALPHA_EXT	= 0x8004,
	GL_BLEND_EQUATION_EXT		= 0x8009,
	GL_MIN_EXT			= 0x8007,
	GL_MAX_EXT			= 0x8008,
	GL_FUNC_ADD_EXT			= 0x8006,
	GL_FUNC_SUBTRACT_EXT		= 0x800A,
	GL_FUNC_REVERSE_SUBTRACT_EXT	= 0x800B,
	GL_BLEND_COLOR_EXT		= 0x8005,

	/* GL_EXT_polygon_offset */
        GL_POLYGON_OFFSET_EXT           = 0x8037,
        GL_POLYGON_OFFSET_FACTOR_EXT    = 0x8038,
        GL_POLYGON_OFFSET_BIAS_EXT      = 0x8039,

	/* GL_EXT_vertex_array */
	GL_VERTEX_ARRAY_EXT		= 0x8074,
	GL_NORMAL_ARRAY_EXT		= 0x8075,
	GL_COLOR_ARRAY_EXT		= 0x8076,
	GL_INDEX_ARRAY_EXT		= 0x8077,
	GL_TEXTURE_COORD_ARRAY_EXT	= 0x8078,
	GL_EDGE_FLAG_ARRAY_EXT		= 0x8079,
	GL_VERTEX_ARRAY_SIZE_EXT	= 0x807A,
	GL_VERTEX_ARRAY_TYPE_EXT	= 0x807B,
	GL_VERTEX_ARRAY_STRIDE_EXT	= 0x807C,
	GL_VERTEX_ARRAY_COUNT_EXT	= 0x807D,
	GL_NORMAL_ARRAY_TYPE_EXT	= 0x807E,
	GL_NORMAL_ARRAY_STRIDE_EXT	= 0x807F,
	GL_NORMAL_ARRAY_COUNT_EXT	= 0x8080,
	GL_COLOR_ARRAY_SIZE_EXT		= 0x8081,
	GL_COLOR_ARRAY_TYPE_EXT		= 0x8082,
	GL_COLOR_ARRAY_STRIDE_EXT	= 0x8083,
	GL_COLOR_ARRAY_COUNT_EXT	= 0x8084,
	GL_INDEX_ARRAY_TYPE_EXT		= 0x8085,
	GL_INDEX_ARRAY_STRIDE_EXT	= 0x8086,
	GL_INDEX_ARRAY_COUNT_EXT	= 0x8087,
	GL_TEXTURE_COORD_ARRAY_SIZE_EXT	= 0x8088,
	GL_TEXTURE_COORD_ARRAY_TYPE_EXT	= 0x8089,
	GL_TEXTURE_COORD_ARRAY_STRIDE_EXT= 0x808A,
	GL_TEXTURE_COORD_ARRAY_COUNT_EXT= 0x808B,
	GL_EDGE_FLAG_ARRAY_STRIDE_EXT	= 0x808C,
	GL_EDGE_FLAG_ARRAY_COUNT_EXT	= 0x808D,
	GL_VERTEX_ARRAY_POINTER_EXT	= 0x808E,
	GL_NORMAL_ARRAY_POINTER_EXT	= 0x808F,
	GL_COLOR_ARRAY_POINTER_EXT	= 0x8090,
	GL_INDEX_ARRAY_POINTER_EXT	= 0x8091,
	GL_TEXTURE_COORD_ARRAY_POINTER_EXT= 0x8092,
	GL_EDGE_FLAG_ARRAY_POINTER_EXT	= 0x8093,

	/* GL_EXT_texture_object */
	GL_TEXTURE_PRIORITY_EXT		= 0x8066,
	GL_TEXTURE_RESIDENT_EXT		= 0x8067,
	GL_TEXTURE_1D_BINDING_EXT	= 0x8068,
	GL_TEXTURE_2D_BINDING_EXT	= 0x8069,

	/* EXT_texture3D */
	GL_PACK_SKIP_IMAGES_EXT		= 0x806B,
	GL_PACK_IMAGE_HEIGHT_EXT	= 0x806C,
	GL_UNPACK_SKIP_IMAGES_EXT	= 0x806D,
	GL_UNPACK_IMAGE_HEIGHT_EXT	= 0x806E,
	GL_TEXTURE_3D_EXT		= 0x806F,
	GL_PROXY_TEXTURE_3D_EXT		= 0x8070,
	GL_TEXTURE_DEPTH_EXT		= 0x8071,
	GL_TEXTURE_WRAP_R_EXT		= 0x8072,
	GL_MAX_3D_TEXTURE_SIZE_EXT	= 0x8073,
	GL_TEXTURE_3D_BINDING_EXT	= 0x806A

}
#ifdef CENTERLINE_CLPP
  /* CenterLine C++ workaround: */
  gl_enum;
  typedef int GLenum;
#else
  /* all other compilers */
  GLenum;
#endif


/* GL_NO_ERROR must be zero */
#define GL_NO_ERROR GL_FALSE



enum {
	GL_CURRENT_BIT		= 0x00000001,
	GL_POINT_BIT		= 0x00000002,
	GL_LINE_BIT		= 0x00000004,
	GL_POLYGON_BIT		= 0x00000008,
	GL_POLYGON_STIPPLE_BIT	= 0x00000010,
	GL_PIXEL_MODE_BIT	= 0x00000020,
	GL_LIGHTING_BIT		= 0x00000040,
	GL_FOG_BIT		= 0x00000080,
	GL_DEPTH_BUFFER_BIT	= 0x00000100,
	GL_ACCUM_BUFFER_BIT	= 0x00000200,
	GL_STENCIL_BUFFER_BIT	= 0x00000400,
	GL_VIEWPORT_BIT		= 0x00000800,
	GL_TRANSFORM_BIT	= 0x00001000,
	GL_ENABLE_BIT		= 0x00002000,
	GL_COLOR_BUFFER_BIT	= 0x00004000,
	GL_HINT_BIT		= 0x00008000,
	GL_EVAL_BIT		= 0x00010000,
	GL_LIST_BIT		= 0x00020000,
	GL_TEXTURE_BIT		= 0x00040000,
	GL_SCISSOR_BIT		= 0x00080000,
	GL_ALL_ATTRIB_BITS	= 0x000fffff
};


enum {
	GL_CLIENT_PIXEL_STORE_BIT	= 0x00000001,
	GL_CLIENT_VERTEX_ARRAY_BIT	= 0x00000002,
	GL_CLIENT_ALL_ATTRIB_BITS	= 0x0000FFFF
};



typedef unsigned int GLbitfield;


#ifdef CENTERLINE_CLPP
#define signed
#endif


/*
 *
 * Data types (may be architecture dependent in some cases)
 *
 */

/*  C type		GL type		storage                            */
/*-------------------------------------------------------------------------*/
typedef void		GLvoid;
typedef unsigned char	GLboolean;
typedef signed char	GLbyte;		/* 1-byte signed */
typedef short		GLshort;	/* 2-byte signed */
typedef int		GLint;		/* 4-byte signed */
typedef unsigned char	GLubyte;	/* 1-byte unsigned */
typedef unsigned short	GLushort;	/* 2-byte unsigned */
typedef unsigned int	GLuint;		/* 4-byte unsigned */
typedef int		GLsizei;	/* 4-byte signed */
typedef float		GLfloat;	/* single precision float */
typedef float		GLclampf;	/* single precision float in [0,1] */
typedef double		GLdouble;	/* double precision float */
typedef double		GLclampd;	/* double precision float in [0,1] */



#if defined(__BEOS__) || defined(__QUICKDRAW__)
#pragma export on
#endif


/*
 * Miscellaneous
 */

extern void glClearIndex( GLfloat c );

extern void glClearColor( GLclampf red,
			  GLclampf green,
			  GLclampf blue,
			  GLclampf alpha );

extern void glClear( GLbitfield mask );

extern void glIndexMask( GLuint mask );

extern void glColorMask( GLboolean red, GLboolean green,
			 GLboolean blue, GLboolean alpha );

extern void glAlphaFunc( GLenum func, GLclampf ref );

extern void glBlendFunc( GLenum sfactor, GLenum dfactor );

extern void glLogicOp( GLenum opcode );

extern void glCullFace( GLenum mode );

extern void glFrontFace( GLenum mode );

extern void glPointSize( GLfloat size );

extern void glLineWidth( GLfloat width );

extern void glLineStipple( GLint factor, GLushort pattern );

extern void glPolygonMode( GLenum face, GLenum mode );

extern void glPolygonOffset( GLfloat factor, GLfloat units );

extern void glPolygonStipple( const GLubyte *mask );

extern void glGetPolygonStipple( GLubyte *mask );

extern void glEdgeFlag( GLboolean flag );

extern void glEdgeFlagv( const GLboolean *flag );

extern void glScissor( GLint x, GLint y, GLsizei width, GLsizei height);

extern void glClipPlane( GLenum plane, const GLdouble *equation );

extern void glGetClipPlane( GLenum plane, GLdouble *equation );

extern void glDrawBuffer( GLenum mode );

extern void glReadBuffer( GLenum mode );

extern void glEnable( GLenum cap );

extern void glDisable( GLenum cap );

extern GLboolean glIsEnabled( GLenum cap );


extern void glGetBooleanv( GLenum pname, GLboolean *params );

extern void glGetDoublev( GLenum pname, GLdouble *params );

extern void glGetFloatv( GLenum pname, GLfloat *params );

extern void glGetIntegerv( GLenum pname, GLint *params );


extern void glPushAttrib( GLbitfield mask );

extern void glPopAttrib( void );


extern void glPushClientAttrib( GLbitfield mask );

extern void glPopClientAttrib( void );


extern GLint glRenderMode( GLenum mode );

extern GLenum glGetError( void );

extern const GLubyte *glGetString( GLenum name );

extern void glFinish( void );

extern void glFlush( void );

extern void glHint( GLenum target, GLenum mode );



/*
 * Depth Buffer
 */

extern void glClearDepth( GLclampd depth );

extern void glDepthFunc( GLenum func );

extern void glDepthMask( GLboolean flag );

extern void glDepthRange( GLclampd near_val, GLclampd far_val );


/*
 * Accumulation Buffer
 */

extern void glClearAccum( GLfloat red, GLfloat green,
			  GLfloat blue, GLfloat alpha );

extern void glAccum( GLenum op, GLfloat value );



/*
 * Transformation
 */

extern void glMatrixMode( GLenum mode );

extern void glOrtho( GLdouble left, GLdouble right,
		     GLdouble bottom, GLdouble top,
		     GLdouble near_val, GLdouble far_val );

extern void glFrustum( GLdouble left, GLdouble right,
		       GLdouble bottom, GLdouble top,
		       GLdouble near_val, GLdouble far_val );

extern void glViewport( GLint x, GLint y, GLsizei width, GLsizei height );

extern void glPushMatrix( void );

extern void glPopMatrix( void );

extern void glLoadIdentity( void );

extern void glLoadMatrixd( const GLdouble *m );
extern void glLoadMatrixf( const GLfloat *m );

extern void glMultMatrixd( const GLdouble *m );
extern void glMultMatrixf( const GLfloat *m );

extern void glRotated( GLdouble angle, GLdouble x, GLdouble y, GLdouble z );
extern void glRotatef( GLfloat angle, GLfloat x, GLfloat y, GLfloat z );

extern void glScaled( GLdouble x, GLdouble y, GLdouble z );
extern void glScalef( GLfloat x, GLfloat y, GLfloat z );

extern void glTranslated( GLdouble x, GLdouble y, GLdouble z );
extern void glTranslatef( GLfloat x, GLfloat y, GLfloat z );



/*
 * Display Lists
 */

extern GLboolean glIsList( GLuint list );

extern void glDeleteLists( GLuint list, GLsizei range );

extern GLuint glGenLists( GLsizei range );

extern void glNewList( GLuint list, GLenum mode );

extern void glEndList( void );

extern void glCallList( GLuint list );

extern void glCallLists( GLsizei n, GLenum type, const GLvoid *lists );

extern void glListBase( GLuint base );



/*
 * Drawing Functions
 */

extern void glBegin( GLenum mode );

extern void glEnd( void );


extern void glVertex2d( GLdouble x, GLdouble y );
extern void glVertex2f( GLfloat x, GLfloat y );
extern void glVertex2i( GLint x, GLint y );
extern void glVertex2s( GLshort x, GLshort y );

extern void glVertex3d( GLdouble x, GLdouble y, GLdouble z );
extern void glVertex3f( GLfloat x, GLfloat y, GLfloat z );
extern void glVertex3i( GLint x, GLint y, GLint z );
extern void glVertex3s( GLshort x, GLshort y, GLshort z );

extern void glVertex4d( GLdouble x, GLdouble y, GLdouble z, GLdouble w );
extern void glVertex4f( GLfloat x, GLfloat y, GLfloat z, GLfloat w );
extern void glVertex4i( GLint x, GLint y, GLint z, GLint w );
extern void glVertex4s( GLshort x, GLshort y, GLshort z, GLshort w );

extern void glVertex2dv( const GLdouble *v );
extern void glVertex2fv( const GLfloat *v );
extern void glVertex2iv( const GLint *v );
extern void glVertex2sv( const GLshort *v );

extern void glVertex3dv( const GLdouble *v );
extern void glVertex3fv( const GLfloat *v );
extern void glVertex3iv( const GLint *v );
extern void glVertex3sv( const GLshort *v );

extern void glVertex4dv( const GLdouble *v );
extern void glVertex4fv( const GLfloat *v );
extern void glVertex4iv( const GLint *v );
extern void glVertex4sv( const GLshort *v );


extern void glNormal3b( GLbyte nx, GLbyte ny, GLbyte nz );
extern void glNormal3d( GLdouble nx, GLdouble ny, GLdouble nz );
extern void glNormal3f( GLfloat nx, GLfloat ny, GLfloat nz );
extern void glNormal3i( GLint nx, GLint ny, GLint nz );
extern void glNormal3s( GLshort nx, GLshort ny, GLshort nz );

extern void glNormal3bv( const GLbyte *v );
extern void glNormal3dv( const GLdouble *v );
extern void glNormal3fv( const GLfloat *v );
extern void glNormal3iv( const GLint *v );
extern void glNormal3sv( const GLshort *v );


extern void glIndexd( GLdouble c );
extern void glIndexf( GLfloat c );
extern void glIndexi( GLint c );
extern void glIndexs( GLshort c );
extern void glIndexub( GLubyte c );  /* 1.1 */

extern void glIndexdv( const GLdouble *c );
extern void glIndexfv( const GLfloat *c );
extern void glIndexiv( const GLint *c );
extern void glIndexsv( const GLshort *c );
extern void glIndexubv( const GLubyte *c );  /* 1.1 */

extern void glColor3b( GLbyte red, GLbyte green, GLbyte blue );
extern void glColor3d( GLdouble red, GLdouble green, GLdouble blue );
extern void glColor3f( GLfloat red, GLfloat green, GLfloat blue );
extern void glColor3i( GLint red, GLint green, GLint blue );
extern void glColor3s( GLshort red, GLshort green, GLshort blue );
extern void glColor3ub( GLubyte red, GLubyte green, GLubyte blue );
extern void glColor3ui( GLuint red, GLuint green, GLuint blue );
extern void glColor3us( GLushort red, GLushort green, GLushort blue );

extern void glColor4b( GLbyte red, GLbyte green, GLbyte blue, GLbyte alpha );
extern void glColor4d( GLdouble red, GLdouble green,
		       GLdouble blue, GLdouble alpha );
extern void glColor4f( GLfloat red, GLfloat green,
		       GLfloat blue, GLfloat alpha );
extern void glColor4i( GLint red, GLint green, GLint blue, GLint alpha );
extern void glColor4s( GLshort red, GLshort green,
		       GLshort blue, GLshort alpha );
extern void glColor4ub( GLubyte red, GLubyte green,
			GLubyte blue, GLubyte alpha );
extern void glColor4ui( GLuint red, GLuint green, GLuint blue, GLuint alpha );
extern void glColor4us( GLushort red, GLushort green,
			GLushort blue, GLushort alpha );


extern void glColor3bv( const GLbyte *v );
extern void glColor3dv( const GLdouble *v );
extern void glColor3fv( const GLfloat *v );
extern void glColor3iv( const GLint *v );
extern void glColor3sv( const GLshort *v );
extern void glColor3ubv( const GLubyte *v );
extern void glColor3uiv( const GLuint *v );
extern void glColor3usv( const GLushort *v );

extern void glColor4bv( const GLbyte *v );
extern void glColor4dv( const GLdouble *v );
extern void glColor4fv( const GLfloat *v );
extern void glColor4iv( const GLint *v );
extern void glColor4sv( const GLshort *v );
extern void glColor4ubv( const GLubyte *v );
extern void glColor4uiv( const GLuint *v );
extern void glColor4usv( const GLushort *v );


extern void glTexCoord1d( GLdouble s );
extern void glTexCoord1f( GLfloat s );
extern void glTexCoord1i( GLint s );
extern void glTexCoord1s( GLshort s );

extern void glTexCoord2d( GLdouble s, GLdouble t );
extern void glTexCoord2f( GLfloat s, GLfloat t );
extern void glTexCoord2i( GLint s, GLint t );
extern void glTexCoord2s( GLshort s, GLshort t );

extern void glTexCoord3d( GLdouble s, GLdouble t, GLdouble r );
extern void glTexCoord3f( GLfloat s, GLfloat t, GLfloat r );
extern void glTexCoord3i( GLint s, GLint t, GLint r );
extern void glTexCoord3s( GLshort s, GLshort t, GLshort r );

extern void glTexCoord4d( GLdouble s, GLdouble t, GLdouble r, GLdouble q );
extern void glTexCoord4f( GLfloat s, GLfloat t, GLfloat r, GLfloat q );
extern void glTexCoord4i( GLint s, GLint t, GLint r, GLint q );
extern void glTexCoord4s( GLshort s, GLshort t, GLshort r, GLshort q );

extern void glTexCoord1dv( const GLdouble *v );
extern void glTexCoord1fv( const GLfloat *v );
extern void glTexCoord1iv( const GLint *v );
extern void glTexCoord1sv( const GLshort *v );

extern void glTexCoord2dv( const GLdouble *v );
extern void glTexCoord2fv( const GLfloat *v );
extern void glTexCoord2iv( const GLint *v );
extern void glTexCoord2sv( const GLshort *v );

extern void glTexCoord3dv( const GLdouble *v );
extern void glTexCoord3fv( const GLfloat *v );
extern void glTexCoord3iv( const GLint *v );
extern void glTexCoord3sv( const GLshort *v );

extern void glTexCoord4dv( const GLdouble *v );
extern void glTexCoord4fv( const GLfloat *v );
extern void glTexCoord4iv( const GLint *v );
extern void glTexCoord4sv( const GLshort *v );


extern void glRasterPos2d( GLdouble x, GLdouble y );
extern void glRasterPos2f( GLfloat x, GLfloat y );
extern void glRasterPos2i( GLint x, GLint y );
extern void glRasterPos2s( GLshort x, GLshort y );

extern void glRasterPos3d( GLdouble x, GLdouble y, GLdouble z );
extern void glRasterPos3f( GLfloat x, GLfloat y, GLfloat z );
extern void glRasterPos3i( GLint x, GLint y, GLint z );
extern void glRasterPos3s( GLshort x, GLshort y, GLshort z );

extern void glRasterPos4d( GLdouble x, GLdouble y, GLdouble z, GLdouble w );
extern void glRasterPos4f( GLfloat x, GLfloat y, GLfloat z, GLfloat w );
extern void glRasterPos4i( GLint x, GLint y, GLint z, GLint w );
extern void glRasterPos4s( GLshort x, GLshort y, GLshort z, GLshort w );

extern void glRasterPos2dv( const GLdouble *v );
extern void glRasterPos2fv( const GLfloat *v );
extern void glRasterPos2iv( const GLint *v );
extern void glRasterPos2sv( const GLshort *v );

extern void glRasterPos3dv( const GLdouble *v );
extern void glRasterPos3fv( const GLfloat *v );
extern void glRasterPos3iv( const GLint *v );
extern void glRasterPos3sv( const GLshort *v );

extern void glRasterPos4dv( const GLdouble *v );
extern void glRasterPos4fv( const GLfloat *v );
extern void glRasterPos4iv( const GLint *v );
extern void glRasterPos4sv( const GLshort *v );


extern void glRectd( GLdouble x1, GLdouble y1, GLdouble x2, GLdouble y2 );
extern void glRectf( GLfloat x1, GLfloat y1, GLfloat x2, GLfloat y2 );
extern void glRecti( GLint x1, GLint y1, GLint x2, GLint y2 );
extern void glRects( GLshort x1, GLshort y1, GLshort x2, GLshort y2 );


extern void glRectdv( const GLdouble *v1, const GLdouble *v2 );
extern void glRectfv( const GLfloat *v1, const GLfloat *v2 );
extern void glRectiv( const GLint *v1, const GLint *v2 );
extern void glRectsv( const GLshort *v1, const GLshort *v2 );



/*
 * Vertex Arrays  (1.1)
 */

extern void glVertexPointer( GLint size, GLenum type, GLsizei stride,
                             const GLvoid *ptr );

extern void glNormalPointer( GLenum type, GLsizei stride,
                             const GLvoid *ptr );

extern void glColorPointer( GLint size, GLenum type, GLsizei stride,
                            const GLvoid *ptr );

extern void glIndexPointer( GLenum type, GLsizei stride, const GLvoid *ptr );

extern void glTexCoordPointer( GLint size, GLenum type, GLsizei stride,
                               const GLvoid *ptr );

extern void glEdgeFlagPointer( GLsizei stride, const GLboolean *ptr );

extern void glGetPointerv( GLenum pname, void **params );

extern void glArrayElement( GLint i );

extern void glDrawArrays( GLenum mode, GLint first, GLsizei count );

extern void glDrawElements( GLenum mode, GLsizei count,
                            GLenum type, const GLvoid *indices );

extern void glInterleavedArrays( GLenum format, GLsizei stride,
                                 const GLvoid *pointer );


/*
 * Lighting
 */

extern void glShadeModel( GLenum mode );

extern void glLightf( GLenum light, GLenum pname, GLfloat param );
extern void glLighti( GLenum light, GLenum pname, GLint param );
extern void glLightfv( GLenum light, GLenum pname, const GLfloat *params );
extern void glLightiv( GLenum light, GLenum pname, const GLint *params );

extern void glGetLightfv( GLenum light, GLenum pname, GLfloat *params );
extern void glGetLightiv( GLenum light, GLenum pname, GLint *params );

extern void glLightModelf( GLenum pname, GLfloat param );
extern void glLightModeli( GLenum pname, GLint param );
extern void glLightModelfv( GLenum pname, const GLfloat *params );
extern void glLightModeliv( GLenum pname, const GLint *params );

extern void glMaterialf( GLenum face, GLenum pname, GLfloat param );
extern void glMateriali( GLenum face, GLenum pname, GLint param );
extern void glMaterialfv( GLenum face, GLenum pname, const GLfloat *params );
extern void glMaterialiv( GLenum face, GLenum pname, const GLint *params );

extern void glGetMaterialfv( GLenum face, GLenum pname, GLfloat *params );
extern void glGetMaterialiv( GLenum face, GLenum pname, GLint *params );

extern void glColorMaterial( GLenum face, GLenum mode );




/*
 * Raster functions
 */

extern void glPixelZoom( GLfloat xfactor, GLfloat yfactor );

extern void glPixelStoref( GLenum pname, GLfloat param );
extern void glPixelStorei( GLenum pname, GLint param );

extern void glPixelTransferf( GLenum pname, GLfloat param );
extern void glPixelTransferi( GLenum pname, GLint param );

extern void glPixelMapfv( GLenum map, GLint mapsize, const GLfloat *values );
extern void glPixelMapuiv( GLenum map, GLint mapsize, const GLuint *values );
extern void glPixelMapusv( GLenum map, GLint mapsize, const GLushort *values );

extern void glGetPixelMapfv( GLenum map, GLfloat *values );
extern void glGetPixelMapuiv( GLenum map, GLuint *values );
extern void glGetPixelMapusv( GLenum map, GLushort *values );

extern void glBitmap( GLsizei width, GLsizei height,
		      GLfloat xorig, GLfloat yorig,
		      GLfloat xmove, GLfloat ymove,
		      const GLubyte *bitmap );

extern void glReadPixels( GLint x, GLint y, GLsizei width, GLsizei height,
			  GLenum format, GLenum type, GLvoid *pixels );

extern void glDrawPixels( GLsizei width, GLsizei height,
			  GLenum format, GLenum type, const GLvoid *pixels );

extern void glCopyPixels( GLint x, GLint y, GLsizei width, GLsizei height,
			  GLenum type );



/*
 * Stenciling
 */

extern void glStencilFunc( GLenum func, GLint ref, GLuint mask );

extern void glStencilMask( GLuint mask );

extern void glStencilOp( GLenum fail, GLenum zfail, GLenum zpass );

extern void glClearStencil( GLint s );



/*
 * Texture mapping
 */

extern void glTexGend( GLenum coord, GLenum pname, GLdouble param );
extern void glTexGenf( GLenum coord, GLenum pname, GLfloat param );
extern void glTexGeni( GLenum coord, GLenum pname, GLint param );

extern void glTexGendv( GLenum coord, GLenum pname, const GLdouble *params );
extern void glTexGenfv( GLenum coord, GLenum pname, const GLfloat *params );
extern void glTexGeniv( GLenum coord, GLenum pname, const GLint *params );

extern void glGetTexGendv( GLenum coord, GLenum pname, GLdouble *params );
extern void glGetTexGenfv( GLenum coord, GLenum pname, GLfloat *params );
extern void glGetTexGeniv( GLenum coord, GLenum pname, GLint *params );


extern void glTexEnvf( GLenum target, GLenum pname, GLfloat param );
extern void glTexEnvi( GLenum target, GLenum pname, GLint param );

extern void glTexEnvfv( GLenum target, GLenum pname, const GLfloat *params );
extern void glTexEnviv( GLenum target, GLenum pname, const GLint *params );

extern void glGetTexEnvfv( GLenum target, GLenum pname, GLfloat *params );
extern void glGetTexEnviv( GLenum target, GLenum pname, GLint *params );


extern void glTexParameterf( GLenum target, GLenum pname, GLfloat param );
extern void glTexParameteri( GLenum target, GLenum pname, GLint param );

extern void glTexParameterfv( GLenum target, GLenum pname,
			      const GLfloat *params );
extern void glTexParameteriv( GLenum target, GLenum pname,
			      const GLint *params );

extern void glGetTexParameterfv( GLenum target, GLenum pname, GLfloat *params);
extern void glGetTexParameteriv( GLenum target, GLenum pname, GLint *params );

extern void glGetTexLevelParameterfv( GLenum target, GLint level,
				      GLenum pname, GLfloat *params );
extern void glGetTexLevelParameteriv( GLenum target, GLint level,
				      GLenum pname, GLint *params );


extern void glTexImage1D( GLenum target, GLint level, GLint components,
			  GLsizei width, GLint border,
			  GLenum format, GLenum type, const GLvoid *pixels );

extern void glTexImage2D( GLenum target, GLint level, GLint components,
			  GLsizei width, GLsizei height, GLint border,
			  GLenum format, GLenum type, const GLvoid *pixels );

extern void glGetTexImage( GLenum target, GLint level, GLenum format,
			   GLenum type, GLvoid *pixels );



/* 1.1 functions */

extern void glGenTextures( GLsizei n, GLuint *textures );

extern void glDeleteTextures( GLsizei n, const GLuint *textures);

extern void glBindTexture( GLenum target, GLuint texture );

extern void glPrioritizeTextures( GLsizei n, const GLuint *textures,
                                  const GLclampf *priorities );

extern GLboolean glAreTexturesResident( GLsizei n,
                                      	const GLuint *textures,
                                        GLboolean *residences );

extern GLboolean glIsTexture( GLuint texture );


extern void glTexSubImage1D( GLenum target, GLint level, GLint xoffset,
                             GLsizei width, GLenum format,
                             GLenum type, const GLvoid *pixels );


extern void glTexSubImage2D( GLenum target, GLint level,
                             GLint xoffset, GLint yoffset,
                             GLsizei width, GLsizei height,
                             GLenum format, GLenum type,
                             const GLvoid *pixels );


extern void glCopyTexImage1D( GLenum target, GLint level,
                              GLenum internalformat,
                              GLint x, GLint y,
                              GLsizei width, GLint border );


extern void glCopyTexImage2D( GLenum target, GLint level,
                              GLenum internalformat,
                              GLint x, GLint y,
                              GLsizei width, GLsizei height, GLint border );


extern void glCopyTexSubImage1D( GLenum target, GLint level,
                                 GLint xoffset, GLint x, GLint y,
                                 GLsizei width );


extern void glCopyTexSubImage2D( GLenum target, GLint level,
                                 GLint xoffset, GLint yoffset,
                                 GLint x, GLint y,
                                 GLsizei width, GLsizei height );




/*
 * Evaluators
 */

extern void glMap1d( GLenum target, GLdouble u1, GLdouble u2, GLint stride,
		     GLint order, const GLdouble *points );
extern void glMap1f( GLenum target, GLfloat u1, GLfloat u2, GLint stride,
		     GLint order, const GLfloat *points );

extern void glMap2d( GLenum target,
		     GLdouble u1, GLdouble u2, GLint ustride, GLint uorder,
		     GLdouble v1, GLdouble v2, GLint vstride, GLint vorder,
		     const GLdouble *points );
extern void glMap2f( GLenum target,
		     GLfloat u1, GLfloat u2, GLint ustride, GLint uorder,
		     GLfloat v1, GLfloat v2, GLint vstride, GLint vorder,
		     const GLfloat *points );

extern void glGetMapdv( GLenum target, GLenum query, GLdouble *v );
extern void glGetMapfv( GLenum target, GLenum query, GLfloat *v );
extern void glGetMapiv( GLenum target, GLenum query, GLint *v );

extern void glEvalCoord1d( GLdouble u );
extern void glEvalCoord1f( GLfloat u );

extern void glEvalCoord1dv( const GLdouble *u );
extern void glEvalCoord1fv( const GLfloat *u );

extern void glEvalCoord2d( GLdouble u, GLdouble v );
extern void glEvalCoord2f( GLfloat u, GLfloat v );

extern void glEvalCoord2dv( const GLdouble *u );
extern void glEvalCoord2fv( const GLfloat *u );

extern void glMapGrid1d( GLint un, GLdouble u1, GLdouble u2 );
extern void glMapGrid1f( GLint un, GLfloat u1, GLfloat u2 );

extern void glMapGrid2d( GLint un, GLdouble u1, GLdouble u2,
			 GLint vn, GLdouble v1, GLdouble v2 );
extern void glMapGrid2f( GLint un, GLfloat u1, GLfloat u2,
			 GLint vn, GLfloat v1, GLfloat v2 );

extern void glEvalPoint1( GLint i );

extern void glEvalPoint2( GLint i, GLint j );

extern void glEvalMesh1( GLenum mode, GLint i1, GLint i2 );

extern void glEvalMesh2( GLenum mode, GLint i1, GLint i2, GLint j1, GLint j2 );



/*
 * Fog
 */

extern void glFogf( GLenum pname, GLfloat param );

extern void glFogi( GLenum pname, GLint param );

extern void glFogfv( GLenum pname, const GLfloat *params );

extern void glFogiv( GLenum pname, const GLint *params );



/*
 * Selection and Feedback
 */

extern void glFeedbackBuffer( GLsizei size, GLenum type, GLfloat *buffer );

extern void glPassThrough( GLfloat token );

extern void glSelectBuffer( GLsizei size, GLuint *buffer );

extern void glInitNames( void );

extern void glLoadName( GLuint name );

extern void glPushName( GLuint name );

extern void glPopName( void );



/*
 * 1.0 Extensions
 */

/* GL_EXT_blend_minmax */
extern void glBlendEquationEXT( GLenum mode );



/* GL_EXT_blend_color */
extern void glBlendColorEXT( GLclampf red, GLclampf green,
			     GLclampf blue, GLclampf alpha );



/* GL_EXT_polygon_offset */
extern void glPolygonOffsetEXT( GLfloat factor, GLfloat bias );



/* GL_EXT_vertex_array */

extern void glVertexPointerEXT( GLint size, GLenum type, GLsizei stride,
                             	GLsizei count, const GLvoid *ptr );

extern void glNormalPointerEXT( GLenum type, GLsizei stride,
				GLsizei count, const GLvoid *ptr );

extern void glColorPointerEXT( GLint size, GLenum type, GLsizei stride,
                               GLsizei count, const GLvoid *ptr );

extern void glIndexPointerEXT( GLenum type, GLsizei stride,
			       GLsizei count, const GLvoid *ptr );

extern void glTexCoordPointerEXT( GLint size, GLenum type, GLsizei stride,
                                  GLsizei count, const GLvoid *ptr );

extern void glEdgeFlagPointerEXT( GLsizei stride,
				  GLsizei count,  const GLboolean *ptr );

extern void glGetPointervEXT( GLenum pname, void **params );

extern void glArrayElementEXT( GLint i );

extern void glDrawArraysEXT( GLenum mode, GLint first, GLsizei count );



/* GL_EXT_texture_object */

extern void glGenTexturesEXT( GLsizei n, GLuint *textures );

extern void glDeleteTexturesEXT( GLsizei n, const GLuint *textures);

extern void glBindTextureEXT( GLenum target, GLuint texture );

extern void glPrioritizeTexturesEXT( GLsizei n, const GLuint *textures,
                                     const GLclampf *priorities );

extern GLboolean glAreTexturesResidentEXT( GLsizei n,
                                           const GLuint *textures,
                                           GLboolean *residences );

extern GLboolean glIsTextureEXT( GLuint texture );



/* GL_EXT_texture3D */

extern void glTexImage3DEXT( GLenum target, GLint level, GLenum internalformat,
                             GLsizei width, GLsizei height, GLsizei depth,
                             GLint border, GLenum format, GLenum type, 
                             const GLvoid *pixels );
 
extern void glTexSubImage3DEXT( GLenum target, GLint level, GLint xoffset, 
                                GLint yoffset, GLint zoffset, GLsizei width, 
                                GLsizei height, GLsizei depth, GLenum format,
                                GLenum type, const GLvoid *pixels );

extern void glCopyTexSubImage3DEXT( GLenum target, GLint level, GLint xoffset,
                                    GLint yoffset, GLint zoffset, 
                                    GLint x, GLint y, GLsizei width, 
                                    GLsizei height );


/* GL_MESA_window_pos */

extern void glWindowPos2iMESA( GLint x, GLint y );
extern void glWindowPos2sMESA( GLshort x, GLshort y );
extern void glWindowPos2fMESA( GLfloat x, GLfloat y );
extern void glWindowPos2dMESA( GLdouble x, GLdouble y );

extern void glWindowPos2ivMESA( const GLint *p );
extern void glWindowPos2svMESA( const GLshort *p );
extern void glWindowPos2fvMESA( const GLfloat *p );
extern void glWindowPos2dvMESA( const GLdouble *p );

extern void glWindowPos3iMESA( GLint x, GLint y, GLint z );
extern void glWindowPos3sMESA( GLshort x, GLshort y, GLshort z );
extern void glWindowPos3fMESA( GLfloat x, GLfloat y, GLfloat z );
extern void glWindowPos3dMESA( GLdouble x, GLdouble y, GLdouble z );

extern void glWindowPos3ivMESA( const GLint *p );
extern void glWindowPos3svMESA( const GLshort *p );
extern void glWindowPos3fvMESA( const GLfloat *p );
extern void glWindowPos3dvMESA( const GLdouble *p );

extern void glWindowPos4iMESA( GLint x, GLint y, GLint z, GLint w );
extern void glWindowPos4sMESA( GLshort x, GLshort y, GLshort z, GLshort w );
extern void glWindowPos4fMESA( GLfloat x, GLfloat y, GLfloat z, GLfloat w );
extern void glWindowPos4dMESA( GLdouble x, GLdouble y, GLdouble z, GLdouble w);

extern void glWindowPos4ivMESA( const GLint *p );
extern void glWindowPos4svMESA( const GLshort *p );
extern void glWindowPos4fvMESA( const GLfloat *p );
extern void glWindowPos4dvMESA( const GLdouble *p );


/* GL_MESA_resize_buffers */

extern void glResizeBuffersMESA();



#if defined(__BEOS__) || defined(__QUICKDRAW__)
#pragma export off
#endif


/*
 * Compile-time tests for extensions:
 */
#define GL_EXT_blend_color	1
#define GL_EXT_blend_logic_op	1
#define GL_EXT_blend_minmax	1
#define GL_EXT_blend_subtract	1
#define GL_EXT_polygon_offset	1
#define GL_EXT_vertex_array	1
#define GL_EXT_texture_object	1
#define GL_EXT_texture3D	1
#define GL_MESA_window_pos	1
#define GL_MESA_resize_buffers	1



#ifdef __cplusplus
}
#endif

#endif
