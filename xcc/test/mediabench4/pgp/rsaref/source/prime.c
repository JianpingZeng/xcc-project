/* PRIME.C - primality-testing routines
 */

/* Copyright (C) 1991-2 RSA Laboratories, a division of RSA Data
   Security, Inc. All rights reserved.
 */

#include "global.h"
#include "rsaref.h"
#include "nn.h"
#include "prime.h"

static unsigned int SMALL_PRIMES[] = { 3, 5, 7, 11 };
#define SMALL_PRIME_COUNT 4

static int RSAPrime PROTO_LIST
  ((NN_DIGIT *, unsigned int, NN_DIGIT *, unsigned int));
static int ProbablePrime PROTO_LIST ((NN_DIGIT *, unsigned int));
static int SmallFactor PROTO_LIST ((NN_DIGIT *, unsigned int));
static int FermatTest PROTO_LIST ((NN_DIGIT *, unsigned int));
static int RelativelyPrime PROTO_LIST
  ((NN_DIGIT *, unsigned int, NN_DIGIT *, unsigned int));

/* Find a probable prime a between 3*2^(b-2) and 2^b-1, starting at
   3*2^(b-2) + (c mod 2^(b-2)), such that gcd (a-1, d) = 1.

   Lengths: a[cDigits], c[cDigits], d[dDigits].
   Assumes b > 2, b < cDigits * NN_DIGIT_BITS, d is odd,
           cDigits < MAX_NN_DIGITS, dDigits < MAX_NN_DIGITS, and a
           probable prime can be found.
 */
void FindRSAPrime (a, b, c, cDigits, d, dDigits)
NN_DIGIT *a, *c, *d;
unsigned int b, cDigits, dDigits;
{
  NN_DIGIT t[MAX_NN_DIGITS], u[MAX_NN_DIGITS], v[MAX_NN_DIGITS],
    w[MAX_NN_DIGITS];
  
  /* Compute t = 2^(b-2), u = 3*2^(b-2).
   */
  NN_Assign2Exp (t, b-2, cDigits);
  NN_Assign2Exp (u, b-1, cDigits);
  NN_Add (u, u, t, cDigits);
  
  /* Compute v = 3*2^(b-2) + (c mod 2^(b-2)); add one if even.
   */
  NN_Mod (v, c, cDigits, t, cDigits);
  NN_Add (v, v, u, cDigits);
  if (NN_EVEN (v, cDigits)) {
    NN_ASSIGN_DIGIT (w, 1, cDigits);
    NN_Add (v, v, w, cDigits);
  }
  
  /* Compute w = 2, u = 2^b - 2.
   */
  NN_ASSIGN_DIGIT (w, 2, cDigits);
  NN_Sub (u, u, w, cDigits);
  NN_Add (u, u, t, cDigits);

  /* Search to 2^b-1 from starting point, then from 3*2^(b-2)+1.
   */
  while (! RSAPrime (v, cDigits, d, dDigits)) {
    if (NN_Cmp (v, u, cDigits) > 0)
      NN_Sub (v, v, t, cDigits);
    NN_Add (v, v, w, cDigits);
  }
  
  NN_Assign (a, v, cDigits);
  
  /* Zeroize sensitive information.
   */
  R_memset ((POINTER)v, 0, sizeof (v));
}

/* Returns nonzero iff a is a probable prime and GCD (a-1, b) = 1.

   Lengths: a[aDigits], b[bDigits].
   Assumes aDigits < MAX_NN_DIGITS, bDigits < MAX_NN_DIGITS.
 */
static int RSAPrime (a, aDigits, b, bDigits)
NN_DIGIT *a, *b;
unsigned int aDigits, bDigits;
{
  int status;
  NN_DIGIT aMinus1[MAX_NN_DIGITS], t[MAX_NN_DIGITS];
  
  NN_ASSIGN_DIGIT (t, 1, aDigits);
  NN_Sub (aMinus1, a, t, aDigits);
  
  status = ProbablePrime (a, aDigits) &&
    RelativelyPrime (aMinus1, aDigits, b, bDigits);

  /* Zeroize sensitive information.
   */
  R_memset ((POINTER)aMinus1, 0, sizeof (aMinus1));
  
  return (status);
}

/* Returns nonzero iff a is a probable prime.

   Lengths: a[aDigits].
   Assumes aDigits < MAX_NN_DIGITS.
 */
static int ProbablePrime (a, aDigits)
NN_DIGIT *a;
unsigned int aDigits;
{
  return (! SmallFactor (a, aDigits) && FermatTest (a, aDigits));
}

/* Returns nonzero iff a has a prime factor in SMALL_PRIMES.

   Lengths: a[aDigits].
   Assumes aDigits < MAX_NN_DIGITS.
 */
static int SmallFactor (a, aDigits)
NN_DIGIT *a;
unsigned int aDigits;
{
  int status;
  NN_DIGIT t[1];
  unsigned int i;
  
  status = 0;
  
  for (i = 0; i < SMALL_PRIME_COUNT; i++) {
    NN_ASSIGN_DIGIT (t, SMALL_PRIMES[i], 1);
    NN_Mod (t, a, aDigits, t, 1);
    if (NN_Zero (t, 1)) {
      status = 1;
      break;
    }
  }
  
  /* Zeroize sensitive information.
   */
  i = 0;
  R_memset ((POINTER)t, 0, sizeof (t));

  return (status);
}

/* Returns nonzero iff a passes Fermat's test for witness 2.
   (All primes pass the test, and nearly all composites fail.)
     
   Lengths: a[aDigits].
   Assumes aDigits < MAX_NN_DIGITS.
 */
static int FermatTest (a, aDigits)
NN_DIGIT *a;
unsigned int aDigits;
{
  int status;
  NN_DIGIT t[MAX_NN_DIGITS], u[MAX_NN_DIGITS];
  
  NN_ASSIGN_DIGIT (t, 2, aDigits);
  NN_ModExp (u, t, a, aDigits, a, aDigits);
  
  status = NN_EQUAL (t, u, aDigits);
  
  /* Zeroize sensitive information.
   */
  R_memset ((POINTER)u, 0, sizeof (u));
  
  return (status);
}

/* Returns nonzero iff a and b are relatively prime.

   Lengths: a[aDigits], b[bDigits].
   Assumes aDigits >= bDigits, aDigits < MAX_NN_DIGITS.
 */
static int RelativelyPrime (a, aDigits, b, bDigits)
NN_DIGIT *a, *b;
unsigned int aDigits, bDigits;
{
  int status;
  NN_DIGIT t[MAX_NN_DIGITS], u[MAX_NN_DIGITS];
  
  NN_AssignZero (t, aDigits);
  NN_Assign (t, b, bDigits);
  NN_Gcd (t, a, t, aDigits);
  NN_ASSIGN_DIGIT (u, 1, aDigits);

  status = NN_EQUAL (t, u, aDigits);
  
  /* Zeroize sensitive information.
   */
  R_memset ((POINTER)t, 0, sizeof (t));
  
  return (status);
}
