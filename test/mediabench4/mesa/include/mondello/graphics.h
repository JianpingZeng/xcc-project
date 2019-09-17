/*
  graphics.h
*/

#ifndef GRAPHICS_H
#define GRAPHICS_H

#include <sys/types.h>
#include "type.h"

/*
 * points:
 *   point(bpp)_(2d=2,3d=3)[colored=c]
 *   
 *   ex: 24 bpp, 3d, colored point: point24_3c
 *
 * lines:
 *   line(bpp)_(2d=2,3d=3)(flat=f,s=smooth)[colored=c]
 *   note: smooth shaded lines do not have the last above argument.
 *
 *   ex: 8bpp, 3d, smooth shaded line: line8_3s
 *
 * polygons:
 *   poly(bpp)_(2d=2,3d=3)(flat=f,s=smooth)[colored=c]
 *   note: smooth shaded polygons do not have the last above argument.
 *  
 *   ex: 24bpp, 3d, flat shaded, colored line: poly24_fc
 *
 * note: colored simply means that the params for how the primitive is 
 *       to be colored are passed to the routine instead of using 
 *       color(bpp)_set(...) beforehand.
 *
 */
 
void clgd547x_flush();
void clgd547x_finish();
void clgd547x_setBuffer(int buffer);           /* 0=front, 1=back */
void clgd547x_clearDepthBuffer();
void color8_set(uint index);
void color24_set(uint r, uint g, uint b, uint a);

void clear8c(uint index);
void clear24c(uint r, uint g, uint b, uint a);

void clearArea8_2(uint x1, uint y1, uint x2, uint y2);
void clearArea8_2c(uint x1, uint y1, uint x2, uint y2, uint index);
void clearArea8_3(uint x1, uint y1, uint x2, uint y2, uint z);
void clearArea8_3c(uint x1, uint y1, uint x2, uint y2, uint z, uint index);

void clearArea24_2(uint x1, uint y1, uint x2, uint y2);
void clearArea24_2c(uint x1, uint y1, uint x2, uint y2,
                   uint r, uint g, uint b, uint a);
void clearArea24_3(uint x1, uint y1, uint x2, uint y2, uint z);
void clearArea24_32c(uint x1, uint y1, uint x2, uint y2, uint z,
                   uint r, uint g, uint b, uint a);

#define point8_2c(x,y,index) line8_2fc(x,y,x,y,index)
#define point8_3c(x,y,z,index) line8_3fc(x,y,z,x,y,z,index)

#define point24_2c(x,y,r,g,b,a) line24_2fc(x,y,x,y,r,g,b,a)
#define point24_3c(x,y,z,r,g,b,a) line24_3fc(x,y,z,x,y,z,r,g,b,a)

void line8_2fc(uint x1, uint y1, uint x2, uint y2, uint index);
void line8_2s(uint x1, uint y1, uint i1, uint x2, uint y2, uint i2);
void line8_3fc(uint x1, uint y1, uint z1, uint x2, uint y2, uint z2, uint index);
void line8_3s(uint x1, uint y1, uint z1, uint i1, uint x2, uint y2, uint z2, uint i2);

void line24_2fc(uint x1, uint y1, uint x2, uint y2, uint r, uint g, uint b, uint a);
void line24_2s(uint x1, uint y1, uint r1, uint g1, uint b1, uint a1, 
               uint x2, uint y2, uint i2, uint g2, uint b2, uint a2 );
void line24_3fc(uint x1, uint y1, uint z1, uint x2, uint y2, uint z2, 
                uint r, uint g, uint b, uint a);
void line24_3s(uint x1, uint y1, uint z1, uint r1, uint g1, uint b1, uint a1, 
               uint x2, uint y2, uint z2, uint r2, uint g2, uint b2, uint a2);

void triangle8_3fc(uint x1, uint y1, uint z1,
                   uint x2, uint y2, uint z2,
                   uint x3, uint y3, uint z3,
                   uint index);              
void triangle8_3s(uint x1, uint y1, uint z1, uint i1,
                  uint x2, uint y2, uint z2, uint i2,
                  uint x3, uint y3, uint z3, uint i3);

void triangle24_3fc(uint x1, uint y1, uint z1,
                    uint x2, uint y2, uint z2,
                    uint x3, uint y3, uint z3,
                    uint r, uint g, uint b, uint a);

void triangle24_3s(uint x1, uint y1, uint z1, uint r1, uint g1, uint b1, uint a1,
                   uint x2, uint y2, uint z2, uint r2, uint g2, uint b2, uint a2,
                   uint x3, uint y3, uint z3, uint r3, uint g3, uint b3, uint a3);              


void triangle8_3fv(uint v1, uint v2, uint v3, uint pv);
void triangle8_3fcv(uint v1, uint v2, uint v3, uint pv);
void triangle8_3sv(uint v1, uint v2, uint v3);
void triangle24_3fv(uint v1, uint v2, uint v3, uint pv);
void triangle24_3fcv(uint v1, uint v2, uint v3, uint pv);
void triangle24_3sv(uint v1, uint v2, uint v3);

#endif

