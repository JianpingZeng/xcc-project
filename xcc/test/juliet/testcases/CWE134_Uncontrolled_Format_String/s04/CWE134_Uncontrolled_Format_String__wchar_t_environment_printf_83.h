/* TEMPLATE GENERATED TESTCASE FILE
Filename: CWE134_Uncontrolled_Format_String__wchar_t_environment_printf_83.h
Label Definition File: CWE134_Uncontrolled_Format_String.label.xml
Template File: sources-sinks-83.tmpl.h
*/
/*
 * @description
 * CWE: 134 Uncontrolled Format String
 * BadSource: environment Read input from an environment variable
 * GoodSource: Copy a fixed string into data
 * Sinks: printf
 *    GoodSink: wprintf with "%s" as the first argument and data as the second
 *    BadSink : wprintf with only data as an argument
 * Flow Variant: 83 Data flow: data passed to class constructor and destructor by declaring the class object on the stack
 *
 * */

#include "std_testcase.h"

#ifndef _WIN32
#include <wchar.h>
#endif

namespace CWE134_Uncontrolled_Format_String__wchar_t_environment_printf_83
{

#ifndef OMITBAD

class CWE134_Uncontrolled_Format_String__wchar_t_environment_printf_83_bad
{
public:
    CWE134_Uncontrolled_Format_String__wchar_t_environment_printf_83_bad(wchar_t * dataCopy);
    ~CWE134_Uncontrolled_Format_String__wchar_t_environment_printf_83_bad();

private:
    wchar_t * data;
};

#endif /* OMITBAD */

#ifndef OMITGOOD

class CWE134_Uncontrolled_Format_String__wchar_t_environment_printf_83_goodG2B
{
public:
    CWE134_Uncontrolled_Format_String__wchar_t_environment_printf_83_goodG2B(wchar_t * dataCopy);
    ~CWE134_Uncontrolled_Format_String__wchar_t_environment_printf_83_goodG2B();

private:
    wchar_t * data;
};

class CWE134_Uncontrolled_Format_String__wchar_t_environment_printf_83_goodB2G
{
public:
    CWE134_Uncontrolled_Format_String__wchar_t_environment_printf_83_goodB2G(wchar_t * dataCopy);
    ~CWE134_Uncontrolled_Format_String__wchar_t_environment_printf_83_goodB2G();

private:
    wchar_t * data;
};

#endif /* OMITGOOD */

}
