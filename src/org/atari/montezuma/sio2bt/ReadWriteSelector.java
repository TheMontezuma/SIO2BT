package org.atari.montezuma.sio2bt;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ReadWriteSelector extends ListActivity
{
	private int mFileForDisk = 0;
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mFileForDisk = getIntent().getIntExtra(FileSelector.DISK_INT_EXTRA, 0);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.device_name);
		adapter.add("R");
		adapter.add("RW");
		setListAdapter(adapter);
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent();
		intent.putExtra(FileSelector.DISK_INT_EXTRA, mFileForDisk);
		intent.putExtra(FileSelector.ATR_READ_WRITE_INT_EXTRA, position+1); // 1 - R, 2 - RW
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

}
