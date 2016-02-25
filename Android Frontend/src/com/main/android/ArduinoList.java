package com.main.android;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;

import com.data.db.MyHelperDB;
import com.resource.arduino.Arduino;

import util.Configuration;
import util.Device;
import util.Session;
import util.Utils;


import android.annotation.SuppressLint;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;




@SuppressLint({ "ParserError", "ParserError", "ParserError", "ParserError", "ParserError", "ParserError" })
public class ArduinoList extends Activity {

	
	MyHelperDB myDB;
	SQLiteDatabase db;
	
	ArrayList<Device> arduinoList;
	int numDevice;
	int numDeviceScanned;
	ArrayAdapter<Device> adapter;
	ArrayList<Device> arduinoList_checked;
	
	Button btn_add;
	Button btn_remove;
	Button btn_send;
	private ListView lview;
	LinearLayout main_layout;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.arduino_list);
		myDB = new MyHelperDB(this);
		db = myDB.getReadableDatabase();
		
		main_layout = (LinearLayout) findViewById( R.id.lay_main );
		 
		arduinoList = new ArrayList<Device>();
		getAllDeviceSaved();
		
		//LIST OF ITEM SELECTED
		arduinoList_checked = new ArrayList<Device>();
		
		
		btn_add = (Button) findViewById(R.id.btn_add_arduino);
		btn_add.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startArduinoNew();
			}
		});
		
		btn_remove = (Button) findViewById(R.id.btn_remove_arduino);
		btn_remove.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				removeElementsSelected();
			}
		});
		
		btn_send = (Button) findViewById(R.id.btn_send_arduino);
		btn_send.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startMainMenu();
			}
		});
		
	}

	public void createListViewUI(){
		adapter = new ArrayAdapter<Device>(this,R.layout.arduino_single_row,R.id.layout_single_raw,arduinoList){
			@Override
			 public View getView (int position, View convertView, ViewGroup parent){
				ViewHolder holder=null;
				
				if(convertView==null){
					System.out.println("QUI");
					LayoutInflater inflater = LayoutInflater.from(ArduinoList.this);
					convertView= inflater.inflate(R.layout.arduino_single_row, null);
					convertView.setClickable(true);
					holder = new ViewHolder();
					holder.name=(TextView)convertView.findViewById(R.id.name_arduino);
					holder.ip=(TextView)convertView.findViewById(R.id.ip_arduino);
					holder.status=(ImageView)convertView.findViewById(R.id.status_arduino);
					holder.ck_name=(CheckBox)convertView.findViewById(R.id.ck_name_arduino);
					convertView.setTag(holder);
				}else{
					 holder=(ViewHolder)convertView.getTag();
				}

				//ricavo l'attuale device della lista
				final Device dev = getItem(position);
				
				//System.out.println("dev.getName() " + dev.getName());
				//System.out.println("dev.getIP() " + dev.getIP());
				//System.out.println("dev.getStatus() " + dev.getStatus());
				//System.out.println("dev.getId() " + dev.getId());
				
				holder.name.setText(dev.getName());
				holder.ip.setText("IP: "+dev.getIP());
				int status = dev.getStatus();
				if(status==0)
					holder.status.setImageResource(R.drawable.status_offline);
				else
					holder.status.setImageResource(R.drawable.status_online);
				
				holder.ck_name.setOnClickListener(new OnClickListener() {
					  public void onClick(View v) {
							if (((CheckBox) v).isChecked()) {
								System.out.println("checked: dev.getId() " + dev.getId());
								  arduinoList_checked.add(dev);
							} else{
								System.out.println("NOT checked: dev.getId() " + dev.getId());
								int index = 0;
								int index_tmp = 0;
								Iterator<Device> it = arduinoList_checked.iterator();
								while(it.hasNext()){
									if(it.next().getId() == dev.getId())
										index = index_tmp;
									index_tmp++;
								}
								arduinoList_checked.remove(index);
							}					 
						  }
				});
				
				return convertView;
			}
			
			//class that used in to listview to know what item is checked
			class ViewHolder {
				 TextView name;
				 TextView ip;
				 ImageView status;
				 CheckBox ck_name;
			 }
		};
		
		//listview e set adapter
		lview = (ListView) findViewById( R.id.list );
		lview.setAdapter(adapter);
	}
	
	private void startArduinoNew() {
		Intent intent = new Intent(this, ArduinoNew.class);
		startActivity(intent);
	}
	
	private void removeElementsSelected() {
		if(arduinoList_checked.size()<=0)
			Toast.makeText(ArduinoList.this,"Error! You didn't select any Devices!", Toast.LENGTH_SHORT).show();
		else{
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			 builder.setTitle("Remove Arduino Configuration");
			 builder.setMessage("Do you confirm to remove selected element?");
			 builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
			      public void onClick(DialogInterface dialog, int id) {
				            System.out.println("CLICK YES");
							
				            Iterator<Device> it = arduinoList_checked.iterator();
							while(it.hasNext()){
								System.out.println("prima di rimuovere un elemento " );
								Device dev = it.next();
								myDB.removeDevice(db,dev.getId());
							}
							
							//svuoto la lista di elementi da rimuovere -> perchè tanto li ho già rimossi
							arduinoList_checked = new ArrayList<Device>();
							
							//aggiorno grafica
							adapter.clear();
							getAllDeviceSaved();
							//adapter.notifyDataSetChanged();
							//lview.setAdapter(adapter);
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
	}

	
	private void startMainMenu() {
		if(arduinoList_checked.size()==1){
			Iterator<Device> it = arduinoList_checked.iterator();
			while(it.hasNext()){
				//SET actual DEVICE 
				Utils.myActualDevice = it.next();
				System.out.println("ID UTILS DEVICE:  " + Utils.myActualDevice.getId() );
			}
			
			
			//se l'utente aveva precedentemente cliccato su Remembar, non faccio comparire la schermata 
			//di login, ma verifico che la sessione.... se è scaduta faccio comparire un errore di
			//"sessione scaduta" altriminti faccio comparire direttamente il MainMenu
			/*
			int there_is_remember = myDB.getFirstAccessID(db);
			 if(there_is_remember==-1){
				 	//go to login activity
				 	Intent intent = new Intent(this, LoginHandler.class);
				 	startActivity(intent);
			 } else {
				 	//set session
					Utils.mySessionObject = new Session();
					Utils.mySessionObject.setIDuser(there_is_remember);
					//go to list of arduino device
				    Intent intent = new Intent(this, ArduinoList.class);
				    startActivity(intent);
			 }
			 */
			
		    Intent intent = new Intent(this, LoginHandler.class);
		    startActivity(intent);
		} else if(arduinoList_checked.size()>1) {
			Toast.makeText(ArduinoList.this,"Error! You must select only one Device!", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(ArduinoList.this,"Error! You didn't select any Devices!", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void getAllDeviceSaved(){
		System.out.println("*** dentro getAllDeviceSaved() >>");
		numDeviceScanned = 0;
		Cursor c = myDB.getAllDevice(db);
		numDevice = c.getCount(); //numero di Device trovati nel database
		System.out.println("Num Dev trovati in DB: " + numDevice);
		while (c.moveToNext()) {
			int dev_id = c.getInt(0);
			String dev_name = c.getString(1);
			String dev_ip = c.getString(2);
			System.out.println("*** Found: << "+ dev_id +" - " + dev_name + " - " + dev_ip +" >>");
			Device tmpDEV = new Device(dev_id, 0, dev_name, dev_ip);
			showActualState(tmpDEV);
		}
	}
	
	public void showActualState( Device dev){
		new UpdateTask(this, dev).execute();
	}
	
	@SuppressLint("ParserError")
	private class UpdateTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog dialog = null;
		private Context context = null;
		private Device dev;

		public UpdateTask(Context context, Device dev) {
			this.context = context;
			this.dev = dev;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = ProgressDialog.show(context,"","Loading...");
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			try {
				Arduino ard = new Arduino(dev.getIP(), "");
				return ard.reqStatus();
			} catch (JSONException e) {	e.printStackTrace(); }
			return false;
		}

		@SuppressLint({ "ParserError", "ParserError" })
		@Override
		protected void onPostExecute(Boolean reply) {
			
			if (reply==false) {	
				dev.setStatus(0);
				System.out.println("TMP STATUS 0");
			}else{
				dev.setStatus(1);
				System.out.println("TMP STATUS 1");
			}
			arduinoList.add(dev);
			
			buildUI();
			
			dialog.cancel();
		}
	}
	
	public synchronized void buildUI(){
		numDeviceScanned++;
		System.out.println("Num Device Scanned: " + numDeviceScanned);
		if(numDeviceScanned==numDevice){
			System.out.println("STESSO NUMERO DI ELEMENTI");
			//aggiorno grafica
			createListViewUI();
		}
	}
	
	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	myDB.close();
    }	
}
