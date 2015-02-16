package tilegame;

import java.awt.Color;

public class Palette {
	public static final Color BACKGROUND = new Color(24, 24, 24);
	
	public final Color[] PIECE_COLORS_LIGHT = {
		new Color(216, 193, 246),
		new Color(193, 216, 246),
		new Color(246, 194, 198),
		new Color(197, 246, 194),
		new Color(246, 249, 198),
		new Color(198, 249, 249)
	};
	
	public final Color[] PIECE_COLORS_DARK = {
		new Color(138, 0, 173),
		new Color(0, 72, 173),
		new Color(173, 0, 0),
		new Color(6, 173, 1),
		new Color(177, 161, 5),
		new Color(5, 174, 177)
	};
}
