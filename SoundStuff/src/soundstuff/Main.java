package soundstuff;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.leapmotion.leap.*;

public class Main {
	
	private static Synthesizer synth;
	private static int channelNum = 0;
	private static JFrame guiFrame = new JFrame("Finger Piano");
	
	private static Controller controller = new Controller();
	
	private static HandList handlist = null;
	private static Map<Integer, Integer> noteForHandID = new HashMap<Integer, Integer>();
	//private static Map<Integer, Boolean> fast
	
	private static final int distancePerNote = 30;
	private static final int[] keyboardNotes = {60, 62, 64, 65, 67, 69, 71, 72}; 
	private static final String[] noteNames = {"C", "D", "E", "F", "G", "A", "B", "C"}; 
	
	public static void setupSynthesizer() {
		try {
			synth = MidiSystem.getSynthesizer();
			synth.open();
		} catch (MidiUnavailableException mue) {
			System.out.println("Sorry, MIDI is unavailable.");
			return;
		}
		
		synth.getChannels()[channelNum].programChange(0);		
	}
	
	public static void setupFrame() {                
		JPanel pane = new JPanel() {
			
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				
				Graphics2D g2 = (Graphics2D) g;
				for (int i = 0; i < keyboardNotes.length; i++) {
					g2.setColor(Color.LIGHT_GRAY);
					g2.fillRect(i * 600 / keyboardNotes.length + 1, 1, 600 / keyboardNotes.length - 2, 398);
					g2.drawString(noteNames[i], i * 600 / keyboardNotes.length, y);
				}
				g2.setColor(Color.DARK_GRAY);
				g2.setStroke(new BasicStroke(2.0f));
				
				g2.drawLine(0, 200, 600, 200);
				
				if (handlist != null) {
					for (Hand hand : handlist) {
						g2.setColor(Color.YELLOW);
						int x = (int) (600.0 / keyboardNotes.length * (0.5 * keyboardNotes.length + hand.palmPosition().getX() / distancePerNote));
						int y = 200 - (int) (hand.palmPosition().getY() - 200.0);
						g2.fillOval(x - 20, y - 20, 40, 40);
						g2.setColor(Color.RED);
						float handzpos = hand.palmPosition().getZ();
						int r;
						if (handzpos > 0 && handzpos < 100) {
							r = 20 - (int) handzpos / 5;
						} else if (handzpos <= 0) {
							g2.setColor(Color.GREEN);
							r = (int) (20 * Math.exp(0.005 * handzpos));
						} else {
							r = 0;
						}
						g2.fillOval(x - r, y - r, 2 * r, 2 * r);
					}	
				}
			}
		};
		pane.setPreferredSize(new Dimension(600, 400));
		
		guiFrame.add(pane);
		guiFrame.pack();
		guiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		guiFrame.setVisible(true);
		
		/*final HashMap<Integer, Integer> keyToNote = new HashMap<Integer, Integer>();
		
		keyToNote.put(KeyEvent.VK_A, 60);
		keyToNote.put(KeyEvent.VK_W, 61);
		keyToNote.put(KeyEvent.VK_S, 62);
		keyToNote.put(KeyEvent.VK_E, 63);
		keyToNote.put(KeyEvent.VK_D, 64);
		keyToNote.put(KeyEvent.VK_F, 65);
		keyToNote.put(KeyEvent.VK_T, 66);
		keyToNote.put(KeyEvent.VK_G, 67);
		keyToNote.put(KeyEvent.VK_Y, 68);
		keyToNote.put(KeyEvent.VK_H, 69);
		keyToNote.put(KeyEvent.VK_U, 70);
		keyToNote.put(KeyEvent.VK_J, 71);
		keyToNote.put(KeyEvent.VK_K, 72);
		
		frame.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent arg0) {
				int keyCode = arg0.getKeyCode();
				if (keyToNote.containsKey(keyCode)) {
					int note = keyToNote.get(keyCode);
					MidiChannel channel = synth.getChannels()[channelNum];
					channel.noteOn(note, 600);
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				int keyCode = arg0.getKeyCode();
				if (keyToNote.containsKey(keyCode)) {
					int note = keyToNote.get(keyCode);
					MidiChannel channel = synth.getChannels()[channelNum];
					channel.noteOff(note, 600);
				}
			}

			@Override
			public void keyTyped(KeyEvent arg0) {
			}
			
		});*/
	}
	
	public static void main(String[] args) {
		
		setupSynthesizer();
		setupFrame();
		
		Timer timer = new Timer(100, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Frame frame = controller.frame();
				MidiChannel channel = synth.getChannels()[channelNum];
				handlist = frame.hands();
				for (Hand hand : frame.hands()) {
					if (hand.palmPosition().getZ() < 0.0) {
						int index = (int) (0.5 * keyboardNotes.length + hand.palmPosition().getX() / distancePerNote);
						if (index < 0) {
							index = 0;
						} else if (index >= keyboardNotes.length) {
							index = keyboardNotes.length - 1;
						}
						int newnote = keyboardNotes[index];
						
						if (hand.palmPosition().getY() > 200.0) {
							newnote += 12;
						}
						
						if (noteForHandID.containsKey(hand.id())) {
							int oldnote = noteForHandID.get(hand.id());
							channel.noteOff(oldnote);
						} else {
							channel.noteOn(newnote, 600);
						}
							noteForHandID.put(hand.id(), newnote);
					} else {
						if (noteForHandID.containsKey(hand.id())) {
							int oldnote = noteForHandID.get(hand.id());
							channel.noteOff(oldnote);
							noteForHandID.remove(hand.id());
						}
					}
				}
				
				guiFrame.repaint();
			}
			
		});
		timer.start();
	}
}
