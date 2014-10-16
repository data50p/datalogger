/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.db;

import datalogger.server.db.entity.Ent;
import datalogger.server.db.entity.LogCurrentData;
import datalogger.server.db.entity.LogData;
import datalogger.server.db.entity.LogDevice;
import datalogger.server.db.entity.LogType;
import datalogger.server.db.entity.Unit;
import java.util.Date;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 *
 * @author lars
 */
public class DataLoggerService extends PersistingService {

    public Unit getUnit(final int id) throws TransactionJobException {
        return getUnit(id, true);
    }

    private Unit getUnit(final int id, boolean touch) throws TransactionJobException {
        isInTransaction();
        
        try {
            Query q = em.createQuery("select u from Unit u where u.id = ?1");
            q.setParameter(1, id);
            Unit u = (Unit) q.getSingleResult();
            System.err.println("got Unit: " + u);
            if (touch && u != null) {
                em.flush();
            }
            return u;
        } catch (NoResultException ex) {
            return null;
        }
    }

    public <T extends Ent> T save(final T u) throws TransactionJobException {
        isInTransaction();

        Date now = new Date();
        if (u.isNew()) {
            em.persist(u);
            em.flush();
            return u;
        } else {
            final T msd = em.merge(u);
            em.flush();
            return msd;
        }
    }

    public int removeUnit(final int id) throws TransactionJobException {
        isInTransaction();

        Unit u = getUnit(id);
        System.err.println("got Unit: " + u);
        if (u != null) {
            em.remove(u);
            return 1;
        }
        return 0;
    }

    public LogData getLogData(final int id, boolean touch) throws TransactionJobException {
        isInTransaction();
        
        try {
            Query q = em.createQuery("select ld from LogData ld where ld.id = ?1");
            q.setParameter(1, id);
            LogData ld = (LogData) q.getSingleResult();
            System.err.println("got: " + ld);
            if (touch && ld != null) {
                em.flush();
            }
            return ld;
        } catch (NoResultException ex) {
            return null;
        }
    }

    public LogDevice getLogDeviceByName(String devName) throws TransactionJobException {
        isInTransaction();
        
        try {
            Query q = em.createQuery("select ld from LogDevice ld where ld.name = ?1");
            q.setParameter(1, devName);
            LogDevice ld = (LogDevice) q.getSingleResult();
            System.err.println("got: " + ld);
            return ld;
        } catch (NoResultException ex) {
            return null;
        }
    }

    public LogType getLogTypeByName(String typeName) throws TransactionJobException {
        isInTransaction();
        
        try {
            Query q = em.createQuery("select lt from LogType lt where lt.name = ?1");
            q.setParameter(1, typeName);
            LogType lt = (LogType) q.getSingleResult();
            System.err.println("got: " + lt);
            return lt;
        } catch (NoResultException ex) {
            return null;
        }
    }

    public List<Unit> getAllUnits() throws TransactionJobException {
        isInTransaction();
        
        try {
            Query q = em.createQuery("select u from Unit u");
            List<Unit> rs = (List<Unit>) q.getResultList();
            System.err.println("got: " + rs);
            return rs;
        } catch (NoResultException ex) {
            return null;
        }	
    }

    public Unit getUnitByName(String name) throws TransactionJobException {
        isInTransaction();
        
        try {
            Query q = em.createQuery("select u from Unit u where u.name = ?1");
	    q.setParameter(1, name);
            Unit u = (Unit) q.getSingleResult();
            System.err.println("got: " + u);
            return u;
        } catch (NoResultException ex) {
            return null;
        }	
    }

    public LogCurrentData getLogCurrentData(LogType t, LogDevice d) throws TransactionJobException {
        isInTransaction();
        
        try {
            Query q = em.createQuery("select lcd from LogCurrentData lcd where lcd.logType = ?1 and lcd.logDev = ?2");
            q.setParameter(1, t);
            q.setParameter(2, d);
            LogCurrentData lcd = (LogCurrentData) q.getSingleResult();
            System.err.println("got: " + lcd);
            return lcd;
        } catch (NoResultException ex) {
            return null;
        }
    }
    
    void save(LogCurrentData ncd) throws TransactionJobException {
        isInTransaction();
        
        try {
            if ( ncd.getId() == null )
                em.persist(ncd);
            else
                ncd = em.merge(ncd);
            System.err.println("saved: " + ncd);
        } catch (NoResultException ex) {
        }
    }
}
