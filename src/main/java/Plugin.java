import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;

import org.silentsoft.actlist.plugin.ActlistPlugin;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Plugin extends ActlistPlugin {

	public static void main(String[] args) {
		//debug();
	}

	public Plugin() throws Exception {
		super("Screen Capture");
		
		setPluginVersion("1.0.0");
		setPluginAuthor("silentsoft.org", URI.create("https://github.com/silentsoft/actlist-plugin-screen-capture"));
		setPluginUpdateCheckURI(URI.create("http://actlist.silentsoft.org/api/plugin/5d58a0f6/update/check"));
		
		setOneTimePlugin(true);
		setMinimumCompatibleVersion(1, 2, 10);
	}

	@Override
	protected void initialize() throws Exception { }

	@Override
	public void pluginActivated() throws Exception {
		new Thread(() -> {
			try {
				// wait for end of the toggle button's animation
				Thread.sleep(150);
				
				requestHideActlist();

				Platform.runLater(() -> {
					try {
						Rectangle captureRectangle = new Rectangle();

						Rectangle screenRectangle = new Rectangle();
						GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
						for (GraphicsDevice graphicsDevice : graphicsEnvironment.getScreenDevices()) {
							screenRectangle = screenRectangle.union(graphicsDevice.getDefaultConfiguration().getBounds());
						}

						Stage stage = new Stage(StageStyle.TRANSPARENT);
						{
							Pane dragObject = new Pane();
							dragObject.setPrefSize(0, 0);
							dragObject.setStyle("-fx-background-color: rgb(0, 0, 0);");

							class CustomPoint {
								Point scenePoint;
								Point screenPoint;
								public CustomPoint(Point scenePoint, Point screenPoint) {
									this.scenePoint = scenePoint;
									this.screenPoint = screenPoint;
								}
							}
							
							Pane pane = new Pane(dragObject);
							pane.setMinSize(screenRectangle.getWidth(), screenRectangle.getHeight());
							pane.setPrefSize(screenRectangle.getWidth(), screenRectangle.getHeight());
							pane.setMaxSize(screenRectangle.getWidth(), screenRectangle.getHeight());
							pane.setCursor(Cursor.CROSSHAIR);
							pane.setOnDragDetected(mouseEvent -> {
								CustomPoint customPoint = new CustomPoint(new Point((int) mouseEvent.getSceneX(), (int) mouseEvent.getSceneY()), new Point((int) mouseEvent.getScreenX(), (int) mouseEvent.getScreenY()));
								dragObject.setUserData(customPoint); // save start point

								pane.startFullDrag();
							});
							pane.setOnMouseDragOver(mouseDragEvent -> {
								CustomPoint customPoint = (CustomPoint) dragObject.getUserData();
								{
									int x = (int) Math.min(customPoint.scenePoint.getX(), mouseDragEvent.getSceneX());
									int y = (int) Math.min(customPoint.scenePoint.getY(), mouseDragEvent.getSceneY());
									int width = (int) Math.abs(customPoint.scenePoint.getX() - mouseDragEvent.getSceneX());
									int height = (int) Math.abs(customPoint.scenePoint.getY() - mouseDragEvent.getSceneY());
									dragObject.setLayoutX(x);
									dragObject.setLayoutY(y);
									dragObject.setPrefWidth(width);
									dragObject.setPrefHeight(height);
								}
								{
									int x = (int) Math.min(customPoint.screenPoint.getX(), mouseDragEvent.getScreenX());
									int y = (int) Math.min(customPoint.screenPoint.getY(), mouseDragEvent.getScreenY());
									int width = (int) Math.abs(customPoint.screenPoint.getX() - mouseDragEvent.getScreenX());
									int height = (int) Math.abs(customPoint.screenPoint.getY() - mouseDragEvent.getScreenY());
									captureRectangle.setBounds(x, y, width, height);
								}
							});
							pane.setOnMouseReleased(mouseEvent -> {
								stage.close();
								Platform.runLater(() -> {
									try {
										class ClipboardImage implements Transferable {
											BufferedImage image;
											public ClipboardImage(BufferedImage image) {
												this.image = image;
											}
											@Override
											public boolean isDataFlavorSupported(DataFlavor flavor) {
												return DataFlavor.imageFlavor.equals(flavor);
											}
		
											@Override
											public DataFlavor[] getTransferDataFlavors() {
												return new DataFlavor[] { DataFlavor.imageFlavor };
											}
		
											@Override
											public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
												if (DataFlavor.imageFlavor.equals(flavor)) {
													return image;
												}
												return null;
											}
										}
										ClipboardImage clipboardImage = new ClipboardImage(new Robot().createScreenCapture(captureRectangle));
										Toolkit.getDefaultToolkit().getSystemClipboard().setContents(clipboardImage, null);
									} catch (Exception e) {
										e.printStackTrace();
									}
								});
							});
							pane.setOpacity(0.20);

							stage.setScene(new Scene(pane, screenRectangle.getWidth(), screenRectangle.getHeight(), Color.TRANSPARENT));
						}
						stage.setAlwaysOnTop(true);
						stage.setX(screenRectangle.getX());
						stage.setY(screenRectangle.getY());
						stage.setWidth(screenRectangle.getWidth());
						stage.setMinWidth(screenRectangle.getWidth());
						stage.setMaxWidth(screenRectangle.getWidth());
						stage.setHeight(screenRectangle.getHeight());
						stage.setMinHeight(screenRectangle.getHeight());
						stage.setMaxHeight(screenRectangle.getHeight());
						stage.addEventHandler(KeyEvent.KEY_RELEASED, (keyEvent) -> {
							if (KeyCode.ESCAPE.equals(keyEvent.getCode())) {
								stage.close();
							}
						});
						stage.showAndWait();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						requestShowActlist();
						requestDeactivate();
					}
				});
			} catch (Exception e) {

			}
		}).start();
	}

	@Override
	public void pluginDeactivated() throws Exception { }

}
