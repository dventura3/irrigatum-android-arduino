package com.main.android;

import org.json.JSONException;

import util.Utils;

//import util.DrawThermometer;

import com.resource.arduino.Arduino;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "ParserError", "ParserError", "ParserError" })
public class ShowActualState extends Activity {
	
	ScrollView sv;
	DrawThermometer therm;
	Arduino ard;
	
	int altezza_therm;
	
	Context context;
	
	int canStop;
	
	long timeSleep = 5000;
	
	TextView labely; //temeperature
	TextView label2; //humidity
	TextView label4; //irradiation
	ImageView lamp1;
	ImageView lamp2;
	ImageView faucet1;
	ImageView faucet2;
	
	int primo = 1;  //il primo thread lo setterà a zero in modo che solo il primo thread visualizzerà la
					//dialog per l'aggiornamento

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		context = this;
		
		/*
		sv = new ScrollView(this);
		sv.setBackgroundResource(R.drawable.background);
		setContentView(sv);
		*/
		
		canStop = 0; //se è 1 -> il thread ciclico si ferma;
        
		ard = new Arduino(Utils.myActualDevice.getIP(), Utils.myActualDevice.getName());
		
		createUI(this);
		
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
	
	public synchronized int canGo(int val){
		return val;
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
			if(primo==1){
				dialog = ProgressDialog.show(context,"","Loading...");
			}
			
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			try {
				ard.setSessionToken(Utils.mySessionObject.getSessionToken());
				return ard.reqHttp(ard.getArduinoIp());
			} catch (JSONException e) {	e.printStackTrace(); }
			return false;
		}

