package org.atari.montezuma.sio2bt;

import java.util.Locale;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LoaderSelector extends ListActivity
{
	private int mFileForDisk = 0;
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mFileForDisk = getIntent().getIntExtra(FileSelector.DISK_INT_EXTRA, 0);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.device_name); 
		for(int i=Executable.LOADER_MIN_OFFSET ; i<=Executable.LOADER_MAX_OFFSET ; i++)
		{
			boolean two_digits = i+5 > 0xF;
			String page = (two_digits?"$":"$0") + Integer.toHexString(i+5).toUpperCase(Locale.getDefault());
			adapter.add(page+"00-"+page+"F7");
		}
		setListAdapter(adapter);
	}
	

	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent();
		intent.putExtra(FileSelector.DISK_INT_EXTRA, mFileForDisk);
		intent.putExtra(FileSelector.LOADER_OFFSET_INT_EXTRA, position);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}
	
	

}
