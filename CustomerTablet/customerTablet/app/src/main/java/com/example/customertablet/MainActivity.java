package com.example.customertablet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;


import com.df.DataFrame;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    TextView tx_temp, tx_setTemp, tx_logTemp, tx_curTemp, tx_logCtl, tx_ctl;
    SeekBar seekBar; // 온도 설정

    Switch sw_power, sw_door; // 스위치를 통한 on/off
    // TCP/IP Server
    ServerSocket serverSocket;
    int serverPort = 5558;
    Sender sender;
    ObjectOutputStream oo;

    NotificationManager manager; // FCM을 위한 NotificationManager

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

        sw_power = findViewById(R.id.sw_power); // FCM을 통한 시동 조작
        sw_power.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new Thread() {
                        public void run() {
                            send("power", "0"); // send 함수로 control과 input을 보낸다
                        }
                    }.start();
                    sw_power.setText("시동 ON");
                } else {
                    new Thread() {
                        public void run() {
                            send("power", "1");
                        }
                    }.start();
                    sw_power.setText("시동 OFF");
                }
            }
        });
        sw_door = findViewById(R.id.sw_door); // FCM을 통한 문 조작
        sw_door.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new Thread() {
                        public void run() {
                            send("door", "0");
                        }
                    }.start();
                    sw_door.setText("문 잠김");
                } else {
                    new Thread() {
                        public void run() {
                            send("door", "1");
                        }
                    }.start();
                    sw_door.setText("문 열림");
                }
            }
        });

        tx_temp.setText("Temperature Control");
        tx_setTemp.setText("Setting Temperature");
        tx_logTemp.setText("Temperature Control Log");
        tx_ctl.setText("Control Command");
        tx_logCtl.setText("Control Command Log");


        // 센서로부터 받은 현재 온도를 표시
        int temp = 17;
        tx_curTemp.setText("현재 온도: " + temp);

        // 온도 조절 SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // SeekBar 조작시 목표 온도 변경
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tx_setTemp.setText(progress+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (tx_setTemp.equals(tx_curTemp.getText())) {
                    Toast.makeText(MainActivity.this,"현재 온도와 목표 온도가 같습니다.",Toast.LENGTH_SHORT).show();
                } else { // 이따가 seekBar를 움직이다가 목표 온도의 변화 없이 제자리로 돌아오면 어떻게 되는지 실험
                    new Thread() {
                        public void run() {
                            send("temp", tx_setTemp.getText().toString()); // 값이 잘 들어오는지 이따가 확인해보기
                        }
                    }.start();
                }
            }
        });

        try {
            startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // FCM사용 (앱이 중단되어 있을 때 기본적으로 title,body값으로 푸시!!)
        FirebaseMessaging.getInstance().subscribeToTopic("car"). //구독, 이걸로 원하는 기능 설정하기(파이널 때, db 활용)
                addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                String msg = "FCM Complete...";
                if (!task.isSuccessful()) {
                    msg = "FCM Fail";
                }
                Log.d("[TAG]", msg);
            }
        });

        // 여기서 부터는 앱 실행상태에서 상태바 설정!!
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this); // 브로드캐스트를 받을 준비
        lbm.registerReceiver(receiver, new IntentFilter("notification")); // notification이라는 이름의 정보를 받겠다
    } // end OnCreate


    public void startServer() throws Exception {
        serverSocket = new ServerSocket(serverPort);


        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Socket socket = null;
                        Log.d("[Server]", "Server Ready..");
                        socket = serverSocket.accept();
                        Log.d("[Server]", socket.getInetAddress() + "Connected...");
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
                            tx_logTemp.setText(input.getContents() + "\n" + logTemp);
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

    public void sendDataFrame(DataFrame df, Socket socket) {
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

    class Sender extends Thread {
        DataFrame dataFrame;

        public Sender() {

        }

        public void setDataFrame(DataFrame df) {
            this.dataFrame = df;
            Log.d("[Server]", "setDataFrame 완료");
        }

        @Override
        public void run() {
            try {
                Log.d("[Server]", "Sender Thread 실행");
                //dataFrame.setIp("192.168.35.149");
                //dataFrame.setSender("[TabletServer]");
                //Log.d("[Server]", "테스트 목적 Client로 목적지 재설정");

                oo.writeObject(dataFrame);
                Log.d("[Server]", "Sender 객체 전송.. " + dataFrame.getIp() + "주소로 " + dataFrame.getContents());
                Log.d("[Server]", "Sender 객체 전송 성공");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    public BroadcastReceiver receiver = new BroadcastReceiver() { // FCM 받는 부분
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String title = intent.getStringExtra("title");
                String control = intent.getStringExtra("control");
                String data = intent.getStringExtra("data");

                if (control.equals("temp")) { // control이 temp면, data(온도값)을 set해라
                    if (Integer.parseInt(data) > 30) {
                        Toast.makeText(MainActivity.this,
                                "30도 이하의 온도로 설정해주세요.", Toast.LENGTH_LONG).show();
                    } else if (Integer.parseInt(data) < 18) {
                        Toast.makeText(MainActivity.this,
                                "18도 이상의 온도로 설정해 주세요.", Toast.LENGTH_LONG).show();
                    } else if (data.equals(tx_setTemp.getText())) {
                        Toast.makeText(MainActivity.this,
                                "바꿀 온도를 입력해주세요.", Toast.LENGTH_LONG).show();
                    } else {
                        tx_logTemp.append("희망 온도가 " + tx_setTemp.getText() + "℃에서 " + data + "℃로 변경되었습니다." + "\n");
                        tx_setTemp.setText(data);
                        seekBar.setProgress(Integer.parseInt(data));
                        Toast.makeText(MainActivity.this,
                                "온도가 변경되었습니다.", Toast.LENGTH_LONG).show();
                    }

                } else if (control.equals("door")) { // 문 제어
                    if (data.equals("0")) {
                        tx_logCtl.append("문이 잠겼습니다." + "\n");
                        sw_door.setChecked(true);
                    } else if (data.equals("1")) {
                        tx_logCtl.append("문이 열렸습니다." + "\n");
                        sw_door.setChecked(false);
                    }

                } else if (control.equals("power")) { // 시동 제어
                    if (data.equals("0")) {
                        tx_logCtl.append("시동이 켜졌습니다." + "\n");
                        sw_power.setChecked(true);
                    } else if (data.equals("1")) {
                        tx_logCtl.append("시동이 꺼졌습니다." + "\n");
                        sw_power.setChecked(false);
                    }
                } // 추가로 제어할 것이 있으면 이곳에 else if 추가

                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); // 진동 없애려면 삭제
                if (Build.VERSION.SDK_INT >= 26) { //버전 체크를 해줘야 작동하도록 한다
                    vibrator.vibrate(VibrationEffect.createOneShot(1000, 10));
                } else {
                    vibrator.vibrate(1000);
                }

                manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                NotificationCompat.Builder builder = null;
                if (Build.VERSION.SDK_INT >= 26) {
                    if (manager.getNotificationChannel("ch1") == null) {
                        manager.createNotificationChannel(
                                new NotificationChannel("ch1", "chname", NotificationManager.IMPORTANCE_DEFAULT));
                    }
                    builder = new NotificationCompat.Builder(context, "ch1");
                } else {
                    builder = new NotificationCompat.Builder(context);
                }

                Intent intent1 = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, 101, intent1, PendingIntent.FLAG_UPDATE_CURRENT
                );
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                builder.setAutoCancel(true);
                builder.setContentIntent(pendingIntent);

                builder.setContentTitle(title);
                if (data.equals("0")) {
                    builder.setContentText(control + " 이(가) ON/LOCK 상태로 변경되었습니다.");
                } else if (data.equals("1")) {
                    builder.setContentText(control + " 이(가) OFF/UNLOCK 상태로 변경되었습니다.");
                } else {
                    builder.setContentText(control + " 이(가)" + data + " ℃로 변경되었습니다.");
                }
                builder.setSmallIcon(R.drawable.a1);
                Notification noti = builder.build();
                manager.notify(1, noti); // 상단 알림을 없애려면 이곳 주석 처리
            }
        }
    };


    // 데이터를 Push Message에 넣어서 보내는 send 함수
    public void send(String control, String input) { // String control, String input 으로 변경하기 !
        System.out.println("phone Send Start...");
        URL url = null;
        try {
            url = new URL("https://fcm.googleapis.com/fcm/send");
        } catch (MalformedURLException e) {
            System.out.println("Error while creating Firebase URL | MalformedURLException");
            e.printStackTrace();
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            System.out.println("Error while createing connection with Firebase URL | IOException");
            e.printStackTrace();
        }
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        // set my firebase server key
        conn.setRequestProperty("Authorization", "key="
                + "AAAAK89FyMY:APA91bGxNwkQC6S_QQAKbn3COepWgndhyyjynT8ZvIEarTaGpEfMA1SPFo-ReN8b9uO21R1OfSOpNhfYbQaeohKP_sKzsgVTxu7K5tmzcjEfHzlgXRFrB1r0uqhfxLp4p836lbKw_iaN");

        // create notification message into JSON format
        JSONObject message = new JSONObject();
        try {
            message.put("to", "/topics/car");
            message.put("priority", "high");

            JSONObject notification = new JSONObject();
            notification.put("title", "HyunDai");
            notification.put("body", "자동차 상태 변경");
            message.put("notification", notification);

            JSONObject data = new JSONObject();
            data.put("control", control); // 이 부분 변경하며 temp, door, power 등 조절
            data.put("data", input);
            message.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            System.out.println("FCM 전송:" + message.toString());
            out.write(message.toString());
            out.flush();
            conn.getInputStream();
            System.out.println("OK...............");

        } catch (IOException e) {
            System.out.println("Error while writing outputstream to firebase sending to ManageApp | IOException");
            e.printStackTrace();
        }

        System.out.println("phone Send End...");
    }
}