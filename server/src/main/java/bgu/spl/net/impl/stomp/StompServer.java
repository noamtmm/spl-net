package bgu.spl.net.impl.stomp;

import bgu.spl.net.impl.MessageEncoderDecoderImpl;
import bgu.spl.net.impl.StompMessagingProtocolImpl;
import bgu.spl.net.srv.Server;


public class StompServer {
    public static void main(String[] args) {
        if (args.length <= 1) {
            System.out.println("Usage: StompServer <port>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        String type = args[1];
        final int numThreads = 7;
        if (type.equals("tpc")) {
            Server.threadPerClient(port, 
                                () -> new StompMessagingProtocolImpl<>(),
                                () -> new MessageEncoderDecoderImpl()).serve();;
        }
        else if (type.equals("reactor")) {
            Server.reactor(numThreads, 
                        port, 
                        () -> new StompMessagingProtocolImpl<>(),
                        () -> new MessageEncoderDecoderImpl() ).serve();
        }
        else {
            throw new IllegalArgumentException("Invalid server type. Use 'tpc' or 'reactor'");
        }
    }
    


  

   
}
