package pathfinding;

import agent.WorldModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class State implements Comparable<State> {
	
	private final int relativeCoordX, relativeCoordY;
	private final char relativeAgentOrientation;

	private int f, g, h;

    private boolean hasGold, hasKey, hasAxe, hasRaft, onRaft;
    private int dynamiteCount;
	
	private State parent;

	private HashSet<Coordinate> blockadesRemoved;

    private HashMap<Character, Integer> xOffset;
    private HashMap<Character, Integer> yOffset;


    public State(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation) {
		this.relativeCoordX = relativeCoordX;
		this.relativeCoordY = relativeCoordY;
		this.relativeAgentOrientation = relativeAgentOrientation;
        blockadesRemoved = new HashSet<>(); //  doors opened, walls blown up, trees cut down
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

    public State(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation, HashSet<Coordinate> blockadesRemoved, Coordinate newDoorUnlock, boolean hasGold, boolean hasKey, boolean hasAxe, boolean hasRaft, boolean onRaft, int dynamiteCount) {
	    this(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount);
        this.blockadesRemoved.add(newDoorUnlock);
    }

	public int getRelativeCoordX() {
	    return  relativeCoordX;
    }

    public int getRelativeCoordY() {
        return  relativeCoordY;
    }

    public char getRelativeAgentOrientation() {
	    return relativeAgentOrientation;
    }

    public HashSet<Coordinate> getBlockadesRemoved() {
	    return blockadesRemoved;
    }

    public ArrayList<State> generateNeighbors(WorldModel worldModel, boolean waterMode) {
        ArrayList<State> newStates = new ArrayList<>();
        if (!worldModel.positionBlocked(relativeCoordX - 1, relativeCoordY, hasKey, new HashSet<>(), waterMode)) {
            newStates.add(new State(relativeCoordX - 1, relativeCoordY, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX, relativeCoordY - 1, hasKey, new HashSet<>(), waterMode)) {
            newStates.add(new State(relativeCoordX, relativeCoordY - 1, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX + 1, relativeCoordY, hasKey, new HashSet<>(), waterMode)) {
            newStates.add(new State(relativeCoordX + 1, relativeCoordY, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX, relativeCoordY + 1, hasKey, new HashSet<>(), waterMode)) {
            newStates.add(new State(relativeCoordX, relativeCoordY + 1, 'N'));
        }
        return newStates;
    }

    public void setDynamiteCount(int dynamiteCount) {
        this.dynamiteCount = dynamiteCount;
    }

	public State getParent() {
		return parent;
	}
	
	public void setParent(State parent) {
		this.parent = parent;
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

    public int getG() {
        return g;
    }

    public void updateG(int g) {
        this.g = g;
        this.f = g + h;
    }

    public int getH() {
        return h;
    }

    public int getF() {
	    return f;
    }

    public void setH(int h) {
        this.h = h;
    }

    public int heuristic(Coordinate goalState, WorldModel worldModel) {
        int minDynamiteDistance = 0;
        if (dynamiteCount < 0) {
            minDynamiteDistance = worldModel.getMinDynamiteDistance(relativeCoordX, relativeCoordY);
        }
        return Math.abs(goalState.x - relativeCoordX) + Math.abs(goalState.y - relativeCoordY) + minDynamiteDistance;
    }

    @Override
    public int compareTo(State otherState) {
        if (getF() != otherState.f) {
            return this.f - otherState.f;
        } else {
            return this.h - otherState.h;
        }
    }

    public ArrayList<State> generateAStarNeighbors(WorldModel worldModel, HashSet<Coordinate> blockadesRemoved) {
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
        if (!worldModel.agentBlocked(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved)) {
            newStates.add(new State(relativeCoordX + xOffset.get(relativeAgentOrientation), relativeCoordY + yOffset.get(relativeAgentOrientation), relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount));
        }
        if (hasKey && worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == '-') {
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
        return newStates;
    }

    public ArrayList<State> generatePlannedAStarNeighbors(WorldModel worldModel, HashSet<Coordinate> blockadesRemoved, boolean waterMode) {
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
        if (waterMode) {
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
        } else {
            if (hasKey && worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == '-') {
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
            else if (hasAxe && worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == 'T') {
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
            else if (dynamiteCount > 0 && worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == '*') {
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
            else if (worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == 'd') {
                newStates.add(new State(
                        relativeCoordX + xOffset.get(relativeAgentOrientation),
                        relativeCoordY + yOffset.get(relativeAgentOrientation),
                        relativeAgentOrientation,
                        blockadesRemoved,
                        hasGold,
                        hasKey,
                        hasAxe,
                        hasRaft,
                        false,
                        dynamiteCount + 1));
            }
            else if (!worldModel.agentBlocked(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved)) {
                newStates.add(new State(
                        relativeCoordX + xOffset.get(relativeAgentOrientation),
                        relativeCoordY + yOffset.get(relativeAgentOrientation),
                        relativeAgentOrientation,
                        blockadesRemoved,
                        hasGold,
                        hasKey,
                        hasAxe,
                        hasRaft,
                        false,
                        dynamiteCount));
            }

        }
        return newStates;
    }

    public int getDynamiteCount() {
        return dynamiteCount;
    }
}
