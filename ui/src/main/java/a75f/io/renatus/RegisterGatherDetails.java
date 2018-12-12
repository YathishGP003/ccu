package a75f.io.renatus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class RegisterGatherDetails extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register_gather_details);

        EditText preconfigTokenEditText = (EditText)findViewById(R.id.preconfig_token_edittext);
        EditText siteIdEditText = (EditText)findViewById(R.id.site_id_edittext);
        Button noSiteButton = (Button)findViewById(R.id.no_site_button);

        //TODO: let's start with the no site button,
        //The site doesn't have a preconfig token and it doesn't have a
        //site id.
        noSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(RegisterGatherDetails.this,
                        RegisterGatherSiteDetails.class);
                startActivity(i);
                finish();
            }
        });

    }
}
