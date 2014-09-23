/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.db.entity;

import datalogger.server.util.ToString;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 *
 * @author lars
 */
@Entity
public class LogData implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "value")
    private double value;

    @ManyToOne
    @JoinColumn(name = "logtype_id", nullable = true)
    private LogType logType;

    @Column(name = "tstamp")
    private Date tstamp;

    @ManyToOne
    @JoinColumn(name = "logdev_id", nullable = true)
    private LogDevice logDev;

    public Integer getId() {
	return id;
    }

    public void setId(Integer id) {
	this.id = id;
    }

    public double getValue() {
	return value;
    }

    public void setValue(double value) {
	this.value = value;
    }

    public LogType getLogType() {
	return logType;
    }

    public void setLogType(LogType logType) {
	this.logType = logType;
    }

    public Date getTstamp() {
	return tstamp;
    }

    public void setTstamp(Date tstamp) {
	this.tstamp = tstamp;
    }

    public LogDevice getLogDev() {
	return logDev;
    }

    public void setLogDev(LogDevice logDev) {
	this.logDev = logDev;
    }

    @Override
    public int hashCode() {
	int hash = 0;
	hash += (id != null ? id.hashCode() : 0);
	return hash;
    }

    @Override
    public boolean equals(Object object) {
	// TODO: Warning - this method won't work in the case the id fields are not set
	if (!(object instanceof LogData)) {
	    return false;
	}
	LogData other = (LogData) object;
	if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return ToString.toString(this, "-serialVersionUID");
    }

}
