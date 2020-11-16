package com.controller;

import java.io.IOException;

import com.can.SendAndReceiveSerial;
import com.tcpip.Client;

public class MainController {

	public static void main(String[] args) {
		SendAndReceiveSerial ss = new SendAndReceiveSerial("COM5", true);
		Client client = new Client("192.168.35.37",5558);
		ss.setClient(client);
		
		try {
			client.connect();
			//client.sendData();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
