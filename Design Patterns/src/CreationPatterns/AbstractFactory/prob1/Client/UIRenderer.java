package CreationPatterns.AbstractFactory.prob1.Client;

import CreationPatterns.AbstractFactory.prob1.Button.Button;
import CreationPatterns.AbstractFactory.prob1.CheckBox.CheckBox;
import CreationPatterns.AbstractFactory.prob1.Factory.GUIFactory;

public class UIRenderer {
   private final GUIFactory factory;
   public UIRenderer(GUIFactory factory) {
      this.factory = factory;
   }
   public void renderUI(){
      Button button = factory.createButton();
      CheckBox checkbox = factory.createCheckbox();
      button.render();
      checkbox.render();
   }
}
