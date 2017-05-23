package pathfinding;

import agent.Agent;
import agent.WorldModel;

import java.util.*;

public class Explore {

    /**
     * Uses BFS to find an unexplored tile (a tile where, if the agent stood in it, would reveal tiles not yet seen)

     * @param currentCoordinate the current coordinate of the agent
     * @param worldModel the world model of the agent
     * @param hasKey if the agent has the key
     * @param stage which stage the agent is currently in
     * @return a state that is an unexplored tile, null if no tile is found
     */
    public static Coordinate findUnexploredTile(Coordinate currentCoordinate, WorldModel worldModel, boolean hasKey, Agent.Stage stage) {
        HashSet<Coordinate> discovered = new HashSet<>();
        ArrayList<Coordinate> queue = new ArrayList<>();
        int agentCurrentPositionX = currentCoordinate.x;
        int agentCurrentPositionY = currentCoordinate.y;

        discovered.add(currentCoordinate);
        queue.add(currentCoordinate);

        while (!queue.isEmpty()) {
            currentCoordinate = queue.remove(0);
            if (worldModel.isUnexplored(currentCoordinate) && !(currentCoordinate.x == agentCurrentPositionX && currentCoordinate.y == agentCurrentPositionY)) {
                return currentCoordinate;
            }
            ArrayList<Coordinate> neighborCoordinates = currentCoordinate.generateBFSNeighbors(worldModel, hasKey, stage);
            for (Coordinate coordinate : neighborCoordinates) {
                if (!discovered.contains(coordinate)) {
                    discovered.add(coordinate);
                    queue.add(coordinate);
                }
            }
        }
        return null;
    }

