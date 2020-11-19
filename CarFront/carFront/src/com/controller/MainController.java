package com.controller;

import com.can.SendAndReceiveSerial;
import com.can.SendAndReceiveSerialCan;

public class MainController {

	public static void main(String[] args) {
		SendAndReceiveSerial arduino = new SendAndReceiveSerial("COM5", true);
		SendAndReceiveSerialCan can = new SendAndReceiveSerialCan("COM9", true);
		can.setArduino(arduino);
		arduino.setCan(can);
	}

}
