package org.bigbluebutton.modules.sharednotes.views
{
	import flash.display.Sprite;
	import flash.events.MouseEvent;

	import mx.controls.Alert;
	import mx.events.CloseEvent;

	import org.bigbluebutton.core.UsersUtil;
	import org.bigbluebutton.main.views.MainCanvas;
	import org.bigbluebutton.modules.sharednotes.events.SharedNotesEvent;
	import org.bigbluebutton.util.i18n.ResourceUtil;

	public class AdditionalSharedNotesWindow extends SharedNotesWindow
	{
		public function AdditionalSharedNotesWindow(n:String) {
			super();

			trace("AdditionalSharedNotesWindow: in-constructor additional notes " + n);
			_notesId = n;

			showCloseButton = UsersUtil.amIModerator();
			width = 240;
			height = 240;

			closeBtn.addEventListener(MouseEvent.CLICK, onCloseBtnClick);
		}

		override public function onCreationComplete():void {
			super.onCreationComplete();

			trace("AdditionalSharedNotesWindow: [2] in-constructor additional notes " + noteId);

			btnNew.visible = btnNew.includeInLayout = false;
		}

		private function onCloseBtnClick(e:MouseEvent):void {
			var alert:Alert = Alert.show(
					ResourceUtil.getInstance().getString('bbb.sharedNotes.additionalNotes.closeWarning.message'),
					ResourceUtil.getInstance().getString('bbb.sharedNotes.additionalNotes.closeWarning.title'),
					Alert.YES | Alert.NO, parent as Sprite, alertClose, null, Alert.YES);
			e.stopPropagation();
		}

		private function alertClose(e:CloseEvent):void {
			if (e.detail == Alert.YES) {
				showCloseButton = false;

				trace("AdditionalSharedNotesWindow: requesting to destroy notes " + noteId);
				var destroyNotesEvent:SharedNotesEvent = new SharedNotesEvent(SharedNotesEvent.DESTROY_ADDITIONAL_NOTES_REQUEST_EVENT);
				destroyNotesEvent.payload.notesId = _notesId;
				_dispatcher.dispatchEvent(destroyNotesEvent);
			}
		}

		override public function getPrefferedPosition():String {
			return MainCanvas.POPUP;
		}

		override protected function updateTitle():void {
			title = ResourceUtil.getInstance().getString('bbb.sharedNotes.title') + " " + noteId;
		}
	}
}
