package a75f.io.renatus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import a75f.io.api.haystack.Alert;

public class AlertAdapter extends ArrayAdapter<Alert> implements View.OnClickListener{
    
    private ArrayList<Alert> alerts;
    Context mContext;
    
    // View lookup cache
    private static class ViewHolder {
        ImageView alertImg;
        TextView  alertTitle;
    }
    
    public AlertAdapter(ArrayList<Alert> alerts, Context context) {
        super(context, R.layout.alert_item, alerts);
        this.alerts = alerts;
        this.mContext=context;
        
    }

    public void resetList(ArrayList<Alert> alerts) {
        this.alerts = alerts;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        //display only first 60 alert
        return alerts.size() > 60 ? 60 : alerts.size();
    }

    @Override
    public void onClick(View v) {
        
        int position=(Integer) v.getTag();
        Object object= getItem(position);
        Alert dataModel=(Alert) object;
        //Show Alert
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
        Alert alert = getItem(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.alert_item, parent, false);
            viewHolder.alertImg = convertView.findViewById(R.id.alert_icon);
            viewHolder.alertTitle = convertView.findViewById(R.id.alert_title);
            
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        
        viewHolder.alertTitle.setText(alert.getmTitle());
        viewHolder.alertImg.setImageResource(alert.isFixed()?R.drawable.green_checkmark : getIcon(alert.mSeverity));
        
        return convertView;
    }

    private int getIcon(Alert.AlertSeverity mSeverity) {
        if (mSeverity.ordinal() == Alert.AlertSeverity.SEVERE.ordinal()
           || mSeverity.ordinal() == Alert.AlertSeverity.ERROR.ordinal()){
            return R.drawable.ic_severe;
        } else  if (mSeverity.ordinal() == Alert.AlertSeverity.MODERATE.ordinal()
                    || mSeverity.ordinal() == Alert.AlertSeverity.WARN.ordinal()){
            return R.drawable.ic_moderate;
        } if (mSeverity.ordinal() == Alert.AlertSeverity.LOW.ordinal()){
            return R.drawable.ic_low;
        }
         return R.drawable.ic_low;
    }
}
