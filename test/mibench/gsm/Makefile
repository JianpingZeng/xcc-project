#
# Copyright 1992 by Jutta Degener and Carsten Bormann, Technische
# Universitaet Berlin.  See the accompanying file "COPYRIGHT" for
# details.  THERE IS ABSOLUTELY NO WARRANTY FOR THIS SOFTWARE.
#


# Machine dependent flags you must configure to port

SASR	= -DSASR=1
######### Define SASR if >> is a signed arithmetic shift (-1 >> 1 == -1)

# MULHACK = -DUSE_FLOAT_MUL
######### Define this if your host multiplies floats faster than integers,
######### e.g. on a SPARCstation.

# FAST	= -DFAST
######### Define together with USE_FLOAT_MUL to enable the GSM library's
######### approximation option for incorrect, but good-enough results.


# Choose a compiler.  The code works both with ANSI and K&R-C.
# Use -DNeedFunctionPrototypes to compile with, -UNeedFunctionPrototypes to
# compile without, function prototypes in the header files.
#
# You can use the -DSTUPID_COMPILER to circumvent some compilers'
# static limits regarding the number of subexpressions in a statement.

# CC		= cc
# CCFLAGS 	= -c -DSTUPID_COMPILER

# CC		= /usr/lang/acc
# CCFLAGS 	= -c -O

#CC		= ../../bin/ssbig-na-sstrix-gcc -static -ansi -pedantic 
#CC		= /home/lqingrui/idriver/gcc.sh -static -ansi -pedantic 

include ../Make_Config

#static compilation 1 (there is a 2 below)
CC		= $(COMPILER)
#CCFLAGS 	= -c -O2 -DNeedFunctionPrototypes=1 $(IDEMFLAG) -static --target=armv7
CCFLAGS 	= -DNeedFunctionPrototypes=1 $(IDEM_CFLAGS)
LD 			= ${IDEM_LINKER}
GCC   		= $(IDEM_LINKER)

# If your compiler needs additional flags/libraries, regardless of
# the source compiled, configure them here.

# CCINC	= -I/usr/gnu/lib/gcc-2.1/gcc-lib/sparc-sun-sunos4.1.2/2.1/include
######### Includes needed by $(CC)

# LDINC	= -L/usr/gnu/lib/gcc-2.1/gcc-lib/sparc-sun-sunos4.1.2/2.1
######### Library paths needed by $(LD)

# LDLIB	= -lgcc
######### Additional libraries needed by $(LD)


# Where do you want to install libraries, binaries, a header file
# and the manual pages?
#
# Leave INSTALL_ROOT empty (or just don't execute "make install") to
# not install gsm and toast outside of this directory.

INSTALL_ROOT	= 

# Where do you want to install the gsm library, header file, and manpages?
#
# Leave GSM_INSTALL_ROOT empty to not install the GSM library outside of
# this directory.

GSM_INSTALL_ROOT = $(INSTALL_ROOT)
GSM_INSTALL_LIB = $(GSM_INSTALL_ROOT)/lib
GSM_INSTALL_INC = $(GSM_INSTALL_ROOT)/inc
GSM_INSTALL_MAN = $(GSM_INSTALL_ROOT)/man/man3


# Where do you want to install the toast binaries and their manpage?
#
# Leave TOAST_INSTALL_ROOT empty to not install the toast binaries outside
# of this directory.

TOAST_INSTALL_ROOT	  = $(INSTALL_ROOT)
TOAST_INSTALL_BIN = $(TOAST_INSTALL_ROOT)/bin
TOAST_INSTALL_MAN = $(TOAST_INSTALL_ROOT)/man/man1

#  Other tools

SHELL		= /bin/sh
LN		= ln
BASENAME 	= basename
AR		= ${IDEM_AR}
ARFLAGS		= cr
RMFLAGS		=
FIND		= find
COMPRESS 	= compress
COMPRESSFLAGS 	= 

