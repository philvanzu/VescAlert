package philvanzu.vescalert;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

public class CreateAlertActivity extends AppCompatActivity {
    EditText thresholdEditText;
    EditText retriggerEditText;
    CheckBox playSoundCheckBox;
    CheckBox vibrateCheckBox;
    CheckBox retriggerCheckBox;
    TextView introTextView;
    TextView unitTextView;

    Alert alert = null;
    MonitoredValue monitoredValue=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_alert);


        Intent intent = getIntent();
        if(intent.hasExtra("Alert"))
        {
            alert = intent.getExtras().getParcelable("Alert");
            monitoredValue = intent.getExtras().getParcelable("MonitoredValue");
        }

        setTitle("Create offscreen feedback");
        introTextView = (TextView)findViewById(R.id.alertIntro_textView);
        unitTextView = (TextView)findViewById(R.id.unit_textView);
        thresholdEditText = (EditText)findViewById(R.id.threshold_editText);
        retriggerCheckBox = (CheckBox)findViewById(R.id.retrigger_checkBox);
        retriggerEditText = (EditText)findViewById(R.id.retrigger_editText);
        playSoundCheckBox = (CheckBox)findViewById(R.id.playsound_checkBox);
        vibrateCheckBox = (CheckBox)findViewById(R.id.vibrate_checkbox);

        if(alert != null )
        {
            introTextView.setText(alert.monitoredValue.intro);
            unitTextView.setText(alert.monitoredValue.unit);
            thresholdEditText.setText(Float.toString(alert.threshold));
            playSoundCheckBox.setChecked(alert.playSound);
            vibrateCheckBox.setChecked(alert.vibrate);
            boolean retrigger = alert.retrigger != -1;
            retriggerCheckBox.setChecked(retrigger);
            retriggerEditText.setText(retrigger?Integer.toString(alert.retrigger):"");
        }
        else if(monitoredValue != null)
        {
            alert = new Alert(monitoredValue);
            introTextView.setText(alert.monitoredValue.intro);
            unitTextView.setText(alert.monitoredValue.unit);
        }
        else finish();
    }


    public void BackButtonPressed(View v)
    {
        finish();
    }

    public void CreateButtonPressed(View v)
    {
        alert.threshold = Float.parseFloat(thresholdEditText.getText().toString());
        alert.playSound = playSoundCheckBox.isChecked();
        alert.vibrate = vibrateCheckBox.isChecked();
        alert.retrigger = (retriggerCheckBox.isChecked())? Integer.parseInt(retriggerEditText.getText().toString()) : -1;
        Intent resultIntent = new Intent();
        resultIntent.putExtra("Alert", (Parcelable) alert);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
