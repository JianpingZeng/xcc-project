# **********************************************************
# Project:		ARG General libraries
# SubTree:		/export/ramoth4/bishop/simplescalar/mediabench/rasta/src/sphere2/nist/src/scripts
# Filename:		Makefile
# Programmer:		J. Fiscus
# Organization:		NIST/NCSL/DIV 670/Auto Rec. Group
# Host System:		SUN 4 OS/4.1.1 (UNIX)
# Date Created:		05/28/92
#
# **********************************************************
# Makefile for SCRIPT Directory
# **********************************************************
SCRIPTS = gen_lord.sh gen_setv.sh h_header.sh raw2nist.sh 
SHELL = /bin/sh

.SILENT:

it depend clean bare:

install:
	@-for i in $(SCRIPTS); do \
	        if test -f ../../bin/$$i ; then \
	                echo "   $$i already installed"; \
	        else  \
	                echo "   $$i"; \
	                cp $$i ../../bin; \
	                chmod 755 $$i ; \
	        fi \
	done
