/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.autoweka.utils;

import autoweka.utils.AutoWekaAvatarLogIntegration;

/**
 *
 * @author ntdun
 */
public class TestAutoWekaAvatarLogIntegration {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        String experimentFolderPath ="C:\\experiments\\results\\autoweka4mcps-avatar\\results-autoweka4mcps-avatar\\smac5_2h\\smac5_2h_avatar_abalone_seed0";
        String seedNumber = "0";
        boolean isAvatar = true;
        
        AutoWekaAvatarLogIntegration log = new AutoWekaAvatarLogIntegration(experimentFolderPath, seedNumber, isAvatar);
        log.analyseLogs();
        
    }
    
}
