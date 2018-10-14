package org.atari.montezuma.sio2bt;

public class DiskInfo
{
	private static final String EXCEPTION_MESSAGE = "invalid image";

// SD = Single density
// ED = Enhanced density
// DD = Double density
// DS/DD = Double sided, double density

// Density  sides TPS SPT BPS enc  total  bytes
// SD         1   40  18  128 FM   92160  (90K)
// ED         1   40  26  128 MFM 133120 (130K)
// DD         1   40  18  256 MFM 183936 (180K) because for the first 3 sectors BPS=128
// DS/DD      2   40  18  256 MFM 368256 (360K) because for the first 3 sectors BPS=128

	static final int SIZE_SD =  92160; // single-sided, single-density
	static final int SIZE_ED = 133120; // single-sided, enhanced-density
	static final int SIZE_DD = 183936; // single-sided, double-density
	static final int SIZE_DSDD = 368256; // double-sided, double-density

	private boolean mDoubleSided;
	private int     mTracksPerSide;
	private int     mSectorsPerTrack;
	private int     mBytesPerSector;
	private int     mSectorCount;
	private int     mSize;

	public DiskInfo(int size, int bytesPerSector) throws DiskFormatException
	{
	    boolean doubleSided;
	    int tracksPerSide;
	    int sectorsPerTrack;
        
        // detect well known size disks

	    if(			size == SIZE_SD &&
	    			bytesPerSector == 128)
	    {
	    	// single-sided, single-density
	    	doubleSided = false;
	        tracksPerSide = 40;
	        sectorsPerTrack = 18;
	    }
	    else if(	size == SIZE_ED &&
	    			bytesPerSector == 128)
	    {
	    	// single-sided, enhanced-density
	    	doubleSided = false;
	        tracksPerSide = 40;
	        sectorsPerTrack = 26;
	    }
	    else if (	size == SIZE_DD &&
	    			bytesPerSector == 256)
	    {
	    	// single-sided, double-density
	    	doubleSided = false;
	        tracksPerSide = 40;
	        sectorsPerTrack = 18;
	    }
	    else if (	size == SIZE_DSDD &&
	    			bytesPerSector == 256)
	    {
	    	// double-sided, double-density
	    	doubleSided = true;
	        tracksPerSide = 40;
	        sectorsPerTrack = 18;
	    }
	    else
	    {
            // handle custom disks
            
	        doubleSided = false;
	        tracksPerSide = 1;
	        
	        if (bytesPerSector == 256)
	        {
	            if (size <= 384)
	            {
	            	if((size % 128) != 0) throw new DiskFormatException(EXCEPTION_MESSAGE);
	                sectorsPerTrack = size / 128;
	            }
	            else
	            {
	            	if((size + 384) % bytesPerSector != 0) throw new DiskFormatException(EXCEPTION_MESSAGE);
	            	sectorsPerTrack = (size + 384) / bytesPerSector;
	            }
	        }
	        else
	        {
	        	if((size % bytesPerSector) != 0) throw new DiskFormatException(EXCEPTION_MESSAGE);
	            sectorsPerTrack = size / bytesPerSector;
	        }
	    }
		
	    setup(doubleSided, tracksPerSide, sectorsPerTrack, bytesPerSector);
	}
	
	public DiskInfo(byte[] percom) throws DiskFormatException
	{
		boolean doubleSided = (percom[4] & 0xFF) != 0;
		int tracksPerSide   = (percom[0] & 0xFF);
	    int sectorsPerTrack = (percom[2] & 0xFF) * 256 + (percom[3] & 0xFF);
	    int bytesPerSector  = (percom[6] & 0xFF) * 256 + (percom[7] & 0xFF);
	    
	    setup(doubleSided, tracksPerSide, sectorsPerTrack, bytesPerSector);
	}
	
	private void setup(boolean doubleSided, int tracksPerSide, int sectorsPerTrack, int bytesPerSector)  throws DiskFormatException
	{
		mDoubleSided     = doubleSided;
	    mTracksPerSide   = tracksPerSide;
	    mSectorsPerTrack = sectorsPerTrack;
	    mBytesPerSector  = bytesPerSector;
	    mSectorCount     = (mDoubleSided?2:1) * mTracksPerSide * mSectorsPerTrack;
	    
	    if (mBytesPerSector == 256)
	    {
	        mSize = mSectorCount * 128;
	        if (mSize > 384)
	        {
	            mSize += 128 * (mSectorCount - 3);
	        }
	    }
	    else
	    {
	        mSize = mSectorCount * mBytesPerSector;
	    }
	    if (mSectorCount > 0xFFFF)
	    {
	    	throw new DiskFormatException(EXCEPTION_MESSAGE);
	    }
	}
	
