/* $Id: light.c,v 1.7 1997/03/11 00:37:39 brianp Exp $ */

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
 * $Log: light.c,v $
 * Revision 1.7  1997/03/11 00:37:39  brianp
 * spotlight factor now effects ambient lighting
 *
 * Revision 1.6  1996/12/18 20:02:07  brianp
 * glColorMaterial() and glMaterial() should finally work right!
 *
 * Revision 1.5  1996/12/07 10:22:41  brianp
 * gl_Materialfv() now calls gl_set_material() if GL_COLOR_MATERIAL disabled
 * implemented gl_GetLightiv()
 *
 * Revision 1.4  1996/11/08 04:39:23  brianp
 * new gl_compute_spot_exp_table() contributed by Randy Frank
 *
 * Revision 1.3  1996/09/27 01:27:55  brianp
 * removed unused variables
 *
 * Revision 1.2  1996/09/15 14:18:10  brianp
 * now use GLframebuffer and GLvisual
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#include <assert.h>
#include <float.h>
#include <math.h>
#include <stdlib.h>
#include "context.h"
#include "light.h"
#include "dlist.h"
#include "macros.h"
#include "matrix.h"
#include "types.h"
#include "vb.h"
#include "xform.h"


#ifdef DEBUG
#  define ASSERT(X)  assert(X)
#else
#  define ASSERT(X)
#endif


#define DEG2RAD (M_PI/180.0)



void gl_ShadeModel( GLcontext *ctx, GLenum mode )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glShadeModel" );
      return;
   }

   switch (mode) {
      case GL_FLAT:
      case GL_SMOOTH:
         if (ctx->Light.ShadeModel!=mode) {
            ctx->Light.ShadeModel = mode;
            ctx->NewState |= NEW_RASTER_OPS;
         }
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glShadeModel" );
   }
}



void gl_Lightfv( GLcontext *ctx,
                 GLenum light, GLenum pname, const GLfloat *params,
                 GLint nparams )
{
   GLint l;

   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glShadeModel" );
      return;
   }

   l = (GLint) (light - GL_LIGHT0);

   if (l<0 || l>=MAX_LIGHTS) {
      gl_error( ctx, GL_INVALID_ENUM, "glLight" );
      return;
   }

   switch (pname) {
      case GL_AMBIENT:
         COPY_4V( ctx->Light.Light[l].Ambient, params );
         break;
      case GL_DIFFUSE:
         COPY_4V( ctx->Light.Light[l].Diffuse, params );
         break;
      case GL_SPECULAR:
         COPY_4V( ctx->Light.Light[l].Specular, params );
         break;
      case GL_POSITION:
	 /* transform position by ModelView matrix */
	 TRANSFORM_POINT( ctx->Light.Light[l].Position, ctx->ModelViewMatrix,
                          params );
         break;
      case GL_SPOT_DIRECTION:
	 /* transform direction by inverse modelview */
         {
            GLfloat direction[4];
            direction[0] = params[0];
            direction[1] = params[1];
            direction[2] = params[2];
            direction[3] = 0.0;
            if (!ctx->ModelViewInvValid) {
               gl_compute_modelview_inverse( ctx );
            }
            gl_transform_vector( ctx->Light.Light[l].Direction,
                                 direction, ctx->ModelViewInv);
         }
         break;
      case GL_SPOT_EXPONENT:
         if (params[0]<0.0 || params[0]>128.0) {
            gl_error( ctx, GL_INVALID_VALUE, "glLight" );
            return;
         }
         ctx->Light.Light[l].SpotExponent = params[0];
         gl_compute_spot_exp_table( &ctx->Light.Light[l] );
         break;
      case GL_SPOT_CUTOFF:
         if ((params[0]<0.0 || params[0]>90.0) && params[0]!=180.0) {
            gl_error( ctx, GL_INVALID_VALUE, "glLight" );
            return;
         }
         ctx->Light.Light[l].SpotCutoff = params[0];
         ctx->Light.Light[l].CosCutoff = cos(params[0]*DEG2RAD);
         break;
      case GL_CONSTANT_ATTENUATION:
         if (params[0]<0.0) {
            gl_error( ctx, GL_INVALID_VALUE, "glLight" );
            return;
         }
         ctx->Light.Light[l].ConstantAttenuation = params[0];
         break;
      case GL_LINEAR_ATTENUATION:
         if (params[0]<0.0) {
            gl_error( ctx, GL_INVALID_VALUE, "glLight" );
            return;
         }
         ctx->Light.Light[l].LinearAttenuation = params[0];
         break;
      case GL_QUADRATIC_ATTENUATION:
         if (params[0]<0.0) {
            gl_error( ctx, GL_INVALID_VALUE, "glLight" );
            return;
         }
         ctx->Light.Light[l].QuadraticAttenuation = params[0];
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glLight" );
         break;
   }

   ctx->NewState |= NEW_LIGHTING;
}



void gl_GetLightfv( GLcontext *ctx,
                    GLenum light, GLenum pname, GLfloat *params )
{
   GLint l = (GLint) (light - GL_LIGHT0);

   if (l<0 || l>=MAX_LIGHTS) {
      gl_error( ctx, GL_INVALID_ENUM, "glGetLightfv" );
      return;
   }

   switch (pname) {
      case GL_AMBIENT:
         COPY_4V( params, ctx->Light.Light[l].Ambient );
         break;
      case GL_DIFFUSE:
         COPY_4V( params, ctx->Light.Light[l].Diffuse );
         break;
      case GL_SPECULAR:
         COPY_4V( params, ctx->Light.Light[l].Specular );
         break;
      case GL_POSITION:
         COPY_4V( params, ctx->Light.Light[l].Position );
         break;
      case GL_SPOT_DIRECTION:
         COPY_3V( params, ctx->Light.Light[l].Direction );
         break;
      case GL_SPOT_EXPONENT:
         params[0] = ctx->Light.Light[l].SpotExponent;
         break;
      case GL_SPOT_CUTOFF:
         params[0] = ctx->Light.Light[l].SpotCutoff;
         break;
      case GL_CONSTANT_ATTENUATION:
         params[0] = ctx->Light.Light[l].ConstantAttenuation;
         break;
      case GL_LINEAR_ATTENUATION:
         params[0] = ctx->Light.Light[l].LinearAttenuation;
         break;
      case GL_QUADRATIC_ATTENUATION:
         params[0] = ctx->Light.Light[l].QuadraticAttenuation;
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetLightfv" );
         break;
   }
}



