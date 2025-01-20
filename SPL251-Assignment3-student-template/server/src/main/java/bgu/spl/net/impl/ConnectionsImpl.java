package bgu.spl.net.impl;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class ConnectionsImpl <T> implements Connections<T> {
    
    private ConcurrentHashMap<Integer, ConnectionHandler<T>> clients; // connectionId -> ConnectionHandler
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<Integer>> topicsSubscribers; // topic -> topic's subscribers connectionId

    public ConnectionsImpl() {
        clients = new ConcurrentHashMap<>();
        topicsSubscribers = new ConcurrentHashMap<>();
    }
    
    public boolean send(int connectionId, T msg) {
        if (clients.get(connectionId) != null) {
            clients.get(connectionId).send(msg);
            return true;
        }
        return false;
    }

    public void send(String channel, T msg) {
        if (topicsSubscribers.get(channel) != null) {
            for (Integer connectionId : topicsSubscribers.get(channel)) {
                clients.get(connectionId).send(msg);
            }
        }    
    }

    public void disconnect(int connectionId) {
        if (clients.get(connectionId) != null) {
            try {
                clients.get(connectionId).close();
                clients.remove(connectionId);
                for (String s : topicsSubscribers.keySet()){
                    topicsSubscribers.get(s).remove(connectionId);
                }
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addConnection(int uniqueId, ConnectionHandler<T> connection) {
        if (clients.get(uniqueId) == null) {
            uniqueId++;
        }
        clients.putIfAbsent(uniqueId, connection);
    }    

    public void subscribe(int connectionId, String topic) {
        topicsSubscribers.computeIfAbsent(topic, k -> new ConcurrentLinkedQueue<>()).add(connectionId);
    }

    public void unsubscribe(int connectionId, String topic) {
        ConcurrentLinkedQueue<Integer> subscribers = topicsSubscribers.get(topic);
        if (subscribers != null) {
            subscribers.remove(connectionId);
        }
    }

}
