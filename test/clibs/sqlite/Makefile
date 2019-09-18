include ../config/make.common
SRCS=$(wildcard *.c)
ASMS=$(SRCS:.c=.s)

all: sqlite3

sqlite3: $(SRCS)
	$(GCC) $^ -o $@ -lpthread -ldl

clean:
	$(RM) *.s *.o *.ll *.i sqlite3
