package org.atari.montezuma.sio2bt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;


/**
 * SIOManager processes incoming data and dispatches Command Frames to the SIO devices.
 * It manages also the emulated SIO devices
 */

public class SIOManager implements IDataFrameReader, IDataFrameWriter
{
    private static final String TAG = "SIOManager";
    private static final boolean D = false;
	
	final static int DISK_DRIVE_BASE_ID = 0x31; // D1
	final static int SUPPORTED_DISK_COUNT = 4;
	final static int SMART_DEVICE = 0x45;
	final static int NETWORKING_DEVICE = 0x4E;
	final static int MAX_SUPPORTED_DEVICE_ID = 0xFF;
	final static int DEFAULT_WRITE_DELAY = 10;
	
	private final static int COMMAND_FRAME_SIZE = 5; // including checksum
	
	private int mWriteDelay = DEFAULT_WRITE_DELAY;
	private boolean mBlinking = true;
	private boolean mNetworkAccessActive = false;
	private int mNetworkOpenTimeout = NetworkDevice.NETWORK_DEFAULT_TIMEOUT_4_OPEN;
	private int mNetworkReadWriteTimeout = NetworkDevice.NETWORK_DEFAULT_TIMEOUT_4_READ_WRITE;
	
	private final static byte SIO_ACK      = (byte)0x41;
	private final static byte SIO_NACK     = (byte)0x4E;
	private final static byte SIO_COMPLETE = (byte)0x43;
	private final static byte SIO_ERROR    = (byte)0x45;
	
	private InputStream mIStream;
	private OutputStream mOStream;
	
	private static byte [] sCommandFrame = new byte[COMMAND_FRAME_SIZE]; // optimization
	
	private static SIOManager sSIOManager = null;
	private Context mContext;
	
	static SIOManager createInstance(Context context)
	{
		sSIOManager = new SIOManager(context);
		return sSIOManager;
	}
	
	static SIOManager getInstance()
	{
		return sSIOManager;
	}
	
	private SIOManager(Context context)
	{
		if(D) Log.d(TAG, "SIOManager");
		mSIODevices = new SIODevice[MAX_SUPPORTED_DEVICE_ID+1];
		mContext = context;
	}
	
	public void setDelay(int delay)
	{
		mWriteDelay = delay;
	}
	
	public int getDelay()
	{
		return mWriteDelay;
	}
	
	public void setBlinking(boolean blinking)
	{
		mBlinking = blinking;
	}
	
	public boolean getBlinking()
	{
		return mBlinking;
	}
	
	synchronized public void setSmartDeviceSupport(boolean smart)
	{
		if(smart)
		{
			if(mSIODevices[SMART_DEVICE] == null)
			{
				if(D) Log.d(TAG, "install Smart Device");
				mSIODevices[SMART_DEVICE] = new SmartDevice(mContext);
			}
		}
		else
		{
			if(mSIODevices[SMART_DEVICE] != null)
			{
				if(D) Log.d(TAG, "Uninstall Smart Device");
				mSIODevices[SMART_DEVICE].finalize();
				mSIODevices[SMART_DEVICE] = null;
			}
		}
	}
	
	synchronized public boolean getSmartDeviceSupport()
	{
		return (mSIODevices[SMART_DEVICE] != null);
	}
	
	synchronized public void setNetworkingSupport(boolean networking)
	{
		if(networking)
		{
			if(mSIODevices[NETWORKING_DEVICE] == null)
			{
				if(D) Log.d(TAG, "install Network Device");
				mSIODevices[NETWORKING_DEVICE] = new NetworkDevice();
				((NetworkDevice)mSIODevices[NETWORKING_DEVICE]).setNetworkAccessActive(mNetworkAccessActive);
				((NetworkDevice)mSIODevices[NETWORKING_DEVICE]).setNetworkOpenTimeout(mNetworkOpenTimeout);
				((NetworkDevice)mSIODevices[NETWORKING_DEVICE]).setNetworkReadWriteTimeout(mNetworkReadWriteTimeout);
			}
		}
		else
		{
			if(mSIODevices[NETWORKING_DEVICE] != null)
			{
				if(D) Log.d(TAG, "Uninstall Network Device");
				mSIODevices[NETWORKING_DEVICE].finalize();
				mSIODevices[NETWORKING_DEVICE] = null;
			}
		}
	}
	
	synchronized public boolean getNetworkingSupport()
	{
		return (mSIODevices[NETWORKING_DEVICE] != null);
	}
	
	synchronized public void setNetworkingOpenTimeout(int timeout)
	{
		mNetworkOpenTimeout = timeout;
		if(mSIODevices[NETWORKING_DEVICE] != null)
		{
			((NetworkDevice)mSIODevices[NETWORKING_DEVICE]).setNetworkOpenTimeout(mNetworkOpenTimeout);
		}
	}
	
