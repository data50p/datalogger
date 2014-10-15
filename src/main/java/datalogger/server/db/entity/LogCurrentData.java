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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "logtype_id", nullable = false)
    private LogType logType;

    @ManyToOne
    @JoinColumn(name = "logdev_id", nullable = false)
    private LogDevice logDev;

    @Column(name = "value")
    private double value;

    @Column(name = "svalue")
    private String svalue;

    @Column(name = "tstamp")
    private Date tstamp;

    @Column(name = "update_counter")
    private int updateCounter;

    @Column(name = "prev_value", nullable = true)
    private Double prevValue;

    @Column(name = "prev_svalue", nullable = true)
    private String prevSvalue;

    @Column(name = "prev_tstamp", nullable = true)
    private Date prevTstamp;

    @Column(name = "note", nullable = true, length = 1024)
    private String note;

    public LogCurrentData() {
    }

    public LogCurrentData(LogDevice logDev, LogType logType) {
	this.logDev = logDev;
	this.logType = logType;
        this.updateCounter = 1;
        this.tstamp = new Date();
    }

    public LogCurrentData(LogDevice logDev, LogType logType, double value) {
	this.logDev = logDev;
	this.logType = logType;
	this.value = value;
        this.updateCounter = 1;
        this.tstamp = new Date();
    }

    public LogCurrentData(LogDevice logDev, LogType logType, String svalue) {
	this.logDev = logDev;
	this.logType = logType;
	this.svalue = svalue;
        this.updateCounter = 1;
        this.tstamp = new Date();
    }

    public LogCurrentData(LogCurrentData prevCurrentData, LogDevice logDev, LogType logType, double value, String svalue) {
        this(prevCurrentData, logDev, logType, value, svalue, new Date());
    }
    
    public LogCurrentData(LogCurrentData prevCurrentData, LogDevice logDev, LogType logType, double value, String svalue, Date tstamp) {
        this.id = prevCurrentData.id;
	this.logDev = logDev;
	this.logType = logType;
	this.value = value;
	this.svalue = svalue;
        this.tstamp = tstamp;
        this.prevValue = prevCurrentData.value;
        this.prevSvalue = prevCurrentData.svalue;
        this.prevTstamp = prevCurrentData.tstamp;
        this.updateCounter = prevCurrentData.updateCounter + 1;
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

    public double getPrevValue() {
        return prevValue;
    }

    public void setPrevValue(double prevValue) {
        this.prevValue = prevValue;
    }

    public String getPrevSvalue() {
        return prevSvalue;
    }

    public void setPrevSvalue(String prevSvalue) {
        this.prevSvalue = prevSvalue;
    }

    public Date getPrevTstamp() {
        return prevTstamp;
    }

    public void setPrevTstamp(Date prevTstamp) {
        this.prevTstamp = prevTstamp;
    }

    public void setPrevValue(Double prevValue) {
        this.prevValue = prevValue;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
