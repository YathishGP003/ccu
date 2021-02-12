package a75f.io.renatus.registration;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
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
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
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
    // TODO: Rename and change types and number of parameters
    public static InstallTypeFragment newInstance(String param1, String param2) {
        InstallTypeFragment fragment = new InstallTypeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root_view = inflater.inflate(R.layout.fragment_installtype, container, false);
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

        return root_view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {


        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*try {
            mCallback = (InstallType)context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }*/
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        //mCallback = null;
        super.onDetach();

    }
}
