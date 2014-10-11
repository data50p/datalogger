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
public class LogCurrentData implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "value")
    private double value;

    @Column(name = "svalue")
    private String svalue;

    @ManyToOne
    @JoinColumn(name = "logtype_id", nullable = true)
    private LogType logType;

    @Column(name = "tstamp")
    private Date tstamp;

    @Column(name = "update_counter")
    private int updateCounter;

    @ManyToOne
    @JoinColumn(name = "logdev_id", nullable = true)
    private LogDevice logDev;

    public LogCurrentData() {
    }

    public LogCurrentData(LogDevice logDev, LogType logType) {
	this.logDev = logDev;
	this.logType = logType;
    }

    public LogCurrentData(LogDevice logDev, LogType logType, double value) {
	this.logDev = logDev;
	this.logType = logType;
	this.value = value;
    }

    public LogCurrentData(LogDevice logDev, LogType logType, String svalue) {
	this.logDev = logDev;
	this.logType = logType;
	this.svalue = svalue;
    }

    
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

    public String getSvalue() {
	return svalue;
    }

    public void setSvalue(String svalue) {
	this.svalue = svalue;
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

    public int getUpdateCounter() {
	return updateCounter;
    }

    public void setUpdateCounter(int updateCounter) {
	this.updateCounter = updateCounter;
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
	if (!(object instanceof LogCurrentData)) {
	    return false;
	}
	LogCurrentData other = (LogCurrentData) object;
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
