package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.militopia.MilitopiaGame;
import com.militopia.net.NetworkManager;
import com.militopia.net.NetworkMessage;

/**
 * LAN chat panel — slides in from the right edge (same pattern as DevPanel).
 */
public class ChatPanel {

    private static final float PANEL_WIDTH    = 360f;
    private static final float SLIDE_DURATION = 0.18f;

    private final MilitopiaGame game;
    private final Stage stage;
    private final NetworkManager networkManager;
    private final String localPlayerName;
    private final com.militopia.data.GameState gameState;

    private Table card;
    private boolean visible = false;

    private Table messageList;
    private ScrollPane scrollPane;
    private TextField inputField;

    public ChatPanel(MilitopiaGame game, Stage stage, NetworkManager networkManager, String localPlayerName, com.militopia.data.GameState gameState) {
        this.game = game;
        this.stage = stage;
        this.networkManager = networkManager;
        this.localPlayerName = localPlayerName;
        this.gameState = gameState;
    }

    public void build() {
        float sw = stage.getWidth();
        float sh = stage.getHeight();

        card = new Table();
        card.setTouchable(Touchable.enabled);
        card.setBackground(makeSolidDrawable(0.07f, 0.07f, 0.07f, 0.96f));
        card.top();

        // Title strip
        Table titleStrip = new Table();
        titleStrip.setBackground(makeSolidDrawable(0.05f, 0.10f, 0.20f, 1f));
        Label title = new Label("CHAT", game.skin);
        title.setFontScale(0.75f);
        title.setColor(new Color(0.4f, 0.8f, 1f, 1f));
        title.setAlignment(Align.center);
        titleStrip.add(title).growX().pad(8);
        card.add(titleStrip).growX().row();

        // Message list inside scroll pane
        messageList = new Table();
        messageList.top().left();

        scrollPane = new ScrollPane(messageList, game.skin);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                stage.setScrollFocus(scrollPane);
            }
        });
        card.add(scrollPane).growX().growY().row();

        inputField = new TextField("", game.skin);
        inputField.setTextFieldListener(new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                if (c == '\r' || c == '\n') {
                    sendMessage();
                }
            }
        });
        TextButton sendBtn = new TextButton("Send", game.skin, "militopia-btn");
        sendBtn.getLabel().setFontScale(0.6f);
        sendBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sendMessage();
            }
        });

        Table inputRow = new Table();
        inputRow.add(inputField).growX().padLeft(8).padRight(4).padBottom(8).padTop(4);
        inputRow.add(sendBtn).width(80).padRight(8).padBottom(8).padTop(4);
        card.add(inputRow).growX().row();

        // Consume touch so clicks don't fall through to map
        card.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                stage.setScrollFocus(scrollPane);
                return true;
            }
        });

        card.setBounds(sw, 0, PANEL_WIDTH, sh);
        card.setVisible(false);
        stage.addActor(card);
        
        if (gameState != null && gameState.chatHistory != null) {
            for (com.militopia.data.GameState.ChatMessage msg : gameState.chatHistory) {
                int resolvedID = msg.senderID;
                if (resolvedID == 0) {
                    resolvedID = (gameState.p1Name != null && gameState.p1Name.equals(msg.sender)) ? 1 : 2;
                    msg.senderID = resolvedID;
                }
                renderMessage(resolvedID, msg.sender, msg.text);
            }
        }
    }

    public void addMessage(int senderID, String sender, String text) {
        if (gameState != null) {
            gameState.chatHistory.add(new com.militopia.data.GameState.ChatMessage(senderID, sender, text));
        }
        renderMessage(senderID, sender, text);
    }

    private void renderMessage(int senderID, String sender, String text) {
        boolean isLocal = (senderID == gameState.localPlayerID);

        Table rowTable = new Table();

        Table bubble = new Table();
        Color bubbleColor = isLocal ? new Color(0.1f, 0.45f, 0.8f, 1f) : new Color(0.2f, 0.2f, 0.2f, 1f);
        bubble.setBackground(makeSolidDrawable(bubbleColor.r, bubbleColor.g, bubbleColor.b, bubbleColor.a));
        bubble.pad(10);

        Label textLbl = new Label(text, game.skin);
        textLbl.setFontScale(0.55f);
        // Do not enable wrap yet so we can measure the single-line width
        textLbl.setWrap(false);
        textLbl.setAlignment(Align.left);

        if (!isLocal) {
            Label nameLbl = new Label(sender, game.skin);
            nameLbl.setFontScale(0.45f);
            nameLbl.setColor(Color.LIGHT_GRAY);
            bubble.add(nameLbl).left().padBottom(4).row();
        }

        float prefWidth = textLbl.getPrefWidth();
        float maxWidth = 240f;

        if (prefWidth > maxWidth) {
            textLbl.setWrap(true);
            bubble.add(textLbl).width(maxWidth).left();
        } else {
            bubble.add(textLbl).left();
        }

        if (isLocal) {
            rowTable.add(bubble).right().expandX().padLeft(40).padRight(12).padBottom(8);
        } else {
            rowTable.add(bubble).left().expandX().padLeft(12).padRight(40).padBottom(8);
        }

        messageList.add(rowTable).width(340f).fillX().row();
        scrollPane.layout();
        scrollPane.setScrollPercentY(1f);
    }

    public void toggle() {
        visible = !visible;
        float sw = stage.getWidth();
        float sh = stage.getHeight();

        card.clearActions();
        if (visible) {
            card.setSize(PANEL_WIDTH, sh);
            card.setVisible(true);
            card.addAction(Actions.moveTo(sw - PANEL_WIDTH, 0, SLIDE_DURATION, Interpolation.fastSlow));
        } else {
            card.addAction(Actions.sequence(
                Actions.moveTo(sw, 0, SLIDE_DURATION, Interpolation.fastSlow),
                Actions.run(new Runnable() {
                    @Override public void run() { card.setVisible(false); }
                })
            ));
        }
    }

    public void resize() {
        if (card == null) return;
        float sw = stage.getWidth();
        float sh = stage.getHeight();
        float targetX = visible ? sw - PANEL_WIDTH : sw;
        card.setBounds(targetX, 0, PANEL_WIDTH, sh);
    }

    private void sendMessage() {
        if (inputField == null) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        if (networkManager != null) {
            networkManager.send(NetworkMessage.chat(localPlayerName, text));
        }
        addMessage(gameState.localPlayerID, localPlayerName, text);
        inputField.setText("");
    }

    private TextureRegionDrawable makeSolidDrawable(float r, float g, float b, float a) {
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(r, g, b, a);
        px.fill();
        Texture t = new Texture(px);
        px.dispose();
        return new TextureRegionDrawable(new TextureRegion(t));
    }
}
