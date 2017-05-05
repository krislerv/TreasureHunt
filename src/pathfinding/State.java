package pathfinding;

import agent.WorldModel;

import java.util.ArrayList;
import java.util.Arrays;

public class State implements Comparable<State> {
	
	private final char relativeAgentOrientation;
	private final int relativeCoordX, relativeCoordY;

	private int f, g, h;
	
	private State parent;

	private ArrayList<Coordinate> blockadesRemoved;

	public State(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation) {
		this.relativeCoordX = relativeCoordX;
		this.relativeCoordY = relativeCoordY;
		this.relativeAgentOrientation = relativeAgentOrientation;
        blockadesRemoved = new ArrayList<>(); //  doors opened, walls blown up, trees cut down
	}

	public State(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation, ArrayList<Coordinate> blockadesRemoved) {
        this(relativeCoordX, relativeCoordY, relativeAgentOrientation);
	    for (int i = 0; i < blockadesRemoved.size(); i++) {
            Coordinate c = blockadesRemoved.get(i);
            this.blockadesRemoved.add(new Coordinate(c.x, c.y));
        }
    }

    public State(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation, ArrayList<Coordinate> blockadesRemoved, Coordinate newDoorUnlock) {
	    this(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved);
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

    public ArrayList<State> generateNeighbors(WorldModel worldModel, ArrayList<Boolean> inventory) {
        //System.out.println(inventory.get(1));
        ArrayList<State> newStates = new ArrayList<>();
        if (!worldModel.positionBlocked(relativeCoordX - 1, relativeCoordY, inventory.get(1))) {
            newStates.add(new State(relativeCoordX - 1, relativeCoordY, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX, relativeCoordY - 1, inventory.get(1))) {
            newStates.add(new State(relativeCoordX, relativeCoordY - 1, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX + 1, relativeCoordY, inventory.get(1))) {
            newStates.add(new State(relativeCoordX + 1, relativeCoordY, 'N'));
        }
        if (!worldModel.positionBlocked(relativeCoordX, relativeCoordY + 1, inventory.get(1))) {
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
		return "(" + relativeCoordX + ", " + relativeCoordY + ", " + relativeAgentOrientation + ")";
	}

    @Override
    public int hashCode() {
	    return 2399 * relativeCoordX + 2083 * relativeCoordY + 1889 * relativeAgentOrientation;
    }

    @Override
    public boolean equals(Object object) {
	    /*for (Coordinate c : blockadesRemoved) {
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

    public ArrayList<State> generateAStarNeighbors(WorldModel worldModel, ArrayList<Character> allowedItemPickups, ArrayList<Boolean> inventory) {//, char objectInFront, ArrayList<Boolean> inventory) {
        ArrayList<State> newStates = new ArrayList<>();
        switch (relativeAgentOrientation) {
            case 'N':
            case 'S':
                newStates.add(new State(relativeCoordX, relativeCoordY, 'W', blockadesRemoved));
                newStates.add(new State(relativeCoordX, relativeCoordY, 'E', blockadesRemoved));
                break;
            case 'W':
            case 'E':
                newStates.add(new State(relativeCoordX, relativeCoordY, 'N', blockadesRemoved));
                newStates.add(new State(relativeCoordX, relativeCoordY, 'S', blockadesRemoved));
                break;
        }
        if (inventory.size() != 0 && worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation) == '-' && inventory.get(1)) {
            switch (relativeAgentOrientation) {
                case 'N':
                    newStates.add(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, new Coordinate(relativeCoordX, relativeCoordY - 1)));
                    break;
                case 'W':
                    newStates.add(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, new Coordinate(relativeCoordX - 1, relativeCoordY)));
                    break;
                case 'S':
                    newStates.add(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, new Coordinate(relativeCoordX, relativeCoordY + 1)));
                    break;
                case 'E':
                    newStates.add(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, new Coordinate(relativeCoordX + 1, relativeCoordY)));
                    break;
            }
        }
        if (!worldModel.agentBlocked(relativeCoordX, relativeCoordY, relativeAgentOrientation, allowedItemPickups, blockadesRemoved)) {
            switch (relativeAgentOrientation) {
                case 'N':
                    newStates.add(new State(relativeCoordX, relativeCoordY - 1, relativeAgentOrientation, blockadesRemoved));
                    break;
                case 'W':
                    newStates.add(new State(relativeCoordX - 1, relativeCoordY, relativeAgentOrientation, blockadesRemoved));
                    break;
                case 'S':
                    newStates.add(new State(relativeCoordX, relativeCoordY + 1, relativeAgentOrientation, blockadesRemoved));
                    break;
                case 'E':
                    newStates.add(new State(relativeCoordX + 1, relativeCoordY, relativeAgentOrientation, blockadesRemoved));
                    break;
            }
        }
        return newStates;
    }
}
