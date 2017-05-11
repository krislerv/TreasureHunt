package pathfinding;

public class DijkstraCoordinate extends Coordinate {

    private Coordinate parent;
    private int distance;

    public DijkstraCoordinate(int x, int y) {
        super(x, y);
    }

    public Coordinate getParent() {
        return parent;
    }

    public void setParent(Coordinate parent) {
        this.parent = parent;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }


}