void gl_GetLightiv( GLcontext *ctx, GLenum light, GLenum pname, GLint *params )
{
   GLint l = (GLint) (light - GL_LIGHT0);

   if (l<0 || l>=MAX_LIGHTS) {
      gl_error( ctx, GL_INVALID_ENUM, "glGetLightiv" );
      return;
   }

   switch (pname) {
      case GL_AMBIENT:
         params[0] = FLOAT_TO_INT(ctx->Light.Light[l].Ambient[0]);
         params[1] = FLOAT_TO_INT(ctx->Light.Light[l].Ambient[1]);
         params[2] = FLOAT_TO_INT(ctx->Light.Light[l].Ambient[2]);
         params[3] = FLOAT_TO_INT(ctx->Light.Light[l].Ambient[3]);
         break;
      case GL_DIFFUSE:
         params[0] = FLOAT_TO_INT(ctx->Light.Light[l].Diffuse[0]);
         params[1] = FLOAT_TO_INT(ctx->Light.Light[l].Diffuse[1]);
         params[2] = FLOAT_TO_INT(ctx->Light.Light[l].Diffuse[2]);
         params[3] = FLOAT_TO_INT(ctx->Light.Light[l].Diffuse[3]);
         break;
      case GL_SPECULAR:
         params[0] = FLOAT_TO_INT(ctx->Light.Light[l].Specular[0]);
         params[1] = FLOAT_TO_INT(ctx->Light.Light[l].Specular[1]);
         params[2] = FLOAT_TO_INT(ctx->Light.Light[l].Specular[2]);
         params[3] = FLOAT_TO_INT(ctx->Light.Light[l].Specular[3]);
         break;
      case GL_POSITION:
         params[0] = ctx->Light.Light[l].Position[0];
         params[1] = ctx->Light.Light[l].Position[1];
         params[2] = ctx->Light.Light[l].Position[2];
         params[3] = ctx->Light.Light[l].Position[3];
         break;
      case GL_SPOT_DIRECTION:
         params[0] = ctx->Light.Light[l].Direction[0];
         params[1] = ctx->Light.Light[l].Direction[1];
         params[2] = ctx->Light.Light[l].Direction[2];
         break;
      case GL_SPOT_EXPONENT:
         params[0] = ctx->Light.Light[l].SpotExponent;
         break;
      case GL_SPOT_CUTOFF:
         params[0] = ctx->Light.Light[l].SpotCutoff;
         break;
      case GL_CONSTANT_ATTENUATION:
         params[0] = ctx->Light.Light[l].ConstantAttenuation;
         break;
      case GL_LINEAR_ATTENUATION:
         params[0] = ctx->Light.Light[l].LinearAttenuation;
         break;
      case GL_QUADRATIC_ATTENUATION:
         params[0] = ctx->Light.Light[l].QuadraticAttenuation;
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetLightiv" );
         break;
   }
}



/**********************************************************************/
/***                        Light Model                             ***/
/**********************************************************************/


void gl_LightModelfv( GLcontext *ctx, GLenum pname, const GLfloat *params )
{
   switch (pname) {
      case GL_LIGHT_MODEL_AMBIENT:
         COPY_4V( ctx->Light.Model.Ambient, params );
         break;
      case GL_LIGHT_MODEL_LOCAL_VIEWER:
         if (params[0]==0.0)
            ctx->Light.Model.LocalViewer = GL_FALSE;
         else
            ctx->Light.Model.LocalViewer = GL_TRUE;
         break;
      case GL_LIGHT_MODEL_TWO_SIDE:
         if (params[0]==0.0)
            ctx->Light.Model.TwoSide = GL_FALSE;
         else
            ctx->Light.Model.TwoSide = GL_TRUE;
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glLightModel" );
         break;
   }
   ctx->NewState |= NEW_LIGHTING;
}




/********** MATERIAL **********/


/*
 * Given a face and pname value (ala glColorMaterial), compute a bitmask
 * of the targeted material values.
 */
GLuint gl_material_bitmask( GLenum face, GLenum pname )
{
   GLuint bitmask = 0;

   /* Make a bitmask indicating what material attribute(s) we're updating */
   switch (pname) {
      case GL_EMISSION:
         bitmask |= FRONT_EMISSION_BIT | BACK_EMISSION_BIT;
         break;
      case GL_AMBIENT:
         bitmask |= FRONT_AMBIENT_BIT | BACK_AMBIENT_BIT;
         break;
      case GL_DIFFUSE:
         bitmask |= FRONT_DIFFUSE_BIT | BACK_DIFFUSE_BIT;
         break;
      case GL_SPECULAR:
         bitmask |= FRONT_SPECULAR_BIT | BACK_SPECULAR_BIT;
         break;
      case GL_SHININESS:
         bitmask |= FRONT_SHININESS_BIT | BACK_SHININESS_BIT;
         break;
      case GL_AMBIENT_AND_DIFFUSE:
         bitmask |= FRONT_AMBIENT_BIT | BACK_AMBIENT_BIT;
         bitmask |= FRONT_DIFFUSE_BIT | BACK_DIFFUSE_BIT;
         break;
      case GL_COLOR_INDEXES:
         bitmask |= FRONT_INDEXES_BIT  | BACK_INDEXES_BIT;
         break;
      default:
         abort();
   }

   ASSERT( face==GL_FRONT || face==GL_BACK || face==GL_FRONT_AND_BACK );

   if (face==GL_FRONT) {
      bitmask &= FRONT_MATERIAL_BITS;
   }
   else if (face==GL_BACK) {
      bitmask &= BACK_MATERIAL_BITS;
   }

   return bitmask;
}



