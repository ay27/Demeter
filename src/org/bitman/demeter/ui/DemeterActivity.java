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
 * ���activity������application����ڣ���DemeterApplication�й���
 * �����������£�
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
		
		// ׼��http����
		HttpServer.prepare();
		// TODO fix it
		//HttpServer.send(HttpServer.LOGIN, "");
		//HttpServer.send(HttpServer.ONLINE, "");
		sendOnline = new SendOnline();
		sendOnline.start();
		
	}
	
	// ������ر�http����
	@Override
	protected void onDestroy() {
		// �ر�HttpServer
		HttpServer.send(HttpServer.CLOSE, "");
		
		Log.i("Kill My Self", "1");
		// ��ɱ
		Process.killProcess(Process.myPid());
		Log.i("Kill My Self", "2");
		
		super.onStop();
	}


	//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
	// �������ӷ�����
	View.OnClickListener connectListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String addr = serverAddress.getText().toString();
			if (addr==null || addr.equals(""))
			{
				Toast.makeText(context, "����д��������ַ", Toast.LENGTH_LONG).show();
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
			Toast.makeText(context, "���ӳɹ�", Toast.LENGTH_LONG).show();
			
		}
	};
	
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
	private EditText searchText;
	View.OnClickListener play_record_Listener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			AlertDialog.Builder chooseFunctionBuilder = new AlertDialog.Builder(context);
			chooseFunctionBuilder.setTitle("�����ص�").setView(searchText = new EditText(context));
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
			//chooseFunctionBuilder.setPositiveButton("����", searchListener);
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
		
		// �����ص㣬��������������ݣ�Ȼ�����cityName
		@Override
		public void onClick(DialogInterface dialog, int which) {
			String content = searchText.getText().toString();
			if (content==null || content.equals(""))
			{
				Toast.makeText(context, "����д��ַ", Toast.LENGTH_LONG).show();
				return;
			}
			
			// ����,��������
			String receive = HttpServer.send(HttpServer.LISTCITY, content);
			
			// ��ȡcityName���洢��cityName, cityID�У�ʧ����᷵��false
			if (!parseCityList(receive)) return;
			
			AlertDialog.Builder chooseAddrBuilder = new AlertDialog.Builder(context);
			chooseAddrBuilder.setTitle("ѡ��ص�").setNegativeButton("ȡ��", null);
			
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
		
		// ���ѡ����һ���ص�ʱ�Ķ���
		@Override
		public void onClick(DialogInterface dialog, int which) {
			HttpServer.cityID = cityID.get(which).toString();
			
			// ѡ��鿴��ʽ
			new AlertDialog.Builder(context)
			.setTitle("ѡ��鿴��ʽ")
			.setPositiveButton("���ڵ�", nowListener)
			.setNegativeButton("��ȥ��", oldListener)
			.create()
			.show();
			
			dialog.dismiss();
		}
	};
	
	DialogInterface.OnClickListener REC_chooseCityListener = new DialogInterface.OnClickListener() {
		
		// ���ѡ����һ���ص�ʱ�Ķ���
		@Override
		public void onClick(DialogInterface dialog, int which) {
			HttpServer.cityID = cityID.get(which).toString();
			
			// ������RecordActivity������¼��
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
			
			// �������ݣ����ݽ��洢��camList��
			if (!parseCamList(camData)) return;
			
			// ��ʾ����ͷ�ĸ���
			new AlertDialog.Builder(context)
			.setTitle("ѡ������ͷ")
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
			
			// �������ݣ��洢��camList�У� ���������֣�һ����phoneID������һ����camID
			if (!parseCamList(camData)) return;
			
			// ��ʾ����ͷ�ĸ���
			new AlertDialog.Builder(context)
			.setTitle("ѡ������ͷ")
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
			
			// �ж�������ͷ�����ֻ���û��'.'�����ֻ�
			if (camList.get(which).indexOf('.') == -1)
			{
				String receive = HttpServer.send(HttpServer.LISTFILE, camList.get(which));
				
				// ����fileNameList���洢��fileList��
				if (!parseFileList(receive)) return;
				
				new AlertDialog.Builder(context)
				.setTitle("ѡ����ֻ�¼�Ƶ���Ƶ")
				.setSingleChoiceItems(fileList.toArray(new String[fileList.size()]), 0, playFileListener)
				.create()
				.show();
			}
			// is camera
			else {
				// ѡ��ʱ��
				timeText = new EditText(context);
				timeText.setInputType(InputType.TYPE_CLASS_DATETIME);
				new AlertDialog.Builder(context)
				.setTitle("ʱ���ʽ��YYYY-MM-DD-hh-mm��")
				.setView(timeText)
				.setPositiveButton("ȷ��", playTimeListener)
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
				Toast.makeText(context, "����д�鿴��ʱ��", Toast.LENGTH_LONG).show();
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
		// ����
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
		
		// ��ȡ����
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
	
	// ���camList��������ȡ���洢��camList��
	private boolean parseCamList(String receive)
	{
		
		camList = new Vector<String>();
		if (receive.equals("ERROR"))
		{
			Toast.makeText(context, HttpServer.errorType, Toast.LENGTH_LONG).show();
			return false;
		}
		
		// ��ȡ����
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
		
		// ��ȡ����
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
	

	// �����÷��ó�һ��button�������û�����
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
		menu.add(Menu.NONE, Menu.FIRST+1, 1, "��Ƶ��ʽ����");
		menu.add(Menu.NONE, Menu.FIRST+2, 2, "�˳�");
		return true;
	}
	*/

}
