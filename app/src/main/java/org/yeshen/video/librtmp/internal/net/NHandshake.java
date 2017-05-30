package org.yeshen.video.librtmp.internal.net;

import android.util.Log;

import org.yeshen.video.librtmp.unstable.net.sender.rtmp.Crypto;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.Util;
import org.yeshen.video.librtmp.unstable.tools.Lg;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Random;

/**
 * Handles the RTMP handshake song 'n dance
 * <p>
 * Thanks to http://thompsonng.blogspot.com/2010/11/rtmp-part-10-handshake.html for some very useful information on
 * the the hidden "features" of the RTMP handshake
 *
 * @author francois
 * @author yeshen
 */
public final class NHandshake {
    private static final String TAG = "Handshake";
    /**
     * S1 as sent by the server
     */
    private byte[] s1;
    private static final byte PROTOCOL_VERSION = 0x03;
    private static final int HANDSHAKE_SIZE = 1536;
    private static final int SHA256_DIGEST_SIZE = 32;
    private static final int DIGEST_OFFSET_INDICATOR_POS = 772; // should either be byte 772 or byte 8
    private static final int READ_TIME_OUT = 3000;

    private static final byte[] GENUINE_FP_KEY = {
            (byte) 0x47, (byte) 0x65, (byte) 0x6E, (byte) 0x75, (byte) 0x69, (byte) 0x6E, (byte) 0x65, (byte) 0x20,
            (byte) 0x41, (byte) 0x64, (byte) 0x6F, (byte) 0x62, (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6C,
            (byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x50, (byte) 0x6C, (byte) 0x61, (byte) 0x79,
            (byte) 0x65, (byte) 0x72, (byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31, // Genuine Adobe Flash Player 001
            (byte) 0xF0, (byte) 0xEE, (byte) 0xC2, (byte) 0x4A, (byte) 0x80, (byte) 0x68, (byte) 0xBE, (byte) 0xE8,
            (byte) 0x2E, (byte) 0x00, (byte) 0xD0, (byte) 0xD1, (byte) 0x02, (byte) 0x9E, (byte) 0x7E, (byte) 0x57,
            (byte) 0x6E, (byte) 0xEC, (byte) 0x5D, (byte) 0x2D, (byte) 0x29, (byte) 0x80, (byte) 0x6F, (byte) 0xAB,
            (byte) 0x93, (byte) 0xB8, (byte) 0xE6, (byte) 0x36, (byte) 0xCF, (byte) 0xEB, (byte) 0x31, (byte) 0xAE};

    private SocketChannel channel;
    private Selector selector = null;

    public NHandshake(SocketChannel channel) throws IOException {
        this.channel = channel;
        this.selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
    }

    public final void writeC0C1() throws IOException {
        Random random = new Random();
        final int digestOffset = random.nextInt(HANDSHAKE_SIZE - DIGEST_OFFSET_INDICATOR_POS - 4 - 8 - SHA256_DIGEST_SIZE);
        final int absoluteDigestOffset = ((digestOffset % 728) + DIGEST_OFFSET_INDICATOR_POS + 4);
        int remaining = digestOffset;
        final byte[] digestOffsetBytes = new byte[4];
        for (int i = 3; i >= 0; i--) {
            if (remaining > 255) {
                digestOffsetBytes[i] = (byte) 255;
                remaining -= 255;
            } else {
                digestOffsetBytes[i] = (byte) remaining;
                remaining -= remaining;
            }
        }
        byte[] partBeforeDigest = new byte[absoluteDigestOffset];
        random.nextBytes(partBeforeDigest);

        byte[] timeStamp = Util.unsignedInt32ToByteArray((int) (System.currentTimeMillis() / 1000));
        System.arraycopy(timeStamp, 0, partBeforeDigest, 0, 4);
        System.arraycopy(new byte[]{(byte) 0x80, 0x00, 0x07, 0x02}, 0, partBeforeDigest, 4, 4);

        byte[] partAfterDigest = new byte[HANDSHAKE_SIZE - absoluteDigestOffset - SHA256_DIGEST_SIZE];
        random.nextBytes(partAfterDigest);

        System.arraycopy(digestOffsetBytes, 0, partBeforeDigest, 772, 4);

        byte[] tempBuffer = new byte[HANDSHAKE_SIZE - SHA256_DIGEST_SIZE];
        System.arraycopy(partBeforeDigest, 0, tempBuffer, 0, partBeforeDigest.length);
        System.arraycopy(partAfterDigest, 0, tempBuffer, partBeforeDigest.length, partAfterDigest.length);

        Crypto crypto = new Crypto();
        byte[] digest = crypto.calculateHmacSHA256(tempBuffer, GENUINE_FP_KEY, 30);


        Log.d(TAG, "writeC0 C1");
        ByteBuffer buffer = ByteBuffer.allocate(1 + HANDSHAKE_SIZE);
        buffer.put(PROTOCOL_VERSION);
        buffer.put(partBeforeDigest);
        buffer.put(digest);
        buffer.put(partAfterDigest);
        buffer.flip();
        channel.write(buffer);
    }

    public final void readS0S1() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1 + HANDSHAKE_SIZE);
        selector.select(READ_TIME_OUT);
        channel.read(buffer);
        buffer.rewind();

        byte s0 = buffer.get();
        if (s0 != PROTOCOL_VERSION) {
            if (s0 == -1) {
                throw new IOException("InputStream closed");
            } else {
                throw new IOException("Invalid RTMP protocol version; expected " + PROTOCOL_VERSION + ", got " + s0);
            }
        }
        s1 = new byte[HANDSHAKE_SIZE];
        buffer.get(s1, 0, HANDSHAKE_SIZE);
    }

    public final void writeC2() throws IOException {
        if (s1 == null) {
            throw new IllegalStateException("C2 cannot be written without S1 being read first");
        }
        ByteBuffer buffer = ByteBuffer.wrap(s1);
        buffer.flip();
        channel.write(buffer);
    }

    public final void readS2() throws IOException {
        byte[] sr_serverTime = new byte[4];
        byte[] s2_serverVersion = new byte[4];
        byte[] s2_rest = new byte[HANDSHAKE_SIZE - 8];

        ByteBuffer buffer = ByteBuffer.allocate(HANDSHAKE_SIZE);
        selector.select(READ_TIME_OUT);
        channel.read(buffer);
        buffer.rewind();

        buffer.get(sr_serverTime, 0, 4);
        buffer.get(s2_serverVersion, 0, 4);
        buffer.get(s2_rest, 0, HANDSHAKE_SIZE - 8);
    }

    public final void done() {
        try {
            if (selector != null) selector.close();
        } catch (IOException e) {
            Lg.e(e);
        }
        selector = null;
    }
}

