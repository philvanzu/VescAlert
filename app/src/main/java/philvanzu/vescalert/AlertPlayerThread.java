package philvanzu.vescalert;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import static android.speech.tts.Voice.QUALITY_VERY_HIGH;

/**
 * Created by philippe on 29/08/2017.
 */

public class AlertPlayerThread extends HandlerThread implements TextToSpeech.OnInitListener
{

    Handler mHandler;

    public static final String earcon_utterance_id = "VescAlert earcon";
    public static final String speech_utterance_id = "VescAlert speaking";
    public static final String init_utterance_id = "ttsinit";

    public static final int ACTION_PLAY = 103248;

    private boolean playing;
    public Context context;
    private TextToSpeech tts;
    boolean ttsready = false;

    public AlertPlayerThread(Context context)
    {
        super("VescAlarmAlertPlayerThread");
        this.context = context;
        tts = new TextToSpeech(context, this);
        tts.addEarcon(context.getResources().getResourceName(R.raw.happyending),"philvanzu.vescalert", R.raw.happyending);
        tts.addEarcon(context.getResources().getResourceName(R.raw.alarmbuzzer),"philvanzu.vescalert", R.raw.alarmbuzzer);
        tts.addEarcon(context.getResources().getResourceName(R.raw.theresafiresomewhere),"philvanzu.vescalert", R.raw.theresafiresomewhere);


    }
    public void Stop()
    {
        if(playing) interrupt();
        tts.shutdown();
        quit();
    }

    public Handler getHandler()
    {
        return mHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mHandler = new Handler(getLooper())
        {


            @Override
            public void handleMessage(Message msg) {
                try{
                    if (msg.what == ACTION_PLAY) {
                        if(playing) return;
                        Alert alert = msg.getData().getParcelable("Alert");
                        int duration = 2000;
                        long pattern[] = new long[]{0, duration};

                        while(!ttsready)
                        {
                            sleep(50);
                        }

                        playing = true;
                        if(alert.vibrate) {
                            Vibrator v = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
                            v.vibrate(pattern, -1);
                        }
                        HashMap<String, String> params = new HashMap<String, String>();
                        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                        if(alert.playSound) {
                            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, earcon_utterance_id);
                            params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(audio.STREAM_MUSIC));
                            tts.playEarcon(context.getResources().getResourceName(alert.monitoredValue.earcon), TextToSpeech.QUEUE_FLUSH, params);
                            params.clear();
                        }
                        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, speech_utterance_id);
                        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(audio.STREAM_MUSIC));
                        tts.speak(alert.toString(), TextToSpeech.QUEUE_ADD, params);
                        while(playing) Thread.sleep(50);
                    }

                }
                catch(InterruptedException e){
                    if(tts.isSpeaking())tts.stop();
                }
            }


        };
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            tts.setPitch(0.85f); // set pitch level
            tts.setSpeechRate(0.9f); // set speech speed rate
            //tts.setVoice(tts.getVoice(QUALITY_VERY_HIGH));

            HashMap<String, String> params = new HashMap<String, String>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, init_utterance_id);
            tts.playSilence (1,TextToSpeech.QUEUE_FLUSH, params);

            tts.setOnUtteranceProgressListener(
                    new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {

                        }

                        @Override
                        public void onDone(String utteranceId) {
                            if(utteranceId.equals(speech_utterance_id)) playing = false;
                            else if (utteranceId.equals(init_utterance_id))ttsready = true;
                        }

                        @Override
                        public void onError(String utteranceId) {
                            if(utteranceId.equals(speech_utterance_id))playing = false;
                        }
                    }
            );
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) Log.e("TTS", "Language is not supported");
        } else {
            Log.e("TTS", "Initilization Failed");
        }

    }

}
