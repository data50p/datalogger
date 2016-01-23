/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server;

import com.femtioprocent.fpd.appl.Appl;
import static com.femtioprocent.fpd.appl.Appl.decodeArgs;
import com.femtioprocent.fpd2.util.MilliTimer;
import datalogger.activation.Activation;
import datalogger.server.db.BootstrapDB;
import datalogger.server.db.DataLoggerService;
import datalogger.server.db.PersistingService;
import datalogger.server.db.PersistingService.TransactionJob;
import datalogger.server.db.entity.LogData;
import datalogger.server.db.entity.Unit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lars
 */
public class Main extends Appl {
    public static String env = "test";
    
    public static void main(String[] args) {
        Appl.flags.put("p.host", "127.0.0.1");
        Appl.flags.put("p.port", "8799");
//        Appl.flags.put("r.host", "");
        decodeArgs(args);
        
        if ( flags.get("env") != null )
            env = flags.get("env");
        
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
	    Activation.runAll();
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
}
