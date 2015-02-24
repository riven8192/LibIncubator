		
if(!Element.prototype.prependChild) {
	Element.prototype.prependChild = function(child) {
		this.insertBefore(child, this.firstChild);
	};
}

if(!String.prototype.startsWith) {
	String.prototype.startsWith = function(sw) {
		return (sw.length <= this.length) && this.substring(0, sw.length) === sw;
	};
}








function d2h(d) {
    var h = d.toString(16);
	if(h.length < 2) {
		h = '0' + h;
	}
	return h;
}
function h2d(h) {
    return parseInt(h, 16);
}
function s2h(str) {
	if(typeof str !== 'string') {
		str = ''+str;
	}
    var hex = '';
    for (var i=0; i < str.length; i += 1) {
        hex += d2h(str.charCodeAt(i));
    }
    return hex;
}
function h2s(hex) {
	var str = '';
    for (var i=0; i < hex.length; i += 2) {
        str += String.fromCharCode( h2d( hex.substring(i,i+2 ) ) );
    }
    return str;
}




function Waiter(channel) {
	var self = this;
	
	var xhrListen = new XMLHttpRequest();
	
	this.channel      = channel;
	this.offMsgId     = 0;
	
	this.bulkMsgDelay = 100;
	this.lastMsgDelay = 1000;
	this.errorDelay   = 10000;
	
	
	
	this.onMessageReceived = function( msgId, line ) {
		// impl. by user
	};
	
	this.sendMessage = function( rcpt, token, msg, onSuccess, onFailure ) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function () {
			if (this.readyState === 0 || this.readyState === 4){
				if (this.readyState === 4 && this.status === 200){
					onSuccess(rcpt, msg, this.responseText);
				}
				else {
					onFailure(rcpt, msg);
				}
			}			
		};
		xhr.open('POST', '/channel/'+rcpt+'/'+token+'/'+msg, true);
		xhr.send();
	};
	

	this.requestOffset = function( onSuccess, onFailure ) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function () {
			if (this.readyState === 0 || this.readyState === 4){
				if (this.readyState === 4 && this.status === 200){
					onSuccess(parseInt(this.responseText));
				}
				else {
					onFailure();
				}
			}			
		};
		xhr.open('GET', '/offset/'+this.channel, true);
		xhr.send();
	};
	
	var xhrOpenSend = function() {
		xhrListen.open('GET', '/channel/'+self.channel+'/'+self.offMsgId, true);
		xhrListen.send();
	};
	
	this.listen = function() {
		xhrListen.onreadystatechange = function () {
			var preMsgId = self.offMsgId;
			
			if (this.readyState === 4 && this.status === 200){
				var lines = this.responseText.split("\n");
				for(var i=0; i<lines.length; i++) {
					self.onMessageReceived(self.offMsgId++, lines[i]);
				}
			}			
			
			if(this.readyState === 0 || this.readyState === 4) {
				var delay;
				if((self.offMsgId - preMsgId) > 1) {
					delay = self.bulkMsgDelay;				
				}
				else if(this.readyState === 4) {
					delay = self.lastMsgDelay;
				}
				else {
					delay = self.errorDelay;
				}
				
				setTimeout(function() {
					xhrOpenSend();
				}, delay);
			}
		};
		xhrOpenSend();
	};
}

function convertMsgToElem(msgBody, msgElem) {
	if(msgBody.startsWith('TXT_')) {
		msgBody = msgBody.substring(4);
		msgBody = h2s(msgBody);
		
		var textElem = document.createTextNode(msgBody);	
		msgElem.appendChild(textElem);
		msgElem.className += ' type_txt';
	}
	else if(msgBody.startsWith('URL_')) {
		msgBody = msgBody.substring(4);
		msgBody = h2s(msgBody);
		
		var textElem = document.createTextNode(msgBody);
		var linkElem = document.createElement('A');
		linkElem.target = "_blank";
		linkElem.href   = msgBody;
		linkElem.appendChild(textElem);
		
		msgElem.appendChild(linkElem);
		msgElem.className += ' type_url';
	}
	else if(msgBody.startsWith('IMG_')) {
		msgBody = msgBody.substring(4);
		msgBody = h2s(msgBody);
		
		var loadingElem = document.createTextNode('Bezig met laden van afbeelding...');
		
		var imgElem = document.createElement('IMG');
		imgElem.onload = function() {
			msgElem.removeChild(loadingElem);
			msgElem.appendChild(imgElem);
		};
		imgElem.width = 400;
		imgElem.src   = msgBody;
		
		msgElem.className += ' type_img';
		msgElem.appendChild(loadingElem);
	}
	else if(msgBody.startsWith('NEW_')) {
		msgBody = msgBody.substring(4);
		msgBody = h2s(msgBody);
		
		var loadingElem = document.createTextNode('Bezig met laden van afbeelding...');
		
		var imgElem = document.createElement('IMG');
		imgElem.onload = function() {
			msgElem.removeChild(loadingElem);
			msgElem.appendChild(imgElem);
		};
		imgElem.width = 400;
		imgElem.src   = msgBody;
		
		msgElem.className += ' type_channel';
		msgElem.appendChild(loadingElem);
	}
}
