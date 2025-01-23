package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.impl.ConnectionsImpl;
import bgu.spl.net.impl.MessageEncoderDecoderImpl;
import bgu.spl.net.impl.StompMessagingProtocolImpl;
import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public class StompServer<T> extends BaseServer<T> {

    public StompServer(int port, Supplier<StompMessagingProtocol<T>> protocolFactory, Supplier<MessageEncoderDecoder<T>> encdecFactory) {
        super(port, protocolFactory, encdecFactory);
    }

    @Override
    protected void execute(BlockingConnectionHandler<T> handler) {
        new Thread(handler).start();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: StompServer <port>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        StompServer <String> server = new StompServer<>(
                port,
                StompMessagingProtocolImpl::new,
                MessageEncoderDecoderImpl::new
        );
        server.serve();
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    


  

   
}