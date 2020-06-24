include ../config/make.common

all: trim.o

trim.o: trim.c
	$(GCC) -c $< -o $@

clean:
	$(RM) *.s *.o *.ll *.i
