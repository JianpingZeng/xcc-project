include ../config/make.common

AR ?= ar
CFLAGS = -Ideps -pedantic -std=c99 -Wall -Wextra -O2

PREFIX ?= /usr/local

SRCS += $(wildcard src/*.c)

OBJS += $(SRCS:.c=.o)

all: build

%.o: %.c
	$(GCC) -c $< -o $@ $(CFLAGS)

example.o: example.c
	$(GCC) -c $< -o $a -Ibuild/include

build: build/lib/libjsw_rbtree.a
	mkdir -p build/include/jsw_rbtree
	cp -f src/jsw_rbtree.h build/include/jsw_rbtree/jsw_rbtree.h

build/lib/libjsw_rbtree.a: $(OBJS)
	mkdir -p build/lib
	$(AR) -crs $@ $^

clean:
	rm -fr *.o build example example.dSYM src/*.o src/*.s src/*.ll *.ll *.s *.o *.a 

example: build
	$(GCC) -o example example.s -Lbuild/lib -ljsw_rbtree

install: all
	mkdir -p $(PREFIX)/include/jsw_rbtree
	mkdir -p $(PREFIX)/lib
	cp -f src/jsw_rbtree.h $(PREFIX)/include/jsw_rbtree/jsw_rbtree.h
	cp -f build/lib/libjsw_rbtree.a $(PREFIX)/lib/libjsw_rbtree.a

uninstall:
	rm -fr $(PREFIX)/include/jsw_rbtree/jsw_rbtree.h
	rm -fr $(PREFIX)/lib/libjsw_rbtree.a

.PHONY: build clean example install uninstall
