package org.atari.montezuma.sio2bt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.atari.montezuma.sio2bt.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity
{
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    private static final String[] WELL_KNOWN_DEVICE_NAMES_PREFIXES = {"SIO2BT","ATARI"};
    public static HashSet<String> mDeviceNamesPrefixes;
	
    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    
    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private ArrayList<String> mPairedDevicesBluetoothAddresses;
    private ArrayList<String> mNewDevicesBluetoothAddresses;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });
        
	    mDeviceNamesPrefixes = new HashSet<String>();
    	for (String device_name_prefix : WELL_KNOWN_DEVICE_NAMES_PREFIXES)
		{
	    	mDeviceNamesPrefixes.add(device_name_prefix);
		}

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mPairedDevicesBluetoothAddresses = new ArrayList<String>();
        mNewDevicesBluetoothAddresses = new ArrayList<String>();

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0)
        {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices)
            {
            	String device_address = device.getAddress();
            	String device_name = device.getName();
            	
            	for (String device_name_prefix : mDeviceNamesPrefixes)
            	{
            		if(device_name.toUpperCase(Locale.getDefault()).startsWith(device_name_prefix))
                	{
                		mPairedDevicesArrayAdapter.add(device_name);
                		mPairedDevicesBluetoothAddresses.add(device_address);
                		break;
                	}
            	}
            }
        }
        else
        {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");
        
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            String info = ((TextView) v).getText().toString();
            
            if(info.equals(getResources().getText(R.string.none_found).toString()))
            {
	            // Set result and finish this Activity
            	Intent intent = new Intent();
	            setResult(Activity.RESULT_CANCELED, intent);            	
            }
            else
            {
            	String address = null;
            	
            	ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
            	if(av.equals(pairedListView))
            	{
            		if(position<mPairedDevicesBluetoothAddresses.size())
            		{
            			address = mPairedDevicesBluetoothAddresses.get(position);
            		}
            	}
            	else
            	{
            		if(position<mNewDevicesBluetoothAddresses.size())
            		{
            			address = mNewDevicesBluetoothAddresses.get(position);
            		}
            	}
            	
	            // Create the result Intent and include the MAC address
	            Intent intent = new Intent();
	            
	            if(address != null)
	            {
	            	intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
	            	setResult(Activity.RESULT_OK, intent);
	            }
	            else
	            {
	            	setResult(Activity.RESULT_CANCELED, intent);            
	            }
            }
	        finish();
        }
    };
    
    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public synchronized void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String device_address = device.getAddress();
                String device_name = device.getName();
                
                // If it's already paired, skip it, because it's been listed already
                if( 	(device.getBondState() != BluetoothDevice.BOND_BONDED) &&
                		device_name!=null
                		)
                {
                	if (D) Log.d(TAG, "ACTION_FOUND "+device_name);
                	
                	// workaround for double items
                	boolean new_device_found = true;
                	for(String item: mNewDevicesBluetoothAddresses)
                	{
                		if(device_address.equals(item))
                		{
                			new_device_found = false;
                			break;
                		}
                	}
                	if(new_device_found)
                	{
                    	for (String device_name_prefix : mDeviceNamesPrefixes)
                    	{
                    		if(device_name.toUpperCase(Locale.getDefault()).startsWith(device_name_prefix))
                        	{
                        		if(mNewDevicesBluetoothAddresses.isEmpty())
                        		{
                        			// workaround for ACTION_FOUND coming after ACTION_DISCOVERY_FINISHED
                        			mNewDevicesArrayAdapter.clear(); // remove "No devices found" text
                        		}
        	                    mNewDevicesArrayAdapter.add(device_name);
        	                    mNewDevicesBluetoothAddresses.add(device_address);
                    			break;
                        	}
                    	}
                	}
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
            	if (D) Log.d(TAG, "ACTION_DISCOVERY_FINISHED");
            	// When discovery is finished, change the Activity title
            	setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0)
                {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

}
