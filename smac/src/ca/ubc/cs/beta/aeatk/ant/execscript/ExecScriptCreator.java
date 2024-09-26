package ca.ubc.cs.beta.aeatk.ant.execscript;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.returnvalues.AEATKReturnValues;

public class ExecScriptCreator {


public static void main(String[] args)
{
	ExecScriptCreatorOptions opts = new ExecScriptCreatorOptions();
	JCommander jcom = JCommanderHelper.getJCommanderAndCheckForHelp(args,opts);
	try {
		jcom.parse(args);
		
		if(!opts.skipClassCheck)
		{
			try {
				Class.forName(opts.clazz);
			} catch (ClassNotFoundException e) {
				
				System.out.println("Classpath is: " + System.getProperty("java.class.path"));
				throw new ParameterException("Error locating class: " + opts.clazz + " error: " + e.getClass().getCanonicalName() + ":" + e.getMessage() + "\n Maybe try using: --skip-class-check");
				
				
			}
		}
		
		
		File f = new File(opts.filename).getAbsoluteFile();
		
		if(f.isDirectory())
		{
			f = new File(opts.filename + File.separator + opts.nameOfProgram).getAbsoluteFile();
		} else if(!f.getParentFile().exists())
		{
			boolean created = f.getParentFile().mkdirs();
			
			if(!created)
			{
				throw new ParameterException("Could not create parent directory " + f.getParentFile());
			}
					
		}
		
		String script = getScript(opts.clazz,opts.nameOfProgram, opts.mem,opts.printMem);
		
			
		System.out.println("Execution Script writing to: "  + f);
		
		FileWriter fWrite = new FileWriter(f);
		
		fWrite.write(script);
		fWrite.close();

		if(opts.batFile)
		{
			
			String batchFile = getBatch(opts.clazz, opts.nameOfProgram,opts.mem,opts.printMem);

			File f2 = new File(opts.filename);
			
			if(f2.isDirectory())
			{
				f2 = new File(opts.filename + File.separator + opts.nameOfProgram + ".bat");
			} else if(!f2.getParentFile().exists())
			{
				boolean created = f2.getParentFile().mkdirs();
				
				if(!created)
				{
					throw new ParameterException("Could not create parent directory " + f2.getParentFile());
				}
						
			}
			
			
			
			System.out.println("Execution Script writing to: "  + f2);
			
			FileWriter fWrite2 = new FileWriter(f2);
			
			fWrite2.write(batchFile);
			fWrite2.close();
			
			
		}
	} catch(ParameterException e)
	{
		e.printStackTrace();
		System.exit(AEATKReturnValues.PARAMETER_EXCEPTION);
	} catch (IOException e) {
		e.printStackTrace();
		System.exit(AEATKReturnValues.OTHER_EXCEPTION);
	} 
	
	System.exit(AEATKReturnValues.SUCCESS);
	
	
	
	
}

public static String getScript(String javaClassName, String nameOfProgram, int ram, boolean printMem)
{
	
	/*
#!/usr/bin/env bash
SMAC_MEMORY_INPUT=$SMAC_MEMORY
SMACMEM=1024
test "$SMAC_MEMORY_INPUT" -ge 1 2>&- && SMACMEM=$SMAC_MEMORY_INPUT
EXEC=ca.ubc.cs.beta.smac.executors.AutomaticConfigurator
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ ! -d "$DIR/lib" ]; then
        DIR="$(dirname "$DIR")"
fi



echo "Starting with $SMACMEM MB of RAM"

for f in $DIR/*.jar
do
        jarconcat=$jarconcat:$f
done

jarconcat=${jarconcat:1}

exec java -Xmx"$SMACMEM"m -cp "$DIR/conf/:$jarconcat" $EXEC "$@"
*/

	StringBuilder sb = new StringBuilder();
	sb.append("#!/usr/bin/env bash").append("\n"); 
	sb.append("SMAC_MEMORY_INPUT=$SMAC_MEMORY").append("\n");
	sb.append("SMACMEM=").append(ram).append("\n");
	
	sb.append("test \"$SMAC_MEMORY_INPUT\" -ge 1 2>&- && SMACMEM=$SMAC_MEMORY_INPUT").append("\n"); 
	sb.append("EXEC=").append(javaClassName).append("\n"); 
	sb.append("DIR=\"$( cd \"$( dirname \"${BASH_SOURCE[0]}\" )\" && pwd )\"").append("\n");
	
	sb.append("if [ ! -d \"$DIR/lib\" ]; then").append("\n");
	sb.append("DIR=\"$(dirname \"$DIR\")\"").append("\n");
	sb.append("fi").append("\n");
	
	sb.append("LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$DIR/lib/native/:$DIR/lib/:$DIR/\n");
	sb.append("DYLD_FALLBACK_LIBRARY_PATH=$DYLD_FALLBACK_LIBRARY_PATH:$DIR/lib/native/:$DIR/lib/:$DIR/\n");
	
	
	if(printMem) {
		sb.append("echo \"Starting ").append(nameOfProgram).append(" with $SMACMEM MB of RAM\"").append("\n"); 
	}
	sb.append("\n");
	sb.append("for f in $DIR/lib/*.jar").append("\n");
	sb.append("do").append("\n");
	        sb.append("\tjarconcat=$jarconcat:$f").append("\n");
	sb.append("done").append("\n");
	sb.append("for f in $DIR/*.jar").append("\n");
	sb.append("do").append("\n");
	        sb.append("\tjarconcat=$jarconcat:$f").append("\n");
	sb.append("done").append("\n");
	sb.append("jarconcat=${jarconcat:1}\n");
	sb.append("\n");
	sb.append("exec java -Xmx\"$SMACMEM\"m -cp \"$DIR/conf/:$DIR/patches/:$jarconcat:$DIR/patches/\" ca.ubc.cs.beta.aeatk.ant.execscript.Launcher $EXEC \"$@\"").append("\n");
	
	return sb.toString();
}

