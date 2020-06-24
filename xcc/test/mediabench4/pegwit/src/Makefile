#
#
#

include ../../Make_Config
CC= $(COMPILER)

CFLAGS=   $(IDEM_CFLAGS)
LDFLAGS=  $(IDEM_LDFLAGS)

LN= ${IDEM_LINKER}

#CC = /export/ramoth4/bishop/simplescalar/bin/ssbig-na-sstrix-gcc -static
#CFLAGS = -O3 -DBIG_ENDIAN
#LDFLAGS = 

MAIN = pegwit
OBJS = $(MAIN).o ec_crypt.o ec_curve.o ec_field.o ec_param.o ec_vlong.o \
       square.o sqcts.o sha1.o binasc.o
HDRS = ec_crypt.h ec_curve.h ec_field.h ec_param.h ec_vlong.h sha1.h \
       square.h sqcts.h square.tab binasc.h

all: $(MAIN)
	cp $(MAIN) ../bin/.

$(MAIN): $(OBJS) $(HDRS)
	$(LN) $(LDFLAGS) -o $(MAIN) $(OBJS)

%.o: %.c
	$(CC) $(CFLAGS) $< -c -o $@

clean:
	rm -f *~ ; rm -f *.o ; rm -f $(MAIN);rm -f *.ll *.s *.bc
	$(RM) ../bin/*
