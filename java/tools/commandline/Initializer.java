package tools.commandline;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2018, Jianping Zeng.
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
 * @author Jianping Zeng
 * @version 0.1
 */
public class Initializer<T> implements Modifier
{
    private T init;
    public Initializer(T initVal)
    {
        init = initVal;
    }
    @Override
    public void apply(Option<?> opt)
    {
        Option<T> optT = (Option<T>)opt;
        optT.setInitializer(init);
    }

    /**
     * This is a static factory method served as creating an instance of {@linkplain
     * Initializer} to specify the initial value of OptionInfo.
     * @param val
     * @param <T>
     * @return
     */
    public static <T> Initializer<T> init(T val)
    {
        return new Initializer<T>(val);
    }
}
