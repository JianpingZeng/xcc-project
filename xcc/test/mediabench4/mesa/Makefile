# Top-level makefile for Mesa

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


# $Id: Makefile,v 1.19 1997/03/14 00:23:53 brianp Exp $

# $Log: Makefile,v $
# Revision 1.19  1997/03/14 00:23:53  brianp
# added STARTUP.MK and mesa3dfx to tar files
#
# Revision 1.18  1997/02/08 07:53:52  brianp
# added beos and osf1-sl configs
#
# Revision 1.17  1997/02/01 10:01:33  brianp
# replaced widgets-old with widgets-mesa
#
# Revision 1.16  1997/01/30 21:10:08  brianp
# src-tk/README was missing from tar file
#
# Revision 1.15  1997/01/30 18:52:12  brianp
# added linux-alpha config
#
# Revision 1.14  1997/01/04 06:19:46  brianp
# added openbsd config from Thomas Graichen
# added a few files to TAR_FILES for DOS/DJGPP from Charlie Wallace
#
# Revision 1.13  1996/12/18 18:19:04  brianp
# added -r flag (recurse) to zip command
#
# Revision 1.12  1996/12/18 18:15:28  brianp
# added xlib.opt to tar file, per Jouk Jansen
#
# Revision 1.11  1996/11/30 14:35:40  brianp
# fixed problem with openstep config
#
# Revision 1.10  1996/11/24 20:50:52  brianp
# removed windows2/ and nt/ from tar file
#
# Revision 1.9  1996/11/24 19:33:10  brianp
# added openstep config
#
# Revision 1.8  1996/11/14 01:42:45  brianp
# added MS-DOS driver files to TAR_FILES macro
#
# Revision 1.7  1996/11/14 01:13:50  brianp
# added rule to make a ZIP file
#
# Revision 1.6  1996/10/31 01:19:32  brianp
# incremented version to 2.1
#
# Revision 1.5  1996/10/31 01:13:02  brianp
# added VMS stuff
#
# Revision 1.4  1996/10/09 03:22:39  brianp
# added unixware-shared config
# removed old Amiga files from tar file list
#
# Revision 1.3  1996/09/27 17:30:40  brianp
# added windows2 subdirectory, removed README.WIN from tar files
#
# Revision 1.2  1996/09/26 22:42:32  brianp
# added linux-debug target
# added widgets-old and widgets-sgi to tar file list
#
# Revision 1.1  1996/09/12 00:39:00  brianp
# Initial revision
#



# To add a new configuration for your system add it to the list below
# then update the Make-config file.



