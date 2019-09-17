# Makefile for PGP (unix)
# Most MS-DOS make utilities are lobotomized so they cannot cope with
# a Makefile as complex as this, so MS-DOS is not included here.
#
# PGP 2.6.1 can be compiled to use either the modular exponentiation
# routines that come with RSAREF or those that are built into the
# multiprecision library (mpilib) that comes with PGP.  For UNIX machines
# (and probably VAX/VMS, too), we recommend using the RSAREF routines.
# For MSDOS, we recommend using the mpilib routines (there are assembly
# language routines that mpilib can call under MSDOS making it much faster
# than the C code in RSAREF).  To use the mpilib routines, you
# should:
#	(1) make sure you have the version of RSAREF that comes with
#	    the PGP 2.6.1 distribution,
#	(2) in this makefile, add the compiler switch -DUSEMPILIB to
#	    CFLAGS for your system below,
#	(3) in the RSAREF makefile, add the compiler switch
#	    -DUSEMPILIB to CFLAGS before building RSAREF.
# Note: when you build RSAREF with -DUSEMPILIB, rdemo and dhdemo will
# fail to link, because they will be unable to find the modular
# exponentiation routines.  This is OK.
#
# Alternatively, to build PGP 2.6.1 with the RSAREF exponentiation routines,
#	(1) you can use any version of the March 16, 1994 distribution
#	    of RSAREF.
#	(2) you need not modify this makefile,
#	(3) you need not modify the RSAREF makefile.
#
# Note:  If your system does not have memmove, then you should add memmove.o
# to OBJS_EXT for your rule and send that in as a bug for your platform.
#
# CFLAGS options:
#
# -DHIGHFIRST if building PGP on a big-endian system
# -DDEBUG     to include debugging information
# -DNOTERMIO  if your system has no termios
# -DSVR2      for System V release 2
# -DDYN_ALLOC if your compiler does not support large static arrays
# -DSMALL_MEM if your machine has a small memory (required for MSDOS)
# -DIDEA32    if your int's are 32 bits this is probably faster
#
# -DPORTABLE  and
# -DMPORTABLE to build the portable version of the RSA primitives
#             (ie if no optimized asm versions are available)
#
# Define one of:
# -DMERRITT    Merritt's modmult (fast on risc machines)
# -DPEASANT    Russian peasant modulo multiply algorithm
# -DUPTON      use Upton's modmult algorithm
# -DSMITH      use Smith's modmult
# See also the file platform.h for system defaults
#
# If you don't have a working FIONREAD ioctl you must use one of these:
# -DUSE_SELECT to use select() system call
# -DUSE_NBIO   to use non-blocking read()

CFLAGS= -O -DUNIX -DPORTABLE $(BYTEORDER)

# must set byte order for targets "sysv" and "bsd"
# BYTEORDER= -DHIGHFIRST

CC      = cc
LD      = cc		# Link command
LDFLAGS	= 
CPP     = $(CC) -E
DBG	= -O

# uncomment this for old versions of make
#MAKE	= make

ASM	= $(CC)	-c		# Assembler command

OBJS_EXT=			# ASM obj. files
LIBS_EXT=			# Libararies

PROJ	= pgp

default:
	@echo "To build PGP on your flavour of Unix, type:"
	@echo "        make <system>"
	@echo ""
	@echo "where <system> can be:"
	@echo "	386bsd, 3b1, 3b1_asm, amix-68k-gcc, aux(*), aux-gcc, bsd,"
	@echo "	bsdgcc, djgpp, encore, hpux-68k(*), hpux-68k-ansi,"
	@echo "	hpux-68k-gcc, hpux-pa(*), hpux-pa-ansi, hpux-pa-gcc,"
	@echo "	hpux9-pa-ansi, irix, irix_asm, isc, isc_asm, linux, machten,"
	@echo "	mach_386, mips-ultrix, netbsd, newsasm, newsgcc, next,"
	@echo "	next486, nextHP, os2, osf, qnx4 , rs6000, rt_aos4,"
	@echo "	sco-2.0, sgigcc, sgigcc_asm, solx86gcc, sun386i, sun3asm,"
	@echo "	sun3cc(*), sun3gcc, sun4cc(*), sun4gcc, sun4sunos5gcc,"
	@echo "	sun4acc, sun4sunos5acc, sunspc,	sysv_386, vax-ultrix,"
	@echo "	vax_bsd43, x286(*), xenix386"
	@echo ""
	@echo "For targets marked with (*) you must first get unproto."
	@echo "See setup.doc for further details."

