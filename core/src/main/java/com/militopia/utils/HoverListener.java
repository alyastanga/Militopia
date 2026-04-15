package com.militopia.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;

public class HoverListener extends ClickListener {
    
    private static final float SCALE_UP = 1.1f;
    private static final float SCALE_DOWN = 1.0f;
    private static final float DURATION = 0.1f;
    
    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
        super.enter(event, x, y, pointer, fromActor);
        
        if (pointer == -1) {
            // Skip if the mouse is moving between child actors of this button
            // (e.g. TextButton's inner Label), not entering from outside
            Actor actor = event.getListenerActor();
            if (fromActor != null && actor.isAscendantOf(fromActor)) {
                return;
            }
            AudioManager.getInstance().playSFX(SFXKeys.UI_HOVER, 0.3f);
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);

            // Validate layout to ensure width/height are correct before setting origin
            if (actor instanceof com.badlogic.gdx.scenes.scene2d.utils.Layout) {
                ((com.badlogic.gdx.scenes.scene2d.utils.Layout) actor).validate();
            }
            
            // Set origin to center so it scales nicely
            actor.setOrigin(actor.getWidth() / 2f, actor.getHeight() / 2f);
            
            actor.clearActions(); 
            actor.addAction(Actions.scaleTo(SCALE_UP, SCALE_UP, DURATION));
        }
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
        super.exit(event, x, y, pointer, toActor);
        
        if (pointer == -1) {
            Actor actor = event.getListenerActor();
            if (toActor != null && actor.isAscendantOf(toActor)) {
                return;
            }
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            
            actor.setOrigin(actor.getWidth() / 2f, actor.getHeight() / 2f);
            
            actor.clearActions();
            actor.addAction(Actions.scaleTo(SCALE_DOWN, SCALE_DOWN, DURATION));
        }
    }
}