/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package datalogger.bean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 *
 * @author lars
 */
@ManagedBean(name = "TestBean")
@SessionScoped
public class TestBean {
    private String data = "123";

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
    
    public void button1() {
	this.data = "1000";
    }
}
