package org.atari.montezuma.sio2bt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import android.util.Log;

public class NetworkDevice extends SIODevice
{
	
    private static final String TAG = "Network";
    private static final boolean D = false;
	
	protected final static int CMD_GET_STATUS               = 0x53;
	protected final static int CMD_OPEN                     = 0x4F;
	protected final static int CMD_READ                     = 0x52;
	protected final static int CMD_WRITE                    = 0x50;
	protected final static int CMD_CLOSE                    = 0x43;
	
	static final int NETWORK_DEFAULT_TIMEOUT_4_OPEN = 5000; // 5 sec
	static final int NETWORK_DEFAULT_TIMEOUT_4_READ_WRITE = 1000; // 1 sec
	static final int MAX_CONNECTION_COUNT = 0x04;
	static final int PROTOCOL_RAW_TCP_IP = 0x00;
	static final int PROTOCOL_MASK = 0xFC;
	static final int CONNECTION_ID_MASK = 0x03;
	static final int STATUS_FRAME_SIZE = MAX_CONNECTION_COUNT+1;
	
	private boolean mNetworkAccessActive = false;
	private int mNetworkOpenTimeout = NETWORK_DEFAULT_TIMEOUT_4_OPEN;
	private int mNetworkReadWriteTimeout = NETWORK_DEFAULT_TIMEOUT_4_READ_WRITE;
	private byte [] mConnectionStatus = new byte[MAX_CONNECTION_COUNT];
	private Socket [] mClientSockets = new Socket[MAX_CONNECTION_COUNT];
	private InputStream [] mInputStream = new InputStream[MAX_CONNECTION_COUNT];
	private OutputStream [] mOutputStream = new OutputStream[MAX_CONNECTION_COUNT];
	
	NetworkDevice()
	{
		Arrays.fill(mConnectionStatus, 0, MAX_CONNECTION_COUNT, (byte)0);
	}
	
