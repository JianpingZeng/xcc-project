$!x='f$ver(0)'
$!
$!Program:	PGPBUILD.COM
$!
$!Author:	David G. North, CCP
$!		1333 Maywood Ct
$!		Plano, Texas  75023-1914
$!		(214) 881-1553
$!		d_north@ondec.lonestar.org
$!
$!Date:		94.05.19
$!
$!Revisions:
$!  Who		Date	Description
$!  D.North	940519	Public Release
$!
$!License:
$!    Ownership of and rights to these programs is retained by the author(s).
$!    Limited license to use and distribute the software in this library is
$!    hereby granted under the following conditions:
$!      1. Any and all authorship, ownership, copyright or licensing
$!         information is preserved within any source copies at all times.
$!      2. Under absolutely *NO* circumstances may any of this code be used
$!         in any form for commercial profit without a written licensing
$!         agreement from the author(s).  This does not imply that such
$!         a written agreement could not be obtained.
$!      3. Except by written agreement under condition 2, and subject to
$!         condition 5, source shall be freely provided with all binaries.
$!      4. Library contents may be transferred or copied in any form so
$!         long as conditions 1, 2, 3, and 5 are met.  Nominal charges may
$!         be assessed for media and transferral labor without such charges
$!         being considered 'commercial profit' thereby violating condition 2.
$!      5. THESE ROUTINES ARE FOR U.S. INTERNAL USE ONLY.  The Author WILL
$!         NOT BE HELD ACCOUNTABLE FOR _YOUR_ EXPORTING THESE ROUTINES FROM
$!         THE UNITED STATES.  EXPORT OF THESE ROUTINES FROM THE U.S. TO ANY
$!         FOREIGN COUNTRY MAY BE A VIOLATION OF ITAR REGULATIONS AND MAY
$!         SUBJECT YOU TO PROSECUTION BY THE U.S. GOVERNMENT, POSSIBLY RESULTING
$!         IN YOUR INCARCERATION.
$!
$!Extended conditions:
$!   These routines were designed to build the RSAREF library distributed
$!   with MIT PGP V2.6, and the MIT PGP V2.6 sources AS DISTRIBUTED.  The
$!   author explicitly denies any responsibilities regarding your rights to
$!   posses, to use, or to modify ANYTHING called PGP or RSAREF.  These
$!   routines simply compile arbitrary source files provided
$!   by you.  The content of those source files, and the source files
$!   resulting from the editing operations are YOUR SOLE RESPONSIBILITY with
$!   respect to any legal ramifications that may exist.
$!
$!Warranty:
$!   These programs are distributed in the hopes that they will be useful, but
$!   WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
$!   or FITNESS FOR A PARTICULAR PURPOSE.
$!
$!Revisions:	When	   What
$!		94.05.16   Initial release
$!
$!		94.09.04   Move RSAREF directory (jis)
$!
$!Description:
$!	Given the presense of appropriately named source files in an
$!      appropriate adjacent directory ([-.PGP26.SRC]), this procedure
$!      will attempt to construct a VMS executable file called PGP26.EXE.
$!
$ me = f$environment("procedure")
$ df = f$elem(0,"]",me)+"]"
$ set def 'df'
$ src = "[-.src]"
$ rsa = "[-.rsaref.source]"
$ obj = "sys$disk:[]"
$ cflags:=/debug/opt=noinline
$!
$! Now compile stuff
$!
$ call compile pgp
$ call compile config
$ call compile crypto
$ call compile keymgmt
$ call compile keyadd
$ call compile keymaint
$ call compile fileio
$ call compile mdfile
$ call compile more
$ call compile armor
$ call compile mpilib
$ call compile mpiio
$ call compile getopt
$ call compile genprime
$ call compile rsagen
$ call compile random
$ call compile idea
$ call compile passwd
$ call compile md5
$ call compile system
$ call compile language
$ macro/object='obj'vax.obj 'src'vax.mar
$ call compile charset
$ call compile rsaglue2
$ call compile noise
$ call compile randpool
$ call compile zbits "/define=(EXPORT,NO_ASM,NOSTORE)"
$ call compile zdeflate "/define=(EXPORT,NO_ASM,NOSTORE)"
$ call compile zglobals "/define=(EXPORT,NO_ASM,NOSTORE)"
$ call compile zinflate "/define=(EXPORT,NO_ASM,NOSTORE)"
$ call compile zip "/define=(EXPORT,NO_ASM,NOSTORE)"
$ call compile zipup "/define=(EXPORT,NO_ASM,NOSTORE)"
$ call compile zfile_io "/define=(EXPORT,NO_ASM,NOSTORE)"
$ call compile ztrees "/define=(EXPORT,NO_ASM,NOSTORE)"
$ call compile zunzip "/define=(EXPORT,NO_ASM,NOSTORE)"
$ exit
$!Last Modified:  19-MAY-1993 07:08:47.39
$1
$!
$ compile: subroutine	!p1 is file name
$ sfile = f$parse("''p1'","''src'","sys$disk:[].c")
$ ofile = f$parse("''obj'","''p1'","sys$disk:[].obj;")
$ set ver
$ define/user c$include 'src','rsa'
$ define/user vaxc$include sys$share:,'src','rsa'
$ define/user sys sys$share:
$ cc 'cflags' 'p2' 'sfile'/object='ofile'
$!x='f$ver(0)'
$ endsubroutine