/*
 * This is called by glColor() when GL_COLOR_MATERIAL is enabled and
 * called by glMaterial() when GL_COLOR_MATERIAL is disabled.
 */
void gl_set_material( GLcontext *ctx, GLuint bitmask, const GLfloat *params )
{
   struct gl_material *mat;

   if (INSIDE_BEGIN_END(ctx)) {
      struct vertex_buffer *VB = ctx->VB;
      /* Save per-vertex material changes in the Vertex Buffer.
       * The update_material function will eventually update the global
       * ctx->Light.Material values.
       */
      mat = VB->Material[VB->Count];
      VB->MaterialMask[VB->Count] |= bitmask;
      VB->MaterialChanges = GL_TRUE;
   }
   else {
      /* just update the global material property */
      mat = ctx->Light.Material;
      ctx->NewState |= NEW_LIGHTING;
   }

   if (bitmask & FRONT_AMBIENT_BIT) {
      COPY_4V( mat[0].Ambient, params );
   }
   if (bitmask & BACK_AMBIENT_BIT) {
      COPY_4V( mat[1].Ambient, params );
   }
   if (bitmask & FRONT_DIFFUSE_BIT) {
      COPY_4V( mat[0].Diffuse, params );
   }
   if (bitmask & BACK_DIFFUSE_BIT) {
      COPY_4V( mat[1].Diffuse, params );
   }
   if (bitmask & FRONT_SPECULAR_BIT) {
      COPY_4V( mat[0].Specular, params );
   }
   if (bitmask & BACK_SPECULAR_BIT) {
      COPY_4V( mat[1].Specular, params );
   }
   if (bitmask & FRONT_EMISSION_BIT) {
      COPY_4V( mat[0].Emission, params );
   }
   if (bitmask & BACK_EMISSION_BIT) {
      COPY_4V( mat[1].Emission, params );
   }
   if (bitmask & FRONT_SHININESS_BIT) {
      mat[0].Shininess = CLAMP( params[0], 0.0, 128.0 );
      gl_compute_material_shine_table( &mat[0] );
   }
   if (bitmask & BACK_SHININESS_BIT) {
      mat[1].Shininess = CLAMP( params[0], 0.0, 128.0 );
      gl_compute_material_shine_table( &mat[1] );
   }
   if (bitmask & FRONT_INDEXES_BIT) {
      mat[0].AmbientIndex = params[0];
      mat[0].DiffuseIndex = params[1];
      mat[0].SpecularIndex = params[2];
   }
   if (bitmask & BACK_INDEXES_BIT) {
      mat[1].AmbientIndex = params[0];
      mat[1].DiffuseIndex = params[1];
      mat[1].SpecularIndex = params[2];
   }
}



void gl_ColorMaterial( GLcontext *ctx, GLenum face, GLenum mode )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glColorMaterial" );
      return;
   }
   switch (face) {
      case GL_FRONT:
      case GL_BACK:
      case GL_FRONT_AND_BACK:
         ctx->Light.ColorMaterialFace = face;
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glColorMaterial(face)" );
         return;
   }
   switch (mode) {
      case GL_EMISSION:
      case GL_AMBIENT:
      case GL_DIFFUSE:
      case GL_SPECULAR:
      case GL_AMBIENT_AND_DIFFUSE:
         ctx->Light.ColorMaterialMode = mode;
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glColorMaterial(mode)" );
         return;
   }

   ctx->Light.ColorMaterialBitmask = gl_material_bitmask( face, mode );
}



/*
 * This is only called via the api_function_table struct or by the
 * display list executor.
 */
void gl_Materialfv( GLcontext *ctx,
                    GLenum face, GLenum pname, const GLfloat *params )
{
   GLuint bitmask;

   /* error checking */
   if (face!=GL_FRONT && face!=GL_BACK && face!=GL_FRONT_AND_BACK) {
      gl_error( ctx, GL_INVALID_ENUM, "glMaterial(face)" );
      return;
   }
   switch (pname) {
      case GL_EMISSION:
      case GL_AMBIENT:
      case GL_DIFFUSE:
      case GL_SPECULAR:
      case GL_SHININESS:
      case GL_AMBIENT_AND_DIFFUSE:
      case GL_COLOR_INDEXES:
         /* OK */
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glMaterial(pname)" );
         return;
   }

   /* convert face and pname to a bitmask */
   bitmask = gl_material_bitmask( face, pname );

   if (ctx->Light.ColorMaterialEnabled) {
      /* The material values specified by glColorMaterial() can't be */
      /* updated by glMaterial() while GL_COLOR_MATERIAL is enabled! */
      bitmask &= ~ctx->Light.ColorMaterialBitmask;
   }

   gl_set_material( ctx, bitmask, params );
}




void gl_GetMaterialfv( GLcontext *ctx,
                       GLenum face, GLenum pname, GLfloat *params )
{
   GLuint f;

   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glGetMaterialfv" );
      return;
   }
   if (face==GL_FRONT) {
      f = 0;
   }
   else if (face==GL_BACK) {
      f = 1;
   }
   else {
      gl_error( ctx, GL_INVALID_ENUM, "glGetMaterialfv(face)" );
      return;
   }
   switch (pname) {
      case GL_AMBIENT:
         COPY_4V( params, ctx->Light.Material[f].Ambient );
         break;
      case GL_DIFFUSE:
         COPY_4V( params, ctx->Light.Material[f].Diffuse );
	 break;
      case GL_SPECULAR:
         COPY_4V( params, ctx->Light.Material[f].Specular );
	 break;
      case GL_EMISSION:
	 COPY_4V( params, ctx->Light.Material[f].Emission );
	 break;
      case GL_SHININESS:
	 *params = ctx->Light.Material[f].Shininess;
	 break;
      case GL_COLOR_INDEXES:
	 params[0] = ctx->Light.Material[f].AmbientIndex;
	 params[1] = ctx->Light.Material[f].DiffuseIndex;
	 params[2] = ctx->Light.Material[f].SpecularIndex;
	 break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetMaterialfv(pname)" );
   }
}



