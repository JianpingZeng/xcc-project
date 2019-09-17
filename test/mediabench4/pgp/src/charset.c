/*
 * charset.c
 *
 * Conversion tables and routines to support different character sets.
 * The PGP internal format is latin-1.
 *
 * (c) Copyright 1990-1994 by Philip Zimmermann.  All rights reserved.
 * The author assumes no liability for damages resulting from the use
 * of this software, even if the damage results from defects in this
 * software.  No warranty is expressed or implied.
 *
 * Code that has been incorporated into PGP from other sources was
 * either originally published in the public domain or is used with
 * permission from the various authors.
 *
 * PGP is available for free to the public under certain restrictions.
 * See the PGP User's Guide (included in the release package) for
 * important information about licensing, patent restrictions on
 * certain algorithms, trademarks, copyrights, and export controls.
 */

#include <stdio.h>
#include <string.h>
#include "usuals.h"
#include "language.h"
#include "charset.h"
#include "system.h"

#ifndef NULL
#define	NULL	0
#endif

#define UNK	'?'

static unsigned char
intern2ascii[] = {  /* ISO 8859-1 Latin Alphabet 1 to US ASCII */
UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK,
UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK, UNK,
 32,  33,  99,  35,  36,  89, 124,  80,  34,  67,  97,  34, 126,  45,  82,  95,
111, UNK,  50,  51,  39, 117,  45,  45,  44,  49, 111,  34, UNK, UNK, UNK,  63,
 65,  65,  65,  65,  65,  65,  65,  67,  69,  69,  69,  69,  73,  73,  73,  73,
 68,  78,  79,  79,  79,  79,  79, 120,  79,  85,  85,  85,  85,  89,  84, 115,
 97,  97,  97,  97,  97,  97,  97,  99, 101, 101, 101, 101, 105, 105, 105, 105,
100, 110, 111, 111, 111, 111, 111,  47, 111, 117, 117, 117, 117, 121, 116, 121
};

static unsigned char
intern2cp850[] = { /* ISO 8859-1 Latin Alphabet 1
		      (Latin-1) to IBM Code Page 850 */
186, 205, 201, 187, 200, 188, 204, 185, 203, 202, 206, 223, 220, 219, 254, 242,
179, 196, 218, 191, 192, 217, 195, 180, 194, 193, 197, 176, 177, 178, 213, 159,
255, 173, 189, 156, 207, 190, 221, 245, 249, 184, 166, 174, 170, 240, 169, 238,
248, 241, 253, 252, 239, 230, 244, 250, 247, 251, 167, 175, 172, 171, 243, 168,
183, 181, 182, 199, 142, 143, 146, 128, 212, 144, 210, 211, 222, 214, 215, 216,
209, 165, 227, 224, 226, 229, 153, 158, 157, 235, 233, 234, 154, 237, 232, 225,
133, 160, 131, 198, 132, 134, 145, 135, 138, 130, 136, 137, 141, 161, 140, 139,
208, 164, 149, 162, 147, 228, 148, 246, 155, 151, 163, 150, 129, 236, 231, 152
};

static unsigned char
cp8502intern[] = {  /* IBM Code Page 850 to Latin-1 */
199, 252, 233, 226, 228, 224, 229, 231, 234, 235, 232, 239, 238, 236, 196, 197,
201, 230, 198, 244, 246, 242, 251, 249, 255, 214, 220, 248, 163, 216, 215, 159,
225, 237, 243, 250, 241, 209, 170, 186, 191, 174, 172, 189, 188, 161, 171, 187,
155, 156, 157, 144, 151, 193, 194, 192, 169, 135, 128, 131, 133, 162, 165, 147,
148, 153, 152, 150, 145, 154, 227, 195, 132, 130, 137, 136, 134, 129, 138, 164,
240, 208, 202, 203, 200, 158, 205, 206, 207, 149, 146, 141, 140, 166, 204, 139,
211, 223, 212, 210, 245, 213, 181, 254, 222, 218, 219, 217, 253, 221, 175, 180,
173, 177, 143, 190, 182, 167, 247, 184, 176, 168, 183, 185, 179, 178, 142, 160
};

/* Russian language specific conversation section */
/* Two point-to-point charset decode tables       */
/* produced by Andrew A. Chernov                  */
/* Decode single char from KOI8-R to ALT-CODES, if present */
static unsigned char intern2alt[] = {
	0xc4, 0xb3, 0xda, 0xbf, 0xc0, 0xd9, 0xc3, 0xb4,
	0xc2, 0xc1, 0xc5, 0xdf, 0xdc, 0xdb, 0xdd, 0xde,
	0xb0, 0xb1, 0xb2, 0xf4, 0xfe, 0xf9, 0xfb, 0xf7,
	0xf3, 0xf2, 0xff, 0xf5, 0xf8, 0xfd, 0xfa, 0xf6,
	0xcd, 0xba, 0xd5, 0xf1, 0xd6, 0xc9, 0xb8, 0xb7,
	0xbb, 0xd4, 0xd3, 0xc8, 0xbe, 0xbd, 0xbc, 0xc6,
	0xc7, 0xcc, 0xb5, 0xf0, 0xb6, 0xb9, 0xd1, 0xd2,
	0xcb, 0xcf, 0xd0, 0xca, 0xd8, 0xd7, 0xce, 0xfc,
	0xee, 0xa0, 0xa1, 0xe6, 0xa4, 0xa5, 0xe4, 0xa3,
	0xe5, 0xa8, 0xa9, 0xaa, 0xab, 0xac, 0xad, 0xae,
	0xaf, 0xef, 0xe0, 0xe1, 0xe2, 0xe3, 0xa6, 0xa2,
	0xec, 0xeb, 0xa7, 0xe8, 0xed, 0xe9, 0xe7, 0xea,
	0x9e, 0x80, 0x81, 0x96, 0x84, 0x85, 0x94, 0x83,
	0x95, 0x88, 0x89, 0x8a, 0x8b, 0x8c, 0x8d, 0x8e,
	0x8f, 0x9f, 0x90, 0x91, 0x92, 0x93, 0x86, 0x82,
	0x9c, 0x9b, 0x87, 0x98, 0x9d, 0x99, 0x97, 0x9a
};

