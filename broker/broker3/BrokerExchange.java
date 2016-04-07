import java.io.*;
import java.net.*;
import java.util.Scanner;

public class BrokerExchange {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// BrokerLookupServer: connection tools
        Socket lookupSocket = null;
        ObjectOutputStream lookupOut = null;
        ObjectInputStream lookupIn = null;
        String lookupIpAddress = "";
        int lookupPort = 0;
        String brokerName = "";

        // OnlineBroker: connection tools
        Socket brokerSocket = null;
        ObjectOutputStream brokerOut = null;
        ObjectInputStream brokerIn = null;
        String brokerIpAddress = "";
        Integer brokerPort = 0;

		try {
			if(args.length == 3 ) {
				lookupIpAddress = args[0];
				lookupPort = Integer.parseInt(args[1]);
                brokerName = args[2];
			} 
            else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			lookupSocket = new Socket(lookupIpAddress, lookupPort);
            lookupOut = new ObjectOutputStream(lookupSocket.getOutputStream());         
            lookupIn = new ObjectInputStream(lookupSocket.getInputStream());
		} 
        catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} 
        catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;

		System.out.print("<CONSOLE>");
		while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("bye") == -1) {
            // If user presses "x", exit
            if (userInput.equals("x")) {
                System.exit(0);
            }
            String[] command = userInput.split("\\s+");

            // request to lookup server
            BrokerPacket packetToLookup = new BrokerPacket();
            packetToLookup.type = BrokerPacket.LOOKUP_REQUEST;
            packetToLookup.exchange = brokerName;
            lookupOut.writeObject(packetToLookup);

            // reply from lookup server
            BrokerPacket packetFromLookup;
            packetFromLookup = (BrokerPacket) lookupIn.readObject();
            if (packetFromLookup.type == BrokerPacket.LOOKUP_REPLY) {
                // using ip and port received from BrokerLookupServer to connect to the broker 
                brokerIpAddress = packetFromLookup.locations[0].broker_host;
                brokerPort = packetFromLookup.locations[0].broker_port;
                brokerSocket = new Socket(brokerIpAddress, brokerPort);
                brokerOut = new ObjectOutputStream(brokerSocket.getOutputStream());
                brokerIn = new ObjectInputStream(brokerSocket.getInputStream());

                // request to broker server  
                BrokerPacket packetToBroker = new BrokerPacket();
                packetToBroker.locations = new BrokerLocation[1];
                packetToBroker.locations[0] = new BrokerLocation(brokerIpAddress, brokerPort);
                // figure out packet type based on user input          
                if (command[0].equals("add") && command.length == 2) {
                    packetToBroker.type = BrokerPacket.EXCHANGE_ADD;
                    packetToBroker.symbol = command[1];
                }
                else if (command[0].equals("remove") && command.length == 2) {
                    packetToBroker.type = BrokerPacket.EXCHANGE_REMOVE;
                    packetToBroker.symbol = command[1];
                }
                else if (command[0].equals("update") && command.length == 3) {
                    packetToBroker.type = BrokerPacket.EXCHANGE_UPDATE;
                    packetToBroker.symbol = command[1];
                    packetToBroker.quote = Long.parseLong(command[2], 10);
                }
                else {
                    System.out.println("Wrong Input Stupid!");
                    System.out.print("<CONSOLE>");
                    continue;
                }
                brokerOut.writeObject(packetToBroker);

                // reply from broker server
                BrokerPacket packetFromBroker;
                packetFromBroker = (BrokerPacket) brokerIn.readObject();                               
                if (packetFromBroker.type == BrokerPacket.EXCHANGE_ADD) {
                    System.out.println(packetToBroker.symbol + " added");
                    System.out.print("<CONSOLE>");
                }
                if (packetFromBroker.type == BrokerPacket.EXCHANGE_REMOVE) {
                    System.out.println(packetToBroker.symbol + " removed");
                    System.out.print("<CONSOLE>");
                }
                if (packetFromBroker.type == BrokerPacket.EXCHANGE_UPDATE) {
                    System.out.println(packetToBroker.symbol + " updated to " + packetToBroker.quote);
                    System.out.print("<CONSOLE>");
                }
                if (packetFromBroker.type == BrokerPacket.ERROR_INVALID_SYMBOL) {
                    System.out.println(packetToBroker.symbol + " invalid");
                    System.out.print("<CONSOLE>");
                }
                if (packetFromBroker.type == BrokerPacket.ERROR_OUT_OF_RANGE) {
                    System.out.println(packetToBroker.symbol + " out of range");
                    System.out.print("<CONSOLE>");
                }
                if (packetFromBroker.type == BrokerPacket.ERROR_SYMBOL_EXISTS) {
                    System.out.println(packetToBroker.symbol + " exists");
                    System.out.print("<CONSOLE>");
                }
            }
		}
		stdIn.close();
        lookupSocket.close();
        lookupOut.close();
        lookupIn.close();
        brokerSocket.close();
        brokerOut.close();
        brokerOut.close();
	}
}
