package pathfinding;

import agent.Agent;
import agent.WorldModel;

import java.util.*;

/**
 * This class is used in search algorithms to keep track of the world state in each node.
 */
public class State {

    /**
     * Keeps track of the relative coordinates of the agent.
     */
	private final int relativeCoordX, relativeCoordY;

    /**
     * Keeps track of the orientation of the agent.
     */
	private final char relativeAgentOrientation;

    /**
     * G and H values used in A* and Dijkstra.
     */
	private int g, h;

    /**
     * Agent inventory.
     */
    private boolean hasGold, hasKey, hasAxe, hasRaft, onRaft;
    private int dynamiteCount;

    /**
     * Keeps track of the state's parent to be used to generate a path in search algorithms.
     */
	private State parent;

    /**
     * Contains coordinates for all walls that have been blown up, doors unlocked, trees cut down, dynamite picked up.
     */
	private HashSet<Coordinate> blockadesRemoved;

    /**
     * Hash maps that specify the position offset of new states when moving in a certain direction.
     * For example: If the agent is moving north and wants to generate a new state with new coordinates,
     * you call the get method on the hash map with 'N' as parameter and you get 0 as the x-offset and -1 as the y-offset.
     */
    public static final HashMap<Character, Integer> xOffset;
    public static final HashMap<Character, Integer> yOffset;

    static {
        xOffset = new HashMap<>();
        xOffset.put('N', 0);
        xOffset.put('W', -1);
        xOffset.put('S', 0);
        xOffset.put('E', 1);
        yOffset = new HashMap<>();
        yOffset.put('N', -1);
        yOffset.put('W', 0);
        yOffset.put('S', 1);
        yOffset.put('E', 0);
    }

    /**
     * Constructor that sets up a state with default values (empty inventory)
     *
     * @param relativeCoordX the relative x coordinate of the agent
     * @param relativeCoordY the relative y coordinate of the agent
     * @param relativeAgentOrientation the relative orientation of the agent
     */
    public State(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation) {
		this.relativeCoordX = relativeCoordX;
		this.relativeCoordY = relativeCoordY;
		this.relativeAgentOrientation = relativeAgentOrientation;
        blockadesRemoved = new HashSet<>(); //  doors opened, walls blown up, trees cut down
    }

    /**
     * Constructor that sets up a state with the given inventory
     *
     * @param relativeCoordX the relative x coordinate of the agent
     * @param relativeCoordY the relative y coordinate of the agent
     * @param relativeAgentOrientation the relative orientation of the agent
     * @param blockadesRemoved a HashSet of the blockades that have been removed
     * @param hasGold if the agent has collected the gold
     * @param hasKey if the agent has collected the key
     * @param hasAxe if the agent has collected the axe
     * @param hasRaft if the agent has collected the raft
     * @param onRaft if the agent is on a raft
     * @param dynamiteCount how many dynamites the agent has
     */
	public State(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation, HashSet<Coordinate> blockadesRemoved, boolean hasGold, boolean hasKey, boolean hasAxe, boolean hasRaft, boolean onRaft, int dynamiteCount) {
        this(relativeCoordX, relativeCoordY, relativeAgentOrientation);
        for (Coordinate c : blockadesRemoved) {
            this.blockadesRemoved.add(new Coordinate(c.x, c.y));
        }
        this.hasGold = hasGold;
        this.hasKey = hasKey;
        this.hasAxe = hasAxe;
        this.hasRaft = hasRaft;
        this.onRaft = onRaft;
        this.dynamiteCount = dynamiteCount;
    }

    /**
     * Constructor that sets up a state with the given inventory and adds another coordinate to blockadesRemoved
     *
     * @param relativeCoordX the relative x coordinate of the agent
     * @param relativeCoordY the relative y coordinate of the agent
     * @param relativeAgentOrientation the relative orientation of the agent
     * @param blockadesRemoved a HashSet of the blockades that have been removed
     * @param newBlockade a new coordinate to be added to blockadesRemoved
     * @param hasGold if the agent has collected the gold
     * @param hasKey if the agent has collected the key
     * @param hasAxe if the agent has collected the axe
     * @param hasRaft if the agent has collected the raft
     * @param onRaft if the agent is on a raft
     * @param dynamiteCount how many dynamites the agent has
     */
    public State(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation, HashSet<Coordinate> blockadesRemoved, Coordinate newBlockade, boolean hasGold, boolean hasKey, boolean hasAxe, boolean hasRaft, boolean onRaft, int dynamiteCount) {
	    this(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount);
        this.blockadesRemoved.add(newBlockade);
    }

