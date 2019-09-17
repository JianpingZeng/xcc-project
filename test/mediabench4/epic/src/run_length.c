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
*/

#include <math.h>
#include <ctype.h>
#include "epic.h"

/*
==========================================================================
run_length_encode_zeros() -- Convert the integers in block into values
of SymbolType, encoding strings of zeros.  One bit of the symbol
(zero_run_mask) is a tag which indicates whether the symbol represents
an integer value or a string of zeros.  If it is an integer value, one
of the remaining bits (sign_mask) is used as the sign bit, and the
remaining bits encode the magnitude.  If it is a string of zeros, the
remaining bits encode the log (base 2) of the length of the zero
string.  Thus the length of a run of zeros is encoded in base two,
with one symbol representing each bit of the value.  We assume the
absolute value of the integers in block does not exceed
8^sizeof(SymbolType)/4!  The routine returns the length (in symbols)
of the encoded_stream.
==========================================================================
*/

int run_length_encode_zeros (block, block_size, encoded_stream)
  register BinIndexType *block;
  int block_size;
  register SymbolType *encoded_stream;
  {
  register BinIndexType value;
  register unsigned long block_pos = 0, stream_pos = 0, count, mask;
  register SymbolType sign_mask = 1 << (sizeof(SymbolType)*8-2);
  register SymbolType value_bits_mask = sign_mask - 1;
  register SymbolType bit_position;
  register SymbolType zero_run_mask = 1 << (sizeof(SymbolType)*8-1);

  while (block_pos < block_size)
      {
      if ((value = block[block_pos]) == 0)
	  {
	  for (count=0; block[block_pos]==0; count++) block_pos++;
	  for (mask=1, bit_position=0;
	       bit_position < LOG_MAX_IMAGE_SIZE;
	       bit_position++, mask<<=1)
	      {
	      if (mask & count)
		  {
		  if ( bit_position >= sign_mask )
		    printf("Zero length overflow error in run_length_encode_zeros!\n");
		  encoded_stream[stream_pos] = bit_position | zero_run_mask; 
		  stream_pos++;
		  }
	      }
	  }
      else 
	  {
	  if ( ((value<0)?(-value):value) > value_bits_mask )
	    printf("Value overflow error in run_length_encode_zeros!\n");
	  if (value < 0)
	    encoded_stream[stream_pos] = 
	      (((SymbolType) (- value)) & value_bits_mask) | sign_mask;
	  else
	    encoded_stream[stream_pos] = 
	      ((SymbolType) value) & value_bits_mask;
	  stream_pos++;
	  block_pos++;
	  }
      }
  return (stream_pos);
  }

run_length_decode_zeros(symbol_stream, block_size, block)
  BinIndexType *block;
  int block_size;
  SymbolType *symbol_stream;
  {
  register unsigned num_zeros, count, block_pos = 0, stream_pos = 0;
  register SymbolType the_symbol;
  register SymbolType sign_mask = 1 << (sizeof(SymbolType)*8-2);
  register SymbolType value_bits_mask = sign_mask - 1;
  register SymbolType zero_run_mask = 1 << (sizeof(SymbolType)*8-1);

  while ( block_pos < block_size )
      {
      the_symbol = symbol_stream[stream_pos];
      if (the_symbol & zero_run_mask)
	  {
	  num_zeros = (1 << (the_symbol & value_bits_mask));
 	  for (count = 0; count < num_zeros; count++, block_pos++)
	      block[block_pos] = 0;
	  }
      else
	  {
	  if (the_symbol & sign_mask)
	    block[block_pos] = - (the_symbol & value_bits_mask); 
	  else
	    block[block_pos] = the_symbol & value_bits_mask;
	  block_pos++; 
	  }
      stream_pos++;
      }
  }

/*
main(argc, argv)
  int argc;
  char *argv[];
  {
  int i, length, symbol_stream_length;
  BinIndexType *int_vector;
  SymbolType *symbol_stream;
  SymbolType mask = 1 << sizeof(SymbolType)*8-2;

  length = argc-1;
  int_vector = (BinIndexType *) malloc (length*sizeof(BinIndexType));
  symbol_stream = (SymbolType *) malloc (length*sizeof(SymbolType));
  for (i=0; i<length; i++) int_vector[i] = atoi(argv[i+1]);
  symbol_stream_length = run_length_encode_zeros(int_vector, length, symbol_stream);
  
  printf("\nEncoded Stream: ");
  for (i=0; i<symbol_stream_length; i++) printf("%04x ", symbol_stream[i]);
  printf("\n");

  run_length_decode_zeros(symbol_stream, length, int_vector);
  printf("\nDecoded Stream: ");
  for (i=0; i<length; i++) printf("%d ", int_vector[i]);
  printf("\n");
  }

--- for debugging encode:
		  if (bit_position > 12)
		    printf("Sym: %04x, num_zeros %d, stream_pos: %d, block_pos %d\n",
			   (bit_position | zero_run_mask), 
			   (1<<((bit_position | zero_run_mask) & value_bits_mask)),
			   stream_pos,
			   block_pos);

--- for debugging decode:
	  if ((the_symbol & value_bits_mask) > 12) 
	    printf("Sym: %04x, num_zeros: %d, stream_pos: %d, block_pos: %d\n",
		   the_symbol,
		   num_zeros,
		   stream_pos,
		   block_pos);


*/
