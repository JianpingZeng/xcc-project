/* DESC.C - Data Encryption Standard routines for RSAREF
 */

#include "global.h"
#include "rsaref.h"
#include "des.h"

/* Initial permutation IP.
 */
unsigned char IP[64] = {
  58, 50, 42, 34, 26, 18, 10,  2, 60, 52, 44, 36, 28, 20, 12,  4,
  62, 54, 46, 38, 30, 22, 14,  6, 64, 56, 48, 40, 32, 24, 16,  8,
  57, 49, 41, 33, 25, 17,  9,  1, 59, 51, 43, 35, 27, 19, 11,  3,
  61, 53, 45, 37, 29, 21, 13,  5, 63, 55, 47, 39, 31, 23, 15,  7
};

/* Final permutation FP = IP^{-1}.
 */
unsigned char FP[64] = {
  40,  8, 48, 16, 56, 24, 64, 32, 39,  7, 47, 15, 55, 23, 63, 31, 
  38,  6, 46, 14, 54, 22, 62, 30, 37,  5, 45, 13, 53, 21, 61, 29, 
  36,  4, 44, 12, 52, 20, 60, 28, 35,  3, 43, 11, 51, 19, 59, 27, 
  34,  2, 42, 10, 50, 18, 58, 26, 33,  1, 41,  9, 49, 17, 57, 25
};

/* Permuted-choice 1.
 */
unsigned char PC1[] = {
  57, 49, 41, 33, 25, 17,  9,  1, 58, 50, 42, 34, 26, 18,
  10,  2, 59, 51, 43, 35, 27, 19, 11,  3, 60, 52, 44, 36,
  63, 55, 47, 39, 31, 23, 15,  7, 62, 54, 46, 38, 30, 22,
  14,  6, 61, 53, 45, 37, 29, 21, 13,  5, 28, 20, 12,  4
};

/* Left shifts for the key schedule.
 */
unsigned char LS[16] = {
  1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1
};

/* Permuted-choice 2.
 */
unsigned char PC2[] = {
  14, 17, 11, 24,  1,  5,  3, 28, 15,  6, 21, 10,
  23, 19, 12,  4, 26,  8, 16,  7, 27, 20, 13,  2,
  41, 52, 31, 37, 47, 55, 30, 40, 51, 45, 33, 48, 
  44, 49, 39, 56, 34, 53, 46, 42, 50, 36, 29, 32
};

/* Bit-selection table E.
 */
unsigned char E[] = {
  32,  1,  2,  3,  4,  5,  4,  5,  6,  7,  8,  9,
   8,  9, 10, 11, 12, 13, 12, 13, 14, 15, 16, 17,
  16, 17, 18, 19, 20, 21, 20, 21, 22, 23, 24, 25,
  24, 25, 26, 27, 28, 29, 28, 29, 30, 31, 32,  1
};

/* Selection functions (S-boxes). [[These are 0-origin indexed.]]
 */
unsigned char S[8][64] = {
  14,  4, 13,  1,  2, 15, 11,  8,  3, 10,  6, 12,  5,  9,  0,  7,
   0, 15,  7,  4, 14,  2, 13,  1, 10,  6, 12, 11,  9,  5,  3,  8,
   4,  1, 14,  8, 13,  6,  2, 11, 15, 12,  9,  7,  3, 10,  5,  0,
  15, 12,  8,  2,  4,  9,  1,  7,  5, 11,  3, 14, 10,  0,  6, 13,

  15,  1,  8, 14,  6, 11,  3,  4,  9,  7,  2, 13, 12,  0,  5, 10,
   3, 13,  4,  7, 15,  2,  8, 14, 12,  0,  1, 10,  6,  9, 11,  5,
   0, 14,  7, 11, 10,  4, 13,  1,  5,  8, 12,  6,  9,  3,  2, 15,
  13,  8, 10,  1,  3, 15,  4,  2, 11,  6,  7, 12,  0,  5, 14,  9,

  10,  0,  9, 14,  6,  3, 15,  5,  1, 13, 12,  7, 11,  4,  2,  8,
  13,  7,  0,  9,  3,  4,  6, 10,  2,  8,  5, 14, 12, 11, 15,  1,
  13,  6,  4,  9,  8, 15,  3,  0, 11,  1,  2, 12,  5, 10, 14,  7,
   1, 10, 13,  0,  6,  9,  8,  7,  4, 15, 14,  3, 11,  5,  2, 12,

   7, 13, 14,  3,  0,  6,  9, 10,  1,  2,  8,  5, 11, 12,  4, 15,
  13,  8, 11,  5,  6, 15,  0,  3,  4,  7,  2, 12,  1, 10, 14,  9,
  10,  6,  9,  0, 12, 11,  7, 13, 15,  1,  3, 14,  5,  2,  8,  4,
   3, 15,  0,  6, 10,  1, 13,  8,  9,  4,  5, 11, 12,  7,  2, 14,

   2, 12,  4,  1,  7, 10, 11,  6,  8,  5,  3, 15, 13,  0, 14,  9,
  14, 11,  2, 12,  4,  7, 13,  1,  5,  0, 15, 10,  3,  9,  8,  6,
   4,  2,  1, 11, 10, 13,  7,  8, 15,  9, 12,  5,  6,  3,  0, 14,
  11,  8, 12,  7,  1, 14,  2, 13,  6, 15,  0,  9, 10,  4,  5,  3,

  12,  1, 10, 15,  9,  2,  6,  8,  0, 13,  3,  4, 14,  7,  5, 11,
  10, 15,  4,  2,  7, 12,  9,  5,  6,  1, 13, 14,  0, 11,  3,  8,
   9, 14, 15,  5,  2,  8, 12,  3,  7,  0,  4, 10,  1, 13, 11,  6,
   4,  3,  2, 12,  9,  5, 15, 10, 11, 14,  1,  7,  6,  0,  8, 13,

   4, 11,  2, 14, 15,  0,  8, 13,  3, 12,  9,  7,  5, 10,  6,  1,
  13,  0, 11,  7,  4,  9,  1, 10, 14,  3,  5, 12,  2, 15,  8,  6,
   1,  4, 11, 13, 12,  3,  7, 14, 10, 15,  6,  8,  0,  5,  9,  2,
   6, 11, 13,  8,  1,  4, 10,  7,  9,  5,  0, 15, 14,  2,  3, 12,

  13,  2,  8,  4,  6, 15, 11,  1, 10,  9,  3, 14,  5,  0, 12,  7,
   1, 15, 13,  8, 10,  3,  7,  4, 12,  5,  6, 11,  0, 14,  9,  2,
   7, 11,  4,  1,  9, 12, 14,  2,  0,  6, 10, 13, 15,  3,  5,  8,
   2,  1, 14,  7,  4, 10,  8, 13, 15, 12,  9,  0,  3,  5,  6, 11
};

