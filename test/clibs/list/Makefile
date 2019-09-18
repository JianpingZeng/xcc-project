include ../config/make.common
AR ?= ar
CC ?= gcc
PREFIX ?= /usr/local

CFLAGS = -O3 -std=c99 -Wall -Wextra -Ideps

SRCS = src/list.c src/list_node.c src/list_iterator.c test.c benchmark.c


all: bin/test bin/benchmark

bin/test: test.c $(SRCS)
	@mkdir -p bin
	$(GCC) $^ -o $@ $(CFLAGS)

bin/benchmark: benchmark.c $(SRCS)
	@mkdir -p bin
	$(GCC) $^ -o $@ $(CFLAGS)

clean:
	rm -fr bin build *.s src/*.s *.i *.ll *.o *.bc src/*.ll src/*.o src/*.bc

test: bin/test
	@./$<

benchmark: bin/benchmark
	@./$<

.PHONY: test benchmark clean install uninstall