    /**
     * Uses A* to find the shortest path from startState to goalState
     *
     * @param startState the state to start the search from
     * @param goalState a goal state coordinate
     * @param worldModel the world model of the agent
     * @param stage which stage the agent is currently in
     * @return a list of states forming a path from the start state to the goal state, empty list if no path is found
     */
    public static ArrayList<State> findPath(State startState, Coordinate goalState, WorldModel worldModel, Agent.Stage stage, ArrayList<Coordinate> legalDynamiteCoordinates) {
        HashSet<State> closedSet = new HashSet<>();
        HashSet<State> openSet = new HashSet<>();
        int cutoff = 25000;

        openSet.add(startState);

        startState.setH(startState.heuristic(goalState, worldModel, stage));

        startState.setG(0);

        while (!openSet.isEmpty() && closedSet.size() < cutoff) {
            State bestState = null;
            int bestStateF = Integer.MAX_VALUE;
            for (State state : openSet) {
                if (state.getF() < bestStateF) {
                    bestState = state;
                    bestStateF = state.getF();
                }
            }
            State currentState = bestState;
            if (currentState.getRelativeCoordX() == goalState.x && currentState.getRelativeCoordY() == goalState.y && (currentState.getDynamiteCount() >= 0 || currentState.getDynamiteCount() < -WorldModel.WORLD_HEIGHT*WorldModel.WORLD_WIDTH)) {
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
            neighborStates = currentState.generateAStarNeighbors(worldModel, stage, legalDynamiteCoordinates);

            for (State state : neighborStates) {
                if (closedSet.contains(state)) {
                    continue;
                }
                int tentativeGScore;
                tentativeGScore = currentState.getG() + 1;
                if (!openSet.contains(state)) {
                    state.setH(state.heuristic(goalState, worldModel, stage));
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
                state.setG(tentativeGScore);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Uses Dijkstra's algorithm to find the shortest path to any tile with a given type.
     *
     * @param type the type of tile to look for
     * @param startState the start state
     * @param worldModel the world model of the agent
     * @return a list of states forming a path from the start state to the goal state, empty list if no path is found
     */
    public static ArrayList<State> findClosestTileOfType(char type, State startState, WorldModel worldModel) {
        ArrayList<Coordinate> coordinates = worldModel.getExploredTiles();
        ArrayList<State> states = new ArrayList<>();

        for (Coordinate coordinate : coordinates) {
            states.addAll(Arrays.asList(
                    new State(coordinate.x, coordinate.y, 'N'),
                    new State(coordinate.x, coordinate.y, 'W'),
                    new State(coordinate.x, coordinate.y, 'S'),
                    new State(coordinate.x, coordinate.y, 'E')
            ));
        }
        for (State state : states) {
            state.setG(Integer.MAX_VALUE);
        }
        startState.setG(0);
        states.add(startState);

        while (!states.isEmpty()) {
            State bestState = null;
            int bestCoordinateValue = Integer.MAX_VALUE;
            for (State state : states) {
                if (state.getG() < bestCoordinateValue) {
                    bestState = state;
                    bestCoordinateValue = state.getG();
                }
            }
            if (bestState == null) {
                return new ArrayList<>();
            }
            if (worldModel.getObjectAtCoordinate(bestState.getRelativeCoordX(), bestState.getRelativeCoordY()) == type) {
                ArrayList<State> path = new ArrayList<>();
                path.add(bestState);
                while (bestState.getParent() != null) {
                    path.add(bestState.getParent());
                    bestState = bestState.getParent();
                }
                Collections.reverse(path);
                return path;
            }
            states.remove(bestState);
            for (State state : states) {
                if (isNeighbor(bestState, state, worldModel, type)) {
                    int alt = bestState.getG() + 1;
                    if (alt < state.getG()) {
                        state.setG(alt);
                        state.setParent(bestState);
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    public static ArrayList<Coordinate> leastDynamitePath(State startState, Coordinate goalState, WorldModel worldModel) {
        ArrayList<Coordinate> coordinates = worldModel.getExploredTiles();
        ArrayList<State> states = new ArrayList<>();

        for (Coordinate coordinate : coordinates) {
            State state = new State(coordinate.x, coordinate.y, 'N');
            state.setG(Integer.MAX_VALUE);
            states.add(state);
        }

        startState.setG(0);
        states.add(startState);

        while (!states.isEmpty()) {
            State bestState = null;
            int bestStateValue = Integer.MAX_VALUE;
            for (State state : states) {
                if (state.getG() < bestStateValue) {
                    bestState = state;
                    bestStateValue = state.getG();
                }
            }
            if (bestState.getRelativeCoordX() == goalState.x && bestState.getRelativeCoordY() == goalState.y) {
                ArrayList<Coordinate> dynamiteCoordinates = new ArrayList<>();
                if (worldModel.getObjectAtCoordinate(bestState.getRelativeCoordX(), bestState.getRelativeCoordY()) == '*') {
                    dynamiteCoordinates.add(new Coordinate(bestState.getRelativeCoordX(), bestState.getRelativeCoordY()));
                }
                while (bestState.getParent() != null) {
                    if (worldModel.getObjectAtCoordinate(bestState.getParent().getRelativeCoordX(), bestState.getParent().getRelativeCoordY()) == '*') {
                        dynamiteCoordinates.add(new Coordinate(bestState.getParent().getRelativeCoordX(), bestState.getParent().getRelativeCoordY()));
                    }
                    bestState = bestState.getParent();
                }
                return dynamiteCoordinates;
                //return (int) Math.floor(bestState.getG() / 1000);
            }
            states.remove(bestState);
            for (State state : states) {
                if (bestState.getRelativeCoordX() == state.getRelativeCoordX() && Math.abs(bestState.getRelativeCoordY() - state.getRelativeCoordY()) == 1 ||
                        bestState.getRelativeCoordY() == state.getRelativeCoordY() && Math.abs(bestState.getRelativeCoordX() - state.getRelativeCoordX()) == 1) {
                    int alt;
                    if (worldModel.getObjectAtCoordinate(state.getRelativeCoordX(), state.getRelativeCoordY()) == '*') {
                        alt = bestState.getG() + 1000;
                    } else {
                        alt = bestState.getG() + 1;
                    }
                    if (alt < state.getG()) {
                        state.setG(alt);
                        state.setParent(bestState);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Used by Dijkstra's algorithm to check if two states are adjacent (that the agent can move from s1 to s2 with one action).
     *
     * @param s1 the from-state
     * @param s2 the to-state
     * @param worldModel the world model of the agent
     * @param type the type of tile the search algorithm is looking for
     * @return true if the two states are neighbors, false otherwise
     */
    private static boolean isNeighbor(State s1, State s2, WorldModel worldModel, char type) {
        if (!(worldModel.getObjectAtCoordinate(s1.getRelativeCoordX(), s1.getRelativeCoordY()) == ' ' && (worldModel.getObjectAtCoordinate(s2.getRelativeCoordX(), s2.getRelativeCoordY()) == ' ' || worldModel.getObjectAtCoordinate(s2.getRelativeCoordX(), s2.getRelativeCoordY()) == type))) {
            return false;
        }
        if (s1.getRelativeCoordX() - s2.getRelativeCoordX() == 0 && s1.getRelativeCoordY() - s2.getRelativeCoordY() == 0) {
            ArrayList<Character> directions = new ArrayList<>(Arrays.asList('N', 'W', 'S', 'E'));
            if (Math.abs(directions.indexOf(s1.getRelativeAgentOrientation()) - directions.indexOf(s2.getRelativeAgentOrientation())) == 1 ||
                    (s1.getRelativeAgentOrientation() == 'N' && s2.getRelativeAgentOrientation() == 'E') ||
                    (s1.getRelativeAgentOrientation() == 'E' && s2.getRelativeAgentOrientation() == 'N')) {
                return true;
            }
        }
        switch (s1.getRelativeAgentOrientation()) {
            case 'N':
                return s1.getRelativeCoordX() - s2.getRelativeCoordX() == 0 && s1.getRelativeCoordY() - s2.getRelativeCoordY() == 1 && s2.getRelativeAgentOrientation() == 'N';
            case 'W':
                return s1.getRelativeCoordX() - s2.getRelativeCoordX() == 1 && s1.getRelativeCoordY() - s2.getRelativeCoordY() == 0 && s2.getRelativeAgentOrientation() == 'W';
            case 'S':
                return s1.getRelativeCoordX() - s2.getRelativeCoordX() == 0 && s1.getRelativeCoordY() - s2.getRelativeCoordY() == -1 && s2.getRelativeAgentOrientation() == 'S';
            case 'E':
                return s1.getRelativeCoordX() - s2.getRelativeCoordX() == -1 && s1.getRelativeCoordY() - s2.getRelativeCoordY() == 0 && s2.getRelativeAgentOrientation() == 'E';
        }
        return false;
    }


    /**
     * Takes a list of states and produces a list of characters to make the agent move along the path
     *
     * @param path the path the agent is supposed to move along
     * @param worldModel the world model of the agent
     * @return a list of characters indicating the moves the agent should make
     */
    public static ArrayList<Character> generateActions(ArrayList<State> path, WorldModel worldModel) {
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
