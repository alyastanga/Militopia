package com.militopia.screen;

import com.militopia.MilitopiaGame;
import com.militopia.data.GameState;
import com.militopia.data.UnitData;
import com.militopia.managers.TutorialManager;
import com.militopia.map.MapGenerator;

public class TutorialScreen extends GameScreen {

    public TutorialScreen(MilitopiaGame game) {
        super(game, createTutorialState());
        TutorialManager.getInstance().startTutorial();
    }

    private static GameState createTutorialState() {
        // Create a fixed 10x10 map for the tutorial
        GameState state = new GameState(12345, "Tutorial", 10, 10);
        state.p1Funding = 10;
        state.p1XP = 0;
        
        // Define a simple map layout
        state.mapObjects = new MapGenerator.ObjectType[10][10];
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                state.mapObjects[x][y] = MapGenerator.ObjectType.NONE;
            }
        }
        
        // Add starting unit for player 1
        state.units.add(new UnitData(2, 1, "Recruit", 1));
        // Add enemy unit for the ATTACK_ENEMY tutorial step
        state.units.add(new UnitData(5, 2, "Recruit", 2));

        // Add specific objects for tutorial steps
        state.mapObjects[1][1] = MapGenerator.ObjectType.BASE_P1;
        state.mapObjects[1][2] = MapGenerator.ObjectType.TOWN; // To capture — adjacent to base so it's visible
        state.mapObjects[3][1] = MapGenerator.ObjectType.TREE; // To cut
        state.mapObjects[4][1] = MapGenerator.ObjectType.DEER; // To hunt
        state.mapObjects[3][3] = MapGenerator.ObjectType.RUINS; // To scavenge
        state.mapObjects[8][8] = MapGenerator.ObjectType.BASE_P2; // Enemy base
        
        return state;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        // Step advancement logic can be handled here or in InputController hooks
    }

    @Override
    public void hide() {
        super.hide();
        TutorialManager.getInstance().endTutorial();
    }
}
