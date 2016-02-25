package com.main.android;

import org.json.JSONException;

import util.Device;
import util.Utils;

import com.resource.arduino.Arduino;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ForceConfig extends Activity {
	
	Arduino ard;

	private TextView txt_p1;
	private TextView txt_p2;
	private TextView txt_l1;
	private TextView txt_l2;
	private ToggleButton p1;
	private ToggleButton p2;
	private ToggleButton l1;
	private ToggleButton l2;
	private Button btn_force;
	
	Context context;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.force_c);
		
		context = this;
		
		ard = new Arduino(Utils.myActualDevice.getIP(), Utils.myActualDevice.getName());
		
		txt_p1 = (TextView) findViewById(R.id.p1);
		p1 = (ToggleButton) findViewById(R.id.toggle_pump_1);
		p1.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if(p1.isChecked())
					txt_p1.setText("Pump 1: ON");
				else
					txt_p1.setText("Pump 1: OFF");
			}
		});
		
		txt_p2 = (TextView) findViewById(R.id.p2);
		p2 = (ToggleButton) findViewById(R.id.toggle_pump_2);
		p2.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if(p2.isChecked())
					txt_p2.setText("Pump 2: ON");
				else
					txt_p2.setText("Pump 2: OFF");
			}
		});
		
		txt_l1 = (TextView) findViewById(R.id.l1);
		l1 = (ToggleButton) findViewById(R.id.toggle_lamp_1);
		l1.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if(l1.isChecked())
					txt_l1.setText("Lamp 1: ON");
				else
					txt_l1.setText("Lamp 1: OFF");
			}
		});
		
		txt_l2 = (TextView) findViewById(R.id.l2);
		l2 = (ToggleButton) findViewById(R.id.toggle_lamp_2);
		l2.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if(l2.isChecked())
					txt_l2.setText("Lamp 2: ON");
				else
					txt_l2.setText("Lamp 2: OFF");
			}
		});
		
		btn_force = (Button)findViewById(R.id.btn_force);
		btn_force.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				sendForceConfig();
			}
		});
		
		//verifico se la sessione è scaduta.
		//se è scaduta, non faccio neppure la richiesta http
		long currentTime = System.currentTimeMillis();
		if(Utils.mySessionObject.compareTime(currentTime)){
			setActualValue();
		} else{
			//reset Session:
			Utils.mySessionObject = null;
			//get error to user
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("Error");
			builder.setMessage("Session Expired! Go to Login!");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			      public void onClick(DialogInterface dialog, int id) {
			    	  Intent intent = new Intent(context, ArduinoList.class);
				      startActivity(intent);
				      finish();
				  }
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}
	
	
	public void setActualValue(){
		new InitialTask(this).execute();
	}
	
	
	private class InitialTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog dialog = null;
		private Context context = null;

		public InitialTask(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = ProgressDialog.show(context,"","Loading...");
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			try {
				ard.setSessionToken(Utils.mySessionObject.getSessionToken());
				return ard.reqHttp(ard.getArduinoIp());
			} catch (JSONException e) {	e.printStackTrace(); }
			return false;
		}

		@SuppressLint({ "ParserError", "ParserError" })
		@Override
		protected void onPostExecute(Boolean reply) {
		
			
			if (reply==false) {	
				//errore
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Error");
				builder.setMessage("Check Connection Status with Arduino!");
				
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				      public void onClick(DialogInterface dialog, int id) {
					         System.out.println("CLICK OK");
					         //finish();
					  }
				});
				AlertDialog alert = builder.create();
				alert.show();
			}else{
				
				 //lamp 1
				 if(ard.getBulb1().getState().equals("1")){
			        	l1.setChecked(true);
			        	txt_l1.setText("Lamp 1: ON");
				 }
			     else{
			    	 	l1.setChecked(false);
			    	 	txt_l1.setText("Lamp 1: OFF");
			     }
				 
				 //lamp 2
				 if(ard.getBulb2().getState().equals("1")){
			        	l2.setChecked(true);
			        	txt_l2.setText("Lamp 2: ON");
				 }
			     else{
			    	 	l2.setChecked(false);
			    	 	txt_l2.setText("Lamp 2: OFF");
			     }
				 
				 //pump 1
				 if(ard.getPump1().getState().equals("1")){
			        	p1.setChecked(true);
			        	txt_p1.setText("Pump 1: ON");
				 }
			     else{
			    	 	p1.setChecked(false);
			    	 	txt_p1.setText("Pump 1: OFF");
			     }
				 
				 //pump 2
				 if(ard.getPump2().getState().equals("1")){
			        	p2.setChecked(true);
			        	txt_p2.setText("Pump 2: ON");
				 }
			     else{
			    	 	p2.setChecked(false);
			    	 	txt_p2.setText("Pump 2: OFF");
			     }
			}
			
			dialog.cancel();
		}
	}
	
	
	
	private void sendForceConfig() {
		
		System.out.println("<< Pompa 1: " + p1.isChecked() + " >>");
		System.out.println("<< Pompa 2: " + p2.isChecked() + " >>");
		System.out.println("<< Luce 1: " + l1.isChecked() + " >>");
		System.out.println("<< Luce 2: " + l2.isChecked() + " >>");
		
		//verifico se la sessione è scaduta.
		//se è scaduta, non faccio neppure la richiesta http
		long currentTime = System.currentTimeMillis();
		if(Utils.mySessionObject.compareTime(currentTime)){
			sendReq();
		} else{
			//reset Session:
			Utils.mySessionObject = null;
			//get error to user
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("Error");
			builder.setMessage("Session Expired! Go to Login!");
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			      public void onClick(DialogInterface dialog, int id) {
			    	  Intent intent = new Intent(context, ArduinoList.class);
				      startActivity(intent);
				      finish();
				  }
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
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
				
				int pump1 = 0;
				if(p1.isChecked())
					pump1 = 1;
								
				int pump2 = 0;
				if(p2.isChecked())
					pump2 = 1;		
				
				int bulb1 = 0;
				if(l1.isChecked())
					bulb1 = 1;			
				
				int bulb2 = 0;
				if(l2.isChecked())
					bulb2 = 1;

				return ard.forceSetHttp(bulb1, bulb2, pump1, pump2);
				
			} catch (JSONException e) { System.out.println(">>> Eccezione!" ); }
			return false;
		}

		@Override
		protected void onPostExecute(Boolean reply) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			
			//set different message in base to result.
			if (reply==false) {
				builder.setTitle("Error");
				builder.setMessage("Check Connection Status with Arduino!");
			}else{
				builder.setTitle("OK");
				builder.setMessage("Arduino has been set successfully!");
			}
			
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			      public void onClick(DialogInterface dialog, int id) {
				         System.out.println("CLICK OK");
				  }
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}
	

}
