package org.yeshen.video.librtmp.internal.net;

import org.yeshen.video.librtmp.internal.net.carriers.Packeter;
import org.yeshen.video.librtmp.unstable.tools.Lg;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/*********************************************************************
 * Created by yeshen on 2017/05/18.
 * Copyright (c) 2017 yeshen.org. - All Rights Reserved
 *********************************************************************/


public class HandshakeHelper {

    private byte[] s1;
    private static final byte PROTOCOL_VERSION = 0x03;
    private static final int HANDSHAKE_SIZE = 1536;
    private static final int SHA256_DIGEST_SIZE = 32;
    private static final int DIGEST_OFFSET_INDICATOR_POS = 772;
    private static final int EOF = -1;
    private static final int DIGEST_OFFSET_RANGE = HANDSHAKE_SIZE - DIGEST_OFFSET_INDICATOR_POS - 4 - 8 - SHA256_DIGEST_SIZE;

    private static final byte[] GENUINE_FMS_KEY = new byte[]{
            71, 101, 110, 117, 105,
            110, 101, 32, 65, 100,
            111, 98, 101, 32, 70,
            108, 97, 115, 104, 32,
            77, 101, 100, 105, 97,
            32, 83, 101, 114, 118,
            101, 114, 32, 48, 48,
            49, -16, -18, -62, 74,
            -128, 104, -66, -24, 46,
            0, -48, -47, 2, -98,
            126, 87, 110, -20, 93,
            45, 41, -128, 111, -85,
            -109, -72, -26, 54, -49,
            -21, 49, -82};
    private static final byte[] GENUINE_FP_KEY = new byte[]{
            71, 101, 110, 117, 105,
            110, 101, 32, 65, 100,
            111, 98, 101, 32, 70,
            108, 97, 115, 104, 32,
            80, 108, 97, 121, 101,
            114, 32, 48, 48, 49,
            -16, -18, -62, 74, -128,
            104, -66, -24, 46, 0,
            -48, -47, 2, -98, 126,
            87, 110, -20, 93, 45,
            41, -128, 111, -85, -109,
            -72, -26, 54, -49, -21,
            49, -82};


    public void shakeInSequence(SocketChannel channel) throws IOException {
        /*C0 Version*/
        channel.write(ByteBuffer.wrap(new byte[]{PROTOCOL_VERSION}));
        /*C1*/
        sequenceC1(channel);
        /*S0*/
        sequenceS0(channel);
        /*S1*/
        sequenceS1(channel);
        /*C2*/
        sequenceC2(channel);
        /*S2*/
        sequenceS2(channel);
    }

    /***
     * C1 consisting of following fields:
     * Time:4bytes
     * Zero:4bytes
     * Random bytes
     * Random bytes cont
     */
    private void sequenceC1(SocketChannel channel) throws IOException {
        Random random = new Random();
        final int digestOffset = random.nextInt(DIGEST_OFFSET_RANGE);
        final int absoluteDigestOffset = ((digestOffset % 728) + DIGEST_OFFSET_INDICATOR_POS + 4);
        final byte[] digestOffsetBytes = new byte[4];
        int remaining = digestOffset;
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
        byte[] timeStamp = Packeter.unsignedInt32ToByteArray((int) (System.currentTimeMillis() / 1000));
        System.arraycopy(timeStamp, 0, partBeforeDigest, 0, 4);
        System.arraycopy(new byte[]{(byte) 0x80, 0x00, 0x07, 0x02}, 0, partBeforeDigest, 4, 4);
        byte[] partAfterDigest = new byte[HANDSHAKE_SIZE - absoluteDigestOffset - SHA256_DIGEST_SIZE];
        random.nextBytes(partAfterDigest);
        System.arraycopy(digestOffsetBytes, 0, partBeforeDigest, 772, 4);
        byte[] tempBuffer = new byte[HANDSHAKE_SIZE - SHA256_DIGEST_SIZE];
        System.arraycopy(partBeforeDigest, 0, tempBuffer, 0, partBeforeDigest.length);
        System.arraycopy(partAfterDigest, 0, tempBuffer, partBeforeDigest.length, partAfterDigest.length);

        byte[] digest = new byte[]{};
        try {
            Mac SHA256 = Mac.getInstance("HmacSHA256");
            SHA256.init(new SecretKeySpec(/*key*/GENUINE_FP_KEY, 0, /*length*/30, "HmacSHA256"));
            digest = SHA256.doFinal(tempBuffer);
        } catch (Exception e) {
            Lg.e(e);
        }

        ByteBuffer buffer = ByteBuffer.allocate(1537);
        buffer.put(partBeforeDigest);
        buffer.put(digest);
        buffer.put(partAfterDigest);
        channel.write(buffer);
    }

    private void sequenceS0(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        channel.read(buffer);
        if (buffer.limit() > 0) {
            byte S0 = buffer.get(0);
            if (S0 != PROTOCOL_VERSION) {
                if (S0 == EOF) {
                    throw new IOException("connect broker,eof");
                } else {
                    throw new IOException("Invalid RTMP protocol version; expected "
                            + PROTOCOL_VERSION + ", got " + S0);
                }
            }
        }
    }

    private void sequenceS1(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(HANDSHAKE_SIZE);
        channel.read(buffer);
        if (buffer.limit() != HANDSHAKE_SIZE) {
            throw new IOException("Unexpected EOF while reading S1, expected "
                    + HANDSHAKE_SIZE
                    + " bytes, but only read "
                    + buffer.limit()
                    + " bytes");
        }
        s1 = buffer.array();
    }

    private void sequenceC2(SocketChannel channel) throws IOException {
        if (s1 == null || s1.length == 0) {
            throw new IOException("nothing to echo,check sequenceC2");
        }
        channel.write(ByteBuffer.wrap(s1));
    }

    /**
     * time :4 bytes
     * version : 4 bytes
     * reset : 1528 bytes
     */
    private void sequenceS2(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(HANDSHAKE_SIZE);
        channel.read(buffer);
        if (buffer.limit() != HANDSHAKE_SIZE) {
            throw new IOException("Unexpected EOF while reading remainder of S2, expected "
                    + HANDSHAKE_SIZE
                    + " bytes, but only read "
                    + buffer.limit() + " bytes");
        }
    }


}
