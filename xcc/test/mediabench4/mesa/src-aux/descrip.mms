# Makefile for aux toolkit for VMS
# contributed by Jouk Jansen  joukj@crys.chem.uva.nl


.first
	define gl [-.include.gl]

.include [-]mms-config.

##### MACROS #####

VPATH = RCS

INCDIR = $disk2:[-.include]
LIBDIR = [-.lib]
CFLAGS = /include=$(INCDIR)/define=(FBIND=1)

OBJECTS = glaux.obj,font.obj,image.obj,shapes.obj,teapot.obj,vect3d.obj,\
	xxform.obj



##### RULES #####


##### TARGETS #####

# Make the library
$(LIBDIR)$(AUX_LIB) : $(OBJECTS)
	$(MAKELIB) $(AUX_LIB) $(OBJECTS)
	rename $(AUX_LIB)* $(LIBDIR)

clean :
	delete *.obj;
	purge


