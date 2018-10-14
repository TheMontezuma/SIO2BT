package org.atari.montezuma.sio2bt;

import java.io.IOException;
import java.util.Locale;

import android.content.res.AssetManager;
import android.util.Log;

public abstract class Disk extends SIODevice
{
    private static final String TAG = "Disk";
    private static final boolean D = false;
    
    static AssetManager mAccessManager;

	static final int STATUS_FRAME_SIZE = 0x04;
	static final int PERCOM_FRAME_SIZE = 0x0C;
	static final int SINGLE_DENSITY_SECTOR_SIZE = 0x80;
	static final int DOUBLE_DENSITY_SECTOR_SIZE = 0x0100;
	static final byte SPEED_BYTE = (byte)0x28;
    
    protected final static int CMD_FORMAT                   = 0x21;
    protected final static int CMD_FORMAT_ED                = 0x22;
    protected final static int CMD_READ_SECTOR              = 0x52;
    protected final static int CMD_WRITE_SECTOR             = 0x50;
    protected final static int CMD_WRITE_SECTOR_WITH_VERIFY = 0x57;
    protected final static int CMD_GET_STATUS               = 0x53;
    protected final static int CMD_READ_PERCOM              = 0x4E;
    protected final static int CMD_WRITE_PERCOM             = 0x4F;
    protected final static int CMD_SPEED_POLL               = 0x3F;
    protected final static int CMD_QUIT                     = 0x51;
	
    public abstract String getPath();
    public abstract String getName();
    public abstract String getParent();
	public abstract boolean isSectorInRange(int sector);
	public abstract DataBuffer readSector(int sector) throws IOException;
	public abstract void writeSector(int sector, DataBuffer buffer) throws IOException;
	public abstract void format(DiskInfo info) throws IOException;
	public abstract int sectorsPerTrack();
	public abstract int bytesPerSector();
	public abstract int bytesPerSector(int sector_no);
	public abstract boolean isReadOnly();
	public abstract byte [] getPercomBlock();
	
	static void setAssetManager(AssetManager am)
	{
		mAccessManager = am;
	}
	
	public static Disk openDisk(String file_path, int loader, int read_write) throws Exception
	{
		// read_write
		// 0 - keep as it is 
		// 1 - read only
		// 2 - read write
		Disk disk = null;
		if(file_path.toLowerCase(Locale.getDefault()).endsWith(FileSelector.ATR_SUFFIX))
		{
			disk = new DiskImage(file_path, read_write);
		}
		else if(file_path.toLowerCase(Locale.getDefault()).endsWith(FileSelector.XEX_SUFFIX) ||
				file_path.toLowerCase(Locale.getDefault()).endsWith(FileSelector.EXE_SUFFIX) ||
				file_path.toLowerCase(Locale.getDefault()).endsWith(FileSelector.COM_SUFFIX))
		{
			try
			{
				disk = new Executable(file_path, loader);
			}
			catch(DiskFormatException e)
			{
				disk = new Otherfile(file_path);
			}
		}
		else
		{
			disk = new Otherfile(file_path);
		}
		return disk;
	}
	
