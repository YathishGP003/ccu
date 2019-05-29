package a75f.io.renatus.registartion;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import a75f.io.renatus.R;

public class Security extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    TextView             mZonePass;
    TextView             mSystemPass;
    TextView             mBuildingPass;
    TextView             mSetupPass;
    ImageView            imageGoback;
    Button mNext;
    Context mContext;

    private static final String TAG = Security.class.getSimpleName();

    public Security() {
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
    public static Security newInstance(String param1, String param2) {
        Security fragment = new Security();
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
        View rootView = inflater.inflate(R.layout.fragment_security, container, false);

        mContext = getContext().getApplicationContext();
        mZonePass = rootView.findViewById(R.id.textPassforZone);
        mSystemPass = rootView.findViewById(R.id.textPassforSystem);
        mBuildingPass = rootView.findViewById(R.id.textPassforBuild);
        mSetupPass = rootView.findViewById(R.id.textPassforSetup);
        mNext = rootView.findViewById(R.id.buttonNext);
        imageGoback = rootView.findViewById(R.id.imageGoback);


        imageGoback.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                ((FreshRegistration)getActivity()).selectItem(4);
            }
        });


        mZonePass.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                showCustomDialog();
            }
        });

        mNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                goTonext();
            }
        });


        return rootView;
    }

    private void showCustomDialog() {
        //then we will inflate the custom alert dialog xml that we created
        /*View dialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_password, null);


        //Now we need an AlertDialog.Builder object
        Dialog alertDialog = new Dialog(getActivity(),android.R.style.Theme_Material_Dialog_Alert);

        //setting the view of the builder to our custom view that we already inflated
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setContentView(dialogView);
        alertDialog.setCancelable(true);
        alertDialog.show();*/


        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        View layoutView = getLayoutInflater().inflate(R.layout.dialog_password, null);
        dialogBuilder.setView(layoutView);
        Dialog alertDialog = dialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        alertDialog.show();
        alertDialog.getWindow().setLayout(436, 304);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
    /* This site never existed we are creating a new orphaned site. */


    private void goTonext() {
        //Intent i = new Intent(mContext, RegisterGatherCCUDetails.class);
        //startActivity(i);
        ((FreshRegistration)getActivity()).selectItem(9);
    }
}
