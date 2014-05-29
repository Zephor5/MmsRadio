package com.weiny;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import android.util.Log;

public class DataFetch {
	
	public static List<Channel> getChannels() throws Exception{
		String path = "http://www.zephor.cn/channel_list.json";
		URL url = new URL(path);
		Log.i("getChannels", "start");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		Log.i("getChannels", "open");
		conn.setConnectTimeout(3000);
		conn.setRequestMethod("GET");
		Log.i("getChannels", "set");
		int code;
		try{
			code = conn.getResponseCode();
		}
		catch (Exception e) {
			Log.e("getChannels", "res");
			e.printStackTrace();
			code = 0;
		}
		Log.i("getChannels", String.valueOf(code));
		if( code == 200 ){
			InputStream inStream = conn.getInputStream();
			return parseJSON( inStream );
		}
		throw new Exception("connect error");
	}

	private static List<Channel> parseJSON(InputStream inStream) throws Exception {
		List<Channel> channels = new ArrayList<Channel>();
		byte[] data = StreamTool.read(inStream);
		String json = new String(data);
		Log.i("dataFetch", json);
		JSONArray array = new JSONArray(json);
		for( int i = 0; i<array.length(); i++ ){
			JSONArray channelArr = array.getJSONArray(i);
			Channel channel = new Channel(i+1, channelArr.getString(0), channelArr.getString(1));
			channels.add(channel);
		}
		return channels;
	}

}