default:
	@echo "Type one of the following:"
	@echo "  make aix                  for IBM RS/6000 with AIX"
	@echo "  make aix-sl               for IBM RS/6000, make shared libs"
	@echo "  make amiwin               for Amiga with SAS/C and AmiWin"
	@echo "  make amix                 for Amiga 3000 UX  SVR4 v2.1 systems"
	@echo "  make beos                 for BeOS with DR 8.2"
	@echo "  make bsdos                for BSD/OS from BSDI using GCC"
	@echo "  make dgux                 for Data General"
	@echo "  make freebsd              for FreeBSD systems with GCC"
	@echo "  make gcc                  for a generic system with GCC"
	@echo "  make hpux                 for HP systems with HPUX"
	@echo "  make hpux-gcc             for HP systems with HPUX using GCC"
	@echo "  make hpux-sl              for HP systems with HPUX, make shared libs"
	@echo "  make irix4                for SGI systems with IRIX 4.x"
	@echo "  make irix5                for SGI systems with IRIX 5.x"
	@echo "  make irix5-dso            for SGI systems with IRIX 5.x, make DSOs"
	@echo "  make irix6-32             for SGI systems with IRIX 6.x, make 32-bit libs"
	@echo "  make irix6-32-dso         for SGI systems with IRIX 6.x, make 32-bit DSOs"
	@echo "  make irix6-n32            for SGI systems with IRIX 6.x, make n32-bit libs"
	@echo "  make irix6-n32-dso        for SGI systems with IRIX 6.x, make n32-bit DSOs"
	@echo "  make irix6-64             for SGI systems with IRIX 6.x, make 64-bit libs"
	@echo "  make irix6-64-dso         for SGI systems with IRIX 6.x, make 64-bit DSOs"

	@echo "  make linux                for Linux systems with GCC"
	@echo "  make linux-elf            for Linux systems, make ELF shared libs"
	@echo "  make linux-alpha          for Linux on Alpha systems"
	@echo "  make linux-mondello       for Linux with prototype Cirrus Mondello card"
	@echo "  make mswindows            for Microsoft Windows"
	@echo "  make macintosh            for Macintosh"
	@echo "  make machten-2.2          for Macs w/ MachTen 2.2 (68k w/ FPU)"
	@echo "  make machten-4.0          for Macs w/ MachTen 4.0.1 or newer with GNU make"
	@echo "  make mklinux              for Linux on Power Macintosh"
	@echo "  make netbsd               for NetBSD 1.0 systems with GCC"
	@echo "  make next                 for NeXT systems with NEXTSTEP 3.3"
	@echo "  make next-x86-x11         for NeXT on Intel x86 with X11"
	@echo "  make next-x11             for NeXT with X11"
	@echo "  make openbsd              for OpenBSD systems"
	@echo "  make openstep             for NeXT systems with OPENSTEP 4.0"
	@echo "  make os2                  for OS/2 with XFree86"
	@echo "  make osf1                 for DEC Alpha systems with OSF/1"
	@echo "  make osf1-sl              for DEC Alpha systems with OSF/1, make shared libs"
	@echo "  make qnx                  for QNX V4 systems with Watcom compiler"
	@echo "  make sco                  for SCO Unix systems with ODT"
	@echo "  make solaris-x86          for PCs with Solaris"
	@echo "  make solaris-x86-gcc      for PCs with Solaris using GCC"
#	@echo "  make solaris-gcc          for Solaris 2 systems with GCC"
	@echo "  make sunos4               for Suns with SunOS 4.x"
	@echo "  make sunos4-sl            for Suns with SunOS 4.x, make shared libs"
	@echo "  make sunos4-gcc           for Suns with SunOS 4.x and GCC"
	@echo "  make sunos4-gcc-sl        for Suns with SunOS 4.x, GCC, make shared libs"
	@echo "  make sunos5               for Suns with SunOS 5.x"
	@echo "  make sunos5-sl            for Suns with SunOS 5.x, make shared libs"
	@echo "  make sunos5-gcc           for Suns with SunOS 5.x and GCC"
	@echo "  make sunos5-gcc-sl        for Suns with SunOS 5.x, GCC, make shared libs"
	@echo "  make sunos5-x11r6-gcc-sl  for Suns with X11R6, GCC, make shared libs"
	@echo "  make sunSolaris-CC        for Solaris using C++ compiler"
	@echo "  make ultrix-gcc           for DEC systems with Ultrix and GCC"
	@echo "  make unicos               for Cray C90 (and other?) systems"
	@echo "  make unixware             for PCs running UnixWare"
	@echo "  make unixware-shared      for PCs running UnixWare, shared libs"
	@echo "  make vistra               for Stardent Vistra systems"
	@echo "  make simple               for simplescalar simulator"
	@echo "  make clean                remove .o files"
	@echo "  make realclean            remove .o, library and executable files"

#################################################
# Comments added by C. Lee
#
# For the purposes of benchmarking, we do not use 
# X11 or any other graphic displays. Outputs are
# directed to files. As such, we do not need to include
# those functions for graphic display. The commented lines
# are excluded from compiling.

