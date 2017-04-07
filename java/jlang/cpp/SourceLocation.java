package jlang.cpp;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2017, Xlous Zeng.
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
public class SourceLocation
{
    public static final SourceLocation NOPOS = new SourceLocation();

    int column;
    int line;

    public SourceLocation(int line, int col)
    {
        column = col;
        this.line = line;
    }

    public SourceLocation(SourceLocation loc)
    {
        column = loc.column;
        line = loc.line;
    }

    public SourceLocation()
    {
        column = -1;
        line = -1;
    }

    public boolean isValid()
    {
        return column < 0 || line < 0;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;

        if (getClass() != obj.getClass())
            return false;
        SourceLocation loc = (SourceLocation)obj;
        return line == loc.line && column == loc.column;
    }

    public static class SourceRange
    {
        private SourceLocation start;
        private SourceLocation end;

        public SourceRange()
        {
            start = new SourceLocation();
            end = new SourceLocation();
        }

        public SourceRange(SourceLocation start, SourceLocation end)
        {
            this.start = start;
            this.end = end;
        }

        public SourceRange(SourceLocation loc)
        {
            this(loc, loc);
        }

        public SourceLocation getStart() { return start; }
        public SourceLocation getEnd() { return end; }

        public void setStart(SourceLocation start)
        {
            this.start = start;
        }
        public void setEnd(SourceLocation end)
        {
            this.end = end;
        }

        public boolean isValid()
        {
            return start.isValid() && end.isValid();
        }

        public boolean isInvalid()
        {
            return !isValid();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null) return false;
            if (this == obj) return true;
            if (getClass() != obj.getClass())
                return false;

            SourceRange other = (SourceRange) obj;
            return start.equals(other.start) && end.equals(other.end);
        }
    }
}