	public int getSectorPosition(int sector)
	{
    	int pos = (sector - 1) * getBytesPerSector();
    	if (getBytesPerSector() == 256)
    	{
    		if (sector <= 3)
    		{
    			pos = (sector - 1) * 128;
    		}
    		else
    		{
    			pos -= 384;
    		}
    	}
    	return pos;
	}
	
	public boolean isStandardSD()
	{
		return  (!mDoubleSided) && 
                mTracksPerSide   == 40 && 
                mSectorsPerTrack == 18 && 
                mBytesPerSector  == 128;
	}
	
	public boolean isStandardED()
	{
		return  (!mDoubleSided) && 
                mTracksPerSide   == 40 &&
                mSectorsPerTrack == 26 &&
                mBytesPerSector  == 128;
	}

	public boolean isStandardDD()
	{
		return  (!mDoubleSided) && 
                mTracksPerSide   == 40 &&
                mSectorsPerTrack == 18 &&
                mBytesPerSector  == 256;
	}

	public boolean isStandardDSDD()
	{
		return  mDoubleSided && 
                mTracksPerSide   == 40 && 
                mSectorsPerTrack == 18 && 
                mBytesPerSector  == 256;
	}
	
	public boolean isDoubleSided()
	{
		return mDoubleSided;
	}
	
	public int getTracksPerSide()
	{
		return mTracksPerSide;
	}
	
	public int getSectorsPerTrack()
	{
		return mSectorsPerTrack;
	}

	public int getBytesPerSector()
	{
		return mBytesPerSector;
	}
	
	public int getBytesPerSector(int sector)
	{
	    int result = mBytesPerSector;
	    if (result == 256 && sector <= 3)
	    {
	        result = 128;
	    }
	    return result;
	}

	public int getSectorCount()
	{
		return mSectorCount;
	}
	
	public int getSize()
	{
		return mSize;
	}
	
//        TR SR SH SL DS FM BH BL OL na na na
//------------------------------------------------------------------------------
//0090    28 01 00 12 00 00 00 80 FF 00 00 00   SD,   720 sectors of 128 bytes
//0130    28 01 00 1A 00 04 00 80 FF 00 00 00   MD,  1040 sectors of 128 bytes
//0180    28 01 00 12 00 04 01 00 FF 00 00 00   DD,   720 sectors of 256 bytes
//0360    28 01 00 12 01 04 01 00 FF 00 00 00   QD,  1440 sectors of 256 bytes
//0720    50 01 00 12 01 04 01 00 FF 00 00 00        2880 sectors of 256 bytes
//1440    50 01 00 24 01 04 01 00 FF 00 00 00   HD,  5760 sectors of 256 bytes
//2880    50 01 00 48 01 04 01 00 FF 00 00 00   ED, 11520 sectors of 256 bytes
//HDD/RD  01 01 xx yy 00 04 01 00 FF 00 00 00   any other sectors of 256 bytes
	
//	Offset 	Description
//	$00 	Number of tracks
//	$01 	Step rate (00=30ms 01=20ms 10=12ms 11=6ms)
//	$02 	Sectors per Track HIGH
//	$03 	Sectors per Track LOW
//	$04 	Number of sides decreased by one
//	$05 	Record Method (0=FM single, 4=MFM double, 6=1.2M)
//	$06 	Bytes per Sector HIGH
//	$07 	Bytes per Sector LOW
//	$08 	Drive online ($FF or $40 for XF551)
//	$09 	Unused (Serial rate control?)
//	$0A 	Unused
//	$0B 	Unused 
	
	public byte[] toPercomBlock()
	{
	    byte[] percom = new byte[Disk.PERCOM_FRAME_SIZE];
	    percom[0] = (byte)mTracksPerSide;
	    percom[1] = (byte)1; // Step rate
	    percom[2] = (byte)(mSectorsPerTrack / 256); // ! BIG ENDIAN !
	    percom[3] = (byte)(mSectorsPerTrack % 256); // ! BIG ENDIAN !
	    percom[4] = (byte)(mDoubleSided?1:0);
	    percom[5] = (byte)(((isStandardSD())?0:4) + ((mTracksPerSide == 77)?2:0));
	    percom[6] = (byte)(mBytesPerSector / 256); // ! BIG ENDIAN !
	    percom[7] = (byte)(mBytesPerSector % 256); // ! BIG ENDIAN !
	    percom[8] = (byte)(0xFF);
	    return percom;
	}
	
	public String toString()
	{
	    StringBuilder sb = new StringBuilder("ATR");

	    if (isStandardSD())
	    {
	    	sb.append(" SD");
	    }
	    else if (isStandardED())
	    {
	    	sb.append(" ED");
	    }
	    else if (isStandardDD())
	    {
	        sb.append(" DD");
	    }
	    else if (isStandardDSDD())
	    {
	        sb.append(" DS/DD");
	    }
	    return sb.toString();
	}
}
