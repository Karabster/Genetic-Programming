package pacman.controllers;

import Genetic.Executable;
import Genetic.LispExecutor;
import pacman.game.Constants;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Collections;

public class SimpleController extends Controller implements Executable {



        private Constants.MOVE lastMove;
        private Constants.MOVE nextMove;
        private Game currentGame;


        @Override
        public Constants.MOVE getMove(Game game, long timeDue) {
            setCurrentGame(game);
            LispExecutor.executeBest("( 4ifle ( distToEdibleGhost moveFromPower distToPower moveToFood ) )");
            lastMove = nextMove;
            return nextMove;
        }

        public void setNextMove(Constants.MOVE nextMove) {
            this.nextMove = nextMove;
        }

        public void setCurrentGame(Game currentGame) {
            this.currentGame = currentGame;
        }

        public Constants.MOVE getNextMove() {
            return nextMove;
        }

        public Game getCurrentGame() {
            return currentGame;
        }



        //2ifblue
        @Override
        public int functional1(ArrayList<String> node) {

            if(currentGame.getClosestNonEdibleGhost(currentGame.getPacmanCurrentNodeIndex()) == null){
                return 1;
            }else{
                return 2;
            }
        }

        //
        @Override
        public int functional2(ArrayList<String> node) {
            if(LispExecutor.evaluate(1,2,node) == 0){
                return 3;
            }else{
                return 4;
            }
        }

        //2ifindanger
        @Override
        public int functional3(ArrayList<String> node) {
            if(currentGame.getDistanceToClosestNonEdibleGhost(currentGame.getPacmanCurrentNodeIndex()) < 15){
                return 1;
            }else{
                return 2;
            }
        }

        @Override
        public int functional4(ArrayList<String> node) {
            if(currentGame.getNumberOfActivePowerPills() == 0){
                return 1;
            }else{
                return 2;
            }
        }

        @Override
        public int functional5(ArrayList<String> node) {
            if(isToPowerPillSafe()){
                return 1;
            }else{
                return 2;
            }
        }

        private boolean isToPowerPillSafe(){
            boolean isSafe = true;
            int pathNode = currentGame.getPacmanCurrentNodeIndex();
            int pacmanNode = currentGame.getPacmanCurrentNodeIndex();
            while(pathNode != currentGame.getClosestPowerPill(pacmanNode)){
                pathNode = currentGame.getNeighbour(pathNode,currentGame.getDirectionTowardsClosestPowerPill(pathNode));
                if(pathNode == -1){
                    isSafe = false;
                    break;
                }
                if(currentGame.getDistanceToClosestNonEdibleGhost(pathNode) < 15){
                    isSafe = false;
                }
            }

            return isSafe;
        }

        @Override
        public int terminal1() {
            return currentGame.getDistToClosestPill(currentGame.getPacmanCurrentNodeIndex());
        }

        @Override
        public int terminal2() {
            return currentGame.getDistToClosestPowerPill(currentGame.getPacmanCurrentNodeIndex());
        }

        @Override
        public int terminal3() {
            int distanceToEdible = currentGame.getDistanceToClosestEdibleGhost(currentGame.getPacmanCurrentNodeIndex());
            int distanceToNonEdible = currentGame.getDistanceToClosestNonEdibleGhost(currentGame.getPacmanCurrentNodeIndex());
            if(distanceToEdible < distanceToNonEdible){
                return distanceToEdible;
            }else{
                return distanceToNonEdible;
            }
        }

