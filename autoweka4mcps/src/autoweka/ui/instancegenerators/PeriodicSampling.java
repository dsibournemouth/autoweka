package autoweka.ui.instancegenerators;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JTextField;

import org.javabuilders.swing.SwingJavaBuilder;

import autoweka.ui.PropertyPanel;

public class PeriodicSampling extends PropertyPanel
{
    private static final long serialVersionUID = -6745962579050336047L;
    
    private JTextField mNumFoldsText;
    public PeriodicSampling(Frame owner)
    {
        super(owner);
        SwingJavaBuilder.build(this);
    }
    
    @Override
    public String getClassName()
    {
        return "autoweka.instancegenerators.PeriodicSampling";
    }

    @Override
    public Properties getProperties()
    {
        Properties props = new Properties();
        props.setProperty("numFolds", mNumFoldsText.getText());
        return props;
    }

    public String toString()
    {
        return "PeriodicSampling";
    }
}

