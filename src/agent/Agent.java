package agent;

import pathfinding.Coordinate;
import pathfinding.Explore;
import pathfinding.State;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class Agent {

    /**
     * The world model of the agent. Keeps track of the world.
     */
    private WorldModel worldModel;

    /**
     * The relative coordinates of the agent.
     */
    private int relativeCoordX, relativeCoordY;

    /**
     * The relative orientation of the agent.
     */
    private char relativeAgentOrientation;

    /**
     * Agent inventory.
     */
    private boolean hasGold, hasKey, hasAxe, hasRaft, onRaft;
    private int dynamiteCount;

    /**
     * If the agent gets to the end of the move generation cycle with an empty move buffer, then this variable is set to true.
     * If the same thing happens on the next iteration, the agent will switch to the lumberjack stage.
     */
    private boolean noop;

    /**
     * If the agent has gone through the WATER stage yet.
     */
    private boolean hasExploredWater;

    /**
     * If the agent has gone through the BOMBERMAN stage yet.
     */
    private boolean hasBeenBomberman;

    /**
     * A list containing all the moves the agent currently has planned.
     */
    private ArrayList<Character> moveBuffer;

    /**
     * Keeps track of which stage the agent is currently in.
     */
    private Stage currentStage;

    /**
     * Denotes what the agent should do.
     * In the SAFE stage, the agent will
     *      1) try to go home if it has the gold
     *      2) try to collect items without using any resources
     *      3) explore without using any resources
     * In the PLANNED stage, the agent will try to plan a path from its current position to the gold to home.
     * In the WATER stage, the agent will try to get a raft and explore the water.
     * In the LUMBERJACK stage, the agent will try to explore the map using as much wood as necessary.
     */
    public enum Stage {
        SAFE, PLANNED, WATER, LUMBERJACK, BOMBERMAN
    }

    /**
     * In the SAFE stage, the different actions can override each other (no point in going for the axe if you can get home with the gold).
     * This variable keeps track of which of the three actions is currently being done.
     */
    private int priority;

    /**
     * Constructor for the agent. Initializes values.
     */
    public Agent() {
        worldModel = new WorldModel();
        relativeCoordX = 0;
        relativeCoordY = 0;
        relativeAgentOrientation = 'N'; // we don't know which way we're facing (and it doesn't matter), so just arbitrarily choose the initial direction
        moveBuffer = new ArrayList<>();
        currentStage = Stage.SAFE;
    }

    /**
     * Tries to uncover unexplored tiles. Does this by first finding an unexplored tile, then uses A* to find a path there from the current position.
     *
     * @param stage which stage the agent is currently in
     * @return true if a path was found, false otherwise
     */
    private boolean explore(Stage stage) {
        Coordinate unexploredTile = Explore.findUnexploredTile(
                new Coordinate(relativeCoordX, relativeCoordY),
                worldModel,
                hasKey,
                stage);

        if (unexploredTile == null) {
            return false;
        }

        ArrayList<State> path = Explore.findPath(
                new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, new HashSet<>(), hasGold, hasKey, hasAxe, hasRaft, stage == Stage.WATER || (stage == Stage.LUMBERJACK && onRaft), dynamiteCount),
                new Coordinate(unexploredTile.x, unexploredTile.y),
                worldModel,
                stage,
                null);

        System.out.println(path);

        moveBuffer = Explore.generateActions(path, worldModel);
        return !moveBuffer.isEmpty();
    }

    /**
     * Tries to safely collect items. Does this by first finding all the items, then uses A* to find a path there from the current position.
     *
     * @return true if a path was found, false otherwise
     */
    private boolean collect() {
        ArrayList<Character> objects = new ArrayList<>(Arrays.asList('$', 'k', 'd', 'a'));  // the priority order for objects
        for (Character objectType : objects) {                  // for each object type
            ArrayList<Coordinate> tiles = worldModel.getObjectTiles(objectType);     // find all tiles containing given object
            if (!tiles.isEmpty()) {                             // if a tile containing given object type was found
                for (Coordinate coordinate : tiles) {                     // check each tile found to see if there is a path there from current position
                    if (coordinate.x == relativeCoordX && coordinate.y == relativeCoordY) {
                        continue;   // the world model doesn't get updated until the agent actually moves, so when the agent picks up an item, the world model thinks the item is still there
                    }               // this makes sure the agent actually moves after picking up an item to allow the world model to update
                    ArrayList<State> path = Explore.findPath(
                            new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, new HashSet<>(), hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount),
                            coordinate,
                            worldModel,
                            Stage.SAFE,
                            null);
                    if (path.size() != 0) {     // if the returned path is not empty, a path was found
                        moveBuffer = Explore.generateActions(path, worldModel);
                        System.out.println("MOVE BUFFER " + moveBuffer);
                        System.out.println("Agent: " + relativeCoordX + " " + relativeCoordY + " " + relativeAgentOrientation);
                        System.out.println(objectType + " " + coordinate + " " + path);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Tries to find a path from the current position to the gold to home. Does this by first finding a path to the gold while being as
     * dynamite efficient as possible. First it checks the number of known dynamite in the world. Then it loops from minus that amount
     * to the number of dynamite the agent currently has. For example: The agent has 2 dynamite and there are 3 more in the world.
     * The method will loop from -3 to 2 (inclusive) and try to find a path with that many dynamite (trying to find a path with -3
     * dynamites means that the agent has to pick up 3 dynamite before being in a valid goal state).
     * After this, the method uses the goal state of the search for the gold as the start state for the search for a path home.
     */
    private void solutionExplore() {
        ArrayList<Coordinate> goldStates = worldModel.getObjectTiles('$');
        if (goldStates.size() == 0) {
            return;
        }

        for (Coordinate coordinate : goldStates) {
            ArrayList<Coordinate> leastDynamite = Explore.leastDynamitePath(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation), goldStates.get(0), worldModel);
            if (leastDynamite == null) {
                continue;
            }
            int leastDynamiteCount = leastDynamite.size();
            int dynamiteAvailable = worldModel.getAvailableDynamiteCount(relativeCoordX, relativeCoordY);  // finds the number of known dynamites in the world
            if (leastDynamiteCount > dynamiteCount + dynamiteAvailable) {
                return;
            }
            /*if (hasBeenBomberman) {     // if you have been bomberman, the map is likely very large. Don't bother searching for a solution where you have to pick up a dynamite along the way. Takes too long.
                dynamiteAvailable = 0;
            }*/
            for (int i = -dynamiteAvailable; i <= dynamiteCount; i++) {    // tries to find a path to the gold using as few dynamite as possible
                ArrayList<State> path = Explore.findPath(                                                        // starts with -dynamiteAvailable, meaning you have to pick up dynamites before going to the gold
                        new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, new HashSet<>(), new Coordinate(relativeCoordX, relativeCoordY), hasGold, hasKey, hasAxe, hasRaft, onRaft, i),
                        coordinate,
                        worldModel,
                        Stage.PLANNED,
                        hasBeenBomberman ? leastDynamite : null);
                if (path.size() != 0) {
                    State startState = path.get(path.size() - 1);
                    startState.setDynamiteCount(dynamiteCount - i); // however many dynamite are left is how many we will have to get from the gold to the start position
                    ArrayList<State> path2 = Explore.findPath(
                            startState,
                            new Coordinate(0, 0),
                            worldModel,
                            Stage.PLANNED,
                            null);
                    if (path2.size() != 0) {
                        moveBuffer = Explore.generateActions(path2, worldModel);
                        System.out.println("MOVE BUFFER " + moveBuffer);
                        System.out.println("Agent: " + relativeCoordX + " " + relativeCoordY + " " + relativeAgentOrientation);
                        System.out.println("$$$" + " " + coordinate + " " + path2);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Tries to get a raft by finding a tree and cutting it.
     *
     * @return true if a path was found, false otherwise
     */
    private boolean getRaft() {
        ArrayList<Coordinate> treeStates = worldModel.getObjectsTilesSortedByDistance('T', new State(relativeCoordX, relativeCoordY, relativeAgentOrientation));
        if (treeStates.size() == 0) {
            return false;
        }
        for (Coordinate coordinate : treeStates) {
            ArrayList<State> path = Explore.findPath(
                    new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, new HashSet<>(), hasGold, hasKey, hasAxe, false, false, -Integer.MAX_VALUE),
                    coordinate,
                    worldModel,
                    Stage.PLANNED,
                    null);
            if (path.size() != 0) {     // if the returned path is not empty, a path was found
                moveBuffer = Explore.generateActions(path, worldModel);
                System.out.println("MOVE BUFFER " + moveBuffer);
                System.out.println("Agent: " + relativeCoordX + " " + relativeCoordY + " " + relativeAgentOrientation);
                System.out.println("get raft" + " " + coordinate + " " + path);
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to find a path to the water.
     */
    private void goToWater() {
        ArrayList<State> path = Explore.findClosestTileOfType('~', new State(relativeCoordX, relativeCoordY, relativeAgentOrientation), worldModel);
        if (path.size() != 0) {     // if the returned path is not empty, a path was found
            moveBuffer = Explore.generateActions(path, worldModel);
            System.out.println("MOVE BUFFER " + moveBuffer);
            System.out.println("Agent: " + relativeCoordX + " " + relativeCoordY + " " + relativeAgentOrientation);
            System.out.println("go to water" + " " + " " + path);
        }
    }

    /**
     * Tries to find a path to a dynamite using as few dynamite as possible. The hope is to have a net gain of dynamite (blow up one wall to get two dynamite)
     * and thus simplifying the search space for the gold/goal
     *
     * @return true if a path was found, false otherwise
     */
    private boolean bomberman() {
        ArrayList<Coordinate> dynamites = worldModel.getAllDynamites(new HashSet<>());
        for (Coordinate dynamite : dynamites) {
            if (dynamite.x == relativeCoordX && dynamite.y == relativeCoordY) {
                continue;
            }
            ArrayList<Coordinate> dynamiteCoordinates = Explore.leastDynamitePath(new State(relativeCoordX, relativeCoordY, relativeAgentOrientation), dynamite, worldModel);
            if (dynamiteCoordinates == null) {
                continue;
            }
            if (dynamiteCoordinates.size() <= dynamiteCount) {
                ArrayList<State> path = Explore.findPath(
                        new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, new HashSet<>(), hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount),
                        dynamite,
                        worldModel,
                        Stage.BOMBERMAN,
                        dynamiteCoordinates);

                System.out.println(path);

                if (path.size() != 0) {
                    ArrayList<Character> actions = Explore.generateActions(path, worldModel);
                    System.out.println(actions);
                    moveBuffer = actions;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tries to find a path home.
     * @return true if a path was found, false otherwise
     */
    private boolean goHome() {
        ArrayList<State> path = Explore.findPath(
                new State(relativeCoordX, relativeCoordY, relativeAgentOrientation, new HashSet<>(), hasGold, hasKey, hasAxe, hasRaft, onRaft, dynamiteCount),
                new Coordinate(0, 0),
                worldModel,
                Stage.PLANNED,
                null);

        System.out.println(path);

        ArrayList<Character> actions = Explore.generateActions(path, worldModel);
        System.out.println(actions);
        moveBuffer = actions;
        return !moveBuffer.isEmpty();
    }

    /**
     * Updates the move buffer if necessary, then performs an action and updates the agent's state depending on the action.
     *
     * How the agent chooses what to do next:
     *  Start in SAFE stage.
     *  If move buffer is not empty:
     *      Choose next move from move buffer
     *  If move buffer is empty, generate new moves: (when a move is found, put it in the move buffer and stop looking for moves)
     *      If currently in SAFE stage:
     *          If I have the gold and I can get home: Go home.
     *          If I can safely collect an item: Collect the item.
     *          If I can find somewhere to safely explore: Explore.
     *          Set currentStage to the PLANNED stage.
     *      If currently in the PLANNED stage:
     *          If I can find a path from my current position to the gold to home: Do that.
     *          Set currentStage to the WATER stage.
     *      If I haven't been to the WATER stage yet and currently in the WATER stage:
     *          If I can get a raft: Get the raft.
     *          If I have a raft and can get to water: Go to the water.
     *          If I am on the water: Explore, don't leave the water.
     *          Set currentStage to the SAFE stage.
     *      If noop variable is true, go to the LUMBERJACK stage.
     *      If currently in the LUMBERJACK stage:
     *          If I can find somewhere to explore using anything but dynamite: Explore.
     *          Set currentStage to the BOMBERMAN stage
     *      If currently in the BOMBERMAN stage:
     *          If I can reach a dynamite, get the dynamite.
     *          Set currentStage to the SAFE stage
     *      Add no-op move to move buffer
     *      Set noop variable to true
     *
     * @param view what the agent perceived after the last action
     * @return the next action the agent is to perform
     */
    public char get_action( char view[][] ) {
        worldModel.updateWorldModel(view, relativeCoordX, relativeCoordY, relativeAgentOrientation);

        if (currentStage == Stage.SAFE) {
            if (moveBuffer.isEmpty() || priority > 0) {
                if (hasGold) {
                    System.out.println("GO HOME");
                    if (goHome()) {
                        priority = 0;
                    }
                }
            }
            if (!onRaft && (moveBuffer.isEmpty() || priority > 1)) {
                System.out.println("COLLECT");
                if (collect()) {
                    priority = 1;
                }
            }
            if (moveBuffer.isEmpty()) {
                System.out.println("EXPLORE");
                if (explore(Stage.SAFE)) {
                    priority = 2;
                }
            }
            if (moveBuffer.isEmpty()) {
                priority = 9;
                currentStage = Stage.PLANNED;
            }
        }

        if (currentStage == Stage.PLANNED) {
            if (moveBuffer.isEmpty()) {
                System.out.println("SOLUTION EXPLORE");
                    solutionExplore();
            }
            if (moveBuffer.isEmpty())  {
                currentStage = Stage.WATER;
            }
        }

        if (!hasExploredWater && currentStage == Stage.WATER) {
            if (moveBuffer.isEmpty() && !hasRaft && !onRaft) {
                System.out.println("GET RAFT");
                if (getRaft()) {    // if we cut down a tree to get a raft, try to explore new areas before using the raft
                    currentStage = Stage.SAFE;
                }
            }
            if (moveBuffer.isEmpty() && hasRaft && !onRaft) {
                System.out.println("GO TO WATER");
                goToWater();
            }
            if (moveBuffer.isEmpty() && onRaft) {
                System.out.println("WATER EXPLORE");
                explore(Stage.WATER);
            }
            if (moveBuffer.isEmpty()) {
                hasExploredWater = true;
                currentStage = Stage.SAFE;
            }
        }

        if (moveBuffer.isEmpty() && noop) {
            priority = 9;
            currentStage = Stage.LUMBERJACK;
        }

        if (currentStage == Stage.LUMBERJACK) {
            if (moveBuffer.isEmpty()) {
                System.out.println("LUMBERJACK EXPLORE");
                explore(Stage.LUMBERJACK);
            }
            if (moveBuffer.isEmpty())  {
                currentStage = Stage.BOMBERMAN;
            }
        }

        if (currentStage == Stage.BOMBERMAN) {
            hasBeenBomberman = true;
            if (moveBuffer.isEmpty()) {
                System.out.println("BOMBERMAN");
                if (bomberman()) {
                    currentStage = Stage.SAFE;
                }
            }

            if (moveBuffer.isEmpty())  {
                currentStage = Stage.SAFE;
            }
        }

        if (moveBuffer.isEmpty()) {
            moveBuffer.add('0');
            noop = true;
        } else {
            noop = false;
        }

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
                    if (worldModel.getObjectAtCoordinate(relativeCoordX, relativeCoordY) == '~') {
                        hasRaft = false;    // if you cut down a tree right before stepping out of water, remove the tree from inventory
                    }
                }
                relativeCoordX += State.xOffset.get(relativeAgentOrientation);
                relativeCoordY += State.yOffset.get(relativeAgentOrientation);
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
                return ch;
            case 'C':
            case 'c':
                objectInFront = worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation);
                if (objectInFront == 'T') {
                    hasRaft = true;
                }
                return ch;
            case 'B':
            case 'b':
                objectInFront = worldModel.getObjectInFront(relativeCoordX, relativeCoordY, relativeAgentOrientation);
                if (objectInFront == '*') {
                    dynamiteCount--;
                }
                return ch;
        }
        return 0;
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
