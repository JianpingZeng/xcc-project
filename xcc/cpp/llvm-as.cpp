/*
 * Extremely Compiler Collection
 * Copyright (c) 2015-2020, Jianping Zeng.
 * All rights reserved.
 * This software is subjected to the protection of BSD 3.0 Licence.
 * For more details, please refers to the LICENSE file.
 */

#include "NativeLauncher.h"

int main(int argc, char **argv)
{
    std::string cmdPath = getpath();
    invokeClass((char*)cmdPath.c_str(), "utils/as/AS", argc, argv);
    return 0;
}