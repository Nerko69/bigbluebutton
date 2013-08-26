package org.bigbluebutton.modules.sharednotes
{
	import org.bigbluebutton.core.BBB;

	public class SharedNotesOptions
	{
		[Bindable]
		public var refreshDelay:int = 500;
		
		[Bindable]
		public var position:String = "bottom-left";
		
		[Bindable]
		public var autoStart:Boolean = false;
		
		[Bindable]
		public var showButton:Boolean = false;

		
		public function SharedNotesOptions()
		{
			var vxml:XML = BBB.getConfigForModule("SharedNotesModule");
			if (vxml != null) {
				if (vxml.@refreshDelay != undefined) {
					refreshDelay = Number(vxml.@refreshDelay);
				}
				if (vxml.@position != undefined) {
					position = vxml.@position.toString();
				}
				if (vxml.@autoStart != undefined) {
					autoStart = (vxml.@autoStart.toString().toUpperCase() == "TRUE") ? true : false;
				}
				if (vxml.@showButton != undefined) {
					showButton = (vxml.@showButton.toString().toUpperCase() == "TRUE") ? true : false;
				}
			}
		}
	}
}