all: $(PROJ)

# For a pure MPILIB version, uncomment the following lines
#RSALIBS =
#RSAOBJS = rsaglue1.o

# For an RSAREF version, uncomment the following lines
RSADIR = ../rsaref
RSALIBDIR = $(RSADIR)/install/unix
RSAINCDIR = -I$(RSADIR)/source -I$(RSADIR)/test $(USEMPILIB)
RSALIBS = $(RSALIBDIR)/rsaref.a
RSAOBJS = rsaglue2.o

# If you want to use MPILIB as a back end to RSAREF, uncomment this line:
USEMPILIB = -DUSEMPILIB

# Assembly-language subroutine dependencies

_80386.o:	80386.S
	$(CPP) $(ASMDEF) 80386.S > _80386.s
	$(ASM) -o $@ _80386.s
	rm -f _80386.s

8086.o: 8086.asm
	cp 8086.asm 8086.s
	$(ASM) -o $@ 8086.s
	rm -f 8086.s

_zmatch.o:	zmatch.S
	$(CPP) $(ASMDEF) zmatch.S > _zmatch.s
	$(ASM) -o $@ _zmatch.s
	rm -f _zmatch.s

sparc.o:	sparc.S
	$(CPP) $(ASMDEF) sparc.S | grep -v '^# ' > _sparc.s
	$(ASM) -o $@ _sparc.s
	rm -f _sparc.s

#mc68020.o:
#	$(CC) -c mc68020.S

ZIPOBJS= zbits.o zdeflate.o zfile_io.o zglobals.o \
	zinflate.o zip.o zipup.o ztrees.o zunzip.o 

OBJ1 =	pgp.o crypto.o keymgmt.o fileio.o \
	mdfile.o more.o armor.o mpilib.o mpiio.o \
	genprime.o rsagen.o random.o idea.o passwd.o \
	md5.o system.o language.o getopt.o keyadd.o \
	config.o keymaint.o charset.o \
	randpool.o noise.o

OBJS =	$(OBJ1) $(ZIPOBJS) $(RSAOBJS) $(OBJS_EXT)
CFLAGS = $(CFLAGS) -I$(RSAINCDIR)

$(PROJ):	$(OBJS)
		$(LD) -o $(PROJ) $(OBJS) $(LDFLAGS) $(LIBS_EXT) $(RSALIBS)

#  Commodore Amiga Running SVR4 2.1p2a with GCC 2.6.0 LER
amix-68k-gcc:
	$(MAKE) all CC=gcc LD=gcc \
	CFLAGS="$(RSAINCDIR) -O2 -DHIGHFIRST -DUNIX \
	-DPORTABLE -DIDEA32"

linux:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT="_80386.o _zmatch.o" \
	CFLAGS="$(RSAINCDIR) -O6 -g3 -DUNIX -DIDEA32 -DASM" 

386bsd:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT="_80386.o _zmatch.o" \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DIDEA32 -DASM -DMAX_NAMELEN=255"

netbsd:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT="_80386.o _zmatch.o" \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DIDEA32 -DASM -DMAX_NAMELEN=255"

# Sun 3 with gcc
# change -traditional-cpp to -traditional for gcc < 2.0
sun3gcc:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT=memmove.o \
	CFLAGS="$(RSAINCDIR) -O -traditional-cpp -DUNIX -DHIGHFIRST \
	-DIDEA32 -DPORTABLE"

sun3asm:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT="mc68020.o memmove.o" \
	CFLAGS="$(RSAINCDIR) -O -traditional-cpp -DUNIX -DHIGHFIRST -DIDEA32"

# Sun 3 with standard cc: compile with unproto
sun3cc: unproto/cpp
	$(MAKE) all CC=cc LD=cc OBJS_EXT=memmove.o \
	CFLAGS="$(RSAINCDIR) -Qpath unproto -O -DUNIX -DHIGHFIRST \
	-DIDEA32 -DPORTABLE"

