//
// Copyright (c) 2011, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   11 Jul 2011  Brian Frank  Creation
//   26 Sep 2012  Brian Frank  Revamp original code
//
package org.projecthaystack.client;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HHisItem;
import org.projecthaystack.HNum;
import org.projecthaystack.HProj;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.HStr;
import org.projecthaystack.HVal;
import org.projecthaystack.HWatch;
import org.projecthaystack.UnknownWatchException;
import org.projecthaystack.auth.AuthClientContext;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.RetryCountCallback;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.exception.NullHGridException;
import a75f.io.api.haystack.sync.SiloApiService;
import a75f.io.constants.HttpConstants;
import a75f.io.logger.CcuLog;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * HClient manages a logical connection to a HTTP REST haystack server.
 *
 * @see <a href='http://project-haystack.org/doc/Rest'>Project Haystack</a>
 */
public class HClient extends HProj
{
  public HClient()
  {
  }

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for construction and call to open().
   */
  public static HClient open(String uri, String user, String pass)
  {
    return new HClient(uri, user, pass).open();
  }

  /**
   * Convenience for constructing client with custom timeouts and call to open()
   */
  public static HClient open(String uri, String user, String pass, final int connectTimeout, final int readTimeout)
  {
    return new HClient(uri, user, pass).setTimeouts(connectTimeout, readTimeout).open();
  }

  /**
   * Constructor with URI to server's API and authentication credentials.
   */
  public HClient(String uri, String user, String pass)
  {
    // check uri
    if (!uri.startsWith("http://") && !uri.startsWith("https://")) throw new IllegalArgumentException("Invalid uri format: " + uri);
    if (!uri.endsWith("/")) uri = uri + "/";

    // sanity check arguments
    if (user.length() == 0) throw new IllegalArgumentException("user cannot be empty string");

    this.uri  = uri;
    //this.auth = new AuthClientContext(uri + "about", user, pass);
  }

//////////////////////////////////////////////////////////////////////////
// State
//////////////////////////////////////////////////////////////////////////

  private final HashMap watches = new HashMap();

  /* contains point id and point data*/
  private final HashMap sharedEntities = new HashMap();
  private final HashMap sharedPointArrays = new HashMap();

  /** Base URI for connection such as "http://host/api/demo/".
      This string always ends with slash. */
  public String uri;

  /** Timeout in milliseconds for opening the HTTP socket */
  public int connectTimeout = 60 * 1000;
  private static OkHttpClient okHttpClient = null;

  /** Set the connect timeout and return this */
  public HClient setConnectTimeout(final int timeout)
  {
    if (timeout < 0) throw new IllegalArgumentException("Invalid timeout: " + timeout);
    this.connectTimeout = timeout;
    return this;
  }

  /** Timeout in milliseconds for reading from the HTTP socket */
  public int readTimeout = 60 * 1000;

  /** Set the read timeout and return this */
  public HClient setReadTimeout(final int timeout)
  {
    if (timeout < 0) throw new IllegalArgumentException("Invalid timeout: " + timeout);
    this.readTimeout = timeout;
    return this;
  }

  /** Set the connect and read timeouts and return this */
  public HClient setTimeouts(final int connectTimeout, final int readTimeout)
  {
    return setConnectTimeout(connectTimeout).setReadTimeout(readTimeout);
  }

  private int version = 2;

  /**
   * @return the zinc version to use when encoding ops
   */
  public int getVersion() { return version; }

  /**
   * Set the zinc version to use when encoding ops
   *
   * @param version the zinc version to use
   * @return this
   */
  public HClient setVersion(final int version)
  {
    this.version = version;
    return this;
  }

//////////////////////////////////////////////////////////////////////////
// Operations
//////////////////////////////////////////////////////////////////////////

  /**
   * Authenticate the client and return this.
   */
  public HClient open()
  {
    //auth.connectTimeout = this.connectTimeout;
    //auth.readTimeout    = this.readTimeout;
    //auth.open();
    return this;
  }

  /**
   * Call "about" to query summary info.
   */
  public HDict about()
  {
    return call("about", HGrid.EMPTY).row(0);
  }

  /**
   * Call "ops" to query which operations are supported by server.
   */
  public HGrid ops()
  {
    return call("ops", HGrid.EMPTY);
  }

