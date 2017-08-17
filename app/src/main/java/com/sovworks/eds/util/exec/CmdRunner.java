package com.sovworks.eds.util.exec;

import com.sovworks.eds.exceptions.ApplicationException;
import com.sovworks.eds.util.TextShell;

import java.io.IOException;

public class CmdRunner extends Thread
{
	public static String executeCommand(int timeout, TextShell exec, Object... command) throws ApplicationException
	{
		CmdRunner cmr = new CmdRunner(exec, command);
		cmr.start();
		try
		{
			cmr.join(timeout);
		}
		catch (InterruptedException ignored)
		{
		}
		if(cmr.isAlive())
		{
			exec.close();
			throw new ApplicationException("Timeout error");
		}
		try
		{
			return cmr.getResult();
		}
		catch (Throwable e)
		{
			throw new ApplicationException("Failed executing command", e);
		}
	}
    public CmdRunner(TextShell exec, Object... command)
    {
        _exec = exec;
        _command = command;
    }

    public String getResult() throws Throwable
    {
        if (_error != null)
            throw _error;
        return _result;
    }

    @Override
    public void run()
    {
        try
        {
            _exec.executeCommand(_command);
            _result = _exec.waitResult();
        }
		catch (ExternalProgramFailedException | IOException e)
		{
			_error = e;
		}
	}

    private Throwable _error;
    private String _result;
    private final Object[] _command;
    private final TextShell _exec;
}
