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

public class VacationAdapter extends RecyclerView.Adapter<VacationAdapter.ViewHolder>
{

    private List<Schedule>              mSchedules;
    private ImageButton.OnClickListener mEditOnClickListener;
    private ImageButton.OnClickListener mDeleteOnClickListener;

    public VacationAdapter(List<Schedule> schedules, ImageButton.OnClickListener editOnClickListener, ImageButton.OnClickListener deleteOnClickListener)
    {
        mSchedules = schedules;
        mEditOnClickListener = editOnClickListener;
        mDeleteOnClickListener = deleteOnClickListener;

    }

    @Override
    public VacationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        Context        context  = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View       scheduleView = inflater.inflate(R.layout.item_vacation_schedule, parent, false);
        ViewHolder viewHolder   = new ViewHolder(scheduleView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(VacationAdapter.ViewHolder viewHolder, int position)
    {
        Schedule schedule = mSchedules.get(position);

        viewHolder.mVacationName.setText(schedule.getDis());
        viewHolder.mEndDate.setText(schedule.getEndDateString());
        viewHolder.mStartDate.setText(schedule.getStartDateString());

    }

    @Override
    public int getItemCount()
    {
        return mSchedules.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        public TextView    mVacationName;
        public TextView    mStartDate;
        public TextView    mEndDate;
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
            mVacationEdit.setOnClickListener(this);
            mVacationDelete.setOnClickListener(this);
        }

        @Override
        public void onClick(View v)
        {
            v.setTag(mSchedules.get(getAdapterPosition()).getId());
            if (v.getId() == R.id.vacationEditButton)
            {
                mEditOnClickListener.onClick(v);
            } else if (v.getId() == R.id.vacationDeleteButton)
            {
                mDeleteOnClickListener.onClick(v);
            }

        }
    }
}