  /**
   * Call "formats" to query which MIME formats are available.
   */
  public HGrid formats()
  {
    return call("formats", HGrid.EMPTY);
  }

//////////////////////////////////////////////////////////////////////////
// Reads
//////////////////////////////////////////////////////////////////////////

  protected HDict onReadById(HRef id)
  {
    HGrid res = readByIds(new HRef[] { id }, false);
    if (res.isEmpty()) return null;
    HDict rec = res.row(0);
    if (rec.missing("id")) return null;
    return rec;
  }

  protected HGrid onReadByIds(HRef[] ids)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("id");
    for (int i=0; i<ids.length; ++i)
      b.addRow(new HVal[] { ids[i] });
    HGrid req = b.toGrid();
    return call("read", req);
  }

  protected HGrid onReadAll(String filter, int limit)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("filter");
    b.addCol("limit");
    b.addRow(new HVal[] { HStr.make(filter), HNum.make(limit) });
    HGrid req = b.toGrid();
    return call("read", req);
  }

//////////////////////////////////////////////////////////////////////////
// Evals
//////////////////////////////////////////////////////////////////////////


//////////////////////////////////////////////////////////////////////////
// Watches
//////////////////////////////////////////////////////////////////////////

  /**
   * Create a new watch with an empty subscriber list.  The dis
   * string is a debug string to keep track of who created the watch.
   */
  public HWatch watchOpen(String dis, HNum lease)
  {
    return new HClientWatch(this, dis, lease);
  }

  /**
   * List the open watches associated with this HClient.
   * This list does *not* contain a watch until it has been successfully
   * subscribed and assigned an identifier by the server.
   */
  public HWatch[] watches()
  {
    return (HWatch[])watches.values().toArray(new HWatch[watches.size()]);
  }

  /**
   * Lookup a watch by its unique identifier associated with this HClient.
   * If not found return null or raise UnknownWatchException based on
   * checked flag.
   */
  public HWatch watch(String id, boolean checked)
  {
    HWatch w = (HWatch)watches.get(id);
    if (w != null) return w;
    if (checked) throw new UnknownWatchException(id);
    return null;
  }

  HGrid watchSub(HClientWatch w, HRef[] ids, boolean checked)
  {
    if (ids.length == 0) throw new IllegalArgumentException("ids are empty");
    if (w.closed) throw new IllegalStateException("watch is closed");

    // grid meta
    HGridBuilder b = new HGridBuilder();
    if (w.id != null) {
      b.meta().add("watchId", w.id);
    } else {
      // generating watch id
      String watchId = UUID.randomUUID().toString();
      b.meta().add("watchId", watchId);
      w.id = watchId;
    }
    if (w.desiredLease != null) b.meta().add("lease", w.desiredLease);
    b.meta().add("watchDis", w.dis);

    // grid rows
    b.addCol("id");
    for (int i=0; i<ids.length; ++i) {
      b.addRow(new HVal[]{ids[i]});
    }

    w.lease = w.desiredLease;
    watches.put(w.id, w);

    List<HRef> newIds = Arrays.asList(ids);
    w.subscribedIds.addAll(Arrays.asList(ids));

    HRef[] hIds = new HRef[newIds.size()];
    for(int i = 0;i < newIds.size();i++){
      hIds[i] = newIds.get(i);
    }

    HGrid grid = CCUHsApi.getInstance().readHDictByIds(hIds);

    Iterator it = grid.iterator();
    while (it.hasNext()) {
      HRow r = (HRow) it.next();
      HVal rowId = r.get("id");
      sharedEntities.put(rowId, r);
      sharedPointArrays.put(rowId, getValue(rowId.toString()));

      //sharedPointArrays.put(rowId, CCUHsApi.getInstance().readPointArr(rowId.toString()));
    }

    return b.toGrid();
  }

  void watchUnsub(HClientWatch w, HRef[] ids)
  {
    if (ids.length == 0) throw new IllegalArgumentException("ids are empty");
    if (w.id == null) throw new IllegalStateException("nothing subscribed yet");
    if (w.closed) throw new IllegalStateException("watch is closed");
    CcuLog.i("CCU_HS","--watchUnsub---"+Arrays.toString(ids));
    for (int i = 0; i < ids.length; ++i) {
      sharedEntities.remove(ids[i]);
      sharedPointArrays.remove(ids[i]);
      w.subscribedIds.remove(ids[i]);
    }
    //w.subscribedIds.clear();
    CcuLog.i("CCU_HS","--unsub--sharedEntities-"+sharedEntities.size() + "-sharedPointArrays-" + sharedPointArrays.size() + "-subscribedIds-" + w.subscribedIds.size());
  }

  HGrid watchPoll(HClientWatch w, boolean refresh)
  {
    if (w.id == null) throw new IllegalStateException("nothing subscribed yet");
    if (w.closed) throw new IllegalStateException("watch is closed");

    CcuLog.i(Tags.BACNET_SUB_UN_SUB_POLL,"watchPoll - watchId->"+w.id+"-refresh->"+refresh);
    // grid meta
    HGridBuilder b = new HGridBuilder();
    b.meta().add("watchId", w.id);
    if (refresh) b.meta().add("refresh");
    b.addCol("empty");

    return pollChanges(w, refresh);

  }

  private HGrid pollChanges(HClientWatch w, boolean refresh) {

    List<HRef> allSubscribedIdsList = (List<HRef>) w.subscribedIds;
    HRef[] ids = allSubscribedIdsList.toArray(new HRef[0]);
    HGrid requestedGridData = CCUHsApi.getInstance().readHDictByIds(ids);

    CcuLog.i(Tags.BACNET_SUB_UN_SUB_POLL,"pollChanges - subscribedIds->"+Arrays.toString(ids));

    if (refresh) {
      // send all
      return requestedGridData;
    } else {
      // send only change points
      ArrayList<HDict> pIds = new ArrayList<>();
      Iterator it = requestedGridData.iterator();

      // iterate through entities subscribed as part of watch with latest data
      String idStr = "id";
      String lastModifiedDateTimeStr = "lastModifiedDateTime";
      while (it.hasNext()) {
        HRow r = (HRow) it.next();
        HVal lastModifiedDateTime = r.get(lastModifiedDateTimeStr);
        HVal rowId = r.get(idStr);
        HRow previousRow = (HRow) sharedEntities.get(rowId);
        HVal previousLastModifiedDateTime = previousRow.get(lastModifiedDateTimeStr);

        if(previousLastModifiedDateTime != null && lastModifiedDateTime != null){
          if (!previousLastModifiedDateTime.equals(lastModifiedDateTime)) {
            pIds.add(getDictFromHRow(r));
            sharedPointArrays.put(rowId, getValue(rowId.toString()));
          }else{
            checkItemsWithInPointArray(pIds, idStr, r, rowId);
          }
        }else{
          checkItemsWithInPointArray(pIds, idStr, r, rowId);
        }
      }
      return HGridBuilder.dictsToGrid(pIds.toArray(new HDict[0]));
    }
  }

  private void checkItemsWithInPointArray(ArrayList<HDict> pIds, String idStr, HRow r, HVal rowId) {
    Double currentHighPriorityVal = getValue(r.get(idStr).toString());
    Boolean isHeartBeatPoint = isHeartBeatPoint("heartbeat", r.get(idStr).toString());
    if(isHeartBeatPoint) {
      ArrayList<HisItem> hisItem = CCUHsApi.getInstance().hisRead(r.get(idStr).toString(), "current");
      if (hisItem != null && hisItem.size() > 0) {

      long lastModifiedTimeInMillis = hisItem.get(0).getDateInMillis();
      long currentTimeInMillis = System.currentTimeMillis();
      long diffTime =
              TimeUnit.MILLISECONDS.toMinutes(currentTimeInMillis - lastModifiedTimeInMillis);
        if (diffTime > 15 || hisItem.get(0).getVal() == null) {
          currentHighPriorityVal = 0.0;
        }
      }
    }
    Double sharedHighPriorityVal = (Double) sharedPointArrays.get(r.get(idStr));
    Log.d("CCU_HS", "-currentHighPriorityVal-" + currentHighPriorityVal + "-sharedHighPriorityVal-" + sharedHighPriorityVal);
    if (Double.compare(currentHighPriorityVal, sharedHighPriorityVal) != 0) {
      pIds.add(getDictFromHRow(r));
      sharedPointArrays.put(rowId, currentHighPriorityVal);
    } else {
      Log.d("CCU_HS", "no change in data");
    }
  }

  private Double getValue(String pointId){
    Double currentHighPriorityVal;
    HashMap<Object, Object> pointData = CCUHsApi.getInstance().readMapById(pointId);
    if(pointData.containsKey("writable")){
      currentHighPriorityVal = CCUHsApi.getInstance().readPointPriorityVal(pointId);
    }else{
      currentHighPriorityVal = CCUHsApi.getInstance().readHisValById(pointId);
    }
    return currentHighPriorityVal;
  }

  private HDict getDictFromHRow(HRow r) {
    HRow.RowIterator ri = (HRow.RowIterator) r.iterator();
    HDictBuilder pid = new HDictBuilder();
    while (ri.hasNext()) {
      HDict.MapEntry e = (HDict.MapEntry) ri.next();
      pid.add((String) e.getKey(), e.getValue().toString());
    }
    return pid.toDict();
  }

  private boolean isValuePresent(String inputLevel, String inputTime, String inputValue, HGrid inputGrid) {
    Iterator itPr = inputGrid.iterator();
    while (itPr.hasNext()) {
      HRow row = (HRow) itPr.next();
      String level = row.get("level").toString();
      String val = row.get("val").toString();
      String lastModifiedDateTimeForVal = row.get("lastModifiedDateTime").toString();
      if(inputLevel.equalsIgnoreCase(level)){
        if (inputValue.equalsIgnoreCase(val)) {
          if (lastModifiedDateTimeForVal.equalsIgnoreCase(inputTime)) {
            return true;
          } else {
            return false;
          }
        }else{
          return false;
        }
      }
      Log.d("CCU_HS", "lastModifiedDateTimeForVal->" + lastModifiedDateTimeForVal + "<----val---->" + val);
    }
    // shared value is removed
    return false;
  }

  void watchClose(HClientWatch w, boolean send)
  {
    // mark flag on watch itself, short circuit if already closed
    if (w.closed) return;
    w.closed = true;

    // remove it from my lookup table
    if (w.id != null) {
      Iterator it = w.subscribedIds.iterator();
      while (it.hasNext()) {
        HRef r = (HRef) it.next();
        sharedEntities.remove(r.val);
        sharedPointArrays.remove(r.val);
      }
      //watchesWithIds.remove(w.id);
      w.subscribedIds.clear();
      watches.remove(w.id);

    }

    // optionally send close message to server
    if (send)
    {
      try
      {
        HGridBuilder b = new HGridBuilder();
        b.meta().add("watchId", w.id).add("close");
        b.addCol("id");
        call("watchUnsub", b.toGrid());
      }
      catch (Exception e) {}
    }
  }

  /**
   * This method will be called when point is deleted from zone,
   * This method will clear point id and point data from watches
   * @param id
   */
  public void clearPointFromWatch(HRef id) {
    for (Object entry : watches.entrySet()) {
      Map.Entry<String,Object> obj = (Map.Entry<String, Object>) entry;
      HClientWatch watch = (HClientWatch) obj.getValue();
      Iterator<HRef> it = watch.subscribedIds.iterator();
      while (it.hasNext()) {
        HRef r = (HRef) it.next();
        if(r.equals(id)){
          sharedEntities.remove(r.val);
          it.remove();
          break;
        }
      }
    }
  }

  static class HClientWatch extends HWatch
  {
    HClientWatch(HClient c, String d, HNum l) {
      client = c;
      dis = d;
      desiredLease = l;
      subscribedIds = new ArrayList<>();
    }
    public String id() { return id; }
    public HNum lease() { return lease; }
    public String dis() { return dis; }
    public HGrid sub(HRef[] ids, boolean checked) { return client.watchSub(this, ids, checked); }

    public void unsub(HRef[] ids) throws IllegalArgumentException {
      client.watchUnsub(this, ids); }
    public HGrid pollChanges() { return client.watchPoll(this, false); }
    public HGrid pollRefresh() { return client.watchPoll(this, true); }
    public void close() { client.watchClose(this, true); }
    public boolean isOpen() { return !closed; }

    final HClient client;
    final String dis;
    final HNum desiredLease;
    String id;
    HNum lease;
    boolean closed;
    List<HRef> subscribedIds;
  }

