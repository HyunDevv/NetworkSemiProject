package com.example.customertablet;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    TextView tx_temp, tx_setTemp, tx_logTemp, tx_curTemp, tx_logCtl, tx_ctl;
    SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tx_temp = findViewById(R.id.tx_temp);
        tx_setTemp = findViewById(R.id.tx_setTemp);
        tx_logTemp = findViewById(R.id.tx_logTemp);
        tx_curTemp = findViewById(R.id.tx_curTemp);
        tx_logCtl = findViewById(R.id.tx_logCtl);
        tx_ctl = findViewById(R.id.tx_ctl);
        seekBar = findViewById(R.id.seekBar);

        tx_temp.setText("Temperature Control");
        tx_setTemp.setText("Current Temperature");
        tx_logTemp.setText("Temperature Control Log");
        tx_ctl.setText("Control Command");
        tx_logCtl.setText("Control Command Log");


        // 센서로부터 받은 현재 온도를 표시
        int temp = 17;
        tx_curTemp.setText("현재 온도: "+temp);
        
        // 온도 조절 SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // SeekBar 조작시 목표 온도 변경
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tx_setTemp.setText("목표 온도: "+progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}