/* Permutation P.
 */
unsigned char P[32] = {
  16,  7, 20, 21, 29, 12, 28, 17,  1, 15, 23, 26,  5, 18, 31, 10,
   2,  8, 24, 14, 32, 27,  3,  9, 19, 13, 30,  6, 22, 11,  4, 25
};

static void Unpack PROTO_LIST
  ((unsigned char *, unsigned char *, unsigned int));
static void Pack PROTO_LIST
  ((unsigned char *, unsigned char *, unsigned int));

/* DES-CBC initialization. Begins a DES-CBC operation, writing a new
   context.
 */
void DES_CBCInit (context, key, iv, encrypt)
DES_CBC_CTX *context;                                    /* DES-CBC context */
unsigned char key[8];                                            /* DES key */
unsigned char iv[8];                             /* DES initializing vector */
int encrypt;                     /* encrypt flag (1 = encrypt, 0 = decrypt) */
{
  unsigned char CD[56], keyBit[64], t;
  unsigned int i, j;

  /* Copy encrypt flag to context.
   */
  context->encrypt = encrypt;

  /* Unpack initializing vector into context.
   */
  Unpack (context->ivBit, iv, 8);

  /* Unpack key and generate C and D by permuting the key according to PC1.
   */
  Unpack (keyBit, key, 8);
  for (i = 0; i < 56; i++)
    CD[i] = keyBit[PC1[i]-1];
  
  /* Generate subkeys Ki by rotating C and D according to schedule and
     permuting C and D according to PC2.
   */
  for (i = 0; i < 16; i++) {
    for (j = 0; j < LS[i]; j++) {
      t = CD[0];
      R_memcpy ((POINTER)CD, (POINTER)&CD[1], 27);
      CD[27] = t;
      t = CD[28];
      R_memcpy ((POINTER)&CD[28], (POINTER)&CD[29], 27);
      CD[55] = t;
    }
    
    for (j = 0; j < 48; j++)
      context->subkeyBit[i][j] = CD[PC2[j]-1];
  }

  /* Zeroize sensitive information.
   */
  R_memset ((POINTER)CD, 0, sizeof (CD));
  R_memset ((POINTER)keyBit, 0, sizeof (keyBit));
}

/* DES-CBC block update operation. Continues a DES-CBC encryption
   operation, processing eight-byte message blocks, and updating
   the context.
 */
