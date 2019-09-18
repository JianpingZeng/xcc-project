
#line 1 "logfmt.rl"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "logfmt.h"

#define emit(T) \
  cb(T, ts, te - ts, data)


#line 15 "logfmt.c"
static const int logfmt_start = 2;

static const int logfmt_en_main = 2;


#line 38 "logfmt.rl"


int
logfmt_scan(char *buf, size_t len, logfmt_scan_callback_t cb, void *data) {
  char *pe = buf + len;
  char *ts, *te;
  char *eof = 0;
  char *p = buf;
  int act = 0;

  (void) logfmt_en_main;

  int cs;

  
#line 37 "logfmt.c"
	{
	cs = logfmt_start;
	ts = 0;
	te = 0;
	act = 0;
	}

#line 45 "logfmt.c"
	{
	if ( p == pe )
		goto _test_eof;
	switch ( cs )
	{
tr1:
#line 33 "logfmt.rl"
	{te = p+1;{ emit(LOGFMT_TOK_LIT); }}
	goto st2;
tr4:
#line 34 "logfmt.rl"
	{te = p+1;{ emit(LOGFMT_TOK_EOL); }}
	goto st2;
tr10:
#line 32 "logfmt.rl"
	{te = p+1;{ emit(LOGFMT_TOK_EQ); }}
	goto st2;
tr13:
#line 35 "logfmt.rl"
	{te = p;p--;}
	goto st2;
tr14:
#line 34 "logfmt.rl"
	{te = p;p--;{ emit(LOGFMT_TOK_EOL); }}
	goto st2;
tr15:
#line 1 "NONE"
	{	switch( act ) {
	case 1:
	{{p = ((te))-1;} emit(LOGFMT_TOK_BOOL); }
	break;
	case 3:
	{{p = ((te))-1;} emit(LOGFMT_TOK_FLOAT); }
	break;
	case 5:
	{{p = ((te))-1;} emit(LOGFMT_TOK_HEX); }
	break;
	case 6:
	{{p = ((te))-1;} emit(LOGFMT_TOK_LIT); }
	break;
	}
	}
	goto st2;
tr16:
#line 31 "logfmt.rl"
	{te = p;p--;{ emit(LOGFMT_TOK_LIT); }}
	goto st2;
tr18:
#line 28 "logfmt.rl"
	{te = p;p--;{ emit(LOGFMT_TOK_FLOAT); }}
	goto st2;
tr22:
#line 27 "logfmt.rl"
	{te = p;p--;{ emit(LOGFMT_TOK_DECIMAL); }}
	goto st2;
tr25:
#line 29 "logfmt.rl"
	{te = p;p--;{ emit(LOGFMT_TOK_OCTAL); }}
	goto st2;
st2:
#line 1 "NONE"
	{ts = 0;}
	if ( ++p == pe )
		goto _test_eof2;
case 2:
#line 1 "NONE"
	{ts = p;}
#line 113 "logfmt.c"
	switch( (*p) ) {
		case 9: goto st3;
		case 10: goto tr4;
		case 13: goto st4;
		case 32: goto st3;
		case 34: goto st1;
		case 46: goto st6;
		case 48: goto st10;
		case 61: goto tr10;
		case 102: goto st14;
		case 116: goto st18;
	}
	if ( (*p) < 49 ) {
		if ( 33 <= (*p) && (*p) <= 47 )
			goto tr6;
	} else if ( (*p) > 57 ) {
		if ( 58 <= (*p) && (*p) <= 126 )
			goto tr6;
	} else
		goto st13;
	goto st0;
st0:
cs = 0;
	goto _out;
st3:
	if ( ++p == pe )
		goto _test_eof3;
case 3:
	switch( (*p) ) {
		case 9: goto st3;
		case 32: goto st3;
	}
	goto tr13;
st4:
	if ( ++p == pe )
		goto _test_eof4;
case 4:
	if ( (*p) == 10 )
		goto tr4;
	goto tr14;
tr6:
#line 1 "NONE"
	{te = p+1;}
#line 31 "logfmt.rl"
	{act = 6;}
	goto st5;
tr30:
#line 1 "NONE"
	{te = p+1;}
#line 26 "logfmt.rl"
	{act = 1;}
	goto st5;
st5:
	if ( ++p == pe )
		goto _test_eof5;
case 5:
#line 170 "logfmt.c"
	if ( (*p) == 33 )
		goto tr6;
	if ( (*p) > 60 ) {
		if ( 62 <= (*p) && (*p) <= 126 )
			goto tr6;
	} else if ( (*p) >= 35 )
		goto tr6;
	goto tr15;
st1:
	if ( ++p == pe )
		goto _test_eof1;
case 1:
	switch( (*p) ) {
		case 34: goto tr1;
		case 92: goto st0;
	}
	goto st1;
st6:
	if ( ++p == pe )
		goto _test_eof6;
case 6:
	if ( (*p) == 33 )
		goto tr6;
	if ( (*p) < 48 ) {
		if ( 35 <= (*p) && (*p) <= 47 )
			goto tr6;
	} else if ( (*p) > 57 ) {
		if ( (*p) > 60 ) {
			if ( 62 <= (*p) && (*p) <= 126 )
				goto tr6;
		} else if ( (*p) >= 58 )
			goto tr6;
	} else
		goto st7;
	goto tr16;
st7:
	if ( ++p == pe )
		goto _test_eof7;
case 7:
	switch( (*p) ) {
		case 33: goto tr6;
		case 69: goto st8;
		case 101: goto st8;
	}
	if ( (*p) < 48 ) {
		if ( 35 <= (*p) && (*p) <= 47 )
			goto tr6;
	} else if ( (*p) > 57 ) {
		if ( (*p) > 60 ) {
			if ( 62 <= (*p) && (*p) <= 126 )
				goto tr6;
		} else if ( (*p) >= 58 )
			goto tr6;
	} else
		goto st7;
	goto tr18;
st8:
	if ( ++p == pe )
		goto _test_eof8;
case 8:
	switch( (*p) ) {
		case 33: goto tr6;
		case 43: goto tr20;
		case 45: goto tr20;
	}
	if ( (*p) < 48 ) {
		if ( 35 <= (*p) && (*p) <= 47 )
			goto tr6;
	} else if ( (*p) > 57 ) {
		if ( (*p) > 60 ) {
			if ( 62 <= (*p) && (*p) <= 126 )
				goto tr6;
		} else if ( (*p) >= 58 )
			goto tr6;
	} else
		goto tr21;
	goto tr16;
tr20:
#line 1 "NONE"
	{te = p+1;}
#line 31 "logfmt.rl"
	{act = 6;}
	goto st9;
tr21:
#line 1 "NONE"
	{te = p+1;}
#line 28 "logfmt.rl"
	{act = 3;}
	goto st9;
st9:
	if ( ++p == pe )
		goto _test_eof9;
case 9:
#line 264 "logfmt.c"
	if ( (*p) == 33 )
		goto tr6;
	if ( (*p) < 48 ) {
		if ( 35 <= (*p) && (*p) <= 47 )
			goto tr6;
	} else if ( (*p) > 57 ) {
		if ( (*p) > 60 ) {
			if ( 62 <= (*p) && (*p) <= 126 )
				goto tr6;
		} else if ( (*p) >= 58 )
			goto tr6;
	} else
		goto tr21;
	goto tr15;
st10:
	if ( ++p == pe )
		goto _test_eof10;
case 10:
	switch( (*p) ) {
		case 33: goto tr6;
		case 46: goto st7;
		case 69: goto st8;
		case 101: goto st8;
		case 120: goto tr24;
	}
	if ( (*p) < 48 ) {
		if ( 35 <= (*p) && (*p) <= 47 )
			goto tr6;
	} else if ( (*p) > 57 ) {
		if ( (*p) > 60 ) {
			if ( 62 <= (*p) && (*p) <= 126 )
				goto tr6;
		} else if ( (*p) >= 58 )
			goto tr6;
	} else
		goto st11;
	goto tr22;
st11:
	if ( ++p == pe )
		goto _test_eof11;
case 11:
	switch( (*p) ) {
		case 33: goto tr6;
		case 46: goto st7;
		case 69: goto st8;
		case 101: goto st8;
	}
	if ( (*p) < 48 ) {
		if ( 35 <= (*p) && (*p) <= 47 )
			goto tr6;
	} else if ( (*p) > 57 ) {
		if ( (*p) > 60 ) {
			if ( 62 <= (*p) && (*p) <= 126 )
				goto tr6;
		} else if ( (*p) >= 58 )
			goto tr6;
	} else
		goto st11;
	goto tr25;
tr24:
#line 1 "NONE"
	{te = p+1;}
#line 31 "logfmt.rl"
	{act = 6;}
	goto st12;
tr26:
#line 1 "NONE"
	{te = p+1;}
#line 30 "logfmt.rl"
	{act = 5;}
	goto st12;
st12:
	if ( ++p == pe )
		goto _test_eof12;
case 12:
#line 340 "logfmt.c"
	if ( (*p) == 33 )
		goto tr6;
	if ( (*p) < 62 ) {
		if ( (*p) < 48 ) {
			if ( 35 <= (*p) && (*p) <= 47 )
				goto tr6;
		} else if ( (*p) > 57 ) {
			if ( 58 <= (*p) && (*p) <= 60 )
				goto tr6;
		} else
			goto tr26;
	} else if ( (*p) > 64 ) {
		if ( (*p) < 71 ) {
			if ( 65 <= (*p) && (*p) <= 70 )
				goto tr26;
		} else if ( (*p) > 96 ) {
			if ( (*p) > 102 ) {
				if ( 103 <= (*p) && (*p) <= 126 )
					goto tr6;
			} else if ( (*p) >= 97 )
				goto tr26;
		} else
			goto tr6;
	} else
		goto tr6;
	goto tr15;
st13:
	if ( ++p == pe )
		goto _test_eof13;
case 13:
	switch( (*p) ) {
		case 33: goto tr6;
		case 46: goto st7;
		case 69: goto st8;
		case 101: goto st8;
	}
	if ( (*p) < 48 ) {
		if ( 35 <= (*p) && (*p) <= 47 )
			goto tr6;
	} else if ( (*p) > 57 ) {
		if ( (*p) > 60 ) {
			if ( 62 <= (*p) && (*p) <= 126 )
				goto tr6;
		} else if ( (*p) >= 58 )
			goto tr6;
	} else
		goto st13;
	goto tr22;
st14:
	if ( ++p == pe )
		goto _test_eof14;
case 14:
	switch( (*p) ) {
		case 33: goto tr6;
		case 97: goto st15;
	}
	if ( (*p) > 60 ) {
		if ( 62 <= (*p) && (*p) <= 126 )
			goto tr6;
	} else if ( (*p) >= 35 )
		goto tr6;
	goto tr16;
st15:
	if ( ++p == pe )
		goto _test_eof15;
case 15:
	switch( (*p) ) {
		case 33: goto tr6;
		case 108: goto st16;
	}
	if ( (*p) > 60 ) {
		if ( 62 <= (*p) && (*p) <= 126 )
			goto tr6;
	} else if ( (*p) >= 35 )
		goto tr6;
	goto tr16;
st16:
	if ( ++p == pe )
		goto _test_eof16;
case 16:
	switch( (*p) ) {
		case 33: goto tr6;
		case 115: goto st17;
	}
	if ( (*p) > 60 ) {
		if ( 62 <= (*p) && (*p) <= 126 )
			goto tr6;
	} else if ( (*p) >= 35 )
		goto tr6;
	goto tr16;
st17:
	if ( ++p == pe )
		goto _test_eof17;
case 17:
	switch( (*p) ) {
		case 33: goto tr6;
		case 101: goto tr30;
	}
	if ( (*p) > 60 ) {
		if ( 62 <= (*p) && (*p) <= 126 )
			goto tr6;
	} else if ( (*p) >= 35 )
		goto tr6;
	goto tr16;
st18:
	if ( ++p == pe )
		goto _test_eof18;
case 18:
	switch( (*p) ) {
		case 33: goto tr6;
		case 114: goto st19;
	}
	if ( (*p) > 60 ) {
		if ( 62 <= (*p) && (*p) <= 126 )
			goto tr6;
	} else if ( (*p) >= 35 )
		goto tr6;
	goto tr16;
st19:
	if ( ++p == pe )
		goto _test_eof19;
case 19:
	switch( (*p) ) {
		case 33: goto tr6;
		case 117: goto st17;
	}
	if ( (*p) > 60 ) {
		if ( 62 <= (*p) && (*p) <= 126 )
			goto tr6;
	} else if ( (*p) >= 35 )
		goto tr6;
	goto tr16;
	}
	_test_eof2: cs = 2; goto _test_eof; 
	_test_eof3: cs = 3; goto _test_eof; 
	_test_eof4: cs = 4; goto _test_eof; 
	_test_eof5: cs = 5; goto _test_eof; 
	_test_eof1: cs = 1; goto _test_eof; 
	_test_eof6: cs = 6; goto _test_eof; 
	_test_eof7: cs = 7; goto _test_eof; 
	_test_eof8: cs = 8; goto _test_eof; 
	_test_eof9: cs = 9; goto _test_eof; 
	_test_eof10: cs = 10; goto _test_eof; 
	_test_eof11: cs = 11; goto _test_eof; 
	_test_eof12: cs = 12; goto _test_eof; 
	_test_eof13: cs = 13; goto _test_eof; 
	_test_eof14: cs = 14; goto _test_eof; 
	_test_eof15: cs = 15; goto _test_eof; 
	_test_eof16: cs = 16; goto _test_eof; 
	_test_eof17: cs = 17; goto _test_eof; 
	_test_eof18: cs = 18; goto _test_eof; 
	_test_eof19: cs = 19; goto _test_eof; 

	_test_eof: {}
	if ( p == eof )
	{
	switch ( cs ) {
	case 3: goto tr13;
	case 4: goto tr14;
	case 5: goto tr15;
	case 6: goto tr16;
	case 7: goto tr18;
	case 8: goto tr16;
	case 9: goto tr15;
	case 10: goto tr22;
	case 11: goto tr25;
	case 12: goto tr15;
	case 13: goto tr22;
	case 14: goto tr16;
	case 15: goto tr16;
	case 16: goto tr16;
	case 17: goto tr16;
	case 18: goto tr16;
	case 19: goto tr16;
	}
	}

	_out: {}
	}

#line 55 "logfmt.rl"


  return 0;
}