	synchronized public int getNetworkingOpenTimeout()
	{
		return mNetworkOpenTimeout;
	}
	
	synchronized public void setNetworkingReadWriteTimeout(int timeout)
	{
		mNetworkReadWriteTimeout = timeout;
		if(mSIODevices[NETWORKING_DEVICE] != null)
		{
			((NetworkDevice)mSIODevices[NETWORKING_DEVICE]).setNetworkReadWriteTimeout(mNetworkReadWriteTimeout);
		}
	}
	
	synchronized public int getNetworkingReadWriteTimeout()
	{
		return mNetworkReadWriteTimeout;
	}
	
	synchronized public void setNetworkAccessActive(boolean status)
	{
		mNetworkAccessActive = status;
		if(mSIODevices[NETWORKING_DEVICE] != null)
		{
			((NetworkDevice)mSIODevices[NETWORKING_DEVICE]).setNetworkAccessActive(mNetworkAccessActive);
		}
	}

	synchronized public boolean installDiskDevice(int id, Disk disk)
	{
		if(D) Log.d(TAG, "installDisk with ID="+id);
		
		if(id<DISK_DRIVE_BASE_ID || id>=DISK_DRIVE_BASE_ID+SUPPORTED_DISK_COUNT) return false;
		
		for(int devid=DISK_DRIVE_BASE_ID ; devid<DISK_DRIVE_BASE_ID+SUPPORTED_DISK_COUNT ; devid++)
		{
			if(mSIODevices[devid]!=null && ((Disk)mSIODevices[devid]).getPath().equals(disk.getPath()))
			{
				return false;
			}
		}
		if(null != mSIODevices[id])
		{
			mSIODevices[id].finalize();
		}
		mSIODevices[id] = disk;
		return true;
	}
	
	synchronized public boolean uninstallDiskDevice(int id)
	{
		if(D) Log.d(TAG, "uninstallDisk with ID="+id);
		
		if(id<DISK_DRIVE_BASE_ID || id>=DISK_DRIVE_BASE_ID+SUPPORTED_DISK_COUNT) return false;
		if(null != mSIODevices[id])
		{
			mSIODevices[id].finalize();
			mSIODevices[id] = null;
		}
		return true;
	}

	synchronized public SIODevice getDevice(int id)
	{
		if(id<1 || id>MAX_SUPPORTED_DEVICE_ID) return null;
		return mSIODevices[id];
	}
	
	synchronized public Disk getDiskDevice(int id)
	{
		if(id<DISK_DRIVE_BASE_ID || id>=DISK_DRIVE_BASE_ID+SUPPORTED_DISK_COUNT) return null;
		return (Disk)mSIODevices[id];
	}
	
	synchronized public void swapDiskDevices()
	{
		if(D) Log.d(TAG, "swapDiskDevices");
		SIODevice tmp_dev = mSIODevices[DISK_DRIVE_BASE_ID];
		for(int i=0 ; i<SUPPORTED_DISK_COUNT-1 ; i++)
		{
			mSIODevices[DISK_DRIVE_BASE_ID+i] = mSIODevices[DISK_DRIVE_BASE_ID+i+1];
		}
		mSIODevices[DISK_DRIVE_BASE_ID+3] = tmp_dev;
	}
	
	public void sioLoop(Handler handler, BluetoothSocket socket) throws IOException
	{
		if(D) Log.d(TAG, "sioLoop");
		
		mIStream = socket.getInputStream();
		mOStream = socket.getOutputStream();
		
		while(true)
		{
			byte[] command_frame = readCommandFrame();
			int devid   = command_frame[0] & 0xFF;
			int command = command_frame[1] & 0xFF;
			int aux1    = command_frame[2] & 0xFF;
			int aux2    = command_frame[3] & 0xFF;
			
			synchronized (this)
			{
				if(null != mSIODevices[devid])
				{
					if(mBlinking)
					{
						switch(devid)
						{
							case DISK_DRIVE_BASE_ID:	
								handler.obtainMessage(DISK_DRIVE_BASE_ID).sendToTarget();
								break;
							case DISK_DRIVE_BASE_ID+1:	
								handler.obtainMessage(DISK_DRIVE_BASE_ID+1).sendToTarget();
								break;
							case DISK_DRIVE_BASE_ID+2:	
								handler.obtainMessage(DISK_DRIVE_BASE_ID+2).sendToTarget();
								break;
							case DISK_DRIVE_BASE_ID+3:	
								handler.obtainMessage(DISK_DRIVE_BASE_ID+3).sendToTarget();
								break;
							case SMART_DEVICE:
								handler.obtainMessage(SMART_DEVICE).sendToTarget();
								break;
							case NETWORKING_DEVICE:
								handler.obtainMessage(NETWORKING_DEVICE).sendToTarget();
								break;
						}
					}
					if(mSIODevices[devid].processSIOCommand(command, aux1, aux2, this, this))
					{
						// update HMI
						handler.obtainMessage(MainActivity.MESSAGE_SIO_DEV_INFO_CHANGED, devid, -1).sendToTarget();
					}
				}
			}
		}
	}
	
