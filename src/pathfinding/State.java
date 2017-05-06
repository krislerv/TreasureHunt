package pathfinding;

import agent.WorldModel;

import java.util.ArrayList;
import java.util.HashSet;

public class State implements Comparable<State> {
	
	private final int relativeCoordX, relativeCoordY;
	private final char relativeAgentOrientation;

	private int f, g, h;

    private boolean hasGold, hasKey, hasAxe, hasRaft;
    private int dynamiteCount;
	
	private State parent;

	private HashSet<Coordinate> blockadesRemoved;

	public State(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation) {
		this.relativeCoordX = relativeCoordX;
		this.relativeCoordY = relativeCoordY;
		this.relativeAgentOrientation = relativeAgentOrientation;
        blockadesRemoved = new HashSet<>(); //  doors opened, walls blown up, trees cut down
	}

	public State(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation, HashSet<Coordinate> blockadesRemoved, boolean hasGold, boolean hasKey, boolean hasAxe, boolean hasRaft, int dynamiteCount) {
        this(relativeCoordX, relativeCoordY, relativeAgentOrientation);
        for (Coordinate c : blockadesRemoved) {
            this.blockadesRemoved.add(new Coordinate(c.x, c.y));
        }
        this.hasGold = hasGold;
        this.hasKey = hasKey;
        this.hasAxe = hasAxe;
        this.hasRaft = hasRaft;
        this.dynamiteCount = dynamiteCount;
    }

    public State(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation, HashSet<Coordinate> blockadesRemoved, Coordinate newDoorUnlock, boolean hasGold, boolean hasKey, boolean hasAxe, boolean hasRaft, int dynamiteCount) {
	    this(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, dynamiteCount);
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

    public ArrayList<State> generateNeighbors(WorldModel worldModel) {
        ArrayList<State> newStates = new ArrayList<>();
        if (!worldModel.positionBlocked(relativeCoordX - 1, relativeCoordY, hasKey, new HashSet<>())) {
            newStates.add(new State(relativeCoordX - 1, relativeCoordY, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX, relativeCoordY - 1, hasKey, new HashSet<>())) {
            newStates.add(new State(relativeCoordX, relativeCoordY - 1, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX + 1, relativeCoordY, hasKey, new HashSet<>())) {
            newStates.add(new State(relativeCoordX + 1, relativeCoordY, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX, relativeCoordY + 1, hasKey, new HashSet<>())) {
            newStates.add(new State(relativeCoordX, relativeCoordY + 1, 'N'));
        }
        return newStates;
    }

	public State getParent() {
		return parent;
	}
	
	public void setParent(State parent) {
		this.parent = parent;
	}

	@Override
	public String toString() {
		return "(" + relativeCoordX + ", " + relativeCoordY + ")";
	}

    @Override
    public int hashCode() {
	    int blockadesRemovedSum = 0;
	    for (Coordinate c : blockadesRemoved) {
	        blockadesRemovedSum += c.hashCode();
        }
	    return 2399 * relativeCoordX + 2083 * relativeCoordY + 1889 * relativeAgentOrientation + blockadesRemovedSum;
    }

    @Override
    public boolean equals(Object object) {
        /*if (blockadesRemoved.size() != ((State)object).blockadesRemoved.size()) {
	        return false;
        }
	    for (Coordinate c : blockadesRemoved) {
	        if (!((State)object).blockadesRemoved.contains(c)) {
	            return false;
            }
        }*/
        return relativeCoordX == ((State)object).relativeCoordX &&  relativeCoordY == ((State)object).relativeCoordY &&  relativeAgentOrientation == ((State)object).relativeAgentOrientation;
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

    public int heuristic(Coordinate goalState) {
        return Math.abs(goalState.x - relativeCoordX) + Math.abs(goalState.y - relativeCoordY);
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
                newStates.add(new State(relativeCoordX, relativeCoordY, 'W', blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                newStates.add(new State(relativeCoordX, relativeCoordY, 'E', blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                break;
            case 'W':
            case 'E':
                newStates.add(new State(relativeCoordX, relativeCoordY, 'N', blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                newStates.add(new State(relativeCoordX, relativeCoordY, 'S', blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                break;
        }
        if (hasKey && worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == '-') {
            switch (relativeAgentOrientation) {
                case 'N':
                    newStates.add(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, new Coordinate(relativeCoordX, relativeCoordY - 1), hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                    break;
                case 'W':
                    newStates.add(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, new Coordinate(relativeCoordX - 1, relativeCoordY), hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                    break;
                case 'S':
                    newStates.add(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, new Coordinate(relativeCoordX, relativeCoordY + 1), hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                    break;
                case 'E':
                    newStates.add(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, new Coordinate(relativeCoordX + 1, relativeCoordY), hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                    break;
            }
        }
        if (!worldModel.agentBlocked(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved)) {
            switch (relativeAgentOrientation) {
                case 'N':
                    newStates.add(new State(relativeCoordX, relativeCoordY - 1, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                    break;
                case 'W':
                    newStates.add(new State(relativeCoordX - 1, relativeCoordY, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                    break;
                case 'S':
                    newStates.add(new State(relativeCoordX, relativeCoordY + 1, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                    break;
                case 'E':
                    newStates.add(new State(relativeCoordX + 1, relativeCoordY, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, dynamiteCount));
                    break;
            }
        }
        return newStates;

        /*
        if (!worldModel.positionBlocked(relativeCoordX - 1, relativeCoordY, false, blockadesRemoved)) {
            newStates.add(new State(relativeCoordX - 1, relativeCoordY, blockadesRemoved));
        }
        if (!worldModel.positionBlocked(relativeCoordX, relativeCoordY - 1, false, blockadesRemoved)) {
            newStates.add(new State(relativeCoordX,  relativeCoordY - 1, blockadesRemoved));
        }
        if (!worldModel.positionBlocked(relativeCoordX + 1, relativeCoordY, false, blockadesRemoved)) {
            newStates.add(new State(relativeCoordX + 1, relativeCoordY, blockadesRemoved));
        }
        if (!worldModel.positionBlocked(relativeCoordX, relativeCoordY + 1, false, blockadesRemoved)) {
            newStates.add(new State(relativeCoordX, relativeCoordY + 1, blockadesRemoved));
        }

        if (hasKey) {
            if (worldModel.getObjectAtCoordinate(relativeCoordX - 1, relativeCoordY) == '-') {
                newStates.add(new State(relativeCoordX, relativeCoordY, blockadesRemoved, new Coordinate(relativeCoordX - 1, relativeCoordY)));
            }
            if (worldModel.getObjectAtCoordinate(relativeCoordX, relativeCoordY - 1) == '-') {
                newStates.add(new State(relativeCoordX, relativeCoordY, blockadesRemoved, new Coordinate(relativeCoordX, relativeCoordY - 1)));
            }
            if (worldModel.getObjectAtCoordinate(relativeCoordX + 1, relativeCoordY) == '-') {
                newStates.add(new State(relativeCoordX, relativeCoordY, blockadesRemoved, new Coordinate(relativeCoordX + 1, relativeCoordY)));
            }
            if (worldModel.getObjectAtCoordinate(relativeCoordX, relativeCoordY + 1) == '-') {
                newStates.add(new State(relativeCoordX, relativeCoordY, blockadesRemoved, new Coordinate(relativeCoordX, relativeCoordY + 1)));
            }
        }
        return newStates;*/
    }
}
