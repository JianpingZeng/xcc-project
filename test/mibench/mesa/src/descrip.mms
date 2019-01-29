# Makefile for core library for VMS
# contributed by Jouk Jansen  joukj@crys.chem.uva.nl


.first
	define gl [-.include.gl]

.include [-]mms-config.

##### MACROS #####

VPATH = RCS

INCDIR = $disk2:[-.include]
LIBDIR = [-.lib]
CFLAGS = /include=$(INCDIR)/define=(FBIND=1)

CORE_SOURCES = accum.c alpha.c alphabuf.c api.c attrib.c bitmap.c \
	blend.c bresenhm.c clip.c context.c copypix.c depth.c \
	dlist.c draw.c drawpix.c enable.c eval.c feedback.c fog.c \
	get.c interp.c image.c light.c lines.c logic.c \
	masking.c matrix.c misc.c pb.c pixel.c points.c pointers.c \
	polygon.c readpix.c scissor.c span.c stencil.c teximage.c \
	texobj.c texture.c triangle.c varray.c vb.c vertex.c winpos.c \
	xform.c

DRIVER_SOURCES = cmesa.c glx.c osmesa.c svgamesa.c \
	xfonts.c xmesa1.c xmesa2.c xmesa3.c

OBJECTS =accum.obj,alpha.obj,alphabuf.obj,api.obj,attrib.obj,bitmap.obj,\
	blend.obj,bresenhm.obj,clip.obj,context.obj,copypix.obj,depth.obj,\
	dlist.obj,draw.obj,drawpix.obj,enable.obj,eval.obj,feedback.obj,fog.obj,\
	get.obj,interp.obj,image.obj,light.obj,lines.obj,logic.obj,\
	masking.obj,matrix.obj,misc.obj,pb.obj,pixel.obj,points.obj,pointers.obj,\
	polygon.obj,readpix.obj,scissor.obj,span.obj,stencil.obj,teximage.obj,\
	texobj.obj,texture.obj,triangle.obj,varray.obj,vb.obj,vertex.obj,winpos.obj,\
	xform.obj,cmesa.obj,glx.obj,osmesa.obj,svgamesa.obj,\
	xfonts.obj,xmesa1.obj,xmesa2.obj,xmesa3.obj


##### RULES #####


##### TARGETS #####

# Make the library
$(LIBDIR):$(GL_LIB) : $(OBJECTS)
	$(MAKELIB) $(GL_LIB) $(OBJECTS)
	rename $(GL_LIB)* $(LIBDIR)

clean :
	purge
	delete *.obj;*

.include mms_depend.
