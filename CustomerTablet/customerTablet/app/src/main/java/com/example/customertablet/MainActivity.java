package com.example.customertablet;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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
    int serverPort = 5558;
    Sender sender;
    ObjectOutputStream oo;



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


        try {
            startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }


    } // End OnCreate


    public void startServer() throws Exception{
        serverSocket = new ServerSocket(serverPort);
        Log.d("[Server]", "Server Started.");


        Runnable r = new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Socket socket = null;
                        Log.d("[Server]", "Server Ready..");
                        socket = serverSocket.accept();
                        Log.d("[Server]", socket.getInetAddress()+"Connected...");
                        new Receiver(socket).start();



                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        new Thread(r).start();
    }


    class Receiver extends Thread {
        Socket socket;
        ObjectInputStream oi;


        public Receiver(Socket socket) throws IOException {
            this.socket = socket;
            oi = new ObjectInputStream(this.socket.getInputStream());
            oo = new ObjectOutputStream(this.socket.getOutputStream());
        }

        @Override
        public void run() {
            while (oi != null) {
                try {
                    final DataFrame input = (DataFrame) oi.readObject();
                    Log.d("[Server]", "input: " + input.getSender() + ": " + input.getContents());
                    sendDataFrame(input, socket);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String logTemp = tx_logTemp.getText().toString();
                            tx_logTemp.setText(input.getContents()+"\n"+logTemp);
                        }
                    });




                } catch (Exception e) {
                    Log.d("[Server]", "Receiver 객체 수신 실패");
                    break;
                }
            } // end while

            try {
                if (oi != null) {
                    Log.d("[Server]", "ObjectInputStream Closed ..");
                    oi.close();
                }
                if (socket != null) {
                    Log.d("[Server]", "Socket Closed ..");
                    socket.close();
                }
            } catch (Exception e) {
                Log.d("[Server]", "객체 수신 실패 후 InputStream, socket 닫기 실패");
            }

        }
    }// End Receiver

    public void sendDataFrame(DataFrame df, Socket socket){
        try {
            sender = new Sender();
            Log.d("[Server]", "setDataFrame 실행");
            sender.setDataFrame(df);
            Log.d("[Server]", "객체 송신 Thread 호출");
            sender.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class Sender extends Thread{
        DataFrame dataFrame;

        public Sender() {

        }

        public void setDataFrame(DataFrame df){
            this.dataFrame = df;
            Log.d("[Server]", "setDataFrame 완료");
        }

        @Override
        public void run() {
            try {
                Log.d("[Server]", "Sender Thread 실행");
                dataFrame.setIp("192.168.35.149");
                dataFrame.setSender("[TabletServer]");
                Log.d("[Server]", "테스트 목적 Client로 목적지 재설정");

                oo.writeObject(dataFrame);
                Log.d("[Server]", "Sender 객체 전송.. "+dataFrame.getIp()+"주소로 "+dataFrame.getContents());
                Log.d("[Server]", "Sender 객체 전송 성공");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}