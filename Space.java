package tilegame;

import java.awt.Polygon;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;


@SuppressWarnings("serial")
public class Space extends Polygon {

	public static final int width = 32;
	public static final int height = Space.width;
	
	int x;
	int y;

	protected Stack<Piece> pieces;
	protected List<Space> neighbours;

	public Polygon translated(int x, int y) {
		Polygon ret = new Polygon(xpoints, ypoints, npoints);
		ret.translate(-1*x, -1*y);
		return ret;
	}
	
	public Space(int x, int y) {
		this.x = x;
		this.y = y;
		pieces = new Stack<Piece>();

		this.addPoint(x, y);
		this.addPoint(x+width, y);
		this.addPoint(x+width, y+height);
		this.addPoint(x, y+height);

		neighbours = new ArrayList<Space>(8);
	}

	public void addPiece(Piece p) {
		pieces.add(p);
	}

	public List<Space> getNeighbours() {
		return neighbours;
	}

	public int getHeight() {
		return pieces.size();
	}

	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
		g2.setColor(Palette.BACKGROUND);
		g2.fillPolygon(this);
		g2.setColor(Color.BLACK);
		g2.drawPolygon(this);
		state.paint(g2);
	}
	public void paintAsSource(Graphics g) {

	}
	public void paintAsTarget(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
		g2.setColor(Palette.SPECIAL);
		g2.fill(new Ellipse2D.Double(this.x, this.y, 10,10));
	}
	public boolean hasPiece() {
		return state.size() > 0;
	}
	public boolean inPlay() {
		return true;
	}

	public Stack liftStack() {
		Stack tmp = state;
		state = new Stack(this); //new empty stack
		return tmp;
	}

	public void dropStack(Stack add) {
		state.addAll(add);
	}

	public boolean isNull() {return false;}
	
	public String toString() {return String.valueOf("("+ this.getBounds().x) + "," + String.valueOf(this.getBounds().y) +")";}

}