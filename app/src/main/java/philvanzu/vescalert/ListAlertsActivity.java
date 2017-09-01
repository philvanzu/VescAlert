package philvanzu.vescalert;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class ListAlertsActivity extends AppCompatActivity
{

    public class AlertsListAdapter extends ArrayAdapter<Alert>
    {
        LayoutInflater mLayoutInflater;
        ArrayList<Alert> mData;
        AlertsListAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull ArrayList<Alert> list)
        {

            super(context, resource, textViewResourceId, list);
            mData = list;
            mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.alertslist, null);
            }

            ImageView itemIcon = (ImageView)convertView.findViewById(R.id.alarmicon);
            itemIcon.setOnClickListener(new android.view.View.OnClickListener(){
                @Override
                public void onClick(View view)
                {
                    ListAlertsActivity.this.onItemClick(position);
                }
            });

            TextView itemTextView = (TextView) convertView.findViewById(R.id.Itemname);
            itemTextView.setText(mData.get(position).toString());
            itemTextView.setOnClickListener(new android.view.View.OnClickListener(){
                @Override
                public void onClick(View view)
                {
                    ListAlertsActivity.this.onItemClick(position);
                }
            });

            ImageButton buttonDelete = (ImageButton) convertView.findViewById(R.id.deleteAlertButton);
            buttonDelete.setOnClickListener(new android.view.View.OnClickListener(){
                @Override
                public void onClick( View view) {
                    ListAlertsActivity.this.onItemDeleteClick(position);
                }
            });
            return convertView;
        }


    }

    public static final int REQ_CREATEITEM = 0;
    public static final int REQ_EDITITEM = 1;

    public HashMap<String, MonitoredValue> monitoredValues = new HashMap<String, MonitoredValue>();
    public MonitoredValue currentTab;
    public ImageButton battvButton;
    public ImageButton cellvButton;
    public ImageButton chargeButton;
    public ImageButton tempButton;
    public ImageButton faultButton;
    public TextView tabTitleTextView;

    public HashMap<String, Integer> alertsMap = new HashMap<String, Integer>();

    ListView alertsListView;
    AlertsListAdapter alertsListViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_alerts);

        alertsListView = (ListView) findViewById(R.id.alertsListView);
        battvButton = (ImageButton)findViewById(R.id.battv_button);
        cellvButton = (ImageButton)findViewById(R.id.cellv_button);
        chargeButton = (ImageButton)findViewById(R.id.charge_button);
        tempButton = (ImageButton)findViewById(R.id.temp_button);
        faultButton = (ImageButton)findViewById(R.id.fault_button);
        tabTitleTextView = (TextView)findViewById(R.id.tabtitle);

        LoadMonitoredValues();
        currentTab = monitoredValues.get("fault");
        faultButton.setImageResource(R.drawable.fault_selected);
        tabTitleTextView.setText(currentTab.name + " Alerts");
        onAlertsModified();
    }
    public void onTabSelected(View v)
    {
        battvButton.setImageResource(R.drawable.battv);
        cellvButton.setImageResource(R.drawable.cellv);
        chargeButton.setImageResource(R.drawable.charge);
        tempButton.setImageResource(R.drawable.temp);
        faultButton.setImageResource(R.drawable.fault);

        ImageButton b = (ImageButton)v;
        if(b == battvButton) {
            currentTab = monitoredValues.get("battv");
            b.setImageResource(R.drawable.battv_selected);
        }
        else if (b == cellvButton) {
            currentTab = monitoredValues.get("cellv");
            b.setImageResource(R.drawable.cellv_selected);
        }
        else if (b == chargeButton) {
            currentTab = monitoredValues.get("charge");
            b.setImageResource(R.drawable.charge_selected);
        }
        else if (b == tempButton){
            currentTab = monitoredValues.get("temp");
            b.setImageResource(R.drawable.temp_selected);
        }
        else if (b == faultButton){
            currentTab = monitoredValues.get("fault");
            b.setImageResource(R.drawable.fault_selected);
        }
        tabTitleTextView.setText(currentTab.name + " Alerts");
        onAlertsModified();
    }
    
    void onAlertsModified()
    {
        alertsMap.clear();
        for(int i = 0; i < currentTab.alerts.size(); i++)alertsMap.put(currentTab.alerts.get(i).guid, i);

        alertsListViewAdapter = new AlertsListAdapter(this, R.layout.alertslist, R.id.Itemname, currentTab.alerts);
        alertsListView.setAdapter(alertsListViewAdapter);
    }


    public void LoadMonitoredValues()
    {
        monitoredValues = MonitoredValue.getValues(this);
    }

    private void SaveMonitoredValues() {
        MonitoredValue.saveMonitoredValues(this, monitoredValues);
    }


    public void onItemClick(int position)
    {
        Alert edit = currentTab.alerts.get(position);
        Intent myIntent = new Intent(this, CreateAlertActivity.class);
        myIntent.putExtra("Alert", (Parcelable) edit);
        myIntent.putExtra("MonitoredValue", (Parcelable)edit.monitoredValue);
        startActivityForResult(myIntent, REQ_EDITITEM);
    }

    public void onItemDeleteClick(int position)
    {
        currentTab.RemoveAlert(position);
        onAlertsModified();
        SaveMonitoredValues();
    }

    public void BackButtonPressed(View v)
    {

        finish();
    }

    public void CreateButtonPressed(View v)
    {
        Intent myIntent = new Intent(this, CreateAlertActivity.class);
        myIntent.putExtra("Alert", (Parcelable)null);
        myIntent.putExtra("MonitoredValue", (Parcelable)currentTab);
        startActivityForResult(myIntent, REQ_CREATEITEM);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (REQ_CREATEITEM) : {
                if (resultCode == Activity.RESULT_OK) {
                    Alert freshAlert = data.getParcelableExtra("Alert");
                    if(freshAlert != null)
                    {
                        currentTab.AddOrUpdateAlert(freshAlert);
                        onAlertsModified();
                        SaveMonitoredValues();
                    }
                }
                break;
            }
            case(REQ_EDITITEM):{
                if (resultCode == Activity.RESULT_OK) {
                    Alert freshAlert = data.getParcelableExtra("Alert");
                    if(freshAlert != null) {
                        Integer pos = alertsMap.get(freshAlert.guid);
                        if (pos != null) currentTab.alerts.set(pos, freshAlert );
                        else currentTab.alerts.add(freshAlert);

                        onAlertsModified();
                        SaveMonitoredValues();
                    }
                }
                break;
            }
        }
    }



    @Override
    protected void onResume()
    {
        super.onResume();

        LoadMonitoredValues();
    }

}
