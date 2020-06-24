include ../config/make.common
SRCS=$(wildcard *.c */*.c)

all: example

example: $(SRCS)
	$(GCC) $^ -o $@

clean:
	$(RM) $(ASMS) example
