public class Oasis {
    int x;
    int y;
    int distance;

    public Oasis(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Oasis{" +
                "x=" + x +
                ", y=" + y +
                ", distance=" + distance +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Oasis))  {
            return false;
        }
        Oasis other = (Oasis)obj;
        return x==other.x && y == other.y;
    }
//    @Override
//    public int hashCode() {
//        return 31*x.hashCode()+y.hashCode();
//    }
}