//////////////////////////////////////////////////////////////////////////
// PointWrite
//////////////////////////////////////////////////////////////////////////

  /**
    * Write to a given level of a writable point, and return the current status
    * of a writable point's priority array (see pointWriteArray()).
    *
    * @param id Ref identifier of writable point
    * @param level Number from 1-17 for level to write
    * @param val value to write or null to auto the level
    * @param who optional username performing the write, otherwise user dis is used
    * @param dur Number with duration unit if setting level 8
    */
  public HGrid pointWrite(
    HRef id, int level, String who,
    HVal val, HNum dur, HDateTime lastModifiedDateTime)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("id");
    b.addCol("level");
    b.addCol("who");
    b.addCol("val");
    b.addCol("duration");
    b.addCol("lastModifiedDateTime");

    b.addRow(new HVal[] {
      id,
      HNum.make(level),
      HStr.make(who),
      val,
      dur,
      lastModifiedDateTime
    });

    HGrid req = b.toGrid();
    HGrid res = call("pointWrite", req);
    return res;
  }

  /**
    * Return the current status
    * of a point's priority array.
    * The result is returned grid with following columns:
    * <ul>
    *   <li>level: number from 1 - 17 (17 is default)
    *   <li>levelDis: human description of level
    *   <li>val: current value at level or null
    *   <li>who: who last controlled the value at this level
    * </ul>
    */
  public HGrid pointWriteArray(HRef id)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("id");
    b.addRow(new HVal[] { id });

    HGrid req = b.toGrid();
    HGrid res = call("pointWrite", req);
    return res;
  }