	/**
	 * Creates a batch file for a program
	 * @param javaClassName
	 * @param nameOfProgram
	 * @return the batch file 
	 */
	public static String getBatch(String javaClassName, String nameOfProgram,int ram, boolean printMem)
	{
		/* courtesy of Chris Thornton
		  @echo off
			set SMACMEM=1024
			IF NOT "%SMAC_MEMORY%"=="" (set SMACMEM=%SMAC_MEMORY%)
			set DIR=%~dp0
			IF EXIST "%DIR%\lib\" GOTO USE_LIB
			set DIR=%DIR%\..\
			:USE_LIB
		
			set EXEC=ca.ubc.cs.beta.smac.executors.AutomaticConfigurator
			set jarconcat=
			SETLOCAL ENABLEDELAYEDEXPANSION
			for /F "delims=" %%a IN ('dir /b /s "%DIR%\*.jar"') do set jarconcat=%%a;!jarconcat!
			echo Starting with %SMACMEM% MB of RAM
			@echo on
			java -Xmx%SMACMEM%m -cp "%DIR%conf\;%DIR%patches\;%jarconcat%%DIR%patches\ " %EXEC% %*

		 */
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("@echo off").append("\r\n");
		sb.append("set SMACMEM=").append(ram).append("\r\n");
		sb.append("IF NOT \"%SMAC_MEMORY%\"==\"\" (set SMACMEM=%SMAC_MEMORY%)").append("\r\n");
		sb.append("set DIR=%~dp0").append("\r\n");
		sb.append("IF EXIST \"%DIR%\\lib\\\" GOTO USE_LIB").append("\r\n");
		sb.append("set DIR=%DIR%\\..\\").append("\r\n");
		sb.append(":USE_LIB").append("\r\n");
		sb.append("\r\n");

		sb.append("set EXEC="+ javaClassName).append("\r\n");
		sb.append("set jarconcat=").append("\r\n");
		sb.append("SETLOCAL ENABLEDELAYEDEXPANSION").append("\r\n");
		sb.append("for /F \"delims=\" %%a IN ('dir /b /s \"%DIR%\\*.jar\"') do set jarconcat=%%a;!jarconcat!").append("\r\n");
		sb.append("for /F \"delims=\" %%a IN ('dir /b /s \"%DIR%\\lib\\*.jar\"') do set jarconcat=%%a;!jarconcat!").append("\r\n");
		if(printMem)
		{
			sb.append("echo Starting "+ nameOfProgram + " with %SMACMEM% MB of RAM").append("\r\n");
		}
		//sb.append("@echo on").append("\n");
		sb.append("java -Xmx%SMACMEM%m -cp \"%DIR%conf\\;%DIR%patches\\;%jarconcat%%DIR%patches\\ \" ca.ubc.cs.beta.aeatk.ant.execscript.Launcher %EXEC% %*").append("\r\n");

		return sb.toString();
	}
}

