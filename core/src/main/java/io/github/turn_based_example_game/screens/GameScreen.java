package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class GameScreen extends Stage {

    public void resize(int width, int height) {
        getViewport().update(width, height, true);
        float margin = 5f;  // same 5 used before
        float worldW = getViewport().getWorldWidth();
        float worldH = getViewport().getWorldHeight();

        // 1) recompute backButton position
        //camera.setToOrtho(false, width, height);
        //backButton.setPosition(worldW - backButton.getWidth() - 10, worldH - backButton.getHeight() - 10);

        // 2) recompute your menuBoxImage in world units
        //uiBoxImage.setSize(worldW * 0.1f,worldH * 0.1f);
        // position it margin units in from the bottom-left
        //uiBoxImage.setPosition(worldW / 2 - uiBoxImage.getWidth() / 2, worldH - uiBoxImage.getHeight() - margin);
    }

    @Override
    public void dispose() {
        //shapeRenderer.dispose();
        //game.dispose();
        super.dispose();
    }
}