# Sun 4 SPARC with gcc (tested with gcc 1.39 and newer, sparc.s can not be
# used with older versions of gcc)
# change -traditional-cpp to -traditional for gcc < 2.0
sun4gcc:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT="sparc.o memmove.o" \
	CFLAGS="$(RSAINCDIR) -O -traditional-cpp -DUNIX -DHIGHFIRST -DIDEA32"

# Sun 4 SPARC with standard cc: compile with unproto
sun4cc: unproto/cpp
	$(MAKE) all CC=cc LD=cc OBJS_EXT="sparc.o memmove.o"  \
	CFLAGS="$(RSAINCDIR) -Qpath unproto -Dconst= -O -DUNIX \
	-DHIGHFIRST -DIDEA32"

# Sun 4 running Sunos5 (Solaris)...
sun4sunos5gcc:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT=sparc.o \
	CFLAGS="$(RSAINCDIR) -O -traditional-cpp -DSOLARIS -DUNIX -DHIGHFIRST \
	-DIDEA32" ASMDEF=-DSYSV

# Sun 4 using SunPro C compiler
sun4acc:
	$(MAKE) all CC=acc LD=acc CPP="cc -E" OBJS_EXT="sparc.o memmove.o" \
	CFLAGS="-w $(RSAINCDIR) -xO4 -DUNIX -DHIGHFIRST -DIDEA32" \
 
# Sun 4 running SunOs 5 (Solaris) using SunPro C compiler
sun4sunos5acc:
	$(MAKE) all CC=cc LD=gcc OBJS_EXT=sparc.o \
	CFLAGS="-w $(RSAINCDIR) -xO4 -DSOLARIS -DUNIX -DHIGHFIRST -DIDEA32" \
	ASMDEF=-DSYSV

sun386i:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT="_80386.o memmove.o" \
	CFLAGS="$(RSAINCDIR) -I. -O -DUNIX -DIDEA32 -DNOTERMIO" \
	ASMDEF=-DSYSV

sunspc:
	$(MAKE) all CC="ccspc -B/1.8.6/sun4 -ansi -w -I/usr/include" \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DIDEA32 -DHIGHFIRST -DUNIT32 \
	-DMERRITT" OBJS_EXT=sparc.o

# x86 running Sunos5 (Solaris)...
solx86gcc:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT=_80386.o \
	CFLAGS="$(RSAINCDIR) -O -traditional-cpp -DSOLARIS -DUNIX  -DIDEA32" \
	ASMDEF=-DSYSV

# Sony newsos v3 for m68k, with gcc
# change -traditional-cpp to -traditional for gcc < 2.0
newsgcc:
	$(MAKE) all CC=gcc LD=gcc \
	CFLAGS="$(RSAINCDIR) -O -I. -traditional-cpp -DNOTERMIO -D_BSD \
	-DUNIX -DHIGHFIRST -DIDEA32 -DPORTABLE"

#  with asm
newsasm:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT=mc68020.s CFLAGS="$(RSAINCDIR) -O \
	-I. -traditional-cpp -DNOTERMIO -D_BSD -DUNIX -DHIGHFIRST -DIDEA32"

qnx4:
	$(MAKE) all \
	CFLAGS="$(RSAINCDIR) -3 -O -I. -b -DNO_PARAM_H -DUNIX -DIDEA32 \
	-DPORTABLE -DMPORTABLE $(BYTEORDER) -DMAX_NAMELEN=255"

encore:
	$(MAKE) all CC=gcc LD=gcc CFLAGS="$(RSAINCDIR) -O -DUNIX -DPORTABLE \
	-DMPORTABLE -DUSE_NBIO -DIDEA32 -DUPTON -DHIGHFIRST"

sysv:
	$(MAKE) all CPP=/lib/cpp \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DPORTABLE -DUSE_NBIO $(BYTEORDER)"

# optimized version with 80386.S
sysv_386:
	$(MAKE) all CPP="/lib/cpp -DSYSV" OBJS_EXT="_80386.o _zmatch.o" \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DIDEA32 -DUSE_NBIO -DASM"

xenix386:
	$(MAKE) all CPP=/lib/cpp OBJS_EXT="_80386.o _zmatch.o" \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DSVR2 -DIDEA32 -DUSE_NBIO -DASM"

