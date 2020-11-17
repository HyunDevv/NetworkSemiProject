package com.example.customertablet;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentValues;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;


import com.df.DataFrame;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    TextView tx_temp, tx_setTemp, tx_logTemp, tx_curTemp, tx_logCtl, tx_ctl;
    SeekBar seekBar;


    // TCP/IP Server
    ServerSocket serverSocket;
    int serverPort = 5558;
    Sender sender;
    ObjectOutputStream oo;

    // HTTP
    DataFrame dataF;
    static String strJson = "";





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

    /*
        HTTP 통신 Code
     */

    public String GET(String webUrl, String ip, String sender, String contents){
        URL url = null;
        StringBuilder html = new StringBuilder();
        webUrl += "?ip="+ip+"&sender="+sender+"&contents="+contents;
        try {
            url = new URL(webUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            if(con != null){
                con.setConnectTimeout(10000);
                con.setUseCaches(false);
                if(con.getResponseCode() == HttpURLConnection.HTTP_OK){
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    for(;;){
                        String line = br.readLine();
                        if(line == null)break;
                        html.append(line );
                        html.append('\n');
                    }
                    br.close();
                }
                con.disconnect();
            }
        }
        catch(Exception ex){;}

        return html.toString();
    }

    public static String POST(String url, DataFrame df){
        InputStream is = null;
        String result = "";
        try {
            URL urlCon = new URL(url);
            HttpURLConnection httpCon = (HttpURLConnection)urlCon.openConnection();

            String json = "";

            // build jsonObject
            JSONObject jsonObject = new JSONObject();
            jsonObject.accumulate("ip", df.getIp());
            jsonObject.accumulate("sender", df.getSender());
            jsonObject.accumulate("contents", df.getContents());

            // convert JSONObject to JSON to String
            json = jsonObject.toString();
            Log.d("[Server]", "HTTP JSON 생성 후 전송 "+json);

            // Set some headers to inform server about the type of the content
            httpCon.setRequestProperty("Accept", "application/json");
            httpCon.setRequestProperty("Content-type", "application/json");
            httpCon.setRequestMethod("POST");

            // OutputStream으로 POST 데이터를 넘겨주겠다는 옵션.
            httpCon.setDoOutput(true);
            // InputStream으로 서버로 부터 응답을 받겠다는 옵션.
            httpCon.setDoInput(true);

            // JSON 전송
            OutputStream os = httpCon.getOutputStream();
            os.write(json.getBytes("utf-8"));
            os.flush();
            Log.d("[Server]", "HTTP JSON 전송");


            // receive response as inputStream
            try {
                is = httpCon.getInputStream();
                // convert inputstream to string
                if(is != null) {
                    result = convertInputStreamToString(is);
                    Log.d("[Server]", "HTTP 통신 수신: " + result);
                }
                else
                    result = "Did not work!";
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                httpCon.disconnect();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            Log.d("[Server]", e.getLocalizedMessage());
        }

        return result;
    }

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {

        private MainActivity mainAct;

        HttpAsyncTask(MainActivity mainActivity) {
            this.mainAct = mainActivity;
        }
        @Override
        protected String doInBackground(String... urls) {

            dataF = new DataFrame();
            dataF.setIp(urls[1]);
            dataF.setSender(urls[2]);
            dataF.setContents(urls[3]);
            Log.d("[Server]", "[AsyncTask Background]"+urls[0]+urls[1]+urls[2]+urls);

//            return POST(urls[0], dataF);
            return GET(urls[0], urls[1], urls[2], urls[3]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            strJson = result;
            mainAct.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mainAct, "Received!", Toast.LENGTH_LONG).show();
                    try {
                        JSONArray json = new JSONArray(strJson);
                        mainAct.tx_logCtl.setText(json.toString(1));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    }



    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }


    // End HTTP 통신 Code


    /*
        TCP/IP 통신 Code
     */

    public void startServer() throws Exception{
        serverSocket = new ServerSocket(serverPort);

        Runnable r = new Runnable() { // Thread로 동작시켜 다른 일도 할 수 있도록!
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


    // TCP/IP Receive CODE
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

                    // 받은 DataFrame을 웹서버로 HTTP 전송
                    // call AsynTask to perform network operation on separate thread
                    HttpAsyncTask httpTask = new HttpAsyncTask(MainActivity.this);
                    httpTask.execute("http://192.168.0.38/tcpip/getFromTablet.mc", input.getIp(), input.getSender(), input.getContents());



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

    // TCP/IP Send CODE
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
                //dataFrame.setIp("192.168.35.149");
                //dataFrame.setSender("[TabletServer]");
                //Log.d("[Server]", "테스트 목적 Client로 목적지 재설정");

                oo.writeObject(dataFrame);
                Log.d("[Server]", "Sender 객체 전송.. "+dataFrame.getIp()+"주소로 "+dataFrame.getContents());
                Log.d("[Server]", "Sender 객체 전송 성공");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    } // End TCP/IP 통신 Code
}