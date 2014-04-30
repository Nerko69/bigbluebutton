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

package org.bigbluebutton.conference;

import java.util.ArrayList;
import java.util.HashMap;

import org.bigbluebutton.conference.service.messaging.MessagingConstants;
import org.bigbluebutton.conference.service.messaging.MessagingService;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

import com.google.gson.Gson;


public class ParticipantUpdatingRoomListener implements IRoomListener{

	private static Logger log = Red5LoggerFactory.getLogger(ParticipantUpdatingRoomListener.class, "bigbluebutton");
	
	MessagingService messagingService;
	private Room room;
	
	public ParticipantUpdatingRoomListener(Room room, MessagingService messagingService) {
		this.room = room;
		this.messagingService=messagingService;
	}
	
	public String getName() {
		return "PARTICIPANT:UPDATE:ROOM";
	}
	
	public void participantStatusChange(User p, String status, Object value){
		if (messagingService != null) {
			HashMap<String,String> map= new HashMap<String, String>();
			map.put("meetingId", this.room.getName());
			map.put("messageId", MessagingConstants.USER_STATUS_CHANGE_EVENT);
			
			map.put("internalUserId", p.getInternalUserID());
			map.put("status", status);
			map.put("value", value.toString());
			
			Gson gson= new Gson();
			messagingService.send(MessagingConstants.PARTICIPANTS_CHANNEL, gson.toJson(map));
			log.debug("Publishing a status change in: " + this.room.getName());
		}
	}

	public void guestEntrance(User p) {
		if (messagingService != null) {
			HashMap<String,String> map= new HashMap<String, String>();
			map.put("meetingId", this.room.getName());
			map.put("messageId", MessagingConstants.GUEST_ASK_TO_ENTER_EVENT);
			map.put("internalUserId", p.getInternalUserID().toString());
			map.put("username", p.getName());
			Gson gson= new Gson();
			messagingService.send(MessagingConstants.PARTICIPANTS_CHANNEL, gson.toJson(map));
			log.debug("Publishing a guest Entrance: " + this.room.getName());
		}
	}

	public void guestWaitingForModerator(String userid, String userId_userName) {
		if (messagingService != null) {
			HashMap<String,String> map= new HashMap<String, String>();
			map.put("meetingId", this.room.getName());
			map.put("messageId", MessagingConstants.GUEST_ASK_TO_ENTER_EVENT);
			map.put("internalUserId", userid);
			map.put("userID_userName", userId_userName);
			Gson gson= new Gson();
			messagingService.send(MessagingConstants.GUESTS_WAITING_EVENT, gson.toJson(map));
		}
	}

	public void guestPolicyChanged(String guestPolicy) {
		if (messagingService != null) {
			HashMap<String,String> map= new HashMap<String, String>();
			map.put("meetingId", this.room.getName());
			map.put("messageId", MessagingConstants.NEW_GUEST_POLICY);
			map.put("guestPolicy", guestPolicy);
			Gson gson= new Gson();
			messagingService.send(MessagingConstants.PARTICIPANTS_CHANNEL, gson.toJson(map));
		}
	}

	public void guestResponse(User p, Boolean resp) {
		if (messagingService != null) {
			HashMap<String,String> map= new HashMap<String, String>();
			map.put("meetingId", this.room.getName());
			map.put("messageId", MessagingConstants.MODERATOR_RESPONSE_EVENT);
			map.put("internalUserId", p.getInternalUserID().toString());
			map.put("resp", resp.toString());
			Gson gson= new Gson();
			messagingService.send(MessagingConstants.PARTICIPANTS_CHANNEL, gson.toJson(map));
			log.debug("Publishing a guest Response: " + this.room.getName());
		}
	}

	public void participantRoleChange(User p, String role){
		if (messagingService != null) {
			HashMap<String,String> map= new HashMap<String, String>();
			map.put("meetingId", this.room.getName());
			map.put("messageId", MessagingConstants.USER_ROLE_CHANGE_EVENT);
			
			map.put("internalUserId", p.getInternalUserID());
			map.put("role", role);
			
			Gson gson= new Gson();
			messagingService.send(MessagingConstants.PARTICIPANTS_CHANNEL, gson.toJson(map));
			log.debug("Publishing a role change in: " + this.room.getName());
		}
	}
	
	public void participantJoined(User p) {
		if (messagingService != null) {
			HashMap<String,String> map= new HashMap<String, String>();
			map.put("meetingId", this.room.getName());
			map.put("messageId", MessagingConstants.USER_JOINED_EVENT);
			map.put("internalUserId", p.getInternalUserID());
			map.put("externalUserId", p.getExternalUserID());
			map.put("fullname", p.getName());
			map.put("role", p.getRole());
			map.put("guest", p.isGuest().toString());
			Gson gson= new Gson();
			messagingService.send(MessagingConstants.PARTICIPANTS_CHANNEL, gson.toJson(map));
			log.debug("Publishing message participant joined in " + this.room.getName());
		}
	}
	
	public void participantLeft(User p) {		
		if (messagingService != null) {
			HashMap<String,String> map= new HashMap<String, String>();
			map.put("meetingId", this.room.getName());
			map.put("messageId", MessagingConstants.USER_LEFT_EVENT);
			map.put("internalUserId", p.getInternalUserID());
			
			Gson gson= new Gson();
			messagingService.send(MessagingConstants.PARTICIPANTS_CHANNEL, gson.toJson(map));
			log.debug("Publishing message participant left in " + this.room.getName());
		}
	}

	public void assignPresenter(ArrayList<String> presenter) {
		// Do nothing.
	}
	
	public void endAndKickAll() {
		// no-op
	}
	
	public void recordingStatusChange(User p, Boolean recording){
		if (messagingService != null) {
			HashMap<String,String> map= new HashMap<String, String>();
			map.put("meetingId", this.room.getName());
			map.put("messageId", MessagingConstants.RECORD_STATUS_EVENT);

			map.put("internalUserId", p.getInternalUserID());
			map.put("value", recording.toString());

			Gson gson= new Gson();
			messagingService.send(MessagingConstants.PARTICIPANTS_CHANNEL, gson.toJson(map));
			log.debug("Publishing a recording status change in: " + this.room.getName());
		}
	}

}