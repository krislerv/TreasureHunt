package agent;

import pathfinding.Coordinate;
import pathfinding.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class WorldModel {

    private ArrayList<ArrayList<Character>> world;
    private int baseCoordX, baseCoordY;

    public static final int WORLD_WIDTH = 160;
    public static final int WORLD_HEIGHT = 160;

    public WorldModel() {
        world = new ArrayList<>();
        for (int i = 0; i < WORLD_HEIGHT; i++) {
            world.add(new ArrayList<>());
            for (int j = 0; j < WORLD_WIDTH; j++) {
                world.get(i).add('?');
            }
        }
        baseCoordX = WORLD_WIDTH / 2;
        baseCoordY = WORLD_HEIGHT / 2;
    }

    public void updateWorldModel(char[][] view, int relativeCoordX, int relativeCoordY, char relativeAgentOrientation) {
        view = rotateView(view, relativeAgentOrientation);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (i == 2 && j == 2) {
                    continue;   // ignore agent position
                }
                world.get(baseCoordY + relativeCoordY + i - 2).set(baseCoordX + relativeCoordX + j - 2, view[i][j]);
            }
        }
        printWorld(relativeCoordX, relativeCoordY, relativeAgentOrientation);
    }

    public char[][] rotateView(char[][] view, char relativeAgentOrientation) {
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

    public boolean agentBlocked(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation, HashSet<Coordinate> blockadesRemoved) {
        ArrayList<Character> blockades = new ArrayList<>(Arrays.asList('~', '*', 'T', '-', '?'));
        switch (relativeAgentOrientation) {
            case 'N':
                return blockades.contains(world.get(baseCoordY + relativeCoordY - 1).get(baseCoordX + relativeCoordX)) && !blockadesRemoved.contains(new Coordinate(relativeCoordX, relativeCoordY - 1));
            case 'W':
                return blockades.contains(world.get(baseCoordY + relativeCoordY).get(baseCoordX + relativeCoordX - 1)) && !blockadesRemoved.contains(new Coordinate(relativeCoordX - 1, relativeCoordY));
            case 'S':
                return blockades.contains(world.get(baseCoordY + relativeCoordY + 1).get(baseCoordX + relativeCoordX)) && !blockadesRemoved.contains(new Coordinate(relativeCoordX, relativeCoordY + 1));
            case 'E':
                return blockades.contains(world.get(baseCoordY + relativeCoordY).get(baseCoordX + relativeCoordX + 1)) && !blockadesRemoved.contains(new Coordinate(relativeCoordX + 1, relativeCoordY));
        }
        return false;
    }

    public boolean positionBlocked(int relativeCoordX, int relativeCoordY, boolean hasKey, HashSet<Coordinate> blockadesRemoved) {
        ArrayList<Character> blockades = new ArrayList<>(Arrays.asList('~', '*', 'T', '-', '?'));
        if (hasKey) {
            blockades.remove((Character)'-');
        }
        return blockades.contains(world.get(baseCoordY + relativeCoordY).get(baseCoordX + relativeCoordX)) && !blockadesRemoved.contains(new Coordinate(relativeCoordX, relativeCoordY));
    }


    public void printWorld(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation) {
        for (int i = 0; i < WORLD_HEIGHT; i++) {
            for (int j = 0; j < WORLD_WIDTH; j++) {
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

    public ArrayList<Coordinate> getObjectsTiles(Character objectType) {
        ArrayList<Coordinate> objectTiles = new ArrayList<>();
        for (int i = 0; i < WORLD_HEIGHT; i++) {
            for (int j = 0; j < WORLD_WIDTH; j++) {
                if (world.get(i).get(j) == objectType) {
                    objectTiles.add(new Coordinate(j - baseCoordX, i - baseCoordY));
                }
            }
        }
        return objectTiles;
    }

    public char getObjectInFront(int relativeCoordX, int relativeCoordY, char relativeAgentOrientation) {
        switch (relativeAgentOrientation) {
            case 'N':
                return world.get(baseCoordY + relativeCoordY - 1).get(baseCoordX + relativeCoordX);
            case 'W':
                return world.get(baseCoordY + relativeCoordY).get(baseCoordX + relativeCoordX - 1);
            case 'S':
                return world.get(baseCoordY + relativeCoordY + 1).get(baseCoordX + relativeCoordX);
            case 'E':
                return world.get(baseCoordY + relativeCoordY).get(baseCoordX + relativeCoordX + 1);
        }
        return 0;
    }

    public char getObjectAtCoordinate(int relativeCoordX, int relativeCoordY) {
        return world.get(baseCoordY + relativeCoordY).get(baseCoordX + relativeCoordX);
    }
}
