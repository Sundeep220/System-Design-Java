package Basics.DesignPen;

public class CartridgeRefill implements RefillStrategy{
    @Override
    public void refill(){
        System.out.println("Refilling with Cartridge.....");
    }
}
