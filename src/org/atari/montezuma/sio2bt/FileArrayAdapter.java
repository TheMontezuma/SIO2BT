package org.atari.montezuma.sio2bt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.atari.montezuma.sio2bt.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

public class FileArrayAdapter extends ArrayAdapter<FileItem> {

	private Context c;
	private int id;
	private List<FileItem> originalItems;
	private List<FileItem> items;

	public FileArrayAdapter(Context context, int textViewResourceId, List<FileItem> objects)
	{
		super(context, textViewResourceId, objects);
		c = context;
		id = textViewResourceId;
		items = objects;
	}

	public FileItem getItem(int i)
	{
		return items.get(i);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View v = convertView;
		if (v == null)
		{
			LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(id, null);
		}
		
		final FileItem o = items.get(position);
		if (o != null)
		{
			ImageView im = (ImageView) v.findViewById(R.id.img1);
			TextView t1 = (TextView) v.findViewById(R.id.TextView01);
			TextView t2 = (TextView) v.findViewById(R.id.TextView02);
			
			if(o.isFolder())
			{
				im.setImageResource(R.drawable.folder);
			}
			else if (o.isParent())
			{
				im.setImageResource(R.drawable.up);
			}
			else
			{
				im.setImageResource(R.drawable.disk);
			}

			t1.setText(o.getName());
			t2.setText(o.getData());				
		}
		return v;
	}
	
	@Override
	public Filter getFilter() {
	    return new Filter() {

	        @Override
	        protected FilterResults performFiltering(CharSequence constraint) {
	        	FilterResults results = new FilterResults();
	        	List<FileItem> filtered =  new ArrayList<FileItem>();
	        	if(originalItems==null)
	        	{
	        		originalItems = new ArrayList<FileItem>(items);
	        	}
	        	for(FileItem item : originalItems)
	        	{
	        		if(item.getName().toLowerCase(Locale.getDefault()).contains(constraint.toString().toLowerCase(Locale.getDefault())))
	        		{
	        			filtered.add(item);
	        		}
	        	}
	        	results.values = filtered;
	        	results.count = filtered.size();
	            return results;
	        }

	        @SuppressWarnings("unchecked")
			@Override
	        protected void publishResults(CharSequence constraint, FilterResults results) {
	        	items.clear();
	        	items.addAll((List<FileItem>) results.values);
	        	if(results.count==0)
	        	{
	        		notifyDataSetInvalidated();
	        	}
	        	else
	        	{
	        		notifyDataSetChanged();
	        	}
	        }
	    };
	}

}
