package server

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    runBlocking {
        val serverHost = args.firstOrNull()
            ?.let { str ->
                InetAddress.getByName(str)
            }
            ?: {
                println("No address")
                exitProcess(0)
            }
        val serverIP = (serverHost as InetAddress).address

        val udpSocket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp()
            .bind(InetSocketAddress(serverPort))

        while (true) {
            val packet = withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
                udpSocket.incoming.receive().packet
            }

            val data = packet.readBytes(240)
            val options = packet.readBytes()

            data[0] = 2
            for (i in 0 until 4) {
                data[i + 16] = ipAddress[i]
                data[i + 20] = serverIP[i]
            }

            val leaseTime = byteArrayOf(51, 4) + ByteBuffer.allocate(Int.SIZE_BYTES)
                .also { it.putInt(86400) }.array()
                .let { it.sliceArray(it.size - 4 until it.size) }
            val messageType = byteArrayOf(
                53, 1, when (options[options.indexOf(53) + 2]) {
                    1.toByte() -> 2
                    3.toByte() -> 5
                    else -> throw IllegalStateException("Illegal message type")
                }
            )
            val serverIdentifier = byteArrayOf(54, 4) + serverIP
            val message = data + messageType + serverIdentifier + leaseTime + byteArrayOf(255.toByte())

            udpSocket.send(Datagram(ByteReadPacket(message), NetworkAddress(broadcastAddress, clientPort)))
        }
    }
}