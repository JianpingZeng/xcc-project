! DESCRIP.MMS - MMS file for PGP/VMS
! (c) Copyright 1991-93 by Hugh Kennedy. All rights reserved.
!
! The author assumes no liability for damages resulting from the use
! of this software, even if the damage results from defects in this
! software.  No warranty is expressed or implied.
!
! The above notwithstanding, a license to use this software is granted
! to anyone either in the original form or modified on condition that 
! this notice is not removed.
!
! Options Flags:
!
! PGP_DEBUG -  Define if you want the debug version
! PGP_GCC -    Define to use GNU C instead of VAX C
! PGP_RSADIR - Use RSAREF Routines from specified directory.
!
! Modified:	03	Date:	21-Nov-1991	Author: Hugh A.J. Kennedy.
!
! Adapted to use new modules from release 1.3
!
! Modified:	04	Date:	11-Mar-1991	Author:	Hugh A.J. Kennedy
!
! Add test harness program, RSATST as a target.
!
! Modified:	07	Date:	8-Apr-1992	Author:	Hugh A.J. Kennedy
!
! Adapted for new modules from release 1.7.
! Change method of support for GCC (use one options file)
!
! Modified:	08	Date:	25-Jun-1992	Author:	Hugh A.J. Kennedy.
!
! All change (again) for 1.8. Alphabetise module list for ease of comparison.
!
! Modified:	09	Date:	18-Sep-1992	Author: Hugh A.J. Kennedy
!
! Remove references to private string routine versions - no longer reqd.
!
! Modified:	10	Date:	7-Nov-1992	Author:	Hugh A.J. Kennedy
!
! Misc fixes for V2.01
!
! Modified:	11	Date:	28-Jan-1993	Author:	Hugh A.J. Kennedy
!
! Allow use of logicals for source and object directories (keeps things
! cleaner).
!
! Modified:	12	Date:	24-Feb-1993	Author: Hugh A.J. Kennedy
!
! Ammend dependencies to include new header files.
!
! Modified:	13	Date:	10-May-1993	Author:	Hugh A.J. Kennedy
!
! Update support for GNU C. Fix dependencies.
! Add support for RSAREF (untested, as I live outside the US).
!
.ifdef PGP_COMPAT

VFLAGS = ,COMPATIBLE
MD = MD4

.else

MD = MD5

.endif

.ifdef WFLAGS
XFLAGS = /define=($(WFLAGS))
ZFLAGS = /define=($(WFLAGS),EXPORT,NO_ASM,NOSTORE)
.else
ZFLAGS = $(CFLAGS) /define=(EXPORT,NO_ASM,NOSTORE)
.endif

.ifdef PGP_GCC		! Use GNU CC Compiler

CC = GCC
C_PATH_NAME = C_INCLUDE_PATH

.ifdef PGP_RSADIR

CCLIB = ,GNU_CC:[000000]GCCLIB/lib,

.else

CCLIB = GNU_CC:[000000]GCCLIB/lib,

.endif

.else

C_PATH_NAME = C$INCLUDE

.endif
!
! Debugging Support
!
.ifdef PGP_DEBUG

MFLAGS = $(MFLAGS) /debug
LINKFLAGS = $(LINKFLAGS) /debug/exe=$(mms$target)

.ifdef PGP_GCC		! Are we debugging AND using GCC?

DFLAGS = /DEBUG

.else			! No, Debugging with VAX C
                      
DFLAGS = /debug/noopt
     
.endif

YFLAGS = $(XFLAGS)

.else			! Not debugging

.ifdef PGP_GCC		! Use GCC w/o debug

YFLAGS = $(XFLAGS)

.else			! Use VAX C w/o debug

DFLAGS = /opt=noinline
YFLAGS = $(XFLAGS)

.endif

LINKFLAGS = /exe=$(mms$target)
.endif

CFLAGS = $(CFLAGS)$(DFLAGS)$(YFLAGS)

default : obj:pgp.exe
	@	! do nothing...
.first :
	if f$trnlnm("src") .eqs. "" then define src 'f$environment("default")'
	if f$trnlnm("obj") .eqs. "" then define obj 'f$environment("default")'
	if f$trnlnm("$(C_PATH_NAME)") .eqs. "" then define $(C_PATH_NAME) SRC
.last :
	deassign $(C_PATH_NAME)
!
! RSAREF Stuff
!
.ifdef PGP_RSADIR
                                     
