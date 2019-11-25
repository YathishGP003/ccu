package a75f.io.renatus;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
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
	private ArrayList<T> objects;


	public DataArrayAdapter(Context context, int textViewResourceId, ArrayList<T> objects)
	{
		super(context, textViewResourceId, objects);
		this.objects = objects;
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
		//convertView = super.getView(position, convertView, parent);
		if(convertView == null){
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.listviewitem, parent, false);
		}
		TextView textView_Data = (TextView) convertView.findViewById(R.id.textData);
		textView_Data.setText(objects.get(position).toString());
		if (!bMultiSelect)
		{
			if(position == -1)
			{
				convertView.setBackground(getContext().getResources().getDrawable(R.drawable.ic_listselector));
				textView_Data.setTextColor(Color.WHITE);
				convertView.setSelected(true);
			}
			if (position == nSelectedPostion)
			{
				//v.setBackgroundColor(getContext().getResources().getColor(R.color.orange_multi));
				convertView.setBackground(getContext().getResources().getDrawable(R.drawable.ic_listselector));
				textView_Data.setTextColor(Color.WHITE);
				convertView.setSelected(true);
			}
			//				v.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.selector));
			else
			{
				//v.setBackgroundColor(Color.TRANSPARENT);
				textView_Data.setTextColor(getContext().getResources().getColor(R.color.text_color));
				convertView.setBackgroundColor(Color.WHITE);
				convertView.setSelected(false);
			}
		}
		else
		{
			if (nSelectedPositions[position] == 1)
			{
				//v.setBackgroundColor(getContext().getResources().getColor(R.color.selection_gray));
				textView_Data.setTextColor(getContext().getResources().getColor(R.color.text_color));
				convertView.setBackgroundColor(getContext().getResources().getColor(R.color.selection_gray));
			}
			//        		v.setBackgroundColor(getContext().getResources().getColor(R.color.accent));
			else
			{
				//v.setBackgroundColor(Color.TRANSPARENT);
				textView_Data.setTextColor(getContext().getResources().getColor(R.color.text_color));
				convertView.setBackgroundColor(Color.WHITE);
			}
		}
		return convertView;
	}
	
	
	public void setSelectedItem(int position)
	{
		nSelectedPostion = position;
		notifyDataSetChanged();
	}
	
	
	public int getSelectedPostion()
	{
		return nSelectedPostion;
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
