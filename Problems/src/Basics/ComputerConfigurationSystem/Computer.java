package Basics.ComputerConfigurationSystem;

public class Computer {

    // Mandatory
    private final String cpu;
    private final String ram;

    // Optional
    private final String ssd;
    private final String gpu;
    private final boolean wifiCard;
    private final boolean rgbKeyboard;
    private final boolean liquidCooling;

    // Private constructor
    private Computer(Builder builder) {
        this.cpu = builder.cpu;
        this.ram = builder.ram;
        this.ssd = builder.ssd;
        this.gpu = builder.gpu;
        this.wifiCard = builder.wifiCard;
        this.rgbKeyboard = builder.rgbKeyboard;
        this.liquidCooling = builder.liquidCooling;
    }

    // Getters only (Immutable)

    public String getCpu() {
        return cpu;
    }

    public String getRam() {
        return ram;
    }

    public String getSsd() {
        return ssd;
    }

    public String getGpu() {
        return gpu;
    }

    public boolean hasWifiCard() {
        return wifiCard;
    }

    public boolean hasRgbKeyboard() {
        return rgbKeyboard;
    }

    public boolean hasLiquidCooling() {
        return liquidCooling;
    }

    @Override
    public String toString() {
        return "Computer{" +
                "cpu='" + cpu + '\'' +
                ", ram='" + ram + '\'' +
                ", ssd='" + ssd + '\'' +
                ", gpu='" + gpu + '\'' +
                ", wifiCard=" + wifiCard +
                ", rgbKeyboard=" + rgbKeyboard +
                ", liquidCooling=" + liquidCooling +
                '}';
    }

    // ================= Builder =================

    public static class Builder {

        // Mandatory
        private final String cpu;
        private final String ram;

        // Optional
        private String ssd;
        private String gpu;
        private boolean wifiCard;
        private boolean rgbKeyboard;
        private boolean liquidCooling;

        public Builder(String cpu, String ram) {
            this.cpu = cpu;
            this.ram = ram;
        }

        public Builder ssd(String ssd) {
            this.ssd = ssd;
            return this;
        }

        public Builder gpu(String gpu) {
            this.gpu = gpu;
            return this;
        }

        public Builder wifiCard(boolean wifiCard) {
            this.wifiCard = wifiCard;
            return this;
        }

        public Builder rgbKeyboard(boolean rgbKeyboard) {
            this.rgbKeyboard = rgbKeyboard;
            return this;
        }

        public Builder liquidCooling(boolean liquidCooling) {
            this.liquidCooling = liquidCooling;
            return this;
        }

        public Computer build() {
            return new Computer(this);
        }
    }
}
