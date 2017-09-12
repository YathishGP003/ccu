package a75f.io.renatus;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by samjithsadasivan isOn 8/9/17.
 */

public class DataArrayAdapter<T> extends ArrayAdapter<T>
{
	private int     nSelectedPostion   = 0;
	private int[]   nSelectedPositions = null;
	private boolean bMultiSelect       = false;
	private int     nColor             = Color.argb(0x55, 0x00, 0x99, 0xcc);
	private int     nMultiColor        = Color.argb(0x33, 0x00, 0x99, 0xcc);
	
	
	public DataArrayAdapter(Context context, int textViewResourceId, List<T> objects)
	{
		super(context, textViewResourceId, objects);
		// TODO Auto-generated constructor stub
	}
	
	
	public DataArrayAdapter(Context context, int textViewResourceId, T[] objects)
	{
		super(context, textViewResourceId, objects);
		// TODO Auto-generated constructor stub
	}
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View v = super.getView(position, convertView, parent);
		if (!bMultiSelect)
		{
			if (position == nSelectedPostion)
			{
				v.setBackgroundColor(getContext().getResources().getColor(R.color.orange_multi));
			}
			//				v.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.selector));
			else
			{
				v.setBackgroundColor(Color.TRANSPARENT);
			}
		}
		else
		{
			if (nSelectedPositions[position] == 1)
			{
				v.setBackgroundColor(getContext().getResources().getColor(R.color.selection_gray));
			}
			//        		v.setBackgroundColor(getContext().getResources().getColor(R.color.accent));
			else
			{
				v.setBackgroundColor(Color.TRANSPARENT);
			}
		}
		return v;
	}
	
	
	public void setSelectedItem(int position)
	{
		nSelectedPostion = position;
		notifyDataSetChanged();
	}
	
	
	public void setMultiSelectMode(boolean bMultiSelect)
	{
		this.bMultiSelect = bMultiSelect;
		if (bMultiSelect)
		{
			nSelectedPositions = new int[this.getCount()];
		}
		else
		{
			nSelectedPositions = null;
		}
	}
	
	
	public void addSelected(int position)
	{
		if (nSelectedPositions != null)
		{
			nSelectedPositions[position] = 1;
			notifyDataSetChanged();
		}
	}
	
	
	public void removeSelected(int position)
	{
		if (nSelectedPositions != null)
		{
			nSelectedPositions[position] = 0;
			notifyDataSetChanged();
		}
	}
}
