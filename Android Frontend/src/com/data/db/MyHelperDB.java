package com.data.db;

import java.text.MessageFormat;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


@SuppressLint("ParserError")
public class MyHelperDB extends SQLiteOpenHelper {

	private static final String DB_NAME = "irrigatumDB.db";
	private static final int DB_VERSION = 1;
	
	public MyHelperDB(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		//Create Configuration Table
		String table_01 = "";
		table_01 += "CREATE TABLE {0} (";
		table_01 += " {1} INTEGER PRIMARY KEY,";
		table_01 += " {2} TEXT NOT NULL,";
		table_01 += " {3} TEXT NOT NULL,";
		table_01 += " {4} TEXT NOT NULL,";
		table_01 += " {5} TEXT NOT NULL,";
		table_01 += " {6} TEXT NOT NULL,";
		table_01 += " {7} TEXT NOT NULL,";
		table_01 += " {8} TEXT NOT NULL";
		table_01 += ")";
		db.execSQL(MessageFormat.format(table_01, ConfigurationTable.TABLE_NAME, ConfigurationTable._ID, ConfigurationTable.NAME, ConfigurationTable.HUMIDITY_MIN, ConfigurationTable.HUMIDITY_MAX, ConfigurationTable.TEMPERATURE_MIN, ConfigurationTable.TEMPERATURE_MAX, ConfigurationTable.THR_IRRID, ConfigurationTable.TIME_IRRID));
		Log.i("DB", "Config table Created!");
		
		//Create User Table
		String table_02 = "";
		table_02 += "CREATE TABLE {0} (";
		table_02 += " {1} INTEGER PRIMARY KEY,";
		table_02 += " {2} TEXT NOT NULL,";
		table_02 += " {3} TEXT NOT NULL,";
		table_02 += " {4} TEXT NOT NULL,";
		table_02 += " {5} LONG NOT NULL";
		table_02 += ")";
		db.execSQL(MessageFormat.format(table_02, UserTable.TABLE_NAME, UserTable._ID, UserTable.user, UserTable.pwd, UserTable.sessionToken, UserTable.timestamp));
		Log.i("DB", "User table Created!");
		
		//Create First Access Table
		String table_03 = "";
		table_03 += "CREATE TABLE {0} (";
		table_03 += " {1} INTEGER PRIMARY KEY,";
		table_03 += " {2} TEXT NOT NULL,";
		table_03 += " {3} TEXT NOT NULL";
		table_03 += ")";
		db.execSQL(MessageFormat.format(table_03, FirstAccess.TABLE_NAME, FirstAccess._ID, FirstAccess.isFirst, FirstAccess.id_user));
		Log.i("DB", "First Access table Created!");
		
		//Create Arduino Table
		String table_04 = "";
		table_04 += "CREATE TABLE {0} (";
		table_04 += " {1} INTEGER PRIMARY KEY,";
		table_04 += " {2} TEXT NOT NULL,";
		table_04 += " {3} TEXT NOT NULL";
		table_04 += ")";
		db.execSQL(MessageFormat.format(table_04, ArduinoTable.TABLE_NAME, ArduinoTable._ID, ArduinoTable.dev_name, ArduinoTable.dev_ip));
		Log.i("DB", "Arduino table Created!");
	}

	public void insertConfig(SQLiteDatabase db, String name, String h_min, String h_max, String t_min, String t_max, String thr_irrid, String time_irrid){
		ContentValues v = new ContentValues();
		v.put(ConfigurationTable.NAME, name);
		v.put(ConfigurationTable.HUMIDITY_MIN, h_min);
		v.put(ConfigurationTable.HUMIDITY_MAX, h_max);
		v.put(ConfigurationTable.TEMPERATURE_MIN, t_min);
		v.put(ConfigurationTable.TEMPERATURE_MAX, t_max);
		v.put(ConfigurationTable.THR_IRRID, thr_irrid);
		v.put(ConfigurationTable.TIME_IRRID, time_irrid);
		db.insert(ConfigurationTable.TABLE_NAME, null, v);
	}
	
	public Cursor getAllConfigInBaseOfName(SQLiteDatabase db, String name){
		String[] columns = { "_ID", "name", "h_min", "h_max", "t_min", "t_max", "thr_irrid", "time_irrid" };
		String whereClause = "name = ?";
		String[] whereClauseArgs = {""+name+""};
		return (db.query(ConfigurationTable.TABLE_NAME, columns,whereClause, whereClauseArgs, null, null, null));
	}
	
	public Cursor getPlantsName(SQLiteDatabase db){
		return db.rawQuery( "SELECT DISTINCT name FROM configPiante", null);
	}
	
	public void modifyPlantConfiguration(SQLiteDatabase db, int config_id, String h_min , String h_max, String t_min, String t_max, String thr_irrid, String time_irrid){
		String table = ConfigurationTable.TABLE_NAME;
		String whereClause = "_ID"+"=?";
		String[]whereArgs = new String[] {String.valueOf(config_id)};
		ContentValues args = new ContentValues();
		args.put("h_min", h_min);
		args.put("h_max", h_max);
		args.put("t_min", t_min);
		args.put("t_max", t_max);
		args.put("thr_irrid", thr_irrid);
		args.put("time_irrid", time_irrid);
		db.update(table, args, whereClause, whereArgs);
	}
	
	public void removeSingleConfiguration(SQLiteDatabase db, int config_id){
		String table = ConfigurationTable.TABLE_NAME;
		String whereClause = "_ID"+"=?";
		String[]whereArgs = new String[] {String.valueOf(config_id)};
		db.delete(table, whereClause , whereArgs);
	}

