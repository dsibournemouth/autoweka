/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoweka.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author ntdun
 */
public class AutoWekaAvatarLogIntegration {

    private String experimentFolderPath;
    private String seedNumber;
    private boolean isAvatar;

    public AutoWekaAvatarLogIntegration(String experimentFolderPath, String seedNumber, boolean isAvatar) {
        this.experimentFolderPath = experimentFolderPath;
        this.seedNumber = seedNumber;
        this.isAvatar = isAvatar;
    }

    

    public void analyseLogs() {

        String autowekaRunAndResultFilePath = experimentFolderPath + "\\out\\autoweka\\state-run" + seedNumber;

        String pipelineLogFilePath = experimentFolderPath + "\\avatar_log\\pipelines.txt";
        String avatarLogFilePath = experimentFolderPath + "\\avatar_log\\rs-avatar.txt";
        String[] pathnames;

        System.out.println(autowekaRunAndResultFilePath);
        File f = new File(autowekaRunAndResultFilePath);
        pathnames = f.list();

        String rsFileName = "";
        for (String pathname : pathnames) {
            // System.out.println(pathname);
            if (pathname.contains("runs_and_results") && pathname.contains(".csv")) {
                rsFileName = pathname;
                break;
            }
        }

        List<String[]> listOfData = new ArrayList<>();

        String[] headerData = new String[9];
        headerData[0] = "run_number";
        headerData[1] = "configuration_id";
        headerData[2] = "fold_id";
        headerData[3] = "validity";
        headerData[4] = "is_avatar";
        headerData[5] = "wall_clock_time";
        headerData[6] = "run_quality"; 
        headerData[7] = "pipeline_structure";
        headerData[8] = "simplified_structure";

        listOfData.add(headerData);

        if (!rsFileName.equals("")) {
            // System.out.println("FOUND RESULT OUTPUT FILE !!!!! \n\n\n");
            rsFileName = autowekaRunAndResultFilePath + "\\" + rsFileName;

            CSVUtils cSVUtils = new CSVUtils();
            List<String[]> autowekaLogListOfRows = cSVUtils.readCSVFile(rsFileName);
            List<String[]> pipelineLogListOfRows = cSVUtils.readCSVFile(pipelineLogFilePath);
            
            List<String[]> avatarLogListOfRows = null;
            
            if(isAvatar) {
                avatarLogListOfRows = cSVUtils.readCSVFile(avatarLogFilePath);
            }
            

            for (int i = 1; i < autowekaLogListOfRows.size(); i++) {

                String[] mergedData = new String[9];

                mergedData[0] = autowekaLogListOfRows.get(i)[0];
                mergedData[1] = autowekaLogListOfRows.get(i)[1];
                mergedData[2] = autowekaLogListOfRows.get(i)[2];

                mergedData[3] = autowekaLogListOfRows.get(i)[13];
                mergedData[5] = autowekaLogListOfRows.get(i)[15];

                mergedData[6] = autowekaLogListOfRows.get(i)[10];

                mergedData[7] = pipelineLogListOfRows.get(i)[0];

                //System.out.println("i: " + i);

                if (pipelineLogListOfRows.get(i).length>1) {
                    mergedData[8] = pipelineLogListOfRows.get(i)[1];
                } else {
                    mergedData[8] = "";
                }
                
                if (isAvatar) {
                    mergedData[4]= avatarLogListOfRows.get(i)[0];
                } else {
                    mergedData[4] = "";
                }
                
                 

                listOfData.add(mergedData);

            }

        } else {
            //System.out.println("CAN'T FIND RESULT OUTPUT FILE !!!!!");
        }

        String fileName = experimentFolderPath + "\\avatar_log\\mergedLog.csv";

        CSVUtils cSVUtils = new CSVUtils();
        cSVUtils.convertToCSV(fileName, listOfData);

    }

}