RSAOBJS = obj:rsa.obj obj:nn.obj obj:r_random.obj obj:r_stdlib.obj
obj:rsa.obj	: $(PGP_RSADIR)rsa.c src:global.h $(PGP_RSADIR)rsaref.h -
		$(PGP_RSADIR)r_random.h $(PGP_RSADIR)md5.h
	$(CC) $(CFLAGS) /INCLUDE=(src,$(PGP_RSADIR))/define=("static=") $(MMS$SOURCE)
obj:nn.obj	: $(PGP_RSADIR)nn.c src:global.h $(PGP_RSADIR)rsaref.h -
		$(PGP_RSADIR)digit.h
	$(CC) $(CFLAGS) /INCLUDE=(src,$(PGP_RSADIR)) $(MMS$SOURCE)
obj:digit.obj	: $(PGP_RSADIR)digit.c src:global.h $(PGP_RSADIR)rsaref.h -
		$(PGP_RSADIR)nn.h $(PGP_RSADIR)digit.h
	$(CC) $(CFLAGS) /INCLUDE=(src,$(PGP_RSADIR)) $(MMS$SOURCE)
obj:r_random.obj : $(PGP_RSADIR)r_random.c src:global.h -
		$(PGP_RSADIR)rsaref.h $(PGP_RSADIR)r_random.h $(PGP_RSADIR)md5.h
	$(CC) $(CFLAGS) /INCLUDE=(src,$(PGP_RSADIR))/define=("static=") $(MMS$SOURCE)
obj:r_stdlib.obj : $(PGP_RSADIR)r_stdlib.c src:global.h -
		$(PGP_RSADIR)rsaref.h 
	$(CC) $(CFLAGS) /INCLUDE=(src,$(PGP_RSADIR))/define=("static=") $(MMS$SOURCE)

.endif

!
! ZIP Stuff
!
ZIPOBJS = obj:zbits.obj obj:zdeflate.obj obj:zglobals.obj obj:zinflate.obj -
	obj:zip.obj obj:zipup.obj obj:zfile_io.obj obj:ztrees.obj obj:zunzip.obj
ZIPH=	src:zrevisio.h src:ztailor.h src:zunzip.h src:zip.h src:ziperr.h      
obj:zbits.obj : src:zbits.c $(ZIPH)
	$(CC) $(DFLAGS) $(ZFLAGS) $(mms$source)
obj:zdeflate.obj : src:zdeflate.c $(ZIPH)
	$(CC)  $(DFLAGS) $(ZFLAGS) $(mms$source)
obj:zfile_io.obj : src:zfile_io.c $(ZIPH)
	$(CC)  $(DFLAGS) $(ZFLAGS) $(mms$source)
obj:zglobals.obj : src:zglobals.c $(ZIPH)
	$(CC)  $(DFLAGS) $(ZFLAGS) $(mms$source)
obj:zinflate.obj : src:zinflate.c $(ZIPH)
	$(CC)  $(DFLAGS) $(ZFLAGS) $(mms$source)
obj:zip.obj : src:zip.c $(ZIPH)
	$(CC)  $(DFLAGS) $(ZFLAGS) $(mms$source)
obj:zipup.obj : src:zipup.c $(ZIPH)
	$(CC)  $(DFLAGS) $(ZFLAGS) $(mms$source)
obj:ztrees.obj : src:ztrees.c $(ZIPH)
	$(CC)  $(DFLAGS) $(ZFLAGS) $(mms$source)
obj:zunzip.obj : src:zunzip.c $(ZIPH)
	$(CC)  $(DFLAGS) $(ZFLAGS) $(mms$source)
!
! PGP Stuff
!
obj:armor.obj : src:armor.c src:armor.h
obj:charset.obj : src:charset.c src:usuals.h src:language.h src:charset.h -
	src:system.h
obj:config.obj : src:config.c src:usuals.h src:pgp.h
obj:CRYPTO.obj : src:mpilib.h src:mpiio.h src:random.h src:crypto.h -
	src:keymgmt.h src:mdfile.h src:md5.h src:fileio.h src:pgp.h -
	src:rsaglue.h src:platform.h src:usuals.h -
	src:CRYPTO.C
obj:idea.obj : src:idea.h src:pgp.h src:idea.c
obj:FILEIO.obj : src:FILEIO.C src:random.h src:mpilib.h src:mpiio.h -
	src:platform.h src:usuals.h -
	src:fileio.h src:pgp.h 
