package pathfinding;

/**
 * This class is used when all you are interested in is the coordinates and nothing else.
 */
public class Coordinate {

    public final int x, y;

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