	private byte[] readCommandFrame() throws IOException
	{
		if(D) Log.d(TAG, "readCommandFrame");
		
		mIStream.skip(mIStream.available()); // purge bytes in the buffer
		
		int read_bytes = mIStream.read(sCommandFrame);
		while(read_bytes<COMMAND_FRAME_SIZE)
		{
			read_bytes += mIStream.read(sCommandFrame, read_bytes, COMMAND_FRAME_SIZE-read_bytes);
		}
		
		while(true)
		{
			int devid = sCommandFrame[0] & 0xFF;
			boolean devid_installed = false;
			
			synchronized (this)
			{
				devid_installed = (null != mSIODevices[devid]);
			}

			if(	devid_installed && (calculateChecksum(sCommandFrame, COMMAND_FRAME_SIZE-1) == sCommandFrame[COMMAND_FRAME_SIZE-1]) )
			{
				if(D) Log.d(TAG, "command_frame="+Utilities.byteArray2String(sCommandFrame));
				return sCommandFrame;
			}

			System.arraycopy(sCommandFrame, 1, sCommandFrame, 0, 4);
			sCommandFrame[4] = (byte)mIStream.read();
		}
	}
	
	private byte calculateChecksum(byte[] frame, int count)
	{
	    int sum = 0;
	    for (int i=0 ; i < count ; i++)
	    {
	        sum += (frame[i] & 0xFF);
	        if (sum > 255)
	        {
	            sum -= 255;
	        }
	    }
		return (byte)sum;
	}
	
	public DataBuffer readDataFrame(int size) throws IOException
	{
		if(D) Log.d(TAG, "readDataFrame");
		
		DataBuffer buffer = new DataBuffer(size);
		int size_with_checksum = size+1;
		
		int read_bytes = mIStream.read(buffer.getData(), 0, size_with_checksum); // read checksum too 
		while(read_bytes < size_with_checksum)
		{
			read_bytes += mIStream.read(buffer.getData(), read_bytes, size_with_checksum-read_bytes);
		}
		if(calculateChecksum(buffer.getData(), size) != buffer.getChecksum())
		{
			return null;
		}
		else
		{
			if(D) Log.d(TAG, "readDataFrame " + Utilities.dataBuffer2String(buffer));
			return buffer;
		}
	}
	
	public void writeDataFrame(DataBuffer buffer) throws IOException
	{
		if(D) Log.d(TAG, "writeDataFrame " + Utilities.dataBuffer2String(buffer));
		if(mWriteDelay!=0)
		{
			try
			{
				Thread.sleep(mWriteDelay);
			}
			catch(InterruptedException e){}
		}
		buffer.setChecksum(calculateChecksum(buffer.getData(), buffer.getSize()));
		mOStream.write(buffer.getData(),0,buffer.getSize()+1);
		mOStream.flush();
	}

	public void writeCommandAck() throws IOException
	{
		if(D) Log.d(TAG, "writeCommandAck");
		mOStream.write(SIO_ACK);
		mOStream.flush();
	}
	
	public void writeDataAck() throws IOException
	{
		if(D) Log.d(TAG, "writeDataAck");
		mOStream.write(SIO_ACK);
		mOStream.flush();
		
	}

	public void writeCommandNack() throws IOException
	{
		if(D) Log.d(TAG, "writeCommandNack");
		mOStream.write(SIO_NACK);
		mOStream.flush();
	}
	
	public void writeDataNack() throws IOException
	{
		if(D) Log.d(TAG, "writeDataNack");
		mOStream.write(SIO_NACK);
		mOStream.flush();
		
	}
	
	public void writeComplete() throws IOException
	{
		if(D) Log.d(TAG, "writeComplete");
		if(mWriteDelay!=0)
		{
			try
			{
				Thread.sleep(mWriteDelay);
			}
			catch(InterruptedException e){}
		}
		mOStream.write(SIO_COMPLETE);
		mOStream.flush();
	}
	
	public void writeError() throws IOException
	{
		if(D) Log.d(TAG, "writeError");
		if(mWriteDelay!=0)
		{
			try
			{
				Thread.sleep(mWriteDelay);
			}
			catch(InterruptedException e){}
		}
		mOStream.write(SIO_ERROR);
		mOStream.flush();
	}
	
	private SIODevice[] mSIODevices;
}