void gl_GetMaterialiv( GLcontext *ctx,
                       GLenum face, GLenum pname, GLint *params )
{
   GLuint f;

   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glGetMaterialiv" );
      return;
   }
   if (face==GL_FRONT) {
      f = 0;
   }
   else if (face==GL_BACK) {
      f = 1;
   }
   else {
      gl_error( ctx, GL_INVALID_ENUM, "glGetMaterialiv(face)" );
      return;
   }
   switch (pname) {
      case GL_AMBIENT:
         params[0] = FLOAT_TO_INT( ctx->Light.Material[f].Ambient[0] );
         params[1] = FLOAT_TO_INT( ctx->Light.Material[f].Ambient[1] );
         params[2] = FLOAT_TO_INT( ctx->Light.Material[f].Ambient[2] );
         params[3] = FLOAT_TO_INT( ctx->Light.Material[f].Ambient[3] );
         break;
      case GL_DIFFUSE:
         params[0] = FLOAT_TO_INT( ctx->Light.Material[f].Diffuse[0] );
         params[1] = FLOAT_TO_INT( ctx->Light.Material[f].Diffuse[1] );
         params[2] = FLOAT_TO_INT( ctx->Light.Material[f].Diffuse[2] );
         params[3] = FLOAT_TO_INT( ctx->Light.Material[f].Diffuse[3] );
	 break;
      case GL_SPECULAR:
         params[0] = FLOAT_TO_INT( ctx->Light.Material[f].Specular[0] );
         params[1] = FLOAT_TO_INT( ctx->Light.Material[f].Specular[1] );
         params[2] = FLOAT_TO_INT( ctx->Light.Material[f].Specular[2] );
         params[3] = FLOAT_TO_INT( ctx->Light.Material[f].Specular[3] );
	 break;
      case GL_EMISSION:
         params[0] = FLOAT_TO_INT( ctx->Light.Material[f].Emission[0] );
         params[1] = FLOAT_TO_INT( ctx->Light.Material[f].Emission[1] );
         params[2] = FLOAT_TO_INT( ctx->Light.Material[f].Emission[2] );
         params[3] = FLOAT_TO_INT( ctx->Light.Material[f].Emission[3] );
	 break;
      case GL_SHININESS:
         *params = ROUNDF( ctx->Light.Material[f].Shininess );
	 break;
      case GL_COLOR_INDEXES:
	 params[0] = ROUNDF( ctx->Light.Material[f].AmbientIndex );
	 params[1] = ROUNDF( ctx->Light.Material[f].DiffuseIndex );
	 params[2] = ROUNDF( ctx->Light.Material[f].SpecularIndex );
	 break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetMaterialfv(pname)" );
   }
}




/**********************************************************************/
/*****                  Lighting computation                      *****/
/**********************************************************************/


/*
 * Notes:
 *   When two-sided lighting is enabled we compute the color (or index)
 *   for both the front and back side of the primitive.  Then, when the
 *   orientation of the facet is later learned, we can determine which
 *   color (or index) to use for rendering.
 *
 * Variables:
 *   n = normal vector
 *   V = vertex position
 *   P = light source position
 *   Pe = (0,0,0,1)
 *
 * Precomputed:
 *   IF P[3]==0 THEN
 *       // light at infinity
 *       IF local_viewer THEN
 *           VP_inf_norm = unit vector from V to P      // Precompute
 *       ELSE 
 *           // eye at infinity
 *           h_inf_norm = Normalize( VP + <0,0,1> )     // Precompute
 *       ENDIF
 *   ENDIF
 *
 * Functions:
 *   Normalize( v ) = normalized vector v
 *   Magnitude( v ) = length of vector v
 */



/*
 * Whenever the spotlight exponent for a light changes we must call
 * this function to recompute the exponent lookup table.
 */
void gl_compute_spot_exp_table( struct gl_light *l )
{
   int i;
   double exponent = l->SpotExponent;
   double tmp;
   int clamp = 0;

   l->SpotExpTable[0][0] = 0.0;

   for (i=EXP_TABLE_SIZE-1;i>0;i--) {
      if (clamp == 0) {
         tmp = pow(i/(double)(EXP_TABLE_SIZE-1), exponent);
         if (tmp < FLT_MIN*100.0) {
            tmp = 0.0;
            clamp = 1;
         }
      }
      l->SpotExpTable[i][0] = tmp;
   }
   for (i=0;i<EXP_TABLE_SIZE-1;i++) {
      l->SpotExpTable[i][1] = l->SpotExpTable[i+1][0] - l->SpotExpTable[i][0];
   }
   l->SpotExpTable[EXP_TABLE_SIZE-1][1] = 0.0;
}



/*
 * Whenever the shininess of a material changes we must call this
 * function to recompute the exponential lookup table.
 */
void gl_compute_material_shine_table( struct gl_material *m )
{
   int i;
   double exponent = m->Shininess;

   m->ShineTable[0] = 0.0;
   for (i=1;i<SHINE_TABLE_SIZE;i++) {
      double x = pow( i/(double)(SHINE_TABLE_SIZE-1), exponent );
      if (x<1.0e-10) {
         m->ShineTable[i] = 0.0;
      }
      else {
         m->ShineTable[i] = x;
      }
   }
}



/*
 * Examine current lighting parameters to determine if the optimized lighting
 * function can be used.  Also, precompute some lighting values which are
 * used by gl_color_shade_vertices_fast().
 */
