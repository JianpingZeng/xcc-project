// RUN: jlang-cc -fsyntax-only -verify -pedantic %s

#pragma jlang diagnostic pop // expected-warning{{pragma diagnostic pop could not pop, no matching push}}

#pragma jlang diagnostic puhs // expected-warning{{pragma diagnostic expected 'error', 'warning', 'ignored', 'fatal' 'push', or 'pop'}}

char a = 'df'; // expected-warning{{multi-character character constant}}

#pragma jlang diagnostic push
#pragma jlang diagnostic ignored "-Wmultichar"

char b = 'df'; // no warning.
#pragma jlang diagnostic pop

char c = 'df';  // expected-warning{{multi-character character constant}}

#pragma jlang diagnostic pop // expected-warning{{pragma diagnostic pop could not pop, no matching push}}
