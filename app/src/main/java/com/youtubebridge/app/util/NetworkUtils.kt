package com.youtubebridge.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.LinkProperties
import android.net.Network
import java.net.Inet4Address
import java.net.InetAddress

object NetworkUtils {

    /** Returns the phone's current IPv4 address on the active network (typically Wi-Fi), or null. */
    fun getLocalIpAddress(context: Context): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network: Network = cm.activeNetwork ?: return null
        val props: LinkProperties = cm.getLinkProperties(network) ?: return null

        val ipv4: LinkAddress? = props.linkAddresses.firstOrNull { it.address is Inet4Address }
        return ipv4?.address?.hostAddress
    }

    /**
     * Security rule: only accept clients whose IP is inside a private/LAN range
     * (RFC1918) so the server never answers requests coming from the public internet.
     */
    fun isLocalNetworkAddress(ip: String): Boolean {
        return try {
            val addr: InetAddress = InetAddress.getByName(ip)
            addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress
        } catch (e: Exception) {
            false
        }
    }
}
