# **********************************************************
# Project:		ARG General libraries
# SubTree:		/export/ramoth4/bishop/simplescalar/mediabench/rasta/src/sphere2/nist/src/lib_fcns
# Filename:		Makefile
# Programmer:		J. Fiscus
# Organization:		NIST/NCSL/DIV 670/Auto Rec. Group
# Host System:		SUN 4 OS/4.1.1 (UNIX)
# Date Created:		05/28/92
#
# **********************************************************
# Makefile for SRC Subdirectory
# **********************************************************
SUBS = util sp 
SHELL = /bin/sh

it install depend clean bare:
	@-X=`pwd`; \
	for i in $(SUBS); \
	do echo '<' $$i '>'; cd $$X/$$i; make $@ PROJDIR=$(PROJDIR) ; done
