/* rasta.h */

#include "values.h" 
#include <math.h>

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

/* This declaration is to be replaced once rasta is autoconf-able */
#define WORDS_BIGENDIAN

#define two_to_the(N) (int)(pow(2.0,(N))+0.5)
#define TRUE  ( 1 == 1 )
#define FALSE ( 1 == 0 )

/* This byte-swapping routine works only if shorts are 2 or 4 bytes! */

#define MASK 0x0ff

#define SWAP_BYTES(A) ( (short) ((sizeof(short) == 2) ? \
                                 (((A & MASK) << 8) | ((A >> 8) & MASK)) : \
                                 (((A & MASK) << 24) | ((A & (MASK << 8)) << 8) | \
                                  ((A & (MASK << 16)) >> 8) | ((A >> 24) & MASK))) )

/* 	Constant values 	*/
#define SPEECHBUFSIZE 512
	/* This is only used to buffer input samples, and
	is noncritical (in other words, doesn't need to be changed
	when you change sample rate, window size, etc.) */

#define COMPRESS_EXP .33
		/* Cube root compression of spectrum is used for
	intensity - loudness conversion */

#define LOG_BASE 10. 
		/* Used for dB-related stuff */

#define LOG_MAXFLOAT ((float)log((double) MAXFLOAT ))

#define TINY 1.0e-45
  
#define MAXHIST 25
  
#define TYP_WIN_SIZE 20

#define TYP_STEP_SIZE 10

#define PHONE_SAMP_FREQ 8000

#define TYP_SAMP_FREQ 16000

#define TYP_ENHANCE 0.6

#define TYP_MODEL_ORDER 8

#define NOTHING_FLOAT 0.0

#define NOTHING 0

#define ONE	1.0

#define NOT_SET 0

#define JAH_DEFAULT 1.0e-6

#define POLE_DEFAULT .94

#define HAMMING_COEF .54

#define HANNING_COEF .50

#define RECT_COEF	1.0

#define FIR_COEF_NUM 5

#define IIR_COEF_NUM 1

/* Limits for parameter checking  */

#define MINFILTS 1

#define MAXFILTS 1000

#define MIN_NFEATS 1 

#define MAX_NFEATS 100 

/* window and step sizes in milliseconds */
#define MIN_WINSIZE 1.0

#define MAX_WINSIZE 1000.

#define MIN_STEPSIZE 1.0

#define MAX_STEPSIZE 1000.

#define MIN_SAMPFREQ 1000

#define MAX_SAMPFREQ 50000

#define MIN_POLEPOS 0.0

#define MAX_POLEPOS 1.0

#define MIN_ORDER 1

#define MAX_ORDER 100

#define MIN_LIFT .01

#define MAX_LIFT 100.

#define MIN_WINCO HANNING_COEF

#define MAX_WINCO RECT_COEF

#define MIN_RFRAC 0.0

#define MAX_RFRAC 1.0

#define MIN_JAH 1.0e-16

#define MAX_JAH 1.0e16

/*-------------------------------------------------------------*/
#define HISTO_TIME 1000

#define HISTO_STEPTIME 100

#define MAX_HISTO_WINDOWLENGTH 200

#define RESET 0

#define SET 1 
  
#define HISTO_RESOLUTION  30 

#define EPSILON  0.01

#define FILT_LENGTH 30

#define TIME_CONSTANT 0.95

#define OVER_EST_FACTOR 2.0

#define C_CONSTANT 3.0

/*-------------------------------------------------------------*/



/*-------------------------------------------------------------*/
#define MAXNJAH  32   

#define MAXMAPBAND 256

#define MAXMAPCOEF 256

/*-------------------------------------------------------------*/

/*     	Structures		*/


struct fvec
{
  float *values;
  int length;
};

struct fmat
{
  float **values;
  int nrows;
  int ncols;
};

struct svec
{
  short *values;
  int length;
};


struct range
{
  int start;
  int end;
};

