package tilegame;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Iterator;
import java.util.Vector;

@SuppressWarnings("serial")
public class Board extends JPanel implements Iterable<Space>, MouseListener, MouseMotionListener, KeyListener {

	protected static final int width  = 60;
	protected static final int height = Board.width*3/4;
	
	protected enum Phase {Setup, Play, GameOver};

	protected Space[][] spaces = new Space[Board.width][Board.height];
	protected Vector<Piece> toPlace;

	private Point topLeftCorner = new Point(150,100);
	private Color bgColor = Palette.BUFF;

	private Player white = null;
	private Player black = null;
	//Player suggester = null;
	public volatile Player nextPlayer = null;

	public ValidMoves vm = null;
	private Phase _phase = Phase.Placement;

	private PerhapsDisplay notification = new PerhapsDisplay("", 10, 20);
	//private PerhapsDisplay suggestion = new PerhapsDisplay("", 10, 50);

	int xindex, yindex;
	Point lastDrag = new Point(0,0);
	Stack liftedStack = new NullStack();
	Point lastLiftedLoc = new Point(0,0);

	public Board() {

		white = new HumanPlayer(this, Piece.WHITE);
		black = new SmartBot(this, Piece.BLACK);
		//suggester = new SimpletonBot(this, null);

		nextPlayer = white;
		notification.setVal("White places first");

		for (int i = 0; i < spaces.length; i++) {
			spaces[i] = new Space[5];
		}

		// create spaces with correct locations;
		for (int i = 0; i < spaces.length; i++) {
			for (int j = 0; j < spaces[i].length; j++) {
				if (j == 0 && ( i == 9 || i == 10))
					spaces[i][j] = new NullSpace();
				else if (j == 1 && (i == 10))
					spaces[i][j] = new NullSpace();
				else if (j == 3 && (i == 0))
					spaces[i][j] = new NullSpace();
				else if (j == 4 && (i == 0 || i == 1))
					spaces[i][j] = new NullSpace();
				else {
					int startx = a1.x;
					int starty = a1.y;
					spaces[i][j] = new Space(startx + i * Space.xgap*2 - j*(Space.xgap), starty + j*Space.ygap);
				}
			}
		}

		// tell the spaces who their neighbours are (works so far for distance 1)
		for (int i = 0; i < spaces.length; i++) {
			for (int j = 0; j < spaces[i].length; j++) {
				int[][] initVectors = {{-1,-1},{-1,0},{0,1},{1,1},{1,0},{0,-1}};
				int[] mults = {1,2,3,4,5,6,7,8,9,10,11};
				for (int mult : mults) {
					int[][] vectors = pointWiseMult(mult, initVectors);
					for (int[] vector : vectors) {
						int possx = i + vector[0];
						int possy = j + vector[1];
						if (isInBoard(possx, possy))
							spaces[i][j].addNeighbour(mult, spaces[possx][possy]);
					}
				}
			}
		}

		liftedStack = new NullStack();
		toPlace = new Vector<Piece>();
		for(int i = 0; i < 3; i++) {toPlace.add(Piece.DVONN);}
		for(int i = 0; i < 23; i++) {toPlace.add(Piece.BLACK); toPlace.add(Piece.WHITE);}
		setSize(new Dimension(845, 446));
		this.addMouseListener(this);
		this.addKeyListener(this);
		this.addMouseMotionListener(this);
		this.setFocusable(true);
		this.requestFocusInWindow();

		//start player threads and ask one to move to keep the game running
		white.start();
		black.start();
		advanceGame();
	}

	private boolean isInBoard(int possx, int possy) {
		if (possx >= 0 && possx < spaces.length)
			if (possy >= 0 && possy < spaces[possx].length)
				return true;
		return false;
	}

	private int[][] pointWiseMult(int m, int[][] vectors) {
		int[][] result = new int[vectors.length][];
		for (int i = 0; i < result.length; i++) {
			result[i] = new int[vectors[i].length];
		}
		for (int i = 0; i < vectors.length; i++) {
			for (int j = 0; j < vectors[i].length; j++)
				result[i][j] = m*vectors[i][j];
		}
		return result;
	}

	public Phase phase() {
		if (toPlace.size() == 0) {
			if (_phase == Phase.Placement) {
				_phase = Phase.Play;
				nextPlayer = white;
				notification.setVal("White moves first");
				advanceGame();
			}
			return _phase;
		}
		return Phase.Placement;
	}

	public synchronized void swapPlayer() {
		if (nextPlayer == white) {
			nextPlayer = black;
			notification.setVal("Black's turn");
		} else {
			nextPlayer = white;
			notification.setVal("White's turn");
		}
	}

	public synchronized void advanceGame() {
		Board.println("Advancing Game");

		//remove disconnected pieces
		removeDisconnected();

		//refresh valid moves for entire board
		vm = new ValidMoves(this);

		//check winning conditions
		if (phase() == Phase.Play && vm.allMoves().length == 0) {
			finishGame();
			return;
		}

		//get next player to move if applicable
		if (phase() == Phase.Placement || vm.allMoves().length > 0)
			nextPlayer.movePlease();

		this.notifyAll();
		this.repaint();
	}

