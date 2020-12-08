package a75f.io.renatus.tuners;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.renatus.R;

public class PriorityArrayAdapter extends RecyclerView.Adapter<PriorityArrayAdapter.PriorityViewHolder> {
    Context context;
    ArrayList<HashMap> priorityArrayList;
    PriorityItemClickListener priorityItemClickListener;

    public PriorityArrayAdapter(Context context, ArrayList<HashMap> priorityArrayList, PriorityItemClickListener itemClickListener) {
        this.context = context;
        this.priorityArrayList = priorityArrayList;
        this.priorityItemClickListener = itemClickListener;
    }

    @Override
    public PriorityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PriorityViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tunerpriority, parent, false));
    }

    @Override
    public void onBindViewHolder(final PriorityViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        final HashMap priorityItem = priorityArrayList.get(position);
        Log.i("TunersUI", "priorityItem:" + priorityItem.toString());
        //priorityArrayList.add(position, priorityItem);
        holder.textViewCurrentValue.setOnClickListener(v -> priorityItemClickListener.priorityClicked(position));
        holder.textViewPriority.setText(priorityItem.get("level").toString());
        if (position == 9) {
            holder.textViewName.setText("Zone");
            setBlackTextColor(holder.textViewName);
            holder.textViewCurrentValue.setText("");
        } else if (position == 13) {
            holder.textViewName.setText("System");
            setOrangeTextColor(holder.textViewName);
            final HashMap systemPriority = priorityArrayList.get(16);
            if (systemPriority.containsKey("val")) {
                holder.textViewCurrentValue.setText("" + systemPriority.get("val"));
                setOrangeTextColor(holder.textViewCurrentValue);
                if (priorityItem.containsKey("newValue")) {
                    if (!priorityItem.get("newValue").toString().equals("")) {
                        holder.textViewCurrentValue.setText("" + priorityItem.get("newValue"));
                    }
                }
            }
        } else if (position == 15) {
            holder.textViewName.setText("Building");
            setBlackTextColor(holder.textViewName);
            holder.textViewCurrentValue.setText("");
        } else if (position == 16) {
            holder.textViewName.setText("Default");
            setBlackTextColor(holder.textViewName);
            if (priorityItem.containsKey("val")) {
                Log.i("TunersUI", "priorityItem:" + " val:" + priorityItem.get("val").toString());
                holder.textViewValue.setText("" + priorityItem.get("val"));
            }
        } else {
            holder.textViewName.setText("");
            holder.textViewCurrentValue.setText("");
            holder.textViewValue.setText("");
            setBlackTextColor(holder.textViewName);
        }

    }

    public void setOrangeTextColor(TextView textView) {
        textView.setTextColor(Color.parseColor("#E24301"));
    }

    public void setBlackTextColor(TextView textView) {
        textView.setTextColor(Color.parseColor("#000000"));
    }

    @Override
    public int getItemCount() {
        return priorityArrayList.size();
    }

    class PriorityViewHolder extends RecyclerView.ViewHolder {
        TextView textViewPriority;
        TextView textViewName;
        TextView textViewValue;
        TextView textViewCurrentValue;
        CheckBox checkBoxparent;

        public PriorityViewHolder(View itemView) {
            super(itemView);
            textViewPriority = itemView.findViewById(R.id.textPriority);
            textViewValue = itemView.findViewById(R.id.textTunerValue);
            textViewCurrentValue = itemView.findViewById(R.id.textCurrentValue);
            textViewName = itemView.findViewById(R.id.textDefaultValue);
            checkBoxparent = itemView.findViewById(R.id.tunerCheckbox);
        }

    }
}