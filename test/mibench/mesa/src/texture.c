/* $Id: texture.c,v 1.19 1997/03/04 19:55:58 brianp Exp $ */

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
 * $Log: texture.c,v $
 * Revision 1.19  1997/03/04 19:55:58  brianp
 * small texture sampling optimizations.  better comments.
 *
 * Revision 1.18  1997/03/04 19:19:20  brianp
 * fixed a number of problems with texture borders
 *
 * Revision 1.17  1997/02/27 19:58:08  brianp
 * call gl_problem() instead of gl_warning()
 *
 * Revision 1.16  1997/02/09 19:53:43  brianp
 * now use TEXTURE_xD enable constants
 *
 * Revision 1.15  1997/02/09 18:53:14  brianp
 * added GL_EXT_texture3D support
 *
 * Revision 1.14  1997/01/30 21:06:03  brianp
 * added some missing glGetTexLevelParameter() GLenums
 *
 * Revision 1.13  1997/01/16 03:36:01  brianp
 * added calls to device driver TexParameter() and TexEnv() functions
 *
 * Revision 1.12  1997/01/09 19:48:30  brianp
 * better error checking
 * added gl_texturing_enabled()
 *
 * Revision 1.11  1996/12/20 20:22:30  brianp
 * linear interpolation between mipmap levels was reverse weighted
 * max mipmap level was incorrectly tested for
 *
 * Revision 1.10  1996/12/12 22:33:05  brianp
 * minor changes to gl_texgen()
 *
 * Revision 1.9  1996/12/07 10:35:41  brianp
 * implmented glGetTexGen*() functions
 *
 * Revision 1.8  1996/11/14 01:03:09  brianp
 * removed const's from gl_texgen() function to avoid VMS compiler warning
 *
 * Revision 1.7  1996/11/08 02:19:52  brianp
 * gl_do_texgen() replaced with gl_texgen()
 *
 * Revision 1.6  1996/10/26 17:17:30  brianp
 * glTexGen GL_EYE_PLANE vector now transformed by inverse modelview matrix
 *
 * Revision 1.5  1996/10/11 03:42:38  brianp
 * replaced old _EXT symbols
 *
 * Revision 1.4  1996/09/27 01:30:24  brianp
 * added missing default cases to switches
 *
 * Revision 1.3  1996/09/15 14:18:55  brianp
 * now use GLframebuffer and GLvisual
 *
 * Revision 1.2  1996/09/15 01:48:58  brianp
 * removed #define NULL 0
 *
 * Revision 1.1  1996/09/13 01:38:16  brianp
 * Initial revision
 *
 */


#include <assert.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "context.h"
#include "dlist.h"
#include "macros.h"
#include "matrix.h"
#include "pb.h"
#include "teximage.h"
#include "texture.h"
#include "types.h"
#include "xform.h"




/**********************************************************************/
/*                       Texture Environment                          */
/**********************************************************************/



void gl_TexEnvfv( GLcontext *ctx,
                  GLenum target, GLenum pname, const GLfloat *param )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glTexEnv" );
      return;
   }

   if (target!=GL_TEXTURE_ENV) {
      gl_error( ctx, GL_INVALID_ENUM, "glTexEnv(target)" );
      return;
   }

   if (pname==GL_TEXTURE_ENV_MODE) {
      GLenum mode = (GLenum) (GLint) *param;
      switch (mode) {
	 case GL_MODULATE:
	 case GL_BLEND:
	 case GL_DECAL:
	 case GL_REPLACE:
	    ctx->Texture.EnvMode = mode;
	    break;
	 default:
	    gl_error( ctx, GL_INVALID_ENUM, "glTexEnv(param)" );
	    return;
      }
   }
   else if (pname==GL_TEXTURE_ENV_COLOR) {
      ctx->Texture.EnvColor[0] = CLAMP( param[0], 0.0, 1.0 );
      ctx->Texture.EnvColor[1] = CLAMP( param[1], 0.0, 1.0 );
      ctx->Texture.EnvColor[2] = CLAMP( param[2], 0.0, 1.0 );
      ctx->Texture.EnvColor[3] = CLAMP( param[3], 0.0, 1.0 );
   }
   else {
      gl_error( ctx, GL_INVALID_ENUM, "glTexEnv(pname)" );
      return;
   }

   /* Tell device driver about the new texture environment */
   if (ctx->Driver.TexEnv) {
      (*ctx->Driver.TexEnv)( ctx, pname, param );
   }
}





void gl_GetTexEnvfv( GLcontext *ctx,
                     GLenum target, GLenum pname, GLfloat *params )
{
   if (target!=GL_TEXTURE_ENV) {
      gl_error( ctx, GL_INVALID_ENUM, "glGetTexEnvfv(target)" );
      return;
   }
   switch (pname) {
      case GL_TEXTURE_ENV_MODE:
         *params = (GLfloat) ctx->Texture.EnvMode;
	 break;
      case GL_TEXTURE_ENV_COLOR:
	 COPY_4V( params, ctx->Texture.EnvColor );
	 break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetTexEnvfv(pname)" );
   }
}


void gl_GetTexEnviv( GLcontext *ctx,
                     GLenum target, GLenum pname, GLint *params )
{
   if (target!=GL_TEXTURE_ENV) {
      gl_error( ctx, GL_INVALID_ENUM, "glGetTexEnvfv(target)" );
      return;
   }
   switch (pname) {
      case GL_TEXTURE_ENV_MODE:
         *params = (GLint) ctx->Texture.EnvMode;
	 break;
      case GL_TEXTURE_ENV_COLOR:
	 params[0] = FLOAT_TO_INT( ctx->Texture.EnvColor[0] );
	 params[1] = FLOAT_TO_INT( ctx->Texture.EnvColor[1] );
	 params[2] = FLOAT_TO_INT( ctx->Texture.EnvColor[2] );
	 params[3] = FLOAT_TO_INT( ctx->Texture.EnvColor[3] );
	 break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetTexEnvfv(pname)" );
   }
}




/**********************************************************************/
/*                       Texture Parameters                           */
/**********************************************************************/


void gl_TexParameterfv( GLcontext *ctx,
                        GLenum target, GLenum pname, const GLfloat *params )
{
   GLenum eparam = (GLenum) (GLint) params[0];
   GLuint ddTexObject;

   if (target==GL_TEXTURE_1D) {
      switch (pname) {
	 case GL_TEXTURE_MIN_FILTER:
	    if (eparam==GL_NEAREST || eparam==GL_LINEAR
		|| eparam==GL_NEAREST_MIPMAP_NEAREST
		|| eparam==GL_LINEAR_MIPMAP_NEAREST
		|| eparam==GL_NEAREST_MIPMAP_LINEAR
		|| eparam==GL_LINEAR_MIPMAP_LINEAR) {
	       ctx->Texture.Current1D->MinFilter = eparam;
               ctx->NewState |= NEW_TEXTURING;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
               return;
	    }
	    break;
	 case GL_TEXTURE_MAG_FILTER:
	    if (eparam==GL_NEAREST || eparam==GL_LINEAR) {
	       ctx->Texture.Current1D->MagFilter = eparam;
               ctx->NewState |= NEW_TEXTURING;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
               return;
	    }
	    break;
	 case GL_TEXTURE_WRAP_S:
	    if (eparam==GL_CLAMP || eparam==GL_REPEAT) {
	       ctx->Texture.Current1D->WrapS = eparam;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
               return;
	    }
	    break;
	 case GL_TEXTURE_WRAP_T:
	    if (eparam==GL_CLAMP || eparam==GL_REPEAT) {
	       ctx->Texture.Current1D->WrapT = eparam;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
               return;
	    }
	    break;
         case GL_TEXTURE_BORDER_COLOR:
            {
               GLint *bc = ctx->Texture.Current1D->BorderColor;
               bc[0] = CLAMP((GLint)(params[0]*255.0), 0, 255);
               bc[1] = CLAMP((GLint)(params[1]*255.0), 0, 255);
               bc[2] = CLAMP((GLint)(params[2]*255.0), 0, 255);
               bc[3] = CLAMP((GLint)(params[3]*255.0), 0, 255);
            }
            break;
	 default:
	    gl_error( ctx, GL_INVALID_ENUM, "glTexParameter(pname)" );
            return;
      }
      ddTexObject = ctx->Texture.Current1D->Name;
   }
   else if (target==GL_TEXTURE_2D) {
      switch (pname) {
	 case GL_TEXTURE_MIN_FILTER:
	    if (eparam==GL_NEAREST || eparam==GL_LINEAR
		|| eparam==GL_NEAREST_MIPMAP_NEAREST
		|| eparam==GL_LINEAR_MIPMAP_NEAREST
		|| eparam==GL_NEAREST_MIPMAP_LINEAR
		|| eparam==GL_LINEAR_MIPMAP_LINEAR) {
	       ctx->Texture.Current2D->MinFilter = eparam;
               ctx->NewState |= NEW_TEXTURING;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
               return;
	    }
	    break;
	 case GL_TEXTURE_MAG_FILTER:
	    if (eparam==GL_NEAREST || eparam==GL_LINEAR) {
	       ctx->Texture.Current2D->MagFilter = eparam;
               ctx->NewState |= NEW_TEXTURING;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
               return;
	    }
	    break;
	 case GL_TEXTURE_WRAP_S:
	    if (eparam==GL_CLAMP || eparam==GL_REPEAT) {
	       ctx->Texture.Current2D->WrapS = eparam;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
               return;
	    }
	    break;
	 case GL_TEXTURE_WRAP_T:
	    if (eparam==GL_CLAMP || eparam==GL_REPEAT) {
	       ctx->Texture.Current2D->WrapT = eparam;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
               return;
	    }
	    break;
         case GL_TEXTURE_BORDER_COLOR:
            {
               GLint *bc = ctx->Texture.Current2D->BorderColor;
               bc[0] = CLAMP((GLint)(params[0]*255.0), 0, 255);
               bc[1] = CLAMP((GLint)(params[1]*255.0), 0, 255);
               bc[2] = CLAMP((GLint)(params[2]*255.0), 0, 255);
               bc[3] = CLAMP((GLint)(params[3]*255.0), 0, 255);
            }
            break;
	 default:
	    gl_error( ctx, GL_INVALID_ENUM, "glTexParameter(pname)" );
            return;
      }
      ddTexObject = ctx->Texture.Current1D->Name;
   }
   else if (target==GL_TEXTURE_3D_EXT) {
      switch (pname) {
         case GL_TEXTURE_MIN_FILTER:
            if (eparam==GL_NEAREST || eparam==GL_LINEAR
                || eparam==GL_NEAREST_MIPMAP_NEAREST
                || eparam==GL_LINEAR_MIPMAP_NEAREST
                || eparam==GL_NEAREST_MIPMAP_LINEAR
                || eparam==GL_LINEAR_MIPMAP_LINEAR) {
               ctx->Texture.Current3D->MinFilter = eparam;
               ctx->NewState |= NEW_TEXTURING;
            }
            else {
               gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
            }
            break;
         case GL_TEXTURE_MAG_FILTER:
            if (eparam==GL_NEAREST || eparam==GL_LINEAR) {
               ctx->Texture.Current3D->MagFilter = eparam;
               ctx->NewState |= NEW_TEXTURING;
            }
            else {
               gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
            }
            break;
         case GL_TEXTURE_WRAP_S:
            if (eparam==GL_CLAMP || eparam==GL_REPEAT) {
               ctx->Texture.Current3D->WrapS = eparam;
            }
            else {
               gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
            }
           break;
         case GL_TEXTURE_WRAP_T:
            if (eparam==GL_CLAMP || eparam==GL_REPEAT) {
               ctx->Texture.Current3D->WrapT = eparam;
            }
            else {
               gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
            }
            break;
         case GL_TEXTURE_WRAP_R_EXT:
            if (eparam==GL_CLAMP || eparam==GL_REPEAT) {
               ctx->Texture.Current3D->WrapR = eparam;
            }
            else {
               gl_error( ctx, GL_INVALID_VALUE, "glTexParameter(param)" );
            }
            break;
         case GL_TEXTURE_BORDER_COLOR:
            {
               GLint *bc = ctx->Texture.Current3D->BorderColor;
               bc[0] = CLAMP((GLint)(params[0]*255.0), 0, 255);
               bc[1] = CLAMP((GLint)(params[1]*255.0), 0, 255);
               bc[2] = CLAMP((GLint)(params[2]*255.0), 0, 255);
               bc[3] = CLAMP((GLint)(params[3]*255.0), 0, 255);
            }
            break;
         default:
            gl_error( ctx, GL_INVALID_ENUM, "glTexParameter(pname)" );
      }
   }
   else {
      gl_error( ctx, GL_INVALID_ENUM, "glTexParameter(target)" );
      return;
   }

   /* Pass this glTexParameter*() call to the device driver. */
   if (ctx->Driver.TexParameter) {
      (*ctx->Driver.TexParameter)( ctx, target, ddTexObject, pname, params );
   }
}



