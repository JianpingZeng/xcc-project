# Makefile for GLU for VMS
# contributed by Jouk Jansen  joukj@crys.chem.uva.nl

.first
	define gl [-.include.gl]

.include [-]mms-config.

##### MACROS #####

VPATH = RCS

INCDIR = $disk2:[-.include]
LIBDIR = [-.lib]
CFLAGS = /include=$(INCDIR)/define=(FBIND=1)

SOURCES = glu.c mipmap.c nurbs.c nurbscrv.c nurbssrf.c nurbsutl.c \
	project.c quadric.c tess.c tesselat.c polytest.c

OBJECTS =glu.obj,mipmap.obj,nurbs.obj,nurbscrv.obj,nurbssrf.obj,nurbsutl.obj,\
	project.obj,quadric.obj,tess.obj,tesselat.obj,polytest.obj



##### RULES #####


##### TARGETS #####

# Make the library:
$(LIBDIR)$(GLU_LIB) : $(OBJECTS)
	$(MAKELIB) $(GLU_LIB) $(OBJECTS)
	rename $(GLU_LIB)* $(LIBDIR)
clean :
	delete *.obj;*
	purge

include mms_depend.

