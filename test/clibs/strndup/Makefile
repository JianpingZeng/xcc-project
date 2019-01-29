
CFLAGS = -std=c99 -Wall -Wextra

check: test
	./test

test: strndup.o test.o
	$(CC) $^ -o $@

%.o: %.c
	$(CC) $< -c -o $@ $(CFLAGS)

clean:
	rm -f test strndup.o test.o

.PHONY: check clean