	public boolean processSIOCommand(int command, int aux1, int aux2, IDataFrameReader reader, IDataFrameWriter writer) throws IOException
	{
		boolean hmi_refresh_needed = false;
		switch(command)
		{
			case CMD_FORMAT:
			{
				writer.writeCommandAck();
				try
				{
					DiskInfo info;
					if(mPercom != null)
					{
						if(D) Log.d(TAG, "CMD_FORMAT with PERCOM="+Utilities.byteArray2String(mPercom));
						info = new DiskInfo(mPercom);
					}
					else
					{
						if(D) Log.d(TAG, "CMD_FORMAT SD");
						info = new DiskInfo(DiskInfo.SIZE_SD,SINGLE_DENSITY_SECTOR_SIZE);
					}
					format(info);
					if(mPercom != null)
					{
						mPercom = getPercomBlock();
					}
					DataBuffer buffer = new DataBuffer(bytesPerSector());
					buffer.clear(); // fills with zeros
					byte [] data = buffer.getData();
					data[0] = (byte)0xFF;
					data[1] = (byte)0xFF;
					writer.writeComplete();
					writer.writeDataFrame(buffer);
				}
				catch(Exception e)
				{
					writer.writeError();
					int bytes_per_sector = (mPercom!=null)?((mPercom[6] & 0xFF) * 0x100 + (mPercom[7] & 0xFF)):bytesPerSector();
					DataBuffer buffer = new DataBuffer(bytes_per_sector);
					buffer.clear(); // fills with zeros
					writer.writeDataFrame(buffer);
				}
				hmi_refresh_needed = true;
			}
			break;
			
			case CMD_FORMAT_ED:
			{
				writer.writeCommandAck();
				try
				{
					if(D) Log.d(TAG, "CMD_FORMAT ED");
					DiskInfo info = new DiskInfo(DiskInfo.SIZE_ED,SINGLE_DENSITY_SECTOR_SIZE);
					format(info);
					if(mPercom != null)
					{
						mPercom = getPercomBlock();
					}
					DataBuffer buffer = new DataBuffer(SINGLE_DENSITY_SECTOR_SIZE);
					buffer.clear(); // fills with zeros
					byte [] data = buffer.getData();
					data[0] = (byte)0xFF;
					data[1] = (byte)0xFF;
					writer.writeComplete();
					writer.writeDataFrame(buffer);
				}
				catch(Exception e)
				{
					writer.writeError();
					DataBuffer buffer = new DataBuffer(SINGLE_DENSITY_SECTOR_SIZE);
					buffer.clear(); // fills with zeros
					writer.writeDataFrame(buffer);
				}
				hmi_refresh_needed = true;
			}
			break;

			case CMD_READ_SECTOR:
			{
				int sector_no = aux1 + aux2 * 0x100;
			    if (isSectorInRange(sector_no)) 
			    {
			    	writer.writeCommandAck();
			    	try
			    	{
			    		DataBuffer buffer = readSector(sector_no);
			    		if(D) Log.d(TAG, "CMD_READ_SECTOR " + sector_no + " SIZE=" + buffer.getSize());
			    		writer.writeComplete();
			    		writer.writeDataFrame(buffer);
			    	}
			    	catch(IOException e)
			    	{
			    		writer.writeError();
			    	}
			    }
			    else
			    {
			    	writer.writeCommandNack();
		        }
			}
			break;
			
			case CMD_WRITE_SECTOR:
			case CMD_WRITE_SECTOR_WITH_VERIFY:
			{
				int sector_no = aux1 + aux2 * 0x100;
			    if (isSectorInRange(sector_no)) 
			    {
			    	writer.writeCommandAck(); 
			    	DataBuffer buffer = reader.readDataFrame(bytesPerSector(sector_no));
			    	if(buffer != null)
			    	{
			    		writer.writeDataAck();
			    		try
				    	{
			    			if(D) Log.d(TAG, "CMD_WRITE_SECTOR " + sector_no + " SIZE=" + buffer.getSize());
			    			writeSector(sector_no, buffer);
				    		writer.writeComplete();
				    	}
				    	catch(IOException e)
				    	{
				    		writer.writeError();
				    	}
			    	}
			    	else
			    	{
			    		writer.writeDataNack();
			    		writer.writeError(); // required due to the OS bug?
			    	}
			    }
			    else
			    {
			    	writer.writeCommandNack();
			    }
			}
			break;

			case CMD_GET_STATUS:
			{
				writer.writeCommandAck();
				DataBuffer status = getStatus();
				if(D) Log.d(TAG, "CMD_GET_STATUS");
				writer.writeComplete();
				writer.writeDataFrame(status);
			}
			break;
			
			case CMD_READ_PERCOM:
			{
				writer.writeCommandAck();
				if(D) Log.d(TAG, "CMD_READ_PERCOM");
				DataBuffer buffer = new DataBuffer(PERCOM_FRAME_SIZE);
				if(null == mPercom)
				{
					mPercom = getPercomBlock();
				}
				System.arraycopy(mPercom, 0, buffer.getData(), 0, buffer.getSize());
				writer.writeComplete();
				writer.writeDataFrame(buffer);
			}
			break;
			
			case CMD_WRITE_PERCOM:
			{
				writer.writeCommandAck();
				if(D) Log.d(TAG, "CMD_WRITE_PERCOM");
				DataBuffer buffer = reader.readDataFrame(PERCOM_FRAME_SIZE);
				if(buffer!=null)
				{
					mPercom = new byte[PERCOM_FRAME_SIZE];
					System.arraycopy(buffer.getData(), 0, mPercom, 0, buffer.getSize());
					writer.writeDataAck();
					writer.writeComplete();
				}
				else
				{
					writer.writeDataNack();
					writer.writeError(); // required due to the OS bug?
				}
			}
			break;
			
			case CMD_SPEED_POLL:
			{
				writer.writeCommandAck();
				if(D) Log.d(TAG, "CMD_SPEED_POLL");
				DataBuffer buffer = new DataBuffer(1);
				byte [] data = buffer.getData();
				data[0] = SPEED_BYTE;
				writer.writeComplete();
				writer.writeDataFrame(buffer);
			}
			break;
			
			case CMD_QUIT:
			{
				writer.writeCommandAck();
				if(D) Log.d(TAG, "CMD_QUIT");
				writer.writeComplete();
			}
			break;
			
			default:
			{
				if(D) Log.d(TAG, "UNKNOWN SIO COMMAND="+command+" AUX1="+aux1+" AUX2="+aux2);
				writer.writeCommandNack();
			}
			break;

		}
		return hmi_refresh_needed;
	}
	
