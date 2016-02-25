package util;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;


public class EmulatedCamera {

   // costanti
   private static final int CONN_TIMEOUT = 1000;
   private static final int READ_TIMEOUT = 1000;

   // classi
   static private EmulatedCamera emCamera;
   private CameraCapture capture;
   private Camera parametersCamera;
   private SurfaceHolder surfaceHolder;

   // sorgente video
   private String sUrl = "";
   private Resources rRes = null;
   private int iRes = 0;
   
   
   // variabili 
   private final boolean preserveAspectRatio = true;
   private final Paint paint = new Paint();
   private int width = 240;
   private int height = 320;
   private Rect bounds = new Rect(0, 0, width, height);
 
   // costruttore
   private EmulatedCamera() {
      // serve solamente a popolare correttamente parametersCamera
      parametersCamera = Camera.open();
      System.out.println("parametersCamera: " +parametersCamera);
   }
 
   // imposto la sorgente video su un URL o una immagine delle risorse
   public void setSource(String sUrl) {
      this.sUrl = sUrl;
      this.iRes = 0;
   }

   public void setSource(Resources rRes,int iRes) {
      this.sUrl = "";
      this.rRes = rRes;
      this.iRes = iRes;
   }   
   
   // open : se non esiste, creo l'istanza
   static public EmulatedCamera open() {
      if (emCamera == null) emCamera = new EmulatedCamera();
      Log.i("EMULATEDCAM", "Creating Emulated Camera");
      return emCamera;
   }

   // startPreview : creo il thread che genera il flusso video
   public void startPreview() {
      capture = new CameraCapture();
      capture.setCapturing(true);
      capture.start(); 
      Log.i("EMULATEDCAM", "Starting Emulated Camera");
   }
 
   // stopPreview : stop al thread che genera il flusso video
   public void stopPreview() {
      capture.setCapturing(false);
      Log.i("EMULATEDCAM", "Stopping Emulated Camera");
   }
 
   // setPreviewDisplay : imposto il surfaceHolder
   public void setPreviewDisplay(SurfaceHolder surfaceHolder) throws IOException {
      this.surfaceHolder = surfaceHolder;
   }
 
   // setParameters : dei parametri da settare quello importante è la size della preview
   public void setParameters(Camera.Parameters parameters) {
      Log.i("EMULATEDCAM", "Setting Emulated Camera parameters");
      parametersCamera.setParameters(parameters);
      System.out.println("PARAMETERS: " + parameters);
      Size size = parameters.getPreviewSize();
      System.out.println("Size - Parameters: " + parameters.getPreviewSize());
      bounds = new Rect(0,0,size.width,size.height);
      System.out.println("bounds - Parameters: " + bounds);
   }
   
   // getParameters : rilettura dei parametri
   public Camera.Parameters getParameters() { 
      Log.i("EMULATEDCAM", "Getting Emulated Camera parameters");
      return parametersCamera.getParameters(); 
   } 
 
   
   public void release() {
      Log.i("EMULATEDCAM", "Releasing Emulated Camera parameters");
      // il release non ha molto senso, perchè al contrario della vera camera non è una risorsa unica
   } 
 
   // THREAD PER GENERARE IL FLUSSO VIDEO
   // interno alla classe
   private class CameraCapture extends Thread  {
  
      // attivazione-disattivazione del thread con relativa variabile
      private boolean capturing = false;
      public void setCapturing(boolean capturing) {
         this.capturing = capturing;
      }

      // loop del thread
      @Override
      public void run() {
         while (capturing) {
            Canvas c = null;
            try {
               c = surfaceHolder.lockCanvas(null);
               synchronized (surfaceHolder) {

                  try {

                     // vars
                     Bitmap bitmap = null;
                     InputStream in = null;
                     int response = -1;

                     // se è una bitmap nelle risorse, uso quella
                     if (iRes!=0) {
                        bitmap = BitmapFactory.decodeResource(rRes,iRes);   
                     }
                     // altrimenti se è un URL inizializzo la connessione http
                     else if (sUrl.length()>10) {                       
                        URL url = new URL(sUrl);
                        URLConnection conn = url.openConnection();
                        if (!(conn instanceof HttpURLConnection)) throw new IOException("Not an HTTP connection.");
                        HttpURLConnection httpConn = (HttpURLConnection) conn;
                        httpConn.setAllowUserInteraction(false);
                        httpConn.setConnectTimeout(CONN_TIMEOUT);
                        httpConn.setReadTimeout(READ_TIMEOUT);
                        httpConn.setInstanceFollowRedirects(true);
                        httpConn.setRequestMethod("GET");
                        httpConn.connect();
                        // gestione risposta
                        response = httpConn.getResponseCode();
                        if (response == HttpURLConnection.HTTP_OK) {
                           in = httpConn.getInputStream();
                           bitmap = BitmapFactory.decodeStream(in);
                        }
                     }
                
                     // se le dimensioni non coincidono, scalo il flusso per la nostra preview
                     if (bounds.right==bitmap.getWidth() && bounds.bottom == bitmap.getHeight()) {
                        c.drawBitmap(bitmap,0,0,null);
                     } else {
                        Rect dest;
                        if (preserveAspectRatio) {
                           dest = new Rect(bounds);
                           dest.bottom = bitmap.getHeight() * bounds.right / bitmap.getWidth();
                           dest.offset(0, (bounds.bottom - dest.bottom)/2);
                        } else {
                           dest = bounds;
                        }
                        if (c != null) c.drawBitmap(bitmap,null,dest,paint);
                     }

                  } catch (RuntimeException e) {
                     e.printStackTrace();
                  } catch (IOException e) {
                     e.printStackTrace();
                  }  
               }
            } catch (Exception e) {
               e.printStackTrace();
            } finally {
               // se viene generata una eccezione, si rilascia il surface
               if (c != null) surfaceHolder.unlockCanvasAndPost(c);
            }
         }
         Log.i("EMULATEDCAM", "Emulated Camera Thread stopped");
      }
   }

}
