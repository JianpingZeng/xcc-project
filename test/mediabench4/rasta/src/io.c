/*************************************************************************
 *                                                                       *
 *               ROUTINES IN THIS FILE:                                  *
 *                                                                       *
 *                      get_data(): offline input routine, accepting     *
 *                              binary shorts, ascii, MAT or esps files; *
 *                              calls get_ascdata, get_bindata,          *
 *                              get_matdata or esps library routines     *
 *                                                                       *
 *                      get_ascdata(): offline input routine, reads      *
 *                              ascii and allocates as it goes           *
 *                                                                       *
 *                      get_bindata(): offline input routine, reads      *
 *                              binary shorts and allocates as it goes   *
 *                                                                       *
 *                      get_online_bindata(): online input routine,      *
 *                              reads binary shorts into rest of         *
 *                              analysis                                 *
 *                                                                       *
 *                      open_out(): opens non-esps output file for       *
 *                               writing                                 *
 *                                                                       *
 *                      write_out(): writes output file; calls esps      *
 *                               library routines or print_vec()         *
 *                               or bin_vec()                            *
 *                                                                       *
 *                      print_vec(): offline output routine, writes      *
 *                              ascii                                    *
 *                                                                       *
 *                      binout_vec(): offline output routine, writes     *
 *                              binary floats                            *
 *                                                                       *
 *                      fvec_HPfilter(): IIR highpass filter on waveform *
 *                                                                       *
 *                      load_history(): load history                     *
 *                                                                       *
 *                      save_history(): save history                     *
 *                                                                       *
 *                      get_abbotdata(): read data in Abbot wav format   *
 *                                                                       *
 *                      abbot_write_eos(): write EOS to output for Abbot *
 *                                         I/O                           *
 *                                                                       *
 *                      abbot_read(): replacement for fread() that       *
 *                                     detects input EOS symbol          *
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

#include "config.h"
#include <stdio.h>
#include <math.h>
#include "rasta.h"
#include "functions.h"

/* replacement for fread that looks for EOS in Abbot wav file input */

static int abbot_read(struct fhistory* hptr,
                      struct param* pptr,
                      short* buf,
                      int n);

/* output data in Abbot output format */
static void abbotout_vec(const struct param* pptr, struct fvec *fptr);

#ifdef HAVE_LIBESPS
#include <esps/esps.h>
#include <esps/fea.h>
#include <esps/feasd.h>

/* pre-defined brain dead definition for ESPS libs */
int debug_level = 4;

static double Start_time = 0.0;
/* Global to this file only */
#endif

#ifdef HAVE_LIBMAT
/* MATLAB */
#include <mat.h>
#endif

#ifdef HAVE_LIBSP
#include "sphere/include/sp/sphere.h"
#endif

/* Output buffer for Abbot (online features) output */

static float* abbot_outbuf = NULL;

/* Here we read in ascii numbers, binary shorts, 
   or esps format files where
   blocks are allocated as necessary. */
struct fvec *get_data(struct param * pptr)
{
#ifdef HAVE_LIBESPS
  /* ESPS input vars */
  struct header *inhead;
  struct feasd *sdrec = (struct feasd *)NULL;
  struct fvec *get_espsdata( FILE *, struct fvec *, 
                            struct header * );
  int readSampfreq;
#endif

#ifdef HAVE_LIBMAT
  /* MATLAB */
  struct fvec *get_matdata( MATFile *, struct fvec *);    
  MATFile *matfptr = (MATFile *)NULL;
#endif

#ifdef HAVE_LIBSP
  /* SPHERE input vars */
  SP_FILE* spInfile;
  SP_INTEGER channel_count;
  SP_INTEGER sample_rate;
  short* spBuffer;
  short* buf_endp;
  short* bp;
  float* fp;
#endif

  /* locally called functions */
  struct fvec *get_ascdata( FILE *, struct fvec *);
  struct fvec *get_bindata( FILE *, struct fvec *, struct param *);

  /* local variables */
  struct fvec *sptr;
  FILE *fptr = (FILE *)NULL;
  char *funcname;

  funcname = "get_data";

  /* Allocate space for structure */
  sptr = (struct fvec *) malloc(sizeof(struct fvec) );
  if(sptr == (struct fvec *)NULL)
    {
      fprintf(stderr,"Unable to allocate speech structure\n");
      exit(-1);
    }
        
