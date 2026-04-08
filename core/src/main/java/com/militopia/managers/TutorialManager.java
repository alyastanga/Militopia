package com.militopia.managers;

import com.militopia.utils.GameLogger;

public class TutorialManager {

    private static TutorialManager instance;
    private int currentStep = 0;
    private boolean active = false;

    public enum Step {
        WELCOME("Welcome to Militopia! Let's learn the basics."),
        CAMERA("Drag the mouse to pan the camera, and use the scroll wheel to zoom."),
        SELECT_UNIT("Click on one of your units to select it."),
        MOVE_UNIT("Move the Recruit to the highlighted tile."),
        SELECT_BASE("Select your Base to see city options."),
        SUMMON_UNIT("Summon a Recruit from the Base menu."),
        END_TURN_REFRESH("Your Recruit needs to rest before it can move. Click 'End Turn' to start the next turn."),
        MOVE_TO_TOWN("Select your new Recruit and move it onto the Town tile — it's directly above your Base."),
        END_TURN_CAPTURE("Good! Now click 'End Turn' — your Recruit will hold the Town overnight."),
        CAPTURE_TOWN("Your Recruit is on the Town! Click the 'Capture' button that appeared to claim it."),
        BUILD_STRUCTURE("Select a tile within your territory and build a Munition Factory."),
        MOVE_TO_TREE("Move a unit onto the Tree tile to the right of your Base."),
        END_TURN_CUT("Your unit needs rest. Click 'End Turn' to continue."),
        CUT_TREE("Click 'Cut Tree' in the Info Panel to harvest the tree."),
        END_TURN_BEFORE_DEER("Your unit is tired. Click 'End Turn' to rest before moving to the Deer."),
        MOVE_TO_DEER("Move a unit onto the Deer tile (just right of the Tree)."),
        END_TURN_HUNT("Your unit needs rest. Click 'End Turn' to continue."),
        HUNT_ANIMAL("Click 'Hunt' in the Slide Menu to hunt the Deer."),
        END_TURN_BEFORE_ATTACK("Your unit is tired. Click 'End Turn' to rest before attacking."),
        MOVE_TO_ATTACK("Move your Recruit next to the enemy unit to the right."),
        END_TURN_ATTACK("Your unit needs rest. Click 'End Turn' to continue."),
        ATTACK_ENEMY("Click on the enemy unit to attack it."),
        END_TURN_BEFORE_RUINS("Your unit is tired. Click 'End Turn' to rest before moving to the Ruins."),
        MOVE_TO_RUINS("Move a unit onto the Ruins tile."),
        END_TURN_SCAVENGE("Your unit needs rest. Click 'End Turn' to continue."),
        SCAVENGE_RUINS("Click 'Scavenge' in the Slide Menu."),
        CHECK_STATS("Click on the enemy unit or base to see their stats."),
        DISBAND_UNIT("Select a unit and click 'Disband' in the Info Panel."),
        DEMOLISH_STRUCT("Select a structure and click 'Demolish' in the Info Panel."),
        END_TURN("Click 'End Turn' to finish your turn."),
        SAVE_EXIT("Open Settings and click 'Save & Exit' to complete the tutorial."),
        COMPLETED("Tutorial Completed! You're ready to play.");

        public final String instruction;
        Step(String instruction) {
            this.instruction = instruction;
        }
    }

    private TutorialManager() {}

    public static TutorialManager getInstance() {
        if (instance == null) {
            instance = new TutorialManager();
        }
        return instance;
    }

    public void startTutorial() {
        active = true;
        currentStep = 0;
        GameLogger.log(GameLogger.UI, "Tutorial Started");
    }

    public void endTutorial() {
        active = false;
        GameLogger.log(GameLogger.UI, "Tutorial Ended");
    }

    public boolean isActive() {
        return active;
    }

    public Step getCurrentStep() {
        return Step.values()[currentStep];
    }

    public void nextStep() {
        if (currentStep < Step.values().length - 1) {
            currentStep++;
            GameLogger.log(GameLogger.UI, "Tutorial Advanced to: " + getCurrentStep().name());
        }
    }

    public boolean isActionAllowed(String action) {
        if (!active) return true;
        
        // Basic logic: only allow actions relevant to the current step
        // This can be expanded as needed
        switch (getCurrentStep()) {
            case SELECT_UNIT: return action.equals("SELECT_UNIT");
            case MOVE_UNIT: return action.equals("MOVE_UNIT");
            case SELECT_BASE: return action.equals("SELECT_BASE");
            case SUMMON_UNIT: return action.equals("SUMMON_UNIT");
            // ... add more guards ...
            default: return true;
        }
    }
}
