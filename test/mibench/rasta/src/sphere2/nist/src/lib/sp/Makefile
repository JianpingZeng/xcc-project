# **********************************************************
# Project:		ARG General libraries
# SubTree:		/export/ramoth4/bishop/simplescalar/mediabench/rasta/src/sphere2/nist/src/lib/ sp
# Filename:		Makefile
# Programmer:		J. Fiscus
# Organization:		NIST/NCSL/DIV 670/Auto Rec. Group
# Host System:		SUN 4 OS/4.1.1 (UNIX)
# Date Created:		05/28/92
# Project:		NIST LIBRARIES
# Revision history:
#
# Apr 04, 1994
#	- fixes the make 'depend' and 'makefile' commands to not fail
# 	  on the SGI
#
# **********************************************************
# Makefile for SRC Library Directory
# **********************************************************
SHELL = /bin/sh

# PROJDIR Passed On Command Line ONLY
PROJECT_ROOT	= /export/ramoth4/bishop/simplescalar/mediabench/rasta/src/sphere2/nist
LIBDIR		= $(PROJECT_ROOT)/lib
DOCDIR		= $(PROJECT_ROOT)/doc

RANLIB		= /export/ramoth4/bishop/simplescalar/bin/ssbig-na-sstrix-ranlib
AR		= /export/ramoth4/bishop/simplescalar/bin/ssbig-na-sstrix-ar ru

# **************************************************
# INDIVIDUAL PROGRAM MODIFICATIONS SHOULD BEGIN HERE
# **************************************************

# Library to make (library name)
LIBRARY_ID = sp
LIBRARY	= lib$(LIBRARY_ID).a
# Source files for LIBRARY
SRC	= alaw.c array.c dif_wavf.c dupfinfo.c exit.c fixio.c h_fix.c license.c lpc.c nat_bord.c poly.c sdatamod.c shorten.c shpack_s.c shpacki2.c shpackio.c sp_ntoh.c sp_utils.c spalloc.c spchksum.c spclose.c speof.c sperror.c sphere.c sphio.c spinput.c sploadf.c spopen.c spoutput.c spread.c sprewind.c sprstat.c spseek.c sptell.c sptemp.c sputils2.c spvers.c spwrite.c stdfield.c ulaw.c vario.c verbose.c wavedit.c wavpack.c 

# Local additions for CFLAG options (eg: -g)
LOCAL_CFLAGS	= -static -DNARCH_SimpleScalar

# *******************************************************
# THE REST OF THE MAKEFILE SHOULD NOT NEED TO BE MODIFIED
#       (EXCEPT UPON APPROVAL OF PROJECT MANAGER)
# *******************************************************

INCLUDE = $(PROJECT_ROOT)/include
OBJ	= alaw.o array.o dif_wavf.o dupfinfo.o exit.o fixio.o h_fix.o license.o lpc.o nat_bord.o poly.o sdatamod.o shorten.o shpack_s.o shpacki2.o shpackio.o sp_ntoh.o sp_utils.o spalloc.o spchksum.o spclose.o speof.o sperror.o sphere.o sphio.o spinput.o sploadf.o spopen.o spoutput.o spread.o sprewind.o sprstat.o spseek.o sptell.o sptemp.o sputils2.o spvers.o spwrite.o stdfield.o ulaw.o vario.o verbose.o wavedit.o wavpack.o 
# Enable converting the distribution to K&R C
#KRC     = $(SRC:%.c=%.kr.c)
LIBRY	= $(LIBRARY)

CFLAGS	= -I$(INCLUDE) $(LOCAL_CFLAGS)
CC	= /export/ramoth4/bishop/simplescalar/bin/ssbig-na-sstrix-gcc $(CFLAGS)

MAKEFILE	= Makefile
.PRECIOUS: $(MAKEFILE)

it: $(LIBRY)

install: $(MAKEFILE) $(LIBDIR)/$(LIBRARY)
$(LIBDIR)/$(LIBRARY): $(LIBRY)
	mv $(LIBRY) $(LIBDIR)
	chmod 664 $(LIBDIR)/$(LIBRARY)
	$(RANLIB) $(LIBDIR)/$(LIBRARY)

$(LIBRY): $(OBJ)
	$(AR) $(LIBRY) $?
	$(RANLIB) $(LIBRY)

#build_kr: $(KRC)
#$(KRC): $@
#	cp `echo $@ | sed 's/.kr//'` $@
#	unprotoize -c "-I$(INCLUDE)" $@
#	rm -f $@.save

%.o: %.c
	$(CC) -c -o $@ $<

clean :
	rm -f $(OBJ) $(LIBRARY) $(LIBRARY_ID).doc *.BAK dependlist;

bare: clean
	rm -f $(LIBRY)

depend $(MAKEFILE): $(SRC)
	$(CC) -M $(SRC) > dependlist
	@sed -e '1,/^# DO NOT DELETE/!d' $(MAKEFILE) > $(MAKEFILE).tmp.$$$$; \
	cat dependlist >> $(MAKEFILE).tmp.$$$$; \
	cp $(MAKEFILE) $(MAKEFILE).BAK; \
	mv $(MAKEFILE).tmp.$$$$ $(MAKEFILE); \
	rm -f dependlist \
	echo make depend complete

document:
	$(DOCUMENT) -h $(SRC) > $(LIBRARY_ID).doc
	echo "" >> $(LIBRARY_ID).doc
	echo "Procedure Definitions" >> $(LIBRARY_ID).doc
	echo "" >> $(LIBRARY_ID).doc
	$(DOCUMENT) -t $(SRC) >> $(LIBRARY_ID).doc




# DO NOT DELETE THIS LINE - make depend uses it