struct param
{
  float winsize;       /* analysis window size in msec               */
  int winpts;          /* analysis window size in points             */
  float stepsize;      /* analysis step size in msec                 */
  int steppts;         /* analysis step size in points               */
  int padInput;        /* if true, pad input so the (n * steppts)-th
                          sample is centered in the n-th frame       */
  int sampfreq;        /* sampling frequency in Hertz                */
  int nframes;         /* number of analysis frames in sample        */
  float nyqbar;	       /* Nyquist frequency  in barks                */
  int nfilts;	       /* number of critical band filters used       */
  int first_good;      /* number of critical band filters to ignore 
                          at start and end (computed)                */
  int trapezoidal;     /* set true if the auditory filters are
                          trapezoidal                                */
  float winco;         /* window coefficient	                     */
  float polepos;       /* rasta integrator pole position             */
  int order;           /* LPC model order                            */
  int nout;            /* length of final feature vector             */
  int gainflag;        /* flag that says to use gain                 */
  float lift;          /* cepstral lifter exponent                   */
  int lrasta;          /* set true if log rasta used                 */
  int jrasta;          /* set true if jah rasta used                 */
  int cJah;            /* set true if constant Jah used              */
  char *mapcoef_fname; /* Jah Rasta mapping coefficients input text 
                          file name                                  */
  int crbout;          /* set true if critical band values after
                          bandpass filtering instead of cepstral
                          coefficients are desired as outputs        */
  int comcrbout;       /* set true if critical band values after
                          cube root compression and equalization
                          are desired as outputs.                    */
  float rfrac;         /* fraction of rasta mixed with plp           */
  float jah;           /* Jah constant                               */
  char *infname;       /* Input file name, where "-" means stdin     */
  char *num_fname;     /* RASTA Numerator polynomial file name       */
  char *denom_fname;   /* RASTA Denominator polynomial file name     */
  char *outfname;      /* Output file name, where "-" means stdout   */
  int ascin;	       /* if true, read ascii in                     */
  int ascout;	       /* if true, write ascii out                   */
  int espsin;	       /* if true, read esps                         */
  int espsout;	       /* if true, write esps                        */
  int matin;	       /* if true, read MAT                          */
  int matout;	       /* if true, write MAT                         */
  int spherein;        /* read SPHERE input                          */
  int abbotIO;         /* read and write CUED's Abbot wav format     */
  int inswapbytes;     /* swap bytes of input                        */
  int outswapbytes;    /* swap bytes of output                       */
  int debug;           /* enable debug info 	                     */
  int smallmask;       /* add small constant to power spectrum       */
  int online;          /* online, not batch file processing          */
  int HPfilter;        /* highpass filter on waveform is used        */
  int history;         /* use stored noise level estimation and
                          RASTA filter history for initialization    */
  char *hist_fname;    /* history filename                           */
};

struct map_param
{
  float reg_coef[MAXNJAH][MAXMAPBAND][MAXMAPCOEF];  /* mapping coefficients */
  float jah_set[MAXNJAH];                           /* set of quantized Jahs  */
  int   n_sets;                                     /* number of quantized Jahs */
  int   n_bands;                                    /* number of critical bands */
  int   n_coefs;                                    /* number of mapping coefficients */
  float boundaries[MAXNJAH];                        /* decision boundaries */
}; 

struct fhistory{
  float noiseOLD[MAXFILTS];          /* last noise level estimation            */
  float filtIN[MAXFILTS][MAXHIST];   /* RASTA filter input buffer              */
  float filtOUT[MAXFILTS][MAXHIST];  /* RASTA filter output buffer             */
  int normInit;  /* if true, use normal initialization
                    (RASTA filter, noise estimation) */
  int eos;       /* for Abbot I/O.  If true, we've hit the end of an
                    utterance and need to reset the state in the
                    rasta filter, highpass filter, and noise
                    estimation */
  int eof;       /* for Abbot I/O.  Indicates we have hit the end of
                    input */
};
