/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
*
* Copyright (c) 2010 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 2.1 of the License, or (at your option) any later
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

package org.bigbluebutton.modules.sharednotes.managers {
	import com.asfusion.mate.events.Dispatcher;
	
	import flash.media.Microphone;
	
	import org.bigbluebutton.common.LogUtil;
	import org.bigbluebutton.core.BBB;
	import org.bigbluebutton.core.managers.UserManager;
	import org.bigbluebutton.main.events.BBBEvent;
	import org.bigbluebutton.modules.sharednotes.events.SendPatchEvent;
	
	public class SharedNotesManager {		
		private var connectionManager:SharedNotesConnectionManager;
		private var attributes:Object;
		
		
		public function SharedNotesManager() {
			//Init SharedNotesService		
		}

		public function setModuleAttributes(attributes:Object):void {
			this.attributes = attributes;
			connectionManager = new SharedNotesConnectionManager(attributes.connection);
		}

		public function joinSharedNotes():void {
			connectionManager.join(attributes.uri + "/" + attributes.room);
		}

		public function patchDocument(e:SendPatchEvent):void {
			connectionManager.patchDocument(UserManager.getInstance().getConference().getMyUserId(), e.patch, e.beginIndex, e.endIndex);
		}

		public function getCurrentDocument():void {
			connectionManager.currentDocument(UserManager.getInstance().getConference().getMyExternalUserID());
		}
	}
}
