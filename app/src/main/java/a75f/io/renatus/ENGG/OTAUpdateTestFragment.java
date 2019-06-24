package a75f.io.renatus.ENGG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.javolution.annotations.Nullable;

import a75f.io.logic.Globals;
import a75f.io.renatus.R;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OTAUpdateTestFragment extends Fragment {

    public static OTAUpdateTestFragment newInstance() { return new OTAUpdateTestFragment(); }

    @BindView(R.id.otaTestAddressText)
    EditText otaTestAddressText;

    @BindView(R.id.otaTestNameStringText)
    EditText otaTestNameStringText;

    @BindView(R.id.startOtaTestBtn)
    Button startOtaTestBtn;

    @BindView(R.id.resetOtaTestBtn)
    Button resetOtaTestBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_otau_test, container, false);
        ButterKnife.bind(this, rootView);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @OnClick(R.id.startOtaTestBtn)
    public void handleStartOtaTest() {
        Activity activity = getActivity();

        int lwMeshAddress = Integer.valueOf(otaTestAddressText.getText().toString());
        String firmwareInfo = otaTestNameStringText.getText().toString();

        Intent otaIntent = new Intent(Globals.IntentActions.ACTIVITY_MESSAGE);
        otaIntent.putExtra("lwMeshAddress", lwMeshAddress);
        otaIntent.putExtra("firmwareVersion", firmwareInfo);

        LocalBroadcastManager.getInstance(activity).sendBroadcast(otaIntent);
        //activity.startService(otaIntent);
    }

    @OnClick(R.id.resetOtaTestBtn)
    public void handleResetOtaTest() {
        Activity activity = getActivity();

        Intent otaIntent = new Intent(Globals.IntentActions.ACTIVITY_RESET);

        LocalBroadcastManager.getInstance(activity).sendBroadcast(otaIntent);
        //activity.startService(otaIntent);
    }
}
