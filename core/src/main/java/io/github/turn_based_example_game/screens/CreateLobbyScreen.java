package io.github.turn_based_example_game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.turn_based_example_game.Main;
import io.github.turn_based_example_game.Network;
import io.github.turn_based_example_game.NetworkManager;
import io.github.turn_based_example_game.SoundController;

public class CreateLobbyScreen extends Stage {

    public CreateLobbyScreen(Main game, SoundController soundController) {
        super(new ScreenViewport());
        Gdx.input.setInputProcessor(this);

        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
        Texture backgroundTexture = new Texture(Gdx.files.internal("menuBackground.png"));

        Image backgroundImage = new Image(backgroundTexture);
        backgroundImage.setFillParent(true);
        backgroundImage.setZIndex(0);
        addActor(backgroundImage);

        Label screenLabel = new Label("Lobby options", skin);
        TextButton.TextButtonStyle optionButtonStyle = createOptionButtonStyle(skin);
        TextButton twoPlayers = new TextButton("2", optionButtonStyle);
        TextButton threePlayers = new TextButton("3", optionButtonStyle);
        TextButton fourPlayers = new TextButton("4", optionButtonStyle);
        Label maxPlayersLabel = new Label("Maximum Players", skin);
        ButtonGroup<TextButton> maxPlayersButtonGroup = new ButtonGroup<>(twoPlayers, threePlayers, fourPlayers);
        TextButton privateButton = new TextButton("Private", optionButtonStyle);
        TextButton publicButton = new TextButton("Public", optionButtonStyle);
        Label lobbyModeLabel = new Label("Lobby Mode", skin);
        ButtonGroup<TextButton> lobbyModeButtonGroup = new ButtonGroup<>(privateButton, publicButton);
        TextButton yesBotsButton = new TextButton("Yes", optionButtonStyle);
        TextButton noBotsButton = new TextButton("No", optionButtonStyle);
        Label fillWithBotsLabel = new Label("Fill empty spots with Bots", skin);
        ButtonGroup<TextButton> botsButtonGroup = new ButtonGroup<>(yesBotsButton, noBotsButton);
        TextButton createLobbyButton = new TextButton("Create Lobby", skin);
        TextButton backButton = new TextButton("Back", skin);

        maxPlayersButtonGroup.setChecked("4");
        lobbyModeButtonGroup.setChecked("Private");
        botsButtonGroup.setChecked("Yes");

        Table table = new Table();
        table.setFillParent(true);

        Table maxPlayersTable = new Table();

        table.add(screenLabel).padBottom(10).colspan(2).center().row();
        table.add(maxPlayersLabel).width(200).padBottom(10).row();
        maxPlayersTable.add(twoPlayers).width(50).padBottom(10).expandX();
        maxPlayersTable.add(threePlayers).width(50).padBottom(10).padLeft(100).padRight(100).expandX();
        maxPlayersTable.add(fourPlayers).width(50).padBottom(10).expandX();
        table.add(maxPlayersTable).colspan(2).row();
        table.add(lobbyModeLabel).width(200).padBottom(10).row();
        table.add(privateButton).width(200).padBottom(10);
        table.add(publicButton).width(200).padBottom(10).row();
        table.add(fillWithBotsLabel).width(200).padBottom(10).row();
        table.add(yesBotsButton).width(200).padBottom(10);
        table.add(noBotsButton).width(200).padBottom(10).row();
        table.add(createLobbyButton).width(200).padBottom(10).colspan(2).row();
        table.add(backButton).width(150).padBottom(10).colspan(2).row();

        addActor(table);

        createLobbyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Network.LobbySettings settings = new Network.LobbySettings();
                settings.maxPlayers = Integer.parseInt(maxPlayersButtonGroup.getChecked().getText().toString());
                settings.lobbyMode = lobbyModeButtonGroup.getChecked().getText().toString();
                settings.fillWithBots = "Yes".contentEquals(botsButtonGroup.getChecked().getText());

                NetworkManager.createLobby(settings);
                Gdx.app.postRunnable(() -> game.switchScreen(new GameLobbyScreen(game, soundController)));
            }
        });

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> game.switchScreen(new JoinGameLobbyScreen(game, soundController)));
            }
        });
    }

    private TextButton.TextButtonStyle createOptionButtonStyle(Skin skin) {
        TextButton.TextButtonStyle baseStyle = skin.get(TextButton.TextButtonStyle.class);
        TextButton.TextButtonStyle optionStyle = new TextButton.TextButtonStyle(baseStyle);

        Drawable defaultButton = skin.newDrawable("default-round", Color.valueOf("8e8e8e"));
        Drawable hoverButton = skin.newDrawable("default-round", Color.valueOf("a5a5a5"));
        Drawable pressedButton = skin.newDrawable("default-round-down", Color.valueOf("676767"));
        Drawable checkedButton = skin.newDrawable("default-round-down", Color.valueOf("4d4d4d"));

        optionStyle.up = defaultButton;
        optionStyle.over = hoverButton;
        optionStyle.down = pressedButton;
        optionStyle.checked = checkedButton;
        optionStyle.checkedOver = checkedButton;
        optionStyle.checkedDown = checkedButton;
        optionStyle.fontColor = Color.WHITE;
        optionStyle.overFontColor = Color.WHITE;
        optionStyle.downFontColor = Color.WHITE;
        optionStyle.checkedFontColor = Color.WHITE;

        return optionStyle;
    }
}