# Interactive Unix SVR3/386 version 3.2 with gcc
isc:
	$(MAKE) all CC=gcc LD=gcc LDFLAGS="-lcposix -lrpc" OBJS_EXT=memmove.o \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DIDEA32 -DUSE_NBIO -DPORTABLE \
	-DNO_ITIMER"

isc_asm:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT="_80386.o memmove.o" ASMDEF=-DSYSV \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DIDEA32 -DUSE_NBIO -DNO_ITIMER" \
	LDFLAGS="-lcposix -lrpc"

mach_386:
	$(MAKE) all CC=gcc LD=gcc CPP=/lib/cpp OBJS_EXT=_80386.o \
	CFLAGS="$(RSAINCDIR) -O -I. -DMACH -DUNIX -DIDEA32 -DNOTERMIO"

machten:
	$(MAKE) all CC=gcc LD=gcc CPP=/lib/cpp OBJS_EXT=mc68020.o \
	CFLAGS="$(RSAINCDIR) -O -I. -DMACH -DUNIX -DHIGHFIRST \
	-DIDEA32 -DNOTERMIO"

sco-2.0:
	$(MAKE) all CC=gcc LD=gcc CPP=/usr/lib/xcpp OBJS_EXT=_80386.o \
	ASMDEF=-DSYSV CFLAGS="$(RSAINCDIR) -Dsco -O -DUNIX -DIDEA32 -DUSE_NBIO"

# Xenix 286
x286:
	$(MAKE) all CC="sh ccc.x286 -M2l" LD="cc -M2l" ASM="cc -M2l" \
	OBJS_EXT=8086.o  LDFLAGS="-F 3000" \
	CFLAGS="$(RSAINCDIR) -LARGE -Ot -DUNIX -DNOPROTO -DSMALL_MEM \
	-DDYN_ALLOC -DUSE_NBIO -DSVR2"

# AIX/386 v.1.3
aix386:
	$(MAKE) all CPP="/lib/cpp -DSYSV" \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DIDEA32 -DUSE_NBIO -DSYSV -DPORTABLE"

# AIX/370  (like general SysV)
aix370:
	$(MAKE) all CPP=/lib/cpp \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DIDEA32 -DUSE_NBIO -DSYSV -DHIGHFIRST \
	-DPORTABLE"

3b1:
	$(MAKE) all CC=gcc LD=gcc CPP=/usr/lib/cpp \
	CFLAGS="$(RSAINCDIR) -O -I. -DUNIX -DSVR2 -DPORTABLE -DUSE_NBIO \
	-DHIGHFIRST -DMAX_NAMELEN=14"

3b1_asm:
	$(MAKE) all CC=gcc LD=gcc CPP=/usr/lib/cpp OBJS_EXT=3b168010.o \
	CFLAGS="$(RSAINCDIR) -O -I. -DUNIX -DSVR2 -DUSE_NBIO -DHIGHFIRST \
	-DMAX_NAMELEN=14"

# Silicon Graphics Iris IRIX
sgigcc:
	$(MAKE) all CC=gcc LD=gcc CPP=/usr/lib/cpp \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DPORTABLE -DUSE_NBIO -DHIGHFIRST"

# SGI with assembler modules
sgigcc_asm:
	$(MAKE) all CC=gcc LD=gcc CPP=/usr/lib/cpp OBJS_EXT="r3000.o r3kd.o" \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DUSE_NBIO -DHIGHFIRST"

irix:
	$(MAKE) all CC=cc LD=cc \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DPORTABLE -DUSE_NBIO -DHIGHFIRST -acpp"

irix_asm:
	$(MAKE) all CC=cc LD=cc OBJS_EXT="r3000.o r3kd.o" \
	CFLAGS="$(RSAINCDIR) -O -g -DUNIX -DUSE_NBIO -DHIGHFIRST -acpp"

# Users of PGP versions <2.5 will expect a target 'hpux' to build for
# a snake (HP Series 700 RISC machine).  Don't disappoint them.
hpux: hpux-pa-ansi

# HP series 700 pa-risc running HP/UX with HP's ANSI cc
# UNTESTED for PGP 2.5
hpux-pa-ansi:
	$(MAKE) all CPP=/usr/lib/cpp \
	CFLAGS="$(RSAINCDIR) -Aa +O3 +Obb5000 -D_HPUX_SOURCE -DHIGHFIRST \
	-DUNIX -DPORTABLE -DUSE_SELECT -DIDEA32 -DMERRITT"

