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
package org.bigbluebutton.freeswitch.voice.freeswitch;

import java.io.File;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.bigbluebutton.freeswitch.voice.freeswitch.actions.BroadcastConferenceCommand;
import org.bigbluebutton.freeswitch.voice.freeswitch.actions.CancelDialCommand;
import org.bigbluebutton.freeswitch.voice.freeswitch.actions.DialCommand;
import org.bigbluebutton.freeswitch.voice.freeswitch.actions.EjectAllUsersCommand;
import org.bigbluebutton.freeswitch.voice.freeswitch.actions.EjectUserCommand;
import org.bigbluebutton.freeswitch.voice.freeswitch.actions.FreeswitchCommand;
import org.bigbluebutton.freeswitch.voice.freeswitch.actions.MuteUserCommand;
import org.bigbluebutton.freeswitch.voice.freeswitch.actions.GetAllUsersCommand;
import org.bigbluebutton.freeswitch.voice.freeswitch.actions.RecordConferenceCommand;
import org.bigbluebutton.freeswitch.voice.freeswitch.actions.SendDtmfCommand;

public class FreeswitchApplication {
	
	private static final int SENDERTHREADS = 1;
	private static final Executor msgSenderExec = Executors.newFixedThreadPool(SENDERTHREADS);
	private static final Executor runExec = Executors.newFixedThreadPool(SENDERTHREADS);
	private BlockingQueue<FreeswitchCommand> messages = new LinkedBlockingQueue<FreeswitchCommand>();
		
	private final ConnectionManager manager;
	 
	private final String USER = "0"; /* not used for now */
	  
	private volatile boolean sendMessages = false;
	  
	public FreeswitchApplication(ConnectionManager manager) {
		this.manager = manager;
	}
	  
	  private void queueMessage(FreeswitchCommand command) {
	  	try {
				messages.offer(command, 5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	  }
	    
	  public void getAllUsers(String voiceConfId) {    
	  	GetAllUsersCommand prc = new GetAllUsersCommand(voiceConfId, USER);
	   	queueMessage(prc);
	  }
	
	  public void muteUser(String voiceConfId, String voiceUserId, Boolean mute) {
	    MuteUserCommand mpc = new MuteUserCommand(voiceConfId, voiceUserId, mute, USER);
	    queueMessage(mpc);
	  }
	
	  public void eject(String voiceConfId, String voiceUserId) {
	    EjectUserCommand mpc = new EjectUserCommand(voiceConfId, voiceUserId, USER);       
	    queueMessage(mpc);
	  }
	
	  public void ejectAll(String voiceConfId) {
	    EjectAllUsersCommand mpc = new EjectAllUsersCommand(voiceConfId, USER);
	    queueMessage(mpc);
	  }
	    
	  private Long genTimestamp() {
	  	return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
	  }
	    
	  public void startRecording(String voiceConfId, String meetingid){
	  	String RECORD_DIR = "/var/freeswitch/meetings";        
	   	String voicePath = RECORD_DIR + File.separatorChar + meetingid + "-" + genTimestamp() + ".wav";
	    	
	   	RecordConferenceCommand rcc = new RecordConferenceCommand(voiceConfId, USER, true, voicePath);
	   	queueMessage(rcc);
	  }
	  
	  public void stopRecording(String voiceConfId, String meetingid, String voicePath){		    	
		   	RecordConferenceCommand rcc = new RecordConferenceCommand(voiceConfId, USER, false, voicePath);
		   	queueMessage(rcc);
		  }

		public void dial(String room, String participant, Map<String, String> options, Map<String, String> params) {
			DialCommand command = new DialCommand(room, participant, options, params, USER);
			System.out.println("SENDING DIAL destination=[" + command.getDestination() + "]");
			queueMessage(command);
		}

		public void cancelDial(String room, String uuid) {
			CancelDialCommand command = new CancelDialCommand(room, uuid, USER);
			queueMessage(command);
		}

		public void sendDtmf(String room, String uuid, String dtmfDigit) {
			SendDtmfCommand command = new SendDtmfCommand(room, uuid, dtmfDigit, USER);
			queueMessage(command);
		}
	
		private void sendMessageToFreeswitch(final FreeswitchCommand command) {
			Runnable task = new Runnable() {
				public void run() {
					if (command instanceof GetAllUsersCommand) {
						GetAllUsersCommand cmd = (GetAllUsersCommand) command;
						System.out.println("Sending PopulateRoomCommand for conference = [" + cmd.getRoom() + "]");
						manager.getUsers(cmd);
					} else if (command instanceof MuteUserCommand) {
						MuteUserCommand cmd = (MuteUserCommand) command;
						System.out.println("Sending MuteParticipantCommand for conference = [" + cmd.getRoom() + "]");
						System.out.println("Sending MuteParticipantCommand for conference = [" + cmd.getRoom() + "]");
						manager.mute(cmd);
					} else if (command instanceof EjectUserCommand) {
						EjectUserCommand cmd = (EjectUserCommand) command;
						System.out.println("Sending EjectParticipantCommand for conference = [" + cmd.getRoom() + "]");
						manager.eject(cmd);
					} else if (command instanceof EjectAllUsersCommand) {
						EjectAllUsersCommand cmd = (EjectAllUsersCommand) command;
						System.out.println("Sending EjectAllUsersCommand for conference = [" + cmd.getRoom() + "]");
						manager.ejectAll(cmd);
					} else if (command instanceof RecordConferenceCommand) {
						manager.record((RecordConferenceCommand) command);
					} else if (command instanceof BroadcastConferenceCommand) {
						manager.broadcast((BroadcastConferenceCommand) command);
					} else if (command instanceof DialCommand) {
						manager.dial((DialCommand) command);
					} else if (command instanceof CancelDialCommand) {
						CancelDialCommand cmd = (CancelDialCommand) command;
						manager.cancelDial(cmd);
					} else if (command instanceof SendDtmfCommand) {
						SendDtmfCommand sdc = (SendDtmfCommand) command;
						manager.sendDtmf(sdc);
					}
				}
			};
			
			runExec.execute(task);	
		}
		
		public void start() {
			sendMessages = true;
			Runnable sender = new Runnable() {
				public void run() {
					while (sendMessages) {
						FreeswitchCommand message;
						try {
							message = messages.take();
							sendMessageToFreeswitch(message);	
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
										
					}
				}
			};
			msgSenderExec.execute(sender);		
		}
		
		public void stop() {
			sendMessages = false;
		}

}
