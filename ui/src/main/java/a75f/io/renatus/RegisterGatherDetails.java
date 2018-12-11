package a75f.io.renatus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.projecthaystack.HGrid;
import org.projecthaystack.HStr;

import a75f.io.api.haystack.CCUHsApi;

public class RegisterGatherDetails extends Activity {




    Button mUseExistingSiteButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register_gather_details);

        EditText siteIdEditText = (EditText)findViewById(R.id.site_id_edittext);
        Button noSiteButton = (Button)findViewById(R.id.no_site_button);

        mUseExistingSiteButton = (Button)findViewById(R.id.use_existing_site_button);
        mUseExistingSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadExistingSite(siteIdEditText.getText().toString());
            }
        });
        //TODO: let's start with the no site button,
        //The site doesn't have a preconfig token and it doesn't have a
        //site id.
        noSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(RegisterGatherDetails.this,
                        RegisterGatherSiteDetails.class);
                startActivity(i);
            }
        });


    }


    public void loadExistingSite(String siteId) {


        //String siteIdAzure = "123";  //Azure ID
        HGrid hGrid = CCUHsApi.getInstance().hsClient.nav(HStr.make(siteId));
        hGrid.dump();
        //Get this site from azure.
        //Site azureSite = CCUHsApi.getInstance().getSiteFromAzure();

        //Pre-Populate these fields
        //Set it as an orphan site and orphan ccu

    }
}
