// Use of tagged type without tag. rdar://6783347
struct xyz { int y; };
xyz b;         // expected-error {{must use 'struct' tag to refer to type 'xyz'}}

float *test7() {
  // We should recover 'b' by parsing it with a valid type of "struct xyz", which
  // allows us to diagnose other bad things done with y, such as this.
  return &b.y;   // expected-warning {{incompatible pointer types returning 'int *', expected 'float *'}}
}


// PR6208
struct test10 { int a; } static test10x;
struct test11 { int a; } const test11x;

// rdar://7608537
struct test13 { int a; } (test13x);