void gl_GetTexLevelParameterfv( GLcontext *ctx, GLenum target, GLint level,
                                GLenum pname, GLfloat *params )
{
   struct gl_texture_image *tex;

   if (level<0 || level>=MAX_TEXTURE_LEVELS) {
      gl_error( ctx, GL_INVALID_VALUE, "glGetTexLevelParameterfv" );
      return;
   }

   switch (target) {
      case GL_TEXTURE_1D:
         tex = ctx->Texture.Current1D->Image[level];
         switch (pname) {
	    case GL_TEXTURE_WIDTH:
	       *params = (GLfloat) tex->Width;
	       break;
	    case GL_TEXTURE_COMPONENTS:
	       *params = (GLfloat) tex->Format;
	       break;
	    case GL_TEXTURE_BORDER:
	       *params = (GLfloat) tex->Border;
	       break;
            case GL_TEXTURE_RED_SIZE:
            case GL_TEXTURE_GREEN_SIZE:
            case GL_TEXTURE_BLUE_SIZE:
            case GL_TEXTURE_ALPHA_SIZE:
            case GL_TEXTURE_INTENSITY_SIZE:
            case GL_TEXTURE_LUMINANCE_SIZE:
               *params = 8.0;  /* 8-bits */
               break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameterfv(pname)" );
	 }
	 break;
      case GL_TEXTURE_2D:
         tex = ctx->Texture.Current2D->Image[level];
	 switch (pname) {
	    case GL_TEXTURE_WIDTH:
	       *params = (GLfloat) tex->Width;
	       break;
	    case GL_TEXTURE_HEIGHT:
	       *params = (GLfloat) tex->Height;
	       break;
	    case GL_TEXTURE_COMPONENTS:
	       *params = (GLfloat) tex->Format;
	       break;
	    case GL_TEXTURE_BORDER:
	       *params = (GLfloat) tex->Border;
	       break;
            case GL_TEXTURE_RED_SIZE:
            case GL_TEXTURE_GREEN_SIZE:
            case GL_TEXTURE_BLUE_SIZE:
            case GL_TEXTURE_ALPHA_SIZE:
            case GL_TEXTURE_INTENSITY_SIZE:
            case GL_TEXTURE_LUMINANCE_SIZE:
               *params = 8.0;  /* 8-bits */
               break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameterfv(pname)" );
	 }
	 break;
      case GL_TEXTURE_3D_EXT:
         tex = ctx->Texture.Current3D->Image[level];
         switch (pname) {
            case GL_TEXTURE_WIDTH:
               *params = (GLfloat) tex->Width;
               break;
            case GL_TEXTURE_HEIGHT:
               *params = (GLfloat) tex->Height;
               break;
            case GL_TEXTURE_DEPTH_EXT:
               *params = (GLfloat) tex->Depth;
               break;
            case GL_TEXTURE_COMPONENTS:
               *params = (GLfloat) tex->Format;
               break;
            case GL_TEXTURE_BORDER:
               *params = (GLfloat) tex->Border;
               break;
            default:
               gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameterfv(pname)" );
         }
         break;
      case GL_PROXY_TEXTURE_1D:
         tex = ctx->Texture.Proxy1D->Image[level];
         switch (pname) {
	    case GL_TEXTURE_WIDTH:
	       *params = (GLfloat) tex->Width;
	       break;
	    case GL_TEXTURE_COMPONENTS:
	       *params = (GLfloat) tex->Format;
	       break;
	    case GL_TEXTURE_BORDER:
	       *params = (GLfloat) tex->Border;
	       break;
            case GL_TEXTURE_RED_SIZE:
            case GL_TEXTURE_GREEN_SIZE:
            case GL_TEXTURE_BLUE_SIZE:
            case GL_TEXTURE_ALPHA_SIZE:
            case GL_TEXTURE_INTENSITY_SIZE:
            case GL_TEXTURE_LUMINANCE_SIZE:
               *params = 8.0;  /* 8-bits */
               break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameterfv(pname)" );
	 }
	 break;
      case GL_PROXY_TEXTURE_2D:
         tex = ctx->Texture.Proxy2D->Image[level];
	 switch (pname) {
	    case GL_TEXTURE_WIDTH:
	       *params = (GLfloat) tex->Width;
	       break;
	    case GL_TEXTURE_HEIGHT:
	       *params = (GLfloat) tex->Height;
	       break;
	    case GL_TEXTURE_COMPONENTS:
	       *params = (GLfloat) tex->Format;
	       break;
	    case GL_TEXTURE_BORDER:
	       *params = (GLfloat) tex->Border;
	       break;
            case GL_TEXTURE_RED_SIZE:
            case GL_TEXTURE_GREEN_SIZE:
            case GL_TEXTURE_BLUE_SIZE:
            case GL_TEXTURE_ALPHA_SIZE:
            case GL_TEXTURE_INTENSITY_SIZE:
            case GL_TEXTURE_LUMINANCE_SIZE:
               *params = 8.0;  /* 8-bits */
               break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameterfv(pname)" );
	 }
	 break;
      case GL_PROXY_TEXTURE_3D_EXT:
         tex = ctx->Texture.Proxy3D->Image[level];
	 switch (pname) {
	    case GL_TEXTURE_WIDTH:
	       *params = (GLfloat) tex->Width;
	       break;
	    case GL_TEXTURE_HEIGHT:
	       *params = (GLfloat) tex->Height;
	       break;
	    case GL_TEXTURE_DEPTH_EXT:
	       *params = (GLfloat) tex->Depth;
	       break;
	    case GL_TEXTURE_COMPONENTS:
	       *params = (GLfloat) tex->Format;
	       break;
	    case GL_TEXTURE_BORDER:
	       *params = (GLfloat) tex->Border;
	       break;
            case GL_TEXTURE_RED_SIZE:
            case GL_TEXTURE_GREEN_SIZE:
            case GL_TEXTURE_BLUE_SIZE:
            case GL_TEXTURE_ALPHA_SIZE:
            case GL_TEXTURE_INTENSITY_SIZE:
            case GL_TEXTURE_LUMINANCE_SIZE:
               *params = 8.0;  /* 8-bits */
               break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameterfv(pname)" );
	 }
	 break;
     default:
	 gl_error( ctx, GL_INVALID_ENUM, "glGetTexLevelParameterfv(target)" );
   }	 
}



void gl_GetTexLevelParameteriv( GLcontext *ctx, GLenum target, GLint level,
                                GLenum pname, GLint *params )
{
   struct gl_texture_image *tex;

   if (level<0 || level>=MAX_TEXTURE_LEVELS) {
      gl_error( ctx, GL_INVALID_VALUE, "glGetTexLevelParameteriv" );
      return;
   }

   switch (target) {
      case GL_TEXTURE_1D:
         tex = ctx->Texture.Current1D->Image[level];
         switch (pname) {
	    case GL_TEXTURE_WIDTH:
	       *params = tex->Width;
	       break;
	    case GL_TEXTURE_COMPONENTS:
	       *params = tex->Format;
	       break;
	    case GL_TEXTURE_BORDER:
	       *params = tex->Border;
	       break;
            case GL_TEXTURE_RED_SIZE:
            case GL_TEXTURE_GREEN_SIZE:
            case GL_TEXTURE_BLUE_SIZE:
            case GL_TEXTURE_ALPHA_SIZE:
            case GL_TEXTURE_INTENSITY_SIZE:
            case GL_TEXTURE_LUMINANCE_SIZE:
               *params = 8;  /* 8-bits */
               break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameteriv(pname)" );
	 }
	 break;
      case GL_TEXTURE_2D:
         tex = ctx->Texture.Current2D->Image[level];
	 switch (pname) {
	    case GL_TEXTURE_WIDTH:
	       *params = tex->Width;
	       break;
	    case GL_TEXTURE_HEIGHT:
	       *params = tex->Height;
	       break;
	    case GL_TEXTURE_COMPONENTS:
	       *params = tex->Format;
	       break;
	    case GL_TEXTURE_BORDER:
	       *params = tex->Border;
	       break;
            case GL_TEXTURE_RED_SIZE:
            case GL_TEXTURE_GREEN_SIZE:
            case GL_TEXTURE_BLUE_SIZE:
            case GL_TEXTURE_ALPHA_SIZE:
            case GL_TEXTURE_INTENSITY_SIZE:
            case GL_TEXTURE_LUMINANCE_SIZE:
               *params = 8;  /* 8-bits */
               break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameteriv(pname)" );
	 }
	 break;
      case GL_TEXTURE_3D_EXT:
         tex = ctx->Texture.Current3D->Image[level];
         switch (pname) {
            case GL_TEXTURE_WIDTH:
               *params = tex->Width;
               break;
            case GL_TEXTURE_HEIGHT:
               *params = tex->Height;
               break;
            case GL_TEXTURE_DEPTH_EXT:
               *params = tex->Depth;
               break;
            case GL_TEXTURE_COMPONENTS:
               *params = tex->Format;
               break;
            case GL_TEXTURE_BORDER:
               *params = tex->Border;
               break;
            default:
               gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameteriv(pname)" );
         }
         break;
      case GL_PROXY_TEXTURE_1D:
         tex = ctx->Texture.Proxy1D->Image[level];
         switch (pname) {
	    case GL_TEXTURE_WIDTH:
	       *params = tex->Width;
	       break;
	    case GL_TEXTURE_COMPONENTS:
	       *params = tex->Format;
	       break;
	    case GL_TEXTURE_BORDER:
	       *params = tex->Border;
	       break;
            case GL_TEXTURE_RED_SIZE:
            case GL_TEXTURE_GREEN_SIZE:
            case GL_TEXTURE_BLUE_SIZE:
            case GL_TEXTURE_ALPHA_SIZE:
            case GL_TEXTURE_INTENSITY_SIZE:
            case GL_TEXTURE_LUMINANCE_SIZE:
               *params = 8;  /* 8-bits */
               break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameteriv(pname)" );
	 }
	 break;
      case GL_PROXY_TEXTURE_2D:
         tex = ctx->Texture.Proxy2D->Image[level];
	 switch (pname) {
	    case GL_TEXTURE_WIDTH:
	       *params = tex->Width;
	       break;
	    case GL_TEXTURE_HEIGHT:
	       *params = tex->Height;
	       break;
	    case GL_TEXTURE_COMPONENTS:
	       *params = tex->Format;
	       break;
	    case GL_TEXTURE_BORDER:
	       *params = tex->Border;
	       break;
            case GL_TEXTURE_RED_SIZE:
            case GL_TEXTURE_GREEN_SIZE:
            case GL_TEXTURE_BLUE_SIZE:
            case GL_TEXTURE_ALPHA_SIZE:
            case GL_TEXTURE_INTENSITY_SIZE:
            case GL_TEXTURE_LUMINANCE_SIZE:
               *params = 8;  /* 8-bits */
               break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameteriv(pname)" );
	 }
	 break;
      case GL_PROXY_TEXTURE_3D_EXT:
         tex = ctx->Texture.Proxy3D->Image[level];
	 switch (pname) {
	    case GL_TEXTURE_WIDTH:
	       *params = tex->Width;
	       break;
	    case GL_TEXTURE_HEIGHT:
	       *params = tex->Height;
	       break;
	    case GL_TEXTURE_DEPTH_EXT:
	       *params = tex->Depth;
	       break;
	    case GL_TEXTURE_COMPONENTS:
	       *params = tex->Format;
	       break;
	    case GL_TEXTURE_BORDER:
	       *params = tex->Border;
	       break;
            case GL_TEXTURE_RED_SIZE:
            case GL_TEXTURE_GREEN_SIZE:
            case GL_TEXTURE_BLUE_SIZE:
            case GL_TEXTURE_ALPHA_SIZE:
            case GL_TEXTURE_INTENSITY_SIZE:
            case GL_TEXTURE_LUMINANCE_SIZE:
               *params = 8;  /* 8-bits */
               break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM,
                         "glGetTexLevelParameteriv(pname)" );
	 }
	 break;
     default:
	 gl_error( ctx, GL_INVALID_ENUM, "glGetTexLevelParameteriv(target)" );
   }	 
}




void gl_GetTexParameterfv( GLcontext *ctx,
                           GLenum target, GLenum pname, GLfloat *params )
{
   switch (target) {
      case GL_TEXTURE_1D:
         switch (pname) {
	    case GL_TEXTURE_MAG_FILTER:
	       *params = (GLfloat) ctx->Texture.Current1D->MagFilter;
	       break;
	    case GL_TEXTURE_MIN_FILTER:
	       *params = (GLfloat) ctx->Texture.Current1D->MinFilter;
	       break;
	    case GL_TEXTURE_WRAP_S:
	       *params = (GLfloat) ctx->Texture.Current1D->WrapS;
	       break;
	    case GL_TEXTURE_WRAP_T:
	       *params = (GLfloat) ctx->Texture.Current1D->WrapT;
	       break;
	    case GL_TEXTURE_BORDER_COLOR:
               params[0] = ctx->Texture.Current1D->BorderColor[0] / 255.0f;
               params[1] = ctx->Texture.Current1D->BorderColor[1] / 255.0f;
               params[2] = ctx->Texture.Current1D->BorderColor[2] / 255.0f;
               params[3] = ctx->Texture.Current1D->BorderColor[3] / 255.0f;
	       break;
	    case GL_TEXTURE_RESIDENT:
               *params = (GLfloat) GL_TRUE;
	       break;
            case GL_TEXTURE_PRIORITY:
               *params = ctx->Texture.Current1D->Priority;
	       break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM, "glGetTexParameterfv(pname)" );
	 }
         break;
      case GL_TEXTURE_2D:
         switch (pname) {
	    case GL_TEXTURE_MAG_FILTER:
	       *params = (GLfloat) ctx->Texture.Current2D->MagFilter;
	       break;
	    case GL_TEXTURE_MIN_FILTER:
	       *params = (GLfloat) ctx->Texture.Current2D->MinFilter;
	       break;
	    case GL_TEXTURE_WRAP_S:
	       *params = (GLfloat) ctx->Texture.Current2D->WrapS;
	       break;
	    case GL_TEXTURE_WRAP_T:
	       *params = (GLfloat) ctx->Texture.Current2D->WrapT;
	       break;
	    case GL_TEXTURE_BORDER_COLOR:
               params[0] = ctx->Texture.Current2D->BorderColor[0] / 255.0f;
               params[1] = ctx->Texture.Current2D->BorderColor[1] / 255.0f;
               params[2] = ctx->Texture.Current2D->BorderColor[2] / 255.0f;
               params[3] = ctx->Texture.Current2D->BorderColor[3] / 255.0f;
               break;
	    case GL_TEXTURE_RESIDENT:
               *params = (GLfloat) GL_TRUE;
	       break;
	    case GL_TEXTURE_PRIORITY:
               *params = ctx->Texture.Current2D->Priority;
	       break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM, "glGetTexParameterfv(pname)" );
	 }
	 break;
      case GL_TEXTURE_3D_EXT:
         switch (pname) {
            case GL_TEXTURE_MAG_FILTER:
               *params = (GLfloat) ctx->Texture.Current3D->MagFilter;
               break;
            case GL_TEXTURE_MIN_FILTER:
               *params = (GLfloat) ctx->Texture.Current3D->MinFilter;
               break;
            case GL_TEXTURE_WRAP_S:
               *params = (GLfloat) ctx->Texture.Current3D->WrapS;
               break;
            case GL_TEXTURE_WRAP_T:
               *params = (GLfloat) ctx->Texture.Current3D->WrapT;
               break;
            case GL_TEXTURE_WRAP_R_EXT:
               *params = (GLfloat) ctx->Texture.Current3D->WrapR;
               break;
            case GL_TEXTURE_BORDER_COLOR:
               params[0] = ctx->Texture.Current3D->BorderColor[0] / 255.0f;
               params[1] = ctx->Texture.Current3D->BorderColor[1] / 255.0f;
               params[2] = ctx->Texture.Current3D->BorderColor[2] / 255.0f;
               params[3] = ctx->Texture.Current3D->BorderColor[3] / 255.0f;
               break;
            case GL_TEXTURE_RESIDENT:
               *params = (GLfloat) GL_TRUE;
               break;
            case GL_TEXTURE_PRIORITY:
               *params = ctx->Texture.Current3D->Priority;
               break;
            default:
               gl_error( ctx, GL_INVALID_ENUM, "glGetTexParameterfv(pname)" );
         }
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetTexParameterfv(target)" );
   }
}


