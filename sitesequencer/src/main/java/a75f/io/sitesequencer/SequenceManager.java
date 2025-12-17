package a75f.io.sitesequencer;


import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.interfaces.ProfileConfigurationHandlerInterface;
import a75f.io.sitesequencer.cloud.ServiceGenerator;
import a75f.io.sitesequencer.cloud.SiteSequencerService;


public class SequenceManager
{
    private static final String TAG = SequencerParser.TAG_CCU_SITE_SEQUENCER;
    /** There will always be an instance, but there may be no service.  That occurs when
     * there is no bearer token.
     */
    private static SequenceManager mInstance;
    public static ProfileConfigurationHandlerInterface profileConfigurationHandler;

    // Set at creation & whenever base url changes b/c of user setting on local build.
    private String baseUrl;

    // Created each time baseUrl or token changes.
    private SiteSequencerService siteSequencerService = null;

    SiteSequencerRepository repo;

    private final Context appContext;

    private final HashMap<String, Object> persistentBlockMap= new HashMap<>();

    /**
     * Call this when apiBase changes.  Token should not be null, so please include current token.
     */
    public void setSequencerApiBase(String sequencerApiBase) {
        this.baseUrl = sequencerApiBase;

        this.siteSequencerService = ServiceGenerator.getInstance().createService(sequencerApiBase);
        repo = new SiteSequencerRepository(
                new SequenceDataStore(appContext),
                new SequencerParser(),
                siteSequencerService,
                CCUHsApi.getInstance()
        );
    }

    public SiteSequencerRepository getRepo(){ return repo; }

    public SiteSequencerService getSiteSequencerService() {
        return siteSequencerService;
    }

    /**
     * Please use this constructor (and not getInstance()) wherever possible to
     * avoid a crash if we haven't instantiated yet.
     *
     * @param appContext     application context (android)
     * @param sequencerApiBase  the base of the URL for sequencer service, e.g. "http://192.168.0.122:8087"
     */
    private SequenceManager(Context appContext, String sequencerApiBase, ProfileConfigurationHandlerInterface profileConfigurationHandler) {
        this.appContext = appContext;
        setSequencerApiBase(sequencerApiBase);
        this.profileConfigurationHandler = profileConfigurationHandler;
    }

    public static SequenceManager getInstance(Context c, String sequencerApiBase, ProfileConfigurationHandlerInterface profileConfigurationHandler) {
        if (mInstance == null) {
            mInstance = new SequenceManager(c, sequencerApiBase, profileConfigurationHandler);
        }
        if (!mInstance.hasService()) {
            mInstance.setSequencerApiBase(sequencerApiBase);
        }
        return mInstance;
    }

    /**
     * Please only call this from places you know registration is complete.
     * <p>
     * Otherwise there will be consequences.
     */
    public static SequenceManager getInstance()
    {
        if (mInstance == null)
        {
            throw new IllegalStateException("No SequenceManager instance found");
        }
        return mInstance;
    }

    public boolean hasService() {
        return siteSequencerService != null && repo != null;
    }


    public void fetchPredefinedSequencesIfEmpty() {
        CcuLog.d(SequencerParser.TAG_CCU_SITE_SEQUENCER, "fetchPredefinedSequencesIfEmpty repoCheck()-->" + repoCheck());
        if (! repoCheck()) return;
        repo.fetchSequencerDefssIfEmpty();
    }

    public void fetchPredefinedSequencesForCleanUp() {
        CcuLog.d(SequencerParser.TAG_CCU_SITE_SEQUENCER, "fetchPredefinedSequencesForCleanUp repoCheck()-->" + repoCheck());
        if (! repoCheck()) return;
        repo.fetchSequencerDefinitionsForCleanup();
    }

    public void addSequencerDefinition(SiteSequencerDefinition def) {
        if (! repoCheck()) return;
        repo.addSequencerDefinition(def);
    }

    public SiteSequencerDefinition getSequenceById(String id) {
        if (! repoCheck()) return null;
        return repo.getSequenceById(id);
    }

    public void deleteDefinition(String _id) {
        if (! repoCheck()) return;
        repo.deleteSequencerDefinition(_id);
    }

    public void deleteAlertsBySequenceId(SiteSequencerDefinition sequencerDefinition) {
        if (! repoCheck()) return;
        repo.cleanUpAlerts(sequencerDefinition);
    }

    public void fixAlertsBySequenceId(SiteSequencerDefinition sequencerDefinition) {
        if (! repoCheck()) return;
        repo.fixAlerts(sequencerDefinition);
    }

    public void removePendingIntent(String seqId){
        if (! repoCheck()) return;
        repo.removePendingIntent(seqId);
    }

    private boolean repoCheck() {
        if (repo == null) {
            CcuLog.d(SequencerParser.TAG_CCU_SITE_SEQUENCER, "Repository null (no service) in SequenceManager.  Returning.");
            return false;
        }
        return true;
    }

    public void initValue(String key, Object value) {
        persistentBlockMap.computeIfAbsent(key, k -> value);
    }

    public void putValue(String key, Object value) {
        persistentBlockMap.put(key, value);
    }

    public Object getValue(String key) {
        return persistentBlockMap.get(key);
    }

    public List<JsonElement> getSequenceIds(@NotNull JsonObject jsonObject) {
        if (jsonObject.has("ids")) {
            JsonArray ids = jsonObject.getAsJsonArray("ids");
            ids.asList().forEach(id -> {
                CcuLog.d(SequenceManager.TAG, "seq id-->" + id);
            });
            return ids.asList();
        }
        return null;
    }

    public HashMap<String, String> getSequenceIdsMap(@NotNull JsonObject jsonObject) {
        HashMap<String, String> hashMap = new HashMap();
        if (jsonObject.has("ids")) {
            JsonArray ids = jsonObject.getAsJsonArray("ids");
            ids.asList().forEach(id -> {
                CcuLog.d(SequenceManager.TAG, "seq id-->" + id);
                hashMap.put(id.getAsString(), id.getAsString());
            });
        }
        return hashMap;
    }

    public String getSequenceId(@NotNull JsonObject jsonObject) {
        if (jsonObject.has("ids")) {
            String id = jsonObject.get("id").getAsString();
            return id;
        }
        return null;
    }
}