void gl_update_lighting( GLcontext *ctx )
{
   GLint i;
   struct gl_light *prev_enabled, *light;

   if (!ctx->Light.Enabled) {
      /* If lighting is not enabled, we can skip all this. */
      return;
   }

   /* base color = material_emission + global_ambient */
   ctx->Light.BaseColor[0] = ctx->Light.Material[0].Emission[0]
             + ctx->Light.Model.Ambient[0] * ctx->Light.Material[0].Ambient[0];
   ctx->Light.BaseColor[1] = ctx->Light.Material[0].Emission[1]
             + ctx->Light.Model.Ambient[1] * ctx->Light.Material[0].Ambient[1];
   ctx->Light.BaseColor[2] = ctx->Light.Material[0].Emission[2]
             + ctx->Light.Model.Ambient[2] * ctx->Light.Material[0].Ambient[2];
   ctx->Light.BaseColor[3] = MIN2( ctx->Light.Material[0].Diffuse[3], 1.0F );

   /* Setup linked list of enabled light sources */
   prev_enabled = NULL;
   ctx->Light.FirstEnabled = NULL;
   for (i=0;i<MAX_LIGHTS;i++) {
      ctx->Light.Light[i].NextEnabled = NULL;
      if (ctx->Light.Light[i].Enabled) {
         if (prev_enabled) {
            prev_enabled->NextEnabled = &ctx->Light.Light[i];
         }
         else {
            ctx->Light.FirstEnabled = &ctx->Light.Light[i];
         }
         prev_enabled = &ctx->Light.Light[i];
      }
   }

   /* Precompute some lighting stuff */
   for (light = ctx->Light.FirstEnabled; light; light = light->NextEnabled) {
      struct gl_material *mat = &ctx->Light.Material[0];
      /* Add each light's ambient component to base color */
      ctx->Light.BaseColor[0] += light->Ambient[0] * mat->Ambient[0];
      ctx->Light.BaseColor[1] += light->Ambient[1] * mat->Ambient[1];
      ctx->Light.BaseColor[2] += light->Ambient[2] * mat->Ambient[2];
      /* compute product of light's ambient with front material ambient */
      light->MatAmbient[0] = light->Ambient[0] * mat->Ambient[0];
      light->MatAmbient[1] = light->Ambient[1] * mat->Ambient[1];
      light->MatAmbient[2] = light->Ambient[2] * mat->Ambient[2];
      /* compute product of light's diffuse with front material diffuse */
      light->MatDiffuse[0] = light->Diffuse[0] * mat->Diffuse[0];
      light->MatDiffuse[1] = light->Diffuse[1] * mat->Diffuse[1];
      light->MatDiffuse[2] = light->Diffuse[2] * mat->Diffuse[2];
      /* compute product of light's specular with front material specular */
      light->MatSpecular[0] = light->Specular[0] * mat->Specular[0];
      light->MatSpecular[1] = light->Specular[1] * mat->Specular[1];
      light->MatSpecular[2] = light->Specular[2] * mat->Specular[2];

      /* VP (VP) = Normalize( Position ) */
      COPY_3V( light->VP_inf_norm, light->Position );
      NORMALIZE_3V( light->VP_inf_norm );

      /* h_inf_norm = Normalize( V_to_P + <0,0,1> ) */
      COPY_3V( light->h_inf_norm, light->VP_inf_norm );
      light->h_inf_norm[2] += 1.0F;
      NORMALIZE_3V( light->h_inf_norm );

      COPY_3V( light->NormDirection, light->Direction );
      NORMALIZE_3V( light->NormDirection );

      /* Compute color index diffuse and specular light intensities */
      light->dli = 0.30F * light->Diffuse[0]
                 + 0.59F * light->Diffuse[1]
                 + 0.11F * light->Diffuse[2];
      light->sli = 0.30F * light->Specular[0]
                 + 0.59F * light->Specular[1]
                 + 0.11F * light->Specular[2];

   }

   /* Determine if the fast lighting function can be used */
   ctx->Light.Fast = GL_TRUE;
   if (    ctx->Light.BaseColor[0]<0.0F
        || ctx->Light.BaseColor[1]<0.0F
        || ctx->Light.BaseColor[2]<0.0F
        || ctx->Light.BaseColor[3]<0.0F
        || ctx->Light.Model.TwoSide
        || ctx->Light.Model.LocalViewer
        || ctx->Light.ColorMaterialEnabled) {
      ctx->Light.Fast = GL_FALSE;
   }
   else {
      for (light=ctx->Light.FirstEnabled; light; light=light->NextEnabled) {
         if (   light->Position[3]!=0.0F
             || light->SpotCutoff!=180.0F
             || light->MatDiffuse[0]<0.0F
             || light->MatDiffuse[1]<0.0F
             || light->MatDiffuse[2]<0.0F
             || light->MatSpecular[0]<0.0F
             || light->MatSpecular[1]<0.0F
             || light->MatSpecular[2]<0.0F) {
            ctx->Light.Fast = GL_FALSE;
            break;
         }
      }
   }
}





/*
 * Use current lighting/material settings to compute the RGBA colors of
 * an array of vertexes.
 * Input:  n - number of vertexes to process
 *         vertex - array of vertex positions in eye coordinates
 *         normal - array of surface normal vectors
 *         twoside - 0 = front face shading only, 1 = two-sided lighting
 * Output:  frontcolor - array of resulting front-face colors
 *          backcolor - array of resulting back-face colors
 */
