package com.edumentic.classbuilder.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class StdInMonitor extends Thread{

    private final StringReadConsumer consumer;
    public StdInMonitor(StringReadConsumer consumer){
        this.consumer = consumer;
    }

    public void run(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        int inputChar;
        try {
            while ((inputChar = reader.read()) != -1) {
                consumer.consumeStdInString(Character.toString((char) inputChar));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
