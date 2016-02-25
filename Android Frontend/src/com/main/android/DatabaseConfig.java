package com.main.android;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;

import util.Configuration;
import util.Device;
import util.Utils;

import com.data.db.MyHelperDB;
import com.resource.arduino.Arduino;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class DatabaseConfig extends Activity  implements OnItemSelectedListener {

	MyHelperDB myDB;
	SQLiteDatabase db;
	
	Arduino ard;
	
	Context context;
	
	String[] array_spinner;
	
	ArrayList<Configuration> configList;
	ArrayAdapter<Configuration> adapter;
	ListView lview;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.database_c);
		myDB = new MyHelperDB(this);
		db = myDB.getReadableDatabase();
		
		ard = new Arduino(Utils.myActualDevice.getIP(), Utils.myActualDevice.getName());
		
		context = this;
		
		//popolo lo SPINNER e setto l'adapter
		popolaSpinner();
		
	}
	
	public void popolaSpinner(){
		//popolo lo SPINNER
		Cursor c = myDB.getPlantsName(db);
		int num_elem = c.getCount();
		System.out.println("*** Numero elementi letti da DB: << "+ num_elem +" >>");
		array_spinner = new String[num_elem];
		int count = 0;
		while (c.moveToNext()) {
				String name = c.getString(0);
				System.out.println("*** Found: << "+ name +" >>");
				array_spinner[count] = name;
				++count;
		}
		
		
		Spinner spinner = (Spinner) findViewById(R.id.spinner_name_plants);
		//set ascoltatore per lo spinner
		spinner.setOnItemSelectedListener(this);
		
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, array_spinner);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
	}

	
	 public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
	        // An item was selected. You can retrieve the selected item using
	        // parent.getItemAtPosition(pos)
		 	System.out.println("Elemento selezionato: " +  parent.getItemAtPosition(pos));
			
		 	String plantsName = parent.getItemAtPosition(pos).toString();
		 	
		 	//nella lista "configList" devo inserire gli oggetti "Configuration" che hanno
		 	//per nome quello selezionato dall'utente e prelevati dal Database
		 	createConfigListFromDB(plantsName);
			
		 	//una volta "popolata" la lista, devo rappresentarla graficamente andando a 
		 	//riempire la list View
			createListView();
	 }
	 
	 
	 public void createConfigListFromDB(String plantsName){
		 	//svuoto la lista di configuration - se è stata precedentemente usata
			configList= new ArrayList<Configuration>();
			
			Cursor c = myDB.getAllConfigInBaseOfName(db, plantsName);
			while (c.moveToNext()) {
					int config_id = c.getInt(0);
					String name = c.getString(1);
					String h_min = c.getString(2);
					String h_max = c.getString(3);
					String t_min = c.getString(4);
					String t_max = c.getString(5);
					String thr_irrad = c.getString(6);
					String time_irrad = c.getString(7);
					System.out.println("*** Found: << "+ config_id +" >>");
					System.out.println("*** TIME: << "+ time_irrad +" >>");
					
					Configuration tmp = new Configuration();
					tmp.setH_min(h_min);
					tmp.setH_max(h_max);
					tmp.setT_min(t_min);
					tmp.setT_max(t_max);
					tmp.setThr_irrad(thr_irrad);
					tmp.setTime_irrad(time_irrad);
					tmp.setId(config_id);
					tmp.setName(name);
					
					//add in list of configuration 
					configList.add(tmp);
			}
	 }
	 
	 
	 public void createListView(){
		 
		 adapter = new ArrayAdapter<Configuration>(this,R.layout.database_c_single_row,R.id.layout_single_raw,configList){
				@Override
				 public View getView (int position, View convertView, ViewGroup parent){
					ViewHolder holder=null;
					if(convertView==null){
						System.out.println("QUI");
						LayoutInflater inflater = LayoutInflater.from(DatabaseConfig.this);
						convertView= inflater.inflate(R.layout.database_c_single_row, null);
						convertView.setClickable(true);
						holder = new ViewHolder();
						holder.h_min=(TextView)convertView.findViewById(R.id.h_min);
						holder.h_max=(TextView)convertView.findViewById(R.id.h_max);
						holder.t_min=(TextView)convertView.findViewById(R.id.t_min);
						holder.t_max=(TextView)convertView.findViewById(R.id.t_max);
						holder.thr_irrad=(TextView)convertView.findViewById(R.id.thr_irrad);
						holder.time_irrad=(TextView)convertView.findViewById(R.id.time_irrad);
						holder.btn_modify=(Button)convertView.findViewById(R.id.btn_modify);
						holder.btn_remove=(Button)convertView.findViewById(R.id.btn_remove);
						holder.btn_send=(Button)convertView.findViewById(R.id.btn_send);
						convertView.setTag(holder);
					}else{
						 holder=(ViewHolder)convertView.getTag();
					}

					//ricavo l'attuale device della lista
					final Configuration configTMP = getItem(position);
					
					holder.h_min.setText("Min Value: "+ configTMP.getH_min()+"%");
					holder.h_max.setText("Max Value: "+ configTMP.getH_max()+"%");
					holder.t_min.setText("Min Value: "+ configTMP.getT_min()+"°");
					holder.t_max.setText("Max Value: "+ configTMP.getT_max()+"°");
					holder.thr_irrad.setText("Threasold: "+ configTMP.getThr_irrad()+"%");
					holder.time_irrad.setText("Time: "+ configTMP.getTime_irrad());
					
					holder.btn_modify.setOnClickListener(new OnClickListener() {
						public void onClick(View arg0) {
							startModifyConfigurationOnDB(configTMP);
						}
					});

					holder.btn_remove.setOnClickListener(new OnClickListener() {
						public void onClick(View arg0) {
							removeConfigurationOnDB(configTMP);
						}
					});
					
					holder.btn_send.setOnClickListener(new OnClickListener() {
						public void onClick(View arg0) {
							sendToArduino(configTMP);
						}
					});
					
					return convertView;
				}
				
				//class that used in to listview to know what item is checked
				class ViewHolder {
					 TextView h_min;
					 TextView h_max;
					 TextView t_min;
					 TextView t_max;
					 TextView thr_irrad;
					 TextView time_irrad;
					 Button btn_modify;
					 Button btn_remove;
					 Button btn_send;
				 }
			};
				
			//listview e set adapter
			lview = (ListView) findViewById( R.id.list );
			lview.setAdapter(adapter);
			
	 }
	 
	 
	 
	 private void startModifyConfigurationOnDB(Configuration config) {
		 	Utils.myActualConfig = config;
			Intent intent = new Intent(this, ModifyConfig.class);
			startActivity(intent);
			
			//aggiorno la grafica RIpopolando lo spinner
       	 	popolaSpinner();
	 }
	 
	 
	 private void removeConfigurationOnDB(Configuration config) {
		 
		 final int id_configuration = config.getId();
		 final String name_configuration = config.getName();
		 
		 AlertDialog.Builder builder = new AlertDialog.Builder(context);
		 builder.setTitle("Remove Configuration");
		 builder.setMessage("Do you confirm to remove element?");
		 builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
		      public void onClick(DialogInterface dialog, int id) {
			         System.out.println("CLICK YES");
			         myDB.removeSingleConfiguration(db, id_configuration);
			         
			         //verifico se c'è almeno una configurazione nel database... in modo da aggiornare correttamente la grafica
			         if(myDB.thereIsConfiguration(db)){
			        	 //aggiorno la grafica RIpopolando lo spinner
			        	 popolaSpinner();
			         } else{
			        	 popolaSpinner();
			        	 configList = new ArrayList<Configuration>();
			        	 createListView();
			         }
			  }
		});
		 builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
		      public void onClick(DialogInterface dialog, int id) {
			         System.out.println("CLICK OK");
			         //nothing to do
			  }
		});
		
		 AlertDialog alert = builder.create();
		 alert.show();
		 
	 }
	 
	 private void sendToArduino(Configuration config) {

			if(config.getH_min().equals("-") && config.getH_max().equals("-") ){
				ard.getTemp().setMinValue(String.valueOf(-1));
				ard.getTemp().setMaxValue(String.valueOf(-1));
			}else {				
				ard.getTemp().setMinValue(String.valueOf(config.getH_min()));
				ard.getTemp().setMaxValue(String.valueOf(config.getH_max()));
			}
			
			if(config.getT_min().equals("-") && config.getT_max().equals("-")){
				ard.getHum().setMinValue(String.valueOf(-1));
				ard.getHum().setMaxValue(String.valueOf(-1));
			}else {		
				ard.getHum().setMinValue(String.valueOf(config.getT_min()));
				ard.getHum().setMaxValue(String.valueOf(config.getT_max()));
			}
			
			if(config.getThr_irrad().equals("-") && config.getTime_irrad().equals("-")){
				ard.getLdr().setThr(String.valueOf(-1));
				ard.getLdr().setTime(String.valueOf(-1));
			}else {
				ard.getLdr().setThr(String.valueOf(config.getThr_irrad()));
				ard.getLdr().setTime(config.getTime_irrad());
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
		 	new UpdateTask(context).execute();
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

	 public void onNothingSelected(AdapterView<?> parent) {
	        // Another interface callback
	 }
	 
	 @Override
     protected void onDestroy(){
    	super.onDestroy();
    	myDB.close();
    }	
}
