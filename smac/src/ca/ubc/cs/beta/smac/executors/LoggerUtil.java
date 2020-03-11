package ca.ubc.cs.beta.smac.executors;


import java.util.logging.Level;
import java.util.logging.Logger;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ntdun
 */
public class LoggerUtil {
  
    public static void log(String logStr) {
        Logger.getLogger(LoggerUtil.class.getName()).log(Level.INFO, logStr, "");
    }



    
}
