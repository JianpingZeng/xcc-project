
SRC = strsplit.c
DEPS = $(wildcard deps/*/*.c)
OBJS = $(SRC:.c=.o) $(DEPS:.c=.o)
CFLAGS = -std=c99 -Wall -Wextra -Ideps

all: clean test

clean:
	rm -f strsplit-test $(OBJS)

test: test.o $(OBJS)
	$(CC) $^ -o strsplit-test $(CFLAGS)
	./strsplit-test

%.o: %.c
	$(CC) $< -c -o $@ $(CFLAGS)

.PHONY: clean all test