# HP series 700/800 pa-risc running HP/UX 9.* with cc
hpux9-pa-ansi:
	$(MAKE) all CPP=/lib/cpp \
	CFLAGS="$(RSAINCDIR) -Ae -D_HPUX_SOURCE +O3 +Onolimit -DHIGHFIRST \
	-DUNIX -DPORTABLE -DUSE_SELECT -DIDEA32 -DMERRITT"


# HP series 700 pa-risc running HP/UX with gcc
# UNTESTED for PGP 2.5
hpux-pa-gcc:
	$(MAKE) all CC=gcc LD=gcc \
	CFLAGS="$(RSAINCDIR) -O2 -D_HPUX_SOURCE -DHIGHFIRST -DUNIX \
	-DPORTABLE -DUSE_SELECT -DIDEA32 -DMERRITT"

# HP series 700 pa-risc running HP/UX with HP's standard cc: use unproto
# UNTESTED for PGP 2.5
hpux-pa: unproto/cpp
	$(MAKE) all \
	CFLAGS="-tp,./unproto/cpp $(RSAINCDIR) -DHIGHFIRST \
	-DUNIX -DPORTABLE -DUSE_SELECT -DIDEA32 -DMERRITT"

# HP series 300 mc68k running HP/UX with HP's ANSI cc
hpux-68k-ansi:
	$(MAKE) all \
	CFLAGS="$(RSAINCDIR) -Aa +O2 -D_HPUX_SOURCE -DHIGHFIRST -DUNIX \
	-DPORTABLE -DUSE_SELECT -DIDEA32"

# HP series 300 mc68k running HP/UX with gcc
hpux-68k-gcc:
	$(MAKE) all CC=gcc LD=gcc \
	CFLAGS="$(RSAINCDIR) -O2 -D_HPUX_SOURCE -DHIGHFIRST -DUNIX \
	-DPORTABLE -DUSE_SELECT -DIDEA32"

# HP series 300 mc68k running HP/UX with HP's standard cc: use unproto
# UNTESTED for PGP 2.5
hpux-68k: unproto/cpp
	$(MAKE) all \
	CFLAGS="-tp,./unproto/cpp $(RSAINCDIR) -DHIGHFIRST \
	-DUNIX -DPORTABLE -DUSE_SELECT -DIDEA32"

# VAX Ultrix 4.2 BSD rev. 96, system #3 with gcc
vax-ultrix:
	$(MAKE) all CC=gcc LD=gcc \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -DPORTABLE -DUSE_SELECT \
	-DSIG_IGN=1 -DSIG_DFL=0"

# DEC Ultrix 4.2 BSD with gcc with MIPSco processors
#
# To use dec's cc on mips you should
# uncomment the next two lines because you can't compile r3000.c 
#r3000.o: r3000.s
#	$(AS) -o $@ r3000.s
#
#mips-ultrix:
#	$(MAKE) all CC=gcc LD=gcc OBJS_EXT="r3kd.o r3000.o"\
#	CFLAGS="$(RSAINCDIR) $(DBG) -DUNIX -DUSE_SELECT -DIDEA32"

mips-ultrix:
	$(MAKE) all CC=gcc LD=gcc \
	CFLAGS="$(RSAINCDIR) $(DBG) -DUNIX -DPORTABLE \
	   -DMPORTABLE -DUSE_SELECT -DIDEA32"

# RS6000 running AIX
rs6000:
	$(MAKE) all CFLAGS="$(RSAINCDIR) -O -DUNIX -DNOTERMIO -DPORTABLE \
	-DMODERN -DHIGHFIRST -DIDEA32"

next:
	$(MAKE) all \
	CFLAGS="$(RSAINCDIR) -O -arch m68k -DNEXT -DUNIX -DHIGHFIRST \
	-DMACH -DPORTABLE -DNOTERMIO"

next486:
	$(MAKE) all \
	CFLAGS="$(RSAINCDIR) -O -arch i386 -DNEXT -DUNIX -DMACH -DPORTABLE \
	-DIDEA32 -DNOTERMIO"

