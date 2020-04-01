/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoweka.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ntdun
 */
public class CSVUtils {
    
    public List<String[]> readCSVFile(String fileName) {
        
        List<String[]> listOfRows = new ArrayList<>();
            BufferedReader br = null;
            String line = "";
            String cvsSplitBy = ",";

            try {

                br = new BufferedReader(new FileReader(fileName) );
                while ((line = br.readLine()) != null) {
                    
                    
                    String[] attributes = line.split(cvsSplitBy);
                    //System.out.println("line: " + line);
                    //System.out.println("att: " + attributes.length);
                    
                    listOfRows.add(attributes);
                }
        
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        return listOfRows;
    } 
    
    
    public void convertToCSV(String fileName, List<String[]> data) {
        
        String csvData ="";
        
        for (int i=0;i<data.size();i++) {
            
            String[] str = data.get(i);
            String line="";
            for (String cel : str) {
                line+=cel+",";
            }
            
            line = line.substring(0, line.length()-1);
            if (i!=data.size()-1) {
                csvData += line+"\n";
            } else {
                csvData += line;
            }
            
        }
        
        IOUtils iOUtils = new IOUtils();
        iOUtils.overWriteData(csvData, fileName);
        
    }
    
}
