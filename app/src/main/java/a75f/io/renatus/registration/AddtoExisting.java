package a75f.io.renatus.registration;

import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.constants.HttpConstants;
import a75f.io.constants.SiteFieldConstants;
import a75f.io.logger.CcuLog;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HRef;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.DefaultSystem;
import a75f.io.logic.messaging.MessagingClient;
import a75f.io.logic.pubnub.PbSubscriptionHandler;
import a75f.io.renatus.R;
import a75f.io.renatus.RegisterGatherCCUDetails;
import a75f.io.renatus.RenatusLandingActivity;
import a75f.io.renatus.util.PrefernceConstants;
import a75f.io.renatus.util.Prefs;
import a75f.io.renatus.util.ProgressDialogUtils;
import a75f.io.renatus.util.retrofit.ApiClient;
import a75f.io.renatus.util.retrofit.ApiInterface;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import static com.raygun.raygun4android.RaygunClient.getApplicationContext;

public class AddtoExisting extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ImageView            imageGoback;

    TextInputLayout mTextInputEmail;
    EditText             mSiteEmailId;

    TextInputLayout mTextInputPass;
    EditText             mPassword;

    Button              mNext1;
    Button              mNext2;
    Context mContext;

    ProgressBar mProgressDialog;
    Prefs prefs;
    EditText mEt1, mEt2, mEt3, mEt4, mEt5, mEt6;
    View toastLayout, toast_Fail;
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
        View rootView = inflater.inflate(R.layout.fragment_addtoexisting, container, false);

        try {
            //Creating the LayoutInflater instance
            LayoutInflater li = getLayoutInflater();
            toastLayout = li.inflate(R.layout.custom_toast_layout, (ViewGroup) rootView.findViewById(R.id.custom_toast_layout));
            toast_Fail = li.inflate(R.layout.custom_toast_layout_failed, (ViewGroup) rootView.findViewById(R.id.custom_toast_layout_fail));

            mContext = getContext().getApplicationContext();

            imageGoback = rootView.findViewById(R.id.imageGoback);

            mTextInputEmail = rootView.findViewById(R.id.textInputEmail);
            mSiteEmailId = rootView.findViewById(R.id.editFacilityEmail);

            mTextInputPass = rootView.findViewById(R.id.textInputPassword);
            mPassword = rootView.findViewById(R.id.editFacilityPass);

            mNext1 = rootView.findViewById(R.id.buttonNext1);
            mNext2 = rootView.findViewById(R.id.buttonNext2);

            mProgressDialog = rootView.findViewById(R.id.progressbar);
            mTextInputEmail.setHintEnabled(false);
            mTextInputPass.setHintEnabled(false);


            //mTextInputSiteId.setErrorEnabled(true);
            mTextInputEmail.setErrorEnabled(true);
            mTextInputPass.setErrorEnabled(true);

            //mTextInputSiteId.setError("");
            mTextInputEmail.setError("");
            mTextInputPass.setError("");


            //mSiteId.addTextChangedListener(new EditTextWatcher(mSiteId));
            mSiteEmailId.addTextChangedListener(new EditTextWatcher(mSiteEmailId));
            mPassword.addTextChangedListener(new EditTextWatcher(mPassword));

            mEt1 = rootView.findViewById(R.id.otp_edit_text1);
            mEt2 = rootView.findViewById(R.id.otp_edit_text2);
            mEt3 = rootView.findViewById(R.id.otp_edit_text3);
            mEt4 = rootView.findViewById(R.id.otp_edit_text4);
            mEt5 = rootView.findViewById(R.id.otp_edit_text5);
            mEt6 = rootView.findViewById(R.id.otp_edit_text6);

            mEt1.requestFocus();

            addTextWatcher(mEt1);
            addTextWatcher(mEt2);
            addTextWatcher(mEt3);
            addTextWatcher(mEt4);
            addTextWatcher(mEt5);
            addTextWatcher(mEt6);

            imageGoback.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Intent intent = new Intent(getActivity(), RenatusLandingActivity.class);
                    startActivity(intent);
                }
            });

            mNext1.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    int[] mandotaryIds = new int[]
                            {
                                    R.id.otp_edit_text1,
                                    R.id.otp_edit_text2,
                                    R.id.otp_edit_text3,
                                    R.id.otp_edit_text4,
                                    R.id.otp_edit_text5,
                                    R.id.otp_edit_text6
                            };
                    EditText et1 = rootView.findViewById(R.id.otp_edit_text1);
                    EditText et2 = rootView.findViewById(R.id.otp_edit_text2);
                    EditText et3 = rootView.findViewById(R.id.otp_edit_text3);
                    EditText et4 = rootView.findViewById(R.id.otp_edit_text4);
                    EditText et5 = rootView.findViewById(R.id.otp_edit_text5);
                    EditText et6 = rootView.findViewById(R.id.otp_edit_text6);
                    if (!validateEditText(mandotaryIds)) {
                        String OTP = et1.getText() + "" + et2.getText() + et3.getText() + "" + et4.getText() + "" + et5.getText() + et6.getText();
                        OTPValidation(OTP);
                    } else
                        Toast.makeText(mContext, "Please check the Building Passcode", Toast.LENGTH_SHORT).show();
                }
            });

            mNext2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    int[] mandotaryIds = new int[]
                            {
                                    R.id.editFacilityEmail,
                                    R.id.editFacilityPass
                            };
                    if (!validateEditText(mandotaryIds)) {
                        goTonext();
                    }
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }

        return rootView;
    }

    private void OTPValidation(String OTPCode) {
        ApiInterface apiInterface= null;
        try {
            apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        Call<ResponseBody> signInCall = apiInterface.ValidateOTP(OTPCode);
        ApiClient.getApiResponse(signInCall, new ApiClient.ApiCallBack() {
            @Override
            public void Success(Response<ResponseBody> response) throws IOException {
                //progressLoader.DismissProgress();
                String responseData= response.body().string();
                //Log.e("InsideAddtoExist","responseData_Success- "+responseData);
                try {
                    JSONObject jsonObject=new JSONObject(responseData);
                    if (jsonObject.getString("valid") == "true"){
                        JSONObject siteCode = jsonObject.getJSONObject("siteCode");
                        Toast toast = new Toast(getApplicationContext());
                        toast.setGravity(Gravity.BOTTOM, 50, 50);
                        toast.setView(toastLayout);
                        toast.setDuration(Toast.LENGTH_LONG);
                        toast.show();
                        loadExistingSite(siteCode.getString("siteId"));
                    }else{
                        Toast toast = new Toast(getApplicationContext());
                        toast.setGravity(Gravity.BOTTOM, 50, 50);
                        toast.setView(toast_Fail);
                        toast.setDuration(Toast.LENGTH_LONG);
                        toast.show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    //commonUtils.toastShort(e.toString(),getApplicationContext());
                }

            }

            @Override
            public void Failure(Response<ResponseBody> response) throws IOException
            {
                Toast.makeText(mContext, "Request Failed...Please talk to technical team", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void Error(Throwable t)
            {
               /* progressLoader.DismissProgress();
                commonUtils.toastShort(t.toString(),getApplicationContext());*/
                Toast.makeText(mContext, "Error...Please check Internet connection", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void addTextWatcher(final EditText one) {
        one.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                switch (one.getId()) {
                    case R.id.otp_edit_text1:
                        if (one.length() == 1) {
                            mEt2.requestFocus();
                        }
                        break;
                    case R.id.otp_edit_text2:
                        if (one.length() == 1) {
                            mEt3.requestFocus();
                        } else if (one.length() == 0) {
                            mEt1.requestFocus();
                        }
                        break;
                    case R.id.otp_edit_text3:
                        if (one.length() == 1) {
                            mEt4.requestFocus();
                        } else if (one.length() == 0) {
                            mEt2.requestFocus();
                        }
                        break;
                    case R.id.otp_edit_text4:
                        if (one.length() == 1) {
                            mEt5.requestFocus();
                        } else if (one.length() == 0) {
                            mEt3.requestFocus();
                        }
                        break;
                    case R.id.otp_edit_text5:
                        if (one.length() == 1) {
                            mEt6.requestFocus();
                        } else if (one.length() == 0) {
                            mEt4.requestFocus();
                        }
                        break;
                    case R.id.otp_edit_text6:
                        if (one.length() == 1) {
                            InputMethodManager inputManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                            mNext1.setEnabled(true);
                        } else if (one.length() == 0) {
                            mEt5.requestFocus();
                        }
                        break;
                }
            }
        });
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
                /*case R.id.editSiteID:
                    if(mSiteId.getText().length() > 0) {
                        //mTextInputSiteId.setErrorEnabled(true);
                        //mTextInputSiteId.setError(getString(R.string.input_siteid));
                        mSiteId.setError(null);
                    }else {
                        //mTextInputSiteId.setError("");
                        //mTextInputSiteId.setErrorEnabled(true);
                        mSiteId.setError(null);
                    }*/
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
        
        @SuppressLint("StaticFieldLeak")
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
                //navigateToCCUScreen();
                Toast.makeText(getActivity(), "Thank you for confirming using this site", Toast.LENGTH_LONG).show();

            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Toast.makeText(getActivity(), "Canceled use of existing site", Toast.LENGTH_LONG).show();
            }
        });

        builder.setTitle("Site");
        builder.setTitle("ADD CCU");
        builder.setMessage("Registering for site ID " + siteId);
        /*HashMap site = CCUHsApi.getInstance().read("site");
        builder.setMessage("Are you sure you want to add a new CCU to site " +site.get("dis"));*/

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveExistingSite(String siteId) {

        CcuLog.d("ADD_CCU_EXISTING","Existing site ID used to register for existing site is: " + siteId);

        @SuppressLint("StaticFieldLeak")
        AsyncTask<String, Void, Boolean> syncSiteTask = new AsyncTask<String, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                ProgressDialogUtils.showProgressDialog(getActivity(), "Saving site...This may take upto 5~10 mins");
            }

            @Override
            protected Boolean doInBackground(String... strings) {
                String siteId = strings[0];
                boolean retVal = false;
                if (StringUtils.isNotBlank(siteId)) {
                    retVal = CCUHsApi.getInstance().syncExistingSite(siteId);
                    Globals.getInstance().setSiteAlreadyCreated(true);
                    CCUHsApi.getInstance().setPrimaryCcu(false);
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

    private void navigateToCCUScreen() {
        prefs.setBoolean(PrefernceConstants.ADD_CCU, true);
        prefs.setBoolean(PrefernceConstants.CCU_SETUP, true);
        Intent intent = new Intent(getActivity(), RegisterGatherCCUDetails.class);
        startActivity(intent);
    }
}