	public int getRelativeCoordX() { return  relativeCoordX; }

    public int getRelativeCoordY() { return  relativeCoordY; }

    char getRelativeAgentOrientation() { return relativeAgentOrientation; }

    int getDynamiteCount() { return dynamiteCount; }

    public void setDynamiteCount(int dynamiteCount) { this.dynamiteCount = dynamiteCount; }

    State getParent() { return parent; }

    void setParent(State parent) { this.parent = parent; }

    int getG() { return g; }

    void setG(int g) { this.g = g; }

    void setH(int h) { this.h = h; }

    /**
     * Returns the F-value of the state to be used in A*. The heuristic (greedy) part of the F-value has been emphasized
     * to favor speed over optimality.
     *
     * @return the F-value of the state to be used in A*
     */
    int getF() { return g + 2*h; }

    /**
     * Calculates the heuristic value for the state. Uses the manhattan distance from current position to goal position.
     * If the agent is in planned mode, add onto the heuristic the manhattan distance of a path from the agent's position going through every dynamite.
     * This heuristic is not admissible, but we don't care about an optimal solution.
     *
     * @param goalState the state the algorithm is trying to reach
     * @param worldModel the world model of the agent
     * @return the heuristic value for the state
     */
    int heuristic(Coordinate goalState, WorldModel worldModel, Agent.Stage stage) {
        int dynamiteDistance = 0;
        if (stage == Agent.Stage.PLANNED) {
            ArrayList<Coordinate> dynamites = worldModel.getAllDynamites(blockadesRemoved);
            for (int i = 0; i < dynamites.size() - 1; i++) {
                dynamiteDistance += Math.abs(dynamites.get(i).x - dynamites.get(i+1).x) + Math.abs(dynamites.get(i).y - dynamites.get(i+1).y);
            }
            if (!dynamites.isEmpty()) {
                dynamiteDistance += Math.abs(dynamites.get(0).x - relativeCoordX) + Math.abs(dynamites.get(0).y - relativeCoordY);
            }
        }
        return Math.abs(goalState.x - relativeCoordX) + Math.abs(goalState.y - relativeCoordY) + dynamiteDistance;
    }

