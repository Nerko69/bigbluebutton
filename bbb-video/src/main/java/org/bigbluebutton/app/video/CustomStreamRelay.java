/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bigbluebutton.app.video;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.mconf.red5.client.net.rtmp.ClientExceptionHandler;
import com.mconf.red5.client.net.rtmp.INetStreamEventHandler;
import com.mconf.red5.client.net.rtmp.RTMPClient;
import org.red5.io.utils.ObjectMap;
import com.mconf.red5.proxy.StreamingProxy;
import com.mconf.red5.server.api.event.IEvent;
import com.mconf.red5.server.api.event.IEventDispatcher;
import com.mconf.red5.server.api.service.IPendingServiceCall;
import com.mconf.red5.server.api.service.IPendingServiceCallback;
import com.mconf.red5.server.net.rtmp.event.IRTMPEvent;
import com.mconf.red5.server.net.rtmp.event.Notify;
import com.mconf.red5.server.net.rtmp.status.StatusCodes;
import com.mconf.red5.server.stream.message.RTMPMessage;
import org.red5.server.net.rtmp.event.Ping;
import com.mconf.red5.server.service.PendingCall;


/**
 * Relay a stream from one location to another via RTMP.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */


public class CustomStreamRelay {

	// our consumer
	private CustomRTMPClient client;

	// our publisher
	private StreamingProxy proxy;

	// task timer
	private Timer timer;

	
	private String sourceHost;
	private String destHost;
	private String sourceApp;
	private String destApp;
	private int sourcePort;
	private int destPort;
	private String sourceStreamName;
	private String destStreamName;
	private String publishMode;
	Map<String, Object> defParams;

	private boolean isDisconnecting;

	/**
	 * Creates a stream client to consume a stream from an end point and a proxy to relay the stream
	 * to another end point.
	 * 
	 * @param args application arguments
	 */


	public void setSourceHost(String sourceHost) {
		this.sourceHost = sourceHost;
	}

	public void setSourcePort(int sourcePort) {
		this.sourcePort = sourcePort;
	}

	public void setDestinationHost(String destHost) {
		this.destHost = destHost;
	}

	public void setDestinationPort(int destPort) {
		this.destPort = destPort;
	}

	public void setSourceApp(String sourceApp) {
		this.sourceApp = sourceApp;
	}

	public void setDestinationApp(String destApp) {
		this.destApp = destApp;
	}

	public void setSourceStreamName(String sourceStreamName) {
		this.sourceStreamName = sourceStreamName;
	}

	public void setDestinationStreamName(String destStreamName) {
		this.destStreamName = destStreamName;
	}

	public void setPublishMode(String publishMode) {
		this.publishMode = publishMode;
	}

	public void initRelay(String... args) {
		if (args == null || args.length < 7) {
			System.out
					.println("Not enough args supplied. Usage: <source uri> <source app> <source stream name> <destination uri> <destination app> <destination stream name> <publish mode>");
		}
		else {
			sourceHost = args[0];
			destHost = args[3];
			sourceApp = args[1];
			destApp = args[4];
			sourcePort = 1935;
			destPort = 1935;
			sourceStreamName = args[2];
			destStreamName = args[5];
			publishMode = args[6]; //live, record, or append
			
			int colonIdx = sourceHost.indexOf(':');
			if (colonIdx > 0) {
				sourcePort = Integer.valueOf(sourceHost.substring(colonIdx + 1));
				sourceHost = sourceHost.substring(0, colonIdx);
				System.out.printf("Source host: %s port: %d\n", sourceHost, sourcePort);
			}
			colonIdx = destHost.indexOf(':');
			if (colonIdx > 0) {
				destPort = Integer.valueOf(destHost.substring(colonIdx + 1));
				destHost = destHost.substring(0, colonIdx);
				System.out.printf("Destination host: %s port: %d\n", destHost, destPort);
			}
			

		}
	}

	public void stopRelay() {
		isDisconnecting = true;
		client.disconnect();
		proxy.stop();
	}

	private class CustomRTMPClient extends RTMPClient {
		@Override
		public void play(int streamId, String name, int start, int length) {
			System.out.println("play stream "+ streamId + ", name: " + name + ", start " + start + ", length " + length );
			if (conn != null) {
				// get the channel
				int channel = getChannelForStreamId(streamId);
				// send our requested buffer size
				ping(Ping.CLIENT_BUFFER, streamId, 2000);
				// send our request for a/v
				PendingCall receiveAudioCall = new PendingCall("receiveAudio");
				conn.invoke(receiveAudioCall, channel);
				PendingCall receiveVideoCall = new PendingCall("receiveVideo");
				conn.invoke(receiveVideoCall, channel);
				// call play
				Object[] params = new Object[1];
				params[0] = name;
//				params[1] = start;
//				params[2] = length;
				PendingCall pendingCall = new PendingCall("play", params);
				conn.invoke(pendingCall, channel);
			} else {
				System.out.println("Connection was null ?");
			}

		}
	}

