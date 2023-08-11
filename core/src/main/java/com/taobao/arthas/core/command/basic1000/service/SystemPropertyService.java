package com.taobao.arthas.core.command.basic1000.service;

import java.util.Properties;

public class SystemPropertyService {

    public Properties get(){
        java.util.Properties javaProperties = System.getProperties();
        return  javaProperties;
    }

    public String getByKey(String propertyName){
        String value = System.getProperty(propertyName);
        if (value == null) {
            value = "There is no property with the key "+ propertyName;
        } else {
            value = propertyName + ":" + value;
        }
        return value;
    }

    public void update(String propertyName, String propertyValue){
        System.setProperty(propertyName, propertyValue);
    }

}
