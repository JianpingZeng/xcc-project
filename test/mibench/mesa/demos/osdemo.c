/* osdemo.c */
/* For tracing and profiling purposes, we need to compile it with the static
   option turned on.

   gcc -static -mv8 -I../include -I/usr/openwin/include -O3 osdemo.c \
   -L../lib -lMesaaux -lMesatk -lMesaGLU -lMesaGL -lm -o osdemo

   Refer to Makefile.static.
*/

/* Demo of off-screen Mesa rendering */

/*
 * See Mesa/include/GL/osmesa.h for documentation of the OSMesa functions.
 *
 * If you want to render BIG images you'll probably have to increase
 * MAX_WIDTH and MAX_HEIGHT in src/config.h.
 *
 * This program is in the public domain.
 *
 * Brian Paul
 *
 * PPM output provided by Joerg Schmalzl.
 */


#include "stats_time.h"
#include <stdio.h>
#include <stdlib.h>
#include "GL/osmesa.h"

/* Is it necessary??? */
/* #include "glaux.h" */



#define WIDTH 400
#define HEIGHT 400



static void render_image( void )
{
   GLfloat light_ambient[] = { 0.0, 0.0, 0.0, 1.0 };
   GLfloat light_diffuse[] = { 1.0, 1.0, 1.0, 1.0 };
   GLfloat light_specular[] = { 1.0, 1.0, 1.0, 1.0 };
   GLfloat light_position[] = { 1.0, 1.0, 1.0, 0.0 };
   GLfloat red_mat[]   = { 1.0, 0.2, 0.2, 1.0 };
   GLfloat green_mat[] = { 0.2, 1.0, 0.2, 1.0 };
   GLfloat blue_mat[]  = { 0.2, 0.2, 1.0, 1.0 };


   glLightfv(GL_LIGHT0, GL_AMBIENT, light_ambient);
   glLightfv(GL_LIGHT0, GL_DIFFUSE, light_diffuse);
   glLightfv(GL_LIGHT0, GL_SPECULAR, light_specular);
   glLightfv(GL_LIGHT0, GL_POSITION, light_position);
    
   glEnable(GL_LIGHTING);
   glEnable(GL_LIGHT0);
   glEnable(GL_DEPTH_TEST);

   glMatrixMode(GL_PROJECTION);
   glLoadIdentity();
   glOrtho(-2.5, 2.5, -2.5, 2.5, -10.0, 10.0);
   glMatrixMode(GL_MODELVIEW);

   glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );

   glPushMatrix();
   glRotatef(20.0, 1.0, 0.0, 0.0);

   glPushMatrix();
   glTranslatef(-0.75, 0.5, 0.0); 
   glRotatef(90.0, 1.0, 0.0, 0.0);
   glMaterialfv( GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, red_mat );
   auxSolidTorus(0.275, 0.85);
   glPopMatrix();

   glPushMatrix();
   glTranslatef(-0.75, -0.5, 0.0); 
   glRotatef(270.0, 1.0, 0.0, 0.0);
   glMaterialfv( GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, green_mat );
   auxSolidCone(1.0, 2.0);
   glPopMatrix();

   glPushMatrix();
   glTranslatef(0.75, 0.0, -1.0); 
   glMaterialfv( GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, blue_mat );
   auxSolidSphere(1.0);
   glPopMatrix();

   glPopMatrix();
}



int main( int argc, char *argv[] )
{
//SS_START
   OSMesaContext ctx;
   void *buffer;

   /* Create an RGBA-mode context */
   ctx = OSMesaCreateContext( GL_RGBA, NULL );

   /* Allocate the image buffer */
   buffer = malloc( WIDTH * HEIGHT * 4 );

   /* Bind the buffer to the context and make it current */
   OSMesaMakeCurrent( ctx, buffer, GL_UNSIGNED_BYTE, WIDTH, HEIGHT );

   render_image();

   if (argc>1) {
      /* write PPM file */
      FILE *f = fopen( argv[1], "w" );
      if (f) {
         int i, x, y;
         GLubyte *ptr;
         ptr = buffer;
         fprintf(f,"P6\n");
         fprintf(f,"# ppm-file created by %s\n",  argv[0]);
         fprintf(f,"%i %i\n", WIDTH,HEIGHT);
         fprintf(f,"255\n");
         fclose(f);
         f = fopen( argv[1], "ab" );  /* reopen in binary append mode */
         for (y=HEIGHT-1; y>=0; y--) {
            for (x=0; x<WIDTH; x++) {
               i = (y*WIDTH + x) * 4;
               fputc(ptr[i], f);   /* write red */
               fputc(ptr[i+1], f); /* write green */
               fputc(ptr[i+2], f); /* write blue */
            }
         }
         fclose(f);
      }
   }
   else {
      printf("Specify a filename if you want to make a ppm file\n");
   }

   printf("all done\n");

   /* free the image buffer */
   free( buffer );

   /* destroy the context */
   OSMesaDestroyContext( ctx );
//SS_STOP
//SS_PRINT
   return 0;
}
