#
# Makefile for qsort and test programs
# -------------------

#####################################################################
# Added by Jianping Zeng on 2/15/2019
#
# End of addition
######################################################################

include ../../Make_Config
CC=$(COMPILER) 
LDFLAGS= $(IDEM_LDFLAGS)

#static compilation
LN= ${IDEM_LINKER}

TARGET=qsort

all: $(TARGET) copy clean

qsort: qsort_large qsort_small

qsort_small: qsort_small.c
	$(CC) -c $< -o qsort_small.o
	$(LN) qsort_small.o $(LDFLAGS) -o $@ -lm
qsort_large: qsort_large.c
	$(CC) -c $< -o qsort_large.o
	$(LN) qsort_large.o $(LDFLAGS) -o $@ -lm

copy:
	cp qsort_small ../bin
	cp qsort_large ../bin

clean:
	$(RM) qsort_small qsort_large *.o *.bc *.s *.ll
	$(RM) ../bin/*

