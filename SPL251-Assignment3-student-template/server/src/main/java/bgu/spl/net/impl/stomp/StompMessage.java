package bgu.spl.net.impl.stomp;

import java.util.HashMap;
import java.util.Map;

public class StompMessage {
    private String command;
    private Map<String, String> headers;
    private String body;

    // First constructor - for creating new messages
    public StompMessage() {
        this.headers = new HashMap<>();
        this.body = "";
        this.command = "";
    }

    // Second constructor - for parsing received messages
    public static StompMessage parse(String rawMessage) {
        StompMessage msg = new StompMessage();
        msg.parseMessage(rawMessage);
        return msg;
    }

    // Set command - used when creating new messages
    public void setCommand(String command) {
        this.command = command;
    }

    private void parseMessage(String rawMessage) {
        try {
            String[] parts = rawMessage.split("\n\n", 2); // Split headers and body
            String[] lines = parts[0].split("\n");

            // Parse command (first line)
            if (lines.length > 0) {
                command = lines[0];
            }

            // Parse headers
            for (int i = 1; i < lines.length; i++) {
                String[] header = lines[i].split(":", 2);
                if (header.length == 2) {
                    headers.put(header[0].trim(), header[1].trim());
                }
            }

            // Parse body if exists
            body = (parts.length > 1) ? parts[1] : "";
        }

        catch (Exception e) {
            throw new RuntimeException("Failed to parse STOMP message: " + e.getMessage());
        }
    }

    // Header operations
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public boolean hasHeader(String key) {
        return headers.containsKey(key);
    }

    // Getters
    public String getCommand() {
        return command;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    // Command type checks
    public boolean isConnect() {
        return "CONNECT".equals(command);
    }

    public boolean isConnected() {
        return "CONNECTED".equals(command);
    }

    public boolean isSubscribe() {
        return "SUBSCRIBE".equals(command);
    }

    public boolean isUnsubscribe() {
        return "UNSUBSCRIBE".equals(command);
    }

    public boolean isSend() {
        return "SEND".equals(command);
    }

    public boolean isMessage() {
        return "MESSAGE".equals(command);
    }

    public boolean isReceipt() {
        return "RECEIPT".equals(command);
    }

    public boolean isError() {
        return "ERROR".equals(command);
    }

    public boolean isDisconnect() {
        return "DISCONNECT".equals(command);
    }

    // Factory methods for creating specific message types
    public static StompMessage makeConnect(String login, String passcode) {
        StompMessage frame = new StompMessage();
        frame.setCommand("CONNECT");
        frame.addHeader("accept-version", "1.2");
        frame.addHeader("host", "stomp.cs.bgu.ac.il");
        frame.addHeader("login", login);
        frame.addHeader("passcode", passcode);
        return frame;
    }

    public static StompMessage makeConnected() {
        StompMessage frame = new StompMessage();
        frame.setCommand("CONNECTED");
        frame.addHeader("version", "1.2");
        return frame;
    }

    public static StompMessage makeError(String message, String receiptId) {
        StompMessage frame = new StompMessage();
        frame.setCommand("ERROR");
        frame.addHeader("message", message);
        if (receiptId != null) {
            frame.addHeader("receipt-id", receiptId);
        }
        return frame;
    }

    public static StompMessage makeReceipt(String receiptId) {
        StompMessage frame = new StompMessage();
        frame.setCommand("RECEIPT");
        frame.addHeader("receipt-id", receiptId);
        return frame;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        // Add command
        builder.append(command).append('\n');
        
        // Add headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.append(header.getKey())
                   .append(':')
                   .append(header.getValue())
                   .append('\n');
        }
        

        // Add blank line separator
        builder.append('\n');
        
        // Add body if exists
        if (body != null && !body.isEmpty()) {
            builder.append(body);
        }
        
        return builder.toString();
    }
}