#
# Makefile for bitcount and test programs
# -------------------

#####################################################################
# Added by Jianping Zeng on 2/15/2019
#
# End of addition
######################################################################

include ../../Make_Config
CC=$(COMPILER) 
CFLAGS= $(IDEM_CFLAGS)
LDFLAGS= $(IDEM_LDFLAGS)

#static compilation
LN= ${IDEM_LINKER}

SRCS=$(wildcard *.c) 
OBJS=$(SRCS:.c=.o)
BCS=$(SRCS:.c=.bc)

TARGET=bitcnts

all: $(TARGET) copy

$(TARGET): $(OBJS)
	$(LN) $(LDFLAGS) $^ -o $@
%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

copy:
	cp $(TARGET) ../bin

clean:
	$(RM) $(TARGET) *.o *.bc *.s *.ll ../bin/*

