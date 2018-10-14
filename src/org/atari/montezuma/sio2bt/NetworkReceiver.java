package org.atari.montezuma.sio2bt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkReceiver extends BroadcastReceiver
{
	SIOManager mSioManager;
	
	NetworkReceiver(SIOManager siomanager)
	{
		super();
		mSioManager = siomanager;
	}
	
	public void onReceive(Context context, Intent intent)
	{
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        mSioManager.setNetworkAccessActive(networkInfo != null);
	}
}
