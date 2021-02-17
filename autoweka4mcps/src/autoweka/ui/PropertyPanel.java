package autoweka.ui;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JDialog;

public abstract class PropertyPanel extends JDialog
{
    private static final long serialVersionUID = 6396462051400618042L;

    public PropertyPanel(Frame owner)
    {
        super(owner, "Instance Generator Options", true);
        setLocationRelativeTo(owner);
    }

    abstract public String getClassName();
    abstract public Properties getProperties();
    public Properties getGlobalProperties()
    {
        return null;
    }

    public void close()
    {
        this.setVisible(false);
    }
}
