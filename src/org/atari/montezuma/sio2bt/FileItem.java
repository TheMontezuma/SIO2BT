package org.atari.montezuma.sio2bt;

import java.util.Locale;

public class FileItem implements Comparable<FileItem>{
	private String name;
	private String data;
	private String path;
	private boolean folder;
	private boolean parent;
	
	public FileItem(String n,String d,String p, boolean folder, boolean parent)
	{
		name = n;
		data = d;
		path = p;
		this.folder = folder;
		this.parent = parent;
	}
	public String getName()
	{
		return name;
	}
	public String getData()
	{
		return data;
	}
	public String getPath()
	{
		return path;
	}
	public int compareTo(FileItem o)
	{
			return name.toLowerCase(Locale.getDefault()).compareTo(o.getName().toLowerCase(Locale.getDefault())); 
	}
	public boolean isFolder()
	{
		return folder;
	}
	public boolean isParent()
	{
		return parent;
	}
}