void gl_color_shade_vertices( GLcontext *ctx,
                              GLuint n,
                              GLfloat vertex[][4],
                              GLfloat normal[][3],
                              GLuint twoside,
                              GLfixed frontcolor[][4],
                              GLfixed backcolor[][4] )
{
   GLint side, j;
   GLfloat rscale, gscale, bscale, ascale;

   /* Compute scale factor to go from floats in [0,1] to integers or fixed
    * point values:
    */
   rscale = (GLfloat) ( (GLint) ctx->Visual->RedScale   << ctx->ColorShift );
   gscale = (GLfloat) ( (GLint) ctx->Visual->GreenScale << ctx->ColorShift );
   bscale = (GLfloat) ( (GLint) ctx->Visual->BlueScale  << ctx->ColorShift );
   ascale = (GLfloat) ( (GLint) ctx->Visual->AlphaScale << ctx->ColorShift );


   for (side=0;side<=twoside;side++) {
      GLfloat baseR, baseG, baseB, baseA;
      GLfixed sumA;
      struct gl_light *light;
      struct gl_material *mat;

      mat = &ctx->Light.Material[side];

      /*** Compute color contribution from global lighting ***/
      baseR = mat->Emission[0] + ctx->Light.Model.Ambient[0] * mat->Ambient[0];
      baseG = mat->Emission[1] + ctx->Light.Model.Ambient[1] * mat->Ambient[1];
      baseB = mat->Emission[2] + ctx->Light.Model.Ambient[2] * mat->Ambient[2];

      /* Alpha is simple, same for all vertices */
      baseA = mat->Diffuse[3];
      sumA = (GLfixed) (CLAMP( baseA, 0.0F, 1.0F ) * ascale);

      for (j=0;j<n;j++) {
         GLfloat sumR, sumG, sumB;
         GLfloat nx, ny, nz;

         if (side==0) {
            /* shade frontside */
            nx = normal[j][0];
            ny = normal[j][1];
            nz = normal[j][2];
         }
         else {
            /* shade backside */
            nx = -normal[j][0];
            ny = -normal[j][1];
            nz = -normal[j][2];
         }

         sumR = baseR;
         sumG = baseG;
         sumB = baseB;

         /* Add contribution from each enabled light source */
         for (light=ctx->Light.FirstEnabled; light; light=light->NextEnabled) {
            GLfloat ambientR, ambientG, ambientB;
            GLfloat attenuation, spot;
            GLfloat VPx, VPy, VPz;  /* unit vector from vertex to light */
            GLfloat n_dot_VP;       /* n dot VP */

            /* compute VP and attenuation */
            if (light->Position[3]==0.0) {
               /* directional light */
               VPx = light->VP_inf_norm[0];
               VPy = light->VP_inf_norm[1];
               VPz = light->VP_inf_norm[2];
               attenuation = 1.0F;
            }
            else {
               /* positional light */
               GLfloat d;     /* distance from vertex to light */
               VPx = light->Position[0] - vertex[j][0];
               VPy = light->Position[1] - vertex[j][1];
               VPz = light->Position[2] - vertex[j][2];
               d = (GLfloat) sqrt( VPx*VPx + VPy*VPy + VPz*VPz );
               if (d>0.001F) {
                  GLfloat invd = 1.0F / d;
                  VPx *= invd;
                  VPy *= invd;
                  VPz *= invd;
               }
               attenuation = 1.0F / (light->ConstantAttenuation
                           + d * (light->LinearAttenuation
                           + d * light->QuadraticAttenuation));
            }

            /* spotlight factor */
            if (light->SpotCutoff==180.0F) {
               /* not a spot light */
               spot = 1.0F;
            }
            else {
               GLfloat PVx, PVy, PVz, PV_dot_dir;
               PVx = -VPx;
               PVy = -VPy;
               PVz = -VPz;
               PV_dot_dir = PVx*light->NormDirection[0]
                          + PVy*light->NormDirection[1]
                          + PVz*light->NormDirection[2];
               if (PV_dot_dir<=0.0F || PV_dot_dir<light->CosCutoff) {
                  /* outside of cone */
                  spot = 0.0F;
               }
               else {
                  double x = PV_dot_dir * (EXP_TABLE_SIZE-1);
                  int k = (int) x;
                  spot = light->SpotExpTable[k][0]
                       + (x-k)*light->SpotExpTable[k][1];
               }
            }

            ambientR = light->Ambient[0] * mat->Ambient[0];
            ambientG = light->Ambient[1] * mat->Ambient[1];
            ambientB = light->Ambient[2] * mat->Ambient[2];

            /* Compute dot product or normal and vector from V to light pos */
            n_dot_VP = nx * VPx + ny * VPy + nz * VPz;

            /* diffuse and specular terms */
            if (n_dot_VP<=0.0F) {
               /* surface face away from light, no diffuse or specular */
               GLfloat t = attenuation * spot;
               sumR += t * ambientR;
               sumG += t * ambientG;
               sumB += t * ambientB;
               /* done with this light */
            }
            else {
               GLfloat diffuseR, diffuseG, diffuseB;
               GLfloat specularR, specularG, specularB;
               GLfloat hx, hy, hz, n_dot_h, t;
                  
               /* diffuse term */
               diffuseR = n_dot_VP * light->Diffuse[0] * mat->Diffuse[0];
               diffuseG = n_dot_VP * light->Diffuse[1] * mat->Diffuse[1];
               diffuseB = n_dot_VP * light->Diffuse[2] * mat->Diffuse[2];

               /* specular term */
               if (ctx->Light.Model.LocalViewer) {
                  GLfloat vx, vy, vz, vlen;
                  vx = vertex[j][0];
                  vy = vertex[j][1];
                  vz = vertex[j][2];
                  vlen = sqrt( vx*vx + vy*vy + vz*vz );
                  if (vlen>0.0001F) {
                     GLfloat invlen = 1.0F / vlen;
                     vx *= invlen;
                     vy *= invlen;
                     vz *= invlen;
                  }
                  /* h = VP + VPe */
                  hx = VPx - vx;
                  hy = VPy - vy;
                  hz = VPz - vz;
               }
               else {
                  /* h = VP + <0,0,1> */
                  hx = VPx;
                  hy = VPy;
                  hz = VPz + 1.0F;
               }

               /* attention: h is not normalized, done later if needed */
               n_dot_h = nx*hx + ny*hy + nz*hz;

               if (n_dot_h<=0.0F) {
                  specularR = 0.0F;
                  specularG = 0.0F;
                  specularB = 0.0F;
               }
               else {
                  GLfloat spec_coef;
                  /* now `correct' the dot product */
                  n_dot_h = n_dot_h / sqrt( hx*hx + hy*hy + hz*hz );
                  if (n_dot_h>1.0F) {
                     /* only happens if normal vector length > 1.0 */
                     spec_coef = pow( n_dot_h, mat->Shininess );
                  }
                  else {
                     /* use table lookup approximation */
                     int k = (int) (n_dot_h * (GLfloat) (SHINE_TABLE_SIZE-1));
                     spec_coef = mat->ShineTable[k];
                  }
                  if (spec_coef<1.0e-10) {
                     specularR = 0.0F;
                     specularG = 0.0F;
                     specularB = 0.0F;
                  }
                  else {
                     specularR = spec_coef * light->Specular[0]
                                 * mat->Specular[0];
                     specularG = spec_coef * light->Specular[1]
                                 * mat->Specular[1];
                     specularB = spec_coef * light->Specular[2]
                                 * mat->Specular[2];
                  }
               }

               t = attenuation * spot;
               sumR += t * (ambientR + diffuseR + specularR);
               sumG += t * (ambientG + diffuseG + specularG);
               sumB += t * (ambientB + diffuseB + specularB);
            }

         } /*loop over lights*/

         if (side==0) {
            /* clamp and convert to integer or fixed point */
            frontcolor[j][0] = (GLfixed) (CLAMP( sumR, 0.0F, 1.0F ) * rscale);
            frontcolor[j][1] = (GLfixed) (CLAMP( sumG, 0.0F, 1.0F ) * gscale);
            frontcolor[j][2] = (GLfixed) (CLAMP( sumB, 0.0F, 1.0F ) * bscale);
            frontcolor[j][3] = sumA;
         }
         else {
            /* clamp and convert to integer or fixed point */
            backcolor[j][0] = (GLfixed) (CLAMP( sumR, 0.0F, 1.0F ) * rscale);
            backcolor[j][1] = (GLfixed) (CLAMP( sumG, 0.0F, 1.0F ) * gscale);
            backcolor[j][2] = (GLfixed) (CLAMP( sumB, 0.0F, 1.0F ) * bscale);
            backcolor[j][3] = sumA;
         }
      } /*loop over vertices*/

   } /*for side*/
}



