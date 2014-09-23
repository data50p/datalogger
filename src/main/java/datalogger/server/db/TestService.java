/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.db;

import datalogger.server.db.entity.Unit;
import java.util.Date;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 *
 * @author lars
 */
public class TestService extends PersistingService {

    public Unit getSettingsData(final int id) throws TransactionJobException {
        return getSettingsData(id, true);
    }

    private Unit getSettingsData(final int id, boolean touch) throws TransactionJobException {
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

    public Unit saveSettingsData(final Unit u) throws TransactionJobException {
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

    public int removeSettingsData(final int id) throws TransactionJobException {
        isInTransaction();

        Unit u = getSettingsData(id);
        System.err.println("got Unit: " + u);
        if (u != null) {
            em.remove(u);
            return 1;
        }
        return 0;
    }
}