void gl_GetTexParameteriv( GLcontext *ctx,
                           GLenum target, GLenum pname, GLint *params )
{
   switch (target) {
      case GL_TEXTURE_1D:
         switch (pname) {
	    case GL_TEXTURE_MAG_FILTER:
	       *params = (GLint) ctx->Texture.Current1D->MagFilter;
	       break;
	    case GL_TEXTURE_MIN_FILTER:
	       *params = (GLint) ctx->Texture.Current1D->MinFilter;
	       break;
	    case GL_TEXTURE_WRAP_S:
	       *params = (GLint) ctx->Texture.Current1D->WrapS;
	       break;
	    case GL_TEXTURE_WRAP_T:
	       *params = (GLint) ctx->Texture.Current1D->WrapT;
	       break;
	    case GL_TEXTURE_BORDER_COLOR:
               {
                  GLfloat color[4];
                  color[0] = ctx->Texture.Current1D->BorderColor[0]/255.0;
                  color[1] = ctx->Texture.Current1D->BorderColor[1]/255.0;
                  color[2] = ctx->Texture.Current1D->BorderColor[2]/255.0;
                  color[3] = ctx->Texture.Current1D->BorderColor[3]/255.0;
                  params[0] = FLOAT_TO_INT( color[0] );
                  params[1] = FLOAT_TO_INT( color[1] );
                  params[2] = FLOAT_TO_INT( color[2] );
                  params[3] = FLOAT_TO_INT( color[3] );
               }
	       break;
	    case GL_TEXTURE_RESIDENT:
               *params = (GLint) GL_TRUE;
	       break;
	    case GL_TEXTURE_PRIORITY:
               *params = (GLint) ctx->Texture.Current1D->Priority;
	       break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM, "glGetTexParameteriv(pname)" );
	 }
         break;
      case GL_TEXTURE_2D:
         switch (pname) {
	    case GL_TEXTURE_MAG_FILTER:
	       *params = (GLint) ctx->Texture.Current2D->MagFilter;
	       break;
	    case GL_TEXTURE_MIN_FILTER:
	       *params = (GLint) ctx->Texture.Current2D->MinFilter;
	       break;
	    case GL_TEXTURE_WRAP_S:
	       *params = (GLint) ctx->Texture.Current2D->WrapS;
	       break;
	    case GL_TEXTURE_WRAP_T:
	       *params = (GLint) ctx->Texture.Current2D->WrapT;
	       break;
	    case GL_TEXTURE_BORDER_COLOR:
               {
                  GLfloat color[4];
                  color[0] = ctx->Texture.Current2D->BorderColor[0]/255.0;
                  color[1] = ctx->Texture.Current2D->BorderColor[1]/255.0;
                  color[2] = ctx->Texture.Current2D->BorderColor[2]/255.0;
                  color[3] = ctx->Texture.Current2D->BorderColor[3]/255.0;
                  params[0] = FLOAT_TO_INT( color[0] );
                  params[1] = FLOAT_TO_INT( color[1] );
                  params[2] = FLOAT_TO_INT( color[2] );
                  params[3] = FLOAT_TO_INT( color[3] );
               }
	       break;
	    case GL_TEXTURE_RESIDENT:
               *params = (GLint) GL_TRUE;
	       break;
	    case GL_TEXTURE_PRIORITY:
               *params = (GLint) ctx->Texture.Current2D->Priority;
	       break;
	    default:
	       gl_error( ctx, GL_INVALID_ENUM, "glGetTexParameteriv(pname)" );
	 }
	 break;
      case GL_TEXTURE_3D_EXT:
         switch (pname) {
            case GL_TEXTURE_MAG_FILTER:
               *params = (GLint) ctx->Texture.Current3D->MagFilter;
               break;
            case GL_TEXTURE_MIN_FILTER:
               *params = (GLint) ctx->Texture.Current3D->MinFilter;
               break;
            case GL_TEXTURE_WRAP_S:
               *params = (GLint) ctx->Texture.Current3D->WrapS;
               break;
            case GL_TEXTURE_WRAP_T:
               *params = (GLint) ctx->Texture.Current3D->WrapT;
               break;
            case GL_TEXTURE_WRAP_R_EXT:
               *params = (GLint) ctx->Texture.Current3D->WrapR;
               break;
            case GL_TEXTURE_BORDER_COLOR:
               {
                  GLfloat color[4];
                  color[0] = ctx->Texture.Current3D->BorderColor[0]/255.0;
                  color[1] = ctx->Texture.Current3D->BorderColor[1]/255.0;
                  color[2] = ctx->Texture.Current3D->BorderColor[2]/255.0;
                  color[3] = ctx->Texture.Current3D->BorderColor[3]/255.0;
                  params[0] = FLOAT_TO_INT( color[0] );
                  params[1] = FLOAT_TO_INT( color[1] );
                  params[2] = FLOAT_TO_INT( color[2] );
                  params[3] = FLOAT_TO_INT( color[3] );
               }
               break;
            case GL_TEXTURE_RESIDENT:
               *params = (GLint) GL_TRUE;
               break;
            case GL_TEXTURE_PRIORITY:
               *params = (GLint) ctx->Texture.Current3D->Priority;
               break;
            default:
               gl_error( ctx, GL_INVALID_ENUM, "glGetTexParameteriv(pname)" );
         }
         break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetTexParameteriv(target)" );
   }
}




/**********************************************************************/
/*                    Texture Coord Generation                        */
/**********************************************************************/


void gl_TexGenfv( GLcontext *ctx,
                  GLenum coord, GLenum pname, const GLfloat *params )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glTexGenfv" );
      return;
   }

   switch( coord ) {
      case GL_S:
         if (pname==GL_TEXTURE_GEN_MODE) {
	    GLenum mode = (GLenum) (GLint) *params;
	    if (mode==GL_OBJECT_LINEAR ||
		mode==GL_EYE_LINEAR ||
		mode==GL_SPHERE_MAP) {
	       ctx->Texture.GenModeS = mode;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_ENUM, "glTexGenfv(param)" );
	       return;
	    }
	 }
	 else if (pname==GL_OBJECT_PLANE) {
	    ctx->Texture.ObjectPlaneS[0] = params[0];
	    ctx->Texture.ObjectPlaneS[1] = params[1];
	    ctx->Texture.ObjectPlaneS[2] = params[2];
	    ctx->Texture.ObjectPlaneS[3] = params[3];
	 }
	 else if (pname==GL_EYE_PLANE) {
            /* Transform plane equation by the inverse modelview matrix */
            if (!ctx->ModelViewInvValid) {
               gl_compute_modelview_inverse(ctx);
            }
            gl_transform_vector( ctx->Texture.EyePlaneS, params,
                                 ctx->ModelViewInv );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glTexGenfv(pname)" );
	    return;
	 }
	 break;
      case GL_T:
         if (pname==GL_TEXTURE_GEN_MODE) {
	    GLenum mode = (GLenum) (GLint) *params;
	    if (mode==GL_OBJECT_LINEAR ||
		mode==GL_EYE_LINEAR ||
		mode==GL_SPHERE_MAP) {
	       ctx->Texture.GenModeT = mode;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_ENUM, "glTexGenfv(param)" );
	       return;
	    }
	 }
	 else if (pname==GL_OBJECT_PLANE) {
	    ctx->Texture.ObjectPlaneT[0] = params[0];
	    ctx->Texture.ObjectPlaneT[1] = params[1];
	    ctx->Texture.ObjectPlaneT[2] = params[2];
	    ctx->Texture.ObjectPlaneT[3] = params[3];
	 }
	 else if (pname==GL_EYE_PLANE) {
            /* Transform plane equation by the inverse modelview matrix */
            if (!ctx->ModelViewInvValid) {
               gl_compute_modelview_inverse(ctx);
            }
            gl_transform_vector( ctx->Texture.EyePlaneT, params,
                                 ctx->ModelViewInv );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glTexGenfv(pname)" );
	    return;
	 }
	 break;
      case GL_R:
         if (pname==GL_TEXTURE_GEN_MODE) {
	    GLenum mode = (GLenum) (GLint) *params;
	    if (mode==GL_OBJECT_LINEAR ||
		mode==GL_EYE_LINEAR) {
	       ctx->Texture.GenModeR = mode;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_ENUM, "glTexGenfv(param)" );
	       return;
	    }
	 }
	 else if (pname==GL_OBJECT_PLANE) {
	    ctx->Texture.ObjectPlaneR[0] = params[0];
	    ctx->Texture.ObjectPlaneR[1] = params[1];
	    ctx->Texture.ObjectPlaneR[2] = params[2];
	    ctx->Texture.ObjectPlaneR[3] = params[3];
	 }
	 else if (pname==GL_EYE_PLANE) {
            /* Transform plane equation by the inverse modelview matrix */
            if (!ctx->ModelViewInvValid) {
               gl_compute_modelview_inverse(ctx);
            }
            gl_transform_vector( ctx->Texture.EyePlaneR, params,
                                 ctx->ModelViewInv );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glTexGenfv(pname)" );
	    return;
	 }
	 break;
      case GL_Q:
         if (pname==GL_TEXTURE_GEN_MODE) {
	    GLenum mode = (GLenum) (GLint) *params;
	    if (mode==GL_OBJECT_LINEAR ||
		mode==GL_EYE_LINEAR) {
	       ctx->Texture.GenModeQ = mode;
	    }
	    else {
	       gl_error( ctx, GL_INVALID_ENUM, "glTexGenfv(param)" );
	       return;
	    }
	 }
	 else if (pname==GL_OBJECT_PLANE) {
	    ctx->Texture.ObjectPlaneQ[0] = params[0];
	    ctx->Texture.ObjectPlaneQ[1] = params[1];
	    ctx->Texture.ObjectPlaneQ[2] = params[2];
	    ctx->Texture.ObjectPlaneQ[3] = params[3];
	 }
	 else if (pname==GL_EYE_PLANE) {
            /* Transform plane equation by the inverse modelview matrix */
            if (!ctx->ModelViewInvValid) {
               gl_compute_modelview_inverse(ctx);
            }
            gl_transform_vector( ctx->Texture.EyePlaneQ, params,
                                 ctx->ModelViewInv );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glTexGenfv(pname)" );
	    return;
	 }
	 break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glTexGenfv(coord)" );
	 return;
   }

   ctx->NewState |= NEW_TEXTURING;
}



void gl_GetTexGendv( GLcontext *ctx,
                     GLenum coord, GLenum pname, GLdouble *params )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glGetTexGendv" );
      return;
   }

   switch( coord ) {
      case GL_S:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeS;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneS );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneS );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGendv(pname)" );
	    return;
	 }
	 break;
      case GL_T:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeT;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneT );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneT );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGendv(pname)" );
	    return;
	 }
	 break;
      case GL_R:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeR;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneR );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneR );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGendv(pname)" );
	    return;
	 }
	 break;
      case GL_Q:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeQ;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneQ );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneQ );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGendv(pname)" );
	    return;
	 }
	 break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetTexGendv(coord)" );
	 return;
   }
}



void gl_GetTexGenfv( GLcontext *ctx,
                     GLenum coord, GLenum pname, GLfloat *params )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glGetTexGenfv" );
      return;
   }

   switch( coord ) {
      case GL_S:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeS;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneS );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneS );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGenfv(pname)" );
	    return;
	 }
	 break;
      case GL_T:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeT;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneT );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneT );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGenfv(pname)" );
	    return;
	 }
	 break;
      case GL_R:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeR;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneR );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneR );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGenfv(pname)" );
	    return;
	 }
	 break;
      case GL_Q:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeQ;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneQ );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneQ );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGenfv(pname)" );
	    return;
	 }
	 break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetTexGenfv(coord)" );
	 return;
   }
}



void gl_GetTexGeniv( GLcontext *ctx,
                     GLenum coord, GLenum pname, GLint *params )
{
   if (INSIDE_BEGIN_END(ctx)) {
      gl_error( ctx, GL_INVALID_OPERATION, "glGetTexGeniv" );
      return;
   }

   switch( coord ) {
      case GL_S:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeS;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneS );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneS );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGeniv(pname)" );
	    return;
	 }
	 break;
      case GL_T:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeT;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneT );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneT );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGeniv(pname)" );
	    return;
	 }
	 break;
      case GL_R:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeR;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneR );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneR );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGeniv(pname)" );
	    return;
	 }
	 break;
      case GL_Q:
         if (pname==GL_TEXTURE_GEN_MODE) {
            params[0] = ctx->Texture.GenModeQ;
	 }
	 else if (pname==GL_OBJECT_PLANE) {
            COPY_4V( params, ctx->Texture.ObjectPlaneQ );
	 }
	 else if (pname==GL_EYE_PLANE) {
            COPY_4V( params, ctx->Texture.EyePlaneQ );
	 }
	 else {
	    gl_error( ctx, GL_INVALID_ENUM, "glGetTexGeniv(pname)" );
	    return;
	 }
	 break;
      default:
         gl_error( ctx, GL_INVALID_ENUM, "glGetTexGeniv(coord)" );
	 return;
   }
}



/*
 * Perform automatic texture coordinate generation.
 * Input:  ctx - the context
 *         n - number of texture coordinates to generate
 *         obj - array of vertexes in object coordinate system
 *         eye - array of vertexes in eye coordinate system
 *         normal - array of normal vectores in eye coordinate system
 * Output:  texcoord - array of resuling texture coordinates
 */