/*
 * This is an optimized version of the above function.
 */
void gl_color_shade_vertices_fast( GLcontext *ctx,
                                   GLuint n,
                                   GLfloat vertex[][4],
                                   GLfloat normal[][3],
                                   GLuint twoside,
                                   GLfixed frontcolor[][4],
                                   GLfixed backcolor[][4] )
{
   GLint j;
   GLfloat rscale, gscale, bscale, ascale;
   GLfixed A;

   /* Compute scale factor to go from floats in [0,1] to integers or fixed
    * point values:
    */
   rscale = (GLfloat) ( (GLint) ctx->Visual->RedScale   << ctx->ColorShift );
   gscale = (GLfloat) ( (GLint) ctx->Visual->GreenScale << ctx->ColorShift );
   bscale = (GLfloat) ( (GLint) ctx->Visual->BlueScale  << ctx->ColorShift );
   ascale = (GLfloat) ( (GLint) ctx->Visual->AlphaScale << ctx->ColorShift );

   /* Alpha is easy to compute, same for all vertices */
   A = (GLfixed) ( ctx->Light.BaseColor[3] * ascale);

   /* Loop over vertices */
   for (j=0;j<n;j++) {
      GLfloat R, G, B;
      GLfloat nx, ny, nz;
      struct gl_light *light;

      /* the normal vector */
      nx = normal[j][0];
      ny = normal[j][1];
      nz = normal[j][2];

      /* base color from global illumination and enabled light's ambient */
      R = ctx->Light.BaseColor[0];
      G = ctx->Light.BaseColor[1];
      B = ctx->Light.BaseColor[2];

      /* Add contribution from each light source */
      for (light=ctx->Light.FirstEnabled; light; light=light->NextEnabled) {
         GLfloat n_dot_VP;     /* n dot VP */

         n_dot_VP = nx * light->VP_inf_norm[0]
                  + ny * light->VP_inf_norm[1]
                  + nz * light->VP_inf_norm[2];

         /* diffuse and specular terms */
         if (n_dot_VP>0.0F) {
            GLfloat n_dot_h;

            /** add diffuse term **/
            R += n_dot_VP * light->MatDiffuse[0];
            G += n_dot_VP * light->MatDiffuse[1];
            B += n_dot_VP * light->MatDiffuse[2];

            /** specular term **/
            /* dot product of n and h_inf_norm */
            n_dot_h = nx * light->h_inf_norm[0]
                    + ny * light->h_inf_norm[1]
                    + nz * light->h_inf_norm[2];
            if (n_dot_h>0.0F) {
               if (n_dot_h>1.0F) {
                  /* only happens if Magnitude(n) > 1.0 */
                  GLfloat spec_coef = pow( n_dot_h,
                                           ctx->Light.Material[0].Shininess );
                  if (spec_coef>1.0e-10F) {
                     R += spec_coef * light->MatSpecular[0];
                     G += spec_coef * light->MatSpecular[1];
                     B += spec_coef * light->MatSpecular[2];
                  }
               }
               else {
                  /* use table lookup approximation */
                  int k = (int) (n_dot_h * (GLfloat) (SHINE_TABLE_SIZE-1));
                  GLfloat spec_coef = ctx->Light.Material[0].ShineTable[k];
                  R += spec_coef * light->MatSpecular[0];
                  G += spec_coef * light->MatSpecular[1];
                  B += spec_coef * light->MatSpecular[2];
               }
            }
         }

      } /*loop over lights*/

      /* clamp and convert to integer or fixed point */
      frontcolor[j][0] = (GLfixed) (MIN2( R, 1.0F ) * rscale);
      frontcolor[j][1] = (GLfixed) (MIN2( G, 1.0F ) * gscale);
      frontcolor[j][2] = (GLfixed) (MIN2( B, 1.0F ) * bscale);
      frontcolor[j][3] = A;

   } /*loop over vertices*/
}



