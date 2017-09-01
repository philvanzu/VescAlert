package philvanzu.vescalert;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class CreateBoardProfileActivity extends AppCompatActivity {

    EditText profileNameET;
    EditText cellsSerieET;
    EditText wheelPulleyET;
    EditText motorPulleyET;
    EditText motorPolesET;
    EditText whellDiameterET;
    CheckBox DualVescCB;

    BoardProfile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_board_profile);

        profileNameET = (EditText) findViewById(R.id.editTextProfileName);
        cellsSerieET = (EditText) findViewById(R.id.editTextCellsSerie);
        wheelPulleyET = (EditText) findViewById(R.id.editTextWheelPulley);
        motorPolesET = (EditText) findViewById(R.id.editTextMotorPoles);
        motorPulleyET = (EditText) findViewById(R.id.editTextMotorPulley);
        whellDiameterET = (EditText) findViewById(R.id.editTextWheelDiameter);
        DualVescCB = (CheckBox) findViewById(R.id.checkBoxDualVesc);

        Intent intent = getIntent();
        if(intent.hasExtra("BoardProfile"))
        {
            profile = intent.getExtras().getParcelable("BoardProfile");
            profileNameET.setText(profile.name);
            cellsSerieET.setText(Integer.toString(profile.cellsInSerie));
            whellDiameterET.setText(Integer.toString(profile.wheelDiameter));
            wheelPulleyET.setText(Integer.toString(profile.wheelPulley));
            motorPulleyET.setText(Integer.toString(profile.motorPulley));
            motorPolesET.setText(Integer.toString(profile.motorPoles));
            DualVescCB.setChecked(profile.dualVesc);
        }
        else profile = new BoardProfile();
    }

    public void BackButtonPressed(View v)
    {
        finish();
    }

    public void CreateButtonPressed(View v)
    {
        profile.dualVesc = DualVescCB.isChecked();
        profile.motorPoles = Integer.parseInt(String.valueOf(motorPolesET.getText().toString()));
        profile.wheelDiameter = Integer.parseInt(String.valueOf(whellDiameterET.getText().toString()));
        profile.wheelPulley = Integer.parseInt(String.valueOf(wheelPulleyET.getText().toString()));
        profile.motorPulley = Integer.parseInt(String.valueOf(motorPulleyET.getText().toString()));
        profile.cellsInSerie = Integer.parseInt(String.valueOf(cellsSerieET.getText().toString()));
        profile.name = profileNameET.getText().toString();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("BoardProfile", (Parcelable) profile);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }


}
