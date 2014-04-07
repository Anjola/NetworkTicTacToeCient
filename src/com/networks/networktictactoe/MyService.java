package com.networks.networktictactoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class MyService extends Service {

	public static final int SERVERPORT= 20000;
	public static final String SERVERADDRESS = "54.186.253.58";
	public static final int MAXPACKETSIZE = 512;


	private String clientID = null; 
	private final IBinder mBinder = new MyBinder();

	public DatagramSocket socket;
	public InetSocketAddress serverSocketAddress;
	private String group = null;
	public Messenger myMessenger;
	private boolean isConnected; 
	//binder to return
	public class MyBinder extends Binder {
		MyService getService() {
			return MyService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub

		//start the listeners
		Log.i("MyService", "Bound");
		init();
		return mBinder;
	}
	
	//messenger from mainActivity
	public void setMessenger(Messenger messenger)
	{
		myMessenger = messenger;
	}


	//starts service thread 
	public void init()
	{
		new Thread(){

			@Override
			public void run()
			{
				try {
					
					socket = new DatagramSocket();
					serverSocketAddress =  new InetSocketAddress(SERVERADDRESS,SERVERPORT);
					
					//register the client
					send("REGISTER");

					byte[] buf = new byte[MAXPACKETSIZE];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					// call receive (this will poulate the packet with the received
					// data, and the other endpoint's info)
					socket.receive(packet);
					// start up a worker thread to process the packet (and pass it
					// the socket, too, in case the
					// worker thread wants to respond)
					WorkerThread t = new WorkerThread(packet);
					t.start();

				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}




			}
		}.start();
	}
	public void requestOpponent(char piece)
	{
		if(isConnected)
		{
			send("JOIN,"+clientID+",waiting"+piece);
		}
		else
		{
			Log.i("MyService","Requesting without Registering");
		}
	}
	public void sendMove( int row, int col){
		Log.i("MyService", "sendMove");
		
		sendMSG("MOVE,"+row+","+col);
		
	}

	//handlers 
	private class WorkerThread extends Thread {
		private DatagramPacket rxPacket;

		public WorkerThread(DatagramPacket packet){
			this.rxPacket = packet;
		}

		@Override
		public void run(){
			// convert the rxPacket's payload to a string
			String payload = new String(rxPacket.getData(), 0, rxPacket.getLength())
			.trim();


			// dispatch request handler functions based on the payload's prefix
			String[] params = payload.split(",");
			acknowlege(params[0]);
			String command = params[1];



			if (command.startsWith("+ERROR")) {
				onErrorReceived(payload);
				return;
			}

			if (command.startsWith("+SUCCESS")) {
				onSuccessReceived(payload);
				return;
			}
			if(command.startsWith("game_")){
				onOpponentFound(command);

			}	
			if(command.startsWith("MOVE")){
				onMoveRequested(params[2],params[3]);

			}

			//reach here bad request 
			onBadRequest(payload);
		}


	}

	public void acknowlege(String ackid) {
		// TODO Auto-generated method stub
		Log.i("MyService", "Ack");
		//registration successful
		if(clientID == null)
		{
			clientID = ackid.split(":")[0];
			//name client clientID 
			send("NAME,"+clientID+","+clientID);
			isConnected = true;
			
			
		}
		send("ACK,"+ackid);

	}



	public void onMoveRequested(String row, String col) {
		// TODO Auto-generated method stub
		Log.i("MyService", "MoveReceived");
		
		// send movement data to main
		Message message = Message.obtain(null,MainActivity.MOVERECEIVED,
				Integer.parseInt(row), Integer.parseInt(col));

		try {
			myMessenger.send(message);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}


	public void onOpponentFound(String payload) {
		// TODO Auto-generated method stub
		Log.i("MyService", "OpponentFound");
		group = payload;
		// let main know we are good to go
		Message message = Message.obtain(null, MainActivity.OPPONENTFOUND);

		try {
			myMessenger.send(message);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void onSuccessReceived(String payload) {
		Log.i("MyService", "Success");
		// TODO Auto-generated method stub

	}

	private void onErrorReceived(String payload) {
		Log.i("MyService", "Some Error Occured");
		// TODO Auto-generated method stub
		//some error handling? 

	}

	private void onBadRequest(String payload) {
		Log.i("MyService", "Bad Request");
		try {
			send("BAD REQUEST\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendMSG(String message){
		String payload = "MSG,"+clientID + "," + group +","+message;
		send(payload);
		
	}
	private void send(String payload) {
		// TODO Auto-generated method stub
		//String ackID = "" + user.getID()  + ":" + user.currentReqID.incrementAndGet();
		// Append the ID + reuestID to the beginning of the payload
		//final String sendPayload = ackID + " " + payload +"\n";
		final String sendPayload = payload;
		final ScheduledExecutorService executor = Executors
				.newSingleThreadScheduledExecutor();
		Runnable sendTask = new Runnable() {
			public void run() {
				
				try {
					DatagramPacket txPacket = new DatagramPacket(
							sendPayload.getBytes(), sendPayload.length(), serverSocketAddress);
					socket.send(txPacket);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					executor.shutdown();
					throw new RuntimeException();	
					
				}


			}
		};

		//try to send message every ten second
		executor.scheduleAtFixedRate(sendTask, 0, 100000,
				SECONDS);
	}	
}



