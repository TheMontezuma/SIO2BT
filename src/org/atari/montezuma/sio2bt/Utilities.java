package org.atari.montezuma.sio2bt;

import java.util.Locale;

public class Utilities
{
    static private byte [] sSingleDensitySectorBuffer = new byte[Disk.SINGLE_DENSITY_SECTOR_SIZE];
	static private byte [] sDoubleDensitySectorBuffer = new byte[Disk.DOUBLE_DENSITY_SECTOR_SIZE];
	
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	public static String byteArray2String(byte[] a)
	{
		   StringBuilder sb = new StringBuilder(a.length * 2);
		   for(byte b: a)
		      sb.append(String.format("%02x", b & 0xff));
		   return sb.toString();
	}
	
	public static String dataBuffer2String(DataBuffer buffer)
	{
		   int data_size = buffer.getSize();
		   byte [] data = buffer.getData();
		   StringBuilder sb = new StringBuilder(data_size * 2);
		   for(int i=0 ; i<data_size ; i++)
		   {
			   sb.append(String.format("%02x", data[i] & 0xff));
		   }
		   return sb.toString();
	}
	
	public static byte[] getBuffer(int size)
	{
		byte[] buffer;
		switch(size)
		{
			case Disk.SINGLE_DENSITY_SECTOR_SIZE:
				buffer = sSingleDensitySectorBuffer;
				break;
			case Disk.DOUBLE_DENSITY_SECTOR_SIZE:
				buffer = sDoubleDensitySectorBuffer;
				break;
			default:
				buffer = new byte[size];
				break;
		}
		return buffer;
	}

}
