/* $Id: pointers.c,v 1.4 1997/02/10 19:49:29 brianp Exp $ */

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
 * $Log: pointers.c,v $
 * Revision 1.4  1997/02/10 19:49:29  brianp
 * added glResizeBuffersMESA() code
 *
 * Revision 1.3  1997/02/09 18:50:42  brianp
 * added GL_EXT_texture3D support
 *
 * Revision 1.2  1996/10/16 00:52:22  brianp
 * gl_initialize_api_function_pointers() now gl_init_api_function_pointers()
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "accum.h"
#include "alpha.h"
#include "attrib.h"
#include "bitmap.h"
#include "blend.h"
#include "clip.h"
#include "context.h"
#include "copypix.h"
#include "depth.h"
#include "draw.h"
#include "drawpix.h"
#include "enable.h"
#include "eval.h"
#include "feedback.h"
#include "fog.h"
#include "get.h"
#include "light.h"
#include "lines.h"
#include "dlist.h"
#include "logic.h"
#include "macros.h"
#include "masking.h"
#include "matrix.h"
#include "misc.h"
#include "pixel.h"
#include "points.h"
#include "polygon.h"
#include "readpix.h"
#include "scissor.h"
#include "stencil.h"
#include "teximage.h"
#include "texobj.h"
#include "texture.h"
#include "types.h"
#include "varray.h"
#include "vb.h"
#include "vertex.h"
#include "winpos.h"




/*
 * For debugging
 */
static void check_pointers( struct api_function_table *table )
{
   void **entry;
   int numentries = sizeof( struct api_function_table ) / sizeof(void*);
   int i;

   entry = (void **) table;

   for (i=0;i<numentries;i++) {
      if (!entry[i]) {
         printf("found uninitialized function pointer at %d\n", i );
      }
   }
}



