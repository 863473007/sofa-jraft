package com.alipay.sofa.jraft.entity.codec.v2;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alipay.sofa.jraft.JRaftUtils;
import com.alipay.sofa.jraft.entity.LogEntry;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.codec.LogEntryDecoder;
import com.alipay.sofa.jraft.entity.codec.v2.LogOutter.PBLogEntry;
import com.alipay.sofa.jraft.util.Bits;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ZeroByteStringHelper;

/**
 * V2 log entry decoder based on protobuf, see src/main/resources/log.proto
 * @author boyan(boyan@antfin.com)
 *
 */
public class V2Decoder implements LogEntryDecoder {

    private V2Decoder() {

    }

    private static final Logger   LOG      = LoggerFactory.getLogger(V2Decoder.class);
    public static final V2Decoder INSTANCE = new V2Decoder();

    @Override
    public LogEntry decode(final byte[] bs) {

        if (bs == null || bs.length < LogEntryV2CodecFactory.HEADER_SIZE) {
            return null;
        }

        int i = 0;
        for (byte b : LogEntryV2CodecFactory.MAGIC_BYTES) {
            if (bs[i++] != b) {
                return null;
            }
        }

        if (Bits.getShort(bs, i) != LogEntryV2CodecFactory.VERSION) {
            return null;
        }
        i += 2;

        try {

            PBLogEntry entry = PBLogEntry.parseFrom(ZeroByteStringHelper.wrap(bs, i, bs.length - i));

            LogEntry log = new LogEntry();
            log.setType(entry.getType());
            log.getId().setIndex(entry.getIndex());
            log.getId().setTerm(entry.getTerm());

            if (entry.hasChecksum()) {
                log.setChecksum(entry.getChecksum());
            }
            if (entry.getPeersCount() > 0) {
                List<PeerId> peers = new ArrayList<>(entry.getPeersCount());
                for (String s : entry.getPeersList()) {
                    peers.add(JRaftUtils.getPeerId(s));
                }
                log.setPeers(peers);
            }
            if (entry.getOldPeersCount() > 0) {
                List<PeerId> peers = new ArrayList<>(entry.getOldPeersCount());
                for (String s : entry.getOldPeersList()) {
                    peers.add(JRaftUtils.getPeerId(s));
                }
                log.setOldPeers(peers);
            }

            if (!entry.getData().isEmpty()) {
                log.setData(entry.getData().asReadOnlyByteBuffer());
            }

            return log;
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Fail to decode pb log entry", e);
            return null;
        }
    }

}
