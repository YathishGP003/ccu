package a75f.io.api.haystack;

import android.util.Log;

import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HVal;
import org.projecthaystack.HWatch;
import org.projecthaystack.UnknownRecException;
import org.projecthaystack.UnknownWatchException;
import org.projecthaystack.client.HClient;
import org.projecthaystack.server.HOp;
import org.projecthaystack.server.HServer;

import a75f.io.api.haystack.AndroidHSClient;
import a75f.io.api.haystack.CCUTagsDb;
import a75f.io.api.haystack.TestTagsDb;

/**
 * Created by samjithsadasivan on 8/31/18.
 */

/**
 *
 * This makes a local API call into the HServer instead of REST call.
 */
public class TestHSClient extends AndroidHSClient
{
    TestTagsDb db = new TestTagsDb();

    protected TestHSClient(){
        super();
        uri = null;
    }
    
    @Override
    public HClient open() {
        return this;
    }
    
    @Override
    public HGrid call(String opName, HGrid req)
    {
        
        HServer db = db();
        HOp op = db.op(opName, false);
        if (op == null)
        {
            return null;
        }
        
        try
        {
            return op.onService(db, req);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
    
    public HServer db()
    {
        return db;
    }
}
