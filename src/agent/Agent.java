package agent;

import pathfinding.Coordinate;
import pathfinding.Explore;
import pathfinding.State;

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.util.*;

public class Agent {

    private WorldModel worldModel;
    private int relativeCoordX, relativeCoordY;
    private char relativeAgentOrientation;

    boolean hasGold, hasKey, hasAxe, hasRaft;
    int dynamiteCount;

    private ArrayList<Character> moveBuffer;

    private Stage currentStage;

    private HashSet<Coordinate> blockadesRemoved;

    private enum Stage {
        EXPLORE, COLLECT, RETURN
    }

    public Agent() {
        worldModel = new WorldModel();
        relativeCoordX = 0;
        relativeCoordY = 0;
        relativeAgentOrientation = 'S'; // we don't know which way we're facing, so just call the initial direction south
        moveBuffer = new ArrayList<>();

        currentStage = Stage.EXPLORE;
        blockadesRemoved = new HashSet<>(); //  doors opened, walls blown up, trees cut down
    }

    private boolean explore() {
        State unexploredTile = Explore.findUnexploredTile(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved), worldModel, relativeCoordX, relativeCoordY, hasKey);

        if (unexploredTile == null) {
            currentStage = Stage.COLLECT;
            return false;
        }

        ArrayList<State> path = Explore.findPath(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved), new Coordinate(unexploredTile.getRelativeCoordX(), unexploredTile.getRelativeCoordY()),
                worldModel,
                new ArrayList<>(),
                hasKey);

        System.out.println(path);

        ArrayList<Character> actions = Explore.generateActions(path);
        System.out.println(actions);
        moveBuffer = actions;
        if (moveBuffer.isEmpty()) {
            currentStage = Stage.COLLECT;
            return false;
        }
        return true;
    }

    private boolean collect() {
        ArrayList<Character> objects = new ArrayList<>(Arrays.asList('$', 'k', 'd', 'a'));  // the priority order for objects
        ArrayList<Character> allowedItemPickups = new ArrayList<>();    // list of items the agent is allowed to pick up
        for (Character objectType : objects) {                  // for each object type
            allowedItemPickups.add(objectType);
            System.out.println(objectType);
            ArrayList<Coordinate> tiles = worldModel.getObjectsTiles(objectType);     // find all tiles containing given object
            System.out.println(tiles);
            if (!tiles.isEmpty()) {                             // if a tile containing given object type was found
                for (Coordinate coordinate : tiles) {                     // check each tile found to see if there is a path there from current position
                    System.out.println(coordinate);
                    if (coordinate.x == relativeCoordX && coordinate.y == relativeCoordY) {
                        continue;   // the world model doesn't get updated until the agent actually moves, so when the agent picks up an item, the world model thinks the item is still there
                    }               // this makes sure the agent actually moves after picking up an item to allow the world model to update
                    ArrayList<State> path = Explore.findPath(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, blockadesRemoved),
                            coordinate,
                            worldModel,
                            allowedItemPickups,
                            hasKey);
                    if (path.size() != 0) {     // if the returned path is not empty, a path was found
                        ArrayList<Character> actions = Explore.generateActions(path);
                        //System.out.println(actions);
                        moveBuffer = actions;
                        System.out.println("MOVEBUFFER " + moveBuffer);
                        System.out.println("Agent: " + relativeCoordX + " " + relativeCoordY + " " + relativeAgentOrientation);
                        System.out.println(objectType + " " + coordinate + " " + path);
                        currentStage = Stage.EXPLORE;
                        return true;
                    }
                }
            }
        }
        currentStage = Stage.EXPLORE;
        return false;
    }

    public char get_action( char view[][] ) {
        worldModel.updateWorldModel(view, relativeCoordX, relativeCoordY, relativeAgentOrientation);


        if (moveBuffer.isEmpty()) {
            while (true) {
                System.out.println(currentStage);
                boolean updatedMoveBuffer = false;
                switch (currentStage) {
                    case EXPLORE:
                        updatedMoveBuffer = explore();
                        break;
                    case COLLECT:
                        updatedMoveBuffer = collect();
                        break;
                    case RETURN:
                        break;
                }
                if (updatedMoveBuffer) {
                    System.out.println("Updated buffer");
                    break;
                }
                System.out.println("Did not update buffer");
            }
        }

        //System.out.println("Current Position: " + relativeCoordX + " " + relativeCoordY + " " + relativeAgentOrientation);

        char ch = moveBuffer.remove(0);

        switch( ch ) { // if character is a valid action, return it
            case 'F':
            case 'f':
                //if (!worldModel.agentBlocked(relativeCoordX, relativeCoordY, relativeAgentOrientation, getAllowedItemPickups(), blockadesRemoved)) {
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
                //}
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
            case 'C': case 'B':
            case 'c': case 'b':
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
