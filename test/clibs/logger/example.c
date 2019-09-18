
#include "logger.h"

int
main(void) {

  char repo[] = "user/repo.c";

  logger_info("install"
    , repo);

  logger_warn("warning"
    , "something is slightly wrong about %s"
    , repo);

  logger_error("error"
    , "%s is way broken"
    , repo);

  return 0;
}
