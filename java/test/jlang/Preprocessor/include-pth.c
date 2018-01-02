// RUN: jlang-cc -emit-pth %s -o %t &&
// RUN: jlang-cc -include-pth %t %s -E | grep 'file_to_include' | count 2
#include "file_to_include.h"
