/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.db;

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

    public Unit saveUnit(final Unit u) throws TransactionJobException {
        isInTransaction();

        Date now = new Date();
        if (u.newInstance()) {
            em.persist(u);
            em.flush();
            return u;
        } else {
            final Unit msd = em.merge(u);
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

    public LogData saveLogData(LogData ld) throws TransactionJobException {
        isInTransaction();

        Date now = new Date();
	if ( ld.getTstamp() == null )
	    ld.setTstamp(now);
	
        if (ld.getId() == null) {
            em.persist(ld);
            em.flush();
            return ld;
        } else {
            final LogData mld = em.merge(ld);
            em.flush();
            return mld;
        }
    }

    public LogDevice getLogDevice(String devName) throws TransactionJobException {
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

    public LogType getLogType(String typeName) throws TransactionJobException {
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

    List<Unit> getAllUnits() throws TransactionJobException {
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

    Unit getUnitByName(String name) throws TransactionJobException {
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
}
