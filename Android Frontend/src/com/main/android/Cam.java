package com.main.android;

import java.io.IOException;

import util.EmulatedCamera;



import android.app.Activity;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class Cam extends Activity implements SurfaceHolder.Callback {

	  // oggetti e variabili
	   private SurfaceView mSurfaceView;
	   private SurfaceHolder mSurfaceHolder;
	   private boolean mPreviewRunning;
	   
	   // sull'emulatore usare EmulatedCamera al posto di Camera 
	   private EmulatedCamera mCamera;
	   
	   /** Called when the activity is first created. */
	   @SuppressWarnings("deprecation")
	@Override
	   public void onCreate(Bundle savedInstanceState) {
	      super.onCreate(savedInstanceState);

	      // impostazione per il layout
	      getWindow().setFormat(PixelFormat.TRANSLUCENT);
	      requestWindowFeature(Window.FEATURE_NO_TITLE);
	      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);   //full screen
	      setContentView(R.layout.camera);
	      
	      // (attualmente non usato)
	      // imagebutton
	      ImageButton buttonPicture = (ImageButton) findViewById(R.id.camera_surface_button);
	      buttonPicture.setOnClickListener(new OnClickListener() {
	         public void onClick(View v) {
	            // non ancora implementato nella EmulatedCamera
	            // mCamera.takePicture(null, null, jpegCallback); 
	         }
	      });
	      
	      // SurfaceView e SurfaceHolder
	      mSurfaceView = (SurfaceView)findViewById(R.id.camera_surface);
	      mSurfaceHolder = mSurfaceView.getHolder();
	      mSurfaceHolder.addCallback(this);

	      // sull'emulatore usare SURFACE_TYPE_NORMAL al posto di SURFACE_TYPE_PUSH_BUFFERS
	      mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
	   }
	   
	   // (attualmente non usato)
	   // callback richiamata quando l'immagine è pronta per essere memorizzata
	   PictureCallback jpegCallback = new PictureCallback() {
	      public void onPictureTaken(byte[] _data,Camera _camera) {
	         // salvataggio immagine
	         // riparte la preview
	         mCamera.startPreview();
	      }
	   };

	   // override di SurfaceChange, si reimpostano le dimensioni della view 
	   public void surfaceChanged(SurfaceHolder arg0,int arg1,int arg2,int arg3) {
	      // stop della preview
	      if (mPreviewRunning) mCamera.stopPreview();
	      // setto le preferenze partendo da quelle di default
	      Camera.Parameters p = mCamera.getParameters();
	      p.setPreviewSize(arg2,arg3);
	      mCamera.setParameters(p);
	      // rilancio la preview
	      try {
	         mCamera.setPreviewDisplay(arg0); 
	         mCamera.startPreview();
	         mPreviewRunning = true;
	      } catch (IOException e) {
	         Log.i("EMUCAM","IOException"+e.toString());
	      }
	    
	   }

	   // override di SurfaceCreated, si crea la EmulatedCamera e si imposta la sorgente di emulazione
	   public void surfaceCreated(SurfaceHolder holder) {
	      mCamera = EmulatedCamera.open();
	      // il seguente metodo serve solo per la EmulatedCamera (da togliere per Camera)
	      // (A) questo per ricevere il flusso video http da una webcam
	      mCamera.setSource("http://192.168.42.167:8181");
	      // (B) questo per usare una drawable come sorgente del flusso video
	      // mCamera.setSource(getResources(),R.drawable.fotogramma);
	   }

	  // override di SurfaceDestroyed, si ferma la preview e si rilascia la camera
	   public void surfaceDestroyed(SurfaceHolder holder) {
	      mCamera.stopPreview();
	      mPreviewRunning = false;
	      mCamera.release();
	   }
}
