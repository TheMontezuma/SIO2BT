package org.atari.montezuma.sio2bt;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, OnLongClickListener
{
    // Debugging
    private static final String TAG = "MainSIO2BTActivity";
    private static final boolean D = false;
    
    public static final String SIO2BT_PREFS = "SIO2BT";
    private static final String DISK_PATH = "DISK_PATH_";
    private static final String LOADER_OFFSET = "LOADER_OFFSET_";
    
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_DEVICE_NAME = 2;
    public static final int MESSAGE_TOAST = 3;
    public static final int MESSAGE_SIO_DEV_INFO_CHANGED = 4;
    public static final int MESSAGE_SIO_TIMER_MSG_OFFSET = 255;
    public static final int BLINKING_DELAY = 200;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int CHOOSE_FILE = 3;
    private static final int NEW_FILE = 4;

    // Layout Views
    private TextView mTitle;
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the service
    private BluetoothService mService = null;
    
    private TextView [] mDiskName;
    private TextView [] mDiskInfo;
    private ImageButton [] mDiskButton;
    private AnimationDrawable [] mDiskBlinkingAnimation;
    private AnimationDrawable mOtherDevicesBlinkingAnimation;
    private LinearLayout [] mDiskDescription;
    private ImageButton [] mDiskEjectButton;
    
    private ImageView mOtherDevicesIcon;
    private Button mButtonSwap;
    private ImageView mMenuIcon;
    
    private Menu mOptionsMenu;
    
    private SIOManager mSIOManager = SIOManager.createInstance(this);
    private NetworkReceiver mReceiver = null;
    
    private static final class MainActivityHandler extends Handler
    {
    	private final WeakReference<MainActivity> mActivity;
    	MainActivityHandler(MainActivity activity)
    	{
    		mActivity = new WeakReference<MainActivity>(activity);
    	}
    	public void handleMessage(Message msg)
    	{
    		MainActivity activity = mActivity.get();
    		if(activity != null)
    		{
    			activity.handleMessage(msg);
    		}
    	}
    }
    
    public void handleMessage(Message msg)
    {
        switch (msg.what)
        {
        case MESSAGE_STATE_CHANGE:
            if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
            switch (msg.arg1) {
            case BluetoothService.STATE_CONNECTED:
            	mTitle.setText(R.string.title_connected_to);
            	mTitle.append(" ");
            	mTitle.append(mConnectedDeviceName);
            	if(mOptionsMenu!=null)
            	{
            		MenuItem item = mOptionsMenu.findItem(R.id.scan);
            		item.setEnabled(true);
            		item.setTitle(R.string.disconnect);
            	}
                break;
            case BluetoothService.STATE_CONNECTING:
            	mTitle.setText(R.string.title_connecting);
                break;
            case BluetoothService.STATE_NONE:
            case BluetoothService.STATE_STARTED:
            	mTitle.setText(R.string.title_not_connected);
            	if(mOptionsMenu!=null)
            	{
            		MenuItem item = mOptionsMenu.findItem(R.id.scan);
            		item.setEnabled(true);
            		item.setTitle(R.string.connect);
            	}
            	break;
            }
            break;
        case MESSAGE_DEVICE_NAME:
            // save the connected device's name
        	mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
            Toast.makeText(getApplicationContext(),  getResources().getString(R.string.title_connected_to) + " " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
            break;
        case MESSAGE_TOAST:
            Toast.makeText(getApplicationContext(), msg.getData().getInt(TOAST), Toast.LENGTH_SHORT).show();
            break;
        case SIOManager.DISK_DRIVE_BASE_ID:
        	{
       			mHandler.removeMessages(SIOManager.DISK_DRIVE_BASE_ID+MESSAGE_SIO_TIMER_MSG_OFFSET);
       			if(!mDiskBlinkingAnimation[0].isRunning())
       			{
       				mDiskBlinkingAnimation[0].start();
       			}
       			mHandler.sendMessageDelayed(mHandler.obtainMessage(SIOManager.DISK_DRIVE_BASE_ID+MESSAGE_SIO_TIMER_MSG_OFFSET),BLINKING_DELAY);
        	}
        	break;
        case SIOManager.DISK_DRIVE_BASE_ID+1:
	    	{
	   			mHandler.removeMessages(SIOManager.DISK_DRIVE_BASE_ID+1+MESSAGE_SIO_TIMER_MSG_OFFSET);
	   			if(!mDiskBlinkingAnimation[1].isRunning())
	   			{
	   				mDiskBlinkingAnimation[1].start();
	   			}
	   			mHandler.sendMessageDelayed(mHandler.obtainMessage(SIOManager.DISK_DRIVE_BASE_ID+1+MESSAGE_SIO_TIMER_MSG_OFFSET),BLINKING_DELAY);
	    	}
        	break;
        case SIOManager.DISK_DRIVE_BASE_ID+2:
	    	{
	   			mHandler.removeMessages(SIOManager.DISK_DRIVE_BASE_ID+2+MESSAGE_SIO_TIMER_MSG_OFFSET);
	   			if(!mDiskBlinkingAnimation[2].isRunning())
	   			{
	   				mDiskBlinkingAnimation[2].start();
	   			}
	   			mHandler.sendMessageDelayed(mHandler.obtainMessage(SIOManager.DISK_DRIVE_BASE_ID+2+MESSAGE_SIO_TIMER_MSG_OFFSET),BLINKING_DELAY);
	    	}
    		break;
        case SIOManager.DISK_DRIVE_BASE_ID+3:
	    	{
	   			mHandler.removeMessages(SIOManager.DISK_DRIVE_BASE_ID+3+MESSAGE_SIO_TIMER_MSG_OFFSET);
	   			if(!mDiskBlinkingAnimation[3].isRunning())
	   			{
	   				mDiskBlinkingAnimation[3].start();
	   			}
	   			mHandler.sendMessageDelayed(mHandler.obtainMessage(SIOManager.DISK_DRIVE_BASE_ID+3+MESSAGE_SIO_TIMER_MSG_OFFSET),BLINKING_DELAY);
	    	}
    		break;
        case SIOManager.SMART_DEVICE:
	    	{
	   			mHandler.removeMessages(SIOManager.SMART_DEVICE+MESSAGE_SIO_TIMER_MSG_OFFSET);
	   			if(!mOtherDevicesBlinkingAnimation.isRunning())
	   			{
	   				mOtherDevicesBlinkingAnimation.start();
	   			}
	   			mHandler.sendMessageDelayed(mHandler.obtainMessage(SIOManager.SMART_DEVICE+MESSAGE_SIO_TIMER_MSG_OFFSET),BLINKING_DELAY);
	    	}
    		break;
        case SIOManager.NETWORKING_DEVICE:
	    	{
	   			mHandler.removeMessages(SIOManager.NETWORKING_DEVICE+MESSAGE_SIO_TIMER_MSG_OFFSET);
	   			if(!mOtherDevicesBlinkingAnimation.isRunning())
	   			{
	   				mOtherDevicesBlinkingAnimation.start();
	   			}
	   			mHandler.sendMessageDelayed(mHandler.obtainMessage(SIOManager.NETWORKING_DEVICE+MESSAGE_SIO_TIMER_MSG_OFFSET),BLINKING_DELAY);
	    	}
    		break;
        case SIOManager.DISK_DRIVE_BASE_ID+MESSAGE_SIO_TIMER_MSG_OFFSET:
           	{
       			mDiskBlinkingAnimation[0].stop();
       			mDiskBlinkingAnimation[0].start();
       			mDiskBlinkingAnimation[0].stop();
        	}
        	break;
        case SIOManager.DISK_DRIVE_BASE_ID+1+MESSAGE_SIO_TIMER_MSG_OFFSET:
	       	{
	   			mDiskBlinkingAnimation[1].stop();
	   			mDiskBlinkingAnimation[1].start();
	   			mDiskBlinkingAnimation[1].stop();
	    	}
	    	break;
        case SIOManager.DISK_DRIVE_BASE_ID+2+MESSAGE_SIO_TIMER_MSG_OFFSET:
	       	{
	   			mDiskBlinkingAnimation[2].stop();
	   			mDiskBlinkingAnimation[2].start();
	   			mDiskBlinkingAnimation[2].stop();
	    	}
	    	break;
        case SIOManager.DISK_DRIVE_BASE_ID+3+MESSAGE_SIO_TIMER_MSG_OFFSET:
	       	{
	   			mDiskBlinkingAnimation[3].stop();
	   			mDiskBlinkingAnimation[3].start();
	   			mDiskBlinkingAnimation[3].stop();
	    	}
	    	break;
        case SIOManager.SMART_DEVICE+MESSAGE_SIO_TIMER_MSG_OFFSET:
	       	{
	   			mOtherDevicesBlinkingAnimation.stop();
	   			mOtherDevicesBlinkingAnimation.start();
	   			mOtherDevicesBlinkingAnimation.stop();
	    	}
	        break;
        case SIOManager.NETWORKING_DEVICE+MESSAGE_SIO_TIMER_MSG_OFFSET:
	       	{
	   			mOtherDevicesBlinkingAnimation.stop();
	   			mOtherDevicesBlinkingAnimation.start();
	   			mOtherDevicesBlinkingAnimation.stop();
	    	}
	    	break;
        case MESSAGE_SIO_DEV_INFO_CHANGED:
        	{
	        	int devid = msg.arg1; 
	    		SIODevice device = mSIOManager.getDevice(devid);
	    		updateDeviceInfo(devid, device);
        	}
            break;
        }
    }

    private final Handler mHandler = new MainActivityHandler(this);
    
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        Disk.setAssetManager(getResources().getAssets());
        
        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        mDiskName = new TextView[SIOManager.SUPPORTED_DISK_COUNT];
        mDiskInfo = new TextView[SIOManager.SUPPORTED_DISK_COUNT];
        mDiskButton = new ImageButton[SIOManager.SUPPORTED_DISK_COUNT];
        mDiskDescription = new LinearLayout[SIOManager.SUPPORTED_DISK_COUNT];
        mDiskEjectButton = new ImageButton[SIOManager.SUPPORTED_DISK_COUNT];
	    mDiskBlinkingAnimation = new AnimationDrawable[SIOManager.SUPPORTED_DISK_COUNT];
	    mOtherDevicesBlinkingAnimation = new AnimationDrawable();
        
        LinearLayout[] disk_layout = new LinearLayout[SIOManager.SUPPORTED_DISK_COUNT];
        
        disk_layout[0] = (LinearLayout)findViewById(R.id.disklayout_0);
        disk_layout[1] = (LinearLayout)findViewById(R.id.disklayout_1);
        disk_layout[2] = (LinearLayout)findViewById(R.id.disklayout_2);
        disk_layout[3] = (LinearLayout)findViewById(R.id.disklayout_3);

		for(int disk_index=0 ; disk_index<SIOManager.SUPPORTED_DISK_COUNT ; disk_index++)
		{
			mDiskName[disk_index] = (TextView) disk_layout[disk_index].findViewById(R.id.disk_name);
			mDiskInfo[disk_index] = (TextView) disk_layout[disk_index].findViewById(R.id.disk_info);
			mDiskButton[disk_index] = (ImageButton) disk_layout[disk_index].findViewById(R.id.button_disk);
			mDiskDescription[disk_index] = (LinearLayout) disk_layout[disk_index].findViewById(R.id.description_disk);
			mDiskEjectButton[disk_index] = (ImageButton) disk_layout[disk_index].findViewById(R.id.button_eject);
			mDiskButton[disk_index].setOnClickListener(this);
			mDiskDescription[disk_index].setOnClickListener(this);
			mDiskEjectButton[disk_index].setOnClickListener(this);
			
			mDiskButton[disk_index].setOnLongClickListener(this);
			mDiskDescription[disk_index].setOnLongClickListener(this);
		}
		
		mDiskButton[0].setBackgroundResource(R.drawable.disk1_blinking);
		mDiskButton[1].setBackgroundResource(R.drawable.disk2_blinking);
		mDiskButton[2].setBackgroundResource(R.drawable.disk3_blinking);
		mDiskButton[3].setBackgroundResource(R.drawable.disk4_blinking);

		mDiskBlinkingAnimation[0] = (AnimationDrawable) mDiskButton[0].getBackground();
		mDiskBlinkingAnimation[1] = (AnimationDrawable) mDiskButton[1].getBackground();
		mDiskBlinkingAnimation[2] = (AnimationDrawable) mDiskButton[2].getBackground();
		mDiskBlinkingAnimation[3] = (AnimationDrawable) mDiskButton[3].getBackground();
		
		mOtherDevicesIcon = (ImageView) findViewById(R.id.network_icon);
		mOtherDevicesIcon.setBackgroundResource(R.drawable.network_blinking);
		mOtherDevicesBlinkingAnimation = (AnimationDrawable) mOtherDevicesIcon.getBackground();
		
		mButtonSwap = (Button) findViewById(R.id.buttonSwap);
        mButtonSwap.setOnClickListener(this);
        
        mMenuIcon = (ImageView) findViewById(R.id.menu_icon);
        mMenuIcon.setOnClickListener(this);
        
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String delay = sharedPref.getString(SettingsActivity.KEY_PREF_WRITE_DELAY, String.valueOf(mSIOManager.getDelay()));
        mSIOManager.setDelay(Integer.valueOf(delay));
    	boolean blinking = sharedPref.getBoolean(SettingsActivity.KEY_PREF_BLINKING, mSIOManager.getBlinking());
    	mSIOManager.setBlinking(blinking);
        boolean smart = sharedPref.getBoolean(SettingsActivity.KEY_PREF_SMART, mSIOManager.getSmartDeviceSupport());
    	mSIOManager.setSmartDeviceSupport(smart);
    	boolean networking = sharedPref.getBoolean(SettingsActivity.KEY_PREF_NETWORKING, mSIOManager.getNetworkingSupport());
    	mSIOManager.setNetworkingSupport(networking);
        String net_open_timeout = sharedPref.getString(SettingsActivity.KEY_PRE_NET_OPEN_TIMEOUT, String.valueOf(mSIOManager.getNetworkingOpenTimeout()));
        mSIOManager.setNetworkingOpenTimeout(Integer.valueOf(net_open_timeout));
        String net_read_write_timeout = sharedPref.getString(SettingsActivity.KEY_PREF_NET_READ_WRITE_TIMEOUT, String.valueOf(mSIOManager.getNetworkingReadWriteTimeout()));
        mSIOManager.setNetworkingReadWriteTimeout(Integer.valueOf(net_read_write_timeout));
    	
    	if(smart || networking)
    	{
    		mOtherDevicesIcon.setVisibility(View.VISIBLE);    		
    	}
    	else
    	{
    		mOtherDevicesIcon.setVisibility(View.GONE);
    	}
    	
        // Register BroadcastReceiver to track connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mReceiver = new NetworkReceiver(mSIOManager);
        this.registerReceiver(mReceiver, filter);
     }

    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupBluetoothService() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled())
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        else 
        {
            if (mService == null) setupBluetoothService();
        }
    }

    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
        
        SharedPreferences settings = getSharedPreferences(SIO2BT_PREFS, 0);
		for(int disk_index=0 ; disk_index<SIOManager.SUPPORTED_DISK_COUNT ; disk_index++)
		{
			if(null==mSIOManager.getDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index))
			{
				String file_path = settings.getString(DISK_PATH+disk_index, null);
				int loader_offset = settings.getInt(LOADER_OFFSET+disk_index, Executable.LOADER_DEFAULT_OFFSET);
				if(null!=file_path)
				{
					try
					{
	        			Disk disk = Disk.openDisk(file_path, loader_offset, DiskImage.ATR_WRITE_PROTECTION_DEFAULT);
	        			if(mSIOManager.installDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index, disk))
			    		{
			    			updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+disk_index, disk);
			    		}
					}
					catch(Exception e)
					{
						updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+disk_index, null);
					}
				}
				else
				{
					updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+disk_index, null);
				}
			}
		}
		
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	Boolean smart = sharedPref.getBoolean(SettingsActivity.KEY_PREF_SMART, mSIOManager.getSmartDeviceSupport());
    	Boolean networking = sharedPref.getBoolean(SettingsActivity.KEY_PREF_NETWORKING, mSIOManager.getNetworkingSupport());
    	
    	if(smart || networking)
    	{
    		mOtherDevicesIcon.setVisibility(View.VISIBLE);
    		mOtherDevicesBlinkingAnimation.start();
    		mOtherDevicesBlinkingAnimation.stop();
    	}
    	else
    	{
    		mOtherDevicesIcon.setVisibility(View.GONE);
    	}

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mService != null)
        {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mService.getState() == BluetoothService.STATE_NONE)
            {
              // Start the Bluetooth service
              mService.start();
              if(mOptionsMenu!=null)
              {
            	  mOptionsMenu.findItem(R.id.scan).setEnabled(false);
              }
              // Launch the DeviceListActivity to see devices and do scan
              Intent serverIntent = new Intent(this, DeviceListActivity.class);
              startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            }
            else if(mService.getState() != BluetoothService.STATE_CONNECTING && mOptionsMenu!=null)
        	{
            	// back from DeviceListActivity?
        		mOptionsMenu.findItem(R.id.scan).setEnabled(true);
        	}
        }
    }

    private void setupBluetoothService() {
        if(D) Log.d(TAG, "setupBluetoothService()");
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        // Initialize the Bluetooth Service to perform bluetooth connections
        mService = new BluetoothService(mHandler, mSIOManager, wl);
    }

    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
        
        SharedPreferences settings = getSharedPreferences(MainActivity.SIO2BT_PREFS, 0);
        SharedPreferences.Editor editor = settings.edit();
        for(int disk_index=0 ; disk_index<SIOManager.SUPPORTED_DISK_COUNT ; disk_index++)
        {
        	Disk disk = mSIOManager.getDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index);
        	if(disk != null)
        	{
	        	editor.putString(DISK_PATH+disk_index, disk.getPath());
	        	editor.putInt(LOADER_OFFSET+disk_index, disk.getLoaderOffset());
        	}
        	else
        	{
	        	editor.putString(DISK_PATH+disk_index, null);
	        	editor.putInt(LOADER_OFFSET+disk_index, 0);
        		
        	}
        }
        editor.commit();
    }

    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    public void onDestroy() {
        super.onDestroy();
        if (mReceiver != null)
        {
            this.unregisterReceiver(mReceiver);
        }
        // Stop the Bluetooth service
        if (mService != null) mService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
    public void onBackPressed()
    {
        moveTaskToBack(true);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode)
        {
        case NEW_FILE:
    	if (resultCode == Activity.RESULT_OK)
    	{
    		String file_name = data.getStringExtra(NewFileActivity.FILE_NAME_EXTRA);
	    	try
	    	{
	    		DiskImage.createEmptyDisk(file_name);
	            for(int disk_index=0 ; disk_index<SIOManager.SUPPORTED_DISK_COUNT ; disk_index++)
	            {
	            	Disk disk = mSIOManager.getDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index);
	            	if(disk == null)
	            	{
	            		disk = Disk.openDisk(file_name, Executable.LOADER_DEFAULT_OFFSET, DiskImage.ATR_WRITE_PROTECTION_DEFAULT);
	        			if(mSIOManager.installDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index, disk))
	            		{
	            			updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+disk_index, disk);
	            		}
	            		break;
	            	}
	            }
	    		Toast.makeText(this, file_name+" "+getResources().getString(R.string.disk_created), Toast.LENGTH_SHORT).show();
	    	}
	    	catch(Exception e)
	    	{
	    		Toast.makeText(this, R.string.disk_not_created, Toast.LENGTH_SHORT).show();
	    	}
        }
        break;
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK)
            {
                // Get the device MAC address
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK)
            {
                // Bluetooth is now enabled, so set up the service
                setupBluetoothService();
            }
            else
            {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        case CHOOSE_FILE:
        	if (resultCode == Activity.RESULT_OK)
        	{
        		try
        		{
        			int disk_index = data.getIntExtra(FileSelector.DISK_INT_EXTRA, 0);
        			String file_path = data.getStringExtra(FileSelector.FILE_SELECTED_STRING_EXTRA);
        			int loader_offset = data.getIntExtra(FileSelector.LOADER_OFFSET_INT_EXTRA, Executable.LOADER_DEFAULT_OFFSET);
        			int read_write = data.getIntExtra(FileSelector.ATR_READ_WRITE_INT_EXTRA, DiskImage.ATR_WRITE_PROTECTION_DEFAULT);
        			Disk disk = Disk.openDisk(file_path, loader_offset, read_write);
        			for(int index=0 ; index<SIOManager.SUPPORTED_DISK_COUNT ; index++)
        			{
        				Disk tmp_disk = mSIOManager.getDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+index);
        				if(tmp_disk!=null && tmp_disk.getPath().equals(disk.getPath()))
        				{
        					mSIOManager.uninstallDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+index);
        					updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+index, null);
        				}
        			}
        			if(mSIOManager.installDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index, disk))
            		{
            			updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+disk_index, disk);
            		}
            		else
            		{
            			Toast.makeText(this, R.string.notMounted, Toast.LENGTH_SHORT).show();	
            		}
        		}
        		catch(Exception e)
        		{
        			Toast.makeText(this, R.string.notMounted, Toast.LENGTH_SHORT).show();
        		}
        	}
        	break;
        case FileSelector.SELECT_LOADER:
        	if (resultCode == Activity.RESULT_OK)
        	{
        		try
        		{
	        		int disk_index = data.getIntExtra(FileSelector.DISK_INT_EXTRA, 0);
		        	int loader_offset = data.getIntExtra(FileSelector.LOADER_OFFSET_INT_EXTRA, Executable.LOADER_DEFAULT_OFFSET);
		        	
		        	Disk disk = mSIOManager.getDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index);
		        	if(disk != null)
		        	{
		        		String file_path = disk.getPath();
		        		mSIOManager.uninstallDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index);
		        		updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+disk_index, null);
	
		        		disk = Disk.openDisk(file_path, loader_offset, DiskImage.ATR_WRITE_PROTECTION_DEFAULT);
		    			if(mSIOManager.installDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index, disk))
		        		{
		        			updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+disk_index, disk);
		        		}
	            		else
	            		{
	            			Toast.makeText(this, R.string.notMounted, Toast.LENGTH_SHORT).show();	
	            		}
		        	}
        		}
        		catch(Exception e)
        		{
        			Toast.makeText(this, R.string.notMounted, Toast.LENGTH_SHORT).show();
        		}
        	}
        	break;
        case FileSelector.SELECT_READ_WRITE:
        	if (resultCode == Activity.RESULT_OK)
        	{
        		try
        		{
	        		int disk_index = data.getIntExtra(FileSelector.DISK_INT_EXTRA, 0);
		        	int read_write = data.getIntExtra(FileSelector.ATR_READ_WRITE_INT_EXTRA, DiskImage.ATR_WRITE_PROTECTION_DEFAULT);
		        	
		        	Disk disk = mSIOManager.getDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index);
		        	if(disk != null)
		        	{
		        		String file_path = disk.getPath();
		        		mSIOManager.uninstallDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index);
		        		updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+disk_index, null);
	
		        		disk = Disk.openDisk(file_path, Executable.LOADER_DEFAULT_OFFSET, read_write);
		    			if(mSIOManager.installDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index, disk))
		        		{
		        			updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+disk_index, disk);
		        		}
	            		else
	            		{
	            			Toast.makeText(this, R.string.notMounted, Toast.LENGTH_SHORT).show();	
	            		}
		        	}
        		}
        		catch(Exception e)
        		{
        			Toast.makeText(this, R.string.notMounted, Toast.LENGTH_SHORT).show();
        		}
        	}
        	break;
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        mOptionsMenu = menu;
        if(mService!=null)
        {
        	MenuItem item = mOptionsMenu.findItem(R.id.scan);
        	switch(mService.getState())
        	{
	        	case BluetoothService.STATE_CONNECTING:
	        		item.setEnabled(false);
	        		break;
	        	case BluetoothService.STATE_CONNECTED:
	        		item.setEnabled(true);
	        		item.setTitle(R.string.disconnect);
	        		break;
	        	default:
	        		break;
        	}
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.scan:
            if(mService!=null)
            {
            	switch(mService.getState())
            	{
            		case BluetoothService.STATE_NONE:	
            		case BluetoothService.STATE_STARTED:
    	            	// Launch the DeviceListActivity to see devices and do scan
    	                Intent serverIntent = new Intent(this, DeviceListActivity.class);
    	                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    	                item.setEnabled(false);
    	                break;
    	        	case BluetoothService.STATE_CONNECTED:
    	        		if(mService!=null)
    	        		{
    	        			mService.stop();
    	        			mService.start();
    	        			item.setEnabled(false);
    	        		}
    	        		break;
    	        	default:
    	        		break;
            	}
            }
            return true;
/*
        case R.id.search:
        {
			//Intent intent = new Intent(this, SettingsActivity.class);
			//startActivity(intent);
        	return true;
        }
        case R.id.favourites:
        {
			//Intent intent = new Intent(this, SettingsActivity.class);
			//startActivity(intent);
        	return true;
        }
*/
        case R.id.create_disk:
        {
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    		String currentDateandTime = sdf.format(new Date());
    		SharedPreferences settings = getSharedPreferences(MainActivity.SIO2BT_PREFS, 0);
    		String last_dir = settings.getString(FileSelector.SIO2BT_LAST_DIR, Environment.getExternalStorageDirectory().getPath());
    		String file_name = last_dir+"/disk_"+currentDateandTime+".atr";
			Intent intent = new Intent(this, NewFileActivity.class);
			intent.putExtra(NewFileActivity.FILE_NAME_EXTRA, file_name);
			startActivityForResult(intent, NEW_FILE);
        	return true;
        }
        case R.id.preferences:
        {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
        }
        case R.id.exit:
        {
            finish();
            return true;
        }
        }
        return false;
    }

	public void onClick(View v)
	{
		for(int disk_index=0 ; disk_index<SIOManager.SUPPORTED_DISK_COUNT ; disk_index++)
		{
			if(v==mDiskDescription[disk_index] || v==mDiskButton[disk_index] )
			{
				Intent intent = new Intent(this, FileSelector.class);
				intent.putExtra(FileSelector.DISK_INT_EXTRA, disk_index);
				Disk disk = mSIOManager.getDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index);
				if(disk!=null)
				{
					intent.putExtra(FileSelector.SIO2BT_LAST_DIR, disk.getParent());
					intent.putExtra(FileSelector.FILE_SELECTED_STRING_EXTRA, disk.getName());
				}
				startActivityForResult(intent, CHOOSE_FILE);
				return;
			}
			else if(v==mDiskEjectButton[disk_index])
			{
				mSIOManager.uninstallDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index);
				updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+disk_index, null);
        		return;
			}
		}
		if(v==mButtonSwap)
		{
			mSIOManager.swapDiskDevices();
			for(int disk_index=0 ; disk_index<SIOManager.SUPPORTED_DISK_COUNT ; disk_index++)
			{
				SIODevice device = mSIOManager.getDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index);
				updateDeviceInfo(SIOManager.DISK_DRIVE_BASE_ID+disk_index, device);
			}
			return;
		}
		else if(v==mMenuIcon)
		{
			openOptionsMenu();
		}
	}
	
	public boolean onLongClick(View v)
	{
		for(int disk_index=0 ; disk_index<SIOManager.SUPPORTED_DISK_COUNT ; disk_index++)
		{
			if(v==mDiskDescription[disk_index] || v==mDiskButton[disk_index] )
			{
				Disk disk = mSIOManager.getDiskDevice(SIOManager.DISK_DRIVE_BASE_ID+disk_index);
				if(disk != null)
				{
					if(disk.getClass() == DiskImage.class)
					{
						Intent intent = new Intent(this, ReadWriteSelector.class);
						intent.putExtra(FileSelector.DISK_INT_EXTRA, disk_index);
						startActivityForResult(intent, FileSelector.SELECT_READ_WRITE);				
						return true;
					}
					else if(disk.getClass() == Executable.class)
					{
						Intent intent = new Intent(this, LoaderSelector.class);
						intent.putExtra(FileSelector.DISK_INT_EXTRA, disk_index);
						startActivityForResult(intent, FileSelector.SELECT_LOADER);				
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private void updateDeviceInfo(int devid, SIODevice device)
	{
		if(devid>=SIOManager.DISK_DRIVE_BASE_ID && devid<(SIOManager.DISK_DRIVE_BASE_ID+SIOManager.SUPPORTED_DISK_COUNT) )
		{
			if(device!=null)
			{
				mDiskName[devid-SIOManager.DISK_DRIVE_BASE_ID].setText(device.getDisplayName());
				mDiskInfo[devid-SIOManager.DISK_DRIVE_BASE_ID].setText(device.getDescription());
			}
			else
			{
				mDiskName[devid-SIOManager.DISK_DRIVE_BASE_ID].setText(null);
				mDiskInfo[devid-SIOManager.DISK_DRIVE_BASE_ID].setText(null);				
			}
		}
	}
}