//////////////////////////////////////////////////////////////////////////
// History
//////////////////////////////////////////////////////////////////////////

  /**
   * Read history time-series data for given record and time range. The
   * items returned are exclusive of start time and inclusive of end time.
   * Raise exception if id does not map to a record with the required tags
   * "his" or "tz".  The range may be either a String or a HDateTimeRange.
   * If HTimeDateRange is passed then must match the timezone configured on
   * the history record.  Otherwise if a String is passed, it is resolved
   * relative to the history record's timezone.
   */
  public HGrid hisRead(HRef id, Object range)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("id");
    b.addCol("range");
    b.addRow(new HVal[] { id, HStr.make(range.toString()) });
    HGrid req = b.toGrid();
    HGrid res = call("hisRead", req);
    return res;
  }

  /**
   * Write a set of history time-series data to the given point record.
   * The record must already be defined and must be properly tagged as
   * a historized point.  The timestamp timezone must exactly match the
   * point's configured "tz" tag.  If duplicate or out-of-order items are
   * inserted then they must be gracefully merged.
   */
  public void hisWrite(HRef id, HHisItem[] items)
  {
    long time = System.currentTimeMillis();
    HDict meta = new HDictBuilder().add("id", id).toDict();
    HGrid req = HGridBuilder.hisItemsToGrid(meta, items);
    call("hisWrite", req);
    CcuLog.i("CCU_HS","hisWrite "+id+" "+(System.currentTimeMillis()- time));
  }
  
  
  public HGrid nav(HStr id)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("navId");
    b.addRow(new HVal[] { id });
  
    HGrid req = b.toGrid();
    return call("nav", req);
  }

