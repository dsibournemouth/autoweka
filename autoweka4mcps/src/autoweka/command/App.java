/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoweka.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ntdun
 */
public class App {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        
        String expConfigFolder = "C:\\DATA\\Projects\\eclipse-workspace\\GITHUB\\autoweka-avatar\\autoweka4mcps\\listofexperiments.properties";

        
         try {

            File f = new File(expConfigFolder);

            BufferedReader b = new BufferedReader(new FileReader(f));

            String readLine = "";

            System.out.println("Reading file using Buffered Reader");

            while ((readLine = b.readLine()) != null) {
                if (!readLine.trim().equals("")) {
                
                    String[] configs = readLine.split(",");
                    String expFolder = configs[0];
                    String seedNumber = configs[1];
                    System.out.println("Running: " + expFolder);
                    CommandExperimentRun cer = new CommandExperimentRun(expFolder, seedNumber);
                    cer.runExp();
                    
                    while (!cer.isIsCompleted()) {
                        try {
                            Thread.sleep(120000);
                            //System.out.println("Sleep ...");
                        } catch (InterruptedException ex) {
                            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                    }
                    System.out.println("Completed: " + expFolder);
                    
                    
                }
                
                
                
            }
            
             System.out.println("DONE!");

        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
    }
    
}
