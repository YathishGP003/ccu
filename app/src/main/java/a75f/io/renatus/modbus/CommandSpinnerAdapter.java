package a75f.io.renatus.modbus;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import a75f.io.api.haystack.modbus.Command;
import a75f.io.renatus.R;

/**
 * Created by mahesh on 06-10-2020.
 */
class CommandSpinnerAdapter extends ArrayAdapter<Command> {

    LayoutInflater layoutInflater;

    public CommandSpinnerAdapter(Context context, int resouceId, List<Command> list) {

        super(context, resouceId, list);
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return rowView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {

        return rowView(position, convertView, parent);
    }

    private View rowView(int position, View convertView, ViewGroup parent) {
        Command rowItem = getItem(position);

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.spinner_item_orange, parent, false);
        }

        TextView txtTitle = (TextView) convertView.findViewById(R.id.spinnerTarget);
        txtTitle.setText(rowItem.getName());

        return convertView;
    }
}