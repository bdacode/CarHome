/*
 *   Copyright 2011-2012 Shell M. Shrader
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.shellware.CarHome;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.UiModeManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.location.Location;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.google.android.mms.APNHelper;
import com.google.android.mms.APNHelper.APN;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.transaction.HttpUtils;
import com.shellware.CarHome.MyLocation.LocationResult;

public class CarHomeActivity extends Activity implements OnClickListener {

	private final static String TAG = "CarHomeActivity";
	
    private static final String FEATURE_ENABLE_MMS = "enableMMS";
    
    private static final int APN_ALREADY_ACTIVE     = 0;
    private static final int APN_REQUEST_STARTED    = 1;
    private static final int APN_TYPE_NOT_AVAILABLE = 2;
    private static final int APN_REQUEST_FAILED     = 3;
	
	private static Context ctx = null;
	private SharedPreferences prefs;
	private Resources res;
    private Handler handler;

	private AudioManager mAudioManager;
	private static Camera camera = null;
	private static ConnectivityActionReceiver connectivityActionReceiver;
	
	RemoteControlReceiver mMediaButtonReceiver;
	IntentFilter mediaFilter;


	private static SurfaceView mCameraView;
	private static SurfaceHolder mCameraHolder = null;
	
    private Button mPlayButton;
    private Button mStopButton;
    private Button mForwardButton;
    private Button mRewindButton;
    private Button mMuteButton;
    
    private SeekBar mPositionBar;
    private ImageView mArtworkImage;
    private ImageView mBackground;
    private Gallery gallery;

    private boolean mMuted = false;
    private static String originator = "";
    private static boolean wifiEnabled = false;
    
    private long lastTime = System.currentTimeMillis();
    
	private Menu myMenu = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		 StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//		    StrictMode.setThreadPolicy(policy);
		    
		ctx = this;
		
		setContentView(R.layout.main);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		res = getResources();
		
		handler = new Handler();

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        
    	mMediaButtonReceiver = new RemoteControlReceiver();
    	mediaFilter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
    	mediaFilter.setPriority(Integer.MAX_VALUE);

    	registerReceiver(mMediaButtonReceiver, mediaFilter);

    	mBackground = (ImageView) findViewById(R.id.background);
    	mBackground.setOnTouchListener(new OnTouchListener() {
    		
    		public boolean onTouch(View view, MotionEvent motionevent) {

    			if (motionevent.getAction() == MotionEvent.ACTION_DOWN) {
    				if (gallery.getVisibility() != View.VISIBLE) { 
    			        gallery.setSelection(2, false);
	    				gallery.setVisibility(View.VISIBLE);

	    				gallery.setOnItemClickListener(clicker);

	    				gallery.clearAnimation();
//	    				gallery.setAlpha(1f);
	
	    				AlphaAnimation alpha = new AlphaAnimation(0f,1f);
	    				alpha.setFillAfter(true);
	    				alpha.setDuration(1000);
	
	    				gallery.setAnimation(alpha);
	    				lastTime = System.currentTimeMillis();
    				} else {
    					gallery.setOnItemClickListener(null);
    					
    	                gallery.clearAnimation();
    	                gallery.setAlpha(1f);

    					AlphaAnimation alpha = new AlphaAnimation(1.0f, 0.0f);
    					alpha.setDuration(1000);
    					alpha.setFillAfter(true);

    					alpha.setAnimationListener(new AnimationListener() {
    						public void onAnimationEnd(Animation animation) {
    							gallery.setVisibility(View.INVISIBLE);
    							Log.d(TAG, "gone");
    						}
    						public void onAnimationRepeat(Animation animation) {
    						}

    						public void onAnimationStart(Animation animation) {
    						}
    					});
    					
    					gallery.setAnimation(alpha);
    					lastTime = System.currentTimeMillis();
    				}
    			}

    			return true;
    		}
    	});

        mPlayButton = (Button) findViewById(R.id.playbutton);
        mPlayButton.setOnClickListener(this);
        
        mStopButton = (Button) findViewById(R.id.stopbutton);
        mStopButton.setOnClickListener(this);
        
        mForwardButton = (Button) findViewById(R.id.forwardbutton);
        mForwardButton.setOnClickListener(this);
        
        mRewindButton = (Button) findViewById(R.id.rewindbutton);
        mRewindButton.setOnClickListener(this);
        
        mMuteButton = (Button) findViewById(R.id.mutebutton);
        mMuteButton.setOnClickListener(this);
        
        mPositionBar = (SeekBar) findViewById(R.id.position);
        mPositionBar.setMax(1);
        mPositionBar.setProgress(0);
        mPositionBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar arg0, int position, boolean fromTouch) {
				if (fromTouch) MusicService.setPosition(position);	
			}
			public void onStartTrackingTouch(SeekBar arg0) {	
			}
			public void onStopTrackingTouch(SeekBar arg0) {		
			}   	
        });
        
        mArtworkImage = (ImageView) findViewById(R.id.artwork);
        
        mCameraView = (SurfaceView) findViewById(R.id.cameraView);
        
        this.registerReceiver(this.myBatteryReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        
        
        gallery = (Gallery) findViewById(R.id.gallery);
        gallery.setAdapter(new ImageAdapter(this));

        gallery.setSelection(2, false);
		        
//        ChangeLog cl = new ChangeLog(this);
//        if (cl.firstRun()) {
//            cl.getLogDialog().show();
//        }
	}
	
	private OnItemClickListener clicker = new OnItemClickListener() {
	
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		
        	if (gallery.getVisibility() != View.VISIBLE) return;
        	
            //Toast.makeText(ctx, "" + position, Toast.LENGTH_SHORT).show();

            gallery.clearAnimation();
//            gallery.setAlpha(1f);

			AlphaAnimation alpha = new AlphaAnimation(1.0f, 0.0f);
			alpha.setDuration(0);
			alpha.setFillAfter(true);

			alpha.setAnimationListener(new AnimationListener() {
				public void onAnimationEnd(Animation animation) {
					gallery.setVisibility(View.INVISIBLE);
					gallery.setOnClickListener(null);
					Log.d(TAG,"gone");
				}
				public void onAnimationRepeat(Animation animation) {
				}

				public void onAnimationStart(Animation animation) {
				}
			});		
        	
			lastTime = 0;
			
        	switch (position) {
        		case 0:                    
            		if (camera == null) {
            			startCameraPreview(Camera.CameraInfo.CAMERA_FACING_FRONT, false);
            		} else {
            			stopCameraPreview();
            		}
            		break;
        		case 1:
        		    Intent email = getPackageManager().getLaunchIntentForPackage("com.android.email");
        		    startActivity(email);
        		    break;
        		case 2:
        			String url = "google.navigation:fd=true";
        			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));            
        			startActivity(i);
        			break;
        		case 3:
        			Intent intent = new Intent(Intent.ACTION_MAIN);
        			intent.addCategory(Intent.CATEGORY_LAUNCHER);
        			intent.setClassName("com.android.mms", "com.android.mms.ui.ConversationList");
        			startActivity(intent);
        			break;
    			default:
        			break;	
        	}
			
		} 	
	};
	
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
            return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
            return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(oneSecondHandler);
    }
    
    private Runnable oneSecondHandler = new Runnable() {

		public void run() {

			if (MusicService.getPosition() != 0) {
				mPositionBar.setMax(MusicService.getCurrentItem().getDuration());
				mPositionBar.setProgress(MusicService.getPosition());
			} else {
				mPositionBar.setMax(1);
				mPositionBar.setProgress(0);
			}
			
			if (MusicService.mState == MusicService.State.Playing) {
				mPlayButton.setBackgroundResource(R.drawable.pauseicon);
				mArtworkImage.setImageBitmap(MusicService.getCurrentItem().getAlbumArt());
			} else {
				mPlayButton.setBackgroundResource(R.drawable.playicon);
			}
			
			if (System.currentTimeMillis() - lastTime > 10000 && gallery.getVisibility() == View.VISIBLE) {
				gallery.setOnItemClickListener(null);
				
                gallery.clearAnimation();
                gallery.setAlpha(1f);

				AlphaAnimation alpha = new AlphaAnimation(1.0f, 0.0f);
				alpha.setDuration(1000);
				alpha.setFillAfter(true);

				alpha.setAnimationListener(new AnimationListener() {
					public void onAnimationEnd(Animation animation) {
						gallery.setVisibility(View.INVISIBLE);
						Log.d(TAG, "gone");
					}
					public void onAnimationRepeat(Animation animation) {
					}

					public void onAnimationStart(Animation animation) {
					}
				});
				
				gallery.setAnimation(alpha);
				lastTime = System.currentTimeMillis();	
			}
			
			handler.postDelayed(this, 1000);
		} 	
    };
    
    @Override
    public void onPause() {
    	super.onPause();
    	handler.removeCallbacks(oneSecondHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        UiModeManager manager = (UiModeManager)getSystemService(Context.UI_MODE_SERVICE);    
        manager.disableCarMode(UiModeManager.DISABLE_CAR_MODE_GO_HOME);

        unregisterReceiver(mMediaButtonReceiver);
        unregisterReceiver(myBatteryReceiver);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		myMenu = menu;

		BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();    
	    if (bt != null && bt.isEnabled()) {
	        myMenu.getItem(0).setIcon(R.drawable.bluetooth_on);
	    } else { 
	    	myMenu.getItem(0).setIcon(R.drawable.bluetooth_off);
	    }

	    return super.onPrepareOptionsMenu(menu);
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		
        // Handle item selection
        switch (item.getItemId()) {
	        case android.R.id.home:
//	          	ChangeLog cl = new ChangeLog(this);
//	        	cl.getFullLogDialog().show();
	        	return true;
	        	
        	case R.id.BTMenuItem:
        	    BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();    
        	    if (bt != null && !bt.isEnabled()) {
        	    	try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        	    	bt.enable();
        	    	item.setIcon(R.drawable.bluetooth_on);
        	    } else { 
        	        if (bt != null) bt.disable(); 
        	        item.setIcon(R.drawable.bluetooth_off);
        	    } 
        		return true;
        		
        	case R.id.quitMenuItem:
	           	System.exit(0);
	           	return true;
	           	
        	case R.id.cameraMenuItem:
                
        		if (camera == null) {
        			startCameraPreview(Camera.CameraInfo.CAMERA_FACING_FRONT, false);
        		} else {
        			stopCameraPreview();
        		}
                
            	return true;

        	
        	default:
                return super.onOptionsItemSelected(item);
        }
		
		
    }
	
	private static void startCameraPreview(int whichCamera, boolean withCallback) {
		
        try {
			if (!withCallback) mCameraView.setVisibility(View.VISIBLE);
			
			if (mCameraHolder == null) {
				mCameraHolder = mCameraView.getHolder();
				mCameraHolder.addCallback(surfaceCallback);
			}
			
	        Camera.CameraInfo info=new Camera.CameraInfo();
	
	        for (int i=0; i < Camera.getNumberOfCameras(); i++) {
	        	Camera.getCameraInfo(i, info);
	
	        	if (info.facing == whichCamera) {
	        		camera=Camera.open(i);
	        	}
	        }
	
	        if (camera == null) {
	        	camera=Camera.open();
	        }
	        
	        if (withCallback) camera.setPreviewCallback(new CameraPreviewCallback());       

			camera.setPreviewDisplay(mCameraHolder);	        
	        camera.startPreview();

        } catch (Exception e) {
			// hrm
			e.printStackTrace();
		}
	}

	private static void stopCameraPreview() {
	
		if (camera == null) return;
		
		//tear down camera here
		camera.stopPreview();
		camera.release();
		camera = null;
		
		mCameraHolder = null;
		mCameraView.setVisibility(View.INVISIBLE);
	}
	
	  static SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
		    public void surfaceCreated(SurfaceHolder holder) {
		      // no-op -- wait until surfaceChanged()
		    }

		    public void surfaceChanged(SurfaceHolder holder, int format,
		                               int width, int height) {
		    	Log.d(TAG, "surfaceChanged");
		    }

		    public void surfaceDestroyed(SurfaceHolder holder) {
		      // no-op
		    }
		  };
	
	public void onClick(View target) {
		
        if (target == mPlayButton) {
        	if (MusicService.mState == MusicService.State.Paused) {
        		startService(new Intent(MusicService.ACTION_PLAY));
        	} else {
        		if (MusicService.mState == MusicService.State.Playing) {
        		startService(new Intent(MusicService.ACTION_PAUSE));
        		} else {
        			startService(new Intent(MusicService.ACTION_SKIP));
        		}
            }
        }

        if (target == mStopButton) {
            startService(new Intent(MusicService.ACTION_STOP));
        }
        
        if (target == mForwardButton) {
        	startService(new Intent(MusicService.ACTION_SKIP));
        }
        
        if (target == mRewindButton) {
        	startService(new Intent(MusicService.ACTION_REWIND));
        }

        if (target == mMuteButton) {
        	mMuted = ! mMuted;        	
        	mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, mMuted);
        	if (mMuted) {
        		mMuteButton.setBackgroundResource(R.drawable.unmuteicon);
        	} else {
        		mMuteButton.setBackgroundResource(R.drawable.muteicon);
        	}
        }
	}    
	
	private BroadcastReceiver myBatteryReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent arg1) {

			if (arg1.getAction().equals(Intent.ACTION_BATTERY_CHANGED)){

				final int status = arg1.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
        	    BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();    
				
				if (status == BatteryManager.BATTERY_STATUS_CHARGING ||
						status == BatteryManager.BATTERY_STATUS_FULL) {
					
	        	    if (bt != null && !bt.isEnabled()) {
	        	    	bt.enable();
//	        	    	myMenu.getItem(0).setIcon(R.drawable.bluetooth_on);
	        	    } 
				} else {
	        	    if (bt != null && bt.isEnabled()) {
	        	    	bt.disable();
//	        	    	myMenu.getItem(0).setIcon(R.drawable.bluetooth_off);
	        	    } 					
				}
			}
		}
	};
	
	public static void whereAreYou(final String origin) {
		
		locationResult.setDestination(origin);
		
		MyLocation myLocation = new MyLocation();
    	myLocation.getLocation(ctx, locationResult);
	}

	public static LocationResult locationResult = new LocationResult() {
		
		String destination = "";
		
	    @Override
	    public void gotLocation(final Location location){

	    	final String text = "http://maps.google.com/?q=" + location.getLatitude() + "+" + location.getLongitude();
	    	
    		SmsManager sm = SmsManager.getDefault();
        	sm.sendTextMessage(destination, null, text, null, null);
	    }
	    
	    public void setDestination(final String destination) {
	    	this.destination = destination;
	    }
	};
	
	public static void whatDoYouSee(final String origin) {
		
		if (connectivityActionReceiver == null) {
			originator = origin;
			stopCameraPreview();
			startCameraPreview(Camera.CameraInfo.CAMERA_FACING_FRONT, true); 
			
			Handler handler = new Handler();
			handler.postDelayed(doItAgain, 30000);
			
		} else {
			Log.d(TAG, "aborting mms request - connectivity intent already exists");
		}
	}

	private static Runnable doItAgain = new Runnable() {
	
		public void run() {
			stopCameraPreview();
			startCameraPreview(Camera.CameraInfo.CAMERA_FACING_BACK, true); 			
		}
		
	};
	
	private static class CameraPreviewCallback implements PreviewCallback {

		private long startTime;
		private boolean fired = false;
		
		public CameraPreviewCallback() {
			super();
			startTime = System.currentTimeMillis();
			fired = false;
		}
		
		public void onPreviewFrame(byte[] arg0, Camera arg1) {
			if (!fired && System.currentTimeMillis() - startTime >= 3000) {
				fired = true;
				
				Log.d(TAG, "taking picture");

				camera.stopPreview();
				camera.takePicture(null, null, new CameraPictureCallback());
			}
		}
	}
	
	private static class CameraPictureCallback implements PictureCallback {
		
		public void onPictureTaken(byte[] data, Camera camera) {

			Log.d(TAG, "picture taken");

			stopCameraPreview();	
			
	        WifiManager wifi = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
	        wifiEnabled = wifi.isWifiEnabled();
	        wifi.setWifiEnabled(false);
	        
	        Log.d(TAG, "wifi disabled - was enabled=" + wifiEnabled);

			final ConnectivityManager connMgr = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
	        final int result = connMgr.startUsingNetworkFeature( ConnectivityManager.TYPE_MOBILE, FEATURE_ENABLE_MMS);
	        
	        if (result != APN_ALREADY_ACTIVE) {
				Log.d(TAG, "registering connectivity intent");
	        	final IntentFilter filter = new IntentFilter();
	        	filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
	        	connectivityActionReceiver = new ConnectivityActionReceiver(data); 	
	        	ctx.registerReceiver(connectivityActionReceiver, filter);
	        	return;
	        }
	        
			MmsWorker worker = new MmsWorker(data);
			worker.start();
		}
	}

	private static class ConnectivityActionReceiver extends BroadcastReceiver {

		private byte[] data;
		
		public ConnectivityActionReceiver(byte[] data) {
			super();
			this.data = data;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {

		   String action = intent.getAction();
		    if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
		        return;
		    }

		    NetworkInfo mNetworkInfo = (NetworkInfo) intent.getParcelableExtra(
		        ConnectivityManager.EXTRA_NETWORK_INFO);

		    // Check availability of the mobile network.
		    if ((mNetworkInfo == null) ||
		        (mNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE_MMS)) {
		        return;
		    }

		    if (!mNetworkInfo.isConnected()) {
		        return;
		    } else {
		    	//send mms
				Log.d(TAG, "intent reports MMS ready");
				MmsWorker worker = new MmsWorker(data);
				worker.start();
 		    }
		}
	}

	public static class MmsWorker extends Thread {
		
		private byte[] data;
		
		public MmsWorker(byte[] data) {
			super();
			this.data = data;
		}
		
		public void run() {

			Log.d(TAG, "sending mms message");
			
	        final SendReq sendRequest = new SendReq();
//	        final EncodedStringValue[] sub = EncodedStringValue.extract("What I'm doing. . .");
//	        if (sub != null && sub.length > 0) {
//	            sendRequest.setSubject(sub[0]);
//	        }
	        final EncodedStringValue[] phoneNumbers = EncodedStringValue
	                .extract(originator);
	        if (phoneNumbers != null && phoneNumbers.length > 0) {
	            sendRequest.addTo(phoneNumbers[0]);
	        }

	        final PduBody pduBody = new PduBody();
            final PduPart partPdu = new PduPart();

            partPdu.setContentId("<0>".getBytes());
            partPdu.setName("body".getBytes());
            partPdu.setContentType("image/jpeg".getBytes());
            
            Bitmap bm = BitmapFactory.decodeByteArray(data,0, data.length);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
            bm.compress(CompressFormat.JPEG, 20, baos);
            
            partPdu.setData(baos.toByteArray());

            pduBody.addPart(partPdu);

	        sendRequest.setBody(pduBody);

	        final PduComposer composer = new PduComposer(ctx, sendRequest);
	        final byte[] bytesToSend = composer.make();

	        try {   	
	        	APNHelper apnHelper = new APNHelper(ctx);
	        	List<APN> apns = apnHelper.getMMSApns();
	        			
	        	for (APN apn : apns) {
					HttpUtils.httpConnection(ctx, 4444L, apn.MMSCenterUrl,
					        bytesToSend, HttpUtils.HTTP_POST_METHOD, true, apn.MMSProxy, Integer.parseInt(apn.MMSPort));
					Log.d(TAG, "mms message sent");					
	        	}	

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			} finally {
				if (connectivityActionReceiver != null) {
					ctx.unregisterReceiver(connectivityActionReceiver);
					connectivityActionReceiver = null;
			        
					final ConnectivityManager connMgr = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
					connMgr.stopUsingNetworkFeature( ConnectivityManager.TYPE_MOBILE, FEATURE_ENABLE_MMS);

					Log.d(TAG,"unregistered connectivity intent");
					
			        WifiManager wifi = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
			        wifi.setWifiEnabled(wifiEnabled);
			        
			        Log.d(TAG, "wifi enabled=" + wifiEnabled);
				}
			}
		}
	}
	
	public static void rebootWithSU() {
		try {
			Runtime.getRuntime().exec(new String[]{"/system/xbin/su","-c","ls"});
			Runtime.getRuntime().exec(new String[]{"/system/xbin/su","-c","reboot now"});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public class ImageAdapter extends BaseAdapter {
	    int mGalleryItemBackground;
	    private Context mContext;

	    private Integer[] mImageIds = {
	            R.drawable.camera,
	            R.drawable.email,
	            R.drawable.navigate,
	            R.drawable.sms,
//	            R.drawable.rx8logo,
//	            R.drawable.bluetooth_off,
	            R.drawable.configuration
	    };

	    public ImageAdapter(Context c) {
	        mContext = c;
	        TypedArray attr = mContext.obtainStyledAttributes(R.styleable.HelloGallery);
	        mGalleryItemBackground = attr.getResourceId(
	                R.styleable.HelloGallery_android_galleryItemBackground, 0);
	        attr.recycle();
	    }

	    public int getCount() {
	        return mImageIds.length;
	    }

	    public Object getItem(int position) {
	        return position;
	    }

	    public long getItemId(int position) {
	        return position;
	    }

	    public View getView(int position, View convertView, ViewGroup parent) {
	        ImageView imageView = new ImageView(mContext);

	        imageView.setImageResource(mImageIds[position]);
	        imageView.setLayoutParams(new Gallery.LayoutParams(150, 100));
	        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
	        imageView.setBackgroundResource(mGalleryItemBackground);

	        return imageView;
	    }
	}
}