/* $Id: texobj.c,v 1.5 1997/02/09 18:52:15 brianp Exp $ */

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
 * $Log: texobj.c,v $
 * Revision 1.5  1997/02/09 18:52:15  brianp
 * added GL_EXT_texture3D support
 *
 * Revision 1.4  1997/01/16 03:35:34  brianp
 * added calls to device driver DeleteTexture() and BindTexture() functions
 *
 * Revision 1.3  1997/01/09 19:49:47  brianp
 * added a check to switch rasterizers if needed in glBindTexture()
 *
 * Revision 1.2  1996/09/27 17:09:42  brianp
 * removed a redundant return statement
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#include <assert.h>
#include <stdlib.h>
#include "context.h"
#include "macros.h"
#include "teximage.h"
#include "texobj.h"
#include "types.h"



/*
 * Allocate a new texture object structure.  The name and dimensionality are
 * set to zero here and must be initialized by the caller.
 */
struct gl_texture_object *gl_alloc_texture_object( void )
{
   struct gl_texture_object *obj;

   obj = (struct gl_texture_object *)
                     calloc(1,sizeof(struct gl_texture_object));
   if (obj) {
      /* init the non-zero fields */
      obj->WrapS = GL_REPEAT;
      obj->WrapT = GL_REPEAT;
      obj->MinFilter = GL_NEAREST_MIPMAP_LINEAR;
      obj->MagFilter = GL_LINEAR;
   }
   return obj;
}


/*
 * Append a gl_texture_object struct to a list of texture objects.
 */
static void append_texture_object( struct gl_texture_object *list,
                                   struct gl_texture_object *obj )
{
   struct gl_texture_object *t;

   t = list;
   while (t->Next) {
      t = t->Next;
   }
   t->Next = obj;
}



/*
 * Deallocate a texture object struct and all children structures
 * and image data.
 */
void gl_free_texture_object( struct gl_texture_object *t )
{
   GLuint i;

   for (i=0;i<MAX_TEXTURE_LEVELS;i++) {
      if (t->Image[i]) {
         gl_free_texture_image( t->Image[i] );
      }
   }
   free( t );
}


/*
 * Given a texture object name, return a pointer to the texture object.
 */
static struct gl_texture_object *
find_texture_object( GLcontext *ctx, GLuint name )
{
   struct gl_texture_object *t;

   assert( name>0 );
   t = ctx->Shared->TexObjectList;
   while (t) {
      if (t->Name == name) {
         return t;
      }
      t = t->Next;
   }
   return NULL;
}




void gl_GenTextures( GLcontext *ctx, GLsizei n, GLuint *textures )
{
   struct gl_texture_object *t;
   GLuint i, max;

   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glGenTextures" );
      return;
   }
   if (n<0) {
      gl_error( ctx, GL_INVALID_VALUE, "glGenTextures" );
      return;
   }

   /* Find maximum texture object name in use */
   t = ctx->Shared->TexObjectList;
   max = 0;
   while (t) {
      if (t->Name>max) {
         max = t->Name;
      }
      t = t->Next;
   }

   /* Return new texture names starting at max+1 */
   for (i=0;i<n;i++) {
      max++;
      textures[i] = max;
   }
}



void gl_DeleteTextures( GLcontext *ctx, GLsizei n, const GLuint *textures)
{
   GLuint i;

   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glAreTexturesResident" );
      return;
   }

   for (i=0;i<n;i++) {
      struct gl_texture_object *t, *tprev, *tcurr;
      if (textures[i]>0) {
         t = find_texture_object( ctx, textures[i] );
         if (t) {
            if (ctx->Texture.Current1D==t) {
               /* revert to default 1-D texture */
               ctx->Texture.Current1D = ctx->Shared->TexObjectList;
               t->RefCount--;
               assert( t->RefCount >= 0 );
            }
            else if (ctx->Texture.Current2D==t) {
               /* revert to default 2-D texture */
               ctx->Texture.Current2D = ctx->Shared->TexObjectList->Next;
               t->RefCount--;
               assert( t->RefCount >= 0 );
            }
            else if (ctx->Texture.Current3D==t) {
               /* revert to default 3-D texture */
               ctx->Texture.Current3D = ctx->Shared->TexObjectList->Next;
               t->RefCount--;
               assert( t->RefCount >= 0 );
            }
            if (t->RefCount==0) {
               /* remove texture object t from the linked list */
               tprev = NULL;
               tcurr = ctx->Shared->TexObjectList;
               while (tcurr) {
                  if (tcurr==t) {
                     assert( tprev );
                     tprev->Next = t->Next;
                     gl_free_texture_object( t );
                     break;
                  }
                  tprev = tcurr;
                  tcurr = tcurr->Next;
               }
            }

            /* tell device driver to delete texture */
            if (ctx->Driver.DeleteTexture) {
               (*ctx->Driver.DeleteTexture)( ctx, textures[i] );
            }
         }
      }
   }
}



