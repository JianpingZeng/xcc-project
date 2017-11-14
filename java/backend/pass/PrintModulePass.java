package backend.pass;

/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous zeng.
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

import backend.support.FormattedOutputStream;
import backend.value.Module;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class PrintModulePass implements ModulePass
{
	private FormattedOutputStream os;

	static
	{
		new RegisterPass("print-module", "Print out Module", PrintModulePass.class);
	}

	public PrintModulePass(OutputStream out)
	{
		super();
		os = new FormattedOutputStream(out);
	}

	@Override
	public boolean runOnModule(Module m)
	{
        try
        {
            m.print(os);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
	}

	@Override
    public String getPassName()
	{
		return "Print module into text file";
	}
}
