package org.bitman.demeter.ui;

import java.util.Vector;

import org.bitman.demeter.R;
import org.bitman.demeter.http.HttpServer;
import org.bitman.demeter.http.SendOnline;

import org.json.JSONArray;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 这个activity是整个application的入口，从DemeterApplication中过来
 * 整个流程如下：
 * 1. playButton -> play_record_Listener -> searchListener ->
 *	        PLA_chooseCityListener -> now or old Listener -> doPlay()
 *        
 * 2. recordButton -> play_record_Listener -> searchListener ->
 * 			REC_chooseCityListener -> RecordActivity
 * @author ay27
 *
 */
public class DemeterActivity extends Activity{

	private Button recordButton;
	private Button playButton;
	private Button serverButton;
	private EditText serverAddress;
	
	private Button settingButton;
	
	private DemeterActivity context;
	
	private SendOnline sendOnline;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_layout);
		
		
		recordButton = (Button) findViewById(R.id.welcome_record);
		playButton = (Button) findViewById(R.id.welcome_play);
		serverButton = (Button) findViewById(R.id.serverLink);
		settingButton = (Button) findViewById(R.id.setting_button);
		
		recordButton.setOnClickListener(play_record_Listener);
		playButton.setOnClickListener(play_record_Listener);
		serverButton.setOnClickListener(connectListener);
		settingButton.setOnClickListener(settingListener);
		
		serverAddress = (EditText) findViewById(R.id.serverAddress);
		serverAddress.setText(HttpServer.serverPath);
		
		context = this;
		
		// get the MEID
		String meid = ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).getDeviceId();
		// start the service of the http server
		HttpServer.path = HttpServer.serverPath;
		HttpServer.meid = meid.length()==15? meid:meid+"0";
		
		// 准备http服务
		HttpServer.prepare();
		// TODO fix it
		//HttpServer.send(HttpServer.LOGIN, "");
		//HttpServer.send(HttpServer.ONLINE, "");
		sendOnline = new SendOnline();
		sendOnline.start();
		
	}
	
	// 在这里关闭http服务
	@Override
	protected void onDestroy() {
		// 关闭HttpServer
		HttpServer.send(HttpServer.CLOSE, "");
		
		Log.i("Kill My Self", "1");
		// 自杀
		Process.killProcess(Process.myPid());
		Log.i("Kill My Self", "2");
		
		super.onStop();
	}


	//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
	// 测试连接服务器
	View.OnClickListener connectListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String addr = serverAddress.getText().toString();
			if (addr==null || addr.equals(""))
			{
				Toast.makeText(context, "请填写服务器地址", Toast.LENGTH_LONG).show();
				return;
			}
			HttpServer.serverPath = addr;
			HttpServer.prepare();
			String receive = HttpServer.send(HttpServer.ONLINE, "");
			
			if (receive.equals("ERROR"))
			{
				Toast.makeText(DemeterActivity.this, HttpServer.errorType, Toast.LENGTH_LONG).show();
				return;
			}
			Toast.makeText(context, "连接成功", Toast.LENGTH_LONG).show();
			
		}
	};
	
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
	private EditText searchText;
	View.OnClickListener play_record_Listener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			AlertDialog.Builder chooseFunctionBuilder = new AlertDialog.Builder(context);
			chooseFunctionBuilder.setTitle("搜索地点").setView(searchText = new EditText(context));
			if (v.getId() == playButton.getId())
			{
				searchType = PLAY;
				Log.i("choose", "PLAY");
				
				// TODO fix it
				doPlay(Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.sdp");
				
			}
			else if (v.getId() == recordButton.getId())
			{
				searchType = RECORD;
				Log.i("choose", "RECORD");
				
				// TODO fix it
				Intent intent = new Intent(context, RecordActivity.class);
				startActivity(intent);
				
			}
			else {
				Log.e("choose", "Nothing");
				Toast.makeText(context, "fatal error", Toast.LENGTH_LONG).show();
				context.onDestroy();
			}
			//chooseFunctionBuilder.setPositiveButton("搜索", searchListener);
			//chooseFunctionBuilder.create().show();

		}
	};
