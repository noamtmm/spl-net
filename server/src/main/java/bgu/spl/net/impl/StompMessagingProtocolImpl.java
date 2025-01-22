package bgu.spl.net.impl;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.impl.stomp.StompMessage;
import bgu.spl.net.srv.Connections;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StompMessagingProtocolImpl<T> implements StompMessagingProtocol<T> {
    private int connectionId;
    private Connections<T> connections;
    private boolean shouldTerminate;
    private static final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>(); // username -> password
    private static final ConcurrentHashMap<Integer, String> activeUsers = new ConcurrentHashMap<>(); // connectionId -> username
    private static final ConcurrentHashMap<Integer, Map<String, String>> subscriptions = new ConcurrentHashMap<>(); // connectionId -> (topic -> subId)
    private static Integer messageIdCounter = 0;
    private static int receiptIdCounter = 0;

    @Override
    public void start(int connectionId, Connections<T> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
        this.shouldTerminate = false;
        subscriptions.put(connectionId, new ConcurrentHashMap<>());
    }

    @Override
    public void process(T message) {
        StompMessage stompMessage = StompMessage.parse((String) message);
        String command = stompMessage.getCommand();
        System.out.println(command);

        switch (command) {
            case "CONNECT":
                handleConnect(stompMessage);
                break;
            case "SEND":
                handleSend(stompMessage);
                break;
            case "SUBSCRIBE":
                handleSubscribe(stompMessage);
                break;
            case "UNSUBSCRIBE":
                handleUnsubscribe(stompMessage);
                break;
            case "DISCONNECT":
                handleDisconnect(stompMessage);
                break;
            default:
                sendError("Invalid command");
                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    @SuppressWarnings("unchecked")
    private void handleConnect(StompMessage msg) {
        String login = msg.getHeader("login");
        String passcode = msg.getHeader("passcode");
        String acceptVersion = msg.getHeader("accept-version");
       

        if (login == null || passcode == null || acceptVersion == null) {
            sendError("Missing required headers");
            return;
        }

        if (!acceptVersion.equals("1.2")) {
            sendError("Incompatible version");
            return;
        }

        // Check if user exists
        if (users.containsKey(login)) {
            // Existing user - verify password
            if (!users.get(login).equals(passcode)) {
                sendError("Wrong password");
                return;
            }
            // Check if already connected
            if (isUserConnected(login)) {
                System.out.println("User already logged in");
                sendError("User already logged in");
                return;
            }
        } else {
            // New user - register
            users.putIfAbsent(login, passcode);
        }

        // Mark user as active
        activeUsers.putIfAbsent(connectionId, login);

        // Send CONNECTED frame
        StompMessage connected = new StompMessage();
        connected.setCommand("CONNECTED");
        connected.addHeader("version", "1.2");
       
        boolean ans =  connections.send(connectionId, (T) connected.toString());
       
        if (!ans) {
            System.out.println("Error sending CONNECTED frame");
            shouldTerminate = true;
        }
    }

    private void handleSubscribe(StompMessage msg) {
        if (!isLoggedIn()) return;

        String destination = msg.getHeader("destination");
        String id = msg.getHeader("id");

        if (destination == null || id == null) {
            sendError("Missing required headers");
            return;
        }

        // Save subscription
        subscriptions.get(connectionId).putIfAbsent(destination, id);

        // Add to connections
        connections.subscribe(connectionId, destination);

        // Send receipt if requested
        String receipt = msg.getHeader("receipt");
        if (receipt != null) {
            sendReceipt(receipt);
        }
    }

    private void handleUnsubscribe(StompMessage msg) {
        if (!isLoggedIn()) return;

        String id = msg.getHeader("id");
        if (id == null) {
            sendError("Missing id header");
            return;
        }

        // Find and remove subscription
        Map<String, String> userSubs = subscriptions.get(connectionId);
        String topic = null;
        for (Map.Entry<String, String> entry : userSubs.entrySet()) {
            if (entry.getValue().equals(id)) {
                topic = entry.getKey();
                break;
            }
        }

        if (topic != null) {
            userSubs.remove(topic);
            connections.unsubscribe(connectionId, topic);
        }

        // Send receipt if requested
        String receipt = msg.getHeader("receipt");
        if (receipt != null) {
            sendReceipt(receipt);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSend(StompMessage msg) {
        if (!isLoggedIn()) return;

        String destination = msg.getHeader("destination");
        if (destination == null) {
            sendError("Missing destination header");
            return;
        }

        // Check if subscribed
        if (!subscriptions.get(connectionId).containsKey(destination)) {
            sendError("Not subscribed to topic");
            return;
        }

        // Create MESSAGE frame
        StompMessage messageFrame = new StompMessage();
        messageFrame.setCommand("MESSAGE");
        messageFrame.addHeader("destination", destination);
        messageFrame.addHeader("message-id", String.valueOf(messageIdCounter++));
        messageFrame.addHeader("subscription", subscriptions.get(connectionId).get(destination));
        messageFrame.setBody(msg.getBody());

        // Send to all subscribers
        connections.send(destination, (T)messageFrame.toString());

        // Send receipt if requested
        String receipt = msg.getHeader("receipt");
        if (receipt != null) {
            sendReceipt(receipt);
        }
    }

    private void handleDisconnect(StompMessage msg) {
        if (!isLoggedIn()) return;

        String receipt = msg.getHeader("receipt");
        if (receipt == null) {
            sendError("Missing receipt header");
            return;
        }

        // Send receipt
        sendReceipt(receipt);

        // Clean up
        activeUsers.remove(connectionId);
        subscriptions.remove(connectionId);
        shouldTerminate = true;
    }

    private boolean isLoggedIn() {
        return activeUsers.containsKey(connectionId);
    }

    private boolean isUserConnected(String username) {
        return activeUsers.containsValue(username);
    }

    @SuppressWarnings("unchecked")
    private void sendError(String message) {
        System.out.println("Error: " + message);
        StompMessage error = new StompMessage();
        error.setCommand("ERROR");
        error.addHeader("message", message);
        connections.send(connectionId, (T)error.toString());
        shouldTerminate = true;
    }

    @SuppressWarnings("unchecked")
    private void sendReceipt(String receiptId) {
        StompMessage receipt = new StompMessage();
        receipt.setCommand("RECEIPT");
        receipt.addHeader("receipt-id", String.valueOf(receiptIdCounter++));  // Use counter instead of passed ID
        connections.send(connectionId, (T)receipt.toString());
    }
}