	private synchronized void removeDisconnected() {
		if (phase() != Phase.Play)
			return;

		Vector<Space> marked = new Vector<Space>(29, 10);
		Vector<Space> adjacent = new Vector<Space>(26, 10);

		//mark the stacks with dvonn pieces in the board
		for (Space s : this)
			if (s.state.contains(Piece.DVONN))
				marked.add(s);

		//mark more pieces that are connected
		do {
			adjacent.clear();
			for (Space existing : marked)
				for (Space s : existing.neighbours.get(1))
					if (s.hasPiece())
						if (!marked.contains(s) && !adjacent.contains(s))
							adjacent.add(s);
			marked.addAll(adjacent);
		} while (adjacent.size() > 0);

		//sweep the disconnected stacks
		int removed = 0;
		for (Space s : this) {
			if (s.hasPiece() && !marked.contains(s)) {
				s.liftStack();
				removed ++;
			}
		}
		Board.println("Removed " + removed + " disconnected stack" + (removed == 1 ? "" : "s"));
	}
	
	private Space[] _edgeSpaces = null;
	public synchronized Space[] getEdgeSpaces() {
		if (_edgeSpaces != null)
			return _edgeSpaces;
		_edgeSpaces = new Space[24];
		int i = 0;
		int t = 0;
		for (Space s : this) {
			if (i <= 9 || i == 18 || i == 19 || i == 29 || i == 30 || i >= 39)
				_edgeSpaces[t++] = s;
			i ++;
		}
		return _edgeSpaces;
	}

	public synchronized Vector<Space> getTheoreticalDisconnected(PlayMove m) {
		Vector<Space> marked = new Vector<Space>(29, 10);
		Vector<Space> adjacent = new Vector<Space>(26, 10);

		//mark the stacks with dvonn pieces in the board
		for (Space s : this)
			if (s != m.from && s.state.contains(Piece.DVONN))
				marked.add(s);
		
		//mark the destination stack if the from contains a DVONN piece
		if (m.from.state.contains(Piece.DVONN))
			marked.add(m.to);

		//mark more pieces that are connected
		do {
			adjacent.clear();
			for (Space existing : marked)
				for (Space s : existing.neighbours.get(1))
					if (s.hasPiece() && m.from != s)
						if (!marked.contains(s) && !adjacent.contains(s))
							adjacent.add(s);
			marked.addAll(adjacent);
		} while (adjacent.size() > 0);

		Vector<Space> disconnected = new Vector<Space>(26, 10);

		//list disconnected Spaces that contain Stack<Piece>s
		for (Space s : this) {
			if (s.hasPiece() && m.from != s && !marked.contains(s))
				disconnected.add(s);
		}

		return disconnected;
	}

	public synchronized void finishGame() {
		_phase = Phase.GameOver;

		//figure out who won
		int whiteScore = 0;
		int blackScore = 0;
		for (Space s : this) {
			if (s.state.peek() == Piece.WHITE)
				whiteScore += s.state.size();
			if (s.state.peek() == Piece.BLACK)
				blackScore += s.state.size();
		}

		//show who won
		if (whiteScore > blackScore) {
			notification.setVal("Game Over - White Won");
			bgColor = Palette.FINAL_WHITE;
		} else if (whiteScore < blackScore) {
			notification.setVal("Game Over - Black Won");
			bgColor = Palette.FINAL_BLACK;
		} else {
			notification.setVal("Game Over - Tie");
			bgColor = Palette.FINAL_RED;
		}
		this.repaint();
	}

	public void paint(Graphics g) {
		Board.print("#"); ///I'm painting
		g.setColor(bgColor);
		g.fillRect(0,0, getWidth(), getHeight());

		g.setColor(Color.BLACK);
		for (Space s : this) {
			s.paint(g);
		}

		notification.paint(g);

		liftedStack.paint(g, lastLiftedLoc.x - lastDrag.x, lastLiftedLoc.y - lastDrag.y);
	}

	public void mouseClicked(MouseEvent e) {
		if (phase() == Phase.Placement) {
			nextPlayer.mouseClicked(e);
		}
		if (notification.mouseClicked(e))
			this.repaint();
	}
	public void mousePressed(MouseEvent e) {
		if (phase() == Phase.Play) {
			lastDrag = e.getPoint();
			nextPlayer.mousePressed(e);
		}
	}
	public void mouseReleased(MouseEvent e) {
		if (phase() == Phase.Play) {
			nextPlayer.mouseReleased(e);
		}
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mouseDragged(MouseEvent e) {
		if (liftedStack instanceof NullStack)
			return;
		lastDrag = e.getPoint();	
		this.repaint();
	}
	public void mouseMoved(MouseEvent e) {}

	public synchronized void keyTyped(KeyEvent e) {
		try {Thread.sleep(200);} catch (InterruptedException x) {} //to reduce the likelihood of this ruining other Threads' behaviours
		java.util.Random r = new java.util.Random();
		//fill up the rest of the board automagically
		if (e.getKeyChar() == 'f') {
			for (Space s : this) {
				if (toPlace.size() > 0 && !s.hasPiece() && s.inPlay())
					s.addPiece(toPlace.remove(r.nextInt(toPlace.size())));
			}
			phase();
			this.repaint();
		}
	}

	public void keyPressed(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}

	public Iterator<Space> iterator() {
		return new BoardIterator(this);
	}
	
	public static void print(String s) {
		System.out.print(s);
	}
	public static void println(String s) {
		System.out.println(s);
	}
}