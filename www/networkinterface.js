var networkinterface = function() {
};

networkinterface.getWiFiIPAddress = function( success, fail ) {
    cordova.exec( success, fail, "networkinterface", "getWiFiIPAddress", [] );
};

networkinterface.getCarrierIPAddress = function( success, fail ) {
    cordova.exec( success, fail, "networkinterface", "getCarrierIPAddress", [] );
};

networkinterface.getHttpProxyInformation = function( url, success, fail ) {
    cordova.exec( success, fail, "networkinterface", "getHttpProxyInformation", [url] );
};

// Android only
networkinterface.getNetworkBroadcastAddresses = function( success, fail ) {
    cordova.exec( success, fail, "networkinterface", "getNetworkBroadcastAddresses", [] );
};

networkinterface.sendBroadcastMessage = function( broadcastAddresses, success, fail ) {
    cordova.exec( success, fail, "networkinterface", "sendBroadcastMessage", broadcastAddresses );
};

module.exports = networkinterface;
