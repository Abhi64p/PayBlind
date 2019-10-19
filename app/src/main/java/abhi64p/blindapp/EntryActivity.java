package abhi64p.blindapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Locale;

public class EntryActivity extends AppCompatActivity
{
    private final int PhoneNumberRequestCode=2;
    private final int UPIPinRequestCode = 3;
    private final int PhoneNumberCheckRequestCode = 4;
    private final int UPIPinCheckRequestCode = 5;
    private final int ReadPasswordRequestCode = 6;
    private final int ReceiverPhoneNumberRequestCode = 7;
    private final int ReceiverPhoneNumberCheckRequestCode = 8;
    private final int TransactionAmountRequestCode = 9;
    private final int TransactionAmountCheckRequestCode = 10;
    private TextToSpeech tts;
    private String Pass1;
    private String ReceiverPhoneNumber;
    private String TransactionAmount;
    private DataOutputStream output;
    private String IP;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        IP = getSharedPreferences(CommonData.SP,MODE_PRIVATE).getString("IP","192.168.1.102");

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int i)
            {
                if (i == TextToSpeech.SUCCESS)
                {
                    tts.setLanguage(Locale.ENGLISH);
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener()
                    {
                        @Override
                        public void onStart(String s)
                        { }

                        @Override
                        public void onDone(final String s)
                        {
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    TalkingComplete(s);
                                }
                            });
                        }

                        @Override
                        public void onError(String s)
                        { }
                    });
                    tts.setSpeechRate(0.9f);
                    Continue();
                } else
                {
                    Toast.makeText(EntryActivity.this, "Failed to start TextToSpeech", Toast.LENGTH_SHORT).show();
                    EntryActivity.this.finish();
                }
            }

        });
    }

    private void Continue()
    {
        if (getSharedPreferences(CommonData.SP, MODE_PRIVATE).getBoolean("LoggedIn", false))
            tts.speak("Shake your phone to start a transaction", TextToSpeech.QUEUE_FLUSH, null, "ReadyForTransaction");
        else
            tts.speak("Welcome, please say your phone number linked with bank account", TextToSpeech.QUEUE_FLUSH, null, "RequestPhoneNumber");
    }

    private void TalkingComplete(String ID)
    {
        switch (ID)
        {
            case "RequestPhoneNumber" : promptSpeechInput(PhoneNumberRequestCode); break;
            case "PhoneNumberCheck" : promptSpeechInput(PhoneNumberCheckRequestCode); break;
            case "RequestUPIPin" : promptSpeechInput(UPIPinRequestCode); break;
            case "UPIPinCheck" : promptSpeechInput(UPIPinCheckRequestCode); break;
            case "LinkingComplete" : SetPasswordMapping(); break;
            case "ReadyForTransaction" : ReadyForTransaction(); break;
            case "ReceiverPhoneNumber" : promptSpeechInput(ReceiverPhoneNumberRequestCode); break;
            case "ReceiverPhoneNumberCheck" : promptSpeechInput(ReceiverPhoneNumberCheckRequestCode); break;
            case "TransactionAmount" : promptSpeechInput(TransactionAmountRequestCode); break;
            case "TransactionAmountCheck" : promptSpeechInput(TransactionAmountCheckRequestCode); break;
            case "TransferAmount" : TransferAmount(); break;
            case "ErrorRestart" : this.finish(); break;
        }
    }

    private void promptSpeechInput(int RequestCode)
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
        try
        {
            startActivityForResult(intent, RequestCode);
        } catch (ActivityNotFoundException a)
        {
            Toast.makeText(getApplicationContext(), "Speech To Text not supported!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && data != null)
        {
            String Result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            switch (requestCode)
            {
                case PhoneNumberRequestCode:
                    String PhoneNumber = Result.replace(" ", "");
                    tts.speak("Your phone number is " + AddSpace(PhoneNumber) + ". Is this correct ?", TextToSpeech.QUEUE_FLUSH, null, "PhoneNumberCheck");
                    getSharedPreferences(CommonData.SP, MODE_PRIVATE).edit().putString("PhoneNumber", PhoneNumber).apply();
                    break;
                case PhoneNumberCheckRequestCode:
                    if (Result.toLowerCase().equals("yes"))
                        tts.speak("Say your new U P I pin", TextToSpeech.QUEUE_FLUSH, null, "RequestUPIPin");
                    else
                        tts.speak("Say your phone number again", TextToSpeech.QUEUE_FLUSH, null, "RequestPhoneNumber");
                    break;
                case UPIPinRequestCode:
                    String UPIPin = Result.replace(" ", "");
                    tts.speak("Your new U P I pin is " + AddSpace(UPIPin) + ". Is this correct ?", TextToSpeech.QUEUE_FLUSH, null, "UPIPinCheck");
                    getSharedPreferences(CommonData.SP, MODE_PRIVATE).edit().putString("UPIPin", UPIPin).apply();
                    break;
                case UPIPinCheckRequestCode:
                    if (Result.toLowerCase().equals("yes"))
                        tts.speak("Bank account linked.", TextToSpeech.QUEUE_FLUSH, null, "LinkingComplete");
                    else
                        tts.speak("Say your U P I pin again", TextToSpeech.QUEUE_FLUSH, null, "RequestUPIPin");
                    break;
                case ReadPasswordRequestCode:
                    Pass1 = Result;
                    tts.speak("Confirm pattern", TextToSpeech.QUEUE_FLUSH, null, null);
                    startActivityForResult(new Intent(this, PasswordActivity.class).setAction("ReadPassword"), 0);
                    break;
                case 0:
                    if (Pass1.equals(Result))
                    {
                        tts.speak("Pattern saved! Shake your phone to start a new transaction", TextToSpeech.QUEUE_FLUSH, null, "ReadyForTransaction");
                        SharedPreferences.Editor editor = getSharedPreferences(CommonData.SP, MODE_PRIVATE).edit();
                        editor.putBoolean("LoggedIn", true);
                        editor.putString("Password", Pass1);
                        editor.apply();
                    } else
                        tts.speak("Two patterns does not match. Try again", TextToSpeech.QUEUE_FLUSH, null, "LinkingComplete");
                    break;
                case ReceiverPhoneNumberRequestCode:
                    ReceiverPhoneNumber = Result.replace(" ","");
                    tts.speak("Receiver's phone number is " + AddSpace(ReceiverPhoneNumber) + ". Is this correct ?", TextToSpeech.QUEUE_FLUSH, null, "ReceiverPhoneNumberCheck");
                    break;
                case ReceiverPhoneNumberCheckRequestCode:
                    if (Result.toLowerCase().equals("yes"))
                        tts.speak("How much do you want to transfer ?", TextToSpeech.QUEUE_FLUSH, null, "TransactionAmount");
                    else
                        tts.speak("Say receiver's phone number again", TextToSpeech.QUEUE_FLUSH, null, "ReceiverPhoneNumber");
                    break;
                case TransactionAmountRequestCode:
                    TransactionAmount = Result;
                    tts.speak("Transaction amount is " + TransactionAmount + ". Is this correct?", TextToSpeech.QUEUE_FLUSH, null, "TransactionAmountCheck");
                    break;
                case TransactionAmountCheckRequestCode:
                    if (Result.toLowerCase().equals("yes"))
                    {
                        tts.speak("Enter your pattern and shake phone", TextToSpeech.QUEUE_FLUSH, null, null);
                        startActivityForResult(new Intent(this, PasswordActivity.class).setAction("ReadPassword"), 1);
                    } else
                        tts.speak("Say transaction amount again", TextToSpeech.QUEUE_FLUSH, null, "TransactionAmount");
                    break;
                case 1:
                    Pass1 = getSharedPreferences(CommonData.SP, MODE_PRIVATE).getString("Password", "...");
                    if (Pass1.equals(Result))
                        tts.speak("Transaction in progress. Please wait", TextToSpeech.QUEUE_FLUSH, null, "TransferAmount");
                    else
                    {
                        tts.speak("Wrong pattern. Enter your pattern again and shake phone", TextToSpeech.QUEUE_FLUSH, null, null);
                        startActivityForResult(new Intent(this,PasswordActivity.class).setAction("ReadPassword"), 1);
                    }
            }
        }
    }

    private String AddSpace(String Input)
    {
        int Size = Input.length(), i,j;
        char[] Output = new char[Size * 2];
        for(i=0,j=0; i<Size; i++)
        {
            Output[j++] = Input.charAt(i);
            Output[j++] = ' ';
        }
        return new String(Output);
    }

    private void SetPasswordMapping()
    {
        tts.speak("To keep U P I pin secure, add a new pattern password for transactions. Keep your phone in landscape position and"
                + "place 4 fingers on the screen and tap to create a pattern. Shake your phone to save pattern",
                TextToSpeech.QUEUE_FLUSH,null,null);
        startActivityForResult(new Intent(this,PasswordActivity.class).setAction("ReadPassword"), ReadPasswordRequestCode);
    }
    
    private void ReadyForTransaction()
    {
        final SensorManager SM = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if(SM != null)
        {
            final Sensor Accelerometer = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            final ShakeDetector SD = new ShakeDetector();
            SD.setOnShakeListener(new ShakeDetector.OnShakeListener()
            {
                @Override
                public void onShake(int count)
                {
                    StartTransaction();
                }
            });
            SM.registerListener(SD, Accelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Socket socket = new Socket(IP,1111);
                    output = new DataOutputStream(socket.getOutputStream());
                    output.writeUTF("ID;" + getSharedPreferences(CommonData.SP, MODE_PRIVATE).getString("PhoneNumber", "..."));
                    final DataInputStream input = new DataInputStream(socket.getInputStream());
                    while (true)
                    {
                        String[] InputString = input.readUTF().split(";");
                        switch (InputString[0])
                        {
                            case "Transfer":
                                SharedPreferences SP = getSharedPreferences(CommonData.SP, MODE_PRIVATE);
                                int Amount = SP.getInt("AccountBalance", 0);
                                Amount += Integer.valueOf(InputString[1]);
                                SP.edit().putInt("AccountBalance", Amount).apply();
                                final String ReceivedAmount = InputString[1];
                                runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        tts.speak("Rupees " + ReceivedAmount + " received!", TextToSpeech.QUEUE_ADD, null, null);
                                    }
                                });
                                break;
                            case "TransactionCompleted":
                                tts.speak("Transaction completed successfully", TextToSpeech.QUEUE_FLUSH, null, null);
                                SP = getSharedPreferences(CommonData.SP, MODE_PRIVATE);
                                Amount = SP.getInt("AccountBalance", 0);
                                Amount -= Integer.valueOf(TransactionAmount);
                                SP.edit().putInt("AccountBalance", Amount).apply();
                                break;
                            case "IdNotFound":
                                tts.speak("Transaction failed. Cannot find receiver", TextToSpeech.QUEUE_FLUSH, null, null);
                                break;
                        }
                    }
                }
                catch(final Exception ex)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            ex.printStackTrace();
                            tts.speak("Can't connect to internet. Please restart app",TextToSpeech.QUEUE_ADD,null,"ErrorRestart");
                        }
                    });
                }
            }
        }).start();
    }
    
    private void StartTransaction()
    {
        tts.speak("What's receiver's phone number ?",TextToSpeech.QUEUE_FLUSH, null, "ReceiverPhoneNumber");
    }

    private void TransferAmount()
    {
        try
        {
            output.writeUTF("Transfer;" + ReceiverPhoneNumber + ";" + TransactionAmount);
            output.flush();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            tts.speak("Can't connect to internet. Please restart app",TextToSpeech.QUEUE_ADD,null,"ErrorRestart");
        }
    }

    public void ChangeIP(View view)
    {
        final EditText ET = new EditText(this);
        ET.setText(IP);
        ET.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        new AlertDialog.Builder(this)
                .setTitle("Enter new IP")
                .setView(ET)
                .setPositiveButton("Update", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        IP = ET.getText().toString();
                        getSharedPreferences(CommonData.SP,MODE_PRIVATE).edit().putString("IP",IP).apply();
                    }
                }).create().show();
    }
}