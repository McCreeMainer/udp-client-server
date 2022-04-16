package client

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.system.exitProcess
import kotlin.text.toByteArray

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

        val udpSocket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp()
            .connect(InetSocketAddress(serverHost as InetAddress, serverPort))

        for (i in 0 until 100) {
            val sendData = getRequest(i, "Amogus")
            udpSocket.outgoing.send(Datagram(ByteReadPacket(sendData), udpSocket.remoteAddress))

            println("SNMP send")

            val packet = withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
                udpSocket.incoming.receive().packet
            }
            proceedData(packet.readBytes())
        }
    }
}

fun getRequest(oidIndex: Int, communityName: String): ByteArray {
    val oidLength = 11
    val messageLength = oidLength + communityName.length + 24
    val pduLength = oidLength + 17
    val varBindLength = oidLength + 4
    val varBindListLength = oidLength + 6
    val valueType = 5
    val valueLength = 0
    val snmpInfo = byteArrayOf(48, messageLength.toByte(), 2, 1, 0, 4, communityName.length.toByte())
    val pduVarBind = byteArrayOf(160.toByte(), pduLength.toByte(), 2, 1, 1, 2, 1, 0, 2, 1, 0, 48, varBindListLength.toByte(), 48, varBindLength.toByte(), 6)
    val oid = byteArrayOf(43, 6, 1, 2, 1, 25, 6, 3, 1, 2, oidIndex.toByte())

    return snmpInfo +
            communityName.toByteArray() +
            pduVarBind +
            oid +
            byteArrayOf(valueType.toByte(), valueLength.toByte())
}

fun proceedData(data: ByteArray)
{
    if (data[5] != 4.toByte()) {
        println("Wrong community string type")
        return
    }
    val communityStringLength = data[6]
    val communityNameBytes = data.sliceArray(7 until 7 + communityStringLength)
    val communityName = String(communityNameBytes, Charsets.UTF_8)
    val oidLength = data[communityStringLength + 23]
    val valueType = data[communityStringLength + oidLength + 24]

    if (valueType != 4.toByte()) {
        println("Wrong value type")
        return
    }

    val valueLength = data[communityStringLength + oidLength + 25]
    val valueBytes = data.sliceArray(communityStringLength + oidLength + 26 until valueLength)
    val value = String(valueBytes, Charsets.UTF_8)

    println("\nSNMP reply:\nCommunity: $communityName\nValue: $value$\n")
}
