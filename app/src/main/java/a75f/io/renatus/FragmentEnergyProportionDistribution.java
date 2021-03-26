package a75f.io.renatus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentEnergyProportionDistribution extends Fragment {



    public FragmentEnergyProportionDistribution newInstance() {
        return new FragmentEnergyProportionDistribution();
    }

    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_energy_proportion_distribution, container, false);

        return rootView;
    }
}