aix aix-sl amix bsdos debug dgux freebsd gcc hpux hpux-gcc hpux-sl \
irix4 irix5 irix5-dso irix6-32 irix6-n32 irix6-64 irix6-32-dso irix6-n32-dso \
irix6-64-dso linux linux-debug linux-elf linux-alpha machten-2.2 machten-4.0 \
mklinux netbsd next-x86-x11 next-x11 osf1 osf1-sl openbsd qnx sco \
solaris-x86 solaris-x86-gcc sunos4 sunos4-sl sunos4-gcc sunos4-gcc-sl \
sunos5 sunos5-sl sunos5-gcc sunos5-gcc-sl sunos5-x11r6-gcc-sl \
ultrix-gcc unicos unixware vistra simple sunSolaris-CC:
	-mkdir lib
	touch src/depend
	touch src-glu/depend
	cd src ; $(MAKE) $@
	cd src-glu ; $(MAKE) $@
#	cd src-tk ; $(MAKE) $@
	cd src-aux ; $(MAKE) $@
	cd demos ; $(MAKE) $@
#	cd samples ; $(MAKE) $@
#	cd book ; $(MAKE) $@

amiwin:
	mklib.amiwin

beos:
	-mkdir lib
	cd src ; $(MAKE) -f Makefile.BeOS $@
	cd src-glu ; $(MAKE) -f Makefile $@
	cd src-tk ; $(MAKE) -f Makefile.BeOS $@
	cd src-aux ; $(MAKE) -f Makefile.BeOS $@
	cd BeOS ; $(MAKE) -f Makefile.BeOS $@

linux-mondello:
	-mkdir lib
	touch src/depend
	touch src-glu/depend
	cd src ; $(MAKE) $@
	cd mondello ; $(MAKE) $@
	cd src-glu ; $(MAKE) $@
	cd src-tk2 ; $(MAKE) $@
	cd src-aux ; $(MAKE) $@
	cd demos ; $(MAKE) $@
	cd samples ; $(MAKE) $@
	cd book ; $(MAKE) $@

macintosh:
	@echo "See the README file for Macintosh intallation information"

mswindows:
	@echo "See the windows/README file for installation information"

next:
	-mkdir lib
	cd src ; $(MAKE) -f Makefile.NeXT $@
	cd src-glu ; $(MAKE) -f Makefile $@
	cd src-tk ; $(MAKE) -f Makefile.NeXT $@
	cd src-aux ; $(MAKE) -f Makefile.NeXT $@
	cd NeXT ; $(MAKE) -f Makefile.NeXT $@

openstep:
	-mkdir lib
	cd src ; $(MAKE) -f Makefile.OpenStep $@
	cd src-glu ; $(MAKE) -f Makefile.OpenStep $@
	cd src-tk ; $(MAKE) -f Makefile.OpenStep $@
	cd src-aux ; $(MAKE) -f Makefile.OpenStep $@
	cd OpenStep; $(MAKE)

os2:
	@echo "See the README.OS2 file for instructions"



