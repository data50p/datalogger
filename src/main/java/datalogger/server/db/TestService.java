/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.db;

import datalogger.server.db.entity.LogData;
import datalogger.server.db.entity.Unit;
import java.util.Date;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 *
 * @author lars
 */
public class TestService extends PersistingService {

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
            return new Unit();
        }
    }

    public Unit saveUnit(final Unit u) throws TransactionJobException {
        isInTransaction();

        Date now = new Date();
        if (u.getId() == 0) {
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
            return new LogData();
        }
    }
}