  if(pptr->espsin == TRUE)
    {
#ifdef HAVE_LIBESPS
      eopen("rasta_plp", pptr->infname, "r", FT_FEA, FEA_SD,
            &inhead, &fptr );

      Start_time = (double) get_genhd_val( "start_time", inhead, 0.0);

      sptr->length = inhead->common.ndrec;

      readSampfreq = (int) get_genhd_val("record_freq", 
                                         inhead,
                                         (double)(pptr->sampfreq) );
      if (readSampfreq != pptr->sampfreq)
        {
          fprintf(stderr,
                  "%s does not have the expected sample rate.\n",
                  pptr->infname);
          exit(1);
        }

      if(sptr->length >= 1) 
        {
          sptr->values 
            = (float *)calloc( sptr->length, sizeof(float));
          sdrec = allo_feasd_recs( inhead, FLOAT, sptr->length, 
                                  (char *)sptr->values, NO);
        }
      else
        {
          /* Initial buffer allocation for float speech vector */
          sptr->values 
            = (float *) malloc(SPEECHBUFSIZE * sizeof(float));
          if(sptr->values == (float *)NULL)
            {
              fprintf(stderr,
                      "Can't allocate first speech buffer\n");
              exit(-1);
            }
        }
      if(sdrec == ( struct feasd *)NULL)
        {
          fprintf(stderr,
                  "Can't do ESPS input allocation\n");
          exit(-1);
        }
#endif
    }
  else if(pptr->matin == TRUE)
    {
#ifdef HAVE_LIBMAT
      /* open MAT-file */
      matfptr = matOpen(pptr->infname, "r");
      if(matfptr == (MATFile *)NULL)
        {
          fprintf(stderr,"Error opening %s\n", pptr->infname);
          exit(-1);
        }
      /* Initial buffer allocation for float speech vector */
      sptr->values = (float *) malloc(SPEECHBUFSIZE * sizeof(float));
      if(sptr->values == (float *)NULL)
        {
          fprintf(stderr,"Can't allocate first speech buffer\n");
          exit(-1);
        }
#endif
    }
  else if(pptr->spherein == TRUE)
    {
#ifdef HAVE_LIBSP
      /* open input file */
      if ((spInfile = sp_open(pptr->infname, "r")) == SPNULL)
        {
          fprintf(stderr, "Cannot open %s for input.\n",
                  pptr->infname);
          exit(-1);
        }

      /* want the data to come from the SPHERE file in 2-byte, native
         endian, PCM-encoded format */
      if (sp_set_data_mode(spInfile, "DF-RAW:SE-PCM:SBF-N") != 0)
        {
          sp_print_return_status(stderr);
          sp_close(spInfile);
          exit(1);
        }

      /* Ensure the input is single-channel */
      if (sp_h_get_field(spInfile,
                         "channel_count",
                         T_INTEGER,
                         (void **) &channel_count) != 0)
        {
          fprintf(stderr,
                  "Unable to read channel_count from %s\n",
                  pptr->infname);
          sp_close(spInfile);
          exit(1);
        }
      if (channel_count != 1)
        {
          fprintf(stderr,
                  "Can only process single-channel data\n");
          sp_close(spInfile);
          exit(1);
        }

      /* Check sampling rate of the file against the expected
         sampling rate */
      if (sp_h_get_field(spInfile,
                         "sample_rate",
                         T_INTEGER,
                         (void **) &sample_rate) != 0)
        {
          fprintf(stderr,
                  "Unable to read sample_rate from %s\n",
                  pptr->infname);
          sp_close(spInfile);
          exit(1);
        }
      if (pptr->sampfreq != sample_rate)
        {
          fprintf(stderr,
                  "%s does not have the expected sample rate.\n",
                  pptr->infname);
          sp_close(spInfile);
          exit(1);
        }

      /* get the file length (in points) and allocate the input
         buffers */
      if (sp_h_get_field(spInfile,
                         "sample_count",
                         T_INTEGER,
                         (void **) &(sptr->length)) != 0)
        {
          fprintf(stderr,
                  "Unable to read sample_count from %s\n",
                  pptr->infname);
          sp_close(spInfile);
          exit(1);
        }
      if ((sptr->values =
           (float*) malloc(sptr->length * sizeof(float))) == NULL)
        {
          fprintf(stderr, "Unable to allocate speech buffer.\n");
          exit(-1);
        }
      if ((spBuffer =
           (short*) malloc(sptr->length * sizeof(short))) == NULL)
        {
          fprintf(stderr, "Unable to allocate speech buffer.\n");
          exit(-1);
        }
      buf_endp = spBuffer + sptr->length;
#endif
    }
  else 
    {
      /* Initial buffer allocation for float speech vector */
      sptr->values = (float *) malloc(SPEECHBUFSIZE * sizeof(float));
      if(sptr->values == (float *)NULL)
        {
          fprintf(stderr,"Can't allocate first speech buffer\n");
          exit(-1);
        }
      if(strcmp(pptr->infname, "-") == 0)
        {
          fptr = stdin;
        }
      else
        {
          fptr = fopen(pptr->infname, "r");
          if (fptr == (FILE *)NULL)
            {
              fprintf(stderr,"Error opening %s\n", 
                      pptr->infname);
              exit(-1);
            }
        }
    }
  if(sptr->values == (float *)NULL)
    {
      fprintf(stderr,"Can't allocate first speech buffer\n");
      exit(-1);
    }


  /* here we get data */

  if(pptr->espsin == TRUE)
    {
#ifdef HAVE_LIBESPS
      if(sptr->length < 1 )
        {
          /* read and allocate a chunk at a time */
          sptr = get_espsdata( fptr, sptr, 
                              inhead );
        }
      else
        {
          /* If the length was stored, get it all. */
          (void) get_feasd_recs( sdrec, 0, sptr->length, 
                                inhead, fptr);
        }
#endif
    }
  else if(pptr->matin == TRUE)
    {
#ifdef HAVE_LIBMAT
      sptr = get_matdata(matfptr, sptr);
#endif
    }
  else if(pptr->spherein == TRUE)
    {
#ifdef HAVE_LIBSP
      if (sp_read_data(spBuffer, sptr->length, spInfile) !=
          sptr->length) 
        {
          fprintf(stderr, "Error reading %s.\n", pptr->infname);
          exit(-1);
        }
      bp = spBuffer;
      fp = sptr->values;
      while (bp != buf_endp) { *fp++ = (float) *bp++; }
#endif
    }
  else if(pptr->ascin == TRUE)
    {
      sptr = get_ascdata(fptr, sptr);
    }
  else
    {
      sptr = get_bindata(fptr, sptr, pptr);
    }

  return(sptr);
}

/* reads in ascii data into speech structure array, storing
   length information and allocating memory as necessary */
struct fvec *get_ascdata( FILE *fptr, struct fvec *sptr)
{
  int i, nread;
  char *funcname;
  int buflength = SPEECHBUFSIZE;

  funcname = "get_ascdata";

  sptr->values = (float *)realloc((char*)sptr->values, 
                                  buflength * sizeof(float) );
  if(sptr->values == (float *)NULL)
    {
      fprintf(stderr,
              "Unable to allocate %ld bytes for speech buffer\n",
              buflength * sizeof(float));
      exit(-1);
    }
  i = 0;
  while( (nread = fscanf(fptr, "%f", &(sptr->values[i])) )  == 1)
    {
      i++;                      /* On to the next sample */
      if(i >= buflength)
        {
          buflength += SPEECHBUFSIZE;
          sptr->values = (float *)realloc((char*)sptr->values, 
                                          buflength * sizeof(float) );
          if(sptr->values == (float *)NULL)
            {
              fprintf(stderr,"Can't allocate %ld byte buffer\n",
                      buflength * sizeof(float));
              exit(-1);
            }
        }
    }
  sptr->length = i;             /* Final value of index is the length */
  return ( sptr);
}