/*
 * Use current lighting/material settings to compute the color indexes
 * for an array of vertices.
 * Input:  n - number of vertices to shade
 *         vertex - array of [n] vertex position in viewing coordinates
 *         normal - array of [n] surface normal vector
 *         twoside - 0 = front face shading only, 1 = two-sided lighting
 * Output:  frontindex - resulting array of [n] front-face color indexes
 *          backindex - resulting array of [n] back-face color indexes
 */
void gl_index_shade_vertices( GLcontext *ctx,
                              GLuint n,
                              GLfloat vertex[][4],
                              GLfloat normal[][3],
                              GLuint twoside,
                              GLuint frontindex[],
                              GLuint backindex[] )
{
   GLint side, j;
   GLuint *output_index;

   for (side=0;side<=twoside;side++) {
      struct gl_material *mat = &ctx->Light.Material[side];

      if (side==0) {
         output_index = frontindex;
      }
      else {
         output_index = backindex;
      }

      /* loop over vertices */
      for (j=0;j<n;j++) {
         GLfloat index;
         GLfloat diffuse, specular;  /* accumulated diffuse and specular */
         GLfloat nx, ny, nz;  /* normal vector */
         struct gl_light *light;

         if (side==0) {
            /* shade frontside */
            nx = normal[j][0];
            ny = normal[j][1];
            nz = normal[j][2];
         }
         else {
            /* shade backside */
            nx = -normal[j][0];
            ny = -normal[j][1];
            nz = -normal[j][2];
         }

         diffuse = specular = 0.0F;

         /* Accumulate diffuse and specular from each light source */
         for (light=ctx->Light.FirstEnabled; light; light=light->NextEnabled) {
            GLfloat attenuation;
            GLfloat lx, ly, lz;  /* unit vector from vertex to light */
            GLfloat l_dot_norm;  /* dot product of l and n */

            /* compute l and attenuation */
            if (light->Position[3]==0.0) {
               /* directional light */
               /* Effectively, l is a vector from the origin to the light. */
               lx = light->VP_inf_norm[0];
               ly = light->VP_inf_norm[1];
               lz = light->VP_inf_norm[2];
               attenuation = 1.0F;
            }
            else {
               /* positional light */
               GLfloat d;     /* distance from vertex to light */
               lx = light->Position[0] - vertex[j][0];
               ly = light->Position[1] - vertex[j][1];
               lz = light->Position[2] - vertex[j][2];
               d = (GLfloat) sqrt( lx*lx + ly*ly + lz*lz );
               if (d>0.001F) {
                  GLfloat invd = 1.0F / d;
                  lx *= invd;
                  ly *= invd;
                  lz *= invd;
               }
               attenuation = 1.0F / (light->ConstantAttenuation
                             + d * (light->LinearAttenuation
                             + d * light->QuadraticAttenuation));
            }

            l_dot_norm = lx*nx + ly*ny + lz*nz;

            if (l_dot_norm>0.0F) {
               GLfloat spot_times_atten;

               /* spotlight factor */
               if (light->SpotCutoff==180.0F) {
                  /* not a spot light */
                  spot_times_atten = attenuation;
               }
               else {
                  GLfloat v[3], dot;
                  v[0] = -lx;  /* v points from light to vertex */
                  v[1] = -ly;
                  v[2] = -lz;
                  dot = DOT3( v, light->NormDirection );
                  if (dot<=0.0F || dot<light->CosCutoff) {
                     /* outside of cone */
                     spot_times_atten = 0.0F;
                  }
                  else {
                     double x = dot * (EXP_TABLE_SIZE-1);
                     int k = (int) x;
                     GLfloat spot = light->SpotExpTable[k][0]
                                  + (x-k)*light->SpotExpTable[k][1];
                     spot_times_atten = spot * attenuation;
                  }
               }

               /* accumulate diffuse term */
               diffuse += l_dot_norm * light->dli * spot_times_atten;

               /* accumulate specular term */
               {
                  GLfloat hx, hy, hz, dot, spec_coef;

                  /* specular term */
                  if (ctx->Light.Model.LocalViewer) {
                     GLfloat vx, vy, vz, vlen;
                     vx = vertex[j][0];
                     vy = vertex[j][1];
                     vz = vertex[j][2];
                     vlen = sqrt( vx*vx + vy*vy + vz*vz );
                     if (vlen>0.0001F) {
                        GLfloat invlen = 1.0F / vlen;
                        vx *= invlen;
                        vy *= invlen;
                        vz *= invlen;
                     }
                     hx = lx - vx;
                     hy = ly - vy;
                     hz = lz - vz;
                  }
                  else {
                     hx = lx;
                     hy = ly;
                     hz = lz + 1.0F;
                  }
                  /* attention: s is not normalized, done later if necessary */
                  dot = hx*nx + hy*ny + hz*nz;

                  if (dot<=0.0F) {
                     spec_coef = 0.0F;
                  }
                  else {
                     /* now `correct' the dot product */
                     dot = dot / sqrt(hx*hx + hy*hy + hz*hz);
                     if (dot>1.0F) {
                        spec_coef = pow( dot, mat->Shininess );
                     }
                     else {
                        int k = (int) (dot * (GLfloat)(SHINE_TABLE_SIZE-1));
                        spec_coef = mat->ShineTable[k];
                     }
                  }
                  specular += spec_coef * light->sli * spot_times_atten;
               }
            }

         } /*loop over lights*/

         /* Now compute final color index */
         if (specular>1.0F) {
            index = mat->SpecularIndex;
         }
         else {
            GLfloat d_a, s_a;
            d_a = mat->DiffuseIndex - mat->AmbientIndex;
            s_a = mat->SpecularIndex - mat->AmbientIndex;

            index = mat->AmbientIndex
                  + diffuse * (1.0F-specular) * d_a
                  + specular * s_a;
            if (index>mat->SpecularIndex) {
               index = mat->SpecularIndex;
            }
         }
         output_index[j] = (GLuint) (GLint) index;

      } /*for vertex*/

   } /*for side*/
}