void gl_texgen( GLcontext *ctx, GLint n,
                GLfloat obj[][4], GLfloat eye[][4],
                GLfloat normal[][3], GLfloat texcoord[][4] )
{
   /* special case: S and T sphere mapping */
   if (ctx->Texture.TexGenEnabled==(S_BIT|T_BIT)
       && ctx->Texture.GenModeS==GL_SPHERE_MAP
       && ctx->Texture.GenModeT==GL_SPHERE_MAP) {
      GLint i;
      for (i=0;i<n;i++) {
         GLfloat u[3], two_nn, m, fx, fy, fz;
         COPY_3V( u, eye[i] );
         NORMALIZE_3V( u );
         two_nn = 2.0F * DOT3(normal[i],normal[i]);
         fx = u[0] - two_nn * u[0];
         fy = u[1] - two_nn * u[1];
         fz = u[2] - two_nn * u[2];
         m = 2.0F * sqrt( fx*fx + fy*fy + (fz+1.0F)*(fz+1.0F) );
         if (m==0.0F) {
            texcoord[i][0] = 0.5F;
            texcoord[i][1] = 0.5F;
         }
         else {
            GLfloat mInv = 1.0F / m;
            texcoord[i][0] = fx * mInv + 0.5F;
            texcoord[i][1] = fy * mInv + 0.5F;
         }
      }
      return;
   }

   /* general solution */
   if (ctx->Texture.TexGenEnabled & S_BIT) {
      GLint i;
      switch (ctx->Texture.GenModeS) {
	 case GL_OBJECT_LINEAR:
            for (i=0;i<n;i++) {
               texcoord[i][0] = DOT4( obj[i], ctx->Texture.ObjectPlaneS );
            }
	    break;
	 case GL_EYE_LINEAR:
            for (i=0;i<n;i++) {
               texcoord[i][0] = DOT4( eye[i], ctx->Texture.EyePlaneS );
            }
	    break;
	 case GL_SPHERE_MAP:
            for (i=0;i<n;i++) {
               GLfloat u[3], two_nn, m, fx, fy, fz;
               COPY_3V( u, eye[i] );
               NORMALIZE_3V( u );
               two_nn = 2.0*DOT3(normal[i],normal[i]);
               fx = u[0] - two_nn * u[0];
               fy = u[1] - two_nn * u[1];
               fz = u[2] - two_nn * u[2];
               m = 2.0 * sqrt( fx*fx + fy*fy + (fz+1.0)*(fz+1.0) );
               if (m==0.0) {
                  texcoord[i][0] = 0.0;
               }
               else {
                  texcoord[i][0] = fx / m + 0.5;
               }
            }
	    break;
         default:
            abort();
      }
   }

   if (ctx->Texture.TexGenEnabled & T_BIT) {
      GLint i;
      switch (ctx->Texture.GenModeT) {
	 case GL_OBJECT_LINEAR:
            for (i=0;i<n;i++) {
               texcoord[i][1] = DOT4( obj[i], ctx->Texture.ObjectPlaneT );
            }
	    break;
	 case GL_EYE_LINEAR:
            for (i=0;i<n;i++) {
               texcoord[i][1] = DOT4( eye[i], ctx->Texture.EyePlaneT );
            }
	    break;
	 case GL_SPHERE_MAP:
	    /* TODO: safe to assume that m and fy valid from above??? */
            for (i=0;i<n;i++) {
               GLfloat u[3], two_nn, m, fx, fy, fz;
               COPY_3V( u, eye[i] );
               NORMALIZE_3V( u );
               two_nn = 2.0*DOT3(normal[i],normal[i]);
               fx = u[0] - two_nn * u[0];
               fy = u[1] - two_nn * u[1];
               fz = u[2] - two_nn * u[2];
               m = 2.0 * sqrt( fx*fx + fy*fy + (fz+1.0)*(fz+1.0) );
               if (m==0.0) {
                  texcoord[i][1] = 0.0;
               }
               else {
                  texcoord[i][1] = fy / m + 0.5;
               }
            }
	    break;
         default:
            abort();
      }
   }

   if (ctx->Texture.TexGenEnabled & R_BIT) {
      GLint i;
      switch (ctx->Texture.GenModeR) {
	 case GL_OBJECT_LINEAR:
            for (i=0;i<n;i++) {
               texcoord[i][2] = DOT4( obj[i], ctx->Texture.ObjectPlaneR );
            }
	    break;
	 case GL_EYE_LINEAR:
            for (i=0;i<n;i++) {
               texcoord[i][2] = DOT4( eye[i], ctx->Texture.EyePlaneR );
            }
	    break;
         default:
            abort();
      }
   }

   if (ctx->Texture.TexGenEnabled & Q_BIT) {
      GLint i;
      switch (ctx->Texture.GenModeQ) {
	 case GL_OBJECT_LINEAR:
            for (i=0;i<n;i++) {
               texcoord[i][3] = DOT4( obj[i], ctx->Texture.ObjectPlaneQ );
            }
	    break;
	 case GL_EYE_LINEAR:
            for (i=0;i<n;i++) {
               texcoord[i][3] = DOT4( eye[i], ctx->Texture.EyePlaneQ );
            }
	    break;
         default:
            abort();
      }
   }
}





/**********************************************************************/
/*                    1-D Texture Sampling Functions                  */
/**********************************************************************/


/*
 * Return the fractional part of x.
 */
#define frac(x) ((GLfloat)(x)-floor((GLfloat)x))



/*
 * Given 1-D texture image and an (i) texel column coordinate, return the
 * texel color.
 */
static void get_1d_texel( struct gl_texture_image *img, GLint i,
                          GLubyte *red, GLubyte *green, GLubyte *blue,
                          GLubyte *alpha )
{
   GLubyte *texel;

   /* DEBUG */
   GLint width = img->Width;
   if (i<0 || i>=width)  abort();

   switch (img->Format) {
      case GL_ALPHA:
         *alpha = img->Data[ i ];
         return;
      case GL_LUMINANCE:
      case GL_INTENSITY:
         *red   = img->Data[ i ];
         return;
      case GL_LUMINANCE_ALPHA:
         texel = img->Data + i * 2;
         *red   = texel[0];
         *alpha = texel[1];
         return;
      case GL_RGB:
         texel = img->Data + i * 3;
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         return;
      case GL_RGBA:
         texel = img->Data + i * 4;
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         *alpha = texel[3];
         return;
      default:
         abort();
   }
}



/*
 * Return the texture sample for coordinate (s) using GL_NEAREST filter.
 */
static void sample_1d_nearest( GLcontext *ctx,
                               struct gl_texture_image *img,
                               GLfloat s,
                               GLubyte *red, GLubyte *green,
                               GLubyte *blue, GLubyte *alpha )
{
   GLint width = img->Width2;  /* without border, power of two */
   GLint i;
   GLubyte *texel;

   /* Clamp/Repeat S and convert to integer texel coordinate */
   if (ctx->Texture.Current1D->WrapS==GL_REPEAT) {
      /* s limited to [0,1) */
      /* i limited to [0,width-1] */
      i = (GLint) (s * width);
      i &= (width-1);
   }
   else {
      /* s limited to [0,1] */
      /* i limited to [0,width-1] */
      if (s<0.0F)        i = 0;
      else if (s>1.0F)   i = width-1;
      else               i = (GLint) (s * width);
   }

   /* skip over the border, if any */
   i += img->Border;

   /* Get the texel */
   switch (img->Format) {
      case GL_ALPHA:
         *alpha = img->Data[i];
         return;
      case GL_LUMINANCE:
      case GL_INTENSITY:
         *red   = img->Data[i];
         return;
      case GL_LUMINANCE_ALPHA:
         texel = img->Data + i * 2;
         *red   = texel[0];
         *alpha = texel[1];
         return;
      case GL_RGB:
         texel = img->Data + i * 3;
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         return;
      case GL_RGBA:
         texel = img->Data + i * 4;
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         *alpha = texel[3];
         return;
      default:
         abort();
   }
}



/*
 * Return the texture sample for coordinate (s) using GL_LINEAR filter.
 */
static void sample_1d_linear( GLcontext *ctx,
                              struct gl_texture_image *img,
                              GLfloat s,
                              GLubyte *red, GLubyte *green,
                              GLubyte *blue, GLubyte *alpha )
{
   GLint width = img->Width2;
   GLint i0, i1;
   GLfloat u;
   GLint i0border, i1border;

   u = s * width;
   if (ctx->Texture.Current1D->WrapS==GL_REPEAT) {
      i0 = ((GLint) floor(u - 0.5F)) % width;
      i1 = (i0 + 1) & (width-1);
      i0border = i1border = 0;
   }
   else {
      i0 = (GLint) floor(u - 0.5F);
      i1 = i0 + 1;
      i0border = (i0<0) | (i0>=width);
      i1border = (i1<0) | (i1>=width);
   }

   if (img->Border) {
      i0 += img->Border;
      i1 += img->Border;
      i0border = i1border = 0;
   }
   else {
      i0 &= (width-1);
   }

   {
      GLfloat a = frac(u - 0.5F);

      GLint w0 = (GLint) ((1.0F-a) * 256.0F);
      GLint w1 = (GLint) (      a  * 256.0F);

      GLubyte red0, green0, blue0, alpha0;
      GLubyte red1, green1, blue1, alpha1;

      if (i0border) {
         red0   = ctx->Texture.Current1D->BorderColor[0];
         green0 = ctx->Texture.Current1D->BorderColor[1];
         blue0  = ctx->Texture.Current1D->BorderColor[2];
         alpha0 = ctx->Texture.Current1D->BorderColor[3];
      }
      else {
         get_1d_texel( img, i0, &red0, &green0, &blue0, &alpha0 );
      }
      if (i1border) {
         red1   = ctx->Texture.Current1D->BorderColor[0];
         green1 = ctx->Texture.Current1D->BorderColor[1];
         blue1  = ctx->Texture.Current1D->BorderColor[2];
         alpha1 = ctx->Texture.Current1D->BorderColor[3];
      }
      else {
         get_1d_texel( img, i1, &red1, &green1, &blue1, &alpha1 );
      }

      *red   = (w0*red0   + w1*red1)   >> 8;
      *green = (w0*green0 + w1*green1) >> 8;
      *blue  = (w0*blue0  + w1*blue1)  >> 8;
      *alpha = (w0*alpha0 + w1*alpha1) >> 8;
   }
}


static void sample_1d_nearest_mipmap_nearest( GLcontext *ctx,
                                              GLfloat lambda, GLfloat s,
                                              GLubyte *red, GLubyte *green,
                                              GLubyte *blue, GLubyte *alpha )
{
   GLint level;
   if (lambda<=0.5F) {
      level = 0;
   }
   else {
      GLint widthlog2 = ctx->Texture.Current1D->Image[0]->WidthLog2;
      level = (GLint) (lambda + 0.499999F);
      if (level>widthlog2 ) {
         level = widthlog2;
      }
   }
   sample_1d_nearest( ctx, ctx->Texture.Current1D->Image[level],
                      s, red, green, blue, alpha );
}


static void sample_1d_linear_mipmap_nearest( GLcontext *ctx,
                                             GLfloat lambda, GLfloat s,
                                             GLubyte *red, GLubyte *green,
                                             GLubyte *blue, GLubyte *alpha )
{
   GLint level;
   if (lambda<=0.5F) {
      level = 0;
   }
   else {
      GLint widthlog2 = ctx->Texture.Current1D->Image[0]->WidthLog2;
      level = (GLint) (lambda + 0.499999F);
      if (level>widthlog2 ) {
         level = widthlog2;
      }
   }
   sample_1d_linear( ctx, ctx->Texture.Current1D->Image[level],
                     s, red, green, blue, alpha );
}



static void sample_1d_nearest_mipmap_linear( GLcontext *ctx,
                                             GLfloat lambda, GLfloat s,
                                             GLubyte *red, GLubyte *green,
                                             GLubyte *blue, GLubyte *alpha )
{
   GLint max = ctx->Texture.Current1D->Image[0]->MaxLog2;

   if (lambda>=max) {
      sample_1d_nearest( ctx, ctx->Texture.Current1D->Image[max],
                         s, red, green, blue, alpha );
   }
   else {
      GLubyte red0, green0, blue0, alpha0;
      GLubyte red1, green1, blue1, alpha1;
      GLfloat f = frac(lambda);
      GLint level = (GLint) (lambda + 1.0F);
      level = CLAMP( level, 1, max );
      sample_1d_nearest( ctx, ctx->Texture.Current1D->Image[level-1],
                         s, &red0, &green0, &blue0, &alpha0 );
      sample_1d_nearest( ctx, ctx->Texture.Current1D->Image[level],
                         s, &red1, &green1, &blue1, &alpha1 );
      *red   = (1.0F-f)*red0   + f*red1;
      *green = (1.0F-f)*green0 + f*green1;
      *blue  = (1.0F-f)*blue0  + f*blue1;
      *alpha = (1.0F-f)*alpha0 + f*alpha1;
   }
}



static void sample_1d_linear_mipmap_linear( GLcontext *ctx,
                                            GLfloat lambda, GLfloat s,
                                            GLubyte *red, GLubyte *green,
                                            GLubyte *blue, GLubyte *alpha )
{
   GLint max = ctx->Texture.Current1D->Image[0]->MaxLog2;

   if (lambda>=max) {
      sample_1d_linear( ctx, ctx->Texture.Current1D->Image[max],
                        s, red, green, blue, alpha );
   }
   else {
      GLubyte red0, green0, blue0, alpha0;
      GLubyte red1, green1, blue1, alpha1;
      GLfloat f = frac(lambda);
      GLint level = (GLint) (lambda + 1.0F);
      level = CLAMP( level, 1, max );
      sample_1d_linear( ctx, ctx->Texture.Current1D->Image[level-1],
                        s, &red0, &green0, &blue0, &alpha0 );
      sample_1d_linear( ctx, ctx->Texture.Current1D->Image[level],
                        s, &red1, &green1, &blue1, &alpha1 );
      *red   = (1.0F-f)*red0   + f*red1;
      *green = (1.0F-f)*green0 + f*green1;
      *blue  = (1.0F-f)*blue0  + f*blue1;
      *alpha = (1.0F-f)*alpha0 + f*alpha1;
   }
}



/*
 * Given an (s) texture coordinate and lambda (level of detail) value,
 * return a texture sample.
 *
 */
