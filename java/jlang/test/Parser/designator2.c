// RUN: jlang-cc -fsyntax-only %s -verify -pedantic

struct foo {
  int arr[10];
};

struct foo Y[2] = {
  [0].arr[2] = 4,
  [1].arr[1] = 4,
};