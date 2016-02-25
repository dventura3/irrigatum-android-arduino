package com.main.android;

import org.json.JSONException;

import util.Utils;

import com.data.db.MyHelperDB;
import com.main.android.R;
import com.resource.arduino.Arduino;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainMenu extends Activity implements OnSeekBarChangeListener{

	MyHelperDB myDB;
	SQLiteDatabase db;
	
	Arduino ard;
	
	private TextView textProgress;
	private Button button1;
	private Button button2;
	private Button button3;
	private Button button4;
	private Button button6;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu); 
		
		ard = new Arduino(Utils.myActualDevice.getIP(), Utils.myActualDevice.getName());
		
		//reset della attuale configuration
		Utils.myActualConfig = null;
		
		myDB = new MyHelperDB(this);
		db = myDB.getWritableDatabase();
		
		button1 = (Button) findViewById(R.id.button1);
		button1.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startManualConfig();
			}
		});
		
		
		button2 = (Button) findViewById(R.id.button2);
		button2.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startDatabaseConfig();
			}
		});
		
		
		button3 = (Button) findViewById(R.id.button3);
		button3.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startShowActualState();
			}
		});
		
		
		button4 = (Button) findViewById(R.id.button4);
		button4.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startForceConfig();
			}
		});
		
		button6 = (Button) findViewById(R.id.button6);
		button6.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startCam();
			}
		});

	}
	
	private void startManualConfig() {
    	Intent intent = new Intent(this, ManualConfig.class);
    	startActivity(intent);
	}
	
	private void startDatabaseConfig() {
    	Intent intent = new Intent(this,DatabaseConfig.class);
    	startActivity(intent);
	}
	
	private void startShowActualState() {
    	Intent intent = new Intent(this, ShowActualState.class);
    	startActivity(intent);
	}
	
	private void startForceConfig() {
    	Intent intent = new Intent(this, ForceConfig.class);
    	startActivity(intent);
	}
	
	private void startCam() {
    	Intent intent = new Intent(this,  Cam.class);
    	startActivity(intent);
	}
	

	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	    	// change progress text label with current seekbar value
	    	textProgress.setText("The value is: "+progress);
	 }

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
	 
	public boolean onCreateOptionsMenu(Menu  menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.user_profile, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem  item) {
		switch (item.getItemId()) {
 
		case R.id.logoutMenu:
			//prima chiudo la sessione sull'arduino
			sendReq();
			break;
		}
		return true;
	}
	
	
	public void sendReq(){
		new UpdateTask(this).execute();
	}
	
	private class UpdateTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog dialog = null;
		private Context context = null;

		public UpdateTask(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			try {
				ard.setSessionToken(Utils.mySessionObject.getSessionToken());			
				return ard.endLogin();
			} catch (JSONException e) { System.out.println(">>> Eccezione!" ); }
			return false;
		}

		@Override
		protected void onPostExecute(Boolean reply) {
			
			if (reply==false) {
				System.out.println("Check Connection Status with Arduino!");
				
				//se l'arduino non risponde, dico all'utente di riprovare a chiudere
				//la sessione piu' tardi
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Error");
				builder.setMessage("Check Connection Status with Arduino!");
				//in caso di errore mostro un messaggio di errore
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				      public void onClick(DialogInterface dialog, int id) {
					        System.out.println("CLICK OK");
					        
					        /*
					        //FIXME: DA CANCELLARE
							//cancello tutto...
							myDB.deleteUser(db);
							myDB.freeFirstAccess(db);
							Utils.mySessionObject = null;
							Intent intent = new Intent(context, ArduinoList.class);
						 	startActivity(intent);
						 	finish();
						 	*/
					         
					  }
				});
				AlertDialog alert = builder.create();
				alert.show();
				
			}else{
				//in questo caso:
				//ricevo FAIL = la sessione è già scaduto -> bene!
				//ricevo OK = ho chiuso la sessione -> bene!
				//in pratica qualunque tipo di result ricevo -> non me ne frega nulla!
				if(ard.getResultReq().equals("OK"))
					System.out.println("Arduino has been set successfully!");
				else
					System.out.println("Session Expired!");
				
				
				//cancello elemento in user table
				//cancello elemento in first table
				//cancello session locale
				//torno all'activity iniziale con la lista di device
				myDB.deleteUser(db);
				myDB.freeFirstAccess(db);
				Utils.mySessionObject = null;
				Intent intent = new Intent(context, ArduinoList.class);
			 	startActivity(intent);
			 	finish();
			 	
				
			}
			

		}
	}
	
	
	
}
