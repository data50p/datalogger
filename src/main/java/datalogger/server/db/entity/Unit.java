/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.server.db.entity;

import datalogger.server.util.ToString;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 *
 * @author lars
 */
@Entity
@Table(name = "unit",
    indexes = {
        @Index(columnList = "id", name = "unit_id_idx"),
        @Index(columnList = "name", name = "unit_name_idx", unique = true)}
    )
    public class Unit extends Ent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="name", length = 40)
    private String name;

    @Column(name="description", length = 100)
    private String description;

    @ManyToOne
    @JoinColumn(name="base_unit_id", nullable=true)
    private Unit baseUnit;

    @Column(name="base_factor")
    private Double baseFactor;

    public Unit() {	
    }

    public Unit(String name, String description, Unit baseUnit, double baseFactor) {
	this.name = name;
	this.description = description;
	this.baseUnit = baseUnit;
	this.baseFactor = baseFactor;
    }
    
    @Override
    public boolean isNew() {
	return id == null || id == 0;
    }
    
    public Integer getId() {
	return id;
    }

    public void setId(Integer id) {
	this.id = id;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getDescription() {
	return description;
    }

    public void setDescription(String description) {
	this.description = description;
    }

    public Unit getBaseUnit() {
	return baseUnit;
    }

    public void setBaseUnit(Unit baseUnit) {
	this.baseUnit = baseUnit;
    }

    public Double getBaseFactor() {
	return baseFactor;
    }

    public void setBaseFactor(Double baseFactor) {
	this.baseFactor = baseFactor;
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
	if (!(object instanceof Unit)) {
	    return false;
	}
	Unit other = (Unit) object;
	if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return ToString.toString(this, "-serialVersionUID");
    }

    public boolean newInstance() {
	return id == null || id == 0;
    }

}
