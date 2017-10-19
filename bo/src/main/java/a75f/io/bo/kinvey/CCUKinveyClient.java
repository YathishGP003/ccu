package a75f.io.bo.kinvey;

import android.content.Context;

import com.google.api.client.http.BackOffPolicy;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.kinvey.android.Client;
import com.kinvey.java.auth.CredentialStore;
import com.kinvey.java.core.KinveyClientRequestInitializer;

/**
 * Created by Yinten on 10/14/2017.
 */

public class  CCUKinveyClient extends Client<CCUUser>
{
    protected CCUKinveyClient(HttpTransport transport,
                              HttpRequestInitializer httpRequestInitializer, String rootUrl,
                              String servicePath, JsonObjectParser objectParser,
                              KinveyClientRequestInitializer kinveyRequestInitializer,
                              CredentialStore store, BackOffPolicy requestPolicy, Context context)
    {
        super(transport, httpRequestInitializer, rootUrl, servicePath, objectParser, kinveyRequestInitializer, store, requestPolicy, context);
    }
}