void gl_BindTexture( GLcontext *ctx, GLenum target, GLuint texture )
{
   struct gl_texture_object *oldtexobj;
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glAreTexturesResident" );
      return;
   }
   switch (target) {
      case GL_TEXTURE_1D:
         oldtexobj = ctx->Texture.Current1D;
         if (texture==0) {
            /* use default 1-D texture */
            ctx->Texture.Current1D = ctx->Shared->TexObjectList;
         }
         else {
            struct gl_texture_object *t;
            t = find_texture_object( ctx, texture );
            if (t) {
               if (t->Dimensions==1) {
                  /* success! */
                  ctx->Texture.Current1D = t;
               }
               else {
                  /* wrong dimensionality */
                  gl_error( ctx, GL_INVALID_OPERATION, "glBindTextureEXT" );
                  return;
               }
            }
            else {
               /* create new texture object */
               t = gl_alloc_texture_object();
               append_texture_object( ctx->Shared->TexObjectList, t );
               t->Name = texture;
               t->Dimensions = 1;
               ctx->Texture.Current1D = t;
            }
         }
         /* Tidy up reference counting */
         if (ctx->Texture.Current1D != oldtexobj && oldtexobj->Name>0) {
            /* decr reference count of the prev texture object */
            oldtexobj->RefCount--;
            assert( oldtexobj->RefCount >= 0 );
         }
         if (ctx->Texture.Current1D->Name>0) {
            ctx->Texture.Current1D->RefCount++;
         }
#ifdef FOO
         if (!ctx->Texture.Current1D->Complete) {
            /* re-examine texture completeness */
            gl_update_texture_state();
         }
#endif

         /* Check if we may have to use a new triangle rasterizer */
         if (oldtexobj->WrapS != ctx->Texture.Current1D->WrapS
             || oldtexobj->MinFilter != ctx->Texture.Current1D->MinFilter
             || oldtexobj->MagFilter != ctx->Texture.Current1D->MagFilter) {
            ctx->NewState |= NEW_RASTER_OPS;
         }

         /* The current 1D texture object can never be NULL! */
         assert(ctx->Texture.Current1D);
         break;

      case GL_TEXTURE_2D:
         oldtexobj = ctx->Texture.Current2D;
         if (texture==0) {
            /* use default 2-D texture */
            ctx->Texture.Current1D = ctx->Shared->TexObjectList->Next;
         }
         else {
            struct gl_texture_object *t;
            t = find_texture_object( ctx, texture );
            if (t) {
               if (t->Dimensions==2) {
                  /* success */
                  ctx->Texture.Current2D = t;
               }
               else {
                  /* wrong dimensionality */
                  gl_error( ctx, GL_INVALID_OPERATION, "glBindTexture" );
                  return;
               }
            }
            else {
               /* create new texture object */
               t = gl_alloc_texture_object();
               append_texture_object( ctx->Shared->TexObjectList, t );
               t->Name = texture;
               t->Dimensions = 2;
               ctx->Texture.Current2D = t;
            }
         }
         /* Tidy up reference counting */
         if (ctx->Texture.Current2D != oldtexobj && oldtexobj->Name>0) {
            /* decr reference count of the prev texture object */
            oldtexobj->RefCount--;
            assert( oldtexobj->RefCount >= 0 );
         }
         if (ctx->Texture.Current2D->Name>0) {
            ctx->Texture.Current2D->RefCount++;
         }
#ifdef FOO
         if (!ctx->Texture.Current2D->Complete) {
            /* re-examine texture completeness */
            gl_update_texture_state();
         }
#endif

         /* Check if we may have to use a new triangle rasterizer */
         if (oldtexobj->WrapS != ctx->Texture.Current1D->WrapS
             || oldtexobj->WrapT != ctx->Texture.Current1D->WrapT
             || oldtexobj->MinFilter != ctx->Texture.Current1D->MinFilter
             || oldtexobj->MagFilter != ctx->Texture.Current1D->MagFilter) {
            ctx->NewState |= NEW_RASTER_OPS;
         }

         /* The current 2D texture object can never be NULL! */
         assert(ctx->Texture.Current2D);
         break;

      case GL_TEXTURE_3D_EXT:
         oldtexobj = ctx->Texture.Current3D;
         if (texture==0) {
            /* use default 3-D texture */
            ctx->Texture.Current1D = ctx->Shared->TexObjectList->Next->Next;
         }
         else {
            struct gl_texture_object *t;
            t = find_texture_object( ctx, texture );
            if (t) {
               if (t->Dimensions==3) {
                  /* success */
                  ctx->Texture.Current3D = t;
               }
               else {
                  /* wrong dimensionality */
                  gl_error( ctx, GL_INVALID_OPERATION, "glBindTexture" );
                  return;
               }
            }
            else {
               /* create new texture object */
               t = gl_alloc_texture_object();
               append_texture_object( ctx->Shared->TexObjectList, t );
               t->Name = texture;
               t->Dimensions = 3;
               ctx->Texture.Current3D = t;
            }
         }
         /* Tidy up reference counting */
         if (ctx->Texture.Current3D != oldtexobj && oldtexobj->Name>0) {
            /* decr reference count of the prev texture object */
            oldtexobj->RefCount--;
            assert( oldtexobj->RefCount >= 0 );
         }
         if (ctx->Texture.Current3D->Name>0) {
            ctx->Texture.Current3D->RefCount++;
         }
#ifdef FOO
         if (!ctx->Texture.Current3D->Complete) {
            /* re-examine texture completeness */
            gl_update_texture_state();
         }
#endif

         /* Check if we may have to use a new triangle rasterizer */
         if (oldtexobj->WrapS != ctx->Texture.Current3D->WrapS
             || oldtexobj->WrapT != ctx->Texture.Current3D->WrapT
             || oldtexobj->MinFilter != ctx->Texture.Current3D->MinFilter
             || oldtexobj->MagFilter != ctx->Texture.Current3D->MagFilter) {
            ctx->NewState |= NEW_RASTER_OPS;
         }

         /* The current 3D texture object can never be NULL! */
         assert(ctx->Texture.Current3D);
         break;

      default:
         gl_error( ctx, GL_INVALID_ENUM, "glBindTexture" );
         return;
   }
 
   /* Pass BindTexture to device driver */
   if (ctx->Driver.BindTexture) {
      (*ctx->Driver.BindTexture)( ctx, target, texture );
   }
}



