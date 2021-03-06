package ch.hevs.aislab.magpie.android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Messenger;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import ch.hevs.aislab.magpie.agent.MagpieAgent;
import ch.hevs.aislab.magpie.context.ContextEntity;
import ch.hevs.aislab.magpie.environment.Environment;

public class MagpieService extends Service {

    /** Used for debugging */
    private final String TAG = getClass().getName();

    /** Shared Preferences store the names of the MagpieActivities that bounded to this Service */
    static final String MAGPIE_PREFS = "magpie_prefs";
    static final String MASTER_KEY = "MagpieActivitiesInApplication";

    /** Objects returned to the MagpieActivity for communicating with the Service and the Environment */
    private final IBinder mBinder = new MagpieBinder();
    private Messenger requestMessenger;

    /** Looper associated with the HandlerThread */
    private volatile Looper mServiceLooper;

    private volatile Environment mEnvironment;

    /** Last binding activity */
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");

        // Create and start a background HandlerThread since by default a Service
        // runs in the UI Thread, which we don't want to block
        HandlerThread thread = new HandlerThread("EnvironmentService");
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mEnvironment = new Environment(mServiceLooper, this);

        requestMessenger = new Messenger(mEnvironment);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");

        String action = intent.getAction();
        if (action.equals(MagpieActivity.ACTION_ONE_WAY_COMM)) {
            return mBinder;
        } else if (action.equals(MagpieActivity.ACTION_TWO_WAY_COMM)) {
            return requestMessenger.getBinder();
        } else {
            Log.e(TAG, "MagpieService received an intent without an action");
            return null;
        }
    }

    /**
     * Binder object returned to the caller
     */
    public class MagpieBinder extends Binder {
        public MagpieService getService() {
            return MagpieService.this;
        }
    }

    /**
     * Factory method to make an intent to connect with this service
     */
    public static Intent makeIntent(Context context) {
        mContext = context;
        return new Intent(context, MagpieService.class);
    }

    public static Context getContext() {
        return mContext;
    }

    /**
     * Actions that can be performed in the Environment from an Activity
     */
    public void registerAgent(MagpieAgent agent, String activityName) {
        mEnvironment.registerAgent(agent);

        SharedPreferences settings = getSharedPreferences(MAGPIE_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        Set<String> magpieActivities = settings.getStringSet(MASTER_KEY, new HashSet<String>());
        magpieActivities.add(activityName);
        editor.putStringSet(MASTER_KEY, magpieActivities);

        Set<String> agentNamesFromActivity = settings.getStringSet(activityName, new HashSet<String>());
        agentNamesFromActivity.add(agent.getName());
        editor.putStringSet(activityName, agentNamesFromActivity);

        editor.apply();

    }

    public void setBehaviorsContext(Context context, Set<String> agentNames) {
        mEnvironment.setBehaviorsContext(context, agentNames);
    }

    public void registerAgent(MagpieAgent agent) {
        mEnvironment.registerAgent(agent);
    }

    public ContextEntity getContextEntity(String service) {
        return mEnvironment.getContextEntity(service);
    }
}
