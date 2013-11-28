package org.bitman.demeter.http;

import java.net.URL;

import android.util.Log;


/**
 * ������Ƕ�HttpThread�ķ�װ�����������ֻ�����෽�������������󷽷���
 * Ŀ����ʹ�����application��ֻ��һ��HttpThread����
 * @author ay27
 *
 */

public class HttpServer {

	// ������MainThread����������������
	public static String path = null;
	public static String meid = null;
	
	public static String cityID = null;
	
	public static String localRtspAddr = null;
	
	public static String errorType = null;
	
	// ������prepare()������
	private static URL url;
	
	public static String serverPath = "http://192.168.0.107:8080/Server/Servlet";
	public static final int LISTCITY = 0;		// ��ȡcityID
	public static final int RECORD = 1;			// ��ʼ¼��
	public static final int CLOSE = 2;			// �ر�����
	public static final int LISTDIR = 3;		// ��ȡ��ȥ���б�
	public static final int LISTNOW = 4;		// ��ȡ���ڵ��б�
	public static final int LISTFILE = 5;		// ��ȡ��ȥ�ġ��ֻ����ļ��б�
	public static final int PLAYFILE = 6;		// ���Ź�ȥ�ġ��ֻ����ļ�
	public static final int PLAYTIME = 7;		// ���Ź�ȥ�ġ�����ͷ��ĳ��ʱ��
	public static final int PLAYNOW = 8;		// ������������¼�Ƶ�
	public static final int ONLINE = 9;		// ��ʱ����һ��online��Ϣ
	
	public static HttpThread currentThread;
	
	public static String receive;
	
	public static void prepare()
	{
		try {
			url = new URL(serverPath);
		} catch (Exception e)
		{
			Log.e("prepare", e.toString());
		}
	}
	
	public static String send(int type, String content)
	{
		StringBuffer sBuffer = new StringBuffer();
		switch (type) {
		case LISTCITY:
			sBuffer.append("LISTCITY#");
			//sBuffer.append(meid);
			//sBuffer.append("#");
			sBuffer.append(content);		// keyword
			break;
		case RECORD:
			sBuffer.append("RECORD#");
			//sBuffer.append(meid);
			//sBuffer.append("/");
			sBuffer.append(cityID);
			sBuffer.append("#");
			sBuffer.append(meid);
			sBuffer.append("#");
			sBuffer.append(localRtspAddr);
			break;
		case CLOSE:
			sBuffer.append("CLOSE#");
			//sBuffer.append(cityID);
			//sBuffer.append("/");
			//sBuffer.append(meid);
			sBuffer.append(meid);
			//sBuffer.append("#");
			//sBuffer.append(PID);
			break;
		case LISTDIR:
			sBuffer.append("LISTDIR#");
			sBuffer.append(cityID);
			break;
		case LISTNOW:
			sBuffer.append("LISTNOW#");
			sBuffer.append(cityID);
			break;
		case LISTFILE:
			sBuffer.append("LISTFILE#");
			sBuffer.append(cityID);
			sBuffer.append("#");
			sBuffer.append(content);	// phoneID
			break;
		case PLAYFILE:
			sBuffer.append("PLAYFILE#");
			sBuffer.append(cityID);
			sBuffer.append("#");
			sBuffer.append(content);	// PhoneID#FileName
			sBuffer.append("#");
			sBuffer.append(meid);
			break;
		case PLAYTIME:
			sBuffer.append("PLAYTIME#");
			sBuffer.append(cityID);
			sBuffer.append("#");
			sBuffer.append(content);	// camID#time
			sBuffer.append("#");
			sBuffer.append(meid);
			break;
		case PLAYNOW:
			sBuffer.append("PLAYNOW#");
			sBuffer.append(cityID);
			sBuffer.append("#");
			sBuffer.append(content);	// CamID or phoneID
			sBuffer.append("#");
			sBuffer.append(meid);
			break;
		/*case LOGIN:
			sBuffer.append("LOGIN#");
			sBuffer.append(meid);
			break;*/
		case ONLINE:
			sBuffer.append("ONLINE#");
			sBuffer.append(meid);
			break;
		default:
			break;
		}
		
		Log.i("send", sBuffer.toString());
		
		errorType = null;
		currentThread = new HttpThread(url, sBuffer.toString());
		receive = currentThread.work();
		
		Log.i("receive", receive);
		
		
		String[] strings;
		if ((strings = receive.split("#"))[0].equals("ERROR") || currentThread.error)
		{
			errorType = strings[1];
			
			return "ERROR";
		}

		
		return receive;
	}

}
