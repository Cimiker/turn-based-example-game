package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.turn_based_example_game.Main;

public class TutorialScreen extends Stage {

    Image menuBoxImage;

    public TutorialScreen(Main game) {
        super(new ScreenViewport());

        // Set this screen to handle user input
        Gdx.input.setInputProcessor(this);

        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        // Set background
        Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));
        Image backgroundImage = new Image(backgroundTexture); // Main background
        backgroundImage.setFillParent(true);
        backgroundImage.setZIndex(0); // Behind all UI
        addActor(backgroundImage);

        /*
        Texture menuBoxTexture = new Texture(Gdx.files.internal("menuBoxBrown.png"));
        menuBoxImage = new Image(menuBoxTexture);

        // Layout background and menu box
        float margin = 50f;

        // size the box to fill the virtual world minus margin on each side
        menuBoxImage.setSize(
            getViewport().getWorldWidth()  - 2 * margin,
            getViewport().getWorldHeight() - 2 * margin
        );
        // position it margin units in from the bottom-left
        menuBoxImage.setPosition(margin, margin);
        menuBoxImage.setColor(1f, 1f, 1f, 0.95f);
        menuBoxImage.setZIndex(1);
        addActor(menuBoxImage);
         */

        // Create the base table
        Table baseTable = new Table();
        baseTable.setFillParent(true);
        addActor(baseTable);

        // Create bottom UI table
        Table bottomTable = new Table();
        bottomTable.bottom().setFillParent(true);
        addActor(bottomTable);

        // Create UI elements
        Label titleLabel = new Label("Tutorial", skin);
        Label descriptionLabel1 = new Label("{Description1}", skin);
        descriptionLabel1.setAlignment(Align.center);
        descriptionLabel1.getStyle().font.getData().setLineHeight(30f);
        TextButton backButton = new TextButton("Back", skin);

        String tutorialText1 = """
            Welcome to DUO, the legally distinct version of UNO, now with cheating (possibly).
            The objective is to get rid of all of your cards by any means possible.
            Players take turns placing cards in a stack. The card has to have the same color or the same number.\s
            There are also special cards that reverse the flow of the game, make the next person pick up cards or change the color of the cards.\s
            Special cards can be stacked to avoid their effects.
            When at any point you have two cards in your hand you must call DUO. If other players manage to do so before you, you must face the consequences.\s
            Meaning you must pick up some cards. Shock, I know.

            Credits:
            Card art by JaooPhez from Itch.io
            Background image by Starline from Freepik
            """;

        // Add UI elements to their tables
        descriptionLabel1.setText(tutorialText1);

        baseTable.add(titleLabel).expandX().padTop(-100).row();
        baseTable.add(descriptionLabel1).expandX().maxWidth(Gdx.graphics.getWidth() * 0.8f).padTop(10).row();
        bottomTable.add(backButton).width(200).padBottom(10).padRight(80).minHeight(30).expandX().bottom().right().row();

        backButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                Gdx.app.postRunnable(() -> game.switchScreen(new MainMenuScreen(game)));
            }
        });
    }

    public void resize(int width, int height) {
        // 1) update the viewport & camera as before
        getViewport().update(width, height, true);

        // 2) recompute your menuBoxImage in world units
        float margin = 50f;  // same 50 used before
        float worldW = getViewport().getWorldWidth();
        float worldH = getViewport().getWorldHeight();

        menuBoxImage.setSize(worldW - 2 * margin, worldH - 2 * margin);
        menuBoxImage.setPosition(margin, margin);
    }
}
