/*
--------------------------------------------------------------------- 
---		 EPIC (Efficient Pyramid Image Coder)             ---
---	 Designed by Eero P. Simoncelli and Edward H. Adelson     ---
---		    Written by Eero P. Simoncelli                 ---
---  Developed at the Vision Science Group, The Media Laboratory  ---
---	Copyright 1989, Massachusetts Institute of Technology     ---
---			 All rights reserved.                     ---
---------------------------------------------------------------------

Permission to use, copy, or modify this software and its documentation
for educational and research purposes only and without fee is hereby
granted, provided that this copyright notice appear on all copies and
supporting documentation.  For any other uses of this software, in
original or modified form, including but not limited to distribution
in whole or in part, specific prior permission must be obtained from
M.I.T. and the authors.  These programs shall not be used, rewritten,
or adapted as the basis of a commercial software or hardware product
without first obtaining appropriate licenses from M.I.T.  M.I.T. makes
no representations about the suitability of this software for any
purpose.  It is provided "as is" without express or implied warranty.

---------------------------------------------------------------------

See notes in files README, file-format.txt, and epic.h.

*/

#include "epic.h"
#include "stats_time.h"

/* Hardwire the filters.  Note that these sum to 1.0 */
static float lo_filter[FILTER_SIZE] =     
  {-0.0012475221, -0.0024950907, 0.0087309530, 0.0199579580,
   -0.0505290000, -0.1205509700, 0.2930455800,
    0.7061761600,
    0.2930455800, -0.1205509700, -0.0505290000,
    0.0199579580, 0.0087309530, -0.0024950907, -0.0012475221};

static float hi_filter[FILTER_SIZE] =
  { 0.0012475221, -0.0024950907, -0.0087309530, 0.0199579580,
    0.0505290000, -0.1205509700, -0.2930455800,
    0.7061761600,
   -0.2930455800, -0.1205509700, 0.0505290000,
    0.0199579580, -0.0087309530, -0.0024950907, 0.0012475221};

/*
*** old 15-tap filter:
{-0.0012262390415035739,   -0.003678700794579351,    0.008583567145971109,
    0.02084685570723222,   -0.050280283267807716,   -0.12137238637509237,
    0.29292296128706447,    0.7084084506774304,    0.29292296128706447,
   -0.12137238637509237,   -0.050280283267807716,    0.02084685570723222,
    0.008583567145971109,   -0.003678700794579351,   -0.0012262390415035739};

*** 11-tap filter:
  { 0.007987761489921101,  0.02011649866148413,  -0.05015758257647976,
   -0.12422330961337678,   0.29216982108655865,   0.7082136219037853,
    0.29216982108655865,  -0.12422330961337678,  -0.05015758257647976,
    0.02011649866148413,   0.007987761489921101 };

*** 9-tap orthogonal filters (These work noticably better for coding, at 
    the expense of slower reconstruction). TO use them, replace the
    definitions (above) of lo_filter and hi_filter.  THen modify the
    Makefile to use collapse_ortho_pyr.c in place of collapse_pyr.c.
    Source: E Simoncelli and E Adelson, "Subband Transforms", Chapter 4 
    of Subband Image Coding, ed. John W. Woods, Kluwer Academic 
    Publishers, (1990). 

  {0.019849565349356158, -0.04309091740554781, -0.05188793647806494,
   0.2932311998087948,    0.5637961774509238,   0.2932311998087948,
  -0.05188793647806494, -0.04309091740554781, 0.019849565349356158 };
  {0.019849565349356158, 0.04309091740554781, -0.05188793647806494,
   -0.2932311998087948,    0.5637961774509238,   -0.2932311998087948,
  -0.05188793647806494, 0.04309091740554781, 0.019849565349356158 };
*/

