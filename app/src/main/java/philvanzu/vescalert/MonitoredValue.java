package philvanzu.vescalert;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


import static java.util.UUID.randomUUID;


/**
 * Created by philippe on 24/08/2017.
 */

public class MonitoredValue implements Parcelable, Serializable
{
    private static HashMap<String, MonitoredValue>  values;
    public static HashMap<String, MonitoredValue>  getValues(Context context)
    {
        if(values == null) values = loadMonitoredValues(context);
        if(values == null) values = createMonitoredValues();
        return values;
    }

    private static HashMap<String, MonitoredValue>  loadMonitoredValues(Context context)
    {
        HashMap<String, MonitoredValue>  list = null;
        try {
            FileInputStream fis = context.openFileInput("monitoredValues.data");
            ObjectInputStream is = new ObjectInputStream(fis);
            list = (HashMap<String, MonitoredValue> ) is.readObject();
            is.close();
            fis.close();
        }
        catch (IOException e){e.printStackTrace();}
        catch (ClassNotFoundException e) { e.printStackTrace(); }
        return list;
    }

    public static void saveMonitoredValues(Context context, HashMap<String, MonitoredValue>  monitoredValues)
    {
        values = monitoredValues;
        try {
            FileOutputStream fos = context.openFileOutput("monitoredValues.data", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject((Serializable) monitoredValues);
            os.close();
            fos.close();
        }
        catch (IOException e){e.printStackTrace();}
    }

    private static HashMap<String, MonitoredValue>  createMonitoredValues()
    {
        HashMap<String, MonitoredValue> list = new HashMap<String, MonitoredValue>();

        MonitoredValue battv = new MonitoredValue();
        battv.name = "Battery Voltage";
        battv.intro =  "Alert Triggered when Battery Voltage is Lower Than";
        battv.triggerComparator = "Lower than";
        battv.unit = "Volts";
        battv.earcon = R.raw.happyending;
        battv.alerts = new ArrayList<Alert>();
        list.put("battv", battv);

        MonitoredValue cellv = new MonitoredValue();
        cellv.name = "Cell Voltage";
        cellv.intro ="Alert Triggered when Cell Voltage is Lower Than";
        cellv.triggerComparator = "Lower than";
        cellv.unit = "Volts";
        cellv.earcon = R.raw.happyending;
        cellv.alerts = new ArrayList<Alert>();
        list.put("cellv", cellv);

        MonitoredValue charge = new MonitoredValue();
        charge.name = "Battery Charge";
        charge.intro = "Alert Triggered when Charge is Lower Than";
        charge.triggerComparator = "Lower than";
        charge.unit = "%";
        charge.earcon = R.raw.happyending;
        charge.alerts = new ArrayList<Alert>();
        list.put("charge", charge);

        MonitoredValue temp = new MonitoredValue();
        temp.name = "VESC Temperature";
        temp.intro = "Alert Triggered when VESC Temperature is Higher Than";
        temp.triggerComparator = "Higher than";
        temp.unit = "°C";
        temp.earcon = R.raw.theresafiresomewhere;
        temp.alerts = new ArrayList<Alert>();
        list.put("temp", temp);

        MonitoredValue fault = new MonitoredValue();
        fault.name = "Fault Code";
        fault.intro = "Alert Triggered when Fault Code is Higher Than";
        fault.triggerComparator = "Higher than";
        fault.unit = "";
        fault.earcon = R.raw.alarmbuzzer;
        fault.alerts = new ArrayList<Alert>();
        list.put("fault", fault);

        return list;
    }


    public String guid = randomUUID().toString();
    public String name;
    public String intro;
    public String unit;
    public ArrayList<Alert> alerts;
    public String triggerComparator;
    public int earcon;



    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public MonitoredValue createFromParcel(Parcel in) {
            return new MonitoredValue(in);
        }
        public MonitoredValue[] newArray(int size) {
            return new MonitoredValue[size];
        }
    };


    public MonitoredValue(){}

    public MonitoredValue(Parcel in)
    {
        guid = in.readString();
        name = in.readString();
        intro = in.readString();
        unit = in.readString();
        alerts = in.readArrayList(Alert.class.getClassLoader());
        triggerComparator = in.readString();
        earcon = in.readInt();

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(guid);
        dest.writeString(name);
        dest.writeString(intro);
        dest.writeString(unit);
        dest.writeList(alerts);
        dest.writeString(triggerComparator);
        dest.writeInt(earcon);
    }

    // this will not work as intended if the list of monitoredValues is not sorted in accordance to this.triggerComparator whenever it is updated.
    // Battery level => alert triggered when smaller than threshold. monitoredValues list needs to be sorted by threshold descending.
    // Temperature => alert triggered when larger than threshold. monitoredValues list needs to be sorted by threshold ascending.
    // Fault code :  alert triggered when threshold > 0 regardless. Should not have more than one alert.
    public String onVescStatusUpdated(VescStatus status, AlertPlayerThread playerThread, BoardProfile profile)
    {
        for(int i=0; i < alerts.size(); i++)
        {
            Alert alert  = alerts.get(i);

            if(alert.triggered && alert.retrigger < 0) break;
            else if(alert.triggered && System.currentTimeMillis() - alert.triggerTime < alert.retrigger) break;

            boolean trigger = false;

            switch(name)
            {
                case "Battery Charge": {
                    if( status.current_in < 1f && status.getBatteryCharge(profile) < alert.threshold )
                        trigger = true;
                    break;
                }
                case "Battery Voltage" : {
                    if( status.current_in < 1f && status.v_in < alert.threshold ) trigger = true;
                    break;
                }
                case "Cell Voltage":
                    if( status.current_in < 1f && status.getCellVoltage(profile) < alert.threshold ) trigger = true;
                    break;

                case "VESC Temperature" : {
                    if( status.temp_pcb > alert.threshold) trigger = true;
                    break;
                }

                case "Fault Code" :  {
                    if( status.fault_code != 0 ) trigger = true;
                    break;
                }
            }

            if(trigger)
            {
                alert.Trigger(playerThread);
                return alert.toString();
            }
        }
        return "";
    }

    void ResetTriggers()
    {
        for(int i = 0; i < alerts.size(); i++)alerts.get(i).ResetTriggers();
    }


    public String toString()
    {
        return name;
    }

    public void AddOrUpdateAlert(Alert alert)
    {
        for(int i = 0; i < alerts.size(); i++)
        {
            if(alerts.get(i).guid ==  alert.guid)
            {
                alerts.set(i, alert);
                Collections.sort(alerts);
                return;
            }
        }

        alerts.add(alert);
        Collections.sort(alerts);
    }

    public void RemoveAlert(int id)
    {
        alerts.remove(id);
        Collections.sort(alerts);
    }


}
