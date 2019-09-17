include ../config/make.common

OS = $(shell uname)
LDFLAGS =

ifneq ($(OS), Darwin)
	LDFLAGS += -lrt
endif

all: clean test

clean:
	rm -f bench *.s *.ll *.i *.o

test: test.c
	$(GCC) $^ -o bench $(LDFLAGS)

.PHONY: all clean test
