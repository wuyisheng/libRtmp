package org.yeshen.video.librtmp.unstable.net.sender.rtmp.io;

import android.annotation.SuppressLint;
import android.util.Log;

import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.Abort;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.Acknowledgement;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.Audio;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.Chunk;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.ChunkHeader;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.Command;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.Data;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.SetChunkSize;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.SetPeerBandwidth;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.UserControl;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.Video;
import org.yeshen.video.librtmp.unstable.net.sender.rtmp.packets.WindowAckSize;
import org.yeshen.video.librtmp.unstable.tools.Lg;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 *
 * @author francois
 */
class RtmpDecoder {

    private static final String TAG = "RtmpDecoder";

    private SessionInfo sessionInfo;

    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, StoreChunk> storeChunkHashMap = new HashMap<>();

    RtmpDecoder(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    Chunk readPacket(InputStream in) throws IOException {

        ChunkHeader header = ChunkHeader.readHeader(in, sessionInfo);
        Chunk rtmpPacket;
        Lg.d(TAG, "readPacket(): header.messageType: " + header.getMessageType());

        int messageLength = header.getPacketLength();
        if (header.getPacketLength() > sessionInfo.getRxChunkSize()) {
            Lg.d(TAG, "readPacket(): packet size ("
                    + header.getPacketLength()
                    + ") is bigger than chunk size ("
                    + sessionInfo.getRxChunkSize()
                    + "); storing chunk data");
            // This packet consists of more than one chunk; store the chunks in the chunk stream until everything is read
            StoreChunk storeChunk = storeChunkHashMap.get(header.getChunkStreamId());
            if(storeChunk == null) {
                storeChunk = new StoreChunk();
                storeChunkHashMap.put(header.getChunkStreamId(), storeChunk);
            }
            if (!storeChunk.storeChunk(in, messageLength, sessionInfo.getRxChunkSize())) {
                Log.d(TAG, " readPacket(): returning null because of incomplete packet");
                return null; // packet is not yet complete
            } else {
                Log.d(TAG, " readPacket(): stored chunks complete packet; reading packet");
                in = storeChunk.getStoredInputStream();
            }
        } else {
            Lg.d(TAG, "readPacket(): packet size ("
                    + header.getPacketLength()
                    + ") is LESS than chunk size ("
                    + sessionInfo.getRxChunkSize()
                    + "); reading packet fully");
        }

        switch (header.getMessageType()) {

            case SET_CHUNK_SIZE:
                SetChunkSize setChunkSize = new SetChunkSize(header);
                setChunkSize.readBody(in);
                Log.d(TAG, "readPacket(): Setting chunk size to: " + setChunkSize.getChunkSize());
                sessionInfo.setRxChunkSize(setChunkSize.getChunkSize());
                return null;
            case ABORT:
                rtmpPacket = new Abort(header);
                break;
            case USER_CONTROL_MESSAGE:
                rtmpPacket = new UserControl(header);
                break;
            case WINDOW_ACKNOWLEDGEMENT_SIZE:
                rtmpPacket = new WindowAckSize(header);
                break;
            case SET_PEER_BANDWIDTH:
                rtmpPacket = new SetPeerBandwidth(header);
                break;
            case AUDIO:
                rtmpPacket = new Audio(header);
                break;
            case VIDEO:
                rtmpPacket = new Video(header);
                break;
            case COMMAND_AMF0:
                rtmpPacket = new Command(header);
                break;
            case DATA_AMF0:
                rtmpPacket = new Data(header);
                break;
            case ACKNOWLEDGEMENT:
                rtmpPacket = new Acknowledgement(header);
                break;
            default:
                throw new IOException("No packet body implementation for message type: " + header.getMessageType());
        }                
        rtmpPacket.readBody(in);
        return rtmpPacket;
    }

    void clearStoredChunks(int chunkStreamId) {
        StoreChunk storeChunk = storeChunkHashMap.get(chunkStreamId);
        if(storeChunk != null) {
            storeChunk.clearStoredChunks();
        }
    }
}