/* reads in binary short data into speech structure array, storing
   length information and allocating memory as necessary */
struct fvec *get_bindata(FILE *fptr, struct fvec *sptr, struct param *pptr)
{
  int i, nread, start;
  char *funcname;
  int totlength = 0;
  short buf[SPEECHBUFSIZE];
        
  funcname = "get_bindata";

  while( (nread = fread((char*)buf, sizeof(short), 
                        SPEECHBUFSIZE, fptr) ) == SPEECHBUFSIZE)
    {
      start = totlength;
      totlength += SPEECHBUFSIZE;
      sptr->values = (float *)realloc((char*)sptr->values, 
                                      totlength * sizeof(float) );
      if(sptr->values == (float *)NULL)
        {
          fprintf(stderr,"Can't allocate %ld byte buffer\n",
                  totlength * sizeof(float));
          exit(-1);
        }
      for(i=0; i<SPEECHBUFSIZE; i++)
        {
          if (pptr->inswapbytes == TRUE)
            {
              sptr->values[start+i] =
                (float)(SWAP_BYTES(buf[i]));
            }
          else
            {
              sptr->values[start+i] = (float)buf[i];
            }
        }
    }
  /* now read in last bunch that are less than one buffer full */
  start = totlength;
  totlength += nread;
  sptr->values = (float *)realloc((char*)sptr->values, 
                                  totlength * sizeof(float) );
  if(sptr->values == (float *)NULL)
    {
      fprintf(stderr,"Unable to realloc %ld bytes for speech buffer\n",
              totlength * sizeof(float));
      exit(-1);
    }
  for(i=0; i<nread; i++)
    {
      if (pptr->inswapbytes == TRUE)
        {
          sptr->values[start+i] = (float)(SWAP_BYTES(buf[i]));
        }
      else
        {
          sptr->values[start+i] = (float)buf[i];
        }
    }
  sptr->length = totlength;

  return (sptr );
}

#ifdef HAVE_LIBMAT
/* reads in MATLAB data into speech structure array, storing
   length information and allocating memory as necessary */
struct fvec *get_matdata( MATFile *matfptr, struct fvec *sptr)
{
  Matrix *matm;
  double *matr;
  int totlength, i;
  char *funcname;
        
  funcname = "get_matdata";

  /* load MATLAB-matrix (one row or one column: vector) */
  matm = matGetNextMatrix(matfptr);
  if(mxGetM(matm) == 1)
    totlength = mxGetN(matm);
  else if(mxGetN(matm) == 1)
    totlength = mxGetM(matm);
  else
    {
      fprintf(stderr,"MAT-file input error: more than one row or column\n");
      exit(-1);
    }
  /* allocation for float speech vector */
  sptr->values = (float *)realloc((char*)sptr->values, 
                                  totlength * sizeof(float) );
  if(sptr->values == (float *)NULL)
    {
      fprintf(stderr,"Unable to realloc %ld bytes for speech buffer\n",
              totlength * sizeof(float));
      exit(-1);
    }
  /* get speech data from MATLAB-matrix */
  matr = mxGetPr(matm);
  for(i = 0; i<totlength; i++)
    sptr->values[i] = (float)matr[i];
  sptr->length = totlength;

  return (sptr );
}
#endif

/* reads in binary short data into speech structure array, assuming
   data coming from stdin and only reading in enough to provide a full
   frame's worth of data for the analysis. This means a full frame's
   worth for the first frame, and one analysis step's worth on
   proceeding frames.  

   This routine is very similar to fill_frame(), which can be found in
   anal.c; it is here because it does data i/o (OK, just "i" )

   Speech samples are highpass filtered on waveform if option -F is
   used */

