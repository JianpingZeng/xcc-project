
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "logfmt.h"

// TODO: handle strtoll/strtod retvals

#define emit(T) \
  field.type = LOGFMT_##T; \
  cb(&field, data)

%%{
  machine parser;
  write data nofinal noerror;

  action mark {
    mark = fpc;
  }

  action name_start {
    has_value = false;
    field.name = fpc;
  }

  action name_end {
    field.name_len = fpc - field.name;
  }

  action decimal {
    field.value.as_int = strtoll(mark, &fpc, 10);
    emit(INT);
  }

  action octal {
    field.value.as_int = strtoll(mark, &fpc, 8);
    emit(INT);
  }

  action hex {
    field.value.as_int = strtoll(mark, &fpc, 16);
    emit(INT);
  }

  action float {
    field.value.as_float = strtod(mark, &fpc);
    emit(FLOAT);
  }

  action true {
    field.value.as_bool = true;
    emit(BOOL);
  }

  action false {
    field.value.as_bool = false;
    emit(BOOL);
  }

  action string {
    field.value.as_string = mark;
    field.string_len = fpc-mark;
    emit(STRING);
  }

  action has_value {
    has_value = 1;
  }

  action pair {
    if (!has_value) {
      field.value.as_bool = true;
      emit(BOOL);
    }
  }

  action eol {
    return fpc-buf;
  }

  s = [ \t]+;
  eol = '\r\n' | '\r' | '\n';
  id = (33..126 -- [="0-9]) (33..126 -- [="])*;
  string = '"' ([^"\\])* >mark %string '"';
  exponent = [eE] [+\-]? digit+;
  decimal = '0' | [1-9] digit*;
  octal = '0' digit+;
  float = digit+ '.' digit+ exponent? | digit+ exponent;
  hex = '0x' [0-9a-fA-F]+;
  bool = 'true' %true | 'false' %false;
  number = float >mark %float | octal >mark %octal | hex >mark %hex | decimal >mark %decimal;
  value = bool | number | string | id >mark %string;
  name = id >name_start %name_end;
  pair = name ('=' value %has_value)? %pair;
  main := (pair s | pair eol %eol)+;

}%%

int
logfmt_parse(char *buf, size_t len, logfmt_parse_callback_t cb, void *data) {
  char *pe = buf + len;
  char *mark = 0;
  char *eof = pe;
  char *p = buf;
  int has_value = 0;

  logfmt_field_t field = {0};

  (void) parser_en_main;

  int cs;

  %%{
    write init;
    write exec;
  }%%

  return 0;
}
