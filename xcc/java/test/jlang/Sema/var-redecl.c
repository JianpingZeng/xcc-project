// RUN: jlang-cc -fsyntax-only -verify %s

int outer1; // expected-note{{previous definition is here}}
extern int outer2; // expected-note{{previous definition is here}}
int outer4;
int outer4; // expected-note{{previous definition is here}}
int outer5;
int outer6(float); // expected-note{{previous definition is here}}
int outer7(float);

void outer_test() {
  extern float outer1; // expected-error{{redefinition of 'outer1' with a different type}}
  extern float outer2; // expected-error{{redefinition of 'outer2' with a different type}}
  extern float outer3; // expected-note{{previous definition is here}}
  double outer4;
  extern int outer5; // expected-note{{previous definition is here}}
  extern int outer6; // expected-error{{redefinition of 'outer6' as different kind of symbol}}
  int outer7;
  extern int outer8; // expected-note{{previous definition is here}}
  extern int outer9;
  {
    extern int outer9; // expected-note{{previous definition is here}}
  }
}

int outer3; // expected-error{{redefinition of 'outer3' with a different type}}
float outer4; // expected-error{{redefinition of 'outer4' with a different type}}
float outer5;  // expected-error{{redefinition of 'outer5' with a different type}}
int outer8(int); // expected-error{{redefinition of 'outer8' as different kind of symbol}}
float outer9; // expected-error{{redefinition of 'outer9' with a different type}}

extern int outer13; // expected-note{{previous definition is here}}
void outer_shadowing_test() {
  extern int outer10;
  extern int outer11; // expected-note{{previous definition is here}}
  extern int outer12; // expected-note{{previous definition is here}}
  {
    float outer10;
    float outer11;
    float outer12;
    {
      extern int outer10; // okay
      extern float outer11; // expected-error{{redefinition of 'outer11' with a different type}}
      static double outer12;
      {
        extern float outer12; // expected-error{{redefinition of 'outer12' with a different type}}
        extern float outer13; // expected-error{{redefinition of 'outer13' with a different type}}
      }
    }
  }
}

void g18(void) {
  extern int g19;
}
int *p=&g19; // expected-error{{use of undeclared identifier 'g19'}}

// PR3645
static int a;
extern int a;
int a;
