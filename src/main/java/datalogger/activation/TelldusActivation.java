/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.activation;

import com.femtioprocent.fpd.appl.Appl;
import com.femtioprocent.fpd.sundry.S;
import com.femtioprocent.fpd.util.Ansi;
import com.femtioprocent.propaganda.client.PropagandaClient;
import com.femtioprocent.propaganda.connector.Connector_Tcp;
import com.femtioprocent.propaganda.connector.PropagandaConnectorFactory;
import com.femtioprocent.propaganda.data.AddrType;
import static com.femtioprocent.propaganda.data.AddrType.anonymousAddrType;
import static com.femtioprocent.propaganda.data.AddrType.serverAddrType;
import com.femtioprocent.propaganda.data.Datagram;
import com.femtioprocent.propaganda.data.Message;
import com.femtioprocent.propaganda.data.MessageType;
import static com.femtioprocent.propaganda.data.MessageType.register;
import com.femtioprocent.propaganda.exception.PropagandaException;
import datalogger.server.Main;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lars
 */
public class TelldusActivation extends Activation {

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
                    System.err.println("Telldus got: " + datagram);
                    if (datagram == null) {
                        break;
                    }
                    if (standardProcessMessage(datagram, MessageType.plain) == MessageTypeFilter.FILTERED) {
                        System.err.println(name + " got datagram: " + name + " =----> " + datagram);
                        String msg = datagram.getMessage().getMessage();
                        String msgArr[] = datagram.getMessage().getAddendum().split(" ");
                        if ("logged".equals(msg)) {
                            System.err.println(name + " got datagram: _ " + name + " =----> " + datagram);
                        } else {
                            System.err.println(name + " got datagram: _ " + name + " =----> " + datagram);
                        }
                    }
                }
            } catch (PropagandaException ex) {
                System.err.println("TelldusClient: " + ex);
            }
        }
    }

    protected void doTelldus() {
        final Connector_Tcp conn = (Connector_Tcp) PropagandaConnectorFactory.create("Tcp", "Telldus", null, null);
//        Connector_Plain conn = new Connector_Plain("MainPlain");
        final TelldusClient client = new TelldusClient();
        System.err.println("Connect propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
        if (conn.connect(Appl.flags.get("p.host"), Integer.parseInt(Appl.flags.get("p.port")))) {

            client.setConnectorAndAttach(conn);
            System.err.println("conn " + conn);

            Thread th2 = new Thread(() -> client.start());
            th2.start();
            Thread th3 = new Thread(() -> doTelldusScanning(client));
            th3.start();
        } else {
            System.err.println("No connection to propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
            System.exit(1);
        }
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

    public void doTelldusScanning(TelldusClient client) {
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

        mapperList.add(new Mapper(135, "ute1 $W:out", "kjell-TH"));  // $W -> temp | humidity
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
                                    long tivalue = 0;

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
                                        ix = line.indexOf("time=");  // 2016-07-06 15:59:34
                                        if (ix > 0) {
                                            String s1 = line.substring(ix + 5);
                                            int ix2 = s1.indexOf("\t");
                                            String s2 = s1.substring(0, ix2);
                                            System.err.println(" >T> " + s2);
					    Date date = parseDate(s2);
					    tivalue = date.getTime();
                                        }
                                    }
                                    if (tvalue != null) {
                                        String w = map.dest_spec.replace("$W", "temp");
                                        Message rmsg = logMessage("addT " + tivalue + " " + w + " " + tvalue);
                                        System.err.println(Ansi.green("send T " + rmsg));
                                        client.sendMsg(new Datagram(client.getDefaultAddrType(), AddrType.createAddrType("dl-collector-" + hostname
                                                + "@DATALOGGER"), MessageType.plain, rmsg));
                                    }
                                    if (hvalue != null) {
                                        String w = map.dest_spec.replace("$W", "humidity");
                                        Message rmsg = logMessage("addT " + tivalue + " " + w + " " + hvalue);
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

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Date parseDate(String cd) {
        try {
            return Date.from(Instant.from(LocalDateTime.parse(cd, formatter).atZone(ZoneId.systemDefault())));
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void start() {
        doTelldus();
    }

}
