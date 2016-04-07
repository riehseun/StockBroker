import java.net.*;
import java.io.*;

// OnlineBroker is a client to BrokerlookupServer and is a server to BrokerClient and BrokerExchange
public class OnlineBroker {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            if(args.length == 4) {
                serverSocket = new ServerSocket(Integer.parseInt(args[2]));
            } else {
                System.err.println("ERROR (OnlineBroker as a Server to BrokerClient): Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR (OnlineBroker as a Server to BrokerClient): Could not listen on port!");
            System.exit(-1);
        }

        // Contact BrokerLookupServer to register OnlineBroker
        Socket lookupSocket = null;
        ObjectOutputStream lookupOut = null;
        ObjectInputStream lookupIn = null;

        try {
            String lookupIpAddress = "";
            int lookupPort = 0;
            int brokerPort = 0;
            String brokerName = "";
            if(args.length == 4) {
                lookupIpAddress = args[0];
                lookupPort = Integer.parseInt(args[1]);     
                brokerPort = Integer.parseInt(args[2]);
                brokerName = args[3];
            }
            else {
                System.err.println("ERROR (OnlineBroker as a Clinet to BrokerLookupServer): Invalid arguments!");
                System.exit(-1);
            }

            lookupSocket = new Socket(lookupIpAddress, lookupPort);
            lookupOut = new ObjectOutputStream(lookupSocket.getOutputStream());
            lookupIn = new ObjectInputStream(lookupSocket.getInputStream());

            BrokerPacket packetToLookup = new BrokerPacket();
            packetToLookup.type = BrokerPacket.LOOKUP_REGISTER;
            packetToLookup.exchange = brokerName;
            packetToLookup.locations = new BrokerLocation[1];
            packetToLookup.locations[0] = new BrokerLocation(lookupIpAddress, brokerPort);
            lookupOut.writeObject(packetToLookup);
        }
        catch (IOException e) {
            System.err.println("ERROR (OnlineBroker as a Clinet to BrokerLookupServer): Could not listen on port!");
            System.exit(-1);
        }     

        // Run thread to serve BrokerClient as a Server
        while (listening) {
            new OnlineBrokerHandlerThread(serverSocket.accept(), lookupSocket, lookupOut, lookupIn).start();
        }

        serverSocket.close();
        lookupSocket.close();
        lookupOut.close();
        lookupIn.close();
    }
}
