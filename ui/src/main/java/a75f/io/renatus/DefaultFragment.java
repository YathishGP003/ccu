package a75f.io.renatus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class DefaultFragment extends Fragment {

    public static DefaultFragment getInstance() {
        return new DefaultFragment();
    }


    @BindView(R.id.fragment_main_textview)
    TextView mainTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View retVal = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, retVal);
        return retVal;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainTextView.setText("Butterknife");

    }
}
