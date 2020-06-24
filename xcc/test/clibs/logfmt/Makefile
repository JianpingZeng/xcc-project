include ../config/make.common
CFLAGS = -Wall -I deps/* -O3

example: utils.c logfmt_scan.c logfmt_parse.c logfmt_example.c
	$(GCC) $^ -o $@ $(CFLAGS)

bench: utils.c logfmt_scan.c logfmt_parse.c logfmt_bench.c
	$(GCC) $^ -o $@ $(CLFAGS)

logfmt_scan.c: logfmt_scan.rl
	ragel $< -o $@ -G2

logfmt_parse.c: logfmt_parse.rl
	ragel $< -o $@ -G2

%.png: %.rl
	ragel -Vp $< | dot -Tpng > $@

clean:
	rm -f logfmt_scan.c logfmt_parse.c bench example *.png *.s *.o *.ll *.i
.PHONY: clean
