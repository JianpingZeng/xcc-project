include ../config/make.common
CFLAGS = -std=c99 -Wall -Wextra -I../
SRCS=$(wildcard *.c */*/*.c)
ASMS=$(SRCS:.c=.s)

all: strsplit

strsplit: $(SRCS)
	$(GCC) $^ -o $@ $(CFLAGS)

clean:
	rm -f strsplit *.s *.o *.ll *.i

.PHONY: check clean
