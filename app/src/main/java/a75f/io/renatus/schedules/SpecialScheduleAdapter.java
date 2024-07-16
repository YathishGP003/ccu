package a75f.io.renatus.schedules;

import static a75f.io.renatus.UtilityApplication.context;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.projecthaystack.HDict;

import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.Tags;
import a75f.io.logic.schedule.SpecialSchedule;
import a75f.io.renatus.R;
import a75f.io.renatus.util.FontManager;

public class SpecialScheduleAdapter extends RecyclerView.Adapter<SpecialScheduleAdapter.ViewHolder>{

    private List<HashMap<Object, Object>> specialSchedules;
    private final ImageButton.OnClickListener editOnClickListener;
    private final ImageButton.OnClickListener deleteOnClickListener;
    private static final DateTimeFormatter OUTPUT_DATE_FORMAT = DateTimeFormat.forPattern("dd MMM YYYY | HH:mm");
    private static final DateTimeFormatter OUTPUT_DATE_FORMAT_1 = DateTimeFormat.forPattern("dd MMM YYYY | ");
    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");


    public SpecialScheduleAdapter(List<HashMap<Object, Object>> schedules,
                                  ImageButton.OnClickListener deleteOnClickListener,
                                  ImageButton.OnClickListener editOnClickListener) {
        this.specialSchedules = schedules;
        this.deleteOnClickListener = deleteOnClickListener;
        this.editOnClickListener = editOnClickListener;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context  = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View specialScheduleView = inflater.inflate(R.layout.item_special_schedule, parent, false);
        return new SpecialScheduleAdapter.ViewHolder(specialScheduleView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        HashMap<Object, Object> specialSchedule = specialSchedules.get(position);
        viewHolder.specialScheduleName.setText(specialSchedule.get(Tags.DIS).toString());
        HDict range = (HDict) specialSchedule.get(Tags.RANGE);
        DateTime beginDate = INPUT_DATE_FORMAT.parseDateTime(range.get(Tags.STDT).toString())
                .withHourOfDay(SpecialSchedule.getInt(range.get(Tags.STHH).toString()))
                .withMinuteOfHour(SpecialSchedule.getInt(range.get(Tags.STMM).toString()));

        viewHolder.startDate.setText(beginDate.toString(OUTPUT_DATE_FORMAT));

        int endHour = SpecialSchedule.getInt(range.get(Tags.ETHH).toString());
        int endMin = SpecialSchedule.getInt(range.get(Tags.ETMM).toString());
        if(endHour == 24){
            DateTime endDate = INPUT_DATE_FORMAT.parseDateTime(range.get(Tags.ETDT).toString());
            viewHolder.endDate.setText(endDate.toString(OUTPUT_DATE_FORMAT_1) +"24:00");
        }
        else {
            DateTime endDate = INPUT_DATE_FORMAT.parseDateTime(range.get(Tags.ETDT).toString()).withHourOfDay(endHour)
                    .withMinuteOfHour(endMin);

            viewHolder.endDate.setText(endDate.toString(OUTPUT_DATE_FORMAT));
        }

    }

    @Override
    public int getItemCount() {
        return specialSchedules.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView specialScheduleName;
        private TextView startDate;
        private TextView endDate;
        private ImageButton specialScheduleEdit;
        private ImageButton specialScheduleDelete;

        public ViewHolder(View itemView) {
            super(itemView);

            Typeface iconFont = FontManager.getTypeface(context.getApplicationContext(), FontManager.FONTAWESOME);

            specialScheduleName = itemView.findViewById(R.id.specialScheduleTitle);
            startDate = itemView.findViewById(R.id.specialScheduleStartDate);
            endDate = itemView.findViewById(R.id.specialScheduleEndDate);
            specialScheduleEdit = itemView.findViewById(R.id.specialScheduleEditButton);
            specialScheduleEdit.setOnClickListener(this);
            specialScheduleDelete = itemView.findViewById(R.id.specialScheduleDeleteButton);
            specialScheduleDelete.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            view.setTag(specialSchedules.get(getAdapterPosition()).get(Tags.ID));
            if(view.getId() == R.id.specialScheduleDeleteButton){
                deleteOnClickListener.onClick(view);
            }
            else if(view.getId() == R.id.specialScheduleEditButton){
                editOnClickListener.onClick(view);
            }
        }
    }
}
