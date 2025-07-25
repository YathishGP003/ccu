package a75f.io.renatus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {

    private Context context;
    private ArrayList<String> values;
    private ArrayList<Boolean> hasImage;
    private int selectedPosition = -1;
    final String AIROVERSE_PROD = "airoverse_prod";
    final String CARRIER_PROD = "carrier_prod";
    final String DAIKIN_PROD = "daikin_prod";

    public CustomSpinnerAdapter(Context context, int textViewResourceId, ArrayList<String> values, ArrayList<Boolean> hasImage) {
        super(context, textViewResourceId, values);
        this.context = context;
        this.values = values;
        this.hasImage = hasImage;
    }

    @Override
    public boolean isEnabled(int position) {
        if(position == 2 && values.get(position).contains("No Shared Schedule available"))
            return false;

        return position !=1 ;// As this option is a title for NamedSchedule
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = getCustomView(position, convertView, parent,true);
        int colorSelected = ContextCompat.getColor(context, R.color.zoneselection_gray);
        view.setBackgroundColor(colorSelected);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent){
        parent.setPadding(0, 5, 0, 3);
       return getCustomView(position,convertView,parent,false);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent, Boolean isGetView) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int colorSelected = ContextCompat.getColor(context, R.color.zoneselection_gray);
        int highlightColor;
        if(BuildConfig.BUILD_TYPE.equals(AIROVERSE_PROD)){
            highlightColor = ContextCompat.getColor(context, R.color.airoverse_secondary);
        } else if(BuildConfig.BUILD_TYPE.equals(CARRIER_PROD)){
            highlightColor = ContextCompat.getColor(context, R.color.carrier_75f_secondary);
        } else if(BuildConfig.BUILD_TYPE.equals(DAIKIN_PROD)){
            highlightColor = ContextCompat.getColor(context, R.color.daikin_75f_secondary);
        } else
            highlightColor = ContextCompat.getColor(context, R.color.renatus_75f_secondary);

        if (hasImage.get(position)) {
            View row = inflater.inflate(R.layout.custom_dropdown_item_with_image, parent, false);
            TextView textView = row.findViewById(R.id.textView);
            ImageView imageView = row.findViewById(R.id.imageView);
            textView.setText(values.get(position));
            imageView.setImageResource(R.drawable.image_right_arrow);

            if(position == selectedPosition){
                imageView.setImageDrawable(null);
                row.setBackgroundColor(highlightColor);
            }
            if(isGetView) {
                textView.setPadding(0, 0, 3, 1);
                textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                row.setPadding(0, 0, 4, 2);
            }
            return row;
        } else {
            View row = inflater.inflate(R.layout.custom_dropdown_item_text_only, parent, false);
            TextView textView = row.findViewById(R.id.textView);
            textView.setText(values.get(position));

            if(position == 1) {
                textView.setTextColor(Color.BLACK);
                textView.setEnabled(false);
            }
            if(position == selectedPosition){
                row.setBackgroundColor(highlightColor);
            }
            if(isGetView) {
                textView.setPadding(0, 0, 4, 0);
                textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
            }

            if(position == 2 && values.get(position).contains("No Shared Schedule available")){
                textView.setEnabled(false);
            }
            return row;
        }
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }
}

