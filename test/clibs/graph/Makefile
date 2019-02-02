include ../config/make.common
CFLAGS = -Ideps -lm -lpthread -pedantic -std=c99 -Wall -Wextra

DEPS += $(wildcard deps/*/*.c)
SRCS += $(wildcard src/*.c)

OBJS += $(DEPS:.c=.o)
OBJS += $(SRCS:.c=.o)
all: build

build: build/lib/libgraph.a
	mkdir -p build/include/graph
	cp -f src/graph.h build/include/graph/graph.h

build/lib/libgraph.a: $(OBJS)
	mkdir -p build/lib
	$(AR) -crs $@ $^

%.i: %.c
	$(GCC) $(CFLAGS) -E $< > $@
%.ll: %.i
	$(CLANG) $(CFLAGS) $< -o $@
%.s: %.ll
	$(LLC) $< -o $@
%.o: %.s
	$(GCC) -c $< -o $@

#example: build
#	$(GCC) $(CFLAGS) -Ibuild/include -o example example.c -Lbuild/lib -lgraph

#install: all
#	mkdir -p $(PREFIX)/include/graph
#	mkdir -p $(PREFIX)/lib
#	cp -f src/graph.h $(PREFIX)/include/graph/graph.h
#	cp -f build/libgraph.a $(PREFIX)/lib/libgraph.a
#
#uninstall:
#	rm -fr $(PREFIX)/include/graph/graph.h
#	rm -fr $(PREFIX)/lib/libgraph.a

clean:
	rm -fr *.o build deps/*/*.o deps/*/*/*.o example example.dSYM src/*.o

.PHONY: build clean example install uninstall
