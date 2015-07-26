/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.activation;

import datalogger.activation.TelldusActivation.TelldusClient;
import datalogger.server.Main;
import datalogger.server.db.DataLoggerService;
import datalogger.server.db.PersistingService;
import datalogger.server.db.entity.LogCurrentData;
import datalogger.server.db.entity.LogData;
import datalogger.server.db.entity.LogDevice;
import datalogger.server.db.entity.LogType;
import datalogger.server.db.entity.Unit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lars
 */
public abstract class Activation {

    public static void runAll() {
	CollectActivation ca = new CollectActivation();
	ca.start();

	CrontabActivation cta = new CrontabActivation();
	cta.start();

	TelldusActivation tda = new TelldusActivation();
	tda.start();
    }

    public abstract void start();
    
    protected int log(final String dev, final String type, final double val) {
	try {
	    final DataLoggerService dls = new DataLoggerService();

	    Integer n = dls.withTransaction(new PersistingService.TransactionJob<Integer>() {
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

}