################################
# Modified by C. Lee--on SunOS5
RANLIB 	= true
# RANLIB	 	= ranlib
# End of modification
################################
#
#    You shouldn't have to configure below this line if you're porting.
# 


# Local Directories

ROOT	= .
ADDTST	= $(ROOT)/add-test
TST	= $(ROOT)/tst
MAN	= $(ROOT)/man
BIN	= $(ROOT)/bin
SRC	= $(ROOT)/src
LIB	= $(ROOT)/lib
TLS	= $(ROOT)/tls
INC	= $(ROOT)/inc

# Flags

# DEBUG	= -DNDEBUG
######### Remove -DNDEBUG to enable assertions.

CFLAGS	= $(CCFLAGS) $(SASR) $(DEBUG) $(MULHACK) $(FAST) $(CCINC) -I$(INC)
######### It's $(CC) $(CFLAGS)

LFLAGS	= $(LDFLAGS) $(LDINC) $(IDEM_LDFLAGS)
######### It's $(LD) $(LFLAGS)


# Targets

LIBGSM	= $(LIB)/libgsm.a

TOAST	= $(BIN)/toast
UNTOAST	= $(BIN)/untoast
TCAT	= $(BIN)/tcat

# Headers

GSM_HEADERS =	$(INC)/gsm.h

HEADERS	=	$(INC)/proto.h		\
		$(INC)/unproto.h	\
		$(INC)/config.h		\
		$(INC)/private.h	\
		$(INC)/gsm.h		\
		$(INC)/toast.h		\
		$(TLS)/taste.h

# Sources

GSM_SOURCES =	$(SRC)/add.c		\
		$(SRC)/code.c		\
		$(SRC)/debug.c		\
		$(SRC)/decode.c		\
		$(SRC)/long_term.c	\
		$(SRC)/lpc.c		\
		$(SRC)/preprocess.c	\
		$(SRC)/rpe.c		\
		$(SRC)/gsm_destroy.c	\
		$(SRC)/gsm_decode.c	\
		$(SRC)/gsm_encode.c	\
		$(SRC)/gsm_explode.c	\
		$(SRC)/gsm_implode.c	\
		$(SRC)/gsm_create.c	\
		$(SRC)/gsm_print.c	\
		$(SRC)/gsm_option.c	\
		$(SRC)/short_term.c	\
		$(SRC)/table.c

TOAST_SOURCES = $(SRC)/toast.c 		\
		$(SRC)/toast_lin.c	\
		$(SRC)/toast_ulaw.c	\
		$(SRC)/toast_alaw.c	\
		$(SRC)/toast_audio.c

SOURCES	=	$(GSM_SOURCES)		\
		$(TOAST_SOURCES)	\
		$(ADDTST)/add_test.c	\
		$(TLS)/bitter.c		\
		$(TLS)/bitter.dta	\
		$(TLS)/taste.c		\
		$(TLS)/sweet.c		\
		$(TST)/cod2lin.c	\
		$(TST)/cod2txt.c	\
		$(TST)/gsm2cod.c	\
		$(TST)/lin2cod.c	\
		$(TST)/lin2txt.c

# All preprocessed files.
PREDS = $(SRC)/add.i        \
         $(SRC)/code.i       \
         $(SRC)/debug.i      \
         $(SRC)/decode.i     \
         $(SRC)/long_term.i  \
         $(SRC)/lpc.i        \
         $(SRC)/preprocess.i \
         $(SRC)/rpe.i        \
         $(SRC)/gsm_destroy.i    \
         $(SRC)/gsm_decode.i \
         $(SRC)/gsm_encode.i \
         $(SRC)/gsm_explode.i    \
         $(SRC)/gsm_implode.i    \
         $(SRC)/gsm_create.i \
         $(SRC)/gsm_print.i  \
         $(SRC)/gsm_option.i \
         $(SRC)/short_term.i \
         $(SRC)/table.i      \
         $(SRC)/toast.i      \
         $(SRC)/toast_lin.i  \
         $(SRC)/toast_ulaw.i \
         $(SRC)/toast_alaw.i \
         $(SRC)/toast_audio.i \
         $(ADDTST)/add_test.i \
         $(TLS)/bitter.i     \
         $(TLS)/taste.i      \
         $(TLS)/sweet.i      \
         $(TST)/cod2lin.i    \
         $(TST)/cod2txt.i    \
         $(TST)/gsm2cod.i    \
         $(TST)/lin2cod.i    \
         $(TST)/lin2txt.i

