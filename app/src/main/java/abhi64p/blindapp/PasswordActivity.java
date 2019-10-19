package abhi64p.blindapp;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class PasswordActivity extends AppCompatActivity
{
    private String Input = "";
    private Vibrator v;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        v = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        String Action = getIntent().getAction();
        if(Action != null)
            if(!Action.isEmpty())
                if(getIntent().getAction().equals("ReadPassword"))
                {
                    final SensorManager SM = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    final Sensor Accelerometer = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    final ShakeDetector SD = new ShakeDetector();
                    SD.setOnShakeListener(new ShakeDetector.OnShakeListener()
                    {
                        @Override
                        public void onShake(int count)
                        {
                            ArrayList<String> tmpList = new ArrayList<>();
                            tmpList.add(Input);
                            Intent ReturnIntent = new Intent();
                            ReturnIntent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, tmpList);
                            setResult(RESULT_OK, ReturnIntent);
                            PasswordActivity.this.finish();
                            SM.unregisterListener(SD);
                        }
                    });

                    SM.registerListener(SD,Accelerometer,SensorManager.SENSOR_DELAY_UI);

                }
    }

    public void FingerPressed(View view)
    {
        char x = ' ';
        switch (view.getId())
        {
            case R.id.B1:
                x = 'a';
                break;
            case R.id.B2:
                x = 'b';
                break;
            case R.id.B3:
                x = 'c';
                break;
            case R.id.B4:
                x = 'd';
                break;
        }
        Input += x;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        else
        {
            if (v != null)
                v.vibrate(50);
        }
    }
}
