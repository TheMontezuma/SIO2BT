package org.atari.montezuma.sio2bt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Locale;

import android.util.Log;

public class Executable extends Disk
{
    private static final String TAG = "Executable";
    private static final boolean D = false;
	
	static final int LOADER_MIN_OFFSET = 0;		// 5-th page
	static final int LOADER_DEFAULT_OFFSET = 2; // 7-th page
	static final int LOADER_MAX_OFFSET = 185;   // 190-th page
	
	private static final int SECTOR_DATA_SIZE = 125;
	private int mExecutableSectorCount;
	private File mFile;
	private long mFileLength;
	private byte [] mFileName;
	private RandomAccessFile mRandomAccessFile;
	
	private static byte[] sLoader;
	private static final String LOADER_FILENAME = "loader";
	private final static int SECTOR_OFFSET = 0x171;
	private int mLoaderSectorCount;
	private byte[] mLoader;
	private int mFileLoaderOffset; // 0->$500
	
	public Executable(String fileName, int offset) throws Exception
	{
		mFile = new File(fileName);
		mRandomAccessFile = new RandomAccessFile(mFile,"r");
		mExecutableSectorCount = (int)((mRandomAccessFile.length()+SECTOR_DATA_SIZE-1)/SECTOR_DATA_SIZE);
		if(mExecutableSectorCount > (0xFFFF-0x171))
		{
			throw new Exception();
		}
		mFileLength = mRandomAccessFile.length();
		mFileName = mFile.getName().toUpperCase(Locale.getDefault()).getBytes("US-ASCII");
		
		int BYTE1 = mRandomAccessFile.read();
		int BYTE2 = mRandomAccessFile.read();
		
		if(BYTE1 != 0xFF || BYTE2 != 0xFF)
		{
			throw new DiskFormatException("invalid executable file");
		}
		
		mFileLoaderOffset = offset;
		if(sLoader == null)
		{
			InputStream in = mAccessManager.open(LOADER_FILENAME);
			sLoader = new byte[in.available()];
			in.read(sLoader);
			in.close();
		}
		mLoaderSectorCount = (sLoader.length+Disk.SINGLE_DENSITY_SECTOR_SIZE-1)/Disk.SINGLE_DENSITY_SECTOR_SIZE;
		mLoader = new byte[Disk.SINGLE_DENSITY_SECTOR_SIZE*mLoaderSectorCount];
		for(int i=0 ; i<sLoader.length ; i++ )
		{
			mLoader[i] = sLoader[i];
			if(mLoader[i] == (byte)0x07) {mLoader[i] += (mFileLoaderOffset-2);}
		}
	}
	
