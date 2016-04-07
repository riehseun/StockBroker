import java.io.*;
import java.net.*;

public class BrokerClient {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// BrokerLookupServer: connection tools
		Socket lookupSocket = null;
		ObjectOutputStream lookupOut = null;
		ObjectInputStream lookupIn = null;
		String lookupIpAddress = "";
		int lookupPort = 0;

		// OnlineBroker: connection tools
		Socket brokerSocket = null;
		ObjectOutputStream brokerOut = null;
		ObjectInputStream brokerIn = null;
		String brokerIpAddress = "";
		Integer brokerPort = 0;

		try {
			if(args.length == 2 ) {
				lookupIpAddress = args[0];
				lookupPort = Integer.parseInt(args[1]);
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
			System.err.println("ERROR (lookupSocket): Don't know where to connect!!");
			System.exit(1);
		} 
		catch (IOException e) {
			System.err.println("ERROR (lookupSocket): Couldn't get I/O for the connection.");
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

            // If user enters local server, process Lookup request and reply
            if (command[0].equals("local") && command.length == 2) {
            	// request to lookup server
            	BrokerPacket packetToLookup = new BrokerPacket();				
				packetToLookup.type = BrokerPacket.LOOKUP_REQUEST;
				packetToLookup.exchange = command[1];
				lookupOut.writeObject(packetToLookup);
			
				// reply from lookup server
				BrokerPacket packetFromLookup;
				packetFromLookup = (BrokerPacket) lookupIn.readObject();
				if (packetFromLookup.type == BrokerPacket.LOOKUP_REPLY) {
            		// using broker ip address and port received from BrokerLookupServer to find location of broker 
            		brokerIpAddress = packetFromLookup.locations[0].broker_host;
            		brokerPort = packetFromLookup.locations[0].broker_port;    
            		try {
            			brokerSocket = new Socket(brokerIpAddress, brokerPort);         		
            			brokerOut = new ObjectOutputStream(brokerSocket.getOutputStream());
                		brokerIn = new ObjectInputStream(brokerSocket.getInputStream());
                	}
                	catch (UnknownHostException e) {
						System.err.println("ERROR (brokerSocket): Don't know where to connect!!");
						System.exit(1);
					} 
					catch (IOException e) {
						System.err.println("ERROR (brokerSocket): Couldn't get I/O for the connection.");
						System.exit(1);
					}
					System.out.println(packetFromLookup.exchange + " as local");
					System.out.print("<CONSOLE>"); 
					continue;
				}
				else if (packetFromLookup.type == BrokerPacket.ERROR_INVALID_EXCHANGE) {
					System.out.println("That broker does not exist");
					System.out.print("<CONSOLE>");
					continue;
            	}
            	else if (packetFromLookup.type == BrokerPacket.BROKER_NULL) {
					System.out.println("Something wrong during packet communication");
					System.out.print("<CONSOLE>"); 
					continue;
            	}
				else {
					System.out.println("BrokerLookupServer is broken! Don't know why!");
					System.out.print("<CONSOLE>"); 
					continue;
				}
			}

			// If user types something other than "local" + "broker name"
			if (!(command[0].equals("local") && command.length == 2)) {
				// If ip address and port are not set
				if (brokerIpAddress == "" || brokerPort == 0) {
					System.out.println("You are not connected to a broker. Use 'local' command to connect to a broker");
					System.out.print("<CONSOLE>"); 
					continue;
				}
				// If gets here, user must have entered stock symbol to get quotes
				else {
					// request to broker server
                	BrokerPacket packetToBroker = new BrokerPacket();
                	packetToBroker.type = BrokerPacket.BROKER_REQUEST;
                	packetToBroker.symbol = userInput.toLowerCase();
                	packetToBroker.locations = new BrokerLocation[1];
            		packetToBroker.locations[0] = new BrokerLocation(brokerIpAddress, brokerPort);
                	brokerOut.writeObject(packetToBroker);
                	
                	// reply from broker server
                	BrokerPacket packetFromBroker;
					packetFromBroker = (BrokerPacket) brokerIn.readObject();
					if (packetFromBroker.type == BrokerPacket.BROKER_QUOTE) {
						System.out.println("Quote from broker: " + packetFromBroker.quote);
						System.out.print("<CONSOLE>"); 
						continue;
            		}
            		else {
            			System.out.println("OnlineBroker is broken! Don't know why!");
						System.exit(-1);
            		}
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