        @Override
        public int terminal4() {
            ArrayList<Integer> ghosts = new ArrayList<>();
            int pacmanNode = currentGame.getPacmanCurrentNodeIndex();
            int ghost1distance = (int) currentGame.getDistance(pacmanNode,
                    currentGame.getGhostCurrentNodeIndex(Constants.GHOST.BLINKY), Constants.DM.PATH);
            int ghost2distance = (int) currentGame.getDistance(pacmanNode,
                    currentGame.getGhostCurrentNodeIndex(Constants.GHOST.SUE), Constants.DM.PATH);
            int ghost3distance = (int) currentGame.getDistance(pacmanNode,
                    currentGame.getGhostCurrentNodeIndex(Constants.GHOST.PINKY), Constants.DM.PATH);
            int ghost4distance = (int) currentGame.getDistance(pacmanNode,
                    currentGame.getGhostCurrentNodeIndex(Constants.GHOST.INKY), Constants.DM.PATH);
            ghosts.add(ghost1distance);
            ghosts.add(ghost2distance);
            ghosts.add(ghost3distance);
            ghosts.add(ghost4distance);
            Collections.sort(ghosts);
            return ghosts.get(1);
        }

        @Override
        public int terminal12() {
            ArrayList<Integer> ghosts = new ArrayList<>();
            int pacmanNode = currentGame.getPacmanCurrentNodeIndex();
            int ghost1distance = (int) currentGame.getDistance(pacmanNode,
                    currentGame.getGhostCurrentNodeIndex(Constants.GHOST.BLINKY), Constants.DM.PATH);
            int ghost2distance = (int) currentGame.getDistance(pacmanNode,
                    currentGame.getGhostCurrentNodeIndex(Constants.GHOST.SUE), Constants.DM.PATH);
            int ghost3distance = (int) currentGame.getDistance(pacmanNode,
                    currentGame.getGhostCurrentNodeIndex(Constants.GHOST.PINKY), Constants.DM.PATH);
            int ghost4distance = (int) currentGame.getDistance(pacmanNode,
                    currentGame.getGhostCurrentNodeIndex(Constants.GHOST.INKY), Constants.DM.PATH);
            ghosts.add(ghost1distance);
            ghosts.add(ghost2distance);
            ghosts.add(ghost3distance);
            ghosts.add(ghost4distance);
            Collections.sort(ghosts);
            return ghosts.get(2);
        }

        @Override
        public int terminal5() {
            return currentGame.getDistanceToClosestEdibleGhost(currentGame.getPacmanCurrentNodeIndex());
        }

        @Override
        public int terminal6() {
            return currentGame.getDistanceToClosestNonEdibleGhost(currentGame.getPacmanCurrentNodeIndex());
        }

        @Override
        public int terminal7() {
            nextMove = currentGame.getDirectionTowardsClosestPill(currentGame.getPacmanCurrentNodeIndex());
            return directionToInt(nextMove);
        }

        @Override
        public int terminal8() {
            nextMove = currentGame.getDirectionTowardsClosestPowerPill(currentGame.getPacmanCurrentNodeIndex());
            return directionToInt(nextMove);
        }

        @Override
        public int terminal9() {
            nextMove = currentGame.getDirectionTowardsClosestEdibleGhost(currentGame.getPacmanCurrentNodeIndex());
            return directionToInt(nextMove);
        }

        @Override
        public int terminal10() {
            nextMove = currentGame.getDirectionAwayFromClosestNonEdibleGhost(currentGame.getPacmanCurrentNodeIndex());
            return directionToInt(nextMove);
        }

        @Override
        public int terminal11() {
            nextMove = currentGame.getNextMoveAwayFromTargetUpgraded(currentGame.getPacmanCurrentNodeIndex(), currentGame.getClosestPowerPill(currentGame.getPacmanCurrentNodeIndex()),  Constants.DM.PATH);
            return directionToInt(nextMove);
        }

        @Override
        public int terminal13() {
            nextMove = currentGame.huir();
            return directionToInt(nextMove);
        }

        @Override
        public int terminal14() {
            return 0;
        }

        public int directionToInt(Constants.MOVE move){
            int directionNumber = 0;
            if(move == Constants.MOVE.RIGHT){
                directionNumber = 1;
            }else if( move == Constants.MOVE.DOWN){
                directionNumber = 2;
            } else if (move == Constants.MOVE.LEFT) {
                directionNumber = 3;
            }
            return directionNumber;
        }
    }


