package bgu.spl.net.srv;

public interface Connections<T> {

    boolean send(int connectionId, T string);
    
    void send(String channel, T msg);

    void disconnect(int connectionId);

    void subscribe(int connectionId, String destination);

    void unsubscribe(int connectionId, String topic);

}
