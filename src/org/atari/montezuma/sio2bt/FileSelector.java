package org.atari.montezuma.sio2bt;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class FileSelector extends Activity implements OnItemClickListener, OnItemLongClickListener
{
	static final String SIO2BT_LAST_DIR = "LAST_DIR";
	static final String ATR_SUFFIX = ".atr";
	static final String XEX_SUFFIX = ".xex";
	static final String EXE_SUFFIX = ".exe";
	static final String COM_SUFFIX = ".com";
	static final String DISK_INT_EXTRA = "disk";
	static final String FILE_SELECTED_STRING_EXTRA = "fileSelected";
	static final String LOADER_OFFSET_INT_EXTRA = "offset";
	static final String ATR_READ_WRITE_INT_EXTRA = "readWrite";
	
	static final int SELECT_LOADER = 5;
	static final int SELECT_READ_WRITE = 6;
	
	private File mCurrentDir;
	private FileArrayAdapter mAdapter;
	private int mFileForDisk;
	private String mSelectedFilePath;
	private String mSelectedFileName;
	
	EditText mFileSearchBox;
	ListView mFileListView;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.file_search);
		mFileSearchBox=(EditText)findViewById(R.id.FileSearchBox);
		mFileListView=(ListView)findViewById(R.id.FileListView);
		mFileListView.setTextFilterEnabled(true);
		
		mFileSearchBox.addTextChangedListener(new TextWatcher() {
		    @Override
		    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		    	FileSelector.this.mAdapter.getFilter().filter(arg0);
		    }
		    @Override
		    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		    }
		    @Override
		    public void afterTextChanged(Editable arg0) {
		    }
		});
				
		mFileForDisk = getIntent().getIntExtra(DISK_INT_EXTRA, 0);
		
		mSelectedFileName = getIntent().getStringExtra(FILE_SELECTED_STRING_EXTRA);
		String last_dir = getIntent().getStringExtra(SIO2BT_LAST_DIR);
		
		if(last_dir == null)
		{
			SharedPreferences settings = getSharedPreferences(MainActivity.SIO2BT_PREFS, 0);
			last_dir = settings.getString(SIO2BT_LAST_DIR, Environment.getExternalStorageDirectory().getPath());
		}
		
		mCurrentDir = new File(last_dir);
		fill(mCurrentDir);
		
		mFileListView.setOnItemClickListener(this);
		mFileListView.setOnItemLongClickListener(this);
	}
	
	private void fill(File f)
	{
		SharedPreferences settings = getSharedPreferences(MainActivity.SIO2BT_PREFS, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SIO2BT_LAST_DIR, f.getPath());
        editor.commit();
        
		File[] dirs = f.listFiles();

		this.setTitle(f.getPath());
		
		TextView title = (TextView) findViewById(android.R.id.title);
		title.setEllipsize(TruncateAt.START);
		
		List<FileItem> dir = new ArrayList<FileItem>();
		List<FileItem> fls = new ArrayList<FileItem>();
		
		try
		{
			for (File file : dirs)
			{
				if (file.isDirectory() && !file.isHidden())
				{
					dir.add(new FileItem(file.getName(), getString(R.string.folder), file.getAbsolutePath(), true, false));
				}
				else
				{
					if (!file.isHidden())
					{
						fls.add(new FileItem(file.getName(), Utilities.humanReadableByteCount(file.length(),false), file.getAbsolutePath(), false, false));
					}
				}
			}
		} 
		catch (Exception e)
		{
		}
		Collections.sort(dir);
		Collections.sort(fls);
		dir.addAll(fls);

		if (f.getParentFile() != null)
		{
			dir.add(0, new FileItem("..", getString(R.string.parentDirectory), f.getParent(), false, true));
		}
		else
		{
			dir.add(0, new FileItem(getString(R.string.default_folder), getString(R.string.folder), Environment.getExternalStorageDirectory().getPath(), true, false));
		}

		mAdapter = new FileArrayAdapter(FileSelector.this, R.layout.file_view, dir);
		mFileListView.setAdapter(mAdapter);
		
		int index = 0;
		if(mSelectedFileName!=null && mSelectedFileName.length()!=0)
		{
			for (FileItem item : dir)
			{
				if(item.getName().equals(mSelectedFileName))
				{
					break;
				}
				index++;
			}
			
		}
		if(index!=dir.size())
		{
			mFileListView.setSelection(index);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		FileItem o = mAdapter.getItem(position);
		if (o.isFolder() || o.isParent())
		{			
			mCurrentDir = new File(o.getPath());
			fill(mCurrentDir);
		}
		else
		{
			mSelectedFilePath = o.getPath();
			
			Intent intent = new Intent();
			intent.putExtra(FILE_SELECTED_STRING_EXTRA, mSelectedFilePath);
			intent.putExtra(DISK_INT_EXTRA, mFileForDisk);
			intent.putExtra(LOADER_OFFSET_INT_EXTRA, Executable.LOADER_DEFAULT_OFFSET);
			intent.putExtra(ATR_READ_WRITE_INT_EXTRA, DiskImage.ATR_WRITE_PROTECTION_DEFAULT);
			setResult(Activity.RESULT_OK, intent);
			finish();
		}	
	}

	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
	{
		FileItem o = mAdapter.getItem(position);
		if (o.isFolder())
		{			
			return false; // click not consumed
		}
		else if (o.isParent())
		{
			mCurrentDir = new File(Environment.getExternalStorageDirectory().getPath());
			fill(mCurrentDir);
			return true;
		}
		else
		{
			if(o.getName().toLowerCase(Locale.getDefault()).endsWith(ATR_SUFFIX))
			{
				mSelectedFilePath = o.getPath();
				// get R/RW mode for ATR
				Intent intent = new Intent(this, ReadWriteSelector.class);
				intent.putExtra(DISK_INT_EXTRA, mFileForDisk);
				startActivityForResult(intent, SELECT_READ_WRITE);
				return true; // click consumed
			}
			else if(o.getName().toLowerCase(Locale.getDefault()).endsWith(XEX_SUFFIX) ||
					o.getName().toLowerCase(Locale.getDefault()).endsWith(EXE_SUFFIX) ||
					o.getName().toLowerCase(Locale.getDefault()).endsWith(COM_SUFFIX)
					)
			{
				mSelectedFilePath = o.getPath();
				try
				{
					new Executable(mSelectedFilePath, 0);
				}
				catch(Exception e)
				{
					return false;
				}

				// get offset for executable loader
				Intent intent = new Intent(this, LoaderSelector.class);
				intent.putExtra(DISK_INT_EXTRA, mFileForDisk);
				startActivityForResult(intent, SELECT_LOADER);
				return true; // click consumed
			}
			else
			{
				return false; // click not consumed
			}
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == Activity.RESULT_OK)
		{
			if(SELECT_LOADER == requestCode)
			{
				int loader_offset = data.getIntExtra(LOADER_OFFSET_INT_EXTRA, Executable.LOADER_DEFAULT_OFFSET);
				Intent intent = new Intent();
				intent.putExtra(FILE_SELECTED_STRING_EXTRA, mSelectedFilePath);
				intent.putExtra(DISK_INT_EXTRA, mFileForDisk);
				intent.putExtra(LOADER_OFFSET_INT_EXTRA, loader_offset);
				intent.putExtra(ATR_READ_WRITE_INT_EXTRA, DiskImage.ATR_WRITE_PROTECTION_DEFAULT);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
			else if(SELECT_READ_WRITE == requestCode)
			{
				// read_write
				// 0 - keep as it is 
				// 1 - read only
				// 2 - read write
				int read_write = data.getIntExtra(ATR_READ_WRITE_INT_EXTRA, DiskImage.ATR_WRITE_PROTECTION_DEFAULT);
				Intent intent = new Intent();
				intent.putExtra(FILE_SELECTED_STRING_EXTRA, mSelectedFilePath);
				intent.putExtra(DISK_INT_EXTRA, mFileForDisk);
				intent.putExtra(LOADER_OFFSET_INT_EXTRA, Executable.LOADER_DEFAULT_OFFSET);
				intent.putExtra(ATR_READ_WRITE_INT_EXTRA, read_write);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		}
	}
}