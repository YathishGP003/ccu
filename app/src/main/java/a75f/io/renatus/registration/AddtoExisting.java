package a75f.io.renatus.registration;

import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.constants.HttpConstants;
import a75f.io.constants.SiteFieldConstants;
import a75f.io.logger.CcuLog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.renatus.R;
import a75f.io.renatus.RegisterGatherCCUDetails;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;

public class AddtoExisting extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ImageView            imageGoback;

    TextInputLayout mTextInputSiteId;
    EditText             mSiteId;

    TextInputLayout mTextInputEmail;
    EditText             mSiteEmailId;

    TextInputLayout mTextInputPass;
    EditText             mPassword;

    Button              mNext1;
    Button              mNext2;
    Context mContext;

    ProgressBar mProgressDialog;
    Prefs prefs;

    public AddtoExisting() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StartCCUFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AddtoExisting newInstance(String param1, String param2) {
        AddtoExisting fragment = new AddtoExisting();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(getActivity());
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //((FreshRegistration)getActivity()).showIcons(false);
        View rootView = inflater.inflate(R.layout.fragment_addtoexisting, container, false);

        mContext = getContext().getApplicationContext();

        imageGoback = rootView.findViewById(R.id.imageGoback);

        mTextInputSiteId = rootView.findViewById(R.id.textInputSiteID);
        mSiteId = rootView.findViewById(R.id.editSiteID);

        mTextInputEmail = rootView.findViewById(R.id.textInputEmail);
        mSiteEmailId = rootView.findViewById(R.id.editFacilityEmail);

        mTextInputPass = rootView.findViewById(R.id.textInputPassword);
        mPassword = rootView.findViewById(R.id.editFacilityPass);

        mNext1 = rootView.findViewById(R.id.buttonNext1);
        mNext2 = rootView.findViewById(R.id.buttonNext2);

        mProgressDialog = rootView.findViewById(R.id.progressbar);

        mTextInputSiteId.setHintEnabled(false);
        mTextInputEmail.setHintEnabled(false);
        mTextInputPass.setHintEnabled(false);


        mTextInputSiteId.setErrorEnabled(true);
        mTextInputEmail.setErrorEnabled(true);
        mTextInputPass.setErrorEnabled(true);

        mTextInputSiteId.setError("");
        mTextInputEmail.setError("");
        mTextInputPass.setError("");



        mSiteId.addTextChangedListener(new EditTextWatcher(mSiteId));
        mSiteEmailId.addTextChangedListener(new EditTextWatcher(mSiteEmailId));
        mPassword.addTextChangedListener(new EditTextWatcher(mPassword));



        imageGoback.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                ((FreshRegistration)getActivity()).selectItem(1);
            }
        });


        mNext1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mNext1.setEnabled(false);
                int[] mandotaryIds = new int []
                        {
                                R.id.editSiteID
                        };
                if(!validateEditText(mandotaryIds))
                {
                    String siteId = StringUtils.trim(mSiteId.getText().toString());
                    siteId = StringUtils.prependIfMissing(siteId, "@");
                    loadExistingSite(siteId);
                }
            }
        });

        mNext2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                int[] mandotaryIds = new int []
                        {
                                R.id.editFacilityEmail,
                                R.id.editFacilityPass
                        };
                if(!validateEditText(mandotaryIds))
                {
                    goTonext();
                }
            }
        });

        return rootView;
    }



    private class EditTextWatcher implements TextWatcher {

        private View view;
        private EditTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        public void afterTextChanged(Editable editable) {
            switch(view.getId()){
                case R.id.editSiteID:
                    if(mSiteId.getText().length() > 0) {
                        mTextInputSiteId.setErrorEnabled(true);
                        mTextInputSiteId.setError(getString(R.string.input_siteid));
                        mSiteId.setError(null);
                    }else {
                        mTextInputSiteId.setError("");
                        mTextInputSiteId.setErrorEnabled(true);
                        mSiteId.setError(null);
                    }
                case R.id.editFacilityEmail:
                    if(mSiteEmailId.getText().length() > 0) {
                        mTextInputEmail.setErrorEnabled(true);
                        mTextInputEmail.setError(getString(R.string.input_facilityemail));
                        mSiteEmailId.setError(null);
                        String emailID = mSiteEmailId.getText().toString();
                        if(Patterns.EMAIL_ADDRESS.matcher(emailID).matches())
                        {

                        }else
                        {
                            mSiteEmailId.setError("Invalid Email Address");
                        }
                    }
                    else{
                        mTextInputEmail.setError("");
                        mSiteEmailId.setError(null);
                    }
                case R.id.editFacilityPass:
                    if(mPassword.getText().length() > 0) {
                        mTextInputPass.setErrorEnabled(true);
                        mTextInputPass.setError(getString(R.string.hint_password));
                        mPassword.setError(null);
                    }else {
                        mTextInputPass.setError("");
                        mTextInputPass.setErrorEnabled(true);
                        mPassword.setError(null);
                    }

            }
        }
    }


    public boolean validateEditText(int[] ids)
    {
        boolean isEmpty = false;

        for(int id: ids)
        {
            EditText et = (EditText)getView().findViewById(id);

            if(TextUtils.isEmpty(et.getText().toString()))
            {
                et.setError("Must enter Value");
                isEmpty = true;
            }
        }

        return isEmpty;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void goTonext() {
        ((FreshRegistration)getActivity()).selectItem(4);
    }

    public void loadExistingSite(final String siteId) {


        AsyncTask<Void, Void, String> getHSClientTask = new AsyncTask<Void, Void, String>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                ProgressDialogUtils.showProgressDialog(getActivity(), "Loading Site...");
            }

            @Override
            protected String doInBackground(Void... voids) {
                String siteIdResponse = null;

                if (StringUtils.isNotBlank(siteId)) {
                    String httpResponse = HttpUtil.executeJson(
                            CCUHsApi.getInstance().getAuthenticationUrl() + "sites/" + siteId,
                            "", BuildConfig.CARETAKER_API_KEY,
                            true, HttpConstants.HTTP_METHOD_GET
                    );
                    if (StringUtils.isNotBlank(httpResponse)) {
                        siteIdResponse = getSiteIdFromJson(httpResponse);
                    }
                } else {
                    Toast.makeText(getActivity(), "Unable to load site provided. Please try again or provide a different site ID.", Toast.LENGTH_LONG).show();
                }

                return siteIdResponse;
            }

            @Override
            protected void onPostExecute (String siteId){
                super.onPostExecute(siteId);
                ProgressDialogUtils.hideProgressDialog();

                if (StringUtils.isNotBlank(siteId)) {
                    Toast.makeText(getActivity(), "Site found", Toast.LENGTH_LONG).show();
                    showSiteDialog(siteId);
                } else {
                    Toast.makeText(getActivity(), "Site not found", Toast.LENGTH_LONG).show();
                }

                mNext1.setEnabled(true);
            }
        };
        getHSClientTask.execute();
    }

    // TODO Matt Rudd - This is duplicated in RegisterGatherDetails, but most of this class is duplicated
    private String getSiteIdFromJson(String loadSiteResponse) {
        String siteId = null;

        try {
            JSONObject loadSiteJsonResponse = new JSONObject(loadSiteResponse);
            if (loadSiteJsonResponse != null) {
                siteId = loadSiteJsonResponse.getString(SiteFieldConstants.ID);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            siteId = null;
        }

        return siteId;
    }

    private void showSiteDialog(String siteId) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                saveExistingSite(siteId);
                Toast.makeText(getActivity(), "Thank you for confirming using this site", Toast.LENGTH_LONG).show();

            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Toast.makeText(getActivity(), "Canceled use of existing site", Toast.LENGTH_LONG).show();
            }
        });

        builder.setTitle("Site");
        builder.setMessage("Registering for site ID " + siteId);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

