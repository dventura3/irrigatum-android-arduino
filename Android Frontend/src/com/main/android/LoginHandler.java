package com.main.android;

import org.json.JSONException;

import util.Session;
import util.Utils;

import com.data.db.MyHelperDB;
import com.resource.arduino.Arduino;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "ParserError", "ParserError" })
public class LoginHandler extends Activity {

	MyHelperDB myDB;
	SQLiteDatabase db;
	
	Arduino ard;
	
	EditText login;
	EditText pwd;
	CheckBox remember;
	Button btn_login;
	Button btn_reg;
	
	int toGo;
	private static final Object lock= new Object();

	@SuppressLint("ParserError")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_h);
		
		myDB = new MyHelperDB(this);
		db = myDB.getWritableDatabase();
		
		ard = new Arduino(Utils.myActualDevice.getIP(), Utils.myActualDevice.getName());
		
		toGo = 0;
		
		login = (EditText) findViewById(R.id.user);
		pwd = (EditText) findViewById(R.id.pwd);
		//remember = (CheckBox)findViewById(R.id.ckRemember);
		
		btn_login = (Button) findViewById(R.id.btn_login);
		btn_login.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				startArduinoList();
			}
		});
		
	}
	
	 @SuppressLint({ "ParserError", "ParserError" })
	private void startArduinoList() {
		
		System.out.println("USER: " + login.getText().toString() + " PWD:" + pwd.getText().toString()) ;
		if(!checkFields(login, pwd))
			Toast.makeText(LoginHandler.this,"Please fill all fields!", Toast.LENGTH_SHORT).show();
		else {
			
			/*
			//FIXME:  DA ELIMINARE
			myDB.deleteUser(db);
			long currentTime = System.currentTimeMillis();
			myDB.setUser(db, login.getText().toString(), pwd.getText().toString(), "pippo", currentTime);
			*/
			
			
			//FIXME: DA CONSIDERARE
			//verifico se ci sono elementi nella user table (se ci sono, vuol dire ce non ho fatto "logout")
			//se ci sono -> chiudo la sessione e ne creo una nuova... + ... altre operazioni...
						//altre operazioni: 
						// - cancello elemento in user table
						// - cancello elemento in first table
						// - cancello session locale
			//dopo tutto ciò.... posso inserire il nuovo utente dentro il db (che mi aspetto essere vuoto)
			int user_id = myDB.thereIsUser(db);
			if(user_id!=-1){
				//c'è l'utente sul DB
				
				System.out.println("C'e un utente nel DB");
				
				Cursor c = myDB.getUserInBaseOfID(db,user_id);
				while (c.moveToNext()) {
					String user = c.getString(0);
				    String pwd = c.getString(1);
				    String sessionToken = c.getString(2);
					long time = c.getLong(3);
					//provo a chiudere la sessione
					sendCloseSession(sessionToken);
				}
				
			}else{
				
				String nomeUtente = login.getText().toString();
				String pwdUtente = pwd.getText().toString();
				if(nomeUtente.length()>8 || pwdUtente.length()>8){
					Toast.makeText(LoginHandler.this,"Error! Max number of user or password characters are 8", Toast.LENGTH_SHORT).show();
				}else{
					//mando la richiesta
					sendLoginFirstReq();
				}
			}
			
			

			/*
			//FIXME:  Da eliminare
			long currentTime = System.currentTimeMillis();
			Utils.mySessionObject = new Session();
			Utils.mySessionObject.setSessionToken("pippo");
			Utils.mySessionObject.setStartTime(currentTime);
			Intent intent = new Intent(this, MainMenu.class);
		    startActivity(intent);
		    finish();
		    */
			
			
			
			
			/*
			int id_user = myDB.userIsExist(db, login.getText().toString(), pwd.getText().toString());
			if(id_user==-1)
				Toast.makeText(LoginHandler.this,"Authentication Error, please check fields or register yourself!", Toast.LENGTH_SHORT).show();
			else{
				
				//set session
				Utils.mySessionObject = new Session();
				Utils.mySessionObject.setIDuser(id_user);
				//if remember is checked -> insert id_user on DB
				//else remove elements on FristAccess
				if(remember.isChecked())
					myDB.setFirstAccess(db,String.valueOf(id_user));
				else
					myDB.freeFirstAccess(db);
				//TODO: PROVA:
				//System.out.println("FIRST ACCESS: " + myDB.getFirstAccessID(db));
				//start successiva activity
			    Intent intent = new Intent(this, ArduinoList.class);
			    startActivity(intent);
			    finish();
			}
			*/
		}

	 }

	 //Check sui campi inseriti dall'utente..
	 public boolean checkFields(EditText usn, EditText psw){
		if(usn.getText().toString().equals("") || psw.getText().toString().equals("")) 
			return false;
		return true;    	
	 }
	 
	 public void sendCloseSession(String sessionToken){
		 CloseSession cd = new CloseSession(this,sessionToken);
		 cd.execute();
			if(cd.getStatus()==AsyncTask.Status.FINISHED){
				System.out.println("HA FINITO! ");
				return;
			}
			System.out.println("e' in running ");
	 }
	 
	 
	 private class CloseSession extends AsyncTask<Void, Void, Boolean> {
		    private ProgressDialog dialog = null;
			private Context context = null;
			private String sessionToken = null;

			@SuppressLint("ParserError")
			public CloseSession(Context context,String sexToken) {
				this.context = context;
				this.sessionToken = sexToken;
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				//dialog = ProgressDialog.show(context,"","Loading...");
			}

			@Override
			protected Boolean doInBackground(Void... voids) {
				try {
					ard.setSessionToken(sessionToken);
					System.out.println("deleteThread");
					return ard.endLogin();
				} catch (JSONException e) { System.out.println(">>> Eccezione!" ); }
				return false;
			}

			@Override
			protected void onPostExecute(Boolean reply) {
				
				//cancel dialog
				//dialog.cancel();
				
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
					    	  System.out.println("OK");
						  }
					});
					AlertDialog alert = builder.create();
					alert.show();
			
					
				}else{

					myDB.deleteUser(db);
					myDB.freeFirstAccess(db);
					Utils.mySessionObject = null;
					
					String nomeUtente = login.getText().toString();
					String pwdUtente = pwd.getText().toString();
					if(nomeUtente.length()>8 || pwdUtente.length()>8){
						Toast.makeText(LoginHandler.this,"Error! Max number of user or password characters are 8", Toast.LENGTH_SHORT).show();
					}else{
						//mando la richiesta
						sendLoginFirstReq();
					}
					
				}
				

			}
		}
		
	 
	 
	 public void sendLoginFirstReq(){
			new FirstTask(this).execute();
	 }
	 
	 private class FirstTask extends AsyncTask<Void, Void, Boolean> {
			private ProgressDialog dialog = null;
			private Context context = null;

			public FirstTask(Context context) {
				this.context = context;
				//dialog = ProgressDialog.show(context,"","Loading...");
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			protected Boolean doInBackground(Void... voids) {
				try {
						String nomeUtente = login.getText().toString();
						System.out.println("NOME INSERITO: " + nomeUtente);
						return ard.loginHandler(nomeUtente);
				} catch (JSONException e) { 
					e.printStackTrace();
					System.out.println(">>> Eccezione!" ); 
					}
				return false;
			}

			@Override
			protected void onPostExecute(Boolean reply) {
				
				//cancel dialog
				//dialog.cancel();
				
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				
				//set different message in base to result.
				if (reply==false) {
					builder.setTitle("Error");
					builder.setMessage("Check Connection Status with Arduino!");
				}else{
					//verifico se il risultato datomi da arduino e' corretto o no
					if(!ard.getResultReq().equals("OK")){
						builder.setTitle("Error");
						builder.setMessage("Authentication Error, please check fields or register yourself!");
					}
						
				}
				
				if(reply==false || !ard.getResultReq().equals("OK")){
					
					//in caso di errore mostro un messaggio di errore
					builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					      public void onClick(DialogInterface dialog, int id) {
						         System.out.println("CLICK OK");
						  }
					});
					AlertDialog alert = builder.create();
					alert.show();
					
				}else{					
					//aspetto 1 secondo
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//String randomToken = ard.getRandomToken();
					sendLoginSecondReq();
				}
				
			}
		}
	 
	 
	 public void sendLoginSecondReq(){
			new SecondTask(this).execute();
	 }
	 
	 
	 private class SecondTask extends AsyncTask<Void, Void, Boolean> {
			private Context context = null;
			private ProgressDialog dialog = null;

			public SecondTask(Context context) {
				this.context = context;
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				//dialog = ProgressDialog.show(context,"","Loading...");
			}

			@Override
			protected Boolean doInBackground(Void... voids) {
				try {
						//TODO: gestione della cifratura sfruttando la password dell'utente
						return ard.randomTokenHandler(ard.getRandomToken());
				} catch (JSONException e) { 
					e.printStackTrace();
					System.out.println(">>> Eccezione!" ); 
					}
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
					//verifico se il risultato datomi da arduino e' corretto o no
					if(!ard.getResultReq().equals("OK")){
						builder.setTitle("Error");
						builder.setMessage("Authentication Error, please check fields or register yourself!");
					}
					
						
				}
				
				
				if(ard.getResultReq().equals("OK")){

					long currentTime = System.currentTimeMillis();
					
					System.out.println("SESSION TOKEN: " + ard.getSessionToken() );
					
					//salvo i dati dell'utente sul database
					myDB.setUser(db, login.getText().toString(), pwd.getText().toString(), ard.getSessionToken(), currentTime);
					
					//aggiorno la Sessione
					int id_user = myDB.userIsExist(db, login.getText().toString(), pwd.getText().toString());
					Utils.mySessionObject = new Session();
					Utils.mySessionObject.setIDuser(id_user);
					Utils.mySessionObject.setRandomToken(ard.getRandomToken());
					Utils.mySessionObject.setSessionToken(ard.getSessionToken());
					Utils.mySessionObject.setStartTime(currentTime);
					
					//passo all'activity successiva
					Intent intent = new Intent(context, MainMenu.class);
				    startActivity(intent);
				    //finish();
				}else{
					//in caso di errore mostro un messaggio di errore
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
	 
	 

	 @Override
     protected void onDestroy(){
    	super.onDestroy();
    	myDB.close();
    }	
	
}