# Object files

GSM_OBJECTS =	$(SRC)/add.o		\
		$(SRC)/code.o		\
		$(SRC)/debug.o		\
		$(SRC)/decode.o		\
		$(SRC)/long_term.o	\
		$(SRC)/lpc.o		\
		$(SRC)/preprocess.o	\
		$(SRC)/rpe.o		\
		$(SRC)/gsm_destroy.o	\
		$(SRC)/gsm_decode.o	\
		$(SRC)/gsm_encode.o	\
		$(SRC)/gsm_explode.o	\
		$(SRC)/gsm_implode.o	\
		$(SRC)/gsm_create.o	\
		$(SRC)/gsm_print.o	\
		$(SRC)/gsm_option.o	\
		$(SRC)/short_term.o	\
		$(SRC)/table.o

TOAST_OBJECTS =	$(SRC)/toast.o 		\
		$(SRC)/toast_lin.o	\
		$(SRC)/toast_ulaw.o	\
		$(SRC)/toast_alaw.o	\
		$(SRC)/toast_audio.o

OBJECTS =	 $(GSM_OBJECTS) $(TOAST_OBJECTS)

# Manuals

GSM_MANUALS =	$(MAN)/gsm.3		\
		$(MAN)/gsm_explode.3	\
		$(MAN)/gsm_option.3	\
		$(MAN)/gsm_print.3

TOAST_MANUALS =	$(MAN)/toast.1

MANUALS	= 	$(GSM_MANUALS) $(TOAST_MANUALS) $(MAN)/bitter.1

# Other stuff in the distribution

STUFF = 	ChangeLog			\
		INSTALL			\
		MACHINES		\
		MANIFEST		\
		Makefile		\
		README			\
		$(ADDTST)/add_test.dta	\
		$(TLS)/bitter.dta	\
		$(TST)/run


# Install targets

GSM_INSTALL_TARGETS =	\
		$(GSM_INSTALL_LIB)/libgsm.a		\
		$(GSM_INSTALL_INC)/gsm.h		\
		$(GSM_INSTALL_MAN)/gsm.3		\
		$(GSM_INSTALL_MAN)/gsm_explode.3	\
		$(GSM_INSTALL_MAN)/gsm_option.3		\
		$(GSM_INSTALL_MAN)/gsm_print.3

TOAST_INSTALL_TARGETS =	\
		$(TOAST_INSTALL_BIN)/toast		\
		$(TOAST_INSTALL_BIN)/tcat		\
		$(TOAST_INSTALL_BIN)/untoast		\
		$(TOAST_INSTALL_MAN)/toast.1


# Default rules

#.c.o:
#		$(CC) $(CFLAGS) $?
#		@-mv `$(BASENAME) $@` $@ > /dev/null 2>&1

#%.o: %.i
#	$(CC) $(CFLAGS) $<

$(OBJECTS): $(PREDS)
	$(CC) $(CFLAGS) $^

%.i: %.c
	$(GCC) $(SASR) -I$(INC) -E $< > $@

# Target rules

all:		$(LIBGSM) $(TOAST) $(TCAT) $(UNTOAST)
		@-echo $(ROOT): Done.

tst:		$(TST)/lin2cod $(TST)/cod2lin $(TOAST) $(TST)/test-result
		@-echo tst: Done.

addtst:		$(ADDTST)/add $(ADDTST)/add_test.dta
		$(ADDTST)/add < $(ADDTST)/add_test.dta > /dev/null
		@-echo addtst: Done.

