/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package avatar.config;

import java.util.logging.Level;
import java.util.logging.Logger;
import test.uts.aai.pbmn.AutoProcessTestApp;

/**
 *
 * @author ntdun
 */
public class LoggerUtil {
    public static void logAvatar(String logStr) {
        Logger.getLogger(AutoProcessTestApp.class.getName()).log(Level.INFO, logStr, "");
    }

}
