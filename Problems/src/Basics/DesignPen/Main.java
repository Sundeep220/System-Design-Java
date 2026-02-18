package Basics.DesignPen;

public class Main {
    public static void main(String[] args) {

        Pen refillablePen = new Pen(
                "Parker",
                "Blue",
                Pen.PenType.GEL,
                new CartridgeRefill()
        );

        Pen disposablePen = new Pen(
                "Reynolds",
                "Black",
                Pen.PenType.BALL,
                null
        );

        refillablePen.refill();  // Replacing ink cartridge...
        disposablePen.refill();  // This pen is not refillable.
    }
}
