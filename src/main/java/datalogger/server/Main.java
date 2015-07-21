/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server;

import com.femtioprocent.fpd.appl.Appl;
import static com.femtioprocent.fpd.appl.Appl.decodeArgs;
import com.femtioprocent.fpd.sundry.S;
import com.femtioprocent.fpd.util.Ansi;
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
import datalogger.server.event.Crontab;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.SchedulerException;

/**
 *
 * @author lars
 */
public class Main extends Appl {

    public static void main(String[] args) {
        Appl.flags.put("p.host", "127.0.0.1");
        Appl.flags.put("p.port", "8799");
//        Appl.flags.put("r.host", "");
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
            doCrontab();
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
                                return -1;
                            }
                        } else {
                            System.err.println("DB ignore: unknown " + dev + ' ' + type);
                        }
                        return 0;
                    } catch (PersistingService.TransactionJobException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return null;
                }
            });
            System.out.println("sd id: " + n);
            return n == null ? -2 : n;
        } catch (PersistingService.TransactionJobException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -3;
    }

    private void doCrontab() {
        final Connector_Plain conn = (Connector_Plain) PropagandaConnectorFactory.create("Plain", "Crontab", null, null);
//        Connector_Plain conn = new Connector_Plain("MainPlain");
        final CrontabClient client = new CrontabClient();
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

    class TelldusClient extends PropagandaClient {

        TelldusClient() {
            super("TelldusClient");
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
                S.pL("TelldusClient: " + ex);
            }
        }
    }

    class CrontabClient extends PropagandaClient {

        CrontabClient() {
            super("CrontabClient");
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
                        new Message("dl-crontab-" + hostname
                                + "@DATALOGGER")));

                try {
                    AtomicInteger cnt = new AtomicInteger();
                    Crontab instance = new Crontab();
                    instance.register("datalogger", "0/10 * * * * ?", () -> {
                        try {
                            sendMsg(new Datagram(getDefaultAddrType(), AddrType.createAddrType("*@DATA*"), MessageType.plain, new Message("Crontab trigger")));
                        } catch (PropagandaException ex) {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    });
                } catch (SchedulerException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }

                for (;;) {
                    Datagram datagram = getConnector().recvMsg();
                    S.pL("Crontab got: " + datagram);
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
                        if ("xxxxxxxxx".equals(msg)) {
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
                S.pL("CrontabClient: " + ex);
            }
        }
    }

    private void doCollecting() {
        final Connector_Plain conn = (Connector_Plain) PropagandaConnectorFactory.create("Plain", "Collector", null, null);
//        Connector_Plain conn = new Connector_Plain("MainPlain");
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

    private TelldusClient doTelldus() {
        final Connector_Plain conn = (Connector_Plain) PropagandaConnectorFactory.create("Plain", "Telldus", null, null);
//        Connector_Plain conn = new Connector_Plain("MainPlain");
        final TelldusClient client = new TelldusClient();
        System.err.println("Connect propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
        if (conn.connect(Appl.flags.get("p.host"), Integer.parseInt(Appl.flags.get("p.port")))) {

            client.setConnector(conn);
            conn.attachClient(client);
            S.pL("conn " + conn);

            Thread th2 = new Thread(() -> client.start());
            th2.start();
            return client;
        } else {
            System.err.println("No connection to propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
            System.exit(1);
        }
        return null;
    }

    private Message logMessage(String a) {
        return new Message("log", a);
    }

    static class Mapper {

        int id;
        String dest_spec;
        String hwid;

        public Mapper(int id, String dest_spec, String hwid) {
            this.id = id;
            this.dest_spec = dest_spec;
            this.hwid = hwid;
        }

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

        List<Mapper> mapperList = new ArrayList<Mapper>();

        mapperList.add(new Mapper(135, "ute1 $W:out", "kjell-TH"));
        mapperList.add(new Mapper(151, "inne1 $W:in", "kjell-TH"));

        for (;;) {
            try {

                TimeUnit.SECONDS.sleep(5);

                ProcessBuilder b;
                if (Appl.flags.get("r.host") != null) {
                    b = new ProcessBuilder("ssh", Appl.flags.get("r.host"), "/usr/local/bin/tdtool", "--list-sensors");
                } else {
                    b = new ProcessBuilder("/usr/local/bin/tdtool", "--list-sensors");
                }
                try {
                    final Process pr = b.start();
                    final InputStream inS = pr.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(inS));

                    try {

                        for (;;) {
                            final String line = br.readLine();
                            System.err.println(Ansi.red("got line " + line));
                            if (line == null) {
                                break;
                            }

                            for (Mapper map : mapperList) {
                                int collectId = map.id;

                                if (map.hwid.equals("kjell-TH")) {
                                    Double tvalue = null;
                                    Double hvalue = null;

                                    if (line.contains("id=" + collectId)) {
                                        int ix = line.indexOf("temperature=");
                                        if (ix > 0) {
                                            String s1 = line.substring(ix + 12);
                                            int ix2 = s1.indexOf("\t");
                                            String s2 = s1.substring(0, ix2);
                                            System.err.println(" >t> " + s2);
                                            tvalue = Double.valueOf(s2);
                                        }
                                        ix = line.indexOf("humidity=");
                                        if (ix > 0) {
                                            String s1 = line.substring(ix + 9);
                                            int ix2 = s1.indexOf("\t");
                                            String s2 = s1.substring(0, ix2);
                                            System.err.println(" >h> " + s2);
                                            hvalue = Double.valueOf(s2);
                                        }
                                    }
                                    if (tvalue != null) {
                                        String w = map.dest_spec.replace("$W", "temp");
                                        Message rmsg = logMessage("add " + w + " " + tvalue);
                                        System.err.println(Ansi.green("send T " + rmsg));
                                        client.sendMsg(new Datagram(client.getDefaultAddrType(), AddrType.createAddrType("dl-collector-" + hostname
                                                + "@DATALOGGER"), MessageType.plain, rmsg));
                                    }
                                    if (hvalue != null) {
                                        String w = map.dest_spec.replace("$W", "humidity");
                                        Message rmsg = logMessage("add " + w + " " + hvalue);
                                        System.err.println(Ansi.green("send H " + rmsg));
                                        client.sendMsg(new Datagram(client.getDefaultAddrType(), AddrType.createAddrType("dl-collector-" + hostname
                                                + "@DATALOGGER"), MessageType.plain, rmsg));
                                    }
                                }
                            }
                        }
                        br.close();
                    } finally {
                        pr.destroy();
                    }
                    System.err.println("tdSc 4");
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (PropagandaException | InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
