include ../config/make.common
TESTLINKER=-lcunit
SRCS=test.c sha1.c
ASMS=$(SRCS:.c=.s)
TESTBINARY=sha1test

.FORCE: test
all: test

clean:
	rm -rf *.o $(TESTBINARY) *.s *.ll *.o *.i

test: $(ASMS)
	$(GCC) -o $(TESTBINARY) $^ $(TESTLINKER)
	./$(TESTBINARY)
%.s: %.ll
	$(LLC) $, -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	$(GCC) -E $< > $@
