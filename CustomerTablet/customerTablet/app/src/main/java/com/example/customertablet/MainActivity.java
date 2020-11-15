package com.example.customertablet;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;


import com.df.DataFrame;

public class MainActivity extends AppCompatActivity {
    TextView tx_temp, tx_setTemp, tx_logTemp, tx_curTemp, tx_logCtl, tx_ctl;
    SeekBar seekBar;

    // TCP/IP Server
    ServerSocket serverSocket;
    int serverPort = 5555;
    HashMap<String, ObjectOutputStream> maps;


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
        tx_setTemp.setText("Setting Temperature");
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

        // TCP/IP Server Start
        try {
            serverSocket = new ServerSocket(serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("[Server]","Start Server...");

        Runnable r = new Runnable() { // 받으면서 다른 것도 할 수 있도록 Thread 사용

            @Override
            public void run() {
                while (true) {
                    try {
                        Socket socket = null;
                        Log.d("[Server]","Ready Server ...");
                        socket = serverSocket.accept();
                        Log.d("[Server]", socket.getInetAddress()+"");
                        makeOut(socket);
                        new ServerReceiver(socket).start();
                    } catch (Exception e) {
                        //Android Studio Error(Invalid Stream header)
                        //e.printStackTrace();
                    }
                } // end while
            }
        };
        new Thread(r).start();




        // TCP/IP Client




    } // End OnCreate


    /*
    TCP/IP SERVER CODE
     */

    // HashMap maps에 접속 client 정보 삽입
    public void makeOut(Socket socket) throws IOException {
        ObjectOutputStream oo;
        oo = new ObjectOutputStream(socket.getOutputStream());
        maps.put(socket.getInetAddress().toString(), oo);
        System.out.println("접속자 수: " + maps.size());
    }

    public void serverSendDf(DataFrame df) {
        ServerSender sender = new ServerSender();
        sender.setDataFrame(df);
        sender.start();
    }
    class ServerSender extends Thread {
        DataFrame df;
        public void setDataFrame(DataFrame df) {
            this.df = df;
        }
        @Override
        public void run() {
            try {
                maps.get(df.getIp()).writeObject(df);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    class ServerReceiver extends Thread {
        Socket socket;
        ObjectInputStream oi;

        public ServerReceiver(Socket socket) throws IOException {
            this.socket = socket;
            oi = new ObjectInputStream(this.socket.getInputStream());
        }

        @Override
        public void run() {
            while (oi != null) {
                DataFrame df = null;
                try {
                    df = (DataFrame) oi.readObject();
                    Log.d("[Server]", df.getSender()+df.getContents());
                    serverSendDf(df);

                } catch (Exception e) {
                    maps.remove(socket.getInetAddress().toString());
                    Log.d("[Server]",socket.getInetAddress() + "..Exited");
                    break;
                }
            } // end while

            try {
                if(oi != null) {
                    oi.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {

            }
        }
    }
}