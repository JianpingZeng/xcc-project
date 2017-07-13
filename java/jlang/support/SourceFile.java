package jlang.support;

import java.io.File;

/** 
 * A class was served as representing a source file with different
 * jlang.type, such as .c, assembly, object and executable file.
 * @author Xlous.zeng
 * @version 0.1
 */
public class SourceFile
{
	public enum FileType
	{
		IRSource(".ir"),
		CSource(".c"),
		AsmSource(".s"),
		Obj(".o"),
		Executable("");

		public final String extension;
		FileType(String extension)
		{
			this.extension = extension;
		}
		/**
		 * checks for the specified file is ends with extension.
		 * @param filename
		 * @return
		 */
		public boolean match(String filename)
		{
			return filename.endsWith(extension);
		}
		public static FileType[] KNOWN_EXTENSIONS = values();
	}

	private final String originName;
	private String currentName;
	
	public SourceFile(String name)
	{
		this.originName = name;
		this.currentName = name;
	}

	@Override
	public String toString()
	{
		return currentName;
	}
	
	public String path()
	{
		return currentName;
	}
	
	public String getCurrentName()
	{
		return currentName;
	}
	
	public void setCurrentName(String filename)
	{
		this.currentName = filename;
	}
	
	public boolean isKnownFileType()
	{
		for (FileType type : FileType.KNOWN_EXTENSIONS)
		{
			if (type.match(currentName))
				return true;
		}
		return false;		
	}
	
	public boolean isCSource()
	{
		return FileType.CSource.match(currentName);
	}
	
	public boolean isAssembly()
	{
		return FileType.AsmSource.match(currentName);
	}
	
	public boolean isObject()
	{
		return FileType.Obj.match(currentName);
	}
	
	public boolean isExecutable()
	{
		return FileType.Executable.match(currentName);
	}
	
	public String asmFileName()
	{
		return replaceExtension(FileType.AsmSource);
	}
	
	public String objectFileName()
	{
		return replaceExtension(FileType.Obj);
	}
	
	public String executableFileName()
	{
		return replaceExtension(FileType.Executable);
	}
	
	private String baseName(String path)
	{
		return new File(path).getName();
	}
	private String baseName(String path, boolean stripExt)
	{
		if (stripExt)
		{
			return new File(path).getName().replaceFirst("\\.[^.]*$", "");
		}
		else
		{
			return baseName(path);
		}
	}
	
	public String replaceExtension(FileType type)
    {
	    return baseName(originName, true) + type.extension;
    }
}