main(argc, argv)
  int argc;
  char *argv[];
  {
//SS_START
  FILE *outfile;
  float *image;
  unsigned short scale_factor =  SCALE_FACTOR;
  int i,j, level, im_count, x_size, y_size, num_levels, lopass_im_size;
  long temp;
  double compression_factor;
  int huffman_tree_size;
  BinValueType *bin_size;
  BinIndexType *q_pyr;
  unsigned int symbol_stream_length, encoded_stream_length;
  SymbolType *symbol_stream;
  Byte *huffman_tree;
  CodeType *encoded_stream;           /* a bit stream */

  /* --- Parse the arglist, determining parameter values, mallocing
     and reading the float image, opening output file, etc. --- */
  parse_epic_args(argc, argv, &image, &x_size, &y_size, &num_levels,
		  &compression_factor, &outfile);

  lopass_im_size = (x_size*y_size) / (1<<(2*num_levels));
  q_pyr = (BinIndexType *) check_malloc( x_size*y_size*sizeof(*q_pyr) );
  bin_size = (BinValueType *) check_malloc( (3*num_levels+1)*sizeof(*bin_size) );

  fprintf(stdout,"Xsize: %d, Ysize: %d.\n", x_size, y_size);

  if (num_levels <= 0)
      {
      fprintf(stderr,"Error: attempting to construct pyramid with %d levels.\n\
      Are your image dimensions odd?\n", num_levels);
      exit(-1);
      }

  /* --- Multiply image by scale_factor and build a float pyramid.  Note that 
     the pyramid is built on top of the image, destroying the original data! --- */
  printf("Building pyramid, %d levels ...\n", num_levels);
  for (i=0;i<x_size*y_size;i++) image[i] *= scale_factor;
  build_pyr(image, x_size, y_size, num_levels, lo_filter, hi_filter, FILTER_SIZE);
  
  /* --- Quantize the float pyramid - return integer pyramid and bin value vectors. --- */

  printf("Quantizing, binsize = %4lf ...\n", compression_factor);
  compression_factor *= scale_factor;
  quantize_pyr(image, q_pyr, x_size*y_size, num_levels, compression_factor, bin_size);
  check_free ((char *) image);    /* no longer need the original pyramid */
  
  /* From here down should probably be a function: encode_and_write_epic_file */
  /* --- Run-length encode the pyramid data - return a stream of integers --- */
  printf("Run-length coding ...\n");
  symbol_stream = (SymbolType *) check_malloc( x_size*y_size*sizeof(SymbolType) );
  symbol_stream_length = run_length_encode_zeros((q_pyr+lopass_im_size),
						 (x_size*y_size-lopass_im_size),
						 symbol_stream);
  /* --- Huffman encode the data stream --- */
  printf("Huffman coding ... \n");
  encoded_stream = (CodeType *) check_malloc( symbol_stream_length*sizeof(SymbolType) );
  encoded_stream_length = huffman_encode(symbol_stream, symbol_stream_length, 
					 &huffman_tree, &huffman_tree_size, encoded_stream);

  printf("Low_pass_size: %d,  Symbol_stream_length: %d,  Huffman_stream_length: %d\n",  lopass_im_size, symbol_stream_length, encoded_stream_length);

  /* --- Store all necessary information in an EPIC file (should be a function) --- */
  printf ("Writing EPIC file ...\n");
  write_byte (EPIC_ID_TAG, outfile);
  write_byte (num_levels, outfile);
  write_short (x_size, outfile);
  write_short (y_size, outfile);
  write_short (scale_factor, outfile);
  printf("Stored %ld byte header.\n", (temp = ftell(outfile)));

  im_count = 0;
  write_byte ( (BIN_INFO_TAG | im_count), outfile);
  write_short (bin_size[im_count], outfile);
  write_byte (RAW_DATA_TAG, outfile);
  write_array (q_pyr, lopass_im_size, outfile);
  printf("Stored %ld bytes of Raw (lowpass) Data.\n", (ftell(outfile)-temp));
  temp = ftell(outfile);

  for (im_count=1, level=num_levels; level>0; level--)
    for (i=1; i<4; i++, im_count++)
	{
	write_byte ( (BIN_INFO_TAG | im_count), outfile);
	write_short (bin_size[im_count], outfile);
	}
  printf("Stored %ld bytes of Binsize Info.\n", (ftell(outfile)-temp));
  temp = ftell(outfile);

  write_byte (HUFFMAN_DATA_TAG, outfile);
  write_int (symbol_stream_length, outfile);
  write_array (huffman_tree, huffman_tree_size, outfile);
  printf("Stored %ld byte Huffman tree .\n", (ftell(outfile)-temp));
  temp = ftell(outfile);
  write_array (encoded_stream, encoded_stream_length, outfile);
  printf("Stored %ld bytes of encoded data.\n", (ftell(outfile)-temp));

  printf ("Storage total: %ld bytes (%3f bits/pixel)\nCompression ratio = %2f:1.\n",
	  ftell(outfile), 
	  (8*((float) ftell(outfile)))/(x_size*y_size),
	  (x_size*y_size)/((float) ftell(outfile)) );
  fclose(outfile);

  check_free ((char *) q_pyr );
  check_free ((char *) bin_size );
  check_free ((char *) symbol_stream );
  check_free ((char *) encoded_stream );
//SS_STOP
//SS_PRINT

  }

