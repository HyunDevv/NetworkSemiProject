package com.tcpip;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.df.DataFrame;

public class Server {
	ServerSocket serverSocket;
    Sender sender;
    ObjectOutputStream oo;
    int serverPort;
    
	public Server() {
		
	}
	public Server(int port) {
		this.serverPort = port;
	}
	
    
    public void startServer() throws Exception{
        serverSocket = new ServerSocket(serverPort);
        System.out.println("Start Server...");



        Runnable r = new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Socket socket = null;
                        System.out.println("Server Ready..");
                        socket = serverSocket.accept();
                        System.out.println(socket.getInetAddress()+" Connected...");
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
                    System.out.println("[DataFrame 수신] " + input.getSender() + ": " + input.getContents());
                    
                    // 필요할 경우 사용
                    //sendDataFrame(input, socket);
                   



                } catch (Exception e) {
                	System.out.println("Receiver 객체 수신 실패");
                    break;
                }
            } // end while

            try {
                if (oi != null) {
                	System.out.println("ObjectInputStream Closed ..");
                    oi.close();
                }
                if (socket != null) {
                	System.out.println("Socket Closed ..");
                    socket.close();
                }
            } catch (Exception e) {
            	System.out.println("객체 수신 실패 후 InputStream, socket 닫기 실패");
            }

        }
    }// End Receiver

    public void sendDataFrame(DataFrame df, Socket socket){
        try {
            sender = new Sender();
            System.out.println("setDataFrame 실행");
            sender.setDataFrame(df);
            System.out.println("객체 송신 Thread 호출");
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
            System.out.println("setDataFrame 완료");
        }

        @Override
        public void run() {
            try {
            	System.out.println("Sender Thread 실행");
                //dataFrame.setIp("192.168.35.149");
                //dataFrame.setSender("[TabletServer]");
                //Log.d("[Server]", "테스트 목적 Client로 목적지 재설정");

                oo.writeObject(dataFrame);
                System.out.println("Sender 객체 전송.. "+dataFrame.getIp()+"주소로 "+dataFrame.getContents());
                System.out.println( "Sender 객체 전송 성공");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    public static void main(String[] args){
        Server server = new Server(5555);
        try {
           server.startServer();
        } catch (Exception e) {
           e.printStackTrace();
        }
     }

}
