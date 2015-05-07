package org.onaips.comandomeo;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Very simple class that manages button position and keycode of the remote.
 * Searching for a button within a certain position simply iterates
 *  the list of buttons, returning the first one that contains the input position.
 */
public class Buttons {
	List<Button> mButtons = new ArrayList<Buttons.Button>();

	public class Button {
		String mName;
		Rect mPosition;
		int mKeycode;

		public Button(String name, int keycode, int x, int y, int width, int height) {
			mName = name;
			mKeycode = keycode;
			mPosition = new Rect(x, y, x + width, y + height);
		}
	}

	Buttons() {
		mButtons.add(new Button("power", 233, 230, 22, 80, 80));

		mButtons.add(new Button("1", 49, 10, 106, 80, 80));
		mButtons.add(new Button("2", 50, 120, 106, 80, 80));
		mButtons.add(new Button("3", 51, 230, 106, 80, 80));

		mButtons.add(new Button("4", 52, 10, 191, 80, 80));
		mButtons.add(new Button("5", 53, 120, 191, 80, 80));
		mButtons.add(new Button("6", 54, 230, 191, 80, 80));

		mButtons.add(new Button("7", 55, 10, 276, 80, 80));
		mButtons.add(new Button("8", 56, 120, 276, 80, 80));
		mButtons.add(new Button("9", 57, 230, 276, 80, 80));

		mButtons.add(new Button("0", 48, 120, 360, 80, 80));
		mButtons.add(new Button("av", 0, 10, 360, 80, 80));
		mButtons.add(new Button("enter", 0, 230, 360, 80, 80));

		mButtons.add(new Button("v+", 175, 12, 480, 82, 82));
		mButtons.add(new Button("v-", 174, 10, 694, 82, 82));
		mButtons.add(new Button("p+", 33, 228, 480, 82, 82));
		mButtons.add(new Button("p-", 34, 228, 694, 82, 82));

		mButtons.add(new Button("OK", 13, 100, 570, 120, 120));
		mButtons.add(new Button("menu", 36, 100, 778, 120, 50));

		mButtons.add(new Button("left", 37, 20, 568, 70, 120));
		mButtons.add(new Button("up", 38, 100, 490, 120, 70));
		mButtons.add(new Button("right", 39, 230, 568, 70, 120));
		mButtons.add(new Button("down", 40, 100, 696, 120, 70));

		mButtons.add(new Button("back", 8, 10, 840, 62, 62));
		mButtons.add(new Button("screen", 27, 90, 840, 62, 62));
		mButtons.add(new Button("guia", 112, 168, 840, 62, 62));
		mButtons.add(new Button("videoc", 114, 248, 840, 62, 62));

		mButtons.add(new Button("i", 159, 10, 929, 80, 80));
		mButtons.add(new Button("switchs", 156, 120, 929, 80, 80));
		mButtons.add(new Button("gravac", 115, 230, 929, 80, 80));

		mButtons.add(new Button("stop", 123, 10, 1014, 80, 80));
		mButtons.add(new Button("play", 119, 120, 1014, 80, 80));
		mButtons.add(new Button("rec", 225, 230, 1014, 80, 80));

		mButtons.add(new Button("prev", 117, 10, 1124, 62, 62));
		mButtons.add(new Button("rev", 118, 90, 1124, 62, 62));
		mButtons.add(new Button("forw", 121, 168, 1124, 62, 62));
		mButtons.add(new Button("next", 122, 248, 1124, 62, 62));

		mButtons.add(new Button("red", 140, 10, 1204, 62, 62));
		mButtons.add(new Button("green", 141, 90, 1204, 62, 62));
		mButtons.add(new Button("yellow", 142, 168, 1204, 62, 62));
		mButtons.add(new Button("blue", 143, 248, 1204, 62, 62));

		mButtons.add(new Button("mute", 173, 10, 1283, 62, 62));
		mButtons.add(new Button("song", 0, 90, 1283, 62, 62));
		mButtons.add(new Button("stripes", 111, 168, 1283, 62, 62));
		mButtons.add(new Button("tv", 0, 248, 1283, 62, 62));
	}

	/**
	 * Draws the buttons in the canvas in order to confirm their position
	 * withing the svg.
	 */
	public void renderDebug(Canvas canvas) {
		Paint paint = new Paint();

		for (Button button: mButtons) {
			paint.setColor(Color.GRAY);
			paint.setAlpha(100);
			canvas.drawRect(button.mPosition, paint);
			paint.setColor(Color.WHITE);
			paint.setAlpha(100);

			canvas.drawText(button.mName, button.mPosition.left,
					button.mPosition.top, paint);
		}
	}

	/**
	 * Returns the button keycode for the provided position, -1 if
	 * none.
	 */
	public int get(int x, int y) {
		for (Button button: mButtons) {
			if (button.mPosition.contains(x, y)) {
				return button.mKeycode;
			}
		}
		return -1;
	}
}