/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.activation;

import com.femtioprocent.fpd.appl.Appl;
import com.femtioprocent.fpd.sundry.S;
import com.femtioprocent.propaganda.client.PropagandaClient;
import com.femtioprocent.propaganda.connector.Connector_Plain;
import com.femtioprocent.propaganda.connector.PropagandaConnectorFactory;
import static com.femtioprocent.propaganda.data.AddrType.anonymousAddrType;
import static com.femtioprocent.propaganda.data.AddrType.serverAddrType;
import com.femtioprocent.propaganda.data.Datagram;
import com.femtioprocent.propaganda.data.Message;
import com.femtioprocent.propaganda.data.MessageType;
import static com.femtioprocent.propaganda.data.MessageType.register;
import com.femtioprocent.propaganda.exception.PropagandaException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

/**
 *
 * @author lars
 */
public class CollectActivation extends Activation {

    class CollectorClient extends PropagandaClient {

	CollectorClient() {
	    super("CollectorClient");
	}

	void start() {
	    try {
		String hostname = "unknown";
		try {
		    hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException ex) {
		}
		sendMsg(new Datagram(anonymousAddrType,
			serverAddrType,
			register,
			new Message("dl-collector-" + hostname
				+ "@DATALOGGER")));
		for (;;) {
		    Datagram datagram = getConnector().recvMsg();
		    S.pL("Collector got: " + datagram);
		    if (datagram == null) {
			break;
		    }
		    if (datagram.getMessageType() == MessageType.ping) {
			sendMsg(new Datagram(getDefaultAddrType(), datagram.getSender(), MessageType.pong, datagram.getMessage()));
			System.err.println("got datagram: " + name + " =----> PING " + datagram);
		    } else if (datagram.getMessageType() == MessageType.pong) {
			System.err.println("got datagram: " + name + " =----> PONG " + datagram);
		    } else if (datagram.getMessageType() == MessageType.plain) {
			System.err.println("got datagram: " + name + " =----> " + datagram);
			String msg = datagram.getMessage().getMessage();
			String msgArr[] = datagram.getMessage().getAddendum().split(" ");
			if ("log".equals(msg)) {
			    // log add <dev> <type> <value>
			    if (msgArr.length == 4 && msgArr[0].equals("add")) {
				try {
				    Double val = Double.parseDouble(msgArr[3].replace(",", "."));
				    int id = log(msgArr[1], msgArr[2], val);
				    Message rmsg;
				    if (id == 0) {
					rmsg = new Message("logged", "NOT added");
				    } else if (id == -1) {
					rmsg = new Message("logged", "SAME not added");
				    } else if (id < 0) {
					rmsg = new Message("logged", "NOT added, error: " + id);
				    } else {
					rmsg = new Message("logged", "added id " + id);
				    }
				    sendMsg(new Datagram(getDefaultAddrType(), datagram.getSender(), MessageType.plain, rmsg));
				} catch (Exception ex) {
				    Message rmsg = new Message("logged", "NOT added " + ex);
				    sendMsg(new Datagram(getDefaultAddrType(), datagram.getSender(), MessageType.plain, rmsg));
				}
                            } else if (msgArr.length == 5 && msgArr[0].equals("addT")) {
                                // log addT <timestamp> <dev> <type> <value>
				try {
                                    Long tm = Long.parseLong(msgArr[1]);
                                    Date timestamp = new Date(tm);
				    Double val = Double.parseDouble(msgArr[4].replace(",", "."));
				    int id = log(msgArr[2], msgArr[3], val, timestamp);
				    Message rmsg;
				    if (id == 0) {
					rmsg = new Message("logged", "NOT added");
				    } else if (id == -1) {
					rmsg = new Message("logged", "SAME not added");
				    } else if (id < 0) {
					rmsg = new Message("logged", "NOT added, error: " + id);
				    } else {
					rmsg = new Message("logged", "added id " + id);
				    }
				    sendMsg(new Datagram(getDefaultAddrType(), datagram.getSender(), MessageType.plain, rmsg));
				} catch (Exception ex) {
				    Message rmsg = new Message("logged", "NOT added " + ex);
				    sendMsg(new Datagram(getDefaultAddrType(), datagram.getSender(), MessageType.plain, rmsg));
				}
			    } else {
				Message rmsg = new Message("error", "format");
				sendMsg(new Datagram(getDefaultAddrType(), datagram.getSender(), MessageType.plain, rmsg));
			    }
			}
		    } else {
			System.err.println("got datagram: _ " + name + " =----> " + datagram);
		    }
		}
	    } catch (PropagandaException ex) {
		S.pL("CollectorClient: " + ex);
	    }
	}
    }

    public void doCollecting() {
	final Connector_Plain conn = (Connector_Plain) PropagandaConnectorFactory.create("Plain", "Collector", null, null);
	final CollectorClient client = new CollectorClient();
	System.err.println("Connect propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
	if (conn.connect(Appl.flags.get("p.host"), Integer.parseInt(Appl.flags.get("p.port")))) {
	    client.setConnector(conn);
	    conn.attachClient(client);
	    S.pL("conn " + conn);
	    Thread th2 = new Thread(() -> client.start());
	    th2.start();
	} else {
	    System.err.println("No connection to propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
	    System.exit(1);
	}
    }

    @Override
    public void start() {
	doCollecting();
    }
}
