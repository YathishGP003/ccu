package org.projecthaystack.client;

import org.projecthaystack.HGrid;
import org.projecthaystack.server.HOp;
import org.projecthaystack.server.HServer;


/**
 * Created by samjithsadasivan on 7/9/18.
 */

public class AndroidHClient extends HClient
{
    
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
        return new org.projecthaystack.server.TestDatabase();
    }
}
