package driver;

import java.io.File;

/** 
 * A class was served as representing a source file with different
 * frontend.type, such as .c, assembly, object and executable file.
 * @author Xlous.zeng
 * @version 0.1
 */
public class SourceFile
{
	public enum FileType
	{
		EXT_C_SOURCE(".c"),
		EXT_ASSEMBLY_SOURCE(".s"),
		EXT_OBJECT(".o"), 
		EXT_EXECUTABLE_FILE("");
		
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
		return FileType.EXT_C_SOURCE.match(currentName);
	}
	
	public boolean isAssembly()
	{
		return FileType.EXT_ASSEMBLY_SOURCE.match(currentName);
	}
	
	public boolean isObject()
	{
		return FileType.EXT_OBJECT.match(currentName);
	}
	
	public boolean isExecutable()
	{
		return FileType.EXT_EXECUTABLE_FILE.match(currentName);
	}
	
	public String asmFileName()
	{
		return replaceExtension(FileType.EXT_ASSEMBLY_SOURCE);
	}
	
	public String objectFileName()
	{
		return replaceExtension(FileType.EXT_OBJECT);
	}
	
	public String executableFileName()
	{
		return replaceExtension(FileType.EXT_EXECUTABLE_FILE);
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
	
	private String replaceExtension(FileType type)
    {
	    return baseName(originName, true) + type.extension;
    }
}
