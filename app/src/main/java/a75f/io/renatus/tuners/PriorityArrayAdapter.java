package a75f.io.renatus.tuners;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.renatus.R;

public class PriorityArrayAdapter extends RecyclerView.Adapter<PriorityArrayAdapter.PriorityViewHolder> {
    Context context;
    ArrayList<HashMap> priorityArrayList;
    PriorityItemClickListener priorityItemClickListener;
    String tunerGroupType;

    public PriorityArrayAdapter(Context context, String tunerGroupType, ArrayList<HashMap> priorityArrayList, PriorityItemClickListener itemClickListener) {
        this.context = context;
        this.priorityArrayList = priorityArrayList;
        this.priorityItemClickListener = itemClickListener;
        this.tunerGroupType = tunerGroupType;
    }

    @Override
    public PriorityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PriorityViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tunerpriority, parent, false));
    }

    @Override
    public void onBindViewHolder(final PriorityViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        final HashMap priorityItem = priorityArrayList.get(position);
        HashMap priorityMap = getPriorityLevelMap(priorityArrayList);

        holder.textViewCurrentValue.setOnClickListener(v -> priorityItemClickListener.priorityClicked(position));
        holder.textViewPriority.setText(priorityItem.get("level").toString());
        setBlackTextColor(holder.textViewName);
        holder.imgBtnTunerUndo.setVisibility(View.GONE);
        if (priorityMap != null && priorityMap.size() > 0) {
            if (priorityMap.get("level") != null) {
                if (priorityItem.get("newValue") != null && !priorityItem.get("newValue").toString().equals("")) {
                    holder.textViewCurrentValue.setText(priorityItem.get("newValue").toString());
                    setOrangeTextColor(holder.textViewCurrentValue);
                    setOrangeTextColor(holder.textViewName);
                    holder.imgBtnTunerUndo.setVisibility(View.VISIBLE);
                } else {
                    if (tunerGroupType.contains("Module")) {
                        if (position == 7) {
                            holder.textViewName.setText(context.getText(R.string.txt_tunersModule));
                            if (priorityItem.get("newValue") != null) {
                                holder.textViewCurrentValue.setText(priorityMap.get("newValue").toString());
                            } else {
                                if (Integer.parseInt(priorityMap.get("level").toString()) == position +1){
                                    holder.textViewCurrentValue.setText(priorityMap.get("val").toString());
                                }else {
                                    holder.textViewCurrentValue.setText("-");
                                }
                            }
                            holder.textViewValue.setText("");
                            setOrangeTextColor(holder.textViewCurrentValue);
                            setOrangeTextColor(holder.textViewName);
                        } else {
                            if (priorityItem.get("val") != null && !priorityItem.get("val").toString().equals("")) {
                                holder.textViewValue.setText(priorityItem.get("val").toString());
                            } else {
                                holder.textViewValue.setText("");
                            }
                            holder.textViewCurrentValue.setText("");
                            setBlackTextColor(holder.textViewName);
                        }
                    } else if (tunerGroupType.contains("Zone")) {
                        if (position == 9) {
                            holder.textViewName.setText("Zone");
                            if (priorityItem.get("newValue") != null) {
                                holder.textViewCurrentValue.setText(priorityMap.get("newValue").toString());
                            }else {
                                if (Integer.parseInt(priorityMap.get("level").toString()) == position +1){
                                    holder.textViewCurrentValue.setText(priorityMap.get("val").toString());
                                }else {
                                    holder.textViewCurrentValue.setText("-");
                                }
                            }
                            holder.textViewValue.setText("");
                            setOrangeTextColor(holder.textViewCurrentValue);
                            setOrangeTextColor(holder.textViewName);
                        } else {
                            if (priorityItem.get("val") != null) {
                                holder.textViewValue.setText(priorityItem.get("val").toString());
                            } else {
                                holder.textViewValue.setText("");
                            }
                            holder.textViewCurrentValue.setText("");
                            setBlackTextColor(holder.textViewName);
                        }
                    } else if (tunerGroupType.contains("System")) {
                        if (position == 13) {
                            holder.textViewName.setText("System");
                            if (priorityItem.get("newValue") != null) {
                                holder.textViewCurrentValue.setText(priorityMap.get("newValue").toString());
                            }else {
                                if (Integer.parseInt(priorityMap.get("level").toString()) == position +1){
                                    holder.textViewCurrentValue.setText(priorityMap.get("val").toString());
                                }else {
                                    holder.textViewCurrentValue.setText("-");
                                }
                            }
                            holder.textViewValue.setText("");
                            setOrangeTextColor(holder.textViewCurrentValue);
                            setOrangeTextColor(holder.textViewName);
                        } else {
                            if (priorityItem.get("val") != null && !priorityItem.get("val").toString().equals("")) {
                                holder.textViewValue.setText(priorityItem.get("val").toString());
                            } else {
                                holder.textViewValue.setText("");
                            }
                            holder.textViewCurrentValue.setText("");
                            setBlackTextColor(holder.textViewName);
                        }
                    } else if (tunerGroupType.contains("Building")) {
                        if (position == 15) {
                            holder.textViewName.setText("Building");
                            if (priorityItem.get("newValue") != null) {
                                holder.textViewCurrentValue.setText(priorityMap.get("newValue").toString());
                            } else {
                                if (Integer.parseInt(priorityMap.get("level").toString()) == position +1){
                                    holder.textViewCurrentValue.setText(priorityMap.get("val").toString());
                                } else {
                                    holder.textViewCurrentValue.setText("-");
                                }
                            }
                            holder.textViewValue.setText("");
                            setOrangeTextColor(holder.textViewCurrentValue);
                            setOrangeTextColor(holder.textViewName);
                        } else {
                            if (priorityItem.get("val") != null && !priorityItem.get("val").toString().equals("")) {
                                holder.textViewValue.setText(priorityItem.get("val").toString());
                            } else {
                                holder.textViewValue.setText("");
                            }
                            holder.textViewCurrentValue.setText("");
                            setBlackTextColor(holder.textViewName);
                        }
                    }
                }
            }
        }

        if (position == 7) {
            holder.textViewName.setText(context.getText(R.string.txt_tunersModule));
        } else if (position == 9) {
            holder.textViewName.setText("Zone");
        } else if (position == 13) {
            holder.textViewName.setText("System");
        } else if (position == 15) {
            holder.textViewName.setText("Building");
        } else if (position == 16) {
            holder.textViewName.setText("Default");
        } else {
            holder.textViewName.setText("");
            holder.textViewCurrentValue.setText("");
            holder.textViewValue.setText("");
            setBlackTextColor(holder.textViewName);
        }

        holder.imgBtnTunerUndo.setOnClickListener(v -> {
            final HashMap priorityValMap = getPriorityLevelMap(priorityArrayList);
            if (priorityValMap.containsKey("val")) {
                holder.textViewCurrentValue.setText(priorityValMap.get("val").toString());
                setOrangeTextColor(holder.textViewCurrentValue);
                holder.imgBtnTunerUndo.setVisibility(View.GONE);
            }
        });
    }

    private HashMap getPriorityLevelMap(ArrayList<HashMap> values) {

        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = values.get(l - 1);
                if (valMap.get("level") != null && valMap.get("val") != null) {
                    return valMap;
                }
            }
        }
        return null;
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
        ImageButton imgBtnTunerUndo;

        public PriorityViewHolder(View itemView) {
            super(itemView);
            textViewPriority = itemView.findViewById(R.id.textPriority);
            textViewValue = itemView.findViewById(R.id.textTunerValue);
            textViewCurrentValue = itemView.findViewById(R.id.textCurrentValue);
            textViewName = itemView.findViewById(R.id.textDefaultValue);
            imgBtnTunerUndo = itemView.findViewById(R.id.imgBtnTunerUndo);
        }
    }
}