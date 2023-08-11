package com.taobao.arthas.core.command.basic1000.service;

import java.io.File;

public class PwdService {

    public String pwd(){
        String path = new File("").getAbsolutePath();
        return path;
    }
}