//-----------------------------------------------------------------------------
	
	private Vector<String> cityName;
	private Vector<Integer> cityID;
	private int searchType = 0;
	private final static int RECORD = 1;
	private final static int PLAY = 2;
	
	DialogInterface.OnClickListener searchListener = new DialogInterface.OnClickListener() {
		
		// 搜索地点，向服务器发送数据，然后接收cityName
		@Override
		public void onClick(DialogInterface dialog, int which) {
			String content = searchText.getText().toString();
			if (content==null || content.equals(""))
			{
				Toast.makeText(context, "请填写地址", Toast.LENGTH_LONG).show();
				return;
			}
			
			// 发送,接收数据
			String receive = HttpServer.send(HttpServer.LISTCITY, content);
			
			// 获取cityName，存储到cityName, cityID中，失败则会返回false
			if (!parseCityList(receive)) return;
			
			AlertDialog.Builder chooseAddrBuilder = new AlertDialog.Builder(context);
			chooseAddrBuilder.setTitle("选择地点").setNegativeButton("取消", null);
			
			if (searchType == PLAY)
				chooseAddrBuilder.setSingleChoiceItems(cityName.toArray(new String[cityName.size()]), 0, PLA_chooseCityListener);
			else if (searchType == RECORD)
				chooseAddrBuilder.setSingleChoiceItems(cityName.toArray(new String[cityName.size()]), 0, REC_chooseCityListener);
			else return;
			
			chooseAddrBuilder.create().show();
			
			dialog.dismiss();
		}
	};
	
	DialogInterface.OnClickListener PLA_chooseCityListener = new DialogInterface.OnClickListener() {
		
		// 点击选择了一个地点时的动作
		@Override
		public void onClick(DialogInterface dialog, int which) {
			HttpServer.cityID = cityID.get(which).toString();
			
			// 选择查看方式
			new AlertDialog.Builder(context)
			.setTitle("选择查看方式")
			.setPositiveButton("现在的", nowListener)
			.setNegativeButton("过去的", oldListener)
			.create()
			.show();
			
			dialog.dismiss();
		}
	};
	
	DialogInterface.OnClickListener REC_chooseCityListener = new DialogInterface.OnClickListener() {
		
		// 点击选择了一个地点时的动作
		@Override
		public void onClick(DialogInterface dialog, int which) {
			HttpServer.cityID = cityID.get(which).toString();
			
			// 启动到RecordActivity来进行录制
			Intent intent = new Intent(DemeterActivity.this, RecordActivity.class);
			startActivityForResult(intent, 0);
			dialog.dismiss();
		}
	};

	
	private Vector<String> camList;
	DialogInterface.OnClickListener nowListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			String camData = HttpServer.send(HttpServer.LISTNOW, "");
			
			// 解析数据，数据将存储到camList中
			if (!parseCamList(camData)) return;
			
			// 显示摄像头的个数
			new AlertDialog.Builder(context)
			.setTitle("选择摄像头")
			.setSingleChoiceItems(camList.toArray(new String[camList.size()]), 0, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog1, int which1) {
					String data = HttpServer.send(HttpServer.PLAYNOW, camList.get(which1));
					if (data.equals("ERROR"))
					{
						Toast.makeText(context, HttpServer.errorType, Toast.LENGTH_LONG).show();
						dialog1.dismiss();
						return;
					}
					
					doPlay(data);
					
					dialog1.dismiss();
				}
			})
			.create()
			.show();
			
			dialog.dismiss();
		}
	};
	

	private EditText timeText;
	private String selectTime = null;
	private Vector<String> fileList;
	DialogInterface.OnClickListener oldListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			String camData = HttpServer.send(HttpServer.LISTDIR, "");
			
			// 解析数据，存储到camList中， 数据有两种，一种是phoneID，还有一种是camID
			if (!parseCamList(camData)) return;
			
			// 显示摄像头的个数
			new AlertDialog.Builder(context)
			.setTitle("选择摄像头")
			.setSingleChoiceItems(camList.toArray(new String[camList.size()]), 0, chooseCamListener)
			.create()
			.show();
			
			dialog.dismiss();
		}
		
	};
	
	private int chooseCam = 0;
	DialogInterface.OnClickListener chooseCamListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			chooseCam = which;
			
			// 判断是摄像头还是手机，没有'.'的是手机
			if (camList.get(which).indexOf('.') == -1)
			{
				String receive = HttpServer.send(HttpServer.LISTFILE, camList.get(which));
				
				// 解析fileNameList，存储到fileList中
				if (!parseFileList(receive)) return;
				
				new AlertDialog.Builder(context)
				.setTitle("选择该手机录制的视频")
				.setSingleChoiceItems(fileList.toArray(new String[fileList.size()]), 0, playFileListener)
				.create()
				.show();
			}
			// is camera
			else {
				// 选择时间
				timeText = new EditText(context);
				timeText.setInputType(InputType.TYPE_CLASS_DATETIME);
				new AlertDialog.Builder(context)
				.setTitle("时间格式“YYYY-MM-DD-hh-mm”")
				.setView(timeText)
				.setPositiveButton("确定", playTimeListener)
				.create()
				.show();
			}
			
			dialog.dismiss();
			
		}
	};
	
	DialogInterface.OnClickListener playTimeListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			selectTime = timeText.getText().toString();
			if (selectTime==null || selectTime.equals(""))
			{
				Toast.makeText(context, "请填写查看的时间", Toast.LENGTH_LONG).show();
				return;
			}
			
			String data = HttpServer.send(HttpServer.PLAYTIME, camList.get(chooseCam)+"#"+selectTime);
			
			if (data.equals("ERROR"))
			{
				Toast.makeText(context, HttpServer.errorType, Toast.LENGTH_LONG).show();
				return;
			}

			doPlay(data);
			
			dialog.dismiss();
		}
	};
	
	DialogInterface.OnClickListener playFileListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			String data = HttpServer.send(HttpServer.PLAYFILE, camList.get(chooseCam)+"#"+fileList.get(which));
			
			if (data.equals("ERROR"))
			{
				Toast.makeText(context, HttpServer.errorType, Toast.LENGTH_LONG).show();
				return;
			}

			doPlay(data);
			
			dialog.dismiss();
		}
	};
	
	private void doPlay(String data)
	{
		// 播放
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
			try {
				Intent intent = new Intent(DemeterActivity.this, PlayerActivity.class);
				intent.putExtra("rtspAddr", data);
				startActivityForResult(intent, 0);
			} catch (Exception e)
			{
				Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("play", e.toString());
			}
	}
	
	private boolean parseFileList(String receive) {
		fileList = new Vector<String>();
		if (receive.equals("ERROR"))
		{
			Toast.makeText(context, HttpServer.errorType, Toast.LENGTH_LONG).show();
			return false;
		}
		
		// 提取数据
		JSONArray array = null;
		try {
			array = new JSONArray(receive);
			for (int i=0; i<array.length(); i++)
			{
				fileList.add(array.getString(i));
			}
		}catch (Exception e) {
			Log.e("getFileList", e.toString());
			return false;
		}
		return true;
	}
	
	// 完成camList的数据提取，存储到camList中
	private boolean parseCamList(String receive)
	{
		
		camList = new Vector<String>();
		if (receive.equals("ERROR"))
		{
			Toast.makeText(context, HttpServer.errorType, Toast.LENGTH_LONG).show();
			return false;
		}
		
		// 提取数据
		JSONArray array = null;
		try {
			array = new JSONArray(receive);
			for (int i=0; i<array.length(); i++)
			{
				camList.add(array.getString(i));
			}
		}catch (Exception e) {
			Log.e("getCamList", e.toString());
			return false;
		}
		return true;
	}
	
	private boolean parseCityList(String receive)
	{
		cityName = new Vector<String>();
		cityID = new Vector<Integer>();
		
		if (receive.equals("ERROR"))
		{
			Toast.makeText(context, HttpServer.errorType, Toast.LENGTH_LONG).show();
			return false;
		}
		
		// 提取数据
		JSONArray array = null;
		JSONArray temp;
		try {
			array = new JSONArray(receive);
			for (int i=0; i<array.length(); i++)
			{
				temp = array.getJSONArray(i);
				cityID.add(temp.getInt(0));
				cityName.add(temp.getString(1));
			}
		}catch (Exception e) {
			Log.e("getCityList", e.toString());
			return false;
		}
		return true;
	}
	

	// 将设置放置成一个button，方便用户设置
	View.OnClickListener settingListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(DemeterActivity.this, OptionsActivity.class);
			startActivityForResult(intent, 0);
		}
	};

 /*  @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Menu.FIRST+1:
			Intent intent = new Intent(DemeterActivity.this, OptionsActivity.class);
			startActivityForResult(intent, 0);
			return true;
		case Menu.FIRST+2:
			context.onBackPressed();
			return true;
		default:
			return false;
		}
	}
    @Override    
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, Menu.FIRST+1, 1, "视频格式设置");
		menu.add(Menu.NONE, Menu.FIRST+2, 2, "退出");
		return true;
	}
	*/

}
