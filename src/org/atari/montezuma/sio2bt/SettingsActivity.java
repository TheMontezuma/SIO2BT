package org.atari.montezuma.sio2bt;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	public static final String KEY_PREF_WRITE_DELAY = "WRITE_DELAY";
	public static final String KEY_PREF_BLINKING = "BLINKING";
	public static final String KEY_PREF_SMART = "SMART";
	public static final String KEY_PREF_NETWORKING = "NETWORKING";
	public static final String KEY_PRE_NET_OPEN_TIMEOUT = "NET_OPEN_TIMEOUT";
	public static final String KEY_PREF_NET_READ_WRITE_TIMEOUT = "NET_READ_WRITE_TIMEOUT";
	
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
    
    protected void onResume()
    {
        super.onResume();
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences(); 
        sp.registerOnSharedPreferenceChangeListener(this);
    	boolean networking = sp.getBoolean(KEY_PREF_NETWORKING, SIOManager.getInstance().getNetworkingSupport());
    	Preference p1 = findPreference(KEY_PRE_NET_OPEN_TIMEOUT);
    	p1.setEnabled(networking);
    	Preference p2 = findPreference(KEY_PREF_NET_READ_WRITE_TIMEOUT);
    	p2.setEnabled(networking);
    }

    protected void onPause()
    {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
        if(key.equals(KEY_PREF_WRITE_DELAY))
        {
        	String delay = sharedPreferences.getString(key, String.valueOf(SIOManager.getInstance().getDelay()));
        	SIOManager.getInstance().setDelay(Integer.valueOf(delay));
        }
        else if(key.equals(KEY_PREF_BLINKING))
        {
        	boolean blinking = sharedPreferences.getBoolean(key, SIOManager.getInstance().getBlinking());
        	SIOManager.getInstance().setBlinking(blinking);
        }
        else if(key.equals(KEY_PREF_SMART))
        {
        	boolean smart = sharedPreferences.getBoolean(key, SIOManager.getInstance().getSmartDeviceSupport());
        	SIOManager.getInstance().setSmartDeviceSupport(smart);
        }
        else if(key.equals(KEY_PREF_NETWORKING))
        {
        	boolean networking = sharedPreferences.getBoolean(key, SIOManager.getInstance().getNetworkingSupport());
        	SIOManager.getInstance().setNetworkingSupport(networking);
        	Preference p1 = findPreference(KEY_PRE_NET_OPEN_TIMEOUT);
        	p1.setEnabled(networking);
        	Preference p2 = findPreference(KEY_PREF_NET_READ_WRITE_TIMEOUT);
        	p2.setEnabled(networking);
        }
        else if(key.equals(KEY_PRE_NET_OPEN_TIMEOUT))
        {
        	String delay = sharedPreferences.getString(key, String.valueOf(SIOManager.getInstance().getNetworkingOpenTimeout()));
        	SIOManager.getInstance().setNetworkingOpenTimeout(Integer.valueOf(delay));
        }
        else if(key.equals(KEY_PREF_NET_READ_WRITE_TIMEOUT))
        {
        	String delay = sharedPreferences.getString(key, String.valueOf(SIOManager.getInstance().getNetworkingReadWriteTimeout()));
        	SIOManager.getInstance().setNetworkingReadWriteTimeout(Integer.valueOf(delay));
        }
	}
}