	public void startRelay() {
			
			isDisconnecting = false;
			// create a timer
			timer = new Timer();
			// create our publisher
			proxy = new StreamingProxy();
			proxy.setHost(destHost);
			proxy.setPort(destPort);
			proxy.setApp(destApp);
			proxy.init();
			proxy.setConnectionClosedHandler(new Runnable() {
				public void run() {
					System.out.println("Publish connection has been closed, source will be disconnected");
					client.disconnect();
				}
			});
			proxy.setExceptionHandler(new ClientExceptionHandler() {
				@Override
				public void handleException(Throwable throwable) {
					throwable.printStackTrace();
					proxy.stop();
				}
			});
			proxy.start(destStreamName, publishMode, new Object[] {});
			// wait for the publish state

			// Change to use signal or something more cleaner

			do {
				try {
					Thread.sleep(100L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} while (!proxy.isPublished());
			System.out.println("Publishing...");

			// create the consumer
			client = new CustomRTMPClient();
			client.setStreamEventDispatcher(new StreamEventDispatcher());
			client.setStreamEventHandler(new INetStreamEventHandler() {
				public void onStreamEvent(Notify notify) {
					System.out.printf("AQUIonStreamEvent: %s\n", notify);
					ObjectMap<?, ?> map = (ObjectMap<?, ?>) notify.getCall().getArguments()[0];
					String code = (String) map.get("code");
					System.out.printf("<:%s\n", code);
					if (StatusCodes.NS_PLAY_STREAMNOTFOUND.equals(code)) {
						System.out.println("Requested stream was not found");
						isDisconnecting = true;
						client.disconnect();
					} else if (StatusCodes.NS_PLAY_UNPUBLISHNOTIFY.equals(code) || StatusCodes.NS_PLAY_COMPLETE.equals(code)) {
						System.out.println("Source has stopped publishing or play is complete");
						isDisconnecting = true;
						client.disconnect();
					}
				}
			});
			client.setConnectionClosedHandler(new Runnable() {
				public void run() {
					System.out.println("Source connection has been closed");
					//System.exit(2);
					if(isDisconnecting) {
						System.out.println("Proxy will be stopped");
						client.disconnect();
						proxy.stop();
					} else {
						System.out.println("Reconnecting client...");
						client.connect(sourceHost, sourcePort, defParams, new ClientConnectCallback());
					}
				}
			});
			client.setExceptionHandler(new ClientExceptionHandler() {
				@Override
				public void handleException(Throwable throwable) {
					throwable.printStackTrace();
					//System.exit(1);
					client.disconnect();
					proxy.stop();
				}
			});			
			// connect the consumer
			defParams = client.makeDefaultConnectionParams(sourceHost, sourcePort, sourceApp);
			// add pageurl and swfurl
			defParams.put("pageUrl", "");
			defParams.put("swfUrl", "app:/Red5-StreamRelay.swf");
			// indicate for the handshake to generate swf verification data
			client.setSwfVerification(true);
			// connect the client
			System.out.println("startRelay:: ProxyRelay status is running: " + proxy.isRunning());
			client.connect(sourceHost, sourcePort, defParams, new ClientConnectCallback());
	}

	private final class ClientConnectCallback implements IPendingServiceCallback{
		public void resultReceived(IPendingServiceCall call) {
			System.out.println("connectCallback");
			ObjectMap<?, ?> map = (ObjectMap<?, ?>) call.getResult();
			String code = (String) map.get("code");
			if ("NetConnection.Connect.Rejected".equals(code)) {
				System.out.printf("Rejected: %s\n", map.get("description"));
				client.disconnect();
				proxy.stop();
			} else if ("NetConnection.Connect.Success".equals(code)) {
				// 1. Wait for onBWDone
				timer.schedule(new BandwidthStatusTask(), 2000L);
			} else {
				System.out.printf("Unhandled response code: %s\n", code);
			}
		}
	}

	/**
	 * Dispatches consumer events.
	 */
	private final class StreamEventDispatcher implements IEventDispatcher {

		public void dispatchEvent(IEvent event) {
			System.out.println("ClientStream.dispachEvent()" + event.toString());
			System.out.println("dispatchEvent:: ProxyRelay status is running: " + proxy.isRunning());
			try {
				proxy.pushMessage(null, RTMPMessage.build((IRTMPEvent) event));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Handles result from subscribe call.
	 */
	private final class SubscribeStreamCallBack implements IPendingServiceCallback {

		public void resultReceived(IPendingServiceCall call) {
			System.out.println("SubscirbeStreamCallBack::resultReceived: " + call);
		}

	}	
	
	/**
	 * Creates a "stream" via playback, this is the source stream.
	 */
	private final class CreateStreamCallback implements IPendingServiceCallback {

		public void resultReceived(IPendingServiceCall call) {
			System.out.println("CreateStreamCallBack::resultReceived: " + call);
			int streamId = (Integer) call.getResult();
			System.out.println("stream id: " + streamId);
			// send our buffer size request
			if (sourceStreamName.endsWith(".flv") || sourceStreamName.endsWith(".f4v") || sourceStreamName.endsWith(".mp4")) {
				System.out.println("THIS IS WRONG");
				System.out.println("play stream name " + sourceStreamName + " start 0 lenght -1");
				client.play(streamId, sourceStreamName, 0, -1);
			} else {
				System.out.println("play stream name " + sourceStreamName + " start -1 lenght 0");
				client.play(streamId, sourceStreamName, -1, 0);
			}
		}

	}

	/**
	 * Continues to check for onBWDone
	 */
	private final class BandwidthStatusTask extends TimerTask {

		@Override
		public void run() {
			// check for onBWDone
			System.out.println("Bandwidth check done: " + client.isBandwidthCheckDone());
			// cancel this task
			this.cancel();
			// create a task to wait for subscribed
			timer.schedule(new PlayStatusTask(), 1000L);
			// 2. send FCSubscribe
			client.subscribe(new SubscribeStreamCallBack(), new Object[] { sourceStreamName });
		}

	}

	private final class PlayStatusTask extends TimerTask {

		@Override
		public void run() {
			// checking subscribed
			System.out.println("Subscribed: " + client.isSubscribed());
			// cancel this task
			this.cancel();
			// 3. create stream
			client.createStream(new CreateStreamCallback());
		}

	}

}