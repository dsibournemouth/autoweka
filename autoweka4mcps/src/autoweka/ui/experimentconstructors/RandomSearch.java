
package autoweka.ui.experimentconstructors;

import java.awt.Frame;
import java.util.Properties;

import org.javabuilders.swing.SwingJavaBuilder;

import autoweka.ui.PropertyPanel;

public class RandomSearch
  extends PropertyPanel {

  private static final long serialVersionUID = 7669500148438547238L;

  public RandomSearch(Frame owner) {
    super(owner);
    SwingJavaBuilder.build(this);
  }

  @Override
  public String getClassName() {
    return "autoweka.randomsearch.RandomSearchExperimentConstructor";
  }

  public String toString() {
    return "RandomSearch";
  }

  @Override
  public Properties getProperties() {
    Properties props = new Properties();
    return props;
  }

}
