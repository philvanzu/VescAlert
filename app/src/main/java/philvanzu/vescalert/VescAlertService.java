package philvanzu.vescalert;


import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import android.app.NotificationManager;

public class VescAlertService extends Service {

    //interface implemented by MainActivity
    public interface VescUpdateListener {

        public void onVescStatusUpdate(VescStatus status, long startTime);
        public void onParserThreadFailure(int errorCode);
    }

    public class LocalBinder extends Binder {

        public VescUpdateListener mListener;

        public VescAlertService getService() {
            return VescAlertService.this;
        }

        public void setListener(VescUpdateListener listener) {
            mListener = listener;
        }
    }

    public static final int FOREGROUND_SERVICE = 101;
    public static final String CREATE_ACTION = "CreateService";
    public static final String START_ACTION = "StartService";
    public static final String STOP_ACTION = "StopService";
    public static final String MAIN_ACTION = "ShowMainActivity";
    public static final String SETPROFILE_ACTION = "LoadProfiles";
    public static final String LOADALERTS_ACTION = "LoadMonitoredValues";

    private static final String LOG_TAG = "VescAlertService";

    public static boolean IS_SERVICE_CREATED = false;
    public static boolean IS_SERVICE_RUNNING = false;
    public static boolean IS_SERVICE_BOUND = false;

    ParserThread parserThread;
    AlertPlayerThread playerThread;

    public HashMap<String, MonitoredValue> monitoredValues = new HashMap<String, MonitoredValue>();
    private LocalBinder binder = new LocalBinder();
    private long startTime;
    private BoardProfile profile;

    private String AlertMsg;
    private NotificationCompat.Builder mBuilder;
    String ns = Context.NOTIFICATION_SERVICE;
    NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        monitoredValues = MonitoredValue.getValues(this);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(START_ACTION))
        {
            profile = intent.getExtras().getParcelable("Profile");
            Log.i(LOG_TAG, "Received Start Foreground Intent ");
            showNotification();
            //Toast.makeText(this, "Service Started!", Toast.LENGTH_SHORT).show();
            if(parserThread == null)parserThread = new ParserThread( this);
            if(parserThread.getState() == Thread.State.NEW) parserThread.start();
            else if(parserThread.IsPaused()) parserThread.SetPaused(false);

            if(playerThread == null)
            {
                playerThread = new AlertPlayerThread(this);
                playerThread.start();
            }

            startTime = System.currentTimeMillis();

            for(Map.Entry<String, MonitoredValue> entry : monitoredValues.entrySet()) {
                String key = entry.getKey();
                MonitoredValue value = entry.getValue();

                value.ResetTriggers();
            }

        }
        else if (intent.getAction().equals(STOP_ACTION))
        {
            Log.i(LOG_TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            if(parserThread.getState()!= Thread.State.TERMINATED && !parserThread.IsPaused() ) parserThread.SetPaused(true);
            stopSelf();

        }
        else if (intent.getAction().equals(LOADALERTS_ACTION))
        {
            if(intent.hasExtra("AlertsUpdated") && intent.getExtras().getBoolean("AlertsUpdated"))
                monitoredValues = MonitoredValue.getValues(this);

        }
        else if (intent.getAction().equals(SETPROFILE_ACTION))
        {
            if(intent.hasExtra("BoardProfile")) profile = intent.getExtras().getParcelable("BoardProfile");
        }
        return START_STICKY;
    }

    private void showNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.notification);

        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("MonitoredValue")
                .setTicker("MonitoredValue Ticker")
                .setContentText("")
                .setSmallIcon(R.drawable.notification_24)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true);
        startForeground(FOREGROUND_SERVICE, mBuilder.build());
        notificationManager = (NotificationManager) this.getSystemService(ns);
    }

    @Override
    public void onDestroy() {
        if(parserThread != null && parserThread.getState()!= Thread.State.TERMINATED ) parserThread.interrupt();
        if(playerThread != null) playerThread.Stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        IS_SERVICE_BOUND = true;
        return binder;
    }

    @Override
    public void onRebind(Intent intent)
    {
        IS_SERVICE_BOUND = true;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        IS_SERVICE_BOUND = false;
        return true;
    }

    public void onParserThreadFailure(int errorCode)
    {
        if(binder.mListener != null) binder.mListener.onParserThreadFailure(errorCode);
    }

    public void onVescStatusUpdate(VescStatus status)
    {
        if(binder.mListener != null) binder.mListener.onVescStatusUpdate(status, startTime);
        if(status == null) return;

        for(Map.Entry<String, MonitoredValue> entry : monitoredValues.entrySet()) { //foreach entry in monitoredValues hashmap
            String key = entry.getKey();
            MonitoredValue monitored = entry.getValue();

            String result = monitored.onVescStatusUpdated(status, playerThread, profile);
            if(result != "")
            {
                mBuilder.setContentText(monitored.toString());
                notificationManager.notify(FOREGROUND_SERVICE, mBuilder.build());
            }
        }
    }
}
