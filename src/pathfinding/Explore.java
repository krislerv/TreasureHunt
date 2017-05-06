package pathfinding;

import agent.WorldModel;

import java.util.*;

public class Explore {

    /**
     * Uses DFS to find an unexplored tile (a tile where, if the agent stood in it, would reveal tiles not yet seen)
     * @param currentState The current state of the agent
     * @return A state in an unexplored tile
     */
    public static State findUnexploredTile(State currentState, WorldModel worldModel, int relativeCoordX, int relativeCoordY, boolean hasKey) {
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
            ArrayList<State> neighborStates = currentState.generateNeighbors(worldModel);
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

    public static State findUnexploredTile2(State currentState, WorldModel worldModel, int relativeCoordX, int relativeCoordY) {
        System.out.println("BFS");
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
            ArrayList<State> neighborStates = currentState.generateNeighbors(worldModel);
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
    public static ArrayList<State> findPath(State startState, Coordinate goalState, WorldModel worldModel, boolean safe) {
        HashSet<State> closedSet = new HashSet<>();
        HashSet<State> openSet = new HashSet<>();

        openSet.add(startState);

        startState.setH(startState.heuristic(goalState));

        startState.updateG(0);

        while (!openSet.isEmpty()) {
            State bestState = null;
            int bestStateF = 999;
            for (State state : openSet) {
                if (state.getF() < bestStateF) {
                    bestState = state;
                    bestStateF = state.getF();
                }
            }
            State currentState = bestState;
            if (currentState.getRelativeCoordX() == goalState.x && currentState.getRelativeCoordY() == goalState.y) {
                //System.out.println("GOAL " + currentState);
                ArrayList<State> path = new ArrayList<>();
                path.add(currentState);
                while (currentState.getParent() != null) {
                    path.add(currentState.getParent());
                    currentState = currentState.getParent();
                }
                Collections.reverse(path);
                //System.out.println(path);
                return path;
            }
            openSet.remove(currentState);
            closedSet.add(currentState);
            ArrayList<State> neighborStates;
            if (safe) {
                neighborStates = currentState.generateAStarNeighbors(worldModel, currentState.getBlockadesRemoved());
            } else {
                neighborStates = currentState.generateUnsafeAStarNeighbors(worldModel, currentState.getBlockadesRemoved());
            }
            for (State state : neighborStates) {
                if (closedSet.contains(state)) {
                    continue;
                }
                int tentativeGScore = currentState.getG() + 1;
                if (!openSet.contains(state)) {
                    openSet.add(state);
                } else {
                    for (State ss : openSet) {  // if we generate a duplicate state, make sure we use the old one
                        if (ss.getRelativeCoordX() == state.getRelativeCoordX() && ss.getRelativeCoordY() == state.getRelativeCoordY()) {
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
        //System.out.println("We ain't found shit");
        return new ArrayList<>();
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
        /*System.out.println(path);
        ArrayList<Character> directions = new ArrayList<>(Arrays.asList('N', 'W', 'S', 'E'));
        HashMap<Character, Integer> xOffset = new HashMap<>();
        xOffset.put('N', 0);
        xOffset.put('W', -1);
        xOffset.put('S', 0);
        xOffset.put('E', 1);
        HashMap<Character, Integer> yOffset = new HashMap<>();
        yOffset.put('N', -1);
        yOffset.put('W', 0);
        yOffset.put('S', 1);
        yOffset.put('E', 0);
        ArrayList<Character> actions = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            State fromState = path.get(i);
            State toState = path.get(i + 1);

            int xDiff = toState.getRelativeCoordX() - fromState.getRelativeCoordX();
            int yDiff = toState.getRelativeCoordY() - fromState.getRelativeCoordY();

            char newDirection = 0;

            if (xDiff == -1) {
                newDirection = 'W';
            } else if (xDiff == 1) {
                newDirection = 'E';
            } else if (yDiff == -1) {
                newDirection = 'N';
            } else if (yDiff == 1) {
                newDirection = 'S';
            }
            if (xDiff == 0 && yDiff == 0) {
                char objectInFront = worldModel.getObjectAtCoordinate(fromState.getRelativeCoordX() + xOffset.get(currentAgentOrientation), fromState.getRelativeCoordY() + yOffset.get(currentAgentOrientation));
                if (objectInFront == '-') {
                    actions.add('u');
                } else if (objectInFront == 'T') {
                    actions.add('c');
                } else if (objectInFront == '*') {
                    actions.add('b');
                }
            } else {
                int directionDifference = directions.indexOf(newDirection) - directions.indexOf(currentAgentOrientation);

                if (directionDifference == 0) {
                    actions.add('f');
                } else if (directionDifference == 1) {
                    actions.add('l');
                    actions.add('f');
                } else if (directionDifference == 2) {
                    actions.add('l');
                    actions.add('l');
                    actions.add('f');
                } else if (directionDifference == 3) {
                    actions.add('r');
                    actions.add('f');
                } else if (directionDifference == -1) {
                    actions.add('r');
                    actions.add('f');
                } else if (directionDifference == -2) {
                    actions.add('r');
                    actions.add('r');
                    actions.add('f');
                } else if (directionDifference == -3) {
                    actions.add('l');
                    actions.add('f');
                }
                currentAgentOrientation = newDirection;
            }
        }
        return actions;*/
    }

}