//    private void showSiteDialog(String hGrid) {
//        prefs.setBoolean("registered", true);
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//
//                saveExistingSite(hGrid);
//                Toast.makeText(mContext, "Using this site!", Toast.LENGTH_LONG).show();
//                // CCUHsApi.getInstance().addExistingSite(hGrid);
//                //navigateToCCUScreen();
//
//            }
//        });
//        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                Toast.makeText(mContext, "Canceled using this site!", Toast.LENGTH_LONG).show();
//            }
//        });
//
//        builder.setTitle("Site");
//        String ccuDetails = "";
//        for(int i = 0; i<hGrid.numRows();i++)
//        {
//            ccuDetails = ccuDetails + hGrid.row(i);
//        }
//        builder.setMessage(ccuDetails);
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }

    private void saveExistingSite(String siteId) {

        CcuLog.d("ADD_CCU_EXISTING","Existing site ID used to register for existing site is: " + siteId);

        AsyncTask<String, Void, Boolean> syncSiteTask = new AsyncTask<String, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                ProgressDialogUtils.showProgressDialog(getActivity(), "Saving site...");
            }

            @Override
            protected Boolean doInBackground(String... strings) {
                String siteId = strings[0];
                boolean retVal = false;
                if (StringUtils.isNotBlank(siteId)) {
                    retVal = CCUHsApi.getInstance().syncExistingSite(siteId);
                    Globals.getInstance().setSiteAlreadyCreated(true);
                    L.ccu().systemProfile = new DefaultSystem();
                }
                return retVal;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                ProgressDialogUtils.hideProgressDialog();

                if (success) {
                    Toast.makeText(getActivity(), "Synchronizing the site with the 75F Cloud was successful.", Toast.LENGTH_LONG).show();
                    navigateToCCUScreen();
                } else {
                    Toast.makeText(getActivity(), "Synchronizing the site with the 75F Cloud was not successful. Please try again or try choosing a different site for registering this CCU.", Toast.LENGTH_LONG).show();
                }
            }
        };

        syncSiteTask.execute(siteId);
    }

//    private void saveExistingSite(HGrid hGrid) {
//
//        String siteIdVal = hGrid.row(0).getRef("id").val;
//        System.out.println("Site ID val: " + siteIdVal);
//
//        AsyncTask<String, Void, Boolean> syncSiteTask = new AsyncTask<String, Void, Boolean>() {
//
//
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//                ProgressDialogUtils.showProgressDialog(getActivity(),"Saving Site...");
//            }
//
//            @Override
//            protected Boolean doInBackground(String... strings) {
//                String siteId = strings[0];
//                boolean retVal = CCUHsApi.getInstance().syncExistingSite(siteId);
//                Globals.getInstance().setSiteAlreadyCreated(true);
//                BuildingTuners.getInstance();
//                L.ccu().systemProfile = new DefaultSystem();
//                return retVal;
//            }
//
//            @Override
//            protected void onPostExecute(Boolean success) {
//                super.onPostExecute(success);
//                if (!success) {
//                    Toast.makeText(mContext, "The site failed to sync.", Toast.LENGTH_LONG).show();
//                    ProgressDialogUtils.hideProgressDialog();
//                    return;
//                }
//
//                Toast.makeText(mContext, "Sync successful.", Toast.LENGTH_LONG).show();
//                ProgressDialogUtils.hideProgressDialog();
//                navigateToCCUScreen();
//            }
//        };
//
//        syncSiteTask.execute(siteIdVal);
//    }

    private void navigateToCCUScreen() {
        Intent intent = new Intent(getActivity(), RegisterGatherCCUDetails.class);
        startActivity(intent);
    }
}