static void sample_1d_texture( GLcontext *ctx,
                               GLfloat s, GLfloat lambda,
                               GLubyte *red, GLubyte *green, GLubyte *blue,
                               GLubyte *alpha, GLfloat c )
{
   GLint level;

   if (lambda>c) {
      /* minification */
      switch (ctx->Texture.Current1D->MinFilter) {
         case GL_NEAREST:
            level = 0;
            sample_1d_nearest( ctx, ctx->Texture.Current1D->Image[level],
                               s, red, green, blue, alpha );
            break;
         case GL_LINEAR:
            level = 0;
            sample_1d_linear( ctx, ctx->Texture.Current1D->Image[level],
                              s, red, green, blue, alpha );
            break;
         case GL_NEAREST_MIPMAP_NEAREST:
	    sample_1d_nearest_mipmap_nearest( ctx, lambda, s,
                                              red, green, blue, alpha );
            break;
         case GL_LINEAR_MIPMAP_NEAREST:
	    sample_1d_linear_mipmap_nearest( ctx, lambda, s,
                                             red, green, blue, alpha );
            break;
         case GL_NEAREST_MIPMAP_LINEAR:
	    sample_1d_nearest_mipmap_linear( ctx, lambda, s,
                                             red, green, blue, alpha );
            break;
         case GL_LINEAR_MIPMAP_LINEAR:
	    sample_1d_linear_mipmap_linear( ctx, lambda, s,
                                            red, green, blue, alpha );
            break;
         default:
            abort();
      }
   }
   else {
      /* magnification */
      switch (ctx->Texture.Current1D->MagFilter) {
         case GL_NEAREST:
            sample_1d_nearest( ctx, ctx->Texture.Current1D->Image[0],
                               s, red, green, blue, alpha );
            break;
         case GL_LINEAR:
            sample_1d_linear( ctx, ctx->Texture.Current1D->Image[0],
                              s, red, green, blue, alpha );
            break;
         default:
            abort();
      }
   }
}



/**********************************************************************/
/*                    2-D Texture Sampling Functions                  */
/**********************************************************************/


/*
 * Given a texture image and an (i,j) integer texel coordinate, return the
 * texel color.
 */
static void get_2d_texel( struct gl_texture_image *img, GLint i, GLint j,
                          GLubyte *red, GLubyte *green, GLubyte *blue,
                          GLubyte *alpha )
{
   GLint width = img->Width;    /* includes border */
   GLubyte *texel;

#ifdef DEBUG
   GLint height = img->Height;  /* includes border */
   if (i<0 || i>=width)  abort();
   if (j<0 || j>=height)  abort();
#endif

   switch (img->Format) {
      case GL_ALPHA:
         *alpha = img->Data[ width * j + i ];
         return;
      case GL_LUMINANCE:
      case GL_INTENSITY:
         *red   = img->Data[ width * j + i ];
         return;
      case GL_LUMINANCE_ALPHA:
         texel = img->Data + (width * j + i) * 2;
         *red   = texel[0];
         *alpha = texel[1];
         return;
      case GL_RGB:
         texel = img->Data + (width * j + i) * 3;
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         return;
      case GL_RGBA:
         texel = img->Data + (width * j + i) * 4;
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         *alpha = texel[3];
         return;
      default:
         abort();
   }
}



/*
 * Return the texture sample for coordinate (s,t) using GL_NEAREST filter.
 */
static void sample_2d_nearest( GLcontext *ctx,
                               struct gl_texture_image *img,
                               GLfloat s, GLfloat t,
                               GLubyte *red, GLubyte *green,
                               GLubyte *blue, GLubyte *alpha )
{
   GLint imgWidth = img->Width;  /* includes border */
   GLint width = img->Width2;    /* without border, power of two */
   GLint height = img->Height2;  /* without border, power of two */
   GLint i, j;
   GLubyte *texel;

   /* Clamp/Repeat S and convert to integer texel coordinate */
   if (ctx->Texture.Current2D->WrapS==GL_REPEAT) {
      /* s limited to [0,1) */
      /* i limited to [0,width-1] */
      i = (GLint) (s * width);
      i &= (width-1);
   }
   else {
      /* s limited to [0,1] */
      /* i limited to [0,width-1] */
      if (s<=0.0F)      i = 0;
      else if (s>1.0F)  i = width-1;
      else              i = (GLint) (s * width);
   }

   /* Clamp/Repeat T and convert to integer texel coordinate */
   if (ctx->Texture.Current2D->WrapT==GL_REPEAT) {
      /* t limited to [0,1) */
      /* j limited to [0,height-1] */
      j = (GLint) (t * height);
      j &= (height-1);
   }
   else {
      /* t limited to [0,1] */
      /* j limited to [0,height-1] */
      if (t<=0.0F)      j = 0;
      else if (t>1.0F)  j = height-1;
      else              j = (GLint) (t * height);
   }

   /* skip over the border, if any */
   i += img->Border;
   j += img->Border;

   switch (img->Format) {
      case GL_ALPHA:
         *alpha = img->Data[ j * imgWidth + i ];
         return;
      case GL_LUMINANCE:
      case GL_INTENSITY:
         *red   = img->Data[ j * imgWidth + i ];
         return;
      case GL_LUMINANCE_ALPHA:
         texel = img->Data + ((j * imgWidth + i) << 1);
         *red   = texel[0];
         *alpha = texel[1];
         return;
      case GL_RGB:
         texel = img->Data + (j * imgWidth + i) * 3;
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         return;
      case GL_RGBA:
         texel = img->Data + ((j * imgWidth + i) << 2);
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         *alpha = texel[3];
         return;
      default:
         abort();
   }
}



/*
 * Return the texture sample for coordinate (s,t) using GL_LINEAR filter.
 */
static void sample_2d_linear( GLcontext *ctx,
                              struct gl_texture_image *img,
                              GLfloat s, GLfloat t,
                              GLubyte *red, GLubyte *green,
                              GLubyte *blue, GLubyte *alpha )
{
   GLint width = img->Width2;
   GLint height = img->Height2;
   GLint i0, j0, i1, j1;
   GLint i0border, j0border, i1border, j1border;
   GLfloat u, v;

   u = s * width;
   if (ctx->Texture.Current2D->WrapS==GL_REPEAT) {
      i0 = ((GLint) floor(u - 0.5F)) % width;
      i1 = (i0 + 1) & (width-1);
      i0border = i1border = 0;
   }
   else {
      i0 = (GLint) floor(u - 0.5F);
      i1 = i0 + 1;
      i0border = (i0<0) | (i0>=width);
      i1border = (i1<0) | (i1>=width);
   }

   v = t * height;
   if (ctx->Texture.Current2D->WrapT==GL_REPEAT) {
      j0 = ((GLint) floor(v - 0.5F)) % height;
      j1 = (j0 + 1) & (height-1);
      j0border = j1border = 0;
   }
   else {
      j0 = (GLint) floor(v - 0.5F );
      j1 = j0 + 1;
      j0border = (j0<0) | (j0>=height);
      j1border = (j1<0) | (j1>=height);
   }

   if (img->Border) {
      i0 += img->Border;
      i1 += img->Border;
      j0 += img->Border;
      j1 += img->Border;
      i0border = i1border = 0;
      j0border = j1border = 0;
   }
   else {
      i0 &= (width-1);
      j0 &= (height-1);
   }

   {
      GLfloat a = frac(u - 0.5F);
      GLfloat b = frac(v - 0.5F);

      GLint w00 = (GLint) ((1.0F-a)*(1.0F-b) * 256.0F);
      GLint w10 = (GLint) (      a *(1.0F-b) * 256.0F);
      GLint w01 = (GLint) ((1.0F-a)*      b  * 256.0F);
      GLint w11 = (GLint) (      a *      b  * 256.0F);

      GLubyte red00, green00, blue00, alpha00;
      GLubyte red10, green10, blue10, alpha10;
      GLubyte red01, green01, blue01, alpha01;
      GLubyte red11, green11, blue11, alpha11;

      if (i0border | j0border) {
         red00   = ctx->Texture.Current2D->BorderColor[0];
         green00 = ctx->Texture.Current2D->BorderColor[1];
         blue00  = ctx->Texture.Current2D->BorderColor[2];
         alpha00 = ctx->Texture.Current2D->BorderColor[3];
      }
      else {
         get_2d_texel( img, i0, j0, &red00, &green00, &blue00, &alpha00 );
      }
      if (i1border | j0border) {
         red10   = ctx->Texture.Current2D->BorderColor[0];
         green10 = ctx->Texture.Current2D->BorderColor[1];
         blue10  = ctx->Texture.Current2D->BorderColor[2];
         alpha10 = ctx->Texture.Current2D->BorderColor[3];
      }
      else {
         get_2d_texel( img, i1, j0, &red10, &green10, &blue10, &alpha10 );
      }
      if (i0border | j1border) {
         red01   = ctx->Texture.Current2D->BorderColor[0];
         green01 = ctx->Texture.Current2D->BorderColor[1];
         blue01  = ctx->Texture.Current2D->BorderColor[2];
         alpha01 = ctx->Texture.Current2D->BorderColor[3];
      }
      else {
         get_2d_texel( img, i0, j1, &red01, &green01, &blue01, &alpha01 );
      }
      if (i1border | j1border) {
         red11   = ctx->Texture.Current2D->BorderColor[0];
         green11 = ctx->Texture.Current2D->BorderColor[1];
         blue11  = ctx->Texture.Current2D->BorderColor[2];
         alpha11 = ctx->Texture.Current2D->BorderColor[3];
      }
      else {
         get_2d_texel( img, i1, j1, &red11, &green11, &blue11, &alpha11 );
      }

      *red   = (w00*red00   + w10*red10   + w01*red01   + w11*red11  ) >> 8;
      *green = (w00*green00 + w10*green10 + w01*green01 + w11*green11) >> 8;
      *blue  = (w00*blue00  + w10*blue10  + w01*blue01  + w11*blue11 ) >> 8;
      *alpha = (w00*alpha00 + w10*alpha10 + w01*alpha01 + w11*alpha11) >> 8;
   }
}



static void sample_2d_nearest_mipmap_nearest( GLcontext *ctx,
                                              GLfloat lambda,
                                              GLfloat s, GLfloat t,
                                              GLubyte *red, GLubyte *green,
                                              GLubyte *blue, GLubyte *alpha )
{
   GLint level;
   if (lambda<=0.5F) {
      level = 0;
   }
   else {
      GLint max = ctx->Texture.Current2D->Image[0]->MaxLog2;
      level = (GLint) (lambda + 0.499999F);
      if (level>max) {
         level = max;
      }
   }
   sample_2d_nearest( ctx, ctx->Texture.Current2D->Image[level],
                      s, t, red, green, blue, alpha );
}



static void sample_2d_linear_mipmap_nearest( GLcontext *ctx,
                                             GLfloat lambda,
                                             GLfloat s, GLfloat t,
                                             GLubyte *red, GLubyte *green,
                                             GLubyte *blue, GLubyte *alpha )
{
   GLint level;
   if (lambda<=0.5F) {
      level = 0;
   }
   else {
      GLint max = ctx->Texture.Current2D->Image[0]->MaxLog2;
      level = (GLint) (lambda + 0.499999F);
      if (level>max) {
         level = max;
      }
   }
   sample_2d_linear( ctx, ctx->Texture.Current2D->Image[level],
                     s, t, red, green, blue, alpha );
}



static void sample_2d_nearest_mipmap_linear( GLcontext *ctx,
                                             GLfloat lambda,
                                             GLfloat s, GLfloat t,
                                             GLubyte *red, GLubyte *green,
                                             GLubyte *blue, GLubyte *alpha )
{
   GLint max = ctx->Texture.Current2D->Image[0]->MaxLog2;

   if (lambda>=max) {
      sample_2d_nearest( ctx, ctx->Texture.Current2D->Image[max],
                         s, t, red, green, blue, alpha );
   }
   else {
      GLubyte red0, green0, blue0, alpha0;
      GLubyte red1, green1, blue1, alpha1;
      GLfloat f = frac(lambda);
      GLint level = (GLint) (lambda + 1.0F);
      level = CLAMP( level, 1, max );
      sample_2d_nearest( ctx, ctx->Texture.Current2D->Image[level-1], s, t,
                         &red0, &green0, &blue0, &alpha0 );
      sample_2d_nearest( ctx, ctx->Texture.Current2D->Image[level], s, t,
                         &red1, &green1, &blue1, &alpha1 );
      *red   = (1.0F-f)*red0   + f*red1;
      *green = (1.0F-f)*green0 + f*green1;
      *blue  = (1.0F-f)*blue0  + f*blue1;
      *alpha = (1.0F-f)*alpha0 + f*alpha1;
   }
}



static void sample_2d_linear_mipmap_linear( GLcontext *ctx,
                                            GLfloat lambda,
                                            GLfloat s, GLfloat t,
                                            GLubyte *red, GLubyte *green,
                                            GLubyte *blue, GLubyte *alpha )
{
   GLint max = ctx->Texture.Current2D->Image[0]->MaxLog2;

   if (lambda>=max) {
      sample_2d_linear( ctx, ctx->Texture.Current2D->Image[max],
                         s, t, red, green, blue, alpha );
   }
   else {
      GLubyte red0, green0, blue0, alpha0;
      GLubyte red1, green1, blue1, alpha1;
      GLfloat f = frac(lambda);
      GLint level = (GLint) (lambda + 1.0F);
      level = CLAMP( level, 1, max );
      sample_2d_linear( ctx, ctx->Texture.Current2D->Image[level-1], s, t,
                         &red0, &green0, &blue0, &alpha0 );
      sample_2d_linear( ctx, ctx->Texture.Current2D->Image[level], s, t,
                         &red1, &green1, &blue1, &alpha1 );
      *red   = (1.0F-f)*red0   + f*red1;
      *green = (1.0F-f)*green0 + f*green1;
      *blue  = (1.0F-f)*blue0  + f*blue1;
      *alpha = (1.0F-f)*alpha0 + f*alpha1;
   }
}




/*
 * Given an (s,t) texture coordinate and lambda (level of detail) value,
 * return a texture sample.
 */
static void sample_2d_texture( GLcontext *ctx,
                               GLfloat s, GLfloat t, GLfloat lambda,
                               GLubyte *red, GLubyte *green, GLubyte *blue,
                               GLubyte *alpha, GLfloat c )
{
   if (lambda>c) {
      /* minification */
      switch (ctx->Texture.Current2D->MinFilter) {
         case GL_NEAREST:
            sample_2d_nearest( ctx, ctx->Texture.Current2D->Image[0],
                               s, t, red, green, blue, alpha );
            break;
         case GL_LINEAR:
            sample_2d_linear( ctx, ctx->Texture.Current2D->Image[0],
                              s, t, red, green, blue, alpha );
            break;
         case GL_NEAREST_MIPMAP_NEAREST:
            sample_2d_nearest_mipmap_nearest( ctx, lambda, s, t,
                                              red, green, blue, alpha );
            break;
         case GL_LINEAR_MIPMAP_NEAREST:
            sample_2d_linear_mipmap_nearest( ctx, lambda, s, t,
                                             red, green, blue, alpha );
            break;
         case GL_NEAREST_MIPMAP_LINEAR:
            sample_2d_nearest_mipmap_linear( ctx, lambda, s, t,
                                             red, green, blue, alpha );
            break;
         case GL_LINEAR_MIPMAP_LINEAR:
            sample_2d_linear_mipmap_linear( ctx, lambda, s, t,
                                            red, green, blue, alpha );
            break;
         default:
            abort();
      }
   }
   else {
      /* magnification */
      switch (ctx->Texture.Current2D->MagFilter) {
         case GL_NEAREST:
            sample_2d_nearest( ctx, ctx->Texture.Current2D->Image[0],
                               s, t, red, green, blue, alpha );
            break;
         case GL_LINEAR:
            sample_2d_linear( ctx, ctx->Texture.Current2D->Image[0],
                              s, t, red, green, blue, alpha );
            break;
         default:
            abort();
      }
   }
}



