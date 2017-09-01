package philvanzu.vescalert;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.ServiceConnection;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements VescAlertService.VescUpdateListener {


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mBoundService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            VescAlertService.LocalBinder binder = (VescAlertService.LocalBinder) service;
            mBoundService = binder.getService();
            binder.setListener(MainActivity.this);

            mBound = true;
        }
    };

    private static final int REQ_EDITPROFILES = 125;
    private static final int REQ_EDITALERTS = 251;

    boolean mBound;
    VescAlertService mBoundService;

    TextView runtime_tv;
    TextView vin_tv;
    TextView temp_tv;
    TextView motorCurrent_tv;
    TextView batteryCurrent_tv;
    TextView dutyCycle_tv;
    TextView rpm_tv;
    TextView amph_tv;
    TextView watth_tv;
    TextView tacho_tv;
    TextView fault_tv;
    TextView cellv_tv;
    TextView charge_tv;
    TextView speed_tv;
    TextView distance_tv;
    TextView power_tv;

    ArrayList<BoardProfile> profiles;
    ArrayList<String> profilenames;
    ArrayAdapter<String> adapterProfiles;
    Spinner spinnerProfiles;
    BoardProfile selectedProfile;

    Button startButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setSubtitle("Using ToolBar");

        startButton = (Button) findViewById(R.id.startbutton);

        //Create the service if not done yet
        if(VescAlertService.IS_SERVICE_CREATED == false) {
            Intent service = new Intent(MainActivity.this, VescAlertService.class);
            VescAlertService.IS_SERVICE_CREATED = true;
            service.setAction(VescAlertService.CREATE_ACTION);
            startButton.setText("Start Service");
            startService(service);
        }
        //Update button text if already running.
        else if (VescAlertService.IS_SERVICE_RUNNING)
        {
            startButton.setText("Stop Service");
        }

        runtime_tv = (TextView) findViewById(R.id.runtime_tv);
        vin_tv = (TextView)findViewById(R.id.vin_tv);
        temp_tv = (TextView)findViewById(R.id.temp_tv);
        motorCurrent_tv = (TextView)findViewById(R.id.motorCurrent_tv);
        batteryCurrent_tv = (TextView)findViewById(R.id.batteryCurrent_tv);
        dutyCycle_tv = (TextView)findViewById(R.id.dutycycle_tv);
        rpm_tv = (TextView)findViewById(R.id.rpm_tv);
        amph_tv = (TextView)findViewById(R.id.amph_tv);
        watth_tv = (TextView)findViewById(R.id.watth_tv);
        tacho_tv = (TextView)findViewById(R.id.tacho_tv);
        fault_tv = (TextView)findViewById(R.id.fault_tv);
        cellv_tv = (TextView)findViewById(R.id.cellv_tv);
        charge_tv = (TextView)findViewById(R.id.charge_tv);
        speed_tv = (TextView)findViewById(R.id.speed_tv);
        distance_tv = (TextView)findViewById(R.id.distance_tv);
        power_tv = (TextView)findViewById(R.id.power_tv);

        profiles = new ArrayList<BoardProfile>();
        profilenames = new ArrayList<String>();
        LoadProfilesFromStorage();

    }


    //start / stop the service
    public void ServiceButtonClicked(View v)
    {
        Button button = (Button) v;
        Intent serviceAction = new Intent(MainActivity.this, VescAlertService.class);
        if (!VescAlertService.IS_SERVICE_RUNNING) {
            serviceAction.putExtra("Profile", (Parcelable)selectedProfile);
            serviceAction.setAction(VescAlertService.START_ACTION);
            VescAlertService.IS_SERVICE_RUNNING = true;
            button.setText("Stop Service");
        } else {
            serviceAction.setAction(VescAlertService.STOP_ACTION);
            VescAlertService.IS_SERVICE_RUNNING = false;
            button.setText("Start Service");

        }
        startService(serviceAction);
    }

    @Override
    public void onPause()
    {
        if(mBound) unbindService(mConnection);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = new Intent(this, VescAlertService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy()
    {

/*
        if (VescAlertService.IS_SERVICE_RUNNING) {
            Intent service = new Intent(MainActivity.this, VescAlertService.class);
            service.setAction(VescAlertService.STOP_ACTION);
            VescAlertService.IS_SERVICE_RUNNING = false;
            startService(service);
        }
*/


        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //notify the service that alerts have been modified
        if (resultCode == Activity.RESULT_OK && requestCode == REQ_EDITALERTS) {
            Intent intent = new Intent(this, VescAlertService.class);
            intent.setAction(VescAlertService.LOADALERTS_ACTION);
            intent.putExtra("AlertsUpdated", true);
            startService(intent);
        }
        //board profiles modified, need to reload them
        else if (resultCode == Activity.RESULT_OK && requestCode == REQ_EDITPROFILES) {
            LoadProfilesFromStorage();
        }
    }


    public void AlarmsButtonPressed(View v)
    {
        Intent myIntent = new Intent(this, ListAlertsActivity.class);
        startActivityForResult(myIntent, REQ_EDITALERTS);
    }

    public void ProfilesButtonPressed(View v)
    {
        Intent myIntent = new Intent(this, ListBoardProfilesActivity.class);
        startActivityForResult(myIntent, REQ_EDITPROFILES);
    }

    public void onParserThreadFailure(int errorCode)
    {

        if(errorCode == ParserThread.FAIL_LogUnavailable) {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            dlgAlert.setMessage("MonitoredValue cannot read InternalStorage/btsnoop_hci.log. " +
                    "Please activate the log in android's developer options then open your vesc monitoring app " +
                    "and start communications with a VESC before starting this application.");
            dlgAlert.setTitle("App Title");
            dlgAlert.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //dismiss the dialog
                        }
                    });
            dlgAlert.setCancelable(true);
            dlgAlert.create().show();
        }

        if(mBound) unbindService(mConnection);
        Intent service = new Intent(MainActivity.this, VescAlertService.class);
        service.setAction(VescAlertService.STOP_ACTION);
        VescAlertService.IS_SERVICE_RUNNING = false;
        startService(service);
    }

    public void onVescStatusUpdate(VescStatus status, long startTime)
    {
        long millis = System.currentTimeMillis() - startTime;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        runtime_tv.setText(String.format("%d:%02d", minutes, seconds));
        if(status != null)
        {
            vin_tv.setText(Float.toString(status.v_in) + " V.");
            temp_tv.setText(Float.toString(status.temp_pcb) + " C°.");
            motorCurrent_tv.setText(Float.toString(status.current_motor) + " A.");
            batteryCurrent_tv.setText(Float.toString(status.current_in) + " A.");
            dutyCycle_tv.setText(Float.toString(status.duty_now) + " %");
            rpm_tv.setText(Float.toString(status.rpm) );
            amph_tv.setText(Float.toString(status.amp_hours) + " A/h");
            watth_tv.setText(Float.toString(status.watt_hours) + " W/h");
            tacho_tv.setText(Integer.toString(status.tachometer) + " ER");
            fault_tv.setText(status.getFaultCode());
            cellv_tv.setText(Float.toString(status.getCellVoltage(selectedProfile)) + " V.");
            charge_tv.setText(Integer.toString(status.getBatteryCharge(selectedProfile)) + " %");
            power_tv.setText(Float.toString(status.getPower(selectedProfile))+ " W.");
            speed_tv.setText(Float.toString(status.getSpeed(selectedProfile))+ " Km/h.");
            distance_tv.setText(Float.toString(status.getDistance(selectedProfile))+ " Km.");
        }
    }

    public void LoadProfilesFromStorage()
    {
        try {
            FileInputStream fis = this.openFileInput("profiles.data");
            ObjectInputStream is = new ObjectInputStream(fis);
            profiles = (ArrayList<BoardProfile>) is.readObject();
            is.close();
            fis.close();
        }
        catch (IOException e){e.printStackTrace();}
        catch (ClassNotFoundException e) { e.printStackTrace(); }

        profilenames.clear();
        for(int i = 0; i < profiles.size(); i++) {
            BoardProfile profile = profiles.get(i);
            profilenames.add(profile.name);
        }

        spinnerProfiles = (Spinner)findViewById(R.id.spinnerBoardProfiles);
        adapterProfiles = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, profilenames);
        adapterProfiles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProfiles.setAdapter(adapterProfiles);
        //notify the service when profile selected.
        spinnerProfiles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
            {
                selectedProfile = MainActivity.this.profiles.get(position);
                Intent intent = new Intent(MainActivity.this, VescAlertService.class);
                intent.setAction(VescAlertService.SETPROFILE_ACTION);
                intent.putExtra("BoardProfile", (Parcelable)selectedProfile);
                startService(intent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView)
            {
                selectedProfile = null;
            }
        });
    }
}
