# By default, the code is compiled for a "big endian" machine.
# To compile on a "little endian" machine set the LITTLE_ENDIAN flag.
# To make smaller object code, but run a little slower, don't use UNROLL_LOOPS.
# To use NIST's modified SHA of 7/11/94, define USE_MODIFIED_SHA

include ../Make_Config

CC= $(COMPILER)

# static compilation
CFLAGS=  $(IDEM_CFLAGS)
LDFLAGS=  $(IDEM_LDFLAGS)

## dynamic compilation
#IDEMFLAG = --idempotence-construction=none --idempotence-preservation=vcf --livein-checkpoint=none --reverse-compute=none --target=armv7 -funroll-loops
#CFLAGS= -O3 --target=armv7 $(IDEMFLAG)
#LDFLAGS= -O3 --target=armv7

#CFLAGS= -O3  --target=armv7 $(IDEMFLAG)
#LDFLAGS= -O3  --target=armv7
LN= ${IDEM_LINKER}

sha:	sha_driver.o sha.o
	$(LN) ${LDFLAGS} -o $@ sha_driver.o sha.o
#	strip $@

%.o: %.c
	$(CC) $(CFLAGS) $< -c -o $@


clean:
	rm -rf *.o sha output* *.o *.bc *.s *.ll
