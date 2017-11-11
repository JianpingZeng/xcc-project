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

import java.util.HashMap;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class RegisterPass
{
    private PassInfo passInfo;
    private Class klass;
    private static HashMap<PassInfo, Class<Pass>> passInfoMap;

    /**
     * Creates an instance of PassInfo with default constructor.
     * And register it into {@linkplain #passInfoMap}.
     * @param name
     * @param klass
     */
    public RegisterPass(String name,  String passArg, Class klass)
    {
        this(passArg, name, klass, false);
    }

    public RegisterPass( String name, String passArg, Class klass,
            boolean cfgOnly)
    {
        this(passArg, name, klass, cfgOnly, false);
    }

    /**
     * Creates an instance of PassInfo with the specified arguments list.
     * @param name
     * @param passArg The argument to be printed out into command line.
     * @param klass
     */
    public RegisterPass(String name,  String passArg, Class klass,
            boolean cfgOnly, boolean isAnalysis)
    {
        passInfo = new PassInfo(name, passArg, klass, cfgOnly, isAnalysis);
        this.klass = klass;
        registerPass();
    }

    public void registerPass()
    {
        if (passInfoMap == null)
            passInfoMap = new HashMap<>();

        assert !passInfoMap.containsKey(passInfo) :"Pass already registered!";
        passInfoMap.put(passInfo, klass);
    }

    protected void unregisterPass()
    {
        assert passInfoMap != null : "Pass register factory is uninstantiated as yet!";
        assert passInfoMap.containsKey(passInfo) :"Pass registered but not in register factory!";
        passInfoMap.remove(passInfo);
        if (passInfoMap.isEmpty())
            passInfoMap = null;
    }

    public PassInfo getPassInfo() {return passInfo;}
}