void gl_init_api_function_pointers( GLcontext *ctx )
{
   /*
    * Immediate/execute functions
    */
   ctx->Exec.Accum = gl_Accum;
   ctx->Exec.AlphaFunc = gl_AlphaFunc;
   ctx->Exec.AreTexturesResident = gl_AreTexturesResident;
   ctx->Exec.ArrayElement = gl_ArrayElement;
   ctx->Exec.Begin = gl_Begin;
   ctx->Exec.BindTexture = gl_BindTexture;
   ctx->Exec.Bitmap = gl_Bitmap;
   ctx->Exec.BlendColor = gl_BlendColor;
   ctx->Exec.BlendEquation = gl_BlendEquation;
   ctx->Exec.BlendFunc = gl_BlendFunc;
   ctx->Exec.CallList = gl_CallList;
   ctx->Exec.CallLists = gl_CallLists;
   ctx->Exec.Clear = gl_Clear;
   ctx->Exec.ClearAccum = gl_ClearAccum;
   ctx->Exec.ClearColor = gl_ClearColor;
   ctx->Exec.ClearDepth = gl_ClearDepth;
   ctx->Exec.ClearIndex = gl_ClearIndex;
   ctx->Exec.ClearStencil = gl_ClearStencil;
   ctx->Exec.ClipPlane = gl_ClipPlane;
   ctx->Exec.Color4f = gl_Color4f;
   ctx->Exec.Color4ub = gl_Color4ub;
   ctx->Exec.ColorMask = gl_ColorMask;
   ctx->Exec.ColorMaterial = gl_ColorMaterial;
   ctx->Exec.ColorPointer = gl_ColorPointer;
   ctx->Exec.CopyPixels = gl_CopyPixels;
   ctx->Exec.CopyTexImage1D = gl_CopyTexImage1D;
   ctx->Exec.CopyTexImage2D = gl_CopyTexImage2D;
   ctx->Exec.CopyTexSubImage1D = gl_CopyTexSubImage1D;
   ctx->Exec.CopyTexSubImage2D = gl_CopyTexSubImage2D;
   ctx->Exec.CopyTexSubImage3DEXT = gl_CopyTexSubImage3DEXT;
   ctx->Exec.CullFace = gl_CullFace;
   ctx->Exec.DeleteLists = gl_DeleteLists;
   ctx->Exec.DeleteTextures = gl_DeleteTextures;
   ctx->Exec.DepthFunc = gl_DepthFunc;
   ctx->Exec.DepthMask = gl_DepthMask;
   ctx->Exec.DepthRange = gl_DepthRange;
   ctx->Exec.Disable = gl_Disable;
   ctx->Exec.DisableClientState = gl_DisableClientState;
   ctx->Exec.DrawArrays = gl_DrawArrays;
   ctx->Exec.DrawBuffer = gl_DrawBuffer;
   ctx->Exec.DrawElements = gl_DrawElements;
   ctx->Exec.DrawPixels = gl_DrawPixels;
   ctx->Exec.EdgeFlag = gl_EdgeFlag;
   ctx->Exec.EdgeFlagPointer = gl_EdgeFlagPointer;
   ctx->Exec.Enable = gl_Enable;
   ctx->Exec.EnableClientState = gl_EnableClientState;
   ctx->Exec.End = gl_End;
   ctx->Exec.EndList = gl_EndList;
   ctx->Exec.EvalCoord1f = gl_EvalCoord1f;
   ctx->Exec.EvalCoord2f = gl_EvalCoord2f;
   ctx->Exec.EvalMesh1 = gl_EvalMesh1;
   ctx->Exec.EvalMesh2 = gl_EvalMesh2;
   ctx->Exec.EvalPoint1 = gl_EvalPoint1;
   ctx->Exec.EvalPoint2 = gl_EvalPoint2;
   ctx->Exec.FeedbackBuffer = gl_FeedbackBuffer;
   ctx->Exec.Finish = gl_Finish;
   ctx->Exec.Flush = gl_Flush;
   ctx->Exec.Fogfv = gl_Fogfv;
   ctx->Exec.FrontFace = gl_FrontFace;
   ctx->Exec.Frustum = gl_Frustum;
   ctx->Exec.GenLists = gl_GenLists;
   ctx->Exec.GenTextures = gl_GenTextures;
   ctx->Exec.GetBooleanv = gl_GetBooleanv;
   ctx->Exec.GetClipPlane = gl_GetClipPlane;
   ctx->Exec.GetDoublev = gl_GetDoublev;
   ctx->Exec.GetError = gl_GetError;
   ctx->Exec.GetFloatv = gl_GetFloatv;
   ctx->Exec.GetIntegerv = gl_GetIntegerv;
   ctx->Exec.GetPointerv = gl_GetPointerv;
   ctx->Exec.GetLightfv = gl_GetLightfv;
   ctx->Exec.GetLightiv = gl_GetLightiv;
   ctx->Exec.GetMapdv = gl_GetMapdv;
   ctx->Exec.GetMapfv = gl_GetMapfv;
   ctx->Exec.GetMapiv = gl_GetMapiv;
   ctx->Exec.GetMaterialfv = gl_GetMaterialfv;
   ctx->Exec.GetMaterialiv = gl_GetMaterialiv;
   ctx->Exec.GetPixelMapfv = gl_GetPixelMapfv;
   ctx->Exec.GetPixelMapuiv = gl_GetPixelMapuiv;
   ctx->Exec.GetPixelMapusv = gl_GetPixelMapusv;
   ctx->Exec.GetPolygonStipple = gl_GetPolygonStipple;
   ctx->Exec.GetString = gl_GetString;
   ctx->Exec.GetTexEnvfv = gl_GetTexEnvfv;
   ctx->Exec.GetTexEnviv = gl_GetTexEnviv;
   ctx->Exec.GetTexGendv = gl_GetTexGendv;
   ctx->Exec.GetTexGenfv = gl_GetTexGenfv;
   ctx->Exec.GetTexGeniv = gl_GetTexGeniv;
   ctx->Exec.GetTexImage = gl_GetTexImage;
   ctx->Exec.GetTexLevelParameterfv = gl_GetTexLevelParameterfv;
   ctx->Exec.GetTexLevelParameteriv = gl_GetTexLevelParameteriv;
   ctx->Exec.GetTexParameterfv = gl_GetTexParameterfv;
   ctx->Exec.GetTexParameteriv = gl_GetTexParameteriv;
   ctx->Exec.Hint = gl_Hint;
   ctx->Exec.Indexf = gl_Indexf;
   ctx->Exec.Indexi = gl_Indexi;
   ctx->Exec.IndexMask = gl_IndexMask;
   ctx->Exec.IndexPointer = gl_IndexPointer;
   ctx->Exec.InitNames = gl_InitNames;
   ctx->Exec.InterleavedArrays = gl_InterleavedArrays;
   ctx->Exec.IsEnabled = gl_IsEnabled;
   ctx->Exec.IsList = gl_IsList;
   ctx->Exec.IsTexture = gl_IsTexture;
   ctx->Exec.LightModelfv = gl_LightModelfv;
   ctx->Exec.Lightfv = gl_Lightfv;
   ctx->Exec.LineStipple = gl_LineStipple;
   ctx->Exec.LineWidth = gl_LineWidth;
   ctx->Exec.ListBase = gl_ListBase;
   ctx->Exec.LoadMatrixf = gl_LoadMatrixf;
   ctx->Exec.LoadName = gl_LoadName;
   ctx->Exec.LogicOp = gl_LogicOp;
   ctx->Exec.Map1f = gl_Map1f;
   ctx->Exec.Map2f = gl_Map2f;
   ctx->Exec.MapGrid1f = gl_MapGrid1f;
   ctx->Exec.MapGrid2f = gl_MapGrid2f;
   ctx->Exec.Materialfv = gl_Materialfv;
   ctx->Exec.MatrixMode = gl_MatrixMode;
   ctx->Exec.MultMatrixf = gl_MultMatrixf;
   ctx->Exec.NewList = gl_NewList;
   ctx->Exec.Normal3f = gl_Normal3f;
   ctx->Exec.NormalPointer = gl_NormalPointer;
   ctx->Exec.Normal3fv = gl_Normal3fv;
   ctx->Exec.PassThrough = gl_PassThrough;
   ctx->Exec.PixelMapfv = gl_PixelMapfv;
   ctx->Exec.PixelStorei = gl_PixelStorei;
   ctx->Exec.PixelTransferf = gl_PixelTransferf;
   ctx->Exec.PixelZoom = gl_PixelZoom;
   ctx->Exec.PointSize = gl_PointSize;
   ctx->Exec.PolygonMode = gl_PolygonMode;
   ctx->Exec.PolygonOffset = gl_PolygonOffset;
   ctx->Exec.PolygonStipple = gl_PolygonStipple;
   ctx->Exec.PopAttrib = gl_PopAttrib;
   ctx->Exec.PopClientAttrib = gl_PopClientAttrib;
   ctx->Exec.PopMatrix = gl_PopMatrix;
   ctx->Exec.PopName = gl_PopName;
   ctx->Exec.PrioritizeTextures = gl_PrioritizeTextures;
   ctx->Exec.PushAttrib = gl_PushAttrib;
   ctx->Exec.PushClientAttrib = gl_PushClientAttrib;
   ctx->Exec.PushMatrix = gl_PushMatrix;
   ctx->Exec.PushName = gl_PushName;
   ctx->Exec.RasterPos4f = gl_RasterPos4f;
   ctx->Exec.ReadBuffer = gl_ReadBuffer;
   ctx->Exec.ReadPixels = gl_ReadPixels;
   ctx->Exec.Rectf = gl_Rectf;
   ctx->Exec.RenderMode = gl_RenderMode;
   ctx->Exec.Rotatef = gl_Rotatef;
   ctx->Exec.Scalef = gl_Scalef;
   ctx->Exec.Scissor = gl_Scissor;
   ctx->Exec.SelectBuffer = gl_SelectBuffer;
   ctx->Exec.ShadeModel = gl_ShadeModel;
   ctx->Exec.StencilFunc = gl_StencilFunc;
   ctx->Exec.StencilMask = gl_StencilMask;
   ctx->Exec.StencilOp = gl_StencilOp;
   ctx->Exec.TexCoord4f = gl_TexCoord4f;
   ctx->Exec.TexCoordPointer = gl_TexCoordPointer;
   ctx->Exec.TexEnvfv = gl_TexEnvfv;
   ctx->Exec.TexGenfv = gl_TexGenfv;
   ctx->Exec.TexImage1D = gl_TexImage1D;
   ctx->Exec.TexImage2D = gl_TexImage2D;
   ctx->Exec.TexImage3DEXT = gl_TexImage3DEXT;
   ctx->Exec.TexSubImage1D = gl_TexSubImage1D;
   ctx->Exec.TexSubImage2D = gl_TexSubImage2D;
   ctx->Exec.TexSubImage3DEXT = gl_TexSubImage3DEXT;
   ctx->Exec.TexParameterfv = gl_TexParameterfv;
   ctx->Exec.Translatef = gl_Translatef;
   ctx->Exec.Vertex4f = gl_nop_vertex;          /* FILLED IN LATER */
   ctx->Exec.VertexPointer = gl_VertexPointer;
   ctx->Exec.Viewport = gl_Viewport;

   /* Blending extextensions */

   /* GL_MESA_window_pos extension */
   ctx->Exec.WindowPos4fMESA = gl_WindowPos4fMESA;

   /* GL_MESA_resize_buffers extension */
   ctx->Exec.ResizeBuffersMESA = gl_ResizeBuffersMESA;

   /*
    * Display list compile/save functions
    */
   ctx->Save.Accum = gl_save_Accum;
   ctx->Save.AlphaFunc = gl_save_AlphaFunc;
   ctx->Save.AreTexturesResident = gl_AreTexturesResident;
   ctx->Save.ArrayElement = gl_save_ArrayElement;
   ctx->Save.Begin = gl_save_Begin;
   ctx->Save.BindTexture = gl_save_BindTexture;
   ctx->Save.Bitmap = gl_save_Bitmap;
   ctx->Save.BlendColor = gl_save_BlendColor;
   ctx->Save.BlendEquation = gl_save_BlendEquation;
   ctx->Save.BlendFunc = gl_save_BlendFunc;
   ctx->Save.CallList = gl_save_CallList;
   ctx->Save.CallLists = gl_save_CallLists;
   ctx->Save.Clear = gl_save_Clear;
   ctx->Save.ClearAccum = gl_save_ClearAccum;
   ctx->Save.ClearColor = gl_save_ClearColor;
   ctx->Save.ClearDepth = gl_save_ClearDepth;
   ctx->Save.ClearIndex = gl_save_ClearIndex;
   ctx->Save.ClearStencil = gl_save_ClearStencil;
   ctx->Save.ClipPlane = gl_save_ClipPlane;
   ctx->Save.Color4f = gl_save_Color4f;
   ctx->Save.Color4ub = gl_save_Color4ub;
   ctx->Save.ColorMask = gl_save_ColorMask;
   ctx->Save.ColorMaterial = gl_save_ColorMaterial;
   ctx->Save.ColorPointer = gl_ColorPointer;
   ctx->Save.CopyPixels = gl_save_CopyPixels;
   ctx->Save.CopyTexImage1D = gl_save_CopyTexImage1D;
   ctx->Save.CopyTexImage2D = gl_save_CopyTexImage2D;
   ctx->Save.CopyTexSubImage1D = gl_save_CopyTexSubImage1D;
   ctx->Save.CopyTexSubImage2D = gl_save_CopyTexSubImage2D;
   ctx->Save.CopyTexSubImage3DEXT = gl_save_CopyTexSubImage3DEXT;
   ctx->Save.CullFace = gl_save_CullFace;
   ctx->Save.DeleteLists = gl_DeleteLists;   /* NOT SAVED */
   ctx->Save.DeleteTextures = gl_DeleteTextures;  /* NOT SAVED */
   ctx->Save.DepthFunc = gl_save_DepthFunc;
   ctx->Save.DepthMask = gl_save_DepthMask;
   ctx->Save.DepthRange = gl_save_DepthRange;
   ctx->Save.Disable = gl_save_Disable;
   ctx->Save.DisableClientState = gl_DisableClientState;  /* NOT SAVED */
   ctx->Save.DrawArrays = gl_save_DrawArrays;
   ctx->Save.DrawBuffer = gl_save_DrawBuffer;
   ctx->Save.DrawElements = gl_save_DrawElements;
   ctx->Save.DrawPixels = gl_DrawPixels;   /* SPECIAL CASE */
   ctx->Save.EdgeFlag = gl_save_EdgeFlag;
   ctx->Save.EdgeFlagPointer = gl_EdgeFlagPointer;
   ctx->Save.Enable = gl_save_Enable;
   ctx->Save.EnableClientState = gl_EnableClientState;   /* NOT SAVED */
   ctx->Save.End = gl_save_End;
   ctx->Save.EndList = gl_EndList;   /* NOT SAVED */
   ctx->Save.EvalCoord1f = gl_save_EvalCoord1f;
   ctx->Save.EvalCoord2f = gl_save_EvalCoord2f;
   ctx->Save.EvalMesh1 = gl_save_EvalMesh1;
   ctx->Save.EvalMesh2 = gl_save_EvalMesh2;
   ctx->Save.EvalPoint1 = gl_save_EvalPoint1;
   ctx->Save.EvalPoint2 = gl_save_EvalPoint2;
   ctx->Save.FeedbackBuffer = gl_FeedbackBuffer;   /* NOT SAVED */
   ctx->Save.Finish = gl_Finish;   /* NOT SAVED */
   ctx->Save.Flush = gl_Flush;   /* NOT SAVED */
   ctx->Save.Fogfv = gl_save_Fogfv;
   ctx->Save.FrontFace = gl_save_FrontFace;
   ctx->Save.Frustum = gl_save_Frustum;
   ctx->Save.GenLists = gl_GenLists;   /* NOT SAVED */
   ctx->Save.GenTextures = gl_GenTextures;   /* NOT SAVED */

   /* NONE OF THESE COMMANDS ARE COMPILED INTO DISPLAY LISTS */
   ctx->Save.GetBooleanv = gl_GetBooleanv;
   ctx->Save.GetClipPlane = gl_GetClipPlane;
   ctx->Save.GetDoublev = gl_GetDoublev;
   ctx->Save.GetError = gl_GetError;
   ctx->Save.GetFloatv = gl_GetFloatv;
   ctx->Save.GetIntegerv = gl_GetIntegerv;
   ctx->Save.GetString = gl_GetString;
   ctx->Save.GetLightfv = gl_GetLightfv;
   ctx->Save.GetLightiv = gl_GetLightiv;
   ctx->Save.GetMapdv = gl_GetMapdv;
   ctx->Save.GetMapfv = gl_GetMapfv;
   ctx->Save.GetMapiv = gl_GetMapiv;
   ctx->Save.GetMaterialfv = gl_GetMaterialfv;
   ctx->Save.GetMaterialiv = gl_GetMaterialiv;
   ctx->Save.GetPixelMapfv = gl_GetPixelMapfv;
   ctx->Save.GetPixelMapuiv = gl_GetPixelMapuiv;
   ctx->Save.GetPixelMapusv = gl_GetPixelMapusv;
   ctx->Save.GetPointerv = gl_GetPointerv;
   ctx->Save.GetPolygonStipple = gl_GetPolygonStipple;
   ctx->Save.GetTexEnvfv = gl_GetTexEnvfv;
   ctx->Save.GetTexEnviv = gl_GetTexEnviv;
   ctx->Save.GetTexGendv = gl_GetTexGendv;
   ctx->Save.GetTexGenfv = gl_GetTexGenfv;
   ctx->Save.GetTexGeniv = gl_GetTexGeniv;
   ctx->Save.GetTexImage = gl_GetTexImage;
   ctx->Save.GetTexLevelParameterfv = gl_GetTexLevelParameterfv;
   ctx->Save.GetTexLevelParameteriv = gl_GetTexLevelParameteriv;
   ctx->Save.GetTexParameterfv = gl_GetTexParameterfv;
   ctx->Save.GetTexParameteriv = gl_GetTexParameteriv;

   ctx->Save.Hint = gl_save_Hint;
   ctx->Save.IndexMask = gl_save_IndexMask;
   ctx->Save.Indexf = gl_save_Indexf;
   ctx->Save.Indexi = gl_save_Indexi;
   ctx->Save.IndexPointer = gl_IndexPointer;
   ctx->Save.InitNames = gl_save_InitNames;
   ctx->Save.InterleavedArrays = gl_save_InterleavedArrays;
   ctx->Save.IsEnabled = gl_IsEnabled;   /* NOT SAVED */
   ctx->Save.IsTexture = gl_IsTexture;   /* NOT SAVED */
   ctx->Save.IsList = gl_IsList;   /* NOT SAVED */
   ctx->Save.LightModelfv = gl_save_LightModelfv;
   ctx->Save.Lightfv = gl_save_Lightfv;
   ctx->Save.LineStipple = gl_save_LineStipple;
   ctx->Save.LineWidth = gl_save_LineWidth;
   ctx->Save.ListBase = gl_save_ListBase;
   ctx->Save.LoadMatrixf = gl_save_LoadMatrixf;
   ctx->Save.LoadName = gl_save_LoadName;
   ctx->Save.LogicOp = gl_save_LogicOp;
   ctx->Save.Map1f = gl_save_Map1f;
   ctx->Save.Map2f = gl_save_Map2f;
   ctx->Save.MapGrid1f = gl_save_MapGrid1f;
   ctx->Save.MapGrid2f = gl_save_MapGrid2f;
   ctx->Save.Materialfv = gl_save_Materialfv;
   ctx->Save.MatrixMode = gl_save_MatrixMode;
   ctx->Save.MultMatrixf = gl_save_MultMatrixf;
   ctx->Save.NewList = gl_save_NewList;
   ctx->Save.Normal3f = gl_save_Normal3f;
   ctx->Save.Normal3fv = gl_save_Normal3fv;
   ctx->Save.NormalPointer = gl_NormalPointer;  /* NOT SAVED */
   ctx->Save.PassThrough = gl_save_PassThrough;
   ctx->Save.PixelMapfv = gl_save_PixelMapfv;
   ctx->Save.PixelStorei = gl_PixelStorei;   /* NOT SAVED */
   ctx->Save.PixelTransferf = gl_save_PixelTransferf;
   ctx->Save.PixelZoom = gl_save_PixelZoom;
   ctx->Save.PointSize = gl_save_PointSize;
   ctx->Save.PolygonMode = gl_save_PolygonMode;
   ctx->Save.PolygonOffset = gl_save_PolygonOffset;
   ctx->Save.PolygonStipple = gl_save_PolygonStipple;
   ctx->Save.PopAttrib = gl_save_PopAttrib;
   ctx->Save.PopClientAttrib = gl_PopClientAttrib;  /* NOT SAVED */
   ctx->Save.PopMatrix = gl_save_PopMatrix;
   ctx->Save.PopName = gl_save_PopName;
   ctx->Save.PrioritizeTextures = gl_save_PrioritizeTextures;
   ctx->Save.PushAttrib = gl_save_PushAttrib;
   ctx->Save.PushClientAttrib = gl_PushClientAttrib;  /* NOT SAVED */
   ctx->Save.PushMatrix = gl_save_PushMatrix;
   ctx->Save.PushName = gl_save_PushName;
   ctx->Save.RasterPos4f = gl_save_RasterPos4f;
   ctx->Save.ReadBuffer = gl_save_ReadBuffer;
   ctx->Save.ReadPixels = gl_ReadPixels;   /* NOT SAVED */
   ctx->Save.Rectf = gl_Rectf;   /* SEE gl_Rectf() CODE */
   ctx->Save.RenderMode = gl_RenderMode;   /* NOT SAVED */
   ctx->Save.Rotatef = gl_save_Rotatef;
   ctx->Save.Scalef = gl_save_Scalef;
   ctx->Save.Scissor = gl_save_Scissor;
   ctx->Save.SelectBuffer = gl_SelectBuffer;   /* NOT SAVED */
   ctx->Save.ShadeModel = gl_save_ShadeModel;
   ctx->Save.StencilFunc = gl_save_StencilFunc;
   ctx->Save.StencilMask = gl_save_StencilMask;
   ctx->Save.StencilOp = gl_save_StencilOp;
   ctx->Save.TexCoord4f = gl_save_TexCoord4f;
   ctx->Save.TexCoordPointer = gl_TexCoordPointer;  /* NOT SAVED */
   ctx->Save.TexEnvfv = gl_save_TexEnvfv;
   ctx->Save.TexGenfv = gl_save_TexGenfv;
   ctx->Save.TexImage1D = gl_save_TexImage1D;
   ctx->Save.TexImage2D = gl_save_TexImage2D;
   ctx->Save.TexImage3DEXT = gl_save_TexImage3DEXT;
   ctx->Save.TexSubImage1D = gl_save_TexSubImage1D;
   ctx->Save.TexSubImage2D = gl_save_TexSubImage2D;
   ctx->Save.TexSubImage3DEXT = gl_save_TexSubImage3DEXT;
   ctx->Save.TexParameterfv = gl_save_TexParameterfv;
   ctx->Save.Translatef = gl_save_Translatef;
   ctx->Save.Vertex4f = gl_save_Vertex4f;
   ctx->Save.VertexPointer = gl_VertexPointer;  /* NOT SAVED */
   ctx->Save.Viewport = gl_save_Viewport;

   /* GL_SAVE_MESA_window_pos extension */
   ctx->Save.WindowPos4fMESA = gl_save_WindowPos4fMESA;

   /* GL_MESA_resize_buffers extension */
   ctx->Save.ResizeBuffersMESA = gl_ResizeBuffersMESA;


   /* TODO: remove this someday */
   check_pointers( &ctx->Exec );
   check_pointers( &ctx->Save );
}

