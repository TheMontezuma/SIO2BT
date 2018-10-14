package org.atari.montezuma.sio2bt;

import java.util.Arrays;


public class DataBuffer
{
	static private byte [] sBuffer = new byte[Disk.SINGLE_DENSITY_SECTOR_SIZE+1];
	static private int sSize;
	
	DataBuffer(int size)
	{
		sSize = size;
		if(sBuffer.length < sSize+1)
		{
			sBuffer = new byte[sSize+1]; // +1 <- place for a checksum
		}
	}
	
	public void clear()
	{
		Arrays.fill(sBuffer, 0, sSize, (byte)0);
	}
	
	public byte[] getData()
	{
		return sBuffer;
	}
	
	public int getSize()
	{
		return sSize;
	}
	
	public byte getChecksum()
	{
		return sBuffer[sSize];
	}
	
	public void setChecksum(byte checksum)
	{
		sBuffer[sSize] = checksum;
	}
}
