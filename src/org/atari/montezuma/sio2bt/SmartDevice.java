package org.atari.montezuma.sio2bt;

import java.io.IOException;
import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class SmartDevice extends SIODevice
{
    private static final String TAG = "Smart";
    private static final boolean D = false;
    
    Context mContext;
	
	protected final static int CMD_GET_TIME			= 0x93;
	protected final static int CMD_URL_SUBMISSION	= 0x55;
	protected final static int MAX_URL_LEN = 2000;
	
	SmartDevice(Context context)
	{
		mContext = context;
	}
	
	public boolean processSIOCommand(int command, int aux1, int aux2,
			IDataFrameReader reader, IDataFrameWriter writer)
			throws IOException 
	{
		switch(command)
		{
			case CMD_URL_SUBMISSION:
			{
				int size = aux1+(aux2<<8);
				if(D) Log.d(TAG, "CMD_URL_SUBMISSION SIZE="+size);
				
				String url;
				try
				{
					if(size==0 || size>MAX_URL_LEN)
					{
						throw new Exception("Request-URI wrong size");
					}
					writer.writeCommandAck();
					
					DataBuffer buffer = reader.readDataFrame(size);
					url = new String(buffer.getData(),0,size);
					
					if(D) Log.d(TAG, "CMD_URL_SUBMISSION "+url);
					writer.writeDataAck();
				}
				catch(Exception e)
				{
					if(D) Log.d(TAG, "CMD_URL_SUBMISSION NACK");
					writer.writeDataNack();
					writer.writeError(); // needed due to the OS bug
					return false;
				}
				
				writer.writeComplete();
				
				try
				{
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					mContext.startActivity(intent);
				}
				catch(Exception e)
				{
					if(D) Log.d(TAG, "CMD_URL_SUBMISSION "+e);
					return false;
				}
			}
			break;

			case CMD_GET_TIME:
			{
				if(D) Log.d(TAG, "CMD_TIME");
				writer.writeCommandAck();
				
				DataBuffer buffer = new DataBuffer(6);
				byte [] data = buffer.getData();

				//DDMMYYHHMMSS
				Calendar calendar = Calendar.getInstance();
				data[0] = (byte)(calendar.get(Calendar.DAY_OF_MONTH));
				data[1] = (byte)(calendar.get(Calendar.MONTH)+1); // JAN=1 ... DEC=12
				data[2] = (byte)(calendar.get(Calendar.YEAR) % 100);
				data[3] = (byte)(calendar.get(Calendar.HOUR_OF_DAY));
				data[4] = (byte)(calendar.get(Calendar.MINUTE));
				data[5] = (byte)(calendar.get(Calendar.SECOND));
				
				writer.writeComplete();
				writer.writeDataFrame(buffer);
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
		return new String("Smart Device");
	}

	public String getDescription()
	{
		return new String("Smart Device");
	}

	public void finalize()
	{
	}
}
