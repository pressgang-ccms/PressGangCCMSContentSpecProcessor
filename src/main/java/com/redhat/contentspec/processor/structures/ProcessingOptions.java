package com.redhat.contentspec.processor.structures;

public class ProcessingOptions {

	private boolean permissiveMode = false;
	private boolean validate = false;
	private boolean ignoreSpecRevision = false;
	private boolean allowEmptyLevels = false;
	
	public boolean isPermissiveMode()
	{
		return permissiveMode;
	}
	
	public void setPermissiveMode(boolean permissiveMode)
	{
		this.permissiveMode = permissiveMode;
	}

	public boolean isValidating()
	{
		return validate;
	}

	public void setValidating(boolean validating)
	{
		this.validate = validating;
	}

	public boolean isIgnoreSpecRevision() {
		return ignoreSpecRevision;
	}

	public void setIgnoreChecksum(boolean ignoreSpecRevision)
	{
		this.ignoreSpecRevision = ignoreSpecRevision;
	}

	public boolean isAllowEmptyLevels()
	{
		return allowEmptyLevels;
	}

	public void setAllowEmptyLevels(boolean allowEmptyLevels)
	{
		this.allowEmptyLevels = allowEmptyLevels;
	}
}
