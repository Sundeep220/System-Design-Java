package Basics.DesignPen;


public class Pen {

    enum PenType {
        GEL,
        BALL,
        FOUNTAIN
    }

    private final String brand;
    private final String name;
    private final PenType type;
    private final RefillStrategy refillStrategy;

    public Pen(String brand, String name, PenType type, RefillStrategy refillStrategy){
        this.brand = brand;
        this.name = name;
        this.type = type;
        this.refillStrategy = refillStrategy;
    }

    public void refill(){
        if(refillStrategy == null){
            System.out.println("Pen is not refillable....");
            return;
        }
        refillStrategy.refill();
    }

    public PenType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public void write(){
        System.out.println("Writing......");
    }

}
