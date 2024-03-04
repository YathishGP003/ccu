package a75f.io.renatus.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import a75f.io.renatus.R;


public class CustomSpinnerDropDownAdapter<T> extends ArrayAdapter<T>  {

    public CustomSpinnerDropDownAdapter(Context context, int textViewResourceId, List<T> values) {
        super(context, textViewResourceId, values);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        parent.setPadding(0, 0, 0, 0);
        View view = super.getView(position, convertView, parent);
        view.setPadding(5, 0, 30, 0);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent){
        parent.setPadding(0, 7, 0, 5);
        return getCustomView(position,convertView,parent);
    }

    @SuppressLint("ResourceType")
    public View getCustomView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        view.setPadding(20, 0, 20, 0);
        view.setBackgroundResource(R.drawable.custmspinner);
        return view;
    }


}

