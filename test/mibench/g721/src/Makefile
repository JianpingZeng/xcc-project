# Makefile for CCITT subroutines and sample programs
#

# Modify the CC definition to invoke your ANSI-C compiler
#

#############################
# Modified by C. Lee
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
ALLOBJS = $(OBJECTS) encode.o decode.o
PREDS = g711.i g72x.i g721.i g723_24.i g723_40.i encode.i decode.i


encode:  $(OBJECTS) encode.o
	$(LN) $(LDFLAGS) -o $@ $(OBJECTS) encode.o

decode:  $(OBJECTS) decode.o
	$(LN) $(LDFLAGS) -o $@ $(OBJECTS) decode.o

# Library rules for UNIX systems
#
LIB = ccitt.a
lib: $(LIB)

$(ALLOBJS): $(PREDS)
	$(CC) $(CFLAGS) $^

%.i: %.c
	$(LN) -E $< > $@

#$(LIB): $(OBJECTS)
#	@rm -f $(LIB)
#	ar cr $(LIB) $(OBJECTS)
#	@ranlib $(LIB)

clean:
	rm -rf $(LIB) $(ALL) *.o *.ll *.s *.bc *.i ../bin/*
