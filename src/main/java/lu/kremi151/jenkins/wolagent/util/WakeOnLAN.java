/*
 * Copyright 2019 Michel Kremer (kremi151)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lu.kremi151.jenkins.wolagent.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for sending Wake-on-LAN magic packets.
 */
public final class WakeOnLAN {

    private static final Logger LOGGER = Logger.getLogger(WakeOnLAN.class.getName());

    private static final int PORT = 9;

    private static final int NUM_LEADING_MAGIC_BYTES = 6;
    private static final int NUM_MAC_MAGIC_BYTES = 16;

    private static final int NUM_MAC_ADDRESS_PARTS = 6;

    private static final int RADIX_HEXADECIMAL = 16;

    private static final byte VAL_LEADING_MAGIC_BYTE = (byte) 0xff;

    private final CheckedSupplier<DatagramSocket> socketSupplier;

    /**
     * Creates an instance of {@link WakeOnLAN}.
     * @param socketSupplier A factory for {@link DatagramSocket} instances.
     */
    public WakeOnLAN(final CheckedSupplier<DatagramSocket> socketSupplier) {
        this.socketSupplier = socketSupplier;
    }

    /**
     * Sends a magic packet for a given device denoted by the MAC address to the given broadcast
     * IP address.
     * @param broadcastIpAddr The broadcast IP address.
     * @param macAddr         The MAC address of the target device.
     * @throws IOException    In case of an I/O error.
     */
    public void sendMagicPacket(final String broadcastIpAddr, final String macAddr)
            throws IOException {
        LOGGER.log(Level.INFO,
                "Sending magic packet to broadcast IP {0} for MAC {1}",
                new Object[]{broadcastIpAddr, macAddr});
        byte[] macBytes = convertMacToBytes(macAddr);
        byte[] bytes = new byte[NUM_LEADING_MAGIC_BYTES + NUM_MAC_MAGIC_BYTES * macBytes.length];
        for (int i = 0; i < NUM_LEADING_MAGIC_BYTES; i++) {
            bytes[i] = VAL_LEADING_MAGIC_BYTE;
        }
        for (int i = NUM_LEADING_MAGIC_BYTES; i < bytes.length; i += macBytes.length) {
            System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
        }

        InetAddress address = InetAddress.getByName(broadcastIpAddr);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, PORT);
        DatagramSocket socket;
        try {
            socket = socketSupplier.get();
        } catch (Exception e) {
            throw new IOException("An error occurred while creating a socket", e);
        }
        socket.send(packet);
        socket.close();
        LOGGER.log(Level.INFO, "Magic packet has been sent");
    }

    /**
     * Converts the given MAC string to a byte array representation.
     * @param macStr The MAC address as a string.
     * @return The MAC address as a byte array.
     * @throws IllegalArgumentException If the given string is not a valid MAC address.
     */
    private static byte[] convertMacToBytes(final String macStr) throws IllegalArgumentException {
        byte[] bytes = new byte[NUM_MAC_ADDRESS_PARTS];
        String[] hex = macStr.split("([:\\-])");
        if (hex.length != NUM_MAC_ADDRESS_PARTS) {
            throw new IllegalArgumentException("Invalid MAC address");
        }
        try {
            for (int i = 0; i < NUM_MAC_ADDRESS_PARTS; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], RADIX_HEXADECIMAL);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address");
        }
        return bytes;
    }

}
