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
import datalogger.server.db.entity.LogDevice;
import datalogger.server.db.entity.LogType;
import datalogger.server.db.entity.Unit;
import java.awt.TrayIcon;
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

    private int log(String dev, String type, double val) {
	try {
	    DataLoggerService dls = new DataLoggerService();

	    Integer n = dls.withTransaction(new TransactionJob<Integer>() {
		@Override
		public Integer perform() {
		    try {
			Unit u = dls.getUnit(3);

			LogDevice ldev = dls.getLogDevice(dev);
			LogType ltyp = dls.getLogType(type);

			if (ldev != null && ltyp != null) {
			    LogData ld = new LogData(ldev, ltyp, val);
			    ld = dls.saveLogData(ld);
			    System.err.println("saved: " + ld);
			    return ld.getId();
			}
			return 0;
		    } catch (PersistingService.TransactionJobException ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
		    }
		    return null;
		}
	    });
	    System.out.println("sd id: " + n);
	    return n == null ? -1 : n;
	} catch (PersistingService.TransactionJobException ex) {
	    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
	}
	return -1;
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
			    // log add <dev> <type> <value>
			    if (msgArr.length == 4 && msgArr[0].equals("add")) {
				Double val = Double.parseDouble(msgArr[3]);
				int id = log(msgArr[1], msgArr[2], val);
				Message rmsg = new Message("logged", "added id " + id);
				sendMsg(new Datagram(getDefaultAddrType(), datagram.getSender(), MessageType.plain, rmsg));
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
