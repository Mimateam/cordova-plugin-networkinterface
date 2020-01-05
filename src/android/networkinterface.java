package com.albahra.plugin.networkinterface;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Proxy.Type;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;
import java.util.logging.*;

import java.io.IOException;

public class networkinterface extends CordovaPlugin {
	public static final String GET__WIFI_IP_ADDRESS="getWiFiIPAddress";
	public static final String GET_CARRIER_IP_ADDRESS="getCarrierIPAddress";
	public static final String GET_HTTP_PROXY_INFORMATION="getHttpProxyInformation";
	public static final String GET_NETWORK_BROADCAST_ADDRESSES="getNetworkBroadcastAddresses";
	public static final String GET_SEND_BROADCAST_MESSAGE="sendBroadcastMessage";
	private static final String TAG = "cordova-plugin-networkinterface";

	

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		try {
			if (GET__WIFI_IP_ADDRESS.equals(action)) {
				return extractIpInfo(getWiFiIPAddress(), callbackContext);
			} else if (GET_CARRIER_IP_ADDRESS.equals(action)) {
				return extractIpInfo(getCarrierIPAddress(), callbackContext);
			} else if(GET_HTTP_PROXY_INFORMATION.equals(action)) {
				return getHttpProxyInformation(args.getString(0), callbackContext);
			} else if(GET_NETWORK_BROADCAST_ADDRESSES.equals(action)) {
				return getNetworkBroadcastAddresses(callbackContext);
			} else if(GET_SEND_BROADCAST_MESSAGE.equals(action)) {
				return sendBroadcastMessage(args, callbackContext);
			}
			callbackContext.error("Error no such method '" + action + "'");
			return false;
		} catch(Exception e) {
			callbackContext.error("Error while calling ''" + action + "' '" + e.getMessage());
			return false;
		}
	}

	private JSONObject createProxyInformation (Proxy.Type proxyType, String host, String port) throws JSONException {
		JSONObject proxyInformation = new JSONObject();
		proxyInformation.put("type", proxyType.toString());
		proxyInformation.put("host", host);
		proxyInformation.put("port", port);
		return proxyInformation;
	}

	private boolean getHttpProxyInformation(String url, CallbackContext callbackContext) throws JSONException, URISyntaxException {
		JSONArray proxiesInformation = new JSONArray();
		ProxySelector defaultProxySelector = ProxySelector.getDefault();
		
		if(defaultProxySelector != null){
			List<java.net.Proxy> proxyList = defaultProxySelector.select(new URI(url));
			for(java.net.Proxy proxy: proxyList){
				if (java.net.Proxy.Type.DIRECT.equals(proxy.type())) {
                	break;
            	}
				InetSocketAddress proxyAddress = (InetSocketAddress)proxy.address();
				if(proxyAddress != null){
					proxiesInformation.put(createProxyInformation(proxy.type(), proxyAddress.getHostString(), String.valueOf(proxyAddress.getPort())));
				}
			}
		}

		if(proxiesInformation.length() < 1){
			proxiesInformation.put(createProxyInformation(Proxy.Type.DIRECT, "none", "none"));
		}

		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, proxiesInformation));
		return true;
	}

	private boolean extractIpInfo(String[] ipInfo, CallbackContext callbackContext) throws JSONException {
		String ip = ipInfo[0];
		String subnet = ipInfo[1];
		String fail = "0.0.0.0";
		if (ip == null || ip.equals(fail)) {
			callbackContext.error("No valid IP address identified");
			return false;
		}

		Map<String,String> ipInformation = new HashMap<String,String>();
		ipInformation.put("ip", ip);
		ipInformation.put("subnet", subnet);

		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, new JSONObject(ipInformation)));
		return true;
	}

	private String[] getWiFiIPAddress() {
		WifiManager wifiManager = (WifiManager) cordova.getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();

		String ipString = String.format(
			"%d.%d.%d.%d",
			(ip & 0xff),
			(ip >> 8 & 0xff),
			(ip >> 16 & 0xff),
			(ip >> 24 & 0xff)
			);

		String subnet = "";
		try {
			InetAddress inetAddress = InetAddress.getByName(ipString);
			subnet = getIPv4Subnet(inetAddress);
		} catch (Exception e) {
		}

		return new String[]{ ipString, subnet };
	}

	private String[] getCarrierIPAddress() {
	  try {
	    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	       NetworkInterface intf = (NetworkInterface) en.nextElement();
	       //Log.e(TAG, "Interface: " + intf.toString() + " name: " + intf.getName() + " display nane: " + intf.getDisplayName() );
	       for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	          InetAddress inetAddress = enumIpAddr.nextElement();
			   if (!inetAddress.isLoopbackAddress() && (!intf.getName().equals("wlan0")) && inetAddress instanceof Inet4Address) {
				   String ipaddress = inetAddress.getHostAddress().toString();
				   String subnet = getIPv4Subnet(inetAddress);
				   return new String[]{ ipaddress, subnet };
	          }
	       }
	    }
	  } catch (SocketException ex) {
	     Log.e(TAG, "Exception in Get IP Address: " + ex.toString());
	  }
	  return new String[]{ null, null };
	}

	public static String getIPv4Subnet(InetAddress inetAddress) {
		try {
			NetworkInterface ni = NetworkInterface.getByInetAddress(inetAddress);
			List<InterfaceAddress> intAddrs =  ni.getInterfaceAddresses();
			for (InterfaceAddress ia : intAddrs) {
				if (!ia.getAddress().isLoopbackAddress() && ia.getAddress() instanceof Inet4Address) {
					return getIPv4SubnetFromNetPrefixLength(ia.getNetworkPrefixLength()).getHostAddress().toString();
				}
			}
		} catch (Exception e) {
		}
		return "";
	}

	public static InetAddress getIPv4SubnetFromNetPrefixLength(int netPrefixLength) {
		try {
			int shift = (1<<31);
			for (int i=netPrefixLength-1; i>0; i--) {
				shift = (shift >> 1);
			}
			String subnet = Integer.toString((shift >> 24) & 255) + "." + Integer.toString((shift >> 16) & 255) + "." + Integer.toString((shift >> 8) & 255) + "." + Integer.toString(shift & 255);
			return InetAddress.getByName(subnet);
		}
		catch(Exception e){
		}
		return null;
	}

	private static boolean getNetworkBroadcastAddresses(CallbackContext callbackContext) throws SocketException {
		try {
			ArrayList<String> broadcastAddressesList = new ArrayList<String>();

			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) 
			{
				NetworkInterface networkInterface = interfaces.nextElement();

				if (networkInterface.isLoopback() || !networkInterface.isUp())
					continue;	// Do not want to use the loopback interface.

				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) 
				{
						InetAddress broadcast = interfaceAddress.getBroadcast();
						if (broadcast == null)
							continue;
						
						broadcastAddressesList.add(broadcast.getHostAddress());
				}
			}

			String[] broadcastAddresses = broadcastAddressesList.toArray(new String[0]);

			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, new JSONArray(Arrays.asList(broadcastAddresses))));
			return true;
		}
		catch (SocketException e) {
			callbackContext.error("SocketException: " + e.getMessage());
			return false;
		}
	}

	/*
	*	broadcastAddresses: [0] message, [1] port, [2,...,n] Broadcast IPs
	*/
	private boolean sendBroadcastMessage(JSONArray broadcastAddresses, CallbackContext callbackContext) throws JSONException, UnknownHostException, SocketException, IOException {
		try {
			InetAddress broadcast = null;
			DatagramSocket socket = null;

			byte[] buffer = ((String)broadcastAddresses.get(0)).getBytes();
			int port = Integer.parseInt((String)broadcastAddresses.get(1));

			if (port >= 0 && port <= 65535) {
				for (int i = 2 ; i < broadcastAddresses.length(); i++) {
					System.out.println("--- " + (String)broadcastAddresses.get(i));

					broadcast = InetAddress.getByName((String)broadcastAddresses.get(i));

					//byte[] buffer = new byte[1];

					socket = new DatagramSocket();
					socket.setBroadcast(true);

					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcast, 20020);

					socket.send(packet);
					socket.close();
				}

				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
				return true;
			}
			else {
				callbackContext.error("Port Error: " + Integer.toString(port));
				return false;
			}
		}
		catch (JSONException e) {
			callbackContext.error("JSONException: " + e.getMessage());
			return false;
		}
		catch (UnknownHostException e) {
			callbackContext.error("UnknownHostException: " + e.getMessage());
			return false;
		}
		catch (SocketException e) {
			callbackContext.error("SocketException: " + e.getMessage());
			return false;
		}
		catch (IOException e) {
			callbackContext.error("IOException: " + e.getMessage());
			return false;
		}
	}
}