	public DataBuffer readSector(int sector_no) throws IOException
	{
		DataBuffer buffer = new DataBuffer(SINGLE_DENSITY_SECTOR_SIZE);
		buffer.clear(); // fills with zeros
		byte[] data = buffer.getData();
		
		if(isSectorInRange(sector_no))
		{
			if(sector_no<=mLoaderSectorCount)
			{
				System.arraycopy(mLoader, (sector_no-1)*SINGLE_DENSITY_SECTOR_SIZE, data, 0, SINGLE_DENSITY_SECTOR_SIZE);
			}
			else if(sector_no == 0x167)
			{
				// second VTOC for emulated MyDos disks with huge number of sectors
			}
			else if(sector_no == 0x168)
			{
				data[0] = (byte)((mExecutableSectorCount > (0x400-0x171))? 0x03 : 0x02); // MyDos / DOS 2.0
				data[1] = (byte)(mExecutableSectorCount & 0xFF);		// initial number of free sectors LSB
				data[2] = (byte)((mExecutableSectorCount >> 8) & 0xFF); // initial number of free sectors MSB
				// data[3] - current number of free sectors LSB = 0 
				// data[4] - current number of free sectors MSB = 0
			}
			else if(sector_no == 0x169)
			{
				data[0] = (byte)((mExecutableSectorCount > (0x400-0x171))? 0x66 : 0x62);
				data[1] = (byte)(mExecutableSectorCount & 0xFF);
				data[2] = (byte)((mExecutableSectorCount >> 8) & 0xFF);
				data[3] = (byte)(SECTOR_OFFSET & 0xFF);
				data[4] = (byte)((SECTOR_OFFSET >> 8) & 0xFF);
	
				int i=5;
				while(i<16)
				{
					data[i++] = (byte)0x20;
				}
				i=5;
				for(int j=0 ; i<13 && j<mFileName.length; j++)
				{
					if((mFileName[j] & 0xFF) == 0x2E)
					{
						break;
					}
					else if
					(
					((i==5) && (Character.isLetter(mFileName[j] & 0xFF))) ||
					((i!=5) && (Character.isLetterOrDigit(mFileName[j] & 0xFF)))
					)
					{
						data[i]=mFileName[j];
						i++;
					}
				}
				if(i==5)
				{
					data[i++]='F';
					data[i++]='I';
					data[i++]='L';
					data[i++]='E';
				}
				for(i=13 ; i<16 ; i++)
				{
					data[i] = mFileName[i-13+mFileName.length-3];
				}
			}
			else
			{
		    	int pos = (sector_no-SECTOR_OFFSET)*(SECTOR_DATA_SIZE);
				mRandomAccessFile.seek(pos);
				if(mFileLength-pos <= SECTOR_DATA_SIZE)
				{
					int last_datachunk_size = (int)(mFileLength-pos);
					mRandomAccessFile.read(data, 0, last_datachunk_size);
					data[125] = 0;
					data[126] = 0;
					data[127] = (byte)((last_datachunk_size & 0xFF));
				}
				else
				{
					mRandomAccessFile.read(data, 0, SECTOR_DATA_SIZE);
					data[125] = (byte)(((sector_no+1)>>8) & 0xFF); // BIG ENDIAN !
					data[126] = (byte)((sector_no+1) & 0xFF);      // BIG ENDIAN !
					data[127] = SECTOR_DATA_SIZE;
				}
			}
		}
		return buffer;
	}
	
	public boolean isSectorInRange(int sector)
	{
		return  (sector >= 1 && sector <= mLoaderSectorCount) || 
				(sector == 0x167) ||
				(sector == 0x168) ||
				(sector == 0x169) ||
				(sector >= SECTOR_OFFSET && sector <=SECTOR_OFFSET+mExecutableSectorCount);
	}
	
	public int getLoaderOffset()
	{
		return mFileLoaderOffset;
	}
	
	public void writeSector(int sector, DataBuffer data) throws IOException
	{
		throw new IOException();
	}

	public void format(DiskInfo info) throws IOException
	{
		throw new IOException();
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
	
	public int sectorsPerTrack()
	{
		return (SECTOR_OFFSET+mExecutableSectorCount);
	}

	public int bytesPerSector()
	{
		return Disk.SINGLE_DENSITY_SECTOR_SIZE;
	}
	
	public int bytesPerSector(int sector_no)
	{
		return Disk.SINGLE_DENSITY_SECTOR_SIZE;
	}

	public boolean isReadOnly()
	{
		return true;
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
		StringBuilder sb = new StringBuilder(mFile.getName().substring(mFile.getName().length()-3).toUpperCase(Locale.getDefault()));
		sb.append(" $").append(Integer.toHexString(mFileLoaderOffset+5).toUpperCase(Locale.getDefault())).append("00");
		sb.append(" (").append(Utilities.humanReadableByteCount(mFileLength, false)).append(") R");
		return sb.toString();
	}
	
	public byte[] getPercomBlock()
	{
	    byte[] percom = new byte[Disk.PERCOM_FRAME_SIZE];
	    percom[0] = (byte)1;  // tracks per side
	    percom[1] = (byte)1;  // Step rate
	    percom[2] = (byte)((SECTOR_OFFSET+mExecutableSectorCount) / 0x100); // sector per track ! BIG ENDIAN !
	    percom[3] = (byte)((SECTOR_OFFSET+mExecutableSectorCount) % 0x100); // sector per track ! BIG ENDIAN !
	    percom[4] = (byte)(0); // single sided
	    percom[5] = (byte)(0); // FM
	    percom[6] = (byte)(0);                          // bytes per sector ! BIG ENDIAN !
	    percom[7] = (byte)(SINGLE_DENSITY_SECTOR_SIZE); // bytes per sector ! BIG ENDIAN !
	    percom[8] = (byte)(0xFF);
	    return percom;
	}

}
