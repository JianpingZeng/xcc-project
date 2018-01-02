int main()
{
    /*
     * Compiler-dependent macros that bracket portions of code where the
     * "-Wunreachable-code" warning should be ignored. Please use sparingly.
     */
    #if defined(__clang__)
    # define __unreachable_ok_push \
             _Pragma("jlang diagnostic push") \
             _Pragma("jlang diagnostic ignored \"-Wunreachable-code\"")
    # define __unreachable_ok_pop \
             _Pragma("jlang diagnostic pop")
    #elif defined(__GNUC__) && ((__GNUC__ > 4) || (__GNUC__ == 4 && __GNUC_MINOR__ >= 6))
    # define __unreachable_ok_push \
             _Pragma("GCC diagnostic push") \
             _Pragma("GCC diagnostic ignored \"-Wunreachable-code\"")
    # define __unreachable_ok_pop \
             _Pragma("GCC diagnostic pop")
    #else
    # define __unreachable_ok_push
    # define __unreachable_ok_pop
    #endif
    return 0;
}