/**********************************************************************/
/*                    3-D Texture Sampling Functions                  */
/**********************************************************************/

/*
 * Given a texture image and an (i,j,k) integer texel coordinate, return the
 * texel color.
 */
static void get_3d_texel( struct gl_texture_image *img,
                          GLint i, GLint j, GLint k,
                          GLubyte *red, GLubyte *green, GLubyte *blue,
                          GLubyte *alpha )
{
   GLint width = img->Width;    /* includes border */
   GLint height = img->Height;  /* includes border */
   GLint depth = img->Depth;    /* includes border */
   GLint rectarea;              /* = width * heigth */
   GLubyte *texel;

   rectarea = width*height;

#ifdef DEBUG
   if (i<0 || i>=width)  abort();
   if (j<0 || j>=height)  abort();
   if (k<0 || k>=depth)  abort();
#endif

   switch (img->Format) {
      case GL_ALPHA:
         *alpha = img->Data[ rectarea * k +  width * j + i ];
         return;
      case GL_LUMINANCE:
      case GL_INTENSITY:
         *red   = img->Data[ rectarea * k +  width * j + i ];
         return;
      case GL_LUMINANCE_ALPHA:
         texel = img->Data + ( rectarea * k + width * j + i) * 2;
         *red   = texel[0];
         *alpha = texel[1];
         return;
      case GL_RGB:
         texel = img->Data + (rectarea * k + width * j + i) * 3;
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         return;
      case GL_RGBA:
         texel = img->Data + (rectarea * k + width * j + i) * 4;
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         *alpha = texel[3];
         return;
      default:
         abort();
   }
}


/*
 * Return the texture sample for coordinate (s,t,r) using GL_NEAREST filter.
 */
static void sample_3d_nearest( GLcontext *ctx,
                               struct gl_texture_image *img,
                               GLfloat s, GLfloat t, GLfloat r,
                               GLubyte *red, GLubyte *green,
                               GLubyte *blue, GLubyte *alpha )
{
   GLint imgWidth = img->Width;   /* includes border, if any */
   GLint imgHeight = img->Height; /* includes border, if any */
   GLint width = img->Width2;     /* without border, power of two */
   GLint height = img->Height2;   /* without border, power of two */
   GLint depth = img->Depth2;     /* without border, power of two */
   GLint rectarea;                /* = width * height */
   GLint i, j, k;
   GLubyte *texel;

   rectarea = imgWidth * imgHeight;

   /* Clamp/Repeat S and convert to integer texel coordinate */
   if (ctx->Texture.Current3D->WrapS==GL_REPEAT) {
      /* s limited to [0,1) */
      /* i limited to [0,width-1] */
      i = (GLint) (s * width);
      i &= (width-1);
   }
   else {
      /* s limited to [0,1] */
      /* i limited to [0,width-1] */
      if (s<0.0F)       i = 0;
      else if (s>1.0F)  i = width-1;
      else              i = (GLint) (s * width);
   }

   /* Clamp/Repeat T and convert to integer texel coordinate */
   if (ctx->Texture.Current3D->WrapT==GL_REPEAT) {
      /* t limited to [0,1) */
      /* j limited to [0,height-1] */
      j = (GLint) (t * height);
      j &= (height-1);
   }
   else {
      /* t limited to [0,1] */
      /* j limited to [0,height-1] */
      if (t<0.0F)       j = 0;
      else if (t>1.0F)  j = height-1;
      else              j = (GLint) (t * height);
   }

   /* Clamp/Repeat R and convert to integer texel coordinate */
   if (ctx->Texture.Current3D->WrapR==GL_REPEAT) {
      /* r limited to [0,1) */
      /* k limited to [0,depth-1] */
      k = (GLint) (r * depth);
      k &= (depth-1);
   }
   else {
      /* r limited to [0,1] */
      /* k limited to [0,depth-1] */
      if (r<0.0F)       k = 0;
      else if (r>1.0F)  k = depth-1;
      else              k = (GLint) (r * depth);
   }

   switch (img->Format) {
      case GL_ALPHA:
         *alpha = img->Data[ rectarea * k + j * imgWidth + i ];
         return;
      case GL_LUMINANCE:
      case GL_INTENSITY:
         *red   = img->Data[ rectarea * k + j * imgWidth + i ];
         return;
      case GL_LUMINANCE_ALPHA:
         texel  = img->Data + ((rectarea * k + j * imgWidth + i) << 1);
         *red   = texel[0];
         *alpha = texel[1];
         return;
      case GL_RGB:
         texel = img->Data + ( rectarea * k + j * imgWidth + i) * 3;
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         return;
      case GL_RGBA:
         texel = img->Data + ((rectarea * k + j * imgWidth + i) << 2);
         *red   = texel[0];
         *green = texel[1];
         *blue  = texel[2];
         *alpha = texel[3];
         return;
      default:
         abort();
   }
}


/*
 * Return the texture sample for coordinate (s,t,r) using GL_LINEAR filter.
 */
static void sample_3d_linear( GLcontext *ctx,
                              struct gl_texture_image *img,
                              GLfloat s, GLfloat t, GLfloat r,
                              GLubyte *red, GLubyte *green,
                              GLubyte *blue, GLubyte *alpha )
{
   GLint width = img->Width2;
   GLint height = img->Height2;
   GLint depth = img->Depth2;
   GLint i0, j0, k0, i1, j1, k1;
   GLint i0border, j0border, k0border, i1border, j1border, k1border;
   GLfloat u, v, w;

   u = s * width;
   if (ctx->Texture.Current3D->WrapS==GL_REPEAT) {
      i0 = ((GLint) floor(u - 0.5F)) % width;
      i1 = (i0 + 1) & (width-1);
      i0border = i1border = 0;
   }
   else {
      i0 = (GLint) floor(u - 0.5F);
      i1 = i0 + 1;
      i0border = (i0<0) | (i0>=width);
      i1border = (i1<0) | (i1>=width);
   }

   v = t * height;
   if (ctx->Texture.Current3D->WrapT==GL_REPEAT) {
      j0 = ((GLint) floor(v - 0.5F)) % height;
      j1 = (j0 + 1) & (height-1);
      j0border = j1border = 0;
   }
   else {
      j0 = (GLint) floor(v - 0.5F);
      j1 = j0 + 1;
      j0border = (j0<0) | (j0>=height);
      j1border = (j1<0) | (j1>=height);
   }

   w = r * depth;
   if (ctx->Texture.Current3D->WrapR==GL_REPEAT) {
      k0 = ((GLint) floor(w - 0.5F)) % depth;
      k1 = (k0 + 1) & (depth-1);
      k0border = k1border = 0;
   }
   else {
      k0 = (GLint) floor(v - 0.5F);
      k1 = k0 + 1;
      k0border = (k0<0) | (k0>=depth);
      k1border = (k1<0) | (k1>=depth);
   }

   if (img->Border) {
      i0 += img->Border;
      i1 += img->Border;
      j0 += img->Border;
      j1 += img->Border;
      k0 += img->Border;
      k1 += img->Border;
      i0border = i1border = 0;
      j0border = j1border = 0;
      k0border = k1border = 0;
   }
   else {
      i0 &= (width-1);
      j0 &= (height-1);
      k0 &= (depth-1);
   }

   {
      GLfloat a = frac(u - 0.5F);
      GLfloat b = frac(v - 0.5F);
      GLfloat c = frac(w - 0.5F);

      GLint w000 = (GLint) ((1.0F-a)*(1.0F-b) * (1.0F-c) * 256.0F);
      GLint w010 = (GLint) (      a *(1.0F-b) * (1.0F-c) * 256.0F);
      GLint w001 = (GLint) ((1.0F-a)*      b  * (1.0F-c) * 256.0F);
      GLint w011 = (GLint) (      a *      b  * (1.0F-c) * 256.0F);
      GLint w100 = (GLint) ((1.0F-a)*(1.0F-b) * c * 256.0F);
      GLint w110 = (GLint) (      a *(1.0F-b) * c * 256.0F);
      GLint w101 = (GLint) ((1.0F-a)*      b  * c * 256.0F);
      GLint w111 = (GLint) (      a *      b  * c * 256.0F);


      GLubyte red000, green000, blue000, alpha000;
      GLubyte red010, green010, blue010, alpha010;
      GLubyte red001, green001, blue001, alpha001;
      GLubyte red011, green011, blue011, alpha011;
      GLubyte red100, green100, blue100, alpha100;
      GLubyte red110, green110, blue110, alpha110;
      GLubyte red101, green101, blue101, alpha101;
      GLubyte red111, green111, blue111, alpha111;

      if (k0border | i0border | j0border ) {
         red000   = ctx->Texture.Current3D->BorderColor[0];
         green000 = ctx->Texture.Current3D->BorderColor[1];
         blue000  = ctx->Texture.Current3D->BorderColor[2];
         alpha000 = ctx->Texture.Current3D->BorderColor[3];
      }
      else {
         get_3d_texel( img, i0, j0, k0, &red000, &green000, &blue000, &alpha000 );
      }
      if (k0border | i1border | j0border) {
         red010   = ctx->Texture.Current3D->BorderColor[0];
         green010 = ctx->Texture.Current3D->BorderColor[1];
         blue010  = ctx->Texture.Current3D->BorderColor[2];
         alpha010 = ctx->Texture.Current3D->BorderColor[3];
      }
      else {
         get_3d_texel( img, i1, j0, k0, &red010, &green010, &blue010, &alpha010 );
      }
      if (k0border | i0border | j1border) {
         red001   = ctx->Texture.Current3D->BorderColor[0];
         green001 = ctx->Texture.Current3D->BorderColor[1];
         blue001  = ctx->Texture.Current3D->BorderColor[2];
         alpha001 = ctx->Texture.Current3D->BorderColor[3];
      }
      else {
         get_3d_texel( img, i0, j1, k0, &red001, &green001, &blue001, &alpha001 );
      }
      if (k0border | i1border | j1border) {
         red011   = ctx->Texture.Current3D->BorderColor[0];
         green011 = ctx->Texture.Current3D->BorderColor[1];
         blue011  = ctx->Texture.Current3D->BorderColor[2];
         alpha011 = ctx->Texture.Current3D->BorderColor[3];
      }
      else {
         get_3d_texel( img, i1, j1, k0, &red011, &green011, &blue011, &alpha011 );
      }

      if (k1border | i0border | j0border ) {
         red100   = ctx->Texture.Current3D->BorderColor[0];
         green100 = ctx->Texture.Current3D->BorderColor[1];
         blue100  = ctx->Texture.Current3D->BorderColor[2];
         alpha100 = ctx->Texture.Current3D->BorderColor[3];
      }
      else {
         get_3d_texel( img, i0, j0, k1, &red100, &green100, &blue100, &alpha100 );
      }
      if (k1border | i1border | j0border) {
         red110   = ctx->Texture.Current3D->BorderColor[0];
         green110 = ctx->Texture.Current3D->BorderColor[1];
         blue110  = ctx->Texture.Current3D->BorderColor[2];
         alpha110 = ctx->Texture.Current3D->BorderColor[3];
      }
      else {
         get_3d_texel( img, i1, j0, k1, &red110, &green110, &blue110, &alpha110 );
      }
      if (k1border | i0border | j1border) {
         red101   = ctx->Texture.Current3D->BorderColor[0];
         green101 = ctx->Texture.Current3D->BorderColor[1];
         blue101  = ctx->Texture.Current3D->BorderColor[2];
         alpha101 = ctx->Texture.Current3D->BorderColor[3];
      }
      else {
         get_3d_texel( img, i0, j1, k1, &red101, &green101, &blue101, &alpha101 );
      }
      if (k1border | i1border | j1border) {
         red111   = ctx->Texture.Current3D->BorderColor[0];
         green111 = ctx->Texture.Current3D->BorderColor[1];
         blue111  = ctx->Texture.Current3D->BorderColor[2];
         alpha111 = ctx->Texture.Current3D->BorderColor[3];
      }
      else {
         get_3d_texel( img, i1, j1, k1, &red111, &green111, &blue111, &alpha111 );
      }

      *red   = (w000*red000   + w010*red010   + w001*red001   + w011*red011 +
                w100*red100   + w110*red110   + w101*red101   + w111*red111  )
                >> 8;
      *green = (w000*green000 + w010*green010 + w001*green001 + w011*green011 +
                w100*green100 + w110*green110 + w101*green101 + w111*green111 )
                >> 8;
      *blue  = (w000*blue000  + w010*blue010  + w001*blue001  + w011*blue011 +
                w100*blue100  + w110*blue110  + w101*blue101  + w111*blue111 )
                >> 8;
      *alpha = (w000*alpha000 + w010*alpha010 + w001*alpha001 + w011*alpha011 +
                w100*alpha100 + w110*alpha110 + w101*alpha101 + w111*alpha111 )
                >> 8;
   }
}


static void sample_3d_nearest_mipmap_nearest( GLcontext *ctx,
                                              GLfloat lambda,
                                              GLfloat s, GLfloat t, GLfloat r,
                                              GLubyte *red, GLubyte *green,
                                              GLubyte *blue, GLubyte *alpha )
{
   GLint level;
   if (lambda<=0.5F) {
      level = 0;
   }
   else {
      GLint widthlog2 = ctx->Texture.Current3D->Image[0]->WidthLog2;
      level = (GLint) (lambda + 0.499999F);
      if (level>widthlog2 ) {
         level = widthlog2;
      }
   }
   sample_3d_nearest( ctx, ctx->Texture.Current3D->Image[level],
                      s, t, r, red, green, blue, alpha );
}


