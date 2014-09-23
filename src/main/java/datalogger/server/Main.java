/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server;

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
import datalogger.server.db.PersistingService;
import datalogger.server.db.PersistingService.TransactionJob;
import datalogger.server.db.DataLoggerService;
import datalogger.server.db.entity.LogData;
import datalogger.server.db.entity.Unit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lars
 */
public class Main {

    public static void main(String[] args) {
	Main m = new Main();
	m.test();
	m.init();
	//System.exit(0);
    }

    private void test() {
	try {
	    DataLoggerService dls = new DataLoggerService();

	    Integer n = dls.withTransaction(new TransactionJob<Integer>() {
		@Override
		public Integer perform() {
		    try {
			Unit u = dls.getUnit(3);
			LogData ld = dls.getLogData(1, false);
			return u.getId();
		    } catch (PersistingService.TransactionJobException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		    }
		    return null;
		}
	    });
	    System.out.println("sd " + n);
	} catch (PersistingService.TransactionJobException ex) {
	    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
	}
    }

    private void log(String dev, String val) {
	try {
	    DataLoggerService dls = new DataLoggerService();

	    Integer n = dls.withTransaction(new TransactionJob<Integer>() {
		@Override
		public Integer perform() {
		    try {
			Unit u = dls.getUnit(3);
			LogData ld = new LogData();
			ld.setValue(Double.parseDouble(val));
			ld = dls.saveLogData(ld);
			System.err.println("saved: " + ld);
			return u.getId();
		    } catch (PersistingService.TransactionJobException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		    }
		    return null;
		}
	    });
	    System.out.println("sd " + n);
	} catch (PersistingService.TransactionJobException ex) {
	    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
	}
    }

    class MainClient extends PropagandaClient {

	MainClient() {
	    super("MainClient");
	}

	void start() {
	    try {
		sendMsg(new Datagram(anonymousAddrType,
			serverAddrType,
			register,
			new Message("dl@DATALOGGER")));
		for (;;) {
		    Datagram datagram = getConnector().recvMsg();
		    S.pL("Connector_Plain.Main got: " + datagram);
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
			    log("testdev", "321.123");
			}
		    } else {
			System.err.println("got datagram: _ " + name + " =----> " + datagram);
		    }
		}
	    } catch (PropagandaException ex) {
		S.pL("MainClient: " + ex);
	    }
	}
    }

    private void init() {
//	final Connector_Plain connector = (Connector_Plain) PropagandaConnectorFactory.create("Plain", "DataLoggerServer", null, null);
	Connector_Plain conn = new Connector_Plain("MainPlain");
	MainClient client = new MainClient();
	conn.connect("127.0.0.1", 8899);

	client.setConnector(conn);
	conn.attachClient(client);
	S.pL("conn " + conn);

//	connector.connect();
//	System.err.println("connector " + connector);
	Thread th2 = new Thread(new Runnable() {

	    public void run() {
		client.start();
	    }
	});
	th2.start();
    }
}
