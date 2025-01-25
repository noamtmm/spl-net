package bgu.spl.net.impl;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.stomp.StompMessage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
// fit to the object class
public class MessageEncoderDecoderImpl implements MessageEncoderDecoder<String> {
    private byte[] bytes = new byte[1 << 10]; // 1KB
    private int len = 0;
    private static final byte NULL_CHAR = '\u0000';

    private String convertLoginToConnect(String loginCommand) {
    String[] parts = loginCommand.split(" ");
    if (parts.length != 4) return null;
    return StompMessage.makeConnect(parts[2], parts[3]).toString();
}

@Override
public String decodeNextByte(byte nextByte) {
    if (nextByte == NULL_CHAR) {
        String msg = popString();
        if (msg.startsWith("login ")) {
            return convertLoginToConnect(msg);
        }
        return msg;
    }
    pushByte(nextByte);
    return null;
}

    private String popString() {
        // Create string from accumulated bytes
        String result = new String(bytes, 0, len, StandardCharsets.UTF_8);
        len = 0;
        return result;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;
    }

    @Override
    public byte[] encode(String message) {
        // Add null character at the end for STOMP frame
        return (message + '\u0000').getBytes(StandardCharsets.UTF_8);
    }
}