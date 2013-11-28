package org.bitman.demeter.ui;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;

import org.bitman.demeter.R;

import android.R.integer;
import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PlayerActivity extends Activity{
	public static final String TAG = "PlayerActivity";

	private SurfaceView playerSurface;
	private SurfaceHolder playerHolder;
	private TextView timeText;
	private MediaPlayer player;
	private String playAddr;
	private Button startButton;
	private File file;
	private FileDescriptor fd;
	private Uri uri;
	
	private Context context;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.player);
		
		playerSurface = (SurfaceView) findViewById(R.id.playerSurface);
		timeText = (TextView) findViewById(R.id.timeText);
		startButton = (Button) findViewById(R.id.startButton);
		
		playerHolder = playerSurface.getHolder();
		playerHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		player = new MediaPlayer();
		
		playAddr = this.getIntent().getStringExtra("rtspAddr");
		//playAddr = "rtsp://121.49.83.249:8554/";
		
		startButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				
				try {
					player.setDisplay(playerHolder);
					//player.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.sdp");
					player.setDataSource(playAddr);
					player.prepareAsync();
				} catch (Exception e)
				{
					Log.e(TAG, e.toString());
				}
				
				playedTime = 0;
				new Thread(timeUpdate).start();
				
				Toast.makeText(context, "ÕýÔÚ»º³å...", Toast.LENGTH_LONG).show();
				
				player.start();
			}
		});
		
	}
	
	private int playedTime = 0;
	private Runnable timeUpdate = new Runnable() {
		
		@Override
		public void run() {
			while (player.getVideoWidth() == 0)
			{
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
			changeSize.sendMessage(new Message());
			disableButton.sendMessage(new Message());
			while (true)
			try {
				Thread.sleep(1000);
				playedTime++;
				changeTime.sendMessage(new Message());
			} catch (Exception e) {
				
			}
		}
	};
	
	private Handler disableButton = new Handler() {
		@Override
		public void handleMessage(Message msg)
		{
			startButton.setEnabled(false);
			startButton.setVisibility(Button.INVISIBLE);
		}
	};
	
	// change the surface size
	private Handler changeSize = new Handler() {
		@Override
		public void handleMessage(Message msg)
		{
			playerHolder.setFixedSize(player.getVideoWidth(), player.getVideoHeight());
			RelativeLayout.LayoutParams lp = 
					new RelativeLayout.LayoutParams(player.getVideoWidth(), player.getVideoHeight());
			lp.width = player.getVideoWidth();
			lp.height = player.getVideoHeight();
			playerSurface.setLayoutParams(lp);
			playerSurface.invalidate();
		}
	};
	
	private Handler changeTime = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			String time = ""+(playedTime/60/60)+":"+(playedTime/60)+":"+(playedTime%60);
			timeText.setText(time);
			
		}
		
	};

	@Override
	protected void onDestroy() {
		
		if (player.isPlaying())
			player.stop();

		Log.i(TAG, "kill myself");
		Process.killProcess(Process.myPid());
		
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		
		onDestroy();
	}
	
	

}
