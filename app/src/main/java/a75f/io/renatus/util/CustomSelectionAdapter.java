package a75f.io.renatus.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import a75f.io.renatus.R;

public class CustomSelectionAdapter<T> extends ArrayAdapter<T> {
    private Context context;
    private int configuredIndex;
    private boolean isIpAddressSpinner;
    private List<T> values;
    public CustomSelectionAdapter(Context context, int textViewResourceId, List<T> values, boolean isIpAddressSpinner) {
        super(context, textViewResourceId, values);
        this.context = context;
        this.values = values;
        this.isIpAddressSpinner = isIpAddressSpinner;
        this.configuredIndex = 2; // default value mapped to Normal
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
        if (isIpAddressSpinner) {
            String item = values.get(position).toString();
            if (isDropDownView) {
                row = inflater.inflate(R.layout.custom_spinner_view, parent, false);
                TextView ipAddress = row.findViewById(R.id.spinnerFirstTextView);
                TextView typeName = row.findViewById(R.id.spinnerSecondTextView);
                if (item.contains("(") && item.contains(")")) {
                    // Split the item into IP address and network type
                    String[] parts = item.split(" \\(");
                    String ip = parts[0];
                    String type = "(" + parts[1];

                    // Set the IP address and type to respective TextViews
                    ipAddress.setText(ip.trim());
                    typeName.setText(type.trim());
                } else {
                    // In case the item is not formatted as expected
                    ipAddress.setText(item);
                    typeName.setText("");
                }
                row.setPadding(10, 0, 10, 0);
                row.setBackgroundResource(R.drawable.custmspinner);
            } else {
                row = inflater.inflate(R.layout.custom_dropdown_item_text_only, parent, false);
                TextView textView = row.findViewById(R.id.textView);
                if (item.contains("(") && item.contains(")")) {
                    String[] parts = item.split(" \\(");
                    String ip = parts[0]; // The IP address part

                    // Create a SpannableString to apply different colors
                    SpannableString spannable = new SpannableString(item);
                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ip.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new ForegroundColorSpan(Color.GRAY), ip.length(), item.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    textView.setText(spannable);
                } else {
                    textView.setText(item);
                }
                textView.setTextSize(20);
                textView.setMaxLines(1);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                row.setPadding(5, 0, 30, 0);
            }
        } else {
            if (isDropDownView) {
                row = inflater.inflate(R.layout.custom_textview, parent, false);
                TextView version = row.findViewById(R.id.textViewVersion);
                TextView typeName = row.findViewById(R.id.textView);
                String item = values.get(position).toString();
                version.setText(item);
                version.setTextColor(Color.BLACK);
                if (position == configuredIndex) {
                    GradientDrawable roundedBackground = new GradientDrawable();
                    roundedBackground.setShape(GradientDrawable.RECTANGLE);
                    roundedBackground.setColor(CCUUiUtil.getPrimaryColor()); // Set your background color
                    roundedBackground.setCornerRadius(6f);
                    typeName.setBackground(roundedBackground);
                    typeName.setVisibility(View.VISIBLE);
                } else {
                    typeName.setVisibility(View.INVISIBLE);
                }
                row.setPadding(10, 0, 10, 0);
                row.setBackgroundResource(R.drawable.custmspinner);
            } else {
                row = inflater.inflate(R.layout.custom_dropdown_item_text_only, parent, false);
                TextView textView = row.findViewById(R.id.textView);
                String item = values.get(position).toString();
                textView.setText(item);
                textView.setTextSize(21);
                textView.setTextColor(Color.BLACK);
                row.setPadding(5, 0, 30, 0);
            }
        }
        return row;
    }

    public void setConfiguredIndex(int index){
        this.configuredIndex = index;
    }

    public int getConfiguredIndex(){
        return configuredIndex;
    }

}