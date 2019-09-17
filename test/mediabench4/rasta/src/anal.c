/*************************************************************************
 *                                                                       *
 *               ROUTINES IN THIS FILE:                                  *
 *                                                                       *
 *                      rastaplp(): main calling routine for framewise   *
 *                              analysis                                 *
 *                                                                       *
 *                      fill_frame(): for offline analysis, copy a new   *
 *			chunk of speech to the work frame and window it  *
 *                                                                       *
 *                      get_win(): calculate window function             *
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
	This is the main routine for the analysis on a frame;
	it assumes only that the windowed speech has been put into
	an fvec pointed to by fptr, and that the analysis parameters
	are all available in the structure pointed to by pptr .
	It returns a pointer to the cepstral fvec for this frame.
	The called routines initialize required tables if
	called for the first time.
*/
struct fvec *rastaplp(struct fhistory *hptr, struct param *pptr, struct fvec *fptr)
{
	struct fvec *pspectrum, *aspectrum, *nl_aspectrum,
		*ras_aspectrum, *ras_nl_aspectrum,
		*ras_postaspectrum, *cepstrum;
        struct fvec *npower;
        struct fmat *map_source;  
        
        static struct map_param  mapping_param;
        int mapset;	
        
        char *funcname;

	funcname = "rastaplp";

	/* Compute the power spectrum from the windowed input */
	pspectrum   = powspec(pptr, fptr);

	/* Compute critical band (auditory) spectrum */
	aspectrum = audspec(pptr, pspectrum);

        /*---Jah Rasta with spectral mapping -----------------*/
        if ((pptr->jrasta == TRUE ) && (pptr->cJah == FALSE))
        { 
             npower = comp_noisepower(hptr, pptr, aspectrum);
             if (npower->values[0]  < TINY)
                pptr->jah = 1.0e-6;
             else
                pptr->jah = 1/(C_CONSTANT * npower->values[0]); 
             comp_Jah(pptr, &mapping_param, &mapset);
        }
         
	
        /* Put into some nonlinear domain */
	nl_aspectrum = nl_audspec(pptr, aspectrum);

	/* Do rasta filtering */
	ras_nl_aspectrum = rasta_filt(hptr, pptr, nl_aspectrum);

        if ((pptr->crbout) == FALSE)
        { 
           if ((pptr->jrasta == TRUE) && (pptr->cJah == FALSE))
           {
               map_source = cr_map_source_data(pptr, &mapping_param, ras_nl_aspectrum);
               do_mapping(pptr,&mapping_param, map_source, mapset, ras_nl_aspectrum);  
           }  

	   /* Take the quasi inverse on the nonlinearity
		(currently just an exp) */
	   ras_aspectrum = inverse_nonlin(pptr, ras_nl_aspectrum);

	   /* Do final cube-root compression and equalization */
	   ras_postaspectrum = post_audspec(pptr, ras_aspectrum); 

           if ((pptr->comcrbout) == FALSE)
           {
              /* Code to do final smoothing; initial implementation
                 will only permit plp-style lpc-based representation,
                 which will consist of inverse dft followed
                 by autocorrelation-based lpc analysis and an
                 lpc to cepstrum recursion. */

              cepstrum = lpccep(pptr, ras_postaspectrum);
              return( cepstrum );
           }
           else
           {
              return( ras_postaspectrum );
           }
         } 
         else
         {
            return(ras_nl_aspectrum);    /* return the critical band energies
                                            after bandpass filtering   */
         }
          
}

struct fvec*
fill_frame(struct fvec* sptr,
           struct fhistory* hptr,
           struct param* pptr,
           int nframe)
{
  static struct fvec *outbufptr = NULL;
  static struct fvec *inbufptr;
  static struct fvec *window;
  int i, start, end;
  char *funcname;

  funcname = "fill_frame";

  if (outbufptr == (struct fvec*) NULL)
    /* first frame in analysis */
    {
      outbufptr = alloc_fvec(pptr->winpts);
      inbufptr = alloc_fvec(pptr->winpts); 
      window = get_win(pptr, outbufptr->length);

      if (pptr->padInput == TRUE)
        {
          const int npad = (pptr->winpts - pptr->steppts) >> 1;
          for (i = 0; i < npad; i++)
            {
              inbufptr->values[i] = sptr->values[npad - i];
            }
          for (i = npad; i < inbufptr->length; i++)
            {
              inbufptr->values[i] = sptr->values[i - npad];
            }
        }
      else
        {
          for (i = 0; i < pptr->winpts; i++)
            {
              inbufptr->values[i] = sptr->values[i];
            }
        }
    }
  else
    {
      /* shift down values in inbuf */
      for (i = pptr->steppts; i < pptr->winpts; i++)
        {
          inbufptr->values[i - pptr->steppts] = inbufptr->values[i];
        }
      if (pptr->padInput == TRUE)
        {
          start = nframe * pptr->steppts + 
            ((pptr->winpts - pptr->steppts + 1) >> 1);
          end = start + pptr->steppts;
          if (end < sptr->length)
            /* frame is within unpadded signal */
            {
              const int offset = pptr->winpts - pptr->steppts;

              for (i = 0; i < pptr->steppts; i++)
                {
                  inbufptr->values[i + offset] =
                    sptr->values[i + start];
                }
            }
          else if (start < sptr->length)
            /* start in signal, need to pad at end */
            {
              const int offset = pptr->winpts - pptr->steppts;
              for (i = 0; i < sptr->length - start; i++)
                {
                  inbufptr->values[i + offset] =
                    sptr->values[i + start];
                }
              for (i = sptr->length - start;
                   i < pptr->steppts;
                   i++)
                {
                  inbufptr->values[i + offset] = 
                    sptr->values[2 * sptr->length - start - i - 2];
                }
            }
          else
            /* only adding padding to the end */
            {
              const int offset = pptr->winpts - pptr->steppts;
              for (i = 0; i < pptr->steppts; i++)
                {
                  inbufptr->values[i + offset] =
                    sptr->values[2 * sptr->length - start - i - 2];
                }
            }
        }
      else
        {
          const int offset = -nframe * pptr->steppts;
          start = pptr->winpts + pptr->steppts * (nframe - 1);
          end = start + pptr->steppts;
          for (i = start; i < end; i++)
            {
              inbufptr->values[i + offset] = sptr->values[i];
            }
        }
    }

  if (pptr->HPfilter == TRUE)
    {
      fvec_HPfilter(hptr, pptr, inbufptr);
    }

  for (i = 0; i < outbufptr->length; i++)
    {
      outbufptr->values[i] =
        window->values[i] * inbufptr->values[i];
    }

  return (outbufptr);
}

/* Compute Hamming window, and return pointer to it. */
struct fvec *get_win( struct param *pptr, int winlength )
{
	struct fvec *winptr;
	int i;
	double base_angle;
	char *funcname;
	
	funcname = "get_win";

	winptr = alloc_fvec( winlength );

	/* Note that M_PI is PI, defined in math.h */
	base_angle = 2. * M_PI / (double)(winptr->length - 1);

	for(i=0; i<winptr->length; i++)
	{
		winptr->values[i] = pptr->winco 
		- (1.0 - pptr->winco) * cos (i * base_angle);
	}
    	return (winptr);
} 
