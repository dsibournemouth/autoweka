package autoweka.ui.instancegenerators;

import java.awt.Frame;
import java.util.Properties;

import org.javabuilders.swing.SwingJavaBuilder;

import autoweka.ui.PropertyPanel;

public class Default extends PropertyPanel
{
    private static final long serialVersionUID = -8288228920397148893L;

    public Default(Frame owner)
    {
        super(owner);
        SwingJavaBuilder.build(this);
    }
    
    @Override
    public String getClassName()
    {
        return "autoweka.instancegenerators.Default";
    }

    @Override
    public Properties getProperties()
    {
        return new Properties();
    }

    public String toString()
    {
        return "Default";
    }
}
