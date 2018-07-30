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

package backend.codegen.dagisel;

import tools.Util;
import static backend.codegen.dagisel.SDep.Kind.*;

public class SDep implements Cloneable
{
    public enum Kind
    {
        Data, Anti, Output, Order
    }

    private SUnit unit;
    private Kind depKind;
    private int reg;    // for data, anti,output dependency.

    private static class OrderContent
    {
        boolean isNormalMemory;
        boolean isMustAlias;
        boolean isArtificial;
    }

    private OrderContent order;

    private int latency;

    public SDep()
    {
        depKind = Data;
    }

    public SDep(SUnit u, Kind kind, int latency, int reg)
    {
        this(u, kind, latency, reg, false, false, false);
    }

	public SDep(SUnit u,
		Kind kind,
		int latency,
		int reg,
		boolean isNormalMemory,
		boolean isMustAlias, 
		boolean isArtificial)
	{
		this.depKind = kind;
		this.unit = u;
		this.latency = latency;
		switch(kind)
		{
			case Anti:
			case Output:
				Util.assertion( reg != 0);
			case Data:
				Util.assertion( !isMustAlias);
				Util.assertion( !isArtificial);
				this.reg = reg;
				break;	
			case Order:
				Util.assertion( reg == 0);
				order = new OrderContent();
				order.isNormalMemory = isNormalMemory;
				order.isArtificial = isArtificial;
				order.isMustAlias = isMustAlias;
				break;
		}
	}

	public int getLatency()
	{
		return latency;
	}

	public void setLatency(int l)
	{
		latency = l;
	}

	public SUnit getSUnit()
	{
		return unit;
	}

	public void setSUnit(SUnit u)
	{
		this.unit = u;
	}

	public Kind getDepKind()
	{
		return depKind;
	}

	public boolean isCtrl()
	{
		return getDepKind() != Data;
	}

	public boolean isNormalMemory()
	{
		return getDepKind() == Order && order.isNormalMemory;
	}

	public boolean isMustAlias()
	{
		return getDepKind() == Order && order.isMustAlias;	
	}

	public boolean isArtificial()
	{
		return getDepKind() == Order && order.isArtificial;
	}

	public boolean isAssignedRegDep()
	{
		return getDepKind() == Data && reg != 0;
	}

	public int getReg()
	{
		Util.assertion( getDepKind() == Data || getDepKind() == Anti			|| getDepKind() == Output);

		return reg;
	}

	public void setReg(int r)
	{
		Util.assertion( getDepKind() == Data || getDepKind() == Anti			|| getDepKind() == Output);

		Util.assertion( getDepKind() != Anti || r != 0);
		Util.assertion( getDepKind() != Output || r != 0);
		reg = r;
	}


	@Override
	public SDep clone()
	{
		return new SDep(unit, depKind,latency, reg,
				order != null && order.isNormalMemory,
				order != null && order.isMustAlias,
				order != null && order.isArtificial);
	}
}