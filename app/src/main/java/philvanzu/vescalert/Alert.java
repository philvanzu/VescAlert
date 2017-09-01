package philvanzu.vescalert;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Vibrator;

import java.io.IOException;
import java.io.Serializable;

import static java.util.UUID.randomUUID;

/**
 * Created by philippe on 30/08/2017.
 */

public class Alert implements Parcelable, Serializable, Comparable<Alert>
{
    public static transient boolean playing;

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Alert createFromParcel(Parcel in) {
            return new Alert(in);
        }
        public Alert[] newArray(int size) {
            return new Alert[size];
        }
    };

    public String guid = randomUUID().toString();
    public MonitoredValue monitoredValue;
    public float threshold;
    public boolean playSound;
    public boolean vibrate;
    public int retrigger;


    public transient boolean triggered;
    public transient long triggerTime;

    public Alert(MonitoredValue monitoredValue)
    {
        this.monitoredValue = monitoredValue;
    }

    public Alert(Parcel in)
    {
        guid = in.readString();
        monitoredValue = (MonitoredValue)in.readParcelable(MonitoredValue.class.getClassLoader());
        threshold = in.readFloat();
        playSound = (in.readInt() == 0) ? false : true;
        vibrate = (in.readInt() == 0)? false : true;
        retrigger = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(guid);
        dest.writeParcelable((Parcelable) monitoredValue, flags);
        dest.writeFloat(threshold);
        dest.writeInt(playSound ? 1:0);
        dest.writeInt(vibrate?1:0);
        dest.writeInt(retrigger);
    }

    public String toString()
    {
        return new String(monitoredValue.name + " " + monitoredValue.triggerComparator + " " + Float.toString(threshold) + monitoredValue.unit);
    }


    public void Trigger(AlertPlayerThread mpThread)
    {
        triggered = true;
        triggerTime = System.currentTimeMillis();

        Bundle b = new Bundle();

        Message m = new Message();
        m.what = AlertPlayerThread.ACTION_PLAY;
        b.putParcelable("Alert", this);

        m.setData(b);
        mpThread.getHandler().sendMessage(m);


    }

    public void ResetTriggers()
    {
        triggered = false;
        triggerTime = 0;
    }

    public int compareTo(Alert o)
    {
        if(monitoredValue.triggerComparator == "Higher than") return Float.compare(o.threshold, this.threshold);
        return Float.compare(this.threshold, o.threshold);
    }

    public static void PlaySound(Context context, String soundFilename)
    {
        if(playing) return;
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(soundFilename);
            Bundle bundle = new Bundle();
            bundle.putParcelable("test", (Parcelable) afd);
            final MediaPlayer player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),afd.getLength());
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            player.release();
                            playing = false;
                        }
                    }
            );
            player.prepare();
            player.start();
            playing = true;

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void Vibrate(Context context, long[] pattern)
    {
        Vibrator v = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
        v.vibrate(pattern, -1);
    }

}