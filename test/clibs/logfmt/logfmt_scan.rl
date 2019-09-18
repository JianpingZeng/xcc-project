
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "logfmt.h"

#define emit(T) \
  cb(LOGFMT_TOK_##T, ts, te - ts, data)

%%{
  machine scanner;
  write data nofinal noerror;

  newline = '\r\n' | '\r' | '\n';
  id = (33..126 -- [="0-9]) (33..126 -- [="])*;
  string = '"' ([^"\\])* '"';
  exponent = [eE] [+\-]? digit+;
  decimal = '0' | [1-9] digit*;
  octal = '0' digit+;
  float = digit+ '.' digit+ exponent? | digit+ exponent;
  hex = '0x' [0-9a-fA-F]+;
  bool = 'true' | 'false';

  main := |*
    id       => { emit(LIT); };
    '='      => { emit(EQ); };
    bool     => { emit(BOOL); };
    float    => { emit(FLOAT); };
    octal    => { emit(OCTAL); };
    hex      => { emit(HEX); };
    decimal  => { emit(DECIMAL); };
    string   => { emit(LIT); };
    newline  => { emit(EOL); };
    [ \t]+;
  *|;

}%%

int
logfmt_scan(char *buf, size_t len, logfmt_scan_callback_t cb, void *data) {
  char *pe = buf + len;
  char *ts, *te;
  char *eof = 0;
  char *p = buf;
  int act = 0;

  (void) scanner_en_main;

  int cs;

  %%{
    write init;
    write exec;
  }%%

  return 0;
}
