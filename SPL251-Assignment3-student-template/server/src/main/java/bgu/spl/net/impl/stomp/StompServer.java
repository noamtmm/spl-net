package bgu.spl.net.impl.stomp;

import bgu.spl.net.impl.MessageEncoderDecoderImpl;
import bgu.spl.net.impl.StompMessagingProtocolImpl;
import bgu.spl.net.srv.Server;

public class StompServer {
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: StompServer <port> <server type>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String serverType = args[1];

        if (serverType.equalsIgnoreCase("tpc")) {
            Server.threadPerClient(port, () -> new StompMessagingProtocolImpl<String>(), () -> new MessageEncoderDecoderImpl()).serve();
        }
        else if (serverType.equalsIgnoreCase("reactor")) {
            Server.reactor(
                Runtime.getRuntime().availableProcessors(),
                port,
                () -> new StompMessagingProtocolImpl<String>(),
                () -> new MessageEncoderDecoderImpl()
            ).serve();
        }
        else {
            System.out.println("Invalid server type. Use 'tpc' or 'reactor'");
        }
    }
}