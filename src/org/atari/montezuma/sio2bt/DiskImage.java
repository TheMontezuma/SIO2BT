package org.atari.montezuma.sio2bt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.zip.ZipInputStream;

import android.util.Log;

public class DiskImage extends Disk
{
	private static final String TAG = "DiskImage";
    private static final boolean D = false;
    
    static final int ATR_WRITE_PROTECTION_DEFAULT = 0; // keep the default from the ATR header
    private static final String EMPTY_ATR_FILENAME = "sd.zip";
    private static final int EMPTY_ATR_FILESIZE = 92176;
    
    private static final int ATR_HEADER_SIZE = 16;
    
	private DiskInfo mInfo;
	private boolean mReadOnly;

	private File mFile;
	private byte[] mFileHeader;
	private RandomAccessFile mRandomAccessFile;

	public DiskImage(String fileName, int read_write) throws Exception
	{
		if(D) Log.d(TAG, "DiskImage");
		mFile = new File(fileName);

		mRandomAccessFile = new RandomAccessFile(mFile,"r");
		
		mFileHeader = new byte[ATR_HEADER_SIZE];
		if(ATR_HEADER_SIZE != mRandomAccessFile.read(mFileHeader))
		{
			throw new DiskFormatException("ATR header missing");
		}
		
		if((mFileHeader[0] & 0xFF) != 0x96 && (mFileHeader[1] & 0xFF) != 0x02)
		{
			throw new DiskFormatException("invalid ATR");
		}

	    int secSize = (mFileHeader[4] & 0xFF) + ((mFileHeader[5] & 0xFF) * 0x100);
	    if (secSize != 128 && secSize != 256 && secSize != 512)
	    {
	    	throw new DiskFormatException("invalid sector size");
	    }
		
	    int sizeLo =  (mFileHeader[2] & 0xFF) + ((mFileHeader[3] & 0xFF) * 0x100);
	    int sizeHi =  (mFileHeader[6] & 0xFF);

	    long disk_size = (sizeLo + sizeHi * 0x10000) * 0x10; // size of the content (not including the ATR header)
	    
	    if (disk_size > (mRandomAccessFile.length() - ATR_HEADER_SIZE)) 
	    {
	    	disk_size = (mRandomAccessFile.length() - ATR_HEADER_SIZE);
	    }
	    
	    if((secSize == 0x100) && ( (disk_size - 0x180) % 0x100 != 0))
	    {
	    	throw new DiskFormatException("wrong DD format");
	    }

	    mInfo = new DiskInfo((int)disk_size, secSize);
	    
	    mReadOnly = true;
	    
	    try
		{
			switch(read_write)
			{
			case 0: // 0 - keep as it is defined in ATR header
				if((mFileHeader[0xF] & 0x01)!=0x01)
				{
					mRandomAccessFile = new RandomAccessFile(mFile,"rws");
					mReadOnly = false;
				}
				break;
			case 1: // 1 - read only
				if((mFileHeader[0xF] & 0x01)!=0x01)
				{
					mRandomAccessFile = new RandomAccessFile(mFile,"rws");
					// change to R
					mFileHeader[0xF] = (byte)(mFileHeader[0xF] | (byte)0x01);
					mRandomAccessFile.seek(0);
					mRandomAccessFile.write(mFileHeader);
					mRandomAccessFile = new RandomAccessFile(mFile,"r");
				}
				break;
			case 2: // 2 - read write
				mRandomAccessFile = new RandomAccessFile(mFile,"rws");
				mReadOnly = false;
				if((mFileHeader[0xF] & 0x01)==0x01)
				{
					// change to RW
					mFileHeader[0xF] = (byte)(mFileHeader[0xF] & (byte)0xFE);
					mRandomAccessFile.seek(0);
					mRandomAccessFile.write(mFileHeader);
				}
				break;
			}
		}
	    catch(Exception e)
	    {
	    	mRandomAccessFile = new RandomAccessFile(mFile,"r");
	    }
	}
	
