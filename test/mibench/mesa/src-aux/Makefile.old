# Makefile for aux toolkit

# Mesa 3-D graphics library
# Version:  2.0
# Copyright (C) 1995-1996  Brian Paul
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Library General Public
# License as published by the Free Software Foundation; either
# version 2 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Library General Public License for more details.
#
# You should have received a copy of the GNU Library General Public
# License along with this library; if not, write to the Free
# Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.


# $Id$

# $Log$


##### MACROS #####

VPATH = RCS

INCDIR = ../include
LIBDIR = ../lib

OBJECTS = glaux.o font.o image.o shapes.o teapot.o vect3d.o xxform.o



##### RULES #####

.c.o:
	$(CC) -c -I$(INCDIR) $(CFLAGS) $<



##### TARGETS #####

default:
	@echo "Specify a target configuration"

clean:
	-rm *.o *~

targets: $(LIBDIR)/$(AUX_LIB)

# Make the library
$(LIBDIR)/$(AUX_LIB): $(OBJECTS)
	$(MAKELIB) $(AUX_LIB) $(OBJECTS)
	$(RANLIB) $(AUX_LIB)
	mv $(AUX_LIB)* $(LIBDIR)

include ../Make-config