misc:		$(TLS)/sweet $(TLS)/bitter 	\
			$(TST)/lin2txt $(TST)/cod2txt $(TST)/gsm2cod
		@-echo misc: Done.

install:	toastinstall gsminstall
		@-echo install: Done.


# The basic API: libgsm

$(LIBGSM):	$(LIB) $(GSM_OBJECTS)
		$(RM) $(RMFLAGS) $(LIBGSM)
		$(AR) $(ARFLAGS) $(LIBGSM) $(GSM_OBJECTS)
		$(RANLIB) $(LIBGSM)


# Toast, Untoast and Tcat -- the compress-like frontends to gsm.

$(TOAST):	$(BIN) $(TOAST_OBJECTS) $(LIBGSM)
		$(LD) $(LFLAGS) -o $(TOAST) $(TOAST_OBJECTS) $(LIBGSM) $(LDLIB)

$(UNTOAST):	$(BIN) $(TOAST)
		$(RM) $(RMFLAGS) $(UNTOAST)
		$(LN) $(TOAST) $(UNTOAST)

$(TCAT):	$(BIN) $(TOAST)
		$(RM) $(RMFLAGS) $(TCAT)
		$(LN) $(TOAST) $(TCAT)


# The local bin and lib directories

$(BIN):
		if [ ! -d $(BIN) ] ; then mkdir $(BIN) ; fi

$(LIB):
		if [ ! -d $(LIB) ] ; then mkdir $(LIB) ; fi


# Installation

gsminstall:
		-if [ x"$(GSM_INSTALL_ROOT)" != x ] ; then	\
			make $(GSM_INSTALL_TARGETS) ;	\
		fi

toastinstall:
		-if [ x"$(TOAST_INSTALL_ROOT)" != x ]; then	\
			make $(TOAST_INSTALL_TARGETS);	\
		fi

gsmuninstall:
		-if [ x"$(GSM_INSTALL_ROOT)" != x ] ; then	\
			rm $(RMFLAGS) $(GSM_INSTALL_TARGETS) ;	\
		fi

toastuninstall:
		-if [ x"$(TOAST_INSTALL_ROOT)" != x ] ; then 	\
			rm $(RMFLAGS) $(TOAST_INSTALL_TARGETS);	\
		fi

$(TOAST_INSTALL_BIN)/toast:	$(TOAST)
		-rm $@
		cp $(TOAST) $@
		chmod 755 $@

$(TOAST_INSTALL_BIN)/untoast:	$(TOAST_INSTALL_BIN)/toast
		-rm $@
		ln $? $@

$(TOAST_INSTALL_BIN)/tcat:	$(TOAST_INSTALL_BIN)/toast
		-rm $@
		ln $? $@

$(TOAST_INSTALL_MAN)/toast.1:	$(MAN)/toast.1
		-rm $@
		cp $? $@
		chmod 444 $@

$(GSM_INSTALL_MAN)/gsm.3:	$(MAN)/gsm.3
		-rm $@
		cp $? $@
		chmod 444 $@

$(GSM_INSTALL_MAN)/gsm_option.3:	$(MAN)/gsm_option.3
		-rm $@
		cp $? $@
		chmod 444 $@

$(GSM_INSTALL_MAN)/gsm_explode.3:	$(MAN)/gsm_explode.3
		-rm $@
		cp $? $@
		chmod 444 $@

$(GSM_INSTALL_MAN)/gsm_print.3:	$(MAN)/gsm_print.3
		-rm $@
		cp $? $@
		chmod 444 $@

$(GSM_INSTALL_INC)/gsm.h:	$(INC)/gsm.h
		-rm $@
		cp $? $@
		chmod 444 $@

$(GSM_INSTALL_LIB)/libgsm.a:	$(LIBGSM)
		-rm $@
		cp $? $@
		chmod 444 $@


# Distribution