int DES_CBCUpdate (context, output, input, len)
DES_CBC_CTX *context;                                    /* DES-CBC context */
unsigned char *output;                                      /* output block */
unsigned char *input;                                        /* input block */
unsigned int len;                      /* length of input and output blocks */
{
  unsigned char inputBit[64], LR[64], newL[32], outputBit[64], sInput[48],
    sOutput[32], t;
  unsigned int i, j, k;
  
  if (len % 8)
    return (RE_LEN);
  
  for (i = 0; i < len/8; i++) {

    /* Unpack input block and set LR = IP(input ^ iv) (encrypt) or
       LR = IP(input) (decrypt).
     */
    Unpack (inputBit, &input[8*i], 8);
  
    if (context->encrypt)
      for (j = 0; j < 64; j++)
        LR[j] = inputBit[IP[j]-1] ^ context->ivBit[IP[j]-1];
    else
      for (j = 0; j < 64; j++)
        LR[j] = inputBit[IP[j]-1];
  
    /* 16 rounds.
     */
    for (j = 0; j < 16; j++) {
    
      /* Save R, which will be the new L.
       */
      R_memcpy ((POINTER)newL, &LR[32], 32);

      /* Compute sInput = E(R) ^ Kj (encrypt) or sInput = E(R) ^ K{15-j}
         (decrypt).
       */
      if (context->encrypt)
        for (k = 0; k < 48; k++)
          sInput[k] = LR[E[k]+31] ^ context->subkeyBit[j][k];
      else
        for (k = 0; k < 48; k++)
          sInput[k] = LR[E[k]+31] ^ context->subkeyBit[15-j][k];
    
      /* Apply eight S boxes. Index into S box k is formed from these
         bits of sInput:

                  6*k 6*k+5 6*k+1 6*k+2 6*k+3 6*k+4

         Value of S box k becomes these bits of sOutput:
         
                        4*k 4*k+1 4*k+2 4*k+3
       */
      for (k = 0; k < 8; k++) {
        t = S[k][(sInput[6*k] << 5) | (sInput[6*k + 5] << 4) |
                 (sInput[6*k + 1] << 3) | (sInput[6*k + 2] << 2) |
                 (sInput[6*k + 3] << 1) | (sInput[6*k + 4] << 0)];

        sOutput[4*k] = (unsigned char)((t >> 3) & 1);
        sOutput[4*k + 1] = (unsigned char)((t >> 2) & 1);
        sOutput[4*k + 2] = (unsigned char)((t >> 1) & 1);
        sOutput[4*k + 3] = (unsigned char)(t & 1);
      }

      /* Compute new R = L ^ P(sOutput).
       */
      for (k = 0; k < 32; k++)
        LR[k+32] = LR[k] ^ sOutput[P[k]-1];
    
      /* Restore new L.
       */
      R_memcpy ((POINTER)LR, (POINTER)newL, 32);
    }

    /* Exchange L and R.
     */
    R_memcpy ((POINTER)newL, (POINTER)&LR[32], 32);
    R_memcpy ((POINTER)&LR[32], (POINTER)LR, 32);
    R_memcpy ((POINTER)LR, (POINTER)newL, 32);

    /* Set output = FP(LR) (encrypt) or FP(LR) ^ iv (decrypt), and pack
       output block.
     */
    if (context->encrypt)
      for (j = 0; j < 64; j++)
        outputBit[j] = LR[FP[j]-1];
    else
      for (j = 0; j < 64; j++)
        outputBit[j] = LR[FP[j]-1] ^ context->ivBit[j];

    Pack (&output[8*i], outputBit, 8);
  
    /* Set iv = output (encrypt) or iv = input (decrypt).
     */
    if (context->encrypt)
      R_memcpy ((POINTER)context->ivBit, (POINTER)outputBit, 64);
    else
      R_memcpy ((POINTER)context->ivBit, (POINTER)inputBit, 64);
  }
  
  /* Zeroize sensitive information.
   */
  R_memset ((POINTER)inputBit, 0, sizeof (inputBit));
  R_memset ((POINTER)LR, 0, sizeof (LR));
  R_memset ((POINTER)newL, 0, sizeof (newL));
  R_memset ((POINTER)outputBit, 0, sizeof (outputBit));
  R_memset ((POINTER)sInput, 0, sizeof (sInput));
  R_memset ((POINTER)sOutput, 0, sizeof (sOutput));
  t = 0;
  
  return (0);
}

/* DES-CBC finalization operation. Ends a DES-CBC encryption operation,
   zeroizing the context.
 */
void DES_CBCFinal (context)
DES_CBC_CTX *context;
{
  R_memset ((POINTER)context, 0, sizeof (*context));
}

static void Unpack (bit, block, blockLen)
unsigned char *bit;                                            /* bit array */
unsigned char *block;                                         /* byte array */
unsigned int blockLen;                              /* length of byte array */
{
  unsigned int i, j;
  unsigned char t;

  for (i = 0; i < blockLen; i++) {
    t = block[i];
    for (j = 0; j < 8; j++)
      bit[8*i + j] = (unsigned char)((t >> (7-j)) & 1);
  }
}

static void Pack (block, bit, blockLen)
unsigned char *block;                                         /* byte array */
unsigned char *bit;                                            /* bit array */
unsigned int blockLen;                              /* length of byte array */
{
  unsigned int i, j;
  unsigned char t;

  for (i = 0; i < blockLen; i++) {
    t = 0;
    for (j = 0; j < 8; j++)
      t |= bit[8*i +j] << (7-j);
    block[i] = t;
  }
}
