package org.yeshen.video.librtmp.afix.net.sender.rtmp.packets;

import org.yeshen.video.librtmp.afix.net.sender.rtmp.Util;
import org.yeshen.video.librtmp.afix.net.sender.rtmp.io.SessionInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Window Acknowledgement Size
 * 
 * Also known as ServerBW ("Server bandwidth") in some RTMP implementations.
 * 
 * @author francois
 */
public class WindowAckSize extends Chunk {

    private int acknowledgementWindowSize;

    public WindowAckSize(ChunkHeader header) {
        super(header);
    }
    
    public WindowAckSize(int acknowledgementWindowSize) {
        super(new ChunkHeader(ChunkType.TYPE_0_FULL, SessionInfo.RTMP_CONTROL_CHANNEL, MessageType.WINDOW_ACKNOWLEDGEMENT_SIZE));
        this.acknowledgementWindowSize = acknowledgementWindowSize;
    }


    public int getAcknowledgementWindowSize() {
        return acknowledgementWindowSize;
    }

    public void setAcknowledgementWindowSize(int acknowledgementWindowSize) {
        this.acknowledgementWindowSize = acknowledgementWindowSize;
    }

    @Override
    public void readBody(InputStream in) throws IOException {
        acknowledgementWindowSize = Util.readUnsignedInt32(in);
    }

    @Override
    protected void writeBody(OutputStream out) throws IOException {        
        Util.writeUnsignedInt32(out, acknowledgementWindowSize);
    }
    
    @Override
    public String toString() {
        return "RTMP Window Acknowledgment Size";
    }
}
