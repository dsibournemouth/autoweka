/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoweka.utils;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Jun
 */
public class IOUtils {
  


    public IOUtils() {
    
      
        
    }

  
    
  
    

    public void writeData(String data, String fileName) {

       

        FileWriter fstream;
        try {
            fstream = new FileWriter(fileName, true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(data);

            out.close();
        } catch (IOException ex) {
        }
    }
    
    public void overWriteData(String data, String fileName) {

        FileWriter fstream;
        try {
            fstream = new FileWriter(fileName, false);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(data);

            out.close();
        } catch (IOException ex) {
        }
    }

    public  String readData(String fileName) {

      
        String data = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
          //  BufferedReader br = new BufferedReader(new InputStreamReader(
           //           new FileInputStream(fileName), "UTF8"));
            
            
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {

        }
        return data;
    }

}
