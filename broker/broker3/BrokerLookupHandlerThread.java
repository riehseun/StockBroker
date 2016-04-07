import java.net.*;
import java.io.*;
import java.util.*;

public class BrokerLookupHandlerThread extends Thread {
	private Socket socket = null;
	private File brokers;

	public BrokerLookupHandlerThread(Socket socket, File brokers) {
		super("BrokerLookupHandlerThread");
		this.socket = socket;
		this.brokers = brokers;
		System.out.println("Created new BrokerLookupHandlerThread to handle client");
	}

	public void run() {
		boolean gotByePacket = false;		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			BrokerPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			
			while ((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
				BrokerPacket packetToClient = new BrokerPacket();
				
                File myFile = brokers;
                File tempFile = new File("temp");
                BufferedReader br = new BufferedReader(new FileReader(myFile));
                BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
                Scanner scanner = new Scanner(System.in);
                String currentLine;

				if (packetFromClient.type == BrokerPacket.LOOKUP_REGISTER) {
					// If broker name already exists in database 
        			if (!brokerName("brokers", packetFromClient).equals("")) {
            			System.out.println(packetFromClient.exchange + " is already in the database");
            			continue;
        			}
        			// If not, register the broker by saving information into a file "brokers"
        			else {
            			while ((currentLine = br.readLine()) != null) {
                			bw.write(currentLine);
                			bw.newLine();
            			}
            			bw.write(packetFromClient.locations[0].broker_host); // write ip address
            			bw.write(" ");
            			bw.write(Integer.toString(packetFromClient.locations[0].broker_port)); // write port
            			bw.write(" ");
            			bw.write(packetFromClient.exchange); // write broker name
            			bw.close();
            			boolean success = tempFile.renameTo(myFile);
            			System.out.println("Registration is completed for " + packetFromClient.exchange);
            			continue;
        			}
				}

                if (packetFromClient.type == BrokerPacket.LOOKUP_REQUEST) {
                	int found = 0;
                	// If packet from BrokerClient gets dropped during communication
                	if (packetFromClient.exchange == null) {
                		packetToClient.type = BrokerPacket.BROKER_NULL;
                		toClient.writeObject(packetToClient);
                   		continue; 
                	}
                	// If packet successfully arrives         	
                	else {
                		while ((currentLine = br.readLine()) != null) {
                			// If broker name provided by the client is found in the file "brokers"
                			if (currentLine.contains(packetFromClient.exchange)) {
                				found = 1;
                                System.out.println("local broker client typed is found!");
                				packetToClient.type = BrokerPacket.LOOKUP_REPLY;
                				String[] splitted = currentLine.split("\\s+"); 
                				// Send ip, port, broker name to the client
                                packetToClient.locations = new BrokerLocation[1];
                                packetToClient.locations[0] = new BrokerLocation(splitted[0], Integer.parseInt(splitted[1]));
                				packetToClient.exchange = splitted[2];
                				toClient.writeObject(packetToClient);
                                continue;
                			}
                		}
                		// If broker name provided by the client does not exist in the file "brokers"
                		if (found == 0) {
                            System.out.println("local broker client typed does not exist");
                			packetToClient.type = BrokerPacket.ERROR_INVALID_EXCHANGE;
                			toClient.writeObject(packetToClient);
                   			continue;    
                   		}
                   		      
                   	}
                   	continue;        	       
                }	
				
                if (packetFromClient.type == BrokerPacket.BROKER_FORWARD) {
                    String filePathLookup = "brokers";
                    FileReader frLookup = new FileReader(filePathLookup);
                    BufferedReader brLookup = new BufferedReader(frLookup);
                    String currentLineLookup;
                    // Find broker ip and port
                    while ((currentLineLookup = brLookup.readLine()) != null) {
                        String brokerIpAddress = packetFromClient.locations[0].broker_host;
                        Integer brokerPort = packetFromClient.locations[0].broker_port;
                        String[] splittedLookup = currentLineLookup.split("\\s+");
                        // Find another broker
                        if (!brokerPort.equals(Integer.parseInt(splittedLookup[1]))
                            || !brokerIpAddress.equals(splittedLookup[0])) {
                            // Change the broker by changing the broker location
                            packetToClient.locations = new BrokerLocation[1];      
                            brokerIpAddress = splittedLookup[0];
                            brokerPort = Integer.parseInt(splittedLookup[1]);
                            packetToClient.locations[0] = new BrokerLocation(brokerIpAddress, brokerPort);
                        }  
                    }
                    packetToClient.type = BrokerPacket.BROKER_FORWARD; 
                    packetToClient.symbol = packetFromClient.symbol;
                    toClient.writeObject(packetToClient);
                    continue;
                }	
                       
				/* Sending an BROKER_NULL || BROKER_BYE means quit */
				if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
					gotByePacket = true;
					packetToClient = new BrokerPacket();
					packetToClient.type = BrokerPacket.BROKER_BYE;
					//packetToClient.message = "Bye!";
					toClient.writeObject(packetToClient);
					break;
				}
				
				/* if code comes here, there is an error in the packet */
				System.err.println("BrokerLookupServer: ERROR Unknown ECHO_* packet!!");
				System.exit(-1);
			}
			
			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}

    // Find name of the broker with provided packet
	public static String brokerName(String lookupTable, BrokerPacket packet) throws FileNotFoundException {
        String filePathLookup = lookupTable;
        FileReader frLookup = new FileReader(filePathLookup);
        BufferedReader brLookup = new BufferedReader(frLookup);
        String currentLineLookup;
        try {
            while ((currentLineLookup = brLookup.readLine()) != null) {
                String[] splittedLookup = currentLineLookup.split("\\s+");
                if (packet.locations[0].broker_port.equals(Integer.parseInt(splittedLookup[1]))
                    && packet.locations[0].broker_host.equals(splittedLookup[0])) {
                    return splittedLookup[2];
                }              
            }
            return "";
        }
        catch(IOException e) {
            System.out.println(e);
            return "exception occured";
        }
    }
}
