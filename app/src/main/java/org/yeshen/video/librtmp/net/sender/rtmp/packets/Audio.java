package org.yeshen.video.librtmp.net.sender.rtmp.packets;

import org.yeshen.video.librtmp.net.sender.rtmp.io.SessionInfo;

/**
 * Audio data packet
 *  
 * @author francois
 */
public class Audio extends ContentData {

    public Audio(ChunkHeader header) {
        super(header);
    }

    public Audio() {
        super(new ChunkHeader(ChunkType.TYPE_0_FULL, SessionInfo.RTMP_AUDIO_CHANNEL, MessageType.AUDIO));
    }

    @Override
    public String toString() {
        return "RTMP Audio";
    }
}
