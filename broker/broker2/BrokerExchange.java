import java.io.*;
import java.net.*;
import java.util.Scanner;

public class BrokerExchange {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		Socket brokerSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			/* variables for hostname/port */
			String hostname = "localhost";
			int port = 4444;
			
			if(args.length == 2 ) {
				hostname = args[0];
				port = Integer.parseInt(args[1]);
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			brokerSocket = new Socket(hostname, port);

			out = new ObjectOutputStream(brokerSocket.getOutputStream());
			in = new ObjectInputStream(brokerSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;

		System.out.print("CONSOLE>");
		while ((userInput = stdIn.readLine()) != null
				&& userInput.toLowerCase().indexOf("bye") == -1) {
            // If user presses "x", exit
            if (userInput.equals("x")) {
                System.exit(0);
            }
           
            /* make a new request packet */
            BrokerPacket packetToServer = new BrokerPacket();

            // figure out user command & validate user inputs           
            String[] command = userInput.split("\\s+");
            if (command[0].equals("add") && command.length == 2) {
                packetToServer.type = BrokerPacket.EXCHANGE_ADD;
                packetToServer.symbol = command[1];
            }
            else if (command[0].equals("remove") && command.length == 2) {
                packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
                packetToServer.symbol = command[1];
            }
            else if (command[0].equals("update") && command.length == 3) {
                packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
                packetToServer.symbol = command[1];
                packetToServer.quote = Long.parseLong(command[2], 10);
            }
            else {
            	packetToServer.type = BrokerPacket.EXCHANGE_REPLY;
            }
            out.writeObject(packetToServer);

			/* print server reply */
			BrokerPacket packetFromServer;
			packetFromServer = (BrokerPacket) in.readObject();
                        			
            if (packetFromServer.type == BrokerPacket.EXCHANGE_ADD) {
				System.out.println(packetToServer.symbol + " added");
            }
            if (packetFromServer.type == BrokerPacket.EXCHANGE_REMOVE) {
                System.out.println(packetToServer.symbol + " removed");
            }
            if (packetFromServer.type == BrokerPacket.EXCHANGE_UPDATE) {
                System.out.println(packetToServer.symbol + " updated to " + packetToServer.quote);
            }
            if (packetFromServer.type == BrokerPacket.ERROR_INVALID_SYMBOL) {
                System.out.println(packetToServer.symbol + " invalid");
            }
            if (packetFromServer.type == BrokerPacket.ERROR_OUT_OF_RANGE) {
                System.out.println(packetToServer.symbol + " out of range");
            }
            if (packetFromServer.type == BrokerPacket.ERROR_SYMBOL_EXISTS) {
                System.out.println(packetToServer.symbol + " exists");
            }
            if (packetFromServer.type == BrokerPacket.EXCHANGE_REPLY) {
                System.out.println("Wrong Input Stupid!");
            }
            
			/* re-print console prompt */
			System.out.print("CONSOLE>");
		}

		/* tell server that i'm quitting */
		BrokerPacket packetToServer = new BrokerPacket();
		packetToServer.type = BrokerPacket.BROKER_BYE;
		//packetToServer.message = "Bye!";
		out.writeObject(packetToServer);

		out.close();
		in.close();
		stdIn.close();
		brokerSocket.close();
	}
}
