
#include <string.h>
#include <sys/uio.h>
#include "describe/describe.h"
#include "rle.h"

// TODO: varint
// TODO: grow buffer or add fn for decoded length

int
main(){
  describe("rle_encode()") {
    it("should compress runs") {
      struct iovec iov[] = {
        {
          .iov_base = "hello",
          .iov_len = 5
        },
        {
          .iov_base = "hello",
          .iov_len = 5
        },
        {
          .iov_base = "hello",
          .iov_len = 5
        },
        {
          .iov_base = "foo",
          .iov_len = 3
        },
        {
          .iov_base = "foo",
          .iov_len = 3
        },
        {
          .iov_base = "bar",
          .iov_len = 3
        }
      };

      char buf[256];
      size_t len = rle_encode(iov, 6, buf);
      assert(len == 17);

      struct iovec out[6] = {{0}};
      rle_decode(buf, 17, out);

      assert(strncmp("hello", out[0].iov_base, out[0].iov_len) == 0);
      assert(strncmp("hello", out[1].iov_base, out[1].iov_len) == 0);
      assert(strncmp("hello", out[2].iov_base, out[2].iov_len) == 0);
      assert(strncmp("foo", out[3].iov_base, out[3].iov_len) == 0);
      assert(strncmp("foo", out[4].iov_base, out[4].iov_len) == 0);
      assert(strncmp("bar", out[5].iov_base, out[5].iov_len) == 0);
    }
  }

  return assert_failures();
}
