package a75f.io.renatus.registartion;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
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

import a75f.io.renatus.R;

public class ReplaceCCU extends Fragment {
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

    private static final String TAG = ReplaceCCU.class.getSimpleName();

    public ReplaceCCU() {
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
    public static ReplaceCCU newInstance(String param1, String param2) {
        ReplaceCCU fragment = new ReplaceCCU();
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
        //((FreshRegistration)getActivity()).showIcons(false);
        View rootView = inflater.inflate(R.layout.fragment_replaceccu, container, false);

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
                int[] mandotaryIds = new int []
                        {
                                R.id.editSiteID
                        };
                if(!validateEditText(mandotaryIds))
                {
                    goTonext();
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
            String text = editable.toString();
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
        //Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
        //startActivity(i);
        ((FreshRegistration)getActivity()).selectItem(4);
    }
}
