package a75f.io.renatus.tuners;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.renatus.R;

public class PriorityArrayAdapter extends RecyclerView.Adapter<PriorityArrayAdapter.PriorityViewHolder> {
    Context context;
    ArrayList<HashMap> priorityArrayList;
    PriorityItemClickListener priorityItemClickListener;
    TunerUndoClickListener undoClickListener;
    String tunerGroupType;
    HashMap tunerItemSelected;

    public PriorityArrayAdapter(Context context, String tunerGroupType, ArrayList<HashMap> priorityArrayList, PriorityItemClickListener itemClickListener, TunerUndoClickListener undoClickListener, HashMap tunerItemSelected) {
        this.context = context;
        this.priorityArrayList = priorityArrayList;
        this.priorityItemClickListener = itemClickListener;
        this.tunerGroupType = tunerGroupType;
        this.tunerItemSelected = tunerItemSelected;
        this.undoClickListener = undoClickListener;
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
        double level = Double.parseDouble(priorityItem.get("level").toString());
        holder.textViewPriority.setText(String.valueOf((int)level));
        setBlackTextColor(holder.textViewName);
        holder.imgBtnTunerUndo.setVisibility(View.GONE);
        if (priorityMap != null && priorityMap.size() > 0) {
            if (priorityMap.get("level") != null) {
                if (priorityItem.get("newValue") != null && !priorityItem.get("newValue").toString().equals("")) {
                    holder.textViewCurrentValue.setText(priorityItem.get("newValue").toString());
                    if (getTunerValue(tunerItemSelected.get("id").toString()) != 0){
                        holder.textViewValue.setText(String.valueOf(getTunerValue(tunerItemSelected.get("id").toString())));
                    } else {
                        holder.textViewValue.setText("");
                    }
                    setOrangeTextColor(holder.textViewCurrentValue);
                    setOrangeTextColor(holder.textViewName);
                    setOrangeTextColor(holder.textViewValue);
                    holder.imgBtnTunerUndo.setVisibility(View.VISIBLE);
                } else {
                    if (tunerGroupType.contains("Module")) {
                        if (position == 7) {
                            holder.textViewName.setText(context.getText(R.string.txt_tunersModule));
                            if (priorityItem.get("newValue") != null) {
                                holder.textViewCurrentValue.setText(priorityMap.get("newValue").toString());
                                holder.textViewValue.setText(priorityMap.get("val").toString());
                            } else {
                                if (getTunerValue(tunerItemSelected.get("id").toString()) != 0){
                                    holder.textViewCurrentValue.setText("-");
                                    holder.imgBtnTunerUndo.setVisibility(View.VISIBLE);
                                    holder.textViewValue.setText(String.valueOf(getTunerValue(tunerItemSelected.get("id").toString())));
                                } else {
                                    holder.textViewCurrentValue.setText("-");
                                    holder.textViewValue.setText("");
                                }
                            }
                            setOrangeTextColor(holder.textViewCurrentValue);
                            setOrangeTextColor(holder.textViewName);
                            setOrangeTextColor(holder.textViewValue);
                        } else {
                            if (priorityItem.get("val") != null && !priorityItem.get("val").toString().equals("")) {
                                holder.textViewValue.setText(priorityItem.get("val").toString());
                            } else {
                                holder.textViewValue.setText("");
                            }
                            holder.textViewCurrentValue.setText("");
                            setBlackTextColor(holder.textViewName);
                            setBlackTextColor(holder.textViewValue);
                        }
                    } else if (tunerGroupType.contains("Zone")) {
                        if (position == 9) {
                            holder.textViewName.setText("Zone");
                            if (priorityItem.get("newValue") != null) {
                                holder.textViewCurrentValue.setText(priorityMap.get("newValue").toString());
                                holder.textViewValue.setText(priorityMap.get("val").toString());
                            } else {
                                if (getTunerValue(tunerItemSelected.get("id").toString()) != 0){
                                    holder.textViewCurrentValue.setText("-");
                                    holder.imgBtnTunerUndo.setVisibility(View.VISIBLE);
                                    holder.textViewValue.setText(String.valueOf(getTunerValue(tunerItemSelected.get("id").toString())));
                                } else {
                                    holder.textViewCurrentValue.setText("-");
                                    holder.textViewValue.setText("");
                                }
                            }
                            setOrangeTextColor(holder.textViewCurrentValue);
                            setOrangeTextColor(holder.textViewName);
                            setOrangeTextColor(holder.textViewValue);
                        } else {
                            if (priorityItem.get("val") != null) {
                                holder.textViewValue.setText(priorityItem.get("val").toString());
                            } else {
                                holder.textViewValue.setText("");
                            }
                            holder.textViewCurrentValue.setText("");
                            setBlackTextColor(holder.textViewName);
                            setBlackTextColor(holder.textViewValue);
                        }
                    } else if (tunerGroupType.contains("System")) {
                        if (position == 13) {
                            holder.textViewName.setText("System");
                            if (priorityItem.get("newValue") != null) {
                                holder.textViewCurrentValue.setText(priorityMap.get("newValue").toString());
                                holder.textViewValue.setText(priorityMap.get("val").toString());
                            } else {
                                if (getTunerValue(tunerItemSelected.get("id").toString()) != 0){
                                    holder.textViewCurrentValue.setText("-");
                                    holder.imgBtnTunerUndo.setVisibility(View.VISIBLE);
                                    holder.textViewValue.setText(String.valueOf(getTunerValue(tunerItemSelected.get("id").toString())));
                                } else {
                                    holder.textViewCurrentValue.setText("-");
                                    holder.textViewValue.setText("");
                                }
                            }
                            setOrangeTextColor(holder.textViewCurrentValue);
                            setOrangeTextColor(holder.textViewName);
                            setOrangeTextColor(holder.textViewValue);
                        } else {
                            if (priorityItem.get("val") != null && !priorityItem.get("val").toString().equals("")) {
                                holder.textViewValue.setText(priorityItem.get("val").toString());
                            } else {
                                holder.textViewValue.setText("");
                            }
                            holder.textViewCurrentValue.setText("");
                            setBlackTextColor(holder.textViewName);
                            setBlackTextColor(holder.textViewValue);
                        }
                    } else if (tunerGroupType.contains("Building")) {
                        if (position == 15) {
                            holder.textViewName.setText("Building");
                            if (priorityItem.get("newValue") != null) {
                                holder.textViewCurrentValue.setText(priorityMap.get("newValue").toString());
                                holder.textViewValue.setText(priorityMap.get("val").toString());
                            } else {
                                if (getTunerValue(tunerItemSelected.get("id").toString()) != 0){
                                    holder.textViewCurrentValue.setText("-");
                                    holder.imgBtnTunerUndo.setVisibility(View.VISIBLE);
                                    holder.textViewValue.setText(String.valueOf(getTunerValue(tunerItemSelected.get("id").toString())));
                                } else {
                                    holder.textViewCurrentValue.setText("-");
                                    holder.textViewValue.setText("");
                                }
                            }
                            setOrangeTextColor(holder.textViewCurrentValue);
                            setOrangeTextColor(holder.textViewName);
                            setOrangeTextColor(holder.textViewValue);
                        } else {
                            if (priorityItem.get("val") != null && !priorityItem.get("val").toString().equals("")) {
                                holder.textViewValue.setText(priorityItem.get("val").toString());
                            } else {
                                holder.textViewValue.setText("");
                            }
                            holder.textViewCurrentValue.setText("");
                            setBlackTextColor(holder.textViewName);
                            setBlackTextColor(holder.textViewValue);
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
            if (!holder.textViewCurrentValue.getText().toString().equals("-")) {
                HashMap priorityValMap = getPriorityLevelMap(priorityArrayList);
                undoClickListener.onUndoClick(tunerItemSelected);

                if (priorityValMap.containsKey("val")) {
                    holder.textViewCurrentValue.setText(String.valueOf(getTunerValue(tunerItemSelected.get("id").toString())));
                    setOrangeTextColor(holder.textViewCurrentValue);
                    holder.imgBtnTunerUndo.setVisibility(View.GONE);
                }
            } else {
                holder.imgBtnTunerUndo.setVisibility(View.GONE);
                tunerItemSelected.put("reset", true);
                tunerItemSelected.put("newValue", null);
                tunerItemSelected.put("newLevel", priorityMap.get("level").toString());
                undoClickListener.onUndoClick(tunerItemSelected);
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

    public double getTunerValue(String id) {
        int level = 17;
        if (tunerGroupType.equalsIgnoreCase("Building")){
            level = 16;
        } else if (tunerGroupType.equalsIgnoreCase("System")){
            level = 14;
        } else if (tunerGroupType.equalsIgnoreCase("Zone")){
            level = 10;
        }else if (tunerGroupType.equalsIgnoreCase("Module")){
            level = 8;
        }

        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList values = hayStack.readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null && valMap.get("level").toString().equals(String.valueOf(level))) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
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