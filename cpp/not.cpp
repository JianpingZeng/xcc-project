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
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <string>
#include <iostream>

using namespace std;

static bool findProgramByName(string& progName)
{
    if (progName.find('/') != std::string::npos)
        return true;

    std::vector<string> paths;
    if (char* pathEvn = std::getenv("PATH"))
    {
        char* ptr = strtok(pathEvn, ":");
        while (ptr != NULL)
        {
            paths.push_back(ptr);
            ptr = strtok(NULL, ":");
        }
    }

    for (auto path : paths)
    {
        if (path.empty()) continue;

        string filePath = path + progName;
        if (access(filePath.c_str(), F_OK) != -1 &&
                access(filePath.c_str(), X_OK) != -1)
        {
            progName = filePath;
            return true;
        }
    }
    return false;
}

static int executeAndWait(char* progName, string &msg)
{
    int child = fork();
    int res;
    switch (child)
    {
        case -1:
            msg = "Couldn't fork";
            return -1;
        case 0:
            // child process will return 0 from fork()
            res = system(progName);
            res = res;  // avoiding warning on unused result
            return errno == ENOENT ? 127 : 126;
        default:
            break;
    }
    return 0;
}

int main(int argc, char* argv[])
{
    --argc;
    ++argv;

    bool expectedCrash = false;
    if (argc > 0 && !strcmp(argv[0], "--crash"))
    {
        expectedCrash = true;
        --argc;
        ++argv;
    }

    if (argc <= 0)
        return 1;

    string progName(argv[0]);
    if (!findProgramByName(progName)
    {
        cerr<<"Error: Unable to find '" << progName <<"' in PATH\n";
        return 1;
    }

    string msg;
    int result = executeAndWait(progName.c_str(), msg);
    if (result < 0)
    {
        if (expectedCrash)
        {
            cerr<<msg<<"\n";
            return 0;
        }
        return 1;
    }

    if (expectedCrash)
        return 1;

    return result == 0;
}