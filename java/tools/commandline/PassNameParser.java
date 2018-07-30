/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tools.commandline;

import backend.pass.PassInfo;
import backend.passManaging.PassRegistrationListener;

import java.util.Comparator;

/**
 * @author Jianping Zeng
 * @version 0.1
 */
public class PassNameParser extends Parser<PassInfo> implements PassRegistrationListener
{
    public PassNameParser()
    {
        registerListener();
    }

    @Override
    public <PassInfo> void initialize(Option<PassInfo> opt)
    {
        super.initialize(opt);
        // Add all of the passes to the map that got initialized before 'this' did.
        enumeratePasses();
    }

    public boolean ignorablePass(PassInfo pi)
    {
        return pi.getPassArgument() == null || pi.getPassArgument().isEmpty() || pi.getKlass() == null;
    }

    @Override
    public void passRegistered(PassInfo pi)
    {
        if (ignorablePass(pi))
            return;
        int idx = findOption(pi.getPassArgument());
        if (idx != -1)
        {
            System.err.printf("No pass '%s' found\n", pi.getPassArgument());
            System.exit(-1);
        }
        addLiteralOption(pi.getPassArgument(), pi, pi.getPassName());
    }

    @Override
    public void passEnumerate(PassInfo pi)
    {
        passRegistered(pi);
    }

    @Override
    public void printOptionInfo(Option<?> opt, int globalWidth)
    {
        values.sort(Comparator.comparing(o -> o.first));
        super.printOptionInfo(opt, globalWidth);
    }
}