	public static void createEmptyDisk(String file_path) throws Exception
	{
		byte[] buffer = new byte[EMPTY_ATR_FILESIZE];
		InputStream in = mAccessManager.open(EMPTY_ATR_FILENAME);
		ZipInputStream zin = new ZipInputStream(in);
		zin.getNextEntry();
		zin.read(buffer);
		zin.close();
		FileOutputStream fos = new FileOutputStream(file_path);
		fos.write(buffer);
		fos.close();
	}
	
	public void finalize()
	{
		if(D) Log.d(TAG, "finalize");
		try
		{
			mRandomAccessFile.close();
		}
		catch(IOException e){}
	}
	
	/**
	 * reads the requested sector form a file
	 * @param sector - number of the requested sector
	 * @return sector data if successful otherwise null
	 */
	
	public DataBuffer readSector(int sector) throws IOException
	{
		//long t1 = System.nanoTime();
		seekToSector(sector);
		DataBuffer buffer = new DataBuffer(mInfo.getBytesPerSector(sector));
		mRandomAccessFile.readFully(buffer.getData(), 0 , buffer.getSize());
		//long t2 = System.nanoTime();
		//long timedif = t2-t1;
		//if(D) Log.d(TAG, "TIME="+timedif);
		return buffer;
	}
	
	/**
	 * writes a sector to a file
	 * @param sector - number of a sector to be written
	 * @param buffer - sector data
	 * @return true if successful, otherwise false
	 */
	public void writeSector(int sector, DataBuffer buffer) throws IOException
	{
		if(mReadOnly)
		{
			throw new IOException();
		}
		seekToSector(sector);
		mRandomAccessFile.write(buffer.getData(),0,buffer.getSize());
	}
	
	public void format(DiskInfo info) throws IOException
	{
		if(D) Log.d(TAG, "format");
		if(mReadOnly)
		{
			throw new IOException();
		}
		
		mInfo = info;

		int disk_size = mInfo.getSize() / 0x10; // in paragraphs
	    int sizeLo =  disk_size % 0x10000;
	    int sizeHi =  disk_size / 0x10000;
	    int bps = mInfo.getBytesPerSector();
		
		mFileHeader[2] = (byte)(sizeLo % 0x100);
		mFileHeader[3] = (byte)(sizeLo / 0x100);
		mFileHeader[4] = (byte)(bps % 0x0100);
		mFileHeader[5] = (byte)(bps / 0x0100);
		mFileHeader[6] = (byte)sizeHi;
		
		OutputStream out = new BufferedOutputStream(new FileOutputStream(mFile));
		out.write(mFileHeader);

		int sector_count = info.getSectorCount();
		for(int sector_no=1 ; sector_no<=sector_count ; sector_no++)
		{
			DataBuffer buffer = new DataBuffer(info.getBytesPerSector(sector_no));
			buffer.clear();
			out.write(buffer.getData(),0,buffer.getSize());
		}
		
		out.close();
	}
	
	public int sectorsPerTrack()
	{
		return mInfo.getSectorsPerTrack();
	}
	
	public int bytesPerSector()
	{
		return mInfo.getBytesPerSector();
	}
	
	public int bytesPerSector(int sector_no)
	{
		return mInfo.getBytesPerSector(sector_no);
	}
	
	public boolean isReadOnly()
	{
		return mReadOnly;
	}
	
	public String getDisplayName()
	{
		return mFile.getName().substring(0, mFile.getName().length()-4); // minus suffix
	}
	
	public String getName()
	{
		return mFile.getName();
	}
	
	public String getPath()
	{
		return mFile.getPath();
	}
	
	public String getParent()
	{
		return mFile.getParent();
	}
	
	public String getDescription()
	{
		StringBuilder sb = new StringBuilder(mInfo.toString());
		sb.append(" (");
		sb.append(Utilities.humanReadableByteCount(mFile.length(),false));
		sb.append(mReadOnly?") R":") RW");
		return sb.toString();
	}
	
	private void seekToSector(int sector) throws IOException
	{
    	int pos = mInfo.getSectorPosition(sector);
    	pos+=ATR_HEADER_SIZE; // ATR file header size
		mRandomAccessFile.seek(pos);
	}

	public boolean isSectorInRange(int sector)
	{
		return (sector > 0 && sector <= mInfo.getSectorCount());
	}
	
	public byte[] getPercomBlock()
	{
		return mInfo.toPercomBlock();
	}

}
