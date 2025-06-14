package CreationPatterns.AbstractFactory.prob1.Factory;

import CreationPatterns.AbstractFactory.prob1.Button.Button;
import CreationPatterns.AbstractFactory.prob1.Button.WindowsButton;
import CreationPatterns.AbstractFactory.prob1.CheckBox.CheckBox;
import CreationPatterns.AbstractFactory.prob1.CheckBox.WindowsCheckBox;

public class WindowsGUIFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new WindowsButton();
    }

    @Override
    public CheckBox createCheckbox() {
        return new WindowsCheckBox();
    }
}
