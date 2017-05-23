package pathfinding;

import agent.Agent;
import agent.WorldModel;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * This class is used when all you are interested in is the coordinates and nothing else.
 */
public class Coordinate {

    public final int x, y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Generates neighbor states for use in BFS algorithm.
     *
     * @param worldModel the world model of the agent
     * @param hasKey if the agent has the key
     * @param stage which stage the agent is currently in
     * @return a list of states containing the neighbors of this state
     */
    ArrayList<Coordinate> generateBFSNeighbors(WorldModel worldModel, boolean hasKey, Agent.Stage stage) {
        ArrayList<Coordinate> newStates = new ArrayList<>();
        if (!worldModel.positionBlocked(x - 1, y, hasKey, new HashSet<>(), stage)) {
            newStates.add(new Coordinate(x - 1, y));
        }
        if (!worldModel.positionBlocked(x, y - 1, hasKey, new HashSet<>(), stage)) {
            newStates.add(new Coordinate(x, y - 1));
        }
        if (!worldModel.positionBlocked(x + 1, y, hasKey, new HashSet<>(), stage)) {
            newStates.add(new Coordinate(x + 1, y));
        }
        if (!worldModel.positionBlocked(x, y + 1, hasKey, new HashSet<>(), stage)) {
            newStates.add(new Coordinate(x, y + 1));
        }
        return newStates;
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
