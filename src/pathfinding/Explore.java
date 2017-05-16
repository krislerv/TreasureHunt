package pathfinding;

import agent.WorldModel;

import java.util.*;

public class Explore {

    /**
     * Uses BFS to find an unexplored tile (a tile where, if the agent stood in it, would reveal tiles not yet seen)

     * @param currentState the current state of the agent
     * @param worldModel the world model of the agent
     * @param relativeCoordX the relative x coordinate of the agent
     * @param relativeCoordY the relative y coordinate of the agent
     * @param waterMode
     * @param lumberjackMode
     * @return a state that is an unexplored tile
     */
    public static State findUnexploredTile(State currentState, WorldModel worldModel, int relativeCoordX, int relativeCoordY, boolean waterMode, boolean lumberjackMode) {
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
            ArrayList<State> neighborStates = currentState.generateNeighbors(worldModel, waterMode, lumberjackMode);
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
     * @param worldModel
     * @param safeMode
     * @param waterMode
     * @return a list of states, from the start state to the goal state
     */
    public static ArrayList<State> findPath(State startState, Coordinate goalState, WorldModel worldModel, boolean safeMode, boolean waterMode) {
        long startTime = System.currentTimeMillis();
        HashSet<State> closedSet = new HashSet<>();
        HashSet<State> openSet = new HashSet<>();

        openSet.add(startState);

        startState.setH(startState.heuristic(goalState, worldModel));

        startState.setG(0);

        while (!openSet.isEmpty()) {
            /*if (System.currentTimeMillis() - startTime > 5000) {
                return new ArrayList<>();
            }*/
            State bestState = null;
            int bestStateF = Integer.MAX_VALUE;
            for (State state : openSet) {
                if (state.getF() < bestStateF) {
                    bestState = state;
                    bestStateF = state.getF();
                }
            }
            State currentState = bestState;
                if (currentState.getRelativeCoordX() == goalState.x && currentState.getRelativeCoordY() == goalState.y && (currentState.getDynamiteCount() >= 0 || currentState.getDynamiteCount() < -200000)) {
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
                state.setG(tentativeGScore);
            }
        }
        return new ArrayList<>();
    }

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
