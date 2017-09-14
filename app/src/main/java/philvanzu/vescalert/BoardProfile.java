package philvanzu.vescalert;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

import static java.util.UUID.randomUUID;

/**
 * Created by philippe on 27/08/2017.
 */

public class BoardProfile implements Parcelable, Serializable {



    public String guid;
    public String name;
    public boolean dualVesc;
    public int cellsInSerie;
    public int wheelPulley;
    public int motorPulley;
    public int wheelDiameter;
    public int motorPoles;

    private Float transmission;

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public BoardProfile createFromParcel(Parcel in) {
            return new BoardProfile(in);
        }
        public BoardProfile[] newArray(int size) {
            return new BoardProfile[size];
        }
    };

    public BoardProfile() {
        guid = randomUUID().toString();
    }

    public BoardProfile(Parcel in)
    {
        guid = in.readString();
        name = in.readString();
        dualVesc = in.readInt() != 0;
        cellsInSerie = in.readInt();
        wheelPulley = in.readInt();
        motorPulley = in.readInt();
        wheelDiameter = in.readInt();
        motorPoles = in.readInt();
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(guid);
        dest.writeString(name);
        dest.writeInt((dualVesc)? 1 : 0);
        dest.writeInt(cellsInSerie);
        dest.writeInt(wheelPulley);
        dest.writeInt(motorPulley);
        dest.writeInt(wheelDiameter);
        dest.writeInt(motorPoles);
    }

    public float getTransmission()
    {
        if(transmission == null)transmission = (float)motorPulley / (float)wheelPulley;
        return transmission;
    }
}
