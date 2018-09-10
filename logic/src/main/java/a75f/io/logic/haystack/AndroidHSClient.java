package a75f.io.logic.haystack;

import org.projecthaystack.HGrid;
import org.projecthaystack.client.HClient;
import org.projecthaystack.server.HOp;
import org.projecthaystack.server.HServer;

/**
 * Created by samjithsadasivan on 8/31/18.
 */

/**
 *
 * This makes a local API call into the HServer instead of REST call.
 */
public class AndroidHSClient extends HClient
{
    CCUTagsDb db = new CCUTagsDb();
    
    protected AndroidHSClient(){
        super();
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
