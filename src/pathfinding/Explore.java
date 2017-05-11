package pathfinding;

import agent.Agent;
import agent.WorldModel;

import java.util.*;

public class Explore {

    /**
     * Uses DFS to find an unexplored tile (a tile where, if the agent stood in it, would reveal tiles not yet seen)
     * @param currentState The current state of the agent
     * @return A state in an unexplored tile
     */
    public static State findUnexploredTile(State currentState, WorldModel worldModel, int relativeCoordX, int relativeCoordY, boolean waterMode) {
        System.out.println("DFS");
        HashSet<State> discovered = new HashSet<>();
        Stack<State> stack = new Stack<>();

        discovered.add(currentState);
        stack.push(currentState);

        while (!stack.isEmpty()) {
            currentState = stack.pop();
            if (worldModel.unexplored(currentState) && !(currentState.getRelativeCoordX() == relativeCoordX && currentState.getRelativeCoordY() == relativeCoordY)) {
                System.out.println("Found unexplored tile " + currentState + " from (" + relativeCoordX + ", " + relativeCoordY + ")");
                return currentState;
            }
            ArrayList<State> neighborStates = currentState.generateNeighbors(worldModel, waterMode);
            for (State state : neighborStates) {
                if (!discovered.contains(state)) {
                    discovered.add(state);
                    state.setParent(currentState);
                    stack.push(state);
                }
            }
        }
        //System.out.println("DFS");
        //System.out.println(discovered);
        return null;
    }

    public static State findUnexploredTile2(State currentState, WorldModel worldModel, int relativeCoordX, int relativeCoordY, boolean waterMode) {
        HashSet<State> discovered = new HashSet<>();
        ArrayList<State> queue = new ArrayList<>();

        discovered.add(currentState);
        queue.add(currentState);

        while (!queue.isEmpty()) {
            currentState = queue.remove(0);
            if (worldModel.unexplored(currentState) && !(currentState.getRelativeCoordX() == relativeCoordX && currentState.getRelativeCoordY() == relativeCoordY)) {
                System.out.println("Found unexplored tile " + currentState + " from (" + relativeCoordX + ", " + relativeCoordY + ")");
                return currentState;
            }
            ArrayList<State> neighborStates = currentState.generateNeighbors(worldModel, waterMode);
            for (State state : neighborStates) {
                if (!discovered.contains(state)) {
                    discovered.add(state);
                    state.setParent(currentState);
                    queue.add(state);
                }
            }
        }
        return null;
    }

    /**
     * Uses A* to find the shortest path from startState to goalState
     * @param startState the state to start the search from
     * @param goalState a goal state coordinate
     * @return a list of states, from the start state to the goal state
     */
    public static ArrayList<State> findPath(State startState, Coordinate goalState, WorldModel worldModel, boolean safeMode, boolean waterMode) {
        HashSet<State> closedSet = new HashSet<>();
        HashSet<State> openSet = new HashSet<>();

        openSet.add(startState);

        startState.setH(startState.heuristic(goalState, worldModel));

        startState.updateG(0);

        while (!openSet.isEmpty()) {
            State bestState = null;
            int bestStateF = Integer.MAX_VALUE;
            for (State state : openSet) {
                if (state.getF() < bestStateF) {
                    bestState = state;
                    bestStateF = state.getF();
                }
            }
            State currentState = bestState;
            if (currentState.getRelativeCoordX() == goalState.x && currentState.getRelativeCoordY() == goalState.y && currentState.getDynamiteCount() >= 0) {
                ArrayList<State> path = new ArrayList<>();
                path.add(currentState);
                while (currentState.getParent() != null) {
                    path.add(currentState.getParent());
                    currentState = currentState.getParent();
                }
                Collections.reverse(path);
                return path;
            }
            openSet.remove(currentState);
            closedSet.add(currentState);
            ArrayList<State> neighborStates;
            if (safeMode) {
                neighborStates = currentState.generateAStarNeighbors(worldModel, currentState.getBlockadesRemoved());
            } else {
                neighborStates = currentState.generatePlannedAStarNeighbors(worldModel, currentState.getBlockadesRemoved(), waterMode);
            }
            for (State state : neighborStates) {
                if (closedSet.contains(state)) {
                    continue;
                }
                int tentativeGScore = currentState.getG() + 1;
                if (!openSet.contains(state)) {
                    state.setH(state.heuristic(goalState, worldModel));
                    openSet.add(state);
                } else {
                    for (State ss : openSet) {  // if we generate a duplicate state, make sure we use the old one
                        if (ss.equals(state)) {
                            state = ss;
                            break;
                        }
                    }
                    if (tentativeGScore >= state.getG()) {
                        continue;
                    }
                }
                state.setParent(currentState);
                state.updateG(tentativeGScore);
            }
        }
        return new ArrayList<>();
    }

