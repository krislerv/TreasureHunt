package agent;

import pathfinding.Coordinate;
import pathfinding.State;

import java.util.*;

/**
 * This class keeps a map of all the area the agent has explored.
 */
public class WorldModel {

    /**
     * A 2D ArrayList to keep track of the world
     */
    private ArrayList<ArrayList<Character>> world;

    /**
     * The maximum size of the world is 80x80 and the agent can start anywhere within it.
     * This world size ensures that no matter where the agent starts, it has enough room to maneuver
     * without getting an IndexOutOfBoundsException.
     */
    public static final int WORLD_WIDTH = 164;
    public static final int WORLD_HEIGHT = 164;

    /**
     * The base coordinates are used together with the relative coordinates of the agent to update the
     * map when the agent moves around.
     * The base coordinates of the agent are in the middle of the world.
     */
    private static final int baseCoordX = WORLD_WIDTH / 2;
    private static final int baseCoordY = WORLD_HEIGHT / 2;

    /**
     * Keeps track of the upper and lower bounds of coordinates that have been explored. This makes updating
     * the map go faster and also allows for printing only the explored part of the map, not the entire 164x164 world.
     */
    private int minExploredX = WORLD_WIDTH, minExploredY = WORLD_HEIGHT, maxExploredX = 0, maxExploredY = 0;

    /**
     * Constructor for the world model. Adds question marks to all squares on the map.
     */
    WorldModel() {
        world = new ArrayList<>();
        for (int i = 0; i < WORLD_HEIGHT; i++) {
            world.add(new ArrayList<>());
            for (int j = 0; j < WORLD_WIDTH; j++) {
                world.get(i).add('?');
            }
        }
    }