static void sample_3d_linear_mipmap_nearest( GLcontext *ctx,
                                             GLfloat lambda,
                                             GLfloat s, GLfloat t, GLfloat r,
                                             GLubyte *red, GLubyte *green,
                                             GLubyte *blue, GLubyte *alpha )
{
   GLint level;
   if (lambda<=0.5F) {
      level = 0;
   }
   else {
      GLint widthlog2 = ctx->Texture.Current3D->Image[0]->WidthLog2;
      level = (GLint) (lambda + 0.499999F);
      if (level>widthlog2 ) {
         level = widthlog2;
      }
   }
   sample_3d_linear( ctx, ctx->Texture.Current3D->Image[level],
                     s, t, r, red, green, blue, alpha );
}


static void sample_3d_nearest_mipmap_linear( GLcontext *ctx,
                                             GLfloat lambda,
                                             GLfloat s, GLfloat t, GLfloat r,
                                             GLubyte *red, GLubyte *green,
                                             GLubyte *blue, GLubyte *alpha )
{
   GLint max = ctx->Texture.Current3D->Image[0]->MaxLog2;

   if (lambda>=max) {
      sample_3d_nearest( ctx, ctx->Texture.Current3D->Image[max],
                         s, t, r, red, green, blue, alpha );
   }
   else {
      GLubyte red0, green0, blue0, alpha0;
      GLubyte red1, green1, blue1, alpha1;
      GLfloat f = frac(lambda);
      GLint level = (GLint) (lambda + 1.0F);
      level = CLAMP( level, 1, max );
      sample_3d_nearest( ctx, ctx->Texture.Current3D->Image[level-1], s, t, r,
                         &red0, &green0, &blue0, &alpha0 );
      sample_3d_nearest( ctx, ctx->Texture.Current3D->Image[level], s, t, r,
                         &red1, &green1, &blue1, &alpha1 );
      *red   = (1.0F-f)*red1   + f*red0;
      *green = (1.0F-f)*green1 + f*green0;
      *blue  = (1.0F-f)*blue1  + f*blue0;
      *alpha = (1.0F-f)*alpha1 + f*alpha0;
   }
}


static void sample_3d_linear_mipmap_linear( GLcontext *ctx,
                                            GLfloat lambda,
                                            GLfloat s, GLfloat t, GLfloat r,
                                            GLubyte *red, GLubyte *green,
                                            GLubyte *blue, GLubyte *alpha )
{
   GLint max = ctx->Texture.Current3D->Image[0]->MaxLog2;

   if (lambda>=max) {
      sample_3d_linear( ctx, ctx->Texture.Current3D->Image[max],
                         s, t, r, red, green, blue, alpha );
   }
   else {
      GLubyte red0, green0, blue0, alpha0;
      GLubyte red1, green1, blue1, alpha1;
      GLfloat f = frac(lambda);
      GLint level = (GLint) (lambda + 1.0F);
      level = CLAMP( level, 1, max );
      sample_3d_linear( ctx, ctx->Texture.Current3D->Image[level-1], s, t, r,
                         &red0, &green0, &blue0, &alpha0 );
      sample_3d_linear( ctx, ctx->Texture.Current3D->Image[level], s, t, r,
                         &red1, &green1, &blue1, &alpha1 );
      *red   = (1.0F-f)*red1   + f*red0;
      *green = (1.0F-f)*green1 + f*green0;
      *blue  = (1.0F-f)*blue1  + f*blue0;
      *alpha = (1.0F-f)*alpha1 + f*alpha0;
   }
}


/*
 * Given an (s,t,r) texture coordinate and lambda (level of detail) value,
 * return a texture sample.
 */
static void sample_3d_texture( GLcontext *ctx,
                               GLfloat s, GLfloat t, GLfloat r, GLfloat lambda,
                               GLubyte *red, GLubyte *green, GLubyte *blue,
                               GLubyte *alpha, GLfloat c )
{
   if (lambda>c) {
      /* minification */
      switch (ctx->Texture.Current3D->MinFilter) {
         case GL_NEAREST:
            sample_3d_nearest( ctx, ctx->Texture.Current3D->Image[0],
                               s, t, r, red, green, blue, alpha );
            break;
         case GL_LINEAR:
            sample_3d_linear( ctx, ctx->Texture.Current3D->Image[0],
                              s, t, r, red, green, blue, alpha );
            break;
         case GL_NEAREST_MIPMAP_NEAREST:
            sample_3d_nearest_mipmap_nearest( ctx, lambda, s, t, r,
                                              red, green, blue, alpha );
            break;
         case GL_LINEAR_MIPMAP_NEAREST:
            sample_3d_linear_mipmap_nearest( ctx, lambda, s, t, r,
                                             red, green, blue, alpha );
            break;
         case GL_NEAREST_MIPMAP_LINEAR:
            sample_3d_nearest_mipmap_linear( ctx, lambda, s, t, r,
                                             red, green, blue, alpha );
            break;
         case GL_LINEAR_MIPMAP_LINEAR:
            sample_3d_linear_mipmap_linear( ctx, lambda, s, t, r,
                                            red, green, blue, alpha );
            break;
         default:
            abort();
      }
   }
   else {
      /* magnification */
      switch (ctx->Texture.Current3D->MagFilter) {
         case GL_NEAREST:
            sample_3d_nearest( ctx, ctx->Texture.Current3D->Image[0],
                               s, t, r, red, green, blue, alpha );
            break;
         case GL_LINEAR:
            sample_3d_linear( ctx, ctx->Texture.Current3D->Image[0],
                              s, t, r, red, green, blue, alpha );
            break;
         default:
            abort();
      }
   }
}



/**********************************************************************/
/*                      Texture Application                           */
/**********************************************************************/



/*
 * Combine incoming fragment color with texel color to produce output color.
 * Input:  n - number of fragments
 *         format - base internal texture format
 *         env_mode - texture environment mode
 *         Rt, Gt, Bt, At - array of texel colors
 * InOut:  red, green, blue, alpha - incoming fragment colors modified
 *                                   by texel colors according to the
 *                                   texture environment mode.
 */
static void apply_texture( GLcontext *ctx,
         GLuint n, GLint format, GLenum env_mode,
	 GLubyte red[], GLubyte green[], GLubyte blue[], GLubyte alpha[],
	 GLubyte Rt[], GLubyte Gt[], GLubyte Bt[], GLubyte At[] )
{
   GLuint i;
   GLint Rc, Gc, Bc, Ac;

   if (!ctx->Visual->EightBitColor) {
      /* This is a hack!  Rescale input colors from [0,scale] to [0,255]. */
      GLfloat rscale = 255.0 * ctx->Visual->InvRedScale;
      GLfloat gscale = 255.0 * ctx->Visual->InvGreenScale;
      GLfloat bscale = 255.0 * ctx->Visual->InvBlueScale;
      GLfloat ascale = 255.0 * ctx->Visual->InvAlphaScale;
      for (i=0;i<n;i++) {
	 red[i]   = (GLint) (red[i]   * rscale);
	 green[i] = (GLint) (green[i] * gscale);
	 blue[i]  = (GLint) (blue[i]  * bscale);
	 alpha[i] = (GLint) (alpha[i] * ascale);
      }
   }

/*
 * Use (A*(B+1)) >> 8 as a fast approximation of (A*B)/255 for A
 * and B in [0,255]
 */
#define PROD(A,B)   (((GLint)(A) * (GLint)(B)+1) >> 8)

   switch (env_mode) {
      case GL_REPLACE:
	 switch (format) {
	    case GL_ALPHA:
	       for (i=0;i<n;i++) {
		  /* Cv = Cf */
                  /* Av = At */
                  alpha[i] = At[i];
	       }
	       break;
	    case GL_LUMINANCE:
	       for (i=0;i<n;i++) {
		  /* Cv = Lt */
                  GLint Lt = Rt[i];
                  red[i] = green[i] = blue[i] = Lt;
                  /* Av = Af */
	       }
	       break;
	    case GL_LUMINANCE_ALPHA:
	       for (i=0;i<n;i++) {
                  GLint Lt = Rt[i];
		  /* Cv = Lt */
		  red[i] = green[i] = blue[i] = Lt;
		  /* Av = At */
		  alpha[i] = At[i];
	       }
	       break;
	    case GL_INTENSITY:
	       for (i=0;i<n;i++) {
		  /* Cv = It */
                  GLint It = Rt[i];
                  red[i] = green[i] = blue[i] = It;
                  /* Av = It */
                  alpha[i] = It;
	       }
	       break;
	    case GL_RGB:
	       for (i=0;i<n;i++) {
		  /* Cv = Ct */
		  red[i]   = Rt[i];
		  green[i] = Gt[i];
		  blue[i]  = Bt[i];
		  /* Av = Af */
	       }
	       break;
	    case GL_RGBA:
	       for (i=0;i<n;i++) {
		  /* Cv = Ct */
		  red[i]   = Rt[i];
		  green[i] = Gt[i];
		  blue[i]  = Bt[i];
		  /* Av = At */
		  alpha[i] = At[i];
	       }
	       break;
            default:
               abort();
	 }
	 break;

      case GL_MODULATE:
         switch (format) {
	    case GL_ALPHA:
	       for (i=0;i<n;i++) {
		  /* Cv = Cf */
		  /* Av = AfAt */
		  alpha[i] = PROD( alpha[i], At[i] );
	       }
	       break;
	    case GL_LUMINANCE:
	       for (i=0;i<n;i++) {
		  /* Cv = LtCf */
                  GLint Lt = Rt[i];
		  red[i]   = PROD( red[i],   Lt );
		  green[i] = PROD( green[i], Lt );
		  blue[i]  = PROD( blue[i],  Lt );
		  /* Av = Af */
	       }
	       break;
	    case GL_LUMINANCE_ALPHA:
	       for (i=0;i<n;i++) {
		  /* Cv = CfLt */
                  GLint Lt = Rt[i];
		  red[i]   = PROD( red[i],   Lt );
		  green[i] = PROD( green[i], Lt );
		  blue[i]  = PROD( blue[i],  Lt );
		  /* Av = AfAt */
		  alpha[i] = PROD( alpha[i], At[i] );
	       }
	       break;
	    case GL_INTENSITY:
	       for (i=0;i<n;i++) {
		  /* Cv = CfIt */
                  GLint It = Rt[i];
		  red[i]   = PROD( red[i],   It );
		  green[i] = PROD( green[i], It );
		  blue[i]  = PROD( blue[i],  It );
		  /* Av = AfIt */
		  alpha[i] = PROD( alpha[i], It );
	       }
	       break;
	    case GL_RGB:
	       for (i=0;i<n;i++) {
		  /* Cv = CfCt */
		  red[i]   = PROD( red[i],   Rt[i] );
		  green[i] = PROD( green[i], Gt[i] );
		  blue[i]  = PROD( blue[i],  Bt[i] );
		  /* Av = Af */
	       }
	       break;
	    case GL_RGBA:
	       for (i=0;i<n;i++) {
		  /* Cv = CfCt */
		  red[i]   = PROD( red[i],   Rt[i] );
		  green[i] = PROD( green[i], Gt[i] );
		  blue[i]  = PROD( blue[i],  Bt[i] );
		  /* Av = AfAt */
		  alpha[i] = PROD( alpha[i], At[i] );
	       }
	       break;
            default:
               abort();
	 }
	 break;

      case GL_DECAL:
         switch (format) {
            case GL_ALPHA:
            case GL_LUMINANCE:
            case GL_LUMINANCE_ALPHA:
            case GL_INTENSITY:
               /* undefined */
               break;
	    case GL_RGB:
	       for (i=0;i<n;i++) {
		  /* Cv = Ct */
		  red[i]   = Rt[i];
		  green[i] = Gt[i];
		  blue[i]  = Bt[i];
		  /* Av = Af */
	       }
	       break;
	    case GL_RGBA:
	       for (i=0;i<n;i++) {
		  /* Cv = Cf(1-At) + CtAt */
		  GLint t = At[i], s = 255 - t;
		  red[i]   = PROD(red[i],  s) + PROD(Rt[i],t);
		  green[i] = PROD(green[i],s) + PROD(Gt[i],t);
		  blue[i]  = PROD(blue[i], s) + PROD(Bt[i],t);
		  /* Av = Af */
	       }
	       break;
            default:
               abort();
	 }
	 break;

      case GL_BLEND:
         Rc = (GLint) (ctx->Texture.EnvColor[0] * 255.0F);
         Gc = (GLint) (ctx->Texture.EnvColor[1] * 255.0F);
         Bc = (GLint) (ctx->Texture.EnvColor[2] * 255.0F);
         Ac = (GLint) (ctx->Texture.EnvColor[2] * 255.0F);
	 switch (format) {
	    case GL_ALPHA:
	       for (i=0;i<n;i++) {
		  /* Cv = Cf */
		  /* Av = AfAt */
                  alpha[i] = PROD(alpha[i], At[i]);
	       }
	       break;
            case GL_LUMINANCE:
	       for (i=0;i<n;i++) {
		  /* Cv = Cf(1-Lt) + CcLt */
		  GLint Lt = Rt[i], s = 255 - Lt;
		  red[i]   = PROD(red[i],  s) + PROD(Rc,  Lt);
		  green[i] = PROD(green[i],s) + PROD(Gc,Lt);
		  blue[i]  = PROD(blue[i], s) + PROD(Bc, Lt);
		  /* Av = Af */
	       }
	       break;
	    case GL_LUMINANCE_ALPHA:
	       for (i=0;i<n;i++) {
		  /* Cv = Cf(1-Lt) + CcLt */
		  GLint Lt = Rt[i], s = 255 - Lt;
		  red[i]   = PROD(red[i],  s) + PROD(Rc,  Lt);
		  green[i] = PROD(green[i],s) + PROD(Gc,Lt);
		  blue[i]  = PROD(blue[i], s) + PROD(Bc, Lt);
		  /* Av = AfAt */
		  alpha[i] = PROD(alpha[i],At[i]);
	       }
	       break;
            case GL_INTENSITY:
	       for (i=0;i<n;i++) {
		  /* Cv = Cf(1-It) + CcLt */
		  GLint It = Rt[i], s = 255 - It;
		  red[i]   = PROD(red[i],  s) + PROD(Rc,It);
		  green[i] = PROD(green[i],s) + PROD(Gc,It);
		  blue[i]  = PROD(blue[i], s) + PROD(Bc,It);
                  /* Av = Af(1-It) + Ac*It */
                  alpha[i] = PROD(alpha[i],s) + PROD(Ac,It);
               }
               break;
	    case GL_RGB:
	       for (i=0;i<n;i++) {
		  /* Cv = Cf(1-Ct) + CcCt */
		  red[i]   = PROD(red[i],  (255-Rt[i])) + PROD(Rc,Rt[i]);
		  green[i] = PROD(green[i],(255-Gt[i])) + PROD(Gc,Gt[i]);
		  blue[i]  = PROD(blue[i], (255-Bt[i])) + PROD(Bc,Bt[i]);
		  /* Av = Af */
	       }
	       break;
	    case GL_RGBA:
	       for (i=0;i<n;i++) {
		  /* Cv = Cf(1-Ct) + CcCt */
		  red[i]   = PROD(red[i],  (255-Rt[i])) + PROD(Rc,Rt[i]);
		  green[i] = PROD(green[i],(255-Gt[i])) + PROD(Gc,Gt[i]);
		  blue[i]  = PROD(blue[i], (255-Bt[i])) + PROD(Bc,Bt[i]);
		  /* Av = AfAt */
		  alpha[i] = PROD(alpha[i],At[i]);
	       }
	       break;
	 }
	 break;

      default:
         abort();
   }
#undef PROD

   if (!ctx->Visual->EightBitColor) {
      /* This is a hack!  Rescale input colors from [0,255] to [0,scale]. */
      GLfloat rscale = ctx->Visual->RedScale   * (1.0F/ 255.0F);
      GLfloat gscale = ctx->Visual->GreenScale * (1.0F/ 255.0F);
      GLfloat bscale = ctx->Visual->BlueScale  * (1.0F/ 255.0F);
      GLfloat ascale = ctx->Visual->AlphaScale * (1.0F/ 255.0F);
      for (i=0;i<n;i++) {
	 red[i]   = (GLint) (red[i]   * rscale);
	 green[i] = (GLint) (green[i] * gscale);
	 blue[i]  = (GLint) (blue[i]  * bscale);
	 alpha[i] = (GLint) (alpha[i] * ascale);
      }
   }
}



