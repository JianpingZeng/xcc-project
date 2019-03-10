# **********************************************************
# Project:		ARG General libraries
# SubTree:		/export/ramoth4/bishop/simplescalar/mediabench/rasta/src/sphere2/nist
# Filename:		Makefile
# Programmer:		J. Fiscus
# Organization:		NIST/NCSL/DIV 670/Auto Rec. Group
# Host System:		SUN 4 OS/4.1.1 (UNIX)
# Date Created:		05/28/92
#
# **********************************************************
# Makefile for Root Project Directory
# **********************************************************
PROJDIR = /export/ramoth4/bishop/simplescalar/mediabench/rasta/src/sphere2/nist
SUBS = src
SHELL = /bin/sh

it install depend clean:
	@-X=`pwd`; \
	for i in $(SUBS); \
	do echo '<<<' $$i '>>>'; cd $$X/$$i; \
	make $@ PROJDIR=$(PROJDIR) ; done

bare: clean
	rm -f bin/* lib/packages/*.pas lib/lib*.a 
