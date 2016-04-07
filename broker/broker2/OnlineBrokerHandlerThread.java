import java.net.*;
import java.io.*;
import java.util.*;

public class OnlineBrokerHandlerThread extends Thread {
	private Socket socket = null;

	public OnlineBrokerHandlerThread(Socket socket) {
		super("OnlineBrokerHandlerThread");
		this.socket = socket;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {
		boolean gotByePacket = false;		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			BrokerPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			
			while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
				BrokerPacket packetToClient = new BrokerPacket();
				
                File myFile = new File("nasdaq");
                File tempFile = new File("temp");
                BufferedReader br = new BufferedReader(new FileReader(myFile));
                BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
                Scanner scanner = new Scanner(System.in);
                String currentLine;

				if (packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
					// Query the database with symbol provided by client 		
                    long quote = query(packetFromClient);
                    packetToClient.type = BrokerPacket.BROKER_QUOTE;
                    packetToClient.quote = quote;
					toClient.writeObject(packetToClient); /* send reply back to client */
					continue; /* wait for next packet */
				}

				if (packetFromClient.type == BrokerPacket.EXCHANGE_ADD) {
                    // symbol already exists in database
					if (querycheck(packetFromClient) == true) {
                        packetToClient.type = BrokerPacket.ERROR_SYMBOL_EXISTS;
                        toClient.writeObject(packetToClient); /* send reply back to client */
                        continue; /* wait for next packet */
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
					    toClient.writeObject(packetToClient); /* send reply back to client */
					    continue; /* wait for next packet */
				    }
                }	

				if (packetFromClient.type == BrokerPacket.EXCHANGE_REMOVE) {
                    // symbol does not exist in database
                    if (querycheck(packetFromClient) == false) {
                        packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
                        toClient.writeObject(packetToClient);
                        continue; /* wait for next packet */
                    }
                    else {			
					    while ((currentLine = br.readLine()) != null) {
                    	    if (currentLine.contains(packetFromClient.symbol)) {
                    	        continue; /* wait for next packet */
                    	    }
                   		    bw.write(currentLine);
                    	    bw.newLine();
                	    }
                	    bw.close();
                	    boolean success = tempFile.renameTo(myFile);
                        packetToClient.type = BrokerPacket.EXCHANGE_REMOVE;
                        toClient.writeObject(packetToClient); /* send reply back to client */
                        continue; /* wait for next packet */
				    }
                }    

				if (packetFromClient.type == BrokerPacket.EXCHANGE_UPDATE) {
                    // symbol does not exist in database
                    if (querycheck(packetFromClient) == false) {
                        packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
                        toClient.writeObject(packetToClient);
                        continue; /* wait for next packet */
                    }
                    // quote is out of range between [1,300]
					else if (packetFromClient.quote > 300 || packetFromClient.quote < 1) {
						packetToClient.type = BrokerPacket.ERROR_OUT_OF_RANGE;
                        toClient.writeObject(packetToClient);
						continue; /* wait for next packet */
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
                        toClient.writeObject(packetToClient); /* send reply back to client */
                        continue; /* wait for next packet */
                    }
				}

                if (packetFromClient.type == BrokerPacket.EXCHANGE_REPLY) {
                    packetToClient.type = BrokerPacket.EXCHANGE_REPLY; 
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
				System.err.println("ERROR: Unknown ECHO_* packet!!");
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

    // return quote with provided symbol
    public static long query(BrokerPacket packet) throws FileNotFoundException {
        String file_path = "nasdaq";
        FileReader fr = new FileReader(file_path);
        BufferedReader br = new BufferedReader(fr);
        String currentLine;
        try {
            long quote = 0L;
            while ((currentLine = br.readLine()) != null) {
                String[] splitted = currentLine.split("\\s+");
                if (packet.symbol.equals(splitted[0])) {
                    return quote = Long.parseLong(splitted[1], 10);
                }
            }
            return 0L;
        }
        catch(IOException e) {
            System.out.println(e);
            return -1L;
        }
    }
    
    // return true or false depending on whether provided symbol exists in database
    public static boolean querycheck(BrokerPacket packet) throws FileNotFoundException {
        String file_path = "nasdaq";
        FileReader fr = new FileReader(file_path);
        BufferedReader br = new BufferedReader(fr);
        String currentLine;
        try {
            while ((currentLine = br.readLine()) != null) {
                String[] splitted = currentLine.split("\\s+");
                if (packet.symbol.equals(splitted[0])) {
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
