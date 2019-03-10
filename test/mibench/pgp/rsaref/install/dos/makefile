# This is a MAKEFILE for Microsoft's NMAKE

# extension for object files
O = obj

# commands
CC = cl
LIB = lib
ASM = masm

# name of temporary library script
TEMPFILE = $(TEMP)\temp.mak

# standard include directory
STDINCDIR = i:\msc6\include

# The places to look for include files (in order).
INCL =  -I. -I$(SRCDIR) -I$(STDINCDIR)

# Normal C flags.
CFLAGS = -W3 -AL -Zp $(INCL) -c -DPROTOTYPES=1 -DUSEMPILIB
LFLAGS = /stack:26000

# Debugging C flags.
#CFLAGS =  -W3 -AL -Zpi -Od $(INCL) -c -DPROTOTYPES=1
#LFLAGS = /codeview /map /stack:26000
 
# The location of the common source directory.
SRCDIR = ..\source\#
SRCLIB = rsaref.lib

# The location of the demo source directory.
RDEMODIR = ..\rdemo\#

all : $(SRCLIB)

rdemo.exe : rdemo.$(O) $(SRCLIB)
	link @<<temp.lnk
$(LFLAGS) rdemo.$(O)
$@,,
$(SRCLIB);
<<NOKEEP

$(SRCLIB) : desc.$(O) digit.$(O) md2c.$(O) nn.$(O) prime.$(O)\
  rsa.$(O) r_encode.$(O) r_enhanc.$(O) r_keygen.$(O) r_random.$(O)\
  r_stdlib.$(O)
  @if NOT EXIST $@ $(LIB) $@;
  @echo $@ > $(TEMPFILE)
  @!echo -+$? & >> $(TEMPFILE)
  @echo ;' >> $(TEMPFILE)
  @$(LIB) @$(TEMPFILE)

rdemo.$(O) : $(RDEMODIR)rdemo.c global.h $(SRCDIR)rsaref.h
	$(CC) $(CFLAGS) $(RDEMODIR)rdemo.c

!INCLUDE $(SRCDIR)targets.mak
