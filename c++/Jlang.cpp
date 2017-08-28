/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous
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

using namespace std;

/**
 * Creates a Java Virtual machine.
 */
static JNIEnv* createVM(char* cmdPath)
{
    #ifdef PATH_MAX
        char absolute[PATH_MAX] = {0};
    #else
        char absolute[4086] = {0};
    #endif
    realpath(cmdPath, absolute);

    #ifdef NDEBUG
    cout<<absolute<<endl;
    #endif

    char* dir = dirname(absolute);
	size_t len = strlen(dir);
	char *end = dir + len - 1;
	while (*end != '/' && end >= dir)
	   	--end;

	string path(dir, end);
	path += "/lib";
	string cp = path + "/xcc-0.1.jar:"+ path + "/trove-3.0.3.jar";
	cp = "-Djava.class.path=" + cp;
	//cp = "-Djava.class.path=/home/xlous/Development/Compiler/xcc/out/lib/xcc-0.1.jar:"
	//                         "/home/xlous/Development/Compiler/xcc/out/lib/trove-3.0.3.jar";// + cp;
	#ifdef NDEBUG
    cout<<"classpath: "<<cp<<endl;
	#endif

	JavaVM *jvm;                      // Pointer to the JVM (Java Virtual Machine)
    JNIEnv *env;                      // Pointer to native interface
       //================== prepare loading of Java VM ============================
    JavaVMInitArgs vm_args;                        // Initialization arguments
    JavaVMOption* options = new JavaVMOption[1];   // JVM invocation options
    options[0].optionString = (char*)cp.c_str();   // where to find java .class
    //该参数可以用来观察C++调用JAVA的过程，设置该参数后，程序会在标准输出设备上打印调用的相关信息
    options[1].optionString = "-verbose:NONE";
    vm_args.version = JNI_VERSION_1_8;             // minimum Java version
    vm_args.nOptions = 2;                          // number of options
    vm_args.options = options;
    vm_args.ignoreUnrecognized = false;     // invalid options make the JVM init fail
       //=============== load and initialize Java VM and JNI interface =============
    jint rc = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);  // YES !!
    delete[] options;    // we then no longer need the initialisation options.
    if (rc != JNI_OK) {
          // TO DO: error processing...
         cin.get();
         exit(EXIT_FAILURE);
    }
    //=============== Display JVM version =======================================
    #ifdef NDEBUG
    cout << "JVM load succeeded: Version ";
    jint ver = env->GetVersion();
    cout << ((ver>>16)&0x0f) << "."<<(ver&0x0f) << endl;
    #endif
    // TO DO: add the code that will use JVM <============  (see next steps)
    return env;
}

/**
 * Call the specified main method in the utils.tablegen.TableGen with
 * specified commands line arguments.
 */
static void invokeClass(JNIEnv *env, int argc, char** argv)
{
	jclass tablegen;
	tablegen = env->FindClass("xcc/Jlang");
	if (tablegen == nullptr)
    {
	    cerr << "ERROR: class not found"<<endl;
	    exit(0);
    }
    else
    {
        jmethodID mainMethod;
    	jobjectArray applicationArgs;
        mainMethod = env->GetStaticMethodID(tablegen, "main", "([Ljava/lang/String;)V");
        if (mainMethod == nullptr)
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
            jstring applicationArg0;
            applicationArg0 = env->NewStringUTF(argv[i]);
            env->SetObjectArrayElement(applicationArgs, i+1, applicationArg0);
        }
        env->CallStaticVoidMethod(tablegen, mainMethod, applicationArgs);
        env->DeleteLocalRef(applicationArgs);
	}
}

int main(int argc, char **argv)
{
    JNIEnv* env = createVM(*argv);
    invokeClass(env, argc, argv);
    return 0;
}


