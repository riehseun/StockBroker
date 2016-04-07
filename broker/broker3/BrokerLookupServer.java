import java.net.*;
import java.io.*;
import java.util.*;

// This Naming Service server is the first thing to run. 
// BrokerClient, BrokerExchange, OnlineBroker are all clients to this server.
public class BrokerLookupServer {
    public static void main(String[] args) throws IOException {
        ServerSocket lookupPort = null;
        boolean listening = true;

        try {
            if(args.length == 1) {
                lookupPort = new ServerSocket(Integer.parseInt(args[0]));
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        // Create a file to hold broker ip addresses, ports, and names
        PrintWriter pw = new PrintWriter("brokers", "UTF-8");
        pw.close(); 
        File brokers = new File("brokers");

        while (listening) {
            // pass parameters that are needed in the handler thread
            new BrokerLookupHandlerThread(lookupPort.accept(), brokers).start();
        }

        lookupPort.close(); 
    }
}