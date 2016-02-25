package com.main.android;

import util.Session;
import util.Utils;

import com.data.db.MyHelperDB;
import com.main.android.R;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.view.View;
import android.view.View.OnClickListener;


public class MainApp extends Activity {
	
	MyHelperDB myDB;
	SQLiteDatabase db;
	
	Button button;
	Button button_red;
	ImageView image;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		myDB = new MyHelperDB(this);
		db = myDB.getWritableDatabase();
		
		image = (ImageView) findViewById(R.id.imageView1);

		button_red = (Button) findViewById(R.id.b1);
		button_red.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startArduinoList();
			}
		});
				
	}


	 private void startArduinoList() {
			//go to list of arduino device
		    Intent intent = new Intent(this, ArduinoList.class);
		    startActivity(intent);
	 }

	 
	  protected void onDestroy(){
		super.onDestroy();
		myDB.close();
	  }	
}