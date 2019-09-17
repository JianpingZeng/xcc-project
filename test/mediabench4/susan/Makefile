include ../Make_Config

CC= $(COMPILER)

# static compilation
CFLAGS=   $(IDEM_CFLAGS)
LDFLAGS=  $(IDEM_LDFLAGS)

## dynamic compilation
#IDEMFLAG = --idempotence-construction=none --idempotence-preservation=vcf --livein-checkpoint=none --reverse-compute=none --target=armv7 -funroll-loops
#CFLAGS= -O3 --target=armv7 $(IDEMFLAG)
#LDFLAGS= -O3 --target=armv7

LN= ${IDEM_LINKER}

susan: susan.o
	$(LN) ${LDFLAGS} -o susan susan.o -lm 

%.o: %.c
	$(CC) $(CFLAGS) $< -c -o $@

clean:
	rm -rf susan output* *.bc *.s *.ll *.o
