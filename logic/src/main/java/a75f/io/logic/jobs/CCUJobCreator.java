package a75f.io.logic.jobs;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by Yinten isOn 8/24/2017.
 */

public class CCUJobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        switch (tag) {
            case HeartBeatJob.TAG:
                return new HeartBeatJob();
            default:
                return null;
        }
    }
}