	private DataBuffer getStatus()
	{
		DataBuffer buffer = new DataBuffer(4);
		byte [] data = buffer.getData();

//		Offset 	Description
//		$00 	Drive status
//			bit 0 	Command frame error
//			bit 1 	Checksum error (Data frame error).
//			bit 2 	Write Error (Operation error/FDC error)
//			bit 3 	Write protected
//			bit 4 	Motor is ON
//			bit 5 	Sector size (0=$80 1=$100)
//			bit 6 	Unused
//			bit 7 	Medium Density (MFM & $80 byte mode)
//		$01 	Inverted, contains WD2793 Status Register. Depends on command used prior status.
//			bit 0 	Controller busy
//			bit 1 	Data Request or Index (DRQ)
//			bit 2 	Data lost or track 0
//			bit 3 	CRC Error
//			bit 4 	Record not found
//			bit 5 	Record Type or Head Loaded
//			bit 6 	Write Protected (Always false upon read)
//			bit 7 	Not Ready (Disk Removed)
//		$02 	Timeout for format ($E0) - Time the drive will need to format a disk
//		$03 	Copy of WD2793 Master status register
		
		if(mPercom != null)
		{
			int sectors_per_track = (mPercom[2] & 0xFF) * 0x100 + (mPercom[3] & 0xFF);
			int bytes_per_sector  = (mPercom[6] & 0xFF) * 0x100 + (mPercom[7] & 0xFF);
			data[0] = (byte)(	(isReadOnly()?8:0) + // Write protected 
								((0x80 == bytes_per_sector)?0:0x20) + // Sector size (0=$80 1=$100) 
								(((0x80 == bytes_per_sector) && (0x1A == sectors_per_track))?0x80:0) // Medium Density (MFM & $80 byte mode)
								);
		}
		else
		{
			data[0] = (byte)(	(isReadOnly()?8:0) + // Write protected 
								((0x80 == bytesPerSector())?0:0x20) + // Sector size (0=$80 1=$100) 
								(((0x80 == bytesPerSector()) && (0x1A == sectorsPerTrack()))?0x80:0) // Medium Density (MFM & $80 byte mode)
								);
		}

		data[1] = (byte)0xFF; // WD2793 Status Register
	    data[2] = (byte)0xE0; // Timeout for format ($E0) - Time the drive will need to format a disk 
	    data[3] = (byte)0x00; // not used
		return buffer;
	}
	
	public int getLoaderOffset()
	{
		return 0;
	}
	
	private byte [] mPercom = null;
}
