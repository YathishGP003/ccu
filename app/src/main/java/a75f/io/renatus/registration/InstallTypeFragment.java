package a75f.io.renatus.registration;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.renatus.ENGG.LocalConnectionViewKt;
import a75f.io.renatus.R;
import a75f.io.renatus.util.Prefs;


public class InstallTypeFragment extends Fragment {

    ViewPager pager;
    RelativeLayout layoutCreateNew;
    RelativeLayout layoutAddCCU;
    RelativeLayout layoutPreconfigCCU;
    RelativeLayout layoutReplaceCCU;
    RelativeLayout layoutWithoutCloud;
    DefaultSystem systemProfile = null;
    //InstallType mCallback;
    Prefs prefs;
    public InstallTypeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_installtype, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View root_view = getView();
        layoutCreateNew = (RelativeLayout)root_view.findViewById(R.id.layoutCreateNew);
        layoutAddCCU = (RelativeLayout)root_view.findViewById(R.id.layoutAddCCU);
        layoutPreconfigCCU = (RelativeLayout)root_view.findViewById(R.id.layoutPreconfigCCU);
        layoutReplaceCCU = (RelativeLayout)root_view.findViewById(R.id.layoutReplaceCCU);
        layoutWithoutCloud = (RelativeLayout)root_view.findViewById(R.id.layoutWithoutCloud);

        prefs = new Prefs(getActivity());

        //TODO: enable when implement
        layoutPreconfigCCU.setEnabled(false);
        layoutReplaceCCU.setEnabled(false);
        layoutWithoutCloud.setEnabled(false);

        layoutCreateNew.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                prefs.setString("INSTALL_TYPE","CREATENEW");
                //mCallback.GoTo(2,3);
                ((FreshRegistration)getActivity()).selectItem(2);

            }
        });

        layoutAddCCU.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method
                prefs.setString("INSTALL_TYPE","ADDCCU");
                //mCallback.GoTo(2,6);
                ((FreshRegistration)getActivity()).selectItem(2);
            }
        });

        layoutPreconfigCCU.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                prefs.setString("INSTALL_TYPE","PRECONFIGCCU");
                //mCallback.GoTo(2,7);
                ((FreshRegistration)getActivity()).selectItem(2);
            }
        });

        layoutReplaceCCU.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                prefs.setString("INSTALL_TYPE","REPLACECCU");
                //mCallback.GoTo(2,8);
                ((FreshRegistration)getActivity()).selectItem(2);
            }
        });

        layoutWithoutCloud.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                prefs.setString("INSTALL_TYPE","OFFLINE");
                ((FreshRegistration)getActivity()).selectItem(4);
            }
        });


        HashMap site = CCUHsApi.getInstance().read("site");
        HashMap ccu = CCUHsApi.getInstance().read("ccu");
           /* if(ccu.size() > 0)
            {
                String ccuId = ccu.get("id").toString();
                CCUHsApi.getInstance().deleteEntityTree(ccuId);
            }*/
        if(site.size() > 0)
        {
            String siteId = site.get("id").toString();
            CCUHsApi.getInstance().deleteEntityTree(siteId);


            if (L.ccu().systemProfile instanceof DefaultSystem) {
            } else {

                new AsyncTask<String, Void, Void>() {
                    @Override
                    protected Void doInBackground(final String... params) {
                        if (systemProfile != null) {
                            systemProfile.deleteSystemEquip();
                            L.ccu().systemProfile = null;
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(final Void result) {
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            }

            prefs.setBoolean("CCU_SETUP",false);
            prefs.setBoolean("PROFILE_SETUP",false);
            CCUHsApi.getInstance().saveTagsData();
            CCUHsApi.getInstance().syncEntityTree();
            L.saveCCUState();
        }

        LocalConnectionViewKt.checkForIpEntryOnLocalBuild(requireContext());
    }
}
