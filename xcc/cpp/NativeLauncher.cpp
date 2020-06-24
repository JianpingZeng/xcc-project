/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

#include <jni.h>
#include <string.h>
#include <libgen.h>
#include <string>
#include <iostream>
#include <limits.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include "version.h"
#include "NativeLauncher.h"

#if defined(__APPLE__)
#include <mach-o/dyld.h>
#endif

using namespace std;

/**
 * Creates a Java Virtual machine.
 * @param cmdPath The path to native launcher, like jlang-cc etc.
 */
JNIEnv* createVM(char* cmdPath)
{
#ifdef PATH_MAX
    char absolute[PATH_MAX] = {0};
#else
    char absolute[4086] = {0};
#endif
    if (!realpath(cmdPath, absolute)) {
        fprintf(stderr, "something get wrong on path '%s', %s", cmdPath, strerror(errno));
        exit(errno);
    }
#ifdef DEBUG
    fprintf(stderr, "%s\n", absolute);
#endif

    char* dir = dirname(absolute);
    char path[1024] = {0};
    sprintf(path, "-Djava.class.path=.:%s/lib/%s", dir, XCC_PACKAGE_NAME);
#ifdef DEBUG
    fprintf(stderr, "classpath: %s\n", path);
#endif

    JavaVM *jvm;                      // Pointer to the JVM (Java Virtual Machine)
    JNIEnv *env;                      // Pointer to native interface
    //================== prepare loading of Java VM ============================
    JavaVMInitArgs vm_args;                        // Initialization arguments
    JavaVMOption* options = new JavaVMOption[2];   // JVM invocation options
    options[0].optionString = path;   // where to find java .class
    vm_args.version = JNI_VERSION_1_8;             // minimum Java version
    vm_args.nOptions = 1;                          // number of options
    vm_args.options = options;
    vm_args.ignoreUnrecognized = false;     // invalid options make the JVM init fail
       //=============== load and initialize Java VM and JNI interface =============
    jint rc = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);  // YES !!
    delete[] options;    // we then no longer need the initialisation options.
    if (rc != JNI_OK) 
    {
         cin.get();
         exit(EXIT_FAILURE);
    }
    //=============== Display JVM version =======================================
#ifdef DEBUG
    jint ver = env->GetVersion();
    fprintf("JVM load succeeded: Version %d.%d\n", ver>>16)&0x0f, ver&0x0f);
#endif
    return env;
}

/**
 * Call the specified main method in the main class, like utils.tablegen.TableGen
 * with specified commands line arguments.
 */
void invokeClass(char* cmdPath, const char* mainClassName, int argc, char** argv)
{
    JNIEnv *env = createVM(cmdPath);

	jclass mainClass;
	mainClass = env->FindClass(mainClassName);
	if (!mainClass)
    {
	    cerr << "ERROR: class '" + string(mainClassName) + "' not found"<<endl;
	    exit(0);
    }
    else
    {
        jmethodID mainMethod;
    	jobjectArray applicationArgs;
        mainMethod = env->GetStaticMethodID(mainClass, "main", "([Ljava/lang/String;)V");
        if (!mainMethod)
        {
            cerr<<"ERROR: main method not found."<<endl;
            exit(0);
        }

        applicationArgs = env->NewObjectArray(argc + 1, env->FindClass("java/lang/String"), 0);

        // Set the first argument as 'launcher' to inform the CL.parseCommandLineOptions
        // we calling it by native launcher.
        env->SetObjectArrayElement(applicationArgs, 0, env->NewStringUTF("launcher"));
        for (int i = 0; i != argc; i++)
        {
            jstring applicationArg0 = env->NewStringUTF(argv[i]);
            env->SetObjectArrayElement(applicationArgs, i+1, applicationArg0);
        }
        env->CallStaticVoidMethod(mainClass, mainMethod, applicationArgs);
		// check if an exception occurred
		if (env->ExceptionOccurred())
    		env->ExceptionDescribe(); // print the stack trace
        env->DeleteLocalRef(applicationArgs);
	}
}

/**
 * Obtains the absolute path to the native launcher.
 */
std::string getpath()
{
  char buf[PATH_MAX + 1] = {0};
#if defined(__linux__) 
  if (readlink("/proc/self/exe", buf, sizeof(buf) - 1) == -1)
    throw std::string("readlink() failed");
#elif defined(__APPLE__)
  uint32_t sz = sizeof(buf) - 1;
 if ( _NSGetExecutablePath(buf, &sz) != 0)
   throw std::string("_NSGetExecuablePath failed!");
#else 
#error("Unsupported OS platform, please build on Linux/Darwin")
#endif
  std::string str(buf);
  return str.substr(0, str.rfind('/'));
}
