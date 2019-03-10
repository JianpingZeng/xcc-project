# This is a makefile for UNIX

# CHANGES MADE TO THIS FILE UNDER RSAREF license clause 1(c):
#
#  For the MIT PGP 2.6 distribution, this file was modified to permit
#  replacement of the NN_ModExp routine by an equivalent routine
#  contained in the PGP 2.6 sources.  To enable this change, an #ifdef
#  was added to the nn.c file (search for #ifndef USEMPILIB
#  below).  RSAREF *must* be compiled with USEMPILIB defined for this
#  change to occur.
#
#  This makefile was changed to use GCC and to define USEMPILIB
#  Change made May 21, 1994.
#

# extension for object files
O = o

# commands
CC = gcc
RANLIB = ranlib
LIB = ar
ASM = masm
PROTOTYPES = 1

# name of temporary library script
TEMPFILE = $(TEMP)/temp.mak

# standard include directory
STDINCDIR=/usr/include

# The places to look for include files (in order).
INCL =  -I. -I$(SRCDIR) -I$(STDINCDIR)

# name of main executable to build
PROG = rdemo

# Normal GCC flags.
CFLAGS = $(INCL) -O -c -DPROTOTYPES=$(PROTOTYPES) -DUSEMPILIB
MFLAGS = -I. -I$(SRCDIR)

# The location of the common source directory.
SRCDIR = ../../source/
SRCLIB = rsaref.a

# The location of the demo source directory.
RDEMODIR = ../../rdemo/

all : $(SRCLIB)

rdemo : rdemo.$(O) $(SRCLIB)
	$(CC) -o $@ rdemo.$(O) $(SRCLIB)

$(SRCLIB) : desc.$(O) digit.$(O) md2c.$(O) md5c.$(O) nn.$(O) prime.$(O)\
  rsa.$(O) r_encode.$(O) r_enhanc.$(O) r_keygen.$(O) r_random.$(O)\
  r_stdlib.$(O)
	$(LIB) r $@ $?
	$(RANLIB) $@

rdemo.$(O) : $(RDEMODIR)rdemo.c global.h $(SRCDIR)rsaref.h
	$(CC) $(CFLAGS) $(RDEMODIR)rdemo.c

include $(SRCDIR)targets.mak
