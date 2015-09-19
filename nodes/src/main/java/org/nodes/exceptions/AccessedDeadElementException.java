package org.nodes.exceptions;

public class AccessedDeadElementException extends RuntimeException
{
	private static final long serialVersionUID = 717420046946900204L;

	public AccessedDeadElementException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public AccessedDeadElementException(String message)
	{
		super(message);
	}

	public AccessedDeadElementException(Throwable cause)
	{
		super(cause);
	}

	
}
