package pathfinding;

public class Coordinate {

    public final int x;
    public final int y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Coordinate && x == ((Coordinate) other).x && y == ((Coordinate) other).y;
    }

    @Override
    public int hashCode() {
        return 887 * x + 1031 * y;
    }

}
