/* testd - Test adpcm decoder */


#include "stats_time.h"
#include "adpcm.h"
#include <stdio.h>

struct adpcm_state state;

#define NSAMPLES 1000

char	abuf[NSAMPLES/2];
short	sbuf[NSAMPLES];

main() {
    int n;
//SS_START
    while(1) {
	n = read(0, abuf, NSAMPLES/2);
	if ( n < 0 ) {
	    perror("input file");
	    exit(1);
	}
	if ( n == 0 ) break;
	adpcm_decoder(abuf, sbuf, n*2, &state);
	write(1, sbuf, n*4);
    }
    fprintf(stderr, "Final valprev=%d, index=%d\n",
	    state.valprev, state.index);
//SS_STOP
//SS_PRINT
    exit(0);
}