	public boolean processSIOCommand(int command, int aux1, int aux2,
			IDataFrameReader reader, IDataFrameWriter writer)
			throws IOException 
	{
		switch(command)
		{
			case CMD_GET_STATUS:
			{
				if(D) Log.d(TAG, "CMD_GET_STATUS");
				writer.writeCommandAck();
				try
				{
					DataBuffer buffer = new DataBuffer(STATUS_FRAME_SIZE);
					byte [] data = buffer.getData();
					
					System.arraycopy(mConnectionStatus, 0, data, 0, MAX_CONNECTION_COUNT);
					data[STATUS_FRAME_SIZE-1] = (byte)(mNetworkAccessActive?1:0);
					
					if(D) Log.d(TAG, "CMD_GET_STATUS " + (int)data[0] + " " + (int)data[1] + " " + (int)data[2] + " " + (int)data[3] + " " + (int)data[4]);
					
					writer.writeComplete();
					writer.writeDataFrame(buffer);
					for(int i=0 ; i<MAX_CONNECTION_COUNT;i++)
					{
						mConnectionStatus[i] &= (byte)0xFE; // reset error code
					}
				}
				catch(Exception e)
				{
					writer.writeError();
				}
			}
			break;

			case CMD_OPEN:
			{
				if(D) Log.d(TAG, "CMD_OPEN");
				int protocol = aux1 & PROTOCOL_MASK;
				if(protocol != PROTOCOL_RAW_TCP_IP)
				{
					writer.writeCommandNack();
					return false;
				}
				int connection_id = aux1 & CONNECTION_ID_MASK;
				writer.writeCommandAck();
				if(aux2==0)aux2=256;
				DataBuffer buffer = reader.readDataFrame(aux2);
				String server_address;
				int port;
				try
				{
					String tmp = new String(buffer.getData(),0,aux2);
					int pos = tmp.indexOf(':');
					server_address = tmp.substring(0, pos);
					String port_number = tmp.substring(pos+1,aux2);
					port = Integer.valueOf(port_number);
					
					if(D) Log.d(TAG, "CMD_OPEN "+server_address+":"+port);
					
					writer.writeDataAck();
				}
				catch(Exception e)
				{
					if(D) Log.d(TAG, "CMD_OPEN NACK");
					writer.writeDataNack();
					writer.writeError(); // needed due to the OS bug
					return false;
				}

				try
				{
					if(mClientSockets[connection_id]!=null)
					{
						if(D) Log.d(TAG, "CMD_OPEN close socket");
						mClientSockets[connection_id].close();
					}
					mClientSockets[connection_id] = new Socket();
					SocketAddress sa = new InetSocketAddress(server_address, port);
					mClientSockets[connection_id].setSoTimeout(mNetworkReadWriteTimeout);
					mClientSockets[connection_id].connect(sa, mNetworkOpenTimeout);
					mInputStream[connection_id] = mClientSockets[connection_id].getInputStream();
					mOutputStream[connection_id] = mClientSockets[connection_id].getOutputStream();
					if(D) Log.d(TAG, "CMD_OPEN success");
				}
				catch(Exception e)
				{
					if(D) Log.d(TAG, "CMD_OPEN exception "+e);
					mClientSockets[connection_id] = null;
					mInputStream[connection_id] = null;
					mOutputStream[connection_id] = null;
					mConnectionStatus[connection_id] |= (byte)0x01; // set error code
				}
				writer.writeComplete();
			}
			break;
			
			case CMD_READ:
			{
				if(D) Log.d(TAG, "CMD_READ");

				if(aux2!=0)
				{
					writer.writeCommandAck();
					
					int connection_id = aux1 & CONNECTION_ID_MASK;
					DataBuffer buffer = new DataBuffer(aux2+1);
					buffer.clear();
					byte [] data = buffer.getData();
					int bytes_read = 0;
					try
					{
						bytes_read = mInputStream[connection_id].read(data, 0, aux2);
						if(D) Log.d(TAG, "CMD_READ success");
					}
					catch(SocketTimeoutException e)
					{
						if(D) Log.d(TAG, "CMD_READ SocketTimeoutException");
						bytes_read = 0;
					}
					catch(Exception e)
					{
						if(D) Log.d(TAG, "CMD_READ exception");
						bytes_read = 0;
						mConnectionStatus[connection_id] |= (byte)0x01; // set error code
					}
					
					if(bytes_read==-1)
					{
						data[aux2] = (byte)0;
						mConnectionStatus[connection_id] |= (byte)0x01; // set error code
					}
					else
					{
						data[aux2] = (byte)bytes_read;
					}
					
					writer.writeComplete();
					writer.writeDataFrame(buffer);
				}
				else
				{
					writer.writeCommandNack();
				}
			}
			break;

			case CMD_WRITE:
			{
				if(D) Log.d(TAG, "CMD_WRITE");
				writer.writeCommandAck();

				int connection_id = aux1 & CONNECTION_ID_MASK;
				DataBuffer buffer = reader.readDataFrame(aux2);
				if(buffer!=null)
				{
					writer.writeDataAck();
					try
					{
						mOutputStream[connection_id].write(buffer.getData(), 0, aux2);
						if(D) Log.d(TAG, "CMD_WRITE success");
					}
					catch(Exception e)
					{
						if(D) Log.d(TAG, "CMD_WRITE exception");
						mConnectionStatus[connection_id] |= (byte)0x01; // set error code
					}
					writer.writeComplete();
				}
				else
				{
					if(D) Log.d(TAG, "CMD_WRITE DATA SIO ERROR");
					writer.writeDataNack();
					writer.writeError(); // needed due to the OS bug
				}
			}	
			break;
			
			case CMD_CLOSE:
			{
				if(D) Log.d(TAG, "CMD_CLOSE");
				writer.writeCommandAck();
				
				int connection_id = aux1 & CONNECTION_ID_MASK;
				mInputStream[connection_id] = null;
				mOutputStream[connection_id] = null;
				if(mClientSockets[connection_id]!=null)
				{
					try
					{
						mClientSockets[connection_id].close();
						if(D) Log.d(TAG, "CMD_CLOSE success");
					}
					catch(Exception e)
					{
						if(D) Log.d(TAG, "CMD_CLOSE exception");
						mConnectionStatus[connection_id] |= (byte)0x01; // set error code
					}
					mClientSockets[connection_id] = null;
				}
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
		return false;
	}

	public String getDisplayName()
	{
		return new String("Network Device");
	}

	public String getDescription()
	{
		return new String("Network Device");
	}

	public void finalize()
	{
		// close all connections
		for( Socket s: mClientSockets )
		{
			if(s != null)
			{
				try
				{
					s.close();
				}
				catch(IOException e){}
			}
		}
	}
	
	void setNetworkAccessActive(boolean status)
	{
		if(D) Log.d(TAG, "setNetworkAccessActive="+status);
		mNetworkAccessActive = status;
	}
	
	void setNetworkOpenTimeout(int timeout)
	{
		if(D) Log.d(TAG, "setNetworkOpenTimeout="+timeout);
		mNetworkOpenTimeout = timeout;
	}
	
	void setNetworkReadWriteTimeout(int timeout)
	{
		if(D) Log.d(TAG, "setNetworkReadWriteTimeout="+timeout);
		mNetworkReadWriteTimeout = timeout;
	}
}
