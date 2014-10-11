/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.db;

import datalogger.server.Main;
import datalogger.server.db.PersistingService.TransactionJob;
import datalogger.server.db.entity.LogData;
import datalogger.server.db.entity.Unit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lars
 */
public class BootstrapDB {

    String bootstrapUnits[][] = new String[][] {
	{"", "identity", null, "1.0"},
	{"m", "length", null, "1.0"},
	{"mm", "length", "m", "0.001"},
	{"°", "temp", null, "1.0"},
    };
    
    public void update() {
	try {
	    final DataLoggerService dls = new DataLoggerService();

	    Integer n = dls.withTransaction(new TransactionJob<Integer>() {
		@Override
		public Integer perform() {
		    try {
			int n = 0;
			for(String[] bu : bootstrapUnits) {
			    final Unit u = dls.getUnitByName(bu[0]);
			    if ( u == null ) {
				final Unit bUnit = bu[2] == null ? null : dls.getUnitByName(bu[2]);				
				Unit nu = new Unit(bu[0], bu[1], bUnit, Double.parseDouble(bu[3]));
				dls.saveUnit(nu);
			    }
			}
			List<Unit> allUnits = dls.getAllUnits();
			for(Unit u : allUnits) {
			    System.err.println("Found: " + u);
			}
			
			return n;
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
