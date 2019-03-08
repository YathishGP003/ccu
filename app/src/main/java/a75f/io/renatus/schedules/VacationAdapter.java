package a75f.io.renatus.schedules;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import a75f.io.api.haystack.Schedule;
import a75f.io.renatus.R;

public class VacationAdapter extends RecyclerView.Adapter<VacationAdapter.ViewHolder> {

    private List<Schedule> mSchedules;

    public VacationAdapter(List<Schedule> schedules)
    {
        mSchedules = schedules;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public VacationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View scheduleView = inflater.inflate(R.layout.item_vacation_schedule, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(scheduleView);
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(VacationAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Schedule schedule = mSchedules.get(position);

        // Set item views based on your views and data model

        viewHolder.mVacationName.setText(schedule.getDis());
        viewHolder.mEndDate.setText(schedule.getEndDateString());
        viewHolder.mStartDate.setText(schedule.getStartDateString());

    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mSchedules.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        public TextView mVacationName;
        public TextView mStartDate;
        public TextView mEndDate;
        public ImageButton mVacationEdit;
        public ImageButton mVacationDelete;

        public ViewHolder(View itemView)
        {
            super(itemView);
            mVacationName = itemView.findViewById(R.id.vacationTitle);
            mStartDate = itemView.findViewById(R.id.vacationStartDate);
            mEndDate = itemView.findViewById(R.id.vacationEndDate);
            mVacationEdit = itemView.findViewById(R.id.vacationEditButton);
            mVacationDelete = itemView.findViewById(R.id.vacationDeleteButton);
        }
    }
}
