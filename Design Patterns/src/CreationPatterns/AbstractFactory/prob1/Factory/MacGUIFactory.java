package CreationPatterns.AbstractFactory.prob1.Factory;

import CreationPatterns.AbstractFactory.prob1.Button.Button;
import CreationPatterns.AbstractFactory.prob1.Button.MacButton;
import CreationPatterns.AbstractFactory.prob1.CheckBox.CheckBox;
import CreationPatterns.AbstractFactory.prob1.CheckBox.MacCheckBox;

public class MacGUIFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public CheckBox createCheckbox() {
        return new MacCheckBox();
    }
}
