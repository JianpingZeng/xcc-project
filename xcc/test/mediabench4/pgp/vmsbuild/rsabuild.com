$!x='f$ver(0)'
$!
$!Program:	RSABUILD.COM
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
$!   routines essentially 'edit' and compile arbitrary source files provided
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
$!		94.09.04   Move RSAREF Directory (jis)
$!
$!Description:
$!	Given the presense of appropriately named source files in an
$!      appropriate adjacent directory ([-.RSAREF.SOURCE]), this procedure
$!      will create RSAREFLIB.OLB, which may be used by PGPBUILD in the
$!      construction of a VMS executable file called PGP26.EXE.
$!
$ me = f$environment("procedure")
$ df = f$elem(0,"]",me)+"]"
$ set def 'df'
$ src="[-.rsaref.source]"
$ obj="sys$disk:[]"
$ cflags:=/debug/opt=noinline
$ set ver
$ library /create rsareflib
$!x='f$ver(0)'
$ call compile desc
$ call compile digit
$ call compile md2c
$ call compile md5c
$ call compile nn
$ call compile prime
$ call compile rsa
$ call compile r_encode
$ call compile r_enhanc
$ call compile r_keygen
$ call compile r_random
$ call compile r_stdlib
$ exit
$!
$ compile: subroutine	!p1 is file name
$ sfile = f$parse("''p1'","''src'","sys$disk:[].c")
$ ofile = f$parse("''p1'","''obj'","sys$disk:[].obj;")
$ set ver
$ cc 'cflags' 'sfile'/object='ofile'
$ library rsareflib 'ofile'
$ delete 'ofile'
$!x='f$ver(0)'
$ endsubroutine