void gl_PrioritizeTextures( GLcontext *ctx,
                            GLsizei n, const GLuint *textures,
                            const GLclampf *priorities )
{
   GLuint i;

   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glAreTexturesResident" );
      return;
   }
   if (n<0) {
      gl_error( ctx, GL_INVALID_VALUE, "glAreTexturesResident(n)" );
      return;
   }

   for (i=0;i<n;i++) {
      struct gl_texture_object *t;
      if (textures[i]>0) {
         t = find_texture_object( ctx, textures[i] );
         if (t) {
            t->Priority = CLAMP( priorities[i], 0.0F, 1.0F );
         }
      }
   }
}



GLboolean gl_AreTexturesResident( GLcontext *ctx, GLsizei n,
                                  const GLuint *textures,
                                  GLboolean *residences )
{
   GLboolean resident = GL_TRUE;
   GLuint i;

   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glAreTexturesResident" );
      return GL_FALSE;
   }
   if (n<0) {
      gl_error( ctx, GL_INVALID_VALUE, "glAreTexturesResident(n)" );
      return GL_FALSE;
   }

   for (i=0;i<n;i++) {
      struct gl_texture_object *t;
      if (textures[i]==0) {
         gl_error( ctx, GL_INVALID_VALUE, "glAreTexturesResident(textures)" );
         return GL_FALSE;
      }
      t = find_texture_object( ctx, textures[i] );
      if (t) {
         /* we consider all valid texture objects to be resident */
         residences[i] = GL_TRUE;
      }
      else {
         gl_error( ctx, GL_INVALID_VALUE, "glAreTexturesResident(textures)" );
         return GL_FALSE;
      }
   }
   return resident;
}



GLboolean gl_IsTexture( GLcontext *ctx, GLuint texture )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glIsTextures" );
      return GL_FALSE;
   }
   if (texture>0 && find_texture_object(ctx,texture)) {
      return GL_TRUE;
   }
   else {
      return GL_FALSE;
   }
}

