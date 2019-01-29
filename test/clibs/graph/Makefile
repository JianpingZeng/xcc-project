AR ?= ar
CC ?= gcc
CFLAGS = -Ideps -lm -lpthread -pedantic -std=c99 -v -Wall -Wextra

ifeq ($(APP_DEBUG),true)
	CFLAGS += -g -O0
else
	CFLAGS += -O2
endif

PREFIX ?= /usr/local

DEPS += $(wildcard deps/*/*.c)
SRCS += $(wildcard src/*.c)

OBJS += $(DEPS:.c=.o)
OBJS += $(SRCS:.c=.o)

all: build

%.o: %.c
	$(CC) $< $(CFLAGS) -c -o $@

build: build/lib/libgraph.a
	mkdir -p build/include/graph
	cp -f src/graph.h build/include/graph/graph.h

build/lib/libgraph.a: $(OBJS)
	mkdir -p build/lib
	$(AR) -crs $@ $^

clean:
	rm -fr *.o build deps/*/*.o example example.dSYM src/*.o

example: build
	$(CC) $(CFLAGS) -Ibuild/include -o example example.c -Lbuild/lib -lgraph

install: all
	mkdir -p $(PREFIX)/include/graph
	mkdir -p $(PREFIX)/lib
	cp -f src/graph.h $(PREFIX)/include/graph/graph.h
	cp -f build/libgraph.a $(PREFIX)/lib/libgraph.a

uninstall:
	rm -fr $(PREFIX)/include/graph/graph.h
	rm -fr $(PREFIX)/lib/libgraph.a

.PHONY: build clean example install uninstall