/*
 * Given an array of fragment colors and texture coordinates, apply
 * 1-D texturing to the fragments.
 * Input:  n - number of fragments
 *         s - array of texture coordinate s values
 *         lambda - array of lambda values
 * InOut:  red, green, blue, alpha - incoming and modifed fragment colors
 */
void gl_texture_pixels_1d( GLcontext *ctx,
                           GLuint n, GLfloat s[], GLfloat lambda[],
			   GLubyte red[], GLubyte green[],
			   GLubyte blue[], GLubyte alpha[] )
{
   GLubyte tred[PB_SIZE];
   GLubyte tgreen[PB_SIZE];
   GLubyte tblue[PB_SIZE];
   GLubyte talpha[PB_SIZE];
   GLuint  i;
   GLfloat c;

   if (!ctx->Texture.Current1D->Complete) {
      return;
   }

   /* Compute c, the min/mag filter threshold. */
   if (ctx->Texture.Current1D->MagFilter==GL_LINEAR
       && (ctx->Texture.Current1D->MinFilter==GL_NEAREST_MIPMAP_NEAREST ||
           ctx->Texture.Current1D->MinFilter==GL_LINEAR_MIPMAP_NEAREST)) {
      c = 0.5F;
   }
   else {
      c = 0.0F;
   }

   /* Sample the texture. */
   if (lambda) {
      /* minfilter!=magfilter or using mipmaps */
      for (i=0;i<n;i++)
	 sample_1d_texture( ctx, s[i],lambda[i],
                            &tred[i],&tgreen[i],&tblue[i],&talpha[i],c);
   } 
   else {
      /* use same filter for minification and magnification */
      if (ctx->Texture.Current1D->MagFilter==GL_NEAREST) {
         struct gl_texture_image *img = ctx->Texture.Current1D->Image[0];
         for (i=0;i<n;i++) {
            sample_1d_nearest( ctx, img, s[i],
                               &tred[i], &tgreen[i], &tblue[i], &talpha[i] );
         }
      }
      else {
         /* mag filter must be GL_LINEAR */
         struct gl_texture_image *img = ctx->Texture.Current1D->Image[0];
         for (i=0;i<n;i++) {
            sample_1d_linear( ctx, img, s[i],
                              &tred[i], &tgreen[i], &tblue[i], &talpha[i] );
         }
      }
   }

   /* Modify incoming fragment colors according to sampled texels */
   apply_texture( ctx, n,
                  ctx->Texture.Current1D->Image[0]->Format,
                  ctx->Texture.EnvMode,
		  red, green, blue, alpha,
                  tred, tgreen, tblue, talpha );
}



/*
 * Given an array of fragment colors and texture coordinates, apply
 * 2-D texturing to the fragments.
 * Input:  n - number of fragments
 *         s,s - array of texture coordinate (s,t) values
 *         lambda - array of lambda values
 * InOut:  red, green, blue, alpha - incoming and modifed fragment colors
 */
void gl_texture_pixels_2d( GLcontext *ctx,
                           GLuint n,
			   GLfloat s[], GLfloat t[], GLfloat lambda[],
			   GLubyte red[], GLubyte green[],
			   GLubyte blue[], GLubyte alpha[] )
{
   GLubyte tred[PB_SIZE];
   GLubyte tgreen[PB_SIZE];
   GLubyte tblue[PB_SIZE];
   GLubyte talpha[PB_SIZE];
   GLuint i;
   GLfloat c;

   if (!ctx->Texture.Current2D->Complete) {
      return;
   }

   /* Compute c, the min/mag filter threshold. */
   if (ctx->Texture.Current2D->MagFilter==GL_LINEAR
       && (ctx->Texture.Current2D->MinFilter==GL_NEAREST_MIPMAP_NEAREST ||
           ctx->Texture.Current2D->MinFilter==GL_LINEAR_MIPMAP_NEAREST)) {
      c = 0.5F;
   }
   else {
      c = 0.0F;
   }

   /* Sample the texture. */
   if (lambda) {
      /* minfilter!=magfilter or using mipmaps */
      for (i=0;i<n;i++) {
	 sample_2d_texture( ctx, s[i], t[i], lambda[i],
			    &tred[i], &tgreen[i], &tblue[i], &talpha[i], c);
      }
   }
   else {
      /* use same filter for minification and magnification */
      if (ctx->Texture.Current2D->MagFilter==GL_NEAREST) {
         struct gl_texture_image *img = ctx->Texture.Current2D->Image[0];
         for (i=0;i<n;i++) {
            sample_2d_nearest( ctx, img, s[i], t[i],
                               &tred[i], &tgreen[i], &tblue[i], &talpha[i] );
         }
      }
      else {
         /* mag filter must be GL_LINEAR */
         struct gl_texture_image *img = ctx->Texture.Current2D->Image[0];
         for (i=0;i<n;i++) {
            sample_2d_linear( ctx, img, s[i], t[i],
                              &tred[i], &tgreen[i], &tblue[i], &talpha[i] );
         }
      }
   }

   apply_texture( ctx, n,
                  ctx->Texture.Current2D->Image[0]->Format,
                  ctx->Texture.EnvMode,
		  red, green, blue, alpha,
                  tred, tgreen, tblue, talpha );
}



/*
 * Given an array of fragment colors and texture coordinates, apply
 * 3-D texturing to the fragments.
 * Input:  n - number of fragments
 *         s,t,r - array of texture coordinate (s,t,r) values
 *         lambda - array of lambda values
 * InOut:  red, green, blue, alpha - incoming and modifed fragment colors
 */
void gl_texture_pixels_3d( GLcontext *ctx,
                           GLuint n,
                           GLfloat s[], GLfloat t[], GLfloat r[],
                           GLfloat lambda[],
                           GLubyte red[], GLubyte green[],
                           GLubyte blue[], GLubyte alpha[] )
{
   GLubyte tred[PB_SIZE];
   GLubyte tgreen[PB_SIZE];
   GLubyte tblue[PB_SIZE];
   GLubyte talpha[PB_SIZE];
   GLuint i;
   GLfloat c;

   if (!ctx->Texture.Current3D->Complete) {
      return;
   }

   /* Compute c, the min/mag filter threshold. */
   if (ctx->Texture.Current3D->MagFilter==GL_LINEAR
       && (ctx->Texture.Current3D->MinFilter==GL_NEAREST_MIPMAP_NEAREST ||
           ctx->Texture.Current3D->MinFilter==GL_LINEAR_MIPMAP_NEAREST)) {
      c = 0.5F;
   }
   else {
      c = 0.0F;
   }

   /* Sample the texture. */
   if (lambda) {
      /* minfilter!=magfilter or using mipmaps */
      for (i=0;i<n;i++) {
         sample_3d_texture( ctx, s[i], t[i], r[i], lambda[i],
                            &tred[i], &tgreen[i], &tblue[i], &talpha[i], c);
      }
   }
   else {
      /* use same filter for minification and magnification */
      if (ctx->Texture.Current3D->MagFilter==GL_NEAREST) {
         struct gl_texture_image *img = ctx->Texture.Current3D->Image[0];
         for (i=0;i<n;i++) {
            sample_3d_nearest( ctx, img, s[i], t[i], r[i],
                               &tred[i], &tgreen[i], &tblue[i], &talpha[i] );
         }
      }
      else {
         /* mag filter must be GL_LINEAR */
         struct gl_texture_image *img = ctx->Texture.Current3D->Image[0];
         for (i=0;i<n;i++) {
            sample_3d_linear( ctx, img, s[i], t[i], r[i],
                              &tred[i], &tgreen[i], &tblue[i], &talpha[i] );
         }
      }
   }

   apply_texture( ctx, n,
                  ctx->Texture.Current3D->Image[0]->Format,
                  ctx->Texture.EnvMode,
                  red, green, blue, alpha,
                  tred, tgreen, tblue, talpha );
}



/*
 * This is called by gl_update_state() if the NEW_TEXTURING bit in
 * ctx->NewState is set.  Basically, we check if we have a complete set
 * of mipmaps when mipmapping is enabled.
 */
void gl_update_texture_state( GLcontext *ctx )
{
   GLint i;
   struct gl_texture_object *t;

   for (t = ctx->Shared->TexObjectList; t; t = t->Next) {

      /*
       * Determine if we have a complete set of mipmaps
       */
      t->Complete = GL_TRUE;  /* be optimistic */
      if (   t->MinFilter==GL_NEAREST_MIPMAP_NEAREST
          || t->MinFilter==GL_LINEAR_MIPMAP_NEAREST
          || t->MinFilter==GL_NEAREST_MIPMAP_LINEAR
          || t->MinFilter==GL_LINEAR_MIPMAP_LINEAR) {

         /* Test dimension-independent attributes */
         for (i=1; i<MAX_TEXTURE_LEVELS; i++) {
            if (t->Image[i]) {
               if (!t->Image[i]->Data) {
                  t->Complete = GL_FALSE;
                  break;
               }
               if (t->Image[i]->Format != t->Image[0]->Format) {
                  t->Complete = GL_FALSE;
                  break;
               }
               if (t->Image[i]->Border != t->Image[0]->Border) {
                  t->Complete = GL_FALSE;
                  break;
               }
            }
         }

         if (t->Dimensions==1) {
            if (t->Image[0]) {
               /* Test 1-D mipmaps */
               GLuint width = t->Image[0]->Width2;
               for (i=1; i<MAX_TEXTURE_LEVELS; i++) {
                  if (width>1) {
                     width /= 2;
                  }
                  if (!t->Image[i]) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (!t->Image[i]->Data) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (t->Image[i]->Format != t->Image[0]->Format) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (t->Image[i]->Border != t->Image[0]->Border) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (t->Image[i]->Width2 != width ) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (width==1) {
                     break;
                  }
               }
            }
            else {
               /* Image[0] missing, this is bad */
               t->Complete = GL_FALSE;
            }
         }
         else if (t->Dimensions==2) {
            if (t->Image[0]) {
               /* Test 2-D mipmaps */
               GLuint width = t->Image[0]->Width2;
               GLuint height = t->Image[0]->Height2;
               for (i=1; i<MAX_TEXTURE_LEVELS; i++) {
                  if (width>1) {
                     width /= 2;
                  }
                  if (height>1) {
                     height /= 2;
                  }
                  if (!t->Image[i]) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (t->Image[i]->Width2 != width) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (t->Image[i]->Height2 != height) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (width==1 && height==1) {
                     break;
                  }
               }
            }
            else {
               /* Image[0] is missing, this is bad */
               t->Complete = GL_FALSE;
            }
         }
         else if (t->Dimensions==3) {
            if (t->Image[0]) {
               /* Test 3-D mipmaps */
               GLuint width = t->Image[0]->Width2;
               GLuint height = t->Image[0]->Height2;
               GLuint depth = t->Image[0]->Depth2;
               for (i=1; i<MAX_TEXTURE_LEVELS; i++) {
                  if (width>1) {
                     width /= 2;
                  }
                  if (height>1) {
                     height /= 2;
                  }
                  if (depth>1) {
                     depth /= 2;
                  }
                  if (!t->Image[i]) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (t->Image[i]->Width2 != width) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (t->Image[i]->Height2 != height) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (t->Image[i]->Depth2 != depth) {
                     t->Complete = GL_FALSE;
                     break;
                  }
                  if (width==1 && height==1 && depth==1) {
                     break;
                  }
               }
            }
            else {
               /* Image[0] is missing, this is bad */
               t->Complete = GL_FALSE;
            }
         }
         else {
            /* Dimensions = ??? */
            gl_problem(ctx, "Bug in gl_update_texture_state\n");
         }
      }
      else {
         /* not mipmapping, only need the level 0 texture image */
         if (!t->Image[0] || !t->Image[0]->Data) {
            t->Complete = GL_FALSE;
         }
      }
   }
}



/*
 * Check if texturing is enabled.  Remember, if any texture images
 * (mipmap levels) are missing, it's as if texturing is disabled.
 * Return:  GL_TRUE if texturing is enabled and all is well, else GL_FALSE
 */
GLboolean gl_texturing_enabled( GLcontext *ctx )
{
   if ((ctx->Texture.Enabled & TEXTURE_3D) && ctx->Texture.Current3D->Complete)
      return GL_TRUE;
   else if ((ctx->Texture.Enabled & TEXTURE_2D) && ctx->Texture.Current2D->Complete)
      return GL_TRUE;
   else if ((ctx->Texture.Enabled & TEXTURE_1D) && ctx->Texture.Current1D->Complete)
      return GL_TRUE;
   else
      return GL_FALSE;
}
