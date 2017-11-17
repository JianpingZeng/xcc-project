#include "tools_CPUInfoUtility.h"
#include <cstring>

#ifdef __cplusplus
    extern "C" {
#endif
/**
 * Execute the specified cpuid and return the 4 values in the
 * specified arguments.  If we can't run cpuid on the host, return true.
 */
static bool getCpuIDAndInfo(unsigned value, unsigned *rEAX, unsigned *rEBX,
                          unsigned *rECX, unsigned *rEDX)
{
#if defined(__x86_64__) || defined(_M_AMD64) || defined (_M_X64)
  #if defined(__GNUC__)
    // gcc doesn't know cpuid would clobber ebx/rbx. Preseve it manually.
    asm ("movq\t%%rbx, %%rsi\n\t"
         "cpuid\n\t"
         "xchgq\t%%rbx, %%rsi\n\t"
         : "=a" (*rEAX),
           "=S" (*rEBX),
           "=c" (*rECX),
           "=d" (*rEDX)
         :  "a" (value));
    return false;
  #elif defined(_MSC_VER)
    int registers[4];
    __cpuid(registers, value);
    *rEAX = registers[0];
    *rEBX = registers[1];
    *rECX = registers[2];
    *rEDX = registers[3];
    return false;
  #endif
#elif defined(i386) || defined(__i386__) || defined(__x86__) || defined(_M_IX86)
  #if defined(__GNUC__)
    asm ("movl\t%%ebx, %%esi\n\t"
         "cpuid\n\t"
         "xchgl\t%%ebx, %%esi\n\t"
         : "=a" (*rEAX),
           "=S" (*rEBX),
           "=c" (*rECX),
           "=d" (*rEDX)
         :  "a" (value));
    return false;
  #elif defined(_MSC_VER)
    __asm {
      mov   eax,value
      cpuid
      mov   esi,rEAX
      mov   dword ptr [esi],eax
      mov   esi,rEBX
      mov   dword ptr [esi],ebx
      mov   esi,rECX
      mov   dword ptr [esi],ecx
      mov   esi,rEDX
      mov   dword ptr [esi],edx
    }
    return false;
  #endif
#endif
  return true;
}

/**
 * Class:     tools_CPUInfoUtility
 * Method:    getCpuIDAndInfo
 * Signature: (I[I)Z
 */
JNIEXPORT jboolean JNICALL Java_tools_CPUInfoUtility_getCpuIDAndInfo
  (JNIEnv *env, jclass klass, jint value, jintArray regs)
{
    unsigned reg[4];    //eax, ebx, ecx, edx
    jboolean res = getCpuIDAndInfo(value, reg, reg+1, reg+2, reg+3);
    jboolean b = 0;
    jint *body = (jint*)env->GetPrimitiveArrayCritical(regs, &b);
    memcpy(body, reg, sizeof(reg));
    env->ReleasePrimitiveArrayCritical(regs, body, 0);
    return res;
}

#ifdef __cplusplus
    }
#endif