obj:getopt.obj : src:getopt.c
obj:genprime.obj : src:genprime.c src:genprime.h src:mpilib.h src:random.h -
	src:platform.h src:usuals.h
obj:keyadd.obj : src:mpilib.h src:random.h src:crypto.h src:fileio.h -
	src:keymgmt.h src:keyadd.h src:genprime.h src:rsagen.h src:mpiio.h -
	src:platform.h src:usuals.h -
	src:pgp.h src:language.h src:charset.h src:keyadd.c
obj:keymaint.obj : src:mpilib.h src:random.h src:crypto.h src:fileio.h -
	src:keymgmt.h src:keyadd.h src:genprime.h src:mpiio.h src:pgp.h -
	src:platform.h src:usuals.h -
	src:language.h -
	src:charset.h src:keymaint.c
obj:KEYMGMT.obj : src:mpilib.h src:usuals.h src:random.h src:crypto.h -
	src:fileio.h src:mpiio.h src:pgp.h src:charset.h -
	src:platform.h src:usuals.h -
	src:KEYMGMT.C
obj:MD5.obj : src:md5.h src:md5.C                          
obj:MDFILE.obj : src:mpilib.h src:mdfile.h src:md5.h src:pgp.h -
      	src:platform.h src:usuals.h -
	src:MDFILE.C
obj:MORE.obj : src:MORE.C src:mpilib.h src:pgp.h 
obj:MPIIO.obj : src:MPIIO.C src:mpiio.h src:mpilib.h -
	src:platform.h src:usuals.h
obj:MPILIB.obj : src:MPILIB.C src:mpilib.h src:platform.h src:usuals.h
obj:passwd.obj : src:passwd.c src:random.h src:md5.h src:pgp.h 
obj:PGP.obj : src:mpilib.h src:random.h src:crypto.h src:fileio.h -
	src:keymgmt.h src:keymaint.h src:charset.h src:pgp.h src:config.h -
	src:platform.h src:usuals.h -
	src:PGP.C
obj:RANDOM.obj : src:random.h src:pgp.h src:RANDOM.C
obj:rsagen.obj : src:rsagen.c src:mpilib.h src:genprime.h src:rsagen.h -
	src:platform.h src:usuals.h -
	src:random.h src:rsaglue.h
obj:rsaglue.obj : src:rsaglue.c src:mpilib.h src:mpiio.h src:pgp.h src:rsaglue.h
obj:rsatst.obj : src:rsatst.c src:mpilib.h src:mpiio.h src:genprime.h -
	src:platform.h src:usuals.h -
	src:rsagen.h src:random.h
obj:language.obj : src:language.c src:charset.h src:usuals.h src:fileio.h -
	src:pgp.h
obj:SYSTEM.obj : src:exitpgp.h src:system.h src:pgp.h src:mpilib.h -
	src:mpiio.h src:fileio.h src:charset.h -
	src:platform.h src:usuals.h -
	src:SYSTEM.C
obj:vax.obj : src:vax.mar
!
! RSATST Is the RSA/Multiple Precision Library Test Harness
!
obj:rsatst.exe : src:rsatst.opt obj:rsatst.obj obj:mpilib.obj -
	obj:genprime.obj obj:rsagen.obj obj:mpiio.obj obj:random.obj -
	obj:vax.obj obj:system.obj obj:language.obj obj:fileio.obj
	$(LINK) $(LINKFLAGS) rsatst/opt
!
! Link PGP
!
OBJ1 =	obj:pgp.obj obj:config.obj obj:crypto.obj obj:keymgmt.obj -
	obj:keyadd.obj obj:keymaint.obj obj:fileio.obj obj:mdfile.obj -
	obj:more.obj obj:armor.obj obj:mpilib.obj obj:mpiio.obj -
	obj:getopt.obj obj:genprime.obj obj:rsagen.obj obj:random.obj -
	obj:idea.obj obj:passwd.obj obj:md5.obj obj:system.obj -
	obj:language.obj obj:vax.obj obj:charset.obj obj:rsaglue.obj

obj:pgp.exe : src:pgp.opt $(RSAOBJS) $(OBJ1) $(ZIPOBJS)
	$(LINK) $(LINKFLAGS) src:pgp/opt, $(RSAOBJS) $(CCLIB) src:VAXCRTL/opt
