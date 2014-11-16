/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server;

import com.femtioprocent.fpd.appl.Appl;
import static com.femtioprocent.fpd.appl.Appl.decodeArgs;
import com.femtioprocent.fpd.sundry.S;
import com.femtioprocent.fpd2.util.MilliTimer;
import com.femtioprocent.propaganda.client.PropagandaClient;
import com.femtioprocent.propaganda.connector.Connector_Plain;
import com.femtioprocent.propaganda.connector.PropagandaConnectorFactory;
import com.femtioprocent.propaganda.data.AddrType;
import static com.femtioprocent.propaganda.data.AddrType.anonymousAddrType;
import static com.femtioprocent.propaganda.data.AddrType.serverAddrType;
import com.femtioprocent.propaganda.data.Datagram;
import com.femtioprocent.propaganda.data.Message;
import com.femtioprocent.propaganda.data.MessageType;
import static com.femtioprocent.propaganda.data.MessageType.register;
import com.femtioprocent.propaganda.exception.PropagandaException;
import datalogger.server.db.BootstrapDB;
import datalogger.server.db.PersistingService;
import datalogger.server.db.PersistingService.TransactionJob;
import datalogger.server.db.DataLoggerService;
import datalogger.server.db.entity.LogCurrentData;
import datalogger.server.db.entity.LogData;
import datalogger.server.db.entity.LogDevice;
import datalogger.server.db.entity.LogType;
import datalogger.server.db.entity.Unit;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lars
 */
public class Main extends Appl {

    public static void main(String[] args) {
        Appl.flags.put("p.host", "127.0.0.1");
        Appl.flags.put("p.port", "8899");
        decodeArgs(args);
        main(new Main());
    }

    @Override
    public void main() {
        if (Appl.flags.get("bootstrap") != null) {
            BootstrapDB b = new BootstrapDB();
            b.update();
            System.exit(0);
        } else if (Appl.flags.get("test") != null) {
            BootstrapDB b = new BootstrapDB();
            int loop = 1;
            try {
                loop = Integer.parseInt(Appl.flags.get("test"));
            } catch (Exception _) {
            }
            MilliTimer mt = new MilliTimer();
            b.test(loop);
            System.err.println("saving total: " + loop + ' ' + mt.getString());
            System.exit(0);
        } else {
            test();
            doCollecting();
            TelldusClient client = doTelldus();
            doTelldusScanning(client);
        }
    }

    private void test() {
        try {
            final DataLoggerService dls = new DataLoggerService();

            Integer n = dls.withTransaction(new TransactionJob<Integer>() {
                @Override
                public Integer exec() {
                    try {
                        Unit u = dls.getUnitByName("mm");
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

    private int log(final String dev, final String type, final double val) {
        try {
            final DataLoggerService dls = new DataLoggerService();

            Integer n = dls.withTransaction(new TransactionJob<Integer>() {
                @Override
                public Integer exec() {
                    try {
                        Unit u = dls.getUnit(3);

                        LogDevice ldev = dls.getLogDeviceByName(dev);
                        LogType ltyp = dls.getLogTypeByName(type);

                        if (ldev != null && ltyp != null) {
                            LogCurrentData lcd = dls.getLogCurrentData(ltyp, ldev);
                            if (lcd == null || lcd.getValue() != val) {
                                
                                LogData ld = new LogData(ldev, ltyp, val);
                                ld = dls.save(ld);
                                
                                if (lcd == null) {
                                    lcd = new LogCurrentData(ldev, ltyp, val);
                                } else {
                                    lcd = new LogCurrentData(lcd, ldev, ltyp, val, "");
                                }
                                dls.save(lcd);
                                
                                System.err.println("DB saved: " + ld + ' ' + lcd);
                                
                                return ld.getId();
                            } else {
                                System.err.println("DB saved: SKIP same " + val + ' ' + lcd);
                                
                            }
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

    class CollectorClient extends PropagandaClient {

        CollectorClient() {
            super("DataloggerServer");
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
                S.pL("MainClient: " + ex);
            }
        }
    }

    class TelldusClient extends PropagandaClient {

        TelldusClient() {
            super("DataloggerClient");
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
                        new Message("dl-telldus-" + hostname
                                + "@DATALOGGER")));
                for (;;) {
                    Datagram datagram = getConnector().recvMsg();
                    S.pL("Telldus got: " + datagram);
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
                        if ("logged".equals(msg)) {
                            System.err.println("got datagram: _ " + name + " =----> " + datagram);
                        } else {
                            System.err.println("got datagram: _ " + name + " =----> " + datagram);
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

    private void doCollecting() {
        final Connector_Plain conn = (Connector_Plain) PropagandaConnectorFactory.create("Plain", "DataLoggerServer", null, null);
//        Connector_Plain conn = new Connector_Plain("MainPlain");
        final CollectorClient client = new CollectorClient();
        System.err.println("Connect propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
        if (conn.connect(Appl.flags.get("p.host"), Integer.parseInt(Appl.flags.get("p.port")))) {

            client.setConnector(conn);
            conn.attachClient(client);
            S.pL("conn " + conn);

            Thread th2 = new Thread(new Runnable() {

                public void run() {
                    client.start();
                }
            });
            th2.start();
        } else {
            System.err.println("No connection to propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
            System.exit(1);
        }
    }

    private TelldusClient doTelldus() {
        final Connector_Plain conn = (Connector_Plain) PropagandaConnectorFactory.create("Plain", "DataLoggerClient", null, null);
//        Connector_Plain conn = new Connector_Plain("MainPlain");
        final TelldusClient client = new TelldusClient();
        System.err.println("Connect propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
        if (conn.connect(Appl.flags.get("p.host"), Integer.parseInt(Appl.flags.get("p.port")))) {

            client.setConnector(conn);
            conn.attachClient(client);
            S.pL("conn " + conn);

            Thread th2 = new Thread(new Runnable() {

                public void run() {
                    client.start();
                }
            });
            th2.start();
            return client;
        } else {
            System.err.println("No connection to propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
            System.exit(1);
        }
        return null;
    }

    private void doTelldusScanning(TelldusClient client) {
        System.err.println("tdSc 1");
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
        }
        for (;;) {
            try {
                System.err.println("tdSc 2");
                TimeUnit.SECONDS.sleep(5);

                double value = 99.99;
                Message rmsg = new Message("log", "add ute1 temp:out " + value);
                System.err.println("tdSc 3");
                client.sendMsg(new Datagram(client.getDefaultAddrType(), AddrType.createAddrType("dl-collector-" + hostname
                        + "@DATALOGGER"), MessageType.plain, rmsg));
                System.err.println("tdSc 4");
            } catch (PropagandaException | InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
