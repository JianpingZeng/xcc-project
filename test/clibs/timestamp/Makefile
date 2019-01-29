
CFLAGS = -std=c99 -Wall -Wextra

check: test
	./test

test: test.o timestamp.o
	$(CC) $^ -o $@

%.o: %.c
	$(CC) $< -c -o $@ $(CFLAGS)

clean:
	rm -f test *.o

.PHONY: check clean
