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

using namespace std;

/**
 * Creates a Java Virtual machine.
 */
static JNIEnv* createVM(char* cmdPath)
{
    JavaVM *jvm = 0;
	JNIEnv *env = 0;
	JavaVMInitArgs args;
	JavaVMOption options[1];

	// 
	args.version = JNI_VERSION_1_8;
	args.nOptions = 1;
 	char* dir = dirname(cmdPath);
	size_t len = strlen(dir);
	char *end = dir + len - 1;
	while (*end != '/' && end >= dir)
	   	--end;

	string path(dir, end);
	path += "/lib";
	string cp = path + "/trove-3.0.3-jar:" + path + "/xcc-0.1.jar:.";
	cp = "-Djava.class.path=" + cp;
	options[0].optionString = (char *)cp.c_str();
	args.options = options;
	args.ignoreUnrecognized = JNI_FALSE;

	JNI_CreateJavaVM(&jvm, (void**)&env, &args);
	return env;
}

/**
 * Call the specified main method in the utils.tablegen.TableGen with
 * specified commands line arguments.
 */
static void invokeClass(JNIEnv *env, int argc, char** argv)
{
	jclass tablegen;
	jmethodID mainMethod;
	jobjectArray applicationArgs;
	tablegen = env->FindClass("utils/tablegen/TableGen");
	mainMethod = env->GetStaticMethodID(tablegen, "main", "([Ljava/lang/String;)V");

    applicationArgs = env->NewObjectArray(argc, env->FindClass("java/lang/String"), 0);

	for (int i = 0; i != argc; i++)
	{
		jstring applicationArg0;
		applicationArg0 = env->NewStringUTF(argv[i]);
		env->SetObjectArrayElement(applicationArgs, i, applicationArg0);
	}	
	env->CallStaticVoidMethod(tablegen, mainMethod, applicationArgs);
}


int main(int argc, char **argv)
{
	JNIEnv *env = createVM(argv[0]);
	invokeClass(env, argc-1, argv+1);
	return 0;
}