struct fvec* 
get_online_bindata(struct fhistory *hptr, struct param *pptr)
{
  static struct fvec *outbufptr = NULL;
  static struct fvec *inbufptr;
  static struct svec *sbufptr;
  static struct svec *padbufptr = NULL;
  static struct fvec *window;
  static int nsamples = 0;
  static int nframes = 0;
  static int target_nframes = 0;
  static int padptr = 0;
  int padlen;
  int i, overlap, nread;
  char *funcname;
        
  funcname = "get_online_bindata";
        
  if(outbufptr == (struct fvec *)NULL)
    /* first frame in analysis */
    {
      outbufptr = alloc_fvec(pptr->winpts);
      inbufptr = alloc_fvec(pptr->winpts); 
      sbufptr = alloc_svec(pptr->winpts);
                
      window = get_win(pptr, outbufptr->length);

      if (pptr->padInput == TRUE)
        {
          short* rp =
            sbufptr->values + ((pptr->winpts - pptr->steppts) >> 1);
          short* padp = rp;

          nread = fread((char*) rp,
                        sizeof(short),
                        pptr->winpts -
                        ((pptr->winpts - pptr->steppts) >> 1), 
                        stdin);

          if (nread !=
              (pptr->winpts - ((pptr->winpts - pptr->steppts) >> 1)))
            {
              if (pptr->debug == TRUE)
                {
                  fprintf(stderr,"Done with online input\n");
                }
              if(pptr->history == TRUE)
                {
                  save_history( hptr, pptr );
                }
              exit(0);
            }
          nsamples += nread;
          nframes++;

          /* do padding */
          while (padp != sbufptr->values)
            {
              *--padp = *++rp;
            }
        }
      else
        {
          nread = fread((char*)sbufptr->values,
                        sizeof(short), 
                        pptr->winpts, stdin);

          if(nread != pptr->winpts)
            {
              if (pptr->debug == TRUE)
                {
                  fprintf(stderr,"Done with online input\n");
                }
              if(pptr->history == TRUE)
                {
                  save_history( hptr, pptr );
                }
              exit(0);
            }
        }

      if (pptr->inswapbytes == TRUE)
        for(i = 0; i< pptr->winpts; i++)
          inbufptr->values[i] =
            (float) SWAP_BYTES(sbufptr->values[i]); 
      else
        for(i = 0; i< pptr->winpts; i++)
          inbufptr->values[i] = (float) sbufptr->values[i];
    }
  else
    {
      if (pptr->padInput == TRUE)
        {
          if (padbufptr == (struct svec*) NULL)
            /* still working from input */
            {
              nread = fread(sbufptr->values,
                            sizeof(short),
                            pptr->steppts, stdin);
              nsamples += nread;

              if (nread != pptr->steppts)
                /* hit the end of input.  Check if we are done */
                {
                  target_nframes = nsamples / pptr->steppts;
                  if (nframes == target_nframes)
                    {
                      if (pptr->debug == TRUE)
                        {
                          fprintf(stderr,"Done with online input\n");
                        }
                      if(pptr->history == TRUE)
                        {
                          save_history( hptr, pptr );
                        }
                      exit(0);
                    }

                  /* set up padding buffer */
                  padlen = ((pptr->winpts - pptr->steppts + 1) >> 1) -
                    (nsamples % pptr->steppts);
                  padbufptr = alloc_svec(padlen);

                  if ((nread - 1) < padbufptr->length)
                    {
                      for (i = 0; i < nread - 1; i++)
                        {
                          padbufptr->values[i] =
                            sbufptr->values[nread - i - 2];
                        }
                      /* This is a big, ugly, disgusting hack,
                         but it works */
                      if (pptr->inswapbytes == TRUE)
                        {
                          for (i = nread - 1; i < padbufptr->length; i++)
                            {
                              padbufptr->values[i] = 
                                SWAP_BYTES(((short) inbufptr->values[inbufptr->length + nread - i - 2]));
                            }
                        }
                      else
                        {
                          for (i = nread - 1; i < padbufptr->length; i++)
                            {
                              padbufptr->values[i] = (short) inbufptr->values[inbufptr->length + nread - i - 2]; 
                            }
                        }
                    }
                  else
                    {
                      for (i = 0; i < padbufptr->length; i++)
                        {
                          padbufptr->values[i] =
                            sbufptr->values[nread - i - 2];
                        }
                    }

                  /* pad sbuf */
                  svec_check(funcname,
                             padbufptr,
                             padptr + pptr->steppts - nread - 1);
                  
                  for (i = nread; i < pptr->steppts; i++)
                    {
                      sbufptr->values[i] =
                        padbufptr->values[padptr++];
                    }
                }
            }
          else
            /* padding the tail */
            {
              if (nframes == target_nframes)
                {
                  if (pptr->debug == TRUE)
                    {
                      fprintf(stderr,"Done with online input\n");
                    }
                  if(pptr->history == TRUE)
                    {
                      save_history( hptr, pptr );
                    }
                  exit(0);
                }
              svec_check(funcname,
                         padbufptr,
                         padptr + pptr->steppts - 1);
              for (i = 0; i < pptr->steppts; i++)
                {
                  sbufptr->values[i] = padbufptr->values[padptr++];
                }
            }
          nframes++;
        }
      else
        {
          nread = fread(sbufptr->values,
                        sizeof(short),
                        pptr->steppts, stdin);

          if (nread != pptr->steppts)
            {
              if (pptr->debug == TRUE)
                {
                  fprintf(stderr,"Done with online input\n");
                }
              if(pptr->history == TRUE)
                {
                  save_history( hptr, pptr );
                }
              exit(0);
            }
        }
                
      /* Shift down input values */
      for (i = pptr->steppts; i < pptr->winpts; i++)
        inbufptr->values[i - pptr->steppts] = inbufptr->values[i];

      /* new values */
      overlap = pptr->winpts - pptr->steppts;
      if (pptr->inswapbytes == TRUE)
        for (i = overlap; i < pptr->winpts; i++)
          inbufptr->values[i] = 
            (float) SWAP_BYTES(sbufptr->values[i - overlap]);
      else
        for (i = overlap; i < pptr->winpts; i++)
          inbufptr->values[i] = (float) sbufptr->values[i - overlap];
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

/* reads in binary short data into speech structure array, assuming
   data coming from stdin and only reading in enough to provide a full
   frame's worth of data for the analysis. This means a full frame's
   worth for the first frame, and one analysis step's worth on
   proceeding frames.  

   Uses the Abbot wav format from CUED --- multiple utterances may
   come in, separated by an EOS marker: 0x8000.  Note that any values
   of 0x8000 in the input data must be mapped to 0x8001 before rasta
   processing for this to work.

   This routine is derived from get_online_bindata, with
   specialization for the Abbot wav format. */


struct fvec* 
get_abbotdata(struct fhistory *hptr, struct param *pptr)
{
  static struct fvec *outbufptr = NULL;
  static struct fvec *inbufptr;
  static struct svec *sbufptr;
  static struct svec *padbufptr = NULL;
  static struct fvec *window;
  static int nsamples = 0;
  static int nframes = 0;
  static int target_nframes = 0;
  static int padptr = 0;
  int padlen;
  int i, overlap, nread;
  char *funcname;
        
  funcname = "get_abbotdata";
        
  if(outbufptr == (struct fvec *)NULL)
    /* first frame in analysis */
    {
      outbufptr = alloc_fvec(pptr->winpts);
      inbufptr = alloc_fvec(pptr->winpts); 
      sbufptr = alloc_svec(pptr->winpts);
                
      window = get_win(pptr, outbufptr->length);

      if (pptr->padInput == TRUE)
        {
          short* rp =
            sbufptr->values + ((pptr->winpts - pptr->steppts) >> 1);
          short* padp = rp;

          nread = abbot_read(hptr,
                             pptr,
                             rp,
                             pptr->winpts -
                             ((pptr->winpts - pptr->steppts) >> 1));

          if (nread !=
              (pptr->winpts - ((pptr->winpts - pptr->steppts) >> 1)))
            {
              if (hptr->eof == TRUE)
                {
                  if (pptr->debug == TRUE)
                    {
                      fprintf(stderr,"Done with online input\n");
                    }
                  if(pptr->history == TRUE)
                    {
                      save_history( hptr, pptr );
                    }
                  exit(0);
                }
              else if (hptr->eos == TRUE)
                {
                  return (struct fvec*) NULL;
                }
              else
                /* this should never happen */
                {
                  fprintf(stderr,
                          "rasta (%s): bug in Abbot input.\n",
                          funcname);
                  exit(1);
                }
            }

          nsamples += nread;
          nframes++;

          /* do padding */
          while (padp != sbufptr->values)
            {
              *--padp = *++rp;
            }
        }
      else
        {
          nread = abbot_read(hptr,
                             pptr,
                             sbufptr->values,
                             pptr->winpts);

          if (nread != pptr->winpts)
            {
              if (hptr->eof == TRUE)
                {
                  if (pptr->debug == TRUE)
                    {
                      fprintf(stderr,"Done with online input\n");
                    }
                  if(pptr->history == TRUE)
                    {
                      save_history( hptr, pptr );
                    }
                  exit(0);
                }
              else if (hptr->eos)
                {
                  return (struct fvec*) NULL;
                }
              else
                /* this should never happen */
                {
                  fprintf(stderr,
                          "rasta (%s): bug in Abbot input.\n",
                          funcname);
                  exit(1);
                }
            }
        }

      if (pptr->inswapbytes == TRUE)
        for(i = 0; i< pptr->winpts; i++)
          inbufptr->values[i] =
            (float) SWAP_BYTES(sbufptr->values[i]); 
      else
        for(i = 0; i< pptr->winpts; i++)
          inbufptr->values[i] = (float) sbufptr->values[i];
    }
  else
    {
      if (pptr->padInput == TRUE)
        {
          if (padbufptr == (struct svec*) NULL)
            /* still working from input */
            {
              nread = abbot_read(hptr,
                                 pptr,
                                 sbufptr->values,
                                 pptr->steppts); 
              nsamples += nread;

              if (nread != pptr->steppts)
                /* hit the end of utterance.  Check if we are done */
                {
                  target_nframes = nsamples / pptr->steppts;
                  if (nframes == target_nframes)
                    {
                      if (hptr->eof == TRUE)
                        {
                          abbot_write_eos(pptr);
                          if (pptr->debug == TRUE)
                            {
                              fprintf(stderr,"Done with online input\n");
                            }
                          if(pptr->history == TRUE)
                            {
                              save_history( hptr, pptr );
                            }
                          exit(0);
                        }
                      else if (hptr->eos)
                        /* clear state and return */
                        {
                          free(outbufptr);
                          outbufptr = NULL;
                          free(inbufptr);
                          free(sbufptr);
                          free(window);
                          if (padbufptr != (struct svec*) NULL)
                            {
                              free(padbufptr);
                              padbufptr = (struct svec*) NULL;
                            }
                          nsamples = 0;
                          nframes = 0;
                          target_nframes = 0;
                          padptr = 0;

                          if (pptr->HPfilter == TRUE)
                            {
                              fvec_HPfilter(hptr,
                                            pptr,
                                            (struct fvec*) NULL);
                            }
                          
                          return (struct fvec*) NULL;
                        }
                      else
                        /* this should never happen */
                        {
                          fprintf(stderr,
                                  "rasta (%s): bug in Abbot input.\n",
                                  funcname);
                          exit(1);
                        }
                    }

                  /* set up padding buffer */
                  padlen = ((pptr->winpts - pptr->steppts + 1) >> 1) -
                    (nsamples % pptr->steppts);
                  padbufptr = alloc_svec(padlen);

                  if ((nread - 1) < padbufptr->length)
                    {
                      for (i = 0; i < nread - 1; i++)
                        {
                          padbufptr->values[i] =
                            sbufptr->values[nread - i - 2];
                        }
                      /* This is a big, ugly, disgusting hack,
                         but it works */
                      if (pptr->inswapbytes == TRUE)
                        {
                          for (i = nread - 1; i < padbufptr->length; i++)
                            {
                              padbufptr->values[i] = 
                                SWAP_BYTES(((short) inbufptr->values[inbufptr->length + nread - i - 2]));
                            }
                        }
                      else
                        {
                          for (i = nread - 1; i < padbufptr->length; i++)
                            {
                              padbufptr->values[i] = (short) inbufptr->values[inbufptr->length + nread - i - 2]; 
                            }
                        }
                    }
                  else
                    {
                      for (i = 0; i < padbufptr->length; i++)
                        {
                          padbufptr->values[i] =
                            sbufptr->values[nread - i - 2];
                        }
                    }

                  /* pad sbuf */
                  svec_check(funcname,
                             padbufptr,
                             padptr + pptr->steppts - nread - 1);
                  
                  for (i = nread; i < pptr->steppts; i++)
                    {
                      sbufptr->values[i] =
                        padbufptr->values[padptr++];
                    }

                  hptr->eos = FALSE;
                }
            }
          else
            /* padding the tail */
            {
              if (nframes == target_nframes)
                {
                  if (hptr->eof == TRUE)
                    {
                      abbot_write_eos(pptr);
                      if (pptr->debug == TRUE)
                        {
                          fprintf(stderr,"Done with online input\n");
                        }
                      if(pptr->history == TRUE)
                        {
                          save_history( hptr, pptr );
                        }
                      exit(0);
                    }
                  else
                    /* clear state and return */
                    {
                      hptr->eos = TRUE;
                      free(outbufptr);
                      outbufptr = NULL;
                      free(inbufptr);
                      free(sbufptr);
                      free(window);
                      if (padbufptr != (struct svec*) NULL)
                        {
                          free(padbufptr);
                          padbufptr = (struct svec*) NULL;
                        }
                      nsamples = 0;
                      nframes = 0;
                      target_nframes = 0;
                      padptr = 0;

                      if (pptr->HPfilter == TRUE)
                        {
                          fvec_HPfilter(hptr,
                                        pptr,
                                        (struct fvec*) NULL);
                        }
                          
                      return (struct fvec*) NULL;
                    }
                }

              svec_check(funcname,
                         padbufptr,
                         padptr + pptr->steppts - 1);
              for (i = 0; i < pptr->steppts; i++)
                {
                  sbufptr->values[i] = padbufptr->values[padptr++];
                }
            }
          nframes++;
        }
      else
        {
          nread = abbot_read(hptr,
                             pptr,
                             sbufptr->values,
                             pptr->steppts);

          if (nread != pptr->steppts)
            {
              if (hptr->eof == TRUE)
                {
                  abbot_write_eos(pptr);
                  if (pptr->debug == TRUE)
                    {
                      fprintf(stderr,"Done with online input\n");
                    }
                  if(pptr->history == TRUE)
                    {
                      save_history( hptr, pptr );
                    }
                  exit(0);
                }
              else if (hptr->eos)
                /* clear state and return */
                {
                  free(outbufptr);
                  outbufptr = NULL;
                  free(inbufptr);
                  free(sbufptr);
                  free(window);
                  if (padbufptr != (struct svec*) NULL)
                    {
                      free(padbufptr);
                      padbufptr = (struct svec*) NULL;
                    }
                  nsamples = 0;
                  nframes = 0;
                  target_nframes = 0;
                  padptr = 0;

                  if (pptr->HPfilter == TRUE)
                    {
                      fvec_HPfilter(hptr,
                                    pptr,
                                    (struct fvec*) NULL);
                    }
                          
                  return (struct fvec*) NULL;
                }
              else
                /* this should never happen */
                {
                  fprintf(stderr,
                          "rasta (%s): bug in Abbot input.\n",
                          funcname);
                  exit(1);
                }
            }
        }

      /* Paranoia: shouldn't be able to get this far with
         hptr->eos == TRUE */
      if (hptr->eos == TRUE)
        {
          fprintf(stderr,
                  "rasta (%s): bug in Abbot input.\n",
                  funcname);
          exit(1);
        }

      /* Shift down input values */
      for (i = pptr->steppts; i < pptr->winpts; i++)
        inbufptr->values[i - pptr->steppts] = inbufptr->values[i];

      /* new values */
      overlap = pptr->winpts - pptr->steppts;
      if (pptr->inswapbytes == TRUE)
        for (i = overlap; i < pptr->winpts; i++)
          inbufptr->values[i] = 
            (float) SWAP_BYTES(sbufptr->values[i - overlap]);
      else
        for (i = overlap; i < pptr->winpts; i++)
          inbufptr->values[i] = (float) sbufptr->values[i - overlap];
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

/* read in Abbot wav input, looking for EOS */

static int
abbot_read(struct fhistory* hptr, 
           struct param* pptr,
           short* buf,
           int n)
{
  int nread;
  int t;
  unsigned short eos;
  unsigned short* p = (unsigned short*) buf;

  if (pptr->inswapbytes == TRUE)
    {
#ifdef WORDS_BIGENDIAN
      eos = 0x0080;
#else
      eos = 0x8000;
#endif
    }
  else
    {
#ifdef WORDS_BIGENDIAN
      eos = 0x8000;
#else
      eos = 0x0080;
#endif
    }

  for (nread = 0; nread < n; nread++)
    {
      t = fread((char*) p, sizeof(short), 1, stdin);
      if (t == 0)
        {
          hptr->eof = TRUE;
          break;
        }
      if (*p == eos)
        {
          hptr->eos = TRUE;
          break;
        }
      p++;
    }
  return nread;
}

#ifdef HAVE_LIBESPS
/* reads in esps data into speech structure array, storing
        length information and allocating memory as necessary */
struct fvec *get_espsdata( FILE *fptr, struct fvec *sptr, 
        struct header *ihd )
{
        int i, nread;
        char *funcname;
        struct feasd *sdrec;
        float ftmp[SPEECHBUFSIZE]; 
        int start = 0;
        
        funcname = "get_espsdata";

        sptr->length = SPEECHBUFSIZE;
        sdrec = allo_feasd_recs( ihd, FLOAT, SPEECHBUFSIZE,
                (char *)ftmp, NO);

        /* length starts as what we have allocated space for */
        while( (nread = get_feasd_recs(sdrec, 0L, SPEECHBUFSIZE, 
                        ihd, fptr)) == SPEECHBUFSIZE)
        {

                for(i=0; i<SPEECHBUFSIZE; i++)
                {
                        sptr->values[start+i] = ftmp[i];
                }
                /* increment to next size we will need */
                sptr->length += SPEECHBUFSIZE;
                start += SPEECHBUFSIZE;

                sptr->values = (float *)realloc((char*)sptr->values, 
                          (unsigned)(sptr->length * sizeof(float)) );
                if(sptr->values == (float *)NULL)
                {
                        fprintf(stderr,"Can't allocate %ld byte buffer\n",
                                sptr->length * sizeof(float));
                        exit(-1);
                      }
                
              }
        
        /* now adjust length for last bunch that is less than one buffer full */
        
        sptr->length -= SPEECHBUFSIZE;
        sptr->length += nread;
        for(i=0; i<nread; i++)
          {
            sptr->values[start+i] = ftmp[i];
          }
        
        return (sptr );
      }
#endif

/* Opens output file unless writing ESPS file; in that case,
   file is opened when write_out is first called */
FILE *open_out(struct param *pptr)
{
  FILE *fptr;

  if(((pptr->espsout) == TRUE) || ((pptr->matout) == TRUE))
    {
      fptr = (FILE *)NULL;
    }
  else if(strcmp(pptr->outfname, "-") == 0)
    {
      fptr = stdout;
    }
  else
    {
      fptr = fopen(pptr->outfname, "w");
      if (fptr == (FILE *)NULL)
        {
          fprintf(stderr,"Error opening %s\n", pptr->outfname);
          exit(-1);
        }
    }
  return (fptr);
}

/* General calling routine to write out a vector */
void write_out( struct param *pptr, FILE *outfp, struct fvec *outvec )
{
#ifdef HAVE_LIBESPS
  /* ESPS variables */
  static struct header *outhead = NULL;
  static struct fea_data *outrec;
  static float *opdata;
  static double rec_freq;

  /* Ignore outfp if an ESPS file; file is opened an
     written to within this routine only. */
  static FILE *esps_fp;
#endif

#ifdef HAVE_LIBMAT
  /* MATLAB variables */
  static MATFile *matfptr;
  static double *matr;
  static double *matr_start = (double *)NULL;
  static double *matr_end;
#endif

  int i;
  char *funcname;
  funcname = "write_out";

  if((pptr->espsout)   == TRUE)
    {
#ifdef HAVE_LIBESPS
      if(outhead == (struct header *)NULL)
        {
          rec_freq = 1.0 / (pptr->stepsize * .001);
          eopen("rasta_plp", pptr->outfname, "w", NONE, NONE,
                (struct header **)NULL, &esps_fp);              
          outhead = new_header(FT_FEA);
          add_fea_fld( "rasta_plp", (long)outvec->length, 
                      1, NULL, FLOAT, NULL, outhead);
          *add_genhd_d("start_time", NULL,1,outhead) = Start_time;
          *add_genhd_d("record_freq", NULL,1,outhead) = rec_freq;
          write_header( outhead, esps_fp);
          outrec = allo_fea_rec( outhead );
          opdata = (float *)get_fea_ptr( outrec, "rasta_plp", 
                                        outhead);
                          
          /* ESPS seems to handle its own error
             checking (pretty much) */
        }
      for(i=0; i<outvec->length; i++)
        {
          opdata[i] = outvec->values[i];
        }
      put_fea_rec(outrec, outhead, esps_fp);
#endif
    }
  else if((pptr->matout) == TRUE )
    {
#ifdef HAVE_LIBMAT
      if(matr_start == (double *)NULL)
        {
          matr = (double *) malloc((pptr->nframes * outvec->length) * sizeof(double));
          if(matr == (double *)NULL)
            {
              fprintf(stderr,"Can't allocate output buffer\n");
              exit(-1);
            }
          matr_start = matr;
          matr_end   = matr + (pptr->nframes * outvec->length);
        }
      for(i=0; i<outvec->length; i++, matr++)
        {
          *matr = (double)outvec->values[i];
        }       
      if( matr == matr_end )
        {
          /* open MAT-file */
          matfptr = matOpen(pptr->outfname,"w");
          if(matfptr == (MATFile *)NULL)
            {
              fprintf(stderr,"Error opening %s\n", pptr->outfname);
              exit(-1);
            }
          /* save output as MAT-file */
          if(matPutFull(matfptr, "rasta_out", outvec->length, pptr->nframes, matr_start, NULL) != 0)
            {
              fprintf(stderr, "Error saving %s\n", pptr->outfname);
              exit(-1);
            }                               
          matClose(matfptr);
          mxFree;
        }
#endif
    }
  else if ((pptr->ascout == TRUE) ||
           (pptr->crbout == TRUE) ||
           (pptr->comcrbout == TRUE))
    {
      print_vec(pptr, outfp, outvec, pptr->nout);
    }
  else if (pptr->abbotIO == TRUE)
    {
      abbotout_vec(pptr, outvec);
    }
  else
    {
      binout_vec( pptr, outfp, outvec );
    }
}

/* Print out ascii for float vector with specified width (n columns) */
void print_vec(const struct param *pptr, FILE *fp, struct fvec *fptr, int width)
{
  int i;
  char *funcname;
  int lastfilt;

  funcname = "print_vec";
  if ((pptr->crbout == FALSE) && (pptr->comcrbout == FALSE))
    {
        
      for(i=0; i<fptr->length; i++)
        {
          fprintf(fp, "%g ", fptr->values[i]);
          if((i+1)%width == 0)
            {
              fprintf(fp, "\n");
            }
        }
    }
  else
    {
      lastfilt = pptr->nfilts - pptr->first_good;
      for (i= pptr->first_good; i<lastfilt; i++)
        {
          fprintf(fp, "%g ", fptr->values[i]);
        }
      fprintf(fp, "\n");
    }

}

/* Swap the bytes in a floating point value */
float
swapbytes_float(float f)
{
    union {
        float f;
        char b[sizeof(float)];
    } in, out;
    int i;

    in.f = f;
    for (i=0; i<sizeof(float); i++)
    {
        out.b[i] = in.b[sizeof(float)-i-1];
    }
    return out.f;
}

/* Send float vector values to output stream */
void binout_vec( const struct param* pptr,
                 FILE *fp,
                 struct fvec *fptr )
{
  char *funcname;
  int count;
  float* buf;
  float* buf_endp;
  float* sp = fptr->values;
  float* dp;

  funcname = "binout_vec";

  if (pptr->outswapbytes)
    {
      if ((buf = (float*) malloc(sizeof(float) * fptr->length)) ==
          (float*) NULL)
        {
          fprintf(stderr, "rasta (%s): out of heap space.\n",
                  funcname);
          exit(1);
        }
      buf_endp = buf + fptr->length;
      for (dp = buf; dp != buf_endp; dp++, sp++)
        {
          *dp = swapbytes_float(*sp);
        }
      count = fwrite((char*) buf, sizeof(float), fptr->length, fp);
    }
  else
    {
      count = fwrite((char*) fptr->values,
                     sizeof(float),
                     fptr->length,
                     fp);
    }

  if (count != fptr->length)
    {
      fprintf(stderr, "rasta (%s): error writing output.\n",
              funcname);
      exit(1);
    }
}

/* Send float vector values to output stream in Abbot format */
static void
abbotout_vec(const struct param* pptr,
             struct fvec *fptr)
{
  char *funcname;
  int count;
  const unsigned char eos = 0x00;
  float* buf_endp;
  float* sp = fptr->values;
  float* dp;

  funcname = "abbotout_vec";

  if (abbot_outbuf == (float*) NULL)
    /* first frame in utterance.  Allocate the output buffer. */
    {
      if ((abbot_outbuf =
           (float*) malloc(sizeof(float) * fptr->length)) ==
          (float*) NULL)
        {
          fprintf(stderr, "rasta (%s): out of heap space.\n",
                  funcname);
          exit(1);
        }
    }
  else
    /* write out last frame */
    {
      if (fwrite((char*) &eos, 1, 1, stdout) != 1)
        {
          fprintf(stderr, "rasta (%s): error writing output.\n",
                  funcname);
          exit(1);
        }

      count = fwrite((char*) abbot_outbuf,
                     sizeof(float),
                     fptr->length,
                     stdout);

      if (count != fptr->length)
        {
          fprintf(stderr, "rasta (%s): error writing output.\n",
                  funcname);
          exit(1);
        }
    }

  /* fill buffer with next frame */

  buf_endp = abbot_outbuf + fptr->length;
  if (pptr->outswapbytes)
    {
      for (dp = abbot_outbuf; dp != buf_endp; dp++, sp++)
        {
          *dp = swapbytes_float(*sp);
        }
    }
  else
    {
      for (dp = abbot_outbuf; dp != buf_endp; dp++, sp++)
        {
          *dp = *sp;
        }
    }
}

/* Write an EOS record to the output for Abbot I/O */

void
abbot_write_eos(struct param* pptr)
{
  int count;
  const unsigned char eos = 0x80;
  char* funcname = "abbot_write_eos";

  if (fwrite((char*) &eos, 1, 1, stdout) != 1)
    {
      fprintf(stderr, "rasta (%s): error writing output.\n",
              funcname);
      exit(1);
    }

  count = fwrite((char*) abbot_outbuf,
                 sizeof(float),
                 pptr->nout,
                 stdout);

  if (count != pptr->nout)
    {
      fprintf(stderr, "rasta (%s): error writing output.\n",
              funcname);
      exit(1);
    }

  free(abbot_outbuf);
  abbot_outbuf = (float*) NULL;
  if (fflush(stdout))
    {
      fprintf(stderr, "rasta (%s): error flushing output.\n",
              funcname);
      exit(1);
    }
}

/* digital IIR highpass filter on waveform to remove DC offset
   H(z) = (0.993076-0.993076*pow(z,-1))/(1-0.986152*pow(z,-1)) 
   offline and online version, inplace calculating */
void
fvec_HPfilter(struct fhistory* hptr,
              struct param *pptr,
              struct fvec *fptr)
{
  static double coefB,coefA;
  double d,c,p2;
  static int first_call = 1;
  static float old;
  static int start;
  float tmp;
  int i;
        
  if (hptr->eos == TRUE)
    {
      first_call = 1;
    }
  else
    {
      if (first_call)
        {
          /* computing filter coefficients */
          d = pow(10.,-3./10.);     /* 3dB passband attenuation at frequency f_p */
          c = cos(2.*M_PI* 44.7598 /(double)pptr->sampfreq); /* frequency f_p about 45 Hz */
          p2 = (1.-c+2.*d*c)/(1.-c-2.*d);
          coefA = floor((-p2-sqrt(p2*p2-1.))*1000000.) / 1000000.; /* to conform to */
          coefB = floor((1.+coefA)*500000.) / 1000000.; /* older version */

          start = 1;
          old = fptr->values[0];
        }
        
      for(i=start; i<fptr->length; i++)
        {
          tmp = fptr->values[i];
          fptr->values[i] =  coefB * (fptr->values[i] - old) + coefA * fptr->values[i-1];
          old = tmp;
        }

      if(first_call)
        {
          start = fptr->length - pptr->steppts;
          if(start < 1)
            {
              fprintf(stderr,"step size >= window size -> online highpass filter not available");
              exit(-1);
            }
          first_call = 0;
        }
    }
}

/* load history from file */
void load_history(struct fhistory *hptr, const struct param *pptr)
{
  int head[3];
  int i;
  FILE *fptr;
        

  /* open file */
  fptr = fopen(pptr->hist_fname,"r");
  if(fptr == (FILE *)NULL)
    {
      fprintf(stderr,"Warning: Cannot open %s, using normal initialization\n", pptr->hist_fname);
      return;
    }

  /* read header */
  if(fread(head,sizeof(int),3,fptr) != 3)
    {
      fprintf(stderr,"Warning: Cannot read history file header, using normal initialization\n");
      return;
    }
  if((head[0] != pptr->nfilts) || (head[1] != FIR_COEF_NUM) || (head[2] != IIR_COEF_NUM))
    {
      fprintf(stderr,"Warning: History file is incompatible (header differs), using normal initialization\n");
      return;
    }

  /* read stored noise estimation level */
  if(fread(hptr->noiseOLD, sizeof(float), head[0], fptr) != head[0])
    {
      fprintf(stderr,"Warning: History file failure, using normal initialization\n");
      return;
    }

  /* read stored RASTA filter input buffer */
  for(i=0; i<head[0]; i++)
    {
      if(fread(hptr->filtIN[i], sizeof(float), head[1], fptr) != head[1])
        {
          fprintf(stderr,"Warning: History file failure, using normal initialization\n");
          return;
        }
    }
        
  /* read stored RASTA filter input buffer */
  for(i=0; i<head[0]; i++)
    {
      if(fread(hptr->filtOUT[i], sizeof(float), head[2], fptr) != head[2])
        {
          fprintf(stderr,"Warning: History file failure, using normal initialization\n");
          return;
        }
    }
        
  /* use history values instead of normal initialization */
  hptr->normInit = FALSE;
        
  fclose(fptr);
}


/* save history to file */
void save_history(struct fhistory *hptr, const struct param *pptr)
{
  int head[3];
  int i;
  FILE *fptr;
        

  /* open file */
  fptr = fopen(pptr->hist_fname,"w");
  if(fptr == (FILE *)NULL)
    {
      fprintf(stderr,
              "Warning: cannot open %s for writing.\n",
              pptr->hist_fname);
      return;
    }

  /* write header */
  head[0] = pptr->nfilts;
  head[1] = FIR_COEF_NUM;
  head[2] = IIR_COEF_NUM;
  fwrite(head, sizeof(int), 3, fptr);
        
  /* write noise level estimation */
  fwrite(hptr->noiseOLD, sizeof(float), head[0], fptr);

  /* write RASTA filter input buffer */
  for(i=0; i<head[0]; i++)
    fwrite(hptr->filtIN[i], sizeof(float), head[1], fptr);

  /* write RASTA filter output buffer */
  for(i=0; i<head[0]; i++)
    fwrite(hptr->filtOUT[i], sizeof(float), head[2], fptr);

  fclose(fptr);
}
