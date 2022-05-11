package com.example.rcar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    String ip;
    int port;
    int fps;

    TextView IpTextView;
    TextView PortTextView;
    Button settingsB;
    Button startB;
    SeekBar accelerateS;

    private SensorManager sensorManager = null;

    String msg="";
    String tmp_msg = "";
    int gas=0, rikverc=0,acc=1024,smjer1=0,smjer2=0;
    float smjer=0,smjerD=0;

    MessageSender messageSender;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {


            if (acc > 1024) {
                rikverc = 0;
                gas = (acc - 1024);
            }
            else if (acc < 1024) {
                gas = 0;
                rikverc = -(acc - 1024);

            }
            else if (acc == 1024) {
                gas = rikverc = 0;
            }

            if(smjer>0)
            {
                smjer2=0;
                smjer1=Integer.valueOf((int) (smjer*110));
            }
            else if(smjer<0)
            {
                smjer1=0;
                smjer2=Integer.valueOf((int) (-smjer*110));
            }
            else if(smjer==0)
            {
                smjer1=smjer2=0;
            }


            sendMsg();



           timerHandler.postDelayed(this, fps);//ponovo izvrši sam sebe za vrijeme fps



        }



    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings =  PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        ip=settings.getString("ip", "192.168.0.17");
        port=settings.getInt("port", 4210);
        fps=settings.getInt("fps", 200);

        IpTextView = (TextView) findViewById(R.id.ip);
        PortTextView = (TextView) findViewById(R.id.port);
        settingsB = (Button) findViewById(R.id.settings);
        startB = (Button) findViewById(R.id.start);
        accelerateS=(SeekBar)findViewById(R.id.seekBar);





        display();
        reset();

        settingsB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    settings();
            }
        });


        startB.setText("start");//start-pause-continue
        startB.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (startB.getText().equals("pause")) {
                    timerHandler.removeCallbacks(timerRunnable);//izbrisi sve zadatke od timerRunnable na cekanju
                    startB.setText("continue");
                    reset();
                }
                else {
                    timerHandler.post(timerRunnable);//zapocni jedno mjerenje vremena
                    startB.setText("pause");
                    reset();
                }

            }
        });


        accelerateS.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                reset();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                // TODO Auto-generated method stub
                acc=progress;

            }
        });


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);

    }

    public void reset()
    {
        accelerateS= findViewById(R.id.seekBar);
        accelerateS.setProgress(1024);
        accelerateS.refreshDrawableState();
        gas=0;
        rikverc=0;
        smjer1=0;
        smjer2=0;
        smjer=0;
        smjerD=0;
        acc=1024;
        sendMsg();
    }


    private void sendMsg(){

        msg=(String.valueOf(gas)+"&"+String.valueOf(rikverc)+":"+String.valueOf(smjer1)+"&"+String.valueOf(smjer2)+"e");
        if(!tmp_msg.equals(msg)){
            new MessageSender().execute(msg);
            Log.i("PACKET: ", msg);
            tmp_msg=msg;
        }
    }

    public void settings() {
        reset();
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }

    public void display() {
        IpTextView.setText(ip);
        PortTextView.setText(String.valueOf( port));
        try {
            MessageSender.ip= InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        MessageSender.port=port;
    }


    @Override
    public void onPause(){
        timerHandler.removeCallbacks(timerRunnable);
        sensorManager.unregisterListener((SensorEventListener) this);
        startB.setText("continue");
        reset();
        super.onPause();
    }


    @Override
    public void onStop() {
        timerHandler.removeCallbacks(timerRunnable);
        sensorManager.unregisterListener((SensorEventListener) this);
        startB.setText("continue");
        reset();
        super.onStop();
    }

    @Override
    public void onRestart() {
        timerHandler.removeCallbacks(timerRunnable);//izbrisi sve zadatke od timerRunnable na cekanju
        startB.setText("continue");
        sensorManager.registerListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
               SensorManager.SENSOR_DELAY_GAME);
        reset();
        display();
        super.onRestart();
    }

    @Override
    public void onDestroy() {
        timerHandler.removeCallbacks(timerRunnable);
        sensorManager.unregisterListener((SensorEventListener) this);
        startB.setText("start");
        reset();
        super.onDestroy();
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            smjerD =event.values[1];

            if((smjerD-smjer)>0.5 || (smjerD-smjer)<(-0.5))
                smjer =  smjerD;

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}