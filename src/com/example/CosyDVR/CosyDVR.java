package com.example.CosyDVR;
import com.example.CosyDVR.BackgroundVideoRecorder;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
//import android.widget.Toast;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.IBinder;
import java.io.File;


public class CosyDVR extends Activity{

    BackgroundVideoRecorder mService;
    Button zomButton,favButton,recButton,flsButton,exiButton,focButton,nigButton;
    boolean mBound = false;
    boolean recording;
    private int mWidth=1,mHeight=1;
    long ExitPressTime = 0;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      recording = false;

      setContentView(R.layout.main);
      
      File tmpdir = new File(Environment.getExternalStorageDirectory() + "/CosyDVR/temp/");
      File favdir = new File(Environment.getExternalStorageDirectory() + "/CosyDVR/fav/");
      tmpdir.mkdirs();
      favdir.mkdirs();

      zomButton = (Button)findViewById(R.id.zom_button);
      favButton = (Button)findViewById(R.id.fav_button);
      recButton = (Button)findViewById(R.id.rec_button);
      focButton = (Button)findViewById(R.id.foc_button);
      nigButton = (Button)findViewById(R.id.nig_button);
      flsButton = (Button)findViewById(R.id.fls_button);
      exiButton = (Button)findViewById(R.id.exi_button);
      
      zomButton.setOnClickListener(zomButtonOnClickListener);
      favButton.setOnClickListener(favButtonOnClickListener);
      recButton.setOnClickListener(recButtonOnClickListener);
      focButton.setOnClickListener(focButtonOnClickListener);
      nigButton.setOnClickListener(nigButtonOnClickListener);
      flsButton.setOnClickListener(flsButtonOnClickListener);
      exiButton.setOnClickListener(exiButtonOnClickListener);

      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
     
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
      super.onWindowFocusChanged(hasFocus);
      if(hasFocus){
          //aqcuire screen size 
          Display display = getWindowManager().getDefaultDisplay();
//          if (Build.VERSION.SDK_INT >= 13){
	          Point size = new Point();
	          display.getSize(size);
	          mWidth = size.x;
	          mHeight = size.y - favButton.getHeight();
//          }else {
//        	  mWidth = display.getWidth();
//        	  mHeight = display.getHeight();
//          }

          Intent intent = new Intent(CosyDVR.this, BackgroundVideoRecorder.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startService(intent);
          bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
      }
   }

  @Override  
  public void onDestroy(){
	  if(mBound) {
		  unbindService(mConnection);
          mBound = false;
	  }
	  super.onDestroy();
  }
  
  @Override
  public void onPause(){
	  if(mBound) {
		  mService.ChangeSurface(1, 1);
	  }
	  super.onPause();
  }
  
  @Override
  public void onResume(){
	  if(mBound) {
		  mService.ChangeSurface(mWidth, mHeight);
	  }
	  super.onResume();
  }


  Button.OnClickListener zomButtonOnClickListener
  = new Button.OnClickListener(){
  @Override
  public void onClick(View v) {
   // TODO Auto-generated method stub
	  if(mBound) {
		  mService.toggleZoom();
	  }
 }};

 Button.OnClickListener favButtonOnClickListener
  = new Button.OnClickListener(){
  @Override
  public void onClick(View v) {
   // TODO Auto-generated method stub
	  if(mBound) {
		  mService.toggleFavorite();
		  favButton.setText(getString(R.string.fav) + " [" + mService.isFavorite() + "]");
	  }
 }};

  Button.OnClickListener recButtonOnClickListener
  = new Button.OnClickListener(){

@Override
public void onClick(View v) {
 // TODO Auto-generated method stub
/* if(mService.isRecording()){
	 recButton.setText(getString(R.string.rec));
 }else{
     recButton.setText(getString(R.string.stop));
 }
 	 mService.toggleRecording();
*/
	 mService.RestartRecording(); //stop
}};

Button.OnClickListener focButtonOnClickListener
= new Button.OnClickListener(){
@Override
public void onClick(View v) {
// TODO Auto-generated method stub
	  if(mBound) {
		  mService.toggleFocus();
		  focButton.setText(getString(R.string.focus) + " [" + mService.getFocusMode() + "]");
	  }
}};

Button.OnClickListener nigButtonOnClickListener
= new Button.OnClickListener(){
@Override
public void onClick(View v) {
// TODO Auto-generated method stub
	  if(mBound) {
		  mService.toggleNight();
	  }
}};

Button.OnClickListener flsButtonOnClickListener
= new Button.OnClickListener(){
@Override
public void onClick(View v) {
// TODO Auto-generated method stub
	  if(mBound) {
		  mService.toggleFlash();
	  }
}};

Button.OnClickListener exiButtonOnClickListener
= new Button.OnClickListener(){
@Override
public void onClick(View v) {
// TODO Auto-generated method stub
	if(SystemClock.elapsedRealtime() > (ExitPressTime + 1000)
	   && SystemClock.elapsedRealtime() < (ExitPressTime + 2000)) { 
		if(mBound) {
			unbindService(CosyDVR.this.mConnection);
			CosyDVR.this.mBound = false;
		}
		stopService(new Intent(CosyDVR.this, BackgroundVideoRecorder.class));
		CosyDVR.this.finish();
		//System.exit(0);
	} else {
		ExitPressTime = SystemClock.elapsedRealtime();
		//Toast.makeText(CosyDVR.this, R.string.exit_again, Toast.LENGTH_LONG).show();
	}
}};

/** Defines callbacks for service binding, passed to bindService() */
private ServiceConnection mConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName className,
            IBinder service) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        BackgroundVideoRecorder.LocalBinder binder = (BackgroundVideoRecorder.LocalBinder) service;
        mService = binder.getService();
        mBound = true;
        /*if(!mService.isRecording()){
       	 	//stopService(new Intent(CosyDVR.this, BackgroundVideoRecorder.class));
       	 	recButton.setText(getString(R.string.rec));
        }else{
            recButton.setText(getString(R.string.stop));
        }*/
        mService.ChangeSurface(mWidth, mHeight);
     }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        mBound = false;
    }
    
};
}
