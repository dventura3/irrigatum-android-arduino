package com.main.android;

import util.Utils;

import com.data.db.MyHelperDB;
import com.numpicker.NumberPicker;
import com.resource.arduino.Arduino;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ModifyConfig extends Activity implements OnSeekBarChangeListener {

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
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manual_c);
		
		myDB = new MyHelperDB(this);
		db = myDB.getWritableDatabase();
		
		ard = new Arduino(Utils.myActualDevice.getIP(), Utils.myActualDevice.getName());
		
		ckT=(CheckBox)findViewById(R.id.ckTemp);
		ckH=(CheckBox)findViewById(R.id.ckHum);
		ckI=(CheckBox)findViewById(R.id.ckIrrid);
		text_min_temp = (TextView)findViewById(R.id.temperature_min);
		bar_min_temp = (SeekBar)findViewById(R.id.temp_min_val);
		text_max_temp = (TextView)findViewById(R.id.temperature_max);
		bar_max_temp = (SeekBar)findViewById(R.id.temp_max_val);
		text_min_hum = (TextView)findViewById(R.id.humidity_min);
		bar_min_hum = (SeekBar)findViewById(R.id.hum_min_val);
		text_max_hum = (TextView)findViewById(R.id.humidity_max);
		bar_max_hum = (SeekBar)findViewById(R.id.hum_max_val);
		time_txt = (TextView)findViewById(R.id.irr);
		//time_P = (TimePicker) findViewById(R.id.timeP);
		pick = (NumberPicker) findViewById(R.id.timeP);
		pick.mySetEnable(false);
		text_thr = (TextView)findViewById(R.id.text_thr);
		bar_thr = (SeekBar)findViewById(R.id.seek_thr);
		
		//setto i valori iniziali in base ai dati salvati sul database
		setInitialValue();
		
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
		
		
		ckI.setOnClickListener(new OnClickListener() {
			  public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					time_txt.setVisibility(View.VISIBLE);
					pick.setVisibility(View.VISIBLE);
					text_thr.setVisibility(View.VISIBLE);
					bar_thr.setVisibility(View.VISIBLE);
				} else{
					time_txt.setVisibility(View.GONE);
					pick.setVisibility(View.GONE);
					text_thr.setVisibility(View.GONE);
					bar_thr.setVisibility(View.GONE);
				}
		 
			  }
		});
		
		
		bar_min_temp.setOnSeekBarChangeListener(this);
		
		
		bar_max_temp.setOnSeekBarChangeListener(this);
		
		
		bar_min_hum.setOnSeekBarChangeListener(this);
		
		
		bar_max_hum.setOnSeekBarChangeListener(this);
				
		
		bar_thr.setOnSeekBarChangeListener(this);
		
	
		btn_save = (Button)findViewById(R.id.save_on_db);
		btn_save.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				saveOnDatabase();
			}
		});
		
		set_arduino = (Button)findViewById(R.id.send_arduino);
		set_arduino.setVisibility(View.GONE);
		
	}
	
	
	public void setInitialValue(){
		if(Utils.myActualConfig.getT_min().equals("-") || Utils.myActualConfig.getT_max().endsWith("-")){
			ckT.setChecked(false);
			text_min_temp.setVisibility(View.GONE);
			bar_min_temp.setVisibility(View.GONE);
			text_max_temp.setVisibility(View.GONE);
			bar_max_temp.setVisibility(View.GONE);
		}else{
			ckT.setChecked(true);
			bar_min_temp.setProgress(Integer.parseInt(Utils.myActualConfig.getT_min()));
			bar_max_temp.setProgress(Integer.parseInt(Utils.myActualConfig.getT_max()));
			text_min_temp.setText("Temperature Min Value: "+Utils.myActualConfig.getT_min());
			text_max_temp.setText("Temperature Max Value: "+Utils.myActualConfig.getT_max());
		}
		
		if(Utils.myActualConfig.getH_min().equals("-") || Utils.myActualConfig.getH_max().equals("-")){
			ckH.setChecked(false);
			text_min_hum.setVisibility(View.GONE);
			bar_min_hum.setVisibility(View.GONE);
			text_max_hum.setVisibility(View.GONE);
			bar_max_hum.setVisibility(View.GONE);
		}else{
			ckH.setChecked(true);
			bar_min_hum.setProgress(Integer.parseInt(Utils.myActualConfig.getH_min()));
			bar_max_hum.setProgress(Integer.parseInt(Utils.myActualConfig.getH_max()));
			text_min_hum.setText("Humidity Min Value: "+Utils.myActualConfig.getH_min());
			text_max_hum.setText("Humidity Max Value: "+Utils.myActualConfig.getH_max());
		}
	
		if(Utils.myActualConfig.getThr_irrad().equals("-") || Utils.myActualConfig.getTime_irrad().equals("-")){
			ckI.setChecked(false);
			time_txt.setVisibility(View.VISIBLE);
			pick.setVisibility(View.VISIBLE);
			text_thr.setVisibility(View.VISIBLE);
			bar_thr.setVisibility(View.VISIBLE);
		}else{
			ckI.setChecked(true);
			text_thr.setText("Threshold of Irradiation: "+Utils.myActualConfig.getThr_irrad());
			bar_thr.setProgress(Integer.parseInt(Utils.myActualConfig.getThr_irrad()));
			pick.setCurrent(Integer.parseInt(Utils.myActualConfig.getTime_irrad()));
		}
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
	
	private void saveOnDatabase() {
		
		final String namePlant = Utils.myActualConfig.getName();
		final int idPlant = Utils.myActualConfig.getId();
		
		 AlertDialog.Builder builder = new AlertDialog.Builder(context);
		 builder.setTitle("Modify Configuration");
		 builder.setMessage("Do you confirm to modify element?");
		 builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
		      public void onClick(DialogInterface dialog, int id) {
			         System.out.println("CLICK YES");
			         
			         //estraggo dati settati dall'utente
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
						time = String.valueOf(pick.getCurrent());
					} else{
						thr = "-";
						time = "-";
					}
			         
					myDB.modifyPlantConfiguration(db, idPlant, h_min, h_max, t_min, t_max, thr, time);
					
					Utils.myActualConfig = null;
					
					finish();
			         
			  }
		});
		 builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
		      public void onClick(DialogInterface dialog, int id) {
			         System.out.println("CLICK NO");
			         //nothing to do
			  }
		});
		
		 AlertDialog alert = builder.create();
		 alert.show();
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	 @Override
     protected void onDestroy(){
    	super.onDestroy();
    	myDB.close();
    }	
	 
}
