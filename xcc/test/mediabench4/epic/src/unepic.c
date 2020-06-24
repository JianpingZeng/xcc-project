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

See notes in files README, epic.h.

*/
#include "stats_time.h"
#include "epic.h"

main(argc, argv)
  int argc;
  char *argv[];
  {
//SS_START
  FILE *infile, *outfile;
  int *result, *pyr;
  BinIndexType *q_pyr;
  int x_size, y_size, num_levels;
  double scale_factor, dtemp;
  BinValueType *bin_size;
  Byte the_tag;
  long temp;
  int i, index, im_count=0, block_size, block_pos, first_image_flag;
  unsigned int symbol_stream_length;
  SymbolType *symbol_stream;
  struct code_node *huffman_tree;

  /* --- Parse arglist, Reading the data from the input file, unpacking it, 
     and decoding it, and putting it into the quantized pyramid --- */
  parse_unepic_args(argc, argv, &infile, &num_levels, &x_size, &y_size,
		    &scale_factor, &outfile);
  
  printf("Read %ld byte header.\n", (temp = ftell(infile)));
  printf("num_levels: %d, x_size: %d, y_size: %d, scale: %8lf\n",
	 num_levels, x_size, y_size, scale_factor);

  /* --- Read and decode the file --- */
  q_pyr = (BinIndexType *) check_malloc( x_size*y_size*sizeof(*q_pyr) );
  bin_size = (BinValueType *) check_malloc( (3*num_levels+1)*sizeof(*bin_size) );

  while ( im_count < (3*num_levels+1) )
      {
      read_byte(the_tag, infile);
      for (first_image_flag = 1;
	   the_tag & BIN_INFO_TAG; )
	  {
	  index = the_tag & ~BIN_INFO_TAG;
	  /* printf("BIN-TAG: %02x (index: %d),  ", the_tag, index); */
	  if (first_image_flag)
	      {
	      first_image_flag = 0;
	      if (index == 0)   /* lopass */
		  { block_size = (x_size*y_size)>>(2*num_levels); block_pos = 0; }
	      else
		  {
		  block_size = (x_size*y_size)>>(2*(num_levels-(index-1)/3));
		  block_pos  = block_size * (1+(index-1)%3); 
		  }
	      }
	  else block_size += (x_size*y_size)>>(2*(num_levels-(index-1)/3));
	  read_short(bin_size[index], infile);
	  im_count++;
	  read_byte(the_tag, infile);
	  }
      printf("Read %ld bytes of Binsize Info.\n", (ftell(infile)-temp-1));
      temp = ftell(infile)-1;
	
      /* printf("DATA-tag: %02x,  data_block_size: %d\n", the_tag, block_size); */
      if ( the_tag == RAW_DATA_TAG )
	  {
	  read_array((q_pyr+block_pos), block_size, infile);
	  printf("Read %ld bytes of Raw data.\n", (ftell(infile)-temp));
	  temp = ftell(infile);
	  }
      else if ( the_tag == HUFFMAN_DATA_TAG )
	  {
	  read_int(symbol_stream_length, infile);
	  symbol_stream = (SymbolType *) 
	    check_malloc(symbol_stream_length*sizeof(SymbolType));
	  huffman_tree = read_huffman_tree( infile );
	  printf("Read %ld byte huffman tree.\n", ftell(infile)-temp);
	  temp = ftell(infile);
	  read_and_huffman_decode(infile, huffman_tree, symbol_stream, 
				  symbol_stream_length);
	  printf("Read %ld bytes of encoded data.\n", (ftell(infile)-temp));
	  run_length_decode_zeros(symbol_stream, block_size, (q_pyr+block_pos));
	  }
      else { printf("ERROR: Bad data tag in file: %02x\n", the_tag); exit(-1); }
      }
  printf("Read a total of %ld bytes.\n", ftell(infile));
  fclose(infile);

  clock();  /* initialize process timer */

  /* -- Unquantize the pyramid -- */
  printf("Unquantizing pyramid ...\n");
  pyr = (int *) check_malloc( x_size*y_size*sizeof(*pyr) );
  unquantize_pyr(q_pyr, pyr, (x_size*y_size), num_levels, bin_size);
  check_free((char *) q_pyr);

  /* -- Collapse pyramid, and divide by scale_factor -- */
  printf("Collapsing pyramid ... \n");
  result = (int *) check_malloc (x_size*y_size*sizeof(*result));

  collapse_pyr(pyr, result, x_size, y_size, num_levels);

  /* ** SLOW ** */
  for (i=0; i<x_size*y_size; i++)
    {
    dtemp = result[i] / scale_factor;
    if (dtemp < 0) result[i] = 0;              /* clip below */
    else if (dtemp > 255) result[i] = 255;     /* clip above */
    else result[i] = (int) (dtemp + 0.5);      /* round off  */
    }
  printf("UNEPIC time = %ld milliseconds\n", (clock() / 1000) );

  printf("Writing output file ... \n");

  if (WRITE_PGM_FILE) write_pgm_image(outfile, result, x_size, y_size);
  else                write_byte_image(outfile, result, x_size, y_size);

  check_free ((char *) pyr);
  check_free ((char *) result);
  check_free ((char *) bin_size );
  check_free ((char *) symbol_stream );
//SS_STOP
//SS_PRINT

  }
