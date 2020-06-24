
#include <stdio.h>
#include <assert.h>
#include <string.h>
#include "src/rgba.h"

/*
 * Test named colors.
 */

void
test_named() {
  short ok;
  int32_t val = rgba_from_string("olive", &ok);
  assert(ok);
  assert(0x808000FF == val);
}

/*
 * Test rgb()
 */

void
test_rgb() {
  short ok;
  int32_t val = rgba_from_string("rgb(255, 30   , 0)", &ok);
  assert(ok);
  assert(0xff1e00ff == val);

  val = rgba_from_string("rgb(0,0,0)", &ok);
  assert(ok);
  assert(0x000000ff == val);
}

/*
 * Test rgba()
 */

void
test_rgba() {
  short ok;
  int32_t val = rgba_from_string("rgba(255, 30   , 0, .5)", &ok);
  assert(ok);
  assert(0xff1e007f == val);

  val = rgba_from_string("rgba(0,0,0, 1)", &ok);
  assert(ok);
  assert(0x000000ff == val);
}

/*
 * Test #rgb and #rrggbb
 */

void
test_hex() {
  short ok;
  int32_t val = rgba_from_string("#ff1e00", &ok);
  assert(ok);
  assert(0xff1e00ff == val);

  val = rgba_from_string("#ffffff", &ok);
  assert(ok);
  assert(0xffffffff == val);

  val = rgba_from_string("#ffcc00", &ok);
  assert(ok);
  assert(0xffcc00ff == val);

  val = rgba_from_string("#fco", &ok);
  assert(ok);
  assert(0xffcc00ff == val);
}

/*
 * Test to string.
 */

void
test_to_string() {
  char buf[256];
  rgba_t color = rgba_new(0xffcc00ff);
  rgba_to_string(color, buf, 256);
  assert(0 == strcmp("#ffcc00", buf));

  color = rgba_new(0xffcc0050);
  rgba_to_string(color, buf, 256);
  assert(0 == strcmp("rgba(255, 204, 0, 0.31)", buf));
}

/*
 * Run tests.
 */

int
main(int argc, char **argv){
  test_named();
  test_rgb();
  test_rgba();
  test_hex();
  test_to_string();
  printf("\n  \e[32m\u2713 \e[90mok\e[0m\n\n");
  return 0;
}