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
import datalogger.server.Main;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.primefaces.push.impl.JSONDecoder;

/**
 *
 * @author lars
 */
public class EliqActivation extends Activation {

    class EliqClient extends PropagandaClient {

        EliqClient() {
            super("EliqClient");
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
                        new Message("dl-eliq-" + hostname
                                + "@DATALOGGER")));
                for (;;) {
                    Datagram datagram = getConnector().recvMsg();
                    S.pL("Eliq got: " + datagram);
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

    protected void doEliq() {
        final Connector_Plain conn = (Connector_Plain) PropagandaConnectorFactory.create("Plain", "Eliq", null, null);
//        Connector_Plain conn = new Connector_Plain("MainPlain");
        final EliqClient client = new EliqClient();
        System.err.println("Connect propaganda: " + Appl.flags.get("p.host") + ' ' + Integer.parseInt(Appl.flags.get("p.port")));
        if (conn.connect(Appl.flags.get("p.host"), Integer.parseInt(Appl.flags.get("p.port")))) {

            client.setConnector(conn);
            conn.attachClient(client);
            S.pL("conn " + conn);

            Thread th2 = new Thread(() -> client.start());
            th2.start();
            Thread th3 = new Thread(() -> doEliqScanning(client));
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

    public void doEliqScanning(EliqClient client) {
        System.err.println("eliqSc 1");
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
                    b = new ProcessBuilder("/usr/local/bin/wget", "-O", "-", "https://my.eliq.se/api/datanow?accesstoken=7095a568f8dc459d88779a1e77fcc8c9");
                } else {
                    b = new ProcessBuilder("/usr/local/bin/wget", "-O", "-", "https://my.eliq.se/api/datanow?accesstoken=7095a568f8dc459d88779a1e77fcc8c9");
                }
                try {
                    final Process pr = b.start();
                    final InputStream inS = pr.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(inS));

                    try {

                        for (;;) {
                            final String line = br.readLine();
                            System.err.println(Ansi.red("got line " + line));
                            // {"channelid":13087,"createddate":"2016-01-19T22:38:52","power":1120.0}
                            if (line == null) {
                                break;
                            }
                            JSONTokener jt = new JSONTokener(line);
                            JSONObject jo = new JSONObject(jt);
                            final double pw = jo.getDouble("power");
                            final String cd = jo.getString("createddate");
                            Date cDate = parseDate(cd);
                            Message rmsg = logMessage("addT " + cDate.getTime() + " elmät el " + pw);
                            System.err.println(Ansi.green("Eliq send T " + rmsg));
                            client.sendMsg(new Datagram(client.getDefaultAddrType(), AddrType.createAddrType("dl-collector-" + hostname
                                    + "@DATALOGGER"), MessageType.plain, rmsg));
                        }
                        br.close();
                    } finally {
                        pr.destroy();
                    }
                    System.err.println("Eliq 4: ");
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (PropagandaException | InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Date parseDate(String cd) {
        try {
            return Date.from(Instant.from(LocalDateTime.parse(cd, formatter).atZone(ZoneId.systemDefault())));
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }


    public void start() {
        doEliq();
    }

}