dist:		gsm-1.0.tar.Z
		@echo dist: Done.

gsm-1.0.tar.Z:	$(STUFF) $(SOURCES) $(HEADERS) $(MANUALS)
		(	cd $(ROOT)/..;				\
			tar cvf - `cat $(ROOT)/gsm-1.0/MANIFEST	\
				| sed '/^#/d'`			\
		) | $(COMPRESS) $(COMPRESSFLAGS) > $(ROOT)/gsm-1.0.tar.Z

# Clean

uninstall:	toastuninstall gsmuninstall
		@-echo uninstall: Done.

semi-clean:
		$(RM) $(RMFLAGS)  */*.o	*/*.ll */*.s */*.bc	\
			$(TST)/lin2cod $(TST)/lin2txt	\
			$(TST)/cod2lin $(TST)/cod2txt	\
			$(TST)/gsm2cod 			\
			$(TST)/*.*.*
		-$(FIND) . \( -name core -o -name foo \) \
			-print | xargs rm $(RMFLAGS)

clean:	semi-clean
		$(RM) $(RMFLAGS) $(LIBGSM) $(ADDTST)/add		\
			$(TOAST) $(TCAT) $(UNTOAST)	\
			$(ROOT)/gsm-1.0.tar.Z		\
			$(PREDS)


# Two tools that helped me generate gsm_encode.c and gsm_decode.c,
# but aren't generally needed to port this.

$(TLS)/sweet:	$(TLS)/sweet.o $(TLS)/taste.o
		$(LD) $(LFLAGS) -o $(TLS)/sweet \
			$(TLS)/sweet.o $(TLS)/taste.o $(LDLIB)

$(TLS)/bitter:	$(TLS)/bitter.o $(TLS)/taste.o
		$(LD) $(LFLAGS) -o $(TLS)/bitter \
			$(TLS)/bitter.o $(TLS)/taste.o $(LDLIB)


# Run $(ADDTST)/add < $(ADDTST)/add_test.dta to make sure the
# basic arithmetic functions work as intended.

$(ADDTST)/add:	$(ADDTST)/add_test.o
		$(LD) $(LFLAGS) -o $(ADDTST)/add $(ADDTST)/add_test.o $(LDLIB)


# Various conversion programs between linear, text, .gsm and the code
# format used by the tests we ran (.cod).  We paid for the test data,
# so I guess we can't just provide them with this package.  Still,
# if you happen to have them lying around, here's the code.
# 
# You can use gsm2cod | cod2txt independently to look at what's
# coded inside the compressed frames, although this shouldn't be
# hard to roll on your own using the gsm_print() function from
# the API.


$(TST)/test-result:	$(TST)/lin2cod $(TST)/cod2lin $(TOAST) $(TST)/run
			( cd $(TST); ./run ) 

$(TST)/lin2txt:		$(TST)/lin2txt.o $(LIBGSM)
			$(LD) $(LFLAGS) -o $(TST)/lin2txt \
				$(TST)/lin2txt.o $(LIBGSM) $(LDLIB)

$(TST)/lin2cod:		$(TST)/lin2cod.o $(LIBGSM)
			$(LD) $(LFLAGS) -o $(TST)/lin2cod \
				$(TST)/lin2cod.o $(LIBGSM) $(LDLIB)

$(TST)/gsm2cod:		$(TST)/gsm2cod.o $(LIBGSM)
			$(LD) $(LFLAGS) -o $(TST)/gsm2cod \
				$(TST)/gsm2cod.o $(LIBGSM) $(LDLIB)

$(TST)/cod2txt:		$(TST)/cod2txt.o $(LIBGSM)
			$(LD) $(LFLAGS) -o $(TST)/cod2txt \
				$(TST)/cod2txt.o $(LIBGSM) $(LDLIB)

$(TST)/cod2lin:		$(TST)/cod2lin.o $(LIBGSM)
			$(LD) $(LFLAGS) -o $(TST)/cod2lin \
