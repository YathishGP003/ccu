package a75f.io.renatus;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Mahesh on 17-07-2019.
 */
public class AboutFragment extends Fragment {

    //
    @BindView(R.id.tvSerialNumber)
    TextView tvSerialNumber;
    @BindView(R.id.tvCcuVersion)
    TextView tvCcuVersion;

    public AboutFragment() {

    }

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);
        ButterKnife.bind(this, rootView);

        HashMap site = CCUHsApi.getInstance().read("site");

        PackageManager pm = getActivity().getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0);
            String str = pi.versionName + "." + pi.versionCode;
            tvCcuVersion.setText(str);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        tvSerialNumber.setText(site.get("id").toString());

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
