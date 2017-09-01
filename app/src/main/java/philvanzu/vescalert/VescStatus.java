package philvanzu.vescalert;

/**
 * Created by philippe on 23/08/2017.
 */

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;
import java.util.Calendar;

/**
 * Created by philippe on 23/08/2017.
 * COMM_GET_vALUEs packet structure : https://github.com/vedderb/bldc_uart_comm_stm32f4_discovery/blob/master/bldc_interface.c#L163
 */

public class VescStatus implements Parcelable {

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public VescStatus createFromParcel(Parcel in) {
            return new VescStatus(in);
        }
        public VescStatus[] newArray(int size) {
            return new VescStatus[size];
        }
    };

    public float temp_mos1;
    public float temp_mos2;
    public float temp_mos3;
    public float temp_mos4;
    public float temp_mos5;
    public float temp_mos6;
    public float temp_pcb;
    public float current_motor;
    public float current_in;
    public float duty_now;
    public float rpm;
    public float v_in;
    public float amp_hours;
    public float amp_hours_charged;
    public float watt_hours;
    public float watt_hours_charged;
    public int tachometer;
    public int tachometer_abs;
    public int fault_code;

    public Calendar timestamp;


    public VescStatus(byte[] data, long time)
    {
        timestamp = Calendar.getInstance();
        timestamp.setTimeInMillis(time);

        int tmp = 0;

        temp_mos1 = ((float)(ByteBuffer.wrap(data, 1, 2).getChar())) / 10;
        temp_mos2 = ((float)(ByteBuffer.wrap(data, 3, 2).getChar())) / 10;
        temp_mos3 = ((float)(ByteBuffer.wrap(data, 5, 2).getChar())) / 10;
        temp_mos4 = ((float)(ByteBuffer.wrap(data, 7, 2).getChar())) / 10;
        temp_mos5 = ((float)(ByteBuffer.wrap(data, 9, 2).getChar())) / 10;
        temp_mos6 = ((float)(ByteBuffer.wrap(data, 11, 2).getChar())) / 10;
        temp_pcb = ((float)(ByteBuffer.wrap(data, 13, 2).getChar())) / 10;

        current_motor = ((float)(ByteBuffer.wrap(data, 15, 4).getInt())) / 100;
        current_in = ((float)(ByteBuffer.wrap(data, 19, 4).getInt())) / 100;

        duty_now = ((float)(ByteBuffer.wrap(data, 23, 2).getChar())) / 10;

        rpm = ByteBuffer.wrap(data, 25, 4).getInt();

        v_in = ((float)(ByteBuffer.wrap(data, 29, 2).getChar())) / 100;

        amp_hours = ((float)(ByteBuffer.wrap(data, 31, 4).getInt())) / 10000;
        amp_hours_charged = ((float)(ByteBuffer.wrap(data, 35, 4).getInt())) / 10000;

        watt_hours = ((float)(ByteBuffer.wrap(data, 39, 4).getInt())) / 10000;
        watt_hours_charged = ((float)(ByteBuffer.wrap(data, 43, 4).getInt())) / 10000;

        tachometer = ByteBuffer.wrap(data, 47, 4).getInt();
        tachometer_abs = ByteBuffer.wrap(data, 51, 4).getInt();

        fault_code = data[55];
    }

    private VescStatus(Parcel in)
    {
        this.timestamp = Calendar.getInstance();
        timestamp.setTimeInMillis(in.readLong());
        temp_mos1 = in.readFloat( );
        temp_mos2 = in.readFloat( );
        temp_mos3 = in.readFloat();
        temp_mos4 = in.readFloat();
        temp_mos5 = in.readFloat();
        temp_mos6 = in.readFloat();
        temp_pcb = in.readFloat();
        current_motor = in.readFloat();
        current_in = in.readFloat();
        duty_now = in.readFloat();
        rpm = in.readFloat();
        v_in = in.readFloat();
        amp_hours = in.readFloat();
        amp_hours_charged = in.readFloat();
        watt_hours = in.readFloat();
        watt_hours_charged = in.readFloat();
        tachometer = in.readInt ();
        tachometer_abs = in.readInt ();
        fault_code = in.readInt ();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.timestamp.getTimeInMillis());
        dest.writeFloat( temp_mos1);
        dest.writeFloat( temp_mos2);
        dest.writeFloat( temp_mos3);
        dest.writeFloat( temp_mos4);
        dest.writeFloat( temp_mos5);
        dest.writeFloat( temp_mos6);
        dest.writeFloat( temp_pcb);
        dest.writeFloat( current_motor);
        dest.writeFloat( current_in);
        dest.writeFloat( duty_now);
        dest.writeFloat( rpm);
        dest.writeFloat( v_in);
        dest.writeFloat( amp_hours);
        dest.writeFloat( amp_hours_charged);
        dest.writeFloat( watt_hours);
        dest.writeFloat( watt_hours_charged);
        dest.writeInt (tachometer);
        dest.writeInt (tachometer_abs);
        dest.writeInt (fault_code);
    }

    public String getFaultCode()
    {
        switch(fault_code)
        {
            case 1: return "FAULT_CODE_OVER_VOLTAGE";
            case 2: return "FAULT_CODE_UNDER_VOLTAGE";
            case 3: return "FAULT_CODE_DRV8302";
            case 4: return "FAULT_CODE_ABS_OVER_CURRENT";
            case 5: return "FAULT_CODE_OVER_TEMP_FET";
            case 6: return "FAULT_CODE_OVER_TEMP_MOTOR";
            default:
            case 0: return "FAULT_CODE_NONE";
        }
    }

    public float getPower(BoardProfile profile)
    {
        return (duty_now/100) * current_motor * v_in * (profile != null && profile.dualVesc ? 2.0f : 1.0f);
    }

    public float getCellVoltage(BoardProfile profile)
    {
        if(profile == null) return 0;
        return v_in / profile.cellsInSerie;
    }

    public float getDistance(BoardProfile profile)
    {
        if(profile == null) return 0;
        return (((float)tachometer / (float)profile.motorPoles) * ((float)profile.motorPulley / (float)profile.wheelPulley) * (float)profile.wheelDiameter * 3.1416f) / 1000000f;
    }

    public float getSpeed(BoardProfile profile)
    {
        if(profile == null) return 0;
        return (((float)rpm / (float)profile.motorPoles) * ((float)profile.motorPulley / (float)profile.wheelPulley) * (float)profile.wheelDiameter * 3.1416f * 60 ) / 1000000f;
    }

    public int getBatteryCharge(BoardProfile profile)
    {
        if(profile == null) return 0;
        float ccv = v_in / profile.cellsInSerie;
        float cellmax = 4.1f;
        float cellnominalhigh = 3.9f;
        float cellnominallow = 3.6f;
        float cellmin = 3.2f;

        if(ccv >= cellmax) return 100;
        else if (ccv > cellnominalhigh)
        {
            float dif = cellmax - ccv;
            float prog = (dif / 0.2f) * 20;
            return (int)(100 - prog);
        }
        else if (ccv > cellnominallow)
        {
            float dif = cellnominalhigh - ccv;
            float prog = 20f + ((dif / 0.3f) * 65f);
            return (int)(100 - prog);
        }
        else if(ccv > cellmin)
        {
            float dif = cellnominallow - ccv;
            float prog = 85f + ((dif / 0.6f) * 15f);
            return (int)(100 - prog);
        }
        return 0;
    }
}
