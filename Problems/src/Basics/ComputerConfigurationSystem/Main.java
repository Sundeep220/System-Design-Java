package Basics.ComputerConfigurationSystem;

public class Main {

    public static void main(String[] args) {

        Computer gamingPc = new Computer.Builder("Ryzen 9", "32GB")
                .ssd("2TB")
                .gpu("RTX 5090")
                .wifiCard(true)
                .rgbKeyboard(true)
                .liquidCooling(true)
                .build();

        Computer officePc = new Computer.Builder("Intel i5", "16GB")
                .ssd("512GB")
                .wifiCard(true)
                .build();

        Computer basicPc = new Computer.Builder("Intel i3", "8GB")
                .build();

        System.out.println(gamingPc);

        System.out.println();

        System.out.println(officePc);

        System.out.println();

        System.out.println(basicPc);
    }
}
