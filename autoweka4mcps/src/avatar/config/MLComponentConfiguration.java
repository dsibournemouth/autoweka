/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package avatar.config;

import java.util.List;
import javax.xml.bind.JAXBException;
import uts.aai.avatar.model.MLComponent;
import uts.aai.avatar.service.AlgorithmMetaKnowledge;
import uts.aai.utils.IOUtils;
import uts.aai.utils.JSONUtils;

/**
 *
 * @author ntdun
 */
public class MLComponentConfiguration {
    
    public  List<MLComponent> loadListOfMLComponents(String metaKnowledgeFilePath) {

        IOUtils iou = new IOUtils();
        List<MLComponent> listOfMLComponents = null;

        String metaKnowledgeJSON = iou.readData(metaKnowledgeFilePath);
        try {
            AlgorithmMetaKnowledge metaKnowledge = JSONUtils.unmarshal(metaKnowledgeJSON, AlgorithmMetaKnowledge.class);

            listOfMLComponents = metaKnowledge.getListOfMLComponents();
        } catch (JAXBException ex) {
            System.out.println(ex);
        }

        

    return listOfMLComponents;

    }
    
}