		@Override
		protected void onPostExecute(Boolean reply) {
			if (reply==false) {
				// There is a communication error with Arduino
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Error");
				builder.setMessage("Check Connection Status with Arduino!");
				
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				      public void onClick(DialogInterface dialog, int id) {
					         System.out.println("CLICK OK");
					         finish();
					  }
				});
				AlertDialog alert = builder.create();
				alert.show();
				//finish();
			}else{
				//createUI(context);
				
				System.out.println("modifico grafica");
				
				updateUI(context);
				if(primo==1){
					dialog.cancel();
					primo=0;
				}
				
				if(canStop == 0){
					//verifico se la sessione è scaduta.
					//se è scaduta, non faccio neppure la richiesta http
					long currentTime = System.currentTimeMillis();
					if(Utils.mySessionObject.compareTime(currentTime)){
					
						try{
							System.out.println("STO DORMENDO");
							Thread.sleep(timeSleep);
						} catch (Exception e ){ };
					
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
				
				
			}
		}
	}

	public void updateUI(Context context){
		int gradi = Integer.parseInt(ard.getTemp().getActualValue());
		altezza_therm = getThermometerHigth(gradi);
		therm.invalidate();
		
        if(ard.getTemp().getActualValue().equals("-99"))
        	labely.setText( "- °C");
        else
        	labely.setText( ard.getTemp().getActualValue()+"°C");
		
        if( ard.getHum().getActualValue().equals("-1"))
        	label2.setText( "- %");
        else
        	label2.setText( ard.getHum().getActualValue()+"%");
		
        if(ard.getLdr().getThr().equals("-1"))
        	label4.setText("- %");
        else
        	label4.setText(ard.getLdr().getThr()+"%");
		
		if(ard.getBulb1().getState().equals("1"))
        	lamp1.setImageResource(R.drawable.lamp_accesa);
        else
        	lamp1.setImageResource(R.drawable.lamp_spenta);
		
		if(ard.getBulb2().getState().equals("1"))
        	lamp2.setImageResource(R.drawable.lamp_accesa);
        else
        	lamp2.setImageResource(R.drawable.lamp_spenta);
		
        if(ard.getPump1().getState().equals("1"))
        	faucet1.setImageResource(R.drawable.rub_aperto);
        else
        	faucet1.setImageResource(R.drawable.rub_chiuso);
		
        if(ard.getPump2().getState().equals("1"))
        	faucet2.setImageResource(R.drawable.rub_aperto);
        else
        	faucet2.setImageResource(R.drawable.rub_chiuso);
	}
	
	public void createUI(Context context){
			
		sv = new ScrollView(context);
		sv.setBackgroundResource(R.drawable.background);
		setContentView(sv);
		sv.destroyDrawingCache();
		
		 /* 
         *  Termometro + valore umidità e Irradiazione
         */ 
 		int gradi = Integer.parseInt(ard.getTemp().getActualValue());
		altezza_therm = getThermometerHigth(gradi);		
		therm = new DrawThermometer(context);
		therm.setLayoutParams(new LayoutParams(80, 220));
		therm.setPadding(10, 10, 10, 0);
		
		TextView labelx = new TextView(context);
        labelx.setText("Temperature: ");
        labelx.setGravity(Gravity.CENTER);
        labelx.setTextColor(Color.WHITE);
        labelx.setTextSize(20);
        labely = new TextView(context);
        if(ard.getTemp().getActualValue().equals("-99"))
        	labely.setText( "- °C");
        else
        	labely.setText( ard.getTemp().getActualValue()+"°C");
        labely.setGravity(Gravity.CENTER);
        labely.setTextColor(Color.WHITE);
        labely.setTextSize(40);
        labely.setPadding(8, 5, 0, 10);

		TextView label1 = new TextView(context);
        label1.setText("Humidity: ");
        label1.setGravity(Gravity.CENTER);
        label1.setTextColor(Color.WHITE);
        label1.setTextSize(20);
        label2 = new TextView(context);
        if( ard.getHum().getActualValue().equals("-1"))
        	label2.setText( "- %");
        else
        	label2.setText( ard.getHum().getActualValue()+"%");
        label2.setGravity(Gravity.CENTER);
        label2.setTextColor(Color.WHITE);
        label2.setTextSize(40);
        label2.setPadding(8, 5, 0, 10);
        
        TextView label3 = new TextView(context);
        label3.setText("Irradiation: ");
        label3.setGravity(Gravity.CENTER);
        label3.setTextColor(Color.WHITE);
        label3.setTextSize(20);
        label4 = new TextView(context);
        if(ard.getLdr().getThr().equals("-1"))
        	label4.setText("- %");
        else
        	label4.setText(ard.getLdr().getThr()+"%");
        label4.setGravity(Gravity.CENTER);
        label4.setTextColor(Color.WHITE);
        label4.setTextSize(40);
        label4.setPadding(8, 5, 0, 10);
        
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.addView(labelx);
        ll.addView(labely);
        ll.addView(label1);
        ll.addView(label2);
        ll.addView(label3);
        ll.addView(label4);
		
		LinearLayout rl = new LinearLayout(context);
		rl.setOrientation(LinearLayout.HORIZONTAL);
		rl.setGravity(Gravity.CENTER);
		rl.addView(therm);
		rl.addView(ll);
		
		TableRow row1 = new TableRow(context);
        row1.setGravity(Gravity.CENTER);
        row1.setPadding(0, 10, 0, 0);
        row1.addView(rl); 
        
		/*
		 *  Lampadina 1 - accesa o spenta:
		 */
        TextView label6 = new TextView(context);
        label6.setText("Lamp 1:");
        label6.setTextColor(Color.WHITE);
        label6.setTextSize(30);
        label6.setGravity(Gravity.CENTER);
        
        lamp1 = new ImageView(context);
        if(ard.getBulb1().getState().equals("1"))
        	lamp1.setImageResource(R.drawable.lamp_accesa);
        else
        	lamp1.setImageResource(R.drawable.lamp_spenta);
        lamp1.setScaleType(ScaleType.CENTER);
        
		TableRow row3 = new TableRow(context);
        row3.setPadding(0, 5, 0, 10);
        row3.addView(label6); 
        row3.addView(lamp1); 
        
		/*
		 *  Lampadina 2 - accesa o spenta:
		 */
        TextView label7 = new TextView(context);
        label7.setText("Lamp 2:");
        label7.setTextColor(Color.WHITE);
        label7.setTextSize(30);
        label7.setGravity(Gravity.CENTER);
        
        lamp2 = new ImageView(context);
        if(ard.getBulb2().getState().equals("1"))
        	lamp2.setImageResource(R.drawable.lamp_accesa);
        else
        	lamp2.setImageResource(R.drawable.lamp_spenta);
        lamp2.setScaleType(ScaleType.CENTER);
        
		TableRow row4 = new TableRow(context);
        row4.setPadding(0, 5, 0, 0);
        row4.addView(label7); 
        row4.addView(lamp2); 
        
		/*
		 *  Rubinetto 1 - aperto o chiuso:
		 */
        TextView label8 = new TextView(context);
        label8.setText("Faucet 1:");
        label8.setTextColor(Color.WHITE);
        label8.setTextSize(30);
        label8.setGravity(Gravity.CENTER);
        
        faucet1 = new ImageView(context);
        if(ard.getPump1().getState().equals("1"))
        	faucet1.setImageResource(R.drawable.rub_aperto);
        else
        	faucet1.setImageResource(R.drawable.rub_chiuso);
        faucet1.setScaleType(ScaleType.CENTER);
        
		TableRow row2 = new TableRow(context);
        row2.setPadding(0, 10, 0, 0);
        row2.addView(label8); 
        row2.addView(faucet1); 
        
        /*
		 *  Rubinetto 2 - aperto o chiuso:
		 */
        TextView label9 = new TextView(context);
        label9.setText("Faucet 2:");
        label9.setTextColor(Color.WHITE);
        label9.setTextSize(30);
        label9.setGravity(Gravity.CENTER);
        
        faucet2 = new ImageView(context);
        if(ard.getPump2().getState().equals("1"))
        	faucet2.setImageResource(R.drawable.rub_aperto);
        else
        	faucet2.setImageResource(R.drawable.rub_chiuso);
        faucet2.setScaleType(ScaleType.CENTER);
        
		TableRow row5 = new TableRow(context);
        row5.setPadding(0, 5, 0, 0);
        row5.addView(label9); 
        row5.addView(faucet2); 
        
        /*
         * TableLayout che deve essere contenuta dentro la ScrollView
         */
		TableLayout tableLayout = new TableLayout(context);
        tableLayout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        tableLayout.addView(row1);
        tableLayout.addView(row3);
        tableLayout.addView(row4);
        tableLayout.addView(row2);
        tableLayout.addView(row5);
        tableLayout.setColumnShrinkable(0, true);
        tableLayout.setColumnStretchable(1, true);
        tableLayout.setPadding(5, 5, 5, 5);

        /*
         * Aggiungo l'intero layout allo ScrolView
         */
		sv.addView(tableLayout);
	}
	
	
	public int getThermometerHigth(int thr_value){
		//se metto: altezza_therm = 103 -> nel termometro corrisponde circa a 20°
		//se riduco di 20 pixel -> ho 10° in più (es: 83 = 30°)
		// -30° = 203
		// -20° = 183
		// -10° = 163
		//   0° = 143
		//  10° = 123
		//  20° = 103
		//  30° = 83
		//  40° = 63
		//  50° = 43
		//  60° = 23
		int tmp_value = 203;
		if (thr_value>=-30 && thr_value<-20){
			// -30 < thr_value < -20
			tmp_value = 203 + ((thr_value % 30) * 2);
		} else if (thr_value>=-20 && thr_value<-10){
			tmp_value = 183 + ((thr_value % 20) * 2);
		} else if (thr_value>=-10 && thr_value<0){
			tmp_value = 163 + ((thr_value % 10) * 2);
		} else if (thr_value==0){
			tmp_value = 143;
		} else if (thr_value>0 && thr_value<10){
			tmp_value = 143 - ((thr_value % 0) * 2);
		} else if (thr_value>=10 && thr_value<20){
			tmp_value = 123 - ((thr_value % 10) * 2);
		} else if (thr_value>=20 && thr_value<30){
			tmp_value = 103 - ((thr_value % 20) * 2);
		} else if (thr_value>=30 && thr_value<40){
			tmp_value = 83 - ((thr_value % 30) * 2);
		} else if (thr_value>=40 && thr_value<50){
			tmp_value = 63 - ((thr_value % 40) * 2);
		} else if (thr_value>=50 && thr_value<60){
			tmp_value = 43 - ((thr_value % 50) * 2);
		} else if (thr_value>=60){
			tmp_value = 23;
		}	
		return tmp_value;
	}
	
	
	public boolean onCreateOptionsMenu(Menu  menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.facebook, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem  item) {
		switch (item.getItemId()) {
 
		case R.id.facebook:
			String str = "Temperature: " + ard.getTemp().getActualValue() +"°C - ";
			str = str + "Humidity: " + ard.getHum().getActualValue() + "% - ";
			str = str + "Irradiation: " + ard.getLdr().getThr() +"%";
			System.out.println("STR per FACEBOOK: " + str);
			//C:\Users\Rita\AppData\Local\Android\android-sdk
			Intent sharingIntent = new Intent(Intent.ACTION_SEND);
			sharingIntent.setType("text/plain");
			sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, str);
			sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject");
			startActivity(Intent.createChooser(sharingIntent, "Share using"));
			break;
		}
		return true;
	}
	
	public void onBackPressed() {
	    // This will be called either automatically for you on 2.0
	    // or later, or by the code above on earlier versions of the
	    // platform.
		System.out.println("BACK BUTTON");
		canStop = 1;
		finish();
	    return;
	}
	
	
	//Disegno il termometro
	public class DrawThermometer extends View {
	    Paint paint = new Paint();
	    Paint paint2 = new Paint();
	    Paint paint_oval = new Paint();
	    
	    public DrawThermometer(Context context) {
	        super(context);            
	    }

	    public void onDraw(Canvas canvas) {
			
	    	//sfondo termometro
			Resources res = getResources();
			Bitmap bitmap = BitmapFactory.decodeResource(res, R.drawable.termometro);
			canvas.drawBitmap(bitmap, 0, 0, null);
			
	    	//sfondo grigio
	    	paint.setColor(Color.GRAY);
	        paint.setStrokeWidth(3);
	        //lunghezza = 50 - 40
	        //altezza = 200 - 20
	        canvas.drawRect(50, 25, 57, 205, paint);
	        
	        //termometro vero e proprio
	        paint2.setColor(Color.RED);
	        //ciò che cambierà è il 50 dell'altezza!
	        canvas.drawRect(50, altezza_therm, 57, 205, paint2 ); 
	        
	        //ovale
	        paint_oval.setColor(Color.RED);
	        //paint_oval.setStyle(Paint.Style.STROKE); 
	        //paint_oval.setStrokeWidth(4.5f);
			canvas.drawOval(new RectF(45, 190, 63, 210), paint_oval);
	    }


	}

}