/* Decode single char from ALT-CODES, if present, to KOI8-R */
static unsigned char alt2intern[] = {
	0xe1, 0xe2, 0xf7, 0xe7, 0xe4, 0xe5, 0xf6, 0xfa,
	0xe9, 0xea, 0xeb, 0xec, 0xed, 0xee, 0xef, 0xf0,
	0xf2, 0xf3, 0xf4, 0xf5, 0xe6, 0xe8, 0xe3, 0xfe,
	0xfb, 0xfd, 0xff, 0xf9, 0xf8, 0xfc, 0xe0, 0xf1,
	0xc1, 0xc2, 0xd7, 0xc7, 0xc4, 0xc5, 0xd6, 0xda,
	0xc9, 0xca, 0xcb, 0xcc, 0xcd, 0xce, 0xcf, 0xd0,
	0x90, 0x91, 0x92, 0x81, 0x87, 0xb2, 0xb4, 0xa7,
	0xa6, 0xb5, 0xa1, 0xa8, 0xae, 0xad, 0xac, 0x83,
	0x84, 0x89, 0x88, 0x86, 0x80, 0x8a, 0xaf, 0xb0,
	0xab, 0xa5, 0xbb, 0xb8, 0xb1, 0xa0, 0xbe, 0xb9,
	0xba, 0xb6, 0xb7, 0xaa, 0xa9, 0xa2, 0xa4, 0xbd,
	0xbc, 0x85, 0x82, 0x8d, 0x8c, 0x8e, 0x8f, 0x8b,
	0xd2, 0xd3, 0xd4, 0xd5, 0xc6, 0xc8, 0xc3, 0xde,
	0xdb, 0xdd, 0xdf, 0xd9, 0xd8, 0xdc, 0xc0, 0xd1,
	0xb3, 0xa3, 0x99, 0x98, 0x93, 0x9b, 0x9f, 0x97,
	0x9c, 0x95, 0x9e, 0x96, 0xbf, 0x9d, 0x94, 0x9a
};

/*
 * Most Unixes has KOI8, and DOS has ALT_CODE
 * If your Unix is non-standard, set CHARSET to "alt_codes"
 * in config.txt
 */

#ifndef	DEFAULT_CSET
#define	DEFAULT_CSET	"noconv"
#endif
#ifndef	DEFAULT_RU_CSET
#define	DEFAULT_RU_CSET	"noconv"
#endif

/* End of Russian section */

int CONVERSION = NO_CONV;      /* None text file conversion at start time */

unsigned char *ext_c_ptr;
static unsigned char *int_c_ptr;

#ifdef MSDOS
char charset[64] = "cp850";
#else
char charset[64] = "";
#endif

void
init_charset(void)
{
	ext_c_ptr = NULL;	/* NULL means latin1 or KOI8
				   (internal format) */
	int_c_ptr = NULL;

	if (charset[0] == '\0') {
		/* use default character set for this system */
		if (strcmp(language, "ru") == 0)
			strcpy(charset, DEFAULT_RU_CSET);
		else
			strcpy(charset, DEFAULT_CSET);
	} else {
		strlwr(charset);
	}

	/* latin-1 and KOI8 are in internal format: no conversion needed */
	if (!strcmp(charset, "latin1") || !strcmp(charset, "koi8") ||
		!strcmp(charset, "noconv"))
		return;

	if (!strcmp(charset, "alt_codes")) {
		ext_c_ptr = intern2alt;
		int_c_ptr = alt2intern;
	} else if (!strcmp(charset, "cp850")) {
		ext_c_ptr = intern2cp850;
		int_c_ptr = cp8502intern;
	} else if (!strcmp(charset, "ascii")) {
		ext_c_ptr = intern2ascii;
	} else {
		fprintf(stderr, LANG("Unsupported character set: '%s'\n"),
			charset);
	}
}

char
EXT_C(char c)
{
	if (c > '\0' || !ext_c_ptr)
		return c;
	return ext_c_ptr[c & 0x7f];
}

char
INT_C(char c)
{
	if (c > '\0' || !int_c_ptr)
		return c;
	return int_c_ptr[c & 0x7f];
}

/*
 * to_upper() and to_lower(), replacement for toupper() and tolower(),
 * calling to_upper() on uppercase or to_lower on lowercase characters
 * is handled correctly.
 * 
 * XXX: should handle local characterset when 8-bit userID's are allowed
 */
int
to_upper(int c)
{
	return (c >= 'a' && c <= 'z' ? c - ('a' - 'A') : c);
}

int
to_lower(int c)
{
	return (c >= 'A' && c <= 'Z' ? c + ('a' - 'A') : c);
}
