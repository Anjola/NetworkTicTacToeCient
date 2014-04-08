package com.networks.networktictactoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.SECONDS;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class MyService extends Service {

	public static final int SERVERPORT= 20000;
	public static final String SERVERADDRESS = "54.186.253.58";
	public static final int MAXPACKETSIZE = 512;
	//stores all messages that have not been acknowledged. maps an ackId to the Scheduled sender 
	protected static final Map<String, ScheduledExecutorService> pendingAck = 
			Collections.synchronizedMap(new HashMap<String, ScheduledExecutorService>());

	//for unique id generation for ackID
	protected static final AtomicLong  acKID = new AtomicLong();



	private String clientID = null; 
	private final IBinder mBinder = new MyBinder();

	public DatagramSocket socket;
	public InetSocketAddress serverSocketAddress;
	private String group = null;
	public Messenger myMessenger;
	private AtomicBoolean isRegistered = new AtomicBoolean(); 
	private AtomicBoolean isMyTurn = new AtomicBoolean();
	private ScheduledExecutorService registerExecutor;
	private AtomicBoolean opponentFound =new AtomicBoolean();
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
					registerClient();
					while(true)
					{
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
					}

				} catch (SocketException e) {
					// TODO Auto-generated catch block
					Log.i("Exception",e.getMessage());
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.i("Exception",e.getMessage());
					e.printStackTrace();
				}




			}
		}.start();
	}
	protected void registerClient() {
		final String sendPayload = "REGISTER";
		registerExecutor = Executors
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

				}


			}
		};
		//try to connect every 25 seconds
		registerExecutor.scheduleAtFixedRate(sendTask, 0, 25,
				SECONDS);
	}	


	public void requestOpponent(char piece)
	{
		if(isRegistered.get())
		{
			isMyTurn.set(piece == 'X'?true:false);
			send("JOIN,"+clientID+",waiting"+piece);
		}
		else
		{
			Log.i("MyService","Requesting without Registering");
		}
	}



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
			String command = params[0];
			if(params.length>1);
				command = params[1];
			Log.i("Verbose",payload);
			if(command.startsWith("REGISTERED")){
				onRegistered(params[0]);
				return;
			}

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
				return;

			}	
			if(command.startsWith("MOVE")){
				onMoveRequested(params[2],params[3]);
				return;
			}
			if(payload.startsWith("ACK")){
				onACKReceived(params[1]);
				return;
			}

			//reach here bad request 
			onBadRequest(payload);
		}


	}

	public void acknowlege(final String ackid) {
		// TODO Auto-generated method stub
		Log.i("MyService", "Ack,"+ ackid);
		final String payload = "ACK," + ackid;
		new Thread(){

			@Override
			public void run(){
				byte[] buf = payload.getBytes();

				try {
					//send ack only once. server handles invalid id's
					DatagramPacket packet= new DatagramPacket(buf, buf.length, serverSocketAddress);
					socket.send(packet);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}
			}
		}.start();

	}


	public void onRegistered(String ackid) {
		// TODO Auto-generated method stub
		//registration successful
		registerExecutor.shutdown();
		if(clientID == null)
		{
			clientID = ackid.split(":")[0];
			//name client clientID 
			send("NAME,"+clientID+","+clientID);
			isRegistered.set(true);


		}

	}

	public void onMoveRequested(String row, String col) {
		// TODO Auto-generated method stub
		Log.i("MyService", "MoveReceived");

		// send movement data to main
		if(!isMyTurn.get())
		{
			Message message = Message.obtain(null,MainActivity.MOVERECEIVED,
					Integer.parseInt(row), Integer.parseInt(col));
			try {
				myMessenger.send(message);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			isMyTurn.set(true);
		}
		else
		{
			//repeat request ignore
			Log.i("MyService", "repeat move request");
		}

	}


	public void onOpponentFound(String payload) {
		// TODO Auto-generated method stub
		Log.i("MyService", "OpponentFound");

		if(!opponentFound.get())
		{
			opponentFound.set(true);
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
		else
		{
			//ignore repeat requests
			Log.i("MyService", "repeat found request");
		}

	}

	private void onACKReceived(String ackid) {
		Log.i("MyService", "Ack Received:"+ ackid);
		ScheduledExecutorService executor = pendingAck.get(ackid);
		if (executor != null) 
		{	
			executor.shutdown();
			pendingAck.remove(ackid);
			Log.i("OnAckRecieved","ExecutorShutdown");
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
		Log.i("BadRequest",payload);
	}


	public void sendMove( int row, int col){
		Log.i("MyService", "sendMove");
		isMyTurn.set(false);
		sendMSG("MOVE,"+row+","+col);

	}

	private void sendMSG(String message){
		String payload = "MSG,"+clientID + "," + group +","+message;
		send(payload);

	}
	private void send(String payload) {

		final String ackid  =""+ acKID.getAndIncrement();
		final String sendPayload = ackid+","+ payload;
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
					//throw new RuntimeException();	
				}


			}
		};

		//try to send message every 15 seconds
		executor.scheduleAtFixedRate(sendTask, 0, 15,
				SECONDS);
		pendingAck.put(ackid, executor);
	}

	public void sendPoll() {
		send("POLL,"+clientID);
		Toast.makeText(getApplicationContext(), "Polling Server...",
				Toast.LENGTH_SHORT).show();

	}	
}



