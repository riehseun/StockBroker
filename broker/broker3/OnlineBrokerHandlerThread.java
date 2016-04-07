import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Scanner;

public class OnlineBrokerHandlerThread extends Thread {
	private Socket socket = null;
    private Socket lookupSocket = null;
    private ObjectOutputStream lookupOut = null;
    private ObjectInputStream lookupIn = null;

	public OnlineBrokerHandlerThread(Socket socket, Socket lookupSocket, ObjectOutputStream lookupOut, ObjectInputStream lookupIn) {
		super("OnlineBrokerHandlerThread");
		this.socket = socket;
        this.lookupSocket = lookupSocket;
        this.lookupOut = lookupOut;
        this.lookupIn = lookupIn;
		System.out.println("Created new OnlineBrokerHandlerThread to handle client");
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

				if (packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
					// If symbol exists in local broker
                    if (querycheck(packetFromClient) == true) {
                        long quote = query(packetFromClient);
                        packetToClient.type = BrokerPacket.BROKER_QUOTE;
                        packetToClient.quote = quote;
                        toClient.writeObject(packetToClient); 
                        continue; 
                    }
                    // If symbol does not exist in local broker
                    else {
                        // If there is only one broker
                        if (count("brokers") == 1) {
                            long quote = 0L;
                            packetToClient.quote = quote;
                            packetToClient.type = BrokerPacket.BROKER_QUOTE;
                            toClient.writeObject(packetToClient);
                            continue; 
                        }
                        // If there is at least one other broker to forward the packet
                        else {
                            // request to lookup server
                            BrokerPacket packetToLookup = new BrokerPacket();
                            packetToLookup.type = BrokerPacket.BROKER_FORWARD;
                            packetToLookup.symbol = packetFromClient.symbol;
                            packetToLookup.exchange = brokerName("brokers", packetFromClient);
                            packetToLookup.locations = new BrokerLocation[1];
                            String brokerIpAddress = packetFromClient.locations[0].broker_host;
                            Integer brokerPort = packetFromClient.locations[0].broker_port;
                            packetToLookup.locations[0] = new BrokerLocation(brokerIpAddress, brokerPort);
                            lookupOut.writeObject(packetToLookup);

                            // reply from lookup server
                            BrokerPacket packetFromLookup;
                            packetFromLookup = (BrokerPacket) lookupIn.readObject();
                            if (packetFromLookup.type == BrokerPacket.BROKER_FORWARD) {
                                long quote = query(packetFromLookup);
                                packetToClient.quote = quote;
                                packetToClient.type = BrokerPacket.BROKER_QUOTE;
                                toClient.writeObject(packetToClient);
                                continue; 
                            }
                            else {
                                packetToClient.type = BrokerPacket.BROKER_NULL;
                                toClient.writeObject(packetToClient);
                                continue; 
                            } 
                        }
                    }
                }

				if (packetFromClient.type == BrokerPacket.EXCHANGE_ADD) {
                    // If broker name cannot be found (Client does not know correct broker name)
                    if (brokerName("brokers", packetFromClient).equals("")) {
                        packetToClient.type = BrokerPacket.ERROR_INVALID_EXCHANGE;
                        toClient.writeObject(packetToClient); 
                        continue;
                    }
                    File myFile = new File(brokerName("brokers", packetFromClient));
                    File tempFile = new File("temp");
                    BufferedReader br = new BufferedReader(new FileReader(myFile));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
                    Scanner scanner = new Scanner(System.in);
                    String currentLine;
                    // If symbol already exists in the database
					if (querycheck(packetFromClient) == true) {
                        packetToClient.type = BrokerPacket.ERROR_SYMBOL_EXISTS;
                        packetToClient.symbol = packetFromClient.symbol;
                        toClient.writeObject(packetToClient); 
                        continue; 
                    }
                    else {
            		    while ((currentLine = br.readLine()) != null) {
                   		    bw.write(currentLine);
                    	    bw.newLine();
                	    }
                        bw.write(packetFromClient.symbol);
                	    bw.close();
                	    boolean success = tempFile.renameTo(myFile);
                        packetToClient.type = BrokerPacket.EXCHANGE_ADD;
					    toClient.writeObject(packetToClient); 
					    continue; 
				    }
                }	

				if (packetFromClient.type == BrokerPacket.EXCHANGE_REMOVE) {
                    // If broker name cannot be found (Client does not know correct broker name)
                    if (brokerName("brokers", packetFromClient).equals("")) {
                        packetToClient.type = BrokerPacket.ERROR_INVALID_EXCHANGE;
                        toClient.writeObject(packetToClient); 
                        continue;
                    }
                    File myFile = new File(brokerName("brokers", packetFromClient));
                    File tempFile = new File("temp");
                    BufferedReader br = new BufferedReader(new FileReader(myFile));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
                    Scanner scanner = new Scanner(System.in);
                    String currentLine;
                    // If symbol does not exist in the database
                    if (querycheck(packetFromClient) == false) {
                        packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
                        packetToClient.symbol = packetFromClient.symbol;
                        toClient.writeObject(packetToClient);
                        continue; 
                    }
                    else {			
					    while ((currentLine = br.readLine()) != null) {
                    	    if (currentLine.contains(packetFromClient.symbol)) {
                    	        continue;
                    	    }
                   		    bw.write(currentLine);
                    	    bw.newLine();
                	    }
                	    bw.close();
                	    boolean success = tempFile.renameTo(myFile);
                        packetToClient.type = BrokerPacket.EXCHANGE_REMOVE;
                        packetToClient.symbol = packetFromClient.symbol;
                        toClient.writeObject(packetToClient); 
                        continue; 
				    }
                }    

				if (packetFromClient.type == BrokerPacket.EXCHANGE_UPDATE) {
                    // If broker name cannot be found (Client does not know correct broker name)
                    if (brokerName("brokers", packetFromClient).equals("")) {
                        packetToClient.type = BrokerPacket.ERROR_INVALID_EXCHANGE;
                        toClient.writeObject(packetToClient); 
                        continue;
                    }
                    File myFile = new File(brokerName("brokers", packetFromClient));
                    File tempFile = new File("temp");
                    BufferedReader br = new BufferedReader(new FileReader(myFile));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
                    Scanner scanner = new Scanner(System.in);
                    String currentLine;
                    // If symbol does not exist in the database
                    if (querycheck(packetFromClient) == false) {
                        packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
                        toClient.writeObject(packetToClient);
                        continue; 
                    }
                    // If the quote is out of range between [1,300]
					else if (packetFromClient.quote > 300 || packetFromClient.quote < 1) {
						packetToClient.type = BrokerPacket.ERROR_OUT_OF_RANGE;
                        toClient.writeObject(packetToClient);
						continue; 
					}
                    else {
            		    while ((currentLine = br.readLine()) != null) {
                    	    if (currentLine.contains(packetFromClient.symbol)) {
                      		    bw.write(packetFromClient.symbol);
                      		    bw.write(" ");
                      		    bw.write(Long.toString(packetFromClient.quote));
                      		    bw.newLine();
                      		    continue;
                    	    }
                   		    bw.write(currentLine);
                    	    bw.newLine();
                	    }
                	    bw.close();
                	    boolean success = tempFile.renameTo(myFile);
                        packetToClient.type = BrokerPacket.EXCHANGE_UPDATE; 
                        toClient.writeObject(packetToClient); 
                        continue; 
                    }
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
				System.err.println("OnlineBroker: ERROR Unknown ECHO_* packet!!");
				System.exit(-1);
			}
			
			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();
            lookupSocket.close();
            lookupIn.close();
            lookupOut.close();


		} catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}

    public static int count(String file) throws FileNotFoundException {
        int count = 0;
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        try {
            while(br.readLine() != null) {    
                count++;
            }
            return count;
        } 
        catch(IOException e) {
            System.out.println(e);
            return count;
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
                System.out.println(packet.locations[0].broker_port);
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

    // return quote with the provided symbol
    public static long query(BrokerPacket packet) throws FileNotFoundException {
        String filePathBroker = brokerName("brokers", packet);
        FileReader frBroker = new FileReader(filePathBroker);
        BufferedReader brBroker = new BufferedReader(frBroker);
        String currentLineBroker;
        try {
            long quote = 0L;
            while ((currentLineBroker = brBroker.readLine()) != null) {
                String[] splittedBroker = currentLineBroker.split("\\s+");
                if (packet.symbol.equals(splittedBroker[0])) {
                    return quote = Long.parseLong(splittedBroker[1], 10);
                }
            }
            return 0L;
        }
        catch(IOException e) {
            System.out.println(e);
            return -1L;
        }
    }
    
    // return true or false depending on whether provided string exists in the file
    public static boolean querycheck(BrokerPacket packet) throws FileNotFoundException {
        String filePathBroker = brokerName("brokers", packet);
        FileReader frBroker = new FileReader(filePathBroker);
        BufferedReader brBroker = new BufferedReader(frBroker);
        String currentLineBroker;
        try {
            while ((currentLineBroker = brBroker.readLine()) != null) {
                String[] splittedBroker = currentLineBroker.split("\\s+");
                if (packet.symbol.equals(splittedBroker[0])) {
                    return true;
                }
            }
            return false;
        }
        catch(IOException e) {
            System.out.println(e);
            return false;
        }
    }
}
