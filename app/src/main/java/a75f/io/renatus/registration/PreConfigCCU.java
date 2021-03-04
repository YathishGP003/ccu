package a75f.io.renatus.registration;

import android.content.Context;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import a75f.io.renatus.R;

public class PreConfigCCU extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ImageView            imageGoback;

    TextInputLayout mTextInputPreConfigcode;
    EditText             mPreConfigCode;

    TextInputLayout mTextInputConfirmCode;
    EditText             mConfirmCode;

    Button              mNext;
    Context             mContext;

    private static final String TAG = PreConfigCCU.class.getSimpleName();

    public PreConfigCCU() {
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
    public static PreConfigCCU newInstance(String param1, String param2) {
        PreConfigCCU fragment = new PreConfigCCU();
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
        View rootView = inflater.inflate(R.layout.fragment_preconfigccu, container, false);

        mContext = getContext().getApplicationContext();

        imageGoback = rootView.findViewById(R.id.imageGoback);

        mTextInputPreConfigcode = rootView.findViewById(R.id.textInputConfigCode);

        mPreConfigCode = rootView.findViewById(R.id.editConfigCode);

        mTextInputConfirmCode = rootView.findViewById(R.id.textInputConfirmCode);
        mConfirmCode = rootView.findViewById(R.id.editConfirmCode);

        mNext = rootView.findViewById(R.id.buttonNext);

        mTextInputPreConfigcode.setHintEnabled(false);
        mTextInputConfirmCode.setHintEnabled(false);

        mTextInputPreConfigcode.setErrorEnabled(true);
        mTextInputConfirmCode.setErrorEnabled(true);

        mTextInputPreConfigcode.setError("");
        mTextInputConfirmCode.setError("");



        mPreConfigCode.addTextChangedListener(new EditTextWatcher(mPreConfigCode));
        mConfirmCode.addTextChangedListener(new EditTextWatcher(mConfirmCode));



        imageGoback.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                ((FreshRegistration)getActivity()).selectItem(1);
            }
        });


        mNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                int[] mandotaryIds = new int []
                        {
                                R.id.editConfigCode,
                                R.id.editConfirmCode
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
                case R.id.editConfigCode:
                    if(mPreConfigCode.getText().length() > 0) {
                        mTextInputPreConfigcode.setErrorEnabled(true);
                        mTextInputPreConfigcode.setError(getString(R.string.input_configcode));
                        mPreConfigCode.setError(null);
                    }else {
                        mTextInputPreConfigcode.setErrorEnabled(true);
                        mTextInputPreConfigcode.setError("");
                        mPreConfigCode.setError(null);
                    }
                case R.id.editConfirmCode:
                    if(mConfirmCode.getText().length() > 0) {
                        mTextInputConfirmCode.setErrorEnabled(true);
                        mTextInputConfirmCode.setError(getString(R.string.hint_confirmcode));
                        mConfirmCode.setError(null);
                    }else {
                        mTextInputConfirmCode.setErrorEnabled(true);
                        mTextInputConfirmCode.setError("");
                        mConfirmCode.setError(null);
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
