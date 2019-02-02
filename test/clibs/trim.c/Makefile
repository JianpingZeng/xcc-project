include ../config/make.common

all: trim.o

trim.o: trim.s
	$(GCC) -c $< -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	$(GCC) -E $< > $@

clean:
	$(RM) *.s *.o *.ll *.i
