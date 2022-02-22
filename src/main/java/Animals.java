public enum Animals {
    KRYSA("Krysa", 20),
    PAVOUK("Pavouk", 40),
    HAD("Had", 60),
    NETOPYR("Netopýr", 50),
    DIVOCAK("Divočák", 33),
    VLK("Vlk", 70),
    MEDVED("Medvěd", 200),
    KROKODYL("Krokodýl", 240),
    TYGR("Tygr", 250),
    SLON("Slon", 520);

    public final String label;
    public final int strength;

    private Animals(String label, int strength) {
        this.label = label;
        this.strength = strength;
    }

    public static Animals valueOfLabel(String label) {
        for (Animals e : values()) {
            if (e.label.equals(label)) {
                return e;
            }
        }
        return null;
    }
}
