package a75f.io.renatus.ui.register.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import java.io.Serializable;
import java.util.ArrayList;

import a75f.io.bo.kinvey.BuildingAddresses;
import a75f.io.bo.kinvey.CCUZones;
import a75f.io.renatus.R;

public class LoginAdapter extends BaseAdapter {
    private ArrayList<Serializable> data = new ArrayList<Serializable>();
    private Context context;

    public LoginAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        if(data == null)
            return 0;
        return data.size();
    }

    @Override
    public Serializable getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return -1;
    }

    public void updateData(ArrayList<Serializable> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.step_listitem_enviromenttext, parent, false);
        }

        TextView tv = convertView.findViewById(R.id.login_list_text);

        Serializable s = getItem(position);
        if (s instanceof CCUZones) {
            tv.setText(((CCUZones) s).getFloor_name());
        } else if (s instanceof BuildingAddresses) {
            tv.setText(((BuildingAddresses) s).getBuildingName());
        }

        return convertView;
    }
}