	public boolean thereIsConfiguration(SQLiteDatabase db){
		String[] columns = { "_ID" };
		Cursor c = db.query(ConfigurationTable.TABLE_NAME, columns, null, null, null, null, null);
		int num_elem = c.getCount();
		if (num_elem > 0)
			return true;
		else
			return false;
	}
	
	public void setFirstAccess(SQLiteDatabase db, String id_user){
		ContentValues v = new ContentValues();
		v.put(FirstAccess.isFirst, "y");
		v.put(FirstAccess.id_user, id_user);
		db.insert(FirstAccess.TABLE_NAME, null, v);
	}
	
	public void freeFirstAccess(SQLiteDatabase db){
		//Cursor c = db.rawQuery( "DELETE FROM FirstAccess", null);
		int delete_elem = db.delete(FirstAccess.TABLE_NAME, null, null);
		System.out.println("Num elementi eliminati: "+ delete_elem);
		/*
		//ricreo la tabella:
		ContentValues val = new ContentValues();
		val.put(FirstAccess.isFirst, "");
		val.put(FirstAccess.id_user, "");			
		db.insert(FirstAccess.TABLE_NAME, null, val);
		*/
	}
	
	public int getFirstAccessID(SQLiteDatabase db){
		String[] columns = { "id_user" };
		Cursor c = db.query(FirstAccess.TABLE_NAME, columns, null, null, null, null, null);
		int num_elem = c.getCount();
		System.out.println("Num elementi FIRST ACCESS trovati: " + num_elem);
		while (c.moveToNext()) {
			String id_user = c.getString(0);
			if(!id_user.equals(""))
				return Integer.parseInt(id_user);
		}
		
		return -1;
	}
	
	public void setUser(SQLiteDatabase db, String user_name, String pwd, String sessionToken, long time){
		ContentValues v = new ContentValues();
		v.put(UserTable.user, user_name);
		v.put(UserTable.pwd, pwd);
		v.put(UserTable.sessionToken, sessionToken);
		v.put(UserTable.timestamp, time);
		db.insert(UserTable.TABLE_NAME, null, v);
		System.out.println("NUOVO UTENTE INSERITO NEL DATABASE");
	}

	public int userIsExist(SQLiteDatabase db, String user_name, String pwd){
		Cursor c = db.rawQuery( "SELECT _ID FROM USERS WHERE user = '" +user_name + "' and pwd = '"+pwd+"'", null);
		int num_elem = c.getCount();
		System.out.println("Num elementi USER trovati: " + num_elem);
		if(num_elem==1){
			while (c.moveToNext()) {
				int id_user = c.getInt(0);
				return id_user;
			}
		}
		return -1;
	}
	
	//mi faccio restituire l'ID dell'utente (se c'è)
	public int thereIsUser(SQLiteDatabase db){
		Cursor c = db.rawQuery( "SELECT _ID FROM USERS", null);
		int num_elem = c.getCount();
		if(num_elem == 1){
			while (c.moveToNext()) {
				int id_user = c.getInt(0);
				return id_user;
			}
		}
		return -1;
	}
	
	public void deleteUser(SQLiteDatabase db){
		int delete_elem = db.delete(UserTable.TABLE_NAME, null, null);
		System.out.println("Num elementi eliminati: "+ delete_elem);
	}
	
	public Cursor getUserInBaseOfID(SQLiteDatabase db, int user_id){
		String[] columns = { "user", "pwd", "sessionToken", "timestamp" };
		String whereClause = "_ID"+"=?";
		String[]whereArgs = new String[] {String.valueOf(user_id)};
		return db.query(UserTable.TABLE_NAME, columns, whereClause, whereArgs, null, null, null);
	}
	
	public long getTimestampUserInBaseOfID(SQLiteDatabase db, int user_id){
		String[] columns = { "timestamp" };
		String whereClause = "_ID"+"=?";
		String[]whereArgs = new String[] {String.valueOf(user_id)};
		Cursor c = db.query(UserTable.TABLE_NAME, columns, whereClause, whereArgs, null, null, null);
		int num_elem = c.getCount();
		if(num_elem == 1){
			while (c.moveToNext()) {
				long time = c.getInt(0);
				return time;
			}
		}
		return -1;
	}
	
	public void insertNewDevice(SQLiteDatabase db, String name, String ip){
		ContentValues v = new ContentValues();
		v.put(ArduinoTable.dev_name, name);
		v.put(ArduinoTable.dev_ip, ip);
		db.insert(ArduinoTable.TABLE_NAME, null, v);
	}
	
	public Cursor getAllDevice(SQLiteDatabase db){
		String[] columns = { "_ID", "dev_name", "dev_ip" };
		return (db.query(ArduinoTable.TABLE_NAME, columns, null, null, null, null, null));
	}
	
	public void removeDevice(SQLiteDatabase db, int dev_id){
		//System.out.println("DELETE FROM ardTab WHERE _ID = " + dev_id);
		//db.rawQuery( "DELETE FROM ardTab WHERE _ID = '" + dev_id + "'", null);
		
		String table = ArduinoTable.TABLE_NAME;
		String whereClause = "_ID"+"=?";
		String[]whereArgs = new String[] {String.valueOf(dev_id)};
		db.delete(table, whereClause , whereArgs);
	}
	
	public int getNumberOfDeviceSaved(SQLiteDatabase db){
		String[] columns = { "_ID" };
		Cursor c = db.query(ArduinoTable.TABLE_NAME, columns, null, null, null, null, null);
		return c.getCount();
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
	}
}
