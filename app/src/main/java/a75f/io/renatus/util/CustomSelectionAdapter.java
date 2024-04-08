package a75f.io.renatus.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import a75f.io.renatus.R;

public class CustomSelectionAdapter<T> extends ArrayAdapter<T> {
    private Context context;
    private List<T> values;
    public CustomSelectionAdapter(Context context, int textViewResourceId, List<T> values) {
        super(context, textViewResourceId, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent, false);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent){
        return getCustomView(position,convertView,parent, true);
    }

    @SuppressLint("ResourceType")
    public View getCustomView(int position, View convertView, ViewGroup parent, Boolean isDropDownView) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(position == 0){
            parent.setPadding(0, 7, 0, 5);
        }
        View row;
        if(isDropDownView) {
            row = inflater.inflate(R.layout.custom_textview, parent, false);
            TextView version = row.findViewById(R.id.textViewVersion);
            TextView typeName = row.findViewById(R.id.textView);
            String item = values.get(position).toString();
            if (item.contains(" ") && position > 0) {
                version.setText(item.substring(0, item.indexOf(" ")));
                 String typeString = item.substring(item.indexOf(" ") + 1);
                typeName.setTextColor(Color.parseColor("#21C073"));
                if(typeString.contains("Deprecated")){
                    typeName.setTextColor(Color.parseColor("#999999"));
                    typeName.setPadding(25, 0, 5, 0);
                }
                typeName.setText(typeString);
            } else {
                version.setText(item);
                typeName.setText("");
            }
            row.setPadding(10, 0, 10, 0);
            row.setBackgroundResource(R.drawable.custmspinner);
        } else {
            row = inflater.inflate(R.layout.custom_spinner_view, parent, false);
            TextView textView = row.findViewById(R.id.spinnerTextView);
            String item = values.get(position).toString();
            if (item.contains(" ") && position > 0) {
                textView.setText(item.substring(0, item.indexOf(" ")));
            } else {
                textView.setText(item);
            }
            row.setPadding(5, 0, 30, 0);
        }
        return row;
    }


}