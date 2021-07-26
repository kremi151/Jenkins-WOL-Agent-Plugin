package lu.kremi151.jenkins.wolagent;

import lu.kremi151.jenkins.wolagent.util.WakeOnLAN;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WakeOnLANTest {

    @Test
    public void testMagicPacket() throws IOException {
        String broadcastIP = "123.234.123.0";
        String macAddress = "DE-AD-BE-EF-12-34";

        final List<DatagramSocket> createdSockets = new ArrayList<>();

        WakeOnLAN wakeOnLAN = new WakeOnLAN(() -> {
            DatagramSocket socket = Mockito.mock(DatagramSocket.class);
            createdSockets.add(socket);
            return socket;
        });
        wakeOnLAN.sendMagicPacket(broadcastIP, macAddress);

        assertEquals(1, createdSockets.size(), "Only one socket should have been created");

        DatagramSocket socket = createdSockets.get(0);

        ArgumentCaptor<DatagramPacket> packetCaptor = ArgumentCaptor.forClass(DatagramPacket.class);
        Mockito.verify(socket).send(packetCaptor.capture());

        DatagramPacket packet = packetCaptor.getValue();

        assertEquals(broadcastIP, packet.getAddress().getHostAddress(), "Packet should be sent to correct broadcast IP address");
        assertEquals(9, packet.getPort(), "Packet should be sent to port 9");

        byte[] expectedPayload = new byte[102];
        byte[] macBytes = new byte[] { (byte) 222, (byte) 173, (byte) 190, (byte) 239, 18, 52 };

        for (int i = 0 ; i < 6 ; i++) {
            expectedPayload[i] = (byte) 0xff;
        }
        for (int i = 0 ; i < 16 ; i++) {
            System.arraycopy(macBytes, 0, expectedPayload, 6 + (i * macBytes.length), macBytes.length);
        }

        assertArrayEquals(expectedPayload, packet.getData(), "Magic bytes should match");
    }

}
