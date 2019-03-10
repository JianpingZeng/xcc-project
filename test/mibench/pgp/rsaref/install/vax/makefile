! This is a MAKEFILE for VAX MMS utility

! extension for object files
O = obj

! commands
CC = cc
LIB = lib
LINK = link

! The name for the demo exectuble.
PROG = rdemo

! The location of the source code for the demo.
RDEMODIR = DISK$USERS:[RSAREF.RDEMO]

! The location of the common source directory.
SRCDIR = DISK$USERS:[RSAREF.SOURCE]
SRCLIB = rsaref.olb

! The places to look for include files (in order).
INCL =  /INCLUDE_DIRECTORY=([],$(SRCDIR))

! Normal C flags.
CFLAGS = $(INCL)/NoDebug/Optimize/Obj=$(MMS$TARGET)
LFLAGS = /Exec=$(MMS$TARGET)/NoTraceback/NoDebug

! Debugging C flags.
! CFLAGS = $(INCL)/Debug/NoOptimize/Obj=$(MMS$TARGET)
! LFLAGS = /Exec=$(MMS$TARGET)/Traceback/Debug/Map/Full/Cross
 
$(PROG) : rdemo.$(O) $(SRCLIB)
    $(LINK) $(LFLAGS) rdemo.$(O),-
    $(SRCLIB)/Library

all : $(SRCLIB)

$(SRCLIB) : desc.$(O) digit.$(O) md2c.$(O) md5c.$(O) nn.$(O)\
            prime.$(O) rsa.$(O) r_encode.$(O) r_enhanc.$(O) r_keygen.$(O)\
            r_random.$(O) r_stdlib.$(O)
    $(LIB) /create $@
    $(LIB) /replace $@ $?

.INCLUDE $(SRCDIR)targets.mak

rdemo.$(O) : $(RDEMODIR)rdemo.c global.h $(SRCDIR)rsaref.h
    $(CC) $(CFLAGS) $(RDEMODIR)rdemo.c

