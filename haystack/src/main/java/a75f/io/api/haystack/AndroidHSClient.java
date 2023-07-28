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


    public HGrid watchSubscribe(HGrid req) {
        // check for watchId or watchId
        String watchId = null;
        String watchDis = null;
        if (req.meta().has("watchId"))
            watchId = req.meta().getStr("watchId");
        else
            watchDis = req.meta().getStr("watchDis");

        // check for desired lease
        HNum lease = null;
        if (req.meta().has("lease"))
            lease = (HNum) req.meta().get("lease");

        // open or lookup watch
        HWatch watch = watchId == null ?
                watchOpen(watchDis, lease) :
                watch(watchId);

        // map grid to ids
        HRef[] ids = gridToIds(req);

        // subscribe and return resulting grid
        return watch.sub(ids);
    }

    HRef[] gridToIds(HGrid grid) {
        HRef[] ids = new HRef[grid.numRows()];
        for (int i = 0; i < ids.length; ++i) {
            HVal val = grid.row(i).get("id");
            ids[i] = valToId(val);
        }
        return ids;
    }

    HRef valToId(HVal val) {
        return (HRef) val;
    }

    public HGrid watchUnSubscribe(HGrid req) {
        // lookup watch, silently ignore failure
        String watchId = req.meta().getStr("watchId");
        HWatch watch = watch(watchId, false);

        // check for close or unsub
        if (watch != null) {
            if (req.meta().has("close"))
                watch.close();
            else {
                try {
                    watch.unsub(gridToIds(req));
                }catch (IllegalArgumentException illegalArgumentException){
                    Log.d("CCU_HS", illegalArgumentException.getMessage());
                }
            }
        }
        // nothing to return
        return HGrid.EMPTY;
    }

    public HGrid watchPoll(HGrid req) {
        String watchId = req.meta().getStr("watchId");
        try {
            HWatch watch = watch(watchId);
            // poll cov or refresh
            if (req.meta().has("refresh"))
                return watch.pollRefresh();
            else
                return watch.pollChanges();
        } catch (UnknownWatchException | UnknownRecException unknownWatchException) {
            Log.d("CCU_HS", unknownWatchException.getMessage());
        }catch (Exception exception){
            Log.d("CCU_HS", exception.getMessage());
        }
        return createEmptyGrid("err", "wrong watch id");
    }

    private HGrid createEmptyGrid(String metaInput, String message) {
        HGridBuilder b = new HGridBuilder();
        b.meta().add(metaInput)
                .add("errTrace", message);
        //b.addCol(message);
        return b.toGrid();
    }
    
}