    public static int leastDynamitePath(DijkstraCoordinate startState, DijkstraCoordinate goalState, WorldModel worldModel) {

        ArrayList<DijkstraCoordinate> coordinates = worldModel.getExploredTiles();

        for (DijkstraCoordinate coordinate : coordinates) {
            coordinate.setDistance(Integer.MAX_VALUE);
        }

        startState.setDistance(0);
        coordinates.add(startState);

        while (!coordinates.isEmpty()) {
            DijkstraCoordinate bestCoordinate = null;
            int bestCoordinateValue = Integer.MAX_VALUE;
            for (DijkstraCoordinate coordinate : coordinates) {
                if (coordinate.getDistance() < bestCoordinateValue) {
                    bestCoordinate = coordinate;
                    bestCoordinateValue = coordinate.getDistance();
                }
            }
            if (bestCoordinate.x == goalState.x && bestCoordinate.y == goalState.y) {
                return (int) Math.floor(bestCoordinate.getDistance() / 1000);
            }
            coordinates.remove(bestCoordinate);
            for (DijkstraCoordinate coordinate : coordinates) {
                if (isNeighbor(bestCoordinate, coordinate)) {
                    int alt;
                    if (worldModel.getObjectAtCoordinate(coordinate.x, coordinate.y) == '*') {
                        alt = bestCoordinate.getDistance() + 1000;
                    } else {
                        alt = bestCoordinate.getDistance() + 1;
                    }
                    if (alt < coordinate.getDistance()) {
                        coordinate.setDistance(alt);
                        coordinate.setParent(bestCoordinate);
                    }
                }
            }
        }
        return -1;
    }

    private static boolean isNeighbor(DijkstraCoordinate c1, DijkstraCoordinate c2) {
        return (Math.abs(c1.x - c2.x) == 0 && Math.abs(c1.y - c2.y) == 1) || (Math.abs(c1.x - c2.x) == 1 && Math.abs(c1.y - c2.y) == 0);
    }


    /**
     * Takes a list of states and produces a list of characters to make the agent move along the path
     * @param path the path the agent is supposed to move along
     * @return a list of characters indicating the moves the agent should make
     */
    public static ArrayList<Character> generateActions(ArrayList<State> path, WorldModel worldModel) {//, char currentAgentOrientation, WorldModel worldModel) {
        ArrayList<Character> actions = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            State fromState = path.get(i);
            State toState = path.get(i + 1);
            if (fromState.getRelativeCoordX() != toState.getRelativeCoordX() || fromState.getRelativeCoordY() != toState.getRelativeCoordY()) { // position changed, the agent must move forward
                actions.add('f');
            } else if (fromState.getRelativeAgentOrientation() != toState.getRelativeAgentOrientation()) {  // rotation changed, the agent must turn left or right
                if (toState.getRelativeAgentOrientation() == 'W' && fromState.getRelativeAgentOrientation() == 'N' ||       // if turned left
                        toState.getRelativeAgentOrientation() == 'S' && fromState.getRelativeAgentOrientation() == 'W' ||
                        toState.getRelativeAgentOrientation() == 'E' && fromState.getRelativeAgentOrientation() == 'S' ||
                        toState.getRelativeAgentOrientation() == 'N' && fromState.getRelativeAgentOrientation() == 'E') {
                    actions.add('l');
                } else {
                    actions.add('r');
                }
            } else {
                char objectInFront = worldModel.getObjectInFront(fromState.getRelativeCoordX(), fromState.getRelativeCoordY(), fromState.getRelativeAgentOrientation());
                if (objectInFront == '-') {
                    actions.add('u');
                } else if (objectInFront == 'T') {
                    actions.add('c');
                } else if (objectInFront == '*') {
                    actions.add('b');
                }
            }
        }
        return actions;
    }

}
