package com.main.android;

import com.data.db.MyHelperDB;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ArduinoNew extends Activity {

	MyHelperDB myDB;
	SQLiteDatabase db;
	
	EditText dev_name;
	EditText dev_ip;
	Button btn_save;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.arduino_new);
		myDB = new MyHelperDB(this);
		db = myDB.getReadableDatabase();
		
		dev_name = (EditText) findViewById(R.id.dev_n);
		dev_ip = (EditText) findViewById(R.id.dev_ip);
		
		btn_save = (Button) findViewById(R.id.btn_save);
		btn_save.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startArduinoList();
			}
		});
	}

	 private void startArduinoList() {
		System.out.println("*** Value: << "+ dev_name.getText().toString() +" " + dev_ip.getText().toString() +" >>");
	 	myDB.insertNewDevice(db, dev_name.getText().toString(), dev_ip.getText().toString());
	 	Toast.makeText(ArduinoNew.this,"Thanks To Add new Device", Toast.LENGTH_SHORT).show();
	 	Intent intent = new Intent(this, ArduinoList.class);
	    startActivity(intent);
	 }

	@Override
    protected void onDestroy(){
    	super.onDestroy();
    	myDB.close();
    }	
}
