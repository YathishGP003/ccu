package a75f.io.dal;

import com.kinvey.android.Client;

import a75f.io.util.Globals;

/**
 * Created by ryanmattison on 7/31/17.
 */

public class CloudGlobals {

    private Client kinveyClient;

    public void initKinveyClient() {
        this.kinveyClient = new Client.Builder(Globals.getInstance().getApplicationContext()).build();
    }

    public Client getKinveyClient() {
        return kinveyClient;
    }

    //Globals.getInstance().initKinveyClient();

}
