package com.networks.networktictactoe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {


	public static final int OPPONENTFOUND = 0;
	public static final int MOVERECEIVED = 1; 


	private Button[][] buttons;
	private Game mGame;
	private MyService mService;
	private TextView textView;
	private boolean isBound; 
	private boolean myTurn;

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}


	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		Log.i("MainActivity","onStart");
		super.onStart();
		Intent intent = new Intent(this, MyService.class);
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);


	}


	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		Log.i("MainActivity","onStop");
		super.onStop();
		if (isBound) {
			unbindService(serviceConnection);
		}
	}
	
	private static class MyHandler extends Handler {
		WeakReference<MainActivity> wr;

		MyHandler(MainActivity activity){
			wr = new WeakReference<MainActivity> (activity);
		}

		@Override
		public void handleMessage (Message message){
			MainActivity activity = wr.get();
			if (activity != null) {
				switch(message.what) {
				case OPPONENTFOUND: 
					activity.startGame();
					break;
				case MOVERECEIVED:
					activity.processMove(message.arg1, message.arg2);
					break;
				default:
					super.handleMessage(message);
				}
				
			}
			else{
				Log.i("MainActivity","activity returned null");
			}
		}
	}
	
	
	private Messenger messenger = new Messenger(new MyHandler(this));

	private ServiceConnection serviceConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			Log.i("MainActivity","service connected");
			MyService.MyBinder b = (MyService.MyBinder)binder;
			mService = b.getService();
			mService.setMessenger(messenger);
			isBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			isBound = false;
		}
	};

	private class TicTacToeListener implements OnClickListener {

		int x;  
		int y;  

		public TicTacToeListener(int x, int y) {  
			this.x = x;  
			this.y = y;  
		}  




		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			if(!mGame.isEmpty(x, y))
			{
				textView.setText("Not Open");
			}
			else{
				
				//update model 
				mGame.setPosition(x,y,mGame.myPiece);
				//make unclickable
				buttons[x][y].setEnabled(false);
				buttons[x][y].setText(mGame.myPiece);
				
				//opponent's turn 
				myTurn = false;
				//user must wait
				disableButtons();
				//notify opponent
				mService.sendMove(x, y);
				//check effect of move
				if(mGame.isWinner(mGame.myPiece))
				{
					textView.setText(R.string.win);
				}
				else if(mGame.isWinner(mGame.otherPiece))
				{
					textView.setText(R.string.lose);
				}
				else if(mGame.isFull())
				{
					textView.setText(R.string.draw);
				}
				else
				{
					textView.setText(R.string.opponentTurn);
				}
			}
		}


	}






	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


	}


	public void processMove(int x, int y) {
		// TODO Auto-generated method stub
		if(!mGame.isEmpty(x, y))
		{
			textView.setText("Not Open");
		}
		else{
			//update model 
			mGame.setPosition(x,y,mGame.otherPiece);
			//make unclickable
			buttons[x][y].setEnabled(false);
			buttons[x][y].setText(mGame.myPiece);
			
			//my turn again
			myTurn = true;
			//check effect of move
			if(mGame.isWinner(mGame.myPiece))
			{
				textView.setText(R.string.win);
			}
			else if(mGame.isWinner(mGame.otherPiece))
			{
				textView.setText(R.string.lose);
			}
			else if(mGame.isFull())
			{
				textView.setText(R.string.draw);
			}
			else
			{
				textView.setText(R.string.myTurn);
				enableButtons();
			}
		}
	}



	public void startGame() {
		Log.i("MainActivity","game started");
		if(myTurn)
		{
			for (int i=0 ; i <= 2; i++) {  
				for (int j = 1; j <= 2; j++) {    
					buttons[i][j].setEnabled(true);  
				}  
			}  
			textView.setText(R.string.myTurn);
		}
		else
		{
			textView.setText(R.string.opponentTurn);
		}

	}


	public void opponentMove(int x, int y) {
		// TODO Auto-generated method stub

	}

	private void enableButtons()
	{
		for (int i=0 ; i <= 2; i++) {  
			for (int j = 1; j <= 2; j++) {
				//only enable unused buttons
				if(mGame.isEmpty(i, j))
					buttons[i][j].setEnabled(true);  
			}  
		}  
	}


	private void disableButtons(){
		for (int i=0 ; i <= 2; i++) {  
			for (int j = 1; j <= 2; j++) {    
				buttons[i][j].setEnabled(false);  
			}  
		}  
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		menu.add(0,0,0,"New game as X");
		menu.add(0,1,1,"New game as O");
		menu.add(0,2,2,"New random game");

		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override 
	public boolean onOptionsItemSelected(MenuItem item) {  
		switch(item.getItemId())
		{
		case 0:
			initGame('X');
			break;
		case 1:
			initGame('O');
			break;
		case 2:
			//random X or O
			initGame((Math.random()<0.5)?'X':'O');
			}
			return true;  
		}

		private void initGame(char c) {
			// TODO Auto-generated method stub
			buttons = new Button[4][4];

			mGame = new Game(c);
			mGame.clearBoard();
			myTurn = (c =='X')?true : false;	  

			// get the objects defined in main.xml  
			textView = (TextView) findViewById(R.id.dialogue);  

			buttons[0][0] = (Button) findViewById(R.id.one);  
			buttons[0][1] = (Button) findViewById(R.id.two);  
			buttons[0][2] = (Button) findViewById(R.id.three);  
			buttons[1][0] = (Button) findViewById(R.id.four);  
			buttons[1][1] = (Button) findViewById(R.id.five);  
			buttons[1][2] = (Button) findViewById(R.id.six);  
			buttons[2][0] = (Button) findViewById(R.id.seven);  
			buttons[2][1] = (Button) findViewById(R.id.eight);  
			buttons[2][2] = (Button) findViewById(R.id.nine);  

			textView.setText("Waiting for Opponent");  
			// add the click listeners for each button  
			for (int i=0 ; i <= 2; i++) {  
				for (int j = 1; j <= 2; j++) {  
					buttons[i][j].setOnClickListener(new TicTacToeListener(i, j));  
					buttons[i][j].setEnabled(false);  
				}  
			}
			mService.requestOpponent(mGame.myPiece);
		} 

	}  