nextHP:
	$(MAKE) all \
	CFLAGS="$(RSAINCDIR) -O -arch hppa -DNEXT -DUNIX -DMACH -DPORTABLE \
	-DNOTERMIO -DHIGHFIRST"

bsdgcc:
	$(MAKE) all CC=gcc LD=gcc \
	CFLAGS="$(RSAINCDIR) -O -I. -DNOTERMIO -D_BSD -DUNIX -DPORTABLE \
	-DUSE_NBIO $(BYTEORDER)"

bsd:
	$(MAKE) all "LD=cc -s" \
	CFLAGS="$(RSAINCDIR) -O -I. -B/lib/ -DNOTERMIO -D_BSD -DUNIX \
	-DPORTABLE -DUSE_NBIO $(BYTEORDER)"

sequent: unproto/cpp
	$(MAKE) all "LD=cc -s -j" CPP=/usr/lib/cpp \
	CFLAGS="$(RSAINCDIR) -j -O -I. -Yp,unproto -DUNIX -DNOTERMIO \
	-DPORTABLE -DUSE_NBIO $(BYTEORDER)"

# RT running bsd with gcc
rt_aos4:
	$(MAKE) all CC=gcc LD=gcc \
	CFLAGS="$(RSAINCDIR) -O -DUNIX -D_BSD -DMAX_NAMELEN=255 -DPORTABLE \
	-DHIGHFIRST -DNOTERMIO -I."

# Vax running bsd with gcc
vax_bsd43:
	$(MAKE) all CC=gcc LD=gcc \
	CFLAGS="$(RSAINCDIR) -O -I. -DUNIX -D_BSD -DMAX_NAMELEN=255 \
	-DNOTERMIO -DPORTABLE"

# The Open Software Foundation's OSF/1
osf:
	$(MAKE) all \
	CFLAGS="$(RSAINCDIR) $(DBG) -I. -D_BSD -DUNIX -DNOTERMIO \
	-DPORTABLE $(BYTEORDER)"

# A/UX 3.0 using unproto (mine is invoked as a cpp using the CFLAGS shown
# I placed the cpp in a subdirectory of /src/pgp called unproto
aux: unproto/cpp
	$(MAKE) all CC=cc OBJS_EXT=memmove.o \
	LDFLAGS="-lposix -lbsd" \
	CFLAGS="$(RSAINCDIR) -B unproto/ -tp -DSVR2 \
	-DHIGHFIRST -DUNIX -DPORTABLE -DUSE_SELECT -DUPTON"
aux-gcc:
	$(MAKE) all CC=gcc LD=gcc OBJS_EXT=memmove.o \
	LDFLAGS="-lposix -lbsd" \
	CFLAGS="$(RSAINCDIR) -O2 -DSVR2 -DHIGHFIRST -DUNIX -DPORTABLE \
	-DUSE_SELECT -DUPTON"

# optimized version with 80386.S for OS/2 2.x.
# Requires emx 0.8h (i.e. gcc 2.5.7 or later) and GNU make 3.71 for OS/2
os2:
# make rsaref first
	$(MAKE) -C $(RSALIBDIR) all CC="gcc -Zomf" O="obj" \
	RANLIB="echo" LIB="emxomfar" SRCLIB="rsaref.lib"
# make pgp now
	$(MAKE) all PROJ=pgp.exe OBJS_EXT="_80386.o _zmatch.o" \
	CC="gcc -Zomf" CFLAGS="$(RSAINCDIR) -O -DOS2 -DASM -DIDEA32" \
	ASM="gcc -Zomf -c" LD="gcc -Zomf -Zsys" LDFLAGS="-s pgp.def" \
	RSALIBS=$(RSALIBDIR)/rsaref.lib


# DJGPP, DJ Delorie's port of GNU C to MS-DOA (thanks DJ!).  Tested with dmake.
# You may have to rehack this -- I'm not good with makefiles.
djgpp:
	$(MAKE) "OBJS_EXT=_80386.o _zmatch.o" CC=gcc LD=gcc \
	"CFLAGS=-O2 -DSMALL_MEM -DASM -DIDEA32" all

#
# unproto for K&R compilers
#
# unproto was posted on comp.sources.misc: v23i012 v23i013
#
# unpack the unproto package in subdirectory unproto
#

