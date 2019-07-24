package a75f.io.renatus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import a75f.io.logic.L;
import a75f.io.renatus.registartion.FreshRegistration;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by samjithsadasivan isOn 8/7/17.
 */

public class SystemProfileFragment extends Fragment {
    @BindView(R.id.spinnerSystemProfile)
    Spinner spSystemProfile;

    @BindView(R.id.txt_header)
    TextView txtHeader;

    @BindView(R.id.spinnerLayout)
    LinearLayout spinnerLayout;

    private boolean isFreshRegister;

    public SystemProfileFragment() {
    }

    public static SystemProfileFragment newInstance() {
        return new SystemProfileFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile_selector, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        isFreshRegister = getActivity() instanceof FreshRegistration;

        if (!isFreshRegister) {
            txtHeader.setVisibility(View.GONE);
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.setMargins(0, 30, 0, 0);

            ViewGroup.MarginLayoutParams spinner = (ViewGroup.MarginLayoutParams) spinnerLayout.getLayoutParams();
            spinner.setMargins(0,0,50,0);
        } else {
            txtHeader.setVisibility(View.VISIBLE);
        }

        ArrayAdapter<CharSequence> systemProfileSelectorAdapter = ArrayAdapter.createFromResource(this.getActivity(), R.array.system_profile_select, R.layout.spinner_dropdown_item);
        systemProfileSelectorAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spSystemProfile.setAdapter(systemProfileSelectorAdapter);
        spSystemProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!isFreshRegister) {
                    ((TextView) adapterView.getChildAt(0)).setTextColor(getResources().getColor(R.color.accent));
                    ((TextView) adapterView.getChildAt(0)).setTextSize(18);
                    spSystemProfile.getLayoutParams().width = 320;
                }
                switch (i) {
                    case 0:
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.profileContainer, new DefaultSystemProfile()).commit();
                        break;

                    case 1:
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.profileContainer, new VavStagedRtuProfile()).commit();
                        break;

                    case 2:
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.profileContainer, new VavAnalogRtuProfile()).commit();
                        break;
                    case 3:
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.profileContainer, new VavStagedRtuWithVfdProfile()).commit();
                        break;
                    case 4:
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.profileContainer, new VavHybridRtuProfile()).commit();
                        break;
                    case 5:
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.profileContainer, new DABStagedProfile()).commit();
                        break;
                    case 6:
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.profileContainer, new DABFullyAHUProfile()).commit();
                        break;
                    case 7:
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.profileContainer, new DABStagedRtuWithVfdProfile()).commit();
                        break;
                    case 8:
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.profileContainer, new DABHybridAhuProfile()).commit();
                        break;
					/*case 0:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new DefaultSystemProfile()).commit();
						break;

					case 1:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new VavAnalogRtuProfile()).commit();
						break;

					case 2:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new VavStagedRtuProfile()).commit();
						break;
					case 3:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new VavStagedRtuWithVfdProfile()).commit();
						break;
					case 4:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new VavHybridRtuProfile()).commit();
						break;
					case 5:
						getActivity().getSupportFragmentManager().beginTransaction()
						             .replace(R.id.profileContainer, new DabAnalogRtuProfile()).commit();
						break;
*/
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        spSystemProfile.setSelection(L.ccu().systemProfile != null ?
                systemProfileSelectorAdapter.getPosition(L.ccu().systemProfile.getProfileName()) : 0);
    }
}