# Remove .o files, emacs backup files, etc.
clean:
	-rm -f include/*~
	-rm -f include/GL/*~
	-rm -f src/*.o src/*~ src/*.a src/*.ll src/*.s src/*.bc
	-rm -f src-aux/*.o src-aux/*~ src-aux/*.a src-aux/*.ll src-aux/*.s src-aux/*.bc
	-rm -f src-glu/*.o src-glu/*~ src-glu/*.a src-glu/*.ll src-glu/*.s src-glu/*.bc
	-rm -f src-tk/*.o src-tk/*~ src-tk/*.a src-tk/*.ll src-tk/*.s src-tk/*.bc
	-rm -f src-tk2/*.o src-tk2/*~ src-tk2/*.a src-tk2/*.ll src-tk2/*.s src-tk2/*.bc
#	-rm -f book/*.o book/*~
	-rm -f demos/*.o demos/*~
#	-rm -f samples/*.o samples/*~
#	-rm -f mondello/*.o mondello/*~ mondello/*.a

# Remove everthing that can be remade
realclean: clean
	-rm -f lib/*.a lib/*.so*
#	cd book ; $(MAKE) realclean
	cd demos ; $(MAKE) realclean
#	cd samples ; $(MAKE) realclean
#	cd mondello; $(MAKE) realclean
	-rm -f lib/*.a lib/*.so*



MESA_HOME = Mesa-2.2
MESA_NAME = Mesa-2.2

TAR_FILES =	\
	$(MESA_HOME)/README*			\
	$(MESA_HOME)/IAFA-PACKAGE		\
	$(MESA_HOME)/LICENSE			\
	$(MESA_HOME)/VERSIONS			\
	$(MESA_HOME)/Makefile			\
	$(MESA_HOME)/Make-config		\
	$(MESA_HOME)/Imakefile			\
	$(MESA_HOME)/descrip.mms		\
	$(MESA_HOME)/mms-config			\
	$(MESA_HOME)/xlib.opt			\
	$(MESA_HOME)/MESADJ.BAT			\
	$(MESA_HOME)/MESADOS.BAT		\
	$(MESA_HOME)/STARTUP.MK			\
	$(MESA_HOME)/mklib.*			\
	$(MESA_HOME)/include/*.h		\
	$(MESA_HOME)/include/GL/*.h		\
	$(MESA_HOME)/include/mondello/*.h	\
	$(MESA_HOME)/src*/README		\
	$(MESA_HOME)/src*/README[12]		\
	$(MESA_HOME)/src*/Makefile		\
	$(MESA_HOME)/src*/Makefile.NeXT		\
	$(MESA_HOME)/src*/Makefile.BeOS		\
	$(MESA_HOME)/src*/Imakefile		\
	$(MESA_HOME)/src*/descrip.mms		\
	$(MESA_HOME)/src*/mms_depend		\
	$(MESA_HOME)/src*/DEPEND.DOS		\
	$(MESA_HOME)/src*/MAKEFILE.DJ		\
	$(MESA_HOME)/src*/MAKEFILE.DOS		\
	$(MESA_HOME)/src*/*.def			\
	$(MESA_HOME)/src*/*.rsp			\
	$(MESA_HOME)/src*/depend		\
	$(MESA_HOME)/src*/Makefile.OpenStep	\
	$(MESA_HOME)/src*/*.[ch]		\
	$(MESA_HOME)/src-tk/*cpp		\
	$(MESA_HOME)/demos/Makefile		\
	$(MESA_HOME)/demos/Imakefile		\
	$(MESA_HOME)/demos/descrip.mms		\
	$(MESA_HOME)/demos/*.[cf]		\
	$(MESA_HOME)/demos/isosurf.dat		\
	$(MESA_HOME)/book/Makefile		\
	$(MESA_HOME)/book/Imakefile		\
	$(MESA_HOME)/book/NOTES			\
	$(MESA_HOME)/book/README		\
	$(MESA_HOME)/book/*.[ch]		\
	$(MESA_HOME)/samples/Makefile		\
	$(MESA_HOME)/samples/Imakefile		\
	$(MESA_HOME)/samples/NOTES		\
	$(MESA_HOME)/samples/README		\
	$(MESA_HOME)/samples/*.rgb		\
	$(MESA_HOME)/samples/*.c		\
	$(MESA_HOME)/widgets-mesa		\
	$(MESA_HOME)/widgets-sgi		\
	$(MESA_HOME)/windows			\
	$(MESA_HOME)/NeXT			\
	$(MESA_HOME)/BeOS			\
	$(MESA_HOME)/mondello			\
	$(MESA_HOME)/util/README		\
	$(MESA_HOME)/util/*.c			\
	$(MESA_HOME)/OpenStep			\
	$(MESA_HOME)/mesa3dfx


tar:
	cd .. ; \
	tar -cvf $(MESA_NAME).tar $(TAR_FILES) ; \
	gzip $(MESA_NAME).tar ; \
	mv $(MESA_NAME).tar.gz $(MESA_HOME)

zip:
	-rm $(MESA_NAME).zip
	cd .. ; \
	zip -r $(MESA_NAME).zip $(TAR_FILES) ; \
	mv $(MESA_NAME).zip $(MESA_HOME)



SRC_FILES = src/Makefile src/*.[ch] include

srctar:
	tar -cvf src.tar $(SRC_FILES) ; \
	gzip src.tar

