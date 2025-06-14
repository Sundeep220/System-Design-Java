package CreationPatterns.AbstractFactory.prob1.Factory;

import CreationPatterns.AbstractFactory.prob1.Button.Button;
import CreationPatterns.AbstractFactory.prob1.CheckBox.CheckBox;

public interface GUIFactory {
    public Button createButton();
    public CheckBox createCheckbox();
}
