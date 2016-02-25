package com.main.android;

import java.util.Calendar;

import org.json.JSONException;

import util.Utils;

import com.data.db.MyHelperDB;
import com.main.android.R;
import com.numpicker.NumberPicker;
import com.resource.arduino.Arduino;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TimePicker;


public class ManualConfig extends Activity implements OnSeekBarChangeListener {

	final Context context = this;
	
	MyHelperDB myDB;
	SQLiteDatabase db;
	
	Arduino ard;
	
	private SeekBar bar_min_temp;
	private SeekBar bar_max_temp;
	private SeekBar bar_min_hum;
	private SeekBar bar_max_hum;
	private TextView text_min_temp;
	private TextView text_max_temp;
	private TextView text_min_hum;
	private TextView text_max_hum;
	private CheckBox ckT;
	private CheckBox ckH;
	private CheckBox ckI;
	private TextView time_txt;
	//private TimePicker time_P;
	private TextView text_thr;
	private SeekBar bar_thr;
	private Button btn_save;
	private Button set_arduino;
	private NumberPicker pick;
	
	private int hour;
	private int minute;

	
	@SuppressLint("ParserError")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manual_c);
		
		myDB = new MyHelperDB(this);
		db = myDB.getWritableDatabase();
		
		ard = new Arduino(Utils.myActualDevice.getIP(), Utils.myActualDevice.getName());
		
		ckT=(CheckBox)findViewById(R.id.ckTemp);
		ckT.setOnClickListener(new OnClickListener() {
			  public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					text_min_temp.setVisibility(View.VISIBLE);
					bar_min_temp.setVisibility(View.VISIBLE);
					text_max_temp.setVisibility(View.VISIBLE);
					bar_max_temp.setVisibility(View.VISIBLE);
				} else{
					text_min_temp.setVisibility(View.GONE);
					bar_min_temp.setVisibility(View.GONE);
					text_max_temp.setVisibility(View.GONE);
					bar_max_temp.setVisibility(View.GONE);
				}
		 
			  }
		});

		ckH=(CheckBox)findViewById(R.id.ckHum);
		ckH.setOnClickListener(new OnClickListener() {
			  public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					text_min_hum.setVisibility(View.VISIBLE);
					bar_min_hum.setVisibility(View.VISIBLE);
					text_max_hum.setVisibility(View.VISIBLE);
					bar_max_hum.setVisibility(View.VISIBLE);
				} else{
					text_min_hum.setVisibility(View.GONE);
					bar_min_hum.setVisibility(View.GONE);
					text_max_hum.setVisibility(View.GONE);
					bar_max_hum.setVisibility(View.GONE);
				}
		 
			  }
		});
		
		ckI=(CheckBox)findViewById(R.id.ckIrrid);
		ckI.setOnClickListener(new OnClickListener() {
			  public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					time_txt.setVisibility(View.VISIBLE);
					pick.setVisibility(View.VISIBLE);
					//time_P.setVisibility(View.VISIBLE);
					text_thr.setVisibility(View.VISIBLE);
					bar_thr.setVisibility(View.VISIBLE);
				} else{
					time_txt.setVisibility(View.GONE);
					pick.setVisibility(View.GONE);
					//time_P.setVisibility(View.GONE);
					text_thr.setVisibility(View.GONE);
					bar_thr.setVisibility(View.GONE);
				}
		 
			  }
		});
		
		text_min_temp = (TextView)findViewById(R.id.temperature_min);
		bar_min_temp = (SeekBar)findViewById(R.id.temp_min_val);
		bar_min_temp.setOnSeekBarChangeListener(this);
		
		text_max_temp = (TextView)findViewById(R.id.temperature_max);
		bar_max_temp = (SeekBar)findViewById(R.id.temp_max_val);
		bar_max_temp.setOnSeekBarChangeListener(this);
		
		text_min_hum = (TextView)findViewById(R.id.humidity_min);
		bar_min_hum = (SeekBar)findViewById(R.id.hum_min_val);
		bar_min_hum.setOnSeekBarChangeListener(this);
		
		text_max_hum = (TextView)findViewById(R.id.humidity_max);
		bar_max_hum = (SeekBar)findViewById(R.id.hum_max_val);
		bar_max_hum.setOnSeekBarChangeListener(this);
		
		time_txt = (TextView)findViewById(R.id.irr);
		pick = (NumberPicker) findViewById(R.id.timeP);
		pick.mySetEnable(false);
		//time_P = (TimePicker) findViewById(R.id.timeP);
		//Calendar c = Calendar.getInstance();
		//hour = c.get(Calendar.HOUR_OF_DAY);
		//minute = c.get(Calendar.MINUTE);
		//timeP.setCurrentHour(hour);
		//timeP.setCurrentMinute(minute);
		
		text_thr = (TextView)findViewById(R.id.text_thr);
		bar_thr = (SeekBar)findViewById(R.id.seek_thr);
		bar_thr.setOnSeekBarChangeListener(this);
	
		btn_save = (Button)findViewById(R.id.save_on_db);
		btn_save.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				saveOnDatabase();
			}
		});
		
		set_arduino = (Button)findViewById(R.id.send_arduino);
		set_arduino.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				sendToArduino();
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
				
				setInitialValue();
			}
			
			dialog.cancel();
		}
	}

	public void setInitialValue(){
		
		if(ard.getTemp().getMinValue().equals("-99") || ard.getTemp().getMaxValue().equals("-99")){
			ckT.setChecked(false);
			text_min_temp.setVisibility(View.GONE);
			bar_min_temp.setVisibility(View.GONE);
			text_max_temp.setVisibility(View.GONE);
			bar_max_temp.setVisibility(View.GONE);
		}else{
			ckT.setChecked(true);
			bar_min_temp.setProgress(Integer.parseInt(ard.getTemp().getMinValue()));
			bar_max_temp.setProgress(Integer.parseInt(ard.getTemp().getMaxValue()));
			text_min_temp.setText("Temperature Min Value: "+ard.getTemp().getMinValue());
			text_max_temp.setText("Temperature Max Value: "+ard.getTemp().getMaxValue());
		}
		
		if(ard.getHum().getMinValue().equals("-1") || ard.getHum().getMaxValue().equals("-1")){
			ckH.setChecked(false);
			text_min_hum.setVisibility(View.GONE);
			bar_min_hum.setVisibility(View.GONE);
			text_max_hum.setVisibility(View.GONE);
			bar_max_hum.setVisibility(View.GONE);
		}else{
			ckH.setChecked(true);
			bar_min_hum.setProgress(Integer.parseInt(ard.getHum().getMinValue()));
			bar_max_hum.setProgress(Integer.parseInt(ard.getHum().getMaxValue()));
			text_min_hum.setText("Humidity Min Value: "+ard.getHum().getMinValue());
			text_max_hum.setText("Humidity Max Value: "+ard.getHum().getMaxValue());
		}
		
		if(ard.getLdr().getThr().equals("-1") || ard.getLdr().getTime().equals("-1")){
			ckI.setChecked(false);
			time_txt.setVisibility(View.VISIBLE);
			pick.setVisibility(View.VISIBLE);
			text_thr.setVisibility(View.VISIBLE);
			bar_thr.setVisibility(View.VISIBLE);
		}else{
			ckI.setChecked(true);
			text_thr.setText("Threshold of Irradiation: "+ard.getLdr().getThr());
			bar_thr.setProgress(Integer.parseInt(ard.getLdr().getThr()));
			pick.setCurrent(Integer.parseInt(ard.getLdr().getTime()));
		}
		
	}
	
	private void saveOnDatabase() {
		
		// get prompts.xml view
		LayoutInflater li = LayoutInflater.from(context);
		View promptsView = li.inflate(R.layout.prompts, null);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		alertDialogBuilder.setTitle("Insert Configuration Name:");
		// set prompts.xml to alertdialog builder
		alertDialogBuilder.setView(promptsView);

		final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);

		// set dialog message
		alertDialogBuilder.setCancelable(false)
			.setPositiveButton("OK",new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,int id) {
			    //get user input
			    Editable namePlant = userInput.getText();
			    
			    //save on Database
				String h_min = "";
				String h_max = "";
				if(ckH.isChecked()){
					h_min =  String.valueOf(bar_min_hum.getProgress()) ;
					h_max = String.valueOf(bar_max_hum.getProgress());
				} else{
					h_min = "-";
					h_max = "-";
				}
				
				String t_min = "";
				String t_max = "";
				if(ckT.isChecked()){
					t_min = String.valueOf(bar_min_temp.getProgress());
					t_max = String.valueOf(bar_max_temp.getProgress());
				} else{
					t_min = "-";
					t_max = "-";
				}
				
				String thr = "";
				String time = "";
				if(ckI.isChecked()){
					thr = String.valueOf(bar_thr.getProgress());
					//time = time_P.getCurrentHour().toString() + ":" + time_P.getCurrentMinute().toString();
					System.out.println("TIMEEEEEEEEEEEEEE: " + pick.getCurrent());
					time = String.valueOf(pick.getCurrent());
				} else{
					thr = "-";
					time = "-";
				}
				
				myDB.insertConfig(db, namePlant.toString(), h_min, h_max , t_min, t_max, thr, time );
				
			    }
			  })
			.setNegativeButton("Cancel",
			  new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,int id) {
				dialog.cancel();
			    }
			  });

		// create and show alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}
	
	private void sendToArduino() {

		if(ckT.isChecked()){
			ard.getTemp().setMinValue(String.valueOf(bar_min_temp.getProgress()));
			ard.getTemp().setMaxValue(String.valueOf(bar_max_temp.getProgress()));
		}else {
			ard.getTemp().setMinValue(String.valueOf(-1));
			ard.getTemp().setMaxValue(String.valueOf(-1));
		}
		
		if(ckH.isChecked()){
			ard.getHum().setMinValue(String.valueOf(bar_min_hum.getProgress()));
			ard.getHum().setMaxValue(String.valueOf(bar_max_hum.getProgress()));
		}else {
			ard.getHum().setMinValue(String.valueOf(-1));
			ard.getHum().setMaxValue(String.valueOf(-1));
		}
		
		if(ckI.isChecked()){
			ard.getLdr().setThr(String.valueOf(bar_thr.getProgress()));
			//NB: il tempo viene convertito tutto in SECONDI
			//String time = String.valueOf((time_P.getCurrentHour() * 3600) + (time_P.getCurrentMinute() * 60));
			String time = String.valueOf(pick.getCurrent());
			ard.getLdr().setTime(time);
		}else {
			ard.getLdr().setThr(String.valueOf(-1));
			ard.getLdr().setTime(String.valueOf(-1));
		}
		
		
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
	

	
	 public void onProgressChanged(SeekBar bar, int progress, boolean fromUser){
	    	Log.v("SfondoManagment", "bar: " + bar);

	        switch (bar.getId()) {
		        case R.id.hum_max_val:
		        	text_max_hum.setText("Humidity Max Value: "+progress);
			    	//Log.i("SfondoManagment","H MIN: " + progress);
		            break;
	
		        case R.id.hum_min_val:
		        	text_min_hum.setText("Humidity Min Value: "+progress);
			    	//Log.i("SfondoManagment","H MAX: " + progress);
		            break;
		            
		        case R.id.temp_max_val:
		        	text_max_temp.setText("Temperature Max Value: "+progress);
			    	//Log.i("SfondoManagment","TEMPERATURA MAX: " + progress);
		            break;
	
		        case R.id.temp_min_val:
		        	text_min_temp.setText("Temperature Min Value: "+progress);
			    	//Log.i("SfondoManagment","TEMPERATURA MIN: " + progress);
		            break;
		            
		        case R.id.seek_thr:
		        	text_thr.setText("Threshold of Irradiation: " + progress);
		        	break;
	        }
	 }

	 public void onStartTrackingTouch(SeekBar bar) {}

	 public void onStopTrackingTouch(SeekBar bar) {}
	 
	 
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
					return ard.mySetHttp();
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

	 
	 @Override
     protected void onDestroy(){
    	super.onDestroy();
    	myDB.close();
    }	

}
