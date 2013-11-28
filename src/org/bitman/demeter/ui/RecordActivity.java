package org.bitman.demeter.ui;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.bitman.demeter.DemeterApplication;
import org.bitman.demeter.Utilities;
import org.bitman.streaming.SessionBuilder;
import org.bitman.streaming.rtsp.RtspServer;

import org.bitman.demeter.R;
import org.bitman.demeter.http.HttpServer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.text.InputType;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * RecordActivity主要打开rtspServer，等待服务器连接，然后打开摄像头，往外发送数据
 * @author ay27
 *
 */

/**
 * @author 锦棉
 *
 */
public class RecordActivity extends Activity {

	public final static String TAG = "RecordActivity";
	
    private TextView mLine2, mSignWifi, mTextBitrate;
    private LinearLayout mSignInformation, mSignStreaming;
    private Animation mPulseAnimation;
    
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    
    private DemeterApplication mApplication;
    private RtspServer mRtspServer;
    
    private PowerManager.WakeLock mWakeLock;
    
    private RecordActivity context;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	mApplication  = (DemeterApplication)getApplication();
    	context = this;
    	
    	setContentView(R.layout.main);
    	
    	mSurfaceView = (SurfaceView)findViewById(R.id.handset_camera_view);
    	mSurfaceHolder = mSurfaceView.getHolder();
    	mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    	
    	SessionBuilder.getInstance().setSurfaceHolder(mSurfaceHolder);
    	PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
    	mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "org.bitman.demeter.wakelock");
    	
    	this.startService(new Intent(this, RtspServer.class));
    	
    	
        mLine2 = (TextView)findViewById(R.id.line2);
        mSignWifi = (TextView)findViewById(R.id.advice);
        mSignStreaming = (LinearLayout)findViewById(R.id.streaming);
        mSignInformation = (LinearLayout)findViewById(R.id.information);
        mPulseAnimation = AnimationUtils.loadAnimation(mApplication.getApplicationContext(), R.anim.pulse);
        mTextBitrate = (TextView)findViewById(R.id.bitrate);
        
        Log.i("RecordActivity", "onCreate_end");
        
        
        // TODO onStart
		mWakeLock.acquire();
		
		bindService(new Intent(this,RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
        
        // TODO onResume
    	mApplication.applicationForeground = true;
		bindService(new Intent(context,RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
    	registerReceiver(mWifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    	update();
        
    }
	
	
	private void sleep(final long time)
	{
		if (time <= 0) return;
		
		Timer timer = new Timer();
		TimerTask task;
		task = new TimerTask() {
			
			@Override
			public void run() {
				try {
					context.quitDemeter();
				} catch (Exception e) {
					Log.e("schedule", e.toString());
					Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
				}
			}
		};
		timer.schedule(task, time);
		return;
	}
	
	
	/*
	public void onStart() {

		Log.i("RecordActivity", "onStart_start");
		// Lock screen
		mWakeLock.acquire();
		
		bindService(new Intent(this,RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);

		Log.i("RecordActivity", "onStart_end");
		
		super.onStart();
		

	}
	*/
	
	// 按下返回键时直接退出当前activity，否则会弹出定时窗
	@Override
	public void onBackPressed() {
		quitDemeter();
		
		super.onBackPressed();
	}

	

	/*
	@Override
	protected void onDestroy() {
		
		// TODO onPause
	   	super.onPause();
    	unregisterReceiver(mWifiStateReceiver);
    	unbindService(mRtspServiceConnection);
    	mApplication.applicationForeground = false;
    	
    	// TODO onStop
		// A WakeLock should only be released when isHeld() is true !
		if (mWakeLock.isHeld()) mWakeLock.release();
		
		// 关闭HttpServer
		HttpServer.send(HttpServer.CLOSE, "");

		if (mRtspServer != null) mRtspServer.removeCallbackListener(mRtspCallbackListener);

		quitDemeter();
		super.onDestroy();
	}
	*/
	/*
	@Override
	public void onStop() {
		Log.i("RecordActivity", "onStop_start");
		// A WakeLock should only be released when isHeld() is true !
		if (mWakeLock.isHeld()) mWakeLock.release();
		
		// 关闭HttpServer
		HttpServer.send(HttpServer.CLOSE, "");

		if (mRtspServer != null) mRtspServer.removeCallbackListener(mRtspCallbackListener);

		super.onStop();
		
		Log.i("RecordActivity", "onStop_end");
		
	}
	
	// 关闭服务
	@Override
    public void onPause() {
		
		Log.i("RecordActivity", "onPause_start");
		
    	super.onPause();
    	unregisterReceiver(mWifiStateReceiver);
    	unbindService(mRtspServiceConnection);
    	mApplication.applicationForeground = false;
    	
    	Log.i("RecordActivity", "onPause_end");
    }
	*/
	public void quitDemeter() {      

		// Kills RTSP server
		this.stopService(new Intent(this,RtspServer.class));
		// Returns to home menu
		
		//TODO close it
		Log.i("kill", "in rtspServer");
		Process.killProcess(Process.myPid());
		
		finish();
	}
	
	/*
	// 打开服务
	private EditText REC_timeText;
	@Override
    public void onResume() {
    	super.onResume();
    	mApplication.applicationForeground = true;
		bindService(new Intent(context,RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
    	registerReceiver(mWifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    	update();
    	
    	Log.i("RecordActivity", "onResume_end");
    	
    	REC_timeText = new EditText(context);
    	REC_timeText.setInputType(InputType.TYPE_CLASS_NUMBER);
		// 定时
		new AlertDialog.Builder(context)
		.setTitle("定时（秒），0表示不定时")
		.setView(REC_timeText)
		.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				String time = REC_timeText.getText().toString();
				if (time==null || time.equals(""))
				{
					Toast.makeText(context, "请填写时间", Toast.LENGTH_LONG).show();
					return;
				}
				
				String receive = HttpServer.send(HttpServer.RECORD, "");
				if (receive.equals("ERROR"))
				{
					Toast.makeText(context, HttpServer.errorType, Toast.LENGTH_LONG).show();
					return;
				}
				dialog.dismiss();
				
				sleep(Integer.parseInt(REC_timeText.getText().toString())*1000);
				
			}
		})
		.create()
		.show();
    }*/
	
	// 更新视图
	public void update() {
		runOnUiThread(new Runnable () {
			@Override
			public void run() {
				if (mRtspServer != null)
				{
					mLine2.setVisibility(View.VISIBLE);
					if (!mRtspServer.isStreaming())
						displayIpAddress();
					else streamingState(1);
				}
			}
		});
	}
	
	// 选择显示的界面
	private void streamingState(int state) {
		if (state==0) {
			// Not streaming
			mSignStreaming.clearAnimation();
			mSignWifi.clearAnimation();
			mSignStreaming.setVisibility(View.GONE);
			mSignInformation.setVisibility(View.VISIBLE);
			mSignWifi.setVisibility(View.GONE);
		} else if (state==1) {
			// Streaming
			mSignWifi.clearAnimation();
			mSignStreaming.setVisibility(View.VISIBLE);
			mSignStreaming.startAnimation(mPulseAnimation);
			mHandler.post(mUpdateBitrate);
			mSignInformation.setVisibility(View.INVISIBLE);
			mSignWifi.setVisibility(View.GONE);
		} else if (state==2) {
			// No wifi !
			mSignStreaming.clearAnimation();
			mSignStreaming.setVisibility(View.GONE);
			mSignInformation.setVisibility(View.INVISIBLE);
			mSignWifi.setVisibility(View.VISIBLE);
			mSignWifi.startAnimation(mPulseAnimation);
		}
	}
	
	// 显示本地ip，主要被update()调用
    private void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) mApplication.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
		String ipaddress = null;
    	if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	        String ip = String.format(Locale.ENGLISH,"%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
	    	mLine2.setText("rtsp://");
	    	mLine2.append(ip);
	    	mLine2.append(":"+mRtspServer.getPort());
	    	streamingState(0);
	    	// 设置HttpServer的地址
	    	HttpServer.localRtspAddr = mLine2.getText().toString();
	    	
    	} else if((ipaddress = Utilities.getLocalIpAddress(true)) != null) {
	    	mLine2.setText("rtsp://");
	    	mLine2.append(ipaddress);
	    	mLine2.append(":"+mRtspServer.getPort());
	    	streamingState(0);
	    	// 设置HttpServer的地址
	    	HttpServer.localRtspAddr = mLine2.getText().toString();
	    	
    	} else {
    		streamingState(2);
    	}
    	
    }


	// 启动应用
    private final ServiceConnection mRtspServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRtspServer = (RtspServer) ((RtspServer.LocalBinder)service).getService();
			mRtspServer.addCallbackListener(mRtspCallbackListener);
			mRtspServer.start();
			update();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}
		
	};
	
	private RtspServer.CallbackListener mRtspCallbackListener = new RtspServer.CallbackListener() {

		@Override
		public void onError(RtspServer server, Exception e, int error) {
			// We alert the user that the port is already used by another app.
			if (error == RtspServer.ERROR_BIND_FAILED) {
				new AlertDialog.Builder(context)
				.setTitle(R.string.port_used)
				.setMessage(getString(R.string.bind_failed, "RTSP"))
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						startActivityForResult(new Intent(context, OptionsActivity.class),0);
					}
				})
				.show();
			}
		}

		@Override
		public void onMessage(RtspServer server, int message) {
			update();
		}

	};	
    
    // BroadcastReceiver that detects wifi state changements
    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	// This intent is also received when app resumes even if wifi state hasn't changed :/
        	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        		update();
        	}
        } 
    };
    
	private final Handler mHandler = new Handler();
    
	private Runnable mUpdateBitrate = new Runnable() {
		@Override
		public void run() {
			if (mRtspServer != null && mRtspServer.isStreaming()) {
				long bitrate = mRtspServer!=null? mRtspServer.getBitrate():0;
				mTextBitrate.setText(""+bitrate/1000+" kbps");
				mHandler.postDelayed(mUpdateBitrate, 1000);
			} else {
				mTextBitrate.setText("0 kbps");
			}
		}
	};

}
