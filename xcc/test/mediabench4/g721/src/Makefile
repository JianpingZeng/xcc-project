# Makefile for CCITT subroutines and sample programs
#

# Modify the CC definition to invoke your ANSI-C compiler
#

#############################
# Modified by C. Lee
#CC =  ../../../bin/ssbig-na-sstrix-gcc # gcc -mv8 -static
include ../../Make_Config
CC= $(COMPILER)

#static compilation
CFLAGS =  $(IDEM_CFLAGS)
LDFLAGS=  $(IDEM_LDFLAGS)
LN= ${IDEM_LINKER}

# Rules to build sample programs do not depend on a UNIX library program
#
ALL = encode decode
all: $(ALL)
	cp encode decode ../bin/.


OBJECTS = g711.o g72x.o g721.o g723_24.o g723_40.o

#encode: $(OBJECTS) encode.c
#	$(CC) -o $@ encode.c $(OBJECTS)
#
#decode: $(OBJECTS) decode.c
#	$(CC) -o $@ decode.c $(OBJECTS)

encode:  $(OBJECTS) encode.o
	$(LN) $(LDFLAGS) -o $@ $(OBJECTS) encode.o

decode:  $(OBJECTS) decode.o
	$(LN) $(LDFLAGS) -o $@ $(OBJECTS) decode.o

# Library rules for UNIX systems
#
LIB = ccitt.a
lib: $(LIB)

%.o: %.c
	$(CC) $(CFLAGS) $< -c -o $@

#$(LIB): $(OBJECTS)
#	@rm -f $(LIB)
#	ar cr $(LIB) $(OBJECTS)
#	@ranlib $(LIB)

clean:
	/bin/rm -f $(LIB) $(ALL) *.o *.ll *.s *.bc ; /bin/rm -f ../bin/*
