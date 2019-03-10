#
# Makefile for adpcm and test programs
# -------------------

#####################################################################
# Added by C. Lee.
#
# On Sun machines, gcc need to be informed whether the machines have
# integer multipliers by specifying this option: -mv8.
#
# Also, on Sun machines, in order to use the SPIXTOOLS and SHADE,
# uncomment this option: -static.
#
#CC = /export/ramoth4/bishop/simplescalar/bin/ssbig-na-sstrix-gcc
#CC= /home/lqingrui/idriver/gcc_whole_module.sh
# LFLAGS = -mv8
# LFLAGS = $(LFLAGS) -static
#
# End of addition
######################################################################

# May need this on a sun:
# CFLAGS=-O -I/usr/demo/SOUND

include ../../Make_Config
CC=$(COMPILER) 

#static compilation
CFLAGS= $(IDEM_CFLAGS)
LDFLAGS= $(IDEM_LDFLAGS)

##dynamic compilation
#IDEMFLAG = --idempotence-construction=none --idempotence-preservation=none --livein-checkpoint=none --reverse-compute=none --target=armv7 -funroll-loops
#CFLAGS= -O2  $(IDEMFLAG) --target=armv7
#LDFLAGS= -O2 --target=armv7 -m32

LN= ${IDEM_LINKER}

ALLSRC=adpcm.c timing.c rawcaudio.c rawdaudio.c suncaudio.c sgicaudio.c sgidaudio.c
OBJS=adpcm.o timing.o rawcaudio.o rawdaudio.o suncaudio.o sgicaudio.o sgidaudio.o
PREDS=adpcm.i timing.i rawcaudio.i rawdaudio.i suncaudio.i sgicaudio.i sgidaudio.i

all: rawcaudio rawdaudio timing
	cp rawcaudio rawdaudio timing ../bin/.

sgi: all sgicaudio sgidaudio

sun: all suncaudio sundaudio

#adpcm.o: adpcm.h

suncaudio: suncaudio.o adpcm.o
	$(LN) $(LDFLAGS) adpcm.o suncaudio.o -o suncaudio -L/usr/demo/SOUND -laudio

sundaudio: suncaudio
	cp suncaudio sundaudio

sgicaudio: sgicaudio.o adpcm.o
	$(LN) $(LDFLAGS) sgicaudio.o adpcm.o -o sgicaudio -laudiofile -lm

sgidaudio: sgidaudio.o adpcm.o
	$(LN) $(LDFLAGS) sgidaudio.o adpcm.o -o sgidaudio -laudiofile -lm

rawcaudio: rawcaudio.o adpcm.o
	$(LN) $(LDFLAGS) rawcaudio.o adpcm.o -o rawcaudio

rawdaudio: rawdaudio.o adpcm.o
	$(LN) $(LDFLAGS) rawdaudio.o adpcm.o -o rawdaudio

timing: timing.o adpcm.o
	$(LN) $(LDFLAGS) timing.o adpcm.o -o timing

adpcm.shar: $(ALLSRC)
	shar $(ALLSRC) > adpcm.shara

$(OBJS): $(PREDS)
	$(CC) $(CFLAGS) $^

%.i: %.c
	$(LN) -E $< > $@

# Added by C. Lee
clean:
	$(RM) -r *.o  rawcaudio rawdaudio timing ../bin/* *.ll *.s *.bc *.i
