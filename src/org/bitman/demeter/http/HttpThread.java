package org.bitman.demeter.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class HttpThread extends Thread{
	
	public URL url;
	public String send;
	public boolean error = false;
	
	// �߳��ڲ��ı���
	private HttpURLConnection connection;
	private String receive = null;
	
	public HttpThread(URL url, String send)
	{
		this.send = send;
		this.url = url;
		
		
	}
	
	public String work()
	{
		this.start();
		while (receive == null)
		{
			// �п���cpu�ٶ�̫�죬�����˲���ȥ������Ҫsleep
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) { }
		}
		return receive;
	}
	
	@Override
	public void run()
	{
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			// �������ó�ʱ
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			
			connection.setRequestMethod("POST");
			connection.connect();
			
			// send
			OutputStream dop = connection.getOutputStream();
			dop.write(send.getBytes("UTF-8"));
			dop.flush();
			dop.close();
			
			// receive
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String readLine = null;
			while ((readLine = bufferedReader.readLine()) != null)
				sb.append(readLine);
			receive = sb.toString();
			bufferedReader.close();
			
			connection.disconnect();
			
			super.run();
		}catch (Exception e) {
			error = true;
			Log.e("HttpThread", e.toString());
			receive = "ERROR#"+e.toString();
		}
		finally {
			
		}
	}
}
