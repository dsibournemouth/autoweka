/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoweka.tools;

/**
 *
 * @author ntdun
 */
public class ExpRunner {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        //String experimentFolder = "C:\\DATA\\Projects\\eclipse-workspace\\GITHUB\\autoweka-avatar\\autoweka4mcps\\experiments\\t7";
        //String allocatedMemory = "3076";
        //String seedNumber = "0";
        
        String experimentFolder = args[0];
        String allocatedMemory = args[1];
        String seedNumber = args[2];
        
        
        
        ExpRunnerService expRunnerService = new ExpRunnerService();
        expRunnerService.runExp(experimentFolder,allocatedMemory,seedNumber);
        
        
    }
    
}
