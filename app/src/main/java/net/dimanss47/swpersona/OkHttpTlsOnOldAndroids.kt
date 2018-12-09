package net.dimanss47.swpersona

import android.os.Build
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.*


class TlsSocketFactory(val base: SSLSocketFactory) : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String> = base.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = base.supportedCipherSuites
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket? =
        patch(base.createSocket(s, host, port, autoClose))
    override fun createSocket(host: String, port: Int): Socket? = patch(base.createSocket(host, port))
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket? =
        patch(base.createSocket(host, port, localHost, localPort))
    override fun createSocket(host: InetAddress, port: Int): Socket? = patch(base.createSocket(host, port))

    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket? =
        patch(base.createSocket(address, port, localAddress, localPort))

    private fun patch(s: Socket): Socket {
        if (s is SSLSocket) {
            s.enabledProtocols = if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                TLS_V10
            } else {
                TLS_V12
            }
        }
        return s
    }

    companion object {
        private val TLS_V10 = arrayOf("TLSv1")
        private val TLS_V12 = arrayOf("TLSv1.2")
    }
}
