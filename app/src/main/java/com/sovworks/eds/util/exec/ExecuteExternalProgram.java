package com.sovworks.eds.util.exec;

import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.util.TextShell;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ExecuteExternalProgram implements TextShell
{
	public static void makeExecutable(String path) throws ApplicationException
	{
		executeAndReadString("chmod", "0700", path);
	}

	public static class OutputLinesIterator implements Iterator<String>, Iterable<String>
	{
		public OutputLinesIterator(Reader input)
		{
			_input = input;		
		}

		public boolean hasNext()
		{
			if(!_isLineRead)
				readLine();
			return _line!=null;
		}

		public String next()
		{
			if(!hasNext())
				throw new NoSuchElementException();
			_isLineRead = false;
			return _line;
			
		}

		public void remove()
		{
			throw new UnsupportedOperationException();			
		}
		
		public Iterator<String> iterator()
		{
			return this;
		}
		
		private final Reader _input;
		private String _line;
		private boolean _isLineRead;
		private boolean _eof;
		
		private void readLine()
		{
			_line = null;
			_isLineRead = true;
			if(_eof)
				return;	
			StringWriter out = new StringWriter();
			try
			{
				for(;;)
				{
					int n = _input.read();				
					if(n<0 || n=='\n')
					{
						if(n<0)
							_eof = true;
						_line = out.toString();				
						break;
					}				
					if(n!='\r')
						out.write(n);
				}		
			}
			catch (IOException e)
			{	
				e.printStackTrace();
			}
		}	
		
	}
	
	public static Iterable<String> executeAndReadLines(String... commands) throws ApplicationException
	{
		ExecuteExternalProgram exec = new ExecuteExternalProgram();	
		try
		{
			return executeAndReadLines(exec,commands);		
		}
		finally
		{
			exec.close();
		}
	}
	
	public static String executeAndReadString(String... commands) throws ApplicationException
	{
		ExecuteExternalProgram exec = new ExecuteExternalProgram();	
		try
		{
			return executeAndReadString(exec,0,commands);		
		}
		finally
		{
			exec.close();
		}
	}
	
	public static String executeAndReadString(int timeout,String... commands) throws ApplicationException
	{
		ExecuteExternalProgram exec = new ExecuteExternalProgram();	
		try
		{
			return executeAndReadString(exec,timeout,commands);		
		}
		finally
		{
			exec.close();
		}
	}
	
	public static void execute(String... commands) throws ApplicationException
	{
		ExecuteExternalProgram exec = new ExecuteExternalProgram();	
		try
		{
			execute(exec,commands);		
		}
		finally
		{
			exec.close();
		}
	}
	
	public static final Iterable<String> getStringIterable(InputStream inp)
	{
		return new OutputLinesIterator(new InputStreamReader(inp));
	}	
	
	public static final Iterable<String> getStringIterable(Reader inp)
	{
		return new OutputLinesIterator(inp);
	}
	
	public static String[] objectsToStrings(Object... objects)
	{
		String[] strArgs = new String[objects.length];
		for(int i=0;i<objects.length;i++)
			strArgs[i] = objects[i].toString();
		return strArgs;
	}
	
	public ExecuteExternalProgram()
	{
					
	}
	
	@Override
	public void executeCommand(Object... args) throws IOException
	{		
		executeCommand(objectsToStrings(args));		
	}	

	@Override
	public String waitResult() throws ExternalProgramFailedException, IOException
	{
		int res = waitProcess();
		String out = readAll(getProcInputStream());
		if(res!=0)
			throw new ExternalProgramFailedException(res, out + "\n" + out, _currentArgs);

		return out;
	}

	@Override
	public void writeStdInput(String data) throws IOException
	{
		getProcOutputStream().write(data.getBytes());
		closeProcOutputStream();
	}
	
	public int waitProcess()
	{
		try
		{
			return _process.waitFor();
		}
		catch (InterruptedException e)
		{
			return -1;
		}
	}
	
	public InputStream getProcInputStream()
	{
		return _procInputStream;
	}

	public InputStream getProcErrorStream()
	{
		return _process.getErrorStream();
	}

	public OutputStream getProcOutputStream()
	{
		return _procOutputStream;		
	}
	
	public void closeProcOutputStream() throws IOException
	{
		if(_procOutputStream!=null)
		{
			_procOutputStream.close();
			_procOutputStream = null;
		}
	}
	
	public void closeProcInputStream() throws IOException
	{
		if(_procInputStream!=null)
		{
			_procInputStream.close();
			_procInputStream = null;
		}
	}

	public void redirectErrorStream(boolean val)
	{
		_redirectErrorStream = val;
	}

	public void executeCommand(String... command) throws IOException
	{
		if(_process != null)
			throw new RuntimeException("Previous process is active");
		_currentArgs = command;
		_process = new ProcessBuilder().command(command).redirectErrorStream(_redirectErrorStream).start();
		_procInputStream = _process.getInputStream();
		_procOutputStream = _process.getOutputStream();
	}
	
	public void close()
	{		
		try
		{
			try
			{
				closeProcInputStream();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
				
			try
			{
				closeProcOutputStream();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}
		finally
		{
			if(_process!=null)
			{
				_process.destroy();
				_process = null;
			}
		}
	}

	
	protected static Iterable<String> executeAndReadLines(ExecuteExternalProgram exec,String... command) throws ApplicationException
	{
			
		return new OutputLinesIterator(new StringReader(executeAndReadString(exec,0, command)));				
	}
	
	protected static void execute(ExecuteExternalProgram exec,String... command) throws ApplicationException
	{
		try
		{	
			exec.executeCommand(command);			
			int res = exec.waitProcess();
			if(res!=0)
				throw new ExternalProgramFailedException(res,"",command);			
		}
		catch (IOException e)
		{
			throw new ApplicationException("Failed executing external program", e);
		}
	}
	
	protected static String executeAndReadString(final ExecuteExternalProgram exec, int timeout,final String... command) throws ApplicationException
	{		
		try
		{		
			if(timeout>0)
				return CmdRunner.executeCommand(timeout, exec, (Object[]) command);
			else
			{
				exec.executeCommand(command);		
				return exec.waitResult();
			}
		}
		catch (IOException e)
		{
			throw new ApplicationException("Failed executing external program", e);
		}						
	}
	
	public static String readAll(InputStream inp) throws IOException
	{
		InputStreamReader reader = new InputStreamReader(inp);
		StringWriter sw = new StringWriter();
		char[] buf = new char[512];
		int n;
		while((n = reader.read(buf))>=0)			
			sw.write(buf, 0, n);		
		return sw.toString();
	}

	protected Process _process;
	protected InputStream _procInputStream;
	protected OutputStream _procOutputStream;
	protected boolean _redirectErrorStream = true;

	protected String[] _currentArgs;
}

