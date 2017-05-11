package agent;

import pathfinding.Coordinate;
import pathfinding.Explore;
import pathfinding.State;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class Agent {

    private WorldModel worldModel;
    private int relativeCoordX, relativeCoordY;
    private char relativeAgentOrientation;

    private boolean hasGold, hasKey, hasAxe, hasRaft, onRaft;
    private int dynamiteCount;

    private ArrayList<Character> moveBuffer;

    private Stage currentStage;

    private HashSet<Coordinate> blockadesRemoved;

    public enum Stage {
        EXPLORE, PLANNED, UNSAFE
    }

    private int priority;

    public Agent() {
        worldModel = new WorldModel();
        relativeCoordX = 0;
        relativeCoordY = 0;
        relativeAgentOrientation = 'S'; // we don't know which way we're facing (and it doesn't matter), so just call the initial direction south
        moveBuffer = new ArrayList<>();
        currentStage = Stage.EXPLORE;
        blockadesRemoved = new HashSet<>(); //  doors opened, walls blown up, trees cut down
    }

    private boolean explore(boolean safeMode, boolean waterMode) {
        State unexploredTile = Explore.findUnexploredTile2(
                new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount),
                worldModel,
                relativeCoordX,
                relativeCoordY,
                waterMode);

        if (unexploredTile == null) {
            return false;
        }

        ArrayList<State> path = Explore.findPath(
                new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, waterMode, dynamiteCount),
                new Coordinate(unexploredTile.getRelativeCoordX(), unexploredTile.getRelativeCoordY()),
                worldModel,
                safeMode,
                waterMode);

        System.out.println(path);

        ArrayList<Character> actions = Explore.generateActions(path, worldModel);
        //System.out.println(actions);
        moveBuffer = actions;
        return !moveBuffer.isEmpty();
    }

    private boolean collect() {
        ArrayList<Character> objects = new ArrayList<>(Arrays.asList('$', 'k', 'd', 'a'));  // the priority order for objects
        for (Character objectType : objects) {                  // for each object type
            System.out.println(objectType);
            ArrayList<Coordinate> tiles = worldModel.getObjectsTiles(objectType);     // find all tiles containing given object
            System.out.println(tiles);
            if (!tiles.isEmpty()) {                             // if a tile containing given object type was found
                for (Coordinate coordinate : tiles) {                     // check each tile found to see if there is a path there from current position
                    System.out.println(coordinate);
                    if (coordinate.x == relativeCoordX && coordinate.y == relativeCoordY) {
                        continue;   // the world model doesn't get updated until the agent actually moves, so when the agent picks up an item, the world model thinks the item is still there
                    }               // this makes sure the agent actually moves after picking up an item to allow the world model to update
                    ArrayList<State> path = Explore.findPath(
                            new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount),
                            coordinate,
                            worldModel,
                            true,
                            false);
                    if (path.size() != 0) {     // if the returned path is not empty, a path was found
                        moveBuffer = Explore.generateActions(path, worldModel);
                        System.out.println("MOVEBUFFER " + moveBuffer);
                        System.out.println("Agent: " + relativeCoordX + " " + relativeCoordY + " " + relativeAgentOrientation);
                        System.out.println(objectType + " " + coordinate + " " + path);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean solutionExplore() {
        ArrayList<Coordinate> goldStates = worldModel.getObjectsTiles('$');
        if (goldStates.size() == 0) {
            return false;
        }
        for (Coordinate coordinate : goldStates) {
            int dynamiteAvailable = worldModel.getAvailableDynamite();  // finds the number of known dynamites in the world
            ArrayList<State> path = null;
            for (int i = -dynamiteAvailable; i <= -dynamiteAvailable + dynamiteCount; i++) {    // tries to find a path to the gold using as few dynamite as possible
                path = Explore.findPath(                                                        // starts with -dynamiteAvailable, meaning you have to pick up dynamites before going to the gold
                        new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, onRaft, i),
                        coordinate,
                        worldModel,
                        false,
                        false);
                if (path.size() != 0) {
                    dynamiteAvailable = dynamiteCount - i;      // however many dynamite are left is how many we will have to get from the gold to the start position
                    break;
                }
            }
            if (path != null && path.size() != 0) {     // if the returned path is not empty, a path was found
                State startState = path.get(path.size() - 1);
                startState.setDynamiteCount(dynamiteAvailable);
                ArrayList<State> path2 = Explore.findPath(
                        startState,
                        new Coordinate(0, 0),
                        worldModel,
                        false,
                        false);
                if (path2.size() != 0) {
                    //moveBuffer = Explore.generateActions(path, worldModel); // don't need to include this because when we generate the path in the second call to findPath(), it will go from (0,0) to startState, but because
                    // we took startState directly from the path from the first call to findPath() it will still have parents from that call
                    moveBuffer = Explore.generateActions(path2, worldModel);
                    System.out.println("MOVEBUFFER " + moveBuffer);
                    System.out.println("Agent: " + relativeCoordX + " " + relativeCoordY + " " + relativeAgentOrientation);
                    System.out.println("$$$" + " " + coordinate + " " + path2);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean goToWater() {
        ArrayList<Coordinate> waterStates = worldModel.getObjectsTiles('~');
        if (waterStates.size() == 0) {
            return false;
        }
        for (Coordinate coordinate : waterStates) {
            ArrayList<State> path = Explore.findPath(
                    new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, false, false, hasAxe, false, false, 0),
                    coordinate,
                    worldModel,
                    false,
                    false);
            if (path.size() != 0) {     // if the returned path is not empty, a path was found
                moveBuffer = Explore.generateActions(path, worldModel);
                System.out.println("MOVEBUFFER " + moveBuffer);
                System.out.println("Agent: " + relativeCoordX + " " + relativeCoordY + " " + relativeAgentOrientation);
                System.out.println("go to water" + " " + coordinate + " " + path);
                return true;
            }
        }
        return false;
    }

    private boolean goHome() {
        ArrayList<State> path = Explore.findPath(
                new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved, hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount),
                new Coordinate(0, 0),
                worldModel,
                false,
                false);

        System.out.println(path);

        ArrayList<Character> actions = Explore.generateActions(path, worldModel);
        System.out.println(actions);
        moveBuffer = actions;
        if (moveBuffer.isEmpty()) {
            return false;
        }
        return true;
    }

    public char get_action( char view[][] ) {
        worldModel.updateWorldModel(view, relativeCoordX, relativeCoordY, relativeAgentOrientation);

        if (currentStage == Stage.EXPLORE) {
            if (moveBuffer.isEmpty() || priority > 0) {
                if (hasGold) {
                    System.out.println("GO HOME");
                    if (goHome()) {
                        priority = 0;
                    }
                }
            }
            if (moveBuffer.isEmpty() || priority > 1) {
                System.out.println("COLLECT");
                if (collect()) {
                    priority = 1;
                }
            }
            if (moveBuffer.isEmpty()) {
                System.out.println("EXPLORE");
                if (explore(true, false)) {
                    priority = 2;
                }
            }
        }
        if (moveBuffer.isEmpty()) {
            priority = 9;
            currentStage = Stage.PLANNED;
        }
        if (currentStage == Stage.PLANNED) {
            if (moveBuffer.isEmpty()) {
                System.out.println("SOLUTION EXPLORE");
                solutionExplore();
            }
        }

        if (moveBuffer.isEmpty())  {
            priority = 9;
            currentStage = Stage.UNSAFE;
        }

        if (currentStage == Stage.UNSAFE) {
            if (moveBuffer.isEmpty() && !onRaft) {
                System.out.println("GO TO WATER");
                goToWater();
            }
            if (moveBuffer.isEmpty() && onRaft) {
                System.out.println("WATER EXPLORE");
                if (explore(false, true)) {
                    priority = 0;
                }
            }
        }

        if (moveBuffer.isEmpty()) {
            priority = 9;
            currentStage = Stage.EXPLORE;
            moveBuffer.add('0');
        }

        System.out.println("MOVE GENERATION ITERATION OVER");

        char ch = moveBuffer.remove(0);

        switch( ch ) { // if character is a valid action, return it
            case 'F':
            case 'f':
                char objectInFront = worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation);
                switch (objectInFront) {
                    case '$':
                        hasGold = true;
                        break;
                    case 'k':
                        hasKey = true;
                        break;
                    case 'd':
                        dynamiteCount++;
                        break;
                    case 'a':
                        hasAxe = true;
                        break;
                    case '~':
                        onRaft = true;
                        hasRaft = false;
                        break;
                }
                if (objectInFront != '~' && objectInFront != '.') {
                    onRaft = false;
                }
                switch (relativeAgentOrientation) {
                    case 'N':
                        relativeCoordY--;
                        break;
                    case 'W':
                        relativeCoordX--;
                        break;
                    case 'S':
                        relativeCoordY++;
                        break;
                    case 'E':
                        relativeCoordX++;
                        break;
                }
                return ch;
            case 'L':
            case 'l':
                switch (relativeAgentOrientation) {
                    case 'N':
                        relativeAgentOrientation = 'W';
                        break;
                    case 'W':
                        relativeAgentOrientation = 'S';
                        break;
                    case 'S':
                        relativeAgentOrientation = 'E';
                        break;
                    case 'E':
                        relativeAgentOrientation = 'N';
                        break;
                }
                return ch;
            case 'R':
            case 'r':
                switch (relativeAgentOrientation) {
                    case 'N':
                        relativeAgentOrientation = 'E';
                        break;
                    case 'E':
                        relativeAgentOrientation = 'S';
                        break;
                    case 'S':
                        relativeAgentOrientation = 'W';
                        break;
                    case 'W':
                        relativeAgentOrientation = 'N';
                        break;
                }
                return ch;
            case 'U':
            case 'u':
                objectInFront = worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation);
                if (objectInFront == '-') {
                    Coordinate c = null;
                    switch (relativeAgentOrientation) {
                        case 'N':
                            c = new Coordinate(relativeCoordX, relativeCoordY - 1);
                            break;
                        case 'W':
                            c = new Coordinate(relativeCoordX - 1, relativeCoordY);
                            break;
                        case 'S':
                            c = new Coordinate(relativeCoordX, relativeCoordY + 1);
                            break;
                        case 'E':
                            c = new Coordinate(relativeCoordX + 1, relativeCoordY);
                            break;
                    }
                    blockadesRemoved.add(c);
                }
                return ch;
            case 'C':
            case 'c':
                objectInFront = worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation);
                if (objectInFront == 'T') {
                    hasRaft = true;
                    Coordinate c = null;
                    switch (relativeAgentOrientation) {
                        case 'N':
                            c = new Coordinate(relativeCoordX, relativeCoordY - 1);
                            break;
                        case 'W':
                            c = new Coordinate(relativeCoordX - 1, relativeCoordY);
                            break;
                        case 'S':
                            c = new Coordinate(relativeCoordX, relativeCoordY + 1);
                            break;
                        case 'E':
                            c = new Coordinate(relativeCoordX + 1, relativeCoordY);
                            break;
                    }
                    blockadesRemoved.add(c);
                }
                return ch;
            case 'B':
            case 'b':
                objectInFront = worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation);
                if (objectInFront == '*') {
                    dynamiteCount--;
                    Coordinate c = null;
                    switch (relativeAgentOrientation) {
                        case 'N':
                            c = new Coordinate(relativeCoordX, relativeCoordY - 1);
                            break;
                        case 'W':
                            c = new Coordinate(relativeCoordX - 1, relativeCoordY);
                            break;
                        case 'S':
                            c = new Coordinate(relativeCoordX, relativeCoordY + 1);
                            break;
                        case 'E':
                            c = new Coordinate(relativeCoordX + 1, relativeCoordY);
                            break;
                    }
                    blockadesRemoved.add(c);
                }
                return ch;
        }
        return 0;
    }

    private void print_view(char view[][])
    {
        int i,j;

        System.out.println("\n+-----+");
        for( i=0; i < 5; i++ ) {
            System.out.print("|");
            for( j=0; j < 5; j++ ) {
                if(( i == 2 )&&( j == 2 )) {
                    System.out.print('^');
                }
                else {
                    System.out.print( view[i][j] );
                }
            }
            System.out.println("|");
        }
        System.out.println("+-----+");
    }

    public static void main( String[] args )
    {
        InputStream in  = null;
        OutputStream out= null;
        Socket socket   = null;
        Agent  agent    = new Agent();
        char   view[][] = new char[5][5];
        char   action;
        int port;
        int ch;
        int i,j;

        if( args.length < 2 ) {
            System.out.println("Usage: java agent.Agent -p <port>\n");
            System.exit(-1);
        }

        port = Integer.parseInt( args[1] );

        try { // open socket to Game Engine
            socket = new Socket( "localhost", port );
            in  = socket.getInputStream();
            out = socket.getOutputStream();
        }
        catch( IOException e ) {
            System.out.println("Could not bind to port: "+port);
            System.exit(-1);
        }

        try { // scan 5-by-5 window around current location
            while( true ) {
                for( i=0; i < 5; i++ ) {
                    for( j=0; j < 5; j++ ) {
                        if( !(( i == 2 )&&( j == 2 ))) {
                            ch = in.read();
                            if( ch == -1 ) {
                                System.exit(-1);
                            }
                            view[i][j] = (char) ch;
                        }
                    }
                }
                //agent.print_view( view ); // COMMENT THIS OUT BEFORE SUBMISSION
                action = agent.get_action( view );
                out.write( action );
            }
        }
        catch( IOException e ) {
            System.out.println("Lost connection to port: "+ port );
            System.exit(-1);
        }
        finally {
            try {
                socket.close();
            }
            catch( IOException ignored) {}
        }
    }
}