# unproto: needs preprocessed input
unproto/unproto::
	cd unproto && $(MAKE) PROG=unproto PIPE=

# cpp: pipes through /lib/cpp
unproto/cpp::
	cd unproto && $(MAKE)

clean:
	-rm -f *.o $(PROJ) core a.out tags *.err

tags:
	ctags *.c *.h


## Dependencies ##
armor.o : armor.c mpilib.h usuals.h platform.h fileio.h mpiio.h language.h \
  pgp.h more.h armor.h crypto.h
charset.o : charset.c usuals.h language.h charset.h system.h 
config.o : config.c usuals.h fileio.h pgp.h more.h armor.h config.h \
  charset.h 
crypto.o : crypto.c mpilib.h usuals.h platform.h mpiio.h random.h idea.h \
  crypto.h keymgmt.h keymaint.h mdfile.h md5.h fileio.h charset.h language.h \
  pgp.h more.h armor.h exitpgp.h zipup.h rsaglue.h
fileio.o : fileio.c random.h usuals.h mpilib.h platform.h mpiio.h fileio.h \
  language.h pgp.h more.h armor.h exitpgp.h charset.h system.h 
genprime.o : genprime.c mpilib.h usuals.h platform.h genprime.h random.h 
getopt.o : getopt.c getopt.h 
idea.o : idea.c idea.h usuals.h 
keyadd.o : keyadd.c mpilib.h usuals.h platform.h crypto.h fileio.h \
  keymgmt.h charset.h language.h pgp.h more.h armor.h exitpgp.h keyadd.h \
  keymaint.h 
keymaint.o : keymaint.c mpilib.h usuals.h platform.h random.h crypto.h \
  fileio.h keymgmt.h keymaint.h mpiio.h charset.h language.h pgp.h more.h \
  armor.h 
keymgmt.o : keymgmt.c system.h mpilib.h usuals.h platform.h idea.h random.h \
  crypto.h fileio.h keymgmt.h rsagen.h mpiio.h language.h pgp.h more.h \
  armor.h md5.h charset.h keymaint.h 
language.o : language.c usuals.h fileio.h language.h pgp.h more.h armor.h \
  charset.h 
md5.o : md5.c md5.h 
mdfile.o : mdfile.c mpilib.h usuals.h platform.h mdfile.h md5.h fileio.h \
  language.h pgp.h more.h armor.h 
more.o : more.c system.h mpilib.h usuals.h platform.h language.h fileio.h \
  pgp.h more.h armor.h charset.h 
mpiio.o : mpiio.c mpilib.h # usuals.h platform.h mpiio.h pgp.h more.h armor.h 
mpilib.o : mpilib.c mpilib.h # usuals.h platform.h 
passwd.o : passwd.c random.h usuals.h md5.h language.h pgp.h more.h armor.h 
pgp.o : pgp.c system.h mpilib.h usuals.h platform.h random.h crypto.h \
  fileio.h keymgmt.h language.h pgp.h more.h armor.h exitpgp.h charset.h \
  getopt.h config.h keymaint.h keyadd.h rsaglue.h
random.o : random.c system.h random.h usuals.h language.h 
rsagen.o : rsagen.c mpilib.h usuals.h platform.h genprime.h rsagen.h \
  random.h rsaglue.h
rsaglue.o : rsaglue.c mpilib.h mpiio.h pgp.h rsaglue.h
system.o : system.c exitpgp.h system.h charset.h 
zbits.o : zbits.c zip.h ztailor.h ziperr.h 
zdeflate.o : zdeflate.c zunzip.h usuals.h system.h zip.h ztailor.h ziperr.h 
zfile_io.o : zfile_io.c zunzip.h usuals.h system.h 
zglobals.o : zglobals.c zip.h ztailor.h ziperr.h 
zinflate.o : zinflate.c zunzip.h usuals.h system.h exitpgp.h 
zip.o : zip.c usuals.h fileio.h language.h pgp.h more.h armor.h exitpgp.h 
zipup.o : zipup.c zip.h ztailor.h ziperr.h zrevisio.h 
ztrees.o : ztrees.c zip.h ztailor.h ziperr.h 
zunzip.o : zunzip.c zunzip.h usuals.h system.h 
r3000.o : r3000.c mpilib.h usuals.h platform.h lmul.h 
