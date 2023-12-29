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

    public CustomSpinnerAdapter(Context context, int textViewResourceId, ArrayList<String> values, ArrayList<Boolean> hasImage) {
        super(context, textViewResourceId, values);
        this.context = context;
        this.values = values;
        this.hasImage = hasImage;
    }

    @Override
    public boolean isEnabled(int position) {
        return position !=1 ;// As this option is a title for NamedSchedule
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent){
       return getCustomView(position,convertView,parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int colorSelected = ContextCompat.getColor(context, R.color.zoneselection_gray);

        if (hasImage.get(position)) {
            View row = inflater.inflate(R.layout.custom_dropdown_item_with_image, parent, false);
            row.setBackgroundColor(colorSelected);
            TextView textView = row.findViewById(R.id.textView);
            ImageView imageView = row.findViewById(R.id.imageView);
            textView.setText(values.get(position));
            imageView.setImageResource(R.drawable.icon_arrow_right);

            if(position == selectedPosition)
                imageView.setImageDrawable(null);

            return row;
        } else {
            View row = inflater.inflate(R.layout.custom_dropdown_item_text_only, parent, false);
            row.setBackgroundColor(colorSelected);
            TextView textView = row.findViewById(R.id.textView);
            textView.setText(values.get(position));
            if(position == 2 && values.get(position).contains("No Named Schedule available")){
                textView.setEnabled(false);
            }
            if(position == 1) {
                textView.setTextColor(Color.BLACK);
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