//////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////

  /**
   * Invoke a remote action using the "invokeAction" REST operation.
   */
  public HGrid invokeAction(HRef id, String action, HDict args)
  {
    HDict meta = new HDictBuilder().add("id", id).add("action", action).toDict();
    HGrid req = HGridBuilder.dictsToGrid(meta, new HDict[] { args });
    return call("invokeAction", req);
  }

//////////////////////////////////////////////////////////////////////////
// Call
//////////////////////////////////////////////////////////////////////////

  /**
   * Make a call to the given operation.  The request grid is posted
   * to the URI "this.uri+op" and the response is parsed as a grid.
   * Raise CallNetworkException if there is a communication I/O error.
   * Raise CallErrException if there is a server side error and an error
   * grid is returned.
   */
  public HGrid call(String op, HGrid req)
  {
    CcuLog.d("CCU_HCLIENT", "HClient Op: " + op);
    CcuLog.d("CCU_HCLIENT", "HClient Req: ");
    req.dump();
    HGrid res = postGrid(op, req);
    if (res != null && res.isErr()) {
      CcuLog.e("CCU_HS", "Network Error: " + res);
    }
    return res;
  }

  private HGrid postGrid(String op, HGrid req) {
    String reqStr = HZincWriter.gridToString(req, this.version);
    String resStr = postString(uri + op, reqStr);
    return (resStr == null ? null : new HZincReader(resStr).readGrid());
  }

  public HGrid call(String op, HGrid req, int page, int size) {
    req.dump();
    HGrid res = postGrid(op, req, page, size);
    if (res != null && res.isErr()) {
      CcuLog.e("CCU_HS", "Network Error: " + res);
    }
    return res;
  }

  private HGrid postGrid(String op, HGrid req, int page, int size) {
    String reqStr = HZincWriter.gridToString(req, this.version, page, size);
    CcuLog.d("CCU_HCLIENT", "reqStr: " + reqStr);
    uri = getUri(uri, op);
    String resStr = postString(uri + op, reqStr);
    if(op.equals("readChanges")){
      return (resStr == null ? null : new HZincReader(resStr).readGridForReadChanges());
    }else {
      return (resStr == null ? null : new HZincReader(resStr).readGrid());
    }
  }

  private String postString(String uriStr, String req) {
    return postString(uriStr, req, null);
  }

  // Assuming this old, alternate code is only for Haystack calls since it attaches Haystack API key
  private String postString(String uriStr, String req, String mimeType) {
    String bearerToken = CCUHsApi.getInstance().getJwt();
    String apiKey = BuildConfig.HAYSTACK_API_KEY;
    if (StringUtils.isNotBlank(bearerToken) || StringUtils.isNotBlank(apiKey)) {
      try {
        Log.d("CCU_HTTP_HCLIENT", "Request body: " + req);

        URL url = new URL(uriStr);
        HttpURLConnection c = openHttpConnection(url, "POST");
        try {
          c.setDoOutput(true);
          c.setDoInput(true);
          c.setRequestProperty("Connection", "Close");
          c.setRequestProperty("Content-Type", mimeType == null ? "text/zinc" : mimeType);
          c.setRequestProperty(HttpConstants.APP_NAME_HEADER_NAME, HttpConstants.APP_NAME_HEADER_VALUE);
          if (StringUtils.isNotBlank(bearerToken)) {
            c.setRequestProperty("Authorization", "Bearer " + bearerToken);
          } else {
            c.setRequestProperty("api-key", apiKey);
          }
          c.setConnectTimeout(300000);
          c.setReadTimeout(300000);
          c.connect();

          CcuLog.d("CCU_HTTP_REQUEST", "HClient: [POST] " + uriStr + " - Token: " + bearerToken);

          // post expression
          Writer cout = new OutputStreamWriter(c.getOutputStream(), StandardCharsets.UTF_8);
          cout.write(req);
          cout.close();

          CcuLog.d("CCU_HTTP_RESPONSE", "HClient:postString: " + c.getResponseCode() + " - [POST] "+ uriStr);

          // read response into string
          StringBuffer s = new StringBuffer(1024);
          Reader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
          int n;
          while ((n = r.read()) > 0) s.append((char)n);
          c.getInputStream().close();
          
          return s.toString();
        } finally {
          try {
            c.disconnect();
          } catch(Exception e) {
            CcuLog.e("CCU_HCLIENT", "Could not disconnect");
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }
    return null;
  }

////////////////////////////////////////////////////////////////
// Utils
////////////////////////////////////////////////////////////////

  private HttpURLConnection openHttpConnection(URL url, String method)
    throws IOException
  {
    return openHttpConnection(url, method, this.connectTimeout, this.readTimeout);
  }

  public static HttpURLConnection openHttpConnection(URL url, String method, int connectTimeout, int readTimeout)
    throws IOException
  {
    HttpURLConnection connection;
    String targetUrl = StringUtils.appendIfMissing(String.valueOf(url),"/");
    URL modifiedUrl = new URL(targetUrl);

    connection = openConnection("", modifiedUrl, targetUrl, method);
    connection.setRequestMethod(method);
    connection.setInstanceFollowRedirects(false);
    connection.setConnectTimeout(connectTimeout);
    connection.setReadTimeout(readTimeout);
    return connection;
  }

////////////////////////////////////////////////////////////////
// Property
////////////////////////////////////////////////////////////////

  static class Property
  {
    Property(String key, String value)
    {
      this.key = key;
      this.value = value;
    }

    public String toString()
    {
      return "[Property " +
        "key:" + key + ", " +
        "value:" + value + "]";
    }

    final String key;
    final String value;
  }

//////////////////////////////////////////////////////////////////////////
// Debug Utils
//////////////////////////////////////////////////////////////////////////

  /*
  private void dumpRes(HttpURLConnection c, boolean body) throws Exception
  {
    System.out.println("====  " + c.getURL());
    System.out.println("res: " + c.getResponseCode() + " " + c.getResponseMessage() );
    for (Iterator it = c.getHeaderFields().keySet().iterator(); it.hasNext(); )
    {
      String key = (String)it.next();
      String val = c.getHeaderField(key);
      System.out.println(key + ": " + val);
    }
    System.out.println();
    if (body)
    {
      InputStream in = c.getInputStream();
      int n;
      while ((n = in.read()) > 0) System.out.print((char)n);
    }
  }
  */

////////////////////////////////////////////////////////////////
// main
////////////////////////////////////////////////////////////////

  static HClient makeClient(String uri, String user, String pass) throws Exception
  {
//    // get bad credentials
//    try {
//      HClient.open(uri, "baduser", "badpass").about();
//      throw new IllegalStateException();
//    } catch (CallException e) { }
//
//    try {
//        HClient.open(uri, "haystack", "badpass").about();
//        throw new IllegalStateException();
//    } catch (CallException e) {  }

    // create proper client
    return HClient.open(uri, user, pass);
  }

  public static void main(String[] args) throws Exception
  {
    if (args.length != 3) {
      CcuLog.d("CCU_HS","usage: HClient <uri> <user> <pass>");
        System.exit(0);
    }

    HClient client = makeClient(args[0], args[1], args[2]);
    System.out.println(client.about());
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private AuthClientContext auth;

  private String getUri(String uri, String op) {
    if (op.equalsIgnoreCase("readChanges")) {
      return uri.replace("v1", "v2");
    }
    return uri;
  }

  ///////////////////////////////////////////////////////////////////////
  public HGrid invoke(String op, HGrid req, RetryCountCallback retryCountCallback) {
    CcuLog.d("CCU_HCLIENT", "HClient Op: " + op);
    req.dump();
    if (req.isEmpty()) {
      CcuLog.d("CCU_HCLIENT", "Empty Req: "+req);
      return null;
    }
    String reqStr = HZincWriter.gridToString(req, getVersion());
    String resStr = postStringWithRetry(uri + op, reqStr, retryCountCallback);
    HGrid res = (resStr == null ? null : new HZincReader(resStr).readGrid());
    if (res != null && res.isErr()) {
      CcuLog.e("CCU_HS", "Network Error: " + res);
    }
    return res;
  }
  // Assuming this old, alternate code is only for Haystack calls since it attaches Haystack API key
  private String postStringWithRetry(String uriStr, String req, RetryCountCallback retryCountCallback) {
    String bearerToken = CCUHsApi.getInstance().getJwt();
    String apiKey = BuildConfig.HAYSTACK_API_KEY;
    if (StringUtils.isNotBlank(bearerToken) || StringUtils.isNotBlank(apiKey)) {
      if(uriStr.endsWith("hisRead")){
        uriStr = uriStr.replace("v1", "v2");
      }
      Log.d("CCU_HCLIENT", "Request to " + uriStr);
      Log.d("CCU_HCLIENT", "Request body: " + req);
      Log.i("CCU_HCLIENT","Client Token: " + bearerToken);
      int retry = 0;
      int maxRetryCount = 15;
      long delay = 1000 * 30;
      boolean isOKResponse = true;
      HttpURLConnection httpConnection;
      do{
        try{
        if(!isOKResponse){
          Log.i("CCU_REPLACE", "Delay in the thread!");
          Thread.sleep(delay);
        }
          Log.i("CCU_REPLACE", "retry count : " +retry);
          URL url = new URL(uriStr);
        httpConnection = openHttpConnection(url, "POST");
        httpConnection.setDoOutput(true);
        httpConnection.setDoInput(true);
        httpConnection.setRequestProperty("Connection", "Close");
        httpConnection.setRequestProperty("Content-Type", "text/zinc");
        httpConnection.setRequestProperty(HttpConstants.APP_NAME_HEADER_NAME, HttpConstants.APP_NAME_HEADER_VALUE);
        if (StringUtils.isNotBlank(bearerToken)) {
          httpConnection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        } else {
          httpConnection.setRequestProperty("api-key", apiKey);
        }
        httpConnection.connect();
        Log.d("CCU_HTTP_REQUEST", "HClient: [POST] " + uriStr + " - Token: " + bearerToken);

        // post expression
        Writer cout = new OutputStreamWriter(httpConnection.getOutputStream(), StandardCharsets.UTF_8);
        cout.write(req);
        cout.close();

        Log.i("CCU_HCLIENT", "Request response code: " + httpConnection.getResponseCode());
        Log.i("CCU_HCLIENT", "HClient:postString: " + httpConnection.getResponseCode() + " - [POST] "+ uriStr);


        StringBuffer s = new StringBuffer(1024);
        Reader r = new BufferedReader(new InputStreamReader(httpConnection.getInputStream(), StandardCharsets.UTF_8));
        int n;
        while ((n = r.read()) > 0) s.append((char)n);
        httpConnection.getInputStream().close();
        httpConnection.disconnect();
        if(httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
          retry = 0;
          retryCountCallback.onRetry(0);
          Log.i("CCU_HCLIENT","Response " + s.toString());
          return s.toString();
        }
        }catch (Exception e){
          if(e instanceof SocketTimeoutException){
            // don't throw exception. Do retry
          }
          else if(e instanceof InterruptedIOException){
            throw new NullHGridException(e.getMessage());
          }
          Log.i("CCU_REPLACE","Exception occurred while hitting "+uriStr);
          Log.i("CCU_REPLACE", "Retry "+Log.getStackTraceString(e));
        }
        finally {
        }
        retry++;
        retryCountCallback.onRetry(retry);
        isOKResponse = false;

      }while(retry < maxRetryCount);
    }
    return null;
  }
  /**
   * This is a copy of httpClient present in HttpUtil class.
   * We are creating another client here to make use of a separate thread pool to support more
   * async operation at the cost of more resources.
   * @return
   */
  private static OkHttpClient getOkHttpClient(){
    if (okHttpClient == null) {
      okHttpClient = new OkHttpClient.Builder()
              .build();
    }
    return okHttpClient;
  }

  @NotNull
  private static Retrofit getRetrofitForHaystackBaseUrl(URL url, OkHttpClient okHttpClient) {
    return new Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
  }

  private static HttpURLConnection openConnection(String urlParameters, URL url, String targetURL, String httpMethod) {
    RequestBody requestBody = RequestBody.create(MediaType.parse("text/zinc"), urlParameters);
    OkHttpClient httpClient = getOkHttpClient();
    SiloApiService siloApiService = getRetrofitForHaystackBaseUrl(url, httpClient).create(SiloApiService.class);
    retrofit2.Call<ResponseBody> call = null;

    try {
      switch (httpMethod) {
        case HttpConstants.HTTP_METHOD_POST:
          call = siloApiService.postData(
                  targetURL,
                  requestBody
          );
          break;
        case HttpConstants.HTTP_METHOD_PUT:
          call = siloApiService.putData(
                  targetURL,
                  requestBody
          );
          break;
        case HttpConstants.HTTP_METHOD_GET:
          call = siloApiService.getData(
                  targetURL
          );
          break;
      }

      return (HttpURLConnection) call.request().url().url().openConnection();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

  }

  private boolean isHeartBeatPoint(String filterKey, String pointId) {
    HashMap<Object, Object> pointMap = CCUHsApi.getInstance().readMapById(pointId);
    if (pointMap != null && pointMap.get(filterKey) != null) {
      return true;
    }
    return false;
  }

}
