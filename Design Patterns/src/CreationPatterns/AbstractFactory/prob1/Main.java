package CreationPatterns.AbstractFactory.prob1;

import CreationPatterns.AbstractFactory.prob1.Client.UIRenderer;
import CreationPatterns.AbstractFactory.prob1.Factory.GUIFactory;
import CreationPatterns.AbstractFactory.prob1.Factory.MacGUIFactory;
import CreationPatterns.AbstractFactory.prob1.Factory.WindowsGUIFactory;

public class Main {
    public static void main(String[] args) {
        // Change to new MacFactory() or new WindowsFactory() as needed
        GUIFactory factory = new MacGUIFactory();
        UIRenderer ui = new UIRenderer(factory);
        ui.renderUI();

        GUIFactory factory2 = new WindowsGUIFactory();
        UIRenderer ui2 = new UIRenderer(factory2);
        ui2.renderUI();
    }
}
