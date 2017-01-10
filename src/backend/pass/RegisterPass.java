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
    private static HashMap<Class<Pass>, PassInfo> passInfoMap;

    /**
     * Creates an instance of PassInfo with default constructor.
     * And register it into {@linkplain #passInfoMap}.
     * @param name
     * @param klass
     */
    public RegisterPass(String name, Class klass)
    {
        this(name, null, klass);
    }

    /**
     * Creates an instance of PassInfo with the specified arguments list.
     * @param name
     * @param args
     * @param klass
     */
    public RegisterPass(String name, Object[] args, Class klass)
    {
        passInfo = new PassInfo(name, args, klass);
        this.klass = klass;
        registerPass();
    }

    public void registerPass()
    {
        if (passInfoMap == null)
            passInfoMap = new HashMap<>();

        assert passInfoMap.containsKey(passInfo) :"Pass already registered!";
        passInfoMap.put(klass, passInfo);
    }

    protected void unregisterPass()
    {
        assert passInfoMap != null : "Pass register factory is uninstantiated as yet!";
        assert passInfoMap.containsKey(klass) :"Pass registered but not in register factory!";
        passInfoMap.remove(passInfo);
        if (passInfoMap.isEmpty())
            passInfoMap = null;
    }

    public PassInfo getPassInfo() {return passInfo;}
}