    /**
     * Generates neighbor states for use in BFS algorithm.
     *
     * @param worldModel the world model of the agent
     * @param stage which stage the agent is currently in
     * @return a list of states containing the neighbors of this state
     */
    ArrayList<State> generateBFSNeighbors(WorldModel worldModel, Agent.Stage stage) {
        ArrayList<State> newStates = new ArrayList<>();
        if (!worldModel.positionBlocked(relativeCoordX - 1, relativeCoordY, hasKey, new HashSet<>(), stage)) {
            newStates.add(new State(relativeCoordX - 1, relativeCoordY, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX, relativeCoordY - 1, hasKey, new HashSet<>(), stage)) {
            newStates.add(new State(relativeCoordX, relativeCoordY - 1, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX + 1, relativeCoordY, hasKey, new HashSet<>(), stage)) {
            newStates.add(new State(relativeCoordX + 1, relativeCoordY, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX, relativeCoordY + 1, hasKey, new HashSet<>(), stage)) {
            newStates.add(new State(relativeCoordX, relativeCoordY + 1, 'N'));
        }
        return newStates;
    }

    /**
     * Generates neighbor states for use in A* algorithm.
     *
     * @param worldModel the world model of the agent
     * @param stage which stage the agent is currently in. Safe mode can only move on land and unlock doors
     *             Water mode can only move on water. Planned mode can do everything
     * @param legalDynamiteCoordinates the coordinates the agent  is allowed to blow up. null means any tile can be blown up
     * @return a list of states containing the neighbors of this state
     */
    ArrayList<State> generateAStarNeighbors(WorldModel worldModel, Agent.Stage stage, ArrayList<Coordinate> legalDynamiteCoordinates) {
        ArrayList<State> newStates = new ArrayList<>();
        switch (relativeAgentOrientation) {
            case 'N':
            case 'S':
                newStates.add(new State(relativeCoordX, relativeCoordY, 'W', blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount));
                newStates.add(new State(relativeCoordX, relativeCoordY, 'E', blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount));
                break;
            case 'W':
            case 'E':
                newStates.add(new State(relativeCoordX, relativeCoordY, 'N', blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount));
                newStates.add(new State(relativeCoordX, relativeCoordY, 'S', blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount));
                break;
        }
        if (stage == Agent.Stage.WATER) {
            if ((hasRaft || onRaft) && worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == '~') {
                newStates.add(new State(
                        relativeCoordX + xOffset.get(relativeAgentOrientation),
                        relativeCoordY + yOffset.get(relativeAgentOrientation),
                        relativeAgentOrientation,
                        blockadesRemoved,
                        hasGold,
                        hasKey,
                        hasAxe,
                        false,
                        true,
                        dynamiteCount));
            }
        }
        if (stage == Agent.Stage.PLANNED || stage == Agent.Stage.LUMBERJACK || stage == Agent.Stage.BOMBERMAN) {
            if (hasAxe && worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == 'T' && !blockadesRemoved.contains(new Coordinate(relativeCoordX + xOffset.get(relativeAgentOrientation), relativeCoordY + yOffset.get(relativeAgentOrientation)))) {
                newStates.add(new State(
                        relativeCoordX,
                        relativeCoordY,
                        relativeAgentOrientation,
                        blockadesRemoved,
                        new Coordinate(relativeCoordX + xOffset.get(relativeAgentOrientation), relativeCoordY + yOffset.get(relativeAgentOrientation)),
                        hasGold,
                        hasKey,
                        hasAxe,
                        true,
                        onRaft,
                        dynamiteCount));
            }
            else if (dynamiteCount > 0 && worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == '*' && !blockadesRemoved.contains(new Coordinate(relativeCoordX + xOffset.get(relativeAgentOrientation), relativeCoordY + yOffset.get(relativeAgentOrientation))) && !onRaft && (legalDynamiteCoordinates == null || legalDynamiteCoordinates.contains(new Coordinate(relativeCoordX + xOffset.get(relativeAgentOrientation), relativeCoordY + yOffset.get(relativeAgentOrientation))))) {
                newStates.add(new State(
                        relativeCoordX,
                        relativeCoordY,
                        relativeAgentOrientation,
                        blockadesRemoved,
                        new Coordinate(relativeCoordX + xOffset.get(relativeAgentOrientation), relativeCoordY + yOffset.get(relativeAgentOrientation)),
                        hasGold,
                        hasKey,
                        hasAxe,
                        hasRaft,
                        onRaft,
                        dynamiteCount - 1));
            }
            else if ((hasRaft || onRaft) && worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == '~') {
                newStates.add(new State(
                        relativeCoordX + xOffset.get(relativeAgentOrientation),
                        relativeCoordY + yOffset.get(relativeAgentOrientation),
                        relativeAgentOrientation,
                        blockadesRemoved,
                        hasGold,
                        hasKey,
                        hasAxe,
                        false,
                        true,
                        dynamiteCount));
            }
            else if (worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == '$') {
                newStates.add(new State(
                        relativeCoordX + xOffset.get(relativeAgentOrientation),
                        relativeCoordY + yOffset.get(relativeAgentOrientation),
                        relativeAgentOrientation,
                        blockadesRemoved,
                        true,
                        hasKey,
                        hasAxe,
                        hasRaft,
                        false,
                        dynamiteCount));
            }
            else if (worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == 'k') {
                newStates.add(new State(
                        relativeCoordX + xOffset.get(relativeAgentOrientation),
                        relativeCoordY + yOffset.get(relativeAgentOrientation),
                        relativeAgentOrientation,
                        blockadesRemoved,
                        hasGold,
                        true,
                        hasAxe,
                        hasRaft,
                        false,
                        dynamiteCount));
            }
            else if (worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == 'a') {
                newStates.add(new State(
                        relativeCoordX + xOffset.get(relativeAgentOrientation),
                        relativeCoordY + yOffset.get(relativeAgentOrientation),
                        relativeAgentOrientation,
                        blockadesRemoved,
                        hasGold,
                        hasKey,
                        true,
                        hasRaft,
                        false,
                        dynamiteCount));
            }
            else if (worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == 'd' &&
                    !blockadesRemoved.contains(new Coordinate(relativeCoordX + xOffset.get(relativeAgentOrientation), relativeCoordY + yOffset.get(relativeAgentOrientation)))) {
                newStates.add(new State(
                        relativeCoordX + xOffset.get(relativeAgentOrientation),
                        relativeCoordY + yOffset.get(relativeAgentOrientation),
                        relativeAgentOrientation,
                        blockadesRemoved,
                        new Coordinate(relativeCoordX + xOffset.get(relativeAgentOrientation), relativeCoordY + yOffset.get(relativeAgentOrientation)),
                        hasGold,
                        hasKey,
                        hasAxe,
                        hasRaft,
                        false,
                        dynamiteCount + 1));
            }
        }
        if (stage == Agent.Stage.PLANNED || stage == Agent.Stage.SAFE  || stage == Agent.Stage.LUMBERJACK || stage == Agent.Stage.BOMBERMAN) {
            if (hasKey && worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == '-' && !blockadesRemoved.contains(new Coordinate(relativeCoordX + xOffset.get(relativeAgentOrientation), relativeCoordY + yOffset.get(relativeAgentOrientation)))) {
                newStates.add(new State(
                        relativeCoordX,
                        relativeCoordY,
                        relativeAgentOrientation,
                        blockadesRemoved,
                        new Coordinate(relativeCoordX + xOffset.get(relativeAgentOrientation), relativeCoordY + yOffset.get(relativeAgentOrientation)),
                        hasGold,
                        hasKey,
                        hasAxe,
                        hasRaft,
                        onRaft,
                        dynamiteCount));
            }
            else if (!worldModel.agentBlocked(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved)) {
                boolean steppingOutOfWater = false;
                if (worldModel.getObjectAtCoordinate(relativeCoordX, relativeCoordY) == '~') {  // if the agent cut down a tree while in the water, then stepped right out of the water, make sure to remove raft
                    steppingOutOfWater = true;
                }
                newStates.add(new State(
                        relativeCoordX + xOffset.get(relativeAgentOrientation),
                        relativeCoordY + yOffset.get(relativeAgentOrientation),
                        relativeAgentOrientation,
                        blockadesRemoved,
                        hasGold,
                        hasKey,
                        hasAxe,
                        !steppingOutOfWater && hasRaft,
                        false,
                        dynamiteCount));
            }
        }
        return newStates;
    }

    @Override
    public String toString() {
        return "(" + relativeCoordX + ", " + relativeCoordY + ", " + relativeAgentOrientation + ")";
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {
                relativeCoordX,
                relativeCoordY,
                relativeAgentOrientation,
                blockadesRemoved,
                hasGold,
                hasKey,
                hasAxe,
                hasRaft,
                dynamiteCount,
                onRaft
        });
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof State)) {
            return false;
        }
        if (blockadesRemoved.size() != ((State)object).blockadesRemoved.size()) {
            return false;
        }
        for (Coordinate c : blockadesRemoved) {
            if (!((State)object).blockadesRemoved.contains(c)) {
                return false;
            }
        }
        return relativeCoordX == ((State)object).relativeCoordX &&
                relativeCoordY == ((State)object).relativeCoordY &&
                relativeAgentOrientation == ((State)object).relativeAgentOrientation &&
                hasGold == ((State)object).hasGold &&
                hasKey == ((State)object).hasKey &&
                hasAxe == ((State)object).hasAxe &&
                hasRaft == ((State)object).hasRaft &&
                onRaft == ((State)object).onRaft &&
                dynamiteCount == ((State)object).dynamiteCount;
    }

}
