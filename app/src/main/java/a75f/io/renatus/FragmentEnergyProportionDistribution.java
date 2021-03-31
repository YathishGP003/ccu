package a75f.io.renatus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;

public class FragmentEnergyProportionDistribution extends Fragment {

    public ArrayList<Floor> floorList = new ArrayList();
    Comparator<Floor> floorComparator = new Comparator<Floor>() {
        @Override
        public int compare(Floor a, Floor b) {
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        }
    };
    private View rootView;
    private RecyclerView floorListView;

    public FragmentEnergyProportionDistribution newInstance() {
        return new FragmentEnergyProportionDistribution();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_energy_proportion_distribution, container, false);
        initConfiguration();
        return rootView;
    }

    void initConfiguration() {
        floorListView = rootView.findViewById(R.id.floorList);
        floorList = HSUtil.getFloors();
        Collections.sort(floorList, floorComparator);
        EnergyDistributionAdapter energyDistributionAdapter = new EnergyDistributionAdapter(floorList, getContext());
        floorListView.setLayoutManager(new LinearLayoutManager(getContext()));
        floorListView.setAdapter(energyDistributionAdapter);
    }

}
