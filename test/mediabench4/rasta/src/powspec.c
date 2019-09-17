/************************************************************************
 *                                                                       *
 *               ROUTINES IN THIS FILE:                                  *
 *                                                                       *
 *                         powspec(): compute some kind of power         *
 *                                    spectrum                           *
 *                                                                       *
 ************************************************************************/

/***********************************************************************

 (C) 1995 US West and International Computer Science Institute,
     All rights reserved
 U.S. Patent Numbers 5,450,522 and 5,537,647

 Embodiments of this technology are covered by U.S. Patent
 Nos. 5,450,522 and 5,537,647.  A limited license for internal
 research and development use only is hereby granted.  All other
 rights, including all commercial exploitation, are retained unless a
 license has been granted by either US West or International Computer
 Science Institute.

***********************************************************************/

#include <stdio.h>
#include <math.h>
#include "rasta.h"
#include "functions.h"

/*
 *	This is the file that would get hacked and replaced if
 *	you want to change the basic pre-PLP spectral
 * 	analysis. In original form, it calls the usual
 *	ugly but efficient FFT routine that was translated
 *	from Fortran, and computes a simple power spectrum.
 *	A pointer to the fvec structure (float vector plus length) holding
 *	this computed array is returned to the calling program.
 *
 *	This routine departs from the usual form of allocating only on
 *	the first time it is called, since we wish to allow
 *	for calling it with different frame lengths at some future time.
 *	This means that new memory is allocated for every frame.
 *	To keep this amount from mounting up too badly, we free
 *	the space for the last frame each time through.
 */
struct fvec *powspec( const struct param *pptr, struct fvec *fptr)
{
	int i, fftlength, log2length;
	char *funcname;
	static struct fvec *pspecptr = NULL;
	float noise;

	funcname = "powspec";

	if(pspecptr != (struct fvec *)NULL)
	{
		/* Better not allocate pspecptr->values anywhere but 
			in this routine */
		free( pspecptr->values );
		free( pspecptr );
	}

        /* Round up */
	log2length = ceil(log((double)(fptr->length))/log(2.0));

	fftlength = two_to_the((double)log2length);
	pspecptr = alloc_fvec ( (fftlength / 2) + 1);
		/* Allow space for pspec from bin 0 through bin N/2 */

	/* Not currently checking array bounds for fft;
		since we pass the input length, we know that
		we are not reading from outside of the array.
		The power spectral routine should not write past
		the fftlength/2 + 1 . */
		
	fft_pow( fptr->values, pspecptr->values, 
			(long)fptr->length, (long) log2length );

	if(pptr->smallmask == TRUE)
	{
		noise = (float)fptr->length;
			/* adding the length of the data vector
			to each power spectral value is roughly
			like adding a random least significant
	        	 bit to the data, (differing only
			because of window and powspeclength not
			being exactly the same as data length)
			but is less computaton
			than calling random() many times. */
		for(i=0; i<pspecptr->length; i++)
		{
			pspecptr->values[i] += noise;
		}
	}


	return( pspecptr );
}
