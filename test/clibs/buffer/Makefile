include ../config/make.common

SRCS=$(wildcard *.c)
ASMS=$(SRCS:.c=.s)
LLS=$(SRCS:.c=.ll)

all: clean buffer $(LLS)

buffer: $(ASMS)
	$(GCC) $^ -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.ll: %.i
	$(CLANG) $< -o $@
%.i: %.c
	$(GCC) -E $< > $@

clean:
	$(RM) buffer *.s *.ll *.o

