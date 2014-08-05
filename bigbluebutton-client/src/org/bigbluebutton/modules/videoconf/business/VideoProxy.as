/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
* 
* Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 3.0 of the License, or (at your option) any later
* version.
* 
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
*
*/
package org.bigbluebutton.modules.videoconf.business
{
	import com.asfusion.mate.events.Dispatcher;	
	import flash.events.AsyncErrorEvent;
	import flash.events.IOErrorEvent;
	import flash.events.NetStatusEvent;
	import flash.events.SecurityErrorEvent;
	import flash.media.H264Level;
	import flash.media.H264Profile;
	import flash.media.H264VideoStreamSettings;
	import flash.net.NetConnection;
	import flash.net.NetStream;
	import flash.system.Capabilities;
	import flash.utils.Dictionary;

	import mx.collections.ArrayCollection;
	
	import org.bigbluebutton.common.LogUtil;
	import org.bigbluebutton.core.BBB;
	import org.bigbluebutton.core.managers.UserManager;
	import org.bigbluebutton.main.model.users.BBBUser;
	import org.bigbluebutton.main.model.users.events.StreamStartedEvent;
	import org.bigbluebutton.modules.videoconf.events.ConnectedEvent;
	import org.bigbluebutton.modules.videoconf.events.StartBroadcastEvent;
	import org.bigbluebutton.modules.videoconf.model.VideoConfOptions;
	import org.bigbluebutton.modules.videoconf.events.PlayConnectionReady;

	
	public class VideoProxy
	{		
		public var videoOptions:VideoConfOptions;
		
		// NetConnection used for stream publishing
		private var nc:NetConnection;
		// NetStream used for stream publishing
		private var ns:NetStream;
		private var _url:String;
		private var camerasPublishing:Object = new Object();
		private var connected:Boolean = false;

		// Dictionary<url,NetConnection> used for stream playing
		private var playConnectionDict:Dictionary;
		// Dictionary<url,int> used to keep track of how many streams use a URL
		private var playConnectionCountDict:Dictionary;
		// Dictionary<userID,streamNamePrefix> used for stream playing
		private var streamNamePrefixDict:Dictionary;
		// Dictionary<userID,url>
		private var userUrlDict:Dictionary;
    
		private function parseOptions():void {
			videoOptions = new VideoConfOptions();
			videoOptions.parseOptions();	
		}
		
		public function VideoProxy(url:String)
		{
      _url = url;
			parseOptions();			
			nc = new NetConnection();
			nc.client = this;
			nc.addEventListener(AsyncErrorEvent.ASYNC_ERROR, onAsyncError);
			nc.addEventListener(IOErrorEvent.IO_ERROR, onIOError);
			nc.addEventListener(NetStatusEvent.NET_STATUS, onNetStatus);
			nc.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onSecurityError);
			playConnectionDict = new Dictionary();
			playConnectionCountDict = new Dictionary();
			streamNamePrefixDict = new Dictionary();
			userUrlDict = new Dictionary();
		}
		
    public function connect():void {
    	nc.connect(_url);
		playConnectionDict[_url] = nc;
		playConnectionCountDict[_url] = 0;
    }
    
		private function onAsyncError(event:AsyncErrorEvent):void{
		}
		
		private function onIOError(event:NetStatusEvent):void{
		}
		
    private function onConnectedToVideoApp():void{
      var dispatcher:Dispatcher = new Dispatcher();
      dispatcher.dispatchEvent(new ConnectedEvent(ConnectedEvent.VIDEO_CONNECTED));
    }
    
		private function onNetStatus(event:NetStatusEvent):void{
			switch(event.info.code){
				case "NetConnection.Connect.Success":
					connected = true;
					//ns = new NetStream(nc);
          onConnectedToVideoApp();
					break;
        default:
					LogUtil.debug("[" + event.info.code + "] for [" + _url + "]");
					connected = false;
					break;
			}
		}
		
		private function onSecurityError(event:NetStatusEvent):void{
		}
		
		public function get publishConnection():NetConnection{
			return this.nc;
		}

		private function onPlayNetStatus(event:NetStatusEvent):void {
			switch(event.info.code){
				case "NetConnection.Connect.Success":
					var dispatcher:Dispatcher = new Dispatcher();
					dispatcher.dispatchEvent(new PlayConnectionReady(PlayConnectionReady.PLAY_CONNECTION_READY));
					break;
				default:
					LogUtil.debug("[" + event.info.code + "] for a play connection");
					break;
			}
		}

		public function createPlayConnectionFor(userID:String):void {
			LogUtil.debug("VideoProxy::createPlayConnectionFor:: Creating connection for stream from [" + userID + "]");
			// TODO: Ask LB for path to current user
			var connectionPath:String = "10.0.3.203/10.0.3.254/10.0.3.79";
			var serverIp:String = connectionPath.split("/")[0];
			var ipRegex:RegExp = /([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+)/;
			var newUrl:String = _url.replace(ipRegex, serverIp);

			// Store URL for this user
			userUrlDict[userID] = newUrl;

			var streamPrefix:String;
			if(connectionPath != serverIp) // More than one server -> has prefix
				streamPrefix = connectionPath.replace(serverIp + "/", "") + "/";
			else
				streamPrefix = "";
			// Set current user streamPrefix to use the current path
			streamNamePrefixDict[userID] = streamPrefix;

			// If connection with this URL does not exist
			if(!playConnectionDict[newUrl]){
				// Create new NetConnection and store it
				var connection:NetConnection = new NetConnection();
				connection.client = this;
				connection.addEventListener(AsyncErrorEvent.ASYNC_ERROR, onAsyncError);
				connection.addEventListener(IOErrorEvent.IO_ERROR, onIOError);
				connection.addEventListener(NetStatusEvent.NET_STATUS, onPlayNetStatus);
				connection.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onSecurityError);
				connection.connect(newUrl);
				// TODO change to trace
				LogUtil.debug("VideoProxy::createPlayConnectionFor:: Creating NetConnection for [" + newUrl + "]");
				playConnectionDict[newUrl] = connection;
				playConnectionCountDict[newUrl] = 0;
			}
			else {
				if(playConnectionDict[newUrl].connected) {
					// Connection is ready, send event
					var dispatcher:Dispatcher = new Dispatcher();
					dispatcher.dispatchEvent(new PlayConnectionReady(PlayConnectionReady.PLAY_CONNECTION_READY));
				}
				// TODO change to trace
				LogUtil.debug("VideoProxy::createPlayConnectionFor:: Found NetConnection for [" + newUrl + "]");
			}
		}

		public function playConnectionIsReadyFor(userID:String):Boolean {
			var userUrl:String = userUrlDict[userID];
			if(playConnectionDict[userUrl].connected)
				return true;
			return false;
		}

		public function getPlayConnectionFor(userID:String):NetConnection {
			var userUrl:String = userUrlDict[userID];
			playConnectionCountDict[userUrl] = playConnectionCountDict[userUrl] + 1;
			// TODO: change to trace
			LogUtil.debug("VideoProxy:: getPlayConnection:: URL: [" + userUrl + "], count: [" + playConnectionCountDict[userUrl] + "]");
			return playConnectionDict[userUrl];
		}

		public function getStreamNamePrefixFor(userID:String):String{
			// If does not exist
			if(!streamNamePrefixDict[userID]){
				// TODO: change LogUtil.debug(); to trace();
				LogUtil.debug("VideoProxy:: getStreamNamePrefixFor:: streamPrefix not found. NetConnection might not exist for stream from [" + userID + "]");
				return "";
			}
			else{
				return streamNamePrefixDict[userID];
			}
		}

		public function closePlayConnectionFor(userID:String):void {
			var userUrl:String = userUrlDict[userID];
			// Do not close publish connection, no matter what
			if(playConnectionDict[userUrl] == nc)
				return;
			if(userUrl != null) {
				var count:int = playConnectionCountDict[userUrl] - 1;
				// TODO: change to trace
				LogUtil.debug("VideoProxy:: closePlayConnectionFor:: userID: [" + userID + "], URL: [" + userUrl + "], new streamCount: [" + count + "]");
				playConnectionCountDict[userUrl] = count;
				if(count <= 0) {
					// No one else is using this NetConnection
					var connection:NetConnection = playConnectionDict[userUrl];
					if(connection != null) connection.close();
					delete playConnectionDict[userUrl];
					delete playConnectionCountDict[userUrl];
				}
			}
		}

		public function startPublishing(e:StartBroadcastEvent):void{
			var ns:NetStream = new NetStream(nc);
			ns.addEventListener( NetStatusEvent.NET_STATUS, onNetStatus );
			ns.addEventListener( IOErrorEvent.IO_ERROR, onIOError );
			ns.addEventListener( AsyncErrorEvent.ASYNC_ERROR, onAsyncError );
			ns.client = this;
			ns.attachCamera(e.camera);
//		Uncomment if you want to build support for H264. But you need at least FP 11. (ralam july 23, 2011)	
//			if (Capabilities.version.search("11,0") != -1) {
			if ((BBB.getFlashPlayerVersion() >= 11) && e.videoProfile.enableH264) {
//			if (BBB.getFlashPlayerVersion() >= 11) {
				LogUtil.info("Using H264 codec for video.");
				var h264:H264VideoStreamSettings = new H264VideoStreamSettings();
				var h264profile:String = H264Profile.MAIN;
				if (e.videoProfile.h264Profile != "main") {
					h264profile = H264Profile.BASELINE;
				}
				var h264Level:String = H264Level.LEVEL_4_1;
				switch (e.videoProfile.h264Level) {
					case "1": h264Level = H264Level.LEVEL_1; break;
					case "1.1": h264Level = H264Level.LEVEL_1_1; break;
					case "1.2": h264Level = H264Level.LEVEL_1_2; break;
					case "1.3": h264Level = H264Level.LEVEL_1_3; break;
					case "1b": h264Level = H264Level.LEVEL_1B; break;
					case "2": h264Level = H264Level.LEVEL_2; break;
					case "2.1": h264Level = H264Level.LEVEL_2_1; break;
					case "2.2": h264Level = H264Level.LEVEL_2_2; break;
					case "3": h264Level = H264Level.LEVEL_3; break;
					case "3.1": h264Level = H264Level.LEVEL_3_1; break;
					case "3.2": h264Level = H264Level.LEVEL_3_2; break;
					case "4": h264Level = H264Level.LEVEL_4; break;
					case "4.1": h264Level = H264Level.LEVEL_4_1; break;
					case "4.2": h264Level = H264Level.LEVEL_4_2; break;
					case "5": h264Level = H264Level.LEVEL_5; break;
					case "5.1": h264Level = H264Level.LEVEL_5_1; break;
				}
				
				LogUtil.info("Codec used: " + h264Level);
				
				h264.setProfileLevel(h264profile, h264Level);
				ns.videoStreamSettings = h264;
			}
			
			ns.publish(e.stream);
			camerasPublishing[e.stream] = ns;
		}
		
		public function stopBroadcasting(stream:String):void{
      trace("Closing netstream for webcam publishing");
      			if (camerasPublishing[stream] != null) {
	      			var ns:NetStream = camerasPublishing[stream];
				ns.attachCamera(null);
				ns.close();
				ns = null;
				delete camerasPublishing[stream];
			}	
		}

		public function stopAllBroadcasting():void {
			for each (var ns:NetStream in camerasPublishing)
			{
				ns.attachCamera(null);
				ns.close();
				ns = null;
			}
			camerasPublishing = new Object();
		}

		public function disconnect():void {
      trace("VideoProxy:: disconnecting from Video application");
      stopAllBroadcasting();
			// Close publish NetConnection
			if (nc != null) nc.close();
			// Close play NetConnections
			for (var k:Object in playConnectionDict) {
				var connection:NetConnection = playConnectionDict[k];
				connection.close();
			}
			// Reset dictionaries
			playConnectionDict = new Dictionary();
			playConnectionCountDict = new Dictionary();
			streamNamePrefixDict = new Dictionary();
			userUrlDict = new Dictionary();
		}
		
		public function onBWCheck(... rest):Number { 
			return 0; 
		} 
		
		public function onBWDone(... rest):void { 
			var p_bw:Number; 
			if (rest.length > 0) p_bw = rest[0]; 
			// your application should do something here 
			// when the bandwidth check is complete 
			trace("bandwidth = " + p_bw + " Kbps."); 
		}
		

	}
}
