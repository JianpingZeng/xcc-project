# **********************************************************
# Project:		ARG General libraries
# SubTree:		/export/ramoth4/bishop/simplescalar/mediabench/rasta/src/sphere2/nist/src/lib/ util
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
LIBRARY_ID = util
LIBRARY	= lib$(LIBRARY_ID).a
# Source files for LIBRARY
SRC	= cfg.c comments.c ds.c err_util.c fobops.c hist.c hsgetopt.c itoa.c maldie.c mtrf.c order.c pad.c rsprintf.c sfgets.c statdist.c str_srch.c string.c t_or_f.c word.c 

# Local additions for CFLAG options (eg: -g)
LOCAL_CFLAGS	= -static -DNARCH_SimpleScalar

# *******************************************************
# THE REST OF THE MAKEFILE SHOULD NOT NEED TO BE MODIFIED
#       (EXCEPT UPON APPROVAL OF PROJECT MANAGER)
# *******************************************************

INCLUDE = $(PROJECT_ROOT)/include
OBJ	= cfg.o comments.o ds.o err_util.o fobops.o hist.o hsgetopt.o itoa.o maldie.o mtrf.o order.o pad.o rsprintf.o sfgets.o statdist.o str_srch.o string.o t_or_f.o word.o 
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
