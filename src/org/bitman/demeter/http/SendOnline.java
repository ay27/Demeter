package org.bitman.demeter.http;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

/**
 * 每一分钟发送一个online消息，如果在一分钟后服务器没有收到该消息，则会断开此次连接
 * @author ay27
 *
 */

public class SendOnline extends Thread{
	
	private SendOnline context = this;

	@Override
	public void run() {
		
		Timer timer = new Timer();
		TimerTask task;
		task = new TimerTask() {
			
			@Override
			public void run() {
				try {
					HttpServer.send(HttpServer.ONLINE, "");
					//context.run();
					new SendOnline().start();
					
					return;
					
					//Log.e("Fuck", " do not kill the timer");
				} catch (Exception e) {
					Log.e("schedule", e.toString());
				}
			}
		};
		timer.schedule(task, 15*1000);
	}
	
	

}