    /**
     * Updates the world model given the view of the agent.
     *
     * @param view what the agent perceived after the last action
     * @param relativeCoordX the relative x coordinate of the agent
     * @param relativeCoordY the relative y coordinate of the agent
     * @param relativeAgentOrientation the relative orientation of the agent
     */
    void updateWorldModel(char[][] view, int relativeCoordX, int relativeCoordY, char relativeAgentOrientation) {
        view = rotateView(view, relativeAgentOrientation);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (i == 2 && j == 2) {
                    continue;   // ignore agent position
                }
                world.get(baseCoordY + relativeCoordY + i - 2).set(baseCoordX + relativeCoordX + j - 2, view[i][j]);
                if (baseCoordY + relativeCoordY + i - 2 < minExploredY) {
                    minExploredY = baseCoordY + relativeCoordY + i - 2;
                } else if (baseCoordY + relativeCoordY + i - 2 > maxExploredY) {
                    maxExploredY = baseCoordY + relativeCoordY + i - 2;
                }
                if (baseCoordX + relativeCoordX + j - 2 < minExploredX) {
                    minExploredX = baseCoordX + relativeCoordX + j - 2;
                } else if (baseCoordX + relativeCoordX + j - 2 > maxExploredX) {
                    maxExploredX = baseCoordX + relativeCoordX + j - 2;
                }
            }
        }
        //printWorld(relativeCoordX, relativeCoordY, relativeAgentOrientation);
    }

    /**
     * When the agent rotates around we have to make sure that what the agent perceives
     * is aligned to the same direction every time.
     *
     * @param view what the agent perceived after the last action
     * @param relativeAgentOrientation the relative orientation of the agent
     * @return the view, rotated north
     */
    private char[][] rotateView(char[][] view, char relativeAgentOrientation) {
        if (relativeAgentOrientation == 'N') {
            return view;
        }
        int degrees = 0;
        if (relativeAgentOrientation == 'W') {
            degrees = 270;
        } else if (relativeAgentOrientation == 'S') {
            degrees = 180;
        } else if (relativeAgentOrientation == 'E') {
            degrees = 90;
        }
        for (int k = 0; k < degrees / 90; k++) {
            char newView[][] = new char[5][5];
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    newView[i][j] = view[5 - j - 1][i];
                }
            }
            view = newView;
        }
    return view;
    }

    /**
     * Returns whether there is a blockade in front of the agent.
     *
     * @param relativeCoordX the relative x coordinate of the agent
     * @param relativeCoordY the relative y coordinate of the agent
     * @param relativeAgentOrientation the relative orientation of the agent
     * @param blockadesRemoved a HashSet of the blockades that have been removed
     * @return true if the agent is blocked, false otherwise
     */
    public boolean agentBlocked(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation, HashSet<Coordinate> blockadesRemoved) {
        ArrayList<Character> blockades = new ArrayList<>(Arrays.asList('~', '.', '*', 'T', '-', '?'));
        return blockades.contains(world.get(baseCoordY + relativeCoordY + State.yOffset.get(relativeAgentOrientation)).get(baseCoordX + relativeCoordX + State.xOffset.get(relativeAgentOrientation))) && !blockadesRemoved.contains(new Coordinate(relativeCoordX + State.xOffset.get(relativeAgentOrientation), relativeCoordY + State.yOffset.get(relativeAgentOrientation)));
    }

    /**
     * Returns whether a position is blocked or not.
     *
     * @param relativeCoordX the relative x coordinate of the agent
     * @param relativeCoordY the relative y coordinate of the agent
     * @param hasKey if the agent has the key
     * @param blockadesRemoved a HashSet of the blockades that have been removed
     * @param stage which stage the agent is currently in
     * @return true if the position if blocked, false otherwise
     */
    public boolean positionBlocked(int relativeCoordX, int relativeCoordY, boolean hasKey, HashSet<Coordinate> blockadesRemoved, Agent.Stage stage) {
        ArrayList<Character> blockades = new ArrayList<>(Arrays.asList('~', '.', '*', 'T', '-', '?'));
        if (stage == Agent.Stage.LUMBERJACK) {
            blockades = new ArrayList<>(Arrays.asList('.', '*', '-', '?'));
        }
        if (hasKey) {
            blockades.remove((Character)'-');
        }
        if (stage == Agent.Stage.WATER) {
            blockades = new ArrayList<>(Arrays.asList(' ', '.', '*', 'T', '-', '?', '$', 'k', 'a', 'd'));
        }
        return blockades.contains(getObjectAtCoordinate(relativeCoordX, relativeCoordY)) && !blockadesRemoved.contains(new Coordinate(relativeCoordX, relativeCoordY));
    }

    /**
     * Prints the currently known world.
     *
     * @param relativeCoordX the relative x coordinate of the agent
     * @param relativeCoordY the relative y coordinate of the agent
     * @param relativeAgentOrientation the relative orientation of the agent
     */
    private void printWorld(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation) {
        int minX = minExploredX;
        int minY = minExploredY;
        int maxX = maxExploredX + 1;
        int maxY = maxExploredY + 1;
        for (int i = minY; i < maxY; i++) {
            for (int j = minX; j < maxX; j++) {
                if (baseCoordY + relativeCoordY == i && baseCoordX + relativeCoordX == j) {
                    switch (relativeAgentOrientation) {
                        case 'N':
                            System.out.print("^");
                            break;
                        case 'W':
                            System.out.print("<");
                            break;
                        case 'S':
                            System.out.print("v");
                            break;
                        case 'E':
                            System.out.print(">");
                            break;
                    }
                } else {
                    System.out.print(world.get(i).get(j));
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Checks if the given state is unexplored, meaning that there is a question mark in the 5x5 square centered in the state.
     *
     * @param currentState the given state
     * @return true if the state is unexplored, false otherwise
     */
    public boolean unexplored(State currentState) {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (world.get(baseCoordY + currentState.getRelativeCoordY() + i - 2).get(baseCoordX + currentState.getRelativeCoordX() + j - 2) == '?') {   // if there is a question mark in the 5x5 area around tile
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a list with coordinates of all tiles with the given object
     *
     * @param objectType which object to search for
     * @return list with coordinates of all tiles with the given object
     */
    ArrayList<Coordinate> getObjectsTiles(Character objectType) {
        ArrayList<Coordinate> objectTiles = new ArrayList<>();
        for (int i = minExploredY; i < maxExploredY + 1; i++) {
            for (int j = minExploredX; j < maxExploredX + 1; j++) {
                if (world.get(i).get(j) == objectType) {
                    objectTiles.add(new Coordinate(j - baseCoordX, i - baseCoordY));
                }
            }
        }
        return objectTiles;
    }

    /**
     * Returns a list with coordinates of all tiles with the given object, sorted by distance from the current state
     *
     * @param objectType which object to search for
     * @param currentState the current state
     * @return list with coordinates of all tiles with the given object
     */
    ArrayList<Coordinate> getObjectsTilesSortedByDistance(Character objectType, State currentState) {
        ArrayList<Coordinate> objectTiles = getObjectsTiles(objectType);
        objectTiles.sort(Comparator.comparingInt(c -> (Math.abs(c.x - currentState.getRelativeCoordX()) + Math.abs(c.y - currentState.getRelativeCoordY()))));
        return objectTiles;
    }

    /**
     * Returns the object in front of the given coordinate
     *
     * @param relativeCoordX the relative x coordinate
     * @param relativeCoordY the relative y coordinate
     * @param relativeAgentOrientation the relative orientation
     * @return the object in front of the given coordinate
     */
    public char getObjectInFront(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation) {
        return world.get(baseCoordY + relativeCoordY + State.yOffset.get(relativeAgentOrientation)).get(baseCoordX + relativeCoordX + State.xOffset.get(relativeAgentOrientation));
    }

    /**
     * Returns the object at the given coordinate.
     *
     * @param relativeCoordX the relative x coordinate
     * @param relativeCoordY the relative y coordinate
     * @return the object at the given coordinate
     */
    public char getObjectAtCoordinate(int relativeCoordX, int relativeCoordY) {
        return world.get(baseCoordY + relativeCoordY).get(baseCoordX + relativeCoordX);
    }

    /**
     * Returns a list of the coordinates of all explored tiles in the world.
     *
     * @return a list of the coordinates of all explored tiles in the world
     */
    public ArrayList<Coordinate> getExploredTiles() {
        ArrayList<Coordinate> coordinates = new ArrayList<>();
        for (int i = minExploredY - WORLD_HEIGHT/2; i < maxExploredY - WORLD_HEIGHT/2 + 1; i++) {
            for (int j = minExploredX - WORLD_WIDTH/2; j < maxExploredX - WORLD_WIDTH/2 + 1; j++) {
                if (getObjectAtCoordinate(j, i) != '?') {
                    coordinates.add(new Coordinate(j, i));
                }
            }
        }
        return coordinates;
    }

    /**
     * Returns the number of known dynamites in the world.
     *
     * @return the number of known dynamites in the world
     */
    int getAvailableDynamiteCount(int relativeCoordX, int relativeCoordY) {
        int availableDynamite = 0;
        for (int i = minExploredY - WORLD_HEIGHT/2; i < maxExploredY - WORLD_HEIGHT/2 + 1; i++) {
            for (int j = minExploredX - WORLD_WIDTH/2; j < maxExploredX - WORLD_WIDTH/2 + 1; j++) {
                if (getObjectAtCoordinate(j, i) == 'd' && !(j == relativeCoordX && i == relativeCoordY)) {
                    availableDynamite++;
                }
            }
        }
        return availableDynamite;
    }

    /**
     * Returns the manhattan distance to the closest known dynamite in the world.
     *
     * @param relativeCoordX the relative x coordinate
     * @param relativeCoordY the relative y coordinate
     * @return the manhattan distance to the closest known dynamite in the world
     */
    public int getMinDynamiteDistance(int relativeCoordX, int relativeCoordY, HashSet<Coordinate> blockadesRemoved) {
        int minDynamiteDistance = 999;
        for (int i = minExploredY - WORLD_HEIGHT/2; i < maxExploredY - WORLD_HEIGHT/2 + 1; i++) {
            for (int j = minExploredX - WORLD_WIDTH/2; j < maxExploredX - WORLD_WIDTH/2 + 1; j++) {
                if (getObjectAtCoordinate(j, i) == 'd'  && ! blockadesRemoved.contains(new Coordinate(j, i))) {
                    if (Math.abs(relativeCoordX - j) + Math.abs(relativeCoordY - i) < minDynamiteDistance) {
                        minDynamiteDistance = Math.abs(relativeCoordX - j) + Math.abs(relativeCoordY - i);
                    }
                }
            }
        }
        return minDynamiteDistance;
    }

    /**
     * Returns a list with coordinates to every known dynamite.
     *
     * @param blockadesRemoved a HashSet of the blockades that have been removed
     * @return
     */
    public ArrayList<Coordinate> getAllDynamites(HashSet<Coordinate> blockadesRemoved) {
        ArrayList<Coordinate> dynamites = new ArrayList<>();
        for (int i = minExploredY - WORLD_HEIGHT/2; i < maxExploredY - WORLD_HEIGHT/2 + 1; i++) {
            for (int j = minExploredX - WORLD_WIDTH/2; j < maxExploredX - WORLD_WIDTH/2 + 1; j++) {
                if (getObjectAtCoordinate(j, i) == 'd' && ! blockadesRemoved.contains(new Coordinate(j, i))) {
                    dynamites.add(new Coordinate(j, i));
                }
            }
        }
        return dynamites;
    }
}
