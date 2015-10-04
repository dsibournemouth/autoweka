package autoweka.ui.instancegenerators;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JTextField;

import org.javabuilders.swing.SwingJavaBuilder;

import autoweka.ui.PropertyPanel;

public class DensityPreservingSampling extends PropertyPanel
{
    private static final long serialVersionUID = -2889480227263404283L;
    
    private JTextField mSeedText;
    private JTextField mNumLevelsText;
    public DensityPreservingSampling(Frame owner)
    {
        super(owner);
        SwingJavaBuilder.build(this);
    }
    
    @Override
    public String getClassName()
    {
        return "autoweka.instancegenerators.DensityPreservingSampling";
    }

    @Override
    public Properties getProperties()
    {
        Properties props = new Properties();
        props.setProperty("seed", mSeedText.getText());
        props.setProperty("numLevels", mNumLevelsText.getText());
        return props;
    }

    public String toString()
    {
        return "DensityPreservingSampling";
    }
}

