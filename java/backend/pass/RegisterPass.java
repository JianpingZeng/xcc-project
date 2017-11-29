package backend.pass;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class RegisterPass
{
    private PassInfo passInfo;
    private Class klass;

    /**
     * Creates an instance of PassInfo with default constructor.
     * And register it into {@linkplain PassRegistrar#registeredPasses}.
     * @param name
     * @param klass
     */
    public RegisterPass(String name,  String passArg, Class<? extends Pass> klass)
    {
        this(name, passArg, klass, false);
    }

    public RegisterPass(String name, String passArg, Class<? extends Pass> klass,
            boolean cfgOnly)
    {
        this(name, passArg, klass, cfgOnly, false);
    }

    /**
     * Creates an instance of PassInfo with the specified arguments list.
     * @param name
     * @param passArg The argument to be printed out into command line.
     * @param klass
     */
    public RegisterPass(String name,
            String passArg,
            Class<? extends Pass> klass,
            boolean cfgOnly,
            boolean isAnalysis)
    {
        passInfo = new PassInfo(name, passArg, klass, cfgOnly, isAnalysis);
        this.klass = klass;
        PassRegistrar.registerPass(klass, passInfo);
    }

    public PassInfo getPassInfo() {return passInfo;}
}
