# Makefile for demo programs

# Mesa 3-D graphics library
# Version:  2.2
# Copyright (C) 1995-1997  Brian Paul
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


# $Id: Makefile,v 1.3 1997/02/15 11:11:21 brianp Exp $

# $Log: Makefile,v $
# Revision 1.3  1997/02/15 11:11:21  brianp
# added stex3d demo
#
# Revision 1.2  1997/02/13 18:25:08  brianp
# added morph3d demo
#
# Revision 1.1  1997/02/13 18:21:00  brianp
# Initial revision
#


##### MACROS #####

INCDIR = ../include

GL_LIBS = -L../lib -lMesaaux -lMesatk -lMesaGLU -lMesaGL -lm $(XLIBS)

LIB_DEP = ../lib/$(GL_LIB) ../lib/$(GLU_LIB) ../lib/$(TK_LIB) ../lib/$(AUX_LIB)

PROGS = bounce gamma gears glxdemo glxpixmap isosurf morph3d offset osdemo \
	reflect spin stex3d test0 texobj winpos xdemo 

### TODO: finish new shadow demo


##### RULES #####

.SUFFIXES:
.SUFFIXES: .c

.c: $(LIB_DEP)
	$(CC) -I$(INCDIR) $(CFLAGS) $< $(GL_LIBS) -o $@



##### TARGETS #####

default:
	@echo "Specify a target configuration"

clean:
	-rm *.o *~

realclean:
	-rm $(PROGS)
	-rm *.o *~

targets: $(PROGS)

include ../Make-config

