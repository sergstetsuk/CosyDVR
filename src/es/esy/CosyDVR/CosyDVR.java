package es.esy.CosyDVR;
import es.esy.CosyDVR.BackgroundVideoRecorder;
import es.esy.CosyDVR.CosyDVRPreferenceActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
//import android.os.SystemClock;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
//import android.widget.Toast;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.IBinder;
import java.io.File;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class CosyDVR extends Activity{

    BackgroundVideoRecorder mService;
    Button favButton,recButton,flsButton,exiButton,focButton,nigButton;
    View mainView;
    boolean mBound = false;
    boolean recording = false;
    boolean mayclick = false;
    private int mWidth=1,mHeight=1;
    long ExitPressTime = 0;
    private float mScaleFactor = 4.0f;
    private String[] mFocusNames = {"I",
			 "V",
			 "A",
			 "M",
			 "E",
			 };

    private final class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(4.0f, Math.min(mScaleFactor, 14.0f));
	      	if(mBound) {
	      		mService.setZoom(mScaleFactor);
	    	}
            return true;
        }
    }

    /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      recording = false;
      
      SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(this);
      boolean REVERSE_ORIENTATION = sharedPref.getBoolean("reverse_landscape", false);
      if(REVERSE_ORIENTATION) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
      } else {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      }


      setContentView(R.layout.main);
      
      //~ File tmpdir = new File(Environment.getExternalStorageDirectory() + "/CosyDVR/temp/");
      //~ File favdir = new File(Environment.getExternalStorageDirectory() + "/CosyDVR/fav/");
      //~ tmpdir.mkdirs();
      //~ favdir.mkdirs();

      favButton = (Button)findViewById(R.id.fav_button);
      recButton = (Button)findViewById(R.id.rec_button);
      focButton = (Button)findViewById(R.id.foc_button);
      nigButton = (Button)findViewById(R.id.nig_button);
      flsButton = (Button)findViewById(R.id.fls_button);
      exiButton = (Button)findViewById(R.id.exi_button);
      mainView = (View)findViewById(R.id.mainview);
      
      favButton.setOnClickListener(favButtonOnClickListener);
      recButton.setOnClickListener(recButtonOnClickListener);
      focButton.setOnClickListener(focButtonOnClickListener);
      nigButton.setOnClickListener(nigButtonOnClickListener);
      flsButton.setOnClickListener(flsButtonOnClickListener);
      exiButton.setOnLongClickListener(exiButtonOnLongClickListener);
      recButton.setOnLongClickListener(recButtonOnLongClickListener);
      nigButton.setOnLongClickListener(nigButtonOnLongClickListener);

      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      final ScaleGestureDetector mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());
      mainView.setOnTouchListener(new OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event) {
         	 mScaleDetector.onTouchEvent(event);
         	 //Extra analysis for single tap detection. Swipes are detected as autofocus too for now
         	 int action = event.getAction() & MotionEvent.ACTION_MASK;
         	 switch(action) {
         	 	case MotionEvent.ACTION_DOWN : {
         	 		mayclick = true;	//first finger touch is like click 
         	 		break;
         	 	}
         	 	case MotionEvent.ACTION_POINTER_DOWN : {
         	 		mayclick = false;	//second finger is not click
         	 		break;
         	 	}
         	 	case MotionEvent.ACTION_UP : {
         	 		if(mayclick) {		//first finger up. check if it was single one
         	 			if(mBound) {
         	 				mService.autoFocus();
         	 			}
         	 		}
     	 			mayclick = false;
         	 	}  
         	 }
         	 return true;
          }

      });

     
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

          Intent intent = new Intent(/*CosyDVR.this*/getApplicationContext(), BackgroundVideoRecorder.class);
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
	  this.unregisterReceiver(receiver);
  }

  @Override
  public void onResume(){
	  if(mBound) {
		  mService.ChangeSurface(mWidth, mHeight);
	  }
	  super.onResume();
	  this.registerReceiver(receiver,filter);
  }

  public void updateinterface(){
	  if(mBound) {
		favButton.setText(getString(R.string.fav) + " [" + mService.isFavorite() + "]");
		if(mService.isRecording()) {
			recButton.setText(getString(R.string.restart));
		} else {
			recButton.setText(getString(R.string.start));
		}

	  }
	}
  
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
		  focButton.setText(getString(R.string.focus) + " [" + mFocusNames[mService.getFocusMode()] + "]");
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

Button.OnLongClickListener recButtonOnLongClickListener
= new Button.OnLongClickListener(){
@Override
public boolean onLongClick(View v) {
// TODO Auto-generated method stub
	  mService.ChangeSurface(1, 1);
      Intent myIntent = new Intent(getApplicationContext(), CosyDVRPreferenceActivity.class);
      startActivity(myIntent);
	  //mService.ChangeSurface(mWidth, mHeight);	//size will be returned with app focus
	return true;
}};

Button.OnLongClickListener nigButtonOnLongClickListener
= new Button.OnLongClickListener(){
@Override
public boolean onLongClick(View v) {
	  if(mBound) {
		  mService.toggleTimeLapse();
	  }
	  return true;
}};

Button.OnLongClickListener exiButtonOnLongClickListener
= new Button.OnLongClickListener(){
@Override
public boolean onLongClick(View v) {
// TODO Auto-generated method stub
/*	if(SystemClock.elapsedRealtime() > (ExitPressTime + 1000)
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
	}*/
	if(mBound) {
		unbindService(CosyDVR.this.mConnection);
		CosyDVR.this.mBound = false;
	}
	stopService(new Intent(CosyDVR.this, BackgroundVideoRecorder.class));
	CosyDVR.this.finish();
	return true;
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

private IntentFilter filter = new IntentFilter("es.esy.CosyDVR.updateinterface");

private BroadcastReceiver receiver = new BroadcastReceiver(){
    
    @Override
    public void onReceive(Context c, Intent i) {
    	updateinterface();
    }
};
}
