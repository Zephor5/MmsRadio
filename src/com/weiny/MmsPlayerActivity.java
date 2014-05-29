package com.weiny;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MmsPlayerActivity extends Activity {
	
	static {
		try {
			System.loadLibrary("mmsPlayer");
			Log.v("MMS", "load lib success");
		} catch (UnsatisfiedLinkError localUnsatisfiedLinkError) {
			localUnsatisfiedLinkError.printStackTrace();
		}
	}

	private native int closemms();

	private native byte[] getmmsBytes();

	private native int getmmslistsize();

	public native int initmms();
	
	private native int openmms(String paramString);
	
	private native int wmaDecoder();

	private native int wmaalign();

	private native int wmabit();

	private native int wmabytespersec();

	private native int wmachannels();

	private native int wmasamplerate();

	private native int wmasamplesize();
	
	private Context context;
	private ListView channelsView;
	private Channel channel = null;
	private List<Channel> channels;
	private ImageView oldStatusView = null;
	private String url;
	private int out;
	private PlayTrack track;
	private boolean running = true;
	private boolean replay = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getApplicationContext();
		channelsView = (ListView) findViewById(R.id.channelList);
		dataPrepare.start();
		channelsView.setOnItemClickListener(new ChannelClickListener());
		initmms();
		decoderThread.start();
		playerThread.start();
	}
	
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
				case 0:
					prepareView();
					break;
				case 1:
					Toast.makeText(context, R.string.error_list, Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	private Thread dataPrepare = new Thread(new Runnable() {
		
		@Override
		public void run() {
			Message msg = new Message();
			try {
				channels = DataFetch.getChannels();
				msg.what = 0;
			} catch (Exception e) {
				msg.what = 1;
				Log.e("prepare", e.toString());
				e.printStackTrace();
			}
			handler.sendMessage(msg);
		}
	});

	private void prepareView() {
		List<HashMap<String, Object>> data = new ArrayList<HashMap<String,Object>>();
		for(Channel channel:channels){
			HashMap<String, Object> item = new HashMap<String, Object>();
			item.put("id", channel.getId());
			item.put("name", channel.getName());
			item.put("url", channel.getUrl());
			data.add(item);
		}
		SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.channel,
				new String[]{"name"}, new int[]{R.id.channel_name});
		channelsView.setAdapter(adapter);
	}
	
	private final class ChannelClickListener implements OnItemClickListener{

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
			Channel newChannel = channels.get(pos);
			ImageView statusView = (ImageView) view.findViewById(R.id.channel_status);
			Log.i("item click", "start");
			if( channel != null ){
				if( channel.getId() == newChannel.getId() ){
					channel.setStatus(false);
					Log.i("item click", "channel not null, same item");
					stop();
					//url = null;
					//track = null;
					statusView.setImageDrawable(getResources().getDrawable(R.drawable.play));
					channel = null;
				}
				else{
					Log.i("item click", "channel not null, other item");
					channel.setStatus(false);
					stop();
					//url = null;
					//track = null;
					oldStatusView.setImageDrawable(getResources().getDrawable(R.drawable.play));
					channel = newChannel;
					channel.setStatus(true);
					oldStatusView = statusView;
					play( channel.getUrl() );
					//track = new MMSTrack(channel.getUrl());
					statusView.setImageDrawable(getResources().getDrawable(R.drawable.stop));
				}
			}
			else{
				Log.i("item click", "channel null");
				channel = newChannel;
				oldStatusView = statusView;
				channel.setStatus(true);
				play( channel.getUrl() );
				//track = new MMSTrack(channel.getUrl());
				statusView.setImageDrawable(getResources().getDrawable(R.drawable.stop));
			}
			Log.i("item click", "end");
			//Toast.makeText(context, newChannel.getName(), Toast.LENGTH_SHORT).show();
		}
	}
	
	private void stop(){
		replay = false;
		out = 0;
		url = null;
		if( track != null )
			track.release();
	}
	
	private void play(String url) {
		this.url = "file://"+url;
		replay = true;
	}
	
	private Thread decoderThread = new Thread(new Runnable() {
		
		@Override
		public void run() {
			while(running){
				while(replay){
					if( url == null ) continue;
					//initmms();
					int i = openmms( url );
					Log.i( "decoder", "-----play open mms ----");
					if (i != 0) continue;
					//Log.i( "decoder", "-----play open mms ok ----");
					track = new PlayTrack(wmasamplerate(), wmachannels(), wmabit());
					track.init();
					i = wmaDecoder();
					out = 1;
					//Log.i( "replay", "-----play init play ok ----");
					while (out!=0) {
						if (i <= 0) out = 0;
						try {
							i = wmaDecoder();
						} catch (Exception localException) {
							localException.printStackTrace();
							Log.e("decoder", localException.getMessage());
							break;
						}
						//Log.d("decoder","decode");
					}
					//Log.d("decoder", String.valueOf(replay));
				}
				//Log.d("decoder", "end");
				if( track != null ){
					Log.d("decoder", "release");
					track.release();
					track = null;
				}
			}
		}
	});
	
	private Thread playerThread = new Thread(new Runnable() {
		
		@Override
		public void run() {
			while(running){
				while(replay){
					while( out != 0 ){
						if( track == null ) continue;
//						if (out == 0) {
//							track.release();
//							closemms();
//							break;
//						}
						byte[] arrayOfByte = getmmsBytes();
						Log.v("player", "bytes.length=" + arrayOfByte.length);
						track.playAudioTrack( arrayOfByte.clone(), 0, arrayOfByte.length );
					}
					//Log.d("play", "replay");
				}
			}
		}
	});

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		running = false;
		if(track != null)
			track.release();
		closemms();
		Log.d("main", "destroy");
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

}
