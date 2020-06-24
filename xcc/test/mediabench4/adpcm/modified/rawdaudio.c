/* testd - Test adpcm decoder */

#include "adpcm.h"
#include <stdio.h>

struct adpcm_state state;

#define NSAMPLES 1000

/* Added by C. Lee -- for dinero and spix simulations */
#define Error( Str )        FatalError( Str )
#define FatalError( Str )   fprintf( stderr, "%s\n", Str ), exit( 1 )
static FILE *fp_in, *fp_out;
/* end of addition */

char	abuf[NSAMPLES/2];
short	sbuf[NSAMPLES];

main() {
    int n;

    /* Added by C. Lee */
    char path[80];

    if ( !( fp_in = fopen ( argv[1], "r" ) ) ) 
        FatalError( "file error" );
    strcpy ( path, argv[1] );
    strcat ( path, ".pcm" );
    if ( !( fp_out = fopen ( path, "w" ) ) ) 
        FatalError( "file error" );
    /* end of addition */

    while(1) {
        /* Commented out by C. Lee
	n = read(0, abuf, NSAMPLES/2);
	*/

        /* Added by C. Lee */
        n = fread(sbuf, 1, NSAMPLES*2, fp_in);

	if ( n < 0 ) {
	    perror("input file");
	    exit(1);
	}
	if ( n == 0 ) break;
	adpcm_decoder(abuf, sbuf, n*2, &state);

        /* Commented out by C. Lee
	write(1, sbuf, n*4);
	*/

        fwrite(sbuf, sizeof (char), 1, fp_out);
    }
    fprintf(stderr, "Final valprev=%d, index=%d\n",
	    state.valprev, state.index);
    